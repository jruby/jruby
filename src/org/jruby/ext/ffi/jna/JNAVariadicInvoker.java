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

package org.jruby.ext.ffi.jna;

import com.sun.jna.Function;
import com.sun.jna.NativeLibrary;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.ext.ffi.NativeType;
import org.jruby.ext.ffi.Util;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

@JRubyClass(name = "FFI::VariadicInvoker", parent = "Object")
public class JNAVariadicInvoker extends RubyObject {
    private final Function function;
    private final FunctionInvoker functionInvoker;

    public static RubyClass createVariadicInvokerClass(Ruby runtime, RubyModule module) {
        RubyClass result = module.defineClassUnder("VariadicInvoker",
                runtime.getObject(),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        result.defineAnnotatedMethods(JNAVariadicInvoker.class);
        result.defineAnnotatedConstants(JNAVariadicInvoker.class);

        return result;
    }
    /**
     * Creates a new <tt>Invoker</tt> instance.
     * @param arity
     */
    private JNAVariadicInvoker(Ruby runtime, Function function, FunctionInvoker functionInvoker) {
        super(runtime, runtime.fastGetModule("FFI").fastGetClass("VariadicInvoker"));
        this.function = function;
        this.functionInvoker = functionInvoker;
    }
    
    /**
     * Returns the {@link org.jruby.runtime.Arity} of this function.
     *
     * @return The <tt>Arity</tt> of the native function.
     */
    public final Arity getArity() {
        return Arity.OPTIONAL;
    }

    @JRubyMethod(name = { "__new" }, meta = true, required = 4)
    public static JNAVariadicInvoker newInvoker(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        int conv = "stdcall".equals(args[3].toString()) ? Function.ALT_CONVENTION : Function.C_CONVENTION;
        Function function;
        try {
            NativeLibrary lib = args[0] instanceof DynamicLibrary
                    ? ((DynamicLibrary) args[0]).getNativeLibrary()
                    : NativeLibrary.getInstance(args[0].toString());
            function = lib.getFunction(args[1].toString(), conv);
        } catch (UnsatisfiedLinkError ex) {
            throw context.getRuntime().newLoadError(ex.getMessage());
        }
        FunctionInvoker functionInvoker = JNAProvider.getFunctionInvoker(NativeType.valueOf(Util.int32Value(args[2])));
        return new JNAVariadicInvoker(recv.getRuntime(), function, functionInvoker);
    }

    @JRubyMethod(name = { "invoke" })
    public IRubyObject invoke(ThreadContext context, IRubyObject typesArg, IRubyObject paramsArg) {
        IRubyObject[] types = ((RubyArray) typesArg).toJavaArrayMaybeUnsafe();
        IRubyObject[] params = ((RubyArray) paramsArg).toJavaArrayMaybeUnsafe();
        Object[] args = new Object[types.length];
        Invocation invocation = new Invocation(context);
        for (int i = 0; i < types.length; ++i) {
            NativeType type = NativeType.valueOf(Util.int32Value(types[i]));
            switch (type) {
                case INT8:
                case UINT8:
                case INT16:
                case UINT16:
                case INT32:
                case UINT32:
                    args[i] = Integer.valueOf(Util.int32Value(params[i]));
                    break;
                case FLOAT32:
                case FLOAT64:
                    args[i] = Double.valueOf(Util.doubleValue(params[i]));
                    break;
                default:
                    args[i] = JNAProvider.getMarshaller(type).marshal(invocation, params[i]);
            }
        }
        return functionInvoker.invoke(context.getRuntime(), function, args);
    }
}
