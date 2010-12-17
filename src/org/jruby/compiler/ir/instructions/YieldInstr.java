package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Interp;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

public class YieldInstr extends MultiOperandInstr {
    // SSS FIXME: Correct?  Where does closure arg come from?
    public YieldInstr(Variable result, Operand[] args) {
        super(Operation.YIELD, result, args);
    }
   
    public boolean isRubyInternalsCall() {
        return false;
    }

    public boolean isStaticCallTarget() {
        return false;
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return this;  // This is just a placeholder during inlining.
    }

    @Interp
    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        Object resultValue = interp.getBlock().call(interp.getContext(), prepareArguments(interp));

        getResult().store(interp, resultValue);
        return null;
    }

    public IRubyObject[] prepareArguments(InterpreterContext interp) {
        Operand[] operands = getOperands();
        IRubyObject[] args = new IRubyObject[operands.length];
        int length = args.length;

        for (int i = 0; i < length; i++) {
            args[i] = (IRubyObject) operands[i].retrieve(interp);
        }

        //System.out.println("ARGS>LENGTH " + args.length);
        //System.out.println("ARGS: " + java.util.Arrays.toString(args));
        return args;
    }
}
