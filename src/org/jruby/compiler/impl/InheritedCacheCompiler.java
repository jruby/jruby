/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.compiler.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    List<String> callSiteList = new ArrayList<String>();
    List<CallType> callTypeList = new ArrayList<CallType>();
    int inheritedSymbolCount = 0;
    int inheritedFixnumCount = 0;
    
    public InheritedCacheCompiler(StandardASMCompiler scriptCompiler) {
        super(scriptCompiler);
    }
    
    @Override
    public void cacheCallSite(StandardASMCompiler.AbstractMethodCompiler method, String name, CallType callType) {
        // retrieve call site from sites array
        method.loadThis();
        method.method.pushInt(callSiteCount);
        method.method.invokevirtual(scriptCompiler.getClassname(), "getCallSite", sig(CallSite.class, int.class));

        // add name to call site list
        callSiteList.add(name);
        callTypeList.add(callType);
        
        callSiteCount++;
    }

    public void finish() {
        SkinnyMethodAdapter initMethod = scriptCompiler.getInitMethod();
        initMethod.aload(StandardASMCompiler.THIS);

        // generate call site initialization code
        int size = callSiteList.size();
        if (size != 0) {
            initMethod.aload(0);
            initMethod.pushInt(size);
            initMethod.anewarray(p(CallSite.class));
            
            for (int i = 0; i < size; i++) {
                String name = callSiteList.get(i);
                CallType callType = callTypeList.get(i);

                initMethod.pushInt(i);
                initMethod.ldc(name);
                if (callType.equals(CallType.NORMAL)) {
                    initMethod.invokestatic(scriptCompiler.getClassname(), "setCallSite", sig(CallSite[].class, params(CallSite[].class, int.class, String.class)));
                } else if (callType.equals(CallType.FUNCTIONAL)) {
                    initMethod.invokestatic(scriptCompiler.getClassname(), "setFunctionalCallSite", sig(CallSite[].class, params(CallSite[].class, int.class, String.class)));
                } else if (callType.equals(CallType.VARIABLE)) {
                    initMethod.invokestatic(scriptCompiler.getClassname(), "setVariableCallSite", sig(CallSite[].class, params(CallSite[].class, int.class, String.class)));
                }
            }
            initMethod.putfield(scriptCompiler.getClassname(), "callSites", ci(CallSite[].class));
        }

        // generate symbols initialization code
        size = inheritedSymbolCount;
        if (size != 0) {
            initMethod.aload(0);
            initMethod.pushInt(size);
            initMethod.anewarray(p(RubySymbol.class));
            initMethod.putfield(scriptCompiler.getClassname(), "symbols", ci(RubySymbol[].class));
        }
    }
    
    @Override
    public void cacheSymbol(StandardASMCompiler.AbstractMethodCompiler method, String symbol) {
        method.loadThis();
        method.loadRuntime();
        method.method.pushInt(inheritedSymbolCount);
        method.method.ldc(symbol);
        method.method.invokevirtual(scriptCompiler.getClassname(), "getSymbol", sig(RubySymbol.class, Ruby.class, int.class, String.class));

        inheritedSymbolCount++;
    }
    
    Map<Long, String> inheritedFixnums = new HashMap<Long, String>();
    
    @Override
    public void cacheFixnum(StandardASMCompiler.AbstractMethodCompiler method, long value) {
        if (value <= 5 && value >= -1) {
            method.loadRuntime();
            switch ((int)value) {
            case -1:
                method.method.invokestatic(p(RubyFixnum.class), "minus_one", sig(RubyFixnum.class, Ruby.class));
                break;
            case 0:
                method.method.invokestatic(p(RubyFixnum.class), "zero", sig(RubyFixnum.class, Ruby.class));
                break;
            case 1:
                method.method.invokestatic(p(RubyFixnum.class), "one", sig(RubyFixnum.class, Ruby.class));
                break;
            case 2:
                method.method.invokestatic(p(RubyFixnum.class), "two", sig(RubyFixnum.class, Ruby.class));
                break;
            case 3:
                method.method.invokestatic(p(RubyFixnum.class), "three", sig(RubyFixnum.class, Ruby.class));
                break;
            case 4:
                method.method.invokestatic(p(RubyFixnum.class), "four", sig(RubyFixnum.class, Ruby.class));
                break;
            case 5:
                method.method.invokestatic(p(RubyFixnum.class), "five", sig(RubyFixnum.class, Ruby.class));
                break;
            default:
                throw new RuntimeException("wtf?");
            }
        } else {
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
}
