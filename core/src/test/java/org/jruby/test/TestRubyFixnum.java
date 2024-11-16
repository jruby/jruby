package org.jruby.test;

import org.jruby.*;

import org.jruby.runtime.ThreadContext;
import org.junit.Test;

import static org.jruby.api.Create.newFixnum;

public class TestRubyFixnum extends junit.framework.TestCase {

    final private ThreadContext context = Ruby.newInstance().getCurrentContext();

    @Test
    public void testZero() {
        RubyFixnum num = newFixnum(context, 0);
        assertEquals(new RubyFixnum(context.runtime, 0), num);

        num = RubyFixnum.zero(context.runtime);
        assertEquals(newFixnum(context, 0), num);
        assertEquals(0, num.getLongValue());
    }

    @Test
    public void testMinusOne() {
        RubyFixnum num = RubyFixnum.minus_one(context.runtime);
        assertEquals(newFixnum(context, -1), num);
        assertEquals(-1, num.getLongValue());
    }

    @Test
    public void testOne() {
        RubyFixnum num = RubyFixnum.one(context.runtime);
        assertEquals(newFixnum(context, 1), num);
        assertEquals(1, num.getLongValue());
    }

    @Test
    public void testTwo() {
        RubyFixnum num = RubyFixnum.two(context.runtime);
        assertEquals(newFixnum(context, 2), num);
        assertEquals(2, num.getLongValue());
    }

    @Test
    public void testFour() {
        RubyFixnum num = RubyFixnum.four(context.runtime);
        assertEquals(newFixnum(context, 4), num);
        assertEquals(4, num.getLongValue());
    }

}
