package org.jruby.ir;

import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.ast.*;
import org.jruby.ast.types.INameNode;
import org.jruby.compiler.NotCompilableException;
import org.jruby.internal.runtime.methods.IRMethodArgs;
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
import org.jruby.util.ByteList;
import org.jruby.util.KeyValuePair;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import org.jruby.util.StringSupport;

import static org.jruby.ir.instructions.RuntimeHelperCall.Methods.*;

import static org.jruby.ir.operands.CurrentScope.*;
import static org.jruby.ir.operands.ScopeModule.*;

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
    static final Operand[] NO_ARGS = new Operand[]{};
    static final UnexecutableNil U_NIL = UnexecutableNil.U_NIL;

    public static final String USING_METHOD = "using";

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

        public IRLoop(IRScope s, IRLoop outerLoop) {
            container = s;
            parentLoop = outerLoop;
            loopStartLabel = s.getNewLabel("_LOOP_BEGIN");
            loopEndLabel   = s.getNewLabel("_LOOP_END");
            iterStartLabel = s.getNewLabel("_ITER_BEGIN");
            iterEndLabel   = s.getNewLabel("_ITER_END");
            loopResult     = s.createTemporaryVariable();
            s.setHasLoopsFlag();
        }
    }

    private static class RescueBlockInfo {
        RescueNode rescueNode;             // Rescue node for which we are tracking info
        Label      entryLabel;             // Entry of the rescue block
        Variable   savedExceptionVariable; // Variable that contains the saved $! variable
        IRLoop     innermostLoop;          // Innermost loop within which this rescue block is nested, if any

        public RescueBlockInfo(RescueNode n, Label l, Variable v, IRLoop loop) {
            rescueNode = n;
            entryLabel = l;
            savedExceptionVariable = v;
            innermostLoop = loop;
        }

        public void restoreException(IRBuilder b, IRLoop currLoop) {
            if (currLoop == innermostLoop) b.addInstr(new PutGlobalVarInstr("$!", savedExceptionVariable));
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
        Label    regionStart;
        Label    start;
        Label    end;
        Label    dummyRescueBlockLabel;
        Variable savedGlobalException;

        // Label of block that will rescue exceptions raised by ensure code
        Label    bodyRescuer;

        // Innermost loop within which this ensure block is nested, if any
        IRLoop   innermostLoop;

        // AST node for any associated rescue node in the case of begin-rescue-ensure-end block
        // Will be null in the case of begin-ensure-end block
        RescueNode matchingRescueNode;

        // This ensure block's instructions
        List<Instr> instrs;

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

    // Stack of nested rescue blocks -- this just tracks the start label of the blocks
    private Stack<RescueBlockInfo> activeRescueBlockStack = new Stack<>();

    // Stack of ensure blocks that are currently active
    private Stack<EnsureBlockInfo> activeEnsureBlockStack = new Stack<>();

    // Stack of ensure blocks whose bodies are being constructed
    private Stack<EnsureBlockInfo> ensureBodyBuildStack   = new Stack<>();

    // Combined stack of active rescue/ensure nestings -- required to properly set up
    // rescuers for ensure block bodies cloned into other regions -- those bodies are
    // rescued by the active rescuers at the point of definition rather than the point
    // of cloning.
    private Stack<Label> activeRescuers = new Stack<>();

    // Since we are processing ASTs, loop bodies are processed in depth-first manner
    // with outer loops encountered before inner loops, and inner loops finished before outer ones.
    //
    // So, we can keep track of loops in a loop stack which  keeps track of loops as they are encountered.
    // This lets us implement next/redo/break/retry easily for the non-closure cases.
    private Stack<IRLoop> loopStack = new Stack<>();

    private int _lastProcessedLineNum = -1;

    public IRLoop getCurrentLoop() {
        return loopStack.isEmpty() ? null : loopStack.peek();
    }

    protected IRBuilder parent;
    protected IRManager manager;
    protected IRScope scope;
    protected List<Instr> instructions;
    protected List<String> argumentDescriptions;

    public IRBuilder(IRManager manager, IRScope scope, IRBuilder parent) {
        this.manager = manager;
        this.scope = scope;
        this.parent = parent;
        instructions = new ArrayList<>(50);
        this.activeRescuers.push(Label.UNRESCUED_REGION_LABEL);
    }

    public void addArgumentDescription(IRMethodArgs.ArgType type, String name) {
        if (argumentDescriptions == null) argumentDescriptions = new ArrayList<>();

        argumentDescriptions.add(type.toString());
        argumentDescriptions.add(name);
    }

    public void addInstr(Instr instr) {
        // If we are building an ensure body, stash the instruction
        // in the ensure body's list. If not, add it to the scope directly.
        if (ensureBodyBuildStack.empty()) {
            if (instr instanceof ThreadPollInstr) scope.threadPollInstrsCount++;

            instr.computeScopeFlags(scope);

            if (hasListener()) manager.getIRScopeListener().addedInstr(scope, instr, instructions.size());

            instructions.add(instr);
        } else {
            ensureBodyBuildStack.peek().addInstr(instr);
        }
    }

    public void addInstrAtBeginning(Instr instr) {
        // If we are building an ensure body, stash the instruction
        // in the ensure body's list. If not, add it to the scope directly.
        if (ensureBodyBuildStack.empty()) {
            instr.computeScopeFlags(scope);

            if (hasListener()) manager.getIRScopeListener().addedInstr(scope, instr, 0);

            instructions.add(0, instr);
        } else {
            ensureBodyBuildStack.peek().addInstrAtBeginning(instr);
        }
    }

    public IRBuilder getNearestFlipVariableScopeBuilder() {
        IRBuilder current = this;

        while (current != null && !this.scope.isFlipScope()) {
            current = current.parent;
        }

        return current;
    }

    // Emit cloned ensure bodies by walking up the ensure block stack.
    // If we have been passed a loop value, only emit bodies that are nested within that loop.
    private void emitEnsureBlocks(IRLoop loop) {
        int n = activeEnsureBlockStack.size();
        EnsureBlockInfo[] ebArray = activeEnsureBlockStack.toArray(new EnsureBlockInfo[n]);
        for (int i = n-1; i >= 0; i--) {
            EnsureBlockInfo ebi = ebArray[i];

            // For "break" and "next" instructions, we only want to run
            // ensure blocks from the loops they are present in.
            if (loop != null && ebi.innermostLoop != loop) break;

            // SSS FIXME: Should $! be restored before or after the ensure block is run?
            if (ebi.savedGlobalException != null) {
                addInstr(new PutGlobalVarInstr("$!", ebi.savedGlobalException));
            }

            // Clone into host scope
            ebi.cloneIntoHostScope(this);
        }
    }

    private Operand buildOperand(Node node) throws NotCompilableException {
        switch (node.getNodeType()) {
            case ALIASNODE: return buildAlias((AliasNode) node);
            case ANDNODE: return buildAnd((AndNode) node);
            case ARGSCATNODE: return buildArgsCat((ArgsCatNode) node);
            case ARGSPUSHNODE: return buildArgsPush((ArgsPushNode) node);
            case ARRAYNODE: return buildArray((ArrayNode) node);
            case ATTRASSIGNNODE: return buildAttrAssign((AttrAssignNode) node);
            case BACKREFNODE: return buildBackref((BackRefNode) node);
            case BEGINNODE: return buildBegin((BeginNode) node);
            case BIGNUMNODE: return buildBignum((BignumNode) node);
            case BLOCKNODE: return buildBlock((BlockNode) node);
            case BREAKNODE: return buildBreak((BreakNode) node);
            case CALLNODE: return buildCall((CallNode) node);
            case CASENODE: return buildCase((CaseNode) node);
            case CLASSNODE: return buildClass((ClassNode) node);
            case CLASSVARNODE: return buildClassVar((ClassVarNode) node);
            case CLASSVARASGNNODE: return buildClassVarAsgn((ClassVarAsgnNode) node);
            case CLASSVARDECLNODE: return buildClassVarDecl((ClassVarDeclNode) node);
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
            case DREGEXPNODE: return buildDRegexp((DRegexpNode) node);
            case DSTRNODE: return buildDStr((DStrNode) node);
            case DSYMBOLNODE: return buildDSymbol((DSymbolNode) node);
            case DVARNODE: return buildDVar((DVarNode) node);
            case DXSTRNODE: return buildDXStr((DXStrNode) node);
            case ENCODINGNODE: return buildEncoding((EncodingNode)node);
            case ENSURENODE: return buildEnsureNode((EnsureNode) node);
            case EVSTRNODE: return buildEvStr((EvStrNode) node);
            case FALSENODE: return buildFalse();
            case FCALLNODE: return buildFCall((FCallNode) node);
            case FIXNUMNODE: return buildFixnum((FixnumNode) node);
            case FLIPNODE: return buildFlip((FlipNode) node);
            case FLOATNODE: return buildFloat((FloatNode) node);
            case FORNODE: return buildFor((ForNode) node);
            case GLOBALASGNNODE: return buildGlobalAsgn((GlobalAsgnNode) node);
            case GLOBALVARNODE: return buildGlobalVar((GlobalVarNode) node);
            case HASHNODE: return buildHash((HashNode) node);
            case IFNODE: return buildIf((IfNode) node);
            case INSTASGNNODE: return buildInstAsgn((InstAsgnNode) node);
            case INSTVARNODE: return buildInstVar((InstVarNode) node);
            case ITERNODE: return buildIter((IterNode) node);
            case LAMBDANODE: return buildLambda((LambdaNode)node);
            case LITERALNODE: return buildLiteral((LiteralNode) node);
            case LOCALASGNNODE: return buildLocalAsgn((LocalAsgnNode) node);
            case LOCALVARNODE: return buildLocalVar((LocalVarNode) node);
            case MATCH2NODE: return buildMatch2((Match2Node) node);
            case MATCH3NODE: return buildMatch3((Match3Node) node);
            case MATCHNODE: return buildMatch((MatchNode) node);
            case MODULENODE: return buildModule((ModuleNode) node);
            case MULTIPLEASGN19NODE: return buildMultipleAsgn19((MultipleAsgn19Node) node);
            case NEWLINENODE: return buildNewline((NewlineNode) node);
            case NEXTNODE: return buildNext((NextNode) node);
            case NTHREFNODE: return buildNthRef((NthRefNode) node);
            case NILNODE: return buildNil();
            case OPASGNANDNODE: return buildOpAsgnAnd((OpAsgnAndNode) node);
            case OPASGNNODE: return buildOpAsgn((OpAsgnNode) node);
            case OPASGNORNODE: return buildOpAsgnOr((OpAsgnOrNode) node);
            case OPELEMENTASGNNODE: return buildOpElementAsgn((OpElementAsgnNode) node);
            case ORNODE: return buildOr((OrNode) node);
            case PREEXENODE: return buildPreExe((PreExeNode) node);
            case POSTEXENODE: return buildPostExe((PostExeNode) node);
            case RATIONALNODE: return buildRational((RationalNode) node);
            case REDONODE: return buildRedo();
            case REGEXPNODE: return buildRegexp((RegexpNode) node);
            case RESCUEBODYNODE:
                throw new NotCompilableException("rescue body is handled by rescue compilation at: " + node.getPosition());
            case RESCUENODE: return buildRescue((RescueNode) node);
            case RETRYNODE: return buildRetry();
            case RETURNNODE: return buildReturn((ReturnNode) node);
            case ROOTNODE:
                throw new NotCompilableException("Use buildRoot(); Root node at: " + node.getPosition());
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
            case VCALLNODE: return buildVCall((VCallNode) node);
            case WHILENODE: return buildWhile((WhileNode) node);
            case WHENNODE: assert false : "When nodes are handled by case node compilation."; return null;
            case XSTRNODE: return buildXStr((XStrNode) node);
            case YIELDNODE: return buildYield((YieldNode) node);
            case ZARRAYNODE: return buildZArray();
            case ZSUPERNODE: return buildZSuper((ZSuperNode) node);
            default: throw new NotCompilableException("Unknown node encountered in builder: " + node.getClass());
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

    public Node skipOverNewlines(Node n) {
        if (n.getNodeType() == NodeType.NEWLINENODE) {
            // Do not emit multiple line number instrs for the same line
            int currLineNum = n.getPosition().getLine();
            if (currLineNum != _lastProcessedLineNum) {
                if (RubyInstanceConfig.FULL_TRACE_ENABLED) {
                    addInstr(new TraceInstr(RubyEvent.LINE, methodNameFor(), getFileName(), currLineNum));
                }
               addInstr(new LineNumberInstr(currLineNum));
               _lastProcessedLineNum = currLineNum;
            }
        }

        while (n.getNodeType() == NodeType.NEWLINENODE) {
            n = ((NewlineNode) n).getNextNode();
        }

        return n;
    }

    public Operand build(Node node) {
        if (node == null) return null;

        if (hasListener()) manager.getIRScopeListener().startBuildOperand(node, scope);

        Operand operand = buildOperand(node);

        if (hasListener()) manager.getIRScopeListener().endBuildOperand(node, scope, operand);

        return operand;
    }

    private InterpreterContext buildLambdaInner(LambdaNode node) {
        prepareImplicitState();                                    // recv_self, add frame block, etc)
        addCurrentScopeAndModule();                                // %current_scope/%current_module

        receiveBlockArgs(node);

        Operand closureRetVal = node.getBody() == null ? manager.getNil() : build(node.getBody());

        // can be U_NIL if the node is an if node with returns in both branches.
        if (closureRetVal != U_NIL) addInstr(new ReturnInstr(closureRetVal));

        handleBreakAndReturnsInLambdas();

        return scope.allocateInterpreterContext(instructions);
    }

    public Operand buildLambda(LambdaNode node) {
        IRClosure closure = new IRClosure(manager, scope, node.getPosition().getLine(), node.getScope(), Signature.from(node));

        // Create a new nested builder to ensure this gets its own IR builder state like the ensure block stack
        newIRBuilder(manager, closure).buildLambdaInner(node);

        Variable lambda = createTemporaryVariable();
        WrappedIRClosure lambdaBody = new WrappedIRClosure(closure.getSelf(), closure);
        addInstr(new BuildLambdaInstr(lambda, lambdaBody, node.getPosition()));
        return lambda;
    }

    public Operand buildEncoding(EncodingNode node) {
        Variable ret = createTemporaryVariable();
        addInstr(new GetEncodingInstr(ret, node.getEncoding()));
        return ret;
    }

    // Non-arg masgn
    public Operand buildMultipleAsgn19(MultipleAsgn19Node multipleAsgnNode) {
        Operand  values = build(multipleAsgnNode.getValueNode());
        Variable ret = getValueInTemporaryVariable(values);
        Variable tmp = createTemporaryVariable();
        addInstr(new ToAryInstr(tmp, ret));
        buildMultipleAsgn19Assignment(multipleAsgnNode, null, tmp);
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
                for (Node n: args.childNodes()) {
                    last = buildWithOrder(n, containsAssignment);
                    argsList.add(last);
                }
                return last;
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

        throw new NotCompilableException("Invalid node for attrassign call args: " + args.getClass().getSimpleName() + ":" + args.getPosition());
    }

    protected Operand[] buildCallArgs(Node args) {
        switch (args.getNodeType()) {
            case ARGSCATNODE:
            case ARGSPUSHNODE:
                return new Operand[] { new Splat(build(args)) };
            case ARRAYNODE: {
                List<Node> children = args.childNodes();
                int numberOfArgs = children.size();
                Operand[] builtArgs = new Operand[numberOfArgs];
                boolean hasAssignments = args.containsVariableAssignment();

                for (int i = 0; i < numberOfArgs; i++) {
                    builtArgs[i] = buildWithOrder(children.get(i), hasAssignments);
                }
                return builtArgs;
            }
            case SPLATNODE:
                return new Operand[] { new Splat(buildSplat((SplatNode)args)) };
        }

        throw new NotCompilableException("Invalid node for call args: " + args.getClass().getSimpleName() + ":" + args.getPosition());
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

    // Non-arg masgn (actually a nested masgn)
    public void buildVersionSpecificAssignment(Node node, Variable v) {
        switch (node.getNodeType()) {
        case MULTIPLEASGN19NODE: {
            Variable tmp = createTemporaryVariable();
            addInstr(new ToAryInstr(tmp, v));
            buildMultipleAsgn19Assignment((MultipleAsgn19Node)node, null, tmp);
            break;
        }
        default:
            throw new NotCompilableException("Can't build assignment node: " + node);
        }
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
            case CLASSVARDECLNODE:
                addInstr(new PutClassVariableInstr(classVarDeclarationContainer(), ((ClassVarDeclNode)node).getName(), rhsVal));
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
                throw new NotCompilableException("Shouldn't get here; zeroarg does not do assignment: " + node);
            default:
                buildVersionSpecificAssignment(node, rhsVal);
        }
    }

    protected LocalVariable getBlockArgVariable(String name, int depth) {
        if (!(scope instanceof IRFor)) throw new NotCompilableException("Cannot ask for block-arg variable in 1.9 mode");

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
        if (!(scope instanceof IRFor)) throw new NotCompilableException("Should not have come here for block args assignment in 1.9 mode: " + node);

        // Argh!  For-loop bodies and regular iterators are different in terms of block-args!
        switch (node.getNodeType()) {
            case MULTIPLEASGN19NODE: {
                ListNode sourceArray = ((MultipleAsgn19Node) node).getPre();
                int i = 0;
                for (Node an: sourceArray.childNodes()) {
                    // Use 1.8 mode version for this
                    buildBlockArgsAssignment(an, null, i, false);
                    i++;
                }
                break;
            }
            default:
                throw new NotCompilableException("Can't build assignment node: " + node);
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
            case CLASSVARDECLNODE:
                v = createTemporaryVariable();
                receiveBlockArg(v, argsArray, argIndex, isSplat);
                addInstr(new PutClassVariableInstr(classVarDeclarationContainer(), ((ClassVarDeclNode)node).getName(), v));
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
                int depth = localVariable.getDepth();
                v = getBlockArgVariable(localVariable.getName(), depth);
                receiveBlockArg(v, argsArray, argIndex, isSplat);
                break;
            }
            case ZEROARGNODE:
                throw new NotCompilableException("Shouldn't get here; zeroarg does not do assignment: " + node);
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
            addInstr(BEQInstr.create(v1, manager.getFalse(), l));
            Operand v2 = build(andNode.getSecondNode());
            addInstr(new CopyInstr(ret, v2));
            addInstr(new LabelInstr(l));
            return ret;
        }
    }

    public Operand buildArray(ArrayNode node) {
        List<Operand> elts = new ArrayList<>();
        boolean containsAssignments = node.containsVariableAssignment();
        for (Node e: node.childNodes()) {
            elts.add(buildWithOrder(e, containsAssignments));
        }

        return copyAndReturnValue(new Array(elts));
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

    private Operand buildAttrAssign(final AttrAssignNode attrAssignNode) {
        boolean containsAssignment = attrAssignNode.containsVariableAssignment();
        Operand obj = buildWithOrder(attrAssignNode.getReceiverNode(), containsAssignment);
        List<Operand> args = new ArrayList<>();
        Node argsNode = attrAssignNode.getArgsNode();
        Operand lastArg = buildAttrAssignCallArgs(args, argsNode, containsAssignment);
        addInstr(AttrAssignInstr.create(obj, attrAssignNode.getName(), args.toArray(new Operand[args.size()])));
        return lastArg;
    }

    public Operand buildAttrAssignAssignment(Node node, Operand value) {
        final AttrAssignNode attrAssignNode = (AttrAssignNode) node;
        Operand obj = build(attrAssignNode.getReceiverNode());
        Operand[] args = setupCallArgs(attrAssignNode.getArgsNode());
        args = addArg(args, value);
        addInstr(AttrAssignInstr.create(obj, attrAssignNode.getName(), args));
        return value;
    }

    public Operand buildBackref(BackRefNode node) {
        // SSS FIXME: Required? Verify with Tom/Charlie
        return copyAndReturnValue(new Backref(node.getType()));
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
        for (Node child : node.childNodes()) {
            retVal = build(child);
        }

        // Value of the last expression in the block
        return retVal;
    }

    public Operand buildBreak(BreakNode breakNode) {
        IRLoop currLoop = getCurrentLoop();

        Operand rv = build(breakNode.getValueNode());
        // If we have ensure blocks, have to run those first!
        if (!activeEnsureBlockStack.empty()) {
            emitEnsureBlocks(currLoop);
        } else if (!activeRescueBlockStack.empty()) {
            activeRescueBlockStack.peek().restoreException(this, currLoop);
        }

        if (currLoop != null) {
            addInstr(new CopyInstr(currLoop.loopResult, rv));
            addInstr(new JumpInstr(currLoop.loopEndLabel));
        } else {
            if (scope instanceof IRClosure) {
                // This lexical scope value is only used (and valid) in regular block contexts.
                // If this instruction is executed in a Proc or Lambda context, the lexical scope value is useless.
                IRScope returnScope = scope.getLexicalParent();
                // In 1.9 and later modes, no breaks from evals
                if (scope instanceof IREvalScript || returnScope == null) {
                    addInstr(new ThrowExceptionInstr(IRException.BREAK_LocalJumpError));
                } else {
                    addInstr(new BreakInstr(rv, returnScope.getName()));
                }
            } else {
                // We are not in a closure or a loop => bad break instr!
                addInstr(new ThrowExceptionInstr(IRException.BREAK_LocalJumpError));
            }
        }

        // Once the break instruction executes, control exits this scope
        return U_NIL;
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

        if (RubyInstanceConfig.FULL_TRACE_ENABLED) {
            addInstr(new TraceInstr(RubyEvent.RETURN, getName(), getFileName(), -1));
        }

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
            !(((WrappedIRClosure)block).getClosure()).flags.contains(IRFlags.HAS_BREAK_INSTRS)) {
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
        addInstr(new RuntimeHelperCall(callResult, HANDLE_PROPAGATE_BREAK, new Operand[]{exc} ));

        // End
        addInstr(new LabelInstr(rEndLabel));

        return callResult;
    }

    // Wrap call in a rescue handler that catches the IRBreakJump
    private void receiveBreakException(Operand block, final CallInstr callInstr) {
        receiveBreakException(block, new CodeBlock() { public Operand run() { addInstr(callInstr); return callInstr.getResult(); } });
    }

    public Operand buildCall(CallNode callNode) {
        Node callArgsNode = callNode.getArgsNode();
        Node receiverNode = callNode.getReceiverNode();

        // Frozen string optimization: check for "string".freeze
        if (receiverNode instanceof StrNode && callNode.getName().equals("freeze")) {
            StrNode asString = (StrNode) receiverNode;
            return new FrozenString(asString.getValue(), asString.getCodeRange());
        }

        // Though you might be tempted to move this build into the CallInstr as:
        //    new Callinstr( ... , build(receiverNode, s), ...)
        // that is incorrect IR because the receiver has to be built *before* call arguments are built
        // to preserve expected code execution order
        Operand receiver = buildWithOrder(receiverNode, callNode.containsVariableAssignment());
        Operand[] args       = setupCallArgs(callArgsNode);
        Operand   block      = setupCallClosure(callNode.getIterNode());
        Variable  callResult = createTemporaryVariable();
        CallInstr callInstr  = CallInstr.create(scope, callResult, callNode.getName(), receiver, args, block);

        // This is to support the ugly Proc.new with no block, which must see caller's frame
        if ( callNode.getName().equals("new") &&
             receiverNode instanceof ConstNode &&
             ((ConstNode)receiverNode).getName().equals("Proc")) {
            callInstr.setProcNew(true);
        }

        receiveBreakException(block, callInstr);
        return callResult;
    }

    public Operand buildCase(CaseNode caseNode) {
        // get the incoming case value
        Operand value = build(caseNode.getCaseNode());

        // This is for handling case statements without a value (see example below)
        //   case
        //     when true <blah>
        //     when false <blah>
        //   end
        if (value == null) value = UndefinedValue.UNDEFINED;

        Label     endLabel  = getNewLabel();
        boolean   hasElse   = (caseNode.getElseNode() != null);
        Label     elseLabel = getNewLabel();
        Variable  result    = createTemporaryVariable();

        List<Label> labels = new ArrayList<>();
        Map<Label, Node> bodies = new HashMap<>();

        // build each "when"
        for (Node aCase : caseNode.getCases().childNodes()) {
            WhenNode whenNode = (WhenNode)aCase;
            Label bodyLabel = getNewLabel();

            Variable eqqResult = createTemporaryVariable();
            labels.add(bodyLabel);
            Operand v1, v2;
            if (whenNode.getExpressionNodes() instanceof ListNode
                    // DNode produces a proper result, so we don't want the special ListNode handling below
                    // FIXME: This is obviously gross, and we need a better way to filter out non-expression ListNode here
                    // See GH #2423
                    && !(whenNode.getExpressionNodes() instanceof DNode)) {
                // Note about refactoring:
                // - BEQInstr has a quick implementation when the second operand is a boolean literal
                //   If it can be fixed to do this even on the first operand, we can switch around
                //   v1 and v2 in the UndefinedValue scenario and DRY out this code.
                // - Even with this asymmetric implementation of BEQInstr, you might be tempted to
                //   switch around v1 and v2 in the else case.  But, that is equivalent to this Ruby code change:
                //      (v1 == value) instead of (value == v1)
                //   It seems that they should be identical, but the first one is v1.==(value) and the second one is
                //   value.==(v1).  This is just fine *if* the Ruby programmer has implemented an algebraically
                //   symmetric "==" method on those objects.  If not, then, the results might be unexpected where the
                //   code (intentionally or otherwise) relies on this asymmetry of "==".  While it could be argued
                //   that this a Ruby code bug, we will just try to preserve the order of the == check as it appears
                //   in the Ruby code.
                if (value == UndefinedValue.UNDEFINED)  {
                    v1 = build(whenNode.getExpressionNodes());
                    v2 = manager.getTrue();
                } else {
                    v1 = value;
                    v2 = build(whenNode.getExpressionNodes());
                }
            } else {
                addInstr(new EQQInstr(eqqResult, build(whenNode.getExpressionNodes()), value));
                v1 = eqqResult;
                v2 = manager.getTrue();
            }
            addInstr(BEQInstr.create(v1, v2, bodyLabel));

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
        Node superNode = classNode.getSuperNode();
        Colon3Node cpath = classNode.getCPath();
        Operand superClass = (superNode == null) ? null : build(superNode);
        String className = cpath.getName();
        Operand container = getContainerFromCPath(cpath);
        IRClassBody body = new IRClassBody(manager, scope, className, classNode.getPosition().getLine(), classNode.getScope());
        Variable classVar = addResultInstr(new DefineClassInstr(createTemporaryVariable(), body, container, superClass));

        Variable processBodyResult = addResultInstr(new ProcessModuleBodyInstr(createTemporaryVariable(), classVar, NullBlock.INSTANCE));
        newIRBuilder(manager, body).buildModuleOrClassBody(classNode.getBodyNode(), classNode.getPosition().getLine());
        return processBodyResult;
    }

    // class Foo; class << self; end; end
    // Here, the class << self declaration is in Foo's body.
    // Foo is the class in whose context this is being defined.
    public Operand buildSClass(SClassNode sclassNode) {
        Operand receiver = build(sclassNode.getReceiverNode());
        IRModuleBody body = new IRMetaClassBody(manager, scope, manager.getMetaClassName(), sclassNode.getPosition().getLine(), sclassNode.getScope());
        Variable sClassVar = addResultInstr(new DefineMetaClassInstr(createTemporaryVariable(), receiver, body));

        // sclass bodies inherit the block of their containing method
        Variable processBodyResult = addResultInstr(new ProcessModuleBodyInstr(createTemporaryVariable(), sClassVar, scope.getYieldClosureVariable()));
        newIRBuilder(manager, body).buildModuleOrClassBody(sclassNode.getBodyNode(), sclassNode.getPosition().getLine());
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

    // ClassVarDecl node is assignment outside method/closure scope (top-level, class, module)
    //
    // class C
    //   @@c = 1
    // end
    public Operand buildClassVarDecl(final ClassVarDeclNode classVarDeclNode) {
        Operand val = build(classVarDeclNode.getValueNode());
        addInstr(new PutClassVariableInstr(classVarDeclarationContainer(), classVarDeclNode.getName(), val));
        return val;
    }

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
            cvarScope = cvarScope.getLexicalParent();
            n++;
        }

        if ((cvarScope != null) && cvarScope.isNonSingletonClassBody()) {
            return ScopeModule.ModuleFor(n);
        } else {
            return addResultInstr(new GetClassVarContainerModuleInstr(createTemporaryVariable(),
                    scope.getCurrentScopeVariable(), declContext ? null : buildSelf()));
        }
    }

    public Operand buildConstDecl(ConstDeclNode node) {
        return buildConstDeclAssignment(node, build(node.getValueNode()));
    }

    private Operand findContainerModule() {
        int nearestModuleBodyDepth = scope.getNearestModuleReferencingScopeDepth();
        return (nearestModuleBodyDepth == -1) ? scope.getCurrentModuleVariable() : ScopeModule.ModuleFor(nearestModuleBodyDepth);
    }

    private Operand startingSearchScope() {
        int nearestModuleBodyDepth = scope.getNearestModuleReferencingScopeDepth();
        return nearestModuleBodyDepth == -1 ? scope.getCurrentScopeVariable() : CurrentScope.ScopeFor(nearestModuleBodyDepth);
    }

    public Operand buildConstDeclAssignment(ConstDeclNode constDeclNode, Operand val) {
        Node constNode = constDeclNode.getConstNode();

        if (constNode == null) {
            addInstr(new PutConstInstr(findContainerModule(), constDeclNode.getName(), val));
        } else if (constNode.getNodeType() == NodeType.COLON2NODE) {
            Operand module = build(((Colon2Node) constNode).getLeftNode());
            addInstr(new PutConstInstr(module, constDeclNode.getName(), val));
        } else { // colon3, assign in Object
            addInstr(new PutConstInstr(new ObjectClass(), constDeclNode.getName(), val));
        }

        return val;
    }

    private void genInheritanceSearchInstrs(Operand startingModule, Variable constVal, Label foundLabel, boolean noPrivateConstants, String name) {
        addInstr(new InheritanceSearchConstInstr(constVal, startingModule, name, noPrivateConstants));
        addInstr(BNEInstr.create(foundLabel, constVal, UndefinedValue.UNDEFINED));
        addInstr(new ConstMissingInstr(constVal, startingModule, name));
        addInstr(new LabelInstr(foundLabel));
    }

    private Operand searchConstInInheritanceHierarchy(Operand startingModule, String name) {
        Variable constVal = createTemporaryVariable();
        genInheritanceSearchInstrs(startingModule, constVal, getNewLabel(), true, name);
        return constVal;
    }

    private Operand searchConst(String name) {
        final boolean noPrivateConstants = false;
        Variable v = createTemporaryVariable();
/**
 * SSS FIXME: Went back to a single instruction for now.
 *
 * Do not split search into lexical-search, inheritance-search, and const-missing instrs.
 *
        Label foundLabel = getNewLabel();
        addInstr(new LexicalSearchConstInstr(v, startingSearchScope(s), name));
        addInstr(BNEInstr.create(v, UndefinedValue.UNDEFINED, foundLabel));
        genInheritanceSearchInstrs(s, findContainerModule(startingScope), v, foundLabel, noPrivateConstants, name);
**/
        addInstr(new SearchConstInstr(v, name, startingSearchScope(), noPrivateConstants));
        return v;
    }

    public Operand buildColon2(final Colon2Node iVisited) {
        Node leftNode = iVisited.getLeftNode();
        final String name = iVisited.getName();

        // Colon2ImplicitNode
        if (leftNode == null) return searchConst(name);

        // Colon2ConstNode
        // 1. Load the module first (lhs of node)
        // 2. Then load the constant from the module
        Operand module = build(leftNode);
        return searchConstInInheritanceHierarchy(module, name);
    }

    public Operand buildColon3(Colon3Node node) {
        return searchConstInInheritanceHierarchy(new ObjectClass(), node.getName());
    }

    public Operand buildComplex(ComplexNode node) {
        return new Complex((ImmutableLiteral) build(node.getNumber()));
    }

    interface CodeBlock {
        public Operand run();
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
        addInstr(new ExceptionRegionEndMarkerInstr());
        addInstr(new JumpInstr(rEndLabel));

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
        addInstr(new InheritanceSearchConstInstr(excType, new ObjectClass(), "Exception", false));
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
        node = skipOverNewlines(node);

        // FIXME: Do we still have MASGN and MASGN19?
        switch (node.getNodeType()) {
        case CLASSVARASGNNODE: case CLASSVARDECLNODE: case CONSTDECLNODE:
        case DASGNNODE: case GLOBALASGNNODE: case LOCALASGNNODE: case MULTIPLEASGNNODE:
        case MULTIPLEASGN19NODE: case OPASGNNODE: case OPASGNANDNODE: case OPASGNORNODE:
        case OPELEMENTASGNNODE: case INSTASGNNODE:
            return new FrozenString("assignment");
        case ORNODE: case ANDNODE:
            return new FrozenString("expression");
        case FALSENODE:
            return new FrozenString("false");
        case LOCALVARNODE: case DVARNODE:
            return new FrozenString("local-variable");
        case MATCH2NODE: case MATCH3NODE:
            return new FrozenString("method");
        case NILNODE:
            return new FrozenString("nil");
        case SELFNODE:
            return new FrozenString("self");
        case TRUENODE:
            return new FrozenString("true");
        case DREGEXPNODE: case DSTRNODE: {
            final Node dNode = node;

            // protected code
            CodeBlock protectedCode = new CodeBlock() {
                public Operand run() {
                    build(dNode);
                    // always an expression as long as we get through here without an exception!
                    return new FrozenString("expression");
                }
            };
            // rescue block
            CodeBlock rescueBlock = new CodeBlock() {
                public Operand run() { return manager.getNil(); } // Nothing to do if we got an exception
            };

            // Try verifying definition, and if we get an JumpException exception, process it with the rescue block above
            Operand v = protectCodeWithRescue(protectedCode, rescueBlock);
            Label doneLabel = getNewLabel();
            Variable tmpVar = getValueInTemporaryVariable(v);
            addInstr(BNEInstr.create(doneLabel, tmpVar, manager.getNil()));
            addInstr(new CopyInstr(tmpVar, new FrozenString("expression")));
            addInstr(new LabelInstr(doneLabel));

            return tmpVar;
        }
        case ARRAYNODE: { // If all elts of array are defined the array is as well
            ArrayNode array = (ArrayNode) node;
            Label undefLabel = getNewLabel();
            Label doneLabel = getNewLabel();

            Variable tmpVar = createTemporaryVariable();
            for (Node elt: array.childNodes()) {
                Operand result = buildGetDefinition(elt);

                addInstr(BEQInstr.create(result, manager.getNil(), undefLabel));
            }

            addInstr(new CopyInstr(tmpVar, new FrozenString("expression")));
            addInstr(new JumpInstr(doneLabel));
            addInstr(new LabelInstr(undefLabel));
            addInstr(new CopyInstr(tmpVar, manager.getNil()));
            addInstr(new LabelInstr(doneLabel));

            return tmpVar;
        }
        case BACKREFNODE:
            return addResultInstr(new RuntimeHelperCall(createTemporaryVariable(), IS_DEFINED_BACKREF, Operand.EMPTY_ARRAY));
        case GLOBALVARNODE:
            return addResultInstr(new RuntimeHelperCall(createTemporaryVariable(), IS_DEFINED_GLOBAL,
                    new Operand[] { new StringLiteral(((GlobalVarNode) node).getName()) }));
        case NTHREFNODE: {
            return addResultInstr(new RuntimeHelperCall(createTemporaryVariable(), IS_DEFINED_NTH_REF,
                    new Operand[] { new Fixnum(((NthRefNode) node).getMatchNumber()) }));
        }
        case INSTVARNODE:
            return addResultInstr(new RuntimeHelperCall(createTemporaryVariable(), IS_DEFINED_INSTANCE_VAR,
                    new Operand[] { buildSelf(), new StringLiteral(((InstVarNode) node).getName()) }));
        case CLASSVARNODE:
            return addResultInstr(new RuntimeHelperCall(createTemporaryVariable(), IS_DEFINED_CLASS_VAR,
                    new Operand[]{classVarDefinitionContainer(), new StringLiteral(((ClassVarNode) node).getName())}));
        case SUPERNODE: {
            Label undefLabel = getNewLabel();
            Variable tmpVar  = addResultInstr(new RuntimeHelperCall(createTemporaryVariable(), IS_DEFINED_SUPER,
                    new Operand[] { buildSelf() }));
            addInstr(BEQInstr.create(tmpVar, manager.getNil(), undefLabel));
            Operand superDefnVal = buildGetArgumentDefinition(((SuperNode) node).getArgsNode(), "super");
            return buildDefnCheckIfThenPaths(undefLabel, superDefnVal);
        }
        case VCALLNODE:
            return addResultInstr(new RuntimeHelperCall(createTemporaryVariable(), IS_DEFINED_METHOD,
                    new Operand[] { buildSelf(), new StringLiteral(((VCallNode) node).getName()), manager.getFalse()}));
        case YIELDNODE:
            return buildDefinitionCheck(new BlockGivenInstr(createTemporaryVariable(), scope.getYieldClosureVariable()), "yield");
        case ZSUPERNODE:
            return addResultInstr(new RuntimeHelperCall(createTemporaryVariable(), IS_DEFINED_SUPER,
                    new Operand[] { buildSelf() } ));
        case CONSTNODE: {
            Label defLabel = getNewLabel();
            Label doneLabel = getNewLabel();
            Variable tmpVar  = createTemporaryVariable();
            String constName = ((ConstNode) node).getName();
            addInstr(new LexicalSearchConstInstr(tmpVar, startingSearchScope(), constName));
            addInstr(BNEInstr.create(defLabel, tmpVar, UndefinedValue.UNDEFINED));
            addInstr(new InheritanceSearchConstInstr(tmpVar, findContainerModule(), constName, false)); // SSS FIXME: should this be the current-module var or something else?
            addInstr(BNEInstr.create(defLabel, tmpVar, UndefinedValue.UNDEFINED));
            addInstr(new CopyInstr(tmpVar, manager.getNil()));
            addInstr(new JumpInstr(doneLabel));
            addInstr(new LabelInstr(defLabel));
            addInstr(new CopyInstr(tmpVar, new FrozenString("constant")));
            addInstr(new LabelInstr(doneLabel));
            return tmpVar;
        }
        case COLON3NODE: case COLON2NODE: {
            // SSS FIXME: Is there a reason to do this all with low-level IR?
            // Can't this all be folded into a Java method that would be part
            // of the runtime library, which then can be used by buildDefinitionCheck method above?
            // This runtime library would be used both by the interpreter & the compiled code!

            final Colon3Node colon = (Colon3Node) node;
            final String name = colon.getName();
            final Variable errInfo = createTemporaryVariable();

            // store previous exception for restoration if we rescue something
            addInstr(new GetErrorInfoInstr(errInfo));

            CodeBlock protectedCode = new CodeBlock() {
                public Operand run() {
                    Operand v = colon instanceof Colon2Node ?
                            build(((Colon2Node)colon).getLeftNode()) : new ObjectClass();

                    Variable tmpVar = createTemporaryVariable();
                    addInstr(new RuntimeHelperCall(tmpVar, IS_DEFINED_CONSTANT_OR_METHOD, new Operand[] {v, new FrozenString(name)}));
                    return tmpVar;
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
            Variable tmpVar = addResultInstr(new RuntimeHelperCall(createTemporaryVariable(), IS_DEFINED_METHOD,
                    new Operand[]{buildSelf(), new StringLiteral(((FCallNode) node).getName()), manager.getFalse()}));
            addInstr(BEQInstr.create(tmpVar, manager.getNil(), undefLabel));
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
                    addInstr(BEQInstr.create(receiverDefn, manager.getNil(), undefLabel));
                    Variable tmpVar = createTemporaryVariable();
                    addInstr(new RuntimeHelperCall(tmpVar, IS_DEFINED_CALL,
                            new Operand[]{build(callNode.getReceiverNode()), new StringLiteral(callNode.getName())}));
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
                    addInstr(BEQInstr.create(receiverDefn, manager.getNil(), undefLabel));
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
                    addInstr(new RuntimeHelperCall(tmpVar, IS_DEFINED_METHOD,
                            new Operand[] { receiver, new StringLiteral(attrAssign.getName()), manager.getTrue() }));
                    addInstr(BEQInstr.create(tmpVar, manager.getNil(), undefLabel));
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
        addInstr(BEQInstr.create(definedInstr.getResult(), manager.getFalse(), undefLabel));
        return buildDefnCheckIfThenPaths(undefLabel, new FrozenString(definedReturnValue));
    }

    public Operand buildGetArgumentDefinition(final Node node, String type) {
        if (node == null) return new StringLiteral(type);

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
                    addInstr(BEQInstr.create(def, manager.getNil(), failLabel));
                }
            }
        } else {
            Operand def = buildGetDefinition(node);
            if (def == manager.getNil()) { // Optimization!
                rv = manager.getNil();
            } else if (!def.hasKnownValue()) { // Optimization!
                failPathReqd = true;
                addInstr(BEQInstr.create(def, manager.getNil(), failLabel));
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
    protected InterpreterContext defineMethodInner(MethodDefNode defNode, IRScope parent) {
        if (RubyInstanceConfig.FULL_TRACE_ENABLED) {
            addInstr(new TraceInstr(RubyEvent.CALL, getName(), getFileName(), scope.getLineNumber()));
        }

        prepareImplicitState();                                    // recv_self, add frame block, etc)

        // These instructions need to be toward the top of the method because they may both be needed for processing
        // optional arguments as in def foo(a = Object).
        // Set %current_scope = <current-scope>
        // Set %current_module = isInstanceMethod ? %self.metaclass : %self
        int nearestScopeDepth = parent.getNearestModuleReferencingScopeDepth();
        addInstr(new CopyInstr(scope.getCurrentScopeVariable(), CurrentScope.ScopeFor(nearestScopeDepth == -1 ? 1 : nearestScopeDepth)));
        addInstr(new CopyInstr(scope.getCurrentModuleVariable(), ScopeModule.ModuleFor(nearestScopeDepth == -1 ? 1 : nearestScopeDepth)));

        // Build IR for arguments (including the block arg)
        receiveMethodArgs(defNode.getArgsNode());

        // Build IR for body
        Operand rv = build(defNode.getBodyNode());

        if (RubyInstanceConfig.FULL_TRACE_ENABLED) {
            addInstr(new TraceInstr(RubyEvent.RETURN, getName(), getFileName(), -1));
        }

        if (rv != null) addInstr(new ReturnInstr(rv));

        scope.computeScopeFlagsEarly(instructions);
        // If the method can receive non-local returns
        if (scope.canReceiveNonlocalReturns()) handleNonlocalReturnInMethod();

        String[] argDesc;
        if (argumentDescriptions == null) {
            argDesc = NO_ARG_DESCS;
        } else {
            argDesc = new String[argumentDescriptions.size()];
            argumentDescriptions.toArray(argDesc);
        }

        ((IRMethod) scope).setArgDesc(argDesc);

        return scope.allocateInterpreterContext(instructions);
    }

    static final String[] NO_ARG_DESCS = new String[0];

    private IRMethod defineNewMethod(MethodDefNode defNode, boolean isInstanceMethod) {
        return new IRMethod(manager, scope, defNode, defNode.getName(), isInstanceMethod, defNode.getPosition().getLine(), defNode.getScope());

        //return newIRBuilder(manager).defineMethodInner(defNode, method, parent);
    }

    public Operand buildDefn(MethodDefNode node) { // Instance method
        IRMethod method = defineNewMethod(node, true);
        addInstr(new DefineInstanceMethodInstr(method));
        // FIXME: Method name should save encoding
        return new Symbol(method.getName(), ASCIIEncoding.INSTANCE);
    }

    public Operand buildDefs(DefsNode node) { // Class method
        Operand container =  build(node.getReceiverNode());
        IRMethod method = defineNewMethod(node, false);
        addInstr(new DefineClassMethodInstr(container, method));
        // FIXME: Method name should save encoding
        return new Symbol(method.getName(), ASCIIEncoding.INSTANCE);
    }

    protected LocalVariable getArgVariable(String name, int depth) {
        // For non-loops, this name will override any name that exists in outer scopes
        return scope instanceof IRFor ? getLocalVariable(name, depth) : getNewLocalVariable(name, 0);
    }

    private void addArgReceiveInstr(Variable v, int argIndex, boolean post, int numPreReqd, int numPostRead) {
        if (post) {
            addInstr(new ReceivePostReqdArgInstr(v, argIndex, numPreReqd, numPostRead));
        } else {
            addInstr(new ReceivePreReqdArgInstr(v, argIndex));
        }
    }

    public void receiveRequiredArg(Node node, int argIndex, boolean post, int numPreReqd, int numPostRead) {
        switch (node.getNodeType()) {
            case ARGUMENTNODE: {
                ArgumentNode a = (ArgumentNode)node;
                String argName = a.getName();
                if (scope instanceof IRMethod) addArgumentDescription(IRMethodArgs.ArgType.req, argName);
                // Ignore duplicate "_" args in blocks
                // (duplicate _ args are named "_$0")
                if (!argName.equals("_$0")) {
                    addArgReceiveInstr(getNewLocalVariable(argName, 0), argIndex, post, numPreReqd, numPostRead);
                }
                break;
            }
            case MULTIPLEASGN19NODE: {
                MultipleAsgn19Node childNode = (MultipleAsgn19Node) node;
                Variable v = createTemporaryVariable();
                addArgReceiveInstr(v, argIndex, post, numPreReqd, numPostRead);
                if (scope instanceof IRMethod) addArgumentDescription(IRMethodArgs.ArgType.req, "");
                Variable tmp = createTemporaryVariable();
                addInstr(new ToAryInstr(tmp, v));
                buildMultipleAsgn19Assignment(childNode, tmp, null);
                break;
            }
            default: throw new NotCompilableException("Can't build assignment node: " + node);
        }
    }

    protected void receiveNonBlockArgs(final ArgsNode argsNode) {
        final int numPreReqd = argsNode.getPreCount();
        final int numPostReqd = argsNode.getPostCount();
        final int required = argsNode.getRequiredArgsCount(); // numPreReqd + numPostReqd
        int opt = argsNode.getOptionalArgsCount();
        int rest = argsNode.getRestArg();

        scope.getStaticScope().setArities(required, opt, rest);
        KeywordRestArgNode keyRest = argsNode.getKeyRest();

        // For closures, we don't need the check arity call
        if (scope instanceof IRMethod) {
            // Expensive to do this explicitly?  But, two advantages:
            // (a) on inlining, we'll be able to get rid of these checks in almost every case.
            // (b) compiler to bytecode will anyway generate this and this is explicit.
            // For now, we are going explicit instruction route.
            // But later, perhaps can make this implicit in the method setup preamble?

            addInstr(new CheckArityInstr(required, opt, rest, argsNode.hasKwargs(),
                    keyRest == null ? -1 : keyRest.getIndex()));
        } else if (scope instanceof IRClosure && argsNode.hasKwargs()) {
            // FIXME: This is added to check for kwargs correctness but bypass regular correctness.
            // Any other arity checking currently happens within Java code somewhere (RubyProc.call?)
            addInstr(new CheckArityInstr(required, opt, rest, argsNode.hasKwargs(),
                    keyRest == null ? -1 : keyRest.getIndex()));
        }

        // Other args begin at index 0
        int argIndex = 0;

        // Pre(-opt and rest) required args
        ListNode preArgs = argsNode.getPre();
        for (int i = 0; i < numPreReqd; i++, argIndex++) {
            receiveRequiredArg(preArgs.get(i), argIndex, false, -1, -1);
        }

        // Fixup opt/rest
        opt = opt > 0 ? opt : 0;
        rest = rest > -1 ? 1 : 0;

        // Now for opt args
        if (opt > 0) {
            ListNode optArgs = argsNode.getOptArgs();
            for (int j = 0; j < opt; j++, argIndex++) {
                // Jump to 'l' if this arg is not null.  If null, fall through and build the default value!
                Label l = getNewLabel();
                OptArgNode n = (OptArgNode)optArgs.get(j);
                String argName = n.getName();
                Variable av = getNewLocalVariable(argName, 0);
                if (scope instanceof IRMethod) addArgumentDescription(IRMethodArgs.ArgType.opt, argName);
                // You need at least required+j+1 incoming args for this opt arg to get an arg at all
                addInstr(new ReceiveOptArgInstr(av, required, numPreReqd, j));
                addInstr(BNEInstr.create(l, av, UndefinedValue.UNDEFINED)); // if 'av' is not undefined, go to default
                build(n.getValue());
                addInstr(new LabelInstr(l));
            }
        }

        // Rest arg
        if (rest > 0) {
            // Consider: def foo(*); .. ; end
            // For this code, there is no argument name available from the ruby code.
            // So, we generate an implicit arg name
            String argName = argsNode.getRestArgNode().getName();
            if (scope instanceof IRMethod) addArgumentDescription(IRMethodArgs.ArgType.rest, argName == null ? "" : argName);
            argName = (argName == null || argName.equals("")) ? "*" : argName;

            // You need at least required+opt+1 incoming args for the rest arg to get any args at all
            // If it is going to get something, then it should ignore required+opt args from the beginning
            // because they have been accounted for already.
            addInstr(new ReceiveRestArgInstr(getNewLocalVariable(argName, 0), required + opt, argIndex));
        }

        // Post(-opt and rest) required args
        ListNode postArgs = argsNode.getPost();
        for (int i = 0; i < numPostReqd; i++) {
            receiveRequiredArg(postArgs.get(i), i, true, numPreReqd, numPostReqd);
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
            String blockArgName = blockArg.getName();
            Variable blockVar = getLocalVariable(blockArgName, 0);
            if (scope instanceof IRMethod) addArgumentDescription(IRMethodArgs.ArgType.block, blockArgName);
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
        addInstr(new ReceiveSelfInstr(buildSelf()));

        // used for yields; metaclass body (sclass) inherits yield var from surrounding, and accesses it as implicit
        if (scope instanceof IRMethod || scope instanceof IRMetaClassBody) {
            addInstr(new LoadImplicitClosureInstr(scope.getYieldClosureVariable()));
        } else {
            addInstr(new LoadFrameClosureInstr(scope.getYieldClosureVariable()));
        }
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
        ListNode keywords = argsNode.getKeywords();
        int required = argsNode.getRequiredArgsCount();
        if (keywords != null) {
            for (Node knode : keywords.childNodes()) {
                KeywordArgNode kwarg = (KeywordArgNode)knode;
                AssignableNode kasgn = kwarg.getAssignable();
                String argName = ((INameNode) kasgn).getName();
                Variable av = getNewLocalVariable(argName, 0);
                Label l = getNewLabel();
                if (scope instanceof IRMethod) addKeyArgDesc(kasgn, argName);
                addInstr(new ReceiveKeywordArgInstr(av, argName, required));
                addInstr(BNEInstr.create(l, av, UndefinedValue.UNDEFINED)); // if 'av' is not undefined, we are done

                // Required kwargs have no value and check_arity will throw if they are not provided.
                if (!isRequiredKeywordArgumentValue(kasgn)) {
                    build(kasgn);
                } else {
                    addInstr(new RaiseRequiredKeywordArgumentError(argName));
                }
                addInstr(new LabelInstr(l));
            }
        }

        // 2.0 keyword rest arg
        KeywordRestArgNode keyRest = argsNode.getKeyRest();
        if (keyRest != null) {
            String argName = keyRest.getName();
            Variable av = getNewLocalVariable(argName, 0);
            if (scope instanceof IRMethod) addArgumentDescription(IRMethodArgs.ArgType.keyrest, argName);
            addInstr(new ReceiveKeywordRestArgInstr(av, required));
        }

        // Block arg
        receiveBlockArg(argsNode);
    }

    private void addKeyArgDesc(AssignableNode kasgn, String argName) {
        if (isRequiredKeywordArgumentValue(kasgn)) {
            addArgumentDescription(IRMethodArgs.ArgType.keyreq, argName);
        } else {
            addArgumentDescription(IRMethodArgs.ArgType.key, argName);
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
            case MULTIPLEASGN19NODE: {
                MultipleAsgn19Node childNode = (MultipleAsgn19Node) node;
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
                throw new NotCompilableException("Shouldn't get here: " + node);
        }
    }

    // This method is called both for regular multiple assignment as well as argument passing
    //
    // Ex: a,b,*c=v  is a regular assignment and in this case, the "values" operand will be non-null
    // Ex: { |a,b,*c| ..} is the argument passing case
    public void buildMultipleAsgn19Assignment(final MultipleAsgn19Node multipleAsgnNode, Operand argsArray, Operand values) {
        final ListNode masgnPre = multipleAsgnNode.getPre();

        // Build assignments for specific named arguments
        int i = 0;
        if (masgnPre != null) {
            for (Node an: masgnPre.childNodes()) {
                if (values == null) {
                    buildArgsMasgn(an, argsArray, false, -1, -1, i, false);
                } else {
                    Variable rhsVal = createTemporaryVariable();
                    addInstr(new ReqdArgMultipleAsgnInstr(rhsVal, values, i));
                    buildAssignment(an, rhsVal);
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
                buildAssignment(restNode, rhsVal); // rest of the argument array!
            }
        }

        // Build assignments for rest of the operands
        final ListNode masgnPost = multipleAsgnNode.getPost();
        if (masgnPost != null) {
            int j = 0;
            for (Node an: masgnPost.childNodes()) {
                if (values == null) {
                    buildArgsMasgn(an, argsArray, false, i, postArgsCount, j, false);
                } else {
                    Variable rhsVal = createTemporaryVariable();
                    addInstr(new ReqdArgMultipleAsgnInstr(rhsVal, values, i, postArgsCount, j));  // Fetch from the end
                    buildAssignment(an, rhsVal);
                }
                j++;
            }
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
        addInstr(new ReturnInstr(ret));

        // End
        addInstr(new LabelInstr(rEndLabel));
    }

    public void receiveMethodArgs(final ArgsNode argsNode) {
        receiveArgs(argsNode);
    }

    public void receiveBlockArgs(final IterNode node) {
        Node args = node.getVarNode();
        if (args instanceof ArgsNode) { // regular blocks
            ((IRClosure) scope).setParameterList(Helpers.encodeParameterList((ArgsNode) args).split(";"));
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

    private Operand dynamicPiece(Node pieceNode) {
        Operand piece = build(pieceNode);

        return piece == null ? manager.getNil() : piece;
    }

    public Operand buildDRegexp(DRegexpNode node) {
        List<Node> nodePieces = node.childNodes();
        Operand[] pieces = new Operand[nodePieces.size()];
        for (int i = 0; i < pieces.length; i++) {
            pieces[i] = dynamicPiece(nodePieces.get(i));
        }

        Variable res = createTemporaryVariable();
        addInstr(new BuildDynRegExpInstr(res, pieces, node.getOptions()));
        return res;
    }

    public Operand buildDStr(DStrNode node) {
        List<Node> nodePieces = node.childNodes();
        Operand[] pieces = new Operand[nodePieces.size()];
        for (int i = 0; i < pieces.length; i++) {
            pieces[i] = dynamicPiece(nodePieces.get(i));
        }

        Variable res = createTemporaryVariable();
        addInstr(new BuildCompoundStringInstr(res, pieces, node.getEncoding()));
        return res;
    }

    public Operand buildDSymbol(DSymbolNode node) {
        List<Node> nodePieces = node.childNodes();
        Operand[] pieces = new Operand[nodePieces.size()];
        for (int i = 0; i < pieces.length; i++) {
            pieces[i] = dynamicPiece(nodePieces.get(i));
        }

        Variable res = createTemporaryVariable();
        addInstr(new BuildCompoundStringInstr(res, pieces, node.getEncoding()));
        return copyAndReturnValue(new DynamicSymbol(res));
    }

    public Operand buildDVar(DVarNode node) {
        return getLocalVariable(node.getName(), node.getDepth());
    }

    public Operand buildDXStr(final DXStrNode dstrNode) {
        List<Node> nodePieces = dstrNode.childNodes();
        Operand[] pieces = new Operand[nodePieces.size()];
        for (int i = 0; i < pieces.length; i++) {
            pieces[i] = dynamicPiece(nodePieces.get(i));
        }

        return addResultInstr(new BacktickInstr(createTemporaryVariable(), pieces));
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
    public Operand buildEnsureNode(EnsureNode ensureNode) {
        Node bodyNode = ensureNode.getBodyNode();

        // ------------ Build the body of the ensure block ------------
        //
        // The ensure code is built first so that when the protected body is being built,
        // the ensure code can be cloned at break/next/return sites in the protected body.

        // Push a new ensure block node onto the stack of ensure bodies being built
        // The body's instructions are stashed and emitted later.
        EnsureBlockInfo ebi = new EnsureBlockInfo(scope,
            (bodyNode instanceof RescueNode) ? (RescueNode)bodyNode : null,
            getCurrentLoop(),
            activeRescuers.peek());

        ensureBodyBuildStack.push(ebi);
        Operand ensureRetVal = (ensureNode.getEnsureNode() == null) ? manager.getNil() : build(ensureNode.getEnsureNode());
        ensureBodyBuildStack.pop();

        // ------------ Build the protected region ------------
        activeEnsureBlockStack.push(ebi);

        // Start of protected region
        addInstr(new LabelInstr(ebi.regionStart));
        addInstr(new ExceptionRegionStartMarkerInstr(ebi.dummyRescueBlockLabel));
        activeRescuers.push(ebi.dummyRescueBlockLabel);

        // Generate IR for code being protected
        Operand rv = bodyNode instanceof RescueNode ? buildRescueInternal((RescueNode) bodyNode, ebi) : build(bodyNode);

        // End of protected region
        addInstr(new ExceptionRegionEndMarkerInstr());
        activeRescuers.pop();

        // Clone the ensure body and jump to the end.
        // Don't bother if the protected body ended in a return.
        if (rv != U_NIL && !(bodyNode instanceof RescueNode)) {
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
        ebi.emitBody(this);

        // 1. Ensure block has no explicit return => the result of the entire ensure expression is the result of the protected body.
        // 2. Ensure block has an explicit return => the result of the protected body is ignored.
        // U_NIL => there was a return from within the ensure block!
        if (ensureRetVal == U_NIL) rv = U_NIL;

        // Return (rethrow exception/end)
        // rethrows the caught exception from the dummy ensure block
        addInstr(new ThrowExceptionInstr(exc));

        // End label for the exception region
        addInstr(new LabelInstr(ebi.end));

        return rv;
    }

    public Operand buildEvStr(EvStrNode node) {
        return new AsString(build(node.getBody()));
    }

    public Operand buildFalse() {
        return manager.getFalse();
    }

    public Operand buildFCall(FCallNode fcallNode) {
        Node      callArgsNode = fcallNode.getArgsNode();
        Operand[] args         = setupCallArgs(callArgsNode);
        Operand   block        = setupCallClosure(fcallNode.getIterNode());
        Variable  callResult   = createTemporaryVariable();

        determineIfMaybeUsingMethod(fcallNode.getName(), args);

        CallInstr callInstr    = CallInstr.create(scope, CallType.FUNCTIONAL, callResult, fcallNode.getName(), buildSelf(), args, block);
        receiveBreakException(block, callInstr);
        return callResult;
    }

    private Operand setupCallClosure(Node node) {
        if (node == null) return null;

        switch (node.getNodeType()) {
            case ITERNODE:
                return build(node);
            case BLOCKPASSNODE:
                return build(((BlockPassNode)node).getBodyNode());
            default:
                throw new NotCompilableException("ERROR: Encountered a method with a non-block, non-blockpass iter node at: " + node);
        }
    }

    // FIXME: This needs to be called on super/zsuper too
    private void determineIfMaybeUsingMethod(String methodName, Operand[] args) {
        IRScope outerScope = scope.getNearestTopLocalVariableScope();

        // 'using single_mod_arg' possible nearly everywhere but method scopes.
        if (USING_METHOD.equals(methodName) && !(outerScope instanceof IRMethod) && args.length == 1) {
            scope.setIsMaybeUsingRefinements();
        }
    }

    public Operand buildFixnum(FixnumNode node) {
        return new Fixnum(node.getValue());
    }

    public Operand buildFlip(FlipNode flipNode) {
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

        Fixnum s1 = new Fixnum((long)1);
        Fixnum s2 = new Fixnum((long)2);

        // Create a variable to hold the flip state
        IRBuilder nearestNonClosureBuilder = getNearestFlipVariableScopeBuilder();

        // Flip is completely broken atm and it was semi-broken in its last incarnation.
        // Method and closures (or evals) are not built at the same time and if -X-C or JIT or AOT
        // and jit.threshold=0 then the non-closure where we want to store the hidden flip variable
        // is unable to get more instrs added to it (not quite true for -X-C but definitely true
        // for JIT/AOT.  Also it means needing to grow the size of any heap scope for variables.
        if (nearestNonClosureBuilder == null) {
            Variable excType = createTemporaryVariable();
            addInstr(new InheritanceSearchConstInstr(excType, new ObjectClass(), "NotImplementedError", false));
            Variable exc = addResultInstr(CallInstr.create(scope, createTemporaryVariable(), "new", excType, new Operand[] {new FrozenString("Flip support currently broken")}, null));
            addInstr(new ThrowExceptionInstr(exc));
            return buildNil();
        }
        Variable flipState = nearestNonClosureBuilder.scope.getNewFlipStateVariable();
        nearestNonClosureBuilder.initFlipStateVariable(flipState, s1);
        if (scope instanceof IRClosure) {
            // Clone the flip variable to be usable at the proper-depth.
            int n = 0;
            IRScope x = scope;
            while (!x.isFlipScope()) {
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
        Operand s1Val = build(flipNode.getBeginNode());
        addInstr(BNEInstr.create(s2Label, s1Val, manager.getTrue()));

        // s1 condition is true => set returnVal to true & move to state 2
        addInstr(new CopyInstr(returnVal, manager.getTrue()));
        addInstr(new CopyInstr(flipState, s2));

        // Check for state 2
        addInstr(new LabelInstr(s2Label));

        // For exclusive ranges/flips, we dont evaluate s2's condition if s1's condition was satisfied
        if (flipNode.isExclusive()) addInstr(BEQInstr.create(returnVal, manager.getTrue(), doneLabel));

        // Are we in state 2?
        addInstr(BNEInstr.create(doneLabel, flipState, s2));

        // ----- Code for when we are in state 2 -----
        Operand s2Val = build(flipNode.getEndNode());
        addInstr(new CopyInstr(returnVal, manager.getTrue()));
        addInstr(BNEInstr.create(doneLabel, s2Val, manager.getTrue()));

        // s2 condition is true => move to state 1
        addInstr(new CopyInstr(flipState, s1));

        // Done testing for s1's and s2's conditions.
        // returnVal will have the result of the flip condition
        addInstr(new LabelInstr(doneLabel));

        return returnVal;
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
        CallInstr callInstr = new CallInstr(CallType.NORMAL, result, "each", receiver, NO_ARGS, forBlock);
        receiveBreakException(forBlock, callInstr);

        return result;
    }

    private InterpreterContext buildForIterInner(ForNode forNode) {
        prepareImplicitState();                                    // recv_self, add frame block, etc)

        Node varNode = forNode.getVarNode();
        if (varNode != null && varNode.getNodeType() != null) receiveBlockArgs(forNode);

        addCurrentScopeAndModule();                                // %current_scope/%current_module
        addInstr(new LabelInstr(((IRClosure) scope).startLabel));  // Start label -- used by redo!

        // Build closure body and return the result of the closure
        Operand closureRetVal = forNode.getBodyNode() == null ? manager.getNil() : build(forNode.getBodyNode());
        if (closureRetVal != U_NIL) { // can be null if the node is an if node with returns in both branches.
            addInstr(new ReturnInstr(closureRetVal));
        }

        return scope.allocateInterpreterContext(instructions);
    }

    public Operand buildForIter(final ForNode forNode) {
        // Create a new closure context
        IRClosure closure = new IRFor(manager, scope, forNode.getPosition().getLine(), forNode.getScope(), Signature.from(forNode));

        // Create a new nested builder to ensure this gets its own IR builder state like the ensure block stack
        newIRBuilder(manager, closure).buildForIterInner(forNode);

        return new WrappedIRClosure(buildSelf(), closure);
    }

    public Operand buildGlobalAsgn(GlobalAsgnNode globalAsgnNode) {
        Operand value = build(globalAsgnNode.getValueNode());
        addInstr(new PutGlobalVarInstr(globalAsgnNode.getName(), value));
        return value;
    }

    public Operand buildGlobalVar(GlobalVarNode node) {
        return addResultInstr(new GetGlobalVariableInstr(createTemporaryVariable(), node.getName()));
    }

    public Operand buildHash(HashNode hashNode) {
        List<KeyValuePair<Operand, Operand>> args = new ArrayList<>();
        Operand splatKeywordArgument = null;
        boolean hasAssignments = hashNode.containsVariableAssignment();

        for (KeyValuePair<Node, Node> pair: hashNode.getPairs()) {
            Node key = pair.getKey();
            Operand keyOperand;

            if (key == null) { // splat kwargs [e.g. foo(a: 1, **splat)] key is null and will be in last pair of hash
                splatKeywordArgument = build(pair.getValue());
                break;
            } else {
                keyOperand = buildWithOrder(key, hasAssignments);
            }

            args.add(new KeyValuePair<>(keyOperand, buildWithOrder(pair.getValue(), hasAssignments)));
        }

        if (splatKeywordArgument != null) { // splat kwargs merge with any explicit kwargs
            Variable tmp = createTemporaryVariable();
            addInstr(new RuntimeHelperCall(tmp, MERGE_KWARGS, new Operand[] { splatKeywordArgument, new Hash(args)}));
            return tmp;
        } else {
            return copyAndReturnValue(new Hash(args));
        }
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
    public Operand buildIf(final IfNode ifNode) {
        Node actualCondition = skipOverNewlines(ifNode.getCondition());

        Variable result;
        Label    falseLabel = getNewLabel();
        Label    doneLabel  = getNewLabel();
        Operand  thenResult;
        addInstr(BEQInstr.create(build(actualCondition), manager.getFalse(), falseLabel));

        boolean thenNull = false;
        boolean elseNull = false;
        boolean thenUnil = false;
        boolean elseUnil = false;

        // Build the then part of the if-statement
        if (ifNode.getThenBody() != null) {
            thenResult = build(ifNode.getThenBody());
            if (thenResult != U_NIL) { // thenResult can be U_NIL if then-body ended with a return!
                // SSS FIXME: Can look at the last instr and short-circuit this jump if it is a break rather
                // than wait for dead code elimination to do it
                result = getValueInTemporaryVariable(thenResult);
                addInstr(new JumpInstr(doneLabel));
            } else {
                result = createTemporaryVariable();
                thenUnil = true;
            }
        } else {
            thenNull = true;
            result = addResultInstr(new CopyInstr(createTemporaryVariable(), manager.getNil()));
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

    private InterpreterContext buildIterInner(IterNode iterNode) {
        prepareImplicitState();                                    // recv_self, add frame block, etc)

        if (iterNode.getVarNode().getNodeType() != null) receiveBlockArgs(iterNode);

        addCurrentScopeAndModule();                                // %current_scope/%current_module
        addInstr(new LabelInstr(((IRClosure) scope).startLabel));  // start label -- used by redo!

        // Build closure body and return the result of the closure
        Operand closureRetVal = iterNode.getBodyNode() == null ? manager.getNil() : build(iterNode.getBodyNode());
        if (closureRetVal != U_NIL) { // can be U_NIL if the node is an if node with returns in both branches.
            addInstr(new ReturnInstr(closureRetVal));
        }

        // Always add break/return handling even though this
        // is only required for lambdas, but we don't know at this time,
        // if this is a lambda or not.
        //
        // SSS FIXME: At a later time, see if we can optimize this and
        // do this on demand.
        handleBreakAndReturnsInLambdas();

        return scope.allocateInterpreterContext(instructions);
    }
    public Operand buildIter(final IterNode iterNode) {
        IRClosure closure = new IRClosure(manager, scope, iterNode.getPosition().getLine(), iterNode.getScope(),
                Signature.from(iterNode));

        // Create a new nested builder to ensure this gets its own IR builder state like the ensure block stack
        newIRBuilder(manager, closure).buildIterInner(iterNode);

        return new WrappedIRClosure(buildSelf(), closure);
    }

    public Operand buildLiteral(LiteralNode literalNode) {
        return copyAndReturnValue(new StringLiteral(literalNode.getName()));
    }

    public Operand buildLocalAsgn(LocalAsgnNode localAsgnNode) {
        Variable var  = getLocalVariable(localAsgnNode.getName(), localAsgnNode.getDepth());
        Operand value = build(localAsgnNode.getValueNode());
        addInstr(new CopyInstr(var, value));
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

    public Operand buildMatch(MatchNode matchNode) {
        Operand regexp = build(matchNode.getRegexpNode());

        return addResultInstr(new MatchInstr(createTemporaryVariable(), regexp));
    }

    public Operand buildMatch2(Match2Node matchNode) {
        Operand receiver = build(matchNode.getReceiverNode());
        Operand value    = build(matchNode.getValueNode());
        Variable result  = createTemporaryVariable();
        addInstr(new Match2Instr(result, receiver, value));
        if (matchNode instanceof Match2CaptureNode) {
            Match2CaptureNode m2c = (Match2CaptureNode)matchNode;
            for (int slot:  m2c.getScopeOffsets()) {
                // Static scope scope offsets store both depth and offset
                int depth = slot >> 16;
                int offset = slot & 0xffff;

                // For now, we'll continue to implicitly reference "$~"
                String var = getVarNameFromScopeTree(scope, depth, offset);
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

    public Operand buildMatch3(Match3Node matchNode) {
        Operand receiver = build(matchNode.getReceiverNode());
        Operand value = build(matchNode.getValueNode());

        return addResultInstr(new Match3Instr(createTemporaryVariable(), receiver, value));
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
        Colon3Node cpath = moduleNode.getCPath();
        String moduleName = cpath.getName();
        Operand container = getContainerFromCPath(cpath);
        IRModuleBody body = new IRModuleBody(manager, scope, moduleName, moduleNode.getPosition().getLine(), moduleNode.getScope());
        Variable moduleVar = addResultInstr(new DefineModuleInstr(createTemporaryVariable(), body, container));

        Variable processBodyResult = addResultInstr(new ProcessModuleBodyInstr(createTemporaryVariable(), moduleVar, NullBlock.INSTANCE));
        newIRBuilder(manager, body).buildModuleOrClassBody(moduleNode.getBodyNode(), moduleNode.getPosition().getLine());
        return processBodyResult;
    }

    public Operand buildNewline(NewlineNode node) {
        return build(skipOverNewlines(node));
    }

    public Operand buildNext(final NextNode nextNode) {
        IRLoop currLoop = getCurrentLoop();

        Operand rv = build(nextNode.getValueNode());

        // If we have ensure blocks, have to run those first!
        if (!activeEnsureBlockStack.empty()) emitEnsureBlocks(currLoop);
        else if (!activeRescueBlockStack.empty()) activeRescueBlockStack.peek().restoreException(this, currLoop);

        if (currLoop != null) {
            // If a regular loop, the next is simply a jump to the end of the iteration
            addInstr(new JumpInstr(currLoop.iterEndLabel));
        } else {
            addInstr(new ThreadPollInstr(true));
            // If a closure, the next is simply a return from the closure!
            if (scope instanceof IRClosure) {
                addInstr(new ReturnInstr(rv));
            } else {
                addInstr(new ThrowExceptionInstr(IRException.NEXT_LocalJumpError));
            }
        }

        // Once the "next instruction" (closure-return) executes, control exits this scope
        return U_NIL;
    }

    public Operand buildNthRef(NthRefNode nthRefNode) {
        return copyAndReturnValue(new NthRef(nthRefNode.getMatchNumber()));
    }

    public Operand buildNil() {
        return manager.getNil();
    }

    public Operand buildOpAsgn(OpAsgnNode opAsgnNode) {
        Label l;
        Variable readerValue = createTemporaryVariable();
        Variable writerValue = createTemporaryVariable();

        // get attr
        Operand  v1 = build(opAsgnNode.getReceiverNode());
        addInstr(CallInstr.create(scope, readerValue, opAsgnNode.getVariableName(), v1, NO_ARGS, null));

        // Ex: e.val ||= n
        //     e.val &&= n
        String opName = opAsgnNode.getOperatorName();
        if (opName.equals("||") || opName.equals("&&")) {
            l = getNewLabel();
            addInstr(BEQInstr.create(readerValue, opName.equals("||") ? manager.getTrue() : manager.getFalse(), l));

            // compute value and set it
            Operand  v2 = build(opAsgnNode.getValueNode());
            addInstr(CallInstr.create(scope, writerValue, opAsgnNode.getVariableNameAsgn(), v1, new Operand[] {v2}, null));
            // It is readerValue = v2.
            // readerValue = writerValue is incorrect because the assignment method
            // might return something else other than the value being set!
            addInstr(new CopyInstr(readerValue, v2));
            addInstr(new LabelInstr(l));

            return readerValue;
        }
        // Ex: e.val = e.val.f(n)
        else {
            // call operator
            Operand  v2 = build(opAsgnNode.getValueNode());
            Variable setValue = createTemporaryVariable();
            addInstr(CallInstr.create(scope, setValue, opAsgnNode.getOperatorName(), readerValue, new Operand[]{v2}, null));

            // set attr
            addInstr(CallInstr.create(scope, writerValue, opAsgnNode.getVariableNameAsgn(), v1, new Operand[] {setValue}, null));
            // Returning writerValue is incorrect becuase the assignment method
            // might return something else other than the value being set!
            return setValue;
        }
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
        addInstr(BEQInstr.create(v1, manager.getFalse(), l));
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
            addInstr(BEQInstr.create(flag, manager.getNil(), l2)); // if v1 is undefined, go to v2's computation
        }
        v1 = build(orNode.getFirstNode()); // build of 'x'
        addInstr(new CopyInstr(flag, v1));
        Variable result = getValueInTemporaryVariable(v1);
        if (needsDefnCheck) {
            addInstr(new LabelInstr(l2));
        }
        addInstr(BEQInstr.create(flag, manager.getTrue(), l1));  // if v1 is defined and true, we are done!
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
        Operand array = buildWithOrder(opElementAsgnNode.getReceiverNode(), opElementAsgnNode.containsVariableAssignment());
        Label endLabel = getNewLabel();
        Variable elt = createTemporaryVariable();
        Operand[] argList = setupCallArgs(opElementAsgnNode.getArgsNode());
        addInstr(CallInstr.create(scope, elt, "[]", array, argList, null));
        addInstr(BEQInstr.create(elt, truthy, endLabel));
        Operand value = build(opElementAsgnNode.getValueNode());

        argList = addArg(argList, value);
        addInstr(CallInstr.create(scope, elt, "[]=", array, argList, null));
        addInstr(new CopyInstr(elt, value));

        addInstr(new LabelInstr(endLabel));
        return elt;
    }

    // a[i] *= n, etc.  anything that is not "a[i] &&= .. or a[i] ||= .."
    public Operand buildOpElementAsgnWithMethod(OpElementAsgnNode opElementAsgnNode) {
        Operand array = buildWithOrder(opElementAsgnNode.getReceiverNode(), opElementAsgnNode.containsVariableAssignment());
        Operand[] argList = setupCallArgs(opElementAsgnNode.getArgsNode());
        Variable elt = createTemporaryVariable();
        addInstr(CallInstr.create(scope, elt, "[]", array, argList, null)); // elt = a[args]
        Operand value = build(opElementAsgnNode.getValueNode());                                       // Load 'value'
        String  operation = opElementAsgnNode.getOperatorName();
        addInstr(CallInstr.create(scope, elt, operation, elt, new Operand[] { value }, null)); // elt = elt.OPERATION(value)
        // SSS: do not load the call result into 'elt' to eliminate the RAW dependency on the call
        // We already know what the result is going be .. we are just storing it back into the array
        Variable tmp = createTemporaryVariable();
        argList = addArg(argList, elt);
        addInstr(CallInstr.create(scope, tmp, "[]=", array, argList, null));   // a[args] = elt
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
        addInstr(BEQInstr.create(left, manager.getTrue(), endOfExprLabel));
        Operand right  = build(orNode.getSecondNode());
        addInstr(new CopyInstr(result, right));
        addInstr(new LabelInstr(endOfExprLabel));

        return result;
    }

    private InterpreterContext buildPrePostExeInner(Node body) {
        // Set up %current_scope and %current_module
        addInstr(new CopyInstr(scope.getCurrentScopeVariable(), CURRENT_SCOPE[0]));
        addInstr(new CopyInstr(scope.getCurrentModuleVariable(), SCOPE_MODULE[0]));
        build(body);

        // END does not have either explicit or implicit return, so we add one
        addInstr(new ReturnInstr(new Nil()));

        return scope.allocateInterpreterContext(instructions);
    }

    public Operand buildPostExe(PostExeNode postExeNode) {
        IRScope topLevel = scope.getTopLevelScope();
        IRScope nearestLVarScope = scope.getNearestTopLocalVariableScope();

        IRClosure endClosure = new IRClosure(manager, scope, postExeNode.getPosition().getLine(), nearestLVarScope.getStaticScope(), Signature.from(postExeNode), "_END_", true);
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
        IRScope topLevel = scope.getTopLevelScope();
        IRClosure beginClosure = new IRFor(manager, scope, preExeNode.getPosition().getLine(), topLevel.getStaticScope(),
                Signature.from(preExeNode), "_BEGIN_");
        // Create a new nested builder to ensure this gets its own IR builder state like the ensure block stack
        newIRBuilder(manager, beginClosure).buildPrePostExeInner(preExeNode.getBodyNode());

        topLevel.recordBeginBlock(beginClosure);  // Record the begin block at IR build time
        return manager.getNil();
    }

    public Operand buildRational(RationalNode rationalNode) {
        return new Rational(rationalNode.getNumerator(), rationalNode.getDenominator());
    }

    public Operand buildRedo() {
        // If in a loop, a redo is a jump to the beginning of the loop.
        // If not, for closures, a redo is a jump to the beginning of the closure.
        // If not in a loop or a closure, it is a local jump error
        IRLoop currLoop = getCurrentLoop();
        if (currLoop != null) {
             addInstr(new JumpInstr(currLoop.iterStartLabel));
        } else {
            if (scope instanceof IRClosure) {
                addInstr(new ThreadPollInstr(true));
                addInstr(new JumpInstr(((IRClosure) scope).startLabel));
            } else {
                addInstr(new ThrowExceptionInstr(IRException.REDO_LocalJumpError));
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
        return buildRescueInternal(node, null);
    }

    private Operand buildRescueInternal(RescueNode rescueNode, EnsureBlockInfo ensure) {
        // Labels marking start, else, end of the begin-rescue(-ensure)-end block
        Label rBeginLabel = ensure == null ? getNewLabel() : ensure.regionStart;
        Label rEndLabel   = ensure == null ? getNewLabel() : ensure.end;
        Label rescueLabel = getNewLabel(); // Label marking start of the first rescue code.

        // Save $! in a temp var so it can be restored when the exception gets handled.
        Variable savedGlobalException = createTemporaryVariable();
        addInstr(new GetGlobalVariableInstr(savedGlobalException, "$!"));
        if (ensure != null) ensure.savedGlobalException = savedGlobalException;

        addInstr(new LabelInstr(rBeginLabel));

        // Placeholder rescue instruction that tells rest of the compiler passes the boundaries of the rescue block.
        addInstr(new ExceptionRegionStartMarkerInstr(rescueLabel));
        activeRescuers.push(rescueLabel);

        // Body
        Operand tmp = manager.getNil();  // default return value if for some strange reason, we neither have the body node or the else node!
        Variable rv = createTemporaryVariable();
        if (rescueNode.getBodyNode() != null) tmp = build(rescueNode.getBodyNode());

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
        RescueBlockInfo rbi = new RescueBlockInfo(rescueNode, rBeginLabel, savedGlobalException, getCurrentLoop());
        activeRescueBlockStack.push(rbi);

        // Since rescued regions are well nested within Ruby, this bare marker is sufficient to
        // let us discover the edge of the region during linear traversal of instructions during cfg construction.
        addInstr(new ExceptionRegionEndMarkerInstr());
        activeRescuers.pop();

        // Else part of the body -- we simply fall through from the main body if there were no exceptions
        Label elseLabel = rescueNode.getElseNode() == null ? null : getNewLabel();
        if (elseLabel != null) {
            addInstr(new LabelInstr(elseLabel));
            tmp = build(rescueNode.getElseNode());
        }

        if (tmp != U_NIL) {
            addInstr(new CopyInstr(rv, tmp));

            // No explicit return from the protected body
            // - If we dont have any ensure blocks, simply jump to the end of the rescue block
            // - If we do, execute the ensure code.
            if (ensure != null) {
                ensure.cloneIntoHostScope(this);
            }
            addInstr(new JumpInstr(rEndLabel));
        }   //else {
            // If the body had an explicit return, the return instruction IR build takes care of setting
            // up execution of all necessary ensure blocks.  So, nothing to do here!
            //
            // Additionally, the value in 'rv' will never be used, so need to set it to any specific value.
            // So, we can leave it undefined.  If on the other hand, there was an exception in that block,
            // 'rv' will get set in the rescue handler -- see the 'rv' being passed into
            // buildRescueBodyInternal below.  So, in either case, we are good!
            //}

        // Start of rescue logic
        addInstr(new LabelInstr(rescueLabel));

        // Save off exception & exception comparison type
        Variable exc = addResultInstr(new ReceiveRubyExceptionInstr(createTemporaryVariable()));

        // Build the actual rescue block(s)
        buildRescueBodyInternal(rescueNode.getRescueNode(), rv, exc, rEndLabel);

        // End label -- only if there is no ensure block!  With an ensure block, you end at ensureEndLabel.
        if (ensure == null) addInstr(new LabelInstr(rEndLabel));

        activeRescueBlockStack.pop();
        return rv;
    }

    private void outputExceptionCheck(Operand excType, Operand excObj, Label caughtLabel) {
        Variable eqqResult = addResultInstr(new RescueEQQInstr(createTemporaryVariable(), excType, excObj));
        addInstr(BEQInstr.create(eqqResult, manager.getTrue(), caughtLabel));
    }

    private void buildRescueBodyInternal(RescueBodyNode rescueBodyNode, Variable rv, Variable exc, Label endLabel) {
        final Node exceptionList = rescueBodyNode.getExceptionNodes();

        // Compare and branch as necessary!
        Label uncaughtLabel = getNewLabel();
        Label caughtLabel = getNewLabel();
        if (exceptionList != null) {
            if (exceptionList instanceof ListNode) {
                List<Operand> excTypes = new ArrayList<>();
                for (Node excType : exceptionList.childNodes()) {
                    excTypes.add(build(excType));
                }
                outputExceptionCheck(new Array(excTypes), exc, caughtLabel);
            } else if (exceptionList instanceof SplatNode) { // splatnode, catch
                outputExceptionCheck(build(((SplatNode)exceptionList).getValue()), exc, caughtLabel);
            } else { // argscat/argspush
                outputExceptionCheck(build(exceptionList), exc, caughtLabel);
            }
        } else {
            // SSS FIXME:
            // rescue => e AND rescue implicitly EQQ the exception object with StandardError
            // We generate explicit IR for this test here.  But, this can lead to inconsistent
            // behavior (when compared to MRI) in certain scenarios.  See example:
            //
            //   self.class.const_set(:StandardError, 1)
            //   begin; raise TypeError.new; rescue; puts "AHA"; end
            //
            // MRI rescues the error, but we will raise an exception because of reassignment
            // of StandardError.  I am ignoring this for now and treating this as undefined behavior.
            //
            // Solution: Create a 'StandardError' operand type to eliminate this.
            Variable v = addResultInstr(new InheritanceSearchConstInstr(createTemporaryVariable(), new ObjectClass(), "StandardError", false));
            outputExceptionCheck(v, exc, caughtLabel);
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
        Node realBody = skipOverNewlines(rescueBodyNode.getBodyNode());
        Operand x = build(realBody);
        if (x != U_NIL) { // can be U_NIL if the rescue block has an explicit return
            // Restore "$!"
            RescueBlockInfo rbi = activeRescueBlockStack.peek();
            addInstr(new PutGlobalVarInstr("$!", rbi.savedExceptionVariable));

            // Set up node return value 'rv'
            addInstr(new CopyInstr(rv, x));

            // If we have a matching ensure block, clone it so ensure block runs here
            if (!activeEnsureBlockStack.empty() && rbi.rescueNode == activeEnsureBlockStack.peek().matchingRescueNode) {
                activeEnsureBlockStack.peek().cloneIntoHostScope(this);
            }
            addInstr(new JumpInstr(endLabel));
        }
    }

    public Operand buildRetry() {
        // JRuby only supports retry when present in rescue blocks!
        // 1.9 doesn't support retry anywhere else.

        // Jump back to the innermost rescue block
        // We either find it, or we add code to throw a runtime exception
        if (activeRescueBlockStack.empty()) {
            addInstr(new ThrowExceptionInstr(IRException.RETRY_LocalJumpError));
        } else {
            addInstr(new ThreadPollInstr(true));
            // Restore $! and jump back to the entry of the rescue block
            RescueBlockInfo rbi = activeRescueBlockStack.peek();
            addInstr(new PutGlobalVarInstr("$!", rbi.savedExceptionVariable));
            addInstr(new JumpInstr(rbi.entryLabel));
            // Retries effectively create a loop
            scope.setHasLoopsFlag();
        }
        return manager.getNil();
    }

    private Operand processEnsureRescueBlocks(Operand retVal) {
        // Before we return,
        // - have to go execute all the ensure blocks if there are any.
        //   this code also takes care of resetting "$!"
        // - if we have a rescue block, reset "$!".
        if (!activeEnsureBlockStack.empty()) {
            retVal = addResultInstr(new CopyInstr(createTemporaryVariable(), retVal));
            emitEnsureBlocks(null);
        } else if (!activeRescueBlockStack.empty()) {
            // Restore $!
            RescueBlockInfo rbi = activeRescueBlockStack.peek();
            addInstr(new PutGlobalVarInstr("$!", rbi.savedExceptionVariable));
        }
       return retVal;
    }

    public Operand buildReturn(ReturnNode returnNode) {
        Operand retVal = build(returnNode.getValueNode());

        if (scope instanceof IRClosure) {
            // If 'm' is a block scope, a return returns from the closest enclosing method.
            // If this happens to be a module body, the runtime throws a local jump error if the
            // closure is a proc. If the closure is a lambda, then this becomes a normal return.
            boolean maybeLambda = scope.getNearestMethod() == null;
            addInstr(new CheckForLJEInstr(maybeLambda));
            retVal = processEnsureRescueBlocks(retVal);
            addInstr(new NonlocalReturnInstr(retVal, maybeLambda ? "--none--" : scope.getNearestMethod().getName()));
        } else if (scope.isModuleBody()) {
            IRMethod sm = scope.getNearestMethod();

            // Cannot return from top-level module bodies!
            if (sm == null) addInstr(new ThrowExceptionInstr(IRException.RETURN_LocalJumpError));
            retVal = processEnsureRescueBlocks(retVal);
            if (sm != null) addInstr(new NonlocalReturnInstr(retVal, sm.getName()));
        } else {
            retVal = processEnsureRescueBlocks(retVal);
            addInstr(new ReturnInstr(retVal));
        }

        // The value of the return itself in the containing expression can never be used because of control-flow reasons.
        // The expression that uses this result can never be executed beyond the return and hence the value itself is just
        // a placeholder operand.
        return U_NIL;
    }

    public InterpreterContext buildEvalRoot(RootNode rootNode) {
        addInstr(new LineNumberInstr(scope.getLineNumber()));

        prepareImplicitState();                                    // recv_self, add frame block, etc)
        addCurrentScopeAndModule();                                // %current_scope/%current_module

        Operand returnValue = rootNode.getBodyNode() == null ? manager.getNil() : build(rootNode.getBodyNode());
        addInstr(new ReturnInstr(returnValue));

        return scope.allocateInterpreterContext(instructions);
    }

    public static InterpreterContext buildRoot(IRManager manager, RootNode rootNode) {
        IRScriptBody script = new IRScriptBody(manager, rootNode.getPosition().getFile(), rootNode.getStaticScope());

        return topIRBuilder(manager, script).buildRootInner(rootNode);
    }

    private void addCurrentScopeAndModule() {
        addInstr(new CopyInstr(scope.getCurrentScopeVariable(), CURRENT_SCOPE[0])); // %current_scope
        addInstr(new CopyInstr(scope.getCurrentModuleVariable(), SCOPE_MODULE[0])); // %current_module
    }

    private InterpreterContext buildRootInner(RootNode rootNode) {
        prepareImplicitState();                                    // recv_self, add frame block, etc)
        addCurrentScopeAndModule();                                // %current_scope/%current_module

        // Build IR for the tree and return the result of the expression tree
        addInstr(new ReturnInstr(build(rootNode.getBodyNode())));

        return scope.allocateInterpreterContext(instructions);
    }

    public Variable buildSelf() {
        return scope.getSelf();
    }

    public Operand buildSplat(SplatNode splatNode) {
        return addResultInstr(new BuildSplatInstr(createTemporaryVariable(), build(splatNode.getValue())));
    }

    public Operand buildStr(StrNode strNode) {
        return copyAndReturnValue(new StringLiteral(strNode.getValue(), strNode.getCodeRange()));
    }

    private Operand buildSuperInstr(Operand block, Operand[] args) {
        CallInstr superInstr;
        Variable ret = createTemporaryVariable();
        if (scope instanceof IRMethod && scope.getLexicalParent() instanceof IRClassBody) {
            if (((IRMethod) scope).isInstanceMethod) {
                superInstr = new InstanceSuperInstr(ret, scope.getCurrentModuleVariable(), getName(), args, block);
            } else {
                superInstr = new ClassSuperInstr(ret, scope.getCurrentModuleVariable(), getName(), args, block);
            }
        } else {
            // We dont always know the method name we are going to be invoking if the super occurs in a closure.
            // This is because the super can be part of a block that will be used by 'define_method' to define
            // a new method.  In that case, the method called by super will be determined by the 'name' argument
            // to 'define_method'.
            superInstr = new UnresolvedSuperInstr(ret, buildSelf(), args, block);
        }
        receiveBreakException(block, superInstr);
        return ret;
    }

    public Operand buildSuper(SuperNode superNode) {
        if (scope.isModuleBody()) return buildSuperInScriptBody();

        Operand[] args = setupCallArgs(superNode.getArgsNode());
        Operand block = setupCallClosure(superNode.getIterNode());
        if (block == null) block = scope.getYieldClosureVariable();
        return buildSuperInstr(block, args);
    }

    private Operand buildSuperInScriptBody() {
        return addResultInstr(new UnresolvedSuperInstr(createTemporaryVariable(), buildSelf(), NO_ARGS, null));
    }

    public Operand buildSValue(SValueNode node) {
        // SSS FIXME: Required? Verify with Tom/Charlie
        return copyAndReturnValue(new SValue(build(node.getValue())));
    }

    public Operand buildSymbol(SymbolNode node) {
        // Since symbols are interned objects, no need to copyAndReturnValue(...)
        // SSS FIXME: Premature opt?
        return new Symbol(node.getName(), node.getEncoding());
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
            IRLoop loop = new IRLoop(scope, getCurrentLoop());
            Variable loopResult = loop.loopResult;
            Label setupResultLabel = getNewLabel();

            // Push new loop
            loopStack.push(loop);

            // End of iteration jumps here
            addInstr(new LabelInstr(loop.loopStartLabel));
            if (isLoopHeadCondition) {
                Operand cv = build(conditionNode);
                addInstr(BEQInstr.create(cv, isWhile ? manager.getFalse() : manager.getTrue(), setupResultLabel));
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
                addInstr(BEQInstr.create(cv, isWhile ? manager.getTrue() : manager.getFalse(), loop.iterStartLabel));
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
        addInstr(new GVarAliasInstr(new StringLiteral(valiasNode.getNewName()), new StringLiteral(valiasNode.getOldName())));

        return manager.getNil();
    }

    public Operand buildVCall(VCallNode node) {
        return addResultInstr(CallInstr.create(scope, CallType.VARIABLE, createTemporaryVariable(),
                node.getName(), buildSelf(), NO_ARGS, null));
    }

    public Operand buildWhile(final WhileNode whileNode) {
        return buildConditionalLoop(whileNode.getConditionNode(), whileNode.getBodyNode(), true, whileNode.evaluateAtStart());
    }

    public Operand buildXStr(XStrNode node) {
        return addResultInstr(new BacktickInstr(createTemporaryVariable(), new Operand[] { new StringLiteral(node.getValue(), node.getCodeRange())}));
    }

    public Operand buildYield(YieldNode node) {
        boolean unwrap = true;
        Node argNode = node.getArgsNode();
        // Get rid of one level of array wrapping
        if (argNode != null && (argNode instanceof ArrayNode) && ((ArrayNode)argNode).size() == 1) {
            argNode = ((ArrayNode)argNode).getLast();
            unwrap = false;
        }

        Variable ret = createTemporaryVariable();
        addInstr(new YieldInstr(ret, scope.getYieldClosureVariable(), build(argNode), unwrap));
        return ret;
    }

    public Operand buildZArray() {
       return copyAndReturnValue(new Array());
    }

    private Operand buildZSuperIfNest(final Operand block) {
        final IRBuilder builder = this;
        // If we are in a block, we cannot make any assumptions about what args
        // the super instr is going to get -- if there were no 'define_method'
        // for defining methods, we could guarantee that the super is going to
        // receive args from the nearest method the block is embedded in.  But,
        // in the presence of 'define_method' (and eval and aliasing), all bets
        // are off because, any of the intervening block scopes could be a method
        // via a define_method call.
        //
        // Instead, we can actually collect all arguments of all scopes from here
        // till the nearest method scope and select the right set at runtime based
        // on which one happened to be a method scope. This has the additional
        // advantage of making explicit all used arguments.
        CodeBlock zsuperBuilder = new CodeBlock() {
            public Operand run() {
                Variable scopeDepth = createTemporaryVariable();
                addInstr(new ArgScopeDepthInstr(scopeDepth));

                Label allDoneLabel = getNewLabel();

                int depthFromSuper = 0;
                Label next = null;
                IRBuilder superBuilder = builder;
                IRScope superScope = scope;

                // Loop and generate a block for each possible value of depthFromSuper
                Variable zsuperResult = createTemporaryVariable();
                while (superScope instanceof IRClosure) {
                    // Generate the next set of instructions
                    if (next != null) addInstr(new LabelInstr(next));
                    next = getNewLabel();
                    addInstr(BNEInstr.create(next, new Fixnum(depthFromSuper), scopeDepth));
                    Operand[] args = adjustVariableDepth(getCallArgs(superScope, superBuilder), depthFromSuper);
                    addInstr(new ZSuperInstr(zsuperResult, buildSelf(), args,  block));
                    addInstr(new JumpInstr(allDoneLabel));

                    // We may run out of live builds and walk int already built scopes if zsuper in an eval
                    superBuilder = superBuilder != null && superBuilder.parent != null ? superBuilder.parent : null;
                    superScope = superScope.getLexicalParent();
                    depthFromSuper++;
                }

                addInstr(new LabelInstr(next));

                // If we hit a method, this is known to always succeed
                if (superScope instanceof IRMethod) {
                    Operand[] args = adjustVariableDepth(getCallArgs(superScope, superBuilder), depthFromSuper);
                    addInstr(new ZSuperInstr(zsuperResult, buildSelf(), args, block));
                } //else {
                // FIXME: Do or don't ... there is no try
                    /* Control should never get here in the runtime */
                    /* Should we add an exception throw here just in case? */
                //}

                addInstr(new LabelInstr(allDoneLabel));
                return zsuperResult;
            }
        };

        return receiveBreakException(block, zsuperBuilder);
    }

    public Operand buildZSuper(ZSuperNode zsuperNode) {
        if (scope.isModuleBody()) return buildSuperInScriptBody();

        Operand block = setupCallClosure(zsuperNode.getIterNode());
        if (block == null) block = scope.getYieldClosureVariable();

        // Enebo:ZSuper in for (or nested for) can be statically resolved like method but it needs to fixup depth.
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

    private InterpreterContext buildModuleOrClassBody(Node bodyNode, int linenumber) {
        if (RubyInstanceConfig.FULL_TRACE_ENABLED) {
            addInstr(new TraceInstr(RubyEvent.CLASS, null, getFileName(), linenumber));
        }

        prepareImplicitState();                                    // recv_self, add frame block, etc)
        addCurrentScopeAndModule();                                // %current_scope/%current_module

        Operand bodyReturnValue = build(bodyNode);

        if (RubyInstanceConfig.FULL_TRACE_ENABLED) {
            addInstr(new TraceInstr(RubyEvent.END, null, getFileName(), -1));
        }

        addInstr(new ReturnInstr(bodyReturnValue));

        return scope.allocateInterpreterContext(instructions);
    }

    private String methodNameFor() {
        IRScope method = scope.getNearestMethod();

        return method == null ? null : method.getName();
    }

    private TemporaryVariable createTemporaryVariable() {
        return scope.createTemporaryVariable();
    }

    public LocalVariable getLocalVariable(String name, int scopeDepth) {
        return scope.getLocalVariable(name, scopeDepth);
    }

    public LocalVariable getNewLocalVariable(String name, int scopeDepth) {
        return scope.getNewLocalVariable(name, scopeDepth);
    }

    public String getName() {
        return scope.getName();
    }

    private Label getNewLabel() {
        return scope.getNewLabel();
    }

    private String getFileName() {
        return scope.getFileName();
    }

    public void initFlipStateVariable(Variable v, Operand initState) {
        addInstrAtBeginning(new CopyInstr(v, initState));
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
            ReceiveKeywordArgInstr rkai = (ReceiveKeywordArgInstr) instr;
            // FIXME: This lost encoding information when name was converted to string earlier in IRBuilder
            keywordArgs.add(new KeyValuePair<Operand, Operand>(new Symbol(rkai.argName, USASCIIEncoding.INSTANCE), rkai.getResult()));
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
}
