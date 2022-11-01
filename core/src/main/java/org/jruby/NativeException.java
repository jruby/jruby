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
 * Copyright (C) 2005 David Corbin <dcorbin@users.sourceforge.net>
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

package org.jruby;

import java.lang.reflect.Member;

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.Java;
import org.jruby.runtime.Block;
import org.jruby.runtime.builtin.IRubyObject;

@Deprecated
@JRubyClass(name = "NativeException", parent = "RuntimeError")
public class NativeException extends RubyException {

    private final Throwable cause;
    private final String messageAsJavaString;
    public static final String CLASS_NAME = "NativeException";

    public NativeException(Ruby runtime, RubyClass rubyClass, Throwable cause) {
        this(runtime, rubyClass, cause, buildMessage(cause));
    }

    private NativeException(Ruby runtime, RubyClass rubyClass, Throwable cause, String message) {
        super(runtime, rubyClass, message);
        this.cause = cause;
        String s = buildMessage(cause);
        this.messageAsJavaString = message;
    }

    private static String buildMessage(Throwable cause) {
        return cause.getClass().getName() + ": " + searchStackMessage(cause);
    }

    private NativeException(Ruby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass, null);
        this.cause = new Throwable();
        this.messageAsJavaString = null;
    }

    public static RubyClass createClass(Ruby runtime, RubyClass baseClass) {
        RubyClass exceptionClass = runtime.defineClass(CLASS_NAME, baseClass, NativeException::new);
        runtime.getObject().deprecateConstant(runtime, CLASS_NAME);

        exceptionClass.defineAnnotatedMethods(NativeException.class);

        return exceptionClass;
    }

    @JRubyMethod
    public final IRubyObject cause() {
        return Java.getInstance(getRuntime(), getCause());
    }

    @Deprecated
    public final IRubyObject cause(Block unusedBlock) {
        return cause();
    }

    @Override
    public final IRubyObject backtrace() {
        IRubyObject rubyTrace = super.backtrace();
        if ( rubyTrace.isNil() ) return rubyTrace;
        final Ruby runtime = getRuntime();
        final RubyArray rTrace = (RubyArray) rubyTrace;
        StackTraceElement[] jTrace = cause.getStackTrace();

        // NOTE: with the new filtering ruby trace will already include the source (Java) part
        if ( rTrace.size() > 0 && jTrace.length > 0 ) {
            final String r0 = rTrace.eltInternal(0).toString();
            // final StackTraceElement j0 = jTrace[0];
            final String method = jTrace[0].getMethodName();
            final String file = jTrace[0].getFileName();
            if ( method != null && file != null &&
                r0.indexOf(method) != -1 && r0.indexOf(file) != -1 ) {
                return rTrace; // as is
            }
        }
        // so join-ing is mostly unnecessary, but just in case (due compatibility) make sure :

        return joinedBacktrace(runtime, rTrace, jTrace);
    }

    private static RubyArray joinedBacktrace(final Ruby runtime, final RubyArray rTrace, final StackTraceElement[] jTrace) {
        final IRubyObject[] trace = new IRubyObject[jTrace.length + rTrace.size()];
        final StringBuilder line = new StringBuilder(32);
        for ( int i = 0; i < jTrace.length; i++ ) {
            StackTraceElement element = jTrace[i];
            final String className = element.getClassName();
            line.setLength(0);
            if (element.getFileName() == null) {
                line.append(className).append(':').append(element.getLineNumber()).append(":in `").append(element.getMethodName()).append('\'');
            } else {
                final int index = className.lastIndexOf('.');
                if ( index > - 1 ) {
                    line.append(className.substring(0, index).replace('.', '/'));
                    line.append('/');
                }
                line.append(element.getFileName()).append(':').append(element.getLineNumber()).append(":in `").append(element.getMethodName()).append('\'');
            }
            trace[i] = RubyString.newString(runtime, line.toString());
        }
        System.arraycopy(rTrace.toJavaArrayMaybeUnsafe(), 0, trace, jTrace.length, rTrace.size());
        return RubyArray.newArrayMayCopy(runtime, trace);
    }

    @Deprecated // not used
    public void trimStackTrace(Member target) {
        Throwable t = new Throwable();
        StackTraceElement[] origStackTrace = cause.getStackTrace();
        StackTraceElement[] currentStackTrace = t.getStackTrace();
        int skip = 0;
        for (int i = 1;
                i <= origStackTrace.length && i <= currentStackTrace.length;
                ++i) {
            StackTraceElement a = origStackTrace[origStackTrace.length - i];
            StackTraceElement b = currentStackTrace[currentStackTrace.length - i];
            if (a.equals(b)) {
                skip += 1;
            } else {
                break;
            }
        }
        // If we know what method was being called, strip everything
        // before the call. This hides the JRuby and reflection internals.
        if (target != null) {
            String className = target.getDeclaringClass().getName();
            String methodName = target.getName();
            for (int i = origStackTrace.length - skip - 1; i >= 0; --i) {
                StackTraceElement frame = origStackTrace[i];
                if (frame.getClassName().equals(className) &&
                    frame.getMethodName().equals(methodName)) {
                    skip = origStackTrace.length - i - 1;
                    break;
                }
            }
        }
        if (skip > 0) {
            final int len = origStackTrace.length - skip;
            StackTraceElement[] newStackTrace = new StackTraceElement[len];
            System.arraycopy(origStackTrace, 0, newStackTrace, 0, len);
            cause.setStackTrace(newStackTrace);
        }
    }

    @Override
    public final IRubyObject getMessage() {
        if (message == null) {
            if (messageAsJavaString == null) {
                return message = getRuntime().getNil();
            }
            return message = getRuntime().newString(messageAsJavaString);
        }
        return message;
    }

    @Override
    public final String getMessageAsJavaString() {
        return messageAsJavaString;
    }

    public final Throwable getCause() {
        return cause;
    }

    private static String searchStackMessage(Throwable cause) {
        String message;
        do {
            message = cause.getMessage();
            if ( message != null ) return message;
            cause = cause.getCause();
        } while ( cause != null );
        return null;
    }
}
