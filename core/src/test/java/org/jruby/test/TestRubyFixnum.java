package org.jruby.test;

import org.jruby.*;

import org.junit.Test;

public class TestRubyFixnum extends junit.framework.TestCase {

    private Ruby runtime = Ruby.newInstance();

    static {
        // wasn't working when fixnum cache got disabled, test using:
        //   mvn -Djruby.fixnum.cache=false -Ptest
        System.out.println("RubyFixnum: USE_CACHE = " + RubyFixnum.USE_CACHE + " CACHE_OFFSET = " + RubyFixnum.CACHE_OFFSET);
    }

    @Test
    public void testZero() {
        RubyFixnum num = runtime.newFixnum(0);
        assertEquals(new RubyFixnum(runtime, 0), num);

        num = RubyFixnum.zero(runtime);
        assertEquals(runtime.newFixnum(0), num);
        assertEquals(0, num.getLongValue());
    }

    @Test
    public void testMinusOne() {
        RubyFixnum num = RubyFixnum.minus_one(runtime);
        assertEquals(runtime.newFixnum(-1), num);
        assertEquals(-1, num.getLongValue());
    }

    @Test
    public void testOne() {
        RubyFixnum num = RubyFixnum.one(runtime);
        assertEquals(runtime.newFixnum(1), num);
        assertEquals(1, num.getLongValue());
    }

    @Test
    public void testTwo() {
        RubyFixnum num = RubyFixnum.two(runtime);
        assertEquals(runtime.newFixnum(2), num);
        assertEquals(2, num.getLongValue());
    }

    @Test
    public void testFour() {
        RubyFixnum num = RubyFixnum.four(runtime);
        assertEquals(runtime.newFixnum(4), num);
        assertEquals(4, num.getLongValue());
    }

}
