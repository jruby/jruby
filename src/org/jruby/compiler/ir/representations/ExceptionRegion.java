package org.jruby.compiler.ir.representations;

import java.util.List;
import java.util.ArrayList;
import org.jruby.compiler.ir.operands.Label;

public class ExceptionRegion {
    private Label ensureBlockLabel; 
    private List<Label> rescueBlockLabels; // Labels of all the rescue blocks that handle exceptions in this region

    private List<BasicBlock> exclusiveBBs;  // Basic blocks exclusively contained within this region
    private List<ExceptionRegion> nestedRegions; // Rescue regions nested within this one
    private BasicBlock endBB;         // Last BB of the rescued region
    private BasicBlock firstRescueBB; // First BB of the first rescue block of this region 

    public ExceptionRegion(List<Label> rescueBlockLabels, Label ensureBlockLabel) {
        this.rescueBlockLabels = rescueBlockLabels;
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
        return rescueBlockLabels.get(0);
    }

    public ExceptionRegion cloneForInlining(InlinerInfo ii) {
        List<Label> newLabels = new ArrayList<Label>();
        for (Label l: rescueBlockLabels) {
            newLabels.add(ii.getRenamedLabel(l));
        }

        ExceptionRegion newR = new ExceptionRegion(newLabels, ensureBlockLabel == null ? null : ii.getRenamedLabel(ensureBlockLabel));
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
