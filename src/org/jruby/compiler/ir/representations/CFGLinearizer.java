package org.jruby.compiler.ir.representations;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.JumpInstr;
import org.jruby.compiler.ir.instructions.ReturnInstr;
import org.jruby.compiler.ir.operands.Nil;
import org.jruby.compiler.ir.representations.CFGData.EdgeType;

/**
 * This produces a linear list of BasicBlocks in the same approximate order
 * as the flow of the ruby program.  In processing this list, we will also
 * add jumps where required and remove as many jumps as possible.
 * 
 * Ordinary BasicBlocks will follow FollowThrough edges and just concatenate 
 * together eliminating the need for executing a jump instruction during 
 * execution.
 * 
 * Notes:
 * 1. Branches have two edges (FollowTrough/NotTaken and Taken)
 * 2. All BasicBlocks can possibly have a third edge for exception
 * 3. Branch, Jump, Return, and Exceptions are all boundaries for BasicBlocks
 * 4. Dummy Entry and Exit BasicBlocks exist in all CFGs
 * 
 */
public class CFGLinearizer {
    public static List<BasicBlock> linearize(CFG cfg) {
        List<BasicBlock> list = new ArrayList<BasicBlock>();
        BitSet processed = new BitSet(cfg.size()); // Assumes all id's are used
        
        linearizeInner(cfg, list, processed, cfg.getEntryBB());
        verifyAllBasicBlocksProcessed(cfg, processed);
        fixupList(cfg, list);
        
        return list;
    }

    // If there is no jump at add of block and the next block is not destination insert a valid jump
    private static void addJumpIfNextNotDestination(CFG cfg, BasicBlock next, Instr lastInstr, BasicBlock current) {
        Iterator<BasicBlock> outs = cfg.getOutgoingDestinations(current).iterator();
        BasicBlock target = outs.hasNext() ? outs.next() : null;
        
        if (target != null && !outs.hasNext()) {
            if ((target != next) && ((lastInstr == null) || !lastInstr.getOperation().transfersControl())) {
                current.addInstr(new JumpInstr(target.getLabel()));
            }
        }
    }
    
    private static void linearizeInner(CFG cfg, List<BasicBlock> list, 
            BitSet processed, BasicBlock current) {
        if (processed.get(current.getID())) return;

        // Cannot lay out current block till its fall-through predecessor has been laid out already
        BasicBlock source = cfg.getIncomingSourceOfType(current, EdgeType.FALL_THROUGH);
        if (source != null && !processed.get(source.getID())) return;

        list.add(current);
        processed.set(current.getID());
        
        BasicBlock fallThrough = cfg.getOutgoingDestinationOfType(current, EdgeType.FALL_THROUGH);
        if (fallThrough != null) linearizeInner(cfg, list, processed, fallThrough);
        
        for (BasicBlock destination: cfg.getOutgoingDestinationsNotOfType(current, EdgeType.FALL_THROUGH)) {
            linearizeInner(cfg, list, processed, destination);
        }
    }
    
    /**
     * Process (fixup) list of instruction and add or remove jumps.
     */
    private static void fixupList(CFG cfg, List<BasicBlock> list) {
        BasicBlock exitBB = cfg.getExitBB();
        int n = list.size();
        for (int i = 0; i < n - 1; i++) {
            BasicBlock current = list.get(i);

            if (current == exitBB) { // exit not last
                current.addInstr(new ReturnInstr(Nil.NIL));
                continue;
            }
                  
            Instr lastInstr = current.getLastInstr();
            if (lastInstr instanceof JumpInstr) { // if jumping to next BB then remove it
                tryAndRemoveUnneededJump(list.get(i + 1), cfg, lastInstr, current);
            } else {
                addJumpIfNextNotDestination(cfg, list.get(i + 1), lastInstr, current);
            }
        }
        
        BasicBlock current = list.get(n - 1);
        if (current != exitBB) {
            Iterator<BasicBlock> iter = cfg.getOutgoingDestinationsNotOfType(current, EdgeType.EXCEPTION).iterator();
            BasicBlock target = iter.next();

            // ENEBO: Unsure this ever happens...review this case with subbu
            if (target != exitBB && iter.hasNext()) {
                BasicBlock target2 = iter.next();
                if (target2 == exitBB) target = target2;
            }

            Instr lastInstr = current.getLastInstr();
            if ((lastInstr == null) || !lastInstr.getOperation().transfersControl()) {
                //                    System.out.println("BB " + curr.getID() + " is the last bb in the layout! Adding a jump to " + tgt._label);
                current.addInstr(new JumpInstr(target.getLabel()));
            }
        }
    }

    private static void tryAndRemoveUnneededJump(BasicBlock next, CFG cfg, Instr lastInstr, BasicBlock current) {
        if (next == cfg.getBBForLabel(((JumpInstr) lastInstr).getJumpTarget())) current.removeInstr(lastInstr);
    }

    private static void verifyAllBasicBlocksProcessed(CFG cfg, BitSet processed) throws RuntimeException {
        // Verify that all bbs have been laid out!
        for (BasicBlock b : cfg.getBasicBlocks()) {
            if (!processed.get(b.getID())) {
                throw new RuntimeException("Bad CFG linearization: BB " + b.getID() + " has been missed!");
            }
        }
    }
}
