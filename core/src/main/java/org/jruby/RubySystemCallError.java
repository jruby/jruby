package org.jruby;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyClass;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.SystemCallError;
import org.jruby.platform.Platform;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectMarshal;

import static org.jruby.api.Access.errnoModule;
import static org.jruby.api.Convert.toInt;
import static org.jruby.api.Create.newString;
import static org.jruby.api.Define.defineClass;
import static org.jruby.runtime.Visibility.*;

import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.Variable;
import org.jruby.runtime.component.VariableEntry;
import org.jruby.runtime.marshal.MarshalDumper;

import jnr.constants.platform.Errno;
import org.jruby.runtime.marshal.MarshalLoader;
import org.jruby.util.io.RubyInputStream;
import org.jruby.util.io.RubyOutputStream;

/**
 * The Java representation of a Ruby SystemCallError.
 *
 * @see SystemCallError
 */
@JRubyClass(name="SystemCallError", parent="StandardError")
public class RubySystemCallError extends RubyStandardError {
    private IRubyObject errno = getRuntime().getNil();

    private final static Map<String, String> defaultMessages = new HashMap<String, String>();
    static {
        // FIXME: these descriptions should probably be moved out,
        // to Constantine project which deals with all platform-dependent constants.
        defaultMessages.put("Errno::EPERM", "Operation not permitted");
        defaultMessages.put("Errno::ENOENT", "No such file or directory");
        defaultMessages.put("Errno::ESRCH", "No such process");
        defaultMessages.put("Errno::EINTR", "Interrupted system call");
        defaultMessages.put("Errno::EIO", "Input/output error");
        defaultMessages.put("Errno::ENXIO", "Device not configured");
        defaultMessages.put("Errno::E2BIG", "Argument list too long");
        defaultMessages.put("Errno::ENOEXEC", "Exec format error");
        defaultMessages.put("Errno::EBADF", "Bad file descriptor");
        defaultMessages.put("Errno::ECHILD", "No child processes");
        defaultMessages.put("Errno::EDEADLK", "Resource deadlock avoided");
        defaultMessages.put("Errno::ENOMEM", "Cannot allocate memory");
        defaultMessages.put("Errno::EACCES", "Permission denied");
        defaultMessages.put("Errno::EFAULT", "Bad address");
        defaultMessages.put("Errno::ENOTBLK", "Block device required");
        defaultMessages.put("Errno::EBUSY", "Resource busy");
        defaultMessages.put("Errno::EEXIST", "File exists");
        defaultMessages.put("Errno::EXDEV", "Cross-device link");
        defaultMessages.put("Errno::ENODEV", "Operation not supported by device");
        defaultMessages.put("Errno::ENOTDIR", "Not a directory");
        defaultMessages.put("Errno::EISDIR", "Is a directory");
        defaultMessages.put("Errno::EINVAL", "Invalid argument");
        defaultMessages.put("Errno::ENFILE", "Too many open files in system");
        defaultMessages.put("Errno::EMFILE", "Too many open files");
        defaultMessages.put("Errno::ENOTTY", "Inappropriate ioctl for device");
        defaultMessages.put("Errno::ETXTBSY", "Text file busy");
        defaultMessages.put("Errno::EFBIG", "File too large");
        defaultMessages.put("Errno::ENOSPC", "No space left on device");
        defaultMessages.put("Errno::ESPIPE", "Illegal seek");
        defaultMessages.put("Errno::EROFS", "Read-only file system");
        defaultMessages.put("Errno::EMLINK", "Too many links");
        defaultMessages.put("Errno::EPIPE", "Broken pipe");
        defaultMessages.put("Errno::EDOM", "Numerical argument out of domain");
        defaultMessages.put("Errno::ERANGE", "Result too large");
        defaultMessages.put("Errno::EAGAIN", "Resource temporarily unavailable");
        defaultMessages.put("Errno::EWOULDBLOCK", "Resource temporarily unavailable");
        defaultMessages.put("Errno::EINPROGRESS", "Operation now in progress");
        defaultMessages.put("Errno::EALREADY", "Operation already in progress");
        defaultMessages.put("Errno::ENOTSOCK", "Socket operation on non-socket");
        defaultMessages.put("Errno::EDESTADDRREQ", "Destination address required");
        defaultMessages.put("Errno::EMSGSIZE", "Message too long");
        defaultMessages.put("Errno::EPROTOTYPE", "Protocol wrong type for socket");
        defaultMessages.put("Errno::ENOPROTOOPT", "Protocol not available");
        defaultMessages.put("Errno::EPROTONOSUPPORT", "Protocol not supported");
        defaultMessages.put("Errno::ESOCKTNOSUPPORT", "Socket type not supported");
        defaultMessages.put("Errno::EPFNOSUPPORT", "Protocol family not supported");
        defaultMessages.put("Errno::EAFNOSUPPORT", "Address family not supported by protocol family");
        defaultMessages.put("Errno::EADDRINUSE", "Address already in use");
        defaultMessages.put("Errno::EADDRNOTAVAIL", "Can't assign requested address");
        defaultMessages.put("Errno::ENETDOWN", "Network is down");
        defaultMessages.put("Errno::ENETUNREACH", "Network is unreachable");
        defaultMessages.put("Errno::ENETRESET", "Network dropped connection on reset");
        defaultMessages.put("Errno::ECONNABORTED", "Software caused connection abort");
        defaultMessages.put("Errno::ECONNRESET", "Connection reset by peer");
        defaultMessages.put("Errno::ENOBUFS", "No buffer space available");
        defaultMessages.put("Errno::EISCONN", "Socket is already connected");
        defaultMessages.put("Errno::ENOTCONN", "Socket is not connected");
        defaultMessages.put("Errno::ESHUTDOWN", "Can't send after socket shutdown");
        defaultMessages.put("Errno::ETOOMANYREFS", "Too many references: can't splice");
        defaultMessages.put("Errno::ETIMEDOUT", "Operation timed out");
        defaultMessages.put("Errno::ECONNREFUSED", "Connection refused");
        defaultMessages.put("Errno::ELOOP", "Too many levels of symbolic links");
        defaultMessages.put("Errno::ENAMETOOLONG", "File name too long");
        defaultMessages.put("Errno::EHOSTDOWN", "Host is down");
        defaultMessages.put("Errno::EHOSTUNREACH", "No route to host");
        defaultMessages.put("Errno::ENOTEMPTY", "Directory not empty");
        defaultMessages.put("Errno::EUSERS", "Too many users");
        defaultMessages.put("Errno::EDQUOT", "Disc quota exceeded");
        defaultMessages.put("Errno::ESTALE", "Stale NFS file handle");
        defaultMessages.put("Errno::EREMOTE", "Too many levels of remote in path");
        defaultMessages.put("Errno::ENOLCK", "No locks available");
        defaultMessages.put("Errno::ENOSYS", "Function not implemented");
        defaultMessages.put("Errno::EOVERFLOW", "Value too large to be stored in data type");
        defaultMessages.put("Errno::EIDRM", "Identifier removed");
        defaultMessages.put("Errno::ENOMSG", "No message of desired type");
        defaultMessages.put("Errno::EILSEQ", "Illegal byte sequence");
        defaultMessages.put("Errno::EBADMSG", "Bad message");
        defaultMessages.put("Errno::EMULTIHOP", "EMULTIHOP (Reserved)");
        defaultMessages.put("Errno::ENODATA", "No message available on STREAM");
        defaultMessages.put("Errno::ENOLINK", "ENOLINK (Reserved)");
        defaultMessages.put("Errno::ENOSR", "No STREAM resources");
        defaultMessages.put("Errno::ENOSTR", "Not a STREAM");
        defaultMessages.put("Errno::EPROTO", "Protocol error");
        defaultMessages.put("Errno::ETIME", "STREAM ioctl timeout");
        defaultMessages.put("Errno::EOPNOTSUPP", "Operation not supported");
        defaultMessages.put("Errno::EOPNOTSUPP_DARWIN", "Operation not supported");
    }

    protected RubySystemCallError(Ruby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass, null);
    }

    @Override
    protected RaiseException constructThrowable(String message) {
        return new SystemCallError(message, this);
    }

    private static final ObjectMarshal SYSTEM_CALL_ERROR_MARSHAL = new ObjectMarshal() {
        @Override
        @Deprecated(since = "10.0", forRemoval = true)
        @SuppressWarnings("removal")
        public void marshalTo(Ruby runtime, Object obj, RubyClass type,
                              org.jruby.runtime.marshal.MarshalStream marshalStream) throws IOException {
            RubySystemCallError exc = (RubySystemCallError) obj;
            var context = runtime.getCurrentContext();
            marshalStream.registerLinkTarget(context, exc);

            List<Variable<Object>> attrs = exc.getMarshalVariableList();
            attrs.add(new VariableEntry<>("mesg", exc.message == null ? context.nil : exc.message));
            attrs.add(new VariableEntry<>("errno", exc.errno));
            attrs.add(new VariableEntry<>("bt", exc.getBacktrace()));
            marshalStream.dumpVariables(attrs);
        }

        @Override
        public void marshalTo(ThreadContext context, RubyOutputStream out, Object obj, RubyClass type,
                              MarshalDumper marshalStream) {
            RubySystemCallError exc = (RubySystemCallError) obj;
            marshalStream.registerLinkTarget(exc);

            marshalStream.dumpVariables(context, out, exc, 3, (marshal, c, o, v, receiver) -> {
                receiver.receive(marshal, c, o, "mesg", v.message == null ? c.nil : v.message);
                receiver.receive(marshal, c, o, "errno", v.errno);
                receiver.receive(marshal, c, o, "bt", v.getBacktrace());
            });
        }

        @Override
        @Deprecated(since = "10.0", forRemoval = true)
        @SuppressWarnings("removal")
        public Object unmarshalFrom(Ruby runtime, RubyClass type, org.jruby.runtime.marshal.UnmarshalStream input) throws IOException {
            var context = runtime.getCurrentContext();
            RubySystemCallError exc = (RubySystemCallError) input.entry(type.allocate(context));

            input.ivar(null, exc, null);
            
            exc.message = (IRubyObject) exc.removeInternalVariable("mesg");
            exc.errno = (IRubyObject) exc.removeInternalVariable("errno");
            exc.set_backtrace(context, (IRubyObject) exc.removeInternalVariable("bt"));
            
            return exc;
        }

        @Override
        public Object unmarshalFrom(ThreadContext context, RubyInputStream in, RubyClass type, MarshalLoader input) {
            RubySystemCallError exc = (RubySystemCallError) input.entry(type.allocate(context));

            input.ivar(context, in, null, exc, null);

            exc.message = (IRubyObject) exc.removeInternalVariable("mesg");
            exc.errno = (IRubyObject) exc.removeInternalVariable("errno");
            exc.set_backtrace(context, (IRubyObject) exc.removeInternalVariable("bt"));

            return exc;
        }
    };

    public static RubyClass define(ThreadContext context, RubyClass StandardError) {
        return defineClass(context, "SystemCallError", StandardError, RubySystemCallError::new).
                marshalWith(SYSTEM_CALL_ERROR_MARSHAL).
                defineMethods(context, RubySystemCallError.class);
    }

    @Deprecated
    @Override
    public IRubyObject initialize(IRubyObject[] args, Block unused) {
        return initialize(getRuntime().getCurrentContext(), args);
    }
    
    @JRubyMethod(optional = 2, checkArity = false, visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;

        int argc = Arity.checkArgumentCount(context, args, 0, 2);

        RubyClass sCallErorrClass = runtime.getSystemCallError();
        RubyClass klass = getMetaClass().getRealClass();

        IRubyObject msg = context.nil;
        IRubyObject err = context.nil;

        boolean isErrnoClass = !klass.equals(sCallErorrClass);

        if (!isErrnoClass) {
            // one optional, one required args
            Arity.checkArgumentCount(context, args, 1, 2);
            msg = args[0];
            if (argc == 2) {
                err = args[1];
            }
            if (argc == 1 && (msg instanceof RubyFixnum)) {
                err = msg;
                msg = context.nil;
            }
        } else {
            // one optional and no required args
            Arity.checkArgumentCount(context, args, 0, 1);
            if (argc == 1) msg = args[0];

            // try to get errno value out of the class
            err = klass.getConstant(context, "Errno");
        }

        String val = null;

        if (!err.isNil()) {
            errno = err.convertToInteger();
            int errnoVal = toInt(context, errno);
            if (Errno.valueOf(errnoVal) != Errno.__UNKNOWN_CONSTANT__) {
                // we got a valid errno value
                isErrnoClass = true;

                // set the metaclass to an Errno, if something other than SystemCallError or Errno wasn't provided
                if (metaClass == runtime.getSystemCallError() || metaClass == errnoModule(context)) {
                    setMetaClass(runtime.getErrno(errnoVal));
                }
                
                klass = getMetaClass().getRealClass();

                // FIXME: Errno descriptions from Constantine
                // on Windows are not useful at the moment.
                if (!Platform.IS_WINDOWS) {
                  val = Errno.valueOf(errnoVal).description();
                }
            }
        }

        if (val == null) {
            var className = klass.getName(context);
            val = defaultMessages.get(className);
            if (val == null) val = "Unknown error (" + className + ")";
        }

        // MRI behavior: we don't print errno for actual Errno errors
        if (!errno.isNil() && !isErrnoClass) {
            val += " " + errno.toString();
        }

        if (!msg.isNil()) {
            val += " - " + msg.convertToString();
        }

        message = newString(context, val);
        return this;
    }

    @JRubyMethod
    public IRubyObject errno() {
        return errno;
    }
}
