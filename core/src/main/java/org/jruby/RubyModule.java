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
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

import jnr.constants.Constant;
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
import org.jruby.api.JRubyAPI;
import org.jruby.api.Warn;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.embed.Extension;
import org.jruby.exceptions.LoadError;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.RuntimeError;
import org.jruby.internal.runtime.AbstractIRMethod;
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
import org.jruby.ir.Interp;
import org.jruby.ir.JIT;
import org.jruby.javasupport.JavaUtil;
import org.jruby.javasupport.binding.MethodGatherer;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.Constants;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.IRBlockBody;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.MethodFactory;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.PositionAware;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.backtrace.RubyStackTraceElement;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.Variable;
import org.jruby.runtime.callsite.CacheEntry;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.load.LoadService;
import org.jruby.runtime.marshal.MarshalDumper;
import org.jruby.runtime.marshal.MarshalLoader;
import org.jruby.runtime.opto.Invalidator;
import org.jruby.runtime.opto.OptoFactory;
import org.jruby.runtime.profile.MethodEnhancer;
import org.jruby.util.RubyStringBuilder;
import org.jruby.util.ByteList;
import org.jruby.util.ClassProvider;
import org.jruby.util.IdUtil;
import org.jruby.util.StringSupport;
import org.jruby.util.cli.Options;
import org.jruby.util.io.RubyInputStream;
import org.jruby.util.io.RubyOutputStream;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import static org.jruby.RubySymbol.newHardSymbol;
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
import static org.jruby.api.Access.basicObjectClass;
import static org.jruby.api.Access.instanceConfig;
import static org.jruby.api.Access.loadService;
import static org.jruby.api.Access.objectClass;
import static org.jruby.api.Check.checkID;
import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.asSymbol;
import static org.jruby.api.Convert.castAsHash;
import static org.jruby.api.Convert.castAsModule;
import static org.jruby.api.Create.allocArray;
import static org.jruby.api.Create.dupString;
import static org.jruby.api.Create.newArray;
import static org.jruby.api.Create.newEmptyArray;
import static org.jruby.api.Create.newString;
import static org.jruby.api.Define.defineModule;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.frozenError;
import static org.jruby.api.Error.nameError;
import static org.jruby.api.Error.runtimeError;
import static org.jruby.api.Error.typeError;
import static org.jruby.api.Warn.warn;
import static org.jruby.api.Warn.warnDeprecated;
import static org.jruby.api.Warn.warning;
import static org.jruby.api.Warn.warningDeprecated;
import static org.jruby.runtime.Visibility.MODULE_FUNCTION;
import static org.jruby.runtime.Visibility.PRIVATE;
import static org.jruby.runtime.Visibility.PROTECTED;
import static org.jruby.runtime.Visibility.PUBLIC;

import static org.jruby.runtime.Visibility.UNDEFINED;
import static org.jruby.util.CommonByteLists.COLON_COLON;
import static org.jruby.util.RubyStringBuilder.str;
import static org.jruby.util.RubyStringBuilder.ids;
import static org.jruby.util.RubyStringBuilder.types;

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
    public static final int TEMPORARY_NAME = ObjectFlags.TEMPORARY_NAME;

    public static final String BUILTIN_CONSTANT = "";

    public static final ObjectAllocator MODULE_ALLOCATOR = RubyModule::new;

    public static void finishModuleClass(RubyClass Module) {
        Module.reifiedClass(RubyModule.class).
                kindOf(new RubyModule.JavaClassKindOf(RubyModule.class)).
                classIndex(ClassIndex.MODULE);
    }

    public static void finishCreateModuleClass(ThreadContext context, RubyClass Module) {
        Module.defineMethods(context, RubyModule.class, ModuleKernelMethods.class);
    }

    public void checkValidBindTargetFrom(ThreadContext context, RubyModule originModule, boolean fromBind) throws RaiseException {
        // Module methods can always be transplanted
        if (!originModule.isModule() && !hasModuleInHierarchy(originModule)) {
            if (originModule instanceof MetaClass) {
                throw typeError(context, "can't bind singleton method to a different class");
            } else {
                String thing = fromBind ? "an instance" : "a subclass"; // bind : define_method
                throw typeError(context, "bind argument must be " + thing + " of ", originModule, "");
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
     * Set the ClassIndex for this core class. Only used at boot time for core types.
     *
     * @param classIndex the ClassIndex for this type
     * @deprecated Use {@link org.jruby.RubyModule#classIndex(ClassIndex)} instead.
     */
    @Deprecated(since = "10.0")
    void setClassIndex(ClassIndex classIndex) {
        classIndex(classIndex);
    }

    @JRubyMethod
    public IRubyObject autoload(ThreadContext context, IRubyObject symbol, IRubyObject file) {
        final RubyString fileString = RubyFile.get_path(context, file);
        if (fileString.isEmpty()) throw argumentError(context, "empty file name");

        final String symbolStr = symbol.asJavaString();
        if (!IdUtil.isValidConstantName(symbol)) {
            throw nameError(context, "autoload must be constant name", symbol);
        }

        IRubyObject existingValue = fetchConstant(context, symbolStr);

        if (existingValue != null && existingValue != RubyObject.UNDEF) return context.nil;

        defineAutoload(context, symbolStr, fileString);

        return context.nil;
    }

    @JRubyMethod(name = "autoload?")
    public IRubyObject autoload_p(ThreadContext context, IRubyObject symbol) {
        return hasAutoload(context, checkID(context, symbol).idString(), true);
    }

    @JRubyMethod(name = "autoload?")
    public IRubyObject autoload_p(ThreadContext context, IRubyObject symbol, IRubyObject inherit) {
        return hasAutoload(context, checkID(context, symbol).idString(), inherit.isTrue());
    }

    public IRubyObject hasAutoload(ThreadContext context, String idString, boolean inherit) {
        for (RubyModule mod = this; mod != null; mod = mod.getSuperClass()) {
            final IRubyObject loadedValue = mod.fetchConstant(context, idString);

            if (loadedValue == UNDEF) {
                final RubyString file;

                Autoload autoload = mod.getAutoloadMap().get(idString);

                // autoload has been evacuated
                if (autoload == null) return context.nil;

                // autoload has been completed
                if (autoload.getValue() != null) return context.nil;

                file = autoload.getPath();

                // autoload is in progress on another thread
                if (autoload.ctx != null && !autoload.isSelf(context)) return file;

                // file load is in progress or file is already loaded
                if (!loadService(context).featureAlreadyLoaded(file.asJavaString())) return file;
            }

            if (!inherit) break;
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

            // Not sure how but this happens in test_objects_are_released_by_cache_map
            if (cl == null) return false;

            return cl.hasAncestor(type);
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

    public boolean hasAncestor(RubyModule type) {
        return searchAncestor(type.getDelegate().getOrigin()) != null;
    }

    public Map<String, ConstantEntry> getConstantMap() {
        Map<String, ConstantEntry> constants = getOrigin().constants;
        return constants == null ? Collections.EMPTY_MAP : constants;
    }

    public Map<String, ConstantEntry> getConstantMapForWrite() {
        Map<String, ConstantEntry> constants = this.constants;
        if (constants == null) {
            if (!CONSTANTS_HANDLE.compareAndSet(this, null, constants = new ConcurrentHashMap<>(2, 0.9f, 1))) {
                constants = this.constants;
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
        Map<String, Autoload> autoloads = this.autoloads;
        return autoloads == null ? Collections.EMPTY_MAP : autoloads;
    }

    protected Map<String, Autoload> getAutoloadMapForWrite() {
        Map<String, Autoload> autoloads = this.autoloads;
        if (autoloads == null) {
            if (!AUTOLOADS_HANDLE.compareAndSet(this, null, autoloads = new ConcurrentHashMap<>(2, 0.9f, 1))) {
                autoloads = this.autoloads;
            }
        }
        return autoloads;
    }

    private RubyClass.RubyClassSet getIncludingHierarchiesForRead() {
        RubyClass.RubyClassSet includingHierarchies = this.includingHierarchies;
        return includingHierarchies == null ? RubyClass.EMPTY_RUBYCLASS_SET : includingHierarchies;
    }

    protected RubyClass.RubyClassSet getIncludingHierarchiesForWrite() {
        RubyClass.RubyClassSet includingHierarchies = this.includingHierarchies;
        if (includingHierarchies == null) {
            if (!INCLUDING_HIERARCHIES_HANDLE.compareAndSet(this, null, includingHierarchies = new RubyClass.WeakRubyClassSet(4))) {
                includingHierarchies = this.includingHierarchies;
            }
        }
        return includingHierarchies;
    }

    @SuppressWarnings("unchecked")
    public void addIncludingHierarchy(IncludedModule hierarchy) {
        getIncludingHierarchiesForWrite().addClass(hierarchy);
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

        // track module instances separately, since they don't descend from BasicObject
        if (metaClass == runtime.getModule()) {
            runtime.addModule(this);
        }

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
    public RubyModule(Ruby runtime) {
        this(runtime, runtime.getModule());
    }

    public boolean needsImplementer() {
        return getFlag(NEEDSIMPL_F);
    }

    /** rb_module_new
     *
     */
    @Deprecated(since = "10.0")
    public static RubyModule newModule(Ruby runtime) {
        return new RubyModule(runtime);
    }

    @Deprecated(since = "10.0")
    public static RubyModule newModule(Ruby runtime, String name, RubyModule parent, boolean setParent, String file, int line) {
        return newModule(runtime.getCurrentContext(), name, parent, setParent, file, line);
    }

    /** rb_module_new/rb_define_module_id/rb_name_class/rb_set_class_path
     *
     *  This is used by IR to define a new module.
     */
    public static RubyModule newModule(ThreadContext context, String name, RubyModule parent, boolean setParent, String file, int line) {
        RubyModule module = defineModule(context).baseName(name);
        if (setParent) module.setParent(parent);
        if (file != null) {
            parent.setConstant(context, name, module, file, line);
        } else {
            parent.setConstant(context, name, module);
        }
        return module;
    }

    @Deprecated(since = "10.0")
    public static RubyModule newModule(Ruby runtime, String name, RubyModule parent, boolean setParent) {
        var context = runtime.getCurrentContext();
        RubyModule module = new RubyModule(runtime).baseName(name);
        if (setParent) module.setParent(parent);
        parent.setConstant(context, name, module);
        return module;
    }

    public static RubyModule newModuleBootstrap(Ruby runtime, String name, RubyModule parent) {
        var module = new RubyModule(runtime).baseName(name);
        parent.defineConstantBootstrap(name, module);
        return module;
    }

    /**
     * Return the class providers set for read operations.
     *
     * @return the class provider set or an empty set if there are no class providers
     */
    private Set<ClassProvider> getClassProvidersForRead() {
        Set<ClassProvider> classProviders = this.classProviders;
        return classProviders == null ? Collections.EMPTY_SET : classProviders;
    }

    /**
     * Return the class providers set for write operations.
     *
     * @return the class provider set suitable for updating
     */
    private Set<ClassProvider> getClassProvidersForWrite() {
        Set<ClassProvider> classProviders = this.classProviders;
        if (classProviders == null) {
            if (!CLASS_PROVIDERS_HANDLE.compareAndSet(this, null, classProviders = new CopyOnWriteArraySet<ClassProvider>())) {
                classProviders = this.classProviders;
            }
        }
        return classProviders;
    }

    // synchronized method per JRUBY-1173 (unsafe Double-Checked Locking)
    // FIXME: synchronization is still wrong in CP code
    public final synchronized void addClassProvider(ClassProvider provider) {
        getClassProvidersForWrite().add(provider);
    }

    public final synchronized void removeClassProvider(ClassProvider provider) {
        getClassProvidersForWrite().remove(provider);
    }

    private void checkForCyclicInclude(ThreadContext context, RubyModule m) throws RaiseException {
        if (isSameOrigin(m)) throw argumentError(context, "cyclic include detected");
    }

    protected void checkForCyclicPrepend(ThreadContext context, RubyModule m) throws RaiseException {
        if (isSameOrigin(m)) throw argumentError(context, getName(context) + " cyclic prepend detected " + m.getName(context));
    }

    private RubyClass searchProvidersForClass(ThreadContext context, String name, RubyClass superClazz) {
        Set<ClassProvider> classProviders = this.classProviders;
        if (classProviders == null) return null;

        RubyClass clazz;
        for (ClassProvider classProvider: classProviders) {
            if ((clazz = classProvider.defineClassUnder(context, this, name, superClazz)) != null) {
                return clazz;
            }
        }
        return null;
    }

    private RubyModule searchProvidersForModule(ThreadContext context, String name) {
        Set<ClassProvider> classProviders = this.classProviders;
        if (classProviders == null) return null;

        RubyModule module;
        for (ClassProvider classProvider: classProviders) {
            if ((module = classProvider.defineModuleUnder(context, this, name)) != null) {
                return module;
            }
        }
        return null;
    }

    /** Getter for property superClass.
     * @return Value of property superClass.
     */
    public RubyClass getSuperClass() {
        return superClass();
    }

    /**
     * @param superClass
     * @deprecated Use {@link RubyModule#superClass(RubyClass)} instead.
     */
    @Deprecated(since = "10.0")
    public void setSuperClass(RubyClass superClass) {
        superClass(superClass);
    }

    public RubyModule getParent() {
        return parent;
    }

    public void setParent(RubyModule parent) {
        this.parent = parent;
    }

    public RubyModule getMethodLocation() {
        RubyModule methodLocation = this.methodLocation;
        return methodLocation == null ? this : methodLocation;
    }

    public void setMethodLocation(RubyModule module){
        methodLocation = module;
    }

    public Map<String, DynamicMethod> getMethods() {
        Map<String, DynamicMethod> methods = this.methods;
        return methods == null ? Collections.EMPTY_MAP : methods;
    }

    public Map<String, DynamicMethod> getMethodsForWrite() {
        Map<String, DynamicMethod> methods = this.methods;
        if (methods == null) {
            if (!METHODS_HANDLE.compareAndSet(this, null, methods = new ConcurrentHashMap<>(2, 0.9f, 1))) {
                methods = this.methods;
            }
        }
        return methods;
    }

    @Deprecated(since = "10.0")
    public DynamicMethod putMethod(Ruby runtime, String id, DynamicMethod method) {
        return putMethod(getCurrentContext(), id, method);
    }

    /**
     * <p>Note: Internal API - only public as its used by generated code!</p>
     * @param context the current thread context
     * @param id identifier string (8859_1).  Matching entry in symbol table.
     * @param method
     * @return method
     */ // NOTE: used by AnnotationBinder
    public DynamicMethod putMethod(ThreadContext context, String id, DynamicMethod method) {
        RubyModule methodLocation = getMethodLocation();

        if (hasPrepends()) {
            method = method.dup();
            method.setImplementationClass(methodLocation);
        }

        DynamicMethod oldMethod = methodLocation.getMethodsForWrite().put(id, method);

        if (oldMethod != null && oldMethod.isRefined()) {
            methodLocation.getMethodsForWrite().put(id, new RefinedWrapper(method.getImplementationClass(), method.getVisibility(), id, method));
        }

        context.runtime.addProfiledMethod(id, method);
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
     * @deprecated Use {@link org.jruby.RubyModule#baseName(String)} instead.
     */
    @Deprecated(since = "10.0")
    public void setBaseName(String name) {
        baseName(name);
    }

    @Deprecated(since = "10.0")
    public String getName() {
        return getName(getCurrentContext());
    }

    /**
     * Generate a fully-qualified class name or a #-style name for anonymous and singleton classes.
     *
     * Ruby C equivalent = "classname"
     *
     * @return The generated class name
     */
    public String getName(ThreadContext context) {
        return cachedName != null ? cachedName : calculateName(context);
    }

    @Deprecated(since = "10.0")
    public RubyString rubyName() {
        return rubyName(getCurrentContext());
    }

    /**
     * Generate a fully-qualified class name or a #-style name for anonymous and singleton classes which
     * is properly encoded. The returned string is always frozen.
     *
     * @return a properly encoded class name.
     *
     * Note: getId() is only really valid for ASCII values.  This should be favored over using it.
     */
    public RubyString rubyName(ThreadContext context) {
        return cachedRubyName != null ? cachedRubyName : calculateRubyName(context);
    }

    @Deprecated(since = "10.0")
    public RubySymbol symbolName() {
        return symbolName(getCurrentContext());
    }

    /**
     * Generate a fully-qualified class name or a #-style name as a Symbol. If any element of the path is anonymous,
     * returns null.
     *
     * @return a properly encoded non-anonymous class path as a Symbol, or null.
     */
    public RubySymbol symbolName(ThreadContext context) {
        IRubyObject symbolName = cachedSymbolName;

        if (symbolName == null) {
            cachedSymbolName = symbolName = calculateSymbolName(context);
        }

        return symbolName == UNDEF ? null : (RubySymbol) symbolName;
    }

    @Deprecated(since = "10.0")
    public RubyString rubyBaseName() {
        return rubyBaseName(getCurrentContext());
    }

    public RubyString rubyBaseName(ThreadContext context) {
        String baseName = getBaseName();

        return baseName == null ? null : (RubyString) asSymbol(context, baseName).to_s(context);
    }

    private RubyString calculateAnonymousRubyName(ThreadContext context) {
        RubyString anonBase = newString(context, "#<"); // anonymous classes get the #<Class:0xdeadbeef> format
        anonBase.append(metaClass.getRealClass().rubyName(context)).append(newString(context, ":0x"));
        anonBase.append(newString(context, Integer.toHexString(System.identityHashCode(this)))).append(newString(context, ">"));

        return anonBase;
    }

    private RubyString calculateRubyName(ThreadContext context) {
        boolean cache = true;

        if (getBaseName() == null) return calculateAnonymousRubyName(context); // no name...anonymous!

        if (usingTemporaryName()) { // temporary name
            cachedRubyName = asSymbol(context, baseName).toRubyString(context);
            return cachedRubyName;
        }
        
        RubyClass Object = objectClass(context);
        List<RubyString> parents = new ArrayList<>(5);
        for (RubyModule p = getParent();
             p != null && p != Object && p != this; // Break out of cyclic namespaces like C::A = C2; C2::A = C (jruby/jruby#2314)
             p = p.getParent()) {

            RubyString name = p.rubyBaseName(context);

            // This is needed when the enclosing class or module is a singleton.
            // In that case, we generated a name such as null::Foo, which broke
            // Marshalling, among others. The correct thing to do in this situation
            // is to insert the generate the name of form #<Class:01xasdfasd> if
            // it's a singleton module/class, which this code accomplishes.
            if (name == null) {
                cache = false;
                name = p.rubyName(context);
            }

            parents.add(name);
        }

        RubyString fullName = buildPathString(context, parents);

        if (cache) cachedRubyName = fullName;

        return fullName;
    }

    /**
     * Calculate the module's name path as a Symbol. If any element in the path is anonymous, this will return null.
     *
     * Used primarily by Marshal.dump for class names.
     *
     * @return a non-anonymous class path as a Symbol, or null
     */
    private IRubyObject calculateSymbolName(ThreadContext context) {
        if (getBaseName() == null) return UNDEF;

        if (usingTemporaryName()) return asSymbol(context, baseName);

        RubyClass Object = objectClass(context);
        List<RubyString> parents = new ArrayList<>(5);
        for (RubyModule p = getParent();
             p != null && p != Object && p != this; // Break out of cyclic namespaces like C::A = C2; C2::A = C (jruby/jruby#2314)
             p = p.getParent()) {

            RubyString name = p.rubyBaseName(context);

            if (name == null) {
                return UNDEF;
            }

            parents.add(name);
        }

        RubyString fullName = buildPathString(context, parents);

        return fullName.intern(context);
    }

    private RubyString buildPathString(ThreadContext context, List<RubyString> parents) {
        RubyString colons = newString(context, "::");
        RubyString fullName = context.runtime.newString();       // newString creates empty ByteList which ends up as
        fullName.setEncoding(USASCIIEncoding.INSTANCE);  // ASCII-8BIT.  8BIT is unfriendly to string concats.
        for (int i = parents.size() - 1; i >= 0; i--) {
            RubyString rubyString = fullName.catWithCodeRange(parents.get(i));
            rubyString.catWithCodeRange(colons);
        }
        fullName.catWithCodeRange(rubyBaseName(context));

        fullName.setFrozen(true);
        return fullName;
    }

    @Deprecated(since = "10.0")
    public String getSimpleName() {
        return getSimpleName(getCurrentContext());
    }

    /**
     * Get the "simple" name for the class, which is either the "base" name or
     * the "anonymous" class name.
     *
     * @return the "simple" name of the class
     */
    public String getSimpleName(ThreadContext context) {
        return baseName != null ? baseName : calculateAnonymousName(context);
    }

    /**
     * Recalculate the fully-qualified name of this class/module.
     */
    private String calculateName(ThreadContext context) {
        boolean cache = true;

        // we are anonymous, use anonymous name
        if (getBaseName() == null) return calculateAnonymousName(context);

        String name = getBaseName();
        var Object = objectClass(context);

        // First, we count the parents
        int parentCount = 0;
        for (RubyModule p = getParent() ; p != null && p != Object ; p = p.getParent()) {
            // Break out of cyclic namespaces like C::A = C2; C2::A = C (jruby/jruby#2314)
            if (p == this) break;

            parentCount++;
        }

        // Allocate a String array for all of their names and populate it
        String[] parentNames = new String[parentCount];
        int i = parentCount - 1;
        int totalLength = name.length() + parentCount * 2; // name length + enough :: for all parents
        for (RubyModule p = getParent() ; p != null && p != Object ; p = p.getParent(), i--) {
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
                pName = p.getName(context);
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

    private String calculateAnonymousName(ThreadContext context) {
        String cachedName = this.cachedName;
        if (cachedName == null) {
            // anonymous classes get the #<Class:0xdeadbeef> format
            cachedName = this.cachedName = "#<" + anonymousMetaNameWithIdentifier(context) + '>';
        }
        return cachedName;
    }

    @JRubyMethod(required = 1)
    public IRubyObject set_temporary_name(ThreadContext context, IRubyObject arg) {
        if (baseName != null && IdUtil.isValidConstantName(baseName) && (parent == null || parent.baseName != null)) {
            throw runtimeError(context, "can't change permanent name");
        }

        if (arg.isNil()) {
            baseName(null);
        } else {
            RubyString name = arg.convertToString();

            if (name.length() == 0) throw argumentError(context, "empty class/module name");
            if (isValidConstantPath(name)) throw argumentError(context, "the temporary name must not be a constant path to avoid confusion");

            setFlag(TEMPORARY_NAME, true);

            // We make sure we generate ISO_8859_1 String and also guarantee when we want to print this name
            // later it does not lose track of the orignal encoding.
            baseName(asSymbol(context, name.getByteList()).idString());
        }

        return this;
    }

    String anonymousMetaNameWithIdentifier(ThreadContext context) {
        return metaClass.getRealClass().getName(context) + ":0x" + Integer.toHexString(System.identityHashCode(this));
    }

    @JRubyMethod(name = "refine", reads = SCOPE)
    public IRubyObject refine(ThreadContext context, IRubyObject klass, Block block) {
        if (!block.isGiven()) throw argumentError(context, "no block given");
        if (block.isEscaped()) throw argumentError(context, "can't pass a Proc as a block to Module#refine");
        if (!(klass instanceof RubyModule)) throw typeError(context, klass, "Class or Module");

        RefinementStore refinementStore = getRefinementStoreForWrite();

        RubyModule moduleToRefine = (RubyModule) klass;
        RubyModule refinement = refinementStore.refinements.get(moduleToRefine);
        if (refinement == null) {
            refinement = createNewRefinedModule(context, moduleToRefine);

            // Add it to the activated chain of other refinements already added to this class we are refining
            addActivatedRefinement(context, moduleToRefine, refinement);
        }

        // Executes the block supplied with the defined method definitions using the refinement as it's module.
        yieldRefineBlock(context, refinement, block);

        return refinement;
    }

    RefinementStore getRefinementStoreForWrite() {
        RefinementStore refinementStore = this.refinementStore;
        if (refinementStore == null) {
            if (!REFINEMENT_STORE_HANDLE.compareAndSet(this, null, refinementStore = new RefinementStore())) {
                refinementStore = this.refinementStore;
            }
        }
        return refinementStore;
    }

    private RubyModule createNewRefinedModule(ThreadContext context, RubyModule klass) {
        Ruby runtime = context.runtime;

        RubyModule newRefinement = new RubyModule(runtime, runtime.getRefinement()).
                superClass(refinementSuperclass(context, klass));
        newRefinement.setFlag(REFINED_MODULE_F, true);
        newRefinement.setFlag(NEEDSIMPL_F, false); // Refinement modules should not do implementer check
        RefinementStore newRefinementStore = newRefinement.getRefinementStoreForWrite();
        newRefinementStore.refinedClass = klass;
        newRefinementStore.definedAt = this;
        getRefinementStoreForWrite().refinements.put(klass, newRefinement);

        return newRefinement;
    }

    private static RubyClass refinementSuperclass(ThreadContext context, RubyModule superClass) {
        if (superClass.isModule()) {
            return new IncludedModuleWrapper(context.runtime, basicObjectClass(context), superClass);
        } else {
            return (RubyClass) superClass;
        }
    }

    private void yieldRefineBlock(ThreadContext context, RubyModule refinement, Block block) {
        block = block.cloneBlockAndFrame(EvalType.MODULE_EVAL);

        block.getBinding().setSelf(refinement);

        RubyModule overlayModule = block.getBody().getStaticScope().getOverlayModuleForWrite(context);
        overlayModule.getRefinementStoreForWrite().refinements = getRefinementStoreForWrite().refinements;

        block.yieldSpecific(context);
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
        RefinementStore myRefinementStore = getRefinementStoreForWrite();
        RubyClass c = myRefinementStore.activatedRefinements.get(moduleToRefine);
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
        RefinementStore otherRefinementStore = c.getRefinementStoreForWrite();
        otherRefinementStore.refinedClass = moduleToRefine;
        for (refinement = refinement.getSuperClass(); refinement != null; refinement = refinement.getSuperClass()) {
            refinement.setFlag(IS_OVERLAID_F, true);
            c.superClass(new IncludedModuleWrapper(context.runtime, c.getSuperClass(), refinement));
            c = c.getSuperClass();
            otherRefinementStore.refinedClass = moduleToRefine;
        }
        myRefinementStore.activatedRefinements.put(moduleToRefine, iclass);
    }

    @JRubyMethod(name = "using", visibility = PRIVATE, reads = {SELF, SCOPE})
    public IRubyObject using(ThreadContext context, IRubyObject refinedModule) {
        if (context.getFrameSelf() != this) throw runtimeError(context, "Module#using is not called on self");
        if (context.getCurrentStaticScope().isWithinMethod()) {
            throw runtimeError(context, "Module#using is not permitted in methods");
        }

        // I pass the cref even though I don't need to so that the concept is simpler to read
        StaticScope staticScope = context.getCurrentStaticScope();
        RubyModule overlayModule = staticScope.getOverlayModuleForWrite(context);
        usingModule(context, overlayModule, refinedModule);

        return this;
    }

    // mri: rb_using_module
    public static void usingModule(ThreadContext context, RubyModule cref, IRubyObject refinedModule) {
        usingModuleRecursive(context, cref, castAsModule(context, refinedModule));
    }

    // mri: using_module_recursive
    private static void usingModuleRecursive(ThreadContext context, RubyModule cref, RubyModule module) {
        RubyClass superClass = module.getSuperClass();

        // For each superClass of the refined module also use their refinements for the given cref
        if (superClass != null) usingModuleRecursive(context, cref, superClass);

        if (module instanceof DelegatedModule) {
            module = module.getDelegate();
        } else if (!module.isModule()) {
            throw typeError(context, module, "Module");
        }

        RefinementStore refinementStore = module.refinementStore;
        if (refinementStore == null) return; // No refinements registered for this module

        Map<RubyModule, RubyModule> refinements = refinementStore.refinements;
        for (Map.Entry<RubyModule, RubyModule> entry: refinements.entrySet()) {
            usingRefinement(context, cref, entry.getKey(), entry.getValue());
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
    private static void usingRefinement(ThreadContext context, RubyModule cref, RubyModule klass, RubyModule module) {
        RubyModule iclass, c, superclass = klass;

        RefinementStore crefRefinementStore = cref.getRefinementStoreForWrite();
        if (cref.getFlag(OMOD_SHARED)) {
            crefRefinementStore.refinements = newRefinementsMap(crefRefinementStore.refinements);
            cref.setFlag(OMOD_SHARED, false);
        }
        if ((c = crefRefinementStore.refinements.get(klass)) != null) {
            superclass = c;
            while (c != null && c instanceof IncludedModule) {
                if (c.getOrigin() == module) {
                    /* already used refinement */
                    return;
                }
                c = c.getSuperClass();
            }
        }

        module.setFlag(IS_OVERLAID_F, true);
        superclass = refinementSuperclass(context, superclass);
        c = iclass = new IncludedModuleWrapper(context.runtime, (RubyClass) superclass, module);
        c.getRefinementStoreForWrite().refinedClass = klass;

//        RCLASS_M_TBL(OBJ_WB_UNPROTECT(c)) =
//                RCLASS_M_TBL(OBJ_WB_UNPROTECT(module)); /* TODO: check unprotecting */

        module = module.getSuperClass();
        while (module != null && module != klass) {
            module.setFlag(IS_OVERLAID_F, true);
            c.superClass(new IncludedModuleWrapper(context.runtime, c.getSuperClass(), module));
            c = c.getSuperClass();
            c.getRefinementStoreForWrite().refinedClass = klass;
            module = module.getSuperClass();
        }

        crefRefinementStore.refinements.put(klass, iclass);
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
        var ary = newArray(context);
        while (cref != null) {
            RubyModule overlay;
            if ((overlay = cref.getOverlayModuleForRead()) != null) {
                RefinementStore overlayRefinementStore = overlay.refinementStore;
                if (overlayRefinementStore != null && !overlayRefinementStore.refinements.isEmpty()) {
                    overlayRefinementStore.refinements.forEach((mod, x) -> {
                        while (mod != null && mod.getOrigin().isRefinement()) {
                            ary.push(context, mod.getOrigin().getRefinementStoreForWrite().definedAt);
                            mod = mod.getSuperClass();
                        }
                    });
                }
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
        var context = getCurrentContext();
        IncludedModuleWrapper includedModule = new IncludedModuleWrapper(context.runtime, superClazz, this);

        // include its parent (and in turn that module's parents)
        if (getSuperClass() != null) includedModule.includeModule(context, getSuperClass());

        return includedModule;
    }

    @Deprecated(since = "10.0")
    public RubyModule getModule(String name) {
        return getModule(getCurrentContext(), name);
    }

    /**
     * Finds a module that is within the current module (or class).
     *
     * @param name to be found in this module (or class)
     * @return the module or null if no such module
     * @since 9.2
     */
    public RubyModule getModule(ThreadContext context, String name) {
        return (RubyModule) getConstantAt(context, name);
    }

    @Deprecated(since = "10.0")
    public RubyClass getClass(String name) {
        return getClass(getCurrentContext(), name);
    }

    /**
     * Finds a class that is within the current module/class.  Also consider using
     * the various methods in {@link org.jruby.api.Access} when they are core types
     * to avoid a hash lookup.
     *
     * @param context the current thread context
     * @param name to be found in this module (or class)
     * @return the class or null if no such class
     */
    @JRubyAPI
    public RubyClass getClass(ThreadContext context, String name) {
        return (RubyClass) getConstantAt(context, name);
    }

    @Deprecated
    public RubyClass fastGetClass(String internedName) {
        return getClass(getCurrentContext(), internedName);
    }

    @Deprecated(since = "10.0")
    public void prependModule(RubyModule module) {
        prependModule(getCurrentContext(), module);
    }

    /**
     * Prepend a new module to this module or class.
     *
     * MRI: rb_prepend_module
     *
     * @param module The module to include
     */
    public void prependModule(ThreadContext context, RubyModule module) {
        testFrozen("module");

        RefinementStore moduleRefinementStore = module.refinementStore;
        if (moduleRefinementStore != null && moduleRefinementStore.refinedClass != null) throw typeError(context, "Cannot prepend refinement");

        // Make sure the module we include does not already exist
        checkForCyclicPrepend(context, module);

        synchronized (getRuntime().getHierarchyLock()) {
            if (hasModuleInPrepends(module)) {
                invalidateCacheDescendants(context);
                return;
            }

            doPrependModule(context, module);

            if (this.isModule()) {
                getIncludingHierarchiesForRead().forEachClass(new RubyClass.BiConsumerIgnoresSecond<RubyClass>() {
                    boolean doPrepend = true;

                    public void accept(RubyClass includeClass) {
                        RubyClass checkClass = includeClass;
                        while (checkClass != null) {
                            if (checkClass instanceof IncludedModule && checkClass.getOrigin() == module) {
                                doPrepend = false;
                            }
                            checkClass = checkClass.superClass;
                        }

                        if (doPrepend) {
                            includeClass.doPrependModule(context, module);
                        }
                    }
                });
            }

            invalidateCacheDescendants(context);
            invalidateConstantCacheForModuleInclusion(context, module);
        }
    }

    @Deprecated
    public void prependModule(IRubyObject arg) {
        assert arg != null;
        prependModule(castAsModule(getCurrentContext(), arg));
    }

    @Deprecated(since = "10.0")
    public synchronized void includeModule(IRubyObject arg) {
        includeModule(getCurrentContext(), arg);
    }

    /**
     * Include a new module in this module or class.
     *
     * @param arg The module to include
     */
    public void includeModule(ThreadContext context, IRubyObject arg) {
        assert arg != null;

        testFrozen("module");

        // This cannot use castToModule because includeModule happens before first ThreadContext exists.
        if (!(arg instanceof RubyModule)) typeError(context, arg, "Module");
        RubyModule module = (RubyModule) arg;

        if (module.isRefinement()) throw typeError(context, "Cannot include refinement");

        // Make sure the module we include does not already exist
        checkForCyclicInclude(context, module);

        synchronized (getRuntime().getHierarchyLock()) {
            doIncludeModule(context, module);

            if (this.isModule()) {
                getIncludingHierarchiesForRead().forEachClass(new RubyClass.BiConsumerIgnoresSecond<RubyClass>() {
                    boolean doInclude = true;

                    @Override
                    public void accept(RubyClass includeClass) {
                        RubyClass checkClass = includeClass;
                        while (checkClass != null) {
                            if (checkClass instanceof IncludedModule && checkClass.getOrigin() == module) {
                                doInclude = false;
                            }
                            checkClass = checkClass.superClass;
                        }
                        if (doInclude) {
                            includeClass.doIncludeModule(context, module);
                        }
                    }
                });
            }

            invalidateCacheDescendants(context);
            invalidateConstantCacheForModuleInclusion(context, module);
        }
    }

    /**
     * @param clazz
     * @param name
     * @deprecated Use {@link RubyModule#defineMethods(ThreadContext, Class[])} and organize your method
     * definitions where you only specify the .class and not needing to add a name as a discriminator.  This
     * method is fairly inefficient since it is rescanning the class for all methods to find a single version.
     */
    @Deprecated(since = "10.0")
    public void defineAnnotatedMethod(Class clazz, String name) {
        // FIXME: This is probably not very efficient, since it loads all methods for each call
        boolean foundMethod = false;
        var JRubyClassLoader = getCurrentContext().runtime.getJRubyClassLoader();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(name) && defineAnnotatedMethod(method, MethodFactory.createFactory(JRubyClassLoader))) {
                foundMethod = true;
            }
        }

        if (!foundMethod) {
            throw new RuntimeException("No JRubyMethod present for method " + name + "on class " + clazz.getName());
        }
    }

    /**
     * @param clazz
     * @deprecated Use {@link RubyModule#defineConstants(ThreadContext, Class)} instead.
     */
    @Deprecated(since = "10.0")
    public final void defineAnnotatedConstants(Class clazz) {
        defineConstants(getCurrentContext(), clazz);
    }

    /**
     * @param field
     * @return ""
     * @deprecated Use {@link RubyModule#defineAnnotatedConstant(ThreadContext, Field)} instead.
     */
    @Deprecated(since = "10.0")
    public final boolean defineAnnotatedConstant(Field field) {
        return defineAnnotatedConstant(getCurrentContext(), field);
    }

    private boolean defineAnnotatedConstant(ThreadContext context, Field field) {
        JRubyConstant jrubyConstant = field.getAnnotation(JRubyConstant.class);
        if (jrubyConstant == null) return false;

        Class tp = field.getType();
        IRubyObject realVal;

        try {
            if(tp == Integer.class || tp == Integer.TYPE || tp == Short.class || tp == Short.TYPE || tp == Byte.class || tp == Byte.TYPE) {
                realVal = asFixnum(context, field.getInt(null));
            } else if(tp == Boolean.class || tp == Boolean.TYPE) {
                realVal = asBoolean(context, field.getBoolean(null));
            } else {
                realVal = context.nil;
            }
        } catch(Exception e) {
            realVal = context.nil;
        }

        String[] names = jrubyConstant.value();
        if (names.length == 0) {
            defineConstant(context, field.getName(), realVal);
        } else {
            for (String name : names) {
                defineConstant(context, name, realVal);
            }
        }

        return true;
    }

    /**
     * @param clazz
     * @deprecated Use {@link RubyModule#defineMethods(ThreadContext, Class[])} instead.
     */
    @Extension
    @Deprecated(since = "10.0")
    public void defineAnnotatedMethods(Class clazz) {
        defineAnnotatedMethodsIndividually(clazz);
    }

    public static final class MethodClumper {
        private HashMap<String, List<JavaMethodDescriptor>> annotatedMethods;
        private HashMap<String, List<JavaMethodDescriptor>> staticAnnotatedMethods;

        public Map<Set<FrameField>, List<String>> readGroups = Collections.EMPTY_MAP;
        public Map<Set<FrameField>, List<String>> writeGroups = Collections.EMPTY_MAP;

        public void clump(final Class klass) {
            clump(MethodGatherer.DECLARED_METHODS.get(klass));
        }

        @SuppressWarnings("deprecation")
        public void clump(final Method[] declaredMethods) {
            for (Method method: declaredMethods) {
                JRubyMethod anno = method.getAnnotation(JRubyMethod.class);

                if (anno == null) continue;

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

    @Deprecated(since = "10.0")
    public final void defineAnnotatedMethodsIndividually(Class clazz) {
        var context = getCurrentContext();
        context.runtime.POPULATORS.get(clazz).populate(context, this, clazz);
    }

    /**
     * Note: it is your responsibility to only pass methods which are annotated with @JRubyMethod. It will
     * perform this check when only a single method is passed in but has never checked when more than one.
     * @deprecated Use {@link RubyModule#defineMethods(ThreadContext, Class[])} instead and organize your
     * code around all JRubyMethod annotations in that .class being defined.
     */
    @Deprecated(since = "10.0")
    public final boolean defineAnnotatedMethod(String name, List<JavaMethodDescriptor> methods, MethodFactory methodFactory) {
        if (methods.size() == 1 && methods.get(0).anno == null) return false;
        defineAnnotatedMethod(getCurrentContext(), name, methods, methodFactory);
        return true;
    }

    /**
     * This is an internal API used by the type populator.  This method takes a list of overloads
     * for a static or non-static method and generates the
     * @param context the current method context
     * @param name
     * @param methods
     * @param methodFactory
     */
    public final void defineAnnotatedMethod(ThreadContext context, String name, List<JavaMethodDescriptor> methods, MethodFactory methodFactory) {
        JavaMethodDescriptor desc = methods.get(0);

        var dynamicMethod = methods.size() == 1 ?
                methodFactory.getAnnotatedMethod(this, desc, name) :
                methodFactory.getAnnotatedMethod(this, methods, name);

        define(context, this, desc, dynamicMethod);
    }

    @Deprecated(since = "10.0")
    public final boolean defineAnnotatedMethod(Method method, MethodFactory methodFactory) {
        if (method.getAnnotation(JRubyMethod.class) == null) return false;

        JavaMethodDescriptor desc = new JavaMethodDescriptor(method);
        DynamicMethod dynamicMethod = methodFactory.getAnnotatedMethod(this, desc, method.getName());
        define(getCurrentContext(), this, desc, dynamicMethod);

        return true;
    }

    @Deprecated(since = "10.0")
    public final boolean defineAnnotatedMethod(String name, JavaMethodDescriptor desc, MethodFactory methodFactory) {
        if (desc.anno == null) return false;

        DynamicMethod dynamicMethod = methodFactory.getAnnotatedMethod(this, desc, name);
        define(getCurrentContext(), this, desc, dynamicMethod);

        return true;
    }

    /**
     * Set the base name of the class. If null, the class effectively becomes
     * anonymous (though constants elsewhere may reference it).
     * @param name the new base name of the class
     * @return itself for a composable API.
     */
    @JRubyAPI
    public <T extends RubyModule> T baseName(String name) {
        baseName = name;
        cachedName = null;
        cachedRubyName = null;

        return (T) this;
    }

    /**
     * Sets the ClassIndex for this type
     * @param classIndex to be set
     * @return itself for composable API
     */
    @JRubyAPI
    public <T extends RubyModule> T classIndex(ClassIndex classIndex) {
        this.classIndex = classIndex;
        this.index = classIndex.ordinal();
        return (T) this;
    }

    /**
     * Define a Class under this Class/Module.
     *
     * @param context the current thread context
     * @param name name of the new class
     * @param superClass superclass of this new class
     * @param allocator how to allocate an instance of this class
     * @return the new class
     */
    // MRI: rb_define_class_under
    @JRubyAPI
    public <T extends RubyClass> T defineClassUnder(ThreadContext context, String name, RubyClass superClass,
                                                    ObjectAllocator allocator) {
        return (T) context.runtime.defineClassUnder(context, name, superClass, allocator, this, null);
    }

    /**
     * Define constant for your module/class with the supplied Class which contains @JRubyMethod annotations.
     *
     * @param context
     * @param constantSource class containing the method annotations
     * @return itself for composable API
     */
    @JRubyAPI
    public <T extends RubyModule> T defineConstants(ThreadContext context, Class constantSource) {
        for (Field field : constantSource.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                defineAnnotatedConstant(context, field);
            }
        }
        return (T) this;
    }

    /**
     * Define all constants from the given jnr-constants enum which are defined on the current platform.
     *
     * @param context the current thread context
     * @param enumClass the enum class of the constants to define
     * @param <C> the enum type, which must implement {@link Constant}.
     * @return itself for composable API
     */
    @JRubyAPI
    public <C extends Enum<C> &Constant, T extends RubyModule> T defineConstantsFrom(ThreadContext context, Class<C> enumClass) {
        for (C constant : EnumSet.allOf(enumClass)) {
            String name = constant.name();
            if (constant.defined() && Character.isUpperCase(name.charAt(0))) {
                defineConstant(context, name, asFixnum(context, constant.intValue()));
            }
        }

        return (T) this;
    }

    /**
     * Define a module under this module.
     *
     * @param context
     * @param name
     * @return itself for a composable API
     */
    // MRI: rb_define_module_under
    @JRubyAPI
    public RubyModule defineModuleUnder(ThreadContext context, String name) {
        return context.runtime.defineModuleUnder(context, name, this);
    }

    /**
     * Define methods for your module/class with the supplied Class which contains @JRubyMethod annotations.
     *
     * @param context
     * @param methodSources class containing the method annotations
     * @return itself for composable API
     */
    @JRubyAPI
    public <T extends RubyModule> T defineMethods(ThreadContext context, Class... methodSources) {
        var populators = context.runtime.POPULATORS;

        for (var clazz : methodSources) {
            populators.get(clazz).populate(context, this, clazz);
        }
        return (T) this;
    }

    /**
     * In Defining this type include a module.  Note: This is for defining native extensions
     * and differs from method of same name which is live include while executing Ruby code.
     *
     * @param context
     * @param module  to be included
     * @return itself for composable API
     */
    @JRubyAPI
    public <T extends RubyModule> T include(ThreadContext context, RubyModule module) {
        includeModule(context, module);
        return (T) this;
    }

    /**
     * Set the method for determining whether an Object is a kind of the supplied type.
     *
     * @param kindOf method to determine kind-of status
     * @return itself for composblae API
     * @param <T>  class or module types
     */
    @JRubyAPI
    public <T extends RubyModule> T kindOf(KindOf kindOf) {
        this.kindOf = kindOf;
        return (T) this;
    }

    /**
     * Get this module/class super class.
     * @return the super class
     */
    @JRubyAPI
    public RubyClass superClass() {
        return superClass;
    }

    /**
     * Set this module/class super class.
     * @param superClass to be set
     * @return itself for composable API
     * @param <T> class or module type
     */
    @JRubyAPI
    public <T extends RubyModule> T superClass(RubyClass superClass) {
        // update superclass reference
        this.superClass = superClass;
        if (superClass != null && superClass.isSynchronized()) becomeSynchronized();
        return (T) this;
    }

    /**
     * Provide itself to a lambda then return itself.
     *
     * @param consumer to be given this
     * @return this
     * @param <T> A module or class
     */
    @JRubyAPI
    public <T extends RubyModule> T tap(Consumer<T> consumer) {
        consumer.accept((T) this);
        return (T) this;
    }

    /**
     * Undefine a method from this type.
     *
     * @param context
     * @param names   the methods to undefine
     * @return this type for composable API
     */
    @JRubyAPI
    public <T extends RubyModule> T undefMethods(ThreadContext context, String... names) {
        // FIXME: context needs to feed down into undefineMethod so we are not getting runtime
        for (String name : names) {
            getMethodLocation().addMethod(context, name, UndefinedMethod.getInstance());
        }
        return (T) this;
    }

    /**
     * @deprecated Use {@link RubyModule#undefMethods(ThreadContext, String...)} instead.
     */
    @Deprecated(since = "10.0")
    public void undefineMethod(String name) {
        getMethodLocation().addMethod(getCurrentContext(), name, UndefinedMethod.getInstance());
    }

    /** rb_undef
     *
     */
    public void undef(ThreadContext context, String name) {
        testFrozen("module");
        if (name.equals("__send__") || name.equals("object_id") || name.equals("initialize")) {
            warn(context, "undefining '"+ name +"' may cause serious problems");
        }

        if (name.equals("method_missing")) {
            IRubyObject oldExc = context.getErrorInfo(); // Save $!
            try {
                removeMethod(context, name);
            } catch (RaiseException t) {
                if (!(t.getException() instanceof RubyNameError)) throw t;

                context.setErrorInfo(oldExc); // Restore $!
            }
            return;
        }

        DynamicMethod method = searchMethod(name);
        if (method.isUndefined()) raiseUndefinedNameError(context, name);
        getMethodLocation().addMethod(context, name, UndefinedMethod.getInstance());

        methodUndefined(context, asSymbol(context, name));
    }

    private void raiseUndefinedNameError(ThreadContext context, String name) {
        String s0 = " class";
        RubyModule c = this;

        if (c.isSingleton()) {
            IRubyObject obj = ((MetaClass) c).getAttached();

            if (obj instanceof RubyClass) {
                c = (RubyModule) obj;
            } else if (obj instanceof RubyModule) {
                s0 = "";
            }
        } else if (c.isModule()) {
            s0 = " module";
        }

        // FIXME: Since we found no method we probably do not have symbol entry...do not want to pollute symbol table here.
        throw nameError(context, str(context.runtime, "undefined method '" + name + "' for" + s0 + " '", c, "'"), name);
    }

    @JRubyMethod(name = "include?")
    public IRubyObject include_p(ThreadContext context, IRubyObject arg) {
        if (!arg.isModule()) throw typeError(context, arg, "Module");
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
        return asBoolean(context, isSingleton());
    }

    private String frozenType() {
        return isClass() ? "class" : "module";
    }

    @Deprecated(since = "10.0")
    public void addMethod(String id, DynamicMethod method) {
        addMethod(getCurrentContext(), id, method);
    }

    public void addMethod(ThreadContext context, String id, DynamicMethod method) {
        if (this instanceof MetaClass) {
            // FIXME: Gross and not quite right. See MRI's rb_frozen_class_p logic
            ((MetaClass) this).getAttached().testFrozen();
        } else {
            testFrozen(frozenType());
        }

        RubyModule methodLocation = this.methodLocation;
        if (methodLocation != null) {
            methodLocation.addMethod(context, id, method);
            return;
        }

        // create refined entry on target class
        if (isRefinement()) addRefinedMethodEntry(context, id, method);

        addMethodInternal(context, id, method);
    }

    // MRI: rb_add_refined_method_entry
    private void addRefinedMethodEntry(ThreadContext context, String id, DynamicMethod method) {
        RubyModule refinedClass = refinementStore.refinedClass;
        RubyModule methodLocation = refinedClass.getMethodLocation();
        DynamicMethod orig = methodLocation.searchMethodCommon(id);

        if (orig == null) {
            refinedClass.addMethod(context, id, new RefinedMarker(methodLocation, method.getVisibility(), id));
        } else {
            if (orig.isRefined()) {
                return;
            }
            refinedClass.addMethod(context, id, new RefinedWrapper(methodLocation, method.getVisibility(), id, orig));
        }
    }

    @Deprecated(since = "10.0")
    public final void addMethodInternal(String name, DynamicMethod method) {
        addMethodInternal(getCurrentContext(), name, method);
    }

    public final void addMethodInternal(ThreadContext context, String name, DynamicMethod method) {
        synchronized (getMethodLocation().getMethodsForWrite()) {
            putMethod(context, name, method);
        }

        synchronized (getRuntime().getHierarchyLock()) {
            invalidateCacheDescendants(context);
        }
    }

    public void removeMethod(ThreadContext context, String id) {
        testFrozen("class/module");

        switch (id) {
            case "object_id"  : warnMethodRemoval(context, id); break;
            case "__send__"   : warnMethodRemoval(context, id); break;
            case "initialize" : warnMethodRemoval(context, id); break;
        }

        RubySymbol name = asSymbol(context, id);
        // We can safely reference methods here instead of doing getMethods() since if we
        // are adding we are not using a IncludedModule.
        Map<String, DynamicMethod> methodsForWrite = getMethodLocation().getMethodsForWrite();
        synchronized (methodsForWrite) {
            DynamicMethod method = methodsForWrite.get(id);
            if (method == null ||
                    method.isUndefined() ||
                    method instanceof RefinedMarker) {
                throw nameError(context, str(context.runtime, "method '", name, "' not defined in ", rubyName(context)), id);
            }

            method = methodsForWrite.remove(id);

            if (method.isRefined()) {
                methodsForWrite.put(id, new RefinedMarker(method.getImplementationClass(), method.getVisibility(), id));
            }
        }

        synchronized (getRuntime().getHierarchyLock()) {
            invalidateCacheDescendants(context);
        }

        methodRemoved(context, name);
    }

    private static void warnMethodRemoval(final ThreadContext context, final String id) {
        warn(context, str(context.runtime, "removing '", ids(context.runtime, id), "' may cause serious problems"));
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
        return searchWithCacheAndRefinements(name, null);
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
        return searchWithCacheAndRefinements(name, refinedScope);
    }

    @Deprecated
    public final CacheEntry searchWithCache(String id, boolean cacheUndef) {
        final CacheEntry entry = cacheHit(id);
        return entry != null ? entry : searchWithCacheMiss(getRuntime(), id);
    }

    // MRI: method_entry_resolve_refinement
    private CacheEntry searchWithCacheAndRefinements(String id, StaticScope refinedScope) {
        CacheEntry entry = cacheHit(id);
        if (entry == null) {
            entry = searchWithCacheMiss(getRuntime(), id);
        }

        if (entry.method.isRefined()) {
            // FIXME: We walk up scopes to look for refinements, while MRI seems to copy from parent to child on push
            // CON: Walk improved to only walk up to nearest refined scope, since methods/classes/modules will copy parent's
            for (; refinedScope != null; refinedScope = refinedScope.getEnclosingScope()) {
                // any refined target with scope available
                RubyModule overlay = refinedScope.getOverlayModuleForRead();

                if (overlay == null) continue;

                CacheEntry maybeEntry = resolveRefinedMethod(overlay, entry, id);

                if (maybeEntry.method.isUndefined()) continue;

                return maybeEntry;
            }

            // MRI: refined_method_original_method_entry
            return resolveRefinedMethod(null, entry, id);
        }

        return entry;
    }

    // MRI: refined_method_original_method_entry
    private CacheEntry refinedMethodOriginalMethodEntry(RubyModule overlay, String id, CacheEntry entry) {
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
            return resolveRefinedMethod(overlay, superClass.searchWithCache(id), id);
        }
    }

    /**
     * Search through this module and supermodules for method definitions after {@link RubyModule#cacheHit(String)}
     * failed to return a result. Cache superclass definitions in this class.
     *
     * MRI: method_entry_get_without_cache
     * 
     * @param id The name of the method to search for
     * @return The method, or UndefinedMethod if not found
     */
    private CacheEntry searchWithCacheMiss(Ruby runtime, final String id) {
        // we grab serial number first; the worst that will happen is we cache a later
        // update with an earlier serial number, which would just flush anyway
        final int token = generation;

        CacheEntry methodEntry = searchMethodEntryInner(id);

        if (methodEntry == null) {
            return addToCache(id, UndefinedMethod.getInstance(), this, token);
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

    protected final Map<String, CacheEntry> getCachedMethods() {
        Map<String, CacheEntry> cachedMethods = this.cachedMethods;
        return cachedMethods == null ? Collections.EMPTY_MAP : cachedMethods;
    }

    private final Map<String, CacheEntry> getCachedMethodsForWrite() {
        Map<String, CacheEntry> myCachedMethods = this.cachedMethods;
        if (myCachedMethods == null) {
            // concurrent creation of this map does not impact behavior
            myCachedMethods = this.cachedMethods = new ConcurrentHashMap<>(0, 0.75f, 1);
        }
        return myCachedMethods;
    }

    private CacheEntry cacheHit(String name) {
        CacheEntry cacheEntry = getMethodLocation().getCachedMethods().get(name);

        if (cacheEntry != null) {
            if (cacheEntry.token == getGeneration()) {
                return cacheEntry;
            }
        }

        return null;
    }

    private void invalidateConstantCacheForModuleInclusion(ThreadContext context, RubyModule module) {
        Map<String, Invalidator> invalidators = null;
        for (RubyModule mod : gatherModules(module)) {
            for (String name : mod.getConstantMap().keySet()) {
                if (invalidators == null) invalidators = new HashMap<>();
                invalidators.put(name, context.runtime.getConstantInvalidator(name));
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

        getMethodLocation().getCachedMethodsForWrite().put(id, entry);

        return entry;
    }

    protected CacheEntry addToCache(String id, CacheEntry entry) {
        getMethodLocation().getCachedMethodsForWrite().put(id, entry);

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
     * Searches for a method up until the superclass, but include modules.
     */
    @Deprecated
    public DynamicMethod searchMethodLateral(String id) {
        for (RubyModule module = this; (module == this || (module instanceof IncludedModuleWrapper)); module = module.getSuperClass()) {
            DynamicMethod method = module.searchMethodCommon(id);
            if (method != null) return method.isNull() ? null : method;
        }
        return null;
    }

    // MRI: resolve_refined_method
    public CacheEntry resolveRefinedMethod(RubyModule overlay, CacheEntry entry, String id) {
        if (entry != null && entry.method.isRefined()) {
            // Check for refinements in the given scope
            RubyModule refinement = findRefinement(overlay, entry.method.getDefinedClass());

            if (refinement == null) {
                return refinedMethodOriginalMethodEntry(overlay, id, entry);
            } else {
                CacheEntry tmpEntry = refinement.searchWithCache(id);
                if (!tmpEntry.method.isRefined()) {
                    return tmpEntry;
                } else {
                    return refinedMethodOriginalMethodEntry(overlay, id, entry);
                }
            }
        }

        return entry;
    }

    // MRI: find_refinement
    private static RubyModule findRefinement(RubyModule overlay, RubyModule target) {
        RefinementStore refinementStore;
        if (overlay == null || (refinementStore = overlay.refinementStore) == null) return null;

        Map<RubyModule, RubyModule> refinements = refinementStore.refinements;
        return refinements.get(target);
    }

    // The local method resolution logic. Overridden in IncludedModuleWrapper for recursion.
    protected DynamicMethod searchMethodCommon(String id) {
        return getMethods().get(id);
    }

    @Deprecated(since = "10.0")
    public void invalidateCacheDescendants() {
        invalidateCacheDescendants(getCurrentContext());
    }

    public void invalidateCacheDescendants(ThreadContext context) {
        LOG.debug("{} invalidating descendants", baseName);

       context.runtime.getCaches().incrementMethodInvalidations();

        Invalidator methodInvalidator = this.methodInvalidator;
        RubyClass.RubyClassSet includingHierarchies = this.getIncludingHierarchiesForRead();

        if (includingHierarchies.isEmptyOfClasses()) {
            // it's only us; just invalidate directly
            methodInvalidator.invalidate();
            return;
        }

        InvalidatorList invalidators = new InvalidatorList((int) (lastInvalidatorSize * 1.25));
        methodInvalidator.addIfUsed(invalidators);

        synchronized (getRuntime().getHierarchyLock()) {
            includingHierarchies.forEachClass(invalidators);
        }

        lastInvalidatorSize = invalidators.size();

        methodInvalidator.invalidateAll(invalidators);
    }

    public static class InvalidatorList<T> extends ArrayList<T> implements RubyClass.BiConsumerIgnoresSecond<RubyClass> {
        public InvalidatorList(int size) {
            super(size);
        }

        @Override
        public void accept(RubyClass rubyClass) {
            rubyClass.addInvalidatorsAndFlush(this);
        }
    }

    static class SubclassList extends ArrayList<RubyClass> implements RubyClass.BiConsumerIgnoresSecond<RubyClass> {
        boolean includeDescendants;

        public SubclassList(boolean includeDescendants, int i) {
            super(i);

            this.includeDescendants = includeDescendants;
        }

        @Override
        public void accept(RubyClass klass) {
            add(klass);

            if (includeDescendants) klass.addAllSubclasses(this);
        }
    }

    @Deprecated(since = "10.0")
    protected void invalidateCoreClasses() {
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

    @Deprecated(since = "10.0")
    protected void invalidateConstantCache(String constantName) {
        invalidateConstantCache(getCurrentContext(), constantName);
    }

    protected void invalidateConstantCache(ThreadContext context, String constantName) {
        context.runtime.getConstantInvalidator(constantName).invalidate();
    }

    @Deprecated(since = "10.0")
    protected void invalidateConstantCaches(Set<String> constantNames) {
        if (constantNames.size() > 0) {
            Ruby runtime = getCurrentContext().runtime;

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

    @Deprecated(since = "10.0")
    public void addModuleFunction(String name, DynamicMethod method) {
        var context = getCurrentContext();
        addMethod(context, name, method);
        singletonClass(context).addMethod(context, name, method);
    }

    /**
     * @param name
     * @param oldName
     * @deprecated Use {@link RubyModule#aliasMethod(ThreadContext, IRubyObject, IRubyObject)} instead
     */
    @Deprecated(since = "10.0")
    public synchronized void defineAlias(String name, String oldName) {
        defineAlias(getCurrentContext(), name, oldName);
    }

    @Deprecated(since = "10.0")
    public void putAlias(String id, DynamicMethod method, String oldName) {
        putAlias(getCurrentContext(), id, method, oldName);
    }

    /**
     * <p>Note: Internal API - only public as its used by generated code!</p>
     * <p>Note: Used by AnnotationBinder.</p>
     * <p>Note: Not safe for target methods that super, since the frame class will not reflect original source.</p>
     * @param id
     * @param method
     * @param oldName
     */
    public void putAlias(ThreadContext context, String id, DynamicMethod method, String oldName) {
        if (id.equals(oldName)) return;

        putMethod(context.runtime, id, new AliasMethod(this, new CacheEntry(method, method.getImplementationClass(), generation), id, oldName));

        if (isRefinement()) addRefinedMethodEntry(context, id, method);
    }

    @Deprecated(since = "10.0")
    public void putAlias(String id, CacheEntry entry, String oldName) {
        putAlias(getCurrentContext(), id, entry, oldName);
    }

    /**
     * Alias the method contained in the given CacheEntry as a new entry in this module.
     *
     * @param id
     * @param entry
     * @param oldName
     */
    public void putAlias(ThreadContext context, String id, CacheEntry entry, String oldName) {
        if (id.equals(oldName)) return;

        putMethod(context.runtime, id, new AliasMethod(this, entry, id, oldName));

        if (isRefinement()) addRefinedMethodEntry(context, id, entry.method);
    }

    private CacheEntry searchForAliasMethod(ThreadContext context, String id) {
        return deepMethodSearch(context, id);
    }

    private void checkAliasFrameAccesses(ThreadContext context, String id, String newName, DynamicMethod method) {
        if (method instanceof NativeCallMethod) {
            // JRUBY-2435: Aliasing eval and other "special" methods should display a warning
            // We warn because we treat certain method names as "special" for purposes of
            // optimization. Hopefully this will be enough to convince people not to alias
            // them.

            DynamicMethod.NativeCall nativeCall = ((NativeCallMethod) method).getNativeCall();

            // native-backed but not a direct call, ok
            if (nativeCall == null) return;

            Method javaMethod = nativeCall.getMethod();
            JRubyMethod anno = javaMethod.getAnnotation(JRubyMethod.class);

            if (anno == null) return;

            FrameField[] sourceReads = anno.reads();
            FrameField[] sourceWrites = anno.writes();
            Set<FrameField> targetReads = MethodIndex.METHOD_FRAME_READS.get(newName);
            Set<FrameField> targetWrites = MethodIndex.METHOD_FRAME_READS.get(newName);

            boolean readsSuperset = sourceReads.length == 0 ||
                    (targetReads != null && targetReads.containsAll(Arrays.asList(sourceReads)));
            boolean writesSuperset = sourceWrites.length == 0 ||
                    (targetWrites != null && targetWrites.containsAll(Arrays.asList(sourceWrites)));

            // if the target name does expect the same or a superset of frame fields, warn user
            if (!(readsSuperset && writesSuperset)) {

                MethodIndex.addMethodReadFields(newName, sourceReads);
                MethodIndex.addMethodWriteFields(newName, sourceWrites);

                if (context.runtime.isVerbose()) {
                    String baseName = getBaseName();
                    char refChar = '#';
                    String simpleName = getSimpleName(context);

                    if (baseName == null && this instanceof MetaClass) {
                        IRubyObject attached = ((MetaClass) this).getAttached();
                        if (attached instanceof RubyModule) {
                            simpleName = ((RubyModule) attached).getSimpleName(context);
                            refChar = '.';
                        }
                    }

                    warning(context, simpleName + refChar + id + " accesses caller method's state and should not be aliased");
                }
            }
        }
    }

    @Deprecated(since = "10.0")
    public RubyClass defineOrGetClassUnder(String name, RubyClass superClazz) {
        return defineClassUnder(getCurrentContext(), name, superClazz, null, null, -1);
    }

    @Deprecated(since = "10.0")
    public RubyClass defineOrGetClassUnder(String name, RubyClass superClazz, String file, int line) {
        return defineClassUnder(getCurrentContext(), name, superClazz, null, file, line);
    }

    @Deprecated(since = "10.0")
    public RubyClass defineOrGetClassUnder(String name, RubyClass superClazz, ObjectAllocator allocator) {
        return defineClassUnder(getCurrentContext(), name, superClazz, allocator, null, -1);
    }

    @Deprecated(since = "10.0")
    public RubyClass defineOrGetClassUnder(String name, RubyClass superClazz, ObjectAllocator allocator,
                                           String file, int line) {
        return defineClassUnder(getCurrentContext(), name, superClazz, allocator, file, line);
    }

    /**
     * Internal API only used by our IR runtime helpers in setting up Ruby-defined classes or re-accessing them
     * if they already exist.  Look at
     * {@link RubyModule#defineClassUnder(ThreadContext, String, RubyClass, ObjectAllocator)} for native
     * extensions.
     *
     * @param name to be defined
     * @param superClazz the super class of this new class
     * @param allocator how to allocate it
     * @param file location where it was defined from
     * @param line location where it was defined from
     * @return the new class.
     */
    public RubyClass defineClassUnder(ThreadContext context, String name, RubyClass superClazz,
                                      ObjectAllocator allocator, String file, int line) {
        // This method is intended only for defining new classes in Ruby code,
        // so it uses the allocator of the specified superclass or default to
        // the Object allocator. It should NOT be used to define classes that require a native allocator.

        IRubyObject classObj = getConstantAtSpecial(context, name);
        RubyClass clazz;

        if (classObj != null) {
            if (!(classObj instanceof RubyClass clazzy)) throw typeError(context, name + " is not a class");
            clazz = clazzy;

            if (superClazz != null) {
                RubyClass tmp = clazz.getSuperClass();
                while (tmp != null && tmp.isIncluded()) tmp = tmp.getSuperClass(); // need to skip IncludedModuleWrappers
                if (tmp != null) tmp = tmp.getRealClass();
                if (tmp != superClazz) throw typeError(context, "superclass mismatch for class " + ids(context.runtime, name));
            }
        } else if ((clazz = searchProvidersForClass(context, name, superClazz)) != null) {
            // reopen a java class
        } else {
            if (superClazz == null) superClazz = objectClass(context);

            if (allocator == null) {
                if (isReifiable(objectClass(context), superClazz)) {
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

            clazz = RubyClass.newClass(context, superClazz, name, allocator, this, true, file, line);
        }

        return clazz;
    }

    /**
     * Determine if a new child of the given class can have its variables reified.
     */
    private boolean isReifiable(RubyClass Object, RubyClass superClass) {
        return superClass == Object || superClass.getAllocator() == IVAR_INSPECTING_OBJECT_ALLOCATOR;
    }

    /** this method should be used only by interpreter or compiler
     *
     */
    @Deprecated(since = "10.0")
    public RubyModule defineOrGetModuleUnder(String name) {
        return defineOrGetModuleUnder(getCurrentContext(), name, null, -1);
    }

    @Deprecated(since = "10.0")
    public RubyModule defineOrGetModuleUnder(String name, String file, int line) {
        return defineOrGetModuleUnder(getCurrentContext(), name, file, line);
    }

    @Interp
    @JIT
    public RubyModule defineOrGetModuleUnder(ThreadContext context, String name, String file, int line) {
        // This method is intended only for defining new modules in Ruby code
        IRubyObject moduleObj = getConstantAtSpecial(context, name);
        RubyModule module;
        if (moduleObj != null) {
            if (!moduleObj.isModule()) throw typeError(context, "", moduleObj, " is not a module");
            module = (RubyModule)moduleObj;
        } else if ((module = searchProvidersForModule(context, name)) != null) {
            // reopen a java module
        } else {
            module = RubyModule.newModule(context, name, this, true, file, line);
        }
        return module;
    }

    /**
     * @param name
     * @param superClass
     * @param allocator
     * @return ""
     * @deprecated Use {@link RubyModule#defineClassUnder(ThreadContext, String, RubyClass, ObjectAllocator)} instead.
     */
    @Deprecated(since = "10.0")
    public RubyClass defineClassUnder(String name, RubyClass superClass, ObjectAllocator allocator) {
        return defineClassUnder(getCurrentContext(), name, superClass, allocator);
    }

    @Deprecated
    public RubyModule defineModuleUnder(String name) {
        return defineModuleUnder(getCurrentContext(), name);
    }

    private void addAccessor(ThreadContext context, RubySymbol identifier, Visibility visibility, boolean readable, boolean writeable) {
        String internedIdentifier = identifier.idString();

        if (visibility == MODULE_FUNCTION) {
            warning(context, "attribute accessor as module_function");
            visibility = PRIVATE;
        }

        if (!identifier.validLocalVariableName() && !identifier.validConstantName()) {
            throw nameError(context, "invalid attribute name", identifier);
        }

        final String variableName = identifier.asInstanceVariable().idString();
        if (readable) {
            addMethod(context, internedIdentifier, new AttrReaderMethod(getMethodLocation(), visibility, variableName));
            methodAdded(context, identifier);
        }
        if (writeable) {
            identifier = identifier.asWriter();
            addMethod(context, identifier.idString(), new AttrWriterMethod(getMethodLocation(), visibility, variableName));
            methodAdded(context, identifier);
        }
    }

    protected void methodAdded(ThreadContext context, RubySymbol identifier) {
        if (isSingleton()) {
            ((MetaClass) this).getAttached().callMethod(context, "singleton_method_added", identifier);
        } else {
            callMethod(context, "method_added", identifier);
        }
    }

    private void methodUndefined(ThreadContext context, RubySymbol nameSymbol) {
        if (isSingleton()) {
            ((MetaClass) this).getAttached().callMethod(context, "singleton_method_undefined", nameSymbol);
        } else {
            callMethod(context, "method_undefined", nameSymbol);
        }
    }

    private void methodRemoved(ThreadContext context, RubySymbol name) {
        if (isSingleton()) {
            ((MetaClass) this).getAttached().callMethod(context, "singleton_method_removed", name);
        } else {
            callMethod(context, "method_removed", name);
        }
    }

    @Deprecated(since = "10.0")
    public void setMethodVisibility(IRubyObject[] methods, Visibility visibility) {
        setMethodVisibility(getCurrentContext(), methods, visibility);
    }

    /** set_method_visibility
     *
     */
    public void setMethodVisibility(ThreadContext context, IRubyObject[] methods, Visibility visibility) {
        if (methods.length == 1 && methods[0] instanceof RubyArray) {
            setMethodVisibility(context, ((RubyArray<?>) methods[0]).toJavaArray(context), visibility);
            return;
        }

        for (IRubyObject method : methods) {
            exportMethod(context, checkID(context, method).idString(), visibility);
        }
    }

    @Deprecated(since = "10.0")
    public void exportMethod(String name, Visibility visibility) {
        exportMethod(getCurrentContext(), name, visibility);
    }

    /** rb_export_method
     *
     */
    private void exportMethod(ThreadContext context, String name, Visibility visibility) {
        CacheEntry entry = getMethodLocation().deepMethodSearch(context, name);
        DynamicMethod method = entry.method;

        if (method.getVisibility() != visibility) {
            if (this == method.getImplementationClass()) {
                method.setVisibility(visibility);
            } else {
                DynamicMethod newMethod = new PartialDelegatingMethod(this, entry, visibility);
                getMethodLocation().addMethod(context, name, newMethod);
            }

            invalidateCacheDescendants(context);
        }
    }

    private CacheEntry deepMethodSearch(ThreadContext context, String id) {
        CacheEntry orig = searchWithCache(id);
        if (orig.method.isRefined()) {
            orig = resolveRefinedMethod(null, orig, id);
        }

        if (orig.method.isUndefined() || orig.method.isRefined()) {
            if (!isModule() || (orig = objectClass(context).searchWithCache(id)).method.isUndefined()) {
                // FIXME: Do we potentially leak symbols here if they do not exist?
                RubySymbol name = asSymbol(context, id);
                throw nameError(context, undefinedMethodMessage(context.runtime, name, rubyName(context), isModule()), name);
            }
        }

        return orig;
    }

    public static String undefinedMethodMessage(Ruby runtime, IRubyObject name, IRubyObject modName, boolean isModule) {
        return str(runtime, "undefined method '", name, "' for " + (isModule ? "module" : "class") + " '", modName, "'");
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

    @Deprecated(since = "10.0")
    public final IRubyObject newMethod(IRubyObject receiver, String methodName, boolean bound, Visibility visibility) {
        return newMethod(getCurrentContext(), receiver, methodName, null, bound, visibility, false, true);
    }

    @Deprecated(since = "10.0")
    public final IRubyObject newMethod(IRubyObject receiver, String methodName, StaticScope refinedScope, boolean bound, Visibility visibility) {
        return newMethod(getCurrentContext(), receiver, methodName, refinedScope, bound, visibility, false, true);
    }

    @Deprecated(since = "10.0")
    public final IRubyObject newMethod(IRubyObject receiver, final String methodName, boolean bound, Visibility visibility, boolean respondToMissing) {
        return newMethod(getCurrentContext(), receiver, methodName, null, bound, visibility, respondToMissing, true);
    }

    @Deprecated(since = "10.0")
    public final IRubyObject newMethod(IRubyObject receiver, final String methodName, StaticScope scope, boolean bound, Visibility visibility, boolean respondToMissing) {
        return newMethod(getCurrentContext(), receiver, methodName, scope, bound, visibility, respondToMissing, true);
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

    @Deprecated(since = "10.0")
    public IRubyObject newMethod(IRubyObject receiver, final String methodName, boolean bound, Visibility visibility,
                                 boolean respondToMissing, boolean priv) {
        return newMethod(getCurrentContext(), receiver, methodName, null, bound, visibility, respondToMissing, priv);
    }

    @Deprecated(since = "10.0")
    public IRubyObject newMethod(IRubyObject receiver, final String methodName, StaticScope scope, boolean bound,
                                 Visibility visibility, boolean respondToMissing, boolean priv) {
        return newMethod(getCurrentContext(), receiver, methodName, scope, bound, visibility, respondToMissing, priv);
    }

    protected IRubyObject newMethod(ThreadContext context, IRubyObject receiver, final String methodName, StaticScope scope,
                                 boolean bound, Visibility visibility, boolean respondToMissing, boolean priv) {
        CacheEntry entry = scope == null ? searchWithCache(methodName) : searchWithRefinements(methodName, scope);

        if (entry.method.isUndefined() || visibility != null && entry.method.getVisibility() != visibility) {
            if (!respondToMissing || !receiver.respondsToMissing(methodName, priv)) {
                throw nameError(context, "undefined method '" + methodName + "' for class '" + getName(context) + '\'', methodName);
            }

            entry = new CacheEntry(new RespondToMissingMethod(this, PUBLIC, methodName), entry.sourceModule, entry.token);
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
        RubySymbol name = checkID(context, arg0);
        if (!block.isGiven()) throw argumentError(context, "tried to create Proc object without a block");

        if ("initialize".equals(name.idString())) visibility = PRIVATE;

        // If we know it comes from IR we can convert this directly to a method and
        // avoid overhead of invoking it as a block
        if (block.getBody() instanceof IRBlockBody body &&
                instanceConfig(context).getCompileMode().shouldJIT()) { // FIXME: Once Interp and Mixed Methods are one class we can fix this to work in interp mode too.
            IRClosure closure = body.getScope();

            // closure may be null from AOT scripts
            if (closure != null) {
                // Ask closure to give us a method equivalent.
                IRMethod method = closure.convertToMethod(name.getBytes());
                if (method != null) {
                    var newMethod = new DefineMethodMethod(method, visibility, this, context.getFrameBlock());
                    Helpers.addInstanceMethod(this, name, newMethod, visibility, context);
                    return name;
                }
            }
        }

        var newMethod = createProcMethod(context.runtime, name.idString(), visibility, block);
        Helpers.addInstanceMethod(this, name, newMethod, visibility, context);

        return name;
    }

    @JRubyMethod(name = "define_method", reads = VISIBILITY)
    public IRubyObject define_method(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        Visibility visibility = getCurrentVisibilityForDefineMethod(context);

        return defineMethodFromCallable(context, arg0, arg1, visibility);
    }

    public IRubyObject defineMethodFromCallable(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Visibility visibility) {
        RubySymbol name = checkID(context, arg0);
        DynamicMethod newMethod;

        if ("initialize".equals(name.idString())) visibility = PRIVATE;

        if (context.runtime.getProc().isInstance(arg1)) {
            // double-testing args.length here, but it avoids duplicating the proc-setup code in two places
            RubyProc proc = (RubyProc)arg1;

            newMethod = createProcMethod(context.runtime, name.idString(), visibility, proc.getBlock());
        } else if (arg1 instanceof AbstractRubyMethod) {
            AbstractRubyMethod method = (AbstractRubyMethod)arg1;

            checkValidBindTargetFrom(context, (RubyModule) method.owner(context), false);

            newMethod = method.getMethod().dup();
            newMethod.setImplementationClass(this);
            newMethod.setVisibility(visibility);
        } else {
            throw typeError(context, arg1, "Proc/Method/UnboundMethod");
        }

        Helpers.addInstanceMethod(this, name, newMethod, visibility, context);

        return name;
    }

    @Deprecated
    public IRubyObject define_method(ThreadContext context, IRubyObject[] args, Block block) {
        return switch (args.length) {
            case 1 -> define_method(context, args[0], block);
            case 2 -> define_method(context, args[0], args[1], block);
            default -> throw argumentError(context, "wrong number of arguments (" + args.length + " for 2)");
        };
    }

    private DynamicMethod createProcMethod(Ruby runtime, String name, Visibility visibility, Block block) {
        block = block.cloneBlockAndFrame();

        block.getBinding().getFrame().setKlazz(this);
        block.getBinding().getFrame().setName(name);

        // a normal block passed to define_method changes to do arity checking; make it a lambda
        RubyProc proc = runtime.newProc(Block.Type.LAMBDA, block);

        proc.setFromMethod();

        // various instructions can tell this scope is not an ordinary block but a block representing
        // a method definition.
        block.getBody().getStaticScope().makeArgumentScope();

        return new ProcMethod(this, proc, visibility, name);
    }

    @Deprecated(since = "10.0")
    public IRubyObject name() {
        return name(getCurrentContext());
    }

    @JRubyMethod(name = "name")
    public IRubyObject name(ThreadContext context) {
        return getBaseName() == null ? context.nil : rubyName(context);
    }

    @Deprecated(since = "10.0")
    protected final IRubyObject cloneMethods(RubyModule clone) {
        return cloneMethods(getCurrentContext(), clone);
    }

    protected final IRubyObject cloneMethods(ThreadContext context, RubyModule clone) {
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
                clone.putMethod(context.runtime, entry.getKey(), clonedMethod);
            }
        }

        return clone;
    }

    /** mri: rb_mod_init_copy
     *
     */
    @JRubyMethod(name = "initialize_copy", visibility = Visibility.PRIVATE)
    public IRubyObject initialize_copy(ThreadContext context, IRubyObject original) {
        if (this instanceof RubyClass klazz) checkSafeTypeToCopy(context, klazz);

        super.initialize_copy(context, original);

        RubyModule originalModule = (RubyModule)original;

        if (!getMetaClass().isSingleton()) {
            setMetaClass(originalModule.getSingletonClassCloneAndAttach(context, this));
        }
        superClass(originalModule.superClass());
        if (originalModule.hasVariables()) syncVariables(originalModule);
        syncConstants(originalModule);

        originalModule.cloneMethods(context, this);
        
        this.javaProxy = originalModule.javaProxy;

        return this;
    }

    // mri: class_init_copy_check
    private void checkSafeTypeToCopy(ThreadContext context, RubyClass original) {
        var BasicObject = basicObjectClass(context);
        if (original == BasicObject) throw typeError(context, "can't copy the root class");
        if (getSuperClass() == BasicObject) throw typeError(context, "already initialized class");
        if (original.isSingleton()) throw typeError(context, "can't copy singleton class");
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
        var ary = newArray(context);

        for (RubyModule p = getSuperClass(); p != null; p = p.getSuperClass()) {
            if (p.isIncluded()) ary.append(context, p.getDelegate().getOrigin());
        }

        return ary;
    }

    public boolean hasPrepends() {
        return methodLocation != null;
    }

    /** rb_mod_ancestors
     *
     */
    @JRubyMethod(name = "ancestors")
    public RubyArray ancestors(ThreadContext context) {
        return newArray(context, getAncestorList());
    }

    @Deprecated(since = "10.0")
    public RubyArray ancestors() {
        return ancestors(getCurrentContext());
    }

    public List<IRubyObject> getAncestorList() {
        ArrayList<IRubyObject> list = new ArrayList<>();

        for (RubyModule module = this; module != null; module = module.getSuperClass()) {
            // FIXME this is silly. figure out how to delegate the getNonIncludedClass()
            // call to drop the getDelegate().
            if (module.getMethodLocation() == module) list.add(module.getDelegate().getOrigin());
        }

        return list;
    }

    public boolean hasModuleInPrepends(RubyModule type) {
        RubyModule methodLocation = this.methodLocation;

        // only check if we have prepends, to allow include and prepend of same module
        if (methodLocation == null) {
            return false;
        }

        for (RubyModule module = this.getSuperClass(); module != methodLocation; module = module.getSuperClass()) {
            if (type == module.getOrigin()) return true;
        }

        return false;
    }

    private RubyModule getPrependCeiling() {
        RubyModule methodLocation = getMethodLocation();
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
        RubyClass metaClass = this.metaClass;

        // may be null during boot
        if (metaClass == null) return id;

        Ruby runtime = metaClass.getClassRuntime();

        if (runtime.isBooting()) return id;

        ThreadContext context = runtime.getCurrentContext();
        CachingCallSite hash = sites(context).hash;

        if (hash.isBuiltin(this)) return id;

        // we truncate for Java hashcode
        return (int) hash.call(context, this, this).convertToInteger().getLongValue();
    }

    @JRubyMethod(name = "hash")
    public RubyFixnum hash(ThreadContext context) {
        return asFixnum(context, id);
    }

    @Override
    public RubyString to_s() {
        return (RubyString) to_s(getCurrentContext());
    }

    /** rb_mod_to_s
     *
     */
    @JRubyMethod(name = "to_s", alias = "inspect")
    public IRubyObject to_s(ThreadContext context) {
        if (isSingleton()) {
            IRubyObject attached = ((MetaClass) this).getAttached();
            RubyString buffer = newString(context, "#<Class:");

            if (attached instanceof RubyModule) {
                buffer.catWithCodeRange(attached.inspect(context).convertToString());
            } else if (attached != null) {
                buffer.catWithCodeRange((RubyString) attached.anyToString());
            }
            buffer.cat('>', buffer.getEncoding());

            return buffer;
        }

        RefinementStore refinementStore = this.refinementStore;
        RubyModule refinedClass;
        if (refinementStore != null && (refinedClass = refinementStore.refinedClass) != null) {
            RubyString buffer = newString(context, "#<refinement:");

            buffer.catWithCodeRange(refinedClass.inspect(context).convertToString());
            buffer.cat('@', buffer.getEncoding());
            buffer.catWithCodeRange((refinementStore.definedAt.inspect(context).convertToString()));
            buffer.cat('>', buffer.getEncoding());

            return buffer;
        }

        return dupString(context, rubyName(context));
    }

    /** rb_mod_eqq
     *
     */
    @JRubyMethod(name = "===")
    @Override
    public RubyBoolean op_eqq(ThreadContext context, IRubyObject obj) {
        return asBoolean(context, isInstance(obj));
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

    @JRubyMethod(name = "==")
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
        if(!(other instanceof RubyModule)) return context.fals;

        RubyModule otherModule = (RubyModule) other;
        if(otherModule.isIncluded()) {
            return asBoolean(context, otherModule.isSame(this));
        } else {
            return asBoolean(context, isSame(otherModule));
        }
    }

    /** rb_mod_freeze
     *
     */
    @JRubyMethod(name = "freeze")
    @Override
    public final IRubyObject freeze(ThreadContext context) {
        to_s(context);
        return super.freeze(context);
    }

    @Deprecated(since = "10.0")
    public IRubyObject op_le(IRubyObject arg) {
        return op_le(getCurrentContext(), arg);
    }

    /**
    * MRI: rb_class_inherited_p
    */
    @JRubyMethod(name = "<=")
    public IRubyObject op_le(ThreadContext context, IRubyObject arg) {
        RubyModule argMod = castAsModule(context, arg, "compared with non class/module");

        if (searchAncestor(argMod.getMethodLocation()) != null) return context.tru;
        /* not mod < arg; check if mod > arg */
        if (argMod.searchAncestor(this) != null) return context.fals;

        return context.nil;
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

    @Deprecated(since = "10.0")
    public IRubyObject op_lt(IRubyObject obj) {
        return op_lt(getCurrentContext(), obj);
    }

    /** rb_mod_lt
    *
    */
    @JRubyMethod(name = "<")
    public IRubyObject op_lt(ThreadContext context, IRubyObject obj) {
        return obj == this ? context.fals : op_le(context, obj);
    }

    @Deprecated(since = "10.0")
    public IRubyObject op_ge(IRubyObject obj) {
        return op_ge(getCurrentContext(), obj);
    }

    /** rb_mod_ge
    *
    */
    @JRubyMethod(name = ">=")
    public IRubyObject op_ge(ThreadContext context, IRubyObject obj) {
        return castAsModule(context, obj, "compared with non class/module").op_le(context, this);
    }

    @Deprecated(since = "10.0")
    public IRubyObject op_gt(IRubyObject obj) {
        return op_gt(getCurrentContext(), obj);
    }

    /** rb_mod_gt
    *
    */
    @JRubyMethod(name = ">")
    public IRubyObject op_gt(ThreadContext context, IRubyObject obj) {
        return this == obj ? context.fals : op_ge(context, obj);
    }

    /** rb_mod_cmp
    *
    */
    @JRubyMethod(name = "<=>")
    public IRubyObject op_cmp(ThreadContext context, IRubyObject obj) {
        if (this == obj) return asFixnum(context, 0);
        if (!(obj instanceof RubyModule)) return context.nil;

        RubyModule module = (RubyModule) obj;

        if (module.isKindOfModule(this)) return asFixnum(context, 1);
        if (this.isKindOfModule(module)) return asFixnum(context, -1);

        return context.nil;
    }

    @Deprecated
    public IRubyObject op_cmp(IRubyObject obj) {
        return op_cmp(getCurrentContext(), obj);
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
        addAccessor(context, newHardSymbol(context.runtime, name.intern()), PUBLIC, true, true);
    }

    public void addReadAttribute(ThreadContext context, String name) {
        addAccessor(context, newHardSymbol(context.runtime, name.intern()), PUBLIC, true, false);
    }

    public void addWriteAttribute(ThreadContext context, String name) {
        addAccessor(context, newHardSymbol(context.runtime, name.intern()), PUBLIC, false, true);
    }

    @JRubyMethod(required = 1, rest = true, checkArity = false, visibility = PRIVATE)
    public IRubyObject ruby2_keywords(ThreadContext context, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 1, -1);
        checkFrozen();

        for (IRubyObject name: args) {
            String id = RubySymbol.idStringFromObject(context, name);

            // FIXME: id == null or bad symbol error missing

            DynamicMethod method = searchMethod(id);
            if (method.isUndefined() && isModule()) {
                method = objectClass(context).searchMethod(id);
            }

            if (method.isUndefined()) {
                throw nameError(context, undefinedMethodMessage(context.runtime, name, rubyName(context), isModule()), name);
            }

            // FIXME: missing origin_class module
            if (method.getDefinedClass() == this) {
                if (!method.isNative()) {
                    Signature signature = method.getSignature();
                    if (!signature.hasRest()) {
                        warn(context, str(context.runtime, "Skipping set of ruby2_keywords flag for ", ids(context.runtime, name), " (method accepts keywords or method does not accept argument splat)"));
                    } else if (!signature.hasKwargs()) {
                        method.setRuby2Keywords();
                    } else if (method instanceof AbstractIRMethod && ((AbstractIRMethod) method).getStaticScope().exists("...") == -1) {
                        warn(context, str(context.runtime, "Skipping set of ruby2_keywords flag for ", ids(context.runtime, name), " (method accepts keywords or method does not accept argument splat)"));
                    }
                } else {
                    warn(context, str(context.runtime, "Skipping set of ruby2_keywords flag for ", ids(context.runtime, name), " (method not defined in Ruby)"));
                }
            } else {
                warn(context, str(context.runtime, "Skipping set of ruby2_keywords flag for ", ids(context.runtime, name), " (can only set in method defining module)"));
            }
        }
        return context.nil;
    }

    /** rb_mod_attr
     *
     */
    @JRubyMethod(name = "attr", rest = true, reads = VISIBILITY)
    public IRubyObject attr(ThreadContext context, IRubyObject[] args) {
        if (args.length == 2 && (args[1] == context.tru || args[1] == context.fals)) {
            warningDeprecated(context, "optional boolean argument is obsoleted");
            boolean writeable = args[1].isTrue();
            RubySymbol sym = checkID(context, args[0]);
            addAccessor(context, sym, getCurrentVisibilityForDefineMethod(context), args[0].isTrue(), writeable);

            return writeable ?
                    newArray(context, sym, asSymbol(context, sym.getBytes().dup().append('='))) :
                    newArray(context, sym);
        }

        return attr_reader(context, args);
    }

    @Deprecated
    public IRubyObject attr_reader(IRubyObject[] args) {
        return attr_reader(getCurrentContext(), args);
    }

    /** rb_mod_attr_reader
     *
     */
    @JRubyMethod(name = "attr_reader", rest = true, reads = VISIBILITY)
    public IRubyObject attr_reader(ThreadContext context, IRubyObject[] args) {
        // Check the visibility of the previous frame, which will be the frame in which the class is being eval'ed
        Visibility visibility = getCurrentVisibilityForDefineMethod(context);
        IRubyObject[] result = new IRubyObject[args.length];

        for (int i = 0; i < args.length; i++) {
            RubySymbol sym = checkID(context, args[i]);
            result[i] = sym;

            addAccessor(context, sym, visibility, true, false);
        }

        return newArray(context, result);
    }

    /** rb_mod_attr_writer
     *
     */
    @JRubyMethod(name = "attr_writer", rest = true, reads = VISIBILITY)
    public IRubyObject attr_writer(ThreadContext context, IRubyObject[] args) {
        // Check the visibility of the previous frame, which will be the frame in which the class is being eval'ed
        Visibility visibility = getCurrentVisibilityForDefineMethod(context);
        IRubyObject[] result = new IRubyObject[args.length];

        for (int i = 0; i < args.length; i++) {
            RubySymbol sym = checkID(context, args[i]);
            ByteList writer = sym.getBytes().dup().append('=');
            result[i] = asSymbol(context, writer);

            addAccessor(context, sym, visibility, false, true);
        }

        return newArray(context, result);
    }


    @Deprecated
    public IRubyObject attr_accessor(IRubyObject[] args) {
        return attr_accessor(getCurrentContext(), args);
    }

    /** rb_mod_attr_accessor
     *  Note: this method should not be called from Java in most cases, since
     *  it depends on Ruby frame state for visibility. Use add[Read/Write]Attribute instead.
     */
    @JRubyMethod(name = "attr_accessor", rest = true, reads = VISIBILITY)
    public IRubyObject attr_accessor(ThreadContext context, IRubyObject[] args) {
        // Check the visibility of the previous frame, which will be the frame in which the class is being eval'ed
        Visibility visibility = getCurrentVisibilityForDefineMethod(context);
        IRubyObject[] result = new IRubyObject[2 * args.length];

        for (int i = 0; i < args.length; i++) {
            RubySymbol sym = checkID(context, args[i]);

            ByteList reader = sym.getBytes().shallowDup();
            result[i*2] = asSymbol(context, reader);

            ByteList writer = sym.getBytes().dup().append('=');
            result[i*2+1] = asSymbol(context, writer);

            // This is almost always already interned, since it will be called with a symbol in most cases
            // but when created from Java code, we might getService an argument that needs to be interned.
            // addAccessor has as a precondition that the string MUST be interned
            addAccessor(context, sym, visibility, true, true);
        }

        return newArray(context, result);
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
    @Deprecated(since = "10.0")
    private RubyArray instance_methods(IRubyObject[] args, Visibility visibility, boolean not) {
        boolean includeSuper = args.length > 0 ? args[0].isTrue() : true;
        return instanceMethods(getCurrentContext(), visibility, includeSuper, true, not);
    }

    @Deprecated(since = "10.0")
    public RubyArray instanceMethods(IRubyObject[] args, Visibility visibility, boolean obj, boolean not) {
        return instanceMethods(getCurrentContext(), args, visibility, obj, not);
    }

    public RubyArray instanceMethods(ThreadContext context, IRubyObject[] args, Visibility visibility, boolean obj, boolean not) {
        boolean includeSuper = args.length > 0 ? args[0].isTrue() : true;
        return instanceMethods(context, visibility, includeSuper, obj, not);
    }

    @Deprecated(since = "10.0")
    public RubyArray instanceMethods(Visibility visibility, boolean includeSuper, boolean obj, boolean not) {
        return instanceMethods(getCurrentContext(), visibility, includeSuper, obj, not);
    }

    private RubyArray instanceMethods(ThreadContext context, Visibility visibility, boolean includeSuper, boolean obj, boolean not) {
        var ary = newArray(context);

        populateInstanceMethodNames(context, new HashSet<>(), ary, visibility, obj, not, includeSuper);

        return ary;
    }

    final void populateInstanceMethodNames(ThreadContext context, final Set<String> seen, final RubyArray ary,
                                           Visibility visibility, boolean obj, boolean not, boolean recur) {
        RubyModule mod = this;
        boolean prepended = false;

        RubyModule methodLocation = this.methodLocation;
        if (!recur && methodLocation != null) {
            mod = methodLocation;
            prepended = true;
        }

        for (; mod != null; mod = mod.getSuperClass()) {
            mod.addMethodSymbols(context.runtime, seen, ary, not, visibility);

            if (!prepended && mod.isIncluded()) continue;
            if (obj && mod.isSingleton()) continue;
            if (!recur) break;
        }
    }

    @Deprecated(since = "10.0")
    protected void addMethodSymbols(Ruby runtime, Set<String> seen, RubyArray ary, boolean not, Visibility visibility) {
        addMethodSymbols(getCurrentContext(), seen, ary, not, visibility);
    }

    protected void addMethodSymbols(ThreadContext context, Set<String> seen, RubyArray ary, boolean not, Visibility visibility) {
        getMethods().forEach((id, method) -> {
            if (method instanceof RefinedMarker) return;

            if (seen.add(id)) { // false - not added (already seen)
                if ((!not && method.getVisibility() == visibility || (not && method.getVisibility() != visibility))
                        && !method.isUndefined()) {
                    ary.append(context, asSymbol(context, id));
                }
            }
        });
    }

    @Deprecated
    public RubyArray<?> instance_methods19(IRubyObject[] args) {
        return instance_methods(args);
    }

    @Deprecated
    public RubyArray<?> instance_methods(IRubyObject[] args) {
        return instance_methods(getCurrentContext(), args);
    }

    @JRubyMethod(name = "instance_methods", optional = 1, checkArity = false)
    public RubyArray instance_methods(ThreadContext context, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 0, 1);

        return instanceMethods(context, args, PRIVATE, false, true);
    }

    @Deprecated
    public RubyArray<?> public_instance_methods19(IRubyObject[] args) {
        return public_instance_methods(args);
    }

    @Deprecated
    public RubyArray<?> public_instance_methods(IRubyObject[] args) {
        return public_instance_methods(getCurrentContext(), args);
    }

    @JRubyMethod(name = "public_instance_methods", optional = 1, checkArity = false)
    public RubyArray public_instance_methods(ThreadContext context, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 0, 1);

        return instanceMethods(context, args, PUBLIC, false, false);
    }

    @JRubyMethod(name = "instance_method", reads = SCOPE)
    public IRubyObject instance_method(ThreadContext context, IRubyObject symbol) {
        return newMethod(context, null, checkID(context, symbol).idString(), context.getCurrentStaticScope(),
                false, null, false, true);
    }

    @Deprecated(since = "10.0")
    public IRubyObject instance_method(IRubyObject symbol) {
        return instance_method(getCurrentContext(), symbol);
    }

    @Deprecated(since = "10.0")
    public IRubyObject public_instance_method(IRubyObject symbol) {
        return public_instance_method(getCurrentContext(), symbol);
    }

    @JRubyMethod(name = "public_instance_method")
    public IRubyObject public_instance_method(ThreadContext context, IRubyObject symbol) {
        return newMethod(context, null, checkID(context, symbol).idString(), null, false, PUBLIC, false, true);
    }

    @Deprecated
    public RubyArray protected_instance_methods(IRubyObject[] args) {
        return protected_instance_methods(getCurrentContext(), args);
    }

    /** rb_class_protected_instance_methods
     *
     */
    @JRubyMethod(name = "protected_instance_methods", optional = 1, checkArity = false)
    public RubyArray protected_instance_methods(ThreadContext context, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 0, 1);

        return instanceMethods(context, args, PROTECTED, false, false);
    }

    @Deprecated
    public RubyArray private_instance_methods(IRubyObject[] args) {
        return private_instance_methods(getCurrentContext(), args);
    }

    /** rb_class_private_instance_methods
     *
     */
    @JRubyMethod(name = "private_instance_methods", optional = 1, checkArity = false)
    public RubyArray private_instance_methods(ThreadContext context, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 0, 1);

        return instanceMethods(context, args, PRIVATE, false, false);
    }

    @JRubyMethod(name = "undefined_instance_methods")
    public IRubyObject undefined_instance_method(ThreadContext context) {
        var list = newArray(context);

        getMethods().forEach((id, method) -> {
            if (method instanceof RefinedMarker) return;
            if (method.isUndefined()) list.append(context, asSymbol(context, id));
        });

        return list;
    }

    @Deprecated(since = "10.0")
    public RubyModule prepend_features(IRubyObject include) {
        return prepend_features(getCurrentContext(), include);
    }

    /** rb_mod_prepend_features
     *
     */
    @JRubyMethod(name = "prepend_features", visibility = PRIVATE)
    public RubyModule prepend_features(ThreadContext context, IRubyObject include) {
        verifyNormalModule(context, include).prependModule(context, this);

        return this;
    }

    @Deprecated(since = "10.0")
    public RubyModule append_features(IRubyObject include) {
        return append_features(getCurrentContext(), include);
    }

    /** rb_mod_append_features
     *
     */
    @JRubyMethod(name = "append_features", visibility = PRIVATE)
    public RubyModule append_features(ThreadContext context, IRubyObject include) {
        verifyNormalModule(context, include).include(context, this);

        return this;
    }

    private RubyModule verifyNormalModule(ThreadContext context, IRubyObject include) {
        if (!isModule()) throw typeError(context, this, "Module");

        var mod = castAsModule(context, include);

        // included and prepended modules happen to be RubyModule but are not "modules".
        if (!(mod.isModule() || mod.isClass())) throw typeError(context, include, "Module");

        return mod;
    }


    @Deprecated(since = "10.0")
    public IRubyObject extend_object(IRubyObject obj) {
        return extend_object(getCurrentContext(), obj);
    }

    /** rb_mod_extend_object
     *
     */
    @JRubyMethod(name = "extend_object", visibility = PRIVATE)
    public IRubyObject extend_object(ThreadContext context, IRubyObject obj) {
        if (!isModule()) throw typeError(context, this, "Module");

        obj.singletonClass(context).include(context, this);
        return obj;
    }

    @Deprecated
    public RubyModule include(IRubyObject[] modules) {
        return include(getCurrentContext(), modules);
    }

    /** rb_mod_include
     *
     */
    @JRubyMethod(name = "include", required = 1, rest = true, checkArity = false)
    public RubyModule include(ThreadContext context, IRubyObject[] modules) {
        int argc = Arity.checkArgumentCount(context, modules, 1, -1);

        if (isRefinement()) throw typeError(context, "Refinement#include has been removed");

        for (IRubyObject module: modules) {
            if (!module.isModule()) throw typeError(context, module, "Module");
            if (((RubyModule) module).isRefinement()) throw typeError(context, "Cannot include refinement");
        }

        for (int i = argc - 1; i >= 0; i--) {
            IRubyObject module = modules[i];
            module.callMethod(context, "append_features", this);
            module.callMethod(context, "included", this);
        }

        return this;
    }

    @JRubyMethod(name = "include") // most common path: include Enumerable
    public RubyModule include(ThreadContext context, IRubyObject module) {
        if (isRefinement()) throw typeError(context, "Refinement#include has been removed");
        if (!module.isModule()) throw typeError(context, module, "Module");
        if (((RubyModule) module).isRefinement()) throw typeError(context, "Cannot include refinement");

        module.callMethod(context, "append_features", this);
        module.callMethod(context, "included", this);
        return this;
    }

    @JRubyMethod(name = "included", visibility = PRIVATE)
    public IRubyObject included(ThreadContext context, IRubyObject other) {
        return context.nil;
    }

    @JRubyMethod(name = "extended", visibility = PRIVATE)
    public IRubyObject extended(ThreadContext context, IRubyObject other, Block block) {
        return context.nil;
    }

    @JRubyMethod(name = "mix", visibility = PRIVATE)
    public IRubyObject mix(ThreadContext context, IRubyObject modArg) {
        var mod = castAsModule(context, modArg);

        Map<String, DynamicMethod> methods = mod.getMethods();
        for (Map.Entry<String, DynamicMethod> entry : methods.entrySet()) {
            if (getMethodLocation().getMethods().containsKey(entry.getKey())) {
                throw argumentError(context, "method would conflict - " + entry.getKey());
            }
        }

        for (Map.Entry<String, DynamicMethod> entry : methods.entrySet()) {
            getMethodLocation().getMethodsForWrite().put(entry.getKey(), entry.getValue().dup());
        }

        return mod;
    }

    @JRubyMethod(name = "mix", visibility = PRIVATE)
    public IRubyObject mix(ThreadContext context, IRubyObject modArg, IRubyObject hash0) {
        var mod = castAsModule(context, modArg);
        if (!mod.isModule()) throw typeError(context, mod, "Module");
        var methodNames = castAsHash(context, hash0);

        for (Map.Entry<IRubyObject, IRubyObject> entry : (Set<Map.Entry<IRubyObject, IRubyObject>>)methodNames.directEntrySet()) {
            String name = entry.getValue().toString();
            if (getMethods().containsKey(name)) throw argumentError(context, "constant would conflict - " + name);
        }

        for (Map.Entry<String, DynamicMethod> entry : mod.getMethods().entrySet()) {
            if (getMethods().containsKey(entry.getKey())) {
                throw argumentError(context, "method would conflict - " + entry.getKey());
            }
        }

        for (Map.Entry<String, DynamicMethod> entry : mod.getMethods().entrySet()) {
            String id = entry.getKey();
            IRubyObject mapped = methodNames.fastARef(asSymbol(context, id));
            if (mapped == NEVER) {
                // unmapped
            } else if (mapped == context.nil) {
                // do not mix
                continue;
            } else {
                id = checkID(context, mapped).idString();
            }
            getMethodLocation().getMethodsForWrite().put(id, entry.getValue().dup());
        }

        return mod;
    }

    private void setVisibility(ThreadContext context, IRubyObject[] args, Visibility visibility) {
        if (args.length == 0) {
            // Note: we change current frames visibility here because the methods which call
            // this method are all "fast" (e.g. they do not created their own frame).
            context.setCurrentVisibility(visibility);
        } else {
            setMethodVisibility(context, args, visibility);
        }
    }

    /** rb_mod_public
     *
     */
    @JRubyMethod(name = "public", rest = true, visibility = PRIVATE, writes = VISIBILITY)
    public IRubyObject _public(ThreadContext context, IRubyObject[] args) {
        checkFrozen();
        setVisibility(context, args, PUBLIC);

        switch (args.length) {
            case 0: return context.nil;
            case 1: return args[0];
            default: return newArray(context, args);
        }
    }

    @Deprecated
    public IRubyObject rbPublic(ThreadContext context, IRubyObject[] args) {
        _public(context, args);
        return this;
    }

    /** rb_mod_protected
     *
     */
    @JRubyMethod(name = "protected", rest = true, visibility = PRIVATE, writes = VISIBILITY)
    public IRubyObject _protected(ThreadContext context, IRubyObject[] args) {
        checkFrozen();
        setVisibility(context, args, PROTECTED);

        switch (args.length) {
            case 0: return context.nil;
            case 1: return args[0];
            default: return newArray(context, args);
        }
    }

    @Deprecated
    public IRubyObject rbProtected(ThreadContext context, IRubyObject[] args) {
        _protected(context, args);
        return this;
    }

    /** rb_mod_private
     *
     */
    @JRubyMethod(name = "private", rest = true, visibility = PRIVATE, writes = VISIBILITY)
    public IRubyObject _private(ThreadContext context, IRubyObject[] args) {
        checkFrozen();
        setVisibility(context, args, PRIVATE);

        switch (args.length) {
            case 0: return context.nil;
            case 1: return args[0];
            default: return newArray(context, args);
        }
    }

    @Deprecated
    public IRubyObject rbPrivate(ThreadContext context, IRubyObject[] args) {
        _private(context, args);
        return this;
    }

    /** rb_mod_modfunc
     *
     */
    @JRubyMethod(name = "module_function", rest = true, visibility = PRIVATE, writes = VISIBILITY)
    public IRubyObject _module_function(ThreadContext context, IRubyObject[] args) {
        if (!isModule()) throw typeError(context, "module_function must be called for modules");

        if (args.length == 0) {
            context.setCurrentVisibility(MODULE_FUNCTION);
        } else {
            setMethodVisibility(context, args, PRIVATE);

            var singleton = singletonClass(context);
            for (int i = 0; i < args.length; i++) {
                RubySymbol name = checkID(context, args[i]);
                DynamicMethod newMethod = deepMethodSearch(context, name.idString()).method.dup();
                newMethod.setImplementationClass(singleton);
                newMethod.setVisibility(PUBLIC);
                singleton.addMethod(context, name.idString(), newMethod);
                singleton.methodAdded(context, name);
            }
        }

        switch (args.length) {
            case 0: return context.nil;
            case 1: return args[0];
            default: return newArray(context, args);
        }
    }

    public IRubyObject module_function(ThreadContext context, IRubyObject[] args) {
        _module_function(context, args);
        return this;
    }

    @JRubyMethod(name = "const_added", required = 1, visibility = PRIVATE)
    public IRubyObject const_added(ThreadContext context, IRubyObject _newConstant) {
        return context.nil;
    }

    @JRubyMethod(name = "method_added", visibility = PRIVATE)
    public IRubyObject method_added(ThreadContext context, IRubyObject nothing) {
        return context.nil;
    }

    @JRubyMethod(name = "method_removed", visibility = PRIVATE)
    public IRubyObject method_removed(ThreadContext context, IRubyObject nothing) {
        return context.nil;
    }

    @JRubyMethod(name = "method_undefined", visibility = PRIVATE)
    public IRubyObject method_undefined(ThreadContext context, IRubyObject nothing) {
        return context.nil;
    }

    @JRubyMethod(name = "method_defined?")
    public RubyBoolean method_defined_p(ThreadContext context, IRubyObject symbol) {
        return asBoolean(context, isMethodBound(checkID(context, symbol).idString(), true));
    }

    @JRubyMethod(name = "method_defined?")
    public RubyBoolean method_defined_p(ThreadContext context, IRubyObject symbol, IRubyObject includeSuper) {
        boolean parents = includeSuper.isTrue();

        if (parents) return method_defined_p(context, symbol);

        Visibility visibility = checkMethodVisibility(context, symbol, parents);
        return asBoolean(context, visibility != UNDEFINED && visibility != PRIVATE);
    }

    @JRubyMethod(name = "public_method_defined?")
    public IRubyObject public_method_defined(ThreadContext context, IRubyObject symbol) {
        return asBoolean(context, checkMethodVisibility(context, symbol, true) == PUBLIC);
    }

    @JRubyMethod(name = "public_method_defined?")
    public IRubyObject public_method_defined(ThreadContext context, IRubyObject symbol, IRubyObject includeSuper) {
        boolean parents = includeSuper.isTrue();

        return asBoolean(context, checkMethodVisibility(context, symbol, parents) == PUBLIC);
    }

    @JRubyMethod(name = "protected_method_defined?")
    public IRubyObject protected_method_defined(ThreadContext context, IRubyObject symbol) {
        return asBoolean(context, checkMethodVisibility(context, symbol, true) == PROTECTED);
    }

    @JRubyMethod(name = "protected_method_defined?")
    public IRubyObject protected_method_defined(ThreadContext context, IRubyObject symbol, IRubyObject includeSuper) {
        boolean parents = includeSuper.isTrue();

        return asBoolean(context, checkMethodVisibility(context, symbol, parents) == PROTECTED);
    }

    @JRubyMethod(name = "private_method_defined?")
    public IRubyObject private_method_defined(ThreadContext context, IRubyObject symbol) {
        return asBoolean(context, checkMethodVisibility(context, symbol, true) == PRIVATE);
    }

    @JRubyMethod(name = "private_method_defined?")
    public IRubyObject private_method_defined(ThreadContext context, IRubyObject symbol, IRubyObject includeSuper) {
        boolean parents = includeSuper.isTrue();

        return asBoolean(context, checkMethodVisibility(context, symbol, parents) == PRIVATE);
    }

    private Visibility checkMethodVisibility(ThreadContext context, IRubyObject symbol, boolean parents) {
        String name = checkID(context, symbol).idString();

        RubyModule mod = this;

        if (!parents) mod = getMethodLocation();

        CacheEntry entry = mod.searchWithCache(name);
        DynamicMethod method = entry.method;

        if (method.isUndefined()) return Visibility.UNDEFINED;

        if (!parents && entry.sourceModule != mod) return Visibility.UNDEFINED;

        return method.getVisibility();
    }

    @Deprecated(since = "10.0")
    public RubyModule public_class_method(IRubyObject[] args) {
        return public_class_method(getCurrentContext(), args);
    }

    @JRubyMethod(name = "public_class_method", rest = true)
    public RubyModule public_class_method(ThreadContext context, IRubyObject[] args) {
        checkFrozen();
        singletonClass(context).setMethodVisibility(context, args, PUBLIC);
        return this;
    }

    @Deprecated(since = "10.0")
    public RubyModule private_class_method(IRubyObject[] args) {
        return private_class_method(getCurrentContext(), args);
    }

    @JRubyMethod(name = "private_class_method", rest = true)
    public RubyModule private_class_method(ThreadContext context, IRubyObject[] args) {
        checkFrozen();
        singletonClass(context).setMethodVisibility(context, args, PRIVATE);
        return this;
    }

    /**
     * Add an alias under the name newId pointing at the method under the name oldId.
     *
     * @param context the current context
     * @param newId the new name for the alias
     * @param oldId the current name of the method
     * @return the new name
     */
    @JRubyMethod(name = "alias_method")
    public IRubyObject aliasMethod(ThreadContext context, IRubyObject newId, IRubyObject oldId) {
        RubySymbol newSym = checkID(context, newId);
        RubySymbol oldSym = checkID(context, oldId); //  MRI uses rb_to_id but we return existing symbol

        defineAlias(context, newSym.idString(), oldSym.idString());

        methodAdded(context, newSym);

        return newSym;
    }

    /**
     * Old version of {@link #aliasMethod(ThreadContext, IRubyObject, IRubyObject)} that returns this module. The Ruby
     * alias_method method was updated in 3.0 to return the new name.
     *
     * @param context the current context
     * @param newId the new name for the alias
     * @param oldId the current name of the method
     * @return this module
     */
    public RubyModule alias_method(ThreadContext context, IRubyObject newId, IRubyObject oldId) {
        aliasMethod(context, newId, oldId);

        return this;
    }

    @JRubyMethod(name = "undef_method", rest = true)
    public RubyModule undef_method(ThreadContext context, IRubyObject[] args) {
        for (int i=0; i<args.length; i++) {
            RubySymbol name = checkID(context, args[i]);

            undef(context, name.idString());
        }
        return this;
    }

    @JRubyMethod(name = {"module_eval", "class_eval"},
            reads = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE},
            writes = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE},
            keywords = true)
    public IRubyObject module_eval(ThreadContext context, Block block) {
        return specificEval(context, this, block, EvalType.MODULE_EVAL);
    }
    @JRubyMethod(name = {"module_eval", "class_eval"},
            reads = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE},
            writes = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE},
            keywords = true)
    public IRubyObject module_eval(ThreadContext context, IRubyObject arg0, Block block) {
        return specificEval(context, this, arg0, block, EvalType.MODULE_EVAL);
    }
    @JRubyMethod(name = {"module_eval", "class_eval"},
            reads = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE},
            writes = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE},
            keywords = true)
    public IRubyObject module_eval(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        return specificEval(context, this, arg0, arg1, block, EvalType.MODULE_EVAL);
    }
    @JRubyMethod(name = {"module_eval", "class_eval"},
            reads = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE},
            writes = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE},
            keywords = true)
    public IRubyObject module_eval(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return specificEval(context, this, arg0, arg1, arg2, block, EvalType.MODULE_EVAL);
    }

    // This is callable and will work but the rest = true is put so we can match the expected arity error message
    // Just relying on annotations will give us: got n expected 0..3 when we want got n expected 1..3.
    @JRubyMethod(name = {"module_eval", "class_eval"}, rest = true,
            reads = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE},
            writes = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE})
    public IRubyObject module_eval(ThreadContext context, IRubyObject[] args, Block block) {
        switch(args.length) {
            case 0: return module_eval(context, block);
            case 1: return module_eval(context, args[0], block);
            case 2: return module_eval(context, args[0], args[1], block);
            case 3: return module_eval(context, args[0], args[1], args[2], block);
        }

        throw argumentError(context, args.length, 1, 3);
    }

    @JRubyMethod(name = {"module_exec", "class_exec"},
            reads = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE},
            writes = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE},
            keywords = true)
    public IRubyObject module_exec(ThreadContext context, Block block) {
        if (block.isGiven()) {
            return yieldUnder(context, this, IRubyObject.NULL_ARRAY, block.cloneBlockAndFrame(), EvalType.MODULE_EVAL);
        } else {
            throw context.runtime.newLocalJumpErrorNoBlock();
        }
    }

    @JRubyMethod(name = {"module_exec", "class_exec"}, rest = true,
            reads = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE},
            writes = {LASTLINE, BACKREF, VISIBILITY, BLOCK, SELF, METHODNAME, LINE, CLASS, FILENAME, SCOPE},
            keywords = true)
    public IRubyObject module_exec(ThreadContext context, IRubyObject[] args, Block block) {
        if (block.isGiven()) {
            return yieldUnder(context, this, args, block.cloneBlockAndFrame(), EvalType.MODULE_EVAL);
        } else {
            throw context.runtime.newLocalJumpErrorNoBlock();
        }
    }

    @JRubyMethod(name = "remove_method", rest = true)
    public RubyModule remove_method(ThreadContext context, IRubyObject[] args) {
        for (var name: args) {
            removeMethod(context, checkID(context, name).idString());
        }
        return this;
    }

    @Deprecated(since = "10.0", forRemoval = true)
    @SuppressWarnings("removal")
    public static void marshalTo(RubyModule module, org.jruby.runtime.marshal.MarshalStream output) throws java.io.IOException {
        var context = module.getRuntime().getCurrentContext();
        output.registerLinkTarget(context, module);
        output.writeString(org.jruby.runtime.marshal.MarshalStream.getPathFromClass(module));
    }

    public static void marshalTo(ThreadContext context, RubyOutputStream out, RubyModule module, MarshalDumper output) {
        output.registerLinkTarget(module);
        output.writeString(out, MarshalDumper.getPathFromClass(context, module).idString());
    }

    @Deprecated(since = "10.0", forRemoval = true)
    @SuppressWarnings("removal")
    public static RubyModule unmarshalFrom(org.jruby.runtime.marshal.UnmarshalStream input) throws java.io.IOException {
        String name = RubyString.byteListToString(input.unmarshalString());

        return org.jruby.runtime.marshal.UnmarshalStream.getModuleFromPath(input.getRuntime(), name);
    }

    public static RubyModule unmarshalFrom(ThreadContext context, RubyInputStream in, MarshalLoader input) {
        String name = RubyString.byteListToString(input.unmarshalString(context, in));

        return MarshalLoader.getModuleFromPath(context, name);
    }

    /* Module class methods */

    /**
     * Return an array of nested modules or classes.
     */
    @JRubyMethod(name = "nesting", reads = SCOPE, meta = true)
    public static RubyArray nesting(ThreadContext context, IRubyObject recv, Block block) {
        RubyModule object = objectClass(context);
        StaticScope scope = context.getCurrentStaticScope();
        var result = newArray(context);

        for (StaticScope current = scope; current.getModule() != object; current = current.getPreviousCRefScope()) {
            result.append(context, current.getModule());
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
    void doIncludeModule(ThreadContext context, RubyModule baseModule) {
        List<RubyModule> modulesToInclude = gatherModules(baseModule);

        RubyModule currentInclusionPoint = getMethodLocation();
        ModuleLoop: for (RubyModule nextModule : modulesToInclude) {
            checkForCyclicInclude(context, nextModule);

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

            currentInclusionPoint = proceedWithInclude(context, currentInclusionPoint, nextModule.getDelegate());
        }
    }

    IncludedModuleWrapper findModuleInAncestors(RubyModule arg) {
        for (RubyClass nextClass = getSuperClass(); nextClass != null; nextClass = nextClass.getSuperClass()) {
            if (nextClass.isIncluded()) {
                // does the class equal the module
                if (nextClass.getDelegate() == arg.getDelegate()) {
                    // next in hierarchy is an included version of the module we're attempting,
                    // so we skip including it
                    return (IncludedModuleWrapper) nextClass;
                }
            }
        }

        return null;
    }

    /**
     * Prepend the given module and all related modules into the hierarchy above
     * this module/class. Inspects the hierarchy to ensure the same module isn't
     * included twice, and selects an appropriate insertion point for each incoming
     * module.
     *
     * @param baseModule The module to prepend, along with any modules it itself includes
     */
    void doPrependModule(ThreadContext context, RubyModule baseModule) {
        List<RubyModule> modulesToInclude = gatherModules(baseModule);
        RubyModule startOrigin = getMethodLocation();

        if (!hasPrepends()) { // Set up a new holder class to hold all this types original methods.
            RubyClass origin = new PrependedModule(context.runtime, getSuperClass(), this);

            // if the insertion point is a class, update subclass lists
            if (this instanceof RubyClass) {
                // if there's a non-null superclass, we're including into a normal class hierarchy;
                // update subclass relationships to avoid stale parent/child relationships
                if (getSuperClass() != null) {
                    getSuperClass().replaceSubclass((RubyClass) this, origin);
                }

                origin.addSubclass((RubyClass) this);
            }
            superClass(origin);
        }

        RubyModule inclusionPoint = this;
        ModuleLoop: for (RubyModule nextModule : modulesToInclude) {
            checkForCyclicPrepend(context, nextModule);

            boolean startSeen = false;
            boolean superclassSeen = false;

            if (startOrigin == this) {
                startSeen = true;
            }

            // scan prepend section of hierarchy for module, from superClass to the next concrete superClass
            RubyModule stopClass = getPrependCeiling();
            if (startOrigin != this) {
                for (RubyClass nextClass = getSuperClass(); nextClass != stopClass; nextClass = nextClass.getSuperClass()) {
                    if (startOrigin == nextClass) {
                        break;
                    }
                    if (this == nextClass) {
                        startSeen = true;
                    }
                    if (nextClass.isIncluded()) {
                        // does the class equal the module
                        if (nextClass.getDelegate() == nextModule.getDelegate()) {
                            // next in hierarchy is an included version of the module we're attempting,
                            // so we skip including it

                            // if we haven't encountered a real superclass, use the found module as the new inclusion point
                            if (!superclassSeen && startSeen) inclusionPoint = nextClass;

                            continue ModuleLoop;
                        }
                    } else {
                        superclassSeen = true;
                    }
                }
            }

            inclusionPoint = proceedWithPrepend(context, inclusionPoint, nextModule.getDelegate());
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
     * @param context the current thread context
     * @param insertAbove The hierarchy target above which to include the wrapped module
     * @param moduleToInclude The module to wrap and include
     * @return The new module wrapper resulting from this include
     */
    private RubyModule proceedWithInclude(ThreadContext context, RubyModule insertAbove, RubyModule moduleToInclude) {
        // In the current logic, if we getService here we know that module is not an
        // IncludedModuleWrapper, so there's no need to fish out the delegate. But just
        // in case the logic should change later, let's do it anyway
        RubyClass wrapper = new IncludedModuleWrapper(context.runtime, insertAbove.getSuperClass(), moduleToInclude, moduleToInclude.getMethodLocation());

        // if the insertion point is a class, update subclass lists
        if (insertAbove instanceof RubyClass insertAboveClass) {
            // if there's a non-null superclass, we're including into a normal class hierarchy;
            // update subclass relationships to avoid stale parent/child relationships
            if (insertAboveClass.getSuperClass() != null) {
                insertAboveClass.getSuperClass().replaceSubclass(insertAboveClass, wrapper);
            }

            wrapper.addSubclass(insertAboveClass);
        }

        insertAbove.superClass(wrapper);
        insertAbove = insertAbove.getSuperClass();

        if (isRefinement()) {
            wrapper.getMethods().forEach((name, method) -> addRefinedMethodEntry(context, name, method));
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
    private RubyModule proceedWithPrepend(ThreadContext context, RubyModule insertBelow, RubyModule moduleToPrepend) {
        return proceedWithInclude(context, insertBelow, !moduleToPrepend.isPrepended() ?
                moduleToPrepend.getOrigin() :
                moduleToPrepend);
    }


    //
    ////////////////// CLASS VARIABLE RUBY METHODS ////////////////
    //

    @JRubyMethod(name = "class_variable_defined?")
    public IRubyObject class_variable_defined_p(ThreadContext context, IRubyObject var) {
        String id = validateClassVariable(context, var);

        for (RubyModule module = this; module != null; module = module.getSuperClass()) {
            if (module.hasClassVariable(id)) return context.tru;
        }

        return context.fals;
    }

    @Deprecated
    public IRubyObject class_variable_get19(IRubyObject name) {
        return class_variable_get(name);
    }

    @Deprecated(since = "10.0")
    public IRubyObject class_variable_get(IRubyObject name) {
        return class_variable_get(getCurrentContext(), name);
    }

    /** rb_mod_cvar_get
     *
     */
    @JRubyMethod(name = "class_variable_get")
    public IRubyObject class_variable_get(ThreadContext context, IRubyObject name) {
        return getClassVar(context, name, validateClassVariable(context, name));
    }

    @Deprecated(since = "10.0")
    public IRubyObject class_variable_set(IRubyObject name, IRubyObject value) {
        return class_variable_set(getCurrentContext(), name, value);
    }

    /** rb_mod_cvar_set
     *
     */
    @JRubyMethod(name = "class_variable_set")
    public IRubyObject class_variable_set(ThreadContext context, IRubyObject name, IRubyObject value) {
        return setClassVar(context, validateClassVariable(context, name), value);
    }

    @Deprecated
    public IRubyObject class_variable_set19(IRubyObject name, IRubyObject value) {
        return class_variable_set(name, value);
    }

    /** rb_mod_remove_cvar
     *
     */
    @JRubyMethod(name = "remove_class_variable")
    public IRubyObject remove_class_variable(ThreadContext context, IRubyObject name) {
        return removeClassVariable(context, validateClassVariable(context, name));
    }

    @Deprecated
    public IRubyObject remove_class_variable19(ThreadContext context, IRubyObject name) {
        return remove_class_variable(context, name);
    }

    @JRubyMethod(name = "class_variables")
    public RubyArray class_variables(ThreadContext context) {
        var ary = newArray(context);
        for (String name : classVariablesCommon(true)) {
            ary.add(asSymbol(context, name));
        }
        return ary;
    }

    @JRubyMethod(name = "class_variables")
    public RubyArray class_variables(ThreadContext context, IRubyObject inherit) {
        var ary = newArray(context);
        for (String name : classVariablesCommon(inherit.isTrue())) {
            ary.add(asSymbol(context, name));
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
        return asBoolean(context, constDefined(context, name, true));
    }

    @JRubyMethod(name = "const_defined?")
    public RubyBoolean const_defined_p(ThreadContext context, IRubyObject name, IRubyObject recurse) {
        return asBoolean(context, constDefined(context, name, recurse.isTrue()));
    }

    private boolean constDefined(ThreadContext context, IRubyObject name, boolean inherit) {
        if (name instanceof RubySymbol sym) {
            if (!sym.validConstantName()) {
                throw nameError(context, str(context.runtime, "wrong constant name ", ids(context.runtime, sym)), sym);
            }

            String id = sym.idString();

            return inherit ? constDefined(context, id) : constDefinedAt(context, id);
        }

        RubyString fullName = name.convertToString();
        ByteList value = fullName.getByteList();
        ByteList pattern = COLON_COLON;
        Encoding enc = pattern.getEncoding();
        byte[] bytes = value.getUnsafeBytes();
        int begin = value.getBegin();
        int realSize = value.getRealSize();
        int end = begin + realSize;
        int currentOffset = 0;
        int patternIndex;
        int index = 0;
        RubyModule mod = this;
        boolean includeObject = true;

        if (value.startsWith(pattern)) {
            mod = objectClass(context);
            currentOffset += 2;
        }

        for (; currentOffset < realSize && (patternIndex = value.indexOf(pattern, currentOffset)) >= 0; index++) {
            int t = enc.rightAdjustCharHead(bytes, currentOffset + begin, patternIndex + begin, end) - begin;
            if (t != patternIndex) {
                currentOffset = t;
                continue;
            }

            ByteList segment = value.makeShared(currentOffset, patternIndex - currentOffset);
            String id = RubySymbol.newConstantSymbol(context, fullName, segment).idString();

            IRubyObject obj;

            if (!inherit) {
                if (!mod.constDefinedAt(context, id)) return false;
                obj = mod.getConstantAt(context, id);
            } else if (index == 0 && segment.realSize() == 0) {
                if (!mod.constDefined(context, id)) return false;
                obj = mod.getConstant(context, id);
            } else {
                if (!mod.constDefinedFrom(context, id)) return false;
                obj = mod.getConstantFrom(context, id);
            }

            if (!(obj instanceof RubyModule)) throw typeError(context, segment + " does not refer to class/module");

            mod = (RubyModule) obj;
            currentOffset = patternIndex + pattern.getRealSize();
            includeObject = false;
        }

        if (mod == null) mod = this; // Bare 'Foo'

        ByteList lastSegment = value.makeShared(currentOffset, realSize - currentOffset);

        String id = RubySymbol.newConstantSymbol(context, fullName, lastSegment).idString();

        return mod.getConstantSkipAutoload(context, id, inherit, inherit && includeObject, false) != null;
    }

    // MRI: rb_mod_const_get
    @JRubyMethod(name = "const_get")
    public IRubyObject const_get(ThreadContext context, IRubyObject arg0) {
        return constGetCommon(context, arg0, true);
    }

    // MRI: rb_mod_const_get
    @JRubyMethod(name = "const_get")
    public IRubyObject const_get(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        return constGetCommon(context, arg0, arg1.isTrue());
    }

    private IRubyObject constGetCommon(ThreadContext context, IRubyObject symbol, boolean inherit) {
        RubySymbol fullName = checkID(context, symbol);
        String name = fullName.idString();

        int sep = name.indexOf("::");
        // symbol form does not allow ::
        if (symbol instanceof RubySymbol && sep != -1) throw nameError(context, "wrong constant name ", fullName);

        RubyModule mod = this;

        if (sep == 0) { // ::Foo::Bar
            mod = objectClass(context);
            name = name.substring(2);
        }

        // Bare ::
        if (name.isEmpty()) throw nameError(context, "wrong constant name ", fullName);

        boolean firstConstant = true;
        while ( ( sep = name.indexOf("::") ) != -1 ) {
            final String segment = name.substring(0, sep);
            IRubyObject obj = mod.getConstant(context, validateConstant(context, segment, symbol), inherit, inherit);
            if (!(obj instanceof RubyModule)) throw typeError(context, segment + " does not refer to class/module");
            mod = (RubyModule) obj;
            name = name.substring(sep + 2);
            firstConstant = false;
        }

        return mod.getConstant(context,
                validateConstant(context, name, symbol), firstConstant && inherit, firstConstant && inherit);
    }

    public static boolean isValidConstantPath(RubyString str) {
        ByteList byteList = str.getByteList();

        int index = 0;
        int sep = 0;

        while ((sep = byteList.indexOf(COLON_COLON, index)) != -1) {
            if (sep == 0) {
                index += 2;
                continue;  // '^::'
            }

            if (!isValidConstantName(byteList, index, sep)) return false;

            index = sep + 2;
        }

        return isValidConstantName(byteList, index, byteList.realSize());
    }

    public static boolean isValidConstantName(ByteList bytelist, int start, int end) {
        // empty string
        if (start == end) return false;

        Encoding enc = bytelist.getEncoding();
        byte[] bytes = bytelist.unsafeBytes();
        int beg = bytelist.begin();
        int p = beg + start;
        int e = beg + end; // We assume end is always a valid offset.
        int l = StringSupport.preciseLength(enc, bytes, p, e);
        int c = StringSupport.codePoint(enc, bytes, p, e);
        if (!enc.isUpper(c)) return false;
        p += l;

        while (p < e) {
            l = StringSupport.preciseLength(enc, bytes, p, e);
            c = StringSupport.codePoint(enc, bytes, p, e);

            if (!enc.isAlnum(c)) return false;

            p += l;
        }

        return true;
    }

    @JRubyMethod(required = 1, optional = 1, checkArity = false)
    public IRubyObject const_source_location(ThreadContext context, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 1, 2);
        boolean inherit = argc == 1 || ( ! args[1].isNil() && args[1].isTrue() );

        final IRubyObject symbol = args[0];
        String name = symbol instanceof RubySymbol ?
                checkID(context, symbol).idString() :
                symbol.convertToString().asJavaString();

        int sep = name.indexOf("::");
        // symbol form does not allow ::
        if (symbol instanceof RubySymbol && sep != -1) throw nameError(context, "wrong constant name", symbol);

        RubyModule mod = this;

        if (sep == 0) { // ::Foo::Bar
            mod = objectClass(context);
            name = name.substring(2);
        }

        // Bare ::
        if (name.isEmpty()) throw nameError(context, "wrong constant name ", symbol);

        while ( ( sep = name.indexOf("::") ) != -1 ) {
            final String segment = name.substring(0, sep);
            IRubyObject obj = mod.getConstant(context, validateConstant(context, segment, symbol), inherit, inherit);
            if (!(obj instanceof RubyModule)) throw typeError(context, segment + " does not refer to class/module");
            mod = (RubyModule) obj;
            name = name.substring(sep + 2);
        }

        SourceLocation location = mod.getConstantSourceLocation(context,
                validateConstant(context, name, symbol), inherit, inherit);

        if (location != null && location.getFile() != null) {
            return location.getFile().equals(BUILTIN_CONSTANT) ?
                    newEmptyArray(context) :
                    newArray(context, newString(context, location.getFile()), asFixnum(context, location.getLine()));
        }

        return context.nil;
    }

    @Deprecated(since = "10.0")
    public IRubyObject const_set(IRubyObject name, IRubyObject value) {
        return const_set(getCurrentContext(), name, value);
    }

    /** rb_mod_const_set
     *
     */
    @JRubyMethod(name = "const_set")
    public IRubyObject const_set(ThreadContext context, IRubyObject name, IRubyObject value) {
        return setConstant(context, validateConstant(context, name), value, context.getFile(), context.getLine() + 1);
    }

    @JRubyMethod(name = "remove_const", visibility = PRIVATE)
    public IRubyObject remove_const(ThreadContext context, IRubyObject rubyName) {
        String id = validateConstant(context, rubyName);
        IRubyObject value = deleteConstant(id);

        if (value != null) { // found it!
            invalidateConstantCache(context, id);

            if (value != UNDEF) return value;

            // autoload entry
            removeAutoload(id);
            return context.nil; // if we weren't auto-loaded MRI returns nil
        }

        if (hasConstantInHierarchy(id)) throw cannotRemoveError(context, id);

        throw nameError(context, "constant " + id + " not defined for " + getName(context), id);
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
    @JRubyMethod(name = "const_missing")
    public IRubyObject const_missing(ThreadContext context, IRubyObject rubyName, Block block) {
        RubyModule privateConstReference = context.getPrivateConstantReference();

        if (privateConstReference != null) {
            context.setPrivateConstantReference(null);
            throw context.runtime.newNameError("private constant " + privateConstReference + "::" + rubyName + " referenced", privateConstReference, rubyName);
        }

        if (this != objectClass(context)) {
            throw context.runtime.newNameError("uninitialized constant %2$s::%1$s", this, rubyName);
        } else {
            throw context.runtime.newNameError("uninitialized constant %1$s", this, rubyName);
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

    private RubyArray<?> constantsCommon(ThreadContext context, boolean replaceModule, boolean allConstants) {
        Collection<String> constantNames = constantsCommon(context.runtime, replaceModule, allConstants, false);
        var array = RubyArray.newBlankArrayInternal(context.runtime, constantNames.size());

        int i = 0;
        for (String name : constantNames) {
            array.storeInternal(context, i++, asSymbol(context, name));
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

    @Deprecated(since = "10.0")
    public void deprecateConstant(Ruby runtime, String name) {
        deprecateConstant(runtime.getCurrentContext(), name);
    }

    /**
     * Mark the named constant as deprecated.
     *
     * @param context the current thread context
     * @param name the name of the constant
     */
    @JRubyAPI
    public void deprecateConstant(ThreadContext context, String name) {
        ConstantEntry entry = getConstantMap().get(name);
        if (entry == null) {
            var runtime = context.runtime;
            throw nameError(context, str(runtime, "constant ", types(runtime, this), "::", ids(runtime, name), " not defined"), name);
        }

        storeConstant(context, name, entry.value, entry.hidden, true);
        invalidateConstantCache(context, name);
    }

    @JRubyMethod
    public IRubyObject deprecate_constant(ThreadContext context, IRubyObject name) {
        checkFrozen();

        deprecateConstant(context, validateConstant(context, name));
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

        String id = validateConstant(context, name);

        setConstantVisibility(context, id, true);
        invalidateConstantCache(context, id);

        return this;
    }

    @JRubyMethod(required = 1, rest = true, checkArity = false)
    public IRubyObject private_constant(ThreadContext context, IRubyObject[] rubyNames) {
        Arity.checkArgumentCount(context, rubyNames, 1, -1);

        for (IRubyObject rubyName : rubyNames) {
            private_constant(context, rubyName);
        }
        return this;
    }

    @JRubyMethod
    public IRubyObject public_constant(ThreadContext context, IRubyObject name) {
        checkFrozen();

        String id = validateConstant(context, name);

        setConstantVisibility(context, id, false);
        invalidateConstantCache(context, id);
        return this;
    }

    @JRubyMethod(required = 1, rest = true, checkArity = false)
    public IRubyObject public_constant(ThreadContext context, IRubyObject[] rubyNames) {
        Arity.checkArgumentCount(context, rubyNames, 1, -1);

        for (IRubyObject rubyName : rubyNames) {
            public_constant(context, rubyName);
        }
        return this;
    }

    @JRubyMethod(name = "prepend", required = 1, rest = true, checkArity = false)
    public IRubyObject prepend(ThreadContext context, IRubyObject[] modules) {
        int argc = Arity.checkArgumentCount(context, modules, 1, -1);

        if (isRefinement()) throw typeError(context, "Refinement#prepend has been removed");

        // MRI checks all types first:
        for (int i = argc; --i >= 0; ) {
            if (!modules[i].isModule()) throw typeError(context, modules[i], "Module");
            if (((RubyModule) modules[i]).isRefinement()) throw typeError(context, "Cannot prepend refinement");
        }

        for (int i = argc - 1; i >= 0; i--) {
            modules[i].callMethod(context, "prepend_features", this);
            modules[i].callMethod(context, "prepended", this);
        }

        return this;
    }

    @JRubyMethod(name = "prepended", visibility = PRIVATE)
    public IRubyObject prepended(ThreadContext context, IRubyObject other) {
        return context.nil;
    }

    @Deprecated(since = "10.0")
    public final void setConstantVisibility(Ruby runtime, String name, boolean hidden) {
        setConstantVisibility(runtime.getCurrentContext(), name, hidden);
    }

    // NOTE: internal API
    public final void setConstantVisibility(ThreadContext context, String name, boolean hidden) {
        ConstantEntry entry = getConstantMap().get(name);

        if (entry == null) {
            throw nameError(context, "constant " + getName(context) + "::" + name + " not defined", name);
        }

        setConstantCommon(context, name, entry.value, hidden, false, entry.getFile(), entry.getLine());
    }

    @JRubyMethod(name = "refinements")
    public IRubyObject refinements(ThreadContext context) {
        RefinementStore refinementStore = this.refinementStore;
        if (refinementStore == null ) {
            return newEmptyArray(context);
        }

        var refinementModules = allocArray(context, refinementStore.refinements.size());

        refinementStore.refinements.forEach((key, value) -> refinementModules.append(context, value));

        return refinementModules;
    }

    @Deprecated(since = "10.0")
    public IRubyObject refined_class(ThreadContext context) {
        return getRefinedClassOrThrow(context, false);
    }

    @JRubyMethod(name = "target")
    public IRubyObject target(ThreadContext context) {
        return getRefinedClassOrThrow(context, true);
    }

    private IRubyObject getRefinedClassOrThrow(ThreadContext context, boolean nameIsTarget) {
        if (isRefinement()) return refinementStore.refinedClass;

        String methodName = nameIsTarget ? "target" : "refined_class";
        String errMsg = RubyStringBuilder.str(context.runtime,
                "undefined method '"+ methodName +"' for ", rubyBaseName(context), ":", getMetaClass());
        throw context.runtime.newNoMethodError(errMsg, this, "target", newEmptyArray(context));
    }

    @JRubyMethod(name = "used_refinements")
    public IRubyObject used_refinements(ThreadContext context) {
        // TODO: not implemented
        return newEmptyArray(context);
    }

    //
    ////////////////// CLASS VARIABLE API METHODS ////////////////
    //

    @Deprecated(since = "10.0")
    public IRubyObject setClassVar(String name, IRubyObject value) {
        return setClassVar(getCurrentContext(), name, value);
    }

    /**
     * Set the named class variable to the given value, provided freeze allows setting it.
     *
     * Ruby C equivalent = "rb_cvar_set"
     *
     * @param name The variable name to set
     * @param value The value to set it to
     */
    public IRubyObject setClassVar(ThreadContext context, String name, IRubyObject value) {
        RubyModule module = this;
        RubyModule highest = this;
        do {
            if (module.hasClassVariable(name)) {
                highest = module;
            }
        } while ((module = module.getSuperClass()) != null);

        return highest.storeClassVariable(context, name, value);
    }

    @Deprecated
    public IRubyObject fastSetClassVar(final String internedName, final IRubyObject value) {
        return setClassVar(getCurrentContext(), internedName, value);
    }

    @Deprecated(since = "10.0")
    public IRubyObject getClassVar(String name) {
        return getClassVar(getCurrentContext(), name);
    }

    /**
     * Retrieve the specified class variable, searching through this module, included modules, and supermodules.
     *
     * Ruby C equivalent = "rb_cvar_get"
     *
     * @param name The name of the variable to retrieve
     * @return The variable's value, or throws NameError if not found
     */
    public IRubyObject getClassVar(ThreadContext context, String name) {
        IRubyObject value = getClassVarQuiet(context, name);

        if (value == null) throw context.runtime.newNameError("uninitialized class variable %1$s in %2$s", this, name);

        return value;
    }

    @Deprecated(since = "10.0")
    public IRubyObject getClassVar(IRubyObject nameObject, String name) {
        return getClassVar(getCurrentContext(), nameObject, name);
    }

    public IRubyObject getClassVar(ThreadContext context, IRubyObject nameObject, String name) {
        IRubyObject value = getClassVarQuiet(context, name);

        if (value == null) {
            throw context.runtime.newNameError("uninitialized class variable %1$s in %2$s", this, nameObject);
        }

        return value;
    }

    @Deprecated(since = "10.0")
    public IRubyObject getClassVarQuiet(String name) {
        return getClassVarQuiet(getCurrentContext(), name);
    }

    private IRubyObject getClassVarQuiet(ThreadContext context, String name) {
        assert IdUtil.isClassVariable(name);
        RubyModule module = this;
        RubyModule highest = null;
        RubyModule lowest = null;

        do {
            if (module.hasClassVariable(name)) {
                highest = module;
                if (lowest == null) lowest = module;
            }
        } while ((module = module.getSuperClass()) != null);

        if (lowest != highest) {
            if (!highest.isPrepended()) {
                if (lowest.getOrigin().getRealModule() != highest.getOrigin().getRealModule()) {
                    throw runtimeError(context, str(context.runtime, "class variable " + name + " of ",
                            lowest.getOrigin(), " is overtaken by ", highest.getOrigin()));
                }

                if (lowest.isClass()) lowest.removeClassVariable(name);
            }

        }

        if (highest != null) return highest.fetchClassVariable(name);

        return null;
    }

    @Deprecated
    public IRubyObject fastGetClassVar(String internedName) {
        return getClassVar(getCurrentContext(), internedName);
    }

    /**
     * Is class var defined?
     *
     * Ruby C equivalent = "rb_cvar_defined"
     *
     * @param name The class var to determine "is defined?"
     * @return true if true, false if false
     */
    // runtime-free
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

    @Deprecated(since = "10.0")
    public IRubyObject removeClassVariable(String name) {
        return removeClassVariable(getCurrentContext(), name);
    }

    public IRubyObject removeClassVariable(ThreadContext context, String name) {
        String javaName = validateClassVariable(context, name);
        IRubyObject value = deleteClassVariable(context, javaName);
        if (value != null) return value;

        throw isClassVarDefined(javaName) ?
                cannotRemoveError(context, javaName):
                nameError(context, "class variable " + javaName + " not defined for " + getName(context), javaName);
    }


    //
    ////////////////// CONSTANT API METHODS ////////////////
    //

    @Deprecated(since = "10.0")
    public IRubyObject getConstantAtSpecial(String name) {
        return getConstantAtSpecial(getCurrentContext(), name);
    }

    /**
     * This version searches superclasses if we're starting with Object. This
     * corresponds to logic in rb_const_defined_0 that recurses for Object only.
     *
     * @param name the constant name to find
     * @return the constant, or null if it was not found
     */
    public IRubyObject getConstantAtSpecial(ThreadContext context, String name) {
        IRubyObject value = this == objectClass(context) ?
                getConstantNoConstMissing(context, name) :
                fetchConstant(context, name);

        return value == UNDEF ? resolveUndefConstant(context, name) : value;
    }

    @Deprecated(since = "10.0")
    public IRubyObject getConstantAt(String name) {
        return getConstantAt(getCurrentContext(), name);
    }

    public IRubyObject getConstantAt(ThreadContext context, String name) {
        return getConstantAt(context, name, true);
    }

    @Deprecated(since = "10.0")
    public IRubyObject getConstantAt(String name, boolean includePrivate) {
        return getConstantAt(getCurrentContext(), name, includePrivate);
    }

    public IRubyObject getConstantAt(ThreadContext context, String name, boolean includePrivate) {
        IRubyObject value = fetchConstant(context, name, includePrivate);

        return value == UNDEF ? resolveUndefConstant(context, name) : value;
    }

    @Deprecated
    public IRubyObject fastGetConstantAt(String internedName) {
        return getConstantAt(getCurrentContext(), internedName);
    }

    @Deprecated(since = "10.0")
    public IRubyObject getConstant(String name) {
        return getConstant(getCurrentContext(), name);
    }

    /**
     * Retrieve the named constant, invoking 'const_missing' should that be appropriate.
     *
     * @param name The constant to retrieve
     * @return The value for the constant, or null if not found
     */
    public IRubyObject getConstant(ThreadContext context, String name) {
        return getConstant(context, name, true);
    }

    @Deprecated(since = "10.0")
    public IRubyObject getConstant(String name, boolean inherit) {
        return getConstant(getCurrentContext(), name, inherit);
    }

    public IRubyObject getConstant(ThreadContext context, String name, boolean inherit) {
        return getConstant(context, name, inherit, true);
    }

    @Deprecated(since = "10.0")
    public SourceLocation getConstantSourceLocation(String name, boolean inherit, boolean includeObject) {
        return getConstantSourceLocation(getCurrentContext(), name, inherit, includeObject);
    }

    public SourceLocation getConstantSourceLocation(ThreadContext context, String name, boolean inherit, boolean includeObject) {
        SourceLocation location = iterateConstantEntryNoConstMissing(name, this, inherit);
        var Object = objectClass(context);

        if (location == null && !isClass() && includeObject) {
            location = iterateConstantEntryNoConstMissing(name, Object, inherit);
        }

        return location;
    }

    @Deprecated(since = "10.0")
    public IRubyObject getConstant(String name, boolean inherit, boolean includeObject) {
        return getConstant(getCurrentContext(), name, inherit, includeObject);
    }

    public IRubyObject getConstant(ThreadContext context, String name, boolean inherit, boolean includeObject) {
        assert name != null : "null name";
        //assert IdUtil.isConstant(name) : "invalid constant name: " + name;
        // NOTE: can not assert IdUtil.isConstant(name) until unmarshal-ing is using this for Java classes
        // since some classes won't assert the upper case first char (anonymous classes start with a digit)

        IRubyObject value = getConstantNoConstMissing(context, name, inherit, includeObject);
        return value != null ? value : callMethod(context, "const_missing", asSymbol(context, name));
    }

    @Deprecated
    public IRubyObject fastGetConstant(String internedName) {
        return getConstant(getCurrentContext(), internedName);
    }

    @Deprecated
    public IRubyObject fastGetConstant(String internedName, boolean inherit) {
        return getConstant(getCurrentContext(), internedName, inherit);
    }

    @Deprecated(since = "10.0")
    public IRubyObject getConstantNoConstMissing(String name) {
        return getConstantNoConstMissing(getCurrentContext(), name);
    }

    public IRubyObject getConstantNoConstMissing(ThreadContext context, String name) {
        return getConstantNoConstMissing(context, name, true);
    }

    @Deprecated(since = "10.0")
    public IRubyObject getConstantNoConstMissing(String name, boolean inherit) {
        return getConstantNoConstMissing(getCurrentContext(), name, inherit);
    }

    public IRubyObject getConstantNoConstMissing(ThreadContext context, String name, boolean inherit) {
        return getConstantNoConstMissing(context, name, inherit, true);
    }

    @Deprecated(since = "10.0")
    public IRubyObject getConstantNoConstMissing(String name, boolean inherit, boolean includeObject) {
        return getConstantNoConstMissing(getCurrentContext(), name, inherit, includeObject);
    }

    public IRubyObject getConstantNoConstMissing(ThreadContext context, String name, boolean inherit, boolean includeObject) {
        IRubyObject constant = iterateConstantNoConstMissing(context, name, this, inherit, true, true);

        if (constant == null && !isClass() && includeObject) {
            constant = iterateConstantNoConstMissing(context, name, objectClass(context), inherit, true, true);
        }

        return constant;
    }

    @Deprecated(since = "10.0")
    public final IRubyObject getConstantNoConstMissingSkipAutoload(String name) {
        return getConstantNoConstMissingSkipAutoload(getCurrentContext(), name);
    }

    public final IRubyObject getConstantNoConstMissingSkipAutoload(ThreadContext context, String name) {
        return getConstantSkipAutoload(context, name, true, true, true);
    }

    @Deprecated
    public IRubyObject getConstantNoConstMissingSKipAutoload(String name) {
        return getConstantSkipAutoload(getCurrentContext(), name, true, true, true);
    }

    // returns null for autoloads that have failed
    private IRubyObject getConstantSkipAutoload(ThreadContext context, String name, boolean inherit, boolean searchObject, boolean inheritObject) {
        IRubyObject constant = iterateConstantNoConstMissing(context, name, this, inherit, false, inheritObject);

        if (constant == null && !isClass() && searchObject) {
            constant = iterateConstantNoConstMissing(context, name, getRuntime().getObject(), inherit, false, true);
        }

        return constant;
    }

    // runtime-free
    private static SourceLocation iterateConstantEntryNoConstMissing(String name, RubyModule init, boolean inherit) {
        for (RubyModule mod = init; mod != null; mod = mod.getSuperClass()) {
            ConstantEntry entry = mod.constantEntryFetch(name);

            // Either undef'd or it happens to be an autoload.
            if (entry != null) {
                IRubyObject value = entry.value;
                if (value == UNDEF) return mod.getAutoloadMap().get(name);
                return entry;
            }

            if (!inherit) break;
        }

        return null;
    }

    private static IRubyObject iterateConstantNoConstMissing(ThreadContext context, String name,
        RubyModule init, boolean inherit, boolean loadConstant, boolean includeObject) {
        RubyClass objectClass = init.getRuntime().getObject();
        for (RubyModule mod = init; mod != null; mod = mod.getSuperClass()) {
            IRubyObject value = loadConstant ?
                    mod.getConstantWithAutoload(context, name, null, true) :
                    mod.fetchConstant(context, name, true);

            // if it's UNDEF and we're not loading and there's no autoload set up, consider it undefined
            if (value == UNDEF && !loadConstant && mod.getAutoloadMap().get(name) == null) return null;
            if (value != null) return value;

            if (!inherit) break;

            if (!includeObject && mod.getSuperClass() == objectClass) break;
        }
        return null;
    }

    @Deprecated(since = "10.0")
    public IRubyObject getConstantFrom(String name) {
        return getConstantFrom(getCurrentContext(), name);
    }

    // not actually called anywhere (all known uses call the fast version)
    public IRubyObject getConstantFrom(ThreadContext context, String name) {
        IRubyObject value = getConstantFromNoConstMissing(context, name);

        return value != null ? value : getConstantFromConstMissing(context, name);
    }

    @Deprecated(since = "10.0")
    public IRubyObject getConstantWithAutoload(String name, IRubyObject failedAutoloadValue, boolean includePrivate) {
        return getConstantWithAutoload(getCurrentContext(), name, failedAutoloadValue, includePrivate);
    }

    /**
     * Search just this class for a constant value, or trigger autoloading.
     *
     * @param name
     * @return
     */
    public IRubyObject getConstantWithAutoload(ThreadContext context, String name, IRubyObject failedAutoloadValue,
                                               boolean includePrivate) {
        RubyModule autoloadModule = null;
        IRubyObject result;

        while ((result = fetchConstant(context, name, includePrivate)) != null) { // loop for autoload
            if (result == RubyObject.UNDEF) {
                if (autoloadModule == this) return failedAutoloadValue;
                autoloadModule = this;

                final RubyModule.Autoload autoload = getAutoloadMap().get(name);

                if (autoload == null) return null;
                if (autoload.getValue() != null) return autoload.getValue();

                autoload.load(context);
                continue;
            }

            return result;
        }

        return autoloadModule != null ? failedAutoloadValue : null;
    }

    @Deprecated
    public IRubyObject fastGetConstantFrom(String internedName) {
        return getConstantFrom(getCurrentContext(), internedName);
    }

    @Deprecated(since = "10.0")
    public IRubyObject getConstantFromNoConstMissing(String name) {
        return getConstantFromNoConstMissing(getCurrentContext(), name);
    }

    public IRubyObject getConstantFromNoConstMissing(ThreadContext context, String name) {
        return getConstantFromNoConstMissing(context, name, true);
    }

    @Deprecated(since = "10.0")
    public IRubyObject getConstantFromNoConstMissing(String name, boolean includePrivate) {
        return getConstantFromNoConstMissing(getCurrentContext(), name, includePrivate);
    }

    public IRubyObject getConstantFromNoConstMissing(ThreadContext context, String name, boolean includePrivate) {
        final RubyClass Object = objectClass(context);

        RubyModule mod = this;

        while (mod != null) {
            IRubyObject result = mod.getConstantWithAutoload(context, name, null, includePrivate);

            if (result != null) {
                return mod == Object && this != Object ? null : result;
            }

            mod = mod.getSuperClass();
        }

        return null;
    }

    @Deprecated
    public IRubyObject fastGetConstantFromNoConstMissing(String internedName) {
        return getConstantFromNoConstMissing(getCurrentContext(), internedName);
    }

    @Deprecated(since = "10.0")
    public IRubyObject getConstantFromConstMissing(String name) {
        return getConstantFromConstMissing(getCurrentContext(), name);
    }

    public IRubyObject getConstantFromConstMissing(ThreadContext context, String name) {
        return callMethod(context, "const_missing", context.runtime.fastNewSymbol(name));
    }

    @Deprecated
    public IRubyObject fastGetConstantFromConstMissing(String internedName) {
        return getConstantFromConstMissing(getCurrentContext(), internedName);
    }

    @Deprecated(since = "10.0")
    public final IRubyObject resolveUndefConstant(String name) {
        return resolveUndefConstant(getCurrentContext(), name);
    }

    IRubyObject resolveUndefConstant(ThreadContext context, String name) {
        return getAutoloadConstant(context, name);
    }

    @Deprecated(since = "10.0")
    public IRubyObject setConstantQuiet(String name, IRubyObject value) {
        return setConstantQuiet(getCurrentContext(), name, value);
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
    public IRubyObject setConstantQuiet(ThreadContext context, String name, IRubyObject value) {
        return setConstantCommon(context, name, value, null, false, null, -1);
    }

    @Deprecated(since = "10.0")
    public IRubyObject setConstant(String name, IRubyObject value) {
        return setConstant(getCurrentContext(), name, value);
    }

    /**
     * Set the named constant on this module. Also, if the value provided is another Module and
     * that module has not yet been named, assign it the specified name.
     *
     * @param name The name to assign
     * @param value The value to assign to it; if an unnamed Module, also set its basename to name
     * @return The result of setting the variable.
     */
    public IRubyObject setConstant(ThreadContext context, String name, IRubyObject value) {
        return setConstantCommon(context, name, value, null, true, null, -1);
    }

    @Deprecated(since = "10.0")
    public IRubyObject setConstant(String name, IRubyObject value, String file, int line) {
        return setConstant(getCurrentContext(), name, value, file, line);
    }

    public IRubyObject setConstant(ThreadContext context, String name, IRubyObject value, String file, int line) {
        return setConstantCommon(context, name, value, null, true, file, line);
    }

    @Deprecated(since = "10.0")
    public IRubyObject setConstant(String name, IRubyObject value, boolean hidden) {
        return setConstant(getCurrentContext(), name, value, hidden);
    }

    public IRubyObject setConstant(ThreadContext context, String name, IRubyObject value, boolean hidden) {
        return setConstantCommon(context, name, value, hidden, true, null, -1);
    }

    /**
     * Set the named constant on this module. Also, if the value provided is another Module and
     * that module has not yet been named, assign it the specified name.
     *
     * @param name The name to assign
     * @param value The value to assign to it; if an unnamed Module, also set its basename to name
     * @param hiddenObj whether a constant is private (hidden).  If null, default to public (non-hidden), or leave
     *                  previous visibility in place.
     * @return The result of setting the variable.
     */
    private IRubyObject setConstantCommon(ThreadContext context, String name, IRubyObject value, Boolean hiddenObj,
                                          boolean warn, String file, int line) {
        ConstantEntry oldEntry = fetchConstantEntry(context, name, true);

        setParentForModule(context, name, value);

        if (oldEntry != null) {
            // preserve existing hidden value if none was given
            boolean hidden = oldEntry.hidden;
            if (hiddenObj != null) hidden = hiddenObj;
            boolean notAutoload = oldEntry.value != UNDEF;
            if (notAutoload) {
                if (warn) {
                    warn(context, "already initialized constant " +
                            (this.equals(objectClass(context)) ? name : (this + "::" + name)));
                }

                storeConstant(context, name, value, hidden, file, line);
            } else {
                boolean autoloading = setAutoloadConstant(context, name, value, hidden, file, line);
                if (autoloading) {
                    // invoke const_added for Autoload in progress
                    callMethod(context, "const_added", asSymbol(context, name));
                } else {
                    storeConstant(context, name, value, hidden, file, line);
                }
            }
        } else {
            if (this == context.runtime.getObject() && name.equals("Ruby")) Warn.warnReservedName(context, "::Ruby", "3.5");

            storeConstant(context, name, value, hiddenObj == null ? false : hiddenObj, file, line);
        }

        invalidateConstantCache(context, name);
        return value;
    }

    private void setParentForModule(ThreadContext context, final String name, final IRubyObject value) {
        // if adding a module under a constant name, set that module's basename to the constant name
        if ( value instanceof RubyModule module) {
            if (module != this && (module.getBaseName() == null || module.usingTemporaryName())) {
                module.baseName(name);
                module.setParent(this);
            }
            module.calculateName(context);
        }
    }

    @Deprecated(since = "9.4-")
    public IRubyObject fastSetConstant(String internedName, IRubyObject value) {
        return setConstant(getCurrentContext(), internedName, value);
    }

    /**
     * Define an alias on this module/class.
     *
     * @param context the current thread context
     * @param name the new alias name
     * @param oldName the existing method name
     * @return itself for composable API
     */
    // MRI: rb_alias
    public synchronized <T extends RubyModule> T defineAlias(ThreadContext context, String name, String oldName) {
        testFrozen("module");

        CacheEntry entry = searchForAliasMethod(context, oldName);

        DynamicMethod method = getMethods().get(name);
        if (method != null && entry.method.getRealMethod() != method.getRealMethod() && !method.isUndefined()) {
            if (method.getRealMethod().getAliasCount() == 0) warning(context, "method redefined; discarding old " + name);

            if (method instanceof PositionAware posAware) {
                context.runtime.getWarnings().warning(ID.REDEFINING_METHOD, posAware.getFile(), posAware.getLine() + 1, "previous definition of " + name + " was here");
            }
        }

        checkAliasFrameAccesses(context, oldName, name, entry.method);
        putAlias(context, name, entry, oldName);

        RubyModule methodLocation = getMethodLocation();
        synchronized (context.runtime.getHierarchyLock()) {
            methodLocation.invalidateCacheDescendants(context);
        }

        return (T) this;
    }



    /**
     * Define a constant when you are defining your Ruby class/module.
     *
     * @param context the current thread context
     * @param name the name of the constant
     * @param value the value for the constant
     * @return itself for a composable API
     */
    @JRubyAPI
    public <T extends RubyModule> T defineConstant(ThreadContext context, String name, IRubyObject value) {
        return (T) defineConstant(context, name, value, false);
    }

    /**
     * Define a constant when you are defining your Ruby class/module.
     *
     * @param context the current thread context
     * @param name the name of the constant
     * @param value the value for the constant
     * @param hidden should this be a hidden constant
     * @return itself for a composable API
     */
    @JRubyAPI
    public <T extends RubyModule> T defineConstant(ThreadContext context, String name, IRubyObject value, boolean hidden) {
        if (!IdUtil.isValidConstantName(name)) throw nameError(context, "bad constant name " + name, name);
        setConstantCommon(context, name, value, hidden, true, null, -1);
        return (T) this;
    }

    /** rb_define_const
     *
     */
    @Extension
    @Deprecated(since = "10.0")
    public void defineConstant(String name, IRubyObject value) {
        var context = getCurrentContext();
        assert value != null;

        if (!IdUtil.isValidConstantName(name)) throw nameError(context, "bad constant name " + name, name);

        setConstant(context, name, value);
    }

    @Deprecated(since = "10.0")
    public boolean isConstantDefined(String name, boolean inherit) {
        return constDefinedInner(getCurrentContext(), name, false, inherit, false);
    }

    @Deprecated(since = "10.0")
    public boolean constDefined(String name) {
        return constDefined(getCurrentContext(), name);
    }

    // rb_const_defined
    public boolean constDefined(ThreadContext context, String name) {
        return constDefinedInner(context, name, false, true, false);
    }

    @Deprecated(since = "10.0")
    public boolean constDefinedAt(String name) {
        return constDefinedAt(getCurrentContext(), name);
    }

    // rb_const_defined_at
    public boolean constDefinedAt(ThreadContext context, String name) {
        return constDefinedInner(context, name, true, false, false);
    }

    @Deprecated(since = "10.0")
    public boolean constDefinedFrom(String name) {
        return constDefinedFrom(getCurrentContext(), name);
    }

    // rb_const_defined_from
    public boolean constDefinedFrom(ThreadContext context, String name) {
        return constDefinedInner(context, name, true, true, false);
    }

    @Deprecated(since = "10.0")
    public boolean publicConstDefinedFrom(String name) {
        return publicConstDefinedFrom(getCurrentContext(), name);
    }

    // rb_public_const_defined_from
    public boolean publicConstDefinedFrom(ThreadContext context, String name) {
        return constDefinedInner(context, name, true, true, true);
    }

    // Fix for JRUBY-1339 - search hierarchy for constant
    /**
     * rb_const_defined_0
     */
    private boolean constDefinedInner(ThreadContext context, String name, boolean exclude, boolean recurse, boolean visibility) {
        RubyClass object = objectClass(context);
        boolean moduleRetry = false;

        RubyModule module = this;

        retry: while (true) {
            while (module != null) {
                ConstantEntry entry;
                if ((entry = module.constantEntryFetch(name)) != null) {
                    if (visibility && entry.hidden) return false;

                    IRubyObject value = entry.value;

                    // autoload is not in progress and should not appear defined
                    if (value == UNDEF && module.checkAutoloadRequired(context, name, null) == null &&
                            !module.autoloadingValue(context, name)) {
                        return false;
                    }

                    return !(exclude && module == object && this != object);
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

    @Deprecated(since = "10.0")
    public boolean autoloadingValue(Ruby runtime, String name) {
        return autoloadingValue(runtime.getCurrentContext(), name);
    }

    // MRI: rb_autoloading_value
    public boolean autoloadingValue(ThreadContext context, String name) {
        final Autoload autoload = getAutoloadMap().get(name);

        // Has autoload not been evacuated and is in progress on this thread and has updated the value?
        return autoload != null && autoload.isSelf(context) && autoload.getValue() != null;
    }

    // MRI: check_autoload_required
    private RubyString checkAutoloadRequired(ThreadContext context, String name, String[] autoloadPath) {
        final Autoload autoload = getAutoloadMap().get(name);
        if (autoload == null) return null;  // autoload has been evacuated

        RubyString file = autoload.getPath();
        if (file.length() == 0) throw argumentError(context, "empty file name");

        // autoload is in progress on anther thread
        if (autoload.ctx != null && !autoload.isSelf(context)) return file;

        String[] loading = {null};

        // feature has not been loaded yet
        if (!loadService(context).featureAlreadyLoaded(file.asJavaString(), loading)) return file;

        // feature is currently loading
        if (autoloadPath != null && loading[0] != null) {
            autoloadPath[0] = loading[0];
            return file;
        }

        return null; // autoload has yet to run
    }

    @Deprecated(since = "10.0")
    public boolean isConstantDefined(String name) {
        return isConstantDefined(getCurrentContext(), name);
    }

    public boolean isConstantDefined(ThreadContext context, String name) {
        return constDefinedInner(context, name, false, true, false);
    }

    //
    ////////////////// COMMON CONSTANT / CVAR METHODS ////////////////
    //

    private RaiseException cannotRemoveError(ThreadContext context, String id) {
        return nameError(context, str(context.runtime, "cannot remove ", ids(context.runtime, id), " for ", types(context.runtime, this)), id);
    }

    public static boolean testModuleMatch(ThreadContext context, IRubyObject arg0, int id) {
        return arg0 instanceof RubyModule && ((RubyModule) arg0).id == id;
    }


    //
    ////////////////// INTERNAL MODULE VARIABLE API METHODS ////////////////
    //

    /**
     * Behaves similarly to {@link #getClassVar(ThreadContext, String)}. Searches this
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
     * Behaves similarly to {@link #getClassVar(ThreadContext, String)}. Searches this
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
     * Behaves similarly to {@link #setClassVar(ThreadContext, String, IRubyObject)}. If the
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
        return getClassVariablesForWrite();
    }

    /**
     * Get the class variables for write. If it is not set or not of the right size,
     * prepare it atomically.
     *
     * @return the class vars map, ready for assignment
     */
    private Map<String,IRubyObject> getClassVariablesForWrite() {
        Map<String, IRubyObject> classVariables = this.classVariables;
        if ( classVariables == null ) {
            if (!CLASSVARS_HANDLE.compareAndSet(this, null, classVariables = new ConcurrentHashMap<String, IRubyObject>(4, 0.75f, 2))) {
                classVariables = this.classVariables;
            }
        }
        return classVariables;
    }

    protected Map<String, IRubyObject> getClassVariablesForRead() {
        Map<String, IRubyObject> classVariables = this.classVariables;
        return classVariables == null ? Collections.EMPTY_MAP : classVariables;
    }

    // runtime-free
    public boolean hasClassVariable(String name) {
        assert IdUtil.isClassVariable(name);
        return getClassVariablesForRead().containsKey(name);
    }

    @Deprecated
    public boolean fastHasClassVariable(String internedName) {
        return hasClassVariable(internedName);
    }

    // runtime-free
    public IRubyObject fetchClassVariable(String name) {
        assert IdUtil.isClassVariable(name);
        return getClassVariablesForRead().get(name);
    }

    @Deprecated
    public IRubyObject fastFetchClassVariable(String internedName) {
        return fetchClassVariable(internedName);
    }

    @Deprecated(since = "10.0")
    public IRubyObject storeClassVariable(String name, IRubyObject value) {
        return storeClassVariable(getCurrentContext(), name, value);
    }

    public IRubyObject storeClassVariable(ThreadContext context, String name, IRubyObject value) {
        assert IdUtil.isClassVariable(name) && value != null;
        checkAndRaiseIfFrozen(context);
        getClassVariables().put(name, value);
        return value;
    }

    @Deprecated
    public IRubyObject fastStoreClassVariable(String internedName, IRubyObject value) {
        return storeClassVariable(getCurrentContext(), internedName, value);
    }

    @Deprecated(since = "10.0")
    public IRubyObject deleteClassVariable(String name) {
        return deleteClassVariable(getCurrentContext(), name);
    }

    private IRubyObject deleteClassVariable(ThreadContext context, String name) {
        assert IdUtil.isClassVariable(name);
        checkAndRaiseIfFrozen(context);
        return getClassVariablesForRead().remove(name);
    }

    public List<String> getClassVariableNameList() {
        return new ArrayList<String>(getClassVariablesForRead().keySet());
    }

    @Deprecated(since = "10.0")
    protected final String validateClassVariable(String name) {
        return validateClassVariable(getCurrentContext(), name);
    }

    private String validateClassVariable(ThreadContext context, String name) {
        if (IdUtil.isValidClassVariableName(name)) return name;

        throw context.runtime.newNameError("'%1$s' is not allowed as a class variable name", this, name);
    }

    @Deprecated(since = "10.0")
    protected final String validateClassVariable(IRubyObject nameObj, String name) {
        if (IdUtil.isValidClassVariableName(name)) return name;

        throw getCurrentContext().runtime.newNameError("'%1$s' is not allowed as a class variable name", this, nameObj);
    }

    @Deprecated(since = "10.0")
    protected String validateClassVariable(Ruby runtime, IRubyObject object) {
        return validateClassVariable(runtime.getCurrentContext(), object);
    }

    protected String validateClassVariable(ThreadContext context, IRubyObject object) {
        RubySymbol name = checkID(context, object);

        if (!name.validClassVariableName()) {
            throw context.runtime.newNameError(str(context.runtime, "'", ids(context.runtime, name), "' is not allowed as a class variable name"), this, object);
        }

        return name.idString();
    }

    @Deprecated(since = "10.0")
    protected final void ensureClassVariablesSettable() {
        checkAndRaiseIfFrozen(getCurrentContext());
    }

    //
    ////////////////// LOW-LEVEL CONSTANT INTERFACE ////////////////
    //
    // fetch/store/list constants for this module
    //

    // runtime-free
    public boolean hasConstant(String name) {
        assert IdUtil.isConstant(name);
        return constantTableContains(name);
    }

    @Deprecated
    public boolean fastHasConstant(String internedName) {
        return hasConstant(internedName);
    }

    @Deprecated(since = "10.0")
    public IRubyObject fetchConstant(String name) {
        return fetchConstant(getCurrentContext(), name);
    }

    // returns the stored value without processing undefs (autoloads)
    public IRubyObject fetchConstant(ThreadContext context, String name) {
        return fetchConstant(context, name, true);
    }

    @Deprecated(since = "10.0")
    public IRubyObject fetchConstant(String name, boolean includePrivate) {
        return fetchConstant(getCurrentContext(), name, includePrivate);
    }

    public IRubyObject fetchConstant(ThreadContext context, String name, boolean includePrivate) {
        ConstantEntry entry = fetchConstantEntry(context, name, includePrivate);

        return entry != null ? entry.value : null;
    }

    @Deprecated(since = "10.0")
    public ConstantEntry fetchConstantEntry(String name, boolean includePrivate) {
        return fetchConstantEntry(getCurrentContext(), name, includePrivate);
    }

    /**
     * The equivalent for fetchConstant but is useful for extra state like whether the constant is
     * private or not.
     *
     * @param name of the constant.
     * @param includePrivate include private/hidden constants
     * @return the entry for the constant.
     */
    public ConstantEntry fetchConstantEntry(ThreadContext context, String name, boolean includePrivate) {
        ConstantEntry entry = constantEntryFetch(name);
        if (entry == null) return null;

        if (entry.hidden && !includePrivate) {
            context.setPrivateConstantReference(getOrigin());
            return null;
        }
        if (entry.deprecated) {
            String parent = "Object".equals(getName(context)) ? "" : getName(context);
            warnDeprecated(context, "constant " + parent + "::" + name + " is deprecated");
        }

        return entry;
    }

    @Deprecated
    public IRubyObject fastFetchConstant(String internedName) {
        return fetchConstant(getCurrentContext(), internedName);
    }

    @Deprecated(since = "10.0")
    public IRubyObject storeConstant(String name, IRubyObject value) {
        return storeConstant(getCurrentContext(), name, value);
    }

    public IRubyObject storeConstant(ThreadContext context, String name, IRubyObject value) {
        assert value != null : "value is null";

        checkAndRaiseIfFrozen(context);
        return constantTableStore(name, value);
    }

    @Deprecated(since = "10.0")
    public IRubyObject storeConstant(String name, IRubyObject value, boolean hidden, String file, int line) {
        return storeConstant(getCurrentContext(), name, value, hidden, file, line);
    }

    public IRubyObject storeConstant(ThreadContext context, String name, IRubyObject value, boolean hidden,
                                     String file, int line) {
        assert value != null : "value is null";

        checkAndRaiseIfFrozen(context);
        constantTableStore(name, value, hidden, false, file, line);

        if (file != null && file != BUILTIN_CONSTANT) callMethod(context, "const_added", asSymbol(context, name));

        return value;
    }

    @Deprecated(since = "10.0")
    public IRubyObject storeConstant(String name, IRubyObject value, boolean hidden) {
        assert value != null : "value is null";

        checkAndRaiseIfFrozen(getCurrentContext());
        return constantTableStore(name, value, hidden);
    }

    private IRubyObject storeConstant(ThreadContext context, String name, IRubyObject value, boolean hidden, boolean deprecated) {
        assert value != null : "value is null";

        checkAndRaiseIfFrozen(context);
        return constantTableStore(name, value, hidden, deprecated);
    }

    @Deprecated
    public IRubyObject fastStoreConstant(String internedName, IRubyObject value) {
        return storeConstant(getCurrentContext(), internedName, value);
    }

    @Deprecated(since = "10.0")
    public IRubyObject deleteConstant(String name) {
        return deleteConstant(getCurrentContext(), name);
    }

    // removes and returns the stored value without processing undefs (autoloads)
    public IRubyObject deleteConstant(ThreadContext context, String name) {
        assert name != null : "name is null";

        checkAndRaiseIfFrozen(context);
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

    @Deprecated(since = "10.0")
    protected final String validateConstant(IRubyObject name) {
        return validateConstant(getCurrentContext(), name);
    }

    /**
     * Validates name is a valid constant name and returns its id string.
     * @param name object to verify as a valid constant.
     * @return the id for this valid constant name.
     */
    protected final String validateConstant(ThreadContext context, IRubyObject name) {
        return RubySymbol.retrieveIDSymbol(name, (sym, newSym) -> {
            if (!sym.validConstantName()) {
                throw nameError(context, str(context.runtime, "wrong constant name ", sym), sym);
            }
        }).idString();
    }

    @Deprecated(since = "10.0")
    protected final String validateConstant(String name, IRubyObject errorName) {
        return validateConstant(getCurrentContext(), name, errorName);
    }

    // FIXME: This should really be working with symbol segments (errorName is FQN).
    private final String validateConstant(ThreadContext context, String name, IRubyObject errorName) {
        if (IdUtil.isValidConstantName(name)) return name;

        // MRI is more complicated than this and distinguishes between ID and non-ID.
        RubyString nameString = errorName.asString();

        return RubySymbol.retrieveIDSymbol(nameString, (sym, newSym) -> {
            if (!sym.validConstantName()) {
                throw nameError(context, str(context.runtime, "wrong constant name ", sym), sym);
            }
        }).idString();
    }

    @Deprecated(since = "10.0")
    protected final void ensureConstantsSettable() {
        checkAndRaiseIfFrozen(getCurrentContext());
    }

    private void checkAndRaiseIfFrozen(ThreadContext context) throws RaiseException {
        if (isFrozen()) {
            if (!(this instanceof RubyClass)) throw frozenError(context, this, "Module");
            if (getBaseName() != null) throw frozenError(context, this, "#<Class:" + getName(context) + '>');

            // MRI 2.2.2 does get ugly ... as it skips this logic :
            // RuntimeError: can't modify frozen #<Class:#<Class:0x0000000095a920>>
            throw frozenError(context, this, getName(context));
        }
    }

    @Override
    public final void checkFrozen() {
       if ( isFrozen() ) {
           throw getRuntime().newFrozenError(isClass() ? "class" : "module", this);
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

    // runtime-free
    protected ConstantEntry constantEntryFetch(String name) {
        return getConstantMap().get(name);
    }

    // runtime-free
    protected IRubyObject constantTableStore(String name, IRubyObject value) {
        Map<String, ConstantEntry> constMap = getConstantMapForWrite();
        boolean hidden = false;

        ConstantEntry entry = constMap.get(name);
        if (entry != null && entry.value != UNDEF) {
            hidden = entry.hidden;
        }

        constMap.put(name, new ConstantEntry(value, hidden));
        return value;
    }

    // runtime-free
    protected IRubyObject constantTableStore(String name, IRubyObject value, boolean hidden) {
        return constantTableStore(name, value, hidden, false);
    }

    /**
     * This is an internal API which is only used during runtime creation but BEFORE the first
     * ThreadContext is created.  Nothing other that a few constants in the Ruby constructor should
     * be calling this.  This method has no error checks.
     *
     * A secondary goal of this method is that it does not access Ruby or ThreadContext in any way.
     *
     * @param name of the constant
     * @param value of the constant
     */
    public void defineConstantBootstrap(String name, IRubyObject value) {
        constantTableStore(name, value, false, false, null, -1);
    }

    protected IRubyObject constantTableStore(String name, IRubyObject value, boolean hidden, boolean deprecated,
                                             String file, int line) {
        Map<String, ConstantEntry> constMap = getConstantMapForWrite();
        ConstantEntry entry = new ConstantEntry(value, hidden, deprecated);

        if (file != null) {
            entry.file = file;
            entry.line = line;
        }

        constMap.put(name, entry);
        return value;
    }

    // runtime-free
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

    @Deprecated(since = "10.0")
    protected final void defineAutoload(String symbol, RubyString path) {
        defineAutoload(getCurrentContext(), symbol, path);
    }

    /**
     * Define an autoload. ConstantMap holds UNDEF for the name as an autoload marker.
     */
    protected final void defineAutoload(ThreadContext context, String symbol, RubyString path) {
        final Autoload existingAutoload = getAutoloadMap().get(symbol);
        if (existingAutoload == null || existingAutoload.getValue() == null) {
            storeConstant(context, symbol, RubyObject.UNDEF, false, context.getFile(), context.getLine());
            RubyStackTraceElement caller = context.getSingleBacktrace();
            getAutoloadMapForWrite().put(symbol, new Autoload(symbol, path, caller.getFileName(), caller.getLineNumber()));
        }
    }

    @Deprecated(since = "10.0")
    protected final IRubyObject finishAutoload(String name) {
        return finishAutoload(getCurrentContext(), name);
    }

    /**
     * Extract an Object which is defined by autoload thread from autoloadMap and define it as a constant.
     */
    protected final IRubyObject finishAutoload(ThreadContext context, String name) {
        final Autoload autoload = getAutoloadMap().get(name);
        if ( autoload == null ) return null;
        final IRubyObject value = autoload.getValue();

        if (value != null && value != UNDEF) {
            storeConstant(context, name, value, autoload.hidden, autoload.getFile(), autoload.getLine());
            invalidateConstantCache(context, name);
        }

        removeAutoload(name);

        return value;
    }

    @Deprecated(since = "10.0")
    public final IRubyObject getAutoloadConstant(String name) {
        return getAutoloadConstant(getCurrentContext(), name);
    }

    /**
     * Get autoload constant.
     * If it's first resolution for the constant, it tries to require the defined feature and returns the defined value.
     * Multi-threaded accesses are blocked and processed sequentially except if the caller is the autoloading thread.
     */
    public final IRubyObject getAutoloadConstant(ThreadContext context, String name) {
        return getAutoloadConstant(context, name, true);
    }

    @Deprecated(since = "10.0")
    protected IRubyObject getAutoloadConstant(String name, boolean loadConstant) {
        return getAutoloadConstant(getCurrentContext(), name, loadConstant);
    }

    protected IRubyObject getAutoloadConstant(ThreadContext context, String name, boolean loadConstant) {
        final Autoload autoload = getAutoloadMap().get(name);
        if (autoload == null) return null;
        if (!loadConstant) return RubyObject.UNDEF;
        return autoload.load(context);
    }

    /**
     * Set an Object as a defined constant in autoloading.
     */
    private boolean setAutoloadConstant(ThreadContext context, String name, IRubyObject value, boolean hidden, String file, int line) {
        final Autoload autoload = getAutoloadMap().get(name);
        if (autoload == null) return false;

        boolean set = autoload.setConstant(context, value, hidden, file, line);
        if (!set) removeAutoload(name);
        return set;
    }

    /**
     * Removes an Autoload object from autoloadMap. ConstantMap must be updated before calling this.
     */
    // runtime-free
    private void removeAutoload(String name) {
        getAutoloadMapForWrite().remove(name);
    }

    protected RubyString getAutoloadFile(String name) {
        final Autoload autoload = getAutoloadMap().get(name);
        return autoload == null ? null : autoload.getPath();
    }

    private static void define(ThreadContext context, RubyModule module, JavaMethodDescriptor desc, DynamicMethod dynamicMethod) {
        JRubyMethod jrubyMethod = desc.anno;
        final String[] names = jrubyMethod.name();
        final String[] aliases = jrubyMethod.alias();

        RubyModule singletonClass;

        if (jrubyMethod.meta()) {
            singletonClass = module.singletonClass(context);
            dynamicMethod.setImplementationClass(singletonClass);

            final String baseName;
            if (names.length == 0) {
                baseName = desc.name;
                singletonClass.addMethod(context, baseName, dynamicMethod);
            } else {
                baseName = names[0];
                for (String name : names) singletonClass.addMethod(context, name, dynamicMethod);
            }

            if (aliases.length > 0) {
                for (String alias : aliases) singletonClass.defineAlias(context, alias, baseName);
            }
        } else {
            String baseName;
            if (names.length == 0) {
                baseName = desc.name;
                module.getMethodLocation().addMethod(context, baseName, dynamicMethod);
            } else {
                baseName = names[0];
                for (String name : names) module.getMethodLocation().addMethod(context, name, dynamicMethod);
            }

            if (aliases.length > 0) {
                for (String alias : aliases) module.defineAlias(context, alias, baseName);
            }

            if (jrubyMethod.module()) {
                singletonClass = module.singletonClass(context);
                // module/singleton methods are all defined public
                DynamicMethod moduleMethod = dynamicMethod.dup();
                moduleMethod.setImplementationClass(singletonClass);
                moduleMethod.setVisibility(PUBLIC);

                if (names.length == 0) {
                    baseName = desc.name;
                    singletonClass.addMethod(context, desc.name, moduleMethod);
                } else {
                    baseName = names[0];
                    for (String name : names) singletonClass.addMethod(context, name, moduleMethod);
                }

                if (aliases.length > 0) {
                    for (String alias : aliases) singletonClass.defineAlias(context, alias, baseName);
                }
            }
        }
    }

    @Deprecated
    public IRubyObject initialize(Block block) {
        return initialize(getCurrentContext());
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
    private transient IRubyObject cachedSymbolName;

    @SuppressWarnings("unchecked")
    private volatile Map<String, ConstantEntry> constants;
    private static final VarHandle CONSTANTS_HANDLE;

    public interface SourceLocation {
        String getFile();
        int getLine();
    }

    /**
     * Represents a constant value, possibly hidden (private).
     */
    public static class ConstantEntry implements SourceLocation {
        public final IRubyObject value;
        public final boolean hidden;
        final boolean deprecated;
        private String file;
        private int line;

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

        ConstantEntry(IRubyObject value, boolean hidden, boolean deprecated, String file, int line) {
            this.value = value;
            this.hidden = hidden;
            this.deprecated = deprecated;
            this.file = file;
            this.line = line;
        }

        public String getFile() {
            return file;
        }

        public int getLine() {
            return line;
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
    public final class Autoload implements SourceLocation {
        // A ThreadContext which is executing autoload.
        private volatile ThreadContext ctx;
        // An object defined for the constant while autoloading.
        private volatile IRubyObject value;
        // Whether the autoload constant was set private
        private boolean hidden;
        // The symbol ID for the autoload constant
        private final String symbol;
        // The Ruby string representing the path to load
        private final RubyString path;
        public String file;
        public int line;

        Autoload(String symbol, RubyString path, String file, int line) {
            this.symbol = symbol;
            this.path = path;
            this.file = file;
            this.line = line;
        }

        public String getFile() {
            return file;
        }

        public int getLine() {
            return line;
        }

        public boolean isHidden() {
            return hidden;
        }

        // Returns an object for the constant if the caller is the autoloading thread.
        // Otherwise, try to start autoloading and returns the defined object by autoload.
        synchronized IRubyObject load(ThreadContext context) {
            if (this.ctx == null) {
                this.ctx = context;
            } else if (isSelf(context)) {
                return getValue();
            }

            try {
                // This method needs to be synchronized for removing Autoload
                // from autoloadMap when it's loaded.
                LoadService loadService = loadService(context);
                if (!loadService.featureAlreadyLoaded(path.asJavaString())) {
                    if (loadService.autoloadRequire(path)) {
                        // Do not finish autoloading by cyclic autoload
                        finishAutoload(context, symbol);
                    }
                }
            } catch (LoadError | RuntimeError lre) {
                // reset ctx to null for a future attempt to load
                this.ctx = null;
                throw lre;
            }

            return getValue();
        }

        // Update an object for the constant if the caller is the autoloading thread.
        synchronized boolean setConstant(ThreadContext ctx, IRubyObject newValue, boolean hidden, String file, int line) {
            boolean isSelf = isSelf(ctx);

            if (isSelf) {
                // only update value to valid results
                if (newValue != null && newValue != UNDEF) {
                    value = newValue;
                }
                this.hidden = hidden;
                // Note: we replace undef location with constant location as the autoload resolution is in flight.
                this.file = file;
                this.line = line;
            }

            return isSelf;
        }

        // Returns an object for the constant defined by autoload.
        synchronized IRubyObject getValue() {
            return value;
        }

        // Returns the assigned feature.
        RubyString getPath() {
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
            Class<?> javaClass = JavaUtil.getJavaClass(this, null);
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

    public boolean usingTemporaryName() {
        return getFlag(TEMPORARY_NAME);
    }

    public boolean isIncludedIntoRefinement() {
        return getFlag(INCLUDED_INTO_REFINEMENT);
    }

    /**
     * Return true if the given method is defined on this class and is a builtin,
     * i.e. a method built-in to JRuby and loaded during its core boot process.
     *
     * @param methodName
     * @return
     */
    public boolean isMethodBuiltin(String methodName) {
        DynamicMethod method = searchMethod(methodName);

        return method != null && method.isBuiltin();
    }

    public Map<RubyModule, RubyModule> getRefinements() {
        RefinementStore refinementStore = this.refinementStore;
        if (refinementStore == null) {
            return Collections.EMPTY_MAP;
        }
        return refinementStore.refinements;
    }

    public Map<RubyModule, RubyModule> getRefinementsForWrite() {
        RefinementStore refinementStore = this.getRefinementStoreForWrite();
        return refinementStore.refinements;
    }

    public void setRefinements(Map<RubyModule, RubyModule> refinements) {
        this.getRefinementStoreForWrite().refinements = refinements;
    }

    public static void finishRefinementClass(ThreadContext context, RubyClass Refinement) {
        Refinement.reifiedClass(RubyModule.class).
                classIndex(ClassIndex.REFINEMENT).
                defineMethods(context, RefinementMethods.class).
                undefMethods(context, "append_features", "prepend_features", "extend_object");
    }

    public static class RefinementMethods {
        @JRubyMethod(required = 1, rest = true, checkArity = false, visibility = PRIVATE)
        public static IRubyObject import_methods(ThreadContext context, IRubyObject self, IRubyObject[] modules) {
            Arity.checkArgumentCount(context, modules, 1, -1);

            RubyModule selfModule = (RubyModule) self;

            for (IRubyObject _module : modules) {
                RubyModule module = castAsModule(context, _module);

                if (module.getSuperClass() != null) {
                    warn(context, module.getName(context) + " has ancestors, but Refinement#import_methods doesn't import their methods");
                }
            }

            for (IRubyObject _module : modules) {
                RubyModule module = (RubyModule) _module;

                for (Map.Entry<String, DynamicMethod> entry: module.getMethods().entrySet()) {
                    refinementImportMethodsIter(context, selfModule, module, entry);
                }
            }

            return self;
        }

        // MRI: refinement_import_methods_i
        private static void refinementImportMethodsIter(ThreadContext context, RubyModule selfModule, RubyModule module,
                                                        Map.Entry<String, DynamicMethod> entry) {
            DynamicMethod method = entry.getValue();

            if (!(method instanceof AbstractIRMethod)) {
                throw argumentError(context, "Can't import method which is not defined with Ruby code: " + module.getName(context) + "#" + entry.getKey());
            }

            DynamicMethod dup = entry.getValue().dup();

            // maybe insufficient if we have already compiled assuming no refinements
            ((AbstractIRMethod) dup).getIRScope().setIsMaybeUsingRefinements();

            selfModule.addMethod(context, entry.getKey(), dup);
        }
    }

    private volatile Map<String, Autoload> autoloads;
    private static final VarHandle AUTOLOADS_HANDLE;
    protected volatile Map<String, DynamicMethod> methods;
    private static final VarHandle METHODS_HANDLE;
    protected Map<String, CacheEntry> cachedMethods;
    protected int generation;
    protected Integer generationObject;

    protected volatile RubyClass.RubyClassSet includingHierarchies;
    private static final VarHandle INCLUDING_HIERARCHIES_HANDLE;

    /**
     * Where are the methods of this module/class located?
     *
     * This only happens as a result of prepend (see PrependedModule) where it
     * moves all methods to a PrependedModule which will be beneath the actual
     * module which was prepended.
     */
    protected volatile RubyModule methodLocation;

    // ClassProviders return Java class/module (in #defineOrGetClassUnder and
    // #defineOrGetModuleUnder) when class/module is opened using colon syntax.
    private transient volatile Set<ClassProvider> classProviders;
    private static final VarHandle CLASS_PROVIDERS_HANDLE;

    static {
        try {
            CONSTANTS_HANDLE = MethodHandles.lookup().findVarHandle(RubyModule.class, "constants", Map.class);
            AUTOLOADS_HANDLE = MethodHandles.lookup().findVarHandle(RubyModule.class, "autoloads", Map.class);
            METHODS_HANDLE = MethodHandles.lookup().findVarHandle(RubyModule.class, "methods", Map.class);
            INCLUDING_HIERARCHIES_HANDLE = MethodHandles.lookup().findVarHandle(RubyModule.class, "includingHierarchies", RubyClass.RubyClassSet.class);
            CLASS_PROVIDERS_HANDLE = MethodHandles.lookup().findVarHandle(RubyModule.class, "classProviders", Set.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

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
        return isConstantDefined(getCurrentContext(), internedName);
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

    @Deprecated
    public synchronized void defineAliases(List<String> aliases, String oldId) {
        var context = getCurrentContext();
        testFrozen("module");

        CacheEntry entry = deepMethodSearch(context, oldId);
        DynamicMethod method = entry.method;

        for (String name: aliases) {
            checkAliasFrameAccesses(context, oldId, name, method);
            putAlias(context, name, entry, oldId);
        }

        RubyModule methodLocation = getMethodLocation();
        methodLocation.invalidateCacheDescendants(context);
    }

    protected ClassIndex classIndex = ClassIndex.NO_INDEX;

    private volatile Map<String, IRubyObject> classVariables;
    private static final VarHandle CLASSVARS_HANDLE;

    volatile RefinementStore refinementStore;
    private static final VarHandle REFINEMENT_STORE_HANDLE;

    static {
        try {
            CLASSVARS_HANDLE = MethodHandles.lookup().findVarHandle(RubyModule.class, "classVariables", Map.class);
            REFINEMENT_STORE_HANDLE = MethodHandles.lookup().findVarHandle(RubyModule.class, "refinementStore", RefinementStore.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private class RefinementStore {
        /**
         * Refinements added to this module are stored here
         **/
        private volatile Map<RubyModule, RubyModule> refinements = newRefinementsMap();

        /**
         * A list of refinement hosts for this refinement
         */
        private final Map<RubyModule, IncludedModule> activatedRefinements = newActivatedRefinementsMap();

        /**
         * The class this refinement refines
         */
        volatile RubyModule refinedClass;

        /**
         * The module where this refinement was defined
         */
        private volatile RubyModule definedAt;
    }

    // Invalidator used for method caches
    protected final Invalidator methodInvalidator;

    // track last size to avoid thrashing
    private int lastInvalidatorSize = 4;

    /** Whether this class proxies a normal Java class */
    private boolean javaProxy = false;

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    /**
     * A handle for invoking the module ID test, to be reused for all idTest handles below.
     */
    private static final MethodHandle testModuleMatch = Binder
            .from(boolean.class, ThreadContext.class, IRubyObject.class, int.class)
            .invokeStaticQuiet(LOOKUP, RubyModule.class, "testModuleMatch");

    public static JavaSites.ModuleSites sites(ThreadContext context) {
        return context.sites.Module;
    }

    @Deprecated
    public IRubyObject const_get(IRubyObject symbol) {
        return const_get(getCurrentContext(), symbol);
    }

    @Deprecated
    public IRubyObject const_get(ThreadContext context, IRubyObject... args) {
        int argc = Arity.checkArgumentCount(context, args, 1, 2);

        boolean inherit = argc == 1 || ( ! args[1].isNil() && args[1].isTrue() );

        final IRubyObject symbol = args[0];
        return constGetCommon(context, symbol, inherit);
    }

    @Deprecated
    public IRubyObject const_get_1_9(ThreadContext context, IRubyObject[] args) {
        return const_get(context, args);
    }

    @Deprecated
    public IRubyObject const_get_2_0(ThreadContext context, IRubyObject[] args) {
        return const_get(context, args);
    }
}
