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

import org.jruby.IRuby;
import org.jruby.RubyModule;
import org.jruby.runtime.Arity;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ICallable;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author  jpetersen
 */
public abstract class AbstractCallable implements ICallable {
    protected RubyModule implementationClass;
    protected Visibility visibility;
    
    protected AbstractCallable(RubyModule implementationClass, Visibility visibility) {
        this.implementationClass = implementationClass;
        this.visibility = visibility;
    }
    
    protected AbstractCallable(Visibility visibility) {
        this.visibility = visibility;
    }
    
    /**
     * The base behavior is to do no scope/frame manipulation at all, depending on the caller. Methods (via
     * AbstractMethod) add their own behavior for scoping/framing, and non-methods (Evaluate/IterateCallable)
     * are only invoked from within ThreadContext.yield. Eventually TC.yield scoping/framing may be moved into
     * superclass of the two non-methods.
     */
    public IRubyObject call(IRuby runtime, IRubyObject receiver, RubyModule lastClass, String name, IRubyObject[] args, boolean noSuper) {
        return internalCall(runtime, receiver, lastClass, name, args, noSuper);
    }
    
    public String getOriginalName() {
    	return null;
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

    public boolean isCallableFrom(IRubyObject caller, CallType callType) {
        // default to true
        return true;
    }
    
    // default to false; only non-kernel module methods need implementer
    public boolean needsImplementer() {
        return false;
    }
}
