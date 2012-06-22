/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.exceptions;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.lang.reflect.Member;
import org.jruby.NativeException;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyString;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.backtrace.RubyStackTraceElement;
import org.jruby.runtime.backtrace.TraceType;
import org.jruby.runtime.builtin.IRubyObject;

public class RaiseException extends JumpException {
    public static final boolean DEBUG = false;
    private static final long serialVersionUID = -7612079169559973951L;
    
    private RubyException exception;
    private String providedMessage;
    private boolean nativeException;

    /**
     * Construct a new RaiseException to wrap the given Ruby exception for Java-land
     * throwing purposes.
     *
     * This constructor will generate a backtrace using the Java
     * stack trace and the interpreted Ruby frames for the current thread.
     *
     * @param actException The Ruby exception to wrap
     */
    public RaiseException(RubyException actException) {
        this(actException, false);
    }

    /**
     * Construct a new RaiseException to wrap the given Ruby exception for Java-land
     * throwing purposes.
     * 
     * This constructor will not generate a backtrace and will instead use the
     * one specified by the
     * 
     * @param exception The Ruby exception to wrap
     * @param backtrace
     */
    public RaiseException(RubyException exception, IRubyObject backtrace) {
        super(exception.message.toString());
        if (DEBUG) {
            Thread.dumpStack();
        }
        setException(exception, false);
        preRaise(exception.getRuntime().getCurrentContext(), backtrace);
    }

    public RaiseException(Ruby runtime, RubyClass excptnClass, String msg, boolean nativeException) {
        super(msg);
        if (msg == null) {
            msg = "No message available";
        }
        providedMessage = "(" + excptnClass.getName() + ") " + msg;
        this.nativeException = nativeException;
        if (DEBUG) {
            Thread.dumpStack();
        }
        setException((RubyException)RuntimeHelpers.invoke(
                runtime.getCurrentContext(),
                excptnClass,
                "new",
                RubyString.newUnicodeString(excptnClass.getRuntime(), msg)),
                nativeException);
        preRaise(runtime.getCurrentContext());
    }

    public RaiseException(Ruby runtime, RubyClass excptnClass, String msg, IRubyObject backtrace, boolean nativeException) {
        super(msg);
        if (msg == null) {
            msg = "No message available";
        }
        providedMessage = "(" + excptnClass.getName() + ") " + msg;
        this.nativeException = nativeException;
        if (DEBUG) {
            Thread.dumpStack();
        }
        setException((RubyException)RuntimeHelpers.invoke(
                runtime.getCurrentContext(),
                excptnClass,
                "new",
                RubyString.newUnicodeString(excptnClass.getRuntime(), msg)),
                nativeException);
        preRaise(runtime.getCurrentContext(), backtrace);
    }

    public RaiseException(RubyException exception, boolean isNativeException) {
        super(exception.message.toString());
        if (DEBUG) {
            Thread.dumpStack();
        }
        this.nativeException = isNativeException;
        setException(exception, isNativeException);
        preRaise(exception.getRuntime().getCurrentContext());
    }

    public RaiseException(Throwable cause, NativeException nativeException) {
        super(buildMessage(cause), cause);
        providedMessage = buildMessage(cause);
        setException(nativeException, true);
        preRaise(nativeException.getRuntime().getCurrentContext(), nativeException.getCause().getStackTrace());
    }

    /**
     * Method still in use by jruby-openssl <= 0.5.2
     */
    public static RaiseException createNativeRaiseException(Ruby runtime, Throwable cause) {
        return createNativeRaiseException(runtime, cause, null);
    }

    public static RaiseException createNativeRaiseException(Ruby runtime, Throwable cause, Member target) {
        NativeException nativeException = new NativeException(runtime, runtime.getNativeException(), cause);

        // FIXME: someday, add back filtering of reflection/handle methods between JRuby and target

        return new RaiseException(cause, nativeException);
    }

    private static String buildMessage(Throwable exception) {
        StringBuilder sb = new StringBuilder();
        StringWriter stackTrace = new StringWriter();
        exception.printStackTrace(new PrintWriter(stackTrace));
    
        sb.append("Native Exception: '").append(exception.getClass()).append("'; ");
        sb.append("Message: ").append(exception.getMessage()).append("; ");
        sb.append("StackTrace: ").append(stackTrace.getBuffer().toString());

        return sb.toString();
    }

    @Override
    public String getMessage() {
        if (providedMessage == null) {
            providedMessage = "(" + exception.getMetaClass().getBaseName() + ") " + exception.message(exception.getRuntime().getCurrentContext()).asJavaString();
        }
        return providedMessage;
    }

    /**
     * Gets the exception
     * @return Returns a RubyException
     */
    public RubyException getException() {
        return exception;
    }

    private void preRaise(ThreadContext context) {
        preRaise(context, (IRubyObject)null);
    }

    private void preRaise(ThreadContext context, StackTraceElement[] javaTrace) {
        context.runtime.incrementExceptionCount();
        doSetLastError(context);
        doCallEventHook(context);

        exception.prepareIntegratedBacktrace(context, javaTrace);

        if (RubyInstanceConfig.LOG_EXCEPTIONS) TraceType.dumpException(exception);
    }

    private void preRaise(ThreadContext context, IRubyObject backtrace) {
        context.runtime.incrementExceptionCount();
        doSetLastError(context);
        doCallEventHook(context);
        
        if (backtrace == null) {
            exception.prepareBacktrace(context, nativeException);
        } else {
            exception.forceBacktrace(backtrace);
        }

        // call Throwable.setStackTrace so that when RaiseException appears nested inside another exception,
        // Ruby stack trace gets displayed

        // JRUBY-2673: if wrapping a NativeException, use the actual Java exception's trace as our Java trace
        if (exception instanceof NativeException) {
            setStackTrace(((NativeException)exception).getCause().getStackTrace());
        } else {
            setStackTrace(javaTraceFromRubyTrace(exception.getBacktraceElements()));
        }

        if (RubyInstanceConfig.LOG_EXCEPTIONS) TraceType.dumpException(exception);
    }

    private void doCallEventHook(ThreadContext context) {
        if (context.runtime.hasEventHooks()) {
            context.runtime.callEventHooks(context, RubyEvent.RAISE, context.getFile(), context.getLine(), context.getFrameName(), context.getFrameKlazz());
        }
    }

    private void doSetLastError(ThreadContext context) {
        context.runtime.getGlobalVariables().set("$!", exception);
    }
    
    /**
     * Sets the exception
     * @param newException The exception to set
     */
    protected void setException(RubyException newException, boolean nativeException) {
        this.exception = newException;
        this.nativeException = nativeException;
    }

    private StackTraceElement[] javaTraceFromRubyTrace(RubyStackTraceElement[] trace) {
        StackTraceElement[] newTrace = new StackTraceElement[trace.length];
        for (int i = 0; i < newTrace.length; i++) {
            newTrace[i] = trace[i].getElement();
        }
        return newTrace;
    }
}
