package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class FilenameInstruction extends NoOperandInstr {
    public final String filename;

    public FilenameInstruction(String filename) {
        super(Operation.FILE_NAME);
        this.filename = filename;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + filename + ")";
    }

    public IR_Instr cloneForInlining(InlinerInfo ii) { return this; }
}
