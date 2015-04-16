/*
 **** BEGIN LICENSE BLOCK *****
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import org.jcodings.Encoding;
import org.jruby.anno.AnnotationBinder;
import org.jruby.anno.AnnotationHelper;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyConstant;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JavaMethodDescriptor;
import org.jruby.anno.TypePopulator;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.embed.Extension;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.AliasMethod;
import org.jruby.internal.runtime.methods.AttrReaderMethod;
import org.jruby.internal.runtime.methods.AttrWriterMethod;
import org.jruby.internal.runtime.methods.CacheableMethod;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.Framing;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.internal.runtime.methods.ProcMethod;
import org.jruby.internal.runtime.methods.Scoping;
import org.jruby.internal.runtime.methods.SynchronizedDynamicMethod;
import org.jruby.internal.runtime.methods.UndefinedMethod;
import org.jruby.internal.runtime.methods.WrapperMethod;
import org.jruby.ir.IRMethod;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.MethodFactory;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.Variable;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.runtime.callsite.FunctionalCachingCallSite;
import org.jruby.runtime.ivars.MethodData;
import org.jruby.runtime.load.IAutoloadMethod;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.runtime.opto.Invalidator;
import org.jruby.runtime.opto.OptoFactory;
import org.jruby.runtime.profile.MethodEnhancer;
import org.jruby.util.ClassProvider;
import org.jruby.util.IdUtil;
import org.jruby.util.TypeConverter;
import org.jruby.util.cli.Options;
import org.jruby.util.collections.WeakHashSet;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static org.jruby.anno.FrameField.*;
import static org.jruby.runtime.Visibility.*;


/**
 *
 * @author  jpetersen
 */
@JRubyClass(name="Module")
public class RubyModule extends RubyObject {

    private static final Logger LOG = LoggerFactory.getLogger("RubyModule");

    private static final boolean DEBUG = false;
    protected static final String ERR_INSECURE_SET_CONSTANT  = "Insecure: can't modify constant";
    protected static final String ERR_FROZEN_CONST_TYPE = "class/module ";

    public static final ObjectAllocator MODULE_ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyModule(runtime, klass);
        }
    };

    public static RubyClass createModuleClass(Ruby runtime, RubyClass moduleClass) {
        moduleClass.setClassIndex(ClassIndex.MODULE);
        moduleClass.setReifiedClass(RubyModule.class);
        moduleClass.kindOf = new RubyModule.JavaClassKindOf(RubyModule.class);

        moduleClass.defineAnnotatedMethods(RubyModule.class);
        moduleClass.defineAnnotatedMethods(ModuleKernelMethods.class);

        return moduleClass;
    }

    public void checkValidBindTargetFrom(ThreadContext context, RubyModule originModule) throws RaiseException {
        // Module methods can always be transplanted
        if (originModule.isModule()) return;

        if (!this.hasModuleInHierarchy(originModule)) {
            if (originModule instanceof MetaClass) {
                throw context.runtime.newTypeError("can't bind singleton method to a different class");
            } else {
                throw context.runtime.newTypeError("bind argument must be an instance of " + originModule.getName());
            }
        }
    }

    /**
     * Get the ClassIndex for this class. Will be NO_CLASS for non-core types.
     */
    public ClassIndex getClassIndex() {
        return classIndex;
    }

    /**
     * Set the ClassIndex for this core class. Only used at boot time for core
     * types.
     *
     * @param classIndex the ClassIndex for this type
     */
    @SuppressWarnings("deprecated")
    void setClassIndex(ClassIndex classIndex) {
        this.classIndex = classIndex;
        this.index = classIndex.ordinal();
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

    @Override
    public ClassIndex getNativeClassIndex() {
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

    public static class KindOf {
        public static final KindOf DEFAULT_KIND_OF = new KindOf();
        public boolean isKindOf(IRubyObject obj, RubyModule type) {
            return obj.getMetaClass().hasModuleInHierarchy(type);
        }
    }

    public static final class JavaClassKindOf extends RubyModule.KindOf {
        private final Class klass;

        public JavaClassKindOf(Class klass) {
            this.klass = klass;
        }

        @Override
        public boolean isKindOf(IRubyObject obj, RubyModule type) {
            return klass.isInstance(obj);
        }
    }

    public boolean isInstance(IRubyObject object) {
        return kindOf.isKindOf(object, this);
    }

    public Map<String, ConstantEntry> getConstantMap() {
        return constants;
    }

    public synchronized Map<String, ConstantEntry> getConstantMapForWrite() {
        return constants == Collections.EMPTY_MAP ? constants = new ConcurrentHashMap<String, ConstantEntry>(4, 0.9f, 1) : constants;
    }

    /**
     * AutoloadMap must be accessed after checking ConstantMap. Checking UNDEF value in constantMap works as a guard.
     * For looking up constant, check constantMap first then try to get an Autoload object from autoloadMap.
     * For setting constant, update constantMap first and remove an Autoload object from autoloadMap.
     */
    private Map<String, Autoload> getAutoloadMap() {
        return autoloads;
    }

    private synchronized Map<String, Autoload> getAutoloadMapForWrite() {
        return autoloads == Collections.EMPTY_MAP ? autoloads = new ConcurrentHashMap<String, Autoload>(4, 0.9f, 1) : autoloads;
    }

    public void addIncludingHierarchy(IncludedModule hierarchy) {
        synchronized (getRuntime().getHierarchyLock()) {
            Set<RubyClass> oldIncludingHierarchies = includingHierarchies;
            if (oldIncludingHierarchies == Collections.EMPTY_SET) includingHierarchies = oldIncludingHierarchies = new WeakHashSet(4);
            oldIncludingHierarchies.add(hierarchy);
        }
    }

    /** separate path for MetaClass construction
     *
     */
    protected RubyModule(Ruby runtime, RubyClass metaClass, boolean objectSpace) {
        super(runtime, metaClass, objectSpace);
        id = runtime.allocModuleId();
        runtime.addModule(this);
        // if (parent == null) parent = runtime.getObject();
        setFlag(USER7_F, !isClass());
        generationObject = generation = runtime.getNextModuleGeneration();

        if (runtime.getInstanceConfig().isProfiling()) {
            cacheEntryFactory = new ProfilingCacheEntryFactory(runtime, NormalCacheEntryFactory);
        } else {
            cacheEntryFactory = NormalCacheEntryFactory;
        }

        // set up an invalidator for use in new optimization strategies
        methodInvalidator = OptoFactory.newMethodInvalidator(this);
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
        if (!classProviders.contains(provider)) {
            Set<ClassProvider> cp = new HashSet<ClassProvider>(classProviders);
            cp.add(provider);
            classProviders = cp;
        }
    }

    public synchronized void removeClassProvider(ClassProvider provider) {
        Set<ClassProvider> cp = new HashSet<ClassProvider>(classProviders);
        cp.remove(provider);
        classProviders = cp;
    }

    private void checkForCyclicInclude(RubyModule m) throws RaiseException {
        if (getNonIncludedClass() == m.getNonIncludedClass()) {
            throw getRuntime().newArgumentError("cyclic include detected");
        }
    }

    protected void checkForCyclicPrepend(RubyModule m) throws RaiseException {
        if (getNonIncludedClass() == m.getNonIncludedClass()) {
            throw getRuntime().newArgumentError(getName() + " cyclic prepend detected " + m.getName());
        }
    }

    private RubyClass searchProvidersForClass(String name, RubyClass superClazz) {
        RubyClass clazz;
        for (ClassProvider classProvider: classProviders) {
            if ((clazz = classProvider.defineClassUnder(this, name, superClazz)) != null) {
                return clazz;
            }
        }
        return null;
    }

    private RubyModule searchProvidersForModule(String name) {
        RubyModule module;
        for (ClassProvider classProvider: classProviders) {
            if ((module = classProvider.defineModuleUnder(this, name)) != null) {
                return module;
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

    public void setSuperClass(RubyClass superClass) {
        // update superclass reference
        this.superClass = superClass;
        if (superClass != null && superClass.isSynchronized()) becomeSynchronized();
    }

    public RubyModule getParent() {
        return parent;
    }

    public void setParent(RubyModule parent) {
        this.parent = parent;
    }

    public RubyModule getMethodLocation() {
        return methodLocation;
    }

    public void setMethodLocation(RubyModule module){
        methodLocation = module;
    }

    public Map<String, DynamicMethod> getMethods() {
        return this.methods;
    }

    public synchronized Map<String, DynamicMethod> getMethodsForWrite() {
        Map<String, DynamicMethod> myMethods = this.methods;
        return myMethods == Collections.EMPTY_MAP ?
            this.methods = new ConcurrentHashMap<String, DynamicMethod>(0, 0.9f, 1) :
            myMethods;
    }

    // note that addMethod now does its own put, so any change made to
    // functionality here should be made there as well
    private void putMethod(String name, DynamicMethod method) {
        if (hasPrepends()) method = new WrapperMethod(methodLocation, method, method.getVisibility());
        methodLocation.getMethodsForWrite().put(name, method);

        getRuntime().addProfiledMethod(name, method);
    }

    /**
     * Is this module one that in an included one (e.g. an IncludedModuleWrapper).
     */
    public boolean isIncluded() {
        return false;
    }

    public boolean isPrepended() {
        return false;
    }

    public RubyModule getNonIncludedClass() {
        return this;
    }

    public RubyModule getDelegate() {
        return this;
    }

    public RubyModule getNonPrependedClass() {
        return this;
    }

    /**
     * Get the base name of this class, or null if it is an anonymous class.
     *
     * @return base name of the class
     */
    public String getBaseName() {
        return baseName;
    }

    /**
     * Set the base name of the class. If null, the class effectively becomes
     * anonymous (though constants elsewhere may reference it).
     * @param name the new base name of the class
     */
    public void setBaseName(String name) {
        baseName = name;
        cachedName = null;
    }

    /**
     * Generate a fully-qualified class name or a #-style name for anonymous and singleton classes.
     *
     * Ruby C equivalent = "classname"
     *
     * @return The generated class name
     */
    public String getName() {
        if (cachedName != null) return cachedName;
        return calculateName();
    }

    /**
     * Get the "simple" name for the class, which is either the "base" name or
     * the "anonymous" class name.
     *
     * @return the "simple" name of the class
     */
    public String getSimpleName() {
        if (baseName != null) return baseName;
        return calculateAnonymousName();
    }

    /**
     * Recalculate the fully-qualified name of this class/module.
     */
    private String calculateName() {
        boolean cache = true;

        if (getBaseName() == null) {
            // we are anonymous, use anonymous name
            return calculateAnonymousName();
        }

        Ruby runtime = getRuntime();

        String name = getBaseName();
        RubyClass objectClass = runtime.getObject();

        // First, we count the parents
        int parentCount = 0;
        for (RubyModule p = getParent() ; p != null && p != objectClass ; p = p.getParent()) {
            parentCount++;
        }

        // Allocate a String array for all of their names and populate it
        String[] parentNames = new String[parentCount];
        int i = parentCount - 1;
        int totalLength = name.length() + parentCount * 2; // name length + enough :: for all parents
        for (RubyModule p = getParent() ; p != null && p != objectClass ; p = p.getParent(), i--) {
            String pName = p.getBaseName();

            // This is needed when the enclosing class or module is a singleton.
            // In that case, we generated a name such as null::Foo, which broke
            // Marshalling, among others. The correct thing to do in this situation
            // is to insert the generate the name of form #<Class:01xasdfasd> if
            // it's a singleton module/class, which this code accomplishes.
            if(pName == null) {
                cache = false;
                pName = p.getName();
             }

            parentNames[i] = pName;
            totalLength += pName.length();
        }

        // Then build from the front using a StringBuilder
        StringBuilder builder = new StringBuilder(totalLength);
        for (String parentName : parentNames) {
            builder.append(parentName).append("::");
        }
        builder.append(name);

        String fullName = builder.toString();

        if (cache) cachedName = fullName;

        return fullName;
    }

    private String calculateAnonymousName() {
        if (anonymousName == null) {
            // anonymous classes get the #<Class:0xdeadbeef> format
            StringBuilder anonBase = new StringBuilder("#<" + metaClass.getRealClass().getName() + ":0x");
            anonBase.append(Integer.toHexString(System.identityHashCode(this))).append('>');
            anonymousName = anonBase.toString();
        }
        return anonymousName;
    }


    @JRubyMethod(name = "refine", required = 1)
    public IRubyObject refine(ThreadContext context, IRubyObject classArg, Block block) {
        if (!block.isGiven()) throw context.runtime.newArgumentError("no block given");
        if (block.isEscaped()) throw context.runtime.newArgumentError("can't pass a Proc as a block to Module#refine");
        if (!(classArg instanceof RubyClass)) throw context.runtime.newTypeError(classArg, context.runtime.getClassClass());
        if (refinements == Collections.EMPTY_MAP) refinements = new HashMap<>();
        if (activatedRefinements == Collections.EMPTY_MAP) activatedRefinements = new HashMap<>();

        RubyClass classWeAreRefining = (RubyClass) classArg;
        RubyModule refinement = refinements.get(classWeAreRefining);
        if (refinement == null) {
            refinement = createNewRefinedModule(context, classWeAreRefining);

            // Add it to the activated chain of other refinements already added to this class we are refining
            addActivatedRefinement(context, classWeAreRefining, refinement);
        }

        // Executes the block supplied with the defined method definitions using the refinment as it's module.
        yieldRefineBlock(context, refinement, block);

        return refinement;
    }

    private RubyModule createNewRefinedModule(ThreadContext context, RubyClass classWeAreRefining) {
        RubyModule newRefinement = new RubyModule(context.runtime, classWeAreRefining);
        newRefinement.setFlag(REFINED_MODULE_F, true);
        newRefinement.setFlag(RubyObject.USER7_F, false); // Refinement modules should not do implementer check
        newRefinement.refinedClass = classWeAreRefining;
        newRefinement.definedAt = this;
        refinements.put(classWeAreRefining, newRefinement);

        return newRefinement;
    }

    private void yieldRefineBlock(ThreadContext context, RubyModule refinement, Block block) {
        block.setEvalType(EvalType.MODULE_EVAL);
        block.getBinding().setSelf(refinement);
        block.yieldSpecific(context);
    }

    // This has three cases:
    // 1. class being refined has never had any refines happen to it yet: return itself
    // 2. class has been refined: return already existing refinementwrapper (chain of modules to call against)
    // 3. refinement is already in the refinementwrapper so we do not need to add it to the wrapper again: return null
    private RubyClass getAlreadyActivatedRefinementWrapper(RubyClass classWeAreRefining, RubyModule refinement) {
        // We have already encountered at least one refine on this class.  Return that wrapper.
        RubyClass moduleWrapperForRefinment = activatedRefinements.get(classWeAreRefining);
        if (moduleWrapperForRefinment == null) return classWeAreRefining;

        for (RubyModule c = moduleWrapperForRefinment; c != null && c.isIncluded(); c = c.getSuperClass()) {
            if (c.getNonIncludedClass() == refinement) return null;
        }

        return moduleWrapperForRefinment;
    }

    /*
     * We will find whether we have already refined once and get that set of includedmodules or we will start to create
     * one.  The new refinement will be added as a new included module on the front.  It will also add all superclasses
     * of the refinement into this call chain.
     */
    // MRI: add_activated_refinement
    private void addActivatedRefinement(ThreadContext context, RubyClass classWeAreRefining, RubyModule refinement) {
        RubyClass superClass = getAlreadyActivatedRefinementWrapper(classWeAreRefining, refinement);
        if (superClass == null) return; // already been refined and added to refinementwrapper

        refinement.setFlag(IS_OVERLAID_F, true);
        IncludedModuleWrapper iclass = new IncludedModuleWrapper(context.runtime, superClass, refinement);
        RubyClass c = iclass;
        c.refinedClass = classWeAreRefining;
        for (refinement = refinement.getSuperClass(); refinement != null; refinement = refinement.getSuperClass()) {
            refinement.setFlag(IS_OVERLAID_F, true);
            RubyClass superClazz = c.getSuperClass();
            c.setModuleSuperClass(new IncludedModuleWrapper(context.runtime, c.getSuperClass(), refinement));
            c.refinedClass = classWeAreRefining;
            c = superClazz;
        }
        activatedRefinements.put(classWeAreRefining, iclass);
    }

    @JRubyMethod(name = "using", required = 1, frame = true)
    public IRubyObject using(ThreadContext context, IRubyObject refinedModule) {
        if (context.getFrameSelf() != this) throw context.runtime.newRuntimeError("Module#using is not called on self");
        // FIXME: This is a lame test and I am unsure it works with JIT'd bodies...
        if (context.getCurrentScope().getStaticScope().getIRScope() instanceof IRMethod) {
            throw context.runtime.newRuntimeError("Module#using is not permitted in methods");
        }

        // I pass the cref even though I don't need to so that the concept is simpler to read
        usingModule(context, this, refinedModule);

        return this;
    }

    // mri: rb_using_module
    public static void usingModule(ThreadContext context, RubyModule cref, IRubyObject refinedModule) {
        if (!(refinedModule instanceof RubyModule))throw context.runtime.newTypeError(refinedModule, context.runtime.getModule());

        usingModuleRecursive(cref, (RubyModule) refinedModule);
    }

    // mri: using_module_recursive
    private static void usingModuleRecursive(RubyModule cref, RubyModule refinedModule) {
        RubyClass superClass = refinedModule.getSuperClass();

        // For each superClass of the refined module also use their refinements for the given cref
        if (superClass != null) usingModuleRecursive(cref, superClass);

        //RubyModule realRefinedModule = refinedModule instanceof IncludedModule ?
        //                ((IncludedModule) refinedModule).getRealClass() : refinedModule;

        Map<RubyClass, RubyModule> refinements = refinedModule.refinements;
        if (refinements == null) return; // No refinements registered for this module

        for (Map.Entry<RubyClass, RubyModule> entry: refinements.entrySet()) {
            usingRefinement(cref, entry.getKey(), entry.getValue());
        }
    }

    // This is nearly identical to getAlreadyActivatedRefinementWrapper but thw maps they work against are different.
    // This has three cases:
    // 1. class being refined has never had any refines happen to it yet: return itself
    // 2. class has been refined: return already existing refinementwrapper (chain of modules to call against)
    // 3. refinement is already in the refinementwrapper so we do not need to add it to the wrapper again: return null
    private static RubyModule getAlreadyRefinementWrapper(RubyModule cref, RubyClass classWeAreRefining, RubyModule refinement) {
        // We have already encountered at least one refine on this class.  Return that wrapper.
        RubyModule moduleWrapperForRefinment = cref.refinements.get(classWeAreRefining);
        if (moduleWrapperForRefinment == null) return classWeAreRefining;

        for (RubyModule c = moduleWrapperForRefinment; c != null && c.isIncluded(); c = c.getSuperClass()) {
            if (c.getNonIncludedClass() == refinement) return null;
        }

        return moduleWrapperForRefinment;
    }

    /*
     * Within the context of this cref any references to the class we are refining will try and find
     * that definition from the refinement instead.  At one point I was confused how this would not
     * conflict if the same module was used in two places but the cref must be a lexically containing
     * module so it cannot live in two files.
     */
    private static void usingRefinement(RubyModule cref, RubyClass classWeAreRefining, RubyModule refinement) {
        // Our storage cubby in cref for all known refinements
        if (cref.refinements == Collections.EMPTY_MAP) cref.refinements = new HashMap<>();

        RubyModule superClass = getAlreadyRefinementWrapper(cref, classWeAreRefining, refinement);
        if (superClass == null) return; // already been refined and added to refinementwrapper

        refinement.setFlag(IS_OVERLAID_F, true);
        RubyModule lookup = new IncludedModuleWrapper(cref.getRuntime(), (RubyClass) superClass, refinement);
        RubyModule iclass = lookup;
        lookup.refinedClass = classWeAreRefining;

        for (refinement = refinement.getSuperClass(); refinement != null && refinement != classWeAreRefining; refinement = refinement.getSuperClass()) {
            refinement.setFlag(IS_OVERLAID_F, true);
            RubyClass newInclude = new IncludedModuleWrapper(cref.getRuntime(), lookup.getSuperClass(), refinement);
            lookup.setSuperClass(newInclude);
            lookup = newInclude;
            lookup.refinedClass = classWeAreRefining;
        }
        cref.refinements.put(classWeAreRefining, iclass);
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

    @Deprecated
    public RubyClass fastGetClass(String internedName) {
        return getClass(internedName);
    }

    /**
     * Prepend a new module to this module or class.
     *
     * @param arg The module to include
     */
    public synchronized void prependModule(IRubyObject arg) {
        assert arg != null;

        testFrozen("module");

        if (!(arg instanceof RubyModule)) {
            throw getRuntime().newTypeError("Wrong argument type " + arg.getMetaClass().getName() +
                    " (expected Module).");
        }

        RubyModule module = (RubyModule) arg;

        // Make sure the module we include does not already exist
        checkForCyclicInclude(module);

        if (hasModuleInHierarchy((RubyModule)arg)) {
            invalidateCacheDescendants();
            return;
        }

        infectBy(module);

        doPrependModule(module);

        invalidateCoreClasses();
        invalidateCacheDescendants();
        invalidateConstantCacheForModuleInclusion(module);
    }

    /**
     * Include a new module in this module or class.
     *
     * @param arg The module to include
     */
    public synchronized void includeModule(IRubyObject arg) {
        assert arg != null;

        testFrozen("module");

        if (!(arg instanceof RubyModule)) {
            throw getRuntime().newTypeError("Wrong argument type " + arg.getMetaClass().getName() +
                    " (expected Module).");
        }

        RubyModule module = (RubyModule) arg;

        // Make sure the module we include does not already exist
        checkForCyclicInclude(module);

        if (hasModuleInPrepends(((RubyModule)arg).getNonIncludedClass())) {
            invalidateCacheDescendants();
            return;
        }

        infectBy(module);

        doIncludeModule(module);
        invalidateCoreClasses();
        invalidateCacheDescendants();
        invalidateConstantCacheForModuleInclusion(module);
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
            this.setConstant(name, realVal);
        }

        return true;
    }

    @Extension
    public void defineAnnotatedMethods(Class clazz) {
        defineAnnotatedMethodsIndividually(clazz);
    }

    public static class MethodClumper {
        Map<String, List<JavaMethodDescriptor>> annotatedMethods = new HashMap<String, List<JavaMethodDescriptor>>();
        Map<String, List<JavaMethodDescriptor>> staticAnnotatedMethods = new HashMap<String, List<JavaMethodDescriptor>>();
        Map<String, List<JavaMethodDescriptor>> allAnnotatedMethods = new HashMap<String, List<JavaMethodDescriptor>>();

        public void clump(Class cls) {
            Method[] declaredMethods = cls.getDeclaredMethods();
            for (Method method: declaredMethods) {
                JRubyMethod anno = method.getAnnotation(JRubyMethod.class);

                if (anno == null) continue;

                // skip bridge methods, as generated by JDK8 javac for e.g. return-value overloaded methods
                if (method.isBridge()) continue;

                JavaMethodDescriptor desc = new JavaMethodDescriptor(method);

                String name = anno.name().length == 0 ? method.getName() : anno.name()[0];

                List<JavaMethodDescriptor> methodDescs;
                Map<String, List<JavaMethodDescriptor>> methodsHash;
                if (desc.isStatic) {
                    methodsHash = staticAnnotatedMethods;
                } else {
                    methodsHash = annotatedMethods;
                }

                // add to specific
                methodDescs = methodsHash.get(name);
                if (methodDescs == null) {
                    methodDescs = new ArrayList<JavaMethodDescriptor>();
                    methodsHash.put(name, methodDescs);
                } else {
                    CompatVersion oldCompat = methodDescs.get(0).anno.compat();
                    CompatVersion newCompat = desc.anno.compat();

                    int comparison = newCompat.compareTo(oldCompat);
                    if (comparison == 1) {
                        // new method's compat is higher than old method's, so we throw old one away
                        methodDescs = new ArrayList<JavaMethodDescriptor>();
                        methodsHash.put(name, methodDescs);
                    } else if (comparison == 0) {
                        // same compat version, proceed to adding additional method
                    } else {
                        // lower compat, skip this method
                        continue;
                    }
                }

                methodDescs.add(desc);

                // add to general
                methodDescs = allAnnotatedMethods.get(name);
                if (methodDescs == null) {
                    methodDescs = new ArrayList<JavaMethodDescriptor>();
                    allAnnotatedMethods.put(name, methodDescs);
                }

                methodDescs.add(desc);
            }
        }

        public Map<String, List<JavaMethodDescriptor>> getAllAnnotatedMethods() {
            return allAnnotatedMethods;
        }

        public Map<String, List<JavaMethodDescriptor>> getAnnotatedMethods() {
            return annotatedMethods;
        }

        public Map<String, List<JavaMethodDescriptor>> getStaticAnnotatedMethods() {
            return staticAnnotatedMethods;
        }
    }

    public void defineAnnotatedMethodsIndividually(Class clazz) {
        TypePopulator populator;

        if (RubyInstanceConfig.FULL_TRACE_ENABLED || RubyInstanceConfig.REFLECTED_HANDLES) {
            // we want reflected invokers or need full traces, use default (slow) populator
            if (DEBUG) LOG.info("trace mode, using default populator");
            populator = TypePopulator.DEFAULT;
        } else {
            try {
                String qualifiedName = "org.jruby.gen." + clazz.getCanonicalName().replace('.', '$');

                if (DEBUG) LOG.info("looking for " + qualifiedName + AnnotationBinder.POPULATOR_SUFFIX);

                Class populatorClass = Class.forName(qualifiedName + AnnotationBinder.POPULATOR_SUFFIX);
                populator = (TypePopulator)populatorClass.newInstance();
            } catch (Throwable t) {
                if (DEBUG) LOG.info("Could not find it, using default populator");
                populator = TypePopulator.DEFAULT;
            }
        }

        populator.populate(this, clazz);
    }

    public boolean defineAnnotatedMethod(String name, List<JavaMethodDescriptor> methods, MethodFactory methodFactory) {
        JavaMethodDescriptor desc = methods.get(0);
        if (methods.size() == 1) {
            return defineAnnotatedMethod(name, desc, methodFactory);
        } else {
            DynamicMethod dynamicMethod = methodFactory.getAnnotatedMethod(this, methods);
            define(this, desc, name, dynamicMethod);

            return true;
        }
    }

    public boolean defineAnnotatedMethod(Method method, MethodFactory methodFactory) {
        JRubyMethod jrubyMethod = method.getAnnotation(JRubyMethod.class);

        if (jrubyMethod == null) return false;

        JavaMethodDescriptor desc = new JavaMethodDescriptor(method);
        DynamicMethod dynamicMethod = methodFactory.getAnnotatedMethod(this, desc);
        define(this, desc, method.getName(), dynamicMethod);

        return true;
    }

    public boolean defineAnnotatedMethod(String name, JavaMethodDescriptor desc, MethodFactory methodFactory) {
        JRubyMethod jrubyMethod = desc.anno;

        if (jrubyMethod == null) return false;

        DynamicMethod dynamicMethod = methodFactory.getAnnotatedMethod(this, desc);
        define(this, desc, name, dynamicMethod);

        return true;
    }

    public void undefineMethod(String name) {
        methodLocation.addMethod(name, UndefinedMethod.getInstance());
    }

    /** rb_undef
     *
     */
    public void undef(ThreadContext context, String name) {
        Ruby runtime = context.runtime;

        testFrozen("module");
        if (name.equals("__id__") || name.equals("__send__") || name.equals("object_id")) {
            runtime.getWarnings().warn(ID.UNDEFINING_BAD, "undefining `"+ name +"' may cause serious problem");
        }

        if (name.equals("method_missing")) {
            IRubyObject oldExc = runtime.getGlobalVariables().get("$!"); // Save $!
            try {
                removeMethod(context, name);
            } catch (RaiseException t) {
                if (!(t.getException() instanceof RubyNameError)) {
                    throw t;
                } else {
                    runtime.getGlobalVariables().set("$!", oldExc); // Restore $!
                }
            }
            return;
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
        methodLocation.addMethod(name, UndefinedMethod.getInstance());

        if (isSingleton()) {
            IRubyObject singleton = ((MetaClass)this).getAttached();
            singleton.callMethod(context, "singleton_method_undefined", runtime.newSymbol(name));
        } else {
            callMethod(context, "method_undefined", runtime.newSymbol(name));
        }
    }

    @JRubyMethod(name = "include?", required = 1)
    public IRubyObject include_p(ThreadContext context, IRubyObject arg) {
        if (!arg.isModule()) {
            throw context.runtime.newTypeError(arg, context.runtime.getModule());
        }
        RubyModule moduleToCompare = (RubyModule) arg;

        // See if module is in chain...Cannot match against itself so start at superClass.
        for (RubyModule p = getSuperClass(); p != null; p = p.getSuperClass()) {
            if (p.isSame(moduleToCompare)) {
                return context.runtime.getTrue();
            }
        }

        return context.runtime.getFalse();
    }

    @JRubyMethod(name = "singleton_class?")
    public IRubyObject singleton_class_p(ThreadContext context) {
        return context.runtime.newBoolean(isSingleton());
    }

    // TODO: Consider a better way of synchronizing
    public void addMethod(String name, DynamicMethod method) {
        testFrozen("class/module");

        if (this instanceof MetaClass) {
            // FIXME: Gross and not quite right. See MRI's rb_frozen_class_p logic
            RubyBasicObject attached = (RubyBasicObject)((MetaClass)this).getAttached();
            attached.testFrozen();
        }

        addMethodInternal(name, method);
    }

    public void addMethodInternal(String name, DynamicMethod method) {
        synchronized(methodLocation.getMethodsForWrite()) {
            addMethodAtBootTimeOnly(name, method);
            invalidateCoreClasses();
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
        if (hasPrepends()) method = new WrapperMethod(methodLocation, method, method.getVisibility());

        methodLocation.getMethodsForWrite().put(name, method);

        getRuntime().addProfiledMethod(name, method);
    }

    public void removeMethod(ThreadContext context, String name) {
        Ruby runtime = context.runtime;

        testFrozen("class/module");

        if(name.equals("object_id") || name.equals("__send__") || name.equals("initialize")) {
            runtime.getWarnings().warn(ID.UNDEFINING_BAD, "removing `" + name + "' may cause serious problems");
        }

        // We can safely reference methods here instead of doing getMethods() since if we
        // are adding we are not using a IncludedModule.
        synchronized(methodLocation.getMethodsForWrite()) {
            DynamicMethod method = (DynamicMethod) methodLocation.getMethodsForWrite().remove(name);
            if (method == null) {
                throw runtime.newNameError("method '" + name + "' not defined in " + getName(), name);
            }

            invalidateCoreClasses();
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

    public CacheEntry searchWithCache(String name) {
        return searchWithCache(name, true);
    }

    /**
     * Search through this module and supermodules for method definitions. Cache superclass definitions in this class.
     *
     * @param name The name of the method to search for
     * @param cacheUndef Flag for caching UndefinedMethod. This should normally be true.
     * @return The method, or UndefinedMethod if not found
     */
    public CacheEntry searchWithCache(String name, boolean cacheUndef) {
        CacheEntry entry = cacheHit(name);

        if (entry != null) return entry;

        // we grab serial number first; the worst that will happen is we cache a later
        // update with an earlier serial number, which would just flush anyway
        int token = getGeneration();
        DynamicMethod method = searchMethodInner(name);

        if (method instanceof CacheableMethod) {
            method = ((CacheableMethod) method).getMethodForCaching();
        }

        return method != null ? addToCache(name, method, token) : cacheUndef ? addToCache(name, UndefinedMethod.getInstance(), token) : cacheEntryFactory.newCacheEntry(name, method, token);
    }

    @Deprecated
    public final int getCacheToken() {
        return generation;
    }

    public final int getGeneration() {
        return generation;
    }

    public final Integer getGenerationObject() {
        return generationObject;
    }

    private final Map<String, CacheEntry> getCachedMethods() {
        return this.cachedMethods;
    }

    private final Map<String, CacheEntry> getCachedMethodsForWrite() {
        Map<String, CacheEntry> myCachedMethods = this.cachedMethods;
        return myCachedMethods == Collections.EMPTY_MAP ?
            this.cachedMethods = new ConcurrentHashMap<String, CacheEntry>(0, 0.75f, 1) :
            myCachedMethods;
    }

    private CacheEntry cacheHit(String name) {
        CacheEntry cacheEntry = getCachedMethods().get(name);

        if (cacheEntry != null) {
            if (cacheEntry.token == getGeneration()) {
                return cacheEntry;
            }
        }

        return null;
    }

    private void invalidateConstantCacheForModuleInclusion(RubyModule module)
    {
        for (RubyModule mod : gatherModules(module)) {
            for (String key : mod.getConstantMap().keySet()) {
                invalidateConstantCache(key);
            }
        }
    }

    protected static abstract class CacheEntryFactory {
        public abstract CacheEntry newCacheEntry(String name,DynamicMethod method, int token);

        /**
         * Test all WrapperCacheEntryFactory instances in the chain for assignability
         * from the given class.
         *
         * @param cacheEntryFactoryClass the class from which to test assignability
         * @return whether the given class is assignable from any factory in the chain
         */
        public boolean hasCacheEntryFactory(Class cacheEntryFactoryClass) {
            CacheEntryFactory current = this;
            while (current instanceof WrapperCacheEntryFactory) {
                if (cacheEntryFactoryClass.isAssignableFrom(current.getClass())) {
                    return true;
                }
                current = ((WrapperCacheEntryFactory)current).getPrevious();
            }

            return cacheEntryFactoryClass.isAssignableFrom(current.getClass());
        }
    }

    /**
     * A wrapper CacheEntryFactory, for delegating cache entry creation along a chain.
     */
    protected static abstract class WrapperCacheEntryFactory extends CacheEntryFactory {
        /** The CacheEntryFactory being wrapped. */
        protected final CacheEntryFactory previous;

        /**
         * Construct a new WrapperCacheEntryFactory using the given CacheEntryFactory as
         * the "previous" wrapped factory.
         *
         * @param previous the wrapped factory
         */
        public WrapperCacheEntryFactory(CacheEntryFactory previous) {
            this.previous = previous;
        }

        public CacheEntryFactory getPrevious() {
            return previous;
        }
    }

    protected static final CacheEntryFactory NormalCacheEntryFactory = new CacheEntryFactory() {
        @Override
        public CacheEntry newCacheEntry(String name, DynamicMethod method, int token) {
            return new CacheEntry(method, token);
        }
    };

    protected static class SynchronizedCacheEntryFactory extends WrapperCacheEntryFactory {
        public SynchronizedCacheEntryFactory(CacheEntryFactory previous) {
            super(previous);
        }
        @Override
        public CacheEntry newCacheEntry(String name,DynamicMethod method, int token) {
            if (method.isUndefined()) {
                return new CacheEntry(method, token);
            }
            // delegate up the chain
            CacheEntry delegated = previous.newCacheEntry(name,method, token);
            return new CacheEntry(new SynchronizedDynamicMethod(delegated.method), delegated.token);
        }
    }

    protected static class ProfilingCacheEntryFactory extends WrapperCacheEntryFactory {

        private final MethodEnhancer enhancer;

        public ProfilingCacheEntryFactory( Ruby runtime, CacheEntryFactory previous) {
            super(previous);
            this.enhancer = runtime.getProfilingService().newMethodEnhancer( runtime );
        }

        private MethodEnhancer getMethodEnhancer() {
            return enhancer;
        }

        @Override
        public CacheEntry newCacheEntry(String name, DynamicMethod method, int token) {
            if (method.isUndefined()) {
                return new CacheEntry(method, token);
            }
            CacheEntry delegated = previous.newCacheEntry(name, method, token);
            DynamicMethod enhancedMethod = getMethodEnhancer().enhance( name, delegated.method );
            return new CacheEntry( enhancedMethod, delegated.token );
        }
    }

    private volatile CacheEntryFactory cacheEntryFactory;

    // modifies this class only; used to make the Synchronized module synchronized
    public void becomeSynchronized() {
        cacheEntryFactory = new SynchronizedCacheEntryFactory(cacheEntryFactory);
    }

    public boolean isSynchronized() {
        return cacheEntryFactory.hasCacheEntryFactory(SynchronizedCacheEntryFactory.class);
    }

    private CacheEntry addToCache(String name, DynamicMethod method, int token) {
        CacheEntry entry = cacheEntryFactory.newCacheEntry(name, method, token);
        methodLocation.getCachedMethodsForWrite().put(name, entry);

        return entry;
    }

    public DynamicMethod searchMethodInner(String name) {
        // This flattens some of the recursion that would be otherwise be necessary.
        // Used to recurse up the class hierarchy which got messy with prepend.
        for (RubyModule module = this; module != null; module = module.getSuperClass()) {
            // Only recurs if module is an IncludedModuleWrapper.
            // This way only the recursion needs to be handled differently on
            // IncludedModuleWrapper.
            DynamicMethod method = module.searchMethodCommon(name);
            if (method != null) return method.isNull() ? null : method;
        }
        return null;
    }

    // The local method resolution logic. Overridden in IncludedModuleWrapper for recursion.
    protected DynamicMethod searchMethodCommon(String name) {
        return getMethods().get(name);
    }

    public void invalidateCacheDescendants() {
        if (DEBUG) LOG.debug("invalidating descendants: {}", baseName);

        if (includingHierarchies.isEmpty()) {
            // it's only us; just invalidate directly
            methodInvalidator.invalidate();
            return;
        }

        List<Invalidator> invalidators = new ArrayList<Invalidator>();
        invalidators.add(methodInvalidator);

        synchronized (getRuntime().getHierarchyLock()) {
            for (RubyClass includingHierarchy : includingHierarchies) {
                includingHierarchy.addInvalidatorsAndFlush(invalidators);
            }
        }

        methodInvalidator.invalidateAll(invalidators);
    }

    protected void invalidateCoreClasses() {
        if (!getRuntime().isBootingCore()) {
            if (this == getRuntime().getFixnum()) {
                getRuntime().reopenFixnum();
            } else if (this == getRuntime().getFloat()) {
                getRuntime().reopenFloat();
            }
        }
    }

    public Invalidator getInvalidator() {
        return methodInvalidator;
    }

    public void updateGeneration() {
        generationObject = generation = getRuntime().getNextModuleGeneration();
    }

    @Deprecated
    protected void invalidateCacheDescendantsInner() {
        methodInvalidator.invalidate();
    }

    protected void invalidateConstantCache(String constantName) {
        getRuntime().getConstantInvalidator(constantName).invalidate();
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
     * Find the given class in this hierarchy, considering modules along the way.
     *
     * @param clazz the class to find
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

    /** rb_alias
     *
     */
    public synchronized void defineAlias(String name, String oldName) {
        testFrozen("module");
        if (oldName.equals(name)) return;

        DynamicMethod method = searchForAliasMethod(getRuntime(), oldName);

        putMethod(name, new AliasMethod(this, method, oldName));

        methodLocation.invalidateCoreClasses();
        methodLocation.invalidateCacheDescendants();
    }

    public synchronized void defineAliases(List<String> aliases, String oldName) {
        testFrozen("module");
        DynamicMethod method = searchForAliasMethod(getRuntime(), oldName);

        for (String name: aliases) {
            if (oldName.equals(name)) continue;

            putMethod(name, new AliasMethod(this, method, oldName));
        }

        methodLocation.invalidateCoreClasses();
        methodLocation.invalidateCacheDescendants();
    }

    private DynamicMethod searchForAliasMethod(Ruby runtime, String name) {
        DynamicMethod method = deepMethodSearch(name, runtime);

        if (method instanceof JavaMethod) {
            // JRUBY-2435: Aliasing eval and other "special" methods should display a warning
            // We warn because we treat certain method names as "special" for purposes of
            // optimization. Hopefully this will be enough to convince people not to alias
            // them.
            CallConfiguration callerReq = ((JavaMethod)method).getCallerRequirement();

            if (callerReq.framing() != Framing.None ||
                    callerReq.scoping() != Scoping.None) {String baseName = getBaseName();
                char refChar = '#';
                String simpleName = getSimpleName();

                if (baseName == null && this instanceof MetaClass) {
                    IRubyObject attached = ((MetaClass)this).getAttached();
                    if (attached instanceof RubyModule) {
                        simpleName = ((RubyModule)attached).getSimpleName();
                        refChar = '.';
                    }
                }

                runtime.getWarnings().warn(simpleName + refChar + name + " accesses caller's state and should not be aliased");
            }
        }

        return method;
    }

    /** this method should be used only by interpreter or compiler
     *
     */
    public RubyClass defineOrGetClassUnder(String name, RubyClass superClazz) {
        // This method is intended only for defining new classes in Ruby code,
        // so it uses the allocator of the specified superclass or default to
        // the Object allocator. It should NOT be used to define classes that require a native allocator.

        Ruby runtime = getRuntime();
        IRubyObject classObj = getConstantAtSpecial(name);
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
        } else if (classProviders != null && (clazz = searchProvidersForClass(name, superClazz)) != null) {
            // reopen a java class
        } else {
            if (superClazz == null) superClazz = runtime.getObject();

            ObjectAllocator allocator;
            if (superClazz == runtime.getObject()) {
                if (RubyInstanceConfig.REIFY_RUBY_CLASSES) {
                    allocator = REIFYING_OBJECT_ALLOCATOR;
                } else if (Options.REIFY_VARIABLES.load()) {
                    allocator = IVAR_INSPECTING_OBJECT_ALLOCATOR;
                } else {
                    allocator = OBJECT_ALLOCATOR;
                }
            } else {
                allocator = superClazz.getAllocator();
            }

            clazz = RubyClass.newClass(runtime, superClazz, name, allocator, this, true);
        }

        return clazz;
    }

    /** this method should be used only by interpreter or compiler
     *
     */
    public RubyModule defineOrGetModuleUnder(String name) {
        // This method is intended only for defining new modules in Ruby code
        Ruby runtime = getRuntime();
        IRubyObject moduleObj = getConstantAtSpecial(name);
        RubyModule module;
        if (moduleObj != null) {
            if (!moduleObj.isModule()) throw runtime.newTypeError(name + " is not a module");
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

    private void addAccessor(ThreadContext context, String internedName, Visibility visibility, boolean readable, boolean writeable) {
        assert internedName == internedName.intern() : internedName + " is not interned";

        final Ruby runtime = context.runtime;

        if (visibility == PRIVATE) {
            runtime.getWarnings().warn(ID.PRIVATE_ACCESSOR, "private attribute?");
        } else if (visibility == MODULE_FUNCTION) {
            runtime.getWarnings().warn(ID.ACCESSOR_MODULE_FUNCTION, "attribute accessor as module_function");
            visibility = PRIVATE;
        }

        if (!(IdUtil.isLocal(internedName) || IdUtil.isConstant(internedName))) {
            throw runtime.newNameError("invalid attribute name", internedName);
        }

        final String variableName = ("@" + internedName).intern();
        if (readable) {
            addMethod(internedName, new AttrReaderMethod(methodLocation, visibility, CallConfiguration.FrameNoneScopeNone, variableName));
            callMethod(context, "method_added", runtime.fastNewSymbol(internedName));
        }
        if (writeable) {
            internedName = (internedName + "=").intern();
            addMethod(internedName, new AttrWriterMethod(methodLocation, visibility, CallConfiguration.FrameNoneScopeNone, variableName));
            callMethod(context, "method_added", runtime.fastNewSymbol(internedName));
        }
    }

    /** set_method_visibility
     *
     */
    public void setMethodVisibility(IRubyObject[] methods, Visibility visibility) {
        for (int i = 0; i < methods.length; i++) {
            exportMethod(methods[i].asJavaString(), visibility);
        }
    }

    /** rb_export_method
     *
     */
    public void exportMethod(String name, Visibility visibility) {
        Ruby runtime = getRuntime();

        DynamicMethod method = deepMethodSearch(name, runtime);

        if (method.getVisibility() != visibility) {
            if (this == method.getImplementationClass()) {
                method.setVisibility(visibility);
            } else {
                // FIXME: Why was this using a FullFunctionCallbackMethod before that did callSuper?
                methodLocation.addMethod(name, new WrapperMethod(this, method, visibility));
            }

            invalidateCoreClasses();
            invalidateCacheDescendants();
        }
    }

    private DynamicMethod deepMethodSearch(String name, Ruby runtime) {
        DynamicMethod method = searchMethod(name);

        if (method.isUndefined() && isModule()) {
            method = runtime.getObject().searchMethod(name);
        }

        if (method.isUndefined()) {
            throw runtime.newNameError("undefined method '" + name + "' for " +
                                (isModule() ? "module" : "class") + " '" + getName() + "'", name);
        }
        return method;
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

    public boolean isMethodBound(String name, boolean checkVisibility, boolean checkRespondTo) {
        if (!checkRespondTo) return isMethodBound(name, checkVisibility);
        DynamicMethod method = searchMethod(name);
        if (!method.isUndefined() && !method.isNotImplemented()) {
            return !(checkVisibility && method.getVisibility() == PRIVATE);
        }
        return false;
    }

    public IRubyObject newMethod(IRubyObject receiver, String methodName, boolean bound, Visibility visibility) {
        return newMethod(receiver, methodName, bound, visibility, false, true);
    }

    public IRubyObject newMethod(IRubyObject receiver, final String methodName, boolean bound, Visibility visibility, boolean respondToMissing) {
        return newMethod(receiver, methodName, bound, visibility, respondToMissing, true);
    }

    public static class RespondToMissingMethod extends JavaMethod.JavaMethodNBlock {
        final CallSite site;
        public RespondToMissingMethod(RubyModule implClass, Visibility vis, String methodName) {
            super(implClass, vis);

            site = new FunctionalCachingCallSite(methodName);
        }
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            return site.call(context, self, self, args, block);
        }

        public boolean equals(Object other) {
            if (!(other instanceof RespondToMissingMethod)) return false;

            RespondToMissingMethod rtmm = (RespondToMissingMethod)other;

            return this.site.methodName.equals(rtmm.site.methodName) &&
                    isImplementedBy(rtmm.getImplementationClass());
        }
    }

    public IRubyObject newMethod(IRubyObject receiver, final String methodName, boolean bound, Visibility visibility, boolean respondToMissing, boolean priv) {
        DynamicMethod method = searchMethod(methodName);

        if (method.isUndefined() ||
            (visibility != null && method.getVisibility() != visibility)) {
            if (respondToMissing) { // 1.9 behavior
                if (receiver.respondsToMissing(methodName, priv)) {
                    method = new RespondToMissingMethod(this, PUBLIC, methodName);
                } else {
                    throw getRuntime().newNameError("undefined method `" + methodName +
                        "' for class `" + this.getName() + "'", methodName);
                }
            } else {
                throw getRuntime().newNameError("undefined method `" + methodName +
                    "' for class `" + this.getName() + "'", methodName);
            }
        }

        RubyModule implementationModule = method.getImplementationClass();
        RubyModule originModule = this;
        while (originModule != implementationModule && originModule.isSingleton()) {
            originModule = ((MetaClass)originModule).getRealClass();
        }

        AbstractRubyMethod newMethod;
        if (bound) {
            newMethod = RubyMethod.newMethod(implementationModule, methodName, originModule, methodName, method, receiver);
        } else {
            newMethod = RubyUnboundMethod.newUnboundMethod(implementationModule, methodName, originModule, methodName, method);
        }
        newMethod.infectBy(this);

        return newMethod;
    }

    @JRubyMethod(name = "define_method", visibility = PRIVATE, reads = VISIBILITY)
    public IRubyObject define_method(ThreadContext context, IRubyObject arg0, Block block) {
        Ruby runtime = context.runtime;
        String name = TypeConverter.convertToIdentifier(arg0);
        DynamicMethod newMethod = null;
        Visibility visibility = PUBLIC;

        // We need our identifier to be retrievable and creatable as a symbol.  This side-effect
        // populates this name into our symbol table so it will exist later if needed.  The
        // reason for this hack/side-effect is that symbols store their values as raw bytes.  We lose encoding
        // info so we need to make an entry so any accesses with raw bytes later gets proper symbol.
        RubySymbol nameSym = RubySymbol.newSymbol(runtime, arg0);

        if (!block.isGiven()) {
            throw getRuntime().newArgumentError("tried to create Proc object without a block");
        }

        block = block.cloneBlockAndFrame();
        RubyProc proc = runtime.newProc(Block.Type.LAMBDA, block);

        // a normal block passed to define_method changes to do arity checking; make it a lambda
        proc.getBlock().type = Block.Type.LAMBDA;

        newMethod = createProcMethod(name, visibility, proc);

        Helpers.addInstanceMethod(this, name, newMethod, visibility, context, runtime);

        return nameSym;
    }

    @JRubyMethod(name = "define_method", visibility = PRIVATE, reads = VISIBILITY)
    public IRubyObject define_method(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        Ruby runtime = context.runtime;
        IRubyObject body;
        String name = TypeConverter.convertToIdentifier(arg0);
        DynamicMethod newMethod = null;
        Visibility visibility = PUBLIC;

        // We need our identifier to be retrievable and creatable as a symbol.  This side-effect
        // populates this name into our symbol table so it will exist later if needed.  The
        // reason for this hack/side-effect is that symbols store their values as raw bytes.  We lose encoding
        // info so we need to make an entry so any accesses with raw bytes later gets proper symbol.
        RubySymbol nameSym = RubySymbol.newSymbol(runtime, arg0);

        if (runtime.getProc().isInstance(arg1)) {
            // double-testing args.length here, but it avoids duplicating the proc-setup code in two places
            RubyProc proc = (RubyProc)arg1;
            body = proc;

            newMethod = createProcMethod(name, visibility, proc);
        } else if (arg1 instanceof AbstractRubyMethod) {
            AbstractRubyMethod method = (AbstractRubyMethod)arg1;
            body = method;

            checkValidBindTargetFrom(context, (RubyModule)method.owner(context));

            newMethod = method.getMethod().dup();
            newMethod.setImplementationClass(this);
        } else {
            throw runtime.newTypeError("wrong argument type " + arg1.getType().getName() + " (expected Proc/Method)");
        }

        Helpers.addInstanceMethod(this, name, newMethod, visibility, context, runtime);

        return nameSym;
    }
    @Deprecated
    public IRubyObject define_method(ThreadContext context, IRubyObject[] args, Block block) {
        switch (args.length) {
        case 1:
            return define_method(context, args[0], block);
        case 2:
            return define_method(context, args[0], args[1], block);
        default:
            throw context.runtime.newArgumentError("wrong number of arguments (" + args.length + " for 2)");
        }
    }

    private DynamicMethod createProcMethod(String name, Visibility visibility, RubyProc proc) {
        Block block = proc.getBlock();
        block.getBinding().getFrame().setKlazz(this);
        block.getBinding().getFrame().setName(name);
        block.getBinding().setMethod(name);

        block.type = Block.Type.LAMBDA;
        StaticScope scope = block.getBody().getStaticScope();

        // for zsupers in define_method (blech!) we tell the proc scope to act as the "argument" scope
        scope.makeArgumentScope();

        Arity arity = block.arity();
        // just using required is broken...but no more broken than before zsuper refactoring
        scope.setRequiredArgs(arity.required());

        if(!arity.isFixed()) {
            scope.setHasRest(arity.required() >= 0);
        }

        return new ProcMethod(this, proc, visibility);
    }

    public IRubyObject name() {
        return name19();
    }

    @JRubyMethod(name = "name")
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
            if (method.isImplementedBy(realType) || method.isUndefined()) {

                // A cloned method now belongs to a new class.  Set it.
                // TODO: Make DynamicMethod immutable
                DynamicMethod clonedMethod = method.dup();
                clonedMethod.setImplementationClass(clone);
                clone.putMethod(entry.getKey(), clonedMethod);
            }
        }

        return clone;
    }

    /** mri: rb_mod_init_copy
     *
     */
    @JRubyMethod(name = "initialize_copy", required = 1, visibility = Visibility.PRIVATE)
    @Override
    public IRubyObject initialize_copy(IRubyObject original) {
        if (this instanceof RubyClass) {
            checkSafeTypeToCopy((RubyClass) original);
        }
        super.initialize_copy(original);

        RubyModule originalModule = (RubyModule)original;

        if (!getMetaClass().isSingleton()) setMetaClass(originalModule.getSingletonClassClone());
        setSuperClass(originalModule.getSuperClass());
        if (originalModule.hasVariables()) syncVariables(originalModule);
        syncConstants(originalModule);

        originalModule.cloneMethods(this);

        return this;
    }

    // mri: class_init_copy_check
    private void checkSafeTypeToCopy(RubyClass original) {
        Ruby runtime = getRuntime();

        if (original == runtime.getBasicObject()) throw runtime.newTypeError("can't copy the root class");
        if (getSuperClass() == runtime.getBasicObject()) throw runtime.newTypeError("already initialized class");
        if (original.isSingleton()) throw runtime.newTypeError("can't copy singleton class");
    }

    public void syncConstants(RubyModule other) {
        if (other.getConstantMap() != Collections.EMPTY_MAP) {
            getConstantMapForWrite().putAll(other.getConstantMap());
        }
    }

    public void syncClassVariables(RubyModule other) {
        if (other.getClassVariablesForRead() != Collections.EMPTY_MAP) {
            getClassVariables().putAll(other.getClassVariablesForRead());
        }
    }

    /** rb_mod_included_modules
     *
     */
    @JRubyMethod(name = "included_modules")
    public RubyArray included_modules(ThreadContext context) {
        RubyArray ary = context.runtime.newArray();

        for (RubyModule p = getSuperClass(); p != null; p = p.getSuperClass()) {
            if (p.isIncluded()) {
                ary.append(p.getNonIncludedClass());
            }
        }

        return ary;
    }

    public boolean hasPrepends() {
        return methodLocation != this;
    }

    /** rb_mod_ancestors
     *
     */
    @JRubyMethod(name = "ancestors")
    public RubyArray ancestors(ThreadContext context) {
        return context.runtime.newArray(getAncestorList());
    }

    @Deprecated
    public RubyArray ancestors() {
        return getRuntime().newArray(getAncestorList());
    }

    public List<IRubyObject> getAncestorList() {
        ArrayList<IRubyObject> list = new ArrayList<IRubyObject>();

        for (RubyModule module = this; module != null; module = module.getSuperClass()) {
            // FIXME this is silly. figure out how to delegate the getNonIncludedClass()
            // call to drop the getDelegate().
            if (module.methodLocation == module) list.add(module.getDelegate().getNonIncludedClass());
        }

        return list;
    }

    public boolean hasModuleInPrepends(RubyModule type) {
        for (RubyModule module = this; module != methodLocation; module = module.getSuperClass()) {
            if (type == module.getNonIncludedClass()) return true;
        }
        return false;
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
    @JRubyMethod(name = "to_s", alias = "inspect")
    @Override
    public IRubyObject to_s() {
        if(isSingleton()){
            IRubyObject attached = ((MetaClass)this).getAttached();
            StringBuilder buffer = new StringBuilder("#<Class:");
            if (attached != null) { // FIXME: figure out why we getService null sometimes
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
        return context.runtime.newBoolean(isInstance(obj));
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
        if(!(other instanceof RubyModule)) return context.runtime.getFalse();

        RubyModule otherModule = (RubyModule) other;
        if(otherModule.isIncluded()) {
            return context.runtime.newBoolean(otherModule.isSame(this));
        } else {
            return context.runtime.newBoolean(isSame(otherModule));
        }
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
    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, Block block) {
        if (block.isGiven()) {
            module_exec(context, new IRubyObject[] {this}, block);
        }

        return getRuntime().getNil();
    }

    public void addReadWriteAttribute(ThreadContext context, String name) {
        addAccessor(context, name.intern(), PUBLIC, true, true);
    }

    public void addReadAttribute(ThreadContext context, String name) {
        addAccessor(context, name.intern(), PUBLIC, true, false);
    }

    public void addWriteAttribute(ThreadContext context, String name) {
        addAccessor(context, name.intern(), PUBLIC, false, true);
    }

    /** rb_mod_attr
     *
     */
    public IRubyObject attr(ThreadContext context, IRubyObject[] args) {
        return attr19(context, args);
    }

    @JRubyMethod(name = "attr", rest = true, visibility = PRIVATE, reads = VISIBILITY)
    public IRubyObject attr19(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;

        if (args.length == 2 && (args[1] == runtime.getTrue() || args[1] == runtime.getFalse())) {
            runtime.getWarnings().warn(ID.OBSOLETE_ARGUMENT, "optional boolean argument is obsoleted");
            addAccessor(context, args[0].asJavaString().intern(), context.getCurrentVisibility(), args[0].isTrue(), args[1].isTrue());
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

        return context.runtime.getNil();
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

        return context.runtime.getNil();
    }


    @Deprecated
    public IRubyObject attr_accessor(IRubyObject[] args) {
        return attr_accessor(getRuntime().getCurrentContext(), args);
    }

    /** rb_mod_attr_accessor
     *  Note: this method should not be called from Java in most cases, since
     *  it depends on Ruby frame state for visibility. Use add[Read/Write]Attribute instead.
     */
    @JRubyMethod(name = "attr_accessor", rest = true, visibility = PRIVATE, reads = VISIBILITY)
    public IRubyObject attr_accessor(ThreadContext context, IRubyObject[] args) {
        // Check the visibility of the previous frame, which will be the frame in which the class is being eval'ed
        Visibility visibility = context.getCurrentVisibility();

        for (int i = 0; i < args.length; i++) {
            // This is almost always already interned, since it will be called with a symbol in most cases
            // but when created from Java code, we might getService an argument that needs to be interned.
            // addAccessor has as a precondition that the string MUST be interned
            addAccessor(context, args[i].asJavaString().intern(), visibility, true, true);
        }

        return context.runtime.getNil();
    }

    /**
     * Get a list of all instance methods names of the provided visibility unless not is true, then
     * getService all methods which are not the provided
     *
     * @param args passed into one of the Ruby instance_method methods
     * @param visibility to find matching instance methods against
     * @param not if true only find methods not matching supplied visibility
     * @return a RubyArray of instance method names
     */
    private RubyArray instance_methods(IRubyObject[] args, Visibility visibility, boolean not) {
        boolean includeSuper = args.length > 0 ? args[0].isTrue() : true;
        return instanceMethods(visibility, includeSuper, true, not);
    }

    public RubyArray instanceMethods(IRubyObject[] args, Visibility visibility, boolean obj, boolean not) {
        boolean includeSuper = args.length > 0 ? args[0].isTrue() : true;
        return instanceMethods(visibility, includeSuper, obj, not);
    }

    public RubyArray instanceMethods(Visibility visibility, boolean includeSuper, boolean obj, boolean not) {
        Ruby runtime = getRuntime();
        RubyArray ary = runtime.newArray();
        Set<String> seen = new HashSet<String>();

        populateInstanceMethodNames(seen, ary, visibility, obj, not, includeSuper);

        return ary;
    }

    public void populateInstanceMethodNames(Set<String> seen, RubyArray ary, Visibility visibility, boolean obj, boolean not, boolean recur) {
        Ruby runtime = getRuntime();
        RubyModule mod = this;
        boolean prepended = false;

        if (!recur && methodLocation != this) {
            mod = methodLocation;
            prepended = true;
        }

        for (; mod != null; mod = mod.getSuperClass()) {
            RubyModule realType = mod.getNonIncludedClass();
            for (Map.Entry entry : mod.getMethods().entrySet()) {
                String methodName = (String) entry.getKey();

                if (! seen.contains(methodName)) {
                    seen.add(methodName);

                    DynamicMethod method = (DynamicMethod) entry.getValue();
                    if ((method.isImplementedBy(realType) || method.isImplementedBy(mod)) &&
                        (!not && method.getVisibility() == visibility || (not && method.getVisibility() != visibility)) &&
                        ! method.isUndefined()) {

                        ary.append(runtime.newSymbol(methodName));
                    }
                }
            }

            if (mod.isIncluded() && !prepended) continue;
            if (obj && mod.isSingleton()) continue;
            if (!recur) break;
        }
    }

    public RubyArray instance_methods(IRubyObject[] args) {
        return instance_methods19(args);
    }

    @JRubyMethod(name = "instance_methods", optional = 1)
    public RubyArray instance_methods19(IRubyObject[] args) {
        return instanceMethods(args, PRIVATE, false, true);
    }

    public RubyArray public_instance_methods(IRubyObject[] args) {
        return public_instance_methods19(args);
    }

    @JRubyMethod(name = "public_instance_methods", optional = 1)
    public RubyArray public_instance_methods19(IRubyObject[] args) {
        return instanceMethods(args, PUBLIC, false, false);
    }

    @JRubyMethod(name = "instance_method", required = 1)
    public IRubyObject instance_method(IRubyObject symbol) {
        return newMethod(null, symbol.asJavaString(), false, null);
    }

    @JRubyMethod(name = "public_instance_method", required = 1)
    public IRubyObject public_instance_method(IRubyObject symbol) {
        return newMethod(null, symbol.asJavaString(), false, PUBLIC);
    }

    /** rb_class_protected_instance_methods
     *
     */
    public RubyArray protected_instance_methods(IRubyObject[] args) {
        return protected_instance_methods19(args);
    }

    @JRubyMethod(name = "protected_instance_methods", optional = 1)
    public RubyArray protected_instance_methods19(IRubyObject[] args) {
        return instanceMethods(args, PROTECTED, false, false);
    }

    /** rb_class_private_instance_methods
     *
     */
    public RubyArray private_instance_methods(IRubyObject[] args) {
        return private_instance_methods19(args);
    }

    @JRubyMethod(name = "private_instance_methods", optional = 1)
    public RubyArray private_instance_methods19(IRubyObject[] args) {
        return instanceMethods(args, PRIVATE, false, false);
    }

    /** rb_mod_prepend_features
     *
     */
    @JRubyMethod(name = "prepend_features", required = 1, visibility = PRIVATE)
    public RubyModule prepend_features(IRubyObject include) {
        if (!isModule()) {
            throw getRuntime().newTypeError(this, getRuntime().getModule());
        }
        if (!(include instanceof RubyModule)) {
            throw getRuntime().newTypeError(include, getRuntime().getModule());
        }

        if (!(include.isModule() || include.isClass())) {
            throw getRuntime().newTypeError(include, getRuntime().getModule());
        }

        ((RubyModule) include).prependModule(this);
        return this;
    }

    /** rb_mod_append_features
     *
     */
    @JRubyMethod(name = "append_features", required = 1, visibility = PRIVATE)
    public RubyModule append_features(IRubyObject include) {
        if (!isModule()) {
            throw getRuntime().newTypeError(this, getRuntime().getModule());
        }
        if (!(include instanceof RubyModule)) {
            throw getRuntime().newTypeError(include, getRuntime().getModule());
        }

        if (!(include.isModule() || include.isClass())) {
            throw getRuntime().newTypeError(include, getRuntime().getModule());
        }

        ((RubyModule) include).includeModule(this);
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
    @JRubyMethod(name = "include", rest = true)
    public RubyModule include(IRubyObject[] modules) {
        ThreadContext context = getRuntime().getCurrentContext();
        // MRI checks all types first:
        for (int i = modules.length; --i >= 0; ) {
            IRubyObject obj = modules[i];
            if (!obj.isModule()) {
                throw context.runtime.newTypeError(obj, context.runtime.getModule());
            }
        }
        for (int i = modules.length - 1; i >= 0; i--) {
            modules[i].callMethod(context, "append_features", this);
            modules[i].callMethod(context, "included", this);
        }

        return this;
    }

    @JRubyMethod(name = "included", required = 1, visibility = PRIVATE)
    public IRubyObject included(ThreadContext context, IRubyObject other) {
        return context.runtime.getNil();
    }

    @JRubyMethod(name = "extended", required = 1, visibility = PRIVATE)
    public IRubyObject extended(ThreadContext context, IRubyObject other, Block block) {
        return context.runtime.getNil();
    }

    @JRubyMethod(name = "mix", visibility = PRIVATE)
    public IRubyObject mix(ThreadContext context, IRubyObject mod) {
        Ruby runtime = context.runtime;

        if (!mod.isModule()) {
            throw runtime.newTypeError(mod, runtime.getModule());
        }

        for (Map.Entry<String, DynamicMethod> entry : ((RubyModule)mod).methods.entrySet()) {
            if (methodLocation.getMethods().containsKey(entry.getKey())) {
                throw runtime.newArgumentError("method would conflict - " + entry.getKey());
            }
        }

        for (Map.Entry<String, DynamicMethod> entry : ((RubyModule)mod).methods.entrySet()) {
            methodLocation.getMethodsForWrite().put(entry.getKey(), entry.getValue().dup());
        }

        return mod;
    }

    @JRubyMethod(name = "mix", visibility = PRIVATE)
    public IRubyObject mix(ThreadContext context, IRubyObject mod, IRubyObject hash0) {
        Ruby runtime = context.runtime;
        RubyHash methodNames = null;

        if (!mod.isModule()) {
            throw runtime.newTypeError(mod, runtime.getModule());
        }

        if (hash0 instanceof RubyHash) {
            methodNames = (RubyHash)hash0;
        } else {
            throw runtime.newTypeError(hash0, runtime.getHash());
        }

        for (Map.Entry<IRubyObject, IRubyObject> entry : (Set<Map.Entry<IRubyObject, IRubyObject>>)methodNames.directEntrySet()) {
            String name = entry.getValue().toString();
            if (methods.containsKey(entry.getValue().toString())) {
                throw runtime.newArgumentError("constant would conflict - " + name);
            }
        }

        for (Map.Entry<String, DynamicMethod> entry : ((RubyModule)mod).methods.entrySet()) {
            if (methods.containsKey(entry.getKey())) {
                throw runtime.newArgumentError("method would conflict - " + entry.getKey());
            }
        }

        for (Map.Entry<String, DynamicMethod> entry : ((RubyModule)mod).methods.entrySet()) {
            String name = entry.getKey();
            IRubyObject mapped = methodNames.fastARef(runtime.newSymbol(name));
            if (mapped == NEVER) {
                // unmapped
            } else if (mapped == context.nil) {
                // do not mix
                continue;
            } else {
                name = mapped.toString();
            }
            methodLocation.getMethodsForWrite().put(name, entry.getValue().dup());
        }

        return mod;
    }

    private void setVisibility(ThreadContext context, IRubyObject[] args, Visibility visibility) {
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
        Ruby runtime = context.runtime;

        if (args.length == 0) {
            context.setCurrentVisibility(MODULE_FUNCTION);
        } else {
            setMethodVisibility(args, PRIVATE);

            for (int i = 0; i < args.length; i++) {
                String name = args[i].asJavaString().intern();
                DynamicMethod method = deepMethodSearch(name, runtime);
                getSingletonClass().addMethod(name, new WrapperMethod(getSingletonClass(), method, PUBLIC));
                callMethod(context, "singleton_method_added", context.runtime.fastNewSymbol(name));
            }
        }
        return this;
    }

    @JRubyMethod(name = "method_added", required = 1, visibility = PRIVATE)
    public IRubyObject method_added(ThreadContext context, IRubyObject nothing) {
        return context.runtime.getNil();
    }

    @JRubyMethod(name = "method_removed", required = 1, visibility = PRIVATE)
    public IRubyObject method_removed(ThreadContext context, IRubyObject nothing) {
        return context.runtime.getNil();
    }

    @JRubyMethod(name = "method_undefined", required = 1, visibility = PRIVATE)
    public IRubyObject method_undefined(ThreadContext context, IRubyObject nothing) {
        return context.runtime.getNil();
    }

    @JRubyMethod(name = "method_defined?", required = 1)
    public RubyBoolean method_defined_p(ThreadContext context, IRubyObject symbol) {
        return isMethodBound(symbol.asJavaString(), true) ? context.runtime.getTrue() : context.runtime.getFalse();
    }

    @JRubyMethod(name = "public_method_defined?", required = 1)
    public IRubyObject public_method_defined(ThreadContext context, IRubyObject symbol) {
        DynamicMethod method = searchMethod(symbol.asJavaString());

        return context.runtime.newBoolean(!method.isUndefined() && method.getVisibility() == PUBLIC);
    }

    @JRubyMethod(name = "protected_method_defined?", required = 1)
    public IRubyObject protected_method_defined(ThreadContext context, IRubyObject symbol) {
        DynamicMethod method = searchMethod(symbol.asJavaString());

        return context.runtime.newBoolean(!method.isUndefined() && method.getVisibility() == PROTECTED);
    }

    @JRubyMethod(name = "private_method_defined?", required = 1)
    public IRubyObject private_method_defined(ThreadContext context, IRubyObject symbol) {
        DynamicMethod method = searchMethod(symbol.asJavaString());

        return context.runtime.newBoolean(!method.isUndefined() && method.getVisibility() == PRIVATE);
    }

    @JRubyMethod(name = "public_class_method", rest = true)
    public RubyModule public_class_method(IRubyObject[] args) {
        getSingletonClass().setMethodVisibility(args, PUBLIC);
        return this;
    }

    @JRubyMethod(name = "private_class_method", rest = true)
    public RubyModule private_class_method(IRubyObject[] args) {
        getSingletonClass().setMethodVisibility(args, PRIVATE);
        return this;
    }

    @JRubyMethod(name = "alias_method", required = 2, visibility = PRIVATE)
    public RubyModule alias_method(ThreadContext context, IRubyObject newId, IRubyObject oldId) {
        String newName = newId.asJavaString();
        defineAlias(newName, oldId.asJavaString());
        RubySymbol newSym = newId instanceof RubySymbol ? (RubySymbol)newId :
            context.runtime.newSymbol(newName);
        if (isSingleton()) {
            ((MetaClass)this).getAttached().callMethod(context, "singleton_method_added", newSym);
        } else {
            callMethod(context, "method_added", newSym);
        }
        return this;
    }

    @JRubyMethod(name = "undef_method", rest = true, visibility = PRIVATE)
    public RubyModule undef_method(ThreadContext context, IRubyObject[] args) {
        for (int i=0; i<args.length; i++) {
            undef(context, args[i].asJavaString());
        }
        return this;
    }

    @JRubyMethod(name = {"module_eval", "class_eval"},
            reads = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, JUMPTARGET, CLASS, FILENAME, SCOPE},
            writes = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, JUMPTARGET, CLASS, FILENAME, SCOPE})
    public IRubyObject module_eval(ThreadContext context, Block block) {
        return specificEval(context, this, block, EvalType.MODULE_EVAL);
    }
    @JRubyMethod(name = {"module_eval", "class_eval"},
            reads = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, JUMPTARGET, CLASS, FILENAME, SCOPE},
            writes = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, JUMPTARGET, CLASS, FILENAME, SCOPE})
    public IRubyObject module_eval(ThreadContext context, IRubyObject arg0, Block block) {
        return specificEval(context, this, arg0, block, EvalType.MODULE_EVAL);
    }
    @JRubyMethod(name = {"module_eval", "class_eval"},
            reads = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, JUMPTARGET, CLASS, FILENAME, SCOPE},
            writes = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, JUMPTARGET, CLASS, FILENAME, SCOPE})
    public IRubyObject module_eval(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        return specificEval(context, this, arg0, arg1, block, EvalType.MODULE_EVAL);
    }
    @JRubyMethod(name = {"module_eval", "class_eval"},
            reads = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, JUMPTARGET, CLASS, FILENAME, SCOPE},
            writes = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, JUMPTARGET, CLASS, FILENAME, SCOPE})
    public IRubyObject module_eval(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return specificEval(context, this, arg0, arg1, arg2, block, EvalType.MODULE_EVAL);
    }
    @Deprecated
    public IRubyObject module_eval(ThreadContext context, IRubyObject[] args, Block block) {
        return specificEval(context, this, args, block, EvalType.MODULE_EVAL);
    }

    @JRubyMethod(name = {"module_exec", "class_exec"},
            reads = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, JUMPTARGET, CLASS, FILENAME, SCOPE},
            writes = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, JUMPTARGET, CLASS, FILENAME, SCOPE})
    public IRubyObject module_exec(ThreadContext context, Block block) {
        if (block.isGiven()) {
            return yieldUnder(context, this, IRubyObject.NULL_ARRAY, block, EvalType.MODULE_EVAL);
        } else {
            throw context.runtime.newLocalJumpErrorNoBlock();
        }
    }

    @JRubyMethod(name = {"module_exec", "class_exec"}, rest = true,
            reads = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, JUMPTARGET, CLASS, FILENAME, SCOPE},
            writes = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, JUMPTARGET, CLASS, FILENAME, SCOPE})
    public IRubyObject module_exec(ThreadContext context, IRubyObject[] args, Block block) {
        if (block.isGiven()) {
            return yieldUnder(context, this, args, block, EvalType.MODULE_EVAL);
        } else {
            throw context.runtime.newLocalJumpErrorNoBlock();
        }
    }

    @JRubyMethod(name = "remove_method", rest = true, visibility = PRIVATE)
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
    @JRubyMethod(name = "nesting", reads = SCOPE, meta = true)
    public static RubyArray nesting(ThreadContext context, IRubyObject recv, Block block) {
        Ruby runtime = context.runtime;
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

        RubyModule currentInclusionPoint = methodLocation;
        ModuleLoop: for (RubyModule nextModule : modulesToInclude) {
            checkForCyclicInclude(nextModule);

            boolean superclassSeen = false;

            // nextClass.isIncluded() && nextClass.getNonIncludedClass() == nextModule.getNonIncludedClass();
            // scan class hierarchy for module
            for (RubyClass nextClass = methodLocation.getSuperClass(); nextClass != null; nextClass = nextClass.getSuperClass()) {
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

            currentInclusionPoint = proceedWithInclude(currentInclusionPoint, nextModule.getDelegate());
        }
    }

    /**
     * Prepend the given module and all related modules into the hierarchy above
     * this module/class. Inspects the hierarchy to ensure the same module isn't
     * included twice, and selects an appropriate insertion point for each incoming
     * module.
     *
     * @param baseModule The module to prepend, along with any modules it itself includes
     */
    private void doPrependModule(RubyModule baseModule) {
        List<RubyModule> modulesToInclude = gatherModules(baseModule);

        RubyClass insertBelowSuperClass = null;
        if (methodLocation == this) {
            // In the current logic, if we getService here we know that module is not an
            // IncludedModule, so there's no need to fish out the delegate. But just
            // in case the logic should change later, let's do it anyway
            RubyClass prep = new PrependedModule(getRuntime(), getSuperClass(), this);

            // if the insertion point is a class, update subclass lists
            if (this instanceof RubyClass) {
                RubyClass insertBelowClass = (RubyClass)this;

                // if there's a non-null superclass, we're including into a normal class hierarchy;
                // update subclass relationships to avoid stale parent/child relationships
                if (insertBelowClass.getSuperClass() != null) {
                    insertBelowClass.getSuperClass().replaceSubclass(insertBelowClass, prep);
                }

                prep.addSubclass(insertBelowClass);
            }
            setSuperClass(prep);
        }

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

            currentInclusionPoint = proceedWithPrepend(currentInclusionPoint, nextModule);
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
                theClass.getDelegate() == theModule.getDelegate();
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
            modulesToInclude.add(baseModule.getDelegate());
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
        // In the current logic, if we getService here we know that module is not an
        // IncludedModuleWrapper, so there's no need to fish out the delegate. But just
        // in case the logic should change later, let's do it anyway
        RubyClass wrapper = new IncludedModuleWrapper(getRuntime(), insertAbove.getSuperClass(), moduleToInclude);

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

    /**
     * Actually proceed with prepending the specified module below the given target
     * in a hierarchy. Return the new module wrapper.
     *
     * @param insertBelow The hierarchy target below which to include the wrapped module
     * @param moduleToPrepend The module to wrap and prepend
     * @return The new module wrapper resulting from this prepend
     */
    private RubyModule proceedWithPrepend(RubyModule insertBelow, RubyModule moduleToPrepend) {
        if (!moduleToPrepend.isPrepended()) moduleToPrepend = moduleToPrepend.getNonIncludedClass();

        RubyModule newInclusionPoint = proceedWithInclude(insertBelow, moduleToPrepend);

        return newInclusionPoint;
    }


    //
    ////////////////// CLASS VARIABLE RUBY METHODS ////////////////
    //

    @JRubyMethod(name = "class_variable_defined?", required = 1)
    public IRubyObject class_variable_defined_p(ThreadContext context, IRubyObject var) {
        String internedName = validateClassVariable(var.asJavaString().intern());
        RubyModule module = this;
        do {
            if (module.hasClassVariable(internedName)) {
                return context.runtime.getTrue();
            }
        } while ((module = module.getSuperClass()) != null);

        return context.runtime.getFalse();
    }

    /** rb_mod_cvar_get
     *
     */
    public IRubyObject class_variable_get(IRubyObject var) {
        return getClassVar(validateClassVariable(var.asJavaString()).intern());
    }

    @JRubyMethod(name = "class_variable_get")
    public IRubyObject class_variable_get19(IRubyObject var) {
        return class_variable_get(var);
    }

    /** rb_mod_cvar_set
     *
     */
    public IRubyObject class_variable_set(IRubyObject var, IRubyObject value) {
        return setClassVar(validateClassVariable(var.asJavaString()).intern(), value);
    }

    @JRubyMethod(name = "class_variable_set")
    public IRubyObject class_variable_set19(IRubyObject var, IRubyObject value) {
        return class_variable_set(var, value);
    }

    /** rb_mod_remove_cvar
     *
     */
    public IRubyObject remove_class_variable(ThreadContext context, IRubyObject name) {
        return removeClassVariable(name.asJavaString());
    }

    @JRubyMethod(name = "remove_class_variable")
    public IRubyObject remove_class_variable19(ThreadContext context, IRubyObject name) {
        return remove_class_variable(context, name);
    }

    /** rb_mod_class_variables
     *
     */
    public RubyArray class_variables(ThreadContext context) {
        return class_variables19(context);
    }

    @JRubyMethod(name = "class_variables")
    public RubyArray class_variables19(ThreadContext context) {
        Ruby runtime = context.runtime;
        RubyArray ary = runtime.newArray();

        Collection<String> names = classVariablesCommon();
        for (String name : names) {
            ary.add(runtime.newSymbol(name));
        }
        return ary;
    }

    private Collection<String> classVariablesCommon() {
        Set<String> names = new HashSet<String>();
        for (RubyModule p = this; p != null; p = p.getSuperClass()) {
            names.addAll(p.getClassVariableNameList());
        }
        return names;
    }


    //
    ////////////////// CONSTANT RUBY METHODS ////////////////
    //

    /** rb_mod_const_defined
     *
     */
    public RubyBoolean const_defined_p(ThreadContext context, IRubyObject symbol) {
        return const_defined_p19(context, new IRubyObject[]{symbol});
    }

    @JRubyMethod(name = "const_defined?", required = 1, optional = 1)
    public RubyBoolean const_defined_p19(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        String fullName = args[0].asJavaString();
        String symbol = fullName;
        boolean inherit = args.length == 1 || (!args[1].isNil() && args[1].isTrue());

        // symbol form does not allow ::
        if (args[0] instanceof RubySymbol && symbol.indexOf("::") != -1) {
            throw runtime.newNameError("wrong constant name", symbol);
        }

        RubyModule mod = this;

        if (symbol.startsWith("::")) mod = runtime.getObject();

        int sep;
        while((sep = symbol.indexOf("::")) != -1) {
            String segment = symbol.substring(0, sep);
            symbol = symbol.substring(sep + 2);
            IRubyObject obj = mod.getConstantNoConstMissing(validateConstant(segment, args[0]), inherit, inherit);
            if(obj instanceof RubyModule) {
                mod = (RubyModule)obj;
            } else {
                throw runtime.newTypeError(segment + " does not refer to class/module");
            }
        }

        return runtime.newBoolean(mod.getConstantNoConstMissing(validateConstant(symbol, args[0]), inherit, inherit) != null);
    }

    /** rb_mod_const_get
     *
     */
    public IRubyObject const_get(IRubyObject symbol) {
        return const_get_2_0(getRuntime().getCurrentContext(), new IRubyObject[]{symbol});
    }

    public IRubyObject const_get_1_9(ThreadContext context, IRubyObject[] args) {
        return const_get_2_0(context, args);
    }

    @JRubyMethod(name = "const_get", required = 1, optional = 1)
    public IRubyObject const_get_2_0(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        String fullName = args[0].asJavaString();
        String symbol = fullName;
        boolean inherit = args.length == 1 || (!args[1].isNil() && args[1].isTrue());

        int sep = symbol.indexOf("::");
        // symbol form does not allow ::
        if (args[0] instanceof RubySymbol && sep != -1) {
            throw context.runtime.newNameError("wrong constant name", symbol);
        }

        RubyModule mod = this;

        if (sep == 0) { // ::Foo::Bar
            mod = runtime.getObject();
            symbol = symbol.substring(2);
        }

        while ((sep = symbol.indexOf("::")) != -1) {
            String segment = symbol.substring(0, sep);
            symbol = symbol.substring(sep + 2);
            IRubyObject obj = mod.getConstant(validateConstant(segment, args[0]), inherit, inherit);
            if (obj instanceof RubyModule) {
                mod = (RubyModule) obj;
            } else {
                throw runtime.newTypeError(segment + " does not refer to class/module");
            }
        }

        return mod.getConstant(validateConstant(symbol, args[0]), inherit, inherit);
    }

    /** rb_mod_const_set
     *
     */
    @JRubyMethod(name = "const_set", required = 2)
    public IRubyObject const_set(IRubyObject symbol, IRubyObject value) {
        IRubyObject constant = setConstant(validateConstant(symbol).intern(), value);

        if (constant instanceof RubyModule) {
            ((RubyModule)constant).calculateName();
        }
        return constant;
    }

    @JRubyMethod(name = "remove_const", required = 1, visibility = PRIVATE)
    public IRubyObject remove_const(ThreadContext context, IRubyObject rubyName) {
        String name = validateConstant(rubyName);
        IRubyObject value;
        if ((value = deleteConstant(name)) != null) {
            invalidateConstantCache(name);
            if (value != UNDEF) {
                return value;
            }
            removeAutoload(name);
            // FIXME: I'm not sure this is right, but the old code returned
            // the undef, which definitely isn't right...
            return context.runtime.getNil();
        }

        if (hasConstantInHierarchy(name)) {
            throw cannotRemoveError(name);
        }

        throw context.runtime.newNameError("constant " + name + " not defined for " + getName(), name);
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
     * @param rubyName The constant name which was found to be missing
     * @return Nothing! Absolutely nothing! (though subclasses might choose to return something)
     */
    @JRubyMethod(name = "const_missing", required = 1)
    public IRubyObject const_missing(ThreadContext context, IRubyObject rubyName, Block block) {
        Ruby runtime = context.runtime;
        String name;

        if (this != runtime.getObject()) {
            name = getName() + "::" + rubyName.asJavaString();
        } else {
            name = rubyName.asJavaString();
        }

        throw runtime.newNameErrorObject("uninitialized constant " + name, runtime.newSymbol(name));
    }

    public RubyArray constants(ThreadContext context) {
        return constants19(context);
    }

    @JRubyMethod(name = "constants")
    public RubyArray constants19(ThreadContext context) {
        return constantsCommon19(context, true, true);
    }

    @JRubyMethod(name = "constants")
    public RubyArray constants19(ThreadContext context, IRubyObject allConstants) {
        return constantsCommon19(context, false, allConstants.isTrue());
    }

    public RubyArray constantsCommon19(ThreadContext context, boolean replaceModule, boolean allConstants) {
        Ruby runtime = context.runtime;
        RubyArray array = runtime.newArray();

        Collection<String> constantNames = constantsCommon(runtime, replaceModule, allConstants, false);

        for (String name : constantNames) {
            array.add(runtime.newSymbol(name));
        }
        return array;
    }

    /** rb_mod_constants
     *
     */
    public Collection<String> constantsCommon(Ruby runtime, boolean replaceModule, boolean allConstants) {
        return constantsCommon(runtime, replaceModule, allConstants, true);
    }


    public Collection<String> constantsCommon(Ruby runtime, boolean replaceModule, boolean allConstants, boolean includePrivate) {
        RubyModule objectClass = runtime.getObject();

        Collection<String> constantNames = new HashSet<String>();
        if (allConstants) {
            if ((replaceModule && runtime.getModule() == this) || objectClass == this) {
                constantNames = objectClass.getConstantNames(includePrivate);
            } else {
                Set<String> names = new HashSet<String>();
                for (RubyModule module = this; module != null && module != objectClass; module = module.getSuperClass()) {
                    names.addAll(module.getConstantNames(includePrivate));
                }
                constantNames = names;
            }
        } else {
            if ((replaceModule && runtime.getModule() == this) || objectClass == this) {
                constantNames = objectClass.getConstantNames(includePrivate);
            } else {
                constantNames = getConstantNames(includePrivate);
            }
        }

        return constantNames;
    }

    @JRubyMethod
    public IRubyObject private_constant(ThreadContext context, IRubyObject rubyName) {
        String name = validateConstant(rubyName);

        setConstantVisibility(context, name, true);
        invalidateConstantCache(name);

        return this;
    }

    @JRubyMethod(required = 1, rest = true)
    public IRubyObject private_constant(ThreadContext context, IRubyObject[] rubyNames) {
        for (IRubyObject rubyName : rubyNames) {
            String name = validateConstant(rubyName);

            setConstantVisibility(context, name, true);
            invalidateConstantCache(name);
        }
        return this;
    }

    @JRubyMethod
    public IRubyObject public_constant(ThreadContext context, IRubyObject rubyName) {
        String name = validateConstant(rubyName);

        setConstantVisibility(context, name, false);
        invalidateConstantCache(name);
        return this;
    }

    @JRubyMethod(required = 1, rest = true)
    public IRubyObject public_constant(ThreadContext context, IRubyObject[] rubyNames) {
        for (IRubyObject rubyName : rubyNames) {
            String name = validateConstant(rubyName);
            setConstantVisibility(context, name, false);
            invalidateConstantCache(name);
        }
        return this;
    }

    @JRubyMethod(name = "prepend", rest = true)
    public IRubyObject prepend(ThreadContext context, IRubyObject[] modules) {
        // MRI checks all types first:
        for (int i = modules.length; --i >= 0; ) {
            IRubyObject obj = modules[i];
            if (!obj.isModule()) {
                throw context.runtime.newTypeError(obj, context.runtime.getModule());
            }
        }
        for (int i = modules.length - 1; i >= 0; i--) {
            modules[i].callMethod(context, "prepend_features", this);
            modules[i].callMethod(context, "prepended", this);
        }

        return this;
    }

    @JRubyMethod(name = "prepended", required = 1, visibility = PRIVATE)
    public IRubyObject prepended(ThreadContext context, IRubyObject other) {
        return context.runtime.getNil();
    }

    private void setConstantVisibility(ThreadContext context, String name, boolean hidden) {
        ConstantEntry entry = getConstantMap().get(name);

        if (entry == null) {
            throw context.runtime.newNameError("constant " + getName() + "::" + name + " not defined", name);
        }

        getConstantMapForWrite().put(name, new ConstantEntry(entry.value, hidden));
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

    @Deprecated
    public IRubyObject fastSetClassVar(final String internedName, final IRubyObject value) {
        return setClassVar(internedName, value);
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

    @Deprecated
    public IRubyObject fastGetClassVar(String internedName) {
        return getClassVar(internedName);
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

    @Deprecated
    public boolean fastIsClassVarDefined(String internedName) {
        return isClassVarDefined(internedName);
    }

    /** rb_mod_remove_cvar
     *
     * @deprecated - use {@link #removeClassVariable(String)}
     */
    @Deprecated
    public IRubyObject removeCvar(IRubyObject name) {
        return removeClassVariable(name.asJavaString());
    }

    public IRubyObject removeClassVariable(String name) {
        String javaName = validateClassVariable(name);
        IRubyObject value;

        if ((value = deleteClassVariable(javaName)) != null) {
            return value;
        }

        if (isClassVarDefined(javaName)) {
            throw cannotRemoveError(javaName);
        }

        throw getRuntime().newNameError("class variable " + javaName + " not defined for " + getName(), javaName);
    }


    //
    ////////////////// CONSTANT API METHODS ////////////////
    //

    /**
     * This version searches superclasses if we're starting with Object. This
     * corresponds to logic in rb_const_defined_0 that recurses for Object only.
     *
     * @param name the constant name to find
     * @return the constant, or null if it was not found
     */
    public IRubyObject getConstantAtSpecial(String name) {
        IRubyObject value;
        if (this == getRuntime().getObject()) {
            value = getConstantNoConstMissing(name);
        } else {
            value = fetchConstant(name);
        }

        return value == UNDEF ? resolveUndefConstant(name) : value;
    }

    public IRubyObject getConstantAt(String name) {
        return getConstantAt(name, true);
    }

    public IRubyObject getConstantAt(String name, boolean includePrivate) {
        IRubyObject value = fetchConstant(name, includePrivate);

        return value == UNDEF ? resolveUndefConstant(name) : value;
    }

    @Deprecated
    public IRubyObject fastGetConstantAt(String internedName) {
        return getConstantAt(internedName);
    }

    /**
     * Retrieve the named constant, invoking 'const_missing' should that be appropriate.
     *
     * @param name The constant to retrieve
     * @return The value for the constant, or null if not found
     */
    public IRubyObject getConstant(String name) {
        return getConstant(name, true);
    }

    public IRubyObject getConstant(String name, boolean inherit) {
        return getConstant(name, inherit, true);
    }

    public IRubyObject getConstant(String name, boolean inherit, boolean includeObject) {
        IRubyObject value = getConstantNoConstMissing(name, inherit, includeObject);
        Ruby runtime = getRuntime();

        return value == null ? callMethod(runtime.getCurrentContext(), "const_missing",
                runtime.newSymbol(name)) : value;
    }

    @Deprecated
    public IRubyObject fastGetConstant(String internedName) {
        return getConstant(internedName);
    }

    @Deprecated
    public IRubyObject fastGetConstant(String internedName, boolean inherit) {
        return getConstant(internedName, inherit);
    }

    public IRubyObject getConstantNoConstMissing(String name) {
        return getConstantNoConstMissing(name, true);
    }

    public IRubyObject getConstantNoConstMissing(String name, boolean inherit) {
        return getConstantNoConstMissing(name, inherit, true);
    }

    public IRubyObject getConstantNoConstMissing(String name, boolean inherit, boolean includeObject) {
        assert IdUtil.isConstant(name);

        IRubyObject constant = iterateConstantNoConstMissing(name, this, inherit);

        if (constant == null && !isClass() && includeObject) {
            constant = iterateConstantNoConstMissing(name, getRuntime().getObject(), inherit);
        }

        return constant;
    }

    private IRubyObject iterateConstantNoConstMissing(String name, RubyModule init, boolean inherit) {
        for (RubyModule p = init; p != null; p = p.getSuperClass()) {
            IRubyObject value = p.getConstantAt(name);

            if (value != null) return value == UNDEF ? null : value;
            if (!inherit) break;
        }
        return null;
    }

    // not actually called anywhere (all known uses call the fast version)
    public IRubyObject getConstantFrom(String name) {
        IRubyObject value = getConstantFromNoConstMissing(name);

        return value != null ? value : getConstantFromConstMissing(name);
    }

    @Deprecated
    public IRubyObject fastGetConstantFrom(String internedName) {
        return getConstantFrom(internedName);
    }

    public IRubyObject getConstantFromNoConstMissing(String name) {
        return getConstantFromNoConstMissing(name, true);
    }

    public IRubyObject getConstantFromNoConstMissing(String name, boolean includePrivate) {
        assert name == name.intern() : name + " is not interned";
        assert IdUtil.isConstant(name);
        Ruby runtime = getRuntime();
        RubyClass objectClass = runtime.getObject();
        IRubyObject value;

        RubyModule p = this;

        while (p != null) {
            if ((value = p.fetchConstant(name, false)) != null) {
                if (value == UNDEF) {
                    return p.resolveUndefConstant(name);
                }

                if (p == objectClass && this != objectClass) {
                    String badCName = getName() + "::" + name;
                    runtime.getWarnings().warn(ID.CONSTANT_BAD_REFERENCE, "toplevel constant " +
                            name + " referenced by " + badCName);
                }

                return value;
            }
            p = p.getSuperClass();
        }
        return null;
    }

    @Deprecated
    public IRubyObject fastGetConstantFromNoConstMissing(String internedName) {
        return getConstantFromNoConstMissing(internedName);
    }

    public IRubyObject getConstantFromConstMissing(String name) {
        return callMethod(getRuntime().getCurrentContext(),
                "const_missing", getRuntime().fastNewSymbol(name));
    }

    @Deprecated
    public IRubyObject fastGetConstantFromConstMissing(String internedName) {
        return getConstantFromConstMissing(internedName);
    }

    public IRubyObject resolveUndefConstant(String name) {
        return getAutoloadConstant(name);
    }

    /**
     * Set the named constant on this module. Also, if the value provided is another Module and
     * that module has not yet been named, assign it the specified name. This version does not
     * warn if the constant has already been set.
     *
     * @param name The name to assign
     * @param value The value to assign to it; if an unnamed Module, also set its basename to name
     * @return The result of setting the variable.
     */
    public IRubyObject setConstantQuiet(String name, IRubyObject value) {
        return setConstantCommon(name, value, false);
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
        return setConstantCommon(name, value, true);
    }

    /**
     * Set the named constant on this module. Also, if the value provided is another Module and
     * that module has not yet been named, assign it the specified name.
     *
     * @param name The name to assign
     * @param value The value to assign to it; if an unnamed Module, also set its basename to name
     * @return The result of setting the variable.
     */
    private IRubyObject setConstantCommon(String name, IRubyObject value, boolean warn) {
        IRubyObject oldValue = fetchConstant(name);
        if (oldValue != null) {
            if (oldValue == UNDEF) {
                setAutoloadConstant(name, value);
            } else {
                if (warn) {
                    getRuntime().getWarnings().warn(ID.CONSTANT_ALREADY_INITIALIZED, "already initialized constant " + name);
                }
                storeConstant(name, value);
            }
        } else {
            storeConstant(name, value);
        }

        invalidateConstantCache(name);

        // if adding a module under a constant name, set that module's basename to the constant name
        if (value instanceof RubyModule) {
            RubyModule module = (RubyModule)value;
            if (module != this && module.getBaseName() == null) {
                module.setBaseName(name);
                module.setParent(this);
            }
        }
        return value;
    }

    @Deprecated
    public IRubyObject fastSetConstant(String internedName, IRubyObject value) {
        return setConstant(internedName, value);
    }

    /** rb_define_const
     *
     */
    @Extension
    public void defineConstant(String name, IRubyObject value) {
        assert value != null;

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
                return getAutoloadMap().get(name) != null;
            }

        } while (isObject && (module = module.getSuperClass()) != null );

        return false;
    }

    public boolean fastIsConstantDefined(String internedName) {
        assert internedName.equals(internedName.intern()) : internedName + " is not interned";
        assert IdUtil.isConstant(internedName);
        boolean isObject = this == getRuntime().getObject();

        RubyModule module = this;

        do {
            Object value;
            if ((value = module.constantTableFetch(internedName)) != null) {
                if (value != UNDEF) return true;
                return getAutoloadMap().get(internedName) != null;
            }

        } while (isObject && (module = module.getSuperClass()) != null );

        return false;
    }

    public boolean fastIsConstantDefined19(String internedName) {
        return fastIsConstantDefined19(internedName, true);
    }

    public boolean fastIsConstantDefined19(String internedName, boolean inherit) {
        assert internedName.equals(internedName.intern()) : internedName + " is not interned";
        assert IdUtil.isConstant(internedName);

        for (RubyModule module = this; module != null; module = module.getSuperClass()) {
            Object value;
            if ((value = module.constantTableFetch(internedName)) != null) {
                if (value != UNDEF) return true;
                return getAutoloadMap().get(internedName) != null;
            }
            if (!inherit) {
                break;
            }
        }

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

    protected Map<String, IRubyObject> getClassVariables() {
        if (CLASSVARS_UPDATER == null) {
            return getClassVariablesForWriteSynchronized();
        } else {
            return getClassVariablesForWriteAtomic();
        }
    }

    /**
     * Get the class variables for write. If it is not set or not of the right size,
     * synchronize against the object and prepare it accordingly.
     *
     * @return the class vars map, ready for assignment
     */
    private Map<String,IRubyObject> getClassVariablesForWriteSynchronized() {
        Map myClassVars = classVariables;
        if (myClassVars == Collections.EMPTY_MAP) {
            synchronized (this) {
                myClassVars = classVariables;

                if (myClassVars == Collections.EMPTY_MAP) {
                    return classVariables = new ConcurrentHashMap<String, IRubyObject>(4, 0.75f, 2);
                } else {
                    return myClassVars;
                }
            }
        }

        return myClassVars;
    }


    /**
     * Get the class variables for write. If it is not set or not of the right size,
     * atomically update it with an appropriate value.
     *
     * @return the class vars map, ready for assignment
     */
    private Map<String,IRubyObject> getClassVariablesForWriteAtomic() {
        while (true) {
            Map myClassVars = classVariables;
            Map newClassVars;

            if (myClassVars == Collections.EMPTY_MAP) {
                newClassVars = new ConcurrentHashMap<String, IRubyObject>(4, 0.75f, 2);
            } else {
                return myClassVars;
            }

            // proceed with atomic update of table, or retry
            if (CLASSVARS_UPDATER.compareAndSet(this, myClassVars, newClassVars)) {
                return newClassVars;
            }
        }
    }

    protected Map<String, IRubyObject> getClassVariablesForRead() {
        return classVariables;
    }

    public boolean hasClassVariable(String name) {
        assert IdUtil.isClassVariable(name);
        return getClassVariablesForRead().containsKey(name);
    }

    @Deprecated
    public boolean fastHasClassVariable(String internedName) {
        return hasClassVariable(internedName);
    }

    public IRubyObject fetchClassVariable(String name) {
        assert IdUtil.isClassVariable(name);
        return getClassVariablesForRead().get(name);
    }

    @Deprecated
    public IRubyObject fastFetchClassVariable(String internedName) {
        return fetchClassVariable(internedName);
    }

    public IRubyObject storeClassVariable(String name, IRubyObject value) {
        assert IdUtil.isClassVariable(name) && value != null;
        ensureClassVariablesSettable();
        getClassVariables().put(name, value);
        return value;
    }

    @Deprecated
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

        if (!isFrozen()) {
            return;
        }

        if (this instanceof RubyModule) {
            throw runtime.newFrozenError(ERR_FROZEN_CONST_TYPE);
        } else {
            throw runtime.newFrozenError("");
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

    @Deprecated
    public boolean fastHasConstant(String internedName) {
        return hasConstant(internedName);
    }

    // returns the stored value without processing undefs (autoloads)
    public IRubyObject fetchConstant(String name) {
        return fetchConstant(name, true);
    }

    public IRubyObject fetchConstant(String name, boolean includePrivate) {
        assert IdUtil.isConstant(name);
        ConstantEntry entry = constantEntryFetch(name);

        if (entry == null) return null;

        if (entry.hidden && !includePrivate) {
            throw getRuntime().newNameError("private constant " + getName() + "::" + name + " referenced", name);
        }

        return entry.value;
    }

    @Deprecated
    public IRubyObject fastFetchConstant(String internedName) {
        return fetchConstant(internedName);
    }

    public IRubyObject storeConstant(String name, IRubyObject value) {
        assert IdUtil.isConstant(name) : name + " is not a valid constant name";
        assert value != null : "value is null";

        ensureConstantsSettable();
        return constantTableStore(name, value);
    }

    @Deprecated
    public IRubyObject fastStoreConstant(String internedName, IRubyObject value) {
        return storeConstant(internedName, value);
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

    public Collection<String> getConstantNames(boolean includePrivate) {
        if (includePrivate) return getConstantNames();

        if (getConstantMap().size() == 0) {
            return Collections.EMPTY_SET;
        }

        HashSet<String> publicNames = new HashSet<String>(getConstantMap().size());

        for (Map.Entry<String, ConstantEntry> entry : getConstantMap().entrySet()) {
            if (entry.getValue().hidden) continue;
            publicNames.add(entry.getKey());
        }
        return publicNames;
    }

    protected final String validateConstant(IRubyObject name) {
        return validateConstant(name.asJavaString(), name);
    }

    protected final String validateConstant(String name, IRubyObject errorName) {
        if (IdUtil.isValidConstantName19(name)) return name;

        Ruby runtime = getRuntime();

        Encoding resultEncoding = runtime.getDefaultInternalEncoding();
        if (resultEncoding == null) resultEncoding = runtime.getDefaultExternalEncoding();

        // MRI is more complicated than this and distinguishes between ID and non-ID.
        RubyString nameString = errorName.asString();

        // MRI does strlen to check for \0 vs Ruby string length.
        if ((nameString.getEncoding() != resultEncoding && !nameString.isAsciiOnly()) ||
                nameString.toString().contains("\0")) {
            nameString = (RubyString) nameString.inspect();
        }

        throw getRuntime().newNameError("wrong constant name " + nameString, name);
    }

    protected final void ensureConstantsSettable() {
        if (isFrozen()) throw getRuntime().newFrozenError(ERR_FROZEN_CONST_TYPE);
    }

    protected boolean constantTableContains(String name) {
        return getConstantMap().containsKey(name);
    }

    protected IRubyObject constantTableFetch(String name) {
        ConstantEntry entry = getConstantMap().get(name);
        if (entry == null) return null;
        return entry.value;
    }

    protected ConstantEntry constantEntryFetch(String name) {
        return getConstantMap().get(name);
    }

    protected IRubyObject constantTableStore(String name, IRubyObject value) {
        Map<String, ConstantEntry> constMap = getConstantMapForWrite();
        boolean hidden = false;

        ConstantEntry entry = constMap.get(name);
        if (entry != null) hidden = entry.hidden;

        constMap.put(name, new ConstantEntry(value, hidden));
        return value;
    }

    protected IRubyObject constantTableRemove(String name) {
        ConstantEntry entry = getConstantMapForWrite().remove(name);
        if (entry == null) return null;
        return entry.value;
    }

    /**
     * Define an autoload. ConstantMap holds UNDEF for the name as an autoload marker.
     */
    protected void defineAutoload(String name, IAutoloadMethod loadMethod) {
        Autoload existingAutoload = getAutoloadMap().get(name);
        if (existingAutoload == null || existingAutoload.getValue() == null) {
            storeConstant(name, RubyObject.UNDEF);
            getAutoloadMapForWrite().put(name, new Autoload(loadMethod));
        }
    }

    /**
     * Extract an Object which is defined by autoload thread from autoloadMap and define it as a constant.
     */
    protected IRubyObject finishAutoload(String name) {
        Autoload autoload = getAutoloadMap().get(name);
        if (autoload != null) {
            IRubyObject value = autoload.getValue();
            if (value != null) {
                storeConstant(name, value);
            }
            removeAutoload(name);
            return value;
        }
        return null;
    }

    /**
     * Get autoload constant.
     * If it's first resolution for the constant, it tries to require the defined feature and returns the defined value.
     * Multi-threaded accesses are blocked and processed sequentially except if the caller is the autoloading thread.
     */
    public IRubyObject getAutoloadConstant(String name) {
        Autoload autoload = getAutoloadMap().get(name);
        if (autoload == null) {
            return null;
        }
        return autoload.getConstant(getRuntime().getCurrentContext());
    }

    /**
     * Set an Object as a defined constant in autoloading.
     */
    private void setAutoloadConstant(String name, IRubyObject value) {
        Autoload autoload = getAutoloadMap().get(name);
        if (autoload != null) {
            if (!autoload.setConstant(getRuntime().getCurrentContext(), value)) {
                storeConstant(name, value);
                removeAutoload(name);
            }
        } else {
            storeConstant(name, value);
        }
    }

    /**
     * Removes an Autoload object from autoloadMap. ConstantMap must be updated before calling this.
     */
    private void removeAutoload(String name) {
        getAutoloadMapForWrite().remove(name);
    }

    protected String getAutoloadFile(String name) {
        Autoload autoload = getAutoloadMap().get(name);
        if (autoload != null) {
            return autoload.getFile();
        }
        return null;
    }

    private static void define(RubyModule module, JavaMethodDescriptor desc, String simpleName, DynamicMethod dynamicMethod) {
        JRubyMethod jrubyMethod = desc.anno;
        // check for frame field reads or writes
        CallConfiguration needs = CallConfiguration.valueOf(AnnotationHelper.getCallerCallConfigNameByAnno(jrubyMethod));

        if (needs.framing() == Framing.Full) {
            Set<String> frameAwareMethods = new HashSet<String>();
            AnnotationHelper.addMethodNamesToSet(frameAwareMethods, jrubyMethod, simpleName);
            MethodIndex.FRAME_AWARE_METHODS.addAll(frameAwareMethods);
        }
        if (needs.scoping() == Scoping.Full) {
            Set<String> scopeAwareMethods = new HashSet<String>();
            AnnotationHelper.addMethodNamesToSet(scopeAwareMethods, jrubyMethod, simpleName);
            MethodIndex.SCOPE_AWARE_METHODS.addAll(scopeAwareMethods);
        }

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
                module.getMethodLocation().addMethod(baseName, dynamicMethod);
            } else {
                baseName = jrubyMethod.name()[0];
                for (String name : jrubyMethod.name()) {
                    module.getMethodLocation().addMethod(name, dynamicMethod);
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

    @Deprecated
    public IRubyObject initialize(Block block) {
        return initialize(getRuntime().getCurrentContext());
    }

    public KindOf kindOf = KindOf.DEFAULT_KIND_OF;

    public final int id;

    /**
     * The class/module within whose namespace this class/module resides.
     */
    public RubyModule parent;

    /**
     * The base name of this class/module, excluding nesting. If null, this is
     * an anonymous class.
     */
    protected String baseName;

    /**
     * The cached anonymous class name, since it never changes and has a nonzero
     * cost to calculate.
     */
    private String anonymousName;

    /**
     * The cached name, only cached once this class and all containing classes are non-anonymous
     */
    private String cachedName;

    private volatile Map<String, ConstantEntry> constants = Collections.EMPTY_MAP;

    /**
     * Represents a constant value, possibly hidden (private).
     */
    public static class ConstantEntry {
        public final IRubyObject value;
        public final boolean hidden;

        public ConstantEntry(IRubyObject value, boolean hidden) {
            this.value = value;
            this.hidden = hidden;
        }

        public ConstantEntry dup() {
            return new ConstantEntry(value, hidden);
        }
    }

    /**
     * Objects for holding autoload state for the defined constant.
     *
     * 'Module#autoload' creates this object and stores it in autoloadMap.
     * This object can be shared with multiple threads so take care to change volatile and synchronized definitions.
     */
    private class Autoload {
        // A ThreadContext which is executing autoload.
        private volatile ThreadContext ctx;
        // The lock for test-and-set the ctx.
        private final Object ctxLock = new Object();
        // An object defined for the constant while autoloading.
        private volatile IRubyObject value;
        // A method which actually requires a defined feature.
        private final IAutoloadMethod loadMethod;

        Autoload(IAutoloadMethod loadMethod) {
            this.ctx = null;
            this.value = null;
            this.loadMethod = loadMethod;
        }

        // Returns an object for the constant if the caller is the autoloading thread.
        // Otherwise, try to start autoloading and returns the defined object by autoload.
        IRubyObject getConstant(ThreadContext ctx) {
            synchronized (ctxLock) {
                if (this.ctx == null) {
                    this.ctx = ctx;
                } else if (isSelf(ctx)) {
                    return getValue();
                }
                // This method needs to be synchronized for removing Autoload
                // from autoloadMap when it's loaded.
                getLoadMethod().load(ctx.runtime);
            }
            return getValue();
        }

        // Update an object for the constant if the caller is the autoloading thread.
        boolean setConstant(ThreadContext ctx, IRubyObject newValue) {
            synchronized(ctxLock) {
                boolean isSelf = isSelf(ctx);

                if (isSelf) value = newValue;

                return isSelf;
            }
        }

        // Returns an object for the constant defined by autoload.
        IRubyObject getValue() {
            return value;
        }

        // Returns the assigned feature.
        String getFile() {
            return getLoadMethod().file();
        }

        private IAutoloadMethod getLoadMethod() {
            return loadMethod;
        }

        private boolean isSelf(ThreadContext rhs) {
            return ctx != null && ctx.getThread() == rhs.getThread();
        }
    }

    /**
     * Set whether this class is associated with (i.e. a proxy for) a normal
     * Java class or interface.
     */
    public void setJavaProxy(boolean javaProxy) {
        this.javaProxy = javaProxy;
    }

    /**
     * Get whether this class is associated with (i.e. a proxy for) a normal
     * Java class or interface.
     */
    public boolean getJavaProxy() {
        return javaProxy;
    }

    /**
     * Get whether this Java proxy class should try to keep its instances idempotent
     * and alive using the ObjectProxyCache.
     */
    public boolean getCacheProxy() {
        return getFlag(USER0_F);
    }

    /**
     * Set whether this Java proxy class should try to keep its instances idempotent
     * and alive using the ObjectProxyCache.
     */
    public void setCacheProxy(boolean cacheProxy) {
        setFlag(USER0_F, cacheProxy);
    }

    public Set<String> discoverInstanceVariables() {
        HashSet<String> set = new HashSet();
        RubyModule cls = this;
        while (cls != null) {
            for (DynamicMethod method : cls.getNonIncludedClass().getMethodLocation().getMethods().values()) {
                MethodData methodData = method.getMethodData();
                set.addAll(methodData.getIvarNames());
            }

            if (cls instanceof RubyClass) {
                cls = ((RubyClass)cls).getSuperClass();
            } else {
                break;
            }
        }
        return set;
    }

    public boolean isRefinement() {
        return (flags & REFINED_MODULE_F) == REFINED_MODULE_F;
    }

    /**
     * Return true if the given method is defined on this class and is a builtin
     * (defined in Java at boot).
     *
     * @param methodName
     * @return
     */
    public boolean isMethodBuiltin(String methodName) {
        DynamicMethod method = searchMethodInner(methodName);

        return method != null && method.isBuiltin();
    }

    public Map<RubyClass, RubyModule> getRefinements() {
        return refinements;
    }

    public void setRefinements(Map<RubyClass, RubyModule> refinements) {
        this.refinements = refinements;
    }

    private volatile Map<String, Autoload> autoloads = Collections.EMPTY_MAP;
    protected volatile Map<String, DynamicMethod> methods = Collections.EMPTY_MAP;
    protected Map<String, CacheEntry> cachedMethods = Collections.EMPTY_MAP;
    protected int generation;
    protected Integer generationObject;

    protected volatile Set<RubyClass> includingHierarchies = Collections.EMPTY_SET;
    protected volatile RubyModule methodLocation = this;

    // ClassProviders return Java class/module (in #defineOrGetClassUnder and
    // #defineOrGetModuleUnder) when class/module is opened using colon syntax.
    private transient volatile Set<ClassProvider> classProviders = Collections.EMPTY_SET;

    // superClass may be null.
    protected RubyClass superClass;

    /**
     * The index of this class in the ClassIndex. Only non-zero for native JRuby
     * classes that have a corresponding entry in ClassIndex.
     *
     * @see ClassIndex
     * @deprecated use RubyModule#getClassIndex()
     */
    @Deprecated
    public int index;

    @Deprecated
    public static final Set<String> SCOPE_CAPTURING_METHODS = new HashSet<String>(Arrays.asList(
            "eval",
            "module_eval",
            "class_eval",
            "instance_eval",
            "module_exec",
            "class_exec",
            "instance_exec",
            "binding",
            "local_variables"
    ));

    protected ClassIndex classIndex = ClassIndex.NO_INDEX;

    private volatile Map<String, IRubyObject> classVariables = Collections.EMPTY_MAP;

    /** Refinements added to this module are stored here **/
    private volatile Map<RubyClass, RubyModule> refinements = Collections.EMPTY_MAP;

    /** A list of refinement hosts for this refinement */
    private volatile Map<RubyClass, IncludedModuleWrapper> activatedRefinements = Collections.EMPTY_MAP;

    /** The class this refinement refines */
    volatile RubyClass refinedClass = null;

    /** The moduel where this refinement was defined */
    private volatile RubyModule definedAt = null;

    private static final AtomicReferenceFieldUpdater CLASSVARS_UPDATER;

    static {
        AtomicReferenceFieldUpdater updater = null;
        try {
            updater = AtomicReferenceFieldUpdater.newUpdater(RubyModule.class, Map.class, "classVariables");
        } catch (RuntimeException re) {
            if (re.getCause() instanceof AccessControlException) {
                // security prevented creation; fall back on synchronized assignment
            } else {
                throw re;
            }
        }
        CLASSVARS_UPDATER = updater;
    }

    // Invalidator used for method caches
    protected final Invalidator methodInvalidator;

    /** Whether this class proxies a normal Java class */
    private boolean javaProxy = false;
}
