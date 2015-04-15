package org.jruby.runtime;

import org.jruby.Ruby;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ArgumentNode;
import org.jruby.ast.ForNode;
import org.jruby.ast.IterNode;
import org.jruby.ast.ListNode;
import org.jruby.ast.MultipleAsgn19Node;
import org.jruby.ast.Node;
import org.jruby.ast.PostExeNode;
import org.jruby.ast.PreExeNode;
import org.jruby.ast.StarNode;
import org.jruby.ast.UnnamedRestArgNode;
import org.jruby.ast.RequiredKeywordArgumentValueNode;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.TypeConverter;

/**
 * A representation of a Ruby method signature (argument layout, min/max, keyword layout, rest args).
 */
public class Signature {
    public enum Rest { NONE, NORM, ANON, STAR }

    public static final Signature NO_ARGUMENTS = new Signature(0, 0, 0, Rest.NONE, false);
    public static final Signature ONE_ARGUMENT = new Signature(1, 0, 0, Rest.NONE, false);
    public static final Signature TWO_ARGUMENTS = new Signature(2, 0, 0, Rest.NONE, false);
    public static final Signature THREE_ARGUMENTS = new Signature(3, 0, 0, Rest.NONE, false);
    public static final Signature OPTIONAL = new Signature(0, 0, 0, Rest.NORM, false);
    public static final Signature ONE_REQUIRED = new Signature(1, 0, 0, Rest.NORM, false);
    public static final Signature TWO_REQUIRED = new Signature(2, 0, 0, Rest.NORM, false);
    public static final Signature THREE_REQUIRED = new Signature(3, 0, 0, Rest.NORM, false);

    private final int pre;
    private final int opt;
    private final Rest rest;
    private final int post;
    private final boolean kwargs;
    private final Arity arity;

    // FIXME: three booleans for kwargs?  Even if we keep these we should make it cheaper to get these values from Iter/friends
    private boolean requiredKwargs;
    private boolean restKwargs;

    public Signature(int pre, int opt, int post, Rest rest, boolean kwargs) {
        this.pre = pre;
        this.opt = opt;
        this.post = post;
        this.rest = rest;
        this.kwargs = kwargs;

        // NOTE: Some logic to *assign* variables still uses Arity, which treats Rest.ANON (the
        //       |a,| form) as a rest arg for destructuring purposes. However ANON does *not*
        //       permit more than required args to be passed to a lambda, so we do not consider
        //       it a "true" rest arg for arity-checking purposes below in checkArity.
        if (rest != Rest.NONE || opt != 0) {
            arity = Arity.createArity(-(required() + 1));
        } else {
            arity = Arity.fixed(required() + getRequiredKeywordCount());
        }
    }

    public Signature(int pre, int opt, int post, Rest rest, boolean kwargs, boolean requiredKwargs, boolean restKwargs) {
        this.pre = pre;
        this.opt = opt;
        this.post = post;
        this.rest = rest;
        this.kwargs = kwargs;
        this.requiredKwargs = requiredKwargs;
        this.restKwargs = restKwargs;

        // NOTE: Some logic to *assign* variables still uses Arity, which treats Rest.ANON (the
        //       |a,| form) as a rest arg for destructuring purposes. However ANON does *not*
        //       permit more than required args to be passed to a lambda, so we do not consider
        //       it a "true" rest arg for arity-checking purposes below in checkArity.
        if (rest != Rest.NONE || opt != 0) {
            arity = Arity.createArity(-(required() + 1));
        } else {
            arity = Arity.fixed(required() + getRequiredKeywordCount());
        }
    }

    public int getRequiredKeywordCount() {
        return requiredKwargs ? 1 : 0;
    }

    public boolean restKwargs() {
        return restKwargs;
    }

    public int pre() { return pre; }
    public int opt() { return opt; }
    public Rest rest() { return rest; }
    public int post() { return post; }
    public boolean kwargs() { return kwargs; }

    public int required() { return pre + post; }
    public Arity arity() { return arity; }

    public static Signature from(int pre, int opt, int post, Rest rest, boolean kwargs) {
        if (opt == 0 && post == 0 && !kwargs) {
            switch (pre) {
                case 0:
                    switch (rest) {
                        case NONE:
                            return Signature.NO_ARGUMENTS;
                        case NORM:
                            return Signature.OPTIONAL;
                    }
                    break;
                case 1:
                    switch (rest) {
                        case NONE:
                            return Signature.ONE_ARGUMENT;
                        case NORM:
                            return Signature.ONE_REQUIRED;
                    }
                    break;
                case 2:
                    switch (rest) {
                        case NONE:
                            return Signature.TWO_ARGUMENTS;
                        case NORM:
                            return Signature.TWO_REQUIRED;
                    }
                    break;
                case 3:
                    switch (rest) {
                        case NONE:
                            return Signature.THREE_ARGUMENTS;
                        case NORM:
                            return Signature.THREE_REQUIRED;
                    }
                    break;
            }
        }
        return new Signature(pre, opt, post, rest, kwargs);
    }

    public static Signature from(int pre, int opt, int post, Rest rest, boolean kwargs, boolean requiredKwargs, boolean restKwargs) {
        if (opt == 0 && post == 0 && !kwargs) {
            switch (pre) {
                case 0:
                    switch (rest) {
                        case NONE:
                            return Signature.NO_ARGUMENTS;
                        case NORM:
                            return Signature.OPTIONAL;
                    }
                    break;
                case 1:
                    switch (rest) {
                        case NONE:
                            return Signature.ONE_ARGUMENT;
                        case NORM:
                            return Signature.ONE_REQUIRED;
                    }
                    break;
                case 2:
                    switch (rest) {
                        case NONE:
                            return Signature.TWO_ARGUMENTS;
                        case NORM:
                            return Signature.TWO_REQUIRED;
                    }
                    break;
                case 3:
                    switch (rest) {
                        case NONE:
                            return Signature.THREE_ARGUMENTS;
                        case NORM:
                            return Signature.THREE_REQUIRED;
                    }
                    break;
            }
        }
        return new Signature(pre, opt, post, rest, kwargs, requiredKwargs, restKwargs);
    }

    public static Signature from(IterNode iter) {
        if (iter instanceof ForNode) return from((ForNode)iter);
        if (iter instanceof PreExeNode) return from((PreExeNode)iter);
        if (iter instanceof PostExeNode) return from((PostExeNode)iter);

        Node var = iter.getVarNode();

        // all other iters aggregate ArgsNode
        ArgsNode args = (ArgsNode)var;

        ArgumentNode restArg = args.getRestArgNode();
        Rest rest = restArg != null ? restFromArg(restArg) : Rest.NONE;

        return Signature.from(args.getPreCount(), args.getOptionalArgsCount(), args.getPostCount(), rest,
                args.hasKwargs(), hasRequiredKeywordArg(args), args.hasKeyRest());
    }

    private static boolean hasRequiredKeywordArg(ArgsNode args) {
        if (args.getKeywords() == null) return false;

        for (Node keyWordNode : args.getKeywords().childNodes()) {
            for (Node asgnNode : keyWordNode.childNodes()) {
                if (isRequiredKeywordArgumentValueNode(asgnNode)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isRequiredKeywordArgumentValueNode(Node asgnNode) {
        return asgnNode.childNodes().get(0) instanceof RequiredKeywordArgumentValueNode;
    }

    private static Rest restFromArg(Node restArg) {
        Rest rest;
        if (restArg instanceof UnnamedRestArgNode) {
            UnnamedRestArgNode anonRest = (UnnamedRestArgNode) restArg;
            if (anonRest.isStar()) {
                rest = Rest.STAR;
            } else {
                rest = Rest.ANON;
            }
        } else if (restArg instanceof StarNode) {
            rest = Rest.STAR;
        } else {
            rest = Rest.NORM;
        }
        return rest;
    }

    public static Signature from(ForNode iter) {
        Node var = iter.getVarNode();

        // ForNode can aggregate either a single node (required = 1) or masgn
        if (var instanceof MultipleAsgn19Node) {
            MultipleAsgn19Node masgn = (MultipleAsgn19Node)var;

            Rest rest = Rest.NONE;
            if (masgn.getRest() != null) {
                Node restArg = masgn.getRest();
                rest = restFromArg(restArg);
            }
            return Signature.from(masgn.getPreCount(), 0, masgn.getPostCount(), rest, false);
        }
        return Signature.ONE_ARGUMENT;
    }

    public static Signature from(PreExeNode iter) {
        return Signature.NO_ARGUMENTS;
    }

    public static Signature from(PostExeNode iter) {
        return Signature.NO_ARGUMENTS;
    }

    private static final int MAX_ENCODED_ARGS_EXPONENT = 8;
    private static final int MAX_ENCODED_ARGS_MASK = 0xFF;
    private static final int ENCODE_RESTKWARGS_SHIFT = 0;
    private static final int ENCODE_REQKWARGS_SHIFT = ENCODE_RESTKWARGS_SHIFT + 1;
    private static final int ENCODE_KWARGS_SHIFT = ENCODE_REQKWARGS_SHIFT + 1;
    private static final int ENCODE_REST_SHIFT = ENCODE_KWARGS_SHIFT + 1;
    private static final int ENCODE_POST_SHIFT = ENCODE_REST_SHIFT + MAX_ENCODED_ARGS_EXPONENT;
    private static final int ENCODE_OPT_SHIFT = ENCODE_POST_SHIFT + MAX_ENCODED_ARGS_EXPONENT;
    private static final int ENCODE_PRE_SHIFT = ENCODE_OPT_SHIFT + MAX_ENCODED_ARGS_EXPONENT;

    public long encode() {
        return
                ((long)pre << ENCODE_PRE_SHIFT) |
                        ((long)opt << ENCODE_OPT_SHIFT) |
                        ((long)post << ENCODE_POST_SHIFT) |
                        (rest.ordinal() << ENCODE_REST_SHIFT) |
                        ((kwargs?1:0) << ENCODE_KWARGS_SHIFT) |
                        ((requiredKwargs?1:0) << ENCODE_REQKWARGS_SHIFT) |
                        ((restKwargs?1:0) << ENCODE_RESTKWARGS_SHIFT);
    }

    public static Signature decode(long l) {
        return Signature.from(
                (int)(l >> ENCODE_PRE_SHIFT) & MAX_ENCODED_ARGS_MASK,
                (int)(l >> ENCODE_OPT_SHIFT) & MAX_ENCODED_ARGS_MASK,
                (int)(l >> ENCODE_POST_SHIFT) & MAX_ENCODED_ARGS_MASK,
                Rest.values()[(int)((l >> ENCODE_REST_SHIFT) & MAX_ENCODED_ARGS_MASK)],
                ((int)(l >> ENCODE_KWARGS_SHIFT) & 0x1)==1 ? true : false,
                ((int)(l >> ENCODE_REQKWARGS_SHIFT) & 0x1)==1 ? true : false,
                ((int)(l >> ENCODE_RESTKWARGS_SHIFT) & 0x1)==1 ? true : false

        );
    }

    public String toString() {
        return "signature(pre=" + pre + ",opt=" + opt + ",post=" + post + ",rest=" + rest + ",kwargs=" + kwargs + ",kwreq=" + requiredKwargs + ",kwrest=" + restKwargs + ")";
    }

    public void checkArity(Ruby runtime, IRubyObject[] args) {
        if (args.length < required()) {
            throw runtime.newArgumentError("wrong number of arguments (" + args.length + " for " + required() + ")");
        }
        if (rest == Rest.NONE || rest == Rest.ANON) {
            // no rest, so we have a maximum
            if (args.length > required() + opt()) {
                if (kwargs && !TypeConverter.checkHashType(runtime, args[args.length - 1]).isNil()) {
                    // we have kwargs and a potential kwargs hash, check with length - 1
                    if (args.length - 1 > required() + opt()) {
                        throw runtime.newArgumentError("wrong number of arguments (" + args.length + " for " + (required() + opt) + ")");
                    }
                } else {
                    throw runtime.newArgumentError("wrong number of arguments (" + args.length + " for " + (required() + opt) + ")");
                }
            }
        }
    }
}
