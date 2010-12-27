package org.jruby.compiler.ir.representations;

import org.jruby.compiler.ir.operands.Array;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.YieldInstr;
import org.jruby.compiler.ir.instructions.CopyInstr;
import org.jruby.compiler.ir.instructions.ClosureReturnInstr;
import org.jruby.compiler.ir.instructions.ReceiveClosureArgInstr;

import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;

public class BasicBlock {
    int _id;                        // Basic Block id
    CFG _cfg;                       // CFG that this basic block belongs to
    Label _label;                   // All basic blocks have a starting label
    List<Instr> _instrs;         // List of non-label instructions
    boolean _isLive;

    public BasicBlock(CFG c, Label l) {
        _instrs = new ArrayList<Instr>();
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

    public Label getLabel() {
        return _label;
    }

    public void addInstr(Instr i) {
        _instrs.add(i);
    }

    public void insertInstr(Instr i) {
        _instrs.add(0, i);
    }

    public List<Instr> getInstrs() {
        return _instrs;
    }

	 private Instr[] _instrsArray = null;

    public Instr[] getInstrsArray() {
		  if (_instrsArray == null) {
            _instrsArray = _instrs.toArray(new Instr[_instrs.size()]);
		  }
		  return _instrsArray;
    }

    public Instr getLastInstr() {
        int n = _instrs.size();
        return (n == 0) ? null : _instrs.get(n-1);
    }

    public boolean removeInstr(Instr i) {
       if (i == null)
          return false;
       else
          return _instrs.remove(i);
    }

    public boolean isEmpty() {
        return _instrs.isEmpty();
    }

    public BasicBlock splitAtInstruction(Instr splitPoint, Label newLabel, boolean includeSplitPointInstr) {
        BasicBlock newBB = new BasicBlock(_cfg, newLabel);
        int idx = 0;
        int numInstrs = _instrs.size();
        boolean found = false;
        for (Instr i: _instrs) {
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
        for (Instr i: getInstrs()) {
				Instr clonedInstr = i.cloneForInlining(ii);
            clonedBB.addInstr(clonedInstr);
            if (clonedInstr instanceof YieldInstr)
                ii.recordYieldSite(clonedBB, (YieldInstr)clonedInstr);
        }

        return clonedBB;
    }

    public void processClosureArgAndReturnInstrs(InlinerInfo ii, YieldInstr yi) {
        Variable  yieldResult = ii.getRenamedVariable(yi.result);
        Operand[] yieldArgs   = yi.getOperands();

        for (ListIterator<Instr> it = ((ArrayList<Instr>)_instrs).listIterator(); it.hasNext(); ) {
            Instr i = it.next();
            if (i instanceof ClosureReturnInstr) {
                // Replace the closure return receive with a simple copy
                it.set(new CopyInstr(yieldResult, ((ClosureReturnInstr)i).getArg()));
            }
            else if (i instanceof ReceiveClosureArgInstr) {
                Operand closureArg;
                ReceiveClosureArgInstr rcai = (ReceiveClosureArgInstr)i;
                int argIndex = rcai.argIndex;
                boolean restOfArgs = rcai.restOfArgArray;
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
                it.set(new CopyInstr(rcai.result, closureArg));
            }
        }
    }

    @Override
    public String toString() {
        return "BB [" + _id + ":" + _label + "]";
    }

    public String toStringInstrs() {
        StringBuilder buf = new StringBuilder(toString() + "\n");

        for (Instr instr : getInstrs()) {
            if (!instr.isDead()) buf.append('\t').append(instr).append('\n');
        }
        
        return buf.toString();
    }
}
