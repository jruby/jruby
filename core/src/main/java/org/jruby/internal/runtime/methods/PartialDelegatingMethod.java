/*
 **** BEGIN LICENSE BLOCK *****
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
import org.jruby.internal.runtime.AbstractIRMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;

/**
 * This is similar to {@link DelegatingDynamicMethod} except that it does not delegate most properties of DynamicMethod.
 * Visibility, etc, set on an instance of {@link PartialDelegatingMethod} will not be delegated to the contained method.
 *
 * This type of method is used primarily for altering the visibility of a parent class's method in a child class.
 *
 * Note that {@link AliasMethod} is not a suitable substitute since it always passes the method's original name to the
 * delegate, and {@link DelegatingDynamicMethod} is not a suitable substitute since it delegates all properties to the
 * delegate.
 *
 * @author jpetersen
 */
public class PartialDelegatingMethod extends DynamicMethod {
    private final CacheEntry entry;
    private final boolean findImplementer;

    /**
     * Constructor for PartialDelegatingMethod.
     * @param visibility
     */
    public PartialDelegatingMethod(RubyModule implementationClass, CacheEntry entry, Visibility visibility) {
        super(implementationClass, visibility, entry.method.getName());
        this.entry = entry;
        boolean findImplementer = true;

        if (entry.method instanceof AbstractIRMethod) {
            findImplementer = ((AbstractIRMethod) entry.method).needsToFindImplementer();
        }

        this.findImplementer = findImplementer;
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name) {
        return entry.method.call(context, self, calculateSourceModule(self, calculateSourceModule(self, klazz)), name);
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg) {
        return entry.method.call(context, self, calculateSourceModule(self, calculateSourceModule(self, klazz)), name, arg);
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg1, IRubyObject arg2) {
        return entry.method.call(context, self, calculateSourceModule(self, calculateSourceModule(self, klazz)), name, arg1, arg2);
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        return entry.method.call(context, self, calculateSourceModule(self, calculateSourceModule(self, klazz)), name, arg1, arg2, arg3);
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject[] args) {
        return entry.method.call(context, self, calculateSourceModule(self, klazz), name, args);
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, Block block) {
        return entry.method.call(context, self, calculateSourceModule(self, klazz), name, block);
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg1, Block block) {
        return entry.method.call(context, self, calculateSourceModule(self, klazz), name, arg1, block);
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg1, IRubyObject arg2, Block block) {
        return entry.method.call(context, self, calculateSourceModule(self, klazz), name, arg1, arg2, block);
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        return entry.method.call(context, self, calculateSourceModule(self, klazz), name, arg1, arg2, arg3, block);
    }

    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject[] args, Block block) {
        return entry.method.call(context, self, calculateSourceModule(self, klazz), name, args, block);
    }

    public DynamicMethod dup() {
        return new PartialDelegatingMethod(implementationClass, entry, getVisibility());
    }

    public long getSerialNumber() {
        return entry.method.getSerialNumber();
    }

    @Override
    public DynamicMethod getRealMethod() {
        return entry.method.getRealMethod();
    }

    @Deprecated(since = "9.3.0.0") @Override
    public Arity getArity() {
        return entry.method.getArity();
    }

    @Override
    public Signature getSignature() {
        return entry.method.getSignature();
    }

    @Override
    public RubyModule getDefinedClass() {
        RubyModule definedClass = this.definedClass;
        if (definedClass != null) return definedClass;
        return entry.method.getDefinedClass();
    }

    // MRI: vm_call0_body and aliased_callable_method_entry
    /* FIXME: This is not quite right. It appears that MRI does this logic at call time, but I believe
              the calculated class is cached somewhere along with the "callable" method entry. The code
              below means all aliases in modules will do the implementer search, unless we can detect
              that the related method does not need "super". We can improve this at cache time in either
              CacheEntry logic or CallSite logic.
     */
    private RubyModule calculateSourceModule(IRubyObject self, RubyModule incomingSourceModule) {
        if (findImplementer) {
            return Helpers.findImplementerIfNecessary(self.getMetaClass(), entry.method.getImplementationClass());
        } else {
            return incomingSourceModule;
        }
    }
}
