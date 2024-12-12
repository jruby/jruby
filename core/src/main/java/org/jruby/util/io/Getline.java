package org.jruby.util.io;

import org.jcodings.Encoding;
import org.jruby.Ruby;
import org.jruby.RubyHash;
import org.jruby.RubyString;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.StringSupport;
import org.jruby.util.TypeConverter;

import static org.jruby.api.Access.globalVariables;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.typeError;

/**
 * Encapsulation of the prepare_getline_args logic from MRI, used by StringIO and IO.
 */
public class Getline {
    public interface Callback<Self, Return extends IRubyObject> {
        Return getline(ThreadContext context, Self self, IRubyObject rs, int limit, boolean chomp, Block block);
    }

    public static <Self, Return extends IRubyObject> Return getlineCall(ThreadContext context, Callback<Self, Return> getline,
                                                                        Self self, Encoding enc_io) {
        return getlineCall(context, getline, self, enc_io, 0, null, null, null, Block.NULL_BLOCK, false);
    }

    // Work around native extensions calling without marking keywords annotation AND not calling newer non-deprecated methods.
    @Deprecated
    public static <Self, Return extends IRubyObject> Return getlineCall(ThreadContext context, Callback<Self, Return> getline,
                                                                        Self self, Encoding enc_io, IRubyObject arg0) {
        boolean keywords = arg0 instanceof RubyHash;
        return getlineCall(context, getline, self, enc_io, 1, arg0, null, null, Block.NULL_BLOCK, keywords);
    }
    public static <Self, Return extends IRubyObject> Return getlineCall(ThreadContext context, Callback<Self, Return> getline,
                                                                        Self self, Encoding enc_io, IRubyObject arg0, boolean keywords) {
        return getlineCall(context, getline, self, enc_io, 1, arg0, null, null, Block.NULL_BLOCK, keywords);
    }

    // Work around native extensions calling without marking keywords annotation AND not calling newer non-deprecated methods.
    @Deprecated
    public static <Self, Return extends IRubyObject> Return getlineCall(ThreadContext context, Callback<Self, Return> getline,
                                                                        Self self, Encoding enc_io, IRubyObject arg0, IRubyObject arg1) {
        boolean keywords = arg1 instanceof RubyHash;
        return getlineCall(context, getline, self, enc_io, 2, arg0, arg1, null, Block.NULL_BLOCK, keywords);
    }
    public static <Self, Return extends IRubyObject> Return getlineCall(ThreadContext context, Callback<Self, Return> getline,
                                                                        Self self, Encoding enc_io, IRubyObject arg0,
                                                                        IRubyObject arg1, boolean keywords) {
        return getlineCall(context, getline, self, enc_io, 2, arg0, arg1, null, Block.NULL_BLOCK, keywords);
    }

    // Work around native extensions calling without marking keywords annotation AND not calling newer non-deprecated methods.
    @Deprecated
    public static <Self, Return extends IRubyObject> Return getlineCall(ThreadContext context, Callback<Self, Return> getline,
                                                                        Self self, Encoding enc_io, IRubyObject arg0,
                                                                        IRubyObject arg1, IRubyObject arg2) {
        boolean keywords = arg2 instanceof RubyHash;
        return getlineCall(context, getline, self, enc_io, 3, arg0, arg1, arg2, Block.NULL_BLOCK, keywords);
    }

    public static <Self, Return extends IRubyObject> Return getlineCall(ThreadContext context, Callback<Self, Return> getline,
                                                                        Self self, Encoding enc_io, IRubyObject arg0,
                                                                        IRubyObject arg1, IRubyObject arg2, boolean keywords) {
        return getlineCall(context, getline, self, enc_io, 3, arg0, arg1, arg2, Block.NULL_BLOCK, keywords);
    }

    public static <Self, Return extends IRubyObject> Return getlineCall(ThreadContext context, Callback<Self, Return> getline,
                                                                        Self self, Encoding enc_io, Block block) {
        return getlineCall(context, getline, self, enc_io, 0, null, null, null, block);
    }

    // Work around native extensions calling without marking keywords annotation AND not calling newer non-deprecated methods.
    @Deprecated
    public static <Self, Return extends IRubyObject> Return getlineCall(ThreadContext context, Callback<Self, Return> getline,
                                                                        Self self, Encoding enc_io, IRubyObject arg0, Block block) {
        boolean keywords = arg0 instanceof RubyHash;
        return getlineCall(context, getline, self, enc_io, 1, arg0, null, null, block, keywords);
    }

    public static <Self, Return extends IRubyObject> Return getlineCall(ThreadContext context, Callback<Self, Return> getline,
                                                                        Self self, Encoding enc_io, IRubyObject arg0,
                                                                        Block block, boolean keywords) {
        return getlineCall(context, getline, self, enc_io, 1, arg0, null, null, block, keywords);
    }

    // Work around native extensions calling without marking keywords annotation AND not calling newer non-deprecated methods.
    @Deprecated
    public static <Self, Return extends IRubyObject> Return getlineCall(ThreadContext context, Callback<Self, Return> getline,
                                                                        Self self, Encoding enc_io, IRubyObject arg0,
                                                                        IRubyObject arg1, Block block) {
        boolean keywords = arg1 instanceof RubyHash;
        return getlineCall(context, getline, self, enc_io, 2, arg0, arg1, null, block, keywords);
    }

    public static <Self, Return extends IRubyObject> Return getlineCall(ThreadContext context, Callback<Self, Return> getline,
                                                                        Self self, Encoding enc_io, IRubyObject arg0,
                                                                        IRubyObject arg1, Block block, boolean keywords) {
        return getlineCall(context, getline, self, enc_io, 2, arg0, arg1, null, block, keywords);
    }

    // Work around native extensions calling without marking keywords annotation AND not calling newer non-deprecated methods.
    @Deprecated
    public static <Self, Return extends IRubyObject> Return getlineCall(ThreadContext context, Callback<Self, Return> getline,
                                                                        Self self, Encoding enc_io, IRubyObject arg0,
                                                                        IRubyObject arg1, IRubyObject arg2, Block block) {
        boolean keywords = arg2 instanceof RubyHash;
        return getlineCall(context, getline, self, enc_io, 3, arg0, arg1, arg2, block, keywords);
    }

    public static <Self, Return extends IRubyObject> Return getlineCall(ThreadContext context, Callback<Self, Return> getline,
                                                                        Self self, Encoding enc_io, IRubyObject arg0,
                                                                        IRubyObject arg1, IRubyObject arg2, Block block,
                                                                        boolean keywords) {
        return getlineCall(context, getline, self, enc_io, 3, arg0, arg1, arg2, block, keywords);
    }

    @Deprecated
    public static <Self, Return extends IRubyObject> Return getlineCall(ThreadContext context, Callback<Self, Return> getline,
                                                                        Self self, Encoding enc_io, IRubyObject... args) {
        return switch (args.length) {
            case 0 -> getlineCall(context, getline, self, enc_io, false);
            case 1 -> getlineCall(context, getline, self, enc_io, false, args[0]);
            case 2 -> getlineCall(context, getline, self, enc_io, false, args[0], args[1]);
            case 3 -> getlineCall(context, getline, self, enc_io, false, args[0], args[1], args[2]);
            default -> throw argumentError(context, args.length, 0, 3);
        };
    }

    public static <Self, Return extends IRubyObject> Return getlineCall(ThreadContext context, Callback<Self, Return> getline,
                                                                        Self self, Encoding enc_io, boolean keywords, IRubyObject... args) {
        return switch (args.length) {
            case 0 -> getlineCall(context, getline, self, enc_io, keywords);
            case 1 -> getlineCall(context, getline, self, enc_io, args[0], keywords);
            case 2 -> getlineCall(context, getline, self, enc_io, args[0], args[1], keywords);
            case 3 -> getlineCall(context, getline, self, enc_io, args[0], args[1], args[2], keywords);
            default -> throw argumentError(context, args.length, 0, 3);
        };
    }

    // Work around native extensions calling without marking keywords annotation AND not calling newer non-deprecated methods.
    // Note: some callers use this as the single entry point vs calling into the specific overload so we need to do extra null
    //   checking to figure out actual last valid passed argument.
    @Deprecated
    public static <Self, Return extends IRubyObject> Return getlineCall(ThreadContext context, Callback<Self, Return> getline,
                                                                        Self self, Encoding enc_io, int argc, IRubyObject arg0,
                                                                        IRubyObject arg1, IRubyObject arg2, Block block) {
        IRubyObject lastArg = arg2 == null ? (arg1 == null ? (arg0 == null ? null : arg0) : arg1) : arg2;
        boolean keywords = lastArg != null && lastArg instanceof RubyHash;

        return getlineCall(context, getline, self, enc_io, argc, arg0, arg1, arg2, block, keywords);
    }

    public static <Self, Return extends IRubyObject> Return getlineCall(ThreadContext context, Callback<Self, Return> getline,
                                                                        Self self, Encoding enc_io, int argc, IRubyObject arg0,
                                                                        IRubyObject arg1, IRubyObject arg2, Block block, boolean keywords) {
        final IRubyObject nil = context.nil;

        boolean chomp = false;
        long limit;
        IRubyObject opt, optArg = nil, sepArg = null, limArg = null;

        switch (argc) {
            case 1:
                optArg = arg0;
                break;
            case 2:
                sepArg = arg0;
                optArg = arg1;
                break;
            case 3:
                sepArg = arg0;
                limArg = arg1;
                optArg = arg2;
                break;
        }

        if (optArg instanceof RubyHash && !keywords) {
            // We get args from multiple sources so we are form-fitting this as if we are processing it from
            // the original method.  We should not be doing this processing this deep into this IO processing.
            if (argc == 3) throw argumentError(context, argc, 0, 2);

            throw typeError(context, "no implicit conversion of Hash into Integer");
        }
        opt = ArgsUtil.getOptionsArg(context, optArg);

        if (opt == nil) {
            if (argc == 1) {
                sepArg = arg0;
            } else if (argc == 2) {
                limArg = arg1;
            }
        } else {
            IRubyObject chompKwarg = ArgsUtil.extractKeywordArg(context, "chomp", (RubyHash) opt);
            if (chompKwarg != null && (sepArg == null || !sepArg.isNil())) {
                chomp = chompKwarg.isTrue();
            }
        }

        IRubyObject rs = context.runtime.getRecordSeparatorVar().get();
        IRubyObject lim = nil;

        if (sepArg != null && limArg == null) { // argc == 1
            IRubyObject tmp = nil;
            if (sepArg == nil || (tmp = TypeConverter.checkStringType(context.runtime, sepArg)) != nil) {
                rs = tmp;
            } else {
                lim = sepArg;
            }
        } else if (sepArg != null && limArg != null) { // argc >= 2
            rs = sepArg;
            if (rs != nil) {
                rs = rs.convertToString();
            }
            lim = limArg;
        }

        // properly encode rs
        if (rs != nil) {
            final RubyString rs_s = ((RubyString) rs);
            final Encoding enc_rs = rs_s.getEncoding();
            if (enc_io != enc_rs &&
                    (rs_s.scanForCodeRange() != StringSupport.CR_7BIT ||
                            (rs_s.size() > 0 && !enc_io.isAsciiCompatible()))) {
                if (rs == globalVariables(context).getDefaultSeparator()) {
                    rs = RubyString.newStringLight(context.runtime, 2, enc_io).cat('\n', enc_io);
                } else {
                    throw argumentError(context, "encoding mismatch: " + enc_io + " IO with " + enc_rs + " RS");
                }
            }
        }

        limit = lim == nil ? -1 : lim.convertToInteger().getLongValue();

        return getline.getline(context, self, rs, (int) limit, chomp, block);
    }

}
