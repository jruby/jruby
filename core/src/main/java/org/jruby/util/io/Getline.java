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

/**
 * Encapsulation of the prepare_getline_args logic from MRI, used by StringIO and IO.
 */
public class Getline {
    public interface Callback<Self, Return extends IRubyObject> {
        Return getline(ThreadContext context, Self self, IRubyObject rs, int limit, boolean chomp, Block block);
    }

    public static <Self, Return extends IRubyObject> Return getlineCall(ThreadContext context, Callback<Self, Return> getline, Self self, Encoding enc_io) {
        return getlineCall(context, getline, self, enc_io, 0, null, null, null, Block.NULL_BLOCK);
    }

    public static <Self, Return extends IRubyObject> Return getlineCall(ThreadContext context, Callback<Self, Return> getline, Self self, Encoding enc_io, IRubyObject arg0) {
        return getlineCall(context, getline, self, enc_io, 1, arg0, null, null, Block.NULL_BLOCK);
    }

    public static <Self, Return extends IRubyObject> Return getlineCall(ThreadContext context, Callback<Self, Return> getline, Self self, Encoding enc_io, IRubyObject arg0, IRubyObject arg1) {
        return getlineCall(context, getline, self, enc_io, 2, arg0, arg1, null, Block.NULL_BLOCK);
    }

    public static <Self, Return extends IRubyObject> Return getlineCall(ThreadContext context, Callback<Self, Return> getline, Self self, Encoding enc_io, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return getlineCall(context, getline, self, enc_io, 3, arg0, arg1, arg2, Block.NULL_BLOCK);
    }

    public static <Self, Return extends IRubyObject> Return getlineCall(ThreadContext context, Callback<Self, Return> getline, Self self, Encoding enc_io, Block block) {
        return getlineCall(context, getline, self, enc_io, 0, null, null, null, block);
    }

    public static <Self, Return extends IRubyObject> Return getlineCall(ThreadContext context, Callback<Self, Return> getline, Self self, Encoding enc_io, IRubyObject arg0, Block block) {
        return getlineCall(context, getline, self, enc_io, 1, arg0, null, null, block);
    }

    public static <Self, Return extends IRubyObject> Return getlineCall(ThreadContext context, Callback<Self, Return> getline, Self self, Encoding enc_io, IRubyObject arg0, IRubyObject arg1, Block block) {
        return getlineCall(context, getline, self, enc_io, 2, arg0, arg1, null, block);
    }

    public static <Self, Return extends IRubyObject> Return getlineCall(ThreadContext context, Callback<Self, Return> getline, Self self, Encoding enc_io, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return getlineCall(context, getline, self, enc_io, 3, arg0, arg1, arg2, block);
    }

    public static <Self, Return extends IRubyObject> Return getlineCall(ThreadContext context, Callback<Self, Return> getline, Self self, Encoding enc_io, int argc, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
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

        final Ruby runtime = context.runtime;

        opt = ArgsUtil.getOptionsArg(runtime, optArg);

        if (opt == nil) {
            if (argc == 1) {
                sepArg = arg0;
            } else if (argc == 2) {
                limArg = arg1;
            }
        } else {
            IRubyObject chompKwarg = ArgsUtil.extractKeywordArg(context, "chomp", (RubyHash) opt);
            if (chompKwarg != null) {
                chomp = chompKwarg.isTrue();
            }
        }

        IRubyObject rs = runtime.getRecordSeparatorVar().get();
        IRubyObject lim = nil;

        if (sepArg != null && limArg == null) { // argc == 1
            IRubyObject tmp = nil;
            if (sepArg == nil || (tmp = TypeConverter.checkStringType(runtime, sepArg)) != nil) {
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
                if (rs == runtime.getGlobalVariables().getDefaultSeparator()) {
                    rs = RubyString.newStringLight(runtime, 2, enc_io).cat('\n', enc_io);
                }
                else {
                    throw runtime.newArgumentError("encoding mismatch: " + enc_io + " IO with " + enc_rs + " RS");
                }
            }
        }

        limit = lim == nil ? -1 : lim.convertToInteger().getLongValue();

        return getline.getline(context, self, rs, (int) limit, chomp, block);
    }

}
