package org.jruby.ir.transformations.inlining;

public enum CloneMode {
    // Cloning of a scope's instructions. Used in two contexts currently.
    // 1. Cloning of a method's instructions which is going to get other methods inlined into it.
    // 2. Cloning of a closure when its containing scope is being inlined into another scope.
    NORMAL_CLONE,
    // Cloning of an ensure block's instructions into the host scope.
    ENSURE_BLOCK_CLONE,
    // Cloning of a method's instructions when
    // it is being inlined into a host scope
    METHOD_INLINE,
    // Cloning of a closure when it is being inlined
    // into a host scope
    CLOSURE_INLINE
}
