package org.jruby.ir.instructions;

import org.jruby.ir.IRClosure;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.parser.IRStaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ZSuperInstr extends UnresolvedSuperInstr {
	 // SSS FIXME: receiver is never used -- being passed in only to meet requirements of CallInstr
    public ZSuperInstr(Variable result, Operand receiver, Operand closure) {
        super(Operation.ZSUPER, result, receiver, closure);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new ZSuperInstr(ii.getRenamedVariable(result), getReceiver().cloneForInlining(ii), closure == null ? null : closure.cloneForInlining(ii));
    }

    @Override
    public Operand[] getOperands() {
        return (closure == null) ? EMPTY_OPERANDS : new Operand[] { closure };
    }

    protected IRubyObject[] prepareArguments(ThreadContext context, IRubyObject self, Operand[] arguments, DynamicScope dynamicScope, Object[] temp) {
        // Unlike calls, zsuper args are known only at interpret time, not at constructor time.
        // So, we cannot use the cached containsSplat field from CallBase
        return containsSplat(arguments) ?
                prepareArgumentsComplex(context, self, arguments, dynamicScope, temp) :
                prepareArgumentsSimple(context, self, arguments, dynamicScope, temp);
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block aBlock) {
        DynamicScope argsDynScope = currDynScope;

        // Find args that need to be passed into super
        while (!argsDynScope.getStaticScope().isArgumentScope()) argsDynScope = argsDynScope.getNextCapturedScope();
        IRScope argsIRScope = ((IRStaticScope)argsDynScope.getStaticScope()).getIRScope();
        Operand[] superArgs = (argsIRScope instanceof IRMethod) ? ((IRMethod)argsIRScope).getCallArgs() : ((IRClosure)argsIRScope).getBlockArgs();

        // Prepare args -- but look up in 'argsDynScope', not 'currDynScope'
        IRubyObject[] args = prepareArguments(context, self, superArgs, argsDynScope, temp);

        // Prepare block -- fetching from the frame stack, if necessary
        Block block = prepareBlock(context, self, currDynScope, temp);
        if (block == null || !block.isGiven()) block = context.getFrameBlock();

        return interpretSuper(context, self, args, block);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ZSuperInstr(this);
    }
}
