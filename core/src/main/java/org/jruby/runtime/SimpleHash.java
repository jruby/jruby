package org.jruby.runtime;

public interface SimpleHash {
    default long longHashCode() {
        return hashCode();
    }
}
