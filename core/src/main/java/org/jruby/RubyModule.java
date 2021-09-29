/*
 **** BEGIN LICENSE BLOCK *****
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

import com.headius.invokebinder.Binder;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import org.jcodings.Encoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.anno.AnnotationBinder;
import org.jruby.anno.AnnotationHelper;
import org.jruby.anno.FrameField;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyConstant;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JavaMethodDescriptor;
import org.jruby.anno.TypePopulator;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.embed.Extension;
import org.jruby.exceptions.LoadError;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.RuntimeError;
import org.jruby.internal.runtime.methods.AliasMethod;
import org.jruby.internal.runtime.methods.AttrReaderMethod;
import org.jruby.internal.runtime.methods.AttrWriterMethod;
import org.jruby.internal.runtime.methods.DefineMethodMethod;
import org.jruby.internal.runtime.methods.DelegatingDynamicMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.internal.runtime.methods.NativeCallMethod;
import org.jruby.internal.runtime.methods.PartialDelegatingMethod;
import org.jruby.internal.runtime.methods.ProcMethod;
import org.jruby.internal.runtime.methods.RefinedMarker;
import org.jruby.internal.runtime.methods.RefinedWrapper;
import org.jruby.internal.runtime.methods.SynchronizedDynamicMethod;
import org.jruby.internal.runtime.methods.UndefinedMethod;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRMethod;
import org.jruby.ir.targets.indy.Bootstrap;
import org.jruby.javasupport.JavaClass;
import org.jruby.javasupport.binding.MethodGatherer;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.Constants;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.IRBlockBody;
import org.jruby.runtime.MethodFactory;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.Variable;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.runtime.load.LoadService;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.runtime.opto.Invalidator;
import org.jruby.runtime.opto.OptoFactory;
import org.jruby.runtime.profile.MethodEnhancer;
import org.jruby.util.ByteList;
import org.jruby.util.ClassProvider;
import org.jruby.util.CommonByteLists;
import org.jruby.util.IdUtil;
import org.jruby.util.StringSupport;
import org.jruby.util.TypeConverter;
import org.jruby.util.cli.Options;
import org.jruby.util.collections.WeakHashSet;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import static org.jruby.anno.FrameField.BACKREF;
import static org.jruby.anno.FrameField.BLOCK;
import static org.jruby.anno.FrameField.CLASS;
import static org.jruby.anno.FrameField.FILENAME;
import static org.jruby.anno.FrameField.LASTLINE;
import static org.jruby.anno.FrameField.LINE;
import static org.jruby.anno.FrameField.METHODNAME;
import static org.jruby.anno.FrameField.SCOPE;
import static org.jruby.anno.FrameField.SELF;
import static org.jruby.anno.FrameField.VISIBILITY;
import static org.jruby.runtime.Visibility.MODULE_FUNCTION;
import static org.jruby.runtime.Visibility.PRIVATE;
import static org.jruby.runtime.Visibility.PROTECTED;
import static org.jruby.runtime.Visibility.PUBLIC;

import static org.jruby.runtime.Visibility.UNDEFINED;
import static org.jruby.util.RubyStringBuilder.str;
import static org.jruby.util.RubyStringBuilder.ids;
import static org.jruby.util.RubyStringBuilder.types;


/**
 *
 * @author  jpetersen
 */
@JRubyClass(name="Module")
public class RubyModule extends RubyObject {

    private static final Logger LOG = LoggerFactory.getLogger(RubyModule.class);
    // static { LOG.setDebugEnable(true); } // enable DEBUG output

    public static final int CACHEPROXY_F = ObjectFlags.CACHEPROXY_F;
    public static final int NEEDSIMPL_F = ObjectFlags.NEEDSIMPL_F;
    public static final int REFINED_MODULE_F = ObjectFlags.REFINED_MODULE_F;
    public static final int IS_OVERLAID_F = ObjectFlags.IS_OVERLAID_F;
    public static final int OMOD_SHARED = ObjectFlags.OMOD_SHARED;
    public static final int INCLUDED_INTO_REFINEMENT = ObjectFlags.INCLUDED_INTO_REFINEMENT;

    public static final ObjectAllocator MODULE_ALLOCATOR = RubyModule::new;

    public static RubyClass createModuleClass(Ruby runtime, RubyClass moduleClass) {
        moduleClass.setClassIndex(ClassIndex.MODULE);
        moduleClass.setReifiedClass(RubyModule.class);
        moduleClass.kindOf = new RubyModule.JavaClassKindOf(RubyModule.class);

        moduleClass.defineAnnotatedMethods(RubyModule.class);
        moduleClass.defineAnnotatedMethods(ModuleKernelMethods.class);

        return moduleClass;
    }

    public void checkValidBindTargetFrom(ThreadContext context, RubyModule originModule, boolean fromBind) throws RaiseException {
        // Module methods can always be transplanted
        if (!originModule.isModule() && !hasModuleInHierarchy(originModule)) {
            if (originModule instanceof MetaClass) {
                throw context.runtime.newTypeError("can't bind singleton method to a different class");
            } else {
                String thing = fromBind ? "an instance" : "a subclass"; // bind : define_method
                throw context.runtime.newTypeError("bind argument must be " + thing + " of " + originModule.getName());
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

    @JRubyMethod
    public IRubyObject autoload(ThreadContext context, IRubyObject symbol, IRubyObject file) {
        final Ruby runtime = context.runtime;

        final RubyString fileString =
                StringSupport.checkEmbeddedNulls(runtime, RubyFile.get_path(context, file));

        if (fileString.isEmpty()) throw runtime.newArgumentError("empty file name");

        final String symbolStr = symbol.asJavaString();

        if (!IdUtil.isValidConstantName(symbolStr)) {
            throw runtime.newNameError("autoload must be constant name", symbolStr);
        }

        IRubyObject existingValue = fetchConstant(symbolStr);

        if (existingValue != null && existingValue != RubyObject.UNDEF) return context.nil;

        defineAutoload(symbolStr, fileString);

        return context.nil;
    }

    @JRubyMethod(name = "autoload?")
    public IRubyObject autoload_p(ThreadContext context, IRubyObject symbol) {
        final String name = TypeConverter.checkID(symbol).idString();

        for (RubyModule mod = this; mod != null; mod = mod.getSuperClass()) {
            final IRubyObject loadedValue = mod.fetchConstant(name);

            if (loadedValue == UNDEF) {
                final RubyString file;

                Autoload autoload = mod.getAutoloadMap().get(name);

                // autoload has been evacuated
                if (autoload == null) return context.nil;

                // autoload has been completed
                if (autoload.getValue() != null) return context.nil;

                file = autoload.getFile();

                // autoload is in progress on another thread
                if (autoload.ctx != null && !autoload.isSelf(context)) return file;

                // file load is in progress or file is already loaded
                if (!getRuntime().getLoadService().featureAlreadyLoaded(file.asJavaString())) return file;
            }
        }
        return context.nil;
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
            RubyModule cl = obj.getMetaClass();

            return cl.searchAncestor(type.getDelegate().getOrigin()) != null;
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
        return getOrigin().constants;
    }

    public Map<String, ConstantEntry> getConstantMapForWrite() {
        Map<String, ConstantEntry> constants = this.constants;
        if (constants == Collections.EMPTY_MAP) {
            synchronized (this) {
                constants = this.constants;
                if (constants == Collections.EMPTY_MAP) {
                    constants = this.constants = new ConcurrentHashMap<>(2, 0.9f, 1);
                }
            }
        }
        return constants;
    }

    /**
     * AutoloadMap must be accessed after checking ConstantMap. Checking UNDEF value in constantMap works as a guard.
     * For looking up constant, check constantMap first then try to get an Autoload object from autoloadMap.
     * For setting constant, update constantMap first and remove an Autoload object from autoloadMap.
     */
    protected Map<String, Autoload> getAutoloadMap() {
        return autoloads;
    }

    protected Map<String, Autoload> getAutoloadMapForWrite() {
        Map<String, Autoload> autoloads = this.autoloads;
        if (autoloads == Collections.EMPTY_MAP) {
            synchronized (this) {
                autoloads = this.autoloads;
                if (autoloads == Collections.EMPTY_MAP) {
                    autoloads = this.autoloads = new ConcurrentHashMap<>(2, 0.9f, 1);
                }
            }
        }
        return autoloads;
    }

    @SuppressWarnings("unchecked")
    public void addIncludingHierarchy(IncludedModule hierarchy) {
        synchronized (getRuntime().getHierarchyLock()) {
            Set<RubyClass> including = this.includingHierarchies;
            if (including == Collections.EMPTY_SET) {
                including = this.includingHierarchies = new WeakHashSet(4);
            }
            including.add(hierarchy);
        }
    }

    public final MethodHandle getIdTest() {
        MethodHandle idTest = this.idTest;
        if (idTest != null) return idTest;
        return this.idTest = newIdTest();
    }

    protected final MethodHandle newIdTest() {
        return Binder.from(boolean.class, ThreadContext.class, IRubyObject.class)
                .insert(2, id)
                .invoke(testModuleMatch);
    }

    /** separate path for MetaClass construction
     *
     */
    protected RubyModule(Ruby runtime, RubyClass metaClass, boolean objectSpace) {
        super(runtime, metaClass, objectSpace);

        id = runtime.allocModuleId();

        runtime.addModule(this);
        // if (parent == null) parent = runtime.getObject();
        setFlag(NEEDSIMPL_F, !isClass());
        updateGeneration(runtime);

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
        return getFlag(NEEDSIMPL_F);
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
    public final synchronized void addClassProvider(ClassProvider provider) {
        Set<ClassProvider> classProviders = this.classProviders;
        if (!classProviders.contains(provider)) {
            Set<ClassProvider> cp = new HashSet<>(classProviders.size() + 1);
            cp.addAll(classProviders);
            cp.add(provider);
            this.classProviders = cp;
        }
    }

    public final synchronized void removeClassProvider(ClassProvider provider) {
        Set<ClassProvider> cp = new HashSet<>(classProviders);
        cp.remove(provider);
        this.classProviders = cp;
    }

    private void checkForCyclicInclude(RubyModule m) throws RaiseException {
        if (isSameOrigin(m)) throw getRuntime().newArgumentError("cyclic include detected");
    }

    protected void checkForCyclicPrepend(RubyModule m) throws RaiseException {
        if (isSameOrigin(m)) throw getRuntime().newArgumentError(getName() + " cyclic prepend detected " + m.getName());
    }

    private RubyClass searchProvidersForClass(String name, RubyClass superClazz) {
        Set<ClassProvider> classProviders = this.classProviders;
        if (classProviders == Collections.EMPTY_SET) return null;

        RubyClass clazz;
        for (ClassProvider classProvider: classProviders) {
            if ((clazz = classProvider.defineClassUnder(this, name, superClazz)) != null) {
                return clazz;
            }
        }
        return null;
    }

    private RubyModule searchProvidersForModule(String name) {
        Set<ClassProvider> classProviders = this.classProviders;
        if (classProviders == Collections.EMPTY_SET) return null;

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

    public Map<String, DynamicMethod> getMethodsForWrite() {
        Map<String, DynamicMethod> methods = this.methods;
        if (methods != Collections.EMPTY_MAP) return methods;

        synchronized (this) {
            methods = this.methods;
            return methods == Collections.EMPTY_MAP ?
                this.methods = new ConcurrentHashMap<>(2, 0.9f, 1) : // CHM initial-size: 4
                    methods;
        }
    }

    /**
     * @note Internal API - only public as its used by generated code!
     * @param runtime
     * @param id identifier string (8859_1).  Matching entry in symbol table.
     * @param method
     * @return method
     */ // NOTE: used by AnnotationBinder
    public DynamicMethod putMethod(Ruby runtime, String id, DynamicMethod method) {
        if (hasPrepends()) {
            method = method.dup();
            method.setImplementationClass(methodLocation);
        }

        DynamicMethod oldMethod = methodLocation.getMethodsForWrite().put(id, method);

        if (oldMethod != null && oldMethod.isRefined()) {
            methodLocation.getMethodsForWrite().put(id, new RefinedWrapper(method.getImplementationClass(), method.getVisibility(), id, method));
        }

        runtime.addProfiledMethod(id, method);
        return method;
    }

    /**
     * Is this module one that in an included one (e.g. an {@link IncludedModuleWrapper}).
     * @see IncludedModule
     */
    public boolean isIncluded() {
        return false;
    }

    public boolean isPrepended() {
        return false;
    }

    /**
     * In an included or prepended module what is the ACTUAL module it represents?
     * @return the actual module of an included/prepended module.
     */
    public RubyModule getOrigin() {
        return this;
    }

    @Deprecated
    public RubyModule getNonIncludedClass() {
        return this;
    }

    public RubyModule getDelegate() {
        return this;
    }

    /**
     * Get the "real" module, either the current one or the nearest ancestor that is not a singleton or include wrapper.
     *
     * See {@link RubyClass#getRealClass()}.
     *
     * @return the nearest non-singleton non-include module in the hierarchy
     */
    public RubyModule getRealModule() {
        RubyModule cl = this;
        while (cl != null &&
                (cl.isSingleton() || cl.isIncluded())) {
            cl = cl.superClass;
        }
        return cl;
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
     * Generate a fully-qualified class name or a #-style name for anonymous and singleton classes which
     * is properly encoded. The returned string is always frozen.
     *
     * @return a properly encoded class name.
     *
     * Note: getId() is only really valid for ASCII values.  This should be favored over using it.
     */
    public RubyString rubyName() {
        return cachedRubyName != null ? cachedRubyName : calculateRubyName();
    }

    public RubyString rubyBaseName() {
        String baseName = getBaseName();

        final Ruby runtime = metaClass.runtime;
        return baseName == null ? null : runtime.newSymbol(baseName).to_s(runtime);
    }

    private RubyString calculateAnonymousRubyName() {
        Ruby runtime = getRuntime();
        RubyString anonBase = runtime.newString("#<"); // anonymous classes get the #<Class:0xdeadbeef> format
        anonBase.append(metaClass.getRealClass().rubyName()).append(runtime.newString(":0x"));
        anonBase.append(runtime.newString(Integer.toHexString(System.identityHashCode(this)))).append(runtime.newString(">"));

        return anonBase;
    }

    private RubyString calculateRubyName() {
        boolean cache = true;

        if (getBaseName() == null) return calculateAnonymousRubyName(); // no name...anonymous!

        Ruby runtime = getRuntime();
        RubyClass objectClass = runtime.getObject();
        List<RubyString> parents = new ArrayList<>();
        for (RubyModule p = getParent(); p != null && p != objectClass; p = p.getParent()) {
            if (p == this) break;  // Break out of cyclic namespaces like C::A = C2; C2::A = C (jruby/jruby#2314)

            RubyString name = p.rubyBaseName();

            // This is needed when the enclosing class or module is a singleton.
            // In that case, we generated a name such as null::Foo, which broke
            // Marshalling, among others. The correct thing to do in this situation
            // is to insert the generate the name of form #<Class:01xasdfasd> if
            // it's a singleton module/class, which this code accomplishes.
            if (name == null) {
                cache = false;
                name = p.rubyName();
            }

            parents.add(name);
        }

        Collections.reverse(parents);

        RubyString colons = runtime.newString("::");
        RubyString fullName = runtime.newString();       // newString creates empty ByteList which ends up as
        fullName.setEncoding(USASCIIEncoding.INSTANCE);  // ASCII-8BIT.  8BIT is unfriendly to string concats.
        for (RubyString parent:  parents) {
            fullName.cat19(parent).cat19(colons);
        }
        fullName.cat19(rubyBaseName());

        fullName.setFrozen(true);

        if (cache) cachedRubyName = fullName;

        return fullName;
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
            // Break out of cyclic namespaces like C::A = C2; C2::A = C (jruby/jruby#2314)
            if (p == this) break;

            parentCount++;
        }

        // Allocate a String array for all of their names and populate it
        String[] parentNames = new String[parentCount];
        int i = parentCount - 1;
        int totalLength = name.length() + parentCount * 2; // name length + enough :: for all parents
        for (RubyModule p = getParent() ; p != null && p != objectClass ; p = p.getParent(), i--) {
            // Break out of cyclic namespaces like C::A = C2; C2::A = C (jruby/jruby#2314)
            if (p == this) break;

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
        String cachedName = this.cachedName; // re-use cachedName field since it won't be set for anonymous class
        if (cachedName == null) {
            // anonymous classes get the #<Class:0xdeadbeef> format
            StringBuilder anonBase = new StringBuilder(24);
            anonBase.append("#<").append(metaClass.getRealClass().getName()).append(":0x");
            anonBase.append(Integer.toHexString(System.identityHashCode(this))).append('>');
            cachedName = this.cachedName = anonBase.toString();
        }
        return cachedName;
    }


    @JRubyMethod(name = "refine", required = 1, reads = SCOPE)
    public IRubyObject refine(ThreadContext context, IRubyObject klass, Block block) {
        if (!block.isGiven()) throw context.runtime.newArgumentError("no block given");

        if (block.isEscaped()) throw context.runtime.newArgumentError("can't pass a Proc as a block to Module#refine");

        if (!(klass instanceof RubyModule)) throw context.runtime.newTypeError("wrong argument type " + klass.getType() + "(expected Class or Module)");

        if (refinements == Collections.EMPTY_MAP) refinements = newRefinementsMap();
        if (activatedRefinements == Collections.EMPTY_MAP) activatedRefinements = newActivatedRefinementsMap();

        RubyModule moduleToRefine = (RubyModule) klass;
        RubyModule refinement = refinements.get(moduleToRefine);
        if (refinement == null) {
            refinement = createNewRefinedModule(context, moduleToRefine);

            // Add it to the activated chain of other refinements already added to this class we are refining
            addActivatedRefinement(context, moduleToRefine, refinement);
        }

        // Executes the block supplied with the defined method definitions using the refinement as it's module.
        yieldRefineBlock(context, refinement, block);

        return refinement;
    }

    private RubyModule createNewRefinedModule(ThreadContext context, RubyModule klass) {
        Ruby runtime = context.runtime;

        RubyModule newRefinement = new RubyModule(runtime);

        RubyClass superClass = refinementSuperclass(runtime, klass);
        newRefinement.setSuperClass(superClass);
        newRefinement.setFlag(REFINED_MODULE_F, true);
        newRefinement.setFlag(NEEDSIMPL_F, false); // Refinement modules should not do implementer check
        newRefinement.refinedClass = klass;
        newRefinement.definedAt = this;
        refinements.put(klass, newRefinement);

        return newRefinement;
    }

    private static RubyClass refinementSuperclass(Ruby runtime, RubyModule superClass) {
        if (superClass.isModule()) {
            return new IncludedModuleWrapper(runtime, runtime.getBasicObject(), superClass);
        } else {
            return (RubyClass) superClass;
        }
    }

    private void yieldRefineBlock(ThreadContext context, RubyModule refinement, Block block) {
        block = block.cloneBlockAndFrame(EvalType.MODULE_EVAL);

        block.getBinding().setSelf(refinement);

        RubyModule overlayModule = block.getBody().getStaticScope().getOverlayModuleForWrite(context);
        overlayModule.refinements = refinements;

        block.yieldSpecific(context);
    }

    // This has three cases:
    // 1. class being refined has never had any refines happen to it yet: return itself
    // 2. class has been refined: return already existing refinementwrapper (chain of modules to call against)
    // 3. refinement is already in the refinementwrapper so we do not need to add it to the wrapper again: return null
    private RubyClass getAlreadyActivatedRefinementWrapper(RubyClass classWeAreRefining, RubyModule refinement) {
        // We have already encountered at least one refine on this class.  Return that wrapper.
        RubyClass moduleWrapperForRefinement = activatedRefinements.get(classWeAreRefining);
        if (moduleWrapperForRefinement == null) return classWeAreRefining;

        for (RubyModule c = moduleWrapperForRefinement; c != null && c.isIncluded(); c = c.getSuperClass()) {
            if (c.getOrigin() == refinement) return null;
        }

        return moduleWrapperForRefinement;
    }

    /*
     * We will find whether we have already refined once and get that set of includedmodules or we will start to create
     * one.  The new refinement will be added as a new included module on the front.  It will also add all superclasses
     * of the refinement into this call chain.
     *
     * MRI: add_activated_refinement
     */
    private void addActivatedRefinement(ThreadContext context, RubyModule moduleToRefine, RubyModule refinement) {
//        RubyClass superClass = getAlreadyActivatedRefinementWrapper(classWeAreRefining, refinement);
//        if (superClass == null) return; // already been refined and added to refinementwrapper
        RubyClass superClass = null;
        RubyClass c = activatedRefinements.get(moduleToRefine);
        if (c != null) {
            superClass = c;
            while (c != null && c.isIncluded()) {
                if (c.getOrigin() == refinement) {
            		/* already used refinement */
                    return;
                }
                c = c.getSuperClass();
            }
        }
        refinement.setFlag(IS_OVERLAID_F, true);
        IncludedModuleWrapper iclass = new IncludedModuleWrapper(context.runtime, superClass, refinement);
        c = iclass;
        c.refinedClass = moduleToRefine;
        for (refinement = refinement.getSuperClass(); refinement != null; refinement = refinement.getSuperClass()) {
            refinement.setFlag(IS_OVERLAID_F, true);
            c.setSuperClass(new IncludedModuleWrapper(context.runtime, c.getSuperClass(), refinement));
            c = c.getSuperClass();
            c.refinedClass = moduleToRefine;
        }
        activatedRefinements.put(moduleToRefine, iclass);
    }

    @JRubyMethod(name = "using", required = 1, visibility = PRIVATE, reads = {SELF, SCOPE})
    public IRubyObject using(ThreadContext context, IRubyObject refinedModule) {
        if (context.getFrameSelf() != this) throw context.runtime.newRuntimeError("Module#using is not called on self");
        if (context.getCurrentStaticScope().isWithinMethod()) {
            throw context.runtime.newRuntimeError("Module#using is not permitted in methods");
        }

        // I pass the cref even though I don't need to so that the concept is simpler to read
        StaticScope staticScope = context.getCurrentStaticScope();
        RubyModule overlayModule = staticScope.getOverlayModuleForWrite(context);
        usingModule(context, overlayModule, refinedModule);

        return this;
    }

    // mri: rb_using_module
    public static void usingModule(ThreadContext context, RubyModule cref, IRubyObject refinedModule) {
        if (!(refinedModule instanceof RubyModule)) throw context.runtime.newTypeError(refinedModule, context.runtime.getModule());

        usingModuleRecursive(cref, (RubyModule) refinedModule);
    }

    // mri: using_module_recursive
    private static void usingModuleRecursive(RubyModule cref, RubyModule module) {
        Ruby runtime = cref.getRuntime();
        RubyClass superClass = module.getSuperClass();

        // For each superClass of the refined module also use their refinements for the given cref
        if (superClass != null) usingModuleRecursive(cref, superClass);

        if (module instanceof IncludedModule) {
            module = module.getDelegate();
        } else if (module.isModule()) {
            // ok as is
        } else {
            throw runtime.newTypeError("wrong argument type " + module.getName() + " (expected Module)");
        }

        Map<RubyModule, RubyModule> refinements = module.refinements;
        if (refinements == null) return; // No refinements registered for this module

        for (Map.Entry<RubyModule, RubyModule> entry: refinements.entrySet()) {
            usingRefinement(runtime, cref, entry.getKey(), entry.getValue());
        }
    }

    /*
     * Within the context of this cref any references to the class we are refining will try and find
     * that definition from the refinement instead.  At one point I was confused how this would not
     * conflict if the same module was used in two places but the cref must be a lexically containing
     * module so it cannot live in two files.
     *
     * MRI: rb_using_refinement
     */
    private static void usingRefinement(Ruby runtime, RubyModule cref, RubyModule klass, RubyModule module) {
        RubyModule iclass, c, superclass = klass;

        if (cref.refinements == Collections.EMPTY_MAP) {
            cref.refinements = newRefinementsMap();
        } else {
            if (cref.getFlag(OMOD_SHARED)) {
                cref.refinements = newRefinementsMap(cref.refinements);
                cref.setFlag(OMOD_SHARED, false);
            }
            if ((c = cref.refinements.get(klass)) != null) {
                superclass = c;
                while (c != null && c instanceof IncludedModule) {
                    if (c.getOrigin() == module) {
                        /* already used refinement */
                        return;
                    }
                    c = c.getSuperClass();
                }
            }
        }

        module.setFlag(IS_OVERLAID_F, true);
        superclass = refinementSuperclass(runtime, superclass);
        c = iclass = new IncludedModuleWrapper(runtime, (RubyClass) superclass, module);
        c.refinedClass = klass;

//        RCLASS_M_TBL(OBJ_WB_UNPROTECT(c)) =
//                RCLASS_M_TBL(OBJ_WB_UNPROTECT(module)); /* TODO: check unprotecting */

        module = module.getSuperClass();
        while (module != null && module != klass) {
            module.setFlag(IS_OVERLAID_F, true);
            c.setSuperClass(new IncludedModuleWrapper(cref.getRuntime(), c.getSuperClass(), module));
            c = c.getSuperClass();
            c.refinedClass = klass;
            module = module.getSuperClass();
        }

        cref.refinements.put(klass, iclass);
    }

    public static Map<RubyModule, RubyModule> newRefinementsMap(Map<RubyModule, RubyModule> refinements) {
        return Collections.synchronizedMap(new IdentityHashMap<>(refinements));
    }

    public static Map<RubyModule, RubyModule> newRefinementsMap() {
        return Collections.synchronizedMap(new IdentityHashMap<>());
    }

    public static Map<RubyModule, IncludedModule> newActivatedRefinementsMap() {
        return Collections.synchronizedMap(new IdentityHashMap<>());
    }

    @JRubyMethod(name = "used_modules", reads = SCOPE)
    public IRubyObject used_modules(ThreadContext context) {
        StaticScope cref = context.getCurrentStaticScope();
        RubyArray ary = context.runtime.newArray();
        while (cref != null) {
            RubyModule overlay;
            if ((overlay = cref.getOverlayModuleForRead()) != null &&
                    !overlay.refinements.isEmpty()) {
                overlay.refinements.entrySet().stream().forEach(entry -> {
                    RubyModule mod = entry.getValue();
                    while (mod != null && mod.getOrigin().isRefinement()) {
                        ary.push(mod.getOrigin().definedAt);
                        mod = mod.getSuperClass();
                    }
                });
            }
            cref = cref.getPreviousCRefScope();
        }

        return ary;
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
     * Finds a module that is within the current module (or class).
     *
     * @param name to be found in this module (or class)
     * @return the module or null if no such module
     * @since 9.2
     */
    public RubyModule getModule(String name) {
        return (RubyModule) getConstantAt(name);
    }

    /**
     * Finds a class that is within the current module (or class).
     *
     * @param name to be found in this module (or class)
     * @return the class or null if no such class
     */
    public RubyClass getClass(String name) {
        return (RubyClass) getConstantAt(name);
    }

    @Deprecated
    public RubyClass fastGetClass(String internedName) {
        return getClass(internedName);
    }

    /**
     * Prepend a new module to this module or class.
     *
     * MRI: rb_prepend_module
     *
     * @param module The module to include
     */
    public void prependModule(RubyModule module) {
        testFrozen("module");

        if (module.refinedClass != null) {
            throw getRuntime().newArgumentError("refinement module is not allowed");
        }

        // Make sure the module we include does not already exist
        checkForCyclicPrepend(module);

        synchronized (this) {
            if (hasModuleInPrepends(module)) {
                invalidateCacheDescendants();
                return;
            }

            infectBy(module);

            doPrependModule(module);

            invalidateCoreClasses();
            invalidateCacheDescendants();
            invalidateConstantCacheForModuleInclusion(module);
        }
    }

    @Deprecated
    public void prependModule(IRubyObject arg) {
        assert arg != null;
        if (!(arg instanceof RubyModule)) {
            throw getRuntime().newTypeError("Wrong argument type " + arg.getMetaClass().getName() +
                    " (expected Module).");
        }
        prependModule((RubyModule) arg);
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

        if (module.refinedClass != null) {
            throw getRuntime().newArgumentError("refinement module is not allowed");
        }

        // Make sure the module we include does not already exist
        checkForCyclicInclude(module);

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

    public final void defineAnnotatedConstants(Class clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                defineAnnotatedConstant(field);
            }
        }
    }

    public final boolean defineAnnotatedConstant(Field field) {
        JRubyConstant jrubyConstant = field.getAnnotation(JRubyConstant.class);

        if (jrubyConstant == null) return false;

        Ruby runtime = getRuntime();

        Class tp = field.getType();
        IRubyObject realVal;

        try {
            if(tp == Integer.class || tp == Integer.TYPE || tp == Short.class || tp == Short.TYPE || tp == Byte.class || tp == Byte.TYPE) {
                realVal = RubyNumeric.int2fix(runtime, field.getInt(null));
            } else if(tp == Boolean.class || tp == Boolean.TYPE) {
                realVal = field.getBoolean(null) ? runtime.getTrue() : runtime.getFalse();
            } else {
                realVal = runtime.getNil();
            }
        } catch(Exception e) {
            realVal = runtime.getNil();
        }

        String[] names = jrubyConstant.value();
        if (names.length == 0) {
            setConstant(field.getName(), realVal);
        }
        else {
            for (String name : names) setConstant(name, realVal);
        }

        return true;
    }

    @Extension
    public void defineAnnotatedMethods(Class clazz) {
        defineAnnotatedMethodsIndividually(clazz);
    }

    public static final class MethodClumper {
        private HashMap<String, List<JavaMethodDescriptor>> annotatedMethods;
        private HashMap<String, List<JavaMethodDescriptor>> staticAnnotatedMethods;

        public Map<Set<FrameField>, List<String>> readGroups = Collections.EMPTY_MAP;
        public Map<Set<FrameField>, List<String>> writeGroups = Collections.EMPTY_MAP;

        @SuppressWarnings("deprecation")
        public void clump(final Class klass) {
            Method[] declaredMethods = MethodGatherer.DECLARED_METHODS.get(klass);
            for (Method method: declaredMethods) {
                JRubyMethod anno = method.getAnnotation(JRubyMethod.class);

                if (anno == null) continue;

                if (anno.compat() == org.jruby.CompatVersion.RUBY1_8) continue;

                // skip bridge methods, as generated by JDK8 javac for e.g. return-value overloaded methods
                if (method.isBridge()) continue;

                JavaMethodDescriptor desc = new JavaMethodDescriptor(method);

                final String[] names = anno.name();
                String name = names.length == 0 ? method.getName() : names[0];

                Map<String, List<JavaMethodDescriptor>> methodsHash;
                if (desc.isStatic) {
                    if ( (methodsHash = staticAnnotatedMethods) == null ) {
                        methodsHash = staticAnnotatedMethods = new HashMap<>();
                    }
                } else {
                    if ( (methodsHash = annotatedMethods) == null ) {
                        methodsHash = annotatedMethods = new HashMap<>();
                    }
                }

                List<JavaMethodDescriptor> methodDescs = methodsHash.get(name);
                if (methodDescs == null) {
                    methodsHash.put(name, methodDescs = new ArrayList<>(4));
                }

                methodDescs.add(desc);

                // check for frame field reads or writes
                if (anno.reads().length > 0 && readGroups == Collections.EMPTY_MAP) readGroups = new HashMap<>();
                if (anno.writes().length > 0 && writeGroups == Collections.EMPTY_MAP) writeGroups = new HashMap<>();

                AnnotationHelper.groupFrameFields(readGroups, writeGroups, anno, method.getName());
            }
        }

        @Deprecated // no-longer used
        public Map<String, List<JavaMethodDescriptor>> getAllAnnotatedMethods() {
            return null; // return allAnnotatedMethods;
        }

        public final Map<String, List<JavaMethodDescriptor>> getAnnotatedMethods() {
            return annotatedMethods == null ? Collections.EMPTY_MAP : annotatedMethods;
        }

        public final Map<String, List<JavaMethodDescriptor>> getStaticAnnotatedMethods() {
            return staticAnnotatedMethods == null ? Collections.EMPTY_MAP : staticAnnotatedMethods;
        }
    }

    public static TypePopulator loadPopulatorFor(Class<?> type) {
        if (Options.DEBUG_FULLTRACE.load()) {
            // we want non-generated invokers or need full traces, use default (slow) populator
            LOG.debug("trace mode, using default populator");
        } else {
            try {
                String qualifiedName = Constants.GENERATED_PACKAGE + type.getCanonicalName().replace('.', '$');
                String fullName = qualifiedName + AnnotationBinder.POPULATOR_SUFFIX;
                String fullPath = fullName.replace('.', '/') + ".class";
                if (LOG.isDebugEnabled()) LOG.debug("looking for populator " + fullName);

                if (Ruby.getClassLoader().getResource(fullPath) == null) {
                    LOG.debug("could not find it, using default populator");
                } else {
                    return (TypePopulator) Class.forName(fullName).getConstructor().newInstance();
                }
            } catch (Throwable ex) {
                if (LOG.isDebugEnabled()) LOG.debug("could not find populator, using default (" + ex + ')');
            }
        }

        return new TypePopulator.ReflectiveTypePopulator(type);
    }

    public final void defineAnnotatedMethodsIndividually(Class clazz) {
        getRuntime().POPULATORS.get(clazz).populate(this, clazz);
    }

    public final boolean defineAnnotatedMethod(String name, List<JavaMethodDescriptor> methods, MethodFactory methodFactory) {
        JavaMethodDescriptor desc = methods.get(0);
        if (methods.size() == 1) {
            return defineAnnotatedMethod(name, desc, methodFactory);
        }

        DynamicMethod dynamicMethod = methodFactory.getAnnotatedMethod(this, methods, name);
        define(this, desc, name, dynamicMethod);

        return true;
    }

    public final boolean defineAnnotatedMethod(Method method, MethodFactory methodFactory) {
        JRubyMethod jrubyMethod = method.getAnnotation(JRubyMethod.class);

        if (jrubyMethod == null) return false;

        JavaMethodDescriptor desc = new JavaMethodDescriptor(method);
        DynamicMethod dynamicMethod = methodFactory.getAnnotatedMethod(this, desc, method.getName());
        define(this, desc, method.getName(), dynamicMethod);

        return true;
    }

    public final boolean defineAnnotatedMethod(String name, JavaMethodDescriptor desc, MethodFactory methodFactory) {
        JRubyMethod jrubyMethod = desc.anno;

        if (jrubyMethod == null) return false;

        DynamicMethod dynamicMethod = methodFactory.getAnnotatedMethod(this, desc, name);
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
        if (name.equals("__send__") || name.equals("object_id") || name.equals("initialize")) {
            runtime.getWarnings().warn(ID.UNDEFINING_BAD, "undefining `"+ name +"' may cause serious problems");
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
                IRubyObject obj = ((MetaClass) c).getAttached();

                if (obj instanceof RubyModule) {
                    c = (RubyModule) obj;
                    s0 = "";
                }
            } else if (c.isModule()) {
                s0 = " module";
            }

            // FIXME: Since we found no method we probably do not have symbol entry...do not want to pollute symbol table here.
            throw runtime.newNameError("Undefined method " + name + " for" + s0 + " '" + c.getName() + "'", name);
        }
        methodLocation.addMethod(name, UndefinedMethod.getInstance());

        if (isSingleton()) {
            ((MetaClass) this).getAttached().callMethod(context, "singleton_method_undefined", runtime.newSymbol(name));
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
                return context.tru;
            }
        }

        return context.fals;
    }

    @JRubyMethod(name = "singleton_class?")
    public IRubyObject singleton_class_p(ThreadContext context) {
        return RubyBoolean.newBoolean(context, isSingleton());
    }

    public void addMethod(String id, DynamicMethod method) {
        testFrozen("class/module");

        RubyModule location = this;

        if (methodLocation != this) {
            methodLocation.addMethod(id, method);
            return;
        }

        if (this instanceof MetaClass) {
            // FIXME: Gross and not quite right. See MRI's rb_frozen_class_p logic
            ((MetaClass) this).getAttached().testFrozen();
        }

        if (isRefinement()) {
            // create refined entry on target class
            addRefinedMethodEntry(id, method);
        }

        addMethodInternal(id, method);
    }

    // MRI: rb_add_refined_method_entry
    private void addRefinedMethodEntry(String id, DynamicMethod method) {
        RubyModule methodLocation = refinedClass.getMethodLocation();
        DynamicMethod orig = methodLocation.searchMethodCommon(id);

        if (orig == null) {
            refinedClass.addMethod(id, new RefinedMarker(methodLocation, method.getVisibility(), id));
        } else {
            if (orig.isRefined()) {
                return;
            }
            refinedClass.addMethod(id, new RefinedWrapper(methodLocation, method.getVisibility(), id, orig));
        }
    }

    public final void addMethodInternal(String name, DynamicMethod method) {
        synchronized (methodLocation.getMethodsForWrite()) {
            putMethod(getRuntime(), name, method);
            invalidateCoreClasses();
            invalidateCacheDescendants();
        }
    }

    /**
     * This method is not intended for use by normal users; it is a fast-path
     * method that skips synchronization and hierarchy invalidation to speed
     * boot-time method definition.
     *
     * @param id The name to which to bind the method
     * @param method The method to bind
     * @deprecated No longer used, internal API!
     */
    public final void addMethodAtBootTimeOnly(String id, DynamicMethod method) {
        putMethod(getRuntime(), id, method);
    }

    public void removeMethod(ThreadContext context, String id) {
        testFrozen("class/module");

        switch (id) {
            case "object_id"  : warnMethodRemoval(context, id); break;
            case "__send__"   : warnMethodRemoval(context, id); break;
            case "initialize" : warnMethodRemoval(context, id); break;
        }

        RubySymbol name = context.runtime.newSymbol(id);
        // We can safely reference methods here instead of doing getMethods() since if we
        // are adding we are not using a IncludedModule.
        Map<String, DynamicMethod> methodsForWrite = methodLocation.getMethodsForWrite();
        synchronized (methodsForWrite) {
            DynamicMethod method = methodsForWrite.get(id);
            if (method == null ||
                    method.isUndefined() ||
                    method instanceof RefinedMarker) {
                throw context.runtime.newNameError(str(context.runtime, "method '", name, "' not defined in ", rubyName()), id);
            }

            method = methodsForWrite.remove(id);

            if (method.isRefined()) {
                methodsForWrite.put(id, new RefinedMarker(method.getImplementationClass(), method.getVisibility(), id));
            }

            invalidateCoreClasses();
            invalidateCacheDescendants();
        }

        if (isSingleton()) {
            ((MetaClass) this).getAttached().callMethod(context, "singleton_method_removed", name);
        } else {
            callMethod(context, "method_removed", name);
        }
    }

    private static void warnMethodRemoval(final ThreadContext context, final String id) {
        context.runtime.getWarnings().warn(ID.UNDEFINING_BAD,
                str(context.runtime, "removing `", ids(context.runtime, id), "' may cause serious problems"));
    }

    /**
     * Search through this module and supermodules for method definitions. Cache superclass definitions in this class.
     *
     * @param name The name of the method to search for
     * @return The method, or UndefinedMethod if not found
     */
    public final DynamicMethod searchMethod(String name) {
        return searchWithCache(name).method;
    }

    /**
     * Search for the named method in this class and in superclasses, and if found return the CacheEntry representing
     * the method and this class's serial number.
     *
     * MRI: method_entry_get
     *
     * @param name the method name
     * @return the CacheEntry corresponding to the method and this class's serial number
     */
    public CacheEntry searchWithCache(String name) {
        return searchWithCacheAndRefinements(name, true, null);
    }

    /**
     * Search for the named method in this class and in superclasses applying refinements from the given scope. If
     * found return the method; otherwise, return UndefinedMethod.
     *
     * @param name the method name
     * @param refinedScope the scope containing refinements to search
     * @return the method or UndefinedMethod
     */
    public CacheEntry searchWithRefinements(String name, StaticScope refinedScope) {
        return searchWithCacheAndRefinements(name, true, refinedScope);
    }

    /**
     * Search through this module and supermodules for method definitions. Cache superclass definitions in this class.
     *
     * MRI: method_entry_get
     *
     * @param id The name of the method to search for
     * @param cacheUndef Flag for caching UndefinedMethod. This should normally be true.
     * @return The method, or UndefinedMethod if not found
     */
    public final CacheEntry searchWithCache(String id, boolean cacheUndef) {
        final CacheEntry entry = cacheHit(id);
        return entry != null ? entry : searchWithCacheMiss(getRuntime(), id, cacheUndef);
    }

    // MRI: method_entry_resolve_refinement
    private final CacheEntry searchWithCacheAndRefinements(String id, boolean cacheUndef, StaticScope refinedScope) {
        CacheEntry entry = searchWithCache(id, cacheUndef);

        if (entry.method.isRefined()) {
            // FIXME: We walk up scopes to look for refinements, while MRI seems to copy from parent to child on push
            // CON: Walk improved to only walk up to nearest refined scope, since methods/classes/modules will copy parent's
            for (; refinedScope != null; refinedScope = refinedScope.getEnclosingScope()) {
                // any refined target with scope available
                RubyModule overlay = refinedScope.getOverlayModuleForRead();

                if (overlay == null) continue;

                CacheEntry maybeEntry = resolveRefinedMethod(overlay.refinements, entry, id, cacheUndef);

                if (maybeEntry.method.isUndefined()) continue;

                return maybeEntry;
            }

            // MRI: refined_method_original_method_entry
            return resolveRefinedMethod(null, entry, id, cacheUndef);
        }

        return entry;
    }

    // MRI: refined_method_original_method_entry
    private CacheEntry refinedMethodOriginalMethodEntry(Map<RubyModule, RubyModule> refinements, String id, boolean cacheUndef, CacheEntry entry) {
        RubyModule superClass;

        DynamicMethod method = entry.method;

        // unwrap delegated methods so we can see them properly
        if (method instanceof DelegatingDynamicMethod) {
            method = ((DelegatingDynamicMethod) method).getDelegate();
        }

        if (method instanceof RefinedWrapper){
            // original without refined flag
            return cacheEntryFactory.newCacheEntry(id, ((RefinedWrapper) method).getWrapped(), entry.sourceModule, entry.token);
        } else if ((superClass = entry.sourceModule.getSuperClass()) == null) {
            // marker with no scope and no super, no method
            return CacheEntry.NULL_CACHE;
        } else {
            // marker with no scope available, find super method
            return resolveRefinedMethod(refinements, superClass.searchWithCache(id, cacheUndef), id, cacheUndef);
        }
    }

    /**
     * Search through this module and supermodules for method definitions after {@link RubyModule#cacheHit(String)}
     * failed to return a result. Cache superclass definitions in this class.
     *
     * MRI: method_entry_get_without_cache
     * 
     * @param id The name of the method to search for
     * @param cacheUndef Flag for caching UndefinedMethod. This should normally be true.
     * @return The method, or UndefinedMethod if not found
     */
    private CacheEntry searchWithCacheMiss(Ruby runtime, final String id, final boolean cacheUndef) {
        // we grab serial number first; the worst that will happen is we cache a later
        // update with an earlier serial number, which would just flush anyway
        final int token = generation;

        CacheEntry methodEntry = searchMethodEntryInner(id);

        if (methodEntry == null) {
            if (cacheUndef) {
                return addToCache(id, UndefinedMethod.getInstance(), this, token);
            }
            return cacheEntryFactory.newCacheEntry(id, UndefinedMethod.getInstance(), methodEntry.sourceModule, token);
        } else if (!runtime.isBooting()) {
            addToCache(id, methodEntry);
        }

        return methodEntry;
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
            this.cachedMethods = new ConcurrentHashMap<>(0, 0.75f, 1) :
            myCachedMethods;
    }

    private CacheEntry cacheHit(String name) {
        CacheEntry cacheEntry = methodLocation.getCachedMethods().get(name);

        if (cacheEntry != null) {
            if (cacheEntry.token == getGeneration()) {
                return cacheEntry;
            }
        }

        return null;
    }

    private void invalidateConstantCacheForModuleInclusion(RubyModule module) {
        Map<String, Invalidator> invalidators = null;
        for (RubyModule mod : gatherModules(module)) {
            for (String name : mod.getConstantMap().keySet()) {
                if (invalidators == null) invalidators = new HashMap<>();
                invalidators.put(name, getRuntime().getConstantInvalidator(name));
            }
        }
        if (invalidators != null) {
            List<Invalidator> values = new ArrayList(invalidators.values());
            values.get(0).invalidateAll(values);
        }
    }

    protected static abstract class CacheEntryFactory {
        public abstract CacheEntry newCacheEntry(String id, DynamicMethod method, RubyModule sourceModule, int token);

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
        public CacheEntry newCacheEntry(String id, DynamicMethod method, RubyModule sourceModule, int token) {
            return new CacheEntry(method, sourceModule, token);
        }
    };

    protected static class SynchronizedCacheEntryFactory extends WrapperCacheEntryFactory {
        public SynchronizedCacheEntryFactory(CacheEntryFactory previous) {
            super(previous);
        }
        @Override
        public CacheEntry newCacheEntry(String id, DynamicMethod method, RubyModule sourceModule, int token) {
            if (method.isUndefined()) {
                return new CacheEntry(method, token);
            }
            // delegate up the chain
            CacheEntry delegated = previous.newCacheEntry(id, method, sourceModule, token);
            return new CacheEntry(new SynchronizedDynamicMethod(delegated.method), delegated.sourceModule, delegated.token);
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
        public CacheEntry newCacheEntry(String id, DynamicMethod method, RubyModule sourceModule, int token) {
            if (method.isUndefined()) return new CacheEntry(method, token);

            CacheEntry delegated = previous.newCacheEntry(id, method, sourceModule, token);
            DynamicMethod enhancedMethod = getMethodEnhancer().enhance(id, delegated.method);

            return new CacheEntry(enhancedMethod, delegated.sourceModule, delegated.token);
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

    protected CacheEntry addToCache(String id, DynamicMethod method, RubyModule sourceModule, int token) {
        CacheEntry entry = cacheEntryFactory.newCacheEntry(id, method, sourceModule, token);

        methodLocation.getCachedMethodsForWrite().put(id, entry);

        return entry;
    }

    protected CacheEntry addToCache(String id, CacheEntry entry) {
        methodLocation.getCachedMethodsForWrite().put(id, entry);

        return entry;
    }

    // MRI: search_method
    public DynamicMethod searchMethodInner(String id) {
        // This flattens some of the recursion that would be otherwise be necessary.
        // Used to recurse up the class hierarchy which got messy with prepend.
        for (RubyModule module = this; module != null; module = module.getSuperClass()) {
            // Only recurs if module is an IncludedModuleWrapper.
            // This way only the recursion needs to be handled differently on
            // IncludedModuleWrapper.
            DynamicMethod method = module.searchMethodCommon(id);
            if (method != null) return method.isNull() ? null : method;
        }
        return null;
    }

    public CacheEntry searchMethodEntryInner(String id) {
        int token = generation;
        // This flattens some of the recursion that would be otherwise be necessary.
        // Used to recurse up the class hierarchy which got messy with prepend.
        for (RubyModule module = this; module != null; module = module.getSuperClass()) {
            // Only recurs if module is an IncludedModuleWrapper.
            // This way only the recursion needs to be handled differently on
            // IncludedModuleWrapper.
            DynamicMethod method = module.searchMethodCommon(id);
            if (method != null) return method.isNull() ? null : cacheEntryFactory.newCacheEntry(id, method, module, token);
        }
        return null;
    }

    /**
     * Searches for a method up until the superclass, but include modules. This is
     * for Concrete java ctor initialization
     * TODO: add a cache?
     */
    public DynamicMethod searchMethodLateral(String id) {
       // int token = generation;
        // This flattens some of the recursion that would be otherwise be necessary.
        // Used to recurse up the class hierarchy which got messy with prepend.
        for (RubyModule module = this; module != null && (module == this || (module instanceof IncludedModuleWrapper)); module = module.getSuperClass()) {
            // Only recurs if module is an IncludedModuleWrapper.
            // This way only the recursion needs to be handled differently on
            // IncludedModuleWrapper.
            DynamicMethod method = module.searchMethodCommon(id);
            if (method != null) return method.isNull() ? null : method;
        }
        return null;
    }

    // MRI: resolve_refined_method
    public CacheEntry resolveRefinedMethod(Map<RubyModule, RubyModule> refinements, CacheEntry entry, String id, boolean cacheUndef) {
        if (entry != null && entry.method.isRefined()) {
            // Check for refinements in the given scope
            RubyModule refinement = findRefinement(refinements, entry.method.getDefinedClass());

            if (refinement == null) {
                return refinedMethodOriginalMethodEntry(refinements, id, cacheUndef, entry);
            } else {
                CacheEntry tmpEntry = refinement.searchWithCache(id);
                if (!tmpEntry.method.isRefined()) {
                    return tmpEntry;
                } else {
                    return refinedMethodOriginalMethodEntry(refinements, id, cacheUndef, entry);
                }
            }
        }

        return entry;
    }

    // MRI: find_refinement
    private static RubyModule findRefinement(Map<RubyModule, RubyModule> refinements, RubyModule target) {
        if (refinements == null) {
            return null;
        }
        return refinements.get(target);
    }

    // The local method resolution logic. Overridden in IncludedModuleWrapper for recursion.
    protected DynamicMethod searchMethodCommon(String id) {
        return getMethods().get(id);
    }

    public void invalidateCacheDescendants() {
        LOG.debug("{} invalidating descendants", baseName);

        getRuntime().getCaches().incrementMethodInvalidations();

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

    @SuppressWarnings("deprecation")
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
        updateGeneration(getRuntime());
    }

    private void updateGeneration(final Ruby runtime) {
        generationObject = generation = runtime.getNextModuleGeneration();
    }

    @Deprecated
    protected void invalidateCacheDescendantsInner() {
        methodInvalidator.invalidate();
    }

    protected void invalidateConstantCache(String constantName) {
        getRuntime().getConstantInvalidator(constantName).invalidate();
    }

    protected void invalidateConstantCaches(Set<String> constantNames) {
        if (constantNames.size() > 0) {
            Ruby runtime = getRuntime();

            List<Invalidator> constantInvalidators = new ArrayList<>(constantNames.size());
            for (String name : constantNames) {
                constantInvalidators.add(runtime.getConstantInvalidator(name));
            }

            constantInvalidators.get(0).invalidateAll(constantInvalidators);
        }
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

        putAlias(name, searchForAliasMethod(getRuntime(), oldName), oldName);

        methodLocation.invalidateCoreClasses();
        methodLocation.invalidateCacheDescendants();
    }

    /**
     * @note Internal API - only public as its used by generated code!
     * @note Used by AnnotationBinder.
     * @note Not safe for target methods that super, since the frame class will not reflect original source.
     * @param id
     * @param method
     * @param oldName
     */
    public void putAlias(String id, DynamicMethod method, String oldName) {
        if (id.equals(oldName)) return;

        putMethod(getRuntime(), id, new AliasMethod(this, new CacheEntry(method, method.getImplementationClass(), generation), oldName));

        if (isRefinement()) {
            addRefinedMethodEntry(id, method);
        }
    }

    /**
     * Alias the method contained in the given CacheEntry as a new entry in this module.
     *
     * @param id
     * @param entry
     * @param oldName
     */
    public void putAlias(String id, CacheEntry entry, String oldName) {
        if (id.equals(oldName)) return;

        putMethod(getRuntime(), id, new AliasMethod(this, entry, oldName));

        if (isRefinement()) {
            addRefinedMethodEntry(id, entry.method);
        }
    }

    public synchronized void defineAliases(List<String> aliases, String oldId) {
        testFrozen("module");

        Ruby runtime = getRuntime();
        CacheEntry entry = searchForAliasMethod(runtime, oldId);

        for (String name: aliases) {
            putAlias(name, entry, oldId);
        }

        methodLocation.invalidateCoreClasses();
        methodLocation.invalidateCacheDescendants();
    }

    private CacheEntry searchForAliasMethod(Ruby runtime, String id) {
        CacheEntry entry = deepMethodSearch(id, runtime);
        final DynamicMethod method = entry.method;

        if (method instanceof NativeCallMethod) {
            // JRUBY-2435: Aliasing eval and other "special" methods should display a warning
            // We warn because we treat certain method names as "special" for purposes of
            // optimization. Hopefully this will be enough to convince people not to alias
            // them.

            DynamicMethod.NativeCall nativeCall = ((NativeCallMethod) method).getNativeCall();

            // native-backed but not a direct call, ok
            if (nativeCall == null) return entry;

            Method javaMethod = nativeCall.getMethod();
            JRubyMethod anno = javaMethod.getAnnotation(JRubyMethod.class);

            if (anno == null) return entry;

            if (anno.reads().length > 0 || anno.writes().length > 0) {

                MethodIndex.addMethodReadFields(id, anno.reads());
                MethodIndex.addMethodWriteFields(id, anno.writes());

                if (runtime.isVerbose()) {
                    String baseName = getBaseName();
                    char refChar = '#';
                    String simpleName = getSimpleName();

                    if (baseName == null && this instanceof MetaClass) {
                        IRubyObject attached = ((MetaClass) this).getAttached();
                        if (attached instanceof RubyModule) {
                            simpleName = ((RubyModule) attached).getSimpleName();
                            refChar = '.';
                        }
                    }

                    runtime.getWarnings().warning(simpleName + refChar + id + " accesses caller method's state and should not be aliased");
                }
            }
        }

        return entry;
    }

    /** this method should be used only by interpreter or compiler
     *
     */
    public RubyClass defineOrGetClassUnder(String name, RubyClass superClazz) {
        return defineOrGetClassUnder(name, superClazz, null);
    }

    public RubyClass defineOrGetClassUnder(String name, RubyClass superClazz, ObjectAllocator allocator) {
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
                if (tmp != superClazz) throw runtime.newTypeError(str(runtime, "superclass mismatch for class ", ids(runtime, name)));
                // superClazz = null;
            }
        } else if ((clazz = searchProvidersForClass(name, superClazz)) != null) {
            // reopen a java class
        } else {
            if (superClazz == null) superClazz = runtime.getObject();

            if (allocator == null) {
                if (isReifiable(runtime, superClazz)) {
                    if (Options.REIFY_CLASSES.load()) {
                        allocator = REIFYING_OBJECT_ALLOCATOR;
                    } else if (Options.REIFY_VARIABLES.load()) {
                        allocator = IVAR_INSPECTING_OBJECT_ALLOCATOR;
                    } else {
                        allocator = OBJECT_ALLOCATOR;
                    }
                } else {
                    allocator = superClazz.getAllocator();
                }
            }

            clazz = RubyClass.newClass(runtime, superClazz, name, allocator, this, true);
        }

        return clazz;
    }

    /**
     * Determine if a new child of the given class can have its variables reified.
     */
    private boolean isReifiable(Ruby runtime, RubyClass superClass) {
        if (superClass == runtime.getObject()) return true;

        if (superClass.getAllocator() == IVAR_INSPECTING_OBJECT_ALLOCATOR) return true;

        return false;
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
            if (!moduleObj.isModule()) throw runtime.newTypeError(str(runtime, ids(runtime, name), " is not a module"));
            module = (RubyModule)moduleObj;
        } else if ((module = searchProvidersForModule(name)) != null) {
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
        return superClass.runtime.defineClassUnder(name, superClass, allocator, this);
    }

    /** rb_define_module_under
     *  this method should be used only as an API to define/open nested module
     */
    public RubyModule defineModuleUnder(String name) {
        return metaClass.runtime.defineModuleUnder(name, this);
    }

    private void addAccessor(ThreadContext context, RubySymbol identifier, Visibility visibility, boolean readable, boolean writeable) {
        String internedIdentifier = identifier.idString();

        final Ruby runtime = context.runtime;

        if (visibility == MODULE_FUNCTION) {
            runtime.getWarnings().warn(ID.ACCESSOR_MODULE_FUNCTION, "attribute accessor as module_function");
            visibility = PRIVATE;
        }

        if (!identifier.validLocalVariableName() && !identifier.validConstantName()) {
            throw runtime.newNameError("invalid attribute name", identifier);
        }

        final String variableName = identifier.asInstanceVariable().idString();
        if (readable) {
            addMethod(internedIdentifier, new AttrReaderMethod(methodLocation, visibility, variableName));
            callMethod(context, "method_added", identifier);
        }
        if (writeable) {
            identifier = identifier.asWriter();
            addMethod(identifier.idString(), new AttrWriterMethod(methodLocation, visibility, variableName));
            callMethod(context, "method_added", identifier);
        }
    }

    /** set_method_visibility
     *
     */
    public void setMethodVisibility(IRubyObject[] methods, Visibility visibility) {
        for (int i = 0; i < methods.length; i++) {
            exportMethod(TypeConverter.checkID(methods[i]).idString(), visibility);
        }
    }

    /** rb_export_method
     *
     */
    public void exportMethod(String name, Visibility visibility) {
        Ruby runtime = getRuntime();

        CacheEntry entry = methodLocation.deepMethodSearch(name, runtime);
        DynamicMethod method = entry.method;

        if (method.getVisibility() != visibility) {
            if (this == method.getImplementationClass()) {
                method.setVisibility(visibility);
            } else {
                DynamicMethod newMethod = new PartialDelegatingMethod(this, method, visibility);

                methodLocation.addMethod(name, newMethod);
            }

            invalidateCoreClasses();
            invalidateCacheDescendants();
        }
    }

    private CacheEntry deepMethodSearch(String id, Ruby runtime) {
        CacheEntry orig = searchWithCache(id);
        if (orig.method.isRefined()) {
            orig = resolveRefinedMethod(null, orig, id, true);
        }

        if (orig.method.isUndefined() || orig.method.isRefined()) {
            if (!isModule()
                    || (orig = runtime.getObject().searchWithCache(id)).method.isUndefined()) {
                // FIXME: Do we potentially leak symbols here if they do not exist?
                RubySymbol name = runtime.newSymbol(id);
                throw runtime.newNameError(undefinedMethodMessage(runtime, name, rubyName(), isModule()), name);
            }
        }

        return orig;
    }

    public static String undefinedMethodMessage(Ruby runtime, IRubyObject name, IRubyObject modName, boolean isModule) {
        return str(runtime, "undefined method `", name, "' for " + (isModule ? "module" : "class") + " `", modName, "'");
    }

    /**
     * MRI: rb_method_boundp
     *
     */
    public boolean isMethodBound(String name, boolean checkVisibility) {
        DynamicMethod method = searchMethod(name);

        return !method.isUndefined() && !(checkVisibility && method.getVisibility() == PRIVATE);
    }

    public boolean respondsToMethod(String name, boolean checkVisibility, StaticScope scope) {
        return Helpers.respondsToMethod(searchWithRefinements(name, scope).method, checkVisibility);
    }

    public boolean respondsToMethod(String name, boolean checkVisibility) {
        return Helpers.respondsToMethod(searchMethod(name), checkVisibility);
    }

    @Deprecated
    public boolean isMethodBound(String name, boolean checkVisibility, boolean checkRespondTo) {
        return checkRespondTo ? respondsToMethod(name, checkVisibility): isMethodBound(name, checkVisibility);
    }

    public final IRubyObject newMethod(IRubyObject receiver, String methodName, boolean bound, Visibility visibility) {
        return newMethod(receiver, methodName, bound, visibility, false, true);
    }

    public final IRubyObject newMethod(IRubyObject receiver, final String methodName, boolean bound, Visibility visibility, boolean respondToMissing) {
        return newMethod(receiver, methodName, bound, visibility, respondToMissing, true);
    }

    public static class RespondToMissingMethod extends JavaMethod.JavaMethodNBlock {
        final String methodName;

        public RespondToMissingMethod(RubyModule implClass, Visibility visibility, String methodName) {
            super(implClass, visibility, methodName);

            setParameterList(REST);
            this.methodName = methodName;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            return Helpers.callMethodMissing(context, self, getVisibility(), name, CallType.UNKNOWN, args, block);
        }

        @Override
        public boolean equals(Object other) {
            return this == other ||
                    (other instanceof RespondToMissingMethod &&
                            ((RespondToMissingMethod) other).methodName.equals(methodName));
        }

        @Override
        public int hashCode() {
            return methodName.hashCode();
        }

    }

    public IRubyObject newMethod(IRubyObject receiver, final String methodName, boolean bound, Visibility visibility, boolean respondToMissing, boolean priv) {
        CacheEntry entry = searchWithCache(methodName);

        if (entry.method.isUndefined() || (visibility != null && entry.method.getVisibility() != visibility)) {
            if (respondToMissing) { // 1.9 behavior
                if (receiver.respondsToMissing(methodName, priv)) {
                    entry = new CacheEntry(
                            new RespondToMissingMethod(this, PUBLIC, methodName),
                            entry.sourceModule,
                            entry.token);
                } else {
                    throw getRuntime().newNameError("undefined method `" + methodName + "' for class `" + getName() + '\'', methodName);
                }
            } else {
                throw getRuntime().newNameError("undefined method `" + methodName + "' for class `" + getName() + '\'', methodName);
            }
        }

        RubyModule implementationModule = entry.method.getDefinedClass();
        RubyModule originModule = this;
        while (originModule != implementationModule && (originModule.isSingleton() || originModule.isIncluded())) {
            originModule = originModule.getSuperClass();
        }

        AbstractRubyMethod newMethod;
        if (bound) {
            newMethod = RubyMethod.newMethod(implementationModule, methodName, originModule, methodName, entry, receiver);
        } else {
            newMethod = RubyUnboundMethod.newUnboundMethod(implementationModule, methodName, originModule, methodName, entry);
        }
        newMethod.infectBy(this);

        return newMethod;
    }

    @JRubyMethod(name = "define_method", reads = VISIBILITY)
    public IRubyObject define_method(ThreadContext context, IRubyObject arg0, Block block) {
        Visibility visibility = getCurrentVisibilityForDefineMethod(context);

        return defineMethodFromBlock(context, arg0, block, visibility);
    }

    private Visibility getCurrentVisibilityForDefineMethod(ThreadContext context) {
        // These checks are similar to rb_vm_cref_in_context from MRI.
        return context.getCurrentFrame().getSelf() == this ? context.getCurrentVisibility() : PUBLIC;
    }

    public IRubyObject defineMethodFromBlock(ThreadContext context, IRubyObject arg0, Block block, Visibility visibility) {
        final Ruby runtime = context.runtime;
        RubySymbol name = TypeConverter.checkID(arg0);
        DynamicMethod newMethod;

        if (!block.isGiven()) throw runtime.newArgumentError("tried to create Proc object without a block");

        if ("initialize".equals(name.idString())) visibility = PRIVATE;

        // If we know it comes from IR we can convert this directly to a method and
        // avoid overhead of invoking it as a block
        if (block.getBody() instanceof IRBlockBody &&
                runtime.getInstanceConfig().getCompileMode().shouldJIT()) { // FIXME: Once Interp and Mixed Methods are one class we can fix this to work in interp mode too.
            IRBlockBody body = (IRBlockBody) block.getBody();
            IRClosure closure = body.getScope();

            // closure may be null from AOT scripts
            if (closure != null) {
                // Ask closure to give us a method equivalent.
                IRMethod method = closure.convertToMethod(name.getBytes());
                if (method != null) {
                    newMethod = new DefineMethodMethod(method, visibility, this, context.getFrameBlock());
                    Helpers.addInstanceMethod(this, name, newMethod, visibility, context, runtime);
                    return name;
                }
            }
        }

        newMethod = createProcMethod(runtime, name.idString(), visibility, block);
        Helpers.addInstanceMethod(this, name, newMethod, visibility, context, runtime);

        return name;
    }

    @JRubyMethod(name = "define_method", reads = VISIBILITY)
    public IRubyObject define_method(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        Visibility visibility = getCurrentVisibilityForDefineMethod(context);

        return defineMethodFromCallable(context, arg0, arg1, visibility);
    }

    public IRubyObject defineMethodFromCallable(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Visibility visibility) {
        final Ruby runtime = context.runtime;
        RubySymbol name = TypeConverter.checkID(arg0);
        DynamicMethod newMethod;

        if ("initialize".equals(name.idString())) visibility = PRIVATE;

        if (runtime.getProc().isInstance(arg1)) {
            // double-testing args.length here, but it avoids duplicating the proc-setup code in two places
            RubyProc proc = (RubyProc)arg1;

            newMethod = createProcMethod(runtime, name.idString(), visibility, proc.getBlock());
        } else if (arg1 instanceof AbstractRubyMethod) {
            AbstractRubyMethod method = (AbstractRubyMethod)arg1;

            checkValidBindTargetFrom(context, (RubyModule) method.owner(context), false);

            newMethod = method.getMethod().dup();
            newMethod.setImplementationClass(this);
            newMethod.setVisibility(visibility);
        } else {
            throw runtime.newTypeError("wrong argument type " + arg1.getType().getName() + " (expected Proc/Method)");
        }

        Helpers.addInstanceMethod(this, name, newMethod, visibility, context, runtime);

        return name;
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

    private DynamicMethod createProcMethod(Ruby runtime, String name, Visibility visibility, Block block) {
        block = block.cloneBlockAndFrame();

        block.getBinding().getFrame().setKlazz(this);
        block.getBinding().getFrame().setName(name);
        block.getBinding().setMethod(name);

        // a normal block passed to define_method changes to do arity checking; make it a lambda
        RubyProc proc = runtime.newProc(Block.Type.LAMBDA, block);

        // various instructions can tell this scope is not an ordinary block but a block representing
        // a method definition.
        block.getBody().getStaticScope().makeArgumentScope();

        return new ProcMethod(this, proc, visibility, name);
    }

    public IRubyObject name() {
        return name(getRuntime().getCurrentContext());
    }

    @JRubyMethod(name = "name")
    public IRubyObject name(ThreadContext context) {
        return getBaseName() == null ? context.nil : rubyName().strDup(context.runtime);
    }

    @Deprecated
    public IRubyObject name19() {
        return getBaseName() == null ? getRuntime().getNil() : rubyName().strDup(getRuntime());
    }

    protected final IRubyObject cloneMethods(RubyModule clone) {
        Ruby runtime = getRuntime();
        RubyModule realType = this.getOrigin();
        for (Map.Entry<String, DynamicMethod> entry : getMethods().entrySet()) {
            DynamicMethod method = entry.getValue();
            // Do not clone cached methods
            // FIXME: MRI copies all methods here
            if (method.isImplementedBy(realType) || method.isUndefined()) {

                // A cloned method now belongs to a new class.  Set it.
                // TODO: Make DynamicMethod immutable
                DynamicMethod clonedMethod = method.dup();
                clonedMethod.setImplementationClass(clone);
                clone.putMethod(runtime, entry.getKey(), clonedMethod);
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

        if (!getMetaClass().isSingleton()) {
            setMetaClass(originalModule.getSingletonClassCloneAndAttach(this));
        }
        setSuperClass(originalModule.getSuperClass());
        if (originalModule.hasVariables()) syncVariables(originalModule);
        syncConstants(originalModule);

        originalModule.cloneMethods(this);
        
        this.javaProxy = originalModule.javaProxy; 

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
        Map<String, Autoload> autoloadMap = other.getAutoloadMap();
        if (!autoloadMap.isEmpty()) {
            this.autoloads = autoloadMap;
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
                ary.append(p.getOrigin());
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
            if (module.methodLocation == module) list.add(module.getDelegate().getOrigin());
        }

        return list;
    }

    public boolean hasModuleInPrepends(RubyModule type) {
        RubyModule stopClass = getPrependCeiling();
        for (RubyModule module = this; module != stopClass; module = module.getSuperClass()) {
            if (type == module.getOrigin()) return true;
        }
        return false;
    }

    private RubyModule getPrependCeiling() {
        RubyClass mlSuper = methodLocation.getSuperClass();
        return mlSuper == null ? methodLocation : mlSuper.getRealClass();
    }

    public boolean hasModuleInHierarchy(RubyModule type) {
        // XXX: This check previously used callMethod("==") to check for equality between classes
        // when scanning the hierarchy. However the == check may be safe; we should only ever have
        // one instance bound to a given type/constant. If it's found to be unsafe, examine ways
        // to avoid the == call.
        for (RubyModule module = this; module != null; module = module.getSuperClass()) {
            if (module.getOrigin() == type) return true;
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
    public RubyString to_s() {
        Ruby runtime = getRuntime();
        if (isSingleton()) {
            IRubyObject attached = ((MetaClass) this).getAttached();
            RubyString buffer = runtime.newString("#<Class:");

            if (attached instanceof RubyModule) {
                buffer.cat19(attached.inspect().convertToString());
            } else if (attached != null) {
                buffer.cat19((RubyString) attached.anyToString());
            }
            buffer.cat('>', buffer.getEncoding());

            return buffer;
        }

        RubyModule refinedClass = this.refinedClass;

        if (refinedClass != null) {
            RubyString buffer = runtime.newString("#<refinement:");

            buffer.cat19(refinedClass.inspect().convertToString());
            buffer.cat('@', buffer.getEncoding());
            buffer.cat19((definedAt.inspect().convertToString()));
            buffer.cat('>', buffer.getEncoding());

            return buffer;
        }

        return rubyName().strDup(runtime);
    }

    /** rb_mod_eqq
     *
     */
    @JRubyMethod(name = "===", required = 1)
    @Override
    public RubyBoolean op_eqq(ThreadContext context, IRubyObject obj) {
        return RubyBoolean.newBoolean(context, isInstance(obj));
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
        if(!(other instanceof RubyModule)) return context.fals;

        RubyModule otherModule = (RubyModule) other;
        if(otherModule.isIncluded()) {
            return RubyBoolean.newBoolean(context, otherModule.isSame(this));
        } else {
            return RubyBoolean.newBoolean(context, isSame(otherModule));
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

    /**
    * MRI: rb_class_inherited_p
    */
    @JRubyMethod(name = "<=", required = 1)
    public IRubyObject op_le(IRubyObject arg) {
        Ruby runtime = getRuntime();

        if (!(arg instanceof RubyModule)) {
            throw runtime.newTypeError("compared with non class/module");
        }

        RubyModule argMod = (RubyModule) arg;

        if (searchAncestor(argMod.getMethodLocation()) != null) {
            return runtime.getTrue();
        }

        /* not mod < arg; check if mod > arg */
        if (argMod.searchAncestor(this) != null) {
            return runtime.getFalse();
        }

        return runtime.getNil();
    }

    // MRI: class_search_ancestor
    protected RubyModule searchAncestor(RubyModule c) {
        RubyModule cl = this;
        while (cl != null) {
            if (cl == c || cl.isSame(c) || cl.getDelegate().getOrigin() == c) {
                return cl;
            }
            cl = cl.getSuperClass();
        }
        return null;
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

    protected boolean isSameOrigin(RubyModule other) {
        return getOrigin() == other.getOrigin();
    }

    /** rb_mod_initialize
     *
     */
    @JRubyMethod(name = "initialize", visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, Block block) {
        if (block.isGiven()) {
            module_exec(context, new IRubyObject[] {this}, block);
        }

        return context.nil;
    }

    public void addReadWriteAttribute(ThreadContext context, String name) {
        addAccessor(context, TypeConverter.checkID(context.runtime, name), PUBLIC, true, true);
    }

    public void addReadAttribute(ThreadContext context, String name) {
        addAccessor(context, TypeConverter.checkID(context.runtime, name), PUBLIC, true, false);
    }

    public void addWriteAttribute(ThreadContext context, String name) {
        addAccessor(context, TypeConverter.checkID(context.runtime, name), PUBLIC, false, true);
    }

    /** rb_mod_attr
     *
     */
    @JRubyMethod(name = "attr", rest = true, reads = VISIBILITY)
    public IRubyObject attr(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;

        if (args.length == 2 && (args[1] == runtime.getTrue() || args[1] == runtime.getFalse())) {
            runtime.getWarnings().warning(ID.OBSOLETE_ARGUMENT, "optional boolean argument is obsoleted");
            addAccessor(context, TypeConverter.checkID(args[0]), context.getCurrentVisibility(), args[0].isTrue(), args[1].isTrue());
            return runtime.getNil();
        }

        return attr_reader(context, args);
    }

    @Deprecated
    public IRubyObject attr19(ThreadContext context, IRubyObject[] args) {
        return attr(context, args);
    }

    @Deprecated
    public IRubyObject attr_reader(IRubyObject[] args) {
        return attr_reader(getRuntime().getCurrentContext(), args);
    }

    /** rb_mod_attr_reader
     *
     */
    @JRubyMethod(name = "attr_reader", rest = true, reads = VISIBILITY)
    public IRubyObject attr_reader(ThreadContext context, IRubyObject[] args) {
        // Check the visibility of the previous frame, which will be the frame in which the class is being eval'ed
        Visibility visibility = context.getCurrentVisibility();

        for (int i = 0; i < args.length; i++) {
            addAccessor(context, TypeConverter.checkID(args[i]), visibility, true, false);
        }

        return context.nil;
    }

    /** rb_mod_attr_writer
     *
     */
    @JRubyMethod(name = "attr_writer", rest = true, reads = VISIBILITY)
    public IRubyObject attr_writer(ThreadContext context, IRubyObject[] args) {
        // Check the visibility of the previous frame, which will be the frame in which the class is being eval'ed
        Visibility visibility = context.getCurrentVisibility();

        for (int i = 0; i < args.length; i++) {
            addAccessor(context, TypeConverter.checkID(args[i]), visibility, false, true);
        }

        return context.nil;
    }


    @Deprecated
    public IRubyObject attr_accessor(IRubyObject[] args) {
        return attr_accessor(getRuntime().getCurrentContext(), args);
    }

    /** rb_mod_attr_accessor
     *  Note: this method should not be called from Java in most cases, since
     *  it depends on Ruby frame state for visibility. Use add[Read/Write]Attribute instead.
     */
    @JRubyMethod(name = "attr_accessor", rest = true, reads = VISIBILITY)
    public IRubyObject attr_accessor(ThreadContext context, IRubyObject[] args) {
        // Check the visibility of the previous frame, which will be the frame in which the class is being eval'ed
        Visibility visibility = context.getCurrentVisibility();

        for (int i = 0; i < args.length; i++) {
            // This is almost always already interned, since it will be called with a symbol in most cases
            // but when created from Java code, we might getService an argument that needs to be interned.
            // addAccessor has as a precondition that the string MUST be interned
            addAccessor(context, TypeConverter.checkID(args[i]), visibility, true, true);
        }

        return context.nil;
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
        RubyArray ary = getRuntime().newArray();

        populateInstanceMethodNames(new HashSet<>(), ary, visibility, obj, not, includeSuper);

        return ary;
    }

    final void populateInstanceMethodNames(final Set<String> seen, final RubyArray ary, Visibility visibility,
                                           boolean obj, boolean not, boolean recur) {
        Ruby runtime = getRuntime();
        RubyModule mod = this;
        boolean prepended = false;

        if (!recur && methodLocation != this) {
            mod = methodLocation;
            prepended = true;
        }

        for (; mod != null; mod = mod.getSuperClass()) {
            mod.addMethodSymbols(runtime, seen, ary, not, visibility);

            if (!prepended && mod.isIncluded()) continue;
            if (obj && mod.isSingleton()) continue;
            if (!recur) break;
        }
    }

    protected void addMethodSymbols(Ruby runtime, Set<String> seen, RubyArray ary, boolean not, Visibility visibility) {
        getMethods().forEach((id, method) -> {
            if (method instanceof RefinedMarker) return;

            if (seen.add(id)) { // false - not added (already seen)
                if ((!not && method.getVisibility() == visibility || (not && method.getVisibility() != visibility))
                        && !method.isUndefined()) {
                    ary.append(runtime.newSymbol(id));
                }
            }
        });
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
        return newMethod(null, TypeConverter.checkID(symbol).idString(), false, null);
    }

    @JRubyMethod(name = "public_instance_method", required = 1)
    public IRubyObject public_instance_method(IRubyObject symbol) {
        return newMethod(null, TypeConverter.checkID(symbol).idString(), false, PUBLIC);
    }

    /** rb_class_protected_instance_methods
     *
     */
    @JRubyMethod(name = "protected_instance_methods", optional = 1)
    public RubyArray protected_instance_methods(IRubyObject[] args) {
        return instanceMethods(args, PROTECTED, false, false);
    }

    @Deprecated
    public RubyArray protected_instance_methods19(IRubyObject[] args) {
        return protected_instance_methods(args);
    }

    /** rb_class_private_instance_methods
     *
     */
    @JRubyMethod(name = "private_instance_methods", optional = 1)
    public RubyArray private_instance_methods(IRubyObject[] args) {
        return instanceMethods(args, PRIVATE, false, false);
    }

    @Deprecated
    public RubyArray private_instance_methods19(IRubyObject[] args) {
        return private_instance_methods(args);
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
        if (!isModule()) {
            throw getRuntime().newTypeError(this, getRuntime().getModule());
        }
        obj.getSingletonClass().includeModule(this);
        return obj;
    }

    /** rb_mod_include
     *
     */
    @JRubyMethod(name = "include", required = 1, rest = true)
    public RubyModule include(IRubyObject[] modules) {
        ThreadContext context = metaClass.runtime.getCurrentContext();
        // MRI checks all types first:
        for (int i = modules.length; --i >= 0; ) {
            IRubyObject module = modules[i];
            if ( ! module.isModule() ) {
                throw context.runtime.newTypeError(module, context.runtime.getModule());
            }
        }
        for (int i = modules.length - 1; i >= 0; i--) {
            IRubyObject module = modules[i];
            module.callMethod(context, "append_features", this);
            module.callMethod(context, "included", this);
        }

        return this;
    }

    @JRubyMethod(name = "include", required = 1) // most common path: include Enumerable
    public RubyModule include(ThreadContext context, IRubyObject module) {
        if (!module.isModule()) {
            throw context.runtime.newTypeError(module, context.runtime.getModule());
        }
        module.callMethod(context, "append_features", this);
        module.callMethod(context, "included", this);
        return this;
    }

    @JRubyMethod(name = "included", required = 1, visibility = PRIVATE)
    public IRubyObject included(ThreadContext context, IRubyObject other) {
        return context.nil;
    }

    @JRubyMethod(name = "extended", required = 1, visibility = PRIVATE)
    public IRubyObject extended(ThreadContext context, IRubyObject other, Block block) {
        return context.nil;
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
        RubyHash methodNames;

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
            String id = entry.getKey();
            IRubyObject mapped = methodNames.fastARef(runtime.newSymbol(id));
            if (mapped == NEVER) {
                // unmapped
            } else if (mapped == context.nil) {
                // do not mix
                continue;
            } else {
                id = TypeConverter.checkID(mapped).idString();
            }
            methodLocation.getMethodsForWrite().put(id, entry.getValue().dup());
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
        checkFrozen();
        setVisibility(context, args, PUBLIC);
        return this;
    }

    /** rb_mod_protected
     *
     */
    @JRubyMethod(name = "protected", rest = true, visibility = PRIVATE, writes = VISIBILITY)
    public RubyModule rbProtected(ThreadContext context, IRubyObject[] args) {
        checkFrozen();
        setVisibility(context, args, PROTECTED);
        return this;
    }

    /** rb_mod_private
     *
     */
    @JRubyMethod(name = "private", rest = true, visibility = PRIVATE, writes = VISIBILITY)
    public RubyModule rbPrivate(ThreadContext context, IRubyObject[] args) {
        checkFrozen();
        setVisibility(context, args, PRIVATE);
        return this;
    }

    /** rb_mod_modfunc
     *
     */
    @JRubyMethod(name = "module_function", rest = true, visibility = PRIVATE, writes = VISIBILITY)
    public RubyModule module_function(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;

        if (!isModule()) {
            throw context.runtime.newTypeError("module_function must be called for modules");
        }

        if (args.length == 0) {
            context.setCurrentVisibility(MODULE_FUNCTION);
        } else {
            setMethodVisibility(args, PRIVATE);

            for (int i = 0; i < args.length; i++) {
                RubySymbol name = TypeConverter.checkID(args[i]);
                DynamicMethod newMethod = deepMethodSearch(name.idString(), runtime).method.dup();
                newMethod.setImplementationClass(getSingletonClass());
                newMethod.setVisibility(PUBLIC);
                getSingletonClass().addMethod(name.idString(), newMethod);
                callMethod(context, "singleton_method_added", name);
            }
        }
        return this;
    }

    @JRubyMethod(name = "method_added", required = 1, visibility = PRIVATE)
    public IRubyObject method_added(ThreadContext context, IRubyObject nothing) {
        return context.nil;
    }

    @JRubyMethod(name = "method_removed", required = 1, visibility = PRIVATE)
    public IRubyObject method_removed(ThreadContext context, IRubyObject nothing) {
        return context.nil;
    }

    @JRubyMethod(name = "method_undefined", required = 1, visibility = PRIVATE)
    public IRubyObject method_undefined(ThreadContext context, IRubyObject nothing) {
        return context.nil;
    }

    @JRubyMethod(name = "method_defined?")
    public RubyBoolean method_defined_p(ThreadContext context, IRubyObject symbol) {
        return isMethodBound(TypeConverter.checkID(symbol).idString(), true) ? context.tru : context.fals;
    }

    @JRubyMethod(name = "method_defined?")
    public RubyBoolean method_defined_p(ThreadContext context, IRubyObject symbol, IRubyObject includeSuper) {
        boolean parents = includeSuper.isTrue();

        if (parents) return method_defined_p(context, symbol);

        Visibility visibility = checkMethodVisibility(context, symbol, parents);
        return RubyBoolean.newBoolean(context, visibility != UNDEFINED && visibility != PRIVATE);
    }

    @JRubyMethod(name = "public_method_defined?")
    public IRubyObject public_method_defined(ThreadContext context, IRubyObject symbol) {
        return RubyBoolean.newBoolean(context, checkMethodVisibility(context, symbol, true) == PUBLIC);
    }

    @JRubyMethod(name = "public_method_defined?")
    public IRubyObject public_method_defined(ThreadContext context, IRubyObject symbol, IRubyObject includeSuper) {
        boolean parents = includeSuper.isTrue();

        return RubyBoolean.newBoolean(context, checkMethodVisibility(context, symbol, parents) == PUBLIC);
    }

    @JRubyMethod(name = "protected_method_defined?")
    public IRubyObject protected_method_defined(ThreadContext context, IRubyObject symbol) {
        return RubyBoolean.newBoolean(context, checkMethodVisibility(context, symbol, true) == PROTECTED);
    }

    @JRubyMethod(name = "protected_method_defined?")
    public IRubyObject protected_method_defined(ThreadContext context, IRubyObject symbol, IRubyObject includeSuper) {
        boolean parents = includeSuper.isTrue();

        return RubyBoolean.newBoolean(context, checkMethodVisibility(context, symbol, parents) == PROTECTED);
    }

    @JRubyMethod(name = "private_method_defined?")
    public IRubyObject private_method_defined(ThreadContext context, IRubyObject symbol) {
        return RubyBoolean.newBoolean(context, checkMethodVisibility(context, symbol, true) == PRIVATE);
    }

    @JRubyMethod(name = "private_method_defined?")
    public IRubyObject private_method_defined(ThreadContext context, IRubyObject symbol, IRubyObject includeSuper) {
        boolean parents = includeSuper.isTrue();

        return RubyBoolean.newBoolean(context, checkMethodVisibility(context, symbol, parents) == PRIVATE);
    }

    private Visibility checkMethodVisibility(ThreadContext context, IRubyObject symbol, boolean parents) {
        String name = TypeConverter.checkID(symbol).idString();

        RubyModule mod = this;

        if (!parents) mod = getMethodLocation();

        DynamicMethod method = mod.searchMethod(name);

        if (method.isUndefined()) return Visibility.UNDEFINED;

        if (!parents && method.getDefinedClass() != mod) return Visibility.UNDEFINED;

        return method.getVisibility();
    }

    @JRubyMethod(name = "public_class_method", rest = true)
    public RubyModule public_class_method(IRubyObject[] args) {
        checkFrozen();
        getSingletonClass().setMethodVisibility(args, PUBLIC);
        return this;
    }

    @JRubyMethod(name = "private_class_method", rest = true)
    public RubyModule private_class_method(IRubyObject[] args) {
        checkFrozen();
        getSingletonClass().setMethodVisibility(args, PRIVATE);
        return this;
    }

    @JRubyMethod(name = "alias_method", required = 2)
    public RubyModule alias_method(ThreadContext context, IRubyObject newId, IRubyObject oldId) {
        RubySymbol newSym = TypeConverter.checkID(newId);
        RubySymbol oldSym = TypeConverter.checkID(oldId); //  MRI uses rb_to_id but we return existing symbol

        defineAlias(newSym.idString(), oldSym.idString());

        if (isSingleton()) {
            ((MetaClass) this).getAttached().callMethod(context, "singleton_method_added", newSym);
        } else {
            callMethod(context, "method_added", newSym);
        }
        return this;
    }

    @JRubyMethod(name = "undef_method", rest = true)
    public RubyModule undef_method(ThreadContext context, IRubyObject[] args) {
        for (int i=0; i<args.length; i++) {
            RubySymbol name = TypeConverter.checkID(args[i]);

            undef(context, name.idString());
        }
        return this;
    }

    @JRubyMethod(name = {"module_eval", "class_eval"},
            reads = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE},
            writes = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE})
    public IRubyObject module_eval(ThreadContext context, Block block) {
        return specificEval(context, this, block, EvalType.MODULE_EVAL);
    }
    @JRubyMethod(name = {"module_eval", "class_eval"},
            reads = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE},
            writes = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE})
    public IRubyObject module_eval(ThreadContext context, IRubyObject arg0, Block block) {
        return specificEval(context, this, arg0, block, EvalType.MODULE_EVAL);
    }
    @JRubyMethod(name = {"module_eval", "class_eval"},
            reads = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE},
            writes = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE})
    public IRubyObject module_eval(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        return specificEval(context, this, arg0, arg1, block, EvalType.MODULE_EVAL);
    }
    @JRubyMethod(name = {"module_eval", "class_eval"},
            reads = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE},
            writes = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE})
    public IRubyObject module_eval(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return specificEval(context, this, arg0, arg1, arg2, block, EvalType.MODULE_EVAL);
    }
    @Deprecated
    public IRubyObject module_eval(ThreadContext context, IRubyObject[] args, Block block) {
        return specificEval(context, this, args, block, EvalType.MODULE_EVAL);
    }

    @JRubyMethod(name = {"module_exec", "class_exec"},
            reads = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE},
            writes = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE})
    public IRubyObject module_exec(ThreadContext context, Block block) {
        if (block.isGiven()) {
            return yieldUnder(context, this, IRubyObject.NULL_ARRAY, block.cloneBlockAndFrame(), EvalType.MODULE_EVAL);
        } else {
            throw context.runtime.newLocalJumpErrorNoBlock();
        }
    }

    @JRubyMethod(name = {"module_exec", "class_exec"}, rest = true,
            reads = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE},
            writes = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE})
    public IRubyObject module_exec(ThreadContext context, IRubyObject[] args, Block block) {
        if (block.isGiven()) {
            return yieldUnder(context, this, args, block.cloneBlockAndFrame(), EvalType.MODULE_EVAL);
        } else {
            throw context.runtime.newLocalJumpErrorNoBlock();
        }
    }

    @JRubyMethod(name = "remove_method", rest = true)
    public RubyModule remove_method(ThreadContext context, IRubyObject[] args) {
        for(int i=0;i<args.length;i++) {
            removeMethod(context, TypeConverter.checkID(args[i]).idString());
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
        StaticScope scope = context.getCurrentStaticScope();
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
            for (RubyClass nextClass = getSuperClass(); nextClass != null; nextClass = nextClass.getSuperClass()) {
                if (nextClass.isIncluded()) {
                    // does the class equal the module
                    if (nextClass.getDelegate() == nextModule.getDelegate()) {
                        // next in hierarchy is an included version of the module we're attempting,
                        // so we skip including it

                        // if we haven't encountered a real superclass, use the found module as the new inclusion point
                        if (!superclassSeen) currentInclusionPoint = nextClass;

                        continue ModuleLoop;
                    }
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

        if (!hasPrepends()) { // Set up a new holder class to hold all this types original methods.
            RubyClass origin = new PrependedModule(getRuntime(), getSuperClass(), this);

            // if the insertion point is a class, update subclass lists
            if (this instanceof RubyClass) {
                // if there's a non-null superclass, we're including into a normal class hierarchy;
                // update subclass relationships to avoid stale parent/child relationships
                if (getSuperClass() != null) {
                    getSuperClass().replaceSubclass((RubyClass) this, origin);
                }

                origin.addSubclass((RubyClass) this);
            }
            setSuperClass(origin);
        }

        RubyModule inclusionPoint = this;
        ModuleLoop: for (RubyModule nextModule : modulesToInclude) {
            checkForCyclicPrepend(nextModule);

            boolean superclassSeen = false;

            // scan prepend section of hierarchy for module, from superClass to the next concrete superClass
            RubyModule stopClass = getPrependCeiling();
            for (RubyClass nextClass = getSuperClass(); nextClass != stopClass; nextClass = nextClass.getSuperClass()) {
                if (nextClass.isIncluded()) {
                    // does the class equal the module
                    if (nextClass.getDelegate() == nextModule.getDelegate()) {
                        // next in hierarchy is an included version of the module we're attempting,
                        // so we skip including it

                        // if we haven't encountered a real superclass, use the found module as the new inclusion point
                        if (!superclassSeen) inclusionPoint = nextClass;

                        continue ModuleLoop;
                    }
                } else {
                    superclassSeen = true;
                }
            }

            inclusionPoint = proceedWithPrepend(inclusionPoint, nextModule.getDelegate());
        }
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
        List<RubyModule> modulesToInclude = new ArrayList<>();

        for (; baseModule != null; baseModule = baseModule.superClass) {
            // skip prepended roots
            if (baseModule != baseModule.getMethodLocation()) continue;

            modulesToInclude.add(baseModule.getDelegate());
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

        if (isRefinement()) {
            moduleToInclude.getMethods().forEach((name, method) -> addRefinedMethodEntry(name, method));
            wrapper.setFlag(INCLUDED_INTO_REFINEMENT, true);
        }

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
        return proceedWithInclude(insertBelow, !moduleToPrepend.isPrepended() ?
                moduleToPrepend.getOrigin() :
                moduleToPrepend);
    }


    //
    ////////////////// CLASS VARIABLE RUBY METHODS ////////////////
    //

    @JRubyMethod(name = "class_variable_defined?", required = 1)
    public IRubyObject class_variable_defined_p(ThreadContext context, IRubyObject var) {
        String id = validateClassVariable(context.runtime, var);

        for (RubyModule module = this; module != null; module = module.getSuperClass()) {
            if (module.hasClassVariable(id)) return context.tru;
        }

        return context.fals;
    }

    /** rb_mod_cvar_get
     *
     */
    public IRubyObject class_variable_get(IRubyObject name) {
        return getClassVar(name, validateClassVariable(getRuntime(), name));
    }

    @JRubyMethod(name = "class_variable_get")
    public IRubyObject class_variable_get19(IRubyObject name) {
        return class_variable_get(name);
    }

    /** rb_mod_cvar_set
     *
     */
    public IRubyObject class_variable_set(IRubyObject name, IRubyObject value) {
        return setClassVar(validateClassVariable(getRuntime(), name), value);
    }

    @JRubyMethod(name = "class_variable_set")
    public IRubyObject class_variable_set19(IRubyObject name, IRubyObject value) {
        return class_variable_set(name, value);
    }

    /** rb_mod_remove_cvar
     *
     */
    public IRubyObject remove_class_variable(ThreadContext context, IRubyObject name) {
        return removeClassVariable(validateClassVariable(context.runtime, name));
    }

    @JRubyMethod(name = "remove_class_variable")
    public IRubyObject remove_class_variable19(ThreadContext context, IRubyObject name) {
        return remove_class_variable(context, name);
    }

    @Deprecated
    public RubyArray class_variables19(ThreadContext context) {
        return class_variables(context);
    }

    @JRubyMethod(name = "class_variables")
    public RubyArray class_variables(ThreadContext context) {
        Ruby runtime = context.runtime;
        RubyArray ary = runtime.newArray();

        Collection<String> names = classVariablesCommon(true);
        for (String name : names) {
            ary.add(runtime.newSymbol(name));
        }
        return ary;
    }

    @JRubyMethod(name = "class_variables")
    public RubyArray class_variables(ThreadContext context, IRubyObject inherit) {
        Ruby runtime = context.runtime;
        RubyArray ary = runtime.newArray();

        Collection<String> names = classVariablesCommon(inherit.isTrue());
        for (String name : names) {
            ary.add(runtime.newSymbol(name));
        }
        return ary;
    }


    private Collection<String> classVariablesCommon(boolean inherit) {
        Set<String> names = new HashSet<String>();
        for (RubyModule p = this; p != null; p = p.getSuperClass()) {
            names.addAll(p.getClassVariableNameList());
            if (!inherit) break;
        }
        return names;
    }


    //
    ////////////////// CONSTANT RUBY METHODS ////////////////
    //

    /**
     * rb_mod_const_defined
     */
    @JRubyMethod(name = "const_defined?")
    public RubyBoolean const_defined_p(ThreadContext context, IRubyObject name) {

        return constDefined(context.runtime, name, true) ? context.tru : context.fals;
    }

    @JRubyMethod(name = "const_defined?")
    public RubyBoolean const_defined_p(ThreadContext context, IRubyObject name, IRubyObject recurse) {
        return constDefined(context.runtime, name, recurse.isTrue()) ? context.tru : context.fals;
    }

    private boolean constDefined(Ruby runtime, IRubyObject name, boolean inherit) {
        if (name instanceof RubySymbol) {
            RubySymbol sym = (RubySymbol) name;

            if (!sym.validConstantName()) {
                throw runtime.newNameError(str(runtime, "wrong constant name ", ids(runtime, sym)), sym);
            }

            String id = sym.idString();

            return inherit ? constDefined(id) : constDefinedAt(id);
        }

        RubyString fullName = name.convertToString();
        ByteList value = fullName.getByteList();

        ByteList pattern = CommonByteLists.COLON_COLON;

        Encoding enc = pattern.getEncoding();
        byte[] bytes = value.getUnsafeBytes();
        int begin = value.getBegin();
        int realSize = value.getRealSize();
        int end = begin + realSize;
        int currentOffset = 0;
        int patternIndex;
        int index = 0;
        RubyModule mod = this;

        if (value.startsWith(pattern)) {
            mod = runtime.getObject();
            currentOffset += 2;
        }

        for (; currentOffset < realSize && (patternIndex = value.indexOf(pattern, currentOffset)) >= 0; index++) {
            int t = enc.rightAdjustCharHead(bytes, currentOffset + begin, patternIndex + begin, end) - begin;
            if (t != patternIndex) {
                currentOffset = t;
                continue;
            }

            ByteList segment = value.makeShared(currentOffset, patternIndex - currentOffset);
            String id = RubySymbol.newConstantSymbol(runtime, fullName, segment).idString();

            IRubyObject obj;

            if (!inherit) {
                if (!mod.constDefinedAt(id)) {
                    return false;
                }
                obj = mod.getConstantAt(id);
            } else if (index == 0 && segment.realSize() == 0) {
                if (!mod.constDefined(id)) {
                    return false;
                }
                obj = mod.getConstant(id);
            } else {
                if (!mod.constDefinedFrom(id)) {
                    return false;
                }
                obj = mod.getConstantFrom(id);
            }

            if (!(obj instanceof RubyModule)) throw runtime.newTypeError(segment + " does not refer to class/module");

            mod = (RubyModule) obj;
            currentOffset = patternIndex + pattern.getRealSize();
        }

        if (mod == null) mod = this; // Bare 'Foo'

        ByteList lastSegment = value.makeShared(currentOffset, realSize - currentOffset);

        String id = RubySymbol.newConstantSymbol(runtime, fullName, lastSegment).idString();

        return mod.getConstantSkipAutoload(id, inherit, inherit) != null;
    }

    public IRubyObject const_get(IRubyObject symbol) {
        return const_get(getRuntime().getCurrentContext(), new IRubyObject[]{symbol});
    }

    @Deprecated
    public IRubyObject const_get_1_9(ThreadContext context, IRubyObject[] args) {
        return const_get(context, args);
    }

    @Deprecated
    public IRubyObject const_get_2_0(ThreadContext context, IRubyObject[] args) {
        return const_get(context, args);
    }

    /** rb_mod_const_get
     *
     */
    @JRubyMethod(name = "const_get", required = 1, optional = 1)
    public IRubyObject const_get(ThreadContext context, IRubyObject... args) {
        final Ruby runtime = context.runtime;
        boolean inherit = args.length == 1 || ( ! args[1].isNil() && args[1].isTrue() );

        final IRubyObject symbol = args[0];
        RubySymbol fullName = TypeConverter.checkID(symbol);
        String name = fullName.idString();

        int sep = name.indexOf("::");
        // symbol form does not allow ::
        if (symbol instanceof RubySymbol && sep != -1) {
            throw runtime.newNameError("wrong constant name ", fullName);
        }

        RubyModule mod = this;

        if (sep == 0) { // ::Foo::Bar
            mod = runtime.getObject();
            name = name.substring(2);
        }

        // Bare ::
        if (name.length() == 0) {
            throw runtime.newNameError("wrong constant name ", fullName);
        }

        while ( ( sep = name.indexOf("::") ) != -1 ) {
            final String segment = name.substring(0, sep);
            IRubyObject obj = mod.getConstant(validateConstant(segment, symbol), inherit, inherit);
            if (obj instanceof RubyModule) {
                mod = (RubyModule) obj;
            } else {
                throw runtime.newTypeError(segment + " does not refer to class/module");
            }
            name = name.substring(sep + 2);
        }

        return mod.getConstant(validateConstant(name, symbol), inherit, inherit);
    }

    /** rb_mod_const_set
     *
     */
    @JRubyMethod(name = "const_set", required = 2)
    public IRubyObject const_set(IRubyObject name, IRubyObject value) {
        return setConstant(validateConstant(name), value);
    }

    @JRubyMethod(name = "remove_const", required = 1, visibility = PRIVATE)
    public IRubyObject remove_const(ThreadContext context, IRubyObject rubyName) {
        String id = validateConstant(rubyName);
        IRubyObject value = deleteConstant(id);

        if (value != null) { // found it!
            invalidateConstantCache(id);

            if (value != UNDEF) return value;

            // autoload entry
            removeAutoload(id);
            return context.nil; // if we weren't auto-loaded MRI returns nil
        }

        if (hasConstantInHierarchy(id)) throw cannotRemoveError(id);

        throw context.runtime.newNameError("constant " + id + " not defined for " + getName(), id);
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

        RubyModule privateConstReference = context.getPrivateConstantReference();

        if (privateConstReference != null) {
            context.setPrivateConstantReference(null);
            throw getRuntime().newNameError("private constant " + privateConstReference + "::" + rubyName + " referenced", privateConstReference, rubyName);
        }

        if (this != runtime.getObject()) {
            throw runtime.newNameError("uninitialized constant %2$s::%1$s", this, rubyName);
        } else {
            throw runtime.newNameError("uninitialized constant %1$s", this, rubyName);
        }

    }

    @JRubyMethod(name = "constants")
    public RubyArray constants(ThreadContext context) {
        return constantsCommon(context, true, true);
    }

    @JRubyMethod(name = "constants")
    public RubyArray constants(ThreadContext context, IRubyObject allConstants) {
        return constantsCommon(context, false, allConstants.isTrue());
    }

    @Deprecated
    public RubyArray constants19(ThreadContext context) {
        return constants(context);
    }

    @Deprecated
    public RubyArray constants19(ThreadContext context, IRubyObject allConstants) {
        return constants(context, allConstants);
    }

    @Deprecated // no longer used
    public RubyArray constantsCommon19(ThreadContext context, boolean replaceModule, boolean allConstants) {
        return constantsCommon(context, replaceModule, allConstants);
    }

    private RubyArray constantsCommon(ThreadContext context, boolean replaceModule, boolean allConstants) {
        Ruby runtime = context.runtime;

        Collection<String> constantNames = constantsCommon(runtime, replaceModule, allConstants, false);
        RubyArray array = RubyArray.newBlankArrayInternal(runtime, constantNames.size());

        int i = 0;
        for (String name : constantNames) {
            array.storeInternal(i++, runtime.newSymbol(name));
        }
        array.realLength = i;
        return array;
    }

    /** rb_mod_constants
     *
     */
    public Collection<String> constantsCommon(Ruby runtime, boolean replaceModule, boolean allConstants) {
        return constantsCommon(runtime, replaceModule, allConstants, true);
    }

    public Collection<String> constantsCommon(Ruby runtime, boolean replaceModule, boolean allConstants, boolean includePrivate) {
        final RubyModule objectClass = runtime.getObject();

        final Collection<String> constantNames;
        if (allConstants) {
            if ((replaceModule && runtime.getModule() == this) || objectClass == this) {
                constantNames = objectClass.getConstantNames(includePrivate);
            } else {
                Set<String> names = new HashSet<>();
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

    public void deprecateConstant(Ruby runtime, String name) {
        ConstantEntry entry = getConstantMap().get(name);
        if (entry == null) {
            throw runtime.newNameError(str(runtime, "constant ", types(runtime, this), "::", ids(runtime, name), " not defined"), name);
        }

        storeConstant(name, entry.value, entry.hidden, true);
        invalidateConstantCache(name);
    }

    @JRubyMethod
    public IRubyObject deprecate_constant(ThreadContext context, IRubyObject name) {
        checkFrozen();

        deprecateConstant(context.runtime, validateConstant(name));
        return this;
    }

    @JRubyMethod(rest = true)
    public IRubyObject deprecate_constant(ThreadContext context, IRubyObject[] names) {
        for (IRubyObject name: names) {
            deprecate_constant(context, name);
        }
        return this;
    }

    @JRubyMethod
    public IRubyObject private_constant(ThreadContext context, IRubyObject name) {
        checkFrozen();

        String id = validateConstant(name);

        setConstantVisibility(context.runtime, id, true);
        invalidateConstantCache(id);

        return this;
    }

    @JRubyMethod(required = 1, rest = true)
    public IRubyObject private_constant(ThreadContext context, IRubyObject[] rubyNames) {
        for (IRubyObject rubyName : rubyNames) {
            private_constant(context, rubyName);
        }
        return this;
    }

    @JRubyMethod
    public IRubyObject public_constant(ThreadContext context, IRubyObject name) {
        checkFrozen();

        String id = validateConstant(name);

        setConstantVisibility(context.runtime, id, false);
        invalidateConstantCache(id);
        return this;
    }

    @JRubyMethod(required = 1, rest = true)
    public IRubyObject public_constant(ThreadContext context, IRubyObject[] rubyNames) {
        for (IRubyObject rubyName : rubyNames) {
            public_constant(context, rubyName);
        }
        return this;
    }

    @JRubyMethod(name = "prepend", required = 1, rest = true)
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
        return context.nil;
    }

    // NOTE: internal API
    public final void setConstantVisibility(Ruby runtime, String name, boolean hidden) {
        ConstantEntry entry = getConstantMap().get(name);

        if (entry == null) {
            throw runtime.newNameError("constant " + getName() + "::" + name + " not defined", name);
        }

        storeConstant(name, entry.value, hidden);
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
        RubyModule highest = this;
        do {
            if (module.hasClassVariable(name)) {
                highest = module;
            }
        } while ((module = module.getSuperClass()) != null);

        return highest.storeClassVariable(name, value);
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
        IRubyObject value = getClassVarQuiet(name);

        if (value == null) {
            throw getRuntime().newNameError("uninitialized class variable %1$s in %2$s", this, name);
        }

        return value;
    }

    public IRubyObject getClassVar(IRubyObject nameObject, String name) {
        IRubyObject value = getClassVarQuiet(name);

        if (value == null) {
            throw getRuntime().newNameError("uninitialized class variable %1$s in %2$s", this, nameObject);
        }

        return value;
    }

    public IRubyObject getClassVarQuiet(String name) {
        assert IdUtil.isClassVariable(name);
        Object value;
        RubyModule module = this;
        RubyModule highest = null;

        do {
            if (module.hasClassVariable(name)) {
                highest = module;
            }
        } while ((module = module.getSuperClass()) != null);

        if (highest != null) return highest.fetchClassVariable(name);

        return null;
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
        assert name != null : "null name";
        //assert IdUtil.isConstant(name) : "invalid constant name: " + name;
        // NOTE: can not assert IdUtil.isConstant(name) until unmarshal-ing is using this for Java classes
        // since some classes won't assert the upper case first char (anonymous classes start with a digit)

        IRubyObject value = getConstantNoConstMissing(name, inherit, includeObject);
        Ruby runtime = metaClass.runtime;

        return value != null ? value :
            callMethod(runtime.getCurrentContext(), "const_missing", runtime.newSymbol(name));
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
        IRubyObject constant = iterateConstantNoConstMissing(name, this, inherit, true);

        if (constant == null && !isClass() && includeObject) {
            constant = iterateConstantNoConstMissing(name, getRuntime().getObject(), inherit, true);
        }

        return constant;
    }

    public final IRubyObject getConstantNoConstMissingSkipAutoload(String name) {
        return getConstantSkipAutoload(name, true, true);
    }

    @Deprecated
    public IRubyObject getConstantNoConstMissingSKipAutoload(String name) {
        return getConstantSkipAutoload(name, true, true);
    }

    // returns null for autoloads that have failed
    private IRubyObject getConstantSkipAutoload(String name, boolean inherit, boolean includeObject) {
        IRubyObject constant = iterateConstantNoConstMissing(name, this, inherit, false);

        if (constant == null && !isClass() && includeObject) {
            constant = iterateConstantNoConstMissing(name, getRuntime().getObject(), inherit, false);
        }

        return constant;
    }

    private static IRubyObject iterateConstantNoConstMissing(String name,
        RubyModule init, boolean inherit, boolean loadConstant) {
        for (RubyModule mod = init; mod != null; mod = mod.getSuperClass()) {
            IRubyObject value =
                    loadConstant ?
                            mod.getConstantWithAutoload(name, null, true) :
                            mod.fetchConstant(name, true);

            // if it's UNDEF and we're not loading and there's no autoload set up, consider it undefined
            if ( value == UNDEF && !loadConstant && mod.getAutoloadMap().get(name) == null) return null;

            if ( value != null ) return value;

            if ( ! inherit ) break;
        }
        return null;
    }

    // not actually called anywhere (all known uses call the fast version)
    public IRubyObject getConstantFrom(String name) {
        IRubyObject value = getConstantFromNoConstMissing(name);

        return value != null ? value : getConstantFromConstMissing(name);
    }

    /**
     * Search just this class for a constant value, or trigger autoloading.
     *
     * @param name
     * @return
     */
    public IRubyObject getConstantWithAutoload(String name, IRubyObject failedAutoloadValue, boolean includePrivate) {
        RubyModule autoloadModule = null;
        IRubyObject result;

        while ((result = fetchConstant(name, includePrivate)) != null) { // loop for autoload
            if (result == RubyObject.UNDEF) {
                if (autoloadModule == this) return failedAutoloadValue;
                autoloadModule = this;

                final RubyModule.Autoload autoload = getAutoloadMap().get(name);

                if (autoload == null) return null;
                if (autoload.getValue() != null) return autoload.getValue();

                autoload.load(getRuntime().getCurrentContext());
                continue;
            }

            return result;
        }

        return autoloadModule != null ? failedAutoloadValue : null;
    }

    @Deprecated
    public IRubyObject fastGetConstantFrom(String internedName) {
        return getConstantFrom(internedName);
    }

    public IRubyObject getConstantFromNoConstMissing(String name) {
        return getConstantFromNoConstMissing(name, true);
    }

    public IRubyObject getConstantFromNoConstMissing(String name, boolean includePrivate) {
        final Ruby runtime = getRuntime();
        final RubyClass objectClass = runtime.getObject();

        RubyModule mod = this;

        while (mod != null) {
            IRubyObject result = mod.getConstantWithAutoload(name, null, includePrivate);

            if (result != null) {
                if ( mod == objectClass && this != objectClass ) {
                    return null;
                }

                return result;
            }

            mod = mod.getSuperClass();
        }

        return null;
    }

    @Deprecated
    public IRubyObject fastGetConstantFromNoConstMissing(String internedName) {
        return getConstantFromNoConstMissing(internedName);
    }

    public IRubyObject getConstantFromConstMissing(String name) {
        final Ruby runtime = metaClass.runtime;
        return callMethod(runtime.getCurrentContext(), "const_missing", runtime.fastNewSymbol(name));
    }

    @Deprecated
    public IRubyObject fastGetConstantFromConstMissing(String internedName) {
        return getConstantFromConstMissing(internedName);
    }

    public final IRubyObject resolveUndefConstant(String name) {
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
        return setConstantCommon(name, value, false, false);
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
        return setConstantCommon(name, value, false, true);
    }

    public IRubyObject setConstant(String name, IRubyObject value, boolean hidden) {
        return setConstantCommon(name, value, hidden, true);
    }

    /**
     * Set the named constant on this module. Also, if the value provided is another Module and
     * that module has not yet been named, assign it the specified name.
     *
     * @param name The name to assign
     * @param value The value to assign to it; if an unnamed Module, also set its basename to name
     * @return The result of setting the variable.
     */
    private IRubyObject setConstantCommon(String name, IRubyObject value, boolean hidden, boolean warn) {
        IRubyObject oldValue = fetchConstant(name);

        setParentForModule(name, value);

        if (oldValue != null) {
            boolean notAutoload = oldValue != UNDEF;
            if (notAutoload || !setAutoloadConstant(name, value)) {
                if (warn && notAutoload) {
                    if (this.equals(getRuntime().getObject())) {
                        getRuntime().getWarnings().warn(ID.CONSTANT_ALREADY_INITIALIZED, "already initialized constant " + name);
                    } else {
                        getRuntime().getWarnings().warn(ID.CONSTANT_ALREADY_INITIALIZED, "already initialized constant " + this + "::" + name);
                    }
                }
                // might just call storeConstant(name, value, hidden) but to maintain
                // backwards compatibility with calling #storeConstant overrides
                if (hidden) storeConstant(name, value, true);
                else storeConstant(name, value);
            }
        } else {
            if (hidden) storeConstant(name, value, true);
            else storeConstant(name, value);
        }

        invalidateConstantCache(name);
        return value;
    }

    private void setParentForModule(final String name, final IRubyObject value) {
        // if adding a module under a constant name, set that module's basename to the constant name
        if ( value instanceof RubyModule ) {
            RubyModule module = (RubyModule) value;
            if (module != this && module.getBaseName() == null) {
                module.setBaseName(name);
                module.setParent(this);
            }
            module.calculateName();
        }
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

    public boolean isConstantDefined(String name, boolean inherit) {
        return constDefinedInner(name, false, inherit, false);
    }

    // rb_const_defined
    public boolean constDefined(String name) {
        return constDefinedInner(name, false, true, false);
    }

    // rb_const_defined_at
    public boolean constDefinedAt(String name) {
        return constDefinedInner(name, true, false, false);
    }

    // rb_const_defined_from
    public boolean constDefinedFrom(String name) {
        return constDefinedInner(name, true, true, false);
    }

    // rb_public_const_defined_from
    public boolean publicConstDefinedFrom(String name) {
        return constDefinedInner(name, true, true, true);
    }

    // Fix for JRUBY-1339 - search hierarchy for constant
    /**
     * rb_const_defined_0
     */
    private boolean constDefinedInner(String name, boolean exclude, boolean recurse, boolean visibility) {
        Ruby runtime = getRuntime();

        RubyClass object = runtime.getObject();
        boolean moduleRetry = false;

        RubyModule module = this;

        retry: while (true) {
            while (module != null) {
                ConstantEntry entry;
                if ((entry = module.constantEntryFetch(name)) != null) {
                    if (visibility && entry.hidden) {
                        return false;
                    }

                    IRubyObject value = entry.value;

                    // autoload is not in progress and should not appear defined
                    if (value == UNDEF && module.checkAutoloadRequired(runtime, name, null) == null &&
                            !module.autoloadingValue(runtime, name)) {
                        return false;
                    }

                    if (exclude && module == object && this != object) {
                        return false;
                    }

                    return true;
                }

                if (!recurse) break;

                module = module.getSuperClass();
            }

            if (!exclude && !moduleRetry && this.isModule()) {
                moduleRetry = true;
                module = object;
                continue retry;
            }

            return false;
        }
    }

    // MRI: rb_autoloading_value
    public boolean autoloadingValue(Ruby runtime, String name) {
        final Autoload autoload = getAutoloadMap().get(name);

        // autoload has been evacuated
        if (autoload == null) return false;

        // autoload is in progress on this thread and has updated the value
        if (autoload.isSelf(runtime.getCurrentContext())) {
            if (autoload.getValue() != null) {
                return true;
            }
        }

        return false; // autoload has yet to run
    }

    // MRI: check_autoload_required
    private RubyString checkAutoloadRequired(Ruby runtime, String name, String[] autoloadPath) {
        final Autoload autoload = getAutoloadMap().get(name);

        // autoload has been evacuated
        if (autoload == null) return null;

        RubyString file = autoload.getFile();

        // autoload filename is empty
        if (file.length() == 0) {
            throw runtime.newArgumentError("empty file name");
        }

        // autoload is in progress on anther thread
        if (autoload.ctx != null && !autoload.isSelf(runtime.getCurrentContext())) {
            return file;
        }

        String[] loading = {null};

        // feature has not been loaded yet
        if (!runtime.getLoadService().featureAlreadyLoaded(file.asJavaString(), loading)) return file;

        // feature is currently loading
        if (autoloadPath != null && loading[0] != null) {
            autoloadPath[0] = loading[0];
            return file;
        }

        return null; // autoload has yet to run
    }

    public boolean isConstantDefined(String name) {
        return constDefinedInner(name, false, true, false);
    }

    //
    ////////////////// COMMON CONSTANT / CVAR METHODS ////////////////
    //

    private RaiseException cannotRemoveError(String id) {
        Ruby runtime = getRuntime();

        return getRuntime().newNameError(str(runtime, "cannot remove ", ids(runtime, id), " for ", types(runtime, this)), id);
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
        }
        return getClassVariablesForWriteAtomic();
    }

    /**
     * Get the class variables for write. If it is not set or not of the right size,
     * synchronize against the object and prepare it accordingly.
     *
     * @return the class vars map, ready for assignment
     */
    private Map<String,IRubyObject> getClassVariablesForWriteSynchronized() {
        Map<String, IRubyObject> myClassVars = classVariables;
        if ( myClassVars == Collections.EMPTY_MAP ) {
            synchronized (this) {
                myClassVars = classVariables;

                if ( myClassVars == Collections.EMPTY_MAP ) {
                    return classVariables = new ConcurrentHashMap<String, IRubyObject>(4, 0.75f, 2);
                }
                return myClassVars;
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
            Map<String, IRubyObject> myClassVars = classVariables;

            if ( myClassVars != Collections.EMPTY_MAP ) return myClassVars;

            Map<String, IRubyObject> newClassVars;
            newClassVars = new ConcurrentHashMap<String, IRubyObject>(4, 0.75f, 2);

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

    protected final String validateClassVariable(String name) {
        if (IdUtil.isValidClassVariableName(name)) {
            return name;
        }
        throw getRuntime().newNameError("`%1$s' is not allowed as a class variable name", this, name);
    }

    protected final String validateClassVariable(IRubyObject nameObj, String name) {
        if (IdUtil.isValidClassVariableName(name)) {
            return name;
        }
        throw getRuntime().newNameError("`%1$s' is not allowed as a class variable name", this, nameObj);
    }

    protected String validateClassVariable(Ruby runtime, IRubyObject object) {
        RubySymbol name = TypeConverter.checkID(object);

        if (!name.validClassVariableName()) {
            throw getRuntime().newNameError(str(runtime, "`", ids(runtime, name), "' is not allowed as a class variable name"), this, object);
        }

        return name.idString();
    }

    protected final void ensureClassVariablesSettable() {
        checkAndRaiseIfFrozen();
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
        ConstantEntry entry = constantEntryFetch(name);

        if (entry == null) return null;

        if (entry.hidden && !includePrivate) {
            getRuntime().getCurrentContext().setPrivateConstantReference(getOrigin());
            return null;
        }
        if (entry.deprecated) {
            final Ruby runtime = getRuntime();
            if ( "Object".equals( getName() ) ) {
                runtime.getWarnings().warn(ID.CONSTANT_DEPRECATED, "constant ::"+ name +" is deprecated");
            }
            else {
                runtime.getWarnings().warn(ID.CONSTANT_DEPRECATED, "constant "+ getName() +"::"+ name +" is deprecated");
            }
        }

        return entry.value;
    }

    @Deprecated
    public IRubyObject fastFetchConstant(String internedName) {
        return fetchConstant(internedName);
    }

    public IRubyObject storeConstant(String name, IRubyObject value) {
        assert value != null : "value is null";

        ensureConstantsSettable();
        return constantTableStore(name, value);
    }

    public IRubyObject storeConstant(String name, IRubyObject value, boolean hidden) {
        assert value != null : "value is null";

        ensureConstantsSettable();
        return constantTableStore(name, value, hidden);
    }

    // NOTE: private for now - not sure about the API - maybe an int mask would be better?
    private IRubyObject storeConstant(String name, IRubyObject value, boolean hidden, boolean deprecated) {
        assert value != null : "value is null";

        ensureConstantsSettable();
        return constantTableStore(name, value, hidden, deprecated);
    }

    @Deprecated
    public IRubyObject fastStoreConstant(String internedName, IRubyObject value) {
        return storeConstant(internedName, value);
    }

    // removes and returns the stored value without processing undefs (autoloads)
    public IRubyObject deleteConstant(String name) {
        ensureConstantsSettable();
        return constantTableRemove(name);
    }

    @Deprecated
    public List<Variable<IRubyObject>> getStoredConstantList() {
        return null;
    }

    @Deprecated
    public List<String> getStoredConstantNameList() {
        return new ArrayList<>(getConstantMap().keySet());
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

        HashSet<String> publicNames = new HashSet<>(getConstantMap().size());

        for (Map.Entry<String, ConstantEntry> entry : getConstantMap().entrySet()) {
            if (entry.getValue().hidden) continue;
            publicNames.add(entry.getKey());
        }
        return publicNames;
    }

    /**
     * Validates name is a valid constant name and returns its id string.
     * @param name object to verify as a valid constant.
     * @return the id for this valid constant name.
     */
    protected final String validateConstant(IRubyObject name) {
        return RubySymbol.retrieveIDSymbol(name, (sym, newSym) -> {
            if (!sym.validConstantName()) {
                throw getRuntime().newNameError(str(getRuntime(), "wrong constant name ", sym), sym);
            }
        }).idString();
    }

    // FIXME: This should really be working with symbol segments (errorName is FQN).
    protected final String validateConstant(String name, IRubyObject errorName) {
        if (IdUtil.isValidConstantName(name)) return name;

        Ruby runtime = getRuntime();

        Encoding resultEncoding = runtime.getDefaultInternalEncoding();
        if (resultEncoding == null) resultEncoding = runtime.getDefaultExternalEncoding();

        // MRI is more complicated than this and distinguishes between ID and non-ID.
        RubyString nameString = errorName.asString();

        return RubySymbol.retrieveIDSymbol(nameString, (sym, newSym) -> {
            if (!sym.validConstantName()) {
                throw getRuntime().newNameError(str(getRuntime(), "wrong constant name ", sym), sym);
            }
        }).idString();
    }

    protected final void ensureConstantsSettable() {
        checkAndRaiseIfFrozen();
    }

    private void checkAndRaiseIfFrozen() throws RaiseException {
        if ( isFrozen() ) {
            if (this instanceof RubyClass) {
                if (getBaseName() == null) { // anonymous
                    // MRI 2.2.2 does get ugly ... as it skips this logic :
                    // RuntimeError: can't modify frozen #<Class:#<Class:0x0000000095a920>>
                    throw getRuntime().newFrozenError(getName());
                }
                throw getRuntime().newFrozenError("#<Class:" + getName() + '>');
            }
            throw getRuntime().newFrozenError("Module");
        }
    }

    @Override
    public final void checkFrozen() {
       if ( isFrozen() ) {
           throw getRuntime().newFrozenError(isClass() ? "class" : "module");
       }
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

    protected IRubyObject constantTableStore(String name, IRubyObject value, boolean hidden) {
        return constantTableStore(name, value, hidden, false);
    }

    protected IRubyObject constantTableStore(String name, IRubyObject value, boolean hidden, boolean deprecated) {
        Map<String, ConstantEntry> constMap = getConstantMapForWrite();
        constMap.put(name, new ConstantEntry(value, hidden, deprecated));
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
    protected final void defineAutoload(String symbol, RubyString path) {
        final Autoload existingAutoload = getAutoloadMap().get(symbol);
        if (existingAutoload == null || existingAutoload.getValue() == null) {
            storeConstant(symbol, RubyObject.UNDEF);
            getAutoloadMapForWrite().put(symbol, new Autoload(symbol, path));
        }
    }

    /**
     * Extract an Object which is defined by autoload thread from autoloadMap and define it as a constant.
     */
    protected final IRubyObject finishAutoload(String name) {
        final Autoload autoload = getAutoloadMap().get(name);
        if ( autoload == null ) return null;
        final IRubyObject value = autoload.getValue();

        if (value != null && value != UNDEF) {
            storeConstant(name, value);
            invalidateConstantCache(name);
        }

        removeAutoload(name);

        return value;
    }

    /**
     * Get autoload constant.
     * If it's first resolution for the constant, it tries to require the defined feature and returns the defined value.
     * Multi-threaded accesses are blocked and processed sequentially except if the caller is the autoloading thread.
     */
    public final IRubyObject getAutoloadConstant(String name) {
        return getAutoloadConstant(name, true);
    }

    protected IRubyObject getAutoloadConstant(String name, boolean loadConstant) {
        final Autoload autoload = getAutoloadMap().get(name);
        if ( autoload == null ) return null;
        if ( ! loadConstant ) return RubyObject.UNDEF;
        return autoload.load( getRuntime().getCurrentContext() );
    }

    /**
     * Set an Object as a defined constant in autoloading.
     */
    private boolean setAutoloadConstant(String name, IRubyObject value) {
        final Autoload autoload = getAutoloadMap().get(name);
        if ( autoload != null ) {
            boolean set = autoload.setConstant(getRuntime().getCurrentContext(), value);
            if ( ! set ) removeAutoload(name);
            return set;
        }
        return false;
    }

    /**
     * Removes an Autoload object from autoloadMap. ConstantMap must be updated before calling this.
     */
    private void removeAutoload(String name) {
        getAutoloadMapForWrite().remove(name);
    }

    protected RubyString getAutoloadFile(String name) {
        final Autoload autoload = getAutoloadMap().get(name);
        return autoload == null ? null : autoload.getFile();
    }

    private static void define(RubyModule module, JavaMethodDescriptor desc, final String simpleName, DynamicMethod dynamicMethod) {
        JRubyMethod jrubyMethod = desc.anno;
        final String[] names = jrubyMethod.name();
        final String[] aliases = jrubyMethod.alias();

        RubyModule singletonClass;

        if (jrubyMethod.meta()) {
            singletonClass = module.getSingletonClass();
            dynamicMethod.setImplementationClass(singletonClass);

            final String baseName;
            if (names.length == 0) {
                baseName = desc.name;
                singletonClass.addMethod(baseName, dynamicMethod);
            } else {
                baseName = names[0];
                for (String name : names) singletonClass.addMethod(name, dynamicMethod);
            }

            if (aliases.length > 0) {
                for (String alias : aliases) singletonClass.defineAlias(alias, baseName);
            }
        } else {
            String baseName;
            if (names.length == 0) {
                baseName = desc.name;
                module.getMethodLocation().addMethod(baseName, dynamicMethod);
            } else {
                baseName = names[0];
                for (String name : names) module.getMethodLocation().addMethod(name, dynamicMethod);
            }

            if (aliases.length > 0) {
                for (String alias : aliases) module.defineAlias(alias, baseName);
            }

            if (jrubyMethod.module()) {
                singletonClass = module.getSingletonClass();
                // module/singleton methods are all defined public
                DynamicMethod moduleMethod = dynamicMethod.dup();
                moduleMethod.setImplementationClass(singletonClass);
                moduleMethod.setVisibility(PUBLIC);

                if (names.length == 0) {
                    baseName = desc.name;
                    singletonClass.addMethod(desc.name, moduleMethod);
                } else {
                    baseName = names[0];
                    for (String name : names) singletonClass.addMethod(name, moduleMethod);
                }

                if (aliases.length > 0) {
                    for (String alias : aliases) singletonClass.defineAlias(alias, baseName);
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
     * Pre-built test that takes ThreadContext, IRubyObject and checks that the object is a module with the
     * same ID as this one.
     */
    private MethodHandle idTest;

    /**
     * The class/module within whose namespace this class/module resides.
     */
    public RubyModule parent;

    /**
     * The base name of this class/module, excluding nesting. If null, this is an anonymous class.
     */
    protected String baseName;

    /**
     * The cached name, full class name e.g. Foo::Bar if this class and all containing classes are non-anonymous.
     * The cached anonymous class name never changes and has a nonzero cost to calculate.
     */
    private transient String cachedName;
    private transient RubyString cachedRubyName;

    @SuppressWarnings("unchecked")
    private volatile Map<String, ConstantEntry> constants = Collections.EMPTY_MAP;

    /**
     * Represents a constant value, possibly hidden (private).
     */
    public static class ConstantEntry {
        public final IRubyObject value;
        public final boolean hidden;
        final boolean deprecated;

        public ConstantEntry(IRubyObject value, boolean hidden) {
            this.value = value;
            this.hidden = hidden;
            this.deprecated = false;
        }

        ConstantEntry(IRubyObject value, boolean hidden, boolean deprecated) {
            this.value = value;
            this.hidden = hidden;
            this.deprecated = deprecated;
        }

        public ConstantEntry dup() {
            return new ConstantEntry(value, hidden, deprecated);
        }
    }

    @Deprecated
    public interface AutoloadMethod {
        void load(Ruby runtime);
        RubyString getFile();
    }

    /**
     * Objects for holding autoload state for the defined constant.
     *
     * 'Module#autoload' creates this object and stores it in autoloadMap.
     * This object can be shared with multiple threads so take care to change volatile and synchronized definitions.
     */
    public final class Autoload {
        // A ThreadContext which is executing autoload.
        private volatile ThreadContext ctx;
        // An object defined for the constant while autoloading.
        private volatile IRubyObject value;
        // The symbol ID for the autoload constant
        private final String symbol;
        // The Ruby string representing the path to load
        private final RubyString path;

        Autoload(String symbol, RubyString path) {
            this.ctx = null;
            this.value = null;
            this.symbol = symbol;
            this.path = path;
        }

        // Returns an object for the constant if the caller is the autoloading thread.
        // Otherwise, try to start autoloading and returns the defined object by autoload.
        synchronized IRubyObject load(ThreadContext ctx) {
            if (this.ctx == null) {
                this.ctx = ctx;
            } else if (isSelf(ctx)) {
                return getValue();
            }

            try {
                // This method needs to be synchronized for removing Autoload
                // from autoloadMap when it's loaded.
                load(ctx.runtime);
            } catch (LoadError | RuntimeError lre) {
                // reset ctx to null for a future attempt to load
                this.ctx = null;
                throw lre;
            }

            return getValue();
        }

        // Update an object for the constant if the caller is the autoloading thread.
        synchronized boolean setConstant(ThreadContext ctx, IRubyObject newValue) {
            boolean isSelf = isSelf(ctx);

            if (isSelf) value = newValue;

            return isSelf;
        }

        // Returns an object for the constant defined by autoload.
        synchronized IRubyObject getValue() {
            return value;
        }

        private void load(Ruby runtime) {
            LoadService loadService = runtime.getLoadService();
            if (!loadService.featureAlreadyLoaded(path.asJavaString())) {
                if (loadService.autoloadRequire(path)) {
                    // Do not finish autoloading by cyclic autoload
                    finishAutoload(symbol);
                }
            }
        }

        // Returns the assigned feature.
        RubyString getFile() {
            return path;
        }

        private boolean isSelf(ThreadContext rhs) {
            ThreadContext ctx = this.ctx;
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
        return getFlag(CACHEPROXY_F);
    }

    /**
     * Set whether this Java proxy class should try to keep its instances idempotent
     * and alive using the ObjectProxyCache.
     */
    public void setCacheProxy(boolean cacheProxy) {
        setFlag(CACHEPROXY_F, cacheProxy);
    }

    @Override
    public <T> T toJava(Class<T> target) {
        if (target == Class.class) { // try java_class for proxy modules
            final ThreadContext context = metaClass.runtime.getCurrentContext();
            Class<?> javaClass = JavaClass.getJavaClassIfProxy(context, this);
            if (javaClass != null) return (T) javaClass;
        }

        return super.toJava(target);
    }

    public Set<String> discoverInstanceVariables() {
        HashSet<String> set = new HashSet();
        RubyModule cls = this;
        while (cls != null) {
            Map<String, DynamicMethod> methods = cls.getOrigin().getMethodLocation().getMethods();

            methods.forEach((name, method) -> set.addAll(method.getInstanceVariableNames()));

            cls = cls.getSuperClass();
        }
        return set;
    }

    public boolean isRefinement() {
        return getFlag(REFINED_MODULE_F);
    }

    public boolean isIncludedIntoRefinement() {
        return getFlag(INCLUDED_INTO_REFINEMENT);
    }

    /**
     * Return true if the given method is defined on this class and is a builtin
     * (defined in Java at boot).
     *
     * @param methodName
     * @return
     */
    public boolean isMethodBuiltin(String methodName) {
        DynamicMethod method = searchMethod(methodName);

        return method != null && method.isBuiltin();
    }

    public Map<RubyModule, RubyModule> getRefinements() {
        return refinements;
    }

    public Map<RubyModule, RubyModule> getRefinementsForWrite() {
        Map<RubyModule, RubyModule> refinements = this.refinements;
        return !refinements.isEmpty() ? refinements : (this.refinements = newRefinementsMap());
    }

    public void setRefinements(Map<RubyModule, RubyModule> refinements) {
        this.refinements = refinements;
    }

    private volatile Map<String, Autoload> autoloads = Collections.EMPTY_MAP;
    protected volatile Map<String, DynamicMethod> methods = Collections.EMPTY_MAP;
    protected Map<String, CacheEntry> cachedMethods = Collections.EMPTY_MAP;
    protected int generation;
    protected Integer generationObject;

    protected volatile Set<RubyClass> includingHierarchies = Collections.EMPTY_SET;

    /**
     * Where are the methods of this module/class located?
     *
     * This only happens as a result of prepend (see PrependedModule) where it
     * moves all methods to a PrependedModule which will be beneath the actual
     * module which was prepended.
     */
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

    @Deprecated
    public boolean fastIsConstantDefined(String internedName){
        return isConstantDefined(internedName);
    }

    @Deprecated
    public boolean fastIsConstantDefined19(String internedName) {
        return isConstantDefined(internedName, true);
    }

    @Deprecated
    public boolean fastIsConstantDefined19(String internedName, boolean inherit) {
        return isConstantDefined(internedName, inherit);
    }

    @Deprecated
    public RubyBoolean const_defined_p19(ThreadContext context, IRubyObject[] args) {
        switch (args.length) {
            case 1:
                return const_defined_p(context, args[0]);
            case 2:
                return const_defined_p(context, args[0], args[1]);
        }

        Arity.checkArgumentCount(context, args, 1, 2);
        return null; // not reached
    }

    @Deprecated
    public static class ModuleKernelMethods {
        @Deprecated
        public static IRubyObject autoload(ThreadContext context, IRubyObject self, IRubyObject symbol, IRubyObject file) {
            return ((RubyModule) self).autoload(context, symbol, file);
        }

        @Deprecated
        public static IRubyObject autoload_p(ThreadContext context, IRubyObject self, IRubyObject symbol) {
            return ((RubyModule) self).autoload_p(context, symbol);
        }
    }

    protected ClassIndex classIndex = ClassIndex.NO_INDEX;

    private volatile Map<String, IRubyObject> classVariables = Collections.EMPTY_MAP;

    /** Refinements added to this module are stored here **/
    private volatile Map<RubyModule, RubyModule> refinements = Collections.EMPTY_MAP;

    /** A list of refinement hosts for this refinement */
    private volatile Map<RubyModule, IncludedModule> activatedRefinements = Collections.EMPTY_MAP;

    /** The class this refinement refines */
    volatile RubyModule refinedClass = null;

    /** The module where this refinement was defined */
    private volatile RubyModule definedAt = null;

    private static final AtomicReferenceFieldUpdater<RubyModule, Map> CLASSVARS_UPDATER;

    static {
        AtomicReferenceFieldUpdater<RubyModule, Map> updater = null;
        try {
            updater = AtomicReferenceFieldUpdater.newUpdater(RubyModule.class, Map.class, "classVariables");
        }
        catch (final RuntimeException ex) {
            if (ex.getCause() instanceof AccessControlException) {
                // security prevented creation; fall back on synchronized assignment
            }
            else {
                throw ex;
            }
        }
        CLASSVARS_UPDATER = updater;
    }

    // Invalidator used for method caches
    protected final Invalidator methodInvalidator;

    /** Whether this class proxies a normal Java class */
    private boolean javaProxy = false;

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    /**
     * A handle for invoking the module ID test, to be reused for all idTest handles below.
     */
    private static final MethodHandle testModuleMatch = Binder
            .from(boolean.class, ThreadContext.class, IRubyObject.class, int.class)
            .invokeStaticQuiet(LOOKUP, Bootstrap.class, "testModuleMatch");
}
