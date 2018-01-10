/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
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

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author kares
 */
@JRubyClass(name="UncaughtThrowError", parent="ArgumentError")
public class RubyUncaughtThrowError extends RubyException {

    private IRubyObject tag, value;

    private static final ObjectAllocator ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyUncaughtThrowError(runtime, klass);
        }
    };

    static RubyClass createUncaughtThrowErrorClass(Ruby runtime, RubyClass argumentError) {
        RubyClass UncaughtThrowError = runtime.defineClass("UncaughtThrowError", argumentError, ALLOCATOR);
        UncaughtThrowError.defineAnnotatedMethods(RubyUncaughtThrowError.class);
        return UncaughtThrowError;
    }

    protected RubyUncaughtThrowError(Ruby runtime, RubyClass exceptionClass) {
        super(runtime, exceptionClass, exceptionClass.getName());
        // this.tag = this.value = runtime.getNil();
        this.message = runtime.getNil();
    }

    public static RubyUncaughtThrowError newUncaughtThrowError(final Ruby runtime,
        IRubyObject tag, IRubyObject value, RubyString message) {
        RubyUncaughtThrowError error = new RubyUncaughtThrowError(runtime, runtime.getUncaughtThrowError());
        error.tag = tag;
        error.value = value;
        error.message = message;
        return error;
    }

    @Override
    @JRubyMethod(required = 2, optional = 1, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(IRubyObject[] args, Block block) {
        this.tag = args[0]; this.value = args[1];
        if ( args.length > 2 ) this.message = args[2];
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
        if ( message.isNil() ) {
            return RubyString.newEmptyString(context.runtime);
        }
        if ( tag == null ) return message.asString();

        final RubyString str = message.asString();
        return str.op_format(context, RubyArray.newArray(context.runtime, tag));
    }

    @Override
    public void copySpecialInstanceVariables(IRubyObject clone) {
        super.copySpecialInstanceVariables(clone);
        RubyUncaughtThrowError exception = (RubyUncaughtThrowError) clone;
        exception.tag = tag;
        exception.value = value;
    }

}