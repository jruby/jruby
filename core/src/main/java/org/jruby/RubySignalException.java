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

import static jnr.constants.platform.Signal.NSIG;

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.SignalException;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import static org.jruby.runtime.Visibility.PRIVATE;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.TypeConverter;

/**
 * The Java representation of a Ruby SignalException.
 *
 * @see SignalException
 */
@JRubyClass(name="SignalException", parent="Exception")
public class RubySignalException extends RubyException {
    protected RubySignalException(Ruby runtime, RubyClass exceptionClass) {
        super(runtime, exceptionClass);
    }

    @Override
    protected RaiseException constructThrowable(String message) {
        return new SignalException(message, this);
    }

    static RubyClass define(Ruby runtime, RubyClass exceptionClass) {
        RubyClass signalExceptionClass = runtime.defineClass("SignalException", exceptionClass, RubySignalException::new);
        signalExceptionClass.defineAnnotatedMethods(RubySignalException.class);

        return signalExceptionClass;
    }

    @JRubyMethod(optional = 2, visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args, Block block) {
        final Ruby runtime = context.runtime;
        int argnum = 1;
        IRubyObject sig = context.nil;
        long _signo;
        int argc = args.length;

        if (argc > 0) {
            sig = TypeConverter.checkIntegerType(runtime, args[0], "to_int");

            if (sig.isNil()) {
                sig = args[0];
            } else {
                argnum = 2;
            }
        }

        Arity.checkArgumentCount(runtime, args, 1, argnum);

        if (argnum == 2) {
            _signo = sig.convertToInteger().getLongValue();
            if (_signo < 0 || _signo > NSIG.longValue()) {
                throw runtime.newArgumentError("invalid signal number (" + _signo + ")");
            }

            if (argc > 1) {
                sig = args[1];
            } else {
                sig = runtime.newString(RubySignal.signmWithPrefix(RubySignal.signo2signm(_signo)));
            }
        } else {
            String signm = sig.toString();
            _signo = RubySignal.signm2signo(RubySignal.signmWithoutPrefix(signm));

            if (_signo == 0) {
                throw runtime.newArgumentError("unsupported name " + sig);
            }

            sig = runtime.newString(RubySignal.signmWithPrefix(signm));
        }

        super.initialize(new IRubyObject[]{sig}, block);
        this.signo = runtime.newFixnum(_signo);

        return this;
    }

    @JRubyMethod
    public IRubyObject signo(ThreadContext context) {
        assert signo != null;

        if (signo == RubyBasicObject.UNDEF) return context.nil;

        return signo;
    }

    @JRubyMethod(name = {"message","signm"})
    @Override
    public IRubyObject message(ThreadContext context) {
        return super.message(context);
    }

    private IRubyObject signo;
}
