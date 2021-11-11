/*
 ***** BEGIN LICENSE BLOCK *****
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
/*
 * $Id: $
 */
package org.jruby.util;

import org.jruby.util.RubyDateFormatter.FieldType;

import junit.framework.TestCase;

import static org.jruby.util.CommonByteLists.*;

public class RubyTimeOutputFormatterTest extends TestCase {
    public static final ByteList CARET_UNDERSCORE = new ByteList(new byte[] {'^', '_'});
    public static final ByteList MINUS_ZERO = new ByteList(new byte[] {'-', '0'});
    public static final ByteList UNDERSCORE_ZERO = new ByteList(new byte[] {'_', '0'});
    public static final ByteList ZERO_UNDERSCORE = new ByteList(new byte[] {'0', '_'});


    private RubyTimeOutputFormatter getFormatter(ByteList flags, int width) {
        return new RubyTimeOutputFormatter(flags, width);
    }

    private String format(RubyTimeOutputFormatter formatter, CharSequence sequence, FieldType type) {
        ByteList out = new ByteList();
        formatter.format(out, sequence, type);
        return out.toString();
    }

    private String format(RubyTimeOutputFormatter formatter, long value, FieldType type) {
        ByteList out = new ByteList();
        formatter.format(out, value, type);
        return out.toString();
    }

    public void testFormatUpperCase() {
        RubyTimeOutputFormatter formatter = getFormatter(CARET, 0);
        assertEquals("UP", format(formatter, "up", FieldType.TEXT));
    }

    public void testFormatPaddingBlank() {
        RubyTimeOutputFormatter formatter = getFormatter(UNDERSCORE, 5);
        assertEquals("   up", format(formatter, "up", FieldType.TEXT));
    }

    public void testFormatPaddingZeros() {
        RubyTimeOutputFormatter formatter = getFormatter(ZERO, 5);
        assertEquals("000up", format(formatter, "up", FieldType.TEXT));
    }

    public void testFormatNoPadding() {
        RubyTimeOutputFormatter formatter = getFormatter(DASH, 5);
        assertEquals("up", format(formatter, "up", FieldType.TEXT));
    }

    public void testFormatPaddingBlankAndUpperCase() {
        RubyTimeOutputFormatter formatter = getFormatter(CARET_UNDERSCORE, 5);
        assertEquals("   UP", format(formatter, "up", FieldType.TEXT));
    }

    public void testPaddingWithoutFormat() {
        RubyTimeOutputFormatter formatter = getFormatter(ByteList.EMPTY_BYTELIST, 5);
        assertEquals("   up", format(formatter, "up", FieldType.TEXT));
    }

    public void testPaddingZeroFirstOption() {
        RubyTimeOutputFormatter formatter = getFormatter(ZERO_UNDERSCORE, 5);
        assertEquals("   up", format(formatter, "up", FieldType.TEXT));
    }

    public void testPaddingBlankFirstOption() {
        RubyTimeOutputFormatter formatter = getFormatter(UNDERSCORE_ZERO, 5);
        assertEquals("000up", format(formatter, "up", FieldType.TEXT));
    }

    public void testPaddingWithUpperCase() {
        RubyTimeOutputFormatter formatter = getFormatter(CARET, 5);
        assertEquals("   UP", format(formatter, "up", FieldType.TEXT));
    }

    public void testFormatNoPaddingForBlankPaddedNumericValues() {
        RubyTimeOutputFormatter formatter = getFormatter(DASH, 3);
        assertEquals("42", format(formatter, 42, FieldType.NUMERIC3));
    }

    public void testFormatNoPaddingForZeroPaddedNumericValues() {
        RubyTimeOutputFormatter formatter = getFormatter(MINUS_ZERO, 3);
        assertEquals("42", format(formatter, 42, FieldType.NUMERIC3));
    }
}
