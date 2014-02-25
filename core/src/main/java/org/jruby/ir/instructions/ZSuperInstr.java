package org.jruby.ir.instructions;

import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Arrays;

public class ZSuperInstr extends UnresolvedSuperInstr {
    Operand[] allPossibleArgs;
    Integer[] argCounts;

	// SSS FIXME: receiver is never used -- being passed in only to meet requirements of CallInstr
    public ZSuperInstr(Variable result, Operand receiver, Operand closure, Operand[] allPossibleArgs, Integer[] argCounts) {
        super(Operation.ZSUPER, result, receiver, allPossibleArgs, closure);
        this.allPossibleArgs = allPossibleArgs;
        this.argCounts = argCounts;
    }

    @Override
    public boolean computeScopeFlags(IRScope scope) {
        scope.getFlags().add(IRFlags.USES_ZSUPER);
        scope.getFlags().add(IRFlags.CAN_CAPTURE_CALLERS_BINDING);
        return true;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        int numArgs = allPossibleArgs.length;
        Operand[] clonedArgs = new Operand[numArgs];
        for (int i = 0; i < numArgs; i++) {
            clonedArgs[i] = allPossibleArgs[i].cloneForInlining(ii);
        }

        return new ZSuperInstr(ii.getRenamedVariable(result), getReceiver().cloneForInlining(ii), closure == null ? null : closure.cloneForInlining(ii), clonedArgs, argCounts);
    }

    @Override
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
        int i = 0, offset = 0;
        while (!argsDynScope.getStaticScope().isArgumentScope()) {
            argsDynScope = argsDynScope.getNextCapturedScope();
            offset += argCounts[i];
            i++;
        }

        int n = argCounts[i];
        Operand[] superArgs = new Operand[n];
        for (int j = 0; j < n; j++) {
            superArgs[j] = allPossibleArgs[offset+j];
        }

        // Prepare args -- but look up in 'argsDynScope', not 'currDynScope'
        IRubyObject[] args = prepareArguments(context, self, superArgs, argsDynScope, temp);

        // Prepare block -- fetching from the frame stack, if necessary
        Block block = prepareBlock(context, self, currDynScope, temp);
        if (block == null || !block.isGiven()) block = context.getFrameBlock();

        return IRRuntimeHelpers.unresolvedSuper(context, self, args, block);
    }

    public Integer[] getArgCounts() {
        return argCounts;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ZSuperInstr(this);
    }

    @Override
    public String toString() {
        return "" + getOperation()  + "(" + receiver + ", " + Arrays.toString(getCallArgs()) + ", " + Arrays.toString(argCounts) + (closure == null ? "" : ", &" + closure) + ")";
    }
}
