package org.jruby.compiler.ir.instructions;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import org.jruby.RubyArray;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Splat;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.MethAddr;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

// Instruction representing Ruby code of the form: "a[i] = 5"
// which is equivalent to: a.[](i,5)
public class AttrAssignInstr extends MultiOperandInstr {
    private Operand   obj;   // Object being updated
    private MethAddr  attr;  // Attribute being called
    private Operand[] args;  // Arguments to the attribute call
    private Operand   value; // Value being assigned

    public AttrAssignInstr(Operand obj, MethAddr attr, Operand[] args) {
        super(Operation.ATTR_ASSIGN, null);
        this.obj   = obj;
        this.attr  = attr;
        this.args  = args;
        this.value = null;
    }

    public AttrAssignInstr(Operand obj, MethAddr attr, Operand[] args, Operand value) {
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

        return new AttrAssignInstr(obj.cloneForInlining(ii), attr, clonedArgs, value == null ? null : value.cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        IRubyObject receiver = (IRubyObject) obj.retrieve(interp, context, self);
        String      attrMeth = attr.getName();
        List<IRubyObject> argList = new ArrayList<IRubyObject>(); 

        // Process attr method args -- splats are expanded
        for (int i = 0; i < args.length; i++) {
            IRubyObject rArg = (IRubyObject)args[i].retrieve(interp, context, self);
            if (args[i] instanceof Splat) { 
                argList.addAll(Arrays.asList(((RubyArray)rArg).toJavaArray()));
            } else {
                argList.add(rArg);
            }
        }

        // Process value -- splats are NOT expanded
        if (value != null) argList.add((IRubyObject)value.retrieve(interp, context, self));

        // no visibility checks if receiver is self
        RuntimeHelpers.invoke(context, receiver, attrMeth, argList.toArray(new IRubyObject[argList.size()]), (self == receiver) ? CallType.FUNCTIONAL : CallType.NORMAL, Block.NULL_BLOCK);
        return null;
    }
}
