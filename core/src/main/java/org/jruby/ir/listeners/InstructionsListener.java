package org.jruby.ir.listeners;

import java.util.List;
import org.jruby.ir.instructions.Instr;

public interface InstructionsListener {
    
    public enum OperationType {
        ADD,
        REMOVE,
        UPDATE
    }
    
    /**
     * listen to a change of a list of instructions, right before the change going to take place
     * 
     * @param instrs List of instructions before the change happens
     * @param oldInstr If possible, specify which instruction is the old one at index
     * @param newInstr If possible, this the new element inserted at index
     * @param index where the manipulation takes place
     * @param op the operation type can be an ADD, REMOVE or UPDATE 
     */
    public void instrChanged(List<Instr> instrs, Instr oldInstr, Instr newInstr, int index, OperationType op);
    
}
