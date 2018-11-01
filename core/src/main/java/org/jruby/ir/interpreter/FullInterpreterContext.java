package org.jruby.ir.interpreter;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jruby.RubyInstanceConfig;
import org.jruby.ir.IRFlags;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.dataflow.DataFlowProblem;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.LabelInstr;
import org.jruby.ir.instructions.ReceiveSelfInstr;
import org.jruby.ir.instructions.Site;
import org.jruby.ir.passes.CompilerPass;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.ir.representations.CFG;
import org.jruby.ir.representations.CFGLinearizer;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

/**
 * Created by enebo on 2/27/15.
 */
public class FullInterpreterContext extends InterpreterContext {
    private CFG cfg;

    // Creation of this field will happen in generateInstructionsForInterpretation or during IRScope.prepareForCompilation.
    // FIXME: At some point when we relinearize after running another phase of passes we should document that here to know how this field is changed
    private BasicBlock[] linearizedBBList = null;

    /** Map of name -> dataflow problem */
    private Map<String, DataFlowProblem> dataFlowProblems;

    /** What passes have been run on this scope? */
    private List<CompilerPass> executedPasses = new ArrayList<>();

    // For duplicate()
    public FullInterpreterContext(IRScope scope, CFG cfg, BasicBlock[] linearizedBBList) {
        super(scope, (List<Instr>) null);

        this.cfg = cfg;
        this.linearizedBBList = linearizedBBList;
    }

    // FIXME: Perhaps abstract IC into interface of base class so we do not have a null instructions field here
    public FullInterpreterContext(IRScope scope, Instr[] instructions) {
        super(scope, (List<Instr>)null);

        cfg = buildCFG(instructions);
    }

    /**
     * have this interpretercontext fully built?  This is slightly more complicated than this simple check, but it
     * should work.  In -X-C full builds we linearize at the beginning of our generateInstructionsForInterpretation
     * method.  Last thing we do essentially is set instructions to be something.  For JIT builds last thing we
     * need to check is whether we have linearized the BB list.
     */
    @Override
    public boolean buildComplete() {
        return linearizedBBList != null;
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
        return getScope().getFlags().contains(IRFlags.HAS_EXPLICIT_CALL_PROTOCOL);
    }

    @Override
    public boolean pushNewDynScope() {
        return !getScope().getFlags().contains(IRFlags.DYNSCOPE_ELIMINATED) && !reuseParentDynScope();
    }

    @Override
    public boolean popDynScope() {
        return pushNewDynScope() || reuseParentDynScope();
    }

    @Override
    public boolean reuseParentDynScope() {
        return getScope().getFlags().contains(IRFlags.REUSE_PARENT_DYNSCOPE);
    }

    /** We plan on running this in full interpreted mode.  This will fixup ipc, rpc, and generate instr list */
    public void generateInstructionsForInterpretation() {
        linearizeBasicBlocks();

        // Pass 1. Set up IPCs for labels and instructions and build linear instr list
        List<Instr> newInstrs = new ArrayList<>();
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
        rescueIPCs = new int[2 * basicBlocks.length];

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

        instructions = linearizedInstrArray;
        temporaryVariablecount = getScope().getTemporaryVariablesCount();

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
                i.computeScopeFlags(getScope());
            }
        }
    }

    public Map<String, DataFlowProblem> getDataFlowProblems() {
        if (dataFlowProblems == null) dataFlowProblems = new HashMap<>();
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

            return new FullInterpreterContext(getScope(), newCFG, newLinearizedBBList);
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
}
