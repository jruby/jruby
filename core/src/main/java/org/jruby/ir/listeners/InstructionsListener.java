package org.jruby.ir.listeners;

import java.util.List;
import org.jruby.ir.instructions.Instr;

public interface InstructionsListener {
    
    public enum OperationType {
        ADD,
        REMOVE,
        UPDATE
    }
    
    public void instrChanged(List<Instr> instrs, Instr instr, int index, OperationType type);
    
}
