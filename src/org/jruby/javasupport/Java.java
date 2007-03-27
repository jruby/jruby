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
 * Copyright (C) 2006 Kresten Krab Thorup <krab@gnu.org>
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

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBignum;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyProc;
import org.jruby.RubyString;
import org.jruby.RubyTime;
import org.jruby.javasupport.proxy.JavaProxyClass;
import org.jruby.javasupport.proxy.JavaProxyConstructor;
import org.jruby.javasupport.proxy.JavaProxyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callback.Callback;

public class Java {
    public static RubyModule createJavaModule(Ruby runtime) {
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
       
        // also create the JavaProxy* classes
        JavaProxyClass.createJavaProxyModule(runtime);

        RubyModule javaUtils = runtime.defineModule("JavaUtilities");
        javaUtils.defineFastModuleFunction("wrap", callbackFactory.getFastSingletonMethod("wrap",IRubyObject.class));
        javaUtils.defineFastModuleFunction("valid_constant_name?", callbackFactory.getFastSingletonMethod("valid_constant_name_p",IRubyObject.class));
        javaUtils.defineFastModuleFunction("primitive_match", callbackFactory.getFastSingletonMethod("primitive_match",IRubyObject.class,IRubyObject.class));
        javaUtils.defineFastModuleFunction("access", callbackFactory.getFastSingletonMethod("access",IRubyObject.class));
        javaUtils.defineFastModuleFunction("matching_method", callbackFactory.getFastSingletonMethod("matching_method", IRubyObject.class, IRubyObject.class));
        javaUtils.defineFastModuleFunction("get_proxy_class", callbackFactory.getFastSingletonMethod("get_proxy_class", IRubyObject.class));

        javaUtils.setInstanceVariable("@callback", JavaUtil.convertJavaToRuby(runtime, callbackFactory.getFastSingletonMethod("concrete_proxy_inherited", IRubyObject.class)));

        RubyClass javaProxy = runtime.defineClass("JavaProxy", runtime.getObject(), runtime.getObject().getAllocator());
        javaProxy.getMetaClass().defineFastMethod("new_instance_for", callbackFactory.getFastSingletonMethod("new_instance_for", IRubyObject.class));

        return javaModule;
    }

    // JavaProxy
    public static IRubyObject new_instance_for(IRubyObject recv, IRubyObject java_object) {
        IRubyObject new_instance = ((RubyClass)recv).allocate();
        new_instance.setInstanceVariable("@java_object",java_object);
        return new_instance;
    }
    
    // JavaUtilities
    public static IRubyObject get_proxy_class(IRubyObject recv, IRubyObject java_class) {
        if(java_class instanceof RubyString) {
            java_class = JavaClass.for_name(recv, java_class);
        }
        RubyFixnum class_id = java_class.id();
        Ruby runtime = recv.getRuntime();
        RubyHash proxy_classes = (RubyHash)recv.getInstanceVariable("@proxy_classes");
        synchronized(java_class) {
            if(proxy_classes.aref(class_id).isNil()) {
                Class c = ((JavaClass)java_class).javaClass();
                RubyClass base_type;
                boolean concrete = false;
                if(c.isInterface()) {
                    base_type = runtime.getClass("InterfaceJavaProxy");
                } else if(c.isArray()) {
                    base_type = runtime.getClass("ArrayJavaProxy");
                } else {
                    concrete = true;
                    base_type = runtime.getClass("ConcreteJavaProxy");
                }

                RubyClass proxy_class = RubyClass.newClass(recv, new IRubyObject[]{base_type}, Block.NULL_BLOCK);
                proxy_class.callMethod(runtime.getCurrentContext(), "java_class=", java_class);
                if(concrete) {
                    proxy_class.getMetaClass().defineFastMethod("inherited", (Callback)  JavaUtil.convertRubyToJava(recv.getInstanceVariable("@callback")));
                }
                proxy_classes.aset(class_id, proxy_class);
                // We do not setup the proxy before we register it so that same-typed constants do
                // not try and create a fresh proxy class and go into an infinite loop
                proxy_class.callMethod(runtime.getCurrentContext(), "setup");
                
                RubyArray proxy_extenders = (RubyArray)recv.getInstanceVariable("@proxy_extenders");
                for(int i=0,j=proxy_extenders.getLength();i<j;i++) {
                    proxy_extenders.eltInternal(i).callMethod(runtime.getCurrentContext(), "extend_proxy", proxy_class);
                }
            }
        }
        return proxy_classes.aref(class_id);
    }

    public static IRubyObject concrete_proxy_inherited(IRubyObject recv, IRubyObject subclass) {
        ThreadContext tc = recv.getRuntime().getCurrentContext();
        recv.callMethod(tc,recv.getMetaClass().getSuperClass(), "inherited", new IRubyObject[]{subclass}, org.jruby.runtime.CallType.SUPER, Block.NULL_BLOCK);
        return recv.getRuntime().getModule("JavaUtilities").callMethod(tc, "setup_java_subclass", new IRubyObject[]{subclass, recv.callMethod(tc,"java_class")});
    }

    public static IRubyObject matching_method(IRubyObject recv, IRubyObject methods, IRubyObject args) {
        Map matchCache = (Map)recv.dataGetStruct();
        if(null == matchCache) {
            matchCache = new HashMap();
            recv.dataWrapStruct(matchCache);
        }

        List arg_types = new ArrayList();
        int alen = ((RubyArray)args).getLength();
        IRubyObject[] aargs = ((RubyArray)args).toJavaArrayMaybeUnsafe();
        for(int i=0;i<alen;i++) {
            arg_types.add(((JavaClass)((JavaObject)aargs[i]).java_class()).getValue());
        }

        Map ms = (Map)matchCache.get(methods);
        if(ms == null) {
            ms = new HashMap();
            matchCache.put(methods, ms);
        } else {
            IRubyObject method = (IRubyObject)ms.get(arg_types);
            if(method != null) {
                return method;
            }
        }

        int mlen = ((RubyArray)methods).getLength();
        IRubyObject[] margs = ((RubyArray)methods).toJavaArrayMaybeUnsafe();

        for(int i=0;i<2;i++) {
            for(int k=0;k<mlen;k++) {
                List types = null;
                IRubyObject method = margs[k];
                if(method instanceof JavaCallable) {
                    types = java.util.Arrays.asList(((JavaCallable)method).parameterTypes());
                } else if(method instanceof JavaProxyMethod) {
                    types = java.util.Arrays.asList(((JavaProxyMethod)method).getParameterTypes());
                } else if(method instanceof JavaProxyConstructor) {
                    types = java.util.Arrays.asList(((JavaProxyConstructor)method).getParameterTypes());
                }
                // Exact match
                if(types.equals(arg_types)) {
                    ms.put(arg_types, method);
                    return method;
                }

                // Compatible (by inheritance)
                if(arg_types.size() == types.size()) {
                    boolean match = true;
                    for(int j=0; j<types.size(); j++) {
                        if(!(
                             JavaClass.assignable((Class)types.get(j),(Class)arg_types.get(j)) &&
                             (i > 0 || primitive_match(types.get(j),arg_types.get(j)))
                             )) {
                            match = false;
                        }
                    }
                    if(match) {
                        ms.put(arg_types, method);
                        return method;
                    }
                } // Could check for varargs here?
            }
        }

        Object o1 = margs[0];

        if(o1 instanceof JavaConstructor || o1 instanceof JavaProxyConstructor) {
            throw recv.getRuntime().newNameError("no constructor with arguments matching " + arg_types, null);
        } else {
            throw recv.getRuntime().newNameError("no " + ((JavaMethod)o1).name() + " with arguments matching " + arg_types, null);
        }
    }

    public static IRubyObject access(IRubyObject recv, IRubyObject java_type) {
        int modifiers = ((JavaClass)java_type).javaClass().getModifiers();
        return recv.getRuntime().newString(Modifier.isPublic(modifiers) ? "public" : (Modifier.isProtected(modifiers) ? "protected" : "private"));
    }

    public static IRubyObject valid_constant_name_p(IRubyObject recv, IRubyObject name) {
        RubyString sname = name.convertToString();
        if(sname.getByteList().length() == 0) {
            return recv.getRuntime().getFalse();
        }
        return Character.isUpperCase(sname.getByteList().charAt(0)) ? recv.getRuntime().getTrue() : recv.getRuntime().getFalse();
    }

    public static boolean primitive_match(Object v1, Object v2) {
        if(((Class)v1).isPrimitive()) {
            if(v1 == Integer.TYPE || v1 == Long.TYPE || v1 == Short.TYPE || v1 == Character.TYPE) {
                return v2 == Integer.class ||
                    v2 == Long.class ||
                    v2 == Short.class ||
                    v2 == Character.class;
            } else if(v1 == Float.TYPE || v1 == Double.TYPE) {
                return v2 == Float.class ||
                    v2 == Double.class;
            } else if(v1 == Boolean.TYPE) {
                return v2 == Boolean.class;
            }
            return false;
        }
        return true;
    }

    public static IRubyObject primitive_match(IRubyObject recv, IRubyObject t1, IRubyObject t2) {
        if(((JavaClass)t1).primitive_p().isTrue()) {
            Object v1 = ((JavaObject)t1).getValue();
            Object v2 = ((JavaObject)t2).getValue();
            return primitive_match(v1,v2) ? recv.getRuntime().getTrue() : recv.getRuntime().getFalse();
        }
        return recv.getRuntime().getTrue();
    }

    public static IRubyObject wrap(IRubyObject recv, IRubyObject java_object) {
        Ruby runtime = recv.getRuntime();
        ThreadContext ctx = runtime.getCurrentContext();
        return recv.callMethod(ctx, "get_proxy_class", ((JavaObject)java_object).java_class()).callMethod(ctx,"new_instance_for",java_object);
    }

	// Java methods
    public static IRubyObject define_exception_handler(IRubyObject recv, IRubyObject[] args, Block block) {
        String name = args[0].toString();
        RubyProc handler = null;
        if (args.length > 1) {
            handler = (RubyProc)args[1];
        } else {
            handler = recv.getRuntime().newProc(false, block);
        }
        recv.getRuntime().getJavaSupport().defineExceptionHandler(name, handler);

        return recv;
    }

    public static IRubyObject primitive_to_java(IRubyObject recv, IRubyObject object, Block unusedBlock) {
        if (object instanceof JavaObject) {
            return object;
        }
        Ruby runtime = recv.getRuntime();
        Object javaObject;
        switch (object.getMetaClass().index) {
        case ClassIndex.NIL:
            javaObject = null;
            break;
        case ClassIndex.FIXNUM:
            javaObject = new Long(((RubyFixnum) object).getLongValue());
            break;
        case ClassIndex.BIGNUM:
            javaObject = ((RubyBignum) object).getValue();
            break;
        case ClassIndex.FLOAT:
            javaObject = new Double(((RubyFloat) object).getValue());
            break;
        case ClassIndex.STRING:
            javaObject = ((RubyString) object).toString();
            break;
        case ClassIndex.TRUE:
            javaObject = Boolean.TRUE;
            break;
        case ClassIndex.FALSE:
            javaObject = Boolean.FALSE;
            break;
        default:
            if (object instanceof RubyTime) {
                javaObject = ((RubyTime)object).getJavaDate();
            } else {
                javaObject = object;
            }
        }
        return JavaObject.wrap(runtime, javaObject);
    }

    /**
     * High-level object conversion utility function 'java_to_primitive' is the low-level version 
     */
    public static IRubyObject java_to_ruby(IRubyObject recv, IRubyObject object, Block unusedBlock) {
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
    public static IRubyObject ruby_to_java(final IRubyObject recv, IRubyObject object, Block unusedBlock) {
    	if (object.respondsTo("to_java_object")) {
            IRubyObject result = object.getInstanceVariable("@java_object");
            if (result == null) {
    		result = object.callMethod(recv.getRuntime().getCurrentContext(), "to_java_object");
            }
            return result;
    	}
    	
    	return primitive_to_java(recv, object, unusedBlock);
    }    

    public static IRubyObject java_to_primitive(IRubyObject recv, IRubyObject object, Block unusedBlock) {
        if (object instanceof JavaObject) {
        	return JavaUtil.convertJavaToRuby(recv.getRuntime(), ((JavaObject) object).getValue());
        }

		return object;
    }

    public static IRubyObject new_proxy_instance(final IRubyObject recv, IRubyObject[] args, Block block) {
    	int size = recv.checkArgumentCount(args, 1, -1) - 1;
    	final RubyProc proc;

    	// Is there a supplied proc argument or do we assume a block was supplied
    	if (args[size] instanceof RubyProc) {
    		proc = (RubyProc) args[size];
    	} else {
    		proc = recv.getRuntime().newProc(false, block);
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
