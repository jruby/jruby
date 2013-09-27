package org.jruby.ir.transformations.inlining;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jruby.runtime.Arity;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRClosure;
import org.jruby.ir.Tuple;
import org.jruby.ir.instructions.CallBase;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.instructions.ToAryInstr;
import org.jruby.ir.instructions.YieldInstr;
import org.jruby.ir.operands.Array;
import org.jruby.ir.operands.ClosureLocalVariable;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Self;
import org.jruby.ir.operands.Splat;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.ir.representations.CFG;

public class InlinerInfo {
    private static Integer globalInlineCount = 0;

    private CFG callerCFG;
    private CallBase call;

    private Operand[] callArgs;
    private boolean canMapArgsStatically;
    private Variable argsArray;
    private Map<Label, Label> lblRenameMap;
    private Map<Variable, Variable> varRenameMap;
    private Map<BasicBlock, BasicBlock> bbRenameMap;
    private List yieldSites;
    private Operand callReceiver;
    private String inlineVarPrefix;

    // SSS FIXME: Ugly?
    // For inlining closures
    private Operand yieldArg;
    private Variable yieldResult;
    private boolean inClosureInlineMode;

    // SSS FIXME: Ugly?
    // For cloning closure
    private boolean inClosureCloneMode;
    private IRClosure clonedClosure;

    // SSS FIXME: This is a copy of a method in instructions/calladapter/CallAdapter.java
    // Maybe move this is to a util/Helpers class?
    private static boolean containsSplat(Operand args[]) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Splat) return true;
        }

        return false;
    }

    public InlinerInfo() { }

    public InlinerInfo(CallBase call, CFG c) {
        this.varRenameMap = new HashMap<Variable, Variable>();
        this.lblRenameMap = new HashMap<Label, Label>();
        this.bbRenameMap = new HashMap<BasicBlock, BasicBlock>();
        this.yieldSites = new ArrayList();
        this.call = call;
        this.callArgs = call.getCallArgs();
        this.callerCFG = c;
        this.callReceiver = call.getReceiver();
        this.inClosureCloneMode = false;
        this.inClosureInlineMode = false;
        this.canMapArgsStatically = !containsSplat(callArgs);
        this.argsArray = this.canMapArgsStatically ?  null : getInlineHostScope().getNewTemporaryVariable();
        synchronized(globalInlineCount) {
            this.inlineVarPrefix = "%in" + globalInlineCount + "_";
            globalInlineCount++;
        }
    }

    public InlinerInfo cloneForInliningClosure() {
        InlinerInfo clone = new InlinerInfo();
        clone.varRenameMap = new HashMap<Variable, Variable>();
        clone.lblRenameMap = new HashMap<Label, Label>();
        clone.bbRenameMap = new HashMap<BasicBlock, BasicBlock>();
        clone.call = this.call;
        clone.callArgs = this.callArgs;
        clone.callerCFG = this.callerCFG;
        clone.callReceiver = this.callReceiver;
        clone.inClosureCloneMode = false;
        clone.inClosureInlineMode = true;
        clone.canMapArgsStatically = false;
        return clone;
    }

    public InlinerInfo cloneForCloningClosure(IRClosure clonedClosure) {
        InlinerInfo clone = new InlinerInfo();
        clone.varRenameMap = new HashMap<Variable, Variable>();
        for (Variable v: varRenameMap.keySet()) {
            clone.varRenameMap.put(v, varRenameMap.get(v));
        }
        clone.lblRenameMap = new HashMap<Label, Label>();
        clone.clonedClosure = clonedClosure;
        clone.inClosureCloneMode = true;
        clone.inClosureInlineMode = false;
        clone.canMapArgsStatically = false;
        return clone;
    }

    /**
     * Returns the scope into which code is being inlined.
     */
    public IRScope getInlineHostScope() {
        return callerCFG.getScope();
    }

    public IRScope getNewLexicalParentForClosure() {
        return inClosureCloneMode ? clonedClosure : getInlineHostScope();
    }

    public Label getRenamedLabel(Label l) {
        Label newLbl = this.lblRenameMap.get(l);
        if (newLbl == null) {
           newLbl = inClosureCloneMode ? l.clone() : getInlineHostScope().getNewLabel();
           this.lblRenameMap.put(l, newLbl);
        }
        return newLbl;
    }

    public void setupYieldArgsAndYieldResult(YieldInstr yi, BasicBlock yieldBB, Arity blockArity) {
        int     blockArityValue = blockArity.getValue();
        Operand yieldInstrArg = yi.getYieldArg();

        if ((yieldInstrArg == UndefinedValue.UNDEFINED) || (blockArityValue == 0)) {
            this.yieldArg = new Array(); // Zero-elt array
        } else if (yieldInstrArg instanceof Array) {
            this.yieldArg = yieldInstrArg;
            // 1:1 arg match
            if (((Array)yieldInstrArg).size() == blockArityValue) canMapArgsStatically = true;
        } else {
            // SSS FIXME: The code below is not entirely correct.  We have to process 'yi.getYieldArg()' similar
            // to how InterpretedIRBlockBody (1.8 and 1.9 modes) processes it.  We may need a special instruction
            // that takes care of aligning the stars and bringing good fortune to arg yielder and arg receiver.

            IRScope callerScope   = getInlineHostScope();
            boolean needSpecialProcessing = (blockArityValue != -1) && (blockArityValue != 1);
            Variable yieldArgArray = callerScope.getNewTemporaryVariable();
            yieldBB.addInstr(new ToAryInstr(yieldArgArray, yieldInstrArg, callerScope.getManager().getTrue()));
            this.yieldArg = yieldArgArray;
        }

        this.yieldResult = yi.getResult();
    }

    public Variable getRenamedVariable(Variable v) {
        Variable newVar = this.varRenameMap.get(v);
        if (newVar == null) {
            if (inClosureCloneMode) {
                // when cloning a closure, local vars and temps are not renamed
                newVar = v.cloneForCloningClosure(this);
            } else if (inClosureInlineMode) {
                // when inlining a closure,
                // - local var depths are reduced by 1 (to move them to the host scope)
                // - tmp vars are reallocated in the host scope
                if (v instanceof LocalVariable) {
                    LocalVariable lv = (LocalVariable)v;
                    int depth = lv.getScopeDepth();
                    newVar = getInlineHostScope().getLocalVariable(lv.getName(), depth > 1 ? depth - 1 : 0);
                } else {
                    newVar = getInlineHostScope().getNewTemporaryVariable();
                }
            } else {
                // when inlining a method, local vars and temps have to be renamed
                newVar = getInlineHostScope().getNewInlineVariable(inlineVarPrefix, v);
            }
            this.varRenameMap.put(v, newVar);
        } else if (inClosureCloneMode && (v instanceof LocalVariable)) {
            LocalVariable l_v = (LocalVariable)v;
            LocalVariable l_newVar = (LocalVariable)newVar;
            if (l_v.getScopeDepth() != l_newVar.getScopeDepth()) newVar = l_newVar.cloneForDepth(l_v.getScopeDepth());
        }
        return newVar;
    }

    public BasicBlock getRenamedBB(BasicBlock bb) {
        return bbRenameMap.get(bb);
    }

    public BasicBlock getOrCreateRenamedBB(BasicBlock bb) {
        BasicBlock renamedBB = getRenamedBB(bb);
        if (renamedBB == null) {
            renamedBB =  new BasicBlock(this.callerCFG, getRenamedLabel(bb.getLabel()));
            if (bb.isRescueEntry()) renamedBB.markRescueEntryBB();
            bbRenameMap.put(bb, renamedBB);
        }
        return renamedBB;
    }

    public boolean canMapArgsStatically() {
        return this.canMapArgsStatically;
    }

    public Operand getArgs() {
        return inClosureInlineMode ? yieldArg : argsArray;
    }

    public int getArgsCount() {
        return canMapArgsStatically ? (inClosureInlineMode ? ((Array)yieldArg).size() : callArgs.length) : -1;
    }

    public Operand getArg(int index) {
        int n = getArgsCount();
        return index < n ? (inClosureInlineMode ? ((Array)yieldArg).get(index) : callArgs[index]) : null;
    }

    public Operand getArg(int argIndex, boolean restOfArgArray) {
        if (restOfArgArray == false) {
            return getArg(argIndex);
        } else if (inClosureInlineMode) {
            throw new RuntimeException("Cannot get rest yield arg at inline time!");
        } else {
            if(argIndex >= callArgs.length) {
               return new Array();
           }
           else {
               Operand[] tmp = new Operand[callArgs.length - argIndex];
               for (int j = argIndex; j < callArgs.length; j++)
                   tmp[j-argIndex] = callArgs[j];

               return new Array(tmp);
           }
        }
    }

    public Operand getSelfValue(Self self) {
        return inClosureCloneMode ? self : callReceiver;
    }

    public Operand getCallClosure() {
        return call.getClosureArg(callerCFG.getScope().getManager().getNil());
    }

    // SSS FIXME: Ugly?
    public IRClosure getClonedClosure() {
        return clonedClosure;
    }

    public Variable getCallResultVariable() {
        return (call instanceof ResultInstr) ? ((ResultInstr)call).getResult() : null;
    }

    public void recordYieldSite(BasicBlock bb, YieldInstr i) {
        yieldSites.add(new Tuple<BasicBlock, YieldInstr>(bb, i));
    }

    public List getYieldSites() {
        return yieldSites;
    }

    public Variable getYieldResult() {
        return yieldResult;
    }
}
