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
 * Copyright (C) 2001 Chad Fowler <chadfowler@chadfowler.com>
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002 uid41545 <uid41545@users.sourceforge.net>
 * Copyright (C) 2002 Don Schwartz <schwardo@users.sourceforge.net>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
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
package org.jruby.test;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.jruby.TestRegexpTranslator;
import org.jruby.javasupport.TestJavaClass;
import org.jruby.javasupport.test.JavaSupportTestSuite;
import org.jruby.runtime.callback.TestReflectionCallback;

/**
 *
 * @author chadfowler
 */
public class MainTestSuite extends TestSuite {

    public static Test suite() throws Throwable {
        TestSuite suite = new TestSuite();
        suite.addTest(new TestSuite(TestRubyObject.class));
        suite.addTest(new TestSuite(TestRubyNil.class));
        suite.addTest(new TestSuite(TestRubyHash.class));
        suite.addTest(new TestSuite(TestRuby.class));
        suite.addTest(new TestSuite(TestRequire.class));
        suite.addTest(new TestSuite(TestJavaUtil.class));
        suite.addTestSuite(TestJavaClass.class);
        suite.addTest(new TestSuite(TestKernel.class));
        suite.addTest(new TestSuite(TestKernel.class));
        suite.addTest(new TestSuite(TestRubyCollect.class));
        suite.addTest(new TestSuite(TestObjectSpace.class));
        suite.addTest(ScriptTestSuite.suite());
        suite.addTest(new TestSuite(TestRubySymbol.class));
        suite.addTest(JavaSupportTestSuite.suite());
        suite.addTest(new TestSuite(TestIdentitySet.class));
        suite.addTest(new TestSuite(TestCommandlineParser.class));
        suite.addTest(new TestSuite(TestRubyException.class));
        suite.addTest(new TestSuite(TestReflectionCallback.class));
        suite.addTest(new TestSuite(TestRegexpTranslator.class));
        suite.addTest(new TestSuite(TestAdoptedThreading.class));
        suite.addTest(new TestSuite(TestRubyArray.class));
        return suite;
    }
}
