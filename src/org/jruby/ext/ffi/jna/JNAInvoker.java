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
import org.jruby.RubyModule;
import org.jruby.ext.ffi.Invoker;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A native invoker that uses JNA.
 */
final class JNAInvoker extends Invoker {

    private final Function function;
    private final FunctionInvoker functionInvoker;
    private final Marshaller[] marshallers;

    public JNAInvoker(Ruby runtime, Function function, FunctionInvoker functionInvoker, Marshaller[] marshallers) {
        super(runtime, marshallers.length);
        this.function = function;
        this.functionInvoker = functionInvoker;
        this.marshallers = marshallers;
    }

    public IRubyObject invoke(Ruby runtime, IRubyObject[] rubyArgs) {
        Object[] args = new Object[rubyArgs.length];
        Invocation invocation = new Invocation();
        for (int i = 0; i < args.length; ++i) {
            args[i] = marshallers[i].marshal(invocation, rubyArgs[i]);
        }
        IRubyObject retVal = functionInvoker.invoke(runtime, function, args);
        invocation.finish();
        return retVal;
    }
    
    /**
     * Attaches this function to a ruby module or class.
     * 
     * @param module The module or class to attach the function to.
     * @param methodName The ruby name to attach the function as.
     */
    @Override
    public DynamicMethod createDynamicMethod(RubyModule module) {
        if (Arity.NO_ARGUMENTS.equals(arity)) {
            return new DynamicMethodZeroArg(module, function, functionInvoker);
        } else if (Arity.ONE_ARGUMENT.equals(arity)) {
            return new DynamicMethodOneArg(module, function, functionInvoker, marshallers);
        } else if (Arity.TWO_ARGUMENTS.equals(arity)) {
            return new DynamicMethodTwoArg(module, function, functionInvoker, marshallers);
        } else if (Arity.THREE_ARGUMENTS.equals(arity)) {
            return new DynamicMethodThreeArg(module, function, functionInvoker, marshallers);
        } else {
            return super.createDynamicMethod(module);
        }
    }
}
