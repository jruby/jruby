package org.jruby.ir.instructions.ruby18;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.OptArgMultipleAsgnInstr;
import org.jruby.ir.instructions.ReceiveOptArgBase;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.builtin.IRubyObject;

// Assign the 'index' argument to 'dest'.
public class ReceiveOptArgInstr18 extends ReceiveOptArgBase {
    public ReceiveOptArgInstr18(Variable result, int index) {
        super(result, index);
    }

    @Override
    public Instr cloneForInlinedScope(InlinerInfo ii) {
        if (ii.canMapArgsStatically()) {
            return new CopyInstr(ii.getRenamedVariable(result), ii.getArg(argIndex));
        } else {
            return new OptArgMultipleAsgnInstr(ii.getRenamedVariable(result), ii.getArgs(), argIndex, argIndex);
        }
    }

    @Override
    public Instr cloneForBlockCloning(InlinerInfo ii) {
        return new ReceiveOptArgInstr18(ii.getRenamedVariable(result), argIndex);
    }

    public Object receiveOptArg(IRubyObject[] args) {
        return (argIndex < args.length ? args[argIndex] : UndefinedValue.UNDEFINED);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ReceiveOptArgInstr18(this);
    }
}
