/*
 **** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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

import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.chrono.GJChronology;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyInteger;
import org.jruby.RubyRational;
import org.jruby.RubyTime;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * JRuby's <code>DateTime</code> implementation - 'native' parts.
 * In MRI, since 2.x, all of date.rb has been moved to native (C) code.
 *
 * @see RubyDate
 *
 * @author kares
 */
@JRubyClass(name = "DateTime")
public class RubyDateTime extends RubyDate {

    static RubyClass createDateTimeClass(Ruby runtime, RubyClass Date) {
        RubyClass DateTime = runtime.defineClass("DateTime", Date, ALLOCATOR);
        DateTime.setReifiedClass(RubyDateTime.class);
        DateTime.defineAnnotatedMethods(RubyDateTime.class);
        return DateTime;
    }

    private static final ObjectAllocator ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyDateTime(runtime, klass, defaultDateTime);
        }
    };

    private static RubyClass getDateTime(final Ruby runtime) {
        return (RubyClass) runtime.getObject().getConstantAt("DateTime");
    }

    public RubyDateTime(Ruby runtime, RubyClass klass, DateTime dt) {
        super(runtime, klass, dt);
    }

    public RubyDateTime(Ruby runtime, DateTime dt) {
        super(runtime, getDateTime(runtime), dt);
    }

    public RubyDateTime(Ruby runtime, long millis, Chronology chronology) {
        super(runtime, getDateTime(runtime), new DateTime(millis, chronology));
    }

    RubyDateTime(Ruby runtime, DateTime dt, int off, int start) {
        super(runtime, getDateTime(runtime));

        this.dt = dt;
        this.off = off; this.start = start;
    }

    /**
     # Create a new DateTime object corresponding to the specified
     # Civil Date and hour +h+, minute +min+, second +s+.
     #
     # The 24-hour clock is used.  Negative values of +h+, +min+, and
     # +sec+ are treating as counting backwards from the end of the
     # next larger unit (e.g. a +min+ of -2 is treated as 58).  No
     # wraparound is performed.  If an invalid time portion is specified,
     # an ArgumentError is raised.
     #
     # +of+ is the offset from UTC as a fraction of a day (defaults to 0).
     # +sg+ specifies the Day of Calendar Reform.
     #
     # +y+ defaults to -4712, +m+ to 1, and +d+ to 1; this is Julian Day
     # Number day 0.  The time values default to 0.
     **/
    // DateTime.civil([year=-4712[, month=1[, mday=1[, hour=0[, minute=0[, second=0[, offset=0[, start=Date::ITALY]]]]]]]])
    // DateTime.new([year=-4712[, month=1[, mday=1[, hour=0[, minute=0[, second=0[, offset=0[, start=Date::ITALY]]]]]]]])

    @JRubyMethod(name = "civil", alias = "new", meta = true)
    public static RubyDateTime civil(ThreadContext context, IRubyObject self) {
        return new RubyDateTime(context.runtime, defaultDateTime);
    }

    @JRubyMethod(name = "civil", alias = "new", meta = true)
    public static RubyDateTime civil(ThreadContext context, IRubyObject self, IRubyObject year) {
        return new RubyDateTime(context.runtime, civilImpl(context, year));
    }

    @JRubyMethod(name = "civil", alias = "new", meta = true)
    public static RubyDateTime civil(ThreadContext context, IRubyObject self, IRubyObject year, IRubyObject month) {
        return new RubyDateTime(context.runtime, civilImpl(context, year, month));
    }

    @JRubyMethod(name = "civil", alias = "new", meta = true)
    public static RubyDateTime civil(ThreadContext context, IRubyObject self, IRubyObject year, IRubyObject month, IRubyObject mday) {
        return new RubyDateTime(context.runtime, civilImpl(context, year, month, mday));
    }

    @JRubyMethod(name = "civil", alias = "new", meta = true, optional = 8)
    public static RubyDateTime civil(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        // year=-4712, month=1, mday=1,
        //  hour=0, minute=0, second=0, offset=0, start=Date::ITALY

        int hour = 0, minute = 0, second = 0; long millis = 0; int off = 0, sg = ITALY;

        switch (args.length) {
            case 8: sg = val2sg(context, args[7]);
            case 7: off = val2off(context, args[6]);
            case 6:
                long sec = args[5].convertToInteger().getLongValue();
                if (args[5] instanceof RubyRational) {
                    millis = secMillis(context, (RubyRational) args[5]);
                }
                second = (int) (sec < 0 ? sec + 60 : sec); // JODA will handle invalid value
            case 5: minute = getMinute(context, args[4]);
            case 4: hour = getHour(context, args[3]);
                break;

            // TODO interpreter needs a ThreeOperandArgNoBlockCallInstr otherwise routes 3 args via []
            case 3: return civil(context, self, args[0], args[1], args[2]);
        }

        final int year = (sg > 0) ? getYear(args[0]) : args[0].convertToInteger().getIntValue();
        final int month = getMonth(args[1]);
        final int day = args[2].convertToInteger().getIntValue();

        final Chronology chronology = getChronology(context, sg, off);
        DateTime dt = civilDate(context, year, month, day, chronology); // hour: 0, minute: 0, second: 0
        try {
            long ms = dt.getMillis();
            ms = chronology.hourOfDay().set(ms, hour);
            ms = chronology.minuteOfHour().set(ms, minute);
            ms = chronology.secondOfMinute().set(ms, second);
            dt = dt.withMillis(ms + millis);
        }
        catch (IllegalArgumentException ex) {
            debug(context, "invalid date", ex);
            throw context.runtime.newArgumentError("invalid date");
        }

        return new RubyDateTime(context.runtime, dt, off, sg);
    }

    private static long secMillis(ThreadContext context, RubyRational sec) {
        RubyInteger val = (RubyInteger) sec.getNumerator().op_mul(context, 1000);
        val = (RubyInteger) val.idiv(context, sec.getDenominator());
        return ((RubyInteger) val.op_mod(context, 1000)).getLongValue();
    }

    private static int getHour(ThreadContext context, IRubyObject hour) {
        long h = hour.convertToInteger().getLongValue();
        assertValidFraction(context, hour, h);
        return (int) (h < 0 ? h + 24 : h); // JODA will handle invalid value
    }

    private static int getMinute(ThreadContext context, IRubyObject min) {
        long m = min.convertToInteger().getLongValue();
        assertValidFraction(context, min, m);
        return (int) (m < 0 ? m + 60 : m); // JODA will handle invalid value
    }

    private static void assertValidFraction(ThreadContext context, IRubyObject val, long ival) {
        if (val instanceof RubyRational) {
            IRubyObject eql = ((RubyRational) val).op_equal(context, RubyFixnum.newFixnum(context.runtime, ival));
            if (eql != context.tru) throw context.runtime.newArgumentError("invalid fraction");
        }
    }

    /**
     # Create a new DateTime object representing the current time.
     #
     # +sg+ specifies the Day of Calendar Reform.
     **/

    @JRubyMethod(meta = true)
    public static RubyDateTime now(ThreadContext context, IRubyObject self) { // sg=ITALY
        final DateTimeZone zone = RubyTime.getLocalTimeZone(context.runtime);
        if (zone == DateTimeZone.UTC) {
            return new RubyDateTime(context.runtime, new DateTime(CHRONO_ITALY_UTC));
        }
        return new RubyDateTime(context.runtime, new DateTime(GJChronology.getInstance(zone)));
    }

    @JRubyMethod(meta = true)
    public static RubyDateTime now(ThreadContext context, IRubyObject self, IRubyObject sg) {
        final int start = val2sg(context, sg);
        final DateTimeZone zone = RubyTime.getLocalTimeZone(context.runtime);
        return new RubyDateTime(context.runtime, new DateTime(getChronology(context, start, zone)), 0, start);
    }

    @JRubyMethod // Date.civil(year, mon, mday, @sg)
    public RubyDate to_date(ThreadContext context) {
        return new RubyDate(context.runtime, dt.withTimeAtStartOfDay(), 0, start, 0);
    }

}
