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

import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyHash;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Implementation of Ruby 1.9.2's "Coverage" module
 */
public class CoverageModule {
    @JRubyMethod(module = true)
    public static IRubyObject start(ThreadContext context, IRubyObject self) {
        Ruby runtime = context.runtime;
        
        if (!runtime.getCoverageData().isCoverageEnabled()) {
            runtime.getCoverageData().setCoverageEnabled(runtime, true);
        }
        
        return context.nil;
    }

    @JRubyMethod(module = true)
    public static IRubyObject result(ThreadContext context, IRubyObject self) {
        Ruby runtime = context.runtime;
        
        if (!runtime.getCoverageData().isCoverageEnabled()) {
            throw runtime.newRuntimeError("coverage measurement is not enabled");
        }
        
        Map<String, Integer[]> coverage = runtime.getCoverageData().resetCoverage(runtime);
        
        // populate a Ruby Hash with coverage data
        RubyHash covHash = RubyHash.newHash(runtime);
        for (Map.Entry<String, Integer[]> entry : coverage.entrySet()) {
            RubyArray ary = RubyArray.newArray(runtime, entry.getValue().length);
            for (int i = 0; i < entry.getValue().length; i++) {
                Integer integer = entry.getValue()[i];
                ary.store(i, integer == null ? runtime.getNil() : runtime.newFixnum(integer));
                covHash.fastASetCheckString(runtime, RubyString.newString(runtime, entry.getKey()), ary);
            }
        }
        
        return covHash;
    }
    
}
