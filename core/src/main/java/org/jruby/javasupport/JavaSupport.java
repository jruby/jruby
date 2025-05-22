/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.Unrescuable;
import org.jruby.javasupport.binding.AssignedName;
import org.jruby.javasupport.ext.JavaExtensions;
import org.jruby.javasupport.proxy.JavaProxyClass;
import org.jruby.javasupport.util.ObjectProxyCache;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.Loader;
import org.jruby.util.collections.ClassValue;
import org.jruby.util.collections.ClassValueCalculator;

import java.lang.reflect.Member;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import static org.jruby.javasupport.Java.initCause;

public abstract class JavaSupport {

    protected final Ruby runtime;

    @Deprecated
    private final ClassValue<JavaClass> javaClassCache;
    private final ClassValue<RubyModule> proxyClassCache;

    static final class UnfinishedProxy extends ReentrantLock {
        final RubyModule proxy;
        UnfinishedProxy(RubyModule proxy) {
            this.proxy = proxy;
        }
    }

    private final Map<Class, UnfinishedProxy> unfinishedProxies;

    private final ObjectProxyCache<IRubyObject,RubyClass> objectProxyCache =
            // TODO: specifying soft refs, may want to compare memory consumption,
            // behavior with weak refs (specify WEAK in place of SOFT below)
            new ObjectProxyCache<IRubyObject,RubyClass>(ObjectProxyCache.ReferenceType.WEAK) {

                public IRubyObject allocateProxy(Object javaObject, RubyClass clazz) {
                    return Java.allocateProxy(javaObject, clazz);
                }
            };

    private RubyModule javaModule;
    private RubyModule javaUtilitiesModule;
    private RubyModule javaArrayUtilitiesModule;
    private RubyClass javaObjectClass;
    @Deprecated
    private Object objectJavaClass;
    private RubyClass javaClassClass;
    private RubyClass javaPackageClass;
    private RubyClass javaArrayClass;
    private RubyClass javaProxyClass;
    private RubyClass arrayJavaProxyCreatorClass;
    private RubyClass javaFieldClass;
    private RubyClass javaMethodClass;
    private RubyClass javaConstructorClass;
    private RubyModule javaInterfaceTemplate;
    private RubyClass arrayProxyClass;
    private RubyClass concreteProxyClass;
    private RubyClass mapJavaProxy;
    private RubyClass javaProxyConstructorClass;

    @SuppressWarnings("deprecation")
    public JavaSupport(final Ruby runtime) {
        this.runtime = runtime;

        this.javaClassCache = ClassValue.newInstance(klass -> new JavaClass(runtime, getJavaClassClass(), klass));

        this.proxyClassCache = ClassValue.newInstance(new ClassValueCalculator<RubyModule>() {
            /**
             * Because of the complexity of processing a given class and all its dependencies,
             * we opt to synchronize this logic. Creation of all proxies goes through here,
             * allowing us to skip some threading work downstream.
             *
             * Note: when this is used with StableClassValue, the synchronization is unnecessary, and should be removed
             * when only the stable form remains.
             */
            @Override
            public synchronized RubyModule computeValue(Class<?> klass) {
                RubyModule proxyKlass = Java.createProxyClassForClass(runtime, klass);
                JavaExtensions.define(runtime, klass, proxyKlass); // (lazy) load extensions
                return proxyKlass;
            }
        });
        // Proxy creation is synchronized (see above) so a HashMap is fine for recursion detection.
        this.unfinishedProxies = new ConcurrentHashMap<>(8, 0.75f, 1);
    }

    @Deprecated
    public Class loadJavaClassVerbose(String className) {
        try {
            return loadJavaClass(className);
        } catch (ClassNotFoundException ex) {
            throw initCause(runtime.newNameError("cannot load Java class " + className, className, ex), ex);
        } catch (ExceptionInInitializerError ex) {
            throw initCause(runtime.newNameError("cannot initialize Java class " + className, className, ex), ex);
        } catch (LinkageError ex) {
            throw initCause(runtime.newNameError("cannot link Java class " + className + ", probable missing dependency: " + ex.getLocalizedMessage(), className, ex), ex);
        } catch (SecurityException ex) {
            if (runtime.isVerbose()) ex.printStackTrace(runtime.getErrorStream());
            throw initCause(runtime.newSecurityError(ex.getLocalizedMessage()), ex);
        }
    }

    @Deprecated
    public Class loadJavaClassQuiet(String className) {
        try {
            return loadJavaClass(className);
        } catch (ClassNotFoundException ex) {
            throw initCause(runtime.newNameError("cannot load Java class " + className, className, ex, false), ex);
        } catch (ExceptionInInitializerError ex) {
            throw initCause(runtime.newNameError("cannot initialize Java class " + className, className, ex, false), ex);
        } catch (LinkageError ex) {
            throw initCause(runtime.newNameError("cannot link Java class " + className, className, ex, false), ex);
        } catch (SecurityException ex) {
            throw initCause(runtime.newSecurityError(ex.getLocalizedMessage()), ex);
        }
    }

    public void handleNativeException(Throwable exception, Member target) {
        if ( exception instanceof RaiseException) {
            // allow RaiseExceptions to propagate
            throw (RaiseException) exception;
        }
        if (exception instanceof Unrescuable) {
            // allow "unrescuable" flow-control exceptions to propagate
            if ( exception instanceof Error ) {
                throw (Error) exception;
            }
            if ( exception instanceof RuntimeException ) {
                throw (RuntimeException) exception;
            }
        }
        // rethrow original
        Helpers.throwException(exception);
    }

    // not synchronizing these methods, no harm if these values get set more
    // than once.
    // (also note that there's no chance of getting a partially initialized
    // class/module, as happens-before is guaranteed by volatile write/read
    // of constants table.)

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

    @Deprecated // no longer used
    public RubyClass getJavaObjectClass() {
        RubyClass clazz;
        if ((clazz = javaObjectClass) != null) return clazz;
        return javaObjectClass = getJavaModule().getClass("JavaObject");
    }

    public RubyClass getJavaProxyConstructorClass() {
        RubyClass clazz;
        if ((clazz = javaProxyConstructorClass) != null) return clazz;
        return javaProxyConstructorClass = getJavaModule().getClass("JavaProxyConstructor");
    }

    @Deprecated // no longer used
    public JavaClass getObjectJavaClass() {
        Object clazz;
        if ((clazz = objectJavaClass) != null) return (JavaClass) clazz;
        JavaClass javaClass = JavaClass.get(runtime, Object.class);
        objectJavaClass = javaClass;
        return javaClass;
    }

    @Deprecated
    public void setObjectJavaClass(JavaClass objectJavaClass) {
        // noop
    }

    @Deprecated
    public RubyClass getJavaArrayClass() {
        RubyClass clazz;
        if ((clazz = javaArrayClass) != null) return clazz;
        return javaArrayClass = getJavaModule().getClass("JavaArray");
    }

    @Deprecated
    public RubyClass getJavaClassClass() {
        RubyClass clazz;
        if ((clazz = javaClassClass) != null) return clazz;
        return javaClassClass = getJavaModule().getClass("JavaClass");
    }

    public RubyClass getJavaPackageClass() {
        return javaPackageClass;
    }

    public void setJavaPackageClass(RubyClass javaPackageClass) {
        this.javaPackageClass = javaPackageClass;
    }

    public RubyModule getJavaInterfaceTemplate() {
        RubyModule module;
        if ((module = javaInterfaceTemplate) != null) return module;
        return javaInterfaceTemplate = runtime.getModule("JavaInterfaceTemplate");
    }

    @Deprecated
    public RubyModule getPackageModuleTemplate() {
        return null; // no longer used + has been deprecated since ~ 9.1
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

    @Deprecated // not used
    public RubyClass getJavaFieldClass() {
        RubyClass clazz;
        if ((clazz = javaFieldClass) != null) return clazz;
        return javaFieldClass = getJavaModule().getClass("JavaField");
    }

    @Deprecated // not used
    public RubyClass getJavaMethodClass() {
        RubyClass clazz;
        if ((clazz = javaMethodClass) != null) return clazz;
        return javaMethodClass = getJavaModule().getClass("JavaMethod");
    }

    @Deprecated // not used
    public RubyClass getJavaConstructorClass() {
        RubyClass clazz;
        if ((clazz = javaConstructorClass) != null) return clazz;
        return javaConstructorClass = getJavaModule().getClass("JavaConstructor");
    }

    public Class<?> loadJavaClass(String className) throws ClassNotFoundException {
        return loadJavaClass(className, true);
    }

    public Class<?> loadJavaClass(String className, boolean initialize) throws ClassNotFoundException {
        Class<?> primitiveClass;
        if ((primitiveClass = JavaUtil.getPrimitiveClass(className)) == null) {
            if (!Ruby.isSecurityRestricted()) {
                for (Loader loader : runtime.getInstanceConfig().getExtraLoaders()) {
                    try {
                        return loader.loadClass(className);
                    } catch (ClassNotFoundException ignored) { /* continue */ }
                }
                return Class.forName(className, initialize, runtime.getJRubyClassLoader());
            }
            return Class.forName(className, initialize, JavaSupport.class.getClassLoader());
        }
        return primitiveClass;
    }

    public ObjectProxyCache<IRubyObject, RubyClass> getObjectProxyCache() {
        return objectProxyCache;
    }

    // Internal API

    abstract ClassValue<Map<String, AssignedName>> getStaticAssignedNames();

    abstract ClassValue<Map<String, AssignedName>> getInstanceAssignedNames();

    @Deprecated
    public abstract Map<String, JavaClass> getNameClassMap();

    @Deprecated // internal API - no longer used
    public abstract Map<Set<?>, JavaProxyClass> getJavaProxyClassCache();

    @Deprecated // internal API - no longer used (kept functional due deprecated JavaClass.get API)
    public JavaClass getJavaClassFromCache(Class clazz) {
        return javaClassCache.get(clazz);
    }

    final void beginProxy(Class clazz, RubyModule proxy) {
        UnfinishedProxy up = new UnfinishedProxy(proxy);
        up.lock();
        unfinishedProxies.put(clazz, up);
    }

    final void endProxy(Class clazz) {
        UnfinishedProxy up = unfinishedProxies.remove(clazz);
        up.unlock();
    }

    final RubyModule getUnfinishedProxy(Class clazz) {
        UnfinishedProxy up = unfinishedProxies.get(clazz);
        if (up != null && up.isHeldByCurrentThread()) return up.proxy;
        return null;
    }

    RubyModule getProxyClassFromCache(Class clazz) {
        return proxyClassCache.get(clazz);
    }

}
