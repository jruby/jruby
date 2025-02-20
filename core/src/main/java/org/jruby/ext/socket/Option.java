package org.jruby.ext.socket;

import jnr.constants.platform.Errno;
import jnr.constants.platform.ProtocolFamily;
import jnr.constants.platform.Sock;
import jnr.constants.platform.SocketLevel;
import jnr.constants.platform.SocketOption;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyInteger;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.Pack;
import org.jruby.util.Sprintf;

import java.nio.ByteBuffer;
import java.util.Locale;

import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.checkToInteger;
import static org.jruby.api.Convert.toInt;
import static org.jruby.api.Create.newArray;
import static org.jruby.api.Create.newString;
import static org.jruby.api.Error.typeError;

public class Option extends RubyObject {
    public static void createOption(ThreadContext context, RubyClass Object, RubyClass Socket) {
        Socket.defineClassUnder(context, "Option", Object, Option::new).defineMethods(context, Option.class);
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
        family = SocketUtils.protocolFamilyFromArg(context, args[0]);
        level = SocketUtils.levelFromArg(context, args[1]);
        option = SocketUtils.optionFromArg(context, args[2]);
        data = args[3].convertToString().getByteList();

        return this;
    }

    @JRubyMethod
    public IRubyObject family(ThreadContext context) {
        return asFixnum(context, family.longValue());
    }

    @JRubyMethod
    public IRubyObject level(ThreadContext context) {
        return asFixnum(context, level.longValue());
    }

    @JRubyMethod
    public IRubyObject optname(ThreadContext context) {
        return asFixnum(context, option.longValue());
    }

    @JRubyMethod
    public IRubyObject data(ThreadContext context) {
        return newString(context, data).freeze(context);
    }

    // rb_sockopt_inspect
    @JRubyMethod
    public IRubyObject inspect(ThreadContext context) {
        StringBuilder buf = new StringBuilder(32);
        buf.append("#<");

        buf
            .append(metaClass.getRealClass().getName(context))
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

        return newString(context, buf.toString());
    }

    private String noPrefix(ProtocolFamily family) {
        return family.description().substring("PF_".length());
    }

    private String noPrefix(SocketOption option) {
        return option.description().substring("SO_".length());
    }

    // from rb_sockopt_inspect
    private String optionValue() {
        return switch (option) {
            case SO_DEBUG, SO_ACCEPTCONN, SO_BROADCAST, SO_REUSEADDR, SO_KEEPALIVE, SO_OOBINLINE, SO_SNDBUF, SO_RCVBUF,
                 SO_DONTROUTE, SO_RCVLOWAT, SO_SNDLOWAT -> String.valueOf(unpackInt(data));
            case SO_LINGER -> {
                int[] linger = Option.unpackLinger(data);

                yield ((linger[0] == 0) ? "off " : "on ") + linger[1] + "sec";
            }
            case SO_RCVTIMEO, SO_SNDTIMEO ->
                    Sprintf.getNumberFormat(Locale.getDefault()).format(unpackInt(data) / 1000.0);
            case SO_ERROR -> Errno.valueOf(unpackInt(data)).description();
            case SO_TYPE -> Sock.valueOf(unpackInt(data)).description();
            default -> "";
        };

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
        ByteBuffer buf = ByteBuffer.wrap(data.unsafeBytes(), data.begin(), data.realSize());
        int vonoff = Pack.unpackInt_i(buf);
        int vsecs = Pack.unpackInt_i(buf);
        return new int[] {vonoff, vsecs};
    }

    @JRubyMethod(name = "int", required = 4, meta = true)
    public static IRubyObject rb_int(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        ProtocolFamily family = SocketUtils.protocolFamilyFromArg(context, args[0]);
        SocketLevel level = SocketUtils.levelFromArg(context, args[1]);
        SocketOption option = SocketUtils.optionFromArg(context, args[2]);
        ByteList data = packInt(toInt(context, args[3]));

        return new Option(context.getRuntime(), family, level, option, data);
    }

    @JRubyMethod(name = "int")
    public IRubyObject asInt(ThreadContext context) {
        validateDataSize(context, data, 4);

        return asFixnum(context, unpackInt(data));
    }

    @JRubyMethod(required = 4, meta = true)
    public static IRubyObject bool(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        ProtocolFamily family = SocketUtils.protocolFamilyFromArg(context, args[0]);
        SocketLevel level = SocketUtils.levelFromArg(context, args[1]);
        SocketOption option = SocketUtils.optionFromArg(context, args[2]);
        ByteList data = packInt(args[3].isTrue() ? 1 : 0);

        return new Option(context.getRuntime(), family, level, option, data);
    }

    @JRubyMethod
    public IRubyObject bool(ThreadContext context) {
        validateDataSize(context, data, 4);

        return context.runtime.newBoolean(unpackInt(data) != 0);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject linger(ThreadContext context, IRubyObject self, IRubyObject vonoffArg, IRubyObject vsecs) {
        IRubyObject vonoff = checkToInteger(context, vonoffArg);
        int coercedVonoff = !vonoff.isNil() ? ((RubyInteger) vonoff).asInt(context) : (vonoffArg.isTrue() ? 1 : 0);
        ByteList data = packLinger(coercedVonoff, toInt(context, vsecs));

        return new Option(context.runtime, ProtocolFamily.PF_UNSPEC, SocketLevel.SOL_SOCKET, SocketOption.SO_LINGER, data);
     }

    @JRubyMethod
    public IRubyObject linger(ThreadContext context) {
        validateDataSize(context, data, 8);

        int[] linger = Option.unpackLinger(data);

        return newArray(context, asBoolean(context,linger[0] != 0), asFixnum(context, linger[1]));
    }

    @JRubyMethod
    public IRubyObject unpack(ThreadContext context, IRubyObject arg0) {
        return Pack.unpack(context, data, arg0.convertToString().getByteList());
    }

    @JRubyMethod
    public IRubyObject to_s(ThreadContext context) {
        return newString(context, data);
    }

    private static void validateDataSize(ThreadContext context, ByteList data, int size) {
        int realSize = data.realSize();

        if (realSize != size) throw typeError(context, "size differ.  expected as sizeof(int)=" + size + " but " + realSize);
    }

    private ProtocolFamily family;
    private SocketLevel level;
    private SocketOption option;
    private ByteList data;
}
