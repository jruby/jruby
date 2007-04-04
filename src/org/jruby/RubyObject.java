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

import org.jruby.evaluator.EvaluationState;
import org.jruby.exceptions.JumpException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callback.Callback;
import org.jruby.util.IdUtil;
import org.jruby.util.Sprintf;
import org.jruby.util.collections.SinglyLinkedList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.jruby.ast.Node;
import org.jruby.evaluator.CreateJumpTargetVisitor;
import org.jruby.runtime.ClassIndex;

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
    protected boolean isTrue = true;

    public RubyObject(Ruby runtime, RubyClass metaClass) {
        this(runtime, metaClass, runtime.isObjectSpaceEnabled());
    }

    public RubyObject(Ruby runtime, RubyClass metaClass, boolean useObjectSpace) {
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

    public void attachToObjectSpace() {
        getRuntime().getObjectSpace().add(this);
    }
    
    /**
     * This is overridden in the other concrete Java builtins to provide a fast way
     * to determine what type they are.
     */
    public int getNativeTypeIndex() {
        return ClassIndex.OBJECT;
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
    public RubyClass makeMetaClass(RubyClass superClass, SinglyLinkedList parentCRef) {
        RubyClass klass = new MetaClass(getRuntime(), superClass, getMetaClass().getAllocator(), parentCRef);
        setMetaClass(klass);
		
        klass.setInstanceVariable("__attached__", this);

        if (this instanceof RubyClass && isSingleton()) { // could be pulled down to RubyClass in future
            klass.setMetaClass(klass);
            klass.setSuperClass(((RubyClass)this).getSuperClass().getRealClass().getMetaClass());
        } else {
            klass.setMetaClass(superClass.getRealClass().getMetaClass());
        }
        
        // use same ClassIndex as metaclass, since we're technically still of that type 
        klass.index = superClass.index;
        return klass;
    }
        
    public boolean isSingleton() {
        return false;
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
        return callMethod(getRuntime().getCurrentContext(), "to_s").toString();
    }

    /** Getter for property ruby.
     * @return Value of property ruby.
     */
    public Ruby getRuntime() {
        return metaClass.getRuntime();
    }
    
    public boolean safeHasInstanceVariables() {
        return instanceVariables != null && instanceVariables.size() > 0;
    }
    
    public Map safeGetInstanceVariables() {
        return instanceVariables == null ? null : getInstanceVariablesSnapshot();
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
           throw getRuntime().newFrozenError(message + getMetaClass().getName());
       }
   }

   protected void checkFrozen() {
       testFrozen("can't modify frozen ");
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

    public final boolean isTrue() {
        return isTrue;
    }

    public final boolean isFalse() {
        return !isTrue;
    }

    public boolean respondsTo(String name) {
        if(getMetaClass().searchMethod("respond_to?") == getRuntime().getRespondToMethod()) {
            return getMetaClass().isMethodBound(name, false);
        } else {
            return callMethod(getRuntime().getCurrentContext(),"respond_to?",getRuntime().newSymbol(name)).isTrue();
        }
    }

    public boolean isKindOf(RubyModule type) {
        return getMetaClass().hasModuleInHierarchy(type);
    }

    /** rb_singleton_class
     *
     */    
    public RubyClass getSingletonClass() {
        RubyClass klass;
        
        if (getMetaClass().isSingleton() && getMetaClass().getInstanceVariable("__attached__") == this) {
            klass = getMetaClass();            
        } else {
            klass = makeMetaClass(getMetaClass(), getMetaClass().getCRef());
        }
        
        klass.setTaint(isTaint());
        klass.setFrozen(isFrozen());
        
        return klass;
    }
    
    /** rb_singleton_class_clone
     *
     */
    public RubyClass getSingletonClassClone() {
       RubyClass klass = getMetaClass();

       if (!klass.isSingleton()) {
           return klass;
		}
       
       MetaClass clone = new MetaClass(getRuntime(), klass.getSuperClass(), getMetaClass().getAllocator(), getMetaClass().getCRef());
       clone.setFrozen(klass.isFrozen());
       clone.setTaint(klass.isTaint());

       if (this instanceof RubyClass) {
           clone.setMetaClass(clone);
       } else {
           clone.setMetaClass(klass.getSingletonClassClone());
       }
       
       if (klass.safeHasInstanceVariables()) {
           clone.setInstanceVariables(new HashMap(klass.getInstanceVariables()));
       }

       klass.cloneMethods(clone);

       clone.getMetaClass().setInstanceVariable("__attached__", clone);

       return clone;
    }    

    /** rb_define_singleton_method
     *
     */
    public void defineSingletonMethod(String name, Callback method) {
        getSingletonClass().defineMethod(name, method);
    }

    /** init_copy
     * 
     */
    public static void initCopy(IRubyObject clone, IRubyObject original) {
        assert original != null;
        assert !clone.isFrozen() : "frozen object (" + clone.getMetaClass().getName() + ") allocated";

        clone.setInstanceVariables(new HashMap(original.getInstanceVariables()));
        /* FIXME: finalizer should be dupped here */
        clone.callMethod(clone.getRuntime().getCurrentContext(), "initialize_copy", original);
    }

    /** OBJ_INFECT
     *
     */
    public IRubyObject infectBy(IRubyObject obj) {
        setTaint(isTaint() || obj.isTaint());

        return this;
    }

    public IRubyObject callSuper(ThreadContext context, IRubyObject[] args, Block block) {
        RubyModule klazz = context.getFrameKlazz();

        RubyClass superClass = klazz.getSuperClass();
        
        assert superClass != null : "Superclass should always be something for " + klazz.getBaseName();

        return callMethod(context, superClass, context.getFrameName(), args, CallType.SUPER, block);
    }    

    /**
     *
     */
    public IRubyObject callMethod(ThreadContext context, String name, IRubyObject[] args) {
        return callMethod(context, getMetaClass(), name, args, CallType.FUNCTIONAL, Block.NULL_BLOCK);
    }

    public IRubyObject callMethod(ThreadContext context, String name, IRubyObject[] args, Block block) {
        return callMethod(context, getMetaClass(), name, args, CallType.FUNCTIONAL, block);
    }
    
    /**
     *
     */
    public IRubyObject callMethod(ThreadContext context, String name,
            IRubyObject[] args, CallType callType) {
        return callMethod(context, getMetaClass(), name, args, callType, Block.NULL_BLOCK);
    }
    
    public IRubyObject callMethod(ThreadContext context, String name,
            IRubyObject[] args, CallType callType, Block block) {
        return callMethod(context, getMetaClass(), name, args, callType, block);
    }

    public IRubyObject callMethod(ThreadContext context,byte switchValue, String name,
                                  IRubyObject arg) {
        return callMethod(context,getMetaClass(),switchValue,name,new IRubyObject[]{arg},CallType.FUNCTIONAL, Block.NULL_BLOCK);
    }

    public IRubyObject callMethod(ThreadContext context,byte switchValue, String name,
                                  IRubyObject[] args) {
        return callMethod(context,getMetaClass(),switchValue,name,args,CallType.FUNCTIONAL, Block.NULL_BLOCK);
    }

    public IRubyObject callMethod(ThreadContext context, byte switchValue, String name,
                                  IRubyObject[] args, CallType callType) {
        return callMethod(context,getMetaClass(),switchValue,name,args,callType, Block.NULL_BLOCK);
    }
    
    /**
     * Used by the compiler to ease calling indexed methods, also to handle visibility.
     * NOTE: THIS IS NOT THE SAME AS THE SWITCHVALUE VERSIONS.
     */
    public IRubyObject compilerCallMethodWithIndex(ThreadContext context, byte methodIndex, String name, IRubyObject[] args, IRubyObject self, CallType callType, Block block) {
        RubyModule module = getMetaClass();
        
        if (module.index != 0) {
            return callMethod(context, module, getRuntime().getSelectorTable().table[module.index][methodIndex], name, args, callType, block);
        }
        
        return compilerCallMethod(context, name, args, self, callType, block);
    }
    
    /**
     * Used by the compiler to handle visibility
     */
    public IRubyObject compilerCallMethod(ThreadContext context, String name,
            IRubyObject[] args, IRubyObject self, CallType callType, Block block) {
        assert args != null;
        DynamicMethod method = null;
        RubyModule rubyclass = getMetaClass();
        method = rubyclass.searchMethod(name);
        
        IRubyObject mmResult = callMethodMissingIfNecessary(context, this, method, name, args, self, callType, block);
        if (mmResult != null) {
            return mmResult;
        }

        return method.call(context, this, rubyclass, name, args, false, block);
    }
    
    public static IRubyObject callMethodMissingIfNecessary(ThreadContext context, IRubyObject receiver, DynamicMethod method, String name,
            IRubyObject[] args, IRubyObject self, CallType callType, Block block) {
        if (method.isUndefined() ||
            !(name.equals("method_missing") ||
              method.isCallableFrom(self, callType))) {

            if (callType == CallType.SUPER) {
                throw self.getRuntime().newNameError("super: no superclass method '" + name + "'", name);
            }

            // store call information so method_missing impl can use it
            context.setLastCallStatus(callType);

            if (name.equals("method_missing")) {
                return RubyKernel.method_missing(self, args, block);
            }


            IRubyObject[] newArgs = new IRubyObject[args.length + 1];
            System.arraycopy(args, 0, newArgs, 1, args.length);
            newArgs[0] = RubySymbol.newSymbol(self.getRuntime(), name);

            return receiver.callMethod(context, "method_missing", newArgs, block);
        }
        
        // kludgy.
        return null;
    }

    /**
     *
     */
    public IRubyObject callMethod(ThreadContext context, RubyModule rubyclass, byte switchvalue, String name,
            IRubyObject[] args, CallType callType) {
        return callMethod(context, rubyclass, switchvalue, name, args, callType, Block.NULL_BLOCK);
    }

    /**
     *
     */
    public IRubyObject callMethod(ThreadContext context, RubyModule rubyclass, byte switchvalue, String name,
            IRubyObject[] args, CallType callType, Block block) {
        return callMethod(context, rubyclass, name, args, callType, block);
    }
    
    /**
     *
     */
    public IRubyObject callMethod(ThreadContext context, RubyModule rubyclass, String name,
            IRubyObject[] args, CallType callType, Block block) {
        assert args != null;
        DynamicMethod method = null;
        method = rubyclass.searchMethod(name);
        
        IRubyObject mmResult = callMethodMissingIfNecessary(context, this, method, name, args, context.getFrameSelf(), callType, block);
        if (mmResult != null) {
            return mmResult;
        }

        return method.call(context, this, rubyclass, name, args, false, block);
    }

    public IRubyObject callMethod(ThreadContext context, String name) {
        return callMethod(context, name, IRubyObject.NULL_ARRAY, null, Block.NULL_BLOCK);
    }

    public IRubyObject callMethod(ThreadContext context, String name, Block block) {
        return callMethod(context, name, IRubyObject.NULL_ARRAY, null, block);
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

    	if (!IdUtil.isInstanceVariable(varName)) {
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

    	if (!IdUtil.isInstanceVariable(varName)) {
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

    public void callInit(IRubyObject[] args, Block block) {
        callMethod(getRuntime().getCurrentContext(), "initialize", args, block);
    }

    /** rb_to_id
     *
     */
    public String asSymbol() {
        throw getRuntime().newTypeError(inspect().toString() + " is not a symbol");
    }

    public static String trueFalseNil(IRubyObject v) {
        return trueFalseNil(v.getMetaClass().getName());
    }

    public static String trueFalseNil(String v) {
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
        return (RubyArray) convertToType(getRuntime().getArray(), "to_ary", true);
    }

    public RubyFloat convertToFloat() {
        return (RubyFloat) convertToType(getRuntime().getClass("Float"), "to_f", true);
    }

    public RubyInteger convertToInteger() {
        return (RubyInteger) convertToType(getRuntime().getClass("Integer"), "to_int", true);
    }

    public RubyString convertToString() {
        return (RubyString) convertToType(getRuntime().getString(), "to_str", true);
    }

    /*
     * @see org.jruby.runtime.builtin.IRubyObject#convertToTypeWithCheck(java.lang.String, java.lang.String)
     */
    public IRubyObject convertToTypeWithCheck(RubyClass targetType, String convertMethod) {
        return convertToType(targetType, convertMethod, false, true, false);
    }

    /*
     * @see org.jruby.runtime.builtin.IRubyObject#convertToType(java.lang.String, java.lang.String, boolean)
     */
    public IRubyObject convertToType(RubyClass targetType, String convertMethod, boolean raise) {
        return convertToType(targetType, convertMethod, raise, false, false);
    }
    
    public IRubyObject convertToType(RubyClass targetType, String convertMethod, boolean raiseOnMissingMethod, boolean raiseOnWrongTypeResult, boolean allowNilThrough) {
        if (isKindOf(targetType)) {
            return this;
        }
        
        if (!respondsTo(convertMethod)) {
            if (raiseOnMissingMethod) {
                throw getRuntime().newTypeError(
                    "can't convert " + trueFalseNil(getMetaClass().getName()) + " into " + trueFalseNil(targetType));
            } 

            return getRuntime().getNil();
        }
        
        IRubyObject value = callMethod(getRuntime().getCurrentContext(), convertMethod);
        
        if (allowNilThrough && value.isNil()) {
            return value;
        }
        
        if (raiseOnWrongTypeResult && !value.isKindOf(targetType)) {
            throw getRuntime().newTypeError(getMetaClass().getName() + "#" + convertMethod +
                    " should return " + targetType);
        }
        
        return value;
    }

    /** rb_obj_as_string
     */
    public RubyString asString() {
        if (this instanceof RubyString) return (RubyString) this;
        
        IRubyObject str = this.callMethod(getRuntime().getCurrentContext(), "to_s");
        
        if (!(str instanceof RubyString)) str = anyToString();

        return (RubyString) str;
    }
    
    /** rb_check_string_type
     *
     */
    public IRubyObject checkStringType() {
        IRubyObject str = convertToTypeWithCheck(getRuntime().getString(), "to_str");
        if(!str.isNil() && !(str instanceof RubyString)) {
            str = getRuntime().newString("");
        }
        return str;
    }

    /** rb_check_array_type
    *
    */    
    public IRubyObject checkArrayType() {
        return convertToTypeWithCheck(getRuntime().getArray(), "to_ary");
    }

    /** specific_eval
     *
     */
    public IRubyObject specificEval(RubyModule mod, IRubyObject[] args, Block block) {
        if (block.isGiven()) {
            if (args.length > 0) throw getRuntime().newArgumentError(args.length, 0);

            return yieldUnder(mod, block);
        }
        ThreadContext tc = getRuntime().getCurrentContext();

        if (args.length == 0) {
		    throw getRuntime().newArgumentError("block not supplied");
		} else if (args.length > 3) {
		    String lastFuncName = tc.getFrameName();
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
            public IRubyObject execute(IRubyObject self, IRubyObject[] args, Block block) {
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
        }, new IRubyObject[] { this, src, file, line }, Block.NULL_BLOCK);
    }

    private IRubyObject yieldUnder(RubyModule under, Block block) {
        return under.executeUnder(new Callback() {
            public IRubyObject execute(IRubyObject self, IRubyObject[] args, Block block) {
                ThreadContext context = getRuntime().getCurrentContext();

                Visibility savedVisibility = block.getVisibility();

                block.setVisibility(Visibility.PUBLIC);
                try {
                    IRubyObject valueInYield = args[0];
                    IRubyObject selfInYield = args[0];
                    return block.yield(context, valueInYield, selfInYield, context.getRubyClass(), false);
                    //TODO: Should next and return also catch here?
                } catch (JumpException je) {
                	if (je.getJumpType() == JumpException.JumpType.BreakJump) {
                		return (IRubyObject) je.getValue();
                	} 

                    throw je;
                } finally {
                    block.setVisibility(savedVisibility);
                }
            }

            public Arity getArity() {
                return Arity.optional();
            }
        }, new IRubyObject[] { this }, block);
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
            IRubyObject newSelf = threadContext.getFrameSelf();
            Node node = getRuntime().parse(src.toString(), file, blockOfBinding.getDynamicScope());
            CreateJumpTargetVisitor.setJumpTarget(context.getFrameJumpTarget(), node);

            return EvaluationState.eval(getRuntime(), threadContext, node, newSelf, blockOfBinding);
        } catch (JumpException je) {
            if (je.getJumpType() == JumpException.JumpType.BreakJump) {
                throw getRuntime().newLocalJumpError("unexpected break");
            }
            throw je;
        } finally {
            threadContext.postEvalWithBinding();

            // restore position
            threadContext.setPosition(savedPosition);
        }
    }

    /* (non-Javadoc)
     * @see org.jruby.runtime.builtin.IRubyObject#evalSimple(org.jruby.runtime.builtin.IRubyObject, java.lang.String)
     */
    public IRubyObject evalSimple(ThreadContext context, IRubyObject src, String file) {
        // this is ensured by the callers
        assert file != null;

        ISourcePosition savedPosition = context.getPosition();

        // no binding, just eval in "current" frame (caller's frame)
        try {
            Node node = getRuntime().parse(src.toString(), file, context.getCurrentScope());
            CreateJumpTargetVisitor.setJumpTarget(context.getFrameJumpTarget(), node);
            
            return EvaluationState.eval(getRuntime(), context, node, this, Block.NULL_BLOCK);
        } catch (JumpException je) {
            if (je.getJumpType() == JumpException.JumpType.BreakJump) {
                throw getRuntime().newLocalJumpError("unexpected break");
            }
            throw je;
        } finally {
            // restore position
            context.setPosition(savedPosition);
        }
    }

    // Methods of the Object class (rb_obj_*):

    /** rb_obj_equal
     *
     */
    public IRubyObject obj_equal(IRubyObject obj) {
        return this == obj ? getRuntime().getTrue() : getRuntime().getFalse();
//        if (isNil()) {
//            return getRuntime().newBoolean(obj.isNil());
//        }
//        return getRuntime().newBoolean(this == obj);
    }

	public IRubyObject same(IRubyObject other) {
		return this == other ? getRuntime().getTrue() : getRuntime().getFalse();
	}

    
    /** rb_obj_init_copy
     * 
     */
	public IRubyObject initialize_copy(IRubyObject original) {
	    if (this == original) return this;
	    
	    checkFrozen();
        
        if (getMetaClass().getRealClass() != original.getMetaClass().getRealClass()) {
	            throw getRuntime().newTypeError("initialize_copy should take same class object");
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
        Arity.checkArgumentCount(getRuntime(), args, 1, 2);

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

    public synchronized RubyFixnum id_deprecated() {
        getRuntime().getWarnings().warn("Object#id will be deprecated; use Object#object_id");
        return getRuntime().newFixnum(getRuntime().getObjectSpace().idOf(this));
    }
    
    public RubyFixnum hash() {
        return getRuntime().newFixnum(System.identityHashCode(this));
    }

    public int hashCode() {
        IRubyObject hashValue = callMethod(getRuntime().getCurrentContext(), "hash");
        
        if (hashValue instanceof RubyFixnum) return (int) RubyNumeric.fix2long(hashValue); 
        
        return System.identityHashCode(this);
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
     *  should be overriden only by: Proc, Method, UnboundedMethod, Binding
     */
    public IRubyObject rbClone() {
        if (isImmediate()) { // rb_special_const_p(obj) equivalent
            throw getRuntime().newTypeError("can't clone " + getMetaClass().getName());
        }
        
        IRubyObject clone = doClone();
        clone.setMetaClass(getSingletonClassClone());
        clone.setTaint(isTaint());
        initCopy(clone, this);
        clone.setFrozen(isFrozen());
        return clone;
    }

    // Hack: allow RubyModule and RubyClass to override the allocation and return the the correct Java instance
    // Cloning a class object doesn't work otherwise and I don't really understand why --sma
    protected IRubyObject doClone() {
        RubyClass realClass = getMetaClass().getRealClass();
    	return realClass.getAllocator().allocate(getRuntime(), realClass);
    }

    public IRubyObject display(IRubyObject[] args) {
        IRubyObject port = args.length == 0
            ? getRuntime().getGlobalVariables().get("$>") : args[0];

        port.callMethod(getRuntime().getCurrentContext(), "write", this);

        return getRuntime().getNil();
    }

    /** rb_obj_dup
     *  should be overriden only by: Proc
     */
    public IRubyObject dup() {
        if (isImmediate()) {
            throw getRuntime().newTypeError("can't dup " + getMetaClass().getName());
        }        
        
        IRubyObject dup = doClone();    

        dup.setMetaClass(type());
        dup.setFrozen(false);
        dup.setTaint(isTaint());
        
        initCopy(dup, this);

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
        if ((!isImmediate()) &&
                // TYPE(obj) == T_OBJECT
                !(this instanceof RubyClass) &&
                this != getRuntime().getObject() &&
                this != getRuntime().getClass("Module") &&
                !(this instanceof RubyModule) &&
                safeHasInstanceVariables()) {

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
                    if(IdUtil.isInstanceVariable(name)) {
                        part.append(sep);
                        part.append(" ");
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
            if (IdUtil.isInstanceVariable(name)) {
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
    	Arity.checkArgumentCount(getRuntime(), args, 0, 1);

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
        if(Arity.checkArgumentCount(getRuntime(), args,0,1) == 1) {
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

    public IRubyObject anyToString() {
        String cname = getMetaClass().getRealClass().getName();
        /* 6:tags 16:addr 1:eos */
        RubyString str = getRuntime().newString("#<" + cname + ":0x" + Integer.toHexString(System.identityHashCode(this)) + ">");
        str.setTaint(isTaint());
        return str;
    }

    public IRubyObject to_s() {
    	return anyToString();
    }

    public IRubyObject instance_eval(IRubyObject[] args, Block block) {
        return specificEval(getSingletonClass(), args, block);
    }

    public IRubyObject extend(IRubyObject[] args) {
        Arity.checkArgumentCount(getRuntime(), args, 1, -1);

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

    public IRubyObject inherited(IRubyObject arg, Block block) {
    	return getRuntime().getNil();
    }
    public IRubyObject initialize(IRubyObject[] args, Block block) {
    	return getRuntime().getNil();
    }

    public IRubyObject method_missing(IRubyObject[] args, Block block) {
        if (args.length == 0) {
            throw getRuntime().newArgumentError("no id given");
        }

        String name = args[0].asSymbol();
        String description = null;
        if("inspect".equals(name) || "to_s".equals(name)) {
            description = anyToString().toString();
        } else {
            description = inspect().toString();
        }
        boolean noClass = description.length() > 0 && description.charAt(0) == '#';
        Ruby runtime = getRuntime();
        ThreadContext tc = runtime.getCurrentContext();
        Visibility lastVis = tc.getLastVisibility();
        if(null == lastVis) {
            lastVis = Visibility.PUBLIC;
        }
        CallType lastCallType = tc.getLastCallType();
        String format = lastVis.errorMessageFormat(lastCallType, name);
        String msg = Sprintf.sprintf(runtime.newString(format), 
                runtime.newArray(new IRubyObject[] { 
                        runtime.newString(name), runtime.newString(description),
                        runtime.newString(noClass ? "" : ":"), 
                        runtime.newString(noClass ? "" : getType().getName())
                })).toString();

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
    public IRubyObject send(IRubyObject[] args, Block block) {
        if (args.length < 1) {
            throw getRuntime().newArgumentError("no method name given");
        }
        String name = args[0].asSymbol();

        IRubyObject[] newArgs = new IRubyObject[args.length - 1];
        System.arraycopy(args, 1, newArgs, 0, newArgs.length);

        return callMethod(getRuntime().getCurrentContext(), name, newArgs, CallType.FUNCTIONAL, block);
    }
    
    public IRubyObject nil_p() {
    	return getRuntime().getFalse();
    }
    
    public IRubyObject match(IRubyObject arg) {
    	return getRuntime().getFalse();
    }
    
   public IRubyObject remove_instance_variable(IRubyObject name, Block block) {
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
        int real = Arity.checkArgumentCount(getRuntime(), args,required,total);
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

    /** rb_equal
     * 
     */
    public IRubyObject equal(IRubyObject other) {
        if(this == other || callMethod(getRuntime().getCurrentContext(), "==",other).isTrue()){
            return getRuntime().getTrue();
        }
 
        return getRuntime().getFalse();
    }
    
    public final IRubyObject equalInternal(final ThreadContext context, final IRubyObject other){
        if (this == other) return getRuntime().getTrue();
        return callMethod(context, "==", other);
    }
}
