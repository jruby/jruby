package org.jruby.ir.builder;

import org.jcodings.Encoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.ParseResult;
import org.jruby.Ruby;
import org.jruby.RubyNumeric;
import org.jruby.RubySymbol;
import org.jruby.compiler.NotCompilableException;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRManager;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRModuleBody;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScriptBody;
import org.jruby.ir.Tuple;
import org.jruby.ir.instructions.*;
import org.jruby.ir.instructions.defined.GetErrorInfoInstr;
import org.jruby.ir.instructions.defined.RestoreErrorInfoInstr;
import org.jruby.ir.operands.Array;
import org.jruby.ir.operands.CurrentScope;
import org.jruby.ir.operands.Float;
import org.jruby.ir.operands.FrozenString;
import org.jruby.ir.operands.Hash;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.MutableString;
import org.jruby.ir.operands.NullBlock;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Regexp;
import org.jruby.ir.operands.Self;
import org.jruby.ir.operands.Splat;
import org.jruby.ir.operands.Symbol;
import org.jruby.ir.operands.SymbolProc;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.parser.StaticScope;
import org.jruby.parser.StaticScopeFactory;
import org.jruby.runtime.ArgumentType;
import org.jruby.runtime.CallType;
import org.jruby.runtime.Signature;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.CommonByteLists;
import org.jruby.util.DefinedMessage;
import org.jruby.util.KeyValuePair;
import org.jruby.util.RegexpOptions;
import org.jruby.util.SafeDoubleParser;
import org.jruby.util.StringSupport;
import org.yarp.Nodes;
import org.yarp.Nodes.*;
import org.yarp.YarpParseResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.jruby.ir.instructions.RuntimeHelperCall.Methods.*;
import static org.jruby.runtime.ThreadContext.*;
import static org.jruby.util.CommonByteLists.DOLLAR_BACKTICK;

public class IRBuilderYARP extends IRBuilder<Node, DefNode, WhenNode, RescueNode> {
    String fileName = null;
    byte[] source;

    Nodes.Source nodeSource;

    StaticScope staticScope;

    public IRBuilderYARP(IRManager manager, IRScope scope, IRBuilder parent, IRBuilder variableBuilder) {
        super(manager, scope, parent, variableBuilder);

        if (parent != null) {
            source = ((IRBuilderYARP) parent).source;
            nodeSource = ((IRBuilderYARP) parent).nodeSource;
        }
        staticScope = scope.getStaticScope();
        staticScope.setFile(scope.getFile()); // staticScope and IRScope contain the same field.
    }

    @Override
    public Operand build(ParseResult result) {
        this.source = ((YarpParseResult) result).getSource();
        this.nodeSource = ((YarpParseResult) result).getSourceNode();
        return build(((YarpParseResult) result).getRoot().statements);
    }

    Operand build(Node node) {
        return build(null, node);
    }

    /*
     * @param result preferred result variable (this reduces temp vars pinning values).
     * @param node to be built
     */
    Operand build(Variable result, Node node) {
        if (node == null) return nil();

        if (node.hasNewLineFlag()) determineIfWeNeedLineNumber(getLine(node), true);

        if (node instanceof AliasGlobalVariableNode) {
            return buildAliasGlobalVariable((AliasGlobalVariableNode) node);
        } else if (node instanceof AliasMethodNode) {
            return buildAliasMethod((AliasMethodNode) node);
        } else if (node instanceof AndNode) {
            return buildAnd((AndNode) node);
        } else if (node instanceof ArrayNode) {
            return buildArray((ArrayNode) node);                   // MISSING: ArrayPatternNode
        // AssocNode, AssocSplatNode are processed by HashNode
        } else if (node instanceof BackReferenceReadNode) {
            return buildBackReferenceRead(result, (BackReferenceReadNode) node);
        } else if (node instanceof BeginNode) {
            return buildBegin((BeginNode) node);
        } else if (node instanceof BlockArgumentNode) {
            return buildBlockArgument((BlockArgumentNode) node);
        } else if (node instanceof BlockNode) {
            return buildBlock((BlockNode) node);
        // BlockParameterNode processed during call building.
        } else if (node instanceof BreakNode) {
            return buildBreak((BreakNode) node);
        } else if (node instanceof CallNode) {
            return buildCall(result, (CallNode) node, symbol(((CallNode) node).name));
        } else if (node instanceof CallAndWriteNode) {
            return buildCallAndWrite((CallAndWriteNode) node);
        } else if (node instanceof CallOrWriteNode) {
            return buildCallOrWrite((CallOrWriteNode) node);
        } else if (node instanceof CallOperatorWriteNode) { // foo.bar += baz
            return buildCallOperatorWrite((CallOperatorWriteNode) node);
        } else if (node instanceof CaseNode) {                    // MISSING: CapturePatternNode
            return buildCase((CaseNode) node);
        } else if (node instanceof ClassNode) {
            return buildClass((ClassNode) node);
        } else if (node instanceof ClassVariableAndWriteNode) {
            return buildClassAndVariableWrite((ClassVariableAndWriteNode) node);
        } else if (node instanceof ClassVariableOperatorWriteNode) {
            return buildClassVariableOperatorWrite((ClassVariableOperatorWriteNode) node);
        } else if (node instanceof ClassVariableOrWriteNode) {
            return buildClassOrVariableWrite((ClassVariableOrWriteNode) node);
        } else if (node instanceof ClassVariableReadNode) {
            return buildClassVariableRead(result, (ClassVariableReadNode) node);
        } else if (node instanceof ClassVariableWriteNode) {
            return buildClassVariableWrite((ClassVariableWriteNode) node);
        } else if (node instanceof ConstantAndWriteNode) {
            return buildConstantAndWrite((ConstantAndWriteNode) node);
        } else if (node instanceof ConstantOperatorWriteNode) {
            return buildConstantOperatorWrite((ConstantOperatorWriteNode) node);
        } else if (node instanceof ConstantOrWriteNode) {
            return buildConstantOrWrite((ConstantOrWriteNode) node);
        } else if (node instanceof ConstantPathAndWriteNode) {
            return buildConstantAndWritePath((ConstantPathAndWriteNode) node);
        } else if (node instanceof ConstantPathNode) {
            return buildConstantPath(result, (ConstantPathNode) node);
        } else if (node instanceof ConstantPathOperatorWriteNode) {
            return buildConstantPathOperatorWrite((ConstantPathOperatorWriteNode) node);
        } else if (node instanceof ConstantPathOrWriteNode) {
            return buildConstantOrWritePath((ConstantPathOrWriteNode) node);
        } else if (node instanceof ConstantPathWriteNode) {
            return buildConstantWritePath((ConstantPathWriteNode) node);
        // ConstantPathTargetNode processed in multiple assignment
        } else if (node instanceof ConstantReadNode) {
            return buildConstantRead((ConstantReadNode) node);
        } else if (node instanceof ConstantWriteNode) {
            return buildConstantWrite((ConstantWriteNode) node);
        } else if (node instanceof DefNode) {
            return buildDef((DefNode) node);
        } else if (node instanceof DefinedNode) {
            return buildDefined((DefinedNode) node);
        } else if (node instanceof ElseNode) {
            return buildElse((ElseNode) node);
        // EmbeddedStatementsNode, EmbeddedVariable handle in interpolated processing
        // MISSING: EnsureNode ???? begin will process stuff (and possibly ensure but it is unclear)
        } else if (node instanceof FalseNode) {
            return fals();
        } else if (node instanceof FloatNode) {                    // MISSING: FindPatternNode
            return buildFloat((FloatNode) node);
        } else if (node instanceof FlipFlopNode) {
            return buildFlipFlop((FlipFlopNode) node);
        } else if (node instanceof ForNode) {
            return buildFor((ForNode) node);
        // ForwardingArgumentsNode, ForwardingParametersNode process by def and call sides respectively
        } else if (node instanceof ForwardingSuperNode) {
            return buildForwardingSuper(result, (ForwardingSuperNode) node);
        } else if (node instanceof GlobalVariableAndWriteNode) {
            return buildGlobalVariableAndWrite((GlobalVariableAndWriteNode) node);
        } else if (node instanceof GlobalVariableOperatorWriteNode) {
            return buildGlobalVariableOperatorWrite((GlobalVariableOperatorWriteNode) node);
        } else if (node instanceof GlobalVariableOrWriteNode) {
            return buildGlobalVariableOrWrite((GlobalVariableOrWriteNode) node);
        } else if (node instanceof GlobalVariableReadNode) {
            return buildGlobalVariableRead(result, (GlobalVariableReadNode) node);
        // GlobalVariableTargetNode processed by muliple assignment
        } else if (node instanceof GlobalVariableWriteNode) {
            return buildGlobalVariableWrite((GlobalVariableWriteNode) node);
        } else if (node instanceof HashNode) {
            return buildHash(((HashNode) node).elements, containsVariableAssignment(node));
        } else if (node instanceof IfNode) {
            return buildIf(result, (IfNode) node);
        } else if (node instanceof InNode) {
            return buildIn((InNode) node);
        } else if (node instanceof InstanceVariableAndWriteNode) {
            return buildInstanceVariableAndWrite((InstanceVariableAndWriteNode) node);
        } else if (node instanceof InstanceVariableOperatorWriteNode) {
            return buildInstanceVariableOperatorWrite((InstanceVariableOperatorWriteNode) node);
        } else if (node instanceof InstanceVariableOrWriteNode) {
            return buildInstanceVariableOrWrite((InstanceVariableOrWriteNode) node);
        } else if (node instanceof InstanceVariableReadNode) {
            return buildInstanceVariableRead((InstanceVariableReadNode) node);
        // InstanceVariableTargetNode processed by multiple assignment
        } else if (node instanceof InstanceVariableWriteNode) {
            return buildInstanceVariableWrite((InstanceVariableWriteNode) node);
        } else if (node instanceof IntegerNode) {
            return buildInteger((IntegerNode) node);
        } else if (node instanceof InterpolatedRegularExpressionNode) {
            return buildInterpolatedRegularExpression(result, (InterpolatedRegularExpressionNode) node);
        } else if (node instanceof InterpolatedStringNode) {
            return buildInterpolatedString(result, (InterpolatedStringNode) node);
        } else if (node instanceof InterpolatedSymbolNode) {
            return buildInterpolatedSymbol(result, (InterpolatedSymbolNode) node);
        } else if (node instanceof InterpolatedXStringNode) {
            return buildInterpolatedXString(result, (InterpolatedXStringNode) node);
        } else if (node instanceof KeywordHashNode) {
            return buildKeywordHash((KeywordHashNode) node);
        // KeywordParameterNode, KeywordRestParameterNode processed by call
        } else if (node instanceof LambdaNode) {
            return buildLambda((LambdaNode) node);
        } else if (node instanceof LocalVariableAndWriteNode) {
            return buildLocalAndVariableWrite((LocalVariableAndWriteNode) node);
        } else if (node instanceof LocalVariableOperatorWriteNode) {
            return buildLocalVariableOperatorWrite((LocalVariableOperatorWriteNode) node);
        } else if (node instanceof LocalVariableOrWriteNode) {
            return buildLocalOrVariableWrite((LocalVariableOrWriteNode) node);
        } else if (node instanceof LocalVariableReadNode) {
            return buildLocalVariableRead((LocalVariableReadNode) node);
        // LocalVariableTargetNode processed by multiple assignment
        } else if (node instanceof LocalVariableWriteNode) {
            return buildLocalVariableWrite((LocalVariableWriteNode) node);
        } else if (node instanceof MissingNode) {                // MISSING: MatchPredicateNode, MatchRequiredNode
            return buildMissing((MissingNode) node);
        } else if (node instanceof ModuleNode) {
            return buildModule((ModuleNode) node);
        } else if (node instanceof MultiWriteNode) {
            return buildMultiWriteNode((MultiWriteNode) node);
        } else if (node instanceof NextNode) {
            return buildNext((NextNode) node);
        } else if (node instanceof NilNode) {
            return nil();
        // NoKeywordsParameterNode processed by def                       // MISSING: NoKeywordsParameterNode
        } else if (node instanceof NumberedReferenceReadNode) {
            return buildNumberedReferenceRead((NumberedReferenceReadNode) node);
        // OptionalParameterNode processed by def
        } else if (node instanceof OrNode) {
            return buildOr((OrNode) node);
        // ParametersNode processed by def
        } else if (node instanceof ParenthesesNode) {
            return build(((ParenthesesNode) node).body);
        // PinnedVariableNode processed by pattern matching
        } else if (node instanceof PostExecutionNode) {
            return buildPostExecution((PostExecutionNode) node);
        } else if (node instanceof PreExecutionNode) {
            return buildPreExecution((PreExecutionNode) node);
        } else if (node instanceof ProgramNode) {
            return buildProgram((ProgramNode) node);
        } else if (node instanceof RangeNode) {
            return buildRange((RangeNode) node);
        } else if (node instanceof RationalNode) {
            return buildRational((RationalNode) node);
        } else if (node instanceof RedoNode) {
            return buildRedo((RedoNode) node);
        } else if (node instanceof RegularExpressionNode) {
            return buildRegularExpression((RegularExpressionNode) node);
        // RequiredDestructuredParamterNode, RequiredParameterNode processed by def
        } else if (node instanceof RescueModifierNode) {
            return buildRescueModifier((RescueModifierNode) node);
        // RescueNode handled by begin
        // RestParameterNode handled by def
        } else if (node instanceof RetryNode) {
            return buildRetry((RetryNode) node);
        } else if (node instanceof ReturnNode) {
            return buildReturn((ReturnNode) node);
        } else if (node instanceof SelfNode) {
            return buildSelf();
        } else if (node instanceof SingletonClassNode) {
            return buildSingletonClass((SingletonClassNode) node);
        } else if (node instanceof SourceEncodingNode) {
            return buildSourceEncoding();
        } else if (node instanceof SourceFileNode) {
            return buildSourceFile();
        } else if (node instanceof SourceLineNode) {
            return buildSourceLine(node);
        } else if (node instanceof SplatNode) {
            return buildSplat((SplatNode) node);
        } else if (node instanceof StatementsNode) {
            return buildStatements((StatementsNode) node);
        } else if (node instanceof StringConcatNode) {
            return buildStringConcat((StringConcatNode) node);
        } else if (node instanceof StringNode) {
            return buildString((StringNode) node);
        } else if (node instanceof SuperNode) {
            return buildSuper(result, (SuperNode) node);
        } else if (node instanceof SymbolNode) {
            return buildSymbol((SymbolNode) node);
        } else if (node instanceof TrueNode) {
            return tru();
        } else if (node instanceof UndefNode) {
            return buildUndef((UndefNode) node);
        } else if (node instanceof UnlessNode) {
            return buildUnless(result, (UnlessNode) node);
        } else if (node instanceof UntilNode) {
            return buildUntil((UntilNode) node);
        // WhenNode processed by case
        } else if (node instanceof WhileNode) {
            return buildWhile((WhileNode) node);
        } else if (node instanceof XStringNode) {
            return buildXString(result, (XStringNode) node);
        } else if (node instanceof YieldNode) {
            return buildYield(result, (YieldNode) node);
        } else {
            throw new RuntimeException("Unhandled Node type: " + node);
        }
    }

    private Operand buildRational(RationalNode node) {
        // FIXME: need to look at lexer on how to properly calc denominator
        throw new RuntimeException("Unhandled Node type: " + node);
    }

    private Operand buildIn(InNode node) {
        throw new RuntimeException("Unhandled Node type: " + node);
    }

    private Operand buildAliasGlobalVariable(AliasGlobalVariableNode node) {
        return buildVAlias(symbol(byteListFrom(node.new_name)), symbol(byteListFrom(node.old_name)));
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

    private Operand buildArray(ArrayNode node) {
        Node[] nodes = node.elements;
        Operand[] elts = new Operand[nodes.length];
        //boolean containsAssignments = node.containsVariableAssignment();
        Operand keywordRestSplat = null;

        for (int i = 0; i < nodes.length; i++) {
            for (; i < nodes.length; i++) {
                if (nodes[i] instanceof HashNode && hasOnlyRestKwargs(((HashNode) nodes[i]).elements)) keywordRestSplat = elts[i];
                if (nodes[i] instanceof SplatNode) {
                    break;
                }
                elts[i] = build(nodes[i]);
            }
            if (i < nodes.length) { // splat found
                Operand[] lhs = new Operand[i];
                System.arraycopy(elts, 0, lhs, 0, i);
                elts = new Operand[nodes.length - i];
                // FIXME: This is broken...if more elements after splat then they need to be catted onto this (likely emulate argspush)
                return addResultInstr(new BuildCompoundArrayInstr(temp(), new Array(lhs), build(((SplatNode) nodes[i]).expression), false, keywordRestSplat != null));
                //if (keywordRestSplat != null) keywordRestSplat = null; // splat will handle this
            }
        }


        // We have some amount of ** on the end of this array construction.  This is handled in IR since we
        // do not want arrays to have to know if it has an UNDEFINED on the end and then not include it.  Also
        // since we must evaluate array values left to right we cannot look at last argument first to eliminate
        // complicating the array sizes computation.  Luckily, this is a rare feature to see used in actual code
        // so externalizing this in IR should not be a big deal.
        if (keywordRestSplat != null) {
            Operand[] lambdaElts = elts; // Allow closures to see elts as final
            Variable test = addResultInstr(new RuntimeHelperCall(temp(), IS_HASH_EMPTY, new Operand[]{ keywordRestSplat }));
            final Variable result = temp();
            if_else(test, tru(),
                    () -> copy(result, new Array(removeArg(lambdaElts))),
                    () -> copy(result, new Array(lambdaElts)));
            return result;
        } else {
            Operand array = new Array(elts);
            return copy(array);
        }
    }

    // FIXME: Attrassign should be its own node.
    // This method is called to build assignments for a multiple-assignment instruction
    public void buildAssignment(Node node, Variable rhsVal) {
        if (node == null) return; // case of 'a, = something'

        if (node instanceof CallNode) {
            RubySymbol name = symbol(new ByteList(((CallNode) node).name));
            if ("[]=".equals(name.idString())) {
                buildAttrAssignAssignment((CallNode) node, name, rhsVal);
            } else {
                throw notCompilable("call node found on lhs of masgn", node);
            }
        } else if (node instanceof ClassVariableTargetNode) {
            addInstr(new PutClassVariableInstr(classVarDefinitionContainer(), symbol(((ClassVariableTargetNode) node).name), rhsVal));
        } else if (node instanceof ConstantPathTargetNode) {
            Operand parent = buildModuleParent(((ConstantPathTargetNode) node).parent);
            // FIXME can this be anything else for child?  calls for child will just end up being callnodes.
            addInstr(new PutConstInstr(parent, symbol((((ConstantTargetNode) ((ConstantPathTargetNode) node).child)).name), rhsVal));
        } else if (node instanceof ConstantTargetNode) {
            addInstr(new PutConstInstr(getCurrentModuleVariable(), symbol(((ConstantTargetNode) node).name), rhsVal));
        } else if (node instanceof LocalVariableTargetNode) {
            LocalVariableTargetNode variable = (LocalVariableTargetNode) node;
            copy(getLocalVariable(symbol(variable.name), variable.depth), rhsVal);
        } else if (node instanceof GlobalVariableTargetNode) {
            addInstr(new PutGlobalVarInstr(symbol(((GlobalVariableWriteNode) node).name), rhsVal));
        } else if (node instanceof InstanceVariableTargetNode) {
            addInstr(new PutFieldInstr(buildSelf(), symbol(((InstanceVariableTargetNode) node).name), rhsVal));
        } else if (node instanceof MultiWriteNode) {
            buildMultiAssignment(((MultiWriteNode) node).targets, addResultInstr(new ToAryInstr(temp(), rhsVal)));
        } else if (node instanceof RequiredParameterNode) {
            RequiredParameterNode variable = (RequiredParameterNode) node;
            copy(getLocalVariable(symbol(variable.name), 0), rhsVal);
        } else if (node instanceof SplatNode) {
            buildSplat((SplatNode) node, rhsVal);
        } else {
            throw notCompilable("Can't build assignment node", node);
        }
    }

    private Operand buildModuleParent(Node parent) {
        return parent == null ? getCurrentModuleVariable() : build(parent);
    }

    // FIXME: no kwargs logic (can there be with attrassign?)
    public Operand buildAttrAssignAssignment(CallNode node, RubySymbol name, Operand value) {
        Operand obj = build(node.receiver);
        int[] flags = new int[] { 0 };
        Operand[] args = buildArguments(node.arguments);
        args = addArg(args, value);
        addInstr(AttrAssignInstr.create(scope, obj, name, args, flags[0], scope.maybeUsingRefinements()));
        return value;
    }

    // FIXME(feature): optimization simplifying this from other globals
    private Operand buildBackReferenceRead(Variable result, BackReferenceReadNode node) {
        return buildGlobalVar(result, symbol(DOLLAR_BACKTICK));
    }

    private Operand buildBreak(BreakNode node) {
        return buildBreak(() -> buildArgumentsAsArgument(node.arguments), getLine(node));
    }

    private Operand buildBegin(BeginNode node) {
        if (node.rescue_clause != null) {
            RescueNode rescue = node.rescue_clause;
            Node ensureBody = node.ensure_clause != null ? node.ensure_clause.statements : null;
            return buildEnsureInternal(node.statements, node.else_clause, rescue.exceptions, rescue.statements,
                    rescue.consequent, false, ensureBody, true, rescue.reference);
        } else if (node.ensure_clause != null) {
            EnsureNode ensure = node.ensure_clause;
            return buildEnsureInternal(node.statements, null, null, null, null, false, ensure.statements, false, null);
        }
        return build(node.statements);
    }

    private Operand buildBlock(BlockNode node) {
        StaticScope staticScope = createStaticScopeFrom(node.locals, StaticScope.Type.BLOCK);
        Signature signature = calculateSignature(node.parameters);
        staticScope.setSignature(signature);
        return buildIter(node.parameters, node.body, staticScope, signature, getLine(node), getEndLine(node));
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

    // FIXME: This is used for both for and blocks in AST but we do different path for for in YARP
    public void receiveBlockArgs(Node node) {
        if (node instanceof MultiTargetNode) { // for loops
            buildBlockArgsAssignment(node, null, 0, false);
        } else {
            BlockParametersNode parameters = (BlockParametersNode) node;
            // FIXME: Impl
            //((IRClosure) scope).setArgumentDescriptors(Helpers.argsNodeToArgumentDescriptors(((ArgsNode) args)));
            // FIXME: Missing locals?  Not sure how we handle those but I would have thought with a scope?
            if (parameters != null) buildParameters(parameters.parameters);
        }
    }

    private void buildBlockArgsAssignment(Node node, Operand argsArray, int argIndex, boolean isSplat) {
        if (node instanceof CallNode) { // attribute assignment: a[0], b = 1, 2
            buildAttrAssignAssignment((CallNode) node, symbol(((CallNode) node).name),
                    receiveBlockArg(temp(), argsArray, argIndex, isSplat));
        } else if (node instanceof LocalVariableTargetNode) {
            LocalVariableTargetNode lvar = (LocalVariableTargetNode) node;
            receiveBlockArg(getLocalVariable(symbol(lvar.name), lvar.depth), argsArray, argIndex, isSplat);
        } else if (node instanceof ClassVariableTargetNode) {
            addInstr(new PutClassVariableInstr(classVarDefinitionContainer(), symbol(((ClassVariableTargetNode) node).name),
                    receiveBlockArg(temp(), argsArray, argIndex, isSplat)));
        } else if (node instanceof ConstantTargetNode) {
            putConstant(symbol(((ConstantTargetNode) node).name), receiveBlockArg(temp(), argsArray, argIndex, isSplat));
        } else if (node instanceof GlobalVariableTargetNode) {
            addInstr(new PutGlobalVarInstr(symbol(((GlobalVariableTargetNode) node).name),
                    receiveBlockArg(temp(), argsArray, argIndex, isSplat)));
        } else if (node instanceof InstanceVariableTargetNode) {
            addInstr(new PutFieldInstr(buildSelf(), symbol(((InstanceVariableTargetNode) node).name),
                    receiveBlockArg(temp(), argsArray, argIndex, isSplat)));
        } else if (node instanceof MultiTargetNode) {
            Node[] targets = ((MultiTargetNode) node).targets;

            for (int i = 0; i < targets.length; i++) {
                buildBlockArgsAssignment(targets[i], null, i, false);
            }
        } else {
            throw notCompilable("Can't build assignment node", node);
        }
    }

    private Operand buildBlockArgument(BlockArgumentNode node) {
        if (node.expression instanceof SymbolNode && !scope.maybeUsingRefinements()) {
            return new SymbolProc(symbol(((SymbolNode) node.expression).unescaped));
        } else if (node.expression == null) {
            return getYieldClosureVariable();
        }
        return build(node.expression);
    }

    // We do name processing outside of this rather than from the node to support stripping '=' off of opelasgns
    private Operand buildCall(Variable resultArg, CallNode node, RubySymbol name) {
        Variable result = resultArg == null ? temp() : resultArg;

        // FIXME: this should just be typed by parser.
        CallType callType = determineCallType(node.receiver);
        Operand receiver = callType == CallType.FUNCTIONAL ?
                buildSelf() :
                build(node.receiver);
        // FIXME: pretend always . and not &. for now.
        // FIXME: at least type arguments to ArgumentsNode
        // FIXME: block would be a lot easier if both were in .block and not maybe in arguments
        // FIXME: Lots and lots of special logic in AST not here
        int[] flags = new int[] { 0 };
        Operand[] args = setupCallArgs(node.arguments, flags);
        int argsLength = args.length;
        Operand block;
        if (node.block != null) {
            block = setupCallClosure(node.block);
        } else if (node.arguments != null && argsLength != node.arguments.arguments.length) {
            block = build(node.arguments.arguments[argsLength]);
        } else {
            block = NullBlock.INSTANCE;
        }
        Operand[] finalArgs = args; // for lambda to see

        if ((flags[0] & CALL_KEYWORD_REST) != 0) {  // {**k}, {**{}, **k}, etc...
            Variable test = addResultInstr(new RuntimeHelperCall(temp(), IS_HASH_EMPTY, new Operand[] { args[args.length - 1] }));
            if_else(test, tru(),
                    () -> receiveBreakException(block,
                            CallInstr.create(scope, callType, result, name, receiver, removeArg(finalArgs), block, flags[0])),
                    () -> receiveBreakException(block,
                            CallInstr.create(scope, callType, result, name, receiver, finalArgs, block, flags[0])));
        } else {
            determineIfWeNeedLineNumber(getLine(node), node.hasNewLineFlag()); // buildOperand for call was papered over by args operand building so we check once more.
            receiveBreakException(block,
                    CallInstr.create(scope, callType, result, name, receiver, args, block, flags[0]));
        }

        return result;
    }

    private Operand buildCallAndWrite(CallAndWriteNode node) {
        return buildCallOperatorLogicalWrite(symbol(node.read_name), symbol(node.write_name),
                node.receiver, node.arguments, node.value, fals());
    }

    @Override
    Operand[] buildCallArgs(Node args, int[] flags) {
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

            if (child instanceof SplatNode) {
                builtArgs[i] = new Splat(addResultInstr(new BuildSplatInstr(temp(), build(((SplatNode) child).expression), false)));
            } else if (child instanceof KeywordHashNode && i == numberOfArgs - 1) {
                builtArgs[i] = buildCallKeywordArguments((KeywordHashNode) children[i], flags); // FIXME: here and possibly AST make isKeywordsHash() method.
            } else {
                builtArgs[i] = buildWithOrder(children[i], hasAssignments);
            }
        }

        return builtArgs;
    }

    private boolean containsBlockArgument(ArgumentsNode node) {
        if (node == null || node.arguments == null || node.arguments.length == 0) return false;

        return node.arguments[node.arguments.length - 1] instanceof BlockArgumentNode;
    }

    protected Operand buildCallKeywordArguments(KeywordHashNode node, int[] flags) {
        flags[0] |= CALL_KEYWORD;

        if (hasOnlyRestKwargs(node.elements)) return buildRestKeywordArgs(node, flags);

        return buildHash(node.elements, containsVariableAssignment(node.elements));
    }

    private Operand buildCallOperatorLogicalWrite(RubySymbol readName, RubySymbol writeName, Node receiverNode,
                                                  ArgumentsNode arguments, Node value, Operand comparator) {
        Operand receiver = build(receiverNode);
        Variable result;
        Operand[] args = null;
        if (arguments != null) {
            // FIXME: can [] accept kwargs?
            int[] flags = new int[]{0};
            args = buildCallArgs(arguments, flags);
            result = call(temp(), receiver, readName, args);
        } else {
            result = call(temp(), receiver, readName);
        }
        Label end = getNewLabel("end_or");
        addInstr(createBranch(result, comparator, end));  // if v1 is defined and true, we are done!
        Operand rhs = build(value); // This is an AST node that sets x = y, so nothing special to do here.
        if (args != null) {
            call(result, receiver, writeName, addArg(args, rhs));
        } else {
            call(result, receiver, writeName, rhs);
        }
        addInstr(new LabelInstr(end));
        return result;
    }

    //     foo.bar += baz
    private Operand buildCallOperatorWrite(CallOperatorWriteNode node) {
        Operand receiver = build(node.receiver);
        Variable lhs = call(temp(), receiver, symbol(node.read_name));
        Operand rhs = build(node.value);
        Operand value = call(temp(), lhs, symbol(node.operator), rhs);
        return call(temp(), receiver, symbol(node.write_name), value);
    }

    private Operand buildCallOrWrite(CallOrWriteNode node) {
        return buildCallOperatorLogicalWrite(symbol(node.read_name), symbol(node.write_name),
                node.receiver, node.arguments, node.value, tru());
    }

    private Operand buildCase(CaseNode node) {
        return buildCase(node.predicate, node.conditions, node.consequent);
    }

    private Operand buildClass(ClassNode node) {
        return buildClass(determineBaseName(node.constant_path), node.superclass, node.constant_path,
                node.body, createStaticScopeFrom(node.locals, StaticScope.Type.LOCAL), getLine(node), getEndLine(node));
    }

    private Operand buildClassVariableOperatorWrite(ClassVariableOperatorWriteNode node) {
        RubySymbol name = symbol(node.name);
        Operand lhs = buildClassVar(temp(), name);
        Operand rhs = build(node.value);
        Variable value = call(temp(), lhs, symbol(node.operator), rhs);
        addInstr(new PutClassVariableInstr(classVarDefinitionContainer(), name, value));
        return value;
    }

    private Operand buildInstanceVariableOperatorWrite(InstanceVariableOperatorWriteNode node) {
        RubySymbol name = symbol(node.name);
        Operand lhs = buildInstVar(name);
        Operand rhs = build(node.value);
        Variable value = call(temp(), lhs, symbol(node.operator), rhs);
        addInstr(new PutFieldInstr(buildSelf(), name, value));
        return value;
    }

    private Operand buildLocalVariableOperatorWrite(LocalVariableOperatorWriteNode node) {
        RubySymbol name = symbol(node.name);
        int depth = staticScope.isDefined(name.idString()) >> 16;
        Variable lhs = getLocalVariable(name, depth);
        Operand rhs = build(node.value);
        Variable value = call(lhs, lhs, symbol(node.operator), rhs);
        return value;
    }

    private Operand buildClassAndVariableWrite(ClassVariableAndWriteNode node) {
        RubySymbol name = symbol(node.name);
        return buildOpAsgnAnd(() -> addResultInstr(new GetClassVariableInstr(temp(), buildSelf(), name)),
                () -> (buildClassVarAsgn(name, node.value)));
    }

    private Operand buildClassOrVariableWrite(ClassVariableOrWriteNode node) {
        RubySymbol name = symbol(node.name);
        return buildOpAsgnOrWithDefined(node, (result) -> addInstr(new GetClassVariableInstr((Variable) result, buildSelf(), name)),
                () -> (buildClassVarAsgn(name, node.value)));
    }

    private Operand buildClassVariableRead(Variable result, ClassVariableReadNode node) {
        return buildClassVar(result, symbol(node.name));
    }

    private Operand buildClassVariableWrite(ClassVariableWriteNode node) {
        return buildClassVarAsgn(symbol(node.name), node.value);
    }

    private Operand buildConstantAndWrite(ConstantAndWriteNode node) {
        RubySymbol name = symbol(node.name);
        return buildOpAsgnAnd(() -> addResultInstr(new SearchConstInstr(temp(), CurrentScope.INSTANCE, name, false)),
                () -> (putConstant(name, build(node.value))));
    }

    private Operand buildConstantAndWritePath(ConstantPathAndWriteNode node) {
        RubySymbol name = symbol(((ConstantReadNode) node.target.child).name);
        return buildOpAsgnAnd(() -> buildConstantPath(temp(), name, node.target.parent),
                () -> buildConstantWritePath(node.target, build(node.value)));
    }

    private Operand buildConstantOperatorWrite(ConstantOperatorWriteNode node) {
        RubySymbol name = symbol(node.name);
        Operand lhs = searchConst(temp(), name);
        Operand rhs = build(node.value);
        Variable value = call(temp(), lhs, symbol(node.operator), rhs);
        putConstant(buildSelf(), name, value);
        return value;
    }

    private Operand buildConstantOrWrite(ConstantOrWriteNode node) {
        RubySymbol name = symbol(node.name);
        return buildOpAsgnOrWithDefined(node,
                (result) -> addInstr(new SearchConstInstr((Variable) result, CurrentScope.INSTANCE, name, false)),
                () -> (putConstant(name, build(node.value))));
    }

    private Operand buildConstantOrWritePath(ConstantPathOrWriteNode node) {
        RubySymbol name = symbol(((ConstantReadNode) node.target.child).name);
        return buildOpAsgnOrWithDefined(node,
                (result) -> buildConstantPath((Variable) result, name, node.target.parent),
                () -> buildConstantWritePath(node.target, build(node.value)));
    }

    private Operand buildConstantPath(Variable result, ConstantPathNode node) {
        return buildConstantPath(result, symbol(((ConstantReadNode) node.child).name), node.parent);
    }

    private Operand buildConstantPath(Variable result, RubySymbol name, Node parent) {
        Operand where = parent == null ? getManager().getObjectClass() : build(parent);
        return searchModuleForConst(result, where, name);
    }

    private Operand buildConstantPathOperatorWrite(ConstantPathOperatorWriteNode node) {
        RubySymbol name = symbol(((ConstantReadNode) node.target.child).name);
        Operand lhs = buildConstantPath(temp(), name, node.target);
        Operand rhs = build(node.value);
        Variable value = call(temp(), lhs, symbol(node.operator), rhs);
        buildConstantWritePath(node.target, value);
        return value;
    }

    private Operand buildConstantRead(ConstantReadNode node) {
        return addResultInstr(new SearchConstInstr(temp(), CurrentScope.INSTANCE, symbol(node.name), false));
    }

    private Operand buildConstantWrite(ConstantWriteNode node) {
        return putConstant(symbol(node.name), build(node.value));
    }

    private Operand buildConstantWritePath(ConstantPathWriteNode node) {
        return buildConstantWritePath(node.target, build(node.value));
    }

    // Multiple assignments provide the value otherwise it is grabbed from .value on the node.
    private Operand buildConstantWritePath(ConstantPathNode path, Operand value) {
        return putConstant(buildModuleParent(path.parent), symbol(((ConstantReadNode) path.child).name), value);
    }

    private Operand buildDef(DefNode node) {
        if (node.receiver == null) {
            return buildDefn(node);
        } else {
            return buildDefs(node);
        }
    }

    private Operand buildDefined(DefinedNode node) {
        return buildGetDefinition(node.value);
    }

    private Operand buildDefn(DefNode node) {
        // FIXME: due to how lazy methods work we need this set on method before we actually parse the method.
        StaticScope staticScope = createStaticScopeFrom(node.locals, StaticScope.Type.LOCAL);
        staticScope.setSignature(calculateSignature(node.parameters));
        LazyMethodDefinition def = new LazyMethodDefinitionYARP(getManager().getRuntime(), source, nodeSource, node);
        return buildDefn(defineNewMethod(def, symbol(node.name).getBytes(), 0, staticScope, true));
    }

    private Operand buildDefs(DefNode node) {
        // FIXME: due to how lazy methods work we need this set on method before we actually parse the method.
        StaticScope staticScope = createStaticScopeFrom(node.locals, StaticScope.Type.LOCAL);
        staticScope.setSignature(calculateSignature(node.parameters));
        LazyMethodDefinition def = new LazyMethodDefinitionYARP(getManager().getRuntime(), source, nodeSource, node);
        return buildDefs(node.receiver, defineNewMethod(def, symbol(node.name).getBytes(), 0, staticScope, false));
    }

    private Operand buildElse(ElseNode node) {
        return buildStatements(node.statements);
    }

    private Operand buildFlipFlop(FlipFlopNode node) {
        return buildFlip(node.left, node.right, node.isExcludeEnd());
    }

    // FIXME: Do we need warn or will YARP provide it.
    private Operand buildFloat(FloatNode node) {
        String number = byteListFrom(node).toString();
        double d;
        try {
            d = SafeDoubleParser.parseDouble(number);
        } catch (NumberFormatException e) {
            //warnings.warn(IRubyWarnings.ID.FLOAT_OUT_OF_RANGE, getFile(), ruby_sourceline, "Float " + number + " out of range.");

            d = number.startsWith("-") ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        }

        return new Float(d);
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

    @Override
    Operand buildGetDefinition(Node node) {
        if (node == null) return new FrozenString("expression");

        // FIXME: all opassignments needs to return assignment
        if (node instanceof ClassVariableWriteNode ||
                node instanceof ConstantPathWriteNode || node instanceof LocalVariableWriteNode ||
                node instanceof GlobalVariableWriteNode || node instanceof MultiWriteNode ||
                node instanceof InstanceVariableWriteNode) {
            return new FrozenString(DefinedMessage.ASSIGNMENT.getText());
        } else if (node instanceof OrNode || node instanceof AndNode ||
                node instanceof InterpolatedRegularExpressionNode || node instanceof InterpolatedStringNode) {
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
        } else if (node instanceof StatementsNode) {
            Node[] array = ((StatementsNode) node).body;
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
        } else if (node instanceof GlobalVariableReadNode) {
            return buildGlobalVarGetDefinition(symbol(((GlobalVariableReadNode) node).name));
        } else if (node instanceof GlobalVariableOrWriteNode) {
            return buildGlobalVarGetDefinition(symbol(((GlobalVariableOrWriteNode) node).name));
        } else if (node instanceof InstanceVariableReadNode) {
            return buildInstVarGetDefinition(symbol(((InstanceVariableReadNode) node).name));
        } else if (node instanceof InstanceVariableOrWriteNode) {
            return buildInstVarGetDefinition(symbol(((InstanceVariableOrWriteNode) node).name));
        } else if (node instanceof ClassVariableReadNode) {
            return buildClassVarGetDefinition(symbol(((ClassVariableReadNode) node).name));
        } else if (node instanceof ClassVariableOrWriteNode) {
            return buildClassVarGetDefinition(symbol(((ClassVariableOrWriteNode) node).name));
        } else if (node instanceof SuperNode) {
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
            Operand superDefnVal = buildGetArgumentDefinition(((SuperNode) node).arguments, DefinedMessage.SUPER.getText());
            return buildDefnCheckIfThenPaths(undefLabel, superDefnVal);
        } else if (node instanceof ForwardingSuperNode) {
            return addResultInstr(
                    new RuntimeHelperCall(temp(), IS_DEFINED_SUPER,
                            new Operand[] { buildSelf(), new FrozenString(DefinedMessage.SUPER.getText()) }));
        } else if (node instanceof CallNode) {
            CallNode call = (CallNode) node;
            RubySymbol name = symbol(call.name);

            if (call.receiver == null && call.arguments == null) { // VCALL
                return addResultInstr(
                        new RuntimeHelperCall(temp(), IS_DEFINED_METHOD,
                                new Operand[]{ buildSelf(), new FrozenString(name), fals(), new FrozenString(DefinedMessage.METHOD.getText()) }));
            }

            boolean isAttrAssign = "[]=".equals(name.idString());
            String type = isAttrAssign ? DefinedMessage.ASSIGNMENT.getText() : DefinedMessage.METHOD.getText();

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
                                        new Symbol(name),
                                        fals(),
                                        new FrozenString(DefinedMessage.METHOD.getText())
                                }
                        )
                );
                addInstr(createBranch(tmpVar, nil(), undefLabel));
                Operand argsCheckDefn = buildGetArgumentDefinition(((CallNode) node).arguments, type);
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
                                        new FrozenString(isAttrAssign ? DefinedMessage.ASSIGNMENT.getText() : DefinedMessage.METHOD.getText())
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
        } else if (node instanceof ConstantReadNode) {
            return buildConstantGetDefinition(symbol(((ConstantReadNode) node).name));
        } else if (node instanceof ConstantOrWriteNode) {
            return buildConstantGetDefinition(symbol(((ConstantOrWriteNode) node).name));
        } else if (node instanceof ConstantPathNode) {
            // SSS FIXME: Is there a reason to do this all with low-level IR?
            // Can't this all be folded into a Java method that would be part
            // of the runtime library, which then can be used by buildDefinitionCheck method above?
            // This runtime library would be used both by the interpreter & the compiled code!

            ConstantPathNode path = (ConstantPathNode) node;

            final RubySymbol name = symbol(((ConstantReadNode) path.child).name);
            final Variable errInfo = temp();

            // store previous exception for restoration if we rescue something
            addInstr(new GetErrorInfoInstr(errInfo));

            CodeBlock protectedCode = new CodeBlock() {
                public Operand run() {
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
                }
            };

            // Try verifying definition, and if we get an JumpException exception, process it with the rescue block above
            return protectCodeWithRescue(protectedCode, () -> {
                addInstr(new RestoreErrorInfoInstr(errInfo)); // ignore and restore (we don't care about error)
                return nil();
            });
        }

        return new FrozenString("expression");
    }

    private Operand buildGlobalVariableRead(Variable result, GlobalVariableReadNode node) {
        return buildGlobalVar(result, symbol(node.name));
    }

    private Operand buildGlobalVariableOperatorWrite(GlobalVariableOperatorWriteNode node) {
        RubySymbol name = symbol(node.name);
        Operand lhs = buildGlobalVar(temp(), name);
        Operand rhs = build(node.value);
        Variable value = call(temp(), lhs, symbol(node.operator), rhs);
        addInstr(new PutGlobalVarInstr(name, value));
        return value;
    }

    private Operand buildGlobalVariableAndWrite(GlobalVariableAndWriteNode node) {
        RubySymbol name = symbol(node.name);
        return buildOpAsgnAnd(() -> buildGlobalVar(temp(), name),
                () -> buildGlobalAsgn(name, node.value));
    }

    private Operand buildGlobalVariableOrWrite(GlobalVariableOrWriteNode node) {
        RubySymbol name = symbol(node.name);
        return buildOpAsgnOrWithDefined(node,
                (result) -> buildGlobalVar((Variable) result, name),
                () -> buildGlobalAsgn(name, node.value));
    }

    private Operand buildGlobalVariableWrite(GlobalVariableWriteNode node) {
        return buildGlobalAsgn(symbol(node.name), node.value);
    }

    private Operand buildHash(Node[] elements, boolean hasAssignments) {
        List<KeyValuePair<Operand, Operand>> args = new ArrayList<>();
        Variable hash = null;
        // Duplication checks happen when **{} are literals and not **h variable references.
        Operand duplicateCheck = fals();

        // pair is AssocNode or AssocSplatNode
        for (Node pair: elements) {
            Operand keyOperand;

            if (pair instanceof AssocNode) {
                keyOperand = build(((AssocNode) pair).key);
                args.add(new KeyValuePair<>(keyOperand, buildWithOrder(((AssocNode) pair).value, hasAssignments)));
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
        return buildConditional(result, node.predicate, node.statements, node.consequent);
    }

    private Operand buildInstanceVariableRead(InstanceVariableReadNode node) {
        return buildInstVar(symbol(node.name));
    }

    private Operand buildInstanceVariableAndWrite(InstanceVariableAndWriteNode node) {
        RubySymbol name = symbol(node.name);
        return buildOpAsgnAnd(() -> addResultInstr(new GetFieldInstr(temp(), buildSelf(), name, false)),
                () -> buildInstAsgn(name, node.value));
    }

    private Operand buildInstanceVariableOrWrite(InstanceVariableOrWriteNode node) {
        RubySymbol name = symbol(node.name);
        return buildOpAsgnOr(() -> addResultInstr(new GetFieldInstr(temp(), buildSelf(), name, false)),
                () -> buildInstAsgn(name, node.value));
    }

    private Operand buildInstanceVariableWrite(InstanceVariableWriteNode node) {
        return buildInstAsgn(symbol(node.name), node.value);
    }

    private Operand buildInteger(IntegerNode node) {
        // FIXME: HAHAHAH horrible hack around integer being too much postprocessing.
        ByteList value = byteListFrom(node);

        return fix(RubyNumeric.fix2long(RubyNumeric.str2inum(getManager().runtime, getManager().getRuntime().newString(value), 10)));
    }

    private Operand buildInterpolatedRegularExpression(Variable result, InterpolatedRegularExpressionNode node) {
        //String opts = new String(byteListFrom(node.closing).getUnsafeBytes()).substring(1);
        //RegexpOptions options = RegexpOptions.newRegexpOptions(opts);
        // FIXME: missing options token
        return buildDRegex(result, node.parts, RegexpOptions.newRegexpOptions(""));
    }

    private Operand buildInterpolatedString(Variable result, InterpolatedStringNode node) {
        // FIXME: Missing encoding, frozen
        return buildDStr(result, node.parts, UTF8Encoding.INSTANCE, false, getLine(node));
    }

    private Operand buildInterpolatedSymbol(Variable result, InterpolatedSymbolNode node) {
        // FIXME: Missing encoding
        return buildDSymbol(result, node.parts, UTF8Encoding.INSTANCE, getLine(node));
    }

    private Operand buildInterpolatedXString(Variable result, InterpolatedXStringNode node) {
        return buildDXStr(result, node.parts, getEncoding(), getLine(node));
    }

    // FIXME: This and buildHash probably have different logic now.
    private Operand buildKeywordHash(KeywordHashNode node) {
        List<KeyValuePair<Operand, Operand>> args = new ArrayList<>();
        boolean hasAssignments = containsVariableAssignment(node);
        Variable hash = null;
        // Duplication checks happen when **{} are literals and not **h variable references.
        Operand duplicateCheck = fals();

        // pair is AssocNode or AssocSplatNode
        for (Node pair: node.elements) {
            Operand keyOperand;

            if (pair instanceof AssocNode) {
                keyOperand = build(((AssocNode) pair).key);
                args.add(new KeyValuePair<>(keyOperand, buildWithOrder(((AssocNode) pair).value, hasAssignments)));
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

    private Operand buildLambda(LambdaNode node) {
        StaticScope staticScope = createStaticScopeFrom(node.locals, StaticScope.Type.BLOCK);
        Signature signature = calculateSignature(node.parameters);
        staticScope.setSignature(signature);
        return buildLambda(node.parameters, node.body, staticScope, signature, getLine(node));
    }

    private Operand buildLocalVariableRead(LocalVariableReadNode node) {
        return getLocalVariable(symbol(node.name), node.depth);
    }

    private Operand buildLocalAndVariableWrite(LocalVariableAndWriteNode node) {
        RubySymbol name = symbol(node.name);
        return buildOpAsgnAnd(() -> getLocalVariable(name, node.depth),
                () -> buildLocalVariableAssign(name, node.depth, node.value));
    }

    private Operand buildLocalOrVariableWrite(LocalVariableOrWriteNode node) {
        RubySymbol name = symbol(node.name);
        return buildOpAsgnOr(() -> getLocalVariable(name, node.depth),
                () -> buildLocalVariableAssign(name, node.depth, node.value));
    }

    private Operand buildLocalVariableWrite(LocalVariableWriteNode node) {
        return buildLocalVariableAssign(symbol(node.name), node.depth, node.value);
    }

    private Operand buildMissing(MissingNode node) {
        System.out.println("uh oh");
        return nil();
    }

    private Operand buildModule(ModuleNode node) {
        return buildModule(determineBaseName(node.constant_path), node.constant_path, node.body,
                createStaticScopeFrom(node.locals, StaticScope.Type.LOCAL),
                getLine(node), getEndLine(node));
    }

    private Operand buildMultiWriteNode(MultiWriteNode node) {
        Node valueNode = node.value;
        Operand values = build(valueNode);
        Variable ret = getValueInTemporaryVariable(values);
        if (valueNode instanceof ArrayNode) {
            buildMultiAssignment(node.targets, ret);
            // FIXME: need to know equiv of ILiteralNode so we can opt this case.
        /*} else if (valueNode instanceof ILiteralNode) {
            // treat a single literal value as a single-element array
            buildMultiAssignment(node, new Array(new Operand[]{ret}));*/
        } else {
            Variable tmp = addResultInstr(new ToAryInstr(temp(), ret));
            buildMultiAssignment(node.targets, tmp);
        }
        return ret;
    }

    // SplatNode, MultiWriteNode, LocalVariableWrite and lots of other normal writes
    private void buildMultiAssignment(Node[] targets, Operand values) {
        final List<Tuple<Node, Variable>> assigns = new ArrayList<>();
        int length = targets.length;
        int restIndex = -1;

        // FIXME: May be nice to have more info in MultiWriteNode rather than walking it twice
        for (int i = 0; i < length; i++) { // Figure out indices
            Node child = targets[i];

            if (child instanceof  SplatNode) {
                restIndex = i;
            }
        }

        int preCount = restIndex == -1 ? length : restIndex;
        int postCount = restIndex == -1 ? -1 : length - restIndex - 1;

        for (int i = 0; i < preCount; i++) {
            assigns.add(new Tuple<>(targets[i], addResultInstr(new ReqdArgMultipleAsgnInstr(temp(), values, i))));
        }

        if (restIndex >= 0) {
            Node realTarget = ((SplatNode) targets[restIndex]).expression;
            assigns.add(new Tuple<>(realTarget, addResultInstr(new RestArgMultipleAsgnInstr(temp(), values, 0, restIndex, postCount))));
        }

        for (int i = 0; i < postCount; i++) {  // we increment by 1 to skip past rest.
            assigns.add(new Tuple<>(targets[restIndex + i + 1], addResultInstr(new ReqdArgMultipleAsgnInstr(temp(), values, i, restIndex + i, postCount))));
        }

        for (Tuple<Node, Variable> assign: assigns) {
            buildAssignment(assign.a, assign.b);
        }
    }

    private Operand buildNext(NextNode node) {
        return buildNext(buildArgumentsAsArgument(node.arguments), getLine(node));
    }

    private Operand buildNumberedReferenceRead(NumberedReferenceReadNode node) {
        ByteList num = byteListFrom(node);
        num.view(1, num.realSize() - 1);
        int value = Integer.parseInt(num.toString());
        return buildNthRef(value);
    }

    private Operand buildOr(OrNode node) {
        return buildOr(build(node.left), () -> build(node.right), binaryType(node.left));
    }

    private void buildParameters(ParametersNode parameters) {
        if (parameters == null) return;
        boolean hasRest = parameters.rest != null;
        boolean hasKeywords = parameters.keywords.length != 0 || parameters.keyword_rest != null;
        Variable keywords = addResultInstr(new ReceiveKeywordsInstr(temp(), hasRest, hasKeywords));

        // 1.9 pre, opt, rest, post args
        receiveNonBlockArgs(parameters, keywords, hasKeywords);

        // 2.0 keyword args
        if (hasKeywords) {
            int keywordsCount = parameters.keywords.length;
            Node[] kwArgs = parameters.keywords;
            for (int i = 0; i < keywordsCount; i++) {
                KeywordParameterNode kwarg = (KeywordParameterNode) kwArgs[i];

                RubySymbol key = symbol(kwarg.name);
                Variable av = getNewLocalVariable(key, 0);
                Label l = getNewLabel();
                if (scope instanceof IRMethod) addKeyArgDesc(kwarg, key);
                addInstr(new ReceiveKeywordArgInstr(av, keywords, key));
                addInstr(BNEInstr.create(l, av, UndefinedValue.UNDEFINED)); // if 'av' is not undefined, we are done

                // Required kwargs have no value and check_arity will throw if they are not provided.
                if (kwarg.value != null) {
                    addInstr(new CopyInstr(av, nil())); // wipe out undefined value with nil
                    // FIXME: this is performing extra copy but something is generating a temp and not using local if we pass it to build
                    copy(av, build(kwarg.value));
                } else {
                    addInstr(new RaiseRequiredKeywordArgumentError(key));
                }
                addInstr(new LabelInstr(l));
            }
        }

        // 2.0 keyword rest arg
        KeywordRestParameterNode keyRest = (KeywordRestParameterNode) parameters.keyword_rest;
        if (keyRest != null) {
            RubySymbol key = symbol(keyRest.name);
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

        receiveBlockArg(parameters.block);

    }

    private Operand buildPostExecution(PostExecutionNode node) {
        return buildPostExe(node.statements, getLine(node));
    }

    private Operand buildPreExecution(PreExecutionNode node) {
        return buildPreExe(node.statements);
    }

    private Operand buildRange(RangeNode node) {
        return buildRange(node.left, node.right, node.isExcludeEnd());
    }

    private Operand buildRedo(RedoNode node) {
        return buildRedo(getLine(node));
    }

    private Operand buildRegularExpression(RegularExpressionNode node) {
        return new Regexp(byteListFrom(node.content_loc), RegexpOptions.fromJoniOptions(node.flags));
    }

    private Operand buildRescueModifier(RescueModifierNode node) {
        return buildEnsureInternal(node.expression, null, null, node.rescue_expression, null, true, null, true, null);
    }

    private Operand buildRetry(RetryNode node) {
        return buildRetry(getLine(node));
    }

    private Operand buildReturn(ReturnNode node) {
        return buildReturn(operandListToOperand(buildArguments(node.arguments)), getLine(node));
    }

    private Operand buildRestKeywordArgs(KeywordHashNode keywordArgs, int[] flags) {
        flags[0] |= CALL_KEYWORD_REST;
        Node[] pairs = keywordArgs.elements;
        boolean containsVariableAssignment = containsVariableAssignment(keywordArgs);

        if (pairs.length == 1) { // Only a single rest arg here.  Do not bother to merge.
            Operand splat = buildWithOrder(((AssocSplatNode) pairs[0]).value, containsVariableAssignment);

            return addResultInstr(new RuntimeHelperCall(temp(), HASH_CHECK, new Operand[] { splat }));
        }

        Variable splatValue = copy(new Hash(new ArrayList<>()));
        for (int i = 0; i < pairs.length; i++) {
            Operand splat = buildWithOrder(pairs[i], containsVariableAssignment);
            addInstr(new RuntimeHelperCall(splatValue, MERGE_KWARGS, new Operand[] { splatValue, splat, fals() }));
        }

        return splatValue;
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
        return buildSplat(node, build(node.expression));
    }

    private Operand buildSplat(SplatNode node, Operand value) {
        return addResultInstr(new BuildSplatInstr(temp(), value, true));
    }

    public Operand buildStrRaw(StringNode node) {
        int line = getLine(node);

        // FIXME: Need to know if it is frozen
        //if (strNode.isFrozen()) return new FrozenString(strNode.getValue(), strNode.getCodeRange(), scope.getFile(), line);

        // FIXME: need coderange.
        return new MutableString(bytelist(node.unescaped), 0, scope.getFile(), line);
    }

    private Operand buildSuper(Variable result, SuperNode node) {
        return buildSuper(result, node.block, node.arguments, getLine(node), node.hasNewLineFlag());
    }

    private Operand buildSymbol(SymbolNode node) {
        return new Symbol(symbol(node.unescaped));
    }

    private Operand buildUndef(UndefNode node) {
        return buildUndef(operandListToOperand(buildNodeList(node.names)));
    }

    private Operand operandListToOperand(Operand[] args) {
        switch (args.length) {
            case 0: return nil();
            case 1: return args[0];
            default: return new Array(args);
        }
    }

    private Operand buildUnless(Variable result, UnlessNode node) {
        return buildConditional(result, node.predicate, node.consequent, node.statements);
    }

    private Operand buildUntil(UntilNode node) {
        return buildConditionalLoop(node.predicate, node.statements, false, !node.isBeginModifier());
    }

    private void buildWhenSplatValues(Variable eqqResult, Node node, Operand testValue, Label bodyLabel,
                                      Set<IRubyObject> seenLiterals) {
        // FIXME: could see errors since this is missing whatever is YARP args{cat,push}?
        if (node instanceof StatementsNode) {
            buildWhenValues(eqqResult, ((StatementsNode) node).body, testValue, bodyLabel, seenLiterals);
        } else if (node instanceof SplatNode) {
            buildWhenValue(eqqResult, testValue, bodyLabel, node, seenLiterals, true);
        } else {
            buildWhenValue(eqqResult, testValue, bodyLabel, node, seenLiterals, true);
        }
    }

    private Operand buildWhile(WhileNode node) {
        return buildConditionalLoop(node.predicate, node.statements, true, true);
    }

    // FIXME: implement
    @Override
    boolean alwaysFalse(Node node) {
        return false;
    }

    // FIXME: implement
    @Override
    boolean alwaysTrue(Node node) {
        return false;
    }

    @Override
    Node[] exceptionNodesFor(RescueNode node) {
        return node.exceptions;
    }

    @Override
    Node bodyFor(RescueNode node) {
        return node.statements;
    }

    @Override
    RescueNode optRescueFor(RescueNode node) {
        return node.consequent;
    }

    // FIXME: Implement
    @Override
    boolean isSideEffectFree(Node node) {
        return false;
    }

    // FIXME: Implement
    @Override
    boolean isErrorInfoGlobal(Node body) {
        return false;
    }

    int dynamicPiece(Operand[] pieces, int i, Node pieceNode, boolean interpolated) {
        Operand piece;

        // somewhat arbitrary minimum size for interpolated values
        int estimatedSize = 4;

        while (true) { // loop to unwrap EvStr

            // FIXME: missing EmbddedVariableNode.
            if (pieceNode instanceof StringNode) {
                piece = buildString((StringNode) pieceNode, interpolated);
                estimatedSize = bytelist(((StringNode) pieceNode).unescaped).realSize();
            } else if (pieceNode instanceof EmbeddedStatementsNode) {
                if (scope.maybeUsingRefinements()) {
                    // refined asString must still go through dispatch
                    Variable result = temp();
                    addInstr(new AsStringInstr(scope, result, build(((EmbeddedStatementsNode) pieceNode).statements), scope.maybeUsingRefinements()));
                    piece = result;
                } else {
                    // evstr/asstring logic lives in BuildCompoundString now, unwrap and try again
                    pieceNode = ((EmbeddedStatementsNode) pieceNode).statements;
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
            RubySymbol argName = symbol(blockArg.name);
            Variable blockVar = argumentResult(argName);
            if (scope instanceof IRMethod) addArgumentDescription(ArgumentType.block, argName);
            Variable tmp = temp();
            addInstr(new LoadImplicitClosureInstr(tmp));
            addInstr(new ReifyClosureInstr(blockVar, tmp));
        }
    }

    protected void receiveNonBlockArgs(ParametersNode args, Variable keywords, boolean hasKeywords) {
        int preCount = args.requireds.length;
        int optCount = args.optionals.length;
        boolean hasRest = args.rest != null;
        int keywordsCount = args.keywords.length;
        int keyRest = preCount + optCount; // FIXME: I think this is ok?
        int postCount = args.posts.length;
        int requiredCount = preCount + postCount;

        // FIXME: setSignature here since YARP is not doing this during parse
        //Signature signature = scope.getStaticScope().getSignature();

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
                if (scope instanceof IRMethod) addArgumentDescription(ArgumentType.opt, argName);
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
            RestParameterNode restArgNode = args.rest;
            if (scope instanceof IRMethod) {
                // FIXME: how do we annotate generated AST types to have isAnonymous etc...
                if (restArgNode.name == null) {
                    addArgumentDescription(ArgumentType.anonrest, symbol("*"));
                } else {
                    addArgumentDescription(ArgumentType.rest, symbol(restArgNode.name));
                }
            }

            RubySymbol argName =  restArgNode.name == null ? symbol(CommonByteLists.STAR) : symbol(restArgNode.name);

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

    public void receivePreArg(Node node, Variable keywords, int argIndex) {
        if (node instanceof RequiredParameterNode) {
            RubySymbol name = symbol(((RequiredParameterNode) node).name);

            if (scope instanceof IRMethod) addArgumentDescription(ArgumentType.req, name);

            addInstr(new ReceivePreReqdArgInstr(argumentResult(name), keywords, argIndex));
        } else if (node instanceof RequiredDestructuredParameterNode) {
                Variable v = temp();
                addInstr(new ReceivePreReqdArgInstr(v, keywords, argIndex));
                if (scope instanceof IRMethod) addArgumentDescription(ArgumentType.anonreq, null);
                Variable tmp = temp();
                addInstr(new ToAryInstr(tmp, v));

                buildMultiAssignment(((RequiredDestructuredParameterNode) node).parameters, tmp);
        } else {
            throw notCompilable("Can't build required parameter node", node);
        }
    }

    public void receivePostArg(Node node, Variable keywords, int argIndex, int preCount, int optCount, boolean hasRest, int postCount) {
        if (node instanceof RequiredParameterNode) {
            RubySymbol argName = symbol(((RequiredParameterNode) node).name);

            if (scope instanceof IRMethod) addArgumentDescription(ArgumentType.req, argName);

            addInstr(new ReceivePostReqdArgInstr(argumentResult(argName), keywords, argIndex, preCount, optCount, hasRest, postCount));
        } else if (node instanceof RequiredDestructuredParameterNode) {
            Variable v = temp();
            addInstr(new ReceivePostReqdArgInstr(v, keywords, argIndex, preCount, optCount, hasRest, postCount));

            if (scope instanceof IRMethod) addArgumentDescription(ArgumentType.anonreq, null);

            Variable tmp = temp();
            addInstr(new ToAryInstr(tmp, v));

            buildMultiAssignment(((RequiredDestructuredParameterNode) node).parameters, tmp);
        } else {
            throw notCompilable("Can't build required parameter node", node);
        }
    }

    private void addKeyArgDesc(KeywordParameterNode param, RubySymbol key) {
        addArgumentDescription(param.value == null ? ArgumentType.keyreq : ArgumentType.key, key);
    }

    private Operand buildProgram(ProgramNode node) {
        return build(node.statements);
    }

    private Operand buildStatements(StatementsNode node) {
        Operand result = temp();
        for (Node child: node.body) {
            result = build(child);
        }
        return result;
    }

    private Operand buildString(StringNode node) {
        boolean interpolated = node.opening_loc != null && source[node.opening_loc.startOffset] != '\'';
        return buildString(node, interpolated);
    }

    private Operand buildString(StringNode node, boolean interpolated) {
        // FIXME: No code range
        // FIXME: how can we tell if frozen or not

        // no opening is DNode-like string fragments.
        if (interpolated) {
            return new MutableString(bytelist(node.unescaped), 0, scope.getFile(), getLine(node));
        } else {
            return new MutableString(byteListFrom(node.content_loc), 0, scope.getFile(), getLine(node));
        }
    }

    private Operand buildStringConcat(StringConcatNode node) {
        // FIXME: maybe frozen maybe not?
        boolean isInterpolated = node.left instanceof InterpolatedStringNode ||
                node.right instanceof InterpolatedStringNode;

        if (isInterpolated) {
            Node[] pieces;

            if (node.left instanceof InterpolatedStringNode) {
                pieces = ((InterpolatedStringNode) node.left).parts;
            } else {
                pieces = new Node[] { node.left };
            }

            if (node.right instanceof InterpolatedStringNode) {
                Node[] otherPieces = ((InterpolatedStringNode) node.right).parts;
                Node[] temp = pieces;

                pieces = new Node[pieces.length + otherPieces.length];
                System.arraycopy(temp, 0, pieces, 0, temp.length);
                System.arraycopy(otherPieces, 0, pieces, temp.length, otherPieces.length);
            } else {
                Node[] temp = pieces;

                pieces = new Node[pieces.length + 1];
                System.arraycopy(temp, 0, pieces, 0, temp.length);
                pieces[temp.length] = node.right;
            }

            return buildDStr(temp(), pieces, UTF8Encoding.INSTANCE, false, getLine(node));
        }

        ByteList str = byteListFrom(node.left);
        str.append(byteListFrom(node.right));

        return new FrozenString(str, 0, getFileName(), getLine(node));
    }

    @Override
    void buildWhenArgs(WhenNode whenNode, Operand testValue, Label bodyLabel, Set<IRubyObject> seenLiterals) {
        Variable eqqResult = temp();
        Node[] exprNodes = whenNode.conditions;

        if (exprNodes.length == 1) {
            if (exprNodes[0] instanceof SplatNode) {
                buildWhenSplatValues(eqqResult, exprNodes[0], testValue, bodyLabel, seenLiterals);
            } else {
                buildWhenValue(eqqResult, testValue, bodyLabel, exprNodes[0], seenLiterals, false);
            }
        } else {
            buildWhenValues(eqqResult, exprNodes, testValue, bodyLabel, seenLiterals);
        }
    }

    private Operand buildXString(Variable result, XStringNode node) {
        ByteList value = new ByteList(node.unescaped, getEncoding());
        int codeRange = StringSupport.codeRangeScan(value.getEncoding(), value);
        return fcall(result, Self.SELF, "`", new FrozenString(value, codeRange, scope.getFile(), getLine(node)));
    }

    Operand buildYield(Variable result, YieldNode node) {
        if (result == null) result = temp();
        if (scope instanceof IRScriptBody || scope instanceof IRModuleBody) throwSyntaxError(getLine(node), "Invalid yield");

        boolean unwrap = true;
        Operand value;
        int[] flags = new int[]{0};
        if (node.arguments != null) {
            Node[] args = node.arguments.arguments;
            // Get rid of one level of array wrapping
            if (args != null && args.length == 1) {
                // We should not unwrap if it is a keyword argument.
                if (!(args[0] instanceof KeywordHashNode)) {
                    unwrap = false;
                }
            }

            value = buildYieldArgs(args, flags);
        } else {
            value = UndefinedValue.UNDEFINED;
        }

        addInstr(new YieldInstr(result, getYieldClosureVariable(), value, flags[0], unwrap));

        return result;
    }

    Operand buildYieldArgs(Node[] args, int[] flags) {
        if (args == null) return UndefinedValue.UNDEFINED;

        if (args.length == 1) {
            if (args[0] instanceof SplatNode) {
                flags[0] |= CALL_SPLATS;
                return new Splat(addResultInstr(new BuildSplatInstr(temp(), build(args[0]), false)));
            }

            return build(args[0]);
        }

        return new Array(buildCallArgsArray(args, flags));
    }

    // FIXME: needs to derive this walking down tree.
    @Override
    boolean containsVariableAssignment(Node node) {
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
    Operand frozen_string(Node node) {
        return buildStrRaw((StringNode) node);
    }

    @Override
    Operand getContainerFromCPath(Node node) {

        if (node instanceof ConstantReadNode) {
            return findContainerModule();
        } else if (node instanceof ConstantPathNode) {
            ConstantPathNode path = (ConstantPathNode) node;

            if (path.parent == null) { // ::Foo
                return getManager().getObjectClass();
            } else {
                return build(path.parent);
            }
        }
        // FIXME: We may not need these based on whether there are more possible nodes.
        throw notCompilable("Unsupported node in module path", node);
    }

    private Encoding getEncoding() {
        // FIXME: impl
        return UTF8Encoding.INSTANCE;
    }

    private int getEndLine(Node node) {
        return 0;
        // FIXME: assert during test:jruby
        //return nodeSource.line(node.endOffset());
    }

    @Override
    int getLine(Node node) {
        // internals expect 0-based value.
        return nodeSource.line(node.startOffset) - 1;
    }

    @Override
    IRubyObject getWhenLiteral(Node node) {
        Ruby runtime = scope.getManager().getRuntime();

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
            return runtime.getNil();
        } else if (node instanceof TrueNode) {
            return runtime.getTrue();
        } else if (node instanceof FalseNode) {
            return runtime.getFalse();
        } else if (node instanceof SymbolNode) {
            return symbol(((SymbolNode) node).unescaped);
        } else if (node instanceof StringNode) {
            return runtime.newString((bytelist(((StringNode) node).unescaped)));
        }

        return null;
    }

    @Override
    boolean isLiteralString(Node node) {
        return node instanceof StringNode;
    }

    // FIXME: This only seems to be used in opelasgnor on the first element (lhs) but it is very unclear what is possible here.
    @Override
    boolean needsDefinitionCheck(Node node) {
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
    public void receiveMethodArgs(DefNode defNode) {
        buildParameters(defNode.parameters);
    }

    Operand setupCallClosure(Node node) {
        if (node == null) return NullBlock.INSTANCE;

        if (node instanceof BlockNode) {
            return build(node);
        } else if (node instanceof BlockArgumentNode) {
            return buildBlockArgument((BlockArgumentNode) node);
        }

        throw notCompilable("Encountered unexpected block node", node);
    }

    private ByteList byteListFrom(Location location) {
        return new ByteList(source, location.startOffset, location.length);
    }

    private ByteList byteListFrom(Node node) {
        return new ByteList(source, node.startOffset, node.length);
    }

    private ByteList bytelist(byte[] bytes) {
        return new ByteList(bytes);
    }

    public static Signature calculateSignature(ParametersNode parameters) {
        if (parameters == null) return Signature.NO_ARGUMENTS;

        int pre = parameters.requireds.length;
        int opt = parameters.optionals.length;
        int post = parameters.posts.length;
        int kws = parameters.keywords.length;
        // FIXME: this needs more than norm
        Signature.Rest rest = parameters.rest == null ? Signature.Rest.NONE : Signature.Rest.NORM;

        int keywordRestIndex = parameters.keyword_rest == null ? -1 : pre + opt + post + kws;
        // FIXME: need to diff opt kws vs req kws
        return new Signature(pre, opt, post, rest, kws, kws, keywordRestIndex);
    }

    private Signature calculateSignature(BlockParametersNode parameters) {
        return parameters == null ? Signature.NO_ARGUMENTS : calculateSignature(parameters.parameters);
    }

    private Signature calculateSignatureFor(Node node) {
        // FIXME: #1383 will change all this for masn case so impl once done
        return Signature.ONE_REQUIRED;
    }

    // FIXME: we allocate extra byte[] just to make it a String[].  Extra work to consider?
    public StaticScope createStaticScopeFrom(byte[][] tokens, StaticScope.Type type) {
        return createStaticScopeFrom(fileName, tokens, type, staticScope);
    }

    public static StaticScope createStaticScopeFrom(String fileName, byte[][] tokens, StaticScope.Type type, StaticScope parent) {
        String[] strings = new String[tokens.length];
        // FIXME: this should be iso_8859_1 strings and not default charset.
        for(int i = 0; i < tokens.length; i++) {
            strings[i] = new String(tokens[i]);
        }

        // FIXME: keywordArgIndex?
        return StaticScopeFactory.newStaticScope(parent, type, fileName, strings, -1);
    }

    private CallType determineCallType(Node node) {
        return node == null ?
                CallType.FUNCTIONAL :
                CallType.NORMAL;
    }

    private ByteList determineBaseName(Node node) {
        if (node instanceof ConstantReadNode) {
            return bytelist(((ConstantReadNode) node).name);
        } else if (node instanceof ConstantPathNode) {
            return determineBaseName(((ConstantPathNode) node).child);
        }
        throw notCompilable("Unsupported node in module path", node);
    }

    // FIXME: need to know about breaks
    boolean canBeLazyMethod(DefNode node) {
        return true;
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

    Node referenceFor(RescueNode node) {
        return node.reference;
    }

    @Override
    Node whenBody(WhenNode arm) {
        return arm.statements;
    }
}
