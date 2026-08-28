package org.jruby.ir.targets;

public interface LocalVariableCompiler {
    /**
     * Load local variable from dynamic scope.
     * <p>
     * Stack required: scope
     * Stack result: value
     *
     * @param depth depth into scope stack
     * @param index index into scope
     */
    public abstract void getHeapLocal(int depth, int index);
    /**
     * Load local variable from dynamic scope, returning nil if the resulting value is null.
     * <p>
     * Stack required: scope
     * Stack result: value
     *
     * @param depth depth into scope stack
     * @param index index into scope
     */
    public abstract void getHeapLocalOrNil(int depth, int index);
}
