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
 * Copyright (C) 2007 MenTaLguY <mental@rydia.net>
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

import java.util.concurrent.atomic.AtomicBoolean;
import org.jruby.evaluator.ASTInterpreter;
import org.jruby.exceptions.JumpException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callback.Callback;
import org.jruby.util.IdUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.jruby.anno.JRubyMethod;
import org.jruby.ast.Node;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.MethodIndex;

/**
 *
 * @author  jpetersen
 */
public class RubyObject implements Cloneable, IRubyObject {
    
    private RubyObject(){};
    // An instance that never equals any other instance
    public static final IRubyObject NEVER = new RubyObject();
    
    // The class of this object
    protected RubyClass metaClass;

    // The instance variables of this object.
    protected Map instanceVariables;

    private transient Object dataStruct;

    protected int flags; // zeroed by jvm
    public static final int ALL_F = -1;
    public static final int FALSE_F = 1 << 0;
    public static final int NIL_F = 1 << 1;
    public static final int FROZEN_F = 1 << 2;
    public static final int TAINTED_F = 1 << 3;

    public static final int FL_USHIFT = 4;
    
    public static final int USER0_F = (1<<(FL_USHIFT+0));
    public static final int USER1_F = (1<<(FL_USHIFT+1));
    public static final int USER2_F = (1<<(FL_USHIFT+2));
    public static final int USER3_F = (1<<(FL_USHIFT+3));
    public static final int USER4_F = (1<<(FL_USHIFT+4));
    public static final int USER5_F = (1<<(FL_USHIFT+5));
    public static final int USER6_F = (1<<(FL_USHIFT+6));
    public static final int USER7_F = (1<<(FL_USHIFT+7));

    public final void setFlag(int flag, boolean set) {
        if (set) {
            flags |= flag;
        } else {
            flags &= ~flag;
        }
    }
    
    public final boolean getFlag(int flag) { 
        return (flags & flag) != 0;
    }
    
    private Finalizer finalizer;
    
    public class Finalizer implements Finalizable {
        private long id;
        private List finalizers;
        private AtomicBoolean finalized;
        
        public Finalizer(long id) {
            this.id = id;
            this.finalized = new AtomicBoolean(false);
        }
        
        public void addFinalizer(RubyProc finalizer) {
            if (finalizers == null) {
                finalizers = new ArrayList();
            }
            finalizers.add(finalizer);
        }

        public void removeFinalizers() {
            finalizers = null;
        }
    
        public void finalize() {
            if (finalized.compareAndSet(false, true)) {
                if (finalizers != null) {
                    IRubyObject idFixnum = getRuntime().newFixnum(id);
                    for (int i = 0; i < finalizers.size(); i++) {
                        ((RubyProc)finalizers.get(i)).call(
                                new IRubyObject[] {idFixnum});
                    }
                }
            }
        }
    }

    /** standard path for object creation 
     * 
     */
    public RubyObject(Ruby runtime, RubyClass metaClass) {
        this(runtime, metaClass, runtime.isObjectSpaceEnabled());
    }

    /** path for objects who want to decide whether they want to be in ObjectSpace
     *  regardless of it being turned on or off
     *  (notably used by objects being considered immediate, they'll always pass false here)
     */
    protected RubyObject(Ruby runtime, RubyClass metaClass, boolean useObjectSpace) {
        this.metaClass = metaClass;

        if (useObjectSpace) {
            assert runtime.isObjectSpaceEnabled();
            runtime.getObjectSpace().add(this);
        }

        // FIXME are there objects who shouldn't be tainted?
        // (mri: OBJSETUP)
        if (runtime.getSafeLevel() >= 3) flags |= TAINTED_F;
    }
    
    public static RubyClass createObjectClass(Ruby runtime, RubyClass objectClass) {
        objectClass.index = ClassIndex.OBJECT;

        CallbackFactory callbackFactory = runtime.callbackFactory(RubyObject.class);
        objectClass.defineFastPrivateMethod("initialize", callbackFactory.getFastMethod("initialize"));

        return objectClass;
    }
    
    public static final ObjectAllocator OBJECT_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyObject(runtime, klass);
        }
    };

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

    public boolean isModule() {
        return false;
    }
    
    public boolean isClass() {
        return false;
    }
    
    public boolean isSingleton() {
        return false;
    }    

    /*
     *  Is object immediate (def: Fixnum, Symbol, true, false, nil?).
     */
    public boolean isImmediate() {
    	return false;
    }

    /** rb_make_metaclass
     *
     */
    public RubyClass makeMetaClass(RubyClass superClass) {
        MetaClass klass = new MetaClass(getRuntime(), superClass); // rb_class_boot
        setMetaClass(klass);

        klass.setAttached(this);

        if (isSingleton()) { // could be pulled down to RubyClass in future
            klass.setMetaClass(klass);
            klass.setSuperClass(((RubyClass)this).getSuperClass().getRealClass().getMetaClass());
        } else {
            klass.setMetaClass(superClass.getRealClass().getMetaClass());
        }

        return klass;
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
        return other == this || 
                other instanceof IRubyObject && 
                callMethod(getRuntime().getCurrentContext(), MethodIndex.EQUALEQUAL, "==", (IRubyObject) other).isTrue();
    }

    public String toString() {
        return callMethod(getRuntime().getCurrentContext(), MethodIndex.TO_S, "to_s", IRubyObject.NULL_ARRAY).toString();
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
    public final RubyClass getMetaClass() {
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
        return (flags & FROZEN_F) != 0;
    }

    /**
     * Sets the frozen.
     * @param frozen The frozen to set
     */
    public void setFrozen(boolean frozen) {
        if (frozen) {
            flags |= FROZEN_F;
        } else {
            flags &= ~FROZEN_F;
        }
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
        return (flags & TAINTED_F) != 0; 
    }

    /**
     * Sets the taint.
     * @param taint The taint to set
     */
    public void setTaint(boolean taint) {
        if (taint) {
            flags |= TAINTED_F;
        } else {
            flags &= ~TAINTED_F;
    }
    }

    public final boolean isNil() {
        return (flags & NIL_F) != 0;
    }

    public final boolean isTrue() {
        return (flags & FALSE_F) == 0;
    }

    public final boolean isFalse() {
        return (flags & FALSE_F) != 0;
    }

    public boolean respondsTo(String name) {
        if(getMetaClass().searchMethod("respond_to?") == getRuntime().getRespondToMethod()) {
            return getMetaClass().isMethodBound(name, false);
        } else {
            return callMethod(getRuntime().getCurrentContext(),"respond_to?",getRuntime().newSymbol(name)).isTrue();
        }
    }

    public boolean isKindOf(RubyModule type) {
        return type.kindOf.isKindOf(this, type);
    }

    /** rb_singleton_class
     *  Note: this method is specialized for RubyFixnum, RubySymbol, RubyNil and RubyBoolean
     */    
    public RubyClass getSingletonClass() {
        RubyClass klass;
        
        if (getMetaClass().isSingleton() && ((MetaClass)getMetaClass()).getAttached() == this) {
            klass = getMetaClass();            
        } else {
            klass = makeMetaClass(getMetaClass());
        }
        
        klass.setTaint(isTaint());
        if (isFrozen()) klass.setFrozen(true);
        
        return klass;
    }
    
    /** rb_singleton_class_clone
     *
     */
    protected RubyClass getSingletonClassClone() {
       RubyClass klass = getMetaClass();

       if (!klass.isSingleton()) return klass;

       MetaClass clone = new MetaClass(getRuntime());
       clone.flags = flags;

       if (this instanceof RubyClass) {
           clone.setMetaClass(clone);
       } else {
           clone.setMetaClass(klass.getSingletonClassClone());
       }

       clone.setSuperClass(klass.getSuperClass());

       if (klass.safeHasInstanceVariables()) clone.setInstanceVariables(new HashMap(klass.getInstanceVariables()));

       klass.cloneMethods(clone);

       ((MetaClass)clone.getMetaClass()).setAttached(clone);

       return clone;
    }

    /** init_copy
     * 
     */
    private static void initCopy(IRubyObject clone, RubyObject original) {
        assert !clone.isFrozen() : "frozen object (" + clone.getMetaClass().getName() + ") allocated";

        original.copySpecialInstanceVariables(clone);
        if (original.safeHasInstanceVariables()) clone.setInstanceVariables(new HashMap(original.getInstanceVariables()));
        /* FIXME: finalizer should be dupped here */
        clone.callMethod(clone.getRuntime().getCurrentContext(), "initialize_copy", original);
    }

    /** OBJ_INFECT
     *
     */
    public IRubyObject infectBy(IRubyObject obj) {
        if (obj.isTaint()) setTaint(true);
        return this;
    }

    public IRubyObject callSuper(ThreadContext context, IRubyObject[] args, Block block) {
        RubyModule klazz = context.getFrameKlazz();

        RubyClass superClass = RuntimeHelpers.findImplementerIfNecessary(getMetaClass(), klazz).getSuperClass();
        
        assert superClass != null : "Superclass should always be something for " + klazz.getBaseName();

        return callMethod(context, superClass, context.getFrameName(), args, CallType.SUPER, block);
    }    

    public IRubyObject callMethod(ThreadContext context, String name) {
        return callMethod(context, getMetaClass(), name, IRubyObject.NULL_ARRAY, null, Block.NULL_BLOCK);
    }
    public IRubyObject callMethod(ThreadContext context, String name, IRubyObject arg) {
        return callMethod(context, getMetaClass(), name, new IRubyObject[] { arg }, CallType.FUNCTIONAL, Block.NULL_BLOCK);
    }
    public IRubyObject callMethod(ThreadContext context, String name, Block block) {
        return callMethod(context, getMetaClass(), name, IRubyObject.NULL_ARRAY, null, block);
    }
    public IRubyObject callMethod(ThreadContext context, String name, IRubyObject[] args) {
        return callMethod(context, getMetaClass(), name, args, CallType.FUNCTIONAL, Block.NULL_BLOCK);
    }
    public IRubyObject callMethod(ThreadContext context, String name, IRubyObject[] args, Block block) {
        return callMethod(context, getMetaClass(), name, args, CallType.FUNCTIONAL, block);
    }
    public IRubyObject callMethod(ThreadContext context, String name, IRubyObject[] args, CallType callType) {
        return callMethod(context, getMetaClass(), name, args, callType, Block.NULL_BLOCK);
    }
    public IRubyObject callMethod(ThreadContext context, String name, IRubyObject[] args, CallType callType, Block block) {
        return callMethod(context, getMetaClass(), name, args, callType, block);
    }
    public IRubyObject callMethod(ThreadContext context, int methodIndex, String name) {
        return callMethod(context, getMetaClass(), methodIndex, name, IRubyObject.NULL_ARRAY, null, Block.NULL_BLOCK);
    }
    public IRubyObject callMethod(ThreadContext context, int methodIndex, String name, IRubyObject arg) {
        return callMethod(context,getMetaClass(),methodIndex,name,new IRubyObject[]{arg},CallType.FUNCTIONAL, Block.NULL_BLOCK);
    }
    public IRubyObject callMethod(ThreadContext context, int methodIndex, String name, IRubyObject[] args) {
        return callMethod(context,getMetaClass(),methodIndex,name,args,CallType.FUNCTIONAL, Block.NULL_BLOCK);
    }
    public IRubyObject callMethod(ThreadContext context, int methodIndex, String name, IRubyObject[] args, CallType callType) {
        return callMethod(context,getMetaClass(),methodIndex,name,args,callType, Block.NULL_BLOCK);
    }
    public IRubyObject callMethod(ThreadContext context, RubyModule rubyclass, int methodIndex, String name, IRubyObject[] args, CallType callType) {
        return callMethod(context, rubyclass, methodIndex, name, args, callType, Block.NULL_BLOCK);
    }
    
    public IRubyObject callMethod(ThreadContext context, RubyModule rubyclass, int methodIndex, String name, IRubyObject[] args, CallType callType, Block block) {
        if (context.getRuntime().hasEventHooks()) return callMethod(context, rubyclass, name, args, callType, block);
        
        return rubyclass.dispatcher.callMethod(context, this, rubyclass, methodIndex, name, args, callType, block);
    }
    
    /**
     *
     */
    public IRubyObject callMethod(ThreadContext context, RubyModule rubyclass, String name,
            IRubyObject[] args, CallType callType, Block block) {
        assert args != null;
        DynamicMethod method = null;
        method = rubyclass.searchMethod(name);
        

        if (method.isUndefined() || (!name.equals("method_missing") && !method.isCallableFrom(context.getFrameSelf(), callType))) {
            return RuntimeHelpers.callMethodMissing(context, this, method, name, args, context.getFrameSelf(), callType, block);
        }

        return method.call(context, this, rubyclass, name, args, block);
    }

    public IRubyObject instance_variable_get(IRubyObject var) {
    	String varName = var.asSymbol();

    	if (!IdUtil.isValidInstanceVariableName(varName)) {
    		throw getRuntime().newNameError("`" + varName + "' is not allowable as an instance variable name", varName);
    	}

    	IRubyObject variable = getInstanceVariable(varName);

    	// Pickaxe v2 says no var should show NameError, but ruby only sends back nil..
    	return variable == null ? getRuntime().getNil() : variable;
    }

    public IRubyObject instance_variable_defined_p(IRubyObject var) {
    	String varName = var.asSymbol();

    	if (!IdUtil.isValidInstanceVariableName(varName)) {
    		throw getRuntime().newNameError("`" + varName + "' is not allowable as an instance variable name", varName);
    	}

    	IRubyObject variable = getInstanceVariable(varName);

        return (variable != null) ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    public IRubyObject getInstanceVariable(String name) {
        return (IRubyObject) getInstanceVariables().get(name);
    }

    public IRubyObject instance_variable_set(IRubyObject var, IRubyObject value) {
    	String varName = var.asSymbol();

    	if (!IdUtil.isValidInstanceVariableName(varName)) {
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

    public RubyArray convertToArray() {
        return (RubyArray) convertToType(getRuntime().getArray(), MethodIndex.TO_ARY, "to_ary");
    }

    public RubyHash convertToHash() {
        return (RubyHash)convertToType(getRuntime().getHash(), MethodIndex.TO_HASH, "to_hash");
    }
    
    public RubyFloat convertToFloat() {
        return (RubyFloat) convertToType(getRuntime().getFloat(), MethodIndex.TO_F, "to_f");
    }

    public RubyInteger convertToInteger() {
        return convertToInteger(MethodIndex.TO_INT, "to_int");
    }

    public RubyInteger convertToInteger(int convertMethodIndex, String convertMethod) {
        IRubyObject val = convertToType(getRuntime().getInteger(), convertMethodIndex, convertMethod, true);
        if (!(val instanceof RubyInteger)) throw getRuntime().newTypeError(getMetaClass().getName() + "#" + convertMethod + " should return Integer");
        return (RubyInteger)val;
    }

    public RubyString convertToString() {
        return (RubyString) convertToType(getRuntime().getString(), MethodIndex.TO_STR, "to_str");
    }

    /** convert_type
     * 
     */
    public final IRubyObject convertToType(RubyClass target, int convertMethodIndex, String convertMethod, boolean raise) {
        if (!respondsTo(convertMethod)) {
            if (raise) {
                String type;
                if (isNil()) {
                    type = "nil";
                } else if (this instanceof RubyBoolean) {
                    type = isTrue() ? "true" : "false";
                } else {
                    type = target.getName();
                }
                throw getRuntime().newTypeError("can't convert " + getMetaClass().getName() + " into " + type);
            } else {
                return getRuntime().getNil();
            }
        }
        return callMethod(getRuntime().getCurrentContext(), convertMethodIndex, convertMethod);
    }
    
    public final IRubyObject convertToType(RubyClass target, int convertMethodIndex) {
        return convertToType(target, convertMethodIndex, (String)MethodIndex.NAMES.get(convertMethodIndex));
    }

    /** rb_convert_type
     * 
     */
    public final IRubyObject convertToType(RubyClass target, int convertMethodIndex, String convertMethod) {
        if (isKindOf(target)) return this;
        IRubyObject val = convertToType(target, convertMethodIndex, convertMethod, true);
        if (!val.isKindOf(target)) throw getRuntime().newTypeError(getMetaClass() + "#" + convertMethod + " should return " + target.getName());
        return val;
    }

    /*
     * @see org.jruby.runtime.builtin.IRubyObject#convertToTypeWithCheck(java.lang.String, java.lang.String)
     */
    /** rb_check_convert_type
     * 
     */
    public final IRubyObject convertToTypeWithCheck(RubyClass target, int convertMethodIndex, String convertMethod) {  
        if (isKindOf(target)) return this;
        IRubyObject val = convertToType(target, convertMethodIndex, convertMethod, false);
        if (val.isNil()) return val;
        if (!val.isKindOf(target)) throw getRuntime().newTypeError(getMetaClass() + "#" + convertMethod + " should return " + target.getName());
        return val;
    }

    /** rb_obj_as_string
     */
    public RubyString asString() {
        IRubyObject str = callMethod(getRuntime().getCurrentContext(), MethodIndex.TO_S, "to_s", IRubyObject.NULL_ARRAY);
        
        if (!(str instanceof RubyString)) return (RubyString)anyToString();
        if (isTaint()) str.setTaint(true);
        return (RubyString) str;
    }
    
    /** rb_check_string_type
     *
     */
    public IRubyObject checkStringType() {
        IRubyObject str = convertToTypeWithCheck(getRuntime().getString(), MethodIndex.TO_STR, "to_str");
        if(!str.isNil() && !(str instanceof RubyString)) {
            str = getRuntime().newString("");
        }
        return str;
    }

    /** rb_check_array_type
    *
    */    
    public IRubyObject checkArrayType() {
        return convertToTypeWithCheck(getRuntime().getArray(), MethodIndex.TO_ARY, "to_ary");
    }

    /** specific_eval
     *
     */
    public IRubyObject specificEval(RubyModule mod, IRubyObject[] args, Block block) {
        if (block.isGiven()) {
            if (args.length > 0) throw getRuntime().newArgumentError(args.length, 0);

            return yieldUnder(mod, new IRubyObject[] { this }, block);
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
        return under.executeUnder(new Callback() {
            public IRubyObject execute(IRubyObject self, IRubyObject[] args, Block block) {
                IRubyObject source = args[1];
                IRubyObject filename = args[2];
                // FIXME: lineNumber is not supported
                //IRubyObject lineNumber = args[3];

                return args[0].evalSimple(source.getRuntime().getCurrentContext(),
                                  source, filename.convertToString().toString());
            }

            public Arity getArity() {
                return Arity.optional();
            }
        }, new IRubyObject[] { this, src, file, line }, Block.NULL_BLOCK);
    }

    private IRubyObject yieldUnder(RubyModule under, IRubyObject[] args, Block block) {
        return under.executeUnder(new Callback() {
            public IRubyObject execute(IRubyObject self, IRubyObject[] args, Block block) {
                ThreadContext context = getRuntime().getCurrentContext();

                Visibility savedVisibility = block.getVisibility();

                block.setVisibility(Visibility.PUBLIC);
                try {
                    IRubyObject valueInYield;
                    boolean aValue;
                    if (args.length == 1) {
                        valueInYield = args[0];
                        aValue = false;
                    } else {
                        valueInYield = RubyArray.newArray(getRuntime(), args);
                        aValue = true;
                    }
                    
                    // FIXME: This is an ugly hack to resolve JRUBY-1381; I'm not proud of it
                    block = block.cloneBlock();
                    block.setSelf(RubyObject.this);
                    block.getFrame().setSelf(RubyObject.this);
                    // end hack
                    
                    return block.yield(context, valueInYield, RubyObject.this, context.getRubyClass(), aValue);
                    //TODO: Should next and return also catch here?
                } catch (JumpException.BreakJump bj) {
                        return (IRubyObject) bj.getValue();
                } finally {
                    block.setVisibility(savedVisibility);
                }
            }

            public Arity getArity() {
                return Arity.optional();
            }
        }, args, block);
    }

    /* (non-Javadoc)
     * @see org.jruby.runtime.builtin.IRubyObject#evalWithBinding(org.jruby.runtime.builtin.IRubyObject, org.jruby.runtime.builtin.IRubyObject, java.lang.String)
     */
    public IRubyObject evalWithBinding(ThreadContext context, IRubyObject src, IRubyObject scope, 
            String file, int lineNumber) {
        // both of these are ensured by the (very few) callers
        assert !scope.isNil();
        assert file != null;

        ISourcePosition savedPosition = context.getPosition();

        if (!(scope instanceof RubyBinding)) {
            if (scope instanceof RubyProc) {
                scope = ((RubyProc) scope).binding();
            } else {
                // bomb out, it's not a binding or a proc
                throw getRuntime().newTypeError("wrong argument type " + scope.getMetaClass() + " (expected Proc/Binding)");
            }
        }

        Block blockOfBinding = ((RubyBinding)scope).getBlock();
        // FIXME:  This determine module is in a strange location and should somehow be in block
        blockOfBinding.getDynamicScope().getStaticScope().determineModule();

        try {
            // Binding provided for scope, use it
            context.preEvalWithBinding(blockOfBinding);
            IRubyObject newSelf = context.getFrameSelf();
            Node node = 
                getRuntime().parseEval(src.toString(), file, blockOfBinding.getDynamicScope(), lineNumber);

            return ASTInterpreter.eval(getRuntime(), context, node, newSelf, blockOfBinding);
        } catch (JumpException.BreakJump bj) {
            throw getRuntime().newLocalJumpError("break", (IRubyObject)bj.getValue(), "unexpected break");
        } catch (JumpException.RedoJump rj) {
            throw getRuntime().newLocalJumpError("redo", (IRubyObject)rj.getValue(), "unexpected redo");
        } finally {
            context.postEvalWithBinding(blockOfBinding);

            // restore position
            context.setPosition(savedPosition);
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
            Node node = getRuntime().parseEval(src.toString(), file, context.getCurrentScope(), 0);
            
            return ASTInterpreter.eval(getRuntime(), context, node, this, Block.NULL_BLOCK);
        } catch (JumpException.BreakJump bj) {
            throw getRuntime().newLocalJumpError("break", (IRubyObject)bj.getValue(), "unexpected break");
        } finally {
            // restore position
            context.setPosition(savedPosition);
        }
    }

    // Methods of the Object class (rb_obj_*):

    /** rb_obj_equal
     *
     */
    public IRubyObject op_equal(IRubyObject obj) {
        return this == obj ? getRuntime().getTrue() : getRuntime().getFalse();
    }
    
    public IRubyObject equal_p(IRubyObject obj) {
        return this == obj ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    public IRubyObject eql_p(IRubyObject obj) {
        return this == obj ? getRuntime().getTrue() : getRuntime().getFalse();
    }
    
    /** rb_equal
     * 
     */
    public IRubyObject op_eqq(IRubyObject other) {
        if(this == other || callMethod(getRuntime().getCurrentContext(), MethodIndex.EQUALEQUAL, "==",other).isTrue()){
            return getRuntime().getTrue();
        }
 
        return getRuntime().getFalse();
    }
    
    protected static IRubyObject equalInternal(final ThreadContext context, final IRubyObject that, final IRubyObject other){
        if (that == other) return that.getRuntime().getTrue();
        return that.callMethod(context, MethodIndex.EQUALEQUAL, "==", other);
    }

    /** rb_eql
     *  this method is not defind for Ruby objects directly.
     *  notably overriden by RubyFixnum, RubyString, RubySymbol - these do a short-circuit calls.
     *  see: rb_any_cmp() in hash.c
     *  do not confuse this method with eql_p methods (which it calls by default), eql is mainly used for hash key comparison 
     */
    public boolean eql(IRubyObject other) {
        return callMethod(getRuntime().getCurrentContext(), MethodIndex.EQL_P, "eql?", other).isTrue();
    }

    protected static boolean eqlInternal(final ThreadContext context, final IRubyObject that, final IRubyObject other){
        if (that == other) return true;
        return that.callMethod(context, MethodIndex.EQL_P, "eql?", other).isTrue();
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
    public synchronized IRubyObject id() {
        return getRuntime().newFixnum(getRuntime().getObjectSpace().idOf(this));
    }

    /** rb_obj_id_obsolete
     * 
     */
    public synchronized IRubyObject id_deprecated() {
        getRuntime().getWarnings().warn("Object#id will be deprecated; use Object#object_id");
        return id();
    }
    
    public RubyFixnum hash() {
        return getRuntime().newFixnum(super.hashCode());
    }

    public int hashCode() {
        IRubyObject hashValue = callMethod(getRuntime().getCurrentContext(), MethodIndex.HASH, "hash");
        
        if (hashValue instanceof RubyFixnum) return (int) RubyNumeric.fix2long(hashValue); 
        
        return super.hashCode();
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
        if (isImmediate()) throw getRuntime().newTypeError("can't clone " + getMetaClass().getName());
        
        IRubyObject clone = getMetaClass().getRealClass().allocate();
        clone.setMetaClass(getSingletonClassClone());
        if (isTaint()) clone.setTaint(true);

        initCopy(clone, this);

        if (isFrozen()) clone.setFrozen(true);
        return clone;
    }

    /** rb_obj_dup
     *  should be overriden only by: Proc
     */
    public IRubyObject dup() {
        if (isImmediate()) throw getRuntime().newTypeError("can't dup " + getMetaClass().getName());

        IRubyObject dup = getMetaClass().getRealClass().allocate();
        if (isTaint()) dup.setTaint(true);

        initCopy(dup, this);

        return dup;
    }
    
    /** Lots of MRI objects keep their state in non-lookupable ivars (e:g. Range, Struct, etc)
     *  This method is responsible for dupping our java field equivalents 
     * 
     */
    protected void copySpecialInstanceVariables(IRubyObject clone) {
    }    

    public IRubyObject display(IRubyObject[] args) {
        IRubyObject port = args.length == 0
            ? getRuntime().getGlobalVariables().get("$>") : args[0];

        port.callMethod(getRuntime().getCurrentContext(), "write", this);

        return getRuntime().getNil();
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

    /** inspect_obj
     * 
     */
    private StringBuffer inspectObj(StringBuffer part) {
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
        return part;
    }

    /** rb_obj_inspect
     *
     */
    public IRubyObject inspect() {
        Ruby runtime = getRuntime();
        if ((!isImmediate()) &&
                // TYPE(obj) == T_OBJECT
                !(this instanceof RubyClass) &&
                this != runtime.getObject() &&
                this != runtime.getModule() &&
                !(this instanceof RubyModule) &&
                safeHasInstanceVariables()) {

            StringBuffer part = new StringBuffer();
            String cname = getMetaClass().getRealClass().getName();
            part.append("#<").append(cname).append(":0x");
            part.append(Integer.toHexString(System.identityHashCode(this)));

            if (runtime.isInspecting(this)) {
                /* 6:tags 16:addr 1:eos */
                part.append(" ...>");
                return runtime.newString(part.toString());
            }
            try {
                runtime.registerInspecting(this);
                return runtime.newString(inspectObj(part).toString());
            } finally {
                runtime.unregisterInspecting(this);
            }
        }

        if (isNil()) return RubyNil.inspect(this);
        return callMethod(runtime.getCurrentContext(), MethodIndex.TO_S, "to_s", IRubyObject.NULL_ARRAY);
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
        if (!(type instanceof RubyModule)) {
            // TODO: newTypeError does not offer enough for ruby error string...
            throw getRuntime().newTypeError(type, getRuntime().getModule());
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
        Arity.checkArgumentCount(getRuntime(), args, 0, 1);

        if (args.length == 0) {
            args = new IRubyObject[] { getRuntime().getTrue() };
        }

        return getMetaClass().public_instance_methods(args);
	}

    /** rb_obj_protected_methods
     *
     */
    public IRubyObject protected_methods(IRubyObject[] args) {
        Arity.checkArgumentCount(getRuntime(), args, 0, 1);

        if (args.length == 0) {
            args = new IRubyObject[] { getRuntime().getTrue() };
        }

        return getMetaClass().protected_instance_methods(args);
    }

    /** rb_obj_private_methods
     *
     */
    public IRubyObject private_methods(IRubyObject[] args) {
        Arity.checkArgumentCount(getRuntime(), args, 0, 1);

        if (args.length == 0) {
            args = new IRubyObject[] { getRuntime().getTrue() };
        }

        return getMetaClass().private_instance_methods(args);
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
                if (method.getVisibility() == Visibility.PUBLIC && ! result.includes(methodName)) {
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
    
    @JRubyMethod(name = "to_a", visibility = Visibility.PUBLIC)
    public RubyArray to_a() {
        getRuntime().getWarnings().warn("default 'to_a' will be obsolete");
        return getRuntime().newArray(this);
    }

    public IRubyObject instance_eval(IRubyObject[] args, Block block) {
        RubyModule klazz;
        if (isImmediate()) {
            klazz = getRuntime().getCurrentContext().getPreviousFrame().getKlazz();
            if (klazz == null) klazz = getRuntime().getObject();
        } else {
            klazz = getSingletonClass();
        }
        return specificEval(klazz, args, block);
    }

    public IRubyObject instance_exec(IRubyObject[] args, Block block) {
        if (!block.isGiven()) {
            throw getRuntime().newArgumentError("block not supplied");
        }

        RubyModule klazz;
        if (isImmediate()) {
            klazz = getRuntime().getCurrentContext().getPreviousFrame().getKlazz();
            if (klazz == null) klazz = getRuntime().getObject();            
        } else {
            klazz = getSingletonClass();
        }

        return yieldUnder(klazz, args, block);
    }

    public IRubyObject extend(IRubyObject[] args) {
        Arity.checkArgumentCount(getRuntime(), args, 1, -1);

        // Make sure all arguments are modules before calling the callbacks
        for (int i = 0; i < args.length; i++) {
            if (!args[i].isModule()) throw getRuntime().newTypeError(args[i], getRuntime().getModule()); 
        }

        for (int i = 0; i < args.length; i++) {
            args[i].callMethod(getRuntime().getCurrentContext(), "extend_object", this);
            args[i].callMethod(getRuntime().getCurrentContext(), "extended", this);
        }
        return this;
    }

    public IRubyObject initialize() {
        return getRuntime().getNil();
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

        ThreadContext context = getRuntime().getCurrentContext();
        assert args != null;
        DynamicMethod method = null;
        RubyModule rubyClass = getMetaClass();
        method = rubyClass.searchMethod(name);

        // send doesn't check visibility
        if (method.isUndefined()) {
            return RuntimeHelpers.callMethodMissing(context, this, method, name, newArgs, context.getFrameSelf(), CallType.FUNCTIONAL, block);
        }

        return method.call(context, this, rubyClass, name, newArgs, block);
    }
    
    public IRubyObject nil_p() {
    	return getRuntime().getFalse();
    }
    
    public IRubyObject op_match(IRubyObject arg) {
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
 
    public void addFinalizer(RubyProc finalizer) {
        if (this.finalizer == null) {
            this.finalizer = new Finalizer(getRuntime().getObjectSpace().idOf(this));
            getRuntime().addFinalizer(this.finalizer);
        }
        this.finalizer.addFinalizer(finalizer);
    }

    public void removeFinalizers() {
        if (finalizer != null) {
            finalizer.removeFinalizers();
            finalizer = null;
            getRuntime().removeFinalizer(this.finalizer);
        }
    }
}
