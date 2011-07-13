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
 * Copyright (C) 2001-2011 The JRuby Community (and contribs)
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
package org.jruby.util.log;

public class StandardErrorLogger implements Logger {

    private final String loggerName;
    private boolean debug = false;

    public StandardErrorLogger(String loggerName) {
        this.loggerName = loggerName;
    }

    public String getName() {
        return loggerName;
    }

    public void warn(String message, Object... args) {
        write(message, args);
    }

    public void warn(Throwable throwable) {
        writeStackTrace(throwable);
    }

    public void warn(String message, Throwable throwable) {
        write(message, throwable);
    }

    public void error(String message, Object... args) {
        write(message, args);
    }

    public void error(Throwable throwable) {
        writeStackTrace(throwable);
    }

    public void error(String message, Throwable throwable) {
        write(message, throwable);
    }

    public void info(String message, Object... args) {
        write(message, args);
    }

    public void info(Throwable throwable) {
        writeStackTrace(throwable);
    }

    public void info(String message, Throwable throwable) {
        write(message, throwable);
    }

    public void debug(String message, Object... args) {
        if (debug) {
            write(message, args);
        }
    }

    public void debug(Throwable throwable) {
        if (debug) {
            writeStackTrace(throwable);
        }
    }

    public void debug(String message, Throwable throwable) {
        if (debug) {
            write(message, throwable);
        }
    }

    public boolean isDebugEnabled() {
        return debug;
    }

    public void setDebugEnable(boolean debug) {
        this.debug = debug;
    }

    private void write(String message, Object[] args) {
        final StringBuilder builder = new StringBuilder();
        if (message != null) {
            final String[] strings = message.split("\\{\\}");
            if (args.length == 0 || strings.length == args.length) {
                for (int i = 0; i < strings.length; i++) {
                    builder.append(strings[i]);
                    if (args.length > 0) {
                        builder.append(args[i]);
                    }
                }
            } else {
                System.err.println("wrong number of placeholders / arguments");
            }
        }
        System.err.println(builder.toString());
    }

    private void write(String message, Throwable throwable) {
        System.err.println(message);
        writeStackTrace(throwable);
    }

    private void writeStackTrace(Throwable throwable) {
        throwable.printStackTrace(System.err);
    }
}
