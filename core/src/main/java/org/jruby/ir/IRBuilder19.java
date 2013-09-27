package org.jruby.ir;

import org.jruby.ast.ArgsNode;
import org.jruby.ast.ArgsPushNode;
import org.jruby.ast.ArgumentNode;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.BlockArgNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.EncodingNode;
import org.jruby.ast.IterNode;
import org.jruby.ast.LambdaNode;
import org.jruby.ast.ListNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.MultipleAsgn19Node;
import org.jruby.ast.Node;
import org.jruby.ast.NotNode;
import org.jruby.ast.NthRefNode;
import org.jruby.ast.OptArgNode;
import org.jruby.ast.StarNode;
import org.jruby.ast.YieldNode;
import org.jruby.compiler.NotCompilableException;
import org.jruby.ir.instructions.BEQInstr;
import org.jruby.ir.instructions.BNEInstr;
import org.jruby.ir.instructions.CallInstr;
import org.jruby.ir.instructions.CheckArityInstr;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.LabelInstr;
import org.jruby.ir.instructions.ReceiveClosureInstr;
import org.jruby.ir.instructions.ReceiveOptArgInstr;
import org.jruby.ir.instructions.ReceivePreReqdArgInstr;
import org.jruby.ir.instructions.ReceiveRestArgInstr;
import org.jruby.ir.instructions.ReceiveSelfInstr;
import org.jruby.ir.instructions.ReqdArgMultipleAsgnInstr;
import org.jruby.ir.instructions.RestArgMultipleAsgnInstr;
import org.jruby.ir.instructions.ReturnInstr;
import org.jruby.ir.instructions.ToAryInstr;
import org.jruby.ir.instructions.YieldInstr;
import org.jruby.ir.instructions.defined.BackrefIsMatchDataInstr;
import org.jruby.ir.instructions.ruby19.BuildLambdaInstr;
import org.jruby.ir.instructions.ruby19.GetEncodingInstr;
import org.jruby.ir.instructions.ruby19.ReceivePostReqdArgInstr;
import org.jruby.ir.operands.CompoundArray;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.MethAddr;
import org.jruby.ir.operands.NthRef;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Arity;

public class IRBuilder19 extends IRBuilder {
    public IRBuilder19(IRManager manager) {
        super(manager);
    }

    @Override
    public boolean is1_9() {
        return true;
    }

    @Override
    protected Operand buildVersionSpecificNodes(Node node, IRScope s) {
        switch (node.getNodeType()) {
            case ENCODINGNODE: return buildEncoding((EncodingNode)node, s);
            case MULTIPLEASGN19NODE: return buildMultipleAsgn19((MultipleAsgn19Node) node, s);
            case LAMBDANODE: return buildLambda((LambdaNode)node, s);
            default: throw new NotCompilableException("Unknown node encountered in builder: " + node.getClass());
        }
    }

    @Override
    protected LocalVariable getBlockArgVariable(IRScope s, String name, int depth) {
        IRClosure cl = (IRClosure)s;
        if (cl.isForLoopBody()) {
            return cl.getLocalVariable(name, depth);
        } else {
            throw new NotCompilableException("Cannot ask for block-arg variable in 1.9 mode");
        }
    }

    @Override
    public void buildVersionSpecificBlockArgsAssignment(Node node, IRScope s, Operand argsArray, int argIndex, boolean isMasgnRoot, boolean isClosureArg, boolean isSplat) {
        IRClosure cl = (IRClosure)s;
        if (!cl.isForLoopBody())
            throw new NotCompilableException("Should not have come here for block args assignment in 1.9 mode: " + node);

        // Argh!  For-loop bodies and regular iterators are different in terms of block-args!
        switch (node.getNodeType()) {
            case MULTIPLEASGN19NODE: {
                ListNode sourceArray = ((MultipleAsgn19Node) node).getPre();
                int i = 0;
                for (Node an: sourceArray.childNodes()) {
                    // Use 1.8 mode version for this
                    buildBlockArgsAssignment(an, s, null, i, false, false, false);
                    i++;
                }
                break;
            }
            default:
                throw new NotCompilableException("Can't build assignment node: " + node);
        }
    }

    protected LocalVariable getArgVariable(IRScope s, String name, int depth) {
        // For non-loops, this name will override any name that exists in outer scopes
        return s.isForLoopBody() ? s.getLocalVariable(name, depth) : s.getNewLocalVariable(name, 0);
    }

    private void addArgReceiveInstr(IRScope s, Variable v, int argIndex, boolean post, int numPreReqd, int numPostRead) {
        if (post) s.addInstr(new ReceivePostReqdArgInstr(v, argIndex, numPreReqd, numPostRead));
        else s.addInstr(new ReceivePreReqdArgInstr(v, argIndex));
    }

    public void receiveRequiredArg(Node node, IRScope s, int argIndex, boolean post, int numPreReqd, int numPostRead) {
        switch (node.getNodeType()) {
            case ARGUMENTNODE: {
                ArgumentNode a = (ArgumentNode)node;
                String argName = a.getName();
                if (s instanceof IRMethod) ((IRMethod)s).addArgDesc("req", argName);
                addArgReceiveInstr(s, s.getNewLocalVariable(argName, 0), argIndex, post, numPreReqd, numPostRead);
                break;
            }
            case MULTIPLEASGN19NODE: {
                MultipleAsgn19Node childNode = (MultipleAsgn19Node) node;
                Variable v = s.getNewTemporaryVariable();
                addArgReceiveInstr(s, v, argIndex, post, numPreReqd, numPostRead);
                if (s instanceof IRMethod) ((IRMethod)s).addArgDesc("rest", "");
                s.addInstr(new ToAryInstr(v, v, manager.getFalse()));
                buildMultipleAsgn19Assignment(childNode, s, v, null);
                break;
            }
            default: throw new NotCompilableException("Can't build assignment node: " + node);
        }
    }

    private void receiveClosureArg(BlockArgNode blockVarNode, IRScope s) {
        Variable blockVar = null;
        if (blockVarNode != null) {
            String blockArgName = blockVarNode.getName();
            blockVar = s.getNewLocalVariable(blockArgName, 0);
            if (s instanceof IRMethod) ((IRMethod)s).addArgDesc("block", blockArgName);
            s.addInstr(new ReceiveClosureInstr(blockVar));
        }

        // SSS FIXME: This instruction is only needed if there is an yield instr somewhere!
        // In addition, store the block argument in an implicit block variable
        Variable implicitBlockArg = s.getImplicitBlockArg();
        if (blockVar == null) s.addInstr(new ReceiveClosureInstr(implicitBlockArg));
        else s.addInstr(new CopyInstr(implicitBlockArg, blockVar));
    }

    protected void receiveNonBlockArgs(final ArgsNode argsNode, IRScope s) {
        final int numPreReqd = argsNode.getPreCount();
        final int numPostReqd = argsNode.getPostCount();
        final int required = argsNode.getRequiredArgsCount(); // numPreReqd + numPostReqd
        int opt = argsNode.getOptionalArgsCount();
        int rest = argsNode.getRestArg();

        s.getStaticScope().setArities(required, opt, rest);

        // For closures, we don't need the check arity call
        if (s instanceof IRMethod) {
            // FIXME: Expensive to do this explicitly?  But, two advantages:
            // (a) on inlining, we'll be able to get rid of these checks in almost every case.
            // (b) compiler to bytecode will anyway generate this and this is explicit.
            // For now, we are going explicit instruction route.  But later, perhaps can make this implicit in the method setup preamble?
            s.addInstr(new CheckArityInstr(required, opt, rest));
        }

        // Other args begin at index 0
        int argIndex = 0;

        // Pre(-opt and rest) required args
        ListNode preArgs = argsNode.getPre();
        for (int i = 0; i < numPreReqd; i++, argIndex++) {
            receiveRequiredArg(preArgs.get(i), s, argIndex, false, -1, -1);
        }

        // Fixup opt/rest
        opt = opt > 0 ? opt : 0;
        rest = rest > -1 ? 1 : 0;

        // Now for opt args
        if (opt > 0) {
            ListNode optArgs = argsNode.getOptArgs();
            for (int j = 0; j < opt; j++, argIndex++) {
                // Jump to 'l' if this arg is not null.  If null, fall through and build the default value!
                Label l = s.getNewLabel();
                OptArgNode n = (OptArgNode)optArgs.get(j);
                String argName = n.getName();
                Variable av = s.getNewLocalVariable(argName, 0);
                if (s instanceof IRMethod) ((IRMethod)s).addArgDesc("opt", argName);
                // You need at least required+j+1 incoming args for this opt arg to get an arg at all
                s.addInstr(new ReceiveOptArgInstr(av, required, numPreReqd, j));
                s.addInstr(BNEInstr.create(av, UndefinedValue.UNDEFINED, l)); // if 'av' is not undefined, go to default
                build(n.getValue(), s);
                s.addInstr(new LabelInstr(l));
            }
        }

        // Rest arg
        if (rest > 0) {
            // Consider: def foo(*); .. ; end
            // For this code, there is no argument name available from the ruby code.
            // So, we generate an implicit arg name
            String argName = argsNode.getRestArgNode().getName();
            if (s instanceof IRMethod) ((IRMethod)s).addArgDesc("rest", argName == null ? "" : argName);
            argName = (argName == null || argName.equals("")) ? "%_arg_array" : argName;

            // You need at least required+opt+1 incoming args for the rest arg to get any args at all
            // If it is going to get something, then it should ignore required+opt args from the beginning
            // because they have been accounted for already.
            s.addInstr(new ReceiveRestArgInstr(s.getNewLocalVariable(argName, 0), required + opt, argIndex));
            argIndex++;
        }

        // Post(-opt and rest) required args
        ListNode postArgs = argsNode.getPost();
        for (int i = 0; i < numPostReqd; i++) {
            receiveRequiredArg(postArgs.get(i), s, i, true, numPreReqd, numPostReqd);
        }
    }

    protected void receiveBlockArg(final ArgsNode argsNode, IRScope s) {
        // For methods, we always receive it (implicitly, if the block arg is not explicit)
        // For closures, only if it is explicitly present
        BlockArgNode blockArg = argsNode.getBlock();
        if ((s instanceof IRMethod) || (blockArg != null)) receiveClosureArg(blockArg, s);
    }

    public void receiveArgs(final ArgsNode argsNode, IRScope s) {
        receiveNonBlockArgs(argsNode, s);
        receiveBlockArg(argsNode, s);
    }

    @Override
    public void receiveBlockArgs(final IterNode node, IRScope s) {
        Node args = node.getVarNode();
        if (args instanceof ArgsNode) { // regular blocks
            ((IRClosure)s).setParameterList(Helpers.encodeParameterList((ArgsNode) args).split(";"));
            receiveArgs((ArgsNode)args, s);
        } else  {
            // for loops -- reuse code in IRBuilder:buildBlockArgsAssignment
            buildBlockArgsAssignment(args, s, null, 0, false, false, false);
        }
    }

    @Override
    public void receiveBlockClosureArg(Node node, IRScope s) {
        // Nothing to do here.  iterNode.blockVarNode is not valid in 1.9 mode
    }

    @Override
    public void receiveMethodArgs(final ArgsNode argsNode, IRScope s) {
        receiveArgs(argsNode, s);
    }

    protected void receiveArg(IRScope s, Variable v, Operand argsArray, int argIndex, boolean isSplat) {
        // We are in a nested receive situation -- when we are not at the root of a masgn tree
        // Ex: We are trying to receive (b,c) in this example: "|a, (b,c), d| = ..."
    }

    // This method is called to build arguments
    public void buildArgsMasgn(Node node, IRScope s, Operand argsArray, boolean isMasgnRoot, int preArgsCount, int postArgsCount, int index, boolean isSplat) {
        Variable v;
        switch (node.getNodeType()) {
            case DASGNNODE: {
                DAsgnNode dynamicAsgn = (DAsgnNode) node;
                v = getArgVariable(s, dynamicAsgn.getName(), dynamicAsgn.getDepth());
                if (isSplat) s.addInstr(new RestArgMultipleAsgnInstr(v, argsArray, preArgsCount, postArgsCount, index));
                else s.addInstr(new ReqdArgMultipleAsgnInstr(v, argsArray, preArgsCount, postArgsCount, index));
                break;
            }
            case LOCALASGNNODE: {
                LocalAsgnNode localVariable = (LocalAsgnNode) node;
                v = getArgVariable(s, localVariable.getName(), localVariable.getDepth());
                if (isSplat) s.addInstr(new RestArgMultipleAsgnInstr(v, argsArray, preArgsCount, postArgsCount, index));
                else s.addInstr(new ReqdArgMultipleAsgnInstr(v, argsArray, preArgsCount, postArgsCount, index));
                break;
            }
            case MULTIPLEASGN19NODE: {
                Variable oldArgs = null;
                MultipleAsgn19Node childNode = (MultipleAsgn19Node) node;
                if (!isMasgnRoot) {
                    v = s.getNewTemporaryVariable();
                    if (isSplat) s.addInstr(new RestArgMultipleAsgnInstr(v, argsArray, preArgsCount, postArgsCount, index));
                    else s.addInstr(new ReqdArgMultipleAsgnInstr(v, argsArray, preArgsCount, postArgsCount, index));
                    s.addInstr(new ToAryInstr(v, v, manager.getFalse()));
                    argsArray = v;
                }
                // Build
                buildMultipleAsgn19Assignment(childNode, s, argsArray, null);
                break;
            }
            default:
                throw new NotCompilableException("Shouldn't get here: " + node);
        }
    }

    // SSS: This method is called both for regular multiple assignment as well as argument passing
    //
    // Ex: a,b,*c=v  is a regular assignment and in this case, the "values" operand will be non-null
    // Ex: { |a,b,*c| ..} is the argument passing case
    public void buildMultipleAsgn19Assignment(final MultipleAsgn19Node multipleAsgnNode, IRScope s, Operand argsArray, Operand values) {
        final ListNode masgnPre = multipleAsgnNode.getPre();

        // Build assignments for specific named arguments
        int i = 0;
        if (masgnPre != null) {
            for (Node an: masgnPre.childNodes()) {
                if (values == null) {
                    buildArgsMasgn(an, s, argsArray, false, -1, -1, i, false);
                } else {
                    Variable rhsVal = s.getNewTemporaryVariable();
                    s.addInstr(new ReqdArgMultipleAsgnInstr(rhsVal, values, i));
                    buildAssignment(an, s, rhsVal);
                }
                i++;
            }
        }

        // Build an assignment for a splat, if any, with the rest of the operands!
        Node restNode = multipleAsgnNode.getRest();
        int postArgsCount = multipleAsgnNode.getPostCount();
        if (restNode != null) {
            if (restNode instanceof StarNode) {
                // do nothing
            } else if (values == null) {
                buildArgsMasgn(restNode, s, argsArray, false, i, postArgsCount, 0, true); // rest of the argument array!
            } else {
                Variable rhsVal = s.getNewTemporaryVariable();
                s.addInstr(new RestArgMultipleAsgnInstr(rhsVal, values, i, postArgsCount, 0));
                buildAssignment(restNode, s, rhsVal); // rest of the argument array!
            }
        }

        // Build assignments for rest of the operands
        final ListNode masgnPost = multipleAsgnNode.getPost();
        if (masgnPost != null) {
            int j = 0;
            for (Node an: masgnPost.childNodes()) {
                if (values == null) {
                    buildArgsMasgn(an, s, argsArray, false, i, postArgsCount, j, false);
                } else {
                    Variable rhsVal = s.getNewTemporaryVariable();
                    s.addInstr(new ReqdArgMultipleAsgnInstr(rhsVal, values, i, postArgsCount, j));  // Fetch from the end
                    buildAssignment(an, s, rhsVal);
                }
                j++;
            }
        }
    }

    // Non-arg masgn (actually a nested masgn)
    @Override
    public void buildVersionSpecificAssignment(Node node, IRScope s, Variable v) {
        switch (node.getNodeType()) {
        case MULTIPLEASGN19NODE: {
            s.addInstr(new ToAryInstr(v, v, manager.getFalse()));
            buildMultipleAsgn19Assignment((MultipleAsgn19Node)node, s, null, v);
            break;
        }
        default:
            throw new NotCompilableException("Can't build assignment node: " + node);
        }
    }

    @Override
    public Operand buildArgsPush(final ArgsPushNode node, IRScope s) {
        Operand v1 = build(node.getFirstNode(), s);
        Operand v2 = build(node.getSecondNode(), s);
        return new CompoundArray(v1, v2, true);
    }

    public Operand buildEncoding(EncodingNode node, IRScope s) {
        Variable ret = s.getNewTemporaryVariable();
        s.addInstr(new GetEncodingInstr(ret, node.getEncoding()));
        return ret;
    }

/* ------------------------------------------------------------------
 * This code is added on demand at runtime in the interpreter code.
 * For JIT, this may have to be added always!

    // These two methods could have been DRY-ed out if we had closures.
    // For now, just duplicating code.
    private void catchUncaughtBreakInLambdas(IRClosure s) {
        Label rBeginLabel = s.getNewLabel();
        Label rEndLabel   = s.getNewLabel();
        Label rescueLabel = s.getNewLabel();

        // protect the entire body as it exists now with the global ensure block
        s.addInstrAtBeginning(new ExceptionRegionStartMarkerInstr(rBeginLabel, rEndLabel, null, rescueLabel));
        s.addInstr(new ExceptionRegionEndMarkerInstr());

        // Receive exceptions (could be anything, but the handler only processes IRBreakJumps)
        s.addInstr(new LabelInstr(rescueLabel));
        Variable exc = s.getNewTemporaryVariable();
        s.addInstr(new ReceiveExceptionInstr(exc, false));  // no type-checking

        // Handle break using runtime helper
        // --> IRRuntimeHelpers.catchUncaughtBreakInLambdas(context, scope, bj, blockType)
        s.addInstr(new RuntimeHelperCall(null, "catchUncaughtBreakInLambdas", new Operand[]{exc} ));

        // End
        s.addInstr(new LabelInstr(rEndLabel));
    }
 * ------------------------------------------------------------------ */

    public Operand buildLambda(LambdaNode node, IRScope s) {
        IRClosure closure = new IRClosure(manager, s, false, node.getPosition().getStartLine(), node.getScope(), Arity.procArityOf(node.getArgs()), node.getArgumentType(), true);
        s.addClosure(closure);

        // Create a new nested builder to ensure this gets its own IR builder state
        // like the ensure block stack
        IRBuilder closureBuilder = newIRBuilder(manager);

        // Receive self
        closure.addInstr(new ReceiveSelfInstr(getSelf(closure)));

        // args
        closureBuilder.receiveBlockArgs(node, closure);
        closureBuilder.receiveBlockClosureArg(node.getBlockVarNode(), closure);

        Operand closureRetVal = node.getBody() == null ? manager.getNil() : closureBuilder.build(node.getBody(), closure);

        // can be U_NIL if the node is an if node with returns in both branches.
        if (closureRetVal != U_NIL) closure.addInstr(new ReturnInstr(closureRetVal));

        // Added as part of 'prepareForInterpretation' code.
        // catchUncaughtBreakInLambdas(closure);

        Variable lambda = s.getNewTemporaryVariable();
        s.addInstr(new BuildLambdaInstr(lambda, closure, node.getPosition()));
        return lambda;
    }

    @Override
    public Operand buildYield(YieldNode node, IRScope s) {
        boolean unwrap = true;
        Node argNode = node.getArgsNode();
        // Get rid of one level of array wrapping
        if (argNode != null && (argNode instanceof ArrayNode) && ((ArrayNode)argNode).size() == 1) {
            argNode = ((ArrayNode)argNode).getLast();
            unwrap = false;
        }

        Variable ret = s.getNewTemporaryVariable();
        s.addInstr(new YieldInstr(ret, s.getImplicitBlockArg(), build(argNode, s), unwrap));
        return ret;
    }

    // Non-arg masgn
    public Operand buildMultipleAsgn19(MultipleAsgn19Node multipleAsgnNode, IRScope s) {
        Operand  values = build(multipleAsgnNode.getValueNode(), s);
        Variable ret = getValueInTemporaryVariable(s, values);
        s.addInstr(new ToAryInstr(ret, ret, manager.getFalse()));
        buildMultipleAsgn19Assignment(multipleAsgnNode, s, null, ret);
        return ret;
    }

    // 1.9 specific defined? logic
    @Override
    public Operand buildVersionSpecificGetDefinitionIR(Node node, IRScope s) {
        switch (node.getNodeType()) {
            case ORNODE:
            case ANDNODE: {
                return new StringLiteral("expression");
            }
            case MULTIPLEASGN19NODE: {
                return new StringLiteral("assignment");
            }
            case DVARNODE: {
                return new StringLiteral("local-variable");
            }
            case BACKREFNODE: {
                return buildDefinitionCheck(s, new BackrefIsMatchDataInstr(s.getNewTemporaryVariable()), "global-variable");
            }
            case DREGEXPNODE:
            case DSTRNODE: {
                Operand v = super.buildVersionSpecificGetDefinitionIR(node, s);
                Label doneLabel = s.getNewLabel();
                Variable tmpVar = getValueInTemporaryVariable(s, v);
                s.addInstr(BNEInstr.create(tmpVar, manager.getNil(), doneLabel));
                s.addInstr(new CopyInstr(tmpVar, new StringLiteral("expression")));
                s.addInstr(new LabelInstr(doneLabel));
                return tmpVar;
            }
            case NOTNODE: {
                Operand v = buildGetDefinitionBase(((NotNode)node).getConditionNode(), s);
                Label doneLabel = s.getNewLabel();
                Variable tmpVar = getValueInTemporaryVariable(s, v);
                s.addInstr(BEQInstr.create(tmpVar, manager.getNil(), doneLabel));
                s.addInstr(new CopyInstr(tmpVar, new StringLiteral("method")));
                s.addInstr(new LabelInstr(doneLabel));
                return tmpVar;
            }
            case NTHREFNODE: {
            // SSS FIXME: Is there a reason to do this all with low-level IR?
            // Can't this all be folded into a Java method that would be part
            // of the runtime library, which then can be used by buildDefinitionCheck method above?
            // This runtime library would be used both by the interpreter & the compiled code!

                /* -------------------------------------------------------------------------------------
                 * We have to generate IR for this:
                 *    v = backref; (!(v instanceof RubyMatchData) || v.group(n).nil?) ? nil : "global-variable"
                 *
                 * which happens to be identical to: (where nthRef implicitly fetches backref again!)
                 *    v = backref; (!(v instanceof RubyMatchData) || nthRef(n).nil?) ? nil : "global-variable"
                 *
                 * I am using the second form since it let us encode it in fewer IR instructions.
                 * But, note that this second form is not as clean as the first one plus it fetches backref twice!
                 * ------------------------------------------------------------------------------------- */
                int n = ((NthRefNode) node).getMatchNumber();
                Label undefLabel = s.getNewLabel();
                Variable tmpVar = s.getNewTemporaryVariable();
                s.addInstr(new BackrefIsMatchDataInstr(tmpVar));
                s.addInstr(BEQInstr.create(tmpVar, manager.getFalse(), undefLabel));
                // SSS FIXME:
                // - Can/should I use BEQInstr(new NthRef(n), manager.getNil(), undefLabel)? instead of .nil? & compare with flag?
                // - Or, even create a new IsNilInstr and NotNilInstr to represent optimized scenarios where
                //   the nil? method is not monkey-patched?
                // This matters because if String.nil? is monkey-patched, the two sequences can behave differently.
                s.addInstr(CallInstr.create(tmpVar, new MethAddr("nil?"), new NthRef(n), NO_ARGS, null));
                s.addInstr(BEQInstr.create(tmpVar, manager.getTrue(), undefLabel));
                return buildDefnCheckIfThenPaths(s, undefLabel, new StringLiteral("global-variable"));
            }
            default: {
                return buildGenericGetDefinitionIR(node, s);
            }
        }
    }
}
