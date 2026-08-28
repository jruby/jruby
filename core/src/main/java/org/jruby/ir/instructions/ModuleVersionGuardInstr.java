package org.jruby.ir.instructions;

import org.jruby.RubyModule;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Operand;
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
public class ModuleVersionGuardInstr extends TwoOperandInstr implements FixedArityInstr {
    private final int expectedVersion;  // The token value that has been assumed
    private final RubyModule module;    // The module whose version we are testing */

    public ModuleVersionGuardInstr(RubyModule module, int expectedVersion, Operand candidateObj, Label failurePathLabel) {
        super(Operation.MODULE_GUARD, candidateObj, failurePathLabel);
        this.module = module;
        this.expectedVersion = expectedVersion;
    }

    /** The object whose metaclass token has to be verified*/
    public Operand getCandidateObject() {
        return getOperand1();
    }

    /** Where to jump if the version assumption fails? */
    public Label getFailurePathLabel() {
        return (Label) getOperand2();
    }

    // FIXME: We should remove this and only save what we care about..live Module cannot be necessary here?
    public RubyModule getModule() {
        return module;
    }

    public int getExpectedVersion() {
        return expectedVersion;
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] { "name: " + module.getName(module.getRuntime().getCurrentContext()), "expected_version: " + expectedVersion};
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new ModuleVersionGuardInstr(module, expectedVersion, getCandidateObject().cloneForInlining(ii),
                ii.getRenamedLabel(getFailurePathLabel()));
    }

    private boolean versionMatches(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        IRubyObject receiver = (IRubyObject) getCandidateObject().retrieve(context, self, currScope, currDynScope, temp);
        // if (module.getGeneration() != expectedVersion) ... replace this instr with a direct jump
        //
        // SSS FIXME: This is not always correct.  Implementation class is not always receiver.getMetaClass()
        // as we know from how we add instance-methods.  We add it to rubyClass value on the stack.  So, how
        // do we handle this sticky situation?
        boolean matches = receiver.getMetaClass().getGeneration() == getExpectedVersion();

        if (!matches) {
            //System.out.println("GUARD: " + (receiver.getMetaClass().getGeneration() == getExpectedVersion()) + ", OBJ: " +
            //        receiver + ", VERS: " + receiver.getMetaClass().getGeneration() + ", EVERSE: " + getExpectedVersion());
        }

        return matches;
    }

    @Override
    public int interpretAndGetNewIPC(ThreadContext context, DynamicScope currDynScope, StaticScope currScope, IRubyObject self, Object[] temp, int ipc) {
        return versionMatches(context, currScope, currDynScope, self, temp) ? ipc : getFailurePathLabel().getTargetPC();
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ModuleVersionGuardInstr(this);
    }
}
