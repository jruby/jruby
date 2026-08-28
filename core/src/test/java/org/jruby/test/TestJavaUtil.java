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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.test;

import junit.framework.TestCase;

import org.jruby.Ruby;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.ThreadContext;

/**
 * @author jpetersen
 */
public class TestJavaUtil extends TestCase {
    private ThreadContext context;

    public TestJavaUtil(String name) {
        super(name);
    }

    public void setUp() {
        context = Ruby.newInstance().getCurrentContext();
    }

    public void testConvertJavaToRuby() {
        assertEquals(JavaUtil.convertJavaToRuby(context.runtime, null).getType().name(context).toString(), "NilClass");
        assertEquals(JavaUtil.convertJavaToRuby(context.runtime, new Integer(1000)).getType().name(context).toString(), "Integer");
        assertEquals(JavaUtil.convertJavaToRuby(context.runtime, new Double(1.0)).getType().name(context).toString(), "Float");
        assertEquals(JavaUtil.convertJavaToRuby(context.runtime, Boolean.TRUE).getType().name(context).toString(), "TrueClass");
        assertEquals(JavaUtil.convertJavaToRuby(context.runtime, Boolean.FALSE).getType().name(context).toString(), "FalseClass");
        assertEquals(JavaUtil.convertJavaToRuby(context.runtime, "AString").getType().name(context).toString(), "String");
    }
}
