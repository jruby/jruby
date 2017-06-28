/***** BEGIN LICENSE BLOCK *****
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

import org.jruby.Ruby;
import org.jruby.RubyBignum;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyRational;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.List;

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
        final List<StrptimeToken> compiledPattern = context.runtime.getCachedStrptimePattern(format.asJavaString());
        final StrptimeParser.FormatBag bag = new StrptimeParser().parse(compiledPattern, text.asJavaString());

        return bag == null ? context.nil :  convertFormatBagToHash(context, bag, text.isTaint());
    }

    private IRubyObject convertFormatBagToHash(ThreadContext context, StrptimeParser.FormatBag bag, boolean tainted) {
        Ruby runtime = context.runtime;
        RubyHash hash = RubyHash.newHash(runtime);

        if (has(bag.getMDay())) hash.op_aset(context, RubySymbol.newSymbol(runtime, "mday"), runtime.newFixnum(bag.getMDay()));
        if (has(bag.getWDay())) hash.op_aset(context, RubySymbol.newSymbol(runtime, "wday"), runtime.newFixnum(bag.getWDay()));
        if (has(bag.getCWDay())) hash.op_aset(context, RubySymbol.newSymbol(runtime, "cwday"), runtime.newFixnum(bag.getCWDay()));
        if (has(bag.getYDay())) hash.op_aset(context, RubySymbol.newSymbol(runtime, "yday"), runtime.newFixnum(bag.getYDay()));
        if (has(bag.getCWeek())) hash.op_aset(context, RubySymbol.newSymbol(runtime, "cweek"), runtime.newFixnum(bag.getCWeek()));
        if (has(bag.getCWYear())) hash.op_aset(context, RubySymbol.newSymbol(runtime, "cwyear"), RubyBignum.newBignum(runtime, bag.getCWYear()));
        if (has(bag.getMin())) hash.op_aset(context, RubySymbol.newSymbol(runtime, "min"), runtime.newFixnum(bag.getMin()));
        if (has(bag.getMon())) hash.op_aset(context, RubySymbol.newSymbol(runtime, "mon"), runtime.newFixnum(bag.getMon()));
        if (has(bag.getHour())) hash.op_aset(context, RubySymbol.newSymbol(runtime, "hour"), runtime.newFixnum(bag.getHour()));
        if (has(bag.getYear())) hash.op_aset(context, RubySymbol.newSymbol(runtime, "year"), RubyBignum.newBignum(runtime, bag.getYear()));
        if (has(bag.getSec())) hash.op_aset(context, RubySymbol.newSymbol(runtime, "sec"), runtime.newFixnum(bag.getSec()))
                ;
        if (has(bag.getWNum0())) hash.op_aset(context, RubySymbol.newSymbol(runtime, "wnum0"), runtime.newFixnum(bag.getWNum0()));
        if (has(bag.getWNum1())) hash.op_aset(context, RubySymbol.newSymbol(runtime, "wnum1"), runtime.newFixnum(bag.getWNum1()));

        if (bag.getZone() != null) {
            final RubyString zone = RubyString.newString(runtime, bag.getZone());
            if (tainted) zone.taint(context);

            hash.op_aset(context, RubySymbol.newSymbol(runtime, "zone"), zone);
            int offset = TimeZoneConverter.dateZoneToDiff(bag.getZone());
            if (offset != Integer.MIN_VALUE) hash.op_aset(context, RubySymbol.newSymbol(runtime, "offset"), runtime.newFixnum(offset));
        }

        if (has(bag.getSecFraction())) {
            final RubyBignum secFraction = RubyBignum.newBignum(runtime, bag.getSecFraction());
            final RubyFixnum secFractionSize = RubyFixnum.newFixnum(runtime, (long)Math.pow(10, bag.getSecFractionSize()));
            hash.op_aset(context, RubySymbol.newSymbol(runtime, "sec_fraction"),
                    RubyRational.newRationalCanonicalize(context, secFraction, secFractionSize));
        }

        if (bag.has(bag.getSeconds())) {
            if (has(bag.getSecondsSize())) {
                final RubyBignum seconds = RubyBignum.newBignum(runtime, bag.getSeconds());
                final RubyFixnum secondsSize = RubyFixnum.newFixnum(runtime, (long)Math.pow(10, bag.getSecondsSize()));
                hash.op_aset(context, RubySymbol.newSymbol(runtime, "seconds"), RubyRational.newRationalCanonicalize(context, seconds, secondsSize));
            } else {
                hash.op_aset(context, RubySymbol.newSymbol(runtime, "seconds"), RubyBignum.newBignum(runtime, bag.getSeconds()));
            }
        }
        if (has(bag.getMerid())) {
            hash.op_aset(context, RubySymbol.newSymbol(runtime, "_merid"), runtime.newFixnum(bag.getMerid()));
        }
        if (has(bag.getCent())) {
            hash.op_aset(context, RubySymbol.newSymbol(runtime, "_cent"), RubyBignum.newBignum(runtime, bag.getCent()));
        }
        if (bag.getLeftover() != null) {
            final RubyString leftover = RubyString.newString(runtime, bag.getLeftover());
            if (tainted) leftover.taint(context);

            hash.op_aset(context, RubySymbol.newSymbol(runtime, "leftover"), leftover);
        }

        return hash;
    }
}
