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
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
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
import java.util.TreeMap;

import org.jruby.ast.AttrSetNode;
import org.jruby.ast.InstVarNode;
import org.jruby.ast.ZSuperNode;
import org.jruby.exceptions.NameError;
import org.jruby.exceptions.SecurityError;
import org.jruby.internal.runtime.methods.AliasMethod;
import org.jruby.internal.runtime.methods.CacheEntry;
import org.jruby.internal.runtime.methods.CallbackMethod;
import org.jruby.internal.runtime.methods.EvaluateMethod;
import org.jruby.internal.runtime.methods.MethodMethod;
import org.jruby.internal.runtime.methods.ProcMethod;
import org.jruby.internal.runtime.methods.UndefinedMethod;
import org.jruby.internal.runtime.methods.WrapperCallable;
import org.jruby.runtime.CallType;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.Frame;
import org.jruby.runtime.ICallable;
import org.jruby.runtime.Iter;
import org.jruby.runtime.LastCallStatus;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callback.Callback;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.IdUtil;
import org.jruby.util.collections.ArrayStack;

/**
 *
 * @author  jpetersen
 */
public class RubyModule extends RubyObject {
    private static final String CVAR_TAINT_ERROR =
        "Insecure: can't modify class variable";
    private static final String CVAR_FREEZE_ERROR = "class/module";
    
    // superClass may be null.
    private RubyClass superClass;
    
    // Containing class...It is guaranteed to never be null.  And will always
    // end up at RubyObject if you follow it up.
    private RubyModule parentModule;

    // ClassId is the name of the class/module sans where it is located.
    // If it is null, then it an anonymous class.
    private String classId;

    private Map methods = new HashMap();

    private Map methodCache = new TreeMap();

    protected RubyModule(Ruby runtime, RubyClass metaClass, RubyClass superClass, RubyModule parentModule, String name) {
        super(runtime, metaClass);
        
        this.superClass = superClass;
        this.parentModule = parentModule;
        this.classId = name;

        // If no parent is passed in, it is safe to assume Object.
        if (this.parentModule == null) {
            this.parentModule = runtime.getClasses().getObjectClass();

            // We are constructing object itself...Set its parent to itself.
            if (this.parentModule == null) {
                this.parentModule = this;
            }
        }
    }
    
    public void setParentModule(RubyModule parentModule) {
    	this.parentModule = parentModule;
    }

    /** Getter for property superClass.
     * @return Value of property superClass.
     */
    public RubyClass getSuperClass() {
        return this.superClass;
    }

    // FIXME protected should be enough
    public void setSuperClass(RubyClass superClass) {
        this.superClass = superClass;
    }

    public Map getMethods() {
        return methods;
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
        CallbackFactory callbackFactory = moduleClass.getRuntime().callbackFactory(RubyModule.class);
        Callback module_eval = callbackFactory.getOptMethod("module_eval");
        
        moduleClass.defineMethod("===", callbackFactory.getMethod("op_eqq", IRubyObject.class));
        moduleClass.defineMethod("<=>", callbackFactory.getMethod("op_cmp", IRubyObject.class));
        moduleClass.defineMethod("<", callbackFactory.getMethod("op_lt", IRubyObject.class));
        moduleClass.defineMethod("<=", callbackFactory.getMethod("op_le", IRubyObject.class));
        moduleClass.defineMethod(">", callbackFactory.getMethod("op_gt", IRubyObject.class));
        moduleClass.defineMethod(">=", callbackFactory.getMethod("op_ge", IRubyObject.class));
        moduleClass.defineMethod("ancestors", callbackFactory.getMethod("ancestors"));
        moduleClass.defineMethod("class_eval", module_eval);
        moduleClass.defineMethod("class_variables", callbackFactory.getMethod("class_variables"));
        moduleClass.defineMethod("clone", callbackFactory.getMethod("rbClone"));
        moduleClass.defineMethod("const_defined?", callbackFactory.getMethod("const_defined", IRubyObject.class));
        moduleClass.defineMethod("const_get", callbackFactory.getMethod("const_get", IRubyObject.class));
        moduleClass.defineMethod("const_missing", callbackFactory.getMethod("const_missing", IRubyObject.class));
        moduleClass.defineMethod("const_set", callbackFactory.getMethod("const_set", IRubyObject.class, IRubyObject.class));
        moduleClass.defineMethod("constants", callbackFactory.getMethod("constants"));
        moduleClass.defineMethod("dup", callbackFactory.getMethod("dup"));
        moduleClass.defineMethod("included_modules", callbackFactory.getMethod("included_modules"));
        moduleClass.defineMethod("initialize", callbackFactory.getOptMethod("initialize"));
        moduleClass.defineMethod("instance_method", callbackFactory.getMethod("instance_method", IRubyObject.class));
        moduleClass.defineMethod("instance_methods", callbackFactory.getOptMethod("instance_methods"));
        moduleClass.defineMethod("method_defined?", callbackFactory.getMethod("method_defined", IRubyObject.class));
        moduleClass.defineMethod("module_eval", module_eval);
        moduleClass.defineMethod("name", callbackFactory.getMethod("name"));
        moduleClass.defineMethod("private_class_method", callbackFactory.getOptMethod("private_class_method"));
        moduleClass.defineMethod("private_instance_methods", callbackFactory.getOptMethod("private_instance_methods"));
        moduleClass.defineMethod("protected_instance_methods", callbackFactory.getOptMethod("protected_instance_methods"));
        moduleClass.defineMethod("public_class_method", callbackFactory.getOptMethod("public_class_method"));
        moduleClass.defineMethod("public_instance_methods", callbackFactory.getOptMethod("public_instance_methods"));
        moduleClass.defineMethod("to_s",  callbackFactory.getMethod("to_s"));

        moduleClass.definePrivateMethod("alias_method", callbackFactory.getMethod("alias_method", IRubyObject.class, IRubyObject.class));
        moduleClass.definePrivateMethod("append_features", callbackFactory.getMethod("append_features", RubyModule.class));
        moduleClass.definePrivateMethod("attr", callbackFactory.getOptMethod("attr", IRubyObject.class));
        moduleClass.definePrivateMethod("attr_reader", callbackFactory.getOptMethod("attr_reader"));
        moduleClass.definePrivateMethod("attr_writer", callbackFactory.getOptMethod("attr_writer"));
        moduleClass.definePrivateMethod("attr_accessor", callbackFactory.getOptMethod("attr_accessor"));
        moduleClass.definePrivateMethod("define_method", callbackFactory.getOptMethod("define_method"));
        moduleClass.definePrivateMethod("extend_object", callbackFactory.getMethod("extend_object", IRubyObject.class));
        moduleClass.definePrivateMethod("include", callbackFactory.getOptMethod("include"));
        moduleClass.definePrivateMethod("method_added", callbackFactory.getNilMethod(1));
        moduleClass.definePrivateMethod("module_function", callbackFactory.getOptMethod("module_function"));
        moduleClass.definePrivateMethod("public", callbackFactory.getOptMethod("rbPublic"));
        moduleClass.definePrivateMethod("protected", callbackFactory.getOptMethod("rbProtected"));
        moduleClass.definePrivateMethod("private", callbackFactory.getOptMethod("rbPrivate"));
        moduleClass.definePrivateMethod("remove_class_variable", callbackFactory.getMethod("remove_class_variable", IRubyObject.class));
        moduleClass.definePrivateMethod("remove_const", callbackFactory.getMethod("remove_const", IRubyObject.class));
        moduleClass.definePrivateMethod("remove_method", callbackFactory.getMethod("remove_method", IRubyObject.class));
        moduleClass.definePrivateMethod("undef_method", callbackFactory.getMethod("undef_method", IRubyObject.class));

        moduleClass.defineSingletonMethod("new", callbackFactory.getSingletonMethod("newModule"));
        moduleClass.defineSingletonMethod("nesting", callbackFactory.getSingletonMethod("nesting"));
    }

    public String getBaseName() {
        return classId;
    }
    
    public void setBaseName(String name) {
        classId = name;
    }
    
    /** classname
     *
     */
    public String getName() {
        RubyModule module = this;
        while (module.isIncluded() || module.isSingleton()) {
            module = module.getSuperClass();
        }

        if (classId == null) {
            return "<" + (isClass() ? "Class" : "Module") + " 01x" + 
            Integer.toHexString(System.identityHashCode(this)) + ">";
        }
        
        StringBuffer result = new StringBuffer(classId);
        RubyClass objectClass = getRuntime().getClasses().getObjectClass();
        
        for (RubyModule current = this.parentModule; current != objectClass && current != this; current = current.parentModule) {
            assert current != null;
            result.insert(0, "::").insert(0, current.classId);
        }

        return result.toString();
    }

    /** include_class_new
     *
     */
    public IncludedModuleWrapper newIncludeClass(RubyClass superClass) {
        return new IncludedModuleWrapper(getRuntime(), superClass, this);
    }
    
    private RubyModule getModuleWithInstanceVar(String name) {
        for (RubyModule p = this; p != null; p = p.getSuperClass()) {
        	IRubyObject variable = p.getInstanceVariable(name);
            if (variable != null) {
                return p;
            }
        }
        return null;
    }

    /** rb_cvar_set
     *
     */
    public void setClassVar(String name, IRubyObject value) {
        RubyModule module = getModuleWithInstanceVar(name);
        
        if (module != null) {
            module.setInstanceVariable(name, value, CVAR_TAINT_ERROR, 
                    CVAR_FREEZE_ERROR);
            return;
        }

        // If we cannot find the class var, then create it in the super class.
        setInstanceVariable(name, value, CVAR_TAINT_ERROR, 
                CVAR_FREEZE_ERROR);
    }

    /** rb_cvar_get
     *
     */
    public IRubyObject getClassVar(String name) {
        RubyModule module = getModuleWithInstanceVar(name);
        
        if (module != null) {
        	IRubyObject variable = module.getInstanceVariable(name); 
        	
            return variable == null ? getRuntime().getNil() : variable;
        }
        
        throw new NameError(getRuntime(), "uninitialized class variable " + name + " in " + getName());
    }

    /** rb_cvar_defined
     *
     */
    public boolean isClassVarDefined(String name) {
        return getModuleWithInstanceVar(name) != null;
    }

    public IRubyObject setConstant(String name, IRubyObject value) {
        IRubyObject result = setInstanceVariable(name, value, "Insecure: can't set constant", 
                "class/module");
        if (value instanceof RubyModule) {
            RubyModule module = (RubyModule)value;
            if (module.getBaseName() == null) {
                module.setBaseName(name);
            }
        }
        return result;
    }

    public IRubyObject getConstant(String name) {
    	return getConstant(name, true);
    }
    
    public IRubyObject getConstant(String name, boolean invokeConstMissing) {
    	// special case code for "NIL"
    	if (name.equals("NIL")) {
    		return getRuntime().getNil();
    	}
    	
    	// First look for constants in module hierachy
    	for (RubyModule p = this; p != p.parentModule; p = p.parentModule) {
            IRubyObject var = p.getInstanceVariable(name);
            if (var != null) {
                return var;
            }
        }

    	// Above loop does not check top of module hierchy
        IRubyObject var = getRuntime().getClasses().getObjectClass().getInstanceVariable(name);
        if (var != null) {
        	return var;
        }

        // Second look for constants in the inheritance hierachy
        for (RubyModule p = this; p != null; p = p.getSuperClass()) {
        	var = p.getInstanceVariable(name);
            if (var != null) {
                return var;
            }
        }

        // look for constants in module stack
        ArrayStack moduleStack = (ArrayStack)getRuntime().getCurrentContext().getClassStack().clone();
        for (RubyModule p = (RubyModule)moduleStack.peek(); !moduleStack.empty(); p = (RubyModule)moduleStack.pop()) {
        	var = p.getInstanceVariable(name);
            if (var != null) {
                return var;
            }
        }        	

        // Lastly look for constants in top constant
        var = getRuntime().getTopConstant(name);
        if (var != null) {
        	return var;
        }
        
        if (invokeConstMissing) {
        	return callMethod("const_missing", 
        			RubySymbol.newSymbol(getRuntime(), name));
        }
        return null;
    }

    public IRubyObject const_missing(IRubyObject name) {
        /* Uninitialized constant */
        if (this != getRuntime().getClasses().getObjectClass()) {
            throw new NameError(getRuntime(), "uninitialized constant " + name.asSymbol() + " at " + getName());
        } 

        throw new NameError(getRuntime(), "uninitialized constant " + name.asSymbol());
    }

    /** 
     * Include a new module in this module or class.
     */
    public void includeModule(IRubyObject arg) {
        testFrozen("module");
        if (!isTaint()) {
            getRuntime().secure(4);
        }

        if (!(arg instanceof RubyModule)) {
            throw getRuntime().newTypeError("Wrong argument type " + arg.getMetaClass().getName() + " (expected Module).");
        }

        RubyModule module = (RubyModule) arg;
        Map moduleMethods = module.getMethods();

        // Make sure the module we include does not already exist 
        for (RubyModule p = this; p != null; p = p.getSuperClass()) {
        	// XXXEnebo - Lame equality check (cause: IncludedModule?)
        	if (p.getMethods() == moduleMethods) {
        		return;
        	}
        }
        
        // Include new module
        setSuperClass(module.newIncludeClass(getSuperClass()));
        
        // Try to include all included modules from module just added 
        for (RubyModule p = module.getSuperClass(); p != null; 
        	p = p.getSuperClass()) {
        	includeModule(p);
        }

        clearMethodCache();
    }

    /** rb_add_method
     *
     */
    public void addMethod(String name, ICallable method) {
        if (this == getRuntime().getClasses().getObjectClass()) {
            getRuntime().secure(4);
        }

        if (getRuntime().getSafeLevel() >= 4 && !isTaint()) {
            throw new SecurityError(getRuntime(), "Insecure: can't define method");
        }
        testFrozen("class/module");

        // Clear cache for any instance methods
        methodCache.remove(name);
        getMethods().put(name, method);
        clearMethodCache(name);
    }

    public void defineMethod(String name, Callback method) {
        Visibility visibility = name.equals("initialize") ? 
        		Visibility.PRIVATE : Visibility.PUBLIC;

        addMethod(name, new CallbackMethod(method, visibility));
    }

    public void definePrivateMethod(String name, Callback method) {
        addMethod(name, new CallbackMethod(method, Visibility.PRIVATE));
    }

    public void undefineMethod(String name) {
        addMethod(name, UndefinedMethod.getInstance());
    }

    /** rb_undef
     *
     */
    public void undef(String name) {
        Ruby runtime = getRuntime();
        if (this == runtime.getClasses().getObjectClass()) {
            runtime.secure(4);
        }
        if (runtime.getSafeLevel() >= 4 && !isTaint()) {
            throw new SecurityException("Insecure: can't undef");
        }
        testFrozen("module");
        if (name.equals("__id__") || name.equals("__send__")) {
            /*rb_warn("undefining `%s' may cause serious problem",
                     rb_id2name( id ) );*/
        }
        ICallable method = searchMethod(name);
        if (method.isUndefined()) {
            String s0 = " class";
            RubyModule c = this;

            if (c.isSingleton()) {
                IRubyObject obj = getInstanceVariable("__attached__");

                if (obj != null && obj instanceof RubyModule) {
                    c = (RubyModule) obj;
                    s0 = "";
                }
            } else if (c.isModule()) {
                s0 = " module";
            }

            throw new NameError(runtime, "Undefined method " + name + " for" + s0 + " '" + c.getName() + "'");
        }
        addMethod(name, UndefinedMethod.getInstance());
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

    public IRubyObject getConstantAtOrConstantMissing(String name) {
        IRubyObject constant = getConstantAt(name);

        if (constant != null) {
             return constant;
        }
        
        return callMethod("const_missing", RubySymbol.newSymbol(getRuntime(), name));
    }

    public IRubyObject getConstantAt(String name) {
    	IRubyObject constant = getInstanceVariable(name);
    	
    	if (constant != null) {
    		return constant;
    	} 

    	for (RubyModule p = getSuperClass(); p != null; p = p.getSuperClass()) {
    	    constant = p.getConstantAt(name);
    	    if (constant != null)
    	        return constant;
    	}
    	
    	if (this == getRuntime().getClasses().getObjectClass()) {
    		return getConstant(name, false);
    	}
    	return null;
    }

    /** search_method
     *
     */
    public ICallable searchMethod(String name) {
        ICallable method = (ICallable) getMethods().get(name);
        if (method != null) {
            method.setImplementationClass(this);
            return method;
        }
        
        if (getSuperClass() == null) {
        	return UndefinedMethod.getInstance();
        }
        
        return getSuperClass().searchMethod(name);
    }

    public Visibility getMethodVisibility(String name) {
        return getMethodBodyCached(name).getVisibility();
    }

    protected CacheEntry getMethodBodyCached(String name) {
        CacheEntry result = (CacheEntry) methodCache.get(name);
        if (result != null) {
            return result;
        }
        name = name.intern();
        ICallable method = searchMethod(name);
        if (method.isUndefined()) {
            CacheEntry undefinedEntry = CacheEntry.createUndefined(name, this);
            methodCache.put(name, undefinedEntry);
            return undefinedEntry;
        }
        result = new CacheEntry(name, this);
        method.initializeCacheEntry(result);
        // Be careful not to store cache entries that contain classes from super classes of singleton classes
        // because they cannot be cleared in addMethod() and would therefore keep old method definition alive.
        if (result.getOrigin() == this || !(this instanceof MetaClass)) {
        	methodCache.put(name, result);
        }
        return result;
    }

    public static void clearMethodCache(Ruby runtime) {
        Iterator iter = runtime.getClasses().getTopLevelClassMap().values().iterator();
        while (iter.hasNext()) {
            ((RubyModule) iter.next()).methodCache.clear();
        }
        
        iter = runtime.getClasses().getNonTopLevelClassMap().values().iterator();
        while (iter.hasNext()) {
            ((RubyModule) iter.next()).methodCache.clear();
        }
    }

    public static void clearMethodCache(Ruby runtime, String methodName) {
        Iterator iter = runtime.getClasses().getTopLevelClassMap().values().iterator();
        while (iter.hasNext()) {
            RubyModule module = (RubyModule) iter.next();
            module.methodCache.remove(methodName);
        }
        
        iter = runtime.getClasses().getNonTopLevelClassMap().values().iterator();
        while (iter.hasNext()) {
            RubyModule module = (RubyModule) iter.next();
            module.methodCache.remove(methodName);
        }
    }

    private void clearMethodCache() {
        clearMethodCache(getRuntime());
    }

    private void clearMethodCache(String methodName) {
        clearMethodCache(getRuntime(), methodName);
    }

    /** rb_call
     *
     */
    public final IRubyObject call(IRubyObject recv, String name, IRubyObject[] args, CallType callType) {
        if (args == null) {
            args = IRubyObject.NULL_ARRAY;
        }
        CacheEntry entry = getMethodBodyCached(name);

        final LastCallStatus lastCallStatus = getRuntime().getLastCallStatus();
        if (! entry.isDefined()) {
            callType.registerCallStatus(lastCallStatus, name);
            return callMethodMissing(recv, name, args);
        }

        RubyModule klass = entry.getOrigin();
        name = entry.getOriginalName();
        ICallable method = entry.getMethod();

        if (!name.equals("method_missing")) {
            if (method.getVisibility().isPrivate() && callType.isNormal()) {
                lastCallStatus.setPrivate();
                return callMethodMissing(recv, name, args);
            } else if (method.getVisibility().isProtected()) {
                RubyModule defined = klass;
                while (defined.isIncluded()) {
                    defined = defined.getMetaClass();
                }
                if (!getRuntime().getCurrentFrame().getSelf().isKindOf(defined)) {
                    lastCallStatus.setProtected();
                    return callMethodMissing(recv, name, args);
                }
            }
        }

        return klass.call0(recv, name, args, method, false);
    }

    private IRubyObject callMethodMissing(IRubyObject receiver, String name, IRubyObject[] args) {
        if (name == "method_missing") {
            getRuntime().getFrameStack().push();
            try {
                return receiver.method_missing(args);
            } finally {
                getRuntime().getFrameStack().pop();
            }
        }

        IRubyObject[] newArgs = new IRubyObject[args.length + 1];
        System.arraycopy(args, 0, newArgs, 1, args.length);
        newArgs[0] = RubySymbol.newSymbol(getRuntime(), name);

        return receiver.callMethod("method_missing", newArgs);
    }

    /** rb_call0
     *
     */
    public final IRubyObject call0(
        IRubyObject recv,
        String name,
        IRubyObject[] args,
        ICallable method,
        boolean noSuper) {
        ThreadContext context = getRuntime().getCurrentContext();
        context.getIterStack().push(context.getCurrentIter().isPre() ? Iter.ITER_CUR : Iter.ITER_NOT);

        context.getFrameStack().push();
        context.getCurrentFrame().setLastFunc(name);
        context.getCurrentFrame().setLastClass(noSuper ? null : this);
        context.getCurrentFrame().setSelf(recv);
        context.getCurrentFrame().setArgs(args);

        try {
            return method.call(getRuntime(), recv, name, args, noSuper);
        } finally {
            context.getFrameStack().pop();
            context.getIterStack().pop();
        }
    }

    /** rb_alias
     *
     */
    public void aliasMethod(String name, String oldName) {
        testFrozen("module");
        if (oldName.equals(name)) {
            return;
        }
        if (this == getRuntime().getClasses().getObjectClass()) {
            getRuntime().secure(4);
        }
        ICallable method = searchMethod(oldName);
        if (method.isUndefined()) {
            if (isModule()) {
                method = getRuntime().getClasses().getObjectClass().searchMethod(oldName);
            }
            if (method.isUndefined()) {
                throw new NameError(getRuntime(),
                                    "undefined method '" + name + "' for " +
                                    (isModule() ? "module" : "class") + " '" +
                                    getName() + "'");
            }
        }
        getMethods().put(name, new AliasMethod(method, oldName));
        clearMethodCache(name);
    }

    /** remove_method
     *
     */
    public void removeMethod(String name) {
        if (this == getRuntime().getClasses().getObjectClass()) {
            getRuntime().secure(4);
        }
        if (getRuntime().getSafeLevel() >= 4 && !isTaint()) {
            throw new SecurityError(getRuntime(), "Insecure: can't remove method");
        }
        testFrozen("class/module");

        if (getMethods().remove(name) == null) {
            throw new NameError(getRuntime(), "method '" + name + "' not defined in " + getName());
        }

        clearMethodCache(name);
    }

    public RubyClass defineOrGetClassUnder(String name, RubyClass superClass) {
        IRubyObject type = getConstantAt(name);
        
        if (type == null) {
            return (RubyClass) setConstant(name, 
            		getRuntime().defineClassUnder(name, superClass, this)); 
        }

        if (!(type instanceof RubyClass)) {
        	throw getRuntime().newTypeError(name + " is not a class.");
        }
            
        return (RubyClass) type;
    }
    
    /** rb_define_class_under
     *
     */
    public RubyClass defineClassUnder(String name, RubyClass superClass) {
    	IRubyObject type = getConstantAt(name);
    	
    	if (type == null) {
            return (RubyClass) setConstant(name, 
            		getRuntime().defineClassUnder(name, superClass, this)); 
    	}

    	if (!(type instanceof RubyClass)) {
    		throw getRuntime().newTypeError(name + " is not a class.");
        } else if (((RubyClass) type).getSuperClass().getRealClass() != superClass) {
        	throw new NameError(getRuntime(), name + " is already defined.");
        } 
            
    	return (RubyClass) type;
    }

    public RubyModule defineModuleUnder(String name) {
        IRubyObject type = getConstantAt(name);
        
        if (type == null) {
            return (RubyModule) setConstant(name, 
            		getRuntime().defineModuleUnder(name, this)); 
        }

        if (!(type instanceof RubyModule)) {
        	throw getRuntime().newTypeError(name + " is not a module.");
        } 

        return (RubyModule) type;
    }

    /** rb_define_const
     *
     */
    public void defineConstant(String name, IRubyObject value) {
        assert value != null;

        if (this == getRuntime().getClasses().getClassClass()) {
            getRuntime().secure(4);
        }

        if (!IdUtil.isConstant(name)) {
            throw new NameError(getRuntime(), "bad constant name " + name);
        }

        setConstant(name, value);
    }

    /** rb_mod_remove_cvar
     *
     */
    public IRubyObject removeCvar(IRubyObject name) { // Wrong Parameter ?
        if (!IdUtil.isClassVariable(name.asSymbol())) {
            throw new NameError(getRuntime(), "wrong class variable name " + name.asSymbol());
        }

        if (!isTaint() && getRuntime().getSafeLevel() >= 4) {
            throw new SecurityError(getRuntime(), "Insecure: can't remove class variable");
        }
        testFrozen("class/module");

        IRubyObject value = removeInstanceVariable(name.asSymbol());

        if (value != null) {
            return value;
        }

        if (isClassVarDefined(name.asSymbol())) {
            throw new NameError(getRuntime(), "cannot remove " + name.asSymbol() + " for " + getName());
        }

        throw new NameError(getRuntime(), "class variable " + name.asSymbol() + " not defined for " + getName());
    }

    private void addAccessor(String name, boolean readable, boolean writeable) {
        Visibility attributeScope = getRuntime().getCurrentVisibility();
        if (attributeScope.isPrivate()) {
            //FIXME warning
        } else if (attributeScope.isModuleFunction()) {
            attributeScope = Visibility.PRIVATE;
            // FIXME warning
        }
        String variableName = "@" + name;
        if (readable) {
            addMethod(name, new EvaluateMethod(new InstVarNode(getRuntime().getPosition(), variableName), attributeScope));
            callMethod("method_added", RubySymbol.newSymbol(getRuntime(), name));
        }
        if (writeable) {
            name = name + "=";
            addMethod(name, new EvaluateMethod(new AttrSetNode(getRuntime().getPosition(), variableName), attributeScope));
            callMethod("method_added", RubySymbol.newSymbol(getRuntime(), name));
        }
    }

    /** set_method_visibility
     *
     */
    public void setMethodVisibility(IRubyObject[] methods, Visibility visibility) {
        if (getRuntime().getSafeLevel() >= 4 && !isTaint()) {
            throw new SecurityError(getRuntime(), "Insecure: can't change method visibility");
        }

        for (int i = 0; i < methods.length; i++) {
            exportMethod(methods[i].asSymbol(), visibility);
        }
    }

    /** rb_export_method
     *
     */
    public void exportMethod(String name, Visibility visibility) {
        if (this == getRuntime().getClasses().getObjectClass()) {
            getRuntime().secure(4);
        }

        ICallable method = searchMethod(name);

        if (method.isUndefined() && isModule()) {
            method = getRuntime().getClasses().getObjectClass().searchMethod(name);
        }

        if (method.isUndefined()) {
            throw new NameError(
                getRuntime(),
                "undefined method '" + name + "' for " + (isModule() ? "module" : "class") + " '" + getName() + "'");
        }

        if (method.getVisibility() != visibility) {
            if (this == method.getImplementationClass()) {
                method.setVisibility(visibility);
            } else {
                ICallable superCall = new EvaluateMethod(new ZSuperNode(getRuntime().getPosition()), visibility);
                addMethod(name, superCall);
            }
        }
    }

    /**
     * MRI: rb_method_boundp
     *
     */
    public boolean isMethodBound(String name, boolean checkVisibility) {
        CacheEntry entry = (CacheEntry) methodCache.get(name);
        if (entry == null) {
            entry = getMethodBodyCached(name);
        }
        if (entry.isDefined()) {
            return !(checkVisibility && entry.getVisibility().isPrivate());
        }
        return false;
    }

    public IRubyObject newMethod(IRubyObject receiver, String name, boolean bound) {
        CacheEntry ent = getMethodBodyCached(name);
        if (! ent.isDefined()) {
            throw new NameError(getRuntime(), "undefined method `" + name + 
                "' for class `" + this.getName() + "'");
        }

        while (ent.getMethod() instanceof EvaluateMethod
            && ((EvaluateMethod) ent.getMethod()).getNode() instanceof ZSuperNode) {
            ent = ent.getOrigin().getSuperClass().getMethodBodyCached(ent.getOriginalName());
            if (! ent.isDefined()) {
                // printUndef();
                return getRuntime().getNil();
            }
        }

        RubyMethod method = null;
        if (bound) {
            method = RubyMethod.newMethod(ent.getOrigin(), ent.getOriginalName(), this, name, ent.getMethod(), receiver);
        } else {
            method = RubyUnboundMethod.newUnboundMethod(ent.getOrigin(), ent.getOriginalName(), this, name, ent.getMethod());
        }
        method.infectBy(this);

        return method;
    }

    // What is argument 1 for in this method?
    public IRubyObject define_method(IRubyObject[] args) {
        if (args.length < 1 || args.length > 2) {
            throw getRuntime().newArgumentError("wrong # of arguments(" + args.length + " for 1)");
        }

        String name = args[0].asSymbol();
        IRubyObject body;
        ICallable newMethod;
        Visibility visibility = getRuntime().getCurrentVisibility();

        if (visibility.isModuleFunction()) {
            visibility = Visibility.PRIVATE;
        }
        
        if (args.length == 1) {
            body = getRuntime().newProc();
            newMethod = new ProcMethod((RubyProc)body, visibility);
        } else if (args[1].isKindOf(getRuntime().getClasses().getMethodClass())) {
            body = args[1];
            newMethod = new MethodMethod(((RubyMethod)body).unbind(), visibility);
        } else if (args[1].isKindOf(getRuntime().getClasses().getProcClass())) {
            body = args[1];
            newMethod = new ProcMethod((RubyProc)body, visibility);
        } else {
            throw getRuntime().newTypeError("wrong argument type " + args[0].getType().getName() + " (expected Proc/RubyMethod)");
        }

        addMethod(name, newMethod);

        RubySymbol symbol = RubySymbol.newSymbol(getRuntime(), name);
        if (getRuntime().getCurrentVisibility().isModuleFunction()) {
            getSingletonClass().addMethod(name, new WrapperCallable(newMethod, Visibility.PUBLIC));
            callMethod("singleton_method_added", symbol);
        }

        callMethod("method_added", symbol);

        return body;
    }

    public IRubyObject executeUnder(Callback method, IRubyObject[] args) {
        ThreadContext threadContext = getRuntime().getCurrentContext();

        threadContext.pushClass(this);

        Frame frame = threadContext.getCurrentFrame();
        threadContext.getFrameStack().push();
        threadContext.getCurrentFrame().setLastFunc(frame.getLastFunc());
        threadContext.getCurrentFrame().setLastClass(frame.getLastClass());
        threadContext.getCurrentFrame().setArgs(frame.getArgs());

        try {
            return method.execute(this, args);
        } finally {
            threadContext.getFrameStack().pop();
            threadContext.popClass();
        }
    }

    // Methods of the Module Class (rb_mod_*):

    public static RubyModule newModule(Ruby runtime, String name) {
        return newModule(runtime, name, null);
    }

    public static RubyModule newModule(Ruby runtime, String name, RubyModule parentModule) {
        return new RubyModule(runtime, runtime.getClasses().getModuleClass(), null, parentModule, name);
    }
    
    /** rb_mod_name
     *
     */
    public RubyString name() {
        return getRuntime().newString(getName());
    }

    /** rb_mod_class_variables
     *
     */
    public RubyArray class_variables() {
        RubyArray ary = getRuntime().newArray();

        for (RubyModule p = this; p != null; p = p.getSuperClass()) {
            for (Iterator iter = p.instanceVariableNames(); iter.hasNext();) {
                String id = (String) iter.next();
                if (IdUtil.isClassVariable(id)) {
                    RubyString kval = getRuntime().newString(id);
                    if (!ary.includes(kval)) {
                        ary.append(kval);
                    }
                }
            }
        }
        return ary;
    }

    /** rb_mod_clone
     *
     */
    public IRubyObject rbClone() {
        RubyModule clone = (RubyModule) super.rbClone();
        Map cloneMethods = clone.getMethods();
        
        for (Iterator iter = getMethods().entrySet().iterator(); 
        	 iter.hasNext();) {
        	Map.Entry e = (Map.Entry) iter.next();

        	cloneMethods.put(e.getKey(), ((ICallable) e.getValue()).dup()); 
        }
        return clone;
    }
    
    protected IRubyObject doClone() {
    	return RubyModule.newModule(getRuntime(), classId, parentModule);
    }

    /** rb_mod_dup
     *
     */
    public IRubyObject dup() {
        RubyModule dup = (RubyModule) rbClone();
        dup.setMetaClass(getMetaClass());
        dup.setFrozen(false);
        // +++ jpetersen
        // dup.setSingleton(isSingleton());
        // --- jpetersen

        return dup;
    }

    /** rb_mod_included_modules
     *
     */
    public RubyArray included_modules() {
        RubyArray ary = getRuntime().newArray();

        for (RubyModule p = getSuperClass(); p != null; p = p.getSuperClass()) {
            if (p.isIncluded()) {
                ary.append(((IncludedModuleWrapper) p).getDelegate());
            }
        }

        return ary;
    }

    /** rb_mod_ancestors
     *
     */
    public RubyArray ancestors() {
        RubyArray ary = getRuntime().newArray();

        for (RubyModule p = this; p != null; p = p.getSuperClass()) {
            if (p.isSingleton()) {
                continue;
            }

            if (p.isIncluded()) {
                ary.append(((IncludedModuleWrapper) p).getDelegate());
            } else {
                ary.append(p);
            }
        }

        return ary;
    }

    /** rb_mod_to_s
     *
     */
    public RubyString to_s() {
        return getRuntime().newString(getName());
    }

    /** rb_mod_eqq
     *
     */
    public RubyBoolean op_eqq(IRubyObject obj) {
        return getRuntime().newBoolean(obj.isKindOf(this));
    }

    /** rb_mod_le
     *
     */
    public RubyBoolean op_le(IRubyObject obj) {
        if (!(obj instanceof RubyModule)) {
            throw getRuntime().newTypeError("compared with non class/module");
        }

        for (RubyModule p = this; p != null; p = p.getSuperClass()) { 
            if (p.getMethods() == ((RubyModule) obj).getMethods()) {
                return getRuntime().getTrue();
            }
        }

        return getRuntime().getFalse();
    }

    /** rb_mod_lt
     *
     */
    public RubyBoolean op_lt(IRubyObject obj) {
    	return obj == this ? getRuntime().getFalse() : op_le(obj); 
    }

    /** rb_mod_ge
     *
     */
    public RubyBoolean op_ge(IRubyObject obj) {
        if (!(obj instanceof RubyModule)) {
            throw getRuntime().newTypeError("compared with non class/module");
        }

        return ((RubyModule) obj).op_le(this);
    }

    /** rb_mod_gt
     *
     */
    public RubyBoolean op_gt(IRubyObject obj) {
        return this == obj ? getRuntime().getFalse() : op_ge(obj);
    }

    /** rb_mod_cmp
     *
     */
    public RubyFixnum op_cmp(IRubyObject obj) {
        if (this == obj) {
            return getRuntime().newFixnum(0);
        }

        if (!(obj instanceof RubyModule)) {
            throw getRuntime().newTypeError(
                "<=> requires Class or Module (" + getMetaClass().getName() + " given)");
        }

        return getRuntime().newFixnum(
                op_le(obj).isTrue() ? -1 : 1);
    }

    /** rb_mod_initialize
     *
     */
    public IRubyObject initialize(IRubyObject[] args) {
        return getRuntime().getNil();
    }

    public static RubyModule newModule(IRubyObject recv) {
        RubyModule mod = RubyModule.newModule(recv.getRuntime(), null);
        mod.setMetaClass((RubyClass) recv);
        recv.getRuntime().getClasses().getModuleClass().callInit(null);
        return mod;
    }

    /** Return an array of nested modules or classes.
     *
     * rb_mod_nesting
     *
     */
    public static RubyArray nesting(IRubyObject recv) {
        RubyModule objectClass = recv.getRuntime().getClasses().getObjectClass();
        RubyModule recvModule = recv.getRuntime().getCurrentContext().getRubyClass();
        RubyArray result = recv.getRuntime().newArray();
        
        for (RubyModule current = recvModule; current != objectClass;
        	current = current.parentModule) {
            result.append(current);
        }
        
        return result;
    }

    /** rb_mod_attr
     *
     */
    public IRubyObject attr(IRubyObject symbol, IRubyObject[] args) {
        boolean writeable = false;
        if (args.length > 0) {
            writeable = args[0].isTrue();
        }

        addAccessor(symbol.asSymbol(), true, writeable);

        return getRuntime().getNil();
    }

    /** rb_mod_attr_reader
     *
     */
    public IRubyObject attr_reader(IRubyObject[] args) {
        for (int i = 0; i < args.length; i++) {
            addAccessor(args[i].asSymbol(), true, false);
        }

        return getRuntime().getNil();
    }

    /** rb_mod_attr_writer
     *
     */
    public IRubyObject attr_writer(IRubyObject[] args) {
        for (int i = 0; i < args.length; i++) {
            addAccessor(args[i].asSymbol(), false, true);
        }

        return getRuntime().getNil();
    }

    /** rb_mod_attr_accessor
     *
     */
    public IRubyObject attr_accessor(IRubyObject[] args) {
        for (int i = 0; i < args.length; i++) {
            addAccessor(args[i].asSymbol(), true, true);
        }

        return getRuntime().getNil();
    }

    /** rb_mod_const_get
     *
     */
    public IRubyObject const_get(IRubyObject symbol) {
        String name = symbol.asSymbol();

        if (!IdUtil.isConstant(name)) {
            throw new NameError(getRuntime(), "wrong constant name " + name);
        }

        return getConstant(name);
    }

    /** rb_mod_const_set
     *
     */
    public IRubyObject const_set(IRubyObject symbol, IRubyObject value) {
        String name = symbol.asSymbol();

        if (!IdUtil.isConstant(name)) {
            throw new NameError(getRuntime(), "wrong constant name " + name);
        }

        return setConstant(name, value); 
    }

    /** rb_mod_const_defined
     *
     */
    public RubyBoolean const_defined(IRubyObject symbol) {
        String name = symbol.asSymbol();

        if (!IdUtil.isConstant(name)) {
            throw new NameError(getRuntime(), "wrong constant name " + name);
        }

        return getRuntime().newBoolean(getConstant(name, false) != null);
    }

    private RubyArray instance_methods(IRubyObject[] args, final Visibility visibility) {
        boolean includeSuper = args.length > 0 ? args[0].isTrue() : true;
        RubyArray ary = getRuntime().newArray();

        for (RubyModule p = this; p != null; p = p.getSuperClass()) {
            for (Iterator iter = p.getMethods().entrySet().iterator();
            	iter.hasNext();) {
                Map.Entry entry = (Map.Entry) iter.next();
                ICallable method = (ICallable) entry.getValue();

                if (method.getVisibility().is(visibility) &&
                        !method.isUndefined()) {
                    RubyString name = getRuntime().newString(
                    	(String) entry.getKey());

                    if (!ary.includes(name)) {
                    	ary.append(name);
                    }
                } 
            }
            if (!includeSuper) {
                break;
            }
        }

        return ary;
    }

    public RubyArray instance_methods(IRubyObject[] args) {
        return instance_methods(args, Visibility.PUBLIC_PROTECTED);
    }

    public RubyArray public_instance_methods(IRubyObject[] args) {
    	return instance_methods(args, Visibility.PUBLIC);
    }

    public IRubyObject instance_method(IRubyObject symbol) {
        return newMethod(null, symbol.asSymbol(), false);
    }

    /** rb_class_protected_instance_methods
     *
     */
    public RubyArray protected_instance_methods(IRubyObject[] args) {
        return instance_methods(args, Visibility.PROTECTED);
    }

    /** rb_class_private_instance_methods
     *
     */
    public RubyArray private_instance_methods(IRubyObject[] args) {
        return instance_methods(args, Visibility.PRIVATE);
    }

    /** rb_mod_constants
     *
     */
    public RubyArray constants() {
        ArrayList constantNames = new ArrayList();
        RubyModule objectClass = getRuntime().getClasses().getObjectClass();
        
        if (getRuntime().getClasses().getModuleClass() == this) {
            for (Iterator iter = getRuntime().getClasses().nameIterator(); 
            	iter.hasNext();) {
                String name = (String) iter.next();
                if (IdUtil.isConstant(name)) {
                    constantNames.add(getRuntime().newString(name));
                }
            }
            
            for (Iterator vars = objectClass.instanceVariableNames(); 
            	vars.hasNext();) {
                String name = (String) vars.next();
                if (IdUtil.isConstant(name)) {
                    constantNames.add(getRuntime().newString(name));
                }
            }
            
            return getRuntime().newArray(constantNames);
        }

        for (RubyModule p = this; p != null; p = p.getSuperClass()) {
            if (objectClass == p) {
                continue;
            }
            
            for (Iterator vars = p.instanceVariableNames(); vars.hasNext();) {
                String name = (String) vars.next();
                if (IdUtil.isConstant(name)) {
                    constantNames.add(getRuntime().newString(name));
                }
            }
        }
        
        return getRuntime().newArray(constantNames);
    }

    /** rb_mod_remove_cvar
     *
     */
    public IRubyObject remove_class_variable(IRubyObject name) {
        String id = name.asSymbol();

        if (!IdUtil.isClassVariable(id)) {
            throw new NameError(getRuntime(), "wrong class variable name " + id);
        }
        if (!isTaint() && getRuntime().getSafeLevel() >= 4) {
            throw new SecurityError(getRuntime(), "Insecure: can't remove class variable");
        }
        testFrozen("class/module");

        IRubyObject variable = removeInstanceVariable(id); 
        if (variable != null) {
            return variable;
        }

        if (isClassVarDefined(id)) {
            throw new NameError(getRuntime(), "cannot remove " + id + " for " + getName());
        }
        throw new NameError(getRuntime(), "class variable " + id + " not defined for " + getName());
    }
    
    public IRubyObject remove_const(IRubyObject name) {
        String id = name.asSymbol();

        if (!IdUtil.isConstant(id)) {
            throw new NameError(getRuntime(), "wrong constant name " + id);
        }
        if (!isTaint() && getRuntime().getSafeLevel() >= 4) {
            throw new SecurityError(getRuntime(), "Insecure: can't remove class variable");
        }
        testFrozen("class/module");

        IRubyObject variable = getInstanceVariable(id);
        if (variable != null) {
            return removeInstanceVariable(id);
        }

        if (isClassVarDefined(id)) {
            throw new NameError(getRuntime(), "cannot remove " + id + " for " + getName());
        }
        throw new NameError(getRuntime(), "constant " + id + " not defined for " + getName());
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
    public IRubyObject extend_object(IRubyObject obj) {
        obj.extendObject(this);
        return obj;
    }

    /** rb_mod_include
     *
     */
    public RubyModule include(IRubyObject[] modules) {
        for (int i = 0; i < modules.length; i++) {
            modules[i].callMethod("append_features", this);
        }

        return this;
    }

    private void setVisibility(IRubyObject[] args, Visibility visibility) {
        if (getRuntime().getSafeLevel() >= 4 && !isTaint()) {
            throw new SecurityError(getRuntime(), "Insecure: can't change method visibility");
        }

        if (args.length == 0) {
            getRuntime().setCurrentVisibility(visibility);
        } else {
            setMethodVisibility(args, visibility);
        }
    }
    
    /** rb_mod_public
     *
     */
    public RubyModule rbPublic(IRubyObject[] args) {
        setVisibility(args, Visibility.PUBLIC);
        return this;
    }

    /** rb_mod_protected
     *
     */
    public RubyModule rbProtected(IRubyObject[] args) {
        setVisibility(args, Visibility.PROTECTED);
        return this;
    }

    /** rb_mod_private
     *
     */
    public RubyModule rbPrivate(IRubyObject[] args) {
        setVisibility(args, Visibility.PRIVATE);
        return this;
    }

    /** rb_mod_modfunc
     *
     */
    public RubyModule module_function(IRubyObject[] args) {
        if (getRuntime().getSafeLevel() >= 4 && !isTaint()) {
            throw new SecurityError(getRuntime(), "Insecure: can't change method visibility");
        }

        if (args.length == 0) {
            getRuntime().setCurrentVisibility(Visibility.MODULE_FUNCTION);
        } else {
            setMethodVisibility(args, Visibility.PRIVATE);

            for (int i = 0; i < args.length; i++) {
                String name = args[i].asSymbol();
                ICallable method = searchMethod(name);
                assert !method.isUndefined() : "undefined method '" + name + "'";
                getSingletonClass().addMethod(name, new WrapperCallable(method, Visibility.PUBLIC));
                callMethod("singleton_method_added", RubySymbol.newSymbol(getRuntime(), name));
            }
        }
        return this;
    }

    public RubyBoolean method_defined(IRubyObject symbol) {
        return isMethodBound(symbol.asSymbol(), true) ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    public RubyModule public_class_method(IRubyObject[] args) {
        getMetaClass().setMethodVisibility(args, Visibility.PUBLIC);
        return this;
    }

    public RubyModule private_class_method(IRubyObject[] args) {
        getMetaClass().setMethodVisibility(args, Visibility.PRIVATE);
        return this;
    }

    public RubyModule alias_method(IRubyObject newId, IRubyObject oldId) {
        aliasMethod(newId.asSymbol(), oldId.asSymbol());
        return this;
    }

    public RubyModule undef_method(IRubyObject name) {
        undef(name.asSymbol());
        return this;
    }

    public IRubyObject module_eval(IRubyObject[] args) {
        return specificEval(this, args);
    }

    public RubyModule remove_method(IRubyObject name) {
        removeMethod(name.asSymbol());
        return this;
    }

    public void marshalTo(MarshalStream output) throws java.io.IOException {
        output.write('m');
        output.dumpString(name().toString());
    }

    public static RubyModule unmarshalFrom(UnmarshalStream input) throws java.io.IOException {
        String name = input.unmarshalString();
        Ruby runtime = input.getRuntime();
        RubyModule result = runtime.getClasses().getClassFromPath(name);
        if (result == null) {
            throw new NameError(runtime, "uninitialized constant " + name);
        }
        input.registerLinkTarget(result);
        return result;
    }
}
