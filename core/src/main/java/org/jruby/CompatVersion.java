package org.jruby;

public enum CompatVersion {

    RUBY1_8, RUBY1_9, RUBY2_0, BOTH;

    public boolean is1_9() {
        return this == RUBY1_9 || this == RUBY2_0;
    }

    public boolean is2_0() {
        return this == RUBY2_0;
    }

    public static CompatVersion getVersionFromString(String compatString) {
        if (compatString.equalsIgnoreCase("RUBY1_8")) {
            return CompatVersion.RUBY1_8;
        } else if (compatString.equalsIgnoreCase("1.8")) {
            return CompatVersion.RUBY1_8;
        } else if (compatString.equalsIgnoreCase("RUBY1_9")) {
            return CompatVersion.RUBY1_9;
        } else if (compatString.equalsIgnoreCase("1.9")) {
            return CompatVersion.RUBY1_9;
        } else if (compatString.equalsIgnoreCase("RUBY2_0")) {
            return CompatVersion.RUBY2_0;
        } else if (compatString.equalsIgnoreCase("2.0")) {
            return CompatVersion.RUBY2_0;
        } else {
            return null;
        }
    }
    
    public static boolean shouldBindMethod(CompatVersion runtimeVersion, CompatVersion methodVersion) {
        if (runtimeVersion == RUBY1_8) return methodVersion == RUBY1_8 || methodVersion == BOTH;
        if (runtimeVersion == RUBY1_9) return methodVersion == RUBY1_9 || methodVersion == BOTH;
        if (runtimeVersion == RUBY2_0) return methodVersion == RUBY1_9 || methodVersion == RUBY2_0 || methodVersion == BOTH;
        return false;
    }
}
