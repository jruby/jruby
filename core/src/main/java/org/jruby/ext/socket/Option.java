package org.jruby.ext.socket;

import jnr.constants.platform.Errno;
import jnr.constants.platform.ProtocolFamily;
import jnr.constants.platform.Sock;
import jnr.constants.platform.SocketLevel;
import jnr.constants.platform.SocketOption;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.Pack;
import org.jruby.util.Sprintf;
import org.jruby.util.TypeConverter;

import java.nio.ByteBuffer;
import java.util.Locale;

public class Option extends RubyObject {
    public static void createOption(Ruby runtime) {
        RubyClass addrinfo = runtime.getClass("Socket").defineClassUnder(
                "Option",
                runtime.getObject(),
                Option::new);

        addrinfo.defineAnnotatedMethods(Option.class);
    }

    public Option(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }

    public Option(Ruby runtime, ProtocolFamily family, SocketLevel level, SocketOption option, ByteList data) {
        this(runtime, (RubyClass)runtime.getClassFromPath("Socket::Option"), family, level, option, data);
    }

    public Option(Ruby runtime, RubyClass klass, ProtocolFamily family, SocketLevel level, SocketOption option, ByteList data) {
        super(runtime, klass);

        this.family = family;
        this.level = level;
        this.option = option;
        this.data = data;
    }

    @JRubyMethod(required = 4, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        family = SocketUtils.protocolFamilyFromArg(args[0]);
        level = SocketUtils.levelFromArg(args[1]);
        option = SocketUtils.optionFromArg(args[2]);
        data = args[3].convertToString().getByteList();

        return this;
    }

    @JRubyMethod
    public IRubyObject family(ThreadContext context) {
        return context.runtime.newFixnum(family.longValue());
    }

    @JRubyMethod
    public IRubyObject level(ThreadContext context) {
        return context.runtime.newFixnum(level.longValue());
    }

    @JRubyMethod
    public IRubyObject optname(ThreadContext context) {
        return context.runtime.newFixnum(option.longValue());
    }

    @JRubyMethod
    public IRubyObject data(ThreadContext context) {
        return RubyString.newString(context.runtime, data).freeze(context);
    }

    // rb_sockopt_inspect
    @JRubyMethod
    public IRubyObject inspect(ThreadContext context) {
        StringBuilder buf = new StringBuilder(32);
        buf.append("#<");

        buf
            .append(metaClass.getRealClass().getName())
            .append(": ")
            .append(noPrefix(family));

        if (level == SocketLevel.SOL_SOCKET) {
            buf
                .append(" SOCKET ")
                .append(noPrefix(option));
        } else if (family == ProtocolFamily.PF_UNIX) {
            buf
                .append(" level:")
                .append(level.longValue())
                .append(' ')
                .append(noPrefix(option));
        } else {
            buf
                .append(" level:")
                .append(level.description())
                .append(' ')
                .append(noPrefix(option));
        }

        buf
            .append(' ')
            .append(optionValue())
            .append('>');

        return context.runtime.newString(buf.toString());
    }

    private String noPrefix(ProtocolFamily family) {
        return family.description().substring("PF_".length());
    }

    private String noPrefix(SocketOption option) {
        return option.description().substring("SO_".length());
    }

    // from rb_sockopt_inspect
    private String optionValue() {
        switch (option) {
            case SO_DEBUG:
            case SO_ACCEPTCONN:
            case SO_BROADCAST:
            case SO_REUSEADDR:
            case SO_KEEPALIVE:
            case SO_OOBINLINE:
            case SO_SNDBUF:
            case SO_RCVBUF:
            case SO_DONTROUTE:
            case SO_RCVLOWAT:
            case SO_SNDLOWAT:
                return String.valueOf(unpackInt(data));

            case SO_LINGER:
                int[] linger = Option.unpackLinger(data);

                return ((linger[0] == 0) ? "off " : "on ")  + linger[1] + "sec";

            case SO_RCVTIMEO:
            case SO_SNDTIMEO:
                return Sprintf.getNumberFormat(Locale.getDefault()).format(unpackInt(data) / 1000.0);

            case SO_ERROR:
                return Errno.valueOf(unpackInt(data)).description();

            case SO_TYPE:
                return Sock.valueOf(unpackInt(data)).description();
        }

        return "";
    }

    public static ByteList packInt(int i) {
        ByteList result = new ByteList(4);
        Pack.packInt_i(result, i);
        return result;
    }

    public static ByteList packLinger(int vonoff, int vsecs) {
        ByteList result = new ByteList(8);
        Pack.packInt_i(result, vonoff);
        Pack.packInt_i(result, vsecs);
        return result;
    }

    public static int unpackInt(ByteList data) {
        return Pack.unpackInt_i(ByteBuffer.wrap(data.unsafeBytes(), data.begin(), data.realSize()));
    }

    public static int[] unpackLinger(ByteList data) {
        ByteList result = new ByteList(8);
        ByteBuffer buf = ByteBuffer.wrap(data.unsafeBytes(), data.begin(), data.realSize());
        int vonoff = Pack.unpackInt_i(buf);
        int vsecs = Pack.unpackInt_i(buf);
        return new int[] {vonoff, vsecs};
    }

    @JRubyMethod(name = "int", required = 4, meta = true)
    public static IRubyObject rb_int(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        ProtocolFamily family = SocketUtils.protocolFamilyFromArg(args[0]);
        SocketLevel level = SocketUtils.levelFromArg(args[1]);
        SocketOption option = SocketUtils.optionFromArg(args[2]);
        ByteList data = packInt(RubyNumeric.fix2int(args[3]));

        return new Option(context.getRuntime(), family, level, option, data);
    }

    @JRubyMethod(name = "int")
    public IRubyObject asInt(ThreadContext context) {
        final Ruby runtime = context.getRuntime();

        validateDataSize(runtime, data, 4);

        return runtime.newFixnum(unpackInt(data));
    }

    @JRubyMethod(required = 4, meta = true)
    public static IRubyObject bool(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        ProtocolFamily family = SocketUtils.protocolFamilyFromArg(args[0]);
        SocketLevel level = SocketUtils.levelFromArg(args[1]);
        SocketOption option = SocketUtils.optionFromArg(args[2]);
        ByteList data = packInt(args[3].isTrue() ? 1 : 0);

        return new Option(context.getRuntime(), family, level, option, data);
    }

    @JRubyMethod
    public IRubyObject bool(ThreadContext context) {
        final Ruby runtime = context.runtime;

        validateDataSize(runtime, data, 4);

        return runtime.newBoolean(unpackInt(data) != 0);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject linger(ThreadContext context, IRubyObject self, IRubyObject vonoff, IRubyObject vsecs) {
        ProtocolFamily family = ProtocolFamily.PF_UNSPEC;
        SocketLevel level = SocketLevel.SOL_SOCKET;
        SocketOption option = SocketOption.SO_LINGER;
        int coercedVonoff;

        if (!TypeConverter.checkIntegerType(context, vonoff).isNil()) {
            coercedVonoff = vonoff.convertToInteger().getIntValue();
        } else {
            coercedVonoff = vonoff.isTrue() ? 1 : 0;
        }

        ByteList data = packLinger(coercedVonoff, vsecs.convertToInteger().getIntValue());

        return new Option(context.getRuntime(), family, level, option, data);
     }

    @JRubyMethod
    public IRubyObject linger(ThreadContext context) {
        final Ruby runtime = context.runtime;

        validateDataSize(runtime, data, 8);

        int[] linger = Option.unpackLinger(data);

        return runtime.newArray(runtime.newBoolean(linger[0] != 0), runtime.newFixnum(linger[1]));
    }

    @JRubyMethod
    public IRubyObject unpack(ThreadContext context, IRubyObject arg0) {
        return Pack.unpack(context.runtime, data, arg0.convertToString().getByteList());
    }

    @JRubyMethod
    public IRubyObject to_s(ThreadContext context) {
        return RubyString.newString(context.runtime, data);
    }

    private static void validateDataSize(Ruby runtime, ByteList data, int size) {
        int realSize = data.realSize();

        if (realSize != size) {
            throw runtime.newTypeError("size differ.  expected as sizeof(int)=" + size + " but " + realSize);
        }
    }

    private ProtocolFamily family;
    private SocketLevel level;
    private SocketOption option;
    private ByteList data;
}
