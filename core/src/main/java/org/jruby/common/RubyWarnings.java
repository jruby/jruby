/*
 **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
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

package org.jruby.common;

import java.util.EnumSet;
import java.util.Set;

import org.joni.WarnCallback;
import org.jruby.Ruby;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.anno.JRubyMethod;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.backtrace.RubyStackTraceElement;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.TypeConverter;
import org.jruby.util.func.TriFunction;

import static org.jruby.api.Access.globalVariables;
import static org.jruby.api.Access.kernelModule;
import static org.jruby.api.Access.stringClass;
import static org.jruby.api.Access.symbolClass;
import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Convert.asSymbol;
import static org.jruby.api.Create.newArrayFrom;
import static org.jruby.api.Create.newString;
import static org.jruby.api.Define.defineModule;
import static org.jruby.api.Error.argumentError;
import static org.jruby.util.RubyStringBuilder.str;

/**
 *
 */
public class RubyWarnings implements IRubyWarnings, WarnCallback {

    private static final int LINE_NUMBER_NON_WARNING = Integer.MIN_VALUE;

    private final Ruby runtime;
    private final Set<ID> oncelers = EnumSet.allOf(IRubyWarnings.ID.class);

    public RubyWarnings(Ruby runtime) {
        this.runtime = runtime;
    }

    public static RubyModule createWarningModule(ThreadContext context) {
        var Warning = defineModule(context, "Warning").defineMethods(context, RubyWarnings.class);

        Warning.extend_object(context, Warning);

        return Warning;
    }

    @Override
    public void warn(String message) {
        warn(ID.MISCELLANEOUS, message);
    }

    public <Context, State> void warn(Context context, State state, TriFunction<Context, State, RubyStackTraceElement, String> callback) {
        if (!runtime.warningsEnabled()) return;

        RubyStackTraceElement trace = runtime.getCurrentContext().getSingleBacktrace();

        String message = callback.apply(context, state, trace);
        String file;
        int line;

        if (trace == null) {
            file = "(unknown)";
            line = 0;
        } else {
            file = trace.getFileName();
            line = trace.getLineNumber();
        }

        warn(ID.MISCELLANEOUS, file, line, message);
    }

    @Override
    public Ruby getRuntime() {
        return runtime;
    }

    @Override
    public boolean isVerbose() {
        return runtime.isVerbose();
    }

    /**
     * Prints a warning, unless $VERBOSE is nil.
     */
    @Override
    public void warn(ID id, String fileName, int lineNumber, String message) {
        doWarn(id, fileName, lineNumber, message);
    }

    public void warn(String fileName, int lineNumber, String message) {
        doWarn(ID.MISCELLANEOUS, fileName, lineNumber, message);
    }

    private void doWarn(ID id, String fileName, int lineNumber, CharSequence message) {
        if (!runtime.warningsEnabled()) return;

        String fullMessage;
        if (lineNumber != LINE_NUMBER_NON_WARNING) {
            fullMessage = fileName + ':' + lineNumber + ": warning: " + message + '\n';
        } else {
            fullMessage = fileName + ": " + message + '\n'; // warn(fileName, message) behave as in MRI
        }
        ThreadContext context = runtime.getCurrentContext();
        writeWarningDyncall(context, newString(context, fullMessage));
    }

    // MRI: rb_write_warning_str
    private static IRubyObject writeWarningDyncall(ThreadContext context, RubyString errorString) {
        RubyModule warning = context.runtime.getWarning();

        return sites(context).warn.call(context, warning, warning, errorString);
    }

    // MR: rb_write_error_str
    private static IRubyObject writeWarningToError(ThreadContext context, RubyString errorString) {
        IRubyObject errorStream = globalVariables(context).get("$stderr");

        return sites(context).write.call(context, context.runtime.getWarning(), errorStream, errorString);
    }

    public static IRubyObject warnWithCategory(ThreadContext context, IRubyObject errorString, IRubyObject category) {
        RubySymbol cat = (RubySymbol) TypeConverter.convertToType(category, symbolClass(context), "to_sym");

        if (context.runtime.getWarningCategories().contains(Category.fromId(cat.idString()))) warn(context, kernelModule(context), errorString);

        return context.nil;
    }

    @Override
    public void warn(ID id, String message) {
        if (!runtime.warningsEnabled()) return;

        RubyStackTraceElement stack = runtime.getCurrentContext().getSingleBacktrace();
        String file;
        int line;

        if (stack == null) {
            file = "(unknown)";
            line = 0;
        } else {
            file = stack.getFileName();
            line = stack.getLineNumber();
        }

        doWarn(id, file, line, message);
    }

    public void warn(String filename, String message) {
        doWarn(ID.MISCELLANEOUS, filename, LINE_NUMBER_NON_WARNING, message);
    }

    @Deprecated
    public void warnExperimental(String filename, int line, String message) {
        if (runtime.getWarningCategories().contains(Category.EXPERIMENTAL)) warn(ID.MISCELLANEOUS, filename, line, message);
    }

    @Deprecated(since = "10.0")
    public void warnDeprecated(ID id, String message) {
        if (runtime.getWarningCategories().contains(Category.DEPRECATED)) warn(id, message);
    }

    public void warnDeprecated(String message) {
        if (runtime.getWarningCategories().contains(Category.DEPRECATED)) warn(message);
    }

    public void warnExperimental(String message) {
        if (runtime.getWarningCategories().contains(Category.EXPERIMENTAL)) warn(message);
    }

    public void warnPerformance(String message) {
        if (runtime.getWarningCategories().contains(Category.PERFORMANCE)) warn(message);
    }

    @Deprecated(since = "10.0")
    public void warnDeprecatedAlternate(String name, String alternate) {
        if (hasDeprecationWarningEnabled()) warn(ID.DEPRECATED_METHOD, name + " is deprecated; use " + alternate + " instead");
    }

    @Deprecated(since = "10.0")
    public void warnDeprecatedForRemoval(String name, String version) {
        if (hasDeprecationWarningEnabled()) warn(ID.MISCELLANEOUS, name + " is deprecated and will be removed in Ruby " + version);
    }

    public void warnDeprecatedForRemovalAlternate(String name, String version, String alternate) {
        if (hasDeprecationWarningEnabled()) warn(ID.MISCELLANEOUS, name + " is deprecated and will be removed in Ruby " + version + "; use " + alternate + " instead");
    }

    public boolean hasDeprecationWarningEnabled() {
        return runtime.getWarningCategories().contains(Category.DEPRECATED);
    }

    public void warnOnce(ID id, String message) {
        if (!runtime.warningsEnabled()) return;
        if (oncelers.contains(id)) return;

        oncelers.add(id);
        warn(id, message);
    }

    /**
     * Verbose mode warning methods, only warn in verbose mode
     */
    public void warning(String message) {
        warning(ID.MISCELLANEOUS, message);
    }

    public void warningDeprecated(String message) {
        warningDeprecated(ID.MISCELLANEOUS, message);
    }

    @Override
    public void warning(ID id, String message) {
        if (!isVerbose()) return;

        warn(id, message);
    }

    public void warningDeprecated(ID id, String message) {
        if (!isVerbose() && !runtime.getWarningCategories().contains(Category.DEPRECATED)) return;
        warning(id, message);
    }

    /**
     * Prints a warning, only in verbose mode.
     */
    @Override
    public void warning(ID id, String fileName, int lineNumber, String message) {
        if (!isVerbose()) return;

        warn(id, fileName, lineNumber, message);
    }

    public void warning(String fileName, int lineNumber, String message) {
        if (!isVerbose()) return;

        warn(fileName, lineNumber, message);
    }

    @JRubyMethod(name = "[]")
    public static IRubyObject op_aref(ThreadContext context, IRubyObject self, IRubyObject arg) {
        TypeConverter.checkType(context, arg, symbolClass(context));
        String categoryId = ((RubySymbol) arg).idString();
        Category category = Category.fromId(categoryId);

        if (category == null) throw argumentError(context, str(context.runtime, "unknown category: ", arg));

        return asBoolean(context, context.runtime.getWarningCategories().contains(category));
    }

    @JRubyMethod(name = "[]=")
    public static IRubyObject op_aset(ThreadContext context, IRubyObject self, IRubyObject arg, IRubyObject flag) {
        TypeConverter.checkType(context, arg, symbolClass(context));
        String categoryId = ((RubySymbol) arg).idString();
        Category category = Category.fromId(categoryId);

        if (category != null) {
            if (flag.isTrue()) {
                context.runtime.getWarningCategories().add(category);
            } else {
                context.runtime.getWarningCategories().remove(category);
            }
        } else {
            throw argumentError(context, str(context.runtime, "unknown category: ", arg));
        }

        return flag;
    }

    @JRubyMethod
    public static IRubyObject warn(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        TypeConverter.checkType(context, arg, stringClass(context));
        return warn(context, (RubyString) arg);
    }

    @JRubyMethod
    public static IRubyObject warn(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1) {
        IRubyObject opts = TypeConverter.checkHashType(context.runtime, arg1);
        IRubyObject ret = ArgsUtil.extractKeywordArg(context, (RubyHash) opts, "category");
        return ret.isNil() ? warn(context, recv, arg0) : warnWithCategory(context, arg0, ret);
    }

    public static IRubyObject warn(ThreadContext context, RubyString str) {
        str.verifyAsciiCompatible();
        writeWarningToError(context, str);
        return context.nil;
    }

    @JRubyMethod
    public static IRubyObject categories(ThreadContext context, IRubyObject recv) {
        return newArrayFrom(context, Category.values(), (ctx, c) -> asSymbol(ctx, c.id));
    }

    private static JavaSites.WarningSites sites(ThreadContext context) {
        return context.sites.Warning;
    }

    public enum Category {
        EXPERIMENTAL("experimental"), DEPRECATED("deprecated"), PERFORMANCE("performance");

        private String id;

        Category(String id) {
            this.id = id;
        }

        public static Category fromId(String id) {
            return switch (id) {
                case "experimental" -> EXPERIMENTAL;
                case "deprecated" -> DEPRECATED;
                case "performance" -> PERFORMANCE;
                default -> null;
            };
        }
    }

    /**
     * Prints a warning, unless $VERBOSE is nil.
     */
    @Override
    @Deprecated
    public void warn(ID id, String fileName, String message) {
        if (!runtime.warningsEnabled()) return;

        ThreadContext context = runtime.getCurrentContext();
        warn(context, newString(context, fileName + " warning: " + message + '\n'));
    }

    @Deprecated
    public static IRubyObject warn(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return switch (args.length) {
            case 1 -> warn(context, recv, args[0]);
            case 2 -> warn(context, recv, args[0], args[1]);
            default -> throw argumentError(context, args.length, 1, 2);
        };
    }
}
