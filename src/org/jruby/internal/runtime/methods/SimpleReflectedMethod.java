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
 * Copyright (C) 2005 Thomas E Enebo <enebo@acm.org>
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
package org.jruby.internal.runtime.methods;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.jruby.IRuby;
import org.jruby.RubyModule;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.MainExitException;
import org.jruby.exceptions.ThreadKill;
import org.jruby.runtime.Arity;
import org.jruby.runtime.DynamicMethod;
import org.jruby.runtime.ICallable;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public class SimpleReflectedMethod extends AbstractMethod {
    private Method method;
    private Class type;
    private String methodName;
    private Arity arity;
    
    public SimpleReflectedMethod(RubyModule implementationClass, Class type, String methodName, 
        Arity arity, Visibility visibility) {
    	super(implementationClass, visibility);
    	this.type = type;
    	this.methodName = methodName;
    	this.arity = arity;
    	
        assert type != null;
        assert methodName != null;
        assert arity != null;

        Class[] parameterTypes;
        if (arity.isFixed()) {
            parameterTypes = new Class[arity.getValue()];
            Arrays.fill(parameterTypes, IRubyObject.class);
        } else {
            parameterTypes = new Class[1];
            parameterTypes[0] = IRubyObject[].class;
        }
        try {
            method = type.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            assert false : e;
        } catch (SecurityException e) {
            assert false : e;
        }
        
        assert method != null;
    }

    public void preMethod(ThreadContext context, RubyModule lastClass, IRubyObject recv, String name, IRubyObject[] args, boolean noSuper) {
        context.preReflectedMethodInternalCall(implementationClass, lastClass, recv, name, args, noSuper);
    }
    
    public void postMethod(ThreadContext context) {
        context.postReflectedMethodInternalCall();
    }
    
	public IRubyObject internalCall(ThreadContext context, IRubyObject receiver, RubyModule lastClass, String name, IRubyObject[] args, boolean noSuper) {
        IRuby runtime = context.getRuntime();
        arity.checkArity(runtime, args);
        
        assert receiver != null;
        assert args != null;
        assert method != null;
        
        Object[] methodArgs = !arity.isFixed() ? new Object[]{args} : args; 

        try {
            return (IRubyObject) method.invoke(receiver, methodArgs);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof RaiseException) {
                throw (RaiseException) e.getTargetException();
            } else if (e.getTargetException() instanceof JumpException) {
                throw (JumpException) e.getTargetException();
            } else if (e.getTargetException() instanceof ThreadKill) {
            	// allow it to bubble up
            	throw (ThreadKill) e.getTargetException();
            } else if (e.getTargetException() instanceof Exception) {
                if(e.getTargetException() instanceof MainExitException) {
                    throw (RuntimeException)e.getTargetException();
                }
                runtime.getJavaSupport().handleNativeException(e.getTargetException());
                return runtime.getNil();
            } else {
                throw (Error) e.getTargetException();
            }
        } catch (IllegalAccessException e) {
            StringBuffer message = new StringBuffer();
            message.append(e.getMessage());
            message.append(':');
            message.append(" methodName=").append(methodName);
            message.append(" recv=").append(receiver.toString());
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

	public DynamicMethod dup() {
		SimpleReflectedMethod newMethod = 
		    new SimpleReflectedMethod(getImplementationClass(), type, methodName, arity, getVisibility());
		
		newMethod.method = method;
		
		return newMethod;
	}

	// TODO:  Perhaps abstract method should contain this and all other Methods should pass in decent value
	public Arity getArity() {
		return arity;
	}
}
