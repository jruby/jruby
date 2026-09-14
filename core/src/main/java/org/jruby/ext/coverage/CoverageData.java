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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jruby.RubyModule;
import org.jruby.internal.runtime.AbstractIRMethod;
import org.jruby.internal.runtime.methods.AliasMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.MethodMethod;
import org.jruby.internal.runtime.methods.PartialDelegatingMethod;
import org.jruby.internal.runtime.methods.ProcMethod;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScope;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.IRBlockBody;
import org.jruby.util.collections.IntList;

import static org.jruby.ext.coverage.CoverageData.CoverageDataState.*;

/**
 * The runtime-wide state of the Coverage library: which measurement modes are enabled, whether measurement is
 * running, and a {@link FileCoverage} for every file parsed since coverage was set up.
 */
public class CoverageData {
    public enum CoverageDataState {
        IDLE,
        SUSPENDED,
        RUNNING
    };

    private volatile Map<String, FileCoverage> coverage;
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

    /**
     * Are line execution counts being measured (lines or oneshot_lines mode)?
     */
    public boolean isLinesEnabled() {
        return (mode & LINES) != 0;
    }

    /**
     * Are method calls being counted (methods mode)?
     */
    public boolean isMethodsEnabled() {
        return (mode & METHODS) != 0;
    }

    /**
     * The data collected so far, by file name; null when coverage is not set up.
     */
    public Map<String, FileCoverage> getCoverage() {
      return coverage;
    }

    /**
     * Update coverage data for the given file and line number.
     *
     * @param filename
     * @param line
     */
    public synchronized void coverLine(String filename, int line) {
        Map<String, FileCoverage> coverage = this.coverage;

        // negative lines are not included in coverage
        if (line < 0) return;

        if (coverage != null) {
            FileCoverage file = coverage.get(filename);

            if (file == null) return;

            IntList lines = file.getLines();

            if (lines == null) return;

            if (isOneshot()) {
                lines.add(line);
            } else {
                if (lines.size() <= line) return;
                lines.set(line, lines.get(line) + 1);
            }
        }
    }

    /**
     * Zero all counts collected so far but keep measuring (Coverage.result(clear: true)).
     */
    public synchronized void clearCoverage() {
        Map<String, FileCoverage> coverage = this.coverage;

        if (coverage != null) {
            for (FileCoverage file : coverage.values()) {
                IntList lines = file.getLines();

                if (lines != null) {
                    if (isOneshot()) {
                        lines.clear();
                    } else {
                        for (int i = 0; i < lines.size(); i++) {
                            int v = lines.get(i);
                            if (v != -1) lines.set(i, 0);
                        }
                    }
                }

                for (MethodCoverage method : file.getMethods()) {
                    method.clear();
                }
            }
        }
    }

    public synchronized void resumeCoverage() {
        setupCoverage();

        this.state = RUNNING;
    }

    public synchronized void suspendCoverage() {
        this.state = SUSPENDED;
    }

    public synchronized void setCoverage(int mode, int currentMode, CoverageDataState state) {
        this.state = state;
        this.mode = mode;
        this.currentMode = currentMode;
        setupCoverage();
    }

    private void setupCoverage() {
        // files are reported in the order they were parsed, as MRI does
        if (this.coverage == null) this.coverage = new LinkedHashMap<>();
    }

    public synchronized Map<String, FileCoverage> resetCoverage() {
        Map<String, FileCoverage> coverage = this.coverage;

        this.coverage = null;
        this.mode = CoverageData.NONE;

        return coverage;
    }

    /**
     * Start tracking a file that has just been parsed: every file parsed while coverage is set up gets a
     * {@link FileCoverage} entry (that is what makes it appear in Coverage.result), and line counts are prepared
     * when lines are being measured.
     *
     * @param filename the file just parsed
     * @param startingLines the initial per-line counts computed by the parser (-1 for lines without code); only
     *                      consulted when lines are being counted
     * @return the file's entry, or null when coverage is not set up or the file has no name
     */
    public synchronized FileCoverage prepareCoverage(String filename, int[] startingLines) {
        Map<String, FileCoverage> coverage = this.coverage;

        if (filename == null) {
            // null filename from certain evals, Ruby.executeScript, etc (jruby/jruby#5111)
            // we opt to ignore scripts with no filename, since coverage means nothing
            return null;
        }

        if (coverage == null) return null;

        FileCoverage file = coverage.get(filename);

        if (file == null) {
            file = new FileCoverage();
            coverage.put(filename, file);
        }

        if (isLinesEnabled()) {
            if (isOneshot()) {
                file.setLines(new IntList());
            } else {
                IntList existing = file.getLines();

                // Two files with the same path and name just overlay the coverage...weird but true.
                file.setLines(existing == null ? new IntList(startingLines) : mergeLines(existing, startingLines));
            }
        }

        return file;
    }

    private IntList mergeLines(IntList existing, int[] startingLines) {
        IntList result = existing;
        int existingSize = existing.size();
        int startingLinesLength = startingLines.length;

        if (existingSize < startingLinesLength) {
            int[] newLines = new int[startingLinesLength];
            System.arraycopy(existing.toIntArray(), 0, newLines, 0, existingSize);
            Arrays.fill(newLines, existingSize, startingLinesLength, -1);
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

    /**
     * A method entry is being added to a module: if methods are being measured and the method was defined from a
     * file being tracked, create its {@link MethodCoverage} counter and attach it to the entry so that its calls
     * get counted. This is the equivalent of MRI's per-method-entry counters; like MRI, an entry that merely
     * forwards to another entry (an alias, a visibility change of an inherited method) gets no counter of its own
     * because its calls count toward the entry it forwards to.
     *
     * @param method the method entry being added (after any wrapping/duplication the module performs)
     */
    public synchronized void registerMethod(DynamicMethod method) {
        if (!isMethodsEnabled()) return;

        Map<String, FileCoverage> coverage = this.coverage;
        if (coverage == null) return;

        if (method instanceof AliasMethod || method instanceof PartialDelegatingMethod || method instanceof MethodMethod) return;

        DynamicMethod real = method.getRealMethod();
        if (real.getMethodCoverage() != null) return; // already an entry being counted

        IRScope scope = definitionScope(real);
        if (scope == null) return;

        FileCoverage file = coverage.get(scope.getFile());
        if (file == null) return;

        int startLine = scope.getLine() + 1;
        if (startLine <= 0) return; // MRI ignores methods with non-positive line numbers (eval with a line offset)

        RubyModule owner = method.getImplementationClass();
        if (owner == null) return;

        MethodCoverage methodCoverage = new MethodCoverage(scope, owner.getOrigin(), real.getName(),
                startLine, scope.getStartColumn(), scope.getEndLine() + 1, scope.getEndColumn());

        file.getMethods().add(methodCoverage);
        real.setMethodCoverage(methodCoverage);
    }

    /**
     * The scope holding the Ruby source of a method entry: the IRMethod of a def (or of a block define_method
     * converted into a method), or the IRClosure of a block/lambda passed to define_method. Null for anything
     * else (native methods, attr accessors, precompiled code without IR).
     */
    private static IRScope definitionScope(DynamicMethod method) {
        if (method instanceof AbstractIRMethod irMethod) {
            IRScope scope = irMethod.getIRScope();

            return scope instanceof IRMethod ? scope : null;
        }

        if (method instanceof ProcMethod procMethod) {
            BlockBody body = procMethod.getProc().getBlock().getBody();

            return body instanceof IRBlockBody irBody ? irBody.getScope() : null;
        }

        return null;
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
