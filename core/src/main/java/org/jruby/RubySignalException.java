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

import static org.jruby.api.Convert.*;
import static org.jruby.api.Create.newString;
import static org.jruby.api.Define.defineClass;
import static org.jruby.api.Error.argumentError;
import static org.jruby.runtime.Visibility.PRIVATE;

import org.jruby.runtime.builtin.IRubyObject;

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

    static RubyClass define(ThreadContext context, RubyClass Exception) {
        return defineClass(context, "SignalException", Exception, RubySignalException::new).
                defineMethods(context, RubySignalException.class);
    }

    @JRubyMethod(required = 1, optional = 2, checkArity = false, visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args, Block block) {
        int argc = Arity.checkArgumentCount(context, args, 1, 2);
        int argnum = 1;
        IRubyObject sig = checkToInteger(context, args[0]);

        if (sig.isNil()) {
            sig = args[0];
            Arity.checkArgumentCount(context, args, 1, argnum);
        } else {
            argnum = 2;
        }

        long _signo;

        if (argnum == 2) {
            _signo = asLong(context, (RubyInteger) sig);
            if (_signo < 0 || _signo > NSIG.longValue()) throw argumentError(context, "invalid signal number (" + _signo + ")");

            sig = argc > 1 ? args[1] : newString(context, RubySignal.signmWithPrefix(RubySignal.signo2signm(_signo)));
        } else {
            String signm = sig.toString();
            _signo = RubySignal.signm2signo(RubySignal.signmWithoutPrefix(signm));

            if (_signo == 0) throw argumentError(context, "unsupported name " + sig);

            sig = newString(context, RubySignal.signmWithPrefix(signm));
        }

        super.initialize(new IRubyObject[]{sig}, block);
        this.signo = asFixnum(context, _signo);

        return this;
    }

    @JRubyMethod
    public IRubyObject signo(ThreadContext context) {
        assert signo != null;
        return signo == RubyBasicObject.UNDEF ? context.nil : signo;
    }

    @JRubyMethod(name = {"message","signm"})
    @Override
    public IRubyObject message(ThreadContext context) {
        return super.message(context);
    }

    private IRubyObject signo;
}
