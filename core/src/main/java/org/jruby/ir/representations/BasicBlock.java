package org.jruby.ir.representations;

import org.jruby.RubyInstanceConfig;
import org.jruby.dirgra.Edge;
import org.jruby.dirgra.ExplicitVertexID;
import org.jruby.ir.IRManager;
import org.jruby.ir.instructions.CallBase;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.JumpInstr;
import org.jruby.ir.instructions.NonlocalReturnInstr;
import org.jruby.ir.instructions.Site;
import org.jruby.ir.instructions.YieldInstr;
import org.jruby.ir.listeners.InstructionsListener;
import org.jruby.ir.listeners.InstructionsListenerDecorator;
import org.jruby.ir.operands.Label;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.InlineCloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BasicBlock implements ExplicitVertexID, Comparable<BasicBlock> {
    private final int         id;             // Basic Block id
    private final CFG         cfg;            // CFG that this basic block belongs to
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
        instrs = new ArrayList<>(1);
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

    /**
     * On an exception occurring in this block which BB should we go to?
     * @return BB of exception handling or null if none.
     */
    public BasicBlock exceptionBB() {
        return cfg.getRescuerBBFor(this);
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
    // If includeSplitpointInstr is true it will include that instr in the new BB.
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
                // FIXME: move includeSplit when found so we can remove consuming site id logic from here...
                if (includeSplitPointInstr ||
                        !(i instanceof Site) ||
                        ((Site) i).getCallSiteId() != splitPoint.getCallSiteId()) newBB.addInstr(i);
            } else {
                idx++;
            }
        }

        if (!found) throw new RuntimeException("Could not find split point: " + splitPoint);

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
            // Inlining clones the original CFG/BBs, and we want to maintain ipc since it is how
            // we find which instr we want (we clone original instr and ipc is our identity).
            //if (info instanceof SimpleCloneInfo && ((SimpleCloneInfo) info).shouldCloneIPC()) {
            //    newInstr.setIPC(instr.getIPC());
            //    newInstr.setRPC(instr.getRPC());
            //}

            // All call-derived types do not clone this field.  Inliner clones original instrs,
            // and we need this preserved to make sure we do not endless inline the same call.
            if (instr instanceof CallBase && ((CallBase) instr).inliningBlocked()) {
                ((CallBase) newInstr).blockInlining();
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
                if (clonedInstr instanceof YieldInstr) {
                    ii.recordYieldSite(clonedBB, (YieldInstr)clonedInstr);
                } else if (i instanceof NonlocalReturnInstr && clonedInstr instanceof CopyInstr) {
                    // non-local returns assign to method return variable but must jump to proper exit point
                    clonedBB.addInstr(new JumpInstr(ii.getHostScope().getFullInterpreterContext().getCFG().getExitBB().getLabel()));
                    // FIXME: enebo...I see no guarantee that this copy will be part of a return?  This behavior is
                    // masked in any case I can see with optimization to not use a copy but convert non-local to local return.
                }
            }
        }

        return clonedBB;
    }

    @Override
    public int compareTo(final BasicBlock other) {
        return Integer.compare(id, other.id);
    }

    @Override
    public String toString() {
        return "BB [" + id + ':' + label + ']';
    }

    public String toStringInstrs() {
        StringBuilder buf = new StringBuilder(toString());

        Collection<Edge<BasicBlock, CFG.EdgeType>> outs = cfg.getOutgoingEdges(this);
        if (!outs.isEmpty()) {
            for (Edge<BasicBlock, CFG.EdgeType> edge : outs) {
                buf.append(" -").append(edge.getType()).append("->").append(edge.getDestination().getID());
            }
        }
        buf.append('\n');

        for (Instr instr : getInstrs()) {
            buf.append('\t').append(instr).append('\n');
        }

        return buf.toString();
    }
}
