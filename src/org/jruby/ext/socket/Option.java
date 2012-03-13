package org.jruby.ext.socket;

import jnr.constants.platform.Errno;
import jnr.constants.platform.ProtocolFamily;
import jnr.constants.platform.Sock;
import jnr.constants.platform.SocketLevel;
import jnr.constants.platform.SocketOption;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
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
        this(runtime, (RubyClass)runtime.getClassFromPath("Socket::Option"), family, level, option, data);
    }

    public Option(Ruby runtime, RubyClass klass, ProtocolFamily family, SocketLevel level, SocketOption option, int data) {
        super(runtime, klass);
        
        this.family = family;
        this.level = level;
        this.option = option;
        this.intData = data;
        ByteList result = new ByteList(4);
        this.data = Pack.packInt_i(result, data);
    }
    
    @JRubyMethod(required = 4)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        family = ProtocolFamily.valueOf(args[0].convertToInteger().getLongValue());
        level = SocketLevel.valueOf(args[1].convertToInteger().getLongValue());
        option = SocketOption.valueOf(args[2].convertToInteger().getLongValue());
        data = args[3].convertToString().getByteList();
        intData = Pack.unpackInt_i(ByteBuffer.wrap(data.bytes()));
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
        return context.runtime.newFixnum(option.longValue());
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
                return String.valueOf(intData);

            case SO_LINGER:
                return intData == -1 ? "off" :
                        intData == 0 ? "on" :
                                "on(" + intData + ")";

            case SO_RCVTIMEO:
            case SO_SNDTIMEO:
                return Sprintf.getNumberFormat(Locale.getDefault()).format(intData / 1000.0);

            case SO_ERROR:
                return Errno.valueOf(intData).description();

            case SO_TYPE:
                return Sock.valueOf(intData).description();
        }

        return "";
    }

    @JRubyMethod(meta = true)
    public IRubyObject rb_int(ThreadContext context, IRubyObject self) {
        return context.nil;
    }
    
    @JRubyMethod
    public IRubyObject rb_int(ThreadContext context) {
        return context.nil;
    }

    @JRubyMethod(meta = true)
    public IRubyObject bool(ThreadContext context, IRubyObject self) {
        return context.nil;
    }

    @JRubyMethod
    public IRubyObject bool(ThreadContext context) {
        return context.nil;
    }

    @JRubyMethod(meta = true)
    public IRubyObject linger(ThreadContext context, IRubyObject self) {
        return context.nil;
    }

    @JRubyMethod
    public IRubyObject linger(ThreadContext context) {
        return context.nil;
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
    private long intData;
}
