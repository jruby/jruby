package org.jruby.util;

import java.util.List;
import junit.framework.TestCase;
import org.jruby.util.RawArgParser;

public class RawArgParserTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testShouldParseEmptyArgs() {
        assertEquals(0, getArgs(new String[0]).size());
    }
    
    public void testShouldParseSingleArg() {
        List args = getArgs(new String[] { "hello" });
        assertEquals(1, args.size());
        assertEquals("hello", args.get(0));
    }

    public void testShouldSkipSpaces() {
        List args = getArgs(new String[] { " hello   world" });
        assertEquals(2, args.size());
        assertEquals("hello", args.get(0));
        assertEquals("world", args.get(1));
    }

    public void testShouldEliminateQuotes() {
        List args = getArgs(new String[] { "ruby -I\"include me\" 'foo' " });
        assertEquals(3, args.size());
        assertEquals("ruby", args.get(0));
        assertEquals("-Iinclude me", args.get(1));
        assertEquals("foo", args.get(2));
    }
    
    public void testShouldKeepQuotedQuotesAndSpaces() {
        List args = getArgs(new String[] { "hello '\"world\"' 'spaces  here'" });
        assertEquals(3, args.size());
        assertEquals("hello", args.get(0));
        assertEquals("\"world\"", args.get(1));
        assertEquals("spaces  here", args.get(2));
    }
    
    public void testShouldParseRealCommand() {
        List args = getArgs(new String[] { "jruby -e \"system(%Q[ruby -e 'system %q(echo hello) ; puts %q(done)'])\"" });
        assertEquals(3, args.size());
        assertEquals("jruby", args.get(0));
        assertEquals("-e", args.get(1));
        assertEquals("system(%Q[ruby -e 'system %q(echo hello) ; puts %q(done)'])", args.get(2));
    }
    
    private List getArgs(String[] in) {
        return new RawArgParser(in).getArgs();
    }
}
