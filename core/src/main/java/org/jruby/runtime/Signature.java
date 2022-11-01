package org.jruby.runtime;

import org.jruby.Ruby;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ArgumentNode;
import org.jruby.ast.ForNode;
import org.jruby.ast.IterNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.Node;
import org.jruby.ast.PostExeNode;
import org.jruby.ast.PreExeNode;
import org.jruby.ast.StarNode;
import org.jruby.ast.UnnamedRestArgNode;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.TypeConverter;

import static org.jruby.runtime.Arity.UNLIMITED_ARGUMENTS;

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

    public static final Signature NO_ARGUMENTS = new Signature(0, 0, 0, Rest.NONE, 0, 0, -1);
    public static final Signature ONE_ARGUMENT = new Signature(1, 0, 0, Rest.NONE, 0, 0, -1);
    public static final Signature TWO_ARGUMENTS = new Signature(2, 0, 0, Rest.NONE, 0, 0, -1);
    public static final Signature THREE_ARGUMENTS = new Signature(3, 0, 0, Rest.NONE, 0, 0, -1);
    public static final Signature FOUR_ARGUMENTS = new Signature(4, 0, 0, Rest.NONE, 0, 0, -1);
    public static final Signature OPTIONAL = new Signature(0, 0, 0, Rest.NORM, 0, 0, -1);
    public static final Signature ONE_REQUIRED = new Signature(1, 0, 0, Rest.NORM, 0, 0, -1);
    public static final Signature TWO_REQUIRED = new Signature(2, 0, 0, Rest.NORM, 0, 0, -1);
    public static final Signature THREE_REQUIRED = new Signature(3, 0, 0, Rest.NORM, 0, 0, -1);
    public static final Signature FOUR_REQUIRED = new Signature(4, 0, 0, Rest.NORM, 0, 0, -1);
    public static final Signature ONE_OPT_ARGUMENT = new Signature(0, 1, 0, Rest.NONE, 0, 0, -1);

    private final short pre;
    private final short opt;
    private final Rest rest;
    private final short post;
    private final short kwargs;
    private final short requiredKwargs;
    private final int arityValue;
    private final int keyRest;

    public Signature(int pre, int opt, int post, Rest rest, int kwargs, int requiredKwargs, int keyRest) {
        this.pre = (short) pre;
        this.opt = (short) opt;
        this.post = (short) post;
        this.rest = rest;
        this.kwargs = (short) kwargs;
        this.requiredKwargs = (short) requiredKwargs;
        this.keyRest = keyRest;
        this.arityValue = calculateArityValue();
    }

    public int getRequiredKeywordForArityCount() {
        return requiredKwargs > 0 ? 1 : 0;
    }

    public boolean restKwargs() {
        return keyRest != -1;
    }

    public int pre() { return pre; }
    public int opt() { return opt; }
    public Rest rest() { return rest; }
    public int post() { return post; }
    public boolean hasKwargs() { return kwargs > 0 || restKwargs(); }
    public boolean hasRest() { return rest != Rest.NONE; }
    public int keyRest() { return keyRest; }

    /**
     * Are there an exact (fixed) number of parameters to this signature?
     */
    public boolean isFixed() {
        return arityValue() >= 0;
    }

    /**
     * Is this a signature with a no arguments of any kind?
     */
    public boolean isNoArguments() {
        return isFixed() && required() == 0;
    }

    /**
     * Is this a signature with a single fixed argument and NO keyword arguments?
     */
    public boolean isOneArgument() {
        return isFixed() && required() == 1;
    }

    /**
     * Is this a signature with a two fixed arguments and NO keyword arguments?
     */
    public boolean isTwoArguments() {
        return isFixed() && required() == 2;
    }

    public int required() { return pre + post; }

    // We calculate this every time but no one should be using this any more
    @Deprecated
    public Arity arity() {
        // NOTE: Some logic to *assign* variables still uses Arity, which treats Rest.ANON (the
        //       |a,| form) as a rest arg for destructuring purposes. However ANON does *not*
        //       permit more than required args to be passed to a lambda, so we do not consider
        //       it a "true" rest arg for arity-checking purposes below in checkArity.
        if (rest != Rest.NONE || opt != 0) {
            return Arity.createArity(-(required() + 1));
        } else {
            return Arity.fixed(required() + getRequiredKeywordForArityCount());
        }
    }

    /**
     * Best attempt at breaking the code of arity values!  We figure out how many fixed/required parameters
     * must be supplied. Then we figure out if we need to mark the value as optional. Optional is indicated
     * by multiplying -1 * (fixed + 1). Keyword args optional and rest values can indicate this optional
     * condition but only if no required keyword arguments are present.
     */
    public int calculateArityValue() {
        int oneForKeywords = requiredKwargs > 0 ? 1 : 0;
        int fixedValue = pre() + post() + oneForKeywords;
        boolean hasOptionalKeywords = kwargs - requiredKwargs > 0;

        if (opt() > 0 || rest() != Rest.NONE || (hasOptionalKeywords || restKwargs()) && oneForKeywords == 0) {
            return -1 * (fixedValue + 1);
        }

        return fixedValue;
    }

    public int arityValue() {
        return arityValue;
    }

    /**
     * If we are yield'ing to this signature should we spread/destructure a Ruby Array?
     *
     * @return true if the signature expects multiple args
     */
    public boolean isSpreadable() {
        return arityValue < -1 || arityValue > 1;
    }


    // Lossy conversion to support populator constructors
    public static Signature fromArityValue(int arityValue) {
        boolean negative = arityValue < 0;
        int value = negative ? -1 * arityValue - 1 : arityValue;

        switch(value) {
            case 0:
                return negative ? Signature.OPTIONAL : Signature.NO_ARGUMENTS;
            case 1:
                return negative ? Signature.ONE_REQUIRED : Signature.ONE_ARGUMENT;
            case 2:
                return negative ? Signature.TWO_REQUIRED : Signature.TWO_ARGUMENTS;
            case 3:
                return negative ? Signature.THREE_REQUIRED : Signature.THREE_ARGUMENTS;
        }

        throw new UnsupportedOperationException("We do not know enough about the arity to convert it to a signature");
    }

    // Lossy conversion to half-support older signatures which externally use Arity but needs to be converted.
    @Deprecated
    public static Signature from(Arity arity) {
        switch(arity.required()) {
            case 0:
                return arity.isFixed() ? Signature.NO_ARGUMENTS : Signature.OPTIONAL;
            case 1:
                return arity.isFixed() ? Signature.ONE_ARGUMENT : Signature.ONE_REQUIRED;
            case 2:
                return arity.isFixed() ? Signature.TWO_ARGUMENTS : Signature.TWO_REQUIRED;
            case 3:
                return arity.isFixed() ? Signature.THREE_ARGUMENTS : Signature.THREE_REQUIRED;
        }

        throw new UnsupportedOperationException("We do not know enough about the arity to convert it to a signature");
    }

    public static Signature from(int pre, int opt, int post, int kwargs, int requiredKwargs, Rest rest, int keyRest) {
        if (opt == 0 && post == 0 && kwargs == 0 && keyRest == -1) {
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
                case 4:
                    switch (rest) {
                        case NONE:
                            return Signature.FOUR_ARGUMENTS;
                        case NORM:
                            return Signature.FOUR_REQUIRED;
                    }
                    break;
            }
        } else if (opt == 1 && pre == 0 && rest == Rest.NONE && post == 0 && kwargs == 0 && keyRest == -1) {
            return Signature.ONE_OPT_ARGUMENT;
        }
        return new Signature(pre, opt, post, rest, kwargs, requiredKwargs, keyRest);
    }

    public static Signature from(ArgsNode args) {
        ArgumentNode restArg = args.getRestArgNode();
        Rest rest = restArg != null ? restFromArg(restArg) : Rest.NONE;

        return Signature.from(args.getPreCount(), args.getOptionalArgsCount(), args.getPostCount(),
                args.getKeywordCount(), args.getRequiredKeywordCount(),rest,args.hasKeyRest() ? args.getKeyRest().getIndex() : -1);
    }

    public static Signature from(IterNode iter) {
        if (iter instanceof ForNode) return from((ForNode)iter);
        if (iter instanceof PreExeNode) return from((PreExeNode)iter);
        if (iter instanceof PostExeNode) return from((PostExeNode)iter);

        return from((ArgsNode) iter.getVarNode());
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
        if (var instanceof MultipleAsgnNode) {
            MultipleAsgnNode masgn = (MultipleAsgnNode)var;
            Rest rest = masgn.getRest() == null ? Rest.NONE : restFromArg(masgn.getRest());

            // 'for' can only have rest and pre args (no post or opt).  If var is a masgn then it is either
            // n > 1 args, it includes rest, or it is a special case (for th, in @bleh).  In this last case,
            // we need to increase the arg count by one so our iter arg passing code will destructure the
            // incoming args array and not just pass it through as a single value.
            int argCount = masgn.getPreCount();
            if (rest == Rest.NONE && argCount == 1) argCount = 2;
            return Signature.from(argCount, 0, 0, 0, 0, rest, -1);
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
    private static final int ENCODE_REST_SHIFT = ENCODE_RESTKWARGS_SHIFT + MAX_ENCODED_ARGS_EXPONENT;
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
                        (keyRest & 0xFF) << ENCODE_RESTKWARGS_SHIFT;
    }

    public static Signature decode(long l) {
        return Signature.from(
                (int)(l >>> ENCODE_PRE_SHIFT) & MAX_ENCODED_ARGS_MASK,
                (int)(l >>> ENCODE_OPT_SHIFT) & MAX_ENCODED_ARGS_MASK,
                (int)(l >>> ENCODE_POST_SHIFT) & MAX_ENCODED_ARGS_MASK,
                (int)(l >>> ENCODE_KWARGS_SHIFT) & MAX_ENCODED_ARGS_MASK,
                (int)(l >>> ENCODE_REQKWARGS_SHIFT) & MAX_ENCODED_ARGS_MASK,
                Rest.fromOrdinal((int)((l >>> ENCODE_REST_SHIFT) & MAX_ENCODED_ARGS_MASK)),
                (byte)((l >>> ENCODE_RESTKWARGS_SHIFT) & MAX_ENCODED_ARGS_MASK)

        );
    }

    public String toString() {
        return "signature(pre=" + pre + ",opt=" + opt + ",post=" + post + ",rest=" + rest + ",kwargs=" + kwargs + ",kwreq=" + requiredKwargs + ",kwrest=" + keyRest + ")";
    }

    public void checkArity(Ruby runtime, IRubyObject[] args) {
        if (args.length < required()) {
            throw runtime.newArgumentError(args.length, required(), hasRest() ? UNLIMITED_ARGUMENTS : (required() + opt));
        }
        if (rest == Rest.NONE || rest == Rest.ANON) {
            // no rest, so we have a maximum
            if (args.length > required() + opt()) {
                if (hasKwargs() && !TypeConverter.checkHashType(runtime, args[args.length - 1]).isNil()) {
                    // we have kwargs and a potential kwargs hash, check with length - 1
                    if (args.length - 1 > required() + opt()) {
                        throw runtime.newArgumentError(args.length, required(), hasRest() ? UNLIMITED_ARGUMENTS : (required() + opt));
                    }
                } else {
                    throw runtime.newArgumentError(args.length, required(), hasRest() ? UNLIMITED_ARGUMENTS : (required() + opt));
                }
            }
        }
    }

    public boolean equals(Object other) {
        if (!(other instanceof Signature)) return false;

        Signature otherSig = (Signature) other;

        return pre == otherSig.pre &&
                opt == otherSig.opt &&
                post == otherSig.post &&
                rest == otherSig.rest &&
                kwargs == otherSig.kwargs &&
                requiredKwargs == otherSig.requiredKwargs &&
                keyRest == otherSig.keyRest;
    }
}
