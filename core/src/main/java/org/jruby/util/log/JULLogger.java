/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2015-2015 The JRuby Community (and contribs)
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
package org.jruby.util.log;

import java.util.logging.Level;

/**
 * Logger which delegates to {@link java.util.logging.Logger}.
 *
 * @author kares
 */
public class JULLogger implements Logger {

    private final java.util.logging.Logger logger; // our delegate

    public JULLogger(final String loggerName) {
        this( java.util.logging.Logger.getLogger(loggerName) );
    }

    public JULLogger(final Class<?> loggerClass) {
        this( java.util.logging.Logger.getLogger(loggerClass.getName()) );
    }

    protected JULLogger(final java.util.logging.Logger logger) {
        this.logger = logger;
    }

    public String getName() {
        return logger.getName();
    }

    public void warn(String message, Object... args) {
        log(Level.WARNING, adjustPattern(message), args);
    }

    public void warn(Throwable throwable) {
        log(Level.WARNING, null, throwable);
    }

    public void warn(String message, Throwable throwable) {
        log(Level.WARNING, message, throwable);
    }

    public void error(String message, Object... args) {
        log(Level.SEVERE, adjustPattern(message), args);
    }

    public void error(Throwable throwable) {
        log(Level.SEVERE, null, throwable);
    }

    public void error(String message, Throwable throwable) {
        log(Level.SEVERE, message, throwable);
    }

    public void info(String message, Object... args) {
        if (logger.isLoggable(Level.INFO)) {
            log(Level.INFO, adjustPattern(message), args);
        }
    }

    public void info(Throwable throwable) {
        log(Level.INFO, null, throwable);
    }

    public void info(String message, Throwable throwable) {
        log(Level.INFO, message, throwable);
    }

    public void debug(String message, Object... args) {
        if (logger.isLoggable(Level.FINE)) {
            log(Level.FINE, adjustPattern(message), args);
        }
    }

    public void debug(Throwable throwable) {
        log(Level.FINE, null, throwable);
    }

    public void debug(String message, Throwable throwable) {
        log(Level.FINE, message, throwable);
    }


    protected void log(Level level, String message, Object... args) {
        final String souceClass = null;
        final String souceMethod = null;
        logger.logp(level, souceClass, souceMethod, message, args);
    }

    protected void log(Level level, String message, Throwable ex) {
        final String souceClass = null;
        final String souceMethod = null;
        logger.logp(level, souceClass, souceMethod, message, ex);
    }

    public boolean isDebugEnabled() {
        return logger.isLoggable(Level.FINE);
    }

    public void setDebugEnable(boolean debug) {
        logger.setLevel(debug ? Level.FINE : Level.INFO);
    }

    // adjust {} to JUL conventions e.g. "hello {} {}" -> "hello {0} {1}"
    static String adjustPattern(final String messagePattern) {
        if (messagePattern == null) return null;

        StringBuilder julPattern = null;
        final int len = messagePattern.length(); int last = 0; int counter = 0;
        for ( int i = 0; i < len; i++ ) {
            if ( messagePattern.charAt(i) == '{' &&
                ( i == 0 || (i > 0 && messagePattern.charAt(i - 1) != '\\' ) ) &&
                ( i < len - 1 && messagePattern.charAt(i + 1) == '}' ) ) {
                if (julPattern == null) {
                    julPattern = new StringBuilder(len + 8);
                }
                julPattern.
                    append(messagePattern, last, i).
                    append('{').append(counter++).append('}');
                last = i + 2;
            }
        }

        if (julPattern != null) {
            if (last < len) {
                julPattern.append(messagePattern, last, len);
            }
            return julPattern.toString();
        }

        return messagePattern; // no {} place holders
    }

}
