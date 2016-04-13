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
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Ruby's StopIteration exception.
 * @see RubyEnumerator
 * @author kares
 */
@JRubyClass(name="StopIteration", parent="IndexError")
public class RubyStopIteration extends RubyException {

    private static final ObjectAllocator ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyStopIteration(runtime, klass);
        }
    };

    static RubyClass createStopIterationClass(Ruby runtime, RubyClass superClass) {
        RubyClass StopIteration = runtime.defineClass("StopIteration", superClass, ALLOCATOR);
        StopIteration.defineAnnotatedMethods(RubyStopIteration.class);
        return StopIteration;
    }

    public static RubyStopIteration newInstance(ThreadContext context, IRubyObject result, String message) {
        final Ruby runtime = context.runtime;
        RubyClass StopIteration = runtime.getStopIteration();
        final IRubyObject msg = message == null ? context.nil : runtime.newString(message);
        RubyStopIteration instance = (RubyStopIteration)
                StopIteration.newInstance(context, msg, Block.NULL_BLOCK);
        instance.result = result;
        return instance;
    }

    private IRubyObject result;

    protected RubyStopIteration(Ruby runtime, RubyClass exceptionClass) {
        super(runtime, exceptionClass);
    }

    @JRubyMethod
    public IRubyObject result() {
        return result == null ? getRuntime().getNil() : result;
    }

}
