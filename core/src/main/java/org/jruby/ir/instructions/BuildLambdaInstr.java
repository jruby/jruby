package org.jruby.ir.instructions;

import org.jruby.RubyProc;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Map;
import org.jruby.ir.operands.Fixnum;
import org.jruby.ir.operands.StringLiteral;

public class BuildLambdaInstr extends Instr implements ResultInstr, FixedArityInstr, ClosureAcceptingInstr {
    /** The position for the block */
    private final ISourcePosition position;
    private Variable result;
    private WrappedIRClosure lambdaBody;

    public BuildLambdaInstr(Variable lambda, WrappedIRClosure lambdaBody, ISourcePosition position) {
        super(Operation.LAMBDA);

        this.result = lambda;
        this.lambdaBody = lambdaBody;
        this.position = position;
    }

    public String getLambdaBodyName() {
        return getLambdaBody().getClosure().getName();
    }
    @Override
    public Operand[] getOperands() {
        return new Operand[] { lambdaBody, new StringLiteral(position.getFile()), new Fixnum(position.getLine()) };
    }

    public ISourcePosition getPosition() {
        return position;
    }

    @Override
    public Variable getResult() {
        return result;
    }

    @Override
    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        // SSS FIXME: This is buggy. The lambda body might have to be cloned depending on cloning context.
        return new BuildLambdaInstr(ii.getRenamedVariable(getResult()), getLambdaBody(), position);
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        lambdaBody = (WrappedIRClosure) lambdaBody.getSimplifiedOperand(valueMap, force);
    }

    private WrappedIRClosure getLambdaBody() {
        return lambdaBody;
    }

    public Operand getClosureArg() {
        return lambdaBody;
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block aBlock) {
        // SSS FIXME: Copied this from ast/LambdaNode ... Is this required here as well?
        //
        // JRUBY-5686: do this before executing so first time sets cref module
        getLambdaBody().getClosure().getStaticScope().determineModule();

        IRClosure body = getLambdaBody().getClosure();
        // ENEBO: Now can live nil be passed as block reference?
        // SSS FIXME: Should we do the same %self retrieval as in the case of WrappedIRClosure? Or are lambdas special??
        return RubyProc.newProc(context.runtime,
                (Block) (body == null ? context.runtime.getIRManager().getNil() : getLambdaBody()).retrieve(context, self, currDynScope, temp),
                Block.Type.LAMBDA, position);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BuildLambdaInstr(this);
    }

    @Override
    public String toString() {
        return "" + ((ResultInstr)this).getResult() + " = lambda(" + lambdaBody + ")";
    }
}
