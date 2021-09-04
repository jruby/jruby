package org.jruby.ir;

public enum IRScopeType {
    CLOSURE, EVAL_SCRIPT, INSTANCE_METHOD, CLASS_METHOD, MODULE_BODY, CLASS_BODY, METACLASS_BODY, SCRIPT_BODY, FOR;

    private static final IRScopeType[] VALUES = values();

    public static IRScopeType fromOrdinal(int ordinal) {
        if (ordinal < 0 || ordinal >= VALUES.length) {
            return null;
        }
        return VALUES[ordinal];
    }

    // Is this a block type of closure vs an instance of IRClosure (which is also a for/eval internally)?
    public boolean isBlock() {
        return this == CLOSURE;
    }

    public boolean isEval() {
        return this == EVAL_SCRIPT;
    }

    public boolean isMethodType() {
        return this == INSTANCE_METHOD || this == CLASS_METHOD;
    }

    public boolean isClosureType() {
        return this == CLOSURE || this == FOR || this == EVAL_SCRIPT;
    }

    public boolean isMethod() {
        return this == INSTANCE_METHOD || this == CLASS_METHOD;
    }

    public boolean isMethodContainer() {
        switch (this) {
            case MODULE_BODY:
            case CLASS_BODY:
            case METACLASS_BODY:
            case SCRIPT_BODY:
                return true;

            default:
                return false;
        }
    }
}
