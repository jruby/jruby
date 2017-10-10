/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
import java.util.HashMap;
import java.util.Map;
import org.jruby.Ruby;
import org.jruby.runtime.EventHook;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class CoverageData {
    public static final String STARTED = "";        // no load/require ruby file can be "" so we
    private static final int[] SVALUE = new int[0];  // use it as a holder to know if start occurs
    private volatile Map<String, int[]> coverage;

    public boolean isCoverageEnabled() {
        return coverage != null && coverage.get(STARTED) != null;
    }

    public Map<String, int[]> getCoverage() {
      return coverage;
    }

    public synchronized void setCoverageEnabled(Ruby runtime, boolean enabled) {
        Map<String, int[]> coverage = this.coverage;

        if (coverage == null) coverage = new HashMap<String, int[]>();

        if (enabled) {
            coverage.put(STARTED, SVALUE);
            runtime.addEventHook(COVERAGE_HOOK);
        } else {
            coverage.remove(STARTED);
        }

        this.coverage = coverage;
    }

    public synchronized Map<String, int[]> resetCoverage(Ruby runtime) {
        Map<String, int[]> coverage = this.coverage;
        runtime.removeEventHook(COVERAGE_HOOK);
        coverage.remove(STARTED);


        for (Map.Entry<String, int[]> entry : coverage.entrySet()) {
            String key = entry.getKey();

            // on reset we do not reset files where no execution ever happened but we do reset
            // any files visited to be an empty array.  Why?  I don't know.  Matching MRI.
            if (hasCodeBeenPartiallyCovered(entry.getValue())) coverage.put(key, SVALUE);
        }

        this.coverage = null;

        return coverage;
    }

    private boolean hasCodeBeenPartiallyCovered(int[] lines) {
        for (int i = 0; i < lines.length; i++) {
            if (lines[i] > 0) return true;
        }

        return false;
    }

    public synchronized Map<String, int[]> prepareCoverage(String filename, int[] lines) {
        assert lines != null;

        Map<String, int[]> coverage = this.coverage;

        if (coverage != null) {
            coverage.put(filename, lines);
        }

        return coverage;
    }
    
    private final EventHook COVERAGE_HOOK = new EventHook() {
        @Override
        public synchronized void eventHandler(ThreadContext context, String eventName, String file, int line, String name, IRubyObject type) {
            if (coverage == null || line <= 0) return; // Should not be needed but I predict serialization of IR might hit this.

            int[] lines = coverage.get(file);
            if (lines == null) return;           // no coverage lines for this record.  bail out (should never happen)
            if (lines.length == 0) return;       // coverage is dead for this record.  result() has been called once
                                                 // and we marked it as such as an empty list.
            lines[line - 1] += 1;                // increment usage count by one.
        }

        @Override
        public boolean isInterestedInEvent(RubyEvent event) {
            return event == RubyEvent.COVERAGE;
        }
    };
    
}
