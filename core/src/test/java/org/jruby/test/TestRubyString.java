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

    private static final String[] UTF8_SAMPLES = {
        "", "ASCII", "\u00e9\u00ff", "\u07ff\u0800\u4e2d", "\ud83d\ude00",
        "\ud800", "\udc00", "a\ud800b", "a\udc00b", "a\ud800", "a\udc00\ud800b"
    };

    public void testUTF8StringEncoding() {
        for (int padding : new int[] { 0, 1023, 1024 }) {
            for (String sample : UTF8_SAMPLES) {
                String input = filler(padding) + sample;
                String msg = utf8Case(sample, padding, input);
                assertUTF8(msg + " newString", input, RubyString.newString(context.runtime, input));
                assertUTF8(msg + " newUTF8String", input, RubyString.newUTF8String(context.runtime, input));
                assertUTF8(msg + " newUnicodeString", input, RubyString.newUnicodeString(context.runtime, input));
                assertBytes(msg + " encodeUTF8", input.getBytes(StandardCharsets.UTF_8), RubyEncoding.encodeUTF8(input));
            }
        }
    }

    public void testUTF8CharSequenceEncoding() {
        for (int padding : new int[] { 0, 1023, 1024, 1025 }) {
            for (String sample : UTF8_SAMPLES) {
                String input = filler(padding) + sample;
                CharBuffer buffer = CharBuffer.wrap("[" + input + "]");
                buffer.position(1);
                buffer.limit(input.length() + 1);
                CharSequence[] sequences = { input, new StringBuilder(input), new StringBuffer(input), buffer,
                        RubyString.newString(context.runtime, input), new DifferentStringSequence(input) };
                for (CharSequence sequence : sequences) {
                    // Bytes must come from the charAt() view, not toString().
                    StringBuilder chars = new StringBuilder(sequence.length());
                    for (int i = 0; i < sequence.length(); i++) chars.append(sequence.charAt(i));
                    String expected = chars.toString();
                    String msg = utf8Case(sample, padding, sequence);

                    assertUTF8(msg + " newString", expected, RubyString.newString(context.runtime, sequence));
                    assertUnconsumed(msg + " newString", buffer, input);
                    assertUTF8(msg + " newUTF8String", expected, RubyString.newUTF8String(context.runtime, sequence));
                    assertUnconsumed(msg + " newUTF8String", buffer, input);
                    assertUTF8(msg + " newUnicodeString", expected, RubyString.newUnicodeString(context.runtime, sequence));
                    assertUnconsumed(msg + " newUnicodeString", buffer, input);
                    assertBytes(msg + " encodeUTF8", expected.getBytes(StandardCharsets.UTF_8), RubyEncoding.encodeUTF8(sequence));
                    assertUnconsumed(msg + " encodeUTF8", buffer, input);
                }
            }
        }
    }

    private static final class DifferentStringSequence implements CharSequence {
        private final String value;

        private DifferentStringSequence(String value) { this.value = value; }
        public int length() { return value.length(); }
        public char charAt(int index) { return value.charAt(index); }
        public CharSequence subSequence(int start, int end) {
            return new DifferentStringSequence(value.substring(start, end));
        }
        public String toString() { return "DifferentStringSequence(" + value + ")"; }
    }

    public void testUTF8UnpairedSurrogateReplacement() {
        String[] inputs = { "a\ud800b", "a\udc00b", "a\ud800", "a\udc00\ud800b", "\ud800\ud800\udc00" };
        String[] expected = { "a?b", "a?b", "a?", "a??b", "?\ud800\udc00" };
        for (int padding : new int[] { 0, 1022, 1023, 1024, 1025 }) {
            String prefix = filler(padding);
            for (int i = 0; i < inputs.length; i++) {
                byte[] bytes = (prefix + expected[i]).getBytes(StandardCharsets.UTF_8);
                for (CharSequence sequence : new CharSequence[] { prefix + inputs[i], new StringBuilder(prefix + inputs[i]) }) {
                    assertBytes(utf8Case(inputs[i], padding, sequence), bytes, RubyEncoding.encodeUTF8(sequence));
                }
            }
        }
    }

    private static String utf8Case(String sample, int padding, Object input) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < sample.length(); i++) {
            char c = sample.charAt(i);
            if (c < 32 || c > 126) escaped.append(String.format("\\u%04x", (int) c));
            else escaped.append(c);
        }
        return "input=[" + escaped + "], padding=" + padding + ", type=" + input.getClass().getSimpleName();
    }

    private static String filler(int length) {
        char[] chars = new char[length];
        Arrays.fill(chars, 'x');
        return new String(chars);
    }

    // Encoding must not move a CharBuffer's position or limit, at any length.
    private static void assertUnconsumed(String msg, CharBuffer buffer, String input) {
        assertEquals(msg + " position", 1, buffer.position());
        assertEquals(msg + " limit", input.length() + 1, buffer.limit());
    }

    private static void assertBytes(String msg, byte[] expected, byte[] actual) {
        assertEquals(msg, Arrays.toString(expected), Arrays.toString(actual));
    }

    public void testUTF8MalformedDecoding() {
        byte[][] samples = {
            {}, { 'a' },
            { (byte) 0xed, (byte) 0xa0, (byte) 0x80 },
            { (byte) 0xc0, (byte) 0xaf },
            { (byte) 0xe2, (byte) 0x82 },
            { (byte) 0xf0, (byte) 0x80, (byte) 0x80, (byte) 0x80 },
            { (byte) 0xf5, (byte) 0xfe, (byte) 0xff }
        };
        String[] expected = { "", "a", "\ufffd", "\ufffd\ufffd", "\ufffd", "\ufffd\ufffd\ufffd\ufffd", "\ufffd\ufffd\ufffd" };
        for (int length : new int[] { 0, 1022, 1023, 1024, 1025, 2090 }) {
            String prefix = filler(length);
            for (int i = 0; i < samples.length; i++) {
                byte[] bytes = new byte[length + samples[i].length + 4];
                Arrays.fill(bytes, (byte) 'x');
                System.arraycopy(samples[i], 0, bytes, length + 2, samples[i].length);
                int size = length + samples[i].length;
                String msg = utf8Case(expected[i], length, bytes) + ", bytes=" + Arrays.toString(samples[i]);
                assertEquals(msg + " decodeUTF8", prefix + expected[i], RubyEncoding.decodeUTF8(bytes, 2, size));
                RubyString string = RubyString.newString(context.runtime,
                        new ByteList(bytes, 2, size, UTF8Encoding.INSTANCE, false));
                assertEquals(msg + " getUnicodeValue", prefix + expected[i], string.getUnicodeValue());
                assertEquals(msg + " toString", prefix + expected[i], string.toString());
            }
        }
    }

    private void assertUTF8(String msg, String input, RubyString actual) {
        byte[] expected = input.getBytes(StandardCharsets.UTF_8);
        assertEquals(msg + " encoding", UTF8Encoding.INSTANCE, actual.getEncoding());
        assertBytes(msg + " bytes", expected, actual.getByteList().bytes());
        boolean ascii = true;
        for (byte value : expected) if (value < 0) ascii = false;
        int scanned = ascii ? CR_7BIT : CR_VALID;
        assertTrue(msg + " code range=" + actual.getCodeRange(),
                actual.getCodeRange() == CR_UNKNOWN || actual.getCodeRange() == scanned);
        assertEquals(msg + " scanned code range", scanned, actual.scanForCodeRange());
        String normalized = new String(expected, StandardCharsets.UTF_8);
        assertEquals(msg + " getUnicodeValue", normalized, actual.getUnicodeValue());
        assertEquals(msg + " toString", normalized, actual.toString());
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
