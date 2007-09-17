/*
 * InvocationCompiler.java
 * 
 * Created on Jul 14, 2007, 12:37:44 AM
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.runtime.CallType;

import org.jruby.compiler.impl.SkinnyMethodAdapter;

/**
 *
 * @author headius
 */
public interface InvocationCompiler {
    public SkinnyMethodAdapter getMethodAdapter();
    public void setMethodAdapter(SkinnyMethodAdapter sma);
    /**
     * Invoke the named method as a "function", i.e. as a method on the current "self"
     * object, using the specified argument count. It is expected that previous calls
     * to the compiler has prepared the exact number of argument values necessary for this
     * call. Those values will be consumed, and the result of the call will be generated.
     */
    //public void invokeDynamic(String name, boolean hasReceiver, boolean hasArgs, CallType callType, ClosureCallback closureArg, boolean attrAssign);
    public void invokeDynamic(String name, ClosureCallback receiverCallback, ClosureCallback argsCallback, CallType callType, ClosureCallback closureArg, boolean attrAssign);
    
    public void invokeSuper(ClosureCallback argsCallback, ClosureCallback closureCallback);
    
    /**
     * Attr assign calls have slightly different semantics that normal calls, so this method handles those additional semantics.
     */
    public void invokeAttrAssign(String name);
    
    public void opElementAsgn(ClosureCallback valueCallback, String operator);
    
    /**
     * Invoke the block passed into this method, or throw an error if no block is present.
     * If arguments have been prepared for the block, specify true. Otherwise the default
     * empty args will be used.
     */
    public void yield(boolean hasArgs, boolean unwrap);
    
    /**
     * Used for when nodes with a case; assumes stack is ..., case_value, when_cond_array
     */
    public void invokeEqq();
}
