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
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.cli.Options;

import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.typeError;

public abstract class JavaCallable implements ParameterTypes {

    protected final Class<?>[] parameterTypes;

    private static final boolean REWRITE_JAVA_TRACE = Options.REWRITE_JAVA_TRACE.load();

    protected JavaCallable(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public final int getArity() { return parameterTypes.length; }

    public final Class<?>[] getParameterTypes() { return parameterTypes; }

    public abstract int getModifiers();
    public abstract Class<?>[] getExceptionTypes();
    public abstract Type[] getGenericExceptionTypes();
    public abstract Type[] getGenericParameterTypes();
    public abstract Annotation[][] getParameterAnnotations();
    public abstract boolean isVarArgs();
    public abstract String toGenericString();

    public abstract AccessibleObject accessibleObject();

    public abstract boolean equals(final Object other);
    public abstract int hashCode();

    @Override
    public String toString() {
        return accessibleObject().toString();
    }

    protected final void checkArity(ThreadContext context, final int length) {
        if ( length != getArity() ) throw argumentError(context, length, getArity());
    }

    protected final IRubyObject handleThrowable(ThreadContext context, final Throwable ex) {
        if ( ex instanceof JumpException ) {
            // RaiseException (from the Ruby side) is expected to already
            // have its stack-trace rewritten - no need to do it again ...
            throw (JumpException) ex;
        }

        if (REWRITE_JAVA_TRACE) {
            Helpers.rewriteStackTraceAndThrow(context, ex);
        }

        Helpers.throwException(ex);
        return null; // not reached
    }

    protected final IRubyObject handleInvocationTargetEx(ThreadContext context, InvocationTargetException ex) {
        return handleThrowable(context, ex.getTargetException()); // NOTE: we no longer unwrap
    }

    final IRubyObject handleIllegalAccessEx(ThreadContext context, final IllegalAccessException ex, Member target) throws RaiseException {
        throw typeError(context, "illegal access on '" + target.getName() + "': " + ex.getMessage());
    }

    final IRubyObject handleIllegalAccessEx(ThreadContext context, final IllegalAccessException ex, Constructor target)  throws RaiseException {
        throw typeError(context, "illegal access on constructor for type '" + target.getDeclaringClass().getSimpleName() + "': " + ex.getMessage());
    }

    final IRubyObject handlelIllegalArgumentEx(ThreadContext context, final IllegalArgumentException ex, Method target, Object... arguments) throws RaiseException {
        final StringBuilder msg = new StringBuilder(64);
        msg.append("for method ").append( target.getDeclaringClass().getSimpleName() )
           .append('.').append( target.getName() );
        msg.append(" expected "); dumpParameterTypes(msg);
        msg.append("; got: "); dumpArgTypes(arguments, msg);
        msg.append("; error: ").append( ex.getMessage() );
        throw typeError(context, msg.toString());
    }

    final IRubyObject handlelIllegalArgumentEx(ThreadContext context, final IllegalArgumentException ex, Constructor target, Object... arguments) throws RaiseException {
        return handlelIllegalArgumentEx(context, ex, target, true, arguments);
    }

    final IRubyObject handlelIllegalArgumentEx(ThreadContext context, final IllegalArgumentException ex, Constructor target, final boolean targetInfo, Object... arguments) throws RaiseException {
        final StringBuilder msg = new StringBuilder(64);
        if ( targetInfo ) {
            msg.append("for constructor of type ").append( target.getDeclaringClass().getSimpleName() );
        }
        msg.append(" expected "); dumpParameterTypes(msg);
        msg.append("; got: "); dumpArgTypes(arguments, msg);
        msg.append("; error: ").append( ex.getMessage() );
        throw typeError(context, msg.toString());
    }

    private void dumpParameterTypes(final StringBuilder str) {
        str.append('[');
        inspectParameterTypes(str, this, false);
        str.append(']');
    }

    static CharSequence dumpArgTypes(final Object[] args, final StringBuilder str) {
        str.append('[');
        for ( int i = 0; i < args.length; i++ ) {
            if ( i > 0 ) str.append(',');
            if ( args[i] == null ) str.append("null");
            else str.append( args[i].getClass().getName() );
        }
        str.append(']');
        return str;
    }

    public static StringBuilder inspectParameterTypes(
        final StringBuilder str, final ParameterTypes target) {
        return inspectParameterTypes(str, target, true);
    }

    private static StringBuilder inspectParameterTypes(
        final StringBuilder str, final ParameterTypes target, final boolean brackets) {
        if ( brackets ) str.append('(');
        final Class<?>[] types = target.getParameterTypes();
        for ( int i = 0; i < types.length; i++ ) {
            str.append( types[i].getName() );
            if ( i < types.length - 1 ) str.append(',');
        }
        if ( brackets ) str.append(')');
        return str;
    }

}
