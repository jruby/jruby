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

/**
 * Using SLF4J API as a logging backend.
 * @author kares
 */
public class SLF4JLogger implements Logger {

    private final org.slf4j.Logger logger;

    public SLF4JLogger(final String loggerName) {
        this( org.slf4j.LoggerFactory.getLogger(loggerName) );
    }

    public SLF4JLogger(final Class<?> loggerClass) {
        this( org.slf4j.LoggerFactory.getLogger(loggerClass) );
    }

    protected SLF4JLogger(final org.slf4j.Logger logger) {
        this.logger = logger;
    }

    public String getName() {
        return logger.getName();
    }

    public void warn(String message, Object... args) {
        logger.warn(message, args);
    }

    public void warn(Throwable throwable) {
        logger.warn("", throwable);
    }

    public void warn(String message, Throwable throwable) {
        logger.warn(message, throwable);
    }

    public void error(String message, Object... args) {
        logger.error(message, args);
    }

    public void error(Throwable throwable) {
        logger.error("", throwable);
    }

    public void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    public void info(String message, Object... args) {
        logger.info(message, args);
    }

    public void info(Throwable throwable) {
        logger.info("", throwable);
    }

    public void info(String message, Throwable throwable) {
        logger.info(message, throwable);
    }

    public void debug(String message, Object... args) {
        logger.debug(message, args);
    }

    public void debug(Throwable throwable) {
        logger.debug("", throwable);
    }

    public void debug(String message, Throwable throwable) {
        logger.debug(message, throwable);
    }

    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    public void setDebugEnable(boolean debug) {
        // no-op ignore
    }

}
