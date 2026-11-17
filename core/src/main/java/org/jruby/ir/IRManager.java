package org.jruby.ir;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;

import org.jcodings.specific.UTF8Encoding;
import org.jruby.ParseResult;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.RubySymbol;
import org.jruby.ast.DefNode;
import org.jruby.ast.IScopingNode;
import org.jruby.ast.ModuleNode;
import org.jruby.ast.Node;
import org.jruby.ast.RootNode;
import org.jruby.ext.coverage.CoverageData;
import org.jruby.ir.builder.IRBuilderFactory;
import org.jruby.ir.builder.LazyMethodDefinitionAST;
import org.jruby.ir.instructions.LineNumberInstr;
import org.jruby.ir.instructions.ReceiveSelfInstr;
import org.jruby.ir.instructions.ToggleBacktraceInstr;
import org.jruby.ir.interpreter.FullInterpreterContext;
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
import java.util.concurrent.atomic.AtomicLong;

import org.jruby.ir.passes.DeadCodeElimination;
import org.jruby.ir.passes.OptimizeDelegationPass;
import org.jruby.ir.passes.OptimizeDynScopesPass;
import org.jruby.ir.util.IGVInstrListener;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.FileResource;
import org.jruby.util.JRubyFile;
import org.jruby.util.cli.Options;

import static org.jruby.api.Convert.asSymbol;
import static org.jruby.ir.IRFlags.REQUIRES_DYNSCOPE;

public class IRManager {
    public static final String SAFE_COMPILER_PASSES = "";
    public static final String DEFAULT_BUILD_PASSES = "";
    public static final String DEFAULT_JIT_PASSES = "LocalOptimizationPass,DeadCodeElimination,OptimizeDynScopesPass,OptimizeDelegationPass,AddCallProtocolInstructions,AddMissingInitsPass";
    public static final String DEFAULT_INLINING_COMPILER_PASSES = "LocalOptimizationPass";
    
    public static final boolean IR_INLINER = Options.IR_INLINER.load();
    public static final int IR_INLINER_THRESHOLD = Options.IR_INLINER_THRESHOLD.load();
    public static final boolean IR_INLINER_VERBOSE = Options.IR_INLINER_VERBOSE.load();

    private final CompilerPass deadCodeEliminationPass = new DeadCodeElimination();
    private final CompilerPass optimizeDynScopesPass = new OptimizeDynScopesPass();
    private final CompilerPass optimizeDelegationPass = new OptimizeDelegationPass();

    private final static ByteList OBJECT = new ByteList(new byte[] {'O', 'b', 'j', 'e', 'c', 't'});

    private int dummyMetaClassCount = 0;
    private final IRModuleBody object;
    private final Nil nil = new Nil();
    private final Boolean tru = new Boolean(true);
    private final Boolean fals = new Boolean(false);
    private final BuiltinClass arrayClass = new BuiltinClass(BuiltinClass.Type.ARRAY);
    private final BuiltinClass hashClass = new BuiltinClass(BuiltinClass.Type.HASH);
    private final BuiltinClass objectClass = new BuiltinClass(BuiltinClass.Type.OBJECT);

    private final BuiltinClass symbolClass = new BuiltinClass(BuiltinClass.Type.SYMBOL);
    private final StandardError standardError = new StandardError();
    public final ToggleBacktraceInstr needsBacktrace = new ToggleBacktraceInstr(true);
    public final ToggleBacktraceInstr needsNoBacktrace = new ToggleBacktraceInstr(false);

    // Listeners for debugging and testing of IR
    private final Set<CompilerPassListener> passListeners = new HashSet<CompilerPassListener>();
    private final CompilerPassListener defaultListener = new BasicCompilerPassListener();

    private InstructionsListener instrsListener = null;
    private IRScopeListener irScopeListener = null;

    // FIXME: Eventually make these attrs into either a) set b) part of state machine
    private final List<CompilerPass> compilerPasses;
    private final List<CompilerPass> inliningCompilerPasses;
    private final List<CompilerPass> jitPasses;
    private final List<CompilerPass> safePasses;
    private final RubyInstanceConfig config;
    public final Ruby runtime;
    private IRBuilderFactory builderFactory;
    private AtomicLong callSiteCounter = new AtomicLong(1);

    public IRManager(Ruby runtime, RubyInstanceConfig config) {
        this.runtime = runtime;
        this.config = config;
        object = new IRClassBody(this, null, OBJECT, 0, null, false);
        compilerPasses = CompilerPass.getPassesFromString(RubyInstanceConfig.IR_COMPILER_PASSES, DEFAULT_BUILD_PASSES);
        inliningCompilerPasses = CompilerPass.getPassesFromString(RubyInstanceConfig.IR_COMPILER_PASSES, DEFAULT_INLINING_COMPILER_PASSES);
        jitPasses = CompilerPass.getPassesFromString(RubyInstanceConfig.IR_JIT_PASSES, DEFAULT_JIT_PASSES);
        safePasses = CompilerPass.getPassesFromString(null, SAFE_COMPILER_PASSES);

        if (RubyInstanceConfig.IR_DEBUG_IGV != null) instrsListener = new IGVInstrListener();
    }

    public void setBuilderFactory(IRBuilderFactory builderFactory) {
        this.builderFactory = builderFactory;
    }

    public IRBuilderFactory getBuilderFactory() {
        return builderFactory;
    }

    public Ruby getRuntime() {
        return runtime;
    }

    public Nil getNil() {
        return nil;
    }

    public StandardError getStandardError() {
        return standardError;
    }

    public BuiltinClass getArrayClass() {
        return arrayClass;
    }

    public BuiltinClass getObjectClass() {
        return objectClass;
    }

    public BuiltinClass getHashClass() {
        return hashClass;
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

    public ToggleBacktraceInstr needsBacktrace(boolean needsIt) {
        return needsIt ? needsBacktrace : needsNoBacktrace;
    }

    public CompilerPassScheduler schedulePasses() {
        return schedulePasses(compilerPasses);
    }

    public static CompilerPassScheduler schedulePasses(final List<CompilerPass> passes) {
        CompilerPassScheduler scheduler = new CompilerPassScheduler() {
            private final Iterator<CompilerPass> iterator;
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

    private static final int CLOSURE_PREFIX_CACHE_SIZE = 300; // arbtrary.  one library in rails 6 uses over 270 in one scope...
    private final String[] closurePrefixCache = new String[CLOSURE_PREFIX_CACHE_SIZE];

    public String getClosurePrefix(int closureId) {
        if (closureId >= CLOSURE_PREFIX_CACHE_SIZE) {
            return "CL" + closureId + "_LBL";
        }

        String prefix = closurePrefixCache[closureId];

        if (prefix == null) {
            prefix = "CL" + closureId + "_LBL";
            closurePrefixCache[closureId] = prefix;
        }

        return prefix;
    }


    private static final int FIXNUM_CACHE_HALF_SIZE = 16384;
    private final Fixnum[] fixnums = new Fixnum[2 * FIXNUM_CACHE_HALF_SIZE];

    // Fixnum operand caches end up providing twice the value since it will share the same instance of
    // the same logical fixnum, but since immutable literals cache the actual RubyFixnum they end up
    // sharing all occurences of those in Ruby code as well.h
    public Fixnum newFixnum(long value) {
        if (value < -FIXNUM_CACHE_HALF_SIZE || value > FIXNUM_CACHE_HALF_SIZE) return new Fixnum(value);

        int adjustedValue = (int) value + FIXNUM_CACHE_HALF_SIZE; // adjust to where 0 is in signed range.

        Fixnum fixnum;

        if (adjustedValue >= 0 && adjustedValue < fixnums.length) {
            fixnum = fixnums[adjustedValue];

            if (fixnum == null) {
                fixnum = new Fixnum(value);
                fixnums[adjustedValue] = fixnum;
            }
        } else {
            fixnum = new Fixnum(value);
        }

        return fixnum;
    }

    public LineNumberInstr newLineNumber(int line) {
        if (line >= lineNumbers.length-1) growLineNumbersPool(line);

        // We do not cache negative line numbers as they are very rare...
        if (line < 0) return new LineNumberInstr((line));

        LineNumberInstr tempVar = lineNumbers[line];

        if (tempVar == null) {
            tempVar = new LineNumberInstr(line);
            lineNumbers[line] = tempVar;
        }

        return tempVar;

    }

    private final ReceiveSelfInstr receiveSelfInstr = new ReceiveSelfInstr(Self.SELF);

    public ReceiveSelfInstr getReceiveSelfInstr() {
        return receiveSelfInstr;
    }

    private LineNumberInstr[] lineNumbers = new LineNumberInstr[3000];

    protected LineNumberInstr[] growLineNumbersPool(int index) {
        int newLength = index * 2;
        LineNumberInstr[] newPool = new LineNumberInstr[newLength];

        System.arraycopy(lineNumbers, 0, newPool, 0, lineNumbers.length);
        lineNumbers = newPool;
        return newPool;
    }


    public void removeListener(IRScopeListener listener) {
        if (irScopeListener.equals(listener)) irScopeListener = null;
    }

    public RubySymbol getMetaClassName() {
        return runtime.newSymbol("<DUMMY_MC:" + dummyMetaClassCount++ + ">");
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

    /**
     * Temporarily provided for loading/storing a normal local as int on JVM; interpreter will still box as Integer.
     * @param index
     * @return
     */
    public TemporaryLocalVariable newTemporaryIntVariable(int index) {
        if (index >= temporaryLocalVariables.length-1) growTemporaryVariablePool(index);

        TemporaryLocalVariable tempVar = temporaryLocalVariables[index];

        if (tempVar == null || !(tempVar instanceof TemporaryIntVariable)) {
            tempVar = new TemporaryIntVariable(index);
            temporaryLocalVariables[index] = tempVar;
        }

        return tempVar;
    }

    /**
     * For scopes that don't require a dynamic scope we can run DCE and some other passes which cannot
     * be stymied by escaped bindings.
     */
    protected void optimizeIfSimpleScope(FullInterpreterContext fic) {
        // We cannot pick the passes if we want an explicit set to run.
        if (RubyInstanceConfig.IR_COMPILER_PASSES != null) return;

        EnumSet<IRFlags> flags = fic.getFlags();

        if (!flags.contains(REQUIRES_DYNSCOPE)) {
            if (fic.getScope().receivesClosureArg()) optimizeDelegationPass.run(fic);
            deadCodeEliminationPass.run(fic);
            optimizeDynScopesPass.run(fic);
        }
    }

    public RubyInstanceConfig getInstanceConfig() {
        return config;
    }

    // FIXME: needs info for specialized method selection.
    // FIXME: should allow non-classpath loading for easier debugging and hacking.
    public IRMethod loadInternalMethod(ThreadContext context, IRubyObject self, String method) {
        try {
            RubyModule type = self.getMetaClass();
            String fileName = "classpath:/jruby/ruby_implementations/" + type + "/" + method + ".rb";
            FileResource file = JRubyFile.createResourceAsFile(context.runtime, fileName);
            ParseResult parseResult = parse(context, file, fileName);
            // FIXME: PRISM: This does not work on prism but method inlining is current not working anyways.
            IScopingNode scopeNode = (IScopingNode) ((RootNode) parseResult.getAST()).childNodes().get(0);
            scopeNode.getScope().setModule(type);
            DefNode defNode = (DefNode) scopeNode.getBodyNode();
            IRScriptBody script = new IRScriptBody(this, parseResult.getFile(), parseResult.getStaticScope());
            IRModuleBody containingScope;
            if (scopeNode instanceof ModuleNode) {
                containingScope = new IRModuleBody(this, script, scopeNode.getCPath().getName().getBytes(), 0, scopeNode.getScope(), false);
            } else {
                containingScope = new IRClassBody(this, script, scopeNode.getCPath().getName().getBytes(), 0, scopeNode.getScope(), false);
            }
            // FIXME: Broken with Prism
            LazyMethodDefinitionAST defn = new LazyMethodDefinitionAST(defNode);
            IRMethod newMethod = new IRMethod(this, containingScope, defn, asSymbol(context, method).getBytes(), true, 0, defNode.getScope(), CoverageData.NONE);

            newMethod.prepareForCompilation();

            return newMethod;
        } catch (IOException e) {
            e.printStackTrace(); // FIXME: More elegantly handle broken internal implementations
            return null;
        }
    }

    private ParseResult parse(ThreadContext context, FileResource file, String fileName) throws IOException {
        try (InputStream stream = file.openInputStream()) {
            return context.runtime.getParserManager().parseFile(fileName, 0, stream, UTF8Encoding.INSTANCE);
        }
    }

    public BuiltinClass getSymbolClass() {
        return symbolClass;
    }

    public long nextCallSiteID() {
        return callSiteCounter.incrementAndGet();
    }
}
