/***** BEGIN LICENSE BLOCK *****
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

package org.jruby.util;

import org.jcodings.Encoding;
import org.jruby.*;
import org.jruby.api.Convert;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.math.BigInteger;
import java.util.List;

import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Create.*;
import static org.jruby.util.StrptimeParser.FormatBag.has;

/**
 * This class has {@code StrptimeParser} and provides methods that are calls from JRuby.
 */
public class RubyDateParser {

    /**
     * Date._strptime method in JRuby 9.1.5.0's lib/ruby/stdlib/date/format.rb is replaced
     * with this method. This is Java implementation of date__strptime method in MRI 2.3.1's
     * ext/date/date_strptime.c.
     * See:
     *  https://github.com/jruby/jruby/blob/036ce39f0476d4bd718e23e64caff36bb50b8dbc/lib/ruby/stdlib/date/format.rb
     *  https://github.com/ruby/ruby/blob/394fa89c67722d35bdda89f10c7de5c304a5efb1/ext/date/date_strptime.c
     */

    public IRubyObject parse(ThreadContext context, final RubyString format, final RubyString text) {
        return parse(context, format.asJavaString(), text);
    }

    public IRubyObject parse(ThreadContext context, final String format, final RubyString text) {
        final List<StrptimeToken> compiledPattern = context.runtime.getCachedStrptimePattern(format);
        final StrptimeParser.FormatBag bag = new StrptimeParser().parse(compiledPattern, text.asJavaString());

        return bag == null ? context.nil : convertFormatBagToHash(context, bag, text.getEncoding());
    }

    static RubyHash convertFormatBagToHash(ThreadContext context, StrptimeParser.FormatBag bag,
                                           Encoding encoding) {
        final RubyHash hash = newHash(context);

        if (has(bag.getMDay())) setHashValue(context, hash, "mday", asFixnum(context, bag.getMDay()));
        if (has(bag.getWDay())) setHashValue(context, hash, "wday", asFixnum(context, bag.getWDay()));
        if (has(bag.getCWDay())) setHashValue(context, hash, "cwday", asFixnum(context, bag.getCWDay()));
        if (has(bag.getYDay())) setHashValue(context, hash, "yday", asFixnum(context, bag.getYDay()));
        if (has(bag.getCWeek())) setHashValue(context, hash, "cweek", asFixnum(context, bag.getCWeek()));
        if (has(bag.getCWYear())) setHashValue(context, hash, "cwyear", RubyBignum.newBignum(context.runtime, bag.getCWYear()));
        if (has(bag.getMin())) setHashValue(context, hash, "min", asFixnum(context, bag.getMin()));
        if (has(bag.getMon())) setHashValue(context, hash, "mon", asFixnum(context, bag.getMon()));
        if (has(bag.getHour())) setHashValue(context, hash, "hour", asFixnum(context, bag.getHour()));
        if (has(bag.getYear())) setHashValue(context, hash, "year", RubyBignum.newBignum(context.runtime, bag.getYear()));
        if (has(bag.getSec())) setHashValue(context, hash, "sec", asFixnum(context, bag.getSec()));
        if (has(bag.getWNum0())) setHashValue(context, hash, "wnum0", asFixnum(context, bag.getWNum0()));
        if (has(bag.getWNum1())) setHashValue(context, hash, "wnum1", asFixnum(context, bag.getWNum1()));

        if (bag.getZone() != null) {
            final RubyString zone = RubyString.newString(context.runtime, bag.getZone(), encoding);

            setHashValue(context, hash, "zone", zone);
            int offset = TimeZoneConverter.dateZoneToDiff(bag.getZone());
            if (offset != TimeZoneConverter.INVALID_ZONE) setHashValue(context, hash, "offset", asFixnum(context, offset));
        }

        if (has(bag.getSecFraction())) {
            final RubyInteger secFraction = toRubyInteger(context, bag.getSecFraction());
            final RubyFixnum secFractionSize = asFixnum(context, (long) Math.pow(10, bag.getSecFractionSize()));
            setHashValue(context, hash, "sec_fraction",
                    RubyRational.newRationalCanonicalize(context, secFraction, secFractionSize));
        }

        if (has(bag.getSeconds())) {
            if (has(bag.getSecondsSize())) {
                final RubyInteger seconds = toRubyInteger(context, bag.getSeconds());
                final RubyFixnum secondsSize = asFixnum(context, (long) Math.pow(10, bag.getSecondsSize()));
                setHashValue(context, hash, "seconds", RubyRational.newRationalCanonicalize(context, seconds, secondsSize));
            } else {
                setHashValue(context, hash, "seconds", toRubyInteger(context, bag.getSeconds()));
            }
        }
        if (has(bag.getMerid())) {
            setHashValue(context, hash, "_merid", asFixnum(context, bag.getMerid()));
        }
        if (has(bag.getCent())) {
            setHashValue(context, hash, "_cent", RubyBignum.newBignum(context.runtime, bag.getCent()));
        }
        if (bag.getLeftover() != null) {
            final RubyString leftover = newString(context, bag.getLeftover(), encoding);

            setHashValue(context, hash, "leftover", leftover);
        }

        return hash;
    }

    private static RubyInteger toRubyInteger(ThreadContext context, final Number i) {
        return i instanceof BigInteger bigint ?
                RubyBignum.newBignum(context.runtime, bigint) :
                asFixnum(context, i.longValue());
    }

    private static void setHashValue(final ThreadContext context, final RubyHash hash, final String key, final IRubyObject value) {
        hash.fastASet(Convert.asSymbol(context, key), value);
    }

}
