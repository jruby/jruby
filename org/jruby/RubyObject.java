/*
 * RubyObject.java - No description
 * Created on 04. Juli 2001, 22:53
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina, Chad Fowler
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Chad Fowler <chadfowler@chadfowler.com>
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

import org.ablaf.ast.*;

import org.jruby.evaluator.*;
import org.jruby.exceptions.*;
import org.jruby.internal.runtime.methods.*;
import org.jruby.ast.*;
import org.jruby.runtime.*;
import org.jruby.runtime.methods.*;
import org.jruby.util.*;
import org.jruby.marshal.*;

/**
 *
 * @author  jpetersen
 */
public class RubyObject implements Cloneable {

    // A reference to the JRuby runtime.
    protected transient Ruby ruby;

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

    public RubyObject(Ruby ruby, RubyClass rubyClass, boolean useObjectSpace) {
        this.ruby = ruby;
        this.rubyClass = rubyClass;
        this.frozen = false;
        this.taint = false;

        if (useObjectSpace) {
            ruby.objectSpace.add(this);
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
    
    /**
     * Create a new meta class.
     * 
     * This method is used by a lot of other methods.
     * 
     * @since Ruby 1.6.7
     */
    public RubyClass makeMetaClass(RubyClass type) {
        type = type.newSingletonClass();
        setRubyClass(type);
        type.attachSingletonClass(this);
        return type;
    }

    public Class getJavaClass() {
        return RubyObject.class;
    }

    /**
     * This method is just a wrapper around the Ruby "==" method,
     * provided so that RubyObjects can be used as keys in the Java
     * HashMap object underlying RubyHash.
     */
    public boolean equals(Object other) {
        return other == this || (other instanceof RubyObject) && funcall("==", (RubyObject) other).isTrue();
    }

    public String toString() {
        return ((RubyString)funcall("to_s")).getValue();
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

    public boolean respondsTo(String methodName) {
        return respond_to(new RubyObject[]{RubySymbol.newSymbol(getRuby(), methodName)}).isTrue();
    }

    public static void createObjectClass(RubyModule objectClass) {
        objectClass.defineMethod("==", CallbackFactory.getMethod(RubyObject.class, "equal", RubyObject.class));
        objectClass.defineMethod("=~", CallbackFactory.getFalseMethod(1));
        objectClass.defineMethod("clone", CallbackFactory.getMethod(RubyObject.class, "rbClone"));
        objectClass.defineMethod("dup", CallbackFactory.getMethod(RubyObject.class, "dup"));
        objectClass.defineMethod("eql?", CallbackFactory.getMethod(RubyObject.class, "equal", RubyObject.class));
        objectClass.defineMethod("respond_to?", CallbackFactory.getOptMethod(RubyObject.class, "respond_to"));
        objectClass.defineMethod("extend", CallbackFactory.getOptMethod(RubyObject.class, "extend"));
        objectClass.defineMethod("freeze", CallbackFactory.getMethod(RubyObject.class, "freeze"));
        objectClass.defineMethod("frozen?", CallbackFactory.getMethod(RubyObject.class, "frozen"));
        objectClass.defineMethod("id", CallbackFactory.getMethod(RubyObject.class, "id"));
        objectClass.defineMethod("hash", CallbackFactory.getMethod(RubyObject.class, "hash"));
        objectClass.defineMethod("__id__", CallbackFactory.getMethod(RubyObject.class, "id"));
        objectClass.defineMethod("inspect", CallbackFactory.getMethod(RubyObject.class, "inspect"));
        objectClass.defineMethod("instance_eval", CallbackFactory.getOptMethod(RubyObject.class, "instance_eval"));
        objectClass.defineMethod("instance_of?", CallbackFactory.getMethod(RubyObject.class, "instance_of", RubyModule.class));
        objectClass.defineMethod("instance_variables", CallbackFactory.getMethod(RubyObject.class, "instance_variables"));
        objectClass.defineMethod("is_a?", CallbackFactory.getMethod(RubyObject.class, "kind_of", RubyModule.class));
        objectClass.defineMethod("kind_of?", CallbackFactory.getMethod(RubyObject.class, "kind_of", RubyModule.class));
        objectClass.defineMethod("method", CallbackFactory.getMethod(RubyObject.class, "method", RubyObject.class));
        objectClass.defineMethod("methods", CallbackFactory.getMethod(RubyObject.class, "methods"));
        objectClass.defineMethod("nil?", CallbackFactory.getFalseMethod(0));
        objectClass.defineMethod("private_methods", CallbackFactory.getMethod(RubyObject.class, "private_methods"));
        objectClass.defineMethod("protected_methods", CallbackFactory.getMethod(RubyObject.class, "protected_methods"));
        objectClass.defineMethod("public_methods", CallbackFactory.getMethod(RubyObject.class, "methods"));
        objectClass.defineMethod("send", CallbackFactory.getOptMethod(RubyObject.class, "send", RubyObject.class));
        objectClass.defineMethod("__send__", CallbackFactory.getOptMethod(RubyObject.class, "send", RubyObject.class));
        objectClass.defineMethod("taint", CallbackFactory.getMethod(RubyObject.class, "taint"));
        objectClass.defineMethod("tainted?", CallbackFactory.getMethod(RubyObject.class, "tainted"));
        objectClass.defineMethod("to_a", CallbackFactory.getMethod(RubyObject.class, "to_a"));
        objectClass.defineMethod("to_s", CallbackFactory.getMethod(RubyObject.class, "to_s"));
        objectClass.defineMethod("type", CallbackFactory.getMethod(RubyObject.class, "type"));
        objectClass.defineMethod("untaint", CallbackFactory.getMethod(RubyObject.class, "untaint"));

        objectClass.defineAlias("===", "==");
        objectClass.defineAlias("class", "type");
        objectClass.defineAlias("equal?", "==");

        Ruby ruby = objectClass.getRuby();

        ruby.defineGlobalFunction("method_missing", CallbackFactory.getOptMethod(RubyObject.class, "method_missing", RubyObject.class));
    }

    // Some helper functions:

    protected int argCount(RubyObject[] args, int min, int max) {
        int len = args.length;
        if (len < min || (max > -1 && len > max)) {
            throw new ArgumentError(getRuby(), "Wrong # of arguments for method. " + args.length + " is not in Range " + min + ".." + max);
        }
        return len;
    }

    public boolean isKindOf(RubyModule type) {
        RubyClass currType = getRubyClass();
        while (currType != null) {
            if (currType == type || currType.getMethods().keySet().containsAll(type.getMethods().keySet())) {
                return true;
            }
            currType = currType.getSuperClass();
        }
        return false;
    }

    /** SPECIAL_SINGLETON(x,c)
     *
     */
    private RubyClass getNilSingletonClass() {
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
        if (isNil()) {
            return getNilSingletonClass();
        }

        RubyClass type = null;
        if (getRubyClass().isSingleton()) {
            type = getRubyClass();
        } else {
            type = makeMetaClass(getRubyClass());
        }
        type.setTaint(isTaint());
        type.setFrozen(isFrozen());

        return type;
    }

    /** rb_define_singleton_method
     *
     */
    public void defineSingletonMethod(String name, Callback method) {
        getSingletonClass().defineMethod(name, method);
    }

    /** CLONESETUP
     *
     */
    protected void setupClone(RubyObject obj) {
        setRubyClass(obj.getRubyClass().getSingletonClassClone());
        getRubyClass().attachSingletonClass(this);
		frozen = obj.frozen;
		taint = obj.taint;
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
    public RubyObject funcall(String name, RubyObject[] args) {
        return getRubyClass().call(this, name, args, 1);
    }

    public RubyObject funcall(String name) {
        return funcall(name, new RubyObject[0]);
    }

    /** rb_funcall3
     *
     */
    public RubyObject funcall3(String name, RubyObject[] args) {
        return getRubyClass().call(this, name, args, 0);
    }

    /** rb_funcall
     *
     */
    public RubyObject funcall(String name, RubyObject arg) {
        return funcall(name, new RubyObject[] { arg });
    }

    /** rb_iv_get / rb_ivar_get
     *
     */
    public RubyObject getInstanceVar(String name) {
        if (getInstanceVariables() != null) {
            RubyObject value = (RubyObject) getInstanceVariables().get(name);
            if (value != null) {
                return value;
            }
        }
        // todo: add warn if verbose
        return getRuby().getNil();
    }

    /** rb_iv_set / rb_ivar_set
     *
     */
    public RubyObject setInstanceVar(String name, RubyObject value) {
        if (isTaint() && getRuby().getSafeLevel() >= 4) {
            throw new RubySecurityException(getRuby(), "Insecure: can't modify instance variable");
        }
        if (isFrozen()) {
            throw new RubyFrozenException(getRuby(), "");
        }
        if (getInstanceVariables() == null) {
            setInstanceVariables(new RubyHashMap());
        }
        getInstanceVariables().put(name, value);

        return value;
    }

    public boolean isInstanceVarDefined(String name) {
        if (getInstanceVariables() != null) {
            if (getInstanceVariables().get(name) != null) {
                return true;
            }
        }
        return false;
    }

    /** rb_cvar_singleton
     * 
     *@deprecated  since Ruby 1.6.7
     */
    public RubyModule getClassVarSingleton() {
        return getRubyClass();
    }

    /** rb_eval
     *
     */
    public RubyObject eval(INode n) {
        return n == null ? getRuby().getNil() : 
            EvaluateVisitor.createVisitor(this).eval(n);
    }

    public final void callInit(final RubyObject[] args) {
        ruby.getIterStack().push(ruby.isBlockGiven() ? Iter.ITER_PRE : Iter.ITER_NOT);
        funcall("initialize", args);
        ruby.getIterStack().pop();
    }

    public void extendObject(RubyModule module) {
        getSingletonClass().includeModule(module);
    }

    /** rb_to_id
     *
     */
    public String toId() {
        throw new TypeError(getRuby(), inspect().getValue() + " is not a symbol");
    }

    /** Converts this object to type 'targetType' using 'convertMethod' method.
     * 
     * MRI: convert_type
     * 
     * @since Ruby 1.6.7.
     * @fixme error handling
     */
    public RubyObject convertToType(String targetType, String convertMethod, boolean raise) {
        if (! respondsTo(convertMethod)) {
            if (raise) {
                throw new TypeError(ruby, "Failed to convert " + getRubyClass().toName() + " into " + targetType + ".");
                // FIXME nil, true and false instead of NilClass, TrueClass, FalseClass;
            } else {
                return ruby.getNil();
            }
        }
        return funcall(convertMethod);
    }

    /** rb_convert_type
     *
     */
    public RubyObject convertType(Class type, String targetType, String convertMethod) {
        if (type.isAssignableFrom(getClass())) {
            return this;
        }

        RubyObject result = convertToType(targetType, convertMethod, true);

        if (!type.isAssignableFrom(result.getClass())) {
            throw new TypeError(ruby, getRubyClass().toName() + "#" + convertMethod + " should return " + targetType + ".");
        }

        return result;
    }

    public void checkSafeString() {
        if (ruby.getSafeLevel() > 0 && isTaint()) {
            if (ruby.getActFrame().getLastFunc() != null) {
                throw new RubySecurityException(ruby, "Insecure operation - " + ruby.getActFrame().getLastFunc());
            } else {
                throw new RubySecurityException(ruby, "Insecure operation: -r");
            }
        }
        getRuby().secure(4);
        if (!(this instanceof RubyString)) {
            throw new TypeError(getRuby(), "wrong argument type " + getRubyClass().toName() + " (expected String)");
        }
    }

    /** specific_eval
     * 
     */
    public RubyObject specificEval(RubyModule mod, RubyObject[] args) {
        if (getRuby().isBlockGiven()) {
            if (args.length > 0) {
                throw new ArgumentError(getRuby(), "wrong # of arguments (" + args.length + " for 0)");
            }
            return yieldUnder(mod);
        } else {
            if (args.length == 0) {
                throw new ArgumentError(getRuby(), "block not supplied");
            } else if (args.length > 3) {
                String lastFuncName = ruby.getActFrame().getLastFunc();
                throw new ArgumentError(getRuby(), "wrong # of arguments: " + lastFuncName + "(src) or " + lastFuncName + "{..}");
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
            
            public int getArity() {
                return -1;
            }
        }, new RubyObject[] { this, src, file, line });
    }

    public RubyObject yieldUnder(RubyModule under) {
        return under.executeUnder(new Callback() {
            public RubyObject execute(RubyObject self, RubyObject[] args, Ruby ruby) {
                // if () {             
                Block oldBlock = ruby.getBlock().getAct().cloneBlock();

                /* copy the block to avoid modifying global data. */
                ruby.getBlock().getAct().getFrame().setNamespace(ruby.getActFrame().getNamespace());
                RubyObject result = null;
                try {
                    result = ruby.yield0(args[0], args[0], ruby.getRubyClass(), false);
                } finally {
                    ruby.getBlock().setAct(oldBlock);
                }
                return result;
                // }
                /* static block, no need to restore */
                // ruby.getBlock().frame.setNamespace(ruby.getRubyFrame().getNamespace());
                // return ruby.yield0(args[0], args[0], ruby.getRubyClass(), false);
            }
            
            public int getArity() {
                return -1;
            }
        }, new RubyObject[] { this });
    }


	/**@fixme*/
    public RubyObject eval(RubyObject src, RubyObject scope, String file, int line) {
        String fileSave = ruby.getSourceFile();
        int lineSave = ruby.getSourceLine();
        Iter iter = ruby.getActFrame().getIter();
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
            if (ruby.getFrameStack().getPrevious() != null) {
                ruby.getActFrame().setIter(ruby.getFrameStack().getPrevious().getIter());
            }
        }
        getRuby().pushClass(ruby.getCBase());
        ruby.setInEval(ruby.getInEval() + 1);
        if (ruby.getRubyClass().isIncluded()) {
            ruby.setRubyClass(((RubyIncludedClass) ruby.getRubyClass()).getDelegate());
        }
        RubyObject result = getRuby().getNil();
        try {
            // result = ruby_errinfo;
            // ruby_errinfo = Qnil;
            
            // FIXME
            INode node = getRuby().compile(src.toString(), file, line);
            
            // if (ruby_nerrs > 0) {
            // 	compile_error(0);
            //}
            // if (!result.isNil()) {
            //	ruby_errinfo = result;
            //}
            result = eval(node);
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
                ruby.getActFrame().setIter(iter);
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
        if (isNil()) {
            return RubyBoolean.newBoolean(getRuby(), obj.isNil());
        }
        return RubyBoolean.newBoolean(getRuby(), this == obj);
    }

    /** rb_obj_respond_to
     *
     * "respond_to?"
     * @fixme ...Need to change this to support the optional boolean arg
     * And the associated access control on methods
     */
    public RubyBoolean respond_to(RubyObject[] args) {
        argCount(args, 1, 2);

        String name = args[0].toId();

        //Look in cache
        CacheEntry ent = getRuby().getMethodCache().getEntry(getRubyClass(), name);
        if (ent != null) {
            //Check to see if it's private and we're not including privates(return false)
            //otherwise return true
            return ruby.getTrue();
        }
        //Get from instance
        IMethod method = getRubyClass().searchMethod(name);
        if (method != null) {
            return ruby.getTrue();
        }
        return ruby.getFalse();
    }

    /** Return the internal id of an object.
     * 
     * <b>Warning:</b> In JRuby there is no guarantee that two objects have different ids.
     * 
     * <i>CRuby function: rb_obj_id</i>
     *
     */
    public RubyFixnum id() {
        return RubyFixnum.newFixnum(getRuby(), System.identityHashCode(this));
    }

    /**
     * Get the object's hash code.
     *
     * Classes that need other implementations of hash() should override the
     * Java method hashCode(). It is faster and more robust for Ruby to depend
     * on Java hash codes than the other way around.
     */
    public final RubyFixnum hash() {
        return RubyFixnum.newFixnum(getRuby(), hashCode());
    }

    /** rb_obj_type
     *
     */
    public RubyClass type() {
        return getRubyClass().getRealClass();
    }

    /** rb_obj_clone
     *
     */
    public RubyObject rbClone() {
        try {
            RubyObject clone = (RubyObject)clone();
            clone.setupClone(this);
            if (getInstanceVariables() != null) {
                clone.setInstanceVariables(getInstanceVariables().cloneRubyMap());
            }
            return clone;
        } catch (CloneNotSupportedException cnsExcptn) {
            // BUG
            throw new RubyBugException(cnsExcptn.getMessage());
        }
    }

    /** rb_obj_dup
     *
     */
    public RubyObject dup() {
        RubyObject dup = funcall("clone");
        if (!dup.getClass().equals(getClass())) {
            throw new TypeError(getRuby(), "duplicated object must be same type");
        }

        dup.setRubyClass(type());
//        dup.infectObject(this);  //Benoit done by clone
		dup.frozen = false;
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
        if (getRuby().getSafeLevel() >= 4 && isTaint()) {
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
        return (RubyString) funcall("to_s");
    }

    /** rb_obj_is_instance_of
     *
     */
    public RubyBoolean instance_of(RubyModule type) {
        return RubyBoolean.newBoolean(getRuby(), type() == type);
    }

    /**
     *
     */
    public RubyArray instance_variables() {
        ArrayList names = new ArrayList();
        Iterator iter = instanceVariables.keySet().iterator();
        while (iter.hasNext()) {
            String name = (String) iter.next();
            names.add(RubyString.newString(getRuby(), name));
        }
        return RubyArray.newArray(ruby, names);
    }

    /** rb_obj_is_kind_of
     *
     */
    public RubyBoolean kind_of(RubyModule type) {
        return RubyBoolean.newBoolean(ruby, isKindOf(type));
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
                    RubyString name = RubyString.newString(getRuby(), (String) key);
                    if ((((IMethod) value).getNoex() & (Constants.NOEX_PRIVATE | Constants.NOEX_PROTECTED)) == 0) {
                        if (((RubyArray) arg).includes(name).isFalse()) {
                            if (((IMethod) value) == null) {
                                ((RubyArray) arg).push(getRuby().getNil());
                            }
                            ((RubyArray) arg).push(name);
                        }
                    } else if (value instanceof EvaluateMethod && ((EvaluateMethod)value).getNode() instanceof ZSuperNode) {
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
	
	/**
	 *  @fixme: Check_Type?
	 **/
    public RubyObject extend(RubyObject args[]) {
        if (args.length == 0) {
            throw new ArgumentError(ruby, "wrong # of arguments");
        }
        // FIXME: Check_Type?
        for (int i = 0; i < args.length; i++) {
            args[i].funcall("extend_object", this);
        }
        return this;
    }

    public RubyObject method_missing(RubyObject symbol, RubyObject[] args) {
        throw new NameError(getRuby(),
                            "Undefined local variable or method '" + symbol.toId()
                            + "' for " + inspect().getValue());
    }

    public RubyObject send(RubyObject method, RubyObject[] args) {
        try {
            getRuby().getIterStack().push(getRuby().isBlockGiven() ? Iter.ITER_PRE : Iter.ITER_NOT);
            return getRubyClass().call(this, method.toId(), args, 1);
        } finally {
            getRuby().getIterStack().pop();
        }
    }

    public void marshalTo(MarshalStream output) throws java.io.IOException {
        output.write('o');
        RubySymbol classname = RubySymbol.newSymbol(ruby,  getRubyClass().getClassname());
        output.dumpObject(classname);

        if (getInstanceVariables() == null) {
            output.dumpInt(0);
        } else {
            output.dumpInt(getInstanceVariables().size());
            Iterator iter = getInstanceVariables().entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                String name = (String) entry.getKey();
                RubyObject value = (RubyObject) entry.getValue();

                output.dumpObject(RubySymbol.newSymbol(ruby, name));
                output.dumpObject(value);
            }
        }
    }
}
