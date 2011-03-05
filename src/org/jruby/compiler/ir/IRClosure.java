package org.jruby.compiler.ir;

// Closures are contexts/scopes for the purpose of IR building.  They are self-contained and accumulate instructions
// that don't merge into the flow of the containing scope.  They are manipulated as an unit.
// Their parents are always execution scopes.
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.parser.BlockStaticScope;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.InterpretedIRBlockBody;

public class IRClosure extends IRExecutionScope {

    public final Label startLabel; // Label for the start of the closure (used to implement redo)
    public final Label endLabel;   // Label for the end of the closure (used to implement retry)
    public final int closureId;  // Unique id for this closure within the nearest ancestor method.

    private final BlockBody body;

    public IRClosure(IRScope lexicalParent, StaticScope staticScope, Arity arity, int argumentType) {
        super(lexicalParent, MetaObject.create(lexicalParent), null, staticScope);
        startLabel = getNewLabel("_CLOSURE_START");
        endLabel = getNewLabel("_CLOSURE_END");
        closureId = lexicalParent.getNextClosureId();
        setName("_CLOSURE_" + closureId);

        this.body = new InterpretedIRBlockBody(this, arity, argumentType);
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
}
