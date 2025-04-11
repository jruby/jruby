/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ir.persistence;

import org.jcodings.Encoding;
import org.jruby.RubySymbol;
import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScopeType;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.OperandType;
import org.jruby.ir.operands.TemporaryVariableType;
import org.jruby.ir.operands.Variable;
import org.jruby.parser.StaticScope;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.Signature;
import org.jruby.util.ByteList;

public interface IRReaderDecoder {
    String decodeString();
    String[] decodeStringArray();
    int[] decodeIntArray();
    Instr decodeInstr();
    IRScopeType decodeIRScopeType();
    StaticScope.Type decodeStaticScopeType();
    Operation decodeOperation();
    Operand decodeOperand();
    List<Operand> decodeOperandList();
    Label decodeLabel();
    Label[] decodeLabelArray();
    Operand[] decodeOperandArray();
    OperandType decodeOperandType();
    boolean decodeBoolean();
    byte decodeByte();
    byte[] decodeByteArray();
    Encoding decodeEncoding();
    ByteList decodeByteList();
    char decodeChar();
    int decodeInt();
    int decodeIntRaw();
    long decodeLong();
    double decodeDouble();
    float decodeFloat();
    RubyEvent decodeRubyEvent();
    RubySymbol decodeSymbol();
    Signature decodeSignature();
    EnumSet<IRFlags> decodeIRFlags();

    Variable decodeVariable();

    List<Instr> decodeInstructionsAt(IRScope scope, int poolOffset, int instructionOffset);
    IRScope getCurrentScope();
    Map<String, Operand> getVars();

    void addScope(IRScope scope);
    void seek(int headersOffset);

    IRScope decodeScope();

    TemporaryVariableType decodeTemporaryVariableType();
    String getFilename();

    /**
     * Duplicate this decoder to isolate any state changes.
     *
     * @return An identical decoder that's isolated from the original
     */
    IRReaderDecoder dup();
}
