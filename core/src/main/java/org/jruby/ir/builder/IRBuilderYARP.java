package org.jruby.ir.builder;

import org.jcodings.specific.UTF8Encoding;
import org.jruby.ParseResult;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
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
import org.jruby.ir.instructions.AsStringInstr;
import org.jruby.ir.instructions.AttrAssignInstr;
import org.jruby.ir.instructions.BNEInstr;
import org.jruby.ir.instructions.BlockGivenInstr;
import org.jruby.ir.instructions.BuildSplatInstr;
import org.jruby.ir.instructions.CallInstr;
import org.jruby.ir.instructions.CheckArityInstr;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.InheritanceSearchConstInstr;
import org.jruby.ir.instructions.JumpInstr;
import org.jruby.ir.instructions.LabelInstr;
import org.jruby.ir.instructions.LexicalSearchConstInstr;
import org.jruby.ir.instructions.LoadImplicitClosureInstr;
import org.jruby.ir.instructions.PutClassVariableInstr;
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
import org.jruby.ir.instructions.ReturnInstr;
import org.jruby.ir.instructions.RuntimeHelperCall;
import org.jruby.ir.instructions.SearchConstInstr;
import org.jruby.ir.instructions.ToAryInstr;
import org.jruby.ir.instructions.TraceInstr;
import org.jruby.ir.instructions.YieldInstr;
import org.jruby.ir.instructions.defined.GetErrorInfoInstr;
import org.jruby.ir.instructions.defined.RestoreErrorInfoInstr;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.operands.Array;
import org.jruby.ir.operands.CurrentScope;
import org.jruby.ir.operands.FrozenString;
import org.jruby.ir.operands.Hash;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.MutableString;
import org.jruby.ir.operands.NullBlock;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Regexp;
import org.jruby.ir.operands.Splat;
import org.jruby.ir.operands.Symbol;
import org.jruby.ir.operands.SymbolProc;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
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
import org.yarp.Nodes.*;
import org.yarp.YarpParseResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.jruby.ir.instructions.RuntimeHelperCall.Methods.*;
import static org.jruby.runtime.ThreadContext.*;

public class IRBuilderYARP extends IRBuilder<Node, DefNode, WhenNode> {
    byte[] source = null;

    public IRBuilderYARP(IRManager manager, IRScope scope, IRBuilder parent, IRBuilder variableBuilder) {
        super(manager, scope, parent, variableBuilder);

        // FIXME: remove once all paths consstently use same parser.
        if (parent instanceof IRBuilderYARP) {
            source = ((IRBuilderYARP) parent).source;
        }
    }

    @Override
    public Operand build(ParseResult result) {
        this.source = ((YarpParseResult) result).getSource();
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

        // FIXME: Need node types if this is how we process
        if (node instanceof AliasNode) {
            return buildAlias((AliasNode) node);
        } else if (node instanceof AndNode) {
            return buildAnd((AndNode) node);
        } else if (node instanceof ArrayNode) {
            return buildArray((ArrayNode) node);
        } else if (node instanceof BeginNode) {
            return buildBegin((BeginNode) node);
        } else if (node instanceof BlockNode) {
            return buildBlock((BlockNode) node);
        } else if (node instanceof BlockArgumentNode) {
            return buildBlockArgument((BlockArgumentNode) node);
        } else if (node instanceof CallNode) {
            return buildCall(result, (CallNode) node);
        } else if (node instanceof CaseNode) {
            return buildCase((CaseNode) node);
        } else if (node instanceof ClassNode) {
            return buildClass((ClassNode) node);
        } else if (node instanceof ClassVariableReadNode) {
            return buildClassVariableRead(result, (ClassVariableReadNode) node);
        } else if (node instanceof ClassVariableWriteNode) {
            return buildClassVariableWrite((ClassVariableWriteNode) node);
        } else if (node instanceof ConstantPathNode) {
            return buildConstantPath(result, (ConstantPathNode) node);
        } else if (node instanceof ConstantPathWriteNode) {
            return buildConstantWritePath((ConstantPathWriteNode) node);
        } else if (node instanceof ConstantReadNode) {
            return buildConstantRead((ConstantReadNode) node);
        } else if (node instanceof DefNode) {
            return buildDef((DefNode) node);
        } else if (node instanceof DefinedNode) {
            return buildDefined((DefinedNode) node);
        } else if (node instanceof ElseNode) {
            return buildElse((ElseNode) node);
        } else if (node instanceof FalseNode) {
            return fals();
        } else if (node instanceof ForwardingSuperNode) {
            return buildForwardingSuper(result, (ForwardingSuperNode) node);
        } else if (node instanceof GlobalVariableReadNode) {
            return buildGlobalVariableRead(result, (GlobalVariableReadNode) node);
        } else if (node instanceof GlobalVariableWriteNode) {
            return buildGlobalVariableWrite((GlobalVariableWriteNode) node);
        } else if (node instanceof HashNode) {
            return buildHash((HashNode) node);
        } else if (node instanceof IfNode) {
            return buildIf(result, (IfNode) node);
        } else if (node instanceof InstanceVariableReadNode) {
            return buildInstanceVariableRead((InstanceVariableReadNode) node);
        } else if (node instanceof InstanceVariableWriteNode) {
            return buildInstanceVariableWrite((InstanceVariableWriteNode) node);
        } else if (node instanceof IntegerNode) {
            return buildInteger((IntegerNode) node);
        } else if (node instanceof InterpolatedRegularExpressionNode) {
            return buildInterpolatedRegularExpression(result, (InterpolatedRegularExpressionNode) node);
        } else if (node instanceof InterpolatedStringNode) {
            return buildInterpolatedString(result, (InterpolatedStringNode) node);
        } else if (node instanceof LocalVariableReadNode) {
            return buildLocalVariableRead((LocalVariableReadNode) node);
        } else if (node instanceof LocalVariableWriteNode) {
            return buildLocalVariableWrite((LocalVariableWriteNode) node);
        } else if (node instanceof MissingNode) {
            return buildMissing((MissingNode) node);
        } else if (node instanceof ModuleNode) {
            return buildModule((ModuleNode) node);
        } else if (node instanceof MultiWriteNode) {
            return buildMultiWriteNode((MultiWriteNode) node);
        } else if (node instanceof NilNode) {
            return nil();
        } else if (node instanceof OperatorOrAssignmentNode) {
            return buildOperatorOrAssignment((OperatorOrAssignmentNode) node);
        } else if (node instanceof OrNode) {
            return buildOr((OrNode) node);
        } else if (node instanceof ParenthesesNode) {
            return build(((ParenthesesNode) node).statements);
        } else if (node instanceof ProgramNode) {
            return buildProgram((ProgramNode) node);
        } else if (node instanceof RangeNode) {
            return buildRange((RangeNode) node);
        } else if (node instanceof RegularExpressionNode) {
            return buildRegularExpression((RegularExpressionNode) node);
        } else if (node instanceof ReturnNode) {
            return buildReturn((ReturnNode) node);
        } else if (node instanceof SelfNode) {
            return buildSelf();
        } else if (node instanceof SingletonClassNode) {
            return buildSingletonClass((SingletonClassNode) node);
        } else if (node instanceof SourceFileNode) {
            return buildSourceFile();
        } else if (node instanceof SourceLineNode) {
            return buildSourceLine(node);
        } else if (node instanceof SplatNode) {
            return buildSplat((SplatNode) node);
        } else if (node instanceof StatementsNode) {
            return buildStatements((StatementsNode) node);
        } else if (node instanceof StringNode) {
            return buildString((StringNode) node);
        } else if (node instanceof SuperNode) {
            return buildSuper(result, (SuperNode) node);
        } else if (node instanceof SymbolNode) {
            return buildSymbol((SymbolNode) node);
        } else if (node instanceof TrueNode) {
            return tru();
        } else if (node instanceof UnlessNode) {
            return buildUnless(result, (UnlessNode) node);
        } else if (node instanceof UntilNode) {
            return buildUntil((UntilNode) node);
        } else if (node instanceof WhileNode) {
            return buildWhile((WhileNode) node);
        } else if (node instanceof YieldNode) {
            return buildYield(result, (YieldNode) node);
        } else {
            throw new RuntimeException("Unhandled Node type: " + node);
        }
    }

    private Operand buildAlias(AliasNode node) {
        return buildAlias(build(node.new_name), build(node.old_name));
    }

    private Operand buildAnd(AndNode node) {
        return buildAnd(build(node.left), () -> build(node.right), binaryType(node.left));
    }

    private Operand[] buildArguments(ArgumentsNode node) {
        if (node == null) return Operand.EMPTY_ARRAY;

        int length = node.arguments.length;
        if (length == 0) return Operand.EMPTY_ARRAY;

        Operand[] args = new Operand[node.arguments.length];
        for (int i = 0; i < length; i++) {
            args[i] = build(node.arguments[i]);
        }

        return args;
    }

    private Operand buildArray(ArrayNode node) {
        Node[] nodes = node.elements;
        Operand[] elts = new Operand[nodes.length];
        //boolean containsAssignments = node.containsVariableAssignment();
        Operand keywordRestSplat = null;
        for (int i = 0; i < nodes.length; i++) {
            // FIXME: we need to know if this contains assignments to know if we need to preserve order.
            elts[i] = build(nodes[i]);
            if (nodes[i] instanceof HashNode && hasOnlyRestKwargs((HashNode) nodes[i])) keywordRestSplat = elts[i];
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
            return copy(array);
        }
    }

    // FIXME: Idea make common operator + nodetype so we can try and generify stuff like this
    // FIXME: Attrassign should be its own node.
    // This method is called to build assignments for a multiple-assignment instruction
    public void buildAssignment(Node node, Variable rhsVal) {
        if (node == null) return; // case of 'a, = something'

        if (node instanceof CallNode) {
            RubySymbol name = symbol(new ByteList(((CallNode) node).name));
            if ("[]=".equals(name)) {
                buildAttrAssignAssignment((CallNode) node, name, rhsVal);
            } else {
                throw notCompilable("call node found on lhs of masgn", node);
            }
        } else if (node instanceof ClassVariableWriteNode) {
            addInstr(new PutClassVariableInstr(classVarDefinitionContainer(), symbolFor(((ClassVariableWriteNode)node).name_loc), rhsVal));
        } else if (node instanceof ConstantPathWriteNode) {
            buildConstantWritePath((ConstantPathWriteNode) node, rhsVal);
        } else if (node instanceof LocalVariableWriteNode) {
            LocalVariableWriteNode variable = (LocalVariableWriteNode) node;
            copy(getLocalVariable(symbolFor(variable.name_loc), variable.depth), rhsVal);
        } else if (node instanceof GlobalVariableWriteNode) {
            addInstr(new PutGlobalVarInstr(symbolFor(((GlobalVariableWriteNode) node).name), rhsVal));
        } else if (node instanceof InstanceVariableWriteNode) {
            // NOTE: if 's' happens to the a class, this is effectively an assignment of a class instance variable
            addInstr(new PutFieldInstr(buildSelf(), symbolFor(((InstanceVariableWriteNode) node).name_loc), rhsVal));
        } else if (node instanceof MultiWriteNode) {
            buildMultiAssignment((MultiWriteNode) node, addResultInstr(new ToAryInstr(temp(), rhsVal)));
        } else if (node instanceof SplatNode) {
            buildSplat((SplatNode) node, rhsVal);
        } else {
            throw notCompilable("Can't build assignment node", node);
        }
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

    // FIXME: no rescue or ensure!!!!
    private Operand buildBegin(BeginNode node) {
        return build(node.statements);
    }

    // FIXME: Try and genericize this with AST
    private Operand buildBlock(BlockNode node) {
        // FIXME: This needs to be calculated
        Signature signature = calculateSignature(node.parameters != null ? node.parameters.parameters : null);
        IRClosure closure = new IRClosure(getManager(), scope, getLine(node), node.scope, signature, coverageMode);

        // Create a new nested builder to ensure this gets its own IR builder state like the ensure block stack
        ((IRBuilderYARP) newIRBuilder(getManager(), closure, this, node)).buildIterInner(methodName, node);

        methodName = null;

        return new WrappedIRClosure(buildSelf(), closure);
    }

    // FIXME: Try and genericize this with AST
    private InterpreterContext buildIterInner(RubySymbol methodName, BlockNode iterNode) {
        this.methodName = methodName;

        boolean forNode = false; // FIXME: For will be handled separately.
        prepareClosureImplicitState();                                    // recv_self, add frame block, etc)

        if (RubyInstanceConfig.FULL_TRACE_ENABLED) {
            addInstr(new TraceInstr(RubyEvent.B_CALL, getCurrentModuleVariable(), getName(), getFileName(), scope.getLine() + 1));
        }

        if (!forNode) addCurrentModule();                                // %current_module
        receiveBlockArgs(iterNode);
        // for adds these after processing binding block args because and operations at that point happen relative
        // to the previous scope.
        if (forNode) addCurrentModule();                                 // %current_module

        // conceptually abstract prologue scope instr creation so we can put this at the end of it instead of replicate it.
        afterPrologueIndex = instructions.size() - 1;

        // Build closure body and return the result of the closure
        Operand closureRetVal = iterNode.statements == null ? nil() : build(iterNode.statements);

        if (RubyInstanceConfig.FULL_TRACE_ENABLED) {
            addInstr(new TraceInstr(RubyEvent.B_RETURN, getCurrentModuleVariable(), getName(), getFileName(), getEndLine(iterNode) + 1));
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

    // FIXME: This is used for both for and blocks in AST but we do different path for for in YARP
    public void receiveBlockArgs(BlockNode node) {
        // FIXME: Impl
        //((IRClosure) scope).setArgumentDescriptors(Helpers.argsNodeToArgumentDescriptors(((ArgsNode) args)));
        // FIXME: Missing locals?  Not sure how we handle those but I would have thought with a scope?
        if (node.parameters != null) buildParameters(node.parameters.parameters);
    }

    private Operand buildBlockArgument(BlockArgumentNode node) {
        if (node.expression instanceof SymbolNode && !scope.maybeUsingRefinements()) {
            return new SymbolProc(symbolFor(((SymbolNode) node.expression).value));
        } else if (node.expression == null) {
            return getYieldClosureVariable();
        }
        return build(node.expression);
    }

    private Operand buildCall(Variable result, CallNode node) {
        if (result == null) result = temp();

        // FIXME: this should just be typed by parser.
        CallType callType = determineCallType(node.receiver);
        Operand receiver = callType == CallType.FUNCTIONAL ?
                buildSelf() :
                build(node.receiver);
        // FIXME: pretend always . and not &. for now.
        // FIXME: at least type arguments to ArgumentsNode
        // FIXME: block would be a lot easier if both were in .block and not maybe in arguments
        Operand[] args = buildArguments(node.arguments);
        Operand block;
        if (node.block != null) {
            block = setupCallClosure(node.block);
        } else if (containsBlockArgument(node.arguments)) {
            block = args[node.arguments.arguments.length - 1];
            args = removeArg(args);
        } else {
            block = NullBlock.INSTANCE;
        }

        // FIXME: Lots and lots of special logic in AST not here
        return addResultInstr(CallInstr.create(scope, callType, result, symbol(new String(node.name)), receiver, args, block, 0));
    }


    private Signature calculateSignature(ParametersNode parameters) {
        if (parameters == null) return Signature.NO_ARGUMENTS;
        int pre = parameters.requireds.length;
        int opt = parameters.optionals.length;
        int post = parameters.posts.length;
        int kws = parameters.keywords.length;
        // FIXME: this needs more than norm
        Signature.Rest rest = parameters.rest == null ? Signature.Rest.NONE : Signature.Rest.NORM;

        int keywordRestIndex = parameters.keyword_rest == null ? -1 : pre + opt + post + kws;
        // FIXME: need to diff opt kws vs req kws
        Signature signature = new Signature(pre, opt, post, rest, kws, kws, keywordRestIndex);

        scope.getStaticScope().setSignature(signature);

        return signature;
    }

    private boolean containsBlockArgument(ArgumentsNode node) {
        if (node == null || node.arguments == null || node.arguments.length == 0) return false;

        return node.arguments[node.arguments.length - 1] instanceof BlockArgumentNode;
    }


    @Override
    Operand[] buildCallArgs(Node args, int[] flags) {
        return buildCallArgsArray(((ArgumentsNode) args).arguments, flags);

    }
    protected Operand[] buildCallArgsArray(Node[] children, int[] flags) {
        int numberOfArgs = children.length;
        Operand[] builtArgs = new Operand[numberOfArgs];
        boolean hasAssignments = containsVariableAssignment(children);

        for (int i = 0; i < numberOfArgs; i++) {
            // FIXME: here and possibly AST make isKeywordsHash() method.
            if (i == numberOfArgs - 1 && children[i] instanceof HashNode && !isLiteralHash(children[i])) {
                builtArgs[i] = buildCallKeywordArguments((HashNode) children[i], flags);
            } else {
                builtArgs[i] = buildWithOrder(children[i], hasAssignments);
            }
        }

        return builtArgs;
    }

    protected Operand buildCallKeywordArguments(HashNode node, int[] flags) {
        flags[0] |= CALL_KEYWORD;

        if (hasOnlyRestKwargs(node)) return buildRestKeywordArgs(node, flags);

        return buildHash(node);
    }

    private Operand buildCase(CaseNode node) {
        return buildCase(node.predicate, node.conditions, node.consequent);
    }

    private Operand buildClass(ClassNode node) {
        return buildClass(determineBaseName(node.constant_path), node.superclass, node.constant_path, node.statements, node.scope, getLine(node), getEndLine(node));
    }

    private Operand buildClassVariableRead(Variable result, ClassVariableReadNode node) {
        return buildClassVar(result, symbolFor(node));
    }

    private Operand buildClassVariableWrite(ClassVariableWriteNode node) {
        return buildClassVarAsgn(symbolFor(node.name_loc), node.value);
    }


    private Operand buildConstantPath(Variable result, ConstantPathNode node) {
        RubySymbol name = symbolFor(node.child);

        return node.parent == null ? searchConst(result, name) : searchModuleForConst(result, build(node.parent), name);
    }

    private Operand buildConstantWritePath(ConstantPathWriteNode node) {
        return buildConstantWritePath(node, build(node.value));
    }

    // Multiple assignments provide the value otherwise it is grabbed from .value on the node.
    private Operand buildConstantWritePath(ConstantPathWriteNode node, Operand value) {
        if (node.target instanceof ConstantReadNode) return putConstant(symbolFor(node.target), value);

        ConstantPathNode path = (ConstantPathNode) node.target;

        return putConstant(build(path.parent), symbolFor(path.child), value);
    }

    private Operand buildConstantRead(ConstantReadNode node) {
        return addResultInstr(new SearchConstInstr(temp(), CurrentScope.INSTANCE, symbolFor(node), false));
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
        LazyMethodDefinition def = new LazyMethodDefinitionYARP(getManager().getRuntime(), source, node);
        return buildDefn(defineNewMethod(def, byteListFrom(node.name), 0, node.scope, true));
    }

    private Operand buildDefs(DefNode node) {
        LazyMethodDefinition def = new LazyMethodDefinitionYARP(getManager().getRuntime(), source, node);
        return buildDefs(node.receiver, defineNewMethod(def, byteListFrom(node.name), 0, node.scope, false));
    }

    private Operand buildElse(ElseNode node) {
        return buildStatements(node.statements);
    }

    private Operand buildForwardingSuper(Variable result, ForwardingSuperNode node) {
        return buildZSuper(result, node.block);
    }

    private Operand buildGlobalVariableRead(Variable result, GlobalVariableReadNode node) {
        // FIXME: Prefer a full node for nth ref.
        if (node.name.type == TokenType.PARENTHESIS_LEFT) { // FIXME: it is not returning NTH_REFERENCE (seems to be something wrong on my branch
            // FIXME: Gruesome :)
            return buildNthRef(Integer.parseInt(new String(byteListFrom(node.name).getUnsafeBytes()).substring(1)));
        }

        return buildGlobalVar(result, symbolFor(node.name));
    }

    private Operand buildGlobalVariableWrite(GlobalVariableWriteNode node) {
        return buildGlobalAsgn(symbolFor(node.name), node.value);
    }

    private Operand buildHash(HashNode node) {
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
                    hash = copy(new Hash(args, isLiteral));
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
            hash = copy(new Hash(args, true));
        } else if (!args.isEmpty()) { // ordinary hash values encountered after a **arg
            addInstr(new RuntimeHelperCall(hash, MERGE_KWARGS, new Operand[] { hash, new Hash(args), duplicateCheck}));
        }

        return hash;
    }

    private Operand buildIf(Variable result, IfNode node) {
        return buildConditional(result, node.predicate, node.statements, node.consequent);
    }

    private Operand buildInstanceVariableRead(InstanceVariableReadNode node) {
        return buildInstVar(symbolFor(node));
    }

    private Operand buildInstanceVariableWrite(InstanceVariableWriteNode node) {
        return buildInstAsgn(symbolFor(node.name_loc), node.value);
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

    private Operand buildLocalVariableRead(LocalVariableReadNode node) {
        return getLocalVariable(symbolFor(node), node.depth);
    }

    private Operand buildLocalVariableWrite(LocalVariableWriteNode node) {
        return buildLocalVariableAssign(symbolFor(node.name_loc), node.depth, node.value);
    }

    private Operand buildMissing(MissingNode node) {
        System.out.println("uh oh");
        return nil();
    }

    private Operand buildModule(ModuleNode node) {
        return buildModule(determineBaseName(node.constant_path), node.constant_path, node.statements, node.scope,
                getLine(node), getEndLine(node));
    }

    private Operand buildMultiWriteNode(MultiWriteNode node) {
        Node valueNode = node.value;
        Operand values = build(valueNode);
        Variable ret = getValueInTemporaryVariable(values);
        if (valueNode instanceof ArrayNode) {
            buildMultiAssignment(node, ret);
            // FIXME: need to know equiv of ILiteralNode so we can opt this case.
        /*} else if (valueNode instanceof ILiteralNode) {
            // treat a single literal value as a single-element array
            buildMultiAssignment(node, new Array(new Operand[]{ret}));*/
        } else {
            Variable tmp = addResultInstr(new ToAryInstr(temp(), ret));
            buildMultiAssignment(node, tmp);
        }
        return ret;
    }

    // SplatNode, MultiWriteNode, LocalVariableWrite and lots of other normal writes
    private void buildMultiAssignment(MultiWriteNode node, Operand values) {
        final List<Tuple<Node, Variable>> assigns = new ArrayList<>();
        Node[] targets = node.targets;
        int length = targets.length;
        int restIndex = -1;

        // FIXME: May be nice to have more info in MultiWriteNode rather than walking it twice
        for (int i = 0; i < length; i++) { // Figure out indices
            Node child = targets[i];

            if (child instanceof  SplatNode) {
                restIndex = i;
            }
        }

        int preCount = restIndex;
        int postCount = length - restIndex - 1;

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

    private Operand buildOperatorOrAssignment(OperatorOrAssignmentNode node) {
        // FIXME: AST will make a var and an assign as target and value.  This just embeds a Write + Value.
        Node[] hack = assignmentHack(node);

        if (hack != null) {
            return buildOpAsgnOr(hack[0], hack[1]);
        }
        return buildOpAsgnOr(node.target, node.value);
    }

    private Node[] assignmentHack(OperatorOrAssignmentNode op) {
        Node node = op.target;
        if (node instanceof InstanceVariableWriteNode) {
            return new Node[] { new InstanceVariableReadNode(node.startOffset, node.endOffset), new InstanceVariableWriteNode(new Location(node.startOffset, node.endOffset), op.value, node.startOffset, node.endOffset) };
        } else if (node instanceof GlobalVariableWriteNode) {
            return new Node[] { new GlobalVariableReadNode(((GlobalVariableWriteNode) node).name, node.startOffset, ((GlobalVariableWriteNode) node).name.endOffset),
                    new GlobalVariableWriteNode(((GlobalVariableWriteNode) node).name, op.value, node.startOffset, node.endOffset) };
        } else if (node instanceof ClassVariableWriteNode) {
            return new Node[]{new ClassVariableReadNode(((ClassVariableWriteNode) node).name_loc.startOffset, ((ClassVariableWriteNode) node).name_loc.endOffset),
                    new ClassVariableWriteNode(((ClassVariableWriteNode) node).name_loc, op.value, node.startOffset, node.endOffset)};
        } else if (node instanceof LocalVariableWriteNode) {
            return new Node[]{new LocalVariableReadNode(((LocalVariableWriteNode) node).depth, node.startOffset, node.endOffset),
                    new LocalVariableWriteNode(((LocalVariableWriteNode) node).name_loc, op.value, ((LocalVariableWriteNode) node).depth, node.startOffset, node.endOffset)};
        }

        // FIXME: Implement more or do this totally differently

        return null;
    }

    private Operand buildOr(OrNode node) {
        return buildOr(build(node.left), () -> build(node.right), binaryType(node.left));
    }

    private void buildParameters(ParametersNode parameters) {
        // FIXME: If we setup signature here we need to do it for no params before returning.
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

                RubySymbol key = symbolFor(kwarg.name);
                Variable av = getNewLocalVariable(key, 0);
                Label l = getNewLabel();
                if (scope instanceof IRMethod) addKeyArgDesc(kwarg, key);
                addInstr(new ReceiveKeywordArgInstr(av, keywords, key));
                addInstr(BNEInstr.create(l, av, UndefinedValue.UNDEFINED)); // if 'av' is not undefined, we are done

                // Required kwargs have no value and check_arity will throw if they are not provided.
                if (kwarg.value != null) {
                    addInstr(new CopyInstr(av, nil())); // wipe out undefined value with nil
                    build(kwarg.value);
                } else {
                    addInstr(new RaiseRequiredKeywordArgumentError(key));
                }
                addInstr(new LabelInstr(l));
            }
        }

        // 2.0 keyword rest arg
        KeywordRestParameterNode keyRest = (KeywordRestParameterNode) parameters.keyword_rest;
        if (keyRest != null) {
            RubySymbol key = symbolFor(keyRest.name);
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

    private Operand buildRange(RangeNode node) {
        boolean isExclusive = node.operator_loc.endOffset - node.operator_loc.startOffset == 3;
        return buildRange(node.left, node.right, isExclusive);
    }

    private Operand buildRegularExpression(RegularExpressionNode node) {
        ByteList content = byteListFrom(node.content);
        String opts = new String(byteListFrom(node.closing).getUnsafeBytes()).substring(1);
        RegexpOptions options = RegexpOptions.newRegexpOptions(opts);
        return new Regexp(content, options);
    }

    private Operand buildReturn(ReturnNode node) {
        Operand[] args = buildArguments(node.arguments);

        // FIXME: This is weird.  If this is how this should work should this be a helper?
        Operand value;
        switch (args.length) {
            case 0:
                value = nil();
                break;
            case 1:
                value = args[0];
                break;
            default:
                value = new Array(args);
        }

        return buildReturn(value, getLine(node));
    }

    private Operand buildRestKeywordArgs(HashNode keywordArgs, int[] flags) {
        flags[0] |= CALL_KEYWORD_REST;
        Node[] pairs = keywordArgs.elements;
        boolean containsVariableAssignment = containsVariableAssignment(keywordArgs);

        if (pairs.length == 1) { // Only a single rest arg here.  Do not bother to merge.
            Operand splat = buildWithOrder(pairs[0], containsVariableAssignment);

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
        return buildSClass(node.expression, node.statements, node.scope, getLine(node), getEndLine(node));
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
        return new MutableString(byteListFrom(node.content), 0, scope.getFile(), line);
    }

    private Operand buildSuper(Variable result, SuperNode node) {
        return buildSuper(result, node.block, node.arguments, getLine(node));
    }

    private Operand buildSymbol(SymbolNode node) {
        return new Symbol(symbol(byteListFrom(node.value)));
    }

    private Operand buildUnless(Variable result, UnlessNode node) {
        return buildConditional(result, node.predicate, node.consequent, node.statements);
    }

    // FIXME: until and while should have field for whether normal or modifer and no keyword at all.
    private Operand buildUntil(UntilNode node) {
        boolean evaluateAtStart = node.keyword.type == TokenType.KEYWORD_UNTIL;
        return buildConditionalLoop(node.predicate, node.statements, false, evaluateAtStart);
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
        boolean evaluateAtStart = node.keyword.type == TokenType.KEYWORD_WHILE;
        return buildConditionalLoop(node.predicate, node.statements, true, evaluateAtStart);
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

    int dynamicPiece(Operand[] pieces, int i, Node pieceNode, boolean interpolated) {
        Operand piece;

        // somewhat arbitrary minimum size for interpolated values
        int estimatedSize = 4;

        while (true) { // loop to unwrap EvStr

            if (pieceNode instanceof StringNode) {
                piece = buildString((StringNode) pieceNode, interpolated);
                estimatedSize = byteListFrom(((StringNode) pieceNode).content).realSize();
            } else if (pieceNode instanceof StringInterpolatedNode) {
                if (scope.maybeUsingRefinements()) {
                    // refined asString must still go through dispatch
                    Variable result = temp();
                    addInstr(new AsStringInstr(scope, result, build(((StringInterpolatedNode) pieceNode).statements), scope.maybeUsingRefinements()));
                    piece = result;
                } else {
                    // evstr/asstring logic lives in BuildCompoundString now, unwrap and try again
                    pieceNode = ((StringInterpolatedNode) pieceNode).statements;
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
     *  @param blockArg the arguments containing the block arg, if any
     *
     */
    protected void receiveBlockArg(BlockParameterNode blockArg) {
        // reify to Proc if we have a block arg
        if (blockArg != null) {
            RubySymbol argName = symbolFor(blockArg.name);
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
                RubySymbol argName = symbolFor(optArg.name);
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
                    addArgumentDescription(ArgumentType.rest, symbolFor(restArgNode.name));
                }
            }

            RubySymbol argName =  restArgNode.name == null ?
                    scope.getManager().getRuntime().newSymbol(CommonByteLists.STAR) : symbolFor(restArgNode.name);

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
            RubySymbol argName = symbolFor(node);

            if (scope instanceof IRMethod) addArgumentDescription(ArgumentType.req, argName);

            addInstr(new ReceivePreReqdArgInstr(argumentResult(argName), keywords, argIndex));
        } else if (node instanceof RequiredDestructuredParameterNode) {
                Variable v = temp();
                addInstr(new ReceivePreReqdArgInstr(v, keywords, argIndex));
                if (scope instanceof IRMethod) addArgumentDescription(ArgumentType.anonreq, null);
                Variable tmp = temp();
                addInstr(new ToAryInstr(tmp, v));

                // FIXME: Impl
            //((RequiredDestructuredParameterNode) node).parameters;
//                buildMultipleAsgn19Assignment(childNode, tmp, null);
            throw notCompilable("Can't run destructured params yet", node);
        } else {
            throw notCompilable("Can't build required parameter node", node);
        }
    }

    public void receivePostArg(Node node, Variable keywords, int argIndex, int preCount, int optCount, boolean hasRest, int postCount) {
        if (node instanceof RequiredParameterNode) {
            RubySymbol argName = symbolFor(node);

            if (scope instanceof IRMethod) addArgumentDescription(ArgumentType.req, argName);

            addInstr(new ReceivePostReqdArgInstr(argumentResult(argName), keywords, argIndex, preCount, optCount, hasRest, postCount));
        } else if (node instanceof RequiredDestructuredParameterNode) {
            Variable v = temp();
            addInstr(new ReceivePostReqdArgInstr(v, keywords, argIndex, preCount, optCount, hasRest, postCount));

            if (scope instanceof IRMethod) addArgumentDescription(ArgumentType.anonreq, null);

            Variable tmp = temp();
            addInstr(new ToAryInstr(tmp, v));

            //((RequiredDestructuredParameterNode) node).parameters;
//                buildMultipleAsgn19Assignment(childNode, tmp, null);
            throw notCompilable("Can't run destructured params yet", node);
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
        boolean interpolated = node.opening != null && source[node.opening.startOffset] != '\'';
        return buildString(node, interpolated);
    }

    private Operand buildString(StringNode node, boolean interpolated) {
        // FIXME: No code range
        // FIXME: how can we tell if frozen or not

        // no opening is DNode-like string fragments.
        if (interpolated) {
            return new MutableString(new ByteList(node.unescaped), 0, scope.getFile(), getLine(node));
        } else {
            return new MutableString(byteListFrom(node.content), 0, scope.getFile(), getLine(node));
        }
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

        if (node instanceof ClassVariableWriteNode ||
                node instanceof ConstantPathWriteNode || node instanceof LocalVariableWriteNode ||
                node instanceof GlobalVariableWriteNode || node instanceof MultiWriteNode ||
                node instanceof OperatorAssignmentNode || node instanceof InstanceVariableWriteNode) {
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
            return addResultInstr(
                    new RuntimeHelperCall(
                            temp(),
                            IS_DEFINED_BACKREF,
                            new Operand[]{new FrozenString(DefinedMessage.GLOBAL_VARIABLE.getText())}
                    )
            );
        } else if (node instanceof InstanceVariableReadNode) {
            return addResultInstr(
                    new RuntimeHelperCall(
                            temp(),
                            IS_DEFINED_INSTANCE_VAR,
                            new Operand[]{
                                    buildSelf(),
                                    new FrozenString(symbolFor(node)),
                                    new FrozenString(DefinedMessage.INSTANCE_VARIABLE.getText())
                            }
                    )
            );
        } else if (node instanceof ClassVariableReadNode) {
            return addResultInstr(
                    new RuntimeHelperCall(
                            temp(),
                            IS_DEFINED_CLASS_VAR,
                            new Operand[]{
                                    classVarDefinitionContainer(),
                                    new FrozenString(symbolFor(node)),
                                    new FrozenString(DefinedMessage.CLASS_VARIABLE.getText())
                            }
                    )
            );
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
            RubySymbol name = symbolFor(call.message);

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
                                        new Symbol(symbolFor(call.message)),
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
                                        new Symbol(symbolFor(call)),
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
            Label defLabel = getNewLabel();
            Label doneLabel = getNewLabel();
            Variable tmpVar = temp();
            RubySymbol constName = symbolFor(node);
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
        } else if (node instanceof ConstantPathNode) {
            // SSS FIXME: Is there a reason to do this all with low-level IR?
            // Can't this all be folded into a Java method that would be part
            // of the runtime library, which then can be used by buildDefinitionCheck method above?
            // This runtime library would be used both by the interpreter & the compiled code!

            ConstantPathNode path = (ConstantPathNode) node;

            final RubySymbol name = symbolFor(path);
            final Variable errInfo = temp();

            // store previous exception for restoration if we rescue something
            addInstr(new GetErrorInfoInstr(errInfo));

            CodeBlock protectedCode = new CodeBlock() {
                public Operand run() {
                    if (path.parent == null) { // colon3 (weird inheritance)
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

    Operand buildYield(Variable result, YieldNode node) {
        if (result == null) result = temp();
        if (scope instanceof IRScriptBody || scope instanceof IRModuleBody) throwSyntaxError(getLine(node), "Invalid yield");

        boolean unwrap = true;
        Node[] args = node.arguments.arguments;
        // Get rid of one level of array wrapping
        if (args != null && args.length == 1) {
            // We should not unwrap if it is a keyword argument.
            if (!(args[0] instanceof HashNode) || ((HashNode) args[0]).opening != null) {
                unwrap = false;
            }
        }

        int[] flags = new int[] { 0 };
        Operand value = buildYieldArgs(args, flags);

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


    private int getEndLine(Node node) {
        return 0;
    }

    // FIXME: need to get line.
    @Override
    int getLine(Node node) {
        return 0;
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
            return symbolFor(((SymbolNode) node).value);
        } else if (node instanceof StringNode) {
            return runtime.newString((byteListFrom(((StringNode) node).content)));
        }

        return null;
    }

    @Override
    boolean isLiteralString(Node node) {
        return node instanceof StringNode;
    }

    boolean isLiteralHash(Node node) {
        return node instanceof HashNode && ((HashNode) node).opening != null;
    }

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
                node instanceof OperatorAssignmentNode ||
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

    private RubySymbol symbolFor(Token token) {
        return symbol(byteListFrom(token));
    }

    private RubySymbol symbolFor(Location location) {
        return symbol(byteListFrom(location));
    }

    private RubySymbol symbolFor(Node node) {
        return symbol(byteListFrom(node));
    }

    private ByteList byteListFrom(Token token) {
        return new ByteList(source, token.startOffset, token.endOffset - token.startOffset);
    }

    private ByteList byteListFrom(Location location) {
        return new ByteList(source, location.startOffset, location.endOffset - location.startOffset);
    }

    private ByteList byteListFrom(Node node) {
        return new ByteList(source, node.startOffset, node.endOffset - node.startOffset);
    }

    private CallType determineCallType(Node node) {
        return node == null ?
                CallType.FUNCTIONAL :
                CallType.NORMAL;
    }

    private ByteList determineBaseName(Node node) {
        if (node instanceof ConstantReadNode) {
            return byteListFrom(node);
        } else if (node instanceof ConstantPathNode) {
            return byteListFrom(((ConstantPathNode) node).child);
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
    private boolean hasOnlyRestKwargs(HashNode node) {
        for (Node element: node.elements) {
            if (!(element instanceof AssocSplatNode)) return false;
        }

        return true;
    }

    @Override
    Node whenBody(WhenNode arm) {
        return arm.statements;
    }
}
