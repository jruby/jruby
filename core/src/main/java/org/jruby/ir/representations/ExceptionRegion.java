package org.jruby.ir.representations;

import java.util.List;
import java.util.ArrayList;
import org.jruby.ir.operands.Label;
import org.jruby.ir.transformations.inlining.InlinerInfo;

public class ExceptionRegion {
    private Label ensureBlockLabel; // Label of the ensure block
    private Label firstRescueBlockLabel; // Label of the first rescue block

    private List<BasicBlock> exclusiveBBs;  // Basic blocks exclusively contained within this region
    private List<ExceptionRegion> nestedRegions; // Rescue regions nested within this one
    private BasicBlock startBB;       // First BB of the rescued region
    private BasicBlock endBB;         // Last BB of the rescued region
    private BasicBlock firstRescueBB; // First BB of the first rescue block of this region

    public ExceptionRegion(Label firstRescueBlockLabel, Label ensureBlockLabel, BasicBlock startBB) {
        this.firstRescueBlockLabel = firstRescueBlockLabel;
        this.ensureBlockLabel = ensureBlockLabel;
        this.startBB = startBB;
        exclusiveBBs = new ArrayList<BasicBlock>();
        nestedRegions = new ArrayList<ExceptionRegion>();
    }

    public void setEndBB(BasicBlock bb) {
        endBB = bb;
    }

    public Label getEnsureBlockLabel() {
        return ensureBlockLabel;
    }

    public BasicBlock getStartBB() {
        return startBB;
    }

    public BasicBlock getEndBB() {
        return endBB;
    }

    public List<BasicBlock> getExclusiveBBs() {
        return exclusiveBBs;
    }

    public void addBB(BasicBlock bb) {
        exclusiveBBs.add(bb);
    }

    public void addNestedRegion(ExceptionRegion r) {
        nestedRegions.add(r);
        exclusiveBBs.remove(r.exclusiveBBs.get(0));
    }

    // BB b has been merged into BB a.
    // Update the exception region.
    public void mergeBBs(BasicBlock a, BasicBlock b) {

        // Remove b from exclusiveBBs.
        exclusiveBBs.remove(b);

        // Update endBB if it is b
        if (endBB == b) {
            endBB = a;
        }

        // Process nested regions
        for (ExceptionRegion er: nestedRegions) {
            er.mergeBBs(a, b);
        }
    }

    public void setFirstRescueBB(BasicBlock frbb) {
        firstRescueBB = frbb;
    }

    public Label getFirstRescueBlockLabel() {
        return firstRescueBlockLabel;
    }

    public ExceptionRegion cloneForInlining(InlinerInfo ii) {
        ExceptionRegion newR = new ExceptionRegion(ii.getRenamedLabel(firstRescueBlockLabel),
            ensureBlockLabel == null ? null : ii.getRenamedLabel(ensureBlockLabel),
            ii.getRenamedBB(this.startBB));
        newR.endBB = ii.getRenamedBB(endBB);
        newR.firstRescueBB = ii.getRenamedBB(firstRescueBB);

        for (BasicBlock b: exclusiveBBs) {
            newR.addBB(ii.getRenamedBB(b));
        }

        for (ExceptionRegion r: nestedRegions) {
            newR.addNestedRegion(r.cloneForInlining(ii));
        }

        return newR;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("--- Exception Region ---");
        buf.append("\n");

        buf.append("Exclusive BBs\n");
        for (BasicBlock b: exclusiveBBs) {
            buf.append("\t");
            buf.append(b);
            buf.append("\n");
        }
        buf.append("End: ");
        buf.append(endBB.getLabel());
        buf.append("\n");

        buf.append("Rescuer: ");
        buf.append(firstRescueBlockLabel);
        buf.append("\n");

        if (ensureBlockLabel != null) {
            buf.append("Ensurer: ");
            buf.append(ensureBlockLabel);
            buf.append("\n");
        }
        buf.append("\n");

        for (ExceptionRegion er: nestedRegions) {
            buf.append(er.toString());
        }
        return buf.toString();
    }
}
