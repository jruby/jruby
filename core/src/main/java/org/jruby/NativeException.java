/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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

import java.io.PrintStream;

import java.lang.reflect.Member;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.Java;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;

@JRubyClass(name = "NativeException", parent = "RuntimeError")
public class NativeException extends RubyException {

    private final Throwable cause;
    public static final String CLASS_NAME = "NativeException";
    private final Ruby runtime;

    public NativeException(Ruby runtime, RubyClass rubyClass, Throwable cause) {
        super(runtime, rubyClass);
        this.runtime = runtime;
        this.cause = cause;
        this.message = runtime.newString(cause.getClass().getName() + ": " + searchStackMessage(cause));
    }
    
    private NativeException(Ruby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass);
        this.runtime = runtime;
        this.cause   = new Throwable();
        this.message = runtime.newString();
    }
    
    private static ObjectAllocator NATIVE_EXCEPTION_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
            NativeException instance = new NativeException(runtime, klazz);
            instance.setMetaClass(klazz);
            return instance;
        }
    };

    public static RubyClass createClass(Ruby runtime, RubyClass baseClass) {
        RubyClass exceptionClass = runtime.defineClass(CLASS_NAME, baseClass, NATIVE_EXCEPTION_ALLOCATOR);

        exceptionClass.defineAnnotatedMethods(NativeException.class);

        return exceptionClass;
    }

    @JRubyMethod
    public IRubyObject cause(Block unusedBlock) {
        return Java.getInstance(getRuntime(), cause);
    }

    public IRubyObject backtrace() {
        IRubyObject rubyTrace = super.backtrace();
        if (rubyTrace.isNil()) {
            return rubyTrace;
        }
        RubyArray array = (RubyArray) rubyTrace.dup();
        StackTraceElement[] stackTrace = cause.getStackTrace();
        for (int i = stackTrace.length - 1; i >= 0; i--) {
            StackTraceElement element = stackTrace[i];
            String className = element.getClassName();
            String line = null;
            if (element.getFileName() == null) {
                line = className + ":" + element.getLineNumber() + ":in `" + element.getMethodName() + "'";
            } else {
                int index = className.lastIndexOf(".");
                String packageName = null;
                if (index == -1) {
                    packageName = "";
                } else {
                    packageName = className.substring(0, index) + "/";
                }
                line = packageName.replace(".", "/") + element.getFileName() + ":" + element.getLineNumber() + ":in `" + element.getMethodName() + "'";
            }
            RubyString string = runtime.newString(line);
            array.unshift(string);
        }
        return array;
    }

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
            StackTraceElement[] newStackTrace =
                    new StackTraceElement[origStackTrace.length - skip];
            for (int i = 0; i < newStackTrace.length; ++i) {
                newStackTrace[i] = origStackTrace[i];
            }
            cause.setStackTrace(newStackTrace);
        }
    }

    public void printBacktrace(PrintStream errorStream) {
        super.printBacktrace(errorStream);
        if (getRuntime().getDebug().isTrue()) {
            errorStream.println("Complete Java stackTrace");
            cause.printStackTrace(errorStream);
        }
    }

    public Throwable getCause() {
        return cause;
    }

    private String searchStackMessage(Throwable cause) {
        String message = null;

        do {
            message = cause.getMessage();
            cause = cause.getCause();
        } while (message == null && cause != null);

        return message;
    }
}
