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
package org.jruby.truffle.parser;

import org.joni.WarnCallback;
import org.jruby.Ruby;
import org.jruby.common.IRubyWarnings;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.backtrace.RubyStackTraceElement;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.debug.DebugHelpers;

import java.util.EnumSet;
import java.util.Set;

/**
 *
 */
public class RubyWarnings implements IRubyWarnings, WarnCallback {
    private final RubyContext runtime;
    private final Set<ID> oncelers = EnumSet.allOf(ID.class);

    public RubyWarnings(RubyContext runtime) {
        this.runtime = runtime;
    }

    @Override
    public void warn(String message) {
        warn(ID.MISCELLANEOUS, message);
    }

    @Override
    public Ruby getRuntime() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isVerbose() {
        return runtime.isVerbose();
    }

    /**
     * Prints a warning, unless $VERBOSE is nil.
     */
    @Override
    public void warn(ID id, ISourcePosition position, String message) {
        if (!runtime.warningsEnabled()) return;

        warn(id, position.getFile(), position.getLine() + 1, message);
    }

    /**
     * Prints a warning, unless $VERBOSE is nil.
     */
    @SuppressWarnings("deprecation")
    @Override
    public void warn(ID id, String fileName, int lineNumber, String message) {
        if (!runtime.warningsEnabled()) return;

        StringBuilder buffer = new StringBuilder(100);

        buffer.append(fileName).append(':').append(lineNumber).append(": ");
        buffer.append("warning: ").append(message).append('\n');
        DebugHelpers.eval(runtime, "$stderr.write Truffle::Interop.from_java_string(message)", "message", buffer.toString());
    }

    /**
     * Prints a warning, unless $VERBOSE is nil.
     */
    @SuppressWarnings("deprecation")
    @Override
    public void warn(ID id, String fileName, String message) {
        if (!runtime.warningsEnabled()) return;

        StringBuilder buffer = new StringBuilder(100);

        buffer.append(fileName).append(' ');
        buffer.append("warning: ").append(message).append('\n');
        DebugHelpers.eval(runtime, "$stderr.write Truffle::Interop.from_java_string(message)", "message", buffer.toString());
    }

    @Override
    public void warn(ID id, String message) {
        if (!runtime.warningsEnabled()) return;

        RubyStackTraceElement[] stack = new RubyStackTraceElement[]{}; //getRubyStackTrace(runtime);
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
        if (!isVerbose()) return;
        if (!runtime.warningsEnabled()) return;

        warning(ID.MISCELLANEOUS, message);
    }

    @Override
    public void warning(ID id, String message) {
        if (!runtime.warningsEnabled() || !runtime.isVerbose()) return;

        RubyStackTraceElement[] stack = new RubyStackTraceElement[]{};//getRubyStackTrace(runtime);
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
    @Override
    public void warning(ID id, ISourcePosition position, String message) {
        warning(id, position.getFile(), position.getLine() + 1, message);
    }

    /**
     * Prints a warning, only in verbose mode.
     */
    @Override
    public void warning(ID id, String fileName, int lineNumber, String message) {
        if (!runtime.warningsEnabled() || !runtime.isVerbose()) return;

        warn(id, fileName, lineNumber, message);
    }

    private static RubyStackTraceElement[] getRubyStackTrace(Ruby runtime) {
        ThreadContext context = runtime.getCurrentContext();
        RubyStackTraceElement[] stack = context.createWarningBacktrace(runtime);

        return stack;
    }
}
