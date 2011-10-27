package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class FilenameInstr extends NoOperandInstr {
    private final String filename;

    public FilenameInstr(String filename) {
        super(Operation.FILE_NAME);
        
        this.filename = filename;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + filename + ")";
    }

    public Instr cloneForInlining(InlinerInfo ii) { 
        return this;
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        context.setFile(filename);
        return null;
    }
}
