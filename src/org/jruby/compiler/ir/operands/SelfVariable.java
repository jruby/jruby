package org.jruby.compiler.ir.operands;

/**
 * Represents the special variable 'self'
 */
public class SelfVariable extends Variable {
    @Override
    public boolean isSelf() {
        return true;
    }

    @Override
    public String getName() {
        return "self";
    }

    @Override
    public String toString() {
        return "self";
    }

    @Override
    public int hashCode() {
        return "self".hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof SelfVariable)) return false;

        return ((SelfVariable) other).isSelf();
    }

    public int compareTo(Object other) {
        return equals(other) == true ? 0 : 1;
    }
}
