/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.test;

import junit.framework.TestCase;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;

/**
 *
 * @author headius
 */
public class TestRubyClass extends TestCase {
    Ruby runtime;
    
    public TestRubyClass(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        runtime = Ruby.newInstance();

    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        runtime.tearDown();
    }

    public static class MyRubyObjectSubclass extends RubyObject {
        public static Ruby currentRuntime;
        
        public MyRubyObjectSubclass() {
            super(currentRuntime, currentRuntime.getClass("TestSetClassAllocatorClass"));
        }
    }

    // TODO add test methods here. The name must begin with 'test'. For example:
    public void testSetClassAllocator() {
        // start out with a default RubyObject allocator
        RubyClass newClass = runtime.defineClass("TestSetClassAllocatorClass", runtime.getObject(), runtime.getObject().getAllocator());

        assertEquals(RubyObject.class, newClass.allocate().getClass());

        // switch to an allocator based on a default constructor
        MyRubyObjectSubclass.currentRuntime = runtime;
        newClass.setClassAllocator(MyRubyObjectSubclass.class);

        assertEquals(MyRubyObjectSubclass.class, newClass.allocate().getClass());
    }
}
