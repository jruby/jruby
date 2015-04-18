/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ir.persistence;

import org.jruby.ir.operands.OperandType;

/**
 *
 * @author enebo
 */
interface IRPersistenceValues {
    public final static int TWO_MEGS = 1024 * 1024 * 2;

    // Operands and primitive values can be mixed together
    public final static int PRIMITIVE_BASE = OperandType.values().length; // OPERANDS and base data is 1 byte
    public final static byte STRING = (byte) (PRIMITIVE_BASE + 1);

    public final static byte TRUE = (byte) 't';
    public final static byte FALSE = (byte) 'f';
    public final static byte ARRAY = (byte) (PRIMITIVE_BASE + 5);
    public final static byte NULL = (byte) (PRIMITIVE_BASE + 6);
    public final static byte INSTR = (byte) (PRIMITIVE_BASE + 7); // INSTRs 2 bytes
    public final static byte LONG = (byte) (PRIMITIVE_BASE + 8);
    public final static byte FLOAT = (byte) (PRIMITIVE_BASE + 9);
    public final static byte DOUBLE = (byte) (PRIMITIVE_BASE + 10);
    public final static byte FULL = (byte) 255;

    public final static int PROLOGUE_LENGTH = 2 * 4; // 2 ints at front

    public final static int NULL_STRING = -1;
}
