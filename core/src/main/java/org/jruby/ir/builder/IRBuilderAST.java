package org.jruby.ir.builder;

import org.jcodings.Encoding;
import org.jruby.ParseResult;
import org.jruby.Ruby;
import org.jruby.RubyBignum;
import org.jruby.RubyComplex;
import org.jruby.RubyFixnum;
import org.jruby.RubyRational;
import org.jruby.RubySymbol;
import org.jruby.ast.*;
import org.jruby.ast.types.ILiteralNode;
import org.jruby.ast.types.INameNode;
import org.jruby.common.IRubyWarnings;
import org.jruby.compiler.NotCompilableException;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRFor;
import org.jruby.ir.IRManager;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRModuleBody;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScriptBody;
import org.jruby.ir.Tuple;
import org.jruby.ir.instructions.*;
import org.jruby.ir.instructions.defined.GetErrorInfoInstr;
import org.jruby.ir.instructions.defined.RestoreErrorInfoInstr;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.operands.Array;
import org.jruby.ir.operands.Bignum;
import org.jruby.ir.operands.ChilledString;
import org.jruby.ir.operands.Complex;
import org.jruby.ir.operands.Filename;
import org.jruby.ir.operands.Float;
import org.jruby.ir.operands.FrozenString;
import org.jruby.ir.operands.Hash;
import org.jruby.ir.operands.ImmutableLiteral;
import org.jruby.ir.operands.Integer;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.MutableString;
import org.jruby.ir.operands.Nil;
import org.jruby.ir.operands.NullBlock;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Regexp;
import org.jruby.ir.operands.SValue;
import org.jruby.ir.operands.Splat;
import org.jruby.ir.operands.Symbol;
import org.jruby.ir.operands.SymbolProc;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ArgumentType;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Signature;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.CommonByteLists;
import org.jruby.util.DefinedMessage;
import org.jruby.util.KeyValuePair;
import org.jruby.util.cli.Options;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.jruby.api.Warn.warning;
import static org.jruby.ir.builder.StringStyle.Frozen;
import static org.jruby.ir.instructions.RuntimeHelperCall.Methods.*;

import static org.jruby.ir.operands.ScopeModule.*;
import static org.jruby.runtime.CallType.*;
import static org.jruby.runtime.ThreadContext.*;

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
// 2. Returning null vs nil()
// ----------------------------
// - We should be returning null from the build methods where it is a normal "error" condition
// - We should be returning nil() where the actual return value of a build is the ruby nil operand
//   Look in buildIf for an example of this.
//
// 3. Temporary variable reuse
// ---------------------------
// I am reusing variables a lot in places in this code.  Should I instead always get a new variable when I need it
// This introduces artificial data dependencies, but fewer variables.  But, if we are going to implement SSA pass
// this is not a big deal.  Think this through!

public class IRBuilderAST extends IRBuilder<Node, DefNode, WhenNode, RescueBodyNode, Colon3Node, HashNode> {
    @Deprecated(since = "9.4.6.0")
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

    public IRBuilderAST(IRManager manager, IRScope scope, IRBuilder parent, IRBuilder variableBuilder, Encoding encoding) {
        super(manager, scope, parent, variableBuilder, encoding);
    }

    @Override
    public Operand build(ParseResult result) {
        return build(((RootNode) result).getBodyNode());
    }

    public IRBuilderAST(IRManager manager, IRScope scope, IRBuilder parent) {
        this(manager, scope, parent, null, null);
    }

    private NotCompilableException notCompilable(String message, Node node) {
        int line = node != null ? node.getLine() : scope.getLine();
        String loc = scope.getFile() + ":" + line;
        String what = node != null ? node.getClass().getSimpleName() + " - " + loc : loc;
        return new NotCompilableException(message + " (" + what + ").");
    }

    private Operand buildOperand(Variable result, Node node) throws NotCompilableException {
        if (node.isNewline()) determineIfWeNeedLineNumber(node.getLine(), true, node instanceof NilImplicitNode, node instanceof DefNode);

        switch (node.getNodeType()) {
            case ALIASNODE: return buildAlias((AliasNode) node);
            case ANDNODE: return buildAnd((AndNode) node);
            case ARGSCATNODE: return buildArgsCat(result, (ArgsCatNode) node);
            case ARGSPUSHNODE: return buildArgsPush(result, (ArgsPushNode) node);
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
            case CLASSVARNODE: return buildClassVar(result, (ClassVarNode) node);
            case CLASSVARASGNNODE: return buildClassVarAsgn((ClassVarAsgnNode) node);
            case COLON2NODE: return buildColon2(result, (Colon2Node) node);
            case COLON3NODE: return buildColon3(result, (Colon3Node) node);
            case COMPLEXNODE: return buildComplex((ComplexNode) node);
            case CONSTDECLNODE: return buildConstDecl((ConstDeclNode) node);
            case CONSTNODE: return searchConst(result, ((ConstNode) node).getName());
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
            case FALSENODE: return fals();
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
            case MULTIPLEASGNNODE: return buildMultipleAsgn((MultipleAsgnNode) node);
            case NEXTNODE: return buildNext((NextNode) node);
            case NTHREFNODE: return buildNthRef((NthRefNode) node);
            case NILNODE: return buildNil();
            case OPASGNANDNODE: return buildOpAsgnAnd((OpAsgnAndNode) node);
            case OPASGNCONSTDECLNODE: return buildOpAsgnConstDeclNode((OpAsgnConstDeclNode) node);
            case OPASGNNODE: return buildOpAsgn((OpAsgnNode) node);
            case OPASGNORNODE: return buildOpAsgnOr((OpAsgnOrNode) node);
            case OPELEMENTASGNNODE: return buildOpElementAsgn((OpElementAsgnNode) node);
            case ORNODE: return buildOr((OrNode) node);
            case PATTERNCASENODE: return buildPatternCase((PatternCaseNode) node);
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
            case SPLATNODE: return buildSplat(result, (SplatNode) node);
            case STRNODE: return buildStr((StrNode) node);
            case SUPERNODE: return buildSuper(result, (SuperNode) node);
            case SVALUENODE: return buildSValue(result, (SValueNode) node);
            case SYMBOLNODE: return buildSymbol((SymbolNode) node);
            case TRUENODE: return tru();
            case UNDEFNODE: return buildUndef((UndefNode) node);
            case UNTILNODE: return buildUntil((UntilNode) node);
            case VALIASNODE: return buildVAlias((VAliasNode) node);
            case VCALLNODE: return buildVCall(result, (VCallNode) node);
            case WHILENODE: return buildWhile((WhileNode) node);
            case WHENNODE: assert false : "When nodes are handled by case node compilation."; return null;
            case XSTRNODE: return buildXStr(result, (XStrNode) node);
            case YIELDNODE: return buildYield(result, (YieldNode) node);
            case ZARRAYNODE: return buildZArray(result);
            case ZSUPERNODE: return buildZSuper(result, (ZSuperNode) node);
            default: throw notCompilable("Unknown node encountered in builder", node);
        }
    }

    public IRBuilderAST newIRBuilder(IRManager manager, IRScope newScope) {
        return new IRBuilderAST(manager, newScope, this);
    }

    public Operand build(Node node) {
        return build(null, node);
    }

    /*
     * @param result preferred result variable (this reduces temp vars pinning values).
     * @param node to be built
     */
    public Operand build(Variable result, Node node) {
        if (node == null) return null;

        boolean savedExecuteOnce = executesOnce;
        try {
            if (executesOnce) executesOnce = node.executesOnce();

            if (hasListener()) getManager().getIRScopeListener().startBuildOperand(node, scope);

            Operand operand = buildOperand(result, node);

            if (hasListener()) getManager().getIRScopeListener().endBuildOperand(node, scope, operand);

            return operand;
        } finally {
            executesOnce = savedExecuteOnce;
        }
    }

    public Operand buildLambda(LambdaNode node) {
        return buildLambda(node.getArgs(), node.getBody(), node.getScope(), Signature.from(node), node.getLine());
    }

    public Operand buildEncoding(EncodingNode node) {
        return buildEncoding(node.getEncoding());
    }

    // Non-arg masgn
    public Operand buildMultipleAsgn(MultipleAsgnNode multipleAsgnNode) {
        Node valueNode = multipleAsgnNode.getValueNode();
        Map<Node, Operand> reads = new HashMap<>();
        final List<Tuple<Node, ResultInstr>> assigns = new ArrayList<>(4);
        Variable values = temp();
        buildMultipleAssignment2(multipleAsgnNode, assigns, reads, values);

        Variable ret = getValueInTemporaryVariable(build(valueNode));
        if (valueNode instanceof ArrayNode || valueNode instanceof ZArrayNode) {
            copy(values, ret);
        } else if (valueNode instanceof ILiteralNode) {
            // treat a single literal value as a single-element array
            copy(values, new Array(new Operand[]{ret}));
        } else {
            addResultInstr(new ToAryInstr(values, ret));
        }

        for (Tuple<Node, ResultInstr> assign: assigns) {
            addInstr((Instr) assign.b);
        }

        buildAssignment(assigns, reads);

        return ret;
    }


    protected void buildAssignment(List<Tuple<Node, ResultInstr>> assigns, Map<Node, Operand> reads) {

        for (Tuple<Node, ResultInstr> assign: assigns) {
            Node node = assign.a;
            Variable rhs = assign.b.getResult();
            switch (node.getNodeType()) {
                case ATTRASSIGNNODE: {
                    AttrAssignNode attrAssignNode = (AttrAssignNode) node;
                    Operand receiver = reads.get(attrAssignNode.getReceiverNode());
                    Array holders = (Array) reads.get(attrAssignNode.getArgsNode());
                    int flags = ((Integer) holders.get(holders.size() - 1)).value;
                    Operand[] args = new Operand[holders.size() - 1];
                    System.arraycopy(holders.getElts(), 0, args, 0, args.length);
                    args = addArg(args, rhs);
                    addInstr(AttrAssignInstr.create(scope, receiver, attrAssignNode.getName(), args, flags, scope.maybeUsingRefinements()));
                    break;
                }
                case CLASSVARASGNNODE:
                    addInstr(new PutClassVariableInstr(classVarDefinitionContainer(), ((ClassVarAsgnNode) node).getName(), rhs));
                    break;
                case CONSTDECLNODE: {
                    ConstDeclNode constDeclNode = (ConstDeclNode) node;
                    Operand receiver = reads.get(node);
                    if (receiver == null) {
                        putConstant(constDeclNode.getName(), rhs);
                    } else {
                        putConstant(receiver, constDeclNode.getName(), rhs);
                    }
                }
                break;
                case DASGNNODE: {
                    DAsgnNode variable = (DAsgnNode) node;
                    copy(getLocalVariable(variable.getName(), variable.getDepth()), rhs);
                    break;
                }
                case GLOBALASGNNODE:
                    addInstr(new PutGlobalVarInstr(((GlobalAsgnNode) node).getName(), rhs));
                    break;
                case INSTASGNNODE:
                    // NOTE: if 's' happens to the a class, this is effectively an assignment of a class instance variable
                    addInstr(new PutFieldInstr(buildSelf(), ((InstAsgnNode) node).getName(), rhs));
                    break;
                case LOCALASGNNODE: {
                    LocalAsgnNode localVariable = (LocalAsgnNode) node;
                    copy(getLocalVariable(localVariable.getName(), localVariable.getDepth()), rhs);
                    break;
                }
                case MULTIPLEASGNNODE: {
                    //buildAssignment(assigns, reads);
                    break;
                }
            }
        }
    }
    // List of all left to right reads which needs to happen before any assignments.  Original node is a key to link
    // to actual assignment values.

    public void buildMultipleAssignment2(final MultipleAsgnNode multipleAsgnNode, List<Tuple<Node, ResultInstr>> assigns,
                                         Map<Node, Operand> reads, Variable values) {
        final ListNode masgnPre = multipleAsgnNode.getPre();
        int i = 0;

        if (masgnPre != null) {
            for (Node an: masgnPre.children()) {
                ResultInstr get = new ReqdArgMultipleAsgnInstr(temp(), values, i);
                assigns.add(new Tuple<>(an, get));
                processReads(get.getResult(), assigns, reads, an);
                i++;
            }
        }

        Node restNode = multipleAsgnNode.getRest();
        int postCount = multipleAsgnNode.getPostCount();
        if (restNode != null && !(restNode instanceof StarNode)) {
            ResultInstr get = new RestArgMultipleAsgnInstr(temp(), values, 0, i, postCount);
            assigns.add(new Tuple<>(restNode, get));
            processReads(get.getResult(), assigns, reads, restNode);
        }

        final ListNode masgnPost = multipleAsgnNode.getPost();
        if (masgnPost != null) {
            int j = 0;
            for (Node an: masgnPost.children()) {
                ResultInstr get = new ReqdArgMultipleAsgnInstr(temp(), values, j, i, postCount);
                assigns.add(new Tuple<>(an, get));
                processReads(get.getResult(), assigns, reads, an);
                j++;
            }
        }
    }

    private void processReads(Variable rhsVal, List<Tuple<Node, ResultInstr>> assigns, Map<Node, Operand> reads, Node node) {
        switch (node.getNodeType()) {
            case ATTRASSIGNNODE: {
                // We do some wild stuff here.  We need to pass flags and our map only holds references to nodes.
                // We do not have block.  args and receiver are used.  So we stuff flags onto the end of args.
                AttrAssignNode attrAssignNode = (AttrAssignNode) node;
                reads.put(attrAssignNode.getReceiverNode(), build(attrAssignNode.getReceiverNode()));
                int[] flags = new int[]{0};
                Operand[] args = setupCallArgs(attrAssignNode.getArgsNode(), flags);
                Operand[] hackyArgs = new Operand[args.length + 1];
                System.arraycopy(args, 0, hackyArgs, 0, args.length);
                hackyArgs[args.length] = new Integer(flags[0]);
                reads.put(attrAssignNode.getArgsNode(), new Array(hackyArgs));
            }
            break;
            case CLASSVARASGNNODE:
                reads.put(node, classVarDefinitionContainer());
                break;
            case CONSTDECLNODE: {
                ConstDeclNode constDeclNode = (ConstDeclNode) node;
                Node constNode = constDeclNode.getConstNode();
                if (constNode == null) {
                    reads.put(node, null);
                } else if (constNode instanceof Colon2Node) {
                    reads.put(node, build(((Colon2Node) constNode).getLeftNode()));
                } else if (constNode instanceof Colon3Node) {
                    reads.put(node, getManager().getObjectClass());
                } else {
                    reads.put(node, build(constNode));
                }
            }
            break;
            case MULTIPLEASGNNODE: {
                Variable subRet = temp();
                assigns.add(new Tuple<>(node, new ToAryInstr(subRet, rhsVal)));

                buildMultipleAssignment2((MultipleAsgnNode) node, assigns, reads, subRet);
            }
            break;
        }
    }

    protected Operand buildLazyWithOrder(CallNode node, Label lazyLabel, Label endLabel, boolean preserveOrder) {
        Operand value = buildCall(null, node, lazyLabel, endLabel);

        // We need to preserve order in cases (like in presence of assignments) except that immutable
        // literals can never change value so we can still emit these out of order.
        return preserveOrder && !(value instanceof ImmutableLiteral) ? copy(value) : value;
    }

    // Return the last argument in the list as this represents rhs of the overall attrassign expression
    // e.g. 'a[1] = 2 #=> 2' or 'a[1] = 1,2,3 #=> [1,2,3]'
    // _value only used in prism...in AST we need to calculate the last arg.
    @Override
    protected Operand[] buildAttrAssignCallArgs(Node args, Operand[] outValue, boolean containsAssignment) {
        if (args == null) return EMPTY_ARRAY;

        switch (args.getNodeType()) {
            case ARRAYNODE: {     // a[1] = 2; a[1,2,3] = 4,5,6
                Node[] children = ((ListNode) args).children();
                Operand[] operands = new Operand[children.length];
                for (int i = 0; i < children.length; i++) {
                    operands[i] = buildWithOrder(children[i], containsAssignment);
                    outValue[0] = operands[i];
                }
                return operands;
            }
            case ARGSCATNODE: {
                ArgsCatNode argsCatNode = (ArgsCatNode)args;
                Operand lhs = build(argsCatNode.getFirstNode());
                Operand rhs = build(argsCatNode.getSecondNode());
                outValue[0] = rhs;
                Variable res = addResultInstr(new BuildCompoundArrayInstr(temp(), lhs, rhs, false, false));
                return new Operand[] { new Splat(res) };
            }
            case ARGSPUSHNODE:  { // a[1, *b] = 2
                ArgsPushNode argsPushNode = (ArgsPushNode)args;
                Operand lhs = build(argsPushNode.getFirstNode());
                Operand rhs = build(argsPushNode.getSecondNode());
                outValue[0] = rhs;
                Variable res = addResultInstr(new BuildCompoundArrayInstr(temp(), lhs, rhs, true, false));
                return new Operand[] { new Splat(res) };
            }
            case SPLATNODE: {     // a[1] = *b
                outValue[0] = new Splat(buildSplat(temp(), (SplatNode)args));
                return new Operand[] { outValue[0] };
            }
        }

        throw notCompilable("Invalid node for attrassign call args", args);
    }


    // Looks weird to see no key comparison but we know if this is called there are only kwrest value(s).
    private Operand buildRestKeywordArgs(HashNode keywordArgs, int[] flags) {
        flags[0] |= CALL_KEYWORD_REST;
        List<KeyValuePair<Node, Node>> pairs = keywordArgs.getPairs();

        if (pairs.size() == 1) { // Only a single rest arg here.  Do not bother to merge.
            if (pairs.get(0).getValue() instanceof NilNode) return new Hash(new ArrayList<>(4)); // **nil

            Operand splat = buildWithOrder(pairs.get(0).getValue(), keywordArgs.containsVariableAssignment());

            return addResultInstr(new RuntimeHelperCall(temp(), HASH_CHECK, new Operand[] { splat }));
        }

        Variable splatValue = copy(new Hash(new ArrayList<>(1)));
        for (KeyValuePair<Node, Node> pair: pairs) {
            Operand splat = pair.getValue() instanceof NilNode ?
                    new Hash(new ArrayList<>(1)) : // **nil
                    buildWithOrder(pair.getValue(), keywordArgs.containsVariableAssignment()); // **r
            addInstr(new RuntimeHelperCall(splatValue, MERGE_KWARGS, new Operand[] { splatValue, splat, fals() }));
        }

        return splatValue;
    }

    protected Operand buildCallKeywordArguments(HashNode keywords, int[] flags) {
        flags[0] |= CALL_KEYWORD;

        if (keywords.hasOnlyRestKwargs()) return buildRestKeywordArgs(keywords, flags);

        return buildHash(keywords);
    }

    // This is very similar to buildArray but when building generic arrays we do not want to mark callinfo
    // since array builds are not directly for setting up a call.
    protected Operand buildCallArgsArrayForSplat(ListNode args, int[] flags) {
        Node[] nodes = args.children();
        Operand[] elts = new Operand[nodes.length];
        boolean containsAssignments = args.containsVariableAssignment();
        Operand keywordRestSplat = null;
        for (int i = 0; i < nodes.length; i++) {
            elts[i] = buildWithOrder(nodes[i], containsAssignments);
            if (i == nodes.length - 1 && nodes[i] instanceof HashNode && !((HashNode) nodes[i]).isLiteral()) {
                flags[0] |= CALL_KEYWORD;
                if (((HashNode) nodes[i]).hasOnlyRestKwargs()) keywordRestSplat = elts[i];
            }
        }

        // We have some amount of ** on the end of this array construction.  This is handled in IR since we
        // do not want arrays to have to know if it has an UNDEFINED on the end and then not include it.  Also
        // since we must evaluate array values left to right we cannot look at last argument first to eliminate
        // complicating the array sizes computation.  Luckily, this is a rare feature to see used in actual code
        // so externalizing this in IR should not be a big deal.
        if (keywordRestSplat != null) {
            Variable test = addResultInstr(new RuntimeHelperCall(temp(), IS_HASH_EMPTY, new Operand[]{ keywordRestSplat }));
            final Variable result = temp();
            if_else(test, tru(),
                    () -> copy(result, new Array(removeArg(elts))),
                    () -> copy(result, new Array(elts)));
            return result;
        } else {
            return new Array(elts);
        }
    }

    protected Operand[] buildCallArgsArray(ListNode args, int[] flags) {
        Node[] children = args.children();
        int numberOfArgs = children.length;
        Operand[] builtArgs = new Operand[numberOfArgs];
        boolean hasAssignments = args.containsVariableAssignment();

        for (int i = 0; i < numberOfArgs; i++) {
            if (i == numberOfArgs - 1 && children[i] instanceof HashNode && !((HashNode) children[i]).isLiteral()) {
                HashNode hash = (HashNode) children[i];
                builtArgs[i] = buildCallKeywordArguments(hash, flags);
            } else {
                builtArgs[i] = buildWithOrder(children[i], hasAssignments);
            }
        }

        return builtArgs;
    }

    protected Operand[] buildCallArgs(Node args, int[] flags) {
        switch (args.getNodeType()) {
            case ARGSCATNODE:
            case ARGSPUSHNODE:
                Operand lhs = build(((TwoValueNode) args).getFirstNode());
                Node secondNode = ((TwoValueNode) args).getSecondNode();

                flags[0] |= CALL_SPLATS;
                Operand valueToSplat;
                if (secondNode instanceof ListNode && !(secondNode instanceof DNode)) {
                    valueToSplat = buildCallArgsArrayForSplat((ListNode) secondNode, flags);
                } else if (secondNode instanceof HashNode && !((HashNode) secondNode).isLiteral()) {
                    valueToSplat = buildCallKeywordArguments((HashNode) secondNode, flags);
                } else {
                    valueToSplat = build(secondNode);

                }
                Operand array = addResultInstr(new BuildCompoundArrayInstr(temp(), lhs, valueToSplat,
                        args.getNodeType() == NodeType.ARGSPUSHNODE, (flags[0] & CALL_KEYWORD_REST) != 0));

                return new Operand[] { new Splat(addResultInstr(new BuildSplatInstr(temp(), array, false))) };
            case ARRAYNODE:
                return buildCallArgsArray((ListNode) args, flags);
            case SPLATNODE:
                flags[0] |= CALL_SPLATS;
                return new Operand[] { new Splat(addResultInstr(new BuildSplatInstr(temp(), build(((SplatNode) args).getValue()), true))) };

        }

        throw notCompilable("Invalid node for call args: ", args);
    }

    Operand buildYieldArgs(Node args, int[] flags) {
        if (args == null) return UndefinedValue.UNDEFINED;

        switch (args.getNodeType()) {
            case ARGSCATNODE:
            case ARGSPUSHNODE:
                Operand lhs = build(((TwoValueNode) args).getFirstNode());
                Node secondNode = ((TwoValueNode) args).getSecondNode();

                flags[0] |= CALL_SPLATS;
                Operand valueToSplat;
                if (secondNode instanceof ListNode) {
                    valueToSplat = buildCallArgsArrayForSplat((ListNode) secondNode, flags);
                } else if (secondNode instanceof HashNode && !((HashNode) secondNode).isLiteral()) {
                    valueToSplat = buildCallKeywordArguments((HashNode) secondNode, flags);
                } else {
                    valueToSplat = build(secondNode);

                }
                Operand array = addResultInstr(new BuildCompoundArrayInstr(temp(), lhs, valueToSplat,
                        args.getNodeType() == NodeType.ARGSPUSHNODE, (flags[0] & CALL_KEYWORD_REST) != 0));

                return new Splat(addResultInstr(new BuildSplatInstr(temp(), array, false)));
            case ARRAYNODE: {
                return new Array(buildCallArgsArray((ListNode) args, flags));
            }
            case SPLATNODE:
                flags[0] |= CALL_SPLATS;
                return new Splat(addResultInstr(new BuildSplatInstr(temp(), build(args), false)));
            default:
                return build(args);
        }
    }

    // This method is called to build assignments for a multiple-assignment instruction
    protected void buildAssignment(Node node, Operand rhsVal) {
        switch (node.getNodeType()) {
            case ATTRASSIGNNODE:
                buildAttrAssignAssignment(node, rhsVal);
                break;
            case CLASSVARASGNNODE:
                addInstr(new PutClassVariableInstr(classVarDefinitionContainer(), ((ClassVarAsgnNode)node).getName(), rhsVal));
                break;
            case CONSTDECLNODE:
                buildConstDeclAssignment((ConstDeclNode) node, () -> rhsVal);
                break;
            case DASGNNODE: {
                DAsgnNode variable = (DAsgnNode) node;
                copy(getLocalVariable(variable.getName(), variable.getDepth()), rhsVal);
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
                copy(getLocalVariable(localVariable.getName(), localVariable.getDepth()), rhsVal);
                break;
            }
            case ZEROARGNODE:
                throw notCompilable("Shouldn't get here; zeroarg does not do assignment", node);
            case MULTIPLEASGNNODE: {
                buildMultipleAssignment((MultipleAsgnNode) node, addResultInstr(new ToAryInstr(temp(), rhsVal)));
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

    protected Variable receiveBlockArg(Variable v, Operand argsArray, int argIndex, boolean isSplat) {
        if (argsArray != null) {
            // We are in a nested receive situation -- when we are not at the root of a masgn tree
            // Ex: We are trying to receive (b,c) in this example: "|a, (b,c), d| = ..."
            if (isSplat) addInstr(new RestArgMultipleAsgnInstr(v, argsArray, argIndex));
            else addInstr(new ReqdArgMultipleAsgnInstr(v, argsArray, argIndex));
        } else {
            // argsArray can be null when the first node in the args-node-ast is a multiple-assignment
            // For example, for-nodes
            // FIXME: We can have keywords here but this is more complicated to get here
            Variable keywords = copy(UndefinedValue.UNDEFINED);
            addInstr(isSplat ? new ReceiveRestArgInstr(v, keywords, argIndex, argIndex) : new ReceivePreReqdArgInstr(v, keywords, argIndex));
        }

        return v;
    }

    // This method is called to build arguments for a block!
    public void buildBlockArgsAssignment(Node node, Operand argsArray, int argIndex, boolean isSplat) {
        switch (node.getNodeType()) {
            case ATTRASSIGNNODE:
                buildAttrAssignAssignment(node,
                        receiveBlockArg(temp(), argsArray, argIndex, isSplat));
                break;
            case LOCALASGNNODE:
            case DASGNNODE:
                receiveBlockArg(getBlockArgVariable(((INameNode) node).getName(), ((IScopedNode) node).getDepth()),
                        argsArray, argIndex, isSplat);
                break;
            case CLASSVARASGNNODE:
                addInstr(new PutClassVariableInstr(classVarDefinitionContainer(), ((ClassVarAsgnNode)node).getName(),
                        receiveBlockArg(temp(), argsArray, argIndex, isSplat)));
                break;
            case CONSTDECLNODE:
                buildConstDeclAssignment((ConstDeclNode) node, () -> receiveBlockArg(temp(), argsArray, argIndex, isSplat));
                break;
            case GLOBALASGNNODE:
                addInstr(new PutGlobalVarInstr(((GlobalAsgnNode)node).getName(),
                        receiveBlockArg(temp(), argsArray, argIndex, isSplat)));
                break;
            case INSTASGNNODE:
                // NOTE: if 's' happens to the a class, this is effectively an assignment of a class instance variable
                addInstr(new PutFieldInstr(buildSelf(), ((InstAsgnNode)node).getName(),
                        receiveBlockArg(temp(), argsArray, argIndex, isSplat)));
                break;
            case ZEROARGNODE:
                throw notCompilable("Shouldn't get here; zeroarg does not do assignment", node);
            case MULTIPLEASGNNODE: { // only for 'for' nodes.
                ListNode sourceArray = ((MultipleAsgnNode) node).getPre();
                int i = 0;
                for (Node an: sourceArray.children()) {
                    buildBlockArgsAssignment(an, null, i, false);
                    i++;
                }
                break;
            }
            default:
                throw notCompilable("Can't build assignment node", node);
        }
    }

    public Operand buildAlias(final AliasNode alias) {
        return buildAlias(build(alias.getNewName()), build(alias.getOldName()));
    }

    public Operand buildAnd(AndNode node) {
        return buildAnd(build(node.getFirstNode()), () -> build(node.getSecondNode()), binaryType(node.getFirstNode()));
    }

    public Operand buildArray(ArrayNode node, boolean operandOnly) {
        Node[] nodes = node.children();
        Operand[] elts = new Operand[nodes.length];
        boolean containsAssignments = node.containsVariableAssignment();
        Operand keywordRestSplat = null;
        for (int i = 0; i < nodes.length; i++) {
            elts[i] = buildWithOrder(nodes[i], containsAssignments);
            if (nodes[i] instanceof HashNode && ((HashNode) nodes[i]).hasOnlyRestKwargs()) keywordRestSplat = elts[i];
        }

        // We have some amount of ** on the end of this array construction.  This is handled in IR since we
        // do not want arrays to have to know if it has an UNDEFINED on the end and then not include it.  Also
        // since we must evaluate array values left to right we cannot look at last argument first to eliminate
        // complicating the array sizes computation.  Luckily, this is a rare feature to see used in actual code
        // so externalizing this in IR should not be a big deal.
        if (keywordRestSplat != null) {
            Variable test = addResultInstr(new RuntimeHelperCall(temp(), IS_HASH_EMPTY, new Operand[]{ keywordRestSplat }));
            final Variable result = temp();
            if_else(test, tru(),
                    () -> copy(result, new Array(removeArg(elts))),
                    () -> copy(result, new Array(elts)));
            return result;
        } else {
            Operand array = new Array(elts);
            return operandOnly ? array : copy(array);
        }
    }

    public Operand buildArgsCat(Variable result, ArgsCatNode argsCatNode) {
        if (result == null) result = temp();
        Operand lhs = build(argsCatNode.getFirstNode());
        Operand rhs = build(argsCatNode.getSecondNode());

        return addResultInstr(new BuildCompoundArrayInstr(result, lhs, rhs, false, false));
    }

    public Operand buildArgsPush(Variable result, ArgsPushNode node) {
        if (result == null) result = temp();
        Operand lhs = build(node.getFirstNode());
        Operand rhs = build(node.getSecondNode());

        return addResultInstr(new BuildCompoundArrayInstr(result, lhs, rhs, true, false));
    }

    private Operand buildAttrAssign(Variable result, AttrAssignNode node) {
        return buildAttrAssign(result, node.getReceiverNode(), node.getArgsNode(), node.getBlockNode(),
                node.getName(), node.isLazy(), node.containsVariableAssignment());
    }

    public Operand buildAttrAssignAssignment(Node node, Operand value) {
        final AttrAssignNode attrAssignNode = (AttrAssignNode) node;
        Operand obj = build(attrAssignNode.getReceiverNode());
        int[] flags = new int[] { 0 };
        Operand[] args = setupCallArgs(attrAssignNode.getArgsNode(), flags);
        args = addArg(args, value);
        addInstr(AttrAssignInstr.create(scope, obj, attrAssignNode.getName(), args, flags[0], scope.maybeUsingRefinements()));
        return value;
    }

    public Operand buildBackref(Variable result, BackRefNode node) {
        if (result == null) result = temp();
        return addResultInstr(new BuildBackrefInstr(result, node.getType()));
    }

    public Operand buildBegin(BeginNode beginNode) {
        return build(beginNode.getBodyNode());
    }

    public Operand buildBignum(BignumNode node) {
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

    public Operand buildBreak(BreakNode node) {
        return buildBreak(() -> build(node.getValueNode()), node.getLine());
    }

    public Operand buildCall(Variable aResult, CallNode callNode, Label lazyLabel, Label endLabel) {
        RubySymbol name = methodName = callNode.getName();
        Node receiverNode = callNode.getReceiverNode();

        // Ruby 2.7+ treats explicit self.call the same as if it was just call.  We transform rather than testing receiver.
        if (receiverNode instanceof SelfNode) {
            FCallNode fcall = new FCallNode(callNode.getLine(), callNode.getName(), callNode.getArgsNode(), callNode.getIterNode());
            return buildFCall(aResult, fcall);
        }
        String id = name.idString(); // ID Str ok here since compared strings are all 7-bit.

        if (Options.IR_STRING_FREEZE.load()) {
            // Frozen string optimization: check for "string".freeze
            if (receiverNode instanceof StrNode && (id.equals("freeze") || id.equals("-@"))) {
                StrNode asString = (StrNode) receiverNode;
                return new FrozenString(asString.getValue(), asString.getCodeRange(), scope.getFile(), asString.getLine());
            }
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

        final Variable result = aResult == null ? temp() : aResult;

        // obj["string"] optimization for Hash
        ArrayNode argsAry;
        Node arg0;
        if (!callNode.isLazy() &&
                // aref only
                id.equals("[]") &&

                // single literal string argument
                callNode.getArgsNode() instanceof ArrayNode &&
                (argsAry = (ArrayNode) callNode.getArgsNode()).size() == 1 &&
                (arg0 = argsAry.get(0)) instanceof StrNode &&

                // not pre-frozen (which can just go through normal call path)
                ((StrNode) arg0).getStringStyle() != Frozen &&

                // obj#[] definitely not refined
                !scope.maybeUsingRefinements() &&

                // no block argument
                callNode.getIterNode() == null) {

            StrNode keyNode = (StrNode) argsAry.get(0);
            FrozenString key = new FrozenString(keyNode.getValue(), keyNode.getCodeRange(), scope.getFile(), keyNode.getLine());
            addInstr(ArrayDerefInstr.create(scope, result, receiver, key, 0));
            return result;
        }

        if (callNode.isLazy()) addInstr(new BNilInstr(lazyLabel, receiver));

        createCall(result, receiver, NORMAL, name, callNode.getArgsNode(), callNode.getIterNode(), callNode.getLine(), callNode.isNewline());

        if (compileLazyLabel) {
            addInstr(new JumpInstr(endLabel));
            addInstr(new LabelInstr(lazyLabel));
            addInstr(new CopyInstr(result, nil()));
            addInstr(new LabelInstr(endLabel));
        }

        return result;
    }

    protected boolean isNilRest(Node rest) {
        return rest instanceof NilRestArgNode;
    }

    protected void buildAssocs(Label testEnd, Operand original, Variable result, HashNode assocs, boolean inAlteration,
                     boolean isSinglePattern, Variable errorString, boolean hasRest, Variable d) {
        List<KeyValuePair<Node,Node>> kwargs = assocs.getPairs();

        for (KeyValuePair<Node,Node> pair: kwargs) {
            Operand key = build(pair.getKey());
            call(result, d, "key?", key);
            copy(errorString, key); // Sets to symbol which will not be nil or a regular string.
            cond_ne_true(testEnd, result);

            String method = hasRest ? "delete" : "[]";
            Operand value = call(temp(), d, method, key);
            buildPatternEach(testEnd, result, original, copy(nil()), value, pair.getValue(), inAlteration, isSinglePattern, errorString);
            cond_ne_true(testEnd, result);
        }
    }

    protected Variable buildPatternEach(Label testEnd, Variable result, Operand original, Variable deconstructed, Operand value,
                              Node exprNodes, boolean inAlternation, boolean isSinglePattern, Variable errorString) {
        if (exprNodes instanceof ArrayPatternNode) {
            ArrayPatternNode node = (ArrayPatternNode) exprNodes;
            buildArrayPattern(testEnd, result, deconstructed, node.getConstant(), node.getPre(), node.getRestArg(), node.getPost(), value, inAlternation, isSinglePattern, errorString);
        } else if (exprNodes instanceof HashPatternNode) {
            HashPatternNode node = (HashPatternNode) exprNodes;
            buildHashPattern(testEnd, result, deconstructed, node.getConstant(), node.getKeywordArgs(), node.getKeys(), node.getRestArg(), value, inAlternation, isSinglePattern, errorString);
        } else if (exprNodes instanceof FindPatternNode) {
            FindPatternNode node = (FindPatternNode) exprNodes;
            buildFindPattern(testEnd, result, deconstructed, node.getConstant(), node.getPreRestArg(), node.getArgs(), node.getPostRestArg(), value, inAlternation, isSinglePattern, errorString);
        } else if (exprNodes instanceof HashNode) {
            KeyValuePair<Node,Node> pair = ((HashNode) exprNodes).getPairs().get(0);
            buildPatternEachHash(testEnd, result, original, deconstructed, value, pair.getKey(), pair.getValue(), inAlternation, isSinglePattern, errorString);
        } else if (exprNodes instanceof IfNode) {
            IfNode node = (IfNode) exprNodes;
            buildPatternEachIf(result, original, deconstructed, value, node.getCondition(), node.getThenBody(), node.getElseBody(), inAlternation, isSinglePattern, errorString);
        } else if (exprNodes instanceof LocalAsgnNode) {
            LocalAsgnNode node = (LocalAsgnNode) exprNodes;
            buildPatternLocal(value, node.getName(), node.getLine(), node.getDepth(), inAlternation);
        } else if (exprNodes instanceof StarNode) {
            // Do nothing.
        } else if (exprNodes instanceof DAsgnNode) {
            DAsgnNode node = (DAsgnNode) exprNodes;
            buildPatternLocal(value, node.getName(), node.getLine(), node.getDepth(), inAlternation);
        } else if (exprNodes instanceof OrNode) {
            buildPatternOr(testEnd, original, result, deconstructed, value, ((OrNode) exprNodes).getFirstNode(),
                    ((OrNode) exprNodes).getSecondNode(), isSinglePattern, errorString);
        } else {
            Operand expression = build(exprNodes);
            boolean needsSplat = exprNodes instanceof ArgsPushNode || exprNodes instanceof SplatNode || exprNodes instanceof ArgsCatNode;

            addInstr(new EQQInstr(scope, result, expression, value, needsSplat, true, scope.maybeUsingRefinements()));
            if (isSinglePattern) {
                buildPatternSetEQQError(errorString, result, original, expression, value);
            }
        }

        return result;
    }

    @Override
    protected Node getInExpression(Node node) {
        return ((InNode) node).getExpression();
    }

    @Override
    protected Node getInBody(Node node) {
        return ((InNode) node).getBody();
    }

    @Override
    protected boolean isBareStar(Node node) {
        return node instanceof StarNode;
    }

    public Operand buildPatternCase(PatternCaseNode node) {
        return buildPatternCase(node.getCaseNode(), node.getCases(), node.getElseNode());
    }

    public Operand buildCase(CaseNode caseNode) {
        if (caseNode.getCaseNode() != null && !scope.maybeUsingRefinements()) {
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
                        return buildOptimizedCaseWhen(caseNode, RubyFixnum.class, (n) -> ((FixnumNode) n).getValue());
                    case SYMBOLNODE:
                        return buildOptimizedCaseWhen(caseNode, RubySymbol.class, (n) -> (long) ((SymbolNode) n).getName().getId());
                }
            }
        }

        return buildCase(caseNode.getCaseNode(), caseNode.getCases().children(), caseNode.getElseNode());
    }

    protected Node whenBody(WhenNode when) {
        return when.getBodyNode();
    }

    @Override
    protected boolean containsVariableAssignment(Node node) {
        return node.containsVariableAssignment();
    }

    @Override
    protected Operand frozen_string(Node node) {
        ((StrNode) node).setStringStyle(Frozen);
        return buildStrRaw((StrNode) node);
    }

    @Override
    protected int getLine(Node node) {
        return node.getLine();
    }

    private void buildWhenSplatValues(Variable eqqResult, Node node, Operand testValue, Label bodyLabel,
                                      Set<IRubyObject> seenLiterals, Map<IRubyObject, java.lang.Integer> origLocs) {
        if (node instanceof ListNode && !(node instanceof DNode) && !(node instanceof ArrayNode)) {
            buildWhenValues(eqqResult, ((ListNode) node).children(), testValue, bodyLabel, seenLiterals, origLocs);
        } else if (node instanceof SplatNode) {
            buildWhenValue(eqqResult, testValue, bodyLabel, node, seenLiterals, origLocs, true);
        } else if (node instanceof ArgsCatNode) {
            ArgsCatNode catNode = (ArgsCatNode) node;
            buildWhenSplatValues(eqqResult, catNode.getFirstNode(), testValue, bodyLabel, seenLiterals, origLocs);
            buildWhenSplatValues(eqqResult, catNode.getSecondNode(), testValue, bodyLabel, seenLiterals, origLocs);
        } else if (node instanceof ArgsPushNode) {
            ArgsPushNode pushNode = (ArgsPushNode) node;
            buildWhenSplatValues(eqqResult, pushNode.getFirstNode(), testValue, bodyLabel, seenLiterals, origLocs);
            buildWhenValue(eqqResult, testValue, bodyLabel, pushNode.getSecondNode(), seenLiterals, origLocs, false);
        } else {
            buildWhenValue(eqqResult, testValue, bodyLabel, node, seenLiterals, origLocs, true);
        }
    }

    protected void buildWhenArgs(WhenNode whenNode, Operand testValue, Label bodyLabel,
                                 Set<IRubyObject> seenLiterals, Map<IRubyObject, java.lang.Integer> origLocs) {
        Variable eqqResult = temp();
        Node exprNodes = whenNode.getExpressionNodes();

        if (exprNodes instanceof BlockNode list) {
            buildWhenValue(eqqResult, testValue, bodyLabel, list.getLast(), seenLiterals, origLocs, false);
        } else if (exprNodes instanceof ListNode && !(exprNodes instanceof DNode) && !(exprNodes instanceof ArrayNode) && !(exprNodes instanceof ZArrayNode)) {
            buildWhenValues(eqqResult, ((ListNode) exprNodes).children(), testValue, bodyLabel, seenLiterals, origLocs);
        } else if (exprNodes instanceof ArgsPushNode || exprNodes instanceof SplatNode || exprNodes instanceof ArgsCatNode) {
            buildWhenSplatValues(eqqResult, exprNodes, testValue, bodyLabel, seenLiterals, origLocs);
        } else {
            buildWhenValue(eqqResult, testValue, bodyLabel, exprNodes, seenLiterals, origLocs, false);
        }
    }

    // Note: This is potentially a little wasteful in that we eagerly create these literals for a duplicated warning
    // check.  In most cases these would be made anyways (e.g. symbols/fixnum) but in others we double allocation
    // (e.g. strings).
    protected IRubyObject getWhenLiteral(Node node) {
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

    @Override
    protected boolean isLiteralString(Node node) {
        return node instanceof StrNode;
    }

    private <T extends Node & ILiteralNode> Variable buildOptimizedCaseWhen(
            CaseNode caseNode, Class caseClass, Function<T, Long> caseFunction) {
        // get the incoming case value
        Operand value = build(caseNode.getCaseNode());

        Map<Node, Label> nodeBodies = new HashMap<>();

        Map<java.lang.Integer, Tuple<Operand, Label>> jumpTable = gatherLiteralWhenBodies(caseNode, nodeBodies, caseFunction);
        Map.Entry<java.lang.Integer, Tuple<Operand, Label>>[] jumpEntries = sortJumpEntries(jumpTable);

        Label     endLabel  = getNewLabel();
        boolean   hasElse   = (caseNode.getElseNode() != null);
        Label     elseLabel = getNewLabel();
        Variable  result    = temp();

        buildOptimizedSwitch(jumpTable, jumpEntries, elseLabel, value, caseClass);

        return buildStandardCaseWhen(caseNode, nodeBodies, endLabel, hasElse, elseLabel, value, result);
    }

    private Operand buildOptimizedWhenOperand(Node node) {
        if (node instanceof SymbolNode) {
            return buildSymbol((SymbolNode) node);
        } else if (node instanceof FixnumNode) {
            return buildFixnum((FixnumNode) node);
        }

        throw new NotCompilableException("unexpected optimized when value encountered: " + node);
    }

    private <T extends Node & ILiteralNode> Map<java.lang.Integer, Tuple<Operand, Label>> gatherLiteralWhenBodies(
            CaseNode caseNode, Map<Node, Label> nodeBodies, Function<T, Long> caseFunction) {
        Map<java.lang.Integer, Tuple<Operand, Label>> jumpTable = new HashMap<>();
        Map<java.lang.Integer, Node> origTable = new HashMap<>();

        // gather literal when bodies or bail
        for (Node aCase : caseNode.getCases().children()) {
            WhenNode whenNode = (WhenNode) aCase;
            Label bodyLabel = getNewLabel();

            T expr = (T) whenNode.getExpressionNodes();
            long exprLong = caseFunction.apply(expr);
            if (exprLong > java.lang.Integer.MAX_VALUE) throw notCompilable("optimized case has long-ranged value", caseNode);

            if (jumpTable.get((int) exprLong) == null) {
                jumpTable.put((int) exprLong, new Tuple<>(buildOptimizedWhenOperand(expr), bodyLabel));
                origTable.put((int) exprLong, whenNode);
                nodeBodies.put(whenNode, bodyLabel);
            } else {
                var context = getManager().getRuntime().getCurrentContext();
                warning(context, "'when' clause on line " + (getLine(expr) + 1) +
                        " duplicates 'when' clause on line " + (origTable.get((int) exprLong).getLine() + 1) + " and is ignored");
            }
        }

        return jumpTable;
    }

    private static Map.Entry<java.lang.Integer, Tuple<Operand, Label>>[] sortJumpEntries(Map<java.lang.Integer, Tuple<Operand, Label>> jumpTable) {
        // sort the jump table
        Map.Entry<java.lang.Integer, Tuple<Operand, Label>>[] jumpEntries = jumpTable.entrySet().toArray(new Map.Entry[jumpTable.size()]);
        Arrays.sort(jumpEntries, Comparator.comparingInt(Map.Entry::getKey));
        return jumpEntries;
    }

    private void buildOptimizedSwitch(Map<java.lang.Integer, Tuple<Operand, Label>> jumpTable,
            Map.Entry<java.lang.Integer, Tuple<Operand, Label>>[] jumpEntries, Label elseLabel, Operand value, Class valueClass) {
        Label eqqPath = getNewLabel();

        // build a switch
        int[] jumps = new int[jumpTable.size()];
        Operand[] operands = new Operand[jumpTable.size()];
        Label[] targets = new Label[jumps.length];
        int i = 0;
        for (Map.Entry<java.lang.Integer, Tuple<Operand, Label>> jumpEntry : jumpEntries) {
            jumps[i] = jumpEntry.getKey();
            Tuple<Operand, Label> tuple = jumpEntry.getValue();
            operands[i] = tuple.a;
            targets[i] = tuple.b;
            i++;
        }

        // insert fast switch with fallback to eqq
        addInstr(new BSwitchInstr(jumps, operands, value, eqqPath, targets, elseLabel, valueClass));
        addInstr(new LabelInstr(eqqPath));
    }

    private Variable buildStandardCaseWhen(CaseNode caseNode, Map<Node, Label> nodeBodies, Label endLabel, boolean hasElse, Label elseLabel, Operand value, Variable result) {
        List<Label> labels = new ArrayList<>(4);
        Map<Label, Node> bodies = new HashMap<>(4);

        // build each "when"
        for (Node aCase : caseNode.getCases().children()) {
            WhenNode whenNode = (WhenNode)aCase;
            Label bodyLabel = nodeBodies.get(whenNode);
            if (bodyLabel == null) bodyLabel = getNewLabel();

            Variable eqqResult = temp();
            labels.add(bodyLabel);
            Operand expression = build(whenNode.getExpressionNodes());

            // use frozen string for direct literal strings in `when`
            if (expression instanceof MutableString) {
                expression = ((MutableString) expression).frozenString;
            }

            addInstr(new EQQInstr(scope, eqqResult, expression, value, false, false, scope.maybeUsingRefinements()));
            addInstr(createBranch(eqqResult, tru(), bodyLabel));

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
            addInstr(new CopyInstr(result, nil()));
            addInstr(new JumpInstr(endLabel));
        }

        // Close it out
        addInstr(new LabelInstr(endLabel));

        return result;
    }

    /**
     * Build a new class and add it to the current scope (s).
     */
    public Operand buildClass(ClassNode node) {
        // FIXME: can I just use node.getName?
        return buildClass(node.getCPath().getName().getBytes(), node.getSuperNode(), node.getCPath(),
                node.getBodyNode(), node.getScope(), node.getLine(), node.getEndLine());
    }

    // class Foo; class << self; end; end
    // Here, the class << self declaration is in Foo's body.
    // Foo is the class in whose context this is being defined.
    public Operand buildSClass(SClassNode node) {
        return buildSClass(node.getReceiverNode(), node.getBodyNode(), node.getScope(), node.getLine(), node.getEndLine());
    }

    public Operand buildClassVar(Variable result, ClassVarNode node) {
        return buildClassVar(result, node.getName());
    }

    public Operand buildClassVarAsgn(final ClassVarAsgnNode node) {
        return buildClassVarAsgn(node.getName(), node.getValueNode());
    }

    public Operand buildConstDecl(ConstDeclNode node) {
        return buildConstDeclAssignment(node, () -> build(node.getValueNode()));
    }

    public Operand buildConstDeclAssignment(ConstDeclNode constDeclNode, CodeBlock valueBuilder) {
        Node constNode = constDeclNode.getConstNode();

        if (constNode == null) return putConstant(constDeclNode.getName(), valueBuilder.run());

        return putConstant((Colon3Node) constNode, valueBuilder);
    }

    protected Operand putConstant(Colon3Node colonNode, CodeBlock valueBuilder) {
        if (colonNode.getNodeType() == NodeType.COLON2NODE) {
            Colon2Node colon2Node = (Colon2Node) colonNode;
            return putConstant(build(colon2Node.getLeftNode()), colon2Node.getName(), valueBuilder.run());
        } else { // colon3, assign in Object
            return putConstant(getManager().getObjectClass(), colonNode.getName(), valueBuilder.run());
        }
    }

    @Deprecated(since = "10.0.0.0")
    protected Operand putConstant(Colon3Node colonNode, Operand value) {
        if (colonNode.getNodeType() == NodeType.COLON2NODE) {
            Colon2Node colon2Node = (Colon2Node) colonNode;
            return putConstant(build(colon2Node.getLeftNode()), colon2Node.getName(), value);
        } else { // colon3, assign in Object
            return putConstant(getManager().getObjectClass(), colonNode.getName(), value);
        }
    }

    public Operand buildColon2(Variable result, final Colon2Node colon2) {
        Node lhs = colon2.getLeftNode();

        // Colon2ImplicitNode - (module|class) Foo.  Weird, but it is a wrinkle of AST inheritance.
        if (lhs == null) return searchConst(result, colon2.getName());

        // Colon2ConstNode (Left::name)
        return searchModuleForConst(result, build(lhs), colon2.getName());
    }

    public Operand buildColon3(Variable result, Colon3Node node) {
        return searchModuleForConst(result, getManager().getObjectClass(), node.getName());
    }

    public Operand buildComplex(ComplexNode node) {
        return new Complex((ImmutableLiteral) build(node.getNumber()));
    }

    protected boolean needsDefinitionCheck(Node node) {
        return node.needsDefinitionCheck();
    }

    protected Operand buildGetDefinition(Node node) {
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

            Variable tmpVar = temp();
            for (Node elt: array.children()) {
                Operand result = buildGetDefinition(elt);

                addInstr(createBranch(result, nil(), undefLabel));
            }

            addInstr(new CopyInstr(tmpVar, new FrozenString(DefinedMessage.EXPRESSION.getText())));
            addInstr(new JumpInstr(doneLabel));
            addInstr(new LabelInstr(undefLabel));
            addInstr(new CopyInstr(tmpVar, nil()));
            addInstr(new LabelInstr(doneLabel));

            return tmpVar;
        }
        case BACKREFNODE:
            return addResultInstr(
                    new RuntimeHelperCall(
                            temp(),
                            IS_DEFINED_BACKREF,
                            new Operand[] {new FrozenString(DefinedMessage.GLOBAL_VARIABLE.getText())}
                    )
            );
        case GLOBALVARNODE:
            return buildGlobalVarGetDefinition(((GlobalVarNode) node).getName());
        case NTHREFNODE: {
            return addResultInstr(
                    new RuntimeHelperCall(
                            temp(),
                            IS_DEFINED_NTH_REF,
                            new Operand[] {
                                    getManager().newFixnum(((NthRefNode) node).getMatchNumber()),
                                    new FrozenString(DefinedMessage.GLOBAL_VARIABLE.getText())
                            }
                    )
            );
        }
        case INSTVARNODE:
            return buildInstVarGetDefinition(((InstVarNode) node).getName());
        case CLASSVARNODE:
            return buildClassVarGetDefinition(((ClassVarNode) node).getName());
        case SUPERNODE: {
            Label undefLabel = getNewLabel();
            Variable tmpVar  = addResultInstr(
                    new RuntimeHelperCall(
                            temp(),
                            IS_DEFINED_SUPER,
                            new Operand[] {
                                    buildSelf(),
                                    new FrozenString(DefinedMessage.SUPER.getText())
                            }
                    )
            );
            addInstr(createBranch(tmpVar, nil(), undefLabel));
            Operand superDefnVal = buildGetArgumentDefinition(((SuperNode) node).getArgsNode(), DefinedMessage.SUPER.getText());
            return buildDefnCheckIfThenPaths(undefLabel, superDefnVal);
        }
        case VCALLNODE:
            return addResultInstr(
                    new RuntimeHelperCall(
                            temp(),
                            IS_DEFINED_METHOD,
                            new Operand[] {
                                    buildSelf(),
                                    new FrozenString(((VCallNode) node).getName()),
                                    fals(),
                                    new FrozenString(DefinedMessage.METHOD.getText())
                            }
                    )
            );
        case YIELDNODE:
            return buildDefinitionCheck(new BlockGivenInstr(temp(), getYieldClosureVariable()), DefinedMessage.YIELD.getText());
        case ZSUPERNODE:
            return addResultInstr(
                    new RuntimeHelperCall(
                            temp(),
                            IS_DEFINED_SUPER,
                            new Operand[] {
                                    buildSelf(),
                                    new FrozenString(DefinedMessage.SUPER.getText())
                            }
                    )
            );
        case CONSTNODE:
            return buildConstantGetDefinition(((ConstNode) node).getName());
        case COLON3NODE: case COLON2NODE: {
            // SSS FIXME: Is there a reason to do this all with low-level IR?
            // Can't this all be folded into a Java method that would be part
            // of the runtime library, which then can be used by buildDefinitionCheck method above?
            // This runtime library would be used both by the interpreter & the compiled code!

            final Colon3Node colon = (Colon3Node) node;
            final RubySymbol name = colon.getName();
            final Variable errInfo = temp();

            // store previous exception for restoration if we rescue something
            addInstr(new GetErrorInfoInstr(errInfo));

            CodeBlock protectedCode = new CodeBlock() {
                public Operand run() {
                    if (!(colon instanceof Colon2Node)) { // colon3 (weird inheritance)
                        return addResultInstr(
                                new RuntimeHelperCall(
                                        temp(),
                                        IS_DEFINED_CONSTANT_OR_METHOD,
                                        new Operand[] {
                                                getManager().getObjectClass(),
                                                new FrozenString(name),
                                                new FrozenString(DefinedMessage.CONSTANT.getText()),
                                                new FrozenString(DefinedMessage.METHOD.getText())
                                        }
                                )
                        );
                    }

                    Label bad = getNewLabel();
                    Label done = getNewLabel();
                    Variable result = temp();
                    Operand test = buildGetDefinition(((Colon2Node) colon).getLeftNode());
                    addInstr(createBranch(test, nil(), bad));
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
                    addInstr(new CopyInstr(result, nil()));
                    addInstr(new LabelInstr(done));

                    return result;
                }
            };

            // rescue block
            CodeBlock rescueBlock = new CodeBlock() {
                 public Operand run() {
                 // Nothing to do -- ignore the exception, and restore stashed error info!
                 addInstr(new RestoreErrorInfoInstr(errInfo));
                 return nil();
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
                            temp(),
                            IS_DEFINED_METHOD,
                            new Operand[]{
                                    buildSelf(),
                                    new Symbol(((FCallNode) node).getName()),
                                    fals(),
                                    new FrozenString(DefinedMessage.METHOD.getText())
                            }
                    )
            );
            addInstr(createBranch(tmpVar, nil(), undefLabel));
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
                    addInstr(createBranch(receiverDefn, nil(), undefLabel));
                    Variable tmpVar = temp();
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
                public Operand run() { return nil(); } // Nothing to do if we got an exception
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
                    addInstr(createBranch(receiverDefn, nil(), undefLabel));
                    /* --------------------------------------------------------------------------
                     * This basically combines checks from CALLNODE and FCALLNODE
                     *
                     * Generate IR for this sequence
                     *
                     *    1. r  = receiver
                     *    2. mc = r.metaClass
                     *    3. v  = mc.getVisibility(methodName)
                     *    4. f  = !v || v.isPrivate? || (v.isProtected? &amp;&amp; receiver/self?.kindof(mc.getRealClass))
                     *    5. return !f &amp;&amp; mc.methodBound(attrmethod) ? buildGetArgumentDefn(..) : false
                     *
                     * Hide the complexity of instrs 2-4 into a verifyMethodIsPublicAccessible call
                     * which can executely entirely in Java-land.  No reason to expose the guts in IR.
                     * ------------------------------------------------------------------------------ */
                    Operand  receiver   = build(attrAssign.getReceiverNode());
                    Variable tmpVar = addResultInstr(
                            new RuntimeHelperCall(
                                    temp(),
                                    IS_DEFINED_METHOD,
                                    new Operand[] {
                                            receiver,
                                            new Symbol(attrAssign.getName()),
                                            tru(),
                                            new FrozenString(DefinedMessage.METHOD.getText())
                                    }
                            )
                    );
                    addInstr(createBranch(tmpVar, nil(), undefLabel));
                    return buildDefnCheckIfThenPaths(undefLabel, tmpVar);
                }
            };

            // rescue block: Nothing to do if we got an exception
            CodeBlock rescueBlock = () -> nil();

            // Try verifying definition, and if we get an JumpException exception, process it with the rescue block above
            return protectCodeWithRescue(protectedCode, rescueBlock);
        }
        case OPASGNCONSTDECLNODE:
            return new FrozenString("assignment");

        case SPLATNODE: {
            SplatNode splat = (SplatNode) node;
            Label undefLabel = getNewLabel();
            Label doneLabel = getNewLabel();

            Variable tmpVar = temp();
            Operand result = buildGetDefinition(splat.getValue());

            addInstr(createBranch(result, nil(), undefLabel));

            addInstr(new CopyInstr(tmpVar, new FrozenString(DefinedMessage.EXPRESSION.getText())));
            addInstr(new JumpInstr(doneLabel));
            addInstr(new LabelInstr(undefLabel));
            addInstr(new CopyInstr(tmpVar, nil()));
            addInstr(new LabelInstr(doneLabel));

            return tmpVar;
        }

        case HASHNODE: { // If all elts of hash are defined the array is as well
            HashNode hash = (HashNode) node;
            Label undefLabel = getNewLabel();
            Label doneLabel = getNewLabel();

            Variable tmpVar = temp();
            for (Node elt: hash.childNodes()) {
                Operand result = buildGetDefinition(elt);

                addInstr(createBranch(result, nil(), undefLabel));
            }

            addInstr(new CopyInstr(tmpVar, new FrozenString(DefinedMessage.EXPRESSION.getText())));
            addInstr(new JumpInstr(doneLabel));
            addInstr(new LabelInstr(undefLabel));
            addInstr(new CopyInstr(tmpVar, nil()));
            addInstr(new LabelInstr(doneLabel));

            return tmpVar;
        }

        default:
            return new FrozenString("expression");
        }
    }

    public Operand buildGetArgumentDefinition(final Node node, String type) {
        if (node == null) return new MutableString(type);

        Operand rv = new FrozenString(type);
        boolean failPathReqd = false;
        Label failLabel = getNewLabel();
        if (node instanceof ArrayNode) {
            for (int i = 0; i < ((ArrayNode) node).size(); i++) {
                Operand def = buildGetDefinition(((ArrayNode) node).get(i));
                if (def == nil()) { // Optimization!
                    rv = nil();
                    break;
                } else if (!def.hasKnownValue()) { // Optimization!
                    failPathReqd = true;
                    addInstr(createBranch(def, nil(), failLabel));
                }
            }
        } else {
            Operand def = buildGetDefinition(node);
            if (def == nil()) { // Optimization!
                rv = nil();
            } else if (!def.hasKnownValue()) { // Optimization!
                failPathReqd = true;
                addInstr(createBranch(def, nil(), failLabel));
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

    protected boolean canBeLazyMethod(DefNode node) {
        return !((MethodDefNode) node).containsBreakNext();
    }

    @Override
    public void receiveMethodArgs(DefNode defNode) {
        receiveMethodArgs(defNode.getArgsNode());
    }

    public Operand buildDefn(MethodDefNode node) { // Instance method
        LazyMethodDefinition def = new LazyMethodDefinitionAST(node);
        return buildDefn(defineNewMethod(def, node.getName().getBytes(), node.getLine(), node.getScope(), true));
    }

    public Operand buildDefs(DefsNode node) { // Class method
        LazyMethodDefinition def = new LazyMethodDefinitionAST(node);
        return buildDefs(node.getReceiverNode(), defineNewMethod(def, node.getName().getBytes(), node.getLine(), node.getScope(), false));
    }

    protected LocalVariable getArgVariable(RubySymbol name, int depth) {
        // For non-loops, this name will override any name that exists in outer scopes
        return scope instanceof IRFor ? getLocalVariable(name, depth) : getNewLocalVariable(name, 0);
    }

    private void addArgReceiveInstr(Variable v, Variable keywords, int argIndex, Signature signature) {
        boolean post = signature != null;

        if (post) {
            addInstr(new ReceivePostReqdArgInstr(v, keywords, argIndex, signature.pre(), signature.opt(), signature.hasRest(), signature.post()));
        } else {
            addInstr(new ReceivePreReqdArgInstr(v, keywords, argIndex));
        }
    }

    public void receiveRequiredArg(Node node, Variable keywords, int argIndex, Signature signature) {
        switch (node.getNodeType()) {
            case ARGUMENTNODE: {
                RubySymbol argName = ((ArgumentNode)node).getName();

                if (scope instanceof IRMethod) addArgumentDescription(ArgumentType.req, argName);

                addArgReceiveInstr(argumentResult(argName), keywords, argIndex, signature);
                break;
            }
            case MULTIPLEASGNNODE: {
                MultipleAsgnNode childNode = (MultipleAsgnNode) node;
                Variable v = temp();
                addArgReceiveInstr(v, keywords, argIndex, signature);
                if (scope instanceof IRMethod) addArgumentDescription(ArgumentType.anonreq, null);
                Variable tmp = addResultInstr(new ToAryInstr(temp(), v));
                buildMultipleAssignmentArgs(childNode, tmp);
                break;
            }
            default: throw notCompilable("Can't build assignment node", node);
        }
    }

    protected void receiveNonBlockArgs(final ArgsNode argsNode, Variable keywords) {
        Signature signature = scope.getStaticScope().getSignature();

        // For closures, we don't need the check arity call
        if (scope instanceof IRMethod) {
            // Expensive to do this explicitly?  But, two advantages:
            // (a) on inlining, we'll be able to get rid of these checks in almost every case.
            // (b) compiler to bytecode will anyway generate this and this is explicit.
            // For now, we are going explicit instruction route.
            // But later, perhaps can make this implicit in the method setup preamble?

            addInstr(new CheckArityInstr(signature.required(), signature.opt(), signature.hasRest(),
                    signature.keyRest(), keywords));
        } else if (scope instanceof IRClosure && argsNode.hasKwargs()) {
            // FIXME: This is added to check for kwargs correctness but bypass regular correctness.
            // Any other arity checking currently happens within Java code somewhere (RubyProc.call?)
            addInstr(new CheckArityInstr(signature.required(), signature.opt(), signature.hasRest(),
                    signature.keyRest(), keywords));
        }

        // Other args begin at index 0
        int argIndex = 0;

        // Pre(-opt and rest) required args
        Node[] args = argsNode.getArgs();
        int preCount = signature.pre();
        for (int i = 0; i < preCount; i++, argIndex++) {
            receiveRequiredArg(args[i], keywords, argIndex, null);
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
                addInstr(new ReceiveOptArgInstr(argVar, keywords, j, signature.required(), signature.pre()));
                addInstr(BNEInstr.create(variableAssigned, argVar, UndefinedValue.UNDEFINED));
                // We add this extra nil copy because we do not know if we have a circular defininition of
                // argVar: proc { |a=a| } or proc { |a = foo(bar(a))| }.
                addInstr(new CopyInstr(argVar, nil()));
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
                addArgumentDescription(restArgNode.isAnonymous() && restArgNode.getName() == null ?
                        ArgumentType.anonrest : ArgumentType.rest, restArgNode.getName());
            }

            RubySymbol argName =  restArgNode.isAnonymous() ?
                    scope.getManager().getRuntime().newSymbol(CommonByteLists.STAR) : restArgNode.getName();

            // You need at least required+opt+1 incoming args for the rest arg to get any args at all
            // If it is going to get something, then it should ignore required+opt args from the beginning
            // because they have been accounted for already.
            addInstr(new ReceiveRestArgInstr(argumentResult(argName), keywords, argIndex, signature.required() + opt));
        }

        // Post(-opt and rest) required args
        int postCount = argsNode.getPostCount();
        int postIndex = argsNode.getPostIndex();
        for (int i = 0; i < postCount; i++) {
            receiveRequiredArg(args[postIndex + i], keywords, i, signature);
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
            Variable tmp = temp();
            addInstr(new LoadImplicitClosureInstr(tmp));
            addInstr(new ReifyClosureInstr(blockVar, tmp));
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
        Signature signature = scope.getStaticScope().getSignature();

        Variable keywords = addResultInstr(new ReceiveKeywordsInstr(temp(), signature.hasRest(), argsNode.hasKwargs()));

        KeywordRestArgNode keyRest = argsNode.getKeyRest();
        RubySymbol restName = keyRest == null ? null : keyRest.getName();
        // We want this to come in before arity check since arity will think no kwargs should exist.
        if (restName != null && "nil".equals(restName.idString())) {
            if_not(keywords, UndefinedValue.UNDEFINED, () -> addRaiseError("ArgumentError", "no keywords accepted"));
        }

        // 1.9 pre, opt, rest, post args
        receiveNonBlockArgs(argsNode, keywords);

        // 2.0 keyword args
        Node[] args = argsNode.getArgs();
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
                addInstr(new ReceiveKeywordArgInstr(av, keywords, key));
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
        if (keyRest != null) {
            ArgumentType type;
            // FIXME: combined processing of argumentType could be removed and use same helper that blocks use.

            // anonymous keyrest
            if (restName == null || restName.getBytes().realSize() == 0) {
                type = ArgumentType.anonkeyrest;
            } else if (restName.getBytes().equals(CommonByteLists.NIL)) {
                type = ArgumentType.nokey;
            } else {
                type = ArgumentType.keyrest;
            }

            Variable av = getNewLocalVariable(restName, 0);
            if (scope instanceof IRMethod) addArgumentDescription(type, restName);

            addInstr(new ReceiveKeywordRestArgInstr(av, keywords));
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
        switch (node.getNodeType()) {
            case DASGNNODE: {
                DAsgnNode dynamicAsgn = (DAsgnNode) node;
                Variable v = getArgVariable(dynamicAsgn.getName(), dynamicAsgn.getDepth());
                buildSplatForMultiAssign(v, argsArray, preArgsCount, postArgsCount, index, isSplat);
                break;
            }
            case LOCALASGNNODE: {
                LocalAsgnNode localVariable = (LocalAsgnNode) node;
                Variable v = getArgVariable(localVariable.getName(), localVariable.getDepth());
                buildSplatForMultiAssign(v, argsArray, preArgsCount, postArgsCount, index, isSplat);
                break;
            }
            case MULTIPLEASGNNODE: {
                MultipleAsgnNode childNode = (MultipleAsgnNode) node;
                if (!isMasgnRoot) {
                    Variable v = buildSplatForMultiAssign(temp(), argsArray, preArgsCount, postArgsCount, index, isSplat);
                    argsArray = addResultInstr(new ToAryInstr(temp(), v));
                }
                buildMultipleAssignmentArgs(childNode, argsArray);
                break;
            }
            default:
                throw notCompilable("Shouldn't get here", node);
        }
    }

    private Variable buildSplatForMultiAssign(Variable result, Operand argsArray, int preArgsCount, int postArgsCount,
                                              int index, boolean isSplat) {
        return isSplat ?
                addResultInstr(new RestArgMultipleAsgnInstr(result, argsArray, index, preArgsCount, postArgsCount)) :
                addResultInstr(new ReqdArgMultipleAsgnInstr(result, argsArray, index, preArgsCount, postArgsCount));
    }

    // Multiple assignment in an ordinary expression (e.g a,b,*c=v).
    public void buildMultipleAssignment(final MultipleAsgnNode multipleAsgnNode, Operand values) {
        final ListNode masgnPre = multipleAsgnNode.getPre();
        final List<Tuple<Node, Variable>> assigns = new ArrayList<>(4);

        int i = 0;
        if (masgnPre != null) {
            for (Node an: masgnPre.children()) {
                assigns.add(new Tuple<>(an, addResultInstr(new ReqdArgMultipleAsgnInstr(temp(), values, i))));
                i++;
            }
        }

        Node restNode = multipleAsgnNode.getRest();
        int postCount = multipleAsgnNode.getPostCount();
        if (restNode != null && !(restNode instanceof StarNode)) {
            assigns.add(new Tuple<>(restNode, addResultInstr(new RestArgMultipleAsgnInstr(temp(), values, 0, i, postCount))));
        }

        final ListNode masgnPost = multipleAsgnNode.getPost();
        if (masgnPost != null) {
            int j = 0;
            for (Node an: masgnPost.children()) {
                assigns.add(new Tuple<>(an, addResultInstr(new ReqdArgMultipleAsgnInstr(temp(), values, j, i, postCount))));
                j++;
            }
        }

        for (Tuple<Node, Variable> assign: assigns) {
            buildAssignment(assign.a, assign.b);
        }
    }

    // Multiple assignment in arguments (e.g. { |a,b,*c| ..})
    public void buildMultipleAssignmentArgs(final MultipleAsgnNode multipleAsgnNode, Operand argsArray) {
        final List<Tuple<Node, Variable>> assigns = new ArrayList<>(4);
        int i = 0;
        ListNode masgnPre = multipleAsgnNode.getPre();
        if (masgnPre != null) {
            for (Node an: masgnPre.children()) {
                buildArgsMasgn(an, argsArray, false, -1, -1, i++, false);
            }
        }

        Node restNode = multipleAsgnNode.getRest();
        int postArgsCount = multipleAsgnNode.getPostCount();
        if (restNode != null && !(restNode instanceof StarNode)) {
            buildArgsMasgn(restNode, argsArray, false, i, postArgsCount, 0, true); // rest of the argument array!
        }

        ListNode masgnPost = multipleAsgnNode.getPost();
        if (masgnPost != null) {
            int j = 0;
            for (Node an: masgnPost.children()) {
                buildArgsMasgn(an, argsArray, false, i, postArgsCount, j, false);
                j++;
            }
        }

        for (Tuple<Node, Variable> assign: assigns) {
            buildAssignment(assign.a, assign.b);
        }
    }

    public void receiveMethodArgs(final ArgsNode argsNode) {
        receiveArgs(argsNode);
    }

    protected void receiveForArgs(Node node) {
        receiveBlockArgs(node);
    }

    protected void receiveBlockArgs(Node args) {
        if (args instanceof ArgsNode) { // regular blocks
            ((IRClosure) scope).setArgumentDescriptors(Helpers.argsNodeToArgumentDescriptors(((ArgsNode) args)));
            receiveArgs((ArgsNode)args);
        } else  {
            // for loops -- reuse code in IRBuilder:buildBlockArgsAssignment
            buildBlockArgsAssignment(args, null, 0, false);
        }
    }

    public Operand buildDot(final DotNode node) {
        return buildRange(node.getBeginNode(), node.getEndNode(), node.isExclusive());
    }

    protected int dynamicPiece(Operand[] pieces, int i, Node pieceNode, Encoding _unused) {
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
                    Variable result = temp();
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

        pieces[i] = piece == null ? nil() : piece;

        return estimatedSize;
    }

    public Operand buildDRegexp(Variable result, DRegexpNode node) {
        return buildDRegex(result, node.children(), node.getOptions());
    }

    public Operand buildDStr(Variable result, DStrNode node) {
        return buildDStr(result, node.children(), node.getEncoding(), node.getStringStyle(), node.getLine());
    }

    public Operand buildDSymbol(Variable result, DSymbolNode node) {
        return buildDSymbol(result, node.children(), node.getEncoding(), node.getLine());
    }

    public Operand buildDVar(DVarNode node) {
        return getLocalVariable(node.getName(), node.getDepth());
    }

    public Operand buildDXStr(Variable result, DXStrNode node) {
        return buildDXStr(result, node.children(), node.getEncoding(), node.getLine());
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
        return buildEnsureInternal(ensureNode.getBodyNode(), null, null, null, null, false, ensureNode.getEnsureNode(),
                false, null);
    }

    public Operand buildFCall(Variable result, FCallNode node) {
        if (result == null) result = temp();
        RubySymbol name = methodName = node.getName();

        // special case methods with frame handling
        String callName = name.idString();
        switch (callName) {
            case "block_given?":
            case "iterator?":
                if (node.getArgsNode() == null
                        && node.getIterNode() == null) {
                    addInstr(new BlockGivenCallInstr(result, getYieldClosureVariable(), callName));
                    return result;
                }
        }

        return createCall(result, buildSelf(), FUNCTIONAL, name, node.getArgsNode(), node.getIterNode(),
                node.getLine(), node.isNewline());
    }

    protected Operand setupCallClosure(Node _unused, Node node) {
        if (node == null) return NullBlock.INSTANCE;

        switch (node.getNodeType()) {
            case ITERNODE:
                return build(node);
            case BLOCKPASSNODE:
                Node bodyNode = ((BlockPassNode)node).getBodyNode();
                if (bodyNode instanceof SymbolNode && !scope.maybeUsingRefinements()) {
                    return new SymbolProc(((SymbolNode) bodyNode).getName());
                } else if (bodyNode instanceof ArgumentNode && ((ArgumentNode) bodyNode).getName().idString().equals("&")) {
                    return getYieldClosureVariable();
                }
                return build(bodyNode);
            default:
                throw notCompilable("ERROR: Encountered a method with a non-block, non-blockpass iter node", node);
        }
    }

    public Operand buildFixnum(FixnumNode node) {
        return fix(node.getValue());
    }

    public Operand buildFlip(FlipNode node) {
        return buildFlip(node.getBeginNode(), node.getEndNode(), node.isExclusive());
    }

    public Operand buildFloat(FloatNode node) {
        return new Float(node.getValue());
    }

    public Operand buildFor(ForNode node) {
        return buildFor(node.getIterNode(), node.getVarNode(), node.getBodyNode(), node.getScope(),
                Signature.from(node), node.getLine(), node.getEndLine());
    }

    public Operand buildGlobalAsgn(GlobalAsgnNode node) {
        return buildGlobalAsgn(node.getName(), node.getValueNode());
    }

    Operand buildGlobalVar(Variable result, GlobalVarNode node) {
        return buildGlobalVar(result, node.getName());
    }

    public Operand buildHash(HashNode hashNode) {
        List<KeyValuePair<Operand, Operand>> args = new ArrayList<>(1);
        boolean hasAssignments = hashNode.containsVariableAssignment();
        Variable hash = null;
        // Duplication checks happen when **{} are literals and not **h variable references.
        Operand duplicateCheck = fals();

        for (KeyValuePair<Node, Node> pair: hashNode.getPairs()) {
            Node key = pair.getKey();
            Operand keyOperand;

            if (key == null) {                          // Splat kwarg [e.g. {**splat1, a: 1, **splat2)]
                Node value = pair.getValue();
                if (value instanceof NilNode) continue; // **nil contribute nothing to a heterogeneous hash of elements
                 duplicateCheck = value instanceof HashNode && ((HashNode) value).isLiteral() ? tru() : fals();
                if (hash == null) {                     // No hash yet. Define so order is preserved.
                    hash = copy(new Hash(args));
                    args = new ArrayList<>(1);           // Used args but we may find more after the splat so we reset
                } else if (!args.isEmpty()) {
                    addInstr(new RuntimeHelperCall(hash, MERGE_KWARGS, new Operand[] { hash, new Hash(args), duplicateCheck}));
                    args = new ArrayList<>(1);
                }
                Operand splat = buildWithOrder(value, hasAssignments);
                addInstr(new RuntimeHelperCall(hash, MERGE_KWARGS, new Operand[] { hash, splat, duplicateCheck}));
                continue;
            } else {
                keyOperand = buildWithOrder(key, hasAssignments);
            }

            args.add(new KeyValuePair<>(keyOperand, buildWithOrder(pair.getValue(), hasAssignments)));
        }

        if (hash == null) {           // non-**arg ordinary hash
            hash = copy(new Hash(args));
        } else if (!args.isEmpty()) { // ordinary hash values encountered after a **arg
            addInstr(new RuntimeHelperCall(hash, MERGE_KWARGS, new Operand[] { hash, new Hash(args), duplicateCheck}));
        }

        return hash;
    }

    public Operand buildIf(Variable result, final IfNode ifNode) {
        return buildConditional(result, ifNode.getCondition(), ifNode.getThenBody(), ifNode.getElseBody());
    }

    public Operand buildInstAsgn(final InstAsgnNode node) {
        return buildInstAsgn(node.getName(), node.getValueNode());
    }

    public Operand buildInstVar(InstVarNode node) {
        return buildInstVar(node.getName());
    }

    public Operand buildIter(final IterNode iter) {
        return buildIter(iter.getVarNode(), iter.getBodyNode(), iter.getScope(), Signature.from(iter), iter.getLine(), iter.getEndLine());
    }

    public Operand buildLiteral(LiteralNode literalNode) {
        return new MutableString(literalNode.getSymbolName());
    }

    public Operand buildLocalAsgn(LocalAsgnNode localAsgnNode) {
        Variable variable = getLocalVariable(localAsgnNode.getName(), localAsgnNode.getDepth());
        Operand value = build(variable, localAsgnNode.getValueNode());

        // no use copying a variable to itself
        if (variable != value) copy(variable, value);

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
        return buildMatch(result, build(matchNode.getRegexpNode()));
    }

    public Operand buildMatch2(Variable result, Match2Node matchNode) {
        Operand receiver = build(matchNode.getReceiverNode());
        Operand value    = build(matchNode.getValueNode());

        if (result == null) result = temp();

        addInstr(new MatchInstr(scope, result, receiver, value));

        if (matchNode instanceof Match2CaptureNode) {
            Match2CaptureNode m2c = (Match2CaptureNode)matchNode;
            for (int slot:  m2c.getScopeOffsets()) {
                // Static scope scope offsets store both depth and offset
                int depth = slot >> 16;
                int offset = slot & 0xffff;

                // For now, we'll continue to implicitly reference "$~"
                RubySymbol var = getManager().runtime.newSymbol(getVarNameFromScopeTree(scope, depth, offset));
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

        if (result == null) result = temp();

        return addResultInstr(new MatchInstr(scope, result, receiver, value));
    }

    protected Operand getContainerFromCPath(Node cpath) {
        Operand container;

        if (cpath instanceof Colon2Node) {
            Node leftNode = ((Colon2Node) cpath).getLeftNode();

            if (leftNode != null) { // Foo::Bar
                container = build(leftNode);
            } else { // Only name with no left-side Bar <- Note no :: on left
                container = findContainerModule();
            }
        } else { //::Bar
            container = getManager().getObjectClass();
        }

        return container;
    }

    Operand buildModule(ModuleNode node) {
        return buildModule(node.getCPath().getName().getBytes(), node.getCPath(), node.getBodyNode(),
                node.getScope(), node.getLine(), node.getEndLine());
    }

    public Operand buildNext(final NextNode nextNode) {
        return buildNext(build(nextNode.getValueNode()), nextNode.getLine());
    }

    public Operand buildNthRef(NthRefNode node) {
        return buildNthRef(node.getMatchNumber());
    }

    public Operand buildNil() {
        return nil();
    }

    public Operand buildOpAsgn(OpAsgnNode node) {
        return buildOpAsgn(node.getReceiverNode(), node.getValueNode(), node.getVariableSymbolName(),
                node.getVariableSymbolNameAsgn(), node.getOperatorSymbolName(), node.isLazy());
    }

    protected Operand buildColon2ForConstAsgnDeclNode(Node lhs, Variable valueResult, boolean constMissing) {
        RubySymbol name = ((INameNode) lhs).getName();
        Variable leftModule = copy(lhs instanceof Colon2Node ?
                build(((Colon2Node) lhs).getLeftNode()) : getManager().getObjectClass());
        addInstr(new SearchModuleForConstInstr(valueResult, leftModule, name, false, constMissing));

        return leftModule;
    }

    public Operand buildOpAsgnConstDeclNode(OpAsgnConstDeclNode node) {
        if (node.isOr()) {
            return buildOpAsgnConstDeclOr(node.getFirstNode(), node.getSecondNode(), ((Colon3Node) node.getFirstNode()).getName());
        } else if (node.isAnd()) {
            return buildOpAsgnConstDeclAnd(node.getFirstNode(), node.getSecondNode(), ((Colon3Node) node.getFirstNode()).getName());
        }

        return buildOpAsgnConstDecl((Colon3Node) node.getFirstNode(), ((Colon3Node) node.getFirstNode()).getName(), node.getSecondNode(), node.getSymbolOperator());
    }

    public Operand buildOpAsgnAnd(OpAsgnAndNode node) {
        return buildOpAsgnAnd(() -> build(node.getFirstNode()), () -> build(node.getSecondNode()));
    }

    public Operand buildOpAsgnOr(final OpAsgnOrNode orNode) {
        if (!orNode.getFirstNode().needsDefinitionCheck()) {
            return buildOpAsgnOr(() -> build(orNode.getFirstNode()), () -> build(orNode.getSecondNode()));
        }

        return buildOpAsgnOrWithDefined(orNode.getFirstNode(), orNode.getSecondNode());
    }

    public Operand buildOpElementAsgn(OpElementAsgnNode node) {
        // Translate "a[x] ||= n" --> "a[x] = n if !is_true(a[x])"
        if (node.isOr()) return buildOpElementAsgnWith(node.getReceiverNode(), node.getArgsNode(), node.getBlockNode(), node.getValueNode(), tru());

        // Translate "a[x] &&= n" --> "a[x] = n if is_true(a[x])"
        if (node.isAnd()) return buildOpElementAsgnWith(node.getReceiverNode(), node.getArgsNode(), node.getBlockNode(), node.getValueNode(), fals());

        // a[i] *= n, etc.  anything that is not "a[i] &&= .. or a[i] ||= .."
        return buildOpElementAsgnWithMethod(node.getReceiverNode(), node.getArgsNode(), node.getBlockNode(), node.getValueNode(), node.getOperatorSymbolName());
    }

    public Operand buildOr(OrNode node) {
        return buildOr(build(node.getFirstNode()), () -> build(node.getSecondNode()), binaryType(node.getFirstNode()));
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

    public Operand buildPostExe(PostExeNode postExeNode) {
        IRScope topLevel = scope.getRootLexicalScope();
        IRScope nearestLVarScope = scope.getNearestTopLocalVariableScope();

        StaticScope parentScope = nearestLVarScope.getStaticScope();
        StaticScope staticScope = parentScope.duplicate();
        staticScope.setEnclosingScope(parentScope);
        IRClosure endClosure = new IRClosure(getManager(), scope, postExeNode.getLine(), staticScope,
                Signature.from(postExeNode), CommonByteLists._END_, true);
        staticScope.setIRScope(endClosure);
        endClosure.setIsEND();
        // Create a new nested builder to ensure this gets its own IR builder state like the ensure block stack
        newIRBuilder(getManager(), endClosure).buildPrePostExeInner(postExeNode.getBodyNode());

        // Add an instruction in 's' to record the end block in the 'topLevel' scope.
        // SSS FIXME: IR support for end-blocks that access vars in non-toplevel-scopes
        // might be broken currently. We could either fix it or consider dropping support
        // for END blocks altogether or only support them in the toplevel. Not worth the pain.
        addInstr(new RecordEndBlockInstr(topLevel, new WrappedIRClosure(buildSelf(), endClosure)));
        return nil();
    }

    public Operand buildPreExe(PreExeNode preExeNode) {
        return super.buildPreExe(preExeNode.getBodyNode());
    }

    public Operand buildRational(RationalNode node) {
        return buildRational(node.getNumerator(), node.getDenominator());
    }

    public Operand buildRedo(RedoNode redoNode) {
        return buildRedo(redoNode.getLine());
    }

    public Operand buildRegexp(RegexpNode reNode) {
        // SSS FIXME: Rather than throw syntax error at runtime, we should detect
        // regexp syntax errors at build time and add an exception-throwing instruction instead
        return copy(new Regexp(reNode.getValue(), reNode.getOptions()));
    }

    public Operand buildRescue(RescueNode node) {
        RescueBodyNode clause = node.getRescueNode();
        return buildEnsureInternal(node.getBodyNode(), node.getElseNode(), exceptionNodesFor(clause), bodyFor(clause),
                optRescueFor(clause), node instanceof RescueModNode, null, true, null);
    }

    // FIXME: This MIGHT be able to expand to more complicated expressions like Hash or Array if they
    // contain only SideEffectFree nodes.  Constructing a literal out of these should be safe from
    // effecting or being able to access $!.
    protected boolean isSideEffectFree(final Node node) {
        return node instanceof SideEffectFree;
    }

    protected boolean isErrorInfoGlobal(final Node body) {
        if (!(body instanceof GlobalVarNode)) return false;

        String id = ((GlobalVarNode) body).getName().idString();

        // Global names and aliases that reference the exception in flight
        switch (id) {
            case "$!":
            case "$ERROR_INFO":
            case "$@":
            case "$ERROR_POSITION":
                return true;
            default:
                return false;
        }
    }

    // In order to line up with Prism we will spend some cost making Node[] in legacy parser.
    private Node[] asList(Node node) {
        if (node == null) return null;
        if (node instanceof ListNode) return ((ListNode) node).children();
        return new Node[] { node };
    }

    protected Node[] exceptionNodesFor(RescueBodyNode node) {
        return asList(node.getExceptionNodes());
    }

    protected Node bodyFor(RescueBodyNode node) {
        return node.getBodyNode();
    }

    protected RescueBodyNode optRescueFor(RescueBodyNode node) {
        return node.getOptRescueNode();
    }

    @Override
    protected Node referenceFor(RescueBodyNode node) {
        return null;
    }

    public Operand buildRetry(RetryNode node) {
        return buildRetry(node.getLine());
    }

    public Operand buildReturn(ReturnNode returnNode) {
        Node valueNode = returnNode.getValueNode();
        if (isTopLevel() && valueNode != null && !(valueNode instanceof NilImplicitNode)) {
            scope.getManager().getRuntime().getWarnings().warn(getFileName(), valueNode.getLine() + 1, "argument of top-level return is ignored");
        }

        return buildReturn(build(valueNode), returnNode.getLine());
    }

    public Operand buildSplat(Variable result, SplatNode splatNode) {
        if (result == null) result = temp();
        return addResultInstr(new BuildSplatInstr(result, build(splatNode.getValue()), true));
    }

    public Operand buildStr(StrNode strNode) {
        Operand literal = buildStrRaw(strNode);

        return literal instanceof FrozenString ? literal : copy(literal);
    }

    public Operand buildStrRaw(StrNode strNode) {
        if (strNode instanceof FileNode) return new Filename();

        int line = strNode.getLine();

        switch(strNode.getStringStyle()) {
            case Frozen -> { return new FrozenString(strNode.getValue(), strNode.getCodeRange(), scope.getFile(), line); }
            case Mutable -> { return new MutableString(strNode.getValue(), strNode.getCodeRange(), scope.getFile(), line); }
            default -> { return new ChilledString(strNode.getValue(), strNode.getCodeRange(), scope.getFile(), line); }
        }
    }

    public Operand buildSuper(Variable result, SuperNode node) {
        return buildSuper(result, node.getIterNode(), node.getArgsNode(), node.getLine(), node.isNewline());
    }

    public Operand buildSValue(Variable result, SValueNode node) {
        return copy(result, new SValue(build(node.getValue())));
    }

    public Operand buildSymbol(SymbolNode node) {
        // Since symbols are interned objects, no need to copyAndReturnValue(...)
        return new Symbol(node.getName());
    }

    public Operand buildUndef(UndefNode node) {
        return buildUndef(build(node.getName()));
    }

    public Operand buildUntil(UntilNode node) {
        return buildConditionalLoop(node.getConditionNode(), node.getBodyNode(), false, node.evaluateAtStart());
    }

    public Operand buildVAlias(VAliasNode valiasNode) {
        return buildVAlias(valiasNode.getNewName(), valiasNode.getOldName());
    }

    public Operand buildVCall(Variable result, VCallNode node) {
        if (result == null) result = temp();

        RubySymbol name = methodName = node.getName();

        // special case methods with frame handling
        String callName = name.idString();
        switch (callName) {
            case "__method__":
            case "__callee__":
                // narrow to methods until we can fix other scopes' frame names
                if (scope instanceof IRMethod) {
                    addInstr(new FrameNameCallInstr(result, callName));
                    return result;
                }
        }

        return _call(result, VARIABLE, buildSelf(), node.getName());
    }

    public Operand buildWhile(WhileNode node) {
        return buildConditionalLoop(node.getConditionNode(), node.getBodyNode(), true, node.evaluateAtStart());
    }

    public Operand buildXStr(Variable result, XStrNode node) {
        return fcall(result, buildSelf(), "`", new FrozenString(node.getValue(), node.getCodeRange(), scope.getFile(), node.getLine()));
    }

    public Operand buildYield(Variable result, YieldNode node) {
        IRScope hardScope = scope.getNearestNonClosurelikeScope();
        if (hardScope instanceof IRScriptBody || hardScope instanceof IRModuleBody) throwSyntaxError(node.getLine(), "Invalid yield");

        if (result == null) result = temp();

        boolean unwrap = true;
        Node argNode = node.getArgsNode();
        // Get rid of one level of array wrapping
        if (argNode != null && (argNode instanceof ArrayNode) && ((ArrayNode)argNode).size() == 1) {
            Node onlyArg = ((ArrayNode)argNode).getLast();

            // We should not unwrap if it is a keyword argument.
            if (!(onlyArg instanceof HashNode) || ((HashNode) onlyArg).isLiteral()) {
                argNode = onlyArg;
                unwrap = false;
            }
        }

        int[] flags = new int[] { 0 };
        Operand value = buildYieldArgs(argNode, flags);

        addInstr(new YieldInstr(result, getYieldClosureVariable(), value, flags[0], unwrap));

        return result;
    }

    public Operand buildZArray(Variable result) {
       return copy(result, new Array());
    }

    public Operand buildZSuper(Variable result, ZSuperNode node) {
        return buildZSuper(result, node.getIterNode());
    }

    /*
     * Give the ability to print a debug message to stdout.  Not to ever be used outside
     * of debugging an issue with IR.
     */
    private void debug(String message, Operand... operands) {
        addInstr(new DebugOutputInstr(message, operands));
    }

    @Override
    protected boolean alwaysFalse(Node node) {
        return node.getNodeType().alwaysFalse();
    }

    @Override
    protected  boolean alwaysTrue(Node node) {
        return node.getNodeType().alwaysTrue();
    }

    public LocalVariable getLocalVariable(RubySymbol name, int scopeDepth) {
        return scope.getLocalVariable(name, scopeDepth);
    }

    protected void createPrefixFromArgs(ByteList prefix, Node args) {
        if (args instanceof ArgsNode) {
            ArgsNode argsNode = (ArgsNode) args;
            prefix.append(Stream.of(argsNode.getArgs()).
                    filter(n -> n instanceof INameNode).
                    map(n -> {
                        RubySymbol name = ((INameNode) n).getName();
                        return name == null ? "(null)" : name.idString();
                    }).
                    collect(Collectors.joining(",")).getBytes());
        } else { // for loops

        }
    }
}