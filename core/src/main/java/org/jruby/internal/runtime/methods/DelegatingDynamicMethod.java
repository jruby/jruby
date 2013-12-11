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
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004-2005 Thomas E Enebo <enebo@acm.org>
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

import org.jruby.RubyModule;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.ivars.MethodData;

public abstract class DelegatingDynamicMethod extends DynamicMethod {
    protected final DynamicMethod delegate;

    public DelegatingDynamicMethod(DynamicMethod delegate) {
        super(delegate.getImplementationClass(), delegate.getVisibility(), delegate.getCallConfig());
        this.delegate = delegate;
    }

    public DynamicMethod getDelegate() {
        return delegate;
    }
    
    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name) {
        return delegate.call(context, self, klazz, name);
    }
    
    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg) {
        return delegate.call(context, self, klazz, name, arg);
    }
    
    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg1, IRubyObject arg2) {
        return delegate.call(context, self, klazz, name, arg1, arg2);
    }
    
    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        return delegate.call(context, self, klazz, name, arg1, arg2, arg3);
    }
    
    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject[] args) {
        return delegate.call(context, self, klazz, name, args);
    }
    
    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, Block block) {
        return delegate.call(context, self, klazz, name, block);
    }
    
    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg1, Block block) {
        return delegate.call(context, self, klazz, name, arg1, block);
    }
    
    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg1, IRubyObject arg2, Block block) {
        return delegate.call(context, self, klazz, name, arg1, arg2, block);
    }
    
    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        return delegate.call(context, self, klazz, name, arg1, arg2, arg3, block);
    }
    
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject[] args, Block block) {
        return delegate.call(context, self, klazz, name, args, block);
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

    @Override
    public String getName() {
        return delegate.getName();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void setName(String name) {
        delegate.setName(name);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public boolean isNotImplemented() {
        return delegate.isNotImplemented();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void setNotImplemented(boolean setNotImplemented) {
        delegate.setNotImplemented(setNotImplemented);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public MethodData getMethodData() {
        return delegate.getMethodData();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void setHandle(Object handle) {
        delegate.setHandle(handle);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public Object getHandle() {
        return delegate.getHandle();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void setNativeCall(int args, boolean block, NativeCall nativeCall) {
        delegate.setNativeCall(args, block, nativeCall);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public NativeCall getNativeCall(int args, boolean block) {
        return delegate.getNativeCall(args, block);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void setNativeCall(Class nativeTarget, String nativeName, Class nativeReturn, Class[] nativeSignature, boolean statik, boolean java) {
        delegate.setNativeCall(nativeTarget, nativeName, nativeReturn, nativeSignature, statik, java);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public DynamicMethod dup() {
        return new ProfilingDynamicMethod(delegate.dup());
    }

    @Override
    /**
     * We override equals so that for method identity checks we treat delegating wrappers the same as the original
     * methods. See e.g. RubyClass.finvokeChecked and its method_missing comparison.
     */
    public boolean equals(Object other) {
        if (other instanceof DelegatingDynamicMethod) {
            return delegate.equals(((DelegatingDynamicMethod)other).getDelegate());
        }

        return delegate.equals(other);
    }
}
