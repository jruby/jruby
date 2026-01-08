package org.jruby.runtime;

import org.jruby.util.collections.HashMapInt;

import java.lang.invoke.SwitchPoint;
import java.util.EnumMap;
import java.util.HashMap;

public class Builtins {
    // bits for class
    public static final int INTEGER_CLASS =   0b00000000000001;
    public static final int FLOAT_CLASS =     0b00000000000010;
    public static final int STRING_CLASS =    0b00000000000100;
    public static final int ARRAY_CLASS =     0b00000000001000;
    public static final int HASH_CLASS =      0b00000000010000;
    public static final int SYMBOL_CLASS =    0b00000000100000;
    public static final int TIME_CLASS =      0b00000001000000;
    public static final int REGEXP_CLASS =    0b00000010000000;
    public static final int NIL_CLASS =       0b00000100000000;
    public static final int TRUE_CLASS =      0b00001000000000;
    public static final int FALSE_CLASS =     0b00010000000000;
    public static final int PROC_CLASS =      0b00100000000000;
    public static final int RATIONAL_CLASS =  0b01000000000000;
    public static final int STRUCT_CLASS =    0b10000000000000;

    // offsets for method
    public static final int PLUS_METHOD = 0;
    public static final int MINUS_METHOD = 1;
    public static final int MULT_METHOD = 2;
    public static final int DIV_METHOD = 3;
    public static final int MOD_METHOD = 4;
    public static final int EQ_METHOD = 5;
    public static final int EQQ_METHOD = 6;
    public static final int LT_METHOD = 7;
    public static final int LE_METHOD = 8;
    public static final int LTLT_METHOD = 9;
    public static final int AREF_METHOD = 10;
    public static final int ASET_METHOD = 11;
    public static final int LENGTH_METHOD = 12;
    public static final int SIZE_METHOD = 13;
    public static final int EMPTY_P_METHOD = 14;
    public static final int NIL_P_METHOD = 15;
    public static final int SUCC_METHOD = 16;
    public static final int GT_METHOD = 17;
    public static final int GE_METHOD = 18;
    public static final int GTGT_METHOD = 19;
    public static final int NOT_METHOD = 20;
    public static final int NEQ_METHOD = 21;
    public static final int MATCH_METHOD = 22;
    public static final int FREEZE_METHOD = 23;
    public static final int UMINUS_METHOD = 24;
    public static final int MAX_METHOD = 25;
    public static final int MIN_METHOD = 26;
    public static final int HASH_METHOD = 27;
    public static final int CALL_METHOD = 28;
    public static final int AND_METHOD = 29;
    public static final int OR_METHOD = 30;
    public static final int CMP_METHOD = 31;
    public static final int DEFAULT_METHOD = 32;
    public static final int PACK_METHOD = 33;
    public static final int INCLUDE_P_METHOD = 34;
    public static final int TO_F_METHOD = 35;
    public static final int DIG_METHOD = 36;
    private static final int _LAST_METHOD = 37;

    public static final EnumMap<ClassIndex, Integer> classIds = new EnumMap<>(ClassIndex.class);
    public static final HashMapInt<String> methodIds = new HashMapInt<>();

    static {
        classIds.put(ClassIndex.INTEGER, INTEGER_CLASS);

        methodIds.put("==", EQ_METHOD);
    }

    public static short[] allocate() {
        return new short[_LAST_METHOD];
    }

    public static boolean check(ThreadContext context, int klass, int method) {
        return (context.builtinBits[method] & klass) != 0;
    }

    public static boolean checkIntegerPlus(ThreadContext context) {
        return check(context, INTEGER_CLASS, PLUS_METHOD);
    }

    public static boolean checkIntegerMinus(ThreadContext context) {
        return check(context, INTEGER_CLASS, MINUS_METHOD);
    }

    public static boolean checkIntegerMult(ThreadContext context) {
        return check(context, INTEGER_CLASS, MULT_METHOD);
    }

    public static boolean checkIntegerDiv(ThreadContext context) {
        return check(context, INTEGER_CLASS, DIV_METHOD);
    }

    public static boolean checkIntegerMod(ThreadContext context) {
        return check(context, INTEGER_CLASS, MOD_METHOD);
    }

    public static boolean checkIntegerEquals(ThreadContext context) {
        return check(context, INTEGER_CLASS, EQ_METHOD);
    }

    public static boolean checkIntegerEqq(ThreadContext context) {
        return check(context, INTEGER_CLASS, EQQ_METHOD);
    }

    public static boolean checkIntegerLt(ThreadContext context) {
        return check(context, INTEGER_CLASS, LT_METHOD);
    }

    public static boolean checkIntegerLe(ThreadContext context) {
        return check(context, INTEGER_CLASS, LE_METHOD);
    }

    public static boolean checkIntegerLtlt(ThreadContext context) {
        return check(context, INTEGER_CLASS, LTLT_METHOD);
    }

    public static boolean checkIntegerAref(ThreadContext context) {
        return check(context, INTEGER_CLASS, AREF_METHOD);
    }

    public static boolean checkIntegerAset(ThreadContext context) {
        return check(context, INTEGER_CLASS, ASET_METHOD);
    }

    public static boolean checkIntegerLength(ThreadContext context) {
        return check(context, INTEGER_CLASS, LENGTH_METHOD);
    }

    public static boolean checkIntegerSize(ThreadContext context) {
        return check(context, INTEGER_CLASS, SIZE_METHOD);
    }

    public static boolean checkIntegerEmpty(ThreadContext context) {
        return check(context, INTEGER_CLASS, EMPTY_P_METHOD);
    }

    public static boolean checkIntegerNil(ThreadContext context) {
        return check(context, INTEGER_CLASS, NIL_P_METHOD);
    }

    public static boolean checkIntegerSucc(ThreadContext context) {
        return check(context, INTEGER_CLASS, SUCC_METHOD);
    }

    public static boolean checkIntegerGt(ThreadContext context) {
        return check(context, INTEGER_CLASS, GT_METHOD);
    }

    public static boolean checkIntegerGe(ThreadContext context) {
        return check(context, INTEGER_CLASS, GE_METHOD);
    }

    public static boolean checkIntegerGtgt(ThreadContext context) {
        return check(context, INTEGER_CLASS, GTGT_METHOD);
    }

    public static boolean checkIntegerNot(ThreadContext context) {
        return check(context, INTEGER_CLASS, NOT_METHOD);
    }

    public static boolean checkIntegerNeq(ThreadContext context) {
        return check(context, INTEGER_CLASS, NEQ_METHOD);
    }

    public static boolean checkIntegerMatch(ThreadContext context) {
        return check(context, INTEGER_CLASS, MATCH_METHOD);
    }

    public static boolean checkIntegerFreeze(ThreadContext context) {
        return check(context, INTEGER_CLASS, FREEZE_METHOD);
    }

    public static boolean checkIntegerUminus(ThreadContext context) {
        return check(context, INTEGER_CLASS, UMINUS_METHOD);
    }

    public static boolean checkIntegerMax(ThreadContext context) {
        return check(context, INTEGER_CLASS, MAX_METHOD);
    }

    public static boolean checkIntegerMin(ThreadContext context) {
        return check(context, INTEGER_CLASS, MIN_METHOD);
    }

    public static boolean checkIntegerHash(ThreadContext context) {
        return check(context, INTEGER_CLASS, HASH_METHOD);
    }

    public static boolean checkIntegerCall(ThreadContext context) {
        return check(context, INTEGER_CLASS, CALL_METHOD);
    }

    public static boolean checkIntegerAnd(ThreadContext context) {
        return check(context, INTEGER_CLASS, AND_METHOD);
    }

    public static boolean checkIntegerOr(ThreadContext context) {
        return check(context, INTEGER_CLASS, OR_METHOD);
    }

    public static boolean checkIntegerCmp(ThreadContext context) {
        return check(context, INTEGER_CLASS, CMP_METHOD);
    }

    public static boolean checkIntegerDefault(ThreadContext context) {
        return check(context, INTEGER_CLASS, DEFAULT_METHOD);
    }

    public static boolean checkIntegerPack(ThreadContext context) {
        return check(context, INTEGER_CLASS, PACK_METHOD);
    }

    public static boolean checkIntegerInclude(ThreadContext context) {
        return check(context, INTEGER_CLASS, INCLUDE_P_METHOD);
    }

    public static boolean checkIntegerToF(ThreadContext context) {
        return check(context, INTEGER_CLASS, TO_F_METHOD);
    }
    
    public static boolean checkFloatPlus(ThreadContext context) {
        return check(context, FLOAT_CLASS, PLUS_METHOD);
    }

    public static boolean checkFloatMinus(ThreadContext context) {
        return check(context, FLOAT_CLASS, MINUS_METHOD);
    }

    public static boolean checkFloatMult(ThreadContext context) {
        return check(context, FLOAT_CLASS, MULT_METHOD);
    }

    public static boolean checkFloatDiv(ThreadContext context) {
        return check(context, FLOAT_CLASS, DIV_METHOD);
    }

    public static boolean checkFloatMod(ThreadContext context) {
        return check(context, FLOAT_CLASS, MOD_METHOD);
    }

    public static boolean checkFloatEquals(ThreadContext context) {
        return check(context, FLOAT_CLASS, EQ_METHOD);
    }

    public static boolean checkFloatEqq(ThreadContext context) {
        return check(context, FLOAT_CLASS, EQQ_METHOD);
    }

    public static boolean checkFloatLt(ThreadContext context) {
        return check(context, FLOAT_CLASS, LT_METHOD);
    }

    public static boolean checkFloatLe(ThreadContext context) {
        return check(context, FLOAT_CLASS, LE_METHOD);
    }

    public static boolean checkFloatLtlt(ThreadContext context) {
        return check(context, FLOAT_CLASS, LTLT_METHOD);
    }

    public static boolean checkFloatAref(ThreadContext context) {
        return check(context, FLOAT_CLASS, AREF_METHOD);
    }

    public static boolean checkFloatAset(ThreadContext context) {
        return check(context, FLOAT_CLASS, ASET_METHOD);
    }

    public static boolean checkFloatLength(ThreadContext context) {
        return check(context, FLOAT_CLASS, LENGTH_METHOD);
    }

    public static boolean checkFloatSize(ThreadContext context) {
        return check(context, FLOAT_CLASS, SIZE_METHOD);
    }

    public static boolean checkFloatEmpty(ThreadContext context) {
        return check(context, FLOAT_CLASS, EMPTY_P_METHOD);
    }

    public static boolean checkFloatNil(ThreadContext context) {
        return check(context, FLOAT_CLASS, NIL_P_METHOD);
    }

    public static boolean checkFloatSucc(ThreadContext context) {
        return check(context, FLOAT_CLASS, SUCC_METHOD);
    }

    public static boolean checkFloatGt(ThreadContext context) {
        return check(context, FLOAT_CLASS, GT_METHOD);
    }

    public static boolean checkFloatGe(ThreadContext context) {
        return check(context, FLOAT_CLASS, GE_METHOD);
    }

    public static boolean checkFloatGtgt(ThreadContext context) {
        return check(context, FLOAT_CLASS, GTGT_METHOD);
    }

    public static boolean checkFloatNot(ThreadContext context) {
        return check(context, FLOAT_CLASS, NOT_METHOD);
    }

    public static boolean checkFloatNeq(ThreadContext context) {
        return check(context, FLOAT_CLASS, NEQ_METHOD);
    }

    public static boolean checkFloatMatch(ThreadContext context) {
        return check(context, FLOAT_CLASS, MATCH_METHOD);
    }

    public static boolean checkFloatFreeze(ThreadContext context) {
        return check(context, FLOAT_CLASS, FREEZE_METHOD);
    }

    public static boolean checkFloatUminus(ThreadContext context) {
        return check(context, FLOAT_CLASS, UMINUS_METHOD);
    }

    public static boolean checkFloatMax(ThreadContext context) {
        return check(context, FLOAT_CLASS, MAX_METHOD);
    }

    public static boolean checkFloatMin(ThreadContext context) {
        return check(context, FLOAT_CLASS, MIN_METHOD);
    }

    public static boolean checkFloatHash(ThreadContext context) {
        return check(context, FLOAT_CLASS, HASH_METHOD);
    }

    public static boolean checkFloatCall(ThreadContext context) {
        return check(context, FLOAT_CLASS, CALL_METHOD);
    }

    public static boolean checkFloatAnd(ThreadContext context) {
        return check(context, FLOAT_CLASS, AND_METHOD);
    }

    public static boolean checkFloatOr(ThreadContext context) {
        return check(context, FLOAT_CLASS, OR_METHOD);
    }

    public static boolean checkFloatCmp(ThreadContext context) {
        return check(context, FLOAT_CLASS, CMP_METHOD);
    }

    public static boolean checkFloatDefault(ThreadContext context) {
        return check(context, FLOAT_CLASS, DEFAULT_METHOD);
    }

    public static boolean checkFloatPack(ThreadContext context) {
        return check(context, FLOAT_CLASS, PACK_METHOD);
    }

    public static boolean checkFloatInclude(ThreadContext context) {
        return check(context, FLOAT_CLASS, INCLUDE_P_METHOD);
    }

    public static boolean checkStringPlus(ThreadContext context) {
        return check(context, STRING_CLASS, PLUS_METHOD);
    }

    public static boolean checkStringMinus(ThreadContext context) {
        return check(context, STRING_CLASS, MINUS_METHOD);
    }

    public static boolean checkStringMult(ThreadContext context) {
        return check(context, STRING_CLASS, MULT_METHOD);
    }

    public static boolean checkStringDiv(ThreadContext context) {
        return check(context, STRING_CLASS, DIV_METHOD);
    }

    public static boolean checkStringMod(ThreadContext context) {
        return check(context, STRING_CLASS, MOD_METHOD);
    }

    public static boolean checkStringEquals(ThreadContext context) {
        return check(context, STRING_CLASS, EQ_METHOD);
    }

    public static boolean checkStringEqq(ThreadContext context) {
        return check(context, STRING_CLASS, EQQ_METHOD);
    }

    public static boolean checkStringLt(ThreadContext context) {
        return check(context, STRING_CLASS, LT_METHOD);
    }

    public static boolean checkStringLe(ThreadContext context) {
        return check(context, STRING_CLASS, LE_METHOD);
    }

    public static boolean checkStringLtlt(ThreadContext context) {
        return check(context, STRING_CLASS, LTLT_METHOD);
    }

    public static boolean checkStringAref(ThreadContext context) {
        return check(context, STRING_CLASS, AREF_METHOD);
    }

    public static boolean checkStringAset(ThreadContext context) {
        return check(context, STRING_CLASS, ASET_METHOD);
    }

    public static boolean checkStringLength(ThreadContext context) {
        return check(context, STRING_CLASS, LENGTH_METHOD);
    }

    public static boolean checkStringSize(ThreadContext context) {
        return check(context, STRING_CLASS, SIZE_METHOD);
    }

    public static boolean checkStringEmpty(ThreadContext context) {
        return check(context, STRING_CLASS, EMPTY_P_METHOD);
    }

    public static boolean checkStringNil(ThreadContext context) {
        return check(context, STRING_CLASS, NIL_P_METHOD);
    }

    public static boolean checkStringSucc(ThreadContext context) {
        return check(context, STRING_CLASS, SUCC_METHOD);
    }

    public static boolean checkStringGt(ThreadContext context) {
        return check(context, STRING_CLASS, GT_METHOD);
    }

    public static boolean checkStringGe(ThreadContext context) {
        return check(context, STRING_CLASS, GE_METHOD);
    }

    public static boolean checkStringGtgt(ThreadContext context) {
        return check(context, STRING_CLASS, GTGT_METHOD);
    }

    public static boolean checkStringNot(ThreadContext context) {
        return check(context, STRING_CLASS, NOT_METHOD);
    }

    public static boolean checkStringNeq(ThreadContext context) {
        return check(context, STRING_CLASS, NEQ_METHOD);
    }

    public static boolean checkStringMatch(ThreadContext context) {
        return check(context, STRING_CLASS, MATCH_METHOD);
    }

    public static boolean checkStringFreeze(ThreadContext context) {
        return check(context, STRING_CLASS, FREEZE_METHOD);
    }

    public static boolean checkStringUminus(ThreadContext context) {
        return check(context, STRING_CLASS, UMINUS_METHOD);
    }

    public static boolean checkStringMax(ThreadContext context) {
        return check(context, STRING_CLASS, MAX_METHOD);
    }

    public static boolean checkStringMin(ThreadContext context) {
        return check(context, STRING_CLASS, MIN_METHOD);
    }

    public static boolean checkStringHash(ThreadContext context) {
        return check(context, STRING_CLASS, HASH_METHOD);
    }

    public static boolean checkStringCall(ThreadContext context) {
        return check(context, STRING_CLASS, CALL_METHOD);
    }

    public static boolean checkStringAnd(ThreadContext context) {
        return check(context, STRING_CLASS, AND_METHOD);
    }

    public static boolean checkStringOr(ThreadContext context) {
        return check(context, STRING_CLASS, OR_METHOD);
    }

    public static boolean checkStringCmp(ThreadContext context) {
        return check(context, STRING_CLASS, CMP_METHOD);
    }

    public static boolean checkStringDefault(ThreadContext context) {
        return check(context, STRING_CLASS, DEFAULT_METHOD);
    }

    public static boolean checkStringPack(ThreadContext context) {
        return check(context, STRING_CLASS, PACK_METHOD);
    }

    public static boolean checkStringInclude(ThreadContext context) {
        return check(context, STRING_CLASS, INCLUDE_P_METHOD);
    }

    public static boolean checkArrayPlus(ThreadContext context) {
        return check(context, ARRAY_CLASS, PLUS_METHOD);
    }

    public static boolean checkArrayMinus(ThreadContext context) {
        return check(context, ARRAY_CLASS, MINUS_METHOD);
    }

    public static boolean checkArrayMult(ThreadContext context) {
        return check(context, ARRAY_CLASS, MULT_METHOD);
    }

    public static boolean checkArrayDiv(ThreadContext context) {
        return check(context, ARRAY_CLASS, DIV_METHOD);
    }

    public static boolean checkArrayMod(ThreadContext context) {
        return check(context, ARRAY_CLASS, MOD_METHOD);
    }

    public static boolean checkArrayEquals(ThreadContext context) {
        return check(context, ARRAY_CLASS, EQ_METHOD);
    }

    public static boolean checkArrayEqq(ThreadContext context) {
        return check(context, ARRAY_CLASS, EQQ_METHOD);
    }

    public static boolean checkArrayLt(ThreadContext context) {
        return check(context, ARRAY_CLASS, LT_METHOD);
    }

    public static boolean checkArrayLe(ThreadContext context) {
        return check(context, ARRAY_CLASS, LE_METHOD);
    }

    public static boolean checkArrayLtlt(ThreadContext context) {
        return check(context, ARRAY_CLASS, LTLT_METHOD);
    }

    public static boolean checkArrayAref(ThreadContext context) {
        return check(context, ARRAY_CLASS, AREF_METHOD);
    }

    public static boolean checkArrayAset(ThreadContext context) {
        return check(context, ARRAY_CLASS, ASET_METHOD);
    }

    public static boolean checkArrayLength(ThreadContext context) {
        return check(context, ARRAY_CLASS, LENGTH_METHOD);
    }

    public static boolean checkArraySize(ThreadContext context) {
        return check(context, ARRAY_CLASS, SIZE_METHOD);
    }

    public static boolean checkArrayEmpty(ThreadContext context) {
        return check(context, ARRAY_CLASS, EMPTY_P_METHOD);
    }

    public static boolean checkArrayNil(ThreadContext context) {
        return check(context, ARRAY_CLASS, NIL_P_METHOD);
    }

    public static boolean checkArraySucc(ThreadContext context) {
        return check(context, ARRAY_CLASS, SUCC_METHOD);
    }

    public static boolean checkArrayGt(ThreadContext context) {
        return check(context, ARRAY_CLASS, GT_METHOD);
    }

    public static boolean checkArrayGe(ThreadContext context) {
        return check(context, ARRAY_CLASS, GE_METHOD);
    }

    public static boolean checkArrayGtgt(ThreadContext context) {
        return check(context, ARRAY_CLASS, GTGT_METHOD);
    }

    public static boolean checkArrayNot(ThreadContext context) {
        return check(context, ARRAY_CLASS, NOT_METHOD);
    }

    public static boolean checkArrayNeq(ThreadContext context) {
        return check(context, ARRAY_CLASS, NEQ_METHOD);
    }

    public static boolean checkArrayMatch(ThreadContext context) {
        return check(context, ARRAY_CLASS, MATCH_METHOD);
    }

    public static boolean checkArrayFreeze(ThreadContext context) {
        return check(context, ARRAY_CLASS, FREEZE_METHOD);
    }

    public static boolean checkArrayUminus(ThreadContext context) {
        return check(context, ARRAY_CLASS, UMINUS_METHOD);
    }

    public static boolean checkArrayMax(ThreadContext context) {
        return check(context, ARRAY_CLASS, MAX_METHOD);
    }

    public static boolean checkArrayMin(ThreadContext context) {
        return check(context, ARRAY_CLASS, MIN_METHOD);
    }

    public static boolean checkArrayHash(ThreadContext context) {
        return check(context, ARRAY_CLASS, HASH_METHOD);
    }

    public static boolean checkArrayCall(ThreadContext context) {
        return check(context, ARRAY_CLASS, CALL_METHOD);
    }

    public static boolean checkArrayAnd(ThreadContext context) {
        return check(context, ARRAY_CLASS, AND_METHOD);
    }

    public static boolean checkArrayOr(ThreadContext context) {
        return check(context, ARRAY_CLASS, OR_METHOD);
    }

    public static boolean checkArrayCmp(ThreadContext context) {
        return check(context, ARRAY_CLASS, CMP_METHOD);
    }

    public static boolean checkArrayDefault(ThreadContext context) {
        return check(context, ARRAY_CLASS, DEFAULT_METHOD);
    }

    public static boolean checkArrayPack(ThreadContext context) {
        return check(context, ARRAY_CLASS, PACK_METHOD);
    }

    public static boolean checkArrayInclude(ThreadContext context) {
        return check(context, ARRAY_CLASS, INCLUDE_P_METHOD);
    }

    public static boolean checkArrayDig(ThreadContext context) {
        return check(context, ARRAY_CLASS, DIG_METHOD);
    }
    
    public static boolean checkHashPlus(ThreadContext context) {
        return check(context, HASH_CLASS, PLUS_METHOD);
    }

    public static boolean checkHashMinus(ThreadContext context) {
        return check(context, HASH_CLASS, MINUS_METHOD);
    }

    public static boolean checkHashMult(ThreadContext context) {
        return check(context, HASH_CLASS, MULT_METHOD);
    }

    public static boolean checkHashDiv(ThreadContext context) {
        return check(context, HASH_CLASS, DIV_METHOD);
    }

    public static boolean checkHashMod(ThreadContext context) {
        return check(context, HASH_CLASS, MOD_METHOD);
    }

    public static boolean checkHashEquals(ThreadContext context) {
        return check(context, HASH_CLASS, EQ_METHOD);
    }

    public static boolean checkHashEqq(ThreadContext context) {
        return check(context, HASH_CLASS, EQQ_METHOD);
    }

    public static boolean checkHashLt(ThreadContext context) {
        return check(context, HASH_CLASS, LT_METHOD);
    }

    public static boolean checkHashLe(ThreadContext context) {
        return check(context, HASH_CLASS, LE_METHOD);
    }

    public static boolean checkHashLtlt(ThreadContext context) {
        return check(context, HASH_CLASS, LTLT_METHOD);
    }

    public static boolean checkHashAref(ThreadContext context) {
        return check(context, HASH_CLASS, AREF_METHOD);
    }

    public static boolean checkHashAset(ThreadContext context) {
        return check(context, HASH_CLASS, ASET_METHOD);
    }

    public static boolean checkHashLength(ThreadContext context) {
        return check(context, HASH_CLASS, LENGTH_METHOD);
    }

    public static boolean checkHashSize(ThreadContext context) {
        return check(context, HASH_CLASS, SIZE_METHOD);
    }

    public static boolean checkHashEmpty(ThreadContext context) {
        return check(context, HASH_CLASS, EMPTY_P_METHOD);
    }

    public static boolean checkHashNil(ThreadContext context) {
        return check(context, HASH_CLASS, NIL_P_METHOD);
    }

    public static boolean checkHashSucc(ThreadContext context) {
        return check(context, HASH_CLASS, SUCC_METHOD);
    }

    public static boolean checkHashGt(ThreadContext context) {
        return check(context, HASH_CLASS, GT_METHOD);
    }

    public static boolean checkHashGe(ThreadContext context) {
        return check(context, HASH_CLASS, GE_METHOD);
    }

    public static boolean checkHashGtgt(ThreadContext context) {
        return check(context, HASH_CLASS, GTGT_METHOD);
    }

    public static boolean checkHashNot(ThreadContext context) {
        return check(context, HASH_CLASS, NOT_METHOD);
    }

    public static boolean checkHashNeq(ThreadContext context) {
        return check(context, HASH_CLASS, NEQ_METHOD);
    }

    public static boolean checkHashMatch(ThreadContext context) {
        return check(context, HASH_CLASS, MATCH_METHOD);
    }

    public static boolean checkHashFreeze(ThreadContext context) {
        return check(context, HASH_CLASS, FREEZE_METHOD);
    }

    public static boolean checkHashUminus(ThreadContext context) {
        return check(context, HASH_CLASS, UMINUS_METHOD);
    }

    public static boolean checkHashMax(ThreadContext context) {
        return check(context, HASH_CLASS, MAX_METHOD);
    }

    public static boolean checkHashMin(ThreadContext context) {
        return check(context, HASH_CLASS, MIN_METHOD);
    }

    public static boolean checkHashHash(ThreadContext context) {
        return check(context, HASH_CLASS, HASH_METHOD);
    }

    public static boolean checkHashCall(ThreadContext context) {
        return check(context, HASH_CLASS, CALL_METHOD);
    }

    public static boolean checkHashAnd(ThreadContext context) {
        return check(context, HASH_CLASS, AND_METHOD);
    }

    public static boolean checkHashOr(ThreadContext context) {
        return check(context, HASH_CLASS, OR_METHOD);
    }

    public static boolean checkHashCmp(ThreadContext context) {
        return check(context, HASH_CLASS, CMP_METHOD);
    }

    public static boolean checkHashDefault(ThreadContext context) {
        return check(context, HASH_CLASS, DEFAULT_METHOD);
    }

    public static boolean checkHashPack(ThreadContext context) {
        return check(context, HASH_CLASS, PACK_METHOD);
    }

    public static boolean checkHashInclude(ThreadContext context) {
        return check(context, HASH_CLASS, INCLUDE_P_METHOD);
    }
    
    public static boolean checkSymbolPlus(ThreadContext context) {
        return check(context, SYMBOL_CLASS, PLUS_METHOD);
    }

    public static boolean checkHashDig(ThreadContext context) {
        return check(context, HASH_CLASS, DIG_METHOD);
    }

    public static boolean checkSymbolMinus(ThreadContext context) {
        return check(context, SYMBOL_CLASS, MINUS_METHOD);
    }

    public static boolean checkSymbolMult(ThreadContext context) {
        return check(context, SYMBOL_CLASS, MULT_METHOD);
    }

    public static boolean checkSymbolDiv(ThreadContext context) {
        return check(context, SYMBOL_CLASS, DIV_METHOD);
    }

    public static boolean checkSymbolMod(ThreadContext context) {
        return check(context, SYMBOL_CLASS, MOD_METHOD);
    }

    public static boolean checkSymbolEquals(ThreadContext context) {
        return check(context, SYMBOL_CLASS, EQ_METHOD);
    }

    public static boolean checkSymbolEqq(ThreadContext context) {
        return check(context, SYMBOL_CLASS, EQQ_METHOD);
    }

    public static boolean checkSymbolLt(ThreadContext context) {
        return check(context, SYMBOL_CLASS, LT_METHOD);
    }

    public static boolean checkSymbolLe(ThreadContext context) {
        return check(context, SYMBOL_CLASS, LE_METHOD);
    }

    public static boolean checkSymbolLtlt(ThreadContext context) {
        return check(context, SYMBOL_CLASS, LTLT_METHOD);
    }

    public static boolean checkSymbolAref(ThreadContext context) {
        return check(context, SYMBOL_CLASS, AREF_METHOD);
    }

    public static boolean checkSymbolAset(ThreadContext context) {
        return check(context, SYMBOL_CLASS, ASET_METHOD);
    }

    public static boolean checkSymbolLength(ThreadContext context) {
        return check(context, SYMBOL_CLASS, LENGTH_METHOD);
    }

    public static boolean checkSymbolSize(ThreadContext context) {
        return check(context, SYMBOL_CLASS, SIZE_METHOD);
    }

    public static boolean checkSymbolEmpty(ThreadContext context) {
        return check(context, SYMBOL_CLASS, EMPTY_P_METHOD);
    }

    public static boolean checkSymbolNil(ThreadContext context) {
        return check(context, SYMBOL_CLASS, NIL_P_METHOD);
    }

    public static boolean checkSymbolSucc(ThreadContext context) {
        return check(context, SYMBOL_CLASS, SUCC_METHOD);
    }

    public static boolean checkSymbolGt(ThreadContext context) {
        return check(context, SYMBOL_CLASS, GT_METHOD);
    }

    public static boolean checkSymbolGe(ThreadContext context) {
        return check(context, SYMBOL_CLASS, GE_METHOD);
    }

    public static boolean checkSymbolGtgt(ThreadContext context) {
        return check(context, SYMBOL_CLASS, GTGT_METHOD);
    }

    public static boolean checkSymbolNot(ThreadContext context) {
        return check(context, SYMBOL_CLASS, NOT_METHOD);
    }

    public static boolean checkSymbolNeq(ThreadContext context) {
        return check(context, SYMBOL_CLASS, NEQ_METHOD);
    }

    public static boolean checkSymbolMatch(ThreadContext context) {
        return check(context, SYMBOL_CLASS, MATCH_METHOD);
    }

    public static boolean checkSymbolFreeze(ThreadContext context) {
        return check(context, SYMBOL_CLASS, FREEZE_METHOD);
    }

    public static boolean checkSymbolUminus(ThreadContext context) {
        return check(context, SYMBOL_CLASS, UMINUS_METHOD);
    }

    public static boolean checkSymbolMax(ThreadContext context) {
        return check(context, SYMBOL_CLASS, MAX_METHOD);
    }

    public static boolean checkSymbolMin(ThreadContext context) {
        return check(context, SYMBOL_CLASS, MIN_METHOD);
    }

    public static boolean checkSymbolHash(ThreadContext context) {
        return check(context, SYMBOL_CLASS, HASH_METHOD);
    }

    public static boolean checkSymbolCall(ThreadContext context) {
        return check(context, SYMBOL_CLASS, CALL_METHOD);
    }

    public static boolean checkSymbolAnd(ThreadContext context) {
        return check(context, SYMBOL_CLASS, AND_METHOD);
    }

    public static boolean checkSymbolOr(ThreadContext context) {
        return check(context, SYMBOL_CLASS, OR_METHOD);
    }

    public static boolean checkSymbolCmp(ThreadContext context) {
        return check(context, SYMBOL_CLASS, CMP_METHOD);
    }

    public static boolean checkSymbolDefault(ThreadContext context) {
        return check(context, SYMBOL_CLASS, DEFAULT_METHOD);
    }

    public static boolean checkSymbolPack(ThreadContext context) {
        return check(context, SYMBOL_CLASS, PACK_METHOD);
    }

    public static boolean checkSymbolInclude(ThreadContext context) {
        return check(context, SYMBOL_CLASS, INCLUDE_P_METHOD);
    }
    
    public static boolean checkTimePlus(ThreadContext context) {
        return check(context, TIME_CLASS, PLUS_METHOD);
    }

    public static boolean checkTimeMinus(ThreadContext context) {
        return check(context, TIME_CLASS, MINUS_METHOD);
    }

    public static boolean checkTimeMult(ThreadContext context) {
        return check(context, TIME_CLASS, MULT_METHOD);
    }

    public static boolean checkTimeDiv(ThreadContext context) {
        return check(context, TIME_CLASS, DIV_METHOD);
    }

    public static boolean checkTimeMod(ThreadContext context) {
        return check(context, TIME_CLASS, MOD_METHOD);
    }

    public static boolean checkTimeEquals(ThreadContext context) {
        return check(context, TIME_CLASS, EQ_METHOD);
    }

    public static boolean checkTimeEqq(ThreadContext context) {
        return check(context, TIME_CLASS, EQQ_METHOD);
    }

    public static boolean checkTimeLt(ThreadContext context) {
        return check(context, TIME_CLASS, LT_METHOD);
    }

    public static boolean checkTimeLe(ThreadContext context) {
        return check(context, TIME_CLASS, LE_METHOD);
    }

    public static boolean checkTimeLtlt(ThreadContext context) {
        return check(context, TIME_CLASS, LTLT_METHOD);
    }

    public static boolean checkTimeAref(ThreadContext context) {
        return check(context, TIME_CLASS, AREF_METHOD);
    }

    public static boolean checkTimeAset(ThreadContext context) {
        return check(context, TIME_CLASS, ASET_METHOD);
    }

    public static boolean checkTimeLength(ThreadContext context) {
        return check(context, TIME_CLASS, LENGTH_METHOD);
    }

    public static boolean checkTimeSize(ThreadContext context) {
        return check(context, TIME_CLASS, SIZE_METHOD);
    }

    public static boolean checkTimeEmpty(ThreadContext context) {
        return check(context, TIME_CLASS, EMPTY_P_METHOD);
    }

    public static boolean checkTimeNil(ThreadContext context) {
        return check(context, TIME_CLASS, NIL_P_METHOD);
    }

    public static boolean checkTimeSucc(ThreadContext context) {
        return check(context, TIME_CLASS, SUCC_METHOD);
    }

    public static boolean checkTimeGt(ThreadContext context) {
        return check(context, TIME_CLASS, GT_METHOD);
    }

    public static boolean checkTimeGe(ThreadContext context) {
        return check(context, TIME_CLASS, GE_METHOD);
    }

    public static boolean checkTimeGtgt(ThreadContext context) {
        return check(context, TIME_CLASS, GTGT_METHOD);
    }

    public static boolean checkTimeNot(ThreadContext context) {
        return check(context, TIME_CLASS, NOT_METHOD);
    }

    public static boolean checkTimeNeq(ThreadContext context) {
        return check(context, TIME_CLASS, NEQ_METHOD);
    }

    public static boolean checkTimeMatch(ThreadContext context) {
        return check(context, TIME_CLASS, MATCH_METHOD);
    }

    public static boolean checkTimeFreeze(ThreadContext context) {
        return check(context, TIME_CLASS, FREEZE_METHOD);
    }

    public static boolean checkTimeUminus(ThreadContext context) {
        return check(context, TIME_CLASS, UMINUS_METHOD);
    }

    public static boolean checkTimeMax(ThreadContext context) {
        return check(context, TIME_CLASS, MAX_METHOD);
    }

    public static boolean checkTimeMin(ThreadContext context) {
        return check(context, TIME_CLASS, MIN_METHOD);
    }

    public static boolean checkTimeHash(ThreadContext context) {
        return check(context, TIME_CLASS, HASH_METHOD);
    }

    public static boolean checkTimeCall(ThreadContext context) {
        return check(context, TIME_CLASS, CALL_METHOD);
    }

    public static boolean checkTimeAnd(ThreadContext context) {
        return check(context, TIME_CLASS, AND_METHOD);
    }

    public static boolean checkTimeOr(ThreadContext context) {
        return check(context, TIME_CLASS, OR_METHOD);
    }

    public static boolean checkTimeCmp(ThreadContext context) {
        return check(context, TIME_CLASS, CMP_METHOD);
    }

    public static boolean checkTimeDefault(ThreadContext context) {
        return check(context, TIME_CLASS, DEFAULT_METHOD);
    }

    public static boolean checkTimePack(ThreadContext context) {
        return check(context, TIME_CLASS, PACK_METHOD);
    }

    public static boolean checkTimeInclude(ThreadContext context) {
        return check(context, TIME_CLASS, INCLUDE_P_METHOD);
    }

    public static boolean checkRegexpPlus(ThreadContext context) {
        return check(context, REGEXP_CLASS, PLUS_METHOD);
    }

    public static boolean checkRegexpMinus(ThreadContext context) {
        return check(context, REGEXP_CLASS, MINUS_METHOD);
    }

    public static boolean checkRegexpMult(ThreadContext context) {
        return check(context, REGEXP_CLASS, MULT_METHOD);
    }

    public static boolean checkRegexpDiv(ThreadContext context) {
        return check(context, REGEXP_CLASS, DIV_METHOD);
    }

    public static boolean checkRegexpMod(ThreadContext context) {
        return check(context, REGEXP_CLASS, MOD_METHOD);
    }

    public static boolean checkRegexpEquals(ThreadContext context) {
        return check(context, REGEXP_CLASS, EQ_METHOD);
    }

    public static boolean checkRegexpEqq(ThreadContext context) {
        return check(context, REGEXP_CLASS, EQQ_METHOD);
    }

    public static boolean checkRegexpLt(ThreadContext context) {
        return check(context, REGEXP_CLASS, LT_METHOD);
    }

    public static boolean checkRegexpLe(ThreadContext context) {
        return check(context, REGEXP_CLASS, LE_METHOD);
    }

    public static boolean checkRegexpLtlt(ThreadContext context) {
        return check(context, REGEXP_CLASS, LTLT_METHOD);
    }

    public static boolean checkRegexpAref(ThreadContext context) {
        return check(context, REGEXP_CLASS, AREF_METHOD);
    }

    public static boolean checkRegexpAset(ThreadContext context) {
        return check(context, REGEXP_CLASS, ASET_METHOD);
    }

    public static boolean checkRegexpLength(ThreadContext context) {
        return check(context, REGEXP_CLASS, LENGTH_METHOD);
    }

    public static boolean checkRegexpSize(ThreadContext context) {
        return check(context, REGEXP_CLASS, SIZE_METHOD);
    }

    public static boolean checkRegexpEmpty(ThreadContext context) {
        return check(context, REGEXP_CLASS, EMPTY_P_METHOD);
    }

    public static boolean checkRegexpNil(ThreadContext context) {
        return check(context, REGEXP_CLASS, NIL_P_METHOD);
    }

    public static boolean checkRegexpSucc(ThreadContext context) {
        return check(context, REGEXP_CLASS, SUCC_METHOD);
    }

    public static boolean checkRegexpGt(ThreadContext context) {
        return check(context, REGEXP_CLASS, GT_METHOD);
    }

    public static boolean checkRegexpGe(ThreadContext context) {
        return check(context, REGEXP_CLASS, GE_METHOD);
    }

    public static boolean checkRegexpGtgt(ThreadContext context) {
        return check(context, REGEXP_CLASS, GTGT_METHOD);
    }

    public static boolean checkRegexpNot(ThreadContext context) {
        return check(context, REGEXP_CLASS, NOT_METHOD);
    }

    public static boolean checkRegexpNeq(ThreadContext context) {
        return check(context, REGEXP_CLASS, NEQ_METHOD);
    }

    public static boolean checkRegexpMatch(ThreadContext context) {
        return check(context, REGEXP_CLASS, MATCH_METHOD);
    }

    public static boolean checkRegexpFreeze(ThreadContext context) {
        return check(context, REGEXP_CLASS, FREEZE_METHOD);
    }

    public static boolean checkRegexpUminus(ThreadContext context) {
        return check(context, REGEXP_CLASS, UMINUS_METHOD);
    }

    public static boolean checkRegexpMax(ThreadContext context) {
        return check(context, REGEXP_CLASS, MAX_METHOD);
    }

    public static boolean checkRegexpMin(ThreadContext context) {
        return check(context, REGEXP_CLASS, MIN_METHOD);
    }

    public static boolean checkRegexpHash(ThreadContext context) {
        return check(context, REGEXP_CLASS, HASH_METHOD);
    }

    public static boolean checkRegexpCall(ThreadContext context) {
        return check(context, REGEXP_CLASS, CALL_METHOD);
    }

    public static boolean checkRegexpAnd(ThreadContext context) {
        return check(context, REGEXP_CLASS, AND_METHOD);
    }

    public static boolean checkRegexpOr(ThreadContext context) {
        return check(context, REGEXP_CLASS, OR_METHOD);
    }

    public static boolean checkRegexpCmp(ThreadContext context) {
        return check(context, REGEXP_CLASS, CMP_METHOD);
    }

    public static boolean checkRegexpDefault(ThreadContext context) {
        return check(context, REGEXP_CLASS, DEFAULT_METHOD);
    }

    public static boolean checkRegexpPack(ThreadContext context) {
        return check(context, REGEXP_CLASS, PACK_METHOD);
    }

    public static boolean checkRegexpInclude(ThreadContext context) {
        return check(context, REGEXP_CLASS, INCLUDE_P_METHOD);
    }

    public static boolean checkNilPlus(ThreadContext context) {
        return check(context, NIL_CLASS, PLUS_METHOD);
    }

    public static boolean checkNilMinus(ThreadContext context) {
        return check(context, NIL_CLASS, MINUS_METHOD);
    }

    public static boolean checkNilMult(ThreadContext context) {
        return check(context, NIL_CLASS, MULT_METHOD);
    }

    public static boolean checkNilDiv(ThreadContext context) {
        return check(context, NIL_CLASS, DIV_METHOD);
    }

    public static boolean checkNilMod(ThreadContext context) {
        return check(context, NIL_CLASS, MOD_METHOD);
    }

    public static boolean checkNilEquals(ThreadContext context) {
        return check(context, NIL_CLASS, EQ_METHOD);
    }

    public static boolean checkNilEqq(ThreadContext context) {
        return check(context, NIL_CLASS, EQQ_METHOD);
    }

    public static boolean checkNilLt(ThreadContext context) {
        return check(context, NIL_CLASS, LT_METHOD);
    }

    public static boolean checkNilLe(ThreadContext context) {
        return check(context, NIL_CLASS, LE_METHOD);
    }

    public static boolean checkNilLtlt(ThreadContext context) {
        return check(context, NIL_CLASS, LTLT_METHOD);
    }

    public static boolean checkNilAref(ThreadContext context) {
        return check(context, NIL_CLASS, AREF_METHOD);
    }

    public static boolean checkNilAset(ThreadContext context) {
        return check(context, NIL_CLASS, ASET_METHOD);
    }

    public static boolean checkNilLength(ThreadContext context) {
        return check(context, NIL_CLASS, LENGTH_METHOD);
    }

    public static boolean checkNilSize(ThreadContext context) {
        return check(context, NIL_CLASS, SIZE_METHOD);
    }

    public static boolean checkNilEmpty(ThreadContext context) {
        return check(context, NIL_CLASS, EMPTY_P_METHOD);
    }

    public static boolean checkNilNil(ThreadContext context) {
        return check(context, NIL_CLASS, NIL_P_METHOD);
    }

    public static boolean checkNilSucc(ThreadContext context) {
        return check(context, NIL_CLASS, SUCC_METHOD);
    }

    public static boolean checkNilGt(ThreadContext context) {
        return check(context, NIL_CLASS, GT_METHOD);
    }

    public static boolean checkNilGe(ThreadContext context) {
        return check(context, NIL_CLASS, GE_METHOD);
    }

    public static boolean checkNilGtgt(ThreadContext context) {
        return check(context, NIL_CLASS, GTGT_METHOD);
    }

    public static boolean checkNilNot(ThreadContext context) {
        return check(context, NIL_CLASS, NOT_METHOD);
    }

    public static boolean checkNilNeq(ThreadContext context) {
        return check(context, NIL_CLASS, NEQ_METHOD);
    }

    public static boolean checkNilMatch(ThreadContext context) {
        return check(context, NIL_CLASS, MATCH_METHOD);
    }

    public static boolean checkNilFreeze(ThreadContext context) {
        return check(context, NIL_CLASS, FREEZE_METHOD);
    }

    public static boolean checkNilUminus(ThreadContext context) {
        return check(context, NIL_CLASS, UMINUS_METHOD);
    }

    public static boolean checkNilMax(ThreadContext context) {
        return check(context, NIL_CLASS, MAX_METHOD);
    }

    public static boolean checkNilMin(ThreadContext context) {
        return check(context, NIL_CLASS, MIN_METHOD);
    }

    public static boolean checkNilHash(ThreadContext context) {
        return check(context, NIL_CLASS, HASH_METHOD);
    }

    public static boolean checkNilCall(ThreadContext context) {
        return check(context, NIL_CLASS, CALL_METHOD);
    }

    public static boolean checkNilAnd(ThreadContext context) {
        return check(context, NIL_CLASS, AND_METHOD);
    }

    public static boolean checkNilOr(ThreadContext context) {
        return check(context, NIL_CLASS, OR_METHOD);
    }

    public static boolean checkNilCmp(ThreadContext context) {
        return check(context, NIL_CLASS, CMP_METHOD);
    }

    public static boolean checkNilDefault(ThreadContext context) {
        return check(context, NIL_CLASS, DEFAULT_METHOD);
    }

    public static boolean checkNilPack(ThreadContext context) {
        return check(context, NIL_CLASS, PACK_METHOD);
    }

    public static boolean checkNilInclude(ThreadContext context) {
        return check(context, NIL_CLASS, INCLUDE_P_METHOD);
    }

    public static boolean checkTruePlus(ThreadContext context) {
        return check(context, TRUE_CLASS, PLUS_METHOD);
    }

    public static boolean checkTrueMinus(ThreadContext context) {
        return check(context, TRUE_CLASS, MINUS_METHOD);
    }

    public static boolean checkTrueMult(ThreadContext context) {
        return check(context, TRUE_CLASS, MULT_METHOD);
    }

    public static boolean checkTrueDiv(ThreadContext context) {
        return check(context, TRUE_CLASS, DIV_METHOD);
    }

    public static boolean checkTrueMod(ThreadContext context) {
        return check(context, TRUE_CLASS, MOD_METHOD);
    }

    public static boolean checkTrueEquals(ThreadContext context) {
        return check(context, TRUE_CLASS, EQ_METHOD);
    }

    public static boolean checkTrueEqq(ThreadContext context) {
        return check(context, TRUE_CLASS, EQQ_METHOD);
    }

    public static boolean checkTrueLt(ThreadContext context) {
        return check(context, TRUE_CLASS, LT_METHOD);
    }

    public static boolean checkTrueLe(ThreadContext context) {
        return check(context, TRUE_CLASS, LE_METHOD);
    }

    public static boolean checkTrueLtlt(ThreadContext context) {
        return check(context, TRUE_CLASS, LTLT_METHOD);
    }

    public static boolean checkTrueAref(ThreadContext context) {
        return check(context, TRUE_CLASS, AREF_METHOD);
    }

    public static boolean checkTrueAset(ThreadContext context) {
        return check(context, TRUE_CLASS, ASET_METHOD);
    }

    public static boolean checkTrueLength(ThreadContext context) {
        return check(context, TRUE_CLASS, LENGTH_METHOD);
    }

    public static boolean checkTrueSize(ThreadContext context) {
        return check(context, TRUE_CLASS, SIZE_METHOD);
    }

    public static boolean checkTrueEmpty(ThreadContext context) {
        return check(context, TRUE_CLASS, EMPTY_P_METHOD);
    }

    public static boolean checkTrueNil(ThreadContext context) {
        return check(context, TRUE_CLASS, NIL_P_METHOD);
    }

    public static boolean checkTrueSucc(ThreadContext context) {
        return check(context, TRUE_CLASS, SUCC_METHOD);
    }

    public static boolean checkTrueGt(ThreadContext context) {
        return check(context, TRUE_CLASS, GT_METHOD);
    }

    public static boolean checkTrueGe(ThreadContext context) {
        return check(context, TRUE_CLASS, GE_METHOD);
    }

    public static boolean checkTrueGtgt(ThreadContext context) {
        return check(context, TRUE_CLASS, GTGT_METHOD);
    }

    public static boolean checkTrueNot(ThreadContext context) {
        return check(context, TRUE_CLASS, NOT_METHOD);
    }

    public static boolean checkTrueNeq(ThreadContext context) {
        return check(context, TRUE_CLASS, NEQ_METHOD);
    }

    public static boolean checkTrueMatch(ThreadContext context) {
        return check(context, TRUE_CLASS, MATCH_METHOD);
    }

    public static boolean checkTrueFreeze(ThreadContext context) {
        return check(context, TRUE_CLASS, FREEZE_METHOD);
    }

    public static boolean checkTrueUminus(ThreadContext context) {
        return check(context, TRUE_CLASS, UMINUS_METHOD);
    }

    public static boolean checkTrueMax(ThreadContext context) {
        return check(context, TRUE_CLASS, MAX_METHOD);
    }

    public static boolean checkTrueMin(ThreadContext context) {
        return check(context, TRUE_CLASS, MIN_METHOD);
    }

    public static boolean checkTrueHash(ThreadContext context) {
        return check(context, TRUE_CLASS, HASH_METHOD);
    }

    public static boolean checkTrueCall(ThreadContext context) {
        return check(context, TRUE_CLASS, CALL_METHOD);
    }

    public static boolean checkTrueAnd(ThreadContext context) {
        return check(context, TRUE_CLASS, AND_METHOD);
    }

    public static boolean checkTrueOr(ThreadContext context) {
        return check(context, TRUE_CLASS, OR_METHOD);
    }

    public static boolean checkTrueCmp(ThreadContext context) {
        return check(context, TRUE_CLASS, CMP_METHOD);
    }

    public static boolean checkTrueDefault(ThreadContext context) {
        return check(context, TRUE_CLASS, DEFAULT_METHOD);
    }

    public static boolean checkTruePack(ThreadContext context) {
        return check(context, TRUE_CLASS, PACK_METHOD);
    }

    public static boolean checkTrueInclude(ThreadContext context) {
        return check(context, TRUE_CLASS, INCLUDE_P_METHOD);
    }

    public static boolean checkFalsePlus(ThreadContext context) {
        return check(context, FALSE_CLASS, PLUS_METHOD);
    }

    public static boolean checkFalseMinus(ThreadContext context) {
        return check(context, FALSE_CLASS, MINUS_METHOD);
    }

    public static boolean checkFalseMult(ThreadContext context) {
        return check(context, FALSE_CLASS, MULT_METHOD);
    }

    public static boolean checkFalseDiv(ThreadContext context) {
        return check(context, FALSE_CLASS, DIV_METHOD);
    }

    public static boolean checkFalseMod(ThreadContext context) {
        return check(context, FALSE_CLASS, MOD_METHOD);
    }

    public static boolean checkFalseEquals(ThreadContext context) {
        return check(context, FALSE_CLASS, EQ_METHOD);
    }

    public static boolean checkFalseEqq(ThreadContext context) {
        return check(context, FALSE_CLASS, EQQ_METHOD);
    }

    public static boolean checkFalseLt(ThreadContext context) {
        return check(context, FALSE_CLASS, LT_METHOD);
    }

    public static boolean checkFalseLe(ThreadContext context) {
        return check(context, FALSE_CLASS, LE_METHOD);
    }

    public static boolean checkFalseLtlt(ThreadContext context) {
        return check(context, FALSE_CLASS, LTLT_METHOD);
    }

    public static boolean checkFalseAref(ThreadContext context) {
        return check(context, FALSE_CLASS, AREF_METHOD);
    }

    public static boolean checkFalseAset(ThreadContext context) {
        return check(context, FALSE_CLASS, ASET_METHOD);
    }

    public static boolean checkFalseLength(ThreadContext context) {
        return check(context, FALSE_CLASS, LENGTH_METHOD);
    }

    public static boolean checkFalseSize(ThreadContext context) {
        return check(context, FALSE_CLASS, SIZE_METHOD);
    }

    public static boolean checkFalseEmpty(ThreadContext context) {
        return check(context, FALSE_CLASS, EMPTY_P_METHOD);
    }

    public static boolean checkFalseNil(ThreadContext context) {
        return check(context, FALSE_CLASS, NIL_P_METHOD);
    }

    public static boolean checkFalseSucc(ThreadContext context) {
        return check(context, FALSE_CLASS, SUCC_METHOD);
    }

    public static boolean checkFalseGt(ThreadContext context) {
        return check(context, FALSE_CLASS, GT_METHOD);
    }

    public static boolean checkFalseGe(ThreadContext context) {
        return check(context, FALSE_CLASS, GE_METHOD);
    }

    public static boolean checkFalseGtgt(ThreadContext context) {
        return check(context, FALSE_CLASS, GTGT_METHOD);
    }

    public static boolean checkFalseNot(ThreadContext context) {
        return check(context, FALSE_CLASS, NOT_METHOD);
    }

    public static boolean checkFalseNeq(ThreadContext context) {
        return check(context, FALSE_CLASS, NEQ_METHOD);
    }

    public static boolean checkFalseMatch(ThreadContext context) {
        return check(context, FALSE_CLASS, MATCH_METHOD);
    }

    public static boolean checkFalseFreeze(ThreadContext context) {
        return check(context, FALSE_CLASS, FREEZE_METHOD);
    }

    public static boolean checkFalseUminus(ThreadContext context) {
        return check(context, FALSE_CLASS, UMINUS_METHOD);
    }

    public static boolean checkFalseMax(ThreadContext context) {
        return check(context, FALSE_CLASS, MAX_METHOD);
    }

    public static boolean checkFalseMin(ThreadContext context) {
        return check(context, FALSE_CLASS, MIN_METHOD);
    }

    public static boolean checkFalseHash(ThreadContext context) {
        return check(context, FALSE_CLASS, HASH_METHOD);
    }

    public static boolean checkFalseCall(ThreadContext context) {
        return check(context, FALSE_CLASS, CALL_METHOD);
    }

    public static boolean checkFalseAnd(ThreadContext context) {
        return check(context, FALSE_CLASS, AND_METHOD);
    }

    public static boolean checkFalseOr(ThreadContext context) {
        return check(context, FALSE_CLASS, OR_METHOD);
    }

    public static boolean checkFalseCmp(ThreadContext context) {
        return check(context, FALSE_CLASS, CMP_METHOD);
    }

    public static boolean checkFalseDefault(ThreadContext context) {
        return check(context, FALSE_CLASS, DEFAULT_METHOD);
    }

    public static boolean checkFalsePack(ThreadContext context) {
        return check(context, FALSE_CLASS, PACK_METHOD);
    }

    public static boolean checkFalseInclude(ThreadContext context) {
        return check(context, FALSE_CLASS, INCLUDE_P_METHOD);
    }

    public static boolean checkProcPlus(ThreadContext context) {
        return check(context, PROC_CLASS, PLUS_METHOD);
    }

    public static boolean checkProcMinus(ThreadContext context) {
        return check(context, PROC_CLASS, MINUS_METHOD);
    }

    public static boolean checkProcMult(ThreadContext context) {
        return check(context, PROC_CLASS, MULT_METHOD);
    }

    public static boolean checkProcDiv(ThreadContext context) {
        return check(context, PROC_CLASS, DIV_METHOD);
    }

    public static boolean checkProcMod(ThreadContext context) {
        return check(context, PROC_CLASS, MOD_METHOD);
    }

    public static boolean checkProcEquals(ThreadContext context) {
        return check(context, PROC_CLASS, EQ_METHOD);
    }

    public static boolean checkProcEqq(ThreadContext context) {
        return check(context, PROC_CLASS, EQQ_METHOD);
    }

    public static boolean checkProcLt(ThreadContext context) {
        return check(context, PROC_CLASS, LT_METHOD);
    }

    public static boolean checkProcLe(ThreadContext context) {
        return check(context, PROC_CLASS, LE_METHOD);
    }

    public static boolean checkProcLtlt(ThreadContext context) {
        return check(context, PROC_CLASS, LTLT_METHOD);
    }

    public static boolean checkProcAref(ThreadContext context) {
        return check(context, PROC_CLASS, AREF_METHOD);
    }

    public static boolean checkProcAset(ThreadContext context) {
        return check(context, PROC_CLASS, ASET_METHOD);
    }

    public static boolean checkProcLength(ThreadContext context) {
        return check(context, PROC_CLASS, LENGTH_METHOD);
    }

    public static boolean checkProcSize(ThreadContext context) {
        return check(context, PROC_CLASS, SIZE_METHOD);
    }

    public static boolean checkProcEmpty(ThreadContext context) {
        return check(context, PROC_CLASS, EMPTY_P_METHOD);
    }

    public static boolean checkProcNil(ThreadContext context) {
        return check(context, PROC_CLASS, NIL_P_METHOD);
    }

    public static boolean checkProcSucc(ThreadContext context) {
        return check(context, PROC_CLASS, SUCC_METHOD);
    }

    public static boolean checkProcGt(ThreadContext context) {
        return check(context, PROC_CLASS, GT_METHOD);
    }

    public static boolean checkProcGe(ThreadContext context) {
        return check(context, PROC_CLASS, GE_METHOD);
    }

    public static boolean checkProcGtgt(ThreadContext context) {
        return check(context, PROC_CLASS, GTGT_METHOD);
    }

    public static boolean checkProcNot(ThreadContext context) {
        return check(context, PROC_CLASS, NOT_METHOD);
    }

    public static boolean checkProcNeq(ThreadContext context) {
        return check(context, PROC_CLASS, NEQ_METHOD);
    }

    public static boolean checkProcMatch(ThreadContext context) {
        return check(context, PROC_CLASS, MATCH_METHOD);
    }

    public static boolean checkProcFreeze(ThreadContext context) {
        return check(context, PROC_CLASS, FREEZE_METHOD);
    }

    public static boolean checkProcUminus(ThreadContext context) {
        return check(context, PROC_CLASS, UMINUS_METHOD);
    }

    public static boolean checkProcMax(ThreadContext context) {
        return check(context, PROC_CLASS, MAX_METHOD);
    }

    public static boolean checkProcMin(ThreadContext context) {
        return check(context, PROC_CLASS, MIN_METHOD);
    }

    public static boolean checkProcHash(ThreadContext context) {
        return check(context, PROC_CLASS, HASH_METHOD);
    }

    public static boolean checkProcCall(ThreadContext context) {
        return check(context, PROC_CLASS, CALL_METHOD);
    }

    public static boolean checkProcAnd(ThreadContext context) {
        return check(context, PROC_CLASS, AND_METHOD);
    }

    public static boolean checkProcOr(ThreadContext context) {
        return check(context, PROC_CLASS, OR_METHOD);
    }

    public static boolean checkProcCmp(ThreadContext context) {
        return check(context, PROC_CLASS, CMP_METHOD);
    }

    public static boolean checkProcDefault(ThreadContext context) {
        return check(context, PROC_CLASS, DEFAULT_METHOD);
    }

    public static boolean checkProcPack(ThreadContext context) {
        return check(context, PROC_CLASS, PACK_METHOD);
    }

    public static boolean checkProcInclude(ThreadContext context) {
        return check(context, PROC_CLASS, INCLUDE_P_METHOD);
    }

    public static boolean checkRationalPlus(ThreadContext context) {
        return check(context, RATIONAL_CLASS, PLUS_METHOD);
    }

    public static boolean checkRationalMinus(ThreadContext context) {
        return check(context, RATIONAL_CLASS, MINUS_METHOD);
    }

    public static boolean checkRationalMult(ThreadContext context) {
        return check(context, RATIONAL_CLASS, MULT_METHOD);
    }

    public static boolean checkRationalDiv(ThreadContext context) {
        return check(context, RATIONAL_CLASS, DIV_METHOD);
    }

    public static boolean checkRationalMod(ThreadContext context) {
        return check(context, RATIONAL_CLASS, MOD_METHOD);
    }

    public static boolean checkRationalEquals(ThreadContext context) {
        return check(context, RATIONAL_CLASS, EQ_METHOD);
    }

    public static boolean checkRationalEqq(ThreadContext context) {
        return check(context, RATIONAL_CLASS, EQQ_METHOD);
    }

    public static boolean checkRationalLt(ThreadContext context) {
        return check(context, RATIONAL_CLASS, LT_METHOD);
    }

    public static boolean checkRationalLe(ThreadContext context) {
        return check(context, RATIONAL_CLASS, LE_METHOD);
    }

    public static boolean checkRationalLtlt(ThreadContext context) {
        return check(context, RATIONAL_CLASS, LTLT_METHOD);
    }

    public static boolean checkRationalAref(ThreadContext context) {
        return check(context, RATIONAL_CLASS, AREF_METHOD);
    }

    public static boolean checkRationalAset(ThreadContext context) {
        return check(context, RATIONAL_CLASS, ASET_METHOD);
    }

    public static boolean checkRationalLength(ThreadContext context) {
        return check(context, RATIONAL_CLASS, LENGTH_METHOD);
    }

    public static boolean checkRationalSize(ThreadContext context) {
        return check(context, RATIONAL_CLASS, SIZE_METHOD);
    }

    public static boolean checkRationalEmpty(ThreadContext context) {
        return check(context, RATIONAL_CLASS, EMPTY_P_METHOD);
    }

    public static boolean checkRationalNil(ThreadContext context) {
        return check(context, RATIONAL_CLASS, NIL_P_METHOD);
    }

    public static boolean checkRationalSucc(ThreadContext context) {
        return check(context, RATIONAL_CLASS, SUCC_METHOD);
    }

    public static boolean checkRationalGt(ThreadContext context) {
        return check(context, RATIONAL_CLASS, GT_METHOD);
    }

    public static boolean checkRationalGe(ThreadContext context) {
        return check(context, RATIONAL_CLASS, GE_METHOD);
    }

    public static boolean checkRationalGtgt(ThreadContext context) {
        return check(context, RATIONAL_CLASS, GTGT_METHOD);
    }

    public static boolean checkRationalNot(ThreadContext context) {
        return check(context, RATIONAL_CLASS, NOT_METHOD);
    }

    public static boolean checkRationalNeq(ThreadContext context) {
        return check(context, RATIONAL_CLASS, NEQ_METHOD);
    }

    public static boolean checkRationalMatch(ThreadContext context) {
        return check(context, RATIONAL_CLASS, MATCH_METHOD);
    }

    public static boolean checkRationalFreeze(ThreadContext context) {
        return check(context, RATIONAL_CLASS, FREEZE_METHOD);
    }

    public static boolean checkRationalUminus(ThreadContext context) {
        return check(context, RATIONAL_CLASS, UMINUS_METHOD);
    }

    public static boolean checkRationalMax(ThreadContext context) {
        return check(context, RATIONAL_CLASS, MAX_METHOD);
    }

    public static boolean checkRationalMin(ThreadContext context) {
        return check(context, RATIONAL_CLASS, MIN_METHOD);
    }

    public static boolean checkRationalHash(ThreadContext context) {
        return check(context, RATIONAL_CLASS, HASH_METHOD);
    }

    public static boolean checkRationalCall(ThreadContext context) {
        return check(context, RATIONAL_CLASS, CALL_METHOD);
    }

    public static boolean checkRationalAnd(ThreadContext context) {
        return check(context, RATIONAL_CLASS, AND_METHOD);
    }

    public static boolean checkRationalOr(ThreadContext context) {
        return check(context, RATIONAL_CLASS, OR_METHOD);
    }

    public static boolean checkRationalCmp(ThreadContext context) {
        return check(context, RATIONAL_CLASS, CMP_METHOD);
    }

    public static boolean checkRationalDefault(ThreadContext context) {
        return check(context, RATIONAL_CLASS, DEFAULT_METHOD);
    }

    public static boolean checkRationalPack(ThreadContext context) {
        return check(context, RATIONAL_CLASS, PACK_METHOD);
    }

    public static boolean checkRationalInclude(ThreadContext context) {
        return check(context, RATIONAL_CLASS, INCLUDE_P_METHOD);
    }

    public static boolean checkRationalToF(ThreadContext context) {
        return check(context, RATIONAL_CLASS, TO_F_METHOD);
    }

    public static boolean checkStructDig(ThreadContext context) {
        return check(context, STRUCT_CLASS, DIG_METHOD);
    }
    
    public static void invalidateBuiltin(short[] bits, ClassIndex classIndex, String method) {
        int methodId;
        if (classIds.get(classIndex) instanceof Integer classId && (methodId = methodIds.get(method)) != -1) {
            bits[methodId] |= classId;
        }
    }
}
