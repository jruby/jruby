/*
 * RubyModule.java - No description
 * Created on 09. Juli 2001, 21:38
 *
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Stefan Matthias Aust <sma@3plus4.de>
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
import org.jruby.nodes.*;
import org.jruby.nodes.types.*;
import org.jruby.runtime.*;
import org.jruby.util.*;

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

    public static void createModuleClass(RubyClass moduleClass) {
        Callback op_eqq = new ReflectionCallbackMethod(RubyModule.class, "op_eqq", RubyObject.class);
        Callback op_cmp = new ReflectionCallbackMethod(RubyModule.class, "op_cmp", RubyObject.class);
        Callback op_lt = new ReflectionCallbackMethod(RubyModule.class, "op_lt", RubyObject.class);
        Callback op_le = new ReflectionCallbackMethod(RubyModule.class, "op_le", RubyObject.class);
        Callback op_gt = new ReflectionCallbackMethod(RubyModule.class, "op_gt", RubyObject.class);
        Callback op_ge = new ReflectionCallbackMethod(RubyModule.class, "op_ge", RubyObject.class);

        Callback clone = new ReflectionCallbackMethod(RubyModule.class, "rbClone");
        Callback dup = new ReflectionCallbackMethod(RubyModule.class, "dup");
        Callback to_s = new ReflectionCallbackMethod(RubyModule.class, "to_s");
        Callback included_modules = new ReflectionCallbackMethod(RubyModule.class, "included_modules");
        Callback name = new ReflectionCallbackMethod(RubyModule.class, "name");
        Callback ancestors = new ReflectionCallbackMethod(RubyModule.class, "ancestors");

        Callback attr = new ReflectionCallbackMethod(RubyModule.class, "attr", RubyObject.class, true);
        Callback attr_reader = new ReflectionCallbackMethod(RubyModule.class, "attr_reader", true);
        Callback attr_writer = new ReflectionCallbackMethod(RubyModule.class, "attr_writer", true);
        Callback attr_accessor = new ReflectionCallbackMethod(RubyModule.class, "attr_accessor", true);

        Callback newModule = new ReflectionCallbackMethod(RubyModule.class, "newModule", false, true);
        Callback initialize = new ReflectionCallbackMethod(RubyModule.class, "initialize", true);
        Callback instance_methods = new ReflectionCallbackMethod(RubyModule.class, "instance_methods", true);
        Callback public_instance_methods = new ReflectionCallbackMethod(RubyModule.class, "public_instance_methods", true);
        Callback protected_instance_methods =
            new ReflectionCallbackMethod(RubyModule.class, "protected_instance_methods", true);
        Callback private_instance_methods = new ReflectionCallbackMethod(RubyModule.class, "private_instance_methods", true);

        Callback constants = new ReflectionCallbackMethod(RubyModule.class, "constants");
        Callback const_get = new ReflectionCallbackMethod(RubyModule.class, "const_get", RubyObject.class);
        Callback const_set =
            new ReflectionCallbackMethod(RubyModule.class, "const_set", new Class[] { RubyObject.class, RubyObject.class });
        Callback const_defined = new ReflectionCallbackMethod(RubyModule.class, "const_defined", RubyObject.class);
        Callback class_variables = new ReflectionCallbackMethod(RubyModule.class, "class_variables");
        Callback remove_class_variable =
            new ReflectionCallbackMethod(RubyModule.class, "remove_class_variable", RubyObject.class);

        Callback append_features = new ReflectionCallbackMethod(RubyModule.class, "append_features", RubyModule.class);
        Callback extend_object = new ReflectionCallbackMethod(RubyModule.class, "extend_object", RubyObject.class);
        Callback include = new ReflectionCallbackMethod(RubyModule.class, "include", true);
        Callback rbPublic = new ReflectionCallbackMethod(RubyModule.class, "rbPublic", true);
        Callback rbProtected = new ReflectionCallbackMethod(RubyModule.class, "rbProtected", true);
        Callback rbPrivate = new ReflectionCallbackMethod(RubyModule.class, "rbPrivate", true);
        Callback module_function = new ReflectionCallbackMethod(RubyModule.class, "module_function", true);

        Callback method_defined = new ReflectionCallbackMethod(RubyModule.class, "method_defined", RubyObject.class);
        Callback public_class_method = new ReflectionCallbackMethod(RubyModule.class, "public_class_method", true);
        Callback private_class_method = new ReflectionCallbackMethod(RubyModule.class, "private_class_method", true);

        Callback module_eval = new ReflectionCallbackMethod(RubyModule.class, "module_eval", true);
        Callback remove_method = new ReflectionCallbackMethod(RubyModule.class, "remove_method", RubyObject.class);
        Callback undef_method = new ReflectionCallbackMethod(RubyModule.class, "undef_method", RubyObject.class);
        Callback alias_method =
            new ReflectionCallbackMethod(RubyModule.class, "alias_method", new Class[] { RubyObject.class, RubyObject.class });

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
        
        rb_define_singleton_method(rb_cModule, "nesting", rb_mod_nesting, 0);
        rb_define_singleton_method(rb_cModule, "constants", rb_mod_s_constants, 0);*/
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
                path = RubyString.newString(getRuby(), (String)getInstanceVariables().get("__classid__"));
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
                if (tmp.isTaint() && getRuby().getSecurityLevel() >= 4) {
                    throw new RubySecurityException(getRuby(), "Insecure: can't modify class variable");
                }
                tmp.getInstanceVariables().put(name, value);
            }
            tmp = tmp.getSuperClass();
        }
        throw new RubyNameException(getRuby(), "uninitialized class variable " + name + " in " + toName());
    }

    /** rb_cvar_declare
     *
     */
    public void declareClassVar(String name, RubyObject value) {
        RubyModule tmp = this;
        while (tmp != null) {
            if (tmp.getInstanceVariables() != null && tmp.getInstanceVariables().get(name) != null) {
                if (tmp.isTaint() && getRuby().getSecurityLevel() >= 4) {
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
        throw new RubyNameException(getRuby(), "uninitialized class variable " + name + " in " + toName());
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

        /* Uninitialized constant */
        if (this != getRuby().getClasses().getObjectClass()) {
            throw new RubyNameException(getRuby(), "uninitialized constant " + name + " at " + getClassPath().getValue());
        } else {
            throw new RubyNameException(getRuby(), "uninitialized constant " + name);
        }

        // return getRuby().getNil();
    }

    /** rb_include_module
     *
     */
    public void includeModule(RubyModule module) {
        if (module == null || module == this) {
            return;
        }

        RubyModule type = this;

        /* Fixed to Ruby 1.6.5 */
        addModule : while (module != null) {
            for (RubyModule includedModule = type.getSuperClass();
                includedModule != null;
                includedModule = includedModule.getSuperClass()) {
                if (includedModule.isIncluded() && includedModule.getMethods() == module.getMethods()) {
                    module = module.getSuperClass();
                    continue addModule;
                }
            }
            type.setSuperClass(module.newIncludeClass(type.getSuperClass()));
            type = type.getSuperClass();

            module = module.getSuperClass();
        }
    }

    /** mod_av_set
     *
     */
    protected void setAv(String name, RubyObject value, boolean constant) {
        String dest = constant ? "constant" : "class variable";

        if (!isTaint() && getRuby().getSecurityLevel() >= 4) {
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
    public void addMethod(String name, Node node, int noex) {
        if (this == getRuby().getClasses().getObjectClass()) {
            getRuby().secure(4);
        }

        if (getRuby().getSecurityLevel() >= 4 && !isTaint()) {
            throw new RubySecurityException(getRuby(), "Insecure: can't define method");
        }
        if (isFrozen()) {
            throw new RubyFrozenException(getRuby(), "class/module");
        }
        Node body = new NodeFactory(getRuby()).newMethod(node, noex);
        getMethods().put(name, body);
    }

    public void defineMethod(String name, Callback method) {
        int noex = (name.charAt(0) == 'i' && name.equals("initialize")) ? Constants.NOEX_PRIVATE : Constants.NOEX_PUBLIC;

        addMethod(name, new NodeFactory(getRuby()).newCFunc(method), noex | Constants.NOEX_CFUNC);
    }

    public void defineMethodId(String name, Callback method) {
        addMethod(name, new NodeFactory(getRuby()).newCFunc(method), Constants.NOEX_PUBLIC | Constants.NOEX_CFUNC);
    }

    public void defineProtectedMethod(String name, Callback method) {
        addMethod(name, new NodeFactory(getRuby()).newCFunc(method), Constants.NOEX_PROTECTED | Constants.NOEX_CFUNC);
    }

    public void definePrivateMethod(String name, Callback method) {
        addMethod(name, new NodeFactory(getRuby()).newCFunc(method), Constants.NOEX_PRIVATE | Constants.NOEX_CFUNC);
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
        if (this == getRuby().getClasses().getObjectClass()) {
            getRuby().secure(4);
        }
        if (getRuby().getSecurityLevel() >= 4 && !isTaint()) {
            throw new SecurityException("Insecure: can't undef");
        }
        testFrozen();
        if (name.equals("__id__") || name.equals("__send__")) {
            /*rb_warn("undefining `%s' may cause serious problem",
                     rb_id2name( id ) );*/
        }
        MethodNode methodNode = searchMethod(name);
        if (methodNode == null || methodNode.getBodyNode() == null) {
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
            throw new RubyNameException(getRuby(), "undefined method " + name + " for" + s0 + " '" + c.toName() + "'");
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

    /** search_method
     *
     */
    public MethodNode searchMethod(String name) {
        MethodNode body = (MethodNode) getMethods().get(name);
        if (body == null) {
            if (getSuperClass() != null) {
                return getSuperClass().searchMethod(name);
            } else {
                return null;
            }
        } else {
            body.setMethodOrigin(this);
            return body;
        }
    }

    /** rb_get_method_body
     *
     */
    public GetMethodBodyResult getMethodBody(String name, int noex) {
        GetMethodBodyResult result = new GetMethodBodyResult(this, name, noex);

        MethodNode methodNode = searchMethod(name);

        if (methodNode == null || methodNode.getBodyNode() == null) {
            System.out.println("Cant find method \"" + name + "\" in class " + toName());

            RubyMethodCacheEntry.saveEmptyEntry(getRuby(), this, name);

            return result;
        }

        RubyMethodCacheEntry ent = new RubyMethodCacheEntry(this, methodNode.getNoex());

        Node body = methodNode.getBodyNode();

        if (body instanceof FBodyNode) {
            FBodyNode fbody = (FBodyNode) body;

            ent.setMid(name);
            ent.setOrigin((RubyModule) fbody.getOrigin());
            ent.setMid0(fbody.getMId());
            ent.setMethod(fbody.getHeadNode());

            result.setRecvClass((RubyModule) fbody.getOrigin());
            result.setId(fbody.getMId());
            body = fbody.getHeadNode();
        } else {
            ent.setMid(name);
            ent.setMid0(name);
            ent.setOrigin(methodNode.getMethodOrigin());
            ent.setMethod(body);

            result.setRecvClass(methodNode.getMethodOrigin());
        }

        RubyMethodCacheEntry.saveEntry(getRuby(), this, name, ent);

        result.setNoex(ent.getNoex());
        result.setBody(body);
        return result;
    }

    /** rb_call
     *
     */
    public RubyObject call(RubyObject recv, String name, RubyPointer args, int scope) {
        RubyMethodCacheEntry ent = RubyMethodCacheEntry.getEntry(getRuby(), this, name);

        RubyModule klass = this;
        int noex;
        Node body;
        String id = name;

        if (ent != null) {
            if (ent.getMethod() == null) {
                throw new RuntimeException("undefined method " + name + " for " + recv + ":" + recv.getRubyClass().toName());
            }

            klass = ent.getOrigin();
            id = ent.getMid0();
            noex = ent.getNoex();
            body = ent.getMethod();
        } else {
            GetMethodBodyResult gmbr = getMethodBody(name, 0);
            klass = gmbr.getRecvClass();
            id = gmbr.getId();
            noex = gmbr.getNoex();
            body = gmbr.getBody();

            if (body == null) {
                if (scope == 3) {
                    throw new RubyNameException(getRuby(), "super: no superclass method '" + name + "'");
                }
                throw new RuntimeException("undefined method " + name + " for " + recv + ":" + recv.getRubyClass().toName());
            }
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

        return klass.call0(recv, id, args, body, false);
    }

    /** rb_call0
     *
     */
    public RubyObject call0(RubyObject recv, String name, RubyPointer args, Node body, boolean noSuper) {

        // ...

        if (getRuby().getIter().getIter() == RubyIter.ITER_PRE) {
            getRuby().getIter().push(RubyIter.ITER_CUR);
        } else {
            getRuby().getIter().push(RubyIter.ITER_NOT);
        }

        RubyFrame frame = getRuby().getRubyFrame();
        frame.push();
        frame.setLastFunc(name);
        frame.setLastClass(noSuper ? null : this);
        frame.setSelf(recv);
        frame.setArgs(args);

        RubyObject result = ((CallableNode) body).call(getRuby(), recv, name, args, noSuper);

        getRuby().getRubyFrame().pop();
        getRuby().getIter().pop();

        return result;
    }

    /** rb_alias
     *
     */
    public void aliasMethod(String newId, String oldId) {
        testFrozen();

        if (oldId.equals(newId)) {
            return;
        }

        if (this == getRuby().getClasses().getObjectClass()) {
            getRuby().secure(4);
        }

        MethodNode methodNode = searchMethod(oldId);

        RubyModule origin = null;

        if (methodNode == null || methodNode.getBodyNode() == null) {
            if (isModule()) {
                methodNode = getRuby().getClasses().getObjectClass().searchMethod(oldId);
                origin = methodNode.getMethodOrigin();
            }
        }
        if (methodNode == null || methodNode.getBodyNode() == null) {
            // print_undef( klass, def );
            return; //CEF
        }
        origin = methodNode.getMethodOrigin();

        Node body = methodNode.getBodyNode();
        // methodNode.setCnt(methodNode.nd_cnt() + 1);
        if (body instanceof FBodyNode) { /* was alias */
            oldId = body.getMId();
            origin = (RubyModule) body.getOrigin();
            body = body.getBodyNode();
        }

        NodeFactory nf = new NodeFactory(getRuby());

        getMethods().put(newId, nf.newMethod(nf.newFBody(body, oldId, origin), methodNode.getNoex()));
    }

    /** remove_method
     * 
     */
    public void removeMethod(String methodId) {
        if (this == getRuby().getClasses().getObjectClass()) {
            getRuby().secure(4);
        }
        if (getRuby().getSecurityLevel() >= 4 && !isTaint()) {
            throw new RubySecurityException(getRuby(), "Insecure: can't remove method");
        }
        if (isFrozen()) {
            // rb_error_frozen("class/module");
        }
        if (getMethods().remove(methodId) == null) {
            throw new RubyNameException(getRuby(), "method '" + methodId + "' not defined in " + toName());
        }
    }

    /** rb_define_class_under
     *
     */
    public RubyClass defineClassUnder(String name, RubyClass superClass) {
        RubyClass newClass = getRuby().defineClass(name, superClass);

        setConstant(name, newClass);
        newClass.setClassPath(this, name);

        return newClass;
    }

    /** rb_define_module_under
     *
     */
    public RubyModule defineModuleUnder(String name) {
        RubyModule newModule = getRuby().defineModule(name);

        setConstant(name, newModule);
        newModule.setClassPath(this, name);

        return newModule;
    }

    /** rb_class2name
     *
     */
    public String toName() {
        if (this == getRuby().getClasses().getNilClass()) {
            return "nil";
        }
        if (this == getRuby().getClasses().getTrueClass()) {
            return "true";
        }
        if (this == getRuby().getClasses().getFalseClass()) {
            return "false";
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
            throw new RubyNameException(getRuby(), "bad constant name " + name);
        }

        setConstant(name, value);
    }

    /** rb_mod_remove_cvar
     *
     */
    public RubyObject removeCvar(RubyObject name) { // Wrong Parameter ?
        if (!IdUtil.isClassVariable(name.toId())) {
            throw new RubyNameException(getRuby(), "wrong class variable name " + name.toId());
        }

        if (!isTaint() && getRuby().getSecurityLevel() >= 4) {
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
            throw new RubyNameException(getRuby(), "cannot remove " + name.toId() + " for " + toName());
        }

        throw new RubyNameException(getRuby(), "class variable " + name.toId() + " not defined for " + toName());
    }

    /** rb_define_class_variable
     *
     */
    public void defineClassVariable(String name, RubyObject value) {
        if (!IdUtil.isClassVariable(name)) {
            throw new RubyNameException(getRuby(), "wrong class variable name " + name);
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
            addMethod(name, new NodeFactory(getRuby()).newIVar(attrIV), noex);
            // id.clearCache();
            funcall("method_added", RubySymbol.newSymbol(getRuby(), name));
        }

        if (write) {
            name = name + "=";
            addMethod(name, new NodeFactory(getRuby()).newAttrSet(attrIV), noex);
            // id.clearCache();
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
                RubyString name = RubyString.newString(getRuby(), (String)key);
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
        if (getRuby().getSecurityLevel() >= 4 && !isTaint()) {
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

        MethodNode body = searchMethod(name);
        RubyModule origin = body.getMethodOrigin();

        if (body == null && isModule()) {
            body = getRuby().getClasses().getObjectClass().searchMethod(name);
            origin = body.getMethodOrigin();
        }

        if (body == null) {
        }

        if (body.getNoex() != noex) {
            if (this == origin) {
                body.setNoex(noex);
            } else {
                addMethod(name, new NodeFactory(getRuby()).newZSuper(), noex);
            }
        }
    }

    /** rb_method_boundp
     * 
     */
    private boolean isMethodBound(String name, int ex) {
        RubyMethodCacheEntry entry = RubyMethodCacheEntry.getEntry(getRuby(), this, name);

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

    public RubyObject newMethod(RubyObject recv, String name, RubyClass methodClass) {
        RubyClass originalClass = (RubyClass) this;
        String originalName = name;

        GetMethodBodyResult gmbr = getMethodBody(name, 0);
        if (gmbr == null) {
            // printUndef();
            return getRuby().getNil();
        }

        while (gmbr.getBody() instanceof ZSuperNode) {
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
        newMethod.setBodyNode(gmbr.getBody());
        newMethod.setOriginalClass(originalClass);
        newMethod.setOriginalId(originalName);

        return newMethod;
    }

    public RubyObject executeUnder(Callback method, RubyObject[] args) {
        getRuby().pushClass();
        getRuby().setRubyClass(this);
        getRuby().getRubyFrame().push();

        RubyFrame frame = getRuby().getRubyFrame().getPrev();
        getRuby().getRubyFrame().setLastFunc(frame.getLastFunc());
        getRuby().getRubyFrame().setLastClass(frame.getLastClass());
        getRuby().getRubyFrame().setArgs(frame.getArgs());
        if (getRuby().getCBase() != this) {
            getRuby().getRubyFrame().setCbase(new CRefNode(this, getRuby().getRubyFrame().getCbase()));
        }
        getRuby().getCRef().push(this);

        // mode = scope_vmode;
        // SCOPE_SET(SCOPE_PUBLIC);

        RubyObject result = null;

        try {
            result = method.execute(this, args, getRuby());
        } finally {
            getRuby().getCRef().pop();
            getRuby().getRubyFrame().pop();
            getRuby().popClass();
        }

        return result;
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

        if (isSingleton()) {
            rbModule = ((RubyObject) rbModule.getInstanceVar("__atached__")).getClassVarSingleton();
        }

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
        RubyModule clone = new RubyModule(getRuby(), getRubyClass(), getSuperClass());
        clone.setupClone(this);

        if (getInstanceVariables() != null) {
            clone.setInstanceVariables(getInstanceVariables().cloneRubyMap());
        }

        // clone the methods.
        if (getMethods() != null) {
            clone.setMethods(new RubyHashMap());
            getMethods().foreach(new RubyMapMethod() {
                NodeFactory nf = new NodeFactory(getRuby());

                public int execute(Object key, Object value, Object arg) {
                    MethodNode methodNode = (MethodNode) value;

                    ((RubyMap) arg).put(key, nf.newMethod(methodNode.getBodyNode(), methodNode.getNoex()));
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
                ary.push(p.getRubyClass());
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
            throw new RubyTypeException(getRuby(), "compared with non class/module");
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
            throw new RubyTypeException(getRuby(), "compared with non class/module");
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
            throw new RubyTypeException(getRuby(), "<=> requires Class or Module (" + getRubyClass().toName() + " given)");
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
    public RubyObject const_get(RubySymbol symbol) {
        String name = symbol.toId();

        if (!IdUtil.isConstant(name)) {
            throw new RubyNameException(getRuby(), "wrong constant name " + name);
        }

        return getConstant(name);
    }

    /** rb_mod_const_set
     *
     */
    public RubyObject const_set(RubySymbol symbol, RubyObject value) {
        String name = symbol.toId();

        if (!IdUtil.isConstant(name)) {
            throw new RubyNameException(getRuby(), "wrong constant name " + name);
        }

        setConstant(name, value);

        return value;
    }

    /** rb_mod_const_defined
     *
     */
    public RubyBoolean const_defined(RubySymbol symbol) {
        String name = symbol.toId();

        if (!IdUtil.isConstant(name)) {
            throw new RubyNameException(getRuby(), "wrong constant name " + name);
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
                MethodNode body = (MethodNode) value;
                RubyArray ary = (RubyArray) arg;

                if ((body.getNoex() & (Constants.NOEX_PRIVATE | Constants.NOEX_PROTECTED)) == 0) {
                    RubyString name = RubyString.newString(getRuby(), id);

                    if (ary.includes(name).isFalse()) {
                        if (body.getBodyNode() == null) {
                            ary.push(getRuby().getNil());
                        }
                        ary.push(name);
                    }
                } else if (body.getBodyNode() != null && body.getBodyNode() instanceof ZSuperNode) {
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
                MethodNode body = (MethodNode) value;
                RubyArray ary = (RubyArray) arg;

                if (body.getBodyNode() == null) {
                    ary.push(getRuby().getNil());
                    ary.push(RubyString.newString(getRuby(), id));
                } else if ((body.getNoex() & Constants.NOEX_PROTECTED) != 0) {
                    RubyString name = RubyString.newString(getRuby(), id);

                    if (ary.includes(name).isFalse()) {
                        ary.push(name);
                    }
                } else if (body.getBodyNode() instanceof ZSuperNode) {
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
                MethodNode body = (MethodNode) value;
                RubyArray ary = (RubyArray) arg;

                if (body.getBodyNode() == null) {
                    ary.push(getRuby().getNil());
                    ary.push(RubyString.newString(getRuby(), id));
                } else if ((body.getNoex() & Constants.NOEX_PRIVATE) != 0) {
                    RubyString name = RubyString.newString(getRuby(), id);

                    if (ary.includes(name).isFalse()) {
                        ary.push(name);
                    }
                } else if (body.getBodyNode() instanceof ZSuperNode) {
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
            throw new RubyNameException(getRuby(), "wrong class variable name " + id);
        }
        if (!isTaint() && getRuby().getSecurityLevel() >= 4) {
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
            throw new RubyNameException(getRuby(), "cannot remove " + id + " for " + toName());
        }
        throw new RubyNameException(getRuby(), "class variable " + id + " not defined for " + toName());
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
        if (getRuby().getSecurityLevel() >= 4 && !isTaint()) {
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
        if (getRuby().getSecurityLevel() >= 4 && !isTaint()) {
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
        if (getRuby().getSecurityLevel() >= 4 && !isTaint()) {
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
        if (getRuby().getSecurityLevel() >= 4 && !isTaint()) {
            throw new RubySecurityException(getRuby(), "Insecure: can't change method visibility");
        }

        if (args.length == 0) {
            getRuby().setActMethodScope(Constants.SCOPE_MODFUNC);
        } else {
            setMethodVisibility(args, Constants.NOEX_PRIVATE);

            for (int i = 0; i < args.length; i++) {
                String id = args[i].toId();
                MethodNode body = searchMethod(id);
                if (body == null || body.getBodyNode() == null) {
                    throw new RubyBugException("undefined method '" + id + "'; can't happen");
                }
                getSingletonClass().addMethod(id, body.getBodyNode(), Constants.NOEX_PUBLIC);
                // rb_clear_cache_by_id(id);
                funcall("singleton_added", RubySymbol.newSymbol(getRuby(), id));
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

    private static class GetMethodBodyResult {
        private Node body;
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

        /** Getter for property node.
         * @return Value of property node.
         */
        public Node getBody() {
            return body;
        }

        /** Setter for property node.
         * @param node New value of property node.
         */
        public void setBody(Node body) {
            this.body = body;
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
    }

}