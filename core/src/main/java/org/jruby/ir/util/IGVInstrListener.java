package org.jruby.ir.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jruby.ir.Tuple;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.listeners.InstructionsListener;
import org.jruby.ir.representations.BasicBlock;

/**
 * Created by enebo on 1/28/17.
 */
public class IGVInstrListener implements InstructionsListener {
    private Map<BasicBlock, List<Instr>> removedInstrs = new HashMap<BasicBlock, List<Instr>>();
    private List<Tuple<Instr, Instr>> removedEdges = new ArrayList<Tuple<Instr, Instr>>();

    public IGVInstrListener() {
    }

    public List<Tuple<Instr, Instr>> getRemovedEdges() {
        return removedEdges;
    }

    public List<Instr> removedList(BasicBlock basicBlock) {
        List<Instr> removedList = removedInstrs.get(basicBlock);

        if (removedList == null) {
            removedList = new ArrayList<Instr>();
            removedInstrs.put(basicBlock, removedList);
        }

        return removedList;
    }

    @Override
    public void instrChanged(BasicBlock basicBlock, Instr oldInstr, Instr newInstr, int index, OperationType op) {
        switch(op) {
            case REMOVE: {
                if (index > 0) {
                    Instr previousInstr = basicBlock.getInstrs().get(index - 1);
                    removedEdges.add(new Tuple<Instr, Instr>(previousInstr, oldInstr));
                }
                removedList(basicBlock).add(oldInstr);
            }
        }
    }

    public void reset() {
        removedInstrs = new HashMap<BasicBlock, List<Instr>>();
        removedEdges = new ArrayList<Tuple<Instr, Instr>>();
    }
}
