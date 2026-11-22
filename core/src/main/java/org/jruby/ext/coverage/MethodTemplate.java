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
 * the terms of any of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.ext.coverage;

import java.util.ArrayList;
import java.util.List;

import org.jruby.RubyArray;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.anno.CRuby;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.collections.IntList;

import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.asSymbol;
import static org.jruby.api.Create.newArray;
import static org.jruby.api.Create.newHash;

/**
 * The :methods hash of one file without its counts, kept so that a result copies it and fills the counts in
 * rather than building and hashing a new key for every method each time. Methods are only ever added to a
 * file, so bringing the template up to date only adds the keys of the methods defined since the last result.
 *
 * <p>Each key maps to a slot number, and each slot to the positions in {@link FileCoverage#getMethods()} of
 * the methods with that key. More than one method shares a key when a method is defined twice at the same
 * place, and their counts are added, as in MRI.</p>
 *
 * <p>The keys are frozen and shared by every result, as in CRuby 4.1.</p>
 */
final class MethodTemplate {
    private RubyHash keys;
    private final List<IntList> methodsBySlot = new ArrayList<>();
    private int added;

    /**
     * {[owner, name, start_line, start_column, end_line, end_column] => count} for a snapshot of the file's
     * methods.
     *
     * <p>Runs outside the {@link CoverageData} lock, since hashing a key can dispatch to Ruby code. A snapshot
     * taken before one that was already added here has fewer methods than the template has keys for, so it gets
     * a hash of its own instead.</p>
     */
    @CRuby(value = "method_coverage", file = "ext/coverage/coverage.c",
            note = "also brings the template up to date, which rb_coverage_peek_result does first")
    synchronized RubyHash result(ThreadContext context, List<MethodCoverage> methods) {
        if (methods.size() < added) return build(context, methods);
        if (keys == null) keys = newHash(context);

        update(context, methods);

        RubyHash result = keys.dupFast(context);

        for (Object entry : result.directEntrySet()) {
            fill(context, (RubyHash.RubyHashEntry) entry, methods);
        }

        return result;
    }

    @CRuby(value = "method_template_update", file = "ext/coverage/coverage.c")
    private void update(ThreadContext context, List<MethodCoverage> methods) {
        for (int i = added; i < methods.size(); i++) {
            RubyArray<?> key = key(context, methods.get(i));
            IRubyObject slot = keys.fastARef(key);

            if (slot == null) {
                slot = asFixnum(context, methodsBySlot.size());
                methodsBySlot.add(new IntList());
                keys.fastASet(key, slot);
            }

            methodsBySlot.get(slotNumber(slot)).add(i);
            added++;
        }
    }

    @CRuby(value = "method_fill_count", file = "ext/coverage/coverage.c")
    private void fill(ThreadContext context, RubyHash.RubyHashEntry entry, List<MethodCoverage> methods) {
        IntList positions = methodsBySlot.get(slotNumber((IRubyObject) entry.getValue()));
        long count = 0;

        for (int i = 0; i < positions.size(); i++) {
            count += methods.get(positions.get(i)).getCount();
        }

        entry.setValue(asFixnum(context, count));
    }

    private static RubyHash build(ThreadContext context, List<MethodCoverage> methods) {
        RubyHash hash = newHash(context);

        for (MethodCoverage method : methods) {
            RubyArray<?> key = key(context, method);
            long count = method.getCount();
            IRubyObject previous = hash.fastARef(key);

            if (previous != null) count += ((RubyFixnum) previous).getValue();

            hash.fastASet(key, asFixnum(context, count));
        }

        return hash;
    }

    private static RubyArray<?> key(ThreadContext context, MethodCoverage method) {
        RubyArray<?> key = newArray(context, method.getOwner(), asSymbol(context, method.getName()),
                asFixnum(context, method.getStartLine()), asFixnum(context, method.getStartColumn()),
                asFixnum(context, method.getEndLine()), asFixnum(context, method.getEndColumn()));

        key.setFrozen(true);

        return key;
    }

    private static int slotNumber(IRubyObject slot) {
        return (int) ((RubyFixnum) slot).getValue();
    }
}
