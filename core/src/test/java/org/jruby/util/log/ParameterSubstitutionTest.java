package org.jruby.util.log;

import junit.framework.TestCase;

import static org.jruby.util.log.OutputStreamLogger.substitute;

public class ParameterSubstitutionTest extends TestCase {

    public void testWithNoPlaceholder() {
        CharSequence actual = substitute("test");
        assertEquals("test", actual.toString());
    }

    public void testWithEmptyString() {
        CharSequence actual = substitute("");
        assertEquals("", actual.toString());
    }

    public void testWithNonEmptyString() {
        CharSequence actual = substitute(" ");
        assertEquals(" ", actual.toString());
    }

    public void testWithEmptyStringAndArg() {
        CharSequence actual = substitute("", "arg");
        assertEquals("", actual.toString());
    }

    public void testWithJustOnePlaceholder() {
        CharSequence actual = substitute("{}", "a");
        assertEquals("a", actual.toString());
    }

    public void testWithOnePlaceholder() {
        CharSequence actual = substitute("a {}", "test");
        assertEquals("a test", actual.toString());
    }

    public void testWithTwoPlaceholders() {
        CharSequence actual = substitute("{} and {}", "test1", "test2");
        assertEquals("test1 and test2", actual.toString());
    }

    public void testWithPlaceholdersCallsToString() {
        CharSequence actual = substitute("{} and {}", new StringBuilder("test"), new java.util.ArrayList());
        assertEquals("test and []", actual.toString());
    }

    public void testWithTwoPlaceholdersAfterAnother() {
        CharSequence actual = substitute("{}{}", "1", Integer.valueOf(2));
        assertEquals("12", actual.toString());
    }

    public void testWithPlaceholdersAndNullArg() {
        CharSequence actual = substitute("{} and {}", 0, null);
        assertEquals("0 and null", actual.toString());

        actual = substitute("the {} {}!{some}", null, null);
        assertEquals("the null null!{some}", actual.toString());
    }

    public void testPlaceholderEscaping() {
        CharSequence actual = substitute("\\{} & {}", 0, null);
        assertEquals("\\{} & 0", actual.toString());
    }

    public void testMorePlaceholdersThanArgs() {
        CharSequence actual = substitute("hello {}, {} and {}", 'A', 'B');
        assertEquals("hello A, B and {!abs-arg!}", actual.toString());

        String arg = null;
        actual = substitute("{} {} {xxx}", arg);
        assertEquals("null {!abs-arg!} {xxx}", actual.toString());
    }

}
