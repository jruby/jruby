package org.jruby.ir.passes;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import org.jruby.ir.IRClosure;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRModuleBody;
import org.jruby.ir.IRScriptBody;
import org.jruby.ir.IRScope;
import org.jruby.ir.instructions.BreakInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.PopBindingInstr;
import org.jruby.ir.instructions.PopFrameInstr;
import org.jruby.ir.instructions.PushBindingInstr;
import org.jruby.ir.instructions.PushFrameInstr;
import org.jruby.ir.instructions.ReceiveExceptionInstr;
import org.jruby.ir.instructions.ReturnBase;
import org.jruby.ir.instructions.ThrowExceptionInstr;
import org.jruby.ir.dataflow.analyses.StoreLocalVarPlacementProblem;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.ir.representations.CFG;

public class AddCallProtocolInstructions extends CompilerPass {
    boolean addedInstrs = false;

    @Override
    public String getLabel() {
        return "Add Call Protocol Instructions (push/pop of dyn-scope, frame, impl-class values)";
    }

    public static List<Class<? extends CompilerPass>> DEPENDENCIES = Arrays.<Class<? extends CompilerPass>>asList(CFGBuilder.class);

    @Override
    public List<Class<? extends CompilerPass>> getDependencies() {
        return DEPENDENCIES;
    }

    @Override
    public Object execute(IRScope scope, Object... data) {
        StoreLocalVarPlacementProblem slvpp = (StoreLocalVarPlacementProblem)scope.getDataFlowSolution(StoreLocalVarPlacementProblem.NAME);

        boolean scopeHasLocalVarStores      = false;
        boolean scopeHasUnrescuedExceptions = false;

        CFG        cfg = scope.cfg();
        BasicBlock geb = cfg.getGlobalEnsureBB();

        if (slvpp != null) {
            scopeHasLocalVarStores      = slvpp.scopeHasLocalVarStores();
            scopeHasUnrescuedExceptions = slvpp.scopeHasUnrescuedExceptions();
        } else {
            // We dont require local-var load/stores to have been run.
            // If it is not run, we go conservative and add push/pop binding instrs. everywhere
            scopeHasLocalVarStores      = true;
            scopeHasUnrescuedExceptions = false;
            for (BasicBlock bb: cfg.getBasicBlocks()) {
                // SSS FIXME: This is highly conservative.  If the bb has an exception raising instr.
                // and if we dont have a rescuer, only then do we have unrescued exceptions.
                if (cfg.getRescuerBBFor(bb) == null) {
                    scopeHasUnrescuedExceptions = true;
                    break;
                }
            }
        }

        BasicBlock entryBB = cfg.getEntryBB();

        // SSS FIXME: Right now, we always add push/pop frame instrs -- in the future, we may skip them
        // for certain scopes.
        //
        // Add explicit frame and binding push/pop instrs ONLY for methods -- we cannot handle this in closures and evals yet
        // If the scope uses $_ or $~ family of vars, has local load/stores, or if its binding has escaped, we have
        // to allocate a dynamic scope for it and add binding push/pop instructions.
        if ((scope instanceof IRMethod) || (scope instanceof IRScriptBody) || (scope instanceof IRModuleBody)) {
            if (scope.bindingHasEscaped() || scope.usesBackrefOrLastline() || scopeHasLocalVarStores || scopeHasUnrescuedExceptions) {
                // Push
                entryBB.addInstr(new PushFrameInstr());
                entryBB.addInstr(new PushBindingInstr(scope));

                // Allocate GEB if necessary for popping binding
                if (geb == null && (scopeHasLocalVarStores || scopeHasUnrescuedExceptions)) {
                    Variable exc = scope.getNewTemporaryVariable();
                    geb = new BasicBlock(cfg, new Label("_GLOBAL_ENSURE_BLOCK"));
                    geb.addInstr(new ReceiveExceptionInstr(exc, false)); // No need to check type since it is not used before rethrowing
                    geb.addInstr(new ThrowExceptionInstr(exc));
                    cfg.addGlobalEnsureBB(geb);
                }

                // Pop on all scope-exit paths
                BasicBlock exitBB = cfg.getExitBB();
                for (BasicBlock bb: cfg.getBasicBlocks()) {
                    ListIterator<Instr> instrs = bb.getInstrs().listIterator();
                    while (instrs.hasNext()) {
                        Instr i = instrs.next();
                        if ((bb != exitBB) && (i instanceof ReturnBase) || (i instanceof BreakInstr)) {
                            // Add before the break/return
                            instrs.previous();
                            instrs.add(new PopBindingInstr());
                            instrs.add(new PopFrameInstr());
                            break;
                        }
                    }

                    if ((bb == exitBB) && !bb.isEmpty()) {
                        // Last instr could be a return -- so, move iterator one position back
                        if (instrs.hasPrevious()) instrs.previous();
                        instrs.add(new PopBindingInstr());
                        instrs.add(new PopFrameInstr());
                    }

                    if (bb == geb) {
                        // Add before throw-exception-instr which would be the last instr
                        instrs.previous();
                        instrs.add(new PopBindingInstr());
                        instrs.add(new PopFrameInstr());
                    }
                }
            }

            // This scope has an explicit call protocol flag now
            scope.setExplicitCallProtocolFlag(true);
        }

        // Run on all nested closures.
        for (IRClosure c: scope.getClosures()) execute(c);

        // Mark as done
        addedInstrs = true;

        return null;
    }

    @Override
    public Object previouslyRun(IRScope scope) {
        return addedInstrs ? new Object() : null;
    }

    @Override
    public void invalidate(IRScope scope) {
        // Cannot add call protocol instructions after we've added them once.
    }
}
