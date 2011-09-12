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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.joda.time.DateTime;

public class JavaUtilLoggingLogger implements Logger {
    private final java.util.logging.Logger logger;

    public JavaUtilLoggingLogger(String loggerName) {
        logger = java.util.logging.Logger.getLogger(loggerName);
        logger.setUseParentHandlers(false);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord lr) {
                StringBuilder sb = new StringBuilder();
                sb
                        .append(new DateTime(lr.getMillis()).toString())
                        .append(": ")
                        .append(lr.getLoggerName())
                        .append(": ")
                        .append(lr.getMessage())
                        .append("\n");
                return sb.toString();
            }
        });
        logger.addHandler(handler);
    }

    public String getName() {
        return logger.getName();
    }

    public void warn(String message, Object... args) {
        if (logger.isLoggable(Level.WARNING)) {
            logger.warning(message + (args.length == 0 ? "" : "\n" + Arrays.toString(args)));
        }
    }

    public void warn(Throwable throwable) {
        if (logger.isLoggable(Level.WARNING)) {
            logger.warning(getLoggingOutput(throwable));
        }
    }

    public void warn(String message, Throwable throwable) {
        if (logger.isLoggable(Level.WARNING)) {
            logger.warning(message + "\n" + getLoggingOutput(throwable));
        }
    }

    public void error(String message, Object... args) {
        if (logger.isLoggable(Level.SEVERE)) {
            logger.severe(message + (args.length == 0 ? "" : "\n" + Arrays.toString(args)));
        }
    }

    public void error(Throwable throwable) {
        if (logger.isLoggable(Level.SEVERE)) {
            logger.severe(getLoggingOutput(throwable));
        }
    }

    public void error(String message, Throwable throwable) {
        if (logger.isLoggable(Level.SEVERE)) {
            logger.severe(message + "\n" + getLoggingOutput(throwable));
        }
    }

    public void info(String message, Object... args) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(message + (args.length == 0 ? "" : "\n" + Arrays.toString(args)));
        }
    }

    public void info(Throwable throwable) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(getLoggingOutput(throwable));
        }
    }

    public void info(String message, Throwable throwable) {
        if (logger.isLoggable(Level.INFO)) {
            logger.info(message + "\n" + getLoggingOutput(throwable));
        }
    }

    public void debug(String message, Object... args) {
        if (isDebugEnabled()) {
            logger.finest(message + (args.length == 0 ? "" : "\n" + Arrays.toString(args)));
        }
    }

    public void debug(Throwable throwable) {
        if (isDebugEnabled()) {
            logger.finest(getLoggingOutput(throwable));
        }
    }

    public void debug(String message, Throwable throwable) {
        if (isDebugEnabled()) {
            logger.finest(message + "\n" + getLoggingOutput(throwable));
        }
    }

    public boolean isDebugEnabled() {
        return logger.isLoggable(Level.FINEST);
    }

    public void setDebugEnable(boolean debug) {
        logger.setLevel(Level.FINEST);
    }
    
    private String getLoggingOutput(Throwable throwable) {
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

}
