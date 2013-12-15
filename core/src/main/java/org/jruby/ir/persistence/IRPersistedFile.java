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
import org.jruby.ir.operands.Fixnum;
import org.jruby.ir.operands.Operand;
import static org.jruby.ir.persistence.IRWriter.NULL;

// FIXME: Make into a base class at some point to play with different formats

/**
 * Represents a file which is persisted to storage. 
 */
public class IRPersistedFile {
    private final Map<Operand, Integer> counts = new HashMap<Operand, Integer>();
    private final Map<IRScope, Integer> scopeInstructionOffsets = new HashMap<IRScope, Integer>();
    private int offset = 0;
    private FileOutputStream io;
    
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
    
    public void write(String[] values) {
        if (values == null) {
            write(NULL);
            return;
        }
        
        write(values.length);
        for (String value : values) {
            write(value);
        }
    }
    
    public void write(boolean value) {
        write(Boolean.toString(value));
    }
    
    public void write(int value) {
        write(Integer.toString(value));
    }

    public void write(String value) {
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
    
    public void write(Operand[] operands) {
        write(operands.length);
        for (Operand operand: operands) {
            increment(operand);
            write(operand.toString());
        }
    }
    
    public void write(Instr instr) {
        write(instr.getOperation());
        write(instr.getOperands());
    }
    
    public void write(IRPersistableEnum scopeType) {
        write(((Enum) scopeType).toString());
    }

    void commit() {
        write("\n");
        for (Operand operand : counts.keySet()) {
            if (!(operand instanceof Fixnum)) {
            write(operand.getClass().getName());
            write(operand.toString());
            write(counts.get(operand));
            write("\n");
            }
        }
        try {
            io.close();
        } catch (IOException ex) {
            Logger.getLogger(IRPersistedFile.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private void increment(Operand operand) {
        Integer count = counts.get(operand);
        if (count == null) count = new Integer(0);
        
        counts.put(operand, count + 1);
    }
}
