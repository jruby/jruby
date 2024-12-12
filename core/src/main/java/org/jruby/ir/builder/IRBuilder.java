package org.jruby.ir.builder;

import org.jcodings.Encoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.EvalType;
import org.jruby.ParseResult;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubySymbol;
import org.jruby.ast.IterNode;
import org.jruby.ast.StrNode;
import org.jruby.common.IRubyWarnings;
import org.jruby.ext.coverage.CoverageData;
import org.jruby.ir.IRClassBody;
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
import org.jruby.ir.instructions.*;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.operands.*;
import org.jruby.ir.operands.Boolean;
import org.jruby.ir.operands.Integer;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ArgumentDescriptor;
import org.jruby.runtime.ArgumentType;
import org.jruby.runtime.CallType;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.Signature;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.CommonByteLists;
import org.jruby.util.DefinedMessage;
import org.jruby.util.KeyValuePair;
import org.jruby.util.RegexpOptions;
import org.jruby.util.cli.Options;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.jruby.ir.IRFlags.*;
import static org.jruby.ir.builder.StringStyle.Frozen;
import static org.jruby.ir.builder.StringStyle.Mutable;
import static org.jruby.ir.instructions.Instr.EMPTY_OPERANDS;
import static org.jruby.ir.instructions.IntegerMathInstr.Op.ADD;
import static org.jruby.ir.instructions.IntegerMathInstr.Op.SUBTRACT;
import static org.jruby.ir.instructions.RuntimeHelperCall.Methods.*;
import static org.jruby.ir.operands.ScopeModule.SCOPE_MODULE;
import static org.jruby.runtime.CallType.FUNCTIONAL;
import static org.jruby.runtime.CallType.NORMAL;
import static org.jruby.runtime.ThreadContext.CALL_KEYWORD;
import static org.jruby.runtime.ThreadContext.CALL_KEYWORD_REST;
import static org.jruby.util.RubyStringBuilder.str;

public abstract class IRBuilder<U, V, W, X, Y, Z> {
    static final UnexecutableNil U_NIL = UnexecutableNil.U_NIL;

    private boolean parserTiming = Options.PARSER_SUMMARY.load();
    private final IRManager manager;
    protected final IRScope scope;
    protected final IRBuilder<U, V, W, X, Y, Z> parent;
    protected final List<Instr> instructions;
    protected int coverageMode;
    protected IRBuilder<U, V, W, X, Y, Z> variableBuilder;
    protected List<Object> argumentDescriptions;
    public boolean executesOnce = true;
    int temporaryVariableIndex = -1;
    private boolean needsYieldBlock = false;
    public boolean underscoreVariableSeen = false;
    int lastProcessedLineNum = -1;
    private Variable currentModuleVariable = null;

    // FIXME: AST does not use this but Prism does.  AST could put encoding up to RootNode since it is same
    protected Encoding encoding;
    protected int flipVariableCount = 0;

    // We do not need n consecutive line num instrs but only the last one in the sequence.
    // We set this flag to indicate that we need to emit a line number but have not yet.
    // addInstr will then appropriately add line info when it is called (which will never be
    // called by a linenum instr).
    enum LineInfo {
        Coverage,
        Backtrace
    }
    LineInfo needsLineNumInfo = null;

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

    private boolean selfUsed = false;
    private boolean currentModuleUsed = false;

    public IRBuilder(IRManager manager, IRScope scope, IRBuilder<U, V, W, X, Y, Z> parent, IRBuilder<U, V, W, X, Y, Z> variableBuilder, Encoding encoding) {
        this.manager = manager;
        this.scope = scope;
        this.parent = parent;
        this.instructions = new ArrayList<>(50);
        this.activeRescuers.push(Label.UNRESCUED_REGION_LABEL);
        this.coverageMode = parent == null ? CoverageData.NONE : parent.coverageMode;

        if (parent != null) executesOnce = parent.executesOnce;

        this.variableBuilder = variableBuilder;
        this.flags = IRScope.allocateInitialFlags(scope);
        this.encoding = encoding;
    }

    public static InterpreterContext buildRoot(IRManager manager, ParseResult rootNode) {
        String file = rootNode.getFile();
        IRScriptBody script = new IRScriptBody(manager, file == null ? "(anon)" : file, rootNode.getStaticScope());

        //System.out.println("Building " + file);
        return manager.getBuilderFactory().topIRBuilder(manager, script, rootNode).buildRootInner(rootNode);
    }

    // FIXME: consider mod_rescue, rescue, and pure ensure as separate entries
    // Note: reference is only passed in via Prism on legacy this is desugared into AST.
    protected Operand buildEnsureInternal(U body, U elseNode, U[] exceptions, U rescueBody, X optRescue, boolean isModifier,
                                U ensureNode, boolean isRescue, U reference) {
        // Save $!
        final Variable savedGlobalException = temp();
        addInstr(new GetGlobalVariableInstr(savedGlobalException, symbol("$!")));

        // ------------ Build the body of the ensure block ------------
        //
        // The ensure code is built first so that when the protected body is being built,
        // the ensure code can be cloned at break/next/return sites in the protected body.

        // Push a new ensure block node onto the stack of ensure bodies being built
        // The body's instructions are stashed and emitted later.
        EnsureBlockInfo ebi = new EnsureBlockInfo(scope, getCurrentLoop(), activeRescuers.peek());

        // Record $! save var if we had a non-empty rescue node.
        // $! will be restored from it where required.
        if (isRescue) ebi.savedGlobalException = savedGlobalException;

        ensureBodyBuildStack.push(ebi);
        Operand ensureRetVal = ensureNode == null ? nil() : build(ensureNode);
        ensureBodyBuildStack.pop();

        // ------------ Build the protected region ------------
        activeEnsureBlockStack.push(ebi);

        // Start of protected region
        addInstr(new LabelInstr(ebi.regionStart));
        addInstr(new ExceptionRegionStartMarkerInstr(ebi.dummyRescueBlockLabel));
        activeRescuers.push(ebi.dummyRescueBlockLabel);

        // Generate IR for code being protected
        Variable ensureExprValue = temp();
        Operand rv;
        if (isRescue) {
            rv = buildRescueInternal(body, elseNode, exceptions, rescueBody, optRescue, isModifier, ebi, reference);
        } else {
            rv = build(body);
        }

        // End of protected region
        addInstr(new ExceptionRegionEndMarkerInstr());
        activeRescuers.pop();

        // Is this a begin..(rescue..)?ensure..end node that actually computes a value?
        // (vs. returning from protected body)
        boolean isEnsureExpr = ensureNode != null && rv != U_NIL && !isRescue;

        // Clone the ensure body and jump to the end
        if (isEnsureExpr) {
            addInstr(new CopyInstr(ensureExprValue, rv));
            ebi.cloneIntoHostScope(this);
            addInstr(new JumpInstr(ebi.end));
        }

        // Pop the current ensure block info node
        activeEnsureBlockStack.pop();

        // ------------ Emit the ensure body alongwith dummy rescue block ------------
        // Now build the dummy rescue block that
        // catches all exceptions thrown by the body
        addInstr(new LabelInstr(ebi.dummyRescueBlockLabel));
        Variable exc = addResultInstr(new ReceiveJRubyExceptionInstr(temp()));

        // Now emit the ensure body's stashed instructions
        if (ensureNode != null) ebi.emitBody(this);

        // 1. Ensure block has no explicit return => the result of the entire ensure expression is the result of the protected body.
        // 2. Ensure block has an explicit return => the result of the protected body is ignored.
        // U_NIL => there was a return from within the ensure block!
        if (ensureRetVal == U_NIL) rv = U_NIL;

        // Return (rethrow exception/end)
        // rethrows the caught exception from the dummy ensure block
        addInstr(new ThrowExceptionInstr(exc));

        // End label for the exception region
        addInstr(new LabelInstr(ebi.end));

        return isEnsureExpr ? ensureExprValue : rv;
    }

    public InterpreterContext buildEvalRoot(ParseResult rootNode) {
        executesOnce = false;
        coverageMode = rootNode.getCoverageMode();
        addInstr(getManager().newLineNumber(scope.getLine()));

        afterPrologueIndex = instructions.size() - 1;                      // added BEGINs start after scope prologue stuff

        Operand returnValue = build(rootNode);
        addInstr(new ReturnInstr(returnValue));

        prependUsedImplicitState(null);

        computeScopeFlagsFrom(instructions);
        return scope.allocateInterpreterContext(instructions, temporaryVariableIndex + 2, flags);
    }

    protected InterpreterContext buildRootInner(ParseResult parseResult) {
        long time = 0;
        if (parserTiming) time = System.nanoTime();
        coverageMode = parseResult.getCoverageMode();

        // Build IR for the tree and return the result of the expression tree
        addInstr(new ReturnInstr(build(parseResult)));

        computeScopeFlagsFrom(instructions);
        // Root scope can receive returns now, so we add non-local return logic if necessary (2.5+)
        if (scope.canReceiveNonlocalReturns()) handleNonlocalReturnInMethod();

        prependUsedImplicitState(null);

        InterpreterContext ic = scope.allocateInterpreterContext(instructions, temporaryVariableIndex + 1, flags);

        if (parserTiming) manager.getRuntime().getParserManager().getParserStats().addIRBuildTime(System.nanoTime() - time);
        return ic;
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

    protected boolean hasListener() {
        return manager.getIRScopeListener() != null;
    }

    protected void maybeGenerateIsNotEmptyErrorString(Variable errorString, Operand result, Operand value) {
        label("empty", (empty) ->
                cond(empty, result, tru(), ()->
                        addInstr(new BuildCompoundStringInstr(errorString, new Operand[] {value, new FrozenString(" is not empty")},
                                UTF8Encoding.INSTANCE, 13, Frozen, getFileName(), lastProcessedLineNum))));
    }
    protected RubySymbol methodNameFor() {
        IRScope method = scope.getNearestMethod();

        return method == null ? null : method.getName();
    }


    protected IRLoop getCurrentLoop() {
        return loopStack.peek();
    }

    public void addInstr(Instr instr) {
        if (needsLineNumInfo != null) {
            LineInfo type = needsLineNumInfo;
            needsLineNumInfo = null;

            if (type == LineInfo.Coverage) {
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
    protected Variable addResultInstr(ResultInstr instr) {
        addInstr((Instr) instr);

        return instr.getResult();
    }

    // Emit cloned ensure bodies by walking up the ensure block stack.
    // If we have been passed a loop value, only emit bodies that are nested within that loop.
    protected void emitEnsureBlocks(IRLoop loop) {
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

    protected boolean isTopLevel() {
        return scope.isTopLocalVariableScope() && scope instanceof IRScriptBody;
    }

    // FIXME: Technically a binding in top-level could get passed which would should still cause an error but this
    //   scenario is very uncommon combined with setting @@cvar in a place you shouldn't it is an acceptable incompat
    //   for what I consider to be a very low-value error.
    protected boolean isTopScope() {
        IRScope topScope = scope.getNearestNonClosurelikeScope();

        boolean isTopScope = topScope instanceof IRScriptBody && evalType == null ||
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

    protected void outputExceptionCheck(Operand excType, Operand excObj, Label caughtLabel) {
        Variable eqqResult = addResultInstr(new RescueEQQInstr(temp(), excType, excObj));
        addInstr(createBranch(eqqResult, tru(), caughtLabel));
    }

    protected Operand protectCodeWithRescue(CodeBlock protectedCode, CodeBlock rescueBlock) {
        // This effectively mimics a begin-rescue-end code block
        // Except this catches all exceptions raised by the protected code

        Variable rv = temp();
        Label rBeginLabel = getNewLabel();
        Label rEndLabel   = getNewLabel();
        Label rescueLabel = getNewLabel();

        // Protected region code
        addInstr(new LabelInstr(rBeginLabel));
        addInstr(new ExceptionRegionStartMarkerInstr(rescueLabel));
        Operand v1 = protectedCode.run(); // YIELD: Run the protected code block
        addInstr(new CopyInstr(rv, v1));
        addInstr(new JumpInstr(rEndLabel));
        addInstr(new ExceptionRegionEndMarkerInstr());

        // SSS FIXME: Create an 'Exception' operand type to eliminate the constant lookup below
        // We could preload a set of constant objects that are preloaded at boot time and use them
        // directly in IR when we know there is no lookup involved.
        //
        // new Operand type: CachedClass(String name)?
        //
        // Some candidates: Exception, StandardError, Fixnum, Object, Boolean, etc.
        // So, when they are referenced, they are fetched directly from the runtime object
        // which probably already has cached references to these constants.
        //
        // But, unsure if this caching is safe ... so, just an idea here for now.

        // Rescue code
        Label caughtLabel = getNewLabel();
        Variable exc = temp();
        Variable excType = temp();

        // Receive 'exc' and verify that 'exc' is of ruby-type 'Exception'
        addInstr(new LabelInstr(rescueLabel));
        addInstr(new ReceiveRubyExceptionInstr(exc));
        addInstr(new InheritanceSearchConstInstr(excType, getManager().getObjectClass(),
                getManager().runtime.newSymbol(CommonByteLists.EXCEPTION)));
        outputExceptionCheck(excType, exc, caughtLabel);

        // Fall-through when the exc !== Exception; rethrow 'exc'
        addInstr(new ThrowExceptionInstr(exc));

        // exc === Exception; Run the rescue block
        addInstr(new LabelInstr(caughtLabel));
        Object v2 = rescueBlock.run(); // YIELD: Run the protected code block
        if (v2 != null) addInstr(new CopyInstr(rv, nil()));

        // End
        addInstr(new LabelInstr(rEndLabel));

        return rv;
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

    private TemporaryVariable createIntVariable() {
        // BEGIN uses its parent builder to store any variables
        if (variableBuilder != null) return variableBuilder.createIntVariable();
        temporaryVariableIndex++;

        return new TemporaryIntVariable(temporaryVariableIndex);
    }

    // FIXME: Add this to clone on branch instrs so if something changes (like an inline) it will replace with opted branch/jump/nop.
    public static Instr createBranch(Operand v1, Operand v2, Label jmpTarget) {
        if (v2 instanceof Boolean lhs) {
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

    public void determineZSuperCallArgs(IRScope scope, IRBuilder<U, V, W, X, Y, Z> builder, List<Operand> callArgs, List<KeyValuePair<Operand, Operand>> keywordArgs) {
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

    protected Operand putConstant(RubySymbol name, Operand value) {
        return putConstant(findContainerModule(), name, value);
    }

    protected Operand putConstant(Operand parent, RubySymbol name, Operand value) {
        addInstr(new PutConstInstr(parent, name, value));

        return value;
    }

    // No bounds checks.  Only call this when you know you have an arg to remove.
    public static Operand[] removeArg(Operand[] args) {
        Operand[] newArgs = new Operand[args.length - 1];
        System.arraycopy(args, 0, newArgs, 0, args.length - 1);
        return newArgs;
    }

    protected Operand searchModuleForConst(Variable result, Operand startingModule, RubySymbol name) {
        if (result == null) result = temp();
        return addResultInstr(new SearchModuleForConstInstr(result, startingModule, name, true));
    }

    protected Operand searchModuleForConstNoFrills(Variable result, Operand startingModule, RubySymbol name) {
        if (result == null) result = temp();
        return addResultInstr(new SearchModuleForConstInstr(result, startingModule, name, false, false));
    }

    protected Operand searchConst(Variable result, RubySymbol name) {
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

    protected Operand addRaiseError(String id, String message) {
        return addRaiseError(id, new MutableString(message));
    }

    Operand addRaiseError(String id, Operand message) {
        Operand exceptionClass = searchModuleForConst(temp(), getManager().getObjectClass(), symbol(id));
        Operand kernel = searchModuleForConst(temp(), getManager().getObjectClass(), symbol("Kernel"));
        return call(temp(), kernel, "raise", exceptionClass, message);
    }

    protected static void extractCallOperands(List<Operand> callArgs, List<KeyValuePair<Operand, Operand>> keywordArgs, Instr instr) {
        if (instr instanceof ReceiveKeywordRestArgInstr) {
            // Always add the keyword rest arg to the beginning
            keywordArgs.add(0, new KeyValuePair<>(Symbol.KW_REST_ARG_DUMMY, ((ReceiveArgBase) instr).getResult()));
        } else if (instr instanceof ReceiveKeywordArgInstr receiveKwargInstr) {
            keywordArgs.add(new KeyValuePair<>(new Symbol(receiveKwargInstr.getKey()), receiveKwargInstr.getResult()));
        } else if (instr instanceof ReceiveRestArgInstr) {
            callArgs.add(new Splat(((ReceiveRestArgInstr) instr).getResult()));
        } else if (instr instanceof ReceiveArgBase) {
            callArgs.add(((ReceiveArgBase) instr).getResult());
        }
    }

    // Wrap call in a rescue handler that catches the IRBreakJump
    protected void receiveBreakException(Operand block, final CallInstr callInstr) {
        receiveBreakException(block, () -> addResultInstr(callInstr));
    }

    protected void handleBreakAndReturnsInLambdas() {
        Label rEndLabel   = getNewLabel();
        Label rescueLabel = Label.getGlobalEnsureBlockLabel();

        // Protect the entire body as it exists now with the global ensure block
        addInstrAtBeginning(new ExceptionRegionStartMarkerInstr(rescueLabel));
        addInstr(new ExceptionRegionEndMarkerInstr());

        // Receive exceptions (could be anything, but the handler only processes IRBreakJumps)
        addInstr(new LabelInstr(rescueLabel));
        Variable exc = temp();
        addInstr(new ReceiveJRubyExceptionInstr(exc));

        // Handle break using runtime helper
        // --> IRRuntimeHelpers.handleBreakAndReturnsInLambdas(context, scope, bj, blockType)
        Variable ret = temp();
        addInstr(new RuntimeHelperCall(ret, RuntimeHelperCall.Methods.HANDLE_BREAK_AND_RETURNS_IN_LAMBDA, new Operand[]{exc} ));
        addInstr(new ReturnOrRethrowSavedExcInstr(ret));

        // End
        addInstr(new LabelInstr(rEndLabel));
    }

    protected void handleNonlocalReturnInMethod() {
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

    private void receiveBreakException(Operand block, CodeBlock codeBlock) {
        // Check if we have to handle a break
        if (!(block instanceof WrappedIRClosure) ||
                !(((WrappedIRClosure) block).getClosure()).hasBreakInstructions()) {
            // No protection needed -- add the call and return
            codeBlock.run();
            return;
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
    }

    // for simple calls without splats or keywords
    protected Variable call(Variable result, Operand object, String name, Operand... args) {
        return call(result, object, symbol(name), args);
    }

    // for simple calls without splats or keywords
    protected Variable call(Variable result, Operand object, RubySymbol name, Operand... args) {
        return _call(result, NORMAL, object, name, args);
    }

    protected Variable _call(Variable result, CallType type, Operand object, RubySymbol name, Operand... args) {
        if (result == null) result = temp();
        return addResultInstr(CallInstr.create(scope, type, result, name, object, args, NullBlock.INSTANCE, 0));
    }

    public Operand classVarDefinitionContainer() {
        return classVarContainer(false);
    }

    // if-only
    protected void cond(Label label, Operand value, Operand test) {
        addInstr(createBranch(value, test, label));
    }

    // if with body
    protected void cond(Label endLabel, Operand value, Operand test, RunIt body) {
        addInstr(createBranch(value, test, endLabel));
        body.apply();
    }

    // if-only true
    protected void cond_ne_true(Label label, Operand value) {
        addInstr(new BFalseInstr(label, value));
    }

    // if-only false
    protected void cond_ne_false(Label label, Operand value) {
        addInstr(new BTrueInstr(label, value));
    }

    // if !test/else nil
    protected void cond_ne_nil(Label endLabel, Operand value, RunIt body) {
        addInstr(BNEInstr.create(endLabel, value, nil()));
        body.apply();
    }

    public Variable copy(Operand value) {
        return copy(null, value);
    }

    public Variable copy(Variable result, Operand value) {
        if (result == null) {
            result = value instanceof Integer || value instanceof TemporaryIntVariable ?
                    createIntVariable() :
                    createTemporaryVariable();
        }
        return addResultInstr(new CopyInstr(result, value));
    }

    protected Boolean fals() {
        return manager.getFalse();
    }

    // for simple calls without splats or keywords
    protected Variable fcall(Variable result, Operand object, String name, Operand... args) {
        return fcall(result, object, symbol(name), args);
    }

    // for simple calls without splats or keywords
    protected Variable fcall(Variable result, Operand object, RubySymbol name, Operand... args) {
        return _call(result, FUNCTIONAL, object, name, args);
    }

    protected Fixnum fix(long value) {
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
    protected void if_else(Operand testVariable, Operand testValue, VoidCodeBlock ifBlock, VoidCodeBlock elseBlock) {
        Label elseLabel = getNewLabel();
        Label endLabel = getNewLabel();

        addInstr(BNEInstr.create(elseLabel, testVariable, testValue));
        ifBlock.run();
        addInstr(new JumpInstr(endLabel));

        addInstr(new LabelInstr(elseLabel));
        elseBlock.run();
        addInstr(new LabelInstr(endLabel));
    }

    protected void if_not(Operand testVariable, Operand testValue, VoidCodeBlock ifBlock) {
        label("if_not_end", (endLabel) -> {
            addInstr(createBranch(testVariable, testValue, endLabel));
            ifBlock.run();
        });
    }

    // Standard for loop in IR.  'test' is responsible for jumping if it fails.
    protected void for_loop(Consumer<Label> test, Consumer<Label> increment, Consume2<Label, Label> body) {
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

    protected void jump(Label label) {
        addInstr(new JumpInstr(label));
    }

    protected void label(String labelName, Consumer<Label> block) {
        Label label = getNewLabel(labelName);
        block.accept(label);
        addInstr(new LabelInstr(label));
    }

    protected Nil nil() {
        return manager.getNil();
    }

    protected RubySymbol symbol(String id) {
        return manager.runtime.newSymbol(id);
    }

    protected RubySymbol symbol(ByteList bytelist) {
        return manager.runtime.newSymbol(bytelist);
    }

    protected Operand tap(Operand value, Consumer<Operand> block) {
        block.accept(value);

        return value;
    }


    private Variable intTemp() {
        return createIntVariable();
    }

    protected Variable temp() {
        return createTemporaryVariable();
    }

    protected void type_error(String message) {
        addRaiseError("TypeError", message);
    }

    // Create an unrolled loop of expressions passing in the label which marks the end of these tests.
    protected void times(int times, Consume2<Label, Integer> body) {
        label("times_end", end -> {
            for (int i = 0; i < times; i++) {
                body.apply(end, new Integer(i));
            }
        });
    }

    protected Boolean tru() {
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
        // FIXME: this can just look at the variable field and not have extra boolean
        currentModuleUsed = true;
        if (currentModuleVariable == null) currentModuleVariable = createCurrentModuleVariable();

        return currentModuleVariable;
    }

    protected String getFileName() {
        return scope.getFile();
    }

    public RubySymbol getName() {
        return scope.getName();
    }

    protected Label getNewLabel() {
        return scope.getNewLabel();
    }

    protected Label getNewLabel(String labelName) {
        return scope.getNewLabel(labelName);
    }

    protected Variable getValueInTemporaryVariable(Operand val) {
        return val instanceof TemporaryVariable ? (Variable) val : copy(val);
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

    protected static Operand[] getZSuperCallOperands(IRScope scope, List<Operand> callArgs, List<KeyValuePair<Operand, Operand>> keywordArgs, int[] flags) {
        if (scope.getNearestTopLocalVariableScope().receivesKeywordArgs()) {
            flags[0] |= CALL_KEYWORD;
            int i = 0;
            Operand[] args = new Operand[callArgs.size() + 1];
            for (Operand arg : callArgs) {
                args[i++] = arg;
            }
            args[i] = new Hash(keywordArgs);
            return args;
        }

        return callArgs.toArray(new Operand[0]);
    }


    protected boolean canBacktraceBeRemoved(U[] exceptions, U rescueBody, X optRescue, U elseNode, boolean isModifier) {
        if (RubyInstanceConfig.FULL_TRACE_ENABLED) return false; // Tracing needs to trace
        if (!isModifier && elseNode != null) return false; // only very simple rescues
        if (optRescue != null) return false;                     // We will not handle multiple rescues
        if (exceptions != null) return false;            // We cannot know if these are builtin or not statically.
        if (isErrorInfoGlobal(rescueBody)) return false; // Captured backtrace info for the exception cannot optimize.
        return isSideEffectFree(rescueBody);
    }


    protected abstract U[] exceptionNodesFor(X node);
    protected abstract U bodyFor(X node);
    protected abstract X optRescueFor(X node);
    protected abstract U referenceFor(X node);
    protected abstract boolean isSideEffectFree(final U node);
    protected abstract boolean isErrorInfoGlobal(final U body);
    /**
     * Combination of whether it is feasible for a method being processed to be lazy (e.g. methods
     * containing break/next cannot for syntax error purposes) or whether it is enabled as an
     * option (feature does not exist yet).
     *
     * @param defNode syntactical representation of the definition
     * @return true if can be lazy
     */
    protected abstract boolean canBeLazyMethod(V defNode);

    // build methods
    public abstract Operand build(ParseResult result);

    protected Operand buildWithOrder(U node, boolean preserveOrder) {
        Operand value = build(node);

        // We need to preserve order in cases (like in presence of assignments) except that immutable
        // literals can never change value so we can still emit these out of order.
        return preserveOrder && !(value instanceof ImmutableLiteral) ? copy(value) : value;
    }

    protected Operand buildAlias(Operand newName, Operand oldName) {
        addInstr(new AliasInstr(newName, oldName));

        return nil();
    }

    // Note: passing NORMAL just removes ability to remove a branch and will be semantically correct.
    protected Operand buildAnd(Operand left, CodeBlock right, BinaryType truth) {
        return switch (truth) {
            // left is statically true so we return whatever right expr is.
            case LeftTrue -> right.run();
            // left is already false.  we done.
            case LeftFalse -> left;
            default -> tap(getValueInTemporaryVariable(left), (ret) ->
                    label("and", (label) ->
                            cond(label, left, fals(), () ->
                                    copy((Variable) ret, right.run()))));
        };

    }

    protected Operand buildBreak(CodeBlock value, int line) {
        IRLoop currLoop = getCurrentLoop();

        if (currLoop != null) {
            // If we have ensure blocks, have to run those first!
            if (!activeEnsureBlockStack.isEmpty()) emitEnsureBlocks(currLoop);

            currLoop.hasBreak = true;
            addInstr(new CopyInstr(currLoop.loopResult, value.run()));
            addInstr(new JumpInstr(currLoop.loopEndLabel));
        } else {
            if (scope instanceof IRClosure) {
                // This lexical scope value is only used (and valid) in regular block contexts.
                // If this instruction is executed in a Proc or Lambda context, the lexical scope value is useless.
                IRScope returnScope = scope.getLexicalParent();
                if (scope instanceof IREvalScript || returnScope == null) {
                    // We are not in a closure or a loop => bad break instr!
                    throwSyntaxError(line, "Can't escape from eval with redo");
                } else {
                    addInstr(new BreakInstr(value.run(), returnScope.getId()));
                }
            } else {
                // We are not in a closure or a loop => bad break instr!
                throwSyntaxError(line, "Invalid break");
            }
        }

        // Once the break instruction executes, control exits this scope
        return U_NIL;
    }

    protected Operand[] setupCallArgs(U args, int[] flags) {
        return args == null ? Operand.EMPTY_ARRAY : buildCallArgs(args, flags);
    }

    protected Operand buildCase(U predicate, U[] arms, U elsey) {
        // FIXME: Missing optimized homogeneous here (still in AST but will be missed by Prism).

        Operand testValue = buildCaseTestValue(predicate); // what each when arm gets tested against.
        Label elseLabel = getNewLabel();                  // where else body is location (implicit or explicit).
        Label endLabel = getNewLabel();                   // end of the entire case statement.
        boolean hasExplicitElse = elsey != null; // does this have an explicit 'else' or not.
        Variable result = temp();      // final result value of the case statement.
        Map<Label, U> bodies = new HashMap<>();        // we save bodies and emit them after processing when values.
        Set<IRubyObject> seenLiterals = new HashSet<>();  // track to warn on duplicated values in when clauses.
        Map<IRubyObject, java.lang.Integer> originalLocs = new HashMap<>();

        for (U arm: arms) { // Emit each when value test against the case value.
            Label bodyLabel = getNewLabel();
            buildWhenArgs((W) arm, testValue, bodyLabel, seenLiterals, originalLocs);
            bodies.put(bodyLabel, whenBody((W) arm));
        }

        addInstr(new JumpInstr(elseLabel));               // if no explicit matches jump to else

        if (hasExplicitElse) bodies.put(elseLabel, elsey);

        int numberOfBodies = bodies.size();
        int i = 1;
        for (Map.Entry<Label, U> entry: bodies.entrySet()) {
            addInstr(new LabelInstr(entry.getKey()));
            Operand bodyValue = build(entry.getValue());

            if (bodyValue != null) {                      // can be null if the body ends with a return!
                addInstr(new CopyInstr(result, bodyValue));

                //  we can omit the jump to the last body so long as we don't have an implicit else
                //  since that is emitted right after this section.
                if (i != numberOfBodies) {
                    addInstr(new JumpInstr(endLabel));
                } else if (!hasExplicitElse) {
                    addInstr(new JumpInstr(endLabel));
                }
            }
            i++;
        }

        if (!hasExplicitElse) {                           // build implicit else
            addInstr(new LabelInstr(elseLabel));
            addInstr(new CopyInstr(result, nil()));
        }

        addInstr(new LabelInstr(endLabel));

        return result;
    }

    protected abstract U whenBody(W arm);

    private Operand buildCaseTestValue(U test) {
        if (isLiteralString(test)) return frozen_string(test);

        // FIXME: AST will return null from build(test) but prism returns nil.  Added to workaround.
        if (test == null) return UndefinedValue.UNDEFINED;

        Operand testValue = build(test);

        // null is returned for valueless case statements:
        //   case
        //     when true <blah>
        //     when false <blah>
        //   end
        return testValue == null ? UndefinedValue.UNDEFINED : testValue;
    }

    protected Operand buildClass(ByteList className, U superNode, U cpath, U bodyNode, StaticScope scope, int line, int endLine) {
        boolean executesOnce = this.executesOnce;
        Operand superClass = superNode == null ? null : build(superNode);
        Operand container = getContainerFromCPath(cpath);

        IRClassBody body = new IRClassBody(getManager(), this.scope, className, line, scope, executesOnce);
        Variable bodyResult = addResultInstr(new DefineClassInstr(temp(), body, container, superClass));

        getManager().getBuilderFactory().newIRBuilder(getManager(), body, this, encoding).buildModuleOrClassBody(bodyNode, line, endLine);
        return bodyResult;
    }

    protected abstract Operand getContainerFromCPath(U cpath);

    protected Operand buildClassVar(Variable result, RubySymbol name) {
        if (result == null) result = temp();
        if (isTopScope()) return addRaiseError("RuntimeError", "class variable access from toplevel");

        return addResultInstr(new GetClassVariableInstr(result, classVarDefinitionContainer(), name));
    }

    protected Operand buildClassVarAsgn(RubySymbol name, U valueNode) {
        if (isTopScope()) return addRaiseError("RuntimeError", "class variable access from toplevel");

        Operand value = build(valueNode);
        addInstr(new PutClassVariableInstr(classVarDefinitionContainer(), name, value));
        return value;
    }

    // FIXME: AST needs variable passed in to work which I think means some context really needs to pass in the result at least in AST build?
    protected Operand buildConditional(Variable result, U predicate, U statements, U consequent) {
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

    protected Operand buildDefn(IRMethod method) {
        addInstr(new DefineInstanceMethodInstr(method));
        return new Symbol(method.getName());
    }

    protected Operand buildDefs(U receiver, IRMethod method) {
        addInstr(new DefineClassMethodInstr(build(receiver), method));
        return new Symbol(method.getName());
    }

    protected Operand buildDRegex(Variable result, U[] children, RegexpOptions options) {
        Operand[] pieces = new Operand[children.length];
        Encoding encoding = options.getKCode().getEncoding();

        for (int i = 0; i < children.length; i++) {
            dynamicPiece(pieces, i, children[i], encoding); // dregexp does not use estimated size
        }

        if (result == null) result = temp();
        addInstr(new BuildDynRegExpInstr(result, pieces, options));
        return result;
    }

    // Pre-Ruby 3.4 path for JRuby 9.4.x.
    @Deprecated
    protected Operand buildDStr(Variable result, U[] nodePieces, Encoding encoding, boolean isFrozen, int line) {
        if (result == null) result = temp();

        Operand[] pieces = new Operand[nodePieces.length];
        int estimatedSize = 0;

        for (int i = 0; i < pieces.length; i++) {
            estimatedSize += dynamicPiece(pieces, i, nodePieces[i], null);
        }

        addInstr(new BuildCompoundStringInstr(result, pieces, encoding, estimatedSize, isFrozen ? Frozen : Mutable, getFileName(), line));

        return result;
    }

    protected Operand buildDStr(Variable result, U[] nodePieces, Encoding encoding, StringStyle stringStyle, int line) {
        if (result == null) result = temp();

        Operand[] pieces = new Operand[nodePieces.length];
        int estimatedSize = 0;

        for (int i = 0; i < pieces.length; i++) {
            estimatedSize += dynamicPiece(pieces, i, nodePieces[i], null);
        }

        addInstr(new BuildCompoundStringInstr(result, pieces, encoding, estimatedSize, stringStyle, getFileName(), line));

        return result;
    }

    protected Operand buildDSymbol(Variable result, U[] nodePieces, Encoding encoding, int line) {
        return copy(new DynamicSymbol(buildDStr(result, nodePieces, encoding, false, line)));
    }

    public Operand buildDXStr(Variable result, U[] nodePieces, Encoding encoding, int line) {
        return fcall(result, buildSelf(), "`", buildDStr(result, nodePieces, encoding, false, line));
    }
    protected Operand buildEncoding(Encoding encoding) {
        return addResultInstr(new GetEncodingInstr(temp(), encoding));
    }

    /* ----------------------------------------------------------------------
     * Consider a simple 2-state (s1, s2) FSM with the following transitions:
     *
     *     new_state(s1, F) = s1
     *     new_state(s1, T) = s2
     *     new_state(s2, F) = s2
     *     new_state(s2, T) = s1
     *
     * Here is the pseudo-code for evaluating the flip-node.
     * Let 'v' holds the value of the current state.
     *
     *    1. if (v == 's1') f1 = eval_condition(s1-condition); v = new_state(v, f1); ret = f1
     *    2. if (v == 's2') f2 = eval_condition(s2-condition); v = new_state(v, f2); ret = true
     *    3. return ret
     *
     * For exclusive flip conditions, line 2 changes to:
     *    2. if (!f1 && (v == 's2')) f2 = eval_condition(s2-condition); v = new_state(v, f2)
     *
     * In IR code below, we are representing the two states as 1 and 2.  Any
     * two values are good enough (even true and false), but 1 and 2 is simple
     * enough and also makes the IR output readable
     * ---------------------------------------------------------------------- */
    protected Operand buildFlip(U begin, U end, boolean isExclusive) {
            Fixnum s1 = manager.newFixnum(1);
            Fixnum s2 = manager.newFixnum(2);

            // Create a variable to hold the flip state
            IRBuilder<U, V, W, X, Y, Z> nearestNonClosureBuilder = getNearestFlipVariableScopeBuilder();

            // Flip is completely broken atm and it was semi-broken in its last incarnation.
            // Method and closures (or evals) are not built at the same time and if -X-C or JIT or AOT
            // and jit.threshold=0 then the non-closure where we want to store the hidden flip variable
            // is unable to get more instrs added to it (not quite true for -X-C but definitely true
            // for JIT/AOT.  Also it means needing to grow the size of any heap scope for variables.
            if (nearestNonClosureBuilder == null) {
                Variable excType = createTemporaryVariable();
                addInstr(new InheritanceSearchConstInstr(excType, manager.getObjectClass(),
                        manager.runtime.newSymbol(CommonByteLists.NOT_IMPLEMENTED_ERROR)));
                Variable exc = addResultInstr(CallInstr.create(scope, NORMAL, temp(), symbol(CommonByteLists.NEW),
                        excType, new Operand[] {new FrozenString("Flip support currently broken")}, NullBlock.INSTANCE, 0));
                addInstr(new ThrowExceptionInstr(exc));
                return nil();
            }
            // FIXME: This should be special local variable added at this builders SCOPE.
            Variable flipState = nearestNonClosureBuilder.getNewFlipFlopStateVariable();
            nearestNonClosureBuilder.initFlipStateVariable(flipState, s1);
            if (scope instanceof IRClosure) {
                // Clone the flip variable to be usable at the proper-depth.
                int n = 0;
                IRScope x = scope;
                while (!x.isWhereFlipFlopStateVariableIs()) {
                    n++;
                    x = x.getLexicalParent();
                }
                if (n > 0) flipState = ((LocalVariable)flipState).cloneForDepth(n);
            }

            // Variables and labels needed for the code
            Variable returnVal = createTemporaryVariable();
            Label    s2Label   = getNewLabel();
            Label    doneLabel = getNewLabel();

            // Init
            addInstr(new CopyInstr(returnVal, manager.getFalse()));

            // Are we in state 1?
            addInstr(BNEInstr.create(s2Label, flipState, s1));

            // ----- Code for when we are in state 1 -----
            Operand s1Val = build(begin);
            addInstr(new BFalseInstr(s2Label, s1Val));

            // s1 condition is true => set returnVal to true & move to state 2
            addInstr(new CopyInstr(returnVal, tru()));
            addInstr(new CopyInstr(flipState, s2));

            // Check for state 2
            addInstr(new LabelInstr(s2Label));

            // For exclusive ranges/flips, we dont evaluate s2's condition if s1's condition was satisfied
            if (isExclusive) addInstr(createBranch(returnVal, tru(), doneLabel));

            // Are we in state 2?
            addInstr(BNEInstr.create(doneLabel, flipState, s2));

            // ----- Code for when we are in state 2 -----
            Operand s2Val = build(end);
            addInstr(new CopyInstr(returnVal, tru()));
            addInstr(new BFalseInstr(doneLabel, s2Val));

            // s2 condition is true => move to state 1
            addInstr(new CopyInstr(flipState, s1));

            // Done testing for s1's and s2's conditions.
            // returnVal will have the result of the flip condition
            addInstr(new LabelInstr(doneLabel));

            return returnVal;
    }

    private Variable getNewFlipFlopStateVariable() {
        scope.setHasFlipFlops(true);
        return getNewLocalVariable(symbol("%flip" + flipVariableCount++), 0);
    }

    protected Operand buildFor(U receiverNode, U var, U body, StaticScope staticScope, Signature signature, int line, int endLine) {
        Variable result = temp();
        Operand  receiver = build(receiverNode);
        Operand  forBlock = buildForIter(var, body, staticScope, signature, line, endLine);
        CallInstr callInstr = new CallInstr(scope, CallType.NORMAL, result, symbol(CommonByteLists.EACH), receiver,
                EMPTY_OPERANDS, forBlock, 0, scope.maybeUsingRefinements());
        receiveBreakException(forBlock, callInstr);

        return result;
    }

    protected Operand buildForIter(U var, U body, StaticScope staticScope, Signature signature, int line, int endLine) {
        // Create a new closure context
        IRClosure closure = new IRFor(getManager(), scope, line, staticScope, signature);

        // Create a new nested builder to ensure this gets its own IR builder state like the ensure block stack
        getManager().getBuilderFactory().newIRBuilder(getManager(), closure, this, encoding).buildIterInner(null, var, body, endLine);

        return new WrappedIRClosure(buildSelf(), closure);
    }

    protected Operand buildGlobalAsgn(RubySymbol name, U valueNode) {
        Operand value = build(valueNode);
        addInstr(new PutGlobalVarInstr(name, value));
        return value;
    }

    protected Operand buildGlobalVar(Variable result, RubySymbol name) {
        if (result == null) result = temp();

        return addResultInstr(new GetGlobalVariableInstr(result, name));
    }

    protected Operand buildInstAsgn(RubySymbol name, U valueNode) {
        Operand value = build(valueNode);
        addInstr(new PutFieldInstr(buildSelf(), name, value));
        return value;
    }

    protected Operand buildInstVar(RubySymbol name) {
        return addResultInstr(new GetFieldInstr(temp(), buildSelf(), name, false));
    }

    protected Variable buildClassVarGetDefinition(RubySymbol name) {
        return addResultInstr(
                new RuntimeHelperCall(
                        temp(),
                        IS_DEFINED_CLASS_VAR,
                        new Operand[]{
                                classVarDefinitionContainer(),
                                new FrozenString(name),
                                new FrozenString(DefinedMessage.CLASS_VARIABLE.getText())
                        }
                )
        );
    }

    // FIXME: This could be a helper
    protected Variable buildConstantGetDefinition(RubySymbol name) {
        Label defLabel = getNewLabel();
        Label doneLabel = getNewLabel();
        Variable tmpVar = temp();
        addInstr(new LexicalSearchConstInstr(tmpVar, CurrentScope.INSTANCE, name));
        addInstr(BNEInstr.create(defLabel, tmpVar, UndefinedValue.UNDEFINED));
        addInstr(new InheritanceSearchConstInstr(tmpVar, findContainerModule(), name)); // SSS FIXME: should this be the current-module var or something else?
        addInstr(BNEInstr.create(defLabel, tmpVar, UndefinedValue.UNDEFINED));
        addInstr(new CopyInstr(tmpVar, nil()));
        addInstr(new JumpInstr(doneLabel));
        addInstr(new LabelInstr(defLabel));
        addInstr(new CopyInstr(tmpVar, new FrozenString(DefinedMessage.CONSTANT.getText())));
        addInstr(new LabelInstr(doneLabel));
        return tmpVar;
    }

    protected Variable buildGlobalVarGetDefinition(RubySymbol name) {
        return addResultInstr(
                new RuntimeHelperCall(
                        temp(),
                        IS_DEFINED_GLOBAL,
                        new Operand[] {
                                new FrozenString(name),
                                new FrozenString(DefinedMessage.GLOBAL_VARIABLE.getText())
                        }
                )
        );
    }

    protected Operand buildInstVarGetDefinition(RubySymbol name) {
        Variable result = temp();
        Label done = getNewLabel();
        Label undefined = getNewLabel();
        Variable value = addResultInstr(new GetFieldInstr(temp(), buildSelf(), name, true));
        addInstr(createBranch(value, UndefinedValue.UNDEFINED, undefined));
        copy(result, new FrozenString(DefinedMessage.INSTANCE_VARIABLE.getText()));
        jump(done);
        addInstr(new LabelInstr(undefined));
        copy(result, nil());
        addInstr(new LabelInstr(done));

        return result;
    }

    protected Operand buildIter(U var, U body, StaticScope staticScope, Signature signature, int line, int endLine) {
        ByteList prefix = createPrefixForIter(var);
        IRClosure closure = new IRClosure(getManager(), scope, line, staticScope, signature, prefix, coverageMode);

        // Create a new nested builder to ensure this gets its own IR builder state like the ensure block stack
        getManager().getBuilderFactory().newIRBuilder(getManager(), closure, this, encoding).buildIterInner(methodName, var, body, endLine);

        methodName = null;

        return new WrappedIRClosure(buildSelf(), closure);
    }

    private ByteList createPrefixForLambda(U var) {
        ByteList prefix = new ByteList("->(".getBytes());
        createPrefixFromArgs(prefix, var);
        prefix.append(')');

        return prefix;
    }

    private ByteList createPrefixForIter(U var) {
        ByteList prefix = new ByteList();
        if (methodName != null) {
            prefix.append(methodName.getBytes());
            prefix.append(" ".getBytes());
        }
        prefix.append("&|".getBytes());
        createPrefixFromArgs(prefix, var);
        prefix.append('|');

        return prefix;
    }

    protected void createPrefixFromArgs(ByteList prefix, U var) {
        // FIXME: Made for backwards compat for Prism not having this method in all versions (0.15).
    }

    protected void buildIterInner(RubySymbol methodName, U var, U body, int endLine) {
        long time = 0;
        if (parserTiming) time = System.nanoTime();
        this.methodName = methodName;

        boolean forNode = scope instanceof IRFor;

        if (RubyInstanceConfig.FULL_TRACE_ENABLED) {
            addInstr(new TraceInstr(RubyEvent.B_CALL, getCurrentModuleVariable(), getName(), getFileName(), scope.getLine() + 1));
        }

        if (forNode) {
            receiveForArgs(var);
        } else {
            receiveBlockArgs(var);
        }

        // conceptually abstract prologue scope instr creation so we can put this at the end of it instead of replicate it.
        afterPrologueIndex = instructions.size();

        // Build closure body and return the result of the closure
        Operand closureRetVal = body == null ? nil() : build(body);

        if (RubyInstanceConfig.FULL_TRACE_ENABLED) {
            addInstr(new TraceInstr(RubyEvent.B_RETURN, getCurrentModuleVariable(), getName(), getFileName(), endLine + 1));
        }

        // can be U_NIL if the node is an if node with returns in both branches.
        if (closureRetVal != U_NIL) addInstr(new ReturnInstr(closureRetVal));

        prependUsedClosureImplicitState(forNode);

        // Add break/return handling in case it is a lambda (we cannot know at parse time what it is).
        // SSS FIXME: At a later time, see if we can optimize this and do this on demand.
        if (!forNode) handleBreakAndReturnsInLambdas();

        computeScopeFlagsFrom(instructions);
        scope.allocateInterpreterContext(instructions, temporaryVariableIndex + 1, flags);

        if (parserTiming) manager.getRuntime().getParserManager().getParserStats().addIRBuildTime(System.nanoTime() - time);
    }

    public Operand buildLambda(U args, U body, StaticScope staticScope, Signature signature, int line) {
        IRClosure closure = new IRClosure(getManager(), scope, line, staticScope, signature,
                createPrefixForLambda(args), coverageMode);

        // Create a new nested builder to ensure this gets its own IR builder state like the ensure block stack
        getManager().getBuilderFactory().newIRBuilder(getManager(), closure, this, encoding).buildLambdaInner(args, body);

        Variable lambda = temp();
        WrappedIRClosure lambdaBody = new WrappedIRClosure(closure.getSelf(), closure);
        addInstr(new BuildLambdaInstr(lambda, lambdaBody));
        return lambda;
    }

    protected void buildLambdaInner(U blockArgs, U body) {
        long time = 0;
        if (parserTiming) time = System.nanoTime();

        receiveBlockArgs(blockArgs);

        Operand closureRetVal = build(body);

        // can be U_NIL if the node is an if node with returns in both branches.
        if (closureRetVal != U_NIL) addInstr(new ReturnInstr(closureRetVal));

        prependUsedClosureImplicitState(false);

        handleBreakAndReturnsInLambdas();

        computeScopeFlagsFrom(instructions);
        scope.allocateInterpreterContext(instructions, temporaryVariableIndex + 1, flags);

        if (parserTiming) manager.getRuntime().getParserManager().getParserStats().addIRBuildTime(System.nanoTime() - time);
    }

    protected Operand buildLocalVariableAssign(RubySymbol name, int depth, U valueNode) {
        Variable variable  = getLocalVariable(name, depth);
        Operand value = build(variable, valueNode);

        if (variable != value) copy(variable, value);  // no use copying a variable to itself

        return variable;

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

    protected Operand buildConditionalLoop(U conditionNode, U bodyNode, boolean isWhile, boolean isLoopHeadCondition) {
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
            if (loop.hasNext) addInstr(new LabelInstr(loop.iterEndLabel));
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
            if (loop.hasBreak) addInstr(new LabelInstr(loop.loopEndLabel));

            // Done with loop
            loopStack.pop();

            return loopResult;
        }
    }

    protected Variable buildDefinitionCheck(ResultInstr definedInstr, String definedReturnValue) {
        Label undefLabel = getNewLabel();
        addInstr((Instr) definedInstr);
        addInstr(createBranch(definedInstr.getResult(), fals(), undefLabel));
        return buildDefnCheckIfThenPaths(undefLabel, new FrozenString(definedReturnValue));
    }

    protected Variable buildDefnCheckIfThenPaths(Label undefLabel, Operand defVal) {
        Label defLabel = getNewLabel();
        Variable tmpVar = getValueInTemporaryVariable(defVal);
        addInstr(new JumpInstr(defLabel));
        addInstr(new LabelInstr(undefLabel));
        addInstr(new CopyInstr(tmpVar, nil()));
        addInstr(new LabelInstr(defLabel));
        return tmpVar;
    }

    public Operand buildMatch(Variable result, Operand regexp) {
        Variable lastLine = addResultInstr(new GetGlobalVariableInstr(temp(), symbol("$_")));

        if (result == null) result = temp();
        return addResultInstr(new MatchInstr(scope, result, regexp, lastLine));
    }

    protected Operand buildModule(ByteList name, U cpath, U bodyNode, StaticScope scope, int line, int endLine) {
        boolean executesOnce = this.executesOnce;
        Operand container = getContainerFromCPath(cpath);

        IRModuleBody body = new IRModuleBody(getManager(), this.scope, name, line, scope, executesOnce);
        Variable bodyResult = addResultInstr(new DefineModuleInstr(temp(), container, body));

        getManager().getBuilderFactory().newIRBuilder(getManager(), body, this, encoding).buildModuleOrClassBody(bodyNode, line, endLine);

        return bodyResult;
    }

    protected void buildModuleOrClassBody(U body, int startLine, int endLine) {
        addInstr(new TraceInstr(RubyEvent.CLASS, getCurrentModuleVariable(), null, getFileName(), startLine + 1));

        Operand bodyReturnValue = build(body);

        // This is only added when tracing is enabled because an 'end' will normally have no other instrs which can
        // raise after this point.  When we add trace we need to add one so backtrace generated shows the 'end' line.
        addInstr(getManager().newLineNumber(endLine));
        addInstr(new TraceInstr(RubyEvent.END, getCurrentModuleVariable(), null, getFileName(), endLine + 1));

        addInstr(new ReturnInstr(bodyReturnValue));

        prependUsedImplicitState(null);

        computeScopeFlagsFrom(instructions);
        scope.allocateInterpreterContext(instructions, temporaryVariableIndex + 1, flags);
    }

    protected Operand buildNext(final Operand rv, int line) {
        IRLoop currLoop = getCurrentLoop();

        // If we have ensure blocks, have to run those first!
        if (!activeEnsureBlockStack.isEmpty()) emitEnsureBlocks(currLoop);

        if (currLoop != null) {
            currLoop.hasNext = true;
            // If a regular loop, the next is simply a jump to the end of the iteration
            addInstr(new JumpInstr(currLoop.iterEndLabel));
        } else {
            addInstr(new ThreadPollInstr(true));
            // If a closure, the next is simply a return from the closure!
            if (scope instanceof IRClosure) {
                if (scope instanceof IREvalScript) {
                    throwSyntaxError(line, "Can't escape from eval with next");
                } else {
                    addInstr(new ReturnInstr(rv));
                }
            } else {
                throwSyntaxError(line, "Invalid next");
            }
        }

        // Once the "next instruction" (closure-return) executes, control exits this scope
        return U_NIL;
    }

    protected Operand buildNthRef(int matchNumber) {
        return copy(new NthRef(scope, matchNumber));
    }

    // FIXME: The logic for lazy and non-lazy building is pretty icky...clean up
    protected Operand buildOpAsgn(U receiver, U value, RubySymbol reader, RubySymbol writer, RubySymbol operator, boolean isLazy) {
        Label l;
        Variable writerValue = temp();

        // get attr
        Operand  v1 = build(receiver);
        CallType callType = v1 == Self.SELF ? FUNCTIONAL : NORMAL;

        Label lazyLabel = null;
        Label endLabel = null;
        Variable result = temp();
        if (isLazy) {
            lazyLabel = getNewLabel();
            endLabel = getNewLabel();
            addInstr(new BNilInstr(lazyLabel, v1));
        }

        Variable readerValue = _call(temp(), callType, v1, reader);

        // FIXME: consider separating this out since prism does already.
        // logical operations: e.val ||= n OR e.val &&= n
        boolean isOr = operator.idString().equals("||");
        boolean isLogical =  isOr || operator.idString().equals("&&");
        if (isLogical) {
            l = getNewLabel();
            addInstr(createBranch(readerValue, isOr ? tru() : fals(), l));

            // compute value and set it
            Operand  v2 = build(value);
            _call(writerValue, callType, v1, writer, v2);
            // It is readerValue = v2.
            // readerValue = writerValue is incorrect because the assignment method
            // might return something else other than the value being set!
            addInstr(new CopyInstr(readerValue, v2));
            addInstr(new LabelInstr(l));

            if (!isLazy) return readerValue;

            addInstr(new CopyInstr(result, readerValue));
        } else {  // Ex: e.val = e.val.f(n)
            // call operator
            Operand  v2 = build(value);
            Variable setValue = call(temp(), readerValue, operator, v2);

            // set attr
            _call(writerValue, callType, v1, writer, setValue);

            // Returning writerValue is incorrect because the assignment method
            // might return something else other than the value being set!
            if (!isLazy) return setValue;

            addInstr(new CopyInstr(result, setValue));
        }

        addInstr(new JumpInstr(endLabel));
        addInstr(new LabelInstr(lazyLabel));
        addInstr(new CopyInstr(result, nil()));
        addInstr(new LabelInstr(endLabel));

        return result;
    }

    // Translate "x &&= y" --> "x = y if is_true(x)" -->
    //
    //    x = -- build(x) should return a variable! --
    //    f = is_true(x)
    //    beq(f, false, L)
    //    x = -- build(y) --
    // L:
    //
    protected Operand buildOpAsgnAnd(CodeBlock lhs, CodeBlock rhs) {
        Label done = getNewLabel();
        Operand leftValue = lhs.run();
        Variable result = getValueInTemporaryVariable(leftValue);
        addInstr(createBranch(result, fals(), done));
        Operand value = rhs.run();
        copy(result, value);
        addInstr(new LabelInstr(done));
        return result;
    }

    protected Operand buildOpAsgnConstDeclOr(U left, U right, RubySymbol leftName) {
        Variable result = temp();
        Label falseCheck = getNewLabel();
        Label done = getNewLabel();
        Label assign = getNewLabel();
        Operand module = buildColon2ForConstAsgnDeclNode(left, result, false);
        addInstr(BNEInstr.create(falseCheck, result, UndefinedValue.UNDEFINED));
        addInstr(new JumpInstr(assign));
        addInstr(new LabelInstr(falseCheck));
        addInstr(new BTrueInstr(done, result));
        addInstr(new LabelInstr(assign));
        Operand rhsValue = build(right);
        copy(result, rhsValue);
        addInstr(new PutConstInstr(module, leftName, rhsValue));
        addInstr(new LabelInstr(done));
        return result;
    }

    protected Operand buildOpAsgnConstDeclAnd(U left, U right, RubySymbol leftName) {
        Variable result = temp();
        Label done = getNewLabel();
        Operand module = buildColon2ForConstAsgnDeclNode(left, result, true);
        addInstr(new BFalseInstr(done, result));
        Operand rhsValue = build(right);
        copy(result, rhsValue);
        addInstr(new PutConstInstr(module, leftName, rhsValue));
        addInstr(new LabelInstr(done));
        return result;
    }

    protected abstract Operand buildColon2ForConstAsgnDeclNode(U lhs, Variable valueResult, boolean constMissing);

    @Deprecated
    protected Operand buildOpAsgnConstDecl(Y left, U right, RubySymbol operator) {
        Operand lhs = build((U) left);
        Operand rhs = build(right);
        Variable result = call(temp(), lhs, operator, rhs);
        return copy(temp(), putConstant(left, result));
    }

    protected Operand buildOpAsgnConstDecl(Y left, RubySymbol name, U right, RubySymbol operator) {
        Operand parent = buildColon2ForConstAsgnDeclNode((U) left, temp(), false);
        Operand lhs = searchModuleForConst(temp(), parent, name);
        Operand rhs = build(right);
        Variable result = call(temp(), lhs, operator, rhs);
        return copy(temp(), putConstant(parent, name, result));
    }

    @Deprecated
    protected abstract Operand putConstant(Y constant, Operand value);
    protected abstract Operand putConstant(Y constant, CodeBlock value);

    protected Operand buildOpAsgnOr(CodeBlock lhs, CodeBlock rhs) {
        Label done = getNewLabel();
        Variable result = (Variable) lhs.run();
        addInstr(createBranch(result, tru(), done));
        Operand value = rhs.run();
        copy(result, value);
        addInstr(new LabelInstr(done));
        return result;
    }

    protected Operand buildOpElementAsgnWith(U receiver, U args, U block, U value, Boolean truthy) {
        Operand array = buildWithOrder(receiver, containsVariableAssignment(args) || containsVariableAssignment(value));
        CallType callType = array == Self.SELF ? FUNCTIONAL : CallType.NORMAL;
        Label endLabel = getNewLabel();
        Variable elt = temp();
        int[] flags = new int[] { 0 };
        Operand[] argList = setupCallArgs(args, flags);
        Operand blockArg = setupCallClosure(args, block);
        addInstr(CallInstr.create(scope, callType, elt, symbol(ArrayDerefInstr.AREF), array, argList, blockArg, flags[0]));
        addInstr(createBranch(elt, truthy, endLabel));
        Operand valueArg = build(value);

        argList = addArg(argList, valueArg);
        addInstr(CallInstr.create(scope, callType, elt, symbol(ArrayDerefInstr.ASET), array, argList, blockArg, flags[0]));
        addInstr(new CopyInstr(elt, valueArg));

        addInstr(new LabelInstr(endLabel));
        return elt;
    }

    // a[i] *= n, etc.  anything that is not "a[i] &&= .. or a[i] ||= .."
    protected Operand buildOpElementAsgnWithMethod(U receiver, U args, U block, U value, RubySymbol operator) {
        Operand array = buildWithOrder(receiver, containsVariableAssignment(args) || containsVariableAssignment(value));
        CallType callType = array == Self.SELF ? FUNCTIONAL : CallType.NORMAL;
        int[] flags = new int[] { 0 };
        Operand[] argList = setupCallArgs(args, flags);
        Operand blockArg = setupCallClosure(args, block);
        Variable elt = temp();
        addInstr(CallInstr.create(scope, callType, elt, symbol(ArrayDerefInstr.AREF), array, argList, blockArg, flags[0])); // elt = a[args]

        Operand valueArg = build(value);                                       // Load 'value'
        _call(elt, callType, elt, operator, valueArg); // elt = elt.OPERATION(value)
        // SSS: do not load the call result into 'elt' to eliminate the RAW dependency on the call
        // We already know what the result is going be .. we are just storing it back into the array
        argList = addArg(argList, elt);
        addInstr(CallInstr.create(scope, callType, temp(), symbol(ArrayDerefInstr.ASET), array, argList, blockArg, flags[0]));   // a[args] = elt
        return elt;
    }

    // "x ||= y"
    // --> "x = (is_defined(x) && is_true(x) ? x : y)"
    // --> v = -- build(x) should return a variable! --
    //     f = is_true(v)
    //     beq(f, true, L)
    //     -- build(x = y) --
    //   L:
    //
    protected Operand buildOpAsgnOrWithDefined(final U first, U second) {
        Label    l1 = getNewLabel();
        Label    l2 = getNewLabel();
        Variable flag = temp();
        Operand  v1 = buildGetDefinition(first);
        addInstr(new CopyInstr(flag, v1));
        addInstr(createBranch(flag, nil(), l2)); // if v1 is undefined, go to v2's computation
        v1 = build(first); // build of 'x'
        addInstr(new CopyInstr(flag, v1));
        Variable result = getValueInTemporaryVariable(v1);
        addInstr(new LabelInstr(l2));
        addInstr(createBranch(flag, tru(), l1));  // if v1 is defined and true, we are done!
        Operand v2 = build(second); // This is an AST node that sets x = y, so nothing special to do here.
        addInstr(new CopyInstr(result, v2));
        addInstr(new LabelInstr(l1));

        // Return value of x ||= y is always 'x'
        return result;
    }

    protected Operand buildOr(Operand left, CodeBlock right, BinaryType type) {
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

    protected Variable deconstructHashPatternKeys(Label testEnd, Variable errorString, U constantNode, U[] keyNodes, U rest, Variable result,
                                        Operand obj, boolean isSinglePattern) {
        Operand keys;

        if ((keyNodes != null && keyNodes.length > 0 || rest != null) && !hasNamedRest(rest)) {
            int length = keyNodes.length;
            Operand[] builtKeys = new Operand[length];

            for (int i = 0; i < length; i++) {
                builtKeys[i] = build(keyNodes[i]);
            }
            keys = new Array(builtKeys);
        } else {
            keys = nil();
        }

        if (constantNode != null) buildPatternConstant(testEnd, result, constantNode, obj, isSinglePattern, errorString);

        buildPatternDeconstructRespondTo(testEnd, result, obj, isSinglePattern, errorString, "deconstruct_keys");

        return call(temp(), obj, "deconstruct_keys", keys);
    }

    private boolean hasNamedRest(U rest) {
        return rest != null && !isBareStar(rest);
    }

    protected abstract U getInExpression(U node);
    protected abstract U getInBody(U node);
    protected abstract boolean isBareStar(U node);

    protected void buildArrayPattern(Label testEnd, Variable result, Variable deconstructed, U constant, U[] pre,
                                   U rest, U[] post, Operand obj, boolean inAlteration, boolean isSinglePattern,
                                   Variable errorString) {
        Variable restNum = addResultInstr(new CopyInstr(intTemp(), new Integer(0)));

        if (constant != null) buildPatternConstant(testEnd, result, constant, obj, isSinglePattern, errorString);

        buildPatternDeconstructRespondTo(testEnd, result, obj, isSinglePattern, errorString, "deconstruct");

        label("deconstruct_cache_end", (deconstruct_cache_end) ->
                cond_ne_nil(deconstruct_cache_end, deconstructed, () -> {
                    call(deconstructed, obj, "deconstruct");
                    label("array_check_end", arrayCheck -> {
                        addInstr(new EQQInstr(scope, result, getManager().getArrayClass(), deconstructed, false, true, false));
                        cond(arrayCheck, result, tru(), () -> type_error("deconstruct must return Array"));
                    });
                })
        );

        int preArgsSize = pre == null ? 0 : pre.length;
        int postArgsSize = post == null ? 0 : post.length;
        Operand minArgsCount = new Integer(preArgsSize + postArgsSize);
        Variable length = addResultInstr(new RuntimeHelperCall(intTemp(), ARRAY_LENGTH, new Operand[]{deconstructed}));

        buildPatternArrayLengthCheck(testEnd, result, deconstructed, isSinglePattern, errorString, length, minArgsCount, rest != null);

        if (preArgsSize > 0) {
            for (int i = 0; i < preArgsSize; i++) {
                Variable elt = call(temp(), deconstructed, "[]", fix(i));
                buildPatternEach(testEnd, result, obj, copy(nil()), elt, pre[i], inAlteration, isSinglePattern, errorString);
                cond_ne_true(testEnd, result);
            }
        }

        if (rest != null) {
            addInstr(new IntegerMathInstr(SUBTRACT, restNum, length, minArgsCount));

            if (!isBareStar(rest)) {
                Variable min = copy(fix(preArgsSize));
                Variable max = as_fixnum(restNum);
                Variable elt = call(temp(), deconstructed, "[]", min, max);

                buildPatternMatch(result, obj, copy(nil()), rest, elt, inAlteration, isSinglePattern, errorString);
                cond_ne_true(testEnd, result);
            }
        }

        if (postArgsSize > 0) {
            for (int i = 0; i < postArgsSize; i++) {
                Variable j = addResultInstr(new IntegerMathInstr(ADD, intTemp(), new Integer(i + preArgsSize), restNum));
                Variable k = as_fixnum(j);
                Variable elt = call(temp(), deconstructed, "[]", k);

                buildPatternEach(testEnd, result, obj, copy(nil()), elt, post[i], inAlteration, isSinglePattern, errorString);
                cond_ne_true(testEnd, result);
            }
        }
    }

    private void buildPatternConstant(Label testEnd, Variable result, U constant, Operand obj, boolean isSinglePattern, Variable errorString) {
        Operand expression = build(constant);
        addInstr(new EQQInstr(scope, result, expression, obj, false, true, true));
        if (isSinglePattern) {
            buildPatternSetEQQError(errorString, result, obj, expression, obj);
        }
        cond_ne_true(testEnd, result);
    }

    protected void buildPatternSetEQQError(Variable errorString, Variable result, Operand obj, Operand expression, Operand value) {
        buildPatternSetGeneralError(errorString, result, new FrozenString("%s: %s === %s does not return true"), obj, expression, value);
    }

    protected void buildPatternSetGeneralError(Variable errorString, Variable result, Operand... args) {
        label("match_succeeded", (matchSucceeded) -> {
            cond_ne_false(matchSucceeded, result);
            fcall(errorString, getManager().getObjectClass(), "sprintf", args);
        });
    }

    protected void buildFindPattern(Label testEnd, Variable result, Variable deconstructed, U constant, U pre,
                          U[] args, U post, Operand obj, boolean inAlteration, boolean isSinglePattern,
                          Variable errorString) {
        if (constant != null) buildPatternConstant(testEnd, result, constant, obj, isSinglePattern, errorString);

        label("deconstruct_end", deconstructCheck ->
            cond_ne_nil(deconstructCheck, deconstructed, () -> {
                buildPatternDeconstructRespondTo(testEnd, result, obj, isSinglePattern, errorString, "deconstruct");

                call(deconstructed, obj, "deconstruct");
                label("array_check_end", arrayCheck -> {
                    addInstr(new EQQInstr(scope, result, getManager().getArrayClass(), deconstructed, false, true, false));
                    cond(arrayCheck, result, tru(), () -> type_error("deconstruct must return Array"));
                });
            })
        );

        Variable length = addResultInstr(new RuntimeHelperCall(intTemp(), ARRAY_LENGTH, new Operand[]{deconstructed}));
        int fixedArgsLength = args.length;
        Operand minArgsCount = new Integer(fixedArgsLength);
        boolean hasRest = pre != null || post != null;

        buildPatternArrayLengthCheck(testEnd, result, deconstructed, isSinglePattern, errorString, length, minArgsCount, hasRest);

        Variable limit = addResultInstr(new IntegerMathInstr(SUBTRACT, intTemp(), length, minArgsCount));
        Variable i = copy(new Integer(0));

        for_loop(after -> addInstr(new BIntInstr(after, BIntInstr.Op.GT, i, limit)),
                after -> addInstr(new IntegerMathInstr(ADD, i, i, new Integer(1))),
                (after, bottom) -> {
                    times(fixedArgsLength, (end_times, j) -> {
                        U pat = args[j.value];
                        Operand deconstructIndex = addResultInstr(new IntegerMathInstr(ADD, intTemp(), i, new Integer(j.value)));
                        Operand deconstructFixnum = as_fixnum(deconstructIndex);
                        Operand test = call(temp(), deconstructed, "[]", deconstructFixnum);
                        buildPatternMatch(result, obj, copy(nil()), pat, test, false, isSinglePattern, errorString);
                        cond_ne_true(bottom, result);
                    });

                    if (pre != null && !isBareStar(pre)) {
                        Operand iFixnum = as_fixnum(i);
                        Operand test = call(temp(), deconstructed, "[]", getManager().newFixnum(0), iFixnum);
                        buildPatternMatch(result, obj, copy(nil()), pre, test, false, isSinglePattern, errorString);
                        cond_ne_true(bottom, result);
                    }

                    if (post != null && !isBareStar(post)) {
                        Operand deconstructIndex = addResultInstr(new IntegerMathInstr(ADD, intTemp(), i, minArgsCount));
                        Operand deconstructFixnum = as_fixnum(deconstructIndex);
                        Operand lengthFixnum = as_fixnum(length);
                        Operand test = call(temp(), deconstructed, "[]", deconstructFixnum, lengthFixnum);
                        buildPatternMatch(result, obj, copy(nil()), post, test, false, isSinglePattern, errorString);
                        cond_ne_true(bottom, result);
                    }
                    jump(after);
                });
    }

    private void buildPatternArrayLengthCheck(Label testEnd, Variable result, Variable deconstructed, boolean isSinglePattern, Variable errorString, Variable length, Operand minArgsCount, boolean hasRest) {
        label("size_check_end", minArgsCheck -> {
            BIntInstr.Op compareOp = hasRest ? BIntInstr.Op.GTE : BIntInstr.Op.EQ;
            addInstr(new BIntInstr(minArgsCheck, compareOp, length, minArgsCount));
            if (isSinglePattern) {
                fcall(errorString, getManager().getObjectClass(), "sprintf",
                        new FrozenString("%s: %s length mismatch (given %d, expected %d" + (hasRest ? "+" : "") + ")"), deconstructed, deconstructed, as_fixnum(length), as_fixnum(minArgsCount));
            }
            addInstr(new CopyInstr(result, fals()));
            jump(testEnd);
        });
    }

    private void buildPatternDeconstructRespondTo(Label testEnd, Variable result, Operand obj, boolean isSinglePattern,
                                                  Variable errorString, String methodName) {
        call(result, obj, "respond_to?", new Symbol(symbol(methodName)));
        if (isSinglePattern) {
            buildPatternSetGeneralError(errorString, result, new FrozenString("%s: %s does not respond to #" + methodName), obj, obj);
        }
        cond_ne_true(testEnd, result);
    }

    protected Operand buildPatternCase(U test, U[] cases, U consequent) {
        Variable result = temp();
        Operand value = build(test);
        Variable errorString = copy(nil());

        label("pattern_case_end", end -> {
            List<Label> labels = new ArrayList<>();
            Map<Label, U> bodies = new HashMap<>();

            // build each "when"
            Variable deconstructed = copy(nil());
            boolean isSinglePattern = cases.length == 1;
            for (U in : cases) {
                Label bodyLabel = getNewLabel();

                U body = getInBody(in);

                Variable eqqResult = copy(tru());
                labels.add(bodyLabel);
                buildPatternMatch(eqqResult, value, deconstructed, getInExpression(in), value, false, isSinglePattern, errorString);
                addInstr(createBranch(eqqResult, tru(), bodyLabel));
                bodies.put(bodyLabel, body);
            }

            if (consequent != null) {
                Operand bodyValue = build(consequent);
                if (bodyValue != null) copy(result, bodyValue);
            } else {
                Variable inspect = temp();
                label("key_check_error", (key_check_end)-> {
                    // if errorString is a literal symbol then this is for key pattern error.  Normally it is a string or nil
                    Variable whichError = addResultInstr(new EQQInstr(scope, temp(), getManager().getSymbolClass(), errorString, false, true, false));
                    addInstr(createBranch(whichError, tru(), key_check_end));
                    if_else(errorString, nil(),
                            () -> call(inspect, value, "inspect"),
                            () -> copy(inspect, errorString));
                    addRaiseError("NoMatchingPatternError", inspect);
                    jump(end);
                });

                fcall(inspect, manager.getObjectClass(), "sprintf", new FrozenString("%s: key not found: :%s"), value, errorString);
                Operand exceptionClass = searchModuleForConst(temp(), getManager().getObjectClass(), symbol("NoMatchingPatternKeyError"));
                List<KeyValuePair<Operand, Operand>> kwargs = new ArrayList<>();
                kwargs.add(new KeyValuePair<>(new Symbol(symbol("key")), errorString));
                kwargs.add(new KeyValuePair<>(new Symbol(symbol("matchee")), value));
                Variable exception = addResultInstr(CallInstr.create(scope, NORMAL, temp(), symbol("new"), exceptionClass, new Operand[] { inspect, new Hash(kwargs) }, NullBlock.INSTANCE, CALL_KEYWORD));
                Operand kernel = searchModuleForConst(temp(), getManager().getObjectClass(), symbol("Kernel"));
                call(temp(), kernel, "raise", exception);
            }
            jump(end);

            // Now, emit bodies while preserving when clauses order
            for (Label label : labels) {
                addInstr(new LabelInstr(label));
                Operand bodyValue = build(bodies.get(label));
                if (bodyValue != null) copy(result, bodyValue);
                jump(end);
            }
        });

        return result;
    }

    protected abstract Variable buildPatternEach(Label testEnd, Variable result, Operand original, Variable deconstructed,
                                       Operand value, U exprNodes, boolean inAlternation, boolean isSinglePattern,
                                       Variable errorString);

    protected void buildPatternEachIf(Variable result, Operand original, Variable deconstructed, Operand value, U condition, U thenBody, U elseBody,
                            boolean inAlternation, boolean isSinglePattern, Variable errorString) {
        boolean unless; // position of body is how we detect between if/unless

        if (thenBody != null) { // if
            unless = false;
            buildPatternMatch(result, original, deconstructed, thenBody, value, inAlternation, isSinglePattern, errorString);
        } else {
            unless = true;
            buildPatternMatch(result, original, deconstructed, elseBody, value, inAlternation, isSinglePattern, errorString);
        }
        label("if_else_end", conditionalEnd -> {
            cond_ne_true(conditionalEnd, result);
            Operand ifResult = build(condition);
            if (unless) {
                call(result, ifResult, "!"); // FIXME: need non-dynamic dispatch to reverse result
            } else {
                addInstr(new CopyInstr(result, ifResult));
            }
        });
    }

    protected abstract void buildAssocs(Label testEnd, Operand original, Variable result, Z assocs, boolean inAlteration,
                              boolean isSinglePattern, Variable errorString, boolean hasRest, Variable d);

    protected void buildHashPattern(Label testEnd, Variable result, Variable deconstructed, U constant,
                                  Z assocs, U[] assocsKeys, U rest, Operand obj, boolean inAlteration,
                                  boolean isSinglePattern, Variable errorString) {
        boolean hasRest = rest != null;
        Variable d = deconstructHashPatternKeys(testEnd, errorString, constant, assocsKeys, rest, result, obj, isSinglePattern);

        label("hash_check_end", endHashCheck -> {
            addInstr(new EQQInstr(scope, result, getManager().getHashClass(), d, false, true, true));
            cond(endHashCheck, result, tru(), () -> type_error("deconstruct_keys must return Hash"));
        });

        // rest args destructively deletes elements from deconstruct_keys and the default impl is 'self'.
        if (hasRest) call(d, d, "dup");

        if (hasRest || assocsKeys.length > 0) {
            buildAssocs(testEnd, obj, result, assocs, inAlteration, isSinglePattern, errorString, hasRest, d);
        } else {
            call(result, d, "empty?");
            if (isSinglePattern) maybeGenerateIsNotEmptyErrorString(errorString, result, d);
            cond_ne_true(testEnd, result);
        }

        if (hasRest) {
            if (isNilRest(rest)) {
                call(result, d, "empty?");
                if (isSinglePattern) maybeGenerateIsNotEmptyErrorString(errorString, result, d);
                cond_ne_true(testEnd, result);
            } else if (!isBareStar(rest)) {
                buildPatternEach(testEnd, result, obj, copy(nil()), d, rest, inAlteration, isSinglePattern, errorString);
                cond_ne_true(testEnd, result);
            }
        }
    }

    protected void buildPatternEachHash(Label testEnd, Variable result, Operand original, Variable deconstructed, Operand value, U key, U assocValue, boolean inAlternation, boolean isSinglePattern, Variable errorString) {
        buildPatternMatch(result, original, deconstructed, key, value, inAlternation, isSinglePattern, errorString);
        buildPatternEach(testEnd, result, original, deconstructed, value, assocValue, inAlternation, isSinglePattern, errorString);
    }

    protected abstract boolean isNilRest(U rest);

    protected void buildPatternLocal(Operand value, RubySymbol name, int line, int depth, boolean inAlternation) {
        if (inAlternation && name.idString().charAt(0) != '_') {
            throwSyntaxError(line, str(getManager().getRuntime(), "illegal variable in alternative pattern (", name, ")"));
        }

        copy(getLocalVariable(name, depth), value);
    }

    protected void buildPatternOr(Label testEnd, Operand original, Variable result, Variable deconstructed, Operand value, U left,
                        U right, boolean isSinglePattern, Variable errorString) {
        label("or_lhs_end", firstCase ->
                buildPatternEach(firstCase, result, original, deconstructed, value, left, true, isSinglePattern, errorString));
        label("or_rhs_end", secondCase ->
                cond(secondCase, result, tru(), () -> buildPatternEach(testEnd, result, original, deconstructed, value, right, true, isSinglePattern, errorString)));
    }
    protected void buildPatternMatch(Variable result, Operand original, Variable deconstructed, U arg, Operand obj,
                           boolean inAlternation, boolean isSinglePattern, Variable errorString) {
        label("pattern_end", testEnd -> buildPatternEach(testEnd, result, original, deconstructed, obj, arg, inAlternation, isSinglePattern, errorString));
    }

    // FIXME: Remove once all dependence on source in prism is gone.
    protected void hackPostExeSource(IRBuilder builder) {
    }

    protected Operand buildPostExe(U body, int line) {
        IRScope topLevel = scope.getRootLexicalScope();
        IRScope nearestLVarScope = scope.getNearestTopLocalVariableScope();
        StaticScope parentScope = nearestLVarScope.getStaticScope();
        StaticScope staticScope = parentScope.duplicate();
        staticScope.setEnclosingScope(parentScope);
        IRClosure endClosure = new IRClosure(getManager(), scope, line, staticScope, Signature.NO_ARGUMENTS,
                CommonByteLists._END_, true);
        staticScope.setIRScope(endClosure);
        endClosure.setIsEND();

        // Create a new nested builder to ensure this gets its own IR builder state like the ensure block stack
        IRBuilder builder = getManager().getBuilderFactory().newIRBuilder(getManager(), endClosure, null, encoding);

        builder.hackPostExeSource(this);

        builder.buildPrePostExeInner(body);

        // Add an instruction in 's' to record the end block in the 'topLevel' scope.
        // SSS FIXME: IR support for end-blocks that access vars in non-toplevel-scopes
        // might be broken currently. We could either fix it or consider dropping support
        // for END blocks altogether or only support them in the toplevel. Not worth the pain.
        addInstr(new RecordEndBlockInstr(topLevel, new WrappedIRClosure(buildSelf(), endClosure)));
        return nil();
    }

    private void buildPrePostExeInner(U body) {
        build(body);

        // END does not have either explicit or implicit return, so we add one
        addInstr(new ReturnInstr(nil()));

        prependUsedImplicitState(null);

        computeScopeFlagsFrom(instructions);
        scope.allocateInterpreterContext(instructions, temporaryVariableIndex + 1, flags);
    }

    protected Operand buildPreExe(U body) {
        List<Instr> beginInstrs = getManager().getBuilderFactory().newIRBuilder(getManager(), scope, this, this, encoding).buildPreExeInner(body);

        instructions.addAll(afterPrologueIndex, beginInstrs);
        afterPrologueIndex += beginInstrs.size();

        return nil();
    }

    private List<Instr> buildPreExeInner(U body) {
        build(body);

        return instructions;
    }

    protected Operand buildRange(U beginNode, U endNode, boolean isExclusive) {
        Operand begin;
        Operand end;

        // string ranges should have frozen begin and end
        if (beginNode instanceof StrNode beginString) {
            begin = new FrozenString(beginString.getValue(), beginString.getCodeRange(), beginString.getFile(), beginString.getLine());
        } else {
            begin = build(beginNode);
        }

        if (endNode instanceof StrNode endString) {
            end = new FrozenString(endString.getValue(), endString.getCodeRange(), endString.getFile(), endString.getLine());
        } else {
            end = build(endNode);
        }

        if (begin instanceof ImmutableLiteral && end instanceof ImmutableLiteral) {
            // endpoints are free of side effects, cache the range after creation
            return new Range((ImmutableLiteral) begin, (ImmutableLiteral) end, isExclusive);
        }

        // must be built every time
        return addResultInstr(new BuildRangeInstr(temp(), begin, end, isExclusive));
    }

    protected Operand buildRational(U numerator, U denominator) {
        return new Rational((ImmutableLiteral) build(numerator), (ImmutableLiteral) build(denominator));
    }

    protected Operand buildRedo(int line) {
        // If we have ensure blocks, have to run those first!
        if (!activeEnsureBlockStack.isEmpty()) {
            emitEnsureBlocks(getCurrentLoop());
        }

        // If in a loop, a redo is a jump to the beginning of the loop.
        // If not, for closures, a redo is a jump to the beginning of the closure.
        // If not in a loop or a closure, it is a compile/syntax error
        IRLoop currLoop = getCurrentLoop();
        if (currLoop != null) {
            addInstr(new JumpInstr(currLoop.iterStartLabel));
        } else {
            if (scope instanceof IRClosure) {
                if (scope instanceof IREvalScript) {
                    throwSyntaxError(line, "Can't escape from eval with redo");
                } else {
                    addInstr(new ThreadPollInstr(true));
                    Label startLabel = new Label(scope.getId() + "_START", 0);
                    instructions.add(afterPrologueIndex, new LabelInstr(startLabel));
                    addInstr(new JumpInstr(startLabel));
                }
            } else {
                throwSyntaxError(line, "Invalid redo");
            }
        }
        return nil();
    }

    protected void buildRescueBodyInternal(U[] exceptions, U body, X consequent, Variable rv, Variable exc, Label endLabel,
                                 U reference) {
        // Compare and branch as necessary!
        Label uncaughtLabel = getNewLabel("MISSED");
        Label caughtLabel = getNewLabel("RESCUE");
        if (exceptions == null || exceptions.length == 0) {
            outputExceptionCheck(getManager().getStandardError(), exc, caughtLabel);
        } else {
            for (U exception: exceptions) {
                outputExceptionCheck(build(exception), exc, caughtLabel);
            }
        }

        // Uncaught exception -- build other rescue nodes or rethrow!
        addInstr(new LabelInstr(uncaughtLabel));
        if (consequent != null) {
            buildRescueBodyInternal(exceptionNodesFor(consequent), bodyFor(consequent), optRescueFor(consequent), rv,
                    exc, endLabel, referenceFor(consequent));
        } else {
            addInstr(new ThrowExceptionInstr(exc));
        }

        // Caught exception case -- build rescue body
        addInstr(new LabelInstr(caughtLabel));
        if (reference != null) {
            Variable exception = addResultInstr(new GetGlobalVariableInstr(temp(), symbol("$!")));

            buildAssignment(reference, exception);  // Prism does not desugar
        }
        
        if (RubyInstanceConfig.FULL_TRACE_ENABLED) {
            // Explicit line number here because we need a line number for trace before we process any nodes
            addInstr(getManager().newLineNumber(scope.getLine() + 1));
            addInstr(new TraceInstr(RubyEvent.RESCUE, getCurrentModuleVariable(), getName(), getFileName(), scope.getLine() + 1));
        }

        Operand x = build(body);
        if (x != U_NIL) { // can be U_NIL if the rescue block has an explicit return
            // Set up node return value 'rv'
            addInstr(new CopyInstr(rv, x));

            // Clone the topmost ensure block (which will be a wrapper
            // around the current rescue block)
            if (activeEnsureBlockStack.peek() != null) activeEnsureBlockStack.peek().cloneIntoHostScope(this);

            addInstr(new JumpInstr(endLabel));
        }
    }

    protected abstract void buildAssignment(U reference, Operand rhs);

    protected Operand buildAttrAssign(Variable result, U receiver, U argsNode, U blockNode, RubySymbol name,
                            boolean isLazy, boolean containsAssignment) {
        Operand obj = buildWithOrder(receiver, containsAssignment);

        Label lazyLabel = null;
        Label endLabel = null;
        if (result == null) result = temp();
        if (isLazy) {
            lazyLabel = getNewLabel();
            endLabel = getNewLabel();
            addInstr(new BNilInstr(lazyLabel, obj));
        }

        int[] flags = new int[1];
        Operand[] rhs = new Operand[1];
        Operand[] args = buildAttrAssignCallArgs(argsNode, rhs, containsAssignment);
        Operand block = setupCallClosure(argsNode, blockNode);
        addInstr(AttrAssignInstr.create(scope, obj, name, args, block, flags[0], scope.maybeUsingRefinements()));
        addInstr(new CopyInstr(result, rhs[0]));

        if (isLazy) {
            addInstr(new JumpInstr(endLabel));
            addInstr(new LabelInstr(lazyLabel));
            addInstr(new CopyInstr(result, nil()));
            addInstr(new LabelInstr(endLabel));
        }

        return result;
    }

    protected abstract Operand[] buildAttrAssignCallArgs(U args, Operand[] rhs, boolean containsAssignment);

    protected Operand buildRescueInternal(U bodyNode, U elseNode, U[] exceptions, U rescueBody,
                                        X optRescue, boolean isModifier, EnsureBlockInfo ensure, U reference) {
        boolean needsBacktrace = !canBacktraceBeRemoved(exceptions, rescueBody, optRescue, elseNode, isModifier);

        // Labels marking start, else, end of the begin-rescue(-ensure)-end block
        Label rBeginLabel = getNewLabel();
        Label rEndLabel   = ensure.end;
        Label rescueLabel = getNewLabel("RESC_TEST"); // Label marking start of the first rescue code.
        ensure.needsBacktrace = needsBacktrace;

        addInstr(new LabelInstr(rBeginLabel));

        // Placeholder rescue instruction that tells rest of the compiler passes the boundaries of the rescue block.
        addInstr(new ExceptionRegionStartMarkerInstr(rescueLabel));
        activeRescuers.push(rescueLabel);
        addInstr(getManager().needsBacktrace(needsBacktrace));

        // Body
        Operand tmp = nil();  // default return value if for some strange reason, we neither have the body node or the else node!
        Variable rv = temp();
        if (bodyNode != null) tmp = build(bodyNode);

        // Since rescued regions are well nested within Ruby, this bare marker is sufficient to
        // let us discover the edge of the region during linear traversal of instructions during cfg construction.
        addInstr(new ExceptionRegionEndMarkerInstr());
        activeRescuers.pop();

        // Else part of the body -- we simply fall through from the main body if there were no exceptions
        if (elseNode != null) {
            addInstr(new LabelInstr(getNewLabel()));
            tmp = build(elseNode);
        }

        // Push rescue block *after* body has been built.
        // If not, this messes up generation of retry in these scenarios like this:
        //
        //     begin    -- 1
        //       ...
        //     rescue
        //       begin  -- 2
        //         ...
        //         retry
        //       rescue
        //         ...
        //       end
        //     end
        //
        // The retry should jump to 1, not 2.
        // If we push the rescue block before building the body, we will jump to 2.
        RescueBlockInfo rbi = new RescueBlockInfo(rBeginLabel, ensure.savedGlobalException);
        activeRescueBlockStack.push(rbi);

        if (tmp != U_NIL) {
            addInstr(new CopyInstr(rv, tmp));

            // No explicit return from the protected body
            // - If we dont have any ensure blocks, simply jump to the end of the rescue block
            // - If we do, execute the ensure code.
            ensure.cloneIntoHostScope(this);
            addInstr(new JumpInstr(rEndLabel));
        }   //else {
        // If the body had an explicit return, the return instruction IR build takes care of setting
        // up execution of all necessary ensure blocks. So, nothing to do here!
        //
        // Additionally, the value in 'rv' will never be used, so no need to set it to any specific value.
        // So, we can leave it undefined. If on the other hand, there was an exception in that block,
        // 'rv' will get set in the rescue handler -- see the 'rv' being passed into
        // buildRescueBodyInternal below. So, in either case, we are good!
        //}

        // Start of rescue logic
        addInstr(new LabelInstr(rescueLabel));

        // This is optimized no backtrace path so we need to reenable backtraces since we are
        // exiting that region.
        if (!needsBacktrace) addInstr(getManager().needsBacktrace(true));

        // Save off exception & exception comparison type
        Variable exc = addResultInstr(new ReceiveRubyExceptionInstr(temp()));

        // Build the actual rescue block(s)
        buildRescueBodyInternal(exceptions, rescueBody, optRescue, rv, exc, rEndLabel, reference);

        activeRescueBlockStack.pop();
        return rv;
    }

    protected Operand buildRetry(int line) {
        // JRuby only supports retry when present in rescue blocks!
        // 1.9 doesn't support retry anywhere else.

        // SSS FIXME: We should be able to use activeEnsureBlockStack for this
        // But, see the code in buildRescueInternal that pushes/pops these and
        // the documentation for retries.  There is a small ordering issue
        // which is preventing me from getting rid of activeRescueBlockStack
        // altogether!
        //
        // Jump back to the innermost rescue block
        // We either find it, or we add code to throw a runtime exception
        if (activeRescueBlockStack.isEmpty()) {
            throwSyntaxError(line, "Invalid retry");
        } else {
            addInstr(new ThreadPollInstr(true));
            // Restore $! and jump back to the entry of the rescue block
            RescueBlockInfo rbi = activeRescueBlockStack.peek();
            addInstr(new PutGlobalVarInstr(symbol("$!"), rbi.savedExceptionVariable));
            addInstr(new JumpInstr(rbi.entryLabel));
            // Retries effectively create a loop
            scope.setHasLoops();
        }
        return nil();
    }

    protected Operand buildReturn(Operand value, int line) {
        Operand retVal = value;

        if (scope instanceof IRClosure) {
            if (scope.isWithinEND()) {
                // ENDs do not allow returns
                addInstr(new ThrowExceptionInstr(IRException.RETURN_LocalJumpError));
            } else {
                // Closures return behavior has several cases (which depend on runtime state):
                // 1. closure in method (return). !method (error) except if in define_method (return)
                // 2. lambda (return) [dynamic]  // FIXME: I believe ->() can be static and omit LJE check.
                // 3. migrated closure (LJE) [dynamic]
                // 4. eval/for (return) [static]
                boolean definedWithinMethod = scope.getNearestMethod() != null;
                if (!(scope instanceof IREvalScript) && !(scope instanceof IRFor)) {
                    addInstr(new CheckForLJEInstr(definedWithinMethod));
                }
                // for non-local returns (from rescue block) we need to restore $! so it does not get carried over
                if (!activeRescueBlockStack.isEmpty()) {
                    RescueBlockInfo rbi = activeRescueBlockStack.peek();
                    addInstr(new PutGlobalVarInstr(symbol("$!"), rbi.savedExceptionVariable));
                }

                addInstr(new NonlocalReturnInstr(retVal, definedWithinMethod ? scope.getNearestMethod().getId() : "--none--"));
            }
        } else if (scope.isModuleBody()) {
            IRMethod sm = scope.getNearestMethod();

            // Cannot return from top-level module bodies!
            if (sm == null) addInstr(new ThrowExceptionInstr(IRException.RETURN_LocalJumpError));
            if (sm != null) addInstr(new NonlocalReturnInstr(retVal, sm.getId()));
        } else {
            retVal = processEnsureRescueBlocks(retVal);

            if (RubyInstanceConfig.FULL_TRACE_ENABLED) {
                addInstr(new TraceInstr(RubyEvent.RETURN, getCurrentModuleVariable(), getName(), getFileName(), line + 1));
            }
            addInstr(new ReturnInstr(retVal));
        }

        // The value of the return itself in the containing expression can never be used because of control-flow reasons.
        // The expression that uses this result can never be executed beyond the return and hence the value itself is just
        // a placeholder operand.
        return U_NIL;
    }

    protected Operand buildSClass(U receiverNode, U bodyNode, StaticScope scope, int line, int endLine) {
        Operand receiver = build(receiverNode);
        IRModuleBody body = new IRMetaClassBody(getManager(), this.scope, getManager().getMetaClassName().getBytes(), line, scope);
        Variable sClassVar = addResultInstr(new DefineMetaClassInstr(temp(), receiver, body));

        // sclass bodies inherit the block of their containing method
        Variable bodyResult = addResultInstr(new ProcessModuleBodyInstr(temp(), sClassVar));
        getManager().getBuilderFactory().newIRBuilder(getManager(), body, this, encoding).buildModuleOrClassBody(bodyNode, line, endLine);
        return bodyResult;
    }

    protected Variable buildSelf() {
        selfUsed = true;
        return scope.getSelf();
    }

    protected Operand buildSuper(Variable aResult, U iterNode, U argsNode, int line, boolean isNewline) {
        Variable result = aResult == null ? temp() : aResult;
        Operand tempBlock = setupCallClosure(argsNode, iterNode);
        if (tempBlock == NullBlock.INSTANCE) tempBlock = getYieldClosureVariable();
        Operand block = tempBlock;

        boolean inClassBody = scope instanceof IRMethod && scope.getLexicalParent() instanceof IRClassBody;
        boolean isInstanceMethod = inClassBody && ((IRMethod) scope).isInstanceMethod;
        int[] flags = new int[] { 0 };
        Operand[] args = setupCallArgs(argsNode, flags);

        determineIfWeNeedLineNumber(line, isNewline, false, false); // backtrace needs line of call in case of exception.
        if ((flags[0] & CALL_KEYWORD_REST) != 0) {  // {**k}, {**{}, **k}, etc...
            Variable test = addResultInstr(new RuntimeHelperCall(temp(), IS_HASH_EMPTY, new Operand[] { args[args.length - 1] }));
            if_else(test, tru(),
                    () -> receiveBreakException(block,
                            determineSuperInstr(result, removeArg(args), block, flags[0], inClassBody, isInstanceMethod)),
                    () -> receiveBreakException(block,
                            determineSuperInstr(result, args, block, flags[0], inClassBody, isInstanceMethod)));
        } else {
            receiveBreakException(block,
                    determineSuperInstr(result, args, block, flags[0], inClassBody, isInstanceMethod));
        }

        return result;
    }

    protected Operand buildUndef(Operand name) {
        return addResultInstr(new UndefMethodInstr(temp(), name));
    }

    public Operand buildVAlias(RubySymbol left, RubySymbol right) {
        addInstr(new GVarAliasInstr(new MutableString(left), new MutableString(right)));

        return nil();
    }

    // FIXME: This needs to pass node/line of first reference of literals have been shown.
    protected abstract void buildWhenArgs(W whenNode, Operand testValue, Label bodyLabel,
                                          Set<IRubyObject> seenLiterals,
                                          Map<IRubyObject, java.lang.Integer> origLocs);

    protected void buildWhenValue(Variable eqqResult, Operand testValue, Label bodyLabel, U node,
                                  Set<IRubyObject> seenLiterals, Map<IRubyObject, java.lang.Integer> origLocs,
                                  boolean needsSplat) {
        if (literalWhenCheck(node, seenLiterals, origLocs)) { // we only emit first literal of the same value.
            Operand expression;
            if (isLiteralString(node)) {  // compile literal string whens as fstrings
                expression = frozen_string(node);
            } else {
                expression = buildWithOrder(node, containsVariableAssignment(node));
            }

            addInstr(new EQQInstr(scope, eqqResult, expression, testValue, needsSplat, false, scope.maybeUsingRefinements()));
            addInstr(createBranch(eqqResult, tru(), bodyLabel));
        }
    }

    protected void buildWhenValues(Variable eqqResult, U[] exprValues, Operand testValue, Label bodyLabel,
                         Set<IRubyObject> seenLiterals, Map<IRubyObject, java.lang.Integer> origLocs) {
        for (U value: exprValues) {
            buildWhenValue(eqqResult, testValue, bodyLabel, value, seenLiterals, origLocs, false);
        }
    }


    protected abstract Operand[] buildCallArgs(U args, int[] flags);
    protected abstract Operand buildGetDefinition(U node);
    protected abstract boolean containsVariableAssignment(U node);
    protected abstract Operand frozen_string(U node);
    protected abstract int getLine(U node);
    protected abstract IRubyObject getWhenLiteral(U node);
    protected abstract boolean isLiteralString(U node);
    protected abstract boolean needsDefinitionCheck(U node);

    protected abstract void receiveForArgs(U node);
    protected abstract void receiveBlockArgs(U node);
    protected abstract Operand setupCallClosure(U args, U iter);

    // returns true if we should emit an eqq for this value (e.g. it has not already been seen yet).
    protected boolean literalWhenCheck(U value, Set<IRubyObject> seenLiterals, Map<IRubyObject, java.lang.Integer> origLocs) {
        IRubyObject literal = getWhenLiteral(value);

        if (literal != null) {
            if (seenLiterals.contains(literal)) {
                getManager().getRuntime().getWarnings().warning(IRubyWarnings.ID.MISCELLANEOUS, getFileName(), getLine(value),
                        "duplicated 'when' clause with line " + (origLocs.get(literal) + 1) + " is ignored");
                return false;
            } else {
                seenLiterals.add(literal);
                origLocs.put(literal, getLine(value));
                return true;
            }
        }

        return true;
    }

    protected Operand buildZSuper(Variable result, U iter) {
        Operand block = setupCallClosure(null, iter);
        if (block == NullBlock.INSTANCE) block = getYieldClosureVariable();

        return scope instanceof IRMethod ? buildZSuper(result, block) : buildZSuperIfNest(result, block);
    }

    protected Operand buildZSuper(Variable result, Operand block) {
        List<Operand> callArgs = new ArrayList<>(5);
        List<KeyValuePair<Operand, Operand>> keywordArgs = new ArrayList<>(3);
        determineZSuperCallArgs(scope, this, callArgs, keywordArgs);

        boolean inClassBody = scope instanceof IRMethod && scope.getLexicalParent() instanceof IRClassBody;
        boolean isInstanceMethod = inClassBody && ((IRMethod) scope).isInstanceMethod;
        Variable zsuperResult = result == null ? temp() : result;
        int[] flags = new int[] { 0 };
        if (keywordArgs.size() == 1 && keywordArgs.get(0).getKey().equals(Symbol.KW_REST_ARG_DUMMY)) {
            flags[0] |= (CALL_KEYWORD | CALL_KEYWORD_REST);
            Operand keywordRest = keywordArgs.get(0).getValue();
            Operand[] args = callArgs.toArray(new Operand[0]);
            Variable test = addResultInstr(new RuntimeHelperCall(temp(), IS_HASH_EMPTY, new Operand[] { keywordRest }));
            if_else(test, tru(),
                    () -> receiveBreakException(block,
                            determineSuperInstr(zsuperResult, args, block, flags[0], inClassBody, isInstanceMethod)),
                    () -> receiveBreakException(block,
                            determineSuperInstr(zsuperResult, addArg(args, keywordRest), block, flags[0], inClassBody, isInstanceMethod)));
        } else {
            Operand[] args = getZSuperCallOperands(scope, callArgs, keywordArgs, flags);
            receiveBreakException(block,
                    determineSuperInstr(zsuperResult, args, block, flags[0], inClassBody, isInstanceMethod));
        }

        return zsuperResult;
    }

    protected Operand buildZSuperIfNest(Variable result, final Operand block) {
        int depthFrom = 0;
        IRBuilder<U,V,W,X,Y,Z> superBuilder = this;
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
                Operand[] args = adjustVariableDepth(callArgs.toArray(new Operand[0]), depthFromSuper);
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

    protected abstract boolean alwaysFalse(U node);
    protected abstract boolean alwaysTrue(U node);
    protected abstract Operand build(Variable result, U node);
    protected abstract Operand build(U node);
    protected abstract int dynamicPiece(Operand[] pieces, int index, U piece, Encoding encoding);
    protected abstract void receiveMethodArgs(V defNode);

    protected IRMethod defineNewMethod(LazyMethodDefinition<U, V, W, X, Y, Z> defn, ByteList name, int line, StaticScope scope, boolean isInstanceMethod) {
        IRMethod method = new IRMethod(getManager(), this.scope, defn, name, isInstanceMethod, line, scope, coverageMode);

        // poorly placed next/break expects a syntax error so we eagerly build methods which contain them.
        if (!canBeLazyMethod(defn.getMethod())) method.lazilyAcquireInterpreterContext();

        return method;
    }


    public void defineMethodInner(LazyMethodDefinition<U, V, W, X, Y, Z> defNode, IRScope parent, int coverageMode) {
        long time = 0;
        if (parserTiming) time = System.nanoTime();
        this.coverageMode = coverageMode;

        if (RubyInstanceConfig.FULL_TRACE_ENABLED) {
            // Explicit line number here because we need a line number for trace before we process any nodes
            addInstr(getManager().newLineNumber(scope.getLine() + 1));
            addInstr(new TraceInstr(RubyEvent.CALL, getCurrentModuleVariable(), getName(), getFileName(), scope.getLine() + 1));
        }

        receiveMethodArgs(defNode.getMethod());

        Operand rv = build(defNode.getMethodBody());

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

        prependUsedImplicitState(parent);

        computeScopeFlagsFrom(instructions);

        scope.allocateInterpreterContext(instructions, temporaryVariableIndex + 1, flags);

        if (parserTiming) manager.getRuntime().getParserManager().getParserStats().addIRBuildTime(System.nanoTime() - time);
    }

    private void prependUsedImplicitState(IRScope parent) {
        int numberOfInstrs = 0;
        if (needsYieldBlock) {
            if (scope instanceof IRMethod) {
                numberOfInstrs++;
                addInstrAtBeginning(new LoadImplicitClosureInstr(getYieldClosureVariable()));
            } else if (!(scope instanceof IRModuleBody)) { // Class MetaClass are IRModuleBody as well.
                numberOfInstrs++;
                addInstrAtBeginning(new LoadFrameClosureInstr(getYieldClosureVariable()));
            }
        }
        if (currentModuleUsed) {
            // These instructions need to be toward the top of the method because they may both be needed for processing
            // optional arguments as in def foo(a = Object).
            // Set %current_module = isInstanceMethod ? %self.metaclass : %self
            if (scope instanceof IRMethod && parent != null) {
                numberOfInstrs++;
                int nearestScopeDepth = parent.getNearestModuleReferencingScopeDepth();
                addInstrAtBeginning(new CopyInstr(getCurrentModuleVariable(), ScopeModule.ModuleFor(nearestScopeDepth == -1 ? 1 : nearestScopeDepth)));
            } else {
                numberOfInstrs++;
                addInstrAtBeginning(new CopyInstr(getCurrentModuleVariable(), SCOPE_MODULE[0])); // %current_module
            }
        }
        if (selfUsed) {
            numberOfInstrs++;
            addInstrAtBeginning(manager.getReceiveSelfInstr());
        }

        if (numberOfInstrs > 0) afterPrologueIndex += numberOfInstrs;
    }

    private void prependUsedClosureImplicitState(boolean forLoop) {
        int numberOfInstrs = 0;
        if (needsYieldBlock) {
            numberOfInstrs++;
            addInstrAtBeginning(new LoadBlockImplicitClosureInstr(getYieldClosureVariable()));
        }
        // for loops refer to previous module scope and not its own so we do not make one.
        if (!forLoop && currentModuleUsed) {
            numberOfInstrs++;
            addInstrAtBeginning(new CopyInstr(getCurrentModuleVariable(), SCOPE_MODULE[0]));
        }
        if (selfUsed) {
            numberOfInstrs++;
            addInstrAtBeginning(manager.getReceiveSelfInstr());
        }

        if (numberOfInstrs > 0) afterPrologueIndex += numberOfInstrs;
    }

    protected ArgumentDescriptor[] createArgumentDescriptor() {
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
    protected Variable argumentResult(RubySymbol name) {
        // |a,| case
        if (name == null) return temp();

        boolean isUnderscore = name.getBytes().realSize() == 1 && name.getBytes().charAt(0) == '_';

        if (isUnderscore && underscoreVariableSeen) {
            return temp();
        } else {
            if (isUnderscore) underscoreVariableSeen = true;
            return getNewLocalVariable(name, 0);
        }
    }

    // If we see define_method as a call we can potentially convert the closure into a method to
    // avoid the costs associated with executing blocks.
    private void checkForOptimizableDefineMethod(RubySymbol name, U iter, Operand block) {
        // We will stuff away the iters AST source into the closure in the hope we can convert
        // this closure to a method.
        if (CommonByteLists.DEFINE_METHOD_METHOD.equals(name.getBytes()) && block instanceof WrappedIRClosure) {
            IRClosure closure = ((WrappedIRClosure) block).getClosure();

            // FIXME: Prism will never do this because it will never be old IterNode.
            // FIXME: Prism support define_method optimization
            // To convert to a method we need its variable scoping to appear like a normal method.
            if (!closure.accessesParentsLocalVariables() && iter instanceof IterNode) {
                closure.setSource((IterNode) iter);
            }
        }
    }

    protected Variable createCall(Variable result, Operand receiver, CallType callType, RubySymbol name, U argsNode,
                                U iter, int line, boolean isNewline) {
        int[] flags = new int[] { 0 };
        Operand[] args = setupCallArgs(argsNode, flags);
        // check for refinement calls before building any closure
        if (callType == FUNCTIONAL) determineIfMaybeRefined(name, args);
        Operand block = setupCallClosure(argsNode, iter);
        determineIfWeNeedLineNumber(line, isNewline, false, false); // backtrace needs line of call in case of exception.
        if ((flags[0] & CALL_KEYWORD_REST) != 0) {  // {**k}, {**{}, **k}, etc...
            Variable test = addResultInstr(new RuntimeHelperCall(temp(), IS_HASH_EMPTY, new Operand[] { args[args.length - 1] }));
            if_else(test, tru(),
                    () -> receiveBreakException(block,
                            CallInstr.create(scope, callType, result, name, receiver, removeArg(args), block, flags[0])),
                    () -> receiveBreakException(block,
                            CallInstr.create(scope, callType, result, name, receiver, args, block, flags[0])));
        } else {
            if (callType == FUNCTIONAL) checkForOptimizableDefineMethod(name, iter, block);

            receiveBreakException(block,
                    CallInstr.create(scope, callType, result, name, receiver, args, block, flags[0]));
        }

        return result;
    }

    protected void determineIfWeNeedLineNumber(int line, boolean isNewline, boolean implicitNil, boolean def) {
        if (line != lastProcessedLineNum && !implicitNil) {
            LineInfo needsCoverage = isNewline ? LineInfo.Coverage : null;
            // DefNode will set it's own line number as part of impl but if it is for coverage we emit as instr also.
            if (needsCoverage != null && (!def || coverageMode != 0)) { // Do not emit multiple line number instrs for the same line
                needsLineNumInfo = needsCoverage;
            }

            // This line is already process either by linenum or by instr which emits its own.
            lastProcessedLineNum = line;
        }
    }

    // FIXME: This needs to be called on super/zsuper too
    protected void determineIfMaybeRefined(RubySymbol methodName, Operand[] args) {
        IRScope outerScope = scope.getNearestTopLocalVariableScope();

        // 'using single_mod_arg' possible nearly everywhere but method scopes.
        boolean refinement = false;
        if (!(outerScope instanceof IRMethod)) {
            ByteList methodBytes = methodName.getBytes();
            if (args.length == 1) {
                refinement = isRefinementCall(methodBytes);
            } else if (args.length == 2 && CommonByteLists.SEND.equal(methodBytes)) {
                if (args[0] instanceof Symbol sendName) {
                    refinement = isRefinementCall(sendName.getBytes());
                }
            }
        }

        if (refinement) scope.setIsMaybeUsingRefinements();
    }

    protected CallInstr determineSuperInstr(Variable result, Operand[] args, Operand block, int flags,
                                          boolean inClassBody, boolean isInstanceMethod) {
        if (result == null) result = temp();
        return inClassBody ?
                isInstanceMethod ?
                        new InstanceSuperInstr(scope, result, getCurrentModuleVariable(), getName(), args, block, flags, scope.maybeUsingRefinements()) :
                        new ClassSuperInstr(scope, result, getCurrentModuleVariable(), getName(), args, block, flags, scope.maybeUsingRefinements()) :
                // We dont always know the method name we are going to be invoking if the super occurs in a closure.
                // This is because the super can be part of a block that will be used by 'define_method' to define
                // a new method.  In that case, the method called by super will be determined by the 'name' argument
                // to 'define_method'.
                new UnresolvedSuperInstr(scope, result, buildSelf(), args, block, flags, scope.maybeUsingRefinements());
    }

    protected Operand findContainerModule() {
        int nearestModuleBodyDepth = scope.getNearestModuleReferencingScopeDepth();
        return (nearestModuleBodyDepth == -1) ? getCurrentModuleVariable() : ScopeModule.ModuleFor(nearestModuleBodyDepth);
    }

    protected Variable as_fixnum(Operand value) {
        return addResultInstr(new AsFixnumInstr(temp(), value));
    }


    public abstract LocalVariable getLocalVariable(RubySymbol name, int scopeDepth);

    public LocalVariable getNewLocalVariable(RubySymbol name, int scopeDepth) {
        return scope.getNewLocalVariable(name, scopeDepth);
    }

    public IRManager getManager() {
        return manager;
    }

    private static boolean isRefinementCall(ByteList methodBytes) {
        return CommonByteLists.USING_METHOD.equals(methodBytes)
                // FIXME: This sets the bit for the whole module, but really only the refine block needs it
                || CommonByteLists.REFINE_METHOD.equals(methodBytes);
    }

    protected Operand processEnsureRescueBlocks(Operand retVal) {
        // Before we return,
        // - have to go execute all the ensure blocks if there are any.
        //   this code also takes care of resetting "$!"
        if (!activeEnsureBlockStack.isEmpty()) {
            retVal = addResultInstr(new CopyInstr(temp(), retVal));
            emitEnsureBlocks(null);
        }
        return retVal;
    }

    protected void throwSyntaxError(int line , String message) {
        String errorMessage = getFileName() + ":" + (line + 1) + ": " + message;
        throw scope.getManager().getRuntime().newSyntaxError(errorMessage, getFileName());
    }

    protected BinaryType binaryType(U node) {
        return alwaysTrue(node) ? BinaryType.LeftTrue :
                alwaysFalse(node) ? BinaryType.LeftFalse : BinaryType.Normal;
    }

    protected interface CodeBlock {
        Operand run();
    }

    protected interface Consume2<T, U> {
        void apply(T t, U u);
    }

    protected interface RunIt {
        void apply();
    }

    protected interface VoidCodeBlock {
        void run();
    }

    protected interface VoidCodeBlockOne {
        void run(Operand arg);
    }

    public enum BinaryType {
        Normal,    // Left is unknown expression
        LeftTrue,  // Statically true
        LeftFalse  // Statically false
    }

    // FIXME: Currently only valid value in Prism.
    protected Encoding getEncoding() {
        return encoding;
    }

    public void initFlipStateVariable(Variable v, Operand initState) {
        addInstrAtBeginning(new CopyInstr(v, initState));
    }

    private IRBuilder<U, V, W, X, Y, Z> getNearestFlipVariableScopeBuilder() {
        IRBuilder<U, V, W, X, Y, Z> current = this;

        while (current != null && !current.scope.isWhereFlipFlopStateVariableIs()) {
            current = current.parent;
        }

        return current;
    }
}
