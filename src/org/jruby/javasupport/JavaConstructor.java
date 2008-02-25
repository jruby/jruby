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
 * Copyright (C) 2001 Chad Fowler <chadfowler@chadfowler.com>
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
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
package org.jruby.javasupport;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;

public class JavaConstructor extends JavaCallable {
    private final Constructor<?> constructor;
    private final Class<?>[] parameterTypes;

    public static RubyClass createJavaConstructorClass(Ruby runtime, RubyModule javaModule) {
        // TODO: NOT_ALLOCATABLE_ALLOCATOR is probably ok here, since we don't intend for people to monkey with
        // this type and it can't be marshalled. Confirm. JRUBY-415
        RubyClass result =
                javaModule.defineClassUnder("JavaConstructor", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        CallbackFactory callbackFactory = runtime.callbackFactory(JavaConstructor.class);

        JavaAccessibleObject.registerRubyMethods(runtime, result);
        JavaCallable.registerRubyMethods(runtime, result);

        result.defineFastMethod("new_instance", callbackFactory.getFastOptMethod("new_instance"));
        result.defineFastMethod("type_parameters", callbackFactory.getFastMethod("type_parameters"));
        
        return result;
    }

    public JavaConstructor(Ruby runtime, Constructor<?> constructor) {
        super(runtime, runtime.getJavaSupport().getJavaModule().fastGetClass("JavaConstructor"));
        this.constructor = constructor;
        this.parameterTypes = constructor.getParameterTypes();
    }

    public boolean equals(Object other) {
        return other instanceof JavaConstructor &&
            this.constructor == ((JavaConstructor)other).constructor;
    }
    
    public int hashCode() {
        return constructor.hashCode();
    }

    public int getArity() {
        return parameterTypes.length;
    }
    
    protected String nameOnInspection() {
        return getType().toString();
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public Class<?>[] getExceptionTypes() {
        return constructor.getExceptionTypes();
    }

    public Type[] getGenericParameterTypes() {
        return constructor.getGenericParameterTypes();
    }

    public Type[] getGenericExceptionTypes() {
        return constructor.getGenericExceptionTypes();
    }

    public Annotation[][] getParameterAnnotations() {
        return constructor.getParameterAnnotations();
    }
    
    public boolean isVarArgs() {
        return constructor.isVarArgs();
    }

    public int getModifiers() {
        return constructor.getModifiers();
    }
    
    public String toGenericString() {
        return constructor.toGenericString();
    }

    protected AccessibleObject accessibleObject() {
        return constructor;
    }
    
    public IRubyObject type_parameters() {
        return Java.getInstance(getRuntime(), constructor.getTypeParameters());
    }


    public IRubyObject new_instance(IRubyObject[] args) {
        int length = args.length;
        Class<?>[] types = parameterTypes;
        if (length != types.length) {
            throw getRuntime().newArgumentError(length, types.length);
        }
        Object[] constructorArguments = new Object[length];
        for (int i = length; --i >= 0; ) {
            constructorArguments[i] = JavaUtil.convertArgument(args[i], types[i]);
        }
        try {
            Object result = constructor.newInstance(constructorArguments);
            return JavaObject.wrap(getRuntime(), result);

        } catch (IllegalArgumentException iae) {
            throw getRuntime().newTypeError("expected " + argument_types().inspect() +
                                              ", got [" + constructorArguments[0].getClass().getName() + ", ...]");
        } catch (IllegalAccessException iae) {
            throw getRuntime().newTypeError("illegal access");
        } catch (InvocationTargetException ite) {
            getRuntime().getJavaSupport().handleNativeException(ite.getTargetException());
            // not reached
            assert false;
            return null;
        } catch (InstantiationException ie) {
            throw getRuntime().newTypeError("can't make instance of " + constructor.getDeclaringClass().getName());
        }
    }
    
}
