/*
 * MainTestSuite.java - No description
 * Created on 11.01.2002, 12:50:46
 *
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina, Chad Fowler
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Chad Fowler <chadfowler@yahoo.com>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

package org.jruby.test;

import junit.framework.TestSuite;
import junit.framework.Test;
import org.jruby.javasupport.test.JavaSupportTestSuite;

/**
 *
 * @author chadfowler
 * @version $Revision$
 */
public class MainTestSuite extends TestSuite {

    public static Test suite() throws Throwable {
        TestSuite suite = new TestSuite();
        suite.addTest(new TestSuite(TestRubyObject.class));
        suite.addTest(new TestSuite(TestRubyNil.class));
        suite.addTest(new TestSuite(TestRubyHash.class));
        suite.addTest(new TestSuite(TestRubyTime.class));
        suite.addTest(new TestSuite(TestRuby.class));
        suite.addTest(new TestSuite(TestJavaUtil.class));
        suite.addTest(new TestSuite(TestKernel.class));
        suite.addTest(new TestSuite(TestRubyCollect.class));
        suite.addTest(new TestSuite(TestObjectSpace.class));
        suite.addTest(ScriptTestSuite.suite());
        suite.addTest(new TestSuite(TestRubySymbol.class));
        suite.addTest(JavaSupportTestSuite.suite());
        suite.addTest(new TestSuite(TestIdentitySet.class));
        return suite;
    }
}
