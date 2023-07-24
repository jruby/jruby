package org.jruby.ir.builder;

import org.jcodings.specific.UTF8Encoding;
import org.jruby.ParseResult;
import org.jruby.Ruby;
import org.jruby.RubyBignum;
import org.jruby.RubyComplex;
import org.jruby.RubyFixnum;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyRational;
import org.jruby.RubySymbol;
import org.jruby.ast.*;
import org.jruby.ast.types.ILiteralNode;
import org.jruby.ast.types.INameNode;
import org.jruby.common.IRubyWarnings;
import org.jruby.compiler.NotCompilableException;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IREvalScript;
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
import org.jruby.ir.operands.Boolean;
import org.jruby.ir.operands.Complex;
import org.jruby.ir.operands.CurrentScope;
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
import org.jruby.ir.operands.Rational;
import org.jruby.ir.operands.Regexp;
import org.jruby.ir.operands.SValue;
import org.jruby.ir.operands.Self;
import org.jruby.ir.operands.Splat;
import org.jruby.ir.operands.Symbol;
import org.jruby.ir.operands.SymbolProc;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.UnexecutableNil;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ArgumentType;
import org.jruby.runtime.CallType;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.RubyEvent;
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

import static org.jruby.ir.instructions.Instr.EMPTY_OPERANDS;
import static org.jruby.ir.instructions.IntegerMathInstr.Op.ADD;
import static org.jruby.ir.instructions.IntegerMathInstr.Op.SUBTRACT;
import static org.jruby.ir.instructions.RuntimeHelperCall.Methods.*;

import static org.jruby.ir.operands.ScopeModule.*;
import static org.jruby.runtime.CallType.*;
import static org.jruby.runtime.ThreadContext.*;
import static org.jruby.util.RubyStringBuilder.str;

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

public class IRBuilderAST extends IRBuilder<Node, DefNode, WhenNode, RescueBodyNode> {
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

    public IRBuilderAST(IRManager manager, IRScope scope, IRBuilder parent, IRBuilder variableBuilder) {
        super(manager, scope, parent, variableBuilder);
    }

    @Override
    public Operand build(ParseResult result) {
        return build(((RootNode) result).getBodyNode());
    }

    public IRBuilderAST(IRManager manager, IRScope scope, IRBuilder parent) {
        this(manager, scope, parent, null);
    }

    private NotCompilableException notCompilable(String message, Node node) {
        int line = node != null ? node.getLine() : scope.getLine();
        String loc = scope.getFile() + ":" + line;
        String what = node != null ? node.getClass().getSimpleName() + " - " + loc : loc;
        return new NotCompilableException(message + " (" + what + ").");
    }

    private Operand buildOperand(Variable result, Node node) throws NotCompilableException {
        if (node.isNewline()) determineIfWeNeedLineNumber(node.getLine(), node.isNewline());

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
        IRClosure closure = new IRClosure(getManager(), scope, node.getLine(), node.getScope(), Signature.from(node), coverageMode);

        // Create a new nested builder to ensure this gets its own IR builder state like the ensure block stack
        newIRBuilder(getManager(), closure).buildLambdaInner(node.getArgs(), node.getBody());

        Variable lambda = temp();
        WrappedIRClosure lambdaBody = new WrappedIRClosure(closure.getSelf(), closure);
        addInstr(new BuildLambdaInstr(lambda, lambdaBody));
        return lambda;
    }

    public Operand buildEncoding(EncodingNode node) {
        Variable ret = temp();
        addInstr(new GetEncodingInstr(ret, node.getEncoding()));
        return ret;
    }

    // Non-arg masgn
    public Operand buildMultipleAsgn19(MultipleAsgnNode multipleAsgnNode) {
        Node valueNode = multipleAsgnNode.getValueNode();
        Variable ret = getValueInTemporaryVariable(build(valueNode));
        if (valueNode instanceof ArrayNode || valueNode instanceof ZArrayNode) {
            buildMultipleAssignment(multipleAsgnNode, ret);
        } else if (valueNode instanceof ILiteralNode) {
            // treat a single literal value as a single-element array
            buildMultipleAssignment(multipleAsgnNode, new Array(new Operand[]{ret}));
        } else {
            buildMultipleAssignment(multipleAsgnNode, addResultInstr(new ToAryInstr(temp(), ret)));
        }
        return ret;
    }

    protected Operand buildLazyWithOrder(CallNode node, Label lazyLabel, Label endLabel, boolean preserveOrder) {
        Operand value = buildCall(null, node, lazyLabel, endLabel);

        // We need to preserve order in cases (like in presence of assignments) except that immutable
        // literals can never change value so we can still emit these out of order.
        return preserveOrder && !(value instanceof ImmutableLiteral) ? copy(value) : value;
    }

    // Return the last argument in the list as this represents rhs of the overall attrassign expression
    // e.g. 'a[1] = 2 #=> 2' or 'a[1] = 1,2,3 #=> [1,2,3]'
    protected Operand buildAttrAssignCallArgs(List<Operand> argsList, Node args, boolean containsAssignment) {
        if (args == null) return nil();

        switch (args.getNodeType()) {
            case ARRAYNODE: {     // a[1] = 2; a[1,2,3] = 4,5,6
                Operand last = nil();
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
                Variable res = temp();
                addInstr(new BuildCompoundArrayInstr(res, lhs, rhs, false, false));
                argsList.add(new Splat(res));
                return rhs;
            }
            case ARGSPUSHNODE:  { // a[1, *b] = 2
                ArgsPushNode argsPushNode = (ArgsPushNode)args;
                Operand lhs = build(argsPushNode.getFirstNode());
                Operand rhs = build(argsPushNode.getSecondNode());
                Variable res = temp();
                addInstr(new BuildCompoundArrayInstr(res, lhs, rhs, true, false));
                argsList.add(new Splat(res));
                return rhs;
            }
            case SPLATNODE: {     // a[1] = *b
                Splat rhs = new Splat(buildSplat(temp(), (SplatNode)args));
                argsList.add(rhs);
                return rhs;
            }
        }

        throw notCompilable("Invalid node for attrassign call args", args);
    }

    private Operand buildRestKeywordArgs(HashNode keywordArgs, int[] flags) {
        flags[0] |= CALL_KEYWORD_REST;
        List<KeyValuePair<Node, Node>> pairs = keywordArgs.getPairs();

        if (pairs.size() == 1) { // Only a single rest arg here.  Do not bother to merge.
            Operand splat = buildWithOrder(pairs.get(0).getValue(), keywordArgs.containsVariableAssignment());

            return addResultInstr(new RuntimeHelperCall(temp(), HASH_CHECK, new Operand[] { splat }));
        }

        Variable splatValue = copy(new Hash(new ArrayList<>()));
        for (KeyValuePair<Node, Node> pair: pairs) {
            Operand splat = buildWithOrder(pair.getValue(), keywordArgs.containsVariableAssignment());
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
                return new Operand[] { new Splat(addResultInstr(new BuildSplatInstr(temp(), build(args), false))) };

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

    protected void receiveBlockArg(Variable v, Operand argsArray, int argIndex, boolean isSplat) {
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
                v = temp();
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
                v = temp();
                receiveBlockArg(v, argsArray, argIndex, isSplat);
                addInstr(new PutClassVariableInstr(classVarDefinitionContainer(), ((ClassVarAsgnNode)node).getName(), v));
                break;
            case CONSTDECLNODE:
                v = temp();
                receiveBlockArg(v, argsArray, argIndex, isSplat);
                buildConstDeclAssignment((ConstDeclNode) node, v);
                break;
            case GLOBALASGNNODE:
                v = temp();
                receiveBlockArg(v, argsArray, argIndex, isSplat);
                addInstr(new PutGlobalVarInstr(((GlobalAsgnNode)node).getName(), v));
                break;
            case INSTASGNNODE:
                v = temp();
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

    private Operand buildAttrAssign(Variable result, AttrAssignNode attrAssignNode) {
        boolean containsAssignment = attrAssignNode.containsVariableAssignment();
        Operand obj = buildWithOrder(attrAssignNode.getReceiverNode(), containsAssignment);

        Label lazyLabel = null;
        Label endLabel = null;
        if (result == null) result = temp();
        if (attrAssignNode.isLazy()) {
            lazyLabel = getNewLabel();
            endLabel = getNewLabel();
            addInstr(new BNilInstr(lazyLabel, obj));
        }

        List<Operand> args = new ArrayList<>();
        Node argsNode = attrAssignNode.getArgsNode();
        int[] flags = new int[] { 0 };
        Operand lastArg = buildAttrAssignCallArgs(args, argsNode, containsAssignment);
        Operand block = setupCallClosure(attrAssignNode.getBlockNode());
        addInstr(AttrAssignInstr.create(scope, obj, attrAssignNode.getName(), args.toArray(new Operand[args.size()]),
                block, flags[0], scope.maybeUsingRefinements()));
        addInstr(new CopyInstr(result, lastArg));

        if (attrAssignNode.isLazy()) {
            addInstr(new JumpInstr(endLabel));
            addInstr(new LabelInstr(lazyLabel));
            addInstr(new CopyInstr(result, nil()));
            addInstr(new LabelInstr(endLabel));
        }

        return result;
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
                !((StrNode) arg0).isFrozen() &&

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

        int[] flags = new int[] { 0 };
        Operand[] args = setupCallArgs(callNode.getArgsNode(), flags);
        Operand block = setupCallClosure(callNode.getIterNode());

        determineIfWeNeedLineNumber(callNode.getLine(), callNode.isNewline()); // backtrace needs line of call in case of exception.
        if ((flags[0] & CALL_KEYWORD_REST) != 0) {  // {**k}, {**{}, **k}, etc...
            Variable test = addResultInstr(new RuntimeHelperCall(temp(), IS_HASH_EMPTY, new Operand[] { args[args.length - 1] }));
            if_else(test, tru(),
                    () -> receiveBreakException(block,
                            determineIfProcNew(receiverNode,
                                    CallInstr.create(scope, NORMAL, result, name, receiver, removeArg(args), block, flags[0]))),
                    () -> receiveBreakException(block,
                            determineIfProcNew(receiverNode,
                                    CallInstr.create(scope, NORMAL, result, name, receiver, args, block, flags[0]))));
        } else {
            receiveBreakException(block,
                    determineIfProcNew(receiverNode,
                            CallInstr.create(scope, NORMAL, result, name, receiver, args, block, flags[0])));
        }

        if (compileLazyLabel) {
            addInstr(new JumpInstr(endLabel));
            addInstr(new LabelInstr(lazyLabel));
            addInstr(new CopyInstr(result, nil()));
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

    CallInstr determineIfProcNew(Node receiverNode, CallInstr callInstr) {
        // This is to support the ugly Proc.new with no block, which must see caller's frame
        if (CommonByteLists.NEW_METHOD.equals(callInstr.getName().getBytes()) &&
                receiverNode instanceof ConstNode && ((ConstNode)receiverNode).getName().idString().equals("Proc")) {
            callInstr.setProcNew(true);
        }

        return callInstr;
    }

    private void buildFindPattern(Label testEnd, Variable result, Variable deconstructed, FindPatternNode pattern,
                                  Operand obj, boolean inAlteration, boolean isSinglePattern, Variable errorString) {
        if (pattern.hasConstant()) {
            Operand constant = build(pattern.getConstant());
            addInstr(new EQQInstr(scope, result, constant, obj, false, true));
            cond_ne(testEnd, result, tru());
        }

        label("deconstruct_end", deconstructCheck -> {
            cond_ne(deconstructCheck, deconstructed, buildNil(), () -> {
                call(result, obj, "respond_to?", new Symbol(symbol("deconstruct")));
                cond_ne(testEnd, result, tru());

                call(deconstructed, obj, "deconstruct");
                label("array_check_end", arrayCheck -> {
                    addInstr(new EQQInstr(scope, result, getManager().getArrayClass(), deconstructed, false, false));
                    cond(arrayCheck, result, tru(), () -> type_error("deconstruct must return Array"));
                });
            });
        });

        Variable length = addResultInstr(new RuntimeHelperCall(temp(), ARRAY_LENGTH, new Operand[]{deconstructed}));
        int fixedArgsLength = pattern.getArgs().size();
        Operand argsNum = new Integer(fixedArgsLength);

        label("size_check_end", sizeCheckEnd -> {
            addInstr(new BIntInstr(sizeCheckEnd, BIntInstr.Op.LTE, argsNum, length));
            copy(result, fals());
            jump(testEnd);
        });

        Variable limit = addResultInstr(new IntegerMathInstr(SUBTRACT, temp(), length, argsNum));
        Variable i = copy(new Integer(0));

        for_loop(after -> addInstr(new BIntInstr(after, BIntInstr.Op.GT, i, limit)),
                after -> addInstr(new IntegerMathInstr(ADD, i, i, new Integer(1))),
                (after, bottom) -> {
                    times(fixedArgsLength, (end_times, j) -> {
                        Node pat = pattern.getArgs().get(j.value);
                        Operand deconstructIndex = addResultInstr(new IntegerMathInstr(ADD, temp(), i, new Integer(j.value)));
                        Operand deconstructFixnum = as_fixnum(deconstructIndex);
                        Operand test = call(temp(), deconstructed, "[]", deconstructFixnum);
                        buildPatternMatch(result, copy(buildNil()), pat, test, false, isSinglePattern, errorString);
                        cond_ne(bottom, result, tru());
                    });

                    Node pre = pattern.getPreRestArg();
                    if (pre != null && !(pre instanceof StarNode)) {
                        Operand iFixnum = as_fixnum(i);
                        Operand test = call(temp(), deconstructed, "[]", getManager().newFixnum(0), iFixnum);
                        buildPatternMatch(result, copy(buildNil()), pre, test, false, isSinglePattern, errorString);
                        cond_ne(bottom, result, tru());
                    }

                    Node post = pattern.getPostRestArg();
                    if (post != null && !(post instanceof StarNode)) {
                        Operand deconstructIndex = addResultInstr(new IntegerMathInstr(ADD, temp(), i, argsNum));
                        Operand deconstructFixnum = as_fixnum(deconstructIndex);
                        Operand lengthFixnum = as_fixnum(length);
                        Operand test = call(temp(), deconstructed, "[]", deconstructFixnum, lengthFixnum);
                        buildPatternMatch(result, copy(buildNil()), post, test, false, isSinglePattern, errorString);
                        cond_ne(bottom, result, tru());
                    }
                    jump(after);
                });
    }

    private void buildArrayPattern(Label testEnd, Variable result, Variable deconstructed, ArrayPatternNode pattern,
                                   Operand obj, boolean inAlteration, boolean isSinglePattern, Variable errorString) {
        Variable restNum = addResultInstr(new CopyInstr(temp(), new Integer(0)));

        if (pattern.hasConstant()) {
            Operand constant = build(pattern.getConstant());
            addInstr(new EQQInstr(scope, result, constant, obj, false, true));
            cond_ne(testEnd, result, tru());
        }

        call(result, obj, "respond_to?", new Symbol(symbol("deconstruct")));
        cond_ne(testEnd, result, tru());

        label("deconstruct_cache_end", (deconstruct_cache_end) ->
            cond_ne(deconstruct_cache_end, deconstructed, buildNil(), () -> {
                call(deconstructed, obj, "deconstruct");
                label("array_check_end", arrayCheck -> {
                    addInstr(new EQQInstr(scope, result, getManager().getArrayClass(), deconstructed, false, false));
                    cond(arrayCheck, result, tru(), () -> type_error("deconstruct must return Array"));
                });
            })
        );

        Operand minArgsCount = new Integer(pattern.minimumArgsNum());
        Variable length = addResultInstr(new RuntimeHelperCall(temp(), ARRAY_LENGTH, new Operand[]{deconstructed}));
        label("min_args_check_end", minArgsCheck -> {
            BIntInstr.Op compareOp = pattern.hasRestArg() ? BIntInstr.Op.GTE : BIntInstr.Op.EQ;
            addInstr(new BIntInstr(minArgsCheck, compareOp, length, minArgsCount));
            fcall(errorString, buildSelf(), "sprintf",
                    new FrozenString("%s: %s length mismatch (given %d, expected %d)"), deconstructed, deconstructed, as_fixnum(length), as_fixnum(minArgsCount));
            addInstr(new CopyInstr(result, fals()));
            jump(testEnd);
        });

        ListNode preArgs = pattern.getPreArgs();
        int preArgsSize = preArgs == null ? 0 : preArgs.size();
        if (preArgsSize > 0) {
            for (int i = 0; i < preArgsSize; i++) {
                Variable elt = call(temp(), deconstructed, "[]", fix(i));
                Node arg = preArgs.get(i);

                buildPatternEach(testEnd, result, copy(buildNil()), elt, arg, inAlteration, isSinglePattern, errorString);
                cond_ne(testEnd, result, tru());
            }
        }

        if (pattern.hasRestArg()) {
            addInstr(new IntegerMathInstr(SUBTRACT, restNum, length, minArgsCount));

            if (pattern.isNamedRestArg()) {
                Variable min = copy(fix(preArgsSize));
                Variable max = as_fixnum(restNum);
                Variable elt = call(temp(), deconstructed, "[]", min, max);

                buildPatternMatch(result, copy(buildNil()), pattern.getRestArg(), elt, inAlteration, isSinglePattern, errorString);
                cond_ne(testEnd, result, tru());
            }
        }

        ListNode postArgs = pattern.getPostArgs();
        if (postArgs != null) {
            for (int i = 0; i < postArgs.size(); i++) {
                Label matchElementCheck = getNewLabel("match_post_args_element(i)_end");
                Variable j = addResultInstr(new IntegerMathInstr(ADD, temp(), new Integer(i + preArgsSize), restNum));
                Variable k = as_fixnum(j);
                Variable elt = call(temp(), deconstructed, "[]", k);

                buildPatternEach(testEnd, result, copy(buildNil()), elt, postArgs.get(i), inAlteration, isSinglePattern, errorString);
                addInstr(BNEInstr.create(matchElementCheck, result, fals()));
                addInstr(new JumpInstr(testEnd));
                addInstr(new LabelInstr(matchElementCheck));
            }
        }
    }

    private Variable deconstructHashPatternKeys(Label testEnd, HashPatternNode pattern, Variable result, Operand obj) {
        Operand keys;

        if (pattern.hasKeywordArgs() && !pattern.hashNamedKeywordRestArg()) {
            List<Node> keyNodes = pattern.getKeys();
            int length = keyNodes.size();
            Operand[] builtKeys = new Operand[length];

            for (int i = 0; i < length; i++) {
                builtKeys[i] = build(keyNodes.get(i));
            }
            keys = new Array(builtKeys);
        } else {
            keys = nil();
        }

        if (pattern.getConstant() != null) {
            Operand constant = build(pattern.getConstant());
            addInstr(new EQQInstr(scope, result, constant, obj, false, true));
            cond_ne(testEnd, result, tru());
        }

        call(result, obj, "respond_to?", new Symbol(symbol("deconstruct_keys")));
        cond_ne(testEnd, result, tru());

        return call(temp(), obj, "deconstruct_keys", keys);
    }

    private void buildHashPattern(Label testEnd, Variable result, Variable deconstructed, HashPatternNode pattern,
                                  Operand obj, boolean inAlteration, boolean isSinglePattern, Variable errorString) {
        Variable d = deconstructHashPatternKeys(testEnd, pattern, result, obj);

        label("hash_check_end", endHashCheck -> {
            addInstr(new EQQInstr(scope, result, getManager().getHashClass(), d, false, true));
            cond(endHashCheck, result, tru(), () -> type_error("deconstruct_keys must return Hash"));
        });

        // rest args destructively deletes elements from deconstruct_keys and the default impl is 'self'.
        if (pattern.hasRestArg()) call(d, d, "dup");

        if (pattern.hasKeywordArgs()) {
            List<KeyValuePair<Node,Node>> kwargs = pattern.getKeywordArgs().getPairs();

            for (KeyValuePair<Node,Node> pair: kwargs) {
                // FIXME: only build literals (which are guaranteed to build without raising).
                Operand key = build(pair.getKey());
                call(result, d, "key?", key);
                cond_ne(testEnd, result, tru());

                String method = pattern.hasRestArg() ? "delete" : "[]";
                Operand value = call(temp(), d, method, key);
                buildPatternEach(testEnd, result, copy(buildNil()), value, pair.getValue(), inAlteration, isSinglePattern, errorString);
                cond_ne(testEnd, result, tru());
            }
        } else {
            call(result, d, "empty?");
            if (isSinglePattern) maybeGenerateIsNotEmptyErrorString(errorString, result, d);
            cond_ne(testEnd, result, tru());
        }

        if (pattern.hasRestArg()) {
            if (pattern.getRestArg() instanceof NilRestArgNode) {
                call(result, d, "empty?");
                if (isSinglePattern) maybeGenerateIsNotEmptyErrorString(errorString, result, d);
                cond_ne(testEnd, result, tru());
            } else if (pattern.isNamedRestArg()) {
                buildPatternEach(testEnd, result, copy(buildNil()), d, pattern.getRestArg(), inAlteration, isSinglePattern, errorString);
                cond_ne(testEnd, result, tru());
            }
        }
    }

    private void maybeGenerateIsNotEmptyErrorString(Variable errorString, Operand result, Operand value) {
        label("empty", (empty) ->
                cond(empty, result, tru(), ()->
                        addInstr(new BuildCompoundStringInstr(errorString, new Operand[] {value, new FrozenString(" is not empty")},
                                UTF8Encoding.INSTANCE, 13, true, false, getFileName(), lastProcessedLineNum))));
    }

    private void buildPatternMatch(Variable result, Variable deconstructed, Node arg, Operand obj,
                                   boolean inAlternation, boolean isSinglePattern, Variable errorString) {
        label("pattern_end", testEnd -> buildPatternEach(testEnd, result, deconstructed, obj, arg, inAlternation, isSinglePattern, errorString));
    }

    private Variable buildPatternEach(Label testEnd, Variable result, Variable deconstructed, Operand value,
                                      Node exprNodes, boolean inAlternation, boolean isSinglePattern, Variable errorString) {
        if (exprNodes instanceof ArrayPatternNode) {
            buildArrayPattern(testEnd, result, deconstructed, (ArrayPatternNode) exprNodes, value, inAlternation, isSinglePattern, errorString);
        } else if (exprNodes instanceof HashPatternNode) {
            buildHashPattern(testEnd, result, deconstructed, (HashPatternNode) exprNodes, value, inAlternation, isSinglePattern, errorString);
        } else if (exprNodes instanceof FindPatternNode) {
            buildFindPattern(testEnd, result, deconstructed, (FindPatternNode) exprNodes, value, inAlternation, isSinglePattern, errorString);
        } else if (exprNodes instanceof HashNode) {
            HashNode hash = (HashNode) exprNodes;

            if (hash.getPairs().size() != 1) {
                throwSyntaxError(hash.getLine(), "unexpected node");
            }

            KeyValuePair<Node, Node> pair = hash.getPairs().get(0);
            buildPatternMatch(result, deconstructed, pair.getKey(), value, inAlternation, isSinglePattern, errorString);
            buildPatternEach(testEnd, result, deconstructed, value, pair.getValue(), inAlternation, isSinglePattern, errorString);
        } else if (exprNodes instanceof IfNode) {
            IfNode ifNode = (IfNode) exprNodes;

            boolean unless; // position of body is how we detect between if/unless
            if (ifNode.getThenBody() != null) { // if
                unless = false;
                buildPatternMatch(result, deconstructed, ifNode.getThenBody(), value, inAlternation, isSinglePattern, errorString);
            } else {
                unless = true;
                buildPatternMatch(result, deconstructed, ifNode.getElseBody(), value, inAlternation, isSinglePattern, errorString);
            }
            label("if_else_end", conditionalEnd -> {
                cond_ne(conditionalEnd, result, tru());
                Operand ifResult = build(ifNode.getCondition());
                if (unless) {
                    call(result, ifResult, "!"); // FIXME: need non-dynamic dispatch to reverse result
                } else {
                    addInstr(new CopyInstr(result, ifResult));
                }
            });
        } else if (exprNodes instanceof LocalAsgnNode) {
            LocalAsgnNode localAsgnNode = (LocalAsgnNode) exprNodes;
            RubySymbol name = localAsgnNode.getName();

            if (inAlternation && name.idString().charAt(0) != '_') {
                throwSyntaxError(localAsgnNode.getLine(), str(getManager().getRuntime(), "illegal variable in alternative pattern (", name, ")"));
            }

            Variable variable  = getLocalVariable(name, localAsgnNode.getDepth());
            addInstr(new CopyInstr(variable, value));
        } else if (exprNodes instanceof StarNode) {
            // Do nothing.
        } else if (exprNodes instanceof DAsgnNode) {
            DAsgnNode localAsgnNode = (DAsgnNode) exprNodes;
            RubySymbol name = localAsgnNode.getName();

            if (inAlternation && name.idString().charAt(0) != '_') {
                throwSyntaxError(localAsgnNode.getLine(), str(getManager().getRuntime(), "illegal variable in alternative pattern (", name, ")"));
            }

            Variable variable = getLocalVariable(name, localAsgnNode.getDepth());
            addInstr(new CopyInstr(variable, value));
        } else if (exprNodes instanceof OrNode) {
            OrNode orNode = (OrNode) exprNodes;
            label("or_lhs_end", firstCase -> {
                buildPatternEach(firstCase, result, deconstructed, value, orNode.getFirstNode(), true, isSinglePattern, errorString);
            });
            label("or_rhs_end", secondCase -> {
                cond(secondCase, result, tru(), () -> buildPatternEach(testEnd, result, deconstructed, value, orNode.getSecondNode(), true, isSinglePattern, errorString));
            });
        } else {
            Operand expression = build(exprNodes);
            boolean needsSplat = exprNodes instanceof ArgsPushNode || exprNodes instanceof SplatNode || exprNodes instanceof ArgsCatNode;

            addInstr(new EQQInstr(scope, result, expression, value, needsSplat, scope.maybeUsingRefinements()));
        }

        return result;
    }

    public Operand buildPatternCase(PatternCaseNode patternCase) {
        Variable result = temp();
        Operand value = build(patternCase.getCaseNode());
        Variable errorString = copy(buildNil());

        label("pattern_case_end", end -> {
            List<Label> labels = new ArrayList<>();
            Map<Label, Node> bodies = new HashMap<>();

            // build each "when"
            Variable deconstructed = copy(buildNil());
            for (Node aCase : patternCase.getCases().children()) {
                InNode inNode = (InNode) aCase;
                Label bodyLabel = getNewLabel();

                boolean isSinglePattern = inNode.isSinglePattern();

                Variable eqqResult = copy(tru());
                labels.add(bodyLabel);
                buildPatternMatch(eqqResult, deconstructed, inNode.getExpression(), value, false, isSinglePattern, errorString);
                addInstr(createBranch(eqqResult, tru(), bodyLabel));
                bodies.put(bodyLabel, inNode.getBody());
            }

            Label elseLabel = getNewLabel();
            addInstr(new JumpInstr(elseLabel));      // Jump to else in case nothing matches!

            boolean hasElse = patternCase.getElseNode() != null;

            // Build "else" if it exists
            if (hasElse) {
                labels.add(elseLabel);
                bodies.put(elseLabel, patternCase.getElseNode());
            }

            // Now, emit bodies while preserving when clauses order
            for (Label label : labels) {
                addInstr(new LabelInstr(label));
                Operand bodyValue = build(bodies.get(label));
                if (bodyValue != null) copy(result, bodyValue);
                jump(end);
            }

            if (!hasElse) {
                addInstr(new LabelInstr(elseLabel));
                Variable inspect = temp();
                if_else(errorString, buildNil(), () -> {
                    call(inspect, value, "inspect");
                }, () -> {
                    copy(inspect, errorString);
                });

                addRaiseError("NoMatchingPatternError", inspect);
                jump(end);
            }
        });

        return result;
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

    Node whenBody(WhenNode when) {
        return when.getBodyNode();
    }

    @Override
    boolean containsVariableAssignment(Node node) {
        return node.containsVariableAssignment();
    }

    @Override
    Operand frozen_string(Node node) {
        ((StrNode) node).setFrozen(true);
        return buildStrRaw((StrNode) node);
    }

    @Override
    int getLine(Node node) {
        return node.getLine();
    }

    private void buildWhenSplatValues(Variable eqqResult, Node node, Operand testValue, Label bodyLabel,
                                      Set<IRubyObject> seenLiterals) {
        if (node instanceof ListNode && !(node instanceof DNode) && !(node instanceof ArrayNode)) {
            buildWhenValues(eqqResult, ((ListNode) node).children(), testValue, bodyLabel, seenLiterals);
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

    void buildWhenArgs(WhenNode whenNode, Operand testValue, Label bodyLabel, Set<IRubyObject> seenLiterals) {
        Variable eqqResult = temp();
        Node exprNodes = whenNode.getExpressionNodes();

        if (exprNodes instanceof ListNode && !(exprNodes instanceof DNode) && !(exprNodes instanceof ArrayNode) && !(exprNodes instanceof ZArrayNode)) {
            buildWhenValues(eqqResult, ((ListNode) exprNodes).children(), testValue, bodyLabel, seenLiterals);
        } else if (exprNodes instanceof ArgsPushNode || exprNodes instanceof SplatNode || exprNodes instanceof ArgsCatNode) {
            buildWhenSplatValues(eqqResult, exprNodes, testValue, bodyLabel, seenLiterals);
        } else {
            buildWhenValue(eqqResult, testValue, bodyLabel, exprNodes, seenLiterals, false);
        }
    }

    // Note: This is potentially a little wasteful in that we eagerly create these literals for a duplicated warning
    // check.  In most cases these would be made anyways (e.g. symbols/fixnum) but in others we double allocation
    // (e.g. strings).
    IRubyObject getWhenLiteral(Node node) {
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
    boolean isLiteralString(Node node) {
        return node instanceof StrNode;
    }

    private <T extends Node & ILiteralNode> Variable buildOptimizedCaseWhen(
            CaseNode caseNode, Class caseClass, Function<T, Long> caseFunction) {
        // get the incoming case value
        Operand value = build(caseNode.getCaseNode());

        Map<Node, Label> nodeBodies = new HashMap<>();

        Map<java.lang.Integer, Label> jumpTable = gatherLiteralWhenBodies(caseNode, nodeBodies, caseFunction);
        Map.Entry<java.lang.Integer, Label>[] jumpEntries = sortJumpEntries(jumpTable);

        Label     endLabel  = getNewLabel();
        boolean   hasElse   = (caseNode.getElseNode() != null);
        Label     elseLabel = getNewLabel();
        Variable  result    = temp();

        buildOptimizedSwitch(jumpTable, jumpEntries, elseLabel, value, caseClass);

        return buildStandardCaseWhen(caseNode, nodeBodies, endLabel, hasElse, elseLabel, value, result);
    }

    private <T extends Node & ILiteralNode> Map<java.lang.Integer, Label> gatherLiteralWhenBodies(
            CaseNode caseNode, Map<Node, Label> nodeBodies, Function<T, Long> caseFunction) {
        Map<java.lang.Integer, Label> jumpTable = new HashMap<>();

        // gather literal when bodies or bail
        for (Node aCase : caseNode.getCases().children()) {
            WhenNode whenNode = (WhenNode) aCase;
            Label bodyLabel = getNewLabel();

            T expr = (T) whenNode.getExpressionNodes();
            long exprLong = caseFunction.apply(expr);
            if (exprLong > java.lang.Integer.MAX_VALUE) throw notCompilable("optimized case has long-ranged value", caseNode);

            if (jumpTable.get((int) exprLong) == null) {
                jumpTable.put((int) exprLong, bodyLabel);
                nodeBodies.put(whenNode, bodyLabel);
            } else {
                scope.getManager().getRuntime().getWarnings().warning(IRubyWarnings.ID.MISCELLANEOUS, getFileName(), expr.getLine(), "duplicated when clause is ignored");
            }
        }

        return jumpTable;
    }

    private static Map.Entry<java.lang.Integer, Label>[] sortJumpEntries(Map<java.lang.Integer, Label> jumpTable) {
        // sort the jump table
        Map.Entry<java.lang.Integer, Label>[] jumpEntries = jumpTable.entrySet().toArray(new Map.Entry[jumpTable.size()]);
        Arrays.sort(jumpEntries, Comparator.comparingInt(Map.Entry::getKey));
        return jumpEntries;
    }

    private void buildOptimizedSwitch(
            Map<java.lang.Integer, Label> jumpTable,
            Map.Entry<java.lang.Integer,
                    Label>[] jumpEntries,
            Label elseLabel,
            Operand value,
            Class valueClass) {

        Label     eqqPath   = getNewLabel();

        // build a switch
        int[] jumps = new int[jumpTable.size()];
        Label[] targets = new Label[jumps.length];
        int i = 0;
        for (Map.Entry<java.lang.Integer, Label> jumpEntry : jumpEntries) {
            jumps[i] = jumpEntry.getKey();
            targets[i] = jumpEntry.getValue();
            i++;
        }

        // insert fast switch with fallback to eqq
        addInstr(new BSwitchInstr(jumps, value, eqqPath, targets, elseLabel, valueClass));
        addInstr(new LabelInstr(eqqPath));
    }

    private Variable buildStandardCaseWhen(CaseNode caseNode, Map<Node, Label> nodeBodies, Label endLabel, boolean hasElse, Label elseLabel, Operand value, Variable result) {
        List<Label> labels = new ArrayList<>();
        Map<Label, Node> bodies = new HashMap<>();

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

            addInstr(new EQQInstr(scope, eqqResult, expression, value, false, scope.maybeUsingRefinements()));
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
        return buildConstDeclAssignment(node, build(node.getValueNode()));
    }

    public Operand buildConstDeclAssignment(ConstDeclNode constDeclNode, Operand value) {
        Node constNode = constDeclNode.getConstNode();

        if (constNode == null) return putConstant(constDeclNode.getName(), value);

        return putConstant((Colon3Node) constNode, value);
    }

    private Operand putConstant(Colon3Node colonNode, Operand value) {
        if (colonNode.getNodeType() == NodeType.COLON2NODE) {
            Colon2Node colon2Node = (Colon2Node) colonNode;
            return putConstant(build(colon2Node.getLeftNode()), colon2Node.getName(), value);
        } else { // colon3, assign in Object
            return putConstant(getManager().getObjectClass(), colonNode.getName(), value);
        }
    }

    private Operand putConstantAssignment(OpAsgnConstDeclNode node, Operand value) {
        return putConstant((Colon3Node) node.getFirstNode(), value);
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

    boolean needsDefinitionCheck(Node node) {
        return node.needsDefinitionCheck();
    }

    Operand buildGetDefinition(Node node) {
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
            return addResultInstr(
                    new RuntimeHelperCall(
                            temp(),
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
            return addResultInstr(
                    new RuntimeHelperCall(
                            temp(),
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
                            temp(),
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
        case CONSTNODE: {
            Label defLabel = getNewLabel();
            Label doneLabel = getNewLabel();
            Variable tmpVar  = temp();
            RubySymbol constName = ((ConstNode) node).getName();
            addInstr(new LexicalSearchConstInstr(tmpVar, CurrentScope.INSTANCE, constName));
            addInstr(BNEInstr.create(defLabel, tmpVar, UndefinedValue.UNDEFINED));
            addInstr(new InheritanceSearchConstInstr(tmpVar, findContainerModule(), constName)); // SSS FIXME: should this be the current-module var or something else?
            addInstr(BNEInstr.create(defLabel, tmpVar, UndefinedValue.UNDEFINED));
            addInstr(new CopyInstr(tmpVar, nil()));
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
                    Variable tmpVar     = temp();
                    Operand  receiver   = build(attrAssign.getReceiverNode());
                    addInstr(
                            new RuntimeHelperCall(
                                    tmpVar,
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
                    Operand argsCheckDefn = buildGetArgumentDefinition(attrAssign.getArgsNode(), "assignment");
                    return buildDefnCheckIfThenPaths(undefLabel, argsCheckDefn);
                }
            };

            // rescue block
            CodeBlock rescueBlock = new CodeBlock() {
                public Operand run() { return nil(); } // Nothing to do if we got an exception
            };

            // Try verifying definition, and if we get an JumpException exception, process it with the rescue block above
            return protectCodeWithRescue(protectedCode, rescueBlock);
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

    boolean canBeLazyMethod(DefNode node) {
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
                addArgumentDescription(restArgNode.isAnonymous() ?
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
        KeywordRestArgNode keyRest = argsNode.getKeyRest();
        if (keyRest != null) {
            RubySymbol key = keyRest.getName();
            ArgumentType type = ArgumentType.keyrest;

            // anonymous keyrest
            if (key == null || key.getBytes().realSize() == 0) type = ArgumentType.anonkeyrest;

            Variable av = getNewLocalVariable(key, 0);
            if (scope instanceof IRMethod) addArgumentDescription(type, key);

            if (key != null && "nil".equals(key.idString())) {
                if_not(keywords, UndefinedValue.UNDEFINED, () -> addRaiseError("ArgumentError", "no keywords accepted"));
            } else {
                addInstr(new ReceiveKeywordRestArgInstr(av, keywords));
            }
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
        final List<Tuple<Node, Variable>> assigns = new ArrayList<>();

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
        final List<Tuple<Node, Variable>> assigns = new ArrayList<>();
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

    void receiveBlockArgs(Node args) {
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

    int dynamicPiece(Operand[] pieces, int i, Node pieceNode, boolean _interpolated) {
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
        return buildDStr(result, node.children(), node.getEncoding(), node.isFrozen(), node.getLine());
    }

    public Operand buildDSymbol(Variable result, DSymbolNode node) {
        return buildDSymbol(result, node.children(), node.getEncoding(), node.getLine());
    }

    public Operand buildDVar(DVarNode node) {
        return getLocalVariable(node.getName(), node.getDepth());
    }

    public Operand buildDXStr(Variable result, DXStrNode node) {
        Node[] nodePieces = node.children();
        Operand[] pieces = new Operand[nodePieces.length];
        int estimatedSize = 0;

        for (int i = 0; i < pieces.length; i++) {
            estimatedSize += dynamicPiece(pieces, i, nodePieces[i], false);
        }

        Variable stringResult = temp();
        if (result == null) result = temp();

        boolean debuggingFrozenStringLiteral = getManager().getInstanceConfig().isDebuggingFrozenStringLiteral();
        addInstr(new BuildCompoundStringInstr(stringResult, pieces, node.getEncoding(), estimatedSize, false, debuggingFrozenStringLiteral, getFileName(), node.getLine()));

        return fcall(result, Self.SELF, "`", stringResult);
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
        return buildEnsureInternal(null, null, null, null, null, false, ensureNode.getBodyNode(), ensureNode.getEnsureNode(), false);
    }

    public Operand buildFCall(Variable aResult, FCallNode fcallNode) {
        RubySymbol name = methodName = fcallNode.getName();
        Node callArgsNode = fcallNode.getArgsNode();
        Variable result = aResult == null ? temp() : aResult;
        int[] flags = new int[] { 0 };
        Operand[] args = setupCallArgs(callArgsNode, flags);

        // check for refinement calls before building any closure
        determineIfMaybeRefined(fcallNode.getName(), args);

        Operand block = setupCallClosure(fcallNode.getIterNode());
        determineIfWeNeedLineNumber(fcallNode.getLine(), fcallNode.isNewline()); // backtrace needs line of call in case of exception.

        if ((flags[0] & CALL_KEYWORD_REST) != 0) {  // {**k}, {**{}, **k}, etc...
            Variable test = addResultInstr(new RuntimeHelperCall(temp(), IS_HASH_EMPTY, new Operand[] { args[args.length - 1] }));
            if_else(test, tru(),
                    () -> receiveBreakException(block, CallInstr.create(scope, FUNCTIONAL, result, name, buildSelf(), removeArg(args), block, flags[0])),
                    () -> receiveBreakException(block, CallInstr.create(scope, FUNCTIONAL, result, name, buildSelf(), args, block, flags[0])));
        } else {
            // We will stuff away the iters AST source into the closure in the hope we can convert
            // this closure to a method.
            if (CommonByteLists.DEFINE_METHOD_METHOD.equals(fcallNode.getName().getBytes()) && block instanceof WrappedIRClosure) {
                IRClosure closure = ((WrappedIRClosure) block).getClosure();

                // To convert to a method we need its variable scoping to appear like a normal method.
                if (!closure.accessesParentsLocalVariables() && fcallNode.getIterNode() instanceof IterNode) {
                    closure.setSource((IterNode) fcallNode.getIterNode());
                }
            }

            receiveBreakException(block, CallInstr.create(scope, FUNCTIONAL, result, name, buildSelf(), args, block, flags[0]));
        }

        return result;
    }

    Operand setupCallClosure(Node node) {
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

    // FIXME: This needs to be called on super/zsuper too
    private void determineIfMaybeRefined(RubySymbol methodName, Operand[] args) {
        IRScope outerScope = scope.getNearestTopLocalVariableScope();

        // 'using single_mod_arg' possible nearly everywhere but method scopes.
        boolean refinement = false;
        if (!(outerScope instanceof IRMethod)) {
            ByteList methodBytes = methodName.getBytes();
            if (args.length == 1) {
                refinement = isRefinementCall(methodBytes);
            } else if (args.length == 2
                    && CommonByteLists.SEND.equal(methodBytes)) {
                if (args[0] instanceof Symbol) {
                    Symbol sendName = (Symbol) args[0];
                    methodBytes = sendName.getBytes();
                    refinement = isRefinementCall(methodBytes);
                }
            }
        }

        if (refinement) scope.setIsMaybeUsingRefinements();
    }

    private static boolean isRefinementCall(ByteList methodBytes) {
        return CommonByteLists.USING_METHOD.equals(methodBytes)
                // FIXME: This sets the bit for the whole module, but really only the refine block needs it
                || CommonByteLists.REFINE_METHOD.equals(methodBytes);
    }

    public Operand buildFixnum(FixnumNode node) {
        return fix(node.getValue());
    }

    public Operand buildFlip(FlipNode flipNode) {
        addRaiseError("NotImplementedError", "flip-flop is no longer supported in JRuby");
        return nil(); // not-reached
    }

    public Operand buildFloat(FloatNode node) {
        // Since flaot literals are effectively interned objects, no need to copyAndReturnValue(...)
        // SSS FIXME: Or is this a premature optimization?
        return new Float(node.getValue());
    }

    public Operand buildFor(ForNode forNode) {
        Variable result = temp();
        Operand  receiver = build(forNode.getIterNode());
        Operand  forBlock = buildForIter(forNode);
        CallInstr callInstr = new CallInstr(scope, CallType.NORMAL, result, getManager().runtime.newSymbol(CommonByteLists.EACH), receiver, EMPTY_OPERANDS,
                forBlock, 0, scope.maybeUsingRefinements());
        receiveBreakException(forBlock, callInstr);

        return result;
    }

    public Operand buildForIter(final ForNode forNode) {
        // Create a new closure context
        IRClosure closure = new IRFor(getManager(), scope, forNode.getLine(), forNode.getScope(), Signature.from(forNode));

        // Create a new nested builder to ensure this gets its own IR builder state like the ensure block stack
        newIRBuilder(getManager(), closure).buildIterInner(null, forNode);

        return new WrappedIRClosure(buildSelf(), closure);
    }

    public Operand buildGlobalAsgn(GlobalAsgnNode node) {
        return buildGlobalAsgn(node.getName(), node.getValueNode());
    }

    Operand buildGlobalVar(Variable result, GlobalVarNode node) {
        return buildGlobalVar(result, node.getName());
    }

    public Operand buildHash(HashNode hashNode) {
        List<KeyValuePair<Operand, Operand>> args = new ArrayList<>();
        boolean hasAssignments = hashNode.containsVariableAssignment();
        Variable hash = null;
        // Duplication checks happen when **{} are literals and not **h variable references.
        Operand duplicateCheck = fals();

        for (KeyValuePair<Node, Node> pair: hashNode.getPairs()) {
            Node key = pair.getKey();
            Operand keyOperand;

            if (key == null) {                          // Splat kwarg [e.g. {**splat1, a: 1, **splat2)]
                Node value = pair.getValue();
                 duplicateCheck = value instanceof HashNode && ((HashNode) value).isLiteral() ? tru() : fals();
                if (hash == null) {                     // No hash yet. Define so order is preserved.
                    hash = copy(new Hash(args));
                    args = new ArrayList<>();           // Used args but we may find more after the splat so we reset
                } else if (!args.isEmpty()) {
                    addInstr(new RuntimeHelperCall(hash, MERGE_KWARGS, new Operand[] { hash, new Hash(args), duplicateCheck}));
                    args = new ArrayList<>();
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

    private InterpreterContext buildIterInner(RubySymbol methodName, IterNode iterNode) {
        this.methodName = methodName;

        boolean forNode = iterNode instanceof ForNode;
        prepareClosureImplicitState();                                    // recv_self, add frame block, etc)

        if (RubyInstanceConfig.FULL_TRACE_ENABLED) {
            addInstr(new TraceInstr(RubyEvent.B_CALL, getCurrentModuleVariable(), getName(), getFileName(), scope.getLine() + 1));
        }

        if (!forNode) addCurrentModule();                                // %current_module
        receiveBlockArgs(iterNode.getVarNode());
        // for adds these after processing binding block args because and operations at that point happen relative
        // to the previous scope.
        if (forNode) addCurrentModule();                                 // %current_module

        // conceptually abstract prologue scope instr creation so we can put this at the end of it instead of replicate it.
        afterPrologueIndex = instructions.size() - 1;

        // Build closure body and return the result of the closure
        Operand closureRetVal = iterNode.getBodyNode() == null ? nil() : build(iterNode.getBodyNode());

        if (RubyInstanceConfig.FULL_TRACE_ENABLED) {
            addInstr(new TraceInstr(RubyEvent.B_RETURN, getCurrentModuleVariable(), getName(), getFileName(), iterNode.getEndLine() + 1));
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
        IRClosure closure = new IRClosure(getManager(), scope, iterNode.getLine(), iterNode.getScope(), Signature.from(iterNode), coverageMode);

        // Create a new nested builder to ensure this gets its own IR builder state like the ensure block stack
        newIRBuilder(getManager(), closure).buildIterInner(methodName, iterNode);

        methodName = null;

        return new WrappedIRClosure(buildSelf(), closure);
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
        Operand regexp = build(matchNode.getRegexpNode());

        Variable tempLastLine = temp();
        addResultInstr(new GetGlobalVariableInstr(tempLastLine, symbol("$_")));

        if (result == null) result = temp();
        return addResultInstr(new MatchInstr(scope, result, regexp, tempLastLine));
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

    Operand getContainerFromCPath(Node cpath) {
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

    // FIXME: The logic for lazy and non-lazy building is pretty icky...clean up
    public Operand buildOpAsgn(OpAsgnNode opAsgnNode) {
        Label l;
        Variable writerValue = temp();
        Node receiver = opAsgnNode.getReceiverNode();
        CallType callType = receiver instanceof SelfNode ? FUNCTIONAL : NORMAL;

        // get attr
        Operand  v1 = build(opAsgnNode.getReceiverNode());

        Label lazyLabel = null;
        Label endLabel = null;
        Variable result = temp();
        if (opAsgnNode.isLazy()) {
            lazyLabel = getNewLabel();
            endLabel = getNewLabel();
            addInstr(new BNilInstr(lazyLabel, v1));
        }

        Variable readerValue = _call(temp(), callType, v1, opAsgnNode.getVariableSymbolName());

        // Ex: e.val ||= n
        //     e.val &&= n
        boolean isOrOr = opAsgnNode.isOr();
        if (isOrOr || opAsgnNode.isAnd()) {
            l = getNewLabel();
            addInstr(createBranch(readerValue, isOrOr ? tru() : fals(), l));

            // compute value and set it
            Operand  v2 = build(opAsgnNode.getValueNode());
            _call(writerValue, callType, v1, opAsgnNode.getVariableSymbolNameAsgn(), v2);
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
            Variable setValue = call(temp(), readerValue, opAsgnNode.getOperatorSymbolName(), v2);

            // set attr
            _call(writerValue, callType, v1, opAsgnNode.getVariableSymbolNameAsgn(), setValue);

            // Returning writerValue is incorrect because the assignment method
            // might return something else other than the value being set!
            if (!opAsgnNode.isLazy()) return setValue;

            addInstr(new CopyInstr(result, setValue));
        }

        addInstr(new JumpInstr(endLabel));
        addInstr(new LabelInstr(lazyLabel));
        addInstr(new CopyInstr(result, nil()));
        addInstr(new LabelInstr(endLabel));

        return result;
    }

    private Operand buildColon2ForConstAsgnDeclNode(Node lhs, Variable valueResult, boolean constMissing) {
        Variable leftModule = temp();
        RubySymbol name;

        if (lhs instanceof Colon2Node) {
            Colon2Node colon2Node = (Colon2Node) lhs;
            name = colon2Node.getName();
            Operand leftValue = build(colon2Node.getLeftNode());
            copy(leftModule, leftValue);
        } else { // colon3
            copy(leftModule, getManager().getObjectClass());
            name = ((Colon3Node) lhs).getName();
        }

        addInstr(new SearchModuleForConstInstr(valueResult, leftModule, name, false, constMissing));

        return leftModule;
    }

    public Operand buildOpAsgnConstDeclNode(OpAsgnConstDeclNode node) {
        if (node.isOr()) {
            Variable result = temp();
            Label falseCheck = getNewLabel();
            Label done = getNewLabel();
            Label assign = getNewLabel();
            Operand module = buildColon2ForConstAsgnDeclNode(node.getFirstNode(), result, false);
            addInstr(BNEInstr.create(falseCheck, result, UndefinedValue.UNDEFINED));
            addInstr(new JumpInstr(assign));
            addInstr(new LabelInstr(falseCheck));
            addInstr(BNEInstr.create(done, result, fals()));
            addInstr(new LabelInstr(assign));
            Operand rhsValue = build(node.getSecondNode());
            copy(result, rhsValue);
            addInstr(new PutConstInstr(module, ((Colon3Node) node.getFirstNode()).getName(), rhsValue));
            addInstr(new LabelInstr(done));
            return result;
        } else if (node.isAnd()) {
            Variable result = temp();
            Label done = getNewLabel();
            Operand module = buildColon2ForConstAsgnDeclNode(node.getFirstNode(), result, true);
            addInstr(new BFalseInstr(done, result));
            Operand rhsValue = build(node.getSecondNode());
            copy(result, rhsValue);
            addInstr(new PutConstInstr(module, ((Colon3Node) node.getFirstNode()).getName(), rhsValue));
            addInstr(new LabelInstr(done));
            return result;
        }

        Operand lhs = build(node.getFirstNode());
        Operand rhs = build(node.getSecondNode());
        Variable result = call(temp(), lhs, node.getSymbolOperator(), rhs);
        return addResultInstr(new CopyInstr(temp(), putConstantAssignment(node, result)));
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
        addInstr(createBranch(v1, fals(), l));
        Operand v2 = build(andNode.getSecondNode());  // This does the assignment!
        addInstr(new CopyInstr(result, v2));
        addInstr(new LabelInstr(l));
        return result;
    }

    public Operand buildOpAsgnOr(final OpAsgnOrNode orNode) {
        return buildOpAsgnOr(orNode.getFirstNode(), orNode.getSecondNode());
    }

    public Operand buildOpElementAsgn(OpElementAsgnNode node) {
        // Translate "a[x] ||= n" --> "a[x] = n if !is_true(a[x])"
        if (node.isOr()) return buildOpElementAsgnWith(node, tru());

        // Translate "a[x] &&= n" --> "a[x] = n if is_true(a[x])"
        if (node.isAnd()) return buildOpElementAsgnWith(node, fals());

        // a[i] *= n, etc.  anything that is not "a[i] &&= .. or a[i] ||= .."
        return buildOpElementAsgnWithMethod(node);
    }

    private Operand buildOpElementAsgnWith(OpElementAsgnNode opElementAsgnNode, Boolean truthy) {
        Node receiver = opElementAsgnNode.getReceiverNode();
        CallType callType = receiver instanceof SelfNode ? FUNCTIONAL : CallType.NORMAL;
        Operand array = buildWithOrder(receiver, opElementAsgnNode.containsVariableAssignment());
        Label endLabel = getNewLabel();
        Variable elt = temp();
        int[] flags = new int[] { 0 };
        Operand[] argList = setupCallArgs(opElementAsgnNode.getArgsNode(), flags);
        Operand block = setupCallClosure(opElementAsgnNode.getBlockNode());
        addInstr(CallInstr.create(scope, callType, elt, symbol(ArrayDerefInstr.AREF), array, argList, block, flags[0]));
        addInstr(createBranch(elt, truthy, endLabel));
        Operand value = build(opElementAsgnNode.getValueNode());

        // FIXME: I think this will put value into kwargs spot but MRI does not support this at all.
        argList = addArg(argList, value);
        addInstr(CallInstr.create(scope, callType, elt, symbol(ArrayDerefInstr.ASET), array, argList, block, flags[0]));
        addInstr(new CopyInstr(elt, value));

        addInstr(new LabelInstr(endLabel));
        return elt;
    }

    // a[i] *= n, etc.  anything that is not "a[i] &&= .. or a[i] ||= .."
    public Operand buildOpElementAsgnWithMethod(OpElementAsgnNode opElementAsgnNode) {
        Node receiver = opElementAsgnNode.getReceiverNode();
        CallType callType = receiver instanceof SelfNode ? FUNCTIONAL : CallType.NORMAL;
        Operand array = buildWithOrder(receiver, opElementAsgnNode.containsVariableAssignment());
        int[] flags = new int[] { 0 };
        Operand[] argList = setupCallArgs(opElementAsgnNode.getArgsNode(), flags);
        Operand block = setupCallClosure(opElementAsgnNode.getBlockNode());
        Variable elt = temp();
        addInstr(CallInstr.create(scope, callType, elt, symbol(ArrayDerefInstr.AREF), array, argList, block, flags[0])); // elt = a[args]

        Operand value = build(opElementAsgnNode.getValueNode());                                       // Load 'value'
        _call(elt, callType, elt, opElementAsgnNode.getOperatorSymbolName(), value); // elt = elt.OPERATION(value)
        // FIXME: I think this will put value into kwargs spot but MRI does not support this at all.
        // SSS: do not load the call result into 'elt' to eliminate the RAW dependency on the call
        // We already know what the result is going be .. we are just storing it back into the array
        argList = addArg(argList, elt);
        addInstr(CallInstr.create(scope, callType, temp(), symbol(ArrayDerefInstr.ASET), array, argList, block, flags[0]));   // a[args] = elt
        return elt;
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
        IRBuilderAST builder = new IRBuilderAST(getManager(), scope, this, this);

        List<Instr> beginInstrs = builder.buildPreExeInner(preExeNode.getBodyNode());

        instructions.addAll(afterPrologueIndex, beginInstrs);

        afterPrologueIndex += beginInstrs.size();

        return nil();
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
                    throwSyntaxError(redoNode.getLine(), "Can't escape from eval with redo");
                } else {
                    addInstr(new ThreadPollInstr(true));
                    Label startLabel = new Label(scope.getId() + "_START", 0);
                    instructions.add(afterPrologueIndex, new LabelInstr(startLabel));
                    addInstr(new JumpInstr(startLabel));
                }
            } else {
                throwSyntaxError(redoNode.getLine(), "Invalid redo");
            }
        }
        return nil();
    }

    public Operand buildRegexp(RegexpNode reNode) {
        // SSS FIXME: Rather than throw syntax error at runtime, we should detect
        // regexp syntax errors at build time and add an exception-throwing instruction instead
        return copy(new Regexp(reNode.getValue(), reNode.getOptions()));
    }

    public Operand buildRescue(RescueNode node) {
        RescueBodyNode clause = node.getRescueNode();
        return buildEnsureInternal(node.getBodyNode(), node.getElseNode(), exceptionNodesFor(clause), bodyFor(clause),
                optRescueFor(clause), node instanceof RescueModNode, null, null, true);
    }

    // FIXME: This MIGHT be able to expand to more complicated expressions like Hash or Array if they
    // contain only SideEffectFree nodes.  Constructing a literal out of these should be safe from
    // effecting or being able to access $!.
    boolean isSideEffectFree(final Node node) {
        return node instanceof SideEffectFree;
    }

    boolean isErrorInfoGlobal(final Node body) {
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

    // In order to line up with YARP we will spend some cost making Node[] in legacy parser.
    private Node[] asList(Node node) {
        if (node == null) return null;
        if (node instanceof ListNode) return ((ListNode) node).children();
        return new Node[] { node };
    }

    Node[] exceptionNodesFor(RescueBodyNode node) {
        return asList(node.getExceptionNodes());
    }

    Node bodyFor(RescueBodyNode node) {
        return node.getBodyNode();
    }

    RescueBodyNode optRescueFor(RescueBodyNode node) {
        return node.getOptRescueNode();
    }

    public Operand buildRetry(RetryNode node) {
        return buildRetry(node.getLine());
    }

    public Operand buildReturn(ReturnNode returnNode) {
        return buildReturn(build(returnNode.getValueNode()), returnNode.getLine());
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

        if (strNode.isFrozen()) return new FrozenString(strNode.getValue(), strNode.getCodeRange(), scope.getFile(), line);

        return new MutableString(strNode.getValue(), strNode.getCodeRange(), scope.getFile(), line);
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
        addInstr(new GVarAliasInstr(new MutableString(valiasNode.getNewName()), new MutableString(valiasNode.getOldName())));

        return nil();
    }

    public Operand buildVCall(Variable result, VCallNode node) {
        return _call(result, VARIABLE, buildSelf(), node.getName());
    }

    public Operand buildWhile(WhileNode node) {
        return buildConditionalLoop(node.getConditionNode(), node.getBodyNode(), true, node.evaluateAtStart());
    }

    public Operand buildXStr(Variable result, XStrNode node) {
        return fcall(result, Self.SELF, "`", new FrozenString(node.getValue(), node.getCodeRange(), scope.getFile(), node.getLine()));
    }

    public Operand buildYield(Variable result, YieldNode node) {
        if (result == null) result = temp();
        if (scope instanceof IRScriptBody || scope instanceof IRModuleBody) throwSyntaxError(node.getLine(), "Invalid yield");

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

    public Variable as_fixnum(Operand value) {
        return addResultInstr(new AsFixnumInstr(temp(), value));
    }

    public Operand buildZArray(Variable result) {
       return copy(result, new Array());
    }

    public Operand buildZSuper(Variable result, ZSuperNode zsuperNode) {
        Operand block = setupCallClosure(zsuperNode.getIterNode());
        if (block == NullBlock.INSTANCE) block = getYieldClosureVariable();

        return scope instanceof IRMethod ? buildZSuper(result, block) : buildZSuperIfNest(result, block);
    }

    /*
     * Give the ability to print a debug message to stdout.  Not to ever be used outside
     * of debugging an issue with IR.
     */
    private void debug(String message, Operand... operands) {
        addInstr(new DebugOutputInstr(message, operands));
    }

    @Override
    boolean alwaysFalse(Node node) {
        return node.getNodeType().alwaysFalse();
    }

    @Override
    boolean alwaysTrue(Node node) {
        return node.getNodeType().alwaysTrue();
    }
}
