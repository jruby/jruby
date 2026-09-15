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
 * Call counter for one method entry: one definition of a Ruby method (def or define_method) in one owner
 * module. This is JRuby's equivalent of the per-method-entry counters MRI keeps in methods mode. Each instance
 * becomes one key of the :methods hash in Coverage.result:
 * <code>[owner, name, start_line, start_column, end_line, end_column]</code>.
 *
 * <p>How a call is counted: {@link CoverageData#registerMethod} creates the counter when the method is added
 * to its owner and attaches it to the {@link org.jruby.internal.runtime.methods.DynamicMethod}. On each call,
 * the entry passes the counter to its body through the {@link org.jruby.runtime.ThreadContext}. The body
 * takes it before receiving arguments ({@link org.jruby.ir.instructions.ReceiveMethodCoverageInstr}) and
 * increments it after ({@link org.jruby.ir.instructions.CoverMethodInstr}). So a call that fails while
 * receiving arguments is not counted, same as MRI's CALL event.</p>
 *
 * <p>The increment is one lock-free atomic add. Parallel calls neither block nor lose counts.</p>
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
     * True if this counter belongs to the given method or block scope. A body only counts its own counter, so
     * a stale hand-off is never charged to another method.
     */
    public boolean isFor(IRScope scope) {
        return this.scope == scope;
    }

    /**
     * Count one call.
     */
    public void cover() {
        COUNT.getAndAdd(this, 1L);
    }

    public long getCount() {
        return count;
    }

    /**
     * Reset the count to zero. Used by Coverage.result(clear: true).
     */
    public void clear() {
        count = 0;
    }

    /**
     * The module the method was defined in.
     */
    public RubyModule getOwner() {
        return owner;
    }

    /**
     * The name the method was defined with. Calls through aliases count here.
     */
    public String getName() {
        return name;
    }

    /**
     * One-based line where the definition starts: the def keyword, or the block passed to define_method.
     */
    public int getStartLine() {
        return startLine;
    }

    /**
     * Zero-based byte column where the definition starts.
     */
    public int getStartColumn() {
        return startColumn;
    }

    /**
     * One-based line where the definition ends: its end keyword or closing brace.
     */
    public int getEndLine() {
        return endLine;
    }

    /**
     * Zero-based byte column just after the end of the definition.
     */
    public int getEndColumn() {
        return endColumn;
    }

    @Override
    public String toString() {
        return "MethodCoverage[" + owner + "#" + name + " " + startLine + ":" + startColumn + "-" + endLine + ":" + endColumn + " = " + count + "]";
    }
}
