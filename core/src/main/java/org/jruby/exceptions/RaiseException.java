/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Joey Gibson <joey@joeygibson.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.exceptions;

import java.lang.reflect.Member;
import java.util.Arrays;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyStandardError;
import org.jruby.RubyString;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.backtrace.RubyStackTraceElement;
import org.jruby.runtime.backtrace.TraceType;
import org.jruby.runtime.builtin.IRubyObject;

public class RaiseException extends JumpException {
    private static final long serialVersionUID = -7612079169559973951L;

    private RubyException exception;
    private String providedMessage;

    protected RaiseException(String message, RubyException exception) {
        super(message);
        setException(exception);
        preRaise(exception.getRuntime().getCurrentContext(), RubyException.retrieveBacktrace(exception), true);
    }

    @Override
    public final Throwable fillInStackTrace() {
        // NOTE: super logic (RubyInstanceConfig.JUMPS_HAVE_BACKTRACE) is not relevant for RaiseException

        return this; // do not auto-fill from Throwable.<init> we fill (setStackTrace) in preRaise(...)
    }

    /**
     * Construct a new throwable RaiseException appropriate for the target Ruby exception class.
     *
     * @param runtime the current JRuby runtime
     * @param exceptionClass the class of the exception to construct and raise
     * @param msg a simple message for the exception
     * @return a RaiseException instance appropriate for the target Ruby exception class
     */
    public static RaiseException from(Ruby runtime, RubyClass exceptionClass, String msg) {
        return RubyException.newException(runtime, exceptionClass, msg).toThrowable();
    }

    /**
     * Construct a new throwable RaiseException appropriate for the target Ruby exception class.
     *
     * @param runtime the current JRuby runtime
     * @param exceptionClass the class of the exception to construct and raise
     * @param msg a simple message for the exception
     * @param backtrace a Ruby object (usually an Array) to use for the exception's backtrace
     * @return a RaiseException instance appropriate for the target Ruby exception class
     */
    public static RaiseException from(Ruby runtime, RubyClass exceptionClass, String msg, IRubyObject backtrace) {
        RubyException exception = RubyException.newException(runtime, exceptionClass, msg);
        exception.setBacktrace(backtrace);
        return exception.toThrowable();
    }

    /**
     * Construct a new throwable RaiseException appropriate for the target Ruby exception class.
     *
     * @param runtime the current JRuby runtime
     * @param exceptionClass the class of the exception to construct and raise
     * @param args the arguments for the exception's constructor
     * @return a RaiseException instance appropriate for the target Ruby exception class
     */
    public static RaiseException from(Ruby runtime, RubyClass exceptionClass, IRubyObject... args) {
        RubyException exception = RubyException.newException(runtime.getCurrentContext(), exceptionClass, args);
        return exception.toThrowable();
    }

    /**
     * Construct a new throwable RaiseException appropriate for the target Ruby exception class.
     *
     * @param runtime the current JRuby runtime
     * @param exceptionPath a string representing the fully-qualified constant path to look up the exception
     * @param msg a simple message for the exception
     * @return a RaiseException instance appropriate for the target Ruby exception class
     */
    public static RaiseException from(Ruby runtime, String exceptionPath, String msg) {
        RubyClass exceptionClass = findExceptionClass(runtime, exceptionPath);
        return from(runtime, exceptionClass, msg);
    }

    /**
     * Construct a new throwable RaiseException appropriate for the target Ruby exception class.
     *
     * @param runtime the current JRuby runtime
     * @param exceptionPath a string representing the fully-qualified constant path to look up the exception
     * @param msg a simple message for the exception
     * @param backtrace a Ruby object (usually an Array) to use for the exception's backtrace
     * @return a RaiseException instance appropriate for the target Ruby exception class
     */
    public static RaiseException from(Ruby runtime, String exceptionPath, String msg, IRubyObject backtrace) {
        RubyClass exceptionClass = findExceptionClass(runtime, exceptionPath);
        return from(runtime, exceptionClass, msg, backtrace);
    }

    /**
     * Construct a new throwable RaiseException appropriate for the target Ruby exception class.
     *
     * @param runtime the current JRuby runtimeString exceptionPath
     * @param args the arguments for the exception's constructor
     * @return a RaiseException instance appropriate for the target Ruby exception class
     */
    public static RaiseException from(Ruby runtime, String exceptionPath, IRubyObject... args) {
        RubyClass exceptionClass = findExceptionClass(runtime, exceptionPath);
        return from(runtime, exceptionClass, args);
    }

    private static RubyClass findExceptionClass(Ruby runtime, String exceptionPath) {
        IRubyObject exceptionClass = runtime.getObject().getConstant(exceptionPath);

        if (exceptionClass == null) {
            throw runtime.newNameError("exception class not found", exceptionPath);
        } else if (!(exceptionClass instanceof RubyClass)) {
            throw runtime.newTypeError("expected to find exception class for " + exceptionPath + " but got " + exceptionClass.inspect());
        }
        return (RubyClass) exceptionClass;
    }

    @Override
    public String getMessage() {
        if (providedMessage == null) {
            providedMessage = '(' + exception.getMetaClass().getBaseName() + ") " + exception.message(exception.getRuntime().getCurrentContext()).asJavaString();
        }
        return providedMessage;
    }

    @Override
    public Throwable getCause() {
        Throwable cause = super.getCause();
        if (cause == null && exception != null) {
            Object rubyCause = exception.getCause(); // an IRubyObject
            // check for a Ruby Exception cause or a Java (proxy) Throwable
            if (rubyCause instanceof RubyException) {
                cause = ((RubyException) rubyCause).toThrowable();
            } else if (rubyCause instanceof IRubyObject) {
                rubyCause = JavaUtil.unwrapIfJavaObject((IRubyObject) rubyCause);
                if (rubyCause instanceof Throwable) {
                    cause = (Throwable) rubyCause;
                }
            }
        }
        return cause;
    }

    /**
     * Gets the exception
     * @return Returns a RubyException
     */
    public final RubyException getException() {
        return exception;
    }

    private void preRaise(ThreadContext context, IRubyObject backtrace, boolean capture) {
        context.runtime.incrementExceptionCount();
        if (RubyInstanceConfig.LOG_EXCEPTIONS) TraceType.logException(exception);

        doSetLastError(context);
        doCallEventHook(context);

        if (backtrace == null) {
            if (capture) { // only false to support legacy RaiseException construction (not setting trace)
                if (requiresBacktrace(context)) exception.captureBacktrace(context);
                setStackTraceFromException();
            }
        } else {
            exception.setBacktrace(backtrace);
            if (!backtrace.isNil() && !isEmptyArray(backtrace)) {
                if (requiresBacktrace(context)) exception.captureBacktrace(context);
            }
            setStackTraceFromException();
        }
    }

    private void setStackTraceFromException() {
        RubyStackTraceElement[] rubyTrace = exception.getBacktraceElements();
        if (rubyTrace.length > 5 && "getStackTrace".equals(rubyTrace[0].getMethodName())) { // -Xbacktrace.style=raw
            int skip = 0;
            if ("preRaise".equals(rubyTrace[4].getMethodName())) {
                skip = 5;
            } else if ("preRaise".equals(rubyTrace[3].getMethodName())) {
                skip = 4;
            }
            // NOTE: we could skip more useless Throwable.<init> hierarchy constructor trace
            //  up to org.jruby.exceptions.RaiseException.from ?
            rubyTrace = Arrays.copyOfRange(rubyTrace, skip, rubyTrace.length);
        }
        setStackTrace(javaTraceFromRubyTrace(rubyTrace));
    }

    private StackTraceElement[] skipFillInStackTracePart(StackTraceElement[] trace) {
        final int len = trace.length;
        if (len >= 3) {
            // NOTE: let's not do too much work here - there are 2 paths to fillInStackTrace
            int skip = 0;
            if ("preRaise".equals(trace[2].getMethodName())) {
                skip = 3;
            } else if ("preRaise".equals(trace[1].getMethodName())) {
                skip = 2;
            } else if ("fillInStackTrace".equals(trace[2].getMethodName())) {
                skip = 3; // fillInStackTraceCtor = true;
                // NOTE: we could skip more useless Throwable.<init> hierarchy constructor trace
                //  up to org.jruby.exceptions.RaiseException.from ?
            }
            return Arrays.copyOfRange(trace, skip, len);
        }
        return trace;
    }

    private void fillInStackTraceSkipPreRaise() {
        originalFillInStackTrace(); // (fillInStackTraceSkipPreRaise) originalFillInStackTrace, preRaise
        StackTraceElement[] curTrace = getStackTrace();
        StackTraceElement[] newTrace = skipFillInStackTracePart(curTrace);
        if (newTrace != curTrace) setStackTrace(newTrace);
    }

    private static void doCallEventHook(final ThreadContext context) {
        if (context.runtime.hasEventHooks()) {
            context.runtime.callEventHooks(context, RubyEvent.RAISE, context.getFile(), context.getLine(), context.getFrameName(), context.getFrameKlazz());
        }
    }

    private void doSetLastError(final ThreadContext context) {
        context.setErrorInfo(exception); // $!
    }

    /**
     * Sets the exception
     * @param newException The exception to set
     */
    protected final void setException(RubyException newException) {
        this.exception = newException;
    }

    public static StackTraceElement[] javaTraceFromRubyTrace(RubyStackTraceElement[] trace) {
        StackTraceElement[] newTrace = new StackTraceElement[trace.length];
        for (int i = 0; i < newTrace.length; i++) {
            newTrace[i] = trace[i].asStackTraceElement();
        }
        return newTrace;
    }

    @Deprecated // used by JRuby-Rack
    public static RaiseException createNativeRaiseException(Ruby runtime, Throwable cause) {
        return createNativeRaiseException(runtime, cause, null);
    }
    @Deprecated
    public static RaiseException createNativeRaiseException(Ruby runtime, Throwable cause, Member target) {
        org.jruby.NativeException nativeException = new org.jruby.NativeException(runtime, runtime.getNativeException(), cause);
        return new RaiseException(cause, nativeException);
    }
    @Deprecated
    public RaiseException(Throwable cause, org.jruby.NativeException nativeException) {
        super(nativeException.getMessageAsJavaString(), cause);
        providedMessage = super.getMessage(); // cause.getClass().getId() + ": " + message
        setException(nativeException);
        preRaise(nativeException.getRuntime().getCurrentContext(), nativeException.getCause().getStackTrace());
        setStackTraceFromException();
    }

    @Deprecated
    public RaiseException(RubyException exception) {
        this(exception.getMessageAsJavaString(), exception);
    }

    @Deprecated
    public RaiseException(RubyException exception, boolean unused) {
        this(exception.getMessageAsJavaString(), exception);
    }

    @Deprecated
    public RaiseException(RubyException exception, IRubyObject backtrace) {
        // this(exception.getMessageAsJavaString(), exception) would preRaise twice!
        super(exception.getMessageAsJavaString());
        setException(exception);
        preRaise(exception.getRuntime().getCurrentContext(), backtrace, true);
    }

    @Deprecated
    public RaiseException(Ruby runtime, RubyClass exceptionClass, String msg) {
        this(runtime, exceptionClass, msg, null);
    }

    @Deprecated
    public RaiseException(Ruby runtime, RubyClass exceptionClass, String msg, boolean unused) {
        this(runtime, exceptionClass, msg, null);
    }

    @Deprecated
    public RaiseException(Ruby runtime, RubyClass exceptionClass, String msg, IRubyObject backtrace) {
        super(msg == null ? msg = "No message available" : msg);

        providedMessage = '(' + exceptionClass.getName() + ") " + msg;

        final ThreadContext context = runtime.getCurrentContext();
        setException(RubyException.newException(context, exceptionClass, RubyString.newUnicodeString(runtime, msg)));
        preRaise(context, backtrace, true);
    }

    @Deprecated
    public RaiseException(Ruby runtime, RubyClass exceptionClass, String msg, IRubyObject backtrace, boolean unused) {
        this(runtime, exceptionClass, msg, backtrace);
    }

    @Deprecated
    protected final void setException(RubyException newException, boolean unused) {
        this.exception = newException;
    }

    @Deprecated
    private void preRaise(ThreadContext context, StackTraceElement[] javaTrace) {
        preRaise(context, null, false);

        if (requiresBacktrace(context)) {
            exception.prepareIntegratedBacktrace(context, javaTrace);
            setStackTraceFromException();
        } else {
            setStackTrace(skipFillInStackTracePart(javaTrace));
        }
    }

    @Deprecated
    public static RaiseException from(RubyException exception, IRubyObject backtrace) {
        return new RaiseException(exception, backtrace);
    }

    private boolean requiresBacktrace(ThreadContext context) {
        // We can only omit backtraces of descendents of Standard error for 'foo rescue nil'
        return context.exceptionRequiresBacktrace || !(exception instanceof RubyStandardError);
    }

    private static boolean isEmptyArray(final IRubyObject ary) {
        return ary instanceof RubyArray && ((RubyArray) ary).size() == 0;
    }

    private static JavaSites.RaiseExceptionSites sites(ThreadContext context) {
        return context.sites.RaiseException;
    }
}
