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
import org.jruby.RubySymbol;
import org.jruby.anno.JRubyMethod;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.TypeConverter;
import org.jruby.util.collections.IntList;

import static org.jruby.ext.coverage.CoverageData.CoverageDataState.*;
import static org.jruby.ext.coverage.CoverageData.LINES;
import static org.jruby.runtime.ThreadContext.hasKeywords;

/**
 * Implementation of Ruby 1.9.2's "Coverage" module
 */
public class CoverageModule {
    @JRubyMethod(module = true, optional = 1, keywords = true, checkArity = false)
    public static IRubyObject setup(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 0, 1);

        Ruby runtime = context.runtime;
        int mode = 0;

        CoverageData data = runtime.getCoverageData();

        if (data.getCurrentState() != IDLE) {
            throw runtime.newRuntimeError("coverage measurement is already setup");
        }

        if (argc != 0) {
            boolean keyword = hasKeywords(ThreadContext.resetCallInfo(context));

            if (keyword) {
                RubyHash keywords = (RubyHash) TypeConverter.convertToType(args[0], runtime.getHash(), "to_hash");

                if (ArgsUtil.extractKeywordArg(context, "lines", keywords).isTrue()) {
                    mode |= LINES;
                }
                if (ArgsUtil.extractKeywordArg(context, "branches", keywords).isTrue()) {
                    runtime.getWarnings().warn("branch coverage is not supported");
                    mode |= CoverageData.BRANCHES;
                }
                if (ArgsUtil.extractKeywordArg(context, "methods", keywords).isTrue()) {
                    runtime.getWarnings().warn("method coverage is not supported");
                    mode |= CoverageData.METHODS;
                }
                if (ArgsUtil.extractKeywordArg(context, "oneshot_lines", keywords).isTrue()) {
                    if ((mode & LINES) != 0) {
                        throw runtime.newRuntimeError("cannot enable lines and oneshot_lines simultaneously");
                    }
                    mode |= LINES;
                    mode |= CoverageData.ONESHOT_LINES;
                }
            } else if (args[0] instanceof RubySymbol && args[0] == runtime.newSymbol("all")) {
                mode |= CoverageData.ALL;
            }
        }

        int currentMode = mode;
        if (data.getCoverage() == null) {
            if (mode == 0) mode |= LINES;

            data.setCoverage(mode, currentMode, SUSPENDED);
        } else if (currentMode != data.getCurrentMode()) {
            throw runtime.newRuntimeError("cannot change the measuring target during coverage measurement");
        }

        return context.nil;
    }

    @JRubyMethod(module = true)
    public static IRubyObject resume(ThreadContext context, IRubyObject self) {
        CoverageData data = context.runtime.getCoverageData();

        if (data.getCurrentState() == IDLE) {
            throw context.runtime.newRuntimeError("coverage measurement is not set up yet");
        } else if (data.getCurrentState() == RUNNING) {
            throw context.runtime.newRuntimeError("coverage measurement is already running");
        }

        data.resumeCoverage();

        return context.nil;
    }

    @JRubyMethod(module = true)
    public static IRubyObject suspend(ThreadContext context, IRubyObject self) {
        CoverageData data = context.runtime.getCoverageData();

        if (data.getCurrentState() != RUNNING) {
            throw context.runtime.newRuntimeError("coverage measurement is not running");
        }

        data.suspendCoverage();

        return context.nil;
    }

    @JRubyMethod(module = true, optional = 1, keywords = true, checkArity = false)
    public static IRubyObject start(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        setup(context, self, args);
        resume(context, self);
        return context.nil;
    }

    @JRubyMethod(module = true, optional = 1, keywords = true, checkArity = false)
    public static IRubyObject result(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 0, 1);

        Ruby runtime = context.runtime;
        CoverageData data = runtime.getCoverageData();

        if (data.getCurrentState() == IDLE) {
            throw runtime.newRuntimeError("coverage measurement is not enabled");
        }

        boolean stop = true;
        boolean clear = true;

        if (argc > 0 && hasKeywords(ThreadContext.resetCallInfo(context))) {
            RubyHash keywords = (RubyHash) TypeConverter.convertToType(args[0], runtime.getHash(), "to_hash");
            stop = ArgsUtil.extractKeywordArg(context, "stop", keywords).isTrue();
            clear = ArgsUtil.extractKeywordArg(context, "clear", keywords).isTrue();
        }

        IRubyObject result = peek_result(context, self);
        if (stop && !clear) {
            runtime.getWarnings().warn("stop implies clear");
            clear = true;
        }

        if (clear) {
            data.clearCoverage();
        }

        if (stop) {
            if (data.getCurrentState() == RUNNING) {
                data.suspendCoverage();
            }
            data.resetCoverage();
            data.setCurrentState(IDLE);
        }

        return result;
    }

    @JRubyMethod(module = true)
    public static IRubyObject peek_result(ThreadContext context, IRubyObject self) {
        Ruby runtime = context.runtime;

        CoverageData coverageData = runtime.getCoverageData();

        if (!coverageData.isCoverageEnabled()) {
            throw runtime.newRuntimeError("coverage measurement is not enabled");
        }

        return convertCoverageToRuby(context, runtime, coverageData.getCoverage(), coverageData.getCurrentMode());
    }

    @JRubyMethod(name = "running?", module = true)
    public static IRubyObject running_p(ThreadContext context, IRubyObject self) {
        return context.runtime.getCoverageData().getCurrentState() == RUNNING ? context.tru : context.fals;
    }

    @JRubyMethod(module = true)
    public static IRubyObject state(ThreadContext context, IRubyObject self) {
        Ruby runtime = context.runtime;

        switch (runtime.getCoverageData().getCurrentState()) {
            case IDLE: return runtime.newSymbol("idle");
            case SUSPENDED: return runtime.newSymbol("suspended");
            case RUNNING: return runtime.newSymbol("running");
        }

        return context.nil;
    }

    @JRubyMethod(module = true)
    public static IRubyObject line_stub(ThreadContext context, IRubyObject self, IRubyObject arg) {
        return context.runtime.getParserManager().getLineStub(context, arg);
    }


    private static IRubyObject convertCoverageToRuby(ThreadContext context, Ruby runtime, Map<String, IntList> coverage, int mode) {
        // populate a Ruby Hash with coverage data
        RubyHash covHash = RubyHash.newHash(runtime);
        if (coverage != null) {
            for (Map.Entry<String, IntList> entry : coverage.entrySet()) {
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

                if (mode != 0) {
                    RubyHash oneshotHash = RubyHash.newSmallHash(runtime);
                    RubySymbol linesKey = runtime.newSymbol(oneshot ? "oneshot_lines" : "lines");
                    oneshotHash.fastASetSmall(linesKey, ary);
                    value = oneshotHash;
                }

                covHash.fastASetCheckString(runtime, key, value);
            }
        }
        
        return covHash;
    }
    
}
