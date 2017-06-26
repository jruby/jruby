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
 * This is utility class to convert given timezone into integer based timezone
 * diff. It's ported from ext/date/date_parse.c in MRI 2.3.1 under BSDL.
 * @see https://github.com/ruby/ruby/blob/394fa89c67722d35bdda89f10c7de5c304a5efb1/ext/date/date_parse.c
 */
public class TimeZoneConverter {
    // Ports zones_source in ext/date/date_parse.c in MRI 2.3.1 under BSDL.
    private static int getOffsetFromZonesSource(String z) {
        switch (z) {
            case "ut":
                return 0 * 3600;
            case "gmt":
                return 0 * 3600;
            case "est":
                return -5 * 3600;
            case "edt":
                return -4 * 3600;
            case "cst":
                return -6 * 3600;
            case "cdt":
                return -5 * 3600;
            case "mst":
                return -7 * 3600;
            case "mdt":
                return -6 * 3600;
            case "pst":
                return -8 * 3600;
            case "pdt":
                return -7 * 3600;
            case "a":
                return 1 * 3600;
            case "b":
                return 2 * 3600;
            case "c":
                return 3 * 3600;
            case "d":
                return 4 * 3600;
            case "e":
                return 5 * 3600;
            case "f":
                return 6 * 3600;
            case "g":
                return 7 * 3600;
            case "h":
                return 8 * 3600;
            case "i":
                return 9 * 3600;
            case "k":
                return 10 * 3600;
            case "l":
                return 11 * 3600;
            case "m":
                return 12 * 3600;
            case "n":
                return -1 * 3600;
            case "o":
                return -2 * 3600;
            case "p":
                return -3 * 3600;
            case "q":
                return -4 * 3600;
            case "r":
                return -5 * 3600;
            case "s":
                return -6 * 3600;
            case "t":
                return -7 * 3600;
            case "u":
                return -8 * 3600;
            case "v":
                return -9 * 3600;
            case "w":
                return -10 * 3600;
            case "x":
                return -11 * 3600;
            case "y":
                return -12 * 3600;
            case "z":
                return 0 * 3600;
            case "utc":
                return 0 * 3600;
            case "wet":
                return 0 * 3600;
            case "at":
                return -2 * 3600;
            case "brst":
                return -2 * 3600;
            case "ndt":
                return -(2 * 3600 + 1800);
            case "art":
                return -3 * 3600;
            case "adt":
                return -3 * 3600;
            case "brt":
                return -3 * 3600;
            case "clst":
                return -3 * 3600;
            case "nst":
                return -(3 * 3600 + 1800);
            case "ast":
                return -4 * 3600;
            case "clt":
                return -4 * 3600;
            case "akdt":
                return -8 * 3600;
            case "ydt":
                return -8 * 3600;
            case "akst":
                return -9 * 3600;
            case "hadt":
                return -9 * 3600;
            case "hdt":
                return -9 * 3600;
            case "yst":
                return -9 * 3600;
            case "ahst":
                return -10 * 3600;
            case "cat":
                return -10 * 3600;
            case "hast":
                return -10 * 3600;
            case "hst":
                return -10 * 3600;
            case "nt":
                return -11 * 3600;
            case "idlw":
                return -12 * 3600;
            case "bst":
                return 1 * 3600;
            case "cet":
                return 1 * 3600;
            case "fwt":
                return 1 * 3600;
            case "met":
                return 1 * 3600;
            case "mewt":
                return 1 * 3600;
            case "mez":
                return 1 * 3600;
            case "swt":
                return 1 * 3600;
            case "wat":
                return 1 * 3600;
            case "west":
                return 1 * 3600;
            case "cest":
                return 2 * 3600;
            case "eet":
                return 2 * 3600;
            case "fst":
                return 2 * 3600;
            case "mest":
                return 2 * 3600;
            case "mesz":
                return 2 * 3600;
            case "sast":
                return 2 * 3600;
            case "sst":
                return 2 * 3600;
            case "bt":
                return 3 * 3600;
            case "eat":
                return 3 * 3600;
            case "eest":
                return 3 * 3600;
            case "msk":
                return 3 * 3600;
            case "msd":
                return 4 * 3600;
            case "zp4":
                return 4 * 3600;
            case "zp5":
                return 5 * 3600;
            case "ist":
                return 5 * 3600 + 1800;
            case "zp6":
                return 6 * 3600;
            case "wast":
                return 7 * 3600;
            case "cct":
                return 8 * 3600;
            case "sgt":
                return 8 * 3600;
            case "wadt":
                return 8 * 3600;
            case "jst":
                return 9 * 3600;
            case "kst":
                return 9 * 3600;
            case "east":
                return 10 * 3600;
            case "gst":
                return 10 * 3600;
            case "eadt":
                return 11 * 3600;
            case "idle":
                return 12 * 3600;
            case "nzst":
                return 12 * 3600;
            case "nzt":
                return 12 * 3600;
            case "nzdt":
                return 13 * 3600;
            case "afghanistan":
                return 16200;
            case "alaskan":
                return -32400;
            case "arab":
                return 10800;
            case "arabian":
                return 14400;
            case "arabic":
                return 10800;
            case "atlantic":
                return -14400;
            case "aus central":
                return 34200;
            case "aus eastern":
                return 36000;
            case "azores":
                return -3600;
            case "canada central":
                return -21600;
            case "cape verde":
                return -3600;
            case "caucasus":
                return 14400;
            case "cen. australia":
                return 34200;
            case "central america":
                return -21600;
            case "central asia":
                return 21600;
            case "central europe":
                return 3600;
            case "central european":
                return 3600;
            case "central pacific":
                return 39600;
            case "central":
                return -21600;
            case "china":
                return 28800;
            case "dateline":
                return -43200;
            case "e. africa":
                return 10800;
            case "e. australia":
                return 36000;
            case "e. europe":
                return 7200;
            case "e. south america":
                return -10800;
            case "eastern":
                return -18000;
            case "egypt":
                return 7200;
            case "ekaterinburg":
                return 18000;
            case "fiji":
                return 43200;
            case "fle":
                return 7200;
            case "greenland":
                return -10800;
            case "greenwich":
                return 0;
            case "gtb":
                return 7200;
            case "hawaiian":
                return -36000;
            case "india":
                return 19800;
            case "iran":
                return 12600;
            case "jerusalem":
                return 7200;
            case "korea":
                return 32400;
            case "mexico":
                return -21600;
            case "mid-atlantic":
                return -7200;
            case "mountain":
                return -25200;
            case "myanmar":
                return 23400;
            case "n. central asia":
                return 21600;
            case "nepal":
                return 20700;
            case "new zealand":
                return 43200;
            case "newfoundland":
                return -12600;
            case "north asia east":
                return 28800;
            case "north asia":
                return 25200;
            case "pacific sa":
                return -14400;
            case "pacific":
                return -28800;
            case "romance":
                return 3600;
            case "russian":
                return 10800;
            case "sa eastern":
                return -10800;
            case "sa pacific":
                return -18000;
            case "sa western":
                return -14400;
            case "samoa":
                return -39600;
            case "se asia":
                return 25200;
            case "malay peninsula":
                return 28800;
            case "south africa":
                return 7200;
            case "sri lanka":
                return 21600;
            case "taipei":
                return 28800;
            case "tasmania":
                return 36000;
            case "tokyo":
                return 32400;
            case "tonga":
                return 46800;
            case "us eastern":
                return -18000;
            case "us mountain":
                return -25200;
            case "vladivostok":
                return 36000;
            case "w. australia":
                return 28800;
            case "w. central africa":
                return 3600;
            case "w. europe":
                return 3600;
            case "west asia":
                return 18000;
            case "west pacific":
                return 36000;
            case "yakutsk":
                return 32400;
            default:
                return Integer.MIN_VALUE;
        }
    }

    /**
     * Ports date_zone_to_diff from ext/date/date_parse.c in MRI 2.3.1 under BSDL.
     */
    public static int dateZoneToDiff(String zone) {
        String z = zone.toLowerCase();

        final boolean dst;
        if (z.endsWith(" daylight time")) {
            z = z.substring(0, z.length() - " daylight time".length());
            dst = true;
        } else if (z.endsWith(" standard time")) {
            z = z.substring(0, z.length() - " standard time".length());
            dst = false;
        } else if (z.endsWith(" dst")) {
            z = z.substring(0, z.length() - " dst".length());
            dst = true;
        } else {
            dst = false;
        }

        int offsetFromZonesSource;
        if ((offsetFromZonesSource = getOffsetFromZonesSource(z)) != Integer.MIN_VALUE) {
            if (dst) {
                offsetFromZonesSource += 3600;
            }
            return offsetFromZonesSource;
        }

        if (z.startsWith("gmt") || z.startsWith("utc")) {
            z = z.substring(3, z.length()); // remove "gmt" or "utc"
        }

        final boolean sign;
        if (z.charAt(0) == '+') {
            sign = true;
        } else if (z.charAt(0) == '-') {
            sign = false;
        } else {
            // if z doesn't start with "+" or "-", invalid
            return Integer.MIN_VALUE;
        }
        z = z.substring(1);

        int hour = 0, min = 0, sec = 0;
        if (z.contains(":")) {
            final String[] splited = z.split(":");
            if (splited.length == 2) {
                hour = Integer.parseInt(splited[0]);
                min = Integer.parseInt(splited[1]);
            } else {
                hour = Integer.parseInt(splited[0]);
                min = Integer.parseInt(splited[1]);
                sec = Integer.parseInt(splited[2]);
            }

        } else if (z.contains(",") || z.contains(".")) {
            // TODO min = Rational(fr.to_i, 10**fr.size) * 60
            String[] splited = z.split("[\\.,]");
            hour = Integer.parseInt(splited[0]);
            min = (int)(Integer.parseInt(splited[1]) * 60 / Math.pow(10, splited[1].length()));

        } else {
            final int len = z.length();
            if (len % 2 != 0) {
                if (len >= 1) {
                    hour = Integer.parseInt(z.substring(0, 1));
                }
                if (len >= 3) {
                    min = Integer.parseInt(z.substring(1, 3));
                }
                if (len >= 5) {
                    sec = Integer.parseInt(z.substring(3, 5));
                }
            } else {
                if (len >= 2) {
                    hour = Integer.parseInt(z.substring(0, 2));
                }
                if (len >= 4) {
                    min = Integer.parseInt(z.substring(2, 4));
                }
                if (len >= 6) {
                    sec = Integer.parseInt(z.substring(4, 6));
                }
            }
        }

        final int offset = hour * 3600 + min * 60 + sec;
        return sign ? offset : -offset;
    }

    private TimeZoneConverter() {
    }
}
