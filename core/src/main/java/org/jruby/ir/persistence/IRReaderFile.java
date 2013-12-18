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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jruby.ir.IRManager;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScopeType;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.OperandType;
import org.jruby.parser.StaticScope;

/**
 *
 * @author enebo
 */
public class IRReaderFile implements IRReaderDecoder, IRPersistenceValues {
    private ByteBuffer buf;
    private final InstrDecoderMap instrDecoderMap;
    private final OperandDecoderMap operandDecoderMap;

    public IRReaderFile(IRManager manager, File file) {
        try {
            buf = new FileInputStream(file).getChannel().map(FileChannel.MapMode.READ_ONLY, 0, TWO_MEGS);
        } catch (IOException ex) {
            Logger.getLogger(IRReaderFile.class.getName()).log(Level.SEVERE, null, ex);
            
        }
        
        instrDecoderMap = new InstrDecoderMap(manager, this);
        operandDecoderMap = new OperandDecoderMap(manager, this);
    }
    
    @Override
    public String decodeString() {
        int strLength = buf.getInt();
        byte[] bytes = new byte[strLength]; // FIXME: This seems really innefficient
        buf.get(bytes);
        return new String(bytes);
    }

    @Override
    public String[] decodeStringArray() {
        int arrayLength = buf.getInt();
        String[] array = new String[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
            array[i] = decodeString();
        }
        return array;
    }

    @Override
    public Instr decodeInstr() {
        return instrDecoderMap.decode(decodeOperation());
    }

    @Override
    public IRScopeType decodeIRScopeType() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public StaticScope.Type decodeStaticScopeType() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Operation decodeOperation() {
        return instrDecoderMap.decodeOperationType((int) buf.get());
    }

    @Override
    public Operand decodeOperand() {
        return operandDecoderMap.decode(decodeOperandType());
    }
    
    @Override
    public List<Operand> decodeOperandList() {
        int size = decodeInt();
        List<Operand> list = new ArrayList<Operand>(size);
        
        for (int i = 0; i < size; i++) {
            list.add(decodeOperand());
        }
        
        return list;
    }
    
    @Override
    public OperandType decodeOperandType() {
        return operandDecoderMap.decodeOperandType((int) buf.get());
    }

    @Override
    public boolean decodeBoolean() {
        byte value = buf.get();
        if (value == TRUE) return true;
        if (value == FALSE) return false;
        
        throw new IllegalArgumentException("Value (" + ((int) value) + ") is not a boolean.");
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
        return buf.getInt();
    }

    @Override
    public long decodeLong() {
        return buf.getLong();
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
    public void seek(int headersOffset) {
        buf.position(headersOffset);
    }

    @Override
    public IRScope decodeScope() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
