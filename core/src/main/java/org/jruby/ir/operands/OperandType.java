/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ir.operands;

public enum OperandType {

    ARRAY("ary", (byte) 'A'),
    BIGNUM("big", (byte) 'B'),
    BOOLEAN("bool", (byte) 'b'),
    COMPLEX("com", (byte) 'C'),
    CURRENT_SCOPE("scope", (byte) 's'),
    DYNAMIC_SYMBOL("dsym", (byte) 'd'),
    FIXNUM("fix", (byte) 'f'),
    FLOAT("flo", (byte) 'F'),
    GLOBAL_VARIABLE("$", (byte) '$'),
    HASH("hash", (byte) '{'),
    IR_EXCEPTION("ir_ex", (byte) '!'),
    LABEL("ipc", (byte) 'L'),
    LOCAL_VARIABLE("*", (byte) 'l'), // Also applicable for ClosureLocalVariable
    NIL("nil", (byte) 'N'),
    NTH_REF("nth", (byte) '1'),
    OBJECT_CLASS("objcls", (byte) 'O'),
    RANGE("rng", (byte) '.'),
    RATIONAL("rat", (byte) 'r'),
    REGEXP("reg", (byte) '/'),
    SCOPE_MODULE("mod", (byte) '_'),
    SELF("self", (byte) 'S'),
    SPLAT("splat", (byte) '*'),
    STANDARD_ERROR("stderr", (byte) 'E'),
    STRING_LITERAL("str", (byte) '\''),
    SVALUE("sval", (byte) 'V'),
    SYMBOL("sym", (byte) ':'),
    TEMPORARY_VARIABLE("reg", (byte) 't'),
    UNBOXED_BOOLEAN("rawbool", (byte) 'v'),
    UNBOXED_FIXNUM("rawfix", (byte) 'j'),
    UNBOXED_FLOAT("rawflo", (byte) 'J'),
    UNDEFINED_VALUE("undef", (byte) 'u'),
    UNEXECUTABLE_NIL("noex", (byte) 'n'),
    WRAPPED_IR_CLOSURE("block", (byte) 'w'),
    FROZEN_STRING("fstr", (byte) 'z'),
    NULL_BLOCK("noblock", (byte) 'o'),
    FILENAME("file", (byte) 'm'),
    SYMBOL_PROC("symproc", (byte) 'P'),
    SCOPE("scope", (byte) '#')
    ;

    private final String shortName;
    private final byte coded;
    private static final OperandType[] byteToOperand = new OperandType[256];

    OperandType(String shortName, byte coded) {
        this.shortName = shortName;
        this.coded = coded;
    }

    public byte getCoded() {
        return coded;
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    };

    public String shortName() {
        return shortName;
    }

    public static OperandType fromCoded(byte coded) {
        return byteToOperand[coded];
    }

    public static OperandType fromOrdinal(int value) {
        return value < 0 || value >= values().length ? null : values()[value];
    }

    static {
        for (OperandType type : values()) {
            byteToOperand[type.coded] = type;
        }
    }
}

