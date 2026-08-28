/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.test;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.api.Access;

import static org.jruby.api.Access.objectClass;
import static org.jruby.api.Define.defineClass;

/**
 *
 * @author headius
 */
public class TestRubyClass extends junit.framework.TestCase {

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
            super(currentRuntime, Access.getClass(currentRuntime.getCurrentContext(), "TestSetClassAllocatorClass"));
        }
    }

    public void testSetClassAllocator() {
        var context = runtime.getCurrentContext();
        var Object = objectClass(context);
        // start out with a default RubyObject allocator
        RubyClass newClass = defineClass(context, "TestSetClassAllocatorClass", Object, Object.getAllocator());

        assertEquals(RubyObject.class, newClass.allocate(context).getClass());

        // switch to an allocator based on a default constructor
        MyRubyObjectSubclass.currentRuntime = runtime;
        newClass.setClassAllocator(MyRubyObjectSubclass.class);

        assertEquals(MyRubyObjectSubclass.class, newClass.allocate(context).getClass());
    }
}
