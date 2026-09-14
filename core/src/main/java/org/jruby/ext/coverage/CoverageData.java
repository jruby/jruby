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
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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
import org.jruby.runtime.ThreadContext;
import org.jruby.util.collections.IntList;

import static org.jruby.ext.coverage.CoverageData.CoverageDataState.*;

/**
 * Runtime-wide Coverage state: the enabled modes, whether measurement is running, and one {@link FileCoverage}
 * per file parsed since Coverage was set up.
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
     * True when line counts are collected (lines or oneshot_lines mode).
     */
    public boolean isLinesEnabled() {
        return (mode & LINES) != 0;
    }

    /**
     * True when method calls are counted (methods mode).
     */
    public boolean isMethodsEnabled() {
        return (mode & METHODS) != 0;
    }

    /**
     * True when branches are counted (branches mode).
     */
    public boolean isBranchesEnabled() {
        return (mode & BRANCHES) != 0;
    }

    /**
     * Data collected so far, by file name. Null when Coverage is not set up.
     */
    public Map<String, FileCoverage> getCoverage() {
      return coverage;
    }

    /**
     * Update coverage data for the given file and line number.
     *
     * @param filename the file the line belongs to
     * @param line zero-based line number
     * @return true if the line was counted. False if there is nowhere to count it: a negative line, an
     *         untracked file, a file with no line counts, or a line past the end of them.
     */
    public synchronized boolean coverLine(String filename, int line) {
        Map<String, FileCoverage> coverage = this.coverage;

        if (coverage == null) return false;

        // negative lines are not included in coverage
        if (line < 0) return false;

        FileCoverage file = coverage.get(filename);

        if (file == null) return false;

        IntList lines = file.getLines();

        if (lines == null) return false;

        if (isOneshot()) {
            lines.add(line);
        } else {
            if (lines.size() <= line) return false;
            lines.set(line, lines.get(line) + 1);
        }

        return true;
    }

    /**
     * The data collected so far, as an unshared copy that can be read without holding this lock. A result is
     * built from the copy, since converting it runs Ruby code, which must not run under this lock.
     *
     * <p>When clear is true the counts are reset as they are read, in one step. A call counted while the
     * result is being built is then reported once rather than dropped, which is what
     * Coverage.result(clear: true) needs.</p>
     *
     * @param clear reset the counts as they are read, but keep measuring
     * @return a copy by file name, or null when coverage is not set up
     */
    public synchronized Map<String, FileCoverage> snapshot(boolean clear) {
        Map<String, FileCoverage> coverage = this.coverage;

        if (coverage == null) return null;

        Map<String, FileCoverage> snapshot = new LinkedHashMap<>(coverage.size());
        boolean oneshot = isOneshot();

        for (Map.Entry<String, FileCoverage> entry : coverage.entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue().snapshot(clear, oneshot));
        }

        return snapshot;
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
        // files are reported in parse order, as in MRI
        if (this.coverage == null) this.coverage = new LinkedHashMap<>();
    }

    /**
     * Stop measuring and hand back what was collected. Detaches every counter from its method entry, so no
     * method is left instrumented after a run.
     *
     * @param context the current thread context
     * @return the data collected since coverage was set up
     */
    public Map<String, FileCoverage> resetCoverage(ThreadContext context) {
        Map<String, FileCoverage> coverage;
        // By identity: RubyModule#hashCode can dispatch to a Ruby hash method, which must not run here.
        Set<RubyModule> detached = Collections.newSetFromMap(new IdentityHashMap<>());

        synchronized (this) {
            coverage = this.coverage;

            this.coverage = null;
            this.mode = CoverageData.NONE;

            if (coverage != null) {
                for (FileCoverage file : coverage.values()) {
                    for (MethodCoverage method : file.getMethods()) {
                        if (method.detach()) detached.add(method.getOwner());
                    }
                }
            }
        }

        // Lets call sites bound to the counting path re-resolve. Kept outside our lock to avoid a deadlock: this
        // takes the hierarchy lock, and a method being defined holds the method table lock then waits here.
        for (RubyModule owner : detached) {
            owner.invalidateCacheDescendants(context);
        }

        return coverage;
    }

    /**
     * Register a file that was just parsed. Every file parsed while Coverage is set up gets a {@link FileCoverage},
     * which makes it appear in Coverage.result. Line counts are only prepared in lines mode.
     *
     * @param filename the parsed file
     * @param startingLines per-line counts from the parser (-1 for lines without code). Ignored unless lines are counted.
     * @return the file's entry. Null when Coverage is not set up or the file has no name.
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
     * Called when a method entry is added to a module. In methods mode, if the method comes from a tracked file,
     * this creates a {@link MethodCoverage} counter and attaches it to the entry. This is the equivalent of MRI's
     * per-method-entry counters.
     *
     * <p>Entries that only forward to another entry (aliases, visibility changes of inherited methods) get no
     * counter. Their calls count toward the entry they forward to, as in MRI.</p>
     *
     * @param id the name the entry is added under, which MRI keys it by. Not always the name the underlying
     *           method carries: define_method(:new, old_method) copies old_method, and the copy keeps its name.
     * @param method the entry being added, after any wrapping or duplication done by the module
     */
    public void registerMethod(String id, DynamicMethod method) {
        // Every method entry in the process comes through here whenever coverage is set up, so decide without
        // taking the lock that coverLine holds: in lines-only mode there is nothing to do.
        if (!isMethodsEnabled() || this.coverage == null) return;

        if (method instanceof AliasMethod || method instanceof PartialDelegatingMethod || method instanceof MethodMethod) return;

        registerMethodLocked(id, method);
    }

    private synchronized void registerMethodLocked(String id, DynamicMethod method) {
        Map<String, FileCoverage> coverage = this.coverage;
        if (coverage == null) return;

        DynamicMethod real = method.getRealMethod();
        if (real.getMethodCoverage() != null) return; // already counted

        IRScope scope = definitionScope(real);
        if (scope == null) return;

        // The body only counts its calls if it was parsed with the counting instructions in it, which code parsed
        // before methods mode was on was not. Leave such a method out rather than list it with a count stuck at zero.
        if ((scope.getCoverageMode() & METHODS) == 0) return;

        FileCoverage file = coverage.get(scope.getFile());
        if (file == null) return;

        int startLine = scope.getLine() + 1;
        if (startLine <= 0) return; // MRI skips methods with a non-positive line (eval with a line offset)

        RubyModule owner = method.getImplementationClass();
        if (owner == null) return;

        // -1 means no span was recorded (Prism does not supply them yet). Keep the marker, do not make it line 0.
        int endLine = scope.getEndLine();
        if (endLine >= 0) endLine++;

        MethodCoverage methodCoverage = new MethodCoverage(real, scope, owner.getOrigin(), id,
                startLine, scope.getStartColumn(), endLine, scope.getEndColumn());

        file.getMethods().add(methodCoverage);
        real.setMethodCoverage(methodCoverage);
    }

    /**
     * The IR scope holding the Ruby source of a method entry: the IRMethod of a def, or the IRClosure of a block
     * or lambda passed to define_method. Null for anything else, such as native methods and attr accessors.
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
