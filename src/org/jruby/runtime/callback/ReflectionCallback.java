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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jruby.runtime.Arity;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A wrapper for <code>java.lang.reflect.Method</code> objects which implement Ruby methods.
 */
public class ReflectionCallback extends AbstractCallback {
    private final Method method;

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