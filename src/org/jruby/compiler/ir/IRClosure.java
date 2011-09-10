package org.jruby.compiler.ir;

import java.util.List;
import java.util.ArrayList;

// Closures are contexts/scopes for the purpose of IR building.  They are self-contained and accumulate instructions
// that don't merge into the flow of the containing scope.  They are manipulated as an unit.
// Their parents are always execution scopes.
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Splat;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.ReceiveClosureArgInstr;
import org.jruby.parser.BlockStaticScope;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.InterpretedIRBlockBody;

public class IRClosure extends IRExecutionScope {
    public final Label startLabel; // Label for the start of the closure (used to implement redo)
    public final Label endLabel;   // Label for the end of the closure (used to implement retry)
    public final int closureId;    // Unique id for this closure within the nearest ancestor method.

    private final BlockBody body;

    // Oy, I have a headache!
    // for-loop body closures are special in that they dont really define a new variable scope.
    // They just silently reuse the parent scope.  This changes how variables are allocated (see IRMethod.java).
    public final boolean isForLoopBody;

    // Has this closure been inlined into a method? If yes, its independent existence has come to an end
    // because it has very likely been integrated into another scope and we should no longer do anything
    // with the instructions as an independent closure scope.
    private boolean hasBeenInlined;     

    // Block parameters
    private List<Operand> blockArgs;

    public IRClosure(IRScope lexicalParent, boolean isForLoopBody, StaticScope staticScope, Arity arity, int argumentType) {
        super(lexicalParent, MetaObject.create(lexicalParent), null, staticScope);
        this.isForLoopBody = isForLoopBody;
        String prefix = isForLoopBody ? "_FOR_LOOP_" : "_CLOSURE_";
        startLabel = getNewLabel(prefix + "START");
        endLabel = getNewLabel(prefix + "END");
        closureId = lexicalParent.getNextClosureId();
        setName(prefix + closureId);
        blockArgs = new ArrayList<Operand>();

        this.body = new InterpretedIRBlockBody(this, arity, argumentType);
        this.hasBeenInlined = false;
    }

    @Override
    public int getNextClosureId() {
        return lexicalParent.getNextClosureId();
    }

    @Override
    public int getTemporaryVariableSize() {
        return getPrefixCountSize("%cl_" + closureId);
    }

    @Override
    public Variable getNewTemporaryVariable() {
        return getNewTemporaryClosureVariable(closureId);
    }

    @Override
    public Label getNewLabel() {
        return getNewLabel("CL" + closureId + "_LBL");
    }

    public String getScopeName() {
        return "Closure";
    }

    @Override
    public void addInstr(Instr i) {
        // Accumulate block arguments
        if (i instanceof ReceiveClosureArgInstr) blockArgs.add(((ReceiveClosureArgInstr) i).isRestOfArgArray() ? new Splat(i.result) : i.result);

        super.addInstr(i);
    }

    public Operand[] getBlockArgs() { 
        return blockArgs.toArray(new Operand[blockArgs.size()]);
    }

/**
    @Override
    public void setConstantValue(String constRef, Operand val) {
        throw new org.jruby.compiler.NotCompilableException("Unexpected: Encountered set constant value in a closure!");
    }
**/

    public String toStringBody() {
        StringBuilder buf = new StringBuilder();
        buf.append(getName()).append(" = { \n");
        org.jruby.compiler.ir.representations.CFG c = getCFG();
        if (c != null) {
            buf.append("\nCFG:\n").append(c.getGraph().toString());
            buf.append("\nInstructions:\n").append(c.toStringInstrs());
        } else {
            buf.append(toStringInstrs());
        }
        buf.append("\n}\n\n");
        return buf.toString();
    }

    @Override
    protected StaticScope constructStaticScope(StaticScope parent) {
        return new BlockStaticScope(parent);
    }

    public BlockBody getBlockBody() {
        return body;
    }

    public void markInlined() {
        this.hasBeenInlined = true;
    }

    public boolean hasBeenInlined() {
        return this.hasBeenInlined;
    }
}
