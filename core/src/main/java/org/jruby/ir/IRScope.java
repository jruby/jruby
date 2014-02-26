package org.jruby.ir;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jruby.ParseResult;
import org.jruby.RubyInstanceConfig;

import org.jruby.RubyModule;
import org.jruby.ir.dataflow.DataFlowProblem;
import org.jruby.ir.instructions.CallBase;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.ReceiveSelfInstr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.instructions.Specializeable;
import org.jruby.ir.instructions.ThreadPollInstr;
import org.jruby.ir.operands.UnboxedBoolean;
import org.jruby.ir.operands.Fixnum;
import org.jruby.ir.operands.Float;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Self;
import org.jruby.ir.operands.TemporaryCurrentModuleVariable;
import org.jruby.ir.operands.TemporaryCurrentScopeVariable;
import org.jruby.ir.operands.TemporaryBooleanVariable;
import org.jruby.ir.operands.TemporaryFixnumVariable;
import org.jruby.ir.operands.TemporaryFloatVariable;
import org.jruby.ir.operands.TemporaryLocalReplacementVariable;
import org.jruby.ir.operands.TemporaryLocalVariable;
import org.jruby.ir.operands.TemporaryVariableType;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.passes.AddLocalVarLoadStoreInstructions;
import org.jruby.ir.passes.CompilerPass;
import org.jruby.ir.passes.CompilerPassScheduler;
import org.jruby.ir.passes.DeadCodeElimination;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.passes.UnboxingPass;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.ir.representations.CFG;
import org.jruby.ir.representations.CFGLinearizer;
import org.jruby.ir.transformations.inlining.CFGInliner;
import org.jruby.parser.StaticScope;
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
    private static final Logger LOG = LoggerFactory.getLogger("IRScope");

    private static Integer globalScopeCount = 0;

    /** Unique global scope id */
    private int scopeId;

    /** Name */
    private String name;

    /** File within which this scope has been defined */
    private final String fileName;

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
    private StaticScope staticScope;

    /** List of IR instructions for this method */
    private List<Instr> instrList;

    /** Control flow graph representation of this method's instructions */
    private CFG cfg;

    /** Local variables defined in this scope */
    private Set<Variable> definedLocalVars;

    /** Local variables used in this scope */
    private Set<Variable> usedLocalVars;

    /** Map of name -> dataflow problem */
    private Map<String, DataFlowProblem> dfProbs;

    private Instr[] linearizedInstrArray;
    private List<BasicBlock> linearizedBBList;
    private Map<Integer, Integer> rescueMap;
    protected int temporaryVariableIndex;
    protected int floatVariableIndex;
    protected int fixnumVariableIndex;
    protected int booleanVariableIndex;

    /** Keeps track of types of prefix indexes for variables and labels */
    private Map<String, Integer> nextVarIndex;

    private int instructionsOffsetInfoPersistenceBuffer = -1;
    private IRReaderDecoder persistenceStore = null;
    private TemporaryLocalVariable currentModuleVariable;
    private TemporaryLocalVariable currentScopeVariable;

    Map<String, LocalVariable> localVars;
    Map<String, LocalVariable> evalScopeVars;

    EnumSet<IRFlags> flags = EnumSet.noneOf(IRFlags.class);

    /** Have scope flags been computed? */
    private boolean flagsComputed;

    /** # of thread poll instrs added to this scope */
    private int threadPollInstrsCount;

    /** Should we re-run compiler passes -- yes after we've inlined, for example */
    private boolean relinearizeCFG;

    private IRManager manager;

    // Used by cloning code
    protected IRScope(IRScope s, IRScope lexicalParent) {
        this.lexicalParent = lexicalParent;
        this.manager = s.manager;
        this.fileName = s.fileName;
        this.lineNumber = s.lineNumber;
        this.staticScope = s.staticScope;
        this.threadPollInstrsCount = s.threadPollInstrsCount;
        this.nextClosureIndex = s.nextClosureIndex;
        this.temporaryVariableIndex = s.temporaryVariableIndex;
        this.floatVariableIndex = s.floatVariableIndex;
        this.instrList = new ArrayList<Instr>();
        this.nestedClosures = new ArrayList<IRClosure>();
        this.dfProbs = new HashMap<String, DataFlowProblem>();
        this.nextVarIndex = new HashMap<String, Integer>(); // SSS FIXME: clone!
        this.cfg = null;
        this.linearizedInstrArray = null;
        this.linearizedBBList = null;

        this.flagsComputed = s.flagsComputed;
        this.flags = s.flags.clone();

        this.localVars = new HashMap<String, LocalVariable>(s.localVars);
        synchronized(globalScopeCount) { this.scopeId = globalScopeCount++; }
        this.relinearizeCFG = false;

        setupLexicalContainment();
    }

    public IRScope(IRManager manager, IRScope lexicalParent, String name,
            String fileName, int lineNumber, StaticScope staticScope) {
        this.manager = manager;
        this.lexicalParent = lexicalParent;
        this.name = name;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.staticScope = staticScope;
        this.threadPollInstrsCount = 0;
        this.nextClosureIndex = 0;
        this.temporaryVariableIndex = -1;
        this.floatVariableIndex = -1;
        this.instrList = new ArrayList<Instr>();
        this.nestedClosures = new ArrayList<IRClosure>();
        this.dfProbs = new HashMap<String, DataFlowProblem>();
        this.nextVarIndex = new HashMap<String, Integer>();
        this.cfg = null;
        this.linearizedInstrArray = null;
        this.linearizedBBList = null;
        this.flagsComputed = false;
        flags.remove(CAN_RECEIVE_BREAKS);
        flags.remove(CAN_RECEIVE_NONLOCAL_RETURNS);
        flags.remove(HAS_BREAK_INSTRS);
        flags.remove(HAS_END_BLOCKS);
        flags.remove(HAS_EXPLICIT_CALL_PROTOCOL);
        flags.remove(HAS_LOOPS);
        flags.remove(HAS_NONLOCAL_RETURNS);
        flags.remove(HAS_UNUSED_IMPLICIT_BLOCK_ARG);
        flags.remove(RECEIVES_KEYWORD_ARGS);

        // These flags are true by default!
        flags.add(CAN_CAPTURE_CALLERS_BINDING);
        flags.add(BINDING_HAS_ESCAPED);
        flags.add(USES_EVAL);
        flags.add(USES_BACKREF_OR_LASTLINE);
        flags.add(USES_ZSUPER);

        this.localVars = new HashMap<String, LocalVariable>();
        synchronized(globalScopeCount) { this.scopeId = globalScopeCount++; }
        this.relinearizeCFG = false;

        setupLexicalContainment();
    }

    private final void setupLexicalContainment() {
        if (manager.isDryRun() || RubyInstanceConfig.IR_WRITING) {
            lexicalChildren = new ArrayList<IRScope>();
            if (lexicalParent != null) lexicalParent.addChildScope(this);
        }
    }

    private boolean hasListener() {
        return manager.getIRScopeListener() != null;
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

    protected void addChildScope(IRScope scope) {
        lexicalChildren.add(scope);
    }

    public List<IRScope> getLexicalScopes() {
        return lexicalChildren;
    }

    public void initNestedClosures() {
        this.nestedClosures = new ArrayList<IRClosure>();
    }

    public void addClosure(IRClosure c) {
        nestedClosures.add(c);
    }

    public Instr getLastInstr() {
        return instrList.get(instrList.size() - 1);
    }

    public void addInstrAtBeginning(Instr instr) {
        instr.computeScopeFlags(this);

        if (hasListener()) manager.getIRScopeListener().addedInstr(this, instr, 0);

        instrList.add(0, instr);
    }

    public void addInstr(Instr instr) {
        if (instr instanceof ThreadPollInstr) threadPollInstrsCount++;

        instr.computeScopeFlags(this);

        if (hasListener()) manager.getIRScopeListener().addedInstr(this, instr, instrList.size());

        instrList.add(instr);
    }

    public LocalVariable getNewFlipStateVariable() {
        return getLocalVariable("%flip_" + allocateNextPrefixedName("%flip"), 0);
    }

    public void initFlipStateVariable(Variable v, Operand initState) {
        // Add it to the beginning
        instrList.add(0, new CopyInstr(v, initState));
    }

    public Label getNewLabel(String prefix) {
        return new Label(prefix, allocateNextPrefixedName(prefix));
    }

    public Label getNewLabel() {
        return getNewLabel("LBL");
    }

    public List<IRClosure> getClosures() {
        return nestedClosures;
    }

    public IRManager getManager() {
        return manager;
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
    public IRScope getNearestModuleReferencingScope() {
        IRScope current = this;

        while (!(current instanceof IRModuleBody)) {
            // When eval'ing, we dont have a lexical view of what module we are nested in
            // because binding_eval, class_eval, module_eval, instance_eval can switch
            // around the lexical scope for evaluation to be something else.
            if (current == null || current instanceof IREvalScript) return null;

            current = current.getLexicalParent();
        }

        return current;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) { // This is for IRClosure and IRMethod ;(
        this.name = name;
    }

    public String getFileName() {
        return fileName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Returns the top level scope
     */
    public IRScope getTopLevelScope() {
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

    public boolean usesBackrefOrLastline() {
        return flags.contains(USES_BACKREF_OR_LASTLINE);
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

    public CFG buildCFG() {
        CFG newCFG = new CFG(this);
        newCFG.build(getInstrs());
        // Clear out instruction list after CFG has been built.
        this.instrList = null;

        setCFG(newCFG);

        return newCFG;
    }

    protected void setCFG(CFG cfg) {
       this.cfg = cfg;
    }

    public CFG getCFG() {
        return cfg;
    }

    private Instr[] prepareInstructionsForInterpretation() {
        checkRelinearization();

        if (linearizedInstrArray != null) return linearizedInstrArray; // Already prepared

        setupLinearization();

        // Set up IPCs
        List<Instr> newInstrs = new ArrayList<Instr>();
        HashMap<Label, Integer> labelIPCMap = new HashMap<Label, Integer>();
        int ipc = 0;
        for (BasicBlock b: linearizedBBList) {
            Label l = b.getLabel();
            labelIPCMap.put(l, ipc);
            // This assumes if multiple equal/same labels exist which are scattered around the scope
            // must be the same Java instance or only this one will get a targetPC set.
            l.setTargetPC(ipc);
            List<Instr> bbInstrs = b.getInstrs();
            int bbInstrsLength = bbInstrs.size();
            for (int i = 0; i < bbInstrsLength; i++) {
                Instr instr = bbInstrs.get(i);

                if (instr instanceof Specializeable) {
                    instr = ((Specializeable) instr).specializeForInterpretation();
                    bbInstrs.set(i, instr);
                }

                if (!(instr instanceof ReceiveSelfInstr)) {
                    newInstrs.add(instr);
                    instr.setIPC(ipc);
                    ipc++;
                }
            }
        }

        // System.out.println("SCOPE: " + getName());
        // System.out.println("INSTRS: " + cfg().toStringInstrs());

        // Exit BB ipc
        cfg().getExitBB().getLabel().setTargetPC(ipc + 1);

        // Set up rescue map
        setupRescueMap();

        linearizedInstrArray = newInstrs.toArray(new Instr[newInstrs.size()]);
        return linearizedInstrArray;
    }

    public void setupRescueMap() {
        this.rescueMap = new HashMap<Integer, Integer>();
        for (BasicBlock b : linearizedBBList) {
            BasicBlock rescuerBB = cfg().getRescuerBBFor(b);
            int rescuerPC = (rescuerBB == null) ? -1 : rescuerBB.getLabel().getTargetPC();
            for (Instr i : b.getInstrs()) {
                rescueMap.put(i.getIPC(), rescuerPC);
            }
        }
    }

    private void runCompilerPasses(List<CompilerPass> passes) {
        // SSS FIXME: Why is this again?  Document this weirdness!
        // Forcibly clear out the shared eval-scope variable allocator each time this method executes
        initEvalScopeVariableAllocator(true);

        // SSS FIXME: We should configure different optimization levels
        // and run different kinds of analysis depending on time budget.
        // Accordingly, we need to set IR levels/states (basic, optimized, etc.)
        // ENEBO: If we use a MT optimization mechanism we cannot mutate CFG
        // while another thread is using it.  This may need to happen on a clone()
        // and we may need to update the method to return the new method.  Also,
        // if this scope is held in multiple locations how do we update all references?

        boolean unsafeScope = false;
        if (flags.contains(HAS_END_BLOCKS) || this.isBeginEndBlock()) {
            unsafeScope = true;
        } else {
            List beginBlocks = this.getBeginBlocks();
            // Ex: BEGIN {a = 1}; p a
            if (beginBlocks == null || beginBlocks.isEmpty()) {
                // Ex: eval("BEGIN {a = 1}; p a")
                // Here, the BEGIN is added to the outer script scope.
                beginBlocks = this.getNearestTopLocalVariableScope().getBeginBlocks();
                unsafeScope = beginBlocks != null && !beginBlocks.isEmpty();
            } else {
                unsafeScope = true;
            }
        }

        // All passes are disabled in scopes where BEGIN and END scopes might
        // screw around with escaped variables. Optimizing for them is not
        // worth the effort. It is simpler to just go fully safe in scopes influenced
        // by their presence.
        if (unsafeScope) {
            passes = getManager().getSafePasses(this);
        }

        CompilerPassScheduler scheduler = getManager().schedulePasses(passes);
        for (CompilerPass pass: scheduler) {
            pass.run(this);
        }

        CompilerPass pass;

        if (RubyInstanceConfig.IR_UNBOXING) {
            pass = new UnboxingPass();
            pass.run(this);
        }
    }

    private void runDeadCodeAndVarLoadStorePasses() {
        // For methods with unescaped bindings, inline the binding
        // by converting local var loads/store to tmp var loads/stores
        if (this instanceof IRMethod && !this.bindingHasEscaped() && !flags.contains(HAS_END_BLOCKS)) {
            CompilerPass pass;
            pass = new DeadCodeElimination();
            if (pass.previouslyRun(this) == null) {
                pass.run(this);
            }
            pass = new AddLocalVarLoadStoreInstructions();
            if (pass.previouslyRun(this) == null) {
                pass.run(this);
            }
        }
    }

    /** Run any necessary passes to get the IR ready for interpretation */
    public synchronized Instr[] prepareForInterpretation(boolean isLambda) {
        if (isLambda) {
            // Add a global ensure block to catch uncaught breaks
            // and throw a LocalJumpError.
            if (((IRClosure)this).addGEBForUncaughtBreaks()) {
                this.relinearizeCFG = true;
            }
        }

        checkRelinearization();

        if (linearizedInstrArray != null) return linearizedInstrArray;

        // Build CFG and run compiler passes, if necessary
        if (getCFG() == null) {
            runCompilerPasses(getManager().getCompilerPasses(this));

            // run DCE and var load/store
            runDeadCodeAndVarLoadStorePasses();
        }

        // Linearize CFG, etc.
        return prepareInstructionsForInterpretation();
    }

    /* SSS FIXME: Do we need to synchronize on this?  Cache this info in a scope field? */
    /** Run any necessary passes to get the IR ready for compilation */
    public Tuple<Instr[], Map<Integer,Label[]>> prepareForCompilation() {
        // Build CFG and run compiler passes, if necessary
        if (getCFG() == null) {
            runCompilerPasses(getManager().getJITPasses(this));

            // no DCE for now to stress-test JIT
            //runDeadCodeAndVarLoadStorePasses();
        }

        // Add this always since we dont re-JIT a previously
        // JIT-ted closure.  But, check if there are other
        // smarts available to us and eliminate adding this
        // code to every closure there is.
        //
        // Add a global ensure block to catch uncaught breaks
        // and throw a LocalJumpError.
        if (this instanceof IRClosure && ((IRClosure)this).addGEBForUncaughtBreaks()) {
            this.relinearizeCFG = true;
        }

        checkRelinearization();

        prepareInstructionsForInterpretation();

        // Set up IPCs
        // FIXME: Would be nice to collapse duplicate labels; for now, using Label[]
        HashMap<Integer, Label[]> ipcLabelMap = new HashMap<Integer, Label[]>();
        List<Instr> newInstrs = new ArrayList<Instr>();
        int ipc = 0;
        for (BasicBlock b : linearizedBBList) {
            Label l = b.getLabel();
            ipcLabelMap.put(ipc, catLabels(ipcLabelMap.get(ipc), l));
            for (Instr i : b.getInstrs()) {
                if (!(i instanceof ReceiveSelfInstr)) {
                    newInstrs.add(i);
                    i.setIPC(ipc);
                    ipc++;
                }
            }
        }

        return new Tuple<Instr[], Map<Integer,Label[]>>(newInstrs.toArray(new Instr[newInstrs.size()]), ipcLabelMap);
    }

    private void setupLinearization() {
        try {
            buildLinearization(); // FIXME: compiler passes should have done this
            depends(linearization());
        } catch (RuntimeException e) {
            LOG.error("Error linearizing cfg: ", e);
            CFG c = cfg();
            LOG.error("\nGraph:\n" + c.toStringGraph());
            LOG.error("\nInstructions:\n" + c.toStringInstrs());
            throw e;
        }
    }

    private List<Object[]> buildJVMExceptionTable() {
        List<Object[]> etEntries = new ArrayList<Object[]>();
        for (BasicBlock b: linearizedBBList) {
            BasicBlock rBB = cfg().getRescuerBBFor(b);
            if (rBB != null) {
                etEntries.add(new Object[] {b.getLabel(), rBB.getLabel(), Throwable.class});
            }
        }

        // SSS FIXME: This could be optimized by compressing entries for adjacent BBs that have identical handlers
        // This could be optimized either during generation or as another pass over the table.  But, if the JVM
        // does that already, do we need to bother with it?
        return etEntries;
    }

    private static Label[] catLabels(Label[] labels, Label cat) {
        if (labels == null) return new Label[] {cat};
        Label[] newLabels = new Label[labels.length + 1];
        System.arraycopy(labels, 0, newLabels, 0, labels.length);
        newLabels[labels.length] = cat;
        return newLabels;
    }

    public EnumSet<IRFlags> getFlags() {
        return flags;
    }

    // This can help use eliminate writes to %block that are not used since this is
    // a special local-variable, not programmer-defined local-variable
    public void computeScopeFlags() {
        if (flagsComputed) return;

        // init
        flags.remove(CAN_CAPTURE_CALLERS_BINDING);
        flags.remove(CAN_RECEIVE_BREAKS);
        flags.remove(CAN_RECEIVE_NONLOCAL_RETURNS);
        flags.remove(HAS_BREAK_INSTRS);
        flags.remove(HAS_NONLOCAL_RETURNS);
        flags.remove(USES_ZSUPER);
        flags.remove(USES_EVAL);
        flags.remove(USES_BACKREF_OR_LASTLINE);
        // NOTE: bindingHasEscaped is the crucial flag and it effectively is
        // unconditionally true whenever it has a call that receives a closure.
        // See CallBase.computeRequiresCallersBindingFlag
        if (this instanceof IREvalScript) { // for eval scopes, bindings are considered escaped ...
            flags.add(BINDING_HAS_ESCAPED);
        } else {
            flags.remove(BINDING_HAS_ESCAPED);
        }

        // Recompute flags -- we could be calling this method different times.
        // * once after IR generation and local optimizations propagates constants locally
        // * also potentially at later times after other opt passes
        if (cfg == null) {
            for (Instr i: getInstrs()) {
                i.computeScopeFlags(this);
            }
        } else {
            for (BasicBlock b: cfg.getBasicBlocks()) {
                for (Instr i: b.getInstrs()) {
                    i.computeScopeFlags(this);
                }
            }
        }

        // Compute flags for nested closures (recursively) and set derived flags.
        for (IRClosure cl : getClosures()) {
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

        flagsComputed = true;
    }

    public abstract IRScopeType getScopeType();

    @Override
    public String toString() {
        return getScopeType() + " " + getName() + "[" + getFileName() + ":" + getLineNumber() + "]";
    }

    public String toStringInstrs() {
        StringBuilder b = new StringBuilder();

        int i = 0;
        for (Instr instr : instrList) {
            if (i > 0) b.append("\n");

            b.append("  ").append(i).append('\t').append(instr);

            i++;
        }

        if (!nestedClosures.isEmpty()) {
            b.append("\n\n------ Closures encountered in this scope ------\n");
            for (IRClosure c: nestedClosures)
                b.append(c.toStringBody());
            b.append("------------------------------------------------\n");
        }

        return b.toString();
    }

    public LocalVariable getSelf() {
        return Self.SELF;
    }

    public Variable getCurrentModuleVariable() {
        // SSS: Used in only 3 cases in generated IR:
        // -> searching a constant in the inheritance hierarchy
        // -> searching a super-method in the inheritance hierarchy
        // -> looking up 'StandardError' (which can be eliminated by creating a special operand type for this)
        if (currentModuleVariable == null) {
            temporaryVariableIndex++;
            currentModuleVariable = new TemporaryCurrentModuleVariable(temporaryVariableIndex);
        }
        return currentModuleVariable;
    }

    public Variable getCurrentScopeVariable() {
        // SSS: Used in only 1 case in generated IR:
        // -> searching a constant in the lexical scope hierarchy
        if (currentScopeVariable == null) {
            temporaryVariableIndex++;
            currentScopeVariable = new TemporaryCurrentScopeVariable(temporaryVariableIndex);
        }
        return currentScopeVariable;
    }

    public abstract LocalVariable getImplicitBlockArg();

    /**
     * Get the local variables for this scope.
     * This should only be used by persistence layer.
     */
    public Map<String, LocalVariable> getLocalVariables() {
        return localVars;
    }

    /**
     * Set the local variables for this scope. This should only be used by persistence
     * layer.
     */
    // FIXME: Consider making constructor for persistence to pass in all of this stuff
    public void setLocalVariables(Map<String, LocalVariable> variables) {
        this.localVars = variables;
    }

    public void setLabelIndices(Map<String, Integer> indices) {
        nextVarIndex = indices;
    }

    public LocalVariable lookupExistingLVar(String name) {
        return localVars.get(name);
    }

    public LocalVariable findExistingLocalVariable(String name, int depth) {
        return localVars.get(name);
    }

    /**
     * Find or create a local variable.  By default, scopes are assumed to
     * only check current depth.  Blocks/Closures override this because they
     * have special nesting rules.
     */
    public LocalVariable getLocalVariable(String name, int scopeDepth) {
        LocalVariable lvar = findExistingLocalVariable(name, scopeDepth);
        if (lvar == null) {
            lvar = getNewLocalVariable(name, scopeDepth);
        } else if (lvar.getScopeDepth() != scopeDepth) {
            lvar = lvar.cloneForDepth(scopeDepth);
        }

        return lvar;
    }

    public LocalVariable getNewLocalVariable(String name, int scopeDepth) {
        assert scopeDepth == 0: "Scope depth is non-zero for new-var request " + name + " in " + this;
        LocalVariable lvar = new LocalVariable(name, scopeDepth, getStaticScope().addVariable(name));
        localVars.put(name, lvar);
        return lvar;
    }

    protected void initEvalScopeVariableAllocator(boolean reset) {
        if (reset || evalScopeVars == null) evalScopeVars = new HashMap<String, LocalVariable>();
    }

    public TemporaryLocalVariable getNewTemporaryVariable() {
        return getNewTemporaryVariable(TemporaryVariableType.LOCAL);
    }

    public TemporaryLocalVariable getNewTemporaryVariableFor(LocalVariable var) {
        temporaryVariableIndex++;
        return new TemporaryLocalReplacementVariable(var.getName(), temporaryVariableIndex);
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
                // Shares var index with locals
                temporaryVariableIndex++;
                return new TemporaryBooleanVariable(temporaryVariableIndex);
            }
            case LOCAL: {
                temporaryVariableIndex++;
                return new TemporaryLocalVariable(temporaryVariableIndex);
            }
        }

        throw new RuntimeException("Invalid temporary variable being alloced in this scope: " + type);
    }

    public void setTemporaryVariableCount(int count) {
        temporaryVariableIndex = count + 1;
    }

    public TemporaryLocalVariable getNewUnboxedVariable(Class type) {
        TemporaryVariableType varType;
        if (type == Float.class) {
            varType = TemporaryVariableType.FLOAT;
        } else if (type == Fixnum.class) {
            varType = TemporaryVariableType.FIXNUM;
        } else if (type == UnboxedBoolean.class) {
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
    public Variable getNewInlineVariable(String inlinePrefix, Variable v) {
        if (v instanceof LocalVariable) {
            LocalVariable lv = (LocalVariable)v;
            return getLocalVariable(inlinePrefix + lv.getName(), lv.getScopeDepth());
        } else {
            return getNewTemporaryVariable();
        }
    }

    public int getThreadPollInstrsCount() {
        return threadPollInstrsCount;
    }

    public int getLocalVariablesCount() {
        return localVars.size();
    }

    public int getUsedVariablesCount() {
        // System.out.println("For " + this + ", # lvs: " + nextLocalVariableSlot);
        // # local vars, # flip vars
        //
        // SSS FIXME: When we are opting local var access,
        // no need to allocate local var space except when we have been asked to!
        return getLocalVariablesCount() + getPrefixCountSize("%flip");
    }

    public void setUpUseDefLocalVarMaps() {
        definedLocalVars = new java.util.HashSet<Variable>();
        usedLocalVars = new java.util.HashSet<Variable>();
        for (BasicBlock bb : cfg().getBasicBlocks()) {
            for (Instr i : bb.getInstrs()) {
                for (Variable v : i.getUsedVariables()) {
                    if (v instanceof LocalVariable) usedLocalVars.add(v);
                }

                if (i instanceof ResultInstr) {
                    Variable v = ((ResultInstr) i).getResult();

                    if (v instanceof LocalVariable) definedLocalVars.add(v);
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

    public void setDataFlowSolution(String name, DataFlowProblem p) {
        dfProbs.put(name, p);
    }

    public DataFlowProblem getDataFlowSolution(String name) {
        return dfProbs.get(name);
    }

    // This should only be used to do pre-cfg opts and to build the CFG.
    // Everyone else should use the CFG.
    public List<Instr> getInstrs() {
        if (persistenceStore != null) {
            instrList = persistenceStore.decodeInstructionsAt(this, instructionsOffsetInfoPersistenceBuffer);
        }
        if (cfg != null) throw new RuntimeException("Please use the CFG to access this scope's instructions.");
        return instrList;
    }

    public Instr[] getInstrsForInterpretation() {
        return linearizedInstrArray;
    }

    public Instr[] getInstrsForInterpretation(boolean isLambda) {
        if (linearizedInstrArray == null) {
            prepareForInterpretation(isLambda);
        }
        return linearizedInstrArray;
    }

    public void resetLinearizationData() {
        linearizedBBList = null;
        relinearizeCFG = false;
    }

    public void checkRelinearization() {
        if (relinearizeCFG) resetLinearizationData();
    }

    public List<BasicBlock> buildLinearization() {
        checkRelinearization();

        if (linearizedBBList != null) return linearizedBBList; // Already linearized

        linearizedBBList = CFGLinearizer.linearize(cfg);

        return linearizedBBList;
    }

    public Map<Integer, Integer> getRescueMap() {
        return this.rescueMap;
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

    public CFG cfg() {
        assert cfg != null: "Trying to access build before build started";
        return cfg;
    }

    public void resetDFProblemsState() {
        dfProbs = new HashMap<String, DataFlowProblem>();
        for (IRClosure c: nestedClosures) c.resetDFProblemsState();
    }

    public void resetState() {
        relinearizeCFG = true;
        linearizedInstrArray = null;
        cfg.resetState();

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
        rescueMap = null;

        // Reset dataflow problems state
        resetDFProblemsState();
    }

    public void inlineMethod(IRScope method, RubyModule implClass, int classToken, BasicBlock basicBlock, CallBase call, boolean cloneHost) {
        // Inline
        depends(cfg());
        new CFGInliner(cfg).inlineMethod(method, implClass, classToken, basicBlock, call, cloneHost);

        // Reset state
        resetState();

        // Re-run opts
        for (CompilerPass pass: getManager().getInliningCompilerPasses(this)) {
            pass.run(this);
        }
    }

    public void resetCFG() {
        cfg = null;
    }

    /* Record a begin block -- not all scope implementations can handle them */
    public void recordBeginBlock(IRClosure beginBlockClosure) {
        throw new RuntimeException("BEGIN blocks cannot be added to: " + this.getClass().getName());
    }

    /* Record an end block -- not all scope implementations can handle them */
    public void recordEndBlock(IRClosure endBlockClosure) {
        throw new RuntimeException("END blocks cannot be added to: " + this.getClass().getName());
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

    // This is how IR Persistence can re-read existing saved labels and reset
    // scope back to proper index.
    public void setPrefixedNameIndexTo(String prefix, int newIndex) {
        int index = getPrefixCountSize(prefix);

        nextVarIndex.put(prefix, index);
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
     * Does this scope represent a module body?  (SSS FIXME: what about script or eval script bodies?)
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

    public void savePersistenceInfo(int offset, IRReaderDecoder file) {
        instructionsOffsetInfoPersistenceBuffer = offset;
        persistenceStore = file;
    }
}
