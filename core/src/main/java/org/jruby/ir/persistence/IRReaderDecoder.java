/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ir.persistence;

import org.jcodings.Encoding;
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

import java.util.List;
import java.util.Map;
import org.jruby.util.ByteList;

/**
 *
 * @author enebo
 */
public interface IRReaderDecoder {
    public String decodeString();
    public String[] decodeStringArray();
    public Instr decodeInstr();
    public IRScopeType decodeIRScopeType();
    public StaticScope.Type decodeStaticScopeType();
    public Operation decodeOperation();
    public Operand decodeOperand();
    public List<Operand> decodeOperandList();
    public Operand[] decodeOperandArray();
    public OperandType decodeOperandType();
    public boolean decodeBoolean();
    public byte decodeByte();
    public byte[] decodeByteArray();
    public Encoding decodeEncoding();
    public ByteList decodeByteList();
    public char decodeChar();
    public int decodeInt();
    public int decodeIntRaw();
    public long decodeLong();
    public double decodeDouble();
    public float decodeFloat();
    public Label decodeLabel();

    public Variable decodeVariable();

    public List<Instr> decodeInstructionsAt(IRScope scope, int offset);
    public IRScope getCurrentScope();
    public Map<String, Operand> getVars();

    public void addScope(IRScope scope);
    public void seek(int headersOffset);

    public IRScope decodeScope();

    public TemporaryVariableType decodeTemporaryVariableType();
}
