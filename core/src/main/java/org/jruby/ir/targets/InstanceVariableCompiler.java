package org.jruby.ir.targets;

public interface InstanceVariableCompiler {
    /**
     * Store instance variable into self.
     * <p>
     * Stack required: self, value
     * Stack result: empty
     *
     * @param name name of variable to store
     */
    public abstract void putField(String name);

    /**
     * Load instance variable from self.
     * <p>
     * Stack required: self
     * Stack result: value from self
     *
     * @param name name of variable to load
     */
    public abstract void getField(String name);
}
