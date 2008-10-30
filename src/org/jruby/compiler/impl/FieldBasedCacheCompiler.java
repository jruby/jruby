package org.jruby.compiler.impl;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubyFixnum;
import org.jruby.RubySymbol;
import org.jruby.ast.NodeType;
import org.jruby.compiler.ASTInspector;
import org.jruby.compiler.CacheCompiler;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.CompiledBlockCallback;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.util.ByteList;
import org.jruby.util.JavaNameMangler;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import static org.jruby.util.CodegenUtils.*;

public abstract class FieldBasedCacheCompiler implements CacheCompiler {
    protected StandardASMCompiler scriptCompiler;
    
    Map<String, String> sourcePositions = new HashMap<String, String>();
    Map<BigInteger, String> bigIntegers = new HashMap<BigInteger, String>();
    Map<String, String> symbols = new HashMap<String, String>();
    Map<Long, String> fixnums = new HashMap<Long, String>();
    
    public FieldBasedCacheCompiler(StandardASMCompiler scriptCompiler) {
        this.scriptCompiler = scriptCompiler;
    }
    
    public void cacheBigInteger(BaseBodyCompiler method, BigInteger bigint) {
        String fieldName = bigIntegers.get(bigint);
        if (fieldName == null) {
            SkinnyMethodAdapter clinitMethod = scriptCompiler.getClassInitMethod();
            fieldName = scriptCompiler.getNewStaticConstant(ci(BigInteger.class), "bigInt");
            bigIntegers.put(bigint, fieldName);

            clinitMethod.newobj(p(BigInteger.class));
            clinitMethod.dup();
            clinitMethod.ldc(bigint.toString());
            clinitMethod.invokespecial(p(BigInteger.class), "<init>", sig(void.class, String.class));
            clinitMethod.putstatic(scriptCompiler.getClassname(), fieldName, ci(BigInteger.class));
        }
        
        method.method.getstatic(scriptCompiler.getClassname(), fieldName, ci(BigInteger.class));
    }
    
    public void cacheClosure(BaseBodyCompiler method, String closureMethod, int arity, StaticScope scope, boolean hasMultipleArgsHead, NodeType argsNodeId, ASTInspector inspector) {
        String closureFieldName = scriptCompiler.getNewConstant(ci(BlockBody.class), "closure");

        String closureMethodName = "getClosure_" + closureFieldName;

        ClassVisitor cv = scriptCompiler.getClassVisitor();

        {
            SkinnyMethodAdapter closureGetter = new SkinnyMethodAdapter(
                    cv.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC, closureMethodName, 
                            sig(BlockBody.class, ThreadContext.class), null, null));

            closureGetter.aload(0);
            closureGetter.getfield(scriptCompiler.getClassname(), closureFieldName, ci(BlockBody.class));
            closureGetter.dup();
            Label alreadyCreated = new Label();
            closureGetter.ifnonnull(alreadyCreated);

            // no callback, construct and cache it
            closureGetter.pop();
            closureGetter.aload(0); // [this]
            
            // create callbackloadThreadContext();
            closureGetter.aload(1);
            closureGetter.aload(0);
            closureGetter.ldc(closureMethod); // [this, runtime, this, str]
            closureGetter.pushInt(arity);
            StandardASMCompiler.buildStaticScopeNames(closureGetter, scope);
            closureGetter.ldc(Boolean.valueOf(hasMultipleArgsHead));
            closureGetter.pushInt(BlockBody.asArgumentType(argsNodeId));
            // if there's a sub-closure or there's scope-aware methods, it can't be "light"
            closureGetter.ldc(!(inspector.hasClosure() || inspector.hasScopeAwareMethods()));
            closureGetter.invokestatic(p(RuntimeHelpers.class), "createCompiledBlockBody",
                    sig(BlockBody.class, ThreadContext.class, Object.class, String.class, int.class, 
                    String[].class, boolean.class, int.class, boolean.class));
            
            closureGetter.putfield(scriptCompiler.getClassname(), closureFieldName, ci(BlockBody.class)); // []
            closureGetter.aload(0); // [this]
            closureGetter.getfield(scriptCompiler.getClassname(), closureFieldName, ci(BlockBody.class)); // [callback]

            closureGetter.label(alreadyCreated);
            closureGetter.areturn();

            closureGetter.end();
        }

        method.loadThis();
        method.loadThreadContext();
        method.method.invokevirtual(scriptCompiler.getClassname(), closureMethodName, 
                sig(BlockBody.class, ThreadContext.class));
    }
    
    public void cacheClosureOld(BaseBodyCompiler method, String closureMethod) {
        String closureFieldName = scriptCompiler.getNewConstant(ci(CompiledBlockCallback.class), "closure");

        String closureMethodName = "getClosure_" + closureFieldName;

        ClassVisitor cv = scriptCompiler.getClassVisitor();

        {
            SkinnyMethodAdapter closureGetter = new SkinnyMethodAdapter(
                    cv.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC, closureMethodName, 
                            sig(CompiledBlockCallback.class, Ruby.class), null, null));

            closureGetter.aload(0);
            closureGetter.getfield(scriptCompiler.getClassname(), closureFieldName, ci(CompiledBlockCallback.class));
            closureGetter.dup();
            Label alreadyCreated = new Label();
            closureGetter.ifnonnull(alreadyCreated);

            // no callback, construct and cache it
            closureGetter.pop();
            closureGetter.aload(0); // [this]
            closureGetter.aload(1); // [this, runtime]
            closureGetter.aload(0); // [this, runtime, this]
            closureGetter.ldc(closureMethod); // [this, runtime, this, str]
            closureGetter.invokestatic(p(RuntimeHelpers.class), "createBlockCallback",
                    sig(CompiledBlockCallback.class, Ruby.class, Object.class, String.class)); // [this, callback]
            closureGetter.putfield(scriptCompiler.getClassname(), closureFieldName, ci(CompiledBlockCallback.class)); // []
            closureGetter.aload(0); // [this]
            closureGetter.getfield(scriptCompiler.getClassname(), closureFieldName, ci(CompiledBlockCallback.class)); // [callback]

            closureGetter.label(alreadyCreated);
            closureGetter.areturn();

            closureGetter.end();
        }

        method.loadThis();
        method.loadRuntime();
        method.method.invokevirtual(scriptCompiler.getClassname(), closureMethodName, 
                sig(CompiledBlockCallback.class, params(Ruby.class)));
    }
}
