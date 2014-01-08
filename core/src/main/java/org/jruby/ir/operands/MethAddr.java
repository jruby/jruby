package org.jruby.ir.operands;

// Placeholder class for method address

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Float;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class MethAddr extends Reference {
    public static final MethAddr NO_METHOD = new MethAddr("");
    public static final MethAddr UNKNOWN_SUPER_TARGET  = new MethAddr("-unknown-super-target-");

    public MethAddr(String name) {
        super(OperandType.METH_ADDR, name);
    }

    @Override
    public String toString() {
        return "'" + getName() + "'";
    }

    @Override
    public boolean canCopyPropagate() {
        return true;
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        return getName();
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof MethAddr) && ((MethAddr)o).getName().equals(getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    public boolean resemblesALUOp() {
        String n = getName();
        return n.equals("+") || n.equals("-") || n.equals("*") || n.equals("/") ||
            n.equals("|") || n.equals("&") || n.equals(">>") || n.equals("<<") ||
            n.equals(">") || n.equals("<") ||
            n.equals("==") || n.equals("!=");
    }

    public Operation getUnboxedOp(Class unboxedType) {
        String n = getName();
        if (unboxedType == Float.class && n.length() == 1) {
            switch (n.charAt(0)) {
            case '+' : return Operation.FADD;
            case '-' : return Operation.FSUB;
            case '*' : return Operation.FMUL;
            case '/' : return Operation.FDIV;
            case '>' : return Operation.FGT;
            case '<' : return Operation.FLT;
            }
        }
        return null;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.MethAddr(this);
    }
}
