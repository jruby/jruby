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

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.UncaughtThrowError;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Create.newArray;
import static org.jruby.api.Create.newEmptyString;
import static org.jruby.api.Define.defineClass;

/**
 * The Java representation of a Ruby UncaughtThrowError.
 *
 * @see UncaughtThrowError
 * @author kares
 */
@JRubyClass(name="UncaughtThrowError", parent="ArgumentError")
public class RubyUncaughtThrowError extends RubyArgumentError {

    private IRubyObject tag, value;

    static RubyClass define(ThreadContext context, RubyClass ArgumentError) {
        return defineClass(context, "UncaughtThrowError", ArgumentError, RubyUncaughtThrowError::new).
                defineMethods(context, RubyUncaughtThrowError.class);
    }

    @Deprecated(since = "10.0")
    protected RubyUncaughtThrowError(Ruby runtime, RubyClass exceptionClass) {
        this(runtime.getCurrentContext(), exceptionClass);
    }

    protected RubyUncaughtThrowError(ThreadContext context, RubyClass exceptionClass) {
        super(context.runtime, exceptionClass, exceptionClass.getName(context));
        this.message = context.nil;
    }

    @Deprecated(since = "10.0")
    public static RubyUncaughtThrowError newUncaughtThrowError(final Ruby runtime,
                                                               IRubyObject tag, IRubyObject value, RubyString message) {
        return newUncaughtThrowError(runtime.getCurrentContext(), tag, value, message);
    }

    public static RubyUncaughtThrowError newUncaughtThrowError(ThreadContext context,
                                                               IRubyObject tag, IRubyObject value, RubyString message) {
        RubyUncaughtThrowError error = new RubyUncaughtThrowError(context, context.runtime.getUncaughtThrowError());
        error.tag = tag;
        error.value = value;
        error.message = message;
        return error;
    }

    @Override
    protected RaiseException constructThrowable(String message) {
        return new UncaughtThrowError(message, this);
    }

    @Deprecated
    @Override
    public IRubyObject initialize(IRubyObject[] args, Block block) {
        return initialize(getRuntime().getCurrentContext(), args, block);
    }

    @JRubyMethod(required = 2, optional = 1, checkArity = false, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args, Block block) {
        int argc = Arity.checkArgumentCount(context, args, 2, 3);

        this.tag = args[0]; this.value = args[1];
        if ( argc > 2 ) this.message = args[2];
        // makes no-sense for us to have a cause or does it ?!
        // super.initialize(NULL_ARRAY, block); // already set message
        return this;
    }

    @JRubyMethod
    public IRubyObject tag() { return tag; }

    @JRubyMethod
    public IRubyObject value() { return value; }

    @Override
    public RubyString to_s(ThreadContext context) {
        if (message.isNil()) return newEmptyString(context);
        if (tag == null) return message.asString();

        return message.asString().op_format(context, newArray(context, tag));
    }

    @Override
    public void copySpecialInstanceVariables(IRubyObject clone) {
        super.copySpecialInstanceVariables(clone);
        RubyUncaughtThrowError exception = (RubyUncaughtThrowError) clone;
        exception.tag = tag;
        exception.value = value;
    }

}