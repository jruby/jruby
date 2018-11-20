package org.jruby.ir;

import org.jruby.ParseResult;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.RubySymbol;
import org.jruby.compiler.Compilable;
import org.jruby.internal.runtime.methods.CompiledIRMethod;
import org.jruby.internal.runtime.methods.MixedModeIRMethod;
import org.jruby.ir.dataflow.analyses.LiveVariablesProblem;
import org.jruby.ir.dataflow.analyses.StoreLocalVarPlacementProblem;
import org.jruby.ir.dataflow.analyses.UnboxableOpsAnalysisProblem;
import org.jruby.ir.instructions.*;
import org.jruby.ir.interpreter.FullInterpreterContext;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.operands.*;
import org.jruby.ir.operands.Float;
import org.jruby.ir.passes.*;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.ir.representations.CFG;
import org.jruby.ir.transformations.inlining.CFGInliner;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;
import org.jruby.ir.util.IGVDumper;
import org.jruby.parser.StaticScope;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.jruby.runtime.Helpers;
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
    private int scopeId;

    /** Name */
    private RubySymbol name;

    /** Starting line for this scope's definition */
    private final int lineNumber;

    /** Lexical parent scope */
    private IRScope lexicalParent;

    /** List of (nested) closures in this scope */
    private List<IRClosure> nestedClosures;

    // Index values to guarantee we don't assign same internal index twice
    private int nextClosureIndex;

    // List of all scopes this scope contains lexically.  This is not used
    // for execution, but is used during dry-runs for debugging.
    private List<IRScope> lexicalChildren;

    /** Parser static-scope that this IR scope corresponds to */
    private final StaticScope staticScope;

    /** Local variables defined in this scope */
    private Set<LocalVariable> definedLocalVars;

    /** Local variables used in this scope */
    private Set<LocalVariable> usedLocalVars;

    /** Startup interpretation depends on this */
    protected InterpreterContext interpreterContext;

    /** -X-C full interpretation OR JIT depends on this */
    protected FullInterpreterContext fullInterpreterContext;

    /** Speculatively optimized code */
    protected FullInterpreterContext optimizedInterpreterContext;

    protected int temporaryVariableIndex;
    protected int floatVariableIndex;
    protected int fixnumVariableIndex;
    protected int booleanVariableIndex;

    /** Keeps track of types of prefix indexes for variables and labels */
    private Map<String, Integer> nextVarIndex;

    private TemporaryLocalVariable currentModuleVariable;
    private TemporaryLocalVariable currentScopeVariable;

    Map<RubySymbol, LocalVariable> localVars;

    EnumSet<IRFlags> flags = EnumSet.noneOf(IRFlags.class);

    /** Have scope flags been computed? */
    private boolean flagsComputed;

    /** # of thread poll instrs added to this scope */
    protected int threadPollInstrsCount;

    private IRManager manager;

    private TemporaryVariable yieldClosureVariable;

    private boolean alreadyHasInline;
    private String inlineFailed;
    public Compilable compilable;

    // Used by cloning code
    protected IRScope(IRScope s, IRScope lexicalParent) {
        this.lexicalParent = lexicalParent;
        this.manager = s.manager;
        this.lineNumber = s.lineNumber;
        this.staticScope = s.staticScope;
        this.threadPollInstrsCount = s.threadPollInstrsCount;
        this.nextClosureIndex = s.nextClosureIndex;
        this.temporaryVariableIndex = s.temporaryVariableIndex;
        this.floatVariableIndex = s.floatVariableIndex;
        this.nextVarIndex = new HashMap<>(1); // SSS FIXME: clone!
        this.interpreterContext = null;

        this.flagsComputed = s.flagsComputed;
        this.flags = s.flags.clone();

        this.localVars = new HashMap<>(s.localVars);
        this.scopeId = globalScopeCount.getAndIncrement();

        setupLexicalContainment();
    }

    public IRScope(IRManager manager, IRScope lexicalParent, RubySymbol name,
            int lineNumber, StaticScope staticScope) {
        this.manager = manager;
        this.lexicalParent = lexicalParent;
        this.name = name;
        this.lineNumber = lineNumber;
        this.staticScope = staticScope;
        this.threadPollInstrsCount = 0;
        this.nextClosureIndex = 0;
        this.temporaryVariableIndex = -1;
        this.floatVariableIndex = -1;
        this.nextVarIndex = new HashMap<>(1);
        this.interpreterContext = null;
        this.flagsComputed = false;
        flags.remove(CAN_RECEIVE_BREAKS);
        flags.remove(CAN_RECEIVE_NONLOCAL_RETURNS);
        flags.remove(HAS_BREAK_INSTRS);
        flags.remove(HAS_END_BLOCKS);
        flags.remove(HAS_EXPLICIT_CALL_PROTOCOL);
        flags.remove(HAS_LOOPS);
        flags.remove(HAS_NONLOCAL_RETURNS);
        flags.remove(RECEIVES_KEYWORD_ARGS);

        // These flags are true by default!
        flags.add(CAN_CAPTURE_CALLERS_BINDING);
        flags.add(BINDING_HAS_ESCAPED);
        flags.add(USES_EVAL);
        flags.add(REQUIRES_BACKREF);
        flags.add(REQUIRES_LASTLINE);
        flags.add(REQUIRES_DYNSCOPE);
        flags.add(USES_ZSUPER);

        // We only can compute this once since 'module X; using A; class B; end; end' vs
        // 'module X; class B; using A; end; end'.  First case B can see refinements and in second it cannot.
        if (parentMaybeUsingRefinements()) flags.add(MAYBE_USING_REFINEMENTS);

        this.localVars = new HashMap<>(1);
        this.scopeId = globalScopeCount.getAndIncrement();

        setupLexicalContainment();
    }

    private void setupLexicalContainment() {
        if (manager.isDryRun() || RubyInstanceConfig.IR_WRITING || RubyInstanceConfig.RECORD_LEXICAL_HIERARCHY) {
            lexicalChildren = new ArrayList<>(1);
            if (lexicalParent != null) lexicalParent.addChildScope(this);
        }
    }

    public int getScopeId() {
        return scopeId;
    }

    @Override
    public int hashCode() {
        return scopeId;
    }

    public void setInterpreterContext(InterpreterContext interpreterContext) {
        this.interpreterContext = interpreterContext;
    }

    @Override
    public boolean equals(Object other) {
        return (other != null) && (getClass() == other.getClass()) && (scopeId == ((IRScope) other).scopeId);
    }

    protected void addChildScope(IRScope scope) {
        if (lexicalChildren == null) lexicalChildren = new ArrayList<>(1);
        lexicalChildren.add(scope);
    }

    public List<IRScope> getLexicalScopes() {
        if (lexicalChildren == null) lexicalChildren = new ArrayList<>(1);
        return lexicalChildren;
    }

    public void addClosure(IRClosure closure) {
        if (nestedClosures == null) nestedClosures = new ArrayList<>(1);
        nestedClosures.add(closure);
    }

    public void removeClosure(IRClosure closure) {
        if (nestedClosures != null) nestedClosures.remove(closure);
    }

    private static final ByteList FLIP = new ByteList(new byte[] {'%', 'f', 'l', 'i', 'p', '_'});

    public LocalVariable getNewFlipStateVariable() {
        ByteList flip = FLIP.dup();

        flip.append(allocateNextPrefixedName("%flip"));

        return getLocalVariable(getManager().getRuntime().newSymbol(flip) , 0);
    }

    public Label getNewLabel(String prefix) {
        return new Label(prefix, allocateNextPrefixedName(prefix));
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
        flags.add(MAYBE_USING_REFINEMENTS);
    }

    public boolean parentMaybeUsingRefinements() {
        for (IRScope s = this; s != null; s = s.getLexicalParent()) {
            if (s.getFlags().contains(MAYBE_USING_REFINEMENTS)) return true;

            // Evals cannot see outer scope 'using'
            if (s instanceof IREvalScript) return false;
        }

        return false;
    }

    public boolean maybeUsingRefinements() {
        return getFlags().contains(MAYBE_USING_REFINEMENTS);
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

    public IRMethod getNearestMethod() {
        IRScope current = this;

        while (current != null && !(current instanceof IRMethod)) {
            current = current.getLexicalParent();
        }

        return (IRMethod) current;
    }

    public IRScope getNearestFlipVariableScope() {
        IRScope current = this;

        while (current != null && !current.isFlipScope()) {
            current = current.getLexicalParent();
        }

        return current;
    }

    public IRScope getNearestTopLocalVariableScope() {
        IRScope current = this;

        while (current != null && !current.isTopLocalVariableScope()) {
            current = current.getLexicalParent();
        }

        return current;
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
        return name.idString();
    }

    public RubySymbol getName() {
        return name;
    }

    public void setName(RubySymbol name) {
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

    public void setHasLoopsFlag() {
        flags.add(HAS_LOOPS);
    }

    public boolean hasLoops() {
        return flags.contains(HAS_LOOPS);
    }

    public boolean hasExplicitCallProtocol() {
        return flags.contains(HAS_EXPLICIT_CALL_PROTOCOL);
    }

    public void setExplicitCallProtocolFlag() {
        flags.add(HAS_EXPLICIT_CALL_PROTOCOL);
    }

    public boolean receivesKeywordArgs() {
        return flags.contains(RECEIVES_KEYWORD_ARGS);
    }

    public boolean bindingHasEscaped() {
        return flags.contains(BINDING_HAS_ESCAPED);
    }

    public boolean usesEval() {
        return flags.contains(USES_EVAL);
    }

    public boolean usesZSuper() {
        return flags.contains(USES_ZSUPER);
    }

    public boolean canReceiveNonlocalReturns() {
        computeScopeFlags();
        return flags.contains(CAN_RECEIVE_NONLOCAL_RETURNS);
    }

    public void putLiveVariablesProblem(LiveVariablesProblem problem) {
        // Technically this is if a pass is invalidated which has never run on a scope with no CFG/FIC yet.
        if (fullInterpreterContext == null) {
            // This should never trigger unless we got sloppy
            if (problem != null) throw new IllegalStateException("LVP being stored when no FIC");
            return;
        }
        fullInterpreterContext.getDataFlowProblems().put(LiveVariablesProblem.NAME, problem);
    }

    public LiveVariablesProblem getLiveVariablesProblem() {
        if (fullInterpreterContext == null) return null; // no fic so no pass-related info

        return (LiveVariablesProblem) fullInterpreterContext.getDataFlowProblems().get(LiveVariablesProblem.NAME);
    }

    public void putStoreLocalVarPlacementProblem(StoreLocalVarPlacementProblem problem) {
        // Technically this is if a pass is invalidated which has never run on a scope with no CFG/FIC yet.
        if (fullInterpreterContext == null) {
            // This should never trigger unless we got sloppy
            if (problem != null) throw new IllegalStateException("StoreLocalVarPlacementProblem being stored when no FIC");
            return;
        }
        fullInterpreterContext.getDataFlowProblems().put(StoreLocalVarPlacementProblem.NAME, problem);
    }

    public StoreLocalVarPlacementProblem getStoreLocalVarPlacementProblem() {
        if (fullInterpreterContext == null) return null; // no fic so no pass-related info

        return (StoreLocalVarPlacementProblem) fullInterpreterContext.getDataFlowProblems().get(StoreLocalVarPlacementProblem.NAME);
    }

    public void putUnboxableOpsAnalysisProblem(UnboxableOpsAnalysisProblem problem) {
        // Technically this is if a pass is invalidated which has never run on a scope with no CFG/FIC yet.
        if (fullInterpreterContext == null) {
            // This should never trigger unless we got sloppy
            if (problem != null) throw new IllegalStateException("UboxableOpsAnalysisProblem being stored when no FIC");
            return;
        }
        fullInterpreterContext.getDataFlowProblems().put(UnboxableOpsAnalysisProblem.NAME, problem);
    }

    public UnboxableOpsAnalysisProblem getUnboxableOpsAnalysisProblem() {
        if (fullInterpreterContext == null) return null; // no fic so no pass-related info

        return (UnboxableOpsAnalysisProblem) fullInterpreterContext.getDataFlowProblems().get(UnboxableOpsAnalysisProblem.NAME);
    }

    public CFG getCFG() {
        if (getOptimizedInterpreterContext() != null) {
            return getOptimizedInterpreterContext().getCFG();
        }
        // A child scope may not have been prepared yet so we advance it to point of have a fresh CFG.
        if (getFullInterpreterContext() == null) prepareFullBuildCommon();

        return fullInterpreterContext.getCFG();
    }

    protected boolean isUnsafeScope() {
        if (this.isBeginEndBlock()) return true;                        // this is a BEGIN block

        List beginBlocks = getBeginBlocks();
        if (beginBlocks != null && !beginBlocks.isEmpty()) return true; // this contains a BEGIN block

        // Does topmost variable scope contain any BEGIN blocks (IRScriptBody or IREval)?
        // Ex1: eval("BEGIN {a = 1}; p a")    Ex2: BEGIN {a = 1}; p a
        beginBlocks = getNearestTopLocalVariableScope().getBeginBlocks();
        return beginBlocks != null && !beginBlocks.isEmpty();
    }

    public List<CompilerPass> getExecutedPasses() {
        return fullInterpreterContext == null ? new ArrayList<CompilerPass>(1) : fullInterpreterContext.getExecutedPasses();
    }

    // SSS FIXME: We should configure different optimization levels
    // and run different kinds of analysis depending on time budget.
    // Accordingly, we need to set IR levels/states (basic, optimized, etc.)
    private void runCompilerPasses(List<CompilerPass> passes, IGVDumper dumper) {
        // All passes are disabled in scopes where BEGIN and END scopes might
        // screw around with escaped variables. Optimizing for them is not
        // worth the effort. It is simpler to just go fully safe in scopes
        // influenced by their presence.
        if (isUnsafeScope()) {
            passes = getManager().getSafePasses(this);
        }

        if (dumper != null) dumper.dump(getCFG(), "Start");

        CompilerPassScheduler scheduler = IRManager.schedulePasses(passes);
        for (CompilerPass pass : scheduler) {
            pass.run(this);
            if (dumper != null) dumper.dump(getCFG(), pass.getShortLabel());
        }

        if (RubyInstanceConfig.IR_UNBOXING) {
            CompilerPass pass = new UnboxingPass();
            pass.run(this);
            if (dumper != null) dumper.dump(getCFG(), pass.getShortLabel());
        }

        if (dumper != null) dumper.close();

    }

    /** Make version specific to scope which needs it (e.g. Closure vs non-closure). */
    public InterpreterContext allocateInterpreterContext(List<Instr> instructions) {
        interpreterContext = new InterpreterContext(this, instructions);

        if (RubyInstanceConfig.IR_COMPILER_DEBUG) LOG.info(interpreterContext.toString());

        return interpreterContext;
    }

    /** Make version specific to scope which needs it (e.g. Closure vs non-closure). */
    public InterpreterContext allocateInterpreterContext(Callable<List<Instr>> instructions) {
        try {
            interpreterContext = new InterpreterContext(this, instructions);
        } catch (Exception e) {
            Helpers.throwException(e);
        }

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

            // Logically when we move from simple to full we want to keep same callsites in both versions to
            // have the same callsiteid so we can track their usage logically.
            if (instructions[i] instanceof Site) {
                ((Site) newInstructions[i]).setCallSiteId(((Site) instructions[i]).getCallSiteId());
            }
        }

        return newInstructions;
    }

    private void prepareFullBuildCommon() {
        // We already made it.
        if (fullInterpreterContext != null) return;

        // Clone instrs from startup interpreter so we do not swap out instrs out from under the
        // startup interpreter as we are building the full interpreter.
        fullInterpreterContext = new FullInterpreterContext(this, cloneInstrs());
    }

    /**
     * This initializes a more complete(full) InterpreterContext which if used in mixed mode will be
     * used by the JIT and if used in pure-interpreted mode it will be used by an interpreter engine.
     */
    public synchronized FullInterpreterContext prepareFullBuild() {
        if (optimizedInterpreterContext != null) return optimizedInterpreterContext;
        // Don't run if same method was queued up in the tiny race for scheduling JIT/Full Build OR
        // for any nested closures which got a a fullInterpreterContext but have not run any passes
        // or generated instructions.
        if (fullInterpreterContext != null && fullInterpreterContext.buildComplete()) return fullInterpreterContext;

        for (IRScope scope: getClosures()) {
            scope.prepareFullBuild();
        }

        prepareFullBuildCommon();
        runCompilerPasses(getManager().getCompilerPasses(this), dumpToIGV());
        getManager().optimizeIfSimpleScope(this);

        // Always add call protocol instructions now since we are removing support for implicit stuff in interp.
        if (!isUnsafeScope()) new AddCallProtocolInstructions().run(this);

        fullInterpreterContext.generateInstructionsForInterpretation();

        return fullInterpreterContext;
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
        if (optimizedInterpreterContext != null && optimizedInterpreterContext.buildComplete()) {
            return optimizedInterpreterContext.getLinearizedBBList();
        }
        // Don't run if same method was queued up in the tiny race for scheduling JIT/Full Build OR
        // for any nested closures which got a a fullInterpreterContext but have not run any passes
        // or generated instructions.
        if (fullInterpreterContext != null && fullInterpreterContext.buildComplete()) return fullInterpreterContext.getLinearizedBBList();

        for (IRScope scope: getClosures()) {
            scope.prepareForCompilation();
        }

        prepareFullBuildCommon();

        runCompilerPasses(getManager().getJITPasses(this), dumpToIGV());

        BasicBlock[] bbs = fullInterpreterContext.linearizeBasicBlocks();

        return bbs;
    }

    // FIXME: For inlining, culmulative or extra passes run based on profiled execution we need to re-init data or even
    // construct a new fullInterpreterContext.  Primary obstacles is JITFlags and linearization of BBs.

    public Map<BasicBlock, Label> buildJVMExceptionTable() {
        Map<BasicBlock, Label> map = new HashMap<>(1);

        for (BasicBlock bb: fullInterpreterContext.getLinearizedBBList()) {
            BasicBlock rescueBB = getCFG().getRescuerBBFor(bb);
            if (rescueBB != null) {
                map.put(bb, rescueBB.getLabel());
            }
        }

        // SSS FIXME: This could be optimized by compressing entries for adjacent BBs that have identical handlers
        // This could be optimized either during generation or as another pass over the table.  But, if the JVM
        // does that already, do we need to bother with it?
        return map;
    }

    public EnumSet<IRFlags> getFlags() {
        return flags;
    }

    private void initScopeFlags() {
        // CON: why isn't this just clear?
        flags.remove(CAN_CAPTURE_CALLERS_BINDING);
        flags.remove(CAN_RECEIVE_BREAKS);
        flags.remove(CAN_RECEIVE_NONLOCAL_RETURNS);
        flags.remove(HAS_BREAK_INSTRS);
        flags.remove(HAS_NONLOCAL_RETURNS);
        flags.remove(USES_ZSUPER);
        flags.remove(USES_EVAL);
        flags.remove(REQUIRES_DYNSCOPE);

        flags.remove(REQUIRES_LASTLINE);
        flags.remove(REQUIRES_BACKREF);
        flags.remove(REQUIRES_VISIBILITY);
        flags.remove(REQUIRES_BLOCK);
        flags.remove(REQUIRES_SELF);
        flags.remove(REQUIRES_METHODNAME);
        flags.remove(REQUIRES_LINE);
        flags.remove(REQUIRES_CLASS);
        flags.remove(REQUIRES_FILENAME);
        flags.remove(REQUIRES_SCOPE);
    }

    private void bindingEscapedScopeFlagsCheck() {
        // NOTE: bindingHasEscaped is the crucial flag and it effectively is
        // unconditionally true whenever it has a call that receives a closure.
        // See CallBase.computeRequiresCallersBindingFlag
        if (this instanceof IREvalScript || this instanceof IRScriptBody) {
            // For eval scopes, bindings are considered escaped.
            // For top-level script scopes, bindings are considered escaped as well
            // because TOPLEVEL_BINDING can be used in places besides the file
            // that is being parsed?
            flags.add(BINDING_HAS_ESCAPED);
        } else {
            flags.remove(BINDING_HAS_ESCAPED);
        }
    }

    private void calculateClosureScopeFlags() {
        // Compute flags for nested closures (recursively) and set derived flags.
        for (IRClosure cl: getClosures()) {
            cl.computeScopeFlags();
            if (cl.usesEval()) {
                flags.add(CAN_RECEIVE_BREAKS);
                flags.add(CAN_RECEIVE_NONLOCAL_RETURNS);
                flags.add(USES_ZSUPER);
            } else {
                if (cl.flags.contains(HAS_BREAK_INSTRS) || cl.flags.contains(CAN_RECEIVE_BREAKS)) {
                    flags.add(CAN_RECEIVE_BREAKS);
                }
                if (cl.flags.contains(HAS_NONLOCAL_RETURNS) || cl.flags.contains(CAN_RECEIVE_NONLOCAL_RETURNS)) {
                    flags.add(CAN_RECEIVE_NONLOCAL_RETURNS);
                }
                if (cl.usesZSuper()) {
                    flags.add(USES_ZSUPER);
                }
            }
        }
    }

    private static final EnumSet<IRFlags> NEEDS_DYNAMIC_SCOPE_FLAGS =
            EnumSet.of(
                    CAN_RECEIVE_BREAKS,
                    HAS_NONLOCAL_RETURNS,CAN_RECEIVE_NONLOCAL_RETURNS,
                    BINDING_HAS_ESCAPED);

    private void computeNeedsDynamicScopeFlag() {
        for (IRFlags f : NEEDS_DYNAMIC_SCOPE_FLAGS) {
            if (flags.contains(f)) {
                flags.add(REQUIRES_DYNSCOPE);
                return;
            }
        }
    }

    // ENEBO: IRBuild adds more instrs after this so should we force a recompute?
    /**
     * This is called when building an IRMethod before it has completed the build and made an IC
     * yet.
     */
    public void computeScopeFlagsEarly(List<Instr> instructions) {
        initScopeFlags();
        bindingEscapedScopeFlagsCheck();

        for (Instr i : instructions) {
            i.computeScopeFlags(this);
        }

        calculateClosureScopeFlags();
        computeNeedsDynamicScopeFlag();

        flagsComputed = true;
    }


    /**
     * Calculate scope flags used by various passes to know things like whether a binding has escaped.
     * We may recalculate flags in a few scenarios:
     *  - once after IR generation and local optimizations propagates constants locally
     *  - also potentially at later times after other opt passes
     */
    public void computeScopeFlags() {
        if (flagsComputed) return;

        initScopeFlags();
        bindingEscapedScopeFlagsCheck();

        if (fullInterpreterContext != null) {
            fullInterpreterContext.computeScopeFlagsFromInstructions();
        } else {
            interpreterContext.computeScopeFlagsFromInstructions();
        }

        calculateClosureScopeFlags();
        computeNeedsDynamicScopeFlag();

        flagsComputed = true;
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

    public Variable getCurrentModuleVariable() {
        // SSS: Used in only 3 cases in generated IR:
        // -> searching a constant in the inheritance hierarchy
        // -> searching a super-method in the inheritance hierarchy
        // -> looking up 'StandardError' (which can be eliminated by creating a special operand type for this)
        if (currentModuleVariable == null) {
            temporaryVariableIndex++;
            currentModuleVariable = TemporaryCurrentModuleVariable.ModuleVariableFor(temporaryVariableIndex);
        }
        return currentModuleVariable;
    }

    public Variable getCurrentScopeVariable() {
        // SSS: Used in only 1 case in generated IR:
        // -> searching a constant in the lexical scope hierarchy
        if (currentScopeVariable == null) {
            temporaryVariableIndex++;
            currentScopeVariable = TemporaryCurrentScopeVariable.ScopeVariableFor(temporaryVariableIndex);
        }
        return currentScopeVariable;
    }

    /**
     * Get the local variables for this scope.
     * This should only be used by persistence layer.
     */
    public Map<RubySymbol, LocalVariable> getLocalVariables() {
        return localVars;
    }

    /**
     * Get all variables referenced by this scope.
     */
    public Set<LocalVariable> getUsedLocalVariables() {
        return usedLocalVars;
    }

    /**
     * Set the local variables for this scope. This should only be used by persistence layer.
     */
    // FIXME: Consider making constructor for persistence to pass in all of this stuff
    public void setLocalVariables(Map<RubySymbol, LocalVariable> variables) {
        this.localVars = variables;
    }

    public void setLabelIndices(Map<String, Integer> indices) {
        nextVarIndex = indices;
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

    public TemporaryLocalVariable createTemporaryVariable() {
        return getNewTemporaryVariable(TemporaryVariableType.LOCAL);
    }

    public TemporaryLocalVariable getNewTemporaryVariableFor(LocalVariable var) {
        temporaryVariableIndex++;
        return new TemporaryLocalReplacementVariable(var.getId(), temporaryVariableIndex);
    }

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
                temporaryVariableIndex++;
                return manager.newTemporaryLocalVariable(temporaryVariableIndex);
            }
        }

        throw new RuntimeException("Invalid temporary variable being alloced in this scope: " + type);
    }

    public void setTemporaryVariableCount(int count) {
        temporaryVariableIndex = count + 1;
    }

    /**
     * Get the variable for accessing the "yieldable" closure in this scope.
     */
    public TemporaryVariable getYieldClosureVariable() {
        if (yieldClosureVariable == null) {
            return yieldClosureVariable = createTemporaryVariable();
        }

        return yieldClosureVariable;
    }

    public TemporaryLocalVariable getNewUnboxedVariable(Class type) {
        TemporaryVariableType varType;
        if (type == Float.class) {
            varType = TemporaryVariableType.FLOAT;
        } else if (type == Fixnum.class) {
            varType = TemporaryVariableType.FIXNUM;
        } else if (type == java.lang.Boolean.class) {
            varType = TemporaryVariableType.BOOLEAN;
        } else {
            varType = TemporaryVariableType.LOCAL;
        }
        return getNewTemporaryVariable(varType);
    }

    public void resetTemporaryVariables() {
        temporaryVariableIndex = -1;
        floatVariableIndex = -1;
        fixnumVariableIndex = -1;
        booleanVariableIndex = -1;
    }

    public int getTemporaryVariablesCount() {
        return temporaryVariableIndex + 1;
    }

    public int getFloatVariablesCount() {
        return floatVariableIndex + 1;
    }

    public int getFixnumVariablesCount() {
        return fixnumVariableIndex + 1;
    }

    public int getBooleanVariablesCount() {
        return booleanVariableIndex + 1;
    }

    // Generate a new variable for inlined code
    public Variable getNewInlineVariable(ByteList inlinePrefix, Variable v) {
        // FIXME: This should definitely not be polluting inlined scope with %i_old_var_name but we do want
        //   a nice understandable temp var name so it is more easy to track.
        /*
        if (v instanceof LocalVariable) {
            LocalVariable lv = (LocalVariable)v;
            ByteList newName = inlinePrefix.dup();

            newName.append(lv.getName().getBytes());

            return getLocalVariable(getManager().getRuntime().newSymbol(newName), lv.getScopeDepth());
        } else {*/
            return createTemporaryVariable();
        //}
    }

    public int getThreadPollInstrsCount() {
        return threadPollInstrsCount;
    }

    public int getLocalVariablesCount() {
        return localVars.size();
    }

    public int getUsedVariablesCount() {
        // System.out.println("For " + this + ", # lvs: " + getLocalVariablesCount());
        // # local vars, # flip vars
        //
        // SSS FIXME: When we are opting local var access,
        // no need to allocate local var space except when we have been asked to!
        return getLocalVariablesCount() + getPrefixCountSize("%flip");
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

        for (IRClosure cl : getClosures()) {
            cl.setUpUseDefLocalVarMaps();
        }
    }

    public boolean usesLocalVariable(Variable v) {
        if (usedLocalVars == null) setUpUseDefLocalVarMaps();
        if (usedLocalVars.contains(v)) return true;

        for (IRClosure cl : getClosures()) {
            if (cl.usesLocalVariable(v)) return true;
        }

        return false;
    }

    public boolean definesLocalVariable(Variable v) {
        if (definedLocalVars == null) setUpUseDefLocalVarMaps();
        if (definedLocalVars.contains(v)) return true;

        for (IRClosure cl : getClosures()) {
            if (cl.definesLocalVariable(v)) return true;
        }

        return false;
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

    protected void depends(Object obj) {
        assert obj != null: "Unsatisfied dependency and this depends() was set " +
                "up wrong.  Use depends(build()) not depends(build).";
    }

    public void resetState() {
        interpreterContext = null;
        fullInterpreterContext = null;

        // reset flags
        flagsComputed = false;
        flags.add(CAN_CAPTURE_CALLERS_BINDING);
        flags.add(BINDING_HAS_ESCAPED);
        flags.add(USES_EVAL);
        flags.add(USES_ZSUPER);

        flags.remove(HAS_BREAK_INSTRS);
        flags.remove(HAS_NONLOCAL_RETURNS);
        flags.remove(CAN_RECEIVE_BREAKS);
        flags.remove(CAN_RECEIVE_NONLOCAL_RETURNS);

        // Invalidate compiler pass state.
        //
        // SSS FIXME: Re-grabbing passes each iter is to get around concurrent-modification issues
        // since CompilerPass.invalidate modifies this, but some passes cannot be invalidated.  This
        // should be wrapped in an iterator.
        FullInterpreterContext fic = getFullInterpreterContext();
        if (fic != null) {
            int i = 0;
            while (i < fic.getExecutedPasses().size()) {
                if (!fic.getExecutedPasses().get(i).invalidate(this)) {
                    i++;
                }
            }
        }
    }

    private FullInterpreterContext inlineFailed(String reason) {
        inlineFailed = reason;
        return null;
    }

    private FullInterpreterContext inlineMethodCommon(IRMethod methodToInline, long callsiteId, int classToken, boolean cloneHost) {
        alreadyHasInline = true;
        // FIXME: Tried prepareFullBuild here and for methodToInline and a couple of missing callsiteid errors happened in spec:ruby:fast
        if (getFullInterpreterContext() == null) return inlineFailed("inline into startup interpreter scope");

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
        if (!methodToInline.getClosures().isEmpty()) return inlineFailed("inline a method which contains nested closures");
        FullInterpreterContext newContext = getFullInterpreterContext().duplicate();
        BasicBlock basicBlock = newContext.findBasicBlockOf(callsiteId);
        CallBase call = (CallBase) basicBlock.siteOf(callsiteId);  // we know it is callBase and not a yield
        RubyModule implClass = compilable.getImplementationClass();

        String error = new CFGInliner(newContext).inlineMethod(methodToInline, implClass, classToken, basicBlock, call, cloneHost);

        return error == null ? newContext : inlineFailed(error);
    }

    public void inlineMethod(IRMethod methodToInline, long callsiteId, int classToken, boolean cloneHost) {
        if (alreadyHasInline) return;

        FullInterpreterContext newContext = inlineMethodCommon(methodToInline, callsiteId, classToken, cloneHost);
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

    public void inlineMethodJIT(IRMethod methodToInline, long callsiteId, int classToken, boolean cloneHost) {
        if (alreadyHasInline) return;

        FullInterpreterContext newContext = inlineMethodCommon(methodToInline, callsiteId, classToken, cloneHost);
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

    public void inlineMethodCompiled(IRMethod methodToInline, long callsiteId, int classToken, boolean cloneHost) {
        if (alreadyHasInline) return;

        FullInterpreterContext newContext = inlineMethodCommon(methodToInline, callsiteId, classToken, cloneHost);
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

    /** Record a begin block.  Only eval and script body scopes support this */
    public void recordBeginBlock(IRClosure beginBlockClosure) {
        throw new RuntimeException("BEGIN blocks cannot be added to: " + this.getClass().getName());
    }

    public List<IRClosure> getBeginBlocks() {
        return null;
    }

    public List<IRClosure> getEndBlocks() {
        return null;
    }

    // Enebo: We should just make n primitive int and not take the hash hit
    protected int allocateNextPrefixedName(String prefix) {
        int index = getPrefixCountSize(prefix);

        nextVarIndex.put(prefix, index + 1);

        return index;
    }

    protected void resetVariableCounter(String prefix) {
        nextVarIndex.remove(prefix);
    }

    public Map<String, Integer> getVarIndices() {
        return nextVarIndex;
    }

    protected int getPrefixCountSize(String prefix) {
        Integer index = nextVarIndex.get(prefix);

        if (index == null) return 0;

        return index.intValue();
    }

    public int getNextClosureId() {
        nextClosureIndex++;

        return nextClosureIndex;
    }

    public boolean isBeginEndBlock() {
        return false;
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

    public boolean isFlipScope() {
        return true;
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

    public boolean needsFrame() {
        boolean bindingHasEscaped = bindingHasEscaped();
        boolean requireFrame = bindingHasEscaped || usesEval();

        for (IRFlags flag : getFlags()) {
            switch (flag) {
                case BINDING_HAS_ESCAPED:
                case CAN_CAPTURE_CALLERS_BINDING:
                case REQUIRES_LASTLINE:
                case REQUIRES_BACKREF:
                case REQUIRES_VISIBILITY:
                case REQUIRES_BLOCK:
                case REQUIRES_SELF:
                case REQUIRES_METHODNAME:
                case REQUIRES_CLASS:
                case USES_EVAL:
                case USES_ZSUPER:
                    requireFrame = true;
            }
        }

        return requireFrame;
    }

    public boolean needsOnlyBackref() {
        boolean backrefSeen = false;
        for (IRFlags flag : getFlags()) {
            switch (flag) {
                case BINDING_HAS_ESCAPED:
                case CAN_CAPTURE_CALLERS_BINDING:
                case REQUIRES_LASTLINE:
                case REQUIRES_VISIBILITY:
                case REQUIRES_BLOCK:
                case REQUIRES_SELF:
                case REQUIRES_METHODNAME:
                case REQUIRES_CLASS:
                case USES_EVAL:
                case USES_ZSUPER:
                    return false;
                case REQUIRES_BACKREF:
                    backrefSeen = true;
                    break;
            }
        }

        return backrefSeen;
    }

    public boolean reuseParentScope() {
        return getFlags().contains(IRFlags.REUSE_PARENT_DYNSCOPE);
    }

    public boolean needsBinding() {
        return reuseParentScope() || !getFlags().contains(IRFlags.DYNSCOPE_ELIMINATED);
    }

    // FIXME: This should become some heuristic later
    public boolean inliningAllowed() {
        return !alreadyHasInline;
    }
}
