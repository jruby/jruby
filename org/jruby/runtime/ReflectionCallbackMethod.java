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
public final class ReflectionCallbackMethod implements Callback {
    private Class klass = null;
    private String methodName = null;
    private Class[] args = null;
    private boolean isStaticMethod = false;
    private boolean isRestArgs = false;
    private Arity arity;

    private Method method = null;

    public ReflectionCallbackMethod(
        Class klass,
        String methodName,
        Class[] args,
        boolean isRestArgs,
        boolean isStaticMethod,
        Arity arity) {
        super();

        this.klass = klass;
        this.methodName = methodName;
        this.args = args != null ? args : new Class[0];
        this.isRestArgs = isRestArgs;
        this.isStaticMethod = isStaticMethod;
        this.arity = arity;
    }

    public Arity getArity() {
        return arity;
    }

    protected Method getMethod() {
        if (method == null) {
            try {
                Class[] newArgs = args;
                if (isStaticMethod) {
                    newArgs = new Class[args.length + 1];
                    System.arraycopy(args, 0, newArgs, 1, args.length);
                    newArgs[0] = IRubyObject.class;
                }
                method = klass.getMethod(methodName, newArgs);
            } catch (NoSuchMethodException nsmExcptn) {
                throw new RuntimeException(
                    "NoSuchMethodException: Cannot get method \""
                        + methodName
                        + "\" in class \""
                        + klass.getName()
                        + "\" by Reflection.");
            } catch (SecurityException sExcptn) {
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

    protected void testArgsCount(Ruby ruby, IRubyObject[] methodArgs) {
        if (isRestArgs) {
            if (methodArgs.length < (args.length - 1)) {
                throw new ArgumentError(ruby, getExpectedArgsString(methodArgs));
            }
        } else {
            if (methodArgs.length != args.length) {
                throw new ArgumentError(ruby, getExpectedArgsString(methodArgs));
            }
        }
    }

    protected IRubyObject invokeMethod(IRubyObject recv, Object[] methodArgs) {
        Object[] reflectionArguments = packageArgumentsForReflection(methodArgs, recv);
        try {
            return (IRubyObject) getMethod().invoke(isStaticMethod ? null : recv, reflectionArguments);
        } catch (InvocationTargetException itExcptn) {
            if (itExcptn.getTargetException() instanceof RaiseException) {
                throw (RaiseException) itExcptn.getTargetException();
            } else if (itExcptn.getTargetException() instanceof JumpException) {
                throw (JumpException) itExcptn.getTargetException();
            } else if (itExcptn.getTargetException() instanceof Exception) {
                recv.getRuntime().getJavaSupport().handleNativeException((Exception) itExcptn.getTargetException());
                return recv.getRuntime().getNil();
            } else {
                throw (Error)itExcptn.getTargetException();
            }
        } catch (IllegalAccessException iaExcptn) {
            StringBuffer message = new StringBuffer();
            message.append(iaExcptn.getMessage());
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
        args = (args != null) ? args : new IRubyObject[0];
        testArgsCount(recv.getRuntime(), args);
        // testArgsClass(args);
        return invokeMethod(recv, args);
    }

    private Object[] packageArgumentsForReflection(Object[] arguments, IRubyObject rubyClass) {
        Object[] result = arguments;
        if (isRestArgs) {
            result = packageRestArgumentsForReflection(result);
        }
        if (isStaticMethod) {
            result = packageStaticArgumentsForReflection(result, rubyClass);
        }
        return result;
    }

    private Object[] packageStaticArgumentsForReflection(Object[] arguments, IRubyObject rubyClass) {
        Object[] result = new Object[arguments.length + 1];
        result[0] = rubyClass;
        System.arraycopy(arguments, 0, result, 1, arguments.length);
        return result;
    }

    private final Object[] packageRestArgumentsForReflection(final Object[] originalArgs) {
        IRubyObject[] restArray = new IRubyObject[originalArgs.length - (args.length - 1)];
        Object[] result = new Object[args.length];
        try {
            System.arraycopy(originalArgs, args.length - 1, restArray, 0, originalArgs.length - (args.length - 1));
        } catch (ArrayIndexOutOfBoundsException aioobExcptn) {
            throw new RuntimeException(
                "Cannot call \""
                    + methodName
                    + "\" in class \""
                    + klass.getName()
                    + "\". "
                    + getExpectedArgsString((IRubyObject[]) originalArgs));
        }
        System.arraycopy(originalArgs, 0, result, 0, args.length - 1);
        result[args.length - 1] = restArray;
        return result;
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

        if (args.length == 0) {
            sb.append("no arguments excepted.");
        } else {
            sb.append("(");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    sb.append(",");
                }
                String className = args[i].getName();
                sb.append("a").append(className.substring(className.lastIndexOf(".Ruby") + 5));
            }
            if (isRestArgs) {
                sb.append(", ...");
            }
            sb.append(") excepted.");
        }

        return sb.toString();
    }
}
