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

import org.jruby.ext.posix.JavaFileStatTest;
import org.jruby.javasupport.TestJava;
import org.jruby.javasupport.TestJavaClass;
import org.jruby.javasupport.test.JavaSupportTestSuite;
import org.jruby.runtime.EventHookTest;
import org.jruby.util.JRubyThreadContextTest;
import org.jruby.util.PlatformTest;
import org.jruby.util.ShellLauncherTest;
import org.jruby.util.TimeOutputFormatterTest;

/**
 *
 * @author chadfowler
 */
public class MainTestSuite extends TestSuite {

    public static Test suite() throws Throwable {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(TestLoadService.class);
        suite.addTestSuite(TestRubyInstanceConfig.class);
        suite.addTestSuite(TestRubyObject.class);
        suite.addTestSuite(TestRubyNil.class);
        suite.addTestSuite(TestRubyHash.class);
        suite.addTestSuite(TestRuby.class);
        suite.addTestSuite(TestRequire.class);
        suite.addTestSuite(TestJavaUtil.class);
        suite.addTestSuite(TestJava.class);
        suite.addTestSuite(TestJavaClass.class);
        suite.addTestSuite(TestKernel.class);
        suite.addTestSuite(TestRubyCollect.class);
        suite.addTestSuite(TestObjectSpace.class);
        suite.addTestSuite(TestRubySymbol.class);
        suite.addTest(JavaSupportTestSuite.suite());
        suite.addTestSuite(TestCommandlineParser.class);
        suite.addTestSuite(TestRubyException.class);
        suite.addTestSuite(TestAdoptedThreading.class);
        suite.addTestSuite(TestRubyArray.class);
        suite.addTestSuite(TestRaiseException.class);
        suite.addTestSuite(PlatformTest.class);
        suite.addTestSuite(ShellLauncherTest.class);
        suite.addTestSuite(TestRbConfigLibrary.class);
        suite.addTestSuite(TestParser.class);
        suite.addTestSuite(TestRubyBigDecimal.class);
        suite.addTestSuite(JRubyThreadContextTest.class);
        suite.addTestSuite(JavaFileStatTest.class);
        suite.addTestSuite(TestCodeCache.class);
        suite.addTestSuite(TestJavaReentrantExceptions.class);
        suite.addTestSuite(EventHookTest.class);
        suite.addTestSuite(TestMethodFactories.class);
        suite.addTestSuite(TimeOutputFormatterTest.class);
        return suite;
    }
}
