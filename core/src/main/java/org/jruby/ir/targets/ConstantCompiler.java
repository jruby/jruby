package org.jruby.ir.targets;

import org.jruby.util.ByteList;

public interface ConstantCompiler {
    /**
     * Lookup a constant from current context.
     * <p>
     * Stack required: context, static scope
     *
     * @param id the "ID string" of the constant name
     * @param name            name of the constant
     * @param noPrivateConsts whether to ignore private constants
     */
    void searchConst(String id, ByteList name, boolean noPrivateConsts);

    /**
     * Lookup a constant from current module.
     * <p>
     * Stack required: context, module
     *
     * @param id the "ID string" of the constant name
     * @param name            name of the constant
     * @param noPrivateConsts whether to ignore private constants
     */
    void searchModuleForConst(String id, ByteList name, boolean noPrivateConsts, boolean callConstMissing);

    /**
     * Lookup a constant from a given class or module.
     * <p>
     * Stack required: context, module
     *
     * @param id the "ID string" of the constant name
     * @param name            name of the constant
     */
    void inheritanceSearchConst(String id, ByteList name);

    /**
     * Lookup a constant from a lexical scope.
     * <p>
     * Stack required: context, static scope
     *
     * @param id the "ID string" of the constant name
     * @param name name of the constant
     */
    void lexicalSearchConst(String id, ByteList name);
}
