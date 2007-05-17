/**
 * 
 */
package org.jruby.evaluator;

public class InstructionBundle {
    Instruction instruction;
    InstructionContext instructionContext;
    InstructionBundle nextInstruction;
    
    boolean ensured;
    boolean redoable;
    boolean breakable;
    boolean rescuable;
    boolean retriable;
    
    public InstructionBundle(Instruction i, InstructionContext ic) {
        instruction = i;
        instructionContext = ic;
    }
}
