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

import org.jruby.ast.AttrSetNode;
import org.jruby.ast.InstVarNode;
import org.jruby.ast.ZSuperNode;
import org.jruby.exceptions.ArgumentError;
import org.jruby.exceptions.NameError;
import org.jruby.exceptions.RubyBugException;
import org.jruby.exceptions.RubyFrozenException;
import org.jruby.exceptions.RubySecurityException;
import org.jruby.exceptions.TypeError;
import org.jruby.internal.runtime.methods.AliasMethod;
import org.jruby.internal.runtime.methods.CacheEntry;
import org.jruby.internal.runtime.methods.CallbackMethod;
import org.jruby.internal.runtime.methods.EvaluateMethod;
import org.jruby.runtime.Callback;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.Constants;
import org.jruby.runtime.Frame;
import org.jruby.runtime.ICallable;
import org.jruby.runtime.Iter;
import org.jruby.runtime.Namespace;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
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

    public RubyModule(Ruby ruby) {
        this(ruby, null);
    }

    public RubyModule(Ruby ruby, RubyClass rubyClass) {
        this(ruby, rubyClass, null);
    }

    public RubyModule(Ruby ruby, RubyClass rubyClass, RubyClass superClass) {
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
    public void setSuperClass(RubyClass superClass) {
        this.superClass = superClass;
    }

    public RubyMap getMethods() {
        return this.methods;
    }

    public void setMethods(RubyMap methods) {
        this.methods = methods;
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
        ;
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

        moduleClass.defineMethod("remove_method", remove_method);
        moduleClass.defineMethod("undef_method", undef_method);
        moduleClass.defineMethod("alias_method", alias_method);
        /*rb_define_private_method(rb_cModule, "define_method", rb_mod_define_method, -1);
        
        rb_define_singleton_method(rb_cModule, "constants", rb_mod_s_constants, 0);*/
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

        if (module == null) {
            module = getRuntime().getClasses().getObjectClass();
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
    public String findClassPath() {
        FindClassPathResult arg = new FindClassPathResult();
        arg.klass = this;
        arg.track = getRuntime().getClasses().getObjectClass();
        arg.prev = null;

        if (getRuntime().getClasses().getObjectClass().getInstanceVariables() != null) {
            getRuntime().getClasses().getObjectClass().getInstanceVariables().foreach(
                new FindClassPathMapMethod(),
                arg);
        }

        if (arg.name == null) {
            getRuntime().getClasses().getClassMap().foreach(new FindClassPathMapMethod(), arg);
        }

        if (arg.name != null) {
            classPath = arg.path;
            return arg.path;
        }
        return null;
    }

    /** include_class_new
     *
     */
    public RubyIncludedClass newIncludeClass(RubyClass superClass) {
        return new RubyIncludedClass(getRuntime(), superClass, this);
    }

    public void setName(String name) {
        classId = name;
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
            if (tmp.getInstanceVariables() != null && tmp.getInstanceVariables().get(name) != null) {
                if (tmp.isTaint() && getRuntime().getSafeLevel() >= 4) {
                    throw new RubySecurityException(getRuntime(), "Insecure: can't modify class variable");
                }
                tmp.getInstanceVariables().put(name, value);
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
            if (tmp.getInstanceVariables() != null && tmp.getInstanceVariables().get(name) != null) {
                if (tmp.isTaint() && getRuntime().getSafeLevel() >= 4) {
                    throw new RubySecurityException(getRuntime(), "Insecure: can't modify class variable");
                }
                tmp.getInstanceVariables().put(name, value);
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
            if (tmp.getInstanceVariables() != null && tmp.getInstanceVariables().get(name) != null) {
                return (IRubyObject) tmp.getInstanceVariables().get(name);
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
            if (tmp.getInstanceVariables() != null && tmp.getInstanceVariables().get(name) != null) {
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

    /** rb_const_get
     *
     */
    public IRubyObject getConstant(String name) {
        boolean mod_retry = false;
        RubyModule tmp = this;

        while (true) {
            while (tmp != null) {
                if (!tmp.getInstanceVariable(name).isNil()) {
                    return tmp.getInstanceVariable(name);
                }
                if (tmp == getRuntime().getClasses().getObjectClass() && getRuntime().getTopConstant(name) != null) {
                    return getRuntime().getTopConstant(name);
                }
                tmp = tmp.getSuperClass();
            }
            if (!mod_retry && isModule()) {
                mod_retry = true;
                tmp = getRuntime().getClasses().getObjectClass();
                continue;
            }
            break;
        }

        // Now try to load a Java class
        String javaClassName = getRuntime().getJavaSupport().getJavaName(name);
        if (javaClassName == null) {
            javaClassName = name;
        }

        try {
            Class javaClass = getRuntime().getJavaSupport().loadJavaClass(javaClassName);
            return getRuntime().getJavaSupport().loadClass(javaClass, null);
        } catch (NameError excptn) {
        }

        /* Uninitialized constant */
        if (this != getRuntime().getClasses().getObjectClass()) {
            throw new NameError(getRuntime(), "uninitialized constant " + name + " at " + getClassPath());
        } else {
            throw new NameError(getRuntime(), "uninitialized constant " + name);
        }

        // return getRuby().getNil();
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
            ruby.secure(4);
        }
        if (arg == null || arg == this) {
            return;
        }

        if (!(arg instanceof RubyModule)) {
            throw new TypeError(ruby, "Wrong argument type " + arg.getInternalClass().toName() + " (expected Module).");
        }

        RubyModule module = (RubyModule) arg;

        boolean changed = false;

        RubyModule type = this;

        addModule : while (module != null) {
            if (getMethods() == module.getMethods()) {
                throw new ArgumentError(ruby, "Cyclic include detected.");
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
            ruby.getMethodCache().clear();
        }
    }

    /** mod_av_set
     *
     */
    protected void setAv(String name, IRubyObject value, boolean constant) {
        String dest = constant ? "constant" : "class variable";

        if (!isTaint() && getRuntime().getSafeLevel() >= 4) {
            throw new RubySecurityException(getRuntime(), "Insecure: can't set " + dest);
        }
        if (isFrozen()) {
            throw new RubyFrozenException(getRuntime(), "class/module");
        }
        if (constant && (!getInstanceVariable(name).isNil())) {
            //getRuby().warn("already initialized " + dest + " " + name);
        }

        setInstanceVariable(name, value);
    }

    /** rb_add_method
     *
     */
    public void addMethod(String name, ICallable method, int noex) {
        if (this == getRuntime().getClasses().getObjectClass()) {
            getRuntime().secure(4);
        }

        if (getRuntime().getSafeLevel() >= 4 && !isTaint()) {
            throw new RubySecurityException(getRuntime(), "Insecure: can't define method");
        }
        if (isFrozen()) {
            throw new RubyFrozenException(getRuntime(), "class/module");
        }
        ruby.getMethodCache().clearByName(name);
        getMethods().put(name, method);
    }

    public void defineMethod(String name, Callback method) {
        int noex = (name.equals("initialize")) ? Constants.NOEX_PRIVATE : Constants.NOEX_PUBLIC;

        addMethod(name, new CallbackMethod(method), noex | Constants.NOEX_CFUNC);
    }

    public void defineProtectedMethod(String name, Callback method) {
        addMethod(name, new CallbackMethod(method), Constants.NOEX_PROTECTED | Constants.NOEX_CFUNC);
    }

    public void definePrivateMethod(String name, Callback method) {
        addMethod(name, new CallbackMethod(method), Constants.NOEX_PRIVATE | Constants.NOEX_CFUNC);
    }

    /*public void undefMethod(RubyId id) {
        if (isFrozen()) {
            throw new RubyFrozenException();
        }
     
        getMethods().remove(id);
    }*/

    public void undefMethod(String name) {
        addMethod(name, null, Constants.NOEX_UNDEF);
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
            throw new RubyFrozenException(getRuntime(), desc);
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
        if (method == null) {
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
        addMethod(name, null, Constants.NOEX_PUBLIC);
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

    /** rb_define_attr
     *
     */
    public void defineAttribute(String name, boolean read, boolean write) {
        // +++ jpetersen
        addAttribute(name, read, write, true);
        // --- 
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

        return getRuntime().isAutoloadDefined(name);
    }

    /**
     * 
     * MRI: rb_const_defined_at
     * 
     */
    public boolean isConstantDefinedAt(String name) {
        if (!getInstanceVariable(name).isNil()) {
            return true;
        } else if (this == ruby.getClasses().getObjectClass()) {
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
                return null;
            }
        } else {
            method.setImplementationClass(this);
            return method;
        }
    }

    public int getMethodNoex(String name) {
        return getMethodBody(name, 0).getNoex();
    }

    /** rb_get_method_body
     *
     */
    protected CacheEntry getMethodBody(String name, int noex) {
        ICallable method = searchMethod(name);

        if (method == null) {
            // System.out.println("Cant find method \"" + name + "\" in class " + toName());

            getRuntime().getMethodCache().saveUndefinedEntry(this, name);

            return null;
        }

        CacheEntry result = new CacheEntry(this, method.getNoex());

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
    public final IRubyObject call(IRubyObject recv, String name, IRubyObject[] args, final int scope) {
        if (args == null) {
            args = new IRubyObject[0];
        }

        CacheEntry ent = getRuntime().getMethodCache().getEntry(this, name);

        if (ent == null) {
            ent = getMethodBody(name, 0);
        }

        if (ent == null || ent.getMethod() == null) {
            if (scope == 3) {
                throw new NameError(ruby, "super: no superclass method '" + name + "'");
            }
            IRubyObject[] newArgs = new IRubyObject[args.length + 1];
            newArgs[0] = RubySymbol.newSymbol(getRuntime(), name);
            System.arraycopy(args, 0, newArgs, 1, args.length);
            return recv.callMethod("method_missing", newArgs);
        }

        RubyModule klass = ent.getOrigin();
        name = ent.getOriginalName();
        ICallable method = ent.getMethod();

        // if (mid != missing) {
        //     /* receiver specified form for private method */
        //     if ((noex & NOEX_PRIVATE) && scope == 0)
        //         return rb_undefined(recv, mid, argc, argv, CSTAT_PRIV);

        //     /* self must be kind of a specified form for private method */
        //     if ((noex & NOEX_PROTECTED)) {
        //         VALUE defined_class = klass;
        //         while (TYPE(defined_class) == T_ICLASS)
        //             defined_class = RBASIC(defined_class)->klass;
        //         if (!rb_obj_is_kind_of(ruby_frame->self, defined_class))
        //             return rb_undefined(recv, mid, argc, argv, CSTAT_PROT);
        //     }
        // }

        // ...

        return klass.call0(recv, name, args, method, false);
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
        ruby.getIterStack().push(ruby.getCurrentIter().isPre() ? Iter.ITER_CUR : Iter.ITER_NOT);

        ruby.getFrameStack().push();
        ruby.getCurrentFrame().setLastFunc(name);
        ruby.getCurrentFrame().setLastClass(noSuper ? null : this);
        ruby.getCurrentFrame().setSelf(recv);
        ruby.getCurrentFrame().setArgs(args);

        try {
            return method.call(ruby, recv, name, args, noSuper);
        } finally {
            ruby.getFrameStack().pop();
            ruby.getIterStack().pop();
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

        if (method == null) {
            if (isModule()) {
                method = getRuntime().getClasses().getObjectClass().searchMethod(oldName);
            }
            if (method == null) {
                throw new NameError(
                    ruby,
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
        getMethods().put(name, new AliasMethod(method, oldName, origin, method.getNoex()));
    }

    /** remove_method
     * 
     */
    public void removeMethod(String name) {
        if (this == getRuntime().getClasses().getObjectClass()) {
            getRuntime().secure(4);
        }
        if (getRuntime().getSafeLevel() >= 4 && !isTaint()) {
            throw new RubySecurityException(getRuntime(), "Insecure: can't remove method");
        }
        if (isFrozen()) {
            throw new RubyFrozenException(getRuntime(), "class/module");
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
                throw new TypeError(ruby, name + " is not a class.");
            } else if (((RubyClass) type).getSuperClass().getRealClass() != superClass) {
                throw new NameError(ruby, name + " is already defined.");
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

    /** rb_define_module_under
     *
     */
    public RubyModule defineModuleUnder(String name) {
        if (isConstantDefinedAt(name)) {
            IRubyObject module = getConstant(name);
            if (!(module instanceof RubyModule)) {
                throw new TypeError(ruby, toName() + "::" + module.getInternalClass().toName() + " is not a module.");
            } else {
                return (RubyModule) module;
            }
        } else {
            RubyModule newModule = getRuntime().defineModule(name);

            setConstant(name, newModule);
            newModule.setClassPath(this, name);

            return newModule;
        }
    }

    /** rb_class2name
     *
     */
    public String toName() {
        if (this == getRuntime().getClasses().getNilClass()) {
            return "NilClass";
        }
        if (this == getRuntime().getClasses().getTrueClass()) {
            return "TrueClass";
        }
        if (this == getRuntime().getClasses().getFalseClass()) {
            return "FalseClass";
        }

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
        if (!IdUtil.isClassVariable(name.toId())) {
            throw new NameError(getRuntime(), "wrong class variable name " + name.toId());
        }

        if (!isTaint() && getRuntime().getSafeLevel() >= 4) {
            throw new RubySecurityException(getRuntime(), "Insecure: can't remove class variable");
        }

        if (isFrozen()) {
            throw new RubyFrozenException(getRuntime(), "class/module");
        }

        IRubyObject value = (IRubyObject) getInstanceVariables().remove(name.toId());

        if (value != null) {
            return value;
        }

        if (isClassVarDefined(name.toId())) {
            throw new NameError(getRuntime(), "cannot remove " + name.toId() + " for " + toName());
        }

        throw new NameError(getRuntime(), "class variable " + name.toId() + " not defined for " + toName());
    }

    /** rb_define_class_variable
     *
     */
    public void defineClassVariable(String name, IRubyObject value) {
        if (!IdUtil.isClassVariable(name)) {
            throw new NameError(getRuntime(), "wrong class variable name " + name);
        }

        declareClassVar(name, value);
    }

    /** rb_attr
     *
     */
    public void addAttribute(String name, boolean read, boolean write, boolean ex) {
        int noex = Constants.NOEX_PUBLIC;

        if (ex) {
            if (getRuntime().getCurrentMethodScope() == Constants.SCOPE_PRIVATE) {
                noex = Constants.NOEX_PRIVATE;
            } else if (getRuntime().getCurrentMethodScope() == Constants.SCOPE_PROTECTED) {
                noex = Constants.NOEX_PROTECTED;
            } else {
                noex = Constants.NOEX_PUBLIC;
            }
        }

        String attrIV = "@" + name;

        if (read) {
            addMethod(name, new EvaluateMethod(new InstVarNode(getRuntime().getPosition(), attrIV)), noex);
            callMethod("method_added", RubySymbol.newSymbol(getRuntime(), name));
        }

        if (write) {
            name = name + "=";
            addMethod(name, new EvaluateMethod(new AttrSetNode(getRuntime().getPosition(), attrIV)), noex);
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
    public void setMethodVisibility(IRubyObject[] methods, int noex) {
        if (getRuntime().getSafeLevel() >= 4 && !isTaint()) {
            throw new RubySecurityException(getRuntime(), "Insecure: can't change method visibility");
        }

        for (int i = 0; i < methods.length; i++) {
            exportMethod(methods[i].toId(), noex);
        }
    }

    /** rb_export_method
     *
     */
    public void exportMethod(String name, int noex) {
        if (this == getRuntime().getClasses().getObjectClass()) {
            getRuntime().secure(4);
        }

        ICallable method = searchMethod(name);

        if (method == null && isModule()) {
            method = getRuntime().getClasses().getObjectClass().searchMethod(name);
        }

        if (method == null) {
            throw new NameError(
                ruby,
                "undefined method '" + name + "' for " + (isModule() ? "module" : "class") + " '" + toName() + "'");
        }

        if (method.getNoex() != noex) {
            if (this == method.getImplementationClass()) {
                method.setNoex(noex);
            } else {
                addMethod(name, new EvaluateMethod(new ZSuperNode(getRuntime().getPosition())), noex);
            }
        }
    }

    /** 
     * MRI: rb_method_boundp
     * 
     */
    public boolean isMethodBound(String name, int ex) {
        CacheEntry entry = ruby.getMethodCache().getEntry(this, name);

        if (entry == null) {
            entry = getMethodBody(name, ex);
        }

        if (entry != null) {
            if (ex != 0 && (entry.getNoex() & Constants.NOEX_PRIVATE) != 0) {
                return false;
            } else if (entry.getMethod() == null) {
                return false;
            } else {
                return true;
            }
        }

        return false;
    }

    public boolean isMethodDefined(String name) {
        return isMethodBound(name, 1);
    }

    public IRubyObject newMethod(IRubyObject recv, String name, RubyClass methodClass) {
        RubyClass originalClass = (RubyClass) this;
        String originalName = name;

        CacheEntry ent = getMethodBody(name, 0);
        if (ent == null) {
            // printUndef();
            return getRuntime().getNil();
        }

        while (ent.getMethod() instanceof EvaluateMethod
            && ((EvaluateMethod) ent.getMethod()).getNode() instanceof ZSuperNode) {
            ent = ent.getOrigin().getSuperClass().getMethodBody(ent.getOriginalName(), 0);
            if (ent == null) {
                // printUndef();
                return getRuntime().getNil();
            }
        }

        RubyMethod newMethod = new RubyMethod(getRuntime(), methodClass);
        newMethod.setReceiverClass((RubyClass) ent.getOrigin());
        newMethod.setReceiver(recv);
        newMethod.setMethodId(ent.getOriginalName());
        newMethod.setMethod(ent.getMethod());
        newMethod.setOriginalClass(originalClass);
        newMethod.setOriginalId(originalName);

        return newMethod;
    }

    public IRubyObject executeUnder(Callback method, IRubyObject[] args) {
        ruby.pushClass(this);

        Frame frame = ruby.getCurrentFrame();
        ruby.getFrameStack().push();
        ruby.getCurrentFrame().setLastFunc(frame.getLastFunc());
        ruby.getCurrentFrame().setLastClass(frame.getLastClass());
        ruby.getCurrentFrame().setArgs(frame.getArgs());
        if (ruby.getCBase() != this) {
            ruby.getCurrentFrame().setNamespace(new Namespace(this, ruby.getCurrentFrame().getNamespace()));
        }
        ruby.setNamespace(new Namespace(this, ruby.getNamespace()));

        // mode = scope_vmode;
        // SCOPE_SET(SCOPE_PUBLIC);

        try {
            return method.execute(this, args);
        } finally {
            ruby.setNamespace(ruby.getNamespace().getParent());
            ruby.getFrameStack().pop();
            ruby.popClass();
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

    /** rb_mod_name
     *
     */
    public RubyString name() {
        String path = getClassname();
        if (path != null) {
            return RubyString.newString(ruby, path);
        }
        return RubyString.newString(ruby, "");
    }

    /** rb_mod_class_variables
     *
     */
    public RubyArray class_variables() {
        RubyArray ary = RubyArray.newArray(getRuntime());

        RubyModule rbModule = this;

        while (rbModule != null) {
            if (rbModule.getInstanceVariables() != null) {
                Iterator iter = rbModule.getInstanceVariables().keySet().iterator();
                while (iter.hasNext()) {
                    String id = (String) iter.next();
                    if (IdUtil.isClassVariable(id)) {
                        RubyString kval = RubyString.newString(getRuntime(), id);
                        if (!ary.includes(kval)) {
                            ary.append(kval);
                        }
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
        return RubyString.newString(ruby, getClassPath());
    }

    /** rb_mod_eqq
     *
     */
    public RubyBoolean op_eqq(IRubyObject obj) {
        return RubyBoolean.newBoolean(ruby, obj.isKindOf(this));
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

        addAttribute(symbol.toId(), true, writeable, true);

        return getRuntime().getNil();
    }

    /** rb_mod_attr_reader
     *
     */
    public IRubyObject attr_reader(IRubyObject[] args) {
        for (int i = 0; i < args.length; i++) {
            addAttribute(((RubySymbol) args[i]).toId(), true, false, true);
        }

        return getRuntime().getNil();
    }

    /** rb_mod_attr_writer
     *
     */
    public IRubyObject attr_writer(IRubyObject[] args) {
        for (int i = 0; i < args.length; i++) {
            addAttribute(((RubySymbol) args[i]).toId(), false, true, true);
        }

        return getRuntime().getNil();
    }

    /** rb_mod_attr_accessor
     *
     */
    public IRubyObject attr_accessor(IRubyObject[] args) {
        for (int i = 0; i < args.length; i++) {
            addAttribute(((RubySymbol) args[i]).toId(), true, true, true);
        }

        return getRuntime().getNil();
    }

    /** rb_mod_const_get
     *
     */
    public IRubyObject const_get(IRubyObject symbol) {
        String name = symbol.toId();

        if (!IdUtil.isConstant(name)) {
            throw new NameError(getRuntime(), "wrong constant name " + name);
        }

        return getConstant(name);
    }

    /** rb_mod_const_set
     *
     */
    public IRubyObject const_set(IRubyObject symbol, IRubyObject value) {
        String name = symbol.toId();

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
        String name = symbol.toId();

        if (!IdUtil.isConstant(name)) {
            throw new NameError(getRuntime(), "wrong constant name " + name);
        }

        return RubyBoolean.newBoolean(getRuntime(), isConstantDefined(name));
    }

    private RubyArray instance_methods(IRubyObject[] args, final int noex) {
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

                if ((method.getNoex() & noex) == 0) {
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
        return instance_methods(args, (Constants.NOEX_PRIVATE | Constants.NOEX_PROTECTED));
    }

    /** rb_class_protected_instance_methods
     *
     */
    public RubyArray protected_instance_methods(IRubyObject[] args) {
        return instance_methods(args, Constants.NOEX_PROTECTED);
    }

    /** rb_class_private_instance_methods
     *
     */
    public RubyArray private_instance_methods(IRubyObject[] args) {
        return instance_methods(args, Constants.NOEX_PRIVATE);
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
        String id = name.toId();

        if (!IdUtil.isClassVariable(id)) {
            throw new NameError(getRuntime(), "wrong class variable name " + id);
        }
        if (!isTaint() && getRuntime().getSafeLevel() >= 4) {
            throw new RubySecurityException(getRuntime(), "Insecure: can't remove class variable");
        }
        if (isFrozen()) {
            throw new RubyFrozenException(getRuntime(), "class/module");
        }

        if (getInstanceVariables() != null) {
            Object value = getInstanceVariables().remove(id);
            if (value != null) {
                return (IRubyObject) value;
            }
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
            throw new RubySecurityException(getRuntime(), "Insecure: can't change method visibility");
        }

        if (args.length == 0) {
            getRuntime().setCurrentMethodScope(Constants.SCOPE_PUBLIC);
        } else {
            setMethodVisibility(args, Constants.NOEX_PUBLIC);
        }

        return this;
    }

    /** rb_mod_protected
     *
     */
    public RubyModule rbProtected(IRubyObject[] args) {
        if (getRuntime().getSafeLevel() >= 4 && !isTaint()) {
            throw new RubySecurityException(getRuntime(), "Insecure: can't change method visibility");
        }

        if (args.length == 0) {
            getRuntime().setCurrentMethodScope(Constants.SCOPE_PROTECTED);
        } else {
            setMethodVisibility(args, Constants.NOEX_PROTECTED);
        }

        return this;
    }

    /** rb_mod_private
     *
     */
    public RubyModule rbPrivate(IRubyObject[] args) {
        if (getRuntime().getSafeLevel() >= 4 && !isTaint()) {
            throw new RubySecurityException(getRuntime(), "Insecure: can't change method visibility");
        }

        if (args.length == 0) {
            getRuntime().setCurrentMethodScope(Constants.SCOPE_PRIVATE);
        } else {
            setMethodVisibility(args, Constants.NOEX_PRIVATE);
        }

        return this;
    }

    /** rb_mod_modfunc
     *
     */
    public RubyModule module_function(IRubyObject[] args) {
        if (getRuntime().getSafeLevel() >= 4 && !isTaint()) {
            throw new RubySecurityException(getRuntime(), "Insecure: can't change method visibility");
        }

        if (args.length == 0) {
            getRuntime().setCurrentMethodScope(Constants.SCOPE_MODFUNC);
        } else {
            setMethodVisibility(args, Constants.NOEX_PRIVATE);

            for (int i = 0; i < args.length; i++) {
                String name = args[i].toId();
                ICallable method = searchMethod(name);
                if (method == null) {
                    throw new RubyBugException("undefined method '" + name + "'; can't happen");
                }
                getSingletonClass().addMethod(name, method, Constants.NOEX_PUBLIC);
                callMethod("singleton_method_added", RubySymbol.newSymbol(getRuntime(), name));
            }
        }

        return this;
    }

    public RubyBoolean method_defined(IRubyObject symbol) {
        return isMethodBound(symbol.toId(), 1) ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    public RubyModule public_class_method(IRubyObject[] args) {
        getInternalClass().setMethodVisibility(args, Constants.NOEX_PUBLIC);

        return this;
    }

    public RubyModule private_class_method(IRubyObject[] args) {
        getInternalClass().setMethodVisibility(args, Constants.NOEX_PRIVATE);

        return this;
    }

    public RubyModule alias_method(IRubyObject newId, IRubyObject oldId) {
        aliasMethod(newId.toId(), oldId.toId());

        return this;
    }

    public RubyModule undef_method(IRubyObject name) {
        undef(name.toId());

        return this;
    }

    public IRubyObject module_eval(IRubyObject[] args) {
        return specificEval(this, args);
    }

    public RubyModule remove_method(IRubyObject name) {
        removeMethod(name.toId());

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
        return result;
    }

    private static class FindClassPathResult {
        public String name;
        public RubyModule klass;
        public String path;
        public IRubyObject track;
        public FindClassPathResult prev;
    }

    private class FindClassPathMapMethod implements RubyMapMethod {
        public int execute(Object _key, Object _value, Object _res) {
            // Cast the values.
            String key = (String) _key;
            if (_value instanceof String || _value == null) {
                return RubyMapMethod.CONTINUE;
            }
            IRubyObject value = (IRubyObject) _value;
            FindClassPathResult res = (FindClassPathResult) _res;

            String path = null;

            if (!IdUtil.isConstant(key)) {
                return RubyMapMethod.CONTINUE;
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
                return RubyMapMethod.STOP;
            }

            if (value.isKindOf(getRuntime().getClasses().getModuleClass())) {
                if (value.getInstanceVariables() == null) {
                    return RubyMapMethod.CONTINUE;
                }

                FindClassPathResult list = res;

                while (list != null) {
                    if (list.track == value) {
                        return RubyMapMethod.CONTINUE;
                    }
                    list = list.prev;
                }

                FindClassPathResult arg = new FindClassPathResult();
                arg.name = null;
                arg.path = path;
                arg.klass = res.klass;
                arg.track = value;
                arg.prev = res;

                value.getInstanceVariables().foreach(this, arg);

                if (arg.name != null) {
                    res.name = arg.name;
                    res.path = arg.path;
                    return RubyMapMethod.STOP;
                }
            }
            return RubyMapMethod.CONTINUE;
        }
    }
}
