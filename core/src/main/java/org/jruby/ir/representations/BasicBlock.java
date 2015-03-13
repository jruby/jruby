package org.jruby.ir.representations;

import org.jruby.RubyInstanceConfig;
import org.jruby.ir.IRManager;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.YieldInstr;
import org.jruby.ir.listeners.InstructionsListener;
import org.jruby.ir.listeners.InstructionsListenerDecorator;
import org.jruby.ir.operands.Label;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.InlineCloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;
import org.jruby.ir.util.ExplicitVertexID;

import java.util.ArrayList;
import java.util.List;

public class BasicBlock implements ExplicitVertexID, Comparable {
    private int         id;             // Basic Block id
    private CFG         cfg;            // CFG that this basic block belongs to
    private Label       label;          // All basic blocks have a starting label
    private List<Instr> instrs;         // List of non-label instructions
    private boolean     isRescueEntry;  // Is this basic block entry of a rescue?

    public BasicBlock(CFG cfg, Label label) {
        this.label = label;
        this.cfg = cfg;
        id = cfg.getNextBBID();
        isRescueEntry = false;

        assert label != null : "label is null";

        initInstrs();
    }

    private void initInstrs() {
        instrs = new ArrayList<>();
        if (RubyInstanceConfig.IR_COMPILER_DEBUG || RubyInstanceConfig.IR_VISUALIZER) {
            IRManager irManager = cfg.getManager();
            InstructionsListener listener = irManager.getInstructionsListener();
            if (listener != null) {
                instrs = new InstructionsListenerDecorator(instrs, listener);
            }
        }
    }

    @Override
    public int getID() {
        return id;
    }

    public Label getLabel() {
        return label;
    }

    @Override
    public int hashCode() {
        return id;
    }

    public boolean isEntryBB() {
        return cfg.getEntryBB() == this;
    }

    public boolean isExitBB() {
        return cfg.getExitBB() == this;
    }

    public void markRescueEntryBB() {
        this.isRescueEntry = true;
    }

    public boolean isRescueEntry() {
        return this.isRescueEntry;
    }

    public void replaceInstrs(List<Instr> instrs) {
        this.instrs = instrs;
    }

    public void addInstr(Instr i) {
        instrs.add(i);
    }

    public void insertInstr(Instr i) {
        instrs.add(0, i);
    }

    public List<Instr> getInstrs() {
        return instrs;
    }

    public Instr getLastInstr() {
        int n = instrs.size();
        return (n == 0) ? null : instrs.get(n-1);
    }

    public boolean removeInstr(Instr i) {
       return i != null && instrs.remove(i);
    }

    public boolean isEmpty() {
        return instrs.isEmpty();
    }

    public BasicBlock splitAtInstruction(Instr splitPoint, Label newLabel, boolean includeSplitPointInstr) {
        BasicBlock newBB = new BasicBlock(cfg, newLabel);
        int idx = 0;
        int numInstrs = instrs.size();
        boolean found = false;
        for (Instr i: instrs) {
            if (i.getIPC() == splitPoint.getIPC()) found = true;

            // Move instructions from split point into the new bb
            if (found) {
                if (includeSplitPointInstr || i.getIPC() != splitPoint.getIPC()) newBB.addInstr(i);
            } else {
                idx++;
            }
        }

        // Remove all instructions from current bb that were moved over.
        for (int j = 0; j < numInstrs-idx; j++) {
            instrs.remove(idx);
        }

        return newBB;
    }

    public void swallowBB(BasicBlock foodBB) {
        // Gulp!
        this.instrs.addAll(foodBB.instrs);
    }

    // FIXME: Untested in inliner (and we need to replace cloneInstrs(InlineCloneInfo) with this).
    public BasicBlock clone(CloneInfo info, CFG newCFG) {
        BasicBlock newBB = new BasicBlock(newCFG, info.getRenamedLabel(label));
        boolean isClosureClone = info instanceof InlineCloneInfo && ((InlineCloneInfo) info).isClosure();

        for (Instr instr: instrs) {
            Instr newInstr = instr.clone(info);

            if (newInstr != null) {  // inliner may kill off unneeded instr
                newBB.addInstr(newInstr);
                if (isClosureClone && newInstr instanceof YieldInstr) {
                    ((InlineCloneInfo) info).recordYieldSite(newBB, (YieldInstr) newInstr);
                }
            }
        }

        return newBB;
    }

    public void cloneInstrs(SimpleCloneInfo ii) {
        if (!isEmpty()) {
            List<Instr> oldInstrs = instrs;
            initInstrs();

            for (Instr i: oldInstrs) {
                Instr clonedInstr = i.clone(ii);
                clonedInstr.setIPC(i.getIPC());
                clonedInstr.setRPC(i.getRPC());
                instrs.add(clonedInstr);
            }
        }

        // Rename the label as well!
        this.label = ii.getRenamedLabel(this.label);
    }

    public BasicBlock cloneForInlining(InlineCloneInfo ii) {
        BasicBlock clonedBB = ii.getOrCreateRenamedBB(this);

        for (Instr i: getInstrs()) {
            Instr clonedInstr = i.clone(ii);
            if (clonedInstr != null) {
                clonedBB.addInstr(clonedInstr);
                if (clonedInstr instanceof YieldInstr) ii.recordYieldSite(clonedBB, (YieldInstr)clonedInstr);
            }
        }

        return clonedBB;
    }

    @Override
    public int compareTo(Object o) {
        BasicBlock other = (BasicBlock) o;

        if (id == other.id) return 0;
        if (id < other.id) return -1;

        return 1;
    }

    @Override
    public String toString() {
        return "BB [" + id + ":" + label + "]";
    }

    public String toStringInstrs() {
        StringBuilder buf = new StringBuilder(toString() + "\n");

        for (Instr instr : getInstrs()) {
            buf.append('\t').append(instr).append('\n');
        }

        return buf.toString();
    }
}
