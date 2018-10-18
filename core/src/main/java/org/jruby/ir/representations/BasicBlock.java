package org.jruby.ir.representations;

import org.jruby.RubyInstanceConfig;
import org.jruby.dirgra.Edge;
import org.jruby.dirgra.ExplicitVertexID;
import org.jruby.ir.IRManager;
import org.jruby.ir.instructions.CallBase;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.Site;
import org.jruby.ir.instructions.YieldInstr;
import org.jruby.ir.listeners.InstructionsListener;
import org.jruby.ir.listeners.InstructionsListenerDecorator;
import org.jruby.ir.operands.Label;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.InlineCloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

import java.util.ArrayList;
import java.util.Collection;
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
        if (RubyInstanceConfig.IR_COMPILER_DEBUG || RubyInstanceConfig.IR_DEBUG_IGV != null) {
            IRManager irManager = cfg.getManager();
            InstructionsListener listener = irManager.getInstructionsListener();
            if (listener != null) {
                instrs = new InstructionsListenerDecorator(this, instrs, listener);
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

    public boolean canRaiseExceptions() {
        for (Instr i: getInstrs()) {
            if (i.canRaiseException()) return true;
        }

        return false;
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

    public void insertInstr(int index, Instr i) {
        instrs.add(index, i);
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

    /**
     * What site object contains this callsiteId or die trying.
     * @param callsiteId to be found
     * @return the Site instance (CallBase or YieldInstr)
     */
    public Site siteOf(long callsiteId) {
        for (Instr instr: instrs) {
            if (instr instanceof Site && ((Site) instr).getCallSiteId() == callsiteId) return (Site) instr;
        }

        throw new RuntimeException("siteOf asked for non-existent callsiteId: " + callsiteId);
    }

    // Adds all instrs after the found instr to a new BB and removes them from the original BB
    // If includeSpltpointInstr is true it will include that instr in the new BB.
    public BasicBlock splitAtInstruction(Site splitPoint, Label newLabel, boolean includeSplitPointInstr) {
        BasicBlock newBB = new BasicBlock(cfg, newLabel);
        int idx = 0;
        int numInstrs = instrs.size();
        boolean found = false;
        for (Instr i: instrs) {
            // FIXME: once found we should not be continually checking for more should be in !found
            if (i instanceof Site && ((Site) i).getCallSiteId() == splitPoint.getCallSiteId()) found = true;

            // Move instructions from split point into the new bb
            if (found) {
                // FIXME: move includeSplit when found so we can remove consuing site id logic from here...
                if (includeSplitPointInstr ||
                        !(i instanceof Site) ||
                        ((Site) i).getCallSiteId() != splitPoint.getCallSiteId()) newBB.addInstr(i);
            } else {
                idx++;
            }
        }

        if (!found) throw new RuntimeException("Cound not find split point: " + splitPoint);

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

    public BasicBlock clone(CloneInfo info, CFG newCFG) {
        BasicBlock newBB = new BasicBlock(newCFG, info.getRenamedLabel(label));
        boolean isClosureClone = info instanceof InlineCloneInfo && ((InlineCloneInfo) info).isClosure();

        for (Instr instr: instrs) {
            Instr newInstr = instr.clone(info);
            // Inlining clones the original CFG/BBs and we want to maintain ipc since it is how
            // we find which instr we want (we clone original instr and ipc is our identity).
            //if (info instanceof SimpleCloneInfo && ((SimpleCloneInfo) info).shouldCloneIPC()) {
            //    newInstr.setIPC(instr.getIPC());
            //    newInstr.setRPC(instr.getRPC());
            //}

            // All call-derived types do not clone this field.  Inliner clones original instrs
            // and we need this preserved to make sure we do not endless inline the same call.
            if (instr instanceof CallBase && ((CallBase) instr).inliningBlocked()) {
                ((CallBase) newInstr).blockInlining();
            }

            // Really icky but when we clone any call instructions we assign a new callsiteid.
            // If an inline occurs and profiler decides before new inlined host scope has come into
            // play it will not be able to find the current callsite.  By keeping the same values
            // the profiler will continue to work.
            if (instr instanceof Site) {
                ((Site) newInstr).setCallSiteId(((Site) instr).getCallSiteId());
            }

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
                instrs.add(i.clone(ii));
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
        return "BB [" + id + ':' + label + ']';
    }

    public String toStringInstrs() {
        StringBuilder buf = new StringBuilder(toString());

        Collection<Edge<BasicBlock>> outs = cfg.getOutgoingEdges(this);
        if (!outs.isEmpty()) {
            for (Edge<BasicBlock> edge : outs) {
                buf.append(" -" + edge.getType() + "->" + edge.getDestination().getID());
            }
        }
        buf.append('\n');

        for (Instr instr : getInstrs()) {
            buf.append('\t').append(instr).append('\n');
        }

        return buf.toString();
    }
}
