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
 * Copyright (C) 2001-2011 The JRuby Community (and contribs)
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

import java.lang.reflect.Constructor;
import org.jruby.util.cli.Options;

public class LoggerFactory {

    static final String LOGGER_CLASS = Options.LOGGER_CLASS.load();
    static final String BACKUP_LOGGER_CLASS = "org.jruby.util.log.StandardErrorLogger";

    static final Constructor<? extends Logger> LOGGER = resolveLoggerConstructor( LOGGER_CLASS );

    static Constructor<? extends Logger> resolveLoggerConstructor(final String className) {
        Constructor<? extends Logger> loggerCtor;
        try {
            @SuppressWarnings("unchecked")
            Class<? extends Logger> klass = (Class<? extends Logger>) Class.forName(className);
            loggerCtor = klass.getDeclaredConstructor(String.class);
            loggerCtor.newInstance("LoggerFactory"); // check its working
        }
        catch (Exception e1) {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends Logger> klass = (Class<? extends Logger>) Class.forName(BACKUP_LOGGER_CLASS);
                loggerCtor = klass.getDeclaredConstructor(String.class);
                Logger log = loggerCtor.newInstance("LoggerFactory");
                // log failure to load passeed -Djruby.logger.class :
                log.info("failed to create logger \"" + className + "\", using \"" + BACKUP_LOGGER_CLASS + "\"");
            }
            catch (Exception e2) {
                throw new IllegalStateException("unable to instantiate any logger", e1);
            }
        }
        return loggerCtor;
    }

    public static Logger getLogger(String loggerName) {
        try {
            return LOGGER.newInstance(loggerName);
        }
        catch (Exception ex) {
            return getLoggerFallback(loggerName, ex);
        }
    }

    private static Logger getLoggerFallback(final String loggerName, final Exception ex) {
        Throwable rootCause = ex;
        // unwrap reflection exception wrappers
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }

        if (rootCause instanceof SecurityException) {
            return new StandardErrorLogger(loggerName);
        }

        throw new IllegalStateException("unable to instantiate logger", ex);
    }

}
