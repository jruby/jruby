package org.jruby.compiler.ir.instructions.ruby19;

import org.jruby.RubyProc;
import org.jruby.lexer.yacc.ISourcePosition;

import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.IRClosure;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.MethAddr;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.operands.WrappedIRClosure;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.CallInstr;

public class BuildLambdaInstr extends CallInstr {
    /** The position for the block */
    private final ISourcePosition position;

    public BuildLambdaInstr(Variable lambda, IRClosure lambdaBody, ISourcePosition position) {
        super(Operation.LAMBDA, CallType.UNKNOWN, lambda, MethAddr.NO_METHOD, null, EMPTY_OPERANDS, new WrappedIRClosure(lambdaBody));
        this.position = position;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { getClosureArg() };
    }

    @Override
    public Instr discardResult() {
        return this;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new BuildLambdaInstr(ii.getRenamedVariable(getResult()), getLambdaBody(), position);
    }

    private IRClosure getLambdaBody() {
        return ((WrappedIRClosure)getClosureArg()).getClosure();
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block aBlock) {
        // SSS FIXME: Copied this from ast/LambdaNode ... Is this required here as well?
        //
        // JRUBY-5686: do this before executing so first time sets cref module
        getLambdaBody().getStaticScope().determineModule();

        return RubyProc.newProc(context.getRuntime(), (Block)getClosureArg().retrieve(context, self, currDynScope, temp), Block.Type.LAMBDA, position);
    }
}
