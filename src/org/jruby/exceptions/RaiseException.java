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
import org.jruby.RubyString;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class RaiseException extends JumpException {
    public static final boolean DEBUG = false;
    private static final long serialVersionUID = -7612079169559973951L;
    
    private RubyException exception;
    private String providedMessage;

    public RaiseException(RubyException actException) {
        this(actException, false);
    }

    public RaiseException(Ruby runtime, RubyClass excptnClass, String msg, boolean nativeException) {
        super(msg);
        if (msg == null) {
            msg = "No message available";
        }
        providedMessage = msg;
        if (DEBUG) {
            Thread.dumpStack();
        }
        setException((RubyException)RuntimeHelpers.invoke(
                runtime.getCurrentContext(),
                excptnClass,
                "new",
                RubyString.newUnicodeString(excptnClass.getRuntime(), msg)),
                nativeException);
    }

    public RaiseException(RubyException exception, boolean isNativeException) {
        super();
        if (DEBUG) {
            Thread.dumpStack();
        }
        setException(exception, isNativeException);
    }

    /**
     * Method still in use by jruby-openssl <= 0.5.2
     */
    public static RaiseException createNativeRaiseException(Ruby runtime, Throwable cause) {
        return createNativeRaiseException(runtime, cause, null);
    }

    public static RaiseException createNativeRaiseException(Ruby runtime, Throwable cause, Member target) {
        NativeException nativeException = new NativeException(runtime, runtime.getClass(NativeException.CLASS_NAME), cause);
        if (!runtime.getDebug().isTrue()) {
            nativeException.trimStackTrace(target);
        }
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

    public RaiseException(Throwable cause, NativeException nativeException) {
        super(buildMessage(cause), cause);
        providedMessage = buildMessage(cause);
        setException(nativeException, true);
    }

    @Override
    public String getMessage() {
        if (providedMessage == null) {
            providedMessage = exception.message(exception.getRuntime().getCurrentContext()).asJavaString();
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

    /**
     * Sets the exception
     * @param newException The exception to set
     */
    protected void setException(RubyException newException, boolean nativeException) {
        Ruby runtime = newException.getRuntime();
        ThreadContext context = runtime.getCurrentContext();

        if (!context.isWithinDefined()) {
            runtime.getGlobalVariables().set("$!", newException);
        }

        if (runtime.hasEventHooks()) {
            runtime.callEventHooks(
                    context,
                    RubyEvent.RAISE,
                    context.getFile(),
                    context.getLine(),
                    context.getFrameName(),
                    context.getFrameKlazz());
        }

        this.exception = newException;

        if (runtime.getStackTraces() > 5) {
            return;
        }

        runtime.setStackTraces(runtime.getStackTraces() + 1);

        ThreadContext.RubyStackTraceElement[] stackTrace = newException.getBacktraceFrames();
        if (stackTrace == null) {
            stackTrace = context.createBacktrace2(0, nativeException);
            newException.setBacktraceFrames(stackTrace);
        }

        // JRUBY-2673: if wrapping a NativeException, use the actual Java exception's trace as our Java trace
        if (newException instanceof NativeException) {
            setStackTrace(((NativeException)newException).getCause().getStackTrace());
        } else {
            setStackTrace(javaTraceFromRubyTrace(stackTrace));
        }

        runtime.setStackTraces(runtime.getStackTraces() - 1);
    }

    private StackTraceElement[] javaTraceFromRubyTrace(ThreadContext.RubyStackTraceElement[] trace) {
        StackTraceElement[] newTrace = new StackTraceElement[trace.length];
        for (int i = 0; i < newTrace.length; i++) {
            newTrace[i] = trace[i].getElement();
        }
        return newTrace;
    }

    @Override
    public void printStackTrace() {
        printStackTrace(System.err);
    }
    
    @Override
    public void printStackTrace(PrintStream ps) {
        StackTraceElement[] trace = getStackTrace();
        int externalIndex = 0;
        for (int i = trace.length - 1; i > 0; i--) {
            if (trace[i].getClassName().indexOf("org.jruby.evaluator") >= 0) {
                break;
            }
            externalIndex = i;
        }
        IRubyObject backtrace = exception.backtrace();
        Ruby runtime = backtrace.getRuntime();
        if (runtime.getNil() != backtrace) {
            String firstLine = backtrace.callMethod(runtime.getCurrentContext(), "first").convertToString().toString();
            ps.print(firstLine + ": ");
        }

        IRubyObject message = exception.message;
        ps.println((message.isNil() ? "" : message.convertToString()) + " (" + exception.getMetaClass().toString() + ")");
        exception.printBacktrace(ps);
        ps.println("\t...internal jruby stack elided...");
        for (int i = externalIndex; i < trace.length; i++) {
            ps.println("\tfrom " + trace[i].toString());
        }
    }
    
    @Override
    public void printStackTrace(PrintWriter pw) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        printStackTrace(new PrintStream(baos));
        pw.print(baos.toString());
    }
}
