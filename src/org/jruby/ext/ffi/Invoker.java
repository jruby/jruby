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
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A native function invoker
 */
@JRubyClass(name = FFIProvider.MODULE_NAME + "::" + Invoker.CLASS_NAME, parent = "Object")
public abstract class Invoker extends RubyObject {
    static final String CLASS_NAME = "Invoker";
    
    /**
     * The arity of this function.
     */
    protected final Arity arity;
    
    public static RubyClass createInvokerClass(Ruby runtime) {
        RubyModule module = FFIProvider.getModule(runtime);
        RubyClass result = module.defineClassUnder(CLASS_NAME,
                runtime.getObject(), 
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        result.defineAnnotatedMethods(Invoker.class);
        result.defineAnnotatedConstants(Invoker.class);

        return result;
    }

    /**
     * Creates a new <tt>Invoker</tt> instance.
     * @param arity
     */
    protected Invoker(Ruby runtime, int arity) {
        super(runtime, FFIProvider.getModule(runtime).fastGetClass(CLASS_NAME));
        this.arity = Arity.fixed(arity);
    }

    /**
     * Attaches this function to a ruby module or class.
     * 
     * @param module The module or class to attach the function to.
     * @param methodName The ruby name to attach the function as.
     */
    @JRubyMethod(name="attach")
    public IRubyObject attach(ThreadContext context, IRubyObject module, IRubyObject methodName) {

        DynamicMethod m = createDynamicMethod((RubyModule) module);
        ((RubyModule) module).addMethod(methodName.asJavaString(), m);
        return context.getRuntime().getNil();
    }
    protected DynamicMethod createDynamicMethod(RubyModule module) {
        return new DynamicMethod(module, Visibility.PUBLIC, CallConfiguration.NO_FRAME_NO_SCOPE) {

            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
                arity.checkArity(context.getRuntime(), args);
                return invoke(context.getRuntime(), args);
            }

            @Override
            public DynamicMethod dup() {
                return this;
            }

            @Override
            public Arity getArity() {
                return Invoker.this.getArity();
            }

            @Override
            public boolean isNative() {
                return true;
            }
        }; 
    }
    /**
     * Invokes the native function with the supplied ruby arguments.
     * @param rubyArgs The ruby arguments to pass to the native function.
     * @return The return value from the native function, as a ruby object.
     */
    public Object invoke(RubyArray rubyArgs) {
        return invoke(rubyArgs.getRuntime(), rubyArgs.toJavaArrayMaybeUnsafe());
    }
    
    public abstract IRubyObject invoke(Ruby runtime, IRubyObject[] args);
    
    /**
     * Returns the {@link org.jruby.runtime.Arity} of this function.
     * 
     * @return The <tt>Arity</tt> of the native function.
     */
    public final Arity getArity() {
        return arity;
    }
}
