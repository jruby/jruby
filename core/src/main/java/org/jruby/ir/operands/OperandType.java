/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ir.operands;

import org.jruby.ir.persistence.flat.OperandUnion;

public enum OperandType {

    ARRAY("ary", (byte) 'A'),
    AS_STRING("tostr", (byte) 'a'),
    BIGNUM("big", (byte) 'B'),
    BOOLEAN("bool", (byte) 'b'),
    COMPLEX("com", (byte) 'C'),
    CURRENT_SCOPE("scope", (byte) 's', OperandUnion.CurrentScopeFlat),
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
    SCOPE_MODULE("mod", (byte) '_', OperandUnion.ScopeModuleFlat),
    SELF("self", (byte) 'S', OperandUnion.SelfFlat),
    SPLAT("splat", (byte) '*'),
    STANDARD_ERROR("stderr", (byte) 'E'),
    STRING_LITERAL("str", (byte) '\'', OperandUnion.StringLiteralFlat),
    SVALUE("sval", (byte) 'V'),
    SYMBOL("sym", (byte) ':'),
    TEMPORARY_VARIABLE("reg", (byte) 't', OperandUnion.TemporaryVariableFlat),
    UNBOXED_BOOLEAN("rawbool", (byte) 'v'),
    UNBOXED_FIXNUM("rawfix", (byte) 'j'),
    UNBOXED_FLOAT("rawflo", (byte) 'J'),
    UNDEFINED_VALUE("undef", (byte) 'u'),
    UNEXECUTABLE_NIL("noex", (byte) 'n'),
    WRAPPED_IR_CLOSURE("block", (byte) 'w'),
    FROZEN_STRING("fstr", (byte) 'z', OperandUnion.FrozenStringFlat),
    NULL_BLOCK("noblock", (byte) 'o'),
    FILENAME("file", (byte) 'm'),
    SYMBOL_PROC("symproc", (byte) 'P')
    ;

    private final String shortName;
    private final byte coded;
    private final byte flat;
    private static final OperandType[] byteToOperand = new OperandType[256];

    private static final OperandType[] FLAT_MAP = new OperandType[OperandType.values().length];

    static {
        for (OperandType opType : OperandType.values()) {
            if (opType.flat == -1) continue;
            FLAT_MAP[opType.flat] = opType;
        }
    }

    public static OperandType flatMap(byte flat) {
        return FLAT_MAP[flat];
    }

    OperandType(String shortName, byte coded, byte flat) {
        this.shortName = shortName;
        this.coded = coded;
        this.flat = flat;
    }

    OperandType(String shortName, byte coded) {
        this(shortName, coded, (byte) -1);
    }

    public byte getCoded() {
        return coded;
    }

    public byte getFlat() {
        return flat;
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

