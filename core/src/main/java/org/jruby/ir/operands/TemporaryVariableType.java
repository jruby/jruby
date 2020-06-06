package org.jruby.ir.operands;

/**
 * This enum exists because we will frequently run into an arbitrary temporary variable
 * and we want to be able to quickly switch on type.
 */
public enum TemporaryVariableType {
    LOCAL, BOOLEAN, FLOAT, FIXNUM, CLOSURE, CURRENT_MODULE;

    private static final TemporaryVariableType[] VALUES = values();

    public static TemporaryVariableType fromOrdinal(int ordinal) {
        if (ordinal < 0 || ordinal >= VALUES.length) {
            return null;
        }
        return VALUES[ordinal];
    }
}
