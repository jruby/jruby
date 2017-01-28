package org.jruby.ir.representations;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jruby.ir.Tuple;
import org.jruby.ir.instructions.BranchInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.JumpInstr;
import org.jruby.ir.instructions.JumpTargetInstr;

import static org.jruby.ir.util.IGVHelper.emptyTag;
import static org.jruby.ir.util.IGVHelper.endTag;
import static org.jruby.ir.util.IGVHelper.property;
import static org.jruby.ir.util.IGVHelper.startTag;

/**
 * Not a double visitor but I did not want both instr and dirgra
 * to have this visitor have an accept added to it. dirgra is
 * external package and instr I can foresee wanting a different
 * visitor altogether not accessed via CFG.
 */
public class IGVCFGVisitor {
    PrintStream writer;
    Map<BasicBlock, Integer> indexOffsets = new HashMap();
    List<Tuple<Integer, Integer>> instrEdges = new ArrayList();
    List<Tuple<Integer, JumpTargetInstr>> extraInstrEdges = new ArrayList();
    int instrIndex = 0;


    public IGVCFGVisitor(CFG cfg, PrintStream writer, String name) {
        this.writer = writer;

        CFG(cfg, name);
    }

    protected void visitBasicBlocks(CFG cfg) {
        for (BasicBlock basicBlock: cfg.getBasicBlocks()) {
            BasicBlock(basicBlock);
        }
    }

    protected void visitEdges(CFG cfg) {
        for (BasicBlock basicBlock: cfg.getBasicBlocks()) {
            startTag(writer, "block", "name", basicBlock.getLabel());
            startTag(writer, "successors");
            for (BasicBlock destination: cfg.getOutgoingDestinations(basicBlock)) {
                emptyTag(writer, "successor", "name", destination.getLabel());
            }
            endTag(writer, "successors");
            startTag(writer, "nodes");
            int index = indexOffsets.get(basicBlock);
            int length = basicBlock.getInstrs().size();
            for (int i = 0; i < length; i++) {
                emptyTag(writer, "node", "id", index + i);
            }
            endTag(writer, "nodes");
            endTag(writer, "block");
        }
    }

    protected void visitInstrs(BasicBlock basicBlock) {
        List<Instr> instrs = basicBlock.getInstrs();
        int size = instrs.size();

        if (size > 0) {
            for (int i = 0; i < size - 1; i++) {
                Instr(instrs.get(i));
                instrEdges.add(new Tuple(instrIndex - 1, instrIndex));
            }

            Instr lastInstr = instrs.get(size - 1);
            Instr(lastInstr);
            // jumps do not fall to next BB but all other instrs will
            if (!(lastInstr instanceof JumpInstr)) instrEdges.add(new Tuple(instrIndex - 1, instrIndex));
        }
    }

    public void BasicBlock(BasicBlock basicBlock) {
        indexOffsets.put(basicBlock, instrIndex);
        visitInstrs(basicBlock);
    }

    public void CFG(CFG cfg, String name) {
        startTag(writer, "graph");
        startTag(writer, "properties");
        property(writer, "name", name);
        endTag(writer, "properties");

        startTag(writer, "nodes");
        visitBasicBlocks(cfg);
        endTag(writer, "nodes");

        startTag(writer, "edges");
        for (Tuple<Integer, Integer> edge: instrEdges) {
            startTag(writer, "edge", "from", edge.a, "to", edge.b);
            endTag(writer, "edge");
        }

        for (Tuple<Integer, JumpTargetInstr> edge: extraInstrEdges) {
            startTag(writer, "edge", "from", edge.a, "to", indexOffsets.get(cfg.getBBForLabel(edge.b.getJumpTarget())));
            endTag(writer, "edge");
        }

        endTag(writer, "edges");

        startTag(writer, "controlFlow");
        visitEdges(cfg);
        endTag(writer, "controlFlow");

        endTag(writer, "graph");
    }

    public void Instr(Instr instr) {
        int ipc = instrIndex;

        startTag(writer, "node", "id", ipc);
        startTag(writer, "properties");
        property(writer, "label" , ipc);
        property(writer, "name", instr);

        // We have not processed all BBs yet so we cannot resolve ipc locations of the jumps destinations.
        if (instr instanceof BranchInstr) extraInstrEdges.add(new Tuple(ipc, (JumpTargetInstr) instr));

        endTag(writer, "properties");
        endTag(writer, "node");
        instrIndex++;
    }
}
