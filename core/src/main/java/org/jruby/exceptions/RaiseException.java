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
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyString;
import org.jruby.runtime.Helpers;
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
        preRaise(exception.getRuntime().getCurrentContext());
    }

    @Deprecated
    public static RaiseException from(RubyException exception, IRubyObject backtrace) {
        // FIXME: This form currently creates a new exception since it's providing a backtrace
        return new RaiseException(exception, backtrace);
    }

    public static RaiseException from(Ruby runtime, RubyClass excptnClass, String msg) {
        return RubyException.newException(runtime, excptnClass, msg).toThrowable();
    }

    public static RaiseException from(Ruby runtime, RubyClass excptnClass, String msg, IRubyObject backtrace) {
        return RubyException.newException(runtime, excptnClass, msg).toThrowable();
    }

    @Override
    public String getMessage() {
        if (providedMessage == null) {
            providedMessage = '(' + exception.getMetaClass().getBaseName() + ") " + exception.message(exception.getRuntime().getCurrentContext()).asJavaString();
        }
        return providedMessage;
    }

    /**
     * Gets the exception
     * @return Returns a RubyException
     */
    public final RubyException getException() {
        return exception;
    }

    private void preRaise(ThreadContext context) {
        preRaise(context, (IRubyObject) null);
    }

    private void preRaise(ThreadContext context, StackTraceElement[] javaTrace) {
        context.runtime.incrementExceptionCount();
        doSetLastError(context);
        doCallEventHook(context);

        if (RubyInstanceConfig.LOG_EXCEPTIONS) TraceType.logException(exception);

        if (requiresBacktrace(context)) {
            exception.prepareIntegratedBacktrace(context, javaTrace);
        }
    }

    private boolean requiresBacktrace(ThreadContext context) {
        IRubyObject debugMode;
        // We can only omit backtraces of descendents of Standard error for 'foo rescue nil'
        return context.exceptionRequiresBacktrace ||
                ((debugMode = context.runtime.getGlobalVariables().get("$DEBUG")) != null && debugMode.isTrue()) ||
                ! context.runtime.getStandardError().isInstance(exception);
    }

    private void preRaise(ThreadContext context, IRubyObject backtrace) {
        context.runtime.incrementExceptionCount();
        doSetLastError(context);
        doCallEventHook(context);

        if (RubyInstanceConfig.LOG_EXCEPTIONS) TraceType.logException(exception);

        // We can only omit backtraces of descendents of Standard error for 'foo rescue nil'
        if (requiresBacktrace(context)) {
            if (backtrace == null) {
                exception.prepareBacktrace(context);
            } else {
                exception.forceBacktrace(backtrace);
                if ( backtrace.isNil() ) return;
            }

            setStackTrace(RaiseException.javaTraceFromRubyTrace(exception.getBacktraceElements()));
        }
    }

    private static void doCallEventHook(final ThreadContext context) {
        if (context.runtime.hasEventHooks()) {
            context.runtime.callEventHooks(context, RubyEvent.RAISE, context.getFile(), context.getLine(), context.getFrameName(), context.getFrameKlazz());
        }
    }

    private void doSetLastError(final ThreadContext context) {
        context.runtime.getGlobalVariables().set("$!", exception);
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

    @Deprecated
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
        setStackTrace(RaiseException.javaTraceFromRubyTrace(exception.getBacktraceElements()));
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
        this(exception.getMessageAsJavaString(), exception);
        preRaise(exception.getRuntime().getCurrentContext(), backtrace);
    }

    @Deprecated
    public RaiseException(Ruby runtime, RubyClass excptnClass, String msg) {
        this(runtime, excptnClass, msg, null);
    }

    @Deprecated
    public RaiseException(Ruby runtime, RubyClass excptnClass, String msg, boolean unused) {
        this(runtime, excptnClass, msg, null);
    }

    @Deprecated
    public RaiseException(Ruby runtime, RubyClass excptnClass, String msg, IRubyObject backtrace) {
        super(msg == null ? msg = "No message available" : msg);

        providedMessage = '(' + excptnClass.getName() + ") " + msg;

        final ThreadContext context = runtime.getCurrentContext();
        setException((RubyException) Helpers.invoke(
                context,
                excptnClass,
                "new",
                RubyString.newUnicodeString(runtime, msg)));
        preRaise(context, backtrace);
    }

    @Deprecated
    public RaiseException(Ruby runtime, RubyClass excptnClass, String msg, IRubyObject backtrace, boolean unused) {
        this(runtime, excptnClass, msg, backtrace);
    }

    @Deprecated
    protected final void setException(RubyException newException, boolean unused) {
        this.exception = newException;
    }
}
