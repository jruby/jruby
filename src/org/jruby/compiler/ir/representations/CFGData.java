package org.jruby.compiler.ir.representations;

import java.util.List;

import org.jruby.compiler.ir.IRExecutionScope;
import org.jruby.compiler.ir.IRClosure;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.compiler_pass.DominatorTreeBuilder;
import org.jruby.compiler.ir.instructions.CallInstr;
import org.jruby.compiler.ir.instructions.Instr;

import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public class CFGData {
    private static final Logger LOG = LoggerFactory.getLogger("CFG");

    IRExecutionScope scope;   // Scope (method/closure) to which this cfg belongs

    CFG cfg = null;

    public CFGData(IRExecutionScope s) {
        scope = s;
    }
    
    // Dependencies

    public CFG cfg() {
        assert cfg != null: "Trying to access build before build started";
        return cfg;
    }   
    
    protected void depends(Object obj) {
        assert obj != null: "Unsatisfied dependency and this depends() was set " +
                "up wrong.  Use depends(build()) not depends(build).";
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
