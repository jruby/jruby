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
 * Copyright (C) 2012 The JRuby Community <www.jruby.org>
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
package org.jruby.internal.runtime.methods;

import java.lang.invoke.MethodHandle;
import org.jruby.RubyModule;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A DynamicMethod backed by one or more java.lang.invoke.MethodHandle objects.
 * 
 * The MethodHandles contained in this method are used by invokedynamic-based
 * call site binding to bind more directly to the target. Previously, the
 * handles required for binding were built at the call site by inspecting the
 * DynamicMethod that came in. With the newer logic, handles are created at
 * bind time, so they're already ready and so we don't need to generate our
 * own handle classes that might only get used by the interpreter.
 *
 * @author headius
 */
public class HandleMethod extends DynamicMethod implements MethodArgs2 {
    private final MethodHandle[] targets;
    private final MethodHandle target0;
    private final MethodHandle target1;
    private final MethodHandle target2;
    private final MethodHandle target3;
    private final MethodHandle target4;
    private final String parameterDesc;

    public HandleMethod(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig, MethodHandle[] targets, String parameterDesc) {
        super(implementationClass, visibility, callConfig);
        this.targets = targets;
        this.target0 = targets[0];
        this.target1 = targets[1];
        this.target2 = targets[2];
        this.target3 = targets[3];
        this.target4 = targets[4];
        this.parameterDesc = parameterDesc;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        try {
            return (IRubyObject) target4.invokeWithArguments(context, self, args, block);
        } catch (Throwable t) {
            Helpers.throwException(t);
            return null;
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        if (target0 == null) {
            return call(context, self, clazz, name, IRubyObject.NULL_ARRAY, block);
        }
        try {
            return (IRubyObject) target0.invokeWithArguments(context, self, block);
        } catch (Throwable t) {
            Helpers.throwException(t);
            return null;
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
        if (target1 == null) {
            return call(context, self, clazz, name, new IRubyObject[]{arg0}, block);
        }
        try {
            return (IRubyObject) target1.invokeWithArguments(context, self, arg0, block);
        } catch (Throwable t) {
            Helpers.throwException(t);
            return null;
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        if (target2 == null) {
            return call(context, self, clazz, name, new IRubyObject[]{arg0, arg1}, block);
        }
        try {
            return (IRubyObject) target2.invokeWithArguments(context, self, arg0, arg1, block);
        } catch (Throwable t) {
            Helpers.throwException(t);
            return null;
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        if (target3 == null) {
            return call(context, self, clazz, name, new IRubyObject[]{arg0, arg1, arg2}, block);
        }
        try {
            return (IRubyObject) target3.invokeWithArguments(context, self, arg0, arg1, arg2, block);
        } catch (Throwable t) {
            Helpers.throwException(t);
            return null;
        }
    }

    @Override
    public DynamicMethod dup() {
        return new HandleMethod(implementationClass, visibility, callConfig, targets, parameterDesc);
    }

    @Override
    public String[] getParameterList() {
        if (parameterDesc != null && parameterDesc.length() > 0) {
            return parameterDesc.split(";");
        } else {
            return new String[0];
        }
    }

    public MethodHandle getHandle(int arity) {
        return targets[arity];
    }
    
}
