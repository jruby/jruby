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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import org.jruby.RubyModule;
import org.jruby.ir.IRScope;

/**
 * The call counter for a single method entry: one definition of a Ruby-level method (<code>def</code> or
 * <code>define_method</code>) into one owner module. This is JRuby's equivalent of the per-method-entry
 * counters MRI keeps while Coverage measures methods, and one instance corresponds to one key of the
 * <code>:methods</code> hash returned by <code>Coverage.result</code>:
 * <code>[owner, name, start_line, start_column, end_line, end_column]</code>.
 *
 * <p>Lifecycle: {@link CoverageData#registerMethod} creates the counter when the method is added to its owner
 * and attaches it to the {@link org.jruby.internal.runtime.methods.DynamicMethod}. Every call of that method
 * hands the counter to the method body through the calling thread's {@link org.jruby.runtime.ThreadContext}
 * (see {@link org.jruby.ir.instructions.ReceiveMethodCoverageInstr}), and the body increments it once its
 * arguments have been received ({@link org.jruby.ir.instructions.CoverMethodInstr}). Calls that fail while
 * receiving arguments (arity errors, missing keywords, raising default values) are therefore not counted,
 * exactly like MRI's CALL event.</p>
 *
 * <p>Counting is a single lock-free atomic add, so threads calling the same method in parallel neither
 * serialize on a lock nor lose increments.</p>
 */
public final class MethodCoverage {
    private static final VarHandle COUNT;

    static {
        try {
            COUNT = MethodHandles.lookup().findVarHandle(MethodCoverage.class, "count", long.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final IRScope scope;
    private final RubyModule owner;
    private final String name;
    private final int startLine;
    private final int startColumn;
    private final int endLine;
    private final int endColumn;
    private volatile long count;

    MethodCoverage(IRScope scope, RubyModule owner, String name, int startLine, int startColumn, int endLine, int endColumn) {
        this.scope = scope;
        this.owner = owner;
        this.name = name;
        this.startLine = startLine;
        this.startColumn = startColumn;
        this.endLine = endLine;
        this.endColumn = endColumn;
    }

    /**
     * Is this the counter for the given method or block body? A body only counts a counter handed to it for
     * its own scope, which protects against a stale hand-off ever being attributed to the wrong method.
     */
    public boolean isFor(IRScope scope) {
        return this.scope == scope;
    }

    /**
     * Record one call of the method.
     */
    public void cover() {
        COUNT.getAndAdd(this, 1L);
    }

    public long getCount() {
        return count;
    }

    /**
     * Reset the call count to zero (Coverage.result(clear: true)).
     */
    public void clear() {
        count = 0;
    }

    /**
     * The module the method was defined into (MRI: the method entry's owner).
     */
    public RubyModule getOwner() {
        return owner;
    }

    /**
     * The name the method was originally defined with; aliases count toward this entry.
     */
    public String getName() {
        return name;
    }

    /**
     * One-based line of the start of the method's source (the 'def' keyword, or the block passed to define_method).
     */
    public int getStartLine() {
        return startLine;
    }

    /**
     * Zero-based byte column of the start of the method's source.
     */
    public int getStartColumn() {
        return startColumn;
    }

    /**
     * One-based line of the end of the method's source (its 'end' keyword or closing brace).
     */
    public int getEndLine() {
        return endLine;
    }

    /**
     * Zero-based byte column just past the end of the method's source.
     */
    public int getEndColumn() {
        return endColumn;
    }

    @Override
    public String toString() {
        return "MethodCoverage[" + owner + "#" + name + " " + startLine + ":" + startColumn + "-" + endLine + ":" + endColumn + " = " + count + "]";
    }
}
