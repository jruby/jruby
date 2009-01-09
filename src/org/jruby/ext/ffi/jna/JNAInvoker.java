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
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyMethod;
import org.jruby.ext.ffi.AbstractInvoker;
import org.jruby.ext.ffi.NativeParam;
import org.jruby.ext.ffi.NativeType;
import org.jruby.ext.ffi.Util;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A native invoker that uses JNA.
 */
final class JNAInvoker extends AbstractInvoker {

    private final Function function;
    private final FunctionInvoker functionInvoker;
    private final Marshaller[] marshallers;

    public static RubyClass createInvokerClass(Ruby runtime, RubyModule module) {
        RubyClass result = module.defineClassUnder("Invoker",
                runtime.getObject(),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        result.defineAnnotatedMethods(AbstractInvoker.class);
        result.defineAnnotatedMethods(JNAInvoker.class);
        result.defineAnnotatedConstants(JNAInvoker.class);

        return result;
    }

    @JRubyMethod(name = { "new" }, meta = true, required = 5)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        String convention = args[4].toString();
        DynamicLibrary library = (DynamicLibrary) args[0];
        String functionName = ((DynamicLibrary.Symbol) args[1]).getName();
        RubyArray paramTypes = (RubyArray) args[2];
        NativeParam[] parameterTypes = getNativeParameterTypes(context.getRuntime(), paramTypes);
        NativeType returnType = NativeType.valueOf(Util.int32Value(args[3]));
        int conv = "stdcall".equals(convention) ? Function.ALT_CONVENTION : Function.C_CONVENTION;
        Function function = library.getNativeLibrary().getFunction(functionName, conv);
        FunctionInvoker functionInvoker = JNAProvider.getFunctionInvoker(returnType);
        Marshaller[] marshallers = new Marshaller[parameterTypes.length];
        for (int i = 0; i < marshallers.length; ++i) {
            marshallers[i] = JNAProvider.getMarshaller(parameterTypes[i], conv);
        }

        return new JNAInvoker(context.getRuntime(), (RubyClass) recv, function, functionInvoker, marshallers);
    }

    public JNAInvoker(Ruby runtime, RubyClass klass, Function function, FunctionInvoker functionInvoker, Marshaller[] marshallers) {
        super(runtime, klass, marshallers.length);
        this.function = function;
        this.functionInvoker = functionInvoker;
        this.marshallers = marshallers;
    }

    /**
     * Invokes the native function with the supplied ruby arguments.
     * @param rubyArgs The ruby arguments to pass to the native function.
     * @return The return value from the native function, as a ruby object.
     */
    @JRubyMethod(name= { "invoke", "call", "call0", "call1", "call2", "call3" }, rest = true)
    public IRubyObject invoke(ThreadContext context, IRubyObject[] rubyArgs) {
        Object[] args = new Object[rubyArgs.length];
        Invocation invocation = new Invocation(context);
        for (int i = 0; i < args.length; ++i) {
            args[i] = marshallers[i].marshal(invocation, rubyArgs[i]);
        }
        IRubyObject retVal = functionInvoker.invoke(context.getRuntime(), function, args);
        invocation.finish();
        return retVal;
    }
    
    /**
     * Attaches this function to a ruby module or class.
     * 
     * @param module The module or class to attach the function to.
     * @param methodName The ruby name to attach the function as.
     */
    public DynamicMethod createDynamicMethod(RubyModule module) {
        /*
         * If there is exactly _one_ callback argument to the function,
         * then a block can be given and automatically subsituted for the callback
         * parameter.
         */
        if (marshallers.length > 0) {
            int cbcount = 0, cbindex = -1;
            for (int i = 0; i < marshallers.length; ++i) {
                if (marshallers[i] instanceof CallbackMarshaller) {
                    cbcount++;
                    cbindex = i;                    
                }
            }
            if (cbcount == 1) {
                return new CallbackMethodWithBlock(module, function, functionInvoker, marshallers, cbindex);
            }
        }
        if (Arity.NO_ARGUMENTS.equals(arity)) {
            return new DynamicMethodZeroArg(module, function, functionInvoker);
        } else if (Arity.ONE_ARGUMENT.equals(arity)) {
            return new DynamicMethodOneArg(module, function, functionInvoker, marshallers);
        } else if (Arity.TWO_ARGUMENTS.equals(arity)) {
            return new DynamicMethodTwoArg(module, function, functionInvoker, marshallers);
        } else if (Arity.THREE_ARGUMENTS.equals(arity)) {
            return new DynamicMethodThreeArg(module, function, functionInvoker, marshallers);
        } else {
            return new DynamicMethod(module, Visibility.PUBLIC, CallConfiguration.FrameNoneScopeNone) {
                @Override
                public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
                    arity.checkArity(context.getRuntime(), args);
                    return invoke(context, args);
                }

                @Override
                public DynamicMethod dup() {
                    return this;
                }

                @Override
                public Arity getArity() {
                    return JNAInvoker.this.getArity();
                }

                @Override
                public boolean isNative() {
                    return true;
                }
            };
        }
    }
}
