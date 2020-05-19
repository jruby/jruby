package org.jruby.ir.representations;

import org.jruby.ir.instructions.BranchInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.JumpInstr;
import org.jruby.ir.instructions.ReturnInstr;

import java.util.BitSet;
import java.util.Iterator;

import static org.jruby.ir.representations.CFG.EdgeType.*;

/**
 * This produces a linear list of BasicBlocks so that the linearized instruction
 * list is in executable form.  In generating this list, we will also add jumps
 * where required and remove as many jumps as possible.
 *
 * Ordinary BasicBlocks will follow FollowThrough edges and just concatenate
 * together eliminating the need for executing a jump instruction during
 * execution.
 *
 * Notes:
 * 1. Basic blocks ending in branches have two edges (FollowTrough/NotTaken and Taken)
 * 2. All BasicBlocks can possibly have two additional edges related to exceptions:
 *    - one that transfers control to a rescue block (if one exists that protects
 *      the excepting instruction) which is also responsible for running ensures
 *    - one that transfers control to an ensure block (if one exists) for
 *      situations where we bypass the rescue block (breaks and thread-kill).
 * 3. Branch, Jump, Return, and Exceptions are all boundaries for BasicBlocks
 * 4. Dummy Entry and Exit BasicBlocks exist in all CFGs
 *
 * NOTE: When the IR builder first builds its list, and the CFG builder builds the CFG,
 * the order in which BBs are created should already be a linearized list.  Need to verify
 * this and we might be able to skip linearization if the CFG has not been transformed
 * by any code transformation passes.  This might be the case when JRuby first starts up
 * when we may just build the IR and start interpreting it right away without running any
 * opts.  In that scenario, it may be worth it to not run the linearizer at all.
 */
public class CFGLinearizer {
    public static BasicBlock[] linearize(CFG cfg) {
        BasicBlock[] list = new BasicBlock[cfg.size()];
        BitSet processed = new BitSet(cfg.size());

        int listSize = linearizeInner(cfg, list, 0, processed, cfg.getEntryBB());
        verifyAllBasicBlocksProcessed(cfg, processed);
        fixupList(cfg, list, listSize);

        return list;
    }

    private static int linearizeInner(CFG cfg, BasicBlock[] list, int listSize,
            BitSet processed, BasicBlock current) {
        if (processed.get(current.getID())) return listSize;

        // Cannot lay out current block till its fall-through predecessor has been laid out already
        BasicBlock source = cfg.getIncomingSourceOfType(current, FALL_THROUGH);
        if (source != null && !processed.get(source.getID())) return listSize;

        list[listSize] = current;
        listSize++;
        processed.set(current.getID());

        // First, fall-through BB
        BasicBlock fallThrough = cfg.getOutgoingDestinationOfType(current, FALL_THROUGH);
        if (fallThrough != null) listSize = linearizeInner(cfg, list, listSize, processed, fallThrough);

        // Next, regular edges
        for (BasicBlock destination: cfg.getOutgoingDestinationsOfType(current, REGULAR)) {
            listSize = linearizeInner(cfg, list, listSize, processed, destination);
        }

        // Next, exception edges
        for (BasicBlock destination: cfg.getOutgoingDestinationsOfType(current, EXCEPTION)) {
            listSize = linearizeInner(cfg, list, listSize, processed, destination);
        }

        // Next, exit
        for (BasicBlock destination: cfg.getOutgoingDestinationsOfType(current, EXIT)) {
            listSize = linearizeInner(cfg, list, listSize, processed, destination);
        }

        return listSize;
    }

    /**
     * Process (fixup) list of instruction and add or remove jumps.
     */
    private static void fixupList(CFG cfg, BasicBlock[] list, int listSize) {
        for (int i = 0; i < listSize - 1; i++) {
            BasicBlock current = list[i];

            if (current.isExitBB()) { // exit not last
                current.addInstr(new ReturnInstr(cfg.getManager().getNil()));
                continue;
            }

            Instr lastInstr = current.getLastInstr();
            if (lastInstr instanceof JumpInstr) { // if jumping to next BB then remove it
                tryAndRemoveUnneededJump(list[i + 1], cfg, lastInstr, current);
            } else {
                addJumpIfNextNotDestination(cfg, list[i + 1], lastInstr, current);
            }
        }

        BasicBlock current = list[listSize - 1];
        if (!current.isExitBB()) {
            Instr lastInstr = current.getLastInstr();
            // Last instruction of the last basic block in the linearized list can NEVER
            // be a branch instruction because this basic block would then have a fallthrough
            // which would have to be present after it.
            assert (!(lastInstr instanceof BranchInstr));

            if ((lastInstr == null) || !lastInstr.getOperation().transfersControl()) {
                // We are guaranteed to have at least one non-exception edge because
                // the exit BB post-dominates all BBs in the CFG even when exception
                // edges are removed.
                //
                // Verify that we have exactly one non-exception target
                // SSS FIXME: Is this assertion any different from the BranchInstr assertion above?
                Iterator<BasicBlock> iter = cfg.getOutgoingDestinationsNotOfType(current, EXCEPTION).iterator();
                BasicBlock target = iter.next();
                assert (target != null && !iter.hasNext());

                // System.out.println("BB " + curr.getID() + " is the last bb in the layout! Adding a jump to " + tgt._label);
                current.addInstr(new JumpInstr(target.getLabel()));
            }
        }
    }

    private static void tryAndRemoveUnneededJump(BasicBlock next, CFG cfg, Instr lastInstr, BasicBlock current) {
        if (next == cfg.getBBForLabel(((JumpInstr) lastInstr).getJumpTarget())) current.removeInstr(lastInstr);
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

    private static void verifyAllBasicBlocksProcessed(CFG cfg, BitSet processed) throws RuntimeException {
        // Verify that all bbs have been laid out!
        for (BasicBlock b : cfg.getBasicBlocks()) {
            if (!processed.get(b.getID())) {
                throw new RuntimeException("Bad CFG linearization: BB " + b.getID() + " has been missed!");
            }
        }
    }
}
