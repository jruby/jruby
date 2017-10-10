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
 * Copyright (C) 2001-2015 The JRuby Community (and contribs)
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

import java.io.PrintStream;

/**
 * Default JRuby logger implementation, using {@link System.err}.
 */
public class StandardErrorLogger extends OutputStreamLogger {

    public StandardErrorLogger(String loggerName) {
        super(loggerName);
    }

    public StandardErrorLogger(Class<?> loggerClass) {
        super(loggerClass.getSimpleName());
    }

    public StandardErrorLogger(String loggerName, PrintStream stream) {
        super(loggerName, stream);
    }

    @Override
    protected void write(String message, String level, Object[] args) {
        // NOTE: stream is intentionally not set to System.err
        // thus when its programatically redirected it works !
        PrintStream stream = this.stream;
        if ( stream == null ) stream = System.err;

        CharSequence suble = substitute(message, args);
        stream.println(formatMessage(suble, level));
    }

    @Override
    protected void write(String message, String level, Throwable throwable) {
        // NOTE: stream is intentionally not set to System.err
        // thus when its programatically redirected it works !
        PrintStream stream = this.stream;
        if ( stream == null ) stream = System.err;

        synchronized (stream) {
            stream.println(formatMessage(message, level));
            writeStackTrace(stream, throwable);
        }
    }

}
