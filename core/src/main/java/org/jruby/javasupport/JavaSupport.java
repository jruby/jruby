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
import org.jruby.javasupport.binding.AssignedName;
import org.jruby.javasupport.ext.JavaExtensions;
import org.jruby.javasupport.proxy.JavaProxyClass;
import org.jruby.javasupport.util.ObjectProxyCache;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.Loader;
import org.jruby.util.collections.ClassValue;
import org.jruby.util.collections.ClassValueCalculator;

import java.lang.reflect.Member;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public abstract class JavaSupport {

    protected final Ruby runtime;

    private final ClassValue<JavaClass> javaClassCache;
    private final ClassValue<RubyModule> proxyClassCache;

    static final class UnfinishedProxy extends ReentrantLock {
        final RubyModule proxy;
        UnfinishedProxy(RubyModule proxy) {
            this.proxy = proxy;
        }
    }

    private final Map<Class, UnfinishedProxy> unfinishedProxies;

    protected JavaSupport(final Ruby runtime) {
        this.runtime = runtime;

        this.javaClassCache = ClassValue.newInstance(klass -> new JavaClass(runtime, getJavaClassClass(), klass));

        this.proxyClassCache = ClassValue.newInstance(new ClassValueCalculator<RubyModule>() {
            /**
             * Because of the complexity of processing a given class and all its dependencies,
             * we opt to synchronize this logic. Creation of all proxies goes through here,
             * allowing us to skip some threading work downstream.
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

    @Deprecated
    public abstract Class loadJavaClassVerbose(String className);

    @Deprecated
    public abstract Class loadJavaClassQuiet(String className);

    public abstract void handleNativeException(Throwable exception, Member target);

    public abstract ObjectProxyCache<IRubyObject,RubyClass> getObjectProxyCache();

    @Deprecated
    public abstract Map<String, JavaClass> getNameClassMap();

    @Deprecated
    public abstract Object getJavaObjectVariable(Object o, int i);

    @Deprecated
    public abstract void setJavaObjectVariable(Object o, int i, Object v);

    public abstract RubyModule getJavaModule();

    public abstract RubyModule getJavaUtilitiesModule();

    public abstract RubyModule getJavaArrayUtilitiesModule();

    public abstract RubyClass getJavaObjectClass();

    @Deprecated
    public abstract JavaClass getObjectJavaClass();

    @Deprecated
    public abstract void setObjectJavaClass(JavaClass objectJavaClass);

    public abstract RubyClass getJavaArrayClass();

    public abstract RubyClass getJavaClassClass();

    public abstract RubyClass getJavaPackageClass();
    abstract void setJavaPackageClass(RubyClass javaPackageClass);

    public abstract RubyModule getJavaInterfaceTemplate();

    @Deprecated
    public abstract RubyModule getPackageModuleTemplate();

    public abstract RubyClass getJavaProxyClass();

    public abstract RubyClass getArrayJavaProxyCreatorClass();

    public abstract RubyClass getConcreteProxyClass();

    public abstract RubyClass getMapJavaProxyClass();

    public abstract RubyClass getArrayProxyClass();

    @Deprecated
    public abstract RubyClass getJavaFieldClass();

    @Deprecated
    public abstract RubyClass getJavaMethodClass();

    @Deprecated
    public abstract RubyClass getJavaConstructorClass();

    public abstract RubyClass getJavaProxyConstructorClass();

    public abstract ClassValue<Map<String, AssignedName>> getStaticAssignedNames();

    public abstract ClassValue<Map<String, AssignedName>> getInstanceAssignedNames();

    @Deprecated // internal API - no longer used
    public abstract Map<Set<?>, JavaProxyClass> getJavaProxyClassCache();

    /**
     * a replacement for {@link #getJavaProxyClassCache()} API
     */
    protected abstract JavaProxyClass fetchJavaProxyClass(ProxyClassKey classKey);

    /**
     * a replacement for {@link #getJavaProxyClassCache()} API
     */
    protected abstract JavaProxyClass saveJavaProxyClass(ProxyClassKey classKey, JavaProxyClass klass);

    /**
     * @note Internal API - subject to change!
     */
    public static final class ProxyClassKey {
        final Class superClass;
        final Class[] interfaces;
        final Set<String> names; // "usable" method names - assumed immutable

        private ProxyClassKey(Class superClass, Class[] interfaces, Set<String> names) {
            this.superClass = superClass;
            this.interfaces = interfaces;
            this.names = names;
        }

        public static ProxyClassKey getInstance(Class superClass, Class[] interfaces, Set<String> names) {
            return new ProxyClassKey(superClass, interfaces, names);
        }

        @Override
        public boolean equals(Object obj) {
            if ( obj instanceof ProxyClassKey ) {
                final ProxyClassKey that = (ProxyClassKey) obj;
                if (this.superClass != that.superClass) return false;

                if (this.names.size() != that.names.size()) return false;
                if ( ! this.names.equals(that.names) ) return false;

                final int len = this.interfaces.length;
                if (len != that.interfaces.length) return false;
                // order is not important :
                for ( int i = 0; i < len; i++ ) {
                    final Class iface = this.interfaces[i];
                    boolean ifaceFound = false;
                    for ( int j = 0; j < len; j++ ) {
                        if ( iface == that.interfaces[j] ) {
                            ifaceFound = true; break;
                        }
                    }
                    if ( ! ifaceFound ) return false;
                }
                return true;
            }
            return false;
        }

        private int hash;

        @Override
        public int hashCode() {
            int hash = this.hash;
            if (hash != 0) return hash;

            for ( int i = 0; i < interfaces.length; i++ ) {
                hash += interfaces[i].hashCode();
            }
            return this.hash = (hash * superClass.hashCode()) ^ this.names.hashCode();
        }
    }

    // Internal API

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

    /**
     * @deprecated Internal API - no longer used
     */
    public JavaClass getJavaClassFromCache(Class clazz) {
        return javaClassCache.get(clazz);
    }

}
