/*
 * RubyTime.java - No description
 * Created on 1. Dec 2001, 15:53
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Chad Fowler
 * Copyright (C) 2004 Charles O Nutter
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Chad Fowler <chadfowler@chadfowler.com>
 * Charles O Nutter <headius@headius.com>
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
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.RubyDateFormat;

/** The Time class.
 * 
 * @author chadfowler, jpetersen
 */
public class RubyTime extends RubyObject {
    public static final String UTC = "UTC";
	private Calendar cal;
    private long usec;

    private static RubyDateFormat rubyDateFormat = new RubyDateFormat("-", Locale.US);
    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("-", Locale.US);

    private RubyTime(Ruby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass);
    }

    public static RubyClass createTimeClass(Ruby runtime) {
		RubyClass rubyTimeClass = runtime.defineClass("Time", runtime.getClasses().getObjectClass());
    	
		CallbackFactory callbackFactory = runtime.callbackFactory(RubyTime.class);
		
		rubyTimeClass.includeModule(runtime.getClasses().getComparableModule());
        
		rubyTimeClass.defineSingletonMethod("new", 
			callbackFactory.getSingletonMethod("s_new"));
		rubyTimeClass.defineSingletonMethod("now", 
			callbackFactory.getSingletonMethod("s_new"));
		rubyTimeClass.defineSingletonMethod("at", 
			callbackFactory.getOptSingletonMethod("new_at"));
		rubyTimeClass.defineSingletonMethod("local", 
			callbackFactory.getOptSingletonMethod("new_local"));
		rubyTimeClass.defineSingletonMethod("mktime", 
			callbackFactory.getOptSingletonMethod("new_local"));
		rubyTimeClass.defineSingletonMethod("utc", 
			callbackFactory.getOptSingletonMethod("new_utc"));
		rubyTimeClass.defineSingletonMethod("gm", 
			callbackFactory.getOptSingletonMethod("new_utc"));
				
		rubyTimeClass.defineMethod("<=>", 
			callbackFactory.getMethod("op_cmp", IRubyObject.class));
		rubyTimeClass.defineMethod("-", 
			callbackFactory.getMethod("op_minus", IRubyObject.class));
		rubyTimeClass.defineMethod("+", 
			callbackFactory.getMethod("op_plus", IRubyObject.class));
		rubyTimeClass.defineMethod("sec", 
			callbackFactory.getMethod("sec"));
		rubyTimeClass.defineMethod("min", 
			callbackFactory.getMethod("min"));
		rubyTimeClass.defineMethod("hour", 
			callbackFactory.getMethod("hour"));
		rubyTimeClass.defineMethod("mday", 
			callbackFactory.getMethod("mday"));
		rubyTimeClass.defineMethod("day", 
			callbackFactory.getMethod("mday"));
		rubyTimeClass.defineMethod("month", 
			callbackFactory.getMethod("month"));
		rubyTimeClass.defineMethod("mon", 
			callbackFactory.getMethod("month"));
		rubyTimeClass.defineMethod("year", 
			callbackFactory.getMethod("year"));
		rubyTimeClass.defineMethod("wday", 
			callbackFactory.getMethod("wday"));
		rubyTimeClass.defineMethod("yday", 
			callbackFactory.getMethod("yday"));
		rubyTimeClass.defineMethod("isdst", 
			callbackFactory.getMethod("isdst"));
		rubyTimeClass.defineMethod("zone", 
			callbackFactory.getMethod("zone"));
		rubyTimeClass.defineMethod("to_a", 
			callbackFactory.getMethod("to_a"));
		rubyTimeClass.defineMethod("to_f", 
			callbackFactory.getMethod("to_f"));
		rubyTimeClass.defineMethod("asctime", 
			callbackFactory.getMethod("asctime"));
		rubyTimeClass.defineMethod("ctime", 
			callbackFactory.getMethod("asctime"));
		rubyTimeClass.defineMethod("to_s", 
			callbackFactory.getMethod("to_s"));
		rubyTimeClass.defineMethod("inspect", 
			callbackFactory.getMethod("inspect"));
		rubyTimeClass.defineMethod("strftime", 
			callbackFactory.getMethod("strftime", IRubyObject.class));
		rubyTimeClass.defineMethod("usec", 
			callbackFactory.getMethod("usec"));
		rubyTimeClass.defineMethod("tv_usec", 
			callbackFactory.getMethod("usec"));
		rubyTimeClass.defineMethod("to_i", 
			callbackFactory.getMethod("to_i"));
		rubyTimeClass.defineMethod("tv_sec", 
			callbackFactory.getMethod("to_i"));
		rubyTimeClass.defineMethod("gmtime", 
			callbackFactory.getMethod("gmtime"));
		rubyTimeClass.defineMethod("utc", 
			callbackFactory.getMethod("gmtime"));
		rubyTimeClass.defineMethod("gmt?", 
			callbackFactory.getMethod("gmt"));
		rubyTimeClass.defineMethod("gmtime?", 
			callbackFactory.getMethod("gmt"));
		rubyTimeClass.defineMethod("utc?", 
			callbackFactory.getMethod("gmt"));
		rubyTimeClass.defineMethod("localtime", 
			callbackFactory.getMethod("localtime"));
		rubyTimeClass.defineMethod("hash", 
			callbackFactory.getMethod("hash"));

		return rubyTimeClass;
    }

    protected long getTimeInMillis() {
        return cal.getTime().getTime();  // For JDK 1.4 we can use "cal.getTimeInMillis()"
    }


    public static RubyTime s_new(IRubyObject receiver) {
        IRubyObject[] args = new IRubyObject[1];
        args[0] = receiver.getRuntime().newFixnum(new Date().getTime());
        return s_at(receiver, args);
    }

    public static RubyTime new_at(IRubyObject receiver, IRubyObject[] args) {
        int len = receiver.checkArgumentCount(args, 1, 2);

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

    public static RubyTime new_local(IRubyObject type, IRubyObject[] args) {
        return createTime(type, args, false);
    }

    public static RubyTime new_utc(IRubyObject type, IRubyObject[] args) {
        return createTime(type, args, true);
    }

    private static final String[] months = {"jan", "feb", "mar", "apr", "may", "jun",
                                            "jul", "aug", "sep", "oct", "nov", "dec"};
    private static final long[] time_min = {1, 0, 0, 0, 0};
    private static final long[] time_max = {31, 23, 59, 60, Long.MAX_VALUE};

    private static RubyTime createTime(IRubyObject type, IRubyObject[] args, boolean gmt) {
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
            len = type.checkArgumentCount(args, 1, 7);
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
                            throw type.getRuntime().newArgumentError("Argument out of range.");
                        }
                    }
                } else {
                    month = RubyNumeric.fix2int(args[1]) - 1;
                }
            }
            if (0 > month || month > 11) {
                throw type.getRuntime().newArgumentError("Argument out of range.");
            }
        }

        int[] int_args = { 1, 0, 0, 0, 0 };

        for (int i = 0; len > i + 2; i++) {
            if (!args[i + 2].isNil()) {
                int_args[i] = RubyNumeric.fix2int(args[i + 2]);
                if (time_min[i] > int_args[i] || int_args[i] > time_max[i]) {
                    throw type.getRuntime().newArgumentError("Argument out of range.");
                }
            }
        }

        RubyTime time = new RubyTime(type.getRuntime(), (RubyClass) type);
        if (gmt) {
            time.cal = Calendar.getInstance(TimeZone.getTimeZone(UTC));
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
        cal.setTimeZone(TimeZone.getTimeZone(UTC));
        return this;
    }

    public RubyTime localtime() {
        cal.setTimeZone(TimeZone.getDefault());
        return this;
    }

    public RubyBoolean gmt() {
        return getRuntime().newBoolean(cal.getTimeZone().getID().equals(UTC));
    }

    public RubyString strftime(IRubyObject format) {
        rubyDateFormat.setCalendar(cal);
        rubyDateFormat.applyPattern(format.toString());
        String result = rubyDateFormat.format(cal.getTime());

        return getRuntime().newString(result);
    }

    public IRubyObject op_plus(IRubyObject other) {
        long time = getTimeInMillis();

        if (other instanceof RubyTime) {
            throw getRuntime().newTypeError("time + time ?");
        }
		time += ((RubyNumeric) other).getDoubleValue() * 1000;

		RubyTime newTime = new RubyTime(getRuntime(), getMetaClass());
		newTime.cal = Calendar.getInstance();
		newTime.cal.setTime(new Date(time));

		return newTime;
    }

    public IRubyObject op_minus(IRubyObject other) {
        long time = getTimeInMillis();

        if (other instanceof RubyTime) {
            time -= ((RubyTime) other).getTimeInMillis();

            return RubyFloat.newFloat(getRuntime(), time * 10e-4);
        }
		time -= ((RubyNumeric) other).getDoubleValue() * 1000;

		RubyTime newTime = new RubyTime(getRuntime(), getMetaClass());
		newTime.cal = Calendar.getInstance();
		newTime.cal.setTime(new Date(time));

		return newTime;
    }

    public RubyFixnum op_cmp(IRubyObject other) {
        long millis = getTimeInMillis();

        if (other instanceof RubyFloat || other instanceof RubyBignum) {
            double time = millis / 1000.0;

            double time_other = ((RubyNumeric) other).getDoubleValue();

            if (time > time_other) {
                return RubyFixnum.one(getRuntime());
            } else if (time < time_other) {
                return RubyFixnum.minus_one(getRuntime());
            } else {
                return RubyFixnum.zero(getRuntime());
            }
        }
		long millis_other = (other instanceof RubyTime) ? ((RubyTime) other).getTimeInMillis() : RubyNumeric.num2long(other) * 1000;

		if (millis > millis_other) {
		    return RubyFixnum.one(getRuntime());
		} else if (millis < millis_other) {
		    return RubyFixnum.minus_one(getRuntime());
		} else {
		    return RubyFixnum.zero(getRuntime());
		}
    }

    public RubyString asctime() {
        simpleDateFormat.setCalendar(cal);
        simpleDateFormat.applyPattern("EEE MMM dd HH:mm:ss yyyy");
        String result = simpleDateFormat.format(cal.getTime());

        return getRuntime().newString(result);
    }

    public RubyString to_s() {
        simpleDateFormat.setCalendar(cal);
        simpleDateFormat.applyPattern("EEE MMM dd HH:mm:ss z yyyy");
        String result = simpleDateFormat.format(cal.getTime());

        return getRuntime().newString(result);
    }

    public RubyArray to_a() {
        return getRuntime().newArray(new IRubyObject[] {
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
        return RubyFloat.newFloat(getRuntime(), getTimeInMillis() / 1000 + microseconds() / 1000000.0);
    }

    public RubyInteger to_i() {
        return getRuntime().newFixnum(getTimeInMillis() / 1000);
    }

    public RubyInteger usec() {
        return getRuntime().newFixnum(microseconds());
    }
    
    public long microseconds() {
    	return getTimeInMillis() % 1000 * 1000 + usec;
    }

    public RubyInteger sec() {
        return getRuntime().newFixnum(cal.get(Calendar.SECOND));
    }

    public RubyInteger min() {
        return getRuntime().newFixnum(cal.get(Calendar.MINUTE));
    }

    public RubyInteger hour() {
        return getRuntime().newFixnum(cal.get(Calendar.HOUR_OF_DAY));
    }

    public RubyInteger mday() {
        return getRuntime().newFixnum(cal.get(Calendar.DAY_OF_MONTH));
    }

    public RubyInteger month() {
        return getRuntime().newFixnum(cal.get(Calendar.MONTH) + 1);
    }

    public RubyInteger year() {
        return getRuntime().newFixnum(cal.get(Calendar.YEAR));
    }

    public RubyInteger wday() {
        return getRuntime().newFixnum(cal.get(Calendar.DAY_OF_WEEK) - 1);
    }

    public RubyInteger yday() {
        return getRuntime().newFixnum(cal.get(Calendar.DAY_OF_YEAR));
    }
    
    public RubyBoolean isdst() {
        return getRuntime().newBoolean(cal.getTimeZone().inDaylightTime(cal.getTime()));
    }

    public RubyString zone() {
        return getRuntime().newString(cal.getTimeZone().getID());
    }

    public void setJavaCalendar(Calendar cal) {
        this.cal = cal;
    }

    public Date getJavaDate() {
        return this.cal.getTime();
    }

    public RubyFixnum hash() {
    	// modified to match how hash is calculated in 1.8.2
        return getRuntime().newFixnum((int)(((cal.getTimeInMillis() / 1000) ^ microseconds()) << 1) >> 1);
    }    
}
