/*
 * RubyModule.java - No description
 * Created on 09. Juli 2001, 21:38
 *
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
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
import org.jruby.runtime.Callback;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ICallable;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.CallType;
import org.jruby.runtime.LastCallStatus;
import org.jruby.runtime.Frame;
import org.jruby.runtime.Namespace;
import org.jruby.runtime.Iter;
import org.jruby.util.Asserts;
import org.jruby.util.IdUtil;
import org.jruby.util.RubyHashMap;
import org.jruby.util.RubyMap;
import org.jruby.util.RubyMapMethod;

/**
 *
 * @author  jpetersen
 */
public class RubyModule extends RubyObject {
    // The (virtual) super class.
    private RubyClass superClass;

    private String classId;
    private String classPath;

    // The methods.
    private RubyMap methods = new RubyHashMap();

    private RubyModule(Ruby ruby, RubyClass rubyClass) {
        this(ruby, rubyClass, null);
    }

    protected RubyModule(Ruby ruby, RubyClass rubyClass, RubyClass superClass, String name) {
        super(ruby, rubyClass);
        this.superClass = superClass;
        this.classId = name;
    }

    protected RubyModule(Ruby ruby, RubyClass rubyClass, RubyClass superClass) {
        super(ruby, rubyClass);
        this.superClass = superClass;
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

    public RubyMap getMethods() {
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
        Callback op_eqq = CallbackFactory.getMethod(RubyModule.class, "op_eqq", IRubyObject.class);
        Callback op_cmp = CallbackFactory.getMethod(RubyModule.class, "op_cmp", IRubyObject.class);
        Callback op_lt = CallbackFactory.getMethod(RubyModule.class, "op_lt", IRubyObject.class);
        Callback op_le = CallbackFactory.getMethod(RubyModule.class, "op_le", IRubyObject.class);
        Callback op_gt = CallbackFactory.getMethod(RubyModule.class, "op_gt", IRubyObject.class);
        Callback op_ge = CallbackFactory.getMethod(RubyModule.class, "op_ge", IRubyObject.class);

        Callback clone = CallbackFactory.getMethod(RubyModule.class, "rbClone");
        Callback dup = CallbackFactory.getMethod(RubyModule.class, "dup");
        Callback to_s = CallbackFactory.getMethod(RubyModule.class, "to_s");
        Callback included_modules = CallbackFactory.getMethod(RubyModule.class, "included_modules");
        Callback name = CallbackFactory.getMethod(RubyModule.class, "name");
        Callback ancestors = CallbackFactory.getMethod(RubyModule.class, "ancestors");

        Callback attr = CallbackFactory.getOptMethod(RubyModule.class, "attr", IRubyObject.class);
        Callback attr_reader = CallbackFactory.getOptMethod(RubyModule.class, "attr_reader");
        Callback attr_writer = CallbackFactory.getOptMethod(RubyModule.class, "attr_writer");
        Callback attr_accessor = CallbackFactory.getOptMethod(RubyModule.class, "attr_accessor");

        Callback newModule = CallbackFactory.getSingletonMethod(RubyModule.class, "newModule");
        Callback initialize = CallbackFactory.getOptMethod(RubyModule.class, "initialize");
        Callback instance_methods = CallbackFactory.getOptMethod(RubyModule.class, "instance_methods");
        Callback public_instance_methods = CallbackFactory.getOptMethod(RubyModule.class, "instance_methods");
        Callback protected_instance_methods =
            CallbackFactory.getOptMethod(RubyModule.class, "protected_instance_methods");
        Callback private_instance_methods = CallbackFactory.getOptMethod(RubyModule.class, "private_instance_methods");

        Callback constants = CallbackFactory.getMethod(RubyModule.class, "constants");
        Callback const_get = CallbackFactory.getMethod(RubyModule.class, "const_get", IRubyObject.class);
        Callback const_set =
            CallbackFactory.getMethod(RubyModule.class, "const_set", IRubyObject.class, IRubyObject.class);
        Callback const_defined = CallbackFactory.getMethod(RubyModule.class, "const_defined", IRubyObject.class);
        Callback class_variables = CallbackFactory.getMethod(RubyModule.class, "class_variables");
        Callback remove_class_variable =
            CallbackFactory.getMethod(RubyModule.class, "remove_class_variable", IRubyObject.class);

        Callback append_features = CallbackFactory.getMethod(RubyModule.class, "append_features", RubyModule.class);
        Callback extend_object = CallbackFactory.getMethod(RubyModule.class, "extend_object", IRubyObject.class);
        Callback include = CallbackFactory.getOptMethod(RubyModule.class, "include");
        Callback rbPublic = CallbackFactory.getOptMethod(RubyModule.class, "rbPublic");
        Callback rbProtected = CallbackFactory.getOptMethod(RubyModule.class, "rbProtected");
        Callback rbPrivate = CallbackFactory.getOptMethod(RubyModule.class, "rbPrivate");
        Callback module_function = CallbackFactory.getOptMethod(RubyModule.class, "module_function");

        Callback method_defined = CallbackFactory.getMethod(RubyModule.class, "method_defined", IRubyObject.class);
        Callback public_class_method = CallbackFactory.getOptMethod(RubyModule.class, "public_class_method");
        Callback private_class_method = CallbackFactory.getOptMethod(RubyModule.class, "private_class_method");

        Callback module_eval = CallbackFactory.getOptMethod(RubyModule.class, "module_eval");
        Callback remove_method = CallbackFactory.getMethod(RubyModule.class, "remove_method", IRubyObject.class);
        Callback undef_method = CallbackFactory.getMethod(RubyModule.class, "undef_method", IRubyObject.class);
        Callback alias_method =
            CallbackFactory.getMethod(RubyModule.class, "alias_method", IRubyObject.class, IRubyObject.class);

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
        moduleClass.definePrivateMethod("method_added", CallbackFactory.getNilMethod(1));
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
        moduleClass.definePrivateMethod("define_method", CallbackFactory.getOptMethod(RubyModule.class, "define_method"));

        moduleClass.defineMethod("instance_method", CallbackFactory.getMethod(RubyModule.class, "instance_method", IRubyObject.class));

        moduleClass.defineMethod("const_missing", CallbackFactory.getMethod(RubyModule.class, "const_missing", IRubyObject.class));

        moduleClass.defineSingletonMethod("nesting", CallbackFactory.getSingletonMethod(RubyModule.class, "nesting"));
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
        FindClassPathResult arg;
        String path = null;
        String name = null;

        RubyMap instanceVariables = getRuntime().getClasses().getObjectClass().getInstanceVariables();
        if (instanceVariables != null) {
            Iterator iter = instanceVariables.entrySet().iterator();
            arg = findClassPathMap(iter, this, getRuntime().getClasses().getObjectClass());
            name = arg.name;
            path = arg.path;
        }
        if (name == null) {
            Iterator iter = getRuntime().getClasses().getClassMap().entrySet().iterator();
            arg = findClassPathMap(iter, this, getRuntime().getClasses().getObjectClass());
            name = arg.name;
            path = arg.path;
        }
        if (name == null) {
            return null;
        }
        classPath = path;
        return path;
    }

    /** include_class_new
     *
     */
    public RubyIncludedClass newIncludeClass(RubyClass superClass) {
        return new RubyIncludedClass(getRuntime(), superClass, this);
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

        String s = "Module";
        if (isClass()) {
            s = "Class";
        }

        return "<" + s + " 01x" + Integer.toHexString(System.identityHashCode(this)) + ">";
        // 0 = pointer
    }

    /** rb_cvar_singleton
     *
     *@deprecated since Ruby 1.6.7
     */
    public RubyModule getClassVarSingleton() {
        return this;
    }

    /** rb_cvar_set
     *
     */
    public void setClassVar(String name, IRubyObject value) {
        RubyModule tmp = this;
        while (tmp != null) {
            if (tmp.hasInstanceVariable(name)) {
                if (tmp.isTaint() && getRuntime().getSafeLevel() >= 4) {
                    throw new SecurityError(getRuntime(), "Insecure: can't modify class variable");
                }
                tmp.setInstanceVariable(name, value);
                return;
            }
            tmp = tmp.getSuperClass();
        }
        throw new NameError(getRuntime(), "uninitialized class variable " + name + " in " + toName());
    }

    /** rb_cvar_declare
     *
     */
    public void declareClassVar(String name, IRubyObject value) {
        RubyModule tmp = this;
        while (tmp != null) {
            if (tmp.hasInstanceVariable(name)) {
                if (tmp.isTaint() && getRuntime().getSafeLevel() >= 4) {
                    throw new SecurityError(getRuntime(), "Insecure: can't modify class variable");
                }
                tmp.setInstanceVariable(name, value);
            }
            tmp = tmp.getSuperClass();
        }
        setAv(name, value, false);
    }

    /** rb_cvar_get
     *
     */
    public IRubyObject getClassVar(String name) {
        RubyModule tmp = this;
        while (tmp != null) {
            if (tmp.hasInstanceVariable(name)) {
                return tmp.getInstanceVariable(name);
            }
            tmp = tmp.getSuperClass();
        }
        throw new NameError(getRuntime(), "uninitialized class variable " + name + " in " + toName());
    }

    /** rb_cvar_defined
     *
     */
    public boolean isClassVarDefined(String name) {
        RubyModule tmp = this;
        while (tmp != null) {
            if (tmp.hasInstanceVariable(name)) {
                return true;
            }
            tmp = tmp.getSuperClass();
        }
        return false;
    }

    /**
     *
     */
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
            throw new TypeError(runtime, "Wrong argument type " + arg.getInternalClass().toName() + " (expected Module).");
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
            runtime.getMethodCache().clear();
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
        runtime.getMethodCache().clearByName(name);
        getMethods().put(name, method);
    }

    public void defineMethod(String name, Callback method) {
        Visibility visibility = name.equals("initialize") ? Visibility.PRIVATE : Visibility.PUBLIC;

        addMethod(name, new CallbackMethod(method, visibility));
    }

    public void definePrivateMethod(String name, Callback method) {
        addMethod(name, new CallbackMethod(method, Visibility.PRIVATE));
    }

    public void undefMethod(String name) {
        addMethod(name, UndefinedMethod.getInstance());
    }

    /** rb_frozen_class_p
     *
     */
    protected void testFrozen() {
        String desc = "something(?!)";
        if (isFrozen()) {
            if (isSingleton()) {
                desc = "object";
            } else {
                if (isIncluded() || isModule()) {
                    desc = "module";
                } else if (isClass()) {
                    desc = "class";
                }
            }
            throw new FrozenError(getRuntime(), desc);
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
        CacheEntry entry = getMethodBody(name);
        return entry.getVisibility();
    }

    /** rb_get_method_body
     *
     */
    protected CacheEntry getMethodBody(String name) {
        name = name.intern();

        ICallable method = searchMethod(name);

        if (method.isUndefined()) {
            CacheEntry undefinedEntry = CacheEntry.createUndefined(name, this);
            getRuntime().getMethodCache().saveEntry(this, name, undefinedEntry);
            return undefinedEntry;
        }

        CacheEntry result = new CacheEntry(this, method.getVisibility());

        if (method instanceof AliasMethod) {
            result.setName(name);
            result.setOrigin(((AliasMethod) method).getOrigin());
            result.setOriginalName(((AliasMethod) method).getOldName());
            result.setMethod(((AliasMethod) method).getOldMethod());

            result.setRecvClass(((AliasMethod) method).getOrigin());
        } else {
            result.setName(name);
            result.setOrigin(method.getImplementationClass());
            result.setOriginalName(name);
            result.setMethod(method);

            result.setRecvClass(method.getImplementationClass());
        }

        getRuntime().getMethodCache().saveEntry(this, name, result);
        return result;
    }

    /** rb_call
     *
     */
    public final IRubyObject call(IRubyObject recv, String name, IRubyObject[] args, CallType callType) {
        if (args == null) {
            args = new IRubyObject[0];
        }

        CacheEntry entry = getRuntime().getMethodCache().getEntry(this, name);
        if (entry == null) {
            entry = getMethodBody(name);
        }

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
                    defined = defined.getInternalClass();
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
        // ...
        runtime.getIterStack().push(runtime.getCurrentIter().isPre() ? Iter.ITER_CUR : Iter.ITER_NOT);

        runtime.getFrameStack().push();
        runtime.getCurrentFrame().setLastFunc(name);
        runtime.getCurrentFrame().setLastClass(noSuper ? null : this);
        runtime.getCurrentFrame().setSelf(recv);
        runtime.getCurrentFrame().setArgs(args);

        try {
            return method.call(runtime, recv, name, args, noSuper);
        } finally {
            runtime.getFrameStack().pop();
            runtime.getIterStack().pop();
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

        RubyModule origin = null;

        if (method.isUndefined()) {
            if (isModule()) {
                method = getRuntime().getClasses().getObjectClass().searchMethod(oldName);
            }
            if (method.isUndefined()) {
                throw new NameError(
                    runtime,
                    "undefined method '" + name + "' for " + (isModule() ? "module" : "class") + " '" + toName() + "'");
            }
        }
        origin = method.getImplementationClass();

        if (method instanceof AliasMethod) { /* was alias */
            oldName = ((AliasMethod) method).getOldName();
            origin = ((AliasMethod) method).getOrigin();
            method = ((AliasMethod) method).getOldMethod();
        }

        getRuntime().getMethodCache().clearByName(name);
        getMethods().put(name, new AliasMethod(method, oldName, origin, method.getVisibility()));
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

        getRuntime().getMethodCache().clearByName(name);
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
        if (this == getRuntime().getClasses().getNilClass()) {
            return "nil";
        } else if (this == getRuntime().getClasses().getTrueClass()) {
            return "true";
        } else if (this == getRuntime().getClasses().getFalseClass()) {
            return "false";
        }
        // REMOVE ---
        return getClassPath();
    }

    /** rb_define_const
     *
     */
    public void defineConstant(String name, IRubyObject value) {
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

    private void addAttribute(String name, boolean read, boolean write, boolean ex) {
        Visibility attributeScope = Visibility.PUBLIC;

        if (ex) {
            attributeScope = getRuntime().getCurrentVisibility();
            if (attributeScope.isPrivate()) {
                //FIXME warning
            } else if (attributeScope.isModuleFunction()) {
                attributeScope = Visibility.PRIVATE;
                // FIXME warning
            }
        }

        String attrIV = "@" + name;

        if (read) {
            addMethod(name, new EvaluateMethod(new InstVarNode(getRuntime().getPosition(), attrIV), attributeScope));
            callMethod("method_added", RubySymbol.newSymbol(getRuntime(), name));
        }

        if (write) {
            name = name + "=";
            addMethod(name, new EvaluateMethod(new AttrSetNode(getRuntime().getPosition(), attrIV), attributeScope));
            callMethod("method_added", RubySymbol.newSymbol(getRuntime(), name));
        }
    }

    /** method_list
     *
     */
    private RubyArray methodList(boolean option, RubyMapMethod method) {
        RubyArray ary = RubyArray.newArray(getRuntime());

        for (RubyModule klass = this; klass != null; klass = klass.getSuperClass()) {
            klass.getMethods().foreach(method, ary);
            if (!option) {
                break;
            }
        }

        Iterator iter = ary.getList().iterator();
        while (iter.hasNext()) {
            if (getRuntime().getNil() == iter.next()) {
                iter.remove();
                iter.next();
            }
        }

        return ary;
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
        CacheEntry entry = runtime.getMethodCache().getEntry(this, name);

        if (entry == null) {
            entry = getMethodBody(name);
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
        CacheEntry ent = getMethodBody(name);
        if (! ent.isDefined()) {
            // printUndef();
            return getRuntime().getNil();
        }

        while (ent.getMethod() instanceof EvaluateMethod
            && ((EvaluateMethod) ent.getMethod()).getNode() instanceof ZSuperNode) {
            ent = ent.getOrigin().getSuperClass().getMethodBody(ent.getOriginalName());
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
            if (!(args[0].isKindOf(runtime.getClasses().getMethodClass()) ||
                args[0].isKindOf(runtime.getClasses().getProcClass()))) {
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

        if (isSingleton()) {
            getInstanceVariable("__attached__").callMethod("singleton_method_added", symbol);
        } else {
            callMethod("method_added", symbol);
        }

        return body;
    }

    public IRubyObject executeUnder(Callback method, IRubyObject[] args) {
        runtime.pushClass(this);

        Frame frame = runtime.getCurrentFrame();
        runtime.getFrameStack().push();
        runtime.getCurrentFrame().setLastFunc(frame.getLastFunc());
        runtime.getCurrentFrame().setLastClass(frame.getLastClass());
        runtime.getCurrentFrame().setArgs(frame.getArgs());
        if (runtime.getCBase() != this) {
            runtime.getCurrentFrame().setNamespace(new Namespace(this, runtime.getCurrentFrame().getNamespace()));
        }
        runtime.setNamespace(new Namespace(this, runtime.getNamespace()));

        // mode = scope_vmode;
        // SCOPE_SET(SCOPE_PUBLIC);

        try {
            return method.execute(this, args);
        } finally {
            runtime.setNamespace(runtime.getNamespace().getParent());
            runtime.getFrameStack().pop();
            runtime.popClass();
        }
    }

    // Methods of the Module Class (rb_mod_*):

    /** rb_mod_new
     *
     */
    public static RubyModule newModule(Ruby ruby) {
        RubyModule newModule = new RubyModule(ruby, ruby.getClasses().getModuleClass());
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

        // clone the methods.
        if (getMethods() != null) {
            // clone.setMethods(new RubyHashMap());
            getMethods().foreach(new RubyMapMethod() {
                public int execute(Object key, Object value, Object arg) {
                    ICallable method = (ICallable) value;

                    ((RubyMap) arg).put(key, method);

                    return RubyMapMethod.CONTINUE;
                }
            }, clone.getMethods());
        }

        return clone;
    }

    /** rb_mod_dup
     *
     */
    public IRubyObject dup() {
        RubyModule dup = (RubyModule) rbClone();
        dup.setInternalClass(getInternalClass());

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
                ary.append(((RubyIncludedClass) p).getDelegate());
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
                ary.append(((RubyIncludedClass) p).getDelegate());
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
                "<=> requires Class or Module (" + getInternalClass().toName() + " given)");
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

    /** rb_module_s_new
     *
     */
    public static RubyModule newModule(IRubyObject recv) {
        RubyModule mod = RubyModule.newModule(recv.getRuntime());

        mod.setInternalClass((RubyClass) recv);
        recv.getRuntime().getClasses().getModuleClass().callInit(null);

        return mod;
    }

    /** Return an array of nested modules or classes.
     *
     * rb_mod_nesting
     *
     */
    public static RubyArray nesting(IRubyObject recv) {
        Namespace ns = recv.getRuntime().getCurrentFrame().getNamespace();

        RubyArray ary = RubyArray.newArray(recv.getRuntime());

        while (ns != null && ns.getParent() != null) {
            if (!ns.getNamespaceModule().isNil()) {
                ary.append(ns.getNamespaceModule());
            }

            ns = ns.getParent();
        }

        return ary;
    }

    /** rb_mod_attr
     *
     */
    public IRubyObject attr(IRubyObject symbol, IRubyObject[] args) {
        boolean writeable = false;
        if (args.length > 0) {
            writeable = args[0].isTrue();
        }

        addAttribute(symbol.asSymbol(), true, writeable, true);

        return getRuntime().getNil();
    }

    /** rb_mod_attr_reader
     *
     */
    public IRubyObject attr_reader(IRubyObject[] args) {
        for (int i = 0; i < args.length; i++) {
            addAttribute(args[i].asSymbol(), true, false, true);
        }

        return getRuntime().getNil();
    }

    /** rb_mod_attr_writer
     *
     */
    public IRubyObject attr_writer(IRubyObject[] args) {
        for (int i = 0; i < args.length; i++) {
            addAttribute(args[i].asSymbol(), false, true, true);
        }

        return getRuntime().getNil();
    }

    /** rb_mod_attr_accessor
     *
     */
    public IRubyObject attr_accessor(IRubyObject[] args) {
        for (int i = 0; i < args.length; i++) {
            addAttribute(args[i].asSymbol(), true, true, true);
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

        return methodList(includeSuper, new RubyMapMethod() {
            public int execute(Object key, Object value, Object arg) {
                // cast args
                String id = (String) key;
                ICallable method = (ICallable) value;
                RubyArray ary = (RubyArray) arg;

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
                return RubyMapMethod.CONTINUE;
            }
        });
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
                Asserts.assertTrue(!method.isUndefined(), "undefined method '" + name + "'");
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
        getInternalClass().setMethodVisibility(args, Visibility.PUBLIC);
        return this;
    }

    public RubyModule private_class_method(IRubyObject[] args) {
        getInternalClass().setMethodVisibility(args, Visibility.PRIVATE);
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

    private static class FindClassPathResult {
        public String name;
        public RubyModule klass;
        public String path;
        public IRubyObject track;
        public FindClassPathResult prev;
    }

    private FindClassPathResult findClassPathMap(Iterator iter, RubyModule klass, IRubyObject track) {
        FindClassPathResult result = new FindClassPathResult();
        result.klass = klass;
        result.track = track;
        findClassPathMap(iter, result);
        return result;
    }

    private void findClassPathMap(Iterator iter, FindClassPathResult res) {
        OUTER : while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry)iter.next();
            String key = (String) entry.getKey();
            IRubyObject value = (IRubyObject) entry.getValue();

            String path = null;

            if (!IdUtil.isConstant(key)) {
                continue;
            }

            if (res.path != null) {
                path = res.path;
                path += "::" + key;
            } else {
                path = key;
            }

            if (value == res.klass) {
                res.name = key;
                res.path = path;
                break;
            }

            if (value.isKindOf(getRuntime().getClasses().getModuleClass())) {
                if (value.getInstanceVariables() == null) {
                    continue;
                }

                FindClassPathResult list = res;

                while (list != null) {
                    if (list.track == value) {
                        continue OUTER;
                    }
                    list = list.prev;
                }

                FindClassPathResult arg = new FindClassPathResult();
                arg.name = null;
                arg.path = path;
                arg.klass = res.klass;
                arg.track = value;
                arg.prev = res;

                findClassPathMap(value.getInstanceVariables().entrySet().iterator(), arg);

                if (arg.name != null) {
                    res.name = arg.name;
                    res.path = arg.path;
                    break;
                }
            }
        }
    }
}
