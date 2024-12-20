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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004-2005 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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

package org.jruby;

import static org.jruby.api.Access.basicObjectClass;
import static org.jruby.api.Access.classClass;
import static org.jruby.api.Access.instanceConfig;
import static org.jruby.api.Access.objectClass;
import static org.jruby.api.Convert.asSymbol;
import static org.jruby.api.Error.*;
import static org.jruby.api.Warn.warn;
import static org.jruby.runtime.ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR;
import static org.jruby.runtime.Visibility.PRIVATE;
import static org.jruby.runtime.Visibility.PUBLIC;
import static org.jruby.util.CodegenUtils.ci;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;
import static org.objectweb.asm.Opcodes.ACC_BRIDGE;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ACC_VARARGS;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.stream.Collectors;

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.api.Create;
import org.jruby.api.JRubyAPI;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.java.codegen.MultiClassLoader;
import org.jruby.java.codegen.RealClassGenerator;
import org.jruby.java.codegen.Reified;
import org.jruby.java.proxies.ConcreteJavaProxy;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.Java.JCtorCache;
import org.jruby.javasupport.JavaConstructor;
import org.jruby.javasupport.JavaUtil;
import org.jruby.javasupport.proxy.JavaProxyClass;
import org.jruby.javasupport.proxy.ReifiedJavaProxy;
import org.jruby.javasupport.util.JavaClassConfiguration;
import org.jruby.lexer.yacc.SimpleSourcePosition;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.callsite.RespondToCallSite;
import org.jruby.runtime.ivars.VariableAccessor;
import org.jruby.runtime.ivars.VariableAccessorField;
import org.jruby.runtime.ivars.VariableTableManager;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.NewMarshal;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.runtime.opto.Invalidator;
import org.jruby.util.ArraySupport;
import org.jruby.util.ClassDefiningClassLoader;
import org.jruby.util.CodegenUtils;
import org.jruby.util.JavaNameMangler;
import org.jruby.util.Loader;
import org.jruby.util.OneShotClassLoader;
import org.jruby.util.StringSupport;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.commons.GeneratorAdapter;


/**
 *
 * @author  jpetersen
 */
@JRubyClass(name="Class", parent="Module")
public class RubyClass extends RubyModule {

    private static final Logger LOG = LoggerFactory.getLogger(RubyClass.class);
    private static final double SUBCLASSES_CLEAN_FACTOR = 0.25;

    public static void finishClassClass(Ruby runtime, RubyClass Class) {
        Class.reifiedClass(RubyClass.class).
                kindOf(new RubyModule.JavaClassKindOf(RubyClass.class)).
                classIndex(ClassIndex.CLASS);
    }

    public static void finishCreateClassClass(ThreadContext context, RubyClass Class) {
        Class.defineMethods(context, RubyClass.class).
                undefMethods(context, "module_function", "append_features", "prepend_features", "extend_object", "refine");

        context.runtime.setBaseNewMethod(Class.searchMethod("new"));
    }

    public static final ObjectAllocator CLASS_ALLOCATOR = (runtime, klass) -> {
        RubyClass clazz = new RubyClass(runtime);
        clazz.allocator = NOT_ALLOCATABLE_ALLOCATOR; // Class.allocate object is not allocatable before it is initialized
        return clazz;
    };

    public ObjectAllocator getAllocator() {
        return allocator;
    }

    /**
     * @param allocator
     * @deprecated Use {@link org.jruby.RubyClass#allocator(ObjectAllocator)} instead.
     */
    @Deprecated(since = "10.0")
    public void setAllocator(ObjectAllocator allocator) {
        allocator(allocator);
    }

    /**
     * Define an allocator for this class (usually this is part of
     * {@link org.jruby.api.Define#defineClass(ThreadContext, String, RubyClass, ObjectAllocator)}).
     *
     * @param allocator to use
     * @return itself for a composable API
     */
    @JRubyAPI
    public <T extends RubyClass> T allocator(ObjectAllocator allocator) {
        this.allocator = allocator;
        return (T) this;
    }

    /**
     * Set what marshaller we use or generally use this to specify the typw cannot Marshal data.
     * @param marshal the marshaller
     * @return itself for composable API
     */
    @JRubyAPI
    public <T extends RubyClass> T marshalWith(ObjectMarshal marshal) {
        this.marshal = marshal;
        return (T) this;
    }

    /**
     * Sets reified class to use.
     * @param reifiedClass the java type to use
     * @return itself for composable API
     */
    @JRubyAPI
    public <T extends RubyClass> T reifiedClass(Class<? extends IRubyObject> reifiedClass) {
        this.reifiedClass = (Class<? extends Reified>) reifiedClass; // Not always true
        return (T) this;
    }

    /**
     * Set a reflective allocator that calls a no-arg constructor on the given
     * class.
     *
     * @param clazz The class on which to call the default constructor to allocate
     */
    @SuppressWarnings("unchecked")
    public void setClassAllocator(final Class<?> clazz) {
        final Constructor<?> constructor;
        try {
            constructor = clazz.getConstructor();
        } catch (NoSuchMethodException nsme) {
            throw new RuntimeException(nsme);
        }

        this.allocator = (runtime, klazz) -> {
            try {
                RubyBasicObject object = (RubyBasicObject) constructor.newInstance();
                object.setMetaClass(klazz);
                return object;
            } catch (InvocationTargetException e) {
                throw newTypeError(runtime, "could not allocate " + clazz + " with default constructor:\n" + e.getTargetException(), e);
            } catch (InstantiationException e) {
                throw newTypeError(runtime, "could not allocate " + clazz + " with default constructor:\n" + e, e);
            } catch (IllegalAccessException e) {
                throw runtime.newSecurityError("could not allocate " + clazz + " due to inaccessible default constructor:\n" + e);
            }
        };

        this.reifiedClass = (Class<? extends Reified>) clazz;
    }

    /**
     * Set a reflective allocator that calls the "standard" Ruby object
     * constructor (Ruby, RubyClass) on the given class.
     *
     * @param clazz The class from which to grab a standard Ruby constructor
     */
    @SuppressWarnings("unchecked")
    public void setRubyClassAllocator(final Class<? extends IRubyObject> clazz) {
        final Constructor<? extends IRubyObject> constructor;
        try {
            constructor = clazz.getConstructor(Ruby.class, RubyClass.class);
        } catch (NoSuchMethodException nsme) {
            throw new RuntimeException(nsme);
        }

        this.allocator = (runtime, klazz) -> {
            try {
                return constructor.newInstance(runtime, klazz);
            } catch (InvocationTargetException e) {
                throw newTypeError(runtime, "could not allocate " + clazz + " with (Ruby, RubyClass) constructor:\n" + e.getTargetException(), e);
            } catch (InstantiationException e) {
                throw newTypeError(runtime, "could not allocate " + clazz + " with (Ruby, RubyClass) constructor:\n" + e, e);
            } catch (IllegalAccessException e) {
                throw runtime.newSecurityError("could not allocate " + clazz + " due to inaccessible (Ruby, RubyClass) constructor:\n" + e);
            }
        };

        this.reifiedClass = (Class<? extends Reified>) clazz;
    }

    /**
     * Set a reflective allocator that calls the "standard" Ruby object
     * constructor (Ruby, RubyClass) on the given class via a static
     * __allocate__ method intermediate.
     *
     * @param clazz The class from which to grab a standard Ruby __allocate__ method.
     *
     * <p>Note: Used with `jrubyc --java` generated (interoperability) class files.</p>
     * <p>Note: Used with new concrete extension.</p>
     */
    public void setRubyStaticAllocator(final Class<?> clazz) {
        final Method method;
        try {
            method = clazz.getDeclaredMethod("__allocate__", Ruby.class, RubyClass.class);
        } catch (NoSuchMethodException nsme) {
            throw new RuntimeException(nsme);
        }

        this.allocator = (runtime, klazz) -> {
            try {
                return (IRubyObject) method.invoke(null, runtime, klazz);
            } catch (InvocationTargetException e) {
                throw newTypeError(runtime, "could not allocate " + clazz + " with (Ruby, RubyClass) method:\n" + e.getTargetException(), e);
            } catch (IllegalAccessException e) {
                throw runtime.newSecurityError("could not allocate " + clazz + " due to inaccessible (Ruby, RubyClass) method:\n" + e);
            }
        };

        this.reifiedClass = (Class<? extends Reified>) clazz;
    }

    private static RaiseException newTypeError(final Ruby runtime, final String msg, final Exception e) {
        return withException(createTypeError(runtime.getCurrentContext(), msg.toString()), e);
    }

    @JRubyMethod(name = "allocate")
    public IRubyObject allocate() {
        if (superClass == null) {
            if (this != runtime.getBasicObject()) {
                throw typeError(runtime.getCurrentContext(), "can't instantiate uninitialized class");
            }
        }
        IRubyObject obj = allocator.allocate(runtime, this);
        if (getMetaClass(obj).getRealClass() != getRealClass()) {
            throw typeError(runtime.getCurrentContext(), "wrong instance allocation");
        }
        return obj;
    }

    public CallSite getBaseCallSite(int idx) {
        return baseCallSites[idx];
    }

    public CallSite[] getBaseCallSites() {
        return baseCallSites;
    }

    public CallSite[] getExtraCallSites() {
        return extraCallSites;
    }

    public VariableTableManager getVariableTableManager() {
        return variableTableManager;
    }

    public boolean hasObjectID() {
        return variableTableManager.hasObjectID();
    }

    public Map<String, VariableAccessor> getVariableAccessorsForRead() {
        return variableTableManager.getVariableAccessorsForRead();
    }

    public VariableAccessor getVariableAccessorForWrite(String name) {
        return variableTableManager.getVariableAccessorForWrite(name);
    }

    public VariableAccessor getVariableAccessorForRead(String name) {
        VariableAccessor accessor = getVariableAccessorsForRead().get(name);
        if (accessor == null) accessor = VariableAccessor.DUMMY_ACCESSOR;
        return accessor;
    }

    public VariableAccessor getFFIHandleAccessorForRead() {
        return variableTableManager.getFFIHandleAccessorForRead();
    }

    public VariableAccessor getFFIHandleAccessorForWrite() {
        return variableTableManager.getFFIHandleAccessorForWrite();
    }

    public VariableAccessor getObjectGroupAccessorForRead() {
        return variableTableManager.getObjectGroupAccessorForRead();
    }

    public VariableAccessor getObjectGroupAccessorForWrite() {
        return variableTableManager.getObjectGroupAccessorForWrite();
    }

    public int getVariableTableSize() {
        return variableTableManager.getVariableTableSize();
    }

    public int getVariableTableSizeWithExtras() {
        return variableTableManager.getVariableTableSizeWithExtras();
    }

    /**
     * Get an array of all the known instance variable names. The offset into
     * the array indicates the offset of the variable's value in the per-object
     * variable array.
     *
     * @return a copy of the array of known instance variable names
     */
    public String[] getVariableNames() {
        return variableTableManager.getVariableNames();
    }

    public Map<String, VariableAccessor> getVariableTableCopy() {
        return new HashMap<String, VariableAccessor>(getVariableAccessorsForRead());
    }

    @Override
    public ClassIndex getNativeClassIndex() {
        return ClassIndex.CLASS;
    }

    @Override
    public boolean isModule() {
        return false;
    }

    @Override
    public boolean isClass() {
        return true;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    /** boot_defclass
     * Create an initial Object meta class before Module and Kernel dependencies have
     * squirreled themselves together.
     *
     * @param runtime we need it
     * @return a half-baked meta class for object
     */
    public static RubyClass createBootstrapClass(Ruby runtime, String name, RubyClass superClass, ObjectAllocator allocator) {
        RubyClass obj = superClass == null ?
                new RubyClass(runtime).marshalWith(DEFAULT_OBJECT_MARSHAL) :
                new RubyClass(runtime, superClass);
        return obj.allocator(allocator).baseName(name);
    }

    /** separate path for MetaClass and IncludedModuleWrapper construction
     *  (rb_class_boot version for MetaClasses)
     *  no marshal, allocator initialization and addSubclass(this) here!
     */
    protected RubyClass(Ruby runtime, RubyClass superClass, boolean objectSpace) {
        super(runtime, runtime.getClassClass(), objectSpace);
        this.runtime = runtime;

        // Since this path is for included wrappers and singletons, use parent
        // class's realClass and varTableMgr. If the latter is null, create a
        // dummy, since we won't be using it anyway (we're above BasicObject
        // so variable requests won't reach us).
        if (superClass == null) {
            this.realClass = null;
            this.variableTableManager = new VariableTableManager(this);
        } else {
            if ((this.realClass = superClass.realClass) != null) {
                this.variableTableManager = realClass.variableTableManager;
            } else {
                this.variableTableManager = new VariableTableManager(this);
            }
        }

        superClass(superClass); // this is the only case it might be null here (in MetaClass construction)
    }

    /**
     *  This is an internal API only used by Ruby constructor before ThreadContext exists.
     * @param runtime
     * @param superClass
     * @param Class
     */
    protected RubyClass(Ruby runtime, RubyClass superClass, RubyClass Class) {
        super(runtime, Class, false);

        this.runtime = runtime;
        if ((this.realClass = superClass.realClass) != null) {
            this.variableTableManager = realClass.variableTableManager;
        } else {
            this.variableTableManager = new VariableTableManager(this);
        }

        superClass(superClass);
    }

    /** used by CLASS_ALLOCATOR (any Class' class will be a Class!)
     *  also used to bootstrap Object class
     */
    protected RubyClass(Ruby runtime) {
        super(runtime, runtime.getClassClass());
        this.runtime = runtime;
        this.realClass = this;
        this.variableTableManager = new VariableTableManager(this);
        classIndex(ClassIndex.CLASS);
    }

    /** rb_class_boot (for plain Classes)
     *  also used to bootstrap Module and Class classes
     */
    protected RubyClass(Ruby runtime, RubyClass superClazz) {
        this(runtime);
        superClass(superClazz);
        marshal = superClazz.marshal; // use parent's marshal
        superClazz.addSubclass(this);
        allocator = superClazz.allocator;

    }

    /**
     * A constructor which allows passing in an array of supplementary call sites.
     */
    protected RubyClass(Ruby runtime, RubyClass superClazz, CallSite[] extraCallSites) {
        this(runtime);
        superClass(superClazz);
        this.marshal = superClazz.marshal; // use parent's marshal
        superClazz.addSubclass(this);

        this.extraCallSites = extraCallSites;

    }

    /**
     * Construct a new class with the given name scoped under Object (global)
     * and with Object as its immediate superclass.
     * Corresponds to rb_class_new in MRI.
     */
    @Deprecated(since = "10.0")
    public static RubyClass newClass(Ruby runtime, RubyClass superClass) {
        if (superClass == runtime.getClassClass()) throw typeError(runtime.getCurrentContext(), "can't make subclass of Class");
        if (superClass.isSingleton()) throw typeError(runtime.getCurrentContext(), "can't make subclass of virtual class");
        return new RubyClass(runtime, superClass);
    }

    @Deprecated(since = "10.0")
    public static RubyClass newClass(Ruby runtime, RubyClass superClass, CallSite[] extraCallSites) {
        return newClass(runtime.getCurrentContext(), superClass, extraCallSites);
    }

    /**
     * A variation on newClass that allow passing in an array of supplementary
     * call sites to improve dynamic invocation.
     */
    public static RubyClass newClass(ThreadContext context, RubyClass superClass, CallSite[] extraCallSites) {
        if (superClass == classClass(context)) throw typeError(context, "can't make subclass of Class");
        if (superClass.isSingleton()) throw typeError(context, "can't make subclass of virtual class");
        return new RubyClass(context.runtime, superClass, extraCallSites);
    }

    /**
     * Construct a new class with the given name, allocator, parent class,
     * and containing class. If setParent is true, the class's parent will be
     * explicitly set to the provided parent (rather than the new class just
     * being assigned to a constant in that parent).
     * Corresponds to rb_class_new/rb_define_class_id/rb_name_class/rb_set_class_path
     * in MRI.
     */
    @Deprecated(since = "10.0")
    public static RubyClass newClass(Ruby runtime, RubyClass superClass, String name, ObjectAllocator allocator, RubyModule parent, boolean setParent) {
        var context = runtime.getCurrentContext();
        RubyClass clazz = newClass(context, superClass, null).
                allocator(allocator).
                baseName(name);

        clazz.makeMetaClass(superClass.getMetaClass());
        if (setParent) clazz.setParent(parent);
        parent.defineConstant(context, name, clazz);
        superClass.invokeInherited(context, superClass, clazz);
        return clazz;
    }

    @Deprecated(since = "10.0")
    public static RubyClass newClass(Ruby runtime, RubyClass superClass, String name, ObjectAllocator allocator,
                                     RubyModule parent, boolean setParent, String file, int line) {
        return newClass(runtime.getCurrentContext(), superClass, name, allocator, parent, setParent, file, line);
    }

    public static RubyClass newClass(ThreadContext context, RubyClass superClass, String name, ObjectAllocator allocator,
                                     RubyModule parent, boolean setParent, String file, int line) {
        assert superClass != null;
        RubyClass clazz = newClass(context, superClass, null).
                allocator(allocator).
                baseName(name);
        clazz.makeMetaClass(superClass.getMetaClass());
        if (setParent) clazz.setParent(parent);
        parent.setConstant(context, name, clazz, file, line);
        superClass.invokeInherited(context, superClass, clazz);
        return clazz;
    }

    @Deprecated(since = "10.0")
    public static RubyClass newClass(Ruby runtime, RubyClass superClass, String name, ObjectAllocator allocator, RubyModule parent, boolean setParent, CallSite[] extraCallSites) {
        return newClass(runtime.getCurrentContext(), superClass, name, allocator, parent, setParent, extraCallSites);
    }

    /**
     * A variation on newClass that allows passing in an array of supplementary
     * call sites to improve dynamic invocation performance.
     */
    public static RubyClass newClass(ThreadContext context, RubyClass superClass, String name,
                                     ObjectAllocator allocator, RubyModule parent, boolean setParent,
                                     CallSite[] extraCallSites) {
        RubyClass clazz = newClass(context, superClass, extraCallSites).
                allocator(allocator).
                baseName(name);
        clazz.makeMetaClass(superClass.getMetaClass());
        if (setParent) clazz.setParent(parent);
        parent.setConstant(context, name, clazz, BUILTIN_CONSTANT, -1);
        superClass.invokeInherited(context, superClass, clazz);
        return clazz;
    }

    /**
     * This is an internal API for bootstrapping a few classes before ThreadContext is available.  The API
     * is intentionally limited/obtuse so no one is tempted to try and use it.
     *
     * @param runtime the runtime
     * @param Object reference to Object which is superclass and parent for new type
     * @param name the name of the new class
     * @return the new class.
     */
    public static RubyClass newClassBootstrap(Ruby runtime, RubyClass Object, String name) {
        RubyClass clazz = new RubyClass(runtime, Object).
                allocator(NOT_ALLOCATABLE_ALLOCATOR).
                baseName(name);
        clazz.makeMetaClass(Object.getMetaClass());
        Object.defineConstantBootstrap(name, clazz);
        return clazz;
    }

    /**
     * @see #singletonClass(ThreadContext)
     */
    RubyClass toSingletonClass(RubyBasicObject target) {
        // replaced after makeMetaClass with MetaClass's toSingletonClass
        return target.makeMetaClass(this);
    }

    static boolean notVisibleAndNotMethodMissing(DynamicMethod method, String name, IRubyObject caller, CallType callType) {
        return !method.isCallableFrom(caller, callType) && !name.equals("method_missing");
    }

    public IRubyObject finvoke(ThreadContext context, IRubyObject self, String name, Block block) {
        return finvokeWithRefinements(context, self, null, name, block);
    }

    public IRubyObject finvokeWithRefinements(ThreadContext context, IRubyObject self, StaticScope staticScope, String name, Block block) {
        CacheEntry entry = searchWithRefinements(name, staticScope);
        DynamicMethod method = entry.method;

        if (shouldCallMethodMissing(method)) {
            return Helpers.callMethodMissing(context, self, this, method.getVisibility(), name, CallType.FUNCTIONAL, block);
        }
        return method.call(context, self, entry.sourceModule, name, block);
    }

    public IRubyObject finvoke(ThreadContext context, IRubyObject self, String name,
                               IRubyObject[] args, Block block) {
        return finvokeWithRefinements(context, self, null, name, args, block);
    }

    public IRubyObject finvokeWithRefinements(ThreadContext context, IRubyObject self, StaticScope staticScope, String name,
                                              IRubyObject[] args, Block block) {
        assert args != null;
        CacheEntry entry = searchWithRefinements(name, staticScope);
        DynamicMethod method = entry.method;

        if (shouldCallMethodMissing(method)) {
            return Helpers.callMethodMissing(context, self, this, method.getVisibility(), name, CallType.FUNCTIONAL, args, block);
        }
        return method.call(context, self, entry.sourceModule, name, args, block);
    }

    public IRubyObject finvoke(ThreadContext context, IRubyObject self, String name,
                               IRubyObject arg, Block block) {
        return finvokeWithRefinements(context, self, null, name, arg, block);
    }

    public IRubyObject finvokeWithRefinements(ThreadContext context, IRubyObject self, StaticScope staticScope, String name,
                               IRubyObject arg, Block block) {
        CacheEntry entry = searchWithRefinements(name, staticScope);
        DynamicMethod method = entry.method;

        if (shouldCallMethodMissing(method)) {
            return Helpers.callMethodMissing(context, self, this, method.getVisibility(), name, CallType.FUNCTIONAL, arg, block);
        }
        return method.call(context, self, entry.sourceModule, name, arg, block);
    }

    public IRubyObject finvoke(ThreadContext context, IRubyObject self, String name,
                               IRubyObject arg0, IRubyObject arg1, Block block) {
        return finvokeWithRefinements(context, self, null, name, arg0, arg1, block);
    }

    public IRubyObject finvokeWithRefinements(ThreadContext context, IRubyObject self, StaticScope staticScope, String name,
                               IRubyObject arg0, IRubyObject arg1, Block block) {
        CacheEntry entry = searchWithRefinements(name, staticScope);
        DynamicMethod method = entry.method;

        if (shouldCallMethodMissing(method)) {
            return Helpers.callMethodMissing(context, self, this, method.getVisibility(), name, CallType.FUNCTIONAL, arg0, arg1, block);
        }
        return method.call(context, self, entry.sourceModule, name, arg0, arg1, block);
    }

    public IRubyObject finvoke(ThreadContext context, IRubyObject self, String name,
                               IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return finvokeWithRefinements(context, self, null, name, arg0, arg1, arg2, block);
    }

    public IRubyObject finvokeWithRefinements(ThreadContext context, IRubyObject self, StaticScope staticScope, String name,
                               IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        CacheEntry entry = searchWithRefinements(name, staticScope);
        DynamicMethod method = entry.method;

        if (shouldCallMethodMissing(method)) {
            return Helpers.callMethodMissing(context, self, this, method.getVisibility(), name, CallType.FUNCTIONAL, arg0, arg1, arg2, block);
        }
        return method.call(context, self, entry.sourceModule, name, arg0, arg1, arg2, block);
    }

    public IRubyObject finvoke(ThreadContext context, IRubyObject self, String name) {
        CacheEntry entry = searchWithCache(name);
        DynamicMethod method = entry.method;

        if (shouldCallMethodMissing(method)) {
            return Helpers.callMethodMissing(context, self, this, method.getVisibility(), name, CallType.FUNCTIONAL, Block.NULL_BLOCK);
        }
        return method.call(context, self, entry.sourceModule, name);
    }

    /**
    * MRI: rb_funcallv_public
    */
    public IRubyObject invokePublic(ThreadContext context, IRubyObject self, String name, IRubyObject arg) {
        CacheEntry entry = searchWithCache(name);
        DynamicMethod method = entry.method;

        if (shouldCallMethodMissing(method) || method.getVisibility() != PUBLIC) {
            return Helpers.callMethodMissing(context, self, this, method.getVisibility(), name, CallType.FUNCTIONAL, arg, Block.NULL_BLOCK);
        }
        return method.call(context, self, entry.sourceModule, name, arg);
    }

    /**
     * Safely attempt to invoke the given method name on self, using respond_to? and method_missing as appropriate.
     *
     * MRI: rb_check_funcall
     */
    public final IRubyObject finvokeChecked(ThreadContext context, IRubyObject self, String name) {
        return checkFuncallDefault(context, self, name, IRubyObject.NULL_ARRAY);
    }

    /**
     * Safely attempt to invoke the given method name on self, using respond_to? and method_missing as appropriate.
     *
     * MRI: rb_check_funcall
     */
    public final IRubyObject finvokeChecked(ThreadContext context, IRubyObject self, JavaSites.CheckedSites sites) {
        return checkFuncallDefault(context, self, sites);
    }

    /**
     * Safely attempt to invoke the given method name on self, using respond_to? and method_missing as appropriate.
     *
     * MRI: rb_check_funcall
     */
    public final IRubyObject finvokeChecked(ThreadContext context, IRubyObject self, String name, IRubyObject... args) {
        return checkFuncallDefault(context, self, name, args);
    }

    /**
     * Safely attempt to invoke the given method name on self, using respond_to? and method_missing as appropriate.
     *
     * MRI: rb_check_funcall
     */
    public final IRubyObject finvokeChecked(ThreadContext context, IRubyObject self, JavaSites.CheckedSites sites, IRubyObject... args) {
        return checkFuncallDefault(context, self, sites, args);
    }

    // MRI: rb_check_funcall_default
    private IRubyObject checkFuncallDefault(ThreadContext context, IRubyObject self, String name, IRubyObject[] args) {
        final RubyClass klass = this;
        if (!checkFuncallRespondTo(context, klass, self, name)) return null; // return def;

        DynamicMethod method = searchMethod(name);
        if (!checkFuncallCallable(context, method, CallType.FUNCTIONAL, self)) {
            return checkFuncallMissing(context, klass, self, name, args);
        }
        return method.call(context, self, klass, name, args);
    }

    // MRI: rb_check_funcall_default
    private IRubyObject checkFuncallDefault(ThreadContext context, IRubyObject self, JavaSites.CheckedSites sites, IRubyObject[] args) {
        final RubyClass klass = this;
        if (!checkFuncallRespondTo(context, klass, self, sites.respond_to_X)) return null; // return def;

        CacheEntry entry = sites.site.retrieveCache(klass);
        DynamicMethod method = entry.method;
        if (!checkFuncallCallable(context, method, CallType.FUNCTIONAL, self)) {
            return checkFuncallMissing(context, klass, self, sites.methodName, sites.respond_to_missing, sites.method_missing, args);
        }
        return method.call(context, self, entry.sourceModule, sites.methodName, args);
    }

    // MRI: rb_check_funcall_default
    private IRubyObject checkFuncallDefault(ThreadContext context, IRubyObject self, JavaSites.CheckedSites sites) {
        final RubyClass klass = this;
        if (!checkFuncallRespondTo(context, klass, self, sites.respond_to_X)) return null; // return def;

        CacheEntry entry = sites.site.retrieveCache(klass);
        DynamicMethod method = entry.method;
        if (!checkFuncallCallable(context, method, CallType.FUNCTIONAL, self)) {
            return checkFuncallMissing(context, klass, self, sites.methodName, sites.respond_to_missing, sites.method_missing);
        }
        return method.call(context, self, entry.sourceModule, sites.methodName);
    }

    // MRI: check_funcall_exec
    private static IRubyObject checkFuncallExec(ThreadContext context, IRubyObject self, String name, IRubyObject... args) {
        return self.callMethod(context, "method_missing", ArraySupport.newCopy(asSymbol(context, name), args));
    }

    // MRI: check_funcall_exec
    private static IRubyObject checkFuncallExec(ThreadContext context, IRubyObject self, String name, CallSite methodMissingSite, IRubyObject... args) {
        return methodMissingSite.call(context, self, self, ArraySupport.newCopy(asSymbol(context, name), args));
    }

    // MRI: check_funcall_failed
    private static IRubyObject checkFuncallFailed(ThreadContext context, IRubyObject self, String name, RubyClass expClass, IRubyObject... args) {
        if (self.respondsTo(name)) throw context.runtime.newRaiseException(expClass, name);

        return null;
    }

    /**
     * Check if the method has a custom respond_to? and call it if so with the method ID we're hoping to call.
     *
     * MRI: check_funcall_respond_to
     */
    private static boolean checkFuncallRespondTo(ThreadContext context, RubyClass klass, IRubyObject recv, String mid) {
        CacheEntry entry = klass.searchWithCache("respond_to?");
        DynamicMethod me = entry.method;

        // NOTE: isBuiltin here would be NOEX_BASIC in MRI, a flag only added to respond_to?, method_missing, and
        //       respond_to_missing? Same effect, I believe.
        if (me == null || me.isUndefined() || me.isBuiltin()) return true;

        return me.callRespondTo(context, recv, "respond_to?", entry.sourceModule, asSymbol(context, mid));
    }

    /**
     * Check if the method has a custom respond_to? and call it if so with the method ID we're hoping to call.
     *
     * MRI: check_funcall_respond_to
     */
    private static boolean checkFuncallRespondTo(ThreadContext context, RubyClass klass, IRubyObject recv, RespondToCallSite respondToSite) {
        DynamicMethod me = respondToSite.retrieveCache(klass).method;

        // NOTE: isBuiltin here would be NOEX_BASIC in MRI, a flag only added to respond_to?, method_missing, and
        //       respond_to_missing? Same effect, I believe.
        if (me.isUndefined() || me.isBuiltin()) return true;

        int required = me.getSignature().required();

        if (required > 2) throw argumentError(context, "respond_to? must accept 1 or 2 arguments (requires " + required + ")");

        return required == 1 ?
                respondToSite.respondsTo(context, recv, recv) :
                respondToSite.respondsTo(context, recv, recv, true);
    }

    // MRI: check_funcall_callable
    static boolean checkFuncallCallable(ThreadContext context, DynamicMethod method, CallType callType, IRubyObject self) {
        return rbMethodCallStatus(context, method, callType, self);
    }

    // MRI: rb_method_call_status
    // FIXME: Partial impl because we don't have these "NOEX" flags
    private static boolean rbMethodCallStatus(ThreadContext context, DynamicMethod method, CallType callType, IRubyObject self) {
        return !method.isUndefined() && method.isCallableFrom(self, callType);
    }

    // MRI: check_funcall_missing
    private static IRubyObject checkFuncallMissing(ThreadContext context, RubyClass klass, IRubyObject self, String method, IRubyObject... args) {
        final Ruby runtime = context.runtime;

        CacheEntry entry = klass.searchWithCache("respond_to_missing?");
        DynamicMethod me = entry.method;
        // MRI: basic_obj_respond_to_missing ...
        if (!me.isUndefined() && !me.isBuiltin() &&
                !me.callRespondTo(context, self, "respond_to_missing?", entry.sourceModule, asSymbol(context, method))) {
            return null;
        }

        if ( klass.isMethodBuiltin("method_missing") ) return null;

        final IRubyObject $ex = context.getErrorInfo();
        try {
            return checkFuncallExec(context, self, method, args);
        } catch (RaiseException e) {
            context.setErrorInfo($ex); // restore $!
            return checkFuncallFailed(context, self, method, runtime.getNoMethodError(), args);
        }
    }

    // MRI: check_funcall_missing
    private static IRubyObject checkFuncallMissing(ThreadContext context, RubyClass klass, IRubyObject self, String method, CachingCallSite respondToMissingSite, CachingCallSite methodMissingSite, IRubyObject... args) {
        final Ruby runtime = context.runtime;

        CacheEntry entry = respondToMissingSite.retrieveCache(klass);
        DynamicMethod me = entry.method;
        // MRI: basic_obj_respond_to_missing ...
        if (!me.isUndefined() && !me.isBuiltin() &&
                !me.callRespondTo(context, self, "respond_to_missing?", entry.sourceModule, asSymbol(context, method))) {
            return null;
        }

        if (methodMissingSite.retrieveCache(klass).method.isBuiltin()) return null;

        final IRubyObject $ex = context.getErrorInfo();
        try {
            return checkFuncallExec(context, self, method, methodMissingSite, args);
        }
        catch (RaiseException e) {
            context.setErrorInfo($ex); // restore $!
            return checkFuncallFailed(context, self, method, runtime.getNoMethodError(), args);
        }
    }

    public IRubyObject finvoke(ThreadContext context, IRubyObject self, String name,
            IRubyObject[] args) {
        assert args != null;
        CacheEntry entry = searchWithCache(name);
        DynamicMethod method = entry.method;
        if (shouldCallMethodMissing(method)) {
            return Helpers.callMethodMissing(context, self, this, method.getVisibility(), name, CallType.FUNCTIONAL, args, Block.NULL_BLOCK);
        }
        return method.call(context, self, entry.sourceModule, name, args);
    }

    public IRubyObject finvoke(ThreadContext context, IRubyObject self, String name,
            IRubyObject arg) {
        CacheEntry entry = searchWithCache(name);
        DynamicMethod method = entry.method;
        if (shouldCallMethodMissing(method)) {
            return Helpers.callMethodMissing(context, self, this, method.getVisibility(), name, CallType.FUNCTIONAL, arg, Block.NULL_BLOCK);
        }
        return method.call(context, self, entry.sourceModule, name, arg);
    }

    public IRubyObject finvoke(ThreadContext context, IRubyObject self, String name,
            IRubyObject arg0, IRubyObject arg1) {
        CacheEntry entry = searchWithCache(name);
        DynamicMethod method = entry.method;
        if (shouldCallMethodMissing(method)) {
            return Helpers.callMethodMissing(context, self, this, method.getVisibility(), name, CallType.FUNCTIONAL, arg0, arg1, Block.NULL_BLOCK);
        }
        return method.call(context, self, entry.sourceModule, name, arg0, arg1);
    }

    public IRubyObject finvoke(ThreadContext context, IRubyObject self, String name,
            IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        CacheEntry entry = searchWithCache(name);
        DynamicMethod method = entry.method;
        if (shouldCallMethodMissing(method)) {
            return Helpers.callMethodMissing(context, self, this, method.getVisibility(), name, CallType.FUNCTIONAL, arg0, arg1, arg2, Block.NULL_BLOCK);
        }
        return method.call(context, self, entry.sourceModule, name, arg0, arg1, arg2);
    }

    // MRI: rb_method_basic_definition_p
    public boolean checkMethodBasicDefinition(String name) {
        DynamicMethod method = searchMethod(name);
        return method != null && method.isBuiltin();
    }

    private void dumpReifiedClass(ThreadContext context, String dumpDir, String javaPath, byte[] classBytes) {
        if (dumpDir != null) {
            if (dumpDir.length() == 0) dumpDir = ".";

            java.io.FileOutputStream classStream = null;
            try {
                java.io.File classFile = new java.io.File(dumpDir, javaPath + ".class");
                classFile.getParentFile().mkdirs();
                classStream = new java.io.FileOutputStream(classFile);
                classStream.write(classBytes);
            }
            catch (IOException io) {
                warn(context, "unable to dump class file: " + io.getMessage());
            } finally {
                if (classStream != null) {
                    try { classStream.close(); }
                    catch (IOException ignored) { /* no-op */ }
                }
            }
        }
    }

    private void generateMethodAnnotations(Map<Class<?>, Map<String, Object>> methodAnnos, SkinnyMethodAdapter m, List<Map<Class<?>, Map<String, Object>>> parameterAnnos) {
        if (methodAnnos != null && methodAnnos.size() != 0) {
            for (Map.Entry<Class<?>, Map<String, Object>> entry : methodAnnos.entrySet()) {
                m.visitAnnotationWithFields(ci(entry.getKey()), true, entry.getValue());
            }
        }
        if (parameterAnnos != null && parameterAnnos.size() != 0) {
            for (int i = 0; i < parameterAnnos.size(); i++) {
                Map<Class<?>, Map<String, Object>> annos = parameterAnnos.get(i);
                if (annos != null && annos.size() != 0) {
                    for (Iterator<Map.Entry<Class<?>, Map<String, Object>>> it = annos.entrySet().iterator(); it.hasNext();) {
                        Map.Entry<Class<?>, Map<String, Object>> entry = it.next();
                        m.visitParameterAnnotationWithFields(i, ci(entry.getKey()), true, entry.getValue());
                    }
                }
            }
        }
    }

    private static boolean shouldCallMethodMissing(DynamicMethod method) {
        return method.isUndefined();
    }
    private static boolean shouldCallMethodMissing(DynamicMethod method, String name, IRubyObject caller, CallType callType) {
        return method.isUndefined() || notVisibleAndNotMethodMissing(method, name, caller, callType);
    }

    public IRubyObject invokeInherited(ThreadContext context, IRubyObject self, IRubyObject subclass) {
        CacheEntry entry = metaClass.searchWithCache("inherited");
        DynamicMethod method = entry.method;

        if (method.isUndefined()) {
            return Helpers.callMethodMissing(context, self, method.getVisibility(), "inherited", CallType.FUNCTIONAL, Block.NULL_BLOCK);
        }

        return method.call(context, self, entry.sourceModule, "inherited", subclass, Block.NULL_BLOCK);
    }

    /** rb_class_new_instance
    *
    */
    @JRubyMethod(name = "new", keywords = true)
    public IRubyObject newInstance(ThreadContext context, Block block) {
        IRubyObject obj = allocate();
        baseCallSites[CS_IDX_INITIALIZE].call(context, obj, obj, block);
        return obj;
    }

    @JRubyMethod(name = "new", keywords = true)
    public IRubyObject newInstance(ThreadContext context, IRubyObject arg0, Block block) {
        IRubyObject obj = allocate();
        baseCallSites[CS_IDX_INITIALIZE].call(context, obj, obj, arg0, block);
        return obj;
    }

    public IRubyObject newInstance(ThreadContext context, IRubyObject arg0) {
        IRubyObject obj = allocate();
        baseCallSites[CS_IDX_INITIALIZE].call(context, obj, obj, arg0);
        return obj;
    }

    @JRubyMethod(name = "new", keywords = true)
    public IRubyObject newInstance(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        IRubyObject obj = allocate();
        baseCallSites[CS_IDX_INITIALIZE].call(context, obj, obj, arg0, arg1, block);
        return obj;
    }

    @JRubyMethod(name = "new", keywords = true)
    public IRubyObject newInstance(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        IRubyObject obj = allocate();
        baseCallSites[CS_IDX_INITIALIZE].call(context, obj, obj, arg0, arg1, arg2, block);
        return obj;
    }

    @JRubyMethod(name = "new", rest = true, keywords = true)
    public IRubyObject newInstance(ThreadContext context, IRubyObject[] args, Block block) {
        IRubyObject obj = allocate();
        baseCallSites[CS_IDX_INITIALIZE].call(context, obj, obj, args, block);
        return obj;
    }

    /** rb_class_initialize
     *
     */
    @Override
    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, Block block) {
        checkNotInitialized(context);
        return initializeCommon(context, objectClass(context), block);
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject superObject, Block block) {
        checkNotInitialized(context);
        checkInheritable(context, superObject);
        return initializeCommon(context, (RubyClass) superObject, block);
    }

    private RubyClass initializeCommon(ThreadContext context, RubyClass superClazz, Block block) {
        superClass(superClazz);
        allocator = superClazz.allocator;
        makeMetaClass(superClazz.getMetaClass());
        superClazz.addSubclass(this);

        marshal = superClazz.marshal;

        superClazz.invokeInherited(runtime.getCurrentContext(), superClazz, this);
        super.initialize(context, block);

        return this;
    }

    /** rb_class_init_copy
     *
     */
    @JRubyMethod(name = "initialize_copy", visibility = PRIVATE)
    public IRubyObject initialize_copy(ThreadContext context, IRubyObject original) {
        checkNotInitialized(context);
        if (original instanceof MetaClass) throw typeError(context, "can't copy singleton class");

        super.initialize_copy(context, original);
        RubyClass originalClazz = (RubyClass) original;
        allocator = originalClazz.allocator;

        // copy over reify options
        javaClassConfiguration = originalClazz.javaClassConfiguration == null ? null : originalClazz.javaClassConfiguration.clone();

        // copy over reified class if applicable
        if (originalClazz.getJavaProxy() && originalClazz.reifiedClass != null &&
                !Reified.class.isAssignableFrom(originalClazz.reifiedClass)) {
            reifiedClass = originalClazz.reifiedClass;
            reifiedClassJava = originalClazz.reifiedClassJava;
        }

        return this;
    }

    @JRubyMethod
    public IRubyObject subclasses(ThreadContext context) {
        var subs = newConcreteSubclassesArray(context);
        int clearedCount = concreteSubclasses(context, subs);
        finishConcreteSubclasses(context, subs, clearedCount);

        return subs;
    }

    // introduced solely to provide some level of compatibility with previous
    // Class#subclasses implementation ... `ruby_class.to_java.subclasses`
    public final Collection<RubyClass> subclasses() {
        return subclasses(false);
    }

    public Collection<RubyClass> subclasses(boolean includeDescendants) {
        Collection<RubyClass> mine = newSubclassesList(includeDescendants);
        int clearedCount = subclassesInner(mine, includeDescendants);
        finishSubclasses(mine, clearedCount, includeDescendants);

        return mine;
    }

    private RubyArray<?> newConcreteSubclassesArray(ThreadContext context) {
        var subs = Create.newRawArray(context, this.concreteSubclassesEstimate);
        return subs;
    }

    private Collection<RubyClass> newSubclassesList(boolean includeDescendants) {
        Collection<RubyClass> mine = new ArrayList<>(includeDescendants ? allDescendantsEstimate : allSubclassesEstimate);
        return mine;
    }

    private int subclassesInner(Collection<RubyClass> mine, boolean includeDescendants) {
        SubclassNode subclassNode = this.subclassNode;
        int clearedCount = 0;
        while (subclassNode != null) {
            RubyClass klass = subclassNode.ref.get();
            subclassNode = subclassNode.next;

            if (klass == null) {
                clearedCount++;
                continue;
            }

            processSubclass(mine, includeDescendants, klass);
        }
        return clearedCount;
    }

    private static void processSubclass(Collection<RubyClass> mine, boolean includeDescendants, RubyClass klass) {
        mine.add(klass);

        if (includeDescendants) klass.subclassesInner(mine, includeDescendants);
    }

    private void finishSubclasses(Collection<RubyClass> mine, int clearedCount, boolean includeDescendants) {
        int newSize = mine.size();
        if (includeDescendants) {
            allDescendantsEstimate = newSize;
        } else {
            allSubclassesEstimate = newSize;
        }
        cleanSubclasses(newSize, clearedCount);
    }

    private int concreteSubclasses(ThreadContext context, RubyArray<?> subs) {
        SubclassNode subclassNode = this.subclassNode;

        if (subclassNode == null) return 0;

        // skip first entry if not concrete
        if (!subclassNode.concrete) subclassNode = subclassNode.nextConcrete;

        int clearedCount = 0;
        while (subclassNode != null) {
            RubyClass klass = subclassNode.ref.get();
            subclassNode = subclassNode.nextConcrete;

            if (klass == null) {
                clearedCount++;
            } else {
                processConcreteSubclass(context, subs, klass);
            }

        }
        return clearedCount;
    }

    private static void processConcreteSubclass(ThreadContext context, RubyArray<?> subs, RubyClass klass) {
        assert !klass.isSingleton();

        if (klass.isIncluded() || klass.isPrepended()) {
            klass.concreteSubclasses(context, subs);
        } else {
            subs.append(context, klass);
        }
    }

    private void finishConcreteSubclasses(ThreadContext context, RubyArray<?> subs, int clearedCount) {
        subs.finishRawArray(context);
        int newSize = subs.size();
        concreteSubclassesEstimate = newSize;
        cleanSubclasses(newSize, clearedCount);
    }

    private void cleanSubclasses(int size, int vacated) {
        // tidy up if more than threshold of cleared references
        if ((double) vacated / size > SUBCLASSES_CLEAN_FACTOR) {
            SubclassNode subclassNode = this.subclassNode;
            SubclassNode newTop = rebuildSubclasses(subclassNode);
            while (!SUBCLASS_UPDATER.compareAndSet(this, subclassNode, newTop)) {
                subclassNode = this.subclassNode;
                newTop = rebuildSubclasses(subclassNode);
            }
        }
    }

    private static SubclassNode rebuildSubclasses(SubclassNode subclassNode) {
        SubclassNode newTop = null;
        while (subclassNode != null) {
            WeakReference<RubyClass> ref = subclassNode.ref;
            RubyClass klass = ref.get();
            subclassNode = subclassNode.next;
            if (klass == null) continue;
            newTop = new SubclassNode(klass, ref, newTop);
        }
        return newTop;
    }

    // TODO: make into a Record
    static class SubclassNode {
        final SubclassNode next;
        final SubclassNode nextConcrete;
        final boolean concrete;
        final WeakReference<RubyClass> ref;
        SubclassNode(RubyClass klass, SubclassNode next) {
            this(klass, new WeakReference<>(klass), next);
        }
        SubclassNode(RubyClass klass, WeakReference<RubyClass> ref, SubclassNode next) {
            this.ref = ref;
            this.next = next;
            this.nextConcrete = next == null ? null : next.concrete ? next : next.nextConcrete;
            this.concrete = !klass.isSingleton();
        }
    }

    /**
     * Add a new subclass to the weak set of subclasses.
     *
     * This version always constructs a new set to avoid having to synchronize
     * against the set when iterating it for invalidation in
     * invalidateCacheDescendants.
     *
     * @param subclass The subclass to add
     */
    public void addSubclass(RubyClass subclass) {
        SubclassNode subclassNode = this.subclassNode;
        SubclassNode newNode = new SubclassNode(subclass, subclassNode);
        while (!SUBCLASS_UPDATER.compareAndSet(this, subclassNode, newNode)) {
            subclassNode = this.subclassNode;
            newNode = new SubclassNode(subclass, subclassNode);
        }
    }

    /**
     * Remove a subclass from the weak set of subclasses.
     *
     * @param subclass The subclass to remove
     */
    public void removeSubclass(RubyClass subclass) {
        SubclassNode subclassNode = this.subclassNode;
        while (subclassNode != null) {
            WeakReference<RubyClass> ref = subclassNode.ref;
            RubyClass klass = ref.get();
            if (klass == subclass) {
                ref.clear();
                return;
            }
            subclassNode = subclassNode.next;
        }
    }

    /**
     * Replace an existing subclass with a new one.
     *
     * @param subclass The subclass to remove
     * @param newSubclass The subclass to replace it with
     */
    public void replaceSubclass(RubyClass subclass, RubyClass newSubclass) {
        removeSubclass(subclass);
        addSubclass(newSubclass);
    }

    /**
     * make this class and all subclasses sync
     */
    @Override
    public void becomeSynchronized() {
        super.becomeSynchronized();

        SubclassNode subclassNode = this.subclassNode;
        while (subclassNode != null) {
            WeakReference<RubyClass> ref = subclassNode.ref;
            RubyClass klass = ref.get();
            if (klass != null) {
                klass.becomeSynchronized();
            }
            subclassNode = subclassNode.next;
        }
    }

    /**
     * Invalidate all subclasses of this class by walking the set of all
     * subclasses and asking them to invalidate themselves.
     *
     * Note that this version works against a reference to the current set of
     * subclasses, which could be replaced by the time this iteration is
     * complete. In theory, there may be a path by which invalidation would
     * miss a class added during the invalidation process, but the exposure is
     * minimal if it exists at all. The only way to prevent it would be to
     * synchronize both invalidation and subclass set modification against a
     * global lock, which we would like to avoid.
     */
    @Override
    public void invalidateCacheDescendants() {
        super.invalidateCacheDescendants();

        SubclassNode subclassNode = this.subclassNode;
        while (subclassNode != null) {
            WeakReference<RubyClass> ref = subclassNode.ref;
            RubyClass klass = ref.get();
            if (klass != null) {
                klass.invalidateCacheDescendants();
            }
            subclassNode = subclassNode.next;
        }
    }

    public void addInvalidatorsAndFlush(List<Invalidator> invalidators) {
        // add this class's invalidators to the aggregate
        invalidators.add(methodInvalidator);

        // if we're not at boot time, don't bother fully clearing caches
        if (!runtime.isBootingCore()) cachedMethods.clear();

        SubclassNode subclassNode = this.subclassNode;
        while (subclassNode != null) {
            WeakReference<RubyClass> ref = subclassNode.ref;
            RubyClass klass = ref.get();
            if (klass != null) {
                klass.addInvalidatorsAndFlush(invalidators);
            }
            subclassNode = subclassNode.next;
        }
    }

    public final Ruby getClassRuntime() {
        return runtime;
    }

    /**
     * Get the "real" class, either the current one or the nearest ancestor that is not a singleton or include wrapper.
     *
     * @return the nearest non-singleton non-include class
     */
    public final RubyClass getRealClass() {
        return realClass;
    }

    /**
     * Equivalent to {@link #getRealClass()}.
     *
     * @return the nearest non-singleton non-include class
     */
    public RubyModule getRealModule() {
        return getRealClass();
    }

    @JRubyMethod(name = "inherited", visibility = PRIVATE)
    public IRubyObject inherited(ThreadContext context, IRubyObject arg) {
        return context.nil;
    }

    /** rb_class_inherited (reversed semantics!)
     *
     */
    @Deprecated(since = "10.0")
    public void inherit(RubyClass superClazz) {
        if (superClazz == null) superClazz = runtime.getObject();

        superClazz.invokeInherited(runtime.getCurrentContext(), superClazz, this);
    }

    /** Return the real super class of this class.
     *
     * rb_class_superclass
     *
     */
    @JRubyMethod(name = "superclass")
    public IRubyObject superclass(ThreadContext context) {
        RubyClass superClazz = superClass;

        if (superClazz == null) {
            if (metaClass == basicObjectClass(context).metaClass) return context.nil;
            throw typeError(context, "uninitialized class");
        }

        while (superClazz != null && (superClazz.isIncluded() || superClazz.isPrepended())) {
            superClazz = superClazz.superClass;
        }

        return superClazz != null ? superClazz : context.nil;
    }

    @JRubyMethod
    public IRubyObject attached_object(ThreadContext context) {
        throw typeError(context, "'", this, "' is not a singleton class");
    }

    private void checkNotInitialized(ThreadContext context) {
        if (superClass != null || this == basicObjectClass(context)) throw typeError(context, "already initialized class");
    }
    /** rb_check_inheritable
     *
     */
    @Deprecated
    public static void checkInheritable(IRubyObject superClass) {
        checkInheritable(superClass.getRuntime().getCurrentContext(), superClass);
    }

    public static void checkInheritable(ThreadContext context, IRubyObject superClass) {
        if (!(superClass instanceof RubyClass)) {
            throw typeError(context, "superclass must be a Class (" + superClass.getMetaClass() + " given)");
        }
        if (((RubyClass) superClass).isSingleton()) throw typeError(context, "can't make subclass of virtual class");
        if (superClass == classClass(context)) throw typeError(context, "can't make subclass of Class");
    }

    public final ObjectMarshal getMarshal() {
        return marshal;
    }

    /**
     * @param marshal
     * @deprecated Use {@link org.jruby.RubyClass#marshalWith(ObjectMarshal)} instead.
     */
    @Deprecated(since = "10.0")
    public final void setMarshal(ObjectMarshal marshal) {
        marshalWith(marshal);
    }

    public final void marshal(Object obj, MarshalStream marshalStream) throws IOException {
        getMarshal().marshalTo(runtime, obj, this, marshalStream);
    }

    public final void marshal(Object obj, NewMarshal marshalStream, ThreadContext context, NewMarshal.RubyOutputStream out) {
        getMarshal().marshalTo(obj, this, marshalStream, context, out);
    }

    public final Object unmarshal(UnmarshalStream unmarshalStream) throws IOException {
        return getMarshal().unmarshalFrom(runtime, this, unmarshalStream);
    }

    public static void marshalTo(RubyClass clazz, MarshalStream output) throws java.io.IOException {
        output.registerLinkTarget(clazz);
        output.writeString(MarshalStream.getPathFromClass(clazz));
    }

    public static void marshalTo(RubyClass clazz, NewMarshal output, NewMarshal.RubyOutputStream out) {
        output.registerLinkTarget(clazz);
        output.writeString(out, MarshalStream.getPathFromClass(clazz));
    }

    public static RubyClass unmarshalFrom(UnmarshalStream input) throws java.io.IOException {
        String name = RubyString.byteListToString(input.unmarshalString());
        return UnmarshalStream.getClassFromPath(input.getRuntime(), name);
    }

    protected static final ObjectMarshal DEFAULT_OBJECT_MARSHAL = new ObjectMarshal() {
        @Override
        public void marshalTo(Ruby runtime, Object obj, RubyClass type, MarshalStream marshalStream) throws IOException {
            IRubyObject object = (IRubyObject) obj;

            marshalStream.registerLinkTarget(object);
            marshalStream.dumpVariables(object.getMarshalVariableList());
        }

        @Override
        public void marshalTo(Object obj, RubyClass type, NewMarshal marshalStream, ThreadContext context, NewMarshal.RubyOutputStream out) {
            IRubyObject object = (IRubyObject) obj;

            marshalStream.registerLinkTarget(object);
            marshalStream.dumpVariables(context, out, object);
        }

        @Override
        public Object unmarshalFrom(Ruby runtime, RubyClass type, UnmarshalStream input) throws IOException {
            IRubyObject result = input.entry(type.allocate());

            input.ivar(null, result, null);

            return result;
        }
    };

    /**
     * Whether this class can be reified into a Java class. Currently only objects
     * that descend from Object (or descend from Ruby-based classes that descend
     * from Object) can be reified.
     * @param java If reified from java (out param)
     * @return true if the class can be reified, false otherwise. The out param indicate if it is java concrete reification 
     */
    public boolean isReifiable(boolean[] java) {
        // already reified is not reifiable
        if (reifiedClass != null) return false;

        final RubyClass realSuper;
        // root classes are not reifiable
        if (superClass == null || (realSuper = superClass.getRealClass()) == null) return false;

        Class reifiedSuper = realSuper.reifiedClass;

        // if super has been reified or is a native class
        if (reifiedSuper != null) {

            // super must be Object, BasicObject, or a reified user class
            boolean result = reifiedSuper == RubyObject.class ||
                    reifiedSuper == RubyBasicObject.class ||
                    Reified.class.isAssignableFrom(reifiedSuper);
            // TODO: check & test for nested java classes
            if (!result || ReifiedJavaProxy.class.isAssignableFrom(reifiedSuper)) java[0] = true;

            return true;
        } else {
            // non-native, non-reified super; recurse
            return realSuper.isReifiable(java);
        }
    }

    public void reifyWithAncestors() {
        reifyWithAncestors(null, true);
    }
    public void reifyWithAncestors(String classDumpDir) {
        reifyWithAncestors(classDumpDir, true);
    }
    public void reifyWithAncestors(boolean useChildLoader) {
        reifyWithAncestors(null, useChildLoader);
    }

    /**
     * Reify this class, first reifying all its ancestors. This causes the
     * reified class and all ancestors' reified classes to come into existence,
     * so any future changes will not be reflected.
     *
     * This form also accepts a string argument indicating a path in which to dump
     * the intermediate reified class bytes.
     *
     * @param classDumpDir the path in which to dump reified class bytes
     * @param useChildLoader whether to load the class into its own child classloader
     */
    public void reifyWithAncestors(String classDumpDir, boolean useChildLoader) {
        if (isReifiable(new boolean[] { false })) {
            RubyClass realSuper = getSuperClass().getRealClass();

            if (realSuper.reifiedClass == null) realSuper.reifyWithAncestors(classDumpDir, useChildLoader);
            reify(classDumpDir, useChildLoader);
        }
    }

    private static final boolean DEBUG_REIFY = false;

    public final void reify() {
        reify(null, true);
    }
    public final void reify(String classDumpDir) {
        reify(classDumpDir, true);
    }
    public final void reify(boolean useChildLoader) {
        reify(null, useChildLoader);
    }

    /**
     * Stand up a real Java class for the backing store of this object
     * @param classDumpDir Directory to save reified java class
     */
    public synchronized void reify(String classDumpDir, boolean useChildLoader) {
        var context = runtime.getCurrentContext();
        boolean[] java_box = { false };
        // re-check reifiable in case another reify call has jumped in ahead of us
        if (!isReifiable(java_box)) return;
        final boolean concreteExt = java_box[0];

        final Class<?> parentReified = superClass.getRealClass().reifiedClass();
        if (parentReified == null) throw typeError(context, getName() + "'s parent class is not yet reified");


        ClassDefiningClassLoader classLoader; // usually parent's class-loader
        if (parentReified.getClassLoader() instanceof OneShotClassLoader) {
            classLoader = (OneShotClassLoader) parentReified.getClassLoader();
        } else {
            if (useChildLoader) {
                MultiClassLoader parentLoader = new MultiClassLoader(runtime.getJRubyClassLoader());
                for(Loader cLoader : instanceConfig(context).getExtraLoaders()) {
                    parentLoader.addClassLoader(cLoader.getClassLoader());
                }
                classLoader = new OneShotClassLoader(parentLoader);
            } else {
                classLoader = context.runtime.getJRubyClassLoader();
            }
        }

        String javaName = getReifiedJavaClassName(context);
        // *might* need to include a Class identifier in the Java class name, since a Ruby class might be dropped
        // (using remove_const) and re-created in which case using the same name would cause a conflict...
        if (classLoader.hasDefinedClass(javaName)) { // as Ruby class dropping is "unusual" - assume v0 to be the raw name
            String versionedName; int v = 1;
            // NOTE: '@' is not supported in Ruby class names thus it's safe to use as a "separator"
            do {
                versionedName = javaName + "@v" + (v++); // rubyobj.SomeModule.Foo@v1
            } while (classLoader.hasDefinedClass(versionedName));
            javaName = versionedName;
        }
        final String javaPath = javaName.replace('.', '/');

        Reificator reifier;
        if (concreteExt) {
            reifier = new ConcreteJavaReifier(parentReified, javaName, javaPath);
        } else {
            Class<?> reifiedParent = superClass.reifiedClass;
            if (reifiedParent == null) reifiedParent = RubyObject.class;
            reifier = new MethodReificator(reifiedParent, javaName, javaPath, null, javaPath);
        }

        final byte[] classBytes = reifier.reify();

        boolean nearEnd = false;
        // Attempt to load the name we plan to use; skip reification if it exists already (see #1229).
        try {
            Class result = classLoader.defineClass(javaName, classBytes);
            dumpReifiedClass(context, classDumpDir, javaPath, classBytes);

            //Trigger initilization
            @SuppressWarnings("unchecked")
            java.lang.reflect.Field rt = result.getDeclaredField(BaseReificator.RUBY_FIELD);
            rt.setAccessible(true);
            if (rt.get(null) != runtime) throw new RuntimeException("No ruby field set!");

            if (concreteExt) {
                // setAllocator(ConcreteJavaProxy.ALLOCATOR); // this should be already set
                // Allocator "set" via clinit {@link JavaProxyClass#setProxyClassReified()}

                this.setInstanceVariable("@java_class", Java.wrapJavaObject(runtime, result));
                JavaProxy.setJavaClass(this, result);
                reifiedClassJava = Boolean.TRUE;
            } else {
                setRubyClassAllocator(result);
                reifiedClassJava = Boolean.FALSE;
            }
            reifiedClass = result;
            nearEnd = true;
            JavaProxyClass.ensureStaticIntConsumed();
            if (javaClassConfiguration.requestedStorageVariables != null)
                javaClassConfiguration.requestedStorageVariables.forEach(variableTableManager::requestFieldStorage);
            return; // success
        }
        catch (LinkageError error) { // fall through to failure path
            JavaProxyClass.addStaticInitLookup((Object[]) null); // wipe any local values not retrieved
            final String msg = error.getMessage();
            if ( msg != null && msg.contains("duplicate class definition for name") ) {
                logReifyException(error, false);
            }
            else {
                logReifyException(error, true);
            }
        }
        catch (Exception ex) {
            if (nearEnd) Helpers.throwException(ex);
            JavaProxyClass.addStaticInitLookup((Object[]) null); // wipe any local values not retrieved
            logReifyException(ex, true);
        }

        // If we get here, there's some other class in this classloader hierarchy with the same name. In order to
        // avoid a naming conflict, we set reified class to parent and skip reification.

        if (superClass.reifiedClass != null) {
            reifiedClass = superClass.reifiedClass;
            allocator = superClass.allocator;
        }
    }

    private String getReifiedJavaClassName(ThreadContext context) {
        final String basePackagePrefix = "rubyobj.";
        if (getBaseName() == null) { // anonymous Class instance: rubyobj.Class$0x1234abcd
            return basePackagePrefix + anonymousMetaNameWithIdentifier(context).replace(':', '$');
        }
        final CharSequence name = StringSupport.replaceAll(getName(), "::", ".");
        return basePackagePrefix + name; // TheFoo::Bar -> rubyobj.TheFoo.Bar
    }

    interface Reificator {
        byte[] reify();
    } // interface  Reificator
    
    private final static PositionAware defaultSimplePosition = new SimpleSourcePosition("<jruby-internal-reified>", 0);

    public PositionAware getPositionOrDefault(DynamicMethod method) {
        if (method instanceof PositionAware) {
            PositionAware pos = (PositionAware) method;
            return new SimpleSourcePosition(pos.getFile(), pos.getLine() + 1); // convert from 0-based to 1-based that
                                                                               // the JVM requires
        } else {
            return defaultSimplePosition;
        }
    }

    private abstract class BaseReificator implements Reificator {

        public final Class<?> reifiedParent;
        protected final String javaName;
        public final String javaPath;
        public final String rubyName;
        public final String rubyPath;
        protected final JavaClassConfiguration jcc;
        protected final ClassWriter cw;

        public final static String RUBY_FIELD = "ruby";
        public final static String RUBY_CLASS_FIELD = "rubyClass";

        BaseReificator(Class<?> reifiedParent, String javaName, String javaPath, String rubyName, String rubyPath) {
            this.reifiedParent = reifiedParent;
            this.javaName = javaName;
            this.javaPath = javaPath;
            this.rubyName = rubyName;
            this.rubyPath = rubyPath;
            jcc = getClassConfig();

            cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            cw.visit(RubyInstanceConfig.JAVA_VERSION, ACC_PUBLIC + ACC_SUPER, javaPath, null, p(reifiedParent),
                    interfaces());
            cw.visitSource("generated:Reificator@" + this.getClass().getName(), null);
        }

        @Override
        public byte[] reify() {

            // fields to hold Ruby and RubyClass references
            cw.visitField(ACC_SYNTHETIC | ACC_FINAL | ACC_STATIC | ACC_PRIVATE, RUBY_FIELD, ci(Ruby.class), null, null);
            cw.visitField(ACC_SYNTHETIC | ACC_FINAL | ACC_STATIC | ACC_PRIVATE, RUBY_CLASS_FIELD, ci(RubyClass.class), null, null);

            reifyConstructors();
            customReify();

            // static initializing method, note this is after the constructors to check for alloc-ables (see Concrete Java)
            SkinnyMethodAdapter m = new SkinnyMethodAdapter(cw, ACC_PUBLIC | ACC_STATIC, "<clinit>", sig(void.class),
                    null, null);
            m.start();
            reifyClinit(m);
            m.voidreturn();
            m.end();

            cw.visitEnd();

            return cw.toByteArray();
        }

        public abstract void reifyClinit(SkinnyMethodAdapter m);

        public abstract void customReify();

        private String[] interfaces() {
            final Class[] interfaces = Java.getInterfacesFromRubyClass(RubyClass.this);
            final String[] interfaceNames = new String[interfaces.length + 1];
            // mark this as a Reified class
            interfaceNames[0] = p(isRubyObject() ? Reified.class : ReifiedJavaProxy.class);
            // add the other user-specified interfaces
            for (int i = 0; i < interfaces.length; i++) {
                interfaceNames[i + 1] = p(interfaces[i]);
            }
            return interfaceNames;
        }

        protected boolean isRubyObject() {
            return true;
        }

        /**
         * Loads self (if local) or the rubyObject (if a java proxy) cast to a RubyBasicObject, as everything is a RBO
         * and it has a nicer interface
         */
        protected void loadRubyObject(SkinnyMethodAdapter m) {
            m.aload(0); // self
        }

        public void rubycall(SkinnyMethodAdapter m, String signature) {
            m.invokevirtual(rubyPath, "callMethod", signature);
        }

        protected void reifyConstructors() {
            // standard constructor that accepts Ruby, RubyClass. For use by JRuby (internally)
            SkinnyMethodAdapter m = new SkinnyMethodAdapter(cw, ACC_PUBLIC, "<init>",
                    sig(void.class, Ruby.class, RubyClass.class), null, null);
            m.aload(0); // uninitialized this
            m.aload(1); // ruby
            m.aload(2); // rubyclass
            allocAndInitialize(m, false);

            if (jcc.javaConstructable) {
                // no-arg constructor using static references to Ruby and RubyClass. For use by java
                m = new SkinnyMethodAdapter(cw, ACC_PUBLIC, "<init>", CodegenUtils.sig(void.class), null, null);
                m.aload(0); // uninitialized this
                m.getstatic(javaPath, RUBY_FIELD, ci(Ruby.class));
                m.getstatic(javaPath, RUBY_CLASS_FIELD, ci(RubyClass.class));
                allocAndInitialize(m, true);
            }
        }

        // java can't pass args to normal ruby classes right now, only concrete (below)
        protected void allocAndInitialize(SkinnyMethodAdapter m, boolean initIfAllowed) {
            m.invokespecial(p(reifiedParent), "<init>", sig(void.class, Ruby.class, RubyClass.class));
            if (jcc.callInitialize && initIfAllowed) { // if we want to initialize
                m.aload(0); // initialized this
                m.ldc(jcc.javaCtorMethodName);
                rubycall(m, sig(IRubyObject.class, String.class));
            }
            m.voidreturn();
            m.end();
        }

        public Class[] join(Class[] base, Class... extra) {
            Class[] more = ArraySupport.newCopy(base, base.length + extra.length);
            ArraySupport.copy(extra, more, base.length, extra.length);
            return more;
        }
    }

    private class MethodReificator extends BaseReificator {

        MethodReificator(Class<?> reifiedParent, String javaName, String javaPath, String rubyName, String rubyPath) {
            super(reifiedParent, javaName, javaPath, rubyName, rubyPath);
        }

        @Override
        public void customReify() {
            addClassAnnotations();

            // define fields
            defineFields();

            // gather a list of instance methods, so we don't accidentally make static ones that conflict
            final Set<String> instanceMethods = new HashSet<String>(getMethods().size());

            // define instance methods
            defineInstanceMethods(instanceMethods);

            // define class/static methods
            defineClassMethods(instanceMethods);
        }

        private void addClassAnnotations() {
            if (jcc.classAnnotations != null && !jcc.classAnnotations.isEmpty()) {
                for (Map.Entry<Class<?>,Map<String,Object>> entry : jcc.classAnnotations.entrySet()) {
                    Class<?> annoType = entry.getKey();
                    Map<String,Object> fields = entry.getValue();

                    AnnotationVisitor av = cw.visitAnnotation(ci(annoType), true);
                    CodegenUtils.visitAnnotationFields(av, fields);
                    av.visitEnd();
                }
            }
        }

        private void defineFields() {
            for (Map.Entry<String, Class<?>> fieldSignature : getFieldSignatures().entrySet()) {
                String fieldName = fieldSignature.getKey();
                Class<?> type = fieldSignature.getValue();
                Map<Class<?>, Map<String, Object>> fieldAnnos = getFieldAnnotations().get(fieldName);

                FieldVisitor fieldVisitor = cw.visitField(ACC_PUBLIC, fieldName, ci(type), null, null);

                if (fieldAnnos == null) continue;

                for (Map.Entry<Class<?>, Map<String, Object>> fieldAnno : fieldAnnos.entrySet()) {
                    Class<?> annoType = fieldAnno.getKey();
                    AnnotationVisitor av = fieldVisitor.visitAnnotation(ci(annoType), true);
                    CodegenUtils.visitAnnotationFields(av, fieldAnno.getValue());
                }
                fieldVisitor.visitEnd();
            }
        }

        private void defineClassMethods(Set<String> instanceMethods) {
            SkinnyMethodAdapter m;

            // define class/static methods
            for (Map.Entry<String, DynamicMethod> methodEntry : getMetaClass().getMethods().entrySet()) { // TODO: explicitly included but not-yet defined methods?
                String id = methodEntry.getKey();
                if (jcc.getExcluded().contains(id)) continue;

                String javaMethodName = JavaNameMangler.mangleMethodName(id);
                PositionAware position = getPositionOrDefault(methodEntry.getValue());
                if (position.getLine() > 1) cw.visitSource(position.getFile(), null);

                Map<Class<?>,Map<String,Object>> methodAnnos = getMetaClass().getMethodAnnotations().get(id);
                List<Map<Class<?>,Map<String,Object>>> parameterAnnos = getMetaClass().getParameterAnnotations().get(id);
                Class<?>[] methodSignature = getMetaClass().getMethodSignatures().get(id);

                String signature;
                if (methodSignature == null) {
                    if (!jcc.allClassMethods) continue;
                    Signature sig = methodEntry.getValue().getSignature();
                    // non-signature signature with just IRubyObject
                    if (sig.isNoArguments()) {
                        signature = sig(IRubyObject.class);
                        if (instanceMethods.contains(javaMethodName + signature)) continue;
                        m = new SkinnyMethodAdapter(cw, ACC_PUBLIC | ACC_STATIC, javaMethodName, signature, null, null);
                            m.line(position.getLine());
                        generateMethodAnnotations(methodAnnos, m, parameterAnnos);

                            m.getstatic(javaPath, RUBY_CLASS_FIELD, ci(RubyClass.class));
                        m.ldc(id);
                        m.invokevirtual("org/jruby/RubyClass", "callMethod", sig(IRubyObject.class, String.class));
                    } else {
                        signature = sig(IRubyObject.class, IRubyObject[].class);
                        if (instanceMethods.contains(javaMethodName + signature)) continue;
                        m = new SkinnyMethodAdapter(cw, ACC_PUBLIC | ACC_VARARGS | ACC_STATIC, javaMethodName, signature, null, null);
                            m.line(position.getLine());
                        generateMethodAnnotations(methodAnnos, m, parameterAnnos);

                            m.getstatic(javaPath, RUBY_CLASS_FIELD, ci(RubyClass.class));
                        m.ldc(id);
                            m.aload(0); // load the parameter array
                        m.invokevirtual("org/jruby/RubyClass", "callMethod", sig(IRubyObject.class, String.class, IRubyObject[].class) );
                    }
                    m.areturn();
                } else { // generate a real method signature for the method, with to/from coercions

                    // indices for temp values
                    Class<?>[] params = new Class[methodSignature.length - 1];
                    System.arraycopy(methodSignature, 1, params, 0, params.length);
                    final int baseIndex = RealClassGenerator.calcBaseIndex(params, 0);
                    int rubyIndex = baseIndex;

                    signature = sig(methodSignature[0], params);
                    if (instanceMethods.contains(javaMethodName + signature)) continue;
                    m = new SkinnyMethodAdapter(cw, ACC_PUBLIC | ACC_VARARGS | ACC_STATIC, javaMethodName, signature, null, null);
                    m.line(position.getLine());
                    generateMethodAnnotations(methodAnnos, m, parameterAnnos);

                    m.getstatic(javaPath, RUBY_FIELD, ci(Ruby.class));
                    m.astore(rubyIndex);

                    m.getstatic(javaPath, RUBY_CLASS_FIELD, ci(RubyClass.class));

                    m.ldc(id); // method name
                    RealClassGenerator.coerceArgumentsToRuby(m, params, rubyIndex);
                    m.invokevirtual("org/jruby/RubyClass", "callMethod", sig(IRubyObject.class, String.class, IRubyObject[].class));

                    RealClassGenerator.coerceResultAndReturn(m, methodSignature[0]);
                }

                if (DEBUG_REIFY) LOG.debug("defining {}.{} as {}.{}", getName(), id, javaName, javaMethodName + signature);

                m.end();
            }
        }

        //TODO: only generate that are overrideable (javaproxyclass)
        protected void defineInstanceMethods(Set<String> instanceMethods) {
            Set<String> defined = new HashSet<>();
            for (Map.Entry<String,DynamicMethod> methodEntry : getMethods().entrySet()) { // TODO: explicitly included but not-yet defined methods?
                final String id = methodEntry.getKey();
                final String callid = jcc.renamedMethods.getOrDefault(id, id);

                if (defined.contains(id) || jcc.getExcluded().contains(id)) continue;

                defined.add(callid); // id we won't see again, and are only defining java methods named id
                

                DynamicMethod method = methodEntry.getValue();
                if (id != callid) method = searchMethod(callid); // identity is fine as it's the default
                final Signature arity = method.getSignature();

                
                PositionAware position = getPositionOrDefault(methodEntry.getValue());
                if (position.getLine() > 1) cw.visitSource(position.getFile(), null);

                Class<?>[] methodSignature = getMethodSignatures().get(callid); // ruby side, use callid

                // for concrete extension, see if the method is one we are overriding,
                // even if we didn't specify it manually
                if (methodSignature == null) {
                    // TODO: should inherited search for java mangledName?
                    for (Class<?>[] sig : searchInheritedSignatures(id, arity)) { // id (vs callid) here as this is searching in java
                        String signature = defineInstanceMethod(id, callid, arity, position, sig);
                        if (signature != null) instanceMethods.add(signature);
                    }
                } else {
                    String signature = defineInstanceMethod(id, callid, arity, position, methodSignature);
                    if (signature != null) instanceMethods.add(signature);
                }
            }
        }
        
        protected String defineInstanceMethod(final String id, final String callid, final Signature sig,
                PositionAware position, Class<?>[] methodSignature) {
            String javaMethodName = JavaNameMangler.mangleMethodName(id);

            Map<Class<?>, Map<String, Object>> methodAnnos = getMethodAnnotations().get(callid); // ruby side, use callid
            List<Map<Class<?>, Map<String, Object>>> parameterAnnos = getParameterAnnotations().get(callid); // ruby side, use callid

            final String signature;
            SkinnyMethodAdapter m;
            if (methodSignature == null) { // non-signature signature with just IRubyObject
                if (!jcc.allMethods) return null;
                if (sig.isFixed()) {
                    switch (sig.required()) {
                        case 0:
                            signature = sig(IRubyObject.class); // return IRubyObject foo()
                            m = new SkinnyMethodAdapter(cw, ACC_PUBLIC, javaMethodName, signature, null, null);
                            m.line(position.getLine());
                            generateMethodAnnotations(methodAnnos, m, parameterAnnos);
                            generateObjectBarrier(m);

                            loadRubyObject(m); // self/rubyObject
                            m.ldc(callid);
                            rubycall(m, sig(IRubyObject.class, String.class));
                            break;
                        case 1:
                            signature = sig(IRubyObject.class, IRubyObject.class); // return IRubyObject foo(IRubyObject arg1)
                            m = new SkinnyMethodAdapter(cw, ACC_PUBLIC, javaMethodName, signature, null, null);
                            m.line(position.getLine());
                            generateMethodAnnotations(methodAnnos, m, parameterAnnos);
                            generateObjectBarrier(m);

                            loadRubyObject(m); // self/rubyObject
                            m.ldc(callid);
                            m.aload(1); // IRubyObject arg1
                            rubycall(m, sig(IRubyObject.class, String.class, IRubyObject.class));
                            break;
                        default:
                            // currently we only have :
                            //  callMethod(context, name)
                            //  callMethod(context, name, arg1)
                            // so for other arities use generic:
                            //  callMethod(context, name, args...)
                            final int paramCount = sig.required();
                            Class<?>[] params = new Class[paramCount];
                            Arrays.fill(params, IRubyObject.class);
                            signature = sig(IRubyObject.class, params);
                            m = new SkinnyMethodAdapter(cw, ACC_PUBLIC, javaMethodName, signature, null, null);
                            m.line(position.getLine());
                            generateMethodAnnotations(methodAnnos, m, parameterAnnos);
                            generateObjectBarrier(m);

                            loadRubyObject(m); // self/rubyObject
                            m.ldc(callid);

                            // generate an IRubyObject[] for the method arguments :
                            m.pushInt(paramCount);
                            m.anewarray(p(IRubyObject.class)); // new IRubyObject[size]
                            for (int i = 1; i <= paramCount; i++) {
                                m.dup();
                                m.pushInt(i - 1); // array index e.g. iconst_0
                                m.aload(i); // IRubyObject arg1, arg2 e.g. aload_1
                                m.aastore(); // arr[ i - 1 ] = arg_i
                            }
                            rubycall(m, sig(IRubyObject.class, String.class, IRubyObject[].class));
                    }
                } else {
                    // (generic) variable arity e.g. method(*args)
                    // NOTE: maybe improve to match fixed part for < -1 e.g. (IRubObject, IRubyObject, IRubyObject...)
                    signature = sig(IRubyObject.class, IRubyObject[].class);
                    m = new SkinnyMethodAdapter(cw, ACC_PUBLIC | ACC_VARARGS, javaMethodName, signature, null, null);
                    m.line(position.getLine());
                    generateMethodAnnotations(methodAnnos, m, parameterAnnos);
                    generateObjectBarrier(m);

                    loadRubyObject(m); // self/rubyObject
                    m.ldc(callid);
                    m.aload(1); // IRubyObject[] arg1
                    rubycall(m, sig(IRubyObject.class, String.class, IRubyObject[].class));
                }
                m.areturn();
            } else { // generate a real method signature for the method, with to/from coercions

                // indices for temp values
                Class<?>[] params = new Class[methodSignature.length - 1];
                ArraySupport.copy(methodSignature, 1, params, 0, params.length);
                final int baseIndex = RealClassGenerator.calcBaseIndex(params, 1);
                final int rubyIndex = baseIndex;

                signature = sig(methodSignature[0], params);
                int mod = ACC_PUBLIC;
                if ( isVarArgsSignature(callid, methodSignature) ) mod |= ACC_VARARGS;
                m = new SkinnyMethodAdapter(cw, mod, javaMethodName, signature, null, null);
                m.line(position.getLine());
                generateMethodAnnotations(methodAnnos, m, parameterAnnos);
                generateObjectBarrier(m);

                m.getstatic(javaPath, RUBY_FIELD, ci(Ruby.class)); // runtime
                m.astore(rubyIndex);

                loadRubyObject(m); // self/rubyObject
                m.ldc(callid); // method name

                RealClassGenerator.coerceArgumentsToRuby(m, params, rubyIndex);
                rubycall(m, sig(IRubyObject.class, String.class, IRubyObject[].class));
                RealClassGenerator.coerceResultAndReturn(m, methodSignature[0]);
                
                // generate any bridge methods needed as we overrode a defined one
                if (!isRubyObject())
                    generateSuperBridges(javaMethodName, methodSignature);
            }
            m.end();

            if (DEBUG_REIFY) LOG.debug("defining {}#{} (calling #{}) as {}#{}", getName(), id, callid, javaName, javaMethodName + signature);

            return javaMethodName + signature;
        }

        protected void generateSuperBridges(String javaMethodName, Class<?>[] methodSignature) {
            // Only for concrete java
        }

        /**
         * This method generates &lt;clinit> by marshaling the Ruby, RubyClass, etc variables through a static map
         * identified by integer in JavaProxyClass. Integers are serializable through bytecode generation so we can
         * share arbitrary objects with the generated class by saving them in {@link #getExtraClinitInfo()} via
         * {@link JavaProxyClass#addStaticInitLookup(Object...)} and {@link JavaProxyClass#getStaticInitLookup(int)}
         */
        @Override
        public void reifyClinit(SkinnyMethodAdapter m) {
            // top stack layout: ..., i0, o[], i1, o[]

            m.pushInt(1); // rubyclass index
            m.ldc(JavaProxyClass.addStaticInitLookup(getExtraClinitInfo()));
            m.invokestatic(p(JavaProxyClass.class), "getStaticInitLookup", sig(Object[].class, int.class));
            m.dup_x1(); // array
            m.dup_x2(); // array
            m.pushInt(0); // ruby index
            m.aaload(); // extract ruby
            m.checkcast(p(Ruby.class));
            m.putstatic(javaPath, RUBY_FIELD, ci(Ruby.class));
            m.aaload(); // extract rubyclass
            m.checkcast(p(RubyClass.class));
            m.putstatic(javaPath, RUBY_CLASS_FIELD, ci(RubyClass.class));
            extraClinitLookup(m);
        }

        protected Object[] getExtraClinitInfo() {
            return new Object[] { runtime, RubyClass.this };
        }

        /**
         * Override to save more values from the array in {@link #reifyClinit(SkinnyMethodAdapter)}
         */
        protected void extraClinitLookup(SkinnyMethodAdapter m) {
            m.pop();
        }

        protected Collection<Class<?>[]> searchInheritedSignatures(String id, Signature arity) {
            HashMap<String, Class<?>[]> types = new HashMap<>();

            for (Class<?> intf : Java.getInterfacesFromRubyClass(RubyClass.this)) {
                searchClassMethods(intf, arity, id, types);
            }

            if (types.isEmpty()) types.put("", null);
            return types.values();
        }

        protected Collection<Class<?>[]> searchClassMethods(Class<?> clz, Signature arity, String id,
                HashMap<String, Class<?>[]> options) {
            if (clz.getSuperclass() != null) searchClassMethods(clz.getSuperclass(), arity, id, options);
            for (Class<?> intf : clz.getInterfaces())
                searchClassMethods(intf, arity, id, options);
            for (Method method : clz.getDeclaredMethods()) {
                // TODO: java <-> ruby conversion?
                if (!method.getName().equals(id)) continue;
                final int mod = method.getModifiers();
                if (!Modifier.isPublic(mod) && !Modifier.isProtected(mod)) continue;
                if (Modifier.isFinal(mod)) continue;

                if (arity != null) {
                    // ensure arity is reasonable (ignores java varargs)
                    if (arity.isFixed()) {
                        if (arity.required() != method.getParameterCount()) continue;
                    } else if (arity.required() > method.getParameterCount()) {
                        continue;
                    }
                }

                // found! built a signature to return
                Class<?>[] types = join(new Class[] { method.getReturnType() }, method.getParameterTypes());
                options.put(sig(types), types);
            }
            // Note: not stable. May flicker between different arities. TODO: sort?
            return options.values();
        }

        protected void generateObjectBarrier(SkinnyMethodAdapter m) {
            // For non-concrete things, we ignore, as this is a RubyObject
        }

    } // class MethodReificator
    
    //public or private?
    public class ConcreteJavaReifier extends MethodReificator {
        // names follow pattern of `this$0` from javac nested classes to hopefully be ignored by 
        // sane reflection tools. Also similarly marked as synthetic
        public static final String RUBY_OBJECT_FIELD = "this$rubyObject";
        protected static final String RUBY_PROXY_CLASS_FIELD = "this$rubyProxyClass";
        public static final String RUBY_CTOR_CACHE_FIELD = "this$rubyCtorCache";
        
        JavaConstructor[] savedSuperCtors = null;
        Map<String, List<String>> supers = new HashMap<>();

        ConcreteJavaReifier(Class<?> reifiedParent, String javaName, String javaPath) {
            // In theory, we should operate on IRubyObject, but everything
            // that we need is a ConcreteJavaProxy, and it (via RubyBasicObject) has a nicer interface to boot
            super(reifiedParent, javaName, javaPath, ci(ConcreteJavaProxy.class), p(ConcreteJavaProxy.class));
        }

        @Override
        public void customReify() {
            super.customReify();

            defineInterfaceMethods();
        }

        @Override
        protected void loadRubyObject(SkinnyMethodAdapter m) {
            m.aload(0); // self
            m.getfield(javaPath, RUBY_OBJECT_FIELD, rubyName); // rubyObject
        }

        @Override
        public byte[] reify() {
            cw.visitField(ACC_SYNTHETIC | ACC_FINAL | ACC_PRIVATE, RUBY_OBJECT_FIELD, rubyName, null, null);
            cw.visitField(ACC_SYNTHETIC | ACC_FINAL | ACC_STATIC | ACC_PRIVATE, RUBY_PROXY_CLASS_FIELD,
                    ci(JavaProxyClass.class), null, null);
            cw.visitField(ACC_SYNTHETIC | ACC_FINAL | ACC_STATIC | ACC_PRIVATE, RUBY_CTOR_CACHE_FIELD,
                    ci(JCtorCache.class), null, null);
            return super.reify();
        }

        @Override
        protected boolean isRubyObject() {
            return false;
        }

        // also save the ordered array of constructors
        @Override
        protected Object[] getExtraClinitInfo() {
            return new Object[] { runtime, RubyClass.this, savedSuperCtors };
        }

        @Override
        protected void extraClinitLookup(SkinnyMethodAdapter m) {
            // extract cached ctors for lookup ordering

            // note: consume top of stack, lookuparray
            m.newobj(p(JCtorCache.class));
            m.dup_x1(); // jccache, lookuparray, jccache
            m.swap();// jccache, jccache, lookuparray
            m.pushInt(2); // ctor fields = index 2
            m.aaload(); // extract ctors, -> jccache, jccache, ctor[]
            m.checkcast(p(JavaConstructor[].class));
            m.invokespecial(p(JCtorCache.class), "<init>", sig(void.class, JavaConstructor[].class));
            m.putstatic(javaPath, RUBY_CTOR_CACHE_FIELD, ci(JCtorCache.class));

            // now create proxy class
            m.getstatic(javaPath, RUBY_FIELD, ci(Ruby.class));
            m.invokevirtual("org/jruby/Ruby", "getCurrentContext", "()Lorg/jruby/runtime/ThreadContext;");
            m.getstatic(javaPath, RUBY_CLASS_FIELD, ci(RubyClass.class));
            m.ldc(org.objectweb.asm.Type.getType("L" + javaPath + ";"));
            // if (simpleAlloc) // if simple, don't init, if complex, do init
            // m.iconst_0(); // false (as int)
            // else
            m.iconst_1(); // true (as int)

            m.invokestatic(p(JavaProxyClass.class), "setProxyClassReified",
                    sig(JavaProxyClass.class, ThreadContext.class, RubyClass.class, Class.class, boolean.class));
            m.dup();
            m.putstatic(javaPath, RUBY_PROXY_CLASS_FIELD, ci(JavaProxyClass.class));

            supers.forEach((name, sigs) -> {

                for (String sig : sigs) {
                    m.dup();
                    m.ldc(name);
                    m.ldc(sig);
                    m.iconst_1();
                    m.invokevirtual(p(JavaProxyClass.class), "initMethod",
                            sig(void.class, String.class, String.class, boolean.class));
                }
            });
            m.pop();
            // Note: no end, that's in the parent call
        }

        @Override
        protected void generateSuperBridges(String javaMethodName, Class<?>[] methodSignature) {
            // TODO: Would be good to cache, don't look up this interface/method repeatedly

            // don't look on interfaces, just the parent
            Class<?>[] args = new Class[methodSignature.length - 1];
            ArraySupport.copy(methodSignature, 1, args, 0, methodSignature.length - 1);
            Method supr = findTarget(reifiedParent, javaMethodName, methodSignature[0], args);
            if (supr == null) return;

            SkinnyMethodAdapter m = new SkinnyMethodAdapter(cw, ACC_SYNTHETIC | ACC_BRIDGE | ACC_PUBLIC,
                    JavaProxyClass.generateSuperName(javaName, javaMethodName), sig(methodSignature), null, null);
            GeneratorAdapter ga = RealClassGenerator.makeGenerator(m);
            ga.loadThis();
            ga.loadArgs();
            m.invokespecial(p(reifiedParent), javaMethodName, sig(methodSignature));
            ga.returnValue();
            ga.endMethod();

            if (!supers.containsKey(javaMethodName)) supers.put(javaMethodName, new ArrayList<>());

            supers.get(javaMethodName).add(sig(methodSignature));
        }

        private Method findTarget(Class<?> clz, String javaMethodName, Class<?> returns, Class<?>[] params) {
            for (Method method : clz.getDeclaredMethods()) {
                if (!method.getName().equals(javaMethodName)) continue;
                final int mod = method.getModifiers();
                if (!Modifier.isPublic(mod) && !Modifier.isProtected(mod)) continue;
                if (Modifier.isAbstract(mod) || Modifier.isFinal(mod)) continue;

                // TODO: is args necessary?
                if (!method.getReturnType().equals(returns)) continue;
                if (!Arrays.equals(method.getParameterTypes(), params)) continue;

                return method;
            }
            if (clz.getSuperclass() != null) return findTarget(clz.getSuperclass(), javaMethodName, returns, params);
            return null;
        }

        @Override
        protected Collection<Class<?>[]> searchInheritedSignatures(String id, Signature arity) {
            HashMap<String, Class<?>[]> types = new HashMap<>();
            searchClassMethods(reifiedParent, arity, id, types);
            for (Class<?> intf : Java.getInterfacesFromRubyClass(RubyClass.this)) {// this pattern is duplicated a lot. refactor?
                searchClassMethods(intf, arity, id, types);
            }

            if (types.isEmpty()) {
                searchClassMethods(reifiedParent, null, id, types);
                for (Class<?> intf : Java.getInterfacesFromRubyClass(RubyClass.this)) {
                    searchClassMethods(intf, null, id, types);
                }
            }
            if (types.isEmpty()) types.put("", null);

            return types.values();
        }

        @Override
        protected void reifyConstructors() {
            Optional<Constructor<?>> zeroArg = Optional.empty();
            List<Constructor<?>> candidates = new ArrayList<>();
            for (Constructor<?> constructor : reifiedParent.getDeclaredConstructors()) {
                final int mod = constructor.getModifiers();
                if (!Modifier.isPublic(mod) && !Modifier.isProtected(mod)) continue;
                candidates.add(constructor);
                if (constructor.getParameterCount() == 0) zeroArg = Optional.of(constructor); // TODO: varargs?
            }
            boolean isNestedRuby = ReifiedJavaProxy.class.isAssignableFrom(reifiedParent);

            // update the source location
            DynamicMethod methodEntry = searchMethod(jcc.javaCtorMethodName);
            PositionAware position = getPositionOrDefault(methodEntry);
            cw.visitSource(position.getFile(), null);
            int superpos = ConcreteJavaProxy.findSuperLine(runtime, methodEntry, position.getLine());
            Set<String> generatedCtors = new HashSet<>();

            if (candidates.size() > 0) { // TODO: doc: implies javaConstructable?
                List<JavaConstructor> savedCtorsList = new ArrayList<>(candidates.size());
                for (Constructor<?> constructor : candidates) {
                    savedCtorsList.add(JavaConstructor.wrap(constructor));
                }
                savedSuperCtors = savedCtorsList.toArray(new JavaConstructor[savedCtorsList.size()]);
            } else {
                // TODO: copy validateArgs
                // TODO: no ctors = error?
                throw typeError(runtime.getCurrentContext(), "class " + reifiedParent.getName() +
                        " doesn't have a public or protected constructor");
            }

            if (zeroArg.isPresent()) {
                // standard constructor that accepts Ruby, RubyClass. For use by JRuby (internally)
                if (!jcc.allCtors) {
                    if (!isNestedRuby) {
                        generatedCtors.add(RealClassGenerator.makeConcreteConstructorProxy(cw, position, true, this,
                                new Class[0], isNestedRuby));
                    }

                    if (jcc.javaConstructable) {
                        generatedCtors.add(RealClassGenerator.makeConcreteConstructorProxy(cw, position, false, this,
                                new Class[0], isNestedRuby));
                    }
                }
            }

            // TODO: remove rubyCtors if IRO is enabled (by default)
            if (jcc.allCtors && !isNestedRuby) {
                for (Constructor<?> constructor : candidates) {
                    if (jcc.rubyConstructable) generatedCtors.add(RealClassGenerator.makeConcreteConstructorProxy(cw,
                            position, true, this, constructor.getParameterTypes(), false));

                    if (jcc.javaConstructable) generatedCtors.add(RealClassGenerator.makeConcreteConstructorProxy(cw,
                            position, false, this, constructor.getParameterTypes(), false));

                }
            }

            if (jcc.extraCtors != null && jcc.extraCtors.size() > 0) {
                for (Class<?>[] constructor : jcc.extraCtors) {
                    // TODO: support annotations in ctor params

                    if (jcc.rubyConstructable && !generatedCtors.contains(sig(void.class, join(constructor, Ruby.class, RubyClass.class)))) {
                        generatedCtors.add(RealClassGenerator.makeConcreteConstructorProxy(cw, position, true, this,
                                constructor, isNestedRuby));
                    }

                    if (jcc.javaConstructable && !generatedCtors.contains(sig(void.class, constructor))) {
                        generatedCtors.add(RealClassGenerator.makeConcreteConstructorProxy(cw, position, false, this,
                                constructor, isNestedRuby));
                    }
                }
            }
            if (jcc.IroCtors) {
                RealClassGenerator.makeConcreteConstructorIROProxy(cw, position, this);
            } else if (generatedCtors.size() == 0) {
                //TODO: Warn for static classe?
                throw typeError(runtime.getCurrentContext(), "class " + getName() + " doesn't have any exposed java constructors");
            }
            
            // generate the real (IRubyObject) ctor. All other ctor generated proxy to this one
            RealClassGenerator.makeConcreteConstructorSwitch(cw, position, superpos, isNestedRuby, this,
                    savedSuperCtors);
        }

        /**
         * Generates an init barrier. NOT Thread-safe, but hopefully nobody has threads in their constructor? This is
         * used to ensure that self.to_java is valid if the super ctor calls an abstract method that is re-implemented
         * by ruby
         */
        @Override
        protected void generateObjectBarrier(SkinnyMethodAdapter m) {
            // For non-concrete things, we check, as this is not a RubyObject
            m.aload(0);
            m.getfield(javaPath, RUBY_OBJECT_FIELD, rubyName);
            m.aload(0);
            m.invokevirtual(rubyPath, "ensureThis", sig(void.class, Object.class));
        }

        private void defineInterfaceMethods() {
            SkinnyMethodAdapter m = new SkinnyMethodAdapter(cw, ACC_SYNTHETIC | ACC_PUBLIC, "___jruby$rubyObject",
                    sig(IRubyObject.class), null, null);
            m.aload(0); // this
            m.getfield(javaPath, RUBY_OBJECT_FIELD, rubyName);
            m.areturn();
            m.end();

            m = new SkinnyMethodAdapter(cw, ACC_SYNTHETIC | ACC_PUBLIC, "___jruby$proxyClass",
                    sig(JavaProxyClass.class), null, null);
            m.getstatic(javaPath, RUBY_PROXY_CLASS_FIELD, ci(JavaProxyClass.class));
            m.areturn();
            m.end();
        }

    } // class ConcreteJavaReifier

    private boolean isVarArgsSignature(final String method, final Class[] methodSignature) {
        // TODO we should simply detect "java.lang.Object m1(java.lang.Object... args)"
        // var-args distinguished from  "java.lang.Object m2(java.lang.Object[]  args)"
        return methodSignature.length > 1 && // methodSignature[0] is return value
               methodSignature[ methodSignature.length - 1 ].isArray() ;
    }

    private void logReifyException(final Throwable failure, final boolean error) {
        if (RubyInstanceConfig.REIFY_LOG_ERRORS) {
            if ( error ) LOG.error("failed to reify class " + getName() + " due to: ", failure);
            else LOG.info("failed to reify class " + getName() + " due to: ", failure);
        }
    }

    /**
     * @param reifiedClass
     * @deprecated Use {@link org.jruby.RubyClass#reifiedClass(Class)} instead.
     */
    @Deprecated(since = "10.0")
    public void setReifiedClass(Class<? extends IRubyObject> reifiedClass) {
        this.reifiedClass = (Class<? extends Reified>) reifiedClass; // Not always true
    }

    /**
     * @return
     * @deprecated Use {@link RubyClass#reifiedClass()} instead.
     */
    @Deprecated(since = "10.0")
    public Class<? extends Reified> getReifiedClass() {
        return reifiedClass();
    }

    /**
     * Gets a reified Ruby or Java class.
     * To ensure a specific type, see {@link #getReifiedRubyClass()} or  {@link #getReifiedJavaClass()}
     */
    public Class<? extends Reified> reifiedClass() {
        return reifiedClass;
    }

    /**
     * Gets a reified Ruby class. Throws if this is a Java class
     */
    public Class<? extends IRubyObject> getReifiedRubyClass() {
        if (reifiedClassJava == Boolean.TRUE) throw typeError(runtime.getCurrentContext(), "Attempted to get a Ruby class for a Java class");

        return (Class<? extends IRubyObject>) reifiedClass;
    }

    /**
     * Gets a reified Java class. Throws if this is a Ruby class
     */
    public Class<? extends ReifiedJavaProxy> getReifiedJavaClass() {
        // TODO: error type
        if (reifiedClassJava == Boolean.FALSE) throw typeError(runtime.getCurrentContext(), "Attempted to get a Java class for a Ruby class");

        return (Class<? extends ReifiedJavaProxy>) reifiedClass;
    }

    /**
     * If the current class is reified (null otherwise) and if it extends java
     * @return Null if not reified, true if a concrete extension class, and false if a ruby class
     */
    public Boolean getIsReifiedExtendedJavaClass() {
        return reifiedClassJava;
    }

    public static Class<?> nearestReifiedClass(final RubyClass klass) {
        RubyClass current = klass;
        do {
            Class<?> reified = current.reifiedClass();
            if ( reified != null ) return reified;
            current = current.getSuperClass();
        }
        while ( current != null );
        return null;
    }

    public Map<String, List<Map<Class<?>, Map<String,Object>>>> getParameterAnnotations() {
        if (javaClassConfiguration == null || getClassConfig().parameterAnnotations == null) return Collections.EMPTY_MAP;
        return javaClassConfiguration.parameterAnnotations;
    }

    public synchronized void addParameterAnnotation(String method, int i, Class<?> annoClass, Map<String,Object> value) {
        if (getClassConfig().parameterAnnotations == null) javaClassConfiguration.parameterAnnotations = new HashMap<>(8);
        List<Map<Class<?>,Map<String,Object>>> paramList = javaClassConfiguration.parameterAnnotations.get(method);
        if (paramList == null) {
            paramList = new ArrayList<>(i + 1);
            javaClassConfiguration.parameterAnnotations.put(method, paramList);
        }
        if (paramList.size() < i + 1) {
            for (int j = paramList.size(); j < i + 1; j++) {
                paramList.add(null);
            }
        }
        if (annoClass != null && value != null) {
            Map<Class<?>, Map<String, Object>> annos = paramList.get(i);
            if (annos == null) {
                paramList.set(i, annos = new LinkedHashMap<>(4));
            }
            annos.put(annoClass, value);
        } else {
            paramList.set(i, null);
        }
    }

    public Map<String,Map<Class<?>,Map<String,Object>>> getMethodAnnotations() {
        if (javaClassConfiguration == null || getClassConfig().methodAnnotations == null) return Collections.EMPTY_MAP;

        return javaClassConfiguration.methodAnnotations;
    }

    public Map<String,Map<Class<?>,Map<String,Object>>> getFieldAnnotations() {
        if (javaClassConfiguration == null || getClassConfig().fieldAnnotations == null) return Collections.EMPTY_MAP;

        return javaClassConfiguration.fieldAnnotations;
    }

    public synchronized void addMethodAnnotation(String methodName, Class<?> annotation, Map fields) {
        if (getClassConfig().methodAnnotations == null) javaClassConfiguration.methodAnnotations = new HashMap<>(8);

        Map<Class<?>,Map<String,Object>> annos = javaClassConfiguration.methodAnnotations.get(methodName);
        if (annos == null) {
            javaClassConfiguration.methodAnnotations.put(methodName, annos = new LinkedHashMap<>(4));
        }

        annos.put(annotation, fields);
    }

    public synchronized void addFieldAnnotation(String fieldName, Class<?> annotation, Map fields) {
        if (getClassConfig().fieldAnnotations == null) javaClassConfiguration.fieldAnnotations = new HashMap<>(8);

        Map<Class<?>,Map<String,Object>> annos = javaClassConfiguration.fieldAnnotations.get(fieldName);
        if (annos == null) {
            javaClassConfiguration.fieldAnnotations.put(fieldName, annos = new LinkedHashMap<>(4));
        }

        annos.put(annotation, fields);
    }

    public Map<String,Class<?>[]> getMethodSignatures() {
        if (javaClassConfiguration == null || getClassConfig().methodSignatures == null) return Collections.EMPTY_MAP;

        return javaClassConfiguration.methodSignatures.entrySet()
                .stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().get(0)));
    }
    
    public Map<String,List<Class<?>[]>> getAllMethodSignatures() {
        if (javaClassConfiguration == null || getClassConfig().methodSignatures == null) return Collections.EMPTY_MAP;

        return javaClassConfiguration.methodSignatures;
    }

    public Map<String, Class<?>> getFieldSignatures() {
        if (javaClassConfiguration == null || getClassConfig().fieldSignatures == null) return Collections.EMPTY_MAP;

        return javaClassConfiguration.fieldSignatures;
    }

    public synchronized void addMethodSignature(String methodName, Class<?>[] types) {
        if (getClassConfig().methodSignatures == null) javaClassConfiguration.methodSignatures = new HashMap<>(16);
        
        List<Class<?>[]> annos = javaClassConfiguration.methodSignatures.get(methodName);
        if (annos == null) {
            javaClassConfiguration.methodSignatures.put(methodName, annos = new ArrayList<Class<?>[]>(4));
        }

        annos.add(types);
    }

    public synchronized void addFieldSignature(String fieldName, Class<?> type) {
        if (getClassConfig().fieldSignatures == null) javaClassConfiguration.fieldSignatures = new LinkedHashMap<>(8);

        javaClassConfiguration.fieldSignatures.put(fieldName, type);
    }

    public Map<Class<?>,Map<String,Object>> getClassAnnotations() {
        if (javaClassConfiguration == null || getClassConfig().classAnnotations == null) return Collections.EMPTY_MAP;

        return javaClassConfiguration.classAnnotations;
    }

    public synchronized void addClassAnnotation(Class<?> annotation, Map fields) {
        if (getClassConfig().classAnnotations == null) javaClassConfiguration.classAnnotations = new LinkedHashMap<>(4);

        javaClassConfiguration.classAnnotations.put(annotation, fields);
    }

    public synchronized JavaClassConfiguration getClassConfig() {
        if (javaClassConfiguration == null) javaClassConfiguration = new JavaClassConfiguration();

        return javaClassConfiguration;
    }

    public synchronized void setClassConfig(JavaClassConfiguration jcc) {
        javaClassConfiguration = jcc;
    }

    @Override
    public <T> T toJava(Class<T> target) {
        if (target == Class.class) {
            if (reifiedClass == null) reifyWithAncestors(); // possibly auto-reify
            // Class requested; try java_class or else return nearest reified class
            Class<?> javaClass = JavaUtil.getJavaClass(this, null);
            if (javaClass != null) return (T) javaClass;

            Class<?> reifiedClass = nearestReifiedClass(this);
            if ( reifiedClass != null ) return target.cast(reifiedClass);
            // should never fall through, since RubyObject has a reified class
        }

        if (target.isAssignableFrom(RubyClass.class)) {
            // they're asking for something RubyClass extends, give them that
            return target.cast(this);
        }

        return defaultToJava(target);
    }

    /**
     * An enum defining the type of marshaling a given class's objects employ.
     */
    private enum MarshalType { DEFAULT, NEW_USER, OLD_USER, DEFAULT_SLOW, NEW_USER_SLOW, USER_SLOW }

    /**
     * A tuple representing the mechanism by which objects should be marshaled.
     *
     * This tuple caches the type of marshaling to perform (from @MarshalType),
     * the method to be used for marshaling data (either marshal_load/dump or
     * _load/_dump), and the generation of the class at the time this tuple was
     * created. When "dump" or "load" are invoked, they either call the default
     * marshaling logic (@MarshalType.DEFAULT) or they further invoke the cached
     * marshal_dump/load or _dump/_load methods to marshal the data.
     *
     * It is expected that code outside MarshalTuple will validate that the
     * generation number still matches before invoking load or dump.
     */
    private static class MarshalTuple {
        /**
         * Construct a new MarshalTuple with the given values.
         *
         * @param entry The method entry to invoke, or null in the case of default
         * marshaling.
         * @param type The type of marshaling to perform, from @MarshalType
         * @param generation The generation of the associated class at the time
         * of creation.
         */
        public MarshalTuple(CacheEntry entry, MarshalType type, int generation) {
            this.entry = entry;
            this.type = type;
            this.generation = generation;
        }

        /**
         * Dump the given object to the given stream, using the appropriate
         * marshaling method.
         *
         * @param stream The stream to which to dump
         * @param object The object to dump
         * @throws IOException If there is an IO error during dumping
         */
        public void dump(MarshalStream stream, IRubyObject object) throws IOException {
            switch (type) {
                case DEFAULT:
                    stream.writeDirectly(object);
                    return;
                case NEW_USER:
                    stream.userNewMarshal(object, entry);
                    return;
                case OLD_USER:
                    stream.userMarshal(object, entry);
                    return;
                case DEFAULT_SLOW:
                    if (object.respondsTo("marshal_dump")) {
                        stream.userNewMarshal(object);
                    } else if (object.respondsTo("_dump")) {
                        stream.userMarshal(object);
                    } else {
                        stream.writeDirectly(object);
                    }
            }
        }

        public void dump(NewMarshal stream, ThreadContext context, NewMarshal.RubyOutputStream out, IRubyObject object) {
            switch (type) {
                case DEFAULT:
                    stream.writeDirectly(context, out, object);
                    return;
                case NEW_USER:
                    stream.userNewMarshal(context, out, object, entry);
                    return;
                case OLD_USER:
                    stream.userMarshal(context, out, object, entry);
                    return;
                case DEFAULT_SLOW:
                    if (object.respondsTo("marshal_dump")) {
                        stream.userNewMarshal(context, out, object);
                    } else if (object.respondsTo("_dump")) {
                        stream.userMarshal(context, out, object);
                    } else {
                        stream.writeDirectly(context, out, object);
                    }
            }
        }

        /** A "null" tuple, used as the default value for caches. */
        public static final MarshalTuple NULL_TUPLE = new MarshalTuple(null, null, 0);
        /** The method associated with this tuple. */
        public final CacheEntry entry;
        /** The type of marshaling that will be performed */
        public final MarshalType type;
        /** The generation of the associated class at the time of creation */
        public final int generation;
    }

    /**
     * Marshal the given object to the marshaling stream, being "smart" and
     * caching how to do that marshaling.
     *
     * If the class defines a custom "respond_to?" method, then the behavior of
     * dumping could vary without our class structure knowing it. As a result,
     * we do only the slow-path classic behavior.
     *
     * If the class defines a real "marshal_dump" method, we cache and use that.
     *
     * If the class defines a real "_dump" method, we cache and use that.
     *
     * If the class neither defines none of the above methods, we use a fast
     * path directly to the default dumping logic.
     *
     * @param stream The stream to which to marshal the data
     * @param target The object whose data should be marshaled
     * @throws IOException If there is an IO exception while writing to the
     * stream.
     */
    public void smartDump(MarshalStream stream, IRubyObject target) throws IOException {
        MarshalTuple tuple;
        if ((tuple = cachedDumpMarshal).generation == generation) {
        } else {
            // recache
            CacheEntry entry = searchWithCache("respond_to?");
            DynamicMethod method = entry.method;
            if (!method.equals(runtime.getRespondToMethod()) && !method.isUndefined()) {

                // custom respond_to?, always do slow default marshaling
                tuple = (cachedDumpMarshal = new MarshalTuple(null, MarshalType.DEFAULT_SLOW, generation));

            } else if (!(entry = searchWithCache("marshal_dump")).method.isUndefined()) {

                // object really has 'marshal_dump', cache "new" user marshaling
                tuple = (cachedDumpMarshal = new MarshalTuple(entry, MarshalType.NEW_USER, generation));

            } else if (!(entry = searchWithCache("_dump")).method.isUndefined()) {

                // object really has '_dump', cache "old" user marshaling
                tuple = (cachedDumpMarshal = new MarshalTuple(entry, MarshalType.OLD_USER, generation));

            } else {

                // no respond_to?, marshal_dump, or _dump, so cache default marshaling
                tuple = (cachedDumpMarshal = new MarshalTuple(null, MarshalType.DEFAULT, generation));
            }
        }

        tuple.dump(stream, target);
    }

    public void smartDump(NewMarshal stream, ThreadContext context, NewMarshal.RubyOutputStream out, IRubyObject target) {
        MarshalTuple tuple;
        if ((tuple = cachedDumpMarshal).generation == generation) {
        } else {
            // recache
            CacheEntry entry = searchWithCache("respond_to?");
            DynamicMethod method = entry.method;
            if (!method.equals(runtime.getRespondToMethod()) && !method.isUndefined()) {

                // custom respond_to?, always do slow default marshaling
                tuple = (cachedDumpMarshal = new MarshalTuple(null, MarshalType.DEFAULT_SLOW, generation));

            } else if (!(entry = searchWithCache("marshal_dump")).method.isUndefined()) {

                // object really has 'marshal_dump', cache "new" user marshaling
                tuple = (cachedDumpMarshal = new MarshalTuple(entry, MarshalType.NEW_USER, generation));

            } else if (!(entry = searchWithCache("_dump")).method.isUndefined()) {

                // object really has '_dump', cache "old" user marshaling
                tuple = (cachedDumpMarshal = new MarshalTuple(entry, MarshalType.OLD_USER, generation));

            } else {

                // no respond_to?, marshal_dump, or _dump, so cache default marshaling
                tuple = (cachedDumpMarshal = new MarshalTuple(null, MarshalType.DEFAULT, generation));
            }
        }

        tuple.dump(stream, context, out, target);
    }

    /**
     * Load marshaled data into a blank target object using marshal_load, being
     * "smart" and caching the mechanism for invoking marshal_load.
     *
     * If the class implements a custom respond_to?, cache nothing and go slow
     * path invocation of respond_to? and marshal_load every time. Raise error
     * if respond_to? :marshal_load returns true and no :marshal_load is
     * defined.
     *
     * If the class implements marshal_load, cache and use that.
     *
     * Otherwise, error, since marshal_load is not present.
     *
     * @param target The blank target object into which marshal_load will
     * deserialize the given data
     * @param data The marshaled data
     * @return The fully-populated target object
     */
    public IRubyObject smartLoadNewUser(IRubyObject target, IRubyObject data) {
        ThreadContext context = runtime.getCurrentContext();
        CacheEntry cache;
        if ((cache = cachedLoad).token == generation) {
            cache.method.call(context, target, cache.sourceModule, "marshal_load", data);
            return target;
        } else {
            cache = searchWithCache("respond_to?");
            DynamicMethod method = cache.method;
            if (!method.equals(runtime.getRespondToMethod()) && !method.isUndefined()) {

                // custom respond_to?, cache nothing and use slow path
                if (method.call(context, target, cache.sourceModule, "respond_to?", asSymbol(context, "marshal_load")).isTrue()) {
                    target.callMethod(context, "marshal_load", data);
                    return target;
                } else {
                    throw typeError(context, "class ", this, " needs to have method `marshal_load'");
                }

            } else if (!(cache = searchWithCache("marshal_load")).method.isUndefined()) {

                // real marshal_load defined, cache and call it
                cachedLoad = cache;
                cache.method.call(context, target, cache.sourceModule, "marshal_load", data);
                return target;

            } else {

                // go ahead and call, method_missing might handle it
                target.callMethod(context, "marshal_load", data);
                return target;

            }
        }
    }


    /**
     * Load marshaled data into a blank target object using _load, being
     * "smart" and caching the mechanism for invoking _load.
     *
     * If the metaclass implements custom respond_to?, cache nothing and go slow
     * path invocation of respond_to? and _load every time. Raise error if
     * respond_to? :_load returns true and no :_load is defined.
     *
     * If the metaclass implements _load, cache and use that.
     *
     * Otherwise, error, since _load is not present.
     *
     * @param data The marshaled data, to be reconstituted into an object by
     * _load
     * @return The fully-populated target object
     */
    public IRubyObject smartLoadOldUser(IRubyObject data) {
        ThreadContext context = runtime.getCurrentContext();
        var singleton = singletonClass(context);
        CacheEntry cache = singleton.cachedLoad;
        if (cache.token == singleton.generation) return cache.method.call(context, this, cache.sourceModule, "_load", data);

        cache = singleton.searchWithCache("respond_to?");
        DynamicMethod method = cache.method;
        if (!method.equals(runtime.getRespondToMethod()) && !method.isUndefined()) {
            // custom respond_to?, cache nothing and use slow path
            if (method.call(context, this, cache.sourceModule, "respond_to?", asSymbol(context, "_load")).isTrue()) {
                return callMethod(context, "_load", data);
            } else {
                throw typeError(context, "class ", this, " needs to have method `_load'");
            }
        } else if (!(cache = singleton.searchWithCache("_load")).method.isUndefined()) {
            // real _load defined, cache and call it
            singleton.cachedLoad = cache;
            return cache.method.call(context, this, cache.sourceModule, "_load", data);

        } else {
            // provide an error, since it doesn't exist
            throw typeError(context, "class ", this, " needs to have method `_load'");
        }
    }

    // DEPRECATED METHODS

    @Deprecated
    public IRubyObject invoke(ThreadContext context, IRubyObject self, int methodIndex, String name, IRubyObject[] args, CallType callType, Block block) {
        return invoke(context, self, name, args, callType, block);
    }

    /**
     * This method is deprecated because it depends on having a Ruby frame pushed for checking method visibility,
     * and there's no way to enforce that. Most users of this method probably don't need to check visibility.
     *
     * See https://github.com/jruby/jruby/issues/4134
     *
     * @deprecated Use finvoke if you do not want visibility-checking or invokeFrom if you do.
     */
    @Deprecated(since = "9.4-", forRemoval = true)
    public IRubyObject invoke(ThreadContext context, IRubyObject self, String name,
                              CallType callType, Block block) {
        CacheEntry entry = searchWithCache(name);
        DynamicMethod method = entry.method;
        IRubyObject caller = context.getFrameSelf();

        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, this, method.getVisibility(), name, callType, block);
        }
        return method.call(context, self, entry.sourceModule, name, block);
    }

    /**
     * This method is deprecated because it depends on having a Ruby frame pushed for checking method visibility,
     * and there's no way to enforce that. Most users of this method probably don't need to check visibility.
     *
     * See https://github.com/jruby/jruby/issues/4134
     *
     * @deprecated Use finvoke if you do not want visibility-checking or invokeFrom if you do.
     */
    @Deprecated(since = "9.4-")
    public IRubyObject invoke(ThreadContext context, IRubyObject self, String name,
                              IRubyObject[] args, CallType callType, Block block) {
        assert args != null;
        CacheEntry entry = searchWithCache(name);
        DynamicMethod method = entry.method;
        IRubyObject caller = context.getFrameSelf();

        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, this, method.getVisibility(), name, callType, args, block);
        }
        return method.call(context, self, entry.sourceModule, name, args, block);
    }

    /**
     * This method is deprecated because it depends on having a Ruby frame pushed for checking method visibility,
     * and there's no way to enforce that. Most users of this method probably don't need to check visibility.
     *
     * See https://github.com/jruby/jruby/issues/4134
     *
     * @deprecated Use finvoke if you do not want visibility-checking or invokeFrom if you do.
     */
    @Deprecated(since = "9.4-")
    public IRubyObject invoke(ThreadContext context, IRubyObject self, String name,
                              IRubyObject arg, CallType callType, Block block) {
        CacheEntry entry = searchWithCache(name);
        DynamicMethod method = entry.method;
        IRubyObject caller = context.getFrameSelf();

        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, this, method.getVisibility(), name, callType, arg, block);
        }
        return method.call(context, self, entry.sourceModule, name, arg, block);
    }

    /**
     * This method is deprecated because it depends on having a Ruby frame pushed for checking method visibility,
     * and there's no way to enforce that. Most users of this method probably don't need to check visibility.
     *
     * See https://github.com/jruby/jruby/issues/4134
     *
     * @deprecated Use finvoke if you do not want visibility-checking or invokeFrom if you do.
     */
    @Deprecated(since = "9.4-", forRemoval = true)
    public IRubyObject invoke(ThreadContext context, IRubyObject self, String name,
                              IRubyObject arg0, IRubyObject arg1, CallType callType, Block block) {
        CacheEntry entry = searchWithCache(name);
        DynamicMethod method = entry.method;
        IRubyObject caller = context.getFrameSelf();

        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, this, method.getVisibility(), name, callType, arg0, arg1, block);
        }
        return method.call(context, self, entry.sourceModule, name, arg0, arg1, block);
    }

    /**
     * This method is deprecated because it depends on having a Ruby frame pushed for checking method visibility,
     * and there's no way to enforce that. Most users of this method probably don't need to check visibility.
     *
     * See https://github.com/jruby/jruby/issues/4134
     *
     * @deprecated Use finvoke if you do not want visibility-checking or invokeFrom if you do.
     */
    @Deprecated(since = "9.4-", forRemoval = true)
    public IRubyObject invoke(ThreadContext context, IRubyObject self, String name,
                              IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, CallType callType, Block block) {
        CacheEntry entry = searchWithCache(name);
        DynamicMethod method = entry.method;
        IRubyObject caller = context.getFrameSelf();

        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, this, method.getVisibility(), name, callType, arg0, arg1, arg2, block);
        }
        return method.call(context, self, entry.sourceModule, name, arg0, arg1, arg2, block);
    }

    /**
     * This method is deprecated because it depends on having a Ruby frame pushed for checking method visibility,
     * and there's no way to enforce that. Most users of this method probably don't need to check visibility.
     *
     * See https://github.com/jruby/jruby/issues/4134
     *
     * @deprecated Use finvoke if you do not want visibility-checking or invokeFrom if you do.
     */
    @Deprecated(since = "9.4-", forRemoval = true)
    public IRubyObject invoke(ThreadContext context, IRubyObject self, String name,
                              CallType callType) {
        CacheEntry entry = searchWithCache(name);
        DynamicMethod method = entry.method;
        IRubyObject caller = context.getFrameSelf();

        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, this, method.getVisibility(), name, callType, Block.NULL_BLOCK);
        }
        return method.call(context, self, entry.sourceModule, name);
    }

    /**
     * This method is deprecated because it depends on having a Ruby frame pushed for checking method visibility,
     * and there's no way to enforce that. Most users of this method probably don't need to check visibility.
     *
     * See https://github.com/jruby/jruby/issues/4134
     *
     * @deprecated Use finvoke if you do not want visibility-checking or invokeFrom if you do.
     */
    @Deprecated(since = "9.4-", forRemoval = true)
    public IRubyObject invoke(ThreadContext context, IRubyObject self, String name,
                              IRubyObject[] args, CallType callType) {
        assert args != null;
        CacheEntry entry = searchWithCache(name);
        DynamicMethod method = entry.method;
        IRubyObject caller = context.getFrameSelf();

        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, this, method.getVisibility(), name, callType, args, Block.NULL_BLOCK);
        }
        return method.call(context, self, entry.sourceModule, name, args);
    }

    /**
     * This method is deprecated because it depends on having a Ruby frame pushed for checking method visibility,
     * and there's no way to enforce that. Most users of this method probably don't need to check visibility.
     *
     * See https://github.com/jruby/jruby/issues/4134
     *
     * @deprecated Use finvoke if you do not want visibility-checking or invokeFrom if you do.
     */
    @Deprecated(since = "9.4-", forRemoval = true)
    public IRubyObject invoke(ThreadContext context, IRubyObject self, String name,
                              IRubyObject arg, CallType callType) {
        CacheEntry entry = searchWithCache(name);
        DynamicMethod method = entry.method;
        IRubyObject caller = context.getFrameSelf();

        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, this, method.getVisibility(), name, callType, arg, Block.NULL_BLOCK);
        }
        return method.call(context, self, entry.sourceModule, name, arg);
    }

    /**
     * This method is deprecated because it depends on having a Ruby frame pushed for checking method visibility,
     * and there's no way to enforce that. Most users of this method probably don't need to check visibility.
     *
     * See https://github.com/jruby/jruby/issues/4134
     *
     * @deprecated Use finvoke if you do not want visibility-checking or invokeFrom if you do.
     */
    @Deprecated(since = "9.4-", forRemoval = true)
    public IRubyObject invoke(ThreadContext context, IRubyObject self, String name,
                              IRubyObject arg0, IRubyObject arg1, CallType callType) {
        CacheEntry entry = searchWithCache(name);
        DynamicMethod method = entry.method;
        IRubyObject caller = context.getFrameSelf();

        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, this, method.getVisibility(), name, callType, arg0, arg1, Block.NULL_BLOCK);
        }
        return method.call(context, self, entry.sourceModule, name, arg0, arg1);
    }

    /**
     * This method is deprecated because it depends on having a Ruby frame pushed for checking method visibility,
     * and there's no way to enforce that. Most users of this method probably don't need to check visibility.
     *
     * See https://github.com/jruby/jruby/issues/4134
     *
     * @deprecated Use finvoke if you do not want visibility-checking or invokeFrom if you do.
     */
    @Deprecated(since = "9.4-", forRemoval = true)
    public IRubyObject invoke(ThreadContext context, IRubyObject self, String name,
                              IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, CallType callType) {
        CacheEntry entry = searchWithCache(name);
        DynamicMethod method = entry.method;
        IRubyObject caller = context.getFrameSelf();

        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, this, method.getVisibility(), name, callType, arg0, arg1, arg2, Block.NULL_BLOCK);
        }
        return method.call(context, self, entry.sourceModule, name, arg0, arg1, arg2);
    }

    @Deprecated(since = "9.4-", forRemoval = true)
    public VariableAccessorField getObjectIdAccessorField() {
        return variableTableManager.getObjectIdAccessorField();
    }

    @Deprecated(since = "9.4-", forRemoval = true)
    public VariableAccessorField getFFIHandleAccessorField() {
        return variableTableManager.getFFIHandleAccessorField();
    }

    @Deprecated(since = "9.4-", forRemoval = true)
    public VariableAccessorField getObjectGroupAccessorField() {
        return variableTableManager.getObjectGroupAccessorField();
    }

    @Deprecated
    public IRubyObject invokeFrom(ThreadContext context, CallType callType, IRubyObject caller, IRubyObject self, String name,
                                  Block block) {
        CacheEntry entry = searchWithCache(name);
        DynamicMethod method = entry.method;

        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, this, method.getVisibility(), name, callType, block);
        }
        return method.call(context, self, entry.sourceModule, name, block);
    }

    @Deprecated
    public IRubyObject invokeFrom(ThreadContext context, CallType callType, IRubyObject caller, IRubyObject self, String name,
                                  IRubyObject[] args, Block block) {
        assert args != null;
        CacheEntry entry = searchWithCache(name);
        DynamicMethod method = entry.method;

        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, this, method.getVisibility(), name, callType, args, block);
        }
        return method.call(context, self, entry.sourceModule, name, args, block);
    }

    @Deprecated
    public IRubyObject invokeFrom(ThreadContext context, CallType callType, IRubyObject caller, IRubyObject self, String name,
                                  IRubyObject arg, Block block) {
        CacheEntry entry = searchWithCache(name);
        DynamicMethod method = entry.method;

        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, this, method.getVisibility(), name, callType, arg, block);
        }
        return method.call(context, self, entry.sourceModule, name, arg, block);
    }

    @Deprecated
    public IRubyObject invokeFrom(ThreadContext context, CallType callType, IRubyObject caller, IRubyObject self, String name,
                                  IRubyObject arg0, IRubyObject arg1, Block block) { // NOT USED?
        CacheEntry entry = searchWithCache(name);
        DynamicMethod method = entry.method;

        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, this, method.getVisibility(), name, callType, arg0, arg1, block);
        }
        return method.call(context, self, entry.sourceModule, name, arg0, arg1, block);
    }

    @Deprecated
    public IRubyObject invokeFrom(ThreadContext context, CallType callType, IRubyObject caller, IRubyObject self, String name,
                                  IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        CacheEntry entry = searchWithCache(name);
        DynamicMethod method = entry.method;

        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, this, method.getVisibility(), name, callType, arg0, arg1, arg2, block);
        }
        return method.call(context, self, entry.sourceModule, name, arg0, arg1, arg2, block);
    }

    @Deprecated
    public IRubyObject invokeFrom(ThreadContext context, CallType callType, IRubyObject caller, IRubyObject self, String name) {
        CacheEntry entry = searchWithCache(name);
        DynamicMethod method = entry.method;

        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, this, method.getVisibility(), name, callType, Block.NULL_BLOCK);
        }
        return method.call(context, self, entry.sourceModule, name);
    }

    @Deprecated
    public IRubyObject invokeFrom(ThreadContext context, CallType callType, IRubyObject caller, IRubyObject self, String name,
                                  IRubyObject[] args) {
        assert args != null;
        CacheEntry entry = searchWithCache(name);
        DynamicMethod method = entry.method;

        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, this, method.getVisibility(), name, callType, args, Block.NULL_BLOCK);
        }
        return method.call(context, self, entry.sourceModule, name, args);
    }

    @Deprecated
    public IRubyObject invokeFrom(ThreadContext context, CallType callType, IRubyObject caller, IRubyObject self, String name,
                                  IRubyObject arg) {
        CacheEntry entry = searchWithCache(name);
        DynamicMethod method = entry.method;

        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, this, method.getVisibility(), name, callType, arg, Block.NULL_BLOCK);
        }
        return method.call(context, self, entry.sourceModule, name, arg);
    }

    @Deprecated
    public IRubyObject invokeFrom(ThreadContext context, CallType callType, IRubyObject caller, IRubyObject self, String name,
                                  IRubyObject arg0, IRubyObject arg1) {
        CacheEntry entry = searchWithCache(name);
        DynamicMethod method = entry.method;

        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, this, method.getVisibility(), name, callType, arg0, arg1, Block.NULL_BLOCK);
        }
        return method.call(context, self, entry.sourceModule, name, arg0, arg1);
    }

    @Deprecated
    public IRubyObject invokeFrom(ThreadContext context, CallType callType, IRubyObject caller, IRubyObject self, String name,
                                  IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        CacheEntry entry = searchWithCache(name);
        DynamicMethod method = entry.method;

        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, this, method.getVisibility(), name, callType, arg0, arg1, arg2, Block.NULL_BLOCK);
        }
        return method.call(context, self, entry.sourceModule, name, arg0, arg1, arg2);
    }

    // OBJECT STATE

    protected final Ruby runtime;
    private ObjectAllocator allocator; // the default allocator
    protected ObjectMarshal marshal;
    private volatile SubclassNode subclassNode;
    private static final AtomicReferenceFieldUpdater SUBCLASS_UPDATER = AtomicReferenceFieldUpdater.newUpdater(RubyClass.class, SubclassNode.class, "subclassNode");
    private int concreteSubclassesEstimate = 4;
    private int allDescendantsEstimate = 4;
    private int allSubclassesEstimate = 4;
    public static final int CS_IDX_INITIALIZE = 0;
    public enum CS_NAMES {
        INITIALIZE("initialize");

        CS_NAMES(String id) {
            this.id = id;
        }

        private static final CS_NAMES[] VALUES = values();
        public static final int length = VALUES.length;

        public static CS_NAMES fromOrdinal(int ordinal) {
            if (ordinal < 0 || ordinal >= VALUES.length) {
                throw new RuntimeException("invalid rest: " + ordinal);
            }
            return VALUES[ordinal];
        }

        public final String id;
    }

    private final CallSite[] baseCallSites = new CallSite[CS_NAMES.length];
    {
        for(int i = 0; i < baseCallSites.length; i++) {
            baseCallSites[i] = MethodIndex.getFunctionalCallSite(CS_NAMES.fromOrdinal(i).id);
        }
    }

    private CallSite[] extraCallSites;

    private Class<? extends Reified> reifiedClass;
    private Boolean reifiedClassJava;
    
    private JavaClassConfiguration javaClassConfiguration;

    /** A cached tuple of method, type, and generation for dumping */
    private MarshalTuple cachedDumpMarshal = MarshalTuple.NULL_TUPLE;

    /** A cached tuple of method and generation for marshal loading */
    private CacheEntry cachedLoad = CacheEntry.NULL_CACHE;

    /** The "real" class, used by includes and singletons to locate the actual type of the object */
    private final RubyClass realClass;

    /** Variable table manager for this class */
    private final VariableTableManager variableTableManager;
}
