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
 *
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
/**
 * $Id: $
 */
package org.jruby.util;

import junit.framework.TestCase;

public class TimeOutputFormatterTest extends TestCase {

    public void testGetFormatterFails() {
        assertNull(TimeOutputFormatter.getFormatter("%I"));
    }

    public void testGetFormatterPasses() {
        TimeOutputFormatter formatter = TimeOutputFormatter.getFormatter("%^05H");
        assertNotNull(formatter);
        assertEquals("^05", formatter.getFormatter());
    }

    public void testFormatUpperCase() {
        TimeOutputFormatter formatter = TimeOutputFormatter.getFormatter("%^H");
        assertEquals("UP", formatter.format("up"));
    }

    public void testFormatPaddingBlank() {
        TimeOutputFormatter formatter = TimeOutputFormatter.getFormatter("%_5H");
        assertEquals("   up", formatter.format("up"));
    }

    public void testFormatPaddingZeros() {
        TimeOutputFormatter formatter = TimeOutputFormatter.getFormatter("%05H");
        assertEquals("000up", formatter.format("up"));
    }

    public void testFormatNoPadding() {
        TimeOutputFormatter formatter = TimeOutputFormatter.getFormatter("%-5H");
        assertEquals("up", formatter.format("up"));
    }

    public void testFormatPaddingBlankAndUpperCase() {
        TimeOutputFormatter formatter = TimeOutputFormatter.getFormatter("%^_5H");
        assertEquals("   UP", formatter.format("up"));
    }

    public void testPaddingWithoutFormat() {
        TimeOutputFormatter formatter = TimeOutputFormatter.getFormatter("%5H");
        assertEquals("   up", formatter.format("up"));
    }

    public void testPaddingZeroFirstOption() {
        TimeOutputFormatter formatter = TimeOutputFormatter.getFormatter("%0_5H");
        assertEquals("   up", formatter.format("up"));
    }

    public void testPaddingBlankFirstOption() {
        TimeOutputFormatter formatter = TimeOutputFormatter.getFormatter("%_05H");
        assertEquals("000up", formatter.format("up"));
    }

    public void testPaddingWithUpperCase() {
        TimeOutputFormatter formatter = TimeOutputFormatter.getFormatter("%^5H");
        assertEquals("   UP", formatter.format("up"));
    }

    public void testFormatNoPaddingForBlankPaddedValues() {
       TimeOutputFormatter formatter = TimeOutputFormatter.getFormatter("%-3H");
        assertEquals("up", formatter.format(" up"));
    }

   public void testFormatNoPaddingForZeroPaddedValues() {
       TimeOutputFormatter formatter = TimeOutputFormatter.getFormatter("%-3H");
        assertEquals("up", formatter.format("0up"));
    }
}
