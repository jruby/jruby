package org.jruby.ast.util;

import org.jruby.ast.*;
import org.jruby.ir.builder.StringStyle;
import org.jruby.util.KeyValuePair;

import java.util.List;

/**
 * Where MRI reports a statement's line event, and so counts its line coverage: on the line of the statement's first
 * instruction. That is a later line whenever evaluating the statement starts with a part of it on one, as in
 * <code>x =\n  y.size</code> (line 2) or an assignment of a heredoc whose body starts with an interpolation.
 * The parser marks this line as coverable (as does Coverage.line_stub) and the IR builder counts the statement on it.
 */
public final class LineEvents {
    private LineEvents() {}

    public static int lineOf(Node statement) {
        return firstInstruction(statement).getLine();
    }

    /**
     * The node whose instructions come first in statement's, on the line of the statement's line event. A statement
     * inside another with the same first instruction shares its line event.
     */
    public static Node firstInstruction(Node statement) {
        Node node = statement;

        for (Node first = firstEvaluated(node); first != null && first.getLine() >= 0; first = firstEvaluated(node)) {
            node = first;
        }

        return node;
    }

    /**
     * The part of node evaluated first, when its instructions start with that part's instead of one of its own.
     */
    private static Node firstEvaluated(Node node) {
        return switch (node) {
            case BeginNode begin -> begin.getBodyNode();
            case BlockNode block -> block.size() > 0 ? block.get(0) : null;
            case RescueNode rescue -> rescue.getBodyNode();
            case EnsureNode ensure -> ensure.getBodyNode();
            case ConstDeclNode decl -> decl.getConstNode() instanceof Colon2Node path && path.getLeftNode() != null ?
                    path.getLeftNode() : decl.getValueNode();
            case MultipleAsgnNode masgn -> firstReceiver(masgn.getPre(), masgn.getValueNode());
            case AssignableNode asgn -> asgn.getValueNode();
            case AttrAssignNode asgn -> asgn.getReceiverNode();
            case OpAsgnNode asgn -> asgn.getReceiverNode();
            case OpElementAsgnNode asgn -> asgn.getReceiverNode();
            case OpAsgnOrNode asgn -> asgn.getFirstNode();
            case OpAsgnAndNode asgn -> asgn.getFirstNode();
            case OpAsgnConstDeclNode asgn -> asgn.getFirstNode();
            case CallNode call -> call.getReceiverNode();
            case Match2Node match -> match.getReceiverNode();
            case Match3Node match -> match.getReceiverNode();
            case AndNode and -> and.getFirstNode();
            case OrNode or -> or.getFirstNode();
            case IfNode ifNode -> ifNode.getCondition();
            case CaseNode caseNode -> caseNode.getCaseNode() != null ? caseNode.getCaseNode() : firstWhen(caseNode.getCases());
            case Colon2Node path -> path.getLeftNode();
            case DotNode dot -> dot.getBeginNode();
            case SplatNode splat -> splat.getValue();
            case ArgsCatNode cat -> cat.getFirstNode();
            case ArgsPushNode push -> push.getFirstNode();
            case BreakNode breakNode -> breakNode.getValueNode();
            case NextNode next -> next.getValueNode();
            case ReturnNode ret -> ret.getValueNode();
            case EvStrNode str -> str.getBody();
            case ArrayNode array -> array.size() > 0 && !isStaticArray(array) ? array.get(0) : null;
            case HashNode hash -> firstKeyUnlessStatic(hash.getPairs());
            case DXStrNode ignored -> null; // the receiver of ` is self
            case DRegexpNode regexp -> regexp.getOnce() ? null : firstInterpolation(regexp);
            case DNode str -> firstInterpolation(str);
            default -> null;
        };
    }

    // Targets with a receiver have it evaluated before the value.
    private static Node firstReceiver(ListNode targets, Node value) {
        if (targets != null) {
            for (int i = 0; i < targets.size(); i++) {
                if (targets.get(i) instanceof AttrAssignNode asgn) return asgn.getReceiverNode();
            }
        }

        return value;
    }

    private static Node firstWhen(ListNode cases) {
        return cases != null && cases.size() > 0 && cases.get(0) instanceof WhenNode when ? when.getExpressionNodes() : null;
    }

    // An interpolated string starts with its first interpolation's instructions, unless a literal part comes first
    // or the interpolation is the only part (then MRI starts by pushing an empty string).
    private static Node firstInterpolation(DNode str) {
        Node first = null;
        int parts = 0;

        for (int i = 0; i < str.size(); i++) {
            Node part = str.get(i);
            if (part instanceof StrNode literal && literal.getValue().realSize() == 0) continue;
            if (parts++ == 0) first = part;
        }

        // MRI folds an interpolated literal string into the parts around it
        boolean interpolation = first instanceof EvStrNode evStr && !(evStr.getBody() instanceof StrNode);

        return parts > 1 && interpolation ? first : null;
    }

    // MRI builds an array of only simple literals (with frozen string literals, strings too) as one object, on the
    // array's line.
    private static boolean isStaticArray(ArrayNode array) {
        for (int i = 0; i < array.size(); i++) {
            if (!isStaticLiteral(array.get(i)) && !isFrozenString(array.get(i))) return false;
        }

        return true;
    }

    // Likewise a hash starting with a pair of simple literals (a string key is frozen, so it counts as one).
    private static Node firstKeyUnlessStatic(List<KeyValuePair<Node, Node>> pairs) {
        if (pairs.isEmpty()) return null;

        Node key = pairs.get(0).getKey();
        if (key == null) return null; // **splat

        Node value = pairs.get(0).getValue();

        return (isStaticLiteral(key) || key instanceof StrNode) && (isStaticLiteral(value) || isFrozenString(value)) ? null : key;
    }

    private static boolean isFrozenString(Node node) {
        return node instanceof StrNode str && str.getStringStyle() == StringStyle.Frozen;
    }

    private static boolean isStaticLiteral(Node node) {
        return node instanceof FixnumNode || node instanceof BignumNode || node instanceof FloatNode ||
                node instanceof RationalNode || node instanceof ComplexNode || node instanceof SymbolNode ||
                node instanceof NilNode || node instanceof TrueNode || node instanceof FalseNode ||
                node instanceof RegexpNode || node instanceof DSymbolNode symbol && !isInterpolated(symbol);
    }

    // A DSymbolNode need not interpolate ("label": 1).
    private static boolean isInterpolated(DNode str) {
        for (int i = 0; i < str.size(); i++) {
            if (!(str.get(i) instanceof StrNode)) return true;
        }

        return false;
    }
}
