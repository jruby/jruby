/*
 * RubyObject.java - No description
 * Created on 04. Juli 2001, 22:53
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <japetersen@web.de>
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
    private Ruby ruby;

    private RubyModule rubyClass;
    private RubyMap instanceVariables;

    private boolean frozen = false;
    private boolean taint = false;

    // deprecated ???
    private boolean immediate = false;

    public RubyObject(Ruby ruby) {
        this(ruby, null);
    }

    public RubyObject(Ruby ruby, RubyModule rubyClass) {
        this.ruby = ruby;
        this.rubyClass = rubyClass;

        ruby.objectSpace.add(new SoftReference(this));
    }

    public Class getJavaClass() {
        return RubyObject.class;
    }

    /** Getter for property frozen.
     * @return Value of property frozen.
     */
    public boolean isFrozen() {
        return this.frozen;
    }

    /** Setter for property frozen.
     * @param frozen New value of property frozen.
     */
    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    /** Getter for property immediate.
     * @return Value of property immediate.
     */
    public boolean isImmediate() {
        return this.immediate;
    }

    /** Setter for property immediate.
     * @param immediate New value of property immediate.
     */
    public void setImmediate(boolean immediate) {
        this.immediate = immediate;
    }

    /** Getter for property ruby.
     * @return Value of property ruby.
     */
    public Ruby getRuby() {
        return this.ruby;
    }

    /** Getter for property rubyClass.
     * @return Value of property rubyClass.
     */
    public RubyModule getRubyClass() {
        return this.rubyClass;
    }

    /** Setter for property rubyClass.
     * @param rubyClass New value of property rubyClass.
     */
    public void setRubyClass(RubyModule rubyClass) {
        this.rubyClass = rubyClass;
    }

    /** Getter for property taint.
     * @return Value of property taint.
     */
    public boolean isTaint() {
        return this.taint;
    }

    /** Setter for property taint.
     * @param taint New value of property taint.
     */
    public void setTaint(boolean taint) {
        this.taint = taint;
    }

    public RubyMap getInstanceVariables() {
        if (instanceVariables == null) {
            instanceVariables = new RubyHashMap();
        }
        return instanceVariables;
    }

    public void setInstanceVariables(RubyMap instanceVariables) {
        this.instanceVariables = instanceVariables;
    }

    // methods

    public boolean isNil() {
        return false;
    }

    public boolean isTrue() {
        return true;
    }

    public boolean isFalse() {
        return false;
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
        return (isImmediate() || isNil());
    }

    /** SPECIAL_SINGLETON(x,c)
     *
     */
    private RubyClass getSpecialSingleton() {
        RubyClass rubyClass = (RubyClass) getRubyClass();
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
        RubyClass rbClass = null;

        if (getRubyClass().isSingleton()) {
            rbClass = (RubyClass) getRubyClass();
        } else {
            rbClass = ((RubyClass) getRubyClass()).newSingletonClass();
            setRubyClass(rbClass);
            rbClass.attachSingletonClass(this);
        }

        rbClass.setTaint(isTaint());

        if (isFrozen()) {
            rbClass.setFrozen(true);
        }
        //}
        return rbClass;
    }

    /** rb_define_singleton_method
     *
     */
    public void defineSingletonMethod(String name, RubyCallbackMethod method) {
        getSingletonClass().defineMethod(name, method);
    }

    /** OBJSETUP
     *
     */
    protected void setupObject(RubyModule rubyClass) {
        setRubyClass(rubyClass);
    }

    /** CLONESETUP
     *
     */
    protected void setupClone(RubyObject obj) {
        setupObject(obj.getRubyClass().getSingletonClassClone());

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
            throw new RubySecurityException(
                getRuby(),
                "Insecure: can't modify instance variable");
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
        ruby.getIter().push(
            ruby.isBlockGiven() ? RubyIter.ITER_PRE : RubyIter.ITER_NOT);
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
        throw new RubyTypeException(
            getRuby(),
            m_inspect().getValue() + " is not a symbol");
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
            throw new RubyTypeException(
                getRuby(),
                "failed to convert " + getRubyClass().toName() + " into " + className);
            //} catch (RubyS rnExcptn) {
        }

        if (!type.isAssignableFrom(result.getClass())) {
            throw new RubyTypeException(
                getRuby(),
                getRubyClass().toName() + "#" + method + " should return " + className);
        }

        return result;
    }

    /** specific_eval
     * 
     */
    public RubyObject specificEval(RubyModule mod, RubyObject[] args) {
        if (getRuby().isBlockGiven()) {
            if (args.length > 0) {
                throw new RubyArgumentException(
                    getRuby(),
                    "wrong # of arguments (" + args.length + " for 0)");
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

            RubyObject file =
                args.length > 1 ? args[1] : RubyString.m_newString(getRuby(), "(eval)");
            RubyObject line = args.length > 2 ? args[2] : RubyFixnum.one(getRuby());

            return evalUnder(mod, args[0], file, line);
        }
    }

    public RubyObject evalUnder(
        RubyModule under,
        RubyObject src,
        RubyObject file,
        RubyObject line) {
        /*
        if (ruby_safe_level >= 4) {
        	Check_Type(src, T_STRING);
        } else {
        	Check_SafeStr(src);
        	}
        */

        return under.executeUnder(new RubyCallbackMethod() {
            public RubyObject execute(RubyObject self, RubyObject[] args, Ruby ruby) {
                return args[0].eval(
                    args[1],
                    ruby.getNil(),
                    ((RubyString) args[2]).getValue(),
                    RubyNumeric.fix2int(args[3]));
            }
        }, new RubyObject[] { this, src, file, line });
    }

    public RubyObject yieldUnder(RubyModule under) {
        return under.executeUnder(new RubyCallbackMethod() {
            public RubyObject execute(RubyObject self, RubyObject[] args, Ruby ruby) {
                if ((ruby.getBlock().flags & RubyBlock.BLOCK_DYNAMIC) != 0) {
                    RubyBlock oldBlock = ruby.getBlock();

                    RubyBlock block = ruby.getBlock();
                    /* copy the block to avoid modifying global data. */ block.frame.setCbase(
                    ruby.getRubyFrame().getCbase());
                    ruby.setBlock(block);

                    RubyObject result = null;

                    try {
                        result = ruby.yield0(args[0], args[0], ruby.getRubyClass(), false);
                    } finally {
                        ruby.setBlock(oldBlock);
                    }

                    return result;
                }
                /* static block, no need to restore */ ruby.getBlock().frame.setCbase(
                ruby.getRubyFrame().getCbase());
                return ruby.yield0(args[0], args[0], ruby.getRubyClass(), false);
            }
        }, new RubyObject[] { this });
    }

    public RubyObject eval(
        RubyObject src,
        RubyObject scope,
        String file,
        int line) {
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
            ruby.setRubyClass(ruby.getRubyClass().getRubyClass());
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
    public RubyBoolean m_equal(RubyObject obj) {
        if (this == obj) {
            return getRuby().getTrue();
        } else {
            return (RubyBoolean) funcall(getRuby().intern("=="), obj);
        }
    }

    /** rb_obj_id
     *
     */
    public RubyObject m_id() {
        return RubyFixnum.m_newFixnum(getRuby(), hashCode());
    }

    /** rb_obj_type
     *
     */
    public RubyModule m_type() {
        RubyModule rbClass = getRubyClass();

        while (rbClass.isSingleton() || rbClass.isIncluded()) {
            rbClass = rbClass.getSuperClass();
        }

        return rbClass;
    }

    /** rb_obj_clone
     *
     */
    public RubyObject m_clone() {
        RubyObject clone = new RubyObject(getRuby(), (RubyClass) getRubyClass());
        clone.setupClone(this);

        clone.setInstanceVariables(getInstanceVariables().cloneRubyMap());

        return clone;
    }

    /** rb_obj_dup
     *
     */
    public RubyObject m_dup() {
        RubyObject dup = funcall(getRuby().intern("clone"));

        if (!dup.getClass().equals(getClass())) {
            throw new RubyTypeException(getRuby(), "duplicated object must be same type");
        }

        if (!dup.isSpecialConst()) {
            dup.setupObject(m_type());
            dup.infectObject(this);
        }

        return dup;
    }

    /** rb_obj_tainted
     *
     */
    public RubyBoolean m_tainted() {
        if (isTaint()) {
            return getRuby().getTrue();
        } else {
            return getRuby().getFalse();
        }
    }

    /** rb_obj_taint
     *
     */
    public RubyObject m_taint() {
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
    public RubyObject m_untaint() {
        getRuby().secure(3);

        if (isTaint()) {
            if (isFrozen()) {
                throw new RubyFrozenException(getRuby(), "object");
            }
            setTaint(false);
        }

        return this;
    }

    /** rb_obj_freeze
     *
     */
    public RubyObject m_freeze() {
        if (getRuby().getSecurityLevel() >= 4 && isTaint()) {
            throw new RubySecurityException(getRuby(), "Insecure: can't freeze object");
        }

        // ??????????????

        return this;
    }

    /** rb_obj_frozen_p
     *
     */
    public RubyBoolean m_frozen() {
        if (isFrozen()) {
            return getRuby().getTrue();
        } else {
            return getRuby().getFalse();
        }
    }

    /** rb_obj_inspect
     *
     */
    public RubyString m_inspect() {
        //     if (TYPE(obj) == T_OBJECT
        // 	&& ROBJECT(obj)->iv_tbl
        // 	&& ROBJECT(obj)->iv_tbl->num_entries > 0) {
        // 	VALUE str;
        // 	char *c;
        //
        // 	c = rb_class2name(CLASS_OF(obj));
        // 	if (rb_inspecting_p(obj)) {
        // 	    str = rb_str_new(0, strlen(c)+10+16+1); /* 10:tags 16:addr 1:eos */
        // 	    sprintf(RSTRING(str)->ptr, "#<%s:0x%lx ...>", c, obj);
        // 	    RSTRING(str)->len = strlen(RSTRING(str)->ptr);
        // 	    return str;
        // 	}
        // 	str = rb_str_new(0, strlen(c)+6+16+1); /* 6:tags 16:addr 1:eos */
        // 	sprintf(RSTRING(str)->ptr, "-<%s:0x%lx ", c, obj);
        // 	RSTRING(str)->len = strlen(RSTRING(str)->ptr);
        // 	return rb_protect_inspect(inspect_obj, obj, str);
        //     }
        //     return rb_funcall(obj, rb_intern("to_s"), 0, 0);
        // }
        return (RubyString) funcall(getRuby().intern("to_s"));
        //return null;
    }

    /** rb_obj_is_instance_of
     *
     */
    public RubyBoolean m_instance_of(RubyModule rbModule) {
        return RubyBoolean.m_newBoolean(getRuby(), m_type() == rbModule);
    }

    /** rb_obj_is_kind_of
     *
     */
    public RubyBoolean m_kind_of(RubyModule rbModule) {
        RubyModule rbClass = getRubyClass();

        while (rbClass != null) {
            if (rbClass == rbModule || rbClass.getMethods() == rbModule.getMethods()) {
                return getRuby().getTrue();
            }
            rbClass = rbClass.getSuperClass();
        }

        return getRuby().getFalse();
    }

    /** rb_obj_methods
     *
     */
    public RubyArray m_methods() {
        // return getRubyClass().m_instance_methods(getRuby().getTrue());
        return null;
    }

    /** rb_obj_protected_methods
     *
     */
    public RubyArray m_protected_methods() {
        // return getRubyClass().m_protected_instance_methods(getRuby().getTrue());
        return null;
    }

    /** rb_obj_private_methods
     *
     */
    public RubyArray m_private_methods() {
        // return getRubyClass().m_private_instance_methods(getRuby().getTrue());

        return null;
    }

    /** rb_obj_singleton_methods
     *
     */
    public RubyArray m_singleton_methods() {
        RubyArray ary = RubyArray.m_newArray(getRuby());
        RubyModule rbClass = getRubyClass();

        while (rbClass != null && rbClass.isSingleton()) {
            rbClass.getMethods().foreach(new RubyMapMethod() {
                public int execute(Object key, Object value, Object arg) {
                    return CONTINUE;
                }
            }, ary);
            rbClass = rbClass.getSuperClass();
        }
        ary.m_compact_bang();
        return ary;
    }

    public RubyObject m_method(RubyObject symbol) {
        return getRubyClass().newMethod(
            this,
            symbol.toId(),
            getRuby().getClasses().getMethodClass());
    }

    public RubyString m_to_s() {
        String cname = getRubyClass().toName();

        RubyString str = RubyString.m_newString(getRuby(), "");
        /* 6:tags 16:addr 1:eos */
        str.setValue("#<" + cname + ":0x" + Integer.toHexString(hashCode()) + ">");
        if (isTaint()) {
            str.setTaint(true);
        }

        return str;
    }

    public RubyObject instance_eval(RubyObject[] args) {
        return specificEval(getSingletonClass(), args);
    }
}