package org.jruby.ir.representations;

import org.jruby.ir.operands.Label;

import java.util.ArrayList;
import java.util.List;

// This class is currently only used during CFG building and is hence made private.
// A scope's CFG exception regions are currently not exposed anywhere after the CFG is built.
// If in future, it is useful somewhere else, this class can be made public and a scope's
// exception regions can be exposed as well.
class ExceptionRegion {
    private final Label firstRescueBlockLabel; // Label of the first rescue block

    private final List<BasicBlock> exclusiveBBs;  // Basic blocks exclusively contained within this region
    private final List<ExceptionRegion> nestedRegions; // Rescue regions nested within this one
    private final BasicBlock startBB;       // First BB of the rescued region
    private BasicBlock endBB;         // Last BB of the rescued region

    public ExceptionRegion(Label firstRescueBlockLabel, BasicBlock startBB) {
        this.firstRescueBlockLabel = firstRescueBlockLabel;
        this.startBB = startBB;
        exclusiveBBs = new ArrayList<BasicBlock>();
        nestedRegions = new ArrayList<ExceptionRegion>();
    }

    public void setEndBB(BasicBlock bb) {
        endBB = bb;
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

    public Label getFirstRescueBlockLabel() {
        return firstRescueBlockLabel;
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

        for (ExceptionRegion er: nestedRegions) {
            buf.append(er.toString());
        }
        return buf.toString();
    }
}
