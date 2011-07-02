package org.jruby.compiler.ir.instructions;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import org.jruby.RubyArray;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Splat;
import org.jruby.compiler.ir.operands.CompoundArray;
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
/**
             constArgs = true;
             for (int i = 0; i < args.length; i++) {
                 if (args[i].isConstant()) {
                     preparedArgs[i] = (IRubyObject) args[i].retrieve(interp);
                 } else {
                     constArgs = false;
                     break;
                 }
             }
**/
         }

         // SSS FIXME: This encoding of arguments as an array penalizes splats, but keeps other argument arrays fast
         // since there is no array list --> array transformation
         if (!constArgs) {
             for (int i = 0; i < args.length; i++) {
                 if (!(args[i] instanceof Splat) && !(args[i] instanceof CompoundArray)) {
                     preparedArgs[i] = (IRubyObject) args[i].retrieve(interp);
                 }
                 else {
                     // We got a splat or a compound array -- discard the args array, and rebuild as a list
                     // If we had an 'Array.flatten' in Java, this would be trivial code!
                     List<IRubyObject> argList = new ArrayList<IRubyObject>();
                     for (int j = 0; j < i; j++) {
                         argList.add(preparedArgs[j]);
                     }
                     for (int j = i; j < args.length; j++) {
                         IRubyObject rArg = (IRubyObject)args[j].retrieve(interp);
                         if ((args[j] instanceof Splat) || (args[j] instanceof CompoundArray)) { // append the contents of the splatted array
                             for (IRubyObject v: ((RubyArray)rArg).toJavaArray())
                                 argList.add(v);
                         }
                         else {
                             argList.add(rArg);
                         }
                     }
                     preparedArgs = argList.toArray(new IRubyObject[argList.size()]);
                     break;
                 }
             }
         }

         return preparedArgs;
    }
}
