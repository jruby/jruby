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
import org.jruby.ir.instructions.ReceiveJRubyExceptionInstr;
import org.jruby.ir.instructions.ReturnBase;
import org.jruby.ir.instructions.ThrowExceptionInstr;
import org.jruby.ir.dataflow.analyses.LiveVariablesProblem;
import org.jruby.ir.dataflow.analyses.StoreLocalVarPlacementProblem;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.MethAddr;
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
        // IRScriptBody do not get explicit call protocol instructions right now.
        // They dont push/pop a frame and do other special things like run begin/end blocks.
        // So, for now, they go through the runtime stub in IRScriptBody.
        //
        // SSS FIXME: Right now, we always add push/pop frame instrs -- in the future, we may skip them
        // for certain scopes.
        //
        // Add explicit frame and binding push/pop instrs ONLY for methods -- we cannot handle this in closures and evals yet
        // If the scope uses $_ or $~ family of vars, has local load/stores, or if its binding has escaped, we have
        // to allocate a dynamic scope for it and add binding push/pop instructions.
        if (scope instanceof IRMethod || scope instanceof IRModuleBody) {
            StoreLocalVarPlacementProblem slvpp = (StoreLocalVarPlacementProblem)scope.getDataFlowSolution(StoreLocalVarPlacementProblem.NAME);

            boolean scopeHasLocalVarStores      = false;
            boolean scopeHasUnrescuedExceptions = false;
            boolean bindingHasEscaped           = scope.bindingHasEscaped();

            CFG        cfg = scope.cfg();
            BasicBlock geb = cfg.getGlobalEnsureBB();

            if (slvpp != null && bindingHasEscaped) {
                scopeHasLocalVarStores      = slvpp.scopeHasLocalVarStores();
                scopeHasUnrescuedExceptions = slvpp.scopeHasUnrescuedExceptions();
            } else {
                // We dont require local-var load/stores to have been run.
                // If it is not run, we go conservative and add push/pop binding instrs. everywhere
                scopeHasLocalVarStores      = bindingHasEscaped;
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

            // FIXME: Why do we need a push/pop for frame & binding for scopes with unrescued exceptions??
            // 1. I think we need a different check for frames -- it is NOT scopeHasUnrescuedExceptions
            //    We need scope.requiresFrame() to push/pop frames
            // 2. Plus bindingHasEscaped check in IRScope is missing some other check since we should
            //    jsut be able to check (bindingHasEscaped || scopeHasVarStores) to push/pop bindings.
            // We need scopeHasUnrescuedExceptions to add GEB for popping frame/binding on exit from unrescued exceptions
            BasicBlock entryBB = cfg.getEntryBB();
            boolean requireBinding = bindingHasEscaped || scopeHasLocalVarStores;
            if (scope.usesBackrefOrLastline() || requireBinding || scopeHasUnrescuedExceptions) {
                // Push
                entryBB.addInstr(new PushFrameInstr(new MethAddr(scope.getName())));
                if (requireBinding) entryBB.addInstr(new PushBindingInstr(scope));

                // Allocate GEB if necessary for popping
                if (geb == null && scopeHasUnrescuedExceptions) {
                    Variable exc = scope.getNewTemporaryVariable();
                    geb = new BasicBlock(cfg, new Label("_GLOBAL_ENSURE_BLOCK", 0));
                    geb.addInstr(new ReceiveJRubyExceptionInstr(exc)); // JRuby Implementation exception handling
                    geb.addInstr(new ThrowExceptionInstr(exc));
                    cfg.addGlobalEnsureBB(geb);
                }

                // Pop on all scope-exit paths
                for (BasicBlock bb: cfg.getBasicBlocks()) {
                    ListIterator<Instr> instrs = bb.getInstrs().listIterator();
                    while (instrs.hasNext()) {
                        Instr i = instrs.next();
                        if (!bb.isExitBB() && (i instanceof ReturnBase) || (i instanceof BreakInstr)) {
                            // Add before the break/return
                            instrs.previous();
                            if (requireBinding) instrs.add(new PopBindingInstr());
                            instrs.add(new PopFrameInstr());
                            break;
                        }
                    }

                    if (bb.isExitBB() && !bb.isEmpty()) {
                        // Last instr could be a return -- so, move iterator one position back
                        if (instrs.hasPrevious()) instrs.previous();
                        if (requireBinding) instrs.add(new PopBindingInstr());
                        instrs.add(new PopFrameInstr());
                    }

                    if (bb == geb && scopeHasUnrescuedExceptions) {
                        // Add before throw-exception-instr which would be the last instr
                        instrs.previous();
                        if (requireBinding) instrs.add(new PopBindingInstr());
                        instrs.add(new PopFrameInstr());
                    }
                }
            }

            // This scope has an explicit call protocol flag now
            scope.setExplicitCallProtocolFlag();
        }

        // FIXME: Useless for now
        // Run on all nested closures.
        for (IRClosure c: scope.getClosures()) execute(c);

        // Mark as done
        addedInstrs = true;

        // LVA information is no longer valid after the pass
        scope.setDataFlowSolution(LiveVariablesProblem.NAME, null);

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
