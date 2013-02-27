package org.jruby.ir.instructions;

import org.jruby.RubyModule;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Map;

/**
 * This instruction will be generated whenever speculative optimizations are performed
 * based on assuming that an object's metaclass is C (as determined by the version number
 * of C -- where the version number changes every time C's class structure changes).
 */
public class ModuleVersionGuardInstr extends Instr {
    /** The token value that has been assumed */
    private final int expectedVersion;

    /** The module whose version we are testing */
    private final RubyModule module;

    /** The object whose metaclass token has to be verified*/
    private Operand candidateObj;

    /** Where to jump if the version assumption fails? */
    private Label failurePathLabel;

    public ModuleVersionGuardInstr(RubyModule module, int expectedVersion, Operand candidateObj, Label failurePathLabel) {
        super(Operation.MODULE_GUARD);
        this.module = module;
        this.expectedVersion = expectedVersion;
        this.candidateObj = candidateObj;
        this.failurePathLabel = failurePathLabel;
    }

    public Label getFailurePathLabel() {
        return failurePathLabel;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { candidateObj };
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        candidateObj = candidateObj.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + candidateObj + ", " + expectedVersion + "[" + module.getName() + "], " + failurePathLabel + ")";
    }

    @Override
    public Instr cloneForInlinedScope(InlinerInfo ii) {
        return new ModuleVersionGuardInstr(module, expectedVersion, candidateObj.cloneForInlining(ii), ii.getRenamedLabel(failurePathLabel));
    }

    @Override
    public Instr cloneForBlockCloning(InlinerInfo ii) {
        return new ModuleVersionGuardInstr(module, expectedVersion, candidateObj.cloneForInlining(ii), failurePathLabel);
    }

    private boolean versionMatches(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        IRubyObject receiver = (IRubyObject) candidateObj.retrieve(context, self, currDynScope, temp);
        // if (module.getGeneration() != expectedVersion) ... replace this instr with a direct jump
        //
        // SSS FIXME: This is not always correct.  Implementation class is not always receiver.getMetaClass()
        // as we know from how we add instance-methods.  We add it to rubyClass value on the stack.  So, how
        // do we handle this sticky situation?
        return (receiver.getMetaClass().getGeneration() == expectedVersion);
    }

    @Override
    public int interpretAndGetNewIPC(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, int ipc) {
        return versionMatches(context, currDynScope, self, temp) ? ipc : getFailurePathLabel().getTargetPC();
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ModuleVersionGuardInstr(this);
    }
}
