package org.jruby.ir.transformations.inlining;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jruby.ir.IRScope;
import org.jruby.ir.Tuple;
import org.jruby.ir.instructions.CallBase;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.instructions.ToAryInstr;
import org.jruby.ir.instructions.YieldInstr;
import org.jruby.ir.operands.Array;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Splat;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.ir.representations.CFG;
import org.jruby.util.ByteList;

/**
 * Context object when performing an inline.
 */
public class InlineCloneInfo extends CloneInfo {
    private static Integer globalInlineCount = 0;

    private CFG hostCFG;

    private ByteList inlineVarPrefix;

    private Variable callReceiver;
    private CallBase call;
    private Operand[] callArgs;
    private Variable argsArray;
    private boolean canMapArgsStatically;

    private Map<BasicBlock, BasicBlock> bbRenameMap = new HashMap<>();

    private boolean isClosure;    // true for closure inlining
    private Operand yieldArg;     // Closure inlining only
    private Variable yieldResult; // Closure inlining only
    private List yieldSites = new ArrayList(); // Closure inlining only
    private IRScope scopeBeingInlined; // host scope is where we are going and this was original scope


    // Closure Inline
    public InlineCloneInfo(CFG cfg, IRScope scope, IRScope scopeBeingInlined) {
        super(scope);

        this.isClosure = true;
        this.hostCFG = cfg;
        this.scopeBeingInlined = scopeBeingInlined;
    }

    public InlineCloneInfo(CallBase call, CFG c, Variable callReceiver, IRScope scopeBeingInlined) {
        super( c.getScope());

        this.isClosure = false;
        this.hostCFG = c;
        this.call = call;
        this.callArgs = call.getCallArgs();
        this.callReceiver = callReceiver;
        this.canMapArgsStatically = !containsSplat(callArgs);
        this.argsArray = this.canMapArgsStatically ?  null : getHostScope().createTemporaryVariable();
        this.scopeBeingInlined = scopeBeingInlined;
        synchronized(globalInlineCount) {
            this.inlineVarPrefix = new ByteList(("%in" + globalInlineCount + "_").getBytes());
            globalInlineCount++;
        }
    }

    public boolean isClosure() {
        return isClosure;
    }

    public InlineCloneInfo cloneForInliningClosure(IRScope scopeBeingInlined) {
        InlineCloneInfo clone = new InlineCloneInfo(hostCFG, hostCFG.getScope(), scopeBeingInlined);

        clone.call = this.call;
        clone.callArgs = this.callArgs;
        clone.callReceiver = this.callReceiver;

        return clone;
    }

    public Operand getArg(int index) {
        // yield 1 -> { |a| } case
        if (canMapArgsStatically && isClosure && !(yieldArg instanceof Array)) return yieldArg;

        return index < getArgsCount() ? (isClosure ? ((Array)yieldArg).get(index) : callArgs[index]) : null;
    }

    public Operand getArg(int argIndex, boolean restOfArgArray) {
        if (!restOfArgArray) return getArg(argIndex);
        if (isClosure) throw new RuntimeException("Cannot get rest yield arg at inline time!");
        if (argIndex >= callArgs.length) return new Array();

        Operand[] tmp = new Operand[callArgs.length - argIndex];
        System.arraycopy(callArgs, argIndex, tmp, 0, callArgs.length - argIndex);

        return new Array(tmp);
    }

    public boolean canMapArgsStatically() {
        return canMapArgsStatically;
    }

    public Operand getArgs() {
        return isClosure ? yieldArg : argsArray;
    }

    public BasicBlock getRenamedBB(BasicBlock bb) {
        return bbRenameMap.get(bb);
    }

    public int getArgsCount() {
        return canMapArgsStatically ? (isClosure ? ((Array)yieldArg).size() : callArgs.length) : -1;
    }

    public Operand getCallClosure() {
        return call.getClosureArg(scope.getManager().getNil());
    }

    public Variable getCallResultVariable() {
        return call instanceof ResultInstr ? ((ResultInstr) call).getResult() : null;
    }

    public BasicBlock getOrCreateRenamedBB(BasicBlock bb) {
        BasicBlock renamedBB = getRenamedBB(bb);
        if (renamedBB == null) {
            renamedBB =  new BasicBlock(hostCFG, getRenamedLabel(bb.getLabel()));
            if (bb.isRescueEntry()) renamedBB.markRescueEntryBB();
            bbRenameMap.put(bb, renamedBB);
        }
        return renamedBB;
    }

    public IRScope getHostScope() {
        return getScope();
    }

    protected Label getRenamedLabelSimple(Label l) {
        return getHostScope().getNewLabel();
    }

    public Variable getRenamedSelfVariable(Variable self) {
        /* Note: evals make all this weird but our heuristics are such that we should not see the same callsite
         *   make a monocall across an eval (within an eval is fine).  If we ever cache pre-compiled evals we may
         *   end up breaking here.
         *
         * There are two closure types we will see while inlinine.
         *   1. A closure attached to the call we are inlining or exists within host scope itself. ( .... inline_me { foo }
         *         This will have same self as the host scope itself.
         *   2. A closure in the method we are inlining.
         *         That will the same self as the inlined methods scope
         */
        if (isClosure) {
            if (getScopeBeingInlined().getNearestTopLocalVariableScope() == getHostScope()) {
                return self;
            } else {
                return callReceiver;
            }
        } else { // method scope
            return callReceiver;
        }
    }

    protected Variable getRenamedVariableSimple(Variable v) {
        if (isClosure) {
            // when inlining a closure,
            // - local var depths are reduced by 1 (to move them to the host scope)
            // - tmp vars are reallocated in the host scope
            if (v instanceof LocalVariable) {
                LocalVariable lv = (LocalVariable) v;
                int depth = lv.getScopeDepth();
                return getHostScope().getLocalVariable(lv.getName(), depth > 1 ? depth - 1 : 0);
            }

            return getHostScope().createTemporaryVariable();
        }

        // METHOD_INLINE
        return getHostScope().getNewInlineVariable(inlineVarPrefix, v);
    }

    public IRScope getScopeBeingInlined() {
        return scopeBeingInlined;
    }

    public Variable getYieldResult() {
        return yieldResult;
    }

    public List getYieldSites() {
        return yieldSites;
    }

    public void recordYieldSite(BasicBlock bb, YieldInstr i) {
        yieldSites.add(new Tuple<BasicBlock, YieldInstr>(bb, i));
    }

    public void setupYieldArgsAndYieldResult(YieldInstr yi, BasicBlock yieldBB, int blockArityValue) {
        Operand yieldInstrArg = yi.getYieldArg();

        if ((yieldInstrArg == UndefinedValue.UNDEFINED) || blockArityValue == 0) {
            yieldArg = new Array(); // Zero-elt array
        } else if (yieldInstrArg instanceof Array) {
            yieldArg = yieldInstrArg;
            // 1:1 arg match
            if (((Array) yieldInstrArg).size() == blockArityValue) canMapArgsStatically = true;
        } else if (blockArityValue == 1 && yi.unwrapArray == false) {
            yieldArg = yieldInstrArg;
            canMapArgsStatically = true;
        } else {
            // SSS FIXME: The code below is not entirely correct.  We have to process 'yi.getYieldArg()' similar
            // to how InterpretedIRBlockBody (1.8 and 1.9 modes) processes it.  We may need a special instruction
            // that takes care of aligning the stars and bringing good fortune to arg yielder and arg receiver.
            IRScope callerScope   = getHostScope();
            Variable yieldArgArray = callerScope.createTemporaryVariable();
            yieldBB.addInstr(new ToAryInstr(yieldArgArray, yieldInstrArg));
            yieldArg = yieldArgArray;
        }

        yieldResult = yi.getResult();
    }

    // SSS FIXME: This is a copy of a method in instructions/calladapter/CallAdapter.java
    // Maybe move this is to a util/Helpers class?
    private static boolean containsSplat(Operand args[]) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Splat) return true;
        }

        return false;
    }
}