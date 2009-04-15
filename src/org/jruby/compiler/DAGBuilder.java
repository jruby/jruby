package org.jruby.compiler;

import java.util.List;
import org.jruby.ast.BlockNode;
import org.jruby.ast.CallNode;
import org.jruby.ast.FCallNode;
import org.jruby.ast.FixnumNode;
import org.jruby.ast.IfNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.LocalVarNode;
import org.jruby.ast.NewlineNode;
import org.jruby.ast.Node;
import org.jruby.ast.ReturnNode;
import org.jruby.ast.RootNode;
import org.jruby.ast.VCallNode;
import org.jruby.ast.WhileNode;

public class DAGBuilder {
    public static class N {
        public String name;
        public Object[] payload;
        public N tail;

        public N(String name, Object... payload) {
            this.name = name;
            this.payload = payload;
        }

        public void append(N newTail) {
            tail.tail = newTail;
            tail = newTail;
        }

        public String getName() {
            return name;
        }

        public Object[] getPayload() {
            return payload;
        }

        public N getTail() {
            return tail;
        }
    }

    public static class B extends N {
       public N alt;

       public B(N tail, N alt) {
           super("BRANCH");
           this.tail = tail;
           this.alt = alt;
       }
    }

    public static class Pair {
        public N head;
        public N tail;
        public Pair(){}
        public Pair(N ht) {
            head = ht;
            tail = ht;
        }
        public Pair(N h, N t) {
            head = h;
            tail = t;
        }
        public void append(N t) {
            tail.tail = t;
            tail = t;
        }
        public void append(N h, N t) {
            tail.tail = h;
            tail = t;
        }
        public void append(Pair other) {
            tail.tail = other.head;
            tail = other.tail;
        }
        public void copy(Pair other) {
            head = other.head;
            tail = other.tail;
        }
    }

    public static Pair pair(N head, N tail) {
        return new Pair(head,tail);
    }

    public static Pair pair(N ht) {
        return new Pair(ht);
    }

    public static Pair pair() {
        return new Pair();
    }

    public static N node(String name, Object... payload) {
        return new N(name, payload);
    }

    public static Pair getExtents(Node node) {
        Pair pair;
        N join;
        switch (node.getNodeType()) {
        case BLOCKNODE:
            BlockNode blockNode = (BlockNode)node;
            pair = getExtents(blockNode.get(0));
            for (int i = 1; i < blockNode.size(); i++) {
                pair.append(getExtents(blockNode.get(i)));
            }
            break;
        case CALLNODE:
            CallNode callNode = (CallNode)node;
            pair = getExtents(callNode.getReceiverNode());
            for (Node n : (List<Node>)callNode.getArgsNode().childNodes()) {
                pair.append(getExtents(n));
            }
            N call = node("CALL", callNode.getName(), callNode.getArgsNode().childNodes().size() + 1);
            pair.append(call);
            break;
        case FCALLNODE:
            FCallNode fcallNode = (FCallNode)node;
            pair = pair(node("SELF"));
            for (Node n : (List<Node>)fcallNode.getArgsNode().childNodes()) {
                pair.append(getExtents(n));
            }
            N fcall = node("CALL", fcallNode.getName(), fcallNode.getArgsNode().childNodes().size() + 1);
            pair.append(fcall);
            break;
        case FIXNUMNODE:
            FixnumNode fixnumNode = (FixnumNode)node;
            pair = pair(node("FIXNUM", fixnumNode.getValue()));
            break;
        case IFNODE:
            IfNode ifNode = (IfNode)node;
            pair = getExtents(ifNode.getCondition());
            join = node("PHI");

            Pair then;
            if (ifNode.getThenBody() != null) {
                then = getExtents(ifNode.getThenBody());
                then.append(join);
            } else {
                then = pair(join);
            }

            Pair els;
            if (ifNode.getElseBody() != null) {
                els = getExtents(ifNode.getElseBody());
                els.append(join);
            } else {
                els = pair(join);
            }

            pair.append(new B(then.head, els.head), join);
            break;
        case LOCALASGNNODE:
            LocalAsgnNode localAsgnNode = (LocalAsgnNode)node;
            pair = getExtents(localAsgnNode.getValueNode());
            N lasgnTail = node("LSTORE", localAsgnNode.getIndex(), localAsgnNode.getDepth());
            pair.append(lasgnTail);
            break;
        case LOCALVARNODE:
            LocalVarNode localVarNode = (LocalVarNode)node;
            pair = pair(node("LLOAD", localVarNode.getIndex(), localVarNode.getDepth()));
            break;
        case NEWLINENODE:
            return getExtents(((NewlineNode)node).getNextNode());
        case RETURNNODE:
            ReturnNode returnNode = (ReturnNode)node;
            pair = getExtents(returnNode.getValueNode());
            pair.append(node("RETURN"));
            break;
        case ROOTNODE:
            return getExtents(((RootNode)node).getBodyNode());
        case VCALLNODE:
            VCallNode vcallNode = (VCallNode)node;
            pair = pair(node("SELF"));
            pair.append(node("CALL", vcallNode.getName(), 1));
            break;
        case WHILENODE:
            WhileNode whileNode = (WhileNode)node;
            if (whileNode.evaluateAtStart()) {
                pair = getExtents(whileNode.getConditionNode());
                join = node("PHI");

                Pair bodyPair;
                if (whileNode.getBodyNode() != null) {
                    bodyPair = getExtents(whileNode.getBodyNode());
                    bodyPair.append(pair);
                } else {
                    bodyPair = pair;
                }

                Pair escape = pair(join);

                pair.append(new B(bodyPair.head, escape.head), join);
                break;
            }
        default:
            throw new RuntimeException("unknown node: " + node);
        }
        return pair;
    }
}
