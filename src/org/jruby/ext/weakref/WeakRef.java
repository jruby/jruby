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
package org.jruby.ext.weakref;

import java.lang.ref.WeakReference;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyClass;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import static org.jruby.runtime.Visibility.*;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author headius
 */
@JRubyClass(name="WeakRef", parent="Delegator")
public class WeakRef extends RubyObject {
    private WeakReference<IRubyObject> ref;
    
    public static final ObjectAllocator WEAKREF_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
            return new WeakRef(runtime, klazz);
        }
    };
    
    @JRubyClass(name="WeakRef::RefError", parent="StandardError")
    public static class RefError {}

    public WeakRef(Ruby runtime, RubyClass klazz) {
        super(runtime, klazz);
    }
    
    @JRubyMethod(name = "__getobj__")
    public IRubyObject getobj() {
        IRubyObject obj = ref.get();
        
        if (obj == null) {
            // FIXME weakref.rb also does caller(2) here for the backtrace
            throw newRefError("Illegal Reference - probably recycled");
        }
        
        return obj;
    }

    @JRubyMethod(name = "__setobj__")
    public IRubyObject setobj(IRubyObject obj) {
        return getRuntime().getNil();
    }
    
    @JRubyMethod(name = "new", required = 1, meta = true)
    public static IRubyObject newInstance(IRubyObject clazz, IRubyObject arg) {
        WeakRef weakRef = (WeakRef)((RubyClass)clazz).allocate();
        
        weakRef.callInit(new IRubyObject[] {arg}, Block.NULL_BLOCK);
        
        return weakRef;
    }

    // framed for invokeSuper
    @JRubyMethod(frame = true, visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject obj) {
        ref = new WeakReference<IRubyObject>(obj);
        
        return Helpers.invokeSuper(context, this, obj, Block.NULL_BLOCK);
    }
    
    @JRubyMethod(name = "weakref_alive?")
    public IRubyObject weakref_alive_p() {
        return ref.get() != null ? getRuntime().getTrue() : getRuntime().getFalse();
    }
    
    private RaiseException newRefError(String message) {
        Ruby runtime = getRuntime();
        ThreadContext context = runtime.getCurrentContext();
        RubyException exception =
                (RubyException)runtime.getClass("WeakRef").getClass("RefError").newInstance(context,
                new IRubyObject[] {runtime.newString(message)}, Block.NULL_BLOCK);
        
        RaiseException re = new RaiseException(exception);
        return re;
    }
}
