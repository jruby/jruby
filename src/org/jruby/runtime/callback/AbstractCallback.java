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
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2004 David Corbin <dcorbin@users.sourceforge.net>
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
    
    // FIXME temp variable
    protected final boolean new_style;
    
    public AbstractCallback(Class type, String name, Arity arity) {
        this.type = type;
        this.methodName = name;
        this.arity = arity;
        this.new_style = true; // FIXME
        this.argumentTypes = null;
        this.isRestArgs = false;
        this.isStaticMethod = false;
    }

    public AbstractCallback(Class type, String methodName, Class[] argumentTypes, boolean isRestArgs, boolean isStaticMethod, Arity arity) {
        this.type = type;
        this.methodName = methodName;
        this.argumentTypes = argumentTypes != null ? argumentTypes : CallbackFactory.NULL_CLASS_ARRAY;
        this.isRestArgs = isRestArgs;
        this.arity = arity;
        this.isStaticMethod = isStaticMethod;
        this.new_style = false; // FIXME
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
/*            StringBuffer message = new StringBuffer();
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
            message.append(']');*/
            assert false : e;
            return null;
        }
    }

	protected abstract IRubyObject invokeMethod0(IRubyObject recv, Object[] methodArgs)
			throws IllegalAccessException, InvocationTargetException;

	/**
     * Calls a wrapped Ruby method for the specified receiver with the specified arguments.
     */
    public IRubyObject execute(IRubyObject recv, IRubyObject[] args) {
        if (new_style) {
            assert recv != null;
            assert args != null;

            arity.checkArity(recv.getRuntime(), args);
            if (arity.isFixed()) {
                return invokeMethod(recv, args);
            }
            return invokeMethod(recv, new Object[]{args});
        }
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
