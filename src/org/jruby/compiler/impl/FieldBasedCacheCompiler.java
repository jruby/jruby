/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.compiler.impl;

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
    private StandardASMCompiler scriptCompiler;
    
    Map<String, String> sourcePositions = new HashMap<String, String>();
    Map<String, String> byteLists = new HashMap<String, String>();
    Map<String, String> symbols = new HashMap<String, String>();
    
    public FieldBasedCacheCompiler(StandardASMCompiler scriptCompiler) {
        this.scriptCompiler = scriptCompiler;
    }
    
    public void cacheCallSite(SkinnyMethodAdapter method, String name, CallType callType, boolean block) {
        String fieldName = scriptCompiler.getNewConstant(cg.ci(CallSite.class), JavaNameMangler.mangleStringForCleanJavaIdentifier(name));
        
        // retrieve call adapter
        SkinnyMethodAdapter initMethod = scriptCompiler.getInitMethod();
        initMethod.aload(StandardASMCompiler.THIS);
        initMethod.ldc(name);
        if (block) {
            if (callType.equals(CallType.NORMAL)) {
                initMethod.invokestatic(cg.p(MethodIndex.class), "getCallSite", cg.sig(CallSite.class, cg.params(String.class)));
            } else if (callType.equals(CallType.FUNCTIONAL)) {
                initMethod.invokestatic(cg.p(MethodIndex.class), "getFunctionalCallSite", cg.sig(CallSite.class, cg.params(String.class)));
            } else if (callType.equals(CallType.VARIABLE)) {
                initMethod.invokestatic(cg.p(MethodIndex.class), "getVariableCallSite", cg.sig(CallSite.class, cg.params(String.class)));
            }
        } else {
            if (callType.equals(CallType.NORMAL)) {
                initMethod.invokestatic(cg.p(MethodIndex.class), "getNonBlockCallSite", cg.sig(CallSite.class, cg.params(String.class)));
            } else if (callType.equals(CallType.FUNCTIONAL)) {
                initMethod.invokestatic(cg.p(MethodIndex.class), "getNonBlockFunctionalCallSite", cg.sig(CallSite.class, cg.params(String.class)));
            } else if (callType.equals(CallType.VARIABLE)) {
                initMethod.invokestatic(cg.p(MethodIndex.class), "getNonBlockVariableCallSite", cg.sig(CallSite.class, cg.params(String.class)));
            }
        }
        initMethod.putfield(scriptCompiler.getClassname(), fieldName, cg.ci(CallSite.class));
        
        method.getfield(scriptCompiler.getClassname(), fieldName, cg.ci(CallSite.class));
    }
    
    public void cachePosition(SkinnyMethodAdapter method, String file, int line) {
        String cleanName = JavaNameMangler.mangleStringForCleanJavaIdentifier(file + "$" + line);
        String fieldName = sourcePositions.get(cleanName);
        if (fieldName == null) {
            SkinnyMethodAdapter clinitMethod = scriptCompiler.getClassInitMethod();
            fieldName = scriptCompiler.getNewStaticConstant(cg.ci(ISourcePosition.class), cleanName);
            sourcePositions.put(JavaNameMangler.mangleStringForCleanJavaIdentifier(file + "$" + line), fieldName);

            clinitMethod.ldc(file);
            clinitMethod.ldc(line);
            clinitMethod.invokestatic(cg.p(RuntimeHelpers.class), "constructPosition", cg.sig(ISourcePosition.class, String.class, int.class));
            clinitMethod.putstatic(scriptCompiler.getClassname(), fieldName, cg.ci(ISourcePosition.class));
        }
        
        method.getstatic(scriptCompiler.getClassname(), fieldName, cg.ci(ISourcePosition.class));
    }
    
    public void cacheByteList(SkinnyMethodAdapter method, String contents) {
        String fieldName = byteLists.get(contents);
        if (fieldName == null) {
            SkinnyMethodAdapter clinitMethod = scriptCompiler.getClassInitMethod();
            fieldName = scriptCompiler.getNewStaticConstant(cg.ci(ByteList.class), "byteList");
            byteLists.put(contents, fieldName);

            clinitMethod.ldc(contents);
            clinitMethod.invokestatic(cg.p(ByteList.class), "create", cg.sig(ByteList.class, CharSequence.class));
            clinitMethod.putstatic(scriptCompiler.getClassname(), fieldName, cg.ci(ByteList.class));
        }
        
        method.getstatic(scriptCompiler.getClassname(), fieldName, cg.ci(ByteList.class));
    }
    
    public void cacheSymbol(SkinnyMethodAdapter method, String symbol) {
        String methodName = symbols.get(symbol);
        if (methodName == null) {
            String fieldName = scriptCompiler.getNewConstant(cg.ci(RubySymbol.class), "symbol");
            
            methodName = "getSymbol" + fieldName;
            symbols.put(symbol, methodName);
            
            ClassVisitor cv = scriptCompiler.getClassVisitor();
            
            SkinnyMethodAdapter symMethod = new SkinnyMethodAdapter(
                    cv.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC, methodName, 
                            cg.sig(RubySymbol.class, Ruby.class), null, null));
            symMethod.start();
            symMethod.aload(0);
            symMethod.getfield(scriptCompiler.getClassname(), fieldName, cg.ci(RubySymbol.class));
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
            symMethod.invokevirtual(cg.p(Ruby.class), "fastNewSymbol", cg.sig(RubySymbol.class, String.class));
            symMethod.dup_x1();
            symMethod.putfield(scriptCompiler.getClassname(), fieldName, cg.ci(RubySymbol.class));
            symMethod.areturn();
            symMethod.end();
        }
        
        method.aload(0);
        method.aload(StandardASMCompiler.RUNTIME_INDEX);
        method.invokevirtual(scriptCompiler.getClassname(), methodName, 
                cg.sig(RubySymbol.class, cg.params(Ruby.class)));
    }
}
