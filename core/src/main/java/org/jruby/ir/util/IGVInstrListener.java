package org.jruby.ir.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.listeners.InstructionsListener;
import org.jruby.ir.representations.BasicBlock;

/**
 * Created by enebo on 1/28/17.
 */
public class IGVInstrListener implements InstructionsListener {
    private Map<BasicBlock, List<Instr>> removedInstr = new HashMap<BasicBlock, List<Instr>>();

    public IGVInstrListener() {
    }

    public List<Instr> removedList(BasicBlock basicBlock) {
        List<Instr> removedList = removedInstr.get(basicBlock);

        if (removedList == null) {
            removedList = new ArrayList<Instr>();
            removedInstr.put(basicBlock, removedList);
        }

        return removedList;
    }

    @Override
    public void instrChanged(BasicBlock basicBlock, Instr oldInstr, Instr newInstr, int index, OperationType op) {
        switch(op) {
            case REMOVE:
                removedList(basicBlock).add(oldInstr);
        }
    }

    public void reset() {
        removedInstr = new HashMap<BasicBlock, List<Instr>>();
    }
}
