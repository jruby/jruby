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

package org.jruby.javasupport.proxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyProc;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaObject;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;

public class JavaProxyConstructor extends JavaProxyReflectionObject {

    private final Constructor proxyConstructor;
    private final Class[] parameterTypes;

    private final JavaProxyClass declaringProxyClass;

    JavaProxyConstructor(Ruby runtime, JavaProxyClass pClass,
            Constructor constructor) {
        super(runtime, runtime.getModule("Java").getClass(
                "JavaProxyConstructor"));
        this.declaringProxyClass = pClass;
        this.proxyConstructor = constructor;
        this.parameterTypes = proxyConstructor.getParameterTypes();
    }

    public Class[] getParameterTypes() {
        Class[] result = new Class[parameterTypes.length - 1];
        System.arraycopy(parameterTypes, 0, result, 0, result.length);
        return result;
    }

    public JavaProxyClass getDeclaringClass() {
        return declaringProxyClass;
    }

    public Object newInstance(Object[] args, JavaProxyInvocationHandler handler)
            throws IllegalArgumentException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        if (args.length + 1 != parameterTypes.length) {
            throw new IllegalArgumentException("wrong number of parameters");
        }

        Object[] realArgs = new Object[args.length + 1];
        System.arraycopy(args, 0, realArgs, 0, args.length);
        realArgs[args.length] = handler;

        return proxyConstructor.newInstance(realArgs);
    }

    public static RubyClass createJavaProxyConstructorClass(Ruby runtime,
            RubyModule javaProxyModule) {
        RubyClass result = javaProxyModule.defineClassUnder(
                                                            "JavaProxyConstructor", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);

        CallbackFactory callbackFactory = runtime
                .callbackFactory(JavaProxyConstructor.class);

        JavaProxyReflectionObject.registerRubyMethods(runtime, result);

        result.defineFastMethod("argument_types", callbackFactory
                .getFastMethod("argument_types"));

        result.defineFastMethod("declaring_class", callbackFactory
                .getFastMethod("getDeclaringClass"));

        result.defineMethod("new_instance", callbackFactory
                .getOptMethod("new_instance"));
        
        result.defineMethod("new_instance2", callbackFactory.getOptMethod("new_instance2"));

        result.defineFastMethod("arity", callbackFactory.getFastMethod("arity"));

        return result;

    }

    public RubyFixnum arity() {
        return getRuntime().newFixnum(getParameterTypes().length);
    }

    protected String nameOnInspection() {
        return getDeclaringClass().nameOnInspection();
    }

    public IRubyObject inspect() {
        StringBuffer result = new StringBuffer();
        result.append(nameOnInspection());
        Class[] parameterTypes = getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            result.append(parameterTypes[i].getName());
            if (i < parameterTypes.length - 1) {
                result.append(',');
            }
        }
        result.append(")>");
        return getRuntime().newString(result.toString());
    }

    public RubyArray argument_types() {
        return buildRubyArray(getParameterTypes());
    }
    
    public RubyObject new_instance2(IRubyObject[] args, Block unusedBlock) {
        Arity.checkArgumentCount(getRuntime(), args, 2, 2);

        final IRubyObject self = args[0];
        RubyArray constructor_args = (RubyArray) args[1];
        Class[] parameterTypes = getParameterTypes();
        int count = (int) constructor_args.length().getLongValue();
        Object[] converted = new Object[count];
        
        for (int i = 0; i < count; i++) {
            // TODO: call ruby method
            IRubyObject ith = constructor_args.aref(new IRubyObject[] { getRuntime().newFixnum(i) });
            converted[i] = JavaUtil.convertArgument(Java.ruby_to_java(this, ith, Block.NULL_BLOCK), parameterTypes[i]);
        }

        JavaProxyInvocationHandler handler = new JavaProxyInvocationHandler() {
            public Object invoke(Object proxy, JavaProxyMethod m, Object[] nargs) throws Throwable {
                Ruby runtime = self.getRuntime();
                String name = m.getName();
                DynamicMethod method = self.getMetaClass().searchMethod(name);
                int v = method.getArity().getValue();
                IRubyObject[] newArgs = JavaUtil.convertJavaArrayToRuby(runtime, nargs);
                
                if (v < 0 || v == (newArgs.length)) {
                    return JavaUtil.convertRubyToJava(self.callMethod(runtime.getCurrentContext(), name, newArgs, CallType.FUNCTIONAL, Block.NULL_BLOCK));
                } else {
                    return JavaUtil.convertRubyToJava(self.callMethod(runtime.getCurrentContext(),self.getMetaClass().getSuperClass(), name, newArgs, CallType.SUPER, Block.NULL_BLOCK));
                }
            }
        };

        try {
            return JavaObject.wrap(getRuntime(), newInstance(converted, handler));
        } catch (Exception e) {
            RaiseException ex = getRuntime().newArgumentError(
                    "Constructor invocation failed: " + e.getMessage());
            ex.initCause(e);
            throw ex;
        }
    }

    public RubyObject new_instance(IRubyObject[] args, Block block) {
        int size = Arity.checkArgumentCount(getRuntime(), args, 1, 2) - 1;
        final RubyProc proc;

        // Is there a supplied proc argument or do we assume a block was
        // supplied
        if (args[size] instanceof RubyProc) {
            proc = (RubyProc) args[size];
        } else {
            proc = getRuntime().newProc(false,block);
            size++;
        }

        RubyArray constructor_args = (RubyArray) args[0];
        Class[] parameterTypes = getParameterTypes();

        int count = (int) constructor_args.length().getLongValue();
        Object[] converted = new Object[count];
        for (int i = 0; i < count; i++) {
            // TODO: call ruby method
            IRubyObject ith = constructor_args.aref(new IRubyObject[] { getRuntime().newFixnum(i) });
            converted[i] = JavaUtil.convertArgument(Java.ruby_to_java(this, ith, Block.NULL_BLOCK), parameterTypes[i]);
        }

        final IRubyObject recv = this;

        JavaProxyInvocationHandler handler = new JavaProxyInvocationHandler() {

            public Object invoke(Object proxy, JavaProxyMethod method,
                    Object[] nargs) throws Throwable {
                int length = nargs == null ? 0 : nargs.length;
                IRubyObject[] rubyArgs = new IRubyObject[length + 2];
                rubyArgs[0] = JavaObject.wrap(recv.getRuntime(), proxy);
                rubyArgs[1] = method;
                for (int i = 0; i < length; i++) {
                    rubyArgs[i + 2] = JavaUtil.convertJavaToRuby(getRuntime(),
                            nargs[i]);
                }
                IRubyObject call_result = proc.call(rubyArgs);
                Object converted_result = JavaUtil.convertRubyToJava(
                        call_result, method.getReturnType());
                return converted_result;
            }

        };

        Object result;
        try {
            result = newInstance(converted, handler);
        } catch (Exception e) {
            RaiseException ex = getRuntime().newArgumentError(
                    "Constructor invocation failed: " + e.getMessage());
            ex.initCause(e);
            throw ex;
        }

        return JavaObject.wrap(getRuntime(), result);

    }

}
