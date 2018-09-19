package org.jruby.ir.dataflow.analyses;

import org.jruby.RubySymbol;
import org.jruby.ir.dataflow.DataFlowProblem;
import org.jruby.ir.operands.Fixnum;
import org.jruby.ir.operands.Float;
import org.jruby.ir.operands.TemporaryLocalVariable;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.Operation;
import org.jruby.ir.representations.BasicBlock;

import java.util.HashMap;
import java.util.Map;

// This problem tries to find unboxable (currently, Float and Fixnum) operands
// by inferring types and optimistically assuming that Float and Fixnum numeric
// operations have not been / will not be modified in those classes.
//
// This is a very simplistic type analysis. Doesn't analyze Array/Splat/Range
// operands and array/splat dereference/iteration since that requires assumptions
// to be made about those classes.
//
// In this analysis, we will treat:
// * 'null' as Dataflow.TOP
// * a concrete class as known type
// * 'Object.class' as Dataflow.BOTTOM
//
// Type of a variable will change at most twice: TOP --> class --> BOTTOM

abstract class UnboxableOp {
    public final String name;

    public UnboxableOp(String name) {
        this.name = name;
    }

    abstract public boolean acceptsArgTypes(Class receiverType, Class argType);
    abstract public Class getUnboxedType(Class receiverType, Class argType);
    abstract public Class getUnboxedResultType(Class operandType);
    abstract public Operation getUnboxedOp(Class operandType);

    public static Map<String, UnboxableOp> opsMap = new HashMap<String, UnboxableOp>();

    public static void addUnboxableOp(UnboxableOp op) {
        opsMap.put(op.name, op);
    }

    static {
        addUnboxableOp(new ArithOp("+", Operation.IADD, Operation.FADD));
        addUnboxableOp(new ArithOp("-", Operation.ISUB, Operation.FSUB));
        addUnboxableOp(new ArithOp("*", Operation.IMUL, Operation.FMUL));
        addUnboxableOp(new ArithOp("/", Operation.IDIV, Operation.FDIV));
        addUnboxableOp(new LogicOp("|", Operation.IOR));
        addUnboxableOp(new LogicOp("&", Operation.IAND));
        addUnboxableOp(new LogicOp("^", Operation.IXOR));
        addUnboxableOp(new LogicOp("<<", Operation.ISHL));
        addUnboxableOp(new LogicOp(">>", Operation.ISHR));
        addUnboxableOp(new CompareOp("<", Operation.ILT, Operation.FLT));
        addUnboxableOp(new CompareOp(">", Operation.IGT, Operation.FGT));
        addUnboxableOp(new CompareOp("==", Operation.IEQ, Operation.FEQ));
        addUnboxableOp(new CompareOp("===", Operation.IEQ, Operation.FEQ));
    }
}

final class ArithOp extends UnboxableOp {
    private final Operation fixnumOp;
    private final Operation floatOp;

    public ArithOp(String name, Operation fixnumOp, Operation floatOp) {
        super(name);
        this.fixnumOp = fixnumOp;
        this.floatOp = floatOp;
    }

    public boolean acceptsArgTypes(Class receiverType, Class argType) {
        return receiverType == Float.class ||
               (receiverType == Fixnum.class && (argType == Float.class || argType == Fixnum.class));
    }

    public Class getUnboxedType(Class receiverType, Class argType) {
        // acceptsArgTypes should have ensured that we have the right types here
        return receiverType == Float.class || argType == Float.class ? Float.class : Fixnum.class;
    }

    public final Class getUnboxedResultType(Class operandType) {
        return (operandType == Float.class || operandType == Fixnum.class) ? operandType : null;
    }

    public final Operation getUnboxedOp(Class operandType) {
        return (operandType == Fixnum.class) ? fixnumOp : (operandType == Float.class) ? floatOp : null;
    }
}

final class LogicOp extends UnboxableOp {
    private final Operation op;

    public LogicOp(String name, Operation op) {
        super(name);
        this.op = op;
    }

    public boolean acceptsArgTypes(Class receiverType, Class argType) {
        return receiverType == Fixnum.class && argType == Fixnum.class;
    }

    public Class getUnboxedType(Class receiverType, Class argType) {
        // acceptsArgTypes should have ensured that we have the right types here
        return Fixnum.class;
    }

    public final Class getUnboxedResultType(Class operandType) {
        return operandType == Fixnum.class ? Fixnum.class : null;
    }

    public final Operation getUnboxedOp(Class operandType) {
        return operandType == Fixnum.class ? op : null;
    }
}

final class CompareOp extends UnboxableOp {
    private final Operation fixnumOp;
    private final Operation floatOp;

    public CompareOp(String name, Operation fixnumOp, Operation floatOp) {
        super(name);
        this.fixnumOp = fixnumOp;
        this.floatOp = floatOp;
    }

    public boolean acceptsArgTypes(Class receiverType, Class argType) {
        return receiverType == argType && (receiverType == Fixnum.class || receiverType == Float.class);
    }

    public Class getUnboxedType(Class receiverType, Class argType) {
        // acceptsArgTypes should have ensured that we have the right types here
        return receiverType;
    }

    public final Class getUnboxedResultType(Class operandType) {
        return (operandType == Fixnum.class || operandType == Float.class) ? Boolean.class : null;
    }

    public final Operation getUnboxedOp(Class operandType) {
        return (operandType == Fixnum.class) ? fixnumOp : (operandType == Float.class) ? floatOp : null;
    }
}

public class UnboxableOpsAnalysisProblem extends DataFlowProblem<UnboxableOpsAnalysisProblem, UnboxableOpsAnalysisNode> {
    public final static String NAME = "UnboxableOpsAnalysis";

    public UnboxableOpsAnalysisProblem() {
        super(DataFlowProblem.DF_Direction.FORWARD);
    }

    @Override
    public String getName() {
        return "Unboxable Operands Analysis";
    }

    @Override
    public UnboxableOpsAnalysisNode buildFlowGraphNode(BasicBlock bb) {
        return new UnboxableOpsAnalysisNode(this, bb);
    }

    @Override
    public String getDataFlowVarsForOutput() {
        return "";
    }

    public void unbox() {
        // System.out.println("---------------- SCOPE BEFORE unboxing ----------------");
        // System.out.println("\nInstrs:\n" + getScope().cfg().toStringInstrs());
        Map<Variable, TemporaryLocalVariable> unboxMap = new HashMap<Variable, TemporaryLocalVariable>();
        for (UnboxableOpsAnalysisNode n : generateWorkList()) {
            n.unbox(unboxMap);
        }
    }

    public boolean isUnboxableMethod(String name) {
        return UnboxableOp.opsMap.get(name) != null;
    }

    public boolean acceptsArgTypes(String name, Class receiverType, Class argType) {
        UnboxableOp uop = UnboxableOp.opsMap.get(name);
        return uop == null ? false : uop.acceptsArgTypes(receiverType, argType);
    }

    public Class getUnboxedType(String name, Class receiverType, Class argType) {
        UnboxableOp uop = UnboxableOp.opsMap.get(name);
        return uop == null ? null : uop.getUnboxedType(receiverType, argType);
    }

    public Class getUnboxedResultType(String name, Class operandType) {
        UnboxableOp uop = UnboxableOp.opsMap.get(name);
        return uop == null ? null : uop.getUnboxedResultType(operandType);
    }

    public Operation getUnboxedOp(String name, Class operandType) {
        UnboxableOp uop = UnboxableOp.opsMap.get(name);
        return uop == null ? null : uop.getUnboxedOp(operandType);
    }
}
