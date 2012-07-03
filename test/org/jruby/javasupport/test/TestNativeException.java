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
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
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
package org.jruby.javasupport.test; 
 
import org.jruby.Ruby;
import org.jruby.test.TestRubyBase;
 
public class TestNativeException extends TestRubyBase {

    public TestNativeException(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        runtime = Ruby.newInstance();
    }

    public void testCauseIsProxied() throws Exception {
        String result = eval("$-w = nil; require 'java'\n" +
                "java_import('java.io.File') { 'JFile' }\n" +
                "begin\n" +
                "  JFile.new(nil)\n" +
                "rescue NativeException => e\n" +
                "end\n" +
                "p e.cause.respond_to?(:print_stack_trace)");
        assertEquals("Bug: [ JRUBY-106 ]", "true", result);
    }
}
