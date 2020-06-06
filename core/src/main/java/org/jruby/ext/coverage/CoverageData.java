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

public class CoverageData {
    public static final String STARTED = "";        // no load/require ruby file can be "" so we
    private static final IntList SVALUE = new IntList();  // use it as a holder to know if start occurs
    private volatile Map<String, IntList> coverage;
    private volatile int mode;

    public static final int NONE = 0;
    public static final int LINES = 1 << 0;
    public static final int BRANCHES = 1 << 1;
    public static final int METHODS = 1 << 2;
    public static final int ONESHOT_LINES = 1 << 3;
    public static final int ALL = LINES | BRANCHES | METHODS;

    public boolean isCoverageEnabled() {
        return mode != 0;
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
        IntList lines = coverage.get(filename);

        if (lines == null) return;

        if (isOneshot()) {
            lines.add(line);
        } else {
            if (lines.size() <= line) return;
            lines.set(line, lines.get(line) + 1);
        }
    }

    public synchronized void setCoverageEnabled(int mode) {
        Map<String, IntList> coverage = this.coverage;

        if (coverage == null) coverage = new HashMap<>();

        if (mode != CoverageData.NONE) {
            coverage.put(STARTED, SVALUE);
        } else {
            coverage.remove(STARTED);
        }

        this.coverage = coverage;
        this.mode = mode;
    }

    public synchronized Map<String, IntList> resetCoverage() {
        Map<String, IntList> coverage = this.coverage;
        coverage.remove(STARTED);

        for (Map.Entry<String, IntList> entry : coverage.entrySet()) {
            String key = entry.getKey();

            // on reset we do not reset files where no execution ever happened but we do reset
            // any files visited to be an empty array.  Why?  I don't know.  Matching MRI.
            if (hasCodeBeenPartiallyCovered(entry.getValue())) coverage.put(key, SVALUE);
        }

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
                coverage.put(filename, new IntList(startingLines));
            }
        }

        return coverage;
    }
    
}
