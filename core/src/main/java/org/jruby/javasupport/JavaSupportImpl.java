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

import java.lang.reflect.Member;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.Unrescuable;
import org.jruby.runtime.Helpers;
import org.jruby.util.ArraySupport;
import org.jruby.util.collections.ClassValue;
import org.jruby.javasupport.binding.AssignedName;
import org.jruby.javasupport.proxy.JavaProxyClass;
import org.jruby.javasupport.util.ObjectProxyCache;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.WeakIdentityHashMap;

import static org.jruby.javasupport.Java.initCause;

public class JavaSupportImpl extends JavaSupport {

    private final ObjectProxyCache<IRubyObject,RubyClass> objectProxyCache =
            // TODO: specifying soft refs, may want to compare memory consumption,
            // behavior with weak refs (specify WEAK in place of SOFT below)
            new ObjectProxyCache<IRubyObject,RubyClass>(ObjectProxyCache.ReferenceType.WEAK) {

                public IRubyObject allocateProxy(Object javaObject, RubyClass clazz) {
                    return Java.allocateProxy(javaObject, clazz);
                }
            };

    private final ClassValue<Map<String, AssignedName>> staticAssignedNames;
    private final ClassValue<Map<String, AssignedName>> instanceAssignedNames;

    private RubyModule javaModule;
    private RubyModule javaUtilitiesModule;
    private RubyModule javaArrayUtilitiesModule;
    private RubyClass javaObjectClass;
    private JavaClass objectJavaClass;
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

    public JavaSupportImpl(final Ruby runtime) {
        super(runtime);

        this.staticAssignedNames = ClassValue.newInstance(klass -> new HashMap<>(8, 1));
        this.instanceAssignedNames = ClassValue.newInstance(klass -> new HashMap<>(8, 1));
    }

    @Override
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

    @Override
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
        if ( exception instanceof RaiseException ) {
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

    public ObjectProxyCache<IRubyObject,RubyClass> getObjectProxyCache() {
        return objectProxyCache;
    }

    // not synchronizing these methods, no harm if these values get set more
    // than once.
    // (also note that there's no chance of getting a partially initialized
    // class/module, as happens-before is guaranteed by volatile write/read
    // of constants table.)

    public Map<String, JavaClass> getNameClassMap() {
        return Collections.emptyMap();
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

    public RubyClass getJavaProxyConstructorClass() {
        RubyClass clazz;
        if ((clazz = javaProxyConstructorClass) != null) return clazz;
        return javaProxyConstructorClass = getJavaModule().getClass("JavaProxyConstructor");
    }

    @Deprecated // no longer used
    public JavaClass getObjectJavaClass() {
        JavaClass clazz;
        if ((clazz = objectJavaClass) != null) return clazz;
        return objectJavaClass = JavaClass.get(runtime, Object.class);
    }

    @Deprecated
    public void setObjectJavaClass(JavaClass objectJavaClass) {
        // noop
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

    public RubyClass getJavaPackageClass() {
        RubyClass clazz;
        if ((clazz = javaPackageClass) != null) return clazz;
        return javaPackageClass = getJavaModule().getClass("JavaPackage");
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

    @Override
    final protected JavaProxyClass fetchJavaProxyClass(ProxyClassKey classKey) {
        synchronized (javaProxyClasses) {
            return javaProxyClasses.get(classKey);
        }
    }

    @Override
    final protected JavaProxyClass saveJavaProxyClass(ProxyClassKey classKey, JavaProxyClass klass) {
        synchronized (javaProxyClasses) {
            JavaProxyClass existing = javaProxyClasses.get(classKey);
            if ( existing != null ) return existing;
            javaProxyClasses.put(classKey, klass);
        }
        return klass;
    }

    // internal helper to access non-public fetch method
    public static JavaProxyClass fetchJavaProxyClass(final Ruby runtime, ProxyClassKey classKey) {
        return runtime.getJavaSupport().fetchJavaProxyClass(classKey);
    }

    // internal helper to access non-public save method
    public static JavaProxyClass saveJavaProxyClass(final Ruby runtime, ProxyClassKey classKey, JavaProxyClass klass) {
        return runtime.getJavaSupport().saveJavaProxyClass(classKey, klass);
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
