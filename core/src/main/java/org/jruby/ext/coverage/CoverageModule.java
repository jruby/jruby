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

import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyHash;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.collections.IntList;

/**
 * Implementation of Ruby 1.9.2's "Coverage" module
 */
public class CoverageModule {
    @JRubyMethod(module = true)
    public static IRubyObject start(ThreadContext context, IRubyObject self) {
        Ruby runtime = context.runtime;
        
        if (!runtime.getCoverageData().isCoverageEnabled()) {
            runtime.getCoverageData().setCoverageEnabled(CoverageData.LINES);
        }
        
        return context.nil;
    }

    @JRubyMethod(module = true)
    public static IRubyObject start(ThreadContext context, IRubyObject self, IRubyObject opts) {
        Ruby runtime = context.runtime;

        if (!runtime.getCoverageData().isCoverageEnabled()) {
            int mode = 0;

            if (ArgsUtil.extractKeywordArg(context, "all", opts).isTrue()) {
                mode |= CoverageData.ALL;
            } else {
                if (ArgsUtil.extractKeywordArg(context, "lines", opts).isTrue()) {
                    mode |= CoverageData.LINES;
                }
                if (ArgsUtil.extractKeywordArg(context, "branches", opts).isTrue()) {
                    runtime.getWarnings().warn("branch coverage is not supported");
                    mode |= CoverageData.BRANCHES;
                }
                if (ArgsUtil.extractKeywordArg(context, "methods", opts).isTrue()) {
                    runtime.getWarnings().warn("method coverage is not supported");
                    mode |= CoverageData.METHODS;
                }
                if (ArgsUtil.extractKeywordArg(context, "oneshot_lines", opts).isTrue()) {
                    mode |= CoverageData.LINES;
                    mode |= CoverageData.ONESHOT_LINES;
                }
            }

            runtime.getCoverageData().setCoverageEnabled(mode);
        }

        return context.nil;
    }

    @JRubyMethod(module = true)
    public static IRubyObject result(ThreadContext context, IRubyObject self) {
        Ruby runtime = context.runtime;

        CoverageData coverageData = runtime.getCoverageData();

        if (!coverageData.isCoverageEnabled()) {
            throw runtime.newRuntimeError("coverage measurement is not enabled");
        }

        IRubyObject result = convertCoverageToRuby(context, runtime, coverageData.getCoverage(), coverageData.getMode());

        coverageData.resetCoverage();

        return result;
    }

    @JRubyMethod(module = true)
    public static IRubyObject peek_result(ThreadContext context, IRubyObject self) {
        Ruby runtime = context.runtime;

        CoverageData coverageData = runtime.getCoverageData();

        if (!coverageData.isCoverageEnabled()) {
            throw runtime.newRuntimeError("coverage measurement is not enabled");
        }
        
        return convertCoverageToRuby(context, runtime, coverageData.getCoverage(), coverageData.getMode());
    }

    @JRubyMethod(name = "running?", module = true)
    public static IRubyObject running_p(ThreadContext context, IRubyObject self) {
        return context.runtime.getCoverageData().isCoverageEnabled() ? context.tru : context.fals;
    }

    private static IRubyObject convertCoverageToRuby(ThreadContext context, Ruby runtime, Map<String, IntList> coverage, int mode) {
        // populate a Ruby Hash with coverage data
        RubyHash covHash = RubyHash.newHash(runtime);
        for (Map.Entry<String, IntList> entry : coverage.entrySet()) {
            if (entry.getKey().equals(CoverageData.STARTED)) continue; // ignore our hidden marker

            final IntList val = entry.getValue();
            boolean oneshot = (mode & CoverageData.ONESHOT_LINES) != 0;

            RubyArray ary = RubyArray.newArray(runtime, val.size());
            for (int i = 0; i < val.size(); i++) {
                int integer = val.get(i);
                if (oneshot) {
                    ary.push(runtime.newFixnum(integer + 1));
                } else {
                    ary.store(i, integer == -1 ? context.nil : runtime.newFixnum(integer));
                }
            }

            RubyString key = RubyString.newString(runtime, entry.getKey());
            IRubyObject value = ary;

            if (oneshot) {
                RubyHash oneshotHash = RubyHash.newSmallHash(runtime);
                oneshotHash.fastASetSmall(runtime.newSymbol("oneshot_lines"), ary);
                value = oneshotHash;
            }

            covHash.fastASetCheckString(runtime, key, value);
        }
        
        return covHash;
    }
    
}
