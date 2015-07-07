/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ir.operands;

public enum OperandType {

    ARRAY((byte) 'A'),
    AS_STRING((byte) 'a'),
    BACKREF((byte) '\\'),
    BACKTICK_STRING((byte) '`'),
    BIGNUM((byte) 'B'),
    BOOLEAN((byte) 'b'),
    LOCAL_VARIABLE((byte) 'l'), // Also applicable for ClosureLocalVariable
    COMPLEX((byte) 'C'),
    COMPOUND_ARRAY((byte) 'c'),
    COMPOUND_STRING((byte) '"'),
    CURRENT_SCOPE((byte) 's'),
    DYNAMIC_SYMBOL((byte) 'd'),
    FIXNUM((byte) 'f'),
    FLOAT((byte) 'F'),
    GLOBAL_VARIABLE((byte) '$'),
    HASH((byte) '{'),
    IR_EXCEPTION((byte) '!'),
    LABEL((byte) 'L'),
    NIL((byte) 'N'),
    NTH_REF((byte) '1'),
    OBJECT_CLASS((byte) 'O'),
    RANGE((byte) '.'),
    RATIONAL((byte) 'r'),
    REGEXP((byte) '/'),
    SCOPE_MODULE((byte) '_'),
    SELF((byte) 'S'),
    SPLAT((byte) '*'),
    STANDARD_ERROR((byte) 'E'),
    STRING_LITERAL((byte) '\''),
    SVALUE((byte) 'V'),
    SYMBOL((byte) ':'),
    TEMPORARY_VARIABLE((byte) 't'),
    UNBOXED_BOOLEAN((byte) 'v'),
    UNBOXED_FIXNUM((byte) 'j'),
    UNBOXED_FLOAT((byte) 'J'),
    UNDEFINED_VALUE((byte) 'u'),
    UNEXECUTABLE_NIL((byte) 'n'),
    WRAPPED_IR_CLOSURE((byte) 'w'),
    FROZEN_STRING((byte) 'z'),
    NULL_BLOCK((byte) 'o'),
    FILENAME((byte) 'm')
    ;

    private final byte coded;
    private static final OperandType[] byteToOperand = new OperandType[256];

    OperandType(byte coded) {
        this.coded = coded;
    }

    public byte getCoded() {
        return coded;
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    };

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

