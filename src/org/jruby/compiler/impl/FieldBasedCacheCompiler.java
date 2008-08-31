/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

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
import org.jruby.lexer.yacc.ISourcePosition;
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

/**
 *
 * @author headius
 */
public class FieldBasedCacheCompiler implements CacheCompiler {
    protected StandardASMCompiler scriptCompiler;
    
    Map<String, String> sourcePositions = new HashMap<String, String>();
    Map<String, String> byteLists = new HashMap<String, String>();
    Map<BigInteger, String> bigIntegers = new HashMap<BigInteger, String>();
    Map<String, String> symbols = new HashMap<String, String>();
    Map<Long, String> fixnums = new HashMap<Long, String>();
    
    public FieldBasedCacheCompiler(StandardASMCompiler scriptCompiler) {
        this.scriptCompiler = scriptCompiler;
    }

    public void finish() {
        // no finish for field-based
    }
    
    public void cacheCallSite(StandardASMCompiler.AbstractMethodCompiler method, String name, CallType callType) {
        String fieldName = scriptCompiler.getNewConstant(ci(CallSite.class), JavaNameMangler.mangleStringForCleanJavaIdentifier(name));
        
        // retrieve call adapter
        SkinnyMethodAdapter initMethod = scriptCompiler.getInitMethod();
        initMethod.aload(StandardASMCompiler.THIS);
        initMethod.ldc(name);
        
        if (callType.equals(CallType.NORMAL)) {
            initMethod.invokestatic(p(MethodIndex.class), "getCallSite", sig(CallSite.class, params(String.class)));
        } else if (callType.equals(CallType.FUNCTIONAL)) {
            initMethod.invokestatic(p(MethodIndex.class), "getFunctionalCallSite", sig(CallSite.class, params(String.class)));
        } else if (callType.equals(CallType.VARIABLE)) {
            initMethod.invokestatic(p(MethodIndex.class), "getVariableCallSite", sig(CallSite.class, params(String.class)));
        }

        initMethod.putfield(scriptCompiler.getClassname(), fieldName, ci(CallSite.class));
        
        method.method.getfield(scriptCompiler.getClassname(), fieldName, ci(CallSite.class));
    }
    
    @Deprecated
    public void cachePosition(StandardASMCompiler.AbstractMethodCompiler method, String file, int line) {
        String cleanName = JavaNameMangler.mangleStringForCleanJavaIdentifier(file + "$" + line);
        String fieldName = sourcePositions.get(cleanName);
        if (fieldName == null) {
            SkinnyMethodAdapter clinitMethod = scriptCompiler.getClassInitMethod();
            fieldName = scriptCompiler.getNewStaticConstant(ci(ISourcePosition.class), cleanName);
            sourcePositions.put(JavaNameMangler.mangleStringForCleanJavaIdentifier(file + "$" + line), fieldName);

            clinitMethod.ldc(file);
            clinitMethod.ldc(line);
            clinitMethod.invokestatic(p(RuntimeHelpers.class), "constructPosition", sig(ISourcePosition.class, String.class, int.class));
            clinitMethod.putstatic(scriptCompiler.getClassname(), fieldName, ci(ISourcePosition.class));
        }
        
        method.method.getstatic(scriptCompiler.getClassname(), fieldName, ci(ISourcePosition.class));
    }
    
    public void cacheByteList(StandardASMCompiler.AbstractMethodCompiler method, String contents) {
        String fieldName = byteLists.get(contents);
        if (fieldName == null) {
            SkinnyMethodAdapter clinitMethod = scriptCompiler.getClassInitMethod();
            fieldName = scriptCompiler.getNewStaticConstant(ci(ByteList.class), "byteList");
            byteLists.put(contents, fieldName);

            clinitMethod.ldc(contents);
            clinitMethod.invokestatic(p(ByteList.class), "create", sig(ByteList.class, CharSequence.class));
            clinitMethod.putstatic(scriptCompiler.getClassname(), fieldName, ci(ByteList.class));
        }
        
        method.method.getstatic(scriptCompiler.getClassname(), fieldName, ci(ByteList.class));
    }
    
    public void cacheBigInteger(StandardASMCompiler.AbstractMethodCompiler method, BigInteger bigint) {
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
    
    public void cacheSymbol(StandardASMCompiler.AbstractMethodCompiler method, String symbol) {
        String methodName = symbols.get(symbol);
        if (methodName == null) {
            String fieldName = scriptCompiler.getNewConstant(ci(RubySymbol.class), "symbol");
            
            methodName = "getSymbol" + fieldName;
            symbols.put(symbol, methodName);
            
            ClassVisitor cv = scriptCompiler.getClassVisitor();
            
            SkinnyMethodAdapter symMethod = new SkinnyMethodAdapter(
                    cv.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC, methodName, 
                            sig(RubySymbol.class, Ruby.class), null, null));
            symMethod.start();
            symMethod.aload(0);
            symMethod.getfield(scriptCompiler.getClassname(), fieldName, ci(RubySymbol.class));
            symMethod.dup();
            symMethod.astore(2);
            
            Label ifNullEnd = new Label();
            symMethod.ifnull(ifNullEnd);
            symMethod.aload(2);
            symMethod.areturn();
            symMethod.label(ifNullEnd);
            symMethod.aload(0);
            symMethod.aload(1);
            symMethod.ldc(symbol);
            symMethod.invokevirtual(p(Ruby.class), "fastNewSymbol", sig(RubySymbol.class, String.class));
            symMethod.dup_x1();
            symMethod.putfield(scriptCompiler.getClassname(), fieldName, ci(RubySymbol.class));
            symMethod.areturn();
            symMethod.end();
        }
        
        method.loadThis();
        method.loadRuntime();
        method.method.invokevirtual(scriptCompiler.getClassname(), methodName, 
                sig(RubySymbol.class, params(Ruby.class)));
    }
    
    public void cacheFixnum(StandardASMCompiler.AbstractMethodCompiler method, long value) {
        String methodName = fixnums.get(value);
        if (methodName == null) {
            String fieldName = scriptCompiler.getNewConstant(ci(RubyFixnum.class), "symbol");
            
            methodName = "getFixnum" + fieldName;
            fixnums.put(value, methodName);
            
            ClassVisitor cv = scriptCompiler.getClassVisitor();
            
            SkinnyMethodAdapter symMethod = new SkinnyMethodAdapter(
                    cv.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC, methodName, 
                            sig(RubyFixnum.class, Ruby.class), null, null));
            symMethod.start();
            symMethod.aload(0);
            symMethod.getfield(scriptCompiler.getClassname(), fieldName, ci(RubyFixnum.class));
            symMethod.dup();
            symMethod.astore(2);
            
            Label ifNullEnd = new Label();
            symMethod.ifnull(ifNullEnd);
            symMethod.aload(2);
            symMethod.areturn();
            symMethod.label(ifNullEnd);
            symMethod.aload(0);
            symMethod.aload(1);
            symMethod.ldc(value);
            symMethod.invokevirtual(p(Ruby.class), "newFixnum", sig(RubyFixnum.class, long.class));
            symMethod.dup_x1();
            symMethod.putfield(scriptCompiler.getClassname(), fieldName, ci(RubyFixnum.class));
            symMethod.areturn();
            symMethod.end();
        }
        
        method.loadThis();
        method.loadRuntime();
        method.method.invokevirtual(scriptCompiler.getClassname(), methodName, 
                sig(RubyFixnum.class, params(Ruby.class)));
    }
    
    public void cacheClosure(StandardASMCompiler.AbstractMethodCompiler method, String closureMethod, int arity, StaticScope scope, boolean hasMultipleArgsHead, NodeType argsNodeId, ASTInspector inspector) {
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
    
    public void cacheClosureOld(StandardASMCompiler.AbstractMethodCompiler method, String closureMethod) {
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
