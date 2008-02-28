/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.compiler.impl;

import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.MethodIndex;
import static org.jruby.util.CodegenUtils.*;

/**
 *
 * @author headius
 */
public class InheritedCacheCompiler extends FieldBasedCacheCompiler {
    public static final int MAX_INHERITED_CALL_SITES = 0;
    
    int callSiteCount = 0;
//    int byteListCount = 0;
//    int sourcePositionsCount = 0;
//    int symbolCount = 0;
    
    public InheritedCacheCompiler(StandardASMCompiler scriptCompiler) {
        super(scriptCompiler);
    }
    
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
}
