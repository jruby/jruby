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
 * Copyright (C) 2008 JRuby project
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

package org.jruby.ext.ffi;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Defines a C callback's parameters and return type.
 */
@JRubyClass(name = "FFI::CallbackInfo", parent = "Object")
public class CallbackInfo extends RubyObject implements NativeParam {
    public static final String CLASS_NAME = "CallbackInfo";
    
    /**
     * The arity of this function.
     */
    protected final Arity arity;

    protected final NativeParam[] parameterTypes;
    protected final NativeType returnType;
    
    public static RubyClass createCallbackInfoClass(Ruby runtime, RubyModule module) {
        RubyClass result = module.defineClassUnder(CLASS_NAME,
                runtime.getObject(),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        result.defineAnnotatedMethods(CallbackInfo.class);
        result.defineAnnotatedConstants(CallbackInfo.class);

        return result;
    }
    
    /**
     * Creates a new <tt>CallbackInfo</tt> instance.
     * @param arity
     */
    public CallbackInfo(Ruby runtime, RubyClass klazz, NativeType returnType, NativeParam[] paramTypes) {
        super(runtime, klazz);
        this.arity = Arity.fixed(paramTypes.length);
        this.parameterTypes = paramTypes;
        this.returnType = returnType;
    }
    @JRubyMethod(name = "new", meta = true)
    public static final IRubyObject newCallbackInfo(ThreadContext context, IRubyObject self, IRubyObject returnType, IRubyObject _paramTypes)
    {
        RubyArray paramTypes = (RubyArray) _paramTypes;
        NativeParam[] nativeParamTypes = new NativeParam[paramTypes.size()];
        for (int i = 0; i < paramTypes.size(); ++i) {
            nativeParamTypes[i] = NativeType.valueOf(Util.int32Value((IRubyObject) paramTypes.entry(i)));
        }
        try {
            return new CallbackInfo(context.getRuntime(), (RubyClass) self,
                    NativeType.valueOf(Util.int32Value(returnType)), nativeParamTypes);
        } catch (UnsatisfiedLinkError ex) {
            return context.getRuntime().getNil();
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

    public final NativeType getReturnType() {
        return returnType;
    }
    public final NativeParam[] getParameterTypes() {
        return parameterTypes;
    }
}
