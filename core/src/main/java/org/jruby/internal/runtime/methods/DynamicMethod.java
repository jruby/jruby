/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.internal.runtime.methods;

import java.lang.reflect.Method;
import java.util.Arrays;
import org.jruby.MetaClass;
import org.jruby.RubyModule;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.ivars.MethodData;
import org.jruby.util.CodegenUtils;

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
    /** The serial number for this method object, to globally identify it */
    protected long serialNumber;
    /** Is this a builtin core method or not */
    protected boolean builtin = false;
    /** Single-arity native call */
    protected NativeCall nativeCall;
    /** Alternate-arity NativeCalls */
    protected NativeCall[] nativeCalls = new NativeCall[10];
    /** The simple, base name this method was defined under. May be null.*/
    protected String name;
    /** Whether this method is "not implemented". */
    protected boolean notImplemented = false;
    /** An arbitrarily-typed "method handle" for use by compilers and call sites */
    protected Object handle;

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
     * Base constructor for dynamic method handles with names.
     *
     * @param implementationClass The class to which this method will be
     * immediately bound
     * @param visibility The visibility assigned to this method
     * @param callConfig The CallConfiguration to use for this method's
     * pre/post invocation logic.
     */
    protected DynamicMethod(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, String name) {
        this(implementationClass, visibility, callConfig);
        this.name = name;
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
        this.serialNumber = implementationClass.getRuntime().getNextDynamicMethodSerial();
    }

    /**
     * Get the global serial number for this method object
     *
     * @return This method object's serial number
     */
    public long getSerialNumber() {
        return serialNumber;
    }

    public boolean isBuiltin() {
        return builtin;
    }

    public void setIsBuiltin(boolean isBuiltin) {
        this.builtin = isBuiltin;
    }

    /**
     * The minimum 'call' method required for a dynamic method handle.
     * Subclasses must implement this method, but may implement the other
     * signatures to provide faster, non-boxing call paths. Typically
     * subclasses will implement this method to check variable arity calls,
     * then performing a specific-arity invocation to the appropriate method
     * or performing variable-arity logic in-line.
     *
     * @param context The thread context for the currently executing thread
     * @param self The 'self' or 'receiver' object to use for this call
     * @param clazz The Ruby class against which this method is binding
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
     * @param clazz The Ruby class against which this method is binding
     * @param name The incoming name used to invoke this method
     * @param args The first argument to this invocation
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

    /** Arity 0, no block */
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name) {
        return call(context, self, klazz, name, Block.NULL_BLOCK);
    }
    /** Arity 0, with block; calls through IRubyObject[] path */
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, Block block) {
        return call(context, self, klazz, name, IRubyObject.NULL_ARRAY, block);
    }
    /** Arity 1, no block */
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg0) {
        return call(context, self, klazz, name, arg0, Block.NULL_BLOCK);
    }
    /** Arity 1, with block; calls through IRubyObject[] path */
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg0, Block block) {
        return call(context, self, klazz, name, new IRubyObject[] {arg0}, block);
    }
    /** Arity 2, no block */
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg0, IRubyObject arg1) {
        return call(context, self, klazz, name, arg0, arg1, Block.NULL_BLOCK);
    }
    /** Arity 2, with block; calls through IRubyObject[] path */
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        return call(context, self, klazz, name, new IRubyObject[] {arg0, arg1}, block);
    }
    /** Arity 3, no block */
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return call(context, self, klazz, name, arg0, arg1, arg2, Block.NULL_BLOCK);
    }
    /** Arity 3, with block; calls through IRubyObject[] path */
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return call(context, self, klazz, name, new IRubyObject[] {arg0, arg1, arg2}, block);
    }



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

    public boolean isImplementedBy(RubyModule other) {
        return implementationClass == other;
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
     * Whether this method is the "null" method, used to stop method
     * name resolution loops. Only returns true for NullMethod instances,
     * of which there should be only one (a singleton).
     *
     * @return true if this method is the undefined method; false otherwise
     */
    public final boolean isNull() {
        return this == NullMethod.INSTANCE;
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
    
    public static class NativeCall {
        private final Class nativeTarget;
        private final String nativeName;
        private final Class nativeReturn;
        private final Class[] nativeSignature;
        private final boolean statik;
        private final boolean java;

        public NativeCall(Class nativeTarget, String nativeName, Class nativeReturn, Class[] nativeSignature, boolean statik) {
            this(nativeTarget, nativeName, nativeReturn, nativeSignature, statik, false);
        }

        public NativeCall(Class nativeTarget, String nativeName, Class nativeReturn, Class[] nativeSignature, boolean statik, boolean java) {
            this.nativeTarget = nativeTarget;
            this.nativeName = nativeName;
            this.nativeReturn = nativeReturn;
            this.nativeSignature = nativeSignature;
            this.statik = statik;
            this.java = java;
        }

        public Class getNativeTarget() {
            return nativeTarget;
        }

        public String getNativeName() {
            return nativeName;
        }

        public Class getNativeReturn() {
            return nativeReturn;
        }

        public Class[] getNativeSignature() {
            return nativeSignature;
        }

        public boolean isStatic() {
            return statik;
        }
        
        public boolean isJava() {
            return java;
        }

        public boolean hasContext() {
            return nativeSignature.length > 0 && nativeSignature[0] == ThreadContext.class;
        }

        public boolean hasBlock() {
            return nativeSignature.length > 0 && nativeSignature[nativeSignature.length - 1] == Block.class;
        }
        
        /**
         * Get the java.lang.reflect.Method for this NativeCall
         * 
         * @return the reflected method corresponding to this NativeCall
         */
        public Method getMethod() {
            try {
                return nativeTarget.getDeclaredMethod(nativeName, nativeSignature);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String toString() {
            return "" + (statik?"static ":"") + nativeReturn.getSimpleName() + " " + nativeTarget.getSimpleName() + "." + nativeName + CodegenUtils.prettyShortParams(nativeSignature);
        }
    }

    /**
     * Set the single-arity NativeCall for this method. All signatures for the
     * non-single-arity getNativeCall will also be set to this value.
     * 
     * @param nativeTarget native method target
     * @param nativeName native method name
     * @param nativeReturn native method return
     * @param nativeSignature native method arguments
     * @param statik static?
     * @param java plain Java method?
     */
    public void setNativeCall(Class nativeTarget, String nativeName, Class nativeReturn, Class[] nativeSignature, boolean statik, boolean java) {
        this.nativeCall = new NativeCall(nativeTarget, nativeName, nativeReturn, nativeSignature, statik, java);
        Arrays.fill(nativeCalls, nativeCall);
    }


    /**
     * Set the single-arity NativeCall for this method. All signatures for the
     * non-single-arity getNativeCall will also be set to this value.
     * 
     * @param nativeTarget native method target
     * @param nativeName native method name
     * @param nativeReturn native method return
     * @param nativeSignature native method arguments
     * @param statik static?
     */
    public void setNativeCall(Class nativeTarget, String nativeName, Class nativeReturn, Class[] nativeSignature, boolean statik) {
        setNativeCall(nativeTarget, nativeName, nativeReturn, nativeSignature, statik, false);
    }
    
    public NativeCall getNativeCall() {
        return this.nativeCall;
    }
    
    public NativeCall getNativeCall(int args, boolean block) {
        if (args == -1 || args > 3) args = 4;
        if (block) args += 5;
        return this.nativeCalls[args];
    }
    
    public void setNativeCall(int args, boolean block, NativeCall nativeCall) {
        if (args == -1 || args > 3) args = 4;
        if (block) args += 5;
        this.nativeCalls[args] = nativeCall;
    }

    /**
     * Returns true if this method is backed by native (i.e. Java) code.
     *
     * @return true If backed by Java code or JVM bytecode; false otherwise
     */
    public boolean isNative() {
        return false;
    }

    /**
     * Get the base name this method was defined as.
     *
     * @return the base name for the method
     */
    public String getName() {
        return name;
    }

    /**
     * Set the base name for this method.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the "handle" associated with this DynamicMethod.
     * 
     * @return the handle
     */
    public Object getHandle() {
        return handle;
    }

    /**
     * Set the "handle" associated with this DynamicMethod.
     * 
     * @param handle the handle
     */
    public void setHandle(Object handle) {
        this.handle = handle;
    }
    
    /**
     * Whether this method is "not implemented". This is
     * primarily to support Ruby 1.9's behavior of respond_to? yielding false if
     * the feature in question is unsupported (but still having the method defined).
     */
    public boolean isNotImplemented() {
        return notImplemented;
    }
    
    /**
     * Additional metadata about this method.
     */
    public MethodData getMethodData() {
        return MethodData.NULL;
    }
    
    /**
     * Set whether this method is "not implemented".
     */
    public void setNotImplemented(boolean setNotImplemented) {
        this.notImplemented = setNotImplemented;
    }
}
