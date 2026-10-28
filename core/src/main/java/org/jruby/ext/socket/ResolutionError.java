package org.jruby.ext.socket;

import jnr.constants.platform.ErrnoAddressInfo;

import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.anno.CRuby;
import org.jruby.anno.JRubyMethod;
import org.jruby.api.Access;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Convert.asFixnum;

/**
 * Socket::ResolutionError, the SocketError raised when getaddrinfo or getnameinfo fails, and the EAI_*
 * error codes it carries.
 */
public class ResolutionError {
    static void createResolutionError(ThreadContext context, RubyModule Socket, RubyModule SocketConstants) {
        RubyClass SocketError = Access.getClass(context, "SocketError");

        Socket.defineClassUnder(context, "ResolutionError", SocketError, SocketError.getAllocator()).
                defineMethods(context, ResolutionError.class);

        // the getaddrinfo(3) error codes, which differ between platforms
        SocketConstants.defineConstantsFrom(context, ErrnoAddressInfo.class);
    }

    @JRubyMethod
    @CRuby(value = "sock_resolv_error_code", file = "ext/socket/init.c")
    public static IRubyObject error_code(ThreadContext context, IRubyObject self) {
        IRubyObject code = (IRubyObject) self.getInternalVariables().getInternalVariable("error_code");
        return code == null ? context.nil : code;
    }

    /**
     * A Socket::ResolutionError for the given EAI_* code, with a message like "getaddrinfo: name or service
     * not known".
     *
     * @param context the current thread context
     * @param message the message, which names the failing function first
     * @param eai the error code's constant name, such as "EAI_NONAME"
     * @return the exception to throw
     */
    @CRuby(value = "rsock_raise_resolution_error_for_host", file = "ext/socket/init.c")
    public static RaiseException newResolutionError(ThreadContext context, String message, String eai) {
        RubyClass ResolutionError = Access.getClass(context, "Socket", "ResolutionError");
        RaiseException error = RaiseException.from(context.runtime, ResolutionError, message);

        ErrnoAddressInfo code = ErrnoAddressInfo.valueOf(eai);
        if (code.defined()) {
            error.getException().getInternalVariables().setInternalVariable("error_code", asFixnum(context, code.intValue()));
        }

        return error;
    }
}
