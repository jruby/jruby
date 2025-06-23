package org.jruby.util.time;

import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.IllegalFieldValueException;
import org.jruby.RubyBoolean;
import org.jruby.RubyFloat;
import org.jruby.RubyInteger;
import org.jruby.RubyNumeric;
import org.jruby.RubyRational;
import org.jruby.RubyString;
import org.jruby.RubyTime;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.RubyRational.rationalCanonicalize;
import static org.jruby.RubyTime.TIME_SCALE;
import static org.jruby.RubyTime.TIME_SCALE_DIGITS;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.toDouble;
import static org.jruby.api.Convert.toInt;
import static org.jruby.api.Error.argumentError;

public class TimeArgs {
    private final int year;
    private final int month;
    private final int day;
    private final int hour;
    private final int minute;
    private final int second;
    private final IRubyObject secondObj;
    private final IRubyObject usecObj;
    private final boolean dst;

    public TimeArgs(ThreadContext context, IRubyObject year, IRubyObject month, IRubyObject day, IRubyObject hour,
                    IRubyObject minute, IRubyObject second, IRubyObject usec, boolean dst) {
        this.year = parseYear(context, year);
        this.month = parseMonth(context, month);
        this.day = parseIntOrDefault(context, day, 1);
        this.hour = parseIntOrDefault(context, hour, 0);
        this.minute = parseIntOrDefault(context, minute, 0);

        this.secondObj = parseIntArg(context, second);
        this.second = parseIntOrDefault(context, secondObj, 0);

        validateDayHourMinuteSecond(context);

        this.usecObj = usec;
        this.dst = dst;
    }

    public TimeArgs(ThreadContext context, IRubyObject[] args) {
        IRubyObject nil = context.nil;

        IRubyObject year, month, day, hour, minute, second, usec;

        month = day = hour = minute = second = usec = nil;

        boolean dst = false;

        switch (args.length) {
            default: // zero args or more than 10 args
                throw new RuntimeException("BUG: should not get here");

            case 10:
                // smhDMY format
                if (args[8] instanceof RubyBoolean) dst = args[8].isTrue();

                year = args[5];
                month = args[4];
                day = args[3];
                hour = args[2];
                minute = args[1];
                second = args[0];
                usec = nil;
                break;

            // YMDhms formats
            case 8:
                // Ignores usec if 8 args (for compatibility with parse-date) or if not supplied.
                args[6] = nil;

            case 9:

            case 7: usec = args[6];
            case 6: second = args[5];
            case 5: minute = args[4];
            case 4: hour = args[3];
            case 3: day = args[2];
            case 2: month = args[1];
            case 1: year = args[0];
        }

        this.year = parseYear(context, year);
        this.month = parseMonth(context, month);
        this.day = parseIntOrDefault(context, day, 1);
        this.hour = parseIntOrDefault(context, hour, 0);
        this.minute = parseIntOrDefault(context, minute, 0);

        this.secondObj = parseIntArg(context, second);
        this.second = parseIntOrDefault(context, secondObj, 0);

        validateDayHourMinuteSecond(context);

        this.usecObj = usec;
        this.dst = dst;
    }

    public void initializeTime(ThreadContext context, RubyTime time, DateTimeZone dtz) {
        // set up with min values and then add to allow rolling over
        DateTime dt = new DateTime(year, month, 1, 0, 0, 0, 0, DateTimeZone.UTC);
        long instant = dt.getMillis();

        final Chronology chrono = dt.getChronology();

        instant = chrono.days().add(instant, this.day - 1);
        instant = chrono.hours().add(instant, this.hour);
        instant = chrono.minutes().add(instant, this.minute);
        instant = chrono.seconds().add(instant, this.second);

        IRubyObject usecObj = this.usecObj;
        IRubyObject secondObj = this.secondObj;

        long millis = 0;
        long nanos = 0;

        if (usecObj.isNil()) {
            if (!secondObj.isNil()) {
                if (secondObj instanceof RubyRational subSecond) {
                    if (subSecond.isNegativeNumber(context)) throw argumentError(context, "argument out of range.");

                    double subSeconds;
                    var canon = rationalCanonicalize(context, subSecond);
                    if (canon instanceof RubyInteger rint) {
                        subSeconds = rint.getDoubleValue();
                    } else {
                        subSeconds = ((RubyRational) canon).getDoubleValue(context);
                    }

                    if (subSeconds > 0) {
                        float s = (long) subSeconds;
                        subSeconds = subSeconds - s;
                    }

                    subSeconds = subSeconds * TIME_SCALE;

                    millis = (long) subSeconds / 1_000_000;
                    nanos = (long) subSeconds % 1_000_000;
                } else {
                    double secs = toDouble(context, secondObj);

                    if (secs < 0 || secs >= TIME_SCALE) throw argumentError(context, "argument out of range.");

                    millis = (int) (secs * 1000) % 1000;
                    nanos = ((long) (secs * TIME_SCALE) % 1000000);
                }
            }
        } else if (usecObj instanceof RubyRational rat) {
            if (rat.isNegativeNumber(context)) throw argumentError(context, "argument out of range.");

            var subSeconds = rat.asDouble(context);

            millis = (long) subSeconds / 1_000_000;
            nanos = (long) subSeconds % 1_000_000;
        } else if (usecObj instanceof RubyFloat flo) {
            if (flo.isNegativeNumber(context)) throw argumentError(context, "argument out of range.");

            double micros = flo.asDouble(context);

            millis = (long) (micros / 1000);
            nanos = (long) Math.rint((micros * 1000) % 1_000_000);
        } else {
            int subSeconds = parseIntArg(context, usecObj).isNil() ? 0 : toInt(context, usecObj);

            if (subSeconds < 0) throw argumentError(context, "argument out of range.");

            millis = (long) subSeconds / 1_000_000;
            nanos = (long) subSeconds % 1_000_000;
        }

        instant = chrono.millis().add(instant, millis);

        try {
            dt = dt.withMillis(instant);
            dt = dt.withZoneRetainFields(dtz);
            dt = adjustZoneOffset(dtz, dt, dst);
        } catch (IllegalFieldValueException e) {
            throw argumentError(context, "time out of range");
        }

        time.setDateTime(dt);
        time.setNSec(nanos);
    }

    private int numberOfDigits(int anInt) {
        int count = 0;
        while (anInt > 0) {
            count++;
            anInt <<= 10;
        }
        return count - 1;
    }

    private static int parseYear(ThreadContext context, IRubyObject _year) {
        if (_year instanceof RubyString yr) {
            if (!yr.getEncoding().isAsciiCompatible()) {
                throw argumentError(context, "time string should have ASCII compatible encoding");
            }
            _year = RubyNumeric.str2inum(context.runtime, yr, 10, false, true);
        }

        return toInt(context, _year);
    }

    private static int parseMonth(ThreadContext context, IRubyObject _month) {
        if (_month.isNil()) return 1;

        int month;
        IRubyObject tmp = _month.checkStringType();

        if (!tmp.isNil()) {
            String monthStr = tmp.toString().toLowerCase();

            if (monthStr.length() == 3) {
                switch (monthStr) {
                    case "jan" : return  1;
                    case "feb" : return  2;
                    case "mar" : return  3;
                    case "apr" : return  4;
                    case "may" : return  5;
                    case "jun" : return  6;
                    case "jul" : return  7;
                    case "aug" : return  8;
                    case "sep" : return  9;
                    case "oct" : return 10;
                    case "nov" : return 11;
                    case "dec" : return 12;
                }
            }

            try {
                month = Integer.parseInt(monthStr);
            } catch (NumberFormatException ex) {
                throw argumentError(context, "argument out of range.");
            }
        } else {
            month = toInt(context, _month);
        }

        if (month < 1 || month > 12) {
            throw argumentError(context, "argument out of range: for month: " + month);
        }

        return month;
    }

    private static IRubyObject parseIntArg(ThreadContext context, IRubyObject arg) {
        if (arg == context.nil) return arg;

        if (arg instanceof RubyNumeric) return arg;

        final JavaSites.TimeSites sites = sites(context);

        if (sites.respond_to_to_int.respondsTo(context, arg, arg)) {
            return sites.to_int.call(context, arg, arg);
        }

        return sites.to_i.call(context, arg, arg);
    }

    private void validateDayHourMinuteSecond(ThreadContext context) {
        if (day < 1 || day > 31) throw argumentError(context, "argument out of range for day");

        if (hour < 0 || hour > 24) throw argumentError(context, "argument out of range for hour");

        if ((minute < 0 || minute > 59) || (hour == 24 && minute > 0)) {
            throw argumentError(context, "argument out of range for minute");
        }

        if ((second < 0 || second > 60) || (hour == 24 && second > 0)) {
            throw argumentError(context, "argument out of range");
        }
    }

    private static int parseIntOrDefault(ThreadContext context, IRubyObject obj, int def) {
        obj = parseIntArg(context, obj);
        return obj.isNil() ? def : toInt(context, obj);
    }

    private static DateTime adjustZoneOffset(DateTimeZone dtz, DateTime dt, boolean dst) {
        // If we're at a DST boundary, we need to choose the correct side of the boundary
        final DateTime beforeDstBoundary = dt.withEarlierOffsetAtOverlap();
        final DateTime afterDstBoundary = dt.withLaterOffsetAtOverlap();

        final int offsetBeforeBoundary = dtz.getOffset(beforeDstBoundary);
        final int offsetAfterBoundary = dtz.getOffset(afterDstBoundary);

        if (dst) {
            // If the time is during DST, we need to pick the time with the highest offset
            dt = offsetBeforeBoundary > offsetAfterBoundary ? beforeDstBoundary : afterDstBoundary;
        }
        else {
            dt = offsetBeforeBoundary > offsetAfterBoundary ? afterDstBoundary : beforeDstBoundary;
        }
        return dt;
    }

    private static JavaSites.TimeSites sites(ThreadContext context) {
        return context.sites.Time;
    }
}
