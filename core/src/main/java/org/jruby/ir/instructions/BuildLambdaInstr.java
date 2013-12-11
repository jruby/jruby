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

public class BuildLambdaInstr extends Instr implements ResultInstr {
    /** The position for the block */
    private final ISourcePosition position;
    private Variable result;
    private Operand[] operands;

    public BuildLambdaInstr(Variable lambda, WrappedIRClosure lambdaBody, ISourcePosition position) {
        super(Operation.LAMBDA);

        this.result = lambda;
        this.operands = new Operand[] { lambdaBody };
        this.position = position;
    }


    private IRClosure getLambdaBody() {
        return ((WrappedIRClosure) operands[0]).getClosure();
    }
    
    public String getLambdaBodyName() {
        return getLambdaBody().getName();
    }
    @Override
    public Operand[] getOperands() {
        return operands;
    }
    
    public ISourcePosition getPosition() {
        return position;
    }

    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        // SSS FIXME: This is buggy. The lambda body might have to be cloned depending on cloning context.
        return new BuildLambdaInstr(ii.getRenamedVariable(getResult()), (WrappedIRClosure)operands[0], position);
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        for (int i = 0; i < operands.length; i++) {
            operands[i] = operands[i].getSimplifiedOperand(valueMap, force);
        }
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block aBlock) {
        // SSS FIXME: Copied this from ast/LambdaNode ... Is this required here as well?
        //
        // JRUBY-5686: do this before executing so first time sets cref module
        getLambdaBody().getStaticScope().determineModule();

        IRClosure body = getLambdaBody();
        // ENEBO: Now can live nil be passed as block reference?
        // SSS FIXME: Should we do the same %self retrieval as in the case of WrappedIRClosure? Or are lambdas special??
        return RubyProc.newProc(context.runtime,
                (Block) (body == null ? context.runtime.getIRManager().getNil() : operands[0]).retrieve(context, self, currDynScope, temp),
                Block.Type.LAMBDA, position);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BuildLambdaInstr(this);
    }
    
    @Override
    public String toString() {
        return super.toString() + "(" + getLambdaBody().getName() + ", " + position.getFile() + ", " + position.getLine() + ")";
    } 
}
