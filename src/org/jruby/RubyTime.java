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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Date;
import java.util.TimeZone;
import java.util.GregorianCalendar;

import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.RubyDateFormat;
import org.jruby.exceptions.ArgumentError;
import org.jruby.exceptions.TypeError;
import org.jruby.runtime.CallbackFactory;

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
		RubyClass rubyTimeClass = ruby.defineClass("Time", ruby.getClasses().getObjectClass());
    	
		CallbackFactory callbackFactory = ruby.callbackFactory();
        
		rubyTimeClass.defineSingletonMethod("new", 
			callbackFactory.getSingletonMethod(RubyTime.class, "s_new"));
		rubyTimeClass.defineSingletonMethod("now", 
			callbackFactory.getSingletonMethod(RubyTime.class, "s_new"));
		rubyTimeClass.defineSingletonMethod("at", 
			callbackFactory.getOptSingletonMethod(RubyTime.class, "new_at"));
		rubyTimeClass.defineSingletonMethod("local", 
			callbackFactory.getOptSingletonMethod(RubyTime.class, "new_local"));
		rubyTimeClass.defineSingletonMethod("mktime", 
			callbackFactory.getOptSingletonMethod(RubyTime.class, "new_local"));
		rubyTimeClass.defineSingletonMethod("utc", 
			callbackFactory.getOptSingletonMethod(RubyTime.class, "new_utc"));
		rubyTimeClass.defineSingletonMethod("gm", 
			callbackFactory.getOptSingletonMethod(RubyTime.class, "new_utc"));
				
		rubyTimeClass.defineMethod("<=>", 
			callbackFactory.getMethod(RubyTime.class, "op_cmp", IRubyObject.class));
		rubyTimeClass.defineMethod("-", 
			callbackFactory.getMethod(RubyTime.class, "op_minus", IRubyObject.class));
		rubyTimeClass.defineMethod("+", 
			callbackFactory.getMethod(RubyTime.class, "op_plus", IRubyObject.class));
		rubyTimeClass.defineMethod("sec", 
			callbackFactory.getMethod(RubyTime.class, "sec"));
		rubyTimeClass.defineMethod("min", 
			callbackFactory.getMethod(RubyTime.class, "min"));
		rubyTimeClass.defineMethod("hour", 
			callbackFactory.getMethod(RubyTime.class, "hour"));
		rubyTimeClass.defineMethod("mday", 
			callbackFactory.getMethod(RubyTime.class, "mday"));
		rubyTimeClass.defineMethod("day", 
			callbackFactory.getMethod(RubyTime.class, "mday"));
		rubyTimeClass.defineMethod("month", 
			callbackFactory.getMethod(RubyTime.class, "month"));
		rubyTimeClass.defineMethod("mon", 
			callbackFactory.getMethod(RubyTime.class, "month"));
		rubyTimeClass.defineMethod("year", 
			callbackFactory.getMethod(RubyTime.class, "year"));
		rubyTimeClass.defineMethod("wday", 
			callbackFactory.getMethod(RubyTime.class, "wday"));
		rubyTimeClass.defineMethod("yday", 
			callbackFactory.getMethod(RubyTime.class, "yday"));
		rubyTimeClass.defineMethod("isdst", 
			callbackFactory.getMethod(RubyTime.class, "isdst"));
		rubyTimeClass.defineMethod("zone", 
			callbackFactory.getMethod(RubyTime.class, "zone"));
		rubyTimeClass.defineMethod("to_a", 
			callbackFactory.getMethod(RubyTime.class, "to_a"));
		rubyTimeClass.defineMethod("to_f", 
			callbackFactory.getMethod(RubyTime.class, "to_f"));
		rubyTimeClass.defineMethod("asctime", 
			callbackFactory.getMethod(RubyTime.class, "asctime"));
		rubyTimeClass.defineMethod("ctime", 
			callbackFactory.getMethod(RubyTime.class, "asctime"));
		rubyTimeClass.defineMethod("to_s", 
			callbackFactory.getMethod(RubyTime.class, "to_s"));
		rubyTimeClass.defineMethod("inspect", 
			callbackFactory.getMethod(RubyTime.class, "inspect"));
		rubyTimeClass.defineMethod("strftime", 
			callbackFactory.getMethod(RubyTime.class, "strftime", IRubyObject.class));
		rubyTimeClass.defineMethod("usec", 
			callbackFactory.getMethod(RubyTime.class, "usec"));
		rubyTimeClass.defineMethod("tv_usec", 
			callbackFactory.getMethod(RubyTime.class, "usec"));
		rubyTimeClass.defineMethod("to_i", 
			callbackFactory.getMethod(RubyTime.class, "to_i"));
		rubyTimeClass.defineMethod("tv_sec", 
			callbackFactory.getMethod(RubyTime.class, "to_i"));
		rubyTimeClass.defineMethod("gmtime", 
			callbackFactory.getMethod(RubyTime.class, "gmtime"));
		rubyTimeClass.defineMethod("utc", 
			callbackFactory.getMethod(RubyTime.class, "gmtime"));
		rubyTimeClass.defineMethod("gmt?", 
			callbackFactory.getMethod(RubyTime.class, "gmt"));
		rubyTimeClass.defineMethod("gmtime?", 
			callbackFactory.getMethod(RubyTime.class, "gmt"));
		rubyTimeClass.defineMethod("utc?", 
			callbackFactory.getMethod(RubyTime.class, "gmt"));
		rubyTimeClass.defineMethod("localtime", 
			callbackFactory.getMethod(RubyTime.class, "localtime"));
		rubyTimeClass.defineMethod("hash", 
			callbackFactory.getMethod(RubyTime.class, "hash"));		

		return rubyTimeClass;
    }

    protected long getTimeInMillis() {
        return cal.getTime().getTime();  // For JDK 1.4 we can use "cal.getTimeInMillis()"
    }


    public static RubyTime s_new(IRubyObject receiver) {
        IRubyObject[] args = new IRubyObject[1];
        args[0] = RubyFixnum.newFixnum(receiver.getRuntime(), new Date().getTime());
        return s_at(receiver, args);
    }

    public static RubyTime new_at(IRubyObject receiver, IRubyObject args[]) {
        int len = receiver.argCount(args, 1, 2);

        RubyTime time = new RubyTime(receiver.getRuntime(), (RubyClass) receiver);
        time.cal = Calendar.getInstance();

        if (args[0] instanceof RubyTime) {
            time.cal.setTimeZone(((RubyTime) args[0]).cal.getTimeZone());
            time.cal.setTimeInMillis(((RubyTime) args[0]).getTimeInMillis());
        } else {
            long usec = len > 1 ? RubyNumeric.num2long(args[1]) / 1000 : 0;
            time.usec = len > 1 ? RubyNumeric.num2long(args[1]) % 1000 : 0;
            time.cal.setTimeInMillis(RubyNumeric.num2long(args[0]) * 1000 + usec);
        }

        time.callInit(args);

        return time;
    }

    public static RubyTime new_local(IRubyObject type, IRubyObject args[]) {
        return createTime(type, args, false);
    }

    public static RubyTime new_utc(IRubyObject type, IRubyObject args[]) {
        return createTime(type, args, true);
    }

    private static final String[] months = {"jan", "feb", "mar", "apr", "may", "jun",
                                            "jul", "aug", "sep", "oct", "nov", "dec"};
    private static final long[] time_min = {1, 0, 0, 0, 0};
    private static final long[] time_max = {31, 23, 59, 60, Long.MAX_VALUE};

    private static RubyTime createTime(IRubyObject type, IRubyObject args[], boolean gmt) {
        int len = 6;
        if (args.length == 10) {
            args = new IRubyObject[] {
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
                            throw new ArgumentError(type.getRuntime(), "Argument out of range.");
                        }
                    }
                } else {
                    month = RubyNumeric.fix2int(args[1]) - 1;
                }
            }
            if (0 > month || month > 11) {
                throw new ArgumentError(type.getRuntime(), "Argument out of range.");
            }
        }

        int[] int_args = { 1, 0, 0, 0, 0 };

        for (int i = 0; len > (i + 2); i++) {
            if (!args[i + 2].isNil()) {
                int_args[i] = RubyNumeric.fix2int(args[i + 2]);
                if (time_min[i] > int_args[i] || int_args[i] > time_max[i]) {
                    throw new ArgumentError(type.getRuntime(), "Argument out of range.");
                }
            }
        }

        RubyTime time = new RubyTime(type.getRuntime(), (RubyClass) type);
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

    public static RubyTime s_at(IRubyObject rubyClass, IRubyObject[] args) {
        long secs = RubyNumeric.num2long(args[0]);
        RubyTime time = new RubyTime(rubyClass.getRuntime(), (RubyClass) rubyClass);
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
        return RubyBoolean.newBoolean(runtime, cal.getTimeZone().getID().equals("GMT"));
    }

    public RubyString strftime(IRubyObject format) {
        rubyDateFormat.setCalendar(cal);
        rubyDateFormat.applyPattern(format.toString());
        String result = rubyDateFormat.format(cal.getTime());

        return RubyString.newString(runtime, result);
    }

    public IRubyObject op_plus(IRubyObject other) {
        long time = getTimeInMillis();

        if (other instanceof RubyTime) {
            throw new TypeError(runtime, "time + time ?");
        } else {
            time += ((RubyNumeric) other).getDoubleValue() * 1000;

            RubyTime newTime = new RubyTime(runtime, getMetaClass());
            newTime.cal = Calendar.getInstance();
            newTime.cal.setTime(new Date(time));

            return newTime;
        }
    }

    public IRubyObject op_minus(IRubyObject other) {
        long time = getTimeInMillis();

        if (other instanceof RubyTime) {
            time -= ((RubyTime) other).getTimeInMillis();

            return RubyFloat.newFloat(runtime, time * 10e-4);
        } else {
            time -= ((RubyNumeric) other).getDoubleValue() * 1000;

            RubyTime newTime = new RubyTime(runtime, getMetaClass());
            newTime.cal = Calendar.getInstance();
            newTime.cal.setTime(new Date(time));

            return newTime;
        }
    }

    public RubyFixnum op_cmp(IRubyObject other) {
        long millis = getTimeInMillis();

        if (other instanceof RubyFloat || other instanceof RubyBignum) {
            double time = ((double) millis) / 1000.0;

            double time_other = ((RubyNumeric) other).getDoubleValue();

            if (time > time_other) {
                return RubyFixnum.one(runtime);
            } else if (time < time_other) {
                return RubyFixnum.minus_one(runtime);
            } else {
                return RubyFixnum.zero(runtime);
            }
        } else {
            long millis_other = (other instanceof RubyTime) ? ((RubyTime) other).getTimeInMillis() : RubyNumeric.num2long(other) * 1000;

            if (millis > millis_other) {
                return RubyFixnum.one(runtime);
            } else if (millis < millis_other) {
                return RubyFixnum.minus_one(runtime);
            } else {
                return RubyFixnum.zero(runtime);
            }
        }
    }

    public RubyString asctime() {
        simpleDateFormat.setCalendar(cal);
        simpleDateFormat.applyPattern("EEE MMM dd HH:mm:ss yyyy");
        String result = simpleDateFormat.format(cal.getTime());

        return RubyString.newString(runtime, result);
    }

    public RubyString to_s() {
        simpleDateFormat.setCalendar(cal);
        simpleDateFormat.applyPattern("EEE MMM dd HH:mm:ss z yyyy");
        String result = simpleDateFormat.format(cal.getTime());

        return RubyString.newString(runtime, result);
    }

    public RubyArray to_a() {
        return RubyArray.newArray(runtime, new IRubyObject[] {
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
        return RubyFloat.newFloat(runtime, getTimeInMillis() / 1000.0);
    }

    public RubyInteger to_i() {
        return RubyFixnum.newFixnum(runtime, getTimeInMillis() / 1000);
    }

    public RubyInteger usec() {
        return RubyFixnum.newFixnum(runtime, getTimeInMillis() % 1000 * 1000 + usec);
    }

    public RubyInteger sec() {
        return RubyFixnum.newFixnum(runtime, cal.get(Calendar.SECOND));
    }

    public RubyInteger min() {
        return RubyFixnum.newFixnum(runtime, cal.get(Calendar.MINUTE));
    }

    public RubyInteger hour() {
        return RubyFixnum.newFixnum(runtime, cal.get(Calendar.HOUR_OF_DAY));
    }

    public RubyInteger mday() {
        return RubyFixnum.newFixnum(runtime, cal.get(Calendar.DAY_OF_MONTH));
    }

    public RubyInteger month() {
        return RubyFixnum.newFixnum(runtime, cal.get(Calendar.MONTH) + 1);
    }

    public RubyInteger year() {
        return RubyFixnum.newFixnum(runtime, cal.get(Calendar.YEAR));
    }

    public RubyInteger wday() {
        return RubyFixnum.newFixnum(runtime, cal.get(Calendar.DAY_OF_WEEK) - 1);
    }

    public RubyInteger yday() {
        return RubyFixnum.newFixnum(runtime, cal.get(Calendar.DAY_OF_YEAR));
    }
    
    public RubyBoolean isdst() {
        return RubyBoolean.newBoolean(runtime, cal.getTimeZone().inDaylightTime(cal.getTime()));
    }

    public RubyString zone() {
        return RubyString.newString(runtime, cal.getTimeZone().getID());
    }

    public void setJavaCalendar(Calendar cal) {
        this.cal = cal;
    }

    public Date getJavaDate() {
        return this.cal.getTime();
    }

    public RubyFixnum hash() {
        return RubyFixnum.newFixnum(runtime, (int)(cal.get(Calendar.SECOND) ^ usec));
    }    
}
