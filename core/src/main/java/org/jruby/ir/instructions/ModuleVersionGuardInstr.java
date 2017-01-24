package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.runtime.IRDeoptimization;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * This instruction will be generated whenever speculative optimizations are performed
 * based on assuming that an object's metaclass is C (as determined by the version number
 * of C -- where the version number changes every time C's class structure changes).
 */
public class ModuleVersionGuardInstr extends OneOperandInstr implements FixedArityInstr {
    private final int expectedVersion;  // The token value that has been assumed
    private final int ipc; // Where we should fall back to in full build on deopt.

    public ModuleVersionGuardInstr(int expectedVersion, Operand candidateObj, int ipc) {
        super(Operation.MODULE_GUARD, candidateObj);

        this.expectedVersion = expectedVersion;
        this.ipc = ipc;
    }

    /** The object whose metaclass token has to be verified*/
    public Operand getCandidateObject() {
        return getOperand1();
    }

    public int getExpectedVersion() {
        return expectedVersion;
    }

    public int getIPC() {
        return ipc;
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] { "ipc: " + ipc, "expected_version: " + expectedVersion};
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new ModuleVersionGuardInstr(expectedVersion, getCandidateObject().cloneForInlining(ii), ipc);
    }

    private boolean versionMatches(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        IRubyObject receiver = (IRubyObject) getCandidateObject().retrieve(context, self, currScope, currDynScope, temp);
        // if (module.getGeneration() != expectedVersion) ... replace this instr with a direct jump
        //
        // SSS FIXME: This is not always correct.  Implementation class is not always receiver.getMetaClass()
        // as we know from how we add instance-methods.  We add it to rubyClass value on the stack.  So, how
        // do we handle this sticky situation?
        return (receiver.getMetaClass().getGeneration() == getExpectedVersion());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ModuleVersionGuardInstr(this);
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        if (!versionMatches(context, currScope, currDynScope, self, temp)) {
            throw new IRDeoptimization(ipc);
        }

        return context.nil; /* not used */
    }
}
