package org.jruby.compiler.ir; 

import org.jruby.ast.ArgumentNode;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ListNode;
import org.jruby.ast.MultipleAsgn19Node;
import org.jruby.ast.Node;
import org.jruby.ast.OptArgNode;
import org.jruby.ast.StarNode;
import org.jruby.compiler.NotCompilableException;
import org.jruby.compiler.ir.operands.BooleanLiteral;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.LocalVariable;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.UndefinedValue;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.instructions.BNEInstr;
import org.jruby.compiler.ir.instructions.LabelInstr;
import org.jruby.compiler.ir.instructions.ReceiveOptionalArgumentInstr;
import org.jruby.compiler.ir.instructions.jruby.ToAryInstr;

public class IRBuilder19 extends IRBuilder {
    protected int receiveOptArgs(final ArgsNode argsNode, IRScope s, int opt, int argIndex) {
        ListNode optArgs = argsNode.getOptArgs();
        for (int j = 0; j < opt; j++, argIndex++) {
                // Jump to 'l' if this arg is not null.  If null, fall through and build the default value!
            Label l = s.getNewLabel();
            OptArgNode n = (OptArgNode)optArgs.get(j);
            Variable av = s.getLocalVariable(n.getName(), 0);
            s.addInstr(new ReceiveOptionalArgumentInstr(av, argIndex));
            s.addInstr(BNEInstr.create(av, UndefinedValue.UNDEFINED, l)); // if 'av' is not undefined, go to default
            build(n.getValue(), s);
            s.addInstr(new LabelInstr(l));
        }
        return argIndex;
    }

    protected LocalVariable getBlockArgVariable(IRScope s, String name, int depth) {
        // For non-loops, this name will override any name that exists in outer scopes
        IRClosure cl = (IRClosure)s;
        return cl.isForLoopBody() ? cl.getLocalVariable(name, depth) : cl.getNewLocalVariable(name, depth);
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
                    buildBlockArgsAssignment(an, s, argsArray, i, false, false, false);
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
            buildBlockArgsAssignment(argsNode, s, argsArray, i, false, false, true); // rest of the argument array!
        }

        // SSS FIXME: Deal with post as well
    }

    public Operand buildMultipleAsgn19(MultipleAsgn19Node multipleAsgnNode, IRScope s) {
        Operand  values = build(multipleAsgnNode.getValueNode(), s);
        Variable ret = getValueInTemporaryVariable(s, values);
        buildMultipleAsgn19Assignment(multipleAsgnNode, s, null, ret);
        return ret;
    }

    public void buildVersionSpecificAssignment(Node node, IRScope s, Variable v) {
        switch (node.getNodeType()) {
        case MULTIPLEASGN19NODE: {
            s.addInstr(new ToAryInstr(v, v, BooleanLiteral.FALSE));
            buildMultipleAsgn19Assignment((MultipleAsgn19Node)node, s, v, null);
            break;
        }
        default: 
            throw new NotCompilableException("Can't build assignment node: " + node);
        }
    }

    public void buildVersionSpecificBlockArgsAssignment(Node node, IRScope s, Operand argsArray, int argIndex, boolean isMasgnRoot, boolean isClosureArg, boolean isSplat) {
        Variable v;
        switch (node.getNodeType()) {
            case MULTIPLEASGN19NODE: {
                Variable oldArgs = null;
                MultipleAsgn19Node childNode = (MultipleAsgn19Node) node;
                if (!isMasgnRoot) {
                    v = s.getNewTemporaryVariable();
                    receiveBlockArg(s, v, argsArray, argIndex, isClosureArg, isSplat);
                    s.addInstr(new ToAryInstr(v, v, BooleanLiteral.FALSE));
                    argsArray = v;
                }
                buildMultipleAsgn19Assignment(childNode, s, argsArray, null);
                break;
            }
            case ARGSNODE: {
                ArgsNode argsNode = (ArgsNode)node;
                final int required = argsNode.getRequiredArgsCount();
                final int opt = argsNode.getOptionalArgsCount();
                final int rest = argsNode.getRestArg();

                // pre
                ListNode preArgs  = argsNode.getPre();
                for (int i = 0; i < required; i++, argIndex++) {
                    ArgumentNode a = (ArgumentNode)preArgs.get(i);
                    Variable av = getBlockArgVariable((IRClosure)s, a.getName(), 0);
                    receiveBlockArg(s, av, argsArray, argIndex, isClosureArg, isSplat);
                }

                // opt
                ListNode optArgs = argsNode.getOptArgs();
                for (int j = 0; j < opt; j++, argIndex++) {
                    // Jump to 'l' if this arg is not null.  If null, fall through and build the default value!
                    Label l = s.getNewLabel();
                    OptArgNode n = (OptArgNode)optArgs.get(j);
                    Variable av = getBlockArgVariable((IRClosure)s, n.getName(), 0);
                    receiveBlockArg(s, av, argsArray, argIndex, isClosureArg, isSplat);
                    s.addInstr(BNEInstr.create(av, UndefinedValue.UNDEFINED, l)); // if 'av' is not undefined, go to default
                    build(n.getValue(), s);
                    s.addInstr(new LabelInstr(l));
                }
                
                // rest
                if (rest > -1) {
                    String argName = argsNode.getRestArgNode().getName();
                    argName = (argName.equals("")) ? "%_arg_array" : argName;
                    Variable av = getBlockArgVariable((IRClosure)s, argName, 0);
                    receiveBlockArg(s, av, argsArray, argIndex, isClosureArg, isSplat);
                }

                // FIXME: post??
                break;
            }
            default: throw new NotCompilableException("Can't build assignment node: " + node);
        }
    }
}
