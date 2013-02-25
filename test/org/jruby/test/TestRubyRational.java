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

import java.util.ArrayList;

import junit.framework.TestCase;
import org.jruby.CompatVersion;

import org.jruby.Ruby;
import org.jruby.RubyFloat;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyRational;
import org.jruby.RubySymbol;

public class TestRubyRational extends TestCase {
    private Ruby runtime;

    public TestRubyRational(String name) {
	super(name);
    }

    public void setUp() {
        runtime = Ruby.newInstance(new RubyInstanceConfig() {
            {
                setCompatVersion(CompatVersion.RUBY1_9);
            }
        });
    }

    // JRUBY-5941
    public void testRationalToDouble() throws Exception {
        RubyRational rational = RubyRational.newRational(runtime, 1, 1000);
        double toDouble = rational.getDoubleValue();
        double expected = ((RubyFloat)rational.to_f(runtime.getCurrentContext())).getDoubleValue();
        assertEquals(expected, toDouble);
    }
}
