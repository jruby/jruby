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
 * Copyright (C) 2006 Kresten Krab Thorup <krab@gnu.org>
 * Copyright (C) 2007 Miguel Covarrubias <mlcovarrubias@gmail.com>
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

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyInteger;
import org.jruby.RubyMethod;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callback.Callback;
import org.jruby.util.collections.IntHashMap;

public class JavaClass extends JavaObject {

    private JavaClass(Ruby runtime, Class javaClass) {
        super(runtime, (RubyClass) runtime.getModule("Java").getClass("JavaClass"), javaClass);
    }
    
    public static synchronized JavaClass get(Ruby runtime, Class klass) {
        JavaClass javaClass = runtime.getJavaSupport().getJavaClassFromCache(klass);
        if (javaClass == null) {
            javaClass = new JavaClass(runtime, klass);
            runtime.getJavaSupport().putJavaClassIntoCache(javaClass);
        }
        return javaClass;
    }

    public static RubyClass createJavaClassClass(Ruby runtime, RubyModule javaModule) {
        // FIXME: Determine if a real allocator is needed here. Do people want to extend
        // JavaClass? Do we want them to do that? Can you Class.new(JavaClass)? Should
        // you be able to?
        // TODO: NOT_ALLOCATABLE_ALLOCATOR is probably ok here, since we don't intend for people to monkey with
        // this type and it can't be marshalled. Confirm. JRUBY-415
        RubyClass result = javaModule.defineClassUnder("JavaClass", javaModule.getClass("JavaObject"), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR); 

    	CallbackFactory callbackFactory = runtime.callbackFactory(JavaClass.class);
        
        result.includeModule(runtime.getModule("Comparable"));
        
        JavaObject.registerRubyMethods(runtime, result);

        result.getMetaClass().defineFastMethod("for_name", 
                callbackFactory.getFastSingletonMethod("for_name", IRubyObject.class));
        result.defineFastMethod("public?", 
                callbackFactory.getFastMethod("public_p"));
        result.defineFastMethod("protected?", 
                callbackFactory.getFastMethod("protected_p"));
        result.defineFastMethod("private?", 
                callbackFactory.getFastMethod("private_p"));
        result.defineFastMethod("final?", 
                callbackFactory.getFastMethod("final_p"));
        result.defineFastMethod("interface?", 
                callbackFactory.getFastMethod("interface_p"));
        result.defineFastMethod("array?", 
                callbackFactory.getFastMethod("array_p"));
        result.defineFastMethod("name", 
                callbackFactory.getFastMethod("name"));
        result.defineFastMethod("simple_name",
                callbackFactory.getFastMethod("simple_name"));
        result.defineFastMethod("to_s", 
                callbackFactory.getFastMethod("name"));
        result.defineFastMethod("superclass", 
                callbackFactory.getFastMethod("superclass"));
        result.defineFastMethod("<=>", 
                callbackFactory.getFastMethod("op_cmp", IRubyObject.class));
        result.defineFastMethod("java_instance_methods", 
                callbackFactory.getFastMethod("java_instance_methods"));
        result.defineFastMethod("java_class_methods", 
                callbackFactory.getFastMethod("java_class_methods"));
        result.defineFastMethod("java_method", 
                callbackFactory.getFastOptMethod("java_method"));
        result.defineFastMethod("constructors", 
                callbackFactory.getFastMethod("constructors"));
        result.defineFastMethod("constructor", 
                callbackFactory.getFastOptMethod("constructor"));
        result.defineFastMethod("array_class", 
                callbackFactory.getFastMethod("array_class"));
        result.defineFastMethod("new_array", 
                callbackFactory.getFastMethod("new_array", IRubyObject.class));
        result.defineFastMethod("fields", 
                callbackFactory.getFastMethod("fields"));
        result.defineFastMethod("field", 
                callbackFactory.getFastMethod("field", IRubyObject.class));
        result.defineFastMethod("interfaces", 
                callbackFactory.getFastMethod("interfaces"));
        result.defineFastMethod("primitive?", 
                callbackFactory.getFastMethod("primitive_p"));
        result.defineFastMethod("assignable_from?", 
                callbackFactory.getFastMethod("assignable_from_p", IRubyObject.class));
        result.defineFastMethod("component_type", 
                callbackFactory.getFastMethod("component_type"));
        result.defineFastMethod("declared_instance_methods", 
                callbackFactory.getFastMethod("declared_instance_methods"));
        result.defineFastMethod("declared_class_methods", 
                callbackFactory.getFastMethod("declared_class_methods"));
        result.defineFastMethod("declared_fields", 
                callbackFactory.getFastMethod("declared_fields"));
        result.defineFastMethod("declared_field", 
                callbackFactory.getFastMethod("declared_field", IRubyObject.class));
        result.defineFastMethod("declared_constructors", 
                callbackFactory.getFastMethod("declared_constructors"));
        result.defineFastMethod("declared_constructor", 
                callbackFactory.getFastOptMethod("declared_constructor"));
        result.defineFastMethod("declared_classes", 
                callbackFactory.getFastMethod("declared_classes"));
        result.defineFastMethod("declared_method", 
                callbackFactory.getFastOptMethod("declared_method"));
        result.defineFastMethod("define_instance_methods_for_proxy", 
                callbackFactory.getFastMethod("define_instance_methods_for_proxy", IRubyObject.class));
        
        result.getMetaClass().undefineMethod("new");
        result.getMetaClass().undefineMethod("allocate");

        return result;
    }
    
    public static synchronized JavaClass for_name(IRubyObject recv, IRubyObject name) {
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
                methodsWithName = RubyArray.newArrayLight(getRuntime());
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
        Callback method;
        if(methods.size()>1) {
            method = new Callback() {
                    private IntHashMap matchingMethods = new IntHashMap();
                    public IRubyObject execute(IRubyObject self, IRubyObject[] args, Block block) {
                        int len = args.length;
                        IRubyObject[] argsArray = new IRubyObject[len + 1];
                
                        argsArray[0] = self.getInstanceVariable("@java_object");

                        int argsTypeHash = 0;
                        for (int j = 0; j < len; j++) {
                            argsArray[j+1] = Java.ruby_to_java(proxy, args[j], Block.NULL_BLOCK);
                            argsTypeHash += 3*args[j].getMetaClass().id;
                        }

                        IRubyObject match = (IRubyObject)matchingMethods.get(argsTypeHash);
                        if (match == null) {
                            match = Java.matching_method_internal(javaUtilities, methods, argsArray, 1, len);
                            matchingMethods.put(argsTypeHash, match);
                        }

                        return Java.java_to_ruby(self, ((JavaMethod)match).invoke(argsArray), Block.NULL_BLOCK);
                    }

                    public Arity getArity() {
                        return Arity.optional();
                    }
                };
        } else {
            final JavaMethod METHOD = (JavaMethod)methods.eltInternal(0);
            method = new Callback() {
                    public IRubyObject execute(IRubyObject self, IRubyObject[] args, Block block) {
                        int len = args.length;
                        IRubyObject[] argsArray = new IRubyObject[len + 1];
                        argsArray[0] = self.getInstanceVariable("@java_object");
                        for(int j = 0; j < len; j++) {
                            argsArray[j+1] = Java.ruby_to_java(proxy, args[j], Block.NULL_BLOCK);
                        }
                        return Java.java_to_ruby(self, METHOD.invoke(argsArray), Block.NULL_BLOCK);
                    }

                    public Arity getArity() {
                        return Arity.optional();
                    }
                };
        }
        
        for(Iterator iter = names.iterator(); iter.hasNext(); ) {
            String methodName = (String) iter.next();
            
            // We do not override class since it is too important to be overridden by getClass
            // short name.
            if (!methodName.equals("class")) {
                proxy.defineFastMethod(methodName, method);
                
                String rubyCasedName = getRubyCasedName(methodName);
                if (rubyCasedName != null) {
                    proxy.defineAlias(rubyCasedName, methodName);
                }
            }
        }
    }
    
    private static final Pattern CAMEL_CASE_SPLITTER = Pattern.compile("([a-z])([A-Z])");
    
    public static String getRubyCasedName(String javaCasedName) {
        Matcher m = CAMEL_CASE_SPLITTER.matcher(javaCasedName);

        String rubyCasedName = m.replaceAll("$1_$2").toLowerCase();
        
        if (rubyCasedName.equals(javaCasedName)) {
            return null;
        }
        
        return rubyCasedName;
    }
    
    private static final Callback __jsend_method = new Callback() {
            public IRubyObject execute(IRubyObject self, IRubyObject[] args, Block block) {
                String methodSymbol = args[0].asSymbol();
                RubyMethod method = (org.jruby.RubyMethod)self.getMetaClass().newMethod(self, methodSymbol, true);
                int v = RubyNumeric.fix2int(method.arity());
                String name = args[0].asSymbol();

                IRubyObject[] newArgs = new IRubyObject[args.length - 1];
                System.arraycopy(args, 1, newArgs, 0, newArgs.length);

                if(v < 0 || v == (newArgs.length)) {
                    return self.callMethod(self.getRuntime().getCurrentContext(), name, newArgs, CallType.FUNCTIONAL, block);
                } else {
                    return self.callMethod(self.getRuntime().getCurrentContext(),self.getMetaClass().getSuperClass(), name, newArgs, CallType.SUPER, block);
                }
            }

            public Arity getArity() {
                return Arity.optional();
            }
        };

    public IRubyObject define_instance_methods_for_proxy(IRubyObject arg) {
        assert arg instanceof RubyClass;

        Map aliasesClump = getPropertysClumped();
        Map methodsClump = getMethodsClumped(false);
        RubyClass proxy = (RubyClass) arg;

        proxy.defineFastMethod("__jsend!", __jsend_method);
        
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
    

    private static String getSimpleName(Class class_) {
 		if (class_.isArray()) {
 			return getSimpleName(class_.getComponentType()) + "[]";
 		}
 
 		String className = class_.getName();
 
        int i = className.lastIndexOf('$');
 		if (i != -1) {
            do {
 				i++;
 			} while (i < className.length() && Character.isDigit(className.charAt(i)));
 			return className.substring(i);
 		}
 
 		return className.substring(className.lastIndexOf('.') + 1);
 	}

    public RubyString simple_name() {
        return getRuntime().newString(getSimpleName(javaClass()));
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
    
    public RubyArray declared_classes() {
        Class[] classes = javaClass().getDeclaredClasses();
        List accessibleClasses = new ArrayList();
        for (int i = 0; i < classes.length; i++) {
            if (Modifier.isPublic(classes[i].getModifiers())) {
                accessibleClasses.add(classes[i]);
            }
        }
        return buildClasses((Class[]) accessibleClasses.toArray(new Class[accessibleClasses.size()]));
    }
    
    private RubyArray buildClasses(Class [] classes) {
        RubyArray result = getRuntime().newArray(classes.length);
        for (int i = 0; i < classes.length; i++) {
            result.append(new JavaClass(getRuntime(), classes[i]));
        }
        return result;
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
        if (lengthArgument instanceof RubyInteger) {
            // one-dimensional array
        int length = (int) ((RubyInteger) lengthArgument).getLongValue();
        return new JavaArray(getRuntime(), Array.newInstance(javaClass(), length));
        } else if (lengthArgument instanceof RubyArray) {
            // n-dimensional array
            List list = ((RubyArray)lengthArgument).getList();
            int length = list.size();
            if (length == 0) {
                throw getRuntime().newArgumentError("empty dimensions specifier for java array");
    }
            int[] dimensions = new int[length];
            for (int i = length; --i >= 0; ) {
                IRubyObject dimensionLength = (IRubyObject)list.get(i);
                if ( !(dimensionLength instanceof RubyInteger) ) {
                    throw getRuntime()
                        .newTypeError(dimensionLength, getRuntime().getClass("Integer"));
                }
                dimensions[i] = (int) ((RubyInteger) dimensionLength).getLongValue();
            }
            return new JavaArray(getRuntime(), Array.newInstance(javaClass(), dimensions));
        } else {
            throw getRuntime().newArgumentError(
                  "invalid length or dimensions specifier for java array" +
                  " - must be Integer or Array of Integer");
        }
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
        return assignable(javaClass(), otherClass) ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    static boolean assignable(Class thisClass, Class otherClass) {
        if(!thisClass.isPrimitive() && otherClass == Void.TYPE ||
            thisClass.isAssignableFrom(otherClass)) {
            return true;
        }

        otherClass = JavaUtil.primitiveToWrapper(otherClass);
        thisClass = JavaUtil.primitiveToWrapper(thisClass);

        if(thisClass.isAssignableFrom(otherClass)) {
            return true;
        }
        if(Number.class.isAssignableFrom(thisClass)) {
            if(Number.class.isAssignableFrom(otherClass)) {
                return true;
            }
            if(otherClass.equals(Character.class)) {
                return true;
            }
        }
        if(thisClass.equals(Character.class)) {
            if(Number.class.isAssignableFrom(otherClass)) {
                return true;
            }
        }
        return false;
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
