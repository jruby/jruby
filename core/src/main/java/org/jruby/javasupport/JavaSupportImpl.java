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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jruby.Ruby;
import org.jruby.util.ArraySupport;
import org.jruby.util.WeakIdentityHashMap;
import org.jruby.javasupport.binding.AssignedName;
import org.jruby.javasupport.proxy.JavaProxyClass;

/**
 * Internal APIs of {@link JavaSupport}.
 */
public class JavaSupportImpl extends JavaSupport {

    private final java.lang.ClassValue<Map<String, AssignedName>> staticAssignedNames = new java.lang.ClassValue<Map<String, AssignedName>>() {
        public synchronized Map<String, AssignedName> computeValue(Class clazz) {
            return new HashMap<>(8, 1);
        }
    };
    private final ClassValue<Map<String, AssignedName>> instanceAssignedNames = new java.lang.ClassValue<Map<String, AssignedName>>() {
        public synchronized Map<String, AssignedName> computeValue(Class clazz) {
            return new HashMap<>(8, 1);
        }
    };

    public JavaSupportImpl(final Ruby runtime) {
        super(runtime);
    }

    @Deprecated
    public Map<String, JavaClass> getNameClassMap() {
        return Collections.emptyMap();
    }

    public ClassValue<Map<String, AssignedName>> getStaticAssignedNames() {
        return staticAssignedNames;
    }

    public ClassValue<Map<String, AssignedName>> getInstanceAssignedNames() {
        return instanceAssignedNames;
    }

    @Deprecated
    public Map<Set<?>, JavaProxyClass> getJavaProxyClassCache() {
        Map<Set<?>, JavaProxyClass> javaProxyClassCache = new HashMap<>(javaProxyClasses.size());
        synchronized (javaProxyClasses) {
            for ( Map.Entry<ProxyClassKey, JavaProxyClass> entry : javaProxyClasses.entrySet() ) {
                final ProxyClassKey key = entry.getKey();
                final Set<Object> cacheKey = new HashSet<>();
                cacheKey.add(key.superClass);
                for (int i = 0; i < key.interfaces.length; i++) {
                    cacheKey.add(key.interfaces[i]);
                }
                // add (potentially) overridden names to the key.
                if ( ! key.names.isEmpty() ) cacheKey.addAll(key.names);

                javaProxyClassCache.put(cacheKey, entry.getValue());
            }
        }

        return Collections.unmodifiableMap(javaProxyClassCache);
    }

    // cache of all JavaProxyClass objects created for this runtime
    private final Map<ProxyClassKey, JavaProxyClass> javaProxyClasses = new HashMap<>();

    /**
     * a replacement for {@link #getJavaProxyClassCache()} API
     */
    protected JavaProxyClass fetchJavaProxyClass(ProxyClassKey classKey) {
        synchronized (javaProxyClasses) {
            return javaProxyClasses.get(classKey);
        }
    }

    /**
     * a replacement for {@link #getJavaProxyClassCache()} API
     */
    protected JavaProxyClass saveJavaProxyClass(ProxyClassKey classKey, JavaProxyClass klass) {
        synchronized (javaProxyClasses) {
            JavaProxyClass existing = javaProxyClasses.get(classKey);
            if ( existing != null ) return existing;
            javaProxyClasses.put(classKey, klass);
        }
        return klass;
    }

    // internal helper to access non-public fetch method
    public static JavaProxyClass fetchJavaProxyClass(final Ruby runtime, ProxyClassKey classKey) {
        return ((JavaSupportImpl) runtime.getJavaSupport()).fetchJavaProxyClass(classKey);
    }

    // internal helper to access non-public save method
    public static JavaProxyClass saveJavaProxyClass(final Ruby runtime, ProxyClassKey classKey, JavaProxyClass klass) {
        return ((JavaSupportImpl) runtime.getJavaSupport()).saveJavaProxyClass(classKey, klass);
    }

    /**
     * <p>Note: Internal API - subject to change!</p>
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

    @Deprecated
    private volatile Map<Object, Object[]> javaObjectVariables;

    @Deprecated
    public Object getJavaObjectVariable(Object o, int i) {
        if (i == -1) return null;

        Map<Object, Object[]> variables = javaObjectVariables;
        if (variables == null) return null;

        synchronized (this) {
            Object[] vars = variables.get(o);
            if (vars == null || vars.length <= i) return null;
            return vars[i];
        }
    }

    @Deprecated
    public void setJavaObjectVariable(Object o, int i, Object v) {
        if (i == -1) return;

        synchronized (this) {
            Map<Object, Object[]> variables = javaObjectVariables;

            if (variables == null) {
                variables = javaObjectVariables = new WeakIdentityHashMap();
            }

            Object[] vars = variables.get(o);
            if (vars == null) {
                vars = new Object[i + 1];
                variables.put(o, vars);
            }
            else if (vars.length <= i) {
                vars = ArraySupport.newCopy(vars, i + 1);
                variables.put(o, vars);
            }
            vars[i] = v;
        }
    }

}
