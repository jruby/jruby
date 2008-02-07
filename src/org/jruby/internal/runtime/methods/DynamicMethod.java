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
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2005 Thomas E Enebo <enebo@acm.org>
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

import org.jruby.MetaClass;
import org.jruby.RubyModule;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author  jpetersen
 */
public abstract class DynamicMethod {
    protected RubyModule implementationClass;
    protected Visibility visibility;
    protected CallConfiguration callConfig;
    
    protected DynamicMethod(RubyModule implementationClass, Visibility visibility, CallConfiguration callConfig) {
        this.visibility = visibility;
        this.implementationClass = implementationClass;
        this.callConfig = callConfig;
    }

    /**
     * Call the method
     * @param context is the thread-specific information that this method is being invoked on
     * @param receiver 
     */
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, 
            String name, IRubyObject[] args, Block block);
    
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, 
            String name, IRubyObject[] args) {
        return call(context, self, clazz, name, args, Block.NULL_BLOCK);
    }
    
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name) {
        return call(context, self, klazz, name, IRubyObject.NULL_ARRAY, Block.NULL_BLOCK);
    }
    
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, Block block) {
        return call(context, self, klazz, name, IRubyObject.NULL_ARRAY, block);
    }
    
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg) {
        return call(context, self, klazz, name, new IRubyObject[] {arg}, Block.NULL_BLOCK);
    }
    
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg, Block block) {
        return call(context, self, klazz, name, new IRubyObject[] {arg}, block);
    }
    
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg1, IRubyObject arg2) {
        return call(context, self, klazz, name, new IRubyObject[] {arg1,arg2}, Block.NULL_BLOCK);
    }
    
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg1, IRubyObject arg2, Block block) {
        return call(context, self, klazz, name, new IRubyObject[] {arg1,arg2}, block);
    }
    
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        return call(context, self, klazz, name, new IRubyObject[] {arg1,arg2,arg3}, Block.NULL_BLOCK);
    }
    
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        return call(context, self, klazz, name, new IRubyObject[] {arg1,arg2,arg3}, block);
    }
    
    public abstract DynamicMethod dup();

    public boolean isCallableFrom(IRubyObject caller, CallType callType) {
        switch (visibility) {
        case PUBLIC:
            return true;
        case PRIVATE:
            return callType != CallType.NORMAL;
        case PROTECTED:
            RubyModule defined = getImplementationClass();
            while (defined.isIncluded()) {
                defined = defined.getMetaClass();
            }
            
            // For visibility we need real meta class and not anonymous one from class << self
            if (defined instanceof MetaClass) defined = ((MetaClass) defined).getRealClass();

            return defined.isInstance(caller);
        }
        
        return true;
    }
    
    public RubyModule getImplementationClass() {
        return implementationClass;
    }

    public void setImplementationClass(RubyModule implClass) {
        implementationClass = implClass;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public boolean isUndefined() {
        return false;
    }

    public Arity getArity() {
        return Arity.optional();
    }
    
    public DynamicMethod getRealMethod() {
        return this;
    }

    public CallConfiguration getCallConfig() {
        return callConfig;
    }

    public void setCallConfig(CallConfiguration callConfig) {
        this.callConfig = callConfig;
    }
    
    /**
     * Returns true if this method is backed by native (i.e. Java) code
     */
    public boolean isNative() {
        return false;
    }
}
