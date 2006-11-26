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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004-2005 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2004 David Corbin <dcorbin@users.sourceforge.net>
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jruby.IRuby;
import org.jruby.RubyArray;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyInteger;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Arity;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callback.Callback;

public class JavaClass extends JavaObject {

    private JavaClass(IRuby runtime, Class javaClass) {
        super(runtime, (RubyClass) runtime.getModule("Java").getClass("JavaClass"), javaClass);
    }
    
    public static JavaClass get(IRuby runtime, Class klass) {
        JavaClass javaClass = runtime.getJavaSupport().getJavaClassFromCache(klass);
        if (javaClass == null) {
            javaClass = new JavaClass(runtime, klass);
            runtime.getJavaSupport().putJavaClassIntoCache(javaClass);
        }
        return javaClass;
    }

    public static RubyClass createJavaClassClass(IRuby runtime, RubyModule javaModule) {
        RubyClass result = javaModule.defineClassUnder("JavaClass", javaModule.getClass("JavaObject")); 

    	CallbackFactory callbackFactory = runtime.callbackFactory(JavaClass.class);
        
        result.includeModule(runtime.getModule("Comparable"));
        
        JavaObject.registerRubyMethods(runtime, result);
        
        result.defineSingletonMethod("for_name", 
                callbackFactory.getSingletonMethod("for_name", IRubyObject.class));
        result.defineMethod("public?", 
                callbackFactory.getMethod("public_p"));
        result.defineMethod("protected?", 
                callbackFactory.getMethod("protected_p"));
        result.defineMethod("private?", 
                callbackFactory.getMethod("private_p"));
        result.defineMethod("final?", 
                callbackFactory.getMethod("final_p"));
        result.defineMethod("interface?", 
                callbackFactory.getMethod("interface_p"));
        result.defineMethod("array?", 
                callbackFactory.getMethod("array_p"));
        result.defineMethod("name", 
                callbackFactory.getMethod("name"));
        result.defineMethod("to_s", 
                callbackFactory.getMethod("name"));
        result.defineMethod("superclass", 
                callbackFactory.getMethod("superclass"));
        result.defineMethod("<=>", 
                callbackFactory.getMethod("op_cmp", IRubyObject.class));
        result.defineMethod("java_instance_methods", 
                callbackFactory.getMethod("java_instance_methods"));
        result.defineMethod("java_class_methods", 
                callbackFactory.getMethod("java_class_methods"));
        result.defineMethod("java_method", 
                callbackFactory.getOptMethod("java_method"));
        result.defineMethod("constructors", 
                callbackFactory.getMethod("constructors"));
        result.defineMethod("constructor", 
                callbackFactory.getOptMethod("constructor"));
        result.defineMethod("array_class", 
                callbackFactory.getMethod("array_class"));
        result.defineMethod("new_array", 
                callbackFactory.getMethod("new_array", IRubyObject.class));
        result.defineMethod("fields", 
                callbackFactory.getMethod("fields"));
        result.defineMethod("field", 
                callbackFactory.getMethod("field", IRubyObject.class));
        result.defineMethod("interfaces", 
                callbackFactory.getMethod("interfaces"));
        result.defineMethod("primitive?", 
                callbackFactory.getMethod("primitive_p"));
        result.defineMethod("assignable_from?", 
                callbackFactory.getMethod("assignable_from_p", IRubyObject.class));
        result.defineMethod("component_type", 
                callbackFactory.getMethod("component_type"));
		result.defineMethod("declared_instance_methods", 
                callbackFactory.getMethod("declared_instance_methods"));
        result.defineMethod("declared_class_methods", 
                callbackFactory.getMethod("declared_class_methods"));
        result.defineMethod("declared_fields", 
                callbackFactory.getMethod("declared_fields"));
        result.defineMethod("declared_field", 
                callbackFactory.getMethod("declared_field", IRubyObject.class));
        result.defineMethod("declared_constructors", 
                callbackFactory.getMethod("declared_constructors"));
        result.defineMethod("declared_constructor", 
                callbackFactory.getOptMethod("declared_constructor"));
        result.defineMethod("declared_method", 
                callbackFactory.getOptMethod("declared_method"));
        result.defineMethod("define_instance_methods_for_proxy", 
                callbackFactory.getMethod("define_instance_methods_for_proxy", IRubyObject.class));
        
        result.getMetaClass().undefineMethod("new");

        return result;
    }
    
    public static JavaClass for_name(IRubyObject recv, IRubyObject name) {
        String className = name.asSymbol();
        Class klass = recv.getRuntime().getJavaSupport().loadJavaClass(className);
        return JavaClass.get(recv.getRuntime(), klass);
    }
    
    /**
     *  Get all methods grouped by name (e.g. 'new => {new(), new(int), new(int, int)}, ...')
     *  @param isStatic determines whether you want static or instance methods from the class
     */
    private Map getMethodsClumped(boolean isStatic) {
        Map map = new HashMap();
        if(((Class)getValue()).isInterface()) {
            return map;
        }

        Method methods[] = javaClass().getMethods();
        
        for (int i = 0; i < methods.length; i++) {
            if (isStatic != Modifier.isStatic(methods[i].getModifiers())) {
                continue;
            }
            
            String key = methods[i].getName();
            RubyArray methodsWithName = (RubyArray) map.get(key); 
            
            if (methodsWithName == null) {
                methodsWithName = getRuntime().newArray();
                map.put(key, methodsWithName);
            }
            
            methodsWithName.append(JavaMethod.create(getRuntime(), methods[i]));
        }
        
        return map;
    }
    
    private Map getPropertysClumped() {
        Map map = new HashMap();
        BeanInfo info;
        
        try {
            info = Introspector.getBeanInfo(javaClass());
        } catch (IntrospectionException e) {
            return map;
        }
        
        PropertyDescriptor[] descriptors = info.getPropertyDescriptors();
        
        for (int i = 0; i < descriptors.length; i++) {
            Method readMethod = descriptors[i].getReadMethod();
            
            if (readMethod != null) {
                String key = readMethod.getName();
                List aliases = (List) map.get(key);
                
                if (aliases == null) {
                    aliases = new ArrayList();
                    
                    map.put(key, aliases);    
                }

                if (readMethod.getReturnType() == Boolean.class ||
                    readMethod.getReturnType() == boolean.class) {
                    aliases.add(descriptors[i].getName() + "?");
                }
                aliases.add(descriptors[i].getName());
            }
            
            Method writeMethod = descriptors[i].getWriteMethod();

            if (writeMethod != null) {
                String key = writeMethod.getName();
                List aliases = (List) map.get(key);
                
                if (aliases == null) {
                    aliases = new ArrayList();
                    map.put(key, aliases);
                }
                
                aliases.add(descriptors[i].getName()  + "=");
            }
        }
        
        return map;
    }
    
    private void define_instance_method_for_proxy(final RubyClass proxy, List names, 
            final RubyArray methods) {
        final RubyModule javaUtilities = getRuntime().getModule("JavaUtilities");
        Callback method = new Callback() {
            public IRubyObject execute(IRubyObject self, IRubyObject[] args) {
                IRubyObject[] argsArray = new IRubyObject[args.length + 1];
                ThreadContext context = self.getRuntime().getCurrentContext();
                
                argsArray[0] = self.callMethod(context, "java_object");
                RubyArray argsAsArray = getRuntime().newArray();
                for (int j = 0; j < args.length; j++) {
                    argsArray[j+1] = Java.ruby_to_java(proxy, args[j]);
                    argsAsArray.append(argsArray[j+1]);
                }

                IRubyObject[] mmArgs = new IRubyObject[] {methods, argsAsArray};
                IRubyObject result = javaUtilities.callMethod(context, "matching_method", mmArgs);
                return Java.java_to_ruby(self, result.callMethod(context, "invoke", argsArray));
            }

            public Arity getArity() {
                return Arity.optional();
            }
        };
        
        for(Iterator iter = names.iterator(); iter.hasNext(); ) {
            String methodName = (String) iter.next();
            
            // We do not override class since it is too important to be overridden by getClass
            // short name.
            if (!methodName.equals("class")) {
                proxy.defineMethod(methodName, method);
                
                String rubyCasedName = getRubyCasedName(methodName);
                if (rubyCasedName != null) {
                    proxy.defineAlias(rubyCasedName, methodName);
                }
            }
        }
    }
    
    private static final Pattern CAMEL_CASE_SPLITTER = Pattern.compile("([a-z])([A-Z])");
    
    private String getRubyCasedName(String javaCasedName) {
        Matcher m = CAMEL_CASE_SPLITTER.matcher(javaCasedName);

        String rubyCasedName = m.replaceAll("$1_$2").toLowerCase();
        
        if (rubyCasedName.equals(javaCasedName)) {
            return null;
        }
        
        return rubyCasedName;
    }
    
    public IRubyObject define_instance_methods_for_proxy(IRubyObject arg) {
        assert arg instanceof RubyClass;
        
        Map aliasesClump = getPropertysClumped();
        Map methodsClump = getMethodsClumped(false);
        RubyClass proxy = (RubyClass) arg;
        
        for (Iterator iter = methodsClump.keySet().iterator(); iter.hasNext(); ) {
            String name = (String) iter.next();
            RubyArray methods = (RubyArray) methodsClump.get(name);
            List aliases = (List) aliasesClump.get(name);

            if (aliases == null) {
                aliases = new ArrayList();
            }

            aliases.add(name);
            
            define_instance_method_for_proxy(proxy, aliases, methods);
        }
        
        return getRuntime().getNil();
    }

    public RubyBoolean public_p() {
        return getRuntime().newBoolean(Modifier.isPublic(javaClass().getModifiers()));
    }

    public RubyBoolean protected_p() {
        return getRuntime().newBoolean(Modifier.isProtected(javaClass().getModifiers()));
    }

    public RubyBoolean private_p() {
        return getRuntime().newBoolean(Modifier.isPrivate(javaClass().getModifiers()));
    }

	Class javaClass() {
		return (Class) getValue();
	}

    public RubyBoolean final_p() {
        return getRuntime().newBoolean(Modifier.isFinal(javaClass().getModifiers()));
    }

    public RubyBoolean interface_p() {
        return getRuntime().newBoolean(javaClass().isInterface());
    }

    public RubyBoolean array_p() {
        return getRuntime().newBoolean(javaClass().isArray());
    }
    
    public RubyString name() {
        return getRuntime().newString(javaClass().getName());
    }

    public IRubyObject superclass() {
        Class superclass = javaClass().getSuperclass();
        if (superclass == null) {
            return getRuntime().getNil();
        }
        return JavaClass.get(getRuntime(), superclass);
    }

    public RubyFixnum op_cmp(IRubyObject other) {
        if (! (other instanceof JavaClass)) {
            throw getRuntime().newTypeError("<=> requires JavaClass (" + other.getType() + " given)");
        }
        JavaClass otherClass = (JavaClass) other;
        if (this.javaClass() == otherClass.javaClass()) {
            return getRuntime().newFixnum(0);
        }
        if (otherClass.javaClass().isAssignableFrom(this.javaClass())) {
            return getRuntime().newFixnum(-1);
        }
        return getRuntime().newFixnum(1);
    }

    public RubyArray java_instance_methods() {
        return java_methods(javaClass().getMethods(), false);
    }

    public RubyArray declared_instance_methods() {
        return java_methods(javaClass().getDeclaredMethods(), false);
    }

	private RubyArray java_methods(Method[] methods, boolean isStatic) {
        RubyArray result = getRuntime().newArray(methods.length);
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (isStatic == Modifier.isStatic(method.getModifiers())) {
                result.append(JavaMethod.create(getRuntime(), method));
            }
        }
        return result;
	}

	public RubyArray java_class_methods() {
	    return java_methods(javaClass().getMethods(), true);
    }

	public RubyArray declared_class_methods() {
	    return java_methods(javaClass().getDeclaredMethods(), true);
    }

	public JavaMethod java_method(IRubyObject[] args) throws ClassNotFoundException {
        String methodName = args[0].asSymbol();
        Class[] argumentTypes = buildArgumentTypes(args);
        return JavaMethod.create(getRuntime(), javaClass(), methodName, argumentTypes);
    }

    public JavaMethod declared_method(IRubyObject[] args) throws ClassNotFoundException {
        String methodName = args[0].asSymbol();
        Class[] argumentTypes = buildArgumentTypes(args);
        return JavaMethod.createDeclared(getRuntime(), javaClass(), methodName, argumentTypes);
    }

    private Class[] buildArgumentTypes(IRubyObject[] args) throws ClassNotFoundException {
        if (args.length < 1) {
            throw getRuntime().newArgumentError(args.length, 1);
        }
        Class[] argumentTypes = new Class[args.length - 1];
        for (int i = 1; i < args.length; i++) {
            JavaClass type = for_name(this, args[i]);
            argumentTypes[i - 1] = type.javaClass();
        }
        return argumentTypes;
    }

    public RubyArray constructors() {
        return buildConstructors(javaClass().getConstructors());
    }

    public RubyArray declared_constructors() {
        return buildConstructors(javaClass().getDeclaredConstructors());
    }

    private RubyArray buildConstructors(Constructor[] constructors) {
        RubyArray result = getRuntime().newArray(constructors.length);
        for (int i = 0; i < constructors.length; i++) {
            result.append(new JavaConstructor(getRuntime(), constructors[i]));
        }
        return result;
    }

    public JavaConstructor constructor(IRubyObject[] args) {
        try {
            Class[] parameterTypes = buildClassArgs(args);
            Constructor constructor;
            constructor = javaClass().getConstructor(parameterTypes);
            return new JavaConstructor(getRuntime(), constructor);
        } catch (NoSuchMethodException nsme) {
            throw getRuntime().newNameError("no matching java constructor", null);
        }
    }

    public JavaConstructor declared_constructor(IRubyObject[] args) {
        try {
            Class[] parameterTypes = buildClassArgs(args);
            Constructor constructor;
            constructor = javaClass().getDeclaredConstructor (parameterTypes);
            return new JavaConstructor(getRuntime(), constructor);
        } catch (NoSuchMethodException nsme) {
            throw getRuntime().newNameError("no matching java constructor", null);
        }
    }

    private Class[] buildClassArgs(IRubyObject[] args) {
        Class[] parameterTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            String name = args[i].asSymbol();
            parameterTypes[i] = getRuntime().getJavaSupport().loadJavaClass(name);
        }
        return parameterTypes;
    }

    public JavaClass array_class() {
        return JavaClass.get(getRuntime(), Array.newInstance(javaClass(), 0).getClass());
    }

    public JavaObject new_array(IRubyObject lengthArgument) {
        if (! (lengthArgument instanceof RubyInteger)) {
            throw getRuntime().newTypeError(lengthArgument, getRuntime().getClass("Integer"));
        }
        int length = (int) ((RubyInteger) lengthArgument).getLongValue();
        return new JavaArray(getRuntime(), Array.newInstance(javaClass(), length));
    }

    public RubyArray fields() {
        return buildFieldResults(javaClass().getFields());
    }

    public RubyArray declared_fields() {
        return buildFieldResults(javaClass().getDeclaredFields());
    }
    
	private RubyArray buildFieldResults(Field[] fields) {
        RubyArray result = getRuntime().newArray(fields.length);
        for (int i = 0; i < fields.length; i++) {
            result.append(new JavaField(getRuntime(), fields[i]));
        }
        return result;
	}

	public JavaField field(IRubyObject name) {
		String stringName = name.asSymbol();
        try {
            Field field = javaClass().getField(stringName);
			return new JavaField(getRuntime(),field);
        } catch (NoSuchFieldException nsfe) {
            throw undefinedFieldError(stringName);
        }
    }

	public JavaField declared_field(IRubyObject name) {
		String stringName = name.asSymbol();
        try {
            Field field = javaClass().getDeclaredField(stringName);
			return new JavaField(getRuntime(),field);
        } catch (NoSuchFieldException nsfe) {
            throw undefinedFieldError(stringName);
        }
    }

    private RaiseException undefinedFieldError(String name) {
        return getRuntime().newNameError("undefined field '" + name + "' for class '" + javaClass().getName() + "'", name);
    }

    public RubyArray interfaces() {
        Class[] interfaces = javaClass().getInterfaces();
        RubyArray result = getRuntime().newArray(interfaces.length);
        for (int i = 0; i < interfaces.length; i++) {
            result.append(JavaClass.get(getRuntime(), interfaces[i]));
        }
        return result;
    }

    public RubyBoolean primitive_p() {
        return getRuntime().newBoolean(isPrimitive());
    }

    public RubyBoolean assignable_from_p(IRubyObject other) {
        if (! (other instanceof JavaClass)) {
            throw getRuntime().newTypeError("assignable_from requires JavaClass (" + other.getType() + " given)");
        }

        Class otherClass = ((JavaClass) other).javaClass();

        if (!javaClass().isPrimitive() && otherClass == Void.TYPE ||
            javaClass().isAssignableFrom(otherClass)) {
            return getRuntime().getTrue();
        }
        otherClass = JavaUtil.primitiveToWrapper(otherClass);
        Class thisJavaClass = JavaUtil.primitiveToWrapper(javaClass());
        if (thisJavaClass.isAssignableFrom(otherClass)) {
            return getRuntime().getTrue();
        }
        if (Number.class.isAssignableFrom(thisJavaClass)) {
            if (Number.class.isAssignableFrom(otherClass)) {
                return getRuntime().getTrue();
            }
            if (otherClass.equals(Character.class)) {
                return getRuntime().getTrue();
            }
        }
        if (thisJavaClass.equals(Character.class)) {
            if (Number.class.isAssignableFrom(otherClass)) {
                return getRuntime().getTrue();
            }
        }
        return getRuntime().getFalse();
    }

    private boolean isPrimitive() {
        return javaClass().isPrimitive();
    }

    public JavaClass component_type() {
        if (! javaClass().isArray()) {
            throw getRuntime().newTypeError("not a java array-class");
        }
        return JavaClass.get(getRuntime(), javaClass().getComponentType());
    }
}
