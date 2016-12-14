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

    private void property(String name, Object content) {
        startTag("p", "name", name);
        writer.print(content.toString().replace("<", "&lt;"));
        endTag("p");
    }

    private void emptyTag(String name, Object... attributes) {
        writer.print("<" + name + " ");
        for (int i = 0; i < attributes.length; i += 2) {
            writer.print(attributes[i]);
            writer.print("=\"");
            writer.print(attributes[i+1]);
            writer.print("\" ");
        }
        writer.println("/>");
    }


    private void startTag(String name) {
        writer.println("<" + name + ">");
    }

    private void startTag(String name, Object... attributes) {
        writer.print("<" + name + " ");
        for (int i = 0; i < attributes.length; i += 2) {
            writer.print(attributes[i]);
            writer.print("=\"");
            writer.print(attributes[i+1]);
            writer.print("\" ");
        }
        writer.println(">");
    }

    private void endTag(String name) {
        writer.println("</" + name + ">");
    }


    public IGVCFGVisitor(CFG cfg, PrintStream writer, String name) {
        this.writer = writer;

        startTag("group");
        startTag("properties");
        property("name", name);
        endTag("properties");
        CFG(cfg);
        endTag("group");
    }

    protected void visitBasicBlocks(CFG cfg) {
        for (BasicBlock basicBlock: cfg.getBasicBlocks()) {
            BasicBlock(basicBlock);
        }
    }

    protected void visitEdges(CFG cfg) {
        for (BasicBlock basicBlock: cfg.getBasicBlocks()) {
            startTag("block", "name", basicBlock.getLabel());
            startTag("successors");
            for (BasicBlock destination: cfg.getOutgoingDestinations(basicBlock)) {
                emptyTag("successor", "name", destination.getLabel());
            }
            endTag("successors");
            startTag("nodes");
            int index = indexOffsets.get(basicBlock);
            int length = basicBlock.getInstrs().size();
            for (int i = 0; i < length; i++) {
                emptyTag("node", "id", index + i);
            }
            endTag("nodes");
            endTag("block");
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

    public void CFG(CFG cfg) {
        startTag("graph");

        startTag("nodes");
        visitBasicBlocks(cfg);
        endTag("nodes");

        startTag("edges");
        for (Tuple<Integer, Integer> edge: instrEdges) {
            startTag("edge", "from", edge.a, "to", edge.b);
            endTag("edge");
        }

        for (Tuple<Integer, JumpTargetInstr> edge: extraInstrEdges) {
            startTag("edge", "from", edge.a, "to", indexOffsets.get(cfg.getBBForLabel(edge.b.getJumpTarget())));
            endTag("edge");
        }

        endTag("edges");

        startTag("controlFlow");
        visitEdges(cfg);
        endTag("controlFlow");

        endTag("graph");
    }

    public void Instr(Instr instr) {
        int ipc = instrIndex;

        startTag("node", "id", ipc);
        startTag("properties");
        property("label" , ipc);
        property("name", instr);

        // We have not processed all BBs yet so we cannot resolve ipc locations of the jumps destinations.
        if (instr instanceof BranchInstr) extraInstrEdges.add(new Tuple(ipc, (JumpTargetInstr) instr));

        endTag("properties");
        endTag("node");
        instrIndex++;
    }
}
