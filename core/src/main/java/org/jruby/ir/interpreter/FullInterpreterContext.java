package org.jruby.ir.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jruby.ir.IRFlags;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.dataflow.DataFlowProblem;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.ReceiveSelfInstr;
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
    private BasicBlock[] linearizedBBList;

    /** Map of name -> dataflow problem */
    private Map<String, DataFlowProblem> dataFlowProblems;

    /** What passes have been run on this scope? */
    private List<CompilerPass> executedPasses = new ArrayList<>();

    // FIXME: Perhaps abstract IC into interface of base class so we do not have a null instructions field here
    public FullInterpreterContext(IRScope scope, Instr[] instructions) {
        super(scope, null);

        cfg = buildCFG(instructions);
        linearizedBBList = CFGLinearizer.linearize(cfg);
    }


    private CFG buildCFG(Instr[] instructions) {
        CFG newCFG = new CFG(getScope());

        newCFG.build(instructions);

        return newCFG;
    }

    /** We plan on running this in full interpreted mode.  This will fixup ipc, rpc, and generate instr list */
    public void generateInstructionsForIntepretation() {
        boolean simple_method = getScope() instanceof IRMethod;

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
                if (simple_method && SimpleMethodInterpreterEngine.OPERATIONS.get(instr.getOperation()) == null) simple_method = false;
                if (!(instr instanceof ReceiveSelfInstr)) {
                    instr.setIPC(ipc);
                    newInstrs.add(instr);
                    ipc++;
                }
            }
        }

        if (simple_method) getScope().getFlags().add(IRFlags.SIMPLE_METHOD);

        cfg.getExitBB().getLabel().setTargetPC(ipc + 1);  // Exit BB ipc

        // System.out.println("SCOPE: " + getName());
        // System.out.println("INSTRS: " + cfg().toStringInstrs());

        Instr[] linearizedInstrArray = newInstrs.toArray(new Instr[newInstrs.size()]);

        // Pass 2: Use ipc info from previous to mark all linearized instrs rpc
        ipc = 0;
        for (BasicBlock b : getLinearizedBBList()) {
            BasicBlock rescuerBB = cfg.getRescuerBBFor(b);
            int rescuerPC = rescuerBB == null ? -1 : rescuerBB.getLabel().getTargetPC();
            for (Instr instr : b.getInstrs()) {
                // FIXME: If we did not omit instrs from previous pass, we could end up just doing
                // a size and for loop this n times instead of walking an examining each instr
                if (!(instr instanceof ReceiveSelfInstr)) {
                    linearizedInstrArray[ipc].setRPC(rescuerPC);
                    ipc++;
                }
            }
        }

        instructions = linearizedInstrArray;
        temporaryVariablecount = getScope().getTemporaryVariablesCount();
    }

    @Override
    public CFG getCFG() {
        return cfg;
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
}
