/*
 * RubyModule.java - No description
 * Created on 09. Juli 2001, 21:38
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 */

package org.jruby;

import java.util.*;

import org.jruby.core.*;
import org.jruby.exceptions.*;
import org.jruby.interpreter.*;
import org.jruby.original.*;
import org.jruby.util.*;

/**
 *
 * @author  jpetersen
 */
public class RubyModule extends RubyObject implements Scope, node_type {
    private RubyModule superClass;
    private RubyMap methods = new RubyHashMap();
    
    // Flags
    private boolean singleton = false;
    
    /** Holds value of property included. */
    private boolean included;
    
    public RubyModule(Ruby ruby) {
        this(ruby, null);
    }
    
    public RubyModule(Ruby ruby, RubyModule rubyClass) {
        this(ruby, rubyClass, null);
    }
    
    public RubyModule(Ruby ruby, RubyModule rubyClass, RubyModule superClass) {
        super(ruby, rubyClass);
        
        this.superClass = superClass;
    }
    
    /** Getter for property superClass.
     * @return Value of property superClass.
     */
    public RubyModule getSuperClass() {
        return this.superClass;
    }
    
    /** Setter for property superClass.
     * @param superClass New value of property superClass.
     */
    public void setSuperClass(RubyModule superClass) {
        this.superClass = superClass;
    }
    
    public boolean isSingleton() {
        return this.singleton;
    }
    
    public void setSingleton(boolean singleton) {
        this.singleton = singleton;
    }
    
    
    public RubyMap getMethods() {
        return this.methods;
    }
    
    public void setMethods(RubyMap methods) {
        this.methods = methods;
    }
    
    public boolean isModule() {
        return !isIncluded();
    }
    
    public boolean isClass() {
        return false;
    }
    
    /** classname
     *
     */
    public RubyString getClassname() {
        RubyString path = null;
        
        RubyModule rbModule = this;
        while (rbModule.isIncluded() || rbModule.isSingleton()) {
            rbModule = ((RubyClass)rbModule).getSuperClass();
        }
        
        if (rbModule == null) {
            rbModule = getRuby().getObjectClass();
        }
        
        path = (RubyString)getInstanceVariables().get("__classpath__");
        if (path != null) {
            if (getInstanceVariables().get("__classid__") != null) {
                path = RubyString.m_newString(getRuby(), ((RubyId)getInstanceVariables().get("__classid__")).toName()); // todo: convert from symbol to string
                
                getInstanceVariables().put("__classpath__", path);
                getInstanceVariables().remove("__classid__");
            }
        }
        
        if (path == null) {
            path = rbModule.findClassPath();
            
            /* if (path.isNil()) {
                return null;
            }*/
            
            return path;
        }
        
        /*if (!(path instanceof RubyString)) {
            throw new RubyBugException("class path is not set properly");
        }*/
        
        return path;
    }
    
    /**
     *
     */
    public RubyString findClassPath() {
        return null;
    }
    
    /** include_class_new
     *
     */
    public RubyClass newIncludeClass(RubyModule superClass) {
        RubyClass newClass = new RubyClass(getRuby(), getRuby().getClassClass(), superClass);
        newClass.setIncluded(true);
        
        newClass.setInstanceVariables(getInstanceVariables());
        newClass.setMethods(getMethods());
        
        if (isIncluded()) {
            newClass.setRubyClass(getRubyClass());
        } else {
            newClass.setRubyClass(this);
        }
        
        return newClass;
    }
    
    public void setName(RubyId id) {
        getInstanceVariables().put("__classid__", id);
    }
    
    /** rb_set_class_path
     *
     */
    public void setClassPath(RubyModule under, String name) {
        RubyString value = null;
        
        if (under == getRuby().getObjectClass()) {
            value = RubyString.m_newString(getRuby(), name);
        } else {
            value = (RubyString)under.getClassPath().m_dup();
            value.m_cat("::");
            value.m_cat(name);
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
        
        return RubyString.m_newString(getRuby(), "<" + s + " 01x" + Integer.toHexString(hashCode()) + ">"); // 0 = pointer
    }
    
    public void setCvar(RubyId id, RubyObject value) {
    }
    
    public void declareCvar(RubyId id, RubyObject value) {
    }
    
    public RubyObject getCvar(RubyId id) {
        return null;
    }
    
    public boolean isCvarDefined(RubyId id) {
        return false;
    }
    
    /**
     *
     */
    public void setConstant(RubyId id, RubyObject value) {
        setAv(id, value, true);
    }

    /** rb_const_get
     *
     */
    public RubyObject getConstant(RubyId id) {
        boolean mod_retry = false;
        RubyModule tmp = this;
        
        while (true) {
            while (tmp != null) {
                if (tmp.getInstanceVariables().get(id) != null) {
                    return (RubyObject)tmp.getInstanceVariables().get(id);
                }
                if (tmp == getRuby().getObjectClass() && getRuby().getTopConstant(id) != null) {
                    return getRuby().getTopConstant(id);
                }
                tmp = tmp.getSuperClass();
            }
            if (!mod_retry && isModule()) {
                mod_retry = true;
                tmp = getRuby().getObjectClass();
                continue;
            }
            break;
        }

        /* Uninitialized constant */
        if (this != getRuby().getObjectClass()) {
            throw new RubyNameException("uninitialized constant " + id.toName() + " at " + getClassPath().getString());
        } else {
            throw new RubyNameException("uninitialized constant " + id.toName());
        }
        
        // return getRuby().getNil();
    }
            
    /** rb_include_module
     *
     */
    public void includeModule(RubyModule rubyModule) {
        if (rubyModule == null || rubyModule == this) {
            return;
        }
        
        RubyModule actClass = this;
        while (rubyModule != null) {
            for (RubyModule rbClass = actClass.getSuperClass(); rbClass != null; rbClass = rbClass.getSuperClass()) {
                if (rbClass.isIncluded() && rbClass.getMethods() == rubyModule.getMethods()) {
                    if (rubyModule.getSuperClass() != null) {
                        rbClass.includeModule(rubyModule.getSuperClass());
                    }
                    return;
                }
            }
            actClass.setSuperClass(rubyModule.newIncludeClass(actClass.getSuperClass()));
            actClass = actClass.getSuperClass();
            rubyModule = rubyModule.getSuperClass();
	}
    }
    
    /** mod_av_set
     *
     */
    protected void setAv(RubyId id, RubyObject value, boolean constant) {
        String dest = constant ? "constant" : "class variable";
        
        if (!isTaint() && getRuby().getSecurityLevel() >= 4) {
            throw new RubySecurityException("Insecure: can't set " + dest);
        }
        if (isFrozen()) {
            throw new RubyFrozenException("class/module");
        }
        if (constant && (getInstanceVariables().get(id) != null)) {
            //getRuby().warn("already initialized " + dest + " " + name);
        }
        
        if (getInstanceVariables() == null) {
            setInstanceVariables(new RubyHashMap());
        }
        
        getInstanceVariables().put(id, value);
    }

    /** rb_add_method
     *
     */
    public void addMethod(RubyId id, NODE node, int noex) {
        if (this == getRuby().getObjectClass()) {
            getRuby().secure(4);
        }
    
        if (getRuby().getSecurityLevel() >= 4 && !isTaint()) {
            throw new RubySecurityException("Insecure: can't define method");
        }
        if (isFrozen()) {
            throw new RubyFrozenException("class/module");
        }
        NODE body = NODE.newMethod(node, noex);
        getMethods().put(id, body);
    }
    
    public void defineMethod(String name, RubyCallbackMethod method) {
        RubyId id = getRuby().intern(name);

        int noex = (name.charAt(0) == 'i' && id == getRuby().intern("initialize")) ? NOEX_PRIVATE : NOEX_PUBLIC;
        
        addMethod(id, NODE.newCallbackMethod(method), noex | NOEX_CFUNC);
    }
    
    public void defineMethodId(RubyId id, RubyCallbackMethod method) {
        addMethod(id, NODE.newCallbackMethod(method), NOEX_PUBLIC | NOEX_CFUNC);
    }
    
    public void defineProtectedMethod(String name, RubyCallbackMethod method) {
        addMethod(getRuby().intern(name), NODE.newCallbackMethod(method), NOEX_PROTECTED | NOEX_CFUNC);
    }
    
    public void definePrivateMethod(String name, RubyCallbackMethod method) {
        addMethod(getRuby().intern(name), NODE.newCallbackMethod(method), NOEX_PRIVATE | NOEX_CFUNC);
    }
    
    /*public void undefMethod(RubyId id) {
        if (isFrozen()) {
            throw new RubyFrozenException();
        }
        
        getMethods().remove(id);
    }*/
    
    public void undefMethod(String name) {
        addMethod(getRuby().intern(name), null, NOEX_UNDEF);
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
            throw new RubyFrozenException(desc);
        }
    }
    
    /** rb_undef
     *
     */
    public void undef(RubyId id) {
        if (this == getRuby().getObjectClass()) {
            getRuby().secure(4);
        }
        if (getRuby().getSecurityLevel() >= 4 && !isTaint())  {
            throw new SecurityException("Insecure: can't undef");
        }
        testFrozen();
        if (id == getRuby().intern("__id__") || id == getRuby().intern("__send__")) {
            /*rb_warn("undefining `%s' may cause serious problem",
                     rb_id2name( id ) );*/
        }
        NODE body = searchMethod(id);
        if (body == null || body.nd_body() == null) {
            String s0 = " class";
            RubyModule c = this;

            if (c.isSingleton()) {
                RubyObject obj = getIv("__attached__");

                if (obj instanceof RubyModule) {
                    c = (RubyModule)obj;
                    s0 = "";
                }
            } else if (c.isModule()) {
                s0 = " module";
            }
            throw new RubyNameException("undefined method " + id.toName() +
                                        " for" + s0 + " '" + c.toName() + "'");
        }
        addMethod(id, null, NOEX_PUBLIC);
    }
    
    /** rb_define_module_function
     *
     */
    public void defineModuleFunction(String name, RubyCallbackMethod method) {
        definePrivateMethod(name, method);
        defineSingletonMethod(name, method);
    }
    
    /** rb_define_alias
     *
     */
    public void defineAlias(String oldName, String newName) {
        
    }
    
    /** rb_define_attr
     *
     */
    public void defineAttribute(String name, boolean read, boolean write) {
        
    }
    
    /** rb_const_defined
     *
     */
    public boolean isConstantDefined(RubyId id) {
        for (RubyModule tmp = this; tmp != null; tmp = tmp.getSuperClass()) {
            if (tmp.getInstanceVariables() != null && tmp.getInstanceVariables().get(id) != null) {
                return true;
            }
        }
        
        if (isModule()) {
            return getRuby().getObjectClass().isConstantDefined(id);
        }

        if (getRuby().isClassDefined(id)) {
            return true;
        }
        
        return getRuby().isAutoloadDefined(id);
    }
    
    /** search_method
     *
     */
    public NODE searchMethod(RubyId id) {
        RubyModule rubyModule = getMethodOrigin(id);
        
        if (rubyModule != null) {
            return (NODE)rubyModule.getMethods().get(id);
        }
        return null;
    }
    
    /** search_method
     *
     */
    public RubyModule getMethodOrigin(RubyId id) {
        NODE body = (NODE)getMethods().get(id);
        if (body == null) {
            if (getSuperClass() != null) {
                return getSuperClass().getMethodOrigin(id);
            } else {
                return null;
            }
        }
        return this;
    }
    
    /** rb_get_method_body
     *
     */
    public NODE getMethodBody(RubyId id, int scope) {
        NODE body = searchMethod(id);
        
        // ??
        if (body == null) {
            System.out.println("Cant find method: " + id.toName());
            
            return null;
        }
        
        // ??
        
        // ... cache
        
        body = body.nd_body();
        
        if (body.nd_type() == NODE_FBODY) {
            body = body.nd_head();
        }
        
        return body;
    }
    
    /** rb_call
     *
     */
    public RubyObject call(RubyObject recv, RubyId id, RubyObject[] args, int scope) {
        
        // ...
        
        NODE body = getMethodBody(id, scope);
        
        return call0(recv, id, args, body, false);
    }
    
    /** rb_call0
     *
     */
    public RubyObject call0(RubyObject recv, RubyId id, RubyObject[] args, NODE body, boolean noSuper) {
        
        // ...
        RubyInterpreter interpreter = getRuby().getInterpreter();
        
        interpreter.getRubyFrame().push();
        
        // HACK +++
        interpreter.getRubyIter().push(Iter.ITER_NOT);
        // HACK ---
        
        interpreter.getRubyFrame().setLastFunc(id);
        interpreter.getRubyFrame().setLastClass(noSuper ? null : this);
        interpreter.getRubyFrame().setSelf(recv);
        interpreter.getRubyFrame().setArgs(new ShiftableList(args));
        
        RubyObject result = null;
        
        switch (body.nd_type()) {
            case NODE_CFUNC: {
                result = ((RubyCallbackMethod)body.nd_cfnc()).execute(recv, args, getRuby());
                break;
            }
            case NODE_ZSUPER:
            case NODE_ATTRSET:
            case NODE_IVAR: {
                result = interpreter.eval(recv, body);
                break;
            }
            case NODE_SCOPE: {
                NODE savedCref = null;
                // VALUE[] localVars = null;
                
                ShiftableList argsList = new ShiftableList(args);
                ShiftableList localVarsList = null;
                
                getRuby().rubyScope.push();
                
                if (body.nd_rval() != null) {
                    savedCref = interpreter.ruby_cref;
                    interpreter.ruby_cref = (NODE)body.nd_rval();
                    interpreter.getRubyFrame().setCbase(body.nd_rval());
                }
                if (body.nd_tbl() != null) {
                    // ? +++
                    List tmpList = Collections.nCopies(body.nd_tbl()[0].intValue() + 1, getRuby().getNil());
                    // ? ---
                    localVarsList = new ShiftableList(new ArrayList(tmpList));
                    localVarsList.set(0, body);
                    localVarsList.shift(1);
                    
                    getRuby().rubyScope.setLocalTbl(body.nd_tbl());
                    getRuby().rubyScope.setLocalVars(localVarsList.getList());
                } else {
                    localVarsList = getRuby().rubyScope.getLocalVars();
                    
                    getRuby().rubyScope.setLocalVars(null);
                    getRuby().rubyScope.setLocalTbl(null);
                }
            
                body = body.nd_next();
                
                RubyVarmap.push(getRuby());
                // PUSH_TAG(PROT_FUNC);
                
                try {
                    NODE node = null;
                    int i;
                    
                    if (body.nd_type() == NODE_ARGS) {
                        node = body;
                        body = null;
                    } else if (body.nd_type() == NODE_BLOCK) {
                        node = body.nd_head();
                        body = body.nd_next();
                    }
                
                    if (node != null) {
                        if (node.nd_type() != NODE_ARGS) {
                            // rb_bug("no argument-node");
                        }

                        i = node.nd_cnt();
                        if (i > (args != null ? args.length : 0)) {
                            throw new RubyArgumentException("wrong # of arguments(" + args.length + " for " + i + ")");
                        }
                        if (node.nd_rest() == -1) {
                            int opt = i;
                            NODE optnode = node.nd_opt();

                            while (optnode != null) {
                                opt++;
                                optnode = optnode.nd_next();
                            }
                            if (opt < (args != null ? args.length : 0)) {
                                throw new RubyArgumentException("wrong # of arguments(" + args.length + " for " + opt + ")");
                            }
                            
                            interpreter.getRubyFrame().setArgs(localVarsList != null ? localVarsList.getList(2) : null);
                        }

                        if (localVarsList != null) {
                            if (i > 0) {
                                localVarsList.shift(2);
                                for (int j = 0; j < i; j++ ) {
                                    localVarsList.set(j, argsList.get(j));
                                }
                                localVarsList.shiftLeft(2);
                            }
                            
                            argsList.shift(i);
                            
                            if (node.nd_opt() != null) {
                                NODE opt = node.nd_opt();

                                while (opt != null && argsList.size() != 0) {
                                    interpreter.assign(recv, opt.nd_head(), (RubyObject)argsList.get(0), true);
                                    argsList.shift(1);
                                    opt = opt.nd_next();
                                }
                                interpreter.eval(recv, opt);
                            }
                            if (node.nd_rest() >= 0) {
                                RubyArray array = null;
                                if (argsList.size() > 0) {
                                    array = RubyArray.m_newArray(getRuby(), argsList);
                                } else {
                                    array = RubyArray.m_newArray(getRuby(), 0);
                                }
                                localVarsList.set(node.nd_rest(), array);
                            }
                        }
                    }

                    result = interpreter.eval(recv, body);
                } catch (ReturnException rExcptn) {
                }
                
                RubyVarmap.pop(getRuby());
                
                getRuby().rubyScope.pop();
                interpreter.ruby_cref = savedCref;
                
                break;
            }
            default: {
                System.out.println("Not implemented yet (method call): " + id.toName() + ", node_type:" + body.nd_type());
            }
        }
        
        interpreter.getRubyFrame().pop();
        interpreter.getRubyIter().pop();
        
        return result ;
    }
    
    
    /** rb_singleton_class_new
     *
     */
    public RubyClass newSingletonClass() {
        RubyClass newClass = RubyClass.m_newClass(getRuby(), (RubyClass)this);
        newClass.setSingleton(true);
        
        return newClass;
    }
    
    /** rb_alias
     *
     */
    public void aliasMethod(RubyId newId, RubyId oldId) {
        testFrozen();
        
        if (oldId == newId) {
            return;
        }
        
        if (this == getRuby().getObjectClass()) {
            getRuby().secure(4);
        }
        
        NODE orig = searchMethod(oldId);
        RubyModule origin = getMethodOrigin(oldId);
        
        if (orig == null || orig.nd_body() == null) {
            if (isModule()) {
                orig = getRuby().getObjectClass().searchMethod(oldId);
                origin = getRuby().getObjectClass().getMethodOrigin(oldId);
            }
        }
        if (orig == null || orig.nd_body() == null) {
            // print_undef( klass, def );
        }
        
        NODE body = orig.nd_body();
        orig.nd_cnt(orig.nd_cnt() + 1);
        if (body.nd_type() == NODE_FBODY) { /* was alias */
            oldId = (RubyId)body.nd_mid();
            origin = (RubyModule)body.nd_orig();
            body = body.nd_head();
        }

        getMethods().put(newId, NODE.newMethod(NODE.newFBody(body, oldId, origin), orig.nd_noex()));
    }
    
    /** rb_singleton_class_clone
     *
     */
    public RubyModule getSingletonClassClone() {
        if (!isSingleton()) {
            return this;
        }
        
        RubyModule clone = new RubyClass(getRuby(), null, getSuperClass());
        clone.setupClone(this);
        clone.setInstanceVariables(getInstanceVariables().cloneRubyMap());
        
        //clone.setMethods();
        
        // st_foreach(RCLASS(klass)->m_tbl, clone_method, clone->m_tbl);
        
        clone.setSingleton(true);
        
	return clone;
    }
    
    /** rb_singleton_class_attached
     *
     */
    public void attachSingletonClass(RubyObject rbObject) {
        if (isSingleton()) {
            if (getInstanceVariables() == null) {
                setInstanceVariables(new RubyHashMap());
            }
            
            getInstanceVariables().put(getRuby().intern("__atached__"), rbObject);
        }
    }
    
    /** rb_define_class_under
     *
     */
    public RubyClass defineClassUnder(String name, RubyClass superClass) {
        RubyClass newClass = getRuby().defineClassId(getRuby().intern(name), superClass);
        
        setConstant(getRuby().intern(name), newClass);
        newClass.setClassPath(this, name);
        
        return newClass;
    }
    
    /** rb_define_module_under 
     *
     */
    public RubyModule defineModuleUnder(String name) {
        RubyModule newModule = getRuby().defineModuleId(getRuby().intern(name));
        
        setConstant(getRuby().intern(name), newModule);
        newModule.setClassPath(this, name);
        
        return newModule;
    }
    
    /** rb_class2name
     *
     */
    public String toName() {
        if (this == getRuby().getNilClass()) {
            return "nil";
        }
        if (this == getRuby().getTrueClass()) {
            return "true";
        }
        if (this == getRuby().getFalseClass()) {
            return "false";
        }
        
        return ((RubyString)getClassPath()).toString();
    }
    
    
    /** rb_define_const
     *
     */
    public void defineConstant(String name, RubyObject value) {
        RubyId id = getRuby().intern(name);
        
        if (this == getRuby().getClassClass()) {
            getRuby().secure(4);
        }
        
        if (!id.isConstId()) {
            throw new RubyNameException("wrong constnt name " + name);
        }
        
        setConstant(id, value);
    }
    
    /** rb_mod_remove_cvar
     *
     */
    public RubyObject removeCvar(RubyObject name) { // Wrong Parameter ?
        RubyId id = getRuby().toId(name);
        
        if (!id.isClassId()) {
            throw new RubyNameException("wrong class variable name " + name);
        }
        
        if (!isTaint() && getRuby().getSecurityLevel() >= 4) {
            throw new RubySecurityException("Insecure: can't remove class variable");    
        }
        
        if (isFrozen()) {
            throw new RubyFrozenException("class/module");
        }
        
        RubyObject value = (RubyObject)getInstanceVariables().remove(id);
         
        if (value != null) {
            return value;
        }

        if (isCvarDefined(id)) {
            throw new RubyNameException("cannot remove " + id.toName() + " for " + toName());
        }
        
        throw new RubyNameException("class variable " + id.toName() + " not defined for " + toName());
    }

    /** rb_define_class_variable
     *
     */
    public void defineClassVariable(String name, RubyObject value) {
        RubyId id = getRuby().intern(name);

        if (!id.isClassId()) {
            throw new RubyNameException("wrong class variable name " + name);
        }
        
        declareCvar(id, value);
    }
    
    /** rb_attr
     *
     */
    public void addAttribute(RubyId id, boolean read, boolean write, int noex) {
        String name = id.toName();
        
        RubyId attr_iv = getRuby().intern("@" + name);
        
        if (read) {
/*            RubyMethod rubyMethod = new AttrReadMethod(attr_iv);
            rubyMethod.setVisibility(visibility);
            addMethod(id, rubyMethod);
            id.clearCache();
            invokeMethod("method_added", new RubyBasic[] { id.toSym() }, null);
*/        }
        
        if (write) {
/*            RubyMethod rubyMethod = new AttrWriteMethod(attr_iv);
            rubyMethod.setVisibility(visibility);
            addMethod(id, rubyMethod);
            id.clearCache();
            invokeMethod("method_added", new RubyBasic[] { id.toSym() }, null);
*/        }
    }
    
    // Methods of the Module Class (rb_mod_*):
    
    /** rb_mod_new
     *
     */
    public static RubyModule m_newModule(Ruby ruby) {
        RubyModule newModule = new RubyModule(ruby, ruby.getModuleClass());
        
        return newModule;
    }
    
    /** rb_mod_name
     *
     */
    public RubyString m_name() {
        RubyString path = getClassname();
        if (path != null) {
            return (RubyString)path.m_dup();
        }
        return RubyString.m_newString(getRuby(), "");
    }
    
    /** rb_mod_class_variables
     *
     */
    public RubyArray m_class_variables() {
        RubyArray ary = RubyArray.m_newArray(getRuby());
        
        RubyModule rbModule = this;
        
        if (isSingleton()) {
            rbModule = ((RubyObject)rbModule.getIv("__atached__")).getCvarSingleton();
        }
        
        while (rbModule != null) {
            if (rbModule.getInstanceVariables() != null) {
                Iterator iter = rbModule.getInstanceVariables().keySet().iterator();
                while (iter.hasNext()) {
                    RubyId id = (RubyId)iter.next();
                    if (id.isClassId()) {
                        RubyString kval = RubyString.m_newString(getRuby(), id.toName());
                        if (ary.m_includes(kval).isFalse()) {
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
    public RubyObject m_clone() {
        RubyModule clone = new RubyModule(getRuby(), getRubyClass(), getSuperClass());
        clone.setupClone(this);
        
        if (getInstanceVariables() != null) {
            clone.setInstanceVariables(getInstanceVariables().cloneRubyMap());
        }
        
        // clone methods.
        
        return clone;
    }
    
    /** rb_mod_dup
     *
     */
    public RubyObject m_dup() {
        RubyModule dup = (RubyModule)m_clone();
        dup.setupObject(getRubyClass());
        
        dup.setSingleton(isSingleton());
        
        return dup;
    }
    
    /** rb_mod_included_modules
     *
     */
    public RubyArray m_included_modules() {
        RubyArray ary = RubyArray.m_newArray(getRuby());
        
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
    public RubyArray m_ancestors() {
        RubyArray ary = RubyArray.m_newArray(getRuby());
        
        for (RubyModule p = this; p != null; p = p.getSuperClass()) {
            if (p.isSingleton()) {
                continue;
            }
            
            if (p.isIncluded()) {
                ary.push(p.getRubyClass());
            } else {
                ary.push(p);
            }
        }
        
        return ary;
    }
    
    /** rb_mod_to_s
     *
     */
    public RubyString m_to_s() {
        return (RubyString)getClassPath().m_dup();
    }
    
    /** rb_mod_eqq
     *
     */
    public RubyBoolean op_eqq(RubyObject obj) {
        //return obj.isKindOf(this);
        return getRuby().getFalse();
    }
    
    /** rb_mod_le
     *
     */
    public RubyBoolean op_le(RubyObject obj) {
        if (!(obj instanceof RubyModule)) {
            throw new RubyTypeException("compared with non class/module");
        }
        
        RubyModule mod = this;
        while (mod != null) {
            if (mod.getMethods() == ((RubyModule)obj).getMethods()) {
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
            throw new RubyTypeException("compared with non class/module");
        }
        
        return ((RubyModule)obj).op_le(this);
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
            return RubyFixnum.m_newFixnum(getRuby(), 0);
        }
        
        if (!(obj instanceof RubyModule)) {
            throw new RubyTypeException("<=> requires Class or Module (" + getRubyClass().toName() + " given)");
        }
        
        if (op_le(obj).isTrue()) {
            return RubyFixnum.m_newFixnum(getRuby(), -1);
        }
        
        return RubyFixnum.m_newFixnum(getRuby(), 1);
    }
    
    /** rb_mod_initialize
     *
     */
    public RubyObject m_initialize() {
        return getRuby().getNil();
    }
    
    /** rb_module_s_new
     *
     */
    public static RubyModule m_newModule(Ruby ruby, RubyClass rubyClass) {
        RubyModule mod = RubyModule.m_newModule(ruby);
        
        mod.setRubyClass(rubyClass);
        //rubyClass.callInit();
        
        return mod;
    }
    
    /** rb_mod_attr
     *
     */
    public RubyObject m_attr(RubySymbol symbol, RubyBoolean writeable) {
        addAttribute(symbol.toId(), true, writeable.isTrue(), 0 /*act_scope*/);
        
        return getRuby().getNil();
    }

    /** rb_mod_attr_reader
     *
     */
    public RubyObject m_attr_reader(RubyObject[] args) {
        for (int i = 0; i < args.length; i++) {
            addAttribute(((RubySymbol)args[i]).toId(), true, false, 0 /*act_scope*/);
        }
        
        return getRuby().getNil();
    }

    /** rb_mod_attr_writer
     *
     */
    public RubyObject m_attr_writer(RubyObject[] args) {
        for (int i = 0; i < args.length; i++) {
            addAttribute(((RubySymbol)args[i]).toId(), false, true, 0 /*act_scope*/);
        }
        
        return getRuby().getNil();
    }

    /** rb_mod_attr_accessor
     *
     */
    public RubyObject m_attr_accessor(RubyObject[] args) {
        for (int i = 0; i < args.length; i++) {
            addAttribute(((RubySymbol)args[i]).toId(), true, true, 0 /*act_scope*/);
        }
        
        return getRuby().getNil();
    }

    /** rb_mod_const_get
     *
     */
    public RubyObject m_const_get(RubySymbol symbol) {
        RubyId id = symbol.toId();
        
        if (!id.isConstId()) {
            throw new RubyNameException("wrong constant name " + symbol.getName());
        }
        
        return getConstant(id);
    }

    /** rb_mod_const_set
     *
     */
    public RubyObject m_const_set(RubySymbol symbol, RubyObject value) {
        RubyId id = symbol.toId();
        
        if (!id.isConstId()) {
            throw new RubyNameException("wrong constant name " + symbol.getName());
        }
        
        setConstant(id, value);
        
        return value;
    }
    
    /** rb_mod_const_defined
     *
     */
    public RubyBoolean m_const_defined(RubySymbol symbol) {
        RubyId id = symbol.toId();
        
        if (!id.isConstId()) {
            throw new RubyNameException("wrong constant name " + symbol.getName());
        }
        
        return RubyBoolean.m_newBoolean(getRuby(), isConstantDefined(id));
    }
    
    /** Getter for property included.
     * @return Value of property included.
     */
    public boolean isIncluded() {
        return this.included;
    }
    
    /** Setter for property included.
     * @param included New value of property included.
     */
    public void setIncluded(boolean included) {
        this.included = included;
    }
    
}