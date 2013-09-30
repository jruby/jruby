/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
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

import java.util.HashMap;
import java.util.Map;
import org.jruby.Ruby;
import org.jruby.runtime.EventHook;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class CoverageData {
    private volatile Map<String, Integer[]> coverage;

    public boolean isCoverageEnabled() {
        return coverage != null;
    }

    public synchronized void setCoverageEnabled(Ruby runtime, boolean enabled) {
        if (enabled) {
            coverage = new HashMap<String, Integer[]>();
            runtime.addEventHook(COVERAGE_HOOK);
        } else {
            coverage = null;
        }
    }

    public synchronized Map<String, Integer[]> resetCoverage(Ruby runtime) {
        Map<String, Integer[]> coverage = this.coverage;
        runtime.removeEventHook(COVERAGE_HOOK);
        this.coverage = null;
        
        return coverage;
    }

    public synchronized Map<String, Integer[]> prepareCoverage(String filename, Integer[] lines) {
        assert lines != null;

        Map<String, Integer[]> coverage = this.coverage;

        if (coverage != null) {
            coverage.put(filename, lines);
        }

        return coverage;
    }
    
    private final EventHook COVERAGE_HOOK = new EventHook() {
        @Override
        public synchronized void eventHandler(ThreadContext context, String eventName, String file, int line, String name, IRubyObject type) {
            if (coverage == null || line <= 0) {
                return;
            }
            
            // make sure we have a lines array of acceptable length for the given file
            Integer[] lines = coverage.get(file);
            if (lines == null) {
                // loaded before coverage; skip
                return;
            } else if (lines.length <= line) {
                // can this happen? shouldn't all coverable lines be here already (from parse time)?
                Integer[] newLines = new Integer[line];
                System.arraycopy(lines, 0, newLines, 0, lines.length);
                lines = newLines;
                coverage.put(file, lines);
            }
            
            // increment the line's count or set it to 1
            Integer count = lines[line - 1];
            if (count == null) {
                lines[line - 1] = 1;
            } else {
                lines[line - 1] = count + 1;
            }
        }

        @Override
        public boolean isInterestedInEvent(RubyEvent event) {
            return event == RubyEvent.LINE;
        }
    };
    
}
