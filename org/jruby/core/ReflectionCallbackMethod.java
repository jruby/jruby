/*
 * ReflectionCallbackMethod.java - No description
 * Created on 17. September 2001, 23:50
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
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

package org.jruby.core;

import java.lang.reflect.*;

import org.jruby.*;
import org.jruby.exceptions.*;

/**
 *
 * @author  jpetersen
 * @version 
 */
public class ReflectionCallbackMethod implements RubyCallbackMethod {
    private Class klass = null;
    private String methodName = null;
    private Class[] args = null;
    private boolean staticMethod = false;
    private boolean restArgs = false;
    
    private Method method = null;

    public ReflectionCallbackMethod(Class klass, String methodName) {
        this(klass, methodName, (Class[])null, false, false);
    }
    
    public ReflectionCallbackMethod(Class klass, String methodName, Class args) {
        this(klass, methodName, new Class[] {args}, false, false);
    }
    
    public ReflectionCallbackMethod(Class klass, String methodName, Class[] args) {
        this(klass, methodName, args, false, false);
    }
    
    public ReflectionCallbackMethod(Class klass, String methodName, boolean restArgs) {
        this(klass, methodName, (Class[])null, false, false);
    }
    
    public ReflectionCallbackMethod(Class klass, String methodName, Class args, boolean restArgs) {
        this(klass, methodName, new Class[] {args}, restArgs, false);
    }
    
    public ReflectionCallbackMethod(Class klass, String methodName, Class[] args, boolean restArgs) {
        this(klass, methodName, args, restArgs, false);
    }
    
    public ReflectionCallbackMethod(Class klass, String methodName, boolean restArgs, boolean staticMethod) {
        this(klass, methodName, (Class[])null, restArgs, staticMethod);
    }

    public ReflectionCallbackMethod(Class klass, String methodName, Class args, boolean restArgs, boolean staticMethod) {
        this(klass, methodName, new Class[] {args}, restArgs, staticMethod);
    }
    
    public ReflectionCallbackMethod(Class klass, String methodName, Class[] args, boolean restArgs, boolean staticMethod) {
        super();
        
        this.klass = klass;
        this.methodName = methodName;
        this.args = args;
        this.restArgs = restArgs;
        this.staticMethod = staticMethod;
    }
    
    protected Method getMethod() {
        if (method == null) {
            try {
                Class[] newArgs = args;
                if (staticMethod) {
                    newArgs = new Class[args.length + 1];
                    System.arraycopy(args, 0, newArgs, 1, args.length);
                    newArgs[0] = Ruby.class;
                }
                method = klass.getMethod(methodName, newArgs);
            } catch (NoSuchMethodException nsmExcptn) {
                throw new RuntimeException("NoSuchMethodException: Cannot get method \"" + methodName + 
                        "\" in class \"" + klass.getName() + "\" by Reflection.");
            } catch (SecurityException sExcptn) {
                throw new RuntimeException("SecurityException: Cannot get method \"" + methodName + 
                        "\" in class \"" + klass.getName() + "\" by Reflection.");
            }
        }
        return method;
    }
    
    protected void testArgsCount(RubyObject[] methodArgs) {
        if (args == null && methodArgs != null && methodArgs.length > 0) {
            throw new RubyArgumentException(getExceptedArgsString(methodArgs));
        } else if (args == null && (methodArgs == null || methodArgs.length == 0)) {
            return;
        } else if (restArgs) {
            if (methodArgs == null && (args.length == 1)) {
                return;
            } else if (methodArgs.length < (args.length - 1)) {
                throw new RubyArgumentException(getExceptedArgsString(methodArgs));
            }
        } else {
            if ((methodArgs == null) || (methodArgs.length != args.length)) {
                throw new RubyArgumentException(getExceptedArgsString(methodArgs));
            }
        }
    }
    
    protected String getExceptedArgsString(RubyObject[] methodArgs) {
        StringBuffer sb = new StringBuffer();
        sb.append("Wrong arguments:");
        
        if (methodArgs == null || methodArgs.length == 0) {
            sb.append(" No args");
        } else {
            sb.append(" (");
            for (int i = 0; i < methodArgs.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                String className = methodArgs[i].getClass().getName();
                className = className.substring(className.lastIndexOf(".Ruby") + 5);
                sb.append("a");
                if (className.charAt(0) == 'A' || className.charAt(0) == 'E' || 
                    className.charAt(0) == 'I' || className.charAt(0) == 'O' || 
                    className.charAt(0) == 'U') {
                    sb.append("n");
                }
                sb.append(className);
            }
            sb.append(")");
        }
        sb.append(" given, ");
        
        if (args == null || args.length == 0) {
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
            if (restArgs) {
                sb.append(", ...");
            }
            sb.append(") excepted.");
        }
        
        return sb.toString();
    }

    protected RubyObject invokeMethod(RubyObject recv, Object[] methodArgs, Ruby ruby) {
        if (methodArgs == null) {
            methodArgs = new Object[0];
        }
        if (restArgs) {
            RubyObject[] restArray = new RubyObject[methodArgs.length - (args.length - 1)];
            Object[] newMethodArgs = new Object[args.length];
            System.arraycopy(methodArgs, args.length - 1, restArray, 0, methodArgs.length - (args.length - 1));
            System.arraycopy(methodArgs, 0, newMethodArgs, 0, args.length - 1);
            newMethodArgs[args.length - 1] = restArray;
            methodArgs = newMethodArgs;
        }
        if (staticMethod) {
            Object[] newMethodArgs = new Object[methodArgs.length + 1];
            System.arraycopy(methodArgs, 0, newMethodArgs, 1, methodArgs.length);
            newMethodArgs[0] = ruby;
            methodArgs = newMethodArgs;
        }
        try {
            return (RubyObject)getMethod().invoke(staticMethod ? null : recv, methodArgs);
        } catch (IllegalAccessException iaExcptn) {
            throw new RuntimeException("IllegalAccessException: Cannot invoke method \"" + methodName + 
                        "\" in class \"" + klass.getName() + "\" by Reflection.");
        } catch (IllegalArgumentException iaExcptn) {
            throw new RuntimeException("IllegalArgumentException: Cannot invoke method \"" + methodName + 
                        "\" in class \"" + klass.getName() + "\" by Reflection.");
        } catch (InvocationTargetException itExcptn) {
            throw new RuntimeException("InvocationTargetException: Cannot invoke method \"" + methodName + 
                        "\" in class \"" + klass.getName() + "\" by Reflection.");
        }
    }
    
    public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
        testArgsCount(args);
        // testArgsClass(args);
        return invokeMethod(recv, args, ruby);
    }
}