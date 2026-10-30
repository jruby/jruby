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

import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.jcodings.specific.UTF8Encoding;
import org.jruby.RubyArray;
import org.jruby.RubyEncoding;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.exceptions.RaiseException;
import org.jruby.util.ByteList;

import static org.jruby.api.Create.newString;
import static org.jruby.util.StringSupport.CR_7BIT;
import static org.jruby.util.StringSupport.CR_UNKNOWN;
import static org.jruby.util.StringSupport.CR_VALID;

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

    public void testUTF8StringEncoding() {
        for (String input : utf8Inputs()) {
            assertUTF8(input, RubyString.newString(context.runtime, input));
            assertUTF8(input, RubyString.newUTF8String(context.runtime, input));
            assertUTF8(input, RubyString.newUnicodeString(context.runtime, input));
            assertTrue(Arrays.equals(input.getBytes(StandardCharsets.UTF_8), RubyEncoding.encodeUTF8(input)));
        }
    }

    public void testUTF8CharSequenceEncoding() {
        for (String input : utf8Inputs()) {
            CharBuffer buffer = CharBuffer.wrap("[" + input + "]");
            buffer.position(1);
            buffer.limit(input.length() + 1);
            CharSequence[] sequences = { input, new StringBuilder(input), buffer };
            for (CharSequence sequence : sequences) {
                assertUTF8(input, RubyString.newString(context.runtime, sequence));
                buffer.position(1);
                assertUTF8(input, RubyString.newUTF8String(context.runtime, sequence));
                buffer.position(1);
                assertUTF8(input, RubyString.newUnicodeString(context.runtime, sequence));
                buffer.position(1);
                assertTrue(Arrays.equals(input.getBytes(StandardCharsets.UTF_8), RubyEncoding.encodeUTF8(sequence)));
                buffer.position(1);
            }
            CharSequence rubyString = RubyString.newString(context.runtime, input);
            StringBuilder chars = new StringBuilder();
            for (int i = 0; i < rubyString.length(); i++) chars.append(rubyString.charAt(i));
            assertUTF8(chars.toString(), RubyString.newString(context.runtime, rubyString));
            assertTrue(Arrays.equals(chars.toString().getBytes(StandardCharsets.UTF_8), RubyEncoding.encodeUTF8(rubyString)));
        }
    }

    public void testUTF8UnpairedSurrogateReplacement() {
        String[] inputs = { "a\ud800b", "a\udc00b", "a\ud800", "a\udc00\ud800b", "\ud800\ud800\udc00" };
        String[] expected = { "a?b", "a?b", "a?", "a??b", "?\ud800\udc00" };
        for (int length : new int[] { 0, 1022, 1023, 1024, 1025 }) {
            char[] padding = new char[length];
            Arrays.fill(padding, 'x');
            String prefix = new String(padding);
            for (int i = 0; i < inputs.length; i++) {
                byte[] bytes = (prefix + expected[i]).getBytes(StandardCharsets.UTF_8);
                assertTrue(Arrays.equals(bytes, RubyEncoding.encodeUTF8(prefix + inputs[i])));
                assertTrue(Arrays.equals(bytes, RubyEncoding.encodeUTF8(new StringBuilder(prefix + inputs[i]))));
            }
        }
    }

    private String[] utf8Inputs() {
        String[] samples = { "", "ASCII", "\u00e9\u00ff", "\u07ff\u0800\u4e2d", "\ud83d\ude00", "\ud800", "\udc00", "a\ud800b", "a\udc00b", "a\ud800", "a\udc00\ud800b" };
        String[] inputs = new String[samples.length * 3];
        char[] padding = new char[1024];
        Arrays.fill(padding, 'x');
        for (int i = 0; i < samples.length; i++) {
            inputs[i] = samples[i];
            inputs[i + samples.length] = new String(padding, 0, 1023) + samples[i];
            inputs[i + samples.length * 2] = new String(padding) + samples[i];
        }
        return inputs;
    }

    private void assertUTF8(String input, RubyString actual) {
        byte[] expected = input.getBytes(StandardCharsets.UTF_8);
        assertEquals(UTF8Encoding.INSTANCE, actual.getEncoding());
        assertEquals(CR_UNKNOWN, actual.getCodeRange());
        assertTrue(Arrays.equals(expected, actual.getByteList().bytes()));
        boolean ascii = true;
        for (byte value : expected) if (value < 0) ascii = false;
        assertEquals(ascii ? CR_7BIT : CR_VALID, actual.scanForCodeRange());
        String normalized = new String(expected, StandardCharsets.UTF_8);
        assertEquals(normalized, actual.getUnicodeValue());
        assertEquals(normalized, actual.toString());
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
