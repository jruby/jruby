/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2007 William N Dortch <bill.dortch@gmail.com>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.javasupport;

import org.jruby.util.collections.MapBasedClassValue;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.Unrescuable;
import org.jruby.util.collections.ClassValue;
import org.jruby.javasupport.proxy.JavaProxyClass;
import org.jruby.javasupport.util.ObjectProxyCache;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.WeakIdentityHashMap;
import org.jruby.util.cli.Options;
import org.jruby.util.collections.ClassValueCalculator;

public class JavaSupport {
    private static final Map<String,Class> PRIMITIVE_CLASSES = new HashMap<String,Class>();
    static {
        PRIMITIVE_CLASSES.put("boolean", Boolean.TYPE);
        PRIMITIVE_CLASSES.put("byte", Byte.TYPE);
        PRIMITIVE_CLASSES.put("char", Character.TYPE);
        PRIMITIVE_CLASSES.put("short", Short.TYPE);
        PRIMITIVE_CLASSES.put("int", Integer.TYPE);
        PRIMITIVE_CLASSES.put("long", Long.TYPE);
        PRIMITIVE_CLASSES.put("float", Float.TYPE);
        PRIMITIVE_CLASSES.put("double", Double.TYPE);
    }
    
    public static Class getPrimitiveClass(String primitiveType) {
        return PRIMITIVE_CLASSES.get(primitiveType);
    }

    private final Ruby runtime;
    
    private final ObjectProxyCache<IRubyObject,RubyClass> objectProxyCache = 
        // TODO: specifying soft refs, may want to compare memory consumption,
        // behavior with weak refs (specify WEAK in place of SOFT below)
        new ObjectProxyCache<IRubyObject,RubyClass>(ObjectProxyCache.ReferenceType.WEAK) {

        public IRubyObject allocateProxy(Object javaObject, RubyClass clazz) {
            return Java.allocateProxy(javaObject, clazz);
        }
    };
    
    private final ClassValue<JavaClass> javaClassCache;
    private final ClassValue<RubyModule> proxyClassCache;
    private static final Constructor<? extends ClassValue> CLASS_VALUE_CONSTRUCTOR;
    
    static {
        Constructor<? extends ClassValue> constructor = null;

        if (Options.INVOKEDYNAMIC_CLASS_VALUES.load()) {
            try {
                // try to load the ClassValue class. If it succeeds, we can use our
                // ClassValue-based cache.
                Class.forName("java.lang.ClassValue");
                constructor = (Constructor<ClassValue>)Class.forName("org.jruby.java.proxies.ClassValueProxyCache").getConstructor(ClassValueCalculator.class);
            }
            catch (Exception ex) {
                // fall through to Map version
            }
        }

        if (constructor == null) {
            try {
                constructor = MapBasedClassValue.class.getConstructor(ClassValueCalculator.class);
            } catch (Exception ex2) {
                throw new RuntimeException(ex2);
            }
        }

        CLASS_VALUE_CONSTRUCTOR = constructor;
    }

    private RubyModule javaModule;
    private RubyModule javaUtilitiesModule;
    private RubyModule javaArrayUtilitiesModule;
    private RubyClass javaObjectClass;
    private JavaClass objectJavaClass;
    private RubyClass javaClassClass;
    private RubyClass javaArrayClass;
    private RubyClass javaProxyClass;
    private RubyClass arrayJavaProxyCreatorClass;
    private RubyClass javaFieldClass;
    private RubyClass javaMethodClass;
    private RubyClass javaConstructorClass;
    private RubyModule javaInterfaceTemplate;
    private RubyModule packageModuleTemplate;
    private RubyClass arrayProxyClass;
    private RubyClass concreteProxyClass;
    private RubyClass mapJavaProxy;
    
    private final Map<String, JavaClass> nameClassMap = new HashMap<String, JavaClass>();

    private final Map<Object, Object[]> javaObjectVariables = new WeakIdentityHashMap();

    // A cache of all JavaProxyClass objects created for this runtime
    private Map<Set<?>, JavaProxyClass> javaProxyClassCache = Collections.synchronizedMap(new HashMap<Set<?>, JavaProxyClass>());
    
    public JavaSupport(final Ruby ruby) {
        this.runtime = ruby;
        
        try {
            this.javaClassCache = CLASS_VALUE_CONSTRUCTOR.newInstance(new ClassValueCalculator<JavaClass>() {
                @Override
                public JavaClass computeValue(Class<?> cls) {
                    return new JavaClass(runtime, cls);
                }
            });
            this.proxyClassCache = CLASS_VALUE_CONSTRUCTOR.newInstance(new ClassValueCalculator<RubyModule>() {
                @Override
                public RubyModule computeValue(Class<?> cls) {
                    return Java.createProxyClassForClass(runtime, cls);
                }
            });
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public Class loadJavaClass(String className) throws ClassNotFoundException {
        Class primitiveClass;
        if ((primitiveClass = PRIMITIVE_CLASSES.get(className)) == null) {
            if (!Ruby.isSecurityRestricted()) {
                return Class.forName(className, true, runtime.getJRubyClassLoader());
            }
            return Class.forName(className);
        }
        return primitiveClass;
    }
    
    public Class loadJavaClassVerbose(String className) {
        try {
            return loadJavaClass(className);
        } catch (ClassNotFoundException cnfExcptn) {
            throw runtime.newNameError("cannot load Java class " + className, className, cnfExcptn);
        } catch (ExceptionInInitializerError eiie) {
            throw runtime.newNameError("cannot initialize Java class " + className, className, eiie);
        } catch (LinkageError le) {
            throw runtime.newNameError("cannot link Java class " + className + ", probable missing dependency: " + le.getLocalizedMessage(), className, le);
        } catch (SecurityException se) {
            if (runtime.isVerbose()) se.printStackTrace(runtime.getErrorStream());
            throw runtime.newSecurityError(se.getLocalizedMessage());
        }
    }
    
    public Class loadJavaClassQuiet(String className) {
        try {
            return loadJavaClass(className);
        } catch (ClassNotFoundException cnfExcptn) {
            throw runtime.newNameError("cannot load Java class " + className, className, cnfExcptn, false);
        } catch (ExceptionInInitializerError eiie) {
            throw runtime.newNameError("cannot initialize Java class " + className, className, eiie, false);
        } catch (LinkageError le) {
            throw runtime.newNameError("cannot link Java class " + className, className, le, false);
        } catch (SecurityException se) {
            throw runtime.newSecurityError(se.getLocalizedMessage());
        }
    }

    public JavaClass getJavaClassFromCache(Class clazz) {
        return javaClassCache.get(clazz);
    }

    public RubyModule getProxyClassFromCache(Class clazz) {
        return proxyClassCache.get(clazz);
    }

    public void handleNativeException(Throwable exception, Member target) {
        if (exception instanceof RaiseException) {
            // allow RaiseExceptions to propagate
            throw (RaiseException) exception;
        } else if (exception instanceof Unrescuable) {
            // allow "unrescuable" flow-control exceptions to propagate
            if (exception instanceof Error) {
                throw (Error)exception;
            } else if (exception instanceof RuntimeException) {
                throw (RuntimeException)exception;
            }
        }
        throw createRaiseException(exception, target);
    }

    private RaiseException createRaiseException(Throwable exception, Member target) {
        return RaiseException.createNativeRaiseException(runtime, exception, target);
    }

    public ObjectProxyCache<IRubyObject,RubyClass> getObjectProxyCache() {
        return objectProxyCache;
    }

    // not synchronizing these methods, no harm if these values get set more
    // than once.
    // (also note that there's no chance of getting a partially initialized
    // class/module, as happens-before is guaranteed by volatile write/read
    // of constants table.)
    
    public Map<String, JavaClass> getNameClassMap() {
        return nameClassMap;
    }

    public void setJavaObjectVariable(Object o, int i, Object v) {
        synchronized (javaObjectVariables) {
            Object[] vars = javaObjectVariables.get(o);
            if (vars == null) {
                vars = new Object[i + 1];
                javaObjectVariables.put(o, vars);
            } else if (vars.length <= i) {
                Object[] newVars = new Object[i + 1];
                System.arraycopy(vars, 0, newVars, 0, vars.length);
                javaObjectVariables.put(o, newVars);
                vars = newVars;
            }
            vars[i] = v;
        }
    }
    
    public Object getJavaObjectVariable(Object o, int i) {
        if (i == -1) return null;
        
        synchronized (javaObjectVariables) {
            Object[] vars = javaObjectVariables.get(o);
            if (vars == null || vars.length <= i) return null;
            return vars[i];
        }
    }
    
    public RubyModule getJavaModule() {
        RubyModule module;
        if ((module = javaModule) != null) return module;
        return javaModule = runtime.getModule("Java");
    }
    
    public RubyModule getJavaUtilitiesModule() {
        RubyModule module;
        if ((module = javaUtilitiesModule) != null) return module;
        return javaUtilitiesModule = runtime.getModule("JavaUtilities");
    }
    
    public RubyModule getJavaArrayUtilitiesModule() {
        RubyModule module;
        if ((module = javaArrayUtilitiesModule) != null) return module;
        return javaArrayUtilitiesModule = runtime.getModule("JavaArrayUtilities");
    }
    
    public RubyClass getJavaObjectClass() {
        RubyClass clazz;
        if ((clazz = javaObjectClass) != null) return clazz;
        return javaObjectClass = getJavaModule().getClass("JavaObject");
    }
    
    public JavaClass getObjectJavaClass() {
        return objectJavaClass;
    }
    
    public void setObjectJavaClass(JavaClass objectJavaClass) {
        this.objectJavaClass = objectJavaClass;
    }

    public RubyClass getJavaArrayClass() {
        RubyClass clazz;
        if ((clazz = javaArrayClass) != null) return clazz;
        return javaArrayClass = getJavaModule().getClass("JavaArray");
    }
    
    public RubyClass getJavaClassClass() {
        RubyClass clazz;
        if ((clazz = javaClassClass) != null) return clazz;
        return javaClassClass = getJavaModule().getClass("JavaClass");
    }

    public RubyModule getJavaInterfaceTemplate() {
        RubyModule module;
        if ((module = javaInterfaceTemplate) != null) return module;
        return javaInterfaceTemplate = runtime.getModule("JavaInterfaceTemplate");
    }

    public RubyModule getPackageModuleTemplate() {
        RubyModule module;
        if ((module = packageModuleTemplate) != null) return module;
        return packageModuleTemplate = runtime.getModule("JavaPackageModuleTemplate");
    }
    
    public RubyClass getJavaProxyClass() {
        RubyClass clazz;
        if ((clazz = javaProxyClass) != null) return clazz;
        return javaProxyClass = runtime.getClass("JavaProxy");
    }

    public RubyClass getArrayJavaProxyCreatorClass() {
        RubyClass clazz;
        if ((clazz = arrayJavaProxyCreatorClass) != null) return clazz;
        return arrayJavaProxyCreatorClass = runtime.getClass("ArrayJavaProxyCreator");
    }
    
    public RubyClass getConcreteProxyClass() {
        RubyClass clazz;
        if ((clazz = concreteProxyClass) != null) return clazz;
        return concreteProxyClass = runtime.getClass("ConcreteJavaProxy");
    }

    public RubyClass getMapJavaProxyClass() {
        RubyClass clazz;
        if ((clazz = mapJavaProxy) != null) return clazz;
        return mapJavaProxy = runtime.getClass("MapJavaProxy");
    }
    
    public RubyClass getArrayProxyClass() {
        RubyClass clazz;
        if ((clazz = arrayProxyClass) != null) return clazz;
        return arrayProxyClass = runtime.getClass("ArrayJavaProxy");
    }
    
    public RubyClass getJavaFieldClass() {
        RubyClass clazz;
        if ((clazz = javaFieldClass) != null) return clazz;
        return javaFieldClass = getJavaModule().getClass("JavaField");
    }

    public RubyClass getJavaMethodClass() {
        RubyClass clazz;
        if ((clazz = javaMethodClass) != null) return clazz;
        return javaMethodClass = getJavaModule().getClass("JavaMethod");
    }

    public RubyClass getJavaConstructorClass() {
        RubyClass clazz;
        if ((clazz = javaConstructorClass) != null) return clazz;
        return javaConstructorClass = getJavaModule().getClass("JavaConstructor");
    }

    public Map<Set<?>, JavaProxyClass> getJavaProxyClassCache() {
        return this.javaProxyClassCache;
    }

}
