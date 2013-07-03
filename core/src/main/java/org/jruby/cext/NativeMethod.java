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
 * Copyright (C) 2009 Wayne Meissner
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

package org.jruby.cext;

import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.scope.ManyVarsDynamicScope;

/**
 * {@link NativeMethod} represents a method handle to a C extension function in JRuby,
 * to provide entry points into C code. Native methods setup and tear down scope and context
 * around a native method call and lock the {@link GIL}. Native methods restrict concurrency
 * in C extensions to avoid threading issues in C code.
 */
public class NativeMethod extends DynamicMethod {
    protected final Arity arity;
    protected final long function;
    private final Native nativeInstance;

    public NativeMethod(RubyModule clazz, int arity, long function) {
        super(clazz, Visibility.PUBLIC, CallConfiguration.FrameBacktraceScopeFull);
        this.arity = Arity.createArity(arity);
        this.function = function;
        this.nativeInstance = Native.getInstance(clazz.getRuntime());
    }

    @Override
    public final DynamicMethod dup() {
        return this;
    }

    @Override
    public final Arity getArity() {
        return arity;
    }

    @Override
    public final boolean isNative() {
        return true;
    }

    /**
     * Pushes the method frame for execution of the native function. Enforces use of a {@link ManyVarsDynamicScope}
     * since we don't know what we will need in the function execution. Lastly, this acquires the {@link GIL} for native
     * execution in the current Thread.
     */
    static void pre(ThreadContext context, IRubyObject self, RubyModule klazz, String name) {
        context.preMethodFrameOnly(klazz, name, self, Block.NULL_BLOCK);
        DynamicScope currentScope = context.getCurrentScope();
        context.pushScope(new ManyVarsDynamicScope(currentScope.getStaticScope(), currentScope));
        GIL.acquire();
    }

    /** see {@link #pre(ThreadContext, IRubyObject, RubyModule, String)}  */
    static void pre(ThreadContext context, IRubyObject self, RubyModule klazz, String name, Block block) {
        context.preMethodFrameOnly(klazz, name, self, block);
        DynamicScope currentScope = context.getCurrentScope();
        context.pushScope(new ManyVarsDynamicScope(currentScope.getStaticScope(), currentScope));
        GIL.acquire();
    }

    /** 
     * Release the {@link GIL} and pop the call frame and scope.
     */
    static void post(ThreadContext context) {
        GIL.release();
        context.postMethodFrameAndScope();
    }

    final Native getNativeInstance() {
        return nativeInstance;
    }

    
    @Override
    public IRubyObject call(ThreadContext context, IRubyObject recv, RubyModule clazz,
            String name, IRubyObject[] args) {
        pre(context, recv, getImplementationClass(), name);
        try {
            return getNativeInstance().callMethod(context, function, recv, arity.getValue(), args);
        } finally {
            post(context);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject recv, RubyModule clazz,
            String name, IRubyObject[] args, Block block) {

        pre(context, recv, getImplementationClass(), name, block);
        try {
            return getNativeInstance().callMethod(context, function, recv, arity.getValue(), args);
        } finally {
            post(context);
        }
    }

}
