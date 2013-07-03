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
 * Copyright (C) 2008, 2009 JRuby project
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

package org.jruby.ext.ffi;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Defines a C callback's parameters and return type.
 */
@JRubyClass(name = "FFI::CallbackInfo", parent = "FFI::Type")
public class CallbackInfo extends Type {
    public static final String CLASS_NAME = "CallbackInfo";
    
    /** The arity of this function. */
    protected final Arity arity;

    protected final Type[] parameterTypes;
    protected final Type returnType;
    protected final boolean stdcall;


    /**
     * Creates a CallbackInfo class for a ruby runtime
     *
     * @param runtime The runtime to create the class for
     * @param module The module to place the class in
     *
     * @return The newly created ruby class
     */
    public static RubyClass createCallbackInfoClass(Ruby runtime, RubyModule module) {
        RubyClass result = module.defineClassUnder(CLASS_NAME,
                module.getClass("Type"),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        result.defineAnnotatedMethods(CallbackInfo.class);
        result.defineAnnotatedConstants(CallbackInfo.class);

        module.getClass("Type").setConstant("Function", result);
        return result;
    }
    
    /**
     * Creates a new <tt>CallbackInfo</tt> instance.
     *
     * @param runtime The runtime to create the instance for
     * @param klazz The ruby class of the CallbackInfo instance
     * @param returnType The return type of the callback
     * @param paramTypes The parameter types of the callback
     */
    public CallbackInfo(Ruby runtime, RubyClass klazz, Type returnType, Type[] paramTypes, boolean stdcall) {
        super(runtime, klazz, NativeType.POINTER);
        this.arity = Arity.fixed(paramTypes.length);
        this.parameterTypes = paramTypes;
        this.returnType = returnType;
        this.stdcall = stdcall;
    }

    /**
     * CallbackInfo.new
     *
     * @param context The current ruby thread context
     * @param klass The ruby class of the CallbackInfo instance
     * @param returnType The ruby return type
     * @param _paramTypes An array containing the ruby parameter types
     *
     * @return A new CallbackInfo instance
     */
    @JRubyMethod(name = "new", meta = true, required = 2, optional = 1)
    public static final IRubyObject newCallbackInfo(ThreadContext context, IRubyObject klass,
            IRubyObject[] args)
    {
        IRubyObject returnType = args[0], paramTypes = args[1];

        if (!(returnType instanceof Type)) {
            throw context.runtime.newTypeError("wrong argument type "
                    + returnType.getMetaClass().getName() + " (expected FFI::Type)");
        }

        if (!(paramTypes instanceof RubyArray)) {
            throw context.runtime.newTypeError("wrong argument type "
                    + paramTypes.getMetaClass().getName() + " (expected Array)");
        }

        Type[] nativeParamTypes = new Type[((RubyArray)paramTypes).size()];
        for (int i = 0; i < nativeParamTypes.length; ++i) {
            IRubyObject obj = ((RubyArray) paramTypes).entry(i);
            if (!(obj instanceof Type)) {
                throw context.runtime.newTypeError("wrong argument type "
                        + obj.getMetaClass().getName() + " (expected array of FFI::Type)");
            }
            nativeParamTypes[i] = (Type) obj;
        }

        boolean stdcall = false;
        if (args.length > 2) {
            if (!(args[2] instanceof RubyHash)) {
                throw context.runtime.newTypeError("wrong argument type "
                        + args[3].getMetaClass().getName() + " (expected Hash)");
            }
            RubyHash hash = (RubyHash) args[2];
            stdcall = "stdcall".equals(hash.get(context.runtime.newSymbol("convention")));
        }
        
        try {
            return new CallbackInfo(context.runtime, (RubyClass) klass,
                    (Type) returnType, nativeParamTypes, stdcall);
        } catch (UnsatisfiedLinkError ex) {
            return context.runtime.getNil();
        }
    }
    
    /**
     * Returns the {@link org.jruby.runtime.Arity} of this function.
     * 
     * @return The <tt>Arity</tt> of the native function.
     */
    public final Arity getArity() {
        return arity;
    }

    /**
     * Gets the native return type the callback should return
     *
     * @return The native return type
     */
    public final Type getReturnType() {
        return returnType;
    }

    /**
     * Gets the ruby parameter types of the callback
     *
     * @return An array of the parameter types
     */
    public final Type[] getParameterTypes() {
        return parameterTypes;
    }

    public final boolean isStdcall() {
        return stdcall;
    }

    @JRubyMethod(name = "to_s")
    public final IRubyObject to_s(ThreadContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("#<FFI::CallbackInfo [ ");
        for (int i = 0; i < parameterTypes.length; ++i) {
            sb.append(parameterTypes[i].toString().toLowerCase());
            if (i < (parameterTypes.length - 1)) {
                sb.append(", ");
            }
        }
        sb.append(" ], " + returnType.toString().toLowerCase() + ">");
        return context.runtime.newString(sb.toString());
    }
    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CallbackInfo[parameters=[");
        for (int i = 0; i < parameterTypes.length; ++i) {
            sb.append(parameterTypes[i].toString().toLowerCase());
            if (i < (parameterTypes.length - 1)) {
                sb.append(", ");
            }
        }
        sb.append("] return=" + returnType.toString().toLowerCase() + "]");
        return sb.toString();
    }

    @JRubyMethod
    public final IRubyObject result_type(ThreadContext context) {
        return returnType;
    }

    @JRubyMethod
    public final IRubyObject param_types(ThreadContext context) {
        return RubyArray.newArray(context.runtime, parameterTypes);
    }
}
