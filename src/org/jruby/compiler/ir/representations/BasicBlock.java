package org.jruby.compiler.ir.representations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.IRClosure;
import org.jruby.compiler.ir.instructions.CallBase;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.ResultInstr;
import org.jruby.compiler.ir.instructions.YieldInstr;
import org.jruby.compiler.ir.operands.Array;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.LocalVariable;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.operands.WrappedIRClosure;
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

    private void migrateToCFG(CFG newCFG) {
        newCFG.addBasicBlock(this);
        this.cfg = newCFG;
        this.id = newCFG.getNextBBID();
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
        // Gulp!
        this.instrs.addAll(foodBB.instrs);
    }

    public BasicBlock cloneForInlinedMethod(InlinerInfo ii) {
        BasicBlock clonedBB = ii.getOrCreateRenamedBB(this);
        for (Instr i: getInstrs()) {
            Instr clonedInstr = i.cloneForInlinedScope(ii);
            if (clonedInstr != null) {
                clonedBB.addInstr(clonedInstr);
                if (clonedInstr instanceof YieldInstr) ii.recordYieldSite(clonedBB, (YieldInstr)clonedInstr);
                if (clonedInstr instanceof CallBase) {
                    CallBase call = (CallBase)clonedInstr;
                    Operand block = call.getClosureArg(null);
                    if (block instanceof WrappedIRClosure) ii.getInlineHostScope().addClosure(((WrappedIRClosure)block).getClosure());
                }
            }
        }

        return clonedBB;
    }

    public void migrateToHostScope(InlinerInfo ii) {
        // Update cfg for this bb
        IRScope hostScope = ii.getInlineHostScope();
        migrateToCFG(hostScope.getCFG());

        // Clone
        List clonedInstrs = new ArrayList<Instr>();

        // Process instructions
        for (ListIterator<Instr> it = ((ArrayList<Instr>)instrs).listIterator(); it.hasNext(); ) {
            Instr i = it.next();

            // clone
            Instr clonedInstr = i.cloneForInlinedClosure(ii);
            if (clonedInstr != null) {
                clonedInstrs.add(clonedInstr);
                if (clonedInstr instanceof CallBase) {
                    CallBase call = (CallBase)clonedInstr;
                    Operand block = call.getClosureArg(null);
                    if (block instanceof WrappedIRClosure) hostScope.addClosure(((WrappedIRClosure)block).getClosure());
                }
            }
        }

        this.instrs = clonedInstrs;
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
