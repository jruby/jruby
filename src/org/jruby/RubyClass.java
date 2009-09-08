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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyClass;

import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.java.MiniJava;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ObjectMarshal;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.CodegenUtils;
import org.jruby.util.JRubyClassLoader;
import static org.jruby.util.CodegenUtils.*;
import org.jruby.util.JavaNameMangler;
import org.jruby.util.SafePropertyAccessor;
import org.jruby.util.collections.WeakHashSet;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

/**
 *
 * @author  jpetersen
 */
@JRubyClass(name="Class", parent="Module")
public class RubyClass extends RubyModule {
    public static void createClassClass(Ruby runtime, RubyClass classClass) {
        classClass.index = ClassIndex.CLASS;
        classClass.kindOf = new RubyModule.KindOf() {
            @Override
            public boolean isKindOf(IRubyObject obj, RubyModule type) {
                return obj instanceof RubyClass;
            }
        };
        
        classClass.undefineMethod("module_function");
        classClass.undefineMethod("append_features");
        classClass.undefineMethod("extend_object");
        
        classClass.defineAnnotatedMethods(RubyClass.class);
        
        classClass.addMethod("new", new SpecificArityNew(classClass, Visibility.PUBLIC));
        
        // This is a non-standard method; have we decided to start extending Ruby?
        //classClass.defineFastMethod("subclasses", callbackFactory.getFastOptMethod("subclasses"));
        
        // FIXME: for some reason this dispatcher causes a VerifyError...
        //classClass.dispatcher = callbackFactory.createDispatcher(classClass);
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
     * Set an allocator that calls the default constructor for a given class.
     *
     * @param cls The class on which to call the default constructor to allocate
     */
    public void setClassAllocator(final Class cls) {
        this.allocator = new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                try {
                    return (IRubyObject)cls.newInstance();
                } catch (InstantiationException ie) {
                    throw runtime.newTypeError("could not allocate " + cls + " with default constructor:\n" + ie);
                } catch (IllegalAccessException iae) {
                    throw runtime.newSecurityError("could not allocate " + cls + " due to inaccessible default constructor:\n" + iae);
                }
            }
        };
    }

    public void setRubyClassAllocator(final Class cls) {
        try {
            final Constructor constructor = cls.getConstructor(Ruby.class, RubyClass.class);
            
            this.allocator = new ObjectAllocator() {
                public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                    try {
                        return (IRubyObject)constructor.newInstance(runtime, klazz);
                    } catch (InvocationTargetException ite) {
                        throw runtime.newTypeError("could not allocate " + cls + " with (Ruby, RubyClass) constructor:\n" + ite);
                    } catch (InstantiationException ie) {
                        throw runtime.newTypeError("could not allocate " + cls + " with (Ruby, RubyClass) constructor:\n" + ie);
                    } catch (IllegalAccessException iae) {
                        throw runtime.newSecurityError("could not allocate " + cls + " due to inaccessible (Ruby, RubyClass) constructor:\n" + iae);
                    }
                }
            };
        } catch (NoSuchMethodException nsme) {
            throw new RuntimeException(nsme);
        }
    }

    @JRubyMethod(name = "allocate")
    public IRubyObject allocate() {
        if (superClass == null) throw runtime.newTypeError("can't instantiate uninitialized class");
        IRubyObject obj = allocator.allocate(runtime, this);
        if (obj.getMetaClass().getRealClass() != getRealClass()) throw runtime.newTypeError("wrong instance allocation");
        return obj;
    }

    public CallSite[] getBaseCallSites() {
        return baseCallSites;
    }
    
    public CallSite[] getExtraCallSites() {
        return extraCallSites;
    }

    public static class VariableAccessor {
        private int index;
        private final int classId;
        public VariableAccessor(int index, int classId) {
            this.index = index;
            this.classId = classId;
        }
        public int getClassId() {
            return classId;
        }
        public int getIndex() {
            return index;
        }
        public Object get(Object object) {
            return ((IRubyObject)object).getVariable(index);
        }
        public void set(Object object, Object value) {
            ((IRubyObject)object).setVariable(index, value);
        }
        public static final VariableAccessor DUMMY_ACCESSOR = new VariableAccessor(-1, -1);
    }

    @SuppressWarnings("unchecked")
    private Map<String, VariableAccessor> variableAccessors = (Map<String, VariableAccessor>)Collections.EMPTY_MAP;

    public Map<String, VariableAccessor> getVariableAccessorsForRead() {
        return variableAccessors;
    }

    public synchronized Map<String, VariableAccessor> getVariableAccessorsForWrite() {
        if (variableAccessors == Collections.EMPTY_MAP) variableAccessors = new Hashtable<String, VariableAccessor>(1);
        return variableAccessors;
    }

    public synchronized VariableAccessor getVariableAccessorForWrite(String name) {
        Map<String, VariableAccessor> myVariableAccessors = getVariableAccessorsForWrite();
        VariableAccessor ivarAccessor = myVariableAccessors.get(name);
        if (ivarAccessor == null) {
            ivarAccessor = new VariableAccessor(myVariableAccessors.size(), this.id);
            myVariableAccessors.put(name, ivarAccessor);
        }
        return ivarAccessor;
    }

    public VariableAccessor getVariableAccessorForRead(String name) {
        VariableAccessor accessor = getVariableAccessorsForRead().get(name);
        if (accessor == null) accessor = VariableAccessor.DUMMY_ACCESSOR;
        return accessor;
    }

    public int getVariableTableSize() {
        return variableAccessors.size();
    }

    public Map<String, VariableAccessor> getVariableTableCopy() {
        return new HashMap<String, VariableAccessor>(getVariableAccessorsForRead());
    }

    @Override
    public int getNativeTypeIndex() {
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
        this.superClass = superClass; // this is the only case it might be null here (in MetaClass construction)
    }

    /** separate path for MetaClass and IncludedModuleWrapper construction
     *  (rb_class_boot version for MetaClasses)
     *  no marshal, allocator initialization and addSubclass(this) here!
     */
    protected RubyClass(Ruby runtime, RubyClass superClass, Generation generation, boolean objectSpace) {
        super(runtime, runtime.getClassClass(), generation, objectSpace);
        this.runtime = runtime;
        this.superClass = superClass; // this is the only case it might be null here (in MetaClass construction)
    }
    
    /** used by CLASS_ALLOCATOR (any Class' class will be a Class!)
     *  also used to bootstrap Object class
     */
    protected RubyClass(Ruby runtime) {
        super(runtime, runtime.getClassClass());
        this.runtime = runtime;
        index = ClassIndex.CLASS;
    }
    
    /** rb_class_boot (for plain Classes)
     *  also used to bootstrap Module and Class classes 
     */
    protected RubyClass(Ruby runtime, RubyClass superClazz) {
        this(runtime);
        superClass = superClazz;
        marshal = superClazz.marshal; // use parent's marshal
        superClazz.addSubclass(this);
        
        infectBy(superClass);        
    }
    
    /** 
     * A constructor which allows passing in an array of supplementary call sites.
     */
    protected RubyClass(Ruby runtime, RubyClass superClazz, CallSite[] extraCallSites) {
        this(runtime);
        this.superClass = superClazz;
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
            MetaClass klass = new MetaClass(runtime, superClass); // rb_class_boot
            setMetaClass(klass);

            klass.setAttached(this);
            klass.setMetaClass(klass);
            klass.setSuperClass(getSuperClass().getRealClass().getMetaClass());
            
            return klass;
        } else {
            return super.makeMetaClass(superClass);
        }
    }
    
    @Deprecated
    public IRubyObject invoke(ThreadContext context, IRubyObject self, int methodIndex, String name, IRubyObject[] args, CallType callType, Block block) {
        return invoke(context, self, name, args, callType, block);
    }
    
    public boolean notVisibleAndNotMethodMissing(DynamicMethod method, String name, IRubyObject caller, CallType callType) {
        return !method.isCallableFrom(caller, callType) && !name.equals("method_missing");
    }
    
    public IRubyObject invoke(ThreadContext context, IRubyObject self, String name,
            CallType callType, Block block) {
        DynamicMethod method = searchMethod(name);
        IRubyObject caller = context.getFrameSelf();
        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return RuntimeHelpers.callMethodMissing(context, self, method.getVisibility(), name, callType, block);
        }
        return method.call(context, self, this, name, block);
    }
    
    public IRubyObject finvoke(ThreadContext context, IRubyObject self, String name, Block block) {
        DynamicMethod method = searchMethod(name);
        if (shouldCallMethodMissing(method)) {
            return RuntimeHelpers.callMethodMissing(context, self, method.getVisibility(), name, CallType.FUNCTIONAL, block);
        }
        return method.call(context, self, this, name, block);
    }
    
    public IRubyObject invoke(ThreadContext context, IRubyObject self, String name,
            IRubyObject[] args, CallType callType, Block block) {
        assert args != null;
        DynamicMethod method = searchMethod(name);
        IRubyObject caller = context.getFrameSelf();
        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return RuntimeHelpers.callMethodMissing(context, self, method.getVisibility(), name, callType, args, block);
        }
        return method.call(context, self, this, name, args, block);
    }
    
    public IRubyObject finvoke(ThreadContext context, IRubyObject self, String name,
            IRubyObject[] args, Block block) {
        assert args != null;
        DynamicMethod method = searchMethod(name);
        if (shouldCallMethodMissing(method)) {
            return RuntimeHelpers.callMethodMissing(context, self, method.getVisibility(), name, CallType.FUNCTIONAL, args, block);
        }
        return method.call(context, self, this, name, args, block);
    }
    
    public IRubyObject invoke(ThreadContext context, IRubyObject self, String name,
            IRubyObject arg, CallType callType, Block block) {
        DynamicMethod method = searchMethod(name);
        IRubyObject caller = context.getFrameSelf();
        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return RuntimeHelpers.callMethodMissing(context, self, method.getVisibility(), name, callType, arg, block);
        }
        return method.call(context, self, this, name, arg, block);
    }
    
    public IRubyObject finvoke(ThreadContext context, IRubyObject self, String name,
            IRubyObject arg, Block block) {
        DynamicMethod method = searchMethod(name);
        if (shouldCallMethodMissing(method)) {
            return RuntimeHelpers.callMethodMissing(context, self, method.getVisibility(), name, CallType.FUNCTIONAL, arg, block);
        }
        return method.call(context, self, this, name, arg, block);
    }
    
    public IRubyObject invoke(ThreadContext context, IRubyObject self, String name,
            IRubyObject arg0, IRubyObject arg1, CallType callType, Block block) {
        DynamicMethod method = searchMethod(name);
        IRubyObject caller = context.getFrameSelf();
        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return RuntimeHelpers.callMethodMissing(context, self, method.getVisibility(), name, callType, arg0, arg1, block);
        }
        return method.call(context, self, this, name, arg0, arg1, block);
    }
    
    public IRubyObject finvoke(ThreadContext context, IRubyObject self, String name,
            IRubyObject arg0, IRubyObject arg1, Block block) {
        DynamicMethod method = searchMethod(name);
        if (shouldCallMethodMissing(method)) {
            return RuntimeHelpers.callMethodMissing(context, self, method.getVisibility(), name, CallType.FUNCTIONAL, arg0, arg1, block);
        }
        return method.call(context, self, this, name, arg0, arg1, block);
    }
    
    public IRubyObject invoke(ThreadContext context, IRubyObject self, String name,
            IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, CallType callType, Block block) {
        DynamicMethod method = searchMethod(name);
        IRubyObject caller = context.getFrameSelf();
        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return RuntimeHelpers.callMethodMissing(context, self, method.getVisibility(), name, callType, arg0, arg1, arg2, block);
        }
        return method.call(context, self, this, name, arg0, arg1, arg2, block);
    }
    
    public IRubyObject finvoke(ThreadContext context, IRubyObject self, String name,
            IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        DynamicMethod method = searchMethod(name);
        if (shouldCallMethodMissing(method)) {
            return RuntimeHelpers.callMethodMissing(context, self, method.getVisibility(), name, CallType.FUNCTIONAL, arg0, arg1, arg2, block);
        }
        return method.call(context, self, this, name, arg0, arg1, arg2, block);
    }
    
    public IRubyObject invoke(ThreadContext context, IRubyObject self, String name,
            CallType callType) {
        DynamicMethod method = searchMethod(name);
        IRubyObject caller = context.getFrameSelf();
        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return RuntimeHelpers.callMethodMissing(context, self, method.getVisibility(), name, callType, Block.NULL_BLOCK);
        }
        return method.call(context, self, this, name);
    }
    
    public IRubyObject finvoke(ThreadContext context, IRubyObject self, String name) {
        DynamicMethod method = searchMethod(name);
        if (shouldCallMethodMissing(method)) {
            return RuntimeHelpers.callMethodMissing(context, self, method.getVisibility(), name, CallType.FUNCTIONAL, Block.NULL_BLOCK);
        }
        return method.call(context, self, this, name);
    }
    
    public IRubyObject invoke(ThreadContext context, IRubyObject self, String name,
            IRubyObject[] args, CallType callType) {
        assert args != null;
        DynamicMethod method = searchMethod(name);
        IRubyObject caller = context.getFrameSelf();
        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return RuntimeHelpers.callMethodMissing(context, self, method.getVisibility(), name, callType, args, Block.NULL_BLOCK);
        }
        return method.call(context, self, this, name, args);
    }
    
    public IRubyObject finvoke(ThreadContext context, IRubyObject self, String name,
            IRubyObject[] args) {
        assert args != null;
        DynamicMethod method = searchMethod(name);
        if (shouldCallMethodMissing(method)) {
            return RuntimeHelpers.callMethodMissing(context, self, method.getVisibility(), name, CallType.FUNCTIONAL, args, Block.NULL_BLOCK);
        }
        return method.call(context, self, this, name, args);
    }
    
    public IRubyObject invoke(ThreadContext context, IRubyObject self, String name,
            IRubyObject arg, CallType callType) {
        DynamicMethod method = searchMethod(name);
        IRubyObject caller = context.getFrameSelf();
        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return RuntimeHelpers.callMethodMissing(context, self, method.getVisibility(), name, callType, arg, Block.NULL_BLOCK);
        }
        return method.call(context, self, this, name, arg);
    }
    
    public IRubyObject finvoke(ThreadContext context, IRubyObject self, String name,
            IRubyObject arg) {
        DynamicMethod method = searchMethod(name);
        if (shouldCallMethodMissing(method)) {
            return RuntimeHelpers.callMethodMissing(context, self, method.getVisibility(), name, CallType.FUNCTIONAL, arg, Block.NULL_BLOCK);
        }
        return method.call(context, self, this, name, arg);
    }
    
    public IRubyObject invoke(ThreadContext context, IRubyObject self, String name,
            IRubyObject arg0, IRubyObject arg1, CallType callType) {
        DynamicMethod method = searchMethod(name);
        IRubyObject caller = context.getFrameSelf();
        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return RuntimeHelpers.callMethodMissing(context, self, method.getVisibility(), name, callType, arg0, arg1, Block.NULL_BLOCK);
        }
        return method.call(context, self, this, name, arg0, arg1);
    }
    
    public IRubyObject finvoke(ThreadContext context, IRubyObject self, String name,
            IRubyObject arg0, IRubyObject arg1) {
        DynamicMethod method = searchMethod(name);
        if (shouldCallMethodMissing(method)) {
            return RuntimeHelpers.callMethodMissing(context, self, method.getVisibility(), name, CallType.FUNCTIONAL, arg0, arg1, Block.NULL_BLOCK);
        }
        return method.call(context, self, this, name, arg0, arg1);
    }
    
    public IRubyObject invoke(ThreadContext context, IRubyObject self, String name,
            IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, CallType callType) {
        DynamicMethod method = searchMethod(name);
        IRubyObject caller = context.getFrameSelf();
        if (shouldCallMethodMissing(method, name, caller, callType)) {
            return RuntimeHelpers.callMethodMissing(context, self, method.getVisibility(), name, callType, arg0, arg1, arg2, Block.NULL_BLOCK);
        }
        return method.call(context, self, this, name, arg0, arg1, arg2);
    }
    
    public IRubyObject finvoke(ThreadContext context, IRubyObject self, String name,
            IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        DynamicMethod method = searchMethod(name);
        if (shouldCallMethodMissing(method)) {
            return RuntimeHelpers.callMethodMissing(context, self, method.getVisibility(), name, CallType.FUNCTIONAL, arg0, arg1, arg2, Block.NULL_BLOCK);
        }
        return method.call(context, self, this, name, arg0, arg1, arg2);
    }

    private void dumpReifiedClass(String dumpDir, String javaPath, byte[] classBytes) {
        if (dumpDir != null) {
            if (dumpDir.equals("")) {
                dumpDir = ".";
            }
            java.io.FileOutputStream classStream = null;
            try {
                java.io.File classFile = new java.io.File(dumpDir, javaPath + ".class");
                classFile.getParentFile().mkdirs();
                classStream = new java.io.FileOutputStream(classFile);
                classStream.write(classBytes);
            } catch (IOException io) {
                getRuntime().getWarnings().warn("unable to dump class file: " + io.getMessage());
            } finally {
                if (classStream != null) {
                    try {
                        classStream.close();
                    } catch (IOException ignored) {
                    }
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
            return RuntimeHelpers.callMethodMissing(context, self, method.getVisibility(), "inherited", CallType.FUNCTIONAL, Block.NULL_BLOCK);
        }

        return method.call(context, self, getMetaClass(), "inherited", subclass, Block.NULL_BLOCK);
    }

    /** rb_class_new_instance
    *
    */
    public IRubyObject newInstance(ThreadContext context, IRubyObject[] args, Block block) {
        IRubyObject obj = allocate();
        baseCallSites[CS_IDX_INITIALIZE].call(context, this, obj, args, block);
        return obj;
    }
    
    public static class SpecificArityNew extends JavaMethod {
        public SpecificArityNew(RubyModule implClass, Visibility visibility) {
            super(implClass, visibility);
        }
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            preBacktraceOnly(context, name);
            try {
                RubyClass cls = (RubyClass)self;
                IRubyObject obj = cls.allocate();
                cls.baseCallSites[CS_IDX_INITIALIZE].call(context, self, obj, args, block);
                return obj;
            } finally {
                postBacktraceOnly(context);
            }
        }
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
            preBacktraceOnly(context, name);
            try {
                RubyClass cls = (RubyClass)self;
                IRubyObject obj = cls.allocate();
                cls.baseCallSites[CS_IDX_INITIALIZE].call(context, self, obj, block);
                return obj;
            } finally {
                postBacktraceOnly(context);
            }
        }
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
            preBacktraceOnly(context, name);
            try {
                RubyClass cls = (RubyClass)self;
                IRubyObject obj = cls.allocate();
                cls.baseCallSites[CS_IDX_INITIALIZE].call(context, self, obj, arg0, block);
                return obj;
            } finally {
                postBacktraceOnly(context);
            }
        }
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
            preBacktraceOnly(context, name);
            try {
                RubyClass cls = (RubyClass)self;
                IRubyObject obj = cls.allocate();
                cls.baseCallSites[CS_IDX_INITIALIZE].call(context, self, obj, arg0, arg1, block);
                return obj;
            } finally {
                postBacktraceOnly(context);
            }
        }
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
            preBacktraceOnly(context, name);
            try {
                RubyClass cls = (RubyClass)self;
                IRubyObject obj = cls.allocate();
                cls.baseCallSites[CS_IDX_INITIALIZE].call(context, self, obj, arg0, arg1, arg2, block);
                return obj;
            } finally {
                postBacktraceOnly(context);
            }
        }
    }

    /** rb_class_initialize
     * 
     */
    @JRubyMethod(name = "initialize", compat = CompatVersion.RUBY1_8, frame = true, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, Block block) {
        checkNotInitialized();
        return initializeCommon(runtime.getObject(), block, false);
    }
        
    @JRubyMethod(name = "initialize", compat = CompatVersion.RUBY1_8, frame = true, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject superObject, Block block) {
        checkNotInitialized();
        checkInheritable(superObject);
        return initializeCommon((RubyClass)superObject, block, false);
    }
        
    @JRubyMethod(name = "initialize", compat = CompatVersion.RUBY1_9, frame = true, visibility = Visibility.PRIVATE)
    public IRubyObject initialize19(ThreadContext context, Block block) {
        checkNotInitialized();
        return initializeCommon(runtime.getObject(), block, true);
    }
        
    @JRubyMethod(name = "initialize", compat = CompatVersion.RUBY1_9, frame = true, visibility = Visibility.PRIVATE)
    public IRubyObject initialize19(ThreadContext context, IRubyObject superObject, Block block) {
        checkNotInitialized();
        checkInheritable(superObject);
        return initializeCommon((RubyClass)superObject, block, true);
    }

    private IRubyObject initializeCommon(RubyClass superClazz, Block block, boolean callInheritBeforeSuper) {
        superClass = superClazz;
        allocator = superClazz.allocator;
        makeMetaClass(superClazz.getMetaClass());

        marshal = superClazz.marshal;

        superClazz.addSubclass(this);

        if (callInheritBeforeSuper) {
            inherit(superClazz);
            super.initialize(block);
        } else {
            super.initialize(block);
            inherit(superClazz);
        }

        return this;
    }

    /** rb_class_init_copy
     * 
     */
    @JRubyMethod(name = "initialize_copy", required = 1, visibility = Visibility.PRIVATE)
    @Override
    public IRubyObject initialize_copy(IRubyObject original){
        if (superClass != null) throw runtime.newTypeError("already initialized class");
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
        this.superClass = superClass;
    }
    
    public Collection subclasses(boolean includeDescendants) {
        Set<RubyClass> mySubclasses = subclasses;
        if (mySubclasses != null) {
            Collection<RubyClass> mine = new ArrayList<RubyClass>(mySubclasses);
            if (includeDescendants) {
                for (RubyClass i: mySubclasses) {
                    mine.addAll(i.subclasses(includeDescendants));
                }
            }

            return mine;
        } else {
            return Collections.EMPTY_LIST;
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
    public synchronized void addSubclass(RubyClass subclass) {
        synchronized (runtime.getHierarchyLock()) {
            Set<RubyClass> oldSubclasses = subclasses;
            if (oldSubclasses == null) subclasses = oldSubclasses = new WeakHashSet<RubyClass>(4);
            oldSubclasses.add(subclass);
        }
    }
    
    /**
     * Remove a subclass from the weak set of subclasses.
     *
     * This version always constructs a new set to avoid having to synchronize
     * against the set when iterating it for invalidation in
     * invalidateCacheDescendants.
     *
     * @param subclass The subclass to remove
     */
    public synchronized void removeSubclass(RubyClass subclass) {
        synchronized (runtime.getHierarchyLock()) {
            Set<RubyClass> oldSubclasses = subclasses;
            if (oldSubclasses == null) return;

            oldSubclasses.remove(subclass);
        }
    }

    /**
     * Replace an existing subclass with a new one.
     *
     * This version always constructs a new set to avoid having to synchronize
     * against the set when iterating it for invalidation in
     * invalidateCacheDescendants.
     *
     * @param subclass The subclass to remove
     * @param newSubclass The subclass to replace it with
     */
    public synchronized void replaceSubclass(RubyClass subclass, RubyClass newSubclass) {
        synchronized (runtime.getHierarchyLock()) {
            Set<RubyClass> oldSubclasses = subclasses;
            if (oldSubclasses == null) return;

            oldSubclasses.remove(subclass);
            oldSubclasses.add(newSubclass);
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
        // update all subclasses
        synchronized (runtime.getHierarchyLock()) {
            Set<RubyClass> mySubclasses = subclasses;
            if (mySubclasses != null) for (RubyClass subclass : mySubclasses) {
                subclass.invalidateCacheDescendants();
            }
        }
    }
    
    public Ruby getClassRuntime() {
        return runtime;
    }

    public RubyClass getRealClass() {
        return this;
    }    

    @JRubyMethod(name = "inherited", required = 1, visibility = Visibility.PRIVATE)
    public IRubyObject inherited(ThreadContext context, IRubyObject arg) {
        return runtime.getNil();
    }

    /** rb_class_inherited (reversed semantics!)
     * 
     */
    public void inherit(RubyClass superClazz) {
        if (superClazz == null) superClazz = runtime.getObject();

        superClazz.invokeInherited(runtime.getCurrentContext(), superClazz, this);
    }

    /** Return the real super class of this class.
     * 
     * rb_class_superclass
     *
     */
    @JRubyMethod(name = "superclass", compat = CompatVersion.RUBY1_8)
    public IRubyObject superclass(ThreadContext context) {
        RubyClass superClazz = superClass;

        if (superClazz == null) throw runtime.newTypeError("uninitialized class");

        if (isSingleton()) superClazz = metaClass;
        while (superClazz != null && superClazz.isIncluded()) superClazz = superClazz.superClass;

        return superClazz != null ? superClazz : runtime.getNil();
    }
    
    @JRubyMethod(name = "superclass", compat = CompatVersion.RUBY1_9)
    public IRubyObject superclass19(ThreadContext context) {
        RubyClass superClazz = superClass;
        if (superClazz == null) {
            if (metaClass == runtime.getBasicObject()) return runtime.getNil();
            throw runtime.newTypeError("uninitialized class");
        }

        while (superClazz != null && superClazz.isIncluded()) superClazz = superClazz.superClass;

        return superClazz != null ? superClazz : runtime.getNil();
    }

    private void checkNotInitialized() {
        if (superClass != null) throw runtime.newTypeError("already initialized class");
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
        public void marshalTo(Ruby runtime, Object obj, RubyClass type,
                              MarshalStream marshalStream) throws IOException {
            IRubyObject object = (IRubyObject)obj;
            
            marshalStream.registerLinkTarget(object);
            marshalStream.dumpVariables(object.getVariableList());
        }

        public Object unmarshalFrom(Ruby runtime, RubyClass type,
                                    UnmarshalStream unmarshalStream) throws IOException {
            IRubyObject result = type.allocate();
            
            unmarshalStream.registerLinkTarget(result);

            unmarshalStream.defaultVariablesUnmarshal(result);

            return result;
        }
    };

    public synchronized void reify() {
        reify(null);
    }

    /**
     * Stand up a real Java class for the backing store of this object
     * @param classDumpDir Directory to save reified java class
     */
    public synchronized void reify(String classDumpDir) {
        Class reifiedParent = RubyObject.class;
        String javaName = "ruby." + getBaseName();
        String javaPath = "ruby/" + getBaseName();
        JRubyClassLoader parentCL = runtime.getJRubyClassLoader();

        if (superClass.reifiedClass != null) {
            reifiedParent = superClass.reifiedClass;
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.visit(RubyInstanceConfig.JAVA_VERSION, ACC_PUBLIC + ACC_SUPER, javaPath, null, p(reifiedParent), null);

        if (classAnnotations != null && classAnnotations.size() != 0) {
            for (Map.Entry<Class,Map<String,Object>> entry : classAnnotations.entrySet()) {
                Class annoType = entry.getKey();
                Map<String,Object> fields = entry.getValue();

                AnnotationVisitor av = cw.visitAnnotation(ci(annoType), true);
                CodegenUtils.visitAnnotationFields(av, entry.getValue());
                av.visitEnd();
            }
        }

        // fields to hold Ruby and RubyClass references
        cw.visitField(ACC_STATIC | ACC_PRIVATE, "ruby", ci(Ruby.class), null, null);
        cw.visitField(ACC_STATIC | ACC_PRIVATE, "rubyClass", ci(RubyClass.class), null, null);

        // static initializing method
        SkinnyMethodAdapter m = new SkinnyMethodAdapter(cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "clinit", sig(void.class, Ruby.class, RubyClass.class), null, null));
        m.start();
        m.aload(0);
        m.putstatic(javaPath, "ruby", ci(Ruby.class));
        m.aload(1);
        m.putstatic(javaPath, "rubyClass", ci(RubyClass.class));
        m.voidreturn();
        m.end();

        // standard constructor that accepts Ruby, RubyClass
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", sig(void.class, Ruby.class, RubyClass.class), null, null);
        m = new SkinnyMethodAdapter(mv);
        m.aload(0);
        m.aload(1);
        m.aload(2);
        m.invokespecial(p(reifiedParent), "<init>", sig(void.class, Ruby.class, RubyClass.class));
        m.voidreturn();
        m.end();

        // no-arg constructor using static references to Ruby and RubyClass
        mv = cw.visitMethod(ACC_PUBLIC, "<init>", CodegenUtils.sig(void.class), null, null);
        m = new SkinnyMethodAdapter(mv);
        m.aload(0);
        m.getstatic(javaPath, "ruby", ci(Ruby.class));
        m.getstatic(javaPath, "rubyClass", ci(RubyClass.class));
        m.invokespecial(p(reifiedParent), "<init>", sig(void.class, Ruby.class, RubyClass.class));
        m.voidreturn();
        m.end();

        for (Map.Entry<String,DynamicMethod> methodEntry : getMethods().entrySet()) {
            String methodName = methodEntry.getKey();
            String javaMethodName = JavaNameMangler.mangleStringForCleanJavaIdentifier(methodName);
            Map<Class,Map<String,Object>> methodAnnos = getMethodAnnotations().get(methodName);
            List<Map<Class,Map<String,Object>>> parameterAnnos = getParameterAnnotations().get(methodName);
            Class[] methodSignature = getMethodSignatures().get(methodName);

            if (methodSignature == null) {
                // non-signature signature with just IRubyObject
                switch (methodEntry.getValue().getArity().getValue()) {
                case 0:
                    mv = cw.visitMethod(ACC_PUBLIC | ACC_VARARGS, javaMethodName, sig(IRubyObject.class), null, null);
                    m = new SkinnyMethodAdapter(mv);
                    generateMethodAnnotations(methodAnnos, m, parameterAnnos);

                    m.aload(0);
                    m.ldc(methodName);
                    m.invokevirtual(javaPath, "callMethod", sig(IRubyObject.class, String.class));
                    break;
                default:
                    mv = cw.visitMethod(ACC_PUBLIC | ACC_VARARGS, javaMethodName, sig(IRubyObject.class, IRubyObject[].class), null, null);
                    m = new SkinnyMethodAdapter(mv);
                    generateMethodAnnotations(methodAnnos, m, parameterAnnos);

                    m.aload(0);
                    m.ldc(methodName);
                    m.aload(1);
                    m.invokevirtual(javaPath, "callMethod", sig(IRubyObject.class, String.class, IRubyObject[].class));
                }
                m.areturn();
            } else {
                // generate a real method signature for the method, with to/from coercions

                // indices for temp values
                Class[] params = new Class[methodSignature.length - 1];
                System.arraycopy(methodSignature, 1, params, 0, params.length);
                int baseIndex = 1;
                for (Class paramType : params) {
                    if (paramType == double.class || paramType == long.class) {
                        baseIndex += 2;
                    } else {
                        baseIndex += 1;
                    }
                }
                int rubyIndex = baseIndex;

                mv = cw.visitMethod(ACC_PUBLIC | ACC_VARARGS, javaMethodName, sig(methodSignature[0], params), null, null);
                m = new SkinnyMethodAdapter(mv);
                generateMethodAnnotations(methodAnnos, m, parameterAnnos);

                m.getstatic(javaPath, "ruby", ci(Ruby.class));
                m.astore(rubyIndex);

                m.aload(0); // self
                m.ldc(methodName); // method name
                MiniJava.coerceArgumentsToRuby(m, params, rubyIndex);
                m.invokevirtual(javaPath, "callMethod", sig(IRubyObject.class, String.class, IRubyObject[].class));

                MiniJava.coerceResultAndReturn(m, methodSignature[0]);
            }

            m.end();
        }

        cw.visitEnd();
        byte[] classBytes = cw.toByteArray();
        dumpReifiedClass(classDumpDir, javaPath, classBytes);
        Class result = parentCL.defineClass(javaName, classBytes);

        try {
            java.lang.reflect.Method clinit = result.getDeclaredMethod("clinit", Ruby.class, RubyClass.class);
            clinit.invoke(null, runtime, this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        setClassAllocator(result);
        reifiedClass = result;
    }

    public Class getReifiedClass() {
        return reifiedClass;
    }

    private Map<String, List<Map<Class, Map<String,Object>>>> parameterAnnotations;

    public Map<String, List<Map<Class, Map<String,Object>>>> getParameterAnnotations() {
        if (parameterAnnotations == null) return Collections.EMPTY_MAP;
        return parameterAnnotations;
    }

    public void addParameterAnnotation(String method, int i, Class annoClass, Map<String,Object> value) {
        if (parameterAnnotations == null) parameterAnnotations = new Hashtable<String,List<Map<Class,Map<String,Object>>>>();
        List<Map<Class,Map<String,Object>>> paramList = parameterAnnotations.get(method);
        if (paramList == null) {
            paramList = new ArrayList<Map<Class,Map<String,Object>>>(i + 1);
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
                annos = new HashMap<Class, Map<String, Object>>();
                paramList.set(i, annos);
            }
            annos.put(annoClass, value);
        } else {
            paramList.set(i, null);
        }
    }

    private Map<String, Map<Class, Map<String,Object>>> methodAnnotations;

    public Map<String,Map<Class,Map<String,Object>>> getMethodAnnotations() {
        if (methodAnnotations == null) return Collections.EMPTY_MAP;

        return methodAnnotations;
    }

    public void addMethodAnnotation(String methodName, Class annotation, Map fields) {
        if (methodAnnotations == null) methodAnnotations = new Hashtable<String,Map<Class,Map<String,Object>>>();

        Map<Class,Map<String,Object>> annos = methodAnnotations.get(methodName);
        if (annos == null) {
            annos = new Hashtable<Class,Map<String,Object>>();
            methodAnnotations.put(methodName, annos);
        }

        annos.put(annotation, fields);
    }

    private Map<String, Class[]> methodSignatures;

    public Map<String,Class[]> getMethodSignatures() {
        if (methodSignatures == null) return Collections.EMPTY_MAP;

        return methodSignatures;
    }

    public void addMethodSignature(String methodName, Class[] types) {
        if (methodSignatures == null) methodSignatures = new Hashtable<String,Class[]>();

        methodSignatures.put(methodName, types);
    }

    private Map<Class, Map<String,Object>> classAnnotations;

    public Map<Class,Map<String,Object>> getClassAnnotations() {
        if (classAnnotations == null) return Collections.EMPTY_MAP;

        return classAnnotations;
    }

    public void addClassAnnotation(Class annotation, Map fields) {
        if (classAnnotations == null) classAnnotations = new Hashtable<Class,Map<String,Object>>();

        classAnnotations.put(annotation, fields);
    }

    protected final Ruby runtime;
    private ObjectAllocator allocator; // the default allocator
    protected ObjectMarshal marshal;
    private Set<RubyClass> subclasses;
    public static final int CS_IDX_INITIALIZE = 0;
    public static final String[] CS_NAMES = {
        "initialize"
    };
    private final CallSite[] baseCallSites = new CallSite[CS_NAMES.length];
    {
        for(int i = 0; i < CS_NAMES.length; i++) {
            baseCallSites[i] = MethodIndex.getFunctionalCallSite(CS_NAMES[i]);
        }
    }

    private CallSite[] extraCallSites;

    private Class reifiedClass;
}
