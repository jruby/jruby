/*
 * RubyTime.java - No description
 * Created on 1. Dec 2001, 15:53
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Chad Fowler
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Chad Fowler <chadfowler@chadfowler.com>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This file is part of JRuby
 * 
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */
package org.jruby;

import java.text.*;
import java.util.*;

import org.jruby.ast.*;
import org.jruby.exceptions.*;
import org.jruby.runtime.*;
import org.jruby.util.*;

/** The Time class.
 * 
 * @author chadfowler, jpetersen
 */
public class RubyTime extends RubyObject {
    private Calendar cal;
    private long usec;

    private static RubyDateFormat rubyDateFormat = new RubyDateFormat("-", Locale.US);
    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("-", Locale.US);

    public RubyTime(Ruby ruby, RubyClass rubyClass) {
        super(ruby, rubyClass);
    }

    public static RubyClass createTimeClass(Ruby ruby) {
        RubyClass timeClass = ruby.defineClass("Time", ruby.getClasses().getObjectClass());
        timeClass.includeModule(ruby.getClasses().getComparableModule());

        timeClass.defineSingletonMethod("at", CallbackFactory.getOptSingletonMethod(RubyTime.class, "new_at"));

        timeClass.defineSingletonMethod("new", CallbackFactory.getSingletonMethod(RubyTime.class, "s_new"));
        timeClass.defineSingletonMethod("now", CallbackFactory.getSingletonMethod(RubyTime.class, "s_new"));

        timeClass.defineSingletonMethod("local", CallbackFactory.getOptSingletonMethod(RubyTime.class, "new_local"));
        timeClass.defineSingletonMethod("mktime", CallbackFactory.getOptSingletonMethod(RubyTime.class, "new_local"));

        timeClass.defineSingletonMethod("gm", CallbackFactory.getOptSingletonMethod(RubyTime.class, "new_utc"));
        timeClass.defineSingletonMethod("utc", CallbackFactory.getOptSingletonMethod(RubyTime.class, "new_utc"));

        timeClass.defineMethod("hash", CallbackFactory.getMethod(RubyTime.class, "hash"));
        timeClass.defineMethod("<=>", CallbackFactory.getMethod(RubyTime.class, "op_cmp", RubyObject.class));
        timeClass.defineMethod("-", CallbackFactory.getMethod(RubyTime.class, "op_minus", RubyObject.class));
        timeClass.defineMethod("+", CallbackFactory.getMethod(RubyTime.class, "op_plus", RubyObject.class));

        timeClass.defineMethod("sec", CallbackFactory.getMethod(RubyTime.class, "sec"));
        timeClass.defineMethod("min", CallbackFactory.getMethod(RubyTime.class, "min"));
        timeClass.defineMethod("hour", CallbackFactory.getMethod(RubyTime.class, "hour"));
        timeClass.defineMethod("day", CallbackFactory.getMethod(RubyTime.class, "mday"));
        timeClass.defineMethod("mday", CallbackFactory.getMethod(RubyTime.class, "mday"));
        timeClass.defineMethod("mon", CallbackFactory.getMethod(RubyTime.class, "month"));
        timeClass.defineMethod("month", CallbackFactory.getMethod(RubyTime.class, "month"));
        timeClass.defineMethod("year", CallbackFactory.getMethod(RubyTime.class, "year"));
        timeClass.defineMethod("wday", CallbackFactory.getMethod(RubyTime.class, "wday"));
        timeClass.defineMethod("yday", CallbackFactory.getMethod(RubyTime.class, "yday"));
        timeClass.defineMethod("isdst", CallbackFactory.getMethod(RubyTime.class, "isdst"));
        timeClass.defineMethod("zone", CallbackFactory.getMethod(RubyTime.class, "zone"));
        timeClass.defineMethod("to_a", CallbackFactory.getMethod(RubyTime.class, "to_a"));
        
        timeClass.defineMethod("to_f", CallbackFactory.getMethod(RubyTime.class, "to_f"));

        timeClass.defineMethod("asctime", CallbackFactory.getMethod(RubyTime.class, "asctime"));
        timeClass.defineMethod("ctime", CallbackFactory.getMethod(RubyTime.class, "asctime"));
        timeClass.defineMethod("to_s", CallbackFactory.getMethod(RubyTime.class, "to_s"));
        timeClass.defineMethod("inspect", CallbackFactory.getMethod(RubyTime.class, "to_s"));
        timeClass.defineMethod("strftime", CallbackFactory.getMethod(RubyTime.class, "strftime", RubyObject.class));

        timeClass.defineMethod("tv_usec", CallbackFactory.getMethod(RubyTime.class, "usec"));
        timeClass.defineMethod("usec", CallbackFactory.getMethod(RubyTime.class, "usec"));
        timeClass.defineMethod("tv_sec", CallbackFactory.getMethod(RubyTime.class, "to_i"));
        timeClass.defineMethod("to_i", CallbackFactory.getMethod(RubyTime.class, "to_i"));

        timeClass.defineMethod("gmtime", CallbackFactory.getMethod(RubyTime.class, "gmtime"));
        timeClass.defineMethod("utc", CallbackFactory.getMethod(RubyTime.class, "gmtime"));
        timeClass.defineMethod("gmt?", CallbackFactory.getMethod(RubyTime.class, "gmt"));
        timeClass.defineMethod("gmtime?", CallbackFactory.getMethod(RubyTime.class, "gmt"));
        timeClass.defineMethod("utc?", CallbackFactory.getMethod(RubyTime.class, "gmt"));
        timeClass.defineMethod("localtime", CallbackFactory.getMethod(RubyTime.class, "localtime"));

        return timeClass;
    }

    public static RubyTime s_new(Ruby ruby, RubyObject rubyClass) {
        RubyObject[] args = new RubyObject[1];
        args[0] = new RubyFixnum(ruby, new Date().getTime());
        return s_at(ruby, rubyClass, args);
    }

    public static RubyTime new_at(Ruby ruby, RubyObject type, RubyObject args[]) {
        int len = type.argCount(args, 1, 2);

        RubyTime time = new RubyTime(ruby, (RubyClass) type);
        time.cal = Calendar.getInstance();

        if (args[0] instanceof RubyTime) {
            time.cal.setTimeZone(((RubyTime) args[0]).cal.getTimeZone());
            time.cal.setTimeInMillis(((RubyTime) args[0]).cal.getTimeInMillis());
        } else {
            long usec = len > 1 ? RubyNumeric.num2long(args[1]) / 1000 : 0;
            time.usec = len > 1 ? RubyNumeric.num2long(args[1]) % 1000 : 0;
            time.cal.setTimeInMillis(RubyNumeric.num2long(args[0]) * 1000 + usec);
        }

        time.callInit(args);

        return time;
    }

    public static RubyTime new_local(Ruby ruby, RubyObject type, RubyObject args[]) {
        return createTime(ruby, type, args, false);
    }

    public static RubyTime new_utc(Ruby ruby, RubyObject type, RubyObject args[]) {
        return createTime(ruby, type, args, true);
    }

    private static final String[] months = {"jan", "feb", "mar", "apr", "may", "jun",
                                            "jul", "aug", "sep", "oct", "nov", "dec"};
    private static final long[] time_min = {1, 0, 0, 0, 0};
    private static final long[] time_max = {31, 23, 59, 60, Long.MAX_VALUE};

    private static RubyTime createTime(Ruby ruby, RubyObject type, RubyObject args[], boolean gmt) {
        int len = 6;
        if (args.length == 10) {
            args = new RubyObject[] {
                args[5],
                args[4],
                args[3],
                args[2],
                args[1],
                args[0],
            };
        } else {
            len = type.argCount(args, 1, 7);
        }

        int year = RubyNumeric.fix2int(args[0]);
        int month = 0;
        
        if (len > 1) {
            if (!args[1].isNil()) {
                if (args[1] instanceof RubyString) {
                    month = -1;
                    for (int i = 0; i < 12; i++) {
                        if (months[i].equalsIgnoreCase(args[1].toString())) {
                            month = i;
                        }
                    }
                    if (month == -1) {
                        try {
                            month = Integer.parseInt(args[1].toString()) - 1;
                        } catch (NumberFormatException nfExcptn) {
                            throw new ArgumentError(ruby, "Argument out of range.");
                        }
                    }
                } else {
                    month = RubyNumeric.fix2int(args[1]) - 1;
                }
            }
            if (0 > month || month > 11) {
                throw new ArgumentError(ruby, "Argument out of range.");
            }
        }

        int[] int_args = { 1, 0, 0, 0, 0 };

        for (int i = 0; len > (i + 2); i++) {
            if (!args[i + 2].isNil()) {
                int_args[i] = RubyNumeric.fix2int(args[i + 2]);
                if (time_min[i] > int_args[i] || int_args[i] > time_max[i]) {
                    throw new ArgumentError(ruby, "Argument out of range.");
                }
            }
        }

        RubyTime time = new RubyTime(ruby, (RubyClass) type);
        if (gmt) {
            time.cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        } else {
            time.cal = Calendar.getInstance();
        }
        time.cal.set(year, month, int_args[0], int_args[1], int_args[2], int_args[3]);
        time.cal.set(Calendar.MILLISECOND, int_args[4] / 1000);
        time.usec = int_args[4] % 1000;

        time.callInit(args);

        return time;
    }

    public static RubyTime s_at(Ruby ruby, RubyObject rubyClass, RubyObject[] args) {
        long secs = RubyNumeric.num2long(args[0]);
        RubyTime time = new RubyTime(ruby, (RubyClass) rubyClass);
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(new Date(secs));
        time.setJavaCalendar(cal);
        return time;
    }

    public RubyTime gmtime() {
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        return this;
    }

    public RubyTime localtime() {
        cal.setTimeZone(TimeZone.getDefault());
        return this;
    }

    public RubyBoolean gmt() {
        return RubyBoolean.newBoolean(ruby, cal.getTimeZone().getID().equals("GMT"));
    }

    public RubyString strftime(RubyObject format) {
        rubyDateFormat.setCalendar(cal);
        rubyDateFormat.applyPattern(format.toString());
        String result = rubyDateFormat.format(cal.getTime());

        return RubyString.newString(ruby, result);
    }

    public RubyObject op_plus(RubyObject other) {
        long time = cal.getTimeInMillis();

        if (other instanceof RubyTime) {
            throw new TypeError(ruby, "time + time ?");
        } else {
            time += ((RubyNumeric) other).getDoubleValue() * 1000;

            RubyTime newTime = new RubyTime(ruby, getRubyClass());
            newTime.cal = Calendar.getInstance();
            newTime.cal.setTime(new Date(time));

            return newTime;
        }
    }

    public RubyObject op_minus(RubyObject other) {
        long time = cal.getTimeInMillis();

        if (other instanceof RubyTime) {
            time -= ((RubyTime) other).cal.getTimeInMillis();

            return RubyFloat.newFloat(ruby, time * 10e-4);
        } else {
            time -= ((RubyNumeric) other).getDoubleValue() * 1000;

            RubyTime newTime = new RubyTime(ruby, getRubyClass());
            newTime.cal = Calendar.getInstance();
            newTime.cal.setTime(new Date(time));

            return newTime;
        }
    }

    public RubyFixnum op_cmp(RubyObject other) {
        long millis = cal.getTimeInMillis();

        if (other instanceof RubyFloat || other instanceof RubyBignum) {
            double time = ((double) millis) / 1000.0;

            double time_other = ((RubyNumeric) other).getDoubleValue();

            if (time > time_other) {
                return RubyFixnum.one(ruby);
            } else if (time < time_other) {
                return RubyFixnum.minus_one(ruby);
            } else {
                return RubyFixnum.zero(ruby);
            }
        } else {
            long millis_other = (other instanceof RubyTime) ? ((RubyTime) other).cal.getTimeInMillis() : RubyNumeric.num2long(other) * 1000;

            if (millis > millis_other) {
                return RubyFixnum.one(ruby);
            } else if (millis < millis_other) {
                return RubyFixnum.minus_one(ruby);
            } else {
                return RubyFixnum.zero(ruby);
            }
        }
    }

    public RubyString asctime() {
        simpleDateFormat.setCalendar(cal);
        simpleDateFormat.applyPattern("EEE MMM dd HH:mm:ss yyyy");
        String result = simpleDateFormat.format(cal.getTime());

        return RubyString.newString(ruby, result);
    }

    public RubyString to_s() {
        simpleDateFormat.setCalendar(cal);
        simpleDateFormat.applyPattern("EEE MMM dd HH:mm:ss z yyyy");
        String result = simpleDateFormat.format(cal.getTime());

        return RubyString.newString(ruby, result);
    }

    public RubyArray to_a() {
        return RubyArray.newArray(ruby, new RubyObject[] {
            sec(),
            min(),
            hour(),
            mday(),
            month(),
            year(),
            wday(),
            yday(),
            isdst(),
            zone()
        });
    }

    public RubyFloat to_f() {
        return RubyFloat.newFloat(ruby, cal.getTimeInMillis() / 1000.0);
    }

    public RubyInteger to_i() {
        return RubyFixnum.newFixnum(ruby, cal.getTimeInMillis() / 1000);
    }

    public RubyInteger usec() {
        return RubyFixnum.newFixnum(ruby, cal.getTimeInMillis() % 1000 * 1000 + usec);
    }

    public RubyInteger sec() {
        return RubyFixnum.newFixnum(ruby, cal.get(Calendar.SECOND));
    }

    public RubyInteger min() {
        return RubyFixnum.newFixnum(ruby, cal.get(Calendar.MINUTE));
    }

    public RubyInteger hour() {
        return RubyFixnum.newFixnum(ruby, cal.get(Calendar.HOUR_OF_DAY));
    }

    public RubyInteger mday() {
        return RubyFixnum.newFixnum(ruby, cal.get(Calendar.DAY_OF_MONTH));
    }

    public RubyInteger month() {
        return RubyFixnum.newFixnum(ruby, cal.get(Calendar.MONTH) + 1);
    }

    public RubyInteger year() {
        return RubyFixnum.newFixnum(ruby, cal.get(Calendar.YEAR));
    }

    public RubyInteger wday() {
        return RubyFixnum.newFixnum(ruby, cal.get(Calendar.DAY_OF_WEEK) - 1);
    }

    public RubyInteger yday() {
        return RubyFixnum.newFixnum(ruby, cal.get(Calendar.DAY_OF_YEAR));
    }
    
    public RubyBoolean isdst() {
        return RubyBoolean.newBoolean(ruby, cal.getTimeZone().inDaylightTime(cal.getTime()));
    }

    public RubyString zone() {
        return RubyString.newString(ruby, cal.getTimeZone().getID());
    }

    public void setJavaCalendar(Calendar cal) {
        this.cal = cal;
    }

    public Date getJavaDate() {
        return this.cal.getTime();
    }
    
    public RubyFixnum hash() {
        return RubyFixnum.newFixnum(ruby, hashCode());
    }

    /**
     * @see Object#hashCode()
     */
    public int hashCode() {
        return (int)(cal.get(Calendar.SECOND) ^ usec);
    }
}