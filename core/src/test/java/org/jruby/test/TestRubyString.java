/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Copyright (C) 2006 Ola Bini <Ola.Bini@ki.se>
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

import org.jcodings.specific.UTF8Encoding;
import org.jruby.RubyArray;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.exceptions.RaiseException;
import org.jruby.util.ByteList;

import static org.jruby.api.Create.newString;

/**
 * Test case for functionality in RubyArray
 */
public class TestRubyString extends Base {
    public TestRubyString(final String name) {
        super(name);
    }

    /**
     * JRUBY-5646: RubyString.newUnicodeString in 1.9 mode produces ASCII-8BIT
     */
    public void testNewUnicodeString() throws Exception {
        RubyString str = RubyString.newUnicodeString(context.runtime, "hello");
        assertEquals(UTF8Encoding.INSTANCE, str.getByteList().getEncoding());
    }

    public void testSplit() throws RaiseException {
        RubyString str = newString(context, "JRuby is so awesome!");
        RubyArray res = str.split(context, newString(context, " "), 0);
        assertEquals(4, res.size());
        assertEquals("JRuby", res.get(0));
        res = str.split(newString(context, " "), 2);
        assertEquals(2, res.size());
        assertEquals("JRuby", res.get(0));
        assertEquals("is so awesome!", res.get(1));

        RubyRegexp pat = RubyRegexp.newRegexp(context.runtime, ByteList.create("[ie]s"));
        res = str.split(context, pat, 0);
        assertEquals(3, res.size());
        assertEquals("JRuby ", res.get(0));
        assertEquals(" so aw", res.get(1));
        assertEquals("ome!", res.get(2));
        res = str.split(context, pat, 4);
        assertEquals(3, res.size());
    }

    // See https://github.com/jruby/jruby/pull/9145
    public void testDefensiveFString() throws Throwable {
        byte[] bytes = ByteList.plain("foo9145");
        RubyString goodString = newString(context, bytes);
        RubyString badString = newString(context, bytes);
        ByteList badBytes = badString.getByteList();

        RubyString fstring = context.runtime.freezeAndDedupString(badString);
        badBytes.set(0, 'b');

        // previously returned fstring should not have been modified
        assertEquals(goodString, fstring);
        assertNotSame(fstring.getByteList(), badBytes);
        assertNotSame(fstring.getByteList().unsafeBytes(), badBytes.unsafeBytes());
    }
}
