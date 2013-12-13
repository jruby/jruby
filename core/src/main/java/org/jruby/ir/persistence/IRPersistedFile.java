/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ir.persistence;

import java.util.HashMap;
import java.util.Map;
import org.jruby.ir.IRScope;
import org.jruby.ir.operands.Operand;
import static org.jruby.ir.persistence.IRWriter.NULL;

// FIXME: Make into a base class at some point to play with different formats

/**
 * Represents a file which is persisted to storage. 
 */
public class IRPersistedFile {
    private Map<IRScope, Integer> scopeInstructionOffsets = new HashMap<IRScope, Integer>();
    private int offset = 0;
    
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
    }
    
    public void write(int value) {
    }

    public void write(String variableEncode) {
    }
    
    public void write(Operand[] operands) {
    }

    void commit() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
