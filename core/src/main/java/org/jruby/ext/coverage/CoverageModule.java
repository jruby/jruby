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

import static org.jruby.api.Access.hashClass;
import static org.jruby.api.Convert.*;
import static org.jruby.api.Create.*;
import static org.jruby.api.Error.runtimeError;
import static org.jruby.api.Error.typeError;
import static org.jruby.api.Warn.warn;
import static org.jruby.ext.coverage.CoverageData.CoverageDataState.*;
import static org.jruby.ext.coverage.CoverageData.EVAL;
import static org.jruby.ext.coverage.CoverageData.LINES;
import static org.jruby.runtime.ThreadContext.hasKeywords;
import static org.jruby.util.RubyStringBuilder.str;

/**
 * Implementation of Ruby 1.9.2's "Coverage" module
 */
public class CoverageModule {
    @JRubyMethod(module = true, optional = 1, keywords = true, checkArity = false)
    public static IRubyObject setup(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 0, 1);
        int mode = 0;
        CoverageData data = context.runtime.getCoverageData();

        if (data.getCurrentState() != IDLE) throw runtimeError(context, "coverage measurement is already setup");

        if (argc != 0) {
            boolean keyword = hasKeywords(ThreadContext.resetCallInfo(context));

            if (keyword) {
                RubyHash keywords = (RubyHash) TypeConverter.convertToType(args[0], hashClass(context), "to_hash");

                if (ArgsUtil.extractKeywordArg(context, "lines", keywords).isTrue()) {
                    mode |= LINES;
                }
                if (ArgsUtil.extractKeywordArg(context, "eval", keywords).isTrue()) {
                    mode |= EVAL;
                }
                if (ArgsUtil.extractKeywordArg(context, "branches", keywords).isTrue()) {
                    warn(context, "branch coverage is not supported");
                    mode |= CoverageData.BRANCHES;
                }
                if (ArgsUtil.extractKeywordArg(context, "methods", keywords).isTrue()) {
                    warn(context, "method coverage is not supported");
                    mode |= CoverageData.METHODS;
                }
                if (ArgsUtil.extractKeywordArg(context, "oneshot_lines", keywords).isTrue()) {
                    if ((mode & LINES) != 0) throw runtimeError(context, "cannot enable lines and oneshot_lines simultaneously");

                    mode |= LINES;
                    mode |= CoverageData.ONESHOT_LINES;
                }
            } else if (args[0] instanceof RubySymbol && args[0] == asSymbol(context, "all")) {
                mode |= CoverageData.ALL;
            } else {
                throw typeError(context, str(context.runtime, "no implicit conversion of ", args[0].getMetaClass(), " into Hash"));
            }
        }

        int currentMode = mode;
        if (data.getCoverage() == null) {
            if (mode == 0) mode |= LINES;

            data.setCoverage(mode, currentMode, SUSPENDED);
        } else if (currentMode != data.getCurrentMode()) {
            throw runtimeError(context, "cannot change the measuring target during coverage measurement");
        }

        return context.nil;
    }

    @JRubyMethod(module = true)
    public static IRubyObject resume(ThreadContext context, IRubyObject self) {
        CoverageData data = context.runtime.getCoverageData();

        if (data.getCurrentState() == IDLE) throw runtimeError(context, "coverage measurement is not set up yet");
        if (data.getCurrentState() == RUNNING) throw runtimeError(context, "coverage measurement is already running");

        data.resumeCoverage();

        return context.nil;
    }

    @JRubyMethod(module = true)
    public static IRubyObject suspend(ThreadContext context, IRubyObject self) {
        CoverageData data = context.runtime.getCoverageData();

        if (data.getCurrentState() != RUNNING) throw runtimeError(context, "coverage measurement is not running");

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
        CoverageData data = context.runtime.getCoverageData();

        if (data.getCurrentState() == IDLE) throw runtimeError(context, "coverage measurement is not enabled");

        boolean stop = true;
        boolean clear = true;

        if (argc > 0 && hasKeywords(ThreadContext.resetCallInfo(context))) {
            RubyHash keywords = (RubyHash) TypeConverter.convertToType(args[0], hashClass(context), "to_hash");
            stop = ArgsUtil.extractKeywordArg(context, "stop", keywords).isTrue();
            clear = ArgsUtil.extractKeywordArg(context, "clear", keywords).isTrue();
        }

        IRubyObject result = peek_result(context, self);
        if (stop && !clear) {
            warn(context, "stop implies clear");
            clear = true;
        }

        if (clear) data.clearCoverage();
        if (stop) {
            if (data.getCurrentState() == RUNNING) data.suspendCoverage();
            data.resetCoverage();
            data.setCurrentState(IDLE);
        }

        return result;
    }

    @JRubyMethod(module = true)
    public static IRubyObject peek_result(ThreadContext context, IRubyObject self) {
        CoverageData coverageData = context.runtime.getCoverageData();

        if (!coverageData.isCoverageEnabled()) throw runtimeError(context, "coverage measurement is not enabled");

        return convertCoverageToRuby(context, coverageData.getCoverage(), coverageData.getCurrentMode());
    }

    @JRubyMethod(name = "running?", module = true)
    public static IRubyObject running_p(ThreadContext context, IRubyObject self) {
        return context.runtime.getCoverageData().getCurrentState() == RUNNING ? context.tru : context.fals;
    }

    @JRubyMethod(module = true)
    public static IRubyObject state(ThreadContext context, IRubyObject self) {
        return switch (context.runtime.getCoverageData().getCurrentState()) {
            case IDLE -> asSymbol(context, "idle");
            case SUSPENDED -> asSymbol(context, "suspended");
            case RUNNING -> asSymbol(context, "running");
        };
    }

    @JRubyMethod(module = true)
    public static IRubyObject line_stub(ThreadContext context, IRubyObject self, IRubyObject arg) {
        return context.runtime.getParserManager().getLineStub(context, arg);
    }

    @JRubyMethod(module = true, name = "supported?")
    public static IRubyObject supported_p(ThreadContext context, IRubyObject self, IRubyObject arg) {
        RubySymbol mode = castAsSymbol(context, arg);

        return mode == asSymbol(context, "lines") || mode == asSymbol(context, "oneshot_lines") || mode == asSymbol(context, "eval") ?
                context.tru : context.fals;
    }

    private static IRubyObject convertCoverageToRuby(ThreadContext context, Map<String, IntList> coverage, int mode) {
        if (coverage == null) return newSmallHash(context);

        RubyHash covHash = newHash(context);         // populate a Ruby Hash with coverage data

        for (Map.Entry<String, IntList> entry : coverage.entrySet()) {
            final IntList val = entry.getValue();
            boolean oneshot = (mode & CoverageData.ONESHOT_LINES) != 0;

            int size = val.size();
            var ary = allocArray(context, size);
            for (int i = 0; i < size; i++) {
                int integer = val.get(i);
                if (oneshot) {
                    ary.push(context, asFixnum(context, integer + 1));
                } else {
                    ary.store(i, integer == -1 ? context.nil : asFixnum(context, integer));
                }
            }

            RubyString key = newString(context, entry.getKey());
            IRubyObject value;

            if (mode != 0) {
                RubyHash oneshotHash = newSmallHash(context);
                RubySymbol linesKey = asSymbol(context, oneshot ? "oneshot_lines" : "lines");
                oneshotHash.fastASetSmall(linesKey, ary);
                value = oneshotHash;
            } else {
                value = ary;
            }

            covHash.fastASetCheckString(context.runtime, key, value);
        }

        return covHash;
    }
}
