package org.jruby.util.log;

import java.io.PrintStream;
import org.joda.time.DateTime;

/**
 * Removes more of the date and tries to compact log format so it still prints nicely on a line.
 * For a main thread the prefix of the log line is only 24 characters.
 */
public class MinimizedStandardErrorLogger extends StandardErrorLogger {
    public MinimizedStandardErrorLogger(String loggerName) {
        super(loggerName);
    }

    public MinimizedStandardErrorLogger(Class<?> loggerClass) {
        super(loggerClass);
    }

    public MinimizedStandardErrorLogger(String loggerName, PrintStream stream) {
        super(loggerName, stream);
    }

    /*
     * logname : level : hr : min : sec . ms : mesg
     * Profiler:I:11:16:37.981: DONE processing candidates
     *
     * If not the main thread it will add in thread name after ms and before mesg.
     */
    @Override
    protected String formatMessage(CharSequence message, String level) {
        DateTime time = new DateTime(System.currentTimeMillis());
        int hour = time.getHourOfDay();
        int minute = time.getMinuteOfHour();
        int second = time.getSecondOfMinute();
        int millis = time.getMillisOfSecond();

        StringBuilder buf = new StringBuilder(64)
                .append(getName()).append(':')
                .append(level.charAt(0)).append(':');

        if (hour < 10) buf.append('0');
        buf.append(hour).append(':');

        if (minute < 10) buf.append('0');
        buf.append(minute).append(':');

        if (second < 10) buf.append('0');
        buf.append(second).append('.');

        if (millis < 10) {
            buf.append("00");
        } else if (millis < 100) {
            buf.append('0');
        }
        buf.append(millis);

        if (!"main".equals(Thread.currentThread().getName())) {
            buf.append(" [").append(Thread.currentThread().getName()).append(']');
        }

        buf.append(": ").append(message);

        return buf.toString();
    }
}
