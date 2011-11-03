package org.jruby.compiler.ir.compiler_pass;

import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.IRModule;
import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.representations.BasicBlock;
import org.jruby.compiler.ir.instructions.CallInstr;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.operands.MethAddr;
import org.jruby.compiler.ir.representations.CFG;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public class InlineTest implements CompilerPass {

    private static final Logger LOG = LoggerFactory.getLogger("InlineTest");

    public final String methodToInline;

    public InlineTest(String methodToInline) {
        this.methodToInline = methodToInline;
    }

    public boolean isPreOrder()  {
        return true;
    }

    public void run(IRScope s) {
        if (!(s instanceof IRMethod)) return;

        IRMethod method = ((IRMethod) s);
        CFG cfg = method.cfg();

        IRModule m = s.getNearestModule();
        IRMethod mi = m.getInstanceMethod(methodToInline);

        /*
        // aggressive testing .. super class walking
        while ((mi == null) && (m instanceof IRClass)) {
        Operand sc = ((IRClass)m).superClass;
        if (!(sc instanceof ClassMetaObject)) break;
        m = (IRModule)((ClassMetaObject)sc).scope;
        mi = m.getInstanceMethod(methodToInline);
        }
         */

        // just a test .. dont bother if we dont have a match!
        if (mi == null) return;

        for (BasicBlock b : cfg.getBasicBlocks()) {
            for (Instr i : b.getInstrs()) {
                if (i instanceof CallInstr) {
                    CallInstr call = (CallInstr) i;
                    MethAddr addr = call.getMethodAddr();
                    if (methodToInline.equals(((MethAddr) addr).getName())) {
                        LOG.debug("Will be inlining method {} at callsite: {}", methodToInline, call);
                        method.inlineMethod(mi, b, call);
                        // Just inline once per scope -- this is a test after all!
                        // Because, the surrounding iterators will break with a concurrent modification exception if we proceed!
                        return;
                    }
                }
            }
        }
    }
    
}
