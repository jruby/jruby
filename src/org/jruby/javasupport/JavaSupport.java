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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.javasupport;

import java.lang.reflect.Member;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.util.ObjectProxyCache;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callback.Callback;

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
            IRubyObject proxy = clazz.allocate();
            JavaObject wrappedObject = JavaObject.wrap(clazz.getRuntime(), javaObject);
            proxy.dataWrapStruct(wrappedObject);
            
            return proxy;
        }
    };
    
    private boolean active;
    
    // There's not a compelling reason to keep JavaClass instances in a weak map
    // (any proxies created are [were] kept in a non-weak map, so in most cases they will
    // stick around anyway), and some good reasons not to (JavaClass creation is
    // expensive, for one; many lookups are performed when passing parameters to/from
    // methods; etc.).
    // TODO: faster custom concurrent map
    private final ConcurrentHashMap<Class,JavaClass> javaClassCache =
        new ConcurrentHashMap<Class, JavaClass>(128);
    
    // FIXME: needs to be rethought
    private final Map matchCache = Collections.synchronizedMap(new HashMap(128));

    private Callback concreteProxyCallback;

    private RubyModule javaModule;
    private RubyModule javaUtilitiesModule;
    private RubyModule javaArrayUtilitiesModule;
    private RubyClass javaObjectClass;
    private JavaClass objectJavaClass;
    private RubyClass javaClassClass;
    private RubyClass javaArrayClass;
    private RubyClass javaProxyClass;
    private RubyClass javaFieldClass;
    private RubyClass javaMethodClass;
    private RubyClass javaConstructorClass;
    private RubyModule javaInterfaceTemplate;
    private RubyModule packageModuleTemplate;
    private RubyClass arrayProxyClass;
    private RubyClass concreteProxyClass;
    
    private final Map<String, JavaClass> nameClassMap = new HashMap<String, JavaClass>();
    
    public JavaSupport(Ruby ruby) {
        this.runtime = ruby;
    }

    final synchronized void setConcreteProxyCallback(Callback concreteProxyCallback) {
        if (this.concreteProxyCallback == null) {
            this.concreteProxyCallback = concreteProxyCallback;
        }
    }
    
    final Callback getConcreteProxyCallback() {
        return concreteProxyCallback;
    }
    
    final Map getMatchCache() {
        return matchCache;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    private Class loadJavaClass(String className) throws ClassNotFoundException {
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
            throw runtime.newNameError("security exception loading Java class " + className, className, se);
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
            throw runtime.newNameError("security: cannot load Java class " + className, className, se, false);
        }
    }

    public JavaClass getJavaClassFromCache(Class clazz) {
        return javaClassCache.get(clazz);
    }
    
    public void putJavaClassIntoCache(JavaClass clazz) {
        javaClassCache.put(clazz.javaClass(), clazz);
    }

    public void handleNativeException(Throwable exception, Member target) {
        if (exception instanceof RaiseException) {
            throw (RaiseException) exception;
        }
        throw createRaiseException(exception, target);
    }

    private RaiseException createRaiseException(Throwable exception, Member target) {
        RaiseException re = RaiseException.createNativeRaiseException(runtime, exception, target);
        
        return re;
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
    
    public RubyModule getJavaModule() {
        RubyModule module;
        if ((module = javaModule) != null) return module;
        return javaModule = runtime.fastGetModule("Java");
    }
    
    public RubyModule getJavaUtilitiesModule() {
        RubyModule module;
        if ((module = javaUtilitiesModule) != null) return module;
        return javaUtilitiesModule = runtime.fastGetModule("JavaUtilities");
    }
    
    public RubyModule getJavaArrayUtilitiesModule() {
        RubyModule module;
        if ((module = javaArrayUtilitiesModule) != null) return module;
        return javaArrayUtilitiesModule = runtime.fastGetModule("JavaArrayUtilities");
    }
    
    public RubyClass getJavaObjectClass() {
        RubyClass clazz;
        if ((clazz = javaObjectClass) != null) return clazz;
        return javaObjectClass = getJavaModule().fastGetClass("JavaObject");
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
        return javaArrayClass = getJavaModule().fastGetClass("JavaArray");
    }
    
    public RubyClass getJavaClassClass() {
        RubyClass clazz;
        if ((clazz = javaClassClass) != null) return clazz;
        return javaClassClass = getJavaModule().fastGetClass("JavaClass");
    }
    
    public RubyModule getJavaInterfaceTemplate() {
        RubyModule module;
        if ((module = javaInterfaceTemplate) != null) return module;
        return javaInterfaceTemplate = runtime.fastGetModule("JavaInterfaceTemplate");
    }
    
    public RubyModule getPackageModuleTemplate() {
        RubyModule module;
        if ((module = packageModuleTemplate) != null) return module;
        return packageModuleTemplate = runtime.fastGetModule("JavaPackageModuleTemplate");
    }
    
    public RubyClass getJavaProxyClass() {
        RubyClass clazz;
        if ((clazz = javaProxyClass) != null) return clazz;
        return javaProxyClass = runtime.fastGetClass("JavaProxy");
    }
    
    public RubyClass getConcreteProxyClass() {
        RubyClass clazz;
        if ((clazz = concreteProxyClass) != null) return clazz;
        return concreteProxyClass = runtime.fastGetClass("ConcreteJavaProxy");
    }
    
    public RubyClass getArrayProxyClass() {
        RubyClass clazz;
        if ((clazz = arrayProxyClass) != null) return clazz;
        return arrayProxyClass = runtime.fastGetClass("ArrayJavaProxy");
    }
    
    public RubyClass getJavaFieldClass() {
        RubyClass clazz;
        if ((clazz = javaFieldClass) != null) return clazz;
        return javaFieldClass = getJavaModule().fastGetClass("JavaField");
    }

    public RubyClass getJavaMethodClass() {
        RubyClass clazz;
        if ((clazz = javaMethodClass) != null) return clazz;
        return javaMethodClass = getJavaModule().fastGetClass("JavaMethod");
    }

    public RubyClass getJavaConstructorClass() {
        RubyClass clazz;
        if ((clazz = javaConstructorClass) != null) return clazz;
        return javaConstructorClass = getJavaModule().fastGetClass("JavaConstructor");
    }

}
