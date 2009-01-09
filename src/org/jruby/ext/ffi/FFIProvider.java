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
import org.jruby.RubyInteger;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Base class for all FFI providers
 */
@JRubyClass(name = "FFI::FFIProvider", parent = "Object")
public abstract class FFIProvider extends RubyObject {
    
    /**
     * The name of the module to place all the classes/methods under.
     */
    public static final String MODULE_NAME = "FFI";
    public static final String CLASS_NAME = "Provider";
    
    public static RubyClass createProviderClass(Ruby runtime, RubyModule module) {
        RubyClass result = module.defineClassUnder(CLASS_NAME, runtime.getObject(),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        result.defineAnnotatedMethods(FFIProvider.class);
        result.defineAnnotatedConstants(FFIProvider.class);

        return result;
    }
    protected FFIProvider(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }
    
    protected FFIProvider(Ruby runtime) {
        super(runtime, getModule(runtime).fastGetClass(CLASS_NAME));
    }
    
    public static RubyModule getModule(Ruby runtime) {
        return (RubyModule) runtime.fastGetModule("FFI");
    }
    
    @JRubyMethod(name = { "create_invoker", "createInvoker" }, required = 5)
    public IRubyObject createInvoker(ThreadContext context, IRubyObject[] args)
    {
        RubyArray paramTypes = (RubyArray) args[3];
        NativeParam[] nativeParamTypes = new NativeParam[paramTypes.size()];
        for (int i = 0; i < paramTypes.size(); ++i) {
            IRubyObject obj = (IRubyObject) paramTypes.entry(i);
            if (obj instanceof NativeParam) {
                nativeParamTypes[i] = (NativeParam) obj;
            } else if (obj instanceof RubyInteger) {
                nativeParamTypes[i] = NativeType.valueOf(Util.int32Value(obj));
            } else {
                context.getRuntime().newArgumentError("Invalid parameter type");
            }
        }
        try {
            return createInvoker(context.getRuntime(), 
                    args[0].isNil() ? null : args[0].toString(), 
                    args[1].toString(), NativeType.valueOf(Util.int32Value(args[2])), 
                    nativeParamTypes, args[4].toString());
        } catch (UnsatisfiedLinkError ex) {
            return context.getRuntime().getNil();
        }
    }
    
    @JRubyMethod(name = { "error", "last_error" })
    public IRubyObject getLastError(ThreadContext context)
    {
        return context.getRuntime().newFixnum(getLastError());
    }

    @JRubyMethod(name = { "error=", "last_error=" })
    public IRubyObject getLastError(ThreadContext context, IRubyObject error)
    {
        setLastError(Util.int32Value(error));
        return context.getRuntime().getNil();
    }
    
    /**
     * Creates a new invoker for a native function.
     * 
     * @param libraryName The library that contains the function.
     * @param functionName The function name.
     * @param returnType The return type of the function.
     * @param parameterTypes The parameter types the function takes.
     * @return a new <tt>Invoker</tt> instance.
     */
    public abstract AbstractInvoker createInvoker(Ruby runtime, String libraryName, String functionName, NativeType returnType,
            NativeParam[] parameterTypes, String convention);

    /**
     * Gets the last native error code.
     * <p>
     * This returns the errno value that was set at the time of the last native 
     * function call.
     * 
     * @return The errno value.
     */
    public abstract int getLastError();
    
    /**
     * Sets the native error code.
     * 
     * @param error The value to set errno to.
     */
    public abstract void setLastError(int error);

    
}
