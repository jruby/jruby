/*
 **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004-2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006-2007 Miguel Covarrubias <mlcovarrubias@gmail.com>
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
package org.jruby;

import static org.jruby.anno.FrameField.VISIBILITY;
import static org.jruby.runtime.Visibility.MODULE_FUNCTION;
import static org.jruby.runtime.Visibility.PRIVATE;
import static org.jruby.runtime.Visibility.PROTECTED;
import static org.jruby.runtime.Visibility.PUBLIC;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyConstant;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JavaMethodDescriptor;
import org.jruby.anno.TypePopulator;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.compiler.ASTInspector;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.AliasMethod;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.DefaultMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.FullFunctionCallbackMethod;
import org.jruby.internal.runtime.methods.JavaMethod.JavaMethodOne;
import org.jruby.internal.runtime.methods.JavaMethod.JavaMethodZero;
import org.jruby.internal.runtime.methods.MethodMethod;
import org.jruby.internal.runtime.methods.ProcMethod;
import org.jruby.internal.runtime.methods.SimpleCallbackMethod;
import org.jruby.internal.runtime.methods.UndefinedMethod;
import org.jruby.internal.runtime.methods.WrapperMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.MethodFactory;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.Variable;
import org.jruby.runtime.callback.Callback;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.ClassProvider;
import org.jruby.util.IdUtil;
import org.jruby.util.collections.WeakHashSet;

/**
 *
 * @author  jpetersen
 */
@JRubyClass(name="Module")
public class RubyModule extends RubyObject {
    private static final boolean DEBUG = false;
    public static final Set<String> SCOPE_CAPTURING_METHODS = new HashSet<String>(Arrays.asList(
            "eval",
            "module_eval",
            "class_eval",
            "instance_eval",
            "instance_exec",
            "binding",
            "local_variables"
            ));
    
    public static RubyClass createModuleClass(Ruby runtime, RubyClass moduleClass) {
        moduleClass.index = ClassIndex.MODULE;
        moduleClass.kindOf = new RubyModule.KindOf() {
            @Override
            public boolean isKindOf(IRubyObject obj, RubyModule type) {
                return obj instanceof RubyModule;
            }
        };
        
        moduleClass.defineAnnotatedMethods(RubyModule.class);
        moduleClass.defineAnnotatedMethods(ModuleKernelMethods.class);

        return moduleClass;
    }
    
    public static class ModuleKernelMethods {
        @JRubyMethod
        public static IRubyObject autoload(IRubyObject recv, IRubyObject arg0, IRubyObject arg1) {
            return RubyKernel.autoload(recv, arg0, arg1);
        }
        
        @JRubyMethod(name = "autoload?")
        public static IRubyObject autoload_p(ThreadContext context, IRubyObject recv, IRubyObject arg0) {
            return RubyKernel.autoload_p(context, recv, arg0);
        }
    }
    
    static ObjectAllocator MODULE_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyModule(runtime, klass);
        }
    };
    
    @Override
    public int getNativeTypeIndex() {
        return ClassIndex.MODULE;
    }

    @Override
    public boolean isModule() {
        return true;
    }

    @Override
    public boolean isClass() {
        return false;
    }

    public boolean isSingleton() {
        return false;
    }    
    
    // superClass may be null.
    protected RubyClass superClass;

    public int index;

    public static class KindOf {
        public static final KindOf DEFAULT_KIND_OF = new KindOf();
        public boolean isKindOf(IRubyObject obj, RubyModule type) {
            return obj.getMetaClass().hasModuleInHierarchy(type);
        }
    }
    
    public boolean isInstance(IRubyObject object) {
        return kindOf.isKindOf(object, this);
    }

    public KindOf kindOf = KindOf.DEFAULT_KIND_OF;

    public final int id;

    // Containing class...The parent of Object is null. Object should always be last in chain.
    public RubyModule parent;

    // ClassId is the name of the class/module sans where it is located.
    // If it is null, then it an anonymous class.
    protected String classId;

    private static final Map<String, IRubyObject> DUMMY_CONSTANTS = Collections.unmodifiableMap(new HashMap(0));

    private volatile Map<String, IRubyObject> constants;
    private final Map<String, DynamicMethod> methods = new ConcurrentHashMap<String, DynamicMethod>(4, 0.9f, 1);
    private final Map<String, CacheEntry> cachedMethods = new ConcurrentHashMap<String, CacheEntry>(4, 0.75f, 1);

    public Map<String, IRubyObject> getConstantMap() {
        return constants == null ? DUMMY_CONSTANTS : constants;
    }

    public synchronized Map<String, IRubyObject> getConstantMapForWrite() {
        return constants == null ? constants = new ConcurrentHashMap<String, IRubyObject>(4, 0.9f, 1) : constants;
    }
    
    protected static class Generation {
        public volatile Object token;
        public Generation() {
            token = new Object();
        }
        public synchronized void update() {
            token = new Object();
        }
    }
    protected final Generation generation;
    
    protected Set<RubyClass> includingHierarchies;
    
    public void addIncludingHierarchy(IncludedModuleWrapper hierarchy) {
        synchronized (getRuntime().getHierarchyLock()) {
            Set<RubyClass> oldIncludingHierarchies = includingHierarchies;
            if (oldIncludingHierarchies == null) includingHierarchies = oldIncludingHierarchies = new WeakHashSet(4);
            oldIncludingHierarchies.add(hierarchy);
        }
    }
    
    // ClassProviders return Java class/module (in #defineOrGetClassUnder and
    // #defineOrGetModuleUnder) when class/module is opened using colon syntax. 
    private transient List<ClassProvider> classProviders;

    /** separate path for MetaClass construction
     * 
     */
    protected RubyModule(Ruby runtime, RubyClass metaClass, boolean objectSpace) {
        super(runtime, metaClass, objectSpace);
        id = runtime.allocModuleId();
        // if (parent == null) parent = runtime.getObject();
        setFlag(USER7_F, !isClass());
        generation = new Generation();
    }

    /** separate path for MetaClass construction
     * 
     */
    protected RubyModule(Ruby runtime, RubyClass metaClass, Generation generation, boolean objectSpace) {
        super(runtime, metaClass, objectSpace);
        id = runtime.allocModuleId();
        // if (parent == null) parent = runtime.getObject();
        setFlag(USER7_F, !isClass());
        this.generation = generation;
    }
    
    /** used by MODULE_ALLOCATOR and RubyClass constructors
     * 
     */
    protected RubyModule(Ruby runtime, RubyClass metaClass) {
        this(runtime, metaClass, runtime.isObjectSpaceEnabled());
    }
    
    /** standard path for Module construction
     * 
     */
    protected RubyModule(Ruby runtime) {
        this(runtime, runtime.getModule());
    }

    public boolean needsImplementer() {
        return getFlag(USER7_F);
    }
    
    /** rb_module_new
     * 
     */
    public static RubyModule newModule(Ruby runtime) {
        return new RubyModule(runtime);
    }
    
    /** rb_module_new/rb_define_module_id/rb_name_class/rb_set_class_path
     * 
     */
    public static RubyModule newModule(Ruby runtime, String name, RubyModule parent, boolean setParent) {
        RubyModule module = newModule(runtime);
        module.setBaseName(name);
        if (setParent) module.setParent(parent);
        parent.setConstant(name, module);
        return module;
    }
    
    // synchronized method per JRUBY-1173 (unsafe Double-Checked Locking)
    // FIXME: synchronization is still wrong in CP code
    public synchronized void addClassProvider(ClassProvider provider) {
        if (classProviders == null) {
            List<ClassProvider> cp = Collections.synchronizedList(new ArrayList<ClassProvider>());
            cp.add(provider);
            classProviders = cp;
        } else {
            synchronized(classProviders) {
                if (!classProviders.contains(provider)) {
                    classProviders.add(provider);
                }
            }
        }
    }

    public void removeClassProvider(ClassProvider provider) {
        if (classProviders != null) {
            classProviders.remove(provider);
        }
    }

    private void checkForCyclicInclude(RubyModule m) throws RaiseException {
        if (getNonIncludedClass() == m.getNonIncludedClass()) {
            throw getRuntime().newArgumentError("cyclic include detected");
        }
    }

    private RubyClass searchProvidersForClass(String name, RubyClass superClazz) {
        if (classProviders != null) {
            synchronized(classProviders) {
                RubyClass clazz;
                for (ClassProvider classProvider: classProviders) {
                    if ((clazz = classProvider.defineClassUnder(this, name, superClazz)) != null) {
                        return clazz;
                    }
                }
            }
        }
        return null;
    }

    private RubyModule searchProvidersForModule(String name) {
        if (classProviders != null) {
            synchronized(classProviders) {
                RubyModule module;
                for (ClassProvider classProvider: classProviders) {
                    if ((module = classProvider.defineModuleUnder(this, name)) != null) {
                        return module;
                    }
                }
            }
        }
        return null;
    }

    /** Getter for property superClass.
     * @return Value of property superClass.
     */
    public RubyClass getSuperClass() {
        return superClass;
    }

    protected void setSuperClass(RubyClass superClass) {
        // update superclass reference
        this.superClass = superClass;
    }

    public RubyModule getParent() {
        return parent;
    }

    public void setParent(RubyModule parent) {
        this.parent = parent;
    }

    public Map<String, DynamicMethod> getMethods() {
        return methods;
    }
    

    // note that addMethod now does its own put, so any change made to
    // functionality here should be made there as well 
    private void putMethod(String name, DynamicMethod method) {
        getMethods().put(name, method);
    }

    /**
     * Is this module one that in an included one (e.g. an IncludedModuleWrapper). 
     */
    public boolean isIncluded() {
        return false;
    }

    public RubyModule getNonIncludedClass() {
        return this;
    }

    public String getBaseName() {
        return classId;
    }

    public void setBaseName(String name) {
        classId = name;
    }
    
    private volatile String bareName;
    private volatile String fullName;

    /**
     * Generate a fully-qualified class name or a #-style name for anonymous and singleton classes.
     * 
     * Ruby C equivalent = "classname"
     * 
     * @return The generated class name
     */
    public String getName() {
        if (fullName == null) {
            fullName = calculateFullName();
        }
        return fullName;
    }

    private String calculateFullName() {
        if (getBaseName() == null) {
            if (bareName == null) {
                if (isClass()) {
                    bareName = "#<" + "Class" + ":01x" + Integer.toHexString(System.identityHashCode(this)) + ">";
                } else {
                    bareName = "#<" + "Module" + ":01x" + Integer.toHexString(System.identityHashCode(this)) + ">";
                }
            }

            return bareName;
        }

        String result = getBaseName();
        RubyClass objectClass = getRuntime().getObject();

        for (RubyModule p = this.getParent(); p != null && p != objectClass; p = p.getParent()) {
            String pName = p.getBaseName();
            // This is needed when the enclosing class or module is a singleton.
            // In that case, we generated a name such as null::Foo, which broke 
            // Marshalling, among others. The correct thing to do in this situation 
            // is to insert the generate the name of form #<Class:01xasdfasd> if 
            // it's a singleton module/class, which this code accomplishes.
            if(pName == null) {
                pName = p.getName();
            }
            result = pName + "::" + result;
        }

        return result;
    }

    /**
     * Create a wrapper to use for including the specified module into this one.
     * 
     * Ruby C equivalent = "include_class_new"
     * 
     * @return The module wrapper
     */
    @Deprecated
    public IncludedModuleWrapper newIncludeClass(RubyClass superClazz) {
        IncludedModuleWrapper includedModule = new IncludedModuleWrapper(getRuntime(), superClazz, this);

        // include its parent (and in turn that module's parents)
        if (getSuperClass() != null) {
            includedModule.includeModule(getSuperClass());
        }

        return includedModule;
    }
    /**
     * Finds a class that is within the current module (or class).
     * 
     * @param name to be found in this module (or class)
     * @return the class or null if no such class
     */
    public RubyClass getClass(String name) {
        IRubyObject module;
        if ((module = getConstantAt(name)) instanceof RubyClass) {
            return (RubyClass)module;
        }
        return null;
    }

    public RubyClass fastGetClass(String internedName) {
        IRubyObject module;
        if ((module = fastGetConstantAt(internedName)) instanceof RubyClass) {
            return (RubyClass)module;
        }
        return null;
    }

    /**
     * Include a new module in this module or class.
     * 
     * @param arg The module to include
     */
    public synchronized void includeModule(IRubyObject arg) {
        assert arg != null;

        testFrozen("module");
        if (!isTaint()) {
            getRuntime().secure(4);
        }

        if (!(arg instanceof RubyModule)) {
            throw getRuntime().newTypeError("Wrong argument type " + arg.getMetaClass().getName() +
                    " (expected Module).");
        }

        RubyModule module = (RubyModule) arg;

        // Make sure the module we include does not already exist
        checkForCyclicInclude(module);

        infectBy(module);

        doIncludeModule(module);
        invalidateConstantCache();
        invalidateCacheDescendants();
    }

    public void defineMethod(String name, Callback method) {
        Visibility visibility = name.equals("initialize") ?
                PRIVATE : PUBLIC;
        addMethod(name, new FullFunctionCallbackMethod(this, method, visibility));
    }
    
    public void defineAnnotatedMethod(Class clazz, String name) {
        // FIXME: This is probably not very efficient, since it loads all methods for each call
        boolean foundMethod = false;
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(name) && defineAnnotatedMethod(method, MethodFactory.createFactory(getRuntime().getJRubyClassLoader()))) {
                foundMethod = true;
            }
        }

        if (!foundMethod) {
            throw new RuntimeException("No JRubyMethod present for method " + name + "on class " + clazz.getName());
        }
    }
    
    public void defineAnnotatedConstants(Class clazz) {
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field field : declaredFields) {
            if (Modifier.isStatic(field.getModifiers())) {
                defineAnnotatedConstant(field);
            }
        }
    }

    public boolean defineAnnotatedConstant(Field field) {
        JRubyConstant jrubyConstant = field.getAnnotation(JRubyConstant.class);

        if (jrubyConstant == null) return false;

        String[] names = jrubyConstant.value();
        if(names.length == 0) {
            names = new String[]{field.getName()};
        }

        Class tp = field.getType();
        IRubyObject realVal;

        try {
            if(tp == Integer.class || tp == Integer.TYPE || tp == Short.class || tp == Short.TYPE || tp == Byte.class || tp == Byte.TYPE) {
                realVal = RubyNumeric.int2fix(getRuntime(), field.getInt(null));
            } else if(tp == Boolean.class || tp == Boolean.TYPE) {
                realVal = field.getBoolean(null) ? getRuntime().getTrue() : getRuntime().getFalse();
            } else {
                realVal = getRuntime().getNil();
            }
        } catch(Exception e) {
            realVal = getRuntime().getNil();
        }

        
        for(String name : names) {
            this.fastSetConstant(name, realVal);
        }

        return true;
    }

    public void defineAnnotatedMethods(Class clazz) {
        defineAnnotatedMethodsIndividually(clazz);
    }
    
    public static class MethodClumper {
        Map<String, List<JavaMethodDescriptor>> annotatedMethods = new HashMap<String, List<JavaMethodDescriptor>>();
        Map<String, List<JavaMethodDescriptor>> staticAnnotatedMethods = new HashMap<String, List<JavaMethodDescriptor>>();
        Map<String, List<JavaMethodDescriptor>> annotatedMethods1_8 = new HashMap<String, List<JavaMethodDescriptor>>();
        Map<String, List<JavaMethodDescriptor>> staticAnnotatedMethods1_8 = new HashMap<String, List<JavaMethodDescriptor>>();
        Map<String, List<JavaMethodDescriptor>> annotatedMethods1_9 = new HashMap<String, List<JavaMethodDescriptor>>();
        Map<String, List<JavaMethodDescriptor>> staticAnnotatedMethods1_9 = new HashMap<String, List<JavaMethodDescriptor>>();
        
        public void clump(Class cls) {
            Method[] declaredMethods = cls.getDeclaredMethods();
            for (Method method: declaredMethods) {
                JRubyMethod anno = method.getAnnotation(JRubyMethod.class);
                if (anno == null) continue;
                
                JavaMethodDescriptor desc = new JavaMethodDescriptor(method);
                
                String name = anno.name().length == 0 ? method.getName() : anno.name()[0];
                
                List<JavaMethodDescriptor> methodDescs;
                Map<String, List<JavaMethodDescriptor>> methodsHash = null;
                if (desc.isStatic) {
                    if (anno.compat() == CompatVersion.RUBY1_8) {
                        methodsHash = staticAnnotatedMethods1_8;
                    } else if (anno.compat() == CompatVersion.RUBY1_9) {
                        methodsHash = staticAnnotatedMethods1_9;
                    } else {
                        methodsHash = staticAnnotatedMethods;
                    }
                } else {
                    if (anno.compat() == CompatVersion.RUBY1_8) {
                        methodsHash = annotatedMethods1_8;
                    } else if (anno.compat() == CompatVersion.RUBY1_9) {
                        methodsHash = annotatedMethods1_9;
                    } else {
                        methodsHash = annotatedMethods;
                    }
                }
                
                methodDescs = methodsHash.get(name);
                if (methodDescs == null) {
                    methodDescs = new ArrayList<JavaMethodDescriptor>();
                    methodsHash.put(name, methodDescs);
                }
                
                methodDescs.add(desc);
            }
        }

        public Map<String, List<JavaMethodDescriptor>> getAnnotatedMethods() {
            return annotatedMethods;
        }

        public Map<String, List<JavaMethodDescriptor>> getAnnotatedMethods1_8() {
            return annotatedMethods1_8;
        }

        public Map<String, List<JavaMethodDescriptor>> getAnnotatedMethods1_9() {
            return annotatedMethods1_9;
        }

        public Map<String, List<JavaMethodDescriptor>> getStaticAnnotatedMethods() {
            return staticAnnotatedMethods;
        }

        public Map<String, List<JavaMethodDescriptor>> getStaticAnnotatedMethods1_8() {
            return staticAnnotatedMethods1_8;
        }

        public Map<String, List<JavaMethodDescriptor>> getStaticAnnotatedMethods1_9() {
            return staticAnnotatedMethods1_9;
        }
    }
    
    public void defineAnnotatedMethodsIndividually(Class clazz) {
        String x = clazz.getSimpleName();
        TypePopulator populator = null;
        
        if (RubyInstanceConfig.FULL_TRACE_ENABLED) {
            // we need full traces, use default (slow) populator
            if (DEBUG) System.out.println("trace mode, using default populator");
            populator = TypePopulator.DEFAULT;
        } else {
            try {
                String qualifiedName = "org.jruby.gen." + clazz.getCanonicalName().replace('.', '$');

                if (DEBUG) System.out.println("looking for " + qualifiedName + "$Populator");

                Class populatorClass = Class.forName(qualifiedName + "$Populator");
                populator = (TypePopulator)populatorClass.newInstance();
            } catch (Throwable t) {
                if (DEBUG) System.out.println("Could not find it, using default populator");
                populator = TypePopulator.DEFAULT;
            }
        }
        
        populator.populate(this, clazz);
    }
    
    private static MethodFactory.MethodDefiningCallback methodDefiningCallback = new MethodFactory.MethodDefiningCallback() {
        public void define(RubyModule module, JavaMethodDescriptor desc, DynamicMethod dynamicMethod) {
            JRubyMethod jrubyMethod = desc.anno;
            if (jrubyMethod.frame()) {
                for (String name : jrubyMethod.name()) {
                    ASTInspector.FRAME_AWARE_METHODS.add(name);
                }
            }
            if(jrubyMethod.compat() == CompatVersion.BOTH ||
                    module.getRuntime().getInstanceConfig().getCompatVersion() == jrubyMethod.compat()) {
                RubyModule singletonClass;

                if (jrubyMethod.meta()) {
                    singletonClass = module.getSingletonClass();
                    dynamicMethod.setImplementationClass(singletonClass);

                    String baseName;
                    if (jrubyMethod.name().length == 0) {
                        baseName = desc.name;
                        singletonClass.addMethod(baseName, dynamicMethod);
                    } else {
                        baseName = jrubyMethod.name()[0];
                        for (String name : jrubyMethod.name()) {
                            singletonClass.addMethod(name, dynamicMethod);
                        }
                    }

                    if (jrubyMethod.alias().length > 0) {
                        for (String alias : jrubyMethod.alias()) {
                            singletonClass.defineAlias(alias, baseName);
                        }
                    }
                } else {
                    String baseName;
                    if (jrubyMethod.name().length == 0) {
                        baseName = desc.name;
                        module.addMethod(baseName, dynamicMethod);
                    } else {
                        baseName = jrubyMethod.name()[0];
                        for (String name : jrubyMethod.name()) {
                            module.addMethod(name, dynamicMethod);
                        }
                    }

                    if (jrubyMethod.alias().length > 0) {
                        for (String alias : jrubyMethod.alias()) {
                            module.defineAlias(alias, baseName);
                        }
                    }

                    if (jrubyMethod.module()) {
                        singletonClass = module.getSingletonClass();
                        // module/singleton methods are all defined public
                        DynamicMethod moduleMethod = dynamicMethod.dup();
                        moduleMethod.setVisibility(PUBLIC);

                        if (jrubyMethod.name().length == 0) {
                            baseName = desc.name;
                            singletonClass.addMethod(desc.name, moduleMethod);
                        } else {
                            baseName = jrubyMethod.name()[0];
                            for (String name : jrubyMethod.name()) {
                                singletonClass.addMethod(name, moduleMethod);
                            }
                        }

                        if (jrubyMethod.alias().length > 0) {
                            for (String alias : jrubyMethod.alias()) {
                                singletonClass.defineAlias(alias, baseName);
                            }
                        }
                    }
                }
            }
        }
    };
    
    public boolean defineAnnotatedMethod(String name, List<JavaMethodDescriptor> methods, MethodFactory methodFactory) {
        JavaMethodDescriptor desc = methods.get(0);
        if (methods.size() == 1) {
            return defineAnnotatedMethod(desc, methodFactory);
        } else {
            DynamicMethod dynamicMethod = methodFactory.getAnnotatedMethod(this, methods);
            methodDefiningCallback.define(this, desc, dynamicMethod);
            
            return true;
        }
    }
    
    public boolean defineAnnotatedMethod(Method method, MethodFactory methodFactory) { 
        JRubyMethod jrubyMethod = method.getAnnotation(JRubyMethod.class);

        if (jrubyMethod == null) return false;

            if(jrubyMethod.compat() == CompatVersion.BOTH ||
                    getRuntime().getInstanceConfig().getCompatVersion() == jrubyMethod.compat()) {
            JavaMethodDescriptor desc = new JavaMethodDescriptor(method);
            DynamicMethod dynamicMethod = methodFactory.getAnnotatedMethod(this, desc);
            methodDefiningCallback.define(this, desc, dynamicMethod);

            return true;
        }
        return false;
    }
    
    public boolean defineAnnotatedMethod(JavaMethodDescriptor desc, MethodFactory methodFactory) { 
        JRubyMethod jrubyMethod = desc.anno;

        if (jrubyMethod == null) return false;

            if(jrubyMethod.compat() == CompatVersion.BOTH ||
                    getRuntime().getInstanceConfig().getCompatVersion() == jrubyMethod.compat()) {
            DynamicMethod dynamicMethod = methodFactory.getAnnotatedMethod(this, desc);
            methodDefiningCallback.define(this, desc, dynamicMethod);

            return true;
        }
        return false;
    }

    public void defineFastMethod(String name, Callback method) {
        Visibility visibility = name.equals("initialize") ?
                PRIVATE : PUBLIC;
        addMethod(name, new SimpleCallbackMethod(this, method, visibility));
    }

    public void defineFastMethod(String name, Callback method, Visibility visibility) {
        addMethod(name, new SimpleCallbackMethod(this, method, visibility));
    }

    public void definePrivateMethod(String name, Callback method) {
        addMethod(name, new FullFunctionCallbackMethod(this, method, PRIVATE));
    }

    public void defineFastPrivateMethod(String name, Callback method) {
        addMethod(name, new SimpleCallbackMethod(this, method, PRIVATE));
    }

    public void defineFastProtectedMethod(String name, Callback method) {
        addMethod(name, new SimpleCallbackMethod(this, method, PROTECTED));
    }

    public void undefineMethod(String name) {
        addMethod(name, UndefinedMethod.getInstance());
    }

    /** rb_undef
     *
     */
    public void undef(ThreadContext context, String name) {
        Ruby runtime = context.getRuntime();
        
        if (this == runtime.getObject()) runtime.secure(4);

        if (runtime.getSafeLevel() >= 4 && !isTaint()) {
            throw new SecurityException("Insecure: can't undef");
        }
        testFrozen("module");
        if (name.equals("__id__") || name.equals("__send__")) {
            runtime.getWarnings().warn(ID.UNDEFINING_BAD, "undefining `"+ name +"' may cause serious problem");
        }
        DynamicMethod method = searchMethod(name);
        if (method.isUndefined()) {
            String s0 = " class";
            RubyModule c = this;

            if (c.isSingleton()) {
                IRubyObject obj = ((MetaClass)c).getAttached();

                if (obj != null && obj instanceof RubyModule) {
                    c = (RubyModule) obj;
                    s0 = "";
                }
            } else if (c.isModule()) {
                s0 = " module";
            }

            throw runtime.newNameError("Undefined method " + name + " for" + s0 + " '" + c.getName() + "'", name);
        }
        addMethod(name, UndefinedMethod.getInstance());
        
        if (isSingleton()) {
            IRubyObject singleton = ((MetaClass)this).getAttached(); 
            singleton.callMethod(context, "singleton_method_undefined", runtime.newSymbol(name));
        } else {
            callMethod(context, "method_undefined", runtime.newSymbol(name));
        }
    }

    @JRubyMethod(name = "include?", required = 1)
    public IRubyObject include_p(ThreadContext context, IRubyObject arg) {
        if (!arg.isModule()) throw context.getRuntime().newTypeError(arg, context.getRuntime().getModule());
        RubyModule moduleToCompare = (RubyModule) arg;

        // See if module is in chain...Cannot match against itself so start at superClass.
        for (RubyModule p = getSuperClass(); p != null; p = p.getSuperClass()) {
            if (p.isSame(moduleToCompare)) return context.getRuntime().getTrue();
        }
        
        return context.getRuntime().getFalse();
    }

    // TODO: Consider a better way of synchronizing 
    public void addMethod(String name, DynamicMethod method) {
        Ruby runtime = getRuntime();
        
        if (this == runtime.getObject()) runtime.secure(4);

        if (runtime.getSafeLevel() >= 4 && !isTaint()) {
            throw runtime.newSecurityError("Insecure: can't define method");
        }
        testFrozen("class/module");

        addMethodInternal(name, method);
    }

    public void addMethodInternal(String name, DynamicMethod method) {
        synchronized(getMethods()) {
            addMethodAtBootTimeOnly(name, method);
            invalidateCacheDescendants();
        }
    }

    /**
     * This method is not intended for use by normal users; it is a fast-path
     * method that skips synchronization and hierarchy invalidation to speed
     * boot-time method definition.
     *
     * @param name The name to which to bind the method
     * @param method The method to bind
     */
    public void addMethodAtBootTimeOnly(String name, DynamicMethod method) {
        getMethods().put(name, method);
    }

    public void removeMethod(ThreadContext context, String name) {
        Ruby runtime = context.getRuntime();
        
        if (this == runtime.getObject()) runtime.secure(4);

        if (runtime.getSafeLevel() >= 4 && !isTaint()) {
            throw runtime.newSecurityError("Insecure: can't remove method");
        }
        testFrozen("class/module");

        // We can safely reference methods here instead of doing getMethods() since if we
        // are adding we are not using a IncludedModuleWrapper.
        synchronized(getMethods()) {
            DynamicMethod method = (DynamicMethod) getMethods().remove(name);
            if (method == null) {
                throw runtime.newNameError("method '" + name + "' not defined in " + getName(), name);
            }

            invalidateCacheDescendants();
        }
        
        if (isSingleton()) {
            IRubyObject singleton = ((MetaClass)this).getAttached(); 
            singleton.callMethod(context, "singleton_method_removed", runtime.newSymbol(name));
        } else {
            callMethod(context, "method_removed", runtime.newSymbol(name));
        }
    }

    /**
     * Search through this module and supermodules for method definitions. Cache superclass definitions in this class.
     * 
     * @param name The name of the method to search for
     * @return The method, or UndefinedMethod if not found
     */    
    public DynamicMethod searchMethod(String name) {
        return searchWithCache(name).method;
    }

    /**
     * Search through this module and supermodules for method definitions. Cache superclass definitions in this class.
     * 
     * @param name The name of the method to search for
     * @return The method, or UndefinedMethod if not found
     */    
    public CacheEntry searchWithCache(String name) {
        CacheEntry entry = cacheHit(name);

        if (entry != null) return entry;

        // we grab serial number first; the worst that will happen is we cache a later
        // update with an earlier serial number, which would just flush anyway
        Object token = getCacheToken();
        DynamicMethod method = searchMethodInner(name);

        if (method instanceof DefaultMethod) {
            method = ((DefaultMethod)method).getMethodForCaching();
        }

        return method != null ? addToCache(name, method, token) : addToCache(name, UndefinedMethod.getInstance(), token);
    }
    
    public final Object getCacheToken() {
        return generation.token;
    }
    
    private CacheEntry cacheHit(String name) {
        CacheEntry cacheEntry = cachedMethods.get(name);

        if (cacheEntry != null) {
            if (cacheEntry.token == getCacheToken()) {
                return cacheEntry;
            }
        }
        
        return null;
    }
    
    private CacheEntry addToCache(String name, DynamicMethod method, Object token) {
        CacheEntry entry = new CacheEntry(method, token);
        cachedMethods.put(name, entry);

        return entry;
    }
    
    protected DynamicMethod searchMethodInner(String name) {
        DynamicMethod method = getMethods().get(name);
        
        if (method != null) return method;
        
        return superClass == null ? null : superClass.searchMethodInner(name);
    }

    public void invalidateCacheDescendants() {
        generation.update();
        // update all hierarchies into which this module has been included
        synchronized (getRuntime().getHierarchyLock()) {
            if (includingHierarchies != null) for (RubyClass includingHierarchy : includingHierarchies) {
                includingHierarchy.invalidateCacheDescendants();
            }
        }
    }
    
    protected void invalidateConstantCache() {
        getRuntime().incrementConstantGeneration();
    }    

    /**
     * Search through this module and supermodules for method definitions. Cache superclass definitions in this class.
     * 
     * @param name The name of the method to search for
     * @return The method, or UndefinedMethod if not found
     */
    public DynamicMethod retrieveMethod(String name) {
        return getMethods().get(name);
    }

    /**
     * Search through this module and supermodules for method definitions. Cache superclass definitions in this class.
     * 
     * @param name The name of the method to search for
     * @return The method, or UndefinedMethod if not found
     */
    public RubyModule findImplementer(RubyModule clazz) {
        for (RubyModule module = this; module != null; module = module.getSuperClass()) {
            if (module.isSame(clazz)) return module;
        }

        return null;
    }

    public void addModuleFunction(String name, DynamicMethod method) {
        addMethod(name, method);
        getSingletonClass().addMethod(name, method);
    }

    /** rb_define_module_function
     *
     */
    public void defineModuleFunction(String name, Callback method) {
        definePrivateMethod(name, method);
        getSingletonClass().defineMethod(name, method);
    }

    /** rb_define_module_function
     *
     */
    public void definePublicModuleFunction(String name, Callback method) {
        defineMethod(name, method);
        getSingletonClass().defineMethod(name, method);
    }

    /** rb_define_module_function
     *
     */
    public void defineFastModuleFunction(String name, Callback method) {
        defineFastPrivateMethod(name, method);
        getSingletonClass().defineFastMethod(name, method);
    }

    /** rb_define_module_function
     *
     */
    public void defineFastPublicModuleFunction(String name, Callback method) {
        defineFastMethod(name, method);
        getSingletonClass().defineFastMethod(name, method);
    }

    /** rb_alias
     *
     */
    public synchronized void defineAlias(String name, String oldName) {
        testFrozen("module");
        if (oldName.equals(name)) {
            return;
        }
        Ruby runtime = getRuntime();
        if (this == runtime.getObject()) {
            runtime.secure(4);
        }

        // JRUBY-2435: Aliasing eval and other "special" methods should display a warning
        // We warn because we treat certain method names as "special" for purposes of
        // optimization. Hopefully this will be enough to convince people not to alias
        // them.
        if (SCOPE_CAPTURING_METHODS.contains(oldName)) {
            runtime.getWarnings().warn("`" + oldName + "' should not be aliased");
        }

        DynamicMethod method = searchMethod(oldName);
        if (method.isUndefined()) {
            if (isModule()) {
                method = runtime.getObject().searchMethod(oldName);
            }

            if (method.isUndefined()) {
                throw runtime.newNameError("undefined method `" + oldName + "' for " +
                        (isModule() ? "module" : "class") + " `" + getName() + "'", oldName);
            }
        }

        invalidateCacheDescendants();
        putMethod(name, new AliasMethod(this, method, oldName));
    }

    public synchronized void defineAliases(List<String> aliases, String oldName) {
        testFrozen("module");
        Ruby runtime = getRuntime();
        if (this == runtime.getObject()) {
            runtime.secure(4);
        }
        DynamicMethod method = searchMethod(oldName);
        if (method.isUndefined()) {
            if (isModule()) {
                method = runtime.getObject().searchMethod(oldName);
            }

            if (method.isUndefined()) {
                throw runtime.newNameError("undefined method `" + oldName + "' for " +
                        (isModule() ? "module" : "class") + " `" + getName() + "'", oldName);
            }
        }

        for (String name: aliases) {
            if (oldName.equals(name)) continue;

            putMethod(name, new AliasMethod(this, method, oldName));
        }
        invalidateCacheDescendants();
    }

    /** this method should be used only by interpreter or compiler 
     * 
     */
    public RubyClass defineOrGetClassUnder(String name, RubyClass superClazz) {
        // This method is intended only for defining new classes in Ruby code,
        // so it uses the allocator of the specified superclass or default to
        // the Object allocator. It should NOT be used to define classes that require a native allocator.

        Ruby runtime = getRuntime();
        IRubyObject classObj = getConstantAt(name);
        RubyClass clazz;

        if (classObj != null) {
            if (!(classObj instanceof RubyClass)) throw runtime.newTypeError(name + " is not a class");
            clazz = (RubyClass)classObj;

            if (superClazz != null) {
                RubyClass tmp = clazz.getSuperClass();
                while (tmp != null && tmp.isIncluded()) tmp = tmp.getSuperClass(); // need to skip IncludedModuleWrappers
                if (tmp != null) tmp = tmp.getRealClass();
                if (tmp != superClazz) throw runtime.newTypeError("superclass mismatch for class " + name);
                // superClazz = null;
            }

            if (runtime.getSafeLevel() >= 4) throw runtime.newTypeError("extending class prohibited");
        } else if (classProviders != null && (clazz = searchProvidersForClass(name, superClazz)) != null) {
            // reopen a java class
        } else {
            if (superClazz == null) superClazz = runtime.getObject();
            clazz = RubyClass.newClass(runtime, superClazz, name, superClazz.getAllocator(), this, true);
        }

        return clazz;
    }

    /** this method should be used only by interpreter or compiler 
     * 
     */
    public RubyModule defineOrGetModuleUnder(String name) {
        // This method is intended only for defining new modules in Ruby code
        Ruby runtime = getRuntime();
        IRubyObject moduleObj = getConstantAt(name);
        RubyModule module;
        if (moduleObj != null) {
            if (!moduleObj.isModule()) throw runtime.newTypeError(name + " is not a module");
            if (runtime.getSafeLevel() >= 4) throw runtime.newSecurityError("extending module prohibited");
            module = (RubyModule)moduleObj;
        } else if (classProviders != null && (module = searchProvidersForModule(name)) != null) {
            // reopen a java module
        } else {
            module = RubyModule.newModule(runtime, name, this, true); 
        }
        return module;
    }

    /** rb_define_class_under
     *  this method should be used only as an API to define/open nested classes 
     */
    public RubyClass defineClassUnder(String name, RubyClass superClass, ObjectAllocator allocator) {
        return getRuntime().defineClassUnder(name, superClass, allocator, this);
    }

    /** rb_define_module_under
     *  this method should be used only as an API to define/open nested module
     */
    public RubyModule defineModuleUnder(String name) {
        return getRuntime().defineModuleUnder(name, this);
    }

    // FIXME: create AttrReaderMethod, AttrWriterMethod, for faster attr access
    private void addAccessor(ThreadContext context, String internedName, Visibility visibility, boolean readable, boolean writeable) {
        assert internedName == internedName.intern() : internedName + " is not interned";

        final Ruby runtime = context.getRuntime();

        if (visibility == PRIVATE) {
            //FIXME warning
        } else if (visibility == MODULE_FUNCTION) {
            visibility = PRIVATE;
            // FIXME warning
        }
        final String variableName = ("@" + internedName).intern();
        if (readable) {
            addMethod(internedName, new JavaMethodZero(this, visibility, CallConfiguration.FrameNoneScopeNone) {
                private RubyClass.VariableAccessor accessor = RubyClass.VariableAccessor.DUMMY_ACCESSOR;
                public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
                    IRubyObject variable = (IRubyObject)verifyAccessor(self.getMetaClass().getRealClass()).get(self);

                    return variable == null ? runtime.getNil() : variable;
                }

                private RubyClass.VariableAccessor verifyAccessor(RubyClass cls) {
                    RubyClass.VariableAccessor localAccessor = accessor;
                    if (localAccessor.getClassId() != cls.hashCode()) {
                        localAccessor = cls.getVariableAccessorForRead(variableName);
                        accessor = localAccessor;
                    }
                    return localAccessor;
                }
            });
            callMethod(context, "method_added", runtime.fastNewSymbol(internedName));
        }
        if (writeable) {
            internedName = (internedName + "=").intern();
            addMethod(internedName, new JavaMethodOne(this, visibility, CallConfiguration.FrameNoneScopeNone) {
                private RubyClass.VariableAccessor accessor = RubyClass.VariableAccessor.DUMMY_ACCESSOR;
                public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg1) {
                    verifyAccessor(self.getMetaClass().getRealClass()).set(self, arg1);
                    return arg1;
                }

                private RubyClass.VariableAccessor verifyAccessor(RubyClass cls) {
                    RubyClass.VariableAccessor localAccessor = accessor;
                    if (localAccessor.getClassId() != cls.hashCode()) {
                        localAccessor = cls.getVariableAccessorForWrite(variableName);
                        accessor = localAccessor;
                    }
                    return localAccessor;
                }
            });
            callMethod(context, "method_added", runtime.fastNewSymbol(internedName));
        }
    }

    /** set_method_visibility
     *
     */
    public void setMethodVisibility(IRubyObject[] methods, Visibility visibility) {
        if (getRuntime().getSafeLevel() >= 4 && !isTaint()) {
            throw getRuntime().newSecurityError("Insecure: can't change method visibility");
        }

        for (int i = 0; i < methods.length; i++) {
            exportMethod(methods[i].asJavaString(), visibility);
        }
    }

    /** rb_export_method
     *
     */
    public void exportMethod(String name, Visibility visibility) {
        if (this == getRuntime().getObject()) {
            getRuntime().secure(4);
        }

        DynamicMethod method = searchMethod(name);

        if (method.isUndefined()) {
            throw getRuntime().newNameError("undefined method '" + name + "' for " +
                                (isModule() ? "module" : "class") + " '" + getName() + "'", name);
        }

        if (method.getVisibility() != visibility) {
            if (this == method.getImplementationClass()) {
                method.setVisibility(visibility);
            } else {
                // FIXME: Why was this using a FullFunctionCallbackMethod before that did callSuper?
                addMethod(name, new WrapperMethod(this, method, visibility));
            }

            invalidateCacheDescendants();
        }
    }

    /**
     * MRI: rb_method_boundp
     *
     */
    public boolean isMethodBound(String name, boolean checkVisibility) {
        DynamicMethod method = searchMethod(name);
        if (!method.isUndefined()) {
            return !(checkVisibility && method.getVisibility() == PRIVATE);
        }
        return false;
    }

    public IRubyObject newMethod(IRubyObject receiver, String name, boolean bound) {
        DynamicMethod method = searchMethod(name);
        if (method.isUndefined()) {
            throw getRuntime().newNameError("undefined method `" + name +
                "' for class `" + this.getName() + "'", name);
        }

        RubyModule implementationModule = method.getImplementationClass();
        RubyModule originModule = this;
        while (originModule != implementationModule && originModule.isSingleton()) {
            originModule = ((MetaClass)originModule).getRealClass();
        }

        RubyMethod newMethod = null;
        if (bound) {
            newMethod = RubyMethod.newMethod(implementationModule, name, originModule, name, method, receiver);
        } else {
            newMethod = RubyUnboundMethod.newUnboundMethod(implementationModule, name, originModule, name, method);
        }
        newMethod.infectBy(this);

        return newMethod;
    }

    @JRubyMethod(name = "define_method", frame = true, visibility = PRIVATE, reads = VISIBILITY)
    public IRubyObject define_method(ThreadContext context, IRubyObject arg0, Block block) {
        Ruby runtime = context.getRuntime();
        String name = arg0.asJavaString().intern();
        DynamicMethod newMethod = null;
        Visibility visibility = context.getCurrentVisibility();

        if (visibility == MODULE_FUNCTION) visibility = PRIVATE;
        RubyProc proc = runtime.newProc(Block.Type.LAMBDA, block);

        // a normal block passed to define_method changes to do arity checking; make it a lambda
        proc.getBlock().type = Block.Type.LAMBDA;
        
        newMethod = createProcMethod(name, visibility, proc);
        
        RuntimeHelpers.addInstanceMethod(this, name, newMethod, context.getPreviousVisibility(), context, runtime);

        return proc;
    }
    
    @JRubyMethod(name = "define_method", frame = true, visibility = PRIVATE, reads = VISIBILITY)
    public IRubyObject define_method(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        Ruby runtime = context.getRuntime();
        IRubyObject body;
        String name = arg0.asJavaString().intern();
        DynamicMethod newMethod = null;
        Visibility visibility = context.getCurrentVisibility();

        if (visibility == MODULE_FUNCTION) visibility = PRIVATE;
        if (runtime.getProc().isInstance(arg1)) {
            // double-testing args.length here, but it avoids duplicating the proc-setup code in two places
            RubyProc proc = (RubyProc)arg1;
            body = proc;

            newMethod = createProcMethod(name, visibility, proc);
        } else if (runtime.getMethod().isInstance(arg1)) {
            RubyMethod method = (RubyMethod)arg1;
            body = method;

            newMethod = new MethodMethod(this, method.unbind(null), visibility);
        } else {
            throw runtime.newTypeError("wrong argument type " + arg1.getType().getName() + " (expected Proc/Method)");
        }
        
        RuntimeHelpers.addInstanceMethod(this, name, newMethod, context.getPreviousVisibility(), context, runtime);

        return body;
    }
    @Deprecated
    public IRubyObject define_method(ThreadContext context, IRubyObject[] args, Block block) {
        switch (args.length) {
        case 1:
            return define_method(context, args[0], block);
        case 2:
            return define_method(context, args[0], args[1], block);
        default:
            throw context.getRuntime().newArgumentError("wrong # of arguments(" + args.length + " for 2)");
        }
    }
    
    private DynamicMethod createProcMethod(String name, Visibility visibility, RubyProc proc) {
        Block block = proc.getBlock();
        block.getBinding().getFrame().setKlazz(this);
        block.getBinding().getFrame().setName(name);
        
        StaticScope scope = block.getBody().getStaticScope();

        // for zsupers in define_method (blech!) we tell the proc scope to act as the "argument" scope
        scope.setArgumentScope(true);

        Arity arity = block.arity();
        // just using required is broken...but no more broken than before zsuper refactoring
        scope.setRequiredArgs(arity.required());

        if(!arity.isFixed()) {
            scope.setRestArg(arity.required());
        }

        return new ProcMethod(this, proc, visibility);
    }

    @Deprecated
    public IRubyObject executeUnder(ThreadContext context, Callback method, IRubyObject[] args, Block block) {
        context.preExecuteUnder(this, block);
        try {
            return method.execute(this, args, block);
        } finally {
            context.postExecuteUnder();
        }
    }

    @JRubyMethod(name = "name")
    public IRubyObject name() {
        Ruby runtime = getRuntime();
        if (getBaseName() == null) {
            return RubyString.newEmptyString(runtime);
        } else {
            return runtime.newString(getName());
        }
    }

    @JRubyMethod(name = "name", compat = CompatVersion.RUBY1_9)
    public IRubyObject name19() {
        Ruby runtime = getRuntime();
        if (getBaseName() == null) {
            return runtime.getNil();
        } else {
            return runtime.newString(getName());
        }
    }

    protected IRubyObject cloneMethods(RubyModule clone) {
        RubyModule realType = this.getNonIncludedClass();
        for (Map.Entry<String, DynamicMethod> entry : getMethods().entrySet()) {
            DynamicMethod method = entry.getValue();
            // Do not clone cached methods
            // FIXME: MRI copies all methods here
            if (method.getImplementationClass() == realType || method.isUndefined()) {
                
                // A cloned method now belongs to a new class.  Set it.
                // TODO: Make DynamicMethod immutable
                DynamicMethod clonedMethod = method.dup();
                clonedMethod.setImplementationClass(clone);
                clone.putMethod(entry.getKey(), clonedMethod);
            }
        }

        return clone;
    }

    /** rb_mod_init_copy
     * 
     */
    @JRubyMethod(name = "initialize_copy", required = 1)
    @Override
    public IRubyObject initialize_copy(IRubyObject original) {
        super.initialize_copy(original);

        RubyModule originalModule = (RubyModule)original;

        if (!getMetaClass().isSingleton()) setMetaClass(originalModule.getSingletonClassClone());
        setSuperClass(originalModule.getSuperClass());
        if (originalModule.hasVariables()) syncVariables(originalModule.getVariableList());
        syncConstants(originalModule);

        originalModule.cloneMethods(this);

        return this;
    }

    public void syncConstants(RubyModule other) {
        if (other.getConstantMap() != DUMMY_CONSTANTS) {
            getConstantMapForWrite().putAll(other.getConstantMap());
        }
    }

    /** rb_mod_included_modules
     *
     */
    @JRubyMethod(name = "included_modules")
    public RubyArray included_modules(ThreadContext context) {
        RubyArray ary = context.getRuntime().newArray();

        for (RubyModule p = getSuperClass(); p != null; p = p.getSuperClass()) {
            if (p.isIncluded()) {
                ary.append(p.getNonIncludedClass());
            }
        }

        return ary;
    }

    /** rb_mod_ancestors
     *
     */
    @JRubyMethod(name = "ancestors")
    public RubyArray ancestors(ThreadContext context) {
        return context.getRuntime().newArray(getAncestorList());
    }
    
    @Deprecated
    public RubyArray ancestors() {
        return getRuntime().newArray(getAncestorList());
    }

    public List<IRubyObject> getAncestorList() {
        ArrayList<IRubyObject> list = new ArrayList<IRubyObject>();

        for (RubyModule module = this; module != null; module = module.getSuperClass()) {
            if(!module.isSingleton()) list.add(module.getNonIncludedClass());
        }

        return list;
    }

    public boolean hasModuleInHierarchy(RubyModule type) {
        // XXX: This check previously used callMethod("==") to check for equality between classes
        // when scanning the hierarchy. However the == check may be safe; we should only ever have
        // one instance bound to a given type/constant. If it's found to be unsafe, examine ways
        // to avoid the == call.
        for (RubyModule module = this; module != null; module = module.getSuperClass()) {
            if (module.getNonIncludedClass() == type) return true;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @JRubyMethod(name = "hash")
    @Override
    public RubyFixnum hash() {
        return getRuntime().newFixnum(id);
    }

    /** rb_mod_to_s
     *
     */
    @JRubyMethod(name = "to_s")
    @Override
    public IRubyObject to_s() {
        if(isSingleton()){            
            IRubyObject attached = ((MetaClass)this).getAttached();
            StringBuilder buffer = new StringBuilder("#<Class:");
            if (attached != null) { // FIXME: figure out why we get null sometimes
                if(attached instanceof RubyClass || attached instanceof RubyModule){
                    buffer.append(attached.inspect());
                }else{
                    buffer.append(attached.anyToString());
                }
            }
            buffer.append(">");
            return getRuntime().newString(buffer.toString());
        }
        return getRuntime().newString(getName());
    }

    /** rb_mod_eqq
     *
     */
    @JRubyMethod(name = "===", required = 1)
    @Override
    public RubyBoolean op_eqq(ThreadContext context, IRubyObject obj) {
        return context.getRuntime().newBoolean(isInstance(obj));
    }

    /**
     * We override equals here to provide a faster path, since equality for modules
     * is pretty cut and dried.
     * @param other The object to check for equality
     * @return true if reference equality, false otherwise
     */
    @Override
    public boolean equals(Object other) {
        return this == other;
    }

    @JRubyMethod(name = "==", required = 1)
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
        return super.op_equal(context, other);
    }

    /** rb_mod_freeze
     *
     */
    @JRubyMethod(name = "freeze")
    @Override
    public final IRubyObject freeze(ThreadContext context) {
        to_s();
        return super.freeze(context);
    }

    /** rb_mod_le
    *
    */
    @JRubyMethod(name = "<=", required = 1)
   public IRubyObject op_le(IRubyObject obj) {
        if (!(obj instanceof RubyModule)) {
            throw getRuntime().newTypeError("compared with non class/module");
        }

        if (isKindOfModule((RubyModule) obj)) return getRuntime().getTrue();
        if (((RubyModule) obj).isKindOfModule(this)) return getRuntime().getFalse();

        return getRuntime().getNil();
    }

    /** rb_mod_lt
    *
    */
    @JRubyMethod(name = "<", required = 1)
   public IRubyObject op_lt(IRubyObject obj) {
        return obj == this ? getRuntime().getFalse() : op_le(obj);
    }

    /** rb_mod_ge
    *
    */
    @JRubyMethod(name = ">=", required = 1)
   public IRubyObject op_ge(IRubyObject obj) {
        if (!(obj instanceof RubyModule)) {
            throw getRuntime().newTypeError("compared with non class/module");
        }

        return ((RubyModule) obj).op_le(this);
    }

    /** rb_mod_gt
    *
    */
    @JRubyMethod(name = ">", required = 1)
   public IRubyObject op_gt(IRubyObject obj) {
        return this == obj ? getRuntime().getFalse() : op_ge(obj);
    }

    /** rb_mod_cmp
    *
    */
    @JRubyMethod(name = "<=>", required = 1)
   public IRubyObject op_cmp(IRubyObject obj) {
        if (this == obj) return getRuntime().newFixnum(0);
        if (!(obj instanceof RubyModule)) return getRuntime().getNil();

        RubyModule module = (RubyModule) obj;

        if (module.isKindOfModule(this)) return getRuntime().newFixnum(1);
        if (this.isKindOfModule(module)) return getRuntime().newFixnum(-1);

        return getRuntime().getNil();
    }

    public boolean isKindOfModule(RubyModule type) {
        for (RubyModule module = this; module != null; module = module.getSuperClass()) {
            if (module.isSame(type)) return true;
        }

        return false;
    }

    protected boolean isSame(RubyModule module) {
        return this == module;
    }

    /** rb_mod_initialize
     *
     */
    @JRubyMethod(name = "initialize", frame = true, visibility = PRIVATE)
    public IRubyObject initialize(Block block) {
        if (block.isGiven()) {
            // class and module bodies default to public, so make the block's visibility public. JRUBY-1185.
            block.getBinding().setVisibility(PUBLIC);
            block.yield(getRuntime().getCurrentContext(), this, this, this, false);
        }

        return getRuntime().getNil();
    }
    
    public void addReadWriteAttribute(ThreadContext context, String name) {
        addAccessor(context, name.intern(), Visibility.PUBLIC, true, true);
    }
    
    public void addReadAttribute(ThreadContext context, String name) {
        addAccessor(context, name.intern(), Visibility.PUBLIC, true, false);
    }
    
    public void addWriteAttribute(ThreadContext context, String name) {
        addAccessor(context, name.intern(), Visibility.PUBLIC, false, true);
    }

    /** rb_mod_attr
     *
     */
    @JRubyMethod(name = "attr", required = 1, optional = 1, visibility = PRIVATE, reads = VISIBILITY, compat = CompatVersion.RUBY1_8)
    public IRubyObject attr(ThreadContext context, IRubyObject[] args) {
        boolean writeable = args.length > 1 ? args[1].isTrue() : false;

        // Check the visibility of the previous frame, which will be the frame in which the class is being eval'ed
        Visibility visibility = context.getCurrentVisibility();
        
        addAccessor(context, args[0].asJavaString().intern(), visibility, true, writeable);

        return getRuntime().getNil();
    }
    
    @JRubyMethod(name = "attr", rest = true, visibility = PRIVATE, reads = VISIBILITY, compat = CompatVersion.RUBY1_9)
    public IRubyObject attr19(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();

        if (args.length == 2 && (args[1] == runtime.getTrue() || args[1] == runtime.getFalse())) {
            runtime.getWarnings().warn(ID.OBSOLETE_ARGUMENT, "optional boolean argument is obsoleted");
            addAccessor(context, args[0].asJavaString().intern(), context.getCurrentVisibility(), args[0].isTrue(), true);
            return runtime.getNil();
        }

        return attr_reader(context, args);
    }

    @Deprecated
    public IRubyObject attr_reader(IRubyObject[] args) {
        return attr_reader(getRuntime().getCurrentContext(), args);
    }
    
    /** rb_mod_attr_reader
     *
     */
    @JRubyMethod(name = "attr_reader", rest = true, visibility = PRIVATE, reads = VISIBILITY)
    public IRubyObject attr_reader(ThreadContext context, IRubyObject[] args) {
        // Check the visibility of the previous frame, which will be the frame in which the class is being eval'ed
        Visibility visibility = context.getCurrentVisibility();

        for (int i = 0; i < args.length; i++) {
            addAccessor(context, args[i].asJavaString().intern(), visibility, true, false);
        }

        return context.getRuntime().getNil();
    }

    /** rb_mod_attr_writer
     *
     */
    @JRubyMethod(name = "attr_writer", rest = true, visibility = PRIVATE, reads = VISIBILITY)
    public IRubyObject attr_writer(ThreadContext context, IRubyObject[] args) {
        // Check the visibility of the previous frame, which will be the frame in which the class is being eval'ed
        Visibility visibility = context.getCurrentVisibility();

        for (int i = 0; i < args.length; i++) {
            addAccessor(context, args[i].asJavaString().intern(), visibility, false, true);
        }

        return context.getRuntime().getNil();
    }


    @Deprecated
    public IRubyObject attr_accessor(IRubyObject[] args) {
        return attr_accessor(getRuntime().getCurrentContext(), args);
    }

    /** rb_mod_attr_accessor
     *
     */
    @JRubyMethod(name = "attr_accessor", rest = true, visibility = PRIVATE, reads = VISIBILITY)
    public IRubyObject attr_accessor(ThreadContext context, IRubyObject[] args) {
        // Check the visibility of the previous frame, which will be the frame in which the class is being eval'ed
        Visibility visibility = context.getCurrentVisibility();

        for (int i = 0; i < args.length; i++) {
            // This is almost always already interned, since it will be called with a symbol in most cases
            // but when created from Java code, we might get an argument that needs to be interned.
            // addAccessor has as a precondition that the string MUST be interned
            addAccessor(context, args[i].asJavaString().intern(), visibility, true, true);
        }

        return context.getRuntime().getNil();
    }

    /**
     * Get a list of all instance methods names of the provided visibility unless not is true, then 
     * get all methods which are not the provided 
     * 
     * @param args passed into one of the Ruby instance_method methods
     * @param visibility to find matching instance methods against
     * @param not if true only find methods not matching supplied visibility
     * @return a RubyArray of instance method names
     */
    private RubyArray instance_methods(IRubyObject[] args, final Visibility visibility, boolean not, boolean useSymbols) {
        boolean includeSuper = args.length > 0 ? args[0].isTrue() : true;
        Ruby runtime = getRuntime();
        RubyArray ary = runtime.newArray();
        Set<String> seen = new HashSet<String>();

        populateInstanceMethodNames(seen, ary, visibility, not, useSymbols, includeSuper);

        return ary;
    }

    public void populateInstanceMethodNames(Set<String> seen, RubyArray ary, final Visibility visibility, boolean not, boolean useSymbols, boolean includeSuper) {
        Ruby runtime = getRuntime();

        for (RubyModule type = this; type != null; type = type.getSuperClass()) {
            RubyModule realType = type.getNonIncludedClass();
            for (Map.Entry entry : type.getMethods().entrySet()) {
                String methodName = (String) entry.getKey();

                if (! seen.contains(methodName)) {
                    seen.add(methodName);

                    DynamicMethod method = (DynamicMethod) entry.getValue();
                    if (method.getImplementationClass() == realType &&
                        (!not && method.getVisibility() == visibility || (not && method.getVisibility() != visibility)) &&
                        ! method.isUndefined()) {

                        ary.append(useSymbols ? runtime.newSymbol(methodName) : runtime.newString(methodName));
                    }
                }
            }

            if (!includeSuper) {
                break;
            }
        }
    }

    @JRubyMethod(name = "instance_methods", optional = 1, compat = CompatVersion.RUBY1_8)
    public RubyArray instance_methods(IRubyObject[] args) {
        return instance_methods(args, PRIVATE, true, false);
    }

    @JRubyMethod(name = "instance_methods", optional = 1, compat = CompatVersion.RUBY1_9)
    public RubyArray instance_methods19(IRubyObject[] args) {
        return instance_methods(args, PRIVATE, true, true);
    }

    @JRubyMethod(name = "public_instance_methods", optional = 1, compat = CompatVersion.RUBY1_8)
    public RubyArray public_instance_methods(IRubyObject[] args) {
        return instance_methods(args, PUBLIC, false, false);
    }

    @JRubyMethod(name = "public_instance_methods", optional = 1, compat = CompatVersion.RUBY1_9)
    public RubyArray public_instance_methods19(IRubyObject[] args) {
        return instance_methods(args, PUBLIC, false, true);
    }

    @JRubyMethod(name = "instance_method", required = 1)
    public IRubyObject instance_method(IRubyObject symbol) {
        return newMethod(null, symbol.asJavaString(), false);
    }

    /** rb_class_protected_instance_methods
     *
     */
    @JRubyMethod(name = "protected_instance_methods", optional = 1, compat = CompatVersion.RUBY1_8)
    public RubyArray protected_instance_methods(IRubyObject[] args) {
        return instance_methods(args, PROTECTED, false, false);
    }

    @JRubyMethod(name = "protected_instance_methods", optional = 1, compat = CompatVersion.RUBY1_9)
    public RubyArray protected_instance_methods19(IRubyObject[] args) {
        return instance_methods(args, PROTECTED, false, true);
    }

    /** rb_class_private_instance_methods
     *
     */
    @JRubyMethod(name = "private_instance_methods", optional = 1, compat = CompatVersion.RUBY1_8)
    public RubyArray private_instance_methods(IRubyObject[] args) {
        return instance_methods(args, PRIVATE, false, false);
    }

    @JRubyMethod(name = "private_instance_methods", optional = 1, compat = CompatVersion.RUBY1_9)
    public RubyArray private_instance_methods19(IRubyObject[] args) {
        return instance_methods(args, PRIVATE, false, true);
    }

    /** rb_mod_append_features
     *
     */
    @JRubyMethod(name = "append_features", required = 1, visibility = PRIVATE)
    public RubyModule append_features(IRubyObject module) {
        if (!(module instanceof RubyModule)) {
            // MRI error message says Class, even though Module is ok 
            throw getRuntime().newTypeError(module,getRuntime().getClassClass());
        }
        ((RubyModule) module).includeModule(this);
        return this;
    }

    /** rb_mod_extend_object
     *
     */
    @JRubyMethod(name = "extend_object", required = 1, visibility = PRIVATE)
    public IRubyObject extend_object(IRubyObject obj) {
        obj.getSingletonClass().includeModule(this);
        return obj;
    }

    /** rb_mod_include
     *
     */
    @JRubyMethod(name = "include", rest = true, visibility = PRIVATE)
    public RubyModule include(IRubyObject[] modules) {
        ThreadContext context = getRuntime().getCurrentContext();
        // MRI checks all types first:
        for (int i = modules.length; --i >= 0; ) {
            IRubyObject obj = modules[i];
            if (!obj.isModule()) throw context.getRuntime().newTypeError(obj, context.getRuntime().getModule());
        }
        for (int i = modules.length - 1; i >= 0; i--) {
            modules[i].callMethod(context, "append_features", this);
            modules[i].callMethod(context, "included", this);
        }

        return this;
    }

    @JRubyMethod(name = "included", required = 1, visibility = PRIVATE)
    public IRubyObject included(ThreadContext context, IRubyObject other) {
        return context.getRuntime().getNil();
    }

    @JRubyMethod(name = "extended", required = 1, frame = true, visibility = PRIVATE)
    public IRubyObject extended(ThreadContext context, IRubyObject other, Block block) {
        return context.getRuntime().getNil();
    }

    private void setVisibility(ThreadContext context, IRubyObject[] args, Visibility visibility) {
        if (context.getRuntime().getSafeLevel() >= 4 && !isTaint()) {
            throw context.getRuntime().newSecurityError("Insecure: can't change method visibility");
        }

        if (args.length == 0) {
            // Note: we change current frames visibility here because the methods which call
            // this method are all "fast" (e.g. they do not created their own frame).
            context.setCurrentVisibility(visibility);
        } else {
            setMethodVisibility(args, visibility);
        }
    }

    /** rb_mod_public
     *
     */
    @JRubyMethod(name = "public", rest = true, visibility = PRIVATE, writes = VISIBILITY)
    public RubyModule rbPublic(ThreadContext context, IRubyObject[] args) {
        setVisibility(context, args, PUBLIC);
        return this;
    }

    /** rb_mod_protected
     *
     */
    @JRubyMethod(name = "protected", rest = true, visibility = PRIVATE, writes = VISIBILITY)
    public RubyModule rbProtected(ThreadContext context, IRubyObject[] args) {
        setVisibility(context, args, PROTECTED);
        return this;
    }

    /** rb_mod_private
     *
     */
    @JRubyMethod(name = "private", rest = true, visibility = PRIVATE, writes = VISIBILITY)
    public RubyModule rbPrivate(ThreadContext context, IRubyObject[] args) {
        setVisibility(context, args, PRIVATE);
        return this;
    }

    /** rb_mod_modfunc
     *
     */
    @JRubyMethod(name = "module_function", rest = true, visibility = PRIVATE, writes = VISIBILITY)
    public RubyModule module_function(ThreadContext context, IRubyObject[] args) {
        if (context.getRuntime().getSafeLevel() >= 4 && !isTaint()) {
            throw context.getRuntime().newSecurityError("Insecure: can't change method visibility");
        }

        if (args.length == 0) {
            context.setCurrentVisibility(MODULE_FUNCTION);
        } else {
            setMethodVisibility(args, PRIVATE);

            for (int i = 0; i < args.length; i++) {
                String name = args[i].asJavaString().intern();
                DynamicMethod method = searchMethod(name);
                assert !method.isUndefined() : "undefined method '" + name + "'";
                getSingletonClass().addMethod(name, new WrapperMethod(getSingletonClass(), method, PUBLIC));
                callMethod(context, "singleton_method_added", context.getRuntime().fastNewSymbol(name));
            }
        }
        return this;
    }

    @JRubyMethod(name = "method_added", required = 1, visibility = PRIVATE)
    public IRubyObject method_added(ThreadContext context, IRubyObject nothing) {
        return context.getRuntime().getNil();
    }

    @JRubyMethod(name = "method_removed", required = 1, visibility = PRIVATE)
    public IRubyObject method_removed(ThreadContext context, IRubyObject nothing) {
        return context.getRuntime().getNil();
    }

    @JRubyMethod(name = "method_undefined", required = 1, visibility = PRIVATE)
    public IRubyObject method_undefined(ThreadContext context, IRubyObject nothing) {
        return context.getRuntime().getNil();
    }
    
    @JRubyMethod(name = "method_defined?", required = 1)
    public RubyBoolean method_defined_p(ThreadContext context, IRubyObject symbol) {
        return isMethodBound(symbol.asJavaString(), true) ? context.getRuntime().getTrue() : context.getRuntime().getFalse();
    }

    @JRubyMethod(name = "public_method_defined?", required = 1)
    public IRubyObject public_method_defined(ThreadContext context, IRubyObject symbol) {
        DynamicMethod method = searchMethod(symbol.asJavaString());
        
        return context.getRuntime().newBoolean(!method.isUndefined() && method.getVisibility() == PUBLIC);
    }

    @JRubyMethod(name = "protected_method_defined?", required = 1)
    public IRubyObject protected_method_defined(ThreadContext context, IRubyObject symbol) {
        DynamicMethod method = searchMethod(symbol.asJavaString());
	    
        return context.getRuntime().newBoolean(!method.isUndefined() && method.getVisibility() == PROTECTED);
    }
	
    @JRubyMethod(name = "private_method_defined?", required = 1)
    public IRubyObject private_method_defined(ThreadContext context, IRubyObject symbol) {
        DynamicMethod method = searchMethod(symbol.asJavaString());
	    
        return context.getRuntime().newBoolean(!method.isUndefined() && method.getVisibility() == PRIVATE);
    }

    @JRubyMethod(name = "public_class_method", rest = true)
    public RubyModule public_class_method(IRubyObject[] args) {
        getMetaClass().setMethodVisibility(args, PUBLIC);
        return this;
    }

    @JRubyMethod(name = "private_class_method", rest = true)
    public RubyModule private_class_method(IRubyObject[] args) {
        getMetaClass().setMethodVisibility(args, PRIVATE);
        return this;
    }

    @JRubyMethod(name = "alias_method", required = 2, visibility = PRIVATE)
    public RubyModule alias_method(ThreadContext context, IRubyObject newId, IRubyObject oldId) {
        String newName = newId.asJavaString();
        defineAlias(newName, oldId.asJavaString());
        RubySymbol newSym = newId instanceof RubySymbol ? (RubySymbol)newId :
            context.getRuntime().newSymbol(newName);
        if (isSingleton()) {
            ((MetaClass)this).getAttached().callMethod(context, "singleton_method_added", newSym);
        } else {
            callMethod(context, "method_added", newSym);
        }
        return this;
    }

    @JRubyMethod(name = "undef_method", required = 1, rest = true, visibility = PRIVATE)
    public RubyModule undef_method(ThreadContext context, IRubyObject[] args) {
        for (int i=0; i<args.length; i++) {
            undef(context, args[i].asJavaString());
        }
        return this;
    }

    @JRubyMethod(name = {"module_eval", "class_eval"}, frame = true)
    public IRubyObject module_eval(ThreadContext context, Block block) {
        return specificEval(context, this, block);
    }
    @JRubyMethod(name = {"module_eval", "class_eval"}, frame = true)
    public IRubyObject module_eval(ThreadContext context, IRubyObject arg0, Block block) {
        return specificEval(context, this, arg0, block);
    }
    @JRubyMethod(name = {"module_eval", "class_eval"}, frame = true)
    public IRubyObject module_eval(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        return specificEval(context, this, arg0, arg1, block);
    }
    @JRubyMethod(name = {"module_eval", "class_eval"}, frame = true)
    public IRubyObject module_eval(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return specificEval(context, this, arg0, arg1, arg2, block);
    }
    @Deprecated
    public IRubyObject module_eval(ThreadContext context, IRubyObject[] args, Block block) {
        return specificEval(context, this, args, block);
    }

    @JRubyMethod(name = "remove_method", required = 1, rest = true, visibility = PRIVATE)
    public RubyModule remove_method(ThreadContext context, IRubyObject[] args) {
        for(int i=0;i<args.length;i++) {
            removeMethod(context, args[i].asJavaString());
        }
        return this;
    }

    public static void marshalTo(RubyModule module, MarshalStream output) throws java.io.IOException {
        output.registerLinkTarget(module);
        output.writeString(MarshalStream.getPathFromClass(module));
    }

    public static RubyModule unmarshalFrom(UnmarshalStream input) throws java.io.IOException {
        String name = RubyString.byteListToString(input.unmarshalString());
        RubyModule result = UnmarshalStream.getModuleFromPath(input.getRuntime(), name);
        input.registerLinkTarget(result);
        return result;
    }

    /* Module class methods */
    
    /** 
     * Return an array of nested modules or classes.
     */
    @JRubyMethod(name = "nesting", frame = true, meta = true)
    public static RubyArray nesting(ThreadContext context, IRubyObject recv, Block block) {
        Ruby runtime = context.getRuntime();
        RubyModule object = runtime.getObject();
        StaticScope scope = context.getCurrentScope().getStaticScope();
        RubyArray result = runtime.newArray();
        
        for (StaticScope current = scope; current.getModule() != object; current = current.getPreviousCRefScope()) {
            result.append(current.getModule());
        }
        
        return result;
    }

    /**
     * Include the given module and all related modules into the hierarchy above
     * this module/class. Inspects the hierarchy to ensure the same module isn't
     * included twice, and selects an appropriate insertion point for each incoming
     * module.
     * 
     * @param baseModule The module to include, along with any modules it itself includes
     */
    private void doIncludeModule(RubyModule baseModule) {
        List<RubyModule> modulesToInclude = gatherModules(baseModule);
        
        RubyModule currentInclusionPoint = this;
        ModuleLoop: for (RubyModule nextModule : modulesToInclude) {
            checkForCyclicInclude(nextModule);

            boolean superclassSeen = false;

            // scan class hierarchy for module
            for (RubyClass nextClass = this.getSuperClass(); nextClass != null; nextClass = nextClass.getSuperClass()) {
                if (doesTheClassWrapTheModule(nextClass, nextModule)) {
                    // next in hierarchy is an included version of the module we're attempting,
                    // so we skip including it
                    
                    // if we haven't encountered a real superclass, use the found module as the new inclusion point
                    if (!superclassSeen) currentInclusionPoint = nextClass;
                    
                    continue ModuleLoop;
                } else {
                    superclassSeen = true;
                }
            }

            currentInclusionPoint = proceedWithInclude(currentInclusionPoint, nextModule);
        }
    }
    
    /**
     * Is the given class a wrapper for the specified module?
     * 
     * @param theClass The class to inspect
     * @param theModule The module we're looking for
     * @return true if the class is a wrapper for the module, false otherwise
     */
    private boolean doesTheClassWrapTheModule(RubyClass theClass, RubyModule theModule) {
        return theClass.isIncluded() &&
                theClass.getNonIncludedClass() == theModule.getNonIncludedClass();
    }

    /**
     * Gather all modules that would be included by including the given module.
     * The resulting list contains the given module and its (zero or more)
     * module-wrapping superclasses.
     * 
     * @param baseModule The base module from which to aggregate modules
     * @return A list of all modules that would be included by including the given module
     */
    private List<RubyModule> gatherModules(RubyModule baseModule) {
        // build a list of all modules to consider for inclusion
        List<RubyModule> modulesToInclude = new ArrayList<RubyModule>();
        while (baseModule != null) {
            modulesToInclude.add(baseModule);
            baseModule = baseModule.getSuperClass();
        }

        return modulesToInclude;
    }

    /**
     * Actually proceed with including the specified module above the given target
     * in a hierarchy. Return the new module wrapper.
     * 
     * @param insertAbove The hierarchy target above which to include the wrapped module
     * @param moduleToInclude The module to wrap and include
     * @return The new module wrapper resulting from this include
     */
    private RubyModule proceedWithInclude(RubyModule insertAbove, RubyModule moduleToInclude) {
        // In the current logic, if we get here we know that module is not an
        // IncludedModuleWrapper, so there's no need to fish out the delegate. But just
        // in case the logic should change later, let's do it anyway
        RubyClass wrapper = new IncludedModuleWrapper(getRuntime(), insertAbove.getSuperClass(), moduleToInclude.getNonIncludedClass());
        
        // if the insertion point is a class, update subclass lists
        if (insertAbove instanceof RubyClass) {
            RubyClass insertAboveClass = (RubyClass)insertAbove;
            
            // if there's a non-null superclass, we're including into a normal class hierarchy;
            // update subclass relationships to avoid stale parent/child relationships
            if (insertAboveClass.getSuperClass() != null) {
                insertAboveClass.getSuperClass().replaceSubclass(insertAboveClass, wrapper);
            }
            
            wrapper.addSubclass(insertAboveClass);
        }
        
        insertAbove.setSuperClass(wrapper);
        insertAbove = insertAbove.getSuperClass();
        return insertAbove;
    }


    //
    ////////////////// CLASS VARIABLE RUBY METHODS ////////////////
    //

    @JRubyMethod(name = "class_variable_defined?", required = 1)
    public IRubyObject class_variable_defined_p(ThreadContext context, IRubyObject var) {
        String internedName = validateClassVariable(var.asJavaString().intern());
        RubyModule module = this;
        do {
            if (module.fastHasClassVariable(internedName)) {
                return context.getRuntime().getTrue();
            }
        } while ((module = module.getSuperClass()) != null);

        return context.getRuntime().getFalse();
    }

    /** rb_mod_cvar_get
     *
     */
    @JRubyMethod(name = "class_variable_get", required = 1, visibility = PRIVATE)
    public IRubyObject class_variable_get(IRubyObject var) {
        return fastGetClassVar(validateClassVariable(var.asJavaString()).intern());
    }

    /** rb_mod_cvar_set
     *
     */
    @JRubyMethod(name = "class_variable_set", required = 2, visibility = PRIVATE)
    public IRubyObject class_variable_set(IRubyObject var, IRubyObject value) {
        return fastSetClassVar(validateClassVariable(var.asJavaString()).intern(), value);
    }

    /** rb_mod_remove_cvar
     *
     */
    @JRubyMethod(name = "remove_class_variable", required = 1, visibility = PRIVATE)
    public IRubyObject remove_class_variable(ThreadContext context, IRubyObject name) {
        String javaName = validateClassVariable(name.asJavaString());
        IRubyObject value;

        if ((value = deleteClassVariable(javaName)) != null) {
            return value;
        }

        if (fastIsClassVarDefined(javaName)) {
            throw cannotRemoveError(javaName);
        }

        throw context.getRuntime().newNameError("class variable " + javaName + " not defined for " + getName(), javaName);
    }

    /** rb_mod_class_variables
     *
     */
    @JRubyMethod(name = "class_variables")
    public RubyArray class_variables(ThreadContext context) {
        Set<String> names = new HashSet<String>();

        for (RubyModule p = this; p != null; p = p.getSuperClass()) {
            names.addAll(p.getClassVariableNameList());
        }

        RubyArray ary = context.getRuntime().newArray();

        ary.addAll(names);

        return ary;
    }


    //
    ////////////////// CONSTANT RUBY METHODS ////////////////
    //

    /** rb_mod_const_defined
     *
     */
    @JRubyMethod(name = "const_defined?", required = 1)
    public RubyBoolean const_defined_p(ThreadContext context, IRubyObject symbol) {
        // Note: includes part of fix for JRUBY-1339
        return context.getRuntime().newBoolean(fastIsConstantDefined(validateConstant(symbol.asJavaString()).intern()));
    }

    /** rb_mod_const_get
     *
     */
    @JRubyMethod(name = "const_get", required = 1)
    public IRubyObject const_get(IRubyObject symbol) {
        return getConstant(validateConstant(symbol.asJavaString()));
    }

    /** rb_mod_const_set
     *
     */
    @JRubyMethod(name = "const_set", required = 2)
    public IRubyObject const_set(IRubyObject symbol, IRubyObject value) {
        return fastSetConstant(validateConstant(symbol.asJavaString()).intern(), value);
    }

    @JRubyMethod(name = "remove_const", required = 1, visibility = PRIVATE)
    public IRubyObject remove_const(ThreadContext context, IRubyObject rubyName) {
        String name = validateConstant(rubyName.asJavaString());
        IRubyObject value;
        if ((value = deleteConstant(name)) != null) {
            invalidateConstantCache();
            if (value != UNDEF) {
                return value;
            }
            context.getRuntime().getLoadService().removeAutoLoadFor(getName() + "::" + name);
            // FIXME: I'm not sure this is right, but the old code returned
            // the undef, which definitely isn't right...
            return context.getRuntime().getNil();
        }

        if (hasConstantInHierarchy(name)) {
            throw cannotRemoveError(name);
        }

        throw context.getRuntime().newNameError("constant " + name + " not defined for " + getName(), name);
    }

    private boolean hasConstantInHierarchy(final String name) {
        for (RubyModule p = this; p != null; p = p.getSuperClass()) {
            if (p.hasConstant(name)) {
                return true;
        }
        }
        return false;
    }
    
    /**
     * Base implementation of Module#const_missing, throws NameError for specific missing constant.
     * 
     * @param name The constant name which was found to be missing
     * @return Nothing! Absolutely nothing! (though subclasses might choose to return something)
     */
    @JRubyMethod(name = "const_missing", required = 1, frame = true)
    public IRubyObject const_missing(ThreadContext context, IRubyObject rubyName, Block block) {
        Ruby runtime = context.getRuntime();
        String name;
        
        if (this != runtime.getObject()) {
            name = getName() + "::" + rubyName.asJavaString();
        } else {
            name = rubyName.asJavaString();
        }

        throw runtime.newNameError("uninitialized constant " + name, name);
    }

    @JRubyMethod(name = "constants", compat = CompatVersion.RUBY1_8)
    public RubyArray constants(ThreadContext context) {
        return constantsCommon(context, true);
    }

    @JRubyMethod(name = "constants", compat = CompatVersion.RUBY1_9)
    public RubyArray constants19(ThreadContext context) {
        return constantsCommon(context, true);
    }

    @JRubyMethod(name = "constants", compat = CompatVersion.RUBY1_9)
    public RubyArray constants19(ThreadContext context, IRubyObject allConstants) {
        return constantsCommon(context, allConstants.isTrue());
    }

    /** rb_mod_constants
     *
     */
    public RubyArray constantsCommon(ThreadContext context, boolean allConstants) {
        Ruby runtime = context.getRuntime();
        RubyArray array = runtime.newArray();
        RubyModule objectClass = runtime.getObject();

        if (allConstants) {
            if (getRuntime().getModule() == this || objectClass == this) {
                array.addAll(objectClass.getConstantNames());
            } else {
                Set<String> names = new HashSet<String>();
                for (RubyModule module = this; module != null && module != objectClass; module = module.getSuperClass()) {
                    names.addAll(module.getConstantNames());
                }
                array.addAll(names);
            }
        } else {
            if (getRuntime().getModule() == this || objectClass == this) {
                array.addAll(objectClass.getConstantNames());
            } else {
                array.addAll(getConstantNames());
            }
        }

        return array;
    }


    //
    ////////////////// CLASS VARIABLE API METHODS ////////////////
    //

    /**
     * Set the named class variable to the given value, provided taint and freeze allow setting it.
     * 
     * Ruby C equivalent = "rb_cvar_set"
     * 
     * @param name The variable name to set
     * @param value The value to set it to
     */
    public IRubyObject setClassVar(String name, IRubyObject value) {
        RubyModule module = this;
        do {
            if (module.hasClassVariable(name)) {
                return module.storeClassVariable(name, value);
            }
        } while ((module = module.getSuperClass()) != null);
        
        return storeClassVariable(name, value);
    }

    public IRubyObject fastSetClassVar(final String internedName, final IRubyObject value) {
        assert internedName == internedName.intern() : internedName + " is not interned";
        RubyModule module = this;
        do {
            if (module.fastHasClassVariable(internedName)) {
                return module.fastStoreClassVariable(internedName, value);
            }
        } while ((module = module.getSuperClass()) != null);
        
        return fastStoreClassVariable(internedName, value);
    }

    /**
     * Retrieve the specified class variable, searching through this module, included modules, and supermodules.
     * 
     * Ruby C equivalent = "rb_cvar_get"
     * 
     * @param name The name of the variable to retrieve
     * @return The variable's value, or throws NameError if not found
     */
    public IRubyObject getClassVar(String name) {
        assert IdUtil.isClassVariable(name);
        Object value;
        RubyModule module = this;

        do {
            if ((value = module.fetchClassVariable(name)) != null) return (IRubyObject)value;
        } while ((module = module.getSuperClass()) != null);

        throw getRuntime().newNameError("uninitialized class variable " + name + " in " + getName(), name);
    }

    public IRubyObject fastGetClassVar(String internedName) {
        assert internedName == internedName.intern() : internedName + " is not interned";
        assert IdUtil.isClassVariable(internedName);
        IRubyObject value;
        RubyModule module = this;
        
        do {
            if ((value = module.fetchClassVariable(internedName)) != null) return value;
        } while ((module = module.getSuperClass()) != null);

        throw getRuntime().newNameError("uninitialized class variable " + internedName + " in " + getName(), internedName);
    }

    /**
     * Is class var defined?
     * 
     * Ruby C equivalent = "rb_cvar_defined"
     * 
     * @param name The class var to determine "is defined?"
     * @return true if true, false if false
     */
    public boolean isClassVarDefined(String name) {
        RubyModule module = this;
        do {
            if (module.hasClassVariable(name)) return true;
        } while ((module = module.getSuperClass()) != null);

        return false;
    }

    public boolean fastIsClassVarDefined(String internedName) {
        assert internedName == internedName.intern() : internedName + " is not interned";
        RubyModule module = this;
        do {
            if (module.fastHasClassVariable(internedName)) return true;
        } while ((module = module.getSuperClass()) != null);

        return false;
    }

    
    /** rb_mod_remove_cvar
     *
     * FIXME: any good reason to have two identical methods? (same as remove_class_variable)
     */
    public IRubyObject removeCvar(IRubyObject name) { // Wrong Parameter ?
        String internedName = validateClassVariable(name.asJavaString());
        IRubyObject value;

        if ((value = deleteClassVariable(internedName)) != null) {
            return value;
        }

        if (fastIsClassVarDefined(internedName)) {
            throw cannotRemoveError(internedName);
        }

        throw getRuntime().newNameError("class variable " + internedName + " not defined for " + getName(), internedName);
    }


    //
    ////////////////// CONSTANT API METHODS ////////////////
    //

    public IRubyObject getConstantAt(String name) {
        IRubyObject value = fetchConstant(name);
        
        return value == UNDEF ? resolveUndefConstant(getRuntime(), name) : value;
    }

    public IRubyObject fastGetConstantAt(String internedName) {
        assert internedName == internedName.intern() : internedName + " is not interned";
        IRubyObject value = fastFetchConstant(internedName);

        return value == UNDEF ? resolveUndefConstant(getRuntime(), internedName) : value;
    }

    /**
     * Retrieve the named constant, invoking 'const_missing' should that be appropriate.
     * 
     * @param name The constant to retrieve
     * @return The value for the constant, or null if not found
     */
    public IRubyObject getConstant(String name) {
        return fastGetConstant(name.intern());
    }
    
    public IRubyObject fastGetConstant(String internedName) {
        IRubyObject value = getConstantNoConstMissing(internedName);
        Ruby runtime = getRuntime();
        
        return value == null ? callMethod(runtime.getCurrentContext(), "const_missing",
                runtime.fastNewSymbol(internedName)) : value;
    }

    public IRubyObject getConstantNoConstMissing(String name) {
        assert IdUtil.isConstant(name);

        for (RubyModule p = this; p != null; p = p.getSuperClass()) {
            IRubyObject value = p.getConstantInner(name);

            if (value != null) return value == UNDEF ? null : value;
        }

        if (!isClass()) {
            for (RubyModule p = getRuntime().getObject(); p != null; p = p.getSuperClass()) {
                IRubyObject value = p.getConstantInner(name);

                if (value != null) return value == UNDEF ? null : value;
            }
        }

        return null;
    }
    
    protected IRubyObject getConstantInner(String name) {
        IRubyObject value = constantTableFetch(name);

        for (; value == UNDEF; value = constantTableFetch(name)) {
            if (resolveUndefConstant(getRuntime(), name) == null) return UNDEF;
        }
        
        return value;
    }

    // not actually called anywhere (all known uses call the fast version)
    public IRubyObject getConstantFrom(String name) {
        return fastGetConstantFrom(name.intern());
    }
    
    public IRubyObject fastGetConstantFrom(String internedName) {
        IRubyObject value = fastGetConstantFromNoConstMissing(internedName);

        return value != null ? value : fastGetConstantFromConstMissing(internedName);
    }

    public IRubyObject fastGetConstantFromNoConstMissing(String internedName) {
        assert internedName == internedName.intern() : internedName + " is not interned";
        assert IdUtil.isConstant(internedName);
        Ruby runtime = getRuntime();
        RubyClass objectClass = runtime.getObject();
        IRubyObject value;

        RubyModule p = this;

        while (p != null) {
            if ((value = p.constantTableFastFetch(internedName)) != null) {
                if (value == UNDEF) {
                    if (p.resolveUndefConstant(runtime, internedName) == null) break;
                    continue; // Not that is loaded loop around to resolve it next pass
                }

                if (p == objectClass && this != objectClass) {
                    String badCName = getName() + "::" + internedName;
                    runtime.getWarnings().warn(ID.CONSTANT_BAD_REFERENCE, "toplevel constant " +
                            internedName + " referenced by " + badCName, badCName);
                }

                return value;
            }
            p = p.getSuperClass();
        }
        return null;
    }

    public IRubyObject fastGetConstantFromConstMissing(String internedName) {
        assert internedName == internedName.intern() : internedName + " is not interned";
        assert IdUtil.isConstant(internedName);

        return callMethod(getRuntime().getCurrentContext(),
                "const_missing", getRuntime().fastNewSymbol(internedName));
    }
    
    public IRubyObject resolveUndefConstant(Ruby runtime, String name) {
        deleteConstant(name);
        
        return runtime.getLoadService().autoload(getName() + "::" + name);
    }
    
    /**
     * Set the named constant on this module. Also, if the value provided is another Module and
     * that module has not yet been named, assign it the specified name.
     * 
     * @param name The name to assign
     * @param value The value to assign to it; if an unnamed Module, also set its basename to name
     * @return The result of setting the variable.
     */
    public IRubyObject setConstant(String name, IRubyObject value) {
        IRubyObject oldValue = fetchConstant(name);
        if (oldValue != null) {
            Ruby runtime = getRuntime();
            if (oldValue == UNDEF) {
                runtime.getLoadService().removeAutoLoadFor(getName() + "::" + name);
            } else {
                runtime.getWarnings().warn(ID.CONSTANT_ALREADY_INITIALIZED, "already initialized constant " + name, name);
            }
        }

        storeConstant(name, value);
        invalidateConstantCache();

        // if adding a module under a constant name, set that module's basename to the constant name
        if (value instanceof RubyModule) {
            RubyModule module = (RubyModule)value;
            if (module.getBaseName() == null) {
                module.setBaseName(name);
                module.setParent(this);
            }
        }
        return value;
    }

    public IRubyObject fastSetConstant(String internedName, IRubyObject value) {
        return setConstant(internedName, value);
    }
    
    /** rb_define_const
     *
     */
    public void defineConstant(String name, IRubyObject value) {
        assert value != null;

        if (this == getRuntime().getClassClass()) {
            getRuntime().secure(4);
        }

        if (!IdUtil.isValidConstantName(name)) {
            throw getRuntime().newNameError("bad constant name " + name, name);
        }

        setConstant(name, value);
    }

    // Fix for JRUBY-1339 - search hierarchy for constant
    /** rb_const_defined_at
     * 
     */
    public boolean isConstantDefined(String name) {
        assert IdUtil.isConstant(name);
        boolean isObject = this == getRuntime().getObject();

        RubyModule module = this;

        do {
            Object value;
            if ((value = module.constantTableFetch(name)) != null) {
                if (value != UNDEF) return true;
                return getRuntime().getLoadService().autoloadFor(
                        module.getName() + "::" + name) != null;
            }

        } while (isObject && (module = module.getSuperClass()) != null );

        return false;
    }

    public boolean fastIsConstantDefined(String internedName) {
        assert internedName == internedName.intern() : internedName + " is not interned";
        assert IdUtil.isConstant(internedName);
        boolean isObject = this == getRuntime().getObject();

        RubyModule module = this;

        do {
            Object value;
            if ((value = module.constantTableFastFetch(internedName)) != null) {
                if (value != UNDEF) return true;
                return getRuntime().getLoadService().autoloadFor(
                        module.getName() + "::" + internedName) != null;
            }

        } while (isObject && (module = module.getSuperClass()) != null );

        return false;
    }

    //
    ////////////////// COMMON CONSTANT / CVAR METHODS ////////////////
    //

    private RaiseException cannotRemoveError(String id) {
        return getRuntime().newNameError("cannot remove " + id + " for " + getName(), id);
    }


    //
    ////////////////// INTERNAL MODULE VARIABLE API METHODS ////////////////
    //
    
    /**
     * Behaves similarly to {@link #getClassVar(String)}. Searches this
     * class/module <em>and its ancestors</em> for the specified internal
     * variable.
     * 
     * @param name the internal variable name
     * @return the value of the specified internal variable if found, else null
     * @see #setInternalModuleVariable(String, IRubyObject)
     */
    public boolean hasInternalModuleVariable(final String name) {
        for (RubyModule module = this; module != null; module = module.getSuperClass()) {
            if (module.hasInternalVariable(name)) return true;
        }

        return false;
    }
    /**
     * Behaves similarly to {@link #getClassVar(String)}. Searches this
     * class/module <em>and its ancestors</em> for the specified internal
     * variable.
     * 
     * @param name the internal variable name
     * @return the value of the specified internal variable if found, else null
     * @see #setInternalModuleVariable(String, IRubyObject)
     */
    public IRubyObject searchInternalModuleVariable(final String name) {
        for (RubyModule module = this; module != null; module = module.getSuperClass()) {
            IRubyObject value = (IRubyObject)module.getInternalVariable(name);
            if (value != null) return value;
        }

        return null;
    }

    /**
     * Behaves similarly to {@link #setClassVar(String, IRubyObject)}. If the
     * specified internal variable is found in this class/module <em>or an ancestor</em>,
     * it is set where found.  Otherwise it is set in this module. 
     * 
     * @param name the internal variable name
     * @param value the internal variable value
     * @see #searchInternalModuleVariable(String)
     */
    public void setInternalModuleVariable(final String name, final IRubyObject value) {
        for (RubyModule module = this; module != null; module = module.getSuperClass()) {
            if (module.hasInternalVariable(name)) {
                module.setInternalVariable(name, value);
                return;
            }
        }

        setInternalVariable(name, value);
    }

    //
    ////////////////// LOW-LEVEL CLASS VARIABLE INTERFACE ////////////////
    //
    // fetch/store/list class variables for this module
    //

    private volatile Map<String, IRubyObject> classVariables;

    protected synchronized Map<String, IRubyObject> getClassVariables() {
        if (classVariables == null) {
            classVariables = new Hashtable<String, IRubyObject>(4);
        }
        return classVariables;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, IRubyObject> getClassVariablesForRead() {
        return classVariables == null ? Collections.EMPTY_MAP : classVariables;
    }
    
    public boolean hasClassVariable(String name) {
        assert IdUtil.isClassVariable(name);
        return getClassVariablesForRead().containsKey(name);
    }

    public boolean fastHasClassVariable(String internedName) {
        return hasClassVariable(internedName);
    }

    public IRubyObject fetchClassVariable(String name) {
        assert IdUtil.isClassVariable(name);
        return getClassVariablesForRead().get(name);
    }

    public IRubyObject fastFetchClassVariable(String internedName) {
        return fetchClassVariable(internedName);
    }

    public IRubyObject storeClassVariable(String name, IRubyObject value) {
        assert IdUtil.isClassVariable(name) && value != null;
        ensureClassVariablesSettable();
        getClassVariables().put(name, value);
        return value;
    }

    public IRubyObject fastStoreClassVariable(String internedName, IRubyObject value) {
        return storeClassVariable(internedName, value);
    }

    public IRubyObject deleteClassVariable(String name) {
        assert IdUtil.isClassVariable(name);
        ensureClassVariablesSettable();
        return getClassVariablesForRead().remove(name);
    }

    public List<String> getClassVariableNameList() {
        return new ArrayList<String>(getClassVariablesForRead().keySet());
    }

    protected static final String ERR_INSECURE_SET_CLASS_VAR = "Insecure: can't modify class variable";
    protected static final String ERR_FROZEN_CVAR_TYPE = "class/module ";
   
    protected final String validateClassVariable(String name) {
        if (IdUtil.isValidClassVariableName(name)) {
            return name;
        }
        throw getRuntime().newNameError("`" + name + "' is not allowed as a class variable name", name);
    }

    protected final void ensureClassVariablesSettable() {
        Ruby runtime = getRuntime();
        
        if (!isFrozen() && (runtime.getSafeLevel() < 4 || isTaint())) {
            return;
        }
        
        if (runtime.getSafeLevel() >= 4 && !isTaint()) {
            throw runtime.newSecurityError(ERR_INSECURE_SET_CONSTANT);
        }
        if (isFrozen()) {
            if (this instanceof RubyModule) {
                throw runtime.newFrozenError(ERR_FROZEN_CONST_TYPE);
            } else {
                throw runtime.newFrozenError("");
            }
        }
    }

    //
    ////////////////// LOW-LEVEL CONSTANT INTERFACE ////////////////
    //
    // fetch/store/list constants for this module
    //

    public boolean hasConstant(String name) {
        assert IdUtil.isConstant(name);
        return constantTableContains(name);
    }

    public boolean fastHasConstant(String internedName) {
        assert IdUtil.isConstant(internedName);
        return constantTableFastContains(internedName);
    }

    // returns the stored value without processing undefs (autoloads)
    public IRubyObject fetchConstant(String name) {
        assert IdUtil.isConstant(name);
        return constantTableFetch(name);
    }

    // returns the stored value without processing undefs (autoloads)
    public IRubyObject fastFetchConstant(String internedName) {
        assert IdUtil.isConstant(internedName);
        return constantTableFastFetch(internedName);
    }

    public IRubyObject storeConstant(String name, IRubyObject value) {
        assert IdUtil.isConstant(name) && value != null;
        ensureConstantsSettable();
        return constantTableStore(name, value);
    }

    public IRubyObject fastStoreConstant(String internedName, IRubyObject value) {
        assert IdUtil.isConstant(internedName) && value != null;
        ensureConstantsSettable();
        return constantTableFastStore(internedName, value);
    }

    // removes and returns the stored value without processing undefs (autoloads)
    public IRubyObject deleteConstant(String name) {
        assert IdUtil.isConstant(name);
        ensureConstantsSettable();
        return constantTableRemove(name);
    }


    @Deprecated
    public List<Variable<IRubyObject>> getStoredConstantList() {
        return null;
    }

    @Deprecated
    public List<String> getStoredConstantNameList() {
        return new ArrayList<String>(getConstantMap().keySet());
    }

    /**
     * @return a list of constant names that exists at time this was called
     */
    public Collection<String> getConstantNames() {
        return getConstantMap().keySet();
    }

    protected static final String ERR_INSECURE_SET_CONSTANT  = "Insecure: can't modify constant";
    protected static final String ERR_FROZEN_CONST_TYPE = "class/module ";
   
    protected final String validateConstant(String name) {
        if (IdUtil.isValidConstantName(name)) {
            return name;
        }
        throw getRuntime().newNameError("wrong constant name " + name, name);
    }

    protected final void ensureConstantsSettable() {
        boolean isSecure = getRuntime().getSafeLevel() >= 4 && !isTaint();

        if (isSecure) throw getRuntime().newSecurityError(ERR_INSECURE_SET_CONSTANT);
        if (isFrozen()) throw getRuntime().newFrozenError(ERR_FROZEN_CONST_TYPE);
    }

    protected boolean constantTableContains(String name) {
        return getConstantMap().containsKey(name);
    }
    
    protected boolean constantTableFastContains(String internedName) {
        return getConstantMap().containsKey(internedName);
    }
    
    protected IRubyObject constantTableFetch(String name) {
        return getConstantMap().get(name);
    }
    
    protected IRubyObject constantTableFastFetch(String internedName) {
        return getConstantMap().get(internedName);
    }
    
    protected IRubyObject constantTableStore(String name, IRubyObject value) {
        getConstantMapForWrite().put(name, value);
        return value;
    }
    
    protected IRubyObject constantTableFastStore(String internedName, IRubyObject value) {
        getConstantMapForWrite().put(internedName, value);
        return value;
    }
        
    protected IRubyObject constantTableRemove(String name) {
        return getConstantMapForWrite().remove(name);
    }
}
