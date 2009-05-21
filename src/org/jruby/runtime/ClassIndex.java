/*
 * ClassIndex.java
 *
 * Created on January 1, 2007, 7:50 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.runtime;

/**
 *
 * @author headius
 * FIXME convert to enum ?
 */
public final class ClassIndex {
    public static final int NO_INDEX = 0;
    public static final int FIXNUM = 1;
    public static final int BIGNUM = 2;
    public static final int ARRAY = 3;
    public static final int STRING = 4;
    public static final int NIL = 5;
    public static final int TRUE = 6;
    public static final int FALSE = 7;
    public static final int SYMBOL = 8;
    public static final int REGEXP = 9;
    public static final int HASH = 10;
    public static final int FLOAT = 11;
    public static final int MODULE = 12;
    public static final int CLASS = 13;
    public static final int OBJECT = 14;
    public static final int STRUCT = 15;
    public static final int INTEGER = 16;
    public static final int NUMERIC = 17;
    public static final int RANGE = 18;
    public static final int TIME = 19;
    public static final int COMPLEX = 20;
    public static final int RATIONAL = 21;
    public static final int ENCODING = 22;
    public static final int CONVERTER = 23;
    public static final int GENERATOR = 24;
    public static final int YIELDER = 25;
    public static final int FILE = 26;
    public static final int MATCH = 27;
    public static final int MAX_CLASSES = 28;

    /** Creates a new instance of ClassIndex */
    private ClassIndex() {
    }
}
