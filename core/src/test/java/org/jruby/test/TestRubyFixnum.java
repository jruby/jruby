package org.jruby.test;

import org.jruby.*;
import org.jruby.runtime.ThreadContext;

import static org.jruby.api.Convert.asFixnum;

public class TestRubyFixnum extends junit.framework.TestCase {

    final private ThreadContext context = Ruby.newInstance().getCurrentContext();

    public void testZero() {
        RubyFixnum num = RubyFixnum.zero(context.runtime);
        assertEquals(asFixnum(context, 0), num);

        num = RubyFixnum.zero(context.runtime);
        assertEquals(asFixnum(context, 0), num);
        assertEquals(0, num.getValue());
    }

    public void testMinusOne() {
        RubyFixnum num = RubyFixnum.minus_one(context.runtime);
        assertEquals(asFixnum(context, -1), num);
        assertEquals(-1, num.getValue());
    }

    public void testOne() {
        RubyFixnum num = RubyFixnum.one(context.runtime);
        assertEquals(asFixnum(context, 1), num);
        assertEquals(1, num.getValue());
    }

    public void testTwo() {
        RubyFixnum num = RubyFixnum.two(context.runtime);
        assertEquals(asFixnum(context, 2), num);
        assertEquals(2, num.getValue());
    }

    public void testFour() {
        RubyFixnum num = RubyFixnum.four(context.runtime);
        assertEquals(asFixnum(context, 4), num);
        assertEquals(4, num.getValue());
    }
}
