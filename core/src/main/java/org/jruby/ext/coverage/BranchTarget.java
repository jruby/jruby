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

import org.jruby.runtime.ThreadContext;

/**
 * One of the places a {@link BranchCoverage} construct can branch to (the then or else arm of an
 * <code>if</code>, the body of a loop, a <code>when</code> or <code>in</code> clause, the call or the nil
 * path of <code>&amp;.</code>) together with the number of times execution reached it.
 *
 * <p>A {@link org.jruby.ir.instructions.CoverBranchInstr} at the start of the target's code holds a direct
 * reference to its counter (JIT-compiled code binds to it through an invokedynamic site, see CoverageSite),
 * and counting is a single lock-free atomic add, so parallel threads neither serialize nor lose increments.</p>
 */
public final class BranchTarget {
    private static final VarHandle COUNT;

    static {
        try {
            COUNT = MethodHandles.lookup().findVarHandle(BranchTarget.class, "count", long.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final String label;
    private final int startLine;
    private final int startColumn;
    private final int endLine;
    private final int endColumn;
    private final int index;
    private volatile long count;

    /**
     * An inert copy of another target, holding a count read from it at one instant. Only ever read from.
     */
    private BranchTarget(BranchTarget source, long count) {
        this.label = source.label;
        this.startLine = source.startLine;
        this.startColumn = source.startColumn;
        this.endLine = source.endLine;
        this.endColumn = source.endColumn;
        this.index = source.index;
        this.count = count;
    }

    BranchTarget(String label, int startLine, int startColumn, int endLine, int endColumn, int index) {
        this.label = label;
        this.startLine = startLine;
        this.startColumn = startColumn;
        this.endLine = endLine;
        this.endColumn = endColumn;
        this.index = index;
    }

    /**
     * Record that execution reached this target, if coverage is running.
     */
    public void cover(ThreadContext context) {
        if (context.runtime.getCoverageData().isRunning()) COUNT.getAndAdd(this, 1L);
    }

    public long getCount() {
        return count;
    }

    /**
     * A copy of this target holding the count it has now, for building a result. When clear is true the count
     * is read and reset in one step, so a branch reached while the result is being built is reported once
     * rather than dropped.
     *
     * <p>Reading and resetting are atomic, like {@link #cover}: a result is built under the CoverageData lock
     * but counting happens without it.</p>
     */
    BranchTarget snapshot(boolean clear) {
        return new BranchTarget(this, clear ? (long) COUNT.getAndSet(this, 0L) : count);
    }

    /**
     * then, else, body, when or in.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Position of this target among all branch targets of its file; lets compiled code find the counter again.
     */
    public int getIndex() {
        return index;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getStartColumn() {
        return startColumn;
    }

    public int getEndLine() {
        return endLine;
    }

    public int getEndColumn() {
        return endColumn;
    }

    @Override
    public String toString() {
        return label + " " + startLine + ":" + startColumn + "-" + endLine + ":" + endColumn + " = " + count;
    }
}
