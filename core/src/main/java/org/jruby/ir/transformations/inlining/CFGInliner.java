package org.jruby.ir.transformations.inlining;

import org.jruby.dirgra.Edge;
import org.jruby.RubyModule;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRManager;
import org.jruby.ir.IRScope;
import org.jruby.ir.Tuple;
import org.jruby.ir.instructions.*;
import org.jruby.ir.interpreter.FullInterpreterContext;
import org.jruby.ir.operands.*;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.ir.representations.CFG;
import org.jruby.ir.representations.CFG.EdgeType;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class CFGInliner {
    public static final Logger LOG = LoggerFactory.getLogger(CFGInliner.class);
    private static final boolean debug = IRManager.IR_INLINER_VERBOSE;
    private final FullInterpreterContext fullInterpreterContext;
    private final CFG cfg;
    private final IRScope hostScope;

    public CFGInliner(FullInterpreterContext fullInterpreterContext) {
        this.fullInterpreterContext = fullInterpreterContext;
        this.cfg = fullInterpreterContext.getCFG();
        this.hostScope = cfg.getScope();
    }

    private SimpleCloneInfo cloneHostInstrs() {
        SimpleCloneInfo ii = new SimpleCloneInfo(hostScope, false);
        for (BasicBlock b : cfg.getBasicBlocks()) {
            b.cloneInstrs(ii);
        }

        return ii;
    }

    private CFG cloneSelf(InlineCloneInfo ii) {
        CFG selfClone = new CFG(hostScope);

        // clone bbs
        for (BasicBlock b : cfg.getBasicBlocks()) {
            if (!b.isEntryBB() && !b.isExitBB()) selfClone.addBasicBlock(b.cloneForInlining(ii));
        }

        // clone edges
        for (BasicBlock b: cfg.getBasicBlocks()) {
            if (b.isEntryBB() || b.isExitBB()) continue;

            BasicBlock rb = ii.getRenamedBB(b);
            for (Edge<BasicBlock> e : cfg.getOutgoingEdges(b)) {
                BasicBlock destination = e.getDestination().getData();
                if (!destination.isExitBB()) selfClone.addEdge(rb, ii.getRenamedBB(destination), e.getType());
            }
        }

        return selfClone;
    }

    private boolean isRecursiveInline(IRScope methodScope) {
        return hostScope.getNearestMethod() == methodScope;
    }

    // Use receivers variable if it is one.  Otherwise make a new temp for one.
    private Variable getReceiverVariable(Operand receiver) {
        return receiver instanceof Variable ? (Variable) receiver : hostScope.getFullInterpreterContext().createTemporaryVariable();
    }

    public BasicBlock findCallsiteBB(CallBase call) {
        long callSiteId = call.getCallSiteId();
        if (debug) LOG.info("LOOKING FOR CALLSITEID: " + callSiteId);
        for (BasicBlock bb: cfg.getBasicBlocks()) {
            for (Instr i: bb.getInstrs()) {
                // Some instrs reuse instrs (like LineNumberInstr) so we need to add call check.
                if (i instanceof CallBase && ((CallBase) i).getCallSiteId() == callSiteId) {
                    if (debug) LOG.info("Found it!!!! -- " + call +  ", i: " + i);
                    return bb;
                }
            }
        }

        if (debug) LOG.info("Did not find it");
        return null;
    }

    private void printInlineDebugPrologue(IRScope scopeToInline, CallBase call) {
        LOG.info("---------------------------------- PROLOGUE (start) --------");
        LOG.info("Looking for: " + call.getCallSiteId() + ":\n    > " + call + "\n");
        printInlineCFG(cfg, "host of inline");
        LOG.info("method to inline cfg:\n" + scopeToInline.getFullInterpreterContext().getCFG().toStringGraph());
        LOG.info("method to inline instrs:\n" + scopeToInline.getFullInterpreterContext().getCFG().toStringInstrs());
        LOG.info("---------------------------------- PROLOGUE (end) -----------");
    }

    private void printInlineFoundBB(BasicBlock bb) {
        LOG.info("---------------------------------- callBB (start) -----------");
        LOG.info(bb.toStringInstrs());
        LOG.info("---------------------------------- callBB (end) -------------");
    }

    private void printInlineCannotFindCallsiteBB(CallBase call) {
        LOG.info("----------------------------------");
        LOG.info("Did not find BB with call: " + call);
        printInlineCFG(cfg, "");
        LOG.info("----------------------------------");
    }

    private void printInlineCFG(CFG aCFG, String label) {
        LOG.info(label + " cfg:\n" + aCFG.toStringGraph());
        LOG.info(label + " instrs:\n" + aCFG.toStringInstrs());
    }

    private void printInlineEpilogue() {
        LOG.info("---------------------------------- EPILOGUE (start) --------");
        printInlineCFG(cfg, "");
        LOG.info("---------------------------------- EPILOGUE (end) -----------");
    }

    private void printInlineSplitBBs(BasicBlock beforeBB, BasicBlock afterBB) {
        LOG.info("---------------------------------- SPLIT BB (start) --------");
        LOG.info("Before:" + beforeBB.getLabel());
        LOG.info(beforeBB.toStringInstrs());
        LOG.info("After:" + afterBB.getLabel());
        LOG.info(afterBB.toStringInstrs());
        printInlineCFG(cfg, "");
        LOG.info("---------------------------------- SPLIT BB (end) -----------");
    }

    // Vocabulary:
    //   hostScope - scope where the method will be inlining into
    //   methodScope - scope of the method to be inlined
    //   callBB - BB where callsite is located
    //   call - callsite where we want to inline the methods body.
    public String inlineMethod(IRScope scopeToInline, RubyModule implClass, int classToken, BasicBlock callBB, CallBase call, boolean cloneHost) {
        // Temporarily turn off inlining of recursive methods
        // Conservative turning off for inlining of a method in a closure nested within the same method
        if (isRecursiveInline(scopeToInline)) return "cannot inline recursive scopes";
        if (debug) printInlineDebugPrologue(scopeToInline, call);

        if (callBB == null) {
            callBB = findCallsiteBB(call);
            if (callBB == null) {
                if (debug) printInlineCannotFindCallsiteBB(call);
                return "cannot find callsite in host scope: " + call;
            } else {
                if (debug) printInlineFoundBB(callBB);
            }
        }

        // Split callsite bb, move outbound edges from callsite bb to split bb, and unhook call bb
        Label splitBBLabel = hostScope.getNewLabel();
        BasicBlock afterInlineBB = callBB.splitAtInstruction(call, splitBBLabel, false);
        BasicBlock beforeInlineBB = callBB;
        connectOuterEdges(beforeInlineBB, afterInlineBB);
        if (debug) printInlineSplitBBs(beforeInlineBB, afterInlineBB);

        SimpleCloneInfo hostCloneInfo = cloneHost ? cloneHostInstrs() : null;

        // Host method data init
        Variable callReceiverVar = getReceiverVariable(call.getReceiver());
        InlineCloneInfo ii = new InlineCloneInfo(call, cfg, callReceiverVar, scopeToInline);

        // Inlinee method data init
        CFG methodToInline = scopeToInline.getFullInterpreterContext().getCFG();
        List<BasicBlock> methodBBs = new ArrayList<>(methodToInline.getBasicBlocks());

        if (isRecursiveInline(scopeToInline)) {
            // 1. clone self
            // SSS: FIXME: We need a clone-graph api method in cfg and graph
            CFG selfClone = cloneSelf(ii);

            // 2. add callee bbs and their edges
            // SSS: FIXME: We need a swallow-graph api method in cfg and graph
            for (BasicBlock b : selfClone.getBasicBlocks()) {
                cfg.addBasicBlock(b);
                for (Edge<BasicBlock> e : selfClone.getOutgoingEdges(b)) {
                    cfg.addEdge(b, e.getDestination().getData(), e.getType());
                }
            }
        } else {
            // clone callee and add it to the host cfg
            for (BasicBlock b : methodToInline.getBasicBlocks()) {
                if (!b.isEntryBB() && !b.isExitBB()) cfg.addBasicBlock(b.cloneForInlining(ii));
            }
            for (BasicBlock x : methodToInline.getBasicBlocks()) {
                if (x.isEntryBB() || x.isExitBB()) continue;

                BasicBlock rx = ii.getRenamedBB(x);
                for (Edge<BasicBlock> e : methodToInline.getOutgoingEdges(x)) {
                    BasicBlock b = e.getDestination().getData();
                    if (!b.isExitBB()) cfg.addEdge(rx, ii.getRenamedBB(b), e.getType());
                }
            }
        }

        // Hook up entry edges
        assert methodToInline.outDegree(methodToInline.getEntryBB()) == 2: "Entry BB of inlinee method does not have outdegree 2: " + methodToInline.toStringGraph();
        for (BasicBlock destination : methodToInline.getOutgoingDestinations(methodToInline.getEntryBB())) {
            if (destination.isExitBB()) continue;

            BasicBlock dstBB = ii.getRenamedBB(destination);
            // Receiver is not a variable so we made a new temp above...copy into new temp the original recv value.
            if (call.getReceiver() != callReceiverVar) {
                dstBB.insertInstr(new CopyInstr(callReceiverVar, call.getReceiver()));
            }

            if (!ii.canMapArgsStatically()) {
                return "cannot assign non-statically assigned method arguments";
                /*
                // FIXME: fail for now
                // SSS FIXME: This is buggy!
                // This code has to mimic whatever CallBase.prepareArguments does!
                // We may need a special instruction that takes care of this.
                Operand args;
                Operand[] callArgs = call.cloneCallArgs(hostCloneInfo);
                if (callArgs.length == 1 && callArgs[0] instanceof Splat) {
                    args = callArgs[0];
                } else {
                    args = new Array(callArgs);
                }
                dstBB.insertInstr(new CopyInstr((Variable)ii.getArgs(), args));
                */
            }
            cfg.addEdge(beforeInlineBB, dstBB, CFG.EdgeType.FALL_THROUGH);
        }

        // Hook up exit edges
        for (Edge<BasicBlock> e : methodToInline.getIncomingEdges(methodToInline.getExitBB())) {
            BasicBlock source = e.getSource().getData();
            if (source.isEntryBB()) continue;

            BasicBlock clonedSource = ii.getRenamedBB(source);
            if (e.getType() == EdgeType.EXCEPTION) {
                // e._src has an explicit throw that returns from the callee
                // after inlining, if the caller instruction has a rescuer, then the
                // throw has to be captured by the rescuer as well.
                BasicBlock rescuerOfSplitBB = cfg.getRescuerBBFor(afterInlineBB);
                if (rescuerOfSplitBB != null) {
                    cfg.addEdge(clonedSource, rescuerOfSplitBB, EdgeType.EXCEPTION);
                } else {
                    cfg.addEdge(clonedSource, cfg.getExitBB(), EdgeType.EXIT);
                }
            } else {
                cfg.addEdge(clonedSource, afterInlineBB, e.getType());
            }
        }

        // Update bb rescuer map
        // splitBB will be protected by the same bb as beforeInlineBB
        BasicBlock callBBrescuer = cfg.getRescuerBBFor(beforeInlineBB);
        if (callBBrescuer != null) cfg.setRescuerBB(afterInlineBB, callBBrescuer);

        // Remap existing protections for bbs in mcfg to their renamed bbs.
        // bbs in mcfg that aren't protected by an existing bb will be protected by callBBrescuer.
        for (BasicBlock x : methodBBs) {
            if (x.isEntryBB() || x.isExitBB()) continue;

            BasicBlock xRenamed = ii.getRenamedBB(x);
            BasicBlock xProtector = methodToInline.getRescuerBBFor(x);
            if (xProtector != null) {
                cfg.setRescuerBB(xRenamed, ii.getRenamedBB(xProtector));
            } else if (callBBrescuer != null) {
                cfg.setRescuerBB(xRenamed, callBBrescuer);
            }
        }

        // We run this again so new inlined BBs will get assigned proper IPCs
        fullInterpreterContext.generateInstructionsForInterpretation();

        // Add inline guard that verifies that the method inlined is the same
        // that gets called in future invocations.  In addition to the guard, add
        // a failure path code.
        Label failurePathLabel = hostScope.getNewLabel();
        beforeInlineBB.addInstr(new ModuleVersionGuardInstr(implClass, classToken, call.getReceiver(), failurePathLabel));

        BasicBlock failurePathBB = new BasicBlock(cfg, failurePathLabel);
        cfg.addBasicBlock(failurePathBB);
        //failurePathBB.addInstr(new RaiseArgumentErrorInstr(1, 2, false, 10));
        failurePathBB.addInstr(call);
        failurePathBB.addInstr(new JumpInstr(hostCloneInfo == null ? splitBBLabel : hostCloneInfo.getRenamedLabel(splitBBLabel)));
        call.blockInlining();

        // We made a new BB with a call which can always raise an exception so add exception edge.
        cfg.addEdge(failurePathBB, callBB.exceptionBB(), EdgeType.EXCEPTION);
        cfg.setRescuerBB(failurePathBB, callBB.exceptionBB());

        cfg.addEdge(beforeInlineBB, failurePathBB, CFG.EdgeType.REGULAR);
        cfg.addEdge(failurePathBB, afterInlineBB, CFG.EdgeType.REGULAR);

        // Inline any closure argument passed into the call.
        Operand closureArg = call.getClosureArg(null);
        List<Tuple<BasicBlock, YieldInstr>> yieldSites = ii.getYieldSites();
        if (closureArg != null && !yieldSites.isEmpty()) {
            // FIXME: Do we care if we have too many yields?

            if (!(closureArg instanceof WrappedIRClosure)) {
                throw new RuntimeException("Encountered a dynamic closure arg.  Cannot inline it here!  Convert the yield to a call by converting the closure into a dummy method (have to convert all frame vars to call arguments, or at least convert the frame into a call arg");
            }

            for (Tuple t: yieldSites) {
                inlineClosureAtYieldSite(ii, ((WrappedIRClosure) closureArg).getClosure(), (BasicBlock) t.a, (YieldInstr) t.b);
            }
        }

        // Optimize cfg by merging straight-line bbs (just one piece of what CFG.optimize does)
        //cfg.collapseStraightLineBBs();

        /*
        // FIXME: This probably is too much work here. Decide between this and just collapsing straight line BBs
        // FIXME: If we do keep this we should maybe internalize calculating these in CFG itself.
        List<BasicBlock> returnBBs = new ArrayList<>();
        for (BasicBlock basicBlock: cfg.getBasicBlocks()) {
            for (Instr instr: basicBlock.getInstrs()) {
                if (instr.getOperation().isReturn()) returnBBs.add(basicBlock);
            }
        }
        cfg.optimize(returnBBs);*/
        cfg.optimize();

        addMissingJumps();

        if (debug) printInlineEpilogue();
/*
        LOG.info("final cfg   :" + cfg.toStringGraph());
        LOG.info("final instrs:" + cfg.toStringInstrs());
*/
        return null; // success!
    }

    // FIXME: Adding some more similar logic and we could make a CFG verifier looking for invalid CFGs (e.g. mismatched edge vs instrs in it).
    // FIXME: original inlined EXIT scopes should be renamed as regular edges as part of inline since they are no longer actually exits.
    private void addMissingJumps() {
        for (BasicBlock bb: cfg.getBasicBlocks()) {
            boolean fallThrough = false;
            Label jumpLabel = null;

            for (Edge<BasicBlock> edge : cfg.getOutgoingEdges(bb)) {
                if (edge.getType() == EdgeType.FALL_THROUGH) {           // Assume next BB will be correct
                    fallThrough = true;
                } else if (edge.getType() == EdgeType.REGULAR || edge.getType() == EdgeType.EXIT) {         // Not sure if we can have regular and fallthrough but only add regular if no fallthrough
                    if (fallThrough) continue;
                    jumpLabel = edge.getDestination().getData().getLabel();
                }
            }

            if (fallThrough) continue; // we know this will just go to next BB already so no missing jump.
            if (jumpLabel == null) continue;  // last instr does not transfer control so nothing to add.

            Instr lastInstr = bb.getLastInstr();
            if (lastInstr != null && !lastInstr.transfersControl()) {
                bb.addInstr(new JumpInstr(jumpLabel));
            }
        }
    }

    // Make all original outgoing edges get moved to the afterInlineBB since it is now
    // a new BB after the original BB and remove those from the original BB (beforeInlineBB).
    private void connectOuterEdges(BasicBlock beforeInlineBB, BasicBlock afterInlineBB) {
        cfg.addBasicBlock(afterInlineBB);
        for (Edge<BasicBlock> e : cfg.getOutgoingEdges(beforeInlineBB)) {
            cfg.addEdge(afterInlineBB, e.getDestination().getData(), e.getType());
        }
        cfg.removeAllOutgoingEdgesForBB(beforeInlineBB);
    }

    private void inlineClosureAtYieldSite(InlineCloneInfo ii, IRClosure cl, BasicBlock yieldBB, YieldInstr yield) {
        // 1. split yield site bb and move outbound edges from yield site bb to split bb.
        BasicBlock afterInlineBB = yieldBB.splitAtInstruction(yield, hostScope.getNewLabel(), false);
        BasicBlock beforeInlineBB = yieldBB;
        connectOuterEdges(beforeInlineBB, afterInlineBB);
        if (debug) printInlineSplitBBs(beforeInlineBB, afterInlineBB);

        // Allocate new inliner object to reset variable and label rename maps
        ii = ii.cloneForInliningClosure(cl);
        ii.setupYieldArgsAndYieldResult(yield, beforeInlineBB, cl.getBlockBody().getSignature().arityValue());

        // 2. Merge closure cfg into the current cfg
        CFG closureCFG = cl.getFullInterpreterContext().getCFG();

        BasicBlock closureGEB = closureCFG.getGlobalEnsureBB();
        for (BasicBlock b : closureCFG.getBasicBlocks()) {
            if (!b.isEntryBB() && !b.isExitBB() && b != closureGEB) cfg.addBasicBlock(b.cloneForInlining(ii));
        }

        for (BasicBlock b : closureCFG.getBasicBlocks()) {
            if (b.isEntryBB() || b.isExitBB()) continue;

            BasicBlock bClone = ii.getRenamedBB(b);
            for (Edge<BasicBlock> e : closureCFG.getOutgoingEdges(b)) {
                BasicBlock edst = e.getDestination().getData();
                if (!edst.isExitBB() && edst != closureGEB) cfg.addEdge(bClone, ii.getRenamedBB(edst), e.getType());
            }
        }

        // Hook up entry edges
        for (Edge<BasicBlock> e : closureCFG.getOutgoingEdges(closureCFG.getEntryBB())) {
            BasicBlock destination = e.getDestination().getData();
            if (!destination.isExitBB() && destination != closureGEB) {
                cfg.addEdge(beforeInlineBB, ii.getRenamedBB(destination), CFG.EdgeType.FALL_THROUGH);
            }
        }

        // Hook up exit edges
        for (Edge<BasicBlock> e : closureCFG.getIncomingEdges(closureCFG.getExitBB())) {
            BasicBlock source = e.getSource().getData();
            if (source.isEntryBB()) continue;

            BasicBlock clonedSource = ii.getRenamedBB(source);

            if (e.getType() == EdgeType.EXCEPTION) {
                // e._src has an explicit throw that returns from the closure.
                // After inlining, if the yield instruction has a rescuer, then the
                // throw has to be captured by the rescuer as well.
                BasicBlock rescuerOfSplitBB = cfg.getRescuerBBFor(afterInlineBB);
                if (rescuerOfSplitBB != null) {
                    cfg.addEdge(clonedSource, rescuerOfSplitBB, EdgeType.EXCEPTION);
                } else {
                    cfg.addEdge(clonedSource, cfg.getExitBB(), EdgeType.EXIT);
                }
            } else if (source != closureGEB) {
                cfg.addEdge(clonedSource, afterInlineBB, e.getType());
            }
        }

        // 6. Update bb rescuer map
        // 6a. splitBB will be protected by the same bb as yieldB
        BasicBlock yieldBBrescuer = cfg.getRescuerBBFor(beforeInlineBB);
        if (yieldBBrescuer != null) cfg.setRescuerBB(afterInlineBB, yieldBBrescuer);

        // 6b. remap existing protections for bbs in mcfg to their renamed bbs.
        // 6c. bbs in mcfg that aren't protected by an existing bb will be protected by yieldBBrescuer/yieldBBensurer
        for (BasicBlock cb : closureCFG.getBasicBlocks()) {
            if (cb.isEntryBB() || cb.isExitBB() || cb == closureGEB) continue;

            BasicBlock cbProtector = ii.getRenamedBB(closureCFG.getRescuerBBFor(cb));
            if (cbProtector != null) {
                cfg.setRescuerBB(ii.getRenamedBB(cb), cbProtector);
            } else if (yieldBBrescuer != null) {
                cfg.setRescuerBB(ii.getRenamedBB(cb), yieldBBrescuer);
            }
        }
    }
}
