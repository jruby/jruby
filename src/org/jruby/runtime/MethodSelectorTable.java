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
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.RubySymbol;

/**
 *
 * @author headius 
 */
public class MethodSelectorTable {
    public final byte[][] table = new byte[ClassIndex.MAX_CLASSES][MethodIndex.MAX_METHODS];
    
    public void init() {
        // Fixnum
        table[ClassIndex.FIXNUM] = new byte[MethodIndex.MAX_METHODS];
        table[ClassIndex.FIXNUM][MethodIndex.OP_PLUS] = RubyFixnum.OP_PLUS_SWITCHVALUE; 
        table[ClassIndex.FIXNUM][MethodIndex.OP_MINUS] = RubyFixnum.OP_MINUS_SWITCHVALUE; 
        table[ClassIndex.FIXNUM][MethodIndex.OP_LT] = RubyFixnum.OP_LT_SWITCHVALUE;
        table[ClassIndex.FIXNUM][MethodIndex.TO_S] = RubyFixnum.TO_S_SWITCHVALUE;
        table[ClassIndex.FIXNUM][MethodIndex.TO_I] = RubyFixnum.TO_I_SWITCHVALUE;
        table[ClassIndex.FIXNUM][MethodIndex.TO_INT] = RubyFixnum.TO_INT_SWITCHVALUE;
        table[ClassIndex.FIXNUM][MethodIndex.HASH] = RubyFixnum.HASH_SWITCHVALUE;
        table[ClassIndex.FIXNUM][MethodIndex.OP_GT] = RubyFixnum.OP_GT_SWITCHVALUE;
        table[ClassIndex.FIXNUM][MethodIndex.OP_TIMES] = RubyFixnum.OP_TIMES_SWITCHVALUE;
        table[ClassIndex.FIXNUM][MethodIndex.OP_LE] = RubyFixnum.OP_LE_SWITCHVALUE;
        table[ClassIndex.FIXNUM][MethodIndex.OP_SPACESHIP] = RubyFixnum.OP_SPACESHIP_SWITCHVALUE;
        table[ClassIndex.FIXNUM][MethodIndex.INSPECT] = RubyFixnum.INSPECT_SWITCHVALUE;
        
        // Bignum
        table[ClassIndex.BIGNUM] = new byte[MethodIndex.MAX_METHODS];
        table[ClassIndex.BIGNUM][MethodIndex.OP_PLUS] = RubyBignum.OP_PLUS_SWITCHVALUE; 
        table[ClassIndex.BIGNUM][MethodIndex.OP_MINUS] = RubyBignum.OP_MINUS_SWITCHVALUE; 
        table[ClassIndex.BIGNUM][MethodIndex.OP_LT] = RubyBignum.OP_LT_SWITCHVALUE;
        table[ClassIndex.BIGNUM][MethodIndex.TO_S] = RubyBignum.TO_S_SWITCHVALUE;
        table[ClassIndex.BIGNUM][MethodIndex.TO_I] = RubyBignum.TO_I_SWITCHVALUE;
        table[ClassIndex.BIGNUM][MethodIndex.HASH] = RubyBignum.HASH_SWITCHVALUE;
        table[ClassIndex.BIGNUM][MethodIndex.EQUALEQUAL] = RubyBignum.EQUALEQUAL_SWITCHVALUE;
        table[ClassIndex.BIGNUM][MethodIndex.OP_SPACESHIP] = RubyBignum.OP_SPACESHIP_SWITCHVALUE;
        table[ClassIndex.BIGNUM][MethodIndex.INSPECT] = RubyBignum.INSPECT_SWITCHVALUE;
        
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
        table[ClassIndex.ARRAY][MethodIndex.TO_S] = RubyArray.TO_S_SWITCHVALUE;
        table[ClassIndex.ARRAY][MethodIndex.AT] = RubyArray.AT_SWITCHVALUE;
        table[ClassIndex.ARRAY][MethodIndex.TO_ARY] = RubyArray.TO_ARY_SWITCHVALUE;
        table[ClassIndex.ARRAY][MethodIndex.TO_A] = RubyArray.TO_A_SWITCHVALUE;
        table[ClassIndex.ARRAY][MethodIndex.HASH] = RubyArray.HASH_SWITCHVALUE;
        table[ClassIndex.ARRAY][MethodIndex.LENGTH] = RubyArray.LENGTH_SWITCHVALUE;
        table[ClassIndex.ARRAY][MethodIndex.LAST] = RubyArray.LAST_SWITCHVALUE;
        table[ClassIndex.ARRAY][MethodIndex.SHIFT] = RubyArray.SHIFT_SWITCHVALUE;
        table[ClassIndex.ARRAY][MethodIndex.OP_SPACESHIP] = RubyArray.OP_SPACESHIP_SWITCHVALUE;
        table[ClassIndex.ARRAY][MethodIndex.INSPECT] = RubyArray.INSPECT_SWITCHVALUE;
        
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
        table[ClassIndex.STRING][MethodIndex.TO_S] = RubyString.TO_S_SWITCHVALUE;
        table[ClassIndex.STRING][MethodIndex.TO_I] = RubyString.TO_I_SWITCHVALUE;
        table[ClassIndex.STRING][MethodIndex.TO_STR] = RubyString.TO_STR_SWITCHVALUE;
        table[ClassIndex.STRING][MethodIndex.TO_SYM] = RubyString.TO_SYM_SWITCHVALUE;
        table[ClassIndex.STRING][MethodIndex.HASH] = RubyString.HASH_SWITCHVALUE;
        table[ClassIndex.STRING][MethodIndex.OP_GT] = RubyString.OP_GT_SWITCHVALUE;
        table[ClassIndex.STRING][MethodIndex.OP_TIMES] = RubyString.OP_TIMES_SWITCHVALUE;
        table[ClassIndex.STRING][MethodIndex.OP_LE] = RubyString.OP_LE_SWITCHVALUE;
        table[ClassIndex.STRING][MethodIndex.LENGTH] = RubyString.LENGTH_SWITCHVALUE;
        table[ClassIndex.STRING][MethodIndex.OP_MATCH] = RubyString.MATCH_SWITCHVALUE;
        table[ClassIndex.STRING][MethodIndex.OP_EQQ] = RubyString.EQQ_SWITCHVALUE;
        table[ClassIndex.STRING][MethodIndex.OP_SPACESHIP] = RubyString.OP_SPACESHIP_SWITCHVALUE;
        table[ClassIndex.STRING][MethodIndex.INSPECT] = RubyString.INSPECT_SWITCHVALUE;

        // Symbol
        table[ClassIndex.SYMBOL] = new byte[MethodIndex.MAX_METHODS];
        table[ClassIndex.SYMBOL][MethodIndex.NIL_P] = RubySymbol.NIL_P_SWITCHVALUE;
        table[ClassIndex.SYMBOL][MethodIndex.EQUALEQUAL] = RubySymbol.EQUALEQUAL_SWITCHVALUE;
        table[ClassIndex.SYMBOL][MethodIndex.TO_S] = RubySymbol.TO_S_SWITCHVALUE;
        table[ClassIndex.SYMBOL][MethodIndex.TO_I] = RubySymbol.TO_I_SWITCHVALUE;
        table[ClassIndex.SYMBOL][MethodIndex.TO_SYM] = RubySymbol.TO_SYM_SWITCHVALUE;
        table[ClassIndex.SYMBOL][MethodIndex.HASH] = RubySymbol.HASH_SWITCHVALUE;
        table[ClassIndex.SYMBOL][MethodIndex.INSPECT] = RubySymbol.INSPECT_SWITCHVALUE;

        // Regexp
        table[ClassIndex.REGEXP] = new byte[MethodIndex.MAX_METHODS];
        table[ClassIndex.REGEXP][MethodIndex.EQUALEQUAL] = RubyRegexp.EQUALEQUAL_SWITCHVALUE;
        table[ClassIndex.REGEXP][MethodIndex.NIL_P] = RubyRegexp.NIL_P_SWITCHVALUE;
        table[ClassIndex.REGEXP][MethodIndex.TO_S] = RubyRegexp.TO_S_SWITCHVALUE;
        table[ClassIndex.REGEXP][MethodIndex.HASH] = RubyRegexp.HASH_SWITCHVALUE;
        table[ClassIndex.REGEXP][MethodIndex.OP_MATCH] = RubyRegexp.MATCH_SWITCHVALUE;
        table[ClassIndex.REGEXP][MethodIndex.OP_EQQ] = RubyRegexp.EQQ_SWITCHVALUE;
        table[ClassIndex.REGEXP][MethodIndex.INSPECT] = RubyRegexp.INSPECT_SWITCHVALUE;

        // Hash
        table[ClassIndex.HASH] = new byte[MethodIndex.MAX_METHODS];
        table[ClassIndex.HASH][MethodIndex.AREF] = RubyHash.AREF_SWITCHVALUE;
        table[ClassIndex.HASH][MethodIndex.ASET] = RubyHash.ASET_SWITCHVALUE;
        table[ClassIndex.HASH][MethodIndex.DEFAULT] = RubyHash.DEFAULT_SWITCHVALUE;
        table[ClassIndex.HASH][MethodIndex.NIL_P] = RubyHash.NIL_P_SWITCHVALUE;
        table[ClassIndex.HASH][MethodIndex.EQUALEQUAL] = RubyHash.EQUALEQUAL_SWITCHVALUE;
        table[ClassIndex.HASH][MethodIndex.EMPTY_P] = RubyHash.EMPTY_P_SWITCHVALUE;
        table[ClassIndex.HASH][MethodIndex.TO_S] = RubyHash.TO_S_SWITCHVALUE;
        table[ClassIndex.HASH][MethodIndex.TO_A] = RubyHash.TO_A_SWITCHVALUE;
        table[ClassIndex.HASH][MethodIndex.HASH] = RubyHash.HASH_SWITCHVALUE;
        table[ClassIndex.HASH][MethodIndex.LENGTH] = RubyHash.LENGTH_SWITCHVALUE;
        table[ClassIndex.HASH][MethodIndex.TO_HASH] = RubyHash.TO_HASH_SWITCHVALUE;
        table[ClassIndex.HASH][MethodIndex.EQL_P] = RubyHash.EQL_P_SWITCHVALUE;
        table[ClassIndex.HASH][MethodIndex.INSPECT] = RubyHash.INSPECT_SWITCHVALUE;

        // Module
        table[ClassIndex.MODULE] = new byte[MethodIndex.MAX_METHODS];
        table[ClassIndex.MODULE][MethodIndex.OP_EQQ] = RubyModule.EQQ_SWITCHVALUE;
        table[ClassIndex.MODULE][MethodIndex.INSPECT] = RubyModule.INSPECT_SWITCHVALUE;

        // Class
        table[ClassIndex.CLASS] = new byte[MethodIndex.MAX_METHODS];
        table[ClassIndex.CLASS][MethodIndex.OP_EQQ] = RubyClass.EQQ_SWITCHVALUE;
        table[ClassIndex.CLASS][MethodIndex.INSPECT] = RubyClass.INSPECT_SWITCHVALUE;
    }
}
