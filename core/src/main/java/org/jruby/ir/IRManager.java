package org.jruby.ir;

import java.util.EnumSet;
import org.jruby.RubyInstanceConfig;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.listeners.IRScopeListener;
import org.jruby.ir.listeners.InstructionsListener;
import org.jruby.ir.operands.*;
import org.jruby.ir.operands.Boolean;
import org.jruby.ir.passes.BasicCompilerPassListener;
import org.jruby.ir.passes.CompilerPass;
import org.jruby.ir.passes.CompilerPassListener;
import org.jruby.ir.passes.CompilerPassScheduler;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.jruby.ir.passes.DeadCodeElimination;
import org.jruby.ir.passes.OptimizeDelegationPass;
import org.jruby.ir.passes.OptimizeDynScopesPass;
import org.jruby.ir.passes.OptimizeTempVarsPass;

import static org.jruby.ir.IRFlags.RECEIVES_CLOSURE_ARG;
import static org.jruby.ir.IRFlags.REQUIRES_DYNSCOPE;

public class IRManager {
    public static final String SAFE_COMPILER_PASSES = "";
    public static final String DEFAULT_BUILD_PASSES = "LocalOptimizationPass";
    public static final String DEFAULT_JIT_PASSES = "LocalOptimizationPass,OptimizeDelegationPass,DeadCodeElimination,AddLocalVarLoadStoreInstructions,OptimizeDynScopesPass,AddCallProtocolInstructions,EnsureTempsAssigned";
    public static final String DEFAULT_INLINING_COMPILER_PASSES = "LocalOptimizationPass";

    private final CompilerPass deadCodeEliminationPass = new DeadCodeElimination();
    private final CompilerPass optimizeDynScopesPass = new OptimizeDynScopesPass();
    private final CompilerPass optimizeDelegationPass = new OptimizeDelegationPass();

    private int dummyMetaClassCount = 0;
    private final IRModuleBody object = new IRClassBody(this, null, "Object", "", 0, null);
    private final Nil nil = new Nil();
    private final Boolean tru = new Boolean(true);
    private final Boolean fals = new Boolean(false);

    // Listeners for debugging and testing of IR
    private Set<CompilerPassListener> passListeners = new HashSet<CompilerPassListener>();
    private CompilerPassListener defaultListener = new BasicCompilerPassListener();

    private InstructionsListener instrsListener = null;
    private IRScopeListener irScopeListener = null;


    // FIXME: Eventually make these attrs into either a) set b) part of state machine
    private List<CompilerPass> compilerPasses;
    private List<CompilerPass> inliningCompilerPasses;
    private List<CompilerPass> jitPasses;
    private List<CompilerPass> safePasses;
    private final RubyInstanceConfig config;

    // If true then code will not execute (see ir/ast tool)
    private boolean dryRun = false;

    public IRManager(RubyInstanceConfig config) {
        this.config = config;
        compilerPasses = CompilerPass.getPassesFromString(RubyInstanceConfig.IR_COMPILER_PASSES, DEFAULT_BUILD_PASSES);
        inliningCompilerPasses = CompilerPass.getPassesFromString(RubyInstanceConfig.IR_COMPILER_PASSES, DEFAULT_INLINING_COMPILER_PASSES);
        jitPasses = CompilerPass.getPassesFromString(RubyInstanceConfig.IR_JIT_PASSES, DEFAULT_JIT_PASSES);
        safePasses = CompilerPass.getPassesFromString(null, SAFE_COMPILER_PASSES);
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

    public org.jruby.ir.operands.Boolean getTrue() {
        return tru;
    }

    public org.jruby.ir.operands.Boolean getFalse() {
        return fals;
    }

    public IRModuleBody getObject() {
        return object;
    }

    public CompilerPassScheduler schedulePasses() {
        return schedulePasses(compilerPasses);
    }

    public static CompilerPassScheduler schedulePasses(final List<CompilerPass> passes) {
        CompilerPassScheduler scheduler = new CompilerPassScheduler() {
            private Iterator<CompilerPass> iterator;
            {
                this.iterator = passes.iterator();
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

    public List<CompilerPass> getJITPasses(IRScope scope) {
        return jitPasses;
    }

    public List<CompilerPass> getSafePasses(IRScope scope) {
        return safePasses;
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

    public String getMetaClassName() {
        return "<DUMMY_MC:" + dummyMetaClassCount++ + ">";
    }

    private TemporaryLocalVariable[] temporaryLocalVariables = new TemporaryLocalVariable[1600];

    protected TemporaryLocalVariable[] growTemporaryVariablePool(int index) {
        int newLength = index * 2;
        TemporaryLocalVariable[] newPool = new TemporaryLocalVariable[newLength];

        System.arraycopy(temporaryLocalVariables, 0, newPool, 0, temporaryLocalVariables.length);
        temporaryLocalVariables = newPool;
        return newPool;
    }

    // FIXME: Consider IRBuilder not using so many temporary variables for literal initialization.  This is the
    // vast majority of high index temp variables.
    /**
     * Temporary local variables are immutable and always start from a low index value and increment up
     * to a higher index value per scope.  So we can share these and store the ones in a simple list.  If
     * hard pinning is ever an issue we can periodically evict the list and start over at the cost of more
     * live objects but this list cache reduces a simple empty Rails app console from over 140K instances
     * to about 1200 instances.
     *
     */
    public TemporaryLocalVariable newTemporaryLocalVariable(int index) {
        if (index >= temporaryLocalVariables.length-1) growTemporaryVariablePool(index);

        TemporaryLocalVariable tempVar = temporaryLocalVariables[index];

        if (tempVar == null) {
            tempVar = new TemporaryLocalVariable(index);
            temporaryLocalVariables[index] = tempVar;
        }

        return tempVar;
    }

    public Instr[] optimizeTemporaryVariablesIfEnabled(IRScope scope, Instr[] instrs) {
        // FIXME: Make this check ir.passes and not run if ir.passes is set and does not contain opttempvars.
        return OptimizeTempVarsPass.optimizeTmpVars(scope, instrs);
    }

    /**
     * For scopes that don't require a dynamic scope we can run DCE and some other passes which cannot
     * be stymied by escaped bindings.
     */
    protected void optimizeIfSimpleScope(IRScope scope) {
        // We cannot pick the passes if we want an explicit set to run.
        if (RubyInstanceConfig.IR_COMPILER_PASSES != null) return;

        EnumSet<IRFlags> flags = scope.getFlags();

        if (!scope.isUnsafeScope() && !flags.contains(REQUIRES_DYNSCOPE)) {
            if (flags.contains(RECEIVES_CLOSURE_ARG)) optimizeDelegationPass.run(scope);
            deadCodeEliminationPass.run(scope);
            optimizeDynScopesPass.run(scope);
        }
    }

    public RubyInstanceConfig getInstanceConfig() {
        return config;
    }
}
