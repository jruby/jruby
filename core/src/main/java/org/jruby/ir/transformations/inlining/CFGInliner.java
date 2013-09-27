package org.jruby.ir.transformations.inlining;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;

import org.jruby.RubyModule;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRScope;
import org.jruby.ir.Tuple;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.CallBase;
import org.jruby.ir.instructions.JumpInstr;
import org.jruby.ir.instructions.ModuleVersionGuardInstr;
import org.jruby.ir.instructions.ToAryInstr;
import org.jruby.ir.instructions.YieldInstr;
import org.jruby.ir.operands.Array;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.ir.representations.CFG;
import org.jruby.ir.representations.CFG.EdgeType;
import org.jruby.ir.representations.ExceptionRegion;
import org.jruby.ir.util.Edge;

public class CFGInliner {
    private CFG cfg;

    public CFGInliner(CFG build) {
        this.cfg = build;
    }

    private CFG cloneSelf(InlinerInfo ii) {
        CFG selfClone = new CFG(cfg.getScope());

        // clone bbs
        BasicBlock entry = cfg.getEntryBB();
        BasicBlock exit = cfg.getExitBB();
        for (BasicBlock b : cfg.getBasicBlocks()) {
            if ((b != entry) && (b != exit)) {
                selfClone.addBasicBlock(b.cloneForInlinedMethod(ii));
            }
        }

        // clone edges
        for (BasicBlock b: cfg.getBasicBlocks()) {
            if ((b != entry) && (b != exit)) {
                BasicBlock rb = ii.getRenamedBB(b);
                for (Edge<BasicBlock> e : cfg.getOutgoingEdges(b)) {
                    BasicBlock destination = e.getDestination().getData();
                    if (destination != exit) selfClone.addEdge(rb, ii.getRenamedBB(destination), e.getType());
                }
            }
        }

        return selfClone;
    }

    public void inlineMethod(IRScope scope, RubyModule implClass, int classToken, BasicBlock callBB, CallBase call) {
        // Temporarily turn off inlining of recursive methods
        // Conservative turning off for inlining of a method in a closure nested within the same method
        IRScope hostScope = cfg.getScope();
        if (hostScope.getNearestMethod() == scope) return;

/*
        System.out.println("host cfg   :" + cfg.toStringGraph());
        System.out.println("host instrs:" + cfg.toStringInstrs());
        System.out.println("source cfg   :" + scope.getCFG().toStringGraph());
        System.out.println("source instrs:" + scope.getCFG().toStringInstrs());
*/

        // Host method data init
        InlinerInfo ii = new InlinerInfo(call, cfg);
        Label splitBBLabel = hostScope.getNewLabel();
        BasicBlock splitBB;

        // Inlinee method data init
        CFG methodCFG = scope.getCFG();
        BasicBlock mEntry = methodCFG.getEntryBB();
        BasicBlock mExit = methodCFG.getExitBB();
        List<BasicBlock> methodBBs = new ArrayList<BasicBlock>();
        for (BasicBlock b: methodCFG.getBasicBlocks()) methodBBs.add(b);

        // Check if we are inlining a recursive method
        if (hostScope.getNearestMethod() == scope) {
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
            // 2. clone callee and add it to the host cfg
            for (BasicBlock b : methodCFG.getBasicBlocks()) {
                if (b != mEntry && b != mExit) {
                    cfg.addBasicBlock(b.cloneForInlinedMethod(ii));
                }
            }
            for (BasicBlock x : methodCFG.getBasicBlocks()) {
                if (x != mEntry && x != mExit) {
                    BasicBlock rx = ii.getRenamedBB(x);
                    for (Edge<BasicBlock> e : methodCFG.getOutgoingEdges(x)) {
                        BasicBlock b = e.getDestination().getData();
                        if (b != mExit) cfg.addEdge(rx, ii.getRenamedBB(b), e.getType());
                    }
                }
            }
        }

        if (callBB == null) {
            for (BasicBlock x: cfg.getBasicBlocks()) {
                for (Instr i: x.getInstrs()) {
                    if (i == call) {
                        callBB = x;
                        break;
                    }
                }
            }
        }

        if (callBB == null) {
            System.out.println("----------------------------------");
            System.out.println("Did not find BB with call: " + call);
            System.out.println("Host cfg   :" + cfg.toStringGraph());
            System.out.println("Host instrs:" + cfg.toStringInstrs());
            System.out.println("----------------------------------");
            return;
        }

        // 3. split callsite bb, move outbound edges from callsite bb to split bb, and unhook call bb
        splitBB = callBB.splitAtInstruction(call, splitBBLabel, false);
        cfg.addBasicBlock(splitBB);
        for (Edge<BasicBlock> e : cfg.getOutgoingEdges(callBB)) {
            cfg.addEdge(splitBB, e.getDestination().getData(), e.getType());
        }
        cfg.removeAllOutgoingEdgesForBB(callBB);

        // 4a. Hook up entry edges
        assert methodCFG.outDegree(mEntry) == 2: "Entry BB of inlinee method does not have outdegree 2: " + methodCFG.toStringGraph();
        for (Edge<BasicBlock> e : methodCFG.getOutgoingEdges(mEntry)) {
            BasicBlock destination = e.getDestination().getData();
            if (destination != mExit) {
                BasicBlock dstBB = ii.getRenamedBB(destination);
                if (!ii.canMapArgsStatically()) {
                    dstBB.addInstr(new ToAryInstr((Variable)ii.getArgs(), new Array(call.getCallArgs()), cfg.getScope().getManager().getTrue()));
                }
                cfg.addEdge(callBB, dstBB, CFG.EdgeType.FALL_THROUGH);
            }
        }

        // 4b. Hook up exit edges
        for (Edge<BasicBlock> e : methodCFG.getIncomingEdges(mExit)) {
            BasicBlock source = e.getSource().getData();
            if (source != mEntry) {
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
        }

        // SSS FIXME: Are these used anywhere post-CFG building?
        // 5. Clone exception regions
        List<ExceptionRegion> exceptionRegions = cfg.getOutermostExceptionRegions();
        for (ExceptionRegion r : methodCFG.getOutermostExceptionRegions()) {
            exceptionRegions.add(r.cloneForInlining(ii));
        }

        // 6. Update bb rescuer map
        // 6a. splitBB will be protected by the same bb as callBB
        BasicBlock callBBrescuer = cfg.getRescuerBBFor(callBB);
        if (callBBrescuer != null) cfg.setRescuerBB(splitBB, callBBrescuer);

        BasicBlock callBBensurer = cfg.getEnsurerBBFor(callBB);
        if (callBBensurer != null) cfg.setEnsurerBB(splitBB, callBBensurer);

        // 6b. remap existing protections for bbs in mcfg to their renamed bbs.
        // 6c. bbs in mcfg that aren't protected by an existing bb will be protected by callBBrescuer.
        for (BasicBlock x : methodBBs) {
            if (x != mEntry && x != mExit) {
                BasicBlock xRenamed = ii.getRenamedBB(x);
                BasicBlock xProtector = methodCFG.getRescuerBBFor(x);
                if (xProtector != null) {
                    cfg.setRescuerBB(xRenamed, ii.getRenamedBB(xProtector));
                } else if (callBBrescuer != null) {
                    cfg.setRescuerBB(xRenamed, callBBrescuer);
                }

                BasicBlock xEnsurer = methodCFG.getEnsurerBBFor(x);
                if (xEnsurer != null) {
                    cfg.setEnsurerBB(xRenamed, ii.getRenamedBB(xEnsurer));
                } else if (callBBensurer != null) {
                    cfg.setEnsurerBB(xRenamed, callBBensurer);
                }
            }
        }

        // 7. Add inline guard that verifies that the method inlined is the same
        // that gets called in future invocations.  In addition to the guard, add
        // a failure path code.
        Label failurePathLabel = hostScope.getNewLabel();
        callBB.addInstr(new ModuleVersionGuardInstr(implClass, classToken, call.getReceiver(), failurePathLabel));

        BasicBlock failurePathBB = new BasicBlock(cfg, failurePathLabel);
        cfg.addBasicBlock(failurePathBB);
        failurePathBB.addInstr(call);
        failurePathBB.addInstr(new JumpInstr(splitBBLabel));
        call.blockInlining();

        cfg.addEdge(callBB, failurePathBB, CFG.EdgeType.REGULAR);
        cfg.addEdge(failurePathBB, splitBB, CFG.EdgeType.REGULAR);

        // 8. Inline any closure argument passed into the call.
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

        // 9. Optimize cfg by merging straight-line bbs
        cfg.collapseStraightLineBBs();
/*
        System.out.println("final cfg   :" + cfg.toStringGraph());
        System.out.println("final instrs:" + cfg.toStringInstrs());
*/
    }

    private void inlineClosureAtYieldSite(InlinerInfo ii, IRClosure cl, BasicBlock yieldBB, YieldInstr yield) {
        // 1. split yield site bb and move outbound edges from yield site bb to split bb.
        BasicBlock splitBB = yieldBB.splitAtInstruction(yield, cfg.getScope().getNewLabel(), false);
        cfg.addBasicBlock(splitBB);
        for (Edge<BasicBlock> e : cfg.getOutgoingEdges(yieldBB)) {
            cfg.addEdge(splitBB, e.getDestination().getData(), e.getType());
        }

        cfg.removeAllOutgoingEdgesForBB(yieldBB);

        // Allocate new inliner object to reset variable and label rename maps
        ii = ii.cloneForInliningClosure();
        ii.setupYieldArgsAndYieldResult(yield, yieldBB, cl.getBlockBody().arity());

        // 2. Merge closure cfg into the current cfg
        CFG closureCFG = cl.getCFG();
        BasicBlock cEntry = closureCFG.getEntryBB();
        BasicBlock cExit = closureCFG.getExitBB();
        for (BasicBlock b : closureCFG.getBasicBlocks()) {
            if (b != cEntry && b != cExit) {
                cfg.addBasicBlock(b.cloneForInlinedClosure(ii));
            }
        }

        for (BasicBlock b : closureCFG.getBasicBlocks()) {
            if (b != cEntry && b != cExit) {
                BasicBlock bClone = ii.getRenamedBB(b);
                for (Edge<BasicBlock> e : closureCFG.getOutgoingEdges(b)) {
                    BasicBlock edst = e.getDestination().getData();
                    if (edst != cExit) cfg.addEdge(bClone, ii.getRenamedBB(edst), e.getType());
                }
            }
        }

        // Hook up entry edges
        for (Edge<BasicBlock> e : closureCFG.getOutgoingEdges(cEntry)) {
            BasicBlock destination = e.getDestination().getData();
            if (destination != cExit) {
                cfg.addEdge(yieldBB, ii.getRenamedBB(destination), CFG.EdgeType.FALL_THROUGH);
            }
        }

        // Hook up exit edges
        for (Edge<BasicBlock> e : closureCFG.getIncomingEdges(cExit)) {
            BasicBlock source = e.getSource().getData();
            if (source != cEntry) {
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
        }

        // SSS FIXME: Are these used anywhere post-CFG building?
        // 5. No need to clone rescued regions -- just assimilate them
        List<ExceptionRegion> exceptionRegions = cfg.getOutermostExceptionRegions();
        for (ExceptionRegion r : closureCFG.getOutermostExceptionRegions()) {
            exceptionRegions.add(r.cloneForInlining(ii));
        }

        // 6. Update bb rescuer map
        // 6a. splitBB will be protected by the same bb as yieldB
        BasicBlock yieldBBrescuer = cfg.getRescuerBBFor(yieldBB);
        if (yieldBBrescuer != null) cfg.setRescuerBB(splitBB, yieldBBrescuer);

        BasicBlock yieldBBensurer = cfg.getEnsurerBBFor(yieldBB);
        if (yieldBBensurer != null) cfg.setEnsurerBB(splitBB, yieldBBensurer);

        // 6b. remap existing protections for bbs in mcfg to their renamed bbs.
        // 6c. bbs in mcfg that aren't protected by an existing bb will be protected by yieldBBrescuer/yieldBBensurer
        for (BasicBlock cb : closureCFG.getBasicBlocks()) {
            if (cb != cEntry && cb != cExit) {
                BasicBlock cbProtector = ii.getRenamedBB(closureCFG.getRescuerBBFor(cb));
                if (cbProtector != null) {
                    cfg.setRescuerBB(cb, cbProtector);
                } else if (yieldBBrescuer != null) {
                    cfg.setRescuerBB(cb, yieldBBrescuer);
                }

                BasicBlock cbEnsurer = ii.getRenamedBB(closureCFG.getEnsurerBBFor(cb));
                if (cbEnsurer != null) {
                    cfg.setEnsurerBB(cb, cbEnsurer);
                } else if (yieldBBensurer != null) {
                    cfg.setEnsurerBB(cb, yieldBBensurer);
                }
            }
        }
    }
}
