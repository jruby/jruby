package org.jruby.ext.socket;

import jnr.constants.platform.AddressFamily;
import jnr.constants.platform.Errno;
import jnr.constants.platform.ProtocolFamily;
import jnr.constants.platform.Sock;
import jnr.constants.platform.SocketLevel;
import jnr.constants.platform.SocketOption;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.Visibility;
import org.jruby.util.ByteList;
import org.jruby.util.Pack;
import org.jruby.util.Sprintf;

import java.nio.ByteBuffer;
import java.text.NumberFormat;
import java.util.Locale;

public class Option extends RubyObject {
    public static void createOption(Ruby runtime) {
        RubyClass addrinfo = runtime.getClass("Socket").defineClassUnder(
                "Option",
                runtime.getObject(),
                new ObjectAllocator() {
                    public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                        return new Option(runtime, klazz);
                    }
                });

        addrinfo.defineAnnotatedMethods(Option.class);
    }

    public Option(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }

    public Option(Ruby runtime, ProtocolFamily family, SocketLevel level, SocketOption option, int data) {
        this(runtime, (RubyClass) runtime.getClassFromPath("Socket::Option"), family, level, option, data);
    }

    public Option(Ruby runtime, RubyClass klass, ProtocolFamily family, SocketLevel level, SocketOption option, int data) {
        super(runtime, klass);
        
        this.family = family;
        this.level = level;
        this.option = option;
        this.data = packInt(data);
    }
    
    @JRubyMethod(required = 4, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        family = SocketUtils.protocolFamilyFromArg(args[0]);
        level = SocketUtils.socketLevelFromArg(args[1]);
        option = SocketUtils.socketOptionFromArg(args[2]);
        data = args[3].convertToString().getByteList();
        return context.nil;
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
        return context.runtime.newFixnum(option.intValue());
    }

    @JRubyMethod
    public IRubyObject data(ThreadContext context) {
        return RubyString.newString(context.runtime, data).freeze(context);
    }

    // rb_sockopt_inspect
    @JRubyMethod
    public IRubyObject inspect(ThreadContext context) {
        StringBuffer buf = new StringBuffer("#<");

        buf
                .append(metaClass.getRealClass().getName())
                .append(" ")
                .append(noPrefix(family));

        if (level == SocketLevel.SOL_SOCKET) {
            buf
                    .append(" SOCKET ")
                    .append(noPrefix(option));
        } else if (family == ProtocolFamily.PF_UNIX) {
            buf
                    .append(" level:")
                    .append(level.longValue())
                    .append(" ")
                    .append(noPrefix(option));
        } else {
            buf
                    .append(" level:")
                    .append(level.description())
                    .append(" ")
                    .append(noPrefix(option));
        }

        buf
                .append(" ")
                .append(optionValue())
                .append(">");

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
        int intData;
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
                intData = Pack.unpackInt_i(ByteBuffer.wrap(data.unsafeBytes(), data.begin(), data.realSize()));
                return String.valueOf(intData);

            case SO_LINGER:
                intData = Pack.unpackInt_i(ByteBuffer.wrap(data.unsafeBytes(), data.begin(), data.realSize()));
                return intData == -1 ? "off" :
                        intData == 0 ? "on" :
                                "on(" + intData + ")";

            case SO_RCVTIMEO:
            case SO_SNDTIMEO:
                intData = Pack.unpackInt_i(ByteBuffer.wrap(data.unsafeBytes(), data.begin(), data.realSize()));
                return Sprintf.getNumberFormat(Locale.getDefault()).format(intData / 1000.0);

            case SO_ERROR:
                intData = Pack.unpackInt_i(ByteBuffer.wrap(data.unsafeBytes(), data.begin(), data.realSize()));
                return Errno.valueOf(intData).description();

            case SO_TYPE:
                intData = Pack.unpackInt_i(ByteBuffer.wrap(data.unsafeBytes(), data.begin(), data.realSize()));
                return Sock.valueOf(intData).description();
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

    public int[] unpackLinger() {
        ByteList result = new ByteList(8);
        ByteBuffer buf = ByteBuffer.wrap(data.unsafeBytes(), data.begin(), data.realSize());
        int vonoff = Pack.unpackInt_i(buf);
        int vsecs = Pack.unpackInt_i(buf);
        return new int[] {vonoff, vsecs};
    }

    @JRubyMethod(name = {"int", "byte"}, required = 4, meta = true)
    public static IRubyObject rb_int(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        Ruby runtime = context.runtime;

        Option option = new Option(runtime, (RubyClass) self);
        args[3] = runtime.newString(Pack.packInt_i(new ByteList(), args[3].convertToInteger().getIntValue()));
        option.initialize(context, args);

        return option;
    }

    @JRubyMethod(name = {"int", "byte"})
    public IRubyObject asInt(ThreadContext context) {
        Ruby runtime = context.runtime;

        if (data == null || data.realSize() != 4) {
            throw runtime.newTypeError("size differ.  expected as sizeof(int)=4 but " + data.realSize());
        }

        int intData = Pack.unpackInt_i(ByteBuffer.wrap(data.unsafeBytes(), data.begin(), data.realSize()));
        return runtime.newFixnum(intData);
    }

    @JRubyMethod(required = 4, meta = true)
    public static IRubyObject bool(ThreadContext context, IRubyObject self, IRubyObject args[]) {
        Ruby runtime = context.runtime;

        Option option = new Option(runtime, (RubyClass) self);
        args[3] = runtime.newString(packInt(args[3].isTrue() ? 1 : 0));
        option.initialize(context, args);

        return option;
    }

    @JRubyMethod
    public IRubyObject bool(ThreadContext context) {
        Ruby runtime = context.runtime;

        if (data.realSize() != 4) {
            throw runtime.newTypeError("size differ.  expected as sizeof(int)=4 but " + data.realSize());
        }

        int intData = Pack.unpackInt_i(ByteBuffer.wrap(data.unsafeBytes(), data.begin(), data.realSize()));
        return intData == 0 ? runtime.getFalse() : runtime.getTrue();
    }

    @JRubyMethod(meta = true)
    public static IRubyObject linger(ThreadContext context, IRubyObject self, IRubyObject vonoff, IRubyObject vsecs) {
        Ruby runtime = context.runtime;
        Option option = new Option(runtime, (RubyClass) self);
        option.initialize(
                context,
                new IRubyObject[]{
                        runtime.newFixnum(AddressFamily.AF_UNSPEC.intValue()),
                        runtime.newFixnum(SocketLevel.SOL_SOCKET.intValue()),
                        runtime.newFixnum(SocketOption.SO_LINGER.intValue()),
                        runtime.newString(packLinger(vonoff.isTrue() ? 1 : 0, vsecs.convertToInteger().getIntValue()))
                }
        );
        return option;
    }

    @JRubyMethod
    public IRubyObject linger(ThreadContext context) {
        Ruby runtime = context.runtime;

        if (data == null || data.realSize() != 8) {
            throw runtime.newTypeError("size differ.  expected as sizeof(int)=8 but " + data.realSize());
        }

        int[] linger = unpackLinger();

        return runtime.newArray(linger[0] == 0 ? runtime.getFalse() : runtime.getTrue(), runtime.newFixnum(linger[1]));
    }

    @JRubyMethod
    public IRubyObject unpack(ThreadContext context, IRubyObject arg0) {
        return Pack.unpack(context.runtime, data, arg0.convertToString().getByteList());
    }

    @JRubyMethod
    public IRubyObject to_s(ThreadContext context) {
        return RubyString.newString(context.runtime, data);
    }

    private ProtocolFamily family;
    private SocketLevel level;
    private SocketOption option;
    private ByteList data;
}
