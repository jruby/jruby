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

import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.Arity;
import org.jruby.runtime.CallbackFactory;
import org.jruby.Ruby;
import org.jruby.util.Asserts;
import org.jruby.util.AssertError;
import org.jruby.exceptions.ArgumentError;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.ThreadKill;

import java.lang.reflect.InvocationTargetException;

public abstract class AbstractCallback implements Callback {
    protected final Class klass;
    protected final String methodName;
    protected final Class[] argumentTypes;
    protected final boolean isRestArgs;
    protected final Arity arity;
    protected final CallType callType;

    public AbstractCallback(Class klass, String methodName, Class[] args, boolean isRestArgs, boolean isStaticMethod, Arity arity) {
        this.klass = klass;
        this.methodName = methodName;
        this.argumentTypes = args != null ? args : CallbackFactory.NULL_CLASS_ARRAY;
        this.isRestArgs = isRestArgs;
        this.arity = arity;
        this.callType = callType(isStaticMethod);
    }

    protected String getExpectedArgsString(IRubyObject[] methodArgs) {
        StringBuffer sb = new StringBuffer();
        sb.append("Wrong arguments:");

        if (methodArgs.length == 0) {
            sb.append(" No args");
        } else {
            sb.append(" (");
            for (int i = 0; i < methodArgs.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                String className = methodArgs[i].getType().getName();
                sb.append("a");
                if (className.charAt(0) == 'A'
                    || className.charAt(0) == 'E'
                    || className.charAt(0) == 'I'
                    || className.charAt(0) == 'O'
                    || className.charAt(0) == 'U') {
                    sb.append("n");
                }
                sb.append(className);
            }
            sb.append(")");
        }
        sb.append(" given, ");

        if (argumentTypes.length == 0) {
            sb.append("no arguments expected.");
        } else {
            sb.append("(");
            for (int i = 0; i < argumentTypes.length; i++) {
                if (i > 0) {
                    sb.append(",");
                }
                String className = argumentTypes[i].getName();
                sb.append("a").append(className.substring(className.lastIndexOf(".Ruby") + 5));
            }
            if (isRestArgs) {
                sb.append(", ...");
            }
            sb.append(") expected.");
        }

        return sb.toString();
    }

    protected final Object[] packageRestArgumentsForReflection(final Object[] originalArgs) {
        IRubyObject[] restArray = new IRubyObject[originalArgs.length - (argumentTypes.length - 1)];
        Object[] result = new Object[argumentTypes.length];
        try {
            System.arraycopy(originalArgs, argumentTypes.length - 1, restArray, 0, originalArgs.length - (argumentTypes.length - 1));
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new RuntimeException(
                    "Cannot call \""
                    + methodName
                    + "\" in class \""
                    + klass.getName()
                    + "\". "
                    + getExpectedArgsString((IRubyObject[]) originalArgs));
        }
        System.arraycopy(originalArgs, 0, result, 0, argumentTypes.length - 1);
        result[argumentTypes.length - 1] = restArray;
        return result;
    }

    protected void testArgsCount(Ruby runtime, IRubyObject[] methodArgs) {
        if (isRestArgs) {
            if (methodArgs.length < argumentTypes.length - 1) {
                throw new ArgumentError(runtime, getExpectedArgsString(methodArgs));
            }
        } else {
            if (methodArgs.length != argumentTypes.length) {
                throw new ArgumentError(runtime, getExpectedArgsString(methodArgs));
            }
        }
    }

    protected IRubyObject invokeMethod(IRubyObject recv, Object[] methodArgs) {
        try {
            return callType.invokeMethod(recv, methodArgs);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof RaiseException) {
                throw (RaiseException) e.getTargetException();
            } else if (e.getTargetException() instanceof JumpException) {
                throw (JumpException) e.getTargetException();
            } else if (e.getTargetException() instanceof ThreadKill) {
            	// allow it to bubble up
            	throw (ThreadKill)e.getTargetException();
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
            message.append(" klass=").append(klass.getName());
            message.append(" methodArgs=[");
            for (int i = 0; i < methodArgs.length; i++) {
                message.append(methodArgs[i]);
                message.append(' ');
            }
            message.append(']');
            Asserts.notReached(message.toString());
        } catch (final IllegalArgumentException iaExcptn) {
            throw new RaiseException(recv.getRuntime(), "TypeError", iaExcptn.getMessage());
        }
        throw new AssertError("[BUG] Run again with Asserts.ENABLE_ASSERT=true");
    }

    public IRubyObject execute(IRubyObject recv, IRubyObject[] args) {
        args = (args != null) ? args : IRubyObject.NULL_ARRAY;
        testArgsCount(recv.getRuntime(), args);
        return invokeMethod(recv, args);
    }

    public Arity getArity() {
        return arity;
    }

    protected abstract CallType callType(boolean isStaticMethod);


    protected abstract static class CallType {
        public abstract IRubyObject invokeMethod(IRubyObject recv, Object[] methodArgs)
                throws IllegalAccessException, InvocationTargetException;

        public abstract Class[] reflectionArgumentTypes();
    }
}
