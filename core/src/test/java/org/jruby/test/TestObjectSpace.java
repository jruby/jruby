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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.jruby.Ruby;
import org.jruby.RubyString;
import org.jruby.api.Create;
import org.jruby.runtime.ObjectSpace;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Access.arrayClass;
import static org.jruby.api.Access.stringClass;
import static org.jruby.api.Create.newArray;
import static org.jruby.api.Create.newString;

/**
* @author Anders
*/
public class TestObjectSpace extends TestCase {

    private ThreadContext context;
    private ObjectSpace target;

    public TestObjectSpace(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
        context = Ruby.newInstance().getCurrentContext();
        target = new ObjectSpace();
    }

    public void testIdentities() {
        RubyString o1 = newString(context, "hey");
        RubyString o2 = newString(context, "ho");

        long id1 = target.createAndRegisterObjectId(o1);
        long id2 = target.createAndRegisterObjectId(o2);

        assertEquals("id of normal objects must be even", 0, id1 % 2);
        assertEquals("id of normal objects must be even", 0, id2 % 2);
        assertTrue("normal ids must be bigger than reserved values", id1 > 4);
        assertTrue("normal ids must be bigger than reserved values", id2 > 4);
        
        assertSame(o1, target.id2ref(id1));
        assertSame(o2, target.id2ref(id2));
        assertNull(target.id2ref(4711));
    }

    public void testObjectSpace() {
        IRubyObject o1 = Create.allocArray(context, 10);
        IRubyObject o2 = Create.allocArray(context, 20);
        IRubyObject o3 = Create.allocArray(context, 30);
        IRubyObject o4 = newString(context, "hello");

        target.add(o1);
        target.add(o2);
        target.add(o3);
        target.add(o4);

        List storedArrays = new ArrayList(3);
        storedArrays.add(o1);
        storedArrays.add(o2);
        storedArrays.add(o3);

        Iterator strings = target.iterator(stringClass(context));
        assertSame(o4, strings.next());
        assertNull(strings.next());

        Iterator array = target.iterator(arrayClass(context));
        for (int i = 0; i < 3; i++) {
            Object item = array.next();
            assertTrue(storedArrays.contains(item));
        }
        assertNull(array.next());
    }
}
