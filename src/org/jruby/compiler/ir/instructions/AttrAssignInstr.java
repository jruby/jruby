package org.jruby.compiler.ir.instructions;

import java.util.List;
import java.util.Map;
import org.jruby.RubyString;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

public class AttrAssignInstr extends MultiOperandInstr {
    private Operand   obj;   // SSS: Document this.  What is this?
    private Operand   attr;   // SSS: Document this.  What is this?
    private Operand   value; // SSS: Document this.  What is this?
    private Operand[] args;

    public AttrAssignInstr(Operand obj, Operand attr, Operand[] args) {
        super(Operation.ATTR_ASSIGN, null);
        this.obj   = obj;
        this.attr  = attr;
        this.args  = args;
        this.value = null;
    }

    public AttrAssignInstr(Operand obj, Operand attr, Operand[] args, Operand value) {
        super(Operation.ATTR_ASSIGN, null);
        this.obj   = obj;
        this.attr  = attr;
        this.args  = args;
        this.value = value;
    }

    public Operand[] getOperands() {
        Operand[] argsArray = new Operand[args.length + ((this.value == null) ? 2 : 3)];

        int i = 2;
        argsArray[0] = obj;
        argsArray[1] = attr;
        if (value != null) {
            argsArray[2] = value;
            i++;
        }

        // SSS FIXME: Use Arraycopy?
        for (Operand o: args) {
            argsArray[i++] = o;
        }

        return argsArray;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap) {
        obj = obj.getSimplifiedOperand(valueMap);
        attr = attr.getSimplifiedOperand(valueMap);
        for (int i = 0; i < args.length; i++) {
            args[i] = args[i].getSimplifiedOperand(valueMap);
        }
        if (value != null) value = value.getSimplifiedOperand(valueMap);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        int i = 0;
        Operand[] clonedArgs = new Operand[args.length];
        for (Operand a : args)
            clonedArgs[i++] = a.cloneForInlining(ii);

        return new AttrAssignInstr(obj.cloneForInlining(ii), attr.cloneForInlining(ii), clonedArgs, value == null ? null : value.cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        IRubyObject receiver = (IRubyObject) obj.retrieve(interp);
        String      attrMeth = ((RubyString) attr.retrieve(interp)).asJavaString();
        IRubyObject[] callArgs = new IRubyObject[args.length + ((value == null) ? 0 : 1)];

        int i = 0;

        // SSS FIXME: Is this correct?  Is value the 1st arg (or the last arg??)
        if (value != null) {
            callArgs[0] = (IRubyObject)value.retrieve(interp);
            i++;
        }

        for ( ; i < callArgs.length; i++)
            callArgs[i] = (IRubyObject) args[i].retrieve(interp);

        receiver.callMethod(interp.getContext(), attrMeth, callArgs);
        return null;
    }
}
