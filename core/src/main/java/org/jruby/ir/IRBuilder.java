package org.jruby.ir;

import org.jruby.Ruby;
import org.jruby.RubyBignum;
import org.jruby.RubyComplex;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyRational;
import org.jruby.RubySymbol;
import org.jruby.ast.*;
import org.jruby.ast.types.INameNode;
import org.jruby.common.IRubyWarnings;
import org.jruby.compiler.NotCompilableException;
import org.jruby.ext.coverage.CoverageData;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ArgumentDescriptor;
import org.jruby.runtime.ArgumentType;
import org.jruby.ir.instructions.*;
import org.jruby.ir.instructions.defined.GetErrorInfoInstr;
import org.jruby.ir.instructions.defined.RestoreErrorInfoInstr;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.operands.*;
import org.jruby.ir.operands.Boolean;
import org.jruby.ir.operands.Float;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;
import org.jruby.runtime.CallType;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.Signature;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.CommonByteLists;
import org.jruby.util.DefinedMessage;
import org.jruby.util.KeyValuePair;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import static org.jruby.ir.IRFlags.*;
import static org.jruby.ir.instructions.Instr.EMPTY_OPERANDS;
import static org.jruby.ir.instructions.RuntimeHelperCall.Methods.*;

import static org.jruby.ir.operands.ScopeModule.*;
import static org.jruby.runtime.CallType.FUNCTIONAL;
import static org.jruby.runtime.CallType.NORMAL;

// This class converts an AST into a bunch of IR instructions

// IR Building Notes
// -----------------
//
// 1. More copy instructions added than necessary
// ----------------------------------------------
// Note that in general, there will be lots of a = b kind of copies
// introduced in the IR because the translation is entirely single-node focused.
// An example will make this clear.
//
// RUBY:
//     v = @f
// will translate to
//
// AST:
//     LocalAsgnNode v
//       InstrVarNode f
// will translate to
//
// IR:
//     tmp = self.f [ GET_FIELD(tmp,self,f) ]
//     v = tmp      [ COPY(v, tmp) ]
//
// instead of
//     v = self.f   [ GET_FIELD(v, self, f) ]
//
// We could get smarter and pass in the variable into which this expression is going to get evaluated
// and use that to store the value of the expression (or not build the expression if the variable is null).
//
// But, that makes the code more complicated, and in any case, all this will get fixed in a single pass of
// copy propagation and dead-code elimination.
//
// Something to pay attention to and if this extra pass becomes a concern (not convinced that it is yet),
// this smart can be built in here.  Right now, the goal is to do something simple and straightforward that is going to be correct.
//
// 2. Returning null vs manager.getNil()
// ----------------------------
// - We should be returning null from the build methods where it is a normal "error" condition
// - We should be returning manager.getNil() where the actual return value of a build is the ruby nil operand
//   Look in buildIf for an example of this.
//
// 3. Temporary variable reuse
// ---------------------------
// I am reusing variables a lot in places in this code.  Should I instead always get a new variable when I need it
// This introduces artificial data dependencies, but fewer variables.  But, if we are going to implement SSA pass
// this is not a big deal.  Think this through!

public class IRBuilder {
    static final UnexecutableNil U_NIL = UnexecutableNil.U_NIL;

    public static Node buildAST(boolean isCommandLineScript, String arg) {
        Ruby ruby = Ruby.getGlobalRuntime();

        // inline script
        if (isCommandLineScript) return ruby.parse(ByteList.create(arg), "-e", null, 0, false);

        // from file
        FileInputStream fis = null;
        try {
            File file = new File(arg);
            fis = new FileInputStream(file);
            long size = file.length();
            byte[] bytes = new byte[(int)size];
            fis.read(bytes);
            System.out.println("-- processing " + arg + " --");
            return ruby.parse(new ByteList(bytes), arg, null, 0, false);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            try { if (fis != null) fis.close(); } catch(Exception ignored) { }
        }
    }

    private static class IRLoop {
        public final IRScope  container;
        public final IRLoop   parentLoop;
        public final Label    loopStartLabel;
        public final Label    loopEndLabel;
        public final Label    iterStartLabel;
        public final Label    iterEndLabel;
        public final Variable loopResult;

        public IRLoop(IRScope s, IRLoop outerLoop, Variable result) {
            container = s;
            parentLoop = outerLoop;
            loopStartLabel = s.getNewLabel("_LOOP_BEGIN");
            loopEndLabel   = s.getNewLabel("_LOOP_END");
            iterStartLabel = s.getNewLabel("_ITER_BEGIN");
            iterEndLabel   = s.getNewLabel("_ITER_END");
            loopResult     = result;
            s.setHasLoops();
        }
    }

    private static class RescueBlockInfo {
        final Label      entryLabel;             // Entry of the rescue block
        final Variable   savedExceptionVariable; // Variable that contains the saved $! variable

        public RescueBlockInfo(Label l, Variable v) {
            entryLabel = l;
            savedExceptionVariable = v;
        }
    }

    /* -----------------------------------------------------------------------------------
     * Every ensure block has a start label and end label
     *
     * This ruby code will translate to the IR shown below
     * -----------------
     *   begin
     *       ... protected body ...
     *   ensure
     *       ... ensure block to run
     *   end
     * -----------------
     *  L_region_start
     *     IR instructions for the protected body
     *     .. copy of ensure block IR ..
     *  L_dummy_rescue:
     *     e = recv_exc
     *  L_start:
     *     .. ensure block IR ..
     *     throw e
     *  L_end:
     * -----------------
     *
     * If N is a node in the protected body that might exit this scope (exception rethrows
     * and returns), N has to first run the ensure block before exiting.
     *
     * Since we can have a nesting of ensure blocks, we are maintaining a stack of these
     * well-nested ensure blocks.  Every node N that will exit this scope will have to
     * run the stack of ensure blocks in the right order.
     * ----------------------------------------------------------------------------------- */
    private static class EnsureBlockInfo {
        final Label    regionStart;
        final Label    start;
        final Label    end;
        final Label    dummyRescueBlockLabel;
        Variable savedGlobalException;
        boolean needsBacktrace;

        // Label of block that will rescue exceptions raised by ensure code
        final Label    bodyRescuer;

        // Innermost loop within which this ensure block is nested, if any
        final IRLoop   innermostLoop;

        // AST node for any associated rescue node in the case of begin-rescue-ensure-end block
        // Will be null in the case of begin-ensure-end block
        final RescueNode matchingRescueNode;

        // This ensure block's instructions
        final List<Instr> instrs;

        public EnsureBlockInfo(IRScope s, RescueNode n, IRLoop l, Label bodyRescuer) {
            regionStart = s.getNewLabel();
            start       = s.getNewLabel();
            end         = s.getNewLabel();
            dummyRescueBlockLabel = s.getNewLabel();
            instrs = new ArrayList<>();
            savedGlobalException = null;
            innermostLoop = l;
            matchingRescueNode = n;
            this.bodyRescuer = bodyRescuer;
            needsBacktrace = true;
        }

        public void addInstr(Instr i) {
            instrs.add(i);
        }

        public void addInstrAtBeginning(Instr i) {
            instrs.add(0, i);
        }

        public void emitBody(IRBuilder b) {
            b.addInstr(new LabelInstr(start));
            for (Instr i: instrs) {
                b.addInstr(i);
            }
        }

        public void cloneIntoHostScope(IRBuilder builder) {
            // $! should be restored before the ensure block is run
            if (savedGlobalException != null) {
                // We need make sure on all outgoing paths in optimized short-hand rescues we restore the backtrace
                if (!needsBacktrace) builder.addInstr(builder.manager.needsBacktrace(true));
                builder.addInstr(new PutGlobalVarInstr(builder.symbol("$!"), savedGlobalException));
            }

            // Sometimes we process a rescue and it hits something like non-local flow like a 'next' and
            // there are no actual instrs pushed yet (but ebi has reserved a frame for it -- e.g. the rescue/ensure
            // the next is in).  Since it is doing nothing we have nothing to clone.  By skipping this we prevent
            // setting exception regions and simplify CFG construction.
            if (instrs.size() == 0) return;

            SimpleCloneInfo ii = new SimpleCloneInfo(builder.scope, true);

            // Clone required labels.
            // During normal cloning below, labels not found in the rename map
            // are not cloned.
            ii.renameLabel(start);
            for (Instr i: instrs) {
                if (i instanceof LabelInstr) ii.renameLabel(((LabelInstr)i).getLabel());
            }

            // Clone instructions now
            builder.addInstr(new LabelInstr(ii.getRenamedLabel(start)));
            builder.addInstr(new ExceptionRegionStartMarkerInstr(bodyRescuer));
            for (Instr instr: instrs) {
                Instr clonedInstr = instr.clone(ii);
                if (clonedInstr instanceof CallBase) {
                    CallBase call = (CallBase)clonedInstr;
                    Operand block = call.getClosureArg(null);
                    if (block instanceof WrappedIRClosure) builder.scope.addClosure(((WrappedIRClosure)block).getClosure());
                }
                builder.addInstr(clonedInstr);
            }
            builder.addInstr(new ExceptionRegionEndMarkerInstr());
        }
    }

    // SSS FIXME: Currently only used for retries -- we should be able to eliminate this
    // Stack of nested rescue blocks -- this just tracks the start label of the blocks
    private final Deque<RescueBlockInfo> activeRescueBlockStack = new ArrayDeque<>(4);

    // Stack of ensure blocks that are currently active
    private final Deque<EnsureBlockInfo> activeEnsureBlockStack = new ArrayDeque<>(4);

    // Stack of ensure blocks whose bodies are being constructed
    private final Deque<EnsureBlockInfo> ensureBodyBuildStack   = new ArrayDeque<>(4);

    // Combined stack of active rescue/ensure nestings -- required to properly set up
    // rescuers for ensure block bodies cloned into other regions -- those bodies are
    // rescued by the active rescuers at the point of definition rather than the point
    // of cloning.
    private final Deque<Label> activeRescuers = new ArrayDeque<>(4);

    // Since we are processing ASTs, loop bodies are processed in depth-first manner
    // with outer loops encountered before inner loops, and inner loops finished before outer ones.
    //
    // So, we can keep track of loops in a loop stack which  keeps track of loops as they are encountered.
    // This lets us implement next/redo/break/retry easily for the non-closure cases.
    private final Deque<IRLoop> loopStack = new LinkedList<>();

    private int lastProcessedLineNum = -1;

    // We do not need n consecutive line num instrs but only the last one in the sequence.
    // We set this flag to indicate that we need to emit a line number but have not yet.
    // addInstr will then appropriately add line info when it is called (which will never be
    // called by a linenum instr).
    private boolean needsLineNumInfo = false;

    public boolean underscoreVariableSeen = false;

    private IRLoop getCurrentLoop() {
        return loopStack.peek();
    }

    protected final IRBuilder parent;
    protected IRBuilder variableBuilder;
    protected final IRManager manager;
    protected final IRScope scope;
    protected final List<Instr> instructions;
    protected List<Object> argumentDescriptions;
    protected int coverageMode;
    private boolean executesOnce = true;
    private int temporaryVariableIndex = -1;
    private boolean needsYieldBlock = false;

    // This variable is an out-of-band passing mechanism to pass the method name to the block the
    // method is attached to.  call/fcall will set this and iter building will pass it into the iter
    // builder and set it.
    private RubySymbol methodName = null;

    // Current index to put next BEGIN blocks and other things at the front of this scope.
    // Note: in the case of multiple BEGINs this index slides forward so they maintain proper
    // execution order
    protected int afterPrologueIndex = 0;
    private TemporaryVariable yieldClosureVariable = null;
    private Variable currentModuleVariable = null;

    private EnumSet<IRFlags> flags;

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

    public IRBuilder(IRManager manager, IRScope scope, IRBuilder parent) {
        this(manager, scope, parent, null);
    }

    private boolean needsCodeCoverage() {
        return coverageMode != CoverageData.NONE || parent != null && parent.needsCodeCoverage();
    }

    public void addArgumentDescription(ArgumentType type, RubySymbol name) {
        if (argumentDescriptions == null) argumentDescriptions = new ArrayList<>();

        argumentDescriptions.add(type);
        argumentDescriptions.add(name);
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
                addInstr(new TraceInstr(RubyEvent.LINE, methodNameFor(), getFileName(), lastProcessedLineNum + 1));
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

    // Emit cloned ensure bodies by walking up the ensure block stack.
    // If we have been passed a loop value, only emit bodies that are nested within that loop.
    private void emitEnsureBlocks(IRLoop loop) {
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

    private void determineIfWeNeedLineNumber(Node node) {
        if (node.isNewline()) {
            int currLineNum = node.getLine();
            if (currLineNum != lastProcessedLineNum) { // Do not emit multiple line number instrs for the same line
                needsLineNumInfo = true;
                lastProcessedLineNum = currLineNum;
            }
        }
    }

    private NotCompilableException notCompilable(String message, Node node) {
        int line = node != null ? node.getLine() : scope.getLine();
        String loc = scope.getFile() + ":" + line;
        String what = node != null ? node.getClass().getSimpleName() + " - " + loc : loc;
        return new NotCompilableException(message + " (" + what + ").");
    }

    private Operand buildOperand(Variable result, Node node) throws NotCompilableException {
        determineIfWeNeedLineNumber(node);

        switch (node.getNodeType()) {
            case ALIASNODE: return buildAlias((AliasNode) node);
            case ANDNODE: return buildAnd((AndNode) node);
            case ARGSCATNODE: return buildArgsCat((ArgsCatNode) node);
            case ARGSPUSHNODE: return buildArgsPush((ArgsPushNode) node);
            case ARRAYNODE: return buildArray((ArrayNode) node, false);
            case ATTRASSIGNNODE: return buildAttrAssign(result, (AttrAssignNode) node);
            case BACKREFNODE: return buildBackref(result, (BackRefNode) node);
            case BEGINNODE: return buildBegin((BeginNode) node);
            case BIGNUMNODE: return buildBignum((BignumNode) node);
            case BLOCKNODE: return buildBlock((BlockNode) node);
            case BREAKNODE: return buildBreak((BreakNode) node);
            case CALLNODE: return buildCall(result, (CallNode) node, null, null);
            case CASENODE: return buildCase((CaseNode) node);
            case CLASSNODE: return buildClass((ClassNode) node);
            case CLASSVARNODE: return buildClassVar((ClassVarNode) node);
            case CLASSVARASGNNODE: return buildClassVarAsgn((ClassVarAsgnNode) node);
            case COLON2NODE: return buildColon2((Colon2Node) node);
            case COLON3NODE: return buildColon3((Colon3Node) node);
            case COMPLEXNODE: return buildComplex((ComplexNode) node);
            case CONSTDECLNODE: return buildConstDecl((ConstDeclNode) node);
            case CONSTNODE: return searchConst(((ConstNode) node).getName());
            case DASGNNODE: return buildDAsgn((DAsgnNode) node);
            case DEFINEDNODE: return buildGetDefinition(((DefinedNode) node).getExpressionNode());
            case DEFNNODE: return buildDefn((MethodDefNode) node);
            case DEFSNODE: return buildDefs((DefsNode) node);
            case DOTNODE: return buildDot((DotNode) node);
            case DREGEXPNODE: return buildDRegexp(result, (DRegexpNode) node);
            case DSTRNODE: return buildDStr(result, (DStrNode) node);
            case DSYMBOLNODE: return buildDSymbol(result, (DSymbolNode) node);
            case DVARNODE: return buildDVar((DVarNode) node);
            case DXSTRNODE: return buildDXStr(result, (DXStrNode) node);
            case ENCODINGNODE: return buildEncoding((EncodingNode)node);
            case ENSURENODE: return buildEnsureNode((EnsureNode) node);
            case FALSENODE: return buildFalse();
            case FCALLNODE: return buildFCall(result, (FCallNode) node);
            case FIXNUMNODE: return buildFixnum((FixnumNode) node);
            case FLIPNODE: return buildFlip((FlipNode) node);
            case FLOATNODE: return buildFloat((FloatNode) node);
            case FORNODE: return buildFor((ForNode) node);
            case GLOBALASGNNODE: return buildGlobalAsgn((GlobalAsgnNode) node);
            case GLOBALVARNODE: return buildGlobalVar(result, (GlobalVarNode) node);
            case HASHNODE: return buildHash((HashNode) node);
            case IFNODE: return buildIf(result, (IfNode) node);
            case INSTASGNNODE: return buildInstAsgn((InstAsgnNode) node);
            case INSTVARNODE: return buildInstVar((InstVarNode) node);
            case ITERNODE: return buildIter((IterNode) node);
            case LAMBDANODE: return buildLambda((LambdaNode)node);
            case LITERALNODE: return buildLiteral((LiteralNode) node);
            case LOCALASGNNODE: return buildLocalAsgn((LocalAsgnNode) node);
            case LOCALVARNODE: return buildLocalVar((LocalVarNode) node);
            case MATCH2NODE: return buildMatch2(result, (Match2Node) node);
            case MATCH3NODE: return buildMatch3(result, (Match3Node) node);
            case MATCHNODE: return buildMatch(result, (MatchNode) node);
            case MODULENODE: return buildModule((ModuleNode) node);
            case MULTIPLEASGNNODE: return buildMultipleAsgn19((MultipleAsgnNode) node);
            case NEXTNODE: return buildNext((NextNode) node);
            case NTHREFNODE: return buildNthRef((NthRefNode) node);
            case NILNODE: return buildNil();
            case OPASGNANDNODE: return buildOpAsgnAnd((OpAsgnAndNode) node);
            case OPASGNCONSTDECLNODE: return buildOpAsgnConstDeclNode((OpAsgnConstDeclNode) node);
            case OPASGNNODE: return buildOpAsgn((OpAsgnNode) node);
            case OPASGNORNODE: return buildOpAsgnOr((OpAsgnOrNode) node);
            case OPELEMENTASGNNODE: return buildOpElementAsgn((OpElementAsgnNode) node);
            case ORNODE: return buildOr((OrNode) node);
            case PREEXENODE: return buildPreExe((PreExeNode) node);
            case POSTEXENODE: return buildPostExe((PostExeNode) node);
            case RATIONALNODE: return buildRational((RationalNode) node);
            case REDONODE: return buildRedo((RedoNode) node);
            case REGEXPNODE: return buildRegexp((RegexpNode) node);
            case RESCUEBODYNODE:
                throw notCompilable("handled by rescue compilation", node);
            case RESCUENODE: return buildRescue((RescueNode) node);
            case RETRYNODE: return buildRetry((RetryNode) node);
            case RETURNNODE: return buildReturn((ReturnNode) node);
            case ROOTNODE:
                throw notCompilable("Use buildRoot()", node);
            case SCLASSNODE: return buildSClass((SClassNode) node);
            case SELFNODE: return buildSelf();
            case SPLATNODE: return buildSplat((SplatNode) node);
            case STRNODE: return buildStr((StrNode) node);
            case SUPERNODE: return buildSuper((SuperNode) node);
            case SVALUENODE: return buildSValue((SValueNode) node);
            case SYMBOLNODE: return buildSymbol((SymbolNode) node);
            case TRUENODE: return buildTrue();
            case UNDEFNODE: return buildUndef(node);
            case UNTILNODE: return buildUntil((UntilNode) node);
            case VALIASNODE: return buildVAlias((VAliasNode) node);
            case VCALLNODE: return buildVCall(result, (VCallNode) node);
            case WHILENODE: return buildWhile((WhileNode) node);
            case WHENNODE: assert false : "When nodes are handled by case node compilation."; return null;
            case XSTRNODE: return buildXStr((XStrNode) node);
            case YIELDNODE: return buildYield((YieldNode) node, result);
            case ZARRAYNODE: return buildZArray(result);
            case ZSUPERNODE: return buildZSuper((ZSuperNode) node);
            default: throw notCompilable("Unknown node encountered in builder", node);
        }
    }

    private boolean hasListener() {
        return manager.getIRScopeListener() != null;
    }

    public IRBuilder newIRBuilder(IRManager manager, IRScope newScope) {
        return new IRBuilder(manager, newScope, this);
    }

    public static IRBuilder topIRBuilder(IRManager manager, IRScope newScope) {
        return new IRBuilder(manager, newScope, null);
    }

    public Operand build(Node node) {
        return build(null, node);
    }

    public Operand build(Variable result, Node node) {
        if (node == null) return null;

        boolean savedExecuteOnce = executesOnce;
        try {
            if (executesOnce) executesOnce = node.executesOnce();

            if (hasListener()) manager.getIRScopeListener().startBuildOperand(node, scope);

            Operand operand = buildOperand(result, node);

            if (hasListener()) manager.getIRScopeListener().endBuildOperand(node, scope, operand);

            return operand;
        } finally {
            executesOnce = savedExecuteOnce;
        }
    }

    private InterpreterContext buildLambdaInner(LambdaNode node) {
        prepareClosureImplicitState();

        addCurrentModule();                                        // %current_module

        receiveBlockArgs(node);

        Operand closureRetVal = node.getBody() == null ? manager.getNil() : build(node.getBody());

        // can be U_NIL if the node is an if node with returns in both branches.
        if (closureRetVal != U_NIL) addInstr(new ReturnInstr(closureRetVal));

        preloadBlockImplicitClosure();

        handleBreakAndReturnsInLambdas();

        computeScopeFlagsFrom(instructions);
        return scope.allocateInterpreterContext(instructions, temporaryVariableIndex + 1, flags);
    }

    private void preloadBlockImplicitClosure() {
        if (needsYieldBlock) {
            addInstrAtBeginning(new LoadBlockImplicitClosureInstr(getYieldClosureVariable()));
        }
    }

    public Operand buildLambda(LambdaNode node) {
        IRClosure closure = new IRClosure(manager, scope, node.getLine(), node.getScope(), Signature.from(node), coverageMode);

        // Create a new nested builder to ensure this gets its own IR builder state like the ensure block stack
        newIRBuilder(manager, closure).buildLambdaInner(node);

        Variable lambda = createTemporaryVariable();
        WrappedIRClosure lambdaBody = new WrappedIRClosure(closure.getSelf(), closure);
        addInstr(new BuildLambdaInstr(lambda, lambdaBody));
        return lambda;
    }

    public Operand buildEncoding(EncodingNode node) {
        Variable ret = createTemporaryVariable();
        addInstr(new GetEncodingInstr(ret, node.getEncoding()));
        return ret;
    }

    // Non-arg masgn
    public Operand buildMultipleAsgn19(MultipleAsgnNode multipleAsgnNode) {
        Node valueNode = multipleAsgnNode.getValueNode();
        Operand values = build(valueNode);
        Variable ret = getValueInTemporaryVariable(values);
        if (valueNode instanceof ArrayNode) {
            buildMultipleAsgn19Assignment(multipleAsgnNode, null, ret);
        } else {
            Variable tmp = createTemporaryVariable();
            addInstr(new ToAryInstr(tmp, ret));
            buildMultipleAsgn19Assignment(multipleAsgnNode, null, tmp);
        }
        return ret;
    }

    protected Variable copyAndReturnValue(Operand val) {
        return addResultInstr(new CopyInstr(createTemporaryVariable(), val));
    }

    protected Operand buildWithOrder(Node node, boolean preserveOrder) {
        Operand value = build(node);

        // We need to preserve order in cases (like in presence of assignments) except that immutable
        // literals can never change value so we can still emit these out of order.
        return preserveOrder && !(value instanceof ImmutableLiteral) ? copyAndReturnValue(value) : value;
    }

    protected Operand buildLazyWithOrder(CallNode node, Label lazyLabel, Label endLabel, boolean preserveOrder) {
        Operand value = buildCall(null, node, lazyLabel, endLabel);

        // We need to preserve order in cases (like in presence of assignments) except that immutable
        // literals can never change value so we can still emit these out of order.
        return preserveOrder && !(value instanceof ImmutableLiteral) ? copyAndReturnValue(value) : value;
    }

    protected Variable getValueInTemporaryVariable(Operand val) {
        if (val != null && val instanceof TemporaryVariable) return (Variable) val;

        return copyAndReturnValue(val);
    }

    // Return the last argument in the list as this represents rhs of the overall attrassign expression
    // e.g. 'a[1] = 2 #=> 2' or 'a[1] = 1,2,3 #=> [1,2,3]'
    protected Operand buildAttrAssignCallArgs(List<Operand> argsList, Node args, boolean containsAssignment) {
        if (args == null) return manager.getNil();

        switch (args.getNodeType()) {
            case ARRAYNODE: {     // a[1] = 2; a[1,2,3] = 4,5,6
                Operand last = manager.getNil();
                for (Node n: ((ListNode) args).children()) {
                    last = buildWithOrder(n, containsAssignment);
                    argsList.add(last);
                }
                return last;
            }
            case ARGSCATNODE: {
                ArgsCatNode argsCatNode = (ArgsCatNode)args;
                Operand lhs = build(argsCatNode.getFirstNode());
                Operand rhs = build(argsCatNode.getSecondNode());
                Variable res = createTemporaryVariable();
                addInstr(new BuildCompoundArrayInstr(res, lhs, rhs, false));
                argsList.add(new Splat(res));
                return rhs;
            }
            case ARGSPUSHNODE:  { // a[1, *b] = 2
                ArgsPushNode argsPushNode = (ArgsPushNode)args;
                Operand lhs = build(argsPushNode.getFirstNode());
                Operand rhs = build(argsPushNode.getSecondNode());
                Variable res = createTemporaryVariable();
                addInstr(new BuildCompoundArrayInstr(res, lhs, rhs, true));
                argsList.add(new Splat(res));
                return rhs;
            }
            case SPLATNODE: {     // a[1] = *b
                Splat rhs = new Splat(buildSplat((SplatNode)args));
                argsList.add(rhs);
                return rhs;
            }
        }

        throw notCompilable("Invalid node for attrassign call args", args);
    }

    protected Operand[] buildCallArgs(Node args) {
        return buildCallArgsExcept(args, null);
    }

    /**
     * build each argument to a call but if we detect what appears to be a literal simple keyword argument
     * signature we will pass that along to be excluded in the build here (we will build it separately).
     * @param args for a call to be built
     * @param excludeKeywordArg do not build the last one since it is a keyword arg
     * @return the operands for the call.
     */
    protected Operand[] buildCallArgsExcept(Node args, Node excludeKeywordArg) {
        switch (args.getNodeType()) {
            case ARGSCATNODE:
            case ARGSPUSHNODE:
                return new Operand[] { new Splat(addResultInstr(new BuildSplatInstr(createTemporaryVariable(), build(args), false))) };
            case ARRAYNODE: {
                Node[] children = ((ListNode) args).children();
                int numberOfArgs = children.length - (excludeKeywordArg != null ? 1 : 0);
                Operand[] builtArgs = new Operand[numberOfArgs];
                boolean hasAssignments = args.containsVariableAssignment();

                for (int i = 0; i < numberOfArgs; i++) {
                    builtArgs[i] = buildWithOrder(children[i], hasAssignments);
                }
                return builtArgs;
            }
            case SPLATNODE:
                return new Operand[] { new Splat(addResultInstr(new BuildSplatInstr(createTemporaryVariable(), build(args), false))) };
        }

        throw notCompilable("Invalid node for call args: ", args);
    }

    public Operand[] setupCallArgs(Node args) {
        return args == null ? Operand.EMPTY_ARRAY : buildCallArgs(args);
    }

    public static Operand[] addArg(Operand[] args, Operand extraArg) {
        Operand[] newArgs = new Operand[args.length + 1];
        System.arraycopy(args, 0, newArgs, 0, args.length);
        newArgs[args.length] = extraArg;
        return newArgs;
    }

    // This method is called to build assignments for a multiple-assignment instruction
    public void buildAssignment(Node node, Variable rhsVal) {
        switch (node.getNodeType()) {
            case ATTRASSIGNNODE:
                buildAttrAssignAssignment(node, rhsVal);
                break;
            case CLASSVARASGNNODE:
                addInstr(new PutClassVariableInstr(classVarDefinitionContainer(), ((ClassVarAsgnNode)node).getName(), rhsVal));
                break;
            case CONSTDECLNODE:
                buildConstDeclAssignment((ConstDeclNode) node, rhsVal);
                break;
            case DASGNNODE: {
                DAsgnNode variable = (DAsgnNode) node;
                int depth = variable.getDepth();
                addInstr(new CopyInstr(getLocalVariable(variable.getName(), depth), rhsVal));
                break;
            }
            case GLOBALASGNNODE:
                addInstr(new PutGlobalVarInstr(((GlobalAsgnNode)node).getName(), rhsVal));
                break;
            case INSTASGNNODE:
                // NOTE: if 's' happens to the a class, this is effectively an assignment of a class instance variable
                addInstr(new PutFieldInstr(buildSelf(), ((InstAsgnNode)node).getName(), rhsVal));
                break;
            case LOCALASGNNODE: {
                LocalAsgnNode localVariable = (LocalAsgnNode) node;
                int depth = localVariable.getDepth();
                addInstr(new CopyInstr(getLocalVariable(localVariable.getName(), depth), rhsVal));
                break;
            }
            case ZEROARGNODE:
                throw notCompilable("Shouldn't get here; zeroarg does not do assignment", node);
            case MULTIPLEASGNNODE: {
                Variable tmp = createTemporaryVariable();
                addInstr(new ToAryInstr(tmp, rhsVal));
                buildMultipleAsgn19Assignment((MultipleAsgnNode)node, null, tmp);
                break;
            }
            default:
                throw notCompilable("Can't build assignment node", node);
        }
    }

    protected LocalVariable getBlockArgVariable(RubySymbol name, int depth) {
        if (!(scope instanceof IRFor)) throw notCompilable("Cannot ask for block-arg variable in 1.9 mode", null);

        return getLocalVariable(name, depth);
    }

    protected void receiveBlockArg(Variable v, Operand argsArray, int argIndex, boolean isSplat) {
        if (argsArray != null) {
            // We are in a nested receive situation -- when we are not at the root of a masgn tree
            // Ex: We are trying to receive (b,c) in this example: "|a, (b,c), d| = ..."
            if (isSplat) addInstr(new RestArgMultipleAsgnInstr(v, argsArray, argIndex));
            else addInstr(new ReqdArgMultipleAsgnInstr(v, argsArray, argIndex));
        } else {
            // argsArray can be null when the first node in the args-node-ast is a multiple-assignment
            // For example, for-nodes
            addInstr(isSplat ? new ReceiveRestArgInstr(v, argIndex, argIndex) : new ReceivePreReqdArgInstr(v, argIndex));
        }
    }

    public void buildVersionSpecificBlockArgsAssignment(Node node) {
        if (!(scope instanceof IRFor)) throw notCompilable("Should not have come here for block args assignment", node);

        // Argh!  For-loop bodies and regular iterators are different in terms of block-args!
        switch (node.getNodeType()) {
            case MULTIPLEASGNNODE: {
                ListNode sourceArray = ((MultipleAsgnNode) node).getPre();
                int i = 0;
                for (Node an: sourceArray.children()) {
                    // Use 1.8 mode version for this
                    buildBlockArgsAssignment(an, null, i, false);
                    i++;
                }
                break;
            }
            default:
                throw notCompilable("Can't build assignment node", node);
        }
    }

    // This method is called to build arguments for a block!
    public void buildBlockArgsAssignment(Node node, Operand argsArray, int argIndex, boolean isSplat) {
        Variable v;
        switch (node.getNodeType()) {
            case ATTRASSIGNNODE:
                v = createTemporaryVariable();
                receiveBlockArg(v, argsArray, argIndex, isSplat);
                buildAttrAssignAssignment(node, v);
                break;
            case DASGNNODE: {
                DAsgnNode dynamicAsgn = (DAsgnNode) node;
                v = getBlockArgVariable(dynamicAsgn.getName(), dynamicAsgn.getDepth());
                receiveBlockArg(v, argsArray, argIndex, isSplat);
                break;
            }
            case CLASSVARASGNNODE:
                v = createTemporaryVariable();
                receiveBlockArg(v, argsArray, argIndex, isSplat);
                addInstr(new PutClassVariableInstr(classVarDefinitionContainer(), ((ClassVarAsgnNode)node).getName(), v));
                break;
            case CONSTDECLNODE:
                v = createTemporaryVariable();
                receiveBlockArg(v, argsArray, argIndex, isSplat);
                buildConstDeclAssignment((ConstDeclNode) node, v);
                break;
            case GLOBALASGNNODE:
                v = createTemporaryVariable();
                receiveBlockArg(v, argsArray, argIndex, isSplat);
                addInstr(new PutGlobalVarInstr(((GlobalAsgnNode)node).getName(), v));
                break;
            case INSTASGNNODE:
                v = createTemporaryVariable();
                receiveBlockArg(v, argsArray, argIndex, isSplat);
                // NOTE: if 's' happens to the a class, this is effectively an assignment of a class instance variable
                addInstr(new PutFieldInstr(buildSelf(), ((InstAsgnNode)node).getName(), v));
                break;
            case LOCALASGNNODE: {
                LocalAsgnNode localVariable = (LocalAsgnNode) node;
                v = getBlockArgVariable(localVariable.getName(), localVariable.getDepth());
                receiveBlockArg(v, argsArray, argIndex, isSplat);
                break;
            }
            case ZEROARGNODE:
                throw notCompilable("Shouldn't get here; zeroarg does not do assignment", node);
            default:
                buildVersionSpecificBlockArgsAssignment(node);
        }
    }

    public Operand buildAlias(final AliasNode alias) {
        Operand newName = build(alias.getNewName());
        Operand oldName = build(alias.getOldName());
        addInstr(new AliasInstr(newName, oldName));

        return manager.getNil();
    }

    // Translate "ret = (a && b)" --> "ret = (a ? b : false)" -->
    //
    //    v1 = -- build(a) --
    //       OPT: ret can be set to v1, but effectively v1 is false if we take the branch to L.
    //            while this info can be inferred by using attributes, why bother if we can do this?
    //    ret = v1
    //    beq(v1, false, L)
    //    v2 = -- build(b) --
    //    ret = v2
    // L:
    //
    public Operand buildAnd(final AndNode andNode) {
        if (andNode.getFirstNode().getNodeType().alwaysTrue()) {
            // build first node (and ignore its result) and then second node
            build(andNode.getFirstNode());
            return build(andNode.getSecondNode());
        } else if (andNode.getFirstNode().getNodeType().alwaysFalse()) {
            // build first node only and return its value
            return build(andNode.getFirstNode());
        } else {
            Label l = getNewLabel();
            Operand v1 = build(andNode.getFirstNode());
            Variable ret = getValueInTemporaryVariable(v1);
            addInstr(createBranch(v1, manager.getFalse(), l));
            Operand v2 = build(andNode.getSecondNode());
            addInstr(new CopyInstr(ret, v2));
            addInstr(new LabelInstr(l));
            return ret;
        }
    }

    public Operand buildArray(ArrayNode node, boolean operandOnly) {
        Node[] nodes = node.children();
        Operand[] elts = new Operand[nodes.length];
        boolean containsAssignments = node.containsVariableAssignment();
        for (int i = 0; i < nodes.length; i++) {
            elts[i] = buildWithOrder(nodes[i], containsAssignments);
        }

        Operand array = new Array(elts);
        return operandOnly ? array : copyAndReturnValue(array);
    }

    public Operand buildArgsCat(final ArgsCatNode argsCatNode) {
        Operand lhs = build(argsCatNode.getFirstNode());
        Operand rhs = build(argsCatNode.getSecondNode());

        return addResultInstr(new BuildCompoundArrayInstr(createTemporaryVariable(), lhs, rhs, false));
    }

    public Operand buildArgsPush(final ArgsPushNode node) {
        Operand lhs = build(node.getFirstNode());
        Operand rhs = build(node.getSecondNode());

        return addResultInstr(new BuildCompoundArrayInstr(createTemporaryVariable(), lhs, rhs, true));
    }

    private Operand buildAttrAssign(Variable result, AttrAssignNode attrAssignNode) {
        boolean containsAssignment = attrAssignNode.containsVariableAssignment();
        Operand obj = buildWithOrder(attrAssignNode.getReceiverNode(), containsAssignment);

        Label lazyLabel = null;
        Label endLabel = null;
        if (result == null) result = createTemporaryVariable();
        if (attrAssignNode.isLazy()) {
            lazyLabel = getNewLabel();
            endLabel = getNewLabel();
            addInstr(new BNilInstr(lazyLabel, obj));
        }

        List<Operand> args = new ArrayList<>();
        Node argsNode = attrAssignNode.getArgsNode();
        Operand lastArg = buildAttrAssignCallArgs(args, argsNode, containsAssignment);
        Operand block = setupCallClosure(attrAssignNode.getBlockNode());
        addInstr(AttrAssignInstr.create(scope, obj, attrAssignNode.getName(), args.toArray(new Operand[args.size()]), block, scope.maybeUsingRefinements()));
        addInstr(new CopyInstr(result, lastArg));

        if (attrAssignNode.isLazy()) {
            addInstr(new JumpInstr(endLabel));
            addInstr(new LabelInstr(lazyLabel));
            addInstr(new CopyInstr(result, manager.getNil()));
            addInstr(new LabelInstr(endLabel));
        }

        return result;
    }

    public Operand buildAttrAssignAssignment(Node node, Operand value) {
        final AttrAssignNode attrAssignNode = (AttrAssignNode) node;
        Operand obj = build(attrAssignNode.getReceiverNode());
        Operand[] args = setupCallArgs(attrAssignNode.getArgsNode());
        args = addArg(args, value);
        addInstr(AttrAssignInstr.create(scope, obj, attrAssignNode.getName(), args, scope.maybeUsingRefinements()));
        return value;
    }

    public Operand buildBackref(Variable result, BackRefNode node) {
        if (result == null) result = createTemporaryVariable();
        return addResultInstr(new BuildBackrefInstr(result, node.getType()));
    }

    public Operand buildBegin(BeginNode beginNode) {
        return build(beginNode.getBodyNode());
    }

    public Operand buildBignum(BignumNode node) {
        // Since bignum literals are effectively interned objects, no need to copyAndReturnValue(...)
        // SSS FIXME: Or is this a premature optimization?
        return new Bignum(node.getValue());
    }

    public Operand buildBlock(BlockNode node) {
        Operand retVal = null;
        for (Node child : node.children()) {
            retVal = build(child);
        }

        // Value of the last expression in the block
        return retVal;
    }

    public Operand buildBreak(BreakNode breakNode) {
        IRLoop currLoop = getCurrentLoop();

        if (currLoop != null) {
            // If we have ensure blocks, have to run those first!
            if (!activeEnsureBlockStack.isEmpty()) emitEnsureBlocks(currLoop);

            addInstr(new CopyInstr(currLoop.loopResult, build(breakNode.getValueNode())));
            addInstr(new JumpInstr(currLoop.loopEndLabel));
        } else {
            if (scope instanceof IRClosure) {
                // This lexical scope value is only used (and valid) in regular block contexts.
                // If this instruction is executed in a Proc or Lambda context, the lexical scope value is useless.
                IRScope returnScope = scope.getLexicalParent();
                if (scope instanceof IREvalScript || returnScope == null) {
                    // We are not in a closure or a loop => bad break instr!
                    throwSyntaxError(breakNode, "Can't escape from eval with redo");
                } else {
                    addInstr(new BreakInstr(build(breakNode.getValueNode()), returnScope.getId()));
                }
            } else {
                // We are not in a closure or a loop => bad break instr!
                throwSyntaxError(breakNode, "Invalid break");
            }
        }

        // Once the break instruction executes, control exits this scope
        return U_NIL;
    }

    private void throwSyntaxError(Node node, String message) {
        String errorMessage = getFileName() + ":" + (node.getLine() + 1) + ": " + message;
        throw scope.getManager().getRuntime().newSyntaxError(errorMessage);
    }

    private void handleNonlocalReturnInMethod() {
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
        Variable exc = createTemporaryVariable();
        addInstr(new ReceiveJRubyExceptionInstr(exc));

        // Handle break using runtime helper
        // --> IRRuntimeHelpers.handleNonlocalReturn(scope, bj, blockType)
        Variable ret = createTemporaryVariable();
        addInstr(new RuntimeHelperCall(ret, HANDLE_NONLOCAL_RETURN, new Operand[]{exc} ));
        addInstr(new ReturnInstr(ret));

        // End
        addInstr(new LabelInstr(rEndLabel));
    }

    private Operand receiveBreakException(Operand block, CodeBlock codeBlock) {
        // Check if we have to handle a break
        if (block == null ||
            !(block instanceof WrappedIRClosure) ||
            !(((WrappedIRClosure)block).getClosure()).hasBreakInstructions()) {
            // No protection needed -- add the call and return
            return codeBlock.run();
        }

        Label rBeginLabel = getNewLabel();
        Label rEndLabel   = getNewLabel();
        Label rescueLabel = getNewLabel();

        // Protected region
        addInstr(new LabelInstr(rBeginLabel));
        addInstr(new ExceptionRegionStartMarkerInstr(rescueLabel));
        Variable callResult = (Variable)codeBlock.run();
        addInstr(new JumpInstr(rEndLabel));
        addInstr(new ExceptionRegionEndMarkerInstr());

        // Receive exceptions (could be anything, but the handler only processes IRBreakJumps)
        addInstr(new LabelInstr(rescueLabel));
        Variable exc = createTemporaryVariable();
        addInstr(new ReceiveJRubyExceptionInstr(exc));

        // Handle break using runtime helper
        // --> IRRuntimeHelpers.handlePropagatedBreak(context, scope, bj, blockType)
        addInstr(new RuntimeHelperCall(callResult, HANDLE_PROPAGATED_BREAK, new Operand[]{exc} ));

        // End
        addInstr(new LabelInstr(rEndLabel));

        return callResult;
    }

    // Wrap call in a rescue handler that catches the IRBreakJump
    private void receiveBreakException(Operand block, final CallInstr callInstr) {
        receiveBreakException(block, () -> addResultInstr(callInstr));
    }

    public Operand buildCall(Variable aResult, CallNode callNode, Label lazyLabel, Label endLabel) {
        RubySymbol name = methodName = callNode.getName();

        Node receiverNode = callNode.getReceiverNode();

        // Frozen string optimization: check for "string".freeze
        String id = name.idString(); // ID Str ok here since it is 7bit check.
        if (receiverNode instanceof StrNode && (id.equals("freeze") || id.equals("-@"))) {
            StrNode asString = (StrNode) receiverNode;
            return new FrozenString(asString.getValue(), asString.getCodeRange(), scope.getFile(), asString.getLine());
        }

        boolean compileLazyLabel = false;
        if (callNode.isLazy()) {
            if (lazyLabel == null) {
                compileLazyLabel = true;
                lazyLabel = getNewLabel();
                endLabel = getNewLabel();
            }
        }

        // The receiver has to be built *before* call arguments are built
        // to preserve expected code execution order
        Operand receiver;
        if (receiverNode instanceof CallNode && ((CallNode) receiverNode).isLazy()) {
            receiver = buildLazyWithOrder((CallNode) receiverNode, lazyLabel, endLabel, callNode.containsVariableAssignment());
        } else {
            receiver = buildWithOrder(receiverNode, callNode.containsVariableAssignment());
        }

        final Variable result = aResult == null ? createTemporaryVariable() : aResult;

        // obj["string"] optimization for Hash
        ArrayNode argsAry;
        if (!callNode.isLazy() &&
                id.equals("[]") &&
                callNode.getArgsNode() instanceof ArrayNode &&
                (argsAry = (ArrayNode) callNode.getArgsNode()).size() == 1 &&
                argsAry.get(0) instanceof StrNode &&
                !scope.maybeUsingRefinements() &&
                receiverNode instanceof HashNode &&
                callNode.getIterNode() == null) {
            StrNode keyNode = (StrNode) argsAry.get(0);
            addInstr(ArrayDerefInstr.create(scope, result, receiver, new FrozenString(keyNode.getValue(), keyNode.getCodeRange(), scope.getFile(), keyNode.getLine())));
            return result;
        }

        if (callNode.isLazy()) addInstr(new BNilInstr(lazyLabel, receiver));

        HashNode keywordArgs = getPossibleKeywordArgument(callNode.getArgsNode());
        if (keywordArgs != null) {
            Operand[] args = buildCallArgsExcept(callNode.getArgsNode(), keywordArgs);

            if (keywordArgs.hasOnlyRestKwargs()) {  // {**k}, {**{}, **k}, etc...
                Variable splatValue = buildRestKeywordArgs(keywordArgs);
                Variable test = addResultInstr(new RuntimeHelperCall(createTemporaryVariable(), IS_HASH_EMPTY, new Operand[] { splatValue }));
                Operand block = setupCallClosure(callNode.getIterNode());

                determineIfWeNeedLineNumber(callNode); // buildOperand for fcall was papered over by args operand building so we check once more.
                if_else(test, manager.getTrue(),
                        () -> receiveBreakException(block,
                                determineIfProcNew(receiverNode,
                                        CallInstr.create(scope, NORMAL, result, name, receiver, args, block))),
                        () -> receiveBreakException(block,
                                determineIfProcNew(receiverNode,
                                        CallInstr.create(scope, NORMAL, result, name, receiver, addArg(args, splatValue), block))));
            } else {  // {a: 1, b: 2}
                List<KeyValuePair<Operand, Operand>> kwargs = buildKeywordArguments(keywordArgs);
                Operand block = setupCallClosure(callNode.getIterNode());

                determineIfWeNeedLineNumber(callNode); // buildOperand for fcall was papered over by args operand building so we check once more.
                receiveBreakException(block,
                        determineIfProcNew(receiverNode,
                                CallInstr.createWithKwargs(scope, NORMAL, result, name, receiver, args, block, kwargs)));
            }
        } else {
            Operand[] args = setupCallArgs(callNode.getArgsNode());
            Operand block = setupCallClosure(callNode.getIterNode());

            determineIfWeNeedLineNumber(callNode);
            receiveBreakException(block,
                    determineIfProcNew(receiverNode,
                            CallInstr.create(scope, result, name, receiver, args, block)));
        }

        if (compileLazyLabel) {
            addInstr(new JumpInstr(endLabel));
            addInstr(new LabelInstr(lazyLabel));
            addInstr(new CopyInstr(result, manager.getNil()));
            addInstr(new LabelInstr(endLabel));
        }

        return result;
    }

    private List<KeyValuePair<Operand, Operand>> buildKeywordArguments(HashNode keywordArgs) {
        List<KeyValuePair<Operand, Operand>> kwargs = new ArrayList<>();
        for (KeyValuePair<Node, Node> pair: keywordArgs.getPairs()) {
            kwargs.add(new KeyValuePair<>(build(pair.getKey()), build(pair.getValue())));
        }
        return kwargs;
    }

    private CallInstr determineIfProcNew(Node receiverNode, CallInstr callInstr) {
        // This is to support the ugly Proc.new with no block, which must see caller's frame
        if (CommonByteLists.NEW_METHOD.equals(callInstr.getName().getBytes()) &&
                receiverNode instanceof ConstNode && ((ConstNode)receiverNode).getName().idString().equals("Proc")) {
            callInstr.setProcNew(true);
        }

        return callInstr;
    }

    /*
     * This will ignore complexity of a hash which contains restkwargs and only return simple
     * last argument which happens to be all key symbol hashnode which is not empty
     */
    private HashNode getPossibleKeywordArgument(Node argsNode) {
        // Block pass wraps itself around the main args list so don't hold that against it.
        if (argsNode instanceof BlockPassNode) {
            return null; //getPossibleKeywordArgument(((BlockPassNode) argsNode).getArgsNode());
        }

        if (argsNode instanceof ArrayNode) {
            ArrayNode argsList = (ArrayNode) argsNode;

            if (argsList.isEmpty()) return null;

            Node lastNode = argsList.getLast();

            if (lastNode instanceof HashNode) {
                HashNode hash = (HashNode) lastNode;
                if (hash.isMaybeKwargs()) return (HashNode) lastNode;
            }
        }

        return null;
    }

    public Operand buildCase(CaseNode caseNode) {
        if (caseNode.getCaseNode() != null) {
            // scan all cases to see if we have a homogeneous literal case/when
            NodeType seenType = null;
            for (Node aCase : caseNode.getCases().children()) {
                WhenNode whenNode = (WhenNode) aCase;
                NodeType exprNodeType = whenNode.getExpressionNodes().getNodeType();

                if (seenType == null) {
                    seenType = exprNodeType;
                } else if (seenType != exprNodeType) {
                    seenType = null;
                    break;
                }
            }

            if (seenType != null) {
                switch (seenType) {
                    case FIXNUMNODE:
                        return buildFixnumCase(caseNode);
                }
            }
        }

        Operand testValue = buildCaseTestValue(caseNode); // what each when arm gets tested against.
        Label elseLabel = getNewLabel();                  // where else body is location (implicit or explicit).
        Label endLabel = getNewLabel();                   // end of the entire case statement.
        boolean hasExplicitElse = caseNode.getElseNode() != null; // does this have an explicit 'else' or not.
        Variable result = createTemporaryVariable();      // final result value of the case statement.
        Map<Label, Node> bodies = new HashMap<>();        // we save bodies and emit them after processing when values.
        Set<IRubyObject> seenLiterals = new HashSet<>();  // track to warn on duplicated values in when clauses.

        for (Node aCase: caseNode.getCases().children()) { // Emit each when value test against the case value.
            WhenNode when = (WhenNode) aCase;
            Label bodyLabel = getNewLabel();

            buildWhenArgs(when, testValue, bodyLabel, seenLiterals);
            bodies.put(bodyLabel, when.getBodyNode());
        }

        addInstr(new JumpInstr(elseLabel));               // if no explicit matches jump to else

        if (hasExplicitElse) {                            // build explicit else
            bodies.put(elseLabel, caseNode.getElseNode());
        }

        int numberOfBodies = bodies.size();
        int i = 1;
        for (Map.Entry<Label, Node> entry: bodies.entrySet()) {
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
            addInstr(new CopyInstr(result, manager.getNil()));
        }

        addInstr(new LabelInstr(endLabel));

        return result;
    }

    private Operand buildCaseTestValue(CaseNode caseNode) {
        Operand testValue = build(caseNode.getCaseNode());

        // null is returned for valueless case statements:
        //   case
        //     when true <blah>
        //     when false <blah>
        //   end
        return testValue == null ? UndefinedValue.UNDEFINED : testValue;
    }

    // returns true if we should emit an eqq for this value (e.g. it has not already been seen yet).
    private boolean literalWhenCheck(Node value, Set<IRubyObject> seenLiterals) {
        IRubyObject literal = getWhenLiteral(value);

        if (literal != null) {
            if (seenLiterals.contains(literal)) {
                scope.getManager().getRuntime().getWarnings().warning(IRubyWarnings.ID.MISCELLANEOUS,
                        getFileName(), value.getLine(), "duplicated when clause is ignored");
                return false;
            } else {
                seenLiterals.add(literal);
                return true;
            }
        }

        return true;
    }

    private void buildWhenValues(Variable eqqResult, ListNode exprValues, Operand testValue, Label bodyLabel,
                                 Set<IRubyObject> seenLiterals) {
        for (Node value: exprValues.children()) {
            buildWhenValue(eqqResult, testValue, bodyLabel, value, seenLiterals, false);
        }
    }

    private void buildWhenValue(Variable eqqResult, Operand testValue, Label bodyLabel, Node node,
                                Set<IRubyObject> seenLiterals, boolean needsSplat) {
        if (literalWhenCheck(node, seenLiterals)) { // we only emit first literal of the same value.
            Operand expression = buildWithOrder(node, node.containsVariableAssignment());

            addInstr(new EQQInstr(scope, eqqResult, expression, testValue, needsSplat, scope.maybeUsingRefinements()));
            addInstr(createBranch(eqqResult, manager.getTrue(), bodyLabel));
        }
    }

    private void buildWhenSplatValues(Variable eqqResult, Node node, Operand testValue, Label bodyLabel,
                                      Set<IRubyObject> seenLiterals) {
        if (node instanceof ListNode && !(node instanceof DNode) && !(node instanceof ArrayNode)) {
            buildWhenValues(eqqResult, (ListNode) node, testValue, bodyLabel, seenLiterals);
        } else if (node instanceof SplatNode) {
            buildWhenValue(eqqResult, testValue, bodyLabel, node, seenLiterals, true);
        } else if (node instanceof ArgsCatNode) {
            ArgsCatNode catNode = (ArgsCatNode) node;
            buildWhenSplatValues(eqqResult, catNode.getFirstNode(), testValue, bodyLabel, seenLiterals);
            buildWhenSplatValues(eqqResult, catNode.getSecondNode(), testValue, bodyLabel, seenLiterals);
        } else if (node instanceof ArgsPushNode) {
            ArgsPushNode pushNode = (ArgsPushNode) node;
            buildWhenSplatValues(eqqResult, pushNode.getFirstNode(), testValue, bodyLabel, seenLiterals);
            buildWhenValue(eqqResult, testValue, bodyLabel, pushNode.getSecondNode(), seenLiterals, false);
        } else {
            buildWhenValue(eqqResult, testValue, bodyLabel, node, seenLiterals, true);
        }
    }

    private void buildWhenArgs(WhenNode whenNode, Operand testValue, Label bodyLabel, Set<IRubyObject> seenLiterals) {
        Variable eqqResult = createTemporaryVariable();
        Node exprNodes = whenNode.getExpressionNodes();

        if (exprNodes instanceof ListNode && !(exprNodes instanceof DNode) && !(exprNodes instanceof ArrayNode)) {
            buildWhenValues(eqqResult, (ListNode) exprNodes, testValue, bodyLabel, seenLiterals);
        } else if (exprNodes instanceof ArgsPushNode || exprNodes instanceof SplatNode || exprNodes instanceof ArgsCatNode) {
            buildWhenSplatValues(eqqResult, exprNodes, testValue, bodyLabel, seenLiterals);
        } else {
            buildWhenValue(eqqResult, testValue, bodyLabel, exprNodes, seenLiterals, false);
        }
    }

    // Note: This is potentially a little wasteful in that we eagerly create these literals for a duplicated warning
    // check.  In most cases these would be made anyways (e.g. symbols/fixnum) but in others we double allocation
    // (e.g. strings).
    private IRubyObject getWhenLiteral(Node node) {
        Ruby runtime = scope.getManager().getRuntime();

        switch(node.getNodeType()) {
            case FIXNUMNODE:
                return runtime.newFixnum(((FixnumNode) node).getValue());
            case FLOATNODE:
                return runtime.newFloat((((FloatNode) node).getValue()));
            case BIGNUMNODE:
                return new RubyBignum(runtime, ((BignumNode) node).getValue());
            case COMPLEXNODE:
                return RubyComplex.newComplexRaw(runtime, getWhenLiteral(((ComplexNode) node).getNumber()));
            case RATIONALNODE:
                return RubyRational.newRationalRaw(runtime, getWhenLiteral(((RationalNode)node).getDenominator()), getWhenLiteral(((RationalNode) node).getNumerator()));
            case NILNODE:
                return runtime.getNil();
            case TRUENODE:
                return runtime.getTrue();
            case FALSENODE:
                return runtime.getFalse();
            case SYMBOLNODE:
                return ((SymbolNode) node).getName();
            case STRNODE:
                return runtime.newString(((StrNode) node).getValue());
        }

        return null;
    }

    private Operand buildFixnumCase(CaseNode caseNode) {
        Map<Integer, Label> jumpTable = new HashMap<>();
        Map<Node, Label> nodeBodies = new HashMap<>();

        // gather fixnum-when bodies or bail
        for (Node aCase : caseNode.getCases().children()) {
            WhenNode whenNode = (WhenNode) aCase;
            Label bodyLabel = getNewLabel();

            FixnumNode expr = (FixnumNode) whenNode.getExpressionNodes();
            long exprLong = expr.getValue();
            if (exprLong > Integer.MAX_VALUE) throw notCompilable("optimized fixnum case has long-ranged", caseNode);

            if (jumpTable.get((int) exprLong) == null) {
                jumpTable.put((int) exprLong, bodyLabel);
                nodeBodies.put(whenNode, bodyLabel);
            } else {
                scope.getManager().getRuntime().getWarnings().warning(IRubyWarnings.ID.MISCELLANEOUS, getFileName(), expr.getLine(), "duplicated when clause is ignored");
            }
        }

        // sort the jump table
        Map.Entry<Integer, Label>[] jumpEntries = jumpTable.entrySet().toArray(new Map.Entry[jumpTable.size()]);
        Arrays.sort(jumpEntries, new Comparator<Map.Entry<Integer, Label>>() {
            @Override
            public int compare(Map.Entry<Integer, Label> o1, Map.Entry<Integer, Label> o2) {
                return Integer.compare(o1.getKey(), o2.getKey());
            }
        });

        // build a switch
        int[] jumps = new int[jumpTable.size()];
        Label[] targets = new Label[jumps.length];
        int i = 0;
        for (Map.Entry<Integer, Label> jumpEntry : jumpEntries) {
            jumps[i] = jumpEntry.getKey();
            targets[i] = jumpEntry.getValue();
            i++;
        }

        // get the incoming case value
        Operand value = build(caseNode.getCaseNode());

        Label     eqqPath   = getNewLabel();
        Label     endLabel  = getNewLabel();
        boolean   hasElse   = (caseNode.getElseNode() != null);
        Label     elseLabel = getNewLabel();
        Variable  result    = createTemporaryVariable();

        // insert fast switch with fallback to eqq
        addInstr(new BSwitchInstr(jumps, value, eqqPath, targets, elseLabel));
        addInstr(new LabelInstr(eqqPath));

        List<Label> labels = new ArrayList<>();
        Map<Label, Node> bodies = new HashMap<>();

        // build each "when"
        for (Node aCase : caseNode.getCases().children()) {
            WhenNode whenNode = (WhenNode)aCase;
            Label bodyLabel = nodeBodies.get(whenNode);
            if (bodyLabel == null) bodyLabel = getNewLabel();

            Variable eqqResult = createTemporaryVariable();
            labels.add(bodyLabel);
            Operand expression = build(whenNode.getExpressionNodes());

            // use frozen string for direct literal strings in `when`
            if (expression instanceof MutableString) {
                expression = ((MutableString) expression).frozenString;
            }

            addInstr(new EQQInstr(scope, eqqResult, expression, value, false, scope.maybeUsingRefinements()));
            addInstr(createBranch(eqqResult, manager.getTrue(), bodyLabel));

            // SSS FIXME: This doesn't preserve original order of when clauses.  We could consider
            // preserving the order (or maybe not, since we would have to sort the constants first
            // in any case) for outputting jump tables in certain situations.
            //
            // add body to map for emitting later
            bodies.put(bodyLabel, whenNode.getBodyNode());
        }

        // Jump to else in case nothing matches!
        addInstr(new JumpInstr(elseLabel));

        // Build "else" if it exists
        if (hasElse) {
            labels.add(elseLabel);
            bodies.put(elseLabel, caseNode.getElseNode());
        }

        // Now, emit bodies while preserving when clauses order
        for (Label whenLabel: labels) {
            addInstr(new LabelInstr(whenLabel));
            Operand bodyValue = build(bodies.get(whenLabel));
            // bodyValue can be null if the body ends with a return!
            if (bodyValue != null) {
                // SSS FIXME: Do local optimization of break results (followed by a copy & jump) to short-circuit the jump right away
                // rather than wait to do it during an optimization pass when a dead jump needs to be removed.  For this, you have
                // to look at what the last generated instruction was.
                addInstr(new CopyInstr(result, bodyValue));
                addInstr(new JumpInstr(endLabel));
            }
        }

        if (!hasElse) {
            addInstr(new LabelInstr(elseLabel));
            addInstr(new CopyInstr(result, manager.getNil()));
            addInstr(new JumpInstr(endLabel));
        }

        // Close it out
        addInstr(new LabelInstr(endLabel));

        return result;
    }

    /**
     * Build a new class and add it to the current scope (s).
     */
    public Operand buildClass(ClassNode classNode) {
        boolean executesOnce = this.executesOnce;
        Node superNode = classNode.getSuperNode();
        Colon3Node cpath = classNode.getCPath();
        Operand superClass = (superNode == null) ? null : build(superNode);
        ByteList className = cpath.getName().getBytes();
        Operand container = getContainerFromCPath(cpath);

        IRClassBody body = new IRClassBody(manager, scope, className, classNode.getLine(), classNode.getScope(), executesOnce);
        Variable bodyResult = addResultInstr(new DefineClassInstr(createTemporaryVariable(), body, container, superClass));

        newIRBuilder(manager, body).buildModuleOrClassBody(classNode.getBodyNode(), classNode.getLine(), classNode.getEndLine());
        return bodyResult;
    }

    // class Foo; class << self; end; end
    // Here, the class << self declaration is in Foo's body.
    // Foo is the class in whose context this is being defined.
    public Operand buildSClass(SClassNode sclassNode) {
        Operand receiver = build(sclassNode.getReceiverNode());
        // FIXME: metaclass name should be a bytelist
        IRModuleBody body = new IRMetaClassBody(manager, scope, manager.getMetaClassName().getBytes(), sclassNode.getLine(), sclassNode.getScope());
        Variable sClassVar = addResultInstr(new DefineMetaClassInstr(createTemporaryVariable(), receiver, body));

        // sclass bodies inherit the block of their containing method
        Variable processBodyResult = addResultInstr(new ProcessModuleBodyInstr(createTemporaryVariable(), sClassVar, getYieldClosureVariable()));
        newIRBuilder(manager, body).buildModuleOrClassBody(sclassNode.getBodyNode(), sclassNode.getLine(), sclassNode.getEndLine());
        return processBodyResult;
    }

    // @@c
    public Operand buildClassVar(ClassVarNode node) {
        Variable ret = createTemporaryVariable();
        addInstr(new GetClassVariableInstr(ret, classVarDefinitionContainer(), node.getName()));
        return ret;
    }

    // Add the specified result instruction to the scope and return its result variable.
    private Variable addResultInstr(ResultInstr instr) {
        addInstr((Instr) instr);

        return instr.getResult();
    }

    // ClassVarAsgn node is assignment within a method/closure scope
    //
    // def foo
    //   @@c = 1
    // end
    public Operand buildClassVarAsgn(final ClassVarAsgnNode classVarAsgnNode) {
        Operand val = build(classVarAsgnNode.getValueNode());
        addInstr(new PutClassVariableInstr(classVarDefinitionContainer(), classVarAsgnNode.getName(), val));
        return val;
    }

    @Deprecated
    public Operand classVarDeclarationContainer() {
        return classVarContainer(true);
    }

    public Operand classVarDefinitionContainer() {
        return classVarContainer(false);
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
            return addResultInstr(new GetClassVarContainerModuleInstr(createTemporaryVariable(),
                    CurrentScope.INSTANCE, declContext ? null : buildSelf()));
        }
    }

    public Operand buildConstDecl(ConstDeclNode node) {
        return buildConstDeclAssignment(node, build(node.getValueNode()));
    }

    private Operand findContainerModule() {
        int nearestModuleBodyDepth = scope.getNearestModuleReferencingScopeDepth();
        return (nearestModuleBodyDepth == -1) ? getCurrentModuleVariable() : ScopeModule.ModuleFor(nearestModuleBodyDepth);
    }

    public Operand buildConstDeclAssignment(ConstDeclNode constDeclNode, Operand value) {
        Node constNode = constDeclNode.getConstNode();

        if (constNode == null) {
            return putConstant(constDeclNode.getName(), value);
        } else if (constNode.getNodeType() == NodeType.COLON2NODE) {
            return putConstant((Colon2Node) constNode, value);
        } else { // colon3, assign in Object
            return putConstant((Colon3Node) constNode, value);
        }
    }

    private Operand putConstant(RubySymbol name, Operand value) {
        addInstr(new PutConstInstr(findContainerModule(), name, value));

        return value;
    }

    private Operand putConstant(Colon3Node node, Operand value) {
        addInstr(new PutConstInstr(new ObjectClass(), node.getName(), value));

        return value;
    }

    private Operand putConstant(Colon2Node node, Operand value) {
        addInstr(new PutConstInstr(build(node.getLeftNode()), node.getName(), value));

        return value;
    }

    private Operand putConstantAssignment(OpAsgnConstDeclNode node, Operand value) {
        Node constNode = node.getFirstNode();

        if (constNode instanceof Colon2Node) return putConstant((Colon2Node) constNode, value);

        return putConstant((Colon3Node) constNode, value);
    }

    private Operand searchModuleForConst(Operand startingModule, RubySymbol name) {
        return addResultInstr(new SearchModuleForConstInstr(createTemporaryVariable(), startingModule, name, true));
    }

    private Operand searchConst(RubySymbol name) {
        return addResultInstr(new SearchConstInstr(createTemporaryVariable(), name, CurrentScope.INSTANCE, false));
    }

    public Operand buildColon2(final Colon2Node colon2) {
        Node lhs = colon2.getLeftNode();

        // Colon2ImplicitNode - (module|class) Foo.  Weird, but it is a wrinkle of AST inheritance.
        if (lhs == null) return searchConst(colon2.getName());

        // Colon2ConstNode (Left::name)
        return searchModuleForConst(build(lhs), colon2.getName());
    }

    public Operand buildColon3(Colon3Node node) {
        return searchModuleForConst(new ObjectClass(), node.getName());
    }

    public Operand buildComplex(ComplexNode node) {
        return new Complex((ImmutableLiteral) build(node.getNumber()));
    }

    interface CodeBlock {
        Operand run();
    }

    interface VoidCodeBlock {
        void run();
    }

    private Operand protectCodeWithRescue(CodeBlock protectedCode, CodeBlock rescueBlock) {
        // This effectively mimics a begin-rescue-end code block
        // Except this catches all exceptions raised by the protected code

        Variable rv = createTemporaryVariable();
        Label rBeginLabel = getNewLabel();
        Label rEndLabel   = getNewLabel();
        Label rescueLabel = getNewLabel();

        // Protected region code
        addInstr(new LabelInstr(rBeginLabel));
        addInstr(new ExceptionRegionStartMarkerInstr(rescueLabel));
        Object v1 = protectedCode.run(); // YIELD: Run the protected code block
        addInstr(new CopyInstr(rv, (Operand)v1));
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
        Variable exc = createTemporaryVariable();
        Variable excType = createTemporaryVariable();

        // Receive 'exc' and verify that 'exc' is of ruby-type 'Exception'
        addInstr(new LabelInstr(rescueLabel));
        addInstr(new ReceiveRubyExceptionInstr(exc));
        addInstr(new InheritanceSearchConstInstr(excType, new ObjectClass(),
                manager.runtime.newSymbol(CommonByteLists.EXCEPTION)));
        outputExceptionCheck(excType, exc, caughtLabel);

        // Fall-through when the exc !== Exception; rethrow 'exc'
        addInstr(new ThrowExceptionInstr(exc));

        // exc === Exception; Run the rescue block
        addInstr(new LabelInstr(caughtLabel));
        Object v2 = rescueBlock.run(); // YIELD: Run the protected code block
        if (v2 != null) addInstr(new CopyInstr(rv, manager.getNil()));

        // End
        addInstr(new LabelInstr(rEndLabel));

        return rv;
    }

    public Operand buildGetDefinition(Node node) {
        if (node == null) return new FrozenString("expression");

        switch (node.getNodeType()) {
        case CLASSVARASGNNODE: case CLASSVARDECLNODE: case CONSTDECLNODE:
        case DASGNNODE: case GLOBALASGNNODE: case LOCALASGNNODE:
        case MULTIPLEASGNNODE: case OPASGNNODE: case OPASGNANDNODE: case OPASGNORNODE:
        case OPELEMENTASGNNODE: case INSTASGNNODE:
            return new FrozenString(DefinedMessage.ASSIGNMENT.getText());
        case ORNODE: case ANDNODE: case DREGEXPNODE: case DSTRNODE:
            return new FrozenString(DefinedMessage.EXPRESSION.getText());
        case FALSENODE:
            return new FrozenString(DefinedMessage.FALSE.getText());
        case LOCALVARNODE: case DVARNODE:
            return new FrozenString(DefinedMessage.LOCAL_VARIABLE.getText());
        case MATCH2NODE: case MATCH3NODE:
            return new FrozenString(DefinedMessage.METHOD.getText());
        case NILNODE:
            return new FrozenString(DefinedMessage.NIL.getText());
        case SELFNODE:
            return new FrozenString(DefinedMessage.SELF.getText());
        case TRUENODE:
            return new FrozenString(DefinedMessage.TRUE.getText());
        case ARRAYNODE: { // If all elts of array are defined the array is as well
            ArrayNode array = (ArrayNode) node;
            Label undefLabel = getNewLabel();
            Label doneLabel = getNewLabel();

            Variable tmpVar = createTemporaryVariable();
            for (Node elt: array.children()) {
                Operand result = buildGetDefinition(elt);

                addInstr(createBranch(result, manager.getNil(), undefLabel));
            }

            addInstr(new CopyInstr(tmpVar, new FrozenString(DefinedMessage.EXPRESSION.getText())));
            addInstr(new JumpInstr(doneLabel));
            addInstr(new LabelInstr(undefLabel));
            addInstr(new CopyInstr(tmpVar, manager.getNil()));
            addInstr(new LabelInstr(doneLabel));

            return tmpVar;
        }
        case BACKREFNODE:
            return addResultInstr(
                    new RuntimeHelperCall(
                            createTemporaryVariable(),
                            IS_DEFINED_BACKREF,
                            new Operand[] {new FrozenString(DefinedMessage.GLOBAL_VARIABLE.getText())}
                    )
            );
        case GLOBALVARNODE:
            return addResultInstr(
                    new RuntimeHelperCall(
                            createTemporaryVariable(),
                            IS_DEFINED_GLOBAL,
                            new Operand[] {
                                    new FrozenString(((GlobalVarNode) node).getName()),
                                    new FrozenString(DefinedMessage.GLOBAL_VARIABLE.getText())
                            }
                    )
            );
        case NTHREFNODE: {
            return addResultInstr(
                    new RuntimeHelperCall(
                            createTemporaryVariable(),
                            IS_DEFINED_NTH_REF,
                            new Operand[] {
                                    manager.newFixnum(((NthRefNode) node).getMatchNumber()),
                                    new FrozenString(DefinedMessage.GLOBAL_VARIABLE.getText())
                            }
                    )
            );
        }
        case INSTVARNODE:
            return addResultInstr(
                    new RuntimeHelperCall(
                            createTemporaryVariable(),
                            IS_DEFINED_INSTANCE_VAR,
                            new Operand[] {
                                    buildSelf(),
                                    new FrozenString(((InstVarNode) node).getName()),
                                    new FrozenString(DefinedMessage.INSTANCE_VARIABLE.getText())
                            }
                    )
            );
        case CLASSVARNODE:
            return addResultInstr(
                    new RuntimeHelperCall(
                            createTemporaryVariable(),
                            IS_DEFINED_CLASS_VAR,
                            new Operand[]{
                                    classVarDefinitionContainer(),
                                    new FrozenString(((ClassVarNode) node).getName()),
                                    new FrozenString(DefinedMessage.CLASS_VARIABLE.getText())
                            }
                    )
            );
        case SUPERNODE: {
            Label undefLabel = getNewLabel();
            Variable tmpVar  = addResultInstr(
                    new RuntimeHelperCall(
                            createTemporaryVariable(),
                            IS_DEFINED_SUPER,
                            new Operand[] {
                                    buildSelf(),
                                    new FrozenString(DefinedMessage.SUPER.getText())
                            }
                    )
            );
            addInstr(createBranch(tmpVar, manager.getNil(), undefLabel));
            Operand superDefnVal = buildGetArgumentDefinition(((SuperNode) node).getArgsNode(), DefinedMessage.SUPER.getText());
            return buildDefnCheckIfThenPaths(undefLabel, superDefnVal);
        }
        case VCALLNODE:
            return addResultInstr(
                    new RuntimeHelperCall(
                            createTemporaryVariable(),
                            IS_DEFINED_METHOD,
                            new Operand[] {
                                    buildSelf(),
                                    new FrozenString(((VCallNode) node).getName()),
                                    manager.getFalse(),
                                    new FrozenString(DefinedMessage.METHOD.getText())
                            }
                    )
            );
        case YIELDNODE:
            return buildDefinitionCheck(new BlockGivenInstr(createTemporaryVariable(), getYieldClosureVariable()), DefinedMessage.YIELD.getText());
        case ZSUPERNODE:
            return addResultInstr(
                    new RuntimeHelperCall(
                            createTemporaryVariable(),
                            IS_DEFINED_SUPER,
                            new Operand[] {
                                    buildSelf(),
                                    new FrozenString(DefinedMessage.SUPER.getText())
                            }
                    )
            );
        case CONSTNODE: {
            Label defLabel = getNewLabel();
            Label doneLabel = getNewLabel();
            Variable tmpVar  = createTemporaryVariable();
            RubySymbol constName = ((ConstNode) node).getName();
            addInstr(new LexicalSearchConstInstr(tmpVar, CurrentScope.INSTANCE, constName));
            addInstr(BNEInstr.create(defLabel, tmpVar, UndefinedValue.UNDEFINED));
            addInstr(new InheritanceSearchConstInstr(tmpVar, findContainerModule(), constName)); // SSS FIXME: should this be the current-module var or something else?
            addInstr(BNEInstr.create(defLabel, tmpVar, UndefinedValue.UNDEFINED));
            addInstr(new CopyInstr(tmpVar, manager.getNil()));
            addInstr(new JumpInstr(doneLabel));
            addInstr(new LabelInstr(defLabel));
            addInstr(new CopyInstr(tmpVar, new FrozenString(DefinedMessage.CONSTANT.getText())));
            addInstr(new LabelInstr(doneLabel));
            return tmpVar;
        }
        case COLON3NODE: case COLON2NODE: {
            // SSS FIXME: Is there a reason to do this all with low-level IR?
            // Can't this all be folded into a Java method that would be part
            // of the runtime library, which then can be used by buildDefinitionCheck method above?
            // This runtime library would be used both by the interpreter & the compiled code!

            final Colon3Node colon = (Colon3Node) node;
            final RubySymbol name = colon.getName();
            final Variable errInfo = createTemporaryVariable();

            // store previous exception for restoration if we rescue something
            addInstr(new GetErrorInfoInstr(errInfo));

            CodeBlock protectedCode = new CodeBlock() {
                public Operand run() {
                    if (!(colon instanceof Colon2Node)) { // colon3 (weird inheritance)
                        return addResultInstr(
                                new RuntimeHelperCall(
                                        createTemporaryVariable(),
                                        IS_DEFINED_CONSTANT_OR_METHOD,
                                        new Operand[] {
                                                new ObjectClass(),
                                                new FrozenString(name),
                                                new FrozenString(DefinedMessage.CONSTANT.getText()),
                                                new FrozenString(DefinedMessage.METHOD.getText())
                                        }
                                )
                        );
                    }

                    Label bad = getNewLabel();
                    Label done = getNewLabel();
                    Variable result = createTemporaryVariable();
                    Operand test = buildGetDefinition(((Colon2Node) colon).getLeftNode());
                    addInstr(createBranch(test, manager.getNil(), bad));
                    Operand lhs = build(((Colon2Node) colon).getLeftNode());
                    addInstr(
                            new RuntimeHelperCall(
                                    result,
                                    IS_DEFINED_CONSTANT_OR_METHOD,
                                    new Operand[] {
                                            lhs,
                                            new FrozenString(name),
                                            new FrozenString(DefinedMessage.CONSTANT.getText()),
                                            new FrozenString(DefinedMessage.METHOD.getText())
                                    }
                            )
                    );
                    addInstr(new JumpInstr(done));
                    addInstr(new LabelInstr(bad));
                    addInstr(new CopyInstr(result, manager.getNil()));
                    addInstr(new LabelInstr(done));

                    return result;
                }
            };

            // rescue block
            CodeBlock rescueBlock = new CodeBlock() {
                 public Operand run() {
                 // Nothing to do -- ignore the exception, and restore stashed error info!
                 addInstr(new RestoreErrorInfoInstr(errInfo));
                 return manager.getNil();
                 }
            };

            // Try verifying definition, and if we get an JumpException exception, process it with the rescue block above
            return protectCodeWithRescue(protectedCode, rescueBlock);
        }
        case FCALLNODE: {
            /* ------------------------------------------------------------------
             * Generate IR for:
             *    r = self/receiver
             *    mc = r.metaclass
             *    return mc.methodBound(meth) ? buildGetArgumentDefn(..) : false
             * ----------------------------------------------------------------- */
            Label undefLabel = getNewLabel();
            Variable tmpVar = addResultInstr(
                    new RuntimeHelperCall(
                            createTemporaryVariable(),
                            IS_DEFINED_METHOD,
                            new Operand[]{
                                    buildSelf(),
                                    new Symbol(((FCallNode) node).getName()),
                                    manager.getFalse(),
                                    new FrozenString(DefinedMessage.METHOD.getText())
                            }
                    )
            );
            addInstr(createBranch(tmpVar, manager.getNil(), undefLabel));
            Operand argsCheckDefn = buildGetArgumentDefinition(((FCallNode) node).getArgsNode(), "method");
            return buildDefnCheckIfThenPaths(undefLabel, argsCheckDefn);
        }
        case CALLNODE: {
            final CallNode callNode = (CallNode) node;

            // protected main block
            CodeBlock protectedCode = new CodeBlock() {
                public Operand run() {
                    final Label undefLabel = getNewLabel();
                    Operand receiverDefn = buildGetDefinition(callNode.getReceiverNode());
                    addInstr(createBranch(receiverDefn, manager.getNil(), undefLabel));
                    Variable tmpVar = createTemporaryVariable();
                    addInstr(
                            new RuntimeHelperCall(
                                    tmpVar,
                                    IS_DEFINED_CALL,
                                    new Operand[]{
                                            build(callNode.getReceiverNode()),
                                            new Symbol(callNode.getName()),
                                            new FrozenString(DefinedMessage.METHOD.getText())
                                    }
                            )
                    );
                    return buildDefnCheckIfThenPaths(undefLabel, tmpVar);
                }
            };

            // rescue block
            CodeBlock rescueBlock = new CodeBlock() {
                public Operand run() { return manager.getNil(); } // Nothing to do if we got an exception
            };

            // Try verifying definition, and if we get an exception, throw it out, and return nil
            return protectCodeWithRescue(protectedCode, rescueBlock);
        }
        case ATTRASSIGNNODE: {
            final AttrAssignNode attrAssign = (AttrAssignNode) node;

            // protected main block
            CodeBlock protectedCode = new CodeBlock() {
                public Operand run() {
                    final Label  undefLabel = getNewLabel();
                    Operand receiverDefn = buildGetDefinition(attrAssign.getReceiverNode());
                    addInstr(createBranch(receiverDefn, manager.getNil(), undefLabel));
                    /* --------------------------------------------------------------------------
                     * This basically combines checks from CALLNODE and FCALLNODE
                     *
                     * Generate IR for this sequence
                     *
                     *    1. r  = receiver
                     *    2. mc = r.metaClass
                     *    3. v  = mc.getVisibility(methodName)
                     *    4. f  = !v || v.isPrivate? || (v.isProtected? && receiver/self?.kindof(mc.getRealClass))
                     *    5. return !f && mc.methodBound(attrmethod) ? buildGetArgumentDefn(..) : false
                     *
                     * Hide the complexity of instrs 2-4 into a verifyMethodIsPublicAccessible call
                     * which can executely entirely in Java-land.  No reason to expose the guts in IR.
                     * ------------------------------------------------------------------------------ */
                    Variable tmpVar     = createTemporaryVariable();
                    Operand  receiver   = build(attrAssign.getReceiverNode());
                    addInstr(
                            new RuntimeHelperCall(
                                    tmpVar,
                                    IS_DEFINED_METHOD,
                                    new Operand[] {
                                            receiver,
                                            new Symbol(attrAssign.getName()),
                                            manager.getTrue(),
                                            new FrozenString(DefinedMessage.METHOD.getText())
                                    }
                            )
                    );
                    addInstr(createBranch(tmpVar, manager.getNil(), undefLabel));
                    Operand argsCheckDefn = buildGetArgumentDefinition(attrAssign.getArgsNode(), "assignment");
                    return buildDefnCheckIfThenPaths(undefLabel, argsCheckDefn);
                }
            };

            // rescue block
            CodeBlock rescueBlock = new CodeBlock() {
                public Operand run() { return manager.getNil(); } // Nothing to do if we got an exception
            };

            // Try verifying definition, and if we get an JumpException exception, process it with the rescue block above
            return protectCodeWithRescue(protectedCode, rescueBlock);
        }
        default:
            return new FrozenString("expression");
        }
    }

    protected Variable buildDefnCheckIfThenPaths(Label undefLabel, Operand defVal) {
        Label defLabel = getNewLabel();
        Variable tmpVar = getValueInTemporaryVariable(defVal);
        addInstr(new JumpInstr(defLabel));
        addInstr(new LabelInstr(undefLabel));
        addInstr(new CopyInstr(tmpVar, manager.getNil()));
        addInstr(new LabelInstr(defLabel));
        return tmpVar;
    }

    protected Variable buildDefinitionCheck(ResultInstr definedInstr, String definedReturnValue) {
        Label undefLabel = getNewLabel();
        addInstr((Instr) definedInstr);
        addInstr(createBranch(definedInstr.getResult(), manager.getFalse(), undefLabel));
        return buildDefnCheckIfThenPaths(undefLabel, new FrozenString(definedReturnValue));
    }

    public Operand buildGetArgumentDefinition(final Node node, String type) {
        if (node == null) return new MutableString(type);

        Operand rv = new FrozenString(type);
        boolean failPathReqd = false;
        Label failLabel = getNewLabel();
        if (node instanceof ArrayNode) {
            for (int i = 0; i < ((ArrayNode) node).size(); i++) {
                Node iterNode = ((ArrayNode) node).get(i);
                Operand def = buildGetDefinition(iterNode);
                if (def == manager.getNil()) { // Optimization!
                    rv = manager.getNil();
                    break;
                } else if (!def.hasKnownValue()) { // Optimization!
                    failPathReqd = true;
                    addInstr(createBranch(def, manager.getNil(), failLabel));
                }
            }
        } else {
            Operand def = buildGetDefinition(node);
            if (def == manager.getNil()) { // Optimization!
                rv = manager.getNil();
            } else if (!def.hasKnownValue()) { // Optimization!
                failPathReqd = true;
                addInstr(createBranch(def, manager.getNil(), failLabel));
            }
        }

        // Optimization!
        return failPathReqd ? buildDefnCheckIfThenPaths(failLabel, rv) : rv;

    }

    public Operand buildDAsgn(final DAsgnNode dasgnNode) {
        // SSS: Looks like we receive the arg in buildBlockArgsAssignment via the IterNode
        // We won't get here for argument receives!  So, buildDasgn is called for
        // assignments to block variables within a block.  As far as the IR is concerned,
        // this is just a simple copy
        int depth = dasgnNode.getDepth();
        Variable arg = getLocalVariable(dasgnNode.getName(), depth);
        Operand  value = build(dasgnNode.getValueNode());

        // no use copying a variable to itself
        if (arg == value) return value;

        addInstr(new CopyInstr(arg, value));

        return value;

        // IMPORTANT: The return value of this method is value, not arg!
        //
        // Consider this Ruby code: foo((a = 1), (a = 2))
        //
        // If we return 'value' this will get translated to:
        //    a = 1
        //    a = 2
        //    call("foo", [1,2]) <---- CORRECT
        //
        // If we return 'arg' this will get translated to:
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

    // Called by defineMethod but called on a new builder so things like ensure block info recording
    // do not get confused.
    protected InterpreterContext defineMethodInner(DefNode defNode, IRScope parent, int coverageMode) {
        this.coverageMode = coverageMode;

        if (RubyInstanceConfig.FULL_TRACE_ENABLED) {
            // Explicit line number here because we need a line number for trace before we process any nodes
            addInstr(manager.newLineNumber(scope.getLine() + 1));
            addInstr(new TraceInstr(RubyEvent.CALL, getName(), getFileName(), scope.getLine() + 1));
        }

        prepareImplicitState();                                    // recv_self, add frame block, etc)

        // These instructions need to be toward the top of the method because they may both be needed for processing
        // optional arguments as in def foo(a = Object).
        // Set %current_module = isInstanceMethod ? %self.metaclass : %self
        int nearestScopeDepth = parent.getNearestModuleReferencingScopeDepth();
        addInstr(new CopyInstr(getCurrentModuleVariable(), ScopeModule.ModuleFor(nearestScopeDepth == -1 ? 1 : nearestScopeDepth)));

        // Build IR for arguments (including the block arg)
        receiveMethodArgs(defNode.getArgsNode());

        // Build IR for body
        Operand rv = build(defNode.getBodyNode());

        if (RubyInstanceConfig.FULL_TRACE_ENABLED) {
            addInstr(new LineNumberInstr(defNode.getEndLine() + 1));
            addInstr(new TraceInstr(RubyEvent.RETURN, getName(), getFileName(), defNode.getEndLine() + 1));
        }

        if (rv != null) addInstr(new ReturnInstr(rv));

        // We do an extra early one so we can look for non-local returns.
        computeScopeFlagsFrom(instructions);

        // If the method can receive non-local returns
        if (scope.canReceiveNonlocalReturns()) handleNonlocalReturnInMethod();

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

        ((IRMethod) scope).setArgumentDescriptors(argDesc);

        computeScopeFlagsFrom(instructions);

        return scope.allocateInterpreterContext(instructions, temporaryVariableIndex + 1, flags);
    }

    private IRMethod defineNewMethod(MethodDefNode defNode, boolean isInstanceMethod) {
        IRMethod method = new IRMethod(manager, scope, defNode, defNode.getName().getBytes(), isInstanceMethod, defNode.getLine(),
                defNode.getScope(), coverageMode);

        // poorly placed next/break expects a syntax error so we eagerly build methods which contain them.
        if (defNode.containsBreakNext()) method.lazilyAcquireInterpreterContext();

        return method;
    }

    public Operand buildDefn(MethodDefNode node) { // Instance method
        IRMethod method = defineNewMethod(node, true);
        addInstr(new DefineInstanceMethodInstr(method));
        return new Symbol(node.getName());
    }

    public Operand buildDefs(DefsNode node) { // Class method
        Operand container =  build(node.getReceiverNode());
        IRMethod method = defineNewMethod(node, false);
        addInstr(new DefineClassMethodInstr(container, method));
        return new Symbol(node.getName());
    }

    protected LocalVariable getArgVariable(RubySymbol name, int depth) {
        // For non-loops, this name will override any name that exists in outer scopes
        return scope instanceof IRFor ? getLocalVariable(name, depth) : getNewLocalVariable(name, 0);
    }

    private void addArgReceiveInstr(Variable v, int argIndex, Signature signature) {
        boolean post = signature != null;

        if (post) {
            addInstr(new ReceivePostReqdArgInstr(v, argIndex, signature.pre(), signature.opt(), signature.hasRest(), signature.post()));
        } else {
            addInstr(new ReceivePreReqdArgInstr(v, argIndex));
        }
    }

    /* '_' can be seen as a variable only by its first assignment as a local variable.  For any additional
     * '_' we create temporary variables in the case the scope has a zsuper in it.  If so, then the zsuper
     * call will slurp those temps up as it's parameters so it can properly set up the call.
     */
    private Variable argumentResult(RubySymbol name) {
        boolean isUnderscore = name.getBytes().realSize() == 1 && name.getBytes().charAt(0) == '_';

        if (isUnderscore && underscoreVariableSeen) {
            return createTemporaryVariable();
        } else {
            if (isUnderscore) underscoreVariableSeen = true;
            return getNewLocalVariable(name, 0);
        }
    }

    public void receiveRequiredArg(Node node, int argIndex, Signature signature) {
        switch (node.getNodeType()) {
            case ARGUMENTNODE: {
                RubySymbol argName = ((ArgumentNode)node).getName();

                if (scope instanceof IRMethod) addArgumentDescription(ArgumentType.req, argName);

                addArgReceiveInstr(argumentResult(argName), argIndex, signature);
                break;
            }
            case MULTIPLEASGNNODE: {
                MultipleAsgnNode childNode = (MultipleAsgnNode) node;
                Variable v = createTemporaryVariable();
                addArgReceiveInstr(v, argIndex, signature);
                if (scope instanceof IRMethod) addArgumentDescription(ArgumentType.anonreq, null);
                Variable tmp = createTemporaryVariable();
                addInstr(new ToAryInstr(tmp, v));
                buildMultipleAsgn19Assignment(childNode, tmp, null);
                break;
            }
            default: throw notCompilable("Can't build assignment node", node);
        }
    }

    protected void receiveNonBlockArgs(final ArgsNode argsNode) {
        Signature signature = scope.getStaticScope().getSignature();

        // For closures, we don't need the check arity call
        if (scope instanceof IRMethod) {
            // Expensive to do this explicitly?  But, two advantages:
            // (a) on inlining, we'll be able to get rid of these checks in almost every case.
            // (b) compiler to bytecode will anyway generate this and this is explicit.
            // For now, we are going explicit instruction route.
            // But later, perhaps can make this implicit in the method setup preamble?

            addInstr(new CheckArityInstr(signature.required(), signature.opt(), signature.hasRest(), argsNode.hasKwargs(),
                    signature.keyRest()));
        } else if (scope instanceof IRClosure && argsNode.hasKwargs()) {
            // FIXME: This is added to check for kwargs correctness but bypass regular correctness.
            // Any other arity checking currently happens within Java code somewhere (RubyProc.call?)
            addInstr(new CheckArityInstr(signature.required(), signature.opt(), signature.hasRest(), argsNode.hasKwargs(),
                    signature.keyRest()));
        }

        // Other args begin at index 0
        int argIndex = 0;

        // Pre(-opt and rest) required args
        Node[] args = argsNode.getArgs();
        int preCount = signature.pre();
        for (int i = 0; i < preCount; i++, argIndex++) {
            receiveRequiredArg(args[i], argIndex, null);
        }

        // Fixup opt/rest
        int opt = signature.opt() > 0 ? signature.opt() : 0;

        // Now for opt args
        if (opt > 0) {
            int optIndex = argsNode.getOptArgIndex();
            for (int j = 0; j < opt; j++, argIndex++) {
                // We fall through or jump to variableAssigned once we know we have a valid value in place.
                Label variableAssigned = getNewLabel();
                OptArgNode optArg = (OptArgNode)args[optIndex + j];
                RubySymbol argName = optArg.getName();
                Variable argVar = argumentResult(argName);
                if (scope instanceof IRMethod) addArgumentDescription(ArgumentType.opt, argName);
                // You need at least required+j+1 incoming args for this opt arg to get an arg at all
                addInstr(new ReceiveOptArgInstr(argVar, signature.required(), signature.pre(), j));
                addInstr(BNEInstr.create(variableAssigned, argVar, UndefinedValue.UNDEFINED));
                // We add this extra nil copy because we do not know if we have a circular defininition of
                // argVar: proc { |a=a| } or proc { |a = foo(bar(a))| }.
                addInstr(new CopyInstr(argVar, manager.getNil()));
                // This bare build looks weird but OptArgNode is just a marker and value is either a LAsgnNode
                // or a DAsgnNode.  So building the value will end up having a copy(var, assignment).
                build(optArg.getValue());
                addInstr(new LabelInstr(variableAssigned));
            }
        }

        // Rest arg
        if (signature.hasRest()) {
            // Consider: def foo(*); .. ; end
            // For this code, there is no argument name available from the ruby code.
            // So, we generate an implicit arg name
            RestArgNode restArgNode = argsNode.getRestArgNode();
            if (scope instanceof IRMethod) {
                addArgumentDescription(restArgNode.isAnonymous() ?
                        ArgumentType.anonrest : ArgumentType.rest, restArgNode.getName());
            }

            RubySymbol argName =  restArgNode.isAnonymous() ?
                    scope.getManager().getRuntime().newSymbol(CommonByteLists.STAR) : restArgNode.getName();

            // You need at least required+opt+1 incoming args for the rest arg to get any args at all
            // If it is going to get something, then it should ignore required+opt args from the beginning
            // because they have been accounted for already.
            addInstr(new ReceiveRestArgInstr(argumentResult(argName), signature.required() + opt, argIndex));
        }

        // Post(-opt and rest) required args
        int postCount = argsNode.getPostCount();
        int postIndex = argsNode.getPostIndex();
        for (int i = 0; i < postCount; i++) {
            receiveRequiredArg(args[postIndex + i], i, signature);
        }
    }

    /**
     * Reify the implicit incoming block into a full Proc, for use as "block arg", but only if
     * a block arg is specified in this scope's arguments.
     *  @param argsNode the arguments containing the block arg, if any
     *
     */
    protected void receiveBlockArg(final ArgsNode argsNode) {
        // reify to Proc if we have a block arg
        BlockArgNode blockArg = argsNode.getBlock();
        if (blockArg != null) {
            RubySymbol argName = blockArg.getName();
            Variable blockVar = argumentResult(argName);
            if (scope instanceof IRMethod) addArgumentDescription(ArgumentType.block, argName);
            Variable tmp = createTemporaryVariable();
            addInstr(new LoadImplicitClosureInstr(tmp));
            addInstr(new ReifyClosureInstr(blockVar, tmp));
        }
    }

    /**
     * Prepare implicit runtime state needed for typical methods to execute. This includes such things
     * as the implicit self variable and any yieldable block available to this scope.
     */
    private void prepareImplicitState() {
        // Receive self
        addInstr(manager.getReceiveSelfInstr());

        // used for yields; metaclass body (sclass) inherits yield var from surrounding, and accesses it as implicit
        if (scope instanceof IRMethod || scope instanceof IRMetaClassBody) {
            addInstr(new LoadImplicitClosureInstr(getYieldClosureVariable()));
        } else {
            addInstr(new LoadFrameClosureInstr(getYieldClosureVariable()));
        }
    }

    /**
     * Prepare closure runtime state. This includes the implicit self variable and setting up a variable to hold any
     * frame closure if it is needed later.
     */
    private void prepareClosureImplicitState() {
        // Receive self
        addInstr(manager.getReceiveSelfInstr());
    }

    /**
     * Process all arguments specified for this scope. This includes pre args (required args
     * at the beginning of the argument list), opt args (arguments with a default value),
     * a rest arg (catch-all for argument list overflow), post args (required arguments after
     * a rest arg) and a block arg (to reify an incoming block into a Proc object.
     *  @param argsNode the args node containing the specification for the arguments
     *
     */
    public void receiveArgs(final ArgsNode argsNode) {
        // 1.9 pre, opt, rest, post args
        receiveNonBlockArgs(argsNode);

        // 2.0 keyword args
        Node[] args = argsNode.getArgs();
        int required = argsNode.getRequiredArgsCount();
        if (argsNode.hasKwargs()) {
            int keywordIndex = argsNode.getKeywordsIndex();
            int keywordsCount = argsNode.getKeywordCount();
            for (int i = 0; i < keywordsCount; i++) {
                KeywordArgNode kwarg = (KeywordArgNode) args[keywordIndex + i];
                AssignableNode kasgn = kwarg.getAssignable();
                RubySymbol key = ((INameNode) kasgn).getName();
                Variable av = getNewLocalVariable(key, 0);
                Label l = getNewLabel();
                if (scope instanceof IRMethod) addKeyArgDesc(kasgn, key);
                addInstr(new ReceiveKeywordArgInstr(av, key, required));
                addInstr(BNEInstr.create(l, av, UndefinedValue.UNDEFINED)); // if 'av' is not undefined, we are done

                // Required kwargs have no value and check_arity will throw if they are not provided.
                if (!isRequiredKeywordArgumentValue(kasgn)) {
                    addInstr(new CopyInstr(av, buildNil())); // wipe out undefined value with nil
                    build(kasgn);
                } else {
                    addInstr(new RaiseRequiredKeywordArgumentError(key));
                }
                addInstr(new LabelInstr(l));
            }
        }

        // 2.0 keyword rest arg
        KeywordRestArgNode keyRest = argsNode.getKeyRest();
        if (keyRest != null) {
            RubySymbol key = keyRest.getName();
            ArgumentType type = ArgumentType.keyrest;

            // anonymous keyrest
            if (key == null || key.getBytes().realSize() == 0) type = ArgumentType.anonkeyrest;

            Variable av = getNewLocalVariable(key, 0);
            if (scope instanceof IRMethod) addArgumentDescription(type, key);
            addInstr(new ReceiveKeywordRestArgInstr(av, required));
        }

        // Block arg
        receiveBlockArg(argsNode);
    }

    private void addKeyArgDesc(AssignableNode kasgn, RubySymbol key) {
        if (isRequiredKeywordArgumentValue(kasgn)) {
            addArgumentDescription(ArgumentType.keyreq, key);
        } else {
            addArgumentDescription(ArgumentType.key, key);
        }
    }

    private boolean isRequiredKeywordArgumentValue(AssignableNode kasgn) {
        return (kasgn.getValueNode().getNodeType()) ==  NodeType.REQUIRED_KEYWORD_ARGUMENT_VALUE;
    }

    // This method is called to build arguments
    public void buildArgsMasgn(Node node, Operand argsArray, boolean isMasgnRoot, int preArgsCount, int postArgsCount, int index, boolean isSplat) {
        Variable v;
        switch (node.getNodeType()) {
            case DASGNNODE: {
                DAsgnNode dynamicAsgn = (DAsgnNode) node;
                v = getArgVariable(dynamicAsgn.getName(), dynamicAsgn.getDepth());
                if (isSplat) addInstr(new RestArgMultipleAsgnInstr(v, argsArray, preArgsCount, postArgsCount, index));
                else addInstr(new ReqdArgMultipleAsgnInstr(v, argsArray, preArgsCount, postArgsCount, index));
                break;
            }
            case LOCALASGNNODE: {
                LocalAsgnNode localVariable = (LocalAsgnNode) node;
                v = getArgVariable(localVariable.getName(), localVariable.getDepth());
                if (isSplat) addInstr(new RestArgMultipleAsgnInstr(v, argsArray, preArgsCount, postArgsCount, index));
                else addInstr(new ReqdArgMultipleAsgnInstr(v, argsArray, preArgsCount, postArgsCount, index));
                break;
            }
            case MULTIPLEASGNNODE: {
                MultipleAsgnNode childNode = (MultipleAsgnNode) node;
                if (!isMasgnRoot) {
                    v = createTemporaryVariable();
                    if (isSplat) addInstr(new RestArgMultipleAsgnInstr(v, argsArray, preArgsCount, postArgsCount, index));
                    else addInstr(new ReqdArgMultipleAsgnInstr(v, argsArray, preArgsCount, postArgsCount, index));
                    Variable tmp = createTemporaryVariable();
                    addInstr(new ToAryInstr(tmp, v));
                    argsArray = tmp;
                }
                // Build
                buildMultipleAsgn19Assignment(childNode, argsArray, null);
                break;
            }
            default:
                throw notCompilable("Shouldn't get here", node);
        }
    }

    // This method is called both for regular multiple assignment as well as argument passing
    //
    // Ex: a,b,*c=v  is a regular assignment and in this case, the "values" operand will be non-null
    // Ex: { |a,b,*c| ..} is the argument passing case
    public void buildMultipleAsgn19Assignment(final MultipleAsgnNode multipleAsgnNode, Operand argsArray, Operand values) {
        final ListNode masgnPre = multipleAsgnNode.getPre();
        final List<Tuple<Node, Variable>> assigns = new ArrayList<>();

        // Build assignments for specific named arguments
        int i = 0;
        if (masgnPre != null) {
            for (Node an: masgnPre.children()) {
                if (values == null) {
                    buildArgsMasgn(an, argsArray, false, -1, -1, i, false);
                } else {
                    Variable rhsVal = createTemporaryVariable();
                    addInstr(new ReqdArgMultipleAsgnInstr(rhsVal, values, i));
                    assigns.add(new Tuple<>(an, rhsVal));
                }
                i++;
            }
        }

        // Build an assignment for a splat, if any, with the rest of the operands!
        Node restNode = multipleAsgnNode.getRest();
        int postArgsCount = multipleAsgnNode.getPostCount();
        if (restNode != null && !(restNode instanceof StarNode)) {
            if (values == null) {
                buildArgsMasgn(restNode, argsArray, false, i, postArgsCount, 0, true); // rest of the argument array!
            } else {
                Variable rhsVal = createTemporaryVariable();
                addInstr(new RestArgMultipleAsgnInstr(rhsVal, values, i, postArgsCount, 0));
                assigns.add(new Tuple<>(restNode, rhsVal)); // rest of the argument array!
            }
        }

        // Build assignments for rest of the operands
        final ListNode masgnPost = multipleAsgnNode.getPost();
        if (masgnPost != null) {
            int j = 0;
            for (Node an: masgnPost.children()) {
                if (values == null) {
                    buildArgsMasgn(an, argsArray, false, i, postArgsCount, j, false);
                } else {
                    Variable rhsVal = createTemporaryVariable();
                    addInstr(new ReqdArgMultipleAsgnInstr(rhsVal, values, i, postArgsCount, j));  // Fetch from the end
                    assigns.add(new Tuple<>(an, rhsVal));
                }
                j++;
            }
        }

        for (Tuple<Node, Variable> assign: assigns) {
            buildAssignment(assign.a, assign.b);
        }
    }

    private void handleBreakAndReturnsInLambdas() {
        Label rEndLabel   = getNewLabel();
        Label rescueLabel = Label.getGlobalEnsureBlockLabel();

        // Protect the entire body as it exists now with the global ensure block
        addInstrAtBeginning(new ExceptionRegionStartMarkerInstr(rescueLabel));
        addInstr(new ExceptionRegionEndMarkerInstr());

        // Receive exceptions (could be anything, but the handler only processes IRBreakJumps)
        addInstr(new LabelInstr(rescueLabel));
        Variable exc = createTemporaryVariable();
        addInstr(new ReceiveJRubyExceptionInstr(exc));

        // Handle break using runtime helper
        // --> IRRuntimeHelpers.handleBreakAndReturnsInLambdas(context, scope, bj, blockType)
        Variable ret = createTemporaryVariable();
        addInstr(new RuntimeHelperCall(ret, RuntimeHelperCall.Methods.HANDLE_BREAK_AND_RETURNS_IN_LAMBDA, new Operand[]{exc} ));
        addInstr(new ReturnOrRethrowSavedExcInstr(ret));

        // End
        addInstr(new LabelInstr(rEndLabel));
    }

    public void receiveMethodArgs(final ArgsNode argsNode) {
        receiveArgs(argsNode);
    }

    public void receiveBlockArgs(final IterNode node) {
        Node args = node.getVarNode();
        if (args instanceof ArgsNode) { // regular blocks
            ((IRClosure) scope).setArgumentDescriptors(Helpers.argsNodeToArgumentDescriptors(((ArgsNode) args)));
            receiveArgs((ArgsNode)args);
        } else  {
            // for loops -- reuse code in IRBuilder:buildBlockArgsAssignment
            buildBlockArgsAssignment(args, null, 0, false);
        }
    }

    public Operand buildDot(final DotNode dotNode) {
        return addResultInstr(new BuildRangeInstr(createTemporaryVariable(), build(dotNode.getBeginNode()),
                build(dotNode.getEndNode()), dotNode.isExclusive()));
    }

    private int dynamicPiece(Operand[] pieces, int i, Node pieceNode) {
        Operand piece;

        // somewhat arbitrary minimum size for interpolated values
        int estimatedSize = 4;

        while (true) { // loop to unwrap EvStr

            if (pieceNode instanceof StrNode) {
                piece = buildStrRaw((StrNode) pieceNode);
                estimatedSize = ((StrNode) pieceNode).getValue().realSize();
            } else if (pieceNode instanceof EvStrNode) {
                if (scope.maybeUsingRefinements()) {
                    // refined asString must still go through dispatch
                    TemporaryVariable result = createTemporaryVariable();
                    addInstr(new AsStringInstr(scope, result, build(((EvStrNode) pieceNode).getBody()), scope.maybeUsingRefinements()));
                    piece = result;
                } else {
                    // evstr/asstring logic lives in BuildCompoundString now, unwrap and try again
                    pieceNode = ((EvStrNode) pieceNode).getBody();
                    continue;
                }
            } else {
                piece = build(pieceNode);
            }

            break;
        }

        if (piece instanceof MutableString) {
            piece = ((MutableString)piece).frozenString;
        }

        pieces[i] = piece == null ? manager.getNil() : piece;

        return estimatedSize;
    }

    public Operand buildDRegexp(Variable result, DRegexpNode node) {
        Node[] nodePieces = node.children();
        Operand[] pieces = new Operand[nodePieces.length];

        for (int i = 0; i < pieces.length; i++) {
            // dregexp does not use estimated size
            dynamicPiece(pieces, i, nodePieces[i]);
        }

        if (result == null) result = createTemporaryVariable();
        addInstr(new BuildDynRegExpInstr(result, pieces, node.getOptions()));
        return result;
    }

    public Operand buildDStr(Variable result, DStrNode node) {
        Node[] nodePieces = node.children();
        Operand[] pieces = new Operand[nodePieces.length];
        int estimatedSize = 0;

        for (int i = 0; i < pieces.length; i++) {
            estimatedSize += dynamicPiece(pieces, i, nodePieces[i]);
        }

        if (result == null) result = createTemporaryVariable();

        boolean debuggingFrozenStringLiteral = manager.getInstanceConfig().isDebuggingFrozenStringLiteral();
        addInstr(new BuildCompoundStringInstr(result, pieces, node.getEncoding(), estimatedSize, node.isFrozen(), debuggingFrozenStringLiteral, getFileName(), node.getLine()));

        return result;
    }

    public Operand buildDSymbol(Variable result, DSymbolNode node) {
        Node[] nodePieces = node.children();
        Operand[] pieces = new Operand[nodePieces.length];
        int estimatedSize = 0;

        for (int i = 0; i < pieces.length; i++) {
            estimatedSize += dynamicPiece(pieces, i, nodePieces[i]);
        }

        if (result == null) result = createTemporaryVariable();

        boolean debuggingFrozenStringLiteral = manager.getInstanceConfig().isDebuggingFrozenStringLiteral();
        addInstr(new BuildCompoundStringInstr(result, pieces, node.getEncoding(), estimatedSize, false, debuggingFrozenStringLiteral, getFileName(), node.getLine()));

        return copyAndReturnValue(new DynamicSymbol(result));
    }

    public Operand buildDVar(DVarNode node) {
        return getLocalVariable(node.getName(), node.getDepth());
    }

    public Operand buildDXStr(Variable result, DXStrNode node) {
        Node[] nodePieces = node.children();
        Operand[] pieces = new Operand[nodePieces.length];
        int estimatedSize = 0;

        for (int i = 0; i < pieces.length; i++) {
            estimatedSize += dynamicPiece(pieces, i, nodePieces[i]);
        }

        Variable stringResult = createTemporaryVariable();
        if (result == null) result = createTemporaryVariable();

        boolean debuggingFrozenStringLiteral = manager.getInstanceConfig().isDebuggingFrozenStringLiteral();
        addInstr(new BuildCompoundStringInstr(stringResult, pieces, node.getEncoding(), estimatedSize, false, debuggingFrozenStringLiteral, getFileName(), node.getLine()));

        return addResultInstr(CallInstr.create(scope, FUNCTIONAL, result, manager.getRuntime().newSymbol("`"), Self.SELF, new Operand[] { stringResult }, null));
    }

    /* ****************************************************************
     * Consider the ensure-protected ruby code below:

           begin
             .. protected body ..
           ensure
             .. eb code
           end

       This ruby code is effectively rewritten into the following ruby code

          begin
            .. protected body ..
            .. copy of ensure body code ..
          rescue <any-exception-or-error> => e
            .. ensure body code ..
            raise e
          end

      which in IR looks like this:

          L1:
            Exception region start marker_1 (protected by L10)
            ... IR for protected body ...
            Exception region end marker_1
          L2:
            Exception region start marker_2 (protected by whichever block handles exceptions for ensure body)
            .. copy of IR for ensure block ..
            Exception region end marker_2
            jump L3
          L10:          <----- dummy rescue block
            e = recv_exception
            .. IR for ensure block ..
            throw e
          L3:

     * ****************************************************************/
    public Operand buildEnsureNode(final EnsureNode ensureNode) {
        return buildEnsureInternal(ensureNode.getBodyNode(), ensureNode.getEnsureNode());
    }

    public Operand buildEnsureInternal(Node ensureBodyNode, Node ensureNode) {
        // Save $!
        final Variable savedGlobalException = createTemporaryVariable();
        addInstr(new GetGlobalVariableInstr(savedGlobalException, symbol("$!")));

        // ------------ Build the body of the ensure block ------------
        //
        // The ensure code is built first so that when the protected body is being built,
        // the ensure code can be cloned at break/next/return sites in the protected body.

        // Push a new ensure block node onto the stack of ensure bodies being built
        // The body's instructions are stashed and emitted later.
        EnsureBlockInfo ebi = new EnsureBlockInfo(scope,
            (ensureBodyNode instanceof RescueNode) ? (RescueNode) ensureBodyNode : null,
            getCurrentLoop(),
            activeRescuers.peek());

        // Record $! save var if we had a non-empty rescue node.
        // $! will be restored from it where required.
        if (ensureBodyNode instanceof RescueNode) {
            ebi.savedGlobalException = savedGlobalException;
        }

        ensureBodyBuildStack.push(ebi);
        Operand ensureRetVal = ensureNode == null ? manager.getNil() : build(ensureNode);
        ensureBodyBuildStack.pop();

        // ------------ Build the protected region ------------
        activeEnsureBlockStack.push(ebi);

        // Start of protected region
        addInstr(new LabelInstr(ebi.regionStart));
        addInstr(new ExceptionRegionStartMarkerInstr(ebi.dummyRescueBlockLabel));
        activeRescuers.push(ebi.dummyRescueBlockLabel);

        // Generate IR for code being protected
        Variable ensureExprValue = createTemporaryVariable();
        Operand rv = ensureBodyNode instanceof RescueNode ? buildRescueInternal((RescueNode) ensureBodyNode, ebi) : build(ensureBodyNode);

        // End of protected region
        addInstr(new ExceptionRegionEndMarkerInstr());
        activeRescuers.pop();

        // Is this a begin..(rescue..)?ensure..end node that actually computes a value?
        // (vs. returning from protected body)
        boolean isEnsureExpr = ensureNode != null && rv != U_NIL && !(ensureBodyNode instanceof RescueNode);

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
        Variable exc = createTemporaryVariable();
        addInstr(new LabelInstr(ebi.dummyRescueBlockLabel));
        addInstr(new ReceiveJRubyExceptionInstr(exc));

        // Now emit the ensure body's stashed instructions
        if (ensureNode != null) {
            ebi.emitBody(this);
        }

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

    public Operand buildFalse() {
        return manager.getFalse();
    }

    /**
     * Generate if testVariable NEQ testValue { ifBlock } else { elseBlock }.
     *
     * @param testVariable what we will test against testValue
     * @param testValue what we want to testVariable to NOT be equal to.
     * @param ifBlock the code if test values do NOT match
     * @param elseBlock the code to execute otherwise.
     */
    private void if_else(Operand testVariable, Operand testValue, VoidCodeBlock ifBlock, VoidCodeBlock elseBlock) {
        Label elseLabel = getNewLabel();
        Label endLabel = getNewLabel();

        addInstr(BNEInstr.create(elseLabel, testVariable, testValue));
        ifBlock.run();
        addInstr(new JumpInstr(endLabel));

        addInstr(new LabelInstr(elseLabel));
        elseBlock.run();
        addInstr(new LabelInstr(endLabel));
    }

    private Variable buildRestKeywordArgs(HashNode keywordArgs) {
        Variable splatValue = copyAndReturnValue(new Hash(new ArrayList<>()));
        for (KeyValuePair<Node, Node> pair: keywordArgs.getPairs()) {
            Operand splat = buildWithOrder(pair.getValue(), keywordArgs.containsVariableAssignment());
            addInstr(new RuntimeHelperCall(splatValue, MERGE_KWARGS, new Operand[] { splatValue, splat }));
        }

        return splatValue;
    }

    public Operand buildFCall(Variable aResult, FCallNode fcallNode) {
        RubySymbol name = methodName = fcallNode.getName();

        Node callArgsNode = fcallNode.getArgsNode();
        final Variable result = aResult == null ? createTemporaryVariable() : aResult;

        HashNode keywordArgs = getPossibleKeywordArgument(fcallNode.getArgsNode());
        if (keywordArgs != null) {
            Operand[] args = buildCallArgsExcept(fcallNode.getArgsNode(), keywordArgs);

            if (keywordArgs.hasOnlyRestKwargs()) {  // {**k}, {**{}, **k}, etc...
                Variable splatValue = buildRestKeywordArgs(keywordArgs);
                Variable test = addResultInstr(new RuntimeHelperCall(createTemporaryVariable(), IS_HASH_EMPTY, new Operand[] { splatValue }));
                Operand block = setupCallClosure(fcallNode.getIterNode());

                determineIfWeNeedLineNumber(fcallNode); // buildOperand for fcall was papered over by args operand building so we check once more.
                if_else(test, manager.getTrue(),
                        () -> receiveBreakException(block, CallInstr.create(scope, FUNCTIONAL, result, name, buildSelf(), args, block)),
                        () -> receiveBreakException(block, CallInstr.create(scope, FUNCTIONAL, result, name, buildSelf(), addArg(args, splatValue), block)));
            } else {  // {a: 1, b: 2}
                List<KeyValuePair<Operand, Operand>> kwargs = buildKeywordArguments(keywordArgs);
                Operand block = setupCallClosure(fcallNode.getIterNode());

                determineIfWeNeedLineNumber(fcallNode); // buildOperand for fcall was papered over by args operand building so we check once more.
                receiveBreakException(block, CallInstr.createWithKwargs(scope, FUNCTIONAL, result, name, buildSelf(), args, block, kwargs));
            }
        } else {
            Operand[] args = setupCallArgs(callArgsNode);
            determineIfMaybeRefined(fcallNode.getName(), args);
            Operand block = setupCallClosure(fcallNode.getIterNode());

            // We will stuff away the iters AST source into the closure in the hope we can convert
            // this closure to a method.
            if (CommonByteLists.DEFINE_METHOD_METHOD.equals(fcallNode.getName().getBytes()) && block instanceof WrappedIRClosure) {
                IRClosure closure = ((WrappedIRClosure) block).getClosure();

                // To convert to a method we need its variable scoping to appear like a normal method.
                if (!closure.accessesParentsLocalVariables() &&
                        fcallNode.getIterNode() instanceof IterNode) {
                    closure.setSource((IterNode) fcallNode.getIterNode());
                }
            }

            determineIfWeNeedLineNumber(fcallNode); // buildOperand for fcall was papered over by args operand building so we check once more.
            receiveBreakException(block, CallInstr.create(scope, FUNCTIONAL, result, name, buildSelf(), args, block));
        }

        return result;
    }

    private Operand setupCallClosure(Node node) {
        if (node == null) return null;

        switch (node.getNodeType()) {
            case ITERNODE:
                return build(node);
            case BLOCKPASSNODE:
                Node bodyNode = ((BlockPassNode)node).getBodyNode();
                return bodyNode instanceof SymbolNode && !scope.maybeUsingRefinements() ?
                        new SymbolProc(((SymbolNode)bodyNode).getName()) : build(bodyNode);
            default:
                throw notCompilable("ERROR: Encountered a method with a non-block, non-blockpass iter node", node);
        }
    }

    // FIXME: This needs to be called on super/zsuper too
    private void determineIfMaybeRefined(RubySymbol methodName, Operand[] args) {
        IRScope outerScope = scope.getNearestTopLocalVariableScope();

        // 'using single_mod_arg' possible nearly everywhere but method scopes.
        if (!(outerScope instanceof IRMethod) && args.length == 1
                && (
                CommonByteLists.USING_METHOD.equals(methodName.getBytes())
                        // FIXME: This sets the bit for the whole module, but really only the refine block needs it
                        || CommonByteLists.REFINE_METHOD.equals(methodName.getBytes())
        )) {
            scope.setIsMaybeUsingRefinements();
        }
    }

    public Operand buildFixnum(FixnumNode node) {
        return manager.newFixnum(node.getValue());
    }

    public Operand buildFlip(FlipNode flipNode) {
        addRaiseError("NotImplementedError", "flip-flop is no longer supported in JRuby");
        return manager.getNil(); // not-reached
    }

    public Operand buildFloat(FloatNode node) {
        // Since flaot literals are effectively interned objects, no need to copyAndReturnValue(...)
        // SSS FIXME: Or is this a premature optimization?
        return new Float(node.getValue());
    }

    public Operand buildFor(ForNode forNode) {
        Variable result = createTemporaryVariable();
        Operand  receiver = build(forNode.getIterNode());
        Operand  forBlock = buildForIter(forNode);
        CallInstr callInstr = new CallInstr(scope, CallType.NORMAL, result, manager.runtime.newSymbol(CommonByteLists.EACH), receiver, EMPTY_OPERANDS,
                forBlock, scope.maybeUsingRefinements());
        receiveBreakException(forBlock, callInstr);

        return result;
    }

    public Operand buildForIter(final ForNode forNode) {
        // Create a new closure context
        IRClosure closure = new IRFor(manager, scope, forNode.getLine(), forNode.getScope(), Signature.from(forNode));

        // Create a new nested builder to ensure this gets its own IR builder state like the ensure block stack
        newIRBuilder(manager, closure).buildIterInner(null, forNode);

        return new WrappedIRClosure(buildSelf(), closure);
    }

    public Operand buildGlobalAsgn(GlobalAsgnNode globalAsgnNode) {
        Operand value = build(globalAsgnNode.getValueNode());
        addInstr(new PutGlobalVarInstr(globalAsgnNode.getName(), value));
        return value;
    }

    public Operand buildGlobalVar(Variable result, GlobalVarNode node) {
        if (result == null) result = createTemporaryVariable();

        return addResultInstr(new GetGlobalVariableInstr(result, node.getName()));
    }

    public Operand buildHash(HashNode hashNode) {
        List<KeyValuePair<Operand, Operand>> args = new ArrayList<>();
        boolean hasAssignments = hashNode.containsVariableAssignment();
        Variable hash = null;

        for (KeyValuePair<Node, Node> pair: hashNode.getPairs()) {
            Node key = pair.getKey();
            Operand keyOperand;

            if (key == null) {                          // Splat kwarg [e.g. {**splat1, a: 1, **splat2)]
                if (hash == null) {                     // No hash yet. Define so order is preserved.
                    hash = copyAndReturnValue(new Hash(args));
                    args = new ArrayList<>();           // Used args but we may find more after the splat so we reset
                } else if (!args.isEmpty()) {
                    addInstr(new RuntimeHelperCall(hash, MERGE_KWARGS, new Operand[] { hash, new Hash(args) }));
                    args = new ArrayList<>();
                }
                Operand splat = buildWithOrder(pair.getValue(), hasAssignments);
                addInstr(new RuntimeHelperCall(hash, MERGE_KWARGS, new Operand[] { hash, splat}));
                continue;
            } else {
                keyOperand = buildWithOrder(key, hasAssignments);
            }

            args.add(new KeyValuePair<>(keyOperand, buildWithOrder(pair.getValue(), hasAssignments)));
        }

        if (hash == null) {           // non-**arg ordinary hash
            hash = copyAndReturnValue(new Hash(args));
        } else if (!args.isEmpty()) { // ordinary hash values encountered after a **arg
            addInstr(new RuntimeHelperCall(hash, MERGE_KWARGS, new Operand[] { hash, new Hash(args) }));
        }

        return hash;
    }

    // Translate "r = if (cond); .. thenbody ..; else; .. elsebody ..; end" to
    //
    //     v = -- build(cond) --
    //     BEQ(v, FALSE, L1)
    //     r = -- build(thenbody) --
    //     jump L2
    // L1:
    //     r = -- build(elsebody) --
    // L2:
    //     --- r is the result of the if expression --
    //
    public Operand buildIf(Variable result, final IfNode ifNode) {
        Node actualCondition = ifNode.getCondition();

        Label    falseLabel = getNewLabel();
        Label    doneLabel  = getNewLabel();
        Operand  thenResult;
        addInstr(createBranch(build(actualCondition), manager.getFalse(), falseLabel));

        boolean thenNull = false;
        boolean elseNull = false;
        boolean thenUnil = false;
        boolean elseUnil = false;

        // Build the then part of the if-statement
        if (ifNode.getThenBody() != null) {
            thenResult = build(result, ifNode.getThenBody());
            if (thenResult != U_NIL) { // thenResult can be U_NIL if then-body ended with a return!
                // SSS FIXME: Can look at the last instr and short-circuit this jump if it is a break rather
                // than wait for dead code elimination to do it
                result = getValueInTemporaryVariable(thenResult);
                addInstr(new JumpInstr(doneLabel));
            } else {
                if (result == null) result = createTemporaryVariable();
                thenUnil = true;
            }
        } else {
            thenNull = true;
            if (result == null) result = createTemporaryVariable();
            addInstr(new CopyInstr(result, manager.getNil()));
            addInstr(new JumpInstr(doneLabel));
        }

        // Build the else part of the if-statement
        addInstr(new LabelInstr(falseLabel));
        if (ifNode.getElseBody() != null) {
            Operand elseResult = build(ifNode.getElseBody());
            // elseResult can be U_NIL if then-body ended with a return!
            if (elseResult != U_NIL) {
                addInstr(new CopyInstr(result, elseResult));
            } else {
                elseUnil = true;
            }
        } else {
            elseNull = true;
            addInstr(new CopyInstr(result, manager.getNil()));
        }

        if (thenNull && elseNull) {
            addInstr(new LabelInstr(doneLabel));
            return manager.getNil();
        } else if (thenUnil && elseUnil) {
            return U_NIL;
        } else {
            addInstr(new LabelInstr(doneLabel));
            return result;
        }
    }

    public Operand buildInstAsgn(final InstAsgnNode instAsgnNode) {
        Operand val = build(instAsgnNode.getValueNode());
        // NOTE: if 's' happens to the a class, this is effectively an assignment of a class instance variable
        addInstr(new PutFieldInstr(buildSelf(), instAsgnNode.getName(), val));
        return val;
    }

    public Operand buildInstVar(InstVarNode node) {
        return addResultInstr(new GetFieldInstr(createTemporaryVariable(), buildSelf(), node.getName()));
    }

    private InterpreterContext buildIterInner(RubySymbol methodName, IterNode iterNode) {
        this.methodName = methodName;

        boolean forNode = iterNode instanceof ForNode;
        prepareClosureImplicitState();                                    // recv_self, add frame block, etc)

        if (RubyInstanceConfig.FULL_TRACE_ENABLED) {
            addInstr(new TraceInstr(RubyEvent.B_CALL, getName(), getFileName(), scope.getLine() + 1));
        }

        if (!forNode) addCurrentModule();                                // %current_module
        receiveBlockArgs(iterNode);
        // for adds these after processing binding block args because and operations at that point happen relative
        // to the previous scope.
        if (forNode) addCurrentModule();                                 // %current_module

        // conceptually abstract prologue scope instr creation so we can put this at the end of it instead of replicate it.
        afterPrologueIndex = instructions.size() - 1;

        // Build closure body and return the result of the closure
        Operand closureRetVal = iterNode.getBodyNode() == null ? manager.getNil() : build(iterNode.getBodyNode());

        if (RubyInstanceConfig.FULL_TRACE_ENABLED) {
            addInstr(new TraceInstr(RubyEvent.B_RETURN, getName(), getFileName(), iterNode.getEndLine() + 1));
        }

        // can be U_NIL if the node is an if node with returns in both branches.
        if (closureRetVal != U_NIL) addInstr(new ReturnInstr(closureRetVal));

        preloadBlockImplicitClosure();

        // Add break/return handling in case it is a lambda (we cannot know at parse time what it is).
        // SSS FIXME: At a later time, see if we can optimize this and do this on demand.
        if (!forNode) handleBreakAndReturnsInLambdas();

        computeScopeFlagsFrom(instructions);
        return scope.allocateInterpreterContext(instructions, temporaryVariableIndex + 1, flags);
    }

    public Operand buildIter(final IterNode iterNode) {
        IRClosure closure = new IRClosure(manager, scope, iterNode.getLine(), iterNode.getScope(), Signature.from(iterNode), coverageMode);

        // Create a new nested builder to ensure this gets its own IR builder state like the ensure block stack
        newIRBuilder(manager, closure).buildIterInner(methodName, iterNode);

        methodName = null;

        return new WrappedIRClosure(buildSelf(), closure);
    }

    public Operand buildLiteral(LiteralNode literalNode) {
        return new MutableString(literalNode.getSymbolName());
    }

    public Operand buildLocalAsgn(LocalAsgnNode localAsgnNode) {
        Variable variable  = getLocalVariable(localAsgnNode.getName(), localAsgnNode.getDepth());
        Operand value = build(variable, localAsgnNode.getValueNode());

        // no use copying a variable to itself
        if (variable == value) return value;

        addInstr(new CopyInstr(variable, value));

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

    public Operand buildLocalVar(LocalVarNode node) {
        return getLocalVariable(node.getName(), node.getDepth());
    }

    public Operand buildMatch(Variable result, MatchNode matchNode) {
        Operand regexp = build(matchNode.getRegexpNode());

        Variable tempLastLine = createTemporaryVariable();
        addResultInstr(new GetGlobalVariableInstr(tempLastLine, symbol("$_")));

        if (result == null) result = createTemporaryVariable();
        return addResultInstr(new MatchInstr(scope, result, regexp, tempLastLine));
    }

    public Operand buildMatch2(Variable result, Match2Node matchNode) {
        Operand receiver = build(matchNode.getReceiverNode());
        Operand value    = build(matchNode.getValueNode());

        if (result == null) result = createTemporaryVariable();

        addInstr(new MatchInstr(scope, result, receiver, value));

        if (matchNode instanceof Match2CaptureNode) {
            Match2CaptureNode m2c = (Match2CaptureNode)matchNode;
            for (int slot:  m2c.getScopeOffsets()) {
                // Static scope scope offsets store both depth and offset
                int depth = slot >> 16;
                int offset = slot & 0xffff;

                // For now, we'll continue to implicitly reference "$~"
                RubySymbol var = manager.runtime.newSymbol(getVarNameFromScopeTree(scope, depth, offset));
                addInstr(new SetCapturedVarInstr(getLocalVariable(var, depth), result, var));
            }
        }
        return result;
    }

    private String getVarNameFromScopeTree(IRScope scope, int depth, int offset) {
        if (depth == 0) {
            return scope.getStaticScope().getVariables()[offset];
        }
        return getVarNameFromScopeTree(scope.getLexicalParent(), depth - 1, offset);
    }

    public Operand buildMatch3(Variable result, Match3Node matchNode) {
        Operand receiver = build(matchNode.getReceiverNode());
        Operand value = build(matchNode.getValueNode());

        if (result == null) result = createTemporaryVariable();

        return addResultInstr(new MatchInstr(scope, result, receiver, value));
    }

    private Operand getContainerFromCPath(Colon3Node cpath) {
        Operand container;

        if (cpath instanceof Colon2Node) {
            Node leftNode = ((Colon2Node) cpath).getLeftNode();

            if (leftNode != null) { // Foo::Bar
                container = build(leftNode);
            } else { // Only name with no left-side Bar <- Note no :: on left
                container = findContainerModule();
            }
        } else { //::Bar
            container = new ObjectClass();
        }

        return container;
    }

    public Operand buildModule(ModuleNode moduleNode) {
        boolean executesOnce = this.executesOnce;
        Colon3Node cpath = moduleNode.getCPath();
        ByteList moduleName = cpath.getName().getBytes();
        Operand container = getContainerFromCPath(cpath);

        //System.out.println("MODULE IS " +  (executesOnce ? "" : "NOT") + " SINGLE USE:"  + moduleName +  ", " +  scope.getFile() + ":" + moduleNode.getEndLine());

        IRModuleBody body = new IRModuleBody(manager, scope, moduleName, moduleNode.getLine(), moduleNode.getScope(), executesOnce);
        Variable bodyResult = addResultInstr(new DefineModuleInstr(createTemporaryVariable(), body, container));

        newIRBuilder(manager, body).buildModuleOrClassBody(moduleNode.getBodyNode(), moduleNode.getLine(), moduleNode.getEndLine());
        return bodyResult;
    }

    public Operand buildNext(final NextNode nextNode) {
        IRLoop currLoop = getCurrentLoop();

        Operand rv = build(nextNode.getValueNode());

        // If we have ensure blocks, have to run those first!
        if (!activeEnsureBlockStack.isEmpty()) emitEnsureBlocks(currLoop);

        if (currLoop != null) {
            // If a regular loop, the next is simply a jump to the end of the iteration
            addInstr(new JumpInstr(currLoop.iterEndLabel));
        } else {
            addInstr(new ThreadPollInstr(true));
            // If a closure, the next is simply a return from the closure!
            if (scope instanceof IRClosure) {
                if (scope instanceof IREvalScript) {
                    throwSyntaxError(nextNode, "Can't escape from eval with next");
                } else {
                    addInstr(new ReturnInstr(rv));
                }
            } else {
                throwSyntaxError(nextNode, "Invalid next");
            }
        }

        // Once the "next instruction" (closure-return) executes, control exits this scope
        return U_NIL;
    }

    public Operand buildNthRef(NthRefNode nthRefNode) {
        return copyAndReturnValue(new NthRef(scope, nthRefNode.getMatchNumber()));
    }

    public Operand buildNil() {
        return manager.getNil();
    }

    // FIXME: The logic for lazy and non-lazy building is pretty icky...clean up
    public Operand buildOpAsgn(OpAsgnNode opAsgnNode) {
        Label l;
        Variable readerValue = createTemporaryVariable();
        Variable writerValue = createTemporaryVariable();
        Node receiver = opAsgnNode.getReceiverNode();
        CallType callType = receiver instanceof SelfNode ? FUNCTIONAL : CallType.NORMAL;

        // get attr
        Operand  v1 = build(opAsgnNode.getReceiverNode());

        Label lazyLabel = null;
        Label endLabel = null;
        Variable result = createTemporaryVariable();
        if (opAsgnNode.isLazy()) {
            lazyLabel = getNewLabel();
            endLabel = getNewLabel();
            addInstr(new BNilInstr(lazyLabel, v1));
        }

        addInstr(CallInstr.create(scope, callType, readerValue, opAsgnNode.getVariableSymbolName(), v1, EMPTY_OPERANDS, null));

        // Ex: e.val ||= n
        //     e.val &&= n
        boolean isOrOr = opAsgnNode.isOr();
        if (isOrOr || opAsgnNode.isAnd()) {
            l = getNewLabel();
            addInstr(createBranch(readerValue, isOrOr ? manager.getTrue() : manager.getFalse(), l));

            // compute value and set it
            Operand  v2 = build(opAsgnNode.getValueNode());
            addInstr(CallInstr.create(scope, callType, writerValue, opAsgnNode.getVariableSymbolNameAsgn(), v1, new Operand[] {v2}, null));
            // It is readerValue = v2.
            // readerValue = writerValue is incorrect because the assignment method
            // might return something else other than the value being set!
            addInstr(new CopyInstr(readerValue, v2));
            addInstr(new LabelInstr(l));

            if (!opAsgnNode.isLazy()) return readerValue;

            addInstr(new CopyInstr(result, readerValue));
        } else {  // Ex: e.val = e.val.f(n)
            // call operator
            Operand  v2 = build(opAsgnNode.getValueNode());
            Variable setValue = createTemporaryVariable();
            addInstr(CallInstr.create(scope, setValue, opAsgnNode.getOperatorSymbolName(), readerValue, new Operand[]{v2}, null));

            // set attr
            addInstr(CallInstr.create(scope, callType, writerValue, opAsgnNode.getVariableSymbolNameAsgn(), v1, new Operand[] {setValue}, null));
            // Returning writerValue is incorrect because the assignment method
            // might return something else other than the value being set!
            if (!opAsgnNode.isLazy()) return setValue;

            addInstr(new CopyInstr(result, setValue));
        }

        addInstr(new JumpInstr(endLabel));
        addInstr(new LabelInstr(lazyLabel));
        addInstr(new CopyInstr(result, manager.getNil()));
        addInstr(new LabelInstr(endLabel));

        return result;
    }

    private Operand buildColon2ForConstAsgnDeclNode(Node lhs, Variable valueResult, boolean constMissing) {
        Variable leftModule = createTemporaryVariable();
        RubySymbol name;

        if (lhs instanceof Colon2Node) {
            Colon2Node colon2Node = (Colon2Node) lhs;
            name = colon2Node.getName();
            Operand leftValue = build(colon2Node.getLeftNode());
            copy(leftModule, leftValue);
        } else { // colon3
            copy(leftModule, new ObjectClass());
            name = ((Colon3Node) lhs).getName();
        }

        addInstr(new SearchModuleForConstInstr(valueResult, leftModule, name, false, constMissing));

        return leftModule;
    }

    public Operand buildOpAsgnConstDeclNode(OpAsgnConstDeclNode node) {
        if (node.isOr()) {
            Variable result = createTemporaryVariable();
            Label falseCheck = getNewLabel();
            Label done = getNewLabel();
            Label assign = getNewLabel();
            Operand module = buildColon2ForConstAsgnDeclNode(node.getFirstNode(), result, false);
            addInstr(BNEInstr.create(falseCheck, result, UndefinedValue.UNDEFINED));
            addInstr(new JumpInstr(assign));
            addInstr(new LabelInstr(falseCheck));
            addInstr(BNEInstr.create(done, result, manager.getFalse()));
            addInstr(new LabelInstr(assign));
            Operand rhsValue = build(node.getSecondNode());
            copy(result, rhsValue);
            addInstr(new PutConstInstr(module, ((Colon3Node) node.getFirstNode()).getName(), rhsValue));
            addInstr(new LabelInstr(done));
            return result;
        } else if (node.isAnd()) {
            Variable result = createTemporaryVariable();
            Label done = getNewLabel();
            Operand module = buildColon2ForConstAsgnDeclNode(node.getFirstNode(), result, true);
            addInstr(new BFalseInstr(done, result));
            Operand rhsValue = build(node.getSecondNode());
            copy(result, rhsValue);
            addInstr(new PutConstInstr(module, ((Colon3Node) node.getFirstNode()).getName(), rhsValue));
            addInstr(new LabelInstr(done));
            return result;
        }

        Variable result = createTemporaryVariable();
        Operand lhs = build(node.getFirstNode());
        Operand rhs = build(node.getSecondNode());
        addInstr(CallInstr.create(scope, result, node.getSymbolOperator(), lhs, new Operand[] { rhs }, null));
        return addResultInstr(new CopyInstr(createTemporaryVariable(), putConstantAssignment(node, result)));
    }

    // Translate "x &&= y" --> "x = y if is_true(x)" -->
    //
    //    x = -- build(x) should return a variable! --
    //    f = is_true(x)
    //    beq(f, false, L)
    //    x = -- build(y) --
    // L:
    //
    public Operand buildOpAsgnAnd(OpAsgnAndNode andNode) {
        Label    l  = getNewLabel();
        Operand  v1 = build(andNode.getFirstNode());
        Variable result = getValueInTemporaryVariable(v1);
        addInstr(createBranch(v1, manager.getFalse(), l));
        Operand v2 = build(andNode.getSecondNode());  // This does the assignment!
        addInstr(new CopyInstr(result, v2));
        addInstr(new LabelInstr(l));
        return result;
    }

    // "x ||= y"
    // --> "x = (is_defined(x) && is_true(x) ? x : y)"
    // --> v = -- build(x) should return a variable! --
    //     f = is_true(v)
    //     beq(f, true, L)
    //     -- build(x = y) --
    //   L:
    //
    public Operand buildOpAsgnOr(final OpAsgnOrNode orNode) {
        Label    l1 = getNewLabel();
        Label    l2 = null;
        Variable flag = createTemporaryVariable();
        Operand  v1;
        boolean  needsDefnCheck = orNode.getFirstNode().needsDefinitionCheck();
        if (needsDefnCheck) {
            l2 = getNewLabel();
            v1 = buildGetDefinition(orNode.getFirstNode());
            addInstr(new CopyInstr(flag, v1));
            addInstr(createBranch(flag, manager.getNil(), l2)); // if v1 is undefined, go to v2's computation
        }
        v1 = build(orNode.getFirstNode()); // build of 'x'
        addInstr(new CopyInstr(flag, v1));
        Variable result = getValueInTemporaryVariable(v1);
        if (needsDefnCheck) {
            addInstr(new LabelInstr(l2));
        }
        addInstr(createBranch(flag, manager.getTrue(), l1));  // if v1 is defined and true, we are done!
        Operand v2 = build(orNode.getSecondNode()); // This is an AST node that sets x = y, so nothing special to do here.
        addInstr(new CopyInstr(result, v2));
        addInstr(new LabelInstr(l1));

        // Return value of x ||= y is always 'x'
        return result;
    }

    public Operand buildOpElementAsgn(OpElementAsgnNode node) {
        // Translate "a[x] ||= n" --> "a[x] = n if !is_true(a[x])"
        if (node.isOr()) return buildOpElementAsgnWith(node, manager.getTrue());

        // Translate "a[x] &&= n" --> "a[x] = n if is_true(a[x])"
        if (node.isAnd()) return buildOpElementAsgnWith(node, manager.getFalse());

        // a[i] *= n, etc.  anything that is not "a[i] &&= .. or a[i] ||= .."
        return buildOpElementAsgnWithMethod(node);
    }

    private Operand buildOpElementAsgnWith(OpElementAsgnNode opElementAsgnNode, Boolean truthy) {
        Node receiver = opElementAsgnNode.getReceiverNode();
        CallType callType = receiver instanceof SelfNode ? FUNCTIONAL : CallType.NORMAL;
        Operand array = buildWithOrder(receiver, opElementAsgnNode.containsVariableAssignment());
        Label endLabel = getNewLabel();
        Variable elt = createTemporaryVariable();
        Operand[] argList = setupCallArgs(opElementAsgnNode.getArgsNode());
        Operand block = setupCallClosure(opElementAsgnNode.getBlockNode());
        addInstr(CallInstr.create(scope, callType, elt, symbol(ArrayDerefInstr.AREF), array, argList, block));
        addInstr(createBranch(elt, truthy, endLabel));
        Operand value = build(opElementAsgnNode.getValueNode());

        argList = addArg(argList, value);
        addInstr(CallInstr.create(scope, callType, elt, symbol(ArrayDerefInstr.ASET), array, argList, block));
        addInstr(new CopyInstr(elt, value));

        addInstr(new LabelInstr(endLabel));
        return elt;
    }

    // a[i] *= n, etc.  anything that is not "a[i] &&= .. or a[i] ||= .."
    public Operand buildOpElementAsgnWithMethod(OpElementAsgnNode opElementAsgnNode) {
        Node receiver = opElementAsgnNode.getReceiverNode();
        CallType callType = receiver instanceof SelfNode ? FUNCTIONAL : CallType.NORMAL;
        Operand array = buildWithOrder(receiver, opElementAsgnNode.containsVariableAssignment());
        Operand[] argList = setupCallArgs(opElementAsgnNode.getArgsNode());
        Operand block = setupCallClosure(opElementAsgnNode.getBlockNode());
        Variable elt = createTemporaryVariable();
        addInstr(CallInstr.create(scope, callType, elt, symbol(ArrayDerefInstr.AREF), array, argList, block)); // elt = a[args]
        Operand value = build(opElementAsgnNode.getValueNode());                                       // Load 'value'
        RubySymbol operation = opElementAsgnNode.getOperatorSymbolName();
        addInstr(CallInstr.create(scope, callType, elt, operation, elt, new Operand[] { value }, null)); // elt = elt.OPERATION(value)
        // SSS: do not load the call result into 'elt' to eliminate the RAW dependency on the call
        // We already know what the result is going be .. we are just storing it back into the array
        Variable tmp = createTemporaryVariable();
        argList = addArg(argList, elt);
        addInstr(CallInstr.create(scope, callType, tmp, symbol(ArrayDerefInstr.ASET), array, argList, block));   // a[args] = elt
        return elt;
    }

    // Translate ret = (a || b) to ret = (a ? true : b) as follows
    //
    //    v1 = -- build(a) --
    //       OPT: ret can be set to v1, but effectively v1 is true if we take the branch to L.
    //            while this info can be inferred by using attributes, why bother if we can do this?
    //    ret = v1
    //    beq(v1, true, L)
    //    v2 = -- build(b) --
    //    ret = v2
    // L:
    //
    public Operand buildOr(final OrNode orNode) {
        // lazy evaluation opt.  Don't bother building rhs of expr is lhs is unconditionally true.
        if (orNode.getFirstNode().getNodeType().alwaysTrue()) return build(orNode.getFirstNode());

        // lazy evaluation opt. Eliminate conditional logic if we know lhs is always false.
        if (orNode.getFirstNode().getNodeType().alwaysFalse()) {
            build(orNode.getFirstNode());          // needs to be executed for potential side-effects
            return build(orNode.getSecondNode());  // but we return rhs for result.
        }

        Label endOfExprLabel = getNewLabel();
        Operand left = build(orNode.getFirstNode());
        Variable result = getValueInTemporaryVariable(left);
        addInstr(createBranch(left, manager.getTrue(), endOfExprLabel));
        Operand right  = build(orNode.getSecondNode());
        addInstr(new CopyInstr(result, right));
        addInstr(new LabelInstr(endOfExprLabel));

        return result;
    }

    private InterpreterContext buildPrePostExeInner(Node body) {
        // Set up %current_module
        addInstr(new CopyInstr(getCurrentModuleVariable(), SCOPE_MODULE[0]));
        build(body);

        // END does not have either explicit or implicit return, so we add one
        addInstr(new ReturnInstr(new Nil()));

        computeScopeFlagsFrom(instructions);
        return scope.allocateInterpreterContext(instructions, temporaryVariableIndex + 1, flags);
    }

    private List<Instr> buildPreExeInner(Node body) {
        build(body);

        return instructions;
    }

    public Operand buildPostExe(PostExeNode postExeNode) {
        IRScope topLevel = scope.getRootLexicalScope();
        IRScope nearestLVarScope = scope.getNearestTopLocalVariableScope();

        StaticScope parentScope = nearestLVarScope.getStaticScope();
        StaticScope staticScope = parentScope.duplicate();
        staticScope.setEnclosingScope(parentScope);
        IRClosure endClosure = new IRClosure(manager, scope, postExeNode.getLine(), staticScope,
                Signature.from(postExeNode), CommonByteLists._END_, true);
        staticScope.setIRScope(endClosure);
        endClosure.setIsEND();
        // Create a new nested builder to ensure this gets its own IR builder state like the ensure block stack
        newIRBuilder(manager, endClosure).buildPrePostExeInner(postExeNode.getBodyNode());

        // Add an instruction in 's' to record the end block in the 'topLevel' scope.
        // SSS FIXME: IR support for end-blocks that access vars in non-toplevel-scopes
        // might be broken currently. We could either fix it or consider dropping support
        // for END blocks altogether or only support them in the toplevel. Not worth the pain.
        addInstr(new RecordEndBlockInstr(topLevel, new WrappedIRClosure(buildSelf(), endClosure)));
        return manager.getNil();
    }

    public Operand buildPreExe(PreExeNode preExeNode) {
        IRBuilder builder = new IRBuilder(manager, scope, this, this);

        List<Instr> beginInstrs = builder.buildPreExeInner(preExeNode.getBodyNode());

        instructions.addAll(afterPrologueIndex, beginInstrs);

        afterPrologueIndex += beginInstrs.size();

        return manager.getNil();
    }

    public Operand buildRational(RationalNode rationalNode) {

        return new Rational((ImmutableLiteral) build(rationalNode.getNumerator()),
                (ImmutableLiteral) build(rationalNode.getDenominator()));
    }

    public Operand buildRedo(RedoNode redoNode) {
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
                    throwSyntaxError(redoNode, "Can't escape from eval with redo");
                } else {
                    addInstr(new ThreadPollInstr(true));
                    Label startLabel = new Label(scope.getId() + "_START", 0);
                    instructions.add(afterPrologueIndex, new LabelInstr(startLabel));
                    addInstr(new JumpInstr(startLabel));
                }
            } else {
                throwSyntaxError(redoNode, "Invalid redo");
            }
        }
        return manager.getNil();
    }

    public Operand buildRegexp(RegexpNode reNode) {
        // SSS FIXME: Rather than throw syntax error at runtime, we should detect
        // regexp syntax errors at build time and add an exception-throwing instruction instead
        return copyAndReturnValue(new Regexp(reNode.getValue(), reNode.getOptions()));
    }

    public Operand buildRescue(RescueNode node) {
        return buildEnsureInternal(node, null);
    }

    private boolean canBacktraceBeRemoved(RescueNode rescueNode) {
        if (RubyInstanceConfig.FULL_TRACE_ENABLED || !(rescueNode instanceof RescueModNode) &&
                rescueNode.getElseNode() != null) return false;

        RescueBodyNode rescueClause = rescueNode.getRescueNode();

        if (rescueClause.getOptRescueNode() != null) return false;  // We will not handle multiple rescues
        if (rescueClause.getExceptionNodes() != null) return false; // We cannot know if these are builtin or not statically.

        Node body = rescueClause.getBodyNode();

        // This optimization omits backtrace info for the exception getting rescued so we cannot
        // optimize the exception variable.
        if (body instanceof GlobalVarNode && isErrorInfoGlobal(((GlobalVarNode) body).getName().idString())) return false;

        // FIXME: This MIGHT be able to expand to more complicated expressions like Hash or Array if they
        // contain only SideEffectFree nodes.  Constructing a literal out of these should be safe from
        // effecting or being able to access $!.
        return body instanceof SideEffectFree;
    }

    private static boolean isErrorInfoGlobal(final String name) {
        // Global names and aliases that reference the exception in flight
        switch (name) {
            case "$!" :
            case "$ERROR_INFO" :
            case "$@" :
            case "$ERROR_POSITION" :
                return true;
            default :
                return false;
        }
    }

    private Operand buildRescueInternal(RescueNode rescueNode, EnsureBlockInfo ensure) {
        boolean needsBacktrace = !canBacktraceBeRemoved(rescueNode);

        // Labels marking start, else, end of the begin-rescue(-ensure)-end block
        Label rBeginLabel = getNewLabel();
        Label rEndLabel   = ensure.end;
        Label rescueLabel = getNewLabel(); // Label marking start of the first rescue code.
        ensure.needsBacktrace = needsBacktrace;

        addInstr(new LabelInstr(rBeginLabel));

        // Placeholder rescue instruction that tells rest of the compiler passes the boundaries of the rescue block.
        addInstr(new ExceptionRegionStartMarkerInstr(rescueLabel));
        activeRescuers.push(rescueLabel);
        addInstr(manager.needsBacktrace(needsBacktrace));

        // Body
        Operand tmp = manager.getNil();  // default return value if for some strange reason, we neither have the body node or the else node!
        Variable rv = createTemporaryVariable();
        if (rescueNode.getBodyNode() != null) tmp = build(rescueNode.getBodyNode());

        // Since rescued regions are well nested within Ruby, this bare marker is sufficient to
        // let us discover the edge of the region during linear traversal of instructions during cfg construction.
        addInstr(new ExceptionRegionEndMarkerInstr());
        activeRescuers.pop();

        // Else part of the body -- we simply fall through from the main body if there were no exceptions
        if (rescueNode.getElseNode() != null) {
            addInstr(new LabelInstr(getNewLabel()));
            tmp = build(rescueNode.getElseNode());
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
        if (!needsBacktrace) addInstr(manager.needsBacktrace(true));

        // Save off exception & exception comparison type
        Variable exc = addResultInstr(new ReceiveRubyExceptionInstr(createTemporaryVariable()));

        // Build the actual rescue block(s)
        buildRescueBodyInternal(rescueNode.getRescueNode(), rv, exc, rEndLabel);

        activeRescueBlockStack.pop();
        return rv;
    }

    private void outputExceptionCheck(Operand excType, Operand excObj, Label caughtLabel) {
        Variable eqqResult = addResultInstr(new RescueEQQInstr(createTemporaryVariable(), excType, excObj));
        addInstr(createBranch(eqqResult, manager.getTrue(), caughtLabel));
    }

    private void buildRescueBodyInternal(RescueBodyNode rescueBodyNode, Variable rv, Variable exc, Label endLabel) {
        final Node exceptionList = rescueBodyNode.getExceptionNodes();

        // Compare and branch as necessary!
        Label uncaughtLabel = getNewLabel();
        Label caughtLabel = getNewLabel();
        if (exceptionList != null) {
            if (exceptionList instanceof ListNode) {
                Node[] exceptionNodes = ((ListNode) exceptionList).children();
                for (int i = 0; i < exceptionNodes.length; i++) {
                    outputExceptionCheck(build(exceptionNodes[i]), exc, caughtLabel);
                }
            } else if (exceptionList instanceof SplatNode) { // splatnode, catch
                outputExceptionCheck(build(((SplatNode)exceptionList).getValue()), exc, caughtLabel);
            } else { // argscat/argspush
                outputExceptionCheck(build(exceptionList), exc, caughtLabel);
            }
        } else {
            outputExceptionCheck(manager.getStandardError(), exc, caughtLabel);
        }

        // Uncaught exception -- build other rescue nodes or rethrow!
        addInstr(new LabelInstr(uncaughtLabel));
        if (rescueBodyNode.getOptRescueNode() != null) {
            buildRescueBodyInternal(rescueBodyNode.getOptRescueNode(), rv, exc, endLabel);
        } else {
            addInstr(new ThrowExceptionInstr(exc));
        }

        // Caught exception case -- build rescue body
        addInstr(new LabelInstr(caughtLabel));
        Node realBody = rescueBodyNode.getBodyNode();
        Operand x = build(realBody);
        if (x != U_NIL) { // can be U_NIL if the rescue block has an explicit return
            // Set up node return value 'rv'
            addInstr(new CopyInstr(rv, x));

            // Clone the topmost ensure block (which will be a wrapper
            // around the current rescue block)
            activeEnsureBlockStack.peek().cloneIntoHostScope(this);

            addInstr(new JumpInstr(endLabel));
        }
    }

    public Operand buildRetry(RetryNode retryNode) {
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
            throwSyntaxError(retryNode, "Invalid retry");
        } else {
            addInstr(new ThreadPollInstr(true));
            // Restore $! and jump back to the entry of the rescue block
            RescueBlockInfo rbi = activeRescueBlockStack.peek();
            addInstr(new PutGlobalVarInstr(symbol("$!"), rbi.savedExceptionVariable));
            addInstr(new JumpInstr(rbi.entryLabel));
            // Retries effectively create a loop
            scope.setHasLoops();
        }
        return manager.getNil();
    }

    private Operand processEnsureRescueBlocks(Operand retVal) {
        // Before we return,
        // - have to go execute all the ensure blocks if there are any.
        //   this code also takes care of resetting "$!"
        if (!activeEnsureBlockStack.isEmpty()) {
            retVal = addResultInstr(new CopyInstr(createTemporaryVariable(), retVal));
            emitEnsureBlocks(null);
        }
       return retVal;
    }

    public Operand buildReturn(ReturnNode returnNode) {
        Operand retVal = build(returnNode.getValueNode());

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
                addInstr(new TraceInstr(RubyEvent.RETURN, getName(), getFileName(), returnNode.getLine() + 1));
            }

            addInstr(new ReturnInstr(retVal));
        }

        // The value of the return itself in the containing expression can never be used because of control-flow reasons.
        // The expression that uses this result can never be executed beyond the return and hence the value itself is just
        // a placeholder operand.
        return U_NIL;
    }

    public InterpreterContext buildEvalRoot(RootNode rootNode) {
        executesOnce = false;
        coverageMode = CoverageData.NONE;  // Assuming there is no path into build eval root without actually being an eval.
        addInstr(manager.newLineNumber(scope.getLine()));

        prepareImplicitState();                                    // recv_self, add frame block, etc)
        addCurrentModule();                                        // %current_module

        afterPrologueIndex = instructions.size() - 1;                      // added BEGINs start after scope prologue stuff

        Operand returnValue = rootNode.getBodyNode() == null ? manager.getNil() : build(rootNode.getBodyNode());
        addInstr(new ReturnInstr(returnValue));

        computeScopeFlagsFrom(instructions);
        return scope.allocateInterpreterContext(instructions, temporaryVariableIndex + 2, flags);
    }

    public static InterpreterContext buildRoot(IRManager manager, RootNode rootNode) {
        String file = rootNode.getFile();
        IRScriptBody script = new IRScriptBody(manager, file == null ? "(anon)" : file, rootNode.getStaticScope());

        return topIRBuilder(manager, script).buildRootInner(rootNode);
    }

    private void addCurrentModule() {
        addInstr(new CopyInstr(getCurrentModuleVariable(), SCOPE_MODULE[0])); // %current_module
    }

    private InterpreterContext buildRootInner(RootNode rootNode) {
        coverageMode = rootNode.coverageMode();
        prepareImplicitState();                                    // recv_self, add frame block, etc)
        addCurrentModule();                                        // %current_module

        afterPrologueIndex = instructions.size() - 1;                      // added BEGINs start after scope prologue stuff

        // Build IR for the tree and return the result of the expression tree
        addInstr(new ReturnInstr(build(rootNode.getBodyNode())));

        computeScopeFlagsFrom(instructions);
        // Root scope can receive returns now, so we add non-local return logic if necessary (2.5+)
        if (scope.canReceiveNonlocalReturns()) handleNonlocalReturnInMethod();

        return scope.allocateInterpreterContext(instructions, temporaryVariableIndex + 1, flags);
    }

    public Variable buildSelf() {
        return scope.getSelf();
    }

    public Operand buildSplat(SplatNode splatNode) {
        return addResultInstr(new BuildSplatInstr(createTemporaryVariable(), build(splatNode.getValue()), true));
    }

    public Operand buildStr(StrNode strNode) {
        Operand literal = buildStrRaw(strNode);

        return literal instanceof FrozenString ? literal : copyAndReturnValue(literal);
    }

    public Operand buildStrRaw(StrNode strNode) {
        if (strNode instanceof FileNode) return new Filename();

        int line = strNode.getLine();

        if (strNode.isFrozen()) return new FrozenString(strNode.getValue(), strNode.getCodeRange(), scope.getFile(), line);

        return new MutableString(strNode.getValue(), strNode.getCodeRange(), scope.getFile(), line);
    }

    private Operand buildSuperInstr(Operand block, Operand[] args) {
        CallInstr superInstr;
        Variable ret = createTemporaryVariable();
        if (scope instanceof IRMethod && scope.getLexicalParent() instanceof IRClassBody) {
            if (((IRMethod) scope).isInstanceMethod) {
                superInstr = new InstanceSuperInstr(scope, ret, getCurrentModuleVariable(), getName(), args, block, scope.maybeUsingRefinements());
            } else {
                superInstr = new ClassSuperInstr(scope, ret, getCurrentModuleVariable(), getName(), args, block, scope.maybeUsingRefinements());
            }
        } else {
            // We dont always know the method name we are going to be invoking if the super occurs in a closure.
            // This is because the super can be part of a block that will be used by 'define_method' to define
            // a new method.  In that case, the method called by super will be determined by the 'name' argument
            // to 'define_method'.
            superInstr = new UnresolvedSuperInstr(scope, ret, buildSelf(), args, block, scope.maybeUsingRefinements());
        }
        receiveBreakException(block, superInstr);
        return ret;
    }

    public Operand buildSuper(SuperNode superNode) {
        Operand[] args = setupCallArgs(superNode.getArgsNode());
        Operand block = setupCallClosure(superNode.getIterNode());
        if (block == null) block = getYieldClosureVariable();
        return buildSuperInstr(block, args);
    }

    public Operand buildSValue(SValueNode node) {
        // SSS FIXME: Required? Verify with Tom/Charlie
        return copyAndReturnValue(new SValue(build(node.getValue())));
    }

    public Operand buildSymbol(SymbolNode node) {
        // Since symbols are interned objects, no need to copyAndReturnValue(...)
        return new Symbol(node.getName());
    }

    public Operand buildTrue() {
        return manager.getTrue();
    }

    public Operand buildUndef(Node node) {
        Operand methName = build(((UndefNode) node).getName());
        return addResultInstr(new UndefMethodInstr(createTemporaryVariable(), methName));
    }

    private Operand buildConditionalLoop(Node conditionNode,
                                         Node bodyNode, boolean isWhile, boolean isLoopHeadCondition) {
        if (isLoopHeadCondition &&
                ((isWhile && conditionNode.getNodeType().alwaysFalse()) ||
                (!isWhile && conditionNode.getNodeType().alwaysTrue()))) {
            // we won't enter the loop -- just build the condition node
            build(conditionNode);
            return manager.getNil();
        } else {
            IRLoop loop = new IRLoop(scope, getCurrentLoop(), createTemporaryVariable());
            Variable loopResult = loop.loopResult;
            Label setupResultLabel = getNewLabel();

            // Push new loop
            loopStack.push(loop);

            // End of iteration jumps here
            addInstr(new LabelInstr(loop.loopStartLabel));
            if (isLoopHeadCondition) {
                Operand cv = build(conditionNode);
                addInstr(createBranch(cv, isWhile ? manager.getFalse() : manager.getTrue(), setupResultLabel));
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
                addInstr(createBranch(cv, isWhile ? manager.getTrue() : manager.getFalse(), loop.iterStartLabel));
            }

            // Loop result -- nil always
            addInstr(new LabelInstr(setupResultLabel));
            addInstr(new CopyInstr(loopResult, manager.getNil()));

            // Loop end -- breaks jump here bypassing the result set up above
            addInstr(new LabelInstr(loop.loopEndLabel));

            // Done with loop
            loopStack.pop();

            return loopResult;
        }
    }

    public Operand buildUntil(final UntilNode untilNode) {
        return buildConditionalLoop(untilNode.getConditionNode(), untilNode.getBodyNode(), false, untilNode.evaluateAtStart());
    }

    public Operand buildVAlias(VAliasNode valiasNode) {
        addInstr(new GVarAliasInstr(new MutableString(valiasNode.getNewName()), new MutableString(valiasNode.getOldName())));

        return manager.getNil();
    }

    public Operand buildVCall(Variable result, VCallNode node) {
        if (result == null) result = createTemporaryVariable();

        return addResultInstr(CallInstr.create(scope, CallType.VARIABLE, result, node.getName(), buildSelf(), EMPTY_OPERANDS, null));
    }

    public Operand buildWhile(final WhileNode whileNode) {
        return buildConditionalLoop(whileNode.getConditionNode(), whileNode.getBodyNode(), true, whileNode.evaluateAtStart());
    }

    public Operand buildXStr(XStrNode node) {
        return addResultInstr(CallInstr.create(scope, FUNCTIONAL, createTemporaryVariable(),
                manager.getRuntime().newSymbol("`"), Self.SELF,
                new Operand[] { new FrozenString(node.getValue(), node.getCodeRange(), scope.getFile(), node.getLine()) }, null));
    }

    public Operand buildYield(YieldNode node, Variable result) {
        if (scope instanceof IRScriptBody) throwSyntaxError(node, "Invalid yield");

        boolean unwrap = true;
        Node argNode = node.getArgsNode();
        // Get rid of one level of array wrapping
        if (argNode != null && (argNode instanceof ArrayNode) && ((ArrayNode)argNode).size() == 1) {
            argNode = ((ArrayNode)argNode).getLast();
            unwrap = false;
        }

        Variable ret = result == null ? createTemporaryVariable() : result;
        Operand value = argNode instanceof ArrayNode && unwrap ?  buildArray((ArrayNode)argNode, true) : build(argNode);

        addInstr(new YieldInstr(ret, getYieldClosureVariable(), value, unwrap));

        return ret;
    }

    public Operand copy(Operand value) {
        return copy(null, value);
    }

    public Operand copy(Variable result, Operand value) {
        return addResultInstr(new CopyInstr(result == null ? createTemporaryVariable() : result, value));
    }

    public Operand buildZArray(Variable result) {
       return copy(result, new Array());
    }

    private Operand buildZSuperIfNest(final Operand block) {
        int depthFromSuper = 0;
        IRBuilder superBuilder = this;
        IRScope superScope = scope;

        boolean defineMethod = false;
        // Figure out depth from argument scope and whether defineMethod may be one of the method calls.
        while (superScope instanceof IRClosure) {
            if (superBuilder != null && superBuilder.isDefineMethod()) defineMethod = true;

            // We may run out of live builds and walk int already built scopes if zsuper in an eval
            superBuilder = superBuilder != null && superBuilder.parent != null ? superBuilder.parent : null;
            superScope = superScope.getLexicalParent();
            depthFromSuper++;
        }

        // If we hit a method, this is known to always succeed
        Variable zsuperResult = createTemporaryVariable();
        if (superScope instanceof IRMethod && !defineMethod) {
            Operand[] args = adjustVariableDepth(getCallArgs(superScope, superBuilder), depthFromSuper);
            addInstr(new ZSuperInstr(scope, zsuperResult, buildSelf(), args, block, scope.maybeUsingRefinements()));
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

    private boolean isDefineMethod() {
        if (methodName != null) {
            String name = methodName.asJavaString();

            return "define_method".equals(name) || "define_singleton_method".equals(name);
        }

        return false;
    }

    private void addRaiseError(String id, String message) {
        Ruby runtime = scope.getManager().getRuntime();
        Operand exceptionClass = searchModuleForConst(new ObjectClass(), runtime.newSymbol(id));
        Operand kernel = searchModuleForConst(new ObjectClass(), runtime.newSymbol("Kernel"));
        addResultInstr(CallInstr.create(scope,
                createTemporaryVariable(),
                runtime.newSymbol("raise"),
                kernel,
                new Operand[] { exceptionClass, new MutableString(message) },
                null));
    }

    public Operand buildZSuper(ZSuperNode zsuperNode) {
        Operand block = setupCallClosure(zsuperNode.getIterNode());
        if (block == null) block = getYieldClosureVariable();

        if (scope instanceof IRMethod) {
            return buildSuperInstr(block, getCallArgs(scope, this));
        } else {
            return buildZSuperIfNest(block);
        }
    }

    /*
     * Adjust all argument operands by changing their depths to reflect how far they are from
     * super.  This fixup is only currently happening in supers nested in closures.
     */
    private Operand[] adjustVariableDepth(Operand[] args, int depthFromSuper) {
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

    private InterpreterContext buildModuleOrClassBody(Node bodyNode, int startLine, int endLine) {
        addInstr(new TraceInstr(RubyEvent.CLASS, null, getFileName(), startLine + 1));

        prepareImplicitState();                                    // recv_self, add frame block, etc)
        addCurrentModule();                                        // %current_module

        Operand bodyReturnValue = build(bodyNode);

        // This is only added when tracing is enabled because an 'end' will normally have no other instrs which can
        // raise after this point.  When we add trace we need to add one so backtrace generated shows the 'end' line.
        addInstr(manager.newLineNumber(endLine));
        addInstr(new TraceInstr(RubyEvent.END, null, getFileName(), endLine + 1));

        addInstr(new ReturnInstr(bodyReturnValue));

        computeScopeFlagsFrom(instructions);
        return scope.allocateInterpreterContext(instructions, temporaryVariableIndex + 1, flags);
    }

    private RubySymbol methodNameFor() {
        IRScope method = scope.getNearestMethod();

        return method == null ? null : method.getName();
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

    public LocalVariable getLocalVariable(RubySymbol name, int scopeDepth) {
        return scope.getLocalVariable(name, scopeDepth);
    }

    public LocalVariable getNewLocalVariable(RubySymbol name, int scopeDepth) {
        return scope.getNewLocalVariable(name, scopeDepth);
    }

    public RubySymbol getName() {
        return scope.getName();
    }

    private Label getNewLabel() {
        return scope.getNewLabel();
    }

    private String getFileName() {
        return scope.getFile();
    }

    private RubySymbol symbol(String id) {
        return manager.runtime.newSymbol(id);
    }

    private RubySymbol symbol(ByteList bytelist) {
        return manager.runtime.newSymbol(bytelist);
    }

    /**
     * Extract all call arguments from the specified scope (only useful for Closures and Methods) so that
     * we can convert zsupers to supers with explicit arguments.
     *
     * Note: This is fairly expensive because we walk entire scope when we could potentially stop earlier
     * if we knew when recv_* were done.
     */
    public static Operand[] getCallArgs(IRScope scope, IRBuilder builder) {
        List<Operand> callArgs = new ArrayList<>(5);
        List<KeyValuePair<Operand, Operand>> keywordArgs = new ArrayList<>(3);

        if (builder != null) {  // Still in currently building scopes
            for (Instr instr : builder.instructions) {
                extractCallOperands(callArgs, keywordArgs, instr);
            }
        } else {               // walked out past the eval to already build scopes
            for (Instr instr : scope.interpreterContext.getInstructions()) {
                extractCallOperands(callArgs, keywordArgs, instr);
            }
        }

        return getCallOperands(scope, callArgs, keywordArgs);
    }


    private static void extractCallOperands(List<Operand> callArgs, List<KeyValuePair<Operand, Operand>> keywordArgs, Instr instr) {
        if (instr instanceof ReceiveKeywordRestArgInstr) {
            // Always add the keyword rest arg to the beginning
            keywordArgs.add(0, new KeyValuePair<Operand, Operand>(Symbol.KW_REST_ARG_DUMMY, ((ReceiveArgBase) instr).getResult()));
        } else if (instr instanceof ReceiveKeywordArgInstr) {
            ReceiveKeywordArgInstr receiveKwargInstr = (ReceiveKeywordArgInstr) instr;
            keywordArgs.add(new KeyValuePair<Operand, Operand>(new Symbol(receiveKwargInstr.getKey()), receiveKwargInstr.getResult()));
        } else if (instr instanceof ReceiveRestArgInstr) {
            callArgs.add(new Splat(((ReceiveRestArgInstr) instr).getResult()));
        } else if (instr instanceof ReceiveArgBase) {
            callArgs.add(((ReceiveArgBase) instr).getResult());
        }
    }

    private static Operand[] getCallOperands(IRScope scope, List<Operand> callArgs, List<KeyValuePair<Operand, Operand>> keywordArgs) {
        if (scope.receivesKeywordArgs()) {
            int i = 0;
            Operand[] args = new Operand[callArgs.size() + 1];
            for (Operand arg: callArgs) {
                args[i++] = arg;
            }
            args[i] = new Hash(keywordArgs, true);
            return args;
        }

        return callArgs.toArray(new Operand[callArgs.size()]);
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

    public Variable getCurrentModuleVariable() {
        if (currentModuleVariable == null) currentModuleVariable = createCurrentModuleVariable();

        return currentModuleVariable;
    }

    public Variable createCurrentModuleVariable() {
        // SSS: Used in only 3 cases in generated IR:
        // -> searching a constant in the inheritance hierarchy
        // -> searching a super-method in the inheritance hierarchy
        // -> looking up 'StandardError' (which can be eliminated by creating a special operand type for this)
        temporaryVariableIndex++;
        return TemporaryCurrentModuleVariable.ModuleVariableFor(temporaryVariableIndex);
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
        for (IRClosure cl: scope.getClosures()) {
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


}
