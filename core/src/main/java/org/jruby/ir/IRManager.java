package org.jruby.ir;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.jruby.RubyInstanceConfig;
import org.jruby.ir.listeners.IRScopeListener;
import org.jruby.ir.listeners.InstructionsListener;
import org.jruby.ir.operands.BooleanLiteral;
import org.jruby.ir.operands.Nil;
import org.jruby.ir.passes.BasicCompilerPassListener;
import org.jruby.ir.passes.CompilerPass;
import org.jruby.ir.passes.CompilerPassListener;
import org.jruby.ir.passes.CompilerPassScheduler;

/**
 */
public class IRManager {
    public static String DEFAULT_COMPILER_PASSES = "OptimizeTempVarsPass,LocalOptimizationPass,LinearizeCFG";
    public static String DEFAULT_INLINING_COMPILER_PASSES = "LocalOptimizationPass";

    private int dummyMetaClassCount = 0;
    private final IRModuleBody classMetaClass = new IRMetaClassBody(this, null, getMetaClassName(), "", 0, null);
    private final IRModuleBody object = new IRClassBody(this, null, "Object", "", 0, null);
    private final Nil nil = new Nil();
    private final BooleanLiteral trueObject = new BooleanLiteral(true);
    private final BooleanLiteral falseObject = new BooleanLiteral(false);
    // Listeners for debugging and testing of IR
    private Set<CompilerPassListener> passListeners = new HashSet<CompilerPassListener>();
    private CompilerPassListener defaultListener = new BasicCompilerPassListener();

    private InstructionsListener instrsListener = null;
    private IRScopeListener irScopeListener = null;


    // FIXME: Eventually make these attrs into either a) set b) part of state machine
    private List<CompilerPass> compilerPasses = new ArrayList<CompilerPass>();
    private List<CompilerPass> inliningCompilerPasses = new ArrayList<CompilerPass>();

    // If true then code will not execute (see ir/ast tool)
    private boolean dryRun = false;

    public IRManager() {
        compilerPasses = CompilerPass.getPassesFromString(RubyInstanceConfig.IR_COMPILER_PASSES, DEFAULT_COMPILER_PASSES);
        inliningCompilerPasses = CompilerPass.getPassesFromString(RubyInstanceConfig.IR_COMPILER_PASSES, DEFAULT_INLINING_COMPILER_PASSES);
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean value) {
        this.dryRun = value;
    }

    public Nil getNil() {
        return nil;
    }

    public BooleanLiteral getTrue() {
        return trueObject;
    }

    public BooleanLiteral getFalse() {
        return falseObject;
    }

    public IRModuleBody getObject() {
        return object;
    }

    public CompilerPassScheduler schedulePasses() {
        CompilerPassScheduler scheduler = new CompilerPassScheduler() {
            private Iterator<CompilerPass> iterator;
            {
                this.iterator = compilerPasses.iterator();
            }

            @Override
            public Iterator<CompilerPass> iterator() {
                return this.iterator;
            }

        };
        return scheduler;
    }

    public List<CompilerPass> getCompilerPasses(IRScope scope) {
        return compilerPasses;
    }

    public List<CompilerPass> getInliningCompilerPasses(IRScope scope) {
        return inliningCompilerPasses;
    }

    public Set<CompilerPassListener> getListeners() {
        // FIXME: This is ugly but we want to conditionalize output based on JRuby module setting/unsetting
        if (RubyInstanceConfig.IR_COMPILER_DEBUG) {
            addListener(defaultListener);
        } else {
            removeListener(defaultListener);
        }

        return passListeners;
    }

    public InstructionsListener getInstructionsListener() {
        return instrsListener;
    }

    public IRScopeListener getIRScopeListener() {
        return irScopeListener;
    }

    public void addListener(CompilerPassListener listener) {
        passListeners.add(listener);
    }

    public void removeListener(CompilerPassListener listener) {
        passListeners.remove(listener);
    }

    public void addListener(InstructionsListener listener) {
        if (RubyInstanceConfig.IR_COMPILER_DEBUG || RubyInstanceConfig.IR_VISUALIZER) {
            if (instrsListener != null) {
                throw new RuntimeException("InstructionsListener is set and other are currently not allowed");
            }

            instrsListener = listener;
        }
    }

    public void removeListener(InstructionsListener listener) {
        if (instrsListener.equals(listener)) instrsListener = null;
    }

    public void addListener(IRScopeListener listener) {
        if (RubyInstanceConfig.IR_COMPILER_DEBUG || RubyInstanceConfig.IR_VISUALIZER) {
            if (irScopeListener != null) {
                throw new RuntimeException("IRScopeListener is set and other are currently not allowed");
            }

            irScopeListener = listener;
        }
    }

    public void removeListener(IRScopeListener listener) {
        if (irScopeListener.equals(listener)) irScopeListener = null;
    }

    public IRModuleBody getClassMetaClass() {
        return classMetaClass;
    }

    public String getMetaClassName() {
        return "<DUMMY_MC:" + dummyMetaClassCount++ + ">";
    }
}
