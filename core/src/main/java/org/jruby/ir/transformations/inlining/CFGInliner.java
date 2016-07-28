package org.jruby.ir.transformations.inlining;

import org.jruby.dirgra.Edge;
import org.jruby.RubyModule;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRScope;
import org.jruby.ir.Tuple;
import org.jruby.ir.instructions.*;
import org.jruby.ir.operands.*;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.ir.representations.CFG;
import org.jruby.ir.representations.CFG.EdgeType;

import java.util.ArrayList;
import java.util.List;

public class CFGInliner {
    private static final boolean debug = false;
    private final CFG cfg;
    private final IRScope hostScope;

    public CFGInliner(CFG build) {
        this.cfg = build;
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
        return receiver instanceof Variable ? (Variable) receiver : hostScope.createTemporaryVariable();
    }

    private BasicBlock findCallsiteBB(CallBase call) {
        for (BasicBlock bb: cfg.getBasicBlocks()) {
            for (Instr i: bb.getInstrs()) {
                // FIXME: This code will be replaced by landing of inliner branch so commenting out ipc for now
                // System.out.println("IPC " + i.getIPC() + " = " + i);
                //if (i.getIPC() == call.getIPC()) {
                    // System.out.println("Found it!!!! -- " + call);
                 //   return bb;
                //}
            }
        }

        return null;
    }

    private void printInlineDebugPrologue(IRScope methodScope, CallBase call) {
        //System.out.println("Looking for: " + call.getIPC() + ": " + call);
        System.out.println("host cfg   :" + cfg.toStringGraph());
        System.out.println("host instrs:" + cfg.toStringInstrs());
        System.out.println("source cfg   :" + methodScope.getCFG().toStringGraph());
        System.out.println("source instrs:" + methodScope.getCFG().toStringInstrs());
    }

    private void printInlineCannotFindCallsiteBB(CallBase call) {
        System.out.println("----------------------------------");
        System.out.println("Did not find BB with call: " + call);
        System.out.println("Host cfg   :" + cfg.toStringGraph());
        System.out.println("Host instrs:" + cfg.toStringInstrs());
        System.out.println("----------------------------------");
    }

    // Vocabulary:
    //   hostScope - scope where the method will be inlining into
    //   methodScope - scope of the method to be inlined
    //   callBB - BB where callsite is located
    //   call - callsite where we want to inline the methods body.
    public void inlineMethod(IRScope methodScope, RubyModule implClass, int classToken, BasicBlock callBB, CallBase call, boolean cloneHost) {
        // Temporarily turn off inlining of recursive methods
        // Conservative turning off for inlining of a method in a closure nested within the same method
        if (isRecursiveInline(methodScope)) return;
        if (debug) printInlineDebugPrologue(methodScope, call);

        if (callBB == null) callBB = findCallsiteBB(call);
        if (callBB == null) {
            if (debug) printInlineCannotFindCallsiteBB(call);
            return;
        }

        // Split callsite bb, move outbound edges from callsite bb to split bb, and unhook call bb
        Label splitBBLabel = hostScope.getNewLabel();
        BasicBlock splitBB = callBB.splitAtInstruction(call, splitBBLabel, false);
        cfg.addBasicBlock(splitBB);
        for (Edge<BasicBlock> e : cfg.getOutgoingEdges(callBB)) {
            cfg.addEdge(splitBB, e.getDestination().getData(), e.getType());
        }
        cfg.removeAllOutgoingEdgesForBB(callBB);

        SimpleCloneInfo hostCloneInfo = cloneHost ? cloneHostInstrs() : null;

        // Host method data init
        Variable callReceiverVar = getReceiverVariable(call.getReceiver());
        InlineCloneInfo ii = new InlineCloneInfo(call, cfg, callReceiverVar, methodScope);

        // Inlinee method data init
        CFG methodCFG = methodScope.getCFG();
        List<BasicBlock> methodBBs = new ArrayList<>(methodCFG.getBasicBlocks());

        if (isRecursiveInline(methodScope)) {
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
            for (BasicBlock b : methodCFG.getBasicBlocks()) {
                if (!b.isEntryBB() && !b.isExitBB()) cfg.addBasicBlock(b.cloneForInlining(ii));
            }
            for (BasicBlock x : methodCFG.getBasicBlocks()) {
                if (x.isEntryBB() || x.isExitBB()) continue;

                BasicBlock rx = ii.getRenamedBB(x);
                for (Edge<BasicBlock> e : methodCFG.getOutgoingEdges(x)) {
                    BasicBlock b = e.getDestination().getData();
                    if (!b.isExitBB()) cfg.addEdge(rx, ii.getRenamedBB(b), e.getType());
                }
            }
        }

        // Hook up entry edges
        assert methodCFG.outDegree(methodCFG.getEntryBB()) == 2: "Entry BB of inlinee method does not have outdegree 2: " + methodCFG.toStringGraph();
        for (BasicBlock destination : methodCFG.getOutgoingDestinations(methodCFG.getEntryBB())) {
            if (destination.isExitBB()) continue;

            BasicBlock dstBB = ii.getRenamedBB(destination);
            // Receiver is not a variable so we made a new temp above...copy into new temp the original recv value.
            if (call.getReceiver() != callReceiverVar) {
                dstBB.insertInstr(new CopyInstr(callReceiverVar, call.getReceiver()));
            }

            if (!ii.canMapArgsStatically()) {
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
            }
            cfg.addEdge(callBB, dstBB, CFG.EdgeType.FALL_THROUGH);
        }

        // Hook up exit edges
        for (Edge<BasicBlock> e : methodCFG.getIncomingEdges(methodCFG.getExitBB())) {
            BasicBlock source = e.getSource().getData();
            if (source.isEntryBB()) continue;

            BasicBlock clonedSource = ii.getRenamedBB(source);
            if (e.getType() == EdgeType.EXCEPTION) {
                // e._src has an explicit throw that returns from the callee
                // after inlining, if the caller instruction has a rescuer, then the
                // throw has to be captured by the rescuer as well.
                BasicBlock rescuerOfSplitBB = cfg.getRescuerBBFor(splitBB);
                if (rescuerOfSplitBB != null) {
                    cfg.addEdge(clonedSource, rescuerOfSplitBB, EdgeType.EXCEPTION);
                } else {
                    cfg.addEdge(clonedSource, cfg.getExitBB(), EdgeType.EXIT);
                }
            } else {
                cfg.addEdge(clonedSource, splitBB, e.getType());
            }
        }

        // Update bb rescuer map
        // splitBB will be protected by the same bb as callBB
        BasicBlock callBBrescuer = cfg.getRescuerBBFor(callBB);
        if (callBBrescuer != null) cfg.setRescuerBB(splitBB, callBBrescuer);

        // Remap existing protections for bbs in mcfg to their renamed bbs.
        // bbs in mcfg that aren't protected by an existing bb will be protected by callBBrescuer.
        for (BasicBlock x : methodBBs) {
            if (x.isEntryBB() || x.isExitBB()) continue;

            BasicBlock xRenamed = ii.getRenamedBB(x);
            BasicBlock xProtector = methodCFG.getRescuerBBFor(x);
            if (xProtector != null) {
                cfg.setRescuerBB(xRenamed, ii.getRenamedBB(xProtector));
            } else if (callBBrescuer != null) {
                cfg.setRescuerBB(xRenamed, callBBrescuer);
            }
        }

        // Add inline guard that verifies that the method inlined is the same
        // that gets called in future invocations.  In addition to the guard, add
        // a failure path code.
        Label failurePathLabel = hostScope.getNewLabel();
        callBB.addInstr(new ModuleVersionGuardInstr(implClass, classToken, call.getReceiver(), failurePathLabel));

        BasicBlock failurePathBB = new BasicBlock(cfg, failurePathLabel);
        cfg.addBasicBlock(failurePathBB);
        failurePathBB.addInstr(call);
        failurePathBB.addInstr(new JumpInstr(hostCloneInfo == null ? splitBBLabel : hostCloneInfo.getRenamedLabel(splitBBLabel)));
        call.blockInlining();

        cfg.addEdge(callBB, failurePathBB, CFG.EdgeType.REGULAR);
        cfg.addEdge(failurePathBB, splitBB, CFG.EdgeType.REGULAR);

        // Inline any closure argument passed into the call.
        Operand closureArg = call.getClosureArg(null);
        List yieldSites = ii.getYieldSites();
        if (closureArg != null && !yieldSites.isEmpty()) {
            // Detect unlikely but contrived scenarios where there are far too many yield sites that could lead to code blowup
            // if we inline the closure at all those yield sites!
            if (yieldSites.size() > 1) {
                throw new RuntimeException("Encountered " + yieldSites.size() + " yield sites.  Convert the yield to a call by converting the closure into a dummy method (have to convert all frame vars to call arguments, or at least convert the frame into a call arg");
            }

            if (!(closureArg instanceof WrappedIRClosure)) {
                throw new RuntimeException("Encountered a dynamic closure arg.  Cannot inline it here!  Convert the yield to a call by converting the closure into a dummy method (have to convert all frame vars to call arguments, or at least convert the frame into a call arg");
            }

            Tuple t = (Tuple) yieldSites.get(0);
            inlineClosureAtYieldSite(ii, ((WrappedIRClosure) closureArg).getClosure(), (BasicBlock) t.a, (YieldInstr) t.b);
        }

        // Optimize cfg by merging straight-line bbs
        cfg.collapseStraightLineBBs();
/*
        System.out.println("final cfg   :" + cfg.toStringGraph());
        System.out.println("final instrs:" + cfg.toStringInstrs());
*/
    }

    private void inlineClosureAtYieldSite(InlineCloneInfo ii, IRClosure cl, BasicBlock yieldBB, YieldInstr yield) {
        // 1. split yield site bb and move outbound edges from yield site bb to split bb.
        BasicBlock splitBB = yieldBB.splitAtInstruction(yield, hostScope.getNewLabel(), false);
        cfg.addBasicBlock(splitBB);
        for (Edge<BasicBlock> e : cfg.getOutgoingEdges(yieldBB)) {
            cfg.addEdge(splitBB, e.getDestination().getData(), e.getType());
        }

        cfg.removeAllOutgoingEdgesForBB(yieldBB);

        // Allocate new inliner object to reset variable and label rename maps
        ii = ii.cloneForInliningClosure(cl);
        ii.setupYieldArgsAndYieldResult(yield, yieldBB, cl.getBlockBody().getSignature().arityValue());

        // 2. Merge closure cfg into the current cfg
        CFG closureCFG = cl.getCFG();
        for (BasicBlock b : closureCFG.getBasicBlocks()) {
            if (!b.isEntryBB() && !b.isExitBB()) cfg.addBasicBlock(b.cloneForInlining(ii));
        }

        for (BasicBlock b : closureCFG.getBasicBlocks()) {
            if (b.isEntryBB() || b.isExitBB()) continue;

            BasicBlock bClone = ii.getRenamedBB(b);
            for (Edge<BasicBlock> e : closureCFG.getOutgoingEdges(b)) {
                BasicBlock edst = e.getDestination().getData();
                if (!edst.isExitBB()) cfg.addEdge(bClone, ii.getRenamedBB(edst), e.getType());
            }
        }

        // Hook up entry edges
        for (Edge<BasicBlock> e : closureCFG.getOutgoingEdges(closureCFG.getEntryBB())) {
            BasicBlock destination = e.getDestination().getData();
            if (!destination.isExitBB()) {
                cfg.addEdge(yieldBB, ii.getRenamedBB(destination), CFG.EdgeType.FALL_THROUGH);
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
                    BasicBlock rescuerOfSplitBB = cfg.getRescuerBBFor(splitBB);
                    if (rescuerOfSplitBB != null) {
                        cfg.addEdge(clonedSource, rescuerOfSplitBB, EdgeType.EXCEPTION);
                    } else {
                        cfg.addEdge(clonedSource, cfg.getExitBB(), EdgeType.EXIT);
                    }
                } else {
                    cfg.addEdge(clonedSource, splitBB, e.getType());
                }
        }

        // 6. Update bb rescuer map
        // 6a. splitBB will be protected by the same bb as yieldB
        BasicBlock yieldBBrescuer = cfg.getRescuerBBFor(yieldBB);
        if (yieldBBrescuer != null) cfg.setRescuerBB(splitBB, yieldBBrescuer);

        // 6b. remap existing protections for bbs in mcfg to their renamed bbs.
        // 6c. bbs in mcfg that aren't protected by an existing bb will be protected by yieldBBrescuer/yieldBBensurer
        for (BasicBlock cb : closureCFG.getBasicBlocks()) {
            if (cb.isEntryBB() || cb.isExitBB()) continue;

            BasicBlock cbProtector = ii.getRenamedBB(closureCFG.getRescuerBBFor(cb));
            if (cbProtector != null) {
                cfg.setRescuerBB(cb, cbProtector);
            } else if (yieldBBrescuer != null) {
                cfg.setRescuerBB(cb, yieldBBrescuer);
            }
        }
    }
}
