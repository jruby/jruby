package org.jruby.compiler.ir.representations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jruby.compiler.ir.IRExecutionScope;
import org.jruby.compiler.ir.IRClosure;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.compiler_pass.DominatorTreeBuilder;
import org.jruby.compiler.ir.instructions.CallInstr;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.operands.Label;

import org.jruby.compiler.ir.dataflow.DataFlowProblem;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public class CFGData {
    private static final Logger LOG = LoggerFactory.getLogger("CFG");

    public enum EdgeType {
        REGULAR,       // Any non-special edge.  Not really used.
        EXCEPTION,     // Edge to exception handling basic blocks
        FALL_THROUGH,  // Edge which is the natural fall through choice on a branch
        EXIT           // Edge to dummy exit BB
    }

    IRExecutionScope scope;   // Scope (method/closure) to which this cfg belongs

    CFG cfg = null;
    Map<String, DataFlowProblem> dfProbs;       // Map of name -> dataflow problem
    List<BasicBlock> linearizedBBList;  // Linearized list of bbs
    private Instr[] instrs;


    public CFGData(IRExecutionScope s) {
        scope = s;
        dfProbs = new HashMap<String, DataFlowProblem>();
        instrs = null;
    }
    
    // Dependencies

    public CFG cfg() {
        assert cfg != null: "Trying to access build before build started";
        return cfg;
    }
    
    public List<BasicBlock> linearization() {
        depends(cfg());
        
        assert linearizedBBList != null: "You have not run linearization";
        
        return linearizedBBList;
    }    
    
    protected void depends(Object obj) {
        assert obj != null: "Unsatisfied dependency and this depends() was set " +
                "up wrong.  Use depends(build()) not depends(build).";
    }

    public boolean bbIsProtected(BasicBlock b) {
        // No need to look in ensurerMap because (_bbEnsurerMap(b) != null) => (_bbResucerMap(b) != null)
        return (cfg().getRescuerBBFor(b) != null);
    }

    // SSS FIXME: Extremely inefficient
    public int getRescuerPC(Instr excInstr) {
        depends(cfg());
        
        for (BasicBlock b : linearizedBBList) {
            for (Instr i : b.getInstrs()) {
                if (i == excInstr) {
                    BasicBlock rescuerBB = cfg.getRescuerBBFor(b);
                    return (rescuerBB == null) ? -1 : rescuerBB.getLabel().getTargetPC();
                }
            }
        }

        // SSS FIXME: Cannot happen! Throw runtime exception
        LOG.error("Fell through looking for rescuer ipc for " + excInstr);
        return -1;
    }

    // SSS FIXME: Extremely inefficient
    public int getEnsurerPC(Instr excInstr) {
        depends(cfg());
        
        for (BasicBlock b : linearizedBBList) {
            for (Instr i : b.getInstrs()) {
                if (i == excInstr) {
                    BasicBlock ensurerBB = cfg.getEnsurerBBFor(b);
                    return (ensurerBB == null) ? -1 : ensurerBB.getLabel().getTargetPC();
                }
            }
        }

        // SSS FIXME: Cannot happen! Throw runtime exception
        LOG.error("Fell through looking for ensurer ipc for " + excInstr);
        return -1;
    }

    /* Add 'b' as a global ensure block that protects all unprotected blocks in this scope */
    public void addGlobalEnsureBlock(BasicBlock geb) {
        depends(cfg());
        
        cfg.addEdge(geb, cfg.getExitBB(), EdgeType.EXIT);
        
        for (BasicBlock basicBlock: cfg.getBasicBlocks()) {
            if (basicBlock != geb && !bbIsProtected(basicBlock)) {
                cfg.addEdge(basicBlock, geb, EdgeType.EXCEPTION);
                cfg.setRescuerBB(basicBlock, geb);
                cfg.setEnsurerBB(basicBlock, geb);
            }
        }
    }
    
    private void printError(String message) {
        LOG.error(message + "\nGraph:\n" + cfg() + "\nInstructions:\n" + toStringInstrs());
    }    

    public Instr[] prepareForInterpretation() {
        if (instrs != null) return instrs; // Already prepared

        try {
            buildLinearization(); // FIXME: compiler passes should have done this
            depends(linearization());
        } catch (RuntimeException e) {
            printError(e.getMessage());
            throw e;
        }

        // Set up a bb array that maps labels to targets -- just to make sure old code continues to work! 
        // ENEBO: Currently unused
        // setupFallThruMap();

        // Set up IPCs
        HashMap<Label, Integer> labelIPCMap = new HashMap<Label, Integer>();
        List<Label> labelsToFixup = new ArrayList<Label>();
        List<Instr> newInstrs = new ArrayList<Instr>();
        int ipc = 0;
        for (BasicBlock b : linearizedBBList) {
            labelIPCMap.put(b.getLabel(), ipc);
            labelsToFixup.add(b.getLabel());
            for (Instr i : b.getInstrs()) {
                newInstrs.add(i);
                ipc++;
            }
        }

        // Fix up labels
        for (Label l : labelsToFixup) {
            l.setTargetPC(labelIPCMap.get(l));
        }

        // Exit BB ipc
        cfg.getExitBB().getLabel().setTargetPC(ipc + 1);

        instrs = newInstrs.toArray(new Instr[newInstrs.size()]);
        return instrs;
    }

    public void inlineMethod(IRMethod method, BasicBlock basicBlock, CallInstr call) {
        depends(cfg());
        
        new CFGInliner(this, cfg).inlineMethod(method, basicBlock, call);
    }
    
    
    public void buildCFG(List<Instr> instructions) {
        CFG newBuild = new CFG(scope);
        newBuild.build(instructions);
        cfg = newBuild;
    }    

    public void buildDominatorTree(DominatorTreeBuilder builder) {
        depends(cfg());

        // FIXME: Add result from this build and add to CFG as a field, then add depends() for htings which use it.
        builder.buildDominatorTree(cfg, cfg.postOrderList(), cfg.getMaxNodeID());
    }
    
    public List<BasicBlock> buildLinearization() {
        if (linearizedBBList != null) return linearizedBBList; // Already linearized
        
        linearizedBBList = CFGLinearizer.linearize(cfg);
        
        return linearizedBBList;
    }

    public String toStringInstrs() {
        StringBuilder buf = new StringBuilder();        
        if (cfg != null) {
            for (BasicBlock b : cfg.getBasicBlocks()) {
                buf.append(b.toStringInstrs());
            }
            buf.append(cfg().toStringInstrs());
        } else {
            buf.append("CFG not built yet");
        }

        List<IRClosure> closures = scope.getClosures();
        if (!closures.isEmpty()) {
            buf.append("\n\n------ Closures encountered in this scope ------\n");
            for (IRClosure c : closures) {
                buf.append(c.toStringBody());
            }
            buf.append("------------------------------------------------\n");
        }

        return buf.toString();
    }

    public void setDataFlowSolution(String name, DataFlowProblem p) {
        dfProbs.put(name, p);
    }

    public DataFlowProblem getDataFlowSolution(String name) {
        return dfProbs.get(name);
    }

    public void splitCalls() {
        // FIXME: (Enebo) We are going to make a SplitCallInstr so this logic can be separate
        // from unsplit calls.  Comment out until new SplitCall is created.
//        for (BasicBlock b: getNodes()) {
//            List<Instr> bInstrs = b.getInstrs();
//            for (ListIterator<Instr> it = ((ArrayList<Instr>)b.getInstrs()).listIterator(); it.hasNext(); ) {
//                Instr i = it.next();
//                // Only user calls, not Ruby & JRuby internal calls
//                if (i.operation == Operation.CALL) {
//                    CallInstr call = (CallInstr)i;
//                    Operand   r    = call.getReceiver();
//                    Operand   m    = call.getMethodAddr();
//                    Variable  mh   = _scope.getNewTemporaryVariable();
//                    MethodLookupInstr mli = new MethodLookupInstr(mh, m, r);
//                    // insert method lookup at the right place
//                    it.previous();
//                    it.add(mli);
//                    it.next();
//                    // update call address
//                    call.setMethodAddr(mh);
//                }
//            }
//        }
//
//        List<IRClosure> closures = _scope.getClosures();
//        if (!closures.isEmpty()) {
//            for (IRClosure c : closures) {
//                c.getCFG().splitCalls();
//            }
//        }
    }
    
    @Override
    public String toString() {
        return "CFG[" + scope.getScopeName() + ":" + scope.getName() + "]";
    }
}
