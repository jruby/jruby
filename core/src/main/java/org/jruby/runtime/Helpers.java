package org.jruby.runtime;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;

import java.net.PortUnreachableException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.NotYetConnectedException;
import java.nio.charset.Charset;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemLoopException;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.headius.invokebinder.Binder;
import jnr.constants.platform.Errno;
import org.jruby.*;
import org.jruby.api.Convert;
import org.jruby.api.Create;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ArgumentNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.Node;
import org.jruby.ast.UnnamedRestArgNode;
import org.jruby.ast.types.INameNode;
import org.jruby.ast.RequiredKeywordArgumentValueNode;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.exceptions.ArgumentError;
import org.jruby.exceptions.NoMethodError;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.Unrescuable;
import org.jruby.internal.runtime.methods.*;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScopeType;
import org.jruby.ir.Interp;
import org.jruby.ir.JIT;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaUtil;
import org.jruby.javasupport.proxy.ReifiedJavaProxy;
import org.jruby.parser.StaticScope;
import org.jruby.parser.StaticScopeFactory;
import org.jruby.runtime.JavaSites.HelpersSites;
import org.jruby.runtime.backtrace.BacktraceData;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.runtime.invokedynamic.MethodNames;
import org.jruby.util.ArraySupport;
import org.jruby.util.ByteList;
import org.jruby.util.CodegenUtils;
import org.jruby.util.CommonByteLists;
import org.jruby.util.MurmurHash;
import org.jruby.util.TypeConverter;

import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jcodings.unicode.UnicodeEncoding;

import static org.jruby.RubyBasicObject.getMetaClass;
import static org.jruby.api.Access.arrayClass;
import static org.jruby.api.Access.exceptionClass;
import static org.jruby.api.Access.instanceConfig;
import static org.jruby.api.Access.kernelModule;
import static org.jruby.api.Access.moduleClass;
import static org.jruby.api.Access.objectClass;
import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Convert.asSymbol;
import static org.jruby.api.Create.*;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.typeError;
import static org.jruby.runtime.ThreadContext.CALL_KEYWORD_EMPTY;
import static org.jruby.runtime.Visibility.*;
import static org.jruby.runtime.invokedynamic.MethodNames.EQL;
import static org.jruby.util.CodegenUtils.params;
import static org.jruby.util.CodegenUtils.sig;
import static org.jruby.util.RubyStringBuilder.str;
import static org.jruby.util.RubyStringBuilder.ids;
import static org.jruby.util.RubyStringBuilder.types;
import static org.jruby.util.StringSupport.EMPTY_STRING_ARRAY;

import org.jruby.util.io.EncodingUtils;

/**
 * Helper methods which are called by the compiler.  Note: These will show no consumers, but
 * generated code does call these so don't remove them thinking they are dead code.
 *
 */
public class Helpers {

    public static final Pattern SEMICOLON_PATTERN = Pattern.compile(";");
    public static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8; // safe max for new byte[], see GH-6671

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    public static RubyClass getSingletonClass(Ruby runtime, IRubyObject receiver) {
        if (receiver instanceof RubyFixnum || receiver instanceof RubySymbol) {
            throw typeError(runtime.getCurrentContext(), "can't define singleton");
        } else {
            return receiver.getSingletonClass();
        }
    }

    public static IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, RubyClass klass, Visibility visibility, String name, CallType callType, IRubyObject[] args, Block block) {
        return selectMethodMissing(context, klass, visibility, name, callType).call(context, self, klass, name, args, block);
    }

    public static IRubyObject callMethodMissing(ThreadContext context, IRubyObject receiver, Visibility visibility, String name, CallType callType, IRubyObject[] args, Block block) {
        final RubyClass klass = getMetaClass(receiver);
        return selectMethodMissing(context, klass, visibility, name, callType).call(context, receiver, klass, name, args, block);
    }

    public static IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, RubyClass klass, Visibility visibility, String name, CallType callType, IRubyObject arg0, Block block) {
        return selectMethodMissing(context, klass, visibility, name, callType).call(context, self, klass, name, arg0, block);
    }

    public static IRubyObject callMethodMissing(ThreadContext context, IRubyObject receiver, Visibility visibility, String name, CallType callType, IRubyObject arg0, Block block) {
        final RubyClass klass = getMetaClass(receiver);
        return selectMethodMissing(context, klass, visibility, name, callType).call(context, receiver, klass, name, arg0, block);
    }

    public static IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, RubyClass klass, Visibility visibility, String name, CallType callType, IRubyObject arg0, IRubyObject arg1, Block block) {
        return selectMethodMissing(context, klass, visibility, name, callType).call(context, self, klass, name, arg0, arg1, block);
    }

    public static IRubyObject callMethodMissing(ThreadContext context, IRubyObject receiver, Visibility visibility, String name, CallType callType, IRubyObject arg0, IRubyObject arg1, Block block) {
        final RubyClass klass = getMetaClass(receiver);
        return selectMethodMissing(context, klass, visibility, name, callType).call(context, receiver, klass, name, arg0, arg1, block);
    }

    public static IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, RubyClass klass, Visibility visibility, String name, CallType callType, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return selectMethodMissing(context, klass, visibility, name, callType).call(context, self, klass, name, arg0, arg1, arg2, block);
    }

    public static IRubyObject callMethodMissing(ThreadContext context, IRubyObject receiver, Visibility visibility, String name, CallType callType, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        final RubyClass klass = getMetaClass(receiver);
        return selectMethodMissing(context, klass, visibility, name, callType).call(context, receiver, klass, name, arg0, arg1, arg2, block);
    }

    public static IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, RubyClass klass, Visibility visibility, String name, CallType callType, Block block) {
        return selectMethodMissing(context, klass, visibility, name, callType).call(context, self, klass, name, block);
    }

    public static IRubyObject callMethodMissing(ThreadContext context, IRubyObject receiver, Visibility visibility, String name, CallType callType, Block block) {
        final RubyClass klass = getMetaClass(receiver);
        return selectMethodMissing(context, klass, visibility, name, callType).call(context, receiver, klass, name, block);
    }

    public static DynamicMethod selectMethodMissing(ThreadContext context, IRubyObject receiver, Visibility visibility, String name, CallType callType) {
        Ruby runtime = context.runtime;

        if (name.equals("method_missing")) {
            return selectInternalMM(runtime, visibility, callType);
        }

        CacheEntry entry = receiver.getMetaClass().searchWithCache("method_missing");
        DynamicMethod methodMissing = entry.method;

        if (methodMissing.isUndefined() || methodMissing.equals(runtime.getDefaultMethodMissing())) {
            return selectInternalMM(runtime, visibility, callType);
        }
        return new MethodMissingWrapper(entry, visibility, callType);
    }

    public static DynamicMethod selectMethodMissing(ThreadContext context, RubyClass selfClass, Visibility visibility, String name, CallType callType) {
        final Ruby runtime = context.runtime;

        if (name.equals("method_missing")) {
            return selectInternalMM(runtime, visibility, callType);
        }

        CacheEntry entry = selfClass.searchWithCache("method_missing");
        DynamicMethod methodMissing = entry.method;

        if (methodMissing.isUndefined() || methodMissing.equals(runtime.getDefaultMethodMissing())) {
            return selectInternalMM(runtime, visibility, callType);
        }
        return new MethodMissingWrapper(entry, visibility, callType);
    }

    public static DynamicMethod selectMethodMissing(RubyClass selfClass, Visibility visibility, String name, CallType callType) {
        Ruby runtime = selfClass.getClassRuntime();

        if (name.equals("method_missing")) {
            return selectInternalMM(runtime, visibility, callType);
        }

        CacheEntry entry = selfClass.searchWithCache("method_missing");
        DynamicMethod methodMissing = entry.method;

        if (methodMissing.isUndefined() || methodMissing.equals(runtime.getDefaultMethodMissing())) {
            return selectInternalMM(runtime, visibility, callType);
        }
        return new MethodMissingWrapper(entry, visibility, callType);
    }

    public static final Map<String, String> map(String... keyValues) {
        HashMap<String, String> map = new HashMap<>(keyValues.length / 2 + 1, 1);
        for (int i = 0; i < keyValues.length;) {
            map.put(keyValues[i++], keyValues[i++]);
        }
        return map;
    }

    public static boolean additionOverflowed(long original, long other, long result) {
        return (~(original ^ other) & (original ^ result) & RubyFixnum.SIGN_BIT) != 0;
    }

    public static boolean subtractionOverflowed(long original, long other, long result) {
        return (~(original ^ ~other) & (original ^ result) & RubyFixnum.SIGN_BIT) != 0;
    }

    /**
     * This method attempts to produce an Errno value for the given exception.
     *
     * Many low-level operations wrapped by the JDK will raise IOException or subclasses of it when there's a system-
     * level error. In most cases, the only way to determine the cause of the IOException is by inspecting its contents,
     * usually by checking the error message string. This is obviously fragile and breaks on platforms localized to
     * languages other than English, so we also try as much as possible to detect the cause of the error by its actual
     * type (if it is indeed a specialized subtype of IOException).
     *
     * @param t the exception to convert to an {@link Errno}
     * @return the resulting {@link Errno} value, or null if none could be determined.
     */
    public static Errno errnoFromException(Throwable t) {
        // FIXME: Error-message scraping is gross and turns out to be fragile if the host system is localized jruby/jruby#5415

        // Try specific exception types by rethrowing and catching.
        try {
            throw t;
        } catch (FileNotFoundException | NoSuchFileException fnfe) {
            return Errno.ENOENT;
        } catch (EOFException eofe) {
            return Errno.EPIPE;
        } catch (AtomicMoveNotSupportedException amnse) {
            return Errno.EXDEV;
        } catch (ClosedChannelException cce) {
            return Errno.EBADF;
        } catch (PortUnreachableException pue) {
            return Errno.ECONNREFUSED;
        } catch (FileAlreadyExistsException faee) {
            return Errno.EEXIST;
        } catch (FileSystemLoopException fsle) {
            return Errno.ELOOP;
        } catch (NotDirectoryException nde) {
            return Errno.ENOTDIR;
        } catch (AccessDeniedException ade) {
            return Errno.EACCES;
        } catch (IOException be) {
            return errnoFromMessage(be);
        } catch (NotYetConnectedException nyce) {
            return Errno.ENOTCONN;
        } catch (NonReadableChannelException | NonWritableChannelException nrce) {
            // raised by NIO for invalid combinations of file options (read + truncate, for example)
            return Errno.EINVAL;
        } catch (IllegalArgumentException nrce) {
            return Errno.EINVAL;
        } catch (Throwable t2) {
            // fall through
        }

        return errnoFromMessage(t);
    }

    private static Errno errnoFromMessage(Throwable t) {
        final String errorMessage = t.getMessage();

        if (errorMessage != null) {
            // All errors to sysread should be SystemCallErrors, but on a closed stream
            // Ruby returns an IOError.  Java throws same exception for all errors so
            // we resort to this hack...

            switch (errorMessage) {
                case "Bad file descriptor":
                    return Errno.EBADF;
                case "File not open":
                    return null;
                case "An established connection was aborted by the software in your host machine":
                case "connection was aborted": // Windows
                    return Errno.ECONNABORTED;
                case "Broken pipe":
                    return Errno.EPIPE;
                case "Connection reset":
                case "Connection reset by peer":
                case "An existing connection was forcibly closed by the remote host":
                    return Errno.ECONNRESET;
                case "Too many levels of symbolic links":
                    return Errno.ELOOP;
                case "Too many open files":
                    return Errno.EMFILE;
                case "Too many open files in system":
                    return Errno.ENFILE;
                case "Network is unreachable":
                    return Errno.ENETUNREACH;
                case "Address already in use":
                    return Errno.EADDRINUSE;
                case "Cannot assign requested address":
                case "Can't assign requested address":
                    return Errno.EADDRNOTAVAIL;
                case "No space left on device":
                    return Errno.ENOSPC;
                case "Message too large": // Alpine Linux
                case "Message too long":
                    return Errno.EMSGSIZE;
                case "Is a directory":
                    return Errno.EISDIR;
                case "Operation timed out":
                    return Errno.ETIMEDOUT;
                case "No route to host":
                    return Errno.EHOSTUNREACH;
                case "permission denied":
                case "Permission denied":
                    return Errno.EACCES;
                case "Protocol family not supported":
                    return Errno.EPFNOSUPPORT;
            }

            // Raised on Windows for process launch with missing file
            if (errorMessage.endsWith("The system cannot find the file specified")) {
                return Errno.ENOENT;
            }
        }

        return null;
    }

    /**
     * Construct an appropriate error (which may ultimately not be an IOError) for a given IOException.
     *
     * If this method is used on an exception which can't be translated to a Ruby error using {@link #newErrorFromException(Ruby, Throwable)}
     * then a RuntimeError will be returned, due to the unhandled exception type.
     *
     * @param runtime the current runtime
     * @param ex the exception to translate into a Ruby error
     * @return a RaiseException subtype instance appropriate for the given exception
     */
    public static RaiseException newIOErrorFromException(Ruby runtime, IOException ex) {
        return (RaiseException) newErrorFromException(runtime, ex, (t) -> runtime.newRuntimeError("unexpected Java exception: " + ex.toString()));
    }

    /**
     * Return a Ruby-friendly Throwable for a given Throwable.
     *
     * The following translations will be attempted in order:
     *
     * <ul>
     *     <li>if the Throwable is already a Ruby exception type, return it as-is</li>
     *     <li>convert to a Ruby Errno exception via {@link #errnoFromException(Throwable)}</li>
     *     <li>convert to a Ruby IOError if the exception is a java.io.IOException</li>
     *     <li>using the provided function as a fallback transformation</li>
     * </ul>
     *
     * @param runtime the current runtime
     * @param t the exception to translate into a Ruby error
     * @param els a fallback function if the exception cannot be translated
     * @return a RaiseException subtype instance appropriate for the given exception
     */
    public static Throwable newErrorFromException(Ruby runtime, Throwable t, Function<Throwable, Throwable> els) {
        if (t instanceof RaiseException) {
            // already a Ruby-friendly Throwable
            return t;
        }

        Errno errno = errnoFromException(t);

        if (errno != null) {
            return runtime.newErrnoFromErrno(errno, t.getLocalizedMessage());
        } else if (t instanceof IOException) {
            return runtime.newIOError(t.getLocalizedMessage());
        }

        return els.apply(t);
    }

    /**
     * Simplified form of Ruby#newErrorFromException with no default function.
     *
     * @param runtime the current runtime
     * @param t the exception to translate into a Ruby error
     * @return a RaiseException subtype instance appropriate for the given exception
     */
    public static Throwable newErrorFromException(Ruby runtime, Throwable t) {
        return newErrorFromException(runtime, t, t0 -> t0);
    }

    /**
     * Throw an appropriate Ruby-friendly error or exception for a given Java exception.
     *
     * This method will first attempt to translate the exception into a Ruby error using {@link #newErrorFromException(Ruby, Throwable, Function)}.
     *
     * Failing that, it will raise the original Java exception as-is.
     *
     * @param runtime the current runtime
     * @param t the exception to raise as an error, if appropriate, or as itself otherwise
     */
    public static void throwErrorFromException(Ruby runtime, Throwable t) {
        throwException(newErrorFromException(runtime, t));
    }

    public static RubyModule getNthScopeModule(StaticScope scope, int depth) {
        int n = depth;
        while (n > 0) {
            scope = scope.getEnclosingScope();
            if (scope.getScopeType() != null) {
                n--;
            }
        }
        return scope.getModule();
    }

    public static RubyArray viewArgsArray(ThreadContext context, RubyArray rubyArray, int preArgsCount, int postArgsCount) {
        int n = rubyArray.getLength();
        return preArgsCount + postArgsCount >= n ?
                newEmptyArray(context) :
                (RubyArray<?>) rubyArray.subseq(arrayClass(context), preArgsCount, n - preArgsCount - postArgsCount, true);
    }

    public static Class[] getStaticMethodParams(Class target, int args) {
        switch (args) {
        case 0:
            return new Class[] {target, ThreadContext.class, IRubyObject.class, Block.class};
        case 1:
            return new Class[] {target, ThreadContext.class, IRubyObject.class, IRubyObject.class, Block.class};
        case 2:
            return new Class[] {target, ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class};
        case 3:
            return new Class[] {target, ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class};
        case 4:
            return new Class[] {target, ThreadContext.class, IRubyObject.class, IRubyObject[].class, Block.class};
        default:
            throw new RuntimeException("unsupported arity: " + args);
        }
    }

    public static String getStaticMethodSignature(String classname, int args) {
        switch (args) {
        case 0:
            return sig(IRubyObject.class, "L" + classname + ";", ThreadContext.class, IRubyObject.class, Block.class);
        case 1:
            return sig(IRubyObject.class, "L" + classname + ";", ThreadContext.class, IRubyObject.class, IRubyObject.class, Block.class);
        case 2:
            return sig(IRubyObject.class, "L" + classname + ";", ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class);
        case 3:
            return sig(IRubyObject.class, "L" + classname + ";", ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, IRubyObject.class, Block.class);
        case 4:
            return sig(IRubyObject.class, "L" + classname + ";", ThreadContext.class, IRubyObject.class, IRubyObject[].class, Block.class);
        default:
            throw new RuntimeException("unsupported arity: " + args);
        }
    }

    /**
     * Calculate a buffer length based on the required length, expanding by 1.5x or to the maximum array size.
     *
     * @param length the required length
     * @return a larger buffer length with extra room for growth, or else the max array size
     * @throws OutOfMemoryError if the requested length is greated than the max array size
     */
    public static int calculateBufferLength(int length) {
        if (length > MAX_ARRAY_SIZE) throw new OutOfMemoryError("Requested array size exceeds VM limit");

        int newLength;
        try {
            // Try to allocate 1.5 * length but that might take us outside the range of int
            newLength = Math.addExact(length, length >>> 1);
        } catch (ArithmeticException e) {
            newLength = MAX_ARRAY_SIZE;
        }
        return newLength;
    }

    /**
     * Same as {@link #calculateBufferLength(int)} but raises a Ruby ArgumentError.
     */
    public static int calculateBufferLength(Ruby runtime, int length) {
        if (length > MAX_ARRAY_SIZE) throw argumentError(runtime.getCurrentContext(), "argument too big");

        int newLength;
        try {
            // Try to allocate 1.5 * length but that might take us outside the range of int
            newLength = Math.addExact(length, length >>> 1);
        } catch (ArithmeticException e) {
            newLength = MAX_ARRAY_SIZE;
        }
        return newLength;
    }

    /**
     * @param runtime
     * @param base
     * @param multiplier
     * @return ""
     * @deprecated Use {@link Helpers#multiplyBufferLength(ThreadContext, int, int)} instead.
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public static int multiplyBufferLength(Ruby runtime, int base, int multiplier) {
        return multiplyBufferLength(runtime.getCurrentContext(), base, multiplier);
    }

        /**
         * Calculate a buffer length based on a base size and a multiplier. If the resulting size exceeds MAX_ARRAY_SIZE,
         * an {@link ArgumentError} will be thrown, similar to when asking the JVM to allocate a too-large array.
         *
         * @param context the thread context
         * @param base the base size
         * @param multiplier the multiplier
         * @return the multiplied size, if valid
         * @throws ArgumentError if the requested length is greated than the max array size
         */
    public static int multiplyBufferLength(ThreadContext context, int base, int multiplier) {
        try {
            int newSize = Math.multiplyExact(base, multiplier);
            if (newSize <= MAX_ARRAY_SIZE) return newSize;
            // fall through to error below
        } catch (ArithmeticException e) {
            // fall through to error below
        }

        throw argumentError(context, "argument too big");
    }

    /**
     * Calculate a buffer length based on a base size and a extra size. If the resulting size exceeds MAX_ARRAY_SIZE
     * and the extra size is nonzero, use the MAX_ARRAY_SIZE as the buffer length.
     *
     * @param runtime the runtime
     * @param base the base size
     * @param extra the extra buffer size
     * @return the combined buffer size, or MAX_ARRAY_SIZE
     * @throws ArgumentError if the original or combined size cannot be accommodated by MAX_ARRAY_SIZE
     */
    public static int addBufferLength(Ruby runtime, int base, int extra) {
        try {
            int newSize = Math.addExact(base, extra);

            if (newSize <= MAX_ARRAY_SIZE) return newSize;
            if (extra > 0) return MAX_ARRAY_SIZE;  // can't accommodate all of extra buffer size, but do as much as we can
            // fall through to error below
        } catch (ArithmeticException e) {
            // fall through to error below
        }

        throw argumentError(runtime.getCurrentContext(), "argument too big");
    }

    /**
     * Check that the buffer length requested is within the valid range of 0 to MAX_ARRAY_SIZE, or raise an argument
     * error.
     */
    public static int validateBufferLength(Ruby runtime, long length) {
        if (length < 0) throw argumentError(runtime.getCurrentContext(), "negative argument");
        if (length > MAX_ARRAY_SIZE) throw argumentError(runtime.getCurrentContext(), "argument too big");

        return (int) length;
    }

    public static CacheEntry createMethodMissingEntry(ThreadContext context, RubyClass selfClass, CallType callType, Visibility visibility, int token, String methodName) {
        DynamicMethod method = selectMethodMissing(context, selfClass, visibility, methodName, callType);
        return new CacheEntry(
                method,
                selfClass,
                token);
    }

    public static String encodeLongString(List<Long> values) {
        char[] chars = new char[values.size() * 4];

        for (int i = 0; i < values.size(); i++) {
            encodeLongAsChars(chars, i * 4, values.get(i));
        }

        return new String(chars);
    }

    public static long[] decodeLongString(String fixnumString) {
        char[] charValues = fixnumString.toCharArray();
        long[] values = new long[charValues.length / 4];

        for (int i = 0; i < values.length; i++) {
            values[i] = decodeLongFromChars(charValues, i * 4);
        }

        return values;
    }

    public static String encodeDoubleString(List<Double> values) {
        char[] chars = new char[values.size() * 4];

        for (int i = 0; i < values.size(); i++) {
            encodeLongAsChars(chars, i * 4, Double.doubleToRawLongBits(values.get(i)));
        }

        return new String(chars);
    }

    public static double[] decodeDoubleString(String fixnumString) {
        char[] charValues = fixnumString.toCharArray();
        double[] values = new double[charValues.length / 4];

        for (int i = 0; i < values.length; i++) {
            values[i] = decodeLongFromChars(charValues, i * 4);
        }

        return values;
    }

    private static void encodeLongAsChars(char[] chars, int index, long value) {
        chars[index] = (char) value;
        chars[index + 1] = (char) (value >>> 16);
        chars[index + 2] = (char) (value >>> 32);
        chars[index + 3] = (char) (value >>> 48);
    }

    private static long decodeLongFromChars(char[] charValues, int index) {
        long l = charValues[index];
        l |= (long) charValues[index + 1] << 16;
        l |= (long) charValues[index + 2] << 32;
        l |= (long) charValues[index + 3] << 48;
        return l;
    }

    /**
     * Wraps the target method_missing implementation, passing the called method name as a leading symbol argument.
     */
    public static class MethodMissingWrapper extends DynamicMethod {
        public final CacheEntry entry;
        private final CallType lastCallStatus;
        private final Visibility lastVisibility;
        private RubySymbol lastName;

        public MethodMissingWrapper(CacheEntry entry, Visibility lastVisibility, CallType lastCallStatus) {
            super(entry.method.getImplementationClass(), lastVisibility, entry.method.getName());
            this.entry = entry;
            this.lastCallStatus = lastCallStatus;
            this.lastVisibility = lastVisibility;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            context.setLastCallStatusAndVisibility(lastCallStatus, lastVisibility);
            return this.entry.method.call(context, self, entry.sourceModule, "method_missing", prepareMethodMissingArgs(args, context, name), block);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
            context.setLastCallStatusAndVisibility(lastCallStatus, lastVisibility);
            return this.entry.method.call(context, self, entry.sourceModule, "method_missing", nameToSymbol(context, name), block);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
            context.setLastCallStatusAndVisibility(lastCallStatus, lastVisibility);
            return this.entry.method.call(context, self, entry.sourceModule, "method_missing", nameToSymbol(context, name), arg0, block);
        }


        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
            context.setLastCallStatusAndVisibility(lastCallStatus, lastVisibility);
            return this.entry.method.call(context, self, entry.sourceModule, "method_missing", nameToSymbol(context, name), arg0, arg1, block);
        }


        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
            context.setLastCallStatusAndVisibility(lastCallStatus, lastVisibility);
            return this.entry.method.call(context, self, entry.sourceModule, "method_missing", arrayOf(nameToSymbol(context, name), arg0, arg1, arg2), block);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
            context.setLastCallStatusAndVisibility(lastCallStatus, lastVisibility);
            return this.entry.method.call(context, self, entry.sourceModule, "method_missing", prepareMethodMissingArgs(args, context, name));
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
            context.setLastCallStatusAndVisibility(lastCallStatus, lastVisibility);
            return this.entry.method.call(context, self, entry.sourceModule, "method_missing", nameToSymbol(context, name));
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
            context.setLastCallStatusAndVisibility(lastCallStatus, lastVisibility);
            return this.entry.method.call(context, self, entry.sourceModule, "method_missing", nameToSymbol(context, name), arg0);
        }


        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
            context.setLastCallStatusAndVisibility(lastCallStatus, lastVisibility);
            return this.entry.method.call(context, self, entry.sourceModule, "method_missing", nameToSymbol(context, name), arg0, arg1);
        }


        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
            context.setLastCallStatusAndVisibility(lastCallStatus, lastVisibility);
            return this.entry.method.call(context, self, entry.sourceModule, "method_missing", arrayOf(nameToSymbol(context, name), arg0, arg1, arg2));
        }

        @Override
        public DynamicMethod dup() {
            return this;
        }

        private IRubyObject[] prepareMethodMissingArgs(IRubyObject[] args, ThreadContext context, String name) {
            return ArraySupport.newCopy(nameToSymbol(context, name), args);
        }

        private RubySymbol nameToSymbol(ThreadContext context, String name) {
            RubySymbol lastName = this.lastName;
            if (lastName == null || !name.equals(lastName.idString())) {
                this.lastName = lastName = asSymbol(context, name);
            }
            return lastName;
        }
    }

    private static DynamicMethod selectInternalMM(Ruby runtime, Visibility visibility, CallType callType) {
        if (visibility == Visibility.PRIVATE) {
            return runtime.getPrivateMethodMissing();
        } else if (visibility == Visibility.PROTECTED) {
            return runtime.getProtectedMethodMissing();
        } else if (callType == CallType.VARIABLE) {
            return runtime.getVariableMethodMissing();
        } else if (callType == CallType.SUPER) {
            return runtime.getSuperMethodMissing();
        } else {
            return runtime.getNormalMethodMissing();
        }
    }

    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, Block block) {
        return self.getMetaClass().finvoke(context, self, name, block);
    }
    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, IRubyObject arg0, Block block) {
        return self.getMetaClass().finvoke(context, self, name, arg0, block);
    }
    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        return self.getMetaClass().finvoke(context, self, name, arg0, arg1, block);
    }
    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return self.getMetaClass().finvoke(context, self, name, arg0, arg1, arg2, block);
    }
    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, IRubyObject[] args, Block block) {
        return self.getMetaClass().finvoke(context, self, name, args, block);
    }

    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name) {
        return self.getMetaClass().finvoke(context, self, name);
    }
    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, IRubyObject arg0) {
        return self.getMetaClass().finvoke(context, self, name, arg0);
    }
    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1) {
        return self.getMetaClass().finvoke(context, self, name, arg0, arg1);
    }
    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return self.getMetaClass().finvoke(context, self, name, arg0, arg1, arg2);
    }
    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, IRubyObject... args) {
        return self.getMetaClass().finvoke(context, self, name, args);
    }

    public static IRubyObject invokeAs(ThreadContext context, RubyClass asClass, IRubyObject self, String name, IRubyObject[] args, Block block) {
        return asClass.finvoke(context, self, name, args, block);
    }

    public static IRubyObject invokeAs(ThreadContext context, RubyClass asClass, IRubyObject self, String name, Block block) {
        return asClass.finvoke(context, self, name, block);
    }

    public static IRubyObject invokeAs(ThreadContext context, RubyClass asClass, IRubyObject self, String name, IRubyObject arg0, Block block) {
        return asClass.finvoke(context, self, name, arg0, block);
    }

    public static IRubyObject invokeAs(ThreadContext context, RubyClass asClass, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        return asClass.finvoke(context, self, name, arg0, arg1, block);
    }

    public static IRubyObject invokeAs(ThreadContext context, RubyClass asClass, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return asClass.finvoke(context, self, name, arg0, arg1, arg2, block);
    }

    /**
    * MRI: rb_funcallv_public
    */
    public static IRubyObject invokePublic(ThreadContext context, IRubyObject self, String name, IRubyObject arg) {
        return getMetaClass(self).invokePublic(context, self, name, arg);
    }

    // MRI: rb_check_funcall
    public static IRubyObject invokeChecked(ThreadContext context, IRubyObject self, String name) {
        return getMetaClass(self).finvokeChecked(context, self, name);
    }

    // MRI: rb_check_funcall
    public static IRubyObject invokeChecked(ThreadContext context, IRubyObject self, JavaSites.CheckedSites sites) {
        return getMetaClass(self).finvokeChecked(context, self, sites);
    }

    // MRI: rb_check_funcall
    public static IRubyObject invokeChecked(ThreadContext context, IRubyObject self, String name, IRubyObject... args) {
        return getMetaClass(self).finvokeChecked(context, self, name, args);
    }

    // MRI: rb_check_funcall
    public static IRubyObject invokeChecked(ThreadContext context, IRubyObject self, JavaSites.CheckedSites sites, IRubyObject arg0) {
        return getMetaClass(self).finvokeChecked(context, self, sites, arg0);
    }

    // MRI: rb_check_funcall
    public static IRubyObject invokeChecked(ThreadContext context, IRubyObject self, JavaSites.CheckedSites sites, IRubyObject... args) {
        return getMetaClass(self).finvokeChecked(context, self, sites, args);
    }

    /**
     * The protocol for super method invocation is a bit complicated
     * in Ruby. In real terms it involves first finding the real
     * implementation class (the super class), getting the name of the
     * method to call from the frame, and then invoke that on the
     * super class with the current self as the actual object
     * invoking.
     */
    public static IRubyObject invokeSuper(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        return invokeSuper(context, self, context.getFrameKlazz(), context.getFrameName(), args, block);
    }

    public static String getSuperNameFromCompositeName(String compositeName) {
        if (compositeName != null && compositeName.length() > 0 && compositeName.charAt(0) == '\0') {
            return compositeName.substring(compositeName.lastIndexOf('\0') + 1);
        }

        return compositeName;
    }

    public static String getCalleeNameFromCompositeName(String compositeName) {
        if (compositeName != null && compositeName.length() > 0 && compositeName.charAt(0) == '\0') {
            return compositeName.substring(1, compositeName.lastIndexOf('\0'));
        }

        return compositeName;
    }

    public static IRubyObject invokeSuper(ThreadContext context, IRubyObject self, RubyModule klass, String name, IRubyObject[] args, Block block) {
        checkSuperDisabledOrOutOfMethod(context, klass, name);

        RubyClass selfClass = getMetaClass(self);
        RubyClass superClass = klass.getSuperClass();
        CacheEntry entry = superClass != null ? superClass.searchWithCache(name) : CacheEntry.NULL_CACHE;
        DynamicMethod method = entry.method;

        if (method.isUndefined()) {
            return callMethodMissing(context, self, selfClass, method.getVisibility(), name, CallType.SUPER, args, block);
        }
        return method.call(context, self, entry.sourceModule, name, args, block);
    }

    public static IRubyObject invokeSuper(ThreadContext context, IRubyObject self, RubyModule klass, String name, IRubyObject arg0, Block block) {
        checkSuperDisabledOrOutOfMethod(context, klass, name);

        RubyClass selfClass = getMetaClass(self);
        RubyClass superClass = klass.getSuperClass();
        CacheEntry entry = superClass != null ? superClass.searchWithCache(name) : CacheEntry.NULL_CACHE;
        DynamicMethod method = entry.method;

        if (method.isUndefined()) {
            return callMethodMissing(context, self, selfClass, method.getVisibility(), name, CallType.SUPER, arg0, block);
        }
        return method.call(context, self, entry.sourceModule, name, arg0, block);
    }

    public static IRubyObject invokeSuper(ThreadContext context, IRubyObject self, Block block) {
        checkSuperDisabledOrOutOfMethod(context);
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();

        RubyClass selfClass = getMetaClass(self);
        RubyClass superClass = klazz.getSuperClass();
        CacheEntry entry = superClass != null ? superClass.searchWithCache(name) : CacheEntry.NULL_CACHE;
        DynamicMethod method = entry.method;

        if (method.isUndefined()) {
            return callMethodMissing(context, self, selfClass, method.getVisibility(), name, CallType.SUPER, block);
        }
        return method.call(context, self, entry.sourceModule, name, block);
    }

    public static IRubyObject invokeSuper(ThreadContext context, IRubyObject self, IRubyObject arg0, Block block) {
        checkSuperDisabledOrOutOfMethod(context);
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();

        RubyClass selfClass = getMetaClass(self);
        RubyClass superClass = klazz.getSuperClass();
        CacheEntry entry = superClass != null ? superClass.searchWithCache(name) : CacheEntry.NULL_CACHE;
        DynamicMethod method = entry.method;

        if (method.isUndefined()) {
            return callMethodMissing(context, self, selfClass, method.getVisibility(), name, CallType.SUPER, arg0, block);
        }
        return method.call(context, self, entry.sourceModule, name, arg0, block);
    }

    public static IRubyObject invokeSuper(ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1, Block block) {
        checkSuperDisabledOrOutOfMethod(context);
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();

        RubyClass selfClass = getMetaClass(self);
        RubyClass superClass = klazz.getSuperClass();
        CacheEntry entry = superClass != null ? superClass.searchWithCache(name) : CacheEntry.NULL_CACHE;
        DynamicMethod method = entry.method;

        if (method.isUndefined()) {
            return callMethodMissing(context, self, selfClass, method.getVisibility(), name, CallType.SUPER, arg0, arg1, block);
        }
        return method.call(context, self, entry.sourceModule, name, arg0, arg1, block);
    }

    public static IRubyObject invokeSuper(ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        checkSuperDisabledOrOutOfMethod(context);
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();

        RubyClass selfClass = getMetaClass(self);
        RubyClass superClass = klazz.getSuperClass();
        CacheEntry entry = superClass != null ? superClass.searchWithCache(name) : CacheEntry.NULL_CACHE;
        DynamicMethod method = entry.method;

        if (method.isUndefined()) {
            return callMethodMissing(context, self, selfClass, method.getVisibility(), name, CallType.SUPER, arg0, arg1, arg2, block);
        }
        return method.call(context, self, entry.sourceModule, name, arg0, arg1, arg2, block);
    }

    @Deprecated
    public static RubyArray ensureRubyArray(IRubyObject value) {
        return ensureRubyArray(value.getRuntime(), value);
    }

    public static RubyArray ensureRubyArray(Ruby runtime, IRubyObject value) {
        return value instanceof RubyArray ? (RubyArray)value : RubyArray.newArray(runtime, value);
    }

    @Deprecated // not used
    public static IRubyObject nullToNil(IRubyObject value, ThreadContext context) {
        return value != null ? value : context.nil;
    }

    @Deprecated // not used
    public static IRubyObject nullToNil(IRubyObject value, Ruby runtime) {
        return value != null ? value : runtime.getNil();
    }

    /**
     * @see Ruby#getNullToNilHandle()
     */
    public static IRubyObject nullToNil(IRubyObject value, IRubyObject nil) {
        return value != null ? value : nil;
    }

    public static IRubyObject nullToUndefined(IRubyObject value) {
        return value != null ? value : UndefinedValue.UNDEFINED;
    }

    public static void handleArgumentSizes(ThreadContext context, Ruby runtime, int given, int required, int opt, int rest) {
        if (opt == 0) {
            if (rest < 0) {
                // no opt, no rest, exact match
                if (given != required) throw argumentError(context, given, required);
            } else {
                // only rest, must be at least required
                if (given < required) throw argumentError(context, given, required);
            }
        } else {
            if (rest < 0) {
                // opt but no rest, must be at least required and no more than required + opt
                if (given < required) throw argumentError(context, given, required);
                if (given > (required + opt)) throw argumentError(context, given, required + opt);
            } else {
                // opt and rest, must be at least required
                if (given < required) throw argumentError(context, given, required);
            }
        }
    }

    public static String getLocalJumpTypeOrRethrow(RaiseException re) {
        RubyException exception = re.getException();
        Ruby runtime = exception.getRuntime();
        if (runtime.getLocalJumpError().isInstance(exception)) {
            RubyLocalJumpError jumpError = (RubyLocalJumpError)re.getException();

            IRubyObject reason = jumpError.reason();

            return reason.asJavaString();
        }

        throw re;
    }

    public static IRubyObject unwrapLocalJumpErrorValue(RaiseException re) {
        return ((RubyLocalJumpError)re.getException()).exit_value();
    }

    public static Block getBlockFromBlockPassBody(Ruby runtime, IRubyObject proc, Block currentBlock) {
        // No block from a nil proc
        if (proc.isNil()) return Block.NULL_BLOCK;

        // If not already a proc then we should try and make it one.
        if (!(proc instanceof RubyProc)) {
            proc = coerceProc(proc, runtime);
        }

        return getBlockFromProc(currentBlock, proc);
    }

    private static IRubyObject coerceProc(IRubyObject maybeProc, Ruby runtime) throws RaiseException {
        IRubyObject proc = TypeConverter.convertToType(maybeProc, runtime.getProc(), "to_proc", false);

        if (!(proc instanceof RubyProc)) throw typeError(runtime.getCurrentContext(), maybeProc, "Proc");

        return proc;
    }

    private static Block getBlockFromProc(Block currentBlock, IRubyObject proc) {
        if (currentBlock != null && currentBlock.isGiven()) {
            RubyProc procObject = currentBlock.getProcObject();
            // The current block is already associated with proc.  No need to create a new one
            if (procObject != null && procObject == proc) {
                return currentBlock;
            }
        }

        return ((RubyProc) proc).getBlock();
    }

    @JIT
    public static Block getImplicitBlockFromBlockBinding(Block block) {
        return block.getFrame().getBlock();
    }

    public static Block getBlockFromBlockPassBody(IRubyObject proc, Block currentBlock) {
        return getBlockFromBlockPassBody(proc.getRuntime(), proc, currentBlock);
    }

    @Deprecated
    public static IRubyObject backrefLastMatch(ThreadContext context) {
        IRubyObject backref = context.getBackRef();

        return RubyRegexp.last_match(backref);
    }

    @Deprecated
    public static IRubyObject backrefMatchPre(ThreadContext context) {
        IRubyObject backref = context.getBackRef();

        return RubyRegexp.match_pre(backref);
    }

    @Deprecated
    public static IRubyObject backrefMatchPost(ThreadContext context) {
        IRubyObject backref = context.getBackRef();

        return RubyRegexp.match_post(backref);
    }

    @Deprecated
    public static IRubyObject backrefMatchLast(ThreadContext context) {
        IRubyObject backref = context.getBackRef();

        return RubyRegexp.match_last(backref);
    }

    public static IRubyObject[] appendToObjectArray(IRubyObject[] array, IRubyObject add) {
        return ArraySupport.newCopy(array, add);
    }

    public static IRubyObject breakLocalJumpError(Ruby runtime, IRubyObject value) {
        throw newLocalJumpErrorForBreak(runtime, value);
    }

    public static RaiseException newLocalJumpErrorForBreak(Ruby runtime, IRubyObject breakValue) {
        return runtime.newLocalJumpError(RubyLocalJumpError.Reason.BREAK, breakValue, "unexpected break");
    }

    public static IRubyObject[] concatObjectArrays(IRubyObject[] array, IRubyObject[] add) {
        return toArray(array, add);
    }

    public static IRubyObject[] toArray(IRubyObject[] array, IRubyObject... rest) {
        final int len = array.length;
        IRubyObject[] newArray = new IRubyObject[len + rest.length];
        ArraySupport.copy(array, newArray, 0, len);
        ArraySupport.copy(rest, newArray, len, rest.length);
        return newArray;
    }

    public static IRubyObject[] toArray(IRubyObject obj, IRubyObject... rest) {
        return ArraySupport.newCopy(obj, rest);
    }

    public static IRubyObject[] toArray(IRubyObject obj0, IRubyObject obj1, IRubyObject... rest) {
        IRubyObject[] newArray = new IRubyObject[2 + rest.length];
        newArray[0] = obj0;
        newArray[1] = obj1;
        ArraySupport.copy(rest, newArray, 2, rest.length);
        return newArray;
    }

    public static IRubyObject[] toArray(IRubyObject obj0, IRubyObject obj1, IRubyObject obj2, IRubyObject... rest) {
        IRubyObject[] newArray = new IRubyObject[3 + rest.length];
        newArray[0] = obj0;
        newArray[1] = obj1;
        newArray[2] = obj2;
        ArraySupport.copy(rest, newArray, 3, rest.length);
        return newArray;
    }

    public static IRubyObject isExceptionHandled(RubyException currentException, IRubyObject[] exceptions, ThreadContext context) {
        for (int i = 0; i < exceptions.length; i++) {
            IRubyObject result = isExceptionHandled(currentException, exceptions[i], context);
            if (result.isTrue()) return result;
        }
        return context.fals;
    }

    public static IRubyObject isExceptionHandled(RubyException currentException, IRubyObject exception, ThreadContext context) {
        return isExceptionHandled((IRubyObject) currentException, exception, context);
    }

    public static IRubyObject isExceptionHandled(IRubyObject currentException, IRubyObject exception, ThreadContext context) {
        if (!moduleClass(context).isInstance(exception)) throw typeError(context, "class or module required for rescue clause");

        IRubyObject result = invoke(context, exception, "===", currentException);
        return result.isTrue() ? result : context.fals;
    }

    public static IRubyObject isExceptionHandled(RubyException currentException, IRubyObject exception0, IRubyObject exception1, ThreadContext context) {
        IRubyObject result = isExceptionHandled(currentException, exception0, context);
        if (result.isTrue()) return result;
        return isExceptionHandled(currentException, exception1, context);
    }

    public static IRubyObject isExceptionHandled(RubyException currentException, IRubyObject exception0, IRubyObject exception1, IRubyObject exception2, ThreadContext context) {
        IRubyObject result = isExceptionHandled(currentException, exception0, context);
        if (result.isTrue()) return result;
        return isExceptionHandled(currentException, exception1, exception2, context);
    }

    public static boolean checkJavaException(final IRubyObject wrappedEx, final Throwable ex, IRubyObject catchable, ThreadContext context) {
        if (
                // rescue exception needs to catch Java exceptions
                exceptionClass(context) == catchable ||

                // rescue Object needs to catch Java exceptions
                objectClass(context) == catchable ||

                // rescue StandardError needs to catch Java exceptions
                context.runtime.getStandardError() == catchable) {

            if (ex instanceof RaiseException raise) {
                return isExceptionHandled(raise.getException(), catchable, context).isTrue();
            }

            // let Ruby exceptions decide if they handle it
            return isExceptionHandled(wrappedEx, catchable, context).isTrue();
        }

        if (context.runtime.getNativeException() == catchable) {
            // NativeException catches Java exceptions, lazily creating the wrapper
            return true;
        }

        if (catchable instanceof RubyClass cat && Java.isProxyType(cat)) {
            if ( ex instanceof ReifiedJavaProxy ) { // Ruby sub-class of a Java exception type
                final IRubyObject target = ((ReifiedJavaProxy) ex).___jruby$rubyObject();
                if ( target != null ) return cat.isInstance(target);
            }
            return cat.isInstance(wrappedEx);
        }

        if (catchable instanceof RubyModule) {
            IRubyObject result = invoke(context, catchable, "===", wrappedEx);
            return result.isTrue();
        }

        return false;
    }

    public static boolean checkJavaException(final Throwable ex, IRubyObject catchable, ThreadContext context) {
        return checkJavaException(wrapJavaException(context.runtime, ex), ex, catchable, context);
    }

    // wrapJavaException(runtime, ex)

    public static IRubyObject wrapJavaException(final Ruby runtime, final Throwable ex) {
        return JavaUtil.convertJavaToUsableRubyObject(runtime, ex);
    }

    @Deprecated // due deprecated checkJavaException
    public static IRubyObject isJavaExceptionHandled(Throwable currentThrowable, IRubyObject[] throwables, ThreadContext context) {
        if (currentThrowable instanceof Unrescuable) {
            throwException(currentThrowable);
        }

        if (currentThrowable instanceof RaiseException) {
            return isExceptionHandled(((RaiseException) currentThrowable).getException(), throwables, context);
        } else {
            if (throwables.length == 0) {
                // no rescue means StandardError, which rescues Java exceptions
                return context.tru;
            } else {
                for (int i = 0; i < throwables.length; i++) {
                    if (checkJavaException(currentThrowable, throwables[i], context)) {
                        return context.tru;
                    }
                }
            }

            return context.fals;
        }
    }

    @Deprecated // due deprecated checkJavaException
    public static IRubyObject isJavaExceptionHandled(Throwable currentThrowable, IRubyObject throwable, ThreadContext context) {
        if (currentThrowable instanceof Unrescuable) {
            throwException(currentThrowable);
        }

        if (currentThrowable instanceof RaiseException) {
            return isExceptionHandled(((RaiseException) currentThrowable).getException(), throwable, context);
        } else {
            if (checkJavaException(currentThrowable, throwable, context)) {
                return context.tru;
            }

            return context.fals;
        }
    }

    @Deprecated // due deprecated checkJavaException
    public static IRubyObject isJavaExceptionHandled(Throwable currentThrowable, IRubyObject throwable0, IRubyObject throwable1, ThreadContext context) {
        if (currentThrowable instanceof Unrescuable) {
            throwException(currentThrowable);
        }

        if (currentThrowable instanceof RaiseException) {
            return isExceptionHandled(((RaiseException)currentThrowable).getException(), throwable0, throwable1, context);
        } else {
            if (checkJavaException(currentThrowable, throwable0, context)) {
                return context.tru;
            }
            if (checkJavaException(currentThrowable, throwable1, context)) {
                return context.tru;
            }

            return context.fals;
        }
    }

    @Deprecated // due deprecated checkJavaException
    public static IRubyObject isJavaExceptionHandled(Throwable currentThrowable, IRubyObject throwable0, IRubyObject throwable1, IRubyObject throwable2, ThreadContext context) {
        if (currentThrowable instanceof Unrescuable) {
            throwException(currentThrowable);
        }

        if (currentThrowable instanceof RaiseException) {
            return isExceptionHandled(((RaiseException)currentThrowable).getException(), throwable0, throwable1, throwable2, context);
        } else {
            if (checkJavaException(currentThrowable, throwable0, context)) {
                return context.tru;
            }
            if (checkJavaException(currentThrowable, throwable1, context)) {
                return context.tru;
            }
            if (checkJavaException(currentThrowable, throwable2, context)) {
                return context.tru;
            }

            return context.fals;
        }
    }

    @Deprecated
    public static void storeExceptionInErrorInfo(Throwable currentThrowable, ThreadContext context) {
        IRubyObject exception;
        if (currentThrowable instanceof RaiseException) {
            exception = ((RaiseException)currentThrowable).getException();
        } else {
            exception = JavaUtil.convertJavaToUsableRubyObject(context.runtime, currentThrowable);
        }
        context.setErrorInfo(exception);
    }

    public static void clearErrorInfo(ThreadContext context) {
        context.setErrorInfo(context.nil);
    }

    public static void checkSuperDisabledOrOutOfMethod(ThreadContext context) {
        checkSuperDisabledOrOutOfMethod(context, context.getFrameKlazz(), context.getFrameName());
    }

    public static void checkSuperDisabledOrOutOfMethod(ThreadContext context, RubyModule klass, String name) {
        if (klass == null) {
            if (name != null) {
                Ruby runtime = context.runtime;
                throw runtime.newNameError(str(runtime, "superclass method '", ids(runtime, name), "' disabled"), name);
            }
        }
        if (name == null) {
            throw context.runtime.newNoMethodError("super called outside of method", null, context.nil);
        }
    }

    public static RubyModule findImplementerIfNecessary(RubyModule clazz, RubyModule implementationClass) {
        if (implementationClass.needsImplementer()) {
            // modules are included with a shim class; we must find that shim to handle super() appropriately
            return clazz.findImplementer(implementationClass);
        } else {
            // method is directly in a class, so just ensure we don't use any prepends
            return implementationClass.getMethodLocation();
        }
    }

    public static RubyArray createSubarray(RubyArray input, int start) {
        return (RubyArray)input.subseqLight(start, input.size() - start);
    }

    public static RubyArray createSubarray(RubyArray input, int start, int post) {
        return (RubyArray)input.subseqLight(start, input.size() - post - start);
    }

    public static RubyArray createSubarray(IRubyObject[] input, Ruby runtime, int start) {
        if (start >= input.length) {
            return RubyArray.newEmptyArray(runtime);
        } else {
            return RubyArray.newArrayMayCopy(runtime, input, start);
        }
    }

    public static RubyArray createSubarray(IRubyObject[] input, Ruby runtime, int start, int exclude) {
        int length = input.length - exclude - start;
        if (length <= 0) {
            return RubyArray.newEmptyArray(runtime);
        } else {
            return RubyArray.newArrayMayCopy(runtime, input, start, length);
        }
    }

    public static IRubyObject elementOrNull(IRubyObject[] input, int element) {
        if (element >= input.length) {
            return null;
        } else {
            return input[element];
        }
    }

    public static IRubyObject optElementOrNull(IRubyObject[] input, int element, int postCount) {
        if (element + postCount >= input.length) {
            return null;
        } else {
            return input[element];
        }
    }

    public static IRubyObject elementOrNil(IRubyObject[] input, int element, IRubyObject nil) {
        if (element >= input.length) {
            return nil;
        } else {
            return input[element];
        }
    }

    public static IRubyObject setConstantInModule(ThreadContext context, String name, IRubyObject value, IRubyObject module) {
        if (!(module instanceof RubyModule)) {
            throw typeError(context, str(context.runtime, ids(context.runtime, module), " is not a class/module"));
        }
        ((RubyModule) module).setConstant(name, value);

        return value;
    }

    public static final int MAX_SPECIFIC_ARITY_OBJECT_ARRAY = 10;

    public static IRubyObject[] anewarrayIRubyObjects(int size) {
        return new IRubyObject[size];
    }

    public static IRubyObject[] aastoreIRubyObjects(IRubyObject[] ary, IRubyObject one, int start) {
        ary[start] = one;
        return ary;
    }

    public static IRubyObject[] aastoreIRubyObjects(IRubyObject[] ary, IRubyObject one, IRubyObject two, int start) {
        ary[start] = one;
        ary[start+1] = two;
        return ary;
    }

    public static IRubyObject[] aastoreIRubyObjects(IRubyObject[] ary, IRubyObject one, IRubyObject two, IRubyObject three, int start) {
        ary[start] = one;
        ary[start+1] = two;
        ary[start+2] = three;
        return ary;
    }

    public static IRubyObject[] aastoreIRubyObjects(IRubyObject[] ary, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, int start) {
        ary[start] = one;
        ary[start+1] = two;
        ary[start+2] = three;
        ary[start+3] = four;
        return ary;
    }

    public static IRubyObject[] aastoreIRubyObjects(IRubyObject[] ary, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, int start) {
        ary[start] = one;
        ary[start+1] = two;
        ary[start+2] = three;
        ary[start+3] = four;
        ary[start+4] = five;
        return ary;
    }

    public static IRubyObject[] aastoreIRubyObjects(IRubyObject[] ary, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, int start) {
        ary[start] = one;
        ary[start+1] = two;
        ary[start+2] = three;
        ary[start+3] = four;
        ary[start+4] = five;
        ary[start+5] = six;
        return ary;
    }

    public static IRubyObject[] aastoreIRubyObjects(IRubyObject[] ary, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, IRubyObject seven, int start) {
        ary[start] = one;
        ary[start+1] = two;
        ary[start+2] = three;
        ary[start+3] = four;
        ary[start+4] = five;
        ary[start+5] = six;
        ary[start+6] = seven;
        return ary;
    }

    public static IRubyObject[] aastoreIRubyObjects(IRubyObject[] ary, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, IRubyObject seven, IRubyObject eight, int start) {
        ary[start] = one;
        ary[start+1] = two;
        ary[start+2] = three;
        ary[start+3] = four;
        ary[start+4] = five;
        ary[start+5] = six;
        ary[start+6] = seven;
        ary[start+7] = eight;
        return ary;
    }

    public static IRubyObject[] aastoreIRubyObjects(IRubyObject[] ary, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, IRubyObject seven, IRubyObject eight, IRubyObject nine, int start) {
        ary[start] = one;
        ary[start+1] = two;
        ary[start+2] = three;
        ary[start+3] = four;
        ary[start+4] = five;
        ary[start+5] = six;
        ary[start+6] = seven;
        ary[start+7] = eight;
        ary[start+8] = nine;
        return ary;
    }

    public static IRubyObject[] aastoreIRubyObjects(IRubyObject[] ary, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, IRubyObject seven, IRubyObject eight, IRubyObject nine, IRubyObject ten, int start) {
        ary[start] = one;
        ary[start+1] = two;
        ary[start+2] = three;
        ary[start+3] = four;
        ary[start+4] = five;
        ary[start+5] = six;
        ary[start+6] = seven;
        ary[start+7] = eight;
        ary[start+8] = nine;
        ary[start+9] = ten;
        return ary;
    }

    public static IRubyObject[] constructObjectArray(IRubyObject one) {
        return new IRubyObject[] {one};
    }

    public static IRubyObject[] constructObjectArray(IRubyObject one, IRubyObject two) {
        return new IRubyObject[] {one, two};
    }

    public static IRubyObject[] constructObjectArray(IRubyObject one, IRubyObject two, IRubyObject three) {
        return new IRubyObject[] {one, two, three};
    }

    public static IRubyObject[] constructObjectArray(IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four) {
        return new IRubyObject[] {one, two, three, four};
    }

    public static IRubyObject[] constructObjectArray(IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five) {
        return new IRubyObject[] {one, two, three, four, five};
    }

    public static IRubyObject[] constructObjectArray(IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six) {
        return new IRubyObject[] {one, two, three, four, five, six};
    }

    public static IRubyObject[] constructObjectArray(IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, IRubyObject seven) {
        return new IRubyObject[] {one, two, three, four, five, six, seven};
    }

    public static IRubyObject[] constructObjectArray(IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, IRubyObject seven, IRubyObject eight) {
        return new IRubyObject[] {one, two, three, four, five, six, seven, eight};
    }

    public static IRubyObject[] constructObjectArray(IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, IRubyObject seven, IRubyObject eight, IRubyObject nine) {
        return new IRubyObject[] {one, two, three, four, five, six, seven, eight, nine};
    }

    public static IRubyObject[] constructObjectArray(IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, IRubyObject seven, IRubyObject eight, IRubyObject nine, IRubyObject ten) {
        return new IRubyObject[] {one, two, three, four, five, six, seven, eight, nine, ten};
    }

    private static final MethodHandle[] constructObjectArrayHandles = new MethodHandle[11];

    public static MethodHandle constructObjectArrayHandle(int size) {
        if (size < 0) throw new IllegalArgumentException("illegal size: " + size);

        if (size > 10) {
            return Binder
                    .from(IRubyObject[].class, params(IRubyObject.class, size))
                    .collect(0, IRubyObject[].class).identity();
        }

        MethodHandle handle = constructObjectArrayHandles[size];

        if (handle == null) {
            try {
                if (size == 0) {
                    handle = Binder.from(IRubyObject[].class).getStatic(LOOKUP, IRubyObject.class, "NULL_ARRAY");
                } else {
                    handle = MethodHandles.lookup().findStatic(Helpers.class, "constructObjectArray", MethodType.methodType(IRubyObject[].class, CodegenUtils.params(IRubyObject.class, size)));
                }

                constructObjectArrayHandles[size] = handle;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return handle;
    }
    
    public static RubyString[] constructRubyStringArray(RubyString one) {
        return new RubyString[] {one};
    }

    public static RubyString[] constructRubyStringArray(RubyString one, RubyString two) {
        return new RubyString[] {one, two};
    }

    public static RubyString[] constructRubyStringArray(RubyString one, RubyString two, RubyString three) {
        return new RubyString[] {one, two, three};
    }

    public static RubyString[] constructRubyStringArray(RubyString one, RubyString two, RubyString three, RubyString four) {
        return new RubyString[] {one, two, three, four};
    }

    public static RubyString[] constructRubyStringArray(RubyString one, RubyString two, RubyString three, RubyString four, RubyString five) {
        return new RubyString[] {one, two, three, four, five};
    }

    public static RubyString[] constructRubyStringArray(RubyString one, RubyString two, RubyString three, RubyString four, RubyString five, RubyString six) {
        return new RubyString[] {one, two, three, four, five, six};
    }

    public static RubyString[] constructRubyStringArray(RubyString one, RubyString two, RubyString three, RubyString four, RubyString five, RubyString six, RubyString seven) {
        return new RubyString[] {one, two, three, four, five, six, seven};
    }

    public static RubyString[] constructRubyStringArray(RubyString one, RubyString two, RubyString three, RubyString four, RubyString five, RubyString six, RubyString seven, RubyString eight) {
        return new RubyString[] {one, two, three, four, five, six, seven, eight};
    }

    public static RubyString[] constructRubyStringArray(RubyString one, RubyString two, RubyString three, RubyString four, RubyString five, RubyString six, RubyString seven, RubyString eight, RubyString nine) {
        return new RubyString[] {one, two, three, four, five, six, seven, eight, nine};
    }

    public static RubyString[] constructRubyStringArray(RubyString one, RubyString two, RubyString three, RubyString four, RubyString five, RubyString six, RubyString seven, RubyString eight, RubyString nine, RubyString ten) {
        return new RubyString[] {one, two, three, four, five, six, seven, eight, nine, ten};
    }

    private static final MethodHandle[] constructRubyStringArrayHandles = new MethodHandle[11];

    public static MethodHandle constructRubyStringArrayHandle(int size) {
        if (size < 0) throw new IllegalArgumentException("illegal size: " + size);

        if (size > 10) {
            return Binder
                    .from(RubyString[].class, params(RubyString.class, size))
                    .collect(0, RubyString[].class).identity();
        }

        MethodHandle handle = constructRubyStringArrayHandles[size];

        if (handle == null) {
            try {
                if (size == 0) {
                    handle = Binder.from(RubyString[].class).getStatic(LOOKUP, RubyString.class, "NULL_ARRAY");
                } else {
                    handle = MethodHandles.lookup().findStatic(Helpers.class, "constructRubyStringArray", MethodType.methodType(RubyString[].class, CodegenUtils.params(RubyString.class, size)));
                }

                constructRubyStringArrayHandles[size] = handle;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return handle;
    }

    public static RubyArray constructRubyArray(Ruby runtime, IRubyObject one) {
        return RubyArray.newArrayLight(runtime, one);
    }

    public static RubyArray constructRubyArray(Ruby runtime, IRubyObject one, IRubyObject two) {
        return RubyArray.newArrayLight(runtime, one, two);
    }

    public static RubyArray constructRubyArray(Ruby runtime, IRubyObject one, IRubyObject two, IRubyObject three) {
        return RubyArray.newArrayLight(runtime, one, two, three);
    }

    public static RubyArray constructRubyArray(Ruby runtime, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four) {
        return RubyArray.newArrayLight(runtime, one, two, three, four);
    }

    public static RubyArray constructRubyArray(Ruby runtime, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five) {
        return RubyArray.newArrayLight(runtime, one, two, three, four, five);
    }

    public static RubyArray constructRubyArray(Ruby runtime, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six) {
        return RubyArray.newArrayLight(runtime, one, two, three, four, five, six);
    }

    public static RubyArray constructRubyArray(Ruby runtime, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, IRubyObject seven) {
        return RubyArray.newArrayLight(runtime, one, two, three, four, five, six, seven);
    }

    public static RubyArray constructRubyArray(Ruby runtime, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, IRubyObject seven, IRubyObject eight) {
        return RubyArray.newArrayLight(runtime, one, two, three, four, five, six, seven, eight);
    }

    public static RubyArray constructRubyArray(Ruby runtime, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, IRubyObject seven, IRubyObject eight, IRubyObject nine) {
        return RubyArray.newArrayLight(runtime, one, two, three, four, five, six, seven, eight, nine);
    }

    public static RubyArray constructRubyArray(Ruby runtime, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, IRubyObject seven, IRubyObject eight, IRubyObject nine, IRubyObject ten) {
        return RubyArray.newArrayLight(runtime, one, two, three, four, five, six, seven, eight, nine, ten);
    }

    public static String[] constructStringArray(String one) {
        return new String[] {one};
    }

    public static String[] constructStringArray(String one, String two) {
        return new String[] {one, two};
    }

    public static String[] constructStringArray(String one, String two, String three) {
        return new String[] {one, two, three};
    }

    public static String[] constructStringArray(String one, String two, String three, String four) {
        return new String[] {one, two, three, four};
    }

    public static String[] constructStringArray(String one, String two, String three, String four, String five) {
        return new String[] {one, two, three, four, five};
    }

    public static String[] constructStringArray(String one, String two, String three, String four, String five, String six) {
        return new String[] {one, two, three, four, five, six};
    }

    public static String[] constructStringArray(String one, String two, String three, String four, String five, String six, String seven) {
        return new String[] {one, two, three, four, five, six, seven};
    }

    public static String[] constructStringArray(String one, String two, String three, String four, String five, String six, String seven, String eight) {
        return new String[] {one, two, three, four, five, six, seven, eight};
    }

    public static String[] constructStringArray(String one, String two, String three, String four, String five, String six, String seven, String eight, String nine) {
        return new String[] {one, two, three, four, five, six, seven, eight, nine};
    }

    public static String[] constructStringArray(String one, String two, String three, String four, String five, String six, String seven, String eight, String nine, String ten) {
        return new String[] {one, two, three, four, five, six, seven, eight, nine, ten};
    }

    public static final int MAX_SPECIFIC_ARITY_HASH = 5;

    public static RubyHash constructHash(Ruby runtime,
                                         IRubyObject key1, IRubyObject value1, boolean prepareString1) {
        RubyHash hash = RubyHash.newHash(runtime);
        hash.fastASet(runtime, key1, value1, prepareString1);
        return hash;
    }

    public static RubyHash constructHash(Ruby runtime,
                                         IRubyObject key1, IRubyObject value1, boolean prepareString1,
                                         IRubyObject key2, IRubyObject value2, boolean prepareString2) {
        RubyHash hash = RubyHash.newHash(runtime);
        hash.fastASet(runtime, key1, value1, prepareString1);
        hash.fastASet(runtime, key2, value2, prepareString2);
        return hash;
    }

    public static RubyHash constructHash(Ruby runtime,
                                         IRubyObject key1, IRubyObject value1, boolean prepareString1,
                                         IRubyObject key2, IRubyObject value2, boolean prepareString2,
                                         IRubyObject key3, IRubyObject value3, boolean prepareString3) {
        RubyHash hash = RubyHash.newHash(runtime);
        hash.fastASet(runtime, key1, value1, prepareString1);
        hash.fastASet(runtime, key2, value2, prepareString2);
        hash.fastASet(runtime, key3, value3, prepareString3);
        return hash;
    }

    public static RubyHash constructHash(Ruby runtime,
                                         IRubyObject key1, IRubyObject value1, boolean prepareString1,
                                         IRubyObject key2, IRubyObject value2, boolean prepareString2,
                                         IRubyObject key3, IRubyObject value3, boolean prepareString3,
                                         IRubyObject key4, IRubyObject value4, boolean prepareString4) {
        RubyHash hash = RubyHash.newHash(runtime);
        hash.fastASet(runtime, key1, value1, prepareString1);
        hash.fastASet(runtime, key2, value2, prepareString2);
        hash.fastASet(runtime, key3, value3, prepareString3);
        hash.fastASet(runtime, key4, value4, prepareString4);
        return hash;
    }

    public static RubyHash constructHash(Ruby runtime,
                                         IRubyObject key1, IRubyObject value1, boolean prepareString1,
                                         IRubyObject key2, IRubyObject value2, boolean prepareString2,
                                         IRubyObject key3, IRubyObject value3, boolean prepareString3,
                                         IRubyObject key4, IRubyObject value4, boolean prepareString4,
                                         IRubyObject key5, IRubyObject value5, boolean prepareString5) {
        RubyHash hash = RubyHash.newHash(runtime);
        hash.fastASet(runtime, key1, value1, prepareString1);
        hash.fastASet(runtime, key2, value2, prepareString2);
        hash.fastASet(runtime, key3, value3, prepareString3);
        hash.fastASet(runtime, key4, value4, prepareString4);
        hash.fastASet(runtime, key5, value5, prepareString5);
        return hash;
    }

    public static RubyHash constructSmallHash(Ruby runtime,
                                              IRubyObject key1, IRubyObject value1) {
        RubyHash hash = RubyHash.newSmallHash(runtime);
        hash.fastASetSmallCheckString(runtime, key1, value1);
        return hash;
    }

    public static RubyHash constructSmallHash(Ruby runtime,
                                              IRubyObject key1, IRubyObject value1,
                                              IRubyObject key2, IRubyObject value2) {
        RubyHash hash = RubyHash.newSmallHash(runtime);
        hash.fastASetSmallCheckString(runtime, key1, value1);
        hash.fastASetSmallCheckString(runtime, key2, value2);
        return hash;
    }

    public static RubyHash constructSmallHash(Ruby runtime,
                                              IRubyObject key1, IRubyObject value1,
                                              IRubyObject key2, IRubyObject value2,
                                              IRubyObject key3, IRubyObject value3) {
        RubyHash hash = RubyHash.newSmallHash(runtime);
        hash.fastASetSmallCheckString(runtime, key1, value1);
        hash.fastASetSmallCheckString(runtime, key2, value2);
        hash.fastASetSmallCheckString(runtime, key3, value3);
        return hash;
    }

    public static RubyHash constructSmallHash(Ruby runtime,
                                              IRubyObject key1, IRubyObject value1,
                                              IRubyObject key2, IRubyObject value2,
                                              IRubyObject key3, IRubyObject value3,
                                              IRubyObject key4, IRubyObject value4) {
        RubyHash hash = RubyHash.newSmallHash(runtime);
        hash.fastASetSmallCheckString(runtime, key1, value1);
        hash.fastASetSmallCheckString(runtime, key2, value2);
        hash.fastASetSmallCheckString(runtime, key3, value3);
        hash.fastASetSmallCheckString(runtime, key4, value4);
        return hash;
    }

    public static RubyHash constructSmallHash(Ruby runtime,
                                              IRubyObject key1, IRubyObject value1,
                                              IRubyObject key2, IRubyObject value2,
                                              IRubyObject key3, IRubyObject value3,
                                              IRubyObject key4, IRubyObject value4,
                                              IRubyObject key5, IRubyObject value5) {
        RubyHash hash = RubyHash.newSmallHash(runtime);
        hash.fastASetSmallCheckString(runtime, key1, value1);
        hash.fastASetSmallCheckString(runtime, key2, value2);
        hash.fastASetSmallCheckString(runtime, key3, value3);
        hash.fastASetSmallCheckString(runtime, key4, value4);
        hash.fastASetSmallCheckString(runtime, key5, value5);
        return hash;
    }

    public static IRubyObject negate(IRubyObject value, Ruby runtime) {
        if (value.isTrue()) return runtime.getFalse();
        return runtime.getTrue();
    }

    @Deprecated // no-longer used + confusing argument order
    public static IRubyObject stringOrNil(ByteList value, ThreadContext context) {
        if (value == null) return context.nil;
        return RubyString.newStringShared(context.runtime, value);
    }

    @SuppressWarnings("deprecation")
    public static StaticScope preLoad(ThreadContext context, String[] varNames) {
        StaticScope staticScope = context.runtime.getStaticScopeFactory().newLocalScope(null, varNames);
        preLoadCommon(context, staticScope, false);

        return staticScope;
    }

    public static void preLoadCommon(ThreadContext context, StaticScope staticScope, boolean wrap) {
        RubyModule objectClass = wrap ? RubyModule.newModule(context.runtime) : objectClass(context);

        staticScope.setModule(objectClass);

        DynamicScope scope = DynamicScope.newDynamicScope(staticScope);

        // Each root node has a top-level scope that we need to push
        context.preScopedBody(scope);
        context.preNodeEval(context.runtime.getTopSelf());
    }

    public static void postLoad(ThreadContext context) {
        context.postNodeEval();
        context.postScopedBody();
    }

    @Deprecated // not-used
    public static void registerEndBlock(Block block, Ruby runtime) {
        runtime.pushExitBlock(runtime.newProc(Block.Type.LAMBDA, block));
    }

    @Deprecated // not-used
    public static IRubyObject match3(RubyRegexp regexp, IRubyObject value, ThreadContext context) {
        if (value instanceof RubyString) {
            return regexp.op_match(context, value);
        } else {
            return value.callMethod(context, "=~", regexp);
        }
    }

    @Deprecated // not-used
    public static IRubyObject getErrorInfo(Ruby runtime) {
        return runtime.getCurrentContext().getErrorInfo();
    }

    @Deprecated // not-used
    public static void setErrorInfo(Ruby runtime, IRubyObject error) {
        runtime.getCurrentContext().setErrorInfo(error);
    }

    public static IRubyObject setLastLine(Ruby runtime, ThreadContext context, IRubyObject value) {
        return context.setLastLine(value);
    }

    public static IRubyObject getLastLine(Ruby runtime, ThreadContext context) {
        return context.getLastLine();
    }

    public static RubyArray arrayValue(IRubyObject value) {
        return arrayValue(value.getRuntime().getCurrentContext(), value);
    }

    /**
     * @param context
     * @param runtime
     * @param value
     * @return ""
     * @deprecated Use {@link Helpers#arrayValue(ThreadContext, IRubyObject)}
     */
    @Deprecated(since = "10.0")
    public static RubyArray arrayValue(ThreadContext context, Ruby runtime, IRubyObject value) {
        return arrayValue(context, value);
    }

    public static RubyArray arrayValue(ThreadContext context, IRubyObject value) {
        IRubyObject tmp = value.checkArrayType();

        if (tmp.isNil()) {
            if (value.respondsTo("to_a")) {
                IRubyObject avalue = value.callMethod(context, "to_a");
                if (avalue instanceof RubyArray ary) return ary;
                if (avalue.isNil()) return newArray(context, value);

                throw typeError(context, "'to_a' did not return Array");
            } else {
                CacheEntry entry = value.getMetaClass().searchWithCache("method_missing");
                DynamicMethod methodMissing = entry.method;
                if (methodMissing.isUndefined() || context.runtime.isDefaultMethodMissing(methodMissing)) {
                    return newArray(context, value);
                } else {
                    IRubyObject avalue = methodMissing.call(context, value, entry.sourceModule, "to_a",
                            new IRubyObject[] {asSymbol(context, "to_a")}, Block.NULL_BLOCK);
                    if (!(avalue instanceof RubyArray)) {
                        if (avalue.isNil()) {
                            return newArray(context, value);
                        } else {
                            throw typeError(context, "'to_a' did not return Array");
                        }
                    }
                    return (RubyArray<?>)avalue;
                }
            }
        }

        return ((RubyArray<?>) tmp).aryDup();
    }

    // mri: rb_Array
    @Deprecated
    public static RubyArray asArray(ThreadContext context, IRubyObject value) {
        return TypeConverter.rb_Array(context, value);
    }

    @Deprecated // not used
    public static IRubyObject aryToAry(IRubyObject value) {
        return aryToAry(value.getRuntime().getCurrentContext(), value);
    }

    public static IRubyObject aryToAry(ThreadContext context, IRubyObject value) {
        if (value instanceof RubyArray) return value;

        return respondsTo_to_ary(value) ?
                TypeConverter.convertToTypeUnchecked(context, value, arrayClass(context), "to_ary", false) :
                newArray(context, value);
    }

    private static boolean respondsTo_to_ary(IRubyObject value) {
        try {
            return value.respondsTo("to_ary");
        } catch (NoMethodError e) {
            // A non-existent respond_to? should still end up calling method_missing but if m_m does not
            // handle it then we should not raise.
            return false;
        }
    }

    public static IRubyObject aryOrToAry(ThreadContext context, IRubyObject value) {
        if (value instanceof RubyArray) return value;

        return respondsTo_to_ary(value) ?
                TypeConverter.convertToTypeUnchecked(context, value, arrayClass(context), "to_ary", false) :
                context.nil;
    }

    @Deprecated // not used
    public static IRubyObject aValueSplat(IRubyObject value) {
        var context = value.getRuntime().getCurrentContext();
        if (!(value instanceof RubyArray array) || array.length().getLongValue() == 0) {
            return context.nil;
        }

        return array.getLength() == 1 ? array.first(context) : array;
    }

    @Deprecated(since = "9.4-") // not used
    public static RubyArray splatValue(IRubyObject value) {
        var context = value.getRuntime().getCurrentContext();
        return value.isNil() ? newArray(context, value) : arrayValue(context, value);
    }

    @Deprecated(since = "9.4-") // no longer used
    public static IRubyObject[] splatToArguments(IRubyObject value) {
        if (value.isNil()) {
            return value.getRuntime().getSingleNilArray();
        }

        IRubyObject tmp = value.checkArrayType();

        if (tmp.isNil()) {
            return convertSplatToJavaArray(value.getRuntime().getCurrentContext(), value);
        }
        return ((RubyArray)tmp).toJavaArrayMaybeUnsafe();
    }

    @Deprecated(since = "9.4-")
    private static IRubyObject[] convertSplatToJavaArray(ThreadContext context, IRubyObject value) {
        // Object#to_a is obsolete.  We match Ruby's hack until to_a goes away.  Then we can
        // remove this hack too.

        RubyClass metaClass = value.getMetaClass();
        CacheEntry entry = metaClass.searchWithCache("to_a");
        DynamicMethod method = entry.method;
        if (method.isUndefined() || method.isImplementedBy(kernelModule(context))) {
            return new IRubyObject[] {value};
        }

        IRubyObject avalue = method.call(context, value, entry.sourceModule, "to_a");
        if (!(avalue instanceof RubyArray ary)) {
            if (avalue.isNil()) return new IRubyObject[] {value};

            throw typeError(context, "'to_a' did not return Array");
        }
        return ary.toJavaArray(context);
    }

    @SuppressWarnings("deprecation") @Deprecated // no longer used
    public static IRubyObject[] argsCatToArguments(IRubyObject[] args, IRubyObject cat) {
        IRubyObject[] ary = splatToArguments(cat);
        if (ary.length > 0) {
            IRubyObject[] newArgs = new IRubyObject[args.length + ary.length];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            System.arraycopy(ary, 0, newArgs, args.length, ary.length);
            return newArgs;
        }
        return args;
    }

    @Deprecated
    public static RubySymbol addInstanceMethod(RubyModule containingClass, String name, DynamicMethod method, Visibility visibility, ThreadContext context, Ruby runtime) {
        return addInstanceMethod(containingClass, runtime.fastNewSymbol(name), method, visibility, context, runtime);
    }

    @Deprecated
    public static RubySymbol addInstanceMethod(RubyModule containingClass, RubySymbol symbol, DynamicMethod method, Visibility visibility, ThreadContext context, Ruby runtime) {
        return addInstanceMethod(containingClass, symbol, method, visibility, context);
    }

    public static RubySymbol addInstanceMethod(RubyModule containingClass, RubySymbol symbol, DynamicMethod method, Visibility visibility, ThreadContext context) {
        containingClass.addMethod(symbol.idString(), method);

        if (!containingClass.isRefinement()) callNormalMethodHook(containingClass, context, symbol);
        if (visibility == Visibility.MODULE_FUNCTION) addModuleMethod(containingClass, method, context, symbol);

        return symbol;
    }

    private static void addModuleMethod(RubyModule containingClass, DynamicMethod method, ThreadContext context, RubySymbol sym) {
        DynamicMethod singletonMethod = method.dup();
        singletonMethod.setImplementationClass(containingClass.getSingletonClass());
        singletonMethod.setVisibility(Visibility.PUBLIC);
        containingClass.getSingletonClass().addMethod(sym.idString(), singletonMethod);
        containingClass.callMethod(context, "singleton_method_added", sym);
    }

    private static void callNormalMethodHook(RubyModule containingClass, ThreadContext context, RubySymbol name) {
        // 'class << state.self' and 'class << obj' uses defn as opposed to defs
        if (containingClass.isSingleton()) {
            callSingletonMethodHook(((MetaClass) containingClass).getAttached(), context, name);
        } else {
            containingClass.callMethod(context, "method_added", name);
        }
    }

    private static void callSingletonMethodHook(RubyBasicObject receiver, ThreadContext context, RubySymbol name) {
        receiver.callMethod(context, "singleton_method_added", name);
    }

    @SuppressWarnings("deprecation")
    static String encodeScope(StaticScope scope) {
        StringBuilder namesBuilder = new StringBuilder(scope.getType().name()); // 0

        namesBuilder.append(',');  // 1

        boolean first = true;
        for (String name : scope.getVariables()) {
            if (!first) namesBuilder.append(';');
            first = false;
            namesBuilder.append(name);
        }
        namesBuilder.append(',').append(scope.getSignature().encode()); // 2
        namesBuilder.append(',').append(scope.getScopeType());          // 3

        return namesBuilder.toString();
    }

    @SuppressWarnings("deprecation")
    static StaticScope decodeScope(ThreadContext context, StaticScope parent, String scopeString) {
        String[][] decodedScope = decodeScopeDescriptor(scopeString);
        String scopeTypeName = decodedScope[0][0];
        String[] names = decodedScope[1];
        StaticScope scope = null;
        switch (StaticScope.Type.valueOf(scopeTypeName)) {
            case BLOCK:
                scope = context.runtime.getStaticScopeFactory().newBlockScope(parent, names);
                break;
            case EVAL:
                scope = context.runtime.getStaticScopeFactory().newEvalScope(parent, names);
                break;
            case LOCAL:
                scope = context.runtime.getStaticScopeFactory().newLocalScope(parent, names);
                break;
        }
        setAritiesFromDecodedScope(scope, decodedScope[0][2]);
        scope.setScopeType(IRScopeType.valueOf(decodedScope[0][3]));
        return scope;
    }

    private static String[][] decodeScopeDescriptor(String scopeString) {
        String[] scopeElements = scopeString.split(",");
        String[] scopeNames = scopeElements[1].length() == 0 ? EMPTY_STRING_ARRAY : getScopeNames(scopeElements[1]);
        return new String[][] {scopeElements, scopeNames};
    }

    private static void setAritiesFromDecodedScope(StaticScope scope, String encodedSignature) {
        scope.setSignature(Signature.decode(Long.parseLong(encodedSignature)));
    }

    public static StaticScope decodeScopeAndDetermineModule(ThreadContext context, StaticScope parent, String scopeString) {
        StaticScope scope = decodeScope(context, parent, scopeString);
        scope.determineModule();

        return scope;
    }

    public static String describeScope(StaticScope scope) {
        Signature signature = scope.getSignature();
        Collection<String> instanceVariableNames = scope.getInstanceVariableNames();
        String descriptor =
                Integer.toString(scope.getType().ordinal()) + ';'
                + scope.getFile() + ';'
                + Arrays.stream(scope.getVariables()).collect(Collectors.joining(",")) + ';'
                + scope.getFirstKeywordIndex() + ';' +
                + (signature == null ? Signature.NO_ARGUMENTS.encode() : signature.encode()) + ';'
                + scope.getIRScope().getScopeType().ordinal() + ';'
                + (instanceVariableNames.size() > 0
                        ? instanceVariableNames.stream().collect(Collectors.joining(","))
                        : "NONE");

        return descriptor;
    }

    public static StaticScope restoreScope(String descriptor, StaticScope enclosingScope) {
        String[] bits = descriptor.split(";");

        StaticScope.Type type = StaticScope.Type.fromOrdinal(Integer.parseInt(bits[0]));
        String file = bits[1];

        String[] varNames = bits[2].split(",");
        int kwIndex = Integer.parseInt(bits[3]);
        Signature signature = Signature.decode(Long.parseLong(bits[4]));
        IRScopeType scopeType = IRScopeType.fromOrdinal(Integer.parseInt(bits[5]));
        String encodedIvars = bits[6];
        Collection<String> ivarNames = encodedIvars.equals("NONE") ? Collections.EMPTY_LIST : Arrays.asList(encodedIvars.split(","));

        StaticScope scope = StaticScopeFactory.newStaticScope(enclosingScope, type, file, varNames, kwIndex);

        scope.setSignature(signature);
        scope.setScopeType(scopeType);
        scope.setInstanceVariableNames(ivarNames);

        return scope;
    }

    @Deprecated(since = "10.0")
    public static Visibility performNormalMethodChecksAndDetermineVisibility(Ruby runtime, RubyModule clazz,
                                                                             RubySymbol symbol, Visibility visibility) throws RaiseException {
        return performNormalMethodChecksAndDetermineVisibility(runtime.getCurrentContext(), clazz, symbol, visibility);
    }

    public static Visibility performNormalMethodChecksAndDetermineVisibility(ThreadContext context, RubyModule clazz,
                                                                             RubySymbol symbol, Visibility visibility) throws RaiseException {
        if (clazz == context.runtime.getDummy()) throw typeError(context, "no class/module to add method");

        switch(symbol.idString()) {
            case "__id__":
            case "__send__":
                context.runtime.getWarnings().warn(ID.REDEFINING_DANGEROUS, str(context.runtime, "redefining '", ids(context.runtime, symbol), "' may cause serious problem"));
                break;
            case "initialize":
                if (clazz == objectClass(context)) {
                    context.runtime.getWarnings().warn(ID.REDEFINING_DANGEROUS, "redefining Object#initialize may cause infinite loop");
                }
            case "initialize_copy":
            case "initialize_dup":
            case "initialize_clone":
            case "respond_to_missing?":
                visibility = Visibility.PRIVATE;
                break;
        }

        if (visibility == Visibility.MODULE_FUNCTION) {
            visibility = Visibility.PRIVATE;
        }

        return visibility;
    }

    public static RubyClass performSingletonMethodChecks(Ruby runtime, IRubyObject receiver, String name) throws RaiseException {
        if (receiver instanceof RubyFixnum || receiver instanceof RubySymbol) {
            throw typeError(runtime.getCurrentContext(), str(runtime, "can't define singleton method \"", ids(runtime, name), "\" for ", types(runtime, receiver.getMetaClass())));
        }

        if (receiver.isFrozen()) throw runtime.newFrozenError("object", receiver);

        return receiver.getSingletonClass();
    }

    @Deprecated // not used
    public static IRubyObject arrayEntryOrNil(RubyArray array, int index) {
        if (index < array.getLength()) {
            return array.eltInternal(index);
        } else {
            return array.getRuntime().getNil();
        }
    }

    @Deprecated // not used
    public static IRubyObject arrayEntryOrNilZero(RubyArray array) {
        if (0 < array.getLength()) {
            return array.eltInternal(0);
        } else {
            return array.getRuntime().getNil();
        }
    }

    @Deprecated // not used
    public static IRubyObject arrayEntryOrNilOne(RubyArray array) {
        if (1 < array.getLength()) {
            return array.eltInternal(1);
        } else {
            return array.getRuntime().getNil();
        }
    }

    @Deprecated // not used
    public static IRubyObject arrayEntryOrNilTwo(RubyArray array) {
        if (2 < array.getLength()) {
            return array.eltInternal(2);
        } else {
            return array.getRuntime().getNil();
        }
    }

    @Deprecated // not used
    public static IRubyObject arrayPostOrNil(RubyArray array, int pre, int post, int index) {
        if (pre + post < array.getLength()) {
            return array.eltInternal(array.getLength() - post + index);
        } else if (pre + index < array.getLength()) {
            return array.eltInternal(pre + index);
        } else {
            return array.getRuntime().getNil();
        }
    }

    @Deprecated // not used
    public static IRubyObject arrayPostOrNilZero(RubyArray array, int pre, int post) {
        if (pre + post < array.getLength()) {
            return array.eltInternal(array.getLength() - post + 0);
        } else if (pre + 0 < array.getLength()) {
            return array.eltInternal(pre + 0);
        } else {
            return array.getRuntime().getNil();
        }
    }

    @Deprecated // not used
    public static IRubyObject arrayPostOrNilOne(RubyArray array, int pre, int post) {
        if (pre + post < array.getLength()) {
            return array.eltInternal(array.getLength() - post + 1);
        } else if (pre + 1 < array.getLength()) {
            return array.eltInternal(pre + 1);
        } else {
            return array.getRuntime().getNil();
        }
    }

    @Deprecated // not used
    public static IRubyObject arrayPostOrNilTwo(RubyArray array, int pre, int post) {
        if (pre + post < array.getLength()) {
            return array.eltInternal(array.getLength() - post + 2);
        } else if (pre + 2 < array.getLength()) {
            return array.eltInternal(pre + 2);
        } else {
            return array.getRuntime().getNil();
        }
    }

    @Deprecated // not used
    public static RubyArray subarrayOrEmpty(RubyArray array, Ruby runtime, int index) {
        if (index < array.getLength()) {
            return createSubarray(array, index);
        } else {
            return RubyArray.newEmptyArray(runtime);
        }
    }

    @Deprecated // not used
    public static RubyArray subarrayOrEmpty(RubyArray array, Ruby runtime, int index, int post) {
        if (index + post < array.getLength()) {
            return createSubarray(array, index, post);
        } else {
            return RubyArray.newEmptyArray(runtime);
        }
    }

    public static RubyModule checkIsModule(IRubyObject maybeModule) {
        if (maybeModule instanceof RubyModule) return (RubyModule) maybeModule;

        throw typeError(maybeModule.getRuntime().getCurrentContext(), "", maybeModule, " is not a class/module");
    }

    public static IRubyObject getGlobalVariable(Ruby runtime, String name) {
        return runtime.getGlobalVariables().get(name);
    }

    public static IRubyObject setGlobalVariable(IRubyObject value, Ruby runtime, String name) {
        return runtime.getGlobalVariables().set(name, value);
    }

    public static IRubyObject getInstanceVariable(IRubyObject self, Ruby runtime, String internedName) {
        IRubyObject result = self.getInstanceVariables().getInstanceVariable(internedName);
        if (result != null) return result;
        return runtime.getNil();
    }

    public static IRubyObject getInstanceVariableNoWarn(IRubyObject self, ThreadContext context, String internedName) {
        IRubyObject result = self.getInstanceVariables().getInstanceVariable(internedName);
        if (result != null) return result;
        return context.nil;
    }

    public static IRubyObject setInstanceVariable(IRubyObject value, IRubyObject self, String name) {
        return self.getInstanceVariables().setInstanceVariable(name, value);
    }

    public static RubyProc newLiteralLambda(ThreadContext context, Block block, IRubyObject self) {
        return RubyProc.newProc(context.runtime, block, Block.Type.LAMBDA);
    }

    public static void fillNil(final IRubyObject[] arr, int from, int to, Ruby runtime) {
        if (arr.length == 0) return;
        IRubyObject nils[] = runtime.getNilPrefilledArray();
        int i;

        // NOTE: seems that Arrays.fill(arr, runtime.getNil()) won't do better ... on Java 8
        // Object[] array doesn't get the same optimizations as e.g. byte[] int[]

        for (i = from; i + Ruby.NIL_PREFILLED_ARRAY_SIZE < to; i += Ruby.NIL_PREFILLED_ARRAY_SIZE) {
            System.arraycopy(nils, 0, arr, i, Ruby.NIL_PREFILLED_ARRAY_SIZE);
        }
        ArraySupport.copy(nils, arr, i, to - i);
    }

    /**
     * Return a nil-filled IRubyObject array of the specified length.
     *
     * @param length the length of the array requested
     * @param runtime the current runtime
     * @return a nil-filled IRubyObject array of the requested length
     */
    public static IRubyObject[] nilledArray(int length, Ruby runtime) {
        if (length == 0) return IRubyObject.NULL_ARRAY;
        IRubyObject[] nils = runtime.getNilPrefilledArray();
        int i;

        if (length < nils.length) {
            return Arrays.copyOfRange(nils, 0, length);
        }

        IRubyObject[] arr = new IRubyObject[length];

        // NOTE: seems that Arrays.fill(arr, runtime.getNil()) won't do better ... on Java 8
        // Object[] array doesn't get the same optimizations as e.g. byte[] int[]

        for (i = 0; i + Ruby.NIL_PREFILLED_ARRAY_SIZE < length; i += Ruby.NIL_PREFILLED_ARRAY_SIZE) {
            System.arraycopy(nils, 0, arr, i, Ruby.NIL_PREFILLED_ARRAY_SIZE);
        }
        ArraySupport.copy(nils, arr, i, length - i);

        return arr;
    }

    public static void fillNil(IRubyObject[] arr, Ruby runtime) {
        fillNil(arr, 0, arr.length, runtime);
    }

    public static Block getBlock(ThreadContext context, IRubyObject self, Node node) {
        throw new RuntimeException("Should not be called");
    }

    public static Block getBlock(Ruby runtime, ThreadContext context, IRubyObject self, Node node, Block aBlock) {
        throw new RuntimeException("Should not be called");
    }

    /**
     * Equivalent to rb_equal in MRI
     *
     * @param context
     * @param a
     * @param b
     * @return
     */
    public static RubyBoolean rbEqual(ThreadContext context, IRubyObject a, IRubyObject b) {
        if (a == b) return context.tru;
        return asBoolean(context, sites(context).op_equal.call(context, a, a, b).isTrue());
    }

    /**
     * Equivalent to rb_equal in MRI
     *
     * @param context
     * @param a
     * @param b
     * @return
     */
    public static RubyBoolean rbEqual(ThreadContext context, IRubyObject a, IRubyObject b, CallSite equal) {
        if (a == b) return context.tru;
        return asBoolean(context, equal.call(context, a, a, b).isTrue());
    }

    /**
     * Equivalent to rb_eql in MRI
     *
     * @param context
     * @param a
     * @param b
     * @return
     */
    public static RubyBoolean rbEql(ThreadContext context, IRubyObject a, IRubyObject b) {
        if (a == b) return context.tru;
        return asBoolean(context, invokedynamic(context, a, EQL, b).isTrue());
    }

    /**
     * Used by the compiler to simplify arg checking in variable-arity paths
     *
     * @param context thread context
     * @param args arguments array
     * @param min minimum required
     * @param max maximum allowed
     */
    public static void checkArgumentCount(ThreadContext context, IRubyObject[] args, int min, int max) {
        checkArgumentCount(context, args.length, min, max);
    }

    /**
     * Used by the compiler to simplify arg checking in variable-arity paths
     *
     * @param context thread context
     * @param args arguments array
     * @param req required number
     */
    public static void checkArgumentCount(ThreadContext context, IRubyObject[] args, int req) {
        checkArgumentCount(context, args.length, req, req);
    }

    public static void checkArgumentCount(ThreadContext context, int length, int min, int max) {
        int expected;
        if (length < min) {
            expected = min;
        } else if (max > -1 && length > max) {
            expected = max;
        } else {
            return;
        }
        throw argumentError(context, length, expected);
    }

    public static boolean isModuleAndHasConstant(IRubyObject left, String name) {
        return left instanceof RubyModule && ((RubyModule) left).publicConstDefinedFrom(name);
    }

    @JIT @Interp
    public static IRubyObject getDefinedConstantOrBoundMethod(IRubyObject left, String name, IRubyObject definedConstantMessage, IRubyObject definedMethodMessage) {
        if (isModuleAndHasConstant(left, name)) return definedConstantMessage;
        if (left.getMetaClass().isMethodBound(name, true)) return definedMethodMessage;
        return null;
    }

    @Deprecated(since = "10.0")
    public static RubyModule getSuperClassForDefined(Ruby runtime, RubyModule klazz) {
        RubyModule superklazz = klazz.getSuperClass();

        if (superklazz == null && klazz.isModule()) superklazz = objectClass(runtime.getCurrentContext());

        return superklazz;
    }

    public static String[] getScopeNames(String scopeNames) {
        StringTokenizer toker = new StringTokenizer(scopeNames, ";");
        ArrayList list = new ArrayList(10);
        while (toker.hasMoreTokens()) {
            list.add(toker.nextToken().intern());
        }
        return (String[])list.toArray(new String[list.size()]);
    }

    public static RubyClass metaclass(IRubyObject object) {
        return object instanceof RubyBasicObject ?
            ((RubyBasicObject)object).getMetaClass() :
            object.getMetaClass();
    }

    public static String rawBytesToString(byte[] bytes) {
        // stuff bytes into chars
        char[] chars = new char[bytes.length];
        for (int i = 0; i < bytes.length; i++) chars[i] = (char)bytes[i];
        return new String(chars);
    }

    public static byte[] stringToRawBytes(String string) {
        char[] chars = string.toCharArray();
        byte[] bytes = new byte[chars.length];
        for (int i = 0; i < chars.length; i++) bytes[i] = (byte)chars[i];
        return bytes;
    }

    public static String encodeCaptureOffsets(int[] scopeOffsets) {
        char[] encoded = new char[scopeOffsets.length * 2];
        for (int i = 0; i < scopeOffsets.length; i++) {
            int offDepth = scopeOffsets[i];
            char off = (char)(offDepth & 0xFFFF);
            char depth = (char)(offDepth >> 16);
            encoded[2 * i] = off;
            encoded[2 * i + 1] = depth;
        }
        return new String(encoded);
    }

    public static int[] decodeCaptureOffsets(String encoded) {
        char[] chars = encoded.toCharArray();
        int[] scopeOffsets = new int[chars.length / 2];
        for (int i = 0; i < scopeOffsets.length; i++) {
            char off = chars[2 * i];
            char depth = chars[2 * i + 1];
            scopeOffsets[i] = (((int)depth) << 16) | (int)off;
        }
        return scopeOffsets;
    }

    @Deprecated
    public static RubyArray argsPush(ThreadContext context, RubyArray first, IRubyObject second) {
        return ((RubyArray)first.dup()).append(context, second);
    }

    @JIT @Interp
    public static RubyArray argsPush(ThreadContext context, IRubyObject first, IRubyObject second, boolean usesKeywords) {
        boolean isEmptyKeywordRest = usesKeywords && second instanceof RubyHash && ((RubyHash) second).isEmpty();

        if (isEmptyKeywordRest) {
            RubyArray array = (RubyArray) first.dup();
            context.callInfo |= CALL_KEYWORD_EMPTY;
            return array;
        }

        return ((RubyArray)first.dup()).append(context, second);
    }

    public static RubyArray argsCat(ThreadContext context, IRubyObject first, IRubyObject second) {
        IRubyObject secondArgs = IRRuntimeHelpers.irSplat(context, second);

        return ((RubyArray) Helpers.ensureRubyArray(context.runtime, first).dup()).concat(secondArgs);
    }

    @Deprecated
    public static RubyArray argsCat(IRubyObject first, IRubyObject second) {
        return argsCat(first.getRuntime().getCurrentContext(), first, second);
    }

    /** Use an ArgsNode (used for blocks) to generate ArgumentDescriptors */
    public static ArgumentDescriptor[] argsNodeToArgumentDescriptors(ArgsNode argsNode) {
        ArrayList<ArgumentDescriptor> descs = new ArrayList<>();
        Node[] args = argsNode.getArgs();
        int preCount = argsNode.getPreCount();

        if (preCount > 0) {
            for (int i = 0; i < preCount; i++) {
                if (args[i] instanceof MultipleAsgnNode) {
                    descs.add(new ArgumentDescriptor(ArgumentType.anonreq));
                } else {
                    descs.add(new ArgumentDescriptor(ArgumentType.req, ((ArgumentNode) args[i]).getName()));
                }
            }
        }


        int optCount = argsNode.getOptionalArgsCount();
        if (optCount > 0) {
            int optIndex = argsNode.getOptArgIndex();

            for (int i = 0; i < optCount; i++) {
                Node optNode = args[optIndex + i];
                if (optNode instanceof INameNode) {
                    descs.add(new ArgumentDescriptor(ArgumentType.opt, ((INameNode) optNode).getName()));
                } else {
                    descs.add(new ArgumentDescriptor(ArgumentType.anonopt));
                }
            }
        }

        ArgumentNode restArg = argsNode.getRestArgNode();
        if (restArg != null) {
            if (!(restArg instanceof UnnamedRestArgNode) || ((UnnamedRestArgNode) restArg).isStar()) {
                descs.add(new ArgumentDescriptor(ArgumentType.rest, restArg.getName()));
            }
        }

        int postCount = argsNode.getPostCount();
        if (postCount > 0) {
            int postIndex = argsNode.getPostIndex();
            for (int i = 0; i < postCount; i++) {
                Node postNode = args[postIndex + i];
                if (postNode instanceof MultipleAsgnNode) {
                    descs.add(new ArgumentDescriptor(ArgumentType.anonreq));
                } else {
                    descs.add(new ArgumentDescriptor(ArgumentType.req, ((ArgumentNode)postNode).getName()));
                }
            }
        }

        int keywordsCount = argsNode.getKeywordCount();
        if (keywordsCount > 0) {
            int keywordsIndex = argsNode.getKeywordsIndex();
            for (int i = 0; i < keywordsCount; i++) {
                Node keyWordNode = args[keywordsIndex + i];
                for (Node asgnNode : keyWordNode.childNodes()) {
                    ArgumentType type = isRequiredKeywordArgumentValueNode(asgnNode) ? ArgumentType.keyreq : ArgumentType.key;
                    descs.add(new ArgumentDescriptor(type, ((INameNode) asgnNode).getName()));
                }
            }
        }

        if (argsNode.getKeyRest() != null) {
            RubySymbol argName = argsNode.getKeyRest().getName();
            ArgumentType type;

            if (argName == null || argName.getBytes().length() == 0) {
                type = ArgumentType.anonkeyrest;
            } else if (argName.getBytes().equals(CommonByteLists.NIL)) {
                type = ArgumentType.nokey;
            } else {
                type = ArgumentType.keyrest;
            }
            descs.add(new ArgumentDescriptor(type, argName));
        }
        if (argsNode.getBlock() != null) descs.add(new ArgumentDescriptor(ArgumentType.block, argsNode.getBlock().getName()));

        return descs.toArray(new ArgumentDescriptor[descs.size()]);
    }

    /**
     * Convert a parameter list from prefix format to ArgumentDescriptor format.  This source is expected to come
     * from a native path.  Therefore we will be assuming parameterList is UTF-8.
     */
    public static ArgumentDescriptor[] parameterListToArgumentDescriptors(Ruby runtime, String[] parameterList, boolean isLambda) {
        ArgumentDescriptor[] parms = new ArgumentDescriptor[parameterList.length];

        for (int i = 0; i < parameterList.length; i++) {
            String param = parameterList[i];

            if (param.equals("NONE")) break;

            ArgumentType type = ArgumentType.valueOf(param.charAt(0));

            // for lambdas, we call required args optional
            if (type == ArgumentType.req && !isLambda) type = ArgumentType.opt;

            // 'R', 'o', 'n' forms can get here without a name
            if (param.length() > 1) {
                parms[i] = new ArgumentDescriptor(type, runtime.newSymbol(param.substring(1)));
            } else {
                parms[i] = new ArgumentDescriptor(type.anonymousForm());
            }
        }

        return parms;
    }

    /**
     * @param runtime
     * @param argsDesc
     * @param isLambda
     * @return ""
     * @deprecated Use {@link Helpers#argumentDescriptorsToParameters(ThreadContext, ArgumentDescriptor[], boolean)} instead.
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public static RubyArray argumentDescriptorsToParameters(Ruby runtime, ArgumentDescriptor[] argsDesc, boolean isLambda) {
        return argumentDescriptorsToParameters(runtime.getCurrentContext(), argsDesc, isLambda);
    }

    /** Convert a parameter list from ArgumentDescriptor format to "Array of Array" format */
    public static RubyArray argumentDescriptorsToParameters(ThreadContext context, ArgumentDescriptor[] argsDesc, boolean isLambda) {
        if (argsDesc == null) Thread.dumpStack();

        var objArray = new IRubyObject[argsDesc.length];
        for (int i = 0; i < argsDesc.length; i++) {
            objArray[i] = argsDesc[i].toArrayForm(context, isLambda);
        }

        return Create.newArrayNoCopy(context, objArray);
    }

    public static ArgumentDescriptor[] methodToArgumentDescriptors(DynamicMethod method) {
        method = method.getRealMethod();

        if (method instanceof MethodArgs2) {
            return parameterListToArgumentDescriptors(method.getImplementationClass().getRuntime(), ((MethodArgs2) method).getParameterList(), true);
        } else if (method instanceof IRMethodArgs) {
            return ((IRMethodArgs) method).getArgumentDescriptors();
        } else {
            return new ArgumentDescriptor[]{new ArgumentDescriptor(ArgumentType.anonrest)};
        }
    }

    public static IRubyObject methodToParameters(Ruby runtime, AbstractRubyMethod recv) {
        DynamicMethod method = recv.getMethod().getRealMethod();

        return argumentDescriptorsToParameters(runtime, methodToArgumentDescriptors(method), true);
    }

    public static IRubyObject getDefinedCall(ThreadContext context, IRubyObject self, IRubyObject receiver, String name, IRubyObject definedMessage) {
        RubyClass metaClass = receiver.getMetaClass();
        DynamicMethod method = metaClass.searchMethod(name);
        Visibility visibility = method.getVisibility();

        if (visibility != Visibility.PRIVATE &&
                (visibility != Visibility.PROTECTED || metaClass.getRealClass().isInstance(self)) && !method.isUndefined()) {
            return definedMessage;
        }

        if (receiver.callMethod(context, "respond_to_missing?", new IRubyObject[]{asSymbol(context, name), context.fals}).isTrue()) {
            return definedMessage;
        }
        return null;
    }

    public static IRubyObject invokedynamic(ThreadContext context, IRubyObject self, MethodNames method) {
        RubyClass metaclass = self.getMetaClass();
        String name = method.realName();
        CacheEntry entry = getMethodCached(context, metaclass, method.ordinal(), name);
        return entry.method.call(context, self, entry.sourceModule, name);
    }

    public static IRubyObject invokedynamic(ThreadContext context, IRubyObject self, MethodNames method, IRubyObject arg0) {
        RubyClass metaclass = self.getMetaClass();
        String name = method.realName();
        CacheEntry entry = getMethodCached(context, metaclass, method.ordinal(), name);
        return entry.method.call(context, self, entry.sourceModule, name, arg0);
    }

    private static CacheEntry getMethodCached(ThreadContext context, RubyClass metaclass, int index, String name) {
        if (metaclass.getClassIndex() == ClassIndex.NO_INDEX) return metaclass.searchWithCache(name);
        return context.runtimeCache.getMethodEntry(context, metaclass, metaclass.getClassIndex().ordinal() * (index + 1), name);
    }

    @Deprecated // not used
    public static IRubyObject lastElement(IRubyObject[] ary) {
        return ary[ary.length - 1];
    }

    @Deprecated // not used
    public static RubyString appendAsString(RubyString target, IRubyObject other) {
        return target.append(other.asString());
    }

    // . Array given to rest should pass itself
    // . Array with rest + other args should extract array
    // . Array with multiple values and NO rest should extract args if there are more than one argument

    static IRubyObject[] restructureBlockArgs(ThreadContext context,
        IRubyObject value, Signature signature, Block.Type type) {

        if (!type.checkArity && signature == Signature.NO_ARGUMENTS) return IRubyObject.NULL_ARRAY;

        if (value == null) return IRubyObject.NULL_ARRAY;

        return new IRubyObject[] { value };
    }

    @Deprecated // not used
    public static RubyString appendByteList(RubyString target, ByteList source) {
        target.getByteList().append(source);
        return target;
    }

    @JIT
    public static boolean BNE(ThreadContext context, IRubyObject value1, IRubyObject value2) {
        boolean eql = value2 == context.nil || value2 == UndefinedValue.UNDEFINED ?
                value1 == value2 : value1.op_equal(context, value2).isTrue();

        return !eql;
    }

    public static void irCheckArgsArrayArity(ThreadContext context, RubyArray args, int required, int opt, boolean rest) {
        int numArgs = args.size();
        if (numArgs < required || (!rest && numArgs > (required + opt))) {
            Arity.raiseArgumentError(context, numArgs, required, required + opt);
        }
    }

    @Deprecated
    public static IRubyObject invokedynamic(ThreadContext context, IRubyObject self, int index) {
        return invokedynamic(context, self, MethodNames.values()[index]);
    }

    @Deprecated
    public static IRubyObject invokedynamic(ThreadContext context, IRubyObject self, int index, IRubyObject arg0) {
        return invokedynamic(context, self, MethodNames.values()[index], arg0);
    }

    /**
     * <p>Note: Assumes exception: ... to be the only (optional) keyword argument!</p>
     * @param context
     * @param opts
     * @return false if `exception: false`, true otherwise
     */
    public static boolean extractExceptionOnlyArg(ThreadContext context, RubyHash opts) {
        return ArgsUtil.extractKeywordArg(context, opts, "exception") != context.fals;
    }

    /**
     * <p>Note: Assumes exception: ... to be the only (optional) keyword argument!</p>
     * @param context
     * @param opts the keyword args hash
     * @param defValue to return when no keyword options
     * @return false if `exception: false`, true (or default value) otherwise
     */
    public static boolean extractExceptionOnlyArg(ThreadContext context, IRubyObject opts, boolean defValue) {
        IRubyObject hash = TypeConverter.checkHashType(context.runtime, opts);
        if (hash != context.nil) {
            return extractExceptionOnlyArg(context, (RubyHash) opts);
        }
        return defValue;
    }

    /**
     * <p>Note: Assumes exception: ... to be the only (optional) keyword argument!</p>
     * @param context
     * @param args method args
     * @param defValue to return when no keyword options
     * @return false if `exception: false`, true (or default value) otherwise
     */
    public static boolean extractExceptionOnlyArg(ThreadContext context, IRubyObject[] args, boolean defValue) {
        if (args.length == 0) return defValue;
        return extractExceptionOnlyArg(context, args[args.length - 1], defValue);
    }

    public static void throwException(final Throwable e) {
        Helpers.<RuntimeException>throwsUnchecked(e);
    }

    public static <T> T tryThrow(Callable<T> call) {
        try {
            return call.call();
        } catch (Throwable t) {
            throwException(t);
            return null; // not reached
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwsUnchecked(final Throwable e) throws T {
        throw (T) e;
    }

    /**
     * Decode the given value to a Java string using the following rules:
     *
     * * If the string is all US-ASCII characters, it will be decoded as US-ASCII.
     * * If the string is a unicode encoding, it will be decoded as such.
     * * If the string is any other encoding, it will be encoded as raw bytes
     *   using ISO-8859-1.
     *
     * This allows non-unicode, non-US-ASCII encodings to be represented in the
     * symbol table as their raw versions, but properly decodes unicode-
     * encoded strings.
     *
     * @param value the value to decode
     * @return the resulting symbol string
     */
    public static String symbolBytesToString(ByteList value) {
        Encoding encoding = value.getEncoding();
        if (encoding == USASCIIEncoding.INSTANCE || encoding == ASCIIEncoding.INSTANCE) {
            return value.toString(); // raw
        } else if (encoding instanceof UnicodeEncoding) {
            return new String(value.getUnsafeBytes(), value.getBegin(), value.getRealSize(), EncodingUtils.charsetForEncoding(value.getEncoding()));
        } else {
            return value.toString(); // raw
        }
    }

    /**
     * Decode a given ByteList to a Java string.
     *
     * @param runtime the current runtime
     * @param value the bytelist
     * @return a Java String representation of the ByteList
     */
    public static String decodeByteList(Ruby runtime, ByteList value) {
        byte[] unsafeBytes = value.getUnsafeBytes();
        int begin = value.getBegin();
        int length = value.length();

        Encoding encoding = value.getEncoding();

        if (encoding == UTF8Encoding.INSTANCE) {
            return RubyEncoding.decodeUTF8(unsafeBytes, begin, length);
        }

        Charset charset = runtime.getEncodingService().charsetForEncoding(encoding);

        if (charset == null) {
            // No JDK Charset available for this encoding; convert to UTF-16 ourselves.
            Encoding utf16 = EncodingUtils.getUTF16ForPlatform();

            return EncodingUtils.strConvEnc(runtime.getCurrentContext(), value, value.getEncoding(), utf16).toString();
        }

        return RubyEncoding.decode(unsafeBytes, begin, length, charset);
    }

    /**
     * Convert a ByteList into a Java String by using its Encoding's Charset. If
     * the Charset is not available, fall back on other logic.
     *
     * @param bytes the bytelist to decode
     * @return the decoded string
     */
    public static String byteListToString(final ByteList bytes) {
        final Encoding encoding = bytes.getEncoding();

        if (encoding == UTF8Encoding.INSTANCE || encoding == USASCIIEncoding.INSTANCE) {
            return RubyEncoding.decodeUTF8(bytes.getUnsafeBytes(), bytes.getBegin(), bytes.getRealSize());
        }

        final Charset charset = EncodingUtils.charsetForEncoding(encoding);

        if ( charset != null ) {
            return new String(bytes.getUnsafeBytes(), bytes.getBegin(), bytes.getRealSize(), charset);
        }

        return bytes.toString();
    }

    public static IRubyObject rewriteStackTraceAndThrow(ThreadContext context, Throwable t) {
        Ruby runtime = context.runtime;

        StackTraceElement[] javaTrace = t.getStackTrace();
        BacktraceData backtraceData = instanceConfig(context).getTraceType().getIntegratedBacktrace(context, javaTrace);
        t.setStackTrace(RaiseException.javaTraceFromRubyTrace(backtraceData.getBacktrace(context.runtime)));
        throwException(t);
        return null; // not reached
    }

    @Deprecated // un-used
    public static void rewriteStackTrace(final Ruby runtime, final Throwable e) {
        final StackTraceElement[] javaTrace = e.getStackTrace();
        BacktraceData backtraceData = runtime.getInstanceConfig().getTraceType().getIntegratedBacktrace(runtime.getCurrentContext(), javaTrace);
        e.setStackTrace(RaiseException.javaTraceFromRubyTrace(backtraceData.getBacktrace(runtime)));
    }

    public static <T> T[] arrayOf(T... values) {
        return values;
    }

    public static IRubyObject[] arrayOf(IRubyObject first, IRubyObject... values) {
        IRubyObject[] newValues = new IRubyObject[values.length + 1];
        newValues[0] = first;
        System.arraycopy(values, 0, newValues, 1, values.length);
        return newValues;
    }

    public static IRubyObject[] arrayOf(IRubyObject first) {
        return new IRubyObject[] {first};
    }

    public static IRubyObject[] arrayOf(IRubyObject first, IRubyObject second) {
        return new IRubyObject[] {first, second};
    }

    public static IRubyObject[] arrayOf(IRubyObject first, IRubyObject second, IRubyObject third) {
        return new IRubyObject[] {first, second, third};
    }

    public static <T> T[] arrayOf(T[] values, T last, IntFunction<T[]> allocator) {
        T[] newValues = allocator.apply(values.length + 1);
        newValues[values.length] = last;
        System.arraycopy(values, 0, newValues, 0, values.length);
        return newValues;
    }

    public static <T> T[] arrayOf(Class<T> t, int size, T fill) {
        T[] ary = (T[])Array.newInstance(t, size);
        Arrays.fill(ary, fill);
        return ary;
    }

    public static int memchr(boolean[] ary, int start, int len, boolean find) {
        for (int i = 0; i < len; i++) {
            if (ary[i + start] == find) return i + start;
        }
        return -1;
    }

    public static boolean isRequiredKeywordArgumentValueNode(Node asgnNode) {
        return asgnNode.childNodes().get(0) instanceof RequiredKeywordArgumentValueNode;
    }

    // MRI: rb_hash_start
    public static long hashStart(Ruby runtime, long value) {
        long hash = value +
                (runtime.isSiphashEnabled() ?
                        runtime.getHashSeedK1() :
                        runtime.getHashSeedK0());
        return hash;
    }

    public static long hashEnd(long value) {
        value = murmur_step(value, 10);
        value = murmur_step(value, 17);
        return value;
    }

    // MRI: rb_hash
    public static RubyFixnum safeHash(final ThreadContext context, IRubyObject obj) {
        var hval = context.safeRecurse(sites(context).recursive_hash, context.runtime, obj, "hash", true);

        while (!(hval instanceof RubyFixnum fixnum)) {
            // This is different from MRI because we don't have rb_integer_pack
            if (hval instanceof RubyBignum bignum) return bignum.hash(context);
            hval = hval.convertToInteger();
        }

        return fixnum;
    }

    // MRI: mult_and_mix, roughly since we have no uint64 type
    public static long multAndMix(long seed, long hash) {
        long hm1 = seed >> 32, hm2 = hash >> 32;
        long lm1 = seed, lm2 = hash;
        long v64_128 = hm1 * hm2;
        long v32_96 = hm1 * lm2 + lm1 * hm2;
        long v1_32 = lm1 * lm2;

        return (v64_128 + (v32_96 >> 32)) ^ ((v32_96 << 32) + v1_32);
    }

    public static long murmurCombine(long h, long i)
    {
        long v = 0;
        h += i;
        v = murmur1(v + h);
        v = murmur1(v + (h >>> 4*8));
        return v;
    }

    public static long murmur(long h, long k, int r)
    {
        long m = MurmurHash.MURMUR2_MAGIC;
        h += k;
        h *= m;
        h ^= h >> r;
        return h;
    }

    public static long murmur_finish(long h)
    {
        h = murmur(h, 0, 10);
        h = murmur(h, 0, 17);
        return h;
    }

    public static long murmur_step(long h, long k) {
        return murmur((h), (k), 16);
    }

    public static long murmur1(long h) {
        return murmur_step(h, 16);
    }

    private static HelpersSites sites(ThreadContext context) {
        return context.sites.Helpers;
    }

    @Deprecated
    public static String encodeParameterList(List<String[]> args) {
        if (args.size() == 0) return "NONE";

        StringBuilder builder = new StringBuilder();

        boolean added = false;
        for (String[] desc : args) {
            if (added) builder.append(';');
            builder.append(desc[0]).append(desc[1]);
            added = true;
        }

        return builder.toString();
    }

    public static byte[] subseq(byte[] ary, int start, int len) {
        byte[] newAry = new byte[len];
        System.arraycopy(ary, start, newAry, 0, len);
        return newAry;
    }

    /**
     *
     * We have respondTo logic in RubyModule and we have a special callsite for respond_to?.
     * This method is just so we can share that logic.
     */
    public static boolean respondsToMethod(DynamicMethod method, boolean checkVisibility) {
        if (method.isUndefined() || method.isNotImplemented()) return false;

        return !(checkVisibility &&
                (method.getVisibility() == PRIVATE || method.getVisibility() == PROTECTED));
    }

    public static StaticScope getStaticScope(IRScope scope) {
        return scope.getStaticScope();
    }

    // FIXME: to_path should not be called n times it should only be once and that means a cache which would
    // also reduce all this casting and/or string creates.
    // (mkristian) would it make sense to turn $LOAD_PATH into something like RubyClassPathVariable where we could cache
    // the Strings ?
    public static String javaStringFromPath(Ruby runtime, IRubyObject loadPathEntry) {
        return RubyFile.get_path(runtime.getCurrentContext(), loadPathEntry).asJavaString();
    }

    /**
     * This method is deprecated because it depends on having a Ruby frame pushed for checking method visibility,
     * and there's no way to enforce that. Most users of this method probably don't need to check visibility.
     *
     * See https://github.com/jruby/jruby/issues/4134
     *
     * @deprecated Use finvoke if you do not want visibility-checking or invokeFrom if you do.
     */
    @Deprecated(since = "9.4-", forRemoval = true)
    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, IRubyObject[] args, CallType callType, Block block) {
        return self.getMetaClass().invoke(context, self, name, args, callType, block);
    }

    /**
     * This method is deprecated because it depends on having a Ruby frame pushed for checking method visibility,
     * and there's no way to enforce that. Most users of this method probably don't need to check visibility.
     *
     * See https://github.com/jruby/jruby/issues/4134
     *
     * @deprecated Use finvoke if you do not want visibility-checking or invokeFrom if you do.
     */
    @Deprecated(since = "9.4-", forRemoval = true)
    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, IRubyObject arg, CallType callType, Block block) {
        return self.getMetaClass().invoke(context, self, name, arg, callType, block);
    }

    /**
     * This method is deprecated because it depends on having a Ruby frame pushed for checking method visibility,
     * and there's no way to enforce that. Most users of this method probably don't need to check visibility.
     *
     * See https://github.com/jruby/jruby/issues/4134
     *
     * @deprecated Use finvoke if you do not want visibility-checking or invokeFrom if you do.
     */
    @Deprecated(since = "9.4-", forRemoval = true)
    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, CallType callType) {
        return Helpers.invoke(context, self, name, IRubyObject.NULL_ARRAY, callType, Block.NULL_BLOCK);
    }

    @Deprecated
    public static IRubyObject invokeFrom(ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject[] args, CallType callType, Block block) {
        return self.getMetaClass().invokeFrom(context, callType, caller, self, name, args, block);
    }

    @Deprecated
    public static IRubyObject invokeFrom(ThreadContext context, IRubyObject caller, IRubyObject self, String name, IRubyObject arg, CallType callType, Block block) {
        return self.getMetaClass().invokeFrom(context, callType, caller, self, name, arg, block);
    }

    @Deprecated
    public static IRubyObject invokeFrom(ThreadContext context, IRubyObject caller, IRubyObject self, String name, CallType callType) {
        return self.getMetaClass().invokeFrom(context, callType, caller, self, name, IRubyObject.NULL_ARRAY, Block.NULL_BLOCK);
    }

    @Deprecated
    public static IRubyObject setBackref(Ruby runtime, ThreadContext context, IRubyObject value) {
        if (!value.isNil() && !(value instanceof RubyMatchData)) throw typeError(context, value, "MatchData");

        return context.setBackRef(value);
    }

    @Deprecated
    public static IRubyObject getBackref(Ruby runtime, ThreadContext context) {
        return context.getBackRef();
    }

    @Deprecated
    public static IRubyObject backref(ThreadContext context) {
        return context.getBackRef();
    }
}
