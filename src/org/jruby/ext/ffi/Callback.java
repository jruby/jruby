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
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyClass;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ObjectAllocator;

/**
 * Defines a C callback's parameters and return type.
 */
@JRubyClass(name = FFIProvider.MODULE_NAME + "::" + Callback.CLASS_NAME, parent = "Object")
public class Callback extends RubyObject implements NativeParam {
    public static final String CLASS_NAME = "Callback";
    
    /**
     * The arity of this function.
     */
    protected final Arity arity;

    protected final NativeType[] parameterTypes;
    protected final NativeType returnType;
    
    public static RubyClass createCallbackClass(Ruby runtime, RubyModule module) {
        RubyClass result = module.defineClassUnder(CLASS_NAME,
                runtime.getObject(), 
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        result.defineAnnotatedMethods(Callback.class);
        result.defineAnnotatedConstants(Callback.class);

        return result;
    }
    /**
     * Creates a new <tt>Invoker</tt> instance.
     * @param arity
     */
    public Callback(Ruby runtime, NativeType returnType, NativeType[] paramTypes) {
        super(runtime, FFIProvider.getModule(runtime).fastGetClass(CLASS_NAME));
        this.arity = Arity.fixed(paramTypes.length);
        this.parameterTypes = paramTypes;
        this.returnType = returnType;
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
    public final NativeType[] getParameterTypes() {
        return parameterTypes;
    }
}
