package org.jruby.ir.interpreter;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jruby.ir.IRClosure;
import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScopeType;
import org.jruby.ir.dataflow.DataFlowProblem;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.LabelInstr;
import org.jruby.ir.instructions.ReceiveSelfInstr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.instructions.Site;
import org.jruby.ir.operands.Fixnum;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.TemporaryBooleanVariable;
import org.jruby.ir.operands.TemporaryClosureVariable;
import org.jruby.ir.operands.TemporaryFixnumVariable;
import org.jruby.ir.operands.TemporaryFloatVariable;
import org.jruby.ir.operands.TemporaryLocalReplacementVariable;
import org.jruby.ir.operands.TemporaryLocalVariable;
import org.jruby.ir.operands.TemporaryVariable;
import org.jruby.ir.operands.TemporaryVariableType;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.passes.CompilerPass;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.ir.representations.CFG;
import org.jruby.ir.representations.CFGLinearizer;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

import static org.jruby.ir.IRFlags.BINDING_HAS_ESCAPED;

/**
 * Created by enebo on 2/27/15.
 */
public class FullInterpreterContext extends InterpreterContext {
    private final CFG cfg;

    // Creation of this field will happen in generateInstructionsForInterpretation or during IRScope.prepareForCompilation.
    // FIXME: At some point when we relinearize after running another phase of passes we should document that here to know how this field is changed
    private BasicBlock[] linearizedBBList = null;

    /** Map of name -> dataflow problem */
    private Map<String, DataFlowProblem> dataFlowProblems;

    /** What passes have been run on this scope? */
    private final List<CompilerPass> executedPasses = new ArrayList<>(4);


    /** Local variables defined in this scope */
    private Set<LocalVariable> definedLocalVars;

    /** Local variables used in this scope */
    private Set<LocalVariable> usedLocalVars;

    // FIXME: When inlining these unboxed indexes and even defined/used can be cloned as starting point?
    public int floatVariableIndex = -1;
    public int fixnumVariableIndex = -1;
    public int booleanVariableIndex = -1;

    // For duplicate()
    public FullInterpreterContext(IRScope scope, CFG cfg, BasicBlock[] linearizedBBList, int temporaryVariableCount, EnumSet<IRFlags> flags) {
        super(scope, (List<Instr>) null, temporaryVariableCount, flags);

        this.cfg = cfg;
        this.linearizedBBList = linearizedBBList;
    }

    // FIXME: Perhaps abstract IC into interface of base class so we do not have a null instructions field here
    public FullInterpreterContext(IRScope scope, Instr[] instructions, int temporaryVariableCount, EnumSet<IRFlags> flags) {
        super(scope, (List<Instr>)null, temporaryVariableCount, flags);

        cfg = buildCFG(instructions);
    }

    public BasicBlock[] linearizeBasicBlocks() {
        linearizedBBList = CFGLinearizer.linearize(cfg);
        return linearizedBBList;
    }

    private CFG buildCFG(Instr[] instructions) {
        CFG newCFG = new CFG(getScope());

        newCFG.build(instructions);

        return newCFG;
    }

    @Override
    public boolean hasExplicitCallProtocol() {
        return hasExplicitCallProtocol;
    }

    public boolean needsBinding() {
        return reuseParentDynScope() || !isDynamicScopeEliminated();
    }

    /** We plan on running this in full interpreted mode.  This will fixup ipc, rpc, and generate instr list */
    public void generateInstructionsForInterpretation() {
        linearizeBasicBlocks();

        // Pass 1. Set up IPCs for labels and instructions and build linear instr list
        List<Instr> newInstrs = new ArrayList<>(4);
        int ipc = 0;
        for (BasicBlock b: getLinearizedBBList()) {
            // All same-named labels must be same Java instance for this to work or we would need
            // to examine all Label operands and update this as well which would be expensive.
            b.getLabel().setTargetPC(ipc);

            List<Instr> bbInstrs = b.getInstrs();
            int bbInstrsLength = bbInstrs.size();
            // FIXME: Can be replaced with System.arrayCopy to avoid call newInstrs.add a zillion times
            for (int i = 0; i < bbInstrsLength; i++) {
                Instr instr = bbInstrs.get(i);
                if (!(instr instanceof ReceiveSelfInstr)) {
                    if (instr instanceof LabelInstr) ((LabelInstr) instr).getLabel().setTargetPC(ipc);
                    newInstrs.add(instr);
                    ipc++;
                }
            }
        }

        cfg.getExitBB().getLabel().setTargetPC(ipc + 1);  // Exit BB ipc

        Instr[] linearizedInstrArray = newInstrs.toArray(new Instr[newInstrs.size()]);

        BasicBlock[] basicBlocks = getLinearizedBBList();
        int[] rescueIPCs = new int[2 * basicBlocks.length];

        // Pass 2: Use ipc info from previous to mark all linearized instrs rpc
        ipc = 0;
        for (int i = 0; i < basicBlocks.length; i++) {
            BasicBlock bb = basicBlocks[i];
            BasicBlock rescuerBB = cfg.getRescuerBBFor(bb);
            int rescuerPC = rescuerBB == null ? -1 : rescuerBB.getLabel().getTargetPC();
            rescueIPCs[i * 2] = ipc + bb.getInstrs().size();
            rescueIPCs[i * 2 + 1] = rescuerPC;

            for (Instr instr : bb.getInstrs()) {
                // FIXME: If we did not omit instrs from previous pass, we could end up just doing
                // a size and for loop this n times instead of walking an examining each instr
                if (!(instr instanceof ReceiveSelfInstr)) {
                    ipc++;
                } else {
                    rescueIPCs[i * 2]--;
                }
            }
        }

        this.rescueIPCs = rescueIPCs;
        this.instructions = linearizedInstrArray;

        // System.out.println("SCOPE: " + getScope().getId());
        // System.out.println("INSTRS: " + cfg.toStringInstrs());
    }

    @Override
    public CFG getCFG() {
        return cfg;
    }

    @Override
    public void computeScopeFlagsFromInstructions() {
        for (BasicBlock b: cfg.getBasicBlocks()) {
            for (Instr i: b.getInstrs()) {
                i.computeScopeFlags(getScope(), getFlags());
            }
        }
    }

    public Map<String, DataFlowProblem> getDataFlowProblems() {
        if (dataFlowProblems == null) dataFlowProblems = new HashMap<>(1);
        return dataFlowProblems;
    }

    public List<CompilerPass> getExecutedPasses() {
        return executedPasses;
    }

    // FIXME: Potentially remove
    public BasicBlock[] getLinearizedBBList() {
        return linearizedBBList;
    }

    @Override
    public String toStringInstrs() {
        return "\nCFG:\n" + cfg.toStringGraph() + "\nInstructions:\n" + cfg.toStringInstrs();
    }

    public String toStringLinearized() {
        StringBuilder buf = new StringBuilder();

        for (BasicBlock bb: getLinearizedBBList()) {
            buf.append(bb + bb.toStringInstrs());
        }

        return buf.toString();
    }

    public FullInterpreterContext duplicate() {
        try {
            CFG newCFG = cfg.clone(new SimpleCloneInfo(getScope(), false, true), getScope());
            BasicBlock[] newLinearizedBBList = new BasicBlock[linearizedBBList.length];

            for (int i = 0; i < linearizedBBList.length; i++) {
                newLinearizedBBList[i] = newCFG.getBBForLabel(linearizedBBList[i].getLabel());
            }

            return new FullInterpreterContext(getScope(), newCFG, newLinearizedBBList, temporaryVariableCount, getFlags());
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }


    public int determineRPC(int ipc) {
        int length = rescueIPCs.length;
        for (int i = 0; i + 1 < length; i += 2) {
            if (ipc <= rescueIPCs[i]) return rescueIPCs[i + 1];
        }

        throw new RuntimeException("BUG: no RPC found for " + getFileName() + ":" + getName() + ":" + ipc);
    }

    public BasicBlock findBasicBlockOf(long callsiteId) {
        for (BasicBlock basicBlock: linearizeBasicBlocks()) {
            for (Instr instr: basicBlock.getInstrs()) {
                if (instr instanceof Site) {
                    Site site = (Site) instr;

                    if (site.getCallSiteId() == callsiteId) return basicBlock;
                }
            }
        }

        throw new RuntimeException("Bug: Looking for callsiteId: " + callsiteId + " in " + this);
    }

    /**
     * Get all variables referenced by this scope.
     */
    public Set<LocalVariable> getUsedLocalVariables() {
        return usedLocalVars;
    }

    public void setUpUseDefLocalVarMaps() {
        definedLocalVars = new HashSet<>(1);
        usedLocalVars = new HashSet<>(1);
        for (BasicBlock bb : getCFG().getBasicBlocks()) {
            for (Instr i : bb.getInstrs()) {
                for (Variable v : i.getUsedVariables()) {
                    if (v instanceof LocalVariable) usedLocalVars.add((LocalVariable) v);
                }

                if (i instanceof ResultInstr) {
                    Variable v = ((ResultInstr) i).getResult();

                    if (v instanceof LocalVariable && !((LocalVariable)v).isOuterScopeVar()) {
                        definedLocalVars.add((LocalVariable) v);
                    }
                }
            }
        }

        for (IRClosure cl : getScope().getClosures()) {
            cl.getFullInterpreterContext().setUpUseDefLocalVarMaps();
        }
    }

    public boolean usesLocalVariable(Variable v) {
        if (usedLocalVars == null) setUpUseDefLocalVarMaps();
        if (usedLocalVars.contains(v)) return true;

        for (IRClosure cl : getScope().getClosures()) {
            if (cl.getFullInterpreterContext().usesLocalVariable(v)) return true;
        }

        return false;
    }

    public boolean definesLocalVariable(Variable v) {
        if (definedLocalVars == null) setUpUseDefLocalVarMaps();
        if (definedLocalVars.contains(v)) return true;

        for (IRClosure cl : getScope().getClosures()) {
            if (cl.getFullInterpreterContext().definesLocalVariable(v)) return true;
        }

        return false;
    }

    protected void initialize() {
        // no initialize, avoid parent
    }

    public TemporaryVariable createTemporaryVariable() {
        temporaryVariableCount++;

        if (getScope().getScopeType() == IRScopeType.CLOSURE) {
            return new TemporaryClosureVariable(((IRClosure) getScope()).closureId, temporaryVariableCount - 1);
        } else {
            return getScope().getManager().newTemporaryLocalVariable(temporaryVariableCount - 1);
        }
    }

    public TemporaryLocalVariable getNewTemporaryVariableFor(LocalVariable var) {
        temporaryVariableCount++;
        return new TemporaryLocalReplacementVariable(var.getId(), temporaryVariableCount - 1);
    }

    public TemporaryLocalVariable getNewUnboxedVariable(Class type) {
        TemporaryVariableType varType;
        if (type == Float.class) {
            varType = TemporaryVariableType.FLOAT;
        } else if (type == Fixnum.class) {
            varType = TemporaryVariableType.FIXNUM;
        } else if (type == java.lang.Boolean.class) {
            varType = TemporaryVariableType.BOOLEAN;
        } else if (type == java.lang.Integer.class) {
            varType = TemporaryVariableType.INT;
        } else {
            varType = TemporaryVariableType.LOCAL;
        }
        return getNewTemporaryVariable(varType);
    }

    // BUILD + FULL
    public TemporaryLocalVariable getNewTemporaryVariable(TemporaryVariableType type) {
        switch (type) {
            case FLOAT: {
                floatVariableIndex++;
                return new TemporaryFloatVariable(floatVariableIndex);
            }
            case FIXNUM: {
                fixnumVariableIndex++;
                return new TemporaryFixnumVariable(fixnumVariableIndex);
            }
            case BOOLEAN: {
                booleanVariableIndex++;
                return new TemporaryBooleanVariable(booleanVariableIndex);
            }
            case LOCAL: {
                return getScope().getManager().newTemporaryLocalVariable(temporaryVariableCount - 1);
            }
            // FIXME: TemporaryIntegerVariable is being stored boxed since the primitive temp arrays are not wired up
            case INT: {
                return getScope().getManager().newTemporaryIntVariable(temporaryVariableCount - 1);
            }
        }

        throw new RuntimeException("Invalid temporary variable being alloced in this scope: " + type);
    }

    public boolean needsFrame() {
        boolean bindingHasEscaped = bindingHasEscaped();
        boolean requireFrame = bindingHasEscaped || getScope().usesEval() || getScope().usesZSuper() || getScope().canCaptureCallersBinding();

        for (IRFlags flag : getFlags()) {
            switch (flag) {
                case BINDING_HAS_ESCAPED:
                case REQUIRES_LASTLINE:
                case REQUIRES_BACKREF:
                case REQUIRES_VISIBILITY:
                case REQUIRES_BLOCK:
                case REQUIRES_SELF:
                case REQUIRES_METHODNAME:
                case REQUIRES_CLASS:
                    requireFrame = true;
            }
        }

        return requireFrame;
    }

    public boolean bindingHasEscaped() {
        return getFlags().contains(BINDING_HAS_ESCAPED);
    }

    public boolean needsOnlyBackref() {
        if (getScope().usesEval() || getScope().usesZSuper() || getScope().canCaptureCallersBinding()) return false;

        boolean backrefSeen = false;
        for (IRFlags flag : getFlags()) {
            switch (flag) {
                case BINDING_HAS_ESCAPED:
                case REQUIRES_LASTLINE:
                case REQUIRES_VISIBILITY:
                case REQUIRES_BLOCK:
                case REQUIRES_SELF:
                case REQUIRES_METHODNAME:
                case REQUIRES_CLASS:
                    return false;
                case REQUIRES_BACKREF:
                    backrefSeen = true;
                    break;
            }
        }

        return backrefSeen;
    }
}
