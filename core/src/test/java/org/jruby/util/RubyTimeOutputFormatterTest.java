/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
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
 *
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
/**
 * $Id: $
 */
package org.jruby.util;

import org.jruby.util.RubyDateFormatter.FieldType;

import junit.framework.TestCase;

public class RubyTimeOutputFormatterTest extends TestCase {
    private RubyTimeOutputFormatter getFormatter(String flags, int width) {
        return new RubyTimeOutputFormatter(flags, width);
    }

    public void testFormatUpperCase() {
        RubyTimeOutputFormatter formatter = getFormatter("^", 0);
        assertEquals("UP", formatter.format("up", 0, FieldType.TEXT));
    }

    public void testFormatPaddingBlank() {
        RubyTimeOutputFormatter formatter = getFormatter("_", 5);
        assertEquals("   up", formatter.format("up", 0, FieldType.TEXT));
    }

    public void testFormatPaddingZeros() {
        RubyTimeOutputFormatter formatter = getFormatter("0", 5);
        assertEquals("000up", formatter.format("up", 0, FieldType.TEXT));
    }

    public void testFormatNoPadding() {
        RubyTimeOutputFormatter formatter = getFormatter("-", 5);
        assertEquals("up", formatter.format("up", 0, FieldType.TEXT));
    }

    public void testFormatPaddingBlankAndUpperCase() {
        RubyTimeOutputFormatter formatter = getFormatter("^_", 5);
        assertEquals("   UP", formatter.format("up", 0, FieldType.TEXT));
    }

    public void testPaddingWithoutFormat() {
        RubyTimeOutputFormatter formatter = getFormatter("", 5);
        assertEquals("   up", formatter.format("up", 0, FieldType.TEXT));
    }

    public void testPaddingZeroFirstOption() {
        RubyTimeOutputFormatter formatter = getFormatter("0_", 5);
        assertEquals("   up", formatter.format("up", 0, FieldType.TEXT));
    }

    public void testPaddingBlankFirstOption() {
        RubyTimeOutputFormatter formatter = getFormatter("_0", 5);
        assertEquals("000up", formatter.format("up", 0, FieldType.TEXT));
    }

    public void testPaddingWithUpperCase() {
        RubyTimeOutputFormatter formatter = getFormatter("^", 5);
        assertEquals("   UP", formatter.format("up", 0, FieldType.TEXT));
    }

    public void testFormatNoPaddingForBlankPaddedNumericValues() {
        RubyTimeOutputFormatter formatter = getFormatter("-", 3);
        assertEquals("42", formatter.format(null, 42, FieldType.NUMERIC3));
    }

    public void testFormatNoPaddingForZeroPaddedNumericValues() {
        RubyTimeOutputFormatter formatter = getFormatter("-0", 3);
        assertEquals("42", formatter.format(null, 42, FieldType.NUMERIC3));
    }
}
