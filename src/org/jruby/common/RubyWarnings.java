/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.common;

import org.joni.WarnCallback;
import org.jruby.Ruby;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.backtrace.BacktraceData;
import org.jruby.runtime.backtrace.RubyStackTraceElement;
import org.jruby.runtime.backtrace.TraceType;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.HashSet;
import java.util.Set;

/** 
 *
 */
public class RubyWarnings implements IRubyWarnings, WarnCallback {
    private final Ruby runtime;
    private final Set<ID> oncelers = new HashSet<ID>();

    public RubyWarnings(Ruby runtime) {
        this.runtime = runtime;
    }

    public void warn(String message) {
        warn(ID.MISCELLANEOUS, message);
    }

    public Ruby getRuntime() {
        return runtime;
    }

    public boolean isVerbose() {
        return runtime.isVerbose();
    }

    /**
     * Prints a warning, unless $VERBOSE is nil.
     */
    public void warn(ID id, ISourcePosition position, String message) {
        if (!runtime.warningsEnabled()) return;

        warn(id, position.getFile(), position.getStartLine(), message);
    }

    /**
     * Prints a warning, unless $VERBOSE is nil.
     */
    public void warn(ID id, String fileName, int lineNumber, String message) {
        if (!runtime.warningsEnabled()) return;

        StringBuilder buffer = new StringBuilder(100);

        buffer.append(fileName).append(':').append(lineNumber + 1).append(' ');
        buffer.append("warning: ").append(message).append('\n');
        IRubyObject errorStream = runtime.getGlobalVariables().get("$stderr");
        errorStream.callMethod(runtime.getCurrentContext(), "write", runtime.newString(buffer.toString()));
    }

    public void warn(ID id, String message) {
        if (!runtime.warningsEnabled()) return;

        RubyStackTraceElement[] stack = getRubyStackTrace(runtime);
        String file;
        int line;

        if (stack.length == 0) {
            file = "(unknown)";
            line = -1;
        } else {
            file = stack[0].getFileName();
            line = stack[0].getLineNumber();
        }

        warn(id, file, line, message);
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
        if (!runtime.warningsEnabled()) return;

        warning(ID.MISCELLANEOUS, message);
    }

    public void warning(ID id, String message) {
        if (!runtime.warningsEnabled()) return;

        RubyStackTraceElement[] stack = getRubyStackTrace(runtime);
        String file;
        int line;

        if (stack.length == 0) {
            file = "(unknown)";
            line = -1;
        } else {
            file = stack[0].getFileName();
            line = stack[0].getLineNumber();
        }

        warning(id, file, line, message);
    }

    /**
     * Prints a warning, only in verbose mode.
     */
    public void warning(ID id, ISourcePosition position, String message) {
        if (!runtime.warningsEnabled()) return;

        warning(id, position.getFile(), position.getStartLine(), message);
    }

    /**
     * Prints a warning, only in verbose mode.
     */
    public void warning(ID id, String fileName, int lineNumber, String message) {
        assert isVerbose();

        if (!runtime.warningsEnabled()) return;

        warn(id, fileName, lineNumber, message);
    }

    private static RubyStackTraceElement[] getRubyStackTrace(Ruby runtime) {
        ThreadContext context = runtime.getCurrentContext();
        RubyStackTraceElement[] stack = context.createWarningBacktrace(runtime);

        return stack;
    }

    @Deprecated
    public void warn(ID id, ISourcePosition position, String message, Object... data) {
        warn(id, position.getFile(), position.getStartLine(), message, data);
    }

    @Deprecated
    public void warn(ID id, String fileName, int lineNumber, String message, Object... data) {
        if (!runtime.warningsEnabled()) return; // TODO make an assert here

        StringBuilder buffer = new StringBuilder(100);

        buffer.append(fileName).append(':').append(lineNumber + 1).append(' ');
        buffer.append("warning: ").append(message).append('\n');
        IRubyObject errorStream = runtime.getGlobalVariables().get("$stderr");
        errorStream.callMethod(runtime.getCurrentContext(), "write", runtime.newString(buffer.toString()));
    }

    @Deprecated
    public void warn(ID id, String message, Object... data) {
        ThreadContext context = runtime.getCurrentContext();
        warn(id, context.getFile(), context.getLine(), message, data);
    }

    @Deprecated
    public void warning(String message, Object... data) {
        warning(ID.MISCELLANEOUS, message, data);
    }

    @Deprecated
    public void warning(ID id, String message, Object... data) {
        ThreadContext context = runtime.getCurrentContext();
        warning(id, context.getFile(), context.getLine(), message, data);
    }

    @Deprecated
    public void warning(ID id, ISourcePosition position, String message, Object... data) {
        warning(id, position.getFile(), position.getStartLine(), message, data);
    }

    @Deprecated
    public void warning(ID id, String fileName, int lineNumber, String message, Object... data) {
        assert isVerbose(); 
        warn(id, fileName, lineNumber, message, data);
    }
}
