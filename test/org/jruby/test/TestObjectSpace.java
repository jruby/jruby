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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.jruby.IRuby;
import org.jruby.Ruby;
import org.jruby.RubyString;
import org.jruby.runtime.ObjectSpace;
import org.jruby.runtime.builtin.IRubyObject;

/**
* @author Anders
*/
public class TestObjectSpace extends TestCase {

    private IRuby runtime;
    private ObjectSpace target;

    public TestObjectSpace(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
        runtime = Ruby.getDefaultInstance();
        target = new ObjectSpace();
    }

    public void testIdentities() {
        RubyString o1 = runtime.newString("hey");
        RubyString o2 = runtime.newString("ho");

        long id1 = target.idOf(o1);
        long id2 = target.idOf(o2);

        assertEquals("id of normal objects must be even", 0, id1 % 2);
        assertEquals("id of normal objects must be even", 0, id2 % 2);
        assertTrue("normal ids must be bigger than reserved values", id1 > 4);
        assertTrue("normal ids must be bigger than reserved values", id2 > 4);
        
        assertSame(o1, target.id2ref(id1));
        assertSame(o2, target.id2ref(id2));
        assertNull(target.id2ref(4711));
    }

    public void testObjectSpace() {
        IRubyObject o1 = runtime.newFixnum(10);
        IRubyObject o2 = runtime.newFixnum(20);
        IRubyObject o3 = runtime.newFixnum(30);
        IRubyObject o4 = runtime.newString("hello");

        target.add(o1);
        target.add(o2);
        target.add(o3);
        target.add(o4);

        List storedFixnums = new ArrayList(3);
        storedFixnums.add(o1);
        storedFixnums.add(o2);
        storedFixnums.add(o3);

        Iterator strings = target.iterator(runtime.getString());
        assertSame(o4, strings.next());
        assertNull(strings.next());

        Iterator numerics = target.iterator(runtime.getClass("Numeric"));
        for (int i = 0; i < 3; i++) {
            Object item = numerics.next();
            assertTrue(storedFixnums.contains(item));
        }
        assertNull(numerics.next());
    }
}
