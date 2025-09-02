/*
 ***** BEGIN LICENSE BLOCK *****
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

package org.jruby.runtime.invokedynamic;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.jruby.*;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CacheEntry;

import static java.lang.invoke.MethodHandles.*;

public class InvokeDynamicSupport {
    private static final Lookup LOOKUP = MethodHandles.lookup();
    private static final Module JRUBY_MODULE = InvokeDynamicSupport.class.getModule();

    ////////////////////////////////////////////////////////////////////////////
    // method_missing support code
    ////////////////////////////////////////////////////////////////////////////

    public static boolean methodMissing(CacheEntry entry, CallType callType, String name, IRubyObject caller) {
        DynamicMethod method = entry.method;
        return method.isUndefined() || (callType == CallType.NORMAL && !name.equals("method_missing") && !method.isCallableFrom(caller, callType));
    }

    public static IRubyObject callMethodMissing(CacheEntry entry, CallType callType, ThreadContext context, IRubyObject self, String name, IRubyObject arg) {
        return Helpers.selectMethodMissing(context, self, entry.method.getVisibility(), name, callType).call(context, self, self.getMetaClass(), name, arg, Block.NULL_BLOCK);
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Dispatch support methods
    ////////////////////////////////////////////////////////////////////////////

    public static RubyClass pollAndGetClass(ThreadContext context, IRubyObject self) {
        context.callThreadPoll();
        return RubyBasicObject.getMetaClass(self);
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Utility methods for lookup
    ////////////////////////////////////////////////////////////////////////////
    
    public static MethodHandle findStatic(Class target, String name, MethodType type) {
        try {
            JRUBY_MODULE.addReads(target.getModule());
            return LOOKUP.findStatic(target, name, type);
        } catch (NoSuchMethodException|IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static MethodHandle findVirtual(Class target, String name, MethodType type) {
        try {
            JRUBY_MODULE.addReads(target.getModule());
            return LOOKUP.findVirtual(target, name, type);
        } catch (NoSuchMethodException|IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }
}
