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
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004-2005 Thomas E Enebo <enebo@acm.org>
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

import org.jruby.RubyModule;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * This class represents a singleton type of method used as a marker for
 * missing or "undef'ed" methods. Only one instance is ever created, and it
 * can't be invoked.
 */
public class UndefinedMethod extends DynamicMethod {
    public static final UndefinedMethod INSTANCE = new UndefinedMethod();

    /**
     * Constructor for the one UndefinedMethod instance.
     */
    private UndefinedMethod() {
    }

    /**
     * The one implementation of call, which throws an exception because
     * UndefinedMethod can't be invoked.
     * 
     * @see DynamicMethod.call
     */
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule klazz, String name, IRubyObject[] args, Block block) {
        throw new UnsupportedOperationException();
    }
    
    /**
     * A dummy implementation of dup that just returns the singleton instance.
     * 
     * @return The singleton instance
     */
    public DynamicMethod dup() {
        return INSTANCE;
    }

    /**
     * Retrieve the singleton instance.
     * 
     * @return The singleton instance
     */
    public static UndefinedMethod getInstance() {
        return INSTANCE;
    }

    /**
     * Dummy override of setImplementationClass that does nothing.
     * 
     * @param implClass Ignored
     */
    @Override
    public void setImplementationClass(RubyModule implClass) {
        // UndefinedMethod should be immutable
    }

    /**
     * Dummy implementation of setVisibility that does nothing.
     * 
     * @param visibility Ignored
     */
    @Override
    public void setVisibility(Visibility visibility) {
        // UndefinedMethod should be immutable
    }

    /**
     * Dummy implementation of setCallConfig that does nothing.
     * 
     * @param callConfig Ignored
     */
    @Override
    public void setCallConfig(CallConfiguration callConfig) {
        // UndefinedMethod should be immutable
    }
    
    /**
     * UndefinedMethod is always visible because it's only used as a marker for
     * missing or undef'ed methods.
     * 
     * @param caller The calling object
     * @param callType The type of call
     * @return true always
     */
    @Override
    public boolean isCallableFrom(IRubyObject caller, CallType callType) {
        return true;
    }
}
