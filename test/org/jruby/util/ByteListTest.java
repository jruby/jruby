package org.jruby.util;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import junit.framework.TestCase;

public class ByteListTest extends TestCase {
    public void testEmptyByteListHasZeroLength() {
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

    public void testLengthExpandFillsWithZeros() {
        ByteList b = new ByteList();
        b.length(10);
        assertEquals(0, b.get(9));
    }

    public void testGetAndSetOutsideOfLengthShouldFail() {
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

    public void testMethodsThatTakeByteArrayDoNotAllowNull() {
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
        ByteList base = new ByteList(new byte[] {0x01,0x02,0x03});
        ByteList b = (ByteList) base.clone();
        b.replace(1, 1, new byte[] {0x04,0x05});
        assertEquals(new ByteList(new byte[] {0x01,0x04,0x05,0x03}), b);
        b = (ByteList) base.clone();
        b.replace(0, 3, new byte[] {0x00, 0x00, 0x00}, 1, 2);
        assertEquals(new ByteList(new byte[] {0x00, 0x00}), b);
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
}
