package org.jruby.util;

import org.joni.Regex;
import org.joni.Option;
import org.joni.Syntax;
import org.joni.encoding.specific.ASCIIEncoding;

import junit.framework.TestCase;

/**
 * @author kscott
 *
 */
public class StringScannerTest extends TestCase {
	
	private final static Regex WORD_CHARS = new Regex(new byte[]{'\\','w','+'},0,3,Option.DEFAULT,ASCIIEncoding.INSTANCE,Syntax.DEFAULT);
	private final static Regex WHITESPACE = new Regex(new byte[]{'\\','s','+'},0,3,Option.DEFAULT,ASCIIEncoding.INSTANCE,Syntax.DEFAULT);
	
	private final static ByteList DATE_STRING = new ByteList("Fri Dec 12 1975 14:39".getBytes());

	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testCreate() throws Exception {
		StringScanner ss = new StringScanner(S("Test String"));
		
		assertEquals(S("Test String"), ss.getString());
		
		assertEquals(0, ss.getPos());
		
		assertFalse("matched() must return false after create", ss.matched());
		
		assertEquals(-1, ss.matchedSize());
		
		try {
			ss.unscan();
			fail("unscan() called after create must throw exception.");
		} catch (IllegalStateException e) {
			
		}
	}
	
	public void testSetString() throws Exception {
		StringScanner ss = new StringScanner(S("Test String"));
		
		ss.scan(WORD_CHARS);
		
		ss.setString(S("test string"));
		
		assertEquals(S("test string"), ss.getString());
		
		assertEquals(0, ss.getPos());
		
		assertFalse("matched() must return false after setString is called", ss.matched());
		
		assertEquals(-1, ss.matchedSize());
		
		try {
			ss.unscan();
			fail("unscan() must throw exception after setString() is called.");
		} catch (IllegalStateException e) {
			
		}
	}
	
	public void testEOS() throws Exception {
		StringScanner ss = new StringScanner(S("Test String"));
		
		assertFalse("New StringScanner with non-empty string returned true for isEndOfString", ss.isEndOfString());
		
		ss = new StringScanner(S(""));
		
		assertTrue("New StringScanner with empty string returned false for isEndOfString", ss.isEndOfString());
	}
	
	
	public void testGetChar() throws Exception {
		StringScanner ss = new StringScanner(S("12"));
		
		byte val = ss.getChar();
		
		assertEquals('1', val);
		
		assertFalse(ss.isEndOfString());
		
		val = ss.getChar();
		
		assertEquals('2', val);
		assertEquals(S("2"), ss.matchedValue());
		
		assertTrue(ss.isEndOfString());
		
		val = ss.getChar();
		
		assertEquals(0, val);
	}
	
	public void testScan() throws Exception {
		StringScanner ss = new StringScanner(S("Test String"));

		ByteList cs = ss.scan(WORD_CHARS);
		assertEquals(S("Test"), cs);
		assertTrue("matched() should have returned true", ss.matched());
		assertEquals(S("Test"), ss.matchedValue());
		
		cs = ss.scan(WORD_CHARS);
		assertNull("Non match should return null", cs);
		
		cs = ss.scan(WHITESPACE);
		cs = ss.scan(WORD_CHARS);
		assertEquals(S("String"), cs);
		
		assertTrue("isEndOfString should be true", ss.isEndOfString());
	}
	
	public void testScanUntil() throws Exception {
		StringScanner ss = new StringScanner(DATE_STRING);
		
		ByteList cs = ss.scanUntil(new Regex(new byte[]{'1'},0,1,Option.DEFAULT,ASCIIEncoding.INSTANCE,Syntax.DEFAULT));
		
		assertEquals(S("Fri Dec 1"), cs);
		
		assertTrue("matched() must return true after successful scanUntil()", ss.matched());
		
		assertEquals(9, ss.getPos());
		
		assertEquals(S("1"), ss.matchedValue());
	}
	
	public void testCheckUntil() throws Exception {
		StringScanner ss = new StringScanner(DATE_STRING);
		
		ByteList cs = ss.checkUntil(new Regex(new byte[]{'1'},0,1,Option.DEFAULT,ASCIIEncoding.INSTANCE,Syntax.DEFAULT));
		
		assertEquals(S("Fri Dec 1"), cs);
		
		assertEquals(0, ss.getPos());
		
		assertEquals(S("1"), ss.matchedValue());
	}
	
	public void testSkipUntil() throws Exception {
		StringScanner ss = new StringScanner(DATE_STRING);
		
		assertEquals(9, ss.skipUntil(new Regex(new byte[]{'1'},0,1,Option.DEFAULT,ASCIIEncoding.INSTANCE,Syntax.DEFAULT)));
		
		assertEquals(S("1"), ss.matchedValue());
	}
	
	public void testPos() throws Exception {
		StringScanner ss = new StringScanner(S("word 123"));
		
		assertEquals(0, ss.getPos());
		
		ss.getChar();
		
		assertEquals(1, ss.getPos());
		
		ss.scan(WORD_CHARS);
		
		assertEquals(4, ss.getPos());
	}
	
	public void testIsBeginningOfLine() throws Exception {
		StringScanner ss = new StringScanner(S("Test\nString\n"));
		
		assertTrue(ss.isBeginningOfLine());
		
		ss.scan(WORD_CHARS);
		
		assertFalse(ss.isBeginningOfLine());
		
		ss.scan(WHITESPACE);
		
		assertTrue(ss.isBeginningOfLine());
	}
	
	public void testMatched() throws Exception {
		StringScanner ss = new StringScanner(DATE_STRING);
		
		assertEquals(3, ss.matches(WORD_CHARS));
		
		assertEquals(0, ss.getPos());
		
		assertEquals(S("Fri"), ss.matchedValue());
		
		assertEquals(3, ss.matchedSize());
		
		assertTrue("matched() should have returned true", ss.matched());
	}
	
	public void testCheck() throws Exception {
		StringScanner ss = new StringScanner(DATE_STRING);
		
		assertEquals(S("Fri"), ss.check(WORD_CHARS));
		
		assertEquals(0, ss.getPos());
		
		assertEquals(S("Fri"), ss.matchedValue());
		
		assertEquals(3, ss.matchedSize());
		
		assertTrue("matched() should have returned true", ss.matched());
	}
	
	public void testSkip() throws Exception {
		StringScanner ss = new StringScanner(DATE_STRING);
		
		assertEquals(3, ss.skip(WORD_CHARS));
		
		assertEquals(-1, ss.skip(WORD_CHARS));
	}
	
	public void testExists() throws Exception {
		StringScanner ss = new StringScanner(S("test string"));
		
		assertEquals(3, ss.exists(new Regex(new byte[]{'s'},0,1,Option.DEFAULT,ASCIIEncoding.INSTANCE,Syntax.DEFAULT)));
		
		assertEquals(0, ss.getPos());
		
		assertTrue(ss.matched());
		
		assertEquals(-1, ss.exists(new Regex(new byte[]{'z'},0,1,Option.DEFAULT,ASCIIEncoding.INSTANCE,Syntax.DEFAULT)));
	}
	
	public void testUnscan() throws Exception {
		StringScanner ss = new StringScanner(DATE_STRING);
		
		ss.scan(WORD_CHARS);
		
		ss.unscan();
		
		assertEquals(0, ss.getPos());
		
		assertFalse("matched() must return false after unscan()", ss.matched());
		
		assertEquals(-1, ss.matchedSize());
		
		assertNull("matchedValue() must return null after unscan()", ss.matchedValue());
		
		ss.scan(WHITESPACE);
		
		try {
			ss.unscan();
			fail("unscan() called after an unmatched scan must throw exception.");
		} catch (IllegalStateException e) {
			
		}
		
		ss.skip(WORD_CHARS);
		
		ss.unscan();
		
		assertEquals(0, ss.getPos());
	}

	public void testAppend() throws Exception {
		StringScanner ss = new StringScanner(DATE_STRING);
		
		ss.scan(WORD_CHARS);
		
		ss.append(S(" +1000 GMT"));
		
		assertEquals(3, ss.getPos());
		
		assertEquals(S("Fri Dec 12 1975 14:39" + " +1000 GMT"), ss.getString());
	}
	
	public void testReset() throws Exception {
		StringScanner ss = new StringScanner(DATE_STRING);
		
		ss.scan(WORD_CHARS);
		
		ss.reset();
		
		assertEquals(0, ss.getPos());
		
		assertFalse("matched() must return false after reset()", ss.matched());
		
		assertEquals(-1, ss.matchedSize());
		
		assertNull("matchedValue() must return null after reset()", ss.matchedValue());
		
		try {
			ss.unscan();
			fail("unscan() must throw exception after reset()");
		} catch (IllegalStateException e) {
			
		}
	}
		
	public void testGrouping() throws Exception {
		StringScanner ss = new StringScanner(DATE_STRING);
		
		ss.scan(new Regex(new byte[]{'(','\\','w','+',')',' ','(','\\','w','+',')',' ','(','\\','d','+',')'},0,17,Option.DEFAULT,ASCIIEncoding.INSTANCE,Syntax.DEFAULT));
		
		assertEquals(S("Fri"), ss.group(1));
		
		assertEquals(S("Dec"), ss.group(2));
		
		assertEquals(S("12"), ss.group(3));
		
		assertNull(ss.group(4));
		
		ss.scan(WORD_CHARS);
		
		assertNull(ss.group(1));
		
		byte c = ss.getChar();
		assertEquals(c, ss.group(0).charAt(0));
	}
	
	public void testPrePostMatch() throws Exception {
		StringScanner ss = new StringScanner(DATE_STRING);
		
		CharSequence cs = ss.scan(WORD_CHARS);
		
		ss.scan(WORD_CHARS);
		assertNull(ss.preMatch());
		assertNull(ss.postMatch());
		
		ss.scan(WHITESPACE);
		
		assertEquals(S("Fri"), ss.preMatch());
		assertEquals(S("Dec 12 1975 14:39"), ss.postMatch());
		
		ss.getChar();
		
		assertEquals(S("Fri "), ss.preMatch());
		assertEquals(S("ec 12 1975 14:39"), ss.postMatch());
		
		ss.reset();
		
		ss.scanUntil(new Regex(new byte[]{'1'},0,1,Option.DEFAULT,ASCIIEncoding.INSTANCE,Syntax.DEFAULT));
		
		assertEquals(S("Fri Dec "), ss.preMatch());
		assertEquals(S("2 1975 14:39"), ss.postMatch());
	}
	
	public void testTerminate() throws Exception {
		StringScanner ss = new StringScanner(DATE_STRING);
		
		ss.scan(WORD_CHARS);
		
		ss.terminate();
		
		assertTrue("endOfString() should return true after terminate().", ss.isEndOfString());
		
		assertFalse("matched() should return false after terminate().", ss.matched());
	}
	
	public void testPeek() throws Exception {
		StringScanner ss = new StringScanner(S("test string"));
		
		assertEquals(S("test st"), ss.peek(7));
		
		assertEquals(0, ss.getPos());
		
		assertEquals(S("test string"), ss.peek(300));
	}
	
	public void testRest() throws Exception {
		StringScanner ss = new StringScanner(S("test string"));
		
		ss.scan(WORD_CHARS);
		
		assertEquals(S(" string"), ss.rest());
		
		ss.terminate();
		
		assertEquals(S(""), ss.rest());
	}
	
	public void testSetPos() throws Exception {
		StringScanner ss = new StringScanner(S("test string"));
		
		ss.setPos(5);
		
		assertEquals(S("string"), ss.rest());
		
		try {
			ss.setPos(300);
			fail("setPos() with value greater than the length of the string should throw exception.");
		} catch (IllegalArgumentException e) {
			
		}
	}

    private ByteList S(String s) throws Exception {
        return new ByteList(s.getBytes(), false);
    }
}
