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

/**
 * This class is ported from RubyDateFormatter.Format in JRuby 9.1.5.0.
 * @see https://github.com/jruby/jruby/blob/036ce39f0476d4bd718e23e64caff36bb50b8dbc/core/src/main/java/org/jruby/util/RubyDateFormatter.java
 */
enum StrptimeFormat {
    FORMAT_STRING, // raw string, no formatting
    FORMAT_SPECIAL, // composition of other formats

    FORMAT_WEEK_LONG, // %A
    FORMAT_WEEK_SHORT, // %a
    FORMAT_MONTH_LONG, // %B
    FORMAT_MONTH_SHORT, // %b, %h
    FORMAT_CENTURY, // %C
    FORMAT_DAY, // %d
    FORMAT_DAY_S, // %e
    FORMAT_WEEKYEAR, // %G
    FORMAT_WEEKYEAR_SHORT, // %g
    FORMAT_HOUR, // %H
    FORMAT_HOUR_M, // %I
    FORMAT_DAY_YEAR, // %j
    FORMAT_HOUR_BLANK, // %k
    FORMAT_MILLISEC, // %L
    FORMAT_HOUR_S, // %l
    FORMAT_MINUTES, // %M
    FORMAT_MONTH, // %m
    FORMAT_NANOSEC, // %N
    FORMAT_MERIDIAN_LOWER_CASE, // %P
    FORMAT_MERIDIAN, // %p
    FORMAT_MICROSEC_EPOCH, // %Q Only for Date/DateTime from here
    FORMAT_SECONDS, // %S
    FORMAT_EPOCH, // %s
    FORMAT_WEEK_YEAR_S, // %U
    FORMAT_DAY_WEEK2, // %u
    FORMAT_WEEK_WEEKYEAR, // %V
    FORMAT_WEEK_YEAR_M, // %W
    FORMAT_DAY_WEEK, // %w
    FORMAT_YEAR_LONG, // %Y
    FORMAT_YEAR_SHORT, // %y

    FORMAT_COLON_ZONE_OFF, // %z, %:z, %::z, %:::z must be given number of colons as data

    FORMAT_ZONE_ID; // %Z Change between Time and Date
}
