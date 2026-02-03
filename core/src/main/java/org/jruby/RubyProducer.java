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

import org.jruby.exceptions.StopIteration;
import org.jruby.runtime.Block;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Convert.asFloat;

/**
 * Implements Enumerator::Producer
 */
@JRubyClass(name = "Enumerator::Producer")
public class RubyProducer extends RubyObject {

    private IRubyObject init;
    private Block producerBlock;
    private IRubyObject size;

    public static RubyClass createProducerClass(ThreadContext context, RubyClass Object, RubyClass Enumerator, RubyModule Enumerable) {
        return Enumerator.defineClassUnder(context, "Producer", Object, RubyProducer::new).
                include(context, Enumerable).
                defineMethods(context, RubyProducer.class);
    }

    public RubyProducer(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }

    public RubyProducer(Ruby runtime, RubyClass klass, IRubyObject init, final Block block) {
        this(runtime, klass, init, block, runtime.getNil());
    }

    public RubyProducer(Ruby runtime, RubyClass klass, IRubyObject init, final Block block, IRubyObject size) {
        super(runtime, klass);
        this.init = init;
        this.producerBlock = block;
        this.size = size;
    }

    public static RubyProducer newProducer(ThreadContext context, IRubyObject init, final Block block) {
        return newProducer(context, init, block, context.nil);
    }

    public static RubyProducer newProducer(ThreadContext context, IRubyObject init, final Block block, IRubyObject size) {
        return new RubyProducer(context.runtime, context.runtime.getProducer(), init, block, size);
    }

    /** MRI: producer_size
     */
    public static IRubyObject size(ThreadContext context, RubyProducer self, IRubyObject[] args) {
        IRubyObject size = self.size;

        if (size.isNil()) return size;
        if (size instanceof RubyInteger || size instanceof RubyFloat) return size;

        return sites(context).call.call(context, self, size);
    }

    @JRubyMethod(rest = true)
    public IRubyObject each(ThreadContext context, IRubyObject[] args, final Block block) {
        IRubyObject cur;

        if (init == null) {
            cur = context.nil;
        } else {
            cur = init;
            block.yield(context, cur);
        }

        for (;;) {
            try {
                cur = producerBlock.call(context, cur);
                block.yield(context, cur);
            } catch(StopIteration si) {
                break;
            }
        }

        return this;
    }

    private static JavaSites.ProducerSites sites(ThreadContext context) {
        return context.sites.Producer;
    }
}