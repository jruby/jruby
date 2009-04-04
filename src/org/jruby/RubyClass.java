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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyClass;

import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.JavaMethod;
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
import org.jruby.util.SafePropertyAccessor;
import org.jruby.util.collections.WeakHashSet;

/**
 *
 * @author  jpetersen
 */
@JRubyClass(name="Class", parent="Module")
public class RubyClass extends RubyModule {
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
    private Map<String, VariableAccessor> ivarAccessors = (Map<String, VariableAccessor>)Collections.EMPTY_MAP;

    public Map<String, VariableAccessor> getVariableAccessorsForRead() {
        return ivarAccessors;
    }

    public synchronized Map<String, VariableAccessor> getVariableAccessorsForWrite() {
        if (ivarAccessors == Collections.EMPTY_MAP) ivarAccessors = new Hashtable<String, VariableAccessor>(1);
        return ivarAccessors;
    }

    public VariableAccessor getVariableAccessorForRead(String name) {
        VariableAccessor accessor = getVariableAccessorsForRead().get(name);
        if (accessor == null) accessor = VariableAccessor.DUMMY_ACCESSOR;
        return accessor;
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

    public int getVariableTableSize() {
        return getVariableAccessorsForRead().size();
    }

    public Map<String, VariableAccessor> getVariableTableCopy() {
        return new HashMap<String, VariableAccessor>(getVariableAccessorsForRead());
    }

    public void removeVariableAccessor(String name) {
        ivarAccessors.remove(name);
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

    protected final Ruby runtime;
    private ObjectAllocator allocator; // the default allocator
    protected ObjectMarshal marshal;
    private Set<RubyClass> subclasses;

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
            RubyClass cls = (RubyClass)self;
            IRubyObject obj = cls.allocate();
            cls.baseCallSites[CS_IDX_INITIALIZE].call(context, self, obj, args, block);
            return obj;
        }
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
            RubyClass cls = (RubyClass)self;
            IRubyObject obj = cls.allocate();
            cls.baseCallSites[CS_IDX_INITIALIZE].call(context, self, obj, block);
            return obj;
        }
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
            RubyClass cls = (RubyClass)self;
            IRubyObject obj = cls.allocate();
            cls.baseCallSites[CS_IDX_INITIALIZE].call(context, self, obj, arg0, block);
            return obj;
        }
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
            RubyClass cls = (RubyClass)self;
            IRubyObject obj = cls.allocate();
            cls.baseCallSites[CS_IDX_INITIALIZE].call(context, self, obj, arg0, arg1, block);
            return obj;
        }
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
            RubyClass cls = (RubyClass)self;
            IRubyObject obj = cls.allocate();
            cls.baseCallSites[CS_IDX_INITIALIZE].call(context, self, obj, arg0, arg1, arg2, block);
            return obj;
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
        if (subclasses != null) {
            Collection<RubyClass> mine = new ArrayList<RubyClass>(subclasses);
            if (includeDescendants) {
                for (RubyClass i: subclasses) {
                    mine.addAll(i.subclasses(includeDescendants));
                }
            }

            return mine;
        } else {
            return Collections.EMPTY_LIST;
        }
    }
    
    public synchronized void addSubclass(RubyClass subclass) {
        if (subclasses == null) subclasses = new WeakHashSet<RubyClass>();
        subclasses.add(subclass);
    }
    
    public synchronized void removeSubclass(RubyClass subclass) {
        if (subclasses == null) return;
        subclasses.remove(subclass);
    }

    protected void invalidateCacheDescendants() {
        super.invalidateCacheDescendants();
        // update all subclasses
        if (subclasses != null) for (RubyClass subclass : subclasses) {
            subclass.invalidateCacheDescendants();
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
}
