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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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
package org.jruby.runtime.callback;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.jruby.runtime.Arity;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A wrapper for <code>java.lang.reflect.Method</code> objects which implement Ruby methods.
 */
public class ReflectionCallback extends AbstractCallback {
    private final Method method;

    public ReflectionCallback(Class type, String name, Arity arity) {
        super(type, name, arity);

        assert type != null;
        assert name != null;
        assert arity != null;

        Class[] parameterTypes;
        Method m = null;
        if (arity.isFixed()) {
            parameterTypes = new Class[arity.getValue()];
            Arrays.fill(parameterTypes, IRubyObject.class);
        } else {
            parameterTypes = new Class[1];
            parameterTypes[0] = IRubyObject[].class;
        }
        try {
            m = type.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            assert false : e;
        } catch (SecurityException e) {
            assert false : e;
        }
        this.method = m; 
    }

    public ReflectionCallback(Class type, String methodName, Class[] argumentTypes,
            boolean isRestArgs, boolean isStaticMethod, Arity arity) {
        super(type, methodName, argumentTypes, isRestArgs, isStaticMethod, arity);
        if (isStaticMethod) {
            Class[] types = new Class[argumentTypes.length + 1];
            System.arraycopy(argumentTypes, 0, types, 1, argumentTypes.length);
            types[0] = IRubyObject.class;
            argumentTypes = types;
        }
        try {
            this.method = type.getMethod(methodName, argumentTypes);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("NoSuchMethodException: Cannot get method \"" + methodName
                    + "\" in class \"" + type.getName() + "\" by Reflection.");
        } catch (SecurityException e) {
            throw new RuntimeException("SecurityException: Cannot get method \"" + methodName
                    + "\" in class \"" + type.getName() + "\" by Reflection.");
        }
    }

    protected IRubyObject invokeMethod0(IRubyObject recv, Object[] methodArgs)
            throws IllegalAccessException, InvocationTargetException {
        if (isStaticMethod) {
            Object[] args = new Object[methodArgs.length + 1];
            System.arraycopy(methodArgs, 0, args, 1, methodArgs.length);
            args[0] = recv;
            recv = null;
            methodArgs = args;
        }
        return (IRubyObject) method.invoke(recv, methodArgs);
    }
}
