package org.jruby.compiler.ir.compiler_pass;

import org.jruby.RubyModule;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.instructions.CallInstr;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.operands.MethAddr;
import org.jruby.compiler.ir.representations.BasicBlock;
import org.jruby.compiler.ir.representations.CFG;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public class InlineTest extends CompilerPass {
    public static String[] NAMES = new String[] { "inline_test" };
    
    private static final Logger LOG = LoggerFactory.getLogger("InlineTest");
    
    public final String methodToInline;

    public InlineTest(String methodToInline) {
        this.methodToInline = methodToInline;
    }
    
    public String getLabel() {
        return "Inline Test (" + methodToInline + ")";
    }

    public boolean isPreOrder()  {
        return true;
    }
    
    // ENEBO - FIXME: This is fragile and will not work on non-interpreted IR
    private IRScope getIRMethod(IRScope s) {
        IRScope m = s.getNearestModuleReferencingScope();

        if (m == null) return null;
        
        RubyModule realModule = m.getStaticScope().getModule();
        
        if (realModule == null) return null;
        
        DynamicMethod realMethod = realModule.getMethods().get(methodToInline);
        
        if (!(realMethod instanceof InterpretedIRMethod)) return null;

        return ((InterpretedIRMethod) realMethod).getIRMethod();
    }

    public Object execute(IRScope s, Object... data) {
        if (!(s instanceof IRMethod)) return null;

        IRScope mi = getIRMethod(s);

        // Cannot inline something not IR
        // FIXME: Add logging indicating aborted inline attempt
        // just a test .. dont bother if we dont have a match!
        if (mi == null) return null;

        IRMethod method = ((IRMethod) s);
        CFG cfg = method.cfg();
        for (BasicBlock b : cfg.getBasicBlocks()) {
            for (Instr i : b.getInstrs()) {
                if (i instanceof CallInstr) {
                    CallInstr call = (CallInstr) i;
                    MethAddr addr = call.getMethodAddr();
                    if (methodToInline.equals(((MethAddr) addr).getName())) {
                        LOG.debug("Will be inlining method {} at callsite: {}", methodToInline, call);
                        method.inlineMethod(mi, null, 0, b, call);
                        // Just inline once per scope -- this is a test after all!
                        // Because, the surrounding iterators will break with a concurrent modification exception if we proceed!
                        return null;
                    }
                }
            }
        }
        
        return true;
    }
    
}
