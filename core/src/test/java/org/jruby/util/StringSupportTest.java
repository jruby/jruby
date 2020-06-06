package org.jruby.util;

import static org.junit.Assert.*;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class StringSupportTest {

    @Test
    public void testSplit() {
        String str;

        str = "my\nname\nis\nslim\nshady";
        assertEquals(Arrays.asList(str.split("\n")), StringSupport.split(str, '\n'));

        assertEquals(Arrays.asList(str.split("\n", 0)), StringSupport.split(str, '\n', 0));
        assertEquals(Arrays.asList(str.split("\n", 1)), StringSupport.split(str, '\n', 1));
        assertEquals(Arrays.asList(str.split("\n", 2)), StringSupport.split(str, '\n', 2));
        assertEquals(Arrays.asList(str.split("\n", 3)), StringSupport.split(str, '\n', 3));
        assertEquals(Arrays.asList(str.split("\n", 4)), StringSupport.split(str, '\n', 4));
        assertEquals(Arrays.asList(str.split("\n", 5)), StringSupport.split(str, '\n', 5));
        assertEquals(Arrays.asList(str.split("\n", 6)), StringSupport.split(str, '\n', 6));

        str = "";
        assertArrayEquals(str.split(","), StringSupport.split(str, ',').toArray(new String[0]));
        assertArrayEquals(str.split(",", 3), StringSupport.split(str, ',', 3).toArray(new String[0]));
        assertArrayEquals(str.split(",", 0), StringSupport.split(str, ',', 0).toArray(new String[0]));

        str = "my\nname\nis\nslim\nshady\n";
        assertEquals(Arrays.asList(str.split("\n")), StringSupport.split(str, '\n'));
        assertEquals(Arrays.asList(str.split("\n", 4)), StringSupport.split(str, '\n', 4));
        assertEquals(Arrays.asList(str.split("\n", 5)), StringSupport.split(str, '\n', 5));
        assertEquals(Arrays.asList(str.split("\n", 6)), StringSupport.split(str, '\n', 6));

        str = "\nmy\nname\n\nis\nslim\nshady\n";
        assertEquals(Arrays.asList(str.split("\n")), StringSupport.split(str, '\n'));

        assertEquals(Arrays.asList(str.split("\n", 1)), StringSupport.split(str, '\n', 1));
        assertEquals(Arrays.asList(str.split("\n", 2)), StringSupport.split(str, '\n', 2));
        assertEquals(Arrays.asList(str.split("\n", 3)), StringSupport.split(str, '\n', 3));
        assertEquals(Arrays.asList(str.split("\n", 4)), StringSupport.split(str, '\n', 4));
        assertEquals(Arrays.asList(str.split("\n", 5)), StringSupport.split(str, '\n', 5));
        assertEquals(Arrays.asList(str.split("\n", 6)), StringSupport.split(str, '\n', 6));

        str = "0$$one$$$two$3$";
        assertEquals(Arrays.asList(str.split("\\$")), StringSupport.split(str, '$'));
        assertEquals(Arrays.asList(str.split("\\$", 1)), StringSupport.split(str, '$', 1));
        assertEquals(Arrays.asList(str.split("\\$", 2)), StringSupport.split(str, '$', 2));
        assertEquals(Arrays.asList(str.split("\\$", 3)), StringSupport.split(str, '$', 3));
        assertEquals(Arrays.asList(str.split("\\$", 4)), StringSupport.split(str, '$', 4));
        assertEquals(Arrays.asList(str.split("\\$", 5)), StringSupport.split(str, '$', 5));
        assertEquals(Arrays.asList(str.split("\\$", 6)), StringSupport.split(str, '$', 6));
        assertEquals(Arrays.asList(str.split("\\$", 7)), StringSupport.split(str, '$', 7));
        assertEquals(Arrays.asList(str.split("\\$", 8)), StringSupport.split(str, '$', 8));
        assertEquals(Arrays.asList(str.split("\\$", 10)), StringSupport.split(str, '$', 10));

        str = "1;2;;";
        // String#split gives us: [1, 2] here but we're better consistent ...
        // just like when consecutive separators are not at the end of string
        //assertEquals(Arrays.asList(str.split(";")), StringSupport.split(str, ';'));
        assertEquals(Arrays.asList("1", "2", ""), StringSupport.split(str, ';'));

        assertEquals(Arrays.asList(str.split(";", 1)), StringSupport.split(str, ';', 1));
        assertEquals(Arrays.asList(str.split(";", 2)), StringSupport.split(str, ';', 2));
        assertEquals(Arrays.asList(str.split(";", 3)), StringSupport.split(str, ';', 3));

        str = ";";
        // String#split gives us: [] here but we're better consistent ...
        //assertEquals(Arrays.asList(str.split(";")), StringSupport.split(str, ';'));
        assertEquals(Arrays.asList(""), StringSupport.split(str, ';'));

        assertEquals(Arrays.asList(str.split(";", 1)), StringSupport.split(str, ';', 1));
        assertEquals(Arrays.asList(str.split(";", 2)), StringSupport.split(str, ';', 2));
        assertEquals(Arrays.asList(str.split(";", 3)), StringSupport.split(str, ';', 3));
        assertEquals(Arrays.asList(str.split(":", 1)), StringSupport.split(str, ':', 1));
    }

    public static void main(String[] args) {
        System.out.println("WARMUP: ");
        runSplitTest(100 * 1000);
        System.gc(); System.gc();
        System.out.println("\n\n");
        runSplitTest(500 * 1000);
    }

    private static void runSplitTest(final long TIMES) {
        long time;

        System.gc();
        time = stringSupportSplit(TIMES, "1:2", ':');

        System.out.println("StringSupport.split(\"1:2\", ':') " + TIMES + "x took: " + time);

        System.gc();
        time = javaStringSplit(TIMES, "1:2", ":");

        System.out.println("\"1:2\".split(\":\") " + TIMES + "x took: " + time);

        //

        System.gc();
        time = stringSupportSplit(TIMES, "no-split-separator-match", ';');

        System.out.println("StringSupport.split(\"no-split-separator-match\", ';') " + TIMES + "x took: " + time);

        System.gc();
        time = javaStringSplit(TIMES, "no-split-separator-match", ";");

        System.out.println("\"no-split-separator-match\".split(\";\") " + TIMES + "x took: " + time);

        //

        System.gc();
        time = stringSupportSplit(TIMES, "this\nis\na\nlonger\n\nstring\n,well\nnot\nvery-long-but\nstill\n", '\n');

        System.out.println("StringSupport.split(\"this...\", '\\n') " + TIMES + "x took: " + time);

        System.gc();
        time = javaStringSplit(TIMES, "this\nis\na\nlonger\n\nstring\n,well\nnot\nvery-long-but\nstill\n", "\n");

        System.out.println("\"this...\".split(\"\\n\") " + TIMES + "x took: " + time);
    }

    private static long stringSupportSplit(final long TIMES, final String str, final char sep) {
        long time = System.currentTimeMillis();
        for ( int i=0; i<TIMES; i++ ) {
            StringSupport.split(str, sep);
        }
        return System.currentTimeMillis() - time;
    }

    private static long javaStringSplit(final long TIMES, final String str, final String sep) {
        long time = System.currentTimeMillis();
        for ( int i=0; i<TIMES; i++ ) {
            str.split(sep);
        }
        return System.currentTimeMillis() - time;
    }

}
