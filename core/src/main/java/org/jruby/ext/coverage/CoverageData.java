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

package org.jruby.ext.coverage;

import java.util.HashMap;
import java.util.Map;
import org.jruby.util.collections.IntList;

import static org.jruby.ext.coverage.CoverageData.CoverageDataState.*;

public class CoverageData {
    public enum CoverageDataState {
        IDLE,
        SUSPENDED,
        RUNNING
    };

    private volatile Map<String, IntList> coverage;
    private volatile int mode;                      // actual mode (currentMode == 0 is mode of LINES).
    private volatile int currentMode;               // listed mode for sake of reporting.
    private volatile CoverageDataState state = IDLE;

    public static final int NONE = 0;
    public static final int LINES = 1 << 0;
    public static final int BRANCHES = 1 << 1;
    public static final int METHODS = 1 << 2;
    public static final int ONESHOT_LINES = 1 << 3;
    public static final int EVAL = 1 << 4;
    public static final int ALL = LINES | BRANCHES | METHODS | EVAL;

    /**
     * Has coverage been setup?
     */
    public boolean isCoverageEnabled() {
        return state != IDLE;
    }

    public boolean isEvalCovered() {
        return (mode & EVAL) != 0;
    }

    /**
     * Is coverage actively collecting info?
     */
    public boolean isRunning() {
        return state == RUNNING;
    }

    public int getMode() {
        return mode;
    }

    public boolean isOneshot() {
        return (mode & ONESHOT_LINES) != 0;
    }

    public Map<String, IntList> getCoverage() {
      return coverage;
    }

    /**
     * Update coverage data for the given file and line number.
     *
     * @param filename
     * @param line
     */
    public synchronized void coverLine(String filename, int line) {
        Map<String, IntList> coverage = this.coverage;

        if (coverage != null) {
            IntList lines = coverage.get(filename);

            if (lines == null) return;

            if (isOneshot()) {
                lines.add(line);
            } else {
                if (lines.size() <= line) return;
                lines.set(line, lines.get(line) + 1);
            }
        }
    }

    public synchronized void clearCoverage() {
        Map<String, IntList> coverage = this.coverage;

        if (coverage != null) {
            Map<String, IntList> cov = coverage;
            if ((mode & ONESHOT_LINES) != 0) {
                for (IntList value: cov.values()) {
                    value.clear();
                }
            } else {
                for (IntList value: cov.values()) {
                    for (int i = 0; i < value.size(); i++) {
                        int v = value.get(i);
                        if (v != -1) value.set(i, 0);
                    }
                }
            }
        }
    }

    public synchronized void resumeCoverage() {
        setupLines();

        this.state = RUNNING;
    }

    public synchronized void suspendCoverage() {
        this.state = SUSPENDED;
    }

    public synchronized void setCoverage(int mode, int currentMode, CoverageDataState state) {
        this.state = state;
        this.mode = mode;
        this.currentMode = currentMode;
        setupLines();
    }

    private void setupLines() {
        Map<String, IntList> coverage = this.coverage;

        if (coverage == null && ((mode & (LINES|ONESHOT_LINES|EVAL)) != 0)) this.coverage = new HashMap<>();
    }

    public synchronized Map<String, IntList> resetCoverage() {
        Map<String, IntList> coverage = this.coverage;

        this.coverage = null;
        this.mode = CoverageData.NONE;

        return coverage;
    }

    private static boolean hasCodeBeenPartiallyCovered(IntList lines) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i) > 0) return true;
        }

        return false;
    }

    public synchronized Map<String, IntList> prepareCoverage(String filename, int[] startingLines) {
        Map<String, IntList> coverage = this.coverage;

        if (filename == null) {
            // null filename from certain evals, Ruby.executeScript, etc (jruby/jruby#5111)
            // we opt to ignore scripts with no filename, since coverage means nothing
            return coverage;
        }

        if (coverage != null) {
            if (isOneshot()) {
                coverage.put(filename, new IntList());
            } else {
                IntList existing = coverage.get(filename);
                if (existing != null) {
                    // Two files with the same path and name just overlay the coverage...weird but true.
                    coverage.put(filename, mergeLines(existing, startingLines));
                } else {
                    coverage.put(filename, new IntList(startingLines));
                }
            }
        }

        return coverage;
    }

    private IntList mergeLines(IntList existing, int[] startingLines) {
        IntList result = existing;
        int existingSize = existing.size();
        int startingLinesLength = startingLines.length;

        if (existingSize < startingLinesLength) {
            int[] newLines = new int[startingLinesLength];
            System.arraycopy(existing.toIntArray(), 0, newLines, 0, existingSize);
            java.util.Arrays.fill(newLines, existingSize, startingLinesLength, -1);
            result = new IntList(newLines);
        }

        for (int i = 0; i < startingLinesLength; i++) {
            int existingValue = result.get(i);
            int newValue = startingLines[i];

            if (newValue == -1) continue;

            if (existingValue == -1) {
                result.set(i, newValue);
            } else {
                result.set(i, existingValue + newValue);
            }
        }

        return result;
    }

    public CoverageDataState getCurrentState() {
        return state;
    }

    public void setCurrentState(CoverageDataState state) {
        this.state = state;
    }

    public int getCurrentMode() {
        return currentMode;
    }
}
