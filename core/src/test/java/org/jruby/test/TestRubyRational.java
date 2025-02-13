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

package org.jruby.test;

import org.jruby.Ruby;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyRational;
import org.jruby.runtime.ThreadContext;
import org.junit.Test;

import static org.jruby.api.Convert.asFixnum;
import static org.junit.Assert.*;

public class TestRubyRational extends junit.framework.TestCase {

    private Ruby runtime = Ruby.newInstance();

    @Test // JRUBY-5941
    public void testRationalToDouble() {
        var context = runtime.getCurrentContext();
        RubyRational rational = newRational(context, 1, 1000);
        double toDouble = rational.asDouble(context);
        double expected = ((RubyFloat)rational.to_f(context)).asDouble(context);
        assertEquals(expected, toDouble, 0);
    }

    @Test
    public void testRationalSignum() {
        var context = runtime.getCurrentContext();
        assertEquals(newRational(context, 1, 1000).signum(context), +1);
        assertEquals(newRational(context, 1, -100).signum(context), -1);
        assertEquals(newRational(context, -1, 100).signum(context), -1);
        assertEquals(newRational(context, -1, -10).signum(context), +1);
        assertEquals(newRational(context, 0, 1000).signum(context),  0);
        assertEquals(newRational(context, 0, -100).signum(context),  0);
    }

    @Test
    public void testConvertToInteger() {
        var context = runtime.getCurrentContext();
        assertEquals(newRational(context, 11, 1).asInt(context), 11);
        assertEquals(newRational(context, -12, 2).asInt(context), -6);
        assertEquals(newRational(context, 10, 20).asInt(context), 0);
        assertEquals(newRational(context, 0, 5).asInt(context), 0);
        assertEquals(newRational(context, 5, -2).asInt(context), -2);
        assertEquals(newRational(context, 13, 7).asInt(context), 1);
    }

    private RubyRational newRational(ThreadContext context, final long num, final long den) {
        return (RubyRational) RubyRational.newInstance(context, asFixnum(context, num), asFixnum(context, den));
    }

}
