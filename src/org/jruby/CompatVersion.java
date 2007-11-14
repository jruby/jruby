package org.jruby;

public enum CompatVersion {

    RUBY1_8, RUBY1_9, BOTH;

    public static CompatVersion getVersionFromString(String compatString) {
        if (compatString.equalsIgnoreCase("RUBY1_8")) {
            return CompatVersion.RUBY1_8;
        } else if (compatString.equalsIgnoreCase("RUBY1_9")) {
            return CompatVersion.RUBY1_9;
        } else {
            return null;
        }
    }
}
