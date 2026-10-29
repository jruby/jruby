package org.jruby.ext.socket;

import jnr.constants.platform.ErrnoAddressInfo;
import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.anno.CRuby;
import org.jruby.anno.JRubyMethod;
import org.jruby.api.Access;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Convert.asFixnum;

/**
 * Socket::ResolutionError, the SocketError raised when a host or service name cannot be resolved.
 */
public class ResolutionError {
    private static final String ERROR_CODE = "error_code";

    static void createResolutionError(ThreadContext context, RubyClass Socket, RubyClass SocketError) {
        Socket.defineClassUnder(context, "ResolutionError", SocketError, SocketError.getAllocator()).
                defineMethods(context, ResolutionError.class);
    }

    @CRuby(value = "sock_resolv_error_code", file = "ext/socket/init.c")
    @JRubyMethod
    public static IRubyObject error_code(ThreadContext context, IRubyObject self) {
        Object errorCode = ((RubyBasicObject) self).getInternalVariable(ERROR_CODE);
        return errorCode == null ? context.nil : (IRubyObject) errorCode;
    }

    @CRuby(value = "rsock_raise_resolution_error_for_host", file = "ext/socket/init.c")
    static RaiseException newResolutionError(ThreadContext context, String message, ErrnoAddressInfo errorCode) {
        RaiseException exception = RaiseException.from(context.runtime, Access.getClass(context, "Socket", "ResolutionError"), message);
        exception.getException().setInternalVariable(ERROR_CODE, asFixnum(context, errorCode.intValue()));
        return exception;
    }
}
