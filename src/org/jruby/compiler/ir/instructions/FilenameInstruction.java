package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;

public class FilenameInstruction extends NoOperandInstr {
    public final String filename;

    public FilenameInstruction(String filename) {
        super(Operation.FILE_NAME);
        this.filename = filename;
    }

    @Override
    public String toString() {
        return super.toString() + ":" + filename;
    }
}
