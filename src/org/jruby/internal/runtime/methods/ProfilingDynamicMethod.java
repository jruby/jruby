/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2010 Charles Oliver Nutter <headius@headius.com>
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
package org.jruby.internal.runtime.methods;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.exceptions.JumpException.BreakJump;
import org.jruby.exceptions.JumpException.ReturnJump;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author headius
 */
public class ProfilingDynamicMethod extends DynamicMethod {
    private final DynamicMethod delegate;

    public ProfilingDynamicMethod(DynamicMethod delegate) {
        super(delegate.getImplementationClass(), delegate.getVisibility(), delegate.getCallConfig());
        this.delegate = delegate;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
        int previousSerial = context.profileEnter((int)getSerialNumber());
        try {
            return delegate.call(context, self, clazz, name);
        } finally {
            context.profileExit(previousSerial);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
        int previousSerial = context.profileEnter((int)getSerialNumber());
        try {
            return delegate.call(context, self, clazz, name, arg0);
        } finally {
            context.profileExit(previousSerial);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
        int previousSerial = context.profileEnter((int)getSerialNumber());
        try {
            return delegate.call(context, self, clazz, name, arg0, arg1);
        } finally {
            context.profileExit(previousSerial);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        int previousSerial = context.profileEnter((int)getSerialNumber());
        try {
            return delegate.call(context, self, clazz, name, arg0, arg1, arg2);
        } finally {
            context.profileExit(previousSerial);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
        int previousSerial = context.profileEnter((int)getSerialNumber());
        try {
            return delegate.call(context, self, clazz, name, args);
        } finally {
            context.profileExit(previousSerial);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        int previousSerial = context.profileEnter((int)getSerialNumber());
        try {
            return delegate.call(context, self, clazz, name, block);
        } finally {
            context.profileExit(previousSerial);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
        int previousSerial = context.profileEnter((int)getSerialNumber());
        try {
            return delegate.call(context, self, clazz, name, arg0, block);
        } finally {
            context.profileExit(previousSerial);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        int previousSerial = context.profileEnter((int)getSerialNumber());
        try {
            return delegate.call(context, self, clazz, name, arg0, arg1, block);
        } finally {
            context.profileExit(previousSerial);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        int previousSerial = context.profileEnter((int)getSerialNumber());
        try {
            return delegate.call(context, self, clazz, name, arg0, arg1, arg2, block);
        } finally {
            context.profileExit(previousSerial);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        int previousSerial = context.profileEnter((int)getSerialNumber());
        try {
            return delegate.call(context, self, clazz, name, args, block);
        } finally {
            context.profileExit(previousSerial);
        }
    }

    @Override
    public DynamicMethod dup() {
        return new ProfilingDynamicMethod(delegate.dup());
    }

    @Override
    public void setVisibility(Visibility visibility) {
        delegate.setVisibility(visibility);
    }

    @Override
    public void setNativeCall(Class nativeTarget, String nativeName, Class nativeReturn, Class[] nativeSignature, boolean statik) {
        delegate.setNativeCall(nativeTarget, nativeName, nativeReturn, nativeSignature, statik);
    }

    @Override
    public void setIsBuiltin(boolean isBuiltin) {
        delegate.setIsBuiltin(isBuiltin);
    }

    @Override
    public void setImplementationClass(RubyModule implClass) {
        delegate.setImplementationClass(implClass);
    }

    @Override
    public void setCallConfig(CallConfiguration callConfig) {
        delegate.setCallConfig(callConfig);
    }

    @Override
    public boolean isNative() {
        return delegate.isNative();
    }

    @Override
    public boolean isCallableFrom(IRubyObject caller, CallType callType) {
        return delegate.isCallableFrom(caller, callType);
    }

    @Override
    public boolean isBuiltin() {
        return delegate.isBuiltin();
    }

    @Override
    public Visibility getVisibility() {
        return delegate.getVisibility();
    }

    @Override
    public long getSerialNumber() {
        return delegate.getSerialNumber();
    }

    @Override
    public DynamicMethod getRealMethod() {
        return delegate.getRealMethod();
    }

    @Override
    protected RubyModule getProtectedClass() {
        return delegate.getProtectedClass();
    }

    @Override
    public NativeCall getNativeCall() {
        return delegate.getNativeCall();
    }

    @Override
    public RubyModule getImplementationClass() {
        return delegate.getImplementationClass();
    }

    @Override
    public CallConfiguration getCallConfig() {
        return delegate.getCallConfig();
    }

    @Override
    public Arity getArity() {
        return delegate.getArity();
    }
}
