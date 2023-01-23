package org.jruby.ir.targets;

public interface InstanceVariableCompiler {
    /**
     * Store instance variable into self.
     * <p>
     * Stack required: none
     * Stack result: empty
     *
     * @param target runnable to push target object, may be called twice
     * @param value runnable to push value to assign, will only be called once
     * @param name name of variable to store
     */
    public abstract void putField(Runnable target, Runnable value, String name);

    /**
     * Load instance variable from self.
     * <p>
     * Stack required: none
     * Stack result: value from self
     *
     * @param source runnable to push source object, may be called twice
     * @param name name of variable to load
     */
    public abstract void getField(Runnable source, String name);
}
