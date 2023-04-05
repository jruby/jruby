package org.jruby.ir.builder;

import org.jcodings.Encoding;
import org.jruby.EvalType;
import org.jruby.ParseResult;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubySymbol;
import org.jruby.ast.RootNode;
import org.jruby.ext.coverage.CoverageData;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IREvalScript;
import org.jruby.ir.IRFlags;
import org.jruby.ir.IRFor;
import org.jruby.ir.IRManager;
import org.jruby.ir.IRMetaClassBody;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRModuleBody;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScopeType;
import org.jruby.ir.IRScriptBody;
import org.jruby.ir.instructions.AliasInstr;
import org.jruby.ir.instructions.BFalseInstr;
import org.jruby.ir.instructions.BNEInstr;
import org.jruby.ir.instructions.BNilInstr;
import org.jruby.ir.instructions.BTrueInstr;
import org.jruby.ir.instructions.BUndefInstr;
import org.jruby.ir.instructions.BuildCompoundStringInstr;
import org.jruby.ir.instructions.CallInstr;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.DefineInstanceMethodInstr;
import org.jruby.ir.instructions.ExceptionRegionEndMarkerInstr;
import org.jruby.ir.instructions.ExceptionRegionStartMarkerInstr;
import org.jruby.ir.instructions.GetClassVarContainerModuleInstr;
import org.jruby.ir.instructions.GetClassVariableInstr;
import org.jruby.ir.instructions.GetFieldInstr;
import org.jruby.ir.instructions.GetGlobalVariableInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.JumpInstr;
import org.jruby.ir.instructions.LabelInstr;
import org.jruby.ir.instructions.LineNumberInstr;
import org.jruby.ir.instructions.LoadBlockImplicitClosureInstr;
import org.jruby.ir.instructions.LoadFrameClosureInstr;
import org.jruby.ir.instructions.LoadImplicitClosureInstr;
import org.jruby.ir.instructions.NopInstr;
import org.jruby.ir.instructions.PutClassVariableInstr;
import org.jruby.ir.instructions.PutConstInstr;
import org.jruby.ir.instructions.PutFieldInstr;
import org.jruby.ir.instructions.PutGlobalVarInstr;
import org.jruby.ir.instructions.ReceiveArgBase;
import org.jruby.ir.instructions.ReceiveJRubyExceptionInstr;
import org.jruby.ir.instructions.ReceiveKeywordArgInstr;
import org.jruby.ir.instructions.ReceiveKeywordRestArgInstr;
import org.jruby.ir.instructions.ReceiveRestArgInstr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.instructions.ReturnInstr;
import org.jruby.ir.instructions.RuntimeHelperCall;
import org.jruby.ir.instructions.SearchConstInstr;
import org.jruby.ir.instructions.SearchModuleForConstInstr;
import org.jruby.ir.instructions.ThreadPollInstr;
import org.jruby.ir.instructions.TraceInstr;
import org.jruby.ir.instructions.ZSuperInstr;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.operands.Boolean;
import org.jruby.ir.operands.CurrentScope;
import org.jruby.ir.operands.DepthCloneable;
import org.jruby.ir.operands.Fixnum;
import org.jruby.ir.operands.Hash;
import org.jruby.ir.operands.Integer;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.MutableString;
import org.jruby.ir.operands.Nil;
import org.jruby.ir.operands.NthRef;
import org.jruby.ir.operands.NullBlock;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.ScopeModule;
import org.jruby.ir.operands.Splat;
import org.jruby.ir.operands.Symbol;
import org.jruby.ir.operands.TemporaryClosureVariable;
import org.jruby.ir.operands.TemporaryCurrentModuleVariable;
import org.jruby.ir.operands.TemporaryVariable;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.UnexecutableNil;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ArgumentDescriptor;
import org.jruby.runtime.ArgumentType;
import org.jruby.runtime.CallType;
import org.jruby.runtime.RubyEvent;
import org.jruby.util.ByteList;
import org.jruby.util.KeyValuePair;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import static org.jruby.ir.IRFlags.*;
import static org.jruby.ir.instructions.RuntimeHelperCall.Methods.*;
import static org.jruby.ir.operands.ScopeModule.SCOPE_MODULE;
import static org.jruby.runtime.CallType.FUNCTIONAL;
import static org.jruby.runtime.CallType.NORMAL;
import static org.jruby.runtime.ThreadContext.CALL_KEYWORD;
import static org.jruby.runtime.ThreadContext.CALL_KEYWORD_REST;

public abstract class IRBuilder<U, V> {
    static final UnexecutableNil U_NIL = UnexecutableNil.U_NIL;

    private final IRManager manager;
    protected final IRScope scope;
    protected final IRBuilder parent;
    protected final List<Instr> instructions;
    protected int coverageMode;
    protected IRBuilder variableBuilder;
    protected List<Object> argumentDescriptions;
    public boolean executesOnce = true;
    int temporaryVariableIndex = -1;
    private boolean needsYieldBlock = false;
    public boolean underscoreVariableSeen = false;
    int lastProcessedLineNum = -1;
    private Variable currentModuleVariable = null;

    // We do not need n consecutive line num instrs but only the last one in the sequence.
    // We set this flag to indicate that we need to emit a line number but have not yet.
    // addInstr will then appropriately add line info when it is called (which will never be
    // called by a linenum instr).
    boolean needsLineNumInfo = false;

    // SSS FIXME: Currently only used for retries -- we should be able to eliminate this
    // Stack of nested rescue blocks -- this just tracks the start label of the blocks
    final Deque<RescueBlockInfo> activeRescueBlockStack = new ArrayDeque<>(4);

    // Stack of ensure blocks that are currently active
    final Deque<EnsureBlockInfo> activeEnsureBlockStack = new ArrayDeque<>(4);

    // Stack of ensure blocks whose bodies are being constructed
    final Deque<EnsureBlockInfo> ensureBodyBuildStack = new ArrayDeque<>(4);

    // Combined stack of active rescue/ensure nestings -- required to properly set up
    // rescuers for ensure block bodies cloned into other regions -- those bodies are
    // rescued by the active rescuers at the point of definition rather than the point
    // of cloning.
    final Deque<Label> activeRescuers = new ArrayDeque<>(4);

    // Since we are processing ASTs, loop bodies are processed in depth-first manner
    // with outer loops encountered before inner loops, and inner loops finished before outer ones.
    //
    // So, we can keep track of loops in a loop stack which  keeps track of loops as they are encountered.
    // This lets us implement next/redo/break/retry easily for the non-closure cases.
    final Deque<IRLoop> loopStack = new LinkedList<>();


    // If set we know which kind of eval is being performed.  Beyond type it also prevents needing to
    // ask what scope type we are in.
    public EvalType evalType = null;

    // This variable is an out-of-band passing mechanism to pass the method name to the block the
    // method is attached to.  call/fcall will set this and iter building will pass it into the iter
    // builder and set it.
    RubySymbol methodName = null;

    // Current index to put next BEGIN blocks and other things at the front of this scope.
    // Note: in the case of multiple BEGINs this index slides forward so they maintain proper
    // execution order
    protected int afterPrologueIndex = 0;
    private TemporaryVariable yieldClosureVariable = null;

    EnumSet<IRFlags> flags;

    public IRBuilder(IRManager manager, IRScope scope, IRBuilder parent, IRBuilder variableBuilder) {
        this.manager = manager;
        this.scope = scope;
        this.parent = parent;
        this.instructions = new ArrayList<>(50);
        this.activeRescuers.push(Label.UNRESCUED_REGION_LABEL);
        this.coverageMode = parent == null ? CoverageData.NONE : parent.coverageMode;

        if (parent != null) executesOnce = parent.executesOnce;

        this.variableBuilder = variableBuilder;
        this.flags = IRScope.allocateInitialFlags(scope);
    }

    public static InterpreterContext buildRoot(IRManager manager, ParseResult rootNode) {
        String file = rootNode.getFile();
        IRScriptBody script = new IRScriptBody(manager, file == null ? "(anon)" : file, rootNode.getStaticScope());

        return topIRBuilder(manager, script, rootNode).buildRootInner(rootNode);
    }

    public static IRBuilderAST topIRBuilder(IRManager manager, IRScope newScope) {
        return new IRBuilderAST(manager, newScope, null);
    }

    public static IRBuilder methodIRBuilder(IRManager manager, IRScope newScope, Object node) {
        if (node instanceof org.jruby.ast.DefNode) {
            return new IRBuilderAST(manager, newScope, null, null);
        } else {
            return new IRBuilderYARP(manager, newScope, null, null);
        }
    }

    public static IRBuilder topIRBuilder(IRManager manager, IRScope newScope, ParseResult rootNode) {
        if (rootNode instanceof RootNode) {
            return new IRBuilderAST(manager, newScope, null, null);
        } else {
            return new IRBuilderYARP(manager, newScope, null, null);
        }
    }

    InterpreterContext buildRootInner(ParseResult parseResult) {
        coverageMode = parseResult.getCoverageMode();
        prepareImplicitState();                                    // recv_self, add frame block, etc)
        addCurrentModule();                                        // %current_module

        afterPrologueIndex = instructions.size() - 1;                      // added BEGINs start after scope prologue stuff

        // Build IR for the tree and return the result of the expression tree
        addInstr(new ReturnInstr(build(parseResult)));

        computeScopeFlagsFrom(instructions);
        // Root scope can receive returns now, so we add non-local return logic if necessary (2.5+)
        if (scope.canReceiveNonlocalReturns()) handleNonlocalReturnInMethod();

        return scope.allocateInterpreterContext(instructions, temporaryVariableIndex + 1, flags);
    }

    public void computeScopeFlagsFrom(List<Instr> instructions) {
        for (Instr i : instructions) {
            i.computeScopeFlags(scope, flags);
        }

        calculateClosureScopeFlags();

        if (computeNeedsDynamicScopeFlag()) flags.add(REQUIRES_DYNSCOPE);

        flags.add(FLAGS_COMPUTED);
    }

    private void calculateClosureScopeFlags() {
        // Compute flags for nested closures (recursively) and set derived flags.
        for (IRClosure cl : scope.getClosures()) {
            if (cl.usesEval()) {
                scope.setCanReceiveBreaks();
                scope.setCanReceiveNonlocalReturns();
                scope.setUsesZSuper();
            } else {
                if (cl.hasBreakInstructions() || cl.canReceiveBreaks()) scope.setCanReceiveBreaks();
                if (cl.hasNonLocalReturns() || cl.canReceiveNonlocalReturns()) scope.setCanReceiveNonlocalReturns();
                if (cl.usesZSuper()) scope.setUsesZSuper();
            }
        }
    }

    private boolean computeNeedsDynamicScopeFlag() {
        return scope.hasNonLocalReturns() ||
                scope.canCaptureCallersBinding() ||
                scope.canReceiveNonlocalReturns() ||
                flags.contains(BINDING_HAS_ESCAPED);
    }

    boolean hasListener() {
        return manager.getIRScopeListener() != null;
    }

    RubySymbol methodNameFor() {
        IRScope method = scope.getNearestMethod();

        return method == null ? null : method.getName();
    }


    IRLoop getCurrentLoop() {
        return loopStack.peek();
    }

    boolean needsCodeCoverage() {
        return coverageMode != CoverageData.NONE || parent != null && parent.needsCodeCoverage();
    }

    public void addInstr(Instr instr) {
        if (needsLineNumInfo) {
            needsLineNumInfo = false;

            if (needsCodeCoverage()) {
                addInstr(new LineNumberInstr(lastProcessedLineNum, coverageMode));
            } else {
                addInstr(manager.newLineNumber(lastProcessedLineNum));
            }

            if (RubyInstanceConfig.FULL_TRACE_ENABLED) {
                addInstr(new TraceInstr(RubyEvent.LINE, getCurrentModuleVariable(), methodNameFor(), getFileName(), lastProcessedLineNum + 1));
            }
        }

        // If we are building an ensure body, stash the instruction
        // in the ensure body's list. If not, add it to the scope directly.
        if (ensureBodyBuildStack.isEmpty()) {
            instr.computeScopeFlags(scope, flags);

            if (hasListener()) manager.getIRScopeListener().addedInstr(scope, instr, instructions.size());

            instructions.add(instr);
        } else {
            ensureBodyBuildStack.peek().addInstr(instr);
        }
    }

    public void addInstrAtBeginning(Instr instr) {
        // If we are building an ensure body, stash the instruction
        // in the ensure body's list. If not, add it to the scope directly.
        if (ensureBodyBuildStack.isEmpty()) {
            instr.computeScopeFlags(scope, flags);

            if (hasListener()) manager.getIRScopeListener().addedInstr(scope, instr, 0);

            instructions.add(0, instr);
        } else {
            ensureBodyBuildStack.peek().addInstrAtBeginning(instr);
        }
    }

    // Add the specified result instruction to the scope and return its result variable.
    Variable addResultInstr(ResultInstr instr) {
        addInstr((Instr) instr);

        return instr.getResult();
    }

    // Emit cloned ensure bodies by walking up the ensure block stack.
    // If we have been passed a loop value, only emit bodies that are nested within that loop.
    void emitEnsureBlocks(IRLoop loop) {
        int n = activeEnsureBlockStack.size();
        EnsureBlockInfo[] ebArray = activeEnsureBlockStack.toArray(new EnsureBlockInfo[n]);
        for (int i = 0; i < n; i++) { // Deque's head is the first element (unlike Stack's)
            EnsureBlockInfo ebi = ebArray[i];

            // For "break" and "next" instructions, we only want to run
            // ensure blocks from the loops they are present in.
            if (loop != null && ebi.innermostLoop != loop) break;

            // Clone into host scope
            ebi.cloneIntoHostScope(this);
        }
    }

    private boolean isDefineMethod() {
        if (methodName != null) {
            String name = methodName.asJavaString();

            return "define_method".equals(name) || "define_singleton_method".equals(name);
        }

        return false;
    }

    // FIXME: Technically a binding in top-level could get passed which would should still cause an error but this
    //   scenario is very uncommon combined with setting @@cvar in a place you shouldn't it is an acceptable incompat
    //   for what I consider to be a very low-value error.
    boolean isTopScope() {
        IRScope topScope = scope.getNearestNonClosurelikeScope();

        boolean isTopScope = topScope instanceof IRScriptBody ||
                (evalType != null && evalType != EvalType.MODULE_EVAL && evalType != EvalType.BINDING_EVAL);

        // we think it could be a top scope but it could still be called from within a module/class which
        // would then not be a top scope.
        if (!isTopScope) return false;

        IRScope s = topScope;
        while (s != null && !(s instanceof IRModuleBody)) {
            s = s.getLexicalParent();
        }

        return s == null; // nothing means we walked all the way up.
    }

    void preloadBlockImplicitClosure() {
        if (needsYieldBlock) {
            addInstrAtBeginning(new LoadBlockImplicitClosureInstr(getYieldClosureVariable()));
        }
    }

    /**
     * Prepare implicit runtime state needed for typical methods to execute. This includes such things
     * as the implicit self variable and any yieldable block available to this scope.
     */
    void prepareImplicitState() {
        // Receive self
        addInstr(getManager().getReceiveSelfInstr());

        // used for yields; metaclass body (sclass) inherits yield var from surrounding, and accesses it as implicit
        if (scope instanceof IRMethod || scope instanceof IRMetaClassBody) {
            addInstr(new LoadImplicitClosureInstr(getYieldClosureVariable()));
        } else {
            addInstr(new LoadFrameClosureInstr(getYieldClosureVariable()));
        }
    }

    void addCurrentModule() {
        addInstr(new CopyInstr(getCurrentModuleVariable(), SCOPE_MODULE[0])); // %current_module
    }

    /**
     * Prepare closure runtime state. This includes the implicit self variable and setting up a variable to hold any
     * frame closure if it is needed later.
     */
    void prepareClosureImplicitState() {
        // Receive self
        addInstr(getManager().getReceiveSelfInstr());
    }

    private TemporaryVariable createTemporaryVariable() {
        // BEGIN uses its parent builder to store any variables
        if (variableBuilder != null) return variableBuilder.createTemporaryVariable();

        temporaryVariableIndex++;

        if (scope.getScopeType() == IRScopeType.CLOSURE) {
            return new TemporaryClosureVariable(((IRClosure) scope).closureId, temporaryVariableIndex);
        } else {
            return manager.newTemporaryLocalVariable(temporaryVariableIndex);
        }
    }

    // FIXME: Add this to clone on branch instrs so if something changes (like an inline) it will replace with opted branch/jump/nop.
    public static Instr createBranch(Operand v1, Operand v2, Label jmpTarget) {
        if (v2 instanceof Boolean) {
            Boolean lhs = (Boolean) v2;

            if (lhs.isTrue()) {
                if (v1.isTruthyImmediate()) return new JumpInstr(jmpTarget);
                if (v1.isFalseyImmediate()) return NopInstr.NOP;

                return new BTrueInstr(jmpTarget, v1);
            } else if (lhs.isFalse()) {
                if (v1.isTruthyImmediate()) return NopInstr.NOP;
                if (v1.isFalseyImmediate()) return new JumpInstr(jmpTarget);

                return new BFalseInstr(jmpTarget, v1);
            }
        } else if (v2 instanceof Nil) {
            if (v1 instanceof Nil) return new JumpInstr(jmpTarget);
            if (v1.isTruthyImmediate()) return NopInstr.NOP;

            return new BNilInstr(jmpTarget, v1);
        }
        if (v2 == UndefinedValue.UNDEFINED) {
            if (v1 == UndefinedValue.UNDEFINED) return new JumpInstr(jmpTarget);

            return new BUndefInstr(jmpTarget, v1);
        }

        throw new RuntimeException("BUG: no BEQ");
    }

    public void determineZSuperCallArgs(IRScope scope, IRBuilder<U, V> builder, List<Operand> callArgs, List<KeyValuePair<Operand, Operand>> keywordArgs) {
        if (builder != null) {  // Still in currently building scopes
            for (Instr instr : builder.instructions) {
                extractCallOperands(callArgs, keywordArgs, instr);
            }
        } else {               // walked out past the eval to already build scopes
            for (Instr instr : scope.getInterpreterContext().getInstructions()) {
                extractCallOperands(callArgs, keywordArgs, instr);
            }
        }
    }

    /*
     * Adjust all argument operands by changing their depths to reflect how far they are from
     * super.  This fixup is only currently happening in supers nested in closures.
     */
    private Operand[] adjustVariableDepth(Operand[] args, int depthFromSuper) {
        if (depthFromSuper == 0) return args;

        Operand[] newArgs = new Operand[args.length];

        for (int i = 0; i < args.length; i++) {
            // Because of keyword args, we can have a keyword-arg hash in the call args.
            if (args[i] instanceof Hash) {
                newArgs[i] = ((Hash) args[i]).cloneForLVarDepth(depthFromSuper);
            } else {
                newArgs[i] = ((DepthCloneable) args[i]).cloneForDepth(depthFromSuper);
            }
        }

        return newArgs;
    }

    public static Operand[] addArg(Operand[] args, Operand extraArg) {
        Operand[] newArgs = new Operand[args.length + 1];
        System.arraycopy(args, 0, newArgs, 0, args.length);
        newArgs[args.length] = extraArg;
        return newArgs;
    }

    Operand putConstant(RubySymbol name, Operand value) {
        return putConstant(findContainerModule(), name, value);
    }

    Operand putConstant(Operand parent, RubySymbol name, Operand value) {
        addInstr(new PutConstInstr(parent, name, value));

        return value;
    }

    // No bounds checks.  Only call this when you know you have an arg to remove.
    public static Operand[] removeArg(Operand[] args) {
        Operand[] newArgs = new Operand[args.length - 1];
        System.arraycopy(args, 0, newArgs, 0, args.length - 1);
        return newArgs;
    }

    Operand searchModuleForConst(Variable result, Operand startingModule, RubySymbol name) {
        if (result == null) result = temp();
        return addResultInstr(new SearchModuleForConstInstr(result, startingModule, name, true));
    }

    Operand searchConst(Variable result, RubySymbol name) {
        if (result == null) result = temp();
        return addResultInstr(new SearchConstInstr(result, CurrentScope.INSTANCE, name, false));
    }

    // SSS FIXME: This feels a little ugly.  Is there a better way of representing this?
    public Operand classVarContainer(boolean declContext) {
        /* -------------------------------------------------------------------------------
         * We are looking for the nearest enclosing scope that is a non-singleton class body
         * without running into an eval-scope in between.
         *
         * Stop lexical scope walking at an eval script boundary.  Evals are essentially
         * a way for a programmer to splice an entire tree of lexical scopes at the point
         * where the eval happens.  So, when we hit an eval-script boundary at compile-time,
         * defer scope traversal to when we know where this scope has been spliced in.
         * ------------------------------------------------------------------------------- */
        int n = 0;
        IRScope cvarScope = scope;
        while (cvarScope != null && !(cvarScope instanceof IREvalScript) && !cvarScope.isNonSingletonClassBody()) {
            // For loops don't get their own static scope
            if (!(cvarScope instanceof IRFor)) {
                n++;
            }
            cvarScope = cvarScope.getLexicalParent();
        }

        if (cvarScope != null && cvarScope.isNonSingletonClassBody()) {
            return ScopeModule.ModuleFor(n);
        } else {
            return addResultInstr(new GetClassVarContainerModuleInstr(temp(),
                    CurrentScope.INSTANCE, declContext ? null : buildSelf()));
        }
    }

    Operand addRaiseError(String id, String message) {
        return addRaiseError(id, new MutableString(message));
    }

    Operand addRaiseError(String id, Operand message) {
        Operand exceptionClass = searchModuleForConst(temp(), getManager().getObjectClass(), symbol(id));
        Operand kernel = searchModuleForConst(temp(), getManager().getObjectClass(), symbol("Kernel"));
        return call(temp(), kernel, "raise", exceptionClass, message);
    }

    static void extractCallOperands(List<Operand> callArgs, List<KeyValuePair<Operand, Operand>> keywordArgs, Instr instr) {
        if (instr instanceof ReceiveKeywordRestArgInstr) {
            // Always add the keyword rest arg to the beginning
            keywordArgs.add(0, new KeyValuePair<>(Symbol.KW_REST_ARG_DUMMY, ((ReceiveArgBase) instr).getResult()));
        } else if (instr instanceof ReceiveKeywordArgInstr) {
            ReceiveKeywordArgInstr receiveKwargInstr = (ReceiveKeywordArgInstr) instr;
            keywordArgs.add(new KeyValuePair<>(new Symbol(receiveKwargInstr.getKey()), receiveKwargInstr.getResult()));
        } else if (instr instanceof ReceiveRestArgInstr) {
            callArgs.add(new Splat(((ReceiveRestArgInstr) instr).getResult()));
        } else if (instr instanceof ReceiveArgBase) {
            callArgs.add(((ReceiveArgBase) instr).getResult());
        }
    }

    // Wrap call in a rescue handler that catches the IRBreakJump
    void receiveBreakException(Operand block, final CallInstr callInstr) {
        receiveBreakException(block, () -> addResultInstr(callInstr));
    }

    void handleNonlocalReturnInMethod() {
        Label rBeginLabel = getNewLabel();
        Label rEndLabel = getNewLabel();
        Label gebLabel = getNewLabel();

        // Protect the entire body as it exists now with the global ensure block
        //
        // Add label and marker instruction in reverse order to the beginning
        // so that the label ends up being the first instr.
        addInstrAtBeginning(new ExceptionRegionStartMarkerInstr(gebLabel));
        addInstrAtBeginning(new LabelInstr(rBeginLabel));
        addInstr( new ExceptionRegionEndMarkerInstr());

        // Receive exceptions (could be anything, but the handler only processes IRReturnJumps)
        addInstr(new LabelInstr(gebLabel));
        Variable exc = temp();
        addInstr(new ReceiveJRubyExceptionInstr(exc));

        // Handle break using runtime helper
        // --> IRRuntimeHelpers.handleNonlocalReturn(scope, bj, blockType)
        Variable ret = temp();
        addInstr(new RuntimeHelperCall(ret, HANDLE_NONLOCAL_RETURN, new Operand[]{exc} ));
        addInstr(new ReturnInstr(ret));

        // End
        addInstr(new LabelInstr(rEndLabel));
    }

    private Operand receiveBreakException(Operand block, CodeBlock codeBlock) {
        // Check if we have to handle a break
        if (block == null ||
                !(block instanceof WrappedIRClosure) ||
                !(((WrappedIRClosure) block).getClosure()).hasBreakInstructions()) {
            // No protection needed -- add the call and return
            return codeBlock.run();
        }

        Label rBeginLabel = getNewLabel();
        Label rEndLabel = getNewLabel();
        Label rescueLabel = getNewLabel();

        // Protected region
        addInstr(new LabelInstr(rBeginLabel));
        addInstr(new ExceptionRegionStartMarkerInstr(rescueLabel));
        Variable callResult = (Variable) codeBlock.run();
        addInstr(new JumpInstr(rEndLabel));
        addInstr(new ExceptionRegionEndMarkerInstr());

        // Receive exceptions (could be anything, but the handler only processes IRBreakJumps)
        addInstr(new LabelInstr(rescueLabel));
        Variable exc = temp();
        addInstr(new ReceiveJRubyExceptionInstr(exc));

        // Handle break using runtime helper
        // --> IRRuntimeHelpers.handlePropagatedBreak(context, scope, bj, blockType)
        addInstr(new RuntimeHelperCall(callResult, HANDLE_PROPAGATED_BREAK, new Operand[]{exc}));

        // End
        addInstr(new LabelInstr(rEndLabel));

        return callResult;
    }

    // for simple calls without splats or keywords
    Variable call(Variable result, Operand object, String name, Operand... args) {
        return call(result, object, symbol(name), args);
    }

    // for simple calls without splats or keywords
    Variable call(Variable result, Operand object, RubySymbol name, Operand... args) {
        return _call(result, NORMAL, object, name, args);
    }

    Variable _call(Variable result, CallType type, Operand object, RubySymbol name, Operand... args) {
        if (result == null) result = temp();
        return addResultInstr(CallInstr.create(scope, type, result, name, object, args, NullBlock.INSTANCE, 0));
    }

    public Operand classVarDefinitionContainer() {
        return classVarContainer(false);
    }

    // if-only
    void cond(Label label, Operand value, Operand test) {
        addInstr(createBranch(value, test, label));
    }

    // if with body
    void cond(Label endLabel, Operand value, Operand test, RunIt body) {
        addInstr(createBranch(value, test, endLabel));
        body.apply();
    }

    // if-only
    void cond_ne(Label label, Operand value, Operand test) {
        addInstr(BNEInstr.create(label, value, test));
    }
    // if !test/else
    void cond_ne(Label endLabel, Operand value, Operand test, RunIt body) {
        addInstr(BNEInstr.create(endLabel, value, test));
        body.apply();
    }

    public Variable copy(Operand value) {
        return copy(null, value);
    }

    public Variable copy(Variable result, Operand value) {
        return addResultInstr(new CopyInstr(result == null ? temp() : result, value));
    }

    Boolean fals() {
        return manager.getFalse();
    }

    // for simple calls without splats or keywords
    Variable fcall(Variable result, Operand object, String name, Operand... args) {
        return fcall(result, object, symbol(name), args);
    }

    // for simple calls without splats or keywords
    Variable fcall(Variable result, Operand object, RubySymbol name, Operand... args) {
        return _call(result, FUNCTIONAL, object, name, args);
    }

    Fixnum fix(long value) {
        return manager.newFixnum(value);
    }

    /**
     * Generate if testVariable NEQ testValue { ifBlock } else { elseBlock }.
     *
     * @param testVariable what we will test against testValue
     * @param testValue    what we want to testVariable to NOT be equal to.
     * @param ifBlock      the code if test values do NOT match
     * @param elseBlock    the code to execute otherwise.
     */
    void if_else(Operand testVariable, Operand testValue, VoidCodeBlock ifBlock, VoidCodeBlock elseBlock) {
        Label elseLabel = getNewLabel();
        Label endLabel = getNewLabel();

        addInstr(BNEInstr.create(elseLabel, testVariable, testValue));
        ifBlock.run();
        addInstr(new JumpInstr(endLabel));

        addInstr(new LabelInstr(elseLabel));
        elseBlock.run();
        addInstr(new LabelInstr(endLabel));
    }

    void if_not(Operand testVariable, Operand testValue, VoidCodeBlock ifBlock) {
        label("if_not_end", (endLabel) -> {
            addInstr(createBranch(testVariable, testValue, endLabel));
            ifBlock.run();
        });
    }

    // Standard for loop in IR.  'test' is responsible for jumping if it fails.
    void for_loop(Consumer<Label> test, Consumer<Label> increment, Consume2<Label, Label> body) {
        Label top = getNewLabel("for_top");
        Label bottom = getNewLabel("for_bottom");
        label("for_end", after -> {
            addInstr(new LabelInstr(top));
            test.accept(after);
            body.apply(after, bottom);
            addInstr(new LabelInstr(bottom));
            increment.accept(after);
            jump(top);
        });
    }

    void jump(Label label) {
        addInstr(new JumpInstr(label));
    }

    void label(String labelName, Consumer<Label> block) {
        Label label = getNewLabel(labelName);
        block.accept(label);
        addInstr(new LabelInstr(label));
    }

    Nil nil() {
        return manager.getNil();
    }

    RubySymbol symbol(String id) {
        return manager.runtime.newSymbol(id);
    }

    RubySymbol symbol(ByteList bytelist) {
        return manager.runtime.newSymbol(bytelist);
    }

    Operand tap(Operand value, Consumer<Operand> block) {
        block.accept(value);

        return value;
    }

    Variable temp() {
        return createTemporaryVariable();
    }

    void type_error(String message) {
        addRaiseError("TypeError", message);
    }

    // Create an unrolled loop of expressions passing in the label which marks the end of these tests.
    void times(int times, Consume2<Label, Integer> body) {
        label("times_end", end -> {
            for (int i = 0; i < times; i++) {
                body.apply(end, new Integer(i));
            }
        });
    }

    Boolean tru() {
        return manager.getTrue();
    }

    public Variable createCurrentModuleVariable() {
        // SSS: Used in only 3 cases in generated IR:
        // -> searching a constant in the inheritance hierarchy
        // -> searching a super-method in the inheritance hierarchy
        // -> looking up 'StandardError' (which can be eliminated by creating a special operand type for this)
        temporaryVariableIndex++;
        return TemporaryCurrentModuleVariable.ModuleVariableFor(temporaryVariableIndex);
    }

    public Variable getCurrentModuleVariable() {
        if (currentModuleVariable == null) currentModuleVariable = createCurrentModuleVariable();

        return currentModuleVariable;
    }

    String getFileName() {
        return scope.getFile();
    }

    public RubySymbol getName() {
        return scope.getName();
    }

    Label getNewLabel() {
        return scope.getNewLabel();
    }

    Label getNewLabel(String labelName) {
        return scope.getNewLabel(labelName);
    }

    protected Variable getValueInTemporaryVariable(Operand val) {
        if (val != null && val instanceof TemporaryVariable) return (Variable) val;

        return copy(val);
    }

    /**
     * Get the variable for accessing the "yieldable" closure in this scope.
     */
    public TemporaryVariable getYieldClosureVariable() {
        // make sure we prepare yield block for this scope, since it is now needed
        needsYieldBlock = true;

        if (yieldClosureVariable == null) {
            return yieldClosureVariable = createTemporaryVariable();
        }

        return yieldClosureVariable;
    }

    static Operand[] getZSuperCallOperands(IRScope scope, List<Operand> callArgs, List<KeyValuePair<Operand, Operand>> keywordArgs, int[] flags) {
        if (scope.getNearestTopLocalVariableScope().receivesKeywordArgs()) {
            flags[0] |= CALL_KEYWORD;
            int i = 0;
            Operand[] args = new Operand[callArgs.size() + 1];
            for (Operand arg : callArgs) {
                args[i++] = arg;
            }
            args[i] = new Hash(keywordArgs, false);
            return args;
        }

        return callArgs.toArray(new Operand[callArgs.size()]);
    }


    /**
     * Combination of whether it is feasible for a method being processed to be lazy (e.g. methods
     * containing break/next cannot for syntax error purposes) or whether it is enabled as an
     * option (feature does not exist yet).
     *
     * @param defNode syntactical representation of the definition
     * @return true if can be lazy
     */
    abstract boolean canBeLazyMethod(V defNode);

    // build methods
    public abstract Operand build(ParseResult result);

    public Operand buildAlias(Operand newName, Operand oldName) {
        addInstr(new AliasInstr(newName, oldName));

        return nil();
    }

    // Note: passing NORMAL just removes ability to remove a branch and will be semantically correct.
    public Operand buildAnd(Operand left, CodeBlock right, BinaryType truth) {
        switch(truth) {
            case LeftTrue:  // left is statically true so we return whatever right expr is.
                return right.run();
            case LeftFalse: // left is already false.  we done.
                return left;
        }

        return tap(getValueInTemporaryVariable(left), (ret) ->
                label("and", (label) ->
                        cond(label, left, fals(), () ->
                                copy((Variable) ret, right.run()))));
    }

    public Operand buildClassVarAsgn(RubySymbol name, U valueNode) {
        if (isTopScope()) return addRaiseError("RuntimeError", "class variable access from toplevel");

        Operand value = build(valueNode);
        addInstr(new PutClassVariableInstr(classVarDefinitionContainer(), name, value));
        return value;
    }

    public Operand buildClassVar(Variable result, RubySymbol name) {
        if (result == null) result = temp();
        if (isTopScope()) return addRaiseError("RuntimeError", "class variable access from toplevel");

        return addResultInstr(new GetClassVariableInstr(result, classVarDefinitionContainer(), name));
    }

    // FIXME: AST needs variable passed in to work which I think means some context really needs to pass in the result at least in AST build?
    public Operand buildConditional(Variable result, U predicate, U statements, U consequent) {
        Label    falseLabel = getNewLabel();
        Label    doneLabel  = getNewLabel();
        Operand thenResult;
        addInstr(createBranch(build(predicate), fals(), falseLabel));

        boolean thenNull = false;
        boolean elseNull = false;
        boolean thenUnil = false;
        boolean elseUnil = false;

        // Build the then part of the if-statement
        if (statements != null) {
            thenResult = build(statements);
            if (thenResult != U_NIL) { // thenResult can be U_NIL if then-body ended with a return!
                // SSS FIXME: Can look at the last instr and short-circuit this jump if it is a break rather
                // than wait for dead code elimination to do it
                result = getValueInTemporaryVariable(thenResult);
                addInstr(new JumpInstr(doneLabel));
            } else {
                if (result == null) result = temp();
                thenUnil = true;
            }
        } else {
            thenNull = true;
            if (result == null) result = temp();
            copy(result, nil());
            addInstr(new JumpInstr(doneLabel));
        }

        // Build the else part of the if-statement
        addInstr(new LabelInstr(falseLabel));
        if (consequent != null) {
            Operand elseResult = build(consequent);
            // elseResult can be U_NIL if then-body ended with a return!
            if (elseResult != U_NIL) {
                copy(result, elseResult);
            } else {
                elseUnil = true;
            }
        } else {
            elseNull = true;
            copy(result, nil());
        }

        if (thenNull && elseNull) {
            addInstr(new LabelInstr(doneLabel));
            return nil();
        } else if (thenUnil && elseUnil) {
            return U_NIL;
        } else {
            addInstr(new LabelInstr(doneLabel));
            return result;
        }
    }

    public Operand buildDefn(IRMethod method) {
        addInstr(new DefineInstanceMethodInstr(method));
        return new Symbol(method.getName());
    }

    public Operand buildDStr(Variable result, U[] nodePieces, Encoding encoding, boolean isFrozen, int line) {
        if (result == null) result = temp();

        Operand[] pieces = new Operand[nodePieces.length];
        int estimatedSize = 0;

        for (int i = 0; i < pieces.length; i++) {
            estimatedSize += dynamicPiece(pieces, i, nodePieces[i]);
        }

        boolean debuggingFrozenStringLiteral = getManager().getInstanceConfig().isDebuggingFrozenStringLiteral();
        addInstr(new BuildCompoundStringInstr(result, pieces, encoding, estimatedSize, isFrozen, debuggingFrozenStringLiteral, getFileName(), line));

        return result;
    }

    public Operand buildGlobalAsgn(RubySymbol name, U valueNode) {
        Operand value = build(valueNode);
        addInstr(new PutGlobalVarInstr(name, value));
        return value;
    }

    public Operand buildGlobalVar(Variable result, RubySymbol name) {
        if (result == null) result = temp();

        return addResultInstr(new GetGlobalVariableInstr(result, name));
    }

    public Operand buildInstAsgn(RubySymbol name, U valueNode) {
        Operand value = build(valueNode);
        addInstr(new PutFieldInstr(buildSelf(), name, value));
        return value;
    }

    public Operand buildInstVar(RubySymbol name) {
        return addResultInstr(new GetFieldInstr(temp(), buildSelf(), name));
    }

    public Operand buildLocalVariableAssign(RubySymbol name, int depth, U valueNode) {
        Variable variable  = getLocalVariable(name, depth);
        Operand value = build(variable, valueNode);

        if (variable != value) copy(variable, value);  // no use copying a variable to itself

        return value;

        // IMPORTANT: The return value of this method is value, not var!
        //
        // Consider this Ruby code: foo((a = 1), (a = 2))
        //
        // If we return 'value' this will get translated to:
        //    a = 1
        //    a = 2
        //    call("foo", [1,2]) <---- CORRECT
        //
        // If we return 'var' this will get translated to:
        //    a = 1
        //    a = 2
        //    call("foo", [a,a]) <---- BUGGY
        //
        // This technique only works if 'value' is an immutable value (ex: fixnum) or a variable
        // So, for Ruby code like this:
        //     def foo(x); x << 5; end;
        //     foo(a=[1,2]);
        //     p a
        // we are guaranteed that the value passed into foo and 'a' point to the same object
        // because of the use of copyAndReturnValue method for literal objects.
    }

    Operand buildConditionalLoop(U conditionNode, U bodyNode, boolean isWhile, boolean isLoopHeadCondition) {
        if (isLoopHeadCondition && (isWhile && alwaysFalse(conditionNode) || !isWhile && alwaysTrue(conditionNode))) {
            build(conditionNode);  // we won't enter the loop -- just build the condition node
            return nil();
        } else {
            IRLoop loop = new IRLoop(scope, getCurrentLoop(), temp());
            Variable loopResult = loop.loopResult;
            Label setupResultLabel = getNewLabel();

            // Push new loop
            loopStack.push(loop);

            // End of iteration jumps here
            addInstr(new LabelInstr(loop.loopStartLabel));
            if (isLoopHeadCondition) {
                Operand cv = build(conditionNode);
                addInstr(createBranch(cv, isWhile ? fals() : tru(), setupResultLabel));
            }

            // Redo jumps here
            addInstr(new LabelInstr(loop.iterStartLabel));

            // Thread poll at start of iteration -- ensures that redos and nexts run one thread-poll per iteration
            addInstr(new ThreadPollInstr(true));

            // Build body
            if (bodyNode != null) build(bodyNode);

            // Next jumps here
            addInstr(new LabelInstr(loop.iterEndLabel));
            if (isLoopHeadCondition) {
                addInstr(new JumpInstr(loop.loopStartLabel));
            } else {
                Operand cv = build(conditionNode);
                addInstr(createBranch(cv, isWhile ? tru() : fals(), loop.iterStartLabel));
            }

            // Loop result -- nil always
            addInstr(new LabelInstr(setupResultLabel));
            addInstr(new CopyInstr(loopResult, nil()));

            // Loop end -- breaks jump here bypassing the result set up above
            addInstr(new LabelInstr(loop.loopEndLabel));

            // Done with loop
            loopStack.pop();

            return loopResult;
        }
    }

    InterpreterContext buildModuleOrClassBody(U body, int startLine, int endLine) {
        addInstr(new TraceInstr(RubyEvent.CLASS, getCurrentModuleVariable(), null, getFileName(), startLine + 1));

        prepareImplicitState();                                    // recv_self, add frame block, etc)
        addCurrentModule();                                        // %current_module

        Operand bodyReturnValue = build(body);

        // This is only added when tracing is enabled because an 'end' will normally have no other instrs which can
        // raise after this point.  When we add trace we need to add one so backtrace generated shows the 'end' line.
        addInstr(getManager().newLineNumber(endLine));
        addInstr(new TraceInstr(RubyEvent.END, getCurrentModuleVariable(), null, getFileName(), endLine + 1));

        addInstr(new ReturnInstr(bodyReturnValue));

        computeScopeFlagsFrom(instructions);
        return scope.allocateInterpreterContext(instructions, temporaryVariableIndex + 1, flags);
    }

    public Operand buildNthRef(int matchNumber) {
        return copy(new NthRef(scope, matchNumber));
    }

    public Operand buildOr(Operand left, CodeBlock right, BinaryType type) {
        // lazy evaluation opt.  Don't bother building rhs of expr is lhs is unconditionally true.
        if (type == BinaryType.LeftTrue) return left;

        // lazy evaluation opt. Eliminate conditional logic if we know lhs is always false.
        if (type == BinaryType.LeftFalse) return right.run();

        Label endOfExprLabel = getNewLabel();
        Variable result = getValueInTemporaryVariable(left);
        addInstr(createBranch(left, tru(), endOfExprLabel));
        addInstr(new CopyInstr(result, right.run()));
        addInstr(new LabelInstr(endOfExprLabel));

        return result;
    }

    public Variable buildSelf() {
        return scope.getSelf();
    }

    Operand buildZSuperIfNest(Variable result, final Operand block) {
        int depthFrom = 0;
        IRBuilder superBuilder = this;
        IRScope superScope = scope;

        boolean defineMethod = false;
        // Figure out depth from argument scope and whether defineMethod may be one of the method calls.
        while (superScope instanceof IRClosure) {
            if (superBuilder != null && superBuilder.isDefineMethod()) defineMethod = true;

            // We may run out of live builds and walk int already built scopes if zsuper in an eval
            superBuilder = superBuilder != null && superBuilder.parent != null ? superBuilder.parent : null;
            superScope = superScope.getLexicalParent();
            depthFrom++;
        }

        final int depthFromSuper = depthFrom;

        // If we hit a method, this is known to always succeed
        Variable zsuperResult = result == null ? temp() : result;
        if (superScope instanceof IRMethod && !defineMethod) {
            List<Operand> callArgs = new ArrayList<>(5);
            List<KeyValuePair<Operand, Operand>> keywordArgs = new ArrayList<>(3);
            int[] flags = new int[]{0};
            determineZSuperCallArgs(superScope, superBuilder, callArgs, keywordArgs);

            if (keywordArgs.size() == 1 && keywordArgs.get(0).getKey().equals(Symbol.KW_REST_ARG_DUMMY)) {
                flags[0] |= (CALL_KEYWORD | CALL_KEYWORD_REST);
                Operand keywordRest = ((DepthCloneable) keywordArgs.get(0).getValue()).cloneForDepth(depthFromSuper);
                Operand[] args = adjustVariableDepth(callArgs.toArray(new Operand[callArgs.size()]), depthFromSuper);
                Variable test = addResultInstr(new RuntimeHelperCall(temp(), IS_HASH_EMPTY, new Operand[]{keywordRest}));
                if_else(test, tru(),
                        () -> addInstr(new ZSuperInstr(scope, zsuperResult, buildSelf(), args, block, flags[0], scope.maybeUsingRefinements())),
                        () -> addInstr(new ZSuperInstr(scope, zsuperResult, buildSelf(), addArg(args, keywordRest), block, flags[0], scope.maybeUsingRefinements())));
            } else {
                Operand[] args = adjustVariableDepth(getZSuperCallOperands(scope, callArgs, keywordArgs, flags), depthFromSuper);
                addInstr(new ZSuperInstr(scope, zsuperResult, buildSelf(), args, block, flags[0], scope.maybeUsingRefinements()));
            }
        } else {
            // We will not have a zsuper show up since we won't emit it but we still need to toggle it.
            // define_method optimization will try and create a method from a closure but it should not in this case.
            scope.setUsesZSuper();

            // Two conditions will inject an error:
            // 1. We cannot find any method scope above the closure (e.g. module A; define_method(:a) { super }; end)
            // 2. One of the method calls the closure is passed to is named define_method.
            //
            // Note: We are introducing an issue but it is so obscure we are ok with it.
            // A method named define_method containing zsuper in a method scope which is not actually
            // a define_method will get raised as invalid even though it should zsuper to the method.
            addRaiseError("RuntimeError",
                    "implicit argument passing of super from method defined by define_method() is not supported. Specify all arguments explicitly.");
        }

        return zsuperResult;
    }

    abstract boolean alwaysFalse(U node);
    abstract boolean alwaysTrue(U node);
    abstract Operand build(Variable result, U node);
    abstract Operand build(U node);
    abstract int dynamicPiece(Operand[] pieces, int index, U piece);
    abstract void receiveMethodArgs(V defNode);

    IRMethod defineNewMethod(LazyMethodDefinition<U, V> defn, ByteList name, int line, StaticScope scope, boolean isInstanceMethod) {
        IRMethod method = new IRMethod(getManager(), this.scope, defn, name, isInstanceMethod, line, scope, coverageMode);

        // poorly placed next/break expects a syntax error so we eagerly build methods which contain them.
        if (!canBeLazyMethod(defn.getMethod())) method.lazilyAcquireInterpreterContext();

        return method;
    }


    public InterpreterContext defineMethodInner(LazyMethodDefinition<U, V> defNode, IRScope parent, int coverageMode) {
        this.coverageMode = coverageMode;

        if (RubyInstanceConfig.FULL_TRACE_ENABLED) {
            // Explicit line number here because we need a line number for trace before we process any nodes
            addInstr(getManager().newLineNumber(scope.getLine() + 1));
            addInstr(new TraceInstr(RubyEvent.CALL, getCurrentModuleVariable(), getName(), getFileName(), scope.getLine() + 1));
        }

        prepareImplicitState();                                    // recv_self, add frame block, etc)

        // These instructions need to be toward the top of the method because they may both be needed for processing
        // optional arguments as in def foo(a = Object).
        // Set %current_module = isInstanceMethod ? %self.metaclass : %self
        int nearestScopeDepth = parent.getNearestModuleReferencingScopeDepth();
        addInstr(new CopyInstr(getCurrentModuleVariable(), ScopeModule.ModuleFor(nearestScopeDepth == -1 ? 1 : nearestScopeDepth)));

        // Build IR for arguments (including the block arg)
        receiveMethodArgs(defNode.getMethod());

        // Build IR for body
        Operand rv = build(defNode.getMethodBody());

        // FIXME: Need commonality for line numbers between YARP and AST
        if (RubyInstanceConfig.FULL_TRACE_ENABLED) {
            int endLine = defNode.getEndLine();
            addInstr(new LineNumberInstr(endLine));
            addInstr(new TraceInstr(RubyEvent.RETURN, getCurrentModuleVariable(), getName(), getFileName(), endLine));
        }

        if (rv != null) addInstr(new ReturnInstr(rv));

        // We do an extra early one so we can look for non-local returns.
        computeScopeFlagsFrom(instructions);

        // If the method can receive non-local returns
        if (scope.canReceiveNonlocalReturns()) handleNonlocalReturnInMethod();

        ((IRMethod) scope).setArgumentDescriptors(createArgumentDescriptor());

        computeScopeFlagsFrom(instructions);

        return scope.allocateInterpreterContext(instructions, temporaryVariableIndex + 1, flags);
    }

    ArgumentDescriptor[] createArgumentDescriptor() {
        ArgumentDescriptor[] argDesc;
        if (argumentDescriptions == null) {
            argDesc = ArgumentDescriptor.EMPTY_ARRAY;
        } else {
            argDesc = new ArgumentDescriptor[argumentDescriptions.size() / 2];
            for (int i = 0; i < argumentDescriptions.size(); i += 2) {
                ArgumentType type = (ArgumentType) argumentDescriptions.get(i);
                RubySymbol symbol = (RubySymbol) argumentDescriptions.get(i+1);
                argDesc[i / 2] = new ArgumentDescriptor(type, symbol);
            }
        }
        return argDesc;
    }

    public void addArgumentDescription(ArgumentType type, RubySymbol name) {
        if (argumentDescriptions == null) argumentDescriptions = new ArrayList<>();

        argumentDescriptions.add(type);
        argumentDescriptions.add(name);
    }

    /* '_' can be seen as a variable only by its first assignment as a local variable.  For any additional
     * '_' we create temporary variables in the case the scope has a zsuper in it.  If so, then the zsuper
     * call will slurp those temps up as it's parameters so it can properly set up the call.
     */
    Variable argumentResult(RubySymbol name) {
        boolean isUnderscore = name.getBytes().realSize() == 1 && name.getBytes().charAt(0) == '_';

        if (isUnderscore && underscoreVariableSeen) {
            return temp();
        } else {
            if (isUnderscore) underscoreVariableSeen = true;
            return getNewLocalVariable(name, 0);
        }
    }

    Operand findContainerModule() {
        int nearestModuleBodyDepth = scope.getNearestModuleReferencingScopeDepth();
        return (nearestModuleBodyDepth == -1) ? getCurrentModuleVariable() : ScopeModule.ModuleFor(nearestModuleBodyDepth);
    }

    public LocalVariable getLocalVariable(RubySymbol name, int scopeDepth) {
        return scope.getLocalVariable(name, scopeDepth);
    }

    public LocalVariable getNewLocalVariable(RubySymbol name, int scopeDepth) {
        return scope.getNewLocalVariable(name, scopeDepth);
    }

    public IRManager getManager() {
        return manager;
    }

    BinaryType binaryType(U node) {
        return alwaysTrue(node) ? BinaryType.LeftTrue :
                alwaysFalse(node) ? BinaryType.LeftFalse : BinaryType.Normal;
    }

    interface CodeBlock {
        Operand run();
    }

    interface Consume2<T, U> {
        void apply(T t, U u);
    }

    interface RunIt {
        void apply();
    }

    interface VoidCodeBlock {
        void run();
    }

    public enum BinaryType {
        Normal,    // Left is unknown expression
        LeftTrue,  // Statically true
        LeftFalse  // Statically false
    }
}

