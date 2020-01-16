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
import org.jruby.RubyFixnum;

import org.jruby.runtime.Helpers;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.util.ByteList;

import static org.jruby.RubyEnumerator.enumeratorizeWithSize;
import static org.jruby.RubyEnumerator.SizeFn;

import static org.jruby.runtime.Helpers.hashEnd;
import static org.jruby.runtime.Helpers.hashStart;
import static org.jruby.runtime.Helpers.murmurCombine;
import static org.jruby.runtime.Helpers.safeHash;

/**
 * Implements Enumerator::ArithmeticSequence
 */
@JRubyClass(name = "Enumerator::ArithmeticSequence")
public class RubyArithmeticSequence extends RubyObject {

    private IRubyObject begin;
    private IRubyObject end;
    private IRubyObject step;
    private IRubyObject excludeEnd;

    public static RubyClass createArithmeticSequenceClass(Ruby runtime, RubyClass enumeratorModule) {
        RubyClass sequencec = runtime.defineClassUnder("ArithmeticSequence", runtime.getObject(), new ObjectAllocator() {
            @Override
            public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                return new RubyArithmeticSequence(runtime, klazz);
            }
        }, enumeratorModule);

        sequencec.includeModule(runtime.getEnumerable());
        sequencec.defineAnnotatedMethods(RubyArithmeticSequence.class);

        RubyClass seqMetaClass = sequencec.getMetaClass();
        seqMetaClass.undefineMethod("new");

        return sequencec;
    }

    public RubyArithmeticSequence(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }

    public RubyArithmeticSequence(Ruby runtime, RubyClass klass, IRubyObject begin, IRubyObject end, IRubyObject step, IRubyObject excludeEnd) {
        super(runtime, klass);
        this.begin = begin;
        this.end = end;
        this.step = step;
        this.excludeEnd = excludeEnd;
    }

    public static RubyArithmeticSequence newArithmeticSequence(ThreadContext context, IRubyObject begin, IRubyObject end, IRubyObject step, IRubyObject excludeEnd) {
        return new RubyArithmeticSequence(context.runtime, context.runtime.getArithmeticSequence(), begin, end, step, excludeEnd);
    }

    // arith_seq_eq
    @JRubyMethod(name = "==")
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
        if (!(other instanceof RubyArithmeticSequence)) {
            return context.fals;
        }

        RubyArithmeticSequence aseqOther = (RubyArithmeticSequence)other;

        if (!Helpers.rbEqual(context, this.begin, aseqOther.begin).isTrue()) {
            return context.fals;
        }

        if (!Helpers.rbEqual(context, this.end, aseqOther.end).isTrue()) {
            return context.fals;
        }

        if (!Helpers.rbEqual(context, this.step, aseqOther.step).isTrue()) {
            return context.fals;
        }

        if (!Helpers.rbEqual(context, this.excludeEnd, aseqOther.excludeEnd).isTrue()) {
            return context.fals;
        }

        return context.tru;
    }

    @Override
    public RubyFixnum hash() {
        return hash(metaClass.runtime.getCurrentContext());
    }

    @JRubyMethod(name = "hash")
    public RubyFixnum hash(ThreadContext context) {
        Ruby runtime = context.runtime;

        IRubyObject v;

        v = safeHash(context, excludeEnd);
        long hash = hashStart(runtime, v.convertToInteger().getLongValue());

        v = safeHash(context, begin);
        hash = murmurCombine(hash, v.convertToInteger().getLongValue());

        v = safeHash(context, end);
        hash = murmurCombine(hash, v.convertToInteger().getLongValue());

        v = safeHash(context, step);
        hash = murmurCombine(hash, v.convertToInteger().getLongValue());
        hash = hashEnd(hash);

        return runtime.newFixnum(hash);
    }

    @JRubyMethod
    public IRubyObject begin(ThreadContext context) {
        return begin;
    }

    @JRubyMethod
    public IRubyObject end(ThreadContext context) {
        return end;
    }

    @JRubyMethod
    public IRubyObject step(ThreadContext context) {
        return step;
    }

    @JRubyMethod(name = "exclude_end?")
    public IRubyObject exclude_end(ThreadContext context) {
        return excludeEnd;
    }
}