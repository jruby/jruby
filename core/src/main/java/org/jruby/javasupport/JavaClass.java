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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004-2005 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2004 David Corbin <dcorbin@users.sourceforge.net>
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2006 Kresten Krab Thorup <krab@gnu.org>
 * Copyright (C) 2007 Miguel Covarrubias <mlcovarrubias@gmail.com>
 * Copyright (C) 2007 William N Dortch <bill.dortch@gmail.com>
 * Copyright (C) 2011 David Pollak <feeder.of.the.bears@gmail.com>
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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ReflectPermission;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyInteger;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyClass;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.JavaMethod.JavaMethodZero;
import org.jruby.java.invokers.ConstructorInvoker;
import org.jruby.java.invokers.SingletonMethodInvoker;
import org.jruby.java.invokers.InstanceFieldGetter;
import org.jruby.java.invokers.InstanceFieldSetter;
import org.jruby.java.invokers.InstanceMethodInvoker;
import org.jruby.java.invokers.StaticFieldGetter;
import org.jruby.java.invokers.StaticFieldSetter;
import org.jruby.java.invokers.StaticMethodInvoker;
import org.jruby.java.proxies.ArrayJavaProxy;
import org.jruby.java.proxies.ConcreteJavaProxy;
import org.jruby.java.util.ArrayUtils;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import static org.jruby.runtime.Visibility.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.IdUtil;
import org.jruby.util.cli.Options;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

@JRubyClass(name="Java::JavaClass", parent="Java::JavaObject")
public class JavaClass extends JavaObject {

    private static final Logger LOG = LoggerFactory.getLogger("JavaClass");

    public static final String METHOD_MANGLE = "__method";
    public static final boolean DEBUG_SCALA = false;

    public static final boolean CAN_SET_ACCESSIBLE;

    static {
        boolean canSetAccessible = false;

        if (RubyInstanceConfig.CAN_SET_ACCESSIBLE) {
            try {
                AccessController.checkPermission(new ReflectPermission("suppressAccessChecks"));
                canSetAccessible = true;
            } catch (Throwable t) {
                // added this so if things are weird in the future we can debug without
                // spinning a new binary
                if (Options.JI_LOGCANSETACCESSIBLE.load()) {
                    t.printStackTrace();
                }

                // assume any exception means we can't suppress access checks
                canSetAccessible = false;
            }
        }

        CAN_SET_ACCESSIBLE = canSetAccessible;
    }

    private static void handleScalaSingletons(final Class<?> javaClass, final Initializer.State state) {
        // check for Scala companion object
        try {
            final ClassLoader loader = javaClass.getClassLoader();
            if ( loader == null ) return; //this is a core class, bail

            Class<?> companionClass = loader.loadClass(javaClass.getName() + '$');
            final Field field = companionClass.getField("MODULE$");
            final Object singleton = field.get(null);
            if ( singleton == null ) return;

            final Method[] scalaMethods = getMethods(companionClass);
            for ( int j = scalaMethods.length - 1; j >= 0; j-- ) {
                final Method method = scalaMethods[j];
                String name = method.getName();

                if (DEBUG_SCALA) LOG.debug("Companion object method {} for {}", name, companionClass);

                if ( name.indexOf('$') >= 0 ) name = fixScalaNames(name);

                if ( ! Modifier.isStatic( method.getModifiers() ) ) {
                    AssignedName assignedName = state.staticNames.get(name);
                    // For JRUBY-4505, restore __method methods for reserved names
                    if (INSTANCE_RESERVED_NAMES.containsKey(method.getName())) {
                        if (DEBUG_SCALA) LOG.debug("in reserved " + name);
                        setupSingletonMethods(state.staticInstallers, javaClass, singleton, method, name + METHOD_MANGLE);
                        continue;
                    }
                    if (assignedName == null) {
                        state.staticNames.put(name, new AssignedName(name, Priority.METHOD));
                        if (DEBUG_SCALA) LOG.debug("Assigned name is null");
                    } else {
                        if (Priority.METHOD.lessImportantThan(assignedName)) {
                            if (DEBUG_SCALA) LOG.debug("Less important");
                            continue;
                        }
                        if (!Priority.METHOD.asImportantAs(assignedName)) {
                            state.staticInstallers.remove(name);
                            state.staticInstallers.remove(name + '=');
                            state.staticNames.put(name, new AssignedName(name, Priority.METHOD));
                        }
                    }
                    if (DEBUG_SCALA) LOG.debug("Installing {} {} {}", name, method, singleton);
                    setupSingletonMethods(state.staticInstallers, javaClass, singleton, method, name);
                }
                else {
                    if (DEBUG_SCALA) LOG.debug("Method {} is sadly static", method);
                }
            }
        }
        catch (ClassNotFoundException e) { /* there's no companion object */ }
        catch (NoSuchFieldException e) { /* no MODULE$ field in companion */ }
        catch (Exception e) {
            if (DEBUG_SCALA) LOG.debug("Failed with {}", e);
        }
    }

    /**
     * Assigned names only override based priority of an assigned type, the type must be less than
     * or equal to the assigned type. For example, field name (FIELD) in a subclass will override
     * an alias (ALIAS) in a superclass, but not a method (METHOD).
     */
    private enum Priority {
        RESERVED(0), METHOD(1), FIELD(2), PROTECTED_METHOD(3),
        WEAKLY_RESERVED(4), ALIAS(5), PROTECTED_FIELD(6);

        private int value;

        Priority(int value) {
            this.value = value;
        }

        public boolean asImportantAs(AssignedName other) {
            return other != null && other.type.value == value;
        }

        public boolean lessImportantThan(AssignedName other) {
            return other != null && other.type.value < value;
        }

        public boolean moreImportantThan(AssignedName other) {
            return other == null || other.type.value > value;
        }
    }

    private static class AssignedName {
        String name;
        Priority type;

        AssignedName () {}
        AssignedName(String name, Priority type) {
            this.name = name;
            this.type = type;
        }
    }

    // TODO: other reserved names?
    private static final Map<String, AssignedName> RESERVED_NAMES = new HashMap<String, AssignedName>();
    static {
        RESERVED_NAMES.put("__id__", new AssignedName("__id__", Priority.RESERVED));
        RESERVED_NAMES.put("__send__", new AssignedName("__send__", Priority.RESERVED));
        // JRUBY-5132: java.awt.Component.instance_of?() expects 2 args
        RESERVED_NAMES.put("instance_of?", new AssignedName("instance_of?", Priority.RESERVED));
    }
    private static final Map<String, AssignedName> STATIC_RESERVED_NAMES = new HashMap<String, AssignedName>(RESERVED_NAMES);
    static {
        STATIC_RESERVED_NAMES.put("new", new AssignedName("new", Priority.RESERVED));
    }
    private static final Map<String, AssignedName> INSTANCE_RESERVED_NAMES = new HashMap<String, AssignedName>(RESERVED_NAMES);
    static {
        // only possible for "getClass" to be an instance method in Java
        INSTANCE_RESERVED_NAMES.put("class", new AssignedName("class", Priority.RESERVED));
        // "initialize" has meaning only for an instance (as opposed to a class)
        INSTANCE_RESERVED_NAMES.put("initialize", new AssignedName("initialize", Priority.RESERVED));
    }


    private static abstract class NamedInstaller {
        static final int STATIC_FIELD = 1;
        static final int STATIC_METHOD = 2;
        static final int INSTANCE_FIELD = 3;
        static final int INSTANCE_METHOD = 4;

        final String name;
        final int type;

        Visibility visibility = Visibility.PUBLIC;

        NamedInstaller(String name, int type) {
            this.name = name;
            this.type = type;
        }

        abstract void install(RubyModule proxy);

        // small hack to save a cast later on
        boolean hasLocalMethod() { return true; }

        boolean isPublic() { return visibility == Visibility.PUBLIC; }

        //boolean isProtected() { return visibility == Visibility.PROTECTED; }

    }

    private static abstract class FieldInstaller extends NamedInstaller {

        final Field field;

        FieldInstaller(String name, int type, Field field) {
            super(name,type);
            this.field = field;
        }
    }

    private static class StaticFieldGetterInstaller extends FieldInstaller {

        StaticFieldGetterInstaller(String name, Field field) {
            super(name, STATIC_FIELD, field);
        }

        @Override void install(final RubyModule proxy) {
            if ( Modifier.isPublic( field.getModifiers() ) ) {
                proxy.getSingletonClass().addMethod(name, new StaticFieldGetter(name, proxy, field));
            }
        }
    }

    private static class StaticFieldSetterInstaller extends FieldInstaller {

        StaticFieldSetterInstaller(String name, Field field) {
            super(name, STATIC_FIELD, field);
        }

        @Override void install(final RubyModule proxy) {
            if ( Modifier.isPublic( field.getModifiers() ) ) {
                proxy.getSingletonClass().addMethod(name, new StaticFieldSetter(name, proxy, field));
            }
        }
    }

    private static class InstanceFieldGetterInstaller extends FieldInstaller {

        InstanceFieldGetterInstaller(String name, Field field) {
            super(name, INSTANCE_FIELD, field);
        }

        @Override void install(final RubyModule proxy) {
            if ( Modifier.isPublic( field.getModifiers() ) ) {
                proxy.addMethod(name, new InstanceFieldGetter(name, proxy, field));
            }
        }
    }

    private static class InstanceFieldSetterInstaller extends FieldInstaller {

        InstanceFieldSetterInstaller(String name, Field field) {
            super(name, INSTANCE_FIELD, field);
        }

        @Override void install(final RubyModule proxy) {
            if ( Modifier.isPublic( field.getModifiers() ) ) {
                proxy.addMethod(name, new InstanceFieldSetter(name, proxy, field));
            }
        }
    }

    private static abstract class MethodInstaller extends NamedInstaller {

        protected final List<Method> methods = new ArrayList<Method>(4);
        protected List<String> aliases;
        private boolean localMethod;

        MethodInstaller(String name, int type) { super(name, type); }

        // called only by initializing thread; no synchronization required
        void addMethod(final Method method, final Class<?> clazz) {
            this.methods.add(method);
            localMethod |=
                clazz == method.getDeclaringClass() ||
                method.getDeclaringClass().isInterface();
        }

        // called only by initializing thread; no synchronization required
        void addAlias(final String alias) {
            List<String> aliases = this.aliases;
            if (aliases == null) {
                aliases = this.aliases = new ArrayList<String>(4);
            }
            if ( ! aliases.contains(alias) ) aliases.add(alias);
        }

        @Override
        boolean hasLocalMethod () { return localMethod; }

        void setLocalMethod(boolean flag) { localMethod = flag; }

    }

    private static class ConstructorInvokerInstaller extends MethodInstaller {

        protected final List<Constructor> constructors = new ArrayList<Constructor>(4);
        private boolean localConstructor;

        ConstructorInvokerInstaller(String name) { super(name, STATIC_METHOD); }

        // called only by initializing thread; no synchronization required
        void addConstructor(final Constructor ctor, final Class<?> clazz) {
            if ( ! Ruby.isSecurityRestricted() ) {
                try {
                    ctor.setAccessible(true);
                } catch(SecurityException e) {}
            }
            this.constructors.add(ctor);
            localConstructor |= clazz == ctor.getDeclaringClass();
        }

        @Override void install(final RubyModule proxy) {
            if ( localConstructor ) {
                proxy.addMethod(name, new ConstructorInvoker(proxy, constructors));
            }
            else { // if there's no constructor, we must prevent construction
                proxy.addMethod(name, new org.jruby.internal.runtime.methods.JavaMethod(proxy, PUBLIC) {
                    @Override
                    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
                        throw context.runtime.newTypeError("no public constructors for " + clazz);
                    }
                });
            }
        }
    }

    private static class StaticMethodInvokerInstaller extends MethodInstaller {

        StaticMethodInvokerInstaller(String name) { super(name, STATIC_METHOD); }

        @Override void install(final RubyModule proxy) {
            if ( hasLocalMethod() ) {
                final RubyClass singletonClass = proxy.getSingletonClass();
                DynamicMethod method = new StaticMethodInvoker(singletonClass, methods);
                singletonClass.addMethod(name, method);
                if ( aliases != null && isPublic() ) {
                    singletonClass.defineAliases(aliases, name);
                    //aliases = null;
                }
            }
        }
    }

    private static class SingletonMethodInvokerInstaller extends StaticMethodInvokerInstaller {

        final Object singleton;

        SingletonMethodInvokerInstaller(String name, Object singleton) {
            super(name);
            this.singleton = singleton;
        }

        @Override void install(final RubyModule proxy) {
            // we don't check haveLocalMethod() here because it's not local and we know
            // that we always want to go ahead and install it
            final RubyClass singletonClass = proxy.getSingletonClass();
            DynamicMethod method = new SingletonMethodInvoker(this.singleton, singletonClass, methods);
            singletonClass.addMethod(name, method);
            if ( aliases != null && isPublic() ) {
                singletonClass.defineAliases(aliases, name);
                //aliases = null;
            }
        }
    }

    private static class InstanceMethodInvokerInstaller extends MethodInstaller {

        InstanceMethodInvokerInstaller(String name) { super(name, INSTANCE_METHOD); }

        @Override void install(final RubyModule proxy) {
            if ( hasLocalMethod() ) {
                proxy.addMethod(name, new InstanceMethodInvoker(proxy, methods));
                if ( aliases != null && isPublic() ) {
                    proxy.defineAliases(aliases, this.name);
                    //aliases = null;
                }
            }
        }
    }

    private static class ConstantField {

        final Field field;

        ConstantField(Field field) { this.field = field; }

        void install(final RubyModule proxy) {
            final String name = field.getName();
            if ( proxy.getConstantAt(name) == null ) {
                try {
                    final Object value = field.get(null);
                    proxy.setConstant(name, JavaUtil.convertJavaToUsableRubyObject(proxy.getRuntime(), value));
                }
                catch (IllegalAccessException iae) {
                    // if we can't read it, we don't set it
                }
            }
        }

        private static final int CONSTANT = Modifier.FINAL | Modifier.PUBLIC | Modifier.STATIC;

        static boolean isConstant(final Field field) {
            return (field.getModifiers() & CONSTANT) == CONSTANT &&
                Character.isUpperCase( field.getName().charAt(0) );
        }

    }

    private volatile Map<String, AssignedName> staticAssignedNames;
    private volatile Map<String, AssignedName> instanceAssignedNames;

    // caching constructors, as they're accessed for each new instance
    private volatile RubyArray constructors;

    private volatile ArrayList<IRubyObject> proxyExtenders;

    // proxy module for interfaces
    private volatile RubyModule proxyModule;

    // proxy class for concrete classes.  also used for
    // "concrete" interfaces, which is why we have two fields
    private volatile RubyClass proxyClass;

    // readable only by thread building proxy, so don't need to be
    // volatile. used to handle recursive calls to getProxyClass/Module
    // while proxy is being constructed (usually when a constant
    // defined by a class is of the same type as that class).
    private RubyModule unfinishedProxyModule;
    private RubyClass unfinishedProxyClass;

    private final ReentrantLock proxyLock = new ReentrantLock();

    public RubyModule getProxyModule() {
        // allow proxy to be read without synchronization. if proxy
        // is under construction, only the building thread can see it.
        RubyModule proxy = proxyModule;
        if ( proxy != null ) return proxy; // proxy is complete, return it

        ReentrantLock lock = this.proxyLock;
        if ( lock != null && lock.isHeldByCurrentThread() ) {
            // proxy is under construction, building thread can
            // safely read non-volatile value
            return unfinishedProxyModule;
        }
        return null;
    }

    public RubyClass getProxyClass() {
        // allow proxy to be read without synchronization. if proxy
        // is under construction, only the building thread can see it.
        RubyClass proxy = proxyClass;
        if ( proxy != null ) return proxy; // proxy is complete, return it

        ReentrantLock lock = this.proxyLock;
        if ( lock != null && lock.isHeldByCurrentThread() ) {
            // proxy is under construction, building thread can
            // safely read non-volatile value
            return unfinishedProxyClass;
        }
        return null;
    }

    final void lockProxy() {
        proxyLock.lock();
    }

    final void unlockProxy() {
        proxyLock.unlock();
    }

    private void setProxyClass(final RubyClass proxyClass) {
        //assert this.proxyLock != null;
        this.proxyClass = proxyClass;
        //this.unfinishedProxyClass = null;
    }

    private void setProxyModule(final RubyModule proxyModule) {
        //assert this.proxyLock != null;
        this.proxyModule = proxyModule;
        //this.unfinishedProxyModule = null;
    }

    //private Map<String, AssignedName> getStaticAssignedNames() {
    //    return Collections.unmodifiableMap(staticAssignedNames);
    //}

    //private Map<String, AssignedName> getInstanceAssignedNames() {
    //    return Collections.unmodifiableMap(instanceAssignedNames);
    //}

    JavaClass(final Ruby runtime, final Class<?> javaClass) {
        super(runtime, runtime.getJavaSupport().getJavaClassClass(), javaClass);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof JavaClass &&
            this.getValue() == ((JavaClass) other).getValue();
    }

    @Override
    public int hashCode() {
        return javaClass().hashCode();
    }

    private static abstract class Initializer {
        protected final Ruby runtime;
        protected final Class javaClass;

        public Initializer(Ruby runtime, Class javaClass) {
            this.runtime = runtime;
            this.javaClass = javaClass;
        }

        public abstract void initialize(JavaClass javaClassObject, RubyModule proxy);

        public void initializeBase(RubyModule proxy) {
            proxy.addMethod("__jsend!", new org.jruby.internal.runtime.methods.JavaMethod.JavaMethodNBlock(proxy, PUBLIC) {
                @Override
                public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
                    String callName = args[0].asJavaString();

                    DynamicMethod method = self.getMetaClass().searchMethod(callName);
                    int v = method.getArity().getValue();

                    IRubyObject[] newArgs = new IRubyObject[args.length - 1];
                    System.arraycopy(args, 1, newArgs, 0, newArgs.length);

                    if(v < 0 || v == (newArgs.length)) {
                        return Helpers.invoke(context, self, callName, newArgs, Block.NULL_BLOCK);
                    } else {
                        RubyClass superClass = self.getMetaClass().getSuperClass();
                        return Helpers.invokeAs(context, superClass, self, callName, newArgs, Block.NULL_BLOCK);
                    }
                }
            });
        }

        public static class State {

            final Map<String, AssignedName> staticNames;
            final Map<String, AssignedName> instanceNames;
            final Map<String, NamedInstaller> staticInstallers = new HashMap<String, NamedInstaller>();
            final Map<String, NamedInstaller> instanceInstallers = new HashMap<String, NamedInstaller>();
            final List<ConstantField> constantFields = new ArrayList<ConstantField>();

            ConstructorInvokerInstaller constructorInstaller;

            State(final Ruby runtime, final Class superClass) {
                if (superClass == null) {
                    staticNames = new HashMap<String, AssignedName>();
                    instanceNames = new HashMap<String, AssignedName>();
                } else {
                    JavaClass superJavaClass = get(runtime,superClass);
                    staticNames = new HashMap<String, AssignedName>(superJavaClass.staticAssignedNames);
                    instanceNames = new HashMap<String, AssignedName>(superJavaClass.instanceAssignedNames);
                }
                staticNames.putAll(STATIC_RESERVED_NAMES);
                instanceNames.putAll(INSTANCE_RESERVED_NAMES);
            }

        }

    }

    private static class InterfaceInitializer extends Initializer {

        InterfaceInitializer(Ruby runtime, Class<?> javaClass) {
            super(runtime, javaClass);
        }

        public void initialize(JavaClass javaClassObject, RubyModule proxy) {
            final State state = new State(runtime, null);

            super.initializeBase(proxy);

            Field[] fields = getDeclaredFields(javaClass);

            for (int i = fields.length; --i >= 0; ) {
                final Field field = fields[i];
                if ( javaClass != field.getDeclaringClass() ) continue;

                if ( ConstantField.isConstant(field) ) {
                    state.constantFields.add(new ConstantField(field));
                }

                final int mod = field.getModifiers();
                if ( Modifier.isStatic(mod) ) {
                    addField(state.staticInstallers, state.staticNames, field, Modifier.isFinal(mod), true);
                }
            }

            setupInterfaceMethods(javaClass, state);

            // Add in any Scala singleton methods
            handleScalaSingletons(javaClass, state);

            // Now add all aliases for the static methods (fields) as appropriate
            for (Map.Entry<String, NamedInstaller> entry : state.staticInstallers.entrySet()) {
                final NamedInstaller installer = entry.getValue();
                if (installer.type == NamedInstaller.STATIC_METHOD && installer.hasLocalMethod()) {
                    assignAliases((MethodInstaller) installer, state.staticNames);
                }
            }

            javaClassObject.staticAssignedNames = Collections.unmodifiableMap(state.staticNames);
            javaClassObject.instanceAssignedNames = Collections.emptyMap();

            javaClassObject.unfinishedProxyModule = proxy;

            // flag the class as a Java class proxy.
            proxy.setJavaProxy(true);
            proxy.getSingletonClass().setJavaProxy(true);

            installClassFields(proxy, state);
            installClassStaticMethods(proxy, state);
            installClassClasses(javaClass, proxy);

            javaClassObject.setProxyModule(proxy); // this.proxyModule = proxy

            javaClassObject.applyProxyExtenders();
        }

    }

    private static class ClassInitializer extends Initializer {
        ClassInitializer(Ruby runtime, Class<?> javaClass) {
            super(runtime, javaClass);
        }

        public void initialize(JavaClass javaClassObject, RubyModule proxy) {
            RubyClass proxyClass = (RubyClass)proxy;
            Class<?> superclass = javaClass.getSuperclass();

            final State state = new State(runtime, superclass);

            super.initializeBase(proxy);

            if ( javaClass.isArray() || javaClass.isPrimitive() ) {
                // see note below re: 2-field kludge
                javaClassObject.setProxyClass(proxyClass);
                javaClassObject.setProxyModule(proxy);
                return;
            }

            setupClassFields(javaClass, state);
            setupClassMethods(javaClass, state);
            setupClassConstructors(javaClass, state);

            javaClassObject.staticAssignedNames = Collections.unmodifiableMap(state.staticNames);
            javaClassObject.instanceAssignedNames = Collections.unmodifiableMap(state.instanceNames);

            proxyClass.setReifiedClass(javaClass);

            assert javaClassObject.proxyClass == null;
            javaClassObject.unfinishedProxyClass = proxyClass;

            // flag the class as a Java class proxy.
            proxy.setJavaProxy(true);
            proxy.getSingletonClass().setJavaProxy(true);

            // set parent to either package module or outer class
            final RubyModule parent;
            final Class<?> enclosingClass = javaClass.getEnclosingClass();
            if ( enclosingClass != null ) {
                parent = Java.getProxyClass(runtime, enclosingClass);
            } else {
                parent = Java.getJavaPackageModule(runtime, javaClass.getPackage());
            }
            proxy.setParent(parent);

            // set the Java class name and package
            if ( javaClass.isAnonymousClass() ) {
                String baseName = ""; // javaClass.getSimpleName() returns "" for anonymous
                if ( enclosingClass != null ) {
                    // instead of an empty name anonymous classes will have a "conforming"
                    // although not valid (by Ruby semantics) RubyClass name e.g. :
                    // 'Java::JavaUtilConcurrent::TimeUnit::1' for $1 anonymous enum class
                    // NOTE: if this turns out suitable shall do the same for method etc.
                    final String className = javaClass.getName();
                    final int length = className.length();
                    final int offset = enclosingClass.getName().length();
                    if ( length > offset && className.charAt(offset) != '$' ) {
                        baseName = className.substring( offset );
                    }
                    else if ( length > offset + 1 ) { // skip '$'
                        baseName = className.substring( offset + 1 );
                    }
                }
                proxy.setBaseName( baseName );
            }
            else {
                proxy.setBaseName( javaClass.getSimpleName() );
            }

            installClassFields(proxyClass, state);
            installClassInstanceMethods(proxyClass, state);
            installClassConstructors(proxyClass, state);
            installClassClasses(javaClass, proxyClass);

            // FIXME: bit of a kludge here (non-interface classes assigned to both
            // class and module fields). simplifies proxy extender code, will go away
            // when JI is overhauled (and proxy extenders are deprecated).
            javaClassObject.setProxyClass(proxyClass);
            javaClassObject.setProxyModule(proxy);

            javaClassObject.applyProxyExtenders();

            // TODO: we can probably release our references to the constantFields
            // array and static/instance callback hashes at this point.
        }

        private void setupClassMethods(Class<?> javaClass, Initializer.State state) {
            // TODO: protected methods.  this is going to require a rework of some of the mechanism.
            Method[] methods = getMethods(javaClass);

            for (int i = methods.length; --i >= 0;) {
                // we need to collect all methods, though we'll only
                // install the ones that are named in this class
                Method method = methods[i];
                String name = method.getName();

                if (Modifier.isStatic(method.getModifiers())) {
                    prepareStaticMethod(javaClass, state, method, name);
                } else {
                    prepareInstanceMethod(javaClass, state, method, name);
                }
            }

            // try to wire up Scala singleton logic if present
            handleScalaSingletons(javaClass, state);

            // now iterate over all installers and make sure they also have appropriate aliases
            assignStaticAliases(state);
            assignInstanceAliases(state);
        }

        private void setupClassConstructors(final Class<?> javaClass, final Initializer.State state) {
            // TODO: protected methods.  this is going to require a rework
            // of some of the mechanism.
            final Constructor[] constructors = getConstructors(javaClass);

            // create constructorInstaller; if there are no constructors, it will disable construction
            ConstructorInvokerInstaller constructorInstaller = new ConstructorInvokerInstaller("__jcreate!");

            for ( int i = constructors.length; --i >= 0; ) {
                // we need to collect all methods, though we'll only
                // install the ones that are named in this class
                constructorInstaller.addConstructor(constructors[i], javaClass);
            }

            state.constructorInstaller = constructorInstaller;
        }

        private void prepareInstanceMethod(Class<?> javaClass, Initializer.State state, Method method, String name) {
            AssignedName assignedName = state.instanceNames.get(name);

            // For JRUBY-4505, restore __method methods for reserved names
            if (INSTANCE_RESERVED_NAMES.containsKey(method.getName())) {
                setupInstanceMethods(state.instanceInstallers, javaClass, method, name + METHOD_MANGLE);
                return;
            }

            if (assignedName == null) {
                state.instanceNames.put(name, new AssignedName(name, Priority.METHOD));
            } else {
                if (Priority.METHOD.lessImportantThan(assignedName)) return;
                if (!Priority.METHOD.asImportantAs(assignedName)) {
                    state.instanceInstallers.remove(name);
                    state.instanceInstallers.remove(name + '=');
                    state.instanceNames.put(name, new AssignedName(name, Priority.METHOD));
                }
            }
            setupInstanceMethods(state.instanceInstallers, javaClass, method, name);
        }

        private static void assignInstanceAliases(Initializer.State state) {
            for (Map.Entry<String, NamedInstaller> entry : state.instanceInstallers.entrySet()) {
                if (entry.getValue().type == NamedInstaller.INSTANCE_METHOD) {
                    MethodInstaller methodInstaller = (MethodInstaller)entry.getValue();

                    // no aliases for __method methods
                    if (entry.getKey().endsWith("__method")) continue;

                    if (methodInstaller.hasLocalMethod()) {
                        assignAliases(methodInstaller, state.instanceNames);
                    }

                    // JRUBY-6967: Types with java.lang.Comparable were using Ruby Comparable#== instead of dispatching directly to
                    // java.lang.Object.equals. We force an alias here to ensure we always use equals.
                    if (entry.getKey().equals("equals")) {
                        // we only install "local" methods, but need to force this so it will bind
                        methodInstaller.setLocalMethod(true);
                        methodInstaller.addAlias("==");
                    }
                }
            }
        }

        private static void installClassConstructors(final RubyModule proxy, final Initializer.State state) {
            if ( state.constructorInstaller != null ) state.constructorInstaller.install(proxy);
        }

    }

    void setupProxyClass(final RubyClass proxy) {
        assert proxyLock.isHeldByCurrentThread();

        setJavaClassFor(proxy);

        final Class<?> javaClass = javaClass();

        new ClassInitializer(getRuntime(), javaClass).initialize(this, proxy);
    }

    private static void assignAliases(MethodInstaller installer, Map<String, AssignedName> assignedNames) {
        String name = installer.name;
        String rubyCasedName = JavaUtil.getRubyCasedName(name);
        addUnassignedAlias(rubyCasedName,assignedNames,installer);

        String javaPropertyName = JavaUtil.getJavaPropertyName(name);
        String rubyPropertyName = null;

        for (Method method: installer.methods) {
            Class<?>[] argTypes = method.getParameterTypes();
            Class<?> resultType = method.getReturnType();
            int argCount = argTypes.length;

            // Add scala aliases for apply/update to roughly equivalent Ruby names
            if (rubyCasedName.equals("apply")) {
                addUnassignedAlias("[]", assignedNames, installer);
            }
            if (rubyCasedName.equals("update") && argCount == 2) {
                addUnassignedAlias("[]=", assignedNames, installer);
            }

            // Scala aliases for $ method names
            if (name.startsWith("$")) {
                addUnassignedAlias(fixScalaNames(name), assignedNames, installer);
            }

            // Add property name aliases
            if (javaPropertyName != null) {
                if (rubyCasedName.startsWith("get_")) {
                    rubyPropertyName = rubyCasedName.substring(4);
                    if (argCount == 0 ||                                // getFoo      => foo
                        argCount == 1 && argTypes[0] == int.class) {    // getFoo(int) => foo(int)

                        addUnassignedAlias(javaPropertyName,assignedNames,installer);
                        addUnassignedAlias(rubyPropertyName,assignedNames,installer);
                    }
                } else if (rubyCasedName.startsWith("set_")) {
                    rubyPropertyName = rubyCasedName.substring(4);
                    if (argCount == 1 && resultType == void.class) {    // setFoo(Foo) => foo=(Foo)
                        addUnassignedAlias(javaPropertyName+'=',assignedNames,installer);
                        addUnassignedAlias(rubyPropertyName+'=',assignedNames,installer);
                    }
                } else if (rubyCasedName.startsWith("is_")) {
                    rubyPropertyName = rubyCasedName.substring(3);
                    if (resultType == boolean.class) {                  // isFoo() => foo, isFoo(*) => foo(*)
                        addUnassignedAlias(javaPropertyName,assignedNames,installer);
                        addUnassignedAlias(rubyPropertyName,assignedNames,installer);
                    }
                }
            }

            // Additionally add ?-postfixed aliases to any boolean methods and properties.
            if (resultType == boolean.class) {
                // is_something?, contains_thing?
                addUnassignedAlias(rubyCasedName+'?',assignedNames,installer);
                if (rubyPropertyName != null) {
                    // something?
                    addUnassignedAlias(rubyPropertyName+'?',assignedNames,installer);
                }
            }
        }
    }

    private static void addUnassignedAlias(String name, Map<String, AssignedName> assignedNames,
            MethodInstaller installer) {
        if (name == null) return;

        AssignedName assignedName = assignedNames.get(name);
        // TODO: missing additional logic for dealing with conflicting protected fields.
        if (Priority.ALIAS.moreImportantThan(assignedName)) {
            installer.addAlias(name);
            assignedNames.put(name, new AssignedName(name, Priority.ALIAS));
        } else if (Priority.ALIAS.asImportantAs(assignedName)) {
            installer.addAlias(name);
        }
    }

    private static void installClassClasses(final Class<?> javaClass, final RubyModule proxy) {
        // setup constants for public inner classes
        Class<?>[] classes = getDeclaredClasses(javaClass);

        final Ruby runtime = proxy.getRuntime();

        for ( int i = classes.length; --i >= 0; ) {
            final Class<?> clazz = classes[i];
            if ( javaClass != clazz.getDeclaringClass() ) continue;

            // no non-public inner classes
            if ( ! Modifier.isPublic(clazz.getModifiers()) ) continue;

            final String simpleName = getSimpleName(clazz);
            if ( simpleName.length() == 0 ) continue;

            final RubyModule innerProxy = Java.getProxyClass(runtime, get(runtime, clazz));

            if ( IdUtil.isConstant(simpleName) ) {
                if (proxy.getConstantAt(simpleName) == null) {
                    proxy.const_set(runtime.newString(simpleName), innerProxy);
                }
            }
            else { // lower-case name
                if ( ! proxy.respondsTo(simpleName) ) {
                    // define a class method
                    proxy.getSingletonClass().addMethod(simpleName, new JavaMethodZero(proxy.getSingletonClass(), Visibility.PUBLIC) {
                        @Override
                        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
                            return innerProxy;
                        }
                    });
                }
            }
        }
    }

    private static void installClassFields(final RubyModule proxy, final Initializer.State state) {
        //assert state.constantFields != null;
        for (ConstantField field : state.constantFields) {
            field.install(proxy);
        }
    }

    private static void installClassInstanceMethods(final RubyClass proxy, final Initializer.State state) {
        installClassStaticMethods(proxy, state);
        //assert state.instanceInstallers != null;
        for ( Map.Entry<String, NamedInstaller> entry : state.instanceInstallers.entrySet() ) {
            entry.getValue().install(proxy);
        }
    }

    private static void installClassStaticMethods(final RubyModule proxy, final Initializer.State state) {
        //assert state.staticInstallers != null;
        for ( Map.Entry<String, NamedInstaller> entry : state.staticInstallers.entrySet() ) {
            entry.getValue().install(proxy);
        }
    }

    private static void addField(
            final Map <String, NamedInstaller> callbacks,
            final Map<String, AssignedName> names,
            final Field field,
            final boolean isFinal,
            final boolean isStatic) {

        final String name = field.getName();

        if ( Priority.FIELD.lessImportantThan( names.get(name) ) ) return;

        names.put(name, new AssignedName(name, Priority.FIELD));
        callbacks.put(name, isStatic ? new StaticFieldGetterInstaller(name, field) :
            new InstanceFieldGetterInstaller(name, field));

        if (!isFinal) {
            String setName = name + '=';
            callbacks.put(setName, isStatic ? new StaticFieldSetterInstaller(setName, field) :
                new InstanceFieldSetterInstaller(setName, field));
        }
    }

    private static void setupClassFields(Class<?> javaClass, Initializer.State state) {
        Field[] fields = getFields(javaClass);

        for (int i = fields.length; --i >= 0;) {
            Field field = fields[i];
            if (javaClass != field.getDeclaringClass()) continue;

            if (ConstantField.isConstant(field)) {
                state.constantFields.add(new ConstantField(field));
                continue;
            }

            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers)) {
                addField(state.staticInstallers, state.staticNames, field, Modifier.isFinal(modifiers), true);
            } else {
                addField(state.instanceInstallers, state.instanceNames, field, Modifier.isFinal(modifiers), false);
            }
        }
    }

    private static String fixScalaNames(final String name) {
        String s = name;
        for (Map.Entry<String, String> entry : SCALA_OPERATORS.entrySet()) {
            s = s.replaceAll(entry.getKey(), entry.getValue());
        }
        return s;
    }

    private static final Map<String, String> SCALA_OPERATORS;
    static {
        HashMap<String, String> scalaOperators = new HashMap<String, String>();
        scalaOperators.put("\\$plus", "+");
        scalaOperators.put("\\$minus", "-");
        scalaOperators.put("\\$colon", ":");
        scalaOperators.put("\\$div", "/");
        scalaOperators.put("\\$eq", "=");
        scalaOperators.put("\\$less", "<");
        scalaOperators.put("\\$greater", ">");
        scalaOperators.put("\\$bslash", "\\\\");
        scalaOperators.put("\\$hash", "#");
        scalaOperators.put("\\$times", "*");
        scalaOperators.put("\\$bang", "!");
        scalaOperators.put("\\$at", "@");
        scalaOperators.put("\\$percent", "%");
        scalaOperators.put("\\$up", "^");
        scalaOperators.put("\\$amp", "&");
        scalaOperators.put("\\$tilde", "~");
        scalaOperators.put("\\$qmark", "?");
        scalaOperators.put("\\$bar", "|");
        SCALA_OPERATORS = Collections.unmodifiableMap(scalaOperators);
    }

    private static void setupInterfaceMethods(Class<?> javaClass, Initializer.State state) {
        // TODO: protected methods.  this is going to require a rework of some of the mechanism.
        Method[] methods = getMethods(javaClass);

        for (int i = methods.length; --i >= 0;) {
            // Java 8 introduced static methods on interfaces, so we just look for those
            Method method = methods[i];
            String name = method.getName();

            if (!Modifier.isStatic(method.getModifiers())) continue;

            prepareStaticMethod(javaClass, state, method, name);
        }

        // now iterate over all installers and make sure they also have appropriate aliases
        assignStaticAliases(state);
    }

    private static void assignStaticAliases(final Initializer.State state) {
        for (Map.Entry<String, NamedInstaller> entry : state.staticInstallers.entrySet()) {
            // no aliases for __method methods
            if (entry.getKey().endsWith("__method")) continue;

            if (entry.getValue().type == NamedInstaller.STATIC_METHOD && entry.getValue().hasLocalMethod()) {
                assignAliases((MethodInstaller) entry.getValue(), state.staticNames);
            }
        }
    }

    private static void prepareStaticMethod(Class<?> javaClass, Initializer.State state, Method method, String name) {
        AssignedName assignedName = state.staticNames.get(name);

        // For JRUBY-4505, restore __method methods for reserved names
        if (STATIC_RESERVED_NAMES.containsKey(method.getName())) {
            setupStaticMethods(state.staticInstallers, javaClass, method, name + METHOD_MANGLE);
            return;
        }

        if (assignedName == null) {
            state.staticNames.put(name, new AssignedName(name, Priority.METHOD));
        } else {
            if (Priority.METHOD.lessImportantThan(assignedName)) return;
            if (!Priority.METHOD.asImportantAs(assignedName)) {
                state.staticInstallers.remove(name);
                state.staticInstallers.remove(name + '=');
                state.staticNames.put(name, new AssignedName(name, Priority.METHOD));
            }
        }
        setupStaticMethods(state.staticInstallers, javaClass, method, name);
    }

    private static void setupInstanceMethods(Map<String, NamedInstaller> methodCallbacks, Class<?> javaClass, Method method, String name) {
        MethodInstaller invoker = (MethodInstaller) methodCallbacks.get(name);
        if (invoker == null) {
            invoker = new InstanceMethodInvokerInstaller(name);
            methodCallbacks.put(name, invoker);
        }
        invoker.addMethod(method, javaClass);
    }

    private static void setupStaticMethods(Map<String, NamedInstaller> methodCallbacks, Class<?> javaClass, Method method, String name) {
        MethodInstaller invoker = (MethodInstaller) methodCallbacks.get(name);
        if (invoker == null) {
            invoker = new StaticMethodInvokerInstaller(name);
            methodCallbacks.put(name, invoker);
        }
        invoker.addMethod(method, javaClass);
    }

    private static void setupSingletonMethods(Map<String, NamedInstaller> methodCallbacks, Class<?> javaClass, Object singleton, Method method, String name) {
        MethodInstaller invoker = (MethodInstaller) methodCallbacks.get(name);
        if (invoker == null) {
            invoker = new SingletonMethodInvokerInstaller(name, singleton);
            methodCallbacks.put(name, invoker);
        }
        invoker.addMethod(method, javaClass);
    }

    void setupProxyModule(final RubyModule proxy) {
        assert proxyLock.isHeldByCurrentThread();
        assert this.proxyModule == null;

        setJavaClassFor(proxy);

        final Class<?> javaClass = javaClass();
        assert javaClass.isInterface();

        new InterfaceInitializer(getRuntime(), javaClass).initialize(this, proxy);
    }

    private void setJavaClassFor(final RubyModule proxy) {
        proxy.setInstanceVariable("@java_class", this);
    }

    public void addProxyExtender(final IRubyObject extender) {
        if ( ! extender.respondsTo("extend_proxy") ) {
            throw getRuntime().newTypeError("proxy extender must have an extend_proxy method");
        }
        lockProxy();
        try {
            if ( proxyModule == null ) {
                if (proxyExtenders == null) {
                    proxyExtenders = new ArrayList<IRubyObject>();
                }
                proxyExtenders.add(extender);
            }
            else {
                final Ruby runtime = getRuntime();
                runtime.getWarnings().warn(ID.PROXY_EXTENDED_LATE, " proxy extender added after proxy class created for " + this);
                extendProxy(runtime.getCurrentContext(), extender);
            }
        }
        finally { unlockProxy(); }
    }

    private void applyProxyExtenders() {
        final ArrayList<IRubyObject> extenders = proxyExtenders;
        if ( extenders != null ) {
            final ThreadContext context = getRuntime().getCurrentContext();
            for (IRubyObject extender : extenders) {
                extendProxy(context, extender);
            }
            proxyExtenders = null;
        }
    }

    private void extendProxy(final ThreadContext context, final IRubyObject extender) {
        extender.callMethod(context, "extend_proxy", proxyModule);
    }

    @JRubyMethod(required = 1)
    public IRubyObject extend_proxy(final ThreadContext context, IRubyObject extender) {
        addProxyExtender(extender);
        return getRuntime().getNil();
    }

    public static JavaClass get(final Ruby runtime, final Class<?> klass) {
        return runtime.getJavaSupport().getJavaClassFromCache(klass);
    }

    @Deprecated // only been used package internally - a bit poorly named
    public static RubyArray getRubyArray(Ruby runtime, Class<?>[] classes) {
        return toRubyArray(runtime, classes);
    }

    static RubyArray toRubyArray(final Ruby runtime, final Class<?>[] classes) {
        IRubyObject[] javaClasses = new IRubyObject[classes.length];
        for ( int i = classes.length; --i >= 0; ) {
            javaClasses[i] = get(runtime, classes[i]);
        }
        return RubyArray.newArrayNoCopy(runtime, javaClasses);
    }

    public static RubyClass createJavaClassClass(Ruby runtime, RubyModule javaModule) {
        // FIXME: Determine if a real allocator is needed here. Do people want to extend
        // JavaClass? Do we want them to do that? Can you Class.new(JavaClass)? Should
        // you be able to?
        // TODO: NOT_ALLOCATABLE_ALLOCATOR is probably ok here, since we don't intend for people to monkey with
        // this type and it can't be marshalled. Confirm. JRUBY-415
        RubyClass result = javaModule.defineClassUnder("JavaClass", javaModule.getClass("JavaObject"), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);

        result.includeModule(runtime.getModule("Comparable"));

        result.defineAnnotatedMethods(JavaClass.class);

        result.getMetaClass().undefineMethod("new");
        result.getMetaClass().undefineMethod("allocate");

        return result;
    }

    public Class javaClass() {
        return (Class) getValue();
    }

    public static Class<?> getJavaClass(final ThreadContext context, final RubyModule proxy) {
        final IRubyObject javaClass = Helpers.invoke(context, proxy, "java_class");
        return ((JavaClass) javaClass).javaClass();
    }

    public static Class<?> getJavaClassIfProxy(final ThreadContext context, final RubyModule proxy) {
        final IRubyObject javaClass;
        try {
            javaClass = Helpers.invoke(context, proxy, "java_class");
        }
        catch (RuntimeException e) {
            // clear $! since our "java_class" invoke above may have failed and set it
            context.setErrorInfo(context.nil);
            return null;
        }
        return ( javaClass instanceof JavaClass ) ? ((JavaClass) javaClass).javaClass() : null;
    }

    private static final Map<String, Class> PRIMITIVE_TO_CLASS = new HashMap<String,Class>();

    static {
        PRIMITIVE_TO_CLASS.put("byte", byte.class);
        PRIMITIVE_TO_CLASS.put("boolean", boolean.class);
        PRIMITIVE_TO_CLASS.put("short", short.class);
        PRIMITIVE_TO_CLASS.put("char", char.class);
        PRIMITIVE_TO_CLASS.put("int", int.class);
        PRIMITIVE_TO_CLASS.put("long", long.class);
        PRIMITIVE_TO_CLASS.put("float", float.class);
        PRIMITIVE_TO_CLASS.put("double", double.class);
    }

    static boolean isPrimitiveName(final String name) {
        return PRIMITIVE_TO_CLASS.containsKey(name);
    }

    public static synchronized JavaClass forNameVerbose(Ruby runtime, String className) {
        Class <?> klass = null;
        if (className.indexOf('.') == -1 && Character.isLowerCase(className.charAt(0))) {
            // one word type name that starts lower-case...it may be a primitive type
            klass = PRIMITIVE_TO_CLASS.get(className);
        }

        if (klass == null) {
            klass = runtime.getJavaSupport().loadJavaClassVerbose(className);
        }
        return JavaClass.get(runtime, klass);
    }

    public static synchronized JavaClass forNameQuiet(Ruby runtime, String className) {
        Class klass = runtime.getJavaSupport().loadJavaClassQuiet(className);
        return JavaClass.get(runtime, klass);
    }

    @JRubyMethod(name = "for_name", required = 1, meta = true)
    public static JavaClass for_name(IRubyObject recv, IRubyObject name) {
        return forNameVerbose(recv.getRuntime(), name.asJavaString());
    }

    @JRubyMethod
    public RubyModule ruby_class() {
        // Java.getProxyClass deals with sync issues, so we won't duplicate the logic here
        return Java.getProxyClass(getRuntime(), javaClass());
    }

    @JRubyMethod(name = "public?")
    public RubyBoolean public_p() {
        return getRuntime().newBoolean(Modifier.isPublic(javaClass().getModifiers()));
    }

    @JRubyMethod(name = "protected?")
    public RubyBoolean protected_p() {
        return getRuntime().newBoolean(Modifier.isProtected(javaClass().getModifiers()));
    }

    @JRubyMethod(name = "private?")
    public RubyBoolean private_p() {
        return getRuntime().newBoolean(Modifier.isPrivate(javaClass().getModifiers()));
    }

    @JRubyMethod(name = "final?")
    public RubyBoolean final_p() {
        return getRuntime().newBoolean(Modifier.isFinal(javaClass().getModifiers()));
    }

    @JRubyMethod(name = "interface?")
    public RubyBoolean interface_p() {
        return getRuntime().newBoolean(javaClass().isInterface());
    }

    @JRubyMethod(name = "array?")
    public RubyBoolean array_p() {
        return getRuntime().newBoolean(javaClass().isArray());
    }

    @JRubyMethod(name = "enum?")
    public RubyBoolean enum_p() {
        return getRuntime().newBoolean(javaClass().isEnum());
    }

    @JRubyMethod(name = "annotation?")
    public RubyBoolean annotation_p() {
        return getRuntime().newBoolean(javaClass().isAnnotation());
    }

    @JRubyMethod(name = "anonymous_class?")
    public RubyBoolean anonymous_class_p() {
        return getRuntime().newBoolean(javaClass().isAnonymousClass());
    }

    @JRubyMethod(name = "local_class?")
    public RubyBoolean local_class_p() {
        return getRuntime().newBoolean(javaClass().isLocalClass());
    }

    @JRubyMethod(name = "member_class?")
    public RubyBoolean member_class_p() {
        return getRuntime().newBoolean(javaClass().isMemberClass());
    }

    @JRubyMethod(name = "synthetic?")
    public IRubyObject synthetic_p() {
        return getRuntime().newBoolean(javaClass().isSynthetic());
    }

    @JRubyMethod(name = {"name", "to_s"})
    public RubyString name() {
        return getRuntime().newString(javaClass().getName());
    }

    @JRubyMethod
    @Override
    public RubyString inspect() {
        return getRuntime().newString("class " + javaClass().getName());
    }

    @JRubyMethod
    public IRubyObject canonical_name() {
        String canonicalName = javaClass().getCanonicalName();
        if (canonicalName != null) {
            return getRuntime().newString(canonicalName);
        }
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "package")
    public IRubyObject get_package() {
        return Java.getInstance(getRuntime(), javaClass().getPackage());
    }

    @JRubyMethod
    public IRubyObject class_loader() {
        return Java.getInstance(getRuntime(), javaClass().getClassLoader());
    }

    @JRubyMethod
    public IRubyObject protection_domain() {
        return Java.getInstance(getRuntime(), javaClass().getProtectionDomain());
    }

    @JRubyMethod(required = 1)
    public IRubyObject resource(IRubyObject name) {
        return Java.getInstance(getRuntime(), javaClass().getResource(name.asJavaString()));
    }

    @JRubyMethod(required = 1)
    public IRubyObject resource_as_stream(IRubyObject name) {
        return Java.getInstance(getRuntime(), javaClass().getResourceAsStream(name.asJavaString()));
    }

    @JRubyMethod(required = 1)
    public IRubyObject resource_as_string(IRubyObject name) {
        InputStream in = javaClass().getResourceAsStream(name.asJavaString());
        if (in == null) return getRuntime().getNil();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            int len;
            byte[] buf = new byte[4096];
            while ((len = in.read(buf)) >= 0) {
                out.write(buf, 0, len);
            }
        } catch (IOException e) {
            throw getRuntime().newIOErrorFromException(e);
        } finally {
            try {in.close();} catch (IOException ioe) {}
        }
        return getRuntime().newString(new ByteList(out.toByteArray(), false));
    }

    @SuppressWarnings("unchecked")
    @JRubyMethod(required = 1)
    public IRubyObject annotation(IRubyObject annoClass) {
        if (!(annoClass instanceof JavaClass)) {
            throw getRuntime().newTypeError(annoClass, getRuntime().getJavaSupport().getJavaClassClass());
        }
        return Java.getInstance(getRuntime(), javaClass().getAnnotation(((JavaClass)annoClass).javaClass()));
    }

    @JRubyMethod
    public IRubyObject annotations() {
        // note: intentionally returning the actual array returned from Java, rather
        // than wrapping it in a RubyArray. wave of the future, when java_class will
        // return the actual class, rather than a JavaClass wrapper.
        return Java.getInstance(getRuntime(), javaClass().getAnnotations());
    }

    @JRubyMethod(name = "annotations?")
    public RubyBoolean annotations_p() {
        return getRuntime().newBoolean(javaClass().getAnnotations().length > 0);
    }

    @JRubyMethod
    public IRubyObject declared_annotations() {
        // see note above re: return type
        return Java.getInstance(getRuntime(), javaClass().getDeclaredAnnotations());
    }

    @JRubyMethod(name = "declared_annotations?")
    public RubyBoolean declared_annotations_p() {
        return getRuntime().newBoolean(javaClass().getDeclaredAnnotations().length > 0);
    }

    @SuppressWarnings("unchecked")
    @JRubyMethod(name = "annotation_present?", required = 1)
    public IRubyObject annotation_present_p(IRubyObject annoClass) {
        if (!(annoClass instanceof JavaClass)) {
            throw getRuntime().newTypeError(annoClass, getRuntime().getJavaSupport().getJavaClassClass());
        }
        return getRuntime().newBoolean(javaClass().isAnnotationPresent(((JavaClass)annoClass).javaClass()));
    }

    @JRubyMethod
    public IRubyObject modifiers() {
        return getRuntime().newFixnum(javaClass().getModifiers());
    }

    @JRubyMethod
    public IRubyObject declaring_class() {
        Class<?> clazz = javaClass().getDeclaringClass();
        if (clazz != null) {
            return JavaClass.get(getRuntime(), clazz);
        }
        return getRuntime().getNil();
    }

    @JRubyMethod
    public IRubyObject enclosing_class() {
        return Java.getInstance(getRuntime(), javaClass().getEnclosingClass());
    }

    @JRubyMethod
    public IRubyObject enclosing_constructor() {
        Constructor<?> ctor = javaClass().getEnclosingConstructor();
        if (ctor != null) {
            return new JavaConstructor(getRuntime(), ctor);
        }
        return getRuntime().getNil();
    }

    @JRubyMethod
    public IRubyObject enclosing_method() {
        Method meth = javaClass().getEnclosingMethod();
        if (meth != null) {
            return new JavaMethod(getRuntime(), meth);
        }
        return getRuntime().getNil();
    }

    @JRubyMethod
    public IRubyObject enum_constants() {
        return Java.getInstance(getRuntime(), javaClass().getEnumConstants());
    }

    @JRubyMethod
    public IRubyObject generic_interfaces() {
        return Java.getInstance(getRuntime(), javaClass().getGenericInterfaces());
    }

    @JRubyMethod
    public IRubyObject generic_superclass() {
        return Java.getInstance(getRuntime(), javaClass().getGenericSuperclass());
    }

    @JRubyMethod
    public IRubyObject type_parameters() {
        return Java.getInstance(getRuntime(), javaClass().getTypeParameters());
    }

    @JRubyMethod
    public IRubyObject signers() {
        return Java.getInstance(getRuntime(), javaClass().getSigners());
    }

    private static String getSimpleName(Class<?> clazz) {
 		if (clazz.isArray()) {
 			return getSimpleName(clazz.getComponentType()) + "[]";
 		}

 		String className = clazz.getName();
 		int len = className.length();
        int i = className.lastIndexOf('$');
 		if (i != -1) {
            do {
 				i++;
 			} while (i < len && Character.isDigit(className.charAt(i)));
 			return className.substring(i);
 		}

 		return className.substring(className.lastIndexOf('.') + 1);
 	}

    @JRubyMethod
    public RubyString simple_name() {
        return getRuntime().newString(getSimpleName(javaClass()));
    }

    @JRubyMethod
    public IRubyObject superclass() {
        Class<?> superclass = javaClass().getSuperclass();
        if (superclass == null) {
            return getRuntime().getNil();
        }
        return JavaClass.get(getRuntime(), superclass);
    }

    @JRubyMethod(name = "<=>", required = 1)
    public IRubyObject op_cmp(IRubyObject other) {
        Class me = javaClass();
        Class them = null;

        // dig out the other class
        if (other instanceof JavaClass) {
            JavaClass otherClass = (JavaClass) other;
            them = otherClass.javaClass();
        } else if (other instanceof ConcreteJavaProxy) {
            ConcreteJavaProxy proxy = (ConcreteJavaProxy)other;
            if (proxy.getObject() instanceof Class) {
                them = (Class)proxy.getObject();
            }
        }

        if (them != null) {
            if (this.javaClass() == them) {
                return getRuntime().newFixnum(0);
            }
            if (them.isAssignableFrom(me)) {
                return getRuntime().newFixnum(-1);
            }
            if (me.isAssignableFrom(them)) {
                return getRuntime().newFixnum(1);
            }
        }

        // can't do a comparison
        return getRuntime().getNil();
    }

    @JRubyMethod
    public RubyArray java_instance_methods() {
        return java_methods(javaClass().getMethods(), false);
    }

    @JRubyMethod
    public RubyArray declared_instance_methods() {
        return java_methods(javaClass().getDeclaredMethods(), false);
    }

    private RubyArray java_methods(final Method[] methods, final boolean isStatic) {
        final Ruby runtime = getRuntime();
        final RubyArray result = runtime.newArray(methods.length);
        for ( int i = 0; i < methods.length; i++ ) {
            final Method method = methods[i];
            if ( isStatic == Modifier.isStatic(method.getModifiers()) ) {
                result.append( JavaMethod.create(runtime, method) );
            }
        }
        return result;
    }

    @JRubyMethod
    public RubyArray java_class_methods() {
        return java_methods(javaClass().getMethods(), true);
    }

    @JRubyMethod
    public RubyArray declared_class_methods() {
        return java_methods(javaClass().getDeclaredMethods(), true);
    }

    @JRubyMethod(required = 1, rest = true)
    public JavaMethod java_method(IRubyObject[] args) {
        final Ruby runtime = getRuntime();
        final String methodName = args[0].asJavaString();
        try {
            Class<?>[] argumentTypes = buildArgumentTypes(runtime, args);
            return JavaMethod.create(runtime, javaClass(), methodName, argumentTypes);
        }
        catch (ClassNotFoundException cnfe) {
            throw runtime.newNameError("undefined method '" + methodName +
                "' for class '" + javaClass().getName() + "'", methodName);
        }

    }

    @JRubyMethod(required = 1, rest = true)
    public JavaMethod declared_method(final IRubyObject[] args) {
        final Ruby runtime = getRuntime();
        final String methodName = args[0].asJavaString();
        try {
            Class<?>[] argumentTypes = buildArgumentTypes(runtime, args);
            return JavaMethod.createDeclared(runtime, javaClass(), methodName, argumentTypes);
        }
        catch (ClassNotFoundException cnfe) {
            throw runtime.newNameError("undefined method '" + methodName +
                "' for class '" + javaClass().getName() + "'", methodName);
        }
    }

    @JRubyMethod(required = 1, rest = true)
    public JavaCallable declared_method_smart(final IRubyObject[] args) {
        final Ruby runtime = getRuntime();
        final String methodName = args[0].asJavaString();
        try {
            Class<?>[] argumentTypes = buildArgumentTypes(runtime, args);

            JavaCallable callable = getMatchingCallable(runtime, javaClass(), methodName, argumentTypes);

            if ( callable != null ) return callable;
        }
        catch (ClassNotFoundException cnfe) {
            /* fall through to error below */
        }

        throw runtime.newNameError("undefined method '" + methodName +
            "' for class '" + javaClass().getName() + "'", methodName);
    }

    public static JavaCallable getMatchingCallable(Ruby runtime, Class<?> javaClass, String methodName, Class<?>[] argumentTypes) {
        if ( "<init>".equals(methodName) ) {
            return JavaConstructor.getMatchingConstructor(runtime, javaClass, argumentTypes);
        }
        // FIXME: do we really want 'declared' methods?  includes private/protected, and does _not_
        // include superclass methods
        return JavaMethod.getMatchingDeclaredMethod(runtime, javaClass, methodName, argumentTypes);
    }

    private static Class<?>[] buildArgumentTypes(final Ruby runtime,
        final IRubyObject[] args) throws ClassNotFoundException {
        if ( args.length < 1 ) throw runtime.newArgumentError(args.length, 1);

        Class<?>[] argumentTypes = new Class[args.length - 1];
        for ( int i = 1; i < args.length; i++ ) {
            final IRubyObject arg = args[i];
            final JavaClass type;
            if ( arg instanceof JavaClass ) {
                type = (JavaClass) arg;
            } else if ( arg.respondsTo("java_class") ) {
                type = (JavaClass) arg.callMethod(runtime.getCurrentContext(), "java_class");
            } else {
                type = forNameVerbose(runtime, arg.asJavaString());
            }
            argumentTypes[ i - 1 ] = type.javaClass();
        }
        return argumentTypes;
    }

    @JRubyMethod
    public RubyArray constructors() {
        final RubyArray constructors = this.constructors;
        if ( constructors != null) return constructors;
        return this.constructors = buildConstructors(getRuntime(), javaClass().getConstructors());
    }

    @JRubyMethod
    public RubyArray classes() {
        return toRubyArray(getRuntime(), javaClass().getClasses());
    }

    @JRubyMethod
    public RubyArray declared_classes() {
        final Ruby runtime = getRuntime();
        RubyArray result = runtime.newArray();
        Class<?> javaClass = javaClass();
        try {
            Class<?>[] classes = javaClass.getDeclaredClasses();
            for (int i = 0; i < classes.length; i++) {
                if (Modifier.isPublic(classes[i].getModifiers())) {
                    result.append(get(runtime, classes[i]));
                }
            }
        } catch (SecurityException e) {
            // restrictive security policy; no matter, we only want public
            // classes anyway
            try {
                Class<?>[] classes = javaClass.getClasses();
                for (int i = 0; i < classes.length; i++) {
                    if (javaClass == classes[i].getDeclaringClass()) {
                        result.append(get(runtime, classes[i]));
                    }
                }
            } catch (SecurityException e2) {
                // very restrictive policy (disallows Member.PUBLIC)
                // we'd never actually get this far in that case
            }
        }
        return result;
    }

    @JRubyMethod
    public RubyArray declared_constructors() {
        return buildConstructors(getRuntime(), javaClass().getDeclaredConstructors());
    }

    private static RubyArray buildConstructors(final Ruby runtime, Constructor<?>[] constructors) {
        RubyArray result = RubyArray.newArray(runtime, constructors.length);
        for ( int i = 0; i < constructors.length; i++ ) {
            result.append( new JavaConstructor(runtime, constructors[i]) );
        }
        return result;
    }

    @JRubyMethod(rest = true)
    public JavaConstructor constructor(IRubyObject[] args) {
        final Ruby runtime = getRuntime();
        try {
            Class<?>[] parameterTypes = buildClassArgs(runtime, args);
            @SuppressWarnings("unchecked")
            Constructor<?> constructor = javaClass().getConstructor(parameterTypes);
            return new JavaConstructor(runtime, constructor);
        }
        catch (NoSuchMethodException nsme) {
            throw runtime.newNameError("no matching java constructor", null);
        }
    }

    @JRubyMethod(rest = true)
    public JavaConstructor declared_constructor(IRubyObject[] args) {
        final Ruby runtime = getRuntime();
        try {
            Class<?>[] parameterTypes = buildClassArgs(runtime, args);
            @SuppressWarnings("unchecked")
            Constructor<?> constructor = javaClass().getDeclaredConstructor (parameterTypes);
            return new JavaConstructor(runtime, constructor);
        }
        catch (NoSuchMethodException nsme) {
            throw runtime.newNameError("no matching java constructor", null);
        }
    }

    private static Class<?>[] buildClassArgs(final Ruby runtime, IRubyObject[] args) {
        Class<?>[] parameterTypes = new Class<?>[args.length];
        for ( int i = 0; i < args.length; i++ ) {
            final IRubyObject arg = args[i];
            final JavaClass type;
            if ( arg instanceof JavaClass ) {
                type = (JavaClass) arg;
            } else if ( arg.respondsTo("java_class") ) {
                type = (JavaClass) arg.callMethod(runtime.getCurrentContext(), "java_class");
            } else {
                type = forNameVerbose(runtime, arg.asJavaString());
            }
            parameterTypes[i] = type.javaClass();
        }
        return parameterTypes;
    }

    @JRubyMethod
    public JavaClass array_class() {
        final Class<?> arrayClass = Array.newInstance(javaClass(), 0).getClass();
        return JavaClass.get(getRuntime(), arrayClass);
    }

    @JRubyMethod(required = 1)
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
                    .newTypeError(dimensionLength, getRuntime().getInteger());
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

    public IRubyObject emptyJavaArray(ThreadContext context) {
        return ArrayUtils.emptyJavaArrayDirect(context, javaClass());
    }

    public IRubyObject javaArraySubarray(ThreadContext context, JavaArray fromArray, int index, int size) {
        return ArrayUtils.javaArraySubarrayDirect(context, getValue(), index, size);
    }

    /**
     * Contatenate two Java arrays into a new one. The component type of the
     * additional array must be assignable to the component type of the
     * original array.
     *
     * @param context
     * @param original
     * @param additional
     * @return
     */
    public IRubyObject concatArrays(ThreadContext context, JavaArray original, JavaArray additional) {
        return ArrayUtils.concatArraysDirect(context, original.getValue(), additional.getValue());
    }

    /**
     * The slow version for when concatenating a Java array of a different type.
     *
     * @param context
     * @param original
     * @param additional
     * @return
     */
    public IRubyObject concatArrays(ThreadContext context, JavaArray original, IRubyObject additional) {
        return ArrayUtils.concatArraysDirect(context, original.getValue(), additional);
    }

    public IRubyObject javaArrayFromRubyArray(ThreadContext context, IRubyObject fromArray) {
        Ruby runtime = context.runtime;
        if (!(fromArray instanceof RubyArray)) {
            throw runtime.newTypeError(fromArray, runtime.getArray());
        }

        Object newArray = javaArrayFromRubyArrayDirect(context, fromArray);

        return new ArrayJavaProxy(runtime, Java.getProxyClassForObject(runtime, newArray), newArray, JavaUtil.getJavaConverter(javaClass()));
    }

    public Object javaArrayFromRubyArrayDirect(ThreadContext context, IRubyObject fromArray) {
        Ruby runtime = context.runtime;
        if (!(fromArray instanceof RubyArray)) {
            throw runtime.newTypeError(fromArray, runtime.getArray());
        }
        RubyArray rubyArray = (RubyArray)fromArray;
        Object newArray = Array.newInstance(javaClass(), rubyArray.size());

        if (javaClass().isArray()) {
            // if it's an array of arrays, recurse with the component type
            for (int i = 0; i < rubyArray.size(); i++) {
                JavaClass componentType = component_type();
                Object componentArray = componentType.javaArrayFromRubyArrayDirect(context, rubyArray.eltInternal(i));
                ArrayUtils.setWithExceptionHandlingDirect(runtime, newArray, i, componentArray);
            }
        } else {
            ArrayUtils.copyDataToJavaArrayDirect(context, rubyArray, newArray);
        }

        return newArray;
    }

    @JRubyMethod
    public RubyArray fields() {
        return buildFieldResults(getRuntime(), javaClass().getFields());
    }

    @JRubyMethod
    public RubyArray declared_fields() {
        return buildFieldResults(getRuntime(), javaClass().getDeclaredFields());
    }

    private static RubyArray buildFieldResults(final Ruby runtime, Field[] fields) {
        RubyArray result = runtime.newArray( fields.length );
        for ( int i = 0; i < fields.length; i++ ) {
            result.append( new JavaField(runtime, fields[i]) );
        }
        return result;
    }

    @JRubyMethod(required = 1)
    public JavaField field(ThreadContext context, IRubyObject name) {
        Class<?> javaClass = javaClass();
        Ruby runtime = context.runtime;
        String stringName = name.asJavaString();

        try {
            return new JavaField(runtime, javaClass.getField(stringName));
        } catch (NoSuchFieldException nsfe) {
            String newName = JavaUtil.getJavaCasedName(stringName);
            if(newName != null) {
                try {
                    return new JavaField(runtime, javaClass.getField(newName));
                } catch (NoSuchFieldException nsfe2) {}
            }
            throw undefinedFieldError(runtime, javaClass.getName(), stringName);
         }
    }

    @JRubyMethod(required = 1)
    public JavaField declared_field(ThreadContext context, IRubyObject name) {
        Class<?> javaClass = javaClass();
        Ruby runtime = context.runtime;
        String stringName = name.asJavaString();

        try {
            return new JavaField(runtime, javaClass.getDeclaredField(stringName));
        } catch (NoSuchFieldException nsfe) {
            String newName = JavaUtil.getJavaCasedName(stringName);
            if(newName != null) {
                try {
                    return new JavaField(runtime, javaClass.getDeclaredField(newName));
                } catch (NoSuchFieldException nsfe2) {}
            }
            throw undefinedFieldError(runtime, javaClass.getName(), stringName);
        }
    }

    public static RaiseException undefinedFieldError(Ruby runtime, String javaClassName, String name) {
        return runtime.newNameError("undefined field '" + name + "' for class '" + javaClassName + "'", name);
    }

    @JRubyMethod
    public RubyArray interfaces() {
        return toRubyArray(getRuntime(), javaClass().getInterfaces());
    }

    @JRubyMethod(name = "primitive?")
    public RubyBoolean primitive_p() {
        return getRuntime().newBoolean(isPrimitive());
    }

    boolean isPrimitive() { return javaClass().isPrimitive(); }

    @JRubyMethod(name = "assignable_from?", required = 1)
    public RubyBoolean assignable_from_p(IRubyObject other) {
        if (! (other instanceof JavaClass)) {
            throw getRuntime().newTypeError("assignable_from requires JavaClass (" + other.getType() + " given)");
        }

        Class<?> otherClass = ((JavaClass) other).javaClass();
        return assignable(javaClass(), otherClass) ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    public static boolean assignable(Class<?> thisClass, Class<?> otherClass) {
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

    @JRubyMethod
    public JavaClass component_type() {
        if ( ! javaClass().isArray() ) {
            throw getRuntime().newTypeError("not a java array-class");
        }
        return JavaClass.get(getRuntime(), javaClass().getComponentType());
    }

    private static Constructor[] getConstructors(Class<?> javaClass) {
        try {
            return javaClass.getConstructors();
        }
        catch (SecurityException e) { return new Constructor[0]; }
    }

    private static Class<?>[] getDeclaredClasses(Class<?> javaClass) {
        try {
            return javaClass.getDeclaredClasses();
        }
        catch (SecurityException e) { return new Class<?>[0]; }
        catch (NoClassDefFoundError cnfe) {
            // This is a Scala-specific hack, since Scala uses peculiar
            // naming conventions and class attributes that confuse Java's
            // reflection logic and cause a blow up in getDeclaredClasses.
            // See http://lampsvn.epfl.ch/trac/scala/ticket/2749
            return new Class<?>[0];
        }
    }

    private static Class<?>[] getClasses(Class<?> javaClass) {
        try {
            return javaClass.getClasses();
        }
        catch (SecurityException e) { return new Class<?>[0]; }
    }

    public static Field[] getDeclaredFields(Class<?> javaClass) {
        try {
            return javaClass.getDeclaredFields();
        }
        catch (SecurityException e) {
            return getFields(javaClass);
        }
    }

    public static Field[] getFields(Class<?> javaClass) {
        try {
            return javaClass.getFields();
        }
        catch (SecurityException e) { return new Field[0]; }
    }

    private static boolean methodsAreEquivalent(Method child, Method parent) {
        return parent.getDeclaringClass().isAssignableFrom(child.getDeclaringClass())
                && child.getReturnType() == parent.getReturnType()
                && child.isVarArgs() == parent.isVarArgs()
                && Modifier.isPublic(child.getModifiers()) == Modifier.isPublic(parent.getModifiers())
                && Modifier.isProtected(child.getModifiers()) == Modifier.isProtected(parent.getModifiers())
                && Modifier.isStatic(child.getModifiers()) == Modifier.isStatic(parent.getModifiers())
                && Arrays.equals(child.getParameterTypes(), parent.getParameterTypes());
    }

    private static int addNewMethods(HashMap<String, List<Method>> nameMethods, Method[] methods, boolean includeStatic, boolean removeDuplicate) {
        int added = 0;
        Methods: for (Method m : methods) {
            // Skip private methods, since they may mess with dispatch
            if (Modifier.isPrivate(m.getModifiers())) continue;

            // ignore bridge methods because we'd rather directly call methods that this method
            // is bridging (and such methods are by definition always available.)
            if ((m.getModifiers()&ACC_BRIDGE)!=0)
                continue;

            if (!includeStatic && Modifier.isStatic(m.getModifiers())) {
                // Skip static methods if we're not suppose to include them.
                // Generally for superclasses; we only bind statics from the actual
                // class.
                continue;
            }
            List<Method> childMethods = nameMethods.get(m.getName());
            if (childMethods == null) {
                // first method of this name, add a collection for it
                childMethods = new ArrayList<Method>(1);
                childMethods.add(m);
                nameMethods.put(m.getName(), childMethods);
                added++;
            } else {
                // we have seen other methods; check if we already have
                // an equivalent one
                for (ListIterator<Method> iter = childMethods.listIterator(); iter.hasNext();) {
                    Method m2 = iter.next();
                    if (methodsAreEquivalent(m2, m)) {
                        if (removeDuplicate) {
                            // Replace the existing method, since the super call is more general
                            // and virtual dispatch will call the subclass impl anyway.
                            // Used for instance methods, for which we usually want to use the highest-up
                            // callable implementation.
                            iter.set(m);
                        } else {
                            // just skip the new method, since we don't need it (already found one)
                            // used for interface methods, which we want to add unconditionally
                            // but only if we need them
                        }
                        continue Methods;
                    }
                }
                // no equivalent; add it
                childMethods.add(m);
                added++;
            }
        }
        return added;
    }

    public static Method[] getMethods(Class<?> javaClass) {
        HashMap<String, List<Method>> nameMethods = new HashMap<String, List<Method>>(30);

        // to better size the final ArrayList below
        int total = 0;

        // we scan all superclasses, but avoid adding superclass methods with
        // same name+signature as subclass methods (see JRUBY-3130)
        for (Class c = javaClass; c != null; c = c.getSuperclass()) {
            // only add class's methods if it's public or we can set accessible
            // (see JRUBY-4799)
            if (Modifier.isPublic(c.getModifiers()) || CAN_SET_ACCESSIBLE) {
                // for each class, scan declared methods for new signatures
                try {
                    // add methods, including static if this is the actual class,
                    // and replacing child methods with equivalent parent methods
                    total += addNewMethods(nameMethods, c.getDeclaredMethods(), c == javaClass, true);
                } catch (SecurityException e) {
                }
            }

            // then do the same for each interface
            for (Class i : c.getInterfaces()) {
                try {
                    // add methods, not including static (should be none on
                    // interfaces anyway) and not replacing child methods with
                    // parent methods
                    total += addNewMethods(nameMethods, i.getMethods(), false, false);
                } catch (SecurityException e) {
                }
            }
        }

        // now only bind the ones that remain
        ArrayList<Method> finalList = new ArrayList<Method>(total);

        for (Map.Entry<String, List<Method>> entry : nameMethods.entrySet()) {
            finalList.addAll(entry.getValue());
        }

        return finalList.toArray(new Method[finalList.size()]);
    }

    private static final int ACC_BRIDGE    = 0x00000040;
}
