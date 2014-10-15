package org.jruby.ir.instructions;

import org.jruby.RubyProc;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.*;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.ir.IRFlags;

import java.util.Map;

public class BuildLambdaInstr extends Instr implements ResultInstr, FixedArityInstr, ClosureAcceptingInstr {
    /** The position for the block */
    private final ISourcePosition position;
    private Variable result;
    private Operand lambdaBody;

    public BuildLambdaInstr(Variable lambda, Operand lambdaBody, ISourcePosition position) {
        super(Operation.LAMBDA);

        this.result = lambda;
        this.lambdaBody = lambdaBody;
        this.position = position;
    }

    public String getLambdaBodyName() {
        // SSS FIXME: this requires a fix 
        return ""; // getLambdaBody().getClosure().getName();
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
    public boolean computeScopeFlags(IRScope scope) {
        scope.getFlags().add(IRFlags.BINDING_HAS_ESCAPED);
        return true;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new BuildLambdaInstr(ii.getRenamedVariable(getResult()), getLambdaBody().cloneForInlining(ii), position);
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        lambdaBody = lambdaBody.getSimplifiedOperand(valueMap, force);
    }

    public Operand getLambdaBody() {
        return lambdaBody;
    }

    public Operand getClosureArg() {
        return lambdaBody;
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        // SSS FIXME: Copied this from ast/LambdaNode ... Is this required here as well?
        //
        // JRUBY-5686: do this before executing so first time sets cref module
        ((ClosureInterpreterContext)getLambdaBody()).getStaticScope().determineModule();

        // CON: This must not be happening, because nil would never cast to Block
//        IRClosure body = getLambdaBody().getClosure();
//        Block block = (Block) (body == null ? context.runtime.getIRManager().getNil() : getLambdaBody()).retrieve(context, self, currScope, currDynScope, temp);
        Block block = (Block)getLambdaBody().retrieve(context, self, currScope, currDynScope, temp);
        // ENEBO: Now can live nil be passed as block reference?
        // SSS FIXME: Should we do the same %self retrieval as in the case of WrappedIRClosure? Or are lambdas special??
        return RubyProc.newProc(context.runtime,
                block,
                Block.Type.LAMBDA,
                position.getFile(),
                position.getLine());
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
