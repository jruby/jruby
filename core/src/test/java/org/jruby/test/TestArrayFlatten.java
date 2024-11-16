package org.jruby.test;

import junit.framework.TestCase;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Create.newFixnum;

public class TestArrayFlatten extends TestCase {

    public void testFlatten() {
        Ruby runtime = Ruby.newInstance();
        ThreadContext context = runtime.getCurrentContext();
        RubyArray keys = runtime.newArray();
        RubyArray values = runtime.newArray();
        
//        int n = 10;
        int n = 10000;
        for (int i = 0; i < n; ++i) {
            keys.append(newFixnum(context, i));
            values.append(newFixnum(context, i));
        }
        
        RubyArray temp = (RubyArray) keys.zip(context, new IRubyObject[] {values}, Block.NULL_BLOCK);
        RubyArray preHash = (RubyArray) temp.flatten(context);
        
        assertNotNull("We have a hash back", preHash);
    }
}
