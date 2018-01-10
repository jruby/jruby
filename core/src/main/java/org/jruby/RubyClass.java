/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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

import org.jruby.javasupport.JavaClass;
import org.jruby.runtime.Arity;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.callsite.RespondToCallSite;
import org.jruby.runtime.ivars.VariableAccessor;
import static org.jruby.util.CodegenUtils.ci;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ACC_VARARGS;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.java.codegen.RealClassGenerator;
import org.jruby.java.codegen.Reified;
import org.jruby.javasupport.Java;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ObjectMarshal;
import org.jruby.runtime.ThreadContext;
import static org.jruby.runtime.Visibility.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.runtime.ivars.VariableAccessorField;
import org.jruby.runtime.ivars.VariableTableManager;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.runtime.opto.Invalidator;
import org.jruby.util.ArraySupport;
import org.jruby.util.OneShotClassLoader;
import org.jruby.util.ClassDefiningClassLoader;
import org.jruby.util.CodegenUtils;
import org.jruby.util.JavaNameMangler;
import org.jruby.util.collections.WeakHashSet;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;

/**
 *
 * @author  jpetersen
 */
@JRubyClass(name="Class", parent="Module")
public class RubyClass extends RubyModule {

    private static final Logger LOG = LoggerFactory.getLogger(RubyClass.class);

    public static void createClassClass(Ruby runtime, RubyClass classClass) {
        classClass.setClassIndex(ClassIndex.CLASS);
        classClass.setReifiedClass(RubyClass.class);
        classClass.kindOf = new RubyModule.JavaClassKindOf(RubyClass.class);

        classClass.undefineMethod("module_function");
        classClass.undefineMethod("append_features");
        classClass.undefineMethod("prepend_features");
        classClass.undefineMethod("extend_object");

        classClass.defineAnnotatedMethods(RubyClass.class);

        runtime.setBaseNewMethod(classClass.searchMethod("new"));
    }

    public static final ObjectAllocator CLASS_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            RubyClass clazz = new RubyClass(runtime);
            clazz.allocator = ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR; // Class.allocate object is not allocatable before it is initialized
            return clazz;
        }
    };

    public ObjectAllocator getAllocator() {
        return allocator;
    }

    public void setAllocator(ObjectAllocator allocator) {
        this.allocator = allocator;
    }

    /**
     * Set a reflective allocator that calls a no-arg constructor on the given
     * class.
     *
     * @param cls The class on which to call the default constructor to allocate
     */
    public void setClassAllocator(final Class cls) {
        this.allocator = new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                try {
                    RubyBasicObject object = (RubyBasicObject)cls.newInstance();
                    object.setMetaClass(klazz);
                    return object;
                } catch (InstantiationException ie) {
                    throw runtime.newTypeError("could not allocate " + cls + " with default constructor:\n" + ie);
                } catch (IllegalAccessException iae) {
                    throw runtime.newSecurityError("could not allocate " + cls + " due to inaccessible default constructor:\n" + iae);
                }
            }
        };

        this.reifiedClass = cls;
    }

    /**
     * Set a reflective allocator that calls the "standard" Ruby object
     * constructor (Ruby, RubyClass) on the given class.
     *
     * @param clazz The class from which to grab a standard Ruby constructor
     */
    public void setRubyClassAllocator(final Class<? extends IRubyObject> clazz) {
        try {
            final Constructor<? extends IRubyObject> constructor = clazz.getConstructor(Ruby.class, RubyClass.class);

            this.allocator = new ObjectAllocator() {
                public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                    try {
                        return constructor.newInstance(runtime, klazz);
                    } catch (InvocationTargetException ite) {
                        throw runtime.newTypeError("could not allocate " + clazz + " with (Ruby, RubyClass) constructor:\n" + ite);
                    } catch (InstantiationException ie) {
                        throw runtime.newTypeError("could not allocate " + clazz + " with (Ruby, RubyClass) constructor:\n" + ie);
                    } catch (IllegalAccessException iae) {
                        throw runtime.newSecurityError("could not allocate " + clazz + " due to inaccessible (Ruby, RubyClass) constructor:\n" + iae);
                    }
                }
            };

            this.reifiedClass = clazz;
        } catch (NoSuchMethodException nsme) {
            throw new RuntimeException(nsme);
        }
    }

    /**
     * Set a reflective allocator that calls the "standard" Ruby object
     * constructor (Ruby, RubyClass) on the given class via a static
     * __allocate__ method intermediate.
     *
     * @param clazz The class from which to grab a standard Ruby __allocate__ method.
     *
     * @note Used with `jrubyc --java` generated (interoperability) class files.
     */
    public void setRubyStaticAllocator(final Class<?> clazz) {
        try {
            final Method method = clazz.getDeclaredMethod("__allocate__", Ruby.class, RubyClass.class);

            this.allocator = new ObjectAllocator() {
                public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                    try {
                        return (IRubyObject) method.invoke(null, runtime, klazz);
                    } catch (InvocationTargetException ite) {
                        throw runtime.newTypeError("could not allocate " + clazz + " with (Ruby, RubyClass) constructor:\n" + ite);
                    } catch (IllegalAccessException iae) {
                        throw runtime.newSecurityError("could not allocate " + clazz + " due to inaccessible (Ruby, RubyClass) constructor:\n" + iae);
                    }
                }
            };

            this.reifiedClass = clazz;
        } catch (NoSuchMethodException nsme) {
            throw new RuntimeException(nsme);
        }
    }

    @JRubyMethod(name = "allocate")
    public IRubyObject allocate() {
        if (superClass == null) {
            if(this != runtime.getBasicObject()) {
                throw runtime.newTypeError("can't instantiate uninitialized class");
            }
        }
        IRubyObject obj = allocator.allocate(runtime, this);
        if (obj.getMetaClass().getRealClass() != getRealClass()) {
            throw runtime.newTypeError("wrong instance allocation");
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

    public VariableAccessorField getObjectIdAccessorField() {
        return variableTableManager.getObjectIdAccessorField();
    }

    public VariableAccessorField getFFIHandleAccessorField() {
        return variableTableManager.getFFIHandleAccessorField();
    }

    public VariableAccessor getFFIHandleAccessorForRead() {
        return variableTableManager.getFFIHandleAccessorForRead();
    }

    public VariableAccessor getFFIHandleAccessorForWrite() {
        return variableTableManager.getFFIHandleAccessorForWrite();
    }

    public VariableAccessorField getObjectGroupAccessorField() {
        return variableTableManager.getObjectGroupAccessorField();
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
        RubyClass obj;

        if (superClass == null ) {  // boot the Object class
            obj = new RubyClass(runtime);
            obj.marshal = DEFAULT_OBJECT_MARSHAL;
        } else {                    // boot the Module and Class classes
            obj = new RubyClass(runtime, superClass);
        }
        obj.setAllocator(allocator);
        obj.setBaseName(name);
        return obj;
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
            this.realClass = superClass.realClass;
            if (realClass != null) {
                this.variableTableManager = realClass.variableTableManager;
            } else {
                this.variableTableManager = new VariableTableManager(this);
            }
        }

        setSuperClass(superClass); // this is the only case it might be null here (in MetaClass construction)
    }

    /** used by CLASS_ALLOCATOR (any Class' class will be a Class!)
     *  also used to bootstrap Object class
     */
    protected RubyClass(Ruby runtime) {
        super(runtime, runtime.getClassClass());
        this.runtime = runtime;
        this.realClass = this;
        this.variableTableManager = new VariableTableManager(this);
        setClassIndex(ClassIndex.CLASS);
    }

    /** rb_class_boot (for plain Classes)
     *  also used to bootstrap Module and Class classes
     */
    protected RubyClass(Ruby runtime, RubyClass superClazz) {
        this(runtime);
        setSuperClass(superClazz);
        marshal = superClazz.marshal; // use parent's marshal
        superClazz.addSubclass(this);
        allocator = superClazz.allocator;

        infectBy(superClass);
    }

    /**
     * A constructor which allows passing in an array of supplementary call sites.
     */
    protected RubyClass(Ruby runtime, RubyClass superClazz, CallSite[] extraCallSites) {
        this(runtime);
        setSuperClass(superClazz);
        this.marshal = superClazz.marshal; // use parent's marshal
        superClazz.addSubclass(this);

        this.extraCallSites = extraCallSites;

        infectBy(superClass);
    }

    /**
     * Construct a new class with the given name scoped under Object (global)
     * and with Object as its immediate superclass.
     * Corresponds to rb_class_new in MRI.
     */
    public static RubyClass newClass(Ruby runtime, RubyClass superClass) {
        if (superClass == runtime.getClassClass()) throw runtime.newTypeError("can't make subclass of Class");
        if (superClass.isSingleton()) throw runtime.newTypeError("can't make subclass of virtual class");
        return new RubyClass(runtime, superClass);
    }

    /**
     * A variation on newClass that allow passing in an array of supplementary
     * call sites to improve dynamic invocation.
     */
    public static RubyClass newClass(Ruby runtime, RubyClass superClass, CallSite[] extraCallSites) {
        if (superClass == runtime.getClassClass()) throw runtime.newTypeError("can't make subclass of Class");
        if (superClass.isSingleton()) throw runtime.newTypeError("can't make subclass of virtual class");
        return new RubyClass(runtime, superClass, extraCallSites);
    }

    /**
     * Construct a new class with the given name, allocator, parent class,
     * and containing class. If setParent is true, the class's parent will be
     * explicitly set to the provided parent (rather than the new class just
     * being assigned to a constant in that parent).
     * Corresponds to rb_class_new/rb_define_class_id/rb_name_class/rb_set_class_path
     * in MRI.
     */
    public static RubyClass newClass(Ruby runtime, RubyClass superClass, String name, ObjectAllocator allocator, RubyModule parent, boolean setParent) {
        RubyClass clazz = newClass(runtime, superClass);
        clazz.setBaseName(name);
        clazz.setAllocator(allocator);
        clazz.makeMetaClass(superClass.getMetaClass());
        if (setParent) clazz.setParent(parent);
        parent.setConstant(name, clazz);
        clazz.inherit(superClass);
        return clazz;
    }

    /**
     * A variation on newClass that allows passing in an array of supplementary
     * call sites to improve dynamic invocation performance.
     */
    public static RubyClass newClass(Ruby runtime, RubyClass superClass, String name, ObjectAllocator allocator, RubyModule parent, boolean setParent, CallSite[] extraCallSites) {
        RubyClass clazz = newClass(runtime, superClass, extraCallSites);
        clazz.setBaseName(name);
        clazz.setAllocator(allocator);
        clazz.makeMetaClass(superClass.getMetaClass());
        if (setParent) clazz.setParent(parent);
        parent.setConstant(name, clazz);
        clazz.inherit(superClass);
        return clazz;
    }

    /** rb_make_metaclass
     *
     */
    @Override
    public RubyClass makeMetaClass(RubyClass superClass) {
        if (isSingleton()) { // could be pulled down to RubyClass in future
            MetaClass klass = new MetaClass(runtime, superClass, this); // rb_class_boot
            setMetaClass(klass);

            klass.setMetaClass(klass);
            klass.setSuperClass(getSuperClass().getRealClass().getMetaClass());

            return klass;
        } else {
            return super.makeMetaClass(superClass);
        }
    }

    public boolean notVisibleAndNotMethodMissing(DynamicMethod method, String name, IRubyObject caller, CallType callType) {
        return !method.isCallableFrom(caller, callType) && !name.equals("method_missing");
    }

    public IRubyObject finvoke(ThreadContext context, IRubyObject self, String name, Block block) {
        DynamicMethod method = searchMethod(name);
        if (shouldCallMethodMissing(method)) {
            return Helpers.callMethodMissing(context, self, method.getVisibility(), name, CallType.FUNCTIONAL, block);
        }
        return method.call(context, self, this, name, block);
    }

    public IRubyObject finvoke(ThreadContext context, IRubyObject self, String name,
            IRubyObject[] args, Block block) {
        assert args != null;
        DynamicMethod method = searchMethod(name);
        if (shouldCallMethodMissing(method)) {
            return Helpers.callMethodMissing(context, self, method.getVisibility(), name, CallType.FUNCTIONAL, args, block);
        }
        return method.call(context, self, this, name, args, block);
    }

    public IRubyObject finvoke(ThreadContext context, IRubyObject self, String name,
            IRubyObject arg, Block block) {
        DynamicMethod method = searchMethod(name);
        if (shouldCallMethodMissing(method)) {
            return Helpers.callMethodMissing(context, self, method.getVisibility(), name, CallType.FUNCTIONAL, arg, block);
        }
        return method.call(context, self, this, name, arg, block);
    }

    public IRubyObject finvoke(ThreadContext context, IRubyObject self, String name,
            IRubyObject arg0, IRubyObject arg1, Block block) {
        DynamicMethod method = searchMethod(name);
        if (shouldCallMethodMissing(method)) {
            return Helpers.callMethodMissing(context, self, method.getVisibility(), name, CallType.FUNCTIONAL, arg0, arg1, block);
        }
        return method.call(context, self, this, name, arg0, arg1, block);
    }

    public IRubyObject finvoke(ThreadContext context, IRubyObject self, String name,
                               IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        DynamicMethod method = searchMethod(name);
        if (shouldCallMethodMissing(method)) {
            return Helpers.callMethodMissing(context, self, method.getVisibility(), name, CallType.FUNCTIONAL, arg0, arg1, arg2, block);
        }
        return method.call(context, self, this, name, arg0, arg1, arg2, block);
    }

    public IRubyObject finvoke(ThreadContext context, IRubyObject self, String name) {
        DynamicMethod method = searchMethod(name);
        if (shouldCallMethodMissing(method)) {
            return Helpers.callMethodMissing(context, self, method.getVisibility(), name, CallType.FUNCTIONAL, Block.NULL_BLOCK);
        }
        return method.call(context, self, this, name);
    }

    /**
     * Same behavior as finvoke, but uses the given caller object to check visibility if callType demands it.
     */
    public IRubyObject invokeFrom(ThreadContext context, CallType callType, IRubyObject caller, IRubyObject self, String name,
                              Block block) {
        DynamicMethod method = searchMethod(name);
        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, method.getVisibility(), name, callType, block);
        }
        return method.call(context, self, this, name, block);
    }

    /**
     * Same behavior as finvoke, but uses the given caller object to check visibility if callType demands it.
     */
    public IRubyObject invokeFrom(ThreadContext context, CallType callType, IRubyObject caller, IRubyObject self, String name,
                              IRubyObject[] args, Block block) {
        assert args != null;
        DynamicMethod method = searchMethod(name);
        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, method.getVisibility(), name, callType, args, block);
        }
        return method.call(context, self, this, name, args, block);
    }

    /**
     * Same behavior as finvoke, but uses the given caller object to check visibility if callType demands it.
     */
    public IRubyObject invokeFrom(ThreadContext context, CallType callType, IRubyObject caller, IRubyObject self, String name,
                              IRubyObject arg, Block block) {
        DynamicMethod method = searchMethod(name);
        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, method.getVisibility(), name, callType, arg, block);
        }
        return method.call(context, self, this, name, arg, block);
    }

    /**
     * Same behavior as finvoke, but uses the given caller object to check visibility if callType demands it.
     */
    public IRubyObject invokeFrom(ThreadContext context, CallType callType, IRubyObject caller, IRubyObject self, String name,
                              IRubyObject arg0, IRubyObject arg1, Block block) {
        DynamicMethod method = searchMethod(name);
        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, method.getVisibility(), name, callType, arg0, arg1, block);
        }
        return method.call(context, self, this, name, arg0, arg1, block);
    }

    /**
     * Same behavior as finvoke, but uses the given caller object to check visibility if callType demands it.
     */
    public IRubyObject invokeFrom(ThreadContext context, CallType callType, IRubyObject caller, IRubyObject self, String name,
                              IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        DynamicMethod method = searchMethod(name);
        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, method.getVisibility(), name, callType, arg0, arg1, arg2, block);
        }
        return method.call(context, self, this, name, arg0, arg1, arg2, block);
    }

    /**
     * Same behavior as finvoke, but uses the given caller object to check visibility if callType demands it.
     */
    public IRubyObject invokeFrom(ThreadContext context, CallType callType, IRubyObject caller, IRubyObject self, String name) {
        DynamicMethod method = searchMethod(name);
        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, method.getVisibility(), name, callType, Block.NULL_BLOCK);
        }
        return method.call(context, self, this, name);
    }

    /**
     * Same behavior as finvoke, but uses the given caller object to check visibility if callType demands it.
     */
    public IRubyObject invokeFrom(ThreadContext context, CallType callType, IRubyObject caller, IRubyObject self, String name,
                              IRubyObject[] args) {
        assert args != null;
        DynamicMethod method = searchMethod(name);
        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, method.getVisibility(), name, callType, args, Block.NULL_BLOCK);
        }
        return method.call(context, self, this, name, args);
    }

    /**
     * Same behavior as finvoke, but uses the given caller object to check visibility if callType demands it.
     */
    public IRubyObject invokeFrom(ThreadContext context, CallType callType, IRubyObject caller, IRubyObject self, String name,
                              IRubyObject arg) {
        DynamicMethod method = searchMethod(name);
        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, method.getVisibility(), name, callType, arg, Block.NULL_BLOCK);
        }
        return method.call(context, self, this, name, arg);
    }

    /**
     * Same behavior as finvoke, but uses the given caller object to check visibility if callType demands it.
     */
    public IRubyObject invokeFrom(ThreadContext context, CallType callType, IRubyObject caller, IRubyObject self, String name,
                              IRubyObject arg0, IRubyObject arg1) {
        DynamicMethod method = searchMethod(name);
        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, method.getVisibility(), name, callType, arg0, arg1, Block.NULL_BLOCK);
        }
        return method.call(context, self, this, name, arg0, arg1);
    }

    /**
     * Same behavior as finvoke, but uses the given caller object to check visibility if callType demands it.
     */
    public IRubyObject invokeFrom(ThreadContext context, CallType callType, IRubyObject caller, IRubyObject self, String name,
                              IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        DynamicMethod method = searchMethod(name);
        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, method.getVisibility(), name, callType, arg0, arg1, arg2, Block.NULL_BLOCK);
        }
        return method.call(context, self, this, name, arg0, arg1, arg2);
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
        final RubyClass klass = self.getMetaClass();
        if (!checkFuncallRespondTo(context, klass, self, name)) return null; // return def;

        DynamicMethod me = searchMethod(name);
        if (!checkFuncallCallable(context, me, CallType.FUNCTIONAL, self)) {
            return checkFuncallMissing(context, klass, self, name, args);
        }
        return me.call(context, self, klass, name, args);
    }

    // MRI: rb_check_funcall_default
    private IRubyObject checkFuncallDefault(ThreadContext context, IRubyObject self, JavaSites.CheckedSites sites, IRubyObject[] args) {
        final RubyClass klass = self.getMetaClass();
        if (!checkFuncallRespondTo(context, klass, self, sites.respond_to_X)) return null; // return def;

        DynamicMethod me = sites.site.retrieveCache(klass).method;
        if (!checkFuncallCallable(context, me, CallType.FUNCTIONAL, self)) {
            return checkFuncallMissing(context, klass, self, sites.methodName, sites.respond_to_missing, sites.method_missing, args);
        }
        return me.call(context, self, klass, sites.methodName, args);
    }

    // MRI: rb_check_funcall_default
    private IRubyObject checkFuncallDefault(ThreadContext context, IRubyObject self, JavaSites.CheckedSites sites) {
        final RubyClass klass = self.getMetaClass();
        if (!checkFuncallRespondTo(context, klass, self, sites.respond_to_X)) return null; // return def;

        DynamicMethod me = sites.site.retrieveCache(klass).method;
        if (!checkFuncallCallable(context, me, CallType.FUNCTIONAL, self)) {
            return checkFuncallMissing(context, klass, self, sites.methodName, sites.respond_to_missing, sites.method_missing);
        }
        return me.call(context, self, klass, sites.methodName);
    }

    // MRI: check_funcall_exec
    private static IRubyObject checkFuncallExec(ThreadContext context, IRubyObject self, String name, IRubyObject... args) {
        return self.callMethod(context, "method_missing", ArraySupport.newCopy(context.runtime.newSymbol(name), args));
    }

    // MRI: check_funcall_exec
    private static IRubyObject checkFuncallExec(ThreadContext context, IRubyObject self, String name, CallSite methodMissingSite, IRubyObject... args) {
        return methodMissingSite.call(context, self, self, ArraySupport.newCopy(context.runtime.newSymbol(name), args));
    }

    // MRI: check_funcall_failed
    private static IRubyObject checkFuncallFailed(ThreadContext context, IRubyObject self, String name, RubyClass expClass, IRubyObject... args) {
        if (self.respondsTo(name)) {
            throw context.runtime.newRaiseException(expClass, name);
        }
        return null;
    }

    /**
     * Check if the method has a custom respond_to? and call it if so with the method ID we're hoping to call.
     *
     * MRI: check_funcall_respond_to
     */
    private static boolean checkFuncallRespondTo(ThreadContext context, RubyClass klass, IRubyObject recv, String mid) {
        final Ruby runtime = context.runtime;
        DynamicMethod me = klass.searchMethod("respond_to?");

        // NOTE: isBuiltin here would be NOEX_BASIC in MRI, a flag only added to respond_to?, method_missing, and
        //       respond_to_missing? Same effect, I believe.
        if (me != null && !me.isUndefined() && !me.isBuiltin()) {
            int arityValue = me.getArity().getValue();

            if (arityValue > 2) throw runtime.newArgumentError("respond_to? must accept 1 or 2 arguments (requires " + arityValue + ")");

            IRubyObject result;
            if (arityValue == 1) {
                result = me.call(context, recv, klass, "respond_to?", runtime.newSymbol(mid));
            } else {
                result = me.call(context, recv, klass, "respond_to?", runtime.newSymbol(mid), runtime.getTrue());
            }
            return result.isTrue();
        }
        return true;
    }

    /**
     * Check if the method has a custom respond_to? and call it if so with the method ID we're hoping to call.
     *
     * MRI: check_funcall_respond_to
     */
    private static boolean checkFuncallRespondTo(ThreadContext context, RubyClass klass, IRubyObject recv, RespondToCallSite respondToSite) {
        final Ruby runtime = context.runtime;
        DynamicMethod me = respondToSite.retrieveCache(klass).method;

        // NOTE: isBuiltin here would be NOEX_BASIC in MRI, a flag only added to respond_to?, method_missing, and
        //       respond_to_missing? Same effect, I believe.
        if (me != null && !me.isUndefined() && !me.isBuiltin()) {
            int arityValue = me.getArity().getValue();

            if (arityValue > 2) throw runtime.newArgumentError("respond_to? must accept 1 or 2 arguments (requires " + arityValue + ")");

            boolean result;
            if (arityValue == 1) {
                result = respondToSite.respondsTo(context, recv, recv);
            } else {
                result = respondToSite.respondsTo(context, recv, recv, true);
            }
            return result;
        }
        return true;
    }

    // MRI: check_funcall_callable
    public static boolean checkFuncallCallable(ThreadContext context, DynamicMethod method, CallType callType, IRubyObject self) {
        return rbMethodCallStatus(context, method, callType, self);
    }

    // MRI: rb_method_call_status
    // FIXME: Partial impl because we don't have these "NOEX" flags
    public static boolean rbMethodCallStatus(ThreadContext context, DynamicMethod method, CallType callType, IRubyObject self) {
        return method != null && !method.isUndefined() && method.isCallableFrom(self, callType);
    }

    // MRI: check_funcall_missing
    private static IRubyObject checkFuncallMissing(ThreadContext context, RubyClass klass, IRubyObject self, String method, IRubyObject... args) {
        final Ruby runtime = context.runtime;

        DynamicMethod me = klass.searchMethod("respond_to_missing?");
        // MRI: basic_obj_respond_to_missing ...
        if ( me != null && ! me.isUndefined() && ! me.isBuiltin() ) {
            IRubyObject ret;
            if (me.getArity().getValue() == 1) {
                ret = me.call(context, self, klass, "respond_to_missing?", runtime.newSymbol(method));
            } else {
                ret = me.call(context, self, klass, "respond_to_missing?", runtime.newSymbol(method), runtime.getTrue());
            }
            if ( ! ret.isTrue() ) return null;
        }

        if ( klass.isMethodBuiltin("method_missing") ) return null;

        final IRubyObject $ex = context.getErrorInfo();
        try {
            return checkFuncallExec(context, self, method, args);
        }
        catch (RaiseException e) {
            context.setErrorInfo($ex); // restore $!
            return checkFuncallFailed(context, self, method, runtime.getNoMethodError(), args);
        }
    }

    // MRI: check_funcall_missing
    private static IRubyObject checkFuncallMissing(ThreadContext context, RubyClass klass, IRubyObject self, String method, CachingCallSite respondToMissingSite, CachingCallSite methodMissingSite, IRubyObject... args) {
        final Ruby runtime = context.runtime;

        DynamicMethod me = respondToMissingSite.retrieveCache(klass).method;
        // MRI: basic_obj_respond_to_missing ...
        if ( me != null && ! me.isUndefined() && ! me.isBuiltin() ) {
            IRubyObject ret;
            if (me.getArity().getValue() == 1) {
                ret = me.call(context, self, klass, "respond_to_missing?", runtime.newSymbol(method));
            } else {
                ret = me.call(context, self, klass, "respond_to_missing?", runtime.newSymbol(method), runtime.getTrue());
            }
            if ( ! ret.isTrue() ) return null;
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
        DynamicMethod method = searchMethod(name);
        if (shouldCallMethodMissing(method)) {
            return Helpers.callMethodMissing(context, self, method.getVisibility(), name, CallType.FUNCTIONAL, args, Block.NULL_BLOCK);
        }
        return method.call(context, self, this, name, args);
    }

    public IRubyObject finvoke(ThreadContext context, IRubyObject self, String name,
            IRubyObject arg) {
        DynamicMethod method = searchMethod(name);
        if (shouldCallMethodMissing(method)) {
            return Helpers.callMethodMissing(context, self, method.getVisibility(), name, CallType.FUNCTIONAL, arg, Block.NULL_BLOCK);
        }
        return method.call(context, self, this, name, arg);
    }

    public IRubyObject finvoke(ThreadContext context, IRubyObject self, String name,
            IRubyObject arg0, IRubyObject arg1) {
        DynamicMethod method = searchMethod(name);
        if (shouldCallMethodMissing(method)) {
            return Helpers.callMethodMissing(context, self, method.getVisibility(), name, CallType.FUNCTIONAL, arg0, arg1, Block.NULL_BLOCK);
        }
        return method.call(context, self, this, name, arg0, arg1);
    }

    public IRubyObject finvoke(ThreadContext context, IRubyObject self, String name,
            IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        DynamicMethod method = searchMethod(name);
        if (shouldCallMethodMissing(method)) {
            return Helpers.callMethodMissing(context, self, method.getVisibility(), name, CallType.FUNCTIONAL, arg0, arg1, arg2, Block.NULL_BLOCK);
        }
        return method.call(context, self, this, name, arg0, arg1, arg2);
    }

    private void dumpReifiedClass(String dumpDir, String javaPath, byte[] classBytes) {
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
                getRuntime().getWarnings().warn("unable to dump class file: " + io.getMessage());
            }
            finally {
                if (classStream != null) {
                    try { classStream.close(); }
                    catch (IOException ignored) { /* no-op */ }
                }
            }
        }
    }

    private void generateMethodAnnotations(Map<Class, Map<String, Object>> methodAnnos, SkinnyMethodAdapter m, List<Map<Class, Map<String, Object>>> parameterAnnos) {
        if (methodAnnos != null && methodAnnos.size() != 0) {
            for (Map.Entry<Class, Map<String, Object>> entry : methodAnnos.entrySet()) {
                m.visitAnnotationWithFields(ci(entry.getKey()), true, entry.getValue());
            }
        }
        if (parameterAnnos != null && parameterAnnos.size() != 0) {
            for (int i = 0; i < parameterAnnos.size(); i++) {
                Map<Class, Map<String, Object>> annos = parameterAnnos.get(i);
                if (annos != null && annos.size() != 0) {
                    for (Iterator<Map.Entry<Class, Map<String, Object>>> it = annos.entrySet().iterator(); it.hasNext();) {
                        Map.Entry<Class, Map<String, Object>> entry = it.next();
                        m.visitParameterAnnotationWithFields(i, ci(entry.getKey()), true, entry.getValue());
                    }
                }
            }
        }
    }

    private boolean shouldCallMethodMissing(DynamicMethod method) {
        return method.isUndefined();
    }
    private boolean shouldCallMethodMissing(DynamicMethod method, String name, IRubyObject caller, CallType callType) {
        return method.isUndefined() || notVisibleAndNotMethodMissing(method, name, caller, callType);
    }

    public IRubyObject invokeInherited(ThreadContext context, IRubyObject self, IRubyObject subclass) {
        DynamicMethod method = getMetaClass().searchMethod("inherited");

        if (method.isUndefined()) {
            return Helpers.callMethodMissing(context, self, method.getVisibility(), "inherited", CallType.FUNCTIONAL, Block.NULL_BLOCK);
        }

        return method.call(context, self, getMetaClass(), "inherited", subclass, Block.NULL_BLOCK);
    }

    /** rb_class_new_instance
    *
    */
    @JRubyMethod(name = "new", omit = true)
    public IRubyObject newInstance(ThreadContext context, Block block) {
        IRubyObject obj = allocate();
        baseCallSites[CS_IDX_INITIALIZE].call(context, obj, obj, block);
        return obj;
    }

    @JRubyMethod(name = "new", omit = true)
    public IRubyObject newInstance(ThreadContext context, IRubyObject arg0, Block block) {
        IRubyObject obj = allocate();
        baseCallSites[CS_IDX_INITIALIZE].call(context, obj, obj, arg0, block);
        return obj;
    }

    @JRubyMethod(name = "new", omit = true)
    public IRubyObject newInstance(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        IRubyObject obj = allocate();
        baseCallSites[CS_IDX_INITIALIZE].call(context, obj, obj, arg0, arg1, block);
        return obj;
    }

    @JRubyMethod(name = "new", omit = true)
    public IRubyObject newInstance(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        IRubyObject obj = allocate();
        baseCallSites[CS_IDX_INITIALIZE].call(context, obj, obj, arg0, arg1, arg2, block);
        return obj;
    }

    @JRubyMethod(name = "new", rest = true, omit = true)
    public IRubyObject newInstance(ThreadContext context, IRubyObject[] args, Block block) {
        IRubyObject obj = allocate();
        baseCallSites[CS_IDX_INITIALIZE].call(context, obj, obj, args, block);
        return obj;
    }

    /** rb_class_initialize
     *
     */
    @Override
    public IRubyObject initialize(ThreadContext context, Block block) {
        return initialize19(context, block);
    }

    public IRubyObject initialize(ThreadContext context, IRubyObject superObject, Block block) {
        return initialize19(context, superObject, block);
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public IRubyObject initialize19(ThreadContext context, Block block) {
        checkNotInitialized();
        return initializeCommon(context, runtime.getObject(), block, true);
    }

    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public IRubyObject initialize19(ThreadContext context, IRubyObject superObject, Block block) {
        checkNotInitialized();
        checkInheritable(superObject);
        return initializeCommon(context, (RubyClass) superObject, block, true);
    }

    private IRubyObject initializeCommon(ThreadContext context, RubyClass superClazz, Block block, boolean ruby1_9 /*callInheritBeforeSuper*/) {
        setSuperClass(superClazz);
        allocator = superClazz.allocator;
        makeMetaClass(superClazz.getMetaClass());

        marshal = superClazz.marshal;

        superClazz.addSubclass(this);

        if (ruby1_9) {
            inherit(superClazz);
            super.initialize(context, block);
        } else {
            super.initialize(context, block);
            inherit(superClazz);
        }

        return this;
    }

    /** rb_class_init_copy
     *
     */
    @JRubyMethod(name = "initialize_copy", required = 1, visibility = PRIVATE)
    @Override
    public IRubyObject initialize_copy(IRubyObject original){
        checkNotInitialized();
        if (original instanceof MetaClass) throw runtime.newTypeError("can't copy singleton class");

        super.initialize_copy(original);
        allocator = ((RubyClass)original).allocator;
        return this;
    }

    protected void setModuleSuperClass(RubyClass superClass) {
        // remove us from old superclass's child classes
        if (this.superClass != null) this.superClass.removeSubclass(this);
        // add us to new superclass's child classes
        superClass.addSubclass(this);
        // update superclass reference
        setSuperClass(superClass);
    }

    // introduced solely to provide some level of compatibility with previous
    // Class#subclasses implementation ... `ruby_class.to_java.subclasses`
    public final Collection<RubyClass> subclasses() {
        return subclasses(false);
    }

    public synchronized Collection<RubyClass> subclasses(boolean includeDescendants) {
        Set<RubyClass> subclasses = this.subclasses;
        if (subclasses != null) {
            Collection<RubyClass> mine = new ArrayList<>(subclasses);
            if (includeDescendants) {
                for (RubyClass klass: subclasses) {
                    mine.addAll(klass.subclasses(includeDescendants));
                }
            }

            return mine;
        }
        return Collections.EMPTY_LIST;
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
    public synchronized void addSubclass(RubyClass subclass) {
        synchronized (runtime.getHierarchyLock()) {
            Set<RubyClass> subclasses = this.subclasses;
            if (subclasses == null) this.subclasses = subclasses = new WeakHashSet<>(4);
            subclasses.add(subclass);
        }
    }

    /**
     * Remove a subclass from the weak set of subclasses.
     *
     * @param subclass The subclass to remove
     */
    public synchronized void removeSubclass(RubyClass subclass) {
        synchronized (runtime.getHierarchyLock()) {
            Set<RubyClass> subclasses = this.subclasses;
            if (subclasses == null) return;

            subclasses.remove(subclass);
        }
    }

    /**
     * Replace an existing subclass with a new one.
     *
     * @param subclass The subclass to remove
     * @param newSubclass The subclass to replace it with
     */
    public synchronized void replaceSubclass(RubyClass subclass, RubyClass newSubclass) {
        synchronized (runtime.getHierarchyLock()) {
            Set<RubyClass> subclasses = this.subclasses;
            if (subclasses == null) return;

            subclasses.remove(subclass);
            subclasses.add(newSubclass);
        }
    }

    @Override
    public void becomeSynchronized() {
        // make this class and all subclasses sync
        synchronized (runtime.getHierarchyLock()) {
            super.becomeSynchronized();
            Set<RubyClass> subclasses = this.subclasses;
            if (subclasses != null) {
                for (RubyClass subclass : subclasses) subclass.becomeSynchronized();
            }
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

        synchronized (runtime.getHierarchyLock()) {
            Set<RubyClass> subclasses = this.subclasses;
            if (subclasses != null) {
                for (RubyClass subclass : subclasses) subclass.invalidateCacheDescendants();
            }
        }
    }

    public void addInvalidatorsAndFlush(List<Invalidator> invalidators) {
        // add this class's invalidators to the aggregate
        invalidators.add(methodInvalidator);

        // if we're not at boot time, don't bother fully clearing caches
        if (!runtime.isBootingCore()) cachedMethods.clear();

        Set<RubyClass> subclasses = this.subclasses;
        // no subclasses, don't bother with lock and iteration
        if (subclasses == null || subclasses.isEmpty()) return;

        // cascade into subclasses
        synchronized (runtime.getHierarchyLock()) {
            subclasses = this.subclasses;
            if (subclasses != null) {
                for (RubyClass subclass : subclasses) subclass.addInvalidatorsAndFlush(invalidators);
            }
        }
    }

    public final Ruby getClassRuntime() {
        return runtime;
    }

    public final RubyClass getRealClass() {
        return realClass;
    }

    @JRubyMethod(name = "inherited", required = 1, visibility = PRIVATE)
    public IRubyObject inherited(ThreadContext context, IRubyObject arg) {
        return context.nil;
    }

    /** rb_class_inherited (reversed semantics!)
     *
     */
    public void inherit(RubyClass superClazz) {
        if (superClazz == null) superClazz = runtime.getObject();

        if (getRuntime().getNil() != null) {
            superClazz.invokeInherited(runtime.getCurrentContext(), superClazz, this);
        }
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
            if (metaClass == runtime.getBasicObject().getMetaClass()) return context.nil;
            throw runtime.newTypeError("uninitialized class");
        }

        while (superClazz != null && (superClazz.isIncluded() || superClazz.isPrepended())) {
            superClazz = superClazz.superClass;
        }

        return superClazz != null ? superClazz : context.nil;
    }

    private void checkNotInitialized() {
        if (superClass != null || this == runtime.getBasicObject()) {
            throw runtime.newTypeError("already initialized class");
        }
    }
    /** rb_check_inheritable
     *
     */
    public static void checkInheritable(IRubyObject superClass) {
        if (!(superClass instanceof RubyClass)) {
            throw superClass.getRuntime().newTypeError("superclass must be a Class (" + superClass.getMetaClass() + " given)");
        }
        if (((RubyClass)superClass).isSingleton()) {
            throw superClass.getRuntime().newTypeError("can't make subclass of virtual class");
        }
        if (superClass == superClass.getRuntime().getClassClass()) {
            throw superClass.getRuntime().newTypeError("can't make subclass of Class");
        }
    }

    public final ObjectMarshal getMarshal() {
        return marshal;
    }

    public final void setMarshal(ObjectMarshal marshal) {
        this.marshal = marshal;
    }

    public final void marshal(Object obj, MarshalStream marshalStream) throws IOException {
        getMarshal().marshalTo(runtime, obj, this, marshalStream);
    }

    public final Object unmarshal(UnmarshalStream unmarshalStream) throws IOException {
        return getMarshal().unmarshalFrom(runtime, this, unmarshalStream);
    }

    public static void marshalTo(RubyClass clazz, MarshalStream output) throws java.io.IOException {
        output.registerLinkTarget(clazz);
        output.writeString(MarshalStream.getPathFromClass(clazz));
    }

    public static RubyClass unmarshalFrom(UnmarshalStream input) throws java.io.IOException {
        String name = RubyString.byteListToString(input.unmarshalString());
        RubyClass result = UnmarshalStream.getClassFromPath(input.getRuntime(), name);
        input.registerLinkTarget(result);
        return result;
    }

    protected static final ObjectMarshal DEFAULT_OBJECT_MARSHAL = new ObjectMarshal() {
        @Override
        public void marshalTo(Ruby runtime, Object obj, RubyClass type,
                              MarshalStream marshalStream) throws IOException {
            IRubyObject object = (IRubyObject)obj;

            marshalStream.registerLinkTarget(object);
            marshalStream.dumpVariables(object.getVariableList());
        }

        @Override
        public Object unmarshalFrom(Ruby runtime, RubyClass type,
                                    UnmarshalStream unmarshalStream) throws IOException {
            IRubyObject result = type.allocate();

            unmarshalStream.registerLinkTarget(result);

            unmarshalStream.defaultVariablesUnmarshal(result);

            return result;
        }
    };

    /**
     * Whether this class can be reified into a Java class. Currently only objects
     * that descend from Object (or descend from Ruby-based classes that descend
     * from Object) can be reified.
     *
     * @return true if the class can be reified, false otherwise
     */
    public boolean isReifiable() {
        // already reified is not reifiable
        if (reifiedClass != null) return false;

        final RubyClass realSuper;
        // root classes are not reifiable
        if (superClass == null || (realSuper = superClass.getRealClass()) == null) return false;

        Class reifiedSuper = realSuper.reifiedClass;

        // if super has been reified or is a native class
        if (reifiedSuper != null) {

            // super must be Object, BasicObject, or a reified user class
            return reifiedSuper == RubyObject.class ||
                    reifiedSuper == RubyBasicObject.class ||
                    Reified.class.isAssignableFrom(reifiedSuper);
        } else {
            // non-native, non-reified super; recurse
            return realSuper.isReifiable();
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
        if (isReifiable()) {
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
        // re-check reifiable in case another reify call has jumped in ahead of us
        if (!isReifiable()) return;

        // calculate an appropriate name, for anonymous using inspect like format e.g. "Class:0x628fad4a"
        final String name = getBaseName() != null ? getName() :
                ( "Class_0x" + Integer.toHexString(System.identityHashCode(this)) );

        final String javaName = "rubyobj." + name.replaceAll("::", ".");
        final String javaPath = "rubyobj/" + name.replaceAll("::", "/");

        final Class parentReified = superClass.getRealClass().getReifiedClass();
        if (parentReified == null) {
            throw getClassRuntime().newTypeError(getName() + "'s parent class is not yet reified");
        }

        Class reifiedParent = RubyObject.class;
        if (superClass.reifiedClass != null) reifiedParent = superClass.reifiedClass;

        final byte[] classBytes = new Reificator(reifiedParent).reify(javaName, javaPath);

        final ClassDefiningClassLoader parentCL;
        if (parentReified.getClassLoader() instanceof OneShotClassLoader) {
            parentCL = (OneShotClassLoader) parentReified.getClassLoader();
        } else {
            if (useChildLoader) {
                parentCL = new OneShotClassLoader(runtime.getJRubyClassLoader());
            } else {
                parentCL = runtime.getJRubyClassLoader();
            }
        }
        // Attempt to load the name we plan to use; skip reification if it exists already (see #1229).
        try {
            Class result = parentCL.defineClass(javaName, classBytes);
            dumpReifiedClass(classDumpDir, javaPath, classBytes);

            @SuppressWarnings("unchecked")
            java.lang.reflect.Method clinit = result.getDeclaredMethod("clinit", Ruby.class, RubyClass.class);
            clinit.invoke(null, runtime, this);

            setClassAllocator(result);
            reifiedClass = result;

            return; // success
        }
        catch (LinkageError error) { // fall through to failure path
            final String msg = error.getMessage();
            if ( msg != null && msg.contains("duplicate class definition for name") ) {
                logReifyException(error, false);
            }
            else {
                logReifyException(error, true);
            }
        }
        catch (Exception ex) {
            logReifyException(ex, true);
        }

        // If we get here, there's some other class in this classloader hierarchy with the same name. In order to
        // avoid a naming conflict, we set reified class to parent and skip reification.

        if (superClass.reifiedClass != null) {
            reifiedClass = superClass.reifiedClass;
            allocator = superClass.allocator;
        }
    }

    private final class Reificator {

        private final Class reifiedParent;

        Reificator(Class<?> reifiedParent) {
            this.reifiedParent = reifiedParent;
        }

        byte[] reify(final String javaName, final String javaPath) {
            final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            cw.visit(RubyInstanceConfig.JAVA_VERSION, ACC_PUBLIC + ACC_SUPER, javaPath, null, p(reifiedParent), interfaces());

            if (classAnnotations != null && !classAnnotations.isEmpty()) {
                for (Map.Entry<Class,Map<String,Object>> entry : classAnnotations.entrySet()) {
                    Class annoType = entry.getKey();
                    Map<String,Object> fields = entry.getValue();

                    AnnotationVisitor av = cw.visitAnnotation(ci(annoType), true);
                    CodegenUtils.visitAnnotationFields(av, fields);
                    av.visitEnd();
                }
            }

            // fields to hold Ruby and RubyClass references
            cw.visitField(ACC_STATIC | ACC_PRIVATE, "ruby", ci(Ruby.class), null, null);
            cw.visitField(ACC_STATIC | ACC_PRIVATE, "rubyClass", ci(RubyClass.class), null, null);

            // static initializing method
            SkinnyMethodAdapter m = new SkinnyMethodAdapter(cw, ACC_PUBLIC | ACC_STATIC, "clinit", sig(void.class, Ruby.class, RubyClass.class), null, null);
            m.start();
            m.aload(0);
            m.putstatic(javaPath, "ruby", ci(Ruby.class));
            m.aload(1);
            m.putstatic(javaPath, "rubyClass", ci(RubyClass.class));
            m.voidreturn();
            m.end();

            // standard constructor that accepts Ruby, RubyClass
            m = new SkinnyMethodAdapter(cw, ACC_PUBLIC, "<init>", sig(void.class, Ruby.class, RubyClass.class), null, null);
            m.aload(0);
            m.aload(1);
            m.aload(2);
            m.invokespecial(p(reifiedParent), "<init>", sig(void.class, Ruby.class, RubyClass.class));
            m.voidreturn();
            m.end();

            // no-arg constructor using static references to Ruby and RubyClass
            m = new SkinnyMethodAdapter(cw, ACC_PUBLIC, "<init>", CodegenUtils.sig(void.class), null, null);
            m.aload(0);
            m.getstatic(javaPath, "ruby", ci(Ruby.class));
            m.getstatic(javaPath, "rubyClass", ci(RubyClass.class));
            m.invokespecial(p(reifiedParent), "<init>", sig(void.class, Ruby.class, RubyClass.class));
            m.voidreturn();
            m.end();

            // define fields
            for (Map.Entry<String, Class> fieldSignature : getFieldSignatures().entrySet()) {
                String fieldName = fieldSignature.getKey();
                Class type = fieldSignature.getValue();
                Map<Class, Map<String, Object>> fieldAnnos = getFieldAnnotations().get(fieldName);

                FieldVisitor fieldVisitor = cw.visitField(ACC_PUBLIC, fieldName, ci(type), null, null);

                if (fieldAnnos == null) continue;

                for (Map.Entry<Class, Map<String, Object>> fieldAnno : fieldAnnos.entrySet()) {
                    Class annoType = fieldAnno.getKey();
                    AnnotationVisitor av = fieldVisitor.visitAnnotation(ci(annoType), true);
                    CodegenUtils.visitAnnotationFields(av, fieldAnno.getValue());
                }
                fieldVisitor.visitEnd();
            }

            // gather a list of instance methods, so we don't accidentally make static ones that conflict
            final Set<String> instanceMethods = new HashSet<String>(getMethods().size());

            // define instance methods
            for (Map.Entry<String,DynamicMethod> methodEntry : getMethods().entrySet()) {
                final String methodName = methodEntry.getKey();

                if ( ! JavaNameMangler.willMethodMangleOk(methodName) ) {
                    LOG.debug("{} method: '{}' won't be part of reified Java class", getName(), methodName);
                    continue;
                }

                String javaMethodName = JavaNameMangler.mangleMethodName(methodName);

                Map<Class,Map<String,Object>> methodAnnos = getMethodAnnotations().get(methodName);
                List<Map<Class,Map<String,Object>>> parameterAnnos = getParameterAnnotations().get(methodName);
                Class[] methodSignature = getMethodSignatures().get(methodName);

                final String signature;
                if (methodSignature == null) { // non-signature signature with just IRubyObject
                    final Arity arity = methodEntry.getValue().getArity();
                    switch (arity.getValue()) {
                        case 0:
                            signature = sig(IRubyObject.class); // return IRubyObject foo()
                            m = new SkinnyMethodAdapter(cw, ACC_PUBLIC, javaMethodName, signature, null, null);
                            generateMethodAnnotations(methodAnnos, m, parameterAnnos);

                            m.aload(0);
                            m.ldc(methodName);
                            m.invokevirtual(javaPath, "callMethod", sig(IRubyObject.class, String.class));
                            break;
                        case 1:
                            signature = sig(IRubyObject.class, IRubyObject.class); // return IRubyObject foo(IRubyObject arg1)
                            m = new SkinnyMethodAdapter(cw, ACC_PUBLIC, javaMethodName, signature, null, null);
                            generateMethodAnnotations(methodAnnos, m, parameterAnnos);

                            m.aload(0);
                            m.ldc(methodName);
                            m.aload(1); // IRubyObject arg1
                            m.invokevirtual(javaPath, "callMethod", sig(IRubyObject.class, String.class, IRubyObject.class));
                            break;
                        // currently we only have :
                        //  callMethod(context, name)
                        //  callMethod(context, name, arg1)
                        // so for other arities use generic:
                        //  callMethod(context, name, args...)
                        default:
                            if ( arity.isFixed() ) {
                                final int paramCount = arity.getValue();
                                Class[] params = new Class[paramCount]; Arrays.fill(params, IRubyObject.class);
                                signature = sig(IRubyObject.class, params);
                                m = new SkinnyMethodAdapter(cw, ACC_PUBLIC, javaMethodName, signature, null, null);
                                generateMethodAnnotations(methodAnnos, m, parameterAnnos);

                                m.aload(0);
                                m.ldc(methodName);

                                // generate an IRubyObject[] for the method arguments :
                                m.pushInt(paramCount);
                                m.anewarray(p(IRubyObject.class)); // new IRubyObject[size]
                                for ( int i = 1; i <= paramCount; i++ ) {
                                    m.dup();
                                    m.pushInt(i - 1); // array index e.g. iconst_0
                                    m.aload(i); // IRubyObject arg1, arg2 e.g. aload_1
                                    m.aastore(); // arr[ i - 1 ] = arg_i
                                }
                            }
                            else { // (generic) variable arity e.g. method(*args)
                                // NOTE: maybe improve to match fixed part for < -1 e.g. (IRubObject, IRubyObject, IRubyObject...)
                                signature = sig(IRubyObject.class, IRubyObject[].class);
                                m = new SkinnyMethodAdapter(cw, ACC_PUBLIC | ACC_VARARGS, javaMethodName, signature, null, null);
                                generateMethodAnnotations(methodAnnos, m, parameterAnnos);

                                m.aload(0);
                                m.ldc(methodName);
                                m.aload(1); // IRubyObject[] arg1
                            }
                            m.invokevirtual(javaPath, "callMethod", sig(IRubyObject.class, String.class, IRubyObject[].class));
                    }
                    m.areturn();
                }
                else { // generate a real method signature for the method, with to/from coercions

                    // indices for temp values
                    Class[] params = new Class[methodSignature.length - 1];
                    ArraySupport.copy(methodSignature, 1, params, 0, params.length);
                    final int baseIndex = RealClassGenerator.calcBaseIndex(params, 1);
                    final int rubyIndex = baseIndex;

                    signature = sig(methodSignature[0], params);
                    int mod = ACC_PUBLIC;
                    if ( isVarArgsSignature(methodName, methodSignature) ) mod |= ACC_VARARGS;
                    m = new SkinnyMethodAdapter(cw, mod, javaMethodName, signature, null, null);
                    generateMethodAnnotations(methodAnnos, m, parameterAnnos);

                    m.getstatic(javaPath, "ruby", ci(Ruby.class));
                    m.astore(rubyIndex);

                    m.aload(0); // self
                    m.ldc(methodName); // method name
                    RealClassGenerator.coerceArgumentsToRuby(m, params, rubyIndex);
                    m.invokevirtual(javaPath, "callMethod", sig(IRubyObject.class, String.class, IRubyObject[].class));

                    RealClassGenerator.coerceResultAndReturn(m, methodSignature[0]);
                }

                if (DEBUG_REIFY) LOG.debug("defining {}#{} as {}#{}", getName(), methodName, javaName, javaMethodName + signature);

                instanceMethods.add(javaMethodName + signature);

                m.end();
            }

            // define class/static methods
            for (Map.Entry<String,DynamicMethod> methodEntry : getMetaClass().getMethods().entrySet()) {
                String methodName = methodEntry.getKey();

                if (!JavaNameMangler.willMethodMangleOk(methodName)) continue;

                String javaMethodName = JavaNameMangler.mangleMethodName(methodName);

                Map<Class,Map<String,Object>> methodAnnos = getMetaClass().getMethodAnnotations().get(methodName);
                List<Map<Class,Map<String,Object>>> parameterAnnos = getMetaClass().getParameterAnnotations().get(methodName);
                Class[] methodSignature = getMetaClass().getMethodSignatures().get(methodName);

                String signature;
                if (methodSignature == null) {
                    final Arity arity = methodEntry.getValue().getArity();
                    // non-signature signature with just IRubyObject
                    switch (arity.getValue()) {
                        case 0:
                            signature = sig(IRubyObject.class);
                            if (instanceMethods.contains(javaMethodName + signature)) continue;
                            m = new SkinnyMethodAdapter(cw, ACC_PUBLIC | ACC_STATIC, javaMethodName, signature, null, null);
                            generateMethodAnnotations(methodAnnos, m, parameterAnnos);

                            m.getstatic(javaPath, "rubyClass", ci(RubyClass.class));
                            //m.invokevirtual("org/jruby/RubyClass", "getMetaClass", sig(RubyClass.class) );
                            m.ldc(methodName);
                            m.invokevirtual("org/jruby/RubyClass", "callMethod", sig(IRubyObject.class, String.class) );
                            break;
                        default:
                            signature = sig(IRubyObject.class, IRubyObject[].class);
                            if (instanceMethods.contains(javaMethodName + signature)) continue;
                            m = new SkinnyMethodAdapter(cw, ACC_PUBLIC | ACC_VARARGS | ACC_STATIC, javaMethodName, signature, null, null);
                            generateMethodAnnotations(methodAnnos, m, parameterAnnos);

                            m.getstatic(javaPath, "rubyClass", ci(RubyClass.class));
                            m.ldc(methodName);
                            m.aload(0);
                            m.invokevirtual("org/jruby/RubyClass", "callMethod", sig(IRubyObject.class, String.class, IRubyObject[].class) );
                    }
                    m.areturn();
                }
                else { // generate a real method signature for the method, with to/from coercions

                    // indices for temp values
                    Class[] params = new Class[methodSignature.length - 1];
                    System.arraycopy(methodSignature, 1, params, 0, params.length);
                    final int baseIndex = RealClassGenerator.calcBaseIndex(params, 0);
                    int rubyIndex = baseIndex;

                    signature = sig(methodSignature[0], params);
                    if (instanceMethods.contains(javaMethodName + signature)) continue;
                    m = new SkinnyMethodAdapter(cw, ACC_PUBLIC | ACC_VARARGS | ACC_STATIC, javaMethodName, signature, null, null);
                    generateMethodAnnotations(methodAnnos, m, parameterAnnos);

                    m.getstatic(javaPath, "ruby", ci(Ruby.class));
                    m.astore(rubyIndex);

                    m.getstatic(javaPath, "rubyClass", ci(RubyClass.class));

                    m.ldc(methodName); // method name
                    RealClassGenerator.coerceArgumentsToRuby(m, params, rubyIndex);
                    m.invokevirtual("org/jruby/RubyClass", "callMethod", sig(IRubyObject.class, String.class, IRubyObject[].class));

                    RealClassGenerator.coerceResultAndReturn(m, methodSignature[0]);
                }

                if (DEBUG_REIFY) LOG.debug("defining {}.{} as {}.{}", getName(), methodName, javaName, javaMethodName + signature);

                m.end();
            }

            cw.visitEnd();

            return cw.toByteArray();
        }

        private String[] interfaces() {
            final Class[] interfaces = Java.getInterfacesFromRubyClass(RubyClass.this);
            final String[] interfaceNames = new String[interfaces.length + 1];
            // mark this as a Reified class
            interfaceNames[0] = p(Reified.class);
            // add the other user-specified interfaces
            for (int i = 0; i < interfaces.length; i++) {
                interfaceNames[i + 1] = p(interfaces[i]);
            }
            return interfaceNames;
        }

    } // class Reificator

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

    public void setReifiedClass(Class<? extends IRubyObject> reifiedClass) {
        this.reifiedClass = reifiedClass;
    }

    public Class<? extends IRubyObject> getReifiedClass() {
        return reifiedClass;
    }

    public static Class<? extends IRubyObject> nearestReifiedClass(final RubyClass klass) {
        RubyClass current = klass;
        do {
            Class<? extends IRubyObject> reified = current.getReifiedClass();
            if ( reified != null ) return reified;
            current = current.getSuperClass();
        }
        while ( current != null );
        return null;
    }

    public Map<String, List<Map<Class, Map<String,Object>>>> getParameterAnnotations() {
        if (parameterAnnotations == null) return Collections.EMPTY_MAP;
        return parameterAnnotations;
    }

    public synchronized void addParameterAnnotation(String method, int i, Class annoClass, Map<String,Object> value) {
        if (parameterAnnotations == null) parameterAnnotations = new HashMap<>(8);
        List<Map<Class,Map<String,Object>>> paramList = parameterAnnotations.get(method);
        if (paramList == null) {
            paramList = new ArrayList<>(i + 1);
            parameterAnnotations.put(method, paramList);
        }
        if (paramList.size() < i + 1) {
            for (int j = paramList.size(); j < i + 1; j++) {
                paramList.add(null);
            }
        }
        if (annoClass != null && value != null) {
            Map<Class, Map<String, Object>> annos = paramList.get(i);
            if (annos == null) {
                paramList.set(i, annos = new LinkedHashMap<>(4));
            }
            annos.put(annoClass, value);
        } else {
            paramList.set(i, null);
        }
    }

    public Map<String,Map<Class,Map<String,Object>>> getMethodAnnotations() {
        if (methodAnnotations == null) return Collections.EMPTY_MAP;

        return methodAnnotations;
    }

    public Map<String,Map<Class,Map<String,Object>>> getFieldAnnotations() {
        if (fieldAnnotations == null) return Collections.EMPTY_MAP;

        return fieldAnnotations;
    }

    public synchronized void addMethodAnnotation(String methodName, Class annotation, Map fields) {
        if (methodAnnotations == null) methodAnnotations = new HashMap<>(8);

        Map<Class,Map<String,Object>> annos = methodAnnotations.get(methodName);
        if (annos == null) {
            methodAnnotations.put(methodName, annos = new LinkedHashMap<>(4));
        }

        annos.put(annotation, fields);
    }

    public synchronized void addFieldAnnotation(String fieldName, Class annotation, Map fields) {
        if (fieldAnnotations == null) fieldAnnotations = new HashMap<>(8);

        Map<Class,Map<String,Object>> annos = fieldAnnotations.get(fieldName);
        if (annos == null) {
            fieldAnnotations.put(fieldName, annos = new LinkedHashMap<>(4));
        }

        annos.put(annotation, fields);
    }


    public Map<String,Class[]> getMethodSignatures() {
        if (methodSignatures == null) return Collections.EMPTY_MAP;

        return methodSignatures;
    }

    public Map<String, Class> getFieldSignatures() {
        if (fieldSignatures == null) return Collections.EMPTY_MAP;

        return fieldSignatures;
    }

    public synchronized void addMethodSignature(String methodName, Class[] types) {
        if (methodSignatures == null) methodSignatures = new HashMap<>(16);

        methodSignatures.put(methodName, types);
    }

    public synchronized void addFieldSignature(String fieldName, Class type) {
        if (fieldSignatures == null) fieldSignatures = new LinkedHashMap<>(8);

        fieldSignatures.put(fieldName, type);
    }

    public Map<Class,Map<String,Object>> getClassAnnotations() {
        if (classAnnotations == null) return Collections.EMPTY_MAP;

        return classAnnotations;
    }

    public synchronized void addClassAnnotation(Class annotation, Map fields) {
        if (classAnnotations == null) classAnnotations = new LinkedHashMap<>(4);

        classAnnotations.put(annotation, fields);
    }

    @Override
    public Object toJava(final Class target) {
        if (target == Class.class) {
            if (reifiedClass == null) reifyWithAncestors(); // possibly auto-reify
            // Class requested; try java_class or else return nearest reified class
            final ThreadContext context = getRuntime().getCurrentContext();
            IRubyObject javaClass = JavaClass.java_class(context, this);
            if ( ! javaClass.isNil() ) return javaClass.toJava(target);

            Class reifiedClass = nearestReifiedClass(this);
            if ( reifiedClass != null ) return reifiedClass;
            // should never fall through, since RubyObject has a reified class
        }

        if (target.isAssignableFrom(RubyClass.class)) {
            // they're asking for something RubyClass extends, give them that
            return this;
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
         * @param method The method to invoke, or null in the case of default
         * marshaling.
         * @param type The type of marshaling to perform, from @MarshalType
         * @param generation The generation of the associated class at the time
         * of creation.
         */
        public MarshalTuple(DynamicMethod method, MarshalType type, int generation) {
            this.method = method;
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
                    stream.userNewMarshal(object, method);
                    return;
                case OLD_USER:
                    stream.userMarshal(object, method);
                    return;
                case DEFAULT_SLOW:
                    if (object.respondsTo("marshal_dump")) {
                        stream.userNewMarshal(object);
                    } else if (object.respondsTo("_dump")) {
                        stream.userMarshal(object);
                    } else {
                        stream.writeDirectly(object);
                    }
                    return;
            }
        }

        /** A "null" tuple, used as the default value for caches. */
        public static final MarshalTuple NULL_TUPLE = new MarshalTuple(null, null, 0);
        /** The method associated with this tuple. */
        public final DynamicMethod method;
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
            DynamicMethod method = searchMethod("respond_to?");
            if (!method.equals(runtime.getRespondToMethod()) && !method.isUndefined()) {

                // custom respond_to?, always do slow default marshaling
                tuple = (cachedDumpMarshal = new MarshalTuple(null, MarshalType.DEFAULT_SLOW, generation));

            } else if (!(method = searchMethod("marshal_dump")).isUndefined()) {

                // object really has 'marshal_dump', cache "new" user marshaling
                tuple = (cachedDumpMarshal = new MarshalTuple(method, MarshalType.NEW_USER, generation));

            } else if (!(method = searchMethod("_dump")).isUndefined()) {

                // object really has '_dump', cache "old" user marshaling
                tuple = (cachedDumpMarshal = new MarshalTuple(method, MarshalType.OLD_USER, generation));

            } else {

                // no respond_to?, marshal_dump, or _dump, so cache default marshaling
                tuple = (cachedDumpMarshal = new MarshalTuple(null, MarshalType.DEFAULT, generation));
            }
        }

        tuple.dump(stream, target);
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
            cache.method.call(context, target, this, "marshal_load", data);
            return target;
        } else {
            DynamicMethod method = searchMethod("respond_to?");
            if (!method.equals(runtime.getRespondToMethod()) && !method.isUndefined()) {

                // custom respond_to?, cache nothing and use slow path
                if (method.call(context, target, this, "respond_to?", runtime.newSymbol("marshal_load")).isTrue()) {
                    target.callMethod(context, "marshal_load", data);
                    return target;
                } else {
                    throw runtime.newTypeError("class " + getName() + " needs to have method `marshal_load'");
                }

            } else if (!(cache = searchWithCache("marshal_load")).method.isUndefined()) {

                // real marshal_load defined, cache and call it
                cachedLoad = cache;
                cache.method.call(context, target, this, "marshal_load", data);
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
        CacheEntry cache;
        if ((cache = getSingletonClass().cachedLoad).token == getSingletonClass().generation) {
            return cache.method.call(context, this, getSingletonClass(), "_load", data);
        } else {
            DynamicMethod method = getSingletonClass().searchMethod("respond_to?");
            if (!method.equals(runtime.getRespondToMethod()) && !method.isUndefined()) {

                // custom respond_to?, cache nothing and use slow path
                if (method.call(context, this, getSingletonClass(), "respond_to?", runtime.newSymbol("_load")).isTrue()) {
                    return callMethod(context, "_load", data);
                } else {
                    throw runtime.newTypeError("class " + getName() + " needs to have method `_load'");
                }

            } else if (!(cache = getSingletonClass().searchWithCache("_load")).method.isUndefined()) {

                // real _load defined, cache and call it
                getSingletonClass().cachedLoad = cache;
                return cache.method.call(context, this, getSingletonClass(), "_load", data);

            } else {

                // provide an error, since it doesn't exist
                throw runtime.newTypeError("class " + getName() + " needs to have method `_load'");

            }
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
    public IRubyObject invoke(ThreadContext context, IRubyObject self, String name,
                              CallType callType, Block block) {
        DynamicMethod method = searchMethod(name);
        IRubyObject caller = context.getFrameSelf();
        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, method.getVisibility(), name, callType, block);
        }
        return method.call(context, self, this, name, block);
    }

    /**
     * This method is deprecated because it depends on having a Ruby frame pushed for checking method visibility,
     * and there's no way to enforce that. Most users of this method probably don't need to check visibility.
     *
     * See https://github.com/jruby/jruby/issues/4134
     *
     * @deprecated Use finvoke if you do not want visibility-checking or invokeFrom if you do.
     */
    public IRubyObject invoke(ThreadContext context, IRubyObject self, String name,
                              IRubyObject[] args, CallType callType, Block block) {
        assert args != null;
        DynamicMethod method = searchMethod(name);
        IRubyObject caller = context.getFrameSelf();
        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, method.getVisibility(), name, callType, args, block);
        }
        return method.call(context, self, this, name, args, block);
    }

    /**
     * This method is deprecated because it depends on having a Ruby frame pushed for checking method visibility,
     * and there's no way to enforce that. Most users of this method probably don't need to check visibility.
     *
     * See https://github.com/jruby/jruby/issues/4134
     *
     * @deprecated Use finvoke if you do not want visibility-checking or invokeFrom if you do.
     */
    public IRubyObject invoke(ThreadContext context, IRubyObject self, String name,
                              IRubyObject arg, CallType callType, Block block) {
        DynamicMethod method = searchMethod(name);
        IRubyObject caller = context.getFrameSelf();
        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, method.getVisibility(), name, callType, arg, block);
        }
        return method.call(context, self, this, name, arg, block);
    }

    /**
     * This method is deprecated because it depends on having a Ruby frame pushed for checking method visibility,
     * and there's no way to enforce that. Most users of this method probably don't need to check visibility.
     *
     * See https://github.com/jruby/jruby/issues/4134
     *
     * @deprecated Use finvoke if you do not want visibility-checking or invokeFrom if you do.
     */
    public IRubyObject invoke(ThreadContext context, IRubyObject self, String name,
                              IRubyObject arg0, IRubyObject arg1, CallType callType, Block block) {
        DynamicMethod method = searchMethod(name);
        IRubyObject caller = context.getFrameSelf();
        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, method.getVisibility(), name, callType, arg0, arg1, block);
        }
        return method.call(context, self, this, name, arg0, arg1, block);
    }

    /**
     * This method is deprecated because it depends on having a Ruby frame pushed for checking method visibility,
     * and there's no way to enforce that. Most users of this method probably don't need to check visibility.
     *
     * See https://github.com/jruby/jruby/issues/4134
     *
     * @deprecated Use finvoke if you do not want visibility-checking or invokeFrom if you do.
     */
    public IRubyObject invoke(ThreadContext context, IRubyObject self, String name,
                              IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, CallType callType, Block block) {
        DynamicMethod method = searchMethod(name);
        IRubyObject caller = context.getFrameSelf();
        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, method.getVisibility(), name, callType, arg0, arg1, arg2, block);
        }
        return method.call(context, self, this, name, arg0, arg1, arg2, block);
    }

    /**
     * This method is deprecated because it depends on having a Ruby frame pushed for checking method visibility,
     * and there's no way to enforce that. Most users of this method probably don't need to check visibility.
     *
     * See https://github.com/jruby/jruby/issues/4134
     *
     * @deprecated Use finvoke if you do not want visibility-checking or invokeFrom if you do.
     */
    public IRubyObject invoke(ThreadContext context, IRubyObject self, String name,
                              CallType callType) {
        DynamicMethod method = searchMethod(name);
        IRubyObject caller = context.getFrameSelf();
        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, method.getVisibility(), name, callType, Block.NULL_BLOCK);
        }
        return method.call(context, self, this, name);
    }

    /**
     * This method is deprecated because it depends on having a Ruby frame pushed for checking method visibility,
     * and there's no way to enforce that. Most users of this method probably don't need to check visibility.
     *
     * See https://github.com/jruby/jruby/issues/4134
     *
     * @deprecated Use finvoke if you do not want visibility-checking or invokeFrom if you do.
     */
    public IRubyObject invoke(ThreadContext context, IRubyObject self, String name,
                              IRubyObject[] args, CallType callType) {
        assert args != null;
        DynamicMethod method = searchMethod(name);
        IRubyObject caller = context.getFrameSelf();
        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, method.getVisibility(), name, callType, args, Block.NULL_BLOCK);
        }
        return method.call(context, self, this, name, args);
    }

    /**
     * This method is deprecated because it depends on having a Ruby frame pushed for checking method visibility,
     * and there's no way to enforce that. Most users of this method probably don't need to check visibility.
     *
     * See https://github.com/jruby/jruby/issues/4134
     *
     * @deprecated Use finvoke if you do not want visibility-checking or invokeFrom if you do.
     */
    public IRubyObject invoke(ThreadContext context, IRubyObject self, String name,
                              IRubyObject arg, CallType callType) {
        DynamicMethod method = searchMethod(name);
        IRubyObject caller = context.getFrameSelf();
        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, method.getVisibility(), name, callType, arg, Block.NULL_BLOCK);
        }
        return method.call(context, self, this, name, arg);
    }

    /**
     * This method is deprecated because it depends on having a Ruby frame pushed for checking method visibility,
     * and there's no way to enforce that. Most users of this method probably don't need to check visibility.
     *
     * See https://github.com/jruby/jruby/issues/4134
     *
     * @deprecated Use finvoke if you do not want visibility-checking or invokeFrom if you do.
     */
    public IRubyObject invoke(ThreadContext context, IRubyObject self, String name,
                              IRubyObject arg0, IRubyObject arg1, CallType callType) {
        DynamicMethod method = searchMethod(name);
        IRubyObject caller = context.getFrameSelf();
        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, method.getVisibility(), name, callType, arg0, arg1, Block.NULL_BLOCK);
        }
        return method.call(context, self, this, name, arg0, arg1);
    }

    /**
     * This method is deprecated because it depends on having a Ruby frame pushed for checking method visibility,
     * and there's no way to enforce that. Most users of this method probably don't need to check visibility.
     *
     * See https://github.com/jruby/jruby/issues/4134
     *
     * @deprecated Use finvoke if you do not want visibility-checking or invokeFrom if you do.
     */
    public IRubyObject invoke(ThreadContext context, IRubyObject self, String name,
                              IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, CallType callType) {
        DynamicMethod method = searchMethod(name);
        IRubyObject caller = context.getFrameSelf();
        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return Helpers.callMethodMissing(context, self, method.getVisibility(), name, callType, arg0, arg1, arg2, Block.NULL_BLOCK);
        }
        return method.call(context, self, this, name, arg0, arg1, arg2);
    }

    // OBJECT STATE

    protected final Ruby runtime;
    private ObjectAllocator allocator; // the default allocator
    protected ObjectMarshal marshal;
    private Set<RubyClass> subclasses;
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

    private Class reifiedClass;

    private Map<String, List<Map<Class, Map<String,Object>>>> parameterAnnotations;

    private Map<String, Map<Class, Map<String,Object>>> methodAnnotations;

    private Map<String, Map<Class, Map<String,Object>>> fieldAnnotations;

    private Map<String, Class[]> methodSignatures;

    private Map<String, Class> fieldSignatures;

    private Map<Class, Map<String,Object>> classAnnotations;

    /** A cached tuple of method, type, and generation for dumping */
    private MarshalTuple cachedDumpMarshal = MarshalTuple.NULL_TUPLE;

    /** A cached tuple of method and generation for marshal loading */
    private CacheEntry cachedLoad = CacheEntry.NULL_CACHE;

    /** The "real" class, used by includes and singletons to locate the actual type of the object */
    private final RubyClass realClass;

    /** Variable table manager for this class */
    private final VariableTableManager variableTableManager;
}
