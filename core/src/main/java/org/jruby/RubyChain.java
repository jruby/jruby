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

import org.jruby.runtime.Block;

import org.jruby.runtime.Helpers;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.util.ByteList;

import static org.jruby.RubyEnumerator.enumeratorizeWithSize;
import static org.jruby.RubyEnumerator.SizeFn;

/**
 * Implements Enumerator::Chain
 */
@JRubyClass(name = "Enumerator::Chain")
public class RubyChain extends RubyObject {

    private IRubyObject[] enums;
    private int pos = -1;

    public static RubyClass createChainClass(Ruby runtime, RubyClass enumeratorModule) {
        RubyClass chainc = runtime.defineClassUnder("Chain", runtime.getObject(), RubyChain::new, enumeratorModule);

        chainc.includeModule(runtime.getEnumerable());
        chainc.defineAnnotatedMethods(RubyChain.class);

        return chainc;
    }

    public RubyChain(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }

    public RubyChain(Ruby runtime, RubyClass klass, IRubyObject[] enums) {
        super(runtime, klass);
        this.enums = enums;
    }

    //
    @JRubyMethod(name = "initialize", rest = true, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        checkFrozen();

        enums = new IRubyObject[args.length];
        System.arraycopy(args, 0, enums, 0, args.length);

        return this;
    }

    public static RubyChain newChain(ThreadContext context, IRubyObject[] enums) {
        return new RubyChain(context.runtime, context.runtime.getChain(), enums);
    }

    // enum_chain_each
    @JRubyMethod(rest = true)
    public IRubyObject each(ThreadContext context, IRubyObject[] args, Block block) {
        if (!block.isGiven()) {
            return RubyEnumerator.enumeratorizeWithSize(context, this, "each", args, RubyChain::size);
        }

        for (int i = 0; i < enums.length; i++) {
            pos = i;
            Helpers.invoke(context, enums[i], "each", args, block);
        }

        return this;
    }

    /**
     * A size method suitable for lambda method reference implementation of {@link SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])}
     *
     * @see SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])
     */
    private static IRubyObject size(ThreadContext context, RubyChain self, IRubyObject[] args) {
        return self.size(context);
    }

    // enum_chain_rewind
    @JRubyMethod
    public IRubyObject rewind(ThreadContext context) {
        for (int i = pos; 0 <= i && pos < enums.length; i--) {
            Helpers.invokeChecked(context, enums[i], "rewind");
            pos -= 1;
        }

        return this;
    }

    @Override
    public final IRubyObject inspect() {
        return inspect(getRuntime().getCurrentContext());
    }

    @JRubyMethod
    public RubyString inspect(ThreadContext context) {

        ByteList str = new ByteList();
        str.append('#').append('<');
        str.append(getMetaClass().getRealClass().getName().getBytes());
        str.append(':').append(' ').append('[');
        for (int i = 0; i < enums.length - 1; i++) {
            str.append(RubyObject.inspect(context, enums[i]).getByteList());
            str.append(',').append(' ');
        }
        if (enums.length > 0) {
            str.append(RubyObject.inspect(context, enums[enums.length - 1]).getByteList());
        }
        str.append(']').append('>');

        return RubyString.newStringLight(context.runtime, str);
    }

    // enum_chain_size
    @JRubyMethod
    public IRubyObject size(ThreadContext context) {
        return enumChainTotalSize(context, enums);
    }

    // enum_chain_total_size
    private static IRubyObject enumChainTotalSize(ThreadContext context, IRubyObject[] args) {

        RubyFixnum total = RubyFixnum.zero(context.runtime);
        for (int i = 0; i < args.length; i++) {
            IRubyObject size = args[i].respondsTo("size") ? args[i].callMethod(context, "size") : context.nil;

            if (size.isNil() || (size instanceof RubyFloat) && size.equals(context.runtime.getFloat().getConstant("INFINITY"))) {
                return size;
            }

            if (!(size instanceof RubyInteger)) {
                return context.nil;
            }

            total = (RubyFixnum)total.callMethod("+", size);
        }

        return total;
    }

    @JRubyMethod(name = "+", required = 1)
    public IRubyObject op_plus(ThreadContext context, IRubyObject obj) {
        return RubyChain.newChain(context, new IRubyObject[] {this, obj});
    }
    
    @JRubyMethod(name = "dup")
    @Override
    public IRubyObject dup() {
        RubyChain copy = (RubyChain) super.dup();
        copy.enums = this.enums;
        copy.pos = this.pos;

        return copy;
    }
}