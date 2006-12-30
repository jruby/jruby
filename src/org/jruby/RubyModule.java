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
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jruby.internal.runtime.methods.AliasMethod;
import org.jruby.internal.runtime.methods.FullFunctionCallbackMethod;
import org.jruby.internal.runtime.methods.SimpleCallbackMethod;
import org.jruby.internal.runtime.methods.MethodMethod;
import org.jruby.internal.runtime.methods.ProcMethod;
import org.jruby.internal.runtime.methods.UndefinedMethod;
import org.jruby.internal.runtime.methods.WrapperCallable;
import org.jruby.internal.runtime.methods.WrapperMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.DynamicMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callback.Callback;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.IdUtil;
import org.jruby.util.collections.SinglyLinkedList;
import org.jruby.exceptions.RaiseException;

/**
 *
 * @author  jpetersen
 */
public class RubyModule extends RubyObject {
    private static final String CVAR_TAINT_ERROR =
        "Insecure: can't modify class variable";
    private static final String CVAR_FREEZE_ERROR = "class/module";

    // superClass may be null.
    private RubyClass superClass;

    // Containing class...The parent of Object is null. Object should always be last in chain.
    //public RubyModule parentModule;

    // CRef...to eventually replace parentModule
    public SinglyLinkedList cref;

    // ClassId is the name of the class/module sans where it is located.
    // If it is null, then it an anonymous class.
    private String classId;

    // All methods and all CACHED methods for the module.  The cached methods will be removed
    // when appropriate (e.g. when method is removed by source class or a new method is added
    // with same name by one of its subclasses).
    private Map methods = new HashMap();

    protected RubyModule(IRuby runtime, RubyClass metaClass, RubyClass superClass, SinglyLinkedList parentCRef, String name) {
        super(runtime, metaClass);

        this.superClass = superClass;
        //this.parentModule = parentModule;

        setBaseName(name);

        // If no parent is passed in, it is safe to assume Object.
        if (parentCRef == null) {
            if (runtime.getObject() != null) {
                parentCRef = runtime.getObject().getCRef();
            }
        }
        this.cref = new SinglyLinkedList(this, parentCRef);
    }

    /** Getter for property superClass.
     * @return Value of property superClass.
     */
    public RubyClass getSuperClass() {
        return superClass;
    }

    private void setSuperClass(RubyClass superClass) {
        this.superClass = superClass;
    }

    public RubyModule getParent() {
        if (cref.getNext() == null) {
            return null;
        }

        return (RubyModule)cref.getNext().getValue();
    }

    public void setParent(RubyModule p) {
        cref.setNext(p.getCRef());
    }

    public Map getMethods() {
        return methods;
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
            result.insert(0, "::").insert(0, p.getBaseName());
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
    public IRubyObject const_missing(IRubyObject name) {
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

        RubyModule p, c;
        boolean changed = false;
        boolean skip = false;

        c = this;
        while (module != null) {
            if (getNonIncludedClass() == module.getNonIncludedClass()) {
                throw getRuntime().newArgumentError("cyclic include detected");
            }

            boolean superclassSeen = false;
            for (p = getSuperClass(); p != null; p = p.getSuperClass()) {
                if (p instanceof IncludedModuleWrapper) {
                    if (p.getNonIncludedClass() == module.getNonIncludedClass()) {
                        if (!superclassSeen) {
                            c = p;
                        }
                        skip = true;
                        break;
                    }
                } else {
                    superclassSeen = true;
                }
            }
            if (!skip) {
                // In the current logic, if we get here we know that module is not an 
                // IncludedModuleWrapper, so there's no need to fish out the delegate. But just 
                // in case the logic should change later, let's do it anyway:
                c.setSuperClass(new IncludedModuleWrapper(getRuntime(), c.getSuperClass(),
                        module.getNonIncludedClass()));
                c = c.getSuperClass();
                changed = true;
            }

            module = module.getSuperClass();
            skip = false;
        }

        if (changed) {
            // MRI seems to blow away its cache completely after an include; is
            // what we're doing here really safe?
            List methodNames = new ArrayList(((RubyModule) arg).getMethods().keySet());
            for (Iterator iter = methodNames.iterator();
                 iter.hasNext();) {
                String methodName = (String) iter.next();
                getRuntime().getCacheMap().remove(methodName, searchMethod(methodName));
            }
        }

    }

    public void defineMethod(String name, Callback method) {
        Visibility visibility = name.equals("initialize") ?
                Visibility.PRIVATE : Visibility.PUBLIC;
        addMethod(name, new FullFunctionCallbackMethod(this, method, visibility));
    }

    public void defineFastMethod(String name, Callback method) {
        Visibility visibility = name.equals("initialize") ?
                Visibility.PRIVATE : Visibility.PUBLIC;
        addMethod(name, new SimpleCallbackMethod(this, method, visibility));
    }

    public void definePrivateMethod(String name, Callback method) {
        addMethod(name, new FullFunctionCallbackMethod(this, method, Visibility.PRIVATE));
    }

    public void defineFastPrivateMethod(String name, Callback method) {
        addMethod(name, new SimpleCallbackMethod(this, method, Visibility.PRIVATE));
    }

    public void undefineMethod(String name) {
        addMethod(name, UndefinedMethod.getInstance());
    }

    /** rb_undef
     *
     */
    public void undef(String name) {
        IRuby runtime = getRuntime();
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
                IRubyObject obj = getInstanceVariable("__attached__");

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
    }

    private void addCachedMethod(String name, DynamicMethod method) {
        // Included modules modify the original 'included' modules class.  Since multiple
        // classes can include the same module, we cannot cache in the original included module.
        if (!isIncluded()) {
            getMethods().put(name, method);
            getRuntime().getCacheMap().add(method, this);
        }
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

            getMethods().put(name, method);
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

            getRuntime().getCacheMap().remove(name, method);
        }
    }

    /**
     * Search through this module and supermodules for method definitions. Cache superclass definitions in this class.
     * 
     * @param name The name of the method to search for
     * @return The method, or UndefinedMethod if not found
     */
    public DynamicMethod searchMethod(String name) {
        for (RubyModule searchModule = this; searchModule != null; searchModule = searchModule.getSuperClass()) {
            // included modules use delegates methods for we need to synchronize on result of getMethods
            synchronized(searchModule.getMethods()) {
                // See if current class has method or if it has been cached here already
                DynamicMethod method = (DynamicMethod) searchModule.getMethods().get(name);
                if (method != null) {
                    if (searchModule != this) {
                        addCachedMethod(name, method);
                    }

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
        addSingletonMethod(name, method);
    }

    /** rb_define_module_function
     *
     */
    public void defineModuleFunction(String name, Callback method) {
        definePrivateMethod(name, method);
        defineSingletonMethod(name, method);
    }

    /** rb_define_module_function
     *
     */
    public void definePublicModuleFunction(String name, Callback method) {
        defineMethod(name, method);
        defineSingletonMethod(name, method);
    }

    /** rb_define_module_function
     *
     */
    public void defineFastModuleFunction(String name, Callback method) {
        defineFastPrivateMethod(name, method);
        defineFastSingletonMethod(name, method);
    }

    /** rb_define_module_function
     *
     */
    public void defineFastPublicModuleFunction(String name, Callback method) {
        defineFastMethod(name, method);
        defineFastSingletonMethod(name, method);
    }

    private IRubyObject getConstantInner(String name, boolean exclude) {
        IRubyObject objectClass = getRuntime().getObject();
        boolean retryForModule = false;
        RubyModule p = this;

        retry: while (true) {
            while (p != null) {
                IRubyObject constant = p.getConstantAt(name);

                if (constant == null) {
                    if (getRuntime().getLoadService().autoload(p.getName() + "::" + name) != null) {
                        continue;
                    }
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
        return getInstanceVariable(name);
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
        if (method.isUndefined()) {
            if (isModule()) {
                method = getRuntime().getObject().searchMethod(oldName);
            }

            if (method.isUndefined()) {
                throw getRuntime().newNameError("undefined method `" + oldName + "' for " +
                        (isModule() ? "module" : "class") + " `" + getName() + "'", oldName);
            }
        }
        getRuntime().getCacheMap().remove(name, searchMethod(name));
        getMethods().put(name, new AliasMethod(method, oldName));
    }

    public RubyClass defineOrGetClassUnder(String name, RubyClass superClazz) {
        IRubyObject type = getConstantAt(name);

        if (type == null) {
            return (RubyClass) setConstant(name,
                    getRuntime().defineClassUnder(name, superClazz, cref));
        } 

        if (!(type instanceof RubyClass)) {
            throw getRuntime().newTypeError(name + " is not a class.");
        } else if (superClazz != null && ((RubyClass) type).getSuperClass().getRealClass() != superClazz) {
            throw getRuntime().newTypeError("superclass mismatch for class " + name);
        }

        return (RubyClass) type;
    }

    /** rb_define_class_under
     *
     */
    public RubyClass defineClassUnder(String name, RubyClass superClazz) {
        IRubyObject type = getConstantAt(name);

        if (type == null) {
            return (RubyClass) setConstant(name,
                    getRuntime().defineClassUnder(name, superClazz, cref));
        }

        if (!(type instanceof RubyClass)) {
            throw getRuntime().newTypeError(name + " is not a class.");
        } else if (((RubyClass) type).getSuperClass().getRealClass() != superClazz) {
            throw getRuntime().newNameError(name + " is already defined.", name);
        }

        return (RubyClass) type;
    }

    public RubyModule defineModuleUnder(String name) {
        IRubyObject type = getConstantAt(name);

        if (type == null) {
            return (RubyModule) setConstant(name,
                    getRuntime().defineModuleUnder(name, cref));
        }

        if (!(type instanceof RubyModule)) {
            throw getRuntime().newTypeError(name + " is not a module.");
        }

        return (RubyModule) type;
    }

    /** rb_define_const
     *
     */
    public void defineConstant(String name, IRubyObject value) {
        assert value != null;

        if (this == getRuntime().getClass("Class")) {
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
        if (attributeScope.isPrivate()) {
            //FIXME warning
        } else if (attributeScope.isModuleFunction()) {
            attributeScope = Visibility.PRIVATE;
            // FIXME warning
        }
        final String variableName = "@" + name;
        final IRuby runtime = getRuntime();
        ThreadContext context = getRuntime().getCurrentContext();
        if (readable) {
            defineMethod(name, new Callback() {
                public IRubyObject execute(IRubyObject self, IRubyObject[] args) {
                    checkArgumentCount(args, 0, 0);

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
            defineMethod(name, new Callback() {
                public IRubyObject execute(IRubyObject self, IRubyObject[] args) {
                    IRubyObject[] fargs = runtime.getCurrentContext().getFrameArgs();

                    if (fargs.length != 1) {
                        throw runtime.newArgumentError("wrong # of arguments(" + fargs.length + "for 1)");
                    }

                    return self.setInstanceVariable(variableName, fargs[0]);
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
                final ThreadContext context = getRuntime().getCurrentContext();
                addMethod(name, new FullFunctionCallbackMethod(this, new Callback() {
                    public IRubyObject execute(IRubyObject self, IRubyObject[] args) {
                        return context.callSuper(context.getFrameArgs());
                    }

                    public Arity getArity() {
                        return Arity.optional();
                    }
                }, visibility));
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
            return !(checkVisibility && method.getVisibility().isPrivate());
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

    // What is argument 1 for in this method?
    public IRubyObject define_method(IRubyObject[] args) {
        if (args.length < 1 || args.length > 2) {
            throw getRuntime().newArgumentError("wrong # of arguments(" + args.length + " for 1)");
        }

        IRubyObject body;
        String name = args[0].asSymbol();
        DynamicMethod newMethod = null;
        ThreadContext tc = getRuntime().getCurrentContext();
        Visibility visibility = tc.getCurrentVisibility();

        if (visibility.isModuleFunction()) {
            visibility = Visibility.PRIVATE;
        }


        if (args.length == 1 || args[1].isKindOf(getRuntime().getClass("Proc"))) {
            // double-testing args.length here, but it avoids duplicating the proc-setup code in two places
            RubyProc proc = (args.length == 1) ? getRuntime().newProc() : (RubyProc)args[1];
            body = proc;

            proc.getBlock().isLambda = true;
            proc.getBlock().getFrame().setLastClass(this);
            proc.getBlock().getFrame().setLastFunc(name);

            newMethod = new ProcMethod(this, proc, visibility);
        } else if (args[1].isKindOf(getRuntime().getClass("Method"))) {
            RubyMethod method = (RubyMethod)args[1];
            body = method;

            newMethod = new MethodMethod(this, method.unbind(), visibility);
        } else {
            throw getRuntime().newTypeError("wrong argument type " + args[0].getType().getName() + " (expected Proc/Method)");
        }

        addMethod(name, newMethod);

        RubySymbol symbol = RubySymbol.newSymbol(getRuntime(), name);
        ThreadContext context = getRuntime().getCurrentContext();

        if (tc.getPreviousVisibility().isModuleFunction()) {
            getSingletonClass().addMethod(name, new WrapperMethod(getSingletonClass(), newMethod, Visibility.PUBLIC));
            callMethod(context, "singleton_method_added", symbol);
        }

        callMethod(context, "method_added", symbol);

        return body;
    }

    public IRubyObject executeUnder(Callback method, IRubyObject[] args) {
        ThreadContext context = getRuntime().getCurrentContext();

        context.preExecuteUnder(this);

        try {
            return method.execute(this, args);
        } finally {
            context.postExecuteUnder();
        }
    }

    // Methods of the Module Class (rb_mod_*):

    public static RubyModule newModule(IRuby runtime, String name) {
        return newModule(runtime, name, null);
    }

    public static RubyModule newModule(IRuby runtime, String name, SinglyLinkedList parentCRef) {
        // Modules do not directly define Object as their superClass even though in theory they
        // should.  The C version of Ruby may also do this (special checks in rb_alias for Module
        // makes me think this).
        // TODO cnutter: Shouldn't new modules have Module as their superclass?
        RubyModule newModule = new RubyModule(runtime, runtime.getClass("Module"), null, parentCRef, name);
        ThreadContext tc = runtime.getCurrentContext();
        if (tc.isBlockGiven()) {
            tc.yieldCurrentBlock(null, newModule, newModule, false);
        }
        return newModule;
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

    /** rb_mod_clone
     *
     */
    public IRubyObject rbClone() {
        return cloneMethods((RubyModule) super.rbClone());
    }

    protected IRubyObject cloneMethods(RubyModule clone) {
        RubyModule realType = this.getNonIncludedClass();
        for (Iterator iter = getMethods().entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iter.next();
            DynamicMethod method = (DynamicMethod) entry.getValue();

            // Do not clone cached methods
            if (method.getImplementationClass() == realType) {
                // A cloned method now belongs to a new class.  Set it.
                // TODO: Make DynamicMethod immutable
                DynamicMethod clonedMethod = (DynamicMethod)method.dup();
                clonedMethod.setImplementationClass(clone);
                clone.getMethods().put(entry.getKey(), clonedMethod);
            }
        }

        return clone;
    }

    protected IRubyObject doClone() {
        return RubyModule.newModule(getRuntime(), getBaseName(), cref.getNext());
    }

    /** rb_mod_dup
     *
     */
    public IRubyObject dup() {
        RubyModule dup = (RubyModule) rbClone();
        dup.setMetaClass(getMetaClass());
        dup.setFrozen(false);
        // +++ jpetersen
        // dup.setSingleton(isSingleton());
        // --- jpetersen

        return dup;
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

    /** rb_mod_to_s
     *
     */
    public IRubyObject to_s() {
        return getRuntime().newString(getName());
    }

    /** rb_mod_eqq
     *
     */
    public RubyBoolean op_eqq(IRubyObject obj) {
        return getRuntime().newBoolean(obj.isKindOf(this));
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

   public boolean isSame(RubyModule module) {
       return this == module;
   }

    /** rb_mod_initialize
     *
     */
    public IRubyObject initialize(IRubyObject[] args) {
        return getRuntime().getNil();
    }

    /** rb_mod_attr
     *
     */
    public IRubyObject attr(IRubyObject[] args) {
        checkArgumentCount(args, 1, 2);
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

        if (!IdUtil.isConstant(name)) {
            throw wrongConstantNameError(name);
        }

        return getConstant(name);
    }

    /** rb_mod_const_set
     *
     */
    public IRubyObject const_set(IRubyObject symbol, IRubyObject value) {
        String name = symbol.asSymbol();

        if (!IdUtil.isConstant(name)) {
            throw wrongConstantNameError(name);
        }

        return setConstant(name, value);
    }

    /** rb_mod_const_defined
     *
     */
    public RubyBoolean const_defined(IRubyObject symbol) {
        String name = symbol.asSymbol();

        if (!IdUtil.isConstant(name)) {
            throw wrongConstantNameError(name);
        }

        return getRuntime().newBoolean(getConstantAt(name) != null);
    }

    private RaiseException wrongConstantNameError(String name) {
        return getRuntime().newNameError("wrong constant name " + name, name);
    }

    private RubyArray instance_methods(IRubyObject[] args, final Visibility visibility) {
        boolean includeSuper = args.length > 0 ? args[0].isTrue() : true;
        RubyArray ary = getRuntime().newArray();
        HashMap undefinedMethods = new HashMap();

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
                    method.getVisibility().is(visibility) && undefinedMethods.get(methodName) == null) {
                    RubyString name = getRuntime().newString(methodName);

                    if (!ary.includes(name)) {
                        ary.append(name);
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
        return instance_methods(args, Visibility.PUBLIC_PROTECTED);
    }

    public RubyArray public_instance_methods(IRubyObject[] args) {
        return instance_methods(args, Visibility.PUBLIC);
    }

    public IRubyObject instance_method(IRubyObject symbol) {
        return newMethod(null, symbol.asSymbol(), false);
    }

    /** rb_class_protected_instance_methods
     *
     */
    public RubyArray protected_instance_methods(IRubyObject[] args) {
        return instance_methods(args, Visibility.PROTECTED);
    }

    /** rb_class_private_instance_methods
     *
     */
    public RubyArray private_instance_methods(IRubyObject[] args) {
        return instance_methods(args, Visibility.PRIVATE);
    }

    /** rb_mod_constants
     *
     */
    public RubyArray constants() {
        ArrayList constantNames = new ArrayList();
        RubyModule objectClass = getRuntime().getObject();

        if (getRuntime().getClass("Module") == this) {
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
    // TODO: Proper argument check (conversion?)
    public RubyModule append_features(IRubyObject module) {
        ((RubyModule) module).includeModule(this);
        return this;
    }

    /** rb_mod_extend_object
     *
     */
    public IRubyObject extend_object(IRubyObject obj) {
        obj.extendObject(this);
        return obj;
    }

    /** rb_mod_include
     *
     */
    public RubyModule include(IRubyObject[] modules) {
        ThreadContext context = getRuntime().getCurrentContext();

        for (int i = modules.length - 1; i >= 0; i--) {
            modules[i].callMethod(context, "append_features", this);
            modules[i].callMethod(context, "included", this);
        }

        return this;
    }

    public IRubyObject included(IRubyObject other) {
        return getRuntime().getNil();
    }

    public IRubyObject extended(IRubyObject other) {
        return getRuntime().getNil();
    }

    private void setVisibility(IRubyObject[] args, Visibility visibility) {
        if (getRuntime().getSafeLevel() >= 4 && !isTaint()) {
            throw getRuntime().newSecurityError("Insecure: can't change method visibility");
        }

        if (args.length == 0) {
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

    public IRubyObject method_added(IRubyObject nothing) {
        return getRuntime().getNil();
    }

    public RubyBoolean method_defined(IRubyObject symbol) {
        return isMethodBound(symbol.asSymbol(), true) ? getRuntime().getTrue() : getRuntime().getFalse();
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

    public RubyModule undef_method(IRubyObject name) {
        undef(name.asSymbol());
        return this;
    }

    public IRubyObject module_eval(IRubyObject[] args) {
        return specificEval(this, args);
    }

    public RubyModule remove_method(IRubyObject[] args) {
        for(int i=0;i<args.length;i++) {
            removeMethod(args[i].asSymbol());
        }
        return this;
    }

    public void marshalTo(MarshalStream output) throws java.io.IOException {
        output.write('m');
        output.dumpString(name().toString());
    }

    public static RubyModule unmarshalFrom(UnmarshalStream input) throws java.io.IOException {
        String name = input.unmarshalString();
        IRuby runtime = input.getRuntime();
        RubyModule result = runtime.getClassFromPath(name);
        if (result == null) {
            throw runtime.newNameError("uninitialized constant " + name, name);
        }
        input.registerLinkTarget(result);
        return result;
    }

    public SinglyLinkedList getCRef() {
        return cref;
    }

    public IRubyObject inspect() {
        return callMethod(getRuntime().getCurrentContext(), "to_s");
    }
}
