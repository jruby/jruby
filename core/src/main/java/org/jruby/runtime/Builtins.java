/*
 * Builtins.java - Fast-path builtin method validation flags
 *
 * Copyright (C) 2024 The JRuby Team
 *
 * This file implements a thread-local builtin method redefinition tracking
 * system compatible with CRuby's ruby_vm_redefined_flag approach, adapted
 * for JRuby's true parallelism model.
 *
 * Design:
 *   - CRuby uses a global array protected by GVL (single-threaded access)
 *   - JRuby uses thread-local short[] on ThreadContext (lock-free, no sync)
 *   - Each method slot holds a bitmask of which classes have redefined it
 *   - Fast-path checks are O(1): just read bit and mask
 *
 * Memory Layout:
 *   - builtinBits[methodIndex] & classFlag == 0 means NOT redefined (fast path OK)
 *   - builtinBits[methodIndex] & classFlag != 0 means redefined (use slow path)
 *
 * Performance:
 *   - 2 memory accesses vs 8 in the old isBuiltin() chain
 *   - Single cache line for all flags (short[31] = 62 bytes < 64 byte cache line)
 *   - Thread-local means no cache line bouncing between cores
 *
 * @see <a href="https://github.com/ruby/ruby/blob/master/vm_insnhelper.h">CRuby vm_insnhelper.h</a>
 * @see <a href="https://github.com/ruby/ruby/blob/master/vm_core.h">CRuby vm_core.h</a>
 * @see <a href="https://github.com/jruby/jruby/issues/9116">JRuby Issue #9116</a>
 * @see <a href="https://github.com/jruby/jruby/issues/9119">JRuby Issue #9119</a>
 *
 * @author Charles Oliver Nutter (headius) - original prototype
 * @author Troy Mallory (CufeHaco) - CRuby compatibility expansion
 */
package org.jruby.runtime;

import java.util.EnumMap;
import java.util.HashMap;

public class Builtins {

    // =========================================================================
    // CLASS FLAGS (bit positions)
    // Matches CRuby's *_REDEFINED_OP_FLAG defines in vm_insnhelper.h
    // These are bitmasks - each class gets one bit position
    // =========================================================================

    /** Integer - CRuby: INTEGER_REDEFINED_OP_FLAG */
    public static final int INTEGER     = 1 << 0;

    /** Float - CRuby: FLOAT_REDEFINED_OP_FLAG */
    public static final int FLOAT       = 1 << 1;

    /** String - CRuby: STRING_REDEFINED_OP_FLAG */
    public static final int STRING      = 1 << 2;

    /** Array - CRuby: ARRAY_REDEFINED_OP_FLAG */
    public static final int ARRAY       = 1 << 3;

    /** Hash - CRuby: HASH_REDEFINED_OP_FLAG */
    public static final int HASH        = 1 << 4;

    /** Bignum (large integers) - CRuby: BIGNUM_REDEFINED_OP_FLAG */
    public static final int BIGNUM      = 1 << 5;

    /** Symbol - CRuby: SYMBOL_REDEFINED_OP_FLAG */
    public static final int SYMBOL      = 1 << 6;

    /** Time - CRuby: TIME_REDEFINED_OP_FLAG */
    public static final int TIME        = 1 << 7;

    /** Regexp - CRuby: REGEXP_REDEFINED_OP_FLAG */
    public static final int REGEXP      = 1 << 8;

    /** NilClass - CRuby: NIL_REDEFINED_OP_FLAG */
    public static final int NIL         = 1 << 9;

    /** TrueClass - CRuby: TRUE_REDEFINED_OP_FLAG */
    public static final int TRUE        = 1 << 10;

    /** FalseClass - CRuby: FALSE_REDEFINED_OP_FLAG */
    public static final int FALSE       = 1 << 11;

    /** Proc - CRuby: PROC_REDEFINED_OP_FLAG */
    public static final int PROC        = 1 << 12;

    /** Range - CRuby: RANGE_REDEFINED_OP_FLAG (added Ruby 3.1.2+) */
    public static final int RANGE       = 1 << 13;

    /** Struct - New in JRuby */
    public static final int STRUCT      = 1 << 14;

    /** Rational - New in JRuby */
    public static final int RATIONAL    = 1 << 15;

    // =========================================================================
    // METHOD OFFSETS (array indices)
    // Matches CRuby's BOP_* enum in vm_insnhelper.h (bop_type)
    // These are array indices into builtinBits[]
    // =========================================================================

    /** + operator */
    public static final int BOP_PLUS        = 0;

    /** - operator */
    public static final int BOP_MINUS       = 1;

    /** * operator */
    public static final int BOP_MULT        = 2;

    /** / operator */
    public static final int BOP_DIV         = 3;

    /** % operator (modulo) */
    public static final int BOP_MOD         = 4;

    /** == operator */
    public static final int BOP_EQ          = 5;

    /** === operator (case equality) */
    public static final int BOP_EQQ         = 6;

    /** < operator */
    public static final int BOP_LT          = 7;

    /** <= operator */
    public static final int BOP_LE          = 8;

    /** << operator (left shift / append) */
    public static final int BOP_LTLT        = 9;

    /** [] operator (element reference) */
    public static final int BOP_AREF        = 10;

    /** []= operator (element assignment) */
    public static final int BOP_ASET        = 11;

    /** length method */
    public static final int BOP_LENGTH      = 12;

    /** size method */
    public static final int BOP_SIZE        = 13;

    /** empty? method */
    public static final int BOP_EMPTY_P     = 14;

    /** nil? method */
    public static final int BOP_NIL_P       = 15;

    /** succ method */
    public static final int BOP_SUCC        = 16;

    /** > operator */
    public static final int BOP_GT          = 17;

    /** >= operator */
    public static final int BOP_GE          = 18;

    /** >> operator */
    public static final int BOP_GTGT        = 19;

    /** ! operator (logical not) */
    public static final int BOP_NOT         = 20;

    /** != operator */
    public static final int BOP_NEQ         = 21;

    /** =~ operator (pattern match) */
    public static final int BOP_MATCH       = 22;

    /** freeze method */
    public static final int BOP_FREEZE      = 23;

    /** -@ operator (unary minus) */
    public static final int BOP_UMINUS      = 24;

    /** max method */
    public static final int BOP_MAX         = 25;

    /** min method */
    public static final int BOP_MIN         = 26;

    /** hash method */
    public static final int BOP_HASH        = 27;

    /** call method (Proc#call) */
    public static final int BOP_CALL        = 28;

    /** & operator (bitwise and / to_proc) */
    public static final int BOP_AND         = 29;

    /** | operator (bitwise or) */
    public static final int BOP_OR          = 30;

    /** | operator (bitwise or) */
    public static final int BOP_CMP         = 31;

    /** | operator (bitwise or) */
    public static final int BOP_DEFAULT     = 32;

    /** pack method (Array#pack) */
    public static final int BOP_PACK        = 33;

    /** include? method */
    public static final int BOP_INCLUDE_P   = 34;

    // -------------------------------------------------------------------------
    // JRuby Extensions (not in CRuby BOP enum)
    // -------------------------------------------------------------------------

    /** to_f method - JRuby extension for float conversion */
    public static final int BOP_TO_F        = 35;

    /** dig method - JRuby extension for Array/Hash/etc #dig optimization */
    public static final int BOP_DIG         = 36;

    /** Sentinel value - array size. Matches CRuby's BOP_LAST_ naming convention */
    public static final int BOP_LAST_       = 37;

    // =========================================================================
    // CLASS INDEX TO FLAG MAPPING
    // Maps JRuby's ClassIndex enum to our bit flags
    // =========================================================================

    public static final EnumMap<ClassIndex, Integer> CLASS_FLAGS = new EnumMap<>(ClassIndex.class);

    // =========================================================================
    // METHOD NAME TO INDEX MAPPING
    // Maps Ruby method names to BOP indices
    // =========================================================================

    public static final HashMap<String, Integer> METHOD_IDS = new HashMap<>();

    // =========================================================================
    // STATIC INITIALIZATION
    // =========================================================================

    static {
        // ---------------------------------------------------------------------
        // Class mappings - map ClassIndex to bit flags
        // ---------------------------------------------------------------------
        CLASS_FLAGS.put(ClassIndex.INTEGER, INTEGER);
        CLASS_FLAGS.put(ClassIndex.BIGNUM, BIGNUM);
        CLASS_FLAGS.put(ClassIndex.FLOAT, FLOAT);
        CLASS_FLAGS.put(ClassIndex.STRING, STRING);
        CLASS_FLAGS.put(ClassIndex.ARRAY, ARRAY);
        CLASS_FLAGS.put(ClassIndex.HASH, HASH);
        CLASS_FLAGS.put(ClassIndex.SYMBOL, SYMBOL);
        CLASS_FLAGS.put(ClassIndex.TIME, TIME);
        CLASS_FLAGS.put(ClassIndex.REGEXP, REGEXP);
        CLASS_FLAGS.put(ClassIndex.NIL, NIL);
        CLASS_FLAGS.put(ClassIndex.TRUE, TRUE);
        CLASS_FLAGS.put(ClassIndex.FALSE, FALSE);
        CLASS_FLAGS.put(ClassIndex.PROC, PROC);
        CLASS_FLAGS.put(ClassIndex.RANGE, RANGE);
        CLASS_FLAGS.put(ClassIndex.STRUCT, STRUCT);

        // ---------------------------------------------------------------------
        // Method mappings - map method names to BOP indices
        // Core operators (CRuby BOP enum)
        // ---------------------------------------------------------------------
        METHOD_IDS.put("+", BOP_PLUS);
        METHOD_IDS.put("-", BOP_MINUS);
        METHOD_IDS.put("*", BOP_MULT);
        METHOD_IDS.put("/", BOP_DIV);
        METHOD_IDS.put("%", BOP_MOD);
        METHOD_IDS.put("==", BOP_EQ);
        METHOD_IDS.put("===", BOP_EQQ);
        METHOD_IDS.put("<", BOP_LT);
        METHOD_IDS.put("<=", BOP_LE);
        METHOD_IDS.put("<<", BOP_LTLT);
        METHOD_IDS.put("[]", BOP_AREF);
        METHOD_IDS.put("[]=", BOP_ASET);
        METHOD_IDS.put("length", BOP_LENGTH);
        METHOD_IDS.put("size", BOP_SIZE);
        METHOD_IDS.put("empty?", BOP_EMPTY_P);
        METHOD_IDS.put("succ", BOP_SUCC);
        METHOD_IDS.put(">", BOP_GT);
        METHOD_IDS.put(">=", BOP_GE);
        METHOD_IDS.put("!", BOP_NOT);
        METHOD_IDS.put("!=", BOP_NEQ);
        METHOD_IDS.put("=~", BOP_MATCH);
        METHOD_IDS.put("freeze", BOP_FREEZE);
        METHOD_IDS.put("-@", BOP_UMINUS);
        METHOD_IDS.put("max", BOP_MAX);
        METHOD_IDS.put("min", BOP_MIN);
        METHOD_IDS.put("hash", BOP_HASH);
        METHOD_IDS.put("call", BOP_CALL);
        METHOD_IDS.put("&", BOP_AND);
        METHOD_IDS.put("|", BOP_OR);
        METHOD_IDS.put("pack", BOP_PACK);
        METHOD_IDS.put("include?", BOP_INCLUDE_P);
        METHOD_IDS.put("cover?", BOP_INCLUDE_P);  // Same optimization as include?
        METHOD_IDS.put("to_f", BOP_TO_F);
        METHOD_IDS.put("dig", BOP_DIG);

        // Aliases that map to existing BOPs
        METHOD_IDS.put("eql?", BOP_EQ);         // Often same optimization as ==
        METHOD_IDS.put("+@", BOP_PLUS);         // Unary plus (rare but exists)
    }

    // =========================================================================
    // ALLOCATION
    // =========================================================================

    /**
     * Allocate a new builtin bits array for a ThreadContext.
     * All bits start as 0 (no redefinitions detected).
     *
     * @return new short array sized for all BOP indices
     */
    public static short[] allocate() {
        return new short[BOP_LAST_];
    }

    // =========================================================================
    // INVALIDATION
    // Called when a method is defined/redefined on a tracked class
    // =========================================================================

    /**
     * Mark a builtin method as redefined for a specific class.
     * After this call, fast-path checks for this method+class will return false.
     *
     * @param bits the builtinBits array to modify (from Ruby runtime)
     * @param classIndex the ClassIndex of the class being modified
     * @param method the method name being defined
     */
    public static void invalidateBuiltin(short[] bits, ClassIndex classIndex, String method) {
        Integer classFlag = CLASS_FLAGS.get(classIndex);
        Integer methodId = METHOD_IDS.get(method);

        if (classFlag != null && methodId != null) {
            bits[methodId] |= classFlag;
        }
    }

    /**
     * Reset all builtin flags (for testing purposes).
     * After this call, all fast-path checks will return true.
     *
     * @param bits the builtinBits array to reset
     */
    public static void resetAll(short[] bits) {
        for (int i = 0; i < bits.length; i++) {
            bits[i] = 0;
        }
    }

    // =========================================================================
    // FAST-PATH CHECK METHODS
    // These are designed to be inlined by the JIT compiler
    // Return true if fast path is OK (method NOT redefined)
    // Return false if slow path needed (method WAS redefined)
    // =========================================================================

    // -------------------------------------------------------------------------
    // Integer Operations
    // -------------------------------------------------------------------------

    /** Check if Integer#+ is still builtin */
    public static boolean checkIntegerPlus(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_PLUS] & INTEGER) == 0;
    }

    /** Check if Integer#- is still builtin */
    public static boolean checkIntegerMinus(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_MINUS] & INTEGER) == 0;
    }

    /** Check if Integer#* is still builtin */
    public static boolean checkIntegerMult(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_MULT] & INTEGER) == 0;
    }

    /** Check if Integer#/ is still builtin */
    public static boolean checkIntegerDiv(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_DIV] & INTEGER) == 0;
    }

    /** Check if Integer#% is still builtin */
    public static boolean checkIntegerMod(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_MOD] & INTEGER) == 0;
    }

    /** Check if Integer#== is still builtin */
    public static boolean checkIntegerEquals(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_EQ] & INTEGER) == 0;
    }

    /** Check if Integer#< is still builtin */
    public static boolean checkIntegerLt(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_LT] & INTEGER) == 0;
    }

    /** Check if Integer#<= is still builtin */
    public static boolean checkIntegerLe(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_LE] & INTEGER) == 0;
    }

    /** Check if Integer#> is still builtin */
    public static boolean checkIntegerGt(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_GT] & INTEGER) == 0;
    }

    /** Check if Integer#>= is still builtin */
    public static boolean checkIntegerGe(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_GE] & INTEGER) == 0;
    }

    /** Check if all Integer comparison operators are still builtin */
    public static boolean checkIntegerCompare(ThreadContext ctx) {
        short[] bits = ctx.builtinBits;
        return (bits[BOP_LT] & INTEGER) == 0 &&
               (bits[BOP_LE] & INTEGER) == 0 &&
               (bits[BOP_GT] & INTEGER) == 0 &&
               (bits[BOP_GE] & INTEGER) == 0;
    }

    /** Check if Integer#<< is still builtin */
    public static boolean checkIntegerLshift(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_LTLT] & INTEGER) == 0;
    }

    /** Check if Integer#& is still builtin */
    public static boolean checkIntegerAnd(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_AND] & INTEGER) == 0;
    }

    /** Check if Integer#| is still builtin */
    public static boolean checkIntegerOr(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_OR] & INTEGER) == 0;
    }

    /** Check if Integer#-@ is still builtin */
    public static boolean checkIntegerUminus(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_UMINUS] & INTEGER) == 0;
    }

    /** Check if Integer#&lt;=&gt; is still builtin */
    public static boolean checkIntegerCmp(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_CMP] & INTEGER) == 0;
    }

    /** Check if Integer#to_f is still builtin */
    public static boolean checkIntegerToF(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_TO_F] & INTEGER) == 0;
    }

    // -------------------------------------------------------------------------
    // Float Operations
    // -------------------------------------------------------------------------

    /** Check if Float#+ is still builtin */
    public static boolean checkFloatPlus(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_PLUS] & FLOAT) == 0;
    }

    /** Check if Float#- is still builtin */
    public static boolean checkFloatMinus(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_MINUS] & FLOAT) == 0;
    }

    /** Check if Float#* is still builtin */
    public static boolean checkFloatMult(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_MULT] & FLOAT) == 0;
    }

    /** Check if Float#/ is still builtin */
    public static boolean checkFloatDiv(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_DIV] & FLOAT) == 0;
    }

    /** Check if Float#== is still builtin */
    public static boolean checkFloatEquals(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_EQ] & FLOAT) == 0;
    }

    /** Check if Float comparisons are still builtin */
    public static boolean checkFloatCompare(ThreadContext ctx) {
        short[] bits = ctx.builtinBits;
        return (bits[BOP_LT] & FLOAT) == 0 &&
               (bits[BOP_LE] & FLOAT) == 0 &&
               (bits[BOP_GT] & FLOAT) == 0 &&
               (bits[BOP_GE] & FLOAT) == 0;
    }

    // -------------------------------------------------------------------------
    // String Operations
    // -------------------------------------------------------------------------

    /** Check if String#+ is still builtin */
    public static boolean checkStringPlus(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_PLUS] & STRING) == 0;
    }

    /** Check if String#== is still builtin */
    public static boolean checkStringEquals(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_EQ] & STRING) == 0;
    }

    /** Check if String#<< is still builtin */
    public static boolean checkStringConcat(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_LTLT] & STRING) == 0;
    }

    /** Check if String#[] is still builtin */
    public static boolean checkStringAref(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_AREF] & STRING) == 0;
    }

    /** Check if String#length/size is still builtin */
    public static boolean checkStringLength(ThreadContext ctx) {
        short[] bits = ctx.builtinBits;
        return (bits[BOP_LENGTH] & STRING) == 0 &&
               (bits[BOP_SIZE] & STRING) == 0;
    }

    /** Check if String#empty? is still builtin */
    public static boolean checkStringEmpty(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_EMPTY_P] & STRING) == 0;
    }

    /** Check if String#freeze is still builtin */
    public static boolean checkStringFreeze(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_FREEZE] & STRING) == 0;
    }

    /** Check if String#=~ is still builtin */
    public static boolean checkStringMatch(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_MATCH] & STRING) == 0;
    }

    /** Check if String#hash is still builtin */
    public static boolean checkStringHash(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_HASH] & STRING) == 0;
    }

    /** Check if String#&lt;=&gt; is still builtin */
    public static boolean checkStringCmp(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_CMP] & STRING) == 0;
    }

    // -------------------------------------------------------------------------
    // Array Operations
    // -------------------------------------------------------------------------

    /** Check if Array#[] is still builtin */
    public static boolean checkArrayAref(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_AREF] & ARRAY) == 0;
    }

    /** Check if Array#[]= is still builtin */
    public static boolean checkArrayAset(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_ASET] & ARRAY) == 0;
    }

    /** Check if Array#<< is still builtin */
    public static boolean checkArrayPush(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_LTLT] & ARRAY) == 0;
    }

    /** Check if Array#length/size is still builtin */
    public static boolean checkArrayLength(ThreadContext ctx) {
        short[] bits = ctx.builtinBits;
        return (bits[BOP_LENGTH] & ARRAY) == 0 &&
               (bits[BOP_SIZE] & ARRAY) == 0;
    }

    /** Check if Array#empty? is still builtin */
    public static boolean checkArrayEmpty(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_EMPTY_P] & ARRAY) == 0;
    }

    /** Check if Array#max is still builtin */
    public static boolean checkArrayMax(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_MAX] & ARRAY) == 0;
    }

    /** Check if Array#min is still builtin */
    public static boolean checkArrayMin(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_MIN] & ARRAY) == 0;
    }

    /** Check if Array#hash is still builtin */
    public static boolean checkArrayHash(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_HASH] & ARRAY) == 0;
    }

    /** Check if Array#pack is still builtin */
    public static boolean checkArrayPack(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_PACK] & ARRAY) == 0;
    }

    /** Check if Array#dig is still builtin */
    public static boolean checkArrayDig(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_DIG] & ARRAY) == 0;
    }

    // -------------------------------------------------------------------------
    // Hash Operations
    // -------------------------------------------------------------------------

    /** Check if Hash#[] is still builtin */
    public static boolean checkHashAref(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_AREF] & HASH) == 0;
    }

    /** Check if Hash#[]= is still builtin */
    public static boolean checkHashAset(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_ASET] & HASH) == 0;
    }

    /** Check if Hash#length/size is still builtin */
    public static boolean checkHashLength(ThreadContext ctx) {
        short[] bits = ctx.builtinBits;
        return (bits[BOP_LENGTH] & HASH) == 0 &&
               (bits[BOP_SIZE] & HASH) == 0;
    }

    /** Check if Hash#empty? is still builtin */
    public static boolean checkHashEmpty(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_EMPTY_P] & HASH) == 0;
    }

    /** Check if Hash#default is still builtin */
    public static boolean checkHashDefault(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_DEFAULT] & HASH) == 0;
    }

    /** Check if Hash#dig is still builtin */
    public static boolean checkHashDig(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_DIG] & HASH) == 0;
    }

    // -------------------------------------------------------------------------
    // Range Operations (Key for #9116!)
    // -------------------------------------------------------------------------

    /** Check if Range#include? is still builtin */
    public static boolean checkRangeInclude(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_INCLUDE_P] & RANGE) == 0;
    }

    /** Check if Range#cover? is still builtin (same as include? check) */
    public static boolean checkRangeCover(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_INCLUDE_P] & RANGE) == 0;
    }

    /** Check if Range#=== is still builtin */
    public static boolean checkRangeEqq(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_EQQ] & RANGE) == 0;
    }

    /** Check if Range#min is still builtin */
    public static boolean checkRangeMin(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_MIN] & RANGE) == 0;
    }

    /** Check if Range#max is still builtin */
    public static boolean checkRangeMax(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_MAX] & RANGE) == 0;
    }

    // -------------------------------------------------------------------------
    // Symbol Operations
    // -------------------------------------------------------------------------

    /** Check if Symbol#== is still builtin */
    public static boolean checkSymbolEquals(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_EQ] & SYMBOL) == 0;
    }

    // -------------------------------------------------------------------------
    // Proc Operations
    // -------------------------------------------------------------------------

    /** Check if Proc#call is still builtin */
    public static boolean checkProcCall(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_CALL] & PROC) == 0;
    }

    // -------------------------------------------------------------------------
    // NilClass/TrueClass/FalseClass Operations
    // -------------------------------------------------------------------------

    /** Check if NilClass#! is still builtin */
    public static boolean checkNilNot(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_NOT] & NIL) == 0;
    }

    /** Check if TrueClass#! is still builtin */
    public static boolean checkTrueNot(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_NOT] & TRUE) == 0;
    }

    /** Check if FalseClass#! is still builtin */
    public static boolean checkFalseNot(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_NOT] & FALSE) == 0;
    }

    // -------------------------------------------------------------------------
    // Regexp Operations
    // -------------------------------------------------------------------------

    /** Check if Regexp#=~ is still builtin */
    public static boolean checkRegexpMatch(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_MATCH] & REGEXP) == 0;
    }

    /** Check if Regexp#=== is still builtin */
    public static boolean checkRegexpEqq(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_EQQ] & REGEXP) == 0;
    }

    // -------------------------------------------------------------------------
    // Time Operations
    // -------------------------------------------------------------------------

    /** Check if Time#+ is still builtin */
    public static boolean checkTimePlus(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_PLUS] & TIME) == 0;
    }

    /** Check if Time#- is still builtin */
    public static boolean checkTimeMinus(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_MINUS] & TIME) == 0;
    }

    /** Check if Time comparisons are still builtin */
    public static boolean checkTimeCompare(ThreadContext ctx) {
        short[] bits = ctx.builtinBits;
        return (bits[BOP_LT] & TIME) == 0 &&
               (bits[BOP_LE] & TIME) == 0 &&
               (bits[BOP_GT] & TIME) == 0 &&
               (bits[BOP_GE] & TIME) == 0;
    }

    /** Check if Time#&lt;=&gt; is still builtin */
    public static boolean checkTimeCmp(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_CMP] & TIME) == 0;
    }

    // -------------------------------------------------------------------------
    // Struct Operations
    // -------------------------------------------------------------------------

    /** Check if Struct#dig is still builtin */
    public static boolean checkStructDig(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_DIG] & STRUCT) == 0;
    }

    // -------------------------------------------------------------------------
    // Rational Operations
    // -------------------------------------------------------------------------

    /** Check if Rational#to_f is still builtin */
    public static boolean checkRationalToF(ThreadContext ctx) {
        return (ctx.builtinBits[BOP_TO_F] & RATIONAL) == 0;
    }
}
