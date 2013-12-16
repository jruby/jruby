/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ir.persistence;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScopeType;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.FixedArityInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.OperandType;
import org.jruby.parser.StaticScope;

// FIXME: Make into a base class at some point to play with different formats

/**
 * Represents a file which is persisted to storage. 
 */
public class IRPersistedFile implements IRWriterEncoder {
    public final static int TWO_MEGS = 1024 * 1024 * 2;
    public final static int PRIMITIVE_BASE = OperandType.values().length; // OPERANDS and base data is 1 byte
    public final static byte STRING = (byte) (PRIMITIVE_BASE + 1);
    public final static byte INT = (byte) (PRIMITIVE_BASE + 2);
    public final static byte TRUE = (byte) (PRIMITIVE_BASE + 3);
    public final static byte FALSE = (byte) (PRIMITIVE_BASE + 4);
    public final static byte ARRAY = (byte) (PRIMITIVE_BASE + 5);
    public final static byte NULL = (byte) (PRIMITIVE_BASE + 6);
    public final static byte INSTR = (byte) (PRIMITIVE_BASE + 7); // INSTRs 2 bytes
    public final static byte LONG = (byte) (PRIMITIVE_BASE + 8);
    private static final int VERSION = 0;
    
    private final Map<IRScope, Integer> scopeInstructionOffsets = new HashMap<IRScope, Integer>();
    // FIXME: Allocate direct and use one per thread?
    private final ByteBuffer buf = ByteBuffer.allocate(TWO_MEGS);
    private final File file;
    
    int headersOffset = -1;
    int poolOffset = -1;
    
    public IRPersistedFile(File file) throws FileNotFoundException {
        this.file = file;
    }
    
    /**
     * Record current offset as the beginning of specified scopes list of instructions.
     */
    public void addScopeInstructionOffset(IRScope scope) {
        scopeInstructionOffsets.put(scope, buf.position());
    }
    
    /**
     * Get recorded offset for this scropes instruction list.
     */
    public int getScopeInstructionOffset(IRScope scope) {
        return scopeInstructionOffsets.get(scope);
    }
    
    // This cannot tell difference between null and [] which is ok.  Possibly we should even allow
    // encoding null.
    @Override
    public void encode(String[] values) {
        buf.put(ARRAY);
        buf.put(STRING);
        if (values == null) {
            buf.putInt(0);
            return;
        }
        
        buf.putInt(values.length);
        for (String value : values) {
            buf.putInt(value.length());
            buf.put(value.getBytes());
        }
    }
    
    @Override
    public void encode(boolean value) {
        buf.put(value ? TRUE : FALSE);
    }
    
    @Override
    public void encode(int value) {
        buf.put(INT);
        buf.putInt(value);
    }

    @Override
    public void encode(String value) {
        buf.put(STRING);
        buf.putInt((byte) value.length());
        buf.put(value.getBytes());
    }
    
    @Override
    public void encode(Operand operand) {
        buf.put((byte) operand.getOperandType().ordinal());
        OperandEncoderMap.encode(this, operand);
    }
    
    @Override
    public void encode(Instr instr) {
        encode(instr.getOperation());
        if (instr instanceof ResultInstr) encode(((ResultInstr) instr).getResult());

        Operand[] operands = instr.getOperands();
        if (!(instr instanceof FixedArityInstr)) buf.put((byte) operands.length);    

        for (Operand operand: operands) {
            encode(operand);
        }
    }

    @Override
    public void encode(IRScopeType value) {
        buf.put((byte) value.ordinal());
    }

    @Override
    public void encode(StaticScope.Type value) {
        buf.put((byte) value.ordinal());
    }

    @Override
    public void encode(Operation value) {
        buf.put((byte) value.ordinal());
    }

    @Override
    public void encode(OperandType value) {
        buf.put((byte) value.ordinal());
    }    

    @Override
    public void encode(long value) {
        buf.put(LONG);
        buf.putLong(value);
    }

    @Override
    public void startEncodingScopeHeader(IRScope scope) {
    }

    @Override
    public void endEncodingScopeHeader(IRScope scope) {
        encode(getScopeInstructionOffset(scope)); // Write out offset to where this scopes instrs are
    }

    @Override
    public void startEncodingScopeInstrs(IRScope scope) {
        addScopeInstructionOffset(scope); // Record offset so we add this value to scope headers entry
        encode(scope.getInstrs().size()); // Allows us to right-size when reconstructing instr list.
    }

    @Override
    public void endEncodingScopeInstrs(IRScope scope) {
    }

    @Override
    public void startEncodingScopeHeaders(IRScope script) {
        headersOffset = buf.position();
    }

    @Override
    public void endEncodingScopeHeaders(IRScope script) {
    }
    
    @Override
    public void startEncoding(IRScope script) {
    }

    @Override
    public void endEncoding(IRScope script) {
        FileOutputStream fos = null;
        
        try {
            fos = new FileOutputStream(file);
            fos.write(ByteBuffer.allocate(4).putInt(headersOffset).array());
            fos.write(ByteBuffer.allocate(4).putInt(poolOffset).array());
            buf.flip();
            fos.getChannel().write(buf);
            fos.close();            
        } catch (IOException e) {
            try { if (fos != null) fos.close(); } catch (IOException e1) {}
        }
    }
}
