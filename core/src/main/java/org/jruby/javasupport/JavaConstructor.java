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
 * Copyright (C) 2001 Chad Fowler <chadfowler@chadfowler.com>
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
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
import java.lang.reflect.Type;

import org.jruby.Ruby;
import org.jruby.java.util.ClassUtils;
import org.jruby.runtime.ThreadContext;

public class JavaConstructor extends JavaCallable {

    private final Constructor<?> constructor;

    public final Constructor getValue() { return constructor; }

    JavaConstructor(Constructor<?> constructor) {
        super(constructor.getParameterTypes());
        this.constructor = constructor;
    }

    @Deprecated(since = "9.4.0.0")
    public static JavaConstructor create(Ruby runtime, Constructor<?> constructor) {
        return new JavaConstructor(constructor);
    }

    public static JavaConstructor wrap(Constructor<?> constructor) {
        return new JavaConstructor(constructor);
    }

    public static JavaConstructor getMatchingConstructor(final Ruby runtime,
        final Class<?> javaClass, final Class<?>[] argumentTypes) {
        Constructor c = ClassUtils.getMatchingConstructor(javaClass, argumentTypes);

        if (c == null) return null;

        return wrap(c);
    }

    @Override
    public final boolean equals(Object other) {
        return other instanceof JavaConstructor &&
            this.constructor.equals( ((JavaConstructor) other).constructor );
    }

    @Override
    public final int hashCode() {
        return constructor.hashCode();
    }

    @Override
    public final Class<?>[] getExceptionTypes() {
        return constructor.getExceptionTypes();
    }

    @Override
    public Type[] getGenericParameterTypes() {
        return constructor.getGenericParameterTypes();
    }

    @Override
    public Type[] getGenericExceptionTypes() {
        return constructor.getGenericExceptionTypes();
    }

    public Annotation[][] getParameterAnnotations() {
        return constructor.getParameterAnnotations();
    }

    @Override
    public final boolean isVarArgs() {
        return constructor.isVarArgs();
    }

    @Override
    public final int getModifiers() {
        return constructor.getModifiers();
    }

    public String toGenericString() {
        return constructor.toGenericString();
    }

    public Class<?> getDeclaringClass() {
        return constructor.getDeclaringClass();
    }

    public AccessibleObject accessibleObject() {
        return constructor;
    }

    public Object newInstanceDirect(ThreadContext context, Object... arguments) {
        checkArity(context, arguments.length);

        try {
            return constructor.newInstance(arguments);
        } catch (IllegalArgumentException iae) {
            return handlelIllegalArgumentEx(context, iae, constructor, arguments);
        } catch (IllegalAccessException iae) {
            return handleIllegalAccessEx(context, iae, constructor);
        } catch (InvocationTargetException ite) {
            return handleInvocationTargetEx(context, ite);
        } catch (Throwable t) {
            return handleThrowable(context, t);
        }
    }

    public Object newInstanceDirect(ThreadContext context) {
        checkArity(context, 0);

        try {
            return constructor.newInstance();
        } catch (IllegalArgumentException iae) {
            return handlelIllegalArgumentEx(context, iae, constructor);
        } catch (IllegalAccessException iae) {
            return handleIllegalAccessEx(context, iae, constructor);
        } catch (InvocationTargetException ite) {
            return handleInvocationTargetEx(context, ite);
        } catch (Throwable t) {
            return handleThrowable(context, t);
        }
    }

    public Object newInstanceDirect(ThreadContext context, Object arg0) {
        checkArity(context, 1);

        try {
            return constructor.newInstance(arg0);
        } catch (IllegalArgumentException iae) {
            return handlelIllegalArgumentEx(context, iae, constructor, arg0);
        } catch (IllegalAccessException iae) {
            return handleIllegalAccessEx(context, iae, constructor);
        } catch (InvocationTargetException ite) {
            return handleInvocationTargetEx(context, ite);
        } catch (Throwable t) {
            return handleThrowable(context, t);
        }
    }

    public Object newInstanceDirect(ThreadContext context, Object arg0, Object arg1) {
        checkArity(context, 2);

        try {
            return constructor.newInstance(arg0, arg1);
        } catch (IllegalArgumentException iae) {
            return handlelIllegalArgumentEx(context, iae, constructor, arg0, arg1);
        } catch (IllegalAccessException iae) {
            return handleIllegalAccessEx(context, iae, constructor);
        } catch (InvocationTargetException ite) {
            return handleInvocationTargetEx(context, ite);
        } catch (Throwable t) {
            return handleThrowable(context, t);
        }
    }

    public Object newInstanceDirect(ThreadContext context, Object arg0, Object arg1, Object arg2) {
        checkArity(context, 3);

        try {
            return constructor.newInstance(arg0, arg1, arg2);
        } catch (IllegalArgumentException iae) {
            return handlelIllegalArgumentEx(context, iae, constructor, arg0, arg1, arg2);
        } catch (IllegalAccessException iae) {
            return handleIllegalAccessEx(context, iae, constructor);
        } catch (InvocationTargetException ite) {
            return handleInvocationTargetEx(context, ite);
        } catch (Throwable t) {
            return handleThrowable(context, t);
        }
    }

    public Object newInstanceDirect(ThreadContext context, Object arg0, Object arg1, Object arg2, Object arg3) {
        checkArity(context, 4);

        try {
            return constructor.newInstance(arg0, arg1, arg2, arg3);
        } catch (IllegalArgumentException iae) {
            return handlelIllegalArgumentEx(context, iae, constructor, arg0, arg1, arg2, arg3);
        } catch (IllegalAccessException iae) {
            return handleIllegalAccessEx(context, iae, constructor);
        } catch (InvocationTargetException ite) {
            return handleInvocationTargetEx(context, ite);
        } catch (Throwable t) {
            return handleThrowable(context, t);
        }
    }

}
