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
import java.util.Map;

import org.jruby.ast.Node;
import org.jruby.evaluator.EvaluationState;
import org.jruby.exceptions.JumpException;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ICallable;
import org.jruby.runtime.Iter;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callback.Callback;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.util.IdUtil;
import org.jruby.util.PrintfFormat;
import org.jruby.util.collections.SinglyLinkedList;

/**
 *
 * @author  jpetersen
 */
public class RubyObject implements Cloneable, IRubyObject {
	
    // The class of this object
    private RubyClass metaClass;

    // The instance variables of this object.
    protected Map instanceVariables;

    // The two properties frozen and taint
    private boolean frozen;
    private boolean taint;

	public RubyObject(IRuby runtime, RubyClass metaClass) {
        this(runtime, metaClass, true);
    }

    public RubyObject(IRuby runtime, RubyClass metaClass, boolean useObjectSpace) {
        this.metaClass = metaClass;
        this.frozen = false;
        this.taint = false;

        // Do not store any immediate objects into objectspace.
        if (useObjectSpace && !isImmediate()) {
            runtime.getObjectSpace().add(this);
        }

        // FIXME are there objects who shouldn't be tainted?
        // (mri: OBJSETUP)
        taint |= runtime.getSafeLevel() >= 3;
    }
    
    /*
     *  Is object immediate (def: Fixnum, Symbol, true, false, nil?).
     */
    public boolean isImmediate() {
    	return false;
    }

    /**
     * Create a new meta class.
     *
     * @since Ruby 1.6.7
     */
    public MetaClass makeMetaClass(RubyClass type, SinglyLinkedList parentCRef) {
        MetaClass newMetaClass = type.newSingletonClass(parentCRef);
		
		if (!isNil()) {
			setMetaClass(newMetaClass);
		}
        newMetaClass.attachToObject(this);
        return newMetaClass;
    }

    public boolean singletonMethodsAllowed() {
        return true;
    }

    public Class getJavaClass() {
        return IRubyObject.class;
    }

    /**
     * This method is just a wrapper around the Ruby "==" method,
     * provided so that RubyObjects can be used as keys in the Java
     * HashMap object underlying RubyHash.
     */
    public boolean equals(Object other) {
        return other == this || other instanceof IRubyObject && callMethod("==", (IRubyObject) other).isTrue();
    }

    public String toString() {
        return ((RubyString) callMethod("to_s")).toString();
    }

    /** Getter for property ruby.
     * @return Value of property ruby.
     */
    public IRuby getRuntime() {
        return metaClass.getRuntime();
    }

    public IRubyObject removeInstanceVariable(String name) {
        return (IRubyObject) getInstanceVariables().remove(name);
    }

    public Map getInstanceVariables() {
    	// TODO: double checking may or may not be safe enough here
    	if (instanceVariables == null) {
	    	synchronized (this) {
	    		if (instanceVariables == null) {
	    			instanceVariables = new HashMap();
	    		}
	    	}
    	}
        return instanceVariables;
    }

    public void setInstanceVariables(Map instanceVariables) {
        this.instanceVariables = instanceVariables;
    }

    /**
     * if exist return the meta-class else return the type of the object.
     * 
     */
    public RubyClass getMetaClass() {
    	// TODO: Can we assert MetaClass on metaClass here?  This should simplify some callers
        if (isNil()) {
            return getRuntime().getClass("NilClass");
        }
        return metaClass;
    }

    public void setMetaClass(RubyClass metaClass) {
        this.metaClass = metaClass;
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

    /** rb_frozen_class_p
    *
    */
   protected void testFrozen(String message) {
       if (isFrozen()) {
           throw getRuntime().newFrozenError(message);
       }
   }

   protected void checkFrozen() {
       testFrozen("can't modify frozen " + getMetaClass().getName());
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

    public boolean respondsTo(String name) {
        return getMetaClass().isMethodBound(name, false);
    }

    // Some helper functions:

    public int checkArgumentCount(IRubyObject[] args, int min, int max) {
        if (args.length < min) {
            throw getRuntime().newArgumentError("wrong number of arguments (" + args.length + " for " + min + ")");
        }
        if (max > -1 && args.length > max) {
            throw getRuntime().newArgumentError("wrong number of arguments (" + args.length + " for " + max + ")");
        }
        return args.length;
    }

    public boolean isKindOf(RubyModule type) {
        return getMetaClass().ancestors().includes(type);
    }

    /** rb_singleton_class
     *
     */
    public MetaClass getSingletonClass() {
        RubyClass type = getMetaClass();
        if (!type.isSingleton()) { 
            type = makeMetaClass(type, type.getCRef());
        }

        assert type instanceof MetaClass; 

		if (!isNil()) {
			type.setTaint(isTaint());
			type.setFrozen(isFrozen());
		}

        return (MetaClass)type;
    }

    /** rb_define_singleton_method
     *
     */
    public void defineSingletonMethod(String name, Callback method) {
        getSingletonClass().defineMethod(name, method);
    }

    /* rb_init_ccopy */
    public void initCopy(IRubyObject original) {
        assert original != null;
        assert !isFrozen() : "frozen object (" + getMetaClass().getName() + ") allocated";

        setInstanceVariables(new HashMap(original.getInstanceVariables()));

        callMethod("initialize_copy", original);        
    }

    /** OBJ_INFECT
     *
     */
    public IRubyObject infectBy(IRubyObject obj) {
        setTaint(isTaint() || obj.isTaint());
        
        return this;
    }

    /**
     * 
     */
    public IRubyObject callMethod(String name, IRubyObject[] args) {
        return callMethod(getMetaClass(), name, args, CallType.FUNCTIONAL);
    }

    /**
     * 
     */
    public IRubyObject callMethod(String name, IRubyObject[] args,
            CallType callType) {
        return callMethod(getMetaClass(), name, args, callType);
    }

    /**
     * 
     */
    public IRubyObject callMethod(RubyModule context, String name, IRubyObject[] args, 
            CallType callType) {
        assert args != null;
        RubyModule implementer = null;
        ICallable method = null;
        
        method = context.searchMethod(name);
        
        if (method.needsImplementer()) {
            // modules are included with a shim class; we must find that shim to handle super() appropriately
            implementer = context.findImplementer(name);
        } else {
            // classes are directly in the hierarchy, so no special logic is necessary for implementer
            implementer = method.getImplementationClass();
        }

        if (method.isUndefined() ||
            !(name.equals("method_missing") ||
              method.isCallableFrom(getRuntime().getCurrentContext().getCurrentFrame().getSelf(), callType))) {
            if (callType == CallType.SUPER) {
                throw getRuntime().newNameError("super: no superclass method '" + name + "'");
            }

            // store call information so method_missing impl can use it
            getRuntime().getCurrentContext().setLastCallStatus(method.getVisibility(), callType);

            if (name.equals("method_missing")) {
                return RubyKernel.method_missing(this, args);
            }

            IRubyObject[] newArgs = new IRubyObject[args.length + 1];
            System.arraycopy(args, 0, newArgs, 1, args.length);
            newArgs[0] = RubySymbol.newSymbol(getRuntime(), name);

            return callMethod("method_missing", newArgs);
        }
        
        //System.out.println("method " + name + " found in " + (implementer.isIncluded()?"included module ":"module or class ") + implementer.getName());

        String originalName = method.getOriginalName();
        if (originalName != null) {
            name = originalName;
        }

        IRubyObject result = method.call(getRuntime(), this, implementer, name, args, false);
        
        return result;
    }

    public IRubyObject callMethod(String name) {
        return callMethod(name, IRubyObject.NULL_ARRAY);
    }

    /**
     * rb_funcall
     * 
     */
    public IRubyObject callMethod(String name, IRubyObject arg) {
        return callMethod(name, new IRubyObject[] { arg });
    }
    
    public IRubyObject instance_variable_get(IRubyObject var) {
    	String varName = var.asSymbol();
    	
    	if (!varName.startsWith("@")) {
    		throw getRuntime().newNameError("`" + varName + "' is not allowable as an instance variable name");
    	}
    	
    	IRubyObject variable = getInstanceVariable(varName); 
    	
    	// Pickaxe v2 says no var should show NameError, but ruby only sends back nil..
    	return variable == null ? getRuntime().getNil() : variable; 
    }

    public IRubyObject getInstanceVariable(String name) {
        return (IRubyObject) getInstanceVariables().get(name);
    }
    
    public IRubyObject instance_variable_set(IRubyObject var, IRubyObject value) {
    	String varName = var.asSymbol();
    	
    	if (!varName.startsWith("@")) {
    		throw getRuntime().newNameError("`" + varName + "' is not allowable as an instance variable name");
    	}
    	
    	return setInstanceVariable(var.asSymbol(), value);
    }

    public IRubyObject setInstanceVariable(String name, IRubyObject value,
            String taintError, String freezeError) {
        if (isTaint() && getRuntime().getSafeLevel() >= 4) {
            throw getRuntime().newSecurityError(taintError);
        }
        testFrozen(freezeError);

        getInstanceVariables().put(name, value);

        return value;
    }
    
    /** rb_iv_set / rb_ivar_set
     *
     */
    public IRubyObject setInstanceVariable(String name, IRubyObject value) {
        return setInstanceVariable(name, value, 
                "Insecure: can't modify instance variable", "");
    }

    public Iterator instanceVariableNames() {
        return getInstanceVariables().keySet().iterator();
    }

    /** rb_eval
     *
     */
    public IRubyObject eval(Node n) {
        //return new EvaluationState(getRuntime(), this).begin(n);
        // need to continue evaluation with a new self, so save the old one (should be a stack?)
        EvaluationState state = getRuntime().getCurrentContext().getCurrentFrame().getEvalState();
        IRubyObject oldSelf = state.getSelf();
        state.setSelf(this);
        try {
            return state.begin(n);
        } finally {
            state.setSelf(oldSelf);
        }
    }

    public void callInit(IRubyObject[] args) {
        getRuntime().getCurrentContext().pushIter(getRuntime().getCurrentContext().isBlockGiven() ? Iter.ITER_PRE : Iter.ITER_NOT);
        try {
            callMethod("initialize", args);
        } finally {
            getRuntime().getCurrentContext().popIter();
        }
    }

    public void extendObject(RubyModule module) {
        getSingletonClass().includeModule(module);
    }

    /** rb_to_id
     *
     */
    public String asSymbol() {
        throw getRuntime().newTypeError(inspect().toString() + " is not a symbol");
    }

    /** 
     * Converts this object to type 'targetType' using 'convertMethod' method.
     * 
     * @see IRubyObject.convertToType
	 */
    public IRubyObject convertToType(String targetType, String convertMethod, boolean raise) {
        // No need to convert something already of the correct type.
        // XXXEnebo - Could this pass actual class reference instead of String?
        if (targetType.equals(getMetaClass().getName())) {
            return this;
        }
        
        if (!respondsTo(convertMethod)) {
            if (raise) {
                throw getRuntime().newTypeError(
                    "cannot convert " + getMetaClass().getName() + " into " + targetType);
                // FIXME nil, true and false instead of NilClass, TrueClass, FalseClass;
            } 

            return getRuntime().getNil();
        }
        return callMethod(convertMethod);
    }

    public RubyArray convertToArray() {
        return (RubyArray) convertToType("Array", "to_ary", true);
    }

    public RubyFloat convertToFloat() {
        return (RubyFloat) convertToType("Float", "to_f", true);
    }
    
    public RubyInteger convertToInteger() {
        return (RubyInteger) convertToType("Integer", "to_int", true);
    }

    public RubyString convertToString() {
        return (RubyString) convertToType("String", "to_str", true);
    }

    /** rb_convert_type
     *
     */
    public IRubyObject convertType(Class type, String targetType, String convertMethod) {
        if (type.isAssignableFrom(getClass())) {
            return this;
        }

        IRubyObject result = convertToType(targetType, convertMethod, true);

        if (!type.isAssignableFrom(result.getClass())) {
            throw getRuntime().newTypeError(
                getMetaClass().getName() + "#" + convertMethod + " should return " + targetType + ".");
        }

        return result;
    }

    public void checkSafeString() {
        if (getRuntime().getSafeLevel() > 0 && isTaint()) {
            if (getRuntime().getCurrentContext().getCurrentFrame().getLastFunc() != null) {
                throw getRuntime().newSecurityError("Insecure operation - " + getRuntime().getCurrentContext().getCurrentFrame().getLastFunc());
            }
            throw getRuntime().newSecurityError("Insecure operation: -r");
        }
        getRuntime().secure(4);
        if (!(this instanceof RubyString)) {
            throw getRuntime().newTypeError(
                "wrong argument type " + getMetaClass().getName() + " (expected String)");
        }
    }

    /** specific_eval
     *
     */
    public IRubyObject specificEval(RubyModule mod, IRubyObject[] args) {
        if (getRuntime().getCurrentContext().isBlockGiven()) {
            if (args.length > 0) {
                throw getRuntime().newArgumentError(args.length, 0);
            }
            return yieldUnder(mod);
        }
		if (args.length == 0) {
		    throw getRuntime().newArgumentError("block not supplied");
		} else if (args.length > 3) {
		    String lastFuncName = getRuntime().getCurrentContext().getCurrentFrame().getLastFunc();
		    throw getRuntime().newArgumentError(
		        "wrong # of arguments: " + lastFuncName + "(src) or " + lastFuncName + "{..}");
		}
		/*
		if (ruby.getSecurityLevel() >= 4) {
			Check_Type(argv[0], T_STRING);
		} else {
			Check_SafeStr(argv[0]);
		}
		*/
		IRubyObject file = args.length > 1 ? args[1] : getRuntime().newString("(eval)");
		IRubyObject line = args.length > 2 ? args[2] : RubyFixnum.one(getRuntime());

		Visibility savedVisibility = getRuntime().getCurrentContext().getCurrentVisibility();
        getRuntime().getCurrentContext().setCurrentVisibility(Visibility.PUBLIC);
		try {
		    return evalUnder(mod, args[0], file, line);
		} finally {
            getRuntime().getCurrentContext().setCurrentVisibility(savedVisibility);
		}
    }

    public IRubyObject evalUnder(RubyModule under, IRubyObject src, IRubyObject file, IRubyObject line) {
        /*
        if (ruby_safe_level >= 4) {
        	Check_Type(src, T_STRING);
        } else {
        	Check_SafeStr(src);
        	}
        */
        return under.executeUnder(new Callback() {
            public IRubyObject execute(IRubyObject self, IRubyObject[] args) {
                IRubyObject source = args[1];
                IRubyObject filename = args[2];
                IRubyObject lineNumber = args[3];
                return args[0].eval(source,
                                  self.getRuntime().getNil(),
                                  ((RubyString) filename).toString(),
                                  RubyNumeric.fix2int(lineNumber));
            }

            public Arity getArity() {
                return Arity.optional();
            }
        }, new IRubyObject[] { this, src, file, line });
    }

    private IRubyObject yieldUnder(RubyModule under) {
        return under.executeUnder(new Callback() {
            public IRubyObject execute(IRubyObject self, IRubyObject[] args) {
                ThreadContext context = getRuntime().getCurrentContext();

                Block block = (Block) context.getCurrentBlock();
                Visibility savedVisibility = block.getVisibility();

                block.setVisibility(Visibility.PUBLIC);
                try {
                    IRubyObject valueInYield = args[0];
                    IRubyObject selfInYield = args[0];
                    return context.yield(valueInYield, selfInYield, context.getRubyClass(), false, false);
                    //TODO: Should next and return also catch here?
                } catch (JumpException je) {
                	if (je.getJumpType() == JumpException.JumpType.BreakJump) {
                		IRubyObject breakValue = (IRubyObject)je.getPrimaryData();
                    
                		return breakValue == null ? getRuntime().getNil() : breakValue;
                	} else {
                		throw je;
                	}
                } finally {
                    block.setVisibility(savedVisibility);
                }
            }

            public Arity getArity() {
                return Arity.optional();
            }
        }, new IRubyObject[] { this });
    }

    public IRubyObject eval(IRubyObject src, IRubyObject scope, String file, int line) {
        ThreadContext threadContext = getRuntime().getCurrentContext();
        
        ISourcePosition savedPosition = null;
        Iter iter = null;
        IRubyObject oldSelf = null;
        EvaluationState state = null;
        
        IRubyObject result = getRuntime().getNil();
        
        try {
            savedPosition = threadContext.getPosition();
            iter = threadContext.getCurrentFrame().getIter();
            
            // make sure we have a file
            if (file == null) {
                file = threadContext.getSourceFile();
            }
            
            if (scope instanceof RubyProc) {
            	scope = ((RubyProc) scope).binding();
            }
            IRubyObject newSelf = null;
            if (scope.isNil() || !(scope instanceof RubyBinding)) {
                if (threadContext.getPreviousFrame() != null) {
                    threadContext.getCurrentFrame().setIter(threadContext.getPreviousFrame().getIter());
                }
                newSelf = this;
            } else {
                // Binding provided for scope, use it
                threadContext.preEvalWithBinding((RubyBinding)scope);
                
                newSelf = threadContext.getCurrentFrame().getSelf();
            }

            state = getRuntime().getCurrentContext().getCurrentFrame().getEvalState();
            oldSelf = state.getSelf();
            state.setSelf(newSelf);
            
            Node node = getRuntime().parse(src.toString(), file);
            
            result = state.begin(node);
        } finally {
            // return the eval state to its original self
            state.setSelf(oldSelf);
            
            if (scope.isNil() || !(scope instanceof RubyBinding)) {
//              FIXME: this is broken for Proc, see above
                threadContext.getCurrentFrame().setIter(iter);
            } else if (scope instanceof RubyBinding) {
                threadContext.postEvalWithBinding();
            }
            
            // restore position
            threadContext.setPosition(savedPosition);
        }
        return result;
    }

    // Methods of the Object class (rb_obj_*):

    /** rb_obj_equal
     *
     */
    public IRubyObject equal(IRubyObject obj) {
        if (isNil()) {
            return getRuntime().newBoolean(obj.isNil());
        }
        return getRuntime().newBoolean(this == obj);
    }
    
	public IRubyObject same(IRubyObject other) {
		return this == other ? getRuntime().getTrue() : getRuntime().getFalse();
	}
	
	public IRubyObject initialize_copy(IRubyObject original) {
	    if (this != original) {
	        checkFrozen();
	        if (!getClass().equals(original.getClass())) {
	            throw getRuntime().newTypeError("initialize_copy should take same class object");
	        }
	    }

	    return this;
	}

    /**
     * respond_to?( aSymbol, includePriv=false ) -> true or false
     *
     * Returns true if this object responds to the given method. Private
     * methods are included in the search only if the optional second
     * parameter evaluates to true.
     *
     * @return true if this responds to the given method
     */
    public RubyBoolean respond_to(IRubyObject[] args) {
        checkArgumentCount(args, 1, 2);

        String name = args[0].asSymbol();
        boolean includePrivate = args.length > 1 ? args[1].isTrue() : false;

        return getRuntime().newBoolean(getMetaClass().isMethodBound(name, !includePrivate));
    }

    /** Return the internal id of an object.
     *
     * <b>Warning:</b> In JRuby there is no guarantee that two objects have different ids.
     *
     * <i>CRuby function: rb_obj_id</i>
     *
     */
    public RubyFixnum id() {
        return getRuntime().newFixnum(System.identityHashCode(this));
    }

    public RubyFixnum hash() {
        return getRuntime().newFixnum(System.identityHashCode(this));
    }
    
    public final int hashCode() {
    	return RubyNumeric.fix2int(callMethod("hash"));
    }

    /** rb_obj_type
     *
     */
    public RubyClass type() {
        return getMetaClass().getRealClass();
    }

    public RubyClass type_deprecated() {
        getRuntime().getWarnings().warn("Object#type is deprecated; use Object#class");
        return type();
    }

    /** rb_obj_clone
     *
     */
    public IRubyObject rbClone() {
        IRubyObject clone = doClone();
        clone.setMetaClass(getMetaClass().getSingletonClassClone());
        clone.setTaint(this.isTaint());
        clone.initCopy(this);
        clone.setFrozen(isFrozen());
        return clone;
    }
    
    // Hack: allow RubyModule and RubyClass to override the allocation and return the the correct Java instance
    // Cloning a class object doesn't work otherwise and I don't really understand why --sma
    protected IRubyObject doClone() {
    	return getMetaClass().getRealClass().allocate();
    }
    
    public IRubyObject display(IRubyObject[] args) {
        IRubyObject port = args.length == 0
            ? getRuntime().getGlobalVariables().get("$>") : args[0];
        
        port.callMethod("write", this);

        return getRuntime().getNil();
    }
    
    /** rb_obj_dup
     *
     */
    public IRubyObject dup() {
        IRubyObject dup = callMethod("clone");
        if (!dup.getClass().equals(getClass())) {
            throw getRuntime().newTypeError("duplicated object must be same type");
        }

        dup.setMetaClass(type());
        dup.setFrozen(false);
        return dup;
    }

    /** rb_obj_tainted
     *
     */
    public RubyBoolean tainted() {
        return getRuntime().newBoolean(isTaint());
    }

    /** rb_obj_taint
     *
     */
    public IRubyObject taint() {
        getRuntime().secure(4);
        if (!isTaint()) {
        	testFrozen("object");
            setTaint(true);
        }
        return this;
    }

    /** rb_obj_untaint
     *
     */
    public IRubyObject untaint() {
        getRuntime().secure(3);
        if (isTaint()) {
        	testFrozen("object");
            setTaint(false);
        }
        return this;
    }

    /** Freeze an object.
     *
     * rb_obj_freeze
     *
     */
    public IRubyObject freeze() {
        if (getRuntime().getSafeLevel() >= 4 && isTaint()) {
            throw getRuntime().newSecurityError("Insecure: can't freeze object");
        }
        setFrozen(true);
        return this;
    }

    /** rb_obj_frozen_p
     *
     */
    public RubyBoolean frozen() {
        return getRuntime().newBoolean(isFrozen());
    }

    /** rb_obj_inspect
     *
     */
    public IRubyObject inspect() {
        // TODO Review this and either remove the comment, or do it
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
        return callMethod("to_s");
    }

    /** rb_obj_is_instance_of
     *
     */
    public RubyBoolean instance_of(IRubyObject type) {
        return getRuntime().newBoolean(type() == type);
    }

    public RubyArray instance_variables() {
        ArrayList names = new ArrayList();
        Iterator iter = instanceVariableNames();
        while (iter.hasNext()) {
            String name = (String) iter.next();
            names.add(getRuntime().newString(name));
        }
        return getRuntime().newArray(names);
    }

    /** rb_obj_is_kind_of
     *
     */
    public RubyBoolean kind_of(IRubyObject type) {
        // TODO: Generalize this type-checking code into IRubyObject helper.
        if (!type.isKindOf(getRuntime().getClass("Module"))) {
            // TODO: newTypeError does not offer enough for ruby error string...
            throw getRuntime().newTypeError(type, getRuntime().getClass("Module"));
        }

        return getRuntime().newBoolean(isKindOf((RubyModule)type));
    }

    /** rb_obj_methods
     *
     */
    public IRubyObject methods(IRubyObject[] args) {
    	checkArgumentCount(args, 0, 1);
    	
    	if (args.length == 0) {
    		args = new IRubyObject[] { getRuntime().getTrue() };
    	}

        return getMetaClass().instance_methods(args);
    }
	
	public IRubyObject public_methods(IRubyObject[] args) {
        return getMetaClass().public_instance_methods(args);
	}

    /** rb_obj_protected_methods
     *
     */
    public IRubyObject protected_methods() {
        return getMetaClass().protected_instance_methods(new IRubyObject[] { getRuntime().getTrue()});
    }

    /** rb_obj_private_methods
     *
     */
    public IRubyObject private_methods() {
        return getMetaClass().private_instance_methods(new IRubyObject[] { getRuntime().getTrue()});
    }

    /** rb_obj_singleton_methods
     *
     */
    // TODO: This is almost RubyModule#instance_methods on the metaClass.  Perhaps refactor.
    public RubyArray singleton_methods() {
        RubyArray result = getRuntime().newArray();
        
        for (RubyClass type = getMetaClass(); type != null && type instanceof MetaClass; 
             type = type.getSuperClass()) { 
        	for (Iterator iter = type.getMethods().entrySet().iterator(); iter.hasNext(); ) {
                Map.Entry entry = (Map.Entry) iter.next();
                ICallable method = (ICallable) entry.getValue();

                // We do not want to capture cached methods
                if (method.getImplementationClass() != type) {
                	continue;
                }
                
                RubyString methodName = getRuntime().newString((String) entry.getKey());
                if (method.getVisibility().isPublic() && ! result.includes(methodName)) {
                    result.append(methodName);
                }
            }
        }

        return result;
    }

    public IRubyObject method(IRubyObject symbol) {
        return getMetaClass().newMethod(this, symbol.asSymbol(), true);
    }

    public RubyArray to_a() {
        return getRuntime().newArray(this);
    }

    protected IRubyObject anyToString() {
        String cname = getMetaClass().getRealClass().getName();
        /* 6:tags 16:addr 1:eos */
        RubyString str = getRuntime().newString("#<" + cname + ":0x" + Integer.toHexString(System.identityHashCode(this)) + ">");
        str.setTaint(isTaint());
        return str;
    }
    
    public IRubyObject to_s() {
    	return anyToString();
    }

    public IRubyObject instance_eval(IRubyObject[] args) {
        return specificEval(getSingletonClass(), args);
    }

    public IRubyObject extend(IRubyObject[] args) {
        checkArgumentCount(args, 1, -1);
        
        // Make sure all arguments are modules before calling the callbacks
        RubyClass module = getRuntime().getClass("Module");
        for (int i = 0; i < args.length; i++) {
            if (!args[i].isKindOf(module)) {
                throw getRuntime().newTypeError(args[i], module);
            }
        }
        
        for (int i = 0; i < args.length; i++) {
            args[i].callMethod("extend_object", this);
            args[i].callMethod("extended", this);
        }
        return this;
    }
    
    public IRubyObject inherited(IRubyObject arg) {
    	return getRuntime().getNil();
    }
    public IRubyObject initialize(IRubyObject[] args) {
    	return getRuntime().getNil();
    }

    public IRubyObject method_missing(IRubyObject[] args) {
        if (args.length == 0) {
            throw getRuntime().newArgumentError("no id given");
        }

        String name = args[0].asSymbol();
        String description = callMethod("inspect").toString();
        boolean noClass = description.length() > 0 && description.charAt(0) == '#';
        Visibility lastVis = getRuntime().getCurrentContext().getLastVisibility();
        CallType lastCallType = getRuntime().getCurrentContext().getLastCallType();
        String format = lastVis.errorMessageFormat(lastCallType, name);
        String msg = new PrintfFormat(format).sprintf(new Object[] { name, description, 
            noClass ? "" : ":", noClass ? "" : getType().getName()});

        if (lastCallType == CallType.VARIABLE) {
        	throw getRuntime().newNameError(msg);
        }
        throw getRuntime().newNoMethodError(msg);
    }

    /**
     * send( aSymbol  [, args  ]*   ) -> anObject
     *
     * Invokes the method identified by aSymbol, passing it any arguments
     * specified. You can use __send__ if the name send clashes with an
     * existing method in this object.
     *
     * <pre>
     * class Klass
     *   def hello(*args)
     *     "Hello " + args.join(' ')
     *   end
     * end
     *
     * k = Klass.new
     * k.send :hello, "gentle", "readers"
     * </pre>
     *
     * @return the result of invoking the method identified by aSymbol.
     */
    public IRubyObject send(IRubyObject[] args) {
        if (args.length < 1) {
            throw getRuntime().newArgumentError("no method name given");
        }
        String name = args[0].asSymbol();

        IRubyObject[] newArgs = new IRubyObject[args.length - 1];
        System.arraycopy(args, 1, newArgs, 0, newArgs.length);

        getRuntime().getCurrentContext().pushIter(getRuntime().getCurrentContext().isBlockGiven() ? Iter.ITER_PRE : Iter.ITER_NOT);
        try {
            return callMethod(name, newArgs, CallType.FUNCTIONAL);
        } finally {
            getRuntime().getCurrentContext().popIter();
        }
    }
    
    public IRubyObject nil_p() {
    	return getRuntime().getFalse();
    }
    
    public IRubyObject match(IRubyObject arg) {
    	return getRuntime().getFalse();
    }
    
   public IRubyObject remove_instance_variable(IRubyObject name) {
       String id = name.asSymbol();

       if (!IdUtil.isInstanceVariable(id)) {
           throw getRuntime().newNameError("wrong instance variable name " + id);
       }
       if (!isTaint() && getRuntime().getSafeLevel() >= 4) {
           throw getRuntime().newSecurityError("Insecure: can't remove instance variable");
       }
       testFrozen("class/module");

       IRubyObject variable = removeInstanceVariable(id); 
       if (variable != null) {
           return variable;
       }

       throw getRuntime().newNameError("instance variable " + id + " not defined");
   }

    public void marshalTo(MarshalStream output) throws java.io.IOException {
        output.write('o');
        RubySymbol classname = RubySymbol.newSymbol(getRuntime(), getMetaClass().getName());
        output.dumpObject(classname);

        output.dumpInt(getInstanceVariables().size());
        
        for (Iterator iter = instanceVariableNames(); iter.hasNext();) {
            String name = (String) iter.next();
            IRubyObject value = getInstanceVariable(name);

            // Between getting name and retrieving value the instance variable could have been
            // removed
            if (value != null) {
            	output.dumpObject(RubySymbol.newSymbol(getRuntime(), name));
            	output.dumpObject(value);
            }
        }
    }
   
    
    /**
     * @see org.jruby.runtime.builtin.IRubyObject#getType()
     */
    public RubyClass getType() {
        return type();
    }
}
