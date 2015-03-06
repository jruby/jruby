package org.jruby.ir.passes;

import org.jruby.ir.*;
import org.jruby.ir.dataflow.analyses.StoreLocalVarPlacementProblem;
import org.jruby.ir.instructions.*;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.ir.representations.CFG;

import java.util.ListIterator;

public class AddCallProtocolInstructions extends CompilerPass {
    @Override
    public String getLabel() {
        return "Add Call Protocol Instructions (push/pop of dyn-scope, frame, impl-class values)";
    }

    private boolean explicitCallProtocolSupported(IRScope scope) {
        return scope instanceof IRMethod || (scope instanceof IRModuleBody && !(scope instanceof IRMetaClassBody));
    }

    @Override
    public Object execute(IRScope scope, Object... data) {
        // IRScriptBody do not get explicit call protocol instructions right now.
        // They dont push/pop a frame and do other special things like run begin/end blocks.
        // So, for now, they go through the runtime stub in IRScriptBody.
        //
        // Add explicit frame and binding push/pop instrs ONLY for methods -- we cannot handle this in closures and evals yet
        // If the scope uses $_ or $~ family of vars, has local load/stores, or if its binding has escaped, we have
        // to allocate a dynamic scope for it and add binding push/pop instructions.
        if (explicitCallProtocolSupported(scope)) {
            StoreLocalVarPlacementProblem slvpp = scope.getStoreLocalVarPlacementProblem();
            boolean scopeHasLocalVarStores = false;
            boolean bindingHasEscaped      = scope.bindingHasEscaped();

            CFG cfg = scope.getCFG();

            if (slvpp != null && bindingHasEscaped) {
                scopeHasLocalVarStores = slvpp.scopeHasLocalVarStores();
            } else {
                // We dont require local-var load/stores to have been run.
                // If it is not run, we go conservative and add push/pop binding instrs. everywhere
                scopeHasLocalVarStores = bindingHasEscaped;
            }

            boolean requireFrame = doesItRequireFrame(scope, bindingHasEscaped);
            boolean requireBinding = !scope.getFlags().contains(IRFlags.DYNSCOPE_ELIMINATED);

            if (requireBinding || requireFrame) {
                BasicBlock entryBB = cfg.getEntryBB();
                // Push
                if (requireFrame) entryBB.addInstr(new PushFrameInstr(scope.getName()));
                if (requireBinding) entryBB.addInstr(new PushBindingInstr());

                // SSS FIXME: We are doing this conservatively.
                // Only scopes that have unrescued exceptions need a GEB.
                //
                // Allocate GEB if necessary for popping
                BasicBlock geb = cfg.getGlobalEnsureBB();
                if (geb == null) {
                    Variable exc = scope.createTemporaryVariable();
                    geb = new BasicBlock(cfg, Label.getGlobalEnsureBlockLabel());
                    geb.addInstr(new ReceiveJRubyExceptionInstr(exc)); // JRuby Implementation exception handling
                    geb.addInstr(new ThrowExceptionInstr(exc));
                    cfg.addGlobalEnsureBB(geb);
                }

                // Pop on all scope-exit paths
                for (BasicBlock bb: cfg.getBasicBlocks()) {
                    Instr i = null;
                    ListIterator<Instr> instrs = bb.getInstrs().listIterator();
                    while (instrs.hasNext()) {
                        i = instrs.next();
                        // Right now, we only support explicit call protocol on methods.
                        // So, non-local returns and breaks don't get here.
                        // Non-local-returns and breaks are tricky since they almost always
                        // throw an exception and we don't multiple pops (once before the
                        // return/break, and once when the exception is caught).
                        if (!bb.isExitBB() && i instanceof ReturnBase) {
                            // Add before the break/return
                            instrs.previous();
                            if (requireBinding) instrs.add(new PopBindingInstr());
                            if (requireFrame) instrs.add(new PopFrameInstr());
                            break;
                        }
                    }

                    if (bb.isExitBB() && !bb.isEmpty()) {
                        // Last instr could be a return -- so, move iterator one position back
                        if (i != null && i instanceof ReturnBase) instrs.previous();
                        if (requireBinding) instrs.add(new PopBindingInstr());
                        if (requireFrame) instrs.add(new PopFrameInstr());
                    }

                    if (bb == geb) {
                        // Add before throw-exception-instr which would be the last instr
                        if (i != null) {
                            // Assumption: Last instr should always be a control-transfer instruction
                            assert i.getOperation().transfersControl(): "Last instruction of GEB in scope: " + scope + " is " + i + ", not a control-xfer instruction";
                            instrs.previous();
                        }
                        if (requireBinding) instrs.add(new PopBindingInstr());
                        if (requireFrame) instrs.add(new PopFrameInstr());
                    }
                }
            }

            // This scope has an explicit call protocol flag now
            scope.setExplicitCallProtocolFlag();
        }

        // LVA information is no longer valid after the pass
        // FIXME: Grrr ... this seems broken to have to create a new object to invalidate
        (new LiveVariableAnalysis()).invalidate(scope);

        return null;
    }

    private boolean doesItRequireFrame(IRScope scope, boolean bindingHasEscaped) {
        boolean requireFrame = bindingHasEscaped || scope.usesEval();

        for (IRFlags flag : scope.getFlags()) {
            switch (flag) {
                case BINDING_HAS_ESCAPED:
                case CAN_CAPTURE_CALLERS_BINDING:
                case REQUIRES_FRAME:
                case REQUIRES_VISIBILITY:
                case USES_BACKREF_OR_LASTLINE:
                case USES_EVAL:
                case USES_ZSUPER:
                    requireFrame = true;
            }
        }

        return requireFrame;
    }

    @Override
    public boolean invalidate(IRScope scope) {
        // Cannot add call protocol instructions after we've added them once.
        return false;
    }
}
