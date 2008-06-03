/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.compiler.impl;

import java.util.HashMap;
import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubyFixnum;
import org.jruby.RubySymbol;
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
    public static final int MAX_INHERITED_SYMBOLS = 50;
    public static final int MAX_INHERITED_FIXNUMS = 50;
    
    int callSiteCount = 0;
    int inheritedSymbolCount = 0;
    int inheritedFixnumCount = 0;
    
    public InheritedCacheCompiler(StandardASMCompiler scriptCompiler) {
        super(scriptCompiler);
    }
    
    @Override
    public void cacheCallSite(StandardASMCompiler.AbstractMethodCompiler method, String name, CallType callType) {
        String fieldName = "site" + callSiteCount;
        
        // retrieve call adapter
        SkinnyMethodAdapter initMethod = scriptCompiler.getInitMethod();
        initMethod.aload(StandardASMCompiler.THIS);
        method.loadThis();
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
            method.method.getfield(scriptCompiler.getClassname(), fieldName, ci(CallSite.class));
        } else {
            initMethod.putfield(scriptCompiler.getClassname(), fieldName, ci(CallSite.class));
            method.method.getfield(scriptCompiler.getClassname(), fieldName, ci(CallSite.class));
        }
        callSiteCount++;
    }
    
    Map<String, String> inheritedSymbols = new HashMap<String, String>();
    
    @Override
    public void cacheSymbol(StandardASMCompiler.AbstractMethodCompiler method, String symbol) {
        String methodName = inheritedSymbols.get(symbol);
        if (methodName == null && inheritedSymbolCount < MAX_INHERITED_SYMBOLS) {
            methodName = "getSymbol" + inheritedSymbolCount++;
            inheritedSymbols.put(symbol, methodName);
        }
        
        if (methodName == null) {
            super.cacheSymbol(method, symbol);
        } else {
            method.loadThis();
            method.loadRuntime();
            method.method.ldc(symbol);
            method.method.invokevirtual(scriptCompiler.getClassname(), methodName, sig(RubySymbol.class, Ruby.class, String.class));
        }
    }
    
    Map<Long, String> inheritedFixnums = new HashMap<Long, String>();
    
    @Override
    public void cacheFixnum(StandardASMCompiler.AbstractMethodCompiler method, long value) {
        String methodName = inheritedFixnums.get(value);
        if (methodName == null && inheritedFixnumCount < MAX_INHERITED_FIXNUMS) {
            methodName = "getFixnum" + inheritedFixnumCount++;
            inheritedFixnums.put(value, methodName);
        }
        
        if (methodName == null) {
            super.cacheFixnum(method, value);
        } else {
            method.loadThis();
            method.loadRuntime();
            method.method.ldc(value);
            method.method.invokevirtual(scriptCompiler.getClassname(), methodName, sig(RubyFixnum.class, Ruby.class, long.class));
        }
    }
}
