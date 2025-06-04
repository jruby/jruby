package org.jruby.ir.targets;

import org.jruby.RubySymbol;
import org.jruby.ir.instructions.AsStringInstr;
import org.jruby.ir.instructions.CallBase;
import org.jruby.ir.instructions.EQQInstr;

public interface InvocationCompiler {
    /**
     * Invoke a method on an object other than self.
     * <p>
     * Stack required: context, self, all arguments, optional block
     *
     * @param call the call to be invoked
     */
    void invokeOther(String file, String scopeFieldName, CallBase call, int arity);

    /**
     * Invoke the array dereferencing method ([]) on an object other than self.
     * <p>
     * If this invokes against a Hash with a frozen string, it will follow an optimized path.
     * <p>
     * Stack required: context, self, target, arg0
     *  @param file
     *
     */
    void invokeArrayDeref(String file, String scopeFieldName, CallBase call);

    /**
     * Invoke a fixnum-receiving method on an object other than self.
     * <p>
     * Stack required: context, self, receiver (fixnum will be handled separately)
     */
    void invokeOtherOneFixnum(String file, CallBase call, long fixnum);

    /**
     * Invoke a float-receiving method on an object other than self.
     * <p>
     * Stack required: context, self, receiver (float will be handled separately)
     */
    void invokeOtherOneFloat(String file, CallBase call, double flote);

    /**
     * Invoke a method on self.
     *
     * Stack required: context, caller, self, all arguments, optional block
     *  @param file the filename of the script making this call
     * @param call to be invoked on self
     * @param arity of the call.
     */
    void invokeSelf(String file, String scopeFieldName, CallBase call, int arity);

    /**
     * Invoke a superclass method from an instance context.
     * <p>
     * Stack required: context, caller, self, start class, arguments[, block]
     *
     * @param file           the filename of the script making this call
     * @param name           name of the method to invoke
     * @param arity          arity of the arguments on the stack
     * @param hasClosure     whether a block is passed
     * @param literalClosure whether the block passed is a literal closure
     * @param splatmap       a map of arguments to be splatted back into arg list
     * @param flags
     */
    void invokeInstanceSuper(String file, String name, int arity, boolean hasClosure, boolean literalClosure, boolean[] splatmap, int flags);

    /**
     * Invoke a superclass method from a class context.
     * <p>
     * Stack required: context, caller, self, start class, arguments[, block]
     *
     * @param file           the filename of the script making this call
     * @param name           name of the method to invoke
     * @param arity          arity of the arguments on the stack
     * @param hasClosure     whether a block is passed
     * @param literalClosure whether the block passed is a literal closure
     * @param splatmap       a map of arguments to be splatted back into arg list
     * @param flags
     */
    void invokeClassSuper(String file, String name, int arity, boolean hasClosure, boolean literalClosure, boolean[] splatmap, int flags);

    /**
     * Invoke a superclass method from an unresolved context.
     * <p>
     * Stack required: context, caller, self, arguments[, block]
     *
     * @param file           the filename of the script making this call
     * @param name           name of the method to invoke
     * @param arity          arity of the arguments on the stack
     * @param hasClosure     whether a block is passed
     * @param literalClosure whether the block passed is a literal closure
     * @param splatmap       a map of arguments to be splatted back into arg list
     * @param flags
     */
    void invokeUnresolvedSuper(String file, String name, int arity, boolean hasClosure, boolean literalClosure, boolean[] splatmap, int flags);

    /**
     * Invoke a superclass method from a zsuper in a block.
     * <p>
     * Stack required: context, caller, self, arguments[, block]
     *
     * @param file       the filename of the script making this call
     * @param name       name of the method to invoke
     * @param arity      arity of the arguments on the stack
     * @param hasClosure whether a block is passed
     * @param splatmap   a map of arguments to be splatted back into arg list
     * @param flags
     */
    void invokeZSuper(String file, String name, int arity, boolean hasClosure, boolean[] splatmap, int flags);

    /**
     * Perform a === call appropriate for a case/when statement.
     *
     * Stack required: context, case value, when value
     */
    void invokeEQQ(EQQInstr call);

    /**
     * Coerces the receiver to a String using to_s, unless it is already a String
     *
     * Stack required: context, caller, receiver
     */
    void asString(AsStringInstr call, String scopeFieldName, String file);

    /**
     * Sets the current callInfo, when it cannot be passed other ways
     *
     * Stack required: none
     */
    void setCallInfo(int flags);

    /**
     * Invoke block_given? or iterator? with awareness of any built-in methods.
     */
    void invokeBlockGiven(String methodName, String file);

    /**
     * Invoke __method__ or __callee__ with awareness of any built-in methods.
     */
    void invokeFrameName(String methodName, String file);

    /**
     * Check for a literal respond_to? result
     *
     * Stack required: context, caller, target
     *
     * @param id the method name to check respond_to?
     */
    void respondTo(CallBase callBase, RubySymbol id, String scopeFieldName, String file);
}
