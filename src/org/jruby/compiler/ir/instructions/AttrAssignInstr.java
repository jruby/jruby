package org.jruby.compiler.ir.instructions;

import java.util.List;
import org.jruby.RubyString;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

public class AttrAssignInstr extends MultiOperandInstr {
    private static Operand[] buildArgsArray(Operand obj, Operand attr, Operand v, List<Operand> args) {
        Operand[] argsArray = new Operand[args.size() + ((v == null) ? 2 : 3)];
        int i = 2;
        argsArray[0] = obj;
        argsArray[1] = attr;
        if (v != null) {
            argsArray[2] = v;
            i++;
        }
        for (Operand o: args) {
            argsArray[i++] = o;
        }
        return argsArray;
    }

    public AttrAssignInstr(Operand obj, Operand attr, List<Operand> args) {
        super(Operation.ATTR_ASSIGN, null, buildArgsArray(obj, attr, null, args));
    }

    public AttrAssignInstr(Operand obj, Operand attr, List<Operand> args, Operand value) {
        super(Operation.ATTR_ASSIGN, null, buildArgsArray(obj, attr, value, args));
    }

    private AttrAssignInstr(Operand[] args) {
        super(Operation.ATTR_ASSIGN, null, args);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        int i = 0;
        Operand[] clonedArgs = new Operand[_args.length];
        for (Operand a : _args)
            clonedArgs[i++] = a.cloneForInlining(ii);

        return new AttrAssignInstr(clonedArgs);
    }

    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        Operand[] operands = getOperands();
        IRubyObject receiver = (IRubyObject) operands[0].retrieve(interp);
        String      attr     = ((RubyString) operands[1].retrieve(interp)).asJavaString();
        IRubyObject[] args = new IRubyObject[operands.length-2];
        for (int i = 2; i < operands.length; i++)
            args[i-2] = (IRubyObject) operands[i].retrieve(interp);

        receiver.callMethod(interp.getContext(), attr, args);
        return null;
    }
}
