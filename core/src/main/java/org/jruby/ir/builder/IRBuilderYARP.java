package org.jruby.ir.builder;

import org.jruby.ParseResult;
import org.jruby.RubyNumeric;
import org.jruby.RubySymbol;
import org.jruby.compiler.NotCompilableException;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRManager;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRModuleBody;
import org.jruby.ir.IRScope;
import org.jruby.ir.instructions.BNEInstr;
import org.jruby.ir.instructions.BuildSplatInstr;
import org.jruby.ir.instructions.CheckArityInstr;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.DefineModuleInstr;
import org.jruby.ir.instructions.LabelInstr;
import org.jruby.ir.instructions.LoadImplicitClosureInstr;
import org.jruby.ir.instructions.RaiseRequiredKeywordArgumentError;
import org.jruby.ir.instructions.ReceiveKeywordArgInstr;
import org.jruby.ir.instructions.ReceiveKeywordRestArgInstr;
import org.jruby.ir.instructions.ReceiveKeywordsInstr;
import org.jruby.ir.instructions.ReceiveOptArgInstr;
import org.jruby.ir.instructions.ReceivePostReqdArgInstr;
import org.jruby.ir.instructions.ReceivePreReqdArgInstr;
import org.jruby.ir.instructions.ReceiveRestArgInstr;
import org.jruby.ir.instructions.ReifyClosureInstr;
import org.jruby.ir.instructions.RuntimeHelperCall;
import org.jruby.ir.instructions.SearchConstInstr;
import org.jruby.ir.instructions.ToAryInstr;
import org.jruby.ir.operands.Array;
import org.jruby.ir.operands.CurrentScope;
import org.jruby.ir.operands.Hash;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.MutableString;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Symbol;
import org.jruby.ir.operands.SymbolProc;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.runtime.ArgumentType;
import org.jruby.runtime.CallType;
import org.jruby.util.ByteList;
import org.jruby.util.CommonByteLists;
import org.jruby.util.KeyValuePair;
import org.yarp.Nodes.*;
import org.yarp.YarpParseResult;

import java.util.ArrayList;
import java.util.List;

import static org.jruby.ir.instructions.RuntimeHelperCall.Methods.IS_HASH_EMPTY;
import static org.jruby.ir.instructions.RuntimeHelperCall.Methods.MERGE_KWARGS;

public class IRBuilderYARP extends IRBuilder<Node, DefNode> {
    YarpParseResult result = null;

    public IRBuilderYARP(IRManager manager, IRScope scope, IRBuilder parent, IRBuilder variableBuilder) {
        super(manager, scope, parent, variableBuilder);

        // FIXME: remove once all paths consstently use same parser.
        if (parent instanceof IRBuilderYARP) {
            result = ((IRBuilderYARP) parent).result;
        }
    }

    @Override
    public Operand build(ParseResult result) {
        ProgramNode program = ((YarpParseResult) result).getRoot();

        this.result = (YarpParseResult) result;
        return build(program.statements);
    }

    Operand build(Node node) {
        return build(null, node);
    }

    /*
     * @param result preferred result variable (this reduces temp vars pinning values).
     * @param node to be built
     */
    Operand build(Variable result, Node node) {
        // FIXME: Need node types if this is how we process
        if (node instanceof AndNode) {
            return buildAnd((AndNode) node);
        } else if (node instanceof ArrayNode) {
            return buildArray((ArrayNode) node);
        } else if (node instanceof BlockArgumentNode) {
            return buildBlockArgument((BlockArgumentNode) node);
        } else if (node instanceof CallNode) {
            return buildCall(result, (CallNode) node);
        } else if (node instanceof ConstantPathNode) {
            return buildConstantPath(result, (ConstantPathNode) node);
        } else if (node instanceof ConstantPathWriteNode) {
            return buildConstantWritePath((ConstantPathWriteNode) node);
        } else if (node instanceof ConstantReadNode) {
            return buildConstantRead((ConstantReadNode) node);
        } else if (node instanceof DefNode) {
            return buildDef((DefNode) node);
        } else if (node instanceof FalseNode) {
            return fals();
        } else if (node instanceof HashNode) {
            return buildHash((HashNode) node);
        } else if (node instanceof IfNode) {
            return buildIf(result, (IfNode) node);
        } else if (node instanceof IntegerNode) {
            return buildInteger((IntegerNode) node);
        } else if (node instanceof InterpolatedStringNode) {
            return buildInterpolatedString((InterpolatedStringNode) node);
        } else if (node instanceof LocalVariableReadNode) {
            return buildLocalVariableRead((LocalVariableReadNode) node);
        } else if (node instanceof LocalVariableWriteNode) {
            return buildLocalVariableWrite((LocalVariableWriteNode) node);
        } else if (node instanceof ModuleNode) {
            return buildModule((ModuleNode) node);
        } else if (node instanceof NilNode) {
            return nil();
        } else if (node instanceof OrNode) {
            return buildOr((OrNode) node);
        } else if (node instanceof ProgramNode) {
            return buildProgram((ProgramNode) node);
        } else if (node instanceof SplatNode) {
            return buildSplat((SplatNode) node);
        } else if (node instanceof StatementsNode) {
            return buildStatements((StatementsNode) node);
        } else if (node instanceof StringNode) {
            return buildString((StringNode) node);
        } else if (node instanceof SymbolNode) {
            return buildSymbol((SymbolNode) node);
        } else if (node instanceof TrueNode) {
            return tru();
        } else if (node instanceof UnlessNode) {
            return buildUnless(result, (UnlessNode) node);
        } else {
            throw new RuntimeException("Unhandled Node type: " + node);
        }
    }

    private Operand buildAnd(AndNode node) {
        return buildAnd(build(node.left), () -> build(node.right), BinaryType.Normal);
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

    private Operand buildBlockArgument(BlockArgumentNode node) {
        if (node.expression instanceof SymbolNode && !scope.maybeUsingRefinements()) {
            return new SymbolProc(symbol(byteListFromNode(node.expression)));
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
        Operand[] args = buildArguments(node.arguments);
        // FIXME: Lots and lots of special logic in AST not here

        return _call(result, callType, receiver, symbol(new String(node.name)), args);
    }

    private Operand buildConstantPath(Variable result, ConstantPathNode node) {
        RubySymbol name = symbol(byteListFromNode(node.child));

        return node.parent == null ? searchConst(result, name) : searchModuleForConst(result, build(node.parent), name);
    }

    private Operand buildConstantWritePath(ConstantPathWriteNode node) {
        Operand value = build(node.value);

        if (node.target instanceof ConstantReadNode) {
            return putConstant(symbol(byteListFromNode(node.target)), value);
        }

        ConstantPathNode path = (ConstantPathNode) node.target;

        return putConstant(build(path.parent), symbol(byteListFromNode(path.child)), value);
    }

    private Operand buildConstantRead(ConstantReadNode node) {
        return addResultInstr(new SearchConstInstr(temp(), CurrentScope.INSTANCE, symbol(byteListFromNode(node)), false));
    }

    private Operand buildDef(DefNode node) {
        if (node.receiver == null) {
            return buildDefn(node);
        } else {
            return buildDefs(node);
        }
    }

    private Operand buildDefn(DefNode node) {
        return buildDefn(buildNewMethod(node, true));
    }

    private Operand buildDefs(DefNode node) {
        return buildDefn(buildNewMethod(node, false));
    }

    private Operand buildHash(HashNode node) {
        List<KeyValuePair<Operand, Operand>> args = new ArrayList<>();
        boolean hasAssignments = false; // FIXME: Missing variable assignments check
        Variable hash = null;
        // Duplication checks happen when **{} are literals and not **h variable references.
        Operand duplicateCheck = fals();

        // pair is AssocNode or AssocSplatNode
        for (Node pair: node.elements) {
            Operand keyOperand;

            if (pair instanceof AssocNode) {
                keyOperand = build(((AssocNode) pair).key);
                args.add(new KeyValuePair<>(keyOperand, build(((AssocNode) pair).value))); // FIXME: missing possible buildWithOrder
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
                Operand splat = build(assoc.value); // FIXME: buildWithOrder
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

    private Operand buildInteger(IntegerNode node) {
        // FIXME: HAHAHAH horrible hack around integer being too much postprocessing.
        ByteList value = byteListFromNode(node);

        return fix(RubyNumeric.fix2long(RubyNumeric.str2inum(getManager().runtime, getManager().getRuntime().newString(value), 10)));
    }


    private Operand buildInterpolatedString(InterpolatedStringNode node) {
        return nil();
    }

    private Operand buildLocalVariableRead(LocalVariableReadNode node) {
        return getLocalVariable(symbol(byteListFromNode(node)), node.depth);
    }

    private Operand buildLocalVariableWrite(LocalVariableWriteNode node) {
        return buildLocalVariableAssign(symbol(byteListFromLocation(node.name_loc)), node.depth, node.value);
    }

    private Operand buildModule(ModuleNode node) {
        boolean executesOnce = this.executesOnce;
        ByteList moduleName = determineModuleBasName(node);
        Operand container = buildModuleContainer(node);

        // FIXME: Missing line. set to 0.
        int line = 0;
        int endLine = 0;
        IRModuleBody body = new IRModuleBody(getManager(), scope, moduleName, line, node.scope, executesOnce);
        Variable bodyResult = addResultInstr(new DefineModuleInstr(temp(), container, body));

        IRBuilderYARP builder = new IRBuilderYARP(getManager(), body, this, null);
        builder.buildModuleOrClassBody(node.statements, line, endLine);
        return bodyResult;
    }

    private Operand buildModuleContainer(ModuleNode node) {
        if (node.constant_path instanceof ConstantReadNode) {
            return findContainerModule();
        } else if (node.constant_path instanceof ConstantPathNode) {
            ConstantPathNode path = (ConstantPathNode) node.constant_path;

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
    protected Operand buildModuleBody(Node body) {
        return build(body);
    }

    private IRMethod buildNewMethod(DefNode node, boolean isInstanceMethod) {
        IRMethod method = new IRMethod(getManager(), scope, null, byteListFromToken(node.name), isInstanceMethod, 0,
                node.scope, coverageMode);

        if (canBeLazyMethod(node)) throw new RuntimeException("Laziness not implemented for YARP");

        IRBuilder builder = new IRBuilderYARP(getManager(), method, this, null);
        builder.executesOnce = false; // set up so nested things (modules+) which think it could execute once knows it cannot (it is in a method).
        builder.defineMethodInner(node, scope, coverageMode); // sets interpreterContext

        return method;
    }

    // FIXME: Need NodeType.alwaysTrue/False so we can opt this.
    private Operand buildOr(OrNode node) {
        return buildOr(build(node.left), () -> build(node.right), BinaryType.Normal);
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

                RubySymbol key = symbol(byteListFromToken(kwarg.name));
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
            RubySymbol key = symbol(byteListFromToken(keyRest.name));
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

    private Operand buildSplat(SplatNode node) {
        return addResultInstr(new BuildSplatInstr(temp(), build(node.expression), true));
    }

    private Operand buildSymbol(SymbolNode node) {
        return new Symbol(symbol(byteListFromToken(node.value)));
    }

    private Operand buildUnless(Variable result, UnlessNode node) {
        return buildConditional(result, node.predicate, node.consequent, node.statements);
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
            RubySymbol argName = symbol(byteListFromToken(blockArg.name));
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
                RubySymbol argName = symbol(byteListFromToken(optArg.name));
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
                    addArgumentDescription(ArgumentType.anonrest, symbol(byteListFromToken(restArgNode.operator)));
                } else {
                    addArgumentDescription(ArgumentType.rest, symbol(byteListFromToken(restArgNode.name)));
                }
            }

            RubySymbol argName =  restArgNode.name == null ?
                    scope.getManager().getRuntime().newSymbol(CommonByteLists.STAR) : symbol(byteListFromToken(restArgNode.name));

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
            RubySymbol argName = symbol(byteListFromNode(node));

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
            RubySymbol argName = symbol(byteListFromNode(node));

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

    // FIXME: StringNode should enumerate types and not force us to decode strings to figure it out.
    private Operand buildString(StringNode node) {
        // FIXME: only assuming normal strings and not words
        // FIXME: No code range
        // FIXME: how can we tell if frozen or not
        // FIXME: no line
        return new MutableString(byteListFromToken(node.content), 0, scope.getFile(), 0);
    }

    @Override
    public void receiveMethodArgs(DefNode defNode) {
        buildParameters(defNode.parameters);
    }

    @Override
    public Operand buildMethodBody(DefNode defNode) {
        return build(defNode.statements);
    }

    @Override
    public int getMethodEndLine(DefNode defNode) {
        return 0;
    }

    private ByteList byteListFromToken(Token token) {
        byte[] source = result.getSource();

        return new ByteList(source, token.startOffset, token.endOffset - token.startOffset);
    }

    private ByteList byteListFromLocation(Location location) {
        byte[] source = result.getSource();

        return new ByteList(source, location.startOffset, location.endOffset - location.startOffset);
    }

    private ByteList byteListFromNode(Node node) {
        byte[] source = result.getSource();

        return new ByteList(source, node.startOffset, node.endOffset - node.startOffset);
    }

    private CallType determineCallType(Node node) {
        return node == null ?
                CallType.FUNCTIONAL :
                CallType.NORMAL;
    }

    private ByteList determineModuleBasName(ModuleNode node) {
        if (node.constant_path instanceof ConstantReadNode) {
            return byteListFromNode(node.constant_path);
        } else if (node.constant_path instanceof ConstantPathNode) {
            return byteListFromNode(((ConstantPathNode) node.constant_path).child);
        }
        throw notCompilable("Unsupported node in module path", node);
    }

    boolean canBeLazyMethod(Object method) {
        return false;
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
}
