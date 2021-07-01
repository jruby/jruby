package org.jruby.ir;

import org.jruby.ParseResult;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.RubySymbol;
import org.jruby.compiler.Compilable;
import org.jruby.ext.coverage.CoverageData;
import org.jruby.ir.instructions.*;
import org.jruby.ir.interpreter.FullInterpreterContext;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.operands.*;
import org.jruby.ir.passes.*;
import org.jruby.ir.persistence.IRWriter;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.ir.transformations.inlining.CFGInliner;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;
import org.jruby.ir.util.IGVDumper;
import org.jruby.parser.StaticScope;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.jruby.runtime.ThreadContext;
import org.jruby.util.ByteList;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import static org.jruby.ir.IRFlags.*;

/**
 * Right now, this class abstracts the following execution scopes:
 * Method, Closure, Module, Class, MetaClass
 * Top-level Script, and Eval Script
 *
 * In the compiler-land, IR versions of these scopes encapsulate only as much
 * information as is required to convert Ruby code into equivalent Java code.
 *
 * But, in the non-compiler land, there will be a corresponding java object for
 * some of these scopes which encapsulates the runtime semantics and data needed
 * for implementing them.  In the case of Module, Class, MetaClass, and Method,
 * they also happen to be instances of the corresponding Ruby classes -- so,
 * in addition to providing code that help with this specific ruby implementation,
 * they also have code that let them behave as ruby instances of their corresponding
 * classes.
 *
 * Examples:
 * - the runtime class object might have refs. to the runtime method objects.
 * - the runtime method object might have a slot for a heap frame (for when it
 *   has closures that need access to the method's local variables), it might
 *   have version information, it might have references to other methods that
 *   were optimized with the current version number, etc.
 * - the runtime closure object will have a slot for a heap frame (for when it
 *   has closures within) and might get reified as a method in the java land
 *   (but inaccessible in ruby land).  So, passing closures in Java land might
 *   be equivalent to passing around the method handles.
 *
 * and so on ...
 */
public abstract class IRScope implements ParseResult {
    public static final Logger LOG = LoggerFactory.getLogger(IRScope.class);

    private static final Collection<IRClosure> NO_CLOSURES = Collections.EMPTY_LIST;

    private static final AtomicInteger globalScopeCount = new AtomicInteger();

    /** Unique global scope id */
    private final int scopeId;

    /** Starting line for this scope's definition */
    private final int lineNumber;

    /** Lexical parent scope */
    private final IRScope lexicalParent;

    /** Parser static-scope that this IR scope corresponds to */
    private final StaticScope staticScope;

    private final IRManager manager;

    /** Name */
    private ByteList name;

    /** List of (nested) closures in this scope */
    private List<IRClosure> nestedClosures;

    // Index values to guarantee we don't assign same internal index twice
    protected int nextClosureIndex;

    // List of all scopes this scope contains lexically.  This is not used
    // for execution, but is used during dry-runs for debugging.
    private final List<IRScope> lexicalChildren = Collections.synchronizedList(new ArrayList<>());

    /** Startup interpretation depends on this */
    protected InterpreterContext interpreterContext;

    /** -X-C full interpretation OR JIT depends on this */
    protected FullInterpreterContext fullInterpreterContext;

    /** Speculatively optimized code */
    protected FullInterpreterContext optimizedInterpreterContext;

    /** Keeps track of types of prefix indexes for variables and labels */
    private int nextLabelIndex = 0;

    Map<RubySymbol, LocalVariable> localVars;

    private boolean alreadyHasInline;
    private String inlineFailed;
    public Compilable compilable;

    private int coverageMode;
    // At least until we change the design all of these state fields are true from IRBuild forward.  With IR
    // optimization passes it is incredibly unlikely any of these could ever be unset anyways; So this is not
    // a poor list of 'truisms' for this Scope.
    private boolean hasBreakInstructions;
    private boolean hasLoops;
    private boolean hasNonLocalReturns;
    private boolean receivesClosureArg;
    private boolean receivesKeywordArgs;
    private boolean accessesParentsLocalVariables;
    private boolean maybeUsingRefinements;
    private boolean canCaptureCallersBinding;
    private boolean canReceiveBreaks;  // may receive a break during execution (from itself of child scope).
    private boolean canReceiveNonLocalReturns;
    private boolean usesSuper;
    private boolean usesZSuper;
    private boolean needsCodeCoverage;
    private boolean usesEval;

    // Used by cloning code for inlining
    protected IRScope(IRScope s, IRScope lexicalParent) {
        this.lexicalParent = lexicalParent;
        this.manager = s.manager;
        this.lineNumber = s.lineNumber;
        this.staticScope = s.staticScope;
        this.nextClosureIndex = s.nextClosureIndex;
        this.interpreterContext = null;
        this.coverageMode = CoverageData.NONE;
        this.localVars = new HashMap<>(s.localVars);
        this.scopeId = globalScopeCount.getAndIncrement();

        setupLexicalContainment();

        if (staticScope != null && !(this instanceof IRScriptBody) && !(this instanceof IREvalScript)) {
            staticScope.setFile(getFile());
        }
    }

    public IRScope(IRManager manager, IRScope lexicalParent, ByteList name, int lineNumber, StaticScope staticScope) {
        this(manager, lexicalParent, name, lineNumber, staticScope, CoverageData.NONE);
    }

    public IRScope(IRManager manager, IRScope lexicalParent, ByteList name, int lineNumber, StaticScope staticScope, int coverageMode) {
        this.manager = manager;
        this.lexicalParent = lexicalParent;
        this.name = name;
        this.lineNumber = lineNumber;
        this.staticScope = staticScope;
        this.nextClosureIndex = 0;
        this.interpreterContext = null;

        this.coverageMode = coverageMode;

        // We only can compute this once since 'module X; using A; class B; end; end' vs
        // 'module X; class B; using A; end; end'.  First case B can see refinements and in second it cannot.
        if (parentMaybeUsingRefinements()) setIsMaybeUsingRefinements();

        this.localVars = new HashMap<>(1);
        this.scopeId = globalScopeCount.getAndIncrement();

        setupLexicalContainment();
    }

    private void setupLexicalContainment() {
        if (lexicalParent != null) lexicalParent.addChildScope(this);
    }

    public int getScopeId() {
        return scopeId;
    }

    @Override
    public int hashCode() {
        return scopeId;
    }

    @Override
    public boolean equals(Object other) {
        return (other != null) && (getClass() == other.getClass()) && (scopeId == ((IRScope) other).scopeId);
    }

    protected synchronized void addChildScope(IRScope scope) {
        lexicalChildren.add(scope);
    }

    public synchronized List<IRScope> getLexicalScopes() {
        return lexicalChildren;
    }

    public void addClosure(IRClosure closure) {
        if (nestedClosures == null) nestedClosures = new ArrayList<>(1);
        nestedClosures.add(closure);
    }

    public void removeClosure(IRClosure closure) {
        if (nestedClosures != null) nestedClosures.remove(closure);
    }

    public Label getNewLabel(String prefix) {
        return new Label(prefix, nextLabelIndex++);
    }

    public Label getNewLabel() {
        return getNewLabel("LBL");
    }

    public Collection<IRClosure> getClosures() {
        return nestedClosures == null ? NO_CLOSURES : nestedClosures;
    }

    public IRManager getManager() {
        return manager;
    }

    public void setIsMaybeUsingRefinements() {
        maybeUsingRefinements = true;
    }

    public boolean parentMaybeUsingRefinements() {
        for (IRScope s = this; s != null; s = s.getLexicalParent()) {
            if (s.maybeUsingRefinements()) return true;

            // Evals cannot see outer scope 'using'
            if (s instanceof IREvalScript) return false;
        }

        return false;
    }

    public boolean maybeUsingRefinements() {
        return maybeUsingRefinements;
    }

    /**
     *  Returns the lexical scope that contains this scope definition
     */
    public IRScope getLexicalParent() {
        return lexicalParent;
    }

    public StaticScope getStaticScope() {
        return staticScope;
    }

    public boolean isWithinEND() {
        for (IRScope current = this; current != null && current instanceof IRClosure; current = current.getLexicalParent()) {
            if (((IRClosure) current).isEND()) return true;
        }

        return false;
    }

    public IRMethod getNearestMethod() {
        IRScope current = this;

        while (current != null && !(current instanceof IRMethod)) {
            current = current.getLexicalParent();
        }

        return (IRMethod) current;
    }

    public IRScope getNearestTopLocalVariableScope() {
        IRScope current = this;

        while (current != null && !current.isTopLocalVariableScope()) {
            current = current.getLexicalParent();
        }

        return current;
    }

    /**
     * returns whether this scope is contained by the parentScope parameter.
     * For simplicity a scope is considered to contain itself.
     *
     * @param parentScope we want to see if it contains this scope
     * @return true if this scope is contained by parentScope.
     */
    public boolean isScopeContainedBy(IRScope parentScope) {
        IRScope current = this;

        while (current != null) {
            if (parentScope == current) return true;

            current = current.getLexicalParent();
        }

        return false;
    }

    /**
     * Returns the nearest scope which we can extract a live module from.  If
     * this returns null (like for evals), then it means it cannot be statically
     * determined.
     */
    public int getNearestModuleReferencingScopeDepth() {
        int n = 0;
        IRScope current = this;
        while (!(current instanceof IRModuleBody)) {
            // When eval'ing, we dont have a lexical view of what module we are nested in
            // because binding_eval, class_eval, module_eval, instance_eval can switch
            // around the lexical scope for evaluation to be something else.
            if (current == null || current instanceof IREvalScript) return -1;

            current = current.getLexicalParent();
            if (!(current instanceof IRFor)) n++;
        }

        return n;
    }

    public String getId() {
        return getName().idString();
    }

    public RubySymbol getName() {
        return getManager().getRuntime().newSymbol(name);
    }

    public ByteList getByteName() {
        return name;
    }

    public void setByteName(ByteList name) {
        this.name = name;
    }

    public void setFileName(String filename) {
        getRootLexicalScope().setFileName(filename);
    }

    @Deprecated
    public String getFileName() {
        return getFile();
    }

    public String getFile() {
        return getRootLexicalScope().getFile();
    }

    @Deprecated
    public int getLineNumber() {
        return lineNumber;
    }

    public int getLine() {
        return lineNumber;
    }

    public int countForLoops() {
        int count = 0;
        
        for (IRScope current = this; current != null && !current.isTopLocalVariableScope(); current = current.getLexicalParent()) {
            if (current instanceof IRFor) count++;
        }

        return count;
    }

    /**
     * Returns the top level scope
     */
    public IRScope getRootLexicalScope() {
        IRScope current = this;

        for (; current != null && !current.isScriptScope(); current = current.getLexicalParent()) {}

        return current;
    }

    public boolean isNestedInClosure(IRClosure closure) {
        for (IRScope s = this; s != null && !s.isTopLocalVariableScope(); s = s.getLexicalParent()) {
            if (s == closure) return true;
        }

        return false;
    }
    
    public void setHasBreakInstructions() {
        hasBreakInstructions = true;
    }

    public boolean hasBreakInstructions() {
        return hasBreakInstructions;
    }
    
    public void setReceivesKeywordArgs() {
        receivesKeywordArgs = true;
    }
    
    public boolean receivesKeywordArgs() {
        return receivesKeywordArgs;
    }

    public void setReceivesClosureArg() {
        receivesClosureArg = true;
    }

    public boolean receivesClosureArg() {
        return receivesClosureArg;
    }

    public void setAccessesParentsLocalVariables() {
        accessesParentsLocalVariables = true;
    }

    public boolean accessesParentsLocalVariables() {
        return accessesParentsLocalVariables;
    }

    public void setHasLoops() {
        hasLoops = true;
    }

    public boolean hasLoops() {
        return hasLoops;
    }

    public void setCoverageMode(int coverageMode) {
        this.coverageMode = coverageMode;
    }

    public int getCoverageMode() {
        return coverageMode;
    }

    public void setHasNonLocalReturns() {
        hasNonLocalReturns = true;
    }

    public boolean hasNonLocalReturns() {
        return hasNonLocalReturns;
    }

    public void setCanCaptureCallersBinding() {
        canCaptureCallersBinding = true;
    }

    public boolean canCaptureCallersBinding() {
        return canCaptureCallersBinding;
    }

    public void setCanReceiveBreaks() {
        canReceiveBreaks = true;
    }

    public boolean canReceiveBreaks() {
        return canReceiveBreaks;
    }

    public void setCanReceiveNonlocalReturns() {
        canReceiveNonLocalReturns = true;
    }

    public boolean canReceiveNonlocalReturns() {
        return canReceiveNonLocalReturns;
    }
    
    public void setUsesEval() {
        usesEval = true;
    }
    
    public boolean usesEval() {
        return usesEval;
    }

    public boolean anyUsesEval() {
        // Currently methods are only lazy scopes so we need to build them if we decide to persist them.
        if (this instanceof IRMethod) {
            ((IRMethod) this).lazilyAcquireInterpreterContext();
        }

        boolean usesEval = usesEval();

        for (IRScope child : getLexicalScopes()) {
            usesEval |= child.anyUsesEval();
        }

        return usesEval;
    }

    public void setUsesZSuper() {
        usesZSuper = true;
    }

    public void setUsesSuper() {
        usesSuper = true;
    }

    public boolean usesSuper() {
        return usesSuper;
    }

    public boolean usesZSuper() {
        return usesZSuper;
    }

    public void setNeedsCodeCoverage() {
        needsCodeCoverage = true;
    }

    public boolean needsCodeCoverage() {
        return needsCodeCoverage;
    }

    public List<CompilerPass> getExecutedPasses() {
        return fullInterpreterContext == null ? new ArrayList<CompilerPass>(1) : fullInterpreterContext.getExecutedPasses();
    }

    // SSS FIXME: We should configure different optimization levels
    // and run different kinds of analysis depending on time budget.
    // Accordingly, we need to set IR levels/states (basic, optimized, etc.)
    private void runCompilerPasses(FullInterpreterContext fic, List<CompilerPass> passes, IGVDumper dumper) {
        if (dumper != null) dumper.dump(fic.getCFG(), "Start");

        CompilerPassScheduler scheduler = IRManager.schedulePasses(passes);
        for (CompilerPass pass : scheduler) {
            pass.run(fic);
            if (dumper != null) dumper.dump(fic.getCFG(), pass.getShortLabel());
        }

        if (RubyInstanceConfig.IR_UNBOXING) {
            CompilerPass pass = new UnboxingPass();
            pass.run(fic);
            if (dumper != null) dumper.dump(fic.getCFG(), pass.getShortLabel());
        }

        if (dumper != null) dumper.close();

    }

    /** Make version specific to scope which needs it (e.g. Closure vs non-closure). */
    public InterpreterContext allocateInterpreterContext(List<Instr> instructions, int tempVariableCount, EnumSet<IRFlags> flags) {
        interpreterContext = new InterpreterContext(this, instructions, tempVariableCount, flags);

        if (RubyInstanceConfig.IR_COMPILER_DEBUG) LOG.info(interpreterContext.toString());

        return interpreterContext;
    }

    /** Make version specific to scope which needs it (e.g. Closure vs non-closure). */
    public InterpreterContext allocateInterpreterContext(Supplier<List<Instr>> instructions, int tempVariableCount, EnumSet<IRFlags> flags) {
        interpreterContext = new InterpreterContext(this, instructions, tempVariableCount, flags);

        if (RubyInstanceConfig.IR_COMPILER_DEBUG) LOG.info(interpreterContext.toString());

        return interpreterContext;
    }

    private Instr[] cloneInstrs() {
        SimpleCloneInfo cloneInfo = new SimpleCloneInfo(this, false);

        Instr[] instructions = interpreterContext.getInstructions();
        int length = instructions.length;
        Instr[] newInstructions = new Instr[length];

        for (int i = 0; i < length; i++) {
            newInstructions[i] = instructions[i].clone(cloneInfo);
        }

        return newInstructions;
    }

    /**
     * This initializes a more complete(full) InterpreterContext which if used in mixed mode will be
     * used by the JIT and if used in pure-interpreted mode it will be used by an interpreter engine.
     */
    public synchronized FullInterpreterContext prepareFullBuild() {
        if (optimizedInterpreterContext != null) return optimizedInterpreterContext;
        if (fullInterpreterContext != null) return fullInterpreterContext;

        for (IRScope scope: getClosures()) {
            scope.prepareFullBuild();
        }

        FullInterpreterContext fic = new FullInterpreterContext(this, cloneInstrs(), interpreterContext.getTemporaryVariableCount(), interpreterContext.getFlags().clone());
        runCompilerPasses(fic, getManager().getCompilerPasses(this), dumpToIGV());
        getManager().optimizeIfSimpleScope(fic);

        // Always add call protocol instructions now since we are removing support for implicit stuff in interp.
        // FIXME: ACP as normal now since we have no BEGINs to make thing unsafe?
        new AddCallProtocolInstructions().run(fic);

        fic.generateInstructionsForInterpretation();

        this.fullInterpreterContext = fic;

        return fic;
    }

    // FIXME: bytelist_love - we should consider RubyString here if we care for proper printing (used for debugging).
    public String getFullyQualifiedName() {
        if (getLexicalParent() == null) return getId();

        return getLexicalParent().getFullyQualifiedName() + "::" + getId();
    }

    public IGVDumper dumpToIGV() {
        if (RubyInstanceConfig.IR_DEBUG_IGV != null) {
            String spec = RubyInstanceConfig.IR_DEBUG_IGV;

            if (spec.contains(":") && spec.equals(getFileName() + ":" + getLineNumber()) ||
                    spec.equals(getFileName())) {
                return new IGVDumper(getFullyQualifiedName() + "; line " + getLineNumber());
            }
        }

        return null;
    }

    /** Run any necessary passes to get the IR ready for compilation (AOT and/or JIT) */
    public synchronized BasicBlock[] prepareForCompilation() {
        if (optimizedInterpreterContext != null) return optimizedInterpreterContext.getLinearizedBBList();
        if (fullInterpreterContext != null) return fullInterpreterContext.getLinearizedBBList();

        for (IRScope scope: getClosures()) {
            scope.prepareForCompilation();
        }

        FullInterpreterContext fic = new FullInterpreterContext(this, cloneInstrs(), interpreterContext.getTemporaryVariableCount(), interpreterContext.getFlags().clone());
        runCompilerPasses(fic, getManager().getJITPasses(this), dumpToIGV());

        BasicBlock[] bbs = fic.linearizeBasicBlocks();

        this.fullInterpreterContext = fic;

        return bbs;
    }

    // FIXME: For inlining, culmulative or extra passes run based on profiled execution we need to re-init data or even
    // construct a new fullInterpreterContext.  Primary obstacles is JITFlags and linearization of BBs.

    public Map<BasicBlock, Label> buildJVMExceptionTable(FullInterpreterContext fic) {
        Map<BasicBlock, Label> map = new HashMap<>(1);

        for (BasicBlock bb: fic.getLinearizedBBList()) {
            BasicBlock rescueBB = fic.getCFG().getRescuerBBFor(bb);
            if (rescueBB != null) {
                map.put(bb, rescueBB.getLabel());
            }
        }

        // SSS FIXME: This could be optimized by compressing entries for adjacent BBs that have identical handlers
        // This could be optimized either during generation or as another pass over the table.  But, if the JVM
        // does that already, do we need to bother with it?
        return map;
    }

    public abstract IRScopeType getScopeType();

    @Override
    public String toString() {
        return String.valueOf(getScopeType()) + ' ' + getId() + '[' + getFile() + ':' + getLine() + "]<" + toStringCompileForm() + ">";
    }

    // Looking at way of specific startus/full/optimized
    public String toStringCompileForm() {
        return optimizedInterpreterContext != null ? "optimized" : fullInterpreterContext != null ? "full" : "startup";
    }

    public String debugOutput() {
        return toStringInstrs();
    }

    public String toStringInstrs() {
        if (fullInterpreterContext != null) { // JIT or Full interpreter
            return "Instructions:\n" + fullInterpreterContext.toStringInstrs();
        } else {                             // Startup interpreter
            return interpreterContext.toStringInstrs();
        }
    }

    public Variable getSelf() {
        return Self.SELF;
    }

    /**
     * Get the local variables for this scope.
     * This should only be used by persistence layer.
     */
    public Map<RubySymbol, LocalVariable> getLocalVariables() {
        return localVars;
    }

    public void setNextLabelIndex(int index) {
        nextLabelIndex = index;
    }

    public int getNextLabelIndex() {
        return nextLabelIndex;
    }

    public LocalVariable lookupExistingLVar(RubySymbol name) {
        return localVars.get(name);
    }

    protected LocalVariable findExistingLocalVariable(RubySymbol name, int depth) {
        return localVars.get(name);
    }

    /**
     * Find or create a local variable.  By default, scopes are assumed to
     * only check current depth.  Blocks/Closures override this because they
     * have special nesting rules.
     */
    public LocalVariable getLocalVariable(RubySymbol name, int scopeDepth) {
        LocalVariable lvar = findExistingLocalVariable(name, scopeDepth);
        if (lvar == null) {
            lvar = getNewLocalVariable(name, scopeDepth);
        } else if (lvar.getScopeDepth() != scopeDepth) {
            lvar = lvar.cloneForDepth(scopeDepth);
        }

        return lvar;
    }

    public LocalVariable getNewLocalVariable(RubySymbol name, int scopeDepth) {
        assert scopeDepth == 0: "Scope depth is non-zero for new-var request " + name + " in " + this;
        LocalVariable lvar = new LocalVariable(name, scopeDepth, getStaticScope().addVariable(name.idString()));
        localVars.put(name, lvar);
        return lvar;
    }

    /**
     * For lazy scopes which IRBuild on demand we can ask this method whether it has been built yet...
     */
    public boolean hasBeenBuilt() {
        return true;
    }

    public FullInterpreterContext getExecutionContext() {
        return fullInterpreterContext;
    }

    public InterpreterContext getInterpreterContext() {
        return interpreterContext;
    }

    public FullInterpreterContext getFullInterpreterContext() {
        return fullInterpreterContext;
    }

    public FullInterpreterContext getOptimizedInterpreterContext() {
        return optimizedInterpreterContext;
    }

    public InterpreterContext builtInterpreterContext() {
        return getInterpreterContext();
    }

    protected void depends(Object obj) {
        assert obj != null: "Unsatisfied dependency and this depends() was set " +
                "up wrong.  Use depends(build()) not depends(build).";
    }

    private FullInterpreterContext inlineFailed(String reason) {
        inlineFailed = reason;
        return null;
    }

    private FullInterpreterContext inlineMethodCommon(IRMethod methodToInline, RubyModule implClass, long callsiteId, int classToken, boolean cloneHost) {
        alreadyHasInline = true;

        // Host may still be running in startup interp...promote it to full.
        if (getFullInterpreterContext() == null) prepareFullBuild();

        // FIXME: So a potential problem is closures contain local variables in the method being inlined then we will nuke
        // those scoped variables and the closure cannot see them.  One idea is since for deoptimization we will need to
        // create a scope restore table we will have a list of all lvars -> temps.  Since this will be a map we depend on
        // for restoring scope we can probably make an temp variable which will look for values from this table.  Even in
        // that solution we need access to the temp table from the closure so I am unsure that will work.
        //
        // Another solution is to force inline those closures but that only works if the methods they are calling through
        // are IR methods (or are native but can be substituted with IR methods).
        //
        // Note: we can look for scoped methods and make this less conservative.
        if (!methodToInline.getClosures().isEmpty()) {
            boolean accessInaccessibleLocalVariables = false;
            for (IRClosure closure: methodToInline.getClosures()) {
                if (closure.accessesParentsLocalVariables()) {
                    accessInaccessibleLocalVariables = true;
                    break;
                }
            }
            if (accessInaccessibleLocalVariables) return inlineFailed("inline a method which contains nested closures which access methods lvars");
        }

        FullInterpreterContext newContext = getFullInterpreterContext().duplicate();
        if (newContext == null) {
            return inlineFailed("FIXME: BBs are not linearized???");
        }
        BasicBlock basicBlock = newContext.findBasicBlockOf(callsiteId);
        CallBase call = (CallBase) basicBlock.siteOf(callsiteId);  // we know it is callBase and not a yield

        String error = new CFGInliner(newContext).inlineMethod(methodToInline, implClass, classToken, basicBlock, call, cloneHost);

        return error == null ? newContext : inlineFailed(error);
    }

    public void inlineMethod(IRMethod methodToInline, RubyModule metaclass, long callsiteId, int classToken, boolean cloneHost) {
        if (alreadyHasInline) return;

        FullInterpreterContext newContext = inlineMethodCommon(methodToInline, metaclass, callsiteId, classToken, cloneHost);
        if (newContext == null) {
            if (IRManager.IR_INLINER_VERBOSE) LOG.info("Inline of " + methodToInline + " into " + this + " failed: " + inlineFailed + ".");
            return;
        } else {
            if (IRManager.IR_INLINER_VERBOSE) LOG.info("Inline of " + methodToInline + " into " + this + " succeeded.");
        }
        newContext.generateInstructionsForInterpretation();
        this.optimizedInterpreterContext = newContext;

        manager.getRuntime().getJITCompiler().getTaskFor(manager.getRuntime().getCurrentContext(), compilable).run();
    }

    public void inlineMethodJIT(IRMethod methodToInline, RubyModule implClass, long callsiteId, int classToken, boolean cloneHost) {
        if (alreadyHasInline) return;

        FullInterpreterContext newContext = inlineMethodCommon(methodToInline, implClass, callsiteId, classToken, cloneHost);
        Ruby runtime = manager.getRuntime();
        if (newContext == null) {
            if (IRManager.IR_INLINER_VERBOSE) LOG.info("Inline of " + methodToInline + " into " + this + " failed: " + inlineFailed + ".");
            runtime.getInlineStats().incrementInlineFailedCount();
            return;
        } else {
            if (IRManager.IR_INLINER_VERBOSE) LOG.info("Inline of " + methodToInline + " into " + this + " succeeded.");
            runtime.getInlineStats().incrementInlineSuccessCount();
        }

        // We are not running any JIT-specific passes here.

        newContext.linearizeBasicBlocks();
        this.optimizedInterpreterContext = newContext;

        runtime.getJITCompiler().getTaskFor(manager.getRuntime().getCurrentContext(), compilable).run();
     }

    public void inlineMethodCompiled(IRMethod methodToInline, RubyModule implClass, long callsiteId, int classToken, boolean cloneHost) {
        if (alreadyHasInline) return;

        FullInterpreterContext newContext = inlineMethodCommon(methodToInline, implClass, callsiteId, classToken, cloneHost);
        if (newContext == null) {
            if (IRManager.IR_INLINER_VERBOSE) LOG.info("Inline of " + methodToInline + " into " + this + " failed: " + inlineFailed + ".");
            return;
        } else {
            if (IRManager.IR_INLINER_VERBOSE) LOG.info("Inline of " + methodToInline + " into " + this + " succeeded.");
        }

        // We are not running any JIT-specific passes here.

        newContext.linearizeBasicBlocks();
        this.optimizedInterpreterContext = newContext;

        manager.getRuntime().getJITCompiler().getTaskFor(manager.getRuntime().getCurrentContext(), compilable).run();
    }

    public int getNextClosureId() {
        nextClosureIndex++;

        return nextClosureIndex;
    }

    /**
     * Does this scope represent a module body?
     */
    public boolean isModuleBody() {
        return false;
    }

    /**
     * Is this IRClassBody but not IRMetaClassBody?
     */
    public boolean isNonSingletonClassBody() {
        return false;
    }

    public boolean isTopLocalVariableScope() {
        return true;
    }

    /**
     * Is this an eval script or a regular file script?
     */
    public boolean isScriptScope() {
        return false;
    }

    // FIXME: This should become some heuristic later
    public boolean inliningAllowed() {
        return !alreadyHasInline;
    }

    /**
     * Duplicate the parent scope's refinements overlay to get a moment-in-time snapshot.
     *
     * @param context
     */
    public void captureParentRefinements(ThreadContext context) {
        if (maybeUsingRefinements()) {
            getStaticScope().captureParentRefinements(context);
        }
    }

    /**
     * We are done with execution of this scope and we can cleanup some amount of things
     * in this scope which will no longer be used.  Sub-classes will be the deciders of what
     * is no longer needed.  An example, to illustrate the complexity of cleanup:  A class with
     * no nested closures can remove any ICs created and can remove some other infomrational
     * data structures like allocated variables unless closures do exist and then the ICs must
     * stay for when closures JIT.
     */
    public void cleanupAfterExecution() {
    }

    public boolean executesOnce() {
        return false;
    }

    public void persistScopeHeader(IRWriterEncoder file) {
        if (IRWriter.shouldLog(file)) System.out.println("IRScope.persistScopeHeader: type       = " + getScopeType());
        file.encode(getScopeType()); // type is enum of kind of scope
        if (IRWriter.shouldLog(file)) System.out.println("IRScope.persistScopeHeader: line       = " + getLine());
        file.encode(getLine());
        if (RubyInstanceConfig.IR_WRITING_DEBUG) System.out.println("# of temp vars = " + getInterpreterContext().getTemporaryVariableCount());
        file.encode(getInterpreterContext().getTemporaryVariableCount());
        file.encode(getNextLabelIndex());
    }

    public void persistScopeFlags(IRWriterEncoder file) {
        file.encode(getInterpreterContext().getFlags());
        file.encode(hasBreakInstructions());
        file.encode(hasLoops());
        file.encode(hasNonLocalReturns());
        file.encode(receivesClosureArg());
        file.encode(receivesKeywordArgs());
        file.encode(accessesParentsLocalVariables());
        file.encode(maybeUsingRefinements());
        file.encode(canCaptureCallersBinding());
        file.encode(canReceiveBreaks());
        file.encode(canReceiveNonlocalReturns());
        file.encode(usesZSuper());
        file.encode(usesEval());
        file.encode(getCoverageMode());
    }

    public static EnumSet<IRFlags> allocateInitialFlags(IRScope scope) {
        // NOTE: bindingHasEscaped is the crucial flag and it effectively is
        // unconditionally true whenever it has a call that receives a closure.
        // See CallBase.computeRequiresCallersBindingFlag
        if (scope instanceof IREvalScript || scope instanceof IRScriptBody) {
                // For eval scopes, bindings are considered escaped.
                // For top-level script scopes, bindings are considered escaped as well
                // because TOPLEVEL_BINDING can be used in places besides the file
                // that is being parsed?
                return EnumSet.of(BINDING_HAS_ESCAPED);
        } else {
            return EnumSet.noneOf(IRFlags.class);
        }
    }
}
