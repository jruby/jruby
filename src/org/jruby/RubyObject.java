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
 * Copyright (C) 2004-2006 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004-2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006 Ola Bini <ola.bini@ki.se>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
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

import org.jruby.ast.Node;
import org.jruby.evaluator.EvaluationState;
import org.jruby.exceptions.JumpException;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.DynamicMethod;
import org.jruby.runtime.Iter;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callback.Callback;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.util.IdUtil;
import org.jruby.util.PrintfFormat;
import org.jruby.util.collections.SinglyLinkedList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
        this(runtime, metaClass, runtime.isObjectSpaceEnabled());
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
    
    public static void puts(Object obj) {
        System.out.println(obj.toString());
    }

    /**
     * This method is just a wrapper around the Ruby "==" method,
     * provided so that RubyObjects can be used as keys in the Java
     * HashMap object underlying RubyHash.
     */
    public boolean equals(Object other) {
        return other == this || other instanceof IRubyObject && callMethod(getRuntime().getCurrentContext(), "==", (IRubyObject) other).isTrue();
    }

    public String toString() {
        return ((RubyString) callMethod(getRuntime().getCurrentContext(), "to_s")).toString();
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

    /**
     * Returns an unmodifiable snapshot of the current state of instance variables.
     * This method synchronizes access to avoid deadlocks.
     */
    public Map getInstanceVariablesSnapshot() {
        synchronized(getInstanceVariables()) {
            return Collections.unmodifiableMap(new HashMap(getInstanceVariables()));
        }
    }

    public Map getInstanceVariables() {
    	// TODO: double checking may or may not be safe enough here
    	if (instanceVariables == null) {
	    	synchronized (this) {
	    		if (instanceVariables == null) {
                            instanceVariables = Collections.synchronizedMap(new HashMap());
	    		}
	    	}
    	}
        return instanceVariables;
    }

    public void setInstanceVariables(Map instanceVariables) {
        this.instanceVariables = Collections.synchronizedMap(instanceVariables);
    }

    /**
     * if exist return the meta-class else return the type of the object.
     *
     */
    public RubyClass getMetaClass() {
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
        return getMetaClass().hasModuleInHierarchy(type);
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

    /** rb_define_singleton_method
     *
     */
    public void defineFastSingletonMethod(String name, Callback method) {
        getSingletonClass().defineFastMethod(name, method);
    }

    public void addSingletonMethod(String name, DynamicMethod method) {
        getSingletonClass().addMethod(name, method);
    }

    /* rb_init_ccopy */
    public void initCopy(IRubyObject original) {
        assert original != null;
        assert !isFrozen() : "frozen object (" + getMetaClass().getName() + ") allocated";

        setInstanceVariables(new HashMap(original.getInstanceVariables()));

        callMethod(getRuntime().getCurrentContext(), "initialize_copy", original);
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
    public IRubyObject callMethod(ThreadContext context, String name, IRubyObject[] args) {
        return callMethod(context, getMetaClass(), name, args, CallType.FUNCTIONAL);
    }

    /**
     *
     */
    public IRubyObject callMethod(ThreadContext context, String name,
            IRubyObject[] args, CallType callType) {
        return callMethod(context, getMetaClass(), name, args, callType);
    }

    /**
     *
     */
    public IRubyObject callMethod(ThreadContext context, RubyModule rubyclass, String name,
            IRubyObject[] args, CallType callType) {
        assert args != null;
        DynamicMethod method = null;

        method = rubyclass.searchMethod(name);

        if (method.isUndefined() ||
            !(name.equals("method_missing") ||
              method.isCallableFrom(context.getFrameSelf(), callType))) {
            if (callType == CallType.SUPER) {
                throw getRuntime().newNameError("super: no superclass method '" + name + "'", name);
            }

            // store call information so method_missing impl can use it
            context.setLastCallStatus(method.getVisibility(), callType);

            if (name.equals("method_missing")) {
                return RubyKernel.method_missing(this, args);
            }

            IRubyObject[] newArgs = new IRubyObject[args.length + 1];
            System.arraycopy(args, 0, newArgs, 1, args.length);
            newArgs[0] = RubySymbol.newSymbol(getRuntime(), name);

            return callMethod(context, "method_missing", newArgs);
        }

        RubyModule implementer = null;
        if (method.needsImplementer()) {
            // modules are included with a shim class; we must find that shim to handle super() appropriately
            implementer = rubyclass.findImplementer(method.getImplementationClass());
        } else {
            // classes are directly in the hierarchy, so no special logic is necessary for implementer
            implementer = method.getImplementationClass();
        }

        String originalName = method.getOriginalName();
        if (originalName != null) {
            name = originalName;
        }

        IRubyObject result = method.call(context, this, implementer, name, args, false);

        return result;
    }

    public IRubyObject callMethod(ThreadContext context, String name) {
        return callMethod(context, name, IRubyObject.NULL_ARRAY);
    }

    /**
     * rb_funcall
     *
     */
    public IRubyObject callMethod(ThreadContext context, String name, IRubyObject arg) {
        return callMethod(context, name, new IRubyObject[] { arg });
    }

    public IRubyObject instance_variable_get(IRubyObject var) {
    	String varName = var.asSymbol();

    	if (!varName.startsWith("@")) {
    		throw getRuntime().newNameError("`" + varName + "' is not allowable as an instance variable name", varName);
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
    		throw getRuntime().newNameError("`" + varName + "' is not allowable as an instance variable name", varName);
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
        return EvaluationState.eval(getRuntime().getCurrentContext(), n, this);
    }

    public void callInit(IRubyObject[] args) {
        ThreadContext tc = getRuntime().getCurrentContext();

        tc.setIfBlockAvailable();
        try {
            callMethod(getRuntime().getCurrentContext(), "initialize", args);
        } finally {
            tc.clearIfBlockAvailable();
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

    /*
     * @see org.jruby.runtime.builtin.IRubyObject#convertToTypeWithCheck(java.lang.String, java.lang.String)
     */
    public IRubyObject convertToTypeWithCheck(String targetType, String convertMethod) {
        if (targetType.equals(getMetaClass().getName())) {
            return this;
        }

        IRubyObject value = convertToType(targetType, convertMethod, false);
        if (value.isNil()) {
            return value;
        }

        if (!targetType.equals(value.getMetaClass().getName())) {
            throw getRuntime().newTypeError(value.getMetaClass().getName() + "#" + convertMethod +
                    "should return " + targetType);
        }

        return value;
    }

    /*
     * @see org.jruby.runtime.builtin.IRubyObject#convertToType(java.lang.String, java.lang.String, boolean)
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
                    "can't convert " + trueFalseNil(getMetaClass().getName()) + " into " + trueFalseNil(targetType));
            } 

            return getRuntime().getNil();
        }
        return callMethod(getRuntime().getCurrentContext(), convertMethod);
    }

    private String trueFalseNil(String v) {
        if("TrueClass".equals(v)) {
            return "true";
        } else if("FalseClass".equals(v)) {
            return "false";
        } else if("NilClass".equals(v)) {
            return "nil";
        }
        return v;
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
            ThreadContext tc = getRuntime().getCurrentContext();
            if (tc.getFrameLastFunc() != null) {
                throw getRuntime().newSecurityError("Insecure operation - " + tc.getFrameLastFunc());
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
        ThreadContext tc = getRuntime().getCurrentContext();

        if (tc.isBlockGiven()) {
            if (args.length > 0) {
                throw getRuntime().newArgumentError(args.length, 0);
            }
            return yieldUnder(mod);
        }
		if (args.length == 0) {
		    throw getRuntime().newArgumentError("block not supplied");
		} else if (args.length > 3) {
		    String lastFuncName = tc.getFrameLastFunc();
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
        
        // We just want the TypeError if the argument doesn't convert to a String (JRUBY-386)
        args[0].convertToString();
        
		IRubyObject file = args.length > 1 ? args[1] : getRuntime().newString("(eval)");
		IRubyObject line = args.length > 2 ? args[2] : RubyFixnum.one(getRuntime());

		Visibility savedVisibility = tc.getCurrentVisibility();
        tc.setCurrentVisibility(Visibility.PUBLIC);
		try {
		    return evalUnder(mod, args[0], file, line);
		} finally {
            tc.setCurrentVisibility(savedVisibility);
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
                // FIXME: lineNumber is not supported
                //IRubyObject lineNumber = args[3];

                return args[0].evalSimple(source.getRuntime().getCurrentContext(),
                                  source, ((RubyString) filename).toString());
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
                    return context.yieldCurrentBlock(valueInYield, selfInYield, context.getRubyClass(), false);
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

    /* (non-Javadoc)
     * @see org.jruby.runtime.builtin.IRubyObject#evalWithBinding(org.jruby.runtime.builtin.IRubyObject, org.jruby.runtime.builtin.IRubyObject, java.lang.String)
     */
    public IRubyObject evalWithBinding(ThreadContext context, IRubyObject src, IRubyObject scope, String file) {
        // both of these are ensured by the (very few) callers
        assert !scope.isNil();
        assert file != null;

        ThreadContext threadContext = getRuntime().getCurrentContext();

        ISourcePosition savedPosition = threadContext.getPosition();
        IRubyObject result = getRuntime().getNil();

        IRubyObject newSelf = null;

        if (!(scope instanceof RubyBinding)) {
            if (scope instanceof RubyProc) {
                scope = ((RubyProc) scope).binding();
            } else {
                // bomb out, it's not a binding or a proc
                throw getRuntime().newTypeError("wrong argument type " + scope.getMetaClass() + " (expected Proc/Binding)");
            }
        }

        Block blockOfBinding = ((RubyBinding)scope).getBlock();
        try {
            // Binding provided for scope, use it
            threadContext.preEvalWithBinding(blockOfBinding);
            newSelf = threadContext.getFrameSelf();

            result = EvaluationState.eval(threadContext, getRuntime().parse(src.toString(), file, blockOfBinding.getDynamicScope()), newSelf);
        } finally {
            threadContext.postEvalWithBinding(blockOfBinding);

            // restore position
            threadContext.setPosition(savedPosition);
        }
        return result;
    }

    /* (non-Javadoc)
     * @see org.jruby.runtime.builtin.IRubyObject#evalSimple(org.jruby.runtime.builtin.IRubyObject, java.lang.String)
     */
    public IRubyObject evalSimple(ThreadContext context, IRubyObject src, String file) {
        // this is ensured by the callers
        assert file != null;

        ThreadContext threadContext = getRuntime().getCurrentContext();

        ISourcePosition savedPosition = threadContext.getPosition();
        // no binding, just eval in "current" frame (caller's frame)
        Iter iter = threadContext.getFrameIter();
        IRubyObject result = getRuntime().getNil();

        try {
            // hack to avoid using previous frame if we're the first frame, since this eval is used to start execution too
            if (threadContext.getPreviousFrame() != null) {
                threadContext.setFrameIter(threadContext.getPreviousFrameIter());
            }

            result = EvaluationState.eval(threadContext, getRuntime().parse(src.toString(), file, threadContext.getCurrentScope()), this);
        } finally {
            // FIXME: this is broken for Proc, see above
            threadContext.setFrameIter(iter);

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
     * <i>CRuby function: rb_obj_id</i>
     *
     */
    public synchronized RubyFixnum id() {
        return getRuntime().newFixnum(getRuntime().getObjectSpace().idOf(this));
    }
    
    public RubyFixnum hash() {
        return getRuntime().newFixnum(System.identityHashCode(this));
    }

    public int hashCode() {
    	return (int) RubyNumeric.fix2long(callMethod(getRuntime().getCurrentContext(), "hash"));
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

        port.callMethod(getRuntime().getCurrentContext(), "write", this);

        return getRuntime().getNil();
    }

    /** rb_obj_dup
     *
     */
    public IRubyObject dup() {
        IRubyObject dup = callMethod(getRuntime().getCurrentContext(), "clone");
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
        if(getInstanceVariables().size() > 0) {
            StringBuffer part = new StringBuffer();
            String cname = getMetaClass().getRealClass().getName();
            part.append("#<").append(cname).append(":0x");
            part.append(Integer.toHexString(System.identityHashCode(this)));
            if(!getRuntime().registerInspecting(this)) {
                /* 6:tags 16:addr 1:eos */
                part.append(" ...>");
                return getRuntime().newString(part.toString());
            }
            try {
                String sep = "";
                Map iVars = getInstanceVariablesSnapshot();
                for (Iterator iter = iVars.keySet().iterator(); iter.hasNext();) {
                    String name = (String) iter.next();
                    if(name.startsWith("@")) {
                        part.append(" ");
                        part.append(sep);
                        part.append(name);
                        part.append("=");
                        part.append(((IRubyObject)(iVars.get(name))).callMethod(getRuntime().getCurrentContext(), "inspect"));
                        sep = ",";
                    }
                }
                part.append(">");
                return getRuntime().newString(part.toString());
            } finally {
                getRuntime().unregisterInspecting(this);
            }
        }
        return callMethod(getRuntime().getCurrentContext(), "to_s");
    }

    /** rb_obj_is_instance_of
     *
     */
    public RubyBoolean instance_of(IRubyObject type) {
        return getRuntime().newBoolean(type() == type);
    }

    public RubyArray instance_variables() {
        ArrayList names = new ArrayList();
        for(Iterator iter = getInstanceVariablesSnapshot().keySet().iterator();iter.hasNext();) {
            String name = (String) iter.next();

            // Do not include constants which also get stored in instance var list in classes.
            if (!Character.isUpperCase(name.charAt(0))) {
                names.add(getRuntime().newString(name));
            }
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
    public RubyArray singleton_methods(IRubyObject[] args) {
        boolean all = true;
        if(checkArgumentCount(args,0,1) == 1) {
            all = args[0].isTrue();
        }

        RubyArray result = getRuntime().newArray();

        for (RubyClass type = getMetaClass(); type != null && ((type instanceof MetaClass) || (all && type.isIncluded()));
             type = type.getSuperClass()) {
        	for (Iterator iter = type.getMethods().entrySet().iterator(); iter.hasNext(); ) {
                Map.Entry entry = (Map.Entry) iter.next();
                DynamicMethod method = (DynamicMethod) entry.getValue();

                // We do not want to capture cached methods
                if (method.getImplementationClass() != type && !(all && type.isIncluded())) {
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
            args[i].callMethod(getRuntime().getCurrentContext(), "extend_object", this);
            args[i].callMethod(getRuntime().getCurrentContext(), "extended", this);
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
        String description = callMethod(getRuntime().getCurrentContext(), "inspect").toString();
        boolean noClass = description.length() > 0 && description.charAt(0) == '#';
        ThreadContext tc = getRuntime().getCurrentContext();
        Visibility lastVis = tc.getLastVisibility();
        CallType lastCallType = tc.getLastCallType();
        String format = lastVis.errorMessageFormat(lastCallType, name);
        String msg = new PrintfFormat(format).sprintf(new Object[] { name, description,
            noClass ? "" : ":", noClass ? "" : getType().getName()});

        if (lastCallType == CallType.VARIABLE) {
        	throw getRuntime().newNameError(msg, name);
        }
        throw getRuntime().newNoMethodError(msg, name);
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

        ThreadContext tc = getRuntime().getCurrentContext();

        tc.setIfBlockAvailable();
        try {
            return callMethod(getRuntime().getCurrentContext(), name, newArgs, CallType.FUNCTIONAL);
        } finally {
            tc.clearIfBlockAvailable();
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
           throw getRuntime().newNameError("wrong instance variable name " + id, id);
       }
       if (!isTaint() && getRuntime().getSafeLevel() >= 4) {
           throw getRuntime().newSecurityError("Insecure: can't remove instance variable");
       }
       testFrozen("class/module");

       IRubyObject variable = removeInstanceVariable(id); 
       if (variable != null) {
           return variable;
       }

       throw getRuntime().newNameError("instance variable " + id + " not defined", id);
   }

    public void marshalTo(MarshalStream output) throws java.io.IOException {
        output.write('o');
        RubySymbol classname = RubySymbol.newSymbol(getRuntime(), getMetaClass().getName());
        output.dumpObject(classname);
        Map iVars = getInstanceVariablesSnapshot();
        output.dumpInt(iVars.size());
        for (Iterator iter = iVars.keySet().iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            IRubyObject value = (IRubyObject)iVars.get(name);
            
            output.dumpObject(RubySymbol.newSymbol(getRuntime(), name));
            output.dumpObject(value);
        }
    }
   
    
    /**
     * @see org.jruby.runtime.builtin.IRubyObject#getType()
     */
    public RubyClass getType() {
        return type();
    }

    /**
     * @see org.jruby.runtime.builtin.IRubyObject#scanArgs()
     */
    public IRubyObject[] scanArgs(IRubyObject[] args, int required, int optional) {
        int total = required+optional;
        int real = checkArgumentCount(args,required,total);
        IRubyObject[] narr = new IRubyObject[total];
        System.arraycopy(args,0,narr,0,real);
        for(int i=real; i<total; i++) {
            narr[i] = getRuntime().getNil();
        }
        return narr;
    }

    private transient Object dataStruct;

    /**
     * @see org.jruby.runtime.builtin.IRubyObject#dataWrapStruct()
     */
    public synchronized void dataWrapStruct(Object obj) {
        this.dataStruct = obj;
    }

    /**
     * @see org.jruby.runtime.builtin.IRubyObject#dataGetStruct()
     */
    public synchronized Object dataGetStruct() {
        return dataStruct;
    }
}
