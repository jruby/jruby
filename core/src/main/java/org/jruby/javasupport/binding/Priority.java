package org.jruby.javasupport.binding;

/**
 * Assigned names only override based priority of an assigned type, the type must be less than
 * or equal to the assigned type. For example, field name (FIELD) in a subclass will override
 * an alias (ALIAS) in a superclass, but not a method (METHOD).
 */
public enum Priority {
    RESERVED(0), METHOD(1), FIELD(2), PROTECTED_METHOD(3),
    WEAKLY_RESERVED(4), ALIAS(5), PROTECTED_FIELD(6);

    private int value;

    Priority(int value) {
        this.value = value;
    }

    public boolean asImportantAs(AssignedName other) {
        return other != null && other.type.value == value;
    }

    public boolean lessImportantThan(AssignedName other) {
        return other != null && other.type.value < value;
    }

    public boolean moreImportantThan(AssignedName other) {
        return other == null || other.type.value > value;
    }
}
