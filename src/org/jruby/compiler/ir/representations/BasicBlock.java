package org.jruby.compiler.ir.representations;

import org.jruby.compiler.ir.operands.Array;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.instructions.IR_Instr;
import org.jruby.compiler.ir.instructions.YIELD_Instr;
import org.jruby.compiler.ir.instructions.COPY_Instr;
import org.jruby.compiler.ir.instructions.CLOSURE_RETURN_Instr;
import org.jruby.compiler.ir.instructions.RECV_CLOSURE_ARG_Instr;

import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;

public class BasicBlock {
    int _id;                        // Basic Block id
    CFG _cfg;                       // CFG that this basic block belongs to
    Label _label;                   // All basic blocks have a starting label
    List<IR_Instr> _instrs;         // List of non-label instructions
    boolean _isLive;

    public BasicBlock(CFG c, Label l) {
        _instrs = new ArrayList<IR_Instr>();
        _label = l;
        _isLive = true;
        _cfg = c;
        _id = c.getNextBBID();
    }

    public void updateCFG(CFG c) {
        _cfg = c;
        _id = c.getNextBBID();
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

    public BasicBlock splitAtInstruction(IR_Instr splitPoint, Label newLabel, boolean includeSplitPointInstr) {
        BasicBlock newBB = new BasicBlock(_cfg, newLabel);
        int idx = 0;
        int numInstrs = _instrs.size();
        boolean found = false;
        for (IR_Instr i: _instrs) {
            if (i == splitPoint)
                found = true;

            // Move instructions from split point into the new bb
            if (found) {
                if (includeSplitPointInstr || i != splitPoint)
                    newBB.addInstr(i);
            }
            else {
                idx++;
            }
        }

        // Remove all instructions from current bb that were moved over.
        for (int j = 0; j < numInstrs-idx; j++) 
            _instrs.remove(idx);

        return newBB;
    }

    public void swallowBB(BasicBlock foodBB) {
        this._instrs.addAll(foodBB._instrs);
    }

    public BasicBlock cloneForInlining(InlinerInfo ii) {
        BasicBlock clonedBB = ii.getOrCreateRenamedBB(this);
        for (IR_Instr i: getInstrs()) {
            if (i instanceof YIELD_Instr) {
                ii.recordYieldSite(clonedBB, (YIELD_Instr)i);
            }
            else {
                clonedBB.addInstr(i.cloneForInlining(ii));
            }
        }

        return clonedBB;
    }

    public void processClosureArgAndReturnInstrs(InlinerInfo ii, YIELD_Instr yi) {
        Variable  yieldResult = ii.getRenamedVariable(yi._result);
        Operand[] yieldArgs   = yi.getOperands();

        for (ListIterator<IR_Instr> it = ((ArrayList<IR_Instr>)_instrs).listIterator(); it.hasNext(); ) {
            IR_Instr i = it.next();
            if (i instanceof CLOSURE_RETURN_Instr) {
                // Replace the closure return receive with a simple copy
                it.set(new COPY_Instr(yieldResult, ((CLOSURE_RETURN_Instr)i).getArg()));
            }
            else if (i instanceof RECV_CLOSURE_ARG_Instr) {
                Operand closureArg;
                RECV_CLOSURE_ARG_Instr rcai = (RECV_CLOSURE_ARG_Instr)i;
                int argIndex = rcai._argIndex;
                boolean restOfArgs = rcai._restOfArgArray;
                if (argIndex < yieldArgs.length) {
                    closureArg = yieldArgs[argIndex].cloneForInlining(ii);
                }
                else if (argIndex >= yieldArgs.length) {
                    closureArg = new Array();
                }
                else {
                    Operand[] tmp = new Operand[yieldArgs.length - argIndex];
                    for (int j = argIndex; j < yieldArgs.length; j++)
                        tmp[j-argIndex] = yieldArgs[j].cloneForInlining(ii);

                    closureArg = new Array(tmp);
                }

                // Replace the arg receive with a simple copy
                it.set(new COPY_Instr(rcai._result, closureArg));
            }
        }
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
