package org.jruby.test;

import junit.framework.TestCase;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.runtime.Block;
import org.jruby.runtime.builtin.IRubyObject;

public class TestArrayFlatten extends TestCase {

    public void testFlatten() throws Exception {
        Ruby runtime = Ruby.newInstance();
        RubyArray keys = runtime.newArray();
        RubyArray values = runtime.newArray();
        
//        int n = 10;
        int n = 10000;
        for (int i = 0; i < n; ++i) {
            keys.append(runtime.newFixnum(i));
            values.append(runtime.newFixnum(i));
        }
        
        RubyArray temp = (RubyArray) keys.zip(runtime.getCurrentContext(), new IRubyObject[] {values}, Block.NULL_BLOCK);
        RubyArray preHash = (RubyArray) temp.flatten(runtime.getCurrentContext());
        
        assertNotNull("We have a hash back", preHash);
    }
}
