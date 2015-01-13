package org.jruby.runtime;

import org.jruby.Ruby;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ForNode;
import org.jruby.ast.IterNode;
import org.jruby.ast.LambdaNode;
import org.jruby.ast.MultipleAsgn19Node;
import org.jruby.ast.Node;
import org.jruby.ast.PostExeNode;
import org.jruby.ast.PreExeNode;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A representation of a Ruby method signature (argument layout, min/max, keyword layout, rest args).
 */
public class Signature {
    private final int pre;
    private final int opt;
    private final boolean rest;
    private final int post;
    private final Arity arity;

    public Signature(int pre, int opt, int post, boolean rest) {
        this.pre = pre;
        this.opt = opt;
        this.post = post;
        this.rest = rest;
        if (rest || opt != 0) {
            arity = Arity.createArity(-(pre + post + 1));
        } else {
            arity = Arity.fixed(pre + post);
        }
    }

    public int pre() { return pre; }
    public int opt() { return opt; }
    public boolean rest() { return rest; }
    public int post() { return post; }

    public int required() { return pre + post; }
    public Arity arity() { return arity; }

    public static Signature from(int pre, int opt, int post, boolean rest) {
        return new Signature(pre, opt, post, rest);
    }

    public static Signature from(IterNode iter) {
        if (iter instanceof ForNode) return from((ForNode)iter);
        if (iter instanceof PreExeNode) return from((PreExeNode)iter);
        if (iter instanceof PostExeNode) return from((PostExeNode)iter);

        Node var = iter.getVarNode();

        // all other iters aggregate ArgsNode
        ArgsNode args = (ArgsNode)var;
        return Signature.from(args.getPreCount(), args.getOptionalArgsCount(), args.getPostCount(), args.getRestArg() >= 0);
    }

    public static Signature from(ForNode iter) {
        Node var = iter.getVarNode();

        // ForNode can aggregate either a single node (required = 1) or masgn
        if (var instanceof MultipleAsgn19Node) {
            MultipleAsgn19Node masgn = (MultipleAsgn19Node)var;
            return Signature.from(masgn.getPreCount(), 0, masgn.getPostCount(), masgn.getRest() != null);
        }
        return Signature.from(1, 0, 0, false);
    }

    public static Signature from(PreExeNode iter) {
        return Signature.from(0, 0, 0, false);
    }

    public static Signature from(PostExeNode iter) {
        return Signature.from(0, 0, 0, false);
    }

    public long encode() {
        return ((long)pre << 48) | ((long)opt << 32) | ((long)post << 16) | (rest ? 1L : 0L);
    }

    public static Signature decode(long l) {
        return Signature.from(
                (int)(l >> 48) & 0xFFFF,
                (int)(l >> 32) & 0xFFFF,
                (int)(l >> 16) & 0xFFFF,
                (int)(l & 0xFFFF) == 1 ? true : false
        );
    }

    public String toString() {
        return "signature(" + pre + "," + opt + "," + post + "," + rest + ")";
    }

    public void checkArgs(Ruby runtime, IRubyObject[] args) {
        if (args.length < required()) {
            throw runtime.newArgumentError("wrong number of arguments (" + args.length + " for " + required() + ")");
        }
        if (!rest) {
            // no rest, so we have a maximum
            if (args.length > required() + opt()) {
                throw runtime.newArgumentError("wrong number of arguments (" + args.length + " for " + required() + opt + ")");
            }
        }
    }
}
