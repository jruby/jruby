/*
 * RubyModule.java - No description
 * Created on 09. Juli 2001, 21:38
 *
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina
 * Copyright (C) 2004 Thomas E Enebo
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Thomas E Enebo <enebo@acm.org>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package org.jruby;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;

import org.jruby.ast.AttrSetNode;
import org.jruby.ast.InstVarNode;
import org.jruby.ast.ZSuperNode;
import org.jruby.exceptions.ArgumentError;
import org.jruby.exceptions.NameError;
import org.jruby.exceptions.FrozenError;
import org.jruby.exceptions.SecurityError;
import org.jruby.exceptions.TypeError;
import org.jruby.internal.runtime.methods.AliasMethod;
import org.jruby.internal.runtime.methods.CacheEntry;
import org.jruby.internal.runtime.methods.CallbackMethod;
import org.jruby.internal.runtime.methods.EvaluateMethod;
import org.jruby.internal.runtime.methods.MethodMethod;
import org.jruby.internal.runtime.methods.ProcMethod;
import org.jruby.internal.runtime.methods.UndefinedMethod;
import org.jruby.internal.runtime.methods.WrapperCallable;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.callback.Callback;
import org.jruby.runtime.ICallable;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.CallType;
import org.jruby.runtime.LastCallStatus;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Iter;
import org.jruby.runtime.Frame;
import org.jruby.util.Asserts;
import org.jruby.util.IdUtil;

/**
 *
 * @author  jpetersen
 */
public class RubyModule extends RubyObject {

    private RubyClass superClass;
    public RubyModule parentModule;

    private String classId;
    private String classPath;

    private Map methods = new HashMap();

    private Map methodCache = new TreeMap();

    private RubyModule(Ruby ruby, RubyClass rubyClass) {
        this(ruby, rubyClass, null);
    }

    protected RubyModule(Ruby ruby, RubyClass rubyClass, RubyClass superClass, String name) {
        this(ruby, rubyClass, superClass);
        this.classId = name;
    }

    protected RubyModule(Ruby ruby, RubyClass rubyClass, RubyClass superClass) {
        super(ruby, rubyClass);
        this.superClass = superClass;
        this.parentModule = ruby.getRubyClass();
    }

    /** Getter for property superClass.
     * @return Value of property superClass.
     */
    public RubyClass getSuperClass() {
        return this.superClass;
    }

    /** Setter for property superClass.
     * @param superClass New value of property superClass.
     */
    private void setSuperClass(RubyClass superClass) {
        this.superClass = superClass;
    }

    public Map getMethods() {
        return this.methods;
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

    public boolean isIncluded() {
        return false;
    }

    public static void createModuleClass(RubyClass moduleClass) {
        CallbackFactory callbackFactory = moduleClass.callbackFactory();

        Callback op_eqq = callbackFactory.getMethod(RubyModule.class, "op_eqq", IRubyObject.class);
        Callback op_cmp = callbackFactory.getMethod(RubyModule.class, "op_cmp", IRubyObject.class);
        Callback op_lt = callbackFactory.getMethod(RubyModule.class, "op_lt", IRubyObject.class);
        Callback op_le = callbackFactory.getMethod(RubyModule.class, "op_le", IRubyObject.class);
        Callback op_gt = callbackFactory.getMethod(RubyModule.class, "op_gt", IRubyObject.class);
        Callback op_ge = callbackFactory.getMethod(RubyModule.class, "op_ge", IRubyObject.class);

        Callback clone = callbackFactory.getMethod(RubyModule.class, "rbClone");
        Callback dup = callbackFactory.getMethod(RubyModule.class, "dup");
        Callback to_s = callbackFactory.getMethod(RubyModule.class, "to_s");
        Callback included_modules = callbackFactory.getMethod(RubyModule.class, "included_modules");
        Callback name = callbackFactory.getMethod(RubyModule.class, "name");
        Callback ancestors = callbackFactory.getMethod(RubyModule.class, "ancestors");

        Callback attr = callbackFactory.getOptMethod(RubyModule.class, "attr", IRubyObject.class);
        Callback attr_reader = callbackFactory.getOptMethod(RubyModule.class, "attr_reader");
        Callback attr_writer = callbackFactory.getOptMethod(RubyModule.class, "attr_writer");
        Callback attr_accessor = callbackFactory.getOptMethod(RubyModule.class, "attr_accessor");

        Callback newModule = callbackFactory.getSingletonMethod(RubyModule.class, "newModule");
        Callback initialize = callbackFactory.getOptMethod(RubyModule.class, "initialize");
        Callback instance_methods = callbackFactory.getOptMethod(RubyModule.class, "instance_methods");
        Callback public_instance_methods = callbackFactory.getOptMethod(RubyModule.class, "instance_methods");
        Callback protected_instance_methods =
            callbackFactory.getOptMethod(RubyModule.class, "protected_instance_methods");
        Callback private_instance_methods = callbackFactory.getOptMethod(RubyModule.class, "private_instance_methods");

        Callback constants = callbackFactory.getMethod(RubyModule.class, "constants");
        Callback const_get = callbackFactory.getMethod(RubyModule.class, "const_get", IRubyObject.class);
        Callback const_set =
            callbackFactory.getMethod(RubyModule.class, "const_set", IRubyObject.class, IRubyObject.class);
        Callback const_defined = callbackFactory.getMethod(RubyModule.class, "const_defined", IRubyObject.class);
        Callback class_variables = callbackFactory.getMethod(RubyModule.class, "class_variables");
        Callback remove_class_variable =
            callbackFactory.getMethod(RubyModule.class, "remove_class_variable", IRubyObject.class);

        Callback append_features = callbackFactory.getMethod(RubyModule.class, "append_features", RubyModule.class);
        Callback extend_object = callbackFactory.getMethod(RubyModule.class, "extend_object", IRubyObject.class);
        Callback include = callbackFactory.getOptMethod(RubyModule.class, "include");
        Callback rbPublic = callbackFactory.getOptMethod(RubyModule.class, "rbPublic");
        Callback rbProtected = callbackFactory.getOptMethod(RubyModule.class, "rbProtected");
        Callback rbPrivate = callbackFactory.getOptMethod(RubyModule.class, "rbPrivate");
        Callback module_function = callbackFactory.getOptMethod(RubyModule.class, "module_function");

        Callback method_defined = callbackFactory.getMethod(RubyModule.class, "method_defined", IRubyObject.class);
        Callback public_class_method = callbackFactory.getOptMethod(RubyModule.class, "public_class_method");
        Callback private_class_method = callbackFactory.getOptMethod(RubyModule.class, "private_class_method");

        Callback module_eval = callbackFactory.getOptMethod(RubyModule.class, "module_eval");
        Callback remove_method = callbackFactory.getMethod(RubyModule.class, "remove_method", IRubyObject.class);
        Callback undef_method = callbackFactory.getMethod(RubyModule.class, "undef_method", IRubyObject.class);
        Callback alias_method =
            callbackFactory.getMethod(RubyModule.class, "alias_method", IRubyObject.class, IRubyObject.class);

        moduleClass.defineMethod("===", op_eqq);
        moduleClass.defineMethod("<=>", op_cmp);
        moduleClass.defineMethod("<", op_lt);
        moduleClass.defineMethod("<=", op_le);
        moduleClass.defineMethod(">", op_gt);
        moduleClass.defineMethod(">=", op_ge);

        moduleClass.defineMethod("clone", clone);
        moduleClass.defineMethod("dup", dup);
        moduleClass.defineMethod("to_s", to_s);
        moduleClass.defineMethod("included_modules", included_modules);
        moduleClass.defineMethod("name", name);
        moduleClass.defineMethod("ancestors", ancestors);

        moduleClass.definePrivateMethod("attr", attr);
        moduleClass.definePrivateMethod("attr_reader", attr_reader);
        moduleClass.definePrivateMethod("attr_writer", attr_writer);
        moduleClass.definePrivateMethod("attr_accessor", attr_accessor);

        moduleClass.defineSingletonMethod("new", newModule);

        moduleClass.defineMethod("initialize", initialize);
        moduleClass.defineMethod("instance_methods", instance_methods);
        moduleClass.defineMethod("public_instance_methods", public_instance_methods);
        moduleClass.defineMethod("protected_instance_methods", protected_instance_methods);
        moduleClass.defineMethod("private_instance_methods", private_instance_methods);

        moduleClass.defineMethod("constants", constants);
        moduleClass.defineMethod("const_get", const_get);
        moduleClass.defineMethod("const_set", const_set);
        moduleClass.defineMethod("const_defined?", const_defined);
        moduleClass.definePrivateMethod("method_added", callbackFactory.getNilMethod(1));
        moduleClass.defineMethod("class_variables", class_variables);
        moduleClass.definePrivateMethod("remove_class_variable", remove_class_variable);

        moduleClass.definePrivateMethod("append_features", append_features);
        moduleClass.definePrivateMethod("extend_object", extend_object);
        moduleClass.definePrivateMethod("include", include);
        moduleClass.definePrivateMethod("public", rbPublic);
        moduleClass.definePrivateMethod("protected", rbProtected);
        moduleClass.definePrivateMethod("private", rbPrivate);
        moduleClass.definePrivateMethod("module_function", module_function);

        moduleClass.defineMethod("method_defined?", method_defined);
        moduleClass.defineMethod("public_class_method", public_class_method);
        moduleClass.defineMethod("private_class_method", private_class_method);

        moduleClass.defineMethod("module_eval", module_eval);
        moduleClass.defineMethod("class_eval", module_eval);

        moduleClass.definePrivateMethod("remove_method", remove_method);
        moduleClass.definePrivateMethod("undef_method", undef_method);
        moduleClass.definePrivateMethod("alias_method", alias_method);
        moduleClass.definePrivateMethod("define_method", callbackFactory.getOptMethod(RubyModule.class, "define_method"));

        moduleClass.defineMethod("instance_method", callbackFactory.getMethod(RubyModule.class, "instance_method", IRubyObject.class));

        moduleClass.defineMethod("const_missing", callbackFactory.getMethod(RubyModule.class, "const_missing", IRubyObject.class));

        moduleClass.defineSingletonMethod("nesting", callbackFactory.getSingletonMethod(RubyModule.class, "nesting"));
    }

    /** classname
     *
     */
    public String getClassname() {
        RubyModule module = this;
        while (module.isIncluded() || module.isSingleton()) {
            module = module.getSuperClass();
        }

        if (classPath == null && classId != null) {
            classPath = classId;
            classId = null;
        }

        if (classPath == null) {
            return module.findClassPath();
        }

        return classPath;
    }

    /** findclasspath
     *
     */
    private String findClassPath() {
        RubyModule current = this;
        ArrayList path = new ArrayList();
        while (current != runtime.getTopSelf().getType()) {
            Asserts.notNull(current);
            path.add(current);
            current = current.parentModule;
        }
        StringBuffer result = new StringBuffer();
        for (int i = path.size() - 1; i >= 0; i--) {
            result.append(((RubyModule) path.get(i)).classId);
            if (i > 0) {
                result.append("::");
            }
        }
        return result.toString();
    }

    /** include_class_new
     *
     */
    public IncludedModuleWrapper newIncludeClass(RubyClass superClass) {
        return new IncludedModuleWrapper(getRuntime(), superClass, this);
    }

    /** rb_set_class_path
     *
     */
    public void setClassPath(RubyModule outer, String name) {
        if (outer == getRuntime().getClasses().getObjectClass()) {
            classPath = name;
        } else {
            classPath = outer.getClassPath();
            classPath += "::" + name;
        }
    }

    /** rb_class_path
     *
     */
    public String getClassPath() {
        String path = getClassname();

        if (path != null) {
            return path;
        }

        return "<" + (isClass() ? "Class" : "Module") + " 01x" + 
        	Integer.toHexString(System.identityHashCode(this)) + ">";
        // 0 = pointer
    }
    
    private RubyModule getModuleWithInstanceVar(String name) {
        for (RubyModule tmp = this; tmp != null; tmp = tmp.getSuperClass()) {
            if (tmp.hasInstanceVariable(name)) {
                return tmp;
            }
        }
        return null;
    }

    /** rb_cvar_set
     *
     */
    public void setClassVar(String name, IRubyObject value) {
        RubyModule module = getModuleWithInstanceVar(name);
        
        if (module != null) {
            if (module.isTaint() && getRuntime().getSafeLevel() >= 4) {
                throw new SecurityError(getRuntime(), "Insecure: can't modify class variable");
            }
            module.setInstanceVariable(name, value);
            return;
        }

        // If we cannot find the class var, then create it in the super class.
        if (isTaint() && getRuntime().getSafeLevel() >= 4) {
            throw new SecurityError(getRuntime(), "Insecure: can't modify class variable");
        }
        setInstanceVariable(name, value);
    }

    /** rb_cvar_declare
     *
     */
    public void declareClassVar(String name, IRubyObject value) {
        RubyModule module = getModuleWithInstanceVar(name);
        
        if (module != null) {
            if (module.isTaint() && getRuntime().getSafeLevel() >= 4) {
                throw new SecurityError(getRuntime(), "Insecure: can't modify class variable");
            }
            module.setInstanceVariable(name, value);
        }
        setAv(name, value, false);
    }

    /** rb_cvar_get
     *
     */
    public IRubyObject getClassVar(String name) {
        RubyModule module = getModuleWithInstanceVar(name);
        
        if (module != null) {
            return module.getInstanceVariable(name);
        }
        throw new NameError(getRuntime(), "uninitialized class variable " + name + " in " + toName());
    }

    /** rb_cvar_defined
     *
     */
    public boolean isClassVarDefined(String name) {
        return getModuleWithInstanceVar(name) != null;
    }

    public void setConstant(String name, IRubyObject value) {
        setAv(name, value, true);
    }

    public IRubyObject getConstant(String name) {
        boolean mod_retry = false;
        RubyModule module = this;

        while (true) {
            while (module != null) {
                if (!module.getInstanceVariable(name).isNil()) {
                    return module.getInstanceVariable(name);
                }
                if (module == getRuntime().getClasses().getObjectClass() && getRuntime().getTopConstant(name) != null) {
                    return getRuntime().getTopConstant(name);
                }
                module = module.getSuperClass();
            }
            if (!mod_retry && isModule()) {
                mod_retry = true;
                module = getRuntime().getClasses().getObjectClass();
                continue;
            }
            break;
        }
        return callMethod("const_missing", RubySymbol.newSymbol(runtime, name));
    }

    public IRubyObject const_missing(IRubyObject name) {
        /* Uninitialized constant */
        if (this != getRuntime().getClasses().getObjectClass()) {
            throw new NameError(getRuntime(), "uninitialized constant " + name.asSymbol() + " at " + getClassPath());
        } else {
            throw new NameError(getRuntime(), "uninitialized constant " + name.asSymbol());
        }
    }

    /** Include a new module in this module or class.
     *
     * MRI: rb_include_module
     *
     * Updated to Ruby 1.6.7.
     *
     */
    public void includeModule(IRubyObject arg) {
        testFrozen();
        if (!isTaint()) {
            runtime.secure(4);
        }
        if (arg == null || arg == this) {
            return;
        }

        if (!(arg instanceof RubyModule)) {
            throw new TypeError(runtime, "Wrong argument type " + arg.getMetaClass().toName() + " (expected Module).");
        }

        RubyModule module = (RubyModule) arg;

        boolean changed = false;

        RubyModule type = this;

        addModule : while (module != null) {
            if (getMethods() == module.getMethods()) {
                throw new ArgumentError(runtime, "Cyclic include detected.");
            }
            // ignore if module is already included in one of the super classes.
            for (RubyClass p = getSuperClass(); p != null; p = p.getSuperClass()) {
                if (p.isIncluded() && p.getMethods() == module.getMethods()) {
                    type = p;
                    module = module.getSuperClass();
                    continue addModule;
                }
            }
            type.setSuperClass(module.newIncludeClass(type.getSuperClass()));
            type = type.getSuperClass();
            changed = true;

            module = module.getSuperClass();
        }

        if (changed) {
            clearMethodCache();
        }
    }

    /** mod_av_set
     *
     */
    private void setAv(String name, IRubyObject value, boolean constant) {
        String dest = constant ? "constant" : "class variable";

        if (!isTaint() && getRuntime().getSafeLevel() >= 4) {
            throw new SecurityError(getRuntime(), "Insecure: can't set " + dest);
        }
        if (isFrozen()) {
            throw new FrozenError(getRuntime(), "class/module");
        }
        if (constant && (!getInstanceVariable(name).isNil())) {
            //getRuby().warn("already initialized " + dest + " " + name);
        }

        setInstanceVariable(name, value);
    }

    /** rb_add_method
     *
     */
    public void addMethod(String name, ICallable method) {
        if (this == getRuntime().getClasses().getObjectClass()) {
            getRuntime().secure(4);
        }

        if (getRuntime().getSafeLevel() >= 4 && !isTaint()) {
            throw new SecurityError(getRuntime(), "Insecure: can't define method");
        }
        if (isFrozen()) {
            throw new FrozenError(getRuntime(), "class/module");
        }
        getMethods().put(name, method);
        clearMethodCache();
    }

    public void defineMethod(String name, Callback method) {
        Visibility visibility = name.equals("initialize") ? Visibility.PRIVATE : Visibility.PUBLIC;

        addMethod(name, new CallbackMethod(method, visibility));
    }

    public void definePrivateMethod(String name, Callback method) {
        addMethod(name, new CallbackMethod(method, Visibility.PRIVATE));
    }

    public void undefineMethod(String name) {
        addMethod(name, UndefinedMethod.getInstance());
    }

    /** rb_frozen_class_p
     *
     */
    protected void testFrozen() {
        if (isFrozen()) {
            throw new FrozenError(getRuntime(), "module");
        }
    }

    /** rb_undef
     *
     */
    public void undef(String name) {
        Ruby ruby = getRuntime();
        if (this == ruby.getClasses().getObjectClass()) {
            ruby.secure(4);
        }
        if (ruby.getSafeLevel() >= 4 && !isTaint()) {
            throw new SecurityException("Insecure: can't undef");
        }
        testFrozen();
        if (name.equals("__id__") || name.equals("__send__")) {
            /*rb_warn("undefining `%s' may cause serious problem",
                     rb_id2name( id ) );*/
        }
        ICallable method = searchMethod(name);
        if (method.isUndefined()) {
            String s0 = " class";
            RubyModule c = this;

            if (c.isSingleton()) {
                IRubyObject obj = getInstanceVariable("__attached__");

                if (obj instanceof RubyModule) {
                    c = (RubyModule) obj;
                    s0 = "";
                }
            } else if (c.isModule()) {
                s0 = " module";
            }

            throw new NameError(ruby, "Undefined method " + name + " for" + s0 + " '" + c.toName() + "'");
        }
        addMethod(name, UndefinedMethod.getInstance());
    }

    /** rb_define_module_function
     *
     */
    public void defineModuleFunction(String name, Callback method) {
        definePrivateMethod(name, method);
        defineSingletonMethod(name, method);
    }

    /** rb_define_alias
     *
     */
    public void defineAlias(String newName, String oldName) {
        aliasMethod(newName, oldName);
    }

    /** rb_const_defined
     *
     */
    public boolean isConstantDefined(String name) {
        for (RubyModule tmp = this; tmp != null; tmp = tmp.getSuperClass()) {
            if (!tmp.getInstanceVariable(name).isNil()) {
                return true;
            }
        }

        if (isModule()) {
            return getRuntime().getClasses().getObjectClass().isConstantDefined(name);
        }

        if (getRuntime().isClassDefined(name)) {
            return true;
        }

        return runtime.getLoadService().isAutoloadDefined(name);
    }

    /**
     *
     * MRI: rb_const_defined_at
     *
     */
    public boolean isConstantDefinedAt(String name) {
        if (!getInstanceVariable(name).isNil()) {
            return true;
        } else if (this == runtime.getClasses().getObjectClass()) {
            return isConstantDefined(name);
        }
        return false;
    }

    /** search_method
     *
     */
    public ICallable searchMethod(String name) {
        ICallable method = (ICallable) getMethods().get(name);
        if (method == null) {
            if (getSuperClass() != null) {
                return getSuperClass().searchMethod(name);
            } else {
                return UndefinedMethod.getInstance();
            }
        } else {
            method.setImplementationClass(this);
            return method;
        }
    }

    public Visibility getMethodVisibility(String name) {
        CacheEntry entry = getMethodBodyCached(name);
        return entry.getVisibility();
    }

    protected CacheEntry getMethodBodyCached(String name) {
        CacheEntry result = (CacheEntry) methodCache.get(name);
        if (result != null) {
            return result;
        }
        name = name.intern();
        ICallable method = searchMethod(name);
        if (method.isUndefined()) {
            CacheEntry undefinedEntry = CacheEntry.createUndefined(name, this);
            methodCache.put(name, undefinedEntry);
            return undefinedEntry;
        }
        result = new CacheEntry(name, this);
        method.initializeCacheEntry(result);
        methodCache.put(name, result);
        return result;
    }

    public static void clearMethodCache(Ruby runtime) {
        Iterator iter = runtime.getClasses().getClassMap().values().iterator();
        while (iter.hasNext()) {
            ((RubyModule) iter.next()).methodCache.clear();
        }
    }

    private void clearMethodCache() {
        clearMethodCache(getRuntime());
    }

    /** rb_call
     *
     */
    public final IRubyObject call(IRubyObject recv, String name, IRubyObject[] args, CallType callType) {
        if (args == null) {
            args = IRubyObject.NULL_ARRAY;
        }
        CacheEntry entry = getMethodBodyCached(name);

        final LastCallStatus lastCallStatus = runtime.getLastCallStatus();
        if (! entry.isDefined()) {
            callType.registerCallStatus(lastCallStatus, name);
            return callMethodMissing(recv, name, args);
        }

        RubyModule klass = entry.getOrigin();
        name = entry.getOriginalName();
        ICallable method = entry.getMethod();

        if (!name.equals("method_missing")) {
            if (method.getVisibility().isPrivate() && callType.isNormal()) {
                lastCallStatus.setPrivate();
                return callMethodMissing(recv, name, args);
            } else if (method.getVisibility().isProtected()) {
                RubyModule defined = klass;
                while (defined.isIncluded()) {
                    defined = defined.getMetaClass();
                }
                if (!runtime.getCurrentFrame().getSelf().isKindOf(defined)) {
                    lastCallStatus.setProtected();
                    return callMethodMissing(recv, name, args);
                }
            }
        }

        return klass.call0(recv, name, args, method, false);
    }

    private IRubyObject callMethodMissing(IRubyObject receiver, String name, IRubyObject[] args) {
        if (name == "method_missing") {
            runtime.getFrameStack().push();
            try {
                return receiver.method_missing(args);
            } finally {
                runtime.getFrameStack().pop();
            }
        }

        IRubyObject[] newArgs = new IRubyObject[args.length + 1];
        System.arraycopy(args, 0, newArgs, 1, args.length);
        newArgs[0] = RubySymbol.newSymbol(runtime, name);

        return receiver.callMethod("method_missing", newArgs);
    }

    /** rb_call0
     *
     */
    public final IRubyObject call0(
        IRubyObject recv,
        String name,
        IRubyObject[] args,
        ICallable method,
        boolean noSuper) {
        ThreadContext context = runtime.getCurrentContext();
        context.getIterStack().push(context.getCurrentIter().isPre() ? Iter.ITER_CUR : Iter.ITER_NOT);

        context.getFrameStack().push();
        context.getCurrentFrame().setLastFunc(name);
        context.getCurrentFrame().setLastClass(noSuper ? null : this);
        context.getCurrentFrame().setSelf(recv);
        context.getCurrentFrame().setArgs(args);

        try {
            return method.call(runtime, recv, name, args, noSuper);
        } finally {
            context.getFrameStack().pop();
            context.getIterStack().pop();
        }
    }

    /** rb_alias
     *
     */
    public void aliasMethod(String name, String oldName) {
        testFrozen();
        if (oldName.equals(name)) {
            return;
        }
        if (this == getRuntime().getClasses().getObjectClass()) {
            getRuntime().secure(4);
        }
        ICallable method = searchMethod(oldName);
        if (method.isUndefined()) {
            if (isModule()) {
                method = getRuntime().getClasses().getObjectClass().searchMethod(oldName);
            }
            if (method.isUndefined()) {
                throw new NameError(runtime,
                                    "undefined method '" + name + "' for " +
                                    (isModule() ? "module" : "class") + " '" +
                                    toName() + "'");
            }
        }
        getMethods().put(name, new AliasMethod(method, oldName));
        clearMethodCache();
    }

    /** remove_method
     *
     */
    public void removeMethod(String name) {
        if (this == getRuntime().getClasses().getObjectClass()) {
            getRuntime().secure(4);
        }
        if (getRuntime().getSafeLevel() >= 4 && !isTaint()) {
            throw new SecurityError(getRuntime(), "Insecure: can't remove method");
        }
        if (isFrozen()) {
            throw new FrozenError(getRuntime(), "class/module");
        }
        if (getMethods().remove(name) == null) {
            throw new NameError(getRuntime(), "method '" + name + "' not defined in " + toName());
        }

        clearMethodCache();
    }

    /** rb_define_class_under
     *
     */
    public RubyClass defineClassUnder(String name, RubyClass superClass) {
        if (isConstantDefinedAt(name)) {
            IRubyObject type = getConstant(name);
            if (!(type instanceof RubyClass)) {
                throw new TypeError(runtime, name + " is not a class.");
            } else if (((RubyClass) type).getSuperClass().getRealClass() != superClass) {
                throw new NameError(runtime, name + " is already defined.");
            } else {
                return (RubyClass) type;
            }
        } else {
            RubyClass newClass = getRuntime().defineClass(name, superClass);

            newClass.setClassPath(this, name);
            newClass.inheritedBy(superClass);
            setConstant(name, newClass);

            return newClass;
        }
    }

    /** rb_class2name
     *
     */
    public String toName() {
        // REMOVE +++ in 1.7
        if (this == getRuntime().getClass("NilClass")) {
            return "nil";
        } else if (this == getRuntime().getClass("TrueClass")) {
            return "true";
        } else if (this == getRuntime().getClass("FalseClass")) {
            return "false";
        }
        // REMOVE ---
        return getClassPath();
    }

    /** rb_define_const
     *
     */
    public void defineConstant(String name, IRubyObject value) {
        Asserts.notNull(value);

        if (this == getRuntime().getClasses().getClassClass()) {
            getRuntime().secure(4);
        }

        if (!IdUtil.isConstant(name)) {
            throw new NameError(getRuntime(), "bad constant name " + name);
        }

        setConstant(name, value);
    }

    /** rb_mod_remove_cvar
     *
     */
    public IRubyObject removeCvar(IRubyObject name) { // Wrong Parameter ?
        if (!IdUtil.isClassVariable(name.asSymbol())) {
            throw new NameError(getRuntime(), "wrong class variable name " + name.asSymbol());
        }

        if (!isTaint() && getRuntime().getSafeLevel() >= 4) {
            throw new SecurityError(getRuntime(), "Insecure: can't remove class variable");
        }

        if (isFrozen()) {
            throw new FrozenError(getRuntime(), "class/module");
        }

        IRubyObject value = removeInstanceVariable(name.asSymbol());

        if (value != null) {
            return value;
        }

        if (isClassVarDefined(name.asSymbol())) {
            throw new NameError(getRuntime(), "cannot remove " + name.asSymbol() + " for " + toName());
        }

        throw new NameError(getRuntime(), "class variable " + name.asSymbol() + " not defined for " + toName());
    }

    private void addAccessor(String name, boolean readable, boolean writeable) {
        Visibility attributeScope = getRuntime().getCurrentVisibility();
        if (attributeScope.isPrivate()) {
            //FIXME warning
        } else if (attributeScope.isModuleFunction()) {
            attributeScope = Visibility.PRIVATE;
            // FIXME warning
        }
        String variableName = "@" + name;
        if (readable) {
            addMethod(name, new EvaluateMethod(new InstVarNode(getRuntime().getPosition(), variableName), attributeScope));
            callMethod("method_added", RubySymbol.newSymbol(getRuntime(), name));
        }
        if (writeable) {
            name = name + "=";
            addMethod(name, new EvaluateMethod(new AttrSetNode(getRuntime().getPosition(), variableName), attributeScope));
            callMethod("method_added", RubySymbol.newSymbol(getRuntime(), name));
        }
    }

    /** set_method_visibility
     *
     */
    public void setMethodVisibility(IRubyObject[] methods, Visibility visibility) {
        if (getRuntime().getSafeLevel() >= 4 && !isTaint()) {
            throw new SecurityError(getRuntime(), "Insecure: can't change method visibility");
        }

        for (int i = 0; i < methods.length; i++) {
            exportMethod(methods[i].asSymbol(), visibility);
        }
    }

    /** rb_export_method
     *
     */
    public void exportMethod(String name, Visibility visibility) {
        if (this == getRuntime().getClasses().getObjectClass()) {
            getRuntime().secure(4);
        }

        ICallable method = searchMethod(name);

        if (method.isUndefined() && isModule()) {
            method = getRuntime().getClasses().getObjectClass().searchMethod(name);
        }

        if (method.isUndefined()) {
            throw new NameError(
                runtime,
                "undefined method '" + name + "' for " + (isModule() ? "module" : "class") + " '" + toName() + "'");
        }

        if (method.getVisibility() != visibility) {
            if (this == method.getImplementationClass()) {
                method.setVisibility(visibility);
            } else {
                ICallable superCall = new EvaluateMethod(new ZSuperNode(getRuntime().getPosition()), visibility);
                addMethod(name, superCall);
            }
        }
    }

    /**
     * MRI: rb_method_boundp
     *
     */
    public boolean isMethodBound(String name, boolean checkVisibility) {
        CacheEntry entry = (CacheEntry) methodCache.get(name);
        if (entry == null) {
            entry = getMethodBodyCached(name);
        }
        if (entry.isDefined()) {
            if (checkVisibility && entry.getVisibility().isPrivate()) {
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    public boolean isMethodDefined(String name) {
        return isMethodBound(name, true);
    }

    public IRubyObject newMethod(IRubyObject receiver, String name, boolean bound) {
        CacheEntry ent = getMethodBodyCached(name);
        if (! ent.isDefined()) {
            // printUndef();
            return getRuntime().getNil();
        }

        while (ent.getMethod() instanceof EvaluateMethod
            && ((EvaluateMethod) ent.getMethod()).getNode() instanceof ZSuperNode) {
            ent = ent.getOrigin().getSuperClass().getMethodBodyCached(ent.getOriginalName());
            if (! ent.isDefined()) {
                // printUndef();
                return getRuntime().getNil();
            }
        }

        Method method = null;
        if (bound) {
            method = Method.newMethod(ent.getOrigin(), ent.getOriginalName(), this, name, ent.getMethod(), receiver);
        } else {
            method = UnboundMethod.newUnboundMethod(ent.getOrigin(), ent.getOriginalName(), this, name, ent.getMethod());
        }
        method.infectBy(this);

        return method;
    }

    public IRubyObject define_method(IRubyObject[] args) {
        if (args.length < 1 || args.length > 2) {
            throw new ArgumentError(runtime, "wrong # of arguments(" + args.length + " for 1)");
        }
        String name = args[0].asSymbol();

        IRubyObject body;
        if (args.length == 1) {
            body = RubyProc.newProc(runtime);
        } else {
            if (!(args[0].isKindOf(runtime.getClass("Method")) ||
                args[0].isKindOf(runtime.getClass("Proc")))) {
                throw new TypeError(runtime, "wrong argument type " + args[0].getType().toName() + " (expected Proc/Method)");
            }
            body = args[0];
        }


        Visibility visibility = runtime.getCurrentVisibility();
        if (visibility.isModuleFunction()) {
            visibility = Visibility.PRIVATE;
        }

        ICallable newMethod = null;
        if (body instanceof RubyProc) {
            newMethod = new ProcMethod((RubyProc)body, visibility);
        } else {
            newMethod = new MethodMethod(((Method)body).unbind(), visibility);
        }

        addMethod(name, newMethod);

        RubySymbol symbol = RubySymbol.newSymbol(runtime, name);
        if (runtime.getCurrentVisibility().isModuleFunction()) {
            getSingletonClass().addMethod(name, new WrapperCallable(newMethod, Visibility.PUBLIC));
            callMethod("singleton_method_added", symbol);
        }

        methodAdded(symbol);

        return body;
    }

    public void methodAdded(RubySymbol symbol) {
        callMethod("method_added", symbol);
    }

    public IRubyObject executeUnder(Callback method, IRubyObject[] args) {
        ThreadContext threadContext = runtime.getCurrentContext();

        threadContext.pushClass(this);

        Frame frame = threadContext.getCurrentFrame();
        threadContext.getFrameStack().push();
        threadContext.getCurrentFrame().setLastFunc(frame.getLastFunc());
        threadContext.getCurrentFrame().setLastClass(frame.getLastClass());
        threadContext.getCurrentFrame().setArgs(frame.getArgs());

        try {
            return method.execute(this, args);
        } finally {
            threadContext.getFrameStack().pop();
            threadContext.popClass();
        }
    }

    // Methods of the Module Class (rb_mod_*):

    /** rb_mod_new
     *
     */
    public static RubyModule newModule(Ruby ruby) {
        RubyModule newModule = new RubyModule(ruby, ruby.getClass("Module"));
        return newModule;
    }

    public static RubyModule newModule(Ruby ruby, String name) {
        RubyModule result = newModule(ruby);
        result.classId = name;
        return result;
    }

    /** rb_mod_name
     *
     */
    public RubyString name() {
        String path = getClassname();
        if (path != null) {
            return RubyString.newString(runtime, path);
        }
        return RubyString.newString(runtime, "");
    }

    /** rb_mod_class_variables
     *
     */
    public RubyArray class_variables() {
        RubyArray ary = RubyArray.newArray(getRuntime());

        RubyModule rbModule = this;

        while (rbModule != null) {
            Iterator iter = rbModule.instanceVariableNames();
            while (iter.hasNext()) {
                String id = (String) iter.next();
                if (IdUtil.isClassVariable(id)) {
                    RubyString kval = RubyString.newString(getRuntime(), id);
                    if (!ary.includes(kval)) {
                        ary.append(kval);
                    }
                }
            }
            rbModule = rbModule.getSuperClass();
        }
        return ary;
    }

    /** rb_mod_clone
     *
     */
    public IRubyObject rbClone() {
        RubyModule clone = (RubyModule) super.rbClone();
        Map cloneMethods = clone.getMethods();

        if (getMethods() != null) {
            Iterator iter = getMethods().entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                Object key = entry.getKey();
                ICallable value = (ICallable) entry.getValue();
                cloneMethods.put(key, value);
            }
        }
        return clone;
    }

    /** rb_mod_dup
     *
     */
    public IRubyObject dup() {
        RubyModule dup = (RubyModule) rbClone();
        dup.setMetaClass(getMetaClass());

        // +++ jpetersen
        // dup.setSingleton(isSingleton());
        // --- jpetersen

        return dup;
    }

    /** rb_mod_included_modules
     *
     */
    public RubyArray included_modules() {
        RubyArray ary = RubyArray.newArray(getRuntime());

        for (RubyModule p = getSuperClass(); p != null; p = p.getSuperClass()) {
            if (p.isIncluded()) {
                ary.append(((IncludedModuleWrapper) p).getDelegate());
            }
        }

        return ary;
    }

    /** rb_mod_ancestors
     *
     */
    public RubyArray ancestors() {
        RubyArray ary = RubyArray.newArray(getRuntime());

        for (RubyModule p = this; p != null; p = p.getSuperClass()) {
            if (p.isSingleton()) {
                continue;
            }

            if (p.isIncluded()) {
                ary.append(((IncludedModuleWrapper) p).getDelegate());
            } else {
                ary.append(p);
            }
        }

        return ary;
    }

    /** rb_mod_to_s
     *
     */
    public RubyString to_s() {
        return RubyString.newString(runtime, getClassPath());
    }

    /** rb_mod_eqq
     *
     */
    public RubyBoolean op_eqq(IRubyObject obj) {
        return RubyBoolean.newBoolean(runtime, obj.isKindOf(this));
    }

    /** rb_mod_le
     *
     */
    public RubyBoolean op_le(IRubyObject obj) {
        if (!(obj instanceof RubyModule)) {
            throw new TypeError(getRuntime(), "compared with non class/module");
        }

        RubyModule mod = this;
        while (mod != null) {
            if (mod.getMethods() == ((RubyModule) obj).getMethods()) {
                return getRuntime().getTrue();
            }
            mod = mod.getSuperClass();
        }

        return getRuntime().getFalse();
    }

    /** rb_mod_lt
     *
     */
    public RubyBoolean op_lt(IRubyObject obj) {
        if (obj == this) {
            return getRuntime().getFalse();
        }
        return op_le(obj);
    }

    /** rb_mod_ge
     *
     */
    public RubyBoolean op_ge(IRubyObject obj) {
        if (!(obj instanceof RubyModule)) {
            throw new TypeError(getRuntime(), "compared with non class/module");
        }

        return ((RubyModule) obj).op_le(this);
    }

    /** rb_mod_gt
     *
     */
    public RubyBoolean op_gt(IRubyObject obj) {
        if (this == obj) {
            return getRuntime().getFalse();
        }
        return op_ge(obj);
    }

    /** rb_mod_cmp
     *
     */
    public RubyFixnum op_cmp(IRubyObject obj) {
        if (this == obj) {
            return RubyFixnum.newFixnum(getRuntime(), 0);
        }

        if (!(obj instanceof RubyModule)) {
            throw new TypeError(
                getRuntime(),
                "<=> requires Class or Module (" + getMetaClass().toName() + " given)");
        }

        if (op_le(obj).isTrue()) {
            return RubyFixnum.newFixnum(getRuntime(), -1);
        }

        return RubyFixnum.newFixnum(getRuntime(), 1);
    }

    /** rb_mod_initialize
     *
     */
    public IRubyObject initialize(IRubyObject[] args) {
        return getRuntime().getNil();
    }

    public static RubyModule newModule(IRubyObject recv) {
        RubyModule mod = RubyModule.newModule(recv.getRuntime());
        mod.setMetaClass((RubyClass) recv);
        recv.getRuntime().getClasses().getModuleClass().callInit(null);
        return mod;
    }

    /** Return an array of nested modules or classes.
     *
     * rb_mod_nesting
     *
     */
    public static RubyArray nesting(IRubyObject recv) {
        return recv.getRuntime().getCurrentContext().moduleNesting();
    }

    /** rb_mod_attr
     *
     */
    public IRubyObject attr(IRubyObject symbol, IRubyObject[] args) {
        boolean writeable = false;
        if (args.length > 0) {
            writeable = args[0].isTrue();
        }

        addAccessor(symbol.asSymbol(), true, writeable);

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
            throw new NameError(getRuntime(), "wrong constant name " + name);
        }

        return getConstant(name);
    }

    /** rb_mod_const_set
     *
     */
    public IRubyObject const_set(IRubyObject symbol, IRubyObject value) {
        String name = symbol.asSymbol();

        if (!IdUtil.isConstant(name)) {
            throw new NameError(getRuntime(), "wrong constant name " + name);
        }

        setConstant(name, value);

        return value;
    }

    /** rb_mod_const_defined
     *
     */
    public RubyBoolean const_defined(IRubyObject symbol) {
        String name = symbol.asSymbol();

        if (!IdUtil.isConstant(name)) {
            throw new NameError(getRuntime(), "wrong constant name " + name);
        }

        return RubyBoolean.newBoolean(getRuntime(), isConstantDefined(name));
    }

    private RubyArray instance_methods(IRubyObject[] args, final Visibility visibility) {
        boolean includeSuper = false;

        if (args.length > 0) {
            includeSuper = args[0].isTrue();
        }

        RubyArray ary = RubyArray.newArray(getRuntime());

        for (RubyModule klass = this; klass != null; klass = klass.getSuperClass()) {
            Iterator iter = klass.getMethods().entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                String id = (String) entry.getKey();
                ICallable method = (ICallable) entry.getValue();

                if (method.getVisibility() == visibility) {
                    RubyString name = RubyString.newString(getRuntime(), id);

                    if (!ary.includes(name)) {
                        if (method == null) {
                            ary.append(getRuntime().getNil());
                        }
                        ary.append(name);
                    }
                } else if (
                        method instanceof EvaluateMethod && ((EvaluateMethod) method).getNode() instanceof ZSuperNode) {
                    ary.append(getRuntime().getNil());
                    ary.append(RubyString.newString(getRuntime(), id));
                }
            }
            if (!includeSuper) {
                break;
            }
        }

        // What on earth is the above code doing when it inserts nil:s in front of
        // some values in the array!? Very naughty! -- Anders

        Iterator iter = ary.getList().iterator();
        while (iter.hasNext()) {
            if (((IRubyObject) iter.next()).isNil()) {
                iter.remove();
                iter.next();
            }
        }

        return ary;
    }

    /** rb_class_instance_methods
     *
     */
    public RubyArray instance_methods(IRubyObject[] args) {
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
        Iterator iter = getRuntime().getClasses().nameIterator();
        while (iter.hasNext()) {
            String name = (String) iter.next();
            if (IdUtil.isConstant(name)) {
                constantNames.add(RubyString.newString(getRuntime(), name));
            }
        }
        return RubyArray.newArray(getRuntime(), constantNames);
    }

    /** rb_mod_remove_cvar
     *
     */
    public IRubyObject remove_class_variable(IRubyObject name) {
        String id = name.asSymbol();

        if (!IdUtil.isClassVariable(id)) {
            throw new NameError(getRuntime(), "wrong class variable name " + id);
        }
        if (!isTaint() && getRuntime().getSafeLevel() >= 4) {
            throw new SecurityError(getRuntime(), "Insecure: can't remove class variable");
        }
        if (isFrozen()) {
            throw new FrozenError(getRuntime(), "class/module");
        }

        if (hasInstanceVariable(id)) {
            return removeInstanceVariable(id);
        }

        if (isClassVarDefined(id)) {
            throw new NameError(getRuntime(), "cannot remove " + id + " for " + toName());
        }
        throw new NameError(getRuntime(), "class variable " + id + " not defined for " + toName());
    }

    /** rb_mod_append_features
     *
     */
    public RubyModule append_features(RubyModule module) {
        module.includeModule(this);
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
        for (int i = 0; i < modules.length; i++) {
            modules[i].callMethod("append_features", this);
        }

        return this;
    }

    /** rb_mod_public
     *
     */
    public RubyModule rbPublic(IRubyObject[] args) {
        if (getRuntime().getSafeLevel() >= 4 && !isTaint()) {
            throw new SecurityError(getRuntime(), "Insecure: can't change method visibility");
        }

        if (args.length == 0) {
            getRuntime().setCurrentVisibility(Visibility.PUBLIC);
        } else {
            setMethodVisibility(args, Visibility.PUBLIC);
        }

        return this;
    }

    /** rb_mod_protected
     *
     */
    public RubyModule rbProtected(IRubyObject[] args) {
        if (getRuntime().getSafeLevel() >= 4 && !isTaint()) {
            throw new SecurityError(getRuntime(), "Insecure: can't change method visibility");
        }

        if (args.length == 0) {
            getRuntime().setCurrentVisibility(Visibility.PROTECTED);
        } else {
            setMethodVisibility(args, Visibility.PROTECTED);
        }
        return this;
    }

    /** rb_mod_private
     *
     */
    public RubyModule rbPrivate(IRubyObject[] args) {
        if (getRuntime().getSafeLevel() >= 4 && !isTaint()) {
            throw new SecurityError(getRuntime(), "Insecure: can't change method visibility");
        }

        if (args.length == 0) {
            getRuntime().setCurrentVisibility(Visibility.PRIVATE);
        } else {
            setMethodVisibility(args, Visibility.PRIVATE);
        }
        return this;
    }

    /** rb_mod_modfunc
     *
     */
    public RubyModule module_function(IRubyObject[] args) {
        if (getRuntime().getSafeLevel() >= 4 && !isTaint()) {
            throw new SecurityError(getRuntime(), "Insecure: can't change method visibility");
        }

        if (args.length == 0) {
            getRuntime().setCurrentVisibility(Visibility.MODULE_FUNCTION);
        } else {
            setMethodVisibility(args, Visibility.PRIVATE);

            for (int i = 0; i < args.length; i++) {
                String name = args[i].asSymbol();
                ICallable method = searchMethod(name);
                Asserts.isTrue(!method.isUndefined(), "undefined method '" + name + "'");
                getSingletonClass().addMethod(name, new WrapperCallable(method, Visibility.PUBLIC));
                callMethod("singleton_method_added", RubySymbol.newSymbol(getRuntime(), name));
            }
        }
        return this;
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
        aliasMethod(newId.asSymbol(), oldId.asSymbol());
        return this;
    }

    public RubyModule undef_method(IRubyObject name) {
        undef(name.asSymbol());
        return this;
    }

    public IRubyObject module_eval(IRubyObject[] args) {
        return specificEval(this, args);
    }

    public RubyModule remove_method(IRubyObject name) {
        removeMethod(name.asSymbol());
        return this;
    }

    public void marshalTo(MarshalStream output) throws java.io.IOException {
        output.write('m');
        output.dumpString(name().toString());
    }

    public static RubyModule unmarshalFrom(UnmarshalStream input) throws java.io.IOException {
        String name = input.unmarshalString();
        Ruby ruby = input.getRuntime();
        RubyModule result = ruby.getClasses().getClassFromPath(name);
        if (result == null) {
            throw new NameError(ruby, "uninitialized constant " + name);
        }
        input.registerLinkTarget(result);
        return result;
    }

    public void setName(String name) {
        this.classId = name;
    }

}
