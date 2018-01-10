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

import java.io.PrintStream;

import org.joda.time.DateTime;

/**
 * Logger logging to an output (print) stream.
 * @author kares
 */
public class OutputStreamLogger implements Logger {

    private final String loggerName;
    private boolean debug = false;
    protected PrintStream stream; // volatile?

    public OutputStreamLogger(String loggerName) {
        this.loggerName = loggerName;
    }

    public OutputStreamLogger(String loggerName, PrintStream stream) {
        this.loggerName = loggerName;
        this.stream = stream;
    }

    public String getName() {
        return loggerName;
    }

    public PrintStream getStream() {
        return stream;
    }

    public void setStream(PrintStream stream) {
        this.stream = stream;
    }

    public void warn(String message, Object... args) {
        write(message, "WARN", args);
    }

    public void warn(Throwable throwable) {
        write("", "WARN", throwable);
    }

    public void warn(String message, Throwable throwable) {
        write(message, "WARN", throwable);
    }

    public void error(String message, Object... args) {
        write(message, "ERROR", args);
    }

    public void error(Throwable throwable) {
        write("", "ERROR", throwable);
    }

    public void error(String message, Throwable throwable) {
        write(message, "ERROR", throwable);
    }

    public void info(String message, Object... args) {
        write(message, "INFO", args);
    }

    public void info(Throwable throwable) {
        write("", "INFO", throwable);
    }

    public void info(String message, Throwable throwable) {
        write(message, "INFO", throwable);
    }

    public void debug(String message, Object... args) {
        if (debug) write(message, "DEBUG", args);
    }

    public void debug(Throwable throwable) {
        if (debug) write("", "DEBUG", throwable);
    }

    public void debug(String message, Throwable throwable) {
        if (debug) write(message, "DEBUG", throwable);
    }

    public boolean isDebugEnabled() {
        return debug;
    }

    public void setDebugEnable(boolean debug) {
        this.debug = debug;
    }

    protected void write(String message, String level, Object[] args) {
        CharSequence suble = substitute(message, args);
        stream.println(formatMessage(suble, level));
    }

    protected void write(String message, String level, Throwable throwable) {
        synchronized (stream) {
            stream.println(formatMessage(message, level));
            //if ( message == null || message.length() > 0 ) stream.print(' ');
            writeStackTrace(stream, throwable);
        }
    }

    protected static void writeStackTrace(PrintStream stream, Throwable throwable) {
        if (throwable == null) {
            throw new IllegalArgumentException("null throwable");
        }

        throwable.printStackTrace(stream);
    }

    protected String formatMessage(CharSequence message, String level) {
        // 2015-11-04T11:29:41.759+01:00 [main] INFO SampleLogger : hello world
        return new StringBuilder(64)
            .append(new DateTime(System.currentTimeMillis()))
            .append(' ')
            .append('[').append(Thread.currentThread().getName()).append(']')
            .append(' ')
            .append(level)
            .append(' ')
            .append(loggerName)
            .append(" : ")
            .append(message)
            .toString();
    }

    /**
     * Message pattern {} substitution.
     * @param messagePattern
     * @param args
     */
    static CharSequence substitute(final String messagePattern, Object... args) {
        if (messagePattern == null) {
            final StringBuilder msg = new StringBuilder();
            for (int i = 0; i < args.length; i++) msg.append( args[i] );
            return msg;
        }

        StringBuilder msg = null;
        final int len = messagePattern.length(); int s = 0; int a = 0;
        for ( int i = 0; i < len; i++ ) {
            if ( messagePattern.charAt(i) == '{' &&
                ( i == 0 || (i > 0 && messagePattern.charAt(i - 1) != '\\' ) ) &&
                ( i < len - 1 && messagePattern.charAt(i + 1) == '}' ) ) {

                if (msg == null) {
                    msg = new StringBuilder(len + 48);
                }

                msg.append(messagePattern, s, i); s = i + 2;

                if ( a < args.length ) {
                    msg.append( args[a++] );
                }
                else { // invalid e.g. ("hello {} {}", "world")
                    msg.append( "{!abs-arg!}" ); // absent argument
                }
            }
        }

        if ( msg != null ) {
            if ( s < len ) msg.append(messagePattern, s, len);
            return msg;
        }
        return messagePattern; // no substitution place-holders
    }

}
