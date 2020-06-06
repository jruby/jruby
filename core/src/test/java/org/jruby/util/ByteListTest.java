/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.

 * Copyright (C) 2007 Nick Sieger <nicksieger@gmail.com>
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

package org.jruby.util;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import org.jcodings.specific.UTF8Encoding;
import org.jcodings.specific.ASCIIEncoding;

import junit.framework.TestCase;

public class ByteListTest extends TestCase {
    public void testEmptyByteListHasZerolength() {
        assertEquals(0, new ByteList().length());
        assertEquals(0, new ByteList(32).length());
    }

    public void testByteListWithInitialByteArray() {
        byte[] bytes = new byte[] {0x0f, 0x01, 0x02, 0x03, 0x04, 0x05};

        ByteList b = new ByteList(bytes);
        assertEquals(bytes.length, b.length());
        b.set(0, 0x00);
        assertEquals(0x0f, bytes[0]);
        assertEquals(0x00, b.get(0));

        ByteList b2 = new ByteList(b);
        assertEquals(b.length(), b2.length());
        b2.set(0, 0x0f);
        assertEquals(0x0f, b2.get(0));
        assertEquals(0x00, b.get(0));
    }

    public void testByteListWithSubrangeOfInitialBytes() {
        byte[] bytes = new byte[] {0x0f, 0x01, 0x02, 0x03, 0x04, 0x05};
        ByteList b = new ByteList(bytes, 2, 2);
        assertEquals(2, b.length());
        assertEquals(0x02, b.get(0));
        assertEquals(0x03, b.get(1));

        ByteList b2 = new ByteList(b, 1, 1);
        assertEquals(1, b2.length());
        assertEquals(0x03, b2.get(0));
    }

    public void testByteListAppendSingleByte() {
        byte[] bytes = new byte[] {0x0f, 0x01, 0x02, 0x03, 0x04, 0x05};
        ByteList b = new ByteList(1);

        for (int i = 0; i < bytes.length; i++) {
            b.append(bytes[i]);
        }

        assertEquals(new ByteList(bytes), b);
    }

    public void testByteListAppendSingleIntTruncates() {
        int[] ints = new int[] { 0x1001, 0x1002, 0x1003 };

        ByteList b = new ByteList(1);
        for (int i = 0; i < ints.length; i++) {
            b.append(ints[i]);
        }

        assertEquals(0x01, b.get(0));
        assertEquals(0x02, b.get(1));
        assertEquals(0x03, b.get(2));
    }

    public void testByteListPrependSingleByte() {
        byte[] bytes = new byte[] {0x01, 0x02, 0x03};
        ByteList b = new ByteList(1);

        for (int i = 0; i < bytes.length; i++) {
            b.prepend(bytes[i]);
        }

        assertEquals(0x03, b.get(0));
        assertEquals(0x02, b.get(1));
        assertEquals(0x01, b.get(2));

        bytes = new byte[] {0x0f, 0x01, 0x02, 0x03, 0x04, 0x05};
        b = new ByteList(bytes);
        b.prepend((byte) 0x0e);
        assertEquals(7, b.length());
        assertEquals(0x0e, b.get(0));
        assertEquals(0x05, b.get(6));
    }

    public void testByteListAppendArray() {
        ByteList b = new ByteList(1);

        b.append(new byte[] {0x01, 0x02, 0x03});

        assertEquals(3, b.length());
        assertEquals(0x01, b.get(0));
        assertEquals(0x02, b.get(1));
        assertEquals(0x03, b.get(2));
    }

    public void testByteListAppendArrayIndexLength() {
        ByteList b = new ByteList(1);

        b.append(new byte[] {0x0f, 0x01, 0x02, 0x03, 0x04, 0x05}, 1, 4);
        assertEquals(4, b.length());
        assertEquals(0x01, b.get(0));
        assertEquals(0x02, b.get(1));
        assertEquals(0x03, b.get(2));
        assertEquals(0x04, b.get(3));
    }

    public void testByteListAppendSingleByteWithSubrangeOfInitialBytes() {
        byte[] bytes = new byte[] {6,7,8,9,10};
        ByteList b = new ByteList(new byte[] {0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 127}, 6, 4, false);

        for (int i = 0; i < bytes.length; i++) {
            b.append(bytes[i]);
        }

        assertEquals(9, b.length());
        assertEquals(2, b.get(0));
        assertEquals(3, b.get(1));
        assertEquals(4, b.get(2));
        assertEquals(5, b.get(3));
        assertEquals(6, b.get(4));
        assertEquals(7, b.get(5));
        assertEquals(8, b.get(6));
        assertEquals(9, b.get(7));
        assertEquals(10, b.get(8));
    }

    public void testByteListAppendSingleIntTruncatesWithSubrangeOfInitialBytes() {
        int[] ints = new int[] { 0x1006, 0x1007, 0x1008 };

        ByteList b = new ByteList(new byte[] {0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 127}, 6, 4, false);
        for (int i = 0; i < ints.length; i++) {
            b.append(ints[i]);
        }

        assertEquals(7, b.length());
        assertEquals(2, b.get(0));
        assertEquals(3, b.get(1));
        assertEquals(4, b.get(2));
        assertEquals(5, b.get(3));
        assertEquals(6, b.get(4));
        assertEquals(7, b.get(5));
        assertEquals(8, b.get(6));
    }

    public void testByteListPrependSingleByteWithSubrangeOfInitialBytes() {
        byte[] bytes = new byte[] {6, 7, 8};
        ByteList b = new ByteList(new byte[] {0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 127}, 6, 4, false);

        for (int i = 0; i < bytes.length; i++) {
            b.prepend(bytes[i]);
        }

        assertEquals(7, b.length());
        assertEquals(8, b.get(0));
        assertEquals(7, b.get(1));
        assertEquals(6, b.get(2));
        assertEquals(2, b.get(3));
        assertEquals(3, b.get(4));
        assertEquals(4, b.get(5));
        assertEquals(5, b.get(6));
    }

    public void testByteListAppendArrayWithSubrangeOfInitialBytes() {
        ByteList b = new ByteList(new byte[] {0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 127}, 6, 4, false);

        b.append(new byte[] {6, 7, 8, 9, 10});

        assertEquals(9, b.length());
        assertEquals(2, b.get(0));
        assertEquals(3, b.get(1));
        assertEquals(4, b.get(2));
        assertEquals(5, b.get(3));
        assertEquals(6, b.get(4));
        assertEquals(7, b.get(5));
        assertEquals(8, b.get(6));
        assertEquals(9, b.get(7));
        assertEquals(10, b.get(8));
    }

    public void testByteListAppendArrayIndexLengthWithSubrangeOfInitialBytes() {
        ByteList b = new ByteList(new byte[] {0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 127}, 6, 4, false);

        b.append(new byte[] {6, 7, 8, 9, 10}, 1, 4);
        assertEquals(8, b.length());
        assertEquals(2, b.get(0));
        assertEquals(3, b.get(1));
        assertEquals(4, b.get(2));
        assertEquals(5, b.get(3));
        assertEquals(7, b.get(4));
        assertEquals(8, b.get(5));
        assertEquals(9, b.get(6));
        assertEquals(10, b.get(7));
    }

    public void testLengthExpandFillsWithZeros() {
        ByteList b = new ByteList();
        b.length(10);
        assertEquals(0, b.get(9));
    }

    public void notestGetAndSetOutsideOfLengthShouldFail() {
        ByteList b = new ByteList(10);
        assertEquals(0, b.length());
        try {
            b.get(0);
            fail("should throw IndexOfOfBoundsException");
        } catch (IndexOutOfBoundsException oob) {
        }
        try {
            b.set(0, 1);
            fail("should throw IndexOfOfBoundsException");
        } catch (IndexOutOfBoundsException oob) {
        }
    }

    public void notestMethodsThatTakeByteArrayDoNotAllowNull() {
        ByteList b = new ByteList();
        try {
            new ByteList((byte[]) null);
            fail("should throw NPE");
        } catch (NullPointerException npe) {
        }
        try {
            new ByteList((byte[]) null, 0, 0);
            fail("should throw NPE");
        } catch (NullPointerException npe) {
        }
        try {
            b.append((byte[])null);
            fail("should throw NPE");
        } catch (NullPointerException npe) {
        }
        try {
            b.append((byte[])null, 0, 0);
            fail("should throw NPE");
        } catch (NullPointerException npe) {
        }
        try {
            b.replace(null);
            fail("should throw NPE");
        } catch (NullPointerException npe) {
        }
    }

    public void testReplaceSetsNewContents() {
        ByteList b = new ByteList(1);

        b.replace(new byte[] {0x01, 0x02, 0x03, 0x04, 0x05});
        assertEquals(5, b.length());
        assertEquals(0x01, b.get(0));
        assertEquals(0x05, b.get(4));
    }

    public void testReplaceIndexOffset() {
        ByteList base = new ByteList(new byte[] {0x50,0x51,0x52,0x53,0x54});
        base.setBegin(1);
        base.setRealSize(3);
        base.replace(1, 1, new byte[] {0x54,0x55});
        assertEquals(new ByteList(new byte[] {0x51,0x54,0x55,0x53}), base);
        base = new ByteList(new byte[] {0x50,0x51,0x52,0x53,0x54});
        base.setBegin(1);
        base.setRealSize(3);
        base.replace(0, 3, new byte[] {0x50, 0x50, 0x50}, 1, 2);
        assertEquals(new ByteList(new byte[] {0x50, 0x50}), base);
    }

    private ByteList S(String s) throws UnsupportedEncodingException {
        return new ByteList(s.getBytes("ISO8859_1"));
    }
    private String S(ByteList b) throws UnsupportedEncodingException {
        return new String(b.bytes(), "ISO8859_1");
    }

    public void testReplaceStrings() throws UnsupportedEncodingException {
        ByteList b = S("FooBar");
        b.replace(0, 3, S("A"));
        assertEquals("ABar", S(b));
        b.replace(0, 1, S("Foo"));
        assertEquals("FooBar", S(b));
        b.replace(1, 2, S("zz"));
        assertEquals("FzzBar", S(b));
        b.replace(1, 0, S("u"));
        assertEquals("FuzzBar", S(b));
    }

    public void testReplaceLengthOutOfBounds() {
        ByteList b = new ByteList(new byte[] {0x01,0x02,0x03});
        try {
            b.replace(0, 5, new byte[] { 0x00, 0x00, 0x00 }, 1, 2);
            fail("should have thrown exception");
        } catch (IndexOutOfBoundsException e) {
        }
    }

    public void testEquals() {
        assertTrue(new ByteList().equals(new ByteList(10)));
        assertFalse(new ByteList().equals(new ByteList(new byte[] {0x01})));
        byte[] bytes = new byte[] {0x01, 0x02, 0x03, 0x04};
        ByteList b1 = new ByteList(bytes);
        ByteList b2 = new ByteList();
        b2.append(bytes);
        assertTrue(b1.equals(b2));
    }

    public void testBytesCreatesACopyOfInternalBytes() {
        byte[] bytes = new byte[] {0x01,0x02,0x03};
        ByteList b = new ByteList(bytes);
        byte[] copy = b.bytes();
        assertTrue(Arrays.equals(bytes, copy));
        b.set(1, 0);
        assertTrue(Arrays.equals(bytes, copy));
    }

    public void testCloneCopiesBytes() {
        ByteList b = new ByteList(new byte[] {0x01,0x02,0x03});
        ByteList b2 = (ByteList) b.clone();
        b.set(1, 0);
        assertFalse(b.equals(b2));
    }
    
    public void testIndexOf() {
        ByteList b = new ByteList(ByteList.plain("hello bytelist"));
        ByteList b2 = new ByteList(ByteList.plain("bytelist"));
        ByteList b3 = new ByteList(ByteList.plain("el"));
        
        assertEquals(6, b.indexOf('b'));
        assertEquals(6, b.indexOf(b2));
        assertEquals(-1, b.indexOf('x'));
        assertEquals(-1, b2.indexOf(b));
        assertEquals(10, b.indexOf('l', 6));
        assertEquals(9, b.indexOf(b3, 6));
        assertEquals(-1, b.indexOf('b', 7));
        assertEquals(-1, b.indexOf(b3, 10));
    }
    
    public void testLastIndexOf() {
        ByteList b = new ByteList(ByteList.plain("hello bytelist"));
        ByteList b2 = new ByteList(ByteList.plain("bytelist"));
        ByteList b3 = new ByteList(ByteList.plain("el"));
        
        assertEquals(10, b.lastIndexOf('l'));
        assertEquals(6, b.lastIndexOf(b2));
        assertEquals(-1, b.lastIndexOf('x'));
        assertEquals(-1, b2.lastIndexOf(b));
        assertEquals(3, b.lastIndexOf('l', 6));
        assertEquals(1, b.lastIndexOf(b3, 6));
        assertEquals(-1, b.lastIndexOf('b', 5));
        assertEquals(-1, b.lastIndexOf(b2, 5));
    }

    public void testShallowDup() {
        ByteList b = new ByteList(10);
        b.append("hello".getBytes());
        ByteList d = b.shallowDup();

        assertEquals(5, b.length());
        assertEquals(5, d.length());
    }

    public void testEndsWithOnEmptyByteList() {
        ByteList blank = new ByteList();
        ByteList blankAndZeroLength = new ByteList(0);
        ByteList str = new ByteList("hello".getBytes());

        assertFalse(blank.endsWith(str));
        assertFalse(blankAndZeroLength.endsWith(str));
    }

    public void testStartsWithOnEmptyByteList() {
        ByteList blank = new ByteList();
        ByteList blankAndZeroLength = new ByteList(0);
        ByteList str = new ByteList("hello".getBytes());

        assertFalse(blank.startsWith(str));
        assertFalse(blankAndZeroLength.startsWith(str));
    }

    public void testStartsWithWithOffset() {
        ByteList str = new ByteList("xxxx".getBytes());

        assertFalse(str.startsWith(str, 1));
    }

    public void testConstructorsSetEncoding() {
        ByteList utf8 = new ByteList(new byte[0], UTF8Encoding.INSTANCE);
        assertEquals(UTF8Encoding.INSTANCE, utf8.getEncoding());
        utf8 = new ByteList(new byte[0], UTF8Encoding.INSTANCE, false);
        assertEquals(UTF8Encoding.INSTANCE, utf8.getEncoding());
        utf8 = new ByteList(new byte[0], UTF8Encoding.INSTANCE, true);
        assertEquals(UTF8Encoding.INSTANCE, utf8.getEncoding());
        utf8 = new ByteList(new byte[0], 0, 0, UTF8Encoding.INSTANCE, false);
        assertEquals(UTF8Encoding.INSTANCE, utf8.getEncoding());
        utf8 = new ByteList(new byte[0], 0, 0, UTF8Encoding.INSTANCE, true);
        assertEquals(UTF8Encoding.INSTANCE, utf8.getEncoding());
        
        utf8 = new ByteList(utf8);
        assertEquals(UTF8Encoding.INSTANCE, utf8.getEncoding());
    }

    public void testEncodingCantBeNull() {
        ByteList bl = new ByteList(new byte[0], null);
        assertEquals(ASCIIEncoding.INSTANCE, bl.getEncoding());
        bl = new ByteList(new byte[0], null, false);
        assertEquals(ASCIIEncoding.INSTANCE, bl.getEncoding());
        bl = new ByteList(new byte[0], null, true);
        assertEquals(ASCIIEncoding.INSTANCE, bl.getEncoding());
        bl = new ByteList(new byte[0], 0, 0, null, false);
        assertEquals(ASCIIEncoding.INSTANCE, bl.getEncoding());
        bl = new ByteList(new byte[0], 0, 0, null, true);
        assertEquals(ASCIIEncoding.INSTANCE, bl.getEncoding());
    }

    public void testEnsureSameSize() {
        ByteList bl = new ByteList(50);
        byte[] bytes = bl.unsafeBytes();
        assertEquals(50, bytes.length);
        bl.ensure(50);
        assertEquals(bytes, bl.unsafeBytes());
        assertEquals(50, bl.unsafeBytes().length);
    }

    public void testEnsureAdding() {
        byte[] wrap = new byte[1];
        ByteList bl = new ByteList(wrap, 0, 1, false);
        bl.ensure(2);
        byte[] newOne = bl.getUnsafeBytes();

        assertTrue(newOne.length > wrap.length);
        assertEquals(0, bl.getBegin());

        // non-zero begin
        wrap = new byte[100];
        bl = new ByteList(wrap, 49, 50, false);
        bl.ensure(200);

        assertTrue(bl.getUnsafeBytes().length >= 200);
        assertEquals(0, bl.getBegin());
        assertEquals(50, bl.getRealSize());
    }

    public void testStartsWithReturnFalseWhenComparedListHasMoreBytes() {
        ByteList file_url_start = ByteList.create("file:");
        ByteList f = ByteList.create("f");
        try {
            f.startsWith(file_url_start);
        } catch (Exception ex) {
            fail("shouldn't have thrown exception");
        }
    }
}

