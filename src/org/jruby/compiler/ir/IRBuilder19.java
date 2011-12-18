package org.jruby.compiler.ir; 

import org.jruby.ast.ArgumentNode;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.BlockArgNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.EncodingNode;
import org.jruby.ast.IterNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.ListNode;
import org.jruby.ast.MultipleAsgn19Node;
import org.jruby.ast.Node;
import org.jruby.ast.OptArgNode;
import org.jruby.ast.StarNode;
import org.jruby.compiler.NotCompilableException;
import org.jruby.compiler.ir.operands.Array;
import org.jruby.compiler.ir.operands.BooleanLiteral;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.LocalVariable;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.UndefinedValue;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.instructions.BNEInstr;
import org.jruby.compiler.ir.instructions.CopyInstr;
import org.jruby.compiler.ir.instructions.GetArrayInstr;
import org.jruby.compiler.ir.instructions.LabelInstr;
import org.jruby.compiler.ir.instructions.ReceiveArgumentInstruction;
import org.jruby.compiler.ir.instructions.ReceiveClosureInstr;
import org.jruby.compiler.ir.instructions.ReceiveSelfInstruction;
import org.jruby.compiler.ir.instructions.jruby.CheckArityInstr;
import org.jruby.compiler.ir.instructions.jruby.ToAryInstr;
import org.jruby.compiler.ir.instructions.ruby19.GetEncodingInstr;
import org.jruby.compiler.ir.instructions.ruby19.ReceiveOptArgInstr;
import org.jruby.compiler.ir.instructions.ruby19.ReceiveRestArgInstr;
import org.jruby.compiler.ir.instructions.ruby19.ReceiveRequiredArgInstr;

public class IRBuilder19 extends IRBuilder {
    protected Operand buildVersionSpecificNodes(Node node, IRScope s) {
        switch (node.getNodeType()) {
            case ENCODINGNODE: return buildEncoding((EncodingNode)node, s);
            case MULTIPLEASGN19NODE: return buildMultipleAsgn19((MultipleAsgn19Node) node, s);
            default: throw new NotCompilableException("Unknown node encountered in builder: " + node.getClass());
        }
    }

    protected LocalVariable getBlockArgVariable(IRScope s, String name, int depth) {
        throw new NotCompilableException("Cannot ask for block-arg variable in 1.9 mode");
    }

    public void buildVersionSpecificBlockArgsAssignment(Node node, IRScope s, Operand argsArray, int argIndex, boolean isMasgnRoot, boolean isClosureArg, boolean isSplat) {
       throw new NotCompilableException("Should not have come here for block args assignment in 1.9 mode: " + node);
    }

    protected LocalVariable getArgVariable(IRScope s, String name, int depth) {
        // For non-loops, this name will override any name that exists in outer scopes
        return ((s instanceof IRClosure) && ((IRClosure)s).isForLoopBody()) ? s.getLocalVariable(name, depth) : s.getNewLocalVariable(name);
    }

    public void receiveRequiredArg(Node node, IRScope s, int argIndex, boolean post, int totalRequired, int totalOptional) {
        Variable v;
        switch (node.getNodeType()) {
            case ARGUMENTNODE: {
                ArgumentNode a = (ArgumentNode)node;
                if (post) {
                    s.addInstr(new ReceiveRequiredArgInstr(s.getNewLocalVariable(a.getName()), argIndex, totalRequired, totalOptional));
                } else {
                    s.addInstr(new ReceiveArgumentInstruction(s.getNewLocalVariable(a.getName()), argIndex));
                }
                break;
            }
            case MULTIPLEASGN19NODE: {
                MultipleAsgn19Node childNode = (MultipleAsgn19Node) node;
                v = s.getNewTemporaryVariable();
                if (post) {
                    s.addInstr(new ReceiveRequiredArgInstr(v, argIndex, totalRequired, totalOptional));
                } else {
                    s.addInstr(new ReceiveArgumentInstruction(v, argIndex));
                }
                s.addInstr(new ToAryInstr(v, v, BooleanLiteral.FALSE));
                buildMultipleAsgn19Assignment(childNode, s, v, null);
                break;
            }
            default: throw new NotCompilableException("Can't build assignment node: " + node);
        }
    }

    public void receiveArgs(final ArgsNode argsNode, IRScope s) {
        final int requiredPre = argsNode.getPreCount();
        final int requiredPost = argsNode.getPostCount();
        final int required = argsNode.getRequiredArgsCount(); // requiredPre + requiredPost
        int opt = argsNode.getOptionalArgsCount();
        int rest = argsNode.getRestArg();

        s.getStaticScope().setArities(required, opt, rest);

        // For closures, we don't need the check arity call + self would have been received already
        if (s instanceof IRMethod) {
            // FIXME: Expensive to this explicitly?  But, 2 advantages:
            // (a) on inlining, we'll be able to get rid of these checks in almost every case.
            // (b) compiler to bytecode will anyway generate this and this is explicit.
            // For now, we are going explicit instruction route.  But later, perhaps can make this implicit in the method setup preamble?  
            s.addInstr(new CheckArityInstr(required, opt, rest));
            s.addInstr(new ReceiveSelfInstruction(getSelf(s)));
        }

        // Other args begin at index 0
        int argIndex = 0;

        // Pre(-opt and rest) required args
        ListNode preArgs = argsNode.getPre();
        for (int i = 0; i < requiredPre; i++, argIndex++) {
            receiveRequiredArg(preArgs.get(i), s, argIndex, false, 0, 0);
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
                Variable av = s.getNewLocalVariable(n.getName());
                // You need at least required+j+1 incoming args for this opt arg to get an arg at all
                s.addInstr(new ReceiveOptArgInstr(av, argIndex, required+j+1));
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
            argName = (argName == null || argName.equals("")) ? "%_arg_array" : argName;

            // You need at least required+opt+1 incoming args for the rest arg to get any args at all
            // If it is going to get something, then it should ignore required+opt args from the beginning
            // because they have been accounted for already.
            s.addInstr(new ReceiveRestArgInstr(s.getNewLocalVariable(argName), argIndex, required+opt+1, required+opt));
            argIndex++;
        }

        // Post(-opt and rest) required args
        ListNode postArgs = argsNode.getPost();
        for (int i = 0; i < requiredPost; i++, argIndex++) {
            receiveRequiredArg(postArgs.get(i), s, argIndex, true, required, opt+rest);
        }
    }

    public void receiveClosureArg(BlockArgNode blockVarNode, IRScope s) {
        Variable blockVar = null;
        if (blockVarNode != null) {
            blockVar = s.getNewLocalVariable(blockVarNode.getName());
            s.addInstr(new ReceiveClosureInstr(blockVar));
        }

        // SSS FIXME: This instruction is only needed if there is an yield instr somewhere!
        // In addition, store the block argument in an implicit block variable
        Variable implicitBlockArg = s.getImplicitBlockArg();
        if (blockVar == null) s.addInstr(new ReceiveClosureInstr(implicitBlockArg));
        else s.addInstr(new CopyInstr(implicitBlockArg, blockVar));
    }

    public void receiveBlockArgs(final IterNode node, IRScope s) {
        Node args = node.getVarNode();
        if (args instanceof ArgsNode) { // regular blocks
            receiveArgs((ArgsNode)args, s);
        } else if (args instanceof LocalAsgnNode) { // for loops
            // Use local var depth because for-loop uses vars from the surrounding scope
            // SSS FIXME: Verify that this is correct
            LocalAsgnNode lan = (LocalAsgnNode)args;
            s.addInstr(new ReceiveArgumentInstruction(s.getLocalVariable(lan.getName(), lan.getDepth()), 0));
        }
    }

    public void receiveBlockClosureArg(Node node, IRScope s) {
        if (node != null) receiveClosureArg((BlockArgNode)node, s);
    }

    public void receiveMethodArgs(final ArgsNode argsNode, IRScope s) {
        receiveArgs(argsNode, s);
    }

    public void receiveMethodClosureArg(ArgsNode argsNode, IRScope s) {
        receiveClosureArg(argsNode.getBlock(), s);
    }

    protected void receiveArg(IRScope s, Variable v, Operand argsArray, int argIndex, boolean isSplat) {
        // We are in a nested receive situation -- when we are not at the root of a masgn tree
        // Ex: We are trying to receive (b,c) in this example: "|a, (b,c), d| = ..."
    }

    // This method is called to build arguments
    public void buildArgsMasgn(Node node, IRScope s, Operand argsArray, int argIndex, boolean isMasgnRoot, boolean isSplat) {
        Variable v;
        switch (node.getNodeType()) {
            case DASGNNODE: {
                DAsgnNode dynamicAsgn = (DAsgnNode) node;
                v = getArgVariable((IRClosure)s, dynamicAsgn.getName(), dynamicAsgn.getDepth());
                s.addInstr(new GetArrayInstr(v, argsArray, argIndex, isSplat));
                break;
            }
            case LOCALASGNNODE: {
                LocalAsgnNode localVariable = (LocalAsgnNode) node;
                int depth = localVariable.getDepth();
                v = getArgVariable((IRClosure)s, localVariable.getName(), depth);
                s.addInstr(new GetArrayInstr(v, argsArray, argIndex, isSplat));
                break;
            }
            case MULTIPLEASGN19NODE: {
                Variable oldArgs = null;
                MultipleAsgn19Node childNode = (MultipleAsgn19Node) node;
                if (!isMasgnRoot) {
                    v = s.getNewTemporaryVariable();
                    s.addInstr(new GetArrayInstr(v, argsArray, argIndex, isSplat));
                    s.addInstr(new ToAryInstr(v, v, BooleanLiteral.FALSE));
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
        final ListNode sourceArray = multipleAsgnNode.getPre();

        // First, build assignments for specific named arguments
        int i = 0; 
        if (sourceArray != null) {
            ListNode headNode = (ListNode) sourceArray;
            for (Node an: headNode.childNodes()) {
                if (values == null) {
                    buildArgsMasgn(an, s, argsArray, i, false, false);
                } else {
                    buildAssignment(an, s, values, i, false);
                }
                i++;
            }
        }

        // First, build an assignment for a splat, if any, with the rest of the args!
        Node argsNode = multipleAsgnNode.getRest();
        if (argsNode == null) {
            if (sourceArray == null)
                throw new NotCompilableException("Something's wrong, multiple assignment with no head or args at: " + multipleAsgnNode.getPosition());
        } else if (argsNode instanceof StarNode) {
            // do nothing
        } else if (values != null) {
            buildAssignment(argsNode, s, values, i, true); // rest of the argument array!
        } else {
            buildArgsMasgn(argsNode, s, argsArray, i, false, true); // rest of the argument array!
        }

        // SSS FIXME: Deal with post as well
    }

    // Non-arg masgn (actually a nested masgn)
    @Override
    public void buildVersionSpecificAssignment(Node node, IRScope s, Variable v) {
        switch (node.getNodeType()) {
        case MULTIPLEASGN19NODE: {
            s.addInstr(new ToAryInstr(v, v, BooleanLiteral.FALSE));
            buildMultipleAsgn19Assignment((MultipleAsgn19Node)node, s, null, v);
            break;
        }
        default: 
            throw new NotCompilableException("Can't build assignment node: " + node);
        }
    }

    public Operand buildEncoding(EncodingNode node, IRScope s) {
        Variable ret = s.getNewTemporaryVariable();
        s.addInstr(new GetEncodingInstr(ret, node.getEncoding()));
        return ret;
    }

    // Non-arg masgn
    public Operand buildMultipleAsgn19(MultipleAsgn19Node multipleAsgnNode, IRScope s) {
        Operand  values = build(multipleAsgnNode.getValueNode(), s);
        Variable ret = getValueInTemporaryVariable(s, values);
        s.addInstr(new ToAryInstr(ret, ret, BooleanLiteral.FALSE));
        buildMultipleAsgn19Assignment(multipleAsgnNode, s, null, ret);
        return ret;
    }
}
