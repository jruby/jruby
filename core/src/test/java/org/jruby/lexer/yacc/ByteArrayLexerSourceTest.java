package org.jruby.lexer.yacc;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import junit.framework.TestCase;
import org.jruby.util.ByteList;

/**
 *
 * @author nicksieger
 */
public class ByteArrayLexerSourceTest extends TestCase {
    public ByteArrayLexerSourceTest(String testName) {
        super(testName);
    }

    public void testReadShouldReturnArrayContentsInOrder() {
        LexerSource src = newSource("abcd1234");
        assertReadShouldProduce(src, "abcd1234");
    }

    public void testSuccessiveReadsShouldReturnInputInChunks() {
        LexerSource src = newSource("abcd1234");
        assertReadShouldProduce(src, "abcd");
        assertReadShouldProduce(src, "1234");
    }

    public void testCarriageReturnsShouldBeEaten() {
        LexerSource src = newSource("abcd\r\n");
        assertReadShouldProduce(src, "abcd\n");
        assertEquals(6, src.getOffset());
    }

    // This one currently fails with InputStreamLexerSource
    public void testCarriageReturnsShouldBeUnreadAutomatically() throws IOException {
        LexerSource src = newSource("abc\r\ndef");
        assertReadShouldProduce(src, "abc\ndef");
        assertEquals(8, src.getOffset());
        src.unreadMany("\ndef");
        assertEquals(3, src.getOffset());
        src.read();
        assertEquals(5, src.getOffset());
        assertReadShouldProduce(src, "def");
    }

    public void testReadUntilShouldIncludeInputUpToChar() {
        final LexerSource src = newSource("abcd1234");
        assertActionShouldProduce("abcd", new Callable<byte[]>() {
            public byte[] call() throws Exception {
                return src.readUntil('1').bytes();
            }
        });
    }

    public void testReadUnreadReadShouldProduceCorrectSequence() {
        LexerSource src = newSource("abcd1234");
        assertReadShouldProduce(src, "abcd");
        src.unread('d');
        assertReadShouldProduce(src, "d1234");
    }

    public void testReadUnreadManyReadShouldProduceCorrectSequence() {
        LexerSource src = newSource("abcd1234");
        assertReadShouldProduce(src, "abcd");
        src.unreadMany("abcd");
        assertReadShouldProduce(src, "abcd1234");
    }

    public void testUnreadNewCharactersShouldBeAppended() {
        LexerSource src = newSource("abcd");
        src.unreadMany("1234");
        assertReadShouldProduce(src, "1234abcd");
    }

    public void testUnreadBothNewAndExistingCharacters() throws IOException {
        LexerSource src = newSource("abc");
        src.read();
        src.unreadMany("123a");
        assertReadShouldProduce(src, "123abc");
    }

    public void testReadLineBytesShouldIncludeInputExcludingNewline() {
        final LexerSource src = newSource("abcd\n1234");
        assertActionShouldProduce("abcd", new Callable<byte[]>() {
            public byte[] call() throws Exception {
                return src.readLineBytes().bytes();
            }
        });
    }

    public void testReadLineBytesShouldIncludeTheRestOfInputWhenNoMoreNewlines() {
        final LexerSource src = newSource("abcd");
        assertActionShouldProduce("abcd", new Callable<byte[]>() {
            public byte[] call() throws Exception {
                return src.readLineBytes().bytes();
            }
        });
    }

    public void testReadUntilNonExistentMarkerShouldReturnNull() throws IOException {
        final LexerSource src = newSource("abcd\n1234");
        assertNull(src.readUntil('z'));
    }

    public void testSkipUntilShouldSkipPastNonMatchingChars() throws IOException {
        LexerSource src = newSource("abcd1234");
        src.skipUntil('1');
        assertReadShouldProduce(src, "234");
    }

    public void testSkipUntilNonExistentMarkerShouldReturnEOF() throws IOException {
        final LexerSource src = newSource("abcd\n1234");
        assertEquals(RubyYaccLexer.EOF, src.skipUntil('z'));
    }

    public void testMatchMarkerShouldMatchAStringInTheInput() throws IOException {
        final LexerSource src = newSource("=begin\n=end");
        assertTrue(src.matchMarker(new ByteList(safeGetBytes("=begin")), false, false));
    }

    public void testMatchMarkerShouldMatchAStringInTheInputAndAdvance() throws IOException {
        final LexerSource src = newSource("=begin\n=end");
        assertTrue(src.matchMarker(new ByteList(safeGetBytes("=begin")), false, false));
        assertReadShouldProduce(src, "\n=end");
    }

    public void testMatchMarkerShouldNotAdvanceIfItDoesntMatch() throws IOException {
        final LexerSource src = newSource("=begin\n=end");
        assertFalse(src.matchMarker(new ByteList(safeGetBytes("=end")), false, false));
        assertReadShouldProduce(src, "=begin");
    }

    public void testMatchMarkerShouldSkipOverLeadingWhitespace() throws IOException {
        final LexerSource src = newSource("   =begin\n=end");
        assertTrue(src.matchMarker(new ByteList(safeGetBytes("=begin")), true, false));
        assertReadShouldProduce(src, "\n=end");
    }

    public void testMatchMarkerShouldNotCountNewlinesAsWhitespace() throws IOException {
        final LexerSource src = newSource("\n=begin\n=end");
        assertFalse(src.matchMarker(new ByteList(safeGetBytes("=begin")), true, false));
    }

    public void testMatchMarkerShouldVerifyEndOfLine() throws IOException {
        final LexerSource src = newSource("=begin\n=end");
        assertTrue(src.matchMarker(new ByteList(safeGetBytes("=begin")), false, true));
        assertReadShouldProduce(src, "=end");
    }

    public void testMatchMarkerShouldVerifyEndOfLineCRLF() throws IOException {
        final LexerSource src = newSource("=begin\r\n=end\r\n");
        assertTrue(src.matchMarker(new ByteList(safeGetBytes("=begin")), false, true));
        assertReadShouldProduce(src, "=end");
    }

    public void testMatchMarkerShouldVerifyEndOfLineCRLFWithIndent() throws IOException {
        final LexerSource src = newSource("  =begin\r\n=end\r\n");
        assertTrue(src.matchMarker(new ByteList(safeGetBytes("=begin")), true, true));
        assertReadShouldProduce(src, "=end");
    }

    public void testMatchMarkerAcrossUnreadBuffers() throws IOException {
        final LexerSource src = newSource("in\n=end");
        src.unreadMany("=beg");
        assertTrue(src.matchMarker(new ByteList(safeGetBytes("=begin")), false, false));
    }

    public void testWasBeginOfLineBehaviorAtBeginningOfInput() throws IOException {
        final LexerSource src = newSource("=begin");
        src.read();
        assertFalse(src.lastWasBeginOfLine());
        assertTrue(src.wasBeginOfLine());
    }

    public void testWasBeginOfLineTellsIfPreviousCharactersWereNewline() throws IOException {
        final LexerSource src = newSource(".\n=end");
        assertTrue(src.lastWasBeginOfLine());
        assertFalse(src.wasBeginOfLine());
        src.read(); src.read();
        assertTrue(src.lastWasBeginOfLine());
        assertFalse(src.wasBeginOfLine());
        src.read();
        assertFalse(src.lastWasBeginOfLine());
        assertTrue(src.wasBeginOfLine());
        src.read();
        assertFalse(src.lastWasBeginOfLine());
        assertFalse(src.wasBeginOfLine());
    }

    public void testPeekGivesTheNextCharaterWithoutAdvancing() throws IOException {
        LexerSource src = newSource("abc");
        assertTrue(src.peek('a'));
        assertEquals('a', src.read());
    }

    @SuppressWarnings("empty-statement")
    public void testLinesAndOffsetsAreReported() throws IOException {
        LexerSource src = newSource("a\nb\nc\nd\n");
        while (src.read() != RubyYaccLexer.EOF);
        assertEquals(4, src.getLine());
        assertEquals(8, src.getOffset());
    }

    public void testCurrentLineGivesAnErrorLocation() throws IOException {
        LexerSource src = newSource("111111\n222222\n333333");
        for (int i = 0; i < 10; i++) {
            src.read();
        }
        assertEquals("222222\n  ^", src.getCurrentLine());
    }

    @SuppressWarnings("empty-statement")
    public void testCaptureLines() throws IOException {
        List<String> lines = new ArrayList<String>();
        LexerSource src = newSource("111111\n222222\n333333", lines);
        while (src.read() != RubyYaccLexer.EOF);
        assertEquals(3, lines.size());
        assertEquals("111111\n", lines.get(0));
        assertEquals("222222\n", lines.get(1));
        assertEquals("333333", lines.get(2));
    }

    public void testCaptureLinesWithCarriageReturn() throws IOException {
        List<String> lines = new ArrayList<String>();
        LexerSource src = newSource("1\r\n2\r\n3", lines);
        while (src.read() != RubyYaccLexer.EOF);
        assertEquals(3, lines.size());
        assertEquals("1\r\n", lines.get(0));
        assertEquals("2\r\n", lines.get(1));
        assertEquals("3", lines.get(2));
    }

    public void testGetRemainingOutputAsStream() throws IOException {
        LexerSource src = newSource("111111\n222222\n333333\n");
        for (int i = 0; i < 10; i++) {
            src.read();
        }
        final InputStream in = src.getRemainingAsStream();
        assertActionShouldProduce("222\n333333\n", new Callable<byte[]>() {
            public byte[] call() throws Exception {
                ByteList buf = new ByteList();
                int c;
                while ((c = in.read()) != -1) {
                    buf.append(c);
                }
                return buf.bytes();
            }
        });
    }

    private byte[] safeGetBytes(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            fail("missing encoding");
            return null;
        }
    }

    private LexerSource newSource(String contents) {
        return newSource(contents, null);
    }

    private LexerSource newSource(String contents, List<String> scriptLines) {
        //return new InputStreamLexerSource("in-memory-source",
        //    new java.io.ByteArrayInputStream(safeGetBytes(contents)), scriptLines, 0, false);
        return new ByteArrayLexerSource("in-memory-source", safeGetBytes(contents), scriptLines, 0, false);
    }

    private void assertActionShouldProduce(String expected, Callable<byte[]> action) {
        try {
            assertEquals("Wrong result,", expected, new String(action.call(), "UTF-8"));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    private void assertReadShouldProduce(LexerSource src, String expected) {
        final ByteList actual = new ByteList(expected.length());
        for (int i = 0; i < expected.length(); i++) {
            try {
                int c = src.read();
                if (c == RubyYaccLexer.EOF) {
                    break;
                }
                actual.append(c);
            } catch (IOException ex) {
                fail(ex.getMessage());
            }
        }
        assertActionShouldProduce(expected, new Callable<byte[]>() {
            public byte[] call() throws Exception {
                return actual.bytes();
            }
        });
    }
}
