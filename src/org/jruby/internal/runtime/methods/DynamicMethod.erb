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
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2005 Thomas E Enebo <enebo@acm.org>
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

<%= generated_warning %>

package org.jruby.internal.runtime.methods;

import org.jruby.MetaClass;
import org.jruby.Ruby;
import org.jruby.RubyLocalJumpError;
import org.jruby.RubyModule;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * DynamicMethod represents a method handle in JRuby, to provide both entry
 * points into AST and bytecode interpreters, but also to provide handles to
 * JIT-compiled and hand-implemented Java methods. All methods invokable from
 * Ruby code are referenced by method handles, either directly or through
 * delegation or callback mechanisms.
 */
public abstract class DynamicMethod {
    /** The Ruby module or class in which this method is immediately defined. */
    protected RubyModule implementationClass;
    /** The "protected class" used for calculating protected access. */
    protected RubyModule protectedClass;
    /** The visibility of this method. */
    protected Visibility visibility;
    /** The "call configuration" to use for pre/post call logic. */
    protected CallConfiguration callConfig;
    
    /**
     * Base constructor for dynamic method handles.
     * 
     * @param implementationClass The class to which this method will be
     * immediately bound
     * @param visibility The visibility assigned to this method
     * @param callConfig The CallConfiguration to use for this method's
     * pre/post invocation logic.
     */
    protected DynamicMethod(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
        assert implementationClass != null;
        init(implementationClass, visibility, callConfig);
    }
    
    /**
     * A no-arg constructor used only by the UndefinedMethod subclass and
     * CompiledMethod handles. instanceof assertions make sure this is so.
     */
    protected DynamicMethod() {
//        assert (this instanceof UndefinedMethod ||
//                this instanceof CompiledMethod ||
//                this instanceof );
    }
    
    protected void init(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
        this.visibility = visibility;
        this.implementationClass = implementationClass;
        // TODO: Determine whether we should perhaps store non-singleton class
        // in the implementationClass
        this.protectedClass = calculateProtectedClass(implementationClass);
        this.callConfig = callConfig;
    }

    /**
     * The minimum 'call' method required for a dynamic method handle.
     * Subclasses must impleemnt this method, but may implement the other
     * signatures to provide faster, non-boxing call paths. Typically
     * subclasses will implement this method to check variable arity calls,
     * then performing a specific-arity invocation to the appropriate method
     * or performing variable-arity logic in-line.
     * 
     * @param context The thread context for the currently executing thread
     * @param self The 'self' or 'receiver' object to use for this call
     * @param klazz The Ruby class against which this method is binding
     * @param name The incoming name used to invoke this method
     * @param args The argument list to this invocation
     * @param block The block passed to this invocation
     * @return The result of the call
     */
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, 
            String name, IRubyObject[] args, Block block);

    /**
     * A default implementation of n-arity, non-block 'call' method,
     * which simply calls the n-arity, block-receiving version with
     * the arg list and Block.NULL_BLOCK.
     *
     * @param context The thread context for the currently executing thread
     * @param self The 'self' or 'receiver' object to use for this call
     * @param klazz The Ruby class against which this method is binding
     * @param name The incoming name used to invoke this method
     * @param arg1 The first argument to this invocation
     * @param arg2 The second argument to this invocation
     * @return The result of the call
     */
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz,
            String name, IRubyObject[] args) {
        return call(context, self, clazz, name, args, Block.NULL_BLOCK);
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Now we provide default impls of a number of signatures. For each arity,
    // we first generate a non-block version of the method, which just adds
    // NULL_BLOCK and re-calls, allowing e.g. compiled code, which always can
    // potentially take a block, to only generate the block-receiving signature
    // and still avoid arg boxing.
    //
    // We then provide default implementations of each block-accepting method
    // that in turn call the IRubyObject[]+Block version of call. This then
    // finally falls back on the minimum implementation requirement for
    // dynamic method handles.
    ////////////////////////////////////////////////////////////////////////////

<%= generated_arities %>
    
    /**
     * Duplicate this method, returning DynamicMethod referencing the same code
     * and with the same attributes.
     * 
     * It is not required that this method produce a new object if the
     * semantics of the DynamicMethod subtype do not require such.
     * 
     * @return An identical DynamicMethod object to the target.
     */
    public abstract DynamicMethod dup();

    /**
     * Determine whether this method is callable from the given object using
     * the given call type.
     * 
     * @param caller The calling object
     * @param callType The type of call
     * @return true if the call would not violate visibility; false otherwise
     */
    public boolean isCallableFrom(IRubyObject caller, CallType callType) {
        switch (visibility) {
        case PUBLIC:
            return true;
        case PRIVATE:
            return callType != CallType.NORMAL;
        case PROTECTED:
            return protectedAccessOk(caller);
        }
        
        return true;
    }
    
    /**
     * Determine whether the given object can safely invoke protected methods on
     * the class this method is bound to.
     * 
     * @param caller The calling object
     * @return true if the calling object can call protected methods; false
     * otherwise
     */
    private boolean protectedAccessOk(IRubyObject caller) {
        return getProtectedClass().isInstance(caller);
    }
    
    /**
     * Calculate, based on given RubyModule, which class in its hierarchy
     * should be used to determine protected access.
     * 
     * @param cls The class from which to calculate
     * @return The class to be used for protected access checking.
     */
    protected static RubyModule calculateProtectedClass(RubyModule cls) {
        // singleton classes don't get their own visibility domain
        if (cls.isSingleton()) cls = cls.getSuperClass();

        while (cls.isIncluded()) cls = cls.getMetaClass();

        // For visibility we need real meta class and not anonymous one from class << self
        if (cls instanceof MetaClass) cls = ((MetaClass) cls).getRealClass();
        
        return cls;
    }
    
    /**
     * Retrieve the pre-calculated "protected class" used for access checks.
     * 
     * @return The "protected class" for access checks.
     */
    protected RubyModule getProtectedClass() {
        return protectedClass;
    }
    
    /**
     * Retrieve the class or module on which this method is implemented, used
     * for 'super' logic among others.
     * 
     * @return The class on which this method is implemented
     */
    public RubyModule getImplementationClass() {
        return implementationClass;
    }

    /**
     * Set the class on which this method is implemented, used for 'super'
     * logic, among others.
     * 
     * @param implClass The class on which this method is implemented
     */
    public void setImplementationClass(RubyModule implClass) {
        implementationClass = implClass;
        protectedClass = calculateProtectedClass(implClass);
    }

    /**
     * Get the visibility of this method.
     * 
     * @return The visibility of this method
     */
    public Visibility getVisibility() {
        return visibility;
    }

    /**
     * Set the visibility of this method.
     * 
     * @param visibility The visibility of this method
     */
    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    /**
     * Whether this method is the "undefined" method, used to represent a
     * missing or undef'ed method. Only returns true for UndefinedMethod
     * instances, of which there should be only one (a singleton).
     * 
     * @return true if this method is the undefined method; false otherwise
     */
    public final boolean isUndefined() {
        return this == UndefinedMethod.INSTANCE;
    }

    /**
     * Retrieve the arity of this method, used for reporting arity to Ruby
     * code. This arity may or may not reflect the actual specific or variable
     * arities of the referenced method.
     * 
     * @return The arity of the method, as reported to Ruby consumers.
     */
    public Arity getArity() {
        return Arity.optional();
    }
    
    /**
     * Get the "real" method contained within this method. This simply returns
     * self except in cases where a method is wrapped to give it a new
     * name or new implementation class (AliasMethod, WrapperMethod, ...).
     * 
     * @return The "real" method associated with this one
     */
    public DynamicMethod getRealMethod() {
        return this;
    }

    /**
     * Get the CallConfiguration used for pre/post logic for this method handle.
     * 
     * @return The CallConfiguration for this method handle
     */
    public CallConfiguration getCallConfig() {
        return callConfig;
    }

    /**
     * Set the CallConfiguration used for pre/post logic for this method handle.
     * 
     * @param callConfig The CallConfiguration for this method handle
     */
    public void setCallConfig(CallConfiguration callConfig) {
        this.callConfig = callConfig;
    }
    
    /**
     * Returns true if this method is backed by native (i.e. Java) code.
     * 
     * @return true If backed by Java code or JVM bytecode; false otherwise
     */
    public boolean isNative() {
        return false;
    }

    protected IRubyObject handleRedo(Ruby runtime) throws RaiseException {
        throw runtime.newLocalJumpError(RubyLocalJumpError.Reason.REDO, runtime.getNil(), "unexpected redo");
    }

    protected IRubyObject handleReturn(ThreadContext context, JumpException.ReturnJump rj) {
        if (rj.getTarget() == context.getFrameJumpTarget()) {
            return (IRubyObject) rj.getValue();
        }
        throw rj;
    }
}
