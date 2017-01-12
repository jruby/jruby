package org.jruby.truffle.parser;

import org.jruby.truffle.parser.ast.ArgsParseNode;
import org.jruby.truffle.parser.ast.ArgumentParseNode;
import org.jruby.truffle.parser.ast.ForParseNode;
import org.jruby.truffle.parser.ast.IterParseNode;
import org.jruby.truffle.parser.ast.MultipleAsgnParseNode;
import org.jruby.truffle.parser.ast.ParseNode;
import org.jruby.truffle.parser.ast.PostExeParseNode;
import org.jruby.truffle.parser.ast.PreExeParseNode;
import org.jruby.truffle.parser.ast.StarParseNode;
import org.jruby.truffle.parser.ast.UnnamedRestArgParseNode;

/**
 * A representation of a Ruby method signature (argument layout, min/max, keyword layout, rest args).
 */
public class Signature {
    public enum Rest {
        NONE, NORM, ANON, STAR;

        private static final Rest[] VALUES = values();

        public static Rest fromOrdinal(int ordinal) {
            if (ordinal < 0 || ordinal >= VALUES.length) {
                throw new RuntimeException("invalid Rest: " + ordinal);
            }
            return VALUES[ordinal];
        }
    }

    public static final Signature NO_ARGUMENTS = new Signature(0, 0, 0, Rest.NONE, 0, 0, false);
    public static final Signature ONE_ARGUMENT = new Signature(1, 0, 0, Rest.NONE, 0, 0, false);
    public static final Signature TWO_ARGUMENTS = new Signature(2, 0, 0, Rest.NONE, 0, 0, false);
    public static final Signature THREE_ARGUMENTS = new Signature(3, 0, 0, Rest.NONE, 0, 0, false);
    public static final Signature OPTIONAL = new Signature(0, 0, 0, Rest.NORM, 0, 0, false);
    public static final Signature ONE_REQUIRED = new Signature(1, 0, 0, Rest.NORM, 0, 0, false);
    public static final Signature TWO_REQUIRED = new Signature(2, 0, 0, Rest.NORM, 0, 0, false);
    public static final Signature THREE_REQUIRED = new Signature(3, 0, 0, Rest.NORM, 0, 0, false);

    private final short pre;
    private final short opt;
    private final Rest rest;
    private final short post;
    private final short kwargs;
    private final short requiredKwargs;
    private final boolean restKwargs;

    public Signature(int pre, int opt, int post, Rest rest, int kwargs, int requiredKwargs, boolean restKwargs) {
        this.pre = (short) pre;
        this.opt = (short) opt;
        this.post = (short) post;
        this.rest = rest;
        this.kwargs = (short) kwargs;
        this.requiredKwargs = (short) requiredKwargs;
        this.restKwargs = restKwargs;
    }

    public int getRequiredKeywordForArityCount() {
        return requiredKwargs > 0 ? 1 : 0;
    }

    public boolean restKwargs() {
        return restKwargs;
    }

    public int pre() { return pre; }
    public int opt() { return opt; }
    public Rest rest() { return rest; }
    public int post() { return post; }
    public boolean hasKwargs() { return kwargs > 0 || restKwargs; }
    public boolean hasRest() { return rest != Rest.NONE; }

    /**
     * Are there an exact (fixed) number of parameters to this signature?
     */
    public boolean isFixed() {
        return arityValue() >= 0;
    }

    public int required() { return pre + post; }

    /**
     * Best attempt at breaking the code of arity values!  We figure out how many fixed/required parameters
     * must be supplied.  Then we figure out if we need to mark the value as optional.  Optional is indicated
     * by multiplying -1 * (fixed + 1).  Keyword args optional and rest values can indicate this optional
     * condition but only if no required keyword arguments are present.
     */
    public int arityValue() {
        int oneForKeywords = requiredKwargs > 0 ? 1 : 0;
        int fixedValue = pre() + post() + oneForKeywords;
        boolean hasOptionalKeywords = kwargs - requiredKwargs > 0;

        if (opt() > 0 || rest() != Rest.NONE || (hasOptionalKeywords || restKwargs()) && oneForKeywords == 0) {
            return -1 * (fixedValue + 1);
        }

        return fixedValue;
    }

    public static Signature from(int pre, int opt, int post, int kwargs, int requiredKwargs, Rest rest, boolean restKwargs) {
        if (opt == 0 && post == 0 && kwargs == 0 && !restKwargs) {
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

    public static Signature from(ArgsParseNode args) {
        ArgumentParseNode restArg = args.getRestArgNode();
        Rest rest = restArg != null ? restFromArg(restArg) : Rest.NONE;

        return Signature.from(args.getPreCount(), args.getOptionalArgsCount(), args.getPostCount(),
                args.getKeywordCount(), args.getRequiredKeywordCount(),rest,args.hasKeyRest());
    }

    public static Signature from(IterParseNode iter) {
        if (iter instanceof ForParseNode) return from((ForParseNode)iter);
        if (iter instanceof PreExeParseNode) return from((PreExeParseNode)iter);
        if (iter instanceof PostExeParseNode) return from((PostExeParseNode)iter);

        return from((ArgsParseNode) iter.getVarNode());
    }

    private static Rest restFromArg(ParseNode restArg) {
        Rest rest;
        if (restArg instanceof UnnamedRestArgParseNode) {
            UnnamedRestArgParseNode anonRest = (UnnamedRestArgParseNode) restArg;
            if (anonRest.isStar()) {
                rest = Rest.STAR;
            } else {
                rest = Rest.ANON;
            }
        } else if (restArg instanceof StarParseNode) {
            rest = Rest.STAR;
        } else {
            rest = Rest.NORM;
        }
        return rest;
    }

    public static Signature from(ForParseNode iter) {
        ParseNode var = iter.getVarNode();

        // ForParseNode can aggregate either a single node (required = 1) or masgn
        if (var instanceof MultipleAsgnParseNode) {
            MultipleAsgnParseNode masgn = (MultipleAsgnParseNode)var;

            Rest rest = Rest.NONE;
            if (masgn.getRest() != null) {
                ParseNode restArg = masgn.getRest();
                rest = restFromArg(restArg);
            }
            return Signature.from(masgn.getPreCount(), 0, masgn.getPostCount(), 0, 0, rest, false);
        }
        return Signature.ONE_ARGUMENT;
    }

    public static Signature from(PreExeParseNode iter) {
        return Signature.NO_ARGUMENTS;
    }

    public static Signature from(PostExeParseNode iter) {
        return Signature.NO_ARGUMENTS;
    }

    private static final int MAX_ENCODED_ARGS_EXPONENT = 8;
    private static final int MAX_ENCODED_ARGS_MASK = 0xFF;
    private static final int ENCODE_RESTKWARGS_SHIFT = 0;
    private static final int ENCODE_REST_SHIFT = ENCODE_RESTKWARGS_SHIFT + 1;
    private static final int ENCODE_REQKWARGS_SHIFT = ENCODE_REST_SHIFT + MAX_ENCODED_ARGS_EXPONENT;
    private static final int ENCODE_KWARGS_SHIFT = ENCODE_REQKWARGS_SHIFT + MAX_ENCODED_ARGS_EXPONENT;
    private static final int ENCODE_POST_SHIFT = ENCODE_KWARGS_SHIFT + MAX_ENCODED_ARGS_EXPONENT;
    private static final int ENCODE_OPT_SHIFT = ENCODE_POST_SHIFT + MAX_ENCODED_ARGS_EXPONENT;
    private static final int ENCODE_PRE_SHIFT = ENCODE_OPT_SHIFT + MAX_ENCODED_ARGS_EXPONENT;

    public long encode() {
        return
                ((long)pre << ENCODE_PRE_SHIFT) |
                        ((long)opt << ENCODE_OPT_SHIFT) |
                        ((long)post << ENCODE_POST_SHIFT) |
                        ((long)kwargs << ENCODE_KWARGS_SHIFT) |
                        ((long)requiredKwargs << ENCODE_REQKWARGS_SHIFT) |
                        (rest.ordinal() << ENCODE_REST_SHIFT) |
                        ((restKwargs?1:0) << ENCODE_RESTKWARGS_SHIFT);
    }

    public static Signature decode(long l) {
        return Signature.from(
                (int)(l >> ENCODE_PRE_SHIFT) & MAX_ENCODED_ARGS_MASK,
                (int)(l >> ENCODE_OPT_SHIFT) & MAX_ENCODED_ARGS_MASK,
                (int)(l >> ENCODE_POST_SHIFT) & MAX_ENCODED_ARGS_MASK,
                (int)(l >> ENCODE_KWARGS_SHIFT) & MAX_ENCODED_ARGS_MASK,
                (int)(l >> ENCODE_REQKWARGS_SHIFT) & MAX_ENCODED_ARGS_MASK,
                Rest.fromOrdinal((int)((l >> ENCODE_REST_SHIFT) & MAX_ENCODED_ARGS_MASK)),
                ((int)(l >> ENCODE_RESTKWARGS_SHIFT) & 0x1)==1 ? true : false

        );
    }

    @Override
    public String toString() {
        return "signature(pre=" + pre + ",opt=" + opt + ",post=" + post + ",rest=" + rest + ",kwargs=" + kwargs + ",kwreq=" + requiredKwargs + ",kwrest=" + restKwargs + ")";
    }
}
