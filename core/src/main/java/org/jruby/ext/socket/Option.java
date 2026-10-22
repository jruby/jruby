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
        this(runtime, klass, family, level.intValue(), option.intValue(), data);
    }

    // WHY ints, not SocketLevel/SocketOption: option names are only unique
    // within a level (IP_TTL and SO_REUSEADDR are both 2), and most levels
    // (IP, IPV6, TCP, UDP, anything numeric) have no SocketOption at all.
    public Option(Ruby runtime, ProtocolFamily family, int level, int optname, ByteList data) {
        this(runtime, (RubyClass)runtime.getClassFromPath("Socket::Option"), family, level, optname, data);
    }

    public Option(Ruby runtime, RubyClass klass, ProtocolFamily family, int level, int optname, ByteList data) {
        super(runtime, klass);

        this.family = family;
        this.level = level;
        this.optname = optname;
        this.data = data;
    }

    @JRubyMethod(required = 4, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        family = SocketUtils.protocolFamilyFromArg(context, args[0]);
        level = SocketUtils.levelToInt(context, args[1]);
        optname = SocketUtils.optnameToInt(context, level, args[2]);
        data = args[3].convertToString().getByteList();

        return this;
    }

    @JRubyMethod
    public IRubyObject family(ThreadContext context) {
        return asFixnum(context, family.longValue());
    }

    @JRubyMethod
    public IRubyObject level(ThreadContext context) {
        return asFixnum(context, level);
    }

    @JRubyMethod
    public IRubyObject optname(ThreadContext context) {
        return asFixnum(context, optname);
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

        String prefix = SocketUtils.optnamePrefix(level);
        String levelName = levelName();
        String optName = prefix == null ? null : constantName(context, prefix, optname);

        if (levelName == null) {
            buf.append(" level:").append(level).append(" optname:").append(optname);
        } else {
            buf.append(' ').append(levelName).append(' ');
            if (optName == null) buf.append("optname:").append(optname);
            else buf.append(optName.substring(prefix.length()));
        }

        String value = optName == null ? null : optionValue(optName);
        buf
            .append(' ')
            .append(value != null ? value : newString(context, data).inspect(context).toString())
            .append('>');

        return newString(context, buf.toString());
    }

    private String noPrefix(ProtocolFamily family) {
        return family.description().substring("PF_".length());
    }

    private String levelName() {
        String prefix = SocketUtils.optnamePrefix(level);
        if (prefix == null) return null;
        return prefix.equals("SO_") ? "SOCKET" : prefix.substring(0, prefix.length() - 1);
    }

    /** Name of the Socket::Constants constant with this prefix and value, or null. */
    private static String constantName(ThreadContext context, String prefix, int value) {
        org.jruby.RubyModule constants = (org.jruby.RubyModule) context.runtime.getClassFromPath("Socket::Constants");
        for (String name : constants.getConstantMap().keySet()) {
            if (!name.startsWith(prefix)) continue;
            IRubyObject v = constants.getConstantNoConstMissing(name);
            if (v instanceof RubyInteger i && i.getLongValue() == value) return name;
        }
        return null;
    }

    // from rb_sockopt_inspect: options MRI formats as a value; anything else
    // (null here) is shown as the inspected raw data, as MRI does.
    private String optionValue(String name) {
        int size = data.realSize();
        switch (name) {
            case "SO_DEBUG", "SO_ACCEPTCONN", "SO_BROADCAST", "SO_REUSEADDR", "SO_REUSEPORT", "SO_KEEPALIVE",
                 "SO_OOBINLINE", "SO_SNDBUF", "SO_RCVBUF", "SO_DONTROUTE", "SO_RCVLOWAT", "SO_SNDLOWAT",
                 "SO_PASSCRED", "SO_PRIORITY",
                 "IPV6_V6ONLY", "IPV6_UNICAST_HOPS", "IPV6_MULTICAST_HOPS", "IPV6_MULTICAST_LOOP",
                 "IPV6_MULTICAST_IF", "IPV6_RECVPKTINFO",
                 "TCP_NODELAY", "TCP_MAXSEG", "TCP_CORK", "TCP_KEEPIDLE", "TCP_KEEPINTVL", "TCP_KEEPCNT",
                 "TCP_SYNCNT", "TCP_LINGER2", "TCP_DEFER_ACCEPT", "TCP_WINDOW_CLAMP", "TCP_QUICKACK",
                 "TCP_USER_TIMEOUT", "TCP_FASTOPEN":
                return size == 4 ? String.valueOf(unpackInt(data)) : null;
            case "SO_LINGER":
                if (size != 8) return null;
                int[] linger = Option.unpackLinger(data);
                return ((linger[0] == 0) ? "off " : "on ") + linger[1] + "sec";
            case "SO_RCVTIMEO", "SO_SNDTIMEO":
                return size == 4 ? Sprintf.getNumberFormat(Locale.getDefault()).format(unpackInt(data) / 1000.0) : null;
            case "SO_ERROR":
                return size == 4 ? Errno.valueOf(unpackInt(data)).description() : null;
            case "SO_TYPE":
                return size == 4 ? Sock.valueOf(unpackInt(data)).description() : null;
            default:
                return null;
        }
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
        int level = SocketUtils.levelToInt(context, args[1]);
        int option = SocketUtils.optnameToInt(context, level, args[2]);
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
        int level = SocketUtils.levelToInt(context, args[1]);
        int option = SocketUtils.optnameToInt(context, level, args[2]);
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
    private int level;
    private int optname;
    private ByteList data;
}
