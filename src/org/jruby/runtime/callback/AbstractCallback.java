/*
 *  Copyright (C) 2004 Charles O Nutter
 * 
 * Charles O Nutter <headius@headius.com>
 *
 *  JRuby - http://jruby.sourceforge.net
 *
 *  This file is part of JRuby
 *
 *  JRuby is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  JRuby is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with JRuby; if not, write to
 *  the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA  02111-1307 USA
 */
package org.jruby.runtime.callback;

import java.lang.reflect.InvocationTargetException;

import org.jruby.Ruby;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.ThreadKill;
import org.jruby.runtime.Arity;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A wrapper for <code>java.lang.reflect.Method</code> objects which implement Ruby methods.
 * The public methods are {@link #execute execute()} and {@link #getArity getArity()}.
 * Before really calling the Ruby method (via {@link #invokeMethod invokeMethod()}), the arity
 * is checked and an {@link org.jruby.exceptions.ArgumentError ArgumentError} is raised if the
 * number of arguments doesn't match the number of expected arguments.  Furthermore, rest
 * arguments are collected in a single IRubyObject array.
 */
public abstract class AbstractCallback implements Callback {
    protected final Class type;
    protected final String methodName;
    protected final Class[] argumentTypes;
    protected final boolean isRestArgs;
    protected final Arity arity;
    protected final boolean isStaticMethod;

    public AbstractCallback(Class type, String methodName, Class[] argumentTypes, boolean isRestArgs, boolean isStaticMethod, Arity arity) {
        this.type = type;
        this.methodName = methodName;
        this.argumentTypes = argumentTypes != null ? argumentTypes : CallbackFactory.NULL_CLASS_ARRAY;
        this.isRestArgs = isRestArgs;
        this.arity = arity;
        this.isStaticMethod = isStaticMethod;
    }
    
    /**
     * Returns an object array that collects all rest arguments in its own object array which
     * is then put into the last slot of the first object array.  That is, assuming that this
     * callback expects one required argument and any number of rest arguments, an input of
     * <code>[1, 2, 3]</code> is transformed into <code>[1, [2, 3]]</code>.  
     */
    protected final Object[] packageRestArgumentsForReflection(final Object[] originalArgs) {
        IRubyObject[] restArray = new IRubyObject[originalArgs.length - (argumentTypes.length - 1)];
        Object[] result = new Object[argumentTypes.length];
        try {
            System.arraycopy(originalArgs, argumentTypes.length - 1, restArray, 0, originalArgs.length - (argumentTypes.length - 1));
        } catch (ArrayIndexOutOfBoundsException e) {
            assert false : e;
        	return null;
        }
        System.arraycopy(originalArgs, 0, result, 0, argumentTypes.length - 1);
        result[argumentTypes.length - 1] = restArray;
        return result;
    }

    /**
     * Invokes the Ruby method. Actually, this methods delegates to an internal version
     * that may throw the usual Java reflection exceptions.  Ruby exceptions are rethrown, 
     * other exceptions throw an AssertError and abort the execution of the Ruby program.
     * They should never happen.
     */
    protected IRubyObject invokeMethod(IRubyObject recv, Object[] methodArgs) {
    	if (isRestArgs) {
    		methodArgs = packageRestArgumentsForReflection(methodArgs);
    	}
        try {
        	return invokeMethod0(recv, methodArgs);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof RaiseException) {
                throw (RaiseException) e.getTargetException();
            } else if (e.getTargetException() instanceof JumpException) {
                throw (JumpException) e.getTargetException();
            } else if (e.getTargetException() instanceof ThreadKill) {
            	// allow it to bubble up
            	throw (ThreadKill) e.getTargetException();
            } else if (e.getTargetException() instanceof Exception) {
                recv.getRuntime().getJavaSupport().handleNativeException(e.getTargetException());
                return recv.getRuntime().getNil();
            } else {
                throw (Error) e.getTargetException();
            }
        } catch (IllegalAccessException e) {
            StringBuffer message = new StringBuffer();
            message.append(e.getMessage());
            message.append(':');
            message.append(" methodName=").append(methodName);
            message.append(" recv=").append(recv.toString());
            message.append(" type=").append(type.getName());
            message.append(" methodArgs=[");
            for (int i = 0; i < methodArgs.length; i++) {
                message.append(methodArgs[i]);
                message.append(' ');
            }
            message.append(']');
            assert false : message.toString();
            return null;
        } catch (final IllegalArgumentException e) {
            throw new RaiseException(recv.getRuntime(), "TypeError", e.getMessage());
        }
    }

	protected abstract IRubyObject invokeMethod0(IRubyObject recv, Object[] methodArgs)
			throws IllegalAccessException, InvocationTargetException;

	/**
     * Calls a wrapped Ruby method for the specified receiver with the specified arguments.
     */
    public IRubyObject execute(IRubyObject recv, IRubyObject[] args) {
        args = (args != null) ? args : IRubyObject.NULL_ARRAY;
        arity.checkArity(recv.getRuntime(), args);
        return invokeMethod(recv, args);
    }

    /**
     * Returns the arity of the wrapped Ruby method.
     */
    public Arity getArity() {
        return arity;
    }
}
