/*
 * RubyObject.java - No description
 * Created on 04. Juli 2001, 22:53
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

import java.lang.ref.*;
import org.jruby.core.*;
import org.jruby.exceptions.*;
import org.jruby.nodes.*;
import org.jruby.runtime.*;
import org.jruby.util.*;
/**
 *
 * @author  jpetersen
 */
public class RubyObject {
    // A reference to the JRuby runtime.
    private Ruby ruby;

    // The class of this object
    private RubyClass rubyClass;

    // The instance variables of this object.
    private RubyMap instanceVariables;

    // The two properties frozen and taint
    private boolean frozen;
    private boolean taint;

	public RubyObject(Ruby ruby) {
        this(ruby, null, false);
    }
    
    public RubyObject(Ruby ruby, RubyClass rubyClass) {
        this(ruby, rubyClass, true);
    }

    public RubyObject(Ruby ruby, RubyClass rubyClass, boolean objectSpace) {
        this.ruby = ruby;
        this.rubyClass = rubyClass;
        this.instanceVariables = new RubyHashMap();
        this.frozen = false;
        this.taint = false;

        // Add this Object in the ObjectSpace
        if (objectSpace) {
            ruby.objectSpace.add(new SoftReference(this));
        }
    }

    public static RubyObject nilObject(Ruby ruby) {
        if (ruby.getNil() != null) {
            return ruby.getNil();
        } else {
        	return new RubyObject(ruby) {
        	    public boolean isNil() {
        	        return true;
        	    }
        	};
        }
    }

    public Class getJavaClass() {
        return RubyObject.class;
    }

    /**
     * This method is just a wrapper around the Ruby "hash" method,
     * provided so that RubyObjects can be used as keys in the Java
     * HashMap object underlying RubyHash.
     */
    public int hashCode() {
        return RubyNumeric.fix2int(funcall(getRuby().intern("hash")));
    }

    /**
     * This method is just a wrapper around the Ruby "==" method,
     * provided so that RubyObjects can be used as keys in the Java
     * HashMap object underlying RubyHash.
     */
    public boolean equals(Object other) {
        return other == this || (other instanceof RubyObject) && funcall(getRuby().intern("=="), (RubyObject) other).isTrue();
    }

    public String toString() {
        return to_s().getValue();
    }

    /** Getter for property ruby.
     * @return Value of property ruby.
     */
    public Ruby getRuby() {
        return this.ruby;
    }

    public RubyMap getInstanceVariables() {
        return instanceVariables;
    }

    public void setInstanceVariables(RubyMap instanceVariables) {
        this.instanceVariables = instanceVariables;
    }

    /**
     * Gets the rubyClass.
     * @return Returns a RubyClass
     */
    public RubyClass getRubyClass() {
        if (isNil()) {
            return getRuby().getClasses().getNilClass();
        }

        return rubyClass;
    }

    /**
     * Sets the rubyClass.
     * @param rubyClass The rubyClass to set
     */
    public void setRubyClass(RubyClass rubyClass) {
        this.rubyClass = rubyClass;
    }

    /**
     * Gets the frozen.
     * @return Returns a boolean
     */
    public boolean isFrozen() {
        return frozen;
    }

    /**
     * Sets the frozen.
     * @param frozen The frozen to set
     */
    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    /**
     * Gets the taint.
     * @return Returns a boolean
     */
    public boolean isTaint() {
        return taint;
    }

    /**
     * Sets the taint.
     * @param taint The taint to set
     */
    public void setTaint(boolean taint) {
        this.taint = taint;
    }

    public boolean isNil() {
        return false;
    }

    public boolean isTrue() {
        return !isNil();
    }

    public boolean isFalse() {
        return isNil();
    }

    public static void createObjectClass(RubyModule kernelModule) {
        Callback clone = new ReflectionCallbackMethod(RubyObject.class, "rbClone");
        Callback dup = new ReflectionCallbackMethod(RubyObject.class, "dup");
        Callback equal = new ReflectionCallbackMethod(RubyObject.class, "equal", RubyObject.class);
        Callback extend = new ReflectionCallbackMethod(RubyObject.class, "extend", RubyModule[].class, true);
        Callback freeze = new ReflectionCallbackMethod(RubyObject.class, "freeze");
        Callback frozen = new ReflectionCallbackMethod(RubyObject.class, "frozen");
        Callback id = new ReflectionCallbackMethod(RubyObject.class, "id");
        Callback inspect = new ReflectionCallbackMethod(RubyObject.class, "inspect");
        Callback instance_eval =
            new ReflectionCallbackMethod(RubyObject.class, "instance_eval", RubyObject[].class, true);
        Callback instance_of = new ReflectionCallbackMethod(RubyObject.class, "instance_of", RubyModule.class);
        Callback kind_of = new ReflectionCallbackMethod(RubyObject.class, "kind_of", RubyModule.class);
        Callback method = new ReflectionCallbackMethod(RubyObject.class, "method", RubyObject.class);
        Callback methods = new ReflectionCallbackMethod(RubyObject.class, "methods");
        Callback private_methods = new ReflectionCallbackMethod(RubyObject.class, "private_methods");
        Callback protected_methods = new ReflectionCallbackMethod(RubyObject.class, "protected_methods");
        Callback taint = new ReflectionCallbackMethod(RubyObject.class, "taint");
        Callback tainted = new ReflectionCallbackMethod(RubyObject.class, "tainted");
        Callback to_a = new ReflectionCallbackMethod(RubyObject.class, "to_a");
        Callback to_s = new ReflectionCallbackMethod(RubyObject.class, "to_s");
        Callback type = new ReflectionCallbackMethod(RubyObject.class, "type");
        Callback untaint = new ReflectionCallbackMethod(RubyObject.class, "untaint");

        kernelModule.defineMethod("=~", DefaultCallbackMethods.getMethodFalse());
        kernelModule.defineMethod("==", equal);
        kernelModule.defineMethod("class", type);
        kernelModule.defineMethod("clone", clone);
        kernelModule.defineMethod("dup", dup);
        kernelModule.defineMethod("eql?", equal);
        kernelModule.defineMethod("extend", extend);
        kernelModule.defineMethod("freeze", freeze);
        kernelModule.defineMethod("frozen?", frozen);
        kernelModule.defineMethod("hash", id);
        kernelModule.defineMethod("id", id);
        kernelModule.defineMethod("__id__", id);
        kernelModule.defineMethod("inspect", inspect);
        kernelModule.defineMethod("instance_eval", instance_eval);
        kernelModule.defineMethod("instance_of?", instance_of);
        kernelModule.defineMethod("is_a?", kind_of);
        kernelModule.defineMethod("kind_of?", kind_of);
        kernelModule.defineMethod("method", method);
        kernelModule.defineMethod("methods", methods);
        kernelModule.defineMethod("private_methods", private_methods);
        kernelModule.defineMethod("protected_methods", protected_methods);
        kernelModule.defineMethod("public_methods", methods);
        kernelModule.defineMethod("nil?", DefaultCallbackMethods.getMethodFalse());
        kernelModule.defineMethod("taint", taint);
        kernelModule.defineMethod("tainted?", tainted);
        kernelModule.defineMethod("to_a", to_a);
        kernelModule.defineMethod("to_s", to_s);
        kernelModule.defineMethod("type", type);
        kernelModule.defineMethod("untaint", untaint);

        kernelModule.defineAlias("===", "==");
        kernelModule.defineAlias("equal?", "==");
    }

    protected int argCount(RubyObject[] args, int min, int max) {
        int len = args.length;
        if (len < min || (max > -1 && len > max)) {
            throw new RubyArgumentException(getRuby(), "wrong number of arguments");
        }
        return len;
    }

    /** rb_special_const_p
     *
     */
    public boolean isSpecialConst() {
        //return (isImmediate() || isNil());
        return isNil();
    }

    /** SPECIAL_SINGLETON(x,c)
     *
     */
    private RubyClass getSpecialSingleton() {
        RubyClass rubyClass = getRubyClass();
        if (!rubyClass.isSingleton()) {
            rubyClass = rubyClass.newSingletonClass();
            rubyClass.attachSingletonClass(this);
        }
        return rubyClass;
    }

    /** rb_singleton_class
     *
     */
    public RubyClass getSingletonClass() {
        //        if (getType() == Type.FIXNUM || isSymbol()) {
        //            throw new RubyTypeException("can't define singleton");
        //        }
        if (isSpecialConst()) {
            if (isNil() || isTrue() || isFalse()) {
                return getSpecialSingleton();
            }
            throw new RubyBugException("unknown immediate " + toString());
        }
        //synchronize(this) {
        RubyClass type = null;
        if (getRubyClass().isSingleton()) {
            type = getRubyClass();
        } else {
            type = getRubyClass().newSingletonClass();
            setRubyClass(type);
            type.attachSingletonClass(this);
        }
        type.setTaint(isTaint());
        type.setFrozen(isFrozen());
        //}
        return type;
    }

    /** rb_define_singleton_method
     *
     */
    public void defineSingletonMethod(String name, Callback method) {
        getSingletonClass().defineMethod(name, method);
    }

    /** OBJSETUP
     *@deprecated use setRubyClass(RubyClass rubyClass) instead
     */
    protected void setupObject(RubyClass rubyClass) {
        setRubyClass(rubyClass);
    }

    /** CLONESETUP
     *
     */
    protected void setupClone(RubyObject obj) {
        setRubyClass(obj.getRubyClass().getSingletonClassClone());
        getRubyClass().attachSingletonClass(this);
    }

    /** OBJ_INFECT
     *
     */
    protected void infectObject(RubyObject obj) {
        if (obj.isTaint()) {
            setTaint(true);
        }
    }

    /** rb_funcall2
     *
     */
    public RubyObject funcall(RubyId mid, RubyObject[] args) {
        return funcall(mid, new RubyPointer(args));
    }

    public RubyObject funcall(RubyId mid, RubyPointer args) {
        return getRubyClass().call(this, mid, args, 1);
    }

    public RubyObject funcall(RubyId mid) {
        return funcall(mid, (RubyPointer) null);
    }

    /** rb_funcall3
     *
     */
    public RubyObject funcall3(RubyId mid, RubyPointer args) {
        return getRubyClass().call(this, mid, args, 0);
    }

    public RubyObject funcall3(RubyId mid, RubyObject[] args) {
        return funcall3(mid, new RubyPointer(args));
    }

    /** rb_funcall
     *
     */
    public RubyObject funcall(RubyId mid, RubyObject arg) {
        return funcall(mid, new RubyPointer(new RubyObject[] { arg }));
    }

    /** rb_iv_get
     *
     */
    public RubyObject getInstanceVar(String name) {
        return getInstanceVar(getRuby().intern(name));
    }

    /** rb_iv_set
     *
     */
    public void setInstanceVar(String name, RubyObject value) {
        setInstanceVar(getRuby().intern(name), value);
    }

    /** rb_ivar_get
     *
     */
    public RubyObject getInstanceVar(RubyId id) {
        if (getInstanceVariables() != null) {
            RubyObject value = (RubyObject) getInstanceVariables().get(id);
            if (value != null) {
                return value;
            }
        }
        // todo: add warn if verbose
        return getRuby().getNil();
    }

    public boolean isInstanceVarDefined(RubyId id) {
        if (getInstanceVariables() != null) {
            if (getInstanceVariables().get(id) != null) {
                return true;
            }
        }
        return false;
    }

    /** rb_ivar_set
     *
     */
    public RubyObject setInstanceVar(RubyId id, RubyObject value) {
        if (isTaint() && getRuby().getSecurityLevel() >= 4) {
            throw new RubySecurityException(getRuby(), "Insecure: can't modify instance variable");
        }
        if (isFrozen()) {
            throw new RubyFrozenException(getRuby(), "");
        }
        if (getInstanceVariables() == null) {
            setInstanceVariables(new RubyHashMap());
        }
        getInstanceVariables().put(id, value);
        return value;
    }

    /** rb_cvar_singleton
     *
     */
    public RubyModule getClassVarSingleton() {
        return getRubyClass();
    }

    /** rb_eval
     *
     */
    public RubyObject eval(Node n) {
        return n == null ? getRuby().getNil() : n.eval(getRuby(), this);
    }

    public RubyObject evalNode(Node n) {
        Node beginTree = ruby.getParserHelper().getEvalTreeBegin();
        ruby.getParserHelper().setEvalTreeBegin(null);
        if (beginTree != null) {
            eval(beginTree);
        }
        if (n == null) {
            return getRuby().getNil();
        }
        return eval(n);
    }

    public void callInit(RubyObject[] args) {
        ruby.getIter().push(ruby.isBlockGiven() ? RubyIter.ITER_PRE : RubyIter.ITER_NOT);
        funcall(getRuby().intern("initialize"), args);
        ruby.getIter().pop();
    }

    public void extendObject(RubyModule module) {
        getSingletonClass().includeModule(module);
    }

    /** rb_to_id
     *
     */
    public RubyId toId() {
        throw new RubyTypeException(getRuby(), inspect().getValue() + " is not a symbol");
    }

    /** rb_convert_type
     *
     */
    public RubyObject convertType(Class type, String className, String method) {
        if (type.isAssignableFrom(getClass())) {
            return this;
        }
        RubyObject result = null;
        try {
            result = funcall(getRuby().intern(method));
        } catch (RubyNameException rnExcptn) {
            throw new RubyTypeException(getRuby(), "failed to convert " + getRubyClass().toName() + " into " + className);
            //        } catch (RubyS rnExcptn) {
        }
        if (!type.isAssignableFrom(result.getClass())) {
            throw new RubyTypeException(getRuby(), getRubyClass().toName() + "#" + method + " should return " + className);
        }
        return result;
    }

    /** specific_eval
     * 
     */
    public RubyObject specificEval(RubyModule mod, RubyObject[] args) {
        if (getRuby().isBlockGiven()) {
            if (args.length > 0) {
                throw new RubyArgumentException(getRuby(), "wrong # of arguments (" + args.length + " for 0)");
            }
            return yieldUnder(mod);
        } else {
            if (args.length == 0) {
                throw new RubyArgumentException(getRuby(), "block not supplied");
            } else if (args.length > 3) {
                String lastFuncName = ruby.getRubyFrame().getLastFunc().toName();
                throw new RubyArgumentException(
                    getRuby(),
                    "wrong # of arguments: " + lastFuncName + "(src) or " + lastFuncName + "{..}");
            }
            /*
            if (ruby.getSecurityLevel() >= 4) {
            	Check_Type(argv[0], T_STRING);
            } else {
            	Check_SafeStr(argv[0]);
            }
            */
            RubyObject file = args.length > 1 ? args[1] : RubyString.newString(getRuby(), "(eval)");
            RubyObject line = args.length > 2 ? args[2] : RubyFixnum.one(getRuby());
            return evalUnder(mod, args[0], file, line);
        }
    }

    public RubyObject evalUnder(RubyModule under, RubyObject src, RubyObject file, RubyObject line) {
        /*
        if (ruby_safe_level >= 4) {
        	Check_Type(src, T_STRING);
        } else {
        	Check_SafeStr(src);
        	}
        */
        return under.executeUnder(new Callback() {
            public RubyObject execute(RubyObject self, RubyObject[] args, Ruby ruby) {
                return args[0].eval(args[1], ruby.getNil(), ((RubyString) args[2]).getValue(), RubyNumeric.fix2int(args[3]));
            }
        }, new RubyObject[] { this, src, file, line });
    }

    public RubyObject yieldUnder(RubyModule under) {
        return under.executeUnder(new Callback() {
            public RubyObject execute(RubyObject self, RubyObject[] args, Ruby ruby) {
                if ((ruby.getBlock().flags & RubyBlock.BLOCK_DYNAMIC) != 0) {
                    RubyBlock oldBlock = ruby.getBlock();
                    RubyBlock block = ruby.getBlock();
                    /* copy the block to avoid modifying global data. */
                    block.frame.setCbase(ruby.getRubyFrame().getCbase());
                    ruby.setBlock(block);
                    RubyObject result = null;
                    try {
                        result = ruby.yield0(args[0], args[0], ruby.getRubyClass(), false);
                    } finally {
                        ruby.setBlock(oldBlock);
                    }
                    return result;
                }
                /* static block, no need to restore */
                ruby.getBlock().frame.setCbase(ruby.getRubyFrame().getCbase());
                return ruby.yield0(args[0], args[0], ruby.getRubyClass(), false);
            }
        }, new RubyObject[] { this });
    }

    public RubyObject eval(RubyObject src, RubyObject scope, String file, int line) {
        String fileSave = ruby.getSourceFile();
        int lineSave = ruby.getSourceLine();
        int iter = ruby.getRubyFrame().getIter();
        if (file == null) {
            file = ruby.getSourceFile();
            line = ruby.getSourceLine();
        }
        if (!scope.isNil()) {
            /*
            if (!rb_obj_is_block(scope)) {
            	rb_raise(rb_eTypeError, "wrong argument type %s (expected Proc/Binding)", rb_class2name(CLASS_OF(scope)));
            }
            
            Data_Get_Struct(scope, struct BLOCK, data);
            
            // PUSH BLOCK from data
            frame = data->frame;
            frame.tmp = ruby_frame;	// gc protection
            ruby_frame = &(frame);
            old_scope = ruby_scope;
            ruby_scope = data->scope;
            old_block = ruby_block;
            ruby_block = data->prev;
            old_dyna_vars = ruby_dyna_vars;
            ruby_dyna_vars = data->dyna_vars;
            old_vmode = scope_vmode;
            scope_vmode = data->vmode;
            old_cref = (VALUE)ruby_cref;
            ruby_cref = (NODE*)ruby_frame->cbase;
            old_wrapper = ruby_wrapper;
            ruby_wrapper = data->wrapper;
            
            self = data->self;
            ruby_frame->iter = data->iter;
            */
        } else {
            if (ruby.getRubyFrame().getPrev() != null) {
                ruby.getRubyFrame().setIter(ruby.getRubyFrame().getPrev().getIter());
            }
        }
        getRuby().pushClass();
        ruby.setRubyClass(ruby.getCBase());
        ruby.setInEval(ruby.getInEval() + 1);
        if (ruby.getRubyClass().isIncluded()) {
            ruby.setRubyClass(((RubyIncludedClass) ruby.getRubyClass()).getDelegate());
        }
        RubyObject result = getRuby().getNil();
        try {
            // result = ruby_errinfo;
            // ruby_errinfo = Qnil;
            Node node = getRuby().getRubyParser().compileString(file, src, line);
            // if (ruby_nerrs > 0) {
            // 	compile_error(0);
            //}
            // if (!result.isNil()) {
            //	ruby_errinfo = result;
            //}
            result = evalNode(node);
        } catch (RaiseException rExcptn) {
            /*
            VALUE err;
            VALUE errat;
            
            if (strcmp(file, "(eval)") == 0) {
            	if (ruby_sourceline > 1) {
            			errat = get_backtrace(ruby_errinfo);
            		err = RARRAY(errat)->ptr[0];
            		rb_str_cat2(err, ": ");
            		rb_str_append(err, ruby_errinfo);
            	} else {
            	err = rb_str_dup(ruby_errinfo);
            	}
            	errat = Qnil;
            	rb_exc_raise(rb_exc_new3(CLASS_OF(ruby_errinfo), err));
            }
            rb_exc_raise(ruby_errinfo);
            */
        } finally {
            ruby.popClass();
            ruby.setInEval(ruby.getInEval());
            if (!scope.isNil()) {
                /*
                int dont_recycle = ruby_scope->flag & SCOPE_DONT_RECYCLE;
                
                ruby_wrapper = old_wrapper;
                ruby_cref  = (NODE*)old_cref;
                ruby_frame = frame.tmp;
                ruby_scope = old_scope;
                ruby_block = old_block;
                ruby_dyna_vars = old_dyna_vars;
                data->vmode = scope_vmode; // write back visibility mode
                scope_vmode = old_vmode;
                if (dont_recycle) {
                	struct tag *tag;
                	struct RVarmap *vars;
                
                	scope_dup(ruby_scope);
                	for (tag=prot_tag; tag; tag=tag->prev) {
                		scope_dup(tag->scope);
                	}
                	if (ruby_block) {
                		struct BLOCK *block = ruby_block;
                		while (block) {
                			block->tag->flags |= BLOCK_DYNAMIC;
                			block = block->prev;
                		}
                	}
                	for (vars = ruby_dyna_vars; vars; vars = vars->next) {
                		FL_SET(vars, DVAR_DONT_RECYCLE);
                	}
                }
                */
            } else {
                ruby.getRubyFrame().setIter(iter);
            }
            ruby.setSourceFile(fileSave);
            ruby.setSourceLine(lineSave);
        }
        return result;
    }

    // Methods of the Object class (rb_obj_*):

    /** rb_obj_equal
     *
     */
    public RubyBoolean equal(RubyObject obj) {
        return RubyBoolean.newBoolean(getRuby(), this == obj);
    }

    /** Return the internal id of an object.
     * 
     * <b>Warning:</b> In JRuby there is no guarantee that two objects have different ids.
     * 
     * <i>CRuby function: rb_obj_id</i>
     *
     */
    public RubyObject id() {
        return RubyFixnum.newFixnum(getRuby(), System.identityHashCode(this));
    }

    /** rb_obj_type
     *
     */
    public RubyClass type() {
        RubyClass type = getRubyClass();
        while (type.isSingleton() || type.isIncluded()) {
            type = type.getSuperClass();
        }
        return type;
    }

    /** rb_obj_clone
     *
     */
    public RubyObject rbClone() {
        RubyObject clone = new RubyObject(getRuby(), (RubyClass) getRubyClass());
        clone.setupClone(this);
        clone.setInstanceVariables(getInstanceVariables().cloneRubyMap());
        return clone;
    }

    /** rb_obj_dup
     *
     */
    public RubyObject dup() {
        RubyObject dup = funcall(getRuby().intern("clone"));
        if (!dup.getClass().equals(getClass())) {
            throw new RubyTypeException(getRuby(), "duplicated object must be same type");
        }
        if (!dup.isSpecialConst()) {
            dup.setRubyClass(type());
            dup.infectObject(this);
        }
        return dup;
    }

    /** rb_obj_tainted
     *
     */
    public RubyBoolean tainted() {
        if (isTaint()) {
            return getRuby().getTrue();
        } else {
            return getRuby().getFalse();
        }
    }

    /** rb_obj_taint
     *
     */
    public RubyObject taint() {
        getRuby().secure(4);
        if (!isTaint()) {
            if (isFrozen()) {
                throw new RubyFrozenException(getRuby(), "object");
            }
            setTaint(true);
        }
        return this;
    }

    /** rb_obj_untaint
     *
     */
    public RubyObject untaint() {
        getRuby().secure(3);
        if (isTaint()) {
            if (isFrozen()) {
                throw new RubyFrozenException(getRuby(), "object");
            }
            setTaint(false);
        }
        return this;
    }

    /** Freeze an object.
     * 
     * rb_obj_freeze
     * 
     */
    public RubyObject freeze() {
        if (getRuby().getSecurityLevel() >= 4 && isTaint()) {
            throw new RubySecurityException(getRuby(), "Insecure: can't freeze object");
        }
        setFrozen(true);
        return this;
    }

    /** rb_obj_frozen_p
     *
     */
    public RubyBoolean frozen() {
        return RubyBoolean.newBoolean(getRuby(), isFrozen());
    }

    /** rb_obj_inspect
     *
     */
    public RubyString inspect() {
        //     if (TYPE(obj) == T_OBJECT
        // 	&& ROBJECT(obj)->iv_tbl
        // 	&& ROBJECT(obj)->iv_tbl->num_entries > 0) {
        // 	VALUE str;
        // 	char *c;
        //
        // 	c = rb_class2name(CLASS_OF(obj));
        // 	/*if (rb_inspecting_p(obj)) {
        // 	    str = rb_str_new(0, strlen(c)+10+16+1); /* 10:tags 16:addr 1:eos */
        // 	    sprintf(RSTRING(str)->ptr, "#<%s:0x%lx ...>", c, obj);
        // 	    RSTRING(str)->len = strlen(RSTRING(str)->ptr);
        // 	    return str;
        // 	}*/
        // 	str = rb_str_new(0, strlen(c)+6+16+1); /* 6:tags 16:addr 1:eos */
        // 	sprintf(RSTRING(str)->ptr, "-<%s:0x%lx ", c, obj);
        // 	RSTRING(str)->len = strlen(RSTRING(str)->ptr);
        // 	return rb_protect_inspect(inspect_obj, obj, str);
        //     }
        //     return rb_funcall(obj, rb_intern("to_s"), 0, 0);
        // }
        return (RubyString) funcall(getRuby().intern("to_s"));
    }

    /** rb_obj_is_instance_of
     *
     */
    public RubyBoolean instance_of(RubyModule type) {
        return RubyBoolean.newBoolean(getRuby(), type() == type);
    }

    /** rb_obj_is_kind_of
     *
     */
    public RubyBoolean kind_of(RubyModule type) {
        RubyClass currType = getRubyClass();
        while (currType != null) {
            if (currType == type || currType.getMethods().keySet().retainAll(type.getMethods().keySet())) {
                return getRuby().getTrue();
            }
            currType = currType.getSuperClass();
        }
        return getRuby().getFalse();
    }

    /** rb_obj_methods
     *
     */
    public RubyObject methods() {
        return getRubyClass().instance_methods(new RubyObject[] { getRuby().getTrue()});
    }

    /** rb_obj_protected_methods
     *
     */
    public RubyObject protected_methods() {
        return getRubyClass().protected_instance_methods(new RubyObject[] { getRuby().getTrue()});
    }

    /** rb_obj_private_methods
     *
     */
    public RubyObject private_methods() {
        return getRubyClass().private_instance_methods(new RubyObject[] { getRuby().getTrue()});
    }

    /** rb_obj_singleton_methods
     *
     */
    public RubyArray singleton_methods() {
        RubyArray ary = RubyArray.newArray(getRuby());
        RubyClass type = getRubyClass();
        while (type != null && type.isSingleton()) {
            type.getMethods().foreach(new RubyMapMethod() {
                public int execute(Object key, Object value, Object arg) {
                    RubyString name = RubyString.newString(getRuby(), ((RubyId) key).toName());
                    if ((((MethodNode) value).getNoex() & (Constants.NOEX_PRIVATE | Constants.NOEX_PROTECTED)) == 0) {
                        if (((RubyArray) arg).includes(name).isFalse()) {
                            if (((MethodNode) value).getBodyNode() == null) {
                                ((RubyArray) arg).push(getRuby().getNil());
                            }
                            ((RubyArray) arg).push(name);
                        }
                    } else if (((MethodNode) value).getBodyNode() instanceof ZSuperNode) {
                        ((RubyArray) arg).push(getRuby().getNil());
                        ((RubyArray) arg).push(name);
                    }
                    return CONTINUE;
                }
            }, ary);
            type = type.getSuperClass();
        }
        ary.compact_bang();
        return ary;
    }

    public RubyObject method(RubyObject symbol) {
        return getRubyClass().newMethod(this, symbol.toId(), getRuby().getClasses().getMethodClass());
    }

    public RubyArray to_a() {
        return RubyArray.newArray(getRuby(), this);
    }

    public RubyString to_s() {
        String cname = getRubyClass().toName();
        RubyString str = RubyString.newString(getRuby(), "");
        /* 6:tags 16:addr 1:eos */
        str.setValue("#<" + cname + ":0x" + Integer.toHexString(System.identityHashCode(this)) + ">");
        str.setTaint(isTaint());
        return str;
    }

    public RubyObject instance_eval(RubyObject[] args) {
        return specificEval(getSingletonClass(), args);
    }

    public RubyObject extend(RubyObject args[]) {
        if (args.length == 0) {
            throw new RubyArgumentException(ruby, "wrong # of arguments");
        }
        // FIXME: Check_Type?
        for (int i = 0; i < args.length; i++) {
            args[i].funcall(ruby.intern("extend_object"), this);
        }
        return this;
    }
}
