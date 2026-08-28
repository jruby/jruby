package org.jruby.ir.targets;

import org.jruby.util.RegexpOptions;

public interface DynamicValueCompiler {
    /**
     * Build a dynamic regexp.
     * <p>
     * No stack requirement. The callback must push onto this method's stack the ThreadContext and all arguments for
     * building the dregexp, matching the given arity.
     *
     * @param options options for the regexp
     * @param arity   number of Strings passed in
     */
    void pushDRegexp(Runnable callback, RegexpOptions options, int arity);

    /**
     * Construct an Array from elements on stack.
     *
     * Stack required: all elements of array
     *
     * @param length number of elements
     */
    void array(int length);

    /**
     * Construct a Hash from elements on stack.
     *
     * Stack required: context, all elements of hash
     *
     * @param length number of element pairs
     */
    void hash(int length);
}
