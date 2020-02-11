package org.jruby.ir.targets;

public interface ConstantCompiler {
    /**
     * Lookup a constant from current context.
     * <p>
     * Stack required: context, static scope
     *
     * @param name            name of the constant
     * @param noPrivateConsts whether to ignore private constants
     */
    void searchConst(String name, boolean noPrivateConsts);

    /**
     * Lookup a constant from current module.
     * <p>
     * Stack required: context, static scope
     *
     * @param name            name of the constant
     * @param noPrivateConsts whether to ignore private constants
     */
    void searchModuleForConst(String name, boolean noPrivateConsts, boolean callConstMissing);

    /**
     * Lookup a constant from a given class or module.
     * <p>
     * Stack required: context, module
     *
     * @param name            name of the constant
     * @param noPrivateConsts whether to ignore private constants
     */
    void inheritanceSearchConst(String name, boolean noPrivateConsts);

    /**
     * Lookup a constant from a lexical scope.
     * <p>
     * Stack required: context, static scope
     *
     * @param name name of the constant
     */
    void lexicalSearchConst(String name);
}
