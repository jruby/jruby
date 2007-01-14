/*
 * MethodSelectorTable.java
 *
 * Created on January 1, 2007, 7:45 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.runtime;

import org.jruby.RubyArray;
import org.jruby.RubyBignum;
import org.jruby.RubyFixnum;
import org.jruby.RubyString;

/**
 *
 * @author headius 
 */
public class MethodSelectorTable {
    public final byte[][] table = new byte[ClassIndex.MAX_CLASSES][MethodIndex.MAX_METHODS];
    
    public MethodSelectorTable() {
        // Fixnum
        table[ClassIndex.FIXNUM] = new byte[MethodIndex.MAX_METHODS];
        table[ClassIndex.FIXNUM][MethodIndex.OP_PLUS] = RubyFixnum.OP_PLUS_SWITCHVALUE; 
        table[ClassIndex.FIXNUM][MethodIndex.OP_MINUS] = RubyFixnum.OP_MINUS_SWITCHVALUE; 
        table[ClassIndex.FIXNUM][MethodIndex.OP_LT] = RubyFixnum.OP_LT_SWITCHVALUE; 
        
        // Bignum
        table[ClassIndex.BIGNUM] = new byte[MethodIndex.MAX_METHODS];
        table[ClassIndex.BIGNUM][MethodIndex.OP_PLUS] = RubyBignum.OP_PLUS_SWITCHVALUE; 
        table[ClassIndex.BIGNUM][MethodIndex.OP_MINUS] = RubyBignum.OP_MINUS_SWITCHVALUE; 
        table[ClassIndex.BIGNUM][MethodIndex.OP_LT] = RubyBignum.OP_LT_SWITCHVALUE;
        
        // Array
        table[ClassIndex.ARRAY] = new byte[MethodIndex.MAX_METHODS];
        table[ClassIndex.ARRAY][MethodIndex.OP_PLUS] = RubyArray.OP_PLUS_SWITCHVALUE;
        table[ClassIndex.ARRAY][MethodIndex.AREF] = RubyArray.AREF_SWITCHVALUE;
        table[ClassIndex.ARRAY][MethodIndex.ASET] = RubyArray.ASET_SWITCHVALUE;
        table[ClassIndex.ARRAY][MethodIndex.POP] = RubyArray.POP_SWITCHVALUE;
        table[ClassIndex.ARRAY][MethodIndex.PUSH] = RubyArray.PUSH_SWITCHVALUE;
        table[ClassIndex.ARRAY][MethodIndex.NIL_P] = RubyArray.NIL_P_SWITCHVALUE;
        table[ClassIndex.ARRAY][MethodIndex.EQUALEQUAL] = RubyArray.EQUALEQUAL_SWITCHVALUE;
        table[ClassIndex.ARRAY][MethodIndex.UNSHIFT] = RubyArray.UNSHIFT_SWITCHVALUE;
        table[ClassIndex.ARRAY][MethodIndex.OP_LSHIFT] = RubyArray.OP_LSHIFT_SWITCHVALUE;
        table[ClassIndex.ARRAY][MethodIndex.EMPTY_P] = RubyArray.EMPTY_P_SWITCHVALUE;
        
        // String
        table[ClassIndex.STRING] = new byte[MethodIndex.MAX_METHODS];
        table[ClassIndex.STRING][MethodIndex.OP_PLUS] = RubyString.OP_PLUS_SWITCHVALUE;
        table[ClassIndex.STRING][MethodIndex.OP_LT] = RubyString.OP_LT_SWITCHVALUE;
        table[ClassIndex.STRING][MethodIndex.AREF] = RubyString.AREF_SWITCHVALUE;
        table[ClassIndex.STRING][MethodIndex.ASET] = RubyString.ASET_SWITCHVALUE;
        table[ClassIndex.STRING][MethodIndex.NIL_P] = RubyString.NIL_P_SWITCHVALUE;
        table[ClassIndex.STRING][MethodIndex.EQUALEQUAL] = RubyString.EQUALEQUAL_SWITCHVALUE;
        table[ClassIndex.STRING][MethodIndex.OP_GE] = RubyString.OP_GE_SWITCHVALUE;
        table[ClassIndex.STRING][MethodIndex.OP_LSHIFT] = RubyString.OP_LSHIFT_SWITCHVALUE;
        table[ClassIndex.STRING][MethodIndex.EMPTY_P] = RubyString.EMPTY_P_SWITCHVALUE;
    }
    
}
