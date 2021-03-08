/***** BEGIN LICENSE BLOCK *****
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

package org.jruby;

import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyClass;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.SystemExit;
import org.jruby.runtime.Block;

import static org.jruby.runtime.Visibility.*;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * The Java representation of a Ruby SystemExit.
 *
 * @see SystemExit
 */
@JRubyClass(name="SystemExit", parent="Exception")
public class RubySystemExit extends RubyException {

    IRubyObject status;

    static RubyClass define(Ruby runtime, RubyClass exceptionClass) {
        RubyClass systemExitClass = runtime.defineClass("SystemExit", exceptionClass, RubySystemExit::new);

        systemExitClass.defineAnnotatedMethods(RubySystemExit.class);

        return systemExitClass;
    }

    public static RubySystemExit newInstance(Ruby runtime, int status, String message) {
        final RubyClass klass = runtime.getSystemExit();
        final IRubyObject[] args = new IRubyObject[] {
            runtime.newFixnum(status), runtime.newString(message)
        };
        return (RubySystemExit) klass.newInstance(runtime.getCurrentContext(), args, Block.NULL_BLOCK);
    }

    protected RubySystemExit(Ruby runtime, RubyClass exceptionClass) {
        super(runtime, exceptionClass);
        status = runtime.getNil();
    }

    @Override
    protected RaiseException constructThrowable(String message) {
        return new SystemExit(message, this);
    }

    @JRubyMethod(optional = 2, visibility = PRIVATE)
    @Override
    public IRubyObject initialize(IRubyObject[] args, Block block) {
        if (args.length > 0) {
            final IRubyObject arg = args[0];
            if (arg instanceof RubyFixnum) {
                this.status = arg;
                if (args.length > 1) this.message = args[1]; // (status, message)
            }
            else if (arg instanceof RubyBoolean) {
                final Ruby runtime = getRuntime();
                this.status = runtime.newFixnum( arg == runtime.getTrue() ? 0 : 1 );
                if (args.length > 1) this.message = args[1]; // (status, message)
            }
            else {
                this.message = arg;
                this.status = RubyFixnum.zero(getRuntime());
            }
        }
        else {
            this.status = RubyFixnum.zero(getRuntime());
        }
        super.initialize(NULL_ARRAY, block);
        return this;
    }

    @JRubyMethod
    public IRubyObject status() {
        return status;
    }

    @JRubyMethod(name = "success?")
    public IRubyObject success_p() {
        final Ruby runtime = getRuntime();
        final IRubyObject status = this.status;
        if ( status.isNil() ) return runtime.getTrue();
        if ( status == runtime.getTrue() || status == runtime.getFalse() ) return status;
        if ( status.equals(RubyFixnum.zero(runtime)) ) return runtime.getTrue();
        return runtime.getFalse();
    }

}
