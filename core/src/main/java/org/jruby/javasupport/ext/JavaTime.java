/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2019 The JRuby Team
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

package org.jruby.javasupport.ext;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyTime;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.javasupport.JavaUtil.unwrapIfJavaObject;

/**
 * Java::JavaTime package extensions.
 *
 * @author kares
 */
public class JavaTime {

    public static void define(ThreadContext context) {
        var runtime = context.runtime;
        JavaExtensions.put(runtime, java.time.Instant.class, proxy -> proxy.defineMethods(context, Instant.class));
        JavaExtensions.put(runtime, java.time.OffsetDateTime.class, proxy -> proxy.defineMethods(context, OffsetDateTime.class));
        JavaExtensions.put(runtime, java.time.LocalDateTime.class, proxy -> proxy.defineMethods(context, LocalDateTime.class));
        JavaExtensions.put(runtime, java.time.ZonedDateTime.class, proxy -> proxy.defineMethods(context, ZonedDateTime.class));
        JavaExtensions.put(runtime, java.time.ZoneId.class, (klass) -> {
            klass.addMethod(context, "inspect", new JavaLang.InspectRawValue(klass));
        });
        JavaExtensions.put(runtime, java.time.temporal.Temporal.class, (klass) -> {
            klass.addMethod(context, "inspect", new JavaLang.InspectValueWithTypePrefix(klass));
        });
    }

    @JRubyModule(name = "Java::JavaTime::Instant")
    public static class Instant {
        @JRubyMethod(name = "to_time")
        public static IRubyObject to_time(ThreadContext context, IRubyObject self) {
            final java.time.Instant val = unwrapIfJavaObject(self);
            long nano = val.getNano();
            long millis = val.getEpochSecond() * 1000 + (nano / 1_000_000);
            nano = nano % 1_000_000;
            return RubyTime.newTime(context.runtime, new DateTime(millis, RubyTime.getLocalTimeZone(context)), nano);
        }

    }

    @JRubyModule(name = "Java::JavaTime::LocalDateTime")
    public static class LocalDateTime {
        @JRubyMethod(name = "to_time")
        public static IRubyObject to_time(ThreadContext context, IRubyObject self) {
            java.time.LocalDateTime val = unwrapIfJavaObject(self);
            return toTime(context, val.getYear(), val.getMonthValue(), val.getDayOfMonth(),
                    val.getHour(), val.getMinute(), val.getSecond(),
                    val.getNano(), RubyTime.getLocalTimeZone(context));
        }

    }

    @JRubyModule(name = "Java::JavaTime::OffsetDateTime")
    public static class OffsetDateTime {
        @JRubyMethod(name = "to_time")
        public static IRubyObject to_time(ThreadContext context, IRubyObject self) {
            java.time.OffsetDateTime val = unwrapIfJavaObject(self);
            return toTime(context, val.getYear(), val.getMonthValue(), val.getDayOfMonth(),
                    val.getHour(), val.getMinute(), val.getSecond(),
                    val.getNano(), convertZone(val.getOffset().getId()));
        }

    }

    @JRubyModule(name = "Java::JavaTime::ZonedDateTime")
    public static class ZonedDateTime {
        @JRubyMethod(name = "to_time")
        public static IRubyObject to_time(ThreadContext context, IRubyObject self) {
            java.time.ZonedDateTime val = unwrapIfJavaObject(self);
            return toTime(context, val.getYear(), val.getMonthValue(), val.getDayOfMonth(),
                    val.getHour(), val.getMinute(), val.getSecond(),
                    val.getNano(), convertZone(val.getZone().getId()));
        }

    }

    private static RubyTime toTime(ThreadContext context,
                                   int year, int month, int day, int hour, int min, int sec, int nano,
                                   DateTimeZone zone) {
        int millisOfSec = nano / 1_000_000;
        DateTime dt = new DateTime(
                year,
                month,
                day,
                hour,
                min,
                sec,
                millisOfSec,
                zone
        );
        return RubyTime.newTime(context.runtime, dt, nano % 1_000_000);
    }

    /**
     * Convert a Java time zone to a JODA time zone.
     * @param id
     * @return a (joda) date-time zone from Java time's zone id
     */
    private static DateTimeZone convertZone(final String id) {
        if ("Z".equals(id)) { // special Java time case JODA does not handle (for UTC)
            return DateTimeZone.UTC;
        }
        return DateTimeZone.forID(id);
    }

}