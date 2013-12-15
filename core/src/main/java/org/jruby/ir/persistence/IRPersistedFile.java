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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jruby.ir.IRScope;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.Operand;
import static org.jruby.ir.persistence.IRWriter.NULL;

// FIXME: Make into a base class at some point to play with different formats

/**
 * Represents a file which is persisted to storage. 
 */
public class IRPersistedFile implements IRWriterEncoder {
    private final Map<IRScope, Integer> scopeInstructionOffsets = new HashMap<IRScope, Integer>();
    private int offset = 0;
    private final FileOutputStream io;
    
    public IRPersistedFile(File file) throws FileNotFoundException {
        io = new FileOutputStream(file);
    }
    
    /**
     * Record current offset as the beginning of specified scopes list of instructions.
     */
    public void addScopeInstructionOffset(IRScope scope) {
        scopeInstructionOffsets.put(scope, offset());
    }
    
    /**
     * Get recorded offset for this scropes instruction list.
     */
    public int getScopeInstructionOffset(IRScope scope) {
        return scopeInstructionOffsets.get(scope);
    }
    
    /**
     * Where are we within this persisted unit at the moment.
     */
    public int offset() {
        return offset;
    }
    
    @Override
    public void encode(String[] values) {
        if (values == null) {
            encode(NULL);
            return;
        }
        
        encode(values.length);
        for (String value : values) {
            encode(value);
        }
    }
    
    @Override
    public void encode(boolean value) {
        encode(Boolean.toString(value));
    }
    
    @Override
    public void encode(int value) {
        encode(Integer.toString(value));
    }

    @Override
    public void encode(String value) {
        try {
            byte[] bytes = value.getBytes();
            io.write(bytes);
            offset += bytes.length;
            io.write(',');
            offset += 1;
        } catch (IOException e) {
            // error handling
        }
    }
    
    public void encode(Operand[] operands) {
        encode(operands.length);
        for (Operand operand: operands) {
            encode(operand.toString());
        }
    }
    
    public void encode(Instr instr) {
        encode(instr.getOperation());
        encode(instr.getOperands());
    }
    
    public void encode(IRPersistableEnum scopeType) {
        encode(((Enum) scopeType).toString());
    }

    @Override
    public void commit() {
        try {
            io.close();
        } catch (IOException ex) {
            Logger.getLogger(IRPersistedFile.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void encode(long value) {
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
    }

    @Override
    public void endEncodingScopeHeaders(IRScope script) {
    }
}
