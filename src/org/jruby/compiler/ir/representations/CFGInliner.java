/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.compiler.ir.representations;

import java.util.List;
import org.jruby.compiler.ir.IRClosure;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.Tuple;
import org.jruby.compiler.ir.instructions.CallInstr;
import org.jruby.compiler.ir.instructions.YieldInstr;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.CFG.EdgeType;
import org.jruby.compiler.ir.util.Edge;

/**
 *
 * @author enebo
 */
public class CFGInliner {
    private CFG cfg;
    
    public CFGInliner(CFG build) {
        this.cfg = build;
    }
    
    public void inlineMethod(IRMethod m, BasicBlock callBB, CallInstr call) {
        // 1. split callsite bb and move outbound edges from callsite bb to split bb.
        InlinerInfo ii = new InlinerInfo(call, cfg);
        BasicBlock splitBB = callBB.splitAtInstruction(call, cfg.getScope().getNewLabel(), false);
        cfg.putBBForLabel(splitBB.getLabel(), splitBB);
        for (Edge<BasicBlock> e : cfg.getOutgoingEdges(callBB)) {
            cfg.addEdge(splitBB, e.getDestination().getData(), e.getType());
        }

        for (Edge edge: cfg.getOutgoingEdges(callBB)) {
            cfg.removeEdge(edge);
        }

        // 2. clone callee
        CFG methodCFG = m.getCFG();
        BasicBlock mEntry = methodCFG.getEntryBB();
        BasicBlock mExit = methodCFG.getExitBB();

        for (BasicBlock b : methodCFG.getBasicBlocks()) {
            if (b != mEntry && b != mExit) {
                BasicBlock bCloned = b.cloneForInlining(ii);
                cfg.putBBForLabel(bCloned.getLabel(), bCloned);
            }
        }

        // 3. set up new edges
        for (BasicBlock x : methodCFG.getBasicBlocks()) {
            if (x != mEntry && x != mExit) {
                BasicBlock rx = ii.getRenamedBB(x);
                for (Edge<BasicBlock> e : methodCFG.getOutgoingEdges(x)) {
                    BasicBlock b = e.getDestination().getData();
                    if (b != mExit) cfg.addEdge(rx, ii.getRenamedBB(b), e.getType());
                }
            }
        }

        // 4. Hook up entry/exit edges
        for (Edge<BasicBlock> e : methodCFG.getOutgoingEdges(mEntry)) {
            BasicBlock destination = e.getDestination().getData();
            if (destination != mExit) {
                cfg.addEdge(callBB, ii.getRenamedBB(destination), e.getType());
            }
        }

        for (Edge<BasicBlock> e : methodCFG.getIncomingEdges(mExit)) {
            BasicBlock source = e.getSource().getData();
            if (source != mEntry) {
                if (e.getType() == EdgeType.EXCEPTION) {
                    // e._src has an explicit throw that returns from the callee
                    // after inlining, if the caller instruction has a rescuer, then the
                    // throw has to be captured by the rescuer as well.
                    BasicBlock rescuerOfSplitBB = cfg.getRescuerBBFor(splitBB);
                    if (rescuerOfSplitBB != null) {
                        cfg.addEdge(ii.getRenamedBB(source), rescuerOfSplitBB, EdgeType.EXCEPTION);
                    } else {
                        cfg.addEdge(ii.getRenamedBB(source), cfg.getExitBB(), EdgeType.EXIT);                        
                    }
                } else {
                    cfg.addEdge(ii.getRenamedBB(source), splitBB, e.getType());
                }
            }
        }

        List<ExceptionRegion> exceptionRegions = cfg.getOutermostExceptionRegions();
        // 5. Clone exception regions
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
        for (BasicBlock x : methodCFG.getBasicBlocks()) {
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

        // 7. callBB will only have a single successor & splitBB will only have a single predecessor
        //    after inlining the callee.  Merge them with their successor/predecessors respectively
        //    Merge only after fixing up the rescuer map above
        mergeStraightlineBBs(callBB, splitBB);

        // 8. Inline any closure argument passed into the call.
        Operand closureArg = call.getClosureArg();
        List yieldSites = ii.getYieldSites();
        if (closureArg != null && !yieldSites.isEmpty()) {
            // Detect unlikely but contrived scenarios where there are far too many yield sites that could lead to code blowup
            // if we inline the closure at all those yield sites!
            if (yieldSites.size() > 1) {
                throw new RuntimeException("Encountered " + yieldSites.size() + " yield sites.  Convert the yield to a call by converting the closure into a dummy method (have to convert all frame vars to call arguments, or at least convert the frame into a call arg");
            }

            if (!(closureArg instanceof MetaObject)) {
                throw new RuntimeException("Encountered a dynamic closure arg.  Cannot inline it here!  Convert the yield to a call by converting the closure into a dummy method (have to convert all frame vars to call arguments, or at least convert the frame into a call arg");
            }

            Tuple t = (Tuple) yieldSites.get(0);
            inlineClosureAtYieldSite(ii, (IRClosure) ((MetaObject) closureArg).scope, (BasicBlock) t.a, (YieldInstr) t.b);
        }

        // Update the bb array
        // ENEBO: Currently unused
        //cfg.setupFallThruMap();
    }
    
   private void inlineClosureAtYieldSite(InlinerInfo ii, IRClosure cl, BasicBlock yieldBB, YieldInstr yield) {
        // Mark this closure as inlined so we dont run any destructive operations on it.
        // since the closure in its original form will get destroyed by the inlining.
        cl.markInlined();

        // 1. split yield site bb and move outbound edges from yield site bb to split bb.
        BasicBlock splitBB = yieldBB.splitAtInstruction(yield, cfg.getScope().getNewLabel(), false);

        cfg.putBBForLabel(splitBB.getLabel(), splitBB);
        List<Edge<BasicBlock>> edgesToRemove = new java.util.ArrayList<Edge<BasicBlock>>();
        for (Edge<BasicBlock> e : cfg.getOutgoingEdges(yieldBB)) {
            cfg.addEdge(splitBB, e.getDestination().getData(), e.getType());
            edgesToRemove.add(e);
        }
        // Ugh! I get exceptions if I try to pass the set I receive from outgoingEdgesOf!  What a waste!
        for (Edge e : edgesToRemove) {
            cfg.removeEdge(e);
        }

        // 2. Merge closure cfg into the current cfg
        // NOTE: No need to clone basic blocks in the closure because they are part of the caller's cfg
        // and is being merged in at the yield site -- there is no need for the closure after the merge.
        CFG closureCFG = cl.getCFG();
        BasicBlock cEntry = closureCFG.getEntryBB();
        BasicBlock cExit = closureCFG.getExitBB();
        for (BasicBlock b : closureCFG.getBasicBlocks()) {
            if (b != cEntry && b != cExit) {
                cfg.putBBForLabel(b.getLabel(), b);
                b.updateCFG(cfg);
                b.processClosureArgAndReturnInstrs(ii, yield);
            }
        }
        for (BasicBlock b : closureCFG.getBasicBlocks()) {
            if (b != cEntry && b != cExit) {
                for (Edge<BasicBlock> e : closureCFG.getOutgoingEdges(b)) {
                    BasicBlock c = e.getDestination().getData();
                    if (c != cExit) cfg.addEdge(b, c, e.getType());
                }
            }
        }
        
        for (Edge<BasicBlock> e : cfg.getOutgoingEdges(cEntry)) {
            BasicBlock destination = e.getDestination().getData();
            if (destination != cExit) cfg.addEdge(yieldBB, destination, e.getType());
        }
        
        for (Edge<BasicBlock> e : cfg.getIncomingEdges(cExit)) {
            BasicBlock source = e.getSource().getData();
            if (source != cEntry) {
                if (e.getType() == EdgeType.EXCEPTION) {
                    // e._src has an explicit throw that returns from the closure
                    // after inlining, if the yield instruction has a rescuer, then the
                    // throw has to be captured by the rescuer as well.
                    BasicBlock rescuerOfSplitBB = cfg.getRescuerBBFor(splitBB);
                    if (rescuerOfSplitBB != null) {
                        cfg.addEdge(source, rescuerOfSplitBB, EdgeType.EXCEPTION);
                    } else {
                        cfg.addEdge(source, cfg.getExitBB(), EdgeType.EXIT);
                    }

                } else {
                    cfg.addEdge(source, splitBB, e.getType());
                }
            }
        }

        // 5. No need to clone rescued regions -- just assimilate them
        List<ExceptionRegion> exceptionRegions = cfg.getOutermostExceptionRegions();
        for (ExceptionRegion r : closureCFG.getOutermostExceptionRegions()) {
            exceptionRegions.add(r);
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
                BasicBlock cbProtector = closureCFG.getRescuerBBFor(cb);
                if (cbProtector != null) {
                    cfg.setRescuerBB(cb, cbProtector);
                } else if (yieldBBrescuer != null) {
                    cfg.setRescuerBB(cb, yieldBBrescuer);
                }

                BasicBlock cbEnsurer = closureCFG.getEnsurerBBFor(cb);
                if (cbEnsurer != null) {
                    cfg.setEnsurerBB(cb, cbEnsurer);
                } else if (yieldBBensurer != null) {
                    cfg.setEnsurerBB(cb, yieldBBensurer);
                }
            }
        }

        // 7. callBB will only have a single successor & splitBB will only have a single predecessor
        //    after inlining the callee.  Merge them with their successor/predecessors respectively
        //    Merge only after fixing up the rescuer map above
        mergeStraightlineBBs(yieldBB, splitBB);
    }
    
    
    private void mergeBBs(BasicBlock a, BasicBlock b) {
        BasicBlock aR = cfg.getRescuerBBFor(a);
        BasicBlock bR = cfg.getRescuerBBFor(b);
        // We can merge 'a' and 'b' if one of the following is true:
        // 1. 'a' and 'b' are both not empty
        //    They are protected by the same rescue block.
        //    NOTE: We need not check the ensure block map because all ensure blocks are already
        //    captured in the bb rescue block map.  So, if aR == bR, it is guaranteed that the
        //    ensure blocks for the two are identical.
        // 2. One of 'a' or 'b' is empty.  We dont need to check for rescue block match because
        //    an empty basic block cannot raise an exception, can it.
        if ((aR == bR) || a.isEmpty() || b.isEmpty()) {
            a.swallowBB(b);
            cfg.removeEdge(a, b);
            for (Edge<BasicBlock> e : cfg.getOutgoingEdges(b)) {
                cfg.addEdge(a, e.getDestination().getData(), e.getType());
            }

            cfg.removeBB(b);

            // Update rescue and ensure maps
            if ((aR == null) && (bR != null)) {
                cfg.setRescuerBB(a, bR);
                BasicBlock aE = cfg.getEnsurerBBFor(a);
                BasicBlock bE = cfg.getEnsurerBBFor(b);
                if ((aE == null) && (bE != null)) cfg.setRescuerBB(a, bE);
            }
        }
    }    
    
    // callBB will only have a single successor & splitBB will only have a single predecessor
    // after inlining the callee.  Merge them with their successor/predecessors respectively
    private void mergeStraightlineBBs(BasicBlock callBB, BasicBlock splitBB) {
        mergeBBs(callBB, cfg.getOutgoingDestination(callBB));
        mergeBBs(cfg.getIncomingSource(splitBB), splitBB);
    }    
}
