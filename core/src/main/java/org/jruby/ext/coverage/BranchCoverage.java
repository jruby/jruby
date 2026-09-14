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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * One branching construct of a source file (an <code>if</code>, <code>unless</code>, <code>case</code>,
 * <code>while</code>, <code>until</code> or <code>&amp;.</code>) and the {@link BranchTarget}s it can branch to.
 * This is JRuby's equivalent of the branch base MRI's compiler declares for such a node, and it becomes one key
 * of the <code>:branches</code> hash returned by <code>Coverage.result</code>:
 * <code>[type, id, start_line, start_column, end_line, end_column]</code> mapped to the targets' counts.
 *
 * <p>Constructs are declared when the IR of their file is built (see IRBuilder) and identified by type and
 * span, so building the same source again (a file loaded twice, a block turned into a method) finds the
 * existing construct rather than declaring a second one.</p>
 */
public final class BranchCoverage {
    private final FileCoverage file;
    private final String type;
    private final int startLine;
    private final int startColumn;
    private final int endLine;
    private final int endColumn;
    private final List<BranchTarget> targets = new CopyOnWriteArrayList<>();
    private final Map<String, BranchTarget> targetsByKey = new HashMap<>();

    /**
     * An inert copy of another construct, holding copies of its targets. Only ever read from.
     */
    private BranchCoverage(BranchCoverage source) {
        this.file = source.file;
        this.type = source.type;
        this.startLine = source.startLine;
        this.startColumn = source.startColumn;
        this.endLine = source.endLine;
        this.endColumn = source.endColumn;
    }

    BranchCoverage(FileCoverage file, String type, int startLine, int startColumn, int endLine, int endColumn) {
        this.file = file;
        this.type = type;
        this.startLine = startLine;
        this.startColumn = startColumn;
        this.endLine = endLine;
        this.endColumn = endColumn;
    }

    /**
     * Declare (or find) the target of this construct with the given label and source span.
     *
     * @param label then, else, body, when or in
     * @param startLine one-based line where the target's source starts
     * @param startColumn zero-based byte column where it starts
     * @param endLine one-based line where it ends
     * @param endColumn zero-based byte column just past its end
     */
    public synchronized BranchTarget declareTarget(String label, int startLine, int startColumn, int endLine, int endColumn) {
        String key = label + ':' + startLine + ':' + startColumn + ':' + endLine + ':' + endColumn;
        BranchTarget target = targetsByKey.get(key);

        if (target == null) {
            target = new BranchTarget(label, startLine, startColumn, endLine, endColumn, file.registerBranchTarget());
            targetsByKey.put(key, target);
            targets.add(target);
            file.addBranchTarget(target);
        }

        return target;
    }

    /**
     * The targets in declaration order.
     */
    public List<BranchTarget> getTargets() {
        return targets;
    }

    /**
     * A copy of this construct holding the counts its targets have now, for building a result. When clear is
     * true each count is read and reset in one step.
     */
    BranchCoverage snapshot(boolean clear) {
        BranchCoverage copy = new BranchCoverage(this);

        for (BranchTarget target : targets) {
            copy.targets.add(target.snapshot(clear));
        }

        return copy;
    }

    public String getType() {
        return type;
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
        return "BranchCoverage[" + type + " " + startLine + ":" + startColumn + "-" + endLine + ":" + endColumn + " " + targets + "]";
    }
}
