package org.jruby.compiler.ir.representations;

import java.util.List;
import java.util.ArrayList;
import org.jruby.compiler.ir.operands.Label;

public class ExceptionRegion {
    private Label ensureBlockLabel; // Label of the ensure block
    private Label firstRescueBlockLabel; // Label of the first rescue block

    private List<BasicBlock> exclusiveBBs;  // Basic blocks exclusively contained within this region
    private List<ExceptionRegion> nestedRegions; // Rescue regions nested within this one
    private BasicBlock endBB;         // Last BB of the rescued region
    private BasicBlock firstRescueBB; // First BB of the first rescue block of this region 

    public ExceptionRegion(Label firstRescueBlockLabel, Label ensureBlockLabel) {
        this.firstRescueBlockLabel = firstRescueBlockLabel;
        this.ensureBlockLabel = ensureBlockLabel;
        exclusiveBBs = new ArrayList<BasicBlock>();
        nestedRegions = new ArrayList<ExceptionRegion>();
    }

    public void setEndBB(BasicBlock bb) {
        endBB = bb;
    }
    
    public Label getEnsureBlockLabel() {
        return ensureBlockLabel;
    }
    
    public List<BasicBlock> getExclusiveBBs() {
        return exclusiveBBs;
    }

    public void addBB(BasicBlock bb) {
        exclusiveBBs.add(bb);
    }

    public void addNestedRegion(ExceptionRegion r) {
        nestedRegions.add(r);
    }

    public void setFirstRescueBB(BasicBlock frbb) {
        firstRescueBB = frbb;
    }

    public Label getFirstRescueBlockLabel() {
        return firstRescueBlockLabel;
    }

    public ExceptionRegion cloneForInlining(InlinerInfo ii) {
        ExceptionRegion newR = new ExceptionRegion(ii.getRenamedLabel(firstRescueBlockLabel), ensureBlockLabel == null ? null : ii.getRenamedLabel(ensureBlockLabel));
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
}
