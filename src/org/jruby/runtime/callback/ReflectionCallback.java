/*
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002 Chad Fowler <chadfowler@yahoo.com>
 * Copyright (C) 2002 Anders Bengtsson
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This file is part of JRuby
 * 
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */
package org.jruby.runtime.callback;

import org.jruby.runtime.Arity;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 *
 * @author  jpetersen, Anders
 * @version $Revision$
 */
public class ReflectionCallback extends AbstractCallback {
    private Method method = null;

    public ReflectionCallback(
        Class klass,
        String methodName,
        Class[] args,
        boolean isRestArgs,
        boolean isStaticMethod,
        Arity arity)
    {
        super(klass, methodName, args, isRestArgs, isStaticMethod, arity);
    }

    protected CallType callType(boolean isStaticMethod) {
        if (isStaticMethod) {
            return new StaticCallType();
        }
		return new InstanceCallType();
    }

    private Method getMethod() {
        if (method == null) {
            try {
                method = klass.getMethod(methodName, callType.reflectionArgumentTypes());
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(
                    "NoSuchMethodException: Cannot get method \""
                        + methodName
                        + "\" in class \""
                        + klass.getName()
                        + "\" by Reflection.");
            } catch (SecurityException e) {
                throw new RuntimeException(
                    "SecurityException: Cannot get method \""
                        + methodName
                        + "\" in class \""
                        + klass.getName()
                        + "\" by Reflection.");
            }
        }
        return method;
    }

    private class StaticCallType extends CallType {
        public IRubyObject invokeMethod(IRubyObject recv, Object[] arguments)
                throws IllegalAccessException, InvocationTargetException
        {
            if (isRestArgs) {
                arguments = packageRestArgumentsForReflection(arguments);
            }
            Object[] result = new Object[arguments.length + 1];
            System.arraycopy(arguments, 0, result, 1, arguments.length);
            result[0] = recv;
            return (IRubyObject) getMethod().invoke(null, result);
        }

        public Class[] reflectionArgumentTypes() {
            Class[] result = new Class[argumentTypes.length + 1];
            System.arraycopy(argumentTypes, 0, result, 1, argumentTypes.length);
            result[0] = IRubyObject.class;
            return result;
        }
    }

    private class InstanceCallType extends CallType {
        public IRubyObject invokeMethod(IRubyObject recv, Object[] arguments)
                throws IllegalAccessException, InvocationTargetException
        {
            if (isRestArgs) {
                arguments = packageRestArgumentsForReflection(arguments);
            }
            return (IRubyObject) getMethod().invoke(recv, arguments);
        }

        public Class[] reflectionArgumentTypes() {
            return argumentTypes;
        }
    }
}
