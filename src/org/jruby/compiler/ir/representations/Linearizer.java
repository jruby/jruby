package org.jruby.compiler.ir.representations;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.JumpInstr;
import org.jruby.compiler.ir.instructions.ReturnInstr;
import org.jruby.compiler.ir.operands.Nil;
import org.jruby.compiler.ir.representations.CFG.EdgeType;
import org.jruby.compiler.ir.util.DirectedGraph;
import org.jruby.compiler.ir.util.Edge;
import org.jruby.compiler.ir.util.Vertex;

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
public class Linearizer {
    public static List<BasicBlock> linearize(CFG cfg) {
        List<BasicBlock> list = new ArrayList<BasicBlock>();
        DirectedGraph<BasicBlock> graph = cfg.getGraph();
        BitSet processed = new BitSet(1 + cfg.getMaxNodeID());
        
        linearizeInner(graph, list, processed, cfg.getEntryBB());
        verifyAllBasicBlocksProcessed(graph, processed);
        fixupList(cfg, graph, list);
        
        return list;
    }

    // If there is no jump at add of block and the next block is not destination insert a valid jump
    private static void addJumpIfNextNotDestination(DirectedGraph<BasicBlock> graph, BasicBlock next, Instr lastInstr, BasicBlock current) {
        Set<Edge<BasicBlock>> succs = graph.vertexFor(current).getOutgoingEdges();
        
        if (succs.size() == 1) {
            BasicBlock target = succs.iterator().next().getDestination().getData();                        

            if ((target != next) && ((lastInstr == null) || !lastInstr.getOperation().transfersControl())) {
                current.addInstr(new JumpInstr(target.getLabel()));
            }
        }
    }
    
    private static void linearizeInner(DirectedGraph<BasicBlock> graph, List<BasicBlock> list, 
            BitSet processed, BasicBlock current) {
        if (processed.get(current.getID())) return;

        list.add(current);
        processed.set(current.getID());
        
        Vertex<BasicBlock> vertex = graph.vertexFor(current);
        Edge<BasicBlock> fallThrough = vertex.getOutgoingEdgeOfType(EdgeType.FALL_THROUGH);
        
        if (fallThrough != null) {
            linearizeInner(graph, list, processed, fallThrough.getDestination().getData());
        }
        
        for (Edge<BasicBlock> edge: vertex.getOutgoingEdgesNotOfType(EdgeType.FALL_THROUGH)) {
            linearizeInner(graph, list, processed, edge.getDestination().getData());
        }
    }
    
    /**
     * Process (fixup) list of instruction and add or remove jumps.
     */
    private static void fixupList(CFG cfg, DirectedGraph<BasicBlock> graph, List<BasicBlock> list) {
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
                addJumpIfNextNotDestination(graph, list.get(i + 1), lastInstr, current);
            }
        }
        
        BasicBlock current = list.get(n - 1);
        if (current != exitBB) {
            Iterator<Edge<BasicBlock>> edges = graph.vertexFor(current).getOutgoingEdges().iterator();
            BasicBlock target = edges.next().getDestination().getData();

            // ENEBO: Unsure this ever happens...review this case with subbu
            if (target != exitBB && edges.hasNext()) {
                BasicBlock target2 = edges.next().getDestination().getData();
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
        if (next == cfg.getBBMap().get(((JumpInstr) lastInstr).getJumpTarget())) current.removeInstr(lastInstr);
    }

    private static void verifyAllBasicBlocksProcessed(DirectedGraph<BasicBlock> graph, 
            BitSet processed) throws RuntimeException {
        // Verify that all bbs have been laid out!
        for (BasicBlock b : graph.allData()) {
            if (!processed.get(b.getID())) {
                throw new RuntimeException("Bad CFG linearization: BB " + b.getID() + " has been missed!");
            }
        }
    }
}
