/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.compiler.impl;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubySymbol;
import org.jruby.compiler.CacheCompiler;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.MethodIndex;
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
    
    public FieldBasedCacheCompiler(StandardASMCompiler scriptCompiler) {
        this.scriptCompiler = scriptCompiler;
    }
    
    public void cacheCallSite(SkinnyMethodAdapter method, String name, CallType callType) {
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
        
        method.getfield(scriptCompiler.getClassname(), fieldName, ci(CallSite.class));
    }
    
    public void cachePosition(SkinnyMethodAdapter method, String file, int line) {
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
        
        method.getstatic(scriptCompiler.getClassname(), fieldName, ci(ISourcePosition.class));
    }
    
    public void cacheByteList(SkinnyMethodAdapter method, String contents) {
        String fieldName = byteLists.get(contents);
        if (fieldName == null) {
            SkinnyMethodAdapter clinitMethod = scriptCompiler.getClassInitMethod();
            fieldName = scriptCompiler.getNewStaticConstant(ci(ByteList.class), "byteList");
            byteLists.put(contents, fieldName);

            clinitMethod.ldc(contents);
            clinitMethod.invokestatic(p(ByteList.class), "create", sig(ByteList.class, CharSequence.class));
            clinitMethod.putstatic(scriptCompiler.getClassname(), fieldName, ci(ByteList.class));
        }
        
        method.getstatic(scriptCompiler.getClassname(), fieldName, ci(ByteList.class));
    }
    
    public void cacheBigInteger(SkinnyMethodAdapter method, BigInteger bigint) {
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
        
        method.getstatic(scriptCompiler.getClassname(), fieldName, ci(BigInteger.class));
    }
    
    public void cacheSymbol(SkinnyMethodAdapter method, String symbol) {
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
        
        method.aload(0);
        method.aload(StandardASMCompiler.RUNTIME_INDEX);
        method.invokevirtual(scriptCompiler.getClassname(), methodName, 
                sig(RubySymbol.class, params(Ruby.class)));
    }
}
