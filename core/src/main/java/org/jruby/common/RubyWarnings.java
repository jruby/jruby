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
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.backtrace.RubyStackTraceElement;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.TypeConverter;

/**
 *
 */
public class RubyWarnings implements IRubyWarnings, WarnCallback {
    private final Ruby runtime;
    private final Set<ID> oncelers = EnumSet.allOf(IRubyWarnings.ID.class);

    public RubyWarnings(Ruby runtime) {
        this.runtime = runtime;
    }

    public static RubyModule createWarningModule(Ruby runtime) {
        RubyModule warning = runtime.defineModule("Warning");

        warning.defineAnnotatedMethods(RubyWarnings.class);
        warning.extend_object(warning);

        return warning;
    }

    @Override
    public void warn(String message) {
        warn(ID.MISCELLANEOUS, message);
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
        if (!runtime.warningsEnabled()) return;

        String buffer = fileName + ':' + (lineNumber + 1) + ": warning: " + message + '\n';
        RubyString errorString = runtime.newString(buffer);

        writeWarningDyncall(runtime.getCurrentContext(), errorString);
    }

    // MRI: rb_write_warning_str
    public static void writeWarningDyncall(ThreadContext context, RubyString errorString) {
        RubyModule warning = context.runtime.getWarning();

        sites(context).warn.call(context, warning, warning, errorString);
    }

    // MR: rb_write_error_str
    public static void writeWarningToError(ThreadContext context, RubyString errorString) {
        Ruby runtime = context.runtime;

        IRubyObject errorStream = runtime.getGlobalVariables().get("$stderr");
        RubyModule warning = runtime.getWarning();

        sites(context).write.call(context, warning, errorStream, errorString);
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

        // 1 is subtracted here because getRubyStackTrace is 1-indexed.
        warn(id, file, line - 1, message);
    }

    public void warn(String filename, String message) {
        if (!runtime.warningsEnabled()) return;

        RubyString errorString = runtime.newString(filename + ": " + message + '\n');

        writeWarningDyncall(runtime.getCurrentContext(), errorString);
    }

    public void warnOnce(ID id, String message) {
        if (!runtime.warningsEnabled()) return;
        if (oncelers.contains(id)) return;

        oncelers.add(id);
        warn(id, message);
    }

    /**
     * Verbose mode warning methods, their contract is that consumer must explicitly check for runtime.isVerbose()
     * before calling them
     */
    public void warning(String message) {
        if (!isVerbose()) return;
        if (!runtime.warningsEnabled()) return;

        warning(ID.MISCELLANEOUS, message);
    }

    @Override
    public void warning(ID id, String message) {
        if (!runtime.warningsEnabled() || !runtime.isVerbose()) return;

        writeWarning(runtime, id, message);
    }

    private static void writeWarning(Ruby runtime, ID id, String message) {
        RubyStackTraceElement stack = runtime.getCurrentContext().getSingleBacktrace();
        String file;
        int line;

        if (stack == null) {
            file = "(unknown)";
            line = -1;
        } else {
            file = stack.getFileName();
            line = stack.getLineNumber();
        }

        runtime.getWarnings().warning(id, file, line, message);
    }

    /**
     * Prints a warning, only in verbose mode.
     */
    @Override
    public void warning(ID id, String fileName, int lineNumber, String message) {
        if (!runtime.warningsEnabled() || !runtime.isVerbose()) return;

        warn(id, fileName, lineNumber, message);
    }

    @JRubyMethod
    public static IRubyObject warn(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        Ruby runtime = context.runtime;

        TypeConverter.checkType(context, arg, runtime.getString());
        RubyString str = (RubyString) arg;
        if (!str.getEncoding().isAsciiCompatible()) {
            throw runtime.newEncodingCompatibilityError("ASCII incompatible encoding: " + str.getEncoding());
        }
        writeWarningToError(runtime.getCurrentContext(), str);
        return context.nil;
    }

    private static JavaSites.WarningSites sites(ThreadContext context) {
        return context.sites.Warning;
    }

    /**
     * Prints a warning, unless $VERBOSE is nil.
     */
    @Override
    @Deprecated
    public void warn(ID id, String fileName, String message) {
        if (!runtime.warningsEnabled()) return;

        IRubyObject errorStream = runtime.getGlobalVariables().get("$stderr");
        String buffer = fileName + " warning: " + message + '\n';
        errorStream.callMethod(runtime.getCurrentContext(), "write", runtime.newString(buffer));
    }
}
