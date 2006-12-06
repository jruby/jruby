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
import java.util.Locale;
import java.util.TimeZone;

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

    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("-", Locale.US);

    public RubyTime(IRuby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass);
    }
    
    public RubyTime(IRuby runtime, RubyClass rubyClass, Calendar cal) {
        super(runtime, rubyClass);
        this.cal = cal;
    }
    
    public void setUSec(long usec) {
        this.usec = usec;
    }
    
    public void updateCal(Calendar calendar) {
        calendar.setTimeZone(cal.getTimeZone());
        calendar.setTimeInMillis(getTimeInMillis());
    }
    
    protected long getTimeInMillis() {
        return cal.getTimeInMillis();  // For JDK 1.4 we can use "cal.getTimeInMillis()"
    }
    
    public static RubyTime newTime(IRuby runtime, long milliseconds) {
        Calendar cal = Calendar.getInstance(); 
        RubyTime time = new RubyTime(runtime, runtime.getClass("Time"), cal);
        
        cal.setTimeInMillis(milliseconds);
        
        return time;
    }
    
    public static RubyTime newTime(IRuby runtime, Calendar cal) {
        RubyTime time = new RubyTime(runtime, runtime.getClass("Time"), cal);
        
        return time;
    }

    public IRubyObject initialize_copy(IRubyObject original) {
        if (!(original instanceof RubyTime)) {
            throw getRuntime().newTypeError("Expecting an instance of class Time");
        }
        
        RubyTime originalTime = (RubyTime) original;
        
        cal = originalTime.cal;
        usec = originalTime.usec;
        
        return this;
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
    
    public RubyTime getgm() {
        Calendar newCal = (Calendar)cal.clone();
        newCal.setTimeZone(TimeZone.getTimeZone(UTC));
        return newTime(getRuntime(), newCal);
    }
    
    public RubyTime getlocal() {
        Calendar newCal = (Calendar)cal.clone();
        newCal.setTimeZone(TimeZone.getDefault());
        return newTime(getRuntime(), newCal);
    }

    public RubyString strftime(IRubyObject format) {
        final RubyDateFormat rubyDateFormat = new RubyDateFormat("-", Locale.US);
        rubyDateFormat.setCalendar(cal);
        rubyDateFormat.applyPattern(format.toString());
        String result = rubyDateFormat.format(cal.getTime());

        return getRuntime().newString(result);
    }
    
    public IRubyObject op_ge(IRubyObject other) {
        if (other instanceof RubyTime) {
            return getRuntime().newBoolean(cmp((RubyTime) other) >= 0);
        }
        
        return RubyComparable.op_ge(this, other);
    }
    
    public IRubyObject op_gt(IRubyObject other) {
        if (other instanceof RubyTime) {
            return getRuntime().newBoolean(cmp((RubyTime) other) > 0);
        }
        
        return RubyComparable.op_gt(this, other);
    }
    
    public IRubyObject op_le(IRubyObject other) {
        if (other instanceof RubyTime) {
            return getRuntime().newBoolean(cmp((RubyTime) other) <= 0);
        }
        
        return RubyComparable.op_le(this, other);
    }
    
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

    public IRubyObject same2(IRubyObject other) {
        return (RubyNumeric.fix2int(callMethod(getRuntime().getCurrentContext(), "<=>", other)) == 0) ? getRuntime().getTrue() : getRuntime().getFalse();
    }

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

    public RubyString asctime() {
        simpleDateFormat.setCalendar(cal);
        simpleDateFormat.applyPattern("EEE MMM dd HH:mm:ss yyyy");
        String result = simpleDateFormat.format(cal.getTime());

        return getRuntime().newString(result);
    }

    public IRubyObject to_s() {
        simpleDateFormat.setCalendar(cal);
        simpleDateFormat.applyPattern("EEE MMM dd HH:mm:ss z yyyy");
        String result = simpleDateFormat.format(cal.getTime());

        return getRuntime().newString(result);
    }

    public RubyArray to_a() {
        return getRuntime().newArray(new IRubyObject[] { sec(), min(), hour(), mday(), month(), 
                year(), wday(), yday(), isdst(), zone() });
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

    public RubyInteger gmt_offset() {
        return getRuntime().newFixnum((int)(cal.get(Calendar.ZONE_OFFSET)/1000));
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

    public RubyString dump(final IRubyObject[] args) {
        if (args.length > 1) {
            throw getRuntime().newArgumentError(0, 1);
        }

        return (RubyString) mdump(new IRubyObject[] { this });
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
        return RubyString.newString(obj.getRuntime(), dumpValue);
    }
}
