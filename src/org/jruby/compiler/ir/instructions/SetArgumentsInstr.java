package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Interp;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.RubyArray;
import org.jruby.runtime.ThreadContext;

// This instruction sets a new argument array -- this is used to intepret the block-arg assignment
// tree of the form  |a,(b,(c,(d,..))),..| by pushing a argument array as we go up/down one level
// of this assignment tree.
public class SetArgumentsInstr extends OneOperandInstr {
    public final boolean coerceToArray;

    public SetArgumentsInstr(Variable dest, Variable newArgs, boolean coerceToArray) {
        super(Operation.SET_ARGS, dest, newArgs);
        this.coerceToArray = coerceToArray;
    }

    @Override
    public String toString() {
        return super.toString() + (coerceToArray ? "(to_ary)" : "");
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        throw new RuntimeException("Not implemented yet!");
    }

    @Interp
    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        Object o = getArg().retrieve(interp);
        if (coerceToArray) {
            // run to_ary and convert to java array
            if (!(o instanceof RubyArray)) o = RuntimeHelpers.aryToAry((IRubyObject)o);
            o = ((RubyArray)o).toJavaArray();
        }

        // Set new arguments
        IRubyObject[] origArgs = interp.setNewParameters((IRubyObject[])o);

        // Store it into the destination variable if we have a non-null variable
        Variable dest = getResult();
        if (dest != null)
            dest.store(interp, origArgs);

        return null;
    }
}
