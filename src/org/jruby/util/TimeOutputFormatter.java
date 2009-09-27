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
package org.jruby.util;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Support for GNU-C output formattters, see: http://www.gnu.org/software/libc/manual/html_node/Formatting-Calendar-Time.html
 */
public class TimeOutputFormatter {
    private final String formatter;
    private final int totalPadding;

    private static final String formatPattern = "%([\\^0_-]+)?(\\d+)?.+";
    private static final Pattern regexp = Pattern.compile(formatPattern);

    public TimeOutputFormatter(String formatter, int totalPadding) {
        this.formatter = formatter;
        this.totalPadding = totalPadding;
    }

    public static TimeOutputFormatter getFormatter(String pattern) {
        Matcher matcher = regexp.matcher(pattern);
        if (matcher.matches() && (matcher.group(1) != null || matcher.group(2) != null)) {
            String formatter = matcher.group(1);
            int totalPadding = matcher.group(2) != null ? Integer.valueOf(matcher.group(2)) : 0;

            return new TimeOutputFormatter(formatter, totalPadding);
        }
        return null;
    }

    public String getFormatter() {
        return (formatter != null ? formatter : "") + (totalPadding > 0 ? totalPadding : "");
    }

    public String format(String sequence) {
        char paddedWith = ' ';
        if (formatter != null) {
            for (int i = 0; i < formatter.length(); i++) {
                switch (formatter.charAt(i)) {
                    case '^':
                        sequence = sequence.toUpperCase();
                        break;
                    case '_':
                        paddedWith = ' ';
                        break;
                    case '0':
                        paddedWith = '0';
                        break;
                }
            }
        }
        if (totalPadding > 0) {
            sequence = padding(sequence, paddedWith);
        }
        return sequence;
    }

    private String padding(String sequence, char padder) {
        if (formatter != null && formatter.contains("-")) return sequence;

        if (sequence != null && sequence.length() < totalPadding) {
            StringBuilder seqBuf = new StringBuilder(totalPadding);
            for (int i = sequence.length(); i < totalPadding; i++) {
                seqBuf.append(padder);
            }
            seqBuf.append(sequence);
            return seqBuf.toString();
        }
        return sequence;
    }
}
