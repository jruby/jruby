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

import org.jruby.*;
import org.junit.Test;

import java.math.BigInteger;

import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.asFloat;
import static org.junit.Assert.*;

public class TestRubyFloat extends junit.framework.TestCase {

    private Ruby runtime = Ruby.newInstance();

    @Test
    public void testConvertToInteger() {
        var context = runtime.getCurrentContext();
        RubyFloat r = asFloat(context, 0);
        assertEquals(r.convertToInteger(), asFixnum(context, 0));
        r = asFloat(context, 12.81);
        assertEquals(r.convertToInteger(), asFixnum(context, 12));
        r = asFloat(context, -100.9);
        assertEquals(r.convertToInteger(), asFixnum(context, -100));
        r = asFloat(context, 1000000000000000.5d);
        assertEquals(r.convertToInteger(), asFixnum(context, 1000000000000000l));
        r = asFloat(context, 10000000000000000000.9d);
        assertEquals(r.convertToInteger(), RubyBignum.newBignum(runtime, "10000000000000000000"));
    }

    @Test
    public void testToBigInteger() {
        var context = runtime.getCurrentContext();
        RubyFloat r = asFloat(context, 0);
        assertEquals(r.asBigInteger(context), BigInteger.ZERO);
        r = asFloat(context, 123456.789);
        assertEquals(r.asBigInteger(context), BigInteger.valueOf(123456));
        r = asFloat(context, -10000000000000.99d);
        assertEquals(r.asBigInteger(context), BigInteger.valueOf(-10000000000000l));
        r = asFloat(context, 10000000000000000000.9d);
        assertEquals(r.asBigInteger(context), new BigInteger("10000000000000000000"));
    }

}
