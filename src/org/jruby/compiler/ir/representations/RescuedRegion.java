package org.jruby.compiler.ir.representations;

import java.util.List;
import java.util.ArrayList;
import org.jruby.compiler.ir.operands.Label;

public class RescuedRegion {
    Label               _elseBlockLabel;// Label for the else clause
    List<Label>         _rescueBlockLabels;  // Labels of all the rescue blocks that handle exceptions in this region

    List<BasicBlock>    _exclusiveBBs;  // Basic blocks exclusively contained within this region
    List<RescuedRegion> _nestedRegions; // Rescue regions nested within this one
    BasicBlock          _endBB;         // Last BB of the rescued region
    BasicBlock          _firstRescueBB; // First BB of the first rescue block of this region 

    public RescuedRegion(Label elseBlockLabel, List<Label> rescueBlockLabels) {
        _elseBlockLabel = elseBlockLabel;
        _rescueBlockLabels = rescueBlockLabels;
        _exclusiveBBs = new ArrayList<BasicBlock>();
        _nestedRegions = new ArrayList<RescuedRegion>();
    }

    public void setEndBB(BasicBlock bb) {
        _endBB = bb;
    }

    public void addBB(BasicBlock bb) {
        _exclusiveBBs.add(bb);
    }

    public void addNestedRegion(RescuedRegion r) {
        _nestedRegions.add(r);
    }

    public void setFirstRescueBB(BasicBlock frbb) {
        _firstRescueBB = frbb;
    }

    public Label getFirstRescueBlockLabel() {
        return _rescueBlockLabels.get(0);
    }

    public RescuedRegion cloneForInlining(InlinerInfo ii) {
        List<Label> newLabels = new ArrayList<Label>();
        for (Label l: _rescueBlockLabels)
            newLabels.add(ii.getRenamedLabel(l));

        RescuedRegion newR = new RescuedRegion(_elseBlockLabel == null ? null : ii.getRenamedLabel(_elseBlockLabel), newLabels);
        newR._endBB = ii.getRenamedBB(_endBB);
        newR._firstRescueBB = ii.getRenamedBB(_firstRescueBB);
        for (BasicBlock b: _exclusiveBBs) {
            newR.addBB(ii.getRenamedBB(b));
        }

        for (RescuedRegion r: _nestedRegions) {
            newR.addNestedRegion(r.cloneForInlining(ii));
        }

        return newR;
    }
}
