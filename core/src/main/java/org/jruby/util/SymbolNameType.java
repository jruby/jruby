package org.jruby.util;

/**
 * The characters within a Symbol can represent concepts in Ruby.
 * This enum allows us to precalculate what the symbol could be
 * if it is used in that context.  We use the same names as MRI
 * for convenience (except they use -1 to indicate what we will
 * call OTHER).
 */
public enum SymbolNameType {
    GLOBAL, INSTANCE, JUNK, ATTRSET, CLASS, LOCAL, CONST, OTHER;
}
