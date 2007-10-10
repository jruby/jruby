/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001 Chad Fowler <chadfowler@chadfowler.com>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Joey Gibson <joey@joeygibson.com>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2006 Ola Bini <ola.bini@ki.se>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import org.jruby.anno.JRubyMethod;

import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.RubyDateFormat;
import org.jruby.util.ByteList;

/** The Time class.
 * 
 * @author chadfowler, jpetersen
 */
public class RubyTime extends RubyObject {
    public static final String UTC = "UTC";
	private Calendar cal;
    private long usec;

    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("-", Locale.US);

    public static TimeZone getLocalTimeZone(Ruby runtime) {
        // TODO: cache the RubyString "TZ" so it doesn't need to be recreated for each call?
        RubyString tzVar = runtime.newString("TZ");
        RubyHash h = ((RubyHash)runtime.getObject().getConstant("ENV"));
        IRubyObject tz = h.op_aref(tzVar);
        if (tz == null || ! (tz instanceof RubyString)) {
            return TimeZone.getDefault();
        } else {
            return TimeZone.getTimeZone(tz.toString());
        }
    }
    
    public RubyTime(Ruby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass);
    }
    
    public RubyTime(Ruby runtime, RubyClass rubyClass, Calendar cal) {
        super(runtime, rubyClass);
        this.cal = cal;
    }
    
    private static ObjectAllocator TIME_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            RubyTime instance = new RubyTime(runtime, klass);
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(new Date());
            instance.setJavaCalendar(cal);
            return instance;
        }
    };
    
    public static RubyClass createTimeClass(Ruby runtime) {
        RubyClass timeClass = runtime.defineClass("Time", runtime.getObject(), TIME_ALLOCATOR);
        runtime.setTime(timeClass);
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyTime.class);
        
        timeClass.includeModule(runtime.getComparable());
        
        timeClass.defineAnnotatedMethods(RubyTime.class, callbackFactory);
        
        return timeClass;
    }
    
    public void setUSec(long usec) {
        this.usec = usec;
    }
    
    public long getUSec() {
        return usec;
    }
    
    public void updateCal(Calendar calendar) {
        calendar.setTimeZone(cal.getTimeZone());
        calendar.setTimeInMillis(getTimeInMillis());
    }
    
    protected long getTimeInMillis() {
        return cal.getTimeInMillis();  // For JDK 1.4 we can use "cal.getTimeInMillis()"
    }
    
    public static RubyTime newTime(Ruby runtime, long milliseconds) {
        Calendar cal = Calendar.getInstance(); 
        RubyTime time = new RubyTime(runtime, runtime.getTime(), cal);
        
        cal.setTimeInMillis(milliseconds);
        
        return time;
    }
    
    public static RubyTime newTime(Ruby runtime, Calendar cal) {
        RubyTime time = new RubyTime(runtime, runtime.getTime(), cal);
        
        return time;
    }

    @JRubyMethod(name = "initialize_copy", required = 1)
    public IRubyObject initialize_copy(IRubyObject original) {
        if (!(original instanceof RubyTime)) {
            throw getRuntime().newTypeError("Expecting an instance of class Time");
        }
        
        RubyTime originalTime = (RubyTime) original;
        
        cal = (Calendar)(originalTime.cal.clone());
        usec = originalTime.usec;
        
        return this;
    }

    @JRubyMethod(name = "succ")
    public RubyTime succ() {
        Calendar newCal = (Calendar)cal.clone();
        newCal.add(Calendar.SECOND,1);
        return newTime(getRuntime(),newCal);
    }

    @JRubyMethod(name = "gmtime", name2 = "utc")
    public RubyTime gmtime() {
        cal.setTimeZone(TimeZone.getTimeZone(UTC));
        return this;
    }

    @JRubyMethod(name = "localtime")
    public RubyTime localtime() {
        long dump = cal.getTimeInMillis();
        cal = Calendar.getInstance(getLocalTimeZone(getRuntime()));
        cal.setTimeInMillis(dump);
        return this;
    }
    
    @JRubyMethod(name = "gmt?", name2 = "utc?", name3 = "gmtime?")
    public RubyBoolean gmt() {
        return getRuntime().newBoolean(cal.getTimeZone().getID().equals(UTC));
    }
    
    @JRubyMethod(name = "getgm", name2 = "getutc")
    public RubyTime getgm() {
        Calendar newCal = (Calendar)cal.clone();
        newCal.setTimeZone(TimeZone.getTimeZone(UTC));
        return newTime(getRuntime(), newCal);
    }

    @JRubyMethod(name = "getlocal")
    public RubyTime getlocal() {
        Calendar newCal = (Calendar)cal.clone();
        newCal.setTimeZone(getLocalTimeZone(getRuntime()));
        return newTime(getRuntime(), newCal);
    }

    @JRubyMethod(name = "strftime", required = 1)
    public RubyString strftime(IRubyObject format) {
        final RubyDateFormat rubyDateFormat = new RubyDateFormat("-", Locale.US);
        rubyDateFormat.setCalendar(cal);
        rubyDateFormat.applyPattern(format.toString());
        String result = rubyDateFormat.format(cal.getTime());

        return getRuntime().newString(result);
    }
    
    @JRubyMethod(name = ">=", required = 1)
    public IRubyObject op_ge(IRubyObject other) {
        if (other instanceof RubyTime) {
            return getRuntime().newBoolean(cmp((RubyTime) other) >= 0);
        }
        
        return RubyComparable.op_ge(this, other);
    }
    
    @JRubyMethod(name = ">", required = 1)
    public IRubyObject op_gt(IRubyObject other) {
        if (other instanceof RubyTime) {
            return getRuntime().newBoolean(cmp((RubyTime) other) > 0);
        }
        
        return RubyComparable.op_gt(this, other);
    }
    
    @JRubyMethod(name = "<=", required = 1)
    public IRubyObject op_le(IRubyObject other) {
        if (other instanceof RubyTime) {
            return getRuntime().newBoolean(cmp((RubyTime) other) <= 0);
        }
        
        return RubyComparable.op_le(this, other);
    }
    
    @JRubyMethod(name = "<", required = 1)
    public IRubyObject op_lt(IRubyObject other) {
        if (other instanceof RubyTime) {
            return getRuntime().newBoolean(cmp((RubyTime) other) < 0);
        }
        
        return RubyComparable.op_lt(this, other);
    }
    
    private int cmp(RubyTime other) {
        long millis = getTimeInMillis();
		long millis_other = other.getTimeInMillis();
        long usec_other = other.usec;
        
		if (millis > millis_other || (millis == millis_other && usec > usec_other)) {
		    return 1;
		} else if (millis < millis_other || (millis == millis_other && usec < usec_other)) {
		    return -1;
		} 

        return 0;
    }
    
    @JRubyMethod(name = "+", required = 1)
    public IRubyObject op_plus(IRubyObject other) {
        long time = getTimeInMillis();

        if (other instanceof RubyTime) {
            throw getRuntime().newTypeError("time + time ?");
        }
        long adjustment = (long) (RubyNumeric.num2dbl(other) * 1000000);
        int micro = (int) (adjustment % 1000);
        adjustment = adjustment / 1000;
        
		time += adjustment;

		RubyTime newTime = new RubyTime(getRuntime(), getMetaClass());
		newTime.cal = Calendar.getInstance();
        newTime.cal.setTimeZone(cal.getTimeZone());
		newTime.cal.setTime(new Date(time));
        newTime.setUSec(micro);

		return newTime;
    }

    @JRubyMethod(name = "-", required = 1)
    public IRubyObject op_minus(IRubyObject other) {
        long time = getTimeInMillis();

        if (other instanceof RubyTime) {
            time -= ((RubyTime) other).getTimeInMillis();

            return RubyFloat.newFloat(getRuntime(), time * 10e-4);
        }
        long adjustment = (long) (((RubyNumeric) other).getDoubleValue() * 1000000);
        int micro = (int) (adjustment % 1000);
        adjustment = adjustment / 1000;
        
        time -= adjustment;

		RubyTime newTime = new RubyTime(getRuntime(), getMetaClass());
		newTime.cal = Calendar.getInstance();
        newTime.cal.setTimeZone(cal.getTimeZone());
		newTime.cal.setTime(new Date(time));
        newTime.setUSec(micro);

		return newTime;
    }

    @JRubyMethod(name = "===", required = 1)
    public IRubyObject op_eqq(IRubyObject other) {
        return (RubyNumeric.fix2int(callMethod(getRuntime().getCurrentContext(), MethodIndex.OP_SPACESHIP, "<=>", other)) == 0) ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    @JRubyMethod(name = "<=>", required = 1)
    public IRubyObject op_cmp(IRubyObject other) {
        if (other.isNil()) {
        	return other;
        }
        
        if (other instanceof RubyTime) {
            return getRuntime().newFixnum(cmp((RubyTime) other));
        }
        
        long millis = getTimeInMillis();

        if(other instanceof RubyNumeric) {
            if (other instanceof RubyFloat || other instanceof RubyBignum) {
                double time = millis / 1000.0;

                double time_other = ((RubyNumeric) other).getDoubleValue();

                if (time > time_other) {
                    return RubyFixnum.one(getRuntime());
                } else if (time < time_other) {
                    return RubyFixnum.minus_one(getRuntime());
                }

                return RubyFixnum.zero(getRuntime());
            }
            long millis_other = RubyNumeric.num2long(other) * 1000;

            if (millis > millis_other || (millis == millis_other && usec > 0)) {
                return RubyFixnum.one(getRuntime());
            } else if (millis < millis_other || (millis == millis_other && usec < 0)) {
                return RubyFixnum.minus_one(getRuntime());
            }

            return RubyFixnum.zero(getRuntime());
        }
        return getRuntime().getNil();
    }
    
    @JRubyMethod(name = "eql?", required = 1)
    public IRubyObject eql_p(IRubyObject other) {
        if (other instanceof RubyTime) {
            RubyTime otherTime = (RubyTime)other; 
            return (usec == otherTime.usec && getTimeInMillis() == otherTime.getTimeInMillis()) ? getRuntime().getTrue() : getRuntime().getFalse();
        }
        return getRuntime().getFalse();
    }

    @JRubyMethod(name = "asctime", name2 = "ctime")
    public RubyString asctime() {
        simpleDateFormat.setCalendar(cal);
        if (cal.get(Calendar.DAY_OF_MONTH) < 10) {
            simpleDateFormat.applyPattern("EEE MMM  d HH:mm:ss yyyy");
        } else {
            simpleDateFormat.applyPattern("EEE MMM dd HH:mm:ss yyyy");
        }
        String result = simpleDateFormat.format(cal.getTime());

        return getRuntime().newString(result);
    }

    @JRubyMethod(name = "to_s")
    public IRubyObject to_s() {
        simpleDateFormat.setCalendar(cal);
        simpleDateFormat.applyPattern("EEE MMM dd HH:mm:ss z yyyy");
        String result = simpleDateFormat.format(cal.getTime());

        return getRuntime().newString(result);
    }

    @JRubyMethod(name = "to_a")
    public RubyArray to_a() {
        return getRuntime().newArrayNoCopy(new IRubyObject[] { sec(), min(), hour(), mday(), month(), 
                year(), wday(), yday(), isdst(), zone() });
    }

    @JRubyMethod(name = "to_f")
    public RubyFloat to_f() {
        long time = getTimeInMillis();
        time = time * 1000 + usec;
        return RubyFloat.newFloat(getRuntime(), time / 1000000.0);
    }

    @JRubyMethod(name = "to_i", name2 = "tv_sec")
    public RubyInteger to_i() {
        return getRuntime().newFixnum(getTimeInMillis() / 1000);
    }

    @JRubyMethod(name = "usec", name2 = "tv_usec")
    public RubyInteger usec() {
        return getRuntime().newFixnum(microseconds());
    }
    
    public void setMicroseconds(long mic) {
        long millis = getTimeInMillis() % 1000;
        long withoutMillis = getTimeInMillis() - millis;
        withoutMillis += (mic / 1000);
        cal.setTimeInMillis(withoutMillis);
        usec = mic % 1000;
    }
    
    public long microseconds() {
    	return getTimeInMillis() % 1000 * 1000 + usec;
    }

    @JRubyMethod(name = "sec")
    public RubyInteger sec() {
        return getRuntime().newFixnum(cal.get(Calendar.SECOND));
    }

    @JRubyMethod(name = "min")
    public RubyInteger min() {
        return getRuntime().newFixnum(cal.get(Calendar.MINUTE));
    }

    @JRubyMethod(name = "hour")
    public RubyInteger hour() {
        return getRuntime().newFixnum(cal.get(Calendar.HOUR_OF_DAY));
    }

    @JRubyMethod(name = "mday", name2 = "day")
    public RubyInteger mday() {
        return getRuntime().newFixnum(cal.get(Calendar.DAY_OF_MONTH));
    }

    @JRubyMethod(name = "month", name2 = "mon")
    public RubyInteger month() {
        return getRuntime().newFixnum(cal.get(Calendar.MONTH) + 1);
    }

    @JRubyMethod(name = "year")
    public RubyInteger year() {
        return getRuntime().newFixnum(cal.get(Calendar.YEAR));
    }

    @JRubyMethod(name = "wday")
    public RubyInteger wday() {
        return getRuntime().newFixnum(cal.get(Calendar.DAY_OF_WEEK) - 1);
    }

    @JRubyMethod(name = "yday")
    public RubyInteger yday() {
        return getRuntime().newFixnum(cal.get(Calendar.DAY_OF_YEAR));
    }

    @JRubyMethod(name = "gmt_offset", name2 = "gmtoffset", name3 = "utc_offset")
    public RubyInteger gmt_offset() {
        return getRuntime().newFixnum((int)(cal.get(Calendar.ZONE_OFFSET)/1000));
    }
    
    @JRubyMethod(name = "isdst", name2 = "dst?")
    public RubyBoolean isdst() {
        return getRuntime().newBoolean(cal.getTimeZone().inDaylightTime(cal.getTime()));
    }

    @JRubyMethod(name = "zone")
    public RubyString zone() {
        return getRuntime().newString(cal.getTimeZone().getDisplayName(cal.get(Calendar.DST_OFFSET) != 0, TimeZone.SHORT));
    }

    public void setJavaCalendar(Calendar cal) {
        this.cal = cal;
    }

    public Calendar getJavaCalendar() {
        return this.cal;
    }

    public Date getJavaDate() {
        return this.cal.getTime();
    }

    @JRubyMethod(name = "hash")
    public RubyFixnum hash() {
    	// modified to match how hash is calculated in 1.8.2
        return getRuntime().newFixnum((int)(((cal.getTimeInMillis() / 1000) ^ microseconds()) << 1) >> 1);
    }    

    @JRubyMethod(name = "_dump", optional = 1, frame = true)
    public RubyString dump(IRubyObject[] args, Block unusedBlock) {
        RubyString str = (RubyString) mdump(new IRubyObject[] { this });
        str.setInstanceVariables(this.getInstanceVariables());
        return str;
    }    

    public RubyObject mdump(final IRubyObject[] args) {
        RubyTime obj = (RubyTime)args[0];
        Calendar calendar = obj.gmtime().cal;
        byte dumpValue[] = new byte[8];
        int pe = 
            0x1                                 << 31 |
            (calendar.get(Calendar.YEAR)-1900)  << 14 |
            calendar.get(Calendar.MONTH)        << 10 |
            calendar.get(Calendar.DAY_OF_MONTH) << 5  |
            calendar.get(Calendar.HOUR_OF_DAY);
        int se =
            calendar.get(Calendar.MINUTE)       << 26 |
            calendar.get(Calendar.SECOND)       << 20 |
            calendar.get(Calendar.MILLISECOND);

        for(int i = 0; i < 4; i++) {
            dumpValue[i] = (byte)(pe & 0xFF);
            pe >>>= 8;
        }
        for(int i = 4; i < 8 ;i++) {
            dumpValue[i] = (byte)(se & 0xFF);
            se >>>= 8;
        }
        return RubyString.newString(obj.getRuntime(), new ByteList(dumpValue,false));
    }

    @JRubyMethod(name = "initialize", frame = true, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(Block block) {
        return this;
    }
    
    /* Time class methods */
    
    public static IRubyObject s_new(IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = recv.getRuntime();
        RubyTime time = new RubyTime(runtime, (RubyClass) recv);
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(new Date());
        time.setJavaCalendar(cal);
        time.callInit(args,block);
        return time;
    }

    @JRubyMethod(name = "new", name2 = "now", rest = true, frame = true, meta = true)
    public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args, Block block) {
        IRubyObject obj = ((RubyClass)recv).allocate();
        obj.callMethod(recv.getRuntime().getCurrentContext(), "initialize", args, block);
        return obj;
    }
    
    @JRubyMethod(name = "at", required = 1, optional = 1, meta = true)
    public static IRubyObject at(IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = recv.getRuntime();
        int len = Arity.checkArgumentCount(runtime, args, 1, 2);

        Calendar cal = Calendar.getInstance(); 
        RubyTime time = new RubyTime(runtime, (RubyClass) recv, cal);

        if (args[0] instanceof RubyTime) {
            RubyTime other = (RubyTime) args[0];
            other.updateCal(cal);
            time.setUSec(other.getUSec());
        } else {
            long seconds = RubyNumeric.num2long(args[0]);
            long millisecs = 0;
            long microsecs = 0;
            if (len > 1) {
                long tmp = RubyNumeric.num2long(args[1]);
                millisecs = tmp / 1000;
                microsecs = tmp % 1000;
            }
            else {
                // In the case of two arguments, MRI will discard the portion of
                // the first argument after a decimal point (i.e., "floor").
                // However in the case of a single argument, any portion after
                // the decimal point is honored.
                if (args[0] instanceof RubyFloat) {
                    double dbl = ((RubyFloat) args[0]).getDoubleValue();
                    long micro = (long) ((dbl - seconds) * 1000000);
                    millisecs = micro / 1000;
                    microsecs = micro % 1000;
                }
            }
            time.setUSec(microsecs);
            cal.setTimeInMillis(seconds * 1000 + millisecs);
        }

        time.callInit(IRubyObject.NULL_ARRAY, Block.NULL_BLOCK);

        return time;
    }

    @JRubyMethod(name = "local", name2 = "mktime", required = 1, optional = 9, meta = true)
    public static RubyTime new_local(IRubyObject recv, IRubyObject[] args) {
        return createTime(recv, args, false);
    }

    @JRubyMethod(name = "utc", name2 = "gm", required = 1, optional = 9, meta = true)
    public static RubyTime new_utc(IRubyObject recv, IRubyObject[] args) {
        return createTime(recv, args, true);
    }

    @JRubyMethod(name = "_load", required = 1, frame = true, meta = true)
    public static RubyTime load(IRubyObject recv, IRubyObject from, Block block) {
        return s_mload(recv, (RubyTime)(((RubyClass)recv).allocate()), from);
    }

    protected static RubyTime s_mload(IRubyObject recv, RubyTime time, IRubyObject from) {
        Ruby runtime = recv.getRuntime();
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.setTimeZone(TimeZone.getTimeZone(RubyTime.UTC));
        byte[] fromAsBytes = null;
        fromAsBytes = from.convertToString().getBytes();
        if(fromAsBytes.length != 8) {
            throw runtime.newTypeError("marshaled time format differ");
        }
        int p=0;
        int s=0;
        for (int i = 0; i < 4; i++) {
            p |= ((int)fromAsBytes[i] & 0xFF) << (8 * i);
        }
        for (int i = 4; i < 8; i++) {
            s |= ((int)fromAsBytes[i] & 0xFF) << (8 * (i - 4));
        }
        if ((p & (1<<31)) == 0) {
            calendar.setTimeInMillis(p * 1000L + s);
        } else {
            p &= ~(1<<31);
            calendar.set(Calendar.YEAR, ((p >>> 14) & 0xFFFF) + 1900);
            calendar.set(Calendar.MONTH, ((p >>> 10) & 0xF));
            calendar.set(Calendar.DAY_OF_MONTH, ((p >>> 5)  & 0x1F));
            calendar.set(Calendar.HOUR_OF_DAY, (p & 0x1F));
            calendar.set(Calendar.MINUTE, ((s >>> 26) & 0x3F));
            calendar.set(Calendar.SECOND, ((s >>> 20) & 0x3F));
            calendar.set(Calendar.MILLISECOND, (s & 0xFFFFF));
        }
        time.setJavaCalendar(calendar);
        return time;
    }
    
    private static final String[] months = {"jan", "feb", "mar", "apr", "may", "jun",
                                            "jul", "aug", "sep", "oct", "nov", "dec"};
    private static final long[] time_min = {1, 0, 0, 0, 0};
    private static final long[] time_max = {31, 23, 59, 60, Long.MAX_VALUE};

    private static final int ARG_SIZE = 7;
    private static RubyTime createTime(IRubyObject recv, IRubyObject[] args, boolean gmt) {
        Ruby runtime = recv.getRuntime();
        int len = ARG_SIZE;
        
        if (args.length == 10) {
            args = new IRubyObject[] { args[5], args[4], args[3], args[2], args[1], args[0], runtime.getNil() };
        } else {
            // MRI accepts additional wday argument which appears to be ignored.
            len = Arity.checkArgumentCount(runtime, args, 1, 8);
            
            if (len < ARG_SIZE) {
                IRubyObject[] newArgs = new IRubyObject[ARG_SIZE];
                System.arraycopy(args, 0, newArgs, 0, args.length);
                for (int i = len; i < ARG_SIZE; i++) {
                    newArgs[i] = runtime.getNil();
                }
                args = newArgs;
                len = ARG_SIZE;
            }
        }
        ThreadContext tc = runtime.getCurrentContext();
        
        if(args[0] instanceof RubyString) {
            args[0] = RubyNumeric.str2inum(runtime, (RubyString) args[0], 10, false);
        }
        
        int year = (int) RubyNumeric.num2long(args[0]);
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
                            throw runtime.newArgumentError("Argument out of range.");
                        }
                    }
                } else {
                    month = (int)RubyNumeric.num2long(args[1]) - 1;
                }
            }
            if (0 > month || month > 11) {
                throw runtime.newArgumentError("Argument out of range.");
            }
        }

        int[] int_args = { 1, 0, 0, 0, 0, 0 };

        for (int i = 0; int_args.length >= i + 2; i++) {
            if (!args[i + 2].isNil()) {
                if(!(args[i+2] instanceof RubyNumeric)) {
                    args[i+2] = args[i+2].callMethod(tc,"to_i");
                }
                int_args[i] = (int)RubyNumeric.num2long(args[i + 2]);
                if (time_min[i] > int_args[i] || int_args[i] > time_max[i]) {
                    throw runtime.newArgumentError("Argument out of range.");
                }
            }
        }
        
        if (year < 100) year += 2000;
        
        Calendar cal;
        if (gmt) {
            cal = Calendar.getInstance(TimeZone.getTimeZone(RubyTime.UTC)); 
        } else {
            cal = Calendar.getInstance(RubyTime.getLocalTimeZone(runtime));
        }
        cal.set(year, month, int_args[0], int_args[1], int_args[2], int_args[3]);
        cal.set(Calendar.MILLISECOND, int_args[4] / 1000);
        
        if (cal.getTimeInMillis() / 1000 < -0x80000000) {
            throw runtime.newArgumentError("time out of range");
        }
        
        RubyTime time = new RubyTime(runtime, (RubyClass) recv, cal);
        
        time.setUSec(int_args[4] % 1000);
        time.callInit(IRubyObject.NULL_ARRAY, Block.NULL_BLOCK);

        return time;
    }
}
