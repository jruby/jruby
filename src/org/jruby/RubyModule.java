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
 * Copyright (C) 2001 Chad Fowler <chadfowler@chadfowler.com>
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004-2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006-2007 Miguel Covarrubias <mlcovarrubias@gmail.com>
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jruby.anno.JRubyMethod;
import org.jruby.internal.runtime.methods.AliasMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.FullFunctionCallbackMethod;
import org.jruby.internal.runtime.methods.SimpleCallbackMethod;
import org.jruby.internal.runtime.methods.MethodMethod;
import org.jruby.internal.runtime.methods.ProcMethod;
import org.jruby.internal.runtime.methods.UndefinedMethod;
import org.jruby.internal.runtime.methods.WrapperMethod;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.Dispatcher;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callback.Callback;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.ClassProvider;
import org.jruby.util.IdUtil;
import org.jruby.util.MethodCache;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.MethodIndex;

/**
 *
 * @author  jpetersen
 */
public class RubyModule extends RubyObject {
    
    public static RubyClass createModuleClass(Ruby runtime, RubyClass moduleClass) {
        moduleClass.index = ClassIndex.MODULE;
        moduleClass.kindOf = new RubyModule.KindOf() {
            public boolean isKindOf(IRubyObject obj, RubyModule type) {
                return obj instanceof RubyModule;
            }
        };
        
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyModule.class);            
            
        moduleClass.defineFastMethod("===", callbackFactory.getFastMethod("op_eqq", IRubyObject.class));
        moduleClass.defineFastMethod("==", callbackFactory.getFastMethod("op_equal", IRubyObject.class));
        moduleClass.defineFastMethod("freeze", callbackFactory.getFastMethod("freeze"));        
        moduleClass.defineFastMethod("<=>", callbackFactory.getFastMethod("op_cmp", IRubyObject.class));
        moduleClass.defineFastMethod("<", callbackFactory.getFastMethod("op_lt", IRubyObject.class));
        moduleClass.defineFastMethod("<=", callbackFactory.getFastMethod("op_le", IRubyObject.class));
        moduleClass.defineFastMethod(">", callbackFactory.getFastMethod("op_gt", IRubyObject.class));
        moduleClass.defineFastMethod(">=", callbackFactory.getFastMethod("op_ge", IRubyObject.class));
        moduleClass.defineFastMethod("ancestors", callbackFactory.getFastMethod("ancestors"));
        moduleClass.defineFastMethod("class_variables", callbackFactory.getFastMethod("class_variables"));
        moduleClass.defineFastMethod("const_defined?", callbackFactory.getFastMethod("const_defined", IRubyObject.class));
        moduleClass.defineFastMethod("const_get", callbackFactory.getFastMethod("const_get", IRubyObject.class));
        moduleClass.defineMethod("const_missing", callbackFactory.getMethod("const_missing", IRubyObject.class));
        moduleClass.defineFastMethod("const_set", callbackFactory.getFastMethod("const_set", IRubyObject.class, IRubyObject.class));
        moduleClass.defineFastMethod("constants", callbackFactory.getFastMethod("constants"));
        moduleClass.defineMethod("extended", callbackFactory.getMethod("extended", IRubyObject.class));
        moduleClass.defineFastMethod("include?", callbackFactory.getFastMethod("include_p", IRubyObject.class));
        moduleClass.defineFastMethod("included", callbackFactory.getFastMethod("included", IRubyObject.class));
        moduleClass.defineFastMethod("included_modules", callbackFactory.getFastMethod("included_modules"));
        moduleClass.defineMethod("initialize", callbackFactory.getMethod("initialize"));
        moduleClass.defineFastMethod("initialize_copy", callbackFactory.getFastMethod("initialize_copy", IRubyObject.class));
        moduleClass.defineFastMethod("instance_method", callbackFactory.getFastMethod("instance_method", IRubyObject.class));
        moduleClass.defineFastMethod("instance_methods",callbackFactory.getFastOptMethod("instance_methods"));
        moduleClass.defineFastMethod("method_defined?", callbackFactory.getFastMethod("method_defined", IRubyObject.class));
        moduleClass.defineFastMethod("public_method_defined?", callbackFactory.getFastMethod("public_method_defined", IRubyObject.class));
        moduleClass.defineFastMethod("protected_method_defined?", callbackFactory.getFastMethod("protected_method_defined", IRubyObject.class));
        moduleClass.defineFastMethod("private_method_defined?", callbackFactory.getFastMethod("private_method_defined", IRubyObject.class));
        moduleClass.defineMethod("module_eval", callbackFactory.getOptMethod("module_eval"));
        moduleClass.defineFastMethod("name", callbackFactory.getFastMethod("name"));
        moduleClass.defineFastMethod("private_class_method", callbackFactory.getFastOptMethod("private_class_method"));
        moduleClass.defineFastMethod("private_instance_methods", callbackFactory.getFastOptMethod("private_instance_methods"));
        moduleClass.defineFastMethod("protected_instance_methods", callbackFactory.getFastOptMethod("protected_instance_methods"));
        moduleClass.defineFastMethod("public_class_method", callbackFactory.getFastOptMethod("public_class_method"));
        moduleClass.defineFastMethod("public_instance_methods", callbackFactory.getFastOptMethod("public_instance_methods"));
        moduleClass.defineFastMethod("to_s", callbackFactory.getFastMethod("to_s"));
        moduleClass.defineFastMethod("class_variable_defined?", callbackFactory.getFastMethod("class_variable_defined_p", IRubyObject.class));
        
        moduleClass.defineAlias("class_eval", "module_eval");
        
        moduleClass.defineFastPrivateMethod("alias_method", callbackFactory.getFastMethod("alias_method", IRubyObject.class, IRubyObject.class));
        moduleClass.defineFastPrivateMethod("append_features", callbackFactory.getFastMethod("append_features", IRubyObject.class));
        moduleClass.defineFastPrivateMethod("attr", callbackFactory.getFastOptMethod("attr"));
        moduleClass.defineFastPrivateMethod("attr_reader", callbackFactory.getFastOptMethod("attr_reader"));
        moduleClass.defineFastPrivateMethod("attr_writer", callbackFactory.getFastOptMethod("attr_writer"));
        moduleClass.defineFastPrivateMethod("attr_accessor", callbackFactory.getFastOptMethod("attr_accessor"));
        moduleClass.defineFastPrivateMethod("class_variable_get", callbackFactory.getFastMethod("class_variable_get", IRubyObject.class));
        moduleClass.defineFastPrivateMethod("class_variable_set", callbackFactory.getFastMethod("class_variable_set", IRubyObject.class, IRubyObject.class));
        moduleClass.definePrivateMethod("define_method", callbackFactory.getOptMethod("define_method"));
        moduleClass.defineFastPrivateMethod("extend_object", callbackFactory.getFastMethod("extend_object", IRubyObject.class));
        moduleClass.defineFastPrivateMethod("include", callbackFactory.getFastOptMethod("include"));
        moduleClass.definePrivateMethod("method_added", callbackFactory.getMethod("method_added", IRubyObject.class));
        moduleClass.definePrivateMethod("method_removed", callbackFactory.getMethod("method_removed", IRubyObject.class));
        moduleClass.definePrivateMethod("method_undefined", callbackFactory.getMethod("method_undefined", IRubyObject.class));
        moduleClass.defineFastPrivateMethod("module_function", callbackFactory.getFastOptMethod("module_function"));
        moduleClass.defineFastPrivateMethod("public", callbackFactory.getFastOptMethod("rbPublic"));
        moduleClass.defineFastPrivateMethod("protected", callbackFactory.getFastOptMethod("rbProtected"));
        moduleClass.defineFastPrivateMethod("private", callbackFactory.getFastOptMethod("rbPrivate"));
        moduleClass.defineFastPrivateMethod("remove_class_variable", callbackFactory.getFastMethod("remove_class_variable", IRubyObject.class));
        moduleClass.defineFastPrivateMethod("remove_const", callbackFactory.getFastMethod("remove_const", IRubyObject.class));
        moduleClass.defineFastPrivateMethod("remove_method", callbackFactory.getFastOptMethod("remove_method"));
        moduleClass.defineFastPrivateMethod("undef_method", callbackFactory.getFastOptMethod("undef_method"));
        
        RubyClass moduleMetaClass = moduleClass.getMetaClass();
        moduleMetaClass.defineMethod("nesting", callbackFactory.getSingletonMethod("nesting"));
        
        moduleClass.dispatcher = callbackFactory.createDispatcher(moduleClass);

        callbackFactory = runtime.callbackFactory(RubyKernel.class);
        moduleClass.defineFastMethod("autoload", callbackFactory.getFastSingletonMethod("autoload", RubyKernel.IRUBY_OBJECT, RubyKernel.IRUBY_OBJECT));
        moduleClass.defineFastMethod("autoload?", callbackFactory.getFastSingletonMethod("autoload_p", RubyKernel.IRUBY_OBJECT));

        return moduleClass;
    }    
    
    static ObjectAllocator MODULE_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyModule(runtime, klass);
        }
    };
    
    public int getNativeTypeIndex() {
        return ClassIndex.MODULE;
    }

    public boolean isModule() {
        return true;
    }

    public boolean isClass() {
        return false;
    }

    public boolean isSingleton() {
        return false;
    }    
    
    private static final String CVAR_TAINT_ERROR = "Insecure: can't modify class variable";
    private static final String CVAR_FREEZE_ERROR = "class/module";

    // superClass may be null.
    protected RubyClass superClass;

    public int index;
    
    public Dispatcher dispatcher = Dispatcher.DEFAULT_DISPATCHER;

    public static class KindOf {
        public static final KindOf DEFAULT_KIND_OF = new KindOf();
        public boolean isKindOf(IRubyObject obj, RubyModule type) {
            return obj.getMetaClass().hasModuleInHierarchy(type);
        }
    }

    public KindOf kindOf = KindOf.DEFAULT_KIND_OF;

    public final int id;

    // Containing class...The parent of Object is null. Object should always be last in chain.
    public RubyModule parent;

    // ClassId is the name of the class/module sans where it is located.
    // If it is null, then it an anonymous class.
    private String classId;

    // All methods and all CACHED methods for the module.  The cached methods will be removed
    // when appropriate (e.g. when method is removed by source class or a new method is added
    // with same name by one of its subclasses).
    private Map methods = new HashMap();
    
    
    // FIXME: I'm not sure what the serialization/marshalling implications
    // might be of defining this here. We could keep a hash in JavaSupport
    // (or elsewhere) instead, but then RubyModule might need a reference to 
    // JavaSupport code, which I've tried to avoid...
    private transient List classProviders;

    /** separate path for MetaClass construction
     * 
     */
    protected RubyModule(Ruby runtime, RubyClass metaClass, boolean objectSpace) {
        super(runtime, metaClass, objectSpace);
        id = ++runtime.moduleLastId;
        // if (parent == null) parent = runtime.getObject();
        setFlag(USER7_F, !isClass());
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
    public synchronized void addClassProvider(ClassProvider provider) {
        if (classProviders == null) {
            List cp = Collections.synchronizedList(new ArrayList());
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

    private RubyClass searchProvidersForClass(String name, RubyClass superClazz) {
        if (classProviders != null) {
            synchronized(classProviders) {
                RubyClass clazz;
                for (Iterator iter = classProviders.iterator(); iter.hasNext(); ) {
                    if ((clazz = ((ClassProvider)iter.next())
                            .defineClassUnder(this, name, superClazz)) != null) {
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
                for (Iterator iter = classProviders.iterator(); iter.hasNext(); ) {
                    if ((module = ((ClassProvider)iter.next()).defineModuleUnder(this, name)) != null) {
                        return module;
                    }
                }
            }
        }
        return null;
    }

    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    /** Getter for property superClass.
     * @return Value of property superClass.
     */
    public RubyClass getSuperClass() {
        return superClass;
    }

    protected void setSuperClass(RubyClass superClass) {
        this.superClass = superClass;
    }

    public RubyModule getParent() {
        return parent;
    }

    public void setParent(RubyModule parent) {
        this.parent = parent;
    }

    public Map getMethods() {
        return methods;
    }
    
    public void putMethod(Object name, DynamicMethod method) {
        // FIXME: kinda hacky...flush STI here
        dispatcher.clearIndex(MethodIndex.getIndex((String)name));
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

    /**
     * Generate a fully-qualified class name or a #-style name for anonymous and singleton classes.
     * 
     * Ruby C equivalent = "classname"
     * 
     * @return The generated class name
     */
    public String getName() {
        if (getBaseName() == null) {
            if (isClass()) {
                return "#<" + "Class" + ":01x" + Integer.toHexString(System.identityHashCode(this)) + ">";
            } else {
                return "#<" + "Module" + ":01x" + Integer.toHexString(System.identityHashCode(this)) + ">";
            }
        }

        StringBuffer result = new StringBuffer(getBaseName());
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
            result.insert(0, "::").insert(0, pName);
        }

        return result.toString();
    }

    /**
     * Create a wrapper to use for including the specified module into this one.
     * 
     * Ruby C equivalent = "include_class_new"
     * 
     * @return The module wrapper
     */
    public IncludedModuleWrapper newIncludeClass(RubyClass superClazz) {
        IncludedModuleWrapper includedModule = new IncludedModuleWrapper(getRuntime(), superClazz, this);

        // include its parent (and in turn that module's parents)
        if (getSuperClass() != null) {
            includedModule.includeModule(getSuperClass());
        }

        return includedModule;
    }

    /**
     * Search this and parent modules for the named variable.
     * 
     * @param name The variable to search for
     * @return The module in which that variable is found, or null if not found
     */
    private RubyModule getModuleWithInstanceVar(String name) {
        for (RubyModule p = this; p != null; p = p.getSuperClass()) {
            if (p.getInstanceVariable(name) != null) {
                return p;
            }
        }
        return null;
    }

    /**
     * Set the named class variable to the given value, provided taint and freeze allow setting it.
     * 
     * Ruby C equivalent = "rb_cvar_set"
     * 
     * @param name The variable name to set
     * @param value The value to set it to
     */
    public IRubyObject setClassVar(String name, IRubyObject value) {
        RubyModule module = getModuleWithInstanceVar(name);

        if (module == null) {
            module = this;
        }

        return module.setInstanceVariable(name, value, CVAR_TAINT_ERROR, CVAR_FREEZE_ERROR);
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
        RubyModule module = getModuleWithInstanceVar(name);

        if (module != null) {
            IRubyObject variable = module.getInstanceVariable(name);

            return variable == null ? getRuntime().getNil() : variable;
        }

        throw getRuntime().newNameError("uninitialized class variable " + name + " in " + getName(), name);
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
        return getModuleWithInstanceVar(name) != null;
    }

    /**
     * Set the named constant on this module. Also, if the value provided is another Module and
     * that module has not yet been named, assign it the specified name.
     * 
     * @param name The name to assign
     * @param value The value to assign to it; if an unnamed Module, also set its basename to name
     * @return The result of setting the variable.
     * @see RubyObject#setInstanceVariable(String, IRubyObject, String, String)
     */
    public IRubyObject setConstant(String name, IRubyObject value) {
        IRubyObject oldValue = getInstanceVariable(name);
        
        if (oldValue == getRuntime().getUndef()) {
            getRuntime().getLoadService().removeAutoLoadFor(getName() + "::" + name);
        } else if (oldValue != null) {
            getRuntime().getWarnings().warn("already initialized constant " + name);
        }

        IRubyObject result = setInstanceVariable(name, value, "Insecure: can't set constant",
                "class/module");

        // if adding a module under a constant name, set that module's basename to the constant name
        if (value instanceof RubyModule) {
            RubyModule module = (RubyModule)value;
            if (module.getBaseName() == null) {
                module.setBaseName(name);
                module.setParent(this);
            }
            /*
            module.setParent(this);
            */
        }
        return result;
    }

    /**
     * Finds a class that is within the current module (or class).
     * 
     * @param name to be found in this module (or class)
     * @return the class or null if no such class
     */
    public RubyClass getClass(String name) {
        IRubyObject module = getConstantAt(name);

        return  (module instanceof RubyClass) ? (RubyClass) module : null;
    }

    /**
     * Base implementation of Module#const_missing, throws NameError for specific missing constant.
     * 
     * @param name The constant name which was found to be missing
     * @return Nothing! Absolutely nothing! (though subclasses might choose to return something)
     */
    public IRubyObject const_missing(IRubyObject name, Block block) {
        /* Uninitialized constant */
        if (this != getRuntime().getObject()) {
            throw getRuntime().newNameError("uninitialized constant " + getName() + "::" + name.asSymbol(), "" + getName() + "::" + name.asSymbol());
        }

        throw getRuntime().newNameError("uninitialized constant " + name.asSymbol(), name.asSymbol());
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
        if (isSame(module)) {
            return;
        }

        infectBy(module);

        doIncludeModule(module);
    }

    public void defineMethod(String name, Callback method) {
        Visibility visibility = name.equals("initialize") ?
                Visibility.PRIVATE : Visibility.PUBLIC;
        addMethod(name, new FullFunctionCallbackMethod(this, method, visibility));
    }
    
    public void defineAnnotatedMethods(Class clazz, CallbackFactory callbackFactory) {
        Method[] declaredMethods = clazz.getDeclaredMethods();
        for (Method method: declaredMethods) {
            JRubyMethod jrubyMethod = method.getAnnotation(JRubyMethod.class);
            
            if (jrubyMethod == null) continue;
            
            // select current module or module's metaclass for singleton methods
            RubyModule module = this;
            if (jrubyMethod.singleton()) module = getMetaClass();

            Callback callback = callbackFactory.getMethod(method);
            DynamicMethod dynamicMethod;
            
            if (jrubyMethod.frame() || jrubyMethod.scope()) {
                dynamicMethod = new FullFunctionCallbackMethod(this, callback, jrubyMethod.visibility());
            } else {
                dynamicMethod = new SimpleCallbackMethod(this, callback, jrubyMethod.visibility());
            }
            module.addMethod(jrubyMethod.name(), dynamicMethod);
            if (!jrubyMethod.name2().equals("")) {
                module.addMethod(jrubyMethod.name2(), dynamicMethod);
            }
            
            if (!jrubyMethod.alias().equals("")) {
                module.defineAlias(jrubyMethod.alias(), jrubyMethod.name());
            }
        }
    }

    public void defineFastMethod(String name, Callback method) {
        Visibility visibility = name.equals("initialize") ?
                Visibility.PRIVATE : Visibility.PUBLIC;
        addMethod(name, new SimpleCallbackMethod(this, method, visibility));
    }

    public void defineFastMethod(String name, Callback method, Visibility visibility) {
        addMethod(name, new SimpleCallbackMethod(this, method, visibility));
    }

    public void definePrivateMethod(String name, Callback method) {
        addMethod(name, new FullFunctionCallbackMethod(this, method, Visibility.PRIVATE));
    }

    public void defineFastPrivateMethod(String name, Callback method) {
        addMethod(name, new SimpleCallbackMethod(this, method, Visibility.PRIVATE));
    }

    public void defineFastProtectedMethod(String name, Callback method) {
        addMethod(name, new SimpleCallbackMethod(this, method, Visibility.PROTECTED));
    }

    public void undefineMethod(String name) {
        addMethod(name, UndefinedMethod.getInstance());
    }

    /** rb_undef
     *
     */
    public void undef(String name) {
        Ruby runtime = getRuntime();
        if (this == runtime.getObject()) {
            runtime.secure(4);
        }
        if (runtime.getSafeLevel() >= 4 && !isTaint()) {
            throw new SecurityException("Insecure: can't undef");
        }
        testFrozen("module");
        if (name.equals("__id__") || name.equals("__send__")) {
            getRuntime().getWarnings().warn("undefining `"+ name +"' may cause serious problem");
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

            throw getRuntime().newNameError("Undefined method " + name + " for" + s0 + " '" + c.getName() + "'", name);
        }
        addMethod(name, UndefinedMethod.getInstance());
        
        if(isSingleton()){
            IRubyObject singleton = ((MetaClass)this).getAttached(); 
            singleton.callMethod(runtime.getCurrentContext(), "singleton_method_undefined", getRuntime().newSymbol(name));
        }else{
            callMethod(runtime.getCurrentContext(), "method_undefined", getRuntime().newSymbol(name));
    }
    }
    
    public IRubyObject include_p(IRubyObject arg) {
        if (!arg.isModule()) {
            throw getRuntime().newTypeError(arg, getRuntime().getModule());
        }
        
        for (RubyModule p = this; p != null; p = p.getSuperClass()) {
            if ((p instanceof IncludedModuleWrapper) && ((IncludedModuleWrapper) p).getNonIncludedClass() == arg) {
                return getRuntime().newBoolean(true);
            }
        }
        
        return getRuntime().newBoolean(false);
    }

    // TODO: Consider a better way of synchronizing 
    public void addMethod(String name, DynamicMethod method) {
        if (this == getRuntime().getObject()) {
            getRuntime().secure(4);
        }

        if (getRuntime().getSafeLevel() >= 4 && !isTaint()) {
            throw getRuntime().newSecurityError("Insecure: can't define method");
        }
        testFrozen("class/module");

        // We can safely reference methods here instead of doing getMethods() since if we
        // are adding we are not using a IncludedModuleWrapper.
        synchronized(getMethods()) {
            // If we add a method which already is cached in this class, then we should update the 
            // cachemap so it stays up to date.
            DynamicMethod existingMethod = (DynamicMethod) getMethods().remove(name);
            if (existingMethod != null) {
                getRuntime().getCacheMap().remove(name, existingMethod);
            }
            getRuntime().getMethodCache().removeMethod(name);
            putMethod(name, method);
        }
    }

    public void removeCachedMethod(String name) {
        getMethods().remove(name);
    }

    public void removeMethod(String name) {
        if (this == getRuntime().getObject()) {
            getRuntime().secure(4);
        }
        if (getRuntime().getSafeLevel() >= 4 && !isTaint()) {
            throw getRuntime().newSecurityError("Insecure: can't remove method");
        }
        testFrozen("class/module");

        // We can safely reference methods here instead of doing getMethods() since if we
        // are adding we are not using a IncludedModuleWrapper.
        synchronized(getMethods()) {
            DynamicMethod method = (DynamicMethod) getMethods().remove(name);
            if (method == null) {
                throw getRuntime().newNameError("method '" + name + "' not defined in " + getName(), name);
            }
            
            getRuntime().getMethodCache().removeMethod(name);

            getRuntime().getCacheMap().remove(name, method);
        }
        
        if(isSingleton()){
            IRubyObject singleton = ((MetaClass)this).getAttached(); 
            singleton.callMethod(getRuntime().getCurrentContext(), "singleton_method_removed", getRuntime().newSymbol(name));
        }else{
            callMethod(getRuntime().getCurrentContext(), "method_removed", getRuntime().newSymbol(name));
    }
    }

    private DynamicMethod searchMethodWithoutCache(String name) {
        for (RubyModule searchModule = this; searchModule != null; searchModule = searchModule.getSuperClass()) {
            // included modules use delegates methods for we need to synchronize on result of getMethods
            synchronized(searchModule.getMethods()) {
                // See if current class has method or if it has been cached here already
                DynamicMethod method = (DynamicMethod) searchModule.getMethods().get(name);
                
                if (method != null) {
                    return method;
                }
            }
        }

        return UndefinedMethod.getInstance();
    }

    /**
     * Search through this module and supermodules for method definitions. Cache superclass definitions in this class.
     * 
     * @param name The name of the method to search for
     * @return The method, or UndefinedMethod if not found
     */
    public DynamicMethod searchMethod(String name) {
        MethodCache cache = getRuntime().getMethodCache();
        MethodCache.CacheEntry entry = cache.getMethod(this, name);
        if (entry.klass == this && name.equals(entry.methodName)) {
            return entry.method;
        }
        
        for (RubyModule searchModule = this; searchModule != null; searchModule = searchModule.getSuperClass()) {
            // included modules use delegates methods for we need to synchronize on result of getMethods
            synchronized(searchModule.getMethods()) {
                // See if current class has method or if it has been cached here already
                DynamicMethod method = (DynamicMethod) searchModule.getMethods().get(name);
                
                if (method != null) {
                    cache.putMethod(this, name, method);
                    /*
                    // TO BE REMOVED
                    if (searchModule != this) {
                        addCachedMethod(name, method);
                    }
                    */
                    return method;
                }
            }
        }

        return UndefinedMethod.getInstance();
    }

    /**
     * Search through this module and supermodules for method definitions. Cache superclass definitions in this class.
     * 
     * @param name The name of the method to search for
     * @return The method, or UndefinedMethod if not found
     */
    public DynamicMethod retrieveMethod(String name) {
        return (DynamicMethod)getMethods().get(name);
    }

    /**
     * Search through this module and supermodules for method definitions. Cache superclass definitions in this class.
     * 
     * @param name The name of the method to search for
     * @return The method, or UndefinedMethod if not found
     */
    public RubyModule findImplementer(RubyModule clazz) {
        for (RubyModule searchModule = this; searchModule != null; searchModule = searchModule.getSuperClass()) {
            if (searchModule.isSame(clazz)) {
                return searchModule;
            }
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

    private IRubyObject getConstantInner(String name, boolean exclude) {
        IRubyObject objectClass = getRuntime().getObject();
        IRubyObject undef = getRuntime().getUndef();
        boolean retryForModule = false;
        RubyModule p = this;

        retry: while (true) {
            while (p != null) {
                IRubyObject constant = p.getInstanceVariable(name);

                if (constant == undef) {
                    p.removeInstanceVariable(name);
                    if (getRuntime().getLoadService().autoload(p.getName() + "::" + name) == null) break;
                    continue;
                }
                if (constant != null) {
                    if (exclude && p == objectClass && this != objectClass) {
                        getRuntime().getWarnings().warn("toplevel constant " + name +
                                " referenced by " + getName() + "::" + name);
                    }

                    return constant;
                }
                p = p.getSuperClass();
            }

            if (!exclude && !retryForModule && getClass().equals(RubyModule.class)) {
                retryForModule = true;
                p = getRuntime().getObject();
                continue retry;
            }

            break;
        }

        return callMethod(getRuntime().getCurrentContext(), "const_missing", RubySymbol.newSymbol(getRuntime(), name));
    }

    /**
     * Retrieve the named constant, invoking 'const_missing' should that be appropriate.
     * 
     * @param name The constant to retrieve
     * @return The value for the constant, or null if not found
     */
    public IRubyObject getConstant(String name) {
        return getConstantInner(name, false);
    }

    public IRubyObject getConstantFrom(String name) {
        return getConstantInner(name, true);
    }

    public IRubyObject getConstantAt(String name) {
        IRubyObject constant = getInstanceVariable(name);

        if (constant != getRuntime().getUndef()) return constant;
        
        removeInstanceVariable(name);
        return getRuntime().getLoadService().autoload(getName() + "::" + name);
    }

    // Fix for JRUBY-1339 - search hierarchy for constant
    /** rb_const_defined_at
     * 
     */
    public boolean isConstantDefined(String name) {
        boolean isObject = this == getRuntime().getObject();
        Object undef = getRuntime().getUndef();

        RubyModule module = this;

        do {
            Object value;
            if ((value = module.getInstanceVariable(name)) != null) {
                if (value != undef) return true;
                return getRuntime().getLoadService().autoloadFor(
                        module.getName() + "::" + name) != null;
            }

        } while (isObject && (module = module.getSuperClass()) != null );

        return false;
    }

    /** rb_alias
     *
     */
    public synchronized void defineAlias(String name, String oldName) {
        testFrozen("module");
        if (oldName.equals(name)) {
            return;
        }
        if (this == getRuntime().getObject()) {
            getRuntime().secure(4);
        }
        DynamicMethod method = searchMethod(oldName);
        DynamicMethod oldMethod = searchMethodWithoutCache(name);
        if (method.isUndefined()) {
            if (isModule()) {
                method = getRuntime().getObject().searchMethod(oldName);
            }

            if (method.isUndefined()) {
                throw getRuntime().newNameError("undefined method `" + oldName + "' for " +
                        (isModule() ? "module" : "class") + " `" + getName() + "'", oldName);
            }
        }
        getRuntime().getMethodCache().removeMethod(name);
        getRuntime().getCacheMap().remove(name, method);
        getRuntime().getCacheMap().remove(name, oldMethod);
        getRuntime().getCacheMap().remove(name, oldMethod.getRealMethod());
        
        putMethod(name, new AliasMethod(this, method, oldName));
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

    /** rb_define_const
     *
     */
    public void defineConstant(String name, IRubyObject value) {
        assert value != null;

        if (this == getRuntime().getClassClass()) {
            getRuntime().secure(4);
        }

        if (!IdUtil.isConstant(name)) {
            throw getRuntime().newNameError("bad constant name " + name, name);
        }

        setConstant(name, value);
    }

    /** rb_mod_remove_cvar
     *
     */
    public IRubyObject removeCvar(IRubyObject name) { // Wrong Parameter ?
        if (!IdUtil.isClassVariable(name.asSymbol())) {
            throw getRuntime().newNameError("wrong class variable name " + name.asSymbol(), name.asSymbol());
        }

        if (!isTaint() && getRuntime().getSafeLevel() >= 4) {
            throw getRuntime().newSecurityError("Insecure: can't remove class variable");
        }
        testFrozen("class/module");

        IRubyObject value = removeInstanceVariable(name.asSymbol());

        if (value != null) {
            return value;
        }

        if (isClassVarDefined(name.asSymbol())) {
            throw cannotRemoveError(name.asSymbol());
        }

        throw getRuntime().newNameError("class variable " + name.asSymbol() + " not defined for " + getName(), name.asSymbol());
    }

    private void addAccessor(String name, boolean readable, boolean writeable) {
        ThreadContext tc = getRuntime().getCurrentContext();

        // Check the visibility of the previous frame, which will be the frame in which the class is being eval'ed
        Visibility attributeScope = tc.getCurrentVisibility();
        if (attributeScope == Visibility.PRIVATE) {
            //FIXME warning
        } else if (attributeScope == Visibility.MODULE_FUNCTION) {
            attributeScope = Visibility.PRIVATE;
            // FIXME warning
        }
        final String variableName = ("@" + name).intern();
        final Ruby runtime = getRuntime();
        ThreadContext context = getRuntime().getCurrentContext();
        if (readable) {
            defineFastMethod(name, new Callback() {
                public IRubyObject execute(IRubyObject self, IRubyObject[] args, Block block) {
                    Arity.checkArgumentCount(getRuntime(), args, 0, 0);

                    IRubyObject variable = self.getInstanceVariable(variableName);

                    return variable == null ? runtime.getNil() : variable;
                }

                public Arity getArity() {
                    return Arity.noArguments();
                }
            });
            callMethod(context, "method_added", RubySymbol.newSymbol(getRuntime(), name));
        }
        if (writeable) {
            name = name + "=";
            defineFastMethod(name, new Callback() {
                public IRubyObject execute(IRubyObject self, IRubyObject[] args, Block block) {
                    // ENEBO: Can anyone get args to be anything but length 1?
                    Arity.checkArgumentCount(getRuntime(), args, 1, 1);

                    return self.setInstanceVariable(variableName, args[0]);
                }

                public Arity getArity() {
                    return Arity.singleArgument();
                }
            });
            callMethod(context, "method_added", RubySymbol.newSymbol(getRuntime(), name));
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
            exportMethod(methods[i].asSymbol(), visibility);
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
        }
    }

    /**
     * MRI: rb_method_boundp
     *
     */
    public boolean isMethodBound(String name, boolean checkVisibility) {
        DynamicMethod method = searchMethod(name);
        if (!method.isUndefined()) {
            return !(checkVisibility && method.getVisibility() == Visibility.PRIVATE);
        }
        return false;
    }

    public IRubyObject newMethod(IRubyObject receiver, String name, boolean bound) {
        DynamicMethod method = searchMethod(name);
        if (method.isUndefined()) {
            throw getRuntime().newNameError("undefined method `" + name +
                "' for class `" + this.getName() + "'", name);
        }

        RubyMethod newMethod = null;
        if (bound) {
            newMethod = RubyMethod.newMethod(method.getImplementationClass(), name, this, name, method, receiver);
        } else {
            newMethod = RubyUnboundMethod.newUnboundMethod(method.getImplementationClass(), name, this, name, method);
        }
        newMethod.infectBy(this);

        return newMethod;
    }

    // What is argument 1 for in this method? A Method or Proc object /OB
    public IRubyObject define_method(IRubyObject[] args, Block block) {
        if (args.length < 1 || args.length > 2) {
            throw getRuntime().newArgumentError("wrong # of arguments(" + args.length + " for 1)");
        }

        IRubyObject body;
        String name = args[0].asSymbol();
        DynamicMethod newMethod = null;
        ThreadContext tc = getRuntime().getCurrentContext();
        Visibility visibility = tc.getCurrentVisibility();

        if (visibility == Visibility.MODULE_FUNCTION) visibility = Visibility.PRIVATE;

        if (args.length == 1 || args[1].isKindOf(getRuntime().getProc())) {
            // double-testing args.length here, but it avoids duplicating the proc-setup code in two places
            RubyProc proc = (args.length == 1) ? getRuntime().newProc(false, block) : (RubyProc)args[1];
            body = proc;

            proc.getBlock().isLambda = true;
            proc.getBlock().getFrame().setKlazz(this);
            proc.getBlock().getFrame().setName(name);
            
            // for zsupers in define_method (blech!) we tell the proc scope to act as the "argument" scope
            proc.getBlock().getDynamicScope().getStaticScope().setArgumentScope(true);
            // just using required is broken...but no more broken than before zsuper refactoring
            proc.getBlock().getDynamicScope().getStaticScope().setRequiredArgs(proc.getBlock().arity().required());

            newMethod = new ProcMethod(this, proc, visibility);
        } else if (args[1].isKindOf(getRuntime().getMethod())) {
            RubyMethod method = (RubyMethod)args[1];
            body = method;

            newMethod = new MethodMethod(this, method.unbind(null), visibility);
        } else {
            throw getRuntime().newTypeError("wrong argument type " + args[0].getType().getName() + " (expected Proc/Method)");
        }

        addMethod(name, newMethod);

        RubySymbol symbol = RubySymbol.newSymbol(getRuntime(), name);
        ThreadContext context = getRuntime().getCurrentContext();

        if (tc.getPreviousVisibility() == Visibility.MODULE_FUNCTION) {
            getSingletonClass().addMethod(name, new WrapperMethod(getSingletonClass(), newMethod, Visibility.PUBLIC));
        }

        if(isSingleton()){
            IRubyObject singleton = ((MetaClass)this).getAttached(); 
            singleton.callMethod(context, "singleton_method_added", symbol);
        }else{
        callMethod(context, "method_added", symbol);
        }

        return body;
    }

    public IRubyObject executeUnder(Callback method, IRubyObject[] args, Block block) {
        ThreadContext context = getRuntime().getCurrentContext();

        context.preExecuteUnder(this, block);

        try {
            return method.execute(this, args, block);
        } finally {
            context.postExecuteUnder();
        }
    }

    public RubyString name() {
        return getRuntime().newString(getBaseName() == null ? "" : getName());
    }

    /** rb_mod_class_variables
     *
     */
    public RubyArray class_variables() {
        RubyArray ary = getRuntime().newArray();

        for (RubyModule p = this; p != null; p = p.getSuperClass()) {
            for (Iterator iter = p.instanceVariableNames(); iter.hasNext();) {
                String id = (String) iter.next();
                if (IdUtil.isClassVariable(id)) {
                    RubyString kval = getRuntime().newString(id);
                    if (!ary.includes(kval)) {
                        ary.append(kval);
                    }
                }
            }
        }
        return ary;
    }

    /** rb_mod_cvar_get
    *
    */
    public IRubyObject class_variable_get(IRubyObject var) {
        String varName = var.asSymbol();

        if (!IdUtil.isValidClassVariableName(varName)) {
            throw getRuntime().newNameError("`" + varName + "' is not allowed as a class variable name", varName);
        }

        return getClassVar(varName);
    }

    public IRubyObject class_variable_defined_p(IRubyObject var) {
        String name = var.asSymbol();

        if (!IdUtil.isValidClassVariableName(name)) {
            throw getRuntime().newNameError("`" + name + "' is not allowed as a class variable name", name);
        }

        RubyModule module = getModuleWithInstanceVar(name);

        if (module != null) {
            return module.getInstanceVariable(name) == null ? getRuntime().getFalse() : getRuntime().getTrue() ;
        }
        return getRuntime().getFalse();
    }

    /** rb_mod_cvar_set
    *
    */
    public IRubyObject class_variable_set(IRubyObject var, IRubyObject value) {
        String varName = var.asSymbol();

        if (!IdUtil.isValidClassVariableName(varName)) {
            throw getRuntime().newNameError("`" + varName + "' is not allowed as a class variable name", varName);
        }

        return setClassVar(varName, value);
    }

    protected IRubyObject cloneMethods(RubyModule clone) {
        RubyModule realType = this.getNonIncludedClass();
        for (Iterator iter = getMethods().entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iter.next();
            DynamicMethod method = (DynamicMethod) entry.getValue();
            // Do not clone cached methods
            // FIXME: MRI copies all methods here
            if (method.getImplementationClass() == realType || method instanceof UndefinedMethod) {
                
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
    public IRubyObject initialize_copy(IRubyObject original) {
        super.initialize_copy(original);

        RubyModule originalModule = (RubyModule)original;

        if (!getMetaClass().isSingleton()) setMetaClass(originalModule.getSingletonClassClone());
        setSuperClass(originalModule.getSuperClass());

        if (originalModule.safeHasInstanceVariables()) setInstanceVariables(new HashMap(originalModule.getInstanceVariables()));

        originalModule.cloneMethods(this);

        return this;
    }

    /** rb_mod_included_modules
     *
     */
    public RubyArray included_modules() {
        RubyArray ary = getRuntime().newArray();

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
    public RubyArray ancestors() {
        RubyArray ary = getRuntime().newArray(getAncestorList());

        return ary;
    }

    public List getAncestorList() {
        ArrayList list = new ArrayList();

        for (RubyModule p = this; p != null; p = p.getSuperClass()) {
            if(!p.isSingleton()) {
                list.add(p.getNonIncludedClass());
            }
        }

        return list;
    }

    public boolean hasModuleInHierarchy(RubyModule type) {
        // XXX: This check previously used callMethod("==") to check for equality between classes
        // when scanning the hierarchy. However the == check may be safe; we should only ever have
        // one instance bound to a given type/constant. If it's found to be unsafe, examine ways
        // to avoid the == call.
        for (RubyModule p = this; p != null; p = p.getSuperClass()) {
            if (p.getNonIncludedClass() == type) return true;
        }

        return false;
    }

    public int hashCode() {
        return id;
    }

    public RubyFixnum hash() {
        return getRuntime().newFixnum(id);
    }

    /** rb_mod_to_s
     *
     */
    public IRubyObject to_s() {
        if(isSingleton()){            
            IRubyObject attached = ((MetaClass)this).getAttached();
            StringBuffer buffer = new StringBuffer("#<Class:");
            if(attached instanceof RubyClass || attached instanceof RubyModule){
                buffer.append(attached.inspect());
            }else{
                buffer.append(attached.anyToString());
            }
            buffer.append(">");
            return getRuntime().newString(buffer.toString());
        }
        return getRuntime().newString(getName());
    }

    /** rb_mod_eqq
     *
     */
    public RubyBoolean op_eqq(IRubyObject obj) {
        return getRuntime().newBoolean(obj.isKindOf(this));
    }

    public IRubyObject op_equal(IRubyObject other) {
        return super.obj_equal(other);
    }

    /** rb_mod_freeze
     *
     */   
    public IRubyObject freeze() {
        to_s();
        return super.freeze();
    }

    /** rb_mod_le
    *
    */
   public IRubyObject op_le(IRubyObject obj) {
       if (!(obj instanceof RubyModule)) {
           throw getRuntime().newTypeError("compared with non class/module");
       }

       if (isKindOfModule((RubyModule)obj)) {
           return getRuntime().getTrue();
       } else if (((RubyModule)obj).isKindOfModule(this)) {
           return getRuntime().getFalse();
       }

       return getRuntime().getNil();
   }

   /** rb_mod_lt
    *
    */
   public IRubyObject op_lt(IRubyObject obj) {
    return obj == this ? getRuntime().getFalse() : op_le(obj);
   }

   /** rb_mod_ge
    *
    */
   public IRubyObject op_ge(IRubyObject obj) {
       if (!(obj instanceof RubyModule)) {
           throw getRuntime().newTypeError("compared with non class/module");
       }

       return ((RubyModule) obj).op_le(this);
   }

   /** rb_mod_gt
    *
    */
   public IRubyObject op_gt(IRubyObject obj) {
       return this == obj ? getRuntime().getFalse() : op_ge(obj);
   }

   /** rb_mod_cmp
    *
    */
   public IRubyObject op_cmp(IRubyObject obj) {
       if (this == obj) {
           return getRuntime().newFixnum(0);
       }

       if (!(obj instanceof RubyModule)) {
           throw getRuntime().newTypeError(
               "<=> requires Class or Module (" + getMetaClass().getName() + " given)");
       }

       RubyModule module = (RubyModule)obj;

       if (module.isKindOfModule(this)) {
           return getRuntime().newFixnum(1);
       } else if (this.isKindOfModule(module)) {
           return getRuntime().newFixnum(-1);
       }

       return getRuntime().getNil();
   }

   public boolean isKindOfModule(RubyModule type) {
       for (RubyModule p = this; p != null; p = p.getSuperClass()) {
           if (p.isSame(type)) {
               return true;
           }
       }

       return false;
   }

   protected boolean isSame(RubyModule module) {
       return this == module;
   }

    /** rb_mod_initialize
     *
     */
    public IRubyObject initialize(Block block) {
        if (block.isGiven()) {
            // class and module bodies default to public, so make the block's visibility public. JRUBY-1185.
            block.setVisibility(Visibility.PUBLIC);
            block.yield(getRuntime().getCurrentContext(), null, this, this, false);
        }

        return getRuntime().getNil();
    }

    /** rb_mod_attr
     *
     */
    public IRubyObject attr(IRubyObject[] args) {
        Arity.checkArgumentCount(getRuntime(), args, 1, 2);
        boolean writeable = args.length > 1 ? args[1].isTrue() : false;

        addAccessor(args[0].asSymbol(), true, writeable);

        return getRuntime().getNil();
    }

    /** rb_mod_attr_reader
     *
     */
    public IRubyObject attr_reader(IRubyObject[] args) {
        for (int i = 0; i < args.length; i++) {
            addAccessor(args[i].asSymbol(), true, false);
        }

        return getRuntime().getNil();
    }

    /** rb_mod_attr_writer
     *
     */
    public IRubyObject attr_writer(IRubyObject[] args) {
        for (int i = 0; i < args.length; i++) {
            addAccessor(args[i].asSymbol(), false, true);
        }

        return getRuntime().getNil();
    }

    /** rb_mod_attr_accessor
     *
     */
    public IRubyObject attr_accessor(IRubyObject[] args) {
        for (int i = 0; i < args.length; i++) {
            addAccessor(args[i].asSymbol(), true, true);
        }

        return getRuntime().getNil();
    }

    /** rb_mod_const_get
     *
     */
    public IRubyObject const_get(IRubyObject symbol) {
        String name = symbol.asSymbol();

        if (!IdUtil.isValidConstantName(name)) {
            throw wrongConstantNameError(name);
        }

        return getConstant(name);
    }

    /** rb_mod_const_set
     *
     */
    public IRubyObject const_set(IRubyObject symbol, IRubyObject value) {
        String name = symbol.asSymbol();

        if (!IdUtil.isValidConstantName(name)) {
            throw wrongConstantNameError(name);
        }

        return setConstant(name, value);
    }

    /** rb_mod_const_defined
     *
     */
    public RubyBoolean const_defined(IRubyObject symbol) {
        String name = symbol.asSymbol();

        if (!IdUtil.isValidConstantName(name)) {
            throw wrongConstantNameError(name);
        }
        
        return getRuntime().newBoolean(isConstantDefined(name));
    }

    private RaiseException wrongConstantNameError(String name) {
        return getRuntime().newNameError("wrong constant name " + name, name);
    }

    /**
     * Get a list of all instance methods names of the provided visibility unless not is true, then 
     * get all methods which are not the provided visibility.
     * 
     * @param args passed into one of the Ruby instance_method methods
     * @param visibility to find matching instance methods against
     * @param not if true only find methods not matching supplied visibility
     * @return a RubyArray of instance method names
     */
    private RubyArray instance_methods(IRubyObject[] args, final Visibility visibility, boolean not) {
        boolean includeSuper = args.length > 0 ? args[0].isTrue() : true;
        RubyArray ary = getRuntime().newArray();
        HashMap<String, Boolean> undefinedMethods = new HashMap<String, Boolean>();
        Set<String> added = new HashSet<String>();

        for (RubyModule type = this; type != null; type = type.getSuperClass()) {
            RubyModule realType = type.getNonIncludedClass();
            for (Iterator iter = type.getMethods().entrySet().iterator(); iter.hasNext();) {
                Map.Entry entry = (Map.Entry) iter.next();
                DynamicMethod method = (DynamicMethod) entry.getValue();
                String methodName = (String) entry.getKey();

                if (method.isUndefined()) {
                    undefinedMethods.put(methodName, Boolean.TRUE);
                    continue;
                }
                if (method.getImplementationClass() == realType &&
                    (!not && method.getVisibility() == visibility || (not && method.getVisibility() != visibility)) &&
                    undefinedMethods.get(methodName) == null) {

                    if (!added.contains(methodName)) {
                        ary.append(getRuntime().newString(methodName));
                        added.add(methodName);
                    }
                }
            }

            if (!includeSuper) {
                break;
            }
        }

        return ary;
    }

    public RubyArray instance_methods(IRubyObject[] args) {
        return instance_methods(args, Visibility.PRIVATE, true);
    }

    public RubyArray public_instance_methods(IRubyObject[] args) {
        return instance_methods(args, Visibility.PUBLIC, false);
    }

    public IRubyObject instance_method(IRubyObject symbol) {
        return newMethod(null, symbol.asSymbol(), false);
    }

    /** rb_class_protected_instance_methods
     *
     */
    public RubyArray protected_instance_methods(IRubyObject[] args) {
        return instance_methods(args, Visibility.PROTECTED, false);
    }

    /** rb_class_private_instance_methods
     *
     */
    public RubyArray private_instance_methods(IRubyObject[] args) {
        return instance_methods(args, Visibility.PRIVATE, false);
    }

    /** rb_mod_constants
     *
     */
    public RubyArray constants() {
        ArrayList constantNames = new ArrayList();
        RubyModule objectClass = getRuntime().getObject();

        if (getRuntime().getModule() == this) {
            for (Iterator vars = objectClass.instanceVariableNames();
                 vars.hasNext();) {
                String name = (String) vars.next();
                if (IdUtil.isConstant(name)) {
                    constantNames.add(getRuntime().newString(name));
                }
            }

            return getRuntime().newArray(constantNames);
        } else if (getRuntime().getObject() == this) {
            for (Iterator vars = instanceVariableNames(); vars.hasNext();) {
                String name = (String) vars.next();
                if (IdUtil.isConstant(name)) {
                    constantNames.add(getRuntime().newString(name));
                }
            }

            return getRuntime().newArray(constantNames);
        }

        for (RubyModule p = this; p != null; p = p.getSuperClass()) {
            if (objectClass == p) {
                continue;
            }

            for (Iterator vars = p.instanceVariableNames(); vars.hasNext();) {
                String name = (String) vars.next();
                if (IdUtil.isConstant(name)) {
                    constantNames.add(getRuntime().newString(name));
                }
            }
        }

        return getRuntime().newArray(constantNames);
    }

    /** rb_mod_remove_cvar
     *
     */
    public IRubyObject remove_class_variable(IRubyObject name) {
        String id = name.asSymbol();

        if (!IdUtil.isClassVariable(id)) {
            throw getRuntime().newNameError("wrong class variable name " + id, id);
        }
        if (!isTaint() && getRuntime().getSafeLevel() >= 4) {
            throw getRuntime().newSecurityError("Insecure: can't remove class variable");
        }
        testFrozen("class/module");

        IRubyObject variable = removeInstanceVariable(id);
        if (variable != null) {
            return variable;
        }

        if (isClassVarDefined(id)) {
            throw cannotRemoveError(id);
        }
        throw getRuntime().newNameError("class variable " + id + " not defined for " + getName(), id);
    }

    private RaiseException cannotRemoveError(String id) {
        return getRuntime().newNameError("cannot remove " + id + " for " + getName(), id);
    }

    public IRubyObject remove_const(IRubyObject name) {
        String id = name.asSymbol();

        if (!IdUtil.isConstant(id)) {
            throw wrongConstantNameError(id);
        }
        if (!isTaint() && getRuntime().getSafeLevel() >= 4) {
            throw getRuntime().newSecurityError("Insecure: can't remove class variable");
        }
        testFrozen("class/module");

        IRubyObject variable = getInstanceVariable(id);
        if (variable != null) {
            if (variable == getRuntime().getUndef()) {
                getRuntime().getLoadService().removeAutoLoadFor(getName() + "::" + id);
            }
            return removeInstanceVariable(id);
        }

        if (isClassVarDefined(id)) {
            throw cannotRemoveError(id);
        }
        throw getRuntime().newNameError("constant " + id + " not defined for " + getName(), id);
    }

    /** rb_mod_append_features
     *
     */
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
    public IRubyObject extend_object(IRubyObject obj) {
        obj.getSingletonClass().includeModule(this);
        return obj;
    }

    /** rb_mod_include
     *
     */
    public RubyModule include(IRubyObject[] modules) {
        ThreadContext context = getRuntime().getCurrentContext();
        // MRI checks all types first:
        for (int i = modules.length; --i >= 0; ) {
            IRubyObject obj = modules[i];
            if (!obj.isModule()) throw getRuntime().newTypeError(obj,getRuntime().getModule());
        }
        for (int i = modules.length - 1; i >= 0; i--) {
            modules[i].callMethod(context, "append_features", this);
            modules[i].callMethod(context, "included", this);
        }

        return this;
    }

    public IRubyObject included(IRubyObject other) {
        return getRuntime().getNil();
    }

    public IRubyObject extended(IRubyObject other, Block block) {
        return getRuntime().getNil();
    }

    private void setVisibility(IRubyObject[] args, Visibility visibility) {
        if (getRuntime().getSafeLevel() >= 4 && !isTaint()) {
            throw getRuntime().newSecurityError("Insecure: can't change method visibility");
        }

        if (args.length == 0) {
            // Note: we change current frames visibility here because the methods which call
            // this method are all "fast" (e.g. they do not created their own frame).
            getRuntime().getCurrentContext().setCurrentVisibility(visibility);
        } else {
            setMethodVisibility(args, visibility);
        }
    }

    /** rb_mod_public
     *
     */
    public RubyModule rbPublic(IRubyObject[] args) {
        setVisibility(args, Visibility.PUBLIC);
        return this;
    }

    /** rb_mod_protected
     *
     */
    public RubyModule rbProtected(IRubyObject[] args) {
        setVisibility(args, Visibility.PROTECTED);
        return this;
    }

    /** rb_mod_private
     *
     */
    public RubyModule rbPrivate(IRubyObject[] args) {
        setVisibility(args, Visibility.PRIVATE);
        return this;
    }

    /** rb_mod_modfunc
     *
     */
    public RubyModule module_function(IRubyObject[] args) {
        if (getRuntime().getSafeLevel() >= 4 && !isTaint()) {
            throw getRuntime().newSecurityError("Insecure: can't change method visibility");
        }

        ThreadContext context = getRuntime().getCurrentContext();

        if (args.length == 0) {
            context.setCurrentVisibility(Visibility.MODULE_FUNCTION);
        } else {
            setMethodVisibility(args, Visibility.PRIVATE);

            for (int i = 0; i < args.length; i++) {
                String name = args[i].asSymbol();
                DynamicMethod method = searchMethod(name);
                assert !method.isUndefined() : "undefined method '" + name + "'";
                getSingletonClass().addMethod(name, new WrapperMethod(getSingletonClass(), method, Visibility.PUBLIC));
                callMethod(context, "singleton_method_added", RubySymbol.newSymbol(getRuntime(), name));
            }
        }
        return this;
    }

    public IRubyObject method_added(IRubyObject nothing, Block block) {
        return getRuntime().getNil();
    }

    public IRubyObject method_removed(IRubyObject nothing, Block block) {
        return getRuntime().getNil();
    }

    public IRubyObject method_undefined(IRubyObject nothing, Block block) {
        return getRuntime().getNil();
    }
    
    public RubyBoolean method_defined(IRubyObject symbol) {
        return isMethodBound(symbol.asSymbol(), true) ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    public IRubyObject public_method_defined(IRubyObject symbol) {
	    DynamicMethod method = searchMethod(symbol.asSymbol());
	    
		return getRuntime().newBoolean(!method.isUndefined() && method.getVisibility() == Visibility.PUBLIC);
    }

    public IRubyObject protected_method_defined(IRubyObject symbol) {
	    DynamicMethod method = searchMethod(symbol.asSymbol());
	    
		return getRuntime().newBoolean(!method.isUndefined() && method.getVisibility() == Visibility.PROTECTED);
    }
	
    public IRubyObject private_method_defined(IRubyObject symbol) {
	    DynamicMethod method = searchMethod(symbol.asSymbol());
	    
		return getRuntime().newBoolean(!method.isUndefined() && method.getVisibility() == Visibility.PRIVATE);
    }

    public RubyModule public_class_method(IRubyObject[] args) {
        getMetaClass().setMethodVisibility(args, Visibility.PUBLIC);
        return this;
    }

    public RubyModule private_class_method(IRubyObject[] args) {
        getMetaClass().setMethodVisibility(args, Visibility.PRIVATE);
        return this;
    }

    public RubyModule alias_method(IRubyObject newId, IRubyObject oldId) {
        defineAlias(newId.asSymbol(), oldId.asSymbol());
        return this;
    }

    public RubyModule undef_method(IRubyObject[] args) {
        for (int i=0; i<args.length; i++) {
            undef(args[i].asSymbol());
        }
        return this;
    }

    public IRubyObject module_eval(IRubyObject[] args, Block block) {
        return specificEval(this, args, block);
    }

    public RubyModule remove_method(IRubyObject[] args) {
        for(int i=0;i<args.length;i++) {
            removeMethod(args[i].asSymbol());
        }
        return this;
    }

    public static void marshalTo(RubyModule module, MarshalStream output) throws java.io.IOException {
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
    public static RubyArray nesting(IRubyObject recv, Block block) {
        Ruby runtime = recv.getRuntime();
        RubyModule object = runtime.getObject();
        StaticScope scope = runtime.getCurrentContext().getCurrentScope().getStaticScope();
        RubyArray result = runtime.newArray();
        
        for (StaticScope current = scope; current.getModule() != object; current = current.getPreviousCRefScope()) {
            result.append(current.getModule());
        }
        
        return result;
    }

    private void doIncludeModule(RubyModule includedModule) {

        boolean changed = false;
        boolean skip = false;

        RubyModule currentModule = this;
        while (includedModule != null) {

            if (getNonIncludedClass() == includedModule.getNonIncludedClass()) {
                throw getRuntime().newArgumentError("cyclic include detected");
            }

            boolean superclassSeen = false;

            // scan class hierarchy for module
            for (RubyModule superClass = this.getSuperClass(); superClass != null; superClass = superClass.getSuperClass()) {
                if (superClass instanceof IncludedModuleWrapper) {
                    if (superClass.getNonIncludedClass() == includedModule.getNonIncludedClass()) {
                        if (!superclassSeen) {
                            currentModule = superClass;
                        }
                        skip = true;
                        break;
                    }
                } else {
                    superclassSeen = true;
                }
            }

            if (!skip) {

                // blow away caches for any methods that are redefined by module
                getRuntime().getCacheMap().moduleIncluded(currentModule, includedModule);
                
                // In the current logic, if we get here we know that module is not an
                // IncludedModuleWrapper, so there's no need to fish out the delegate. But just
                // in case the logic should change later, let's do it anyway:
                currentModule.setSuperClass(new IncludedModuleWrapper(getRuntime(), currentModule.getSuperClass(),
                        includedModule.getNonIncludedClass()));
                currentModule = currentModule.getSuperClass();
                changed = true;
            }

            includedModule = includedModule.getSuperClass();
            skip = false;
        }
        if (changed) {
            getRuntime().getMethodCache().clearCacheForModule(this);
        }
    }

}
