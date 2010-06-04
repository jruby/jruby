package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.interpreter.Jump;
import org.jruby.runtime.builtin.IRubyObject;

public class JumpInstr extends NoOperandInstr {
    public final Label target;
    private final Jump jumpTarget;

    public JumpInstr(Label target) {
        super(Operation.JUMP);
        
        this.target = target;
        this.jumpTarget = new Jump(target);
    }

    @Override
    public String toString() {
        return super.toString() + " " + target;
    }

    public Label getJumpTarget() {
        return target;
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new JumpInstr(ii.getRenamedLabel(target));
    }

    @Override
    public void interpret(InterpreterContext interp, IRubyObject self) {
        throw jumpTarget;
    }
}
