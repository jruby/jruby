package org.jruby.util.cli;

/**
 * Representation of available option categories, with a short name to use
 * in printing descriptions.
 */
public enum Category {
    COMPILER("compiler"),
    INVOKEDYNAMIC("invokedynamic"),
    JIT("jit"),
    IR("intermediate representation"),
    NATIVE("native"),
    THREADPOOL("thread pooling"),
    MISCELLANEOUS("miscellaneous"),
    DEBUG("debugging and logging"),
    JAVA_INTEGRATION("java integration");

    Category(String desc) {
        this.desc = desc;
    }

    public String desc() {
        return desc;
    }

    public String toString() {
        return desc;
    }
    
    private final String desc;
}
