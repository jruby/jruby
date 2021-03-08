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

import static jnr.constants.platform.Signal.SIGINT;

import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyClass;
import org.jruby.exceptions.Interrupt;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Arity;
import org.jruby.runtime.builtin.IRubyObject;
import static org.jruby.runtime.Visibility.PRIVATE;
import org.jruby.util.ArraySupport;

/**
 * The Java representation of a Ruby Interrupt.
 *
 * @see Interrupt
 */
@JRubyClass(name="Interrupt", parent="SignalException")
public class RubyInterrupt extends RubySignalException {
    private static final ObjectAllocator INTERRUPT_ALLOCATOR = RubyInterrupt::new;

    static RubyClass define(Ruby runtime, RubyClass signalExceptionClass) {
        RubyClass interruptClass = runtime.defineClass("Interrupt", signalExceptionClass, INTERRUPT_ALLOCATOR);
        interruptClass.defineAnnotatedMethods(RubyInterrupt.class);
        return interruptClass;
    }

    protected RubyInterrupt(Ruby runtime, RubyClass exceptionClass) {
        super(runtime, exceptionClass);
    }

    @Override
    protected RaiseException constructThrowable(String message) {
        return new Interrupt(message, this);
    }

    @JRubyMethod(optional = 1, visibility = PRIVATE)
    @Override
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args, Block block) {
        final Ruby runtime = context.runtime;

        Arity.checkArgumentCount(runtime, args, 0, 1);

        IRubyObject signo = runtime.newFixnum(SIGINT);

        if (args.length > 0) {
            args = ArraySupport.newCopy(signo, args);
        } else {
            args = new IRubyObject[]{signo, runtime.newString("Interrupt")};
        }

        super.initialize(context, args, block);
        return this;
    }
}
