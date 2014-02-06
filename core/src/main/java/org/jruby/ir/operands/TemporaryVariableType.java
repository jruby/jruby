package org.jruby.ir.operands;

/**
 * This enum exists because we will frequently run into an arbitrary temporary variable
 * and we want to be able to quickly switch on type.
 */
public enum TemporaryVariableType {
    LOCAL, BOOLEAN, FLOAT, FIXNUM, CLOSURE, CURRENT_MODULE, CURRENT_SCOPE;

    public static TemporaryVariableType fromOrdinal(int value) {
        return value < 0 || value >= values().length ? null : values()[value];
    }
}
