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

import java.lang.reflect.*;

import org.jruby.*;
import org.jruby.exceptions.*;
import org.jruby.util.AssertError;
import org.jruby.util.Asserts;

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
    private int arity;

    private Method method = null;

    public ReflectionCallbackMethod(
        Class klass,
        String methodName,
        Class[] args,
        boolean isRestArgs,
        boolean isStaticMethod,
        int arity) {
        super();

        this.klass = klass;
        this.methodName = methodName;
        this.args = args != null ? args : new Class[0];
        this.isRestArgs = isRestArgs;
        this.isStaticMethod = isStaticMethod;
        this.arity = arity;
    }

    public int getArity() {
        return arity;
    }

    protected Method getMethod() {
        if (method == null) {
            try {
                Class[] newArgs = args;
                if (isStaticMethod) {
                    newArgs = new Class[args.length + 2];
                    System.arraycopy(args, 0, newArgs, 2, args.length);
                    newArgs[0] = Ruby.class;
                    newArgs[1] = RubyObject.class;
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

    protected final void testArgsCount(final Ruby ruby, final RubyObject[] methodArgs) {
        if (isRestArgs) {
            if (methodArgs.length < (args.length - 1)) {
                throw new ArgumentError(
                    ruby,
                    getExpectedArgsString(methodArgs));
            }
        } else {
            if (methodArgs.length != args.length) {
                throw new ArgumentError(
                    ruby,
                    getExpectedArgsString(methodArgs));
            }
        }
    }

    protected final RubyObject invokeMethod(
        final RubyObject recv,
        final Object[] methodArgs,
        final Ruby ruby) {
        final Object[] reflectionArguments =
            packageArgumentsForReflection(methodArgs, ruby, recv);
        try {
            return (RubyObject) getMethod().invoke(
                isStaticMethod ? null : recv,
                reflectionArguments);
        } catch (InvocationTargetException itExcptn) {
            if (itExcptn.getTargetException() instanceof RaiseException) {
                throw (RaiseException) itExcptn.getTargetException();
            } else if (itExcptn.getTargetException() instanceof JumpException) {
                throw (JumpException) itExcptn.getTargetException();
            } else {
                ruby.getJavaSupport().handleNativeException((Exception)itExcptn.getTargetException());
                return ruby.getNil();
            }
        } catch (final IllegalAccessException iaExcptn) {
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
            Asserts.assertNotReached(message.toString());
        } catch (final IllegalArgumentException iaExcptn) {
            throw new RaiseException(ruby, "TypeError", iaExcptn.getMessage());
        }
        throw new AssertError("[BUG] Run again with Asserts.ENABLE_ASSERT=true");
    }

    public final RubyObject execute(final RubyObject recv, RubyObject[] args, final Ruby ruby) {
        args = (args != null) ? args : new RubyObject[0];
        testArgsCount(ruby, args);
        // testArgsClass(args);
        return invokeMethod(recv, args, ruby);
    }

    private final Object[] packageArgumentsForReflection(
        final Object[] arguments,
        final Ruby ruby,
        final RubyObject rubyClass) {
        Object[] result = arguments;
        if (isRestArgs) {
            result = packageRestArgumentsForReflection(result);
        }
        if (isStaticMethod) {
            result =
                packageStaticArgumentsForReflection(result, ruby, rubyClass);
        }
        return result;
    }

    private final Object[] packageStaticArgumentsForReflection(
        final Object[] arguments,
        final Ruby ruby,
        final RubyObject rubyClass) {
        Object[] result = new Object[arguments.length + 2];
        result[0] = ruby;
        result[1] = rubyClass;
        System.arraycopy(arguments, 0, result, 2, arguments.length);
        return result;
    }

    private final Object[] packageRestArgumentsForReflection(final Object[] originalArgs) {
        RubyObject[] restArray =
            new RubyObject[originalArgs.length - (args.length - 1)];
        Object[] result = new Object[args.length];
        try {
            System.arraycopy(
                originalArgs,
                args.length - 1,
                restArray,
                0,
                originalArgs.length - (args.length - 1));
        } catch (ArrayIndexOutOfBoundsException aioobExcptn) {
            throw new RuntimeException(
                "Cannot call \""
                    + methodName
                    + "\" in class \""
                    + klass.getName()
                    + "\". "
                    + getExpectedArgsString((RubyObject[]) originalArgs));
        }
        System.arraycopy(originalArgs, 0, result, 0, args.length - 1);
        result[args.length - 1] = restArray;
        return result;
    }

    protected String getExpectedArgsString(RubyObject[] methodArgs) {
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
                String className = methodArgs[i].getClass().getName();
                className =
                    className.substring(className.lastIndexOf(".Ruby") + 5);
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
                sb.append("a").append(
                    className.substring(className.lastIndexOf(".Ruby") + 5));
            }
            if (isRestArgs) {
                sb.append(", ...");
            }
            sb.append(") excepted.");
        }

        return sb.toString();
    }
}
