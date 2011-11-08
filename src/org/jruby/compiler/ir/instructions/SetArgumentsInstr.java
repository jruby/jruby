package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.compiler.ir.Interp;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.RubyArray;
import org.jruby.runtime.ThreadContext;

// This instruction sets a new newArgs array -- this is used to intepret the block-arg assignment
// tree of the form  |a,(b,(c,(d,..))),..| by pushing a newArgs array as we go up/down one level
// of this assignment tree.
public class SetArgumentsInstr extends Instr implements ResultInstr {
    private final boolean coerceToArray;
    private Operand newArgs;
    private final Variable destination;

    public SetArgumentsInstr(Variable destination, Variable newArgs, boolean coerceToArray) {
        super(Operation.SET_ARGS);
        
        this.coerceToArray = coerceToArray;
        this.newArgs = newArgs;
        this.destination = destination;
    }

    public Operand[] getOperands() {
        return new Operand[]{newArgs};
    }
    
    public Variable getResult() {
        return destination;
    }


    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap) {
        newArgs = newArgs.getSimplifiedOperand(valueMap);
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
        Object o = newArgs.retrieve(interp, context, self);
        if (coerceToArray) {
            // run to_ary and convert to java array
            if (!(o instanceof RubyArray)) o = RuntimeHelpers.aryToAry((IRubyObject)o);
            o = ((RubyArray)o).toJavaArray();
        } else if (destination != null) {
            if (!(o instanceof RubyArray)) o = ArgsUtil.convertToRubyArray(context.getRuntime(), (IRubyObject)o, false);
            o = ((RubyArray)o).toJavaArray();
		  }

        // Set new newArgss
        IRubyObject[] origArgs = interp.setNewParameters((IRubyObject[])o);

        // Store it into the destination variable if we have a non-null variable
        if (destination != null) destination.store(interp, context, self, origArgs);

        return null;
    }
}
