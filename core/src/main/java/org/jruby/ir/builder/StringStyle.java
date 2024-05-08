package org.jruby.ir.builder;

public enum StringStyle {
    Frozen, // Explicitly frozen from option or pragma
    Mutable, // Explicitly mutable from option or pragma
    Chilled, // New String instances which warn if you mutate but say they are frozen
}