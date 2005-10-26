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
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
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

import junit.framework.TestCase;

import org.jruby.IRuby;
import org.jruby.Ruby;
import org.jruby.javasupport.JavaUtil;

/**
 * @author jpetersen
 */
public class TestJavaUtil extends TestCase {
    private IRuby runtime;

    public TestJavaUtil(String name) {
        super(name);
    }

    public void setUp() {
        runtime = Ruby.getDefaultInstance();
    }

    public void testConvertJavaToRuby() {
        assertEquals(JavaUtil.convertJavaToRuby(runtime, null).getType().name().toString(), "NilClass");
        assertEquals(JavaUtil.convertJavaToRuby(runtime, new Integer(1000)).getType().name().toString(), "Fixnum");
        assertEquals(JavaUtil.convertJavaToRuby(runtime, new Double(1.0)).getType().name().toString(), "Float");
        assertEquals(JavaUtil.convertJavaToRuby(runtime, Boolean.TRUE).getType().name().toString(), "TrueClass");
        assertEquals(JavaUtil.convertJavaToRuby(runtime, Boolean.FALSE).getType().name().toString(), "FalseClass");
        assertEquals(JavaUtil.convertJavaToRuby(runtime, "AString").getType().name().toString(), "String");
    }
}
