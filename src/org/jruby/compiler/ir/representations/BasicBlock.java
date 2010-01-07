package org.jruby.compiler.ir.representations;

import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.instructions.IR_Instr;

import java.util.List;

public class BasicBlock {
    int _id;                        // Basic Block id
    CFG _cfg;                       // CFG that this basic block belongs to
    Label _label;                   // All basic blocks have a starting label
    List<IR_Instr> _instrs;         // List of non-label instructions
    boolean _isLive;
    BasicBlock _rescuedBodyEndBB;  // If this is the start of a rescue block, this is the last basic block of the rescued body

    public BasicBlock(CFG c, Label l) {
        _instrs = new java.util.ArrayList<IR_Instr>();
        _label = l;
        _isLive = true;
        _cfg = c;
        _id = c.getNextBBID();
        _rescuedBodyEndBB = null;
    }

    void setRescuedBodyEndBB(BasicBlock rbEnd) {
        _rescuedBodyEndBB = rbEnd;
    }

    public int getID() {
        return _id;
    }

    public void addInstr(IR_Instr i) {
        _instrs.add(i);
    }

    public void insertInstr(IR_Instr i) {
        _instrs.add(0, i);
    }

    public List<IR_Instr> getInstrs() {
        return _instrs;
    }

    public BasicBlock getRescuedBodyEndBB() {
        return _rescuedBodyEndBB;
    }

    @Override
    public String toString() {
        return "BB [" + _id + ":" + _label + "]";
    }

    public String toStringInstrs() {
        StringBuilder buf = new StringBuilder(toString() + "\n");

        for (IR_Instr instr : getInstrs()) {
            if (!instr.isDead()) buf.append('\t').append(instr).append('\n');
        }
        
        return buf.toString();
    }
}
