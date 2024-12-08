/*
 **** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2018 The JRuby Team
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

package org.jruby.ext.date;

import org.joda.time.DateTime;
import org.jruby.*;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Access.timeClass;
import static org.jruby.ext.date.DateUtils.*;
import static org.jruby.ext.date.RubyDate.*;

/**
 * Time's extensions from `require 'date'`
 *
 * @author kares
 */
public abstract class TimeExt {

    private TimeExt() { /* no instances */ }

    static void load(ThreadContext context) {
        timeClass(context).defineMethods(context, TimeExt.class);
    }

    @JRubyMethod
    public static RubyTime to_time(IRubyObject self) { return (RubyTime) self; }

    @JRubyMethod(name = "to_date")
    public static RubyDate to_date(ThreadContext context, IRubyObject self) {
        final DateTime dt = ((RubyTime) self).getDateTime();
        long jd = civil_to_jd(dt.getYear(), dt.getMonthOfYear(), dt.getDayOfMonth(), GREGORIAN);
        return new RubyDate(context, getDate(context.runtime), jd_to_ajd(context, jd), CHRONO_ITALY_UTC, 0);
    }

    @JRubyMethod(name = "to_datetime")
    public static RubyDateTime to_datetime(ThreadContext context, IRubyObject self) {
        final RubyTime time = (RubyTime) self;
        DateTime dt = ((RubyTime) self).getDateTime();

        long subMillisNum = 0, subMillisDen = 1;
        if (time.getNSec() != 0) {
            IRubyObject subMillis = RubyRational.newRationalCanonicalize(context, time.getNSec(), 1_000_000);
            if (subMillis instanceof RubyRational) {
                subMillisNum = ((RubyRational) subMillis).getNumerator().getLongValue();
                subMillisDen = ((RubyRational) subMillis).getDenominator().getLongValue();
            }
            else {
                subMillisNum = ((RubyInteger) subMillis).getLongValue();
            }
        }

        final int off = dt.getZone().getOffset(dt.getMillis()) / 1000;

        int year = dt.getYear(); if (year <= 0) year--; // JODA's Julian chronology (no year 0)

        if (year == 1582) { // take the "slow" path -  JODA isn't adjusting for missing (reform) dates
            return calcAjdFromCivil(context, dt, off, subMillisNum, subMillisDen);
        }

        dt = new DateTime(
                year, dt.getMonthOfYear(), dt.getDayOfMonth(),
                dt.getHourOfDay(), dt.getMinuteOfHour(), dt.getSecondOfMinute(),
                dt.getMillisOfSecond(), getChronology(context, ITALY, dt.getZone())
        );

        return new RubyDateTime(context.runtime, getDateTime(context.runtime), dt, off, ITALY, subMillisNum, subMillisDen);
    }

    private static RubyDateTime calcAjdFromCivil(ThreadContext context, final DateTime dt, final int off,
                                                 final long subMillisNum, final long subMillisDen) {
        final Ruby runtime = context.runtime;

        long jd = civil_to_jd(dt.getYear(), dt.getMonthOfYear(), dt.getDayOfMonth(), ITALY);
        RubyNumeric fr = timeToDayFraction(context, dt.getHourOfDay(), dt.getMinuteOfHour(), dt.getSecondOfMinute());

        final RubyNumeric ajd = jd_to_ajd(context, jd, fr, off);
        RubyDateTime dateTime = new RubyDateTime(context, getDateTime(runtime), ajd, off, ITALY);
        dateTime.dt = dateTime.dt.withMillisOfSecond(dt.getMillisOfSecond());
        dateTime.subMillisNum = subMillisNum; dateTime.subMillisDen = subMillisDen;
        return dateTime;
    }

}
