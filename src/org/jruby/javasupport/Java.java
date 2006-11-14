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
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2004 David Corbin <dcorbin@users.sourceforge.net>
 * Copyright (C) 2004-2005 Thomas E Enebo <enebo@acm.org>
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
package org.jruby.javasupport;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Date;

import org.jruby.IRuby;
import org.jruby.RubyBignum;
import org.jruby.RubyBoolean;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyProc;
import org.jruby.RubyString;
import org.jruby.RubyTime;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

public class Java {
    public static RubyModule createJavaModule(IRuby runtime) {
        RubyModule javaModule = runtime.defineModule("Java");
        CallbackFactory callbackFactory = runtime.callbackFactory(Java.class);
        javaModule.defineModuleFunction("define_exception_handler", callbackFactory.getOptSingletonMethod("define_exception_handler"));
        javaModule.defineModuleFunction("primitive_to_java", callbackFactory.getSingletonMethod("primitive_to_java", IRubyObject.class));
        javaModule.defineModuleFunction("java_to_primitive", callbackFactory.getSingletonMethod("java_to_primitive", IRubyObject.class));
        javaModule.defineModuleFunction("java_to_ruby", callbackFactory.getSingletonMethod("java_to_ruby", IRubyObject.class));
        javaModule.defineModuleFunction("ruby_to_java", callbackFactory.getSingletonMethod("ruby_to_java", IRubyObject.class));
        javaModule.defineModuleFunction("new_proxy_instance", callbackFactory.getOptSingletonMethod("new_proxy_instance"));

        JavaObject.createJavaObjectClass(runtime, javaModule);
        JavaArray.createJavaArrayClass(runtime, javaModule);
        JavaClass.createJavaClassClass(runtime, javaModule);
        JavaMethod.createJavaMethodClass(runtime, javaModule);
        JavaConstructor.createJavaConstructorClass(runtime, javaModule);
        JavaField.createJavaFieldClass(runtime, javaModule);

        return javaModule;
    }

	// Java methods
    public static IRubyObject define_exception_handler(IRubyObject recv, IRubyObject[] args) {
        String name = args[0].toString();
        RubyProc handler = null;
        if (args.length > 1) {
            handler = (RubyProc)args[1];
        } else {
            handler = recv.getRuntime().newProc();
        }
        recv.getRuntime().getJavaSupport().defineExceptionHandler(name, handler);

        return recv;
    }

    public static IRubyObject primitive_to_java(IRubyObject recv, IRubyObject object) {
        if (object instanceof JavaObject) {
            return object;
        }
        IRuby runtime = recv.getRuntime();
        Object javaObject;
        if (object.isNil()) {
            javaObject = null;
        } else if (object instanceof RubyFixnum) {
            javaObject = new Long(((RubyFixnum) object).getLongValue());
        } else if (object instanceof RubyBignum) {
            javaObject = ((RubyBignum) object).getValue();
        } else if (object instanceof RubyFloat) {
            javaObject = new Double(((RubyFloat) object).getValue());
        } else if (object instanceof RubyString) {
            javaObject = ((RubyString) object).toString();
        } else if (object instanceof RubyBoolean) {
            javaObject = Boolean.valueOf(object.isTrue());
        } else if (object instanceof RubyTime) {
            long milliseconds = ((RubyNumeric) object.callMethod(recv.getRuntime().getCurrentContext(), "to_i")).getLongValue();

            javaObject = new Date(milliseconds);
        } else {
            javaObject = object;
        }
        return JavaObject.wrap(runtime, javaObject);
    }

    /**
     * High-level object conversion utility function 'java_to_primitive' is the low-level version 
     */
    public static IRubyObject java_to_ruby(IRubyObject recv, IRubyObject object) {
        if (object instanceof JavaObject) {
        	object = JavaUtil.convertJavaToRuby(recv.getRuntime(), ((JavaObject) object).getValue());
        }

        //if (object.isKindOf(recv.getRuntime().getModule("Java").getClass("JavaObject"))) {
        if (object instanceof JavaObject) {
    		return recv.getRuntime().getModule("JavaUtilities").callMethod(recv.getRuntime().getCurrentContext(), "wrap", object);
    	}

		return object;
    }

    // TODO: Formalize conversion mechanisms between Java and Ruby
    /**
     * High-level object conversion utility. 
     */
    public static IRubyObject ruby_to_java(final IRubyObject recv, IRubyObject object) {
    	if (object.respondsTo("to_java_object")) {
    		return object.callMethod(recv.getRuntime().getCurrentContext(), "to_java_object");
    	}
    	
    	return primitive_to_java(recv, object);
    }    

    public static IRubyObject java_to_primitive(IRubyObject recv, IRubyObject object) {
        if (object instanceof JavaObject) {
        	return JavaUtil.convertJavaToRuby(recv.getRuntime(), ((JavaObject) object).getValue());
        }

		return object;
    }

    public static IRubyObject new_proxy_instance(final IRubyObject recv, IRubyObject[] args) {
    	int size = recv.checkArgumentCount(args, 1, -1) - 1;
    	final RubyProc proc;

    	// Is there a supplied proc argument or do we assume a block was supplied
    	if (args[size] instanceof RubyProc) {
    		proc = (RubyProc) args[size];
    	} else {
    		proc = recv.getRuntime().newProc();
    		size++;
    	}

    	// Create list of interfaces to proxy (and make sure they really are interfaces)
        Class[] interfaces = new Class[size];
        for (int i = 0; i < size; i++) {
            if (!(args[i] instanceof JavaClass) || !((JavaClass)args[i]).interface_p().isTrue()) {
                throw recv.getRuntime().newArgumentError("Java interface expected.");
            }
            interfaces[i] = ((JavaClass) args[i]).javaClass();
        }

        return JavaObject.wrap(recv.getRuntime(), Proxy.newProxyInstance(recv.getRuntime().getJavaSupport().getJavaClassLoader(), interfaces, new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] nargs) throws Throwable {
            	int methodArgsLength = method.getParameterTypes().length;
            	String methodName = method.getName();
            	
                if (methodName.equals("toString") && methodArgsLength == 0) {
                    return proxy.getClass().getName();
                } else if (methodName.equals("hashCode") && methodArgsLength == 0) {
                    return new Integer(proxy.getClass().hashCode());
                } else if (methodName.equals("equals") && methodArgsLength == 1 && method.getParameterTypes()[0].equals(Object.class)) {
                    return Boolean.valueOf(proxy == nargs[0]);
                }
                int length = nargs == null ? 0 : nargs.length;
                IRubyObject[] rubyArgs = new IRubyObject[length + 2];
                rubyArgs[0] = JavaObject.wrap(recv.getRuntime(), proxy);
                rubyArgs[1] = new JavaMethod(recv.getRuntime(), method);
                for (int i = 0; i < length; i++) {
                    rubyArgs[i + 2] = JavaObject.wrap(recv.getRuntime(), nargs[i]);
                }
                return JavaUtil.convertArgument(proc.call(rubyArgs), method.getReturnType());
            }
        }));
    }
}
