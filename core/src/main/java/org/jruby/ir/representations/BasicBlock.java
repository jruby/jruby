package org.jruby.ir.representations;

import java.util.ArrayList;
import java.util.List;
import org.jruby.RubyInstanceConfig;
import org.jruby.ir.IRManager;
import org.jruby.ir.IRScope;
import org.jruby.ir.instructions.CallBase;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.YieldInstr;
import org.jruby.ir.listeners.InstructionsListener;
import org.jruby.ir.listeners.InstructionsListenerDecorator;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.ir.transformations.inlining.CloneMode;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.ir.util.ExplicitVertexID;

public class BasicBlock implements ExplicitVertexID, Comparable {
    private int         id;             // Basic Block id
    private CFG         cfg;            // CFG that this basic block belongs to
    private Label       label;          // All basic blocks have a starting label
    private List<Instr> instrs;         // List of non-label instructions
    private boolean     isRescueEntry;  // Is this basic block entry of a rescue?
    private Instr[]     instrsArray;

    public BasicBlock(CFG c, Label l) {
        label         = l;
        cfg           = c;
        id            = c.getNextBBID();
        isRescueEntry = false;
        initInstrs();
    }

    private void initInstrs() {
        instrs = new ArrayList<Instr>();
        if (RubyInstanceConfig.IR_COMPILER_DEBUG || RubyInstanceConfig.IR_VISUALIZER) {
            IRManager irManager = cfg.getScope().getManager();
            InstructionsListener listener = irManager.getInstructionsListener();
            if (listener != null) {
                instrs = new InstructionsListenerDecorator(instrs, listener);
            }
        }
        instrsArray = null;
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
        this.instrsArray = null;
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

    public int instrCount() {
        return instrs.size();
    }

    public Instr[] getInstrsArray() {
        if (instrsArray == null) instrsArray = instrs.toArray(new Instr[instrs.size()]);

        return instrsArray;
    }

    public Instr getLastInstr() {
        int n = instrs.size();
        return (n == 0) ? null : instrs.get(n-1);
    }

    public boolean removeInstr(Instr i) {
       return i == null? false : instrs.remove(i);
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

    public void cloneInstrs(InlinerInfo ii) {
        if (!isEmpty()) {
            List<Instr> oldInstrs = instrs;
            initInstrs();

            for (Instr i: oldInstrs) {
                Instr clonedInstr = i.cloneForInlining(ii);
                clonedInstr.setIPC(i.getIPC());
                if (clonedInstr instanceof CallBase) {
                    CallBase call = (CallBase)clonedInstr;
                    Operand block = call.getClosureArg(null);
                    if (block instanceof WrappedIRClosure) cfg.getScope().addClosure(((WrappedIRClosure)block).getClosure());
                }
                instrs.add(clonedInstr);
            }
        }

        // Rename the label as well!
        this.label = ii.getRenamedLabel(this.label);
    }

    public BasicBlock cloneForInlining(InlinerInfo ii) {
        IRScope hostScope = ii.getInlineHostScope();
        BasicBlock clonedBB = ii.getOrCreateRenamedBB(this);

        for (Instr i: getInstrs()) {
            Instr clonedInstr = i.cloneForInlining(ii);
            if (clonedInstr != null) {
                clonedBB.addInstr(clonedInstr);
                if (clonedInstr instanceof YieldInstr && ii.getCloneMode() != CloneMode.NORMAL_CLONE) {
                    ii.recordYieldSite(clonedBB, (YieldInstr)clonedInstr);
                }
                if (clonedInstr instanceof CallBase) {
                    CallBase call = (CallBase)clonedInstr;
                    Operand block = call.getClosureArg(null);
                    if (block instanceof WrappedIRClosure) hostScope.addClosure(((WrappedIRClosure)block).getClosure());
                }
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
