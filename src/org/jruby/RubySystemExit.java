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

package org.jruby;

import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyClass;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import static org.jruby.runtime.Visibility.*;
import org.jruby.runtime.builtin.IRubyObject;

@JRubyClass(name="SystemExit", parent="Exception")
public class RubySystemExit extends RubyException {
    IRubyObject status;

    private static ObjectAllocator SYSTEMEXIT_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubySystemExit(runtime, klass);
        }
    };    
    
    public static RubyClass createSystemExitClass(Ruby runtime, RubyClass exceptionClass) {
        RubyClass systemExitClass = runtime.defineClass("SystemExit", exceptionClass, SYSTEMEXIT_ALLOCATOR);

        systemExitClass.defineAnnotatedMethods(RubySystemExit.class);
        
        return systemExitClass;
    }  
    
    public static RubySystemExit newInstance(Ruby runtime, int status, String message) {
        RubyClass exc = runtime.getSystemExit();
        IRubyObject[] exArgs = new IRubyObject[] {
                runtime.newFixnum(status),
                runtime.newString(message) };
        return (RubySystemExit) exc.newInstance(runtime.getCurrentContext(), exArgs, Block.NULL_BLOCK);
    }

    protected RubySystemExit(Ruby runtime, RubyClass exceptionClass) {
        super(runtime, exceptionClass);
        status = runtime.getNil();
    }

    @JRubyMethod(optional = 2, visibility = PRIVATE)
    public IRubyObject initialize(IRubyObject[]args, Block block) {
        status = RubyFixnum.zero(getRuntime());
        if (args.length > 0 && args[0] instanceof RubyFixnum) {
            status = args[0];
            IRubyObject[]tmpArgs = new IRubyObject[args.length - 1];
            System.arraycopy(args, 1, tmpArgs, 0, tmpArgs.length);
            args = tmpArgs;
        }
        super.initialize(args, block);
        return this;
    }

    @JRubyMethod
    public IRubyObject status() {
        return status;
    }

    @JRubyMethod(name = "success?")
    public IRubyObject success_p() {
        if (status.isNil()) return getRuntime().getTrue();
        if (status.equals(RubyFixnum.zero(getRuntime()))) return getRuntime().getTrue();
        return getRuntime().getFalse();
    }

}
