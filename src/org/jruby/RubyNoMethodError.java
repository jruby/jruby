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
import org.jruby.runtime.builtin.IRubyObject;

@JRubyClass(name="NoMethodError", parent="NameError")
public class RubyNoMethodError extends RubyNameError {
    private IRubyObject args;

    private static final ObjectAllocator NOMETHODERROR_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyNoMethodError(runtime, klass);
        }
    };

    public static RubyClass createNoMethodErrorClass(Ruby runtime, RubyClass nameErrorClass) {
        RubyClass noMethodErrorClass = runtime.defineClass("NoMethodError", nameErrorClass, NOMETHODERROR_ALLOCATOR);
        
        noMethodErrorClass.defineAnnotatedMethods(RubyNoMethodError.class);

        return noMethodErrorClass;
    }

    protected RubyNoMethodError(Ruby runtime, RubyClass exceptionClass) {
        super(runtime, exceptionClass, exceptionClass.getName());
        this.args = runtime.getNil();
    }
    
    public RubyNoMethodError(Ruby runtime, RubyClass exceptionClass, String message, String name, IRubyObject args) {
        super(runtime, exceptionClass, message,  name);
        this.args = args;
    }    

    @JRubyMethod(optional = 3)
    public IRubyObject initialize(IRubyObject[] args, Block block) {
        if (args.length > 2) {
            this.args = args[args.length - 1];
            IRubyObject []tmpArgs = new IRubyObject[args.length - 1];
            System.arraycopy(args, 0, tmpArgs, 0, tmpArgs.length);
            args = tmpArgs;
        } else {
            this.args = getRuntime().getNil();
        }

        super.initialize(args, block);
        return this;
    }
    
    @JRubyMethod(name = "args")
    public IRubyObject args() {
        return args;
    }

    @Override
    public void copySpecialInstanceVariables(IRubyObject clone) {
        super.copySpecialInstanceVariables(clone);
        RubyNoMethodError exception = (RubyNoMethodError)clone;
        exception.args = args;
    }
}
