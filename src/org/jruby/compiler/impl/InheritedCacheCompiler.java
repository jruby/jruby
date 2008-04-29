/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.compiler.impl;

import org.jruby.Ruby;
import org.jruby.RubySymbol;
import org.jruby.ast.executable.AbstractScript;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.MethodIndex;
import static org.jruby.util.CodegenUtils.*;

/**
 *
 * @author headius
 */
public class InheritedCacheCompiler extends FieldBasedCacheCompiler {
    public static final int MAX_INHERITED_CALL_SITES = 50;
    public static final int MAX_INHERITED_SYMBOLS = 20;
    
    final int runtimeIndex;
    
    int callSiteCount = 0;
//    int byteListCount = 0;
//    int sourcePositionsCount = 0;
    int symbolCount = 0;
    
    public InheritedCacheCompiler(StandardASMCompiler scriptCompiler, int runtimeIndex) {
        super(scriptCompiler);
        this.runtimeIndex = runtimeIndex;
    }
    
    @Override
    public void cacheCallSite(SkinnyMethodAdapter method, String name, CallType callType) {
        String fieldName = "site" + callSiteCount;
        
        // retrieve call adapter
        SkinnyMethodAdapter initMethod = scriptCompiler.getInitMethod();
        initMethod.aload(StandardASMCompiler.THIS);
        method.aload(StandardASMCompiler.THIS);
        initMethod.ldc(name);
        
        if (callType.equals(CallType.NORMAL)) {
            initMethod.invokestatic(p(MethodIndex.class), "getCallSite", sig(CallSite.class, params(String.class)));
        } else if (callType.equals(CallType.FUNCTIONAL)) {
            initMethod.invokestatic(p(MethodIndex.class), "getFunctionalCallSite", sig(CallSite.class, params(String.class)));
        } else if (callType.equals(CallType.VARIABLE)) {
            initMethod.invokestatic(p(MethodIndex.class), "getVariableCallSite", sig(CallSite.class, params(String.class)));
        }

        if (callSiteCount >= MAX_INHERITED_CALL_SITES) {
            scriptCompiler.getNewField(ci(CallSite.class), fieldName, null);
            initMethod.putfield(scriptCompiler.getClassname(), fieldName, ci(CallSite.class));
            method.getfield(scriptCompiler.getClassname(), fieldName, ci(CallSite.class));
        } else {
            initMethod.putfield(scriptCompiler.getClassname(), fieldName, ci(CallSite.class));
            method.getfield(scriptCompiler.getClassname(), fieldName, ci(CallSite.class));
        }
        callSiteCount++;
    }
    
    @Override
    public void cacheSymbol(SkinnyMethodAdapter method, String symbol) {
        String methodName = symbols.get(symbol);
        if (methodName == null && symbolCount < MAX_INHERITED_SYMBOLS) {
            methodName = "getSymbol" + symbolCount++;
            symbols.put(symbol, methodName);
        }
        
        if (methodName == null) {
            super.cacheSymbol(method, symbol);
        } else {
            method.aload(0);
            method.aload(runtimeIndex);
            method.ldc(symbol);
            method.invokevirtual(p(AbstractScript.class), methodName, sig(RubySymbol.class, Ruby.class, String.class));
        }
    }
}
