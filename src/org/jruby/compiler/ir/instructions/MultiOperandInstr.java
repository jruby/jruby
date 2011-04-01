package org.jruby.compiler.ir.instructions;

import java.util.Arrays;
import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

// This is of the form:
//   v = OP(args, attribute_array); Ex: v = CALL(args, v2)

public abstract class MultiOperandInstr extends Instr {
    public MultiOperandInstr(Operation opType, Variable result) {
        super(opType, result);
    }

    @Override
    public String toString() {
        return super.toString() + Arrays.toString(getOperands());
    }

    public Operand[] cloneOperandsForInlining(InlinerInfo ii) {
		  Operand[] oldArgs = getOperands();
        Operand[] newArgs = new Operand[oldArgs.length];
        for (int i = 0; i < oldArgs.length; i++) {
            newArgs[i] = oldArgs[i].cloneForInlining(ii);
        }

        return newArgs;
    }

	 // Cache!
	 private boolean constArgs = false; 
    private IRubyObject[] preparedArgs = null;

	 protected IRubyObject[] prepareArguments(Operand[] args, InterpreterContext interp) {
         if (preparedArgs == null) {
             preparedArgs = new IRubyObject[args.length];
             constArgs = true;
             for (int i = 0; i < args.length; i++) {
                 if (args[i].isConstant()) {
                     preparedArgs[i] = (IRubyObject) args[i].retrieve(interp);
                 } else {
                     constArgs = false;
                     break;
                 }
             }
         }

         if (!constArgs) {
             for (int i = 0; i < args.length; i++) {
                 preparedArgs[i] = (IRubyObject) args[i].retrieve(interp);
             }
         }

         return preparedArgs;
	 }
}
