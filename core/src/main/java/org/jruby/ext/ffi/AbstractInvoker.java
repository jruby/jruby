/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.ext.ffi;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.runtime.ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR;

/**
 * A native function invoker
 */
@JRubyClass(name = "FFI::" + AbstractInvoker.CLASS_NAME, parent = "Object")
public abstract class AbstractInvoker extends Pointer {
    static final String CLASS_NAME = "AbstractInvoker";
    
    /**
     * The arity of this function.
     */
    protected final Arity arity;
    
    public static RubyClass createAbstractInvokerClass(ThreadContext context, RubyModule FFI, RubyClass Pointer) {
        return FFI.defineClassUnder(context, CLASS_NAME, Pointer, NOT_ALLOCATABLE_ALLOCATOR).
                defineMethods(context, AbstractInvoker.class).
                defineConstants(context, AbstractInvoker.class);
    }
    
    /**
     * Creates a new <code>AbstractInvoker</code> instance.
     * @param arity
     */
    protected AbstractInvoker(Ruby runtime, RubyClass klass, int arity, MemoryIO io) {
        super(runtime, klass, io, 0);
        this.arity = Arity.fixed(arity);
    }

    /**
     * Attaches this function to a ruby module or class.
     * 
     * @param context The thread context.
     * @param obj The module or class to attach the function to.
     * @param methodName The ruby name to attach the function as.
     */
    @JRubyMethod(name="attach")
    public IRubyObject attach(ThreadContext context, IRubyObject obj, IRubyObject methodName) {

        var singleton = obj.singletonClass(context);
        DynamicMethod m = createDynamicMethod(singleton);
        
        singleton.addMethod(methodName.asJavaString(), m);
        if (obj instanceof RubyModule mod) mod.addMethod(methodName.asJavaString(), m);

        getRuntime().getFFI().registerAttachedMethod(m, this);
        
        return this;
    }
    protected abstract DynamicMethod createDynamicMethod(RubyModule module);

    /**
     * Returns the {@link org.jruby.runtime.Arity} of this function.
     * 
     * @return The <code>Arity</code> of the native function.
     */
    public final Arity getArity() {
        return arity;
    }
}
