package org.jruby.ir.targets;

public interface GlobalVariableCompiler {
    /**
     * Retrieve a global variable with the given name.
     * <p>
     * Stack required: none
     */
    void getGlobalVariable(String name, String file);

    /**
     * Set the global variable with the given name to the value on stack.
     * <p>
     * Stack required: the new value
     */
    void setGlobalVariable(String name, String file);
}
