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

import java.util.*;

import org.jruby.exceptions.*;
import org.jruby.exceptions.ArgumentError;
import org.jruby.internal.runtime.methods.*;
import org.jruby.javasupport.*;
import org.jruby.ast.*;
import org.jruby.ast.types.*;
import org.jruby.runtime.*;
import org.jruby.runtime.methods.*;
import org.jruby.util.*;
import org.ablaf.ast.visitor.INodeVisitor;

/**
 *
 * @author  jpetersen
 */
public class RubyModule extends RubyObject {
    // The (virtual) super class.
    private RubyClass superClass;

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

    public static void createModuleClass(RubyClass moduleClass) {;
        Callback op_eqq = CallbackFactory.getMethod(RubyModule.class, "op_eqq", RubyObject.class);
        Callback op_cmp = CallbackFactory.getMethod(RubyModule.class, "op_cmp", RubyObject.class);
        Callback op_lt = CallbackFactory.getMethod(RubyModule.class, "op_lt", RubyObject.class);
        Callback op_le = CallbackFactory.getMethod(RubyModule.class, "op_le", RubyObject.class);
        Callback op_gt = CallbackFactory.getMethod(RubyModule.class, "op_gt", RubyObject.class);
        Callback op_ge = CallbackFactory.getMethod(RubyModule.class, "op_ge", RubyObject.class);

        Callback clone = CallbackFactory.getMethod(RubyModule.class, "rbClone");
        Callback dup = CallbackFactory.getMethod(RubyModule.class, "dup");
        Callback to_s = CallbackFactory.getMethod(RubyModule.class, "to_s");
        Callback included_modules = CallbackFactory.getMethod(RubyModule.class, "included_modules");
        Callback name = CallbackFactory.getMethod(RubyModule.class, "name");
        Callback ancestors = CallbackFactory.getMethod(RubyModule.class, "ancestors");

        Callback attr = CallbackFactory.getOptMethod(RubyModule.class, "attr", RubyObject.class);
        Callback attr_reader = CallbackFactory.getOptMethod(RubyModule.class, "attr_reader");
        Callback attr_writer = CallbackFactory.getOptMethod(RubyModule.class, "attr_writer");
        Callback attr_accessor = CallbackFactory.getOptMethod(RubyModule.class, "attr_accessor");

        Callback newModule = CallbackFactory.getSingletonMethod(RubyModule.class, "newModule");
        Callback initialize = CallbackFactory.getOptMethod(RubyModule.class, "initialize");
        Callback instance_methods = CallbackFactory.getOptMethod(RubyModule.class, "instance_methods");
        Callback public_instance_methods = CallbackFactory.getOptMethod(RubyModule.class, "instance_methods");
        Callback protected_instance_methods = CallbackFactory.getOptMethod(RubyModule.class, "protected_instance_methods");
        Callback private_instance_methods = CallbackFactory.getOptMethod(RubyModule.class, "private_instance_methods");

        Callback constants = CallbackFactory.getMethod(RubyModule.class, "constants");
        Callback const_get = CallbackFactory.getMethod(RubyModule.class, "const_get", RubyObject.class);
        Callback const_set = new ReflectionCallbackMethod(RubyModule.class, "const_set", new Class[] { RubyObject.class, RubyObject.class });
        Callback const_defined = CallbackFactory.getMethod(RubyModule.class, "const_defined", RubyObject.class);
        Callback class_variables = CallbackFactory.getMethod(RubyModule.class, "class_variables");
        Callback remove_class_variable = CallbackFactory.getMethod(RubyModule.class, "remove_class_variable", RubyObject.class);

        Callback append_features = CallbackFactory.getMethod(RubyModule.class, "append_features", RubyModule.class);
        Callback extend_object = CallbackFactory.getMethod(RubyModule.class, "extend_object", RubyObject.class);
        Callback include = CallbackFactory.getOptMethod(RubyModule.class, "include");
        Callback rbPublic = CallbackFactory.getOptMethod(RubyModule.class, "rbPublic");
        Callback rbProtected = CallbackFactory.getOptMethod(RubyModule.class, "rbProtected");
        Callback rbPrivate = CallbackFactory.getOptMethod(RubyModule.class, "rbPrivate");
        Callback module_function = CallbackFactory.getOptMethod(RubyModule.class, "module_function");

        Callback method_defined = CallbackFactory.getMethod(RubyModule.class, "method_defined", RubyObject.class);
        Callback public_class_method = CallbackFactory.getOptMethod(RubyModule.class, "public_class_method");
        Callback private_class_method = CallbackFactory.getOptMethod(RubyModule.class, "private_class_method");

        Callback module_eval = CallbackFactory.getOptMethod(RubyModule.class, "module_eval");
        Callback remove_method = CallbackFactory.getMethod(RubyModule.class, "remove_method", RubyObject.class);
        Callback undef_method = CallbackFactory.getMethod(RubyModule.class, "undef_method", RubyObject.class);
        Callback alias_method = new ReflectionCallbackMethod(RubyModule.class, "alias_method", new Class[] { RubyObject.class, RubyObject.class });

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
        moduleClass.definePrivateMethod("method_added", CallbackFactory.getNilMethod());
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
    public RubyString getClassname() {
        RubyString path = null;

        RubyModule module = this;
        while (module.isIncluded() || module.isSingleton()) {
            module = module.getSuperClass();
        }

        if (module == null) {
            module = getRuby().getClasses().getObjectClass();
        }

        path = (RubyString) getInstanceVariables().get("__classpath__");
        if (path == null) {
            if (getInstanceVariables().get("__classid__") != null) {
                path = RubyString.newString(getRuby(), (String) getInstanceVariables().get("__classid__"));
                // todo: convert from symbol to string

                getInstanceVariables().put("__classpath__", path);
                getInstanceVariables().remove("__classid__");
            }
        }

        if (path == null) {
            return module.findClassPath();
        }

        /*if (!(path instanceof RubyString)) {
            throw new RubyBugException("class path is not set properly");
        }*/

        return path;
    }

    /** findclasspath
     *
     */
    public RubyString findClassPath() {
        FindClassPathResult arg = new FindClassPathResult();
        arg.klass = this;
        arg.track = getRuby().getClasses().getObjectClass();
        arg.prev = null;

        if (getRuby().getClasses().getObjectClass().getInstanceVariables() != null) {
            getRuby().getClasses().getObjectClass().getInstanceVariables().foreach(new FindClassPathMapMethod(), arg);
        }

        if (arg.name == null) {
            getRuby().getClasses().getClassMap().foreach(new FindClassPathMapMethod(), arg);
        }

        if (arg.name != null) {
            getInstanceVariables().put("__classpath__", arg.path);
            return arg.path;
        }
        return RubyString.nilString(getRuby());
    }

    /** include_class_new
     *
     */
    public RubyIncludedClass newIncludeClass(RubyClass superClass) {
        return new RubyIncludedClass(getRuby(), superClass, this);
    }

    public void setName(String name) {
        getInstanceVariables().put("__classid__", name);
    }

    /** rb_set_class_path
     *
     */
    public void setClassPath(RubyModule under, String name) {
        RubyString value = null;

        if (under == getRuby().getClasses().getObjectClass()) {
            value = RubyString.newString(getRuby(), name);
        } else {
            value = (RubyString) under.getClassPath().dup();
            value.cat("::");
            value.cat(name);
        }

        getInstanceVariables().put("__classpath__", value);
    }

    /** rb_class_path
     *
     */
    public RubyString getClassPath() {
        RubyString path = getClassname();

        if (path != null) {
            return path;
        }

        String s = "Module";
        if (isClass()) {
            s = "Class";
        }

        return RubyString.newString(getRuby(), "<" + s + " 01x" + Integer.toHexString(System.identityHashCode(this)) + ">");
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
    public void setClassVar(String name, RubyObject value) {
        RubyModule tmp = this;
        while (tmp != null) {
            if (tmp.getInstanceVariables() != null && tmp.getInstanceVariables().get(name) != null) {
                if (tmp.isTaint() && getRuby().getSafeLevel() >= 4) {
                    throw new RubySecurityException(getRuby(), "Insecure: can't modify class variable");
                }
                tmp.getInstanceVariables().put(name, value);
                return;
            }
            tmp = tmp.getSuperClass();
        }
        throw new NameError(getRuby(), "uninitialized class variable " + name + " in " + toName());
    }

    /** rb_cvar_declare
     *
     */
    public void declareClassVar(String name, RubyObject value) {
        RubyModule tmp = this;
        while (tmp != null) {
            if (tmp.getInstanceVariables() != null && tmp.getInstanceVariables().get(name) != null) {
                if (tmp.isTaint() && getRuby().getSafeLevel() >= 4) {
                    throw new RubySecurityException(getRuby(), "Insecure: can't modify class variable");
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
    public RubyObject getClassVar(String name) {
        RubyModule tmp = this;
        while (tmp != null) {
            if (tmp.getInstanceVariables() != null && tmp.getInstanceVariables().get(name) != null) {
                return (RubyObject) tmp.getInstanceVariables().get(name);
            }
            tmp = tmp.getSuperClass();
        }
        throw new NameError(getRuby(), "uninitialized class variable " + name + " in " + toName());
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
    public void setConstant(String name, RubyObject value) {
        setAv(name, value, true);
    }

    /** rb_const_get
     *
     */
    public RubyObject getConstant(String name) {
        boolean mod_retry = false;
        RubyModule tmp = this;

        while (true) {
            while (tmp != null) {
                if (tmp.getInstanceVariables().get(name) != null) {
                    return (RubyObject) tmp.getInstanceVariables().get(name);
                }
                if (tmp == getRuby().getClasses().getObjectClass() && getRuby().getTopConstant(name) != null) {
                    return getRuby().getTopConstant(name);
                }
                tmp = tmp.getSuperClass();
            }
            if (!mod_retry && isModule()) {
                mod_retry = true;
                tmp = getRuby().getClasses().getObjectClass();
                continue;
            }
            break;
        }

        // Now try to load a Java class
        String javaClassName = (String) getRuby().getJavaSupport().getRenamedJavaClasses().get(name);
        if (javaClassName == null) {
            javaClassName = name;
        }

        try {
            Class javaClass = getRuby().getJavaSupport().loadJavaClass(RubyString.newString(getRuby(), javaClassName));
            return getRuby().getJavaSupport().loadClass(javaClass, null);
        } catch (NameError excptn) {
        }

        /* Uninitialized constant */
        if (this != getRuby().getClasses().getObjectClass()) {
            throw new NameError(getRuby(), "uninitialized constant " + name + " at " + getClassPath().getValue());
        } else {
            throw new NameError(getRuby(), "uninitialized constant " + name);
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
    public void includeModule(RubyObject arg) {
        testFrozen();
        if (!isTaint()) {
            ruby.secure(4);
        }
        if (arg == null || arg == this) {
            return;
        }

        if (!(arg instanceof RubyModule)) {
            throw new TypeError(ruby, "Wrong argument type " + arg.getRubyClass().toName() + " (expected Module).");
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
    protected void setAv(String name, RubyObject value, boolean constant) {
        String dest = constant ? "constant" : "class variable";

        if (!isTaint() && getRuby().getSafeLevel() >= 4) {
            throw new RubySecurityException(getRuby(), "Insecure: can't set " + dest);
        }
        if (isFrozen()) {
            throw new RubyFrozenException(getRuby(), "class/module");
        }
        if (constant && (getInstanceVariables().get(name) != null)) {
            //getRuby().warn("already initialized " + dest + " " + name);
        }

        if (getInstanceVariables() == null) {
            setInstanceVariables(new RubyHashMap());
        }

        getInstanceVariables().put(name, value);
    }

    /** rb_add_method
     *
     */
    public void addMethod(String name, IMethod method, int noex) {
        if (this == getRuby().getClasses().getObjectClass()) {
            getRuby().secure(4);
        }

        if (getRuby().getSafeLevel() >= 4 && !isTaint()) {
            throw new RubySecurityException(getRuby(), "Insecure: can't define method");
        }
        if (isFrozen()) {
            throw new RubyFrozenException(getRuby(), "class/module");
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
            throw new RubyFrozenException(getRuby(), desc);
        }
    }

    /** rb_undef
     *
     */
    public void undef(String name) {
        Ruby ruby = getRuby();
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
        IMethod method = searchMethod(name);
        if (method == null) {
            String s0 = " class";
            RubyModule c = this;

            if (c.isSingleton()) {
                RubyObject obj = getInstanceVar("__attached__");

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
            if (tmp.getInstanceVariables() != null && tmp.getInstanceVariables().get(name) != null) {
                return true;
            }
        }

        if (isModule()) {
            return getRuby().getClasses().getObjectClass().isConstantDefined(name);
        }

        if (getRuby().isClassDefined(name)) {
            return true;
        }

        return getRuby().isAutoloadDefined(name);
    }

    /**
     * 
     * MRI: rb_const_defined_at
     * 
     */
    public boolean isConstantDefinedAt(String name) {
        if (getInstanceVariables().containsKey(name)) {
            return true;
        } else if (this == ruby.getClasses().getObjectClass()) {
            return isConstantDefined(name);
        }
        return false;
    }

    /** search_method
     *
     */
    public IMethod searchMethod(String name) {
        IMethod method = (IMethod) getMethods().get(name);
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

    /** rb_get_method_body
     *
     */
    public GetMethodBodyResult getMethodBody(String name, int noex) {
        GetMethodBodyResult result = new GetMethodBodyResult(this, name, noex);

        IMethod method = searchMethod(name);

        if (method == null) {
            // System.out.println("Cant find method \"" + name + "\" in class " + toName());

            getRuby().getMethodCache().saveUndefinedEntry(this, name);

            return null;
        }

        CacheEntry ent = new CacheEntry(this, method.getNoex());

        if (method instanceof AliasMethod) {
            ent.setName(name);
            ent.setOrigin(((AliasMethod) method).getOrigin());
            ent.setOriginalName(((AliasMethod) method).getOldName());
            ent.setMethod(((AliasMethod) method).getOldMethod());

            result.setRecvClass(((AliasMethod) method).getOrigin());
            result.setId(((AliasMethod) method).getOldName());

            method = ((AliasMethod) method).getOldMethod();
        } else {
            ent.setName(name);
            ent.setOriginalName(name);
            ent.setOrigin(method.getImplementationClass());
            ent.setMethod(method);

            result.setRecvClass(method.getImplementationClass());
        }

        getRuby().getMethodCache().saveEntry(this, name, ent);

        result.setNoex(ent.getNoex());
        result.setMethod(method);
        return result;
    }

    /** rb_call
     *
     */
    public RubyObject call(RubyObject recv, String name, RubyPointer args, int scope) {
        CacheEntry ent = getRuby().getMethodCache().getEntry(this, name);

        RubyModule klass = this;
        int noex;
        IMethod method;

        if (ent != null) {
            if (ent.getMethod() == null) {
                RubyPointer newArgs = new RubyPointer();
                newArgs.add(RubySymbol.newSymbol(getRuby(), name));
                if (args != null) {
                    newArgs.addAll(args);
                }
                return recv.funcall("method_missing", newArgs);
            }

            klass = ent.getOrigin();
            name = ent.getOriginalName();
            noex = ent.getNoex();
            method = ent.getMethod();
        } else {
            GetMethodBodyResult gmbr = getMethodBody(name, 0);

            if (gmbr == null || gmbr.getMethod() == null) {
                if (scope == 3) {
                    throw new NameError(getRuby(), "super: no superclass method '" + name + "'");
                }
                RubyPointer newArgs = new RubyPointer();
                newArgs.add(RubySymbol.newSymbol(getRuby(), name));
                if (args != null) {
                    newArgs.addAll(args);
                }
                return recv.funcall("method_missing", newArgs);
            }

            klass = gmbr.getRecvClass();
            name = gmbr.getId();
            noex = gmbr.getNoex();
            method = gmbr.getMethod();
        }

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
    public RubyObject call0(RubyObject recv, String name, RubyPointer args, IMethod method, boolean noSuper) {

        // ...
        Ruby ruby = getRuby();
        if (ruby.getActIter().isPre()) {
            ruby.getIterStack().push(Iter.ITER_CUR);
        } else {
            ruby.getIterStack().push(Iter.ITER_NOT);
        }

        ruby.getFrameStack().push();
        ruby.getActFrame().setLastFunc(name);
        ruby.getActFrame().setLastClass(noSuper ? null : this);
        ruby.getActFrame().setSelf(recv);
        ruby.getActFrame().setArgs(args);

        try {
            return method.execute(ruby, recv, name, args != null ? args.toRubyArray() : new RubyObject[0], noSuper);
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

        if (this == getRuby().getClasses().getObjectClass()) {
            getRuby().secure(4);
        }

        IMethod method = searchMethod(oldName);

        RubyModule origin = null;

        if (method == null) {
            if (isModule()) {
                method = getRuby().getClasses().getObjectClass().searchMethod(oldName);
            }
            if (method == null) {
                throw new NameError(ruby, "undefined method '" + name + "' for " + (isModule() ? "module" : "class") + " '" + toName() + "'");
            }
        }
        origin = method.getImplementationClass();

        if (method instanceof AliasMethod) { /* was alias */
            oldName = ((AliasMethod) method).getOldName();
            origin = ((AliasMethod) method).getOrigin();
            method = ((AliasMethod) method).getOldMethod();
        }

        getRuby().getMethodCache().clearByName(name);
        getMethods().put(name, new AliasMethod(method, oldName, origin, method.getNoex()));
    }

    /** remove_method
     * 
     */
    public void removeMethod(String name) {
        if (this == getRuby().getClasses().getObjectClass()) {
            getRuby().secure(4);
        }
        if (getRuby().getSafeLevel() >= 4 && !isTaint()) {
            throw new RubySecurityException(getRuby(), "Insecure: can't remove method");
        }
        if (isFrozen()) {
            // rb_error_frozen("class/module");
        }
        if (getMethods().remove(name) == null) {
            throw new NameError(getRuby(), "method '" + name + "' not defined in " + toName());
        }

        getRuby().getMethodCache().clearByName(name);
    }

    /** rb_define_class_under
     *
     */
    public RubyClass defineClassUnder(String name, RubyClass superClass) {
        if (isConstantDefinedAt(name)) {
            RubyObject type = getConstant(name);
            if (!(type instanceof RubyClass)) {
                throw new TypeError(ruby, name + " is not a class.");
            } else if (((RubyClass) type).getSuperClass().getRealClass() != superClass) {
                throw new NameError(ruby, name + " is already defined.");
            } else {
                return (RubyClass) type;
            }
        } else {
            RubyClass newClass = getRuby().defineClass(name, superClass);

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
            RubyObject module = getConstant(name);
            if (!(module instanceof RubyModule)) {
                throw new TypeError(ruby, toName() + "::" + module.getRubyClass().toName() + " is not a module.");
            } else {
                return (RubyModule) module;
            }
        } else {
            RubyModule newModule = getRuby().defineModule(name);

            setConstant(name, newModule);
            newModule.setClassPath(this, name);

            return newModule;
        }
    }

    /** rb_class2name
     *
     */
    public String toName() {
        if (this == getRuby().getClasses().getNilClass()) {
            return "NilClass";
        }
        if (this == getRuby().getClasses().getTrueClass()) {
            return "TrueClass";
        }
        if (this == getRuby().getClasses().getFalseClass()) {
            return "FalseClass";
        }

        return ((RubyString) getClassPath()).getValue();
    }

    /** rb_define_const
     *
     */
    public void defineConstant(String name, RubyObject value) {
        if (this == getRuby().getClasses().getClassClass()) {
            getRuby().secure(4);
        }

        if (!IdUtil.isConstant(name)) {
            throw new NameError(getRuby(), "bad constant name " + name);
        }

        setConstant(name, value);
    }

    /** rb_mod_remove_cvar
     *
     */
    public RubyObject removeCvar(RubyObject name) { // Wrong Parameter ?
        if (!IdUtil.isClassVariable(name.toId())) {
            throw new NameError(getRuby(), "wrong class variable name " + name.toId());
        }

        if (!isTaint() && getRuby().getSafeLevel() >= 4) {
            throw new RubySecurityException(getRuby(), "Insecure: can't remove class variable");
        }

        if (isFrozen()) {
            throw new RubyFrozenException(getRuby(), "class/module");
        }

        RubyObject value = (RubyObject) getInstanceVariables().remove(name.toId());

        if (value != null) {
            return value;
        }

        if (isClassVarDefined(name.toId())) {
            throw new NameError(getRuby(), "cannot remove " + name.toId() + " for " + toName());
        }

        throw new NameError(getRuby(), "class variable " + name.toId() + " not defined for " + toName());
    }

    /** rb_define_class_variable
     *
     */
    public void defineClassVariable(String name, RubyObject value) {
        if (!IdUtil.isClassVariable(name)) {
            throw new NameError(getRuby(), "wrong class variable name " + name);
        }

        declareClassVar(name, value);
    }

    /** rb_attr
     *
     */
    public void addAttribute(String name, boolean read, boolean write, boolean ex) {
        int noex = Constants.NOEX_PUBLIC;

        if (ex) {
            if (getRuby().getActMethodScope() == Constants.SCOPE_PRIVATE) {
                noex = Constants.NOEX_PRIVATE;
            } else if (getRuby().getActMethodScope() == Constants.SCOPE_PROTECTED) {
                noex = Constants.NOEX_PROTECTED;
            } else {
                noex = Constants.NOEX_PUBLIC;
            }
        }

        String attrIV = "@" + name;

        if (read) {
            addMethod(name, new EvaluateMethod(new InstVarNode(null, attrIV)), noex);
            funcall("method_added", RubySymbol.newSymbol(getRuby(), name));
        }

        if (write) {
            name = name + "=";
            addMethod(name, new EvaluateMethod(new AttrSetNode(null, attrIV)), noex);
            funcall("method_added", RubySymbol.newSymbol(getRuby(), name));
        }
    }

    /** method_list
     *
     */
    public RubyArray methodList(boolean option, RubyMapMethod method) {
        RubyArray ary = RubyArray.newArray(getRuby());

        for (RubyModule klass = this; klass != null; klass = klass.getSuperClass()) {
            klass.getMethods().foreach(method, ary);
            if (!option) {
                break;
            }
        }

        Iterator iter = ary.getList().iterator();
        while (iter.hasNext()) {
            if (getRuby().getNil() == iter.next()) {
                iter.remove();
                iter.next();
            }
        }

        return ary;
    }

    public RubyArray getConstOf(RubyArray ary) {
        RubyModule klass = this;
        while (klass != null) {
            // +++ jpetersen
            klass.getConstAt(ary);
            // ---
            klass = klass.getSuperClass();
        }
        return ary;
    }

    public RubyArray getConstAt(RubyArray ary) {
        constantNamesToArray(ary, getInstanceVariables());

        if (this == getRuby().getClasses().getObjectClass()) {
            constantNamesToArray(ary, getRuby().getClasses().getClassMap());
            /*if (autoload_tbl) {
                st_foreach(autoload_tbl, autoload_i, ary);
            }*/
        }
        return ary;
    }

    private void constantNamesToArray(RubyArray ary, Map map) {
        Iterator iter = map.keySet().iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            if (IdUtil.isConstant(key)) {
                RubyString name = RubyString.newString(getRuby(), (String) key);
                if (ary.includes(name).isFalse()) {
                    ary.push(name);
                }
            }
        }
    }

    /** set_method_visibility
     *
     */
    public void setMethodVisibility(RubyObject[] methods, int noex) {
        if (getRuby().getSafeLevel() >= 4 && !isTaint()) {
            throw new RubySecurityException(getRuby(), "Insecure: can't change method visibility");
        }

        for (int i = 0; i < methods.length; i++) {
            exportMethod(methods[i].toId(), noex);
        }
    }

    /** rb_export_method
     *
     */
    public void exportMethod(String name, int noex) {
        if (this == getRuby().getClasses().getObjectClass()) {
            getRuby().secure(4);
        }

        IMethod method = searchMethod(name);

        if (method == null && isModule()) {
            method = getRuby().getClasses().getObjectClass().searchMethod(name);
        }

        if (method == null) {
            throw new NameError(ruby, "undefined method '" + name + "' for " + (isModule() ? "module" : "class") + " '" + toName() + "'");
        }

        if (method.getNoex() != noex) {
            if (this == method.getImplementationClass()) {
                method.setNoex(noex);
            } else {
                addMethod(name, new EvaluateMethod(new ZSuperNode(null)), noex);
            }
        }
    }

    /** 
     * MRI: rb_method_boundp
     * 
     */
    public boolean isMethodBound(String name, int ex) {
        CacheEntry entry = getRuby().getMethodCache().getEntry(this, name);

        if (entry != null) {
            if (ex != 0 && (entry.getNoex() & Constants.NOEX_PRIVATE) != 0) {
                return false;
            } else if (entry.getMethod() == null) {
                return false;
            } else {
                return true;
            }
        }

        GetMethodBodyResult gmbr = getMethodBody(name, ex);

        if (gmbr != null) {
            if (ex != 0 && (gmbr.getNoex() & Constants.NOEX_PRIVATE) != 0) {
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

    public RubyObject newMethod(RubyObject recv, String name, RubyClass methodClass) {
        RubyClass originalClass = (RubyClass) this;
        String originalName = name;

        GetMethodBodyResult gmbr = getMethodBody(name, 0);
        if (gmbr == null) {
            // printUndef();
            return getRuby().getNil();
        }

        while (gmbr.getMethod() instanceof EvaluateMethod && ((EvaluateMethod) gmbr.getMethod()).getNode() instanceof ZSuperNode) {
            gmbr = gmbr.getRecvClass().getSuperClass().getMethodBody(gmbr.getId(), 0);
            if (gmbr == null) {
                // printUndef();
                return getRuby().getNil();
            }
        }

        RubyMethod newMethod = new RubyMethod(getRuby(), methodClass);
        newMethod.setReceiverClass((RubyClass) gmbr.getRecvClass());
        newMethod.setReceiver(recv);
        newMethod.setMethodId(gmbr.getId());
        newMethod.setMethod(gmbr.getMethod());
        newMethod.setOriginalClass(originalClass);
        newMethod.setOriginalId(originalName);

        return newMethod;
    }

    public RubyObject executeUnder(Callback method, RubyObject[] args) {
        ruby.pushClass(this);

        Frame frame = ruby.getActFrame();
        ruby.getFrameStack().push();
        ruby.getActFrame().setLastFunc(frame.getLastFunc());
        ruby.getActFrame().setLastClass(frame.getLastClass());
        ruby.getActFrame().setArgs(frame.getArgs());
        if (ruby.getCBase() != this) {
            ruby.getActFrame().setNamespace(new Namespace(this, ruby.getActFrame().getNamespace()));
        }
        ruby.setNamespace(new Namespace(this, ruby.getNamespace()));

        // mode = scope_vmode;
        // SCOPE_SET(SCOPE_PUBLIC);

        try {
            return method.execute(this, args, getRuby());
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
        RubyString path = getClassname();
        if (path != null) {
            return (RubyString) path.dup();
        }
        return RubyString.newString(getRuby(), "");
    }

    /** rb_mod_class_variables
     *
     */
    public RubyArray class_variables() {
        RubyArray ary = RubyArray.newArray(getRuby());

        RubyModule rbModule = this;

        while (rbModule != null) {
            if (rbModule.getInstanceVariables() != null) {
                Iterator iter = rbModule.getInstanceVariables().keySet().iterator();
                while (iter.hasNext()) {
                    String id = (String) iter.next();
                    if (IdUtil.isClassVariable(id)) {
                        RubyString kval = RubyString.newString(getRuby(), id);
                        if (ary.includes(kval).isFalse()) {
                            ary.push(kval);
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
    public RubyObject rbClone() {
        RubyModule clone = (RubyModule)super.rbClone();

        // clone the methods.
        if (getMethods() != null) {
            // clone.setMethods(new RubyHashMap());
            getMethods().foreach(new RubyMapMethod() {
                public int execute(Object key, Object value, Object arg) {
                    IMethod method = (IMethod) value;

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
    public RubyObject dup() {
        RubyModule dup = (RubyModule) rbClone();
        dup.setRubyClass(getRubyClass());

        // +++ jpetersen
        // dup.setSingleton(isSingleton());
        // --- jpetersen

        return dup;
    }

    /** rb_mod_included_modules
     *
     */
    public RubyArray included_modules() {
        RubyArray ary = RubyArray.newArray(getRuby());

        for (RubyModule p = getSuperClass(); p != null; p = p.getSuperClass()) {
            if (p.isIncluded()) {
                ary.push(((RubyIncludedClass)p).getDelegate());
            }
        }

        return ary;
    }

    /** rb_mod_ancestors
     *
     */
    public RubyArray ancestors() {
        RubyArray ary = RubyArray.newArray(getRuby());

        for (RubyModule p = this; p != null; p = p.getSuperClass()) {
            if (p.isSingleton()) {
                continue;
            }

            if (p.isIncluded()) {
                ary.push(((RubyIncludedClass) p).getDelegate());
            } else {
                ary.push(p);
            }
        }

        return ary;
    }

    /** rb_mod_to_s
     *
     */
    public RubyString to_s() {
        return (RubyString) getClassPath().dup();
    }

    /** rb_mod_eqq
     *
     */
    public RubyBoolean op_eqq(RubyObject obj) {
        return obj.kind_of(this);
    }

    /** rb_mod_le
     *
     */
    public RubyBoolean op_le(RubyObject obj) {
        if (!(obj instanceof RubyModule)) {
            throw new TypeError(getRuby(), "compared with non class/module");
        }

        RubyModule mod = this;
        while (mod != null) {
            if (mod.getMethods() == ((RubyModule) obj).getMethods()) {
                return getRuby().getTrue();
            }
            mod = mod.getSuperClass();
        }

        return getRuby().getFalse();
    }

    /** rb_mod_lt
     *
     */
    public RubyBoolean op_lt(RubyObject obj) {
        if (obj == this) {
            return getRuby().getFalse();
        }
        return op_le(obj);
    }

    /** rb_mod_ge
     *
     */
    public RubyBoolean op_ge(RubyObject obj) {
        if (!(obj instanceof RubyModule)) {
            throw new TypeError(getRuby(), "compared with non class/module");
        }

        return ((RubyModule) obj).op_le(this);
    }

    /** rb_mod_gt
     *
     */
    public RubyBoolean op_gt(RubyObject obj) {
        if (this == obj) {
            return getRuby().getFalse();
        }
        return op_ge(obj);
    }

    /** rb_mod_cmp
     *
     */
    public RubyFixnum op_cmp(RubyObject obj) {
        if (this == obj) {
            return RubyFixnum.newFixnum(getRuby(), 0);
        }

        if (!(obj instanceof RubyModule)) {
            throw new TypeError(getRuby(), "<=> requires Class or Module (" + getRubyClass().toName() + " given)");
        }

        if (op_le(obj).isTrue()) {
            return RubyFixnum.newFixnum(getRuby(), -1);
        }

        return RubyFixnum.newFixnum(getRuby(), 1);
    }

    /** rb_mod_initialize
     *
     */
    public RubyObject initialize(RubyObject[] args) {
        return getRuby().getNil();
    }

    /** rb_module_s_new
     *
     */
    public static RubyModule newModule(Ruby ruby, RubyObject recv) {
        RubyModule mod = RubyModule.newModule(ruby);

        mod.setRubyClass((RubyClass) recv);
        ruby.getClasses().getModuleClass().callInit(null);

        return mod;
    }

    /** Return an array of nested modules or classes.
     * 
     * rb_mod_nesting
     *
     */
    public static RubyArray nesting(Ruby ruby, RubyObject recv) {
        Namespace ns = ruby.getActFrame().getNamespace();
        
        RubyArray ary = RubyArray.newArray(ruby);

        while (ns != null && ns.getParent() != null) {
            if (!ns.getNamespaceModule().isNil()) {
                ary.push(ns.getNamespaceModule());
            }
            
            ns = ns.getParent();
        }

        return ary;
    }

    /** rb_mod_attr
     *
     */
    public RubyObject attr(RubyObject symbol, RubyObject[] args) {
        boolean writeable = false;
        if (args.length > 0) {
            writeable = args[0].isTrue();
        }

        addAttribute(symbol.toId(), true, writeable, true);

        return getRuby().getNil();
    }

    /** rb_mod_attr_reader
     *
     */
    public RubyObject attr_reader(RubyObject[] args) {
        for (int i = 0; i < args.length; i++) {
            addAttribute(((RubySymbol) args[i]).toId(), true, false, true);
        }

        return getRuby().getNil();
    }

    /** rb_mod_attr_writer
     *
     */
    public RubyObject attr_writer(RubyObject[] args) {
        for (int i = 0; i < args.length; i++) {
            addAttribute(((RubySymbol) args[i]).toId(), false, true, true);
        }

        return getRuby().getNil();
    }

    /** rb_mod_attr_accessor
     *
     */
    public RubyObject attr_accessor(RubyObject[] args) {
        for (int i = 0; i < args.length; i++) {
            addAttribute(((RubySymbol) args[i]).toId(), true, true, true);
        }

        return getRuby().getNil();
    }

    /** rb_mod_const_get
     *
     */
    public RubyObject const_get(RubyObject symbol) {
        String name = symbol.toId();

        if (!IdUtil.isConstant(name)) {
            throw new NameError(getRuby(), "wrong constant name " + name);
        }

        return getConstant(name);
    }

    /** rb_mod_const_set
     *
     */
    public RubyObject const_set(RubyObject symbol, RubyObject value) {
        String name = symbol.toId();

        if (!IdUtil.isConstant(name)) {
            throw new NameError(getRuby(), "wrong constant name " + name);
        }

        setConstant(name, value);

        return value;
    }

    /** rb_mod_const_defined
     *
     */
    public RubyBoolean const_defined(RubyObject symbol) {
        String name = symbol.toId();

        if (!IdUtil.isConstant(name)) {
            throw new NameError(getRuby(), "wrong constant name " + name);
        }

        return RubyBoolean.newBoolean(getRuby(), isConstantDefined(name));
    }

    /** rb_class_instance_methods
     *
     */
    public RubyArray instance_methods(RubyObject[] args) {
        boolean includeSuper = false;

        if (args.length > 0) {
            includeSuper = args[0].isTrue();
        }

        return methodList(includeSuper, new RubyMapMethod() {
            public int execute(Object key, Object value, Object arg) {
                // cast args
                String id = (String) key;
                IMethod method = (IMethod) value;
                RubyArray ary = (RubyArray) arg;

                if ((method.getNoex() & (Constants.NOEX_PRIVATE | Constants.NOEX_PROTECTED)) == 0) {
                    RubyString name = RubyString.newString(getRuby(), id);

                    if (ary.includes(name).isFalse()) {
                        if (method == null) {
                            ary.push(getRuby().getNil());
                        }
                        ary.push(name);
                    }
                } else if (method instanceof EvaluateMethod && ((EvaluateMethod) method).getNode() instanceof ZSuperNode) {
                    ary.push(getRuby().getNil());
                    ary.push(RubyString.newString(getRuby(), id));
                }
                return RubyMapMethod.CONTINUE;
            }
        });
    }

    /** rb_class_protected_instance_methods
     *
     */
    public RubyArray protected_instance_methods(RubyObject[] args) {
        boolean includeSuper = false;

        if (args.length > 0) {
            includeSuper = args[0].isTrue();
        }

        return methodList(includeSuper, new RubyMapMethod() {
            public int execute(Object key, Object value, Object arg) {
                // cast args
                String id = (String) key;
                IMethod method = (IMethod) value;
                RubyArray ary = (RubyArray) arg;

                if (method == null) {
                    ary.push(getRuby().getNil());
                    ary.push(RubyString.newString(getRuby(), id));
                } else if ((method.getNoex() & Constants.NOEX_PROTECTED) != 0) {
                    RubyString name = RubyString.newString(getRuby(), id);

                    if (ary.includes(name).isFalse()) {
                        ary.push(name);
                    }
                } else if (method instanceof EvaluateMethod && ((EvaluateMethod) method).getNode() instanceof ZSuperNode) {
                    ary.push(getRuby().getNil());
                    ary.push(RubyString.newString(getRuby(), id));
                }
                return RubyMapMethod.CONTINUE;
            }
        });
    }

    /** rb_class_private_instance_methods
     *
     */
    public RubyArray private_instance_methods(RubyObject[] args) {
        boolean includeSuper = false;

        if (args.length > 0) {
            includeSuper = args[0].isTrue();
        }

        return methodList(includeSuper, new RubyMapMethod() {
            public int execute(Object key, Object value, Object arg) {
                // cast args
                String id = (String) key;
                IMethod method = (IMethod) value;
                RubyArray ary = (RubyArray) arg;

                if (method == null) {
                    ary.push(getRuby().getNil());
                    ary.push(RubyString.newString(getRuby(), id));
                } else if ((method.getNoex() & Constants.NOEX_PRIVATE) != 0) {
                    RubyString name = RubyString.newString(getRuby(), id);

                    if (ary.includes(name).isFalse()) {
                        ary.push(name);
                    }
                } else if (method instanceof EvaluateMethod && ((EvaluateMethod) method).getNode() instanceof ZSuperNode) {
                    ary.push(getRuby().getNil());
                    ary.push(RubyString.newString(getRuby(), id));
                }
                return RubyMapMethod.CONTINUE;
            }
        });
    }

    /** rb_mod_constants
     *
     */
    public RubyArray constants() {
        RubyArray ary = RubyArray.newArray(getRuby());

        return getConstOf(ary);
    }

    /** rb_mod_remove_cvar
     *
     */
    public RubyObject remove_class_variable(RubyObject name) {
        String id = name.toId();

        if (!IdUtil.isClassVariable(id)) {
            throw new NameError(getRuby(), "wrong class variable name " + id);
        }
        if (!isTaint() && getRuby().getSafeLevel() >= 4) {
            throw new RubySecurityException(getRuby(), "Insecure: can't remove class variable");
        }
        if (isFrozen()) {
            throw new RubyFrozenException(getRuby(), "class/module");
        }

        if (getInstanceVariables() != null) {
            Object value = getInstanceVariables().remove(id);
            if (value != null) {
                return (RubyObject) value;
            }
        }

        if (isClassVarDefined(id)) {
            throw new NameError(getRuby(), "cannot remove " + id + " for " + toName());
        }
        throw new NameError(getRuby(), "class variable " + id + " not defined for " + toName());
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
    public RubyObject extend_object(RubyObject obj) {
        obj.extendObject(this);
        return obj;
    }

    /** rb_mod_include
     *
     */
    public RubyModule include(RubyObject[] modules) {
        for (int i = 0; i < modules.length; i++) {
            modules[i].funcall("append_features", this);
        }

        return this;
    }

    /** rb_mod_public
     *
     */
    public RubyModule rbPublic(RubyObject[] args) {
        if (getRuby().getSafeLevel() >= 4 && !isTaint()) {
            throw new RubySecurityException(getRuby(), "Insecure: can't change method visibility");
        }

        if (args.length == 0) {
            getRuby().setActMethodScope(Constants.SCOPE_PUBLIC);
        } else {
            setMethodVisibility(args, Constants.NOEX_PUBLIC);
        }

        return this;
    }

    /** rb_mod_protected
     *
     */
    public RubyModule rbProtected(RubyObject[] args) {
        if (getRuby().getSafeLevel() >= 4 && !isTaint()) {
            throw new RubySecurityException(getRuby(), "Insecure: can't change method visibility");
        }

        if (args.length == 0) {
            getRuby().setActMethodScope(Constants.SCOPE_PROTECTED);
        } else {
            setMethodVisibility(args, Constants.NOEX_PROTECTED);
        }

        return this;
    }

    /** rb_mod_private
     *
     */
    public RubyModule rbPrivate(RubyObject[] args) {
        if (getRuby().getSafeLevel() >= 4 && !isTaint()) {
            throw new RubySecurityException(getRuby(), "Insecure: can't change method visibility");
        }

        if (args.length == 0) {
            getRuby().setActMethodScope(Constants.SCOPE_PRIVATE);
        } else {
            setMethodVisibility(args, Constants.NOEX_PRIVATE);
        }

        return this;
    }

    /** rb_mod_modfunc
     *
     */
    public RubyModule module_function(RubyObject[] args) {
        if (getRuby().getSafeLevel() >= 4 && !isTaint()) {
            throw new RubySecurityException(getRuby(), "Insecure: can't change method visibility");
        }

        if (args.length == 0) {
            getRuby().setActMethodScope(Constants.SCOPE_MODFUNC);
        } else {
            setMethodVisibility(args, Constants.NOEX_PRIVATE);

            for (int i = 0; i < args.length; i++) {
                String name = args[i].toId();
                IMethod method = searchMethod(name);
                if (method == null) {
                    throw new RubyBugException("undefined method '" + name + "'; can't happen");
                }
                getSingletonClass().addMethod(name, method, Constants.NOEX_PUBLIC);
                funcall("singleton_method_added", RubySymbol.newSymbol(getRuby(), name));
            }
        }

        return this;
    }

    public RubyBoolean method_defined(RubyObject symbol) {
        return isMethodBound(symbol.toId(), 1) ? getRuby().getTrue() : getRuby().getFalse();
    }

    public RubyModule public_class_method(RubyObject[] args) {
        getRubyClass().setMethodVisibility(args, Constants.NOEX_PUBLIC);

        return this;
    }

    public RubyModule private_class_method(RubyObject[] args) {
        getRubyClass().setMethodVisibility(args, Constants.NOEX_PRIVATE);

        return this;
    }

    public RubyModule alias_method(RubyObject newId, RubyObject oldId) {
        aliasMethod(newId.toId(), oldId.toId());

        return this;
    }

    public RubyModule undef_method(RubyObject name) {
        undef(name.toId());

        return this;
    }

    public RubyObject module_eval(RubyObject[] args) {
        return specificEval(this, args);
    }

    public RubyModule remove_method(RubyObject name) {
        removeMethod(name.toId());

        return this;
    }


    public void marshalTo(MarshalStream output) throws java.io.IOException {
	output.write('m');
	output.dumpString(name().toString());
    }


    private static class FindClassPathResult {
        public String name;
        public RubyModule klass;
        public RubyString path;
        public RubyObject track;
        public FindClassPathResult prev;
    }

    private class FindClassPathMapMethod implements RubyMapMethod {
        public int execute(Object _key, Object _value, Object _res) {
            // Cast the values.
            String key = (String) _key;
            if (_value instanceof String || _value == null) {
                return RubyMapMethod.CONTINUE;
            }
            RubyObject value = (RubyObject) _value;
            FindClassPathResult res = (FindClassPathResult) _res;

            RubyString path = null;

            if (!IdUtil.isConstant(key)) {
                return RubyMapMethod.CONTINUE;
            }

            if (res.path != null) {
                path = (RubyString) res.path.dup();
                path.cat("::");
                path.cat(key);
            } else {
                path = RubyString.newString(getRuby(), key);
            }

            if (value == res.klass) {
                res.name = key;
                res.path = path;
                return RubyMapMethod.STOP;
            }

            if (value.kind_of(getRuby().getClasses().getModuleClass()).isTrue()) {
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

    public static class GetMethodBodyResult {
        private IMethod method;
        private RubyModule recvClass;
        private String id;
        private int noex;

        public GetMethodBodyResult(RubyModule recvClass, String id, int noex) {
            this.recvClass = recvClass;
            this.id = id;
            this.noex = noex;
        }

        /** Getter for property id.
         * @return Value of property id.
         */
        public String getId() {
            return id;
        }

        /** Setter for property id.
         * @param id New value of property id.
         */
        public void setId(String id) {
            this.id = id;
        }

        /** Getter for property klass.
         * @return Value of property klass.
         */
        public RubyModule getRecvClass() {
            return recvClass;
        }

        /** Setter for property klass.
         * @param klass New value of property klass.
         */
        public void setRecvClass(RubyModule recvClass) {
            this.recvClass = recvClass;
        }

        /** Getter for property scope.
         * @return Value of property scope.
         */
        public int getNoex() {
            return noex;
        }

        /** Setter for property scope.
         * @param scope New value of property scope.
         */
        public void setNoex(int noex) {
            this.noex = noex;
        }
        /**
         * Gets the method.
         * @return Returns a IMethod
         */
        public IMethod getMethod() {
            return method;
        }

        /**
         * Sets the method.
         * @param method The method to set
         */
        public void setMethod(IMethod method) {
            this.method = method;
        }

    }

}
