/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ir.persistence;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jruby.RubyInstanceConfig;
import org.jruby.ir.IRManager;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScopeType;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.OperandType;
import org.jruby.ir.operands.TemporaryVariableType;
import org.jruby.ir.operands.Variable;
import org.jruby.parser.StaticScope;

/**
 *
 * @author enebo
 */
public class IRReaderFile implements IRReaderDecoder, IRPersistenceValues {
    private ByteBuffer buf;
    private final InstrDecoderMap instrDecoderMap;
    private final OperandDecoderMap operandDecoderMap;
    private final List<IRScope> scopes = new ArrayList<IRScope>();
    private IRScope currentScope = null; // FIXME: This is not thread-safe and more than a little gross

    public IRReaderFile(IRManager manager, File file) {
        try {
            byte[] bytes = new byte[(int)file.length()];
            if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("READING IN " + bytes.length + " BYTES OF DATA FROM " + file);
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            FileInputStream fis = new FileInputStream(file);
            FileChannel fc = fis.getChannel();
            fc.read(buffer);
            fis.close();
            buf = ByteBuffer.wrap(bytes);
        } catch (IOException ex) {
            Logger.getLogger(IRReaderFile.class.getName()).log(Level.SEVERE, null, ex);

        }

        instrDecoderMap = new InstrDecoderMap(manager, this);
        operandDecoderMap = new OperandDecoderMap(manager, this);
    }

    @Override
    public String decodeString() {
        int strLength = decodeInt();
        byte[] bytes = new byte[strLength]; // FIXME: This seems really innefficient
        buf.get(bytes);
        
        return new String(bytes).intern();
    }

    @Override
    public void addScope(IRScope scope) {
        scopes.add(scope);
    }

    @Override
    public IRScope getCurrentScope() {
        return currentScope;
    }

    @Override
    public String[] decodeStringArray() {
        int arrayLength = decodeInt();
        String[] array = new String[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
            array[i] = decodeString();
        }
        return array;
    }

    private Map<String, Operand> vars = null;

    @Override
    public Map<String, Operand> getVars() {
        return vars;
    }

    @Override
    public List<Instr> decodeInstructionsAt(IRScope scope, int offset) {
        this.currentScope = scope;
        vars = new HashMap<String, Operand>();
        buf.position(offset);
        int numberOfInstructions = decodeInt();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("Number of Instructions: " + numberOfInstructions);
        List<Instr> instrs = new ArrayList(numberOfInstructions);

        for (int i = 0; i < numberOfInstructions; i++) {
            instrs.add(decodeInstr());
        }

        return instrs;
    }

    @Override
    public Instr decodeInstr() {
        return instrDecoderMap.decode(decodeOperation());
    }

    @Override
    public IRScopeType decodeIRScopeType() {
        return IRScopeType.fromOrdinal(decodeInt());
    }

    @Override
    public TemporaryVariableType decodeTemporaryVariableType() {
        return TemporaryVariableType.fromOrdinal(decodeInt());
    }
    
    @Override
    public StaticScope.Type decodeStaticScopeType() {
        return StaticScope.Type.fromOrdinal(decodeInt());
    }

    @Override
    public Operation decodeOperation() {
        Operation operation = Operation.fromOrdinal(decodeInt());
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("INSTR OP: " + operation);
        return operation;
    }

    @Override
    public Operand decodeOperand() {
        return operandDecoderMap.decode(decodeOperandType());
    }

    @Override
    public Variable decodeVariable() {
        return (Variable) decodeOperand();
    }

    @Override
    public Operand[] decodeOperandArray() {
        int size = decodeInt();
        Operand[] list = new Operand[size];

        for (int i = 0; i < size; i++) {
            list[i] = decodeOperand();
        }

        return list;
    }

    @Override
    public List<Operand> decodeOperandList() {
        int size = decodeInt();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("OPERAND LIST of size: " + size);
        List<Operand> list = new ArrayList<Operand>(size);

        for (int i = 0; i < size; i++) {
            if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("OPERAND #" + i);
            list.add(decodeOperand());
        }

        return list;
    }

    @Override
    public OperandType decodeOperandType() {
        return OperandType.fromCoded(decodeByte());
    }

    @Override
    public boolean decodeBoolean() {
        byte value = buf.get();
        if (value == TRUE) return true;
        if (value == FALSE) return false;

        throw new IllegalArgumentException("Value (" + ((int) value) + ", " + (char) value + ") is not a boolean.");
    }

    @Override
    public byte decodeByte() {
        return buf.get();
    }

    @Override
    public char decodeChar() {
        return buf.getChar();
    }

    @Override
    public int decodeInt() {
        byte b = buf.get();
        return b == FULL ? buf.getInt() : (int) b;
    }

    @Override
    public int decodeIntRaw() {
        return buf.getInt();
    }

    @Override
    public long decodeLong() {
        byte b = buf.get();
        return b == FULL ? buf.getLong() : (int) b;
    }

    @Override
    public double decodeDouble() {
        return buf.getDouble();
    }

    @Override
    public float decodeFloat() {
        return buf.getFloat();
    }

    @Override
    public IRScope decodeScope() {
        return scopes.get(decodeInt());
    }

    @Override
    public void seek(int headersOffset) {
        buf.position(headersOffset);
    }
}
