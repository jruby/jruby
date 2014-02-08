package org.jruby.ir.operands;

// Placeholder class for method address

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
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

    public Class getUnboxedResultType(Class operandType) {
        String n = getName();
        if (n.length() == 1) {
            switch (n.charAt(0)) {
            case '+' :
            case '-' :
            case '*' :
            case '/' : return operandType == Float.class ? Float.class : operandType == Fixnum.class ? Fixnum.class : null;
            case '>' :
            case '<' : return operandType == Float.class || operandType == Fixnum.class ? UnboxedBoolean.class : null;
            }
        }
        return null;
    }

    public Operation getUnboxedOp(Class unboxedType) {
        String n = getName();
        if (unboxedType == Float.class) {
            if (n.length() == 1) {
                switch (n.charAt(0)) {
                    case '+' : return Operation.FADD;
                    case '-' : return Operation.FSUB;
                    case '*' : return Operation.FMUL;
                    case '/' : return Operation.FDIV;
                    case '>' : return Operation.FGT;
                    case '<' : return Operation.FLT;
                }
            } else if (n.length() == 2) {
                if (n.equals("==")) return Operation.FEQ;
            } else if (n.equals("===")) {
                return Operation.FEQ;
            }
        } else if (unboxedType == Fixnum.class) {
            if (n.length() == 1) {
                switch (n.charAt(0)) {
                    case '+' : return Operation.IADD;
                    case '-' : return Operation.ISUB;
                    case '*' : return Operation.IMUL;
                    case '/' : return Operation.IDIV;
                    case '>' : return Operation.IGT;
                    case '<' : return Operation.ILT;
                    case '|' : return Operation.IOR;
                    case '&' : return Operation.IAND;
                    case '^' : return Operation.IXOR;
                }
            } else if (n.length() == 2) {
                if (n.equals(">>")) return Operation.ISHR;
                if (n.equals("<<")) return Operation.ISHL;
                if (n.equals("==")) return Operation.IEQ;
            } else if (n.equals("===")) {
                return Operation.IEQ;
            }
        }
        return null;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.MethAddr(this);
    }
}
