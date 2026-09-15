package org.jruby.prism.builder;

import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.EUCJPEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jcodings.specific.Windows_31JEncoding;
import org.jruby.ParseResult;
import org.jruby.Ruby;
import org.jruby.RubyBignum;
import org.jruby.RubyFloat;
import org.jruby.RubyKernel;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.compiler.NotCompilableException;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRManager;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRModuleBody;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScriptBody;
import org.jruby.ir.Tuple;
import org.jruby.ir.builder.IRBuilder;
import org.jruby.ir.builder.LazyMethodDefinition;
import org.jruby.ir.builder.StringStyle;
import org.jruby.ir.instructions.AsStringInstr;
import org.jruby.ir.instructions.AttrAssignInstr;
import org.jruby.ir.instructions.BNEInstr;
import org.jruby.ir.instructions.BNilInstr;
import org.jruby.ir.instructions.BlockGivenInstr;
import org.jruby.ir.instructions.BuildCompoundArrayInstr;
import org.jruby.ir.instructions.BuildSplatInstr;
import org.jruby.ir.instructions.CheckArityInstr;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.EQQInstr;
import org.jruby.ir.instructions.GetClassVariableInstr;
import org.jruby.ir.instructions.GetFieldInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.JumpInstr;
import org.jruby.ir.instructions.LabelInstr;
import org.jruby.ir.instructions.LoadImplicitClosureInstr;
import org.jruby.ir.instructions.MatchInstr;
import org.jruby.ir.instructions.PutClassVariableInstr;
import org.jruby.ir.instructions.PutConstInstr;
import org.jruby.ir.instructions.PutFieldInstr;
import org.jruby.ir.instructions.PutGlobalVarInstr;
import org.jruby.ir.instructions.RaiseRequiredKeywordArgumentError;
import org.jruby.ir.instructions.ReceiveKeywordArgInstr;
import org.jruby.ir.instructions.ReceiveKeywordRestArgInstr;
import org.jruby.ir.instructions.ReceiveKeywordsInstr;
import org.jruby.ir.instructions.ReceiveOptArgInstr;
import org.jruby.ir.instructions.ReceivePostReqdArgInstr;
import org.jruby.ir.instructions.ReceivePreReqdArgInstr;
import org.jruby.ir.instructions.ReceiveRestArgInstr;
import org.jruby.ir.instructions.ReifyClosureInstr;
import org.jruby.ir.instructions.ReqdArgMultipleAsgnInstr;
import org.jruby.ir.instructions.RestArgMultipleAsgnInstr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.instructions.RuntimeHelperCall;
import org.jruby.ir.instructions.SearchConstInstr;
import org.jruby.ir.instructions.SearchModuleForConstInstr;
import org.jruby.ir.instructions.SetCapturedVarInstr;
import org.jruby.ir.instructions.ToAryInstr;
import org.jruby.ir.instructions.YieldInstr;
import org.jruby.ir.instructions.defined.GetErrorInfoInstr;
import org.jruby.ir.instructions.defined.RestoreErrorInfoInstr;
import org.jruby.ir.operands.Array;
import org.jruby.ir.operands.Bignum;
import org.jruby.ir.operands.ChilledString;
import org.jruby.ir.operands.Complex;
import org.jruby.ir.operands.CurrentScope;
import org.jruby.ir.operands.Fixnum;
import org.jruby.ir.operands.Float;
import org.jruby.ir.operands.FrozenString;
import org.jruby.ir.operands.Hash;
import org.jruby.ir.operands.ImmutableLiteral;
import org.jruby.ir.operands.Integer;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.MutableString;
import org.jruby.ir.operands.NullBlock;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Rational;
import org.jruby.ir.operands.Regexp;
import org.jruby.ir.operands.Splat;
import org.jruby.ir.operands.Symbol;
import org.jruby.ir.operands.SymbolProc;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.parser.StaticScope;
import org.jruby.parser.StaticScopeFactory;
import org.jruby.prism.parser.ParseResultPrism;
import org.jruby.runtime.ArgumentDescriptor;
import org.jruby.runtime.ArgumentType;
import org.jruby.runtime.CallType;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.DefinedMessage;
import org.jruby.util.KCode;
import org.jruby.util.KeyValuePair;
import org.jruby.util.RegexpOptions;
import org.jruby.util.StringSupport;
import org.jruby.util.cli.Options;
import org.ruby_lang.prism.Nodes.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jruby.api.Convert.asSymbol;
import static org.jruby.ir.instructions.RuntimeHelperCall.Methods.HASH_CHECK;
import static org.jruby.ir.instructions.RuntimeHelperCall.Methods.IS_DEFINED_BACKREF;
import static org.jruby.ir.instructions.RuntimeHelperCall.Methods.IS_DEFINED_CALL;
import static org.jruby.ir.instructions.RuntimeHelperCall.Methods.IS_DEFINED_CONSTANT_OR_METHOD;
import static org.jruby.ir.instructions.RuntimeHelperCall.Methods.IS_DEFINED_METHOD;
import static org.jruby.ir.instructions.RuntimeHelperCall.Methods.IS_DEFINED_NTH_REF;
import static org.jruby.ir.instructions.RuntimeHelperCall.Methods.IS_DEFINED_SUPER;
import static org.jruby.ir.instructions.RuntimeHelperCall.Methods.IS_HASH_EMPTY;
import static org.jruby.ir.instructions.RuntimeHelperCall.Methods.MERGE_KWARGS;
import static org.jruby.runtime.CallType.VARIABLE;
import static org.jruby.runtime.ThreadContext.CALL_KEYWORD;
import static org.jruby.runtime.ThreadContext.CALL_KEYWORD_REST;
import static org.jruby.runtime.ThreadContext.CALL_SPLATS;
import static org.jruby.util.CommonByteLists.AMPERSAND_AMPERSAND;
import static org.jruby.util.CommonByteLists.EMPTY;
import static org.jruby.util.CommonByteLists.FWD_ALL;
import static org.jruby.util.CommonByteLists.FWD_BLOCK;
import static org.jruby.util.CommonByteLists.FWD_KWREST;
import static org.jruby.util.CommonByteLists.FWD_REST;
import static org.jruby.util.CommonByteLists.OR_OR;
import static org.jruby.util.CommonByteLists.STAR_STAR;
import static org.jruby.util.RubyStringBuilder.inspectIdentifierByteList;
import static org.jruby.util.RubyStringBuilder.str;
import static org.jruby.util.StringSupport.CR_UNKNOWN;

public class IRBuilderPrism extends IRBuilder<Node, DefNode, WhenNode, RescueNode, ConstantPathNode, HashPatternNode> {
    byte[] source;
    Source nodeSource;
    StaticScope staticScope;
    // This is originally sourced from ParseResult so that we only resolve symbols once per result instead of
    // one per scope.  There is a little dance to set this ... we keep getting it from the parent scope
    // unless it is a method definition then we get reference from the lazymethoddefinition.
    IdentityHashMap<byte[], RubySymbol> symbols = null;
    ThreadContext context;

    public IRBuilderPrism(IRManager manager, IRScope scope, IRBuilder parent, IRBuilder variableBuilder, Encoding encoding) {
        super(manager, scope, parent, variableBuilder, encoding);

        if (parent != null) {
            source = ((IRBuilderPrism) parent).source;
            nodeSource = ((IRBuilderPrism) parent).nodeSource;
            symbols = ((IRBuilderPrism) parent).symbols;
        } else { // For END which is made without a parent and not setup by lazy method def.
            symbols = new IdentityHashMap<>();
        }
        staticScope = scope.getStaticScope();
        staticScope.setFile(scope.getFile()); // staticScope and IRScope contain the same field.
        context = manager.getRuntime().getCurrentContext();
    }

    public void setSymbols(IdentityHashMap<byte[], RubySymbol> symbols) {
        this.symbols = symbols;
    }

    // FIXME: Delete once we are no longer depedent on source code
    public void setSourceFrom(IRBuilderPrism other) {
        if (other.nodeSource == null) throw new RuntimeException("WETF");
        this.nodeSource = other.nodeSource;
        this.source = other.source;
    }

    protected void hackPostExeSource(IRBuilder builder) {
        setSourceFrom((IRBuilderPrism) builder);
    }

    // FIXME: Delete once we are no longer depedent on source code
    public void setSourceFrom(Source nodeSource, byte[] source) {
        this.nodeSource = nodeSource;
        this.source = source;
    }

    @Override
    public Operand build(ParseResult result) {
        // FIXME: Missing support for executes once
        this.executesOnce = false;
        this.source = ((ParseResultPrism) result).getSource();
        this.nodeSource = ((ParseResultPrism) result).getSourceNode();
        this.symbols = ((ParseResultPrism) result).getSymbols();

//        System.out.println("NAME: " + fileName);
//        System.out.println(((ParseResultPrism) result).getRoot());

        return build(((ProgramNode) result.getAST()).statements);
    }

    protected Operand build(Node node) {
        return build(null, node);
    }

    /*
     * @param result preferred result variable (this reduces temp vars pinning values).
     * @param node to be built
     */
    protected Operand build(Variable result, Node node) {
        if (node == null) return nil();

        if (node.hasNewLineFlag()) determineIfWeNeedLineNumber(getLine(node), true, false, node instanceof DefNode);

        return switch (node) {
            case AliasGlobalVariableNode n -> buildAliasGlobalVariable(n);
            case AliasMethodNode n -> buildAliasMethod(n);
            case AndNode n -> buildAnd(n);
            case ArrayNode n -> buildArray(n);
            // MISSING: ArrayPatternNode
            case AssocSplatNode n -> buildAssocSplat(result, n);
            case BackReferenceReadNode n -> buildBackReferenceRead(result, n);
            case BeginNode n -> buildBegin(n);
            case BlockArgumentNode n -> buildBlockArgument(n);
            case BlockNode n -> buildBlock(n);
            // BlockParameterNode processed during call building.
            case BreakNode n -> buildBreak(n);
            case CallNode n -> buildCall(result, n, symbol(n.name));
            case CallAndWriteNode n -> buildCallAndWrite(n);
            case CallOrWriteNode n -> buildCallOrWrite(n);
            case CallOperatorWriteNode n -> buildCallOperatorWrite(n);
            case CaseNode n -> buildCase(n);
            case CaseMatchNode n -> buildCaseMatch(n);
            case ClassNode n -> buildClass(n);
            case ClassVariableAndWriteNode n -> buildClassAndVariableWrite(n);
            case ClassVariableOperatorWriteNode n -> buildClassVariableOperatorWrite(n);
            case ClassVariableOrWriteNode n -> buildClassOrVariableWrite(n);
            case ClassVariableReadNode n -> buildClassVariableRead(result, n);
            case ClassVariableWriteNode n -> buildClassVariableWrite(n);
            case ConstantAndWriteNode n -> buildConstantAndWrite(n);
            case ConstantOperatorWriteNode n -> buildConstantOperatorWrite(n);
            case ConstantOrWriteNode n -> buildConstantOrWrite(n);
            case ConstantPathNode n -> buildConstantPath(result, n);
            case ConstantPathAndWriteNode n -> buildConstantPathAndWrite(n);
            case ConstantPathOperatorWriteNode n -> buildConstantPathOperatorWrite(n);
            case ConstantPathOrWriteNode n -> buildConstantPathOrWrite(n);
            case ConstantPathWriteNode n -> buildConstantWritePath(n);
            // ConstantPathTargetNode processed in multiple assignment
            case ConstantReadNode n -> buildConstantRead(result, n);
            case ConstantWriteNode n -> buildConstantWrite(n);
            case DefNode n -> buildDef(n);
            case DefinedNode n -> buildDefined(n);
            case ElseNode n -> buildElse(n);
            case EmbeddedVariableNode n -> build(n.variable);
            // EmbeddedStatementsNode handle in interpolated processing
            // EnsureNode - covered by BeginNode and DefNode
            case ErrorRecoveryNode n -> buildErrorRecoveryNode(n);
            case FalseNode n -> fals();
            // MISSING: FindPatternNode
            case FloatNode n -> buildFloat(n);
            case FlipFlopNode n -> buildFlipFlop(n);
            case ForNode n -> buildFor(n);
            // ForwardingArgumentsNode, ForwardingParametersNode process by def and call sides respectively
            case ForwardingSuperNode n -> buildForwardingSuper(result, n);
            case GlobalVariableAndWriteNode n -> buildGlobalVariableAndWrite(n);
            case GlobalVariableOperatorWriteNode n -> buildGlobalVariableOperatorWrite(n);
            case GlobalVariableOrWriteNode n -> buildGlobalVariableOrWrite(n);
            case GlobalVariableReadNode n -> buildGlobalVariableRead(result, n);
            // GlobalVariableTargetNode processed by muliple assignment
            case GlobalVariableWriteNode n -> buildGlobalVariableWrite(n);
            case HashNode n -> buildHash(n.elements, containsVariableAssignment(n));
            case IfNode n -> buildIf(result, n);
            case ImaginaryNode n -> buildImaginary(n);
            case ImplicitNode n -> build(n.value);
            case IndexAndWriteNode n -> buildIndexAndWrite(n);
            case IndexOrWriteNode n -> buildIndexOrWrite(n);
            case IndexOperatorWriteNode n -> buildIndexOperatorWrite(n);
            case InstanceVariableAndWriteNode n -> buildInstanceVariableAndWrite(n);
            case InstanceVariableOperatorWriteNode n -> buildInstanceVariableOperatorWrite(n);
            case InstanceVariableOrWriteNode n -> buildInstanceVariableOrWrite(n);
            case InstanceVariableReadNode n -> buildInstanceVariableRead(n);
            // InstanceVariableTargetNode processed by multiple assignment
            case InstanceVariableWriteNode n -> buildInstanceVariableWrite(n);
            case IntegerNode n -> buildInteger(n);
            case InterpolatedMatchLastLineNode n -> buildInterpolatedMatchLastLine(result, n);
            case InterpolatedRegularExpressionNode n -> buildInterpolatedRegularExpression(result, n);
            case InterpolatedStringNode n -> buildInterpolatedString(result, n);
            case InterpolatedSymbolNode n -> buildInterpolatedSymbol(result, n);
            case InterpolatedXStringNode n -> buildInterpolatedXString(result, n);
            case ItLocalVariableReadNode n -> buildItRead();
            case KeywordHashNode n -> buildKeywordHash(n, new int[1]); // FIXME: we don't care about flags but this is odd (seems to only be for array syntax with kwrest?).
            // KeywordParameterNode, KeywordRestParameterNode processed by call
            case LambdaNode n -> buildLambda(n);
            case LocalVariableAndWriteNode n -> buildLocalAndVariableWrite(n);
            case LocalVariableOperatorWriteNode n -> buildLocalVariableOperatorWrite(n);
            case LocalVariableOrWriteNode n -> buildLocalOrVariableWrite(n);
            case LocalVariableReadNode n -> buildLocalVariableRead(n);
            // LocalVariableTargetNode processed by multiple assignment
            case LocalVariableWriteNode n -> buildLocalVariableWrite(n);
            case MatchLastLineNode n -> buildMatchLastLine(result, n);
            case MatchPredicateNode n -> buildMatchPredicate(n);
            case MatchRequiredNode n -> buildMatchRequired(n);
            case MatchWriteNode n -> buildMatchWrite(result, n);
            case ModuleNode n -> buildModule(n);
            // MultiTargetNode handled a few places internally
            case MultiWriteNode n -> buildMultiWriteOrTargetNode(n.lefts, n.rest, n.rights, n.value);
            case NextNode n -> buildNext(n);
            case NilNode n -> nil();
            // NoKeywordsParameterNode processed by def
            case NumberedReferenceReadNode n -> buildNumberedReferenceRead(n);
            // OptionalParameterNode processed by def
            case OrNode n -> buildOr(n);
            // ParametersNode processed by def
            case ParenthesesNode n -> build(n.body);
            case PinnedExpressionNode n -> build(n.expression);
            case PinnedVariableNode n -> build(n.variable);
            case PostExecutionNode n -> buildPostExecution(n);
            case PreExecutionNode n -> buildPreExecution(n);
            case ProgramNode n -> buildProgram(n);
            case RangeNode n -> buildRange(n);
            case RationalNode n -> buildRational(n);
            case RedoNode n -> buildRedo(n);
            case RegularExpressionNode n -> buildRegularExpression(n);
            // RequiredDestructuredParamterNode, RequiredParameterNode processed by def
            case RescueModifierNode n -> buildRescueModifier(n);
            // RescueNode handled by begin
            // RestParameterNode handled by def
            case RetryNode n -> buildRetry(n);
            case ReturnNode n -> buildReturn(n);
            case SelfNode n -> buildSelf();
            case ShareableConstantNode n -> buildShareableConstant(n);
            case SingletonClassNode n -> buildSingletonClass(n);
            case SourceEncodingNode n -> buildSourceEncoding();
            case SourceFileNode n -> buildSourceFile();
            case SourceLineNode n -> buildSourceLine(node);
            case SplatNode n -> buildSplat(n);
            case StatementsNode n -> buildStatements(n);
            case StringNode n -> buildString(n);
            case SuperNode n -> buildSuper(result, n);
            case SymbolNode n -> buildSymbol(n);
            case TrueNode n -> tru();
            case UndefNode n -> buildUndef(n);
            case UnlessNode n -> buildUnless(result, n);
            case UntilNode n -> buildUntil(n);
            // WhenNode processed by case
            case WhileNode n -> buildWhile(n);
            case XStringNode n -> buildXString(result, n);
            case YieldNode n -> buildYield(result, n);
            default -> throw new RuntimeException("Unhandled Node type: " + node);
        };
    }

    private Operand buildCallOperatorWrite(CallOperatorWriteNode node) {
        return buildOpAsgn(node.receiver, node.value, symbol(node.read_name), symbol(node.write_name), symbol(node.binary_operator), node.isSafeNavigation());
    }

    private Operand buildImaginary(ImaginaryNode node) {
        return new Complex((ImmutableLiteral) build(node.numeric));
    }

    private Operand buildAliasGlobalVariable(AliasGlobalVariableNode node) {
        return buildVAlias(globalVariableName(node.new_name), globalVariableName(node.old_name));
    }

    private Operand buildAliasMethod(AliasMethodNode node) {
        return buildAlias(build(node.new_name), build(node.old_name));
    }

    private Operand buildAnd(AndNode node) {
        return buildAnd(build(node.left), () -> build(node.right), binaryType(node.left));
    }

    private Operand[] buildArguments(ArgumentsNode node) {
        return node == null ? Operand.EMPTY_ARRAY : buildNodeList(node.arguments);
    }

    private Operand buildAssocSplat(Variable result, AssocSplatNode node) {
        return build(result, node.value);
    }

    private Operand buildArray(ArrayNode node) {
        Node[] children = node.elements;
        Operand[] elts = new Operand[children.length];
        Variable result = temp();
        int splatIndex = -1;
        Operand keywordRestSplat = null;

        for (int i = 0; i < children.length; i++) {
            Node child = children[i];

            if (child instanceof SplatNode splat) {
                int length = i - splatIndex - 1;

                if (length == 0) {
                    // FIXME: This is wasteful to force this all through argscat+empty array
                    if (splatIndex == -1) copy(result, new Array());
                    if (splat.expression instanceof NilNode) {
                        copy(result, new Array());
                    } else {
                        addResultInstr(new BuildCompoundArrayInstr(result, result, build(splat.expression), false, false));
                    }
                } else {
                    Operand[] lhs = new Operand[length];
                    System.arraycopy(elts, splatIndex + 1, lhs, 0, length);

                    // no actual splat until now.
                    if (splatIndex == -1) {
                        copy(result, new Array(lhs));
                    } else {
                        addResultInstr(new BuildCompoundArrayInstr(result, result, new Array(lhs), false, false));
                    }

                    addResultInstr(new BuildCompoundArrayInstr(result, result, build(splat.expression), false, false));
                }
                splatIndex = i;
            } else if (child instanceof KeywordHashNode) {
                keywordRestSplat = build(child);
                elts[i] = keywordRestSplat;
            } else {
                elts[i] = build(child);
            }
        }

        if (keywordRestSplat != null) {
            Variable test = addResultInstr(new RuntimeHelperCall(temp(), IS_HASH_EMPTY, new Operand[]{ keywordRestSplat }));
            if_else(test, tru(),
                    () -> copy(result, new Array(removeArg(elts))),
                    () -> copy(result, new Array(elts)));
            return result;
        }
        // FIXME: Can we just return the operand only in this case.
        // No splats present.  Just make a simple array Operand.
        if (splatIndex == -1) return copy(result, new Array(elts));

        int length = children.length - splatIndex - 1;
        if (length > 0) {
            Operand[] rhs = new Operand[length];
            System.arraycopy(elts, splatIndex + 1, rhs, 0, length);

            addResultInstr(new BuildCompoundArrayInstr(result, result, new Array(rhs), false, false));
        }

        return result;
    }

    private Node[] argumentsFrom(ArgumentsNode node) {
        return node == null ? Node.EMPTY_ARRAY : node.arguments;
    }

    // This method is called to build assignments for a multiple-assignment instruction
    protected void buildAssignment(Node node, Operand rhsVal) {
        if (node == null) return; // case of 'a, = something'

        switch (node) {
            case CallTargetNode call -> buildCallTarget(call, build(call.receiver), rhsVal);
            case IndexTargetNode index -> buildAttrAssignAssignment(index.receiver, symbol("[]="),
                    argumentsFrom(index.arguments), rhsVal);
            case ClassVariableTargetNode cvar ->
                    addInstr(new PutClassVariableInstr(classVarDefinitionContainer(), symbol(cvar.name), rhsVal));
            case ConstantPathTargetNode cpath ->
                    addInstr(new PutConstInstr(buildModuleParent(cpath.parent), symbol(cpath.name), rhsVal));
            case ConstantTargetNode constant ->
                    addInstr(new PutConstInstr(getCurrentModuleVariable(), symbol(constant.name), rhsVal));
            case LocalVariableTargetNode lvar -> copy(getLocalVariable(symbol(lvar.name), lvar.depth), rhsVal);
            case GlobalVariableTargetNode gvar -> addInstr(new PutGlobalVarInstr(symbol(gvar.name), rhsVal));
            case InstanceVariableTargetNode ivar -> addInstr(new PutFieldInstr(buildSelf(), symbol(ivar.name), rhsVal));
            case MultiTargetNode multi -> {
                Variable rhs = addResultInstr(new ToAryInstr(temp(), rhsVal));

                Map<Node, Operand> reads = new HashMap<>();
                final List<Tuple<Node, ResultInstr>> assigns = new ArrayList<>(4);
                buildMultiAssignment(multi.lefts, multi.rest, multi.rights, rhs, reads, assigns);

                for (Tuple<Node, ResultInstr> assign : assigns) {
                    addInstr((Instr) assign.b);
                }

                buildAssignment(reads, assigns);
            }
            case RequiredParameterNode variable -> copy(getLocalVariable(symbol(variable.name), 0), rhsVal);
            case SplatNode _splat -> buildSplat(rhsVal);
            default -> throw notCompilable("Can't build assignment node", node);
        }
    }

    private void buildCallTarget(CallTargetNode call, Operand receiver, Operand rhsVal) {
        CallType callType = call.isIgnoreVisibility() ? CallType.FUNCTIONAL : CallType.NORMAL;
        if (call.isSafeNavigation()) {
            if_not(receiver, nil(),
                    () -> _call(temp(), callType, receiver, symbol(call.name), rhsVal));
        } else {
            _call(temp(), callType, receiver, symbol(call.name), rhsVal);
        }
    }

    @Override
    protected Operand[] buildAttrAssignCallArgs(Node argsNode, Operand[] rhs, boolean containsAssignment) {
        Operand[] args = buildCallArgs(argsNode, new int[] { 0 });
        rhs[0] = args[args.length - 1];
        return args;
    }

    public Operand buildAttrAssignAssignment(Node receiver, RubySymbol name, Node[] arguments, Operand value) {
        Operand obj = build(receiver);
        int[] flags = new int[] { 0 };
        Operand[] args = buildCallArgsArray(arguments, flags);
        args = addArg(args, value);
        addInstr(AttrAssignInstr.create(scope, obj, name, args, flags[0], scope.maybeUsingRefinements()));
        return value;
    }
    
    // FIXME(feature): optimization simplifying this from other globals
    private Operand buildBackReferenceRead(Variable result, BackReferenceReadNode node) {
        return buildGlobalVar(result, symbol(node.name));
    }

    private Operand buildBreak(BreakNode node) {
        return buildBreak(() -> node.arguments == null ? nil() : buildYieldArgs(node.arguments.arguments, new int[1]), getLine(node));
    }

    private Operand buildBegin(BeginNode node) {
        if (node.rescue_clause != null) {
            RescueNode rescue = node.rescue_clause;
            Node ensureBody = node.ensure_clause != null ? node.ensure_clause.statements : null;
            return buildEnsureInternal(node.statements, node.else_clause, rescue.exceptions, rescue.statements,
                    rescue.subsequent, false, ensureBody, true, rescue.reference);
        } else if (node.ensure_clause != null) {
            EnsureNode ensure = node.ensure_clause;
            return buildEnsureInternal(node.statements, null, null, null, null, false, ensure.statements, false, null);
        }
        return build(node.statements);
    }

    private Operand buildBlock(BlockNode node) {
        StaticScope staticScope = createStaticScopeFrom(node.locals, StaticScope.Type.BLOCK);
        markImplicitVariables(staticScope, node.parameters);
        Signature signature = calculateSignature(node.parameters);
        staticScope.setSignature(signature);
        return buildIter(node.parameters, node.body, staticScope, signature, getLine(node), getEndLine(node));
    }

    private void markImplicitVariables(StaticScope staticScope, Node parameters) {
        if (parameters instanceof NumberedParametersNode num) markImplcitNums(staticScope, num.maximum);
    }

    private void markImplcitNums(StaticScope staticScope, int maximum) {
        for (int i = 0; i < maximum; i++) {
            int slot = staticScope.exists("_" + (i + 1));
            staticScope.markImplicitVariable(slot & 0xffff);
        }
    }

    private void markIt(StaticScope staticScope) {
        int slot = staticScope.exists("it");
        staticScope.markImplicitVariable(slot & 0xffff);
    }

    protected Variable receiveBlockArg(Variable v, Operand argsArray, int argIndex, boolean isSplat) {
        if (argsArray != null) {
            // We are in a nested receive situation -- when we are not at the root of a masgn tree
            // Ex: We are trying to receive (b,c) in this example: "|a, (b,c), d| = ..."
            addInstr(new RestArgMultipleAsgnInstr(v, argsArray, argIndex));
        } else {
            // argsArray can be null when the first node in the args-node-ast is a multiple-assignment
            // For example, for-nodes
            // FIXME: We can have keywords here but this is more complicated to get here
            Variable keywords = copy(UndefinedValue.UNDEFINED);
            addInstr(isSplat ? new ReceiveRestArgInstr(v, keywords, argIndex, argIndex) : new ReceivePreReqdArgInstr(v, keywords, argIndex));
        }

        return v;
    }

    @Override
    protected void receiveForArgs(Node node) {
        Variable keywords = copy(temp(), UndefinedValue.UNDEFINED);

        // FIXME: I think this should rip out receivePre and stop sharingh with method defs
        // FIXME: pattern of use seems to be recv pre then assign value so this may be done with less if (or at least through separate methods between value and receiving the value from args).
        if (node instanceof MultiTargetNode) { // for loops
            buildBlockArgsAssignment(node, null, 0, false);
        } else if (node instanceof ClassVariableTargetNode || node instanceof LocalVariableTargetNode ||
                node instanceof InstanceVariableTargetNode || node instanceof ConstantTargetNode ||
                node instanceof GlobalVariableTargetNode || node instanceof CallTargetNode ||
                node instanceof IndexTargetNode) {
            receivePreArg(node, keywords, 0);
        } else {
            throw notCompilable("missing arg processing for `for`", node);
        }
    }

    private ArgumentDescriptor[] parametersToArgumentDescriptors(NumberedParametersNode node) {
        ArgumentDescriptor[] descriptors = new ArgumentDescriptor[node.maximum];

        for (int i = 0; i < node.maximum; i++) {
            descriptors[i] = new ArgumentDescriptor(ArgumentType.req, symbol("_" + (i + 1)));
        }

        return descriptors;
    }

    public void receiveBlockArgs(Node node) {
        if (node == null) return;

        if (node instanceof NumberedParametersNode params) {
            ((IRClosure) scope).setArgumentDescriptors(parametersToArgumentDescriptors(params));
            Variable keywords = addResultInstr(new ReceiveKeywordsInstr(temp(), true, true));

            for (int i = 0; i < params.maximum; i++) {
                RubySymbol name = symbol("_" + (i + 1));
                addInstr(new ReceivePreReqdArgInstr(argumentResult(name), keywords, i));
            }
            addInstr(new CheckArityInstr(params.maximum, 0, false, params.maximum, keywords));
        } else if (node instanceof ItParametersNode) {
            Variable keywords = addResultInstr(new ReceiveKeywordsInstr(temp(), true, true));
            Variable v = getLocalVariable(symbol("it"), 0);
            markIt(staticScope);
            ((IRClosure) scope).setArgumentDescriptors(new ArgumentDescriptor[] { new ArgumentDescriptor(ArgumentType.anonreq) });
            addInstr(new ReceivePreReqdArgInstr(v, keywords, 0));
        } else if (node instanceof BlockParametersNode params) {
            // FIXME: Missing locals?  Not sure how we handle those but I would have thought with a scope?
            buildParameters(params.parameters);
            ((IRClosure) scope).setArgumentDescriptors(createArgumentDescriptor());
        } else {
            throw notCompilable("missing arg processing for blocks", node);
        }
    }

    private void buildBlockArgsAssignment(Node node, Operand argsArray, int argIndex, boolean isSplat) {
        if (node instanceof CallNode call) { // attribute assignment: a[0], b = 1, 2
            buildAttrAssignAssignment(call.receiver, symbol((call.name)), call.arguments.arguments,
                    receiveBlockArg(temp(), argsArray, argIndex, isSplat));
        } else if (node instanceof LocalVariableTargetNode lvar) {
            receiveBlockArg(getLocalVariable(symbol((lvar.name)), lvar.depth), argsArray, argIndex, isSplat);
        } else if (node instanceof ClassVariableTargetNode cvar) {
            addInstr(new PutClassVariableInstr(classVarDefinitionContainer(), symbol((cvar.name)),
                    receiveBlockArg(temp(), argsArray, argIndex, isSplat)));
        } else if (node instanceof ConstantTargetNode constant) {
            putConstant(symbol((constant.name)), receiveBlockArg(temp(), argsArray, argIndex, isSplat));
        } else if (node instanceof GlobalVariableTargetNode gvar) {
            addInstr(new PutGlobalVarInstr(symbol((gvar.name)), receiveBlockArg(temp(), argsArray, argIndex, isSplat)));
        } else if (node instanceof InstanceVariableTargetNode ivar) {
            addInstr(new PutFieldInstr(buildSelf(), symbol((ivar.name)), receiveBlockArg(temp(), argsArray, argIndex, isSplat)));
        } else if (node instanceof MultiTargetNode multi) {
            for (int i = 0; i < multi.lefts.length; i++) {
                buildBlockArgsAssignment(multi.lefts[i], null, i, false);
            }

            int postIndex = multi.lefts.length;
            if (multi.rest != null) {
                buildBlockArgsAssignment(multi.rest, null, postIndex, true);
                postIndex++;
            }
            for (int i = 0; i < multi.rights.length; i++) {
                buildBlockArgsAssignment(multi.rights[i], null, postIndex + i, false);
            }
        } else if (node instanceof SplatNode) {
            // FIXME: we don't work in legacy either?
        } else if (node instanceof ImplicitRestNode) {
        } else {
            throw notCompilable("Can't build assignment node", node);
        }
    }

    private Operand buildBlockArgument(BlockArgumentNode node) {
        if (node.expression instanceof SymbolNode sym && !scope.maybeUsingRefinements()) {
            return new SymbolProc(symbol(sym));
        } else if (node.expression == null) {
            return getYieldClosureVariable();
        }
        return build(node.expression);
    }

    private Operand buildCall(Variable resultArg, CallNode node, RubySymbol name) {
        return buildCall(resultArg, node, name, null, null);
    }

    protected Operand buildLazyWithOrder(CallNode node, Label lazyLabel, Label endLabel, boolean preserveOrder) {
        Operand value = buildCall(null, node, symbol((node.name)), lazyLabel, endLabel);

        // FIXME: missing !(value instanceof ImmutableLiteral) which will force more copy instr
        // We need to preserve order in cases (like in presence of assignments) except that immutable
        // literals can never change value so we can still emit these out of order.
        return preserveOrder ? copy(value) : value;
    }

    // FIXME: This would be nice to combine in some form with AST side but it requires some pre-processing since prism merged all call types into a single node.
    // We do name processing outside of this rather than from the node to support stripping '=' off of opelasgns
    private Operand buildCall(Variable resultArg, CallNode node, RubySymbol name, Label lazyLabel, Label endLabel) {
        Variable result = resultArg == null ? temp() : resultArg;

        if (node.isVariableCall()) return _call(result, VARIABLE, buildSelf(), name);

        CallType callType = determineCallType(node.receiver);
        String id = name.idString();

        if (node.isAttributeWrite()) return buildAttrAssign(result, node.receiver, node.arguments, node.block, symbol((node.name)), node.isSafeNavigation(), containsVariableAssignment(node));

        if (callType != CallType.FUNCTIONAL && Options.IR_STRING_FREEZE.load()) {
            // Frozen string optimization: check for "string".freeze
            if (node.receiver instanceof StringNode receiver && (id.equals("freeze") || id.equals("-@"))) {
                return new FrozenString(bytelistFrom(receiver), CR_UNKNOWN, scope.getFile(), getLine(receiver));
            }
        }

        boolean reusingLabels = false;
        if (node.isSafeNavigation()) {
            if (lazyLabel == null) {
                lazyLabel = getNewLabel();
                endLabel = getNewLabel();
            } else {
                reusingLabels = true;
            }
        }

        // The receiver has to be built *before* call arguments are built
        // to preserve expected code execution order
        boolean preserveOrder = containsVariableAssignment(node.receiver);
        Operand receiver;
        if (callType == CallType.FUNCTIONAL) {
            receiver = buildSelf();
        } else if (node.receiver instanceof CallNode recv && recv.isSafeNavigation()) {
            receiver = buildLazyWithOrder(recv, lazyLabel, endLabel, preserveOrder);
        } else {
            receiver = buildWithOrder(node.receiver, preserveOrder);
        }

        if (node.isSafeNavigation()) addInstr(new BNilInstr(lazyLabel, receiver));

        // FIXME: Missing arrayderef opti logic

        createCall(result, receiver, callType, name, node.arguments, node.block, getLine(node), node.hasNewLineFlag());

        if (node.isSafeNavigation()) {
            addInstr(new JumpInstr(endLabel));
            if (!reusingLabels) { // This already exists.
                addInstr(new LabelInstr(lazyLabel));
                addInstr(new CopyInstr(result, nil()));
                addInstr(new LabelInstr(endLabel));
            }
        }

        return result;
    }

    private Operand buildCallAndWrite(CallAndWriteNode node) {
        return buildOpAsgn(node.receiver, node.value, symbol(node.read_name), symbol(node.write_name), symbol(AMPERSAND_AMPERSAND), node.isSafeNavigation());
    }

    @Override
    protected Operand[] buildCallArgs(Node args, int[] flags) {
        return buildCallArgsArray(((ArgumentsNode) args).arguments, flags);

    }
    protected Operand[] buildCallArgsArray(Node[] children, int[] flags) {
        int numberOfArgs = children.length;
        // FIXME: hack
        if (numberOfArgs > 0 && children[numberOfArgs - 1] instanceof BlockArgumentNode) {
            Node[] temp = children;
            numberOfArgs--;
            children = new Node[numberOfArgs];
            System.arraycopy(temp, 0, children, 0, numberOfArgs);
        }
        Operand[] builtArgs = new Operand[numberOfArgs];
        boolean hasAssignments = containsVariableAssignment(children);

        for (int i = 0; i < numberOfArgs; i++) {
            Node child = children[i];

            if (child instanceof SplatNode splat) {
                if (splat.expression != null) {
                    flags[0] |= CALL_SPLATS;
                    builtArgs[i] = new Splat(addResultInstr(new BuildSplatInstr(temp(), build(splat), true)));
                } else {
                    var lvar = getLocalVariable(symbol("*"), scope.getStaticScope().isDefined("*"));
                    builtArgs[i] = new Splat(lvar);
                }
            } else if (child instanceof KeywordHashNode hash && i == numberOfArgs - 1) {
                builtArgs[i] = buildCallKeywordArguments(hash, flags); // FIXME: here and possibly AST make isKeywordsHash() method.
            } else if (child instanceof ForwardingArgumentsNode) {
                Operand rest = buildSplat(getLocalVariable(symbol(FWD_REST), scope.getStaticScope().isDefined("*")));
                Operand kwRest = getLocalVariable(symbol(FWD_KWREST), scope.getStaticScope().isDefined("**"));
                Variable check = addResultInstr(new RuntimeHelperCall(temp(), HASH_CHECK, new Operand[] { kwRest }));
                Variable ary = addResultInstr(new BuildCompoundArrayInstr(temp(), rest, check, true, true));
                builtArgs[i] = new Splat(buildSplat(ary));
                flags[0] |= CALL_KEYWORD | CALL_KEYWORD_REST | CALL_SPLATS;
            } else {
                builtArgs[i] = buildWithOrder(children[i], hasAssignments);
            }
        }

        return builtArgs;
    }

    protected Operand buildCallKeywordArguments(KeywordHashNode node, int[] flags) {
        flags[0] |= CALL_KEYWORD;

        if (hasOnlyRestKwargs(node.elements)) return buildRestKeywordArgs(node, flags);

        return buildHash(node.elements, containsVariableAssignment(node.elements));
    }

    private Operand buildCallOrWrite(CallOrWriteNode node) {
        return buildOpAsgn(node.receiver, node.value, symbol(node.read_name), symbol(node.write_name), symbol(OR_OR), node.isSafeNavigation());
    }

    private Operand buildCase(CaseNode node) {
        return buildCase(node.predicate, node.conditions, node.else_clause);
    }

    private Operand buildCaseMatch(CaseMatchNode node) {
        return buildPatternCase(node.predicate, node.conditions, node.else_clause);
    }

    private Operand buildClass(ClassNode node) {
        return buildClass(determineBaseName(node.constant_path), node.superclass, node.constant_path,
                node.body, createStaticScopeFrom(node.locals, StaticScope.Type.LOCAL), getLine(node), getEndLine(node));
    }

    private Operand buildClassVariableOperatorWrite(ClassVariableOperatorWriteNode node) {
        Operand lhs = buildClassVar(temp(), symbol(node.name));
        Operand rhs = build(node.value);
        Variable value = call(temp(), lhs, symbol(node.binary_operator), rhs);
        addInstr(new PutClassVariableInstr(classVarDefinitionContainer(), symbol(node.name), value));
        return value;
    }

    private Operand buildInstanceVariableOperatorWrite(InstanceVariableOperatorWriteNode node) {
        Operand lhs = buildInstVar(symbol(node.name));
        Operand rhs = build(node.value);
        Variable value = call(temp(), lhs, symbol(node.binary_operator), rhs);
        addInstr(new PutFieldInstr(buildSelf(), symbol(node.name), value));
        return value;
    }

    private Operand buildLocalVariableOperatorWrite(LocalVariableOperatorWriteNode node) {
        int depth = staticScope.isDefined(symbol(node.name).idString()) >> 16;
        Variable lhs = getLocalVariable(symbol(node.name), depth);
        Operand rhs = build(node.value);
        Variable value = call(lhs, lhs, symbol(node.binary_operator), rhs);
        return value;
    }

    private Operand buildClassAndVariableWrite(ClassVariableAndWriteNode node) {
        return buildOpAsgnAnd(() -> addResultInstr(new GetClassVariableInstr(temp(), classVarDefinitionContainer(), symbol(node.name))),
                () -> (buildClassVarAsgn(symbol(node.name), node.value)));
    }

    private Operand buildClassOrVariableWrite(ClassVariableOrWriteNode node) {
        return buildOpAsgnOrWithDefined(node,
                (result) -> addInstr(new GetClassVariableInstr((Variable) result, classVarDefinitionContainer(), symbol(node.name))),
                () -> (buildClassVarAsgn(symbol(node.name), node.value)));
    }

    private Operand buildClassVariableRead(Variable result, ClassVariableReadNode node) {
        return buildClassVar(result, symbol(node.name));
    }

    private Operand buildClassVariableWrite(ClassVariableWriteNode node) {
        return buildClassVarAsgn(symbol(node.name), node.value);
    }

    @Override
    protected Operand buildColon2ForConstAsgnDeclNode(Node lhs, Variable valueResult, boolean constMissing) {
        Variable leftModule = temp();
        ConstantPathNode colon2Node = (ConstantPathNode) lhs;
        RubySymbol name = symbol(determineBaseName(colon2Node));
        Operand leftValue;
        if (colon2Node.parent == null)  {
            leftValue = getManager().getObjectClass();   // ::Foo
        } else {
            leftValue = build(colon2Node.parent);
        }
        // FIXME: ::Foo should be able to eliminate this variable reference
        copy(leftModule, leftValue);
        addInstr(new SearchModuleForConstInstr(valueResult, leftModule, name, false, constMissing));

        return leftModule;
    }

    private Operand buildConstantAndWrite(ConstantAndWriteNode node) {
        return buildOpAsgnAnd(() -> addResultInstr(new SearchConstInstr(temp(), CurrentScope.INSTANCE, symbol(node.name), false)),
                () -> (putConstant(symbol(node.name), build(node.value))));
    }

    private Operand buildConstantOperatorWrite(ConstantOperatorWriteNode node) {
        Operand lhs = searchConst(temp(), symbol(node.name));
        Operand rhs = build(node.value);
        Variable value = call(temp(), lhs, symbol(node.binary_operator), rhs);
        putConstant(buildSelf(), symbol(node.name), value);
        return value;
    }

    private Operand buildConstantOrWrite(ConstantOrWriteNode node) {
        return buildOpAsgnOrWithDefined(node,
                (result) -> addInstr(new SearchConstInstr((Variable) result, CurrentScope.INSTANCE, symbol(node.name), false)),
                () -> (putConstant(symbol(node.name), build(node.value))));
    }

    private Operand buildConstantOrWritePath(ConstantPathOrWriteNode node) {
        // FIXME: unify with AST
        RubySymbol name = symbol(((ConstantPathNode) node.target).name);
        Variable result = temp();
        Label falseCheck = getNewLabel();
        Label done = getNewLabel();
        Label assign = getNewLabel();
        // FIXME: this is semi-duplicated from buildConstantPath since we want out param of module and value returned to result
        Operand module = node.target.parent == null ? getManager().getObjectClass() : build(node.target.parent);
        searchModuleForConstNoFrills(result, module, name);
        addInstr(BNEInstr.create(falseCheck, result, UndefinedValue.UNDEFINED));
        addInstr(new JumpInstr(assign));
        addInstr(new LabelInstr(falseCheck));
        addInstr(BNEInstr.create(done, result, fals()));
        addInstr(new LabelInstr(assign));
        Operand rhsValue = build(node.value);
        copy(result, rhsValue);
        addInstr(new PutConstInstr(module, name, rhsValue));
        addInstr(new LabelInstr(done));
        return result;
    }

    private Operand buildConstantPath(Variable result, ConstantPathNode node) {
        return buildConstantPath(result, symbol(node.name), node.parent);
    }

    private Operand buildConstantPath(Variable result, RubySymbol name, Node parent) {
        Operand where = parent == null ? getManager().getObjectClass() : build(parent);
        return searchModuleForConst(result, where, name);
    }

    private Operand buildConstantPathAndWrite(ConstantPathAndWriteNode node) {
        return buildOpAsgnConstDeclAnd(node.target, node.value, symbol(determineBaseName(node.target)));
    }

    private Operand buildConstantPathOperatorWrite(ConstantPathOperatorWriteNode node) {
        var path = node.target;

        if (path.parent == null) {
            return buildOpAsgnConstDecl(node.target, node.value, symbol(node.binary_operator));
        } else {
            Operand parent = build(path.parent);
            Operand lhs = searchModuleForConst(temp(), parent, symbol(path.name));
            Operand rhs = build(node.value);
            Variable result = call(temp(), lhs, symbol(node.binary_operator), rhs);
            return copy(temp(), putConstant(parent, symbol(path.name), result));
        }
    }

    private Operand buildConstantPathOrWrite(ConstantPathOrWriteNode node) {
        return buildOpAsgnConstDeclOr(node.target, node.value, symbol(determineBaseName(node.target)));
    }

    private Operand buildConstantRead(Variable result, ConstantReadNode node) {
        return searchConst(result, symbol(node.name));
    }

    private Operand buildConstantWrite(ConstantWriteNode node) {
        return putConstant(symbol(node.name), build(node.value));
    }

    private Operand buildConstantWritePath(ConstantPathWriteNode node) {
        var path = node.target;
        return putConstant(buildModuleParent(path.parent), symbol(path.name), build(node.value));
    }

    private Operand buildDef(DefNode node) {
        // FIXME: due to how lazy methods work we need this set on method before we actually parse the method.
        StaticScope staticScope = createStaticScopeFrom(node.locals, StaticScope.Type.LOCAL);
        staticScope.setSignature(calculateSignature(node.parameters));
        LazyMethodDefinition def = new LazyMethodDefinitionPrism(getManager().getRuntime(), source, nodeSource, encoding, node, symbols);

        if (node.receiver == null) {
            return buildDefn(defineNewMethod(def, symbol(node.name).getBytes(), getLine(node), staticScope, true));
        } else {
            return buildDefs(node.receiver, defineNewMethod(def, symbol(node.name).getBytes(), getLine(node), staticScope, false));
        }
    }

    private Operand buildDefined(DefinedNode node) {
        return buildGetDefinition(node.value);
    }

    private Operand buildElse(ElseNode node) {
        return buildStatements(node.statements);
    }

    private Operand buildErrorRecoveryNode(ErrorRecoveryNode node) {
        System.out.println("uh oh");
        return nil();
    }

    private Operand buildFlipFlop(FlipFlopNode node) {
        return buildFlip(node.left, node.right, node.isExcludeEnd());
    }

    // FIXME: Do we need warn or will YARP provide it.
    private Operand buildFloat(FloatNode node) {
        String number = bytelistFrom(node).toString();

        // FIXME: This is very expensive but numeric values is still be decided in Prism.
        IRubyObject fl = RubyKernel.new_float(getManager().getRuntime().getCurrentContext(),
                getManager().getRuntime().newString(number), false);

        return new Float(((RubyFloat) fl).getDoubleValue());
    }

    private Operand buildFor(ForNode node) {
        return buildFor(node.collection, node.index, node.statements, scope.getStaticScope(),
                calculateSignatureFor(node.index), getLine(node), getEndLine(node));
    }

    private Operand buildForwardingSuper(Variable result, ForwardingSuperNode node) {
        return buildZSuper(result, node.block);
    }

    public Operand buildGetArgumentDefinition(final ArgumentsNode node, String type) {
        if (node == null) return new MutableString(type);

        Operand rv = new FrozenString(type);
        boolean failPathReqd = false;
        Label failLabel = getNewLabel();
        for(Node arg: node.arguments) {
            Operand def = buildGetDefinition(arg);
            if (def == nil()) { // Optimization!
                rv = nil();
                break;
            } else if (!def.hasKnownValue()) { // Optimization!
                failPathReqd = true;
                addInstr(createBranch(def, nil(), failLabel));
            }
        }

        return failPathReqd ? buildDefnCheckIfThenPaths(failLabel, rv) : rv;

    }

    // FIXME: implementation of @@a ||= 1 uses getDefinition to determine it is defined but defined?(@@a ||= 1) is
    // always defined as "assignment".
    protected Operand buildGetDefinition2(Node node) {
        if (node instanceof ClassVariableOrWriteNode cvar) return buildClassVarGetDefinition(symbol(cvar.name));
        if (node instanceof GlobalVariableOrWriteNode gvar) return buildGlobalVarGetDefinition(symbol(gvar.name));
        if (node instanceof ConstantOrWriteNode constant) return buildConstantGetDefinition(symbol(constant.name));

        return buildGetDefinition(node);
    }
    @Override
    protected Operand buildGetDefinition(Node node) {
        if (node == null) return new FrozenString("expression");

        if (node instanceof ClassVariableWriteNode ||
                node instanceof ClassVariableAndWriteNode ||
                node instanceof ClassVariableOperatorWriteNode || node instanceof ClassVariableOrWriteNode ||
                node instanceof ConstantAndWriteNode || node instanceof ConstantOrWriteNode ||
                node instanceof ConstantPathWriteNode || node instanceof ConstantWriteNode ||
                node instanceof ConstantPathAndWriteNode ||node instanceof ConstantPathOrWriteNode ||
                node instanceof ConstantPathOperatorWriteNode ||
                node instanceof InstanceVariableAndWriteNode || node instanceof InstanceVariableOrWriteNode ||
                node instanceof InstanceVariableOperatorWriteNode ||
                node instanceof LocalVariableWriteNode ||
                node instanceof LocalVariableAndWriteNode || node instanceof LocalVariableOrWriteNode ||
                node instanceof LocalVariableOperatorWriteNode || node instanceof ConstantOperatorWriteNode ||
                node instanceof GlobalVariableOrWriteNode || node instanceof GlobalVariableAndWriteNode ||
                node instanceof GlobalVariableWriteNode || node instanceof GlobalVariableOperatorWriteNode ||
                node instanceof MultiWriteNode ||
                node instanceof InstanceVariableWriteNode || node instanceof IndexAndWriteNode ||
                node instanceof IndexOrWriteNode || node instanceof IndexOperatorWriteNode ||
                node instanceof CallAndWriteNode || node instanceof CallOrWriteNode ||
                node instanceof CallOperatorWriteNode) {
            return new FrozenString(DefinedMessage.ASSIGNMENT.getText());
        } else if (node instanceof OrNode || node instanceof AndNode ||
                node instanceof InterpolatedRegularExpressionNode || node instanceof InterpolatedStringNode) {
            return new FrozenString(DefinedMessage.EXPRESSION.getText());
        } else if (node instanceof ParenthesesNode parens) {
            if (parens.body instanceof StatementsNode body) {
                StatementsNode statements = body;
                switch (statements.body.length) {
                    case 0: return nil();
                    case 1: return buildGetDefinition(statements.body[0]);
                }
            }

            return new FrozenString(DefinedMessage.EXPRESSION.getText());
        } else if (node instanceof FalseNode) {
            return new FrozenString(DefinedMessage.FALSE.getText());
        } else if (node instanceof LocalVariableReadNode) {
            return new FrozenString(DefinedMessage.LOCAL_VARIABLE.getText());
        } else if (node instanceof MatchPredicateNode || node instanceof MatchRequiredNode) {
            return new FrozenString(DefinedMessage.METHOD.getText());
        } else if (node instanceof NilNode) {
            return new FrozenString(DefinedMessage.NIL.getText());
        } else if (node instanceof SelfNode) {
            return new FrozenString(DefinedMessage.SELF.getText());
        } else if (node instanceof TrueNode) {
            return new FrozenString(DefinedMessage.TRUE.getText());
        } else if (node instanceof StatementsNode stmts) {
            Node[] array = stmts.body;
            Label undefLabel = getNewLabel();
            Label doneLabel = getNewLabel();

            Variable tmpVar = temp();
            for (Node elt : array) {
                Operand result = buildGetDefinition(elt);

                addInstr(createBranch(result, nil(), undefLabel));
            }

            addInstr(new CopyInstr(tmpVar, new FrozenString(DefinedMessage.EXPRESSION.getText())));
            addInstr(new JumpInstr(doneLabel));
            addInstr(new LabelInstr(undefLabel));
            addInstr(new CopyInstr(tmpVar, nil()));
            addInstr(new LabelInstr(doneLabel));

            return tmpVar;
        } else if (node instanceof GlobalVariableReadNode gvar) {
            return buildGlobalVarGetDefinition(symbol(gvar.name));
        } else if (node instanceof BackReferenceReadNode) {
            return addResultInstr(
                    new RuntimeHelperCall(
                            temp(),
                            IS_DEFINED_BACKREF,
                            new Operand[] {new FrozenString(DefinedMessage.GLOBAL_VARIABLE.getText())}
                    )
            );
        } else if (node instanceof NumberedReferenceReadNode numparam) {
            return addResultInstr(new RuntimeHelperCall(
                    temp(),
                    IS_DEFINED_NTH_REF,
                    new Operand[] {
                            fix(numparam.number),
                            new FrozenString(DefinedMessage.GLOBAL_VARIABLE.getText())
                    }
            ));
        } else if (node instanceof InstanceVariableReadNode ivar) {
            return buildInstVarGetDefinition(symbol(ivar.name));
        } else if (node instanceof InstanceVariableOrWriteNode ivar) {
            return buildInstVarGetDefinition(symbol(ivar.name));
        } else if (node instanceof ClassVariableReadNode cvar) {
            return buildClassVarGetDefinition(symbol(cvar.name));
        } else if (node instanceof SuperNode zuper) {
            Label undefLabel = getNewLabel();
            Variable tmpVar = addResultInstr(
                    new RuntimeHelperCall(
                            temp(),
                            IS_DEFINED_SUPER,
                            new Operand[]{
                                    buildSelf(),
                                    new FrozenString(DefinedMessage.SUPER.getText())
                            }
                    )
            );
            addInstr(createBranch(tmpVar, nil(), undefLabel));
            Operand superDefnVal = buildGetArgumentDefinition(zuper.arguments, DefinedMessage.SUPER.getText());
            return buildDefnCheckIfThenPaths(undefLabel, superDefnVal);
        } else if (node instanceof ForwardingSuperNode) {
            return addResultInstr(
                    new RuntimeHelperCall(temp(), IS_DEFINED_SUPER,
                            new Operand[] { buildSelf(), new FrozenString(DefinedMessage.SUPER.getText()) }));
        } else if (node instanceof CallNode call) {
            if (call.receiver == null && call.arguments == null) { // VCALL
                return addResultInstr(
                        new RuntimeHelperCall(temp(), IS_DEFINED_METHOD,
                                new Operand[]{ buildSelf(), new FrozenString(symbol(call.name)), fals(), new FrozenString(DefinedMessage.METHOD.getText()) }));
            }

            String type = DefinedMessage.METHOD.getText();

            if (call.receiver == null) { // FCALL
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
                                        new Symbol(symbol(call.name)),
                                        fals(),
                                        new FrozenString(DefinedMessage.METHOD.getText())
                                }
                        )
                );
                addInstr(createBranch(tmpVar, nil(), undefLabel));
                Operand argsCheckDefn = buildGetArgumentDefinition(call.arguments, type);
                return buildDefnCheckIfThenPaths(undefLabel, argsCheckDefn);
            } else { // CALL
                // protected main block
                CodeBlock protectedCode = new CodeBlock() {
                    public Operand run() {
                        final Label undefLabel = getNewLabel();
                        Operand receiverDefn = buildGetDefinition(call.receiver);
                        addInstr(createBranch(receiverDefn, nil(), undefLabel));
                        Variable tmpVar = temp();
                        addInstr(new RuntimeHelperCall(tmpVar, IS_DEFINED_CALL,
                                new Operand[]{
                                        build(call.receiver),
                                        new Symbol(symbol(call.name)),
                                        new FrozenString(DefinedMessage.METHOD.getText())
                                }));
                        return buildDefnCheckIfThenPaths(undefLabel, tmpVar);
                    }
                };

                // Try verifying definition, and if we get an exception, throw it out, and return nil
                return protectCodeWithRescue(protectedCode, () -> nil());
            }
        } else if (node instanceof YieldNode) {
            return buildDefinitionCheck(new BlockGivenInstr(temp(), getYieldClosureVariable()), DefinedMessage.YIELD.getText());
        } else if (node instanceof SuperNode) {
            // FIXME: Current code missing way to tell zsuper from super
            return addResultInstr(
                    new RuntimeHelperCall(temp(), IS_DEFINED_SUPER,
                            new Operand[]{buildSelf(), new FrozenString(DefinedMessage.SUPER.getText())}));
        } else if (node instanceof ConstantReadNode constant) {
            return buildConstantGetDefinition(symbol(constant.name));
        } else if (node instanceof ConstantPathNode path) {
            // SSS FIXME: Is there a reason to do this all with low-level IR?
            // Can't this all be folded into a Java method that would be part
            // of the runtime library, which then can be used by buildDefinitionCheck method above?
            // This runtime library would be used both by the interpreter & the compiled code!

            final RubySymbol name = symbol(path.name);
            final Variable errInfo = temp();

            // store previous exception for restoration if we rescue something
            addInstr(new GetErrorInfoInstr(errInfo));

            CodeBlock protectedCode = () -> {
                if (path.parent == null) { // colon3
                    return addResultInstr(
                            new RuntimeHelperCall(
                                    temp(),
                                    IS_DEFINED_CONSTANT_OR_METHOD,
                                    new Operand[]{
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
                Operand test = buildGetDefinition(path.parent);
                addInstr(createBranch(test, nil(), bad));
                Operand lhs = build(path.parent);
                addInstr(
                        new RuntimeHelperCall(
                                result,
                                IS_DEFINED_CONSTANT_OR_METHOD,
                                new Operand[]{
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
            };

            // Try verifying definition, and if we get an JumpException exception, process it with the rescue block above
            return protectCodeWithRescue(protectedCode, () -> {
                addInstr(new RestoreErrorInfoInstr(errInfo)); // ignore and restore (we don't care about error)
                return nil();
            });
        } else if (node instanceof ArrayNode array) {
            Label undefLabel = getNewLabel();
            Label doneLabel = getNewLabel();

            Variable tmpVar = temp();
            for (Node elt : array.elements) {
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

        return new FrozenString("expression");
    }

    private Operand buildGlobalVariableRead(Variable result, GlobalVariableReadNode node) {
        return buildGlobalVar(result, symbol(node.name));
    }

    private Operand buildGlobalVariableOperatorWrite(GlobalVariableOperatorWriteNode node) {
        Operand lhs = buildGlobalVar(temp(), symbol(node.name));
        Operand rhs = build(node.value);
        Variable value = call(temp(), lhs, symbol(node.binary_operator), rhs);
        addInstr(new PutGlobalVarInstr(symbol(node.name), value));
        return value;
    }

    private Operand buildGlobalVariableAndWrite(GlobalVariableAndWriteNode node) {
        return buildOpAsgnAnd(() -> buildGlobalVar(temp(), symbol(node.name)),
                () -> buildGlobalAsgn(symbol(node.name), node.value));
    }

    private Operand buildGlobalVariableOrWrite(GlobalVariableOrWriteNode node) {
        return buildOpAsgnOrWithDefined(node,
                (result) -> buildGlobalVar((Variable) result, symbol(node.name)),
                () -> buildGlobalAsgn(symbol(node.name), node.value));
    }

    private Operand buildGlobalVariableWrite(GlobalVariableWriteNode node) {
        return buildGlobalAsgn(symbol(node.name), node.value);
    }

    private Operand buildHash(Node[] elements, boolean hasAssignments) {
        List<KeyValuePair<Operand, Operand>> args = new ArrayList<>();
        Set<Object> keysHack = new HashSet<>(); // Remove once prism #2005 is fixed.
        Variable hash = null;
        // Duplication checks happen when **{} are literals and not **h variable references.
        Operand duplicateCheck = fals();

        // pair is AssocNode or AssocSplatNode
        for (Node pair: elements) {
            Operand keyOperand;

            if (pair instanceof AssocNode assoc) {
                Node key = assoc.key;

                if (key instanceof StringNode str) {  // FIXME: #2045 in prism about whether it should be marked frozen by parser.
                    keyOperand = buildFrozenString(str);

                    String hack = ((FrozenString) keyOperand).getByteList().toString();
                    if (!keysHack.add(hack)) getManager().getRuntime().getWarnings().warn("key \"" + hack + "\" is duplicated and overwritten on line " + (getLine(key) + 1));
                } else {
                    keyOperand = build(key);
                    if (keyOperand instanceof Symbol sym) {
                        String hack = sym.getString();
                        if (!keysHack.add(hack)) getManager().getRuntime().getWarnings().warn("key :" + hack + " is duplicated and overwritten on line " + (getLine(key) + 1));
                    } else if (keyOperand instanceof Fixnum fix) {
                        long hack = fix.value;
                        if (!keysHack.add(Long.valueOf(hack))) getManager().getRuntime().getWarnings().warn("key " + hack + " is duplicated and overwritten on line " + (getLine(key) + 1));
                    } else if (keyOperand instanceof Float flote) {
                        double hack = flote.value;
                        if (!keysHack.add(Double.valueOf(hack))) getManager().getRuntime().getWarnings().warn("key " + hack + " is duplicated and overwritten on line " + (getLine(key) + 1));
                    }
                }
                args.add(new KeyValuePair<>(keyOperand, buildWithOrder(assoc.value, hasAssignments)));
            } else {  // AssocHashNode
                AssocSplatNode assoc = (AssocSplatNode) pair;
                boolean isLiteral = assoc.value instanceof HashNode;
                duplicateCheck = isLiteral ? tru() : fals();

                if (hash == null) {                     // No hash yet. Define so order is preserved.
                    hash = copy(new Hash(args));
                    args = new ArrayList<>();           // Used args but we may find more after the splat so we reset
                } else if (!args.isEmpty()) {
                    addInstr(new RuntimeHelperCall(hash, MERGE_KWARGS, new Operand[] { hash, new Hash(args), duplicateCheck}));
                    args = new ArrayList<>();
                }
                Operand splat = buildWithOrder(assoc.value, hasAssignments);
                addInstr(new RuntimeHelperCall(hash, MERGE_KWARGS, new Operand[] { hash, splat, duplicateCheck}));
            }
        }

        if (hash == null) {           // non-**arg ordinary hash
            hash = copy(new Hash(args));
        } else if (!args.isEmpty()) { // ordinary hash values encountered after a **arg
            addInstr(new RuntimeHelperCall(hash, MERGE_KWARGS, new Operand[] { hash, new Hash(args), duplicateCheck}));
        }

        return hash;
    }

    private Operand buildIf(Variable result, IfNode node) {
        return buildConditional(result, node.predicate, node.statements, node.subsequent);
    }

    private Operand buildIndexAndWrite(IndexAndWriteNode node) {
        return buildOpElementAsgnWith(node.receiver, node.arguments, node.block, node.value, fals());
    }

    private Operand buildIndexOperatorWrite(IndexOperatorWriteNode node) {
        return buildOpElementAsgnWithMethod(node.receiver, node.arguments, node.block, node.value, symbol(node.binary_operator));
    }

    private Operand buildIndexOrWrite(IndexOrWriteNode node) {
        return buildOpElementAsgnWith(node.receiver, node.arguments, node.block, node.value, tru());
    }

    private Operand buildInstanceVariableRead(InstanceVariableReadNode node) {
        return buildInstVar(symbol(node.name));
    }

    private Operand buildInstanceVariableAndWrite(InstanceVariableAndWriteNode node) {
        return buildOpAsgnAnd(() -> addResultInstr(new GetFieldInstr(temp(), buildSelf(), symbol(node.name), false)),
                () -> buildInstAsgn(symbol(node.name), node.value));
    }

    private Operand buildInstanceVariableOrWrite(InstanceVariableOrWriteNode node) {
        return buildOpAsgnOr(() -> addResultInstr(new GetFieldInstr(temp(), buildSelf(), symbol(node.name), false)),
                () -> buildInstAsgn(symbol(node.name), node.value));
    }

    private Operand buildInstanceVariableWrite(InstanceVariableWriteNode node) {
        return buildInstAsgn(symbol(node.name), node.value);
    }

    private Operand buildInteger(IntegerNode node) {
        // FIXME: HAHAHAH horrible hack around integer being too much postprocessing.
        ByteList value = bytelistFrom(node);
        int base = node.isDecimal() ? 10 : node.isOctal() ? 8 : node.isHexadecimal() ? 16 : 2;
        int length = value.length();
        ByteList sanitizedValue = new ByteList(length);

        // Something wrong with str2inum not working due to '_'.  Work around for now.
        for (int i = 0; i < length; i++) {
            int c = value.get(i);
            if (c == '_') continue;
            sanitizedValue.append(c);
        }

        IRubyObject number = RubyNumeric.str2inum(getManager().runtime, getManager().getRuntime().newString(sanitizedValue), base, false, false);

        return number instanceof RubyBignum bignum ?
                new Bignum(bignum.getBigIntegerValue()) :
                fix(RubyNumeric.fix2long(number));
    }

    private Operand buildInterpolatedMatchLastLine(Variable result, InterpolatedMatchLastLineNode node) {
        return buildDRegex(result, node.parts, calculateRegexpOptions(node.flags));
    }

    private Operand buildInterpolatedRegularExpression(Variable result, InterpolatedRegularExpressionNode node) {
        return buildDRegex(result, node.parts, calculateRegexpOptions(node.flags));
    }

    private Operand buildItRead() {
        return getLocalVariable(symbol("it"), 0);
    }

    protected Operand buildDRegex(Variable result, Node[] children, RegexpOptions options) {
        Node[] pieces;
        if (children.length == 1) { // Case of interpolation only being an embedded element
            // We add an empty string here because the internal RubyRegexp.processDRegexp expects there
            // will be at least one string element which has had its encoding set properly by the options
            // value of the regexp.  Adding an empty string will pick up the encoding from options (this
            // empty string is how legacy parsers do this but it naturally falls out of the parser.
            pieces = new Node[children.length + 1];
            pieces[0] = new StringNode(0, 0, (short) 0, EMPTY.bytes());
            pieces[1] = children[0];
        } else {
            pieces = children;
        }

        return super.buildDRegex(result, pieces, options);
    }

    private RegexpOptions calculateRegexpOptions(short flags) {
        RegexpOptions options = new RegexpOptions();
        options.setMultiline(RegularExpressionFlags.isMultiLine(flags));
        options.setIgnorecase(RegularExpressionFlags.isIgnoreCase(flags));
        options.setExtended(RegularExpressionFlags.isExtended(flags));
        // FIXME: Does this still exist in Ruby?
        //options.setFixed((joniOptions & RubyRegexp.RE_FIXED) != 0);
        options.setOnce(RegularExpressionFlags.isOnce(flags));
        if (RegularExpressionFlags.isAscii8bit(flags)) {
            options.setEncodingNone(RegularExpressionFlags.isAscii8bit(flags));
            options.setExplicitKCode(KCode.NONE);
        } else if (RegularExpressionFlags.isUtf8(flags)) {
            options.setExplicitKCode(KCode.UTF8);
            options.setFixed(true);
        } else if (RegularExpressionFlags.isEucJp(flags)) {
            options.setExplicitKCode(KCode.EUC);
            options.setFixed(true);
        } else if (RegularExpressionFlags.isWindows31j(flags)) {
            options.setExplicitKCode(KCode.SJIS);
            options.setFixed(true);
        }

        return options;
    }

    private Operand buildInterpolatedString(Variable result, InterpolatedStringNode node) {
        return buildDStr(result, node.parts, getEncoding(), StringStyle.Chilled, getLine(node));
    }

    private Operand buildInterpolatedSymbol(Variable result, InterpolatedSymbolNode node) {
        return buildDSymbol(result, node.parts, getEncoding(), getLine(node));
    }

    private Operand buildInterpolatedXString(Variable result, InterpolatedXStringNode node) {
        return buildDXStr(result, node.parts, getEncoding(), getLine(node));
    }

    private Operand buildKeywordHash(KeywordHashNode node, int[] flags) {
        flags[0] |= CALL_KEYWORD;
        List<KeyValuePair<Operand, Operand>> args = new ArrayList<>();
        boolean hasAssignments = containsVariableAssignment(node);
        Variable hash = null;
        // Duplication checks happen when **{} are literals and not **h variable references.
        Operand duplicateCheck = fals();

        if (node.elements.length == 1 && node.elements[0] instanceof AssocSplatNode assocSplat) { // Only a single rest arg here.  Do not bother to merge.
            flags[0] |= CALL_KEYWORD_REST;
            Operand splat = buildWithOrder(assocSplat.value, hasAssignments);

            return addResultInstr(new RuntimeHelperCall(temp(), HASH_CHECK, new Operand[] { splat }));
        }

        // pair is AssocNode or AssocSplatNode
        for (Node pair: node.elements) {
            Operand keyOperand;

            if (pair instanceof AssocNode assoc) {
                args.add(new KeyValuePair<>(build(assoc.key), buildWithOrder(assoc.value, hasAssignments)));
            } else {  // AssocHashNode
                flags[0] |= CALL_KEYWORD_REST;
                AssocSplatNode assoc = (AssocSplatNode) pair;
                boolean isLiteral = assoc.value instanceof HashNode;
                duplicateCheck = isLiteral ? tru() : fals();

                if (hash == null) {                     // No hash yet. Define so order is preserved.
                    hash = copy(new Hash(args));
                    args = new ArrayList<>();           // Used args but we may find more after the splat so we reset
                } else if (!args.isEmpty()) {
                    addInstr(new RuntimeHelperCall(hash, MERGE_KWARGS, new Operand[] { hash, new Hash(args), duplicateCheck}));
                    args = new ArrayList<>();
                }
                Operand splat = buildWithOrder(assoc.value, hasAssignments);
                addInstr(new RuntimeHelperCall(hash, MERGE_KWARGS, new Operand[] { hash, splat, duplicateCheck}));
            }
        }

        if (hash == null) {           // non-**arg ordinary hash
            hash = copy(new Hash(args));
        } else if (!args.isEmpty()) { // ordinary hash values encountered after a **arg
            addInstr(new RuntimeHelperCall(hash, MERGE_KWARGS, new Operand[] { hash, new Hash(args), duplicateCheck}));
        }

        return hash;
    }

    private Operand buildLambda(LambdaNode node) {
        StaticScope staticScope = createStaticScopeFrom(node.locals, StaticScope.Type.BLOCK);
        markImplicitVariables(staticScope, node.parameters);
        Signature signature = calculateSignature(node.parameters);
        staticScope.setSignature(signature);
        return buildLambda(node.parameters, node.body, staticScope, signature, getLine(node));
    }

    private Operand buildLocalVariableRead(LocalVariableReadNode node) {
        return getLocalVariable(symbol(node.name), node.depth);
    }

    private Operand buildLocalAndVariableWrite(LocalVariableAndWriteNode node) {
        return buildOpAsgnAnd(() -> getLocalVariable(symbol(node.name), node.depth),
                () -> buildLocalVariableAssign(symbol(node.name), node.depth, node.value));
    }

    private Operand buildLocalOrVariableWrite(LocalVariableOrWriteNode node) {
        return buildOpAsgnOr(() -> getLocalVariable(symbol(node.name), node.depth),
                () -> buildLocalVariableAssign(symbol(node.name), node.depth, node.value));
    }

    private Operand buildLocalVariableWrite(LocalVariableWriteNode node) {
        return buildLocalVariableAssign(symbol(node.name), node.depth, node.value);
    }

    private Operand buildMatchLastLine(Variable result, MatchLastLineNode node) {
        return buildMatch(result,
                new Regexp(new ByteList(node.unescaped,
                        getRegexpEncoding(node.unescaped, node.flags)), calculateRegexpOptions(node.flags)));
    }

    private Operand buildMatchPredicate(MatchPredicateNode node) {
        Variable result = copy(tru());
        buildPatternMatch(result, copy(nil()), copy(nil()), node.pattern, build(node.value), false, true, copy(nil()));
        return result;
    }

    private Operand buildMatchRequired(MatchRequiredNode node) {
        return buildPatternCase(node.value, new Node[] { new InNode(0, 0, node.pattern, null) }, null);
    }

    private Operand buildMatchWrite(Variable result, MatchWriteNode node) {
        Operand receiver = build(node.call.receiver);
        // FIXME: Can this ever be something more?
        Operand value = build(node.call.arguments.arguments[0]);

        if (result == null) result = temp();

        addInstr(new MatchInstr(scope, result, receiver, value));

        for (int i = 0; i < node.targets.length; i++) {
            LocalVariableTargetNode target = (LocalVariableTargetNode) node.targets[i];

            addInstr(new SetCapturedVarInstr(getLocalVariable(symbol(target.name), target.depth), result, symbol(target.name)));
        }

        return result;
    }

    private Operand buildModule(ModuleNode node) {
        return buildModule(determineBaseName(node.constant_path), node.constant_path, node.body,
                createStaticScopeFrom(node.locals, StaticScope.Type.LOCAL),
                getLine(node), getEndLine(node));
    }

    private Operand buildMultiWriteOrTargetNode(Node[] lefts, Node rest, Node[] rights, Object value) {
        var values = temp();
        Map<Node, Operand> reads = new HashMap<>();
        final List<Tuple<Node, ResultInstr>> assigns = new ArrayList<>(4);
        buildMultiAssignment(lefts, rest, rights, values, reads, assigns);

        Operand builtValue;
        if (value instanceof Node node) {
            builtValue = getValueInTemporaryVariable(build(node));

            if (value instanceof ArrayNode) {
                copy(values, builtValue);
            } else {
                addResultInstr(new ToAryInstr(values, builtValue));
            }
        } else {
            builtValue = (Operand) value;
            copy(values, builtValue);
        }

        for (Tuple<Node, ResultInstr> assign: assigns) {
            addInstr((Instr) assign.b);
        }

        buildAssignment(reads, assigns);

        return builtValue;
    }

    // This method is called to build assignments for a multiple-assignment instruction
    protected void buildAssignment(Map<Node, Operand> reads, List<Tuple<Node, ResultInstr>> assigns) {

        for (var assign: assigns) {
            var node = assign.a;
            var rhs = assign.b.getResult();

            switch (node) {
                case CallTargetNode call -> buildCallTarget(call, reads.get(call.receiver), rhs);
                case IndexTargetNode index -> {
                    // FIXME: we determine self.foo[] by requiring receiver to be `self` in AttrAssignInstr but we could
                    //  use isIgnoreVisibility() that the parser provides.  We have no code path to do this though in AttrAssignInstr.
                    var receiver = reads.get(index.receiver);
                    Array holders = (Array) reads.get(index.arguments);
                    int flags = ((Integer) holders.get(holders.size() - 1)).value;
                    Operand[] nargs = new Operand[holders.size() - 1];
                    System.arraycopy(holders.getElts(), 0, nargs, 0, nargs.length);
                    Operand[] args = addArg(nargs, rhs);
                    if (index.isSafeNavigation()) {
                        if_not(receiver, nil(),
                                () -> addInstr(AttrAssignInstr.create(scope, receiver, symbol("[]="), args, flags, scope.maybeUsingRefinements())));
                    } else {
                        addInstr(AttrAssignInstr.create(scope, receiver, symbol("[]="), args, flags, scope.maybeUsingRefinements()));
                    }
                }
                case ClassVariableTargetNode cvar -> addInstr(new PutClassVariableInstr(classVarDefinitionContainer(), symbol(cvar.name), rhs));
                case ConstantPathTargetNode constpath -> putConstant(reads.get(constpath), symbol(constpath.name), rhs);
                case ConstantTargetNode consty -> addInstr(new PutConstInstr(getCurrentModuleVariable(), symbol(consty.name), rhs));
                case LocalVariableTargetNode lvar -> copy(getLocalVariable(symbol(lvar.name), lvar.depth), rhs);
                case GlobalVariableTargetNode gvar -> addInstr(new PutGlobalVarInstr(symbol(gvar.name), rhs));
                case InstanceVariableTargetNode ivar -> addInstr(new PutFieldInstr(buildSelf(), symbol(ivar.name), rhs));
                case MultiTargetNode _m -> { } // handled in processReads()
                case RequiredParameterNode req -> copy(getLocalVariable(symbol(req.name), 0), rhs);
                case ImplicitRestNode _r -> { } // This is assigned to nothing
                case SplatNode _r -> { } // This is splat where expression is null
                default -> throw notCompilable("Can't build assignment node", node);
            }
        }
    }

    // SplatNode, MultiWriteNode, LocalVariableWrite and lots of other normal writes
    private void buildMultiAssignment(Node[] pre, Node rest, Node[] post, Operand values,
                                      Map<Node, Operand> reads, List<Tuple<Node, ResultInstr>> assigns) {
        int i = 0;
        for (var an: pre) {
            var get = new ReqdArgMultipleAsgnInstr(temp(), values, i);
            assigns.add(new Tuple<>(an, get));
            processReads(get.getResult(), assigns, reads, an);
            i++;
        }

        if (rest != null) {
            if (rest instanceof SplatNode splat) {
                Node realTarget = splat.expression;
                var get = new RestArgMultipleAsgnInstr(temp(), values, 0, pre.length, post.length);
                if (realTarget == null) {
                    assigns.add(new Tuple<>(rest, get));
                } else {
                    assigns.add(new Tuple<>(realTarget, get));
                }
                processReads(get.getResult(), assigns, reads, rest);
            } else {
                var get = new RestArgMultipleAsgnInstr(temp(), values, 0, pre.length, post.length);
                assigns.add(new Tuple<>(rest, get));
                processReads(get.getResult(), assigns, reads, rest);
            }
        }

        int j = 0;
        for (var an: post) {
            var get = new ReqdArgMultipleAsgnInstr(temp(), values, j, i, post.length);
            assigns.add(new Tuple<>(an, get));
            processReads(get.getResult(), assigns, reads, an);
            j++;
        }
    }

    private void processReads(Variable result, List<Tuple<Node, ResultInstr>> assigns, Map<Node, Operand> reads,
                              Node node) {
        switch(node) {
            case CallTargetNode call -> reads.put(call.receiver, build(call.receiver));
            case IndexTargetNode index -> {
                reads.put(index.receiver, build(index.receiver));
                Node[] arguments = argumentsFrom(index.arguments);
                int[] flags = new int[] { 0 };
                Operand[] args = buildCallArgsArray(arguments, flags);
                Operand[] hackyArgs = new Operand[args.length + 1];
                System.arraycopy(args, 0, hackyArgs, 0, args.length);
                hackyArgs[args.length] = new Integer(flags[0]);
                reads.put(index.arguments, new Array(hackyArgs));
            }
            case ClassVariableTargetNode _cvar -> reads.put(node, classVarDefinitionContainer());
            case ConstantPathTargetNode constpath -> reads.put(node, buildModuleParent(constpath.parent));
            case MultiTargetNode multi -> {
                var subRet = temp();
                assigns.add(new Tuple<>(multi, new ToAryInstr(subRet, result)));
                buildMultiAssignment(multi.lefts, multi.rest, multi.rights, subRet, reads, assigns);
            }
            default -> {}
        }
    }

    private Operand buildNext(NextNode node) {
        return buildNext(buildArgumentsAsArgument(node.arguments), getLine(node));
    }

    private Operand buildNumberedReferenceRead(NumberedReferenceReadNode node) {
        return buildNthRef(node.number);
    }

    private Operand buildOr(OrNode node) {
        return buildOr(build(node.left), () -> build(node.right), binaryType(node.left));
    }

    private void buildParameters(ParametersNode parameters) {
        if (parameters == null) {
            Variable keywords = addResultInstr(new ReceiveKeywordsInstr(temp(), false, false));
            buildArity(false, false, keywords, 0, -1, 0);
            return;
        }

        if (parameters.keyword_rest instanceof ForwardingParameterNode) {
            Variable keywords = addResultInstr(new ReceiveKeywordsInstr(temp(), true, true));
            receiveNonBlockArgs(parameters, keywords, true);
            argumentResult(symbol(FWD_ALL)); // Dummy placeholder so eval parses know this is a forward all
            RubySymbol restName = symbol(FWD_REST);
            RubySymbol kwrestName = symbol(FWD_KWREST);
            RubySymbol blockName = symbol(FWD_BLOCK);
            addInstr(new ReceiveRestArgInstr(argumentResult(restName), keywords, parameters.requireds.length, parameters.requireds.length));
            addInstr(new ReceiveKeywordRestArgInstr(argumentResult(kwrestName), keywords));
            Variable blockVar = argumentResult(blockName);
            Variable tmp = addResultInstr(new LoadImplicitClosureInstr(temp()));
            addInstr(new ReifyClosureInstr(blockVar, tmp));

            addArgumentDescription(ArgumentType.rest, restName);
            addArgumentDescription(ArgumentType.keyrest, kwrestName);
            addArgumentDescription(ArgumentType.block, blockName);

            return;
        }

        boolean hasRest = parameters.rest != null;
        boolean hasKeywords = parameters.keywords.length != 0 || parameters.keyword_rest != null;
        Variable keywords = addResultInstr(new ReceiveKeywordsInstr(temp(), hasRest, hasKeywords));

        // We want this to come in before arity check since arity will think no kwargs should exist.
        if (parameters.keyword_rest instanceof NoKeywordsParameterNode) {
            if_not(keywords, UndefinedValue.UNDEFINED, () -> addRaiseError("ArgumentError", "no keywords accepted"));
        }

        receiveNonBlockArgs(parameters, keywords, hasKeywords);

        if (hasKeywords) {
            int keywordsCount = parameters.keywords.length;
            Node[] kwArgs = parameters.keywords;
            for (int i = 0; i < keywordsCount; i++) {
                if (kwArgs[i] instanceof  OptionalKeywordParameterNode optkwarg) {
                    buildKeywordParameter(keywords, symbol(optkwarg.name), optkwarg.value);
                } else { // RequiredKeywordParameterNode
                    buildKeywordParameter(keywords, symbol(((RequiredKeywordParameterNode) kwArgs[i]).name), null);
                }
            }
        }


        if (parameters.keyword_rest instanceof NoKeywordsParameterNode) {
            addArgumentDescription(ArgumentType.nokey, null);
            // I don't think we need to slurp up anything to **nil so no recv instr here.
        } else if (parameters.keyword_rest instanceof KeywordRestParameterNode kwrest) {
            RubySymbol key = kwrest.name == null ? symbol(STAR_STAR) : symbol(kwrest.name);
            addArgumentDescription(ArgumentType.keyrest, key);

            addInstr(new ReceiveKeywordRestArgInstr(getNewLocalVariable(key, 0), keywords));
        }

        receiveBlockArg(parameters.block);

        // FIXME: we calc signature earlier why do we do this again?  (for Prism and AST)
        int preCount = parameters.requireds.length;
        int optCount = parameters.optionals.length;
        int keywordsCount = parameters.keywords.length;
        int postCount = parameters.posts.length;
        int keyRest = parameters.keyword_rest == null ? -1 : preCount + optCount + postCount + keywordsCount;
        int requiredCount = preCount + postCount;

        buildArity(hasRest, hasKeywords, keywords, optCount, keyRest, requiredCount);
    }

    private void buildArity(boolean hasRest, boolean hasKeywords, Operand keywords, int optCount, int keyRest, int requiredCount) {
        // For closures, we don't need the check arity call
        if (scope instanceof IRMethod) {
            // Expensive to do this explicitly?  But, two advantages:
            // (a) on inlining, we'll be able to get rid of these checks in almost every case.
            // (b) compiler to bytecode will anyway generate this and this is explicit.
            // For now, we are going explicit instruction route.
            // But later, perhaps can make this implicit in the method setup preamble?

            addInstr(new CheckArityInstr(requiredCount, optCount, hasRest, keyRest, keywords));
        } else if (scope instanceof IRClosure && hasKeywords) {
            // FIXME: This is added to check for kwargs correctness but bypass regular correctness.
            // Any other arity checking currently happens within Java code somewhere (RubyProc.call?)
            addInstr(new CheckArityInstr(requiredCount, optCount, hasRest, keyRest, keywords));
        }
    }

    private void buildKeywordParameter(Variable keywords, RubySymbol key, Node value) {
        boolean isOptional = value != null;
        addKeyArgDesc(key, isOptional);

        label("kw_param_end", (end) -> {
            Variable av = getNewLocalVariable(key, 0);

            addInstr(new ReceiveKeywordArgInstr(av, keywords, key));
            addInstr(BNEInstr.create(end, av, UndefinedValue.UNDEFINED)); // if 'av' is not undefined, we are done

            if (isOptional) {
                // FIXME: I think this first nil out is not needed based on second copy
                addInstr(new CopyInstr(av, nil())); // wipe out undefined value with nil
                // FIXME: this is performing extra copy but something is generating a temp and not using local if we pass it to build
                copy(av, build(value));
            } else {
                addInstr(new RaiseRequiredKeywordArgumentError(key));
            }
        });
    }

    private Operand buildPostExecution(PostExecutionNode node) {
        return buildPostExe(node.statements, getLine(node));
    }

    private Operand buildPreExecution(PreExecutionNode node) {
        return buildPreExe(node.statements);
    }

    private Operand buildRational(RationalNode node) {
        ImmutableLiteral num = asRationalValue(node.numerator);
        ImmutableLiteral den = asRationalValue(node.denominator);
        return new Rational(num, den);
    }

    private ImmutableLiteral asRationalValue(Object value) {
        return value instanceof java.lang.Integer ? new Fixnum((int) value) :
                value instanceof Long ? new Fixnum((long) value) :
                        new Bignum((BigInteger) value);
    }

    private Operand buildRange(RangeNode node) {
        return buildRange(node.left, node.right, node.isExcludeEnd());
    }



    private Operand buildRedo(RedoNode node) {
        return buildRedo(getLine(node));
    }

    private Operand buildRegularExpression(RegularExpressionNode node) {
        return new Regexp(new ByteList(node.unescaped,
                getRegexpEncoding(node.unescaped, node.flags)), calculateRegexpOptions(node.flags));
    }

    private Encoding hackRegexpEncoding(byte[] data) {
        Encoding encoding = getEncoding();
        int length = data.length;
        int i = 0;

        while(i < length) {
            int len = encoding.length(data, i, length);
            if (len == -1) break;
            i += len;
        }

        if (i == length) {
            return encoding;
        }

        return ASCIIEncoding.INSTANCE;
    }

    private Operand buildRescueModifier(RescueModifierNode node) {
        return buildEnsureInternal(node.expression, null, null, node.rescue_expression, null, true, null, true, null);
    }

    private Operand buildRetry(RetryNode node) {
        return buildRetry(getLine(node));
    }

    private Operand buildReturn(ReturnNode node) {
        if (isTopLevel() && node.arguments != null) {
            scope.getManager().getRuntime().getWarnings().warn(getFileName(), getLine(node) + 1, "argument of top-level return is ignored");
        }

        Operand args = node.arguments == null ? nil() : buildYieldArgs(node.arguments.arguments, new int[1]);

        return buildReturn(args, getLine(node));
    }

    private Operand buildRestKeywordArgs(KeywordHashNode keywordArgs, int[] flags) {
        flags[0] |= CALL_KEYWORD_REST;
        Node[] pairs = keywordArgs.elements;
        boolean containsVariableAssignment = containsVariableAssignment(keywordArgs);

        if (pairs.length == 1) { // Only a single rest arg here.  Do not bother to merge.
            var pair = (AssocSplatNode) pairs[0];

            if (pair.value == null) {
                return getLocalVariable(symbol("**"), scope.getStaticScope().isDefined("**"));
             } else {
                Operand splat = buildWithOrder(pair.value, containsVariableAssignment);

                return addResultInstr(new RuntimeHelperCall(temp(), HASH_CHECK, new Operand[]{splat}));
            }
        }

        Variable splatValue = copy(new Hash(new ArrayList<>()));
        for (int i = 0; i < pairs.length; i++) {
            Operand splat = buildWithOrder(pairs[i], containsVariableAssignment);
            addInstr(new RuntimeHelperCall(splatValue, MERGE_KWARGS, new Operand[] { splatValue, splat, fals() }));
        }

        return splatValue;
    }

    private Operand buildShareableConstant(ShareableConstantNode node) {
        // FIXME: We do not implement shareable constants yet
        return build(node.write);
    }

    private Operand buildSingletonClass(SingletonClassNode node) {
        return buildSClass(node.expression, node.body,
                createStaticScopeFrom(node.locals, StaticScope.Type.LOCAL), getLine(node), getEndLine(node));
    }

    private Operand buildSourceEncoding() {
        return buildEncoding(getEncoding());
    }

    private Operand buildSourceFile() {
        return new FrozenString(scope.getFile());
    }

    private Operand buildSourceLine(Node node) {
        return fix(getLine(node) + 1);
    }

    private Operand buildSplat(SplatNode node) {
        return buildSplat(build(node.expression));
    }

    private Operand buildSplat(Operand value) {
        return addResultInstr(new BuildSplatInstr(temp(), value, true));
    }

    private Operand buildString(StringNode node) {
        if (node.isMutable()) return copy(temp(), new MutableString(bytelistFrom(node), CR_UNKNOWN, scope.getFile(), getLine(node)));
        if (node.isFrozen()) return new FrozenString(bytelistFrom(node), CR_UNKNOWN, scope.getFile(), getLine(node));

        return new ChilledString(bytelistFrom(node), CR_UNKNOWN, scope.getFile(), getLine(node));
    }

    private Operand buildFrozenString(StringNode node) {
        return new FrozenString(bytelistFrom(node), CR_UNKNOWN, scope.getFile(), getLine(node));
    }

    private Operand buildSuper(Variable result, SuperNode node) {
        return buildSuper(result, node.block, node.arguments, getLine(node), node.hasNewLineFlag());
    }

    private Operand buildSymbol(SymbolNode node) {
        return new Symbol(symbol(node));
    }

    private Operand buildUndef(UndefNode node) {
        Operand last = nil();
        for (Operand name: buildNodeList(node.names)) {
            last = buildUndef(name);
        }
        return last;
    }

    private Operand buildUnless(Variable result, UnlessNode node) {
        return buildConditional(result, node.predicate, node.else_clause, node.statements);
    }

    private Operand buildUntil(UntilNode node) {
        return buildConditionalLoop(node.predicate, node.statements, false, !node.isBeginModifier());
    }

    private void buildWhenSplatValues(Variable eqqResult, Node node, Operand testValue, Label bodyLabel,
                                      Set<IRubyObject> seenLiterals, Map<IRubyObject, java.lang.Integer> origLocs) {
        // FIXME: could see errors since this is missing whatever is YARP args{cat,push}?
        if (node instanceof StatementsNode stmts) {
            buildWhenValues(eqqResult, stmts.body, testValue, bodyLabel, seenLiterals, origLocs);
        } else if (node instanceof SplatNode) {
            buildWhenValue(eqqResult, testValue, bodyLabel, node, seenLiterals, origLocs, true);
        } else {
            buildWhenValue(eqqResult, testValue, bodyLabel, node, seenLiterals, origLocs, true);
        }
    }

    private Operand buildWhile(WhileNode node) {
        return buildConditionalLoop(node.predicate, node.statements, true, !node.isBeginModifier());
    }

    // FIXME: implement
    @Override
    protected boolean alwaysFalse(Node node) {
        return false;
    }

    // FIXME: implement
    @Override
    protected boolean alwaysTrue(Node node) {
        return false;
    }

    @Override
    protected Node[] exceptionNodesFor(RescueNode node) {
        return node.exceptions;
    }

    @Override
    protected Node bodyFor(RescueNode node) {
        return node.statements;
    }

    @Override
    protected RescueNode optRescueFor(RescueNode node) {
        return node.subsequent;
    }

    // FIXME: Implement
    @Override
    protected boolean isSideEffectFree(Node node) {
        return false;
    }

    // FIXME: Implement
    @Override
    protected boolean isErrorInfoGlobal(Node body) {
        return false;
    }

    protected int dynamicPiece(Operand[] pieces, int i, Node pieceNode, Encoding encoding) {
        Operand piece;

        // somewhat arbitrary minimum size for interpolated values
        int estimatedSize = 4;

        while (true) { // loop to unwrap EvStr

            // FIXME: missing EmbddedVariableNode.
            if (pieceNode instanceof StringNode str) {
                // FIXME: This is largely a duplicate buildString.  It can be partially genericalized.
                ByteList data = encoding == null ?
                        bytelistFrom(str) :
                        new ByteList(str.unescaped, encoding);

                if (((StringNode) pieceNode).isFrozen()) {
                    piece = new FrozenString(data, CR_UNKNOWN, scope.getFile(), getLine(pieceNode));
                } else {
                    piece = new MutableString(data, CR_UNKNOWN, scope.getFile(), getLine(pieceNode));
                }

                estimatedSize += data.realSize();
            } else if (pieceNode instanceof EmbeddedStatementsNode embed) {
                if (scope.maybeUsingRefinements()) {
                    // refined asString must still go through dispatch
                    Variable result = temp();
                    addInstr(new AsStringInstr(scope, result, build(embed.statements), scope.maybeUsingRefinements()));
                    piece = result;
                } else {
                    // evstr/asstring logic lives in BuildCompoundString now, unwrap and try again
                    pieceNode = embed.statements;
                    continue;
                }
            } else {
                piece = build(pieceNode);
            }

            break;
        }

        if (piece instanceof MutableString mut) {
            piece = mut.frozenString;
        }

        pieces[i] = piece == null ? nil() : piece;

        return estimatedSize;
    }

    /**
     * Reify the implicit incoming block into a full Proc, for use as "block arg", but only if
     * a block arg is specified in this scope's arguments.
     *  @param node the arguments containing the block arg, if any
     *
     */
    protected void receiveBlockArg(Node node) {
        BlockParameterNode blockArg = (BlockParameterNode) node;

        // reify to Proc if we have a block arg
        if (blockArg != null) {
            // FIXME: Handle bare '&' case?
            RubySymbol name = blockArg.name == null ? symbol(FWD_BLOCK) : symbol(blockArg.name);
            Variable blockVar = argumentResult(name);
            addArgumentDescription(ArgumentType.block, name);
            Variable tmp = temp();
            addInstr(new LoadImplicitClosureInstr(tmp));
            addInstr(new ReifyClosureInstr(blockVar, tmp));
        }
    }

    protected void receiveNonBlockArgs(ParametersNode args, Variable keywords, boolean hasKeywords) {
        int preCount = args.requireds.length;
        int optCount = args.optionals.length;
        boolean hasRest = args.rest != null;
        int postCount = args.posts.length;
        int requiredCount = preCount + postCount;

        // Other args begin at index 0
        int argIndex = 0;

        // Pre(-opt and rest) required args
        Node[] pres = args.requireds;
        for (int i = 0; i < preCount; i++, argIndex++) {
            receivePreArg(pres[i], keywords, argIndex);
        }

        // Fixup opt/rest
        Node[] opts = args.optionals;

        // Now for opt args
        if (optCount > 0) {
            for (int j = 0; j < optCount; j++, argIndex++) {
                // We fall through or jump to variableAssigned once we know we have a valid value in place.
                Label variableAssigned = getNewLabel();
                OptionalParameterNode optArg = (OptionalParameterNode) opts[j];
                RubySymbol argName = symbol(optArg.name);
                Variable argVar = argumentResult(argName);
                addArgumentDescription(ArgumentType.opt, argName);
                // You need at least required+j+1 incoming args for this opt arg to get an arg at all
                addInstr(new ReceiveOptArgInstr(argVar, keywords, j, requiredCount, preCount));
                addInstr(BNEInstr.create(variableAssigned, argVar, UndefinedValue.UNDEFINED));
                // We add this extra nil copy because we do not know if we have a circular defininition of
                // argVar: proc { |a=a| } or proc { |a = foo(bar(a))| }.
                copy(argVar, nil());
                // This bare build looks weird but OptArgNode is just a marker and value is either a LAsgnNode
                // or a DAsgnNode.  So building the value will end up having a copy(var, assignment).
                copy(argVar, build(optArg.value));
                addInstr(new LabelInstr(variableAssigned));
            }
        }

        if (hasRest) {
            // Consider: def foo(*); .. ; end
            // For this code, there is no argument name available from the ruby code.
            // So, we generate an implicit arg name
            RubySymbol argName;

            if (args.rest instanceof RestParameterNode restArg) {
                argName = restArg.name == null ? symbol("*") : symbol(restArg.name);
                addArgumentDescription(ArgumentType.rest, argName);
            } else { // ImplicitRestNode  (*,)
                argName = null;
            }

            // You need at least required+opt+1 incoming args for the rest arg to get any args at all
            // If it is going to get something, then it should ignore required+opt args from the beginning
            // because they have been accounted for already.
            addInstr(new ReceiveRestArgInstr(argumentResult(argName), keywords, argIndex, requiredCount + optCount));
        }

        // Post(-opt and rest) required args
        Node[] posts = args.posts;
        for (int i = 0; i < postCount; i++) {
            receivePostArg(posts[i], keywords, i, preCount, optCount, hasRest, postCount);
        }
    }

    // FIXME: I dislike both methods and procs use the same method.
    public void receivePreArg(Node node, Variable keywords, int argIndex) {
        if (node instanceof RequiredParameterNode req) { // methods
            var name = symbol(req.name);
            addArgumentDescription(ArgumentType.req, name);

            addInstr(new ReceivePreReqdArgInstr(argumentResult(name), keywords, argIndex));
        } else if (node instanceof MultiTargetNode multi) { // methods
            Variable v = temp();
            addInstr(new ReceivePreReqdArgInstr(v, keywords, argIndex));
            addArgumentDescription(ArgumentType.anonreq, null);
            Variable rhs = addResultInstr(new ToAryInstr(temp(), v));
            buildMultiWriteOrTargetNode(multi.lefts, multi.rest, multi.rights, rhs);
        } else if (node instanceof ClassVariableTargetNode cvar) {  // blocks/for
            Variable v = temp();
            addInstr(new ReceivePreReqdArgInstr(v, keywords, argIndex));
            addInstr(new PutClassVariableInstr(classVarDefinitionContainer(), symbol(cvar.name), v));
        } else if (node instanceof ConstantTargetNode constant) {  // blocks/for
            Variable v = temp();
            addInstr(new ReceivePreReqdArgInstr(v, keywords, argIndex));
            putConstant(symbol(constant.name), v);
        } else if (node instanceof InstanceVariableTargetNode ivar) {  // blocks/for
            Variable v = temp();
            addInstr(new ReceivePreReqdArgInstr(v, keywords, argIndex));
            addInstr(new PutFieldInstr(buildSelf(), symbol(ivar.name), v));
        } else if (node instanceof LocalVariableTargetNode lvar) {  // blocks/for
            Variable v = getLocalVariable(symbol(lvar.name), lvar.depth);
            addInstr(new ReceivePreReqdArgInstr(v, keywords, argIndex));
        } else if (node instanceof GlobalVariableTargetNode gvar) {
            Variable v = temp();
            addInstr(new ReceivePreReqdArgInstr(v, keywords, argIndex));
            addInstr(new PutGlobalVarInstr(symbol(gvar.name), v));
        } else if (node instanceof CallTargetNode call) {
            var v = addResultInstr(new ReceivePreReqdArgInstr(temp(), keywords, argIndex));
            buildCallTarget(call, build(call.receiver), v);
        } else if (node instanceof IndexTargetNode target) {
            var v = addResultInstr(new ReceivePreReqdArgInstr(temp(), keywords, argIndex));
            if (target.isSafeNavigation()) {
                var receiver = build(target.receiver);
                if_not(receiver, nil(),
                        () -> call(temp(), receiver, symbol("[]="), addArg(buildArguments(target.arguments), v)));
            } else {
                call(temp(), build(target.receiver), symbol("[]="), addArg(buildArguments(target.arguments), v));
            }
        } else {
            throw notCompilable("Can't build required parameter node", node);
        }
    }

    public void receivePostArg(Node node, Variable keywords, int argIndex, int preCount, int optCount, boolean hasRest, int postCount) {
        if (node instanceof RequiredParameterNode req) {
            RubySymbol argName = symbol(req.name);

            addArgumentDescription(ArgumentType.req, argName);

            addInstr(new ReceivePostReqdArgInstr(argumentResult(argName), keywords, argIndex, preCount, optCount, hasRest, postCount));
        } else if (node instanceof MultiTargetNode multi) {
            Variable v = temp();
            addInstr(new ReceivePostReqdArgInstr(v, keywords, argIndex, preCount, optCount, hasRest, postCount));

            addArgumentDescription(ArgumentType.anonreq, null);

            Variable tmp = temp();
            addInstr(new ToAryInstr(tmp, v));

            buildMultiWriteOrTargetNode(multi.lefts, multi.rest, multi.rights, tmp);
        } else {
            throw notCompilable("Can't build required parameter node", node);
        }
    }

    private void addKeyArgDesc(RubySymbol key, boolean isOptional) {
        addArgumentDescription(isOptional ? ArgumentType.key : ArgumentType.keyreq, key);
    }

    private Operand buildProgram(ProgramNode node) {
        return build(node.statements);
    }

    private Operand buildStatements(StatementsNode node) {
        if (node == null) return nil();

        Operand result = null;

        for (Node child : node.body) {
            result = build(child);
        }

        return result == null ? nil() : result;
    }

    @Override
    protected void buildWhenArgs(WhenNode whenNode, Operand testValue, Label bodyLabel,
                                 Set<IRubyObject> seenLiterals, Map<IRubyObject, java.lang.Integer> origLocs) {
        Variable eqqResult = temp();
        Node[] exprNodes = whenNode.conditions;

        if (exprNodes.length == 1) {
            if (exprNodes[0] instanceof SplatNode) {
                buildWhenSplatValues(eqqResult, exprNodes[0], testValue, bodyLabel, seenLiterals, origLocs);
            } else {
                buildWhenValue(eqqResult, testValue, bodyLabel, exprNodes[0], seenLiterals, origLocs, false);
            }
        } else {
            for (Node value: exprNodes) {
                buildWhenValue(eqqResult, testValue, bodyLabel, value, seenLiterals, origLocs, value instanceof SplatNode);
            }
        }
    }

    private Operand buildXString(Variable result, XStringNode node) {
        ByteList value = new ByteList(node.unescaped, getEncoding());
        int codeRange = StringSupport.codeRangeScan(value.getEncoding(), value);
        return fcall(result, buildSelf(), "`", new FrozenString(value, codeRange, scope.getFile(), getLine(node)));
    }

    Operand buildYield(Variable aResult, YieldNode node) {
        if (aResult == null) aResult = temp();

        IRScope hardScope = scope.getNearestNonClosurelikeScope();
        if (hardScope instanceof IRScriptBody || hardScope instanceof IRModuleBody) throwSyntaxError(getLine(node), "Invalid yield");

        Variable result = aResult;
        int[] flags = new int[]{0};
        boolean unwrap = true;

        if (node.arguments != null) {
            if (node.arguments.arguments != null &&
                    node.arguments.arguments.length == 1 &&
                    !(node.arguments.arguments[0] instanceof KeywordHashNode) &&
            !(node.arguments.arguments[0] instanceof SplatNode)) {
                unwrap = false;
            }
        }
        Operand value = buildYieldArgs(node.arguments != null ? node.arguments.arguments : null, flags);
        addInstr(new YieldInstr(scope, result, getYieldClosureVariable(), value, flags[0], unwrap));
        return result;
    }

    Operand buildYieldArgs(Node[] args, int[] flags) {
        if (args == null) {
            return UndefinedValue.UNDEFINED;
        } else if (args.length == 1) {
            if (args[0] instanceof SplatNode splat) { // yield *c
                flags[0] |= CALL_SPLATS;
                return new Splat(buildSplat(splat));
            } else if (args[0] instanceof KeywordHashNode kwhash) {
                return buildKeywordHash(kwhash, flags);
            } else { // ???
                return build(args[0]);
            }
        } else {
            int lastSplat = -1;
            Operand[] operands = new Operand[args.length];
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof SplatNode splat) { // yield with a splat in it in any position
                    if (lastSplat != -1) { // already one splat encountered
                        operands[i] = twoArgs(operands, lastSplat, i, buildSplat(splat), flags);
                    } else { // first and possibly only splat
                        flags[0] |= CALL_SPLATS; // FIXME: not sure all splats count?
                        if (i == 0) {
                            operands[i] = buildSplat(splat);
                        } else {
                            operands[i] = catArgs(operands, 0, i, buildSplat(splat), flags);
                        }
                    }
                    lastSplat = i;
                } else if (args[i] instanceof KeywordHashNode kwhash) {
                    operands[i] = buildKeywordHash(kwhash, flags);
                } else {
                    operands[i] = build(args[i]);
                }
            }

            if (lastSplat != -1) {
                if (lastSplat == args.length - 1) return operands[lastSplat];

                return pushArgs(operands, lastSplat, args.length, flags); // Some trailing elements after a splat
            } else {
                return new Array(operands);
            }
        }
    }

    private Operand twoArgs(Operand[] operands, int lastSplat, int newSplat, Operand newSplatValue, int[] flags) {
        Operand lhs = pushArgs(operands, lastSplat, newSplat, flags);
        return addResultInstr(new BuildCompoundArrayInstr(temp(), lhs, newSplatValue, false, (flags[0] & CALL_KEYWORD_REST) != 0));
    }

    private Operand pushArgs(Operand[] operands, int lastSplat, int newSplat, int[] flags) {
        int length = newSplat - lastSplat - 1;
        if (length == 0) return operands[lastSplat];

        Operand rhs;
        if (length == 1 && (flags[0] & CALL_KEYWORD_REST) != 0) {
            rhs = operands[lastSplat + 1];
        } else {
            rhs = subArray(operands, lastSplat + 1, length);
        }
        return addResultInstr(new BuildCompoundArrayInstr(temp(), operands[lastSplat], rhs, (flags[0] & CALL_KEYWORD_REST) != 0, (flags[0] & CALL_KEYWORD_REST) != 0));
    }

    private Operand catArgs(Operand[] args, int start, int length, Operand splat, int[] flags) {
        Array lhs = subArray(args, start, length);
        return addResultInstr(new BuildCompoundArrayInstr(temp(), lhs, splat, false, (flags[0] & CALL_KEYWORD_REST) != 0));
    }

    private static Array subArray(Operand[] args, int start, int length) {
        Operand[] elts = new Operand[length];
        System.arraycopy(args, start, elts, 0, length);
        Array lhs = new Array(elts);
        return lhs;
    }

    // Assumption: @, @@, $ assignments are all accessed directly through instructions
    // so they will always be indirected through temporary variables.  LocalVariables
    // are just operands so we only need to be aware of these being used within assignments.
    @Override
    protected boolean containsVariableAssignment(Node node) {
        if (node instanceof ParenthesesNode parens) return containsVariableAssignment(parens.body);
        if (node instanceof StatementsNode statements) return containsVariableAssignment(statements.body);

        if (node instanceof LocalVariableWriteNode ||
                node instanceof LocalVariableOperatorWriteNode ||
                node instanceof LocalVariableAndWriteNode ||
                node instanceof LocalVariableOrWriteNode ||
                node instanceof InstanceVariableWriteNode ||
                node instanceof InstanceVariableOperatorWriteNode ||
                node instanceof InstanceVariableAndWriteNode ||
                node instanceof InstanceVariableOrWriteNode ||
                node instanceof GlobalVariableWriteNode ||
                node instanceof GlobalVariableOperatorWriteNode ||
                node instanceof GlobalVariableAndWriteNode ||
                node instanceof GlobalVariableOrWriteNode ||
                node instanceof ClassVariableWriteNode ||
                node instanceof ClassVariableOperatorWriteNode ||
                node instanceof ClassVariableAndWriteNode ||
                node instanceof ClassVariableOrWriteNode) {
            return true;
        }
        return false;
    }

    // FIXME: needs to derive this walking down tree.
    boolean containsVariableAssignment(Node[] nodes) {
        for (int i = 0; i < nodes.length; i++) {
            if (containsVariableAssignment(nodes[i])) return true;
        }
        return false;
    }

    @Override
    protected Operand frozen_string(Node node) {
        // FIXME: this + isStringLiteral might need to change.
        return buildString((StringNode) node);
    }

    @Override
    protected Operand getContainerFromCPath(Node node) {

        if (node instanceof ConstantReadNode) {
            return findContainerModule();
        } else if (node instanceof ConstantPathNode path) {
            if (path.parent == null) { // ::Foo
                return getManager().getObjectClass();
            } else {
                return build(path.parent);
            }
        }
        // FIXME: We may not need these based on whether there are more possible nodes.
        throw notCompilable("Unsupported node in module path", node);
    }

    @Override
    protected Variable buildPatternEach(Label testEnd, Variable result, Operand original, Variable deconstructed, Operand value, Node exprNodes, boolean inAlternation, boolean isSinglePattern, Variable errorString) {
        if (exprNodes instanceof StatementsNode stmts && stmts.body.length == 1) {
            buildPatternEach(testEnd, result, original, deconstructed, value, stmts.body[0], inAlternation, isSinglePattern, errorString);
        } else if (exprNodes instanceof ArrayPatternNode node) {
            buildArrayPattern(testEnd, result, deconstructed, node.constant, node.requireds, node.rest, node.posts, value, inAlternation, isSinglePattern, errorString);
        } else if (exprNodes instanceof CapturePatternNode capture) {
            buildPatternEach(testEnd, result, original, deconstructed, value, capture.value, inAlternation, isSinglePattern, errorString);
            buildAssignment(capture.target, value);
        } else if (exprNodes instanceof HashPatternNode node) {
            Node[] keys = getKeys(node);
            buildHashPattern(testEnd, result, deconstructed, node.constant, node, keys, node.rest, value, inAlternation, isSinglePattern, errorString);
        } else if (exprNodes instanceof FindPatternNode node) {
            getManager().getRuntime().getWarnings().warnExperimental(getFileName(), getLine(exprNodes) + 1,
                    "Find pattern is experimental, and the behavior may change in future versions of Ruby!");
            buildFindPattern(testEnd, result, deconstructed, node.constant, node.left, node.requireds, node.right, value, inAlternation, isSinglePattern, errorString);
        } else if (exprNodes instanceof IfNode node) {
            buildPatternEachIf(result, original, deconstructed, value, node.predicate, node.statements, node.subsequent, inAlternation, isSinglePattern, errorString);
        } else if (exprNodes instanceof UnlessNode node) {
            buildPatternEachIf(result, original, deconstructed, value, node.predicate, node.else_clause, node.statements, inAlternation, isSinglePattern, errorString);
        } else if (exprNodes instanceof LocalVariableTargetNode node) {
            buildPatternLocal(node, value, inAlternation);
        } else if (exprNodes instanceof ImplicitNode node) {
            buildPatternEach(testEnd, result, original, deconstructed, value, node.value, inAlternation, isSinglePattern, errorString);
        } else if (exprNodes instanceof AssocSplatNode node) {
            buildPatternLocal((LocalVariableTargetNode) node.value, value, inAlternation);
        } else if (exprNodes instanceof ImplicitRestNode) {
            // do nothing
        } else if (exprNodes instanceof SplatNode splat) {
            buildAssignment(splat.expression, value);
            // do nothing
        } else if (exprNodes instanceof AlternationPatternNode node) {
            buildPatternOr(testEnd, original, result, deconstructed, value, node.left, node.right, isSinglePattern, errorString);
        } else {
            Operand expression = build(exprNodes);
            boolean needsSplat = exprNodes instanceof AssocSplatNode; // FIXME: just a guess this is all we need for splat?

            addInstr(new EQQInstr(scope, result, expression, value, needsSplat, false, scope.maybeUsingRefinements()));
        }

        return result;
    }

    private Encoding getRegexpEncoding(byte[] bytes, short flags) {
        Encoding encoding = getRegexpEncodingFromOptions(flags);

        return encoding == null ? hackRegexpEncoding(bytes) : encoding;
    }

    private Encoding getRegexpEncodingFromOptions(short flags) {
        return RegularExpressionFlags.isAscii8bit(flags) ? ASCIIEncoding.INSTANCE :
                RegularExpressionFlags.isUtf8(flags) ? UTF8Encoding.INSTANCE :
                        RegularExpressionFlags.isEucJp(flags) ? EUCJPEncoding.INSTANCE :
                                RegularExpressionFlags.isWindows31j(flags) ? Windows_31JEncoding.INSTANCE :
                                        null;
    }

    private Node[] getKeys(HashPatternNode node) {
        int length = node.elements.length;
        Node[] keys = new Node[length];

        for (int i = 0; i < length; i++) {
            keys[i] = node.elements[i].key;
        }

        return keys;
    }

    void buildPatternLocal(LocalVariableTargetNode node, Operand value, boolean inAlternation) {
        buildPatternLocal(value, symbol(node.name), getLine(node), node.depth, inAlternation);
    }

    @Override
    protected void buildAssocs(Label testEnd, Operand original, Variable result, HashPatternNode assocs, boolean inAlteration, boolean isSinglePattern, Variable errorString, boolean hasRest, Variable d) {

        for (Node node: assocs.elements) {
            // FIXME: There can be more than  AssocNode in here.
            if (node instanceof AssocNode assoc) {
                // FIXME: only build literals (which are guaranteed to build without raising).
                Operand key = build(assoc.key);
                call(result, d, "key?", key);
                copy(errorString, key); // Sets to symbol which will not be nil or a regular string.
                cond_ne_true(testEnd, result);

                String method = hasRest ? "delete" : "[]";
                Operand value = call(temp(), d, method, key);
                Node valueNode =  assoc.value;
                if (valueNode == null) {
                    Node keyHack = assoc.key;
                    RubySymbol name = null;
                    if (keyHack instanceof SymbolNode sym) {
                        name = symbol(sym);
                    } else {
                        throwSyntaxError(getLine(node), "what else is in this");
                    }
                    // FIXME: This needs depth.
                    buildPatternLocal(value, name, getLine(node), 0, inAlteration);
                } else {
                    buildPatternEach(testEnd, result, original, copy(nil()), value, assoc.value, inAlteration, isSinglePattern, errorString);
                }

                cond_ne_true(testEnd, result);
            }
        }
    }

    @Override
    protected boolean isNilRest(Node rest) {
        return rest instanceof NoKeywordsParameterNode;
    }

    @Override
    protected Node getInExpression(Node node) {
        return ((InNode) node).pattern;
    }

    @Override
    protected Node getInBody(Node node) {
        return ((InNode) node).statements;
    }

    @Override
    protected boolean isBareStar(Node node) {
        return node instanceof AssocSplatNode assoc && assoc.value == null;
    }

    private int getEndLine(Node node) {
        int endOffset = node.endOffset();
        // FIXME: Seems like either newline visitor or prism is sometimes reporting the offset one past last indexable source location (fairly rarely).
        //if (endOffset >= nodeSource.bytes.length) endOffset = nodeSource.bytes.length - 1;
        return nodeSource.line(endOffset) - 1;
    }

    @Override
    protected int getLine(Node node) {
        int line = nodeSource.line(node.startOffset);
        //System.out.println("LINE(0): " + line);
        // internals expect 0-based value.
        return line - 1;
    }

    @Override
    public LocalVariable getLocalVariable(RubySymbol name, int scopeDepth) {
        // FIXME: Huge heap of weirdness...scope adds for loop depth per for loop found.  call another method to
        // remove that depth so it can be added back.
        int depth = scope.correctVariableDepthForForLoopsForEncoding(scopeDepth);
        return scope.getLocalVariable(name, depth);
    }

    @Override
    protected IRubyObject getWhenLiteral(Node node) {
        if (node instanceof IntegerNode) {
            // FIXME: determine fixnum/bignum
            return null;
        } else if (node instanceof FloatNode) {
            return null;
            // FIXME:
            //return runtime.newFloat((((FloatNode) node)));
        } else if (node instanceof ImaginaryNode) {
            return null;
            // FIXME:
            //return RubyComplex.newComplexRaw(runtime, getWhenLiteral(((ComplexNode) node).getNumber()));
        } else if (node instanceof RationalNode) {
            return null;
            // FIXME:
            /*
            return RubyRational.newRationalRaw(runtime,
                    getWhenLiteral(((RationalNode) node).getDenominator()),
                    getWhenLiteral(((RationalNode) node).getNumerator()));*/
        } else if (node instanceof NilNode) {
            return context.nil;
        } else if (node instanceof TrueNode) {
            return context.tru;
        } else if (node instanceof FalseNode) {
            return context.fals;
        } else if (node instanceof SymbolNode sym) {
            return symbol(sym);
        } else if (node instanceof StringNode) {
            return context.runtime.newString((bytelistFrom((StringNode) node)));
        }

        return null;
    }

    @Override
    protected boolean isLiteralString(Node node) {
        return node instanceof StringNode;
    }

    // FIXME: This only seems to be used in opelasgnor on the first element (lhs) but it is very unclear what is possible here.
    @Override
    protected boolean needsDefinitionCheck(Node node) {
        return !(node instanceof ClassVariableWriteNode ||
                node instanceof ConstantPathWriteNode ||
                node instanceof LocalVariableWriteNode ||
                node instanceof LocalVariableReadNode ||
                node instanceof FalseNode ||
                node instanceof GlobalVariableWriteNode ||
                node instanceof MatchRequiredNode ||
                node instanceof MatchPredicateNode ||
                node instanceof NilNode ||
                //node instanceof OperatorAssignmentNode ||
                node instanceof SelfNode ||
                node instanceof TrueNode);
    }

    @Override
    protected void receiveMethodArgs(DefNode defNode) {
        buildParameters(defNode.parameters);
    }

    @Override
    protected Operand setupCallClosure(Node args, Node block) {
        if (block == null) {
            if (args != null && isForwardingArguments(args)) {
                return getLocalVariable(symbol(FWD_BLOCK), scope.getStaticScope().isDefined("&"));
            }
            return NullBlock.INSTANCE;
        }

        if (block instanceof BlockNode) {
            return build(block);
        } else if (block instanceof BlockArgumentNode blarg) {
            return buildBlockArgument(blarg);
        }

        throw notCompilable("Encountered unexpected block node", block);
    }

    private boolean isForwardingArguments(Node args) {
        for (Node child: ((ArgumentsNode) args).arguments) {
            if (child instanceof ForwardingArgumentsNode) return true;
        }
        return false;
    }

    private ByteList bytelistFrom(StringNode node) {
        Encoding encoding = node.isForcedBinaryEncoding() ?
                ASCIIEncoding.INSTANCE :
                node.isForcedUtf8Encoding() ?
                        UTF8Encoding.INSTANCE :
                        getEncoding();

        return new ByteList(node.unescaped, encoding);
    }

    private ByteList bytelistFrom(Node node) {
        return new ByteList(source, node.startOffset, node.length);
    }

    public static Signature calculateSignature(ParametersNode parameters) {
        if (parameters == null) return Signature.NO_ARGUMENTS;

        int pre = parameters.requireds.length;
        int opt = parameters.optionals.length;
        int post = parameters.posts.length;
        int keywords = parameters.keywords.length;
        // FIXME: this needs more than norm
        Signature.Rest rest = parameters.rest == null ? Signature.Rest.NONE :
                parameters.rest instanceof ImplicitRestNode ? Signature.Rest.ANON : Signature.Rest.NORM;

        int requiredKeywords = 0;
        if (keywords > 0) {
            for (int i = 0; i < parameters.keywords.length; i++) {
                if (parameters.keywords[i] instanceof RequiredKeywordParameterNode) requiredKeywords++;
            }
        }
        int keywordRestIndex = parameters.keyword_rest == null ? -1 : pre + opt + post + keywords;
        return Signature.from(pre, opt, post, keywords, requiredKeywords, rest, keywordRestIndex);
    }

    private Signature calculateSignature(Node parameters) {
        if (parameters == null) return Signature.NO_ARGUMENTS;

        if (parameters instanceof BlockParametersNode params) {
            return calculateSignature(params.parameters);
        } else if (parameters instanceof NumberedParametersNode params) {
            return Signature.from(params.maximum, 0, 0, 0, 0, Signature.Rest.NONE, -1);
        } else if (parameters instanceof ItParametersNode) {
            return Signature.from(1, 0, 0, 0, 0, Signature.Rest.NONE, -1);
        }

        throw notCompilable("Unknown signature for block parameters", parameters);
    }

    private Signature calculateSignatureFor(Node node) {
        if (node instanceof MultiTargetNode multi) {
            Signature.Rest rest = multi.rest != null ? Signature.Rest.NORM : Signature.Rest.NONE;
            return Signature.from(multi.lefts.length, 0, multi.rights.length, 0, 0, rest, -1);
        } else {
            return Signature.from(1, 0, 0, 0, 0, Signature.Rest.NONE, -1);
        }
    }

    public StaticScope createStaticScopeFrom(byte[][] tokens, StaticScope.Type type) {
        return createStaticScopeFrom(staticScope.getFile(), symbols(tokens), type, staticScope);
    }

    public static StaticScope createStaticScopeFrom(String fileName, RubySymbol[] symbols, StaticScope.Type type, StaticScope parent) {
        String[] strings = new String[symbols.length];
        for(int i = 0; i < symbols.length; i++) {
            strings[i] = symbols[i].idString();
        }

        return StaticScopeFactory.newStaticScope(parent, type, fileName, strings, -1);
    }

    private ByteList determineBaseName(Node node) {
        if (node instanceof ConstantReadNode constant) {
            return symbol(constant.name).getBytes();
        } else if (node instanceof ConstantPathNode path) {
            return symbol(path.name).getBytes();
        }
        throw notCompilable("Unsupported node in module path", node);
    }

    private CallType determineCallType(Node node) {
        return node == null || node instanceof SelfNode ? CallType.FUNCTIONAL : CallType.NORMAL;
    }

    // FIXME: need to know about breaks
    protected boolean canBeLazyMethod(DefNode node) {
        return true;
    }

    // FIXME: We need abstraction for getting names from nodes.
    private RubySymbol globalVariableName(Node node) {
        return switch(node) {
            case GlobalVariableReadNode gvar -> symbol(gvar.name);
            case BackReferenceReadNode backref -> symbol(backref.name);
            case NumberedReferenceReadNode numref -> symbol("$" + numref.number);
            default -> throw notCompilable("Unknown global variable type", node);
        };
    }

    private NotCompilableException notCompilable(String message, Node node) {
        int line = scope.getLine() + 1;
        String loc = scope.getFile() + ":" + line;
        String what = node != null ? node.getClass().getSimpleName() + " - " + loc : loc;
        return new NotCompilableException(message + " (" + what + ").");
    }

    // All splats on array construction will stuff them into a hash.
    private boolean hasOnlyRestKwargs(Node[] elements) {
        for (Node element: elements) {
            if (!(element instanceof AssocSplatNode)) return false;
        }

        return true;
    }
    @Override
    protected Operand putConstant(ConstantPathNode path, Operand value) {
        return putConstant(buildModuleParent(path.parent), symbol(path.name), value);
    }

    @Override
    protected Operand putConstant(ConstantPathNode path, CodeBlock valueBuilder) {
        return putConstant(buildModuleParent(path.parent), symbol(path.name), valueBuilder.run());
    }

    protected RubySymbol symbol(SymbolNode node) {
        var encoding = switch(node.flags) {
            case SymbolFlags.FORCED_BINARY_ENCODING -> ASCIIEncoding.INSTANCE;
            case SymbolFlags.FORCED_US_ASCII_ENCODING -> USASCIIEncoding.INSTANCE;
            case SymbolFlags.FORCED_UTF8_ENCODING ->  UTF8Encoding.INSTANCE;
            default -> getEncoding();
        };
        ByteList bytelist = new ByteList(node.unescaped, encoding);

        // FIXME: This should be done by prism.
        if (RubyString.scanForCodeRange(bytelist) == StringSupport.CR_BROKEN) {
            Ruby runtime = getManager().getRuntime();
            throw runtime.newSyntaxError(str(runtime, "invalid symbol in encoding " + getEncoding() + " :\"", inspectIdentifierByteList(runtime, bytelist), "\""));
        }

        return symbol(bytelist);
    }

    @Override
    protected Node referenceFor(RescueNode node) {
        return node.reference;
    }

    @Override
    protected Node whenBody(WhenNode arm) {
        return arm.statements;
    }

    protected Operand buildOpAsgnOrWithDefined(Node definitionNode, VoidCodeBlockOne getter, CodeBlock setter) {
        Label existsDone = getNewLabel();
        Label done = getNewLabel();
        Operand def = buildGetDefinition2(definitionNode);
        Variable result = def instanceof Variable var ? var : copy(temp(), def);
        addInstr(createBranch(result, nil(), existsDone));
        getter.run(result);
        addInstr(new LabelInstr(existsDone));
        addInstr(createBranch(result, getManager().getTrue(), done));
        Operand value = setter.run();
        copy(result, value);
        addInstr(new LabelInstr(done));

        return result;
    }

    private Operand[] buildNodeList(Node[] list) {
        if (list == null || list.length == 0) return Operand.EMPTY_ARRAY;

        Operand[] args = new Operand[list.length];
        for (int i = 0; i < list.length; i++) {
            args[i] = build(list[i]);
        }

        return args;
    }

    private Operand buildArgumentsAsArgument(ArgumentsNode node) {
        Operand[] args = buildArguments(node);
        return args.length == 0 ? nil() : args.length == 1 ? args[0] : new Array(args);
    }

    private Operand buildModuleParent(Node parent) {
        return parent == null ? getCurrentModuleVariable() : build(parent);
    }
    
    private RubySymbol symbol(byte[] bytes) {
        /*
        var hit = symbols.get(bytes);

        if (hit == null) {
            hit = asSymbol(context, new ByteList(bytes, encoding));
            if (hit.idString().charAt(0) == 'R') {
                System.out.println("SAVING: " + hit + ", ENC: " + encoding);
            }
            symbols.put(bytes, hit);
        } else {
            //System.out.println("ALREADY FOUND: " + hit);
        }

        return hit;
         */
        return symbols.computeIfAbsent(bytes, (_b) -> asSymbol(context, new ByteList(bytes, encoding)));
    }

    public RubySymbol[] symbols(byte[][] tokens) {
        var names = new RubySymbol[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            names[i] = symbol(tokens[i]);
        }
        return names;
    }
}