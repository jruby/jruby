package org.jruby.compiler.ir.representations;

import org.jruby.compiler.ir.operands.Array;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.CopyInstr;
import org.jruby.compiler.ir.instructions.ClosureReturnInstr;
import org.jruby.compiler.ir.instructions.NopInstr;
import org.jruby.compiler.ir.instructions.ReceiveClosureArgInstr;
import org.jruby.compiler.ir.instructions.ReceiveClosureInstr;
import org.jruby.compiler.ir.instructions.ReceiveSelfInstruction;
import org.jruby.compiler.ir.instructions.YieldInstr;

import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;
import org.jruby.compiler.ir.util.DataInfo;

public class BasicBlock implements DataInfo {
    private int id;                        // Basic Block id
    private CFG cfg;                       // CFG that this basic block belongs to
    private Label label;                   // All basic blocks have a starting label
    private List<Instr> instrs;         // List of non-label instructions
    private boolean isLive;
    private Instr[] instrsArray = null;    

    public BasicBlock(CFG c, Label l) {
        instrs = new ArrayList<Instr>();
        label = l;
        isLive = true;
        cfg = c;
        id = c.getNextBBID();
    }

    public void updateCFG(CFG c) {
        cfg = c;
        id = c.getNextBBID();
    }

    public int getID() {
        return id;
    }

    public Label getLabel() {
        return label;
    }

    public void addInstr(Instr i) {
        instrs.add(i);
    }

    public void insertInstr(Instr i) {
        instrs.add(0, i);
    }

    public List<Instr> getInstrs() {
        return instrs;
    }


    public Instr[] getInstrsArray() {
        if (instrsArray == null) instrsArray = instrs.toArray(new Instr[instrs.size()]);

        return instrsArray;
    }

    public Instr getLastInstr() {
        int n = instrs.size();
        return (n == 0) ? null : instrs.get(n-1);
    }

    public boolean removeInstr(Instr i) {
       return i == null? false : instrs.remove(i);
    }

    public boolean isEmpty() {
        return instrs.isEmpty();
    }

    public BasicBlock splitAtInstruction(Instr splitPoint, Label newLabel, boolean includeSplitPointInstr) {
        BasicBlock newBB = new BasicBlock(cfg, newLabel);
        int idx = 0;
        int numInstrs = instrs.size();
        boolean found = false;
        for (Instr i: instrs) {
            if (i == splitPoint) found = true;

            // Move instructions from split point into the new bb
            if (found) {
                if (includeSplitPointInstr || i != splitPoint) newBB.addInstr(i);
            } else {
                idx++;
            }
        }

        // Remove all instructions from current bb that were moved over.
        for (int j = 0; j < numInstrs-idx; j++) {
            instrs.remove(idx);
        }

        return newBB;
    }

    public void swallowBB(BasicBlock foodBB) {
        this.instrs.addAll(foodBB.instrs);
    }

    public BasicBlock cloneForInlining(InlinerInfo ii) {
        BasicBlock clonedBB = ii.getOrCreateRenamedBB(this);
        for (Instr i: getInstrs()) {
            Instr clonedInstr = i.cloneForInlining(ii);
            if (clonedInstr != null) {
                clonedBB.addInstr(clonedInstr);
                if (clonedInstr instanceof YieldInstr) ii.recordYieldSite(clonedBB, (YieldInstr)clonedInstr);
            }
        }

        return clonedBB;
    }

    // SSS FIXME: Verify correctness; also deal with YieldInstr.wrapIntoArray flag
    public void processClosureArgAndReturnInstrs(InlinerInfo ii, YieldInstr yi) {
        Variable  yieldResult = ii.getRenamedVariable(yi.getResult());
        Operand[] yieldArgs   = yi.getNonBlockOperands();

        for (ListIterator<Instr> it = ((ArrayList<Instr>)instrs).listIterator(); it.hasNext(); ) {
            Instr i = it.next();
            if (i instanceof ClosureReturnInstr) {
                // Replace the closure return receive with a simple copy
                it.set(new CopyInstr(yieldResult, ((ClosureReturnInstr)i).getArg()));
            } else if (i instanceof ReceiveSelfInstruction) {
                ReceiveSelfInstruction rsi = (ReceiveSelfInstruction)i;
                // SSS FIXME: It is not always the case that the call receiver is also the %self within
                // a block  Ex: ... r.foo(args) { ... blah .. }.  i.e. there are scenarios where %self
                // within the block is not identical to 'r'.  Handle this!
                if (!rsi.getResult().equals(ii.getCallReceiver())) {
                    it.set(new CopyInstr(rsi.getResult(), ii.getCallReceiver()));
                } else {
                    it.set(NopInstr.NOP);
                }
            } else if (i instanceof ReceiveClosureInstr) {
                ReceiveClosureInstr rci = (ReceiveClosureInstr)i;
                if (!rci.getResult().equals(ii.getCallClosure())) {
                    it.set(new CopyInstr(rci.getResult(), ii.getCallClosure()));
                } else {
                    it.set(NopInstr.NOP);
                }
            } else if (i instanceof ReceiveClosureArgInstr) {
                Operand closureArg;
                ReceiveClosureArgInstr rcai = (ReceiveClosureArgInstr)i;
                int argIndex = rcai.getArgIndex();
                
                if (argIndex < yieldArgs.length) {
                    closureArg = yieldArgs[argIndex].cloneForInlining(ii);
                } else if (argIndex >= yieldArgs.length) {
                    closureArg = new Array();
                } else {
                    Operand[] tmp = new Operand[yieldArgs.length - argIndex];
                    for (int j = argIndex; j < yieldArgs.length; j++) {
                        tmp[j-argIndex] = yieldArgs[j].cloneForInlining(ii);
                    }

                    closureArg = new Array(tmp);
                }

                // Replace the arg receive with a simple copy
                it.set(new CopyInstr(rcai.getResult(), closureArg));
            }
        }
    }

    @Override
    public String toString() {
        return "BB [" + id + ":" + label + "]";
    }

    public String toStringInstrs() {
        StringBuilder buf = new StringBuilder(toString() + "\n");

        for (Instr instr : getInstrs()) {
            buf.append('\t').append(instr).append('\n');
        }
        
        return buf.toString();
    }
}
