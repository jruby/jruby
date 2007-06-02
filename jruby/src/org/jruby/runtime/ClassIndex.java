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
 */
public class ClassIndex {
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
    public static final int MAX_CLASSES = 16;
    
    /** Creates a new instance of ClassIndex */
    public ClassIndex() {
    }
    
}
