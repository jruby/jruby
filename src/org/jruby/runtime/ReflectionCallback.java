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
package org.jruby.runtime;

import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.AssertError;
import org.jruby.util.Asserts;
import org.jruby.Ruby;
import org.jruby.exceptions.ArgumentError;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class ReflectionCallback implements Callback {
    private final Class klass;
    private final String methodName;
    private final Class[] argumentTypes;
    private final boolean isRestArgs;
    private final Arity arity;
    private final CallType callType;

    private Method method = null;

    public ReflectionCallback(
        Class klass,
        String methodName,
        Class[] args,
        boolean isRestArgs,
        boolean isStaticMethod,
        Arity arity)
    {
        this.klass = klass;
        this.methodName = methodName;
        this.argumentTypes = args != null ? args : CallbackFactory.NULL_CLASS_ARRAY;
        this.isRestArgs = isRestArgs;
        this.arity = arity;
        if (isStaticMethod) {
            this.callType = new StaticCallType();
        } else {
            this.callType = new InstanceCallType();
        }
    }

    public Arity getArity() {
        return arity;
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

    private void testArgsCount(Ruby ruby, IRubyObject[] methodArgs) {
        if (isRestArgs) {
            if (methodArgs.length < (argumentTypes.length - 1)) {
                throw new ArgumentError(ruby, getExpectedArgsString(methodArgs));
            }
        } else {
            if (methodArgs.length != argumentTypes.length) {
                throw new ArgumentError(ruby, getExpectedArgsString(methodArgs));
            }
        }
    }

    private IRubyObject invokeMethod(IRubyObject recv, Object[] methodArgs) {
        try {
            return callType.invokeMethod(recv, methodArgs);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof RaiseException) {
                throw (RaiseException) e.getTargetException();
            } else if (e.getTargetException() instanceof JumpException) {
                throw (JumpException) e.getTargetException();
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

    private String getExpectedArgsString(IRubyObject[] methodArgs) {
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
                String className = methodArgs[i].getType().toName();
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
            sb.append("no arguments excepted.");
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
            sb.append(") excepted.");
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

    private abstract class CallType {
        public abstract IRubyObject invokeMethod(IRubyObject recv, Object[] methodArgs)
                throws IllegalAccessException, InvocationTargetException;

        public abstract Class[] reflectionArgumentTypes();
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
