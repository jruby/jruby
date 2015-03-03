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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 David Corbin <dcorbin@users.sourceforge.net>
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
package org.jruby.javasupport;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.cli.Options;
import static org.jruby.javasupport.JavaClass.toRubyArray;

public abstract class JavaCallable extends JavaAccessibleObject implements ParameterTypes {
    protected final Class<?>[] parameterTypes;

    private static final boolean REWRITE_JAVA_TRACE = Options.REWRITE_JAVA_TRACE.load();

    public JavaCallable(Ruby runtime, RubyClass rubyClass, Class<?>[] parameterTypes) {
        super(runtime, rubyClass);
        this.parameterTypes = parameterTypes;
    }

    public static void registerRubyMethods(Ruby runtime, RubyClass result) {
        result.defineAnnotatedMethods(JavaCallable.class);
    }

    public abstract int getArity();
    public abstract int getModifiers();
    public abstract Class<?>[] getParameterTypes();
    public abstract Class<?>[] getExceptionTypes();
    public abstract Type[] getGenericExceptionTypes();
    public abstract Type[] getGenericParameterTypes();
    public abstract Annotation[][] getParameterAnnotations();
    public abstract boolean isVarArgs();
    public abstract String toGenericString();
    /**
     * @return the name used in the head of the string returned from inspect()
     */
    protected abstract String nameOnInspection();

    @JRubyMethod
    public final RubyFixnum arity() {
        return getRuntime().newFixnum(getArity());
    }

    @JRubyMethod
    public final RubyArray argument_types() {
        return toRubyArray(getRuntime(), getParameterTypes());
    }

    // same as argument_types, but matches name in java.lang.reflect.Constructor/Method
    @JRubyMethod
    public RubyArray parameter_types() {
        return toRubyArray(getRuntime(), getParameterTypes());
    }

    @JRubyMethod
    public RubyArray exception_types() {
        return toRubyArray(getRuntime(), getExceptionTypes());
    }

    @JRubyMethod
    public IRubyObject generic_parameter_types() {
        return Java.getInstance(getRuntime(), getGenericParameterTypes());
    }

    @JRubyMethod
    public IRubyObject generic_exception_types() {
        return Java.getInstance(getRuntime(), getGenericExceptionTypes());
    }

    @JRubyMethod
    public IRubyObject parameter_annotations() {
        return Java.getInstance(getRuntime(), getParameterAnnotations());
    }

    @JRubyMethod(name = "varargs?")
    public RubyBoolean varargs_p() {
        return getRuntime().newBoolean(isVarArgs());
    }

    @JRubyMethod
    public RubyString to_generic_string() {
        return getRuntime().newString(toGenericString());
    }

    @JRubyMethod
    public IRubyObject inspect() {
        StringBuilder result = new StringBuilder();
        result.append(nameOnInspection());
        Class<?>[] parameterTypes = getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            result.append(parameterTypes[i].getName());
            if (i < parameterTypes.length - 1) {
                result.append(',');
            }
        }
        result.append(")>");
        return getRuntime().newString(result.toString());
    }


    @JRubyMethod(name = "public?")
    public RubyBoolean public_p() {
        return RubyBoolean.newBoolean(getRuntime(), Modifier.isPublic(getModifiers()));
    }

    protected void checkArity(int length) {
        if (length != getArity()) {
            throw getRuntime().newArgumentError(length, getArity());
        }
    }

    protected static String dumpArgTypes(Object[] arguments) {
        StringBuilder str = new StringBuilder("[");
        for (int i = 0; i < arguments.length; i++) {
            if (i > 0) {
                str.append(",");
            }
            if (arguments[i] == null) {
                str.append("null");
            } else {
                str.append(arguments[i].getClass().getName());
            }
        }
        str.append("]");
        return str.toString();
    }

    protected IRubyObject handleThrowable(ThreadContext context, Throwable t) {
        if (REWRITE_JAVA_TRACE) {
            Helpers.rewriteStackTraceAndThrow(context, t);
        }

        Helpers.throwException(t);
        // not reached
        return getRuntime().getNil();
    }

    protected IRubyObject handleInvocationTargetEx(ThreadContext context, InvocationTargetException ite) {
        return handleThrowable(context, ite.getTargetException());
    }
}
