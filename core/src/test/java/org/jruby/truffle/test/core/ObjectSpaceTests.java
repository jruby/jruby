/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.test.core;

import org.junit.*;

import org.jruby.truffle.test.*;

/**
 * Test the {@code ObjectSpace} module.
 */
public class ObjectSpaceTests extends RubyTests {

    @Test
    public void testEachObjectClass() {
        assertPrints("true\n", "found_string = false; ObjectSpace.each_object(Class) { |o| if o == String; found_string = true; end  }; puts found_string");
    }

    @Test
    public void testId2RefClass() {
        assertPrints("true\n", "puts ObjectSpace._id2ref(String.object_id) == String");
    }

    @Ignore
    @Test
    public void testEachObjectString() {
        final String code = "foo = \"foo\"; found_foo = false; ObjectSpace.each_object(String) { |o| if o == foo; found_foo= true; end  }; puts found_foo";
        final String input = "";
        final String expected = "true\n";
        assertPrints(expected, "(test)", code, input);
    }

    @Ignore
    @Test
    public void testId2RefString() {
        final String code = "foo = \"foo\"; puts ObjectSpace._id2ref(foo.object_id) == foo";
        final String input = "";
        final String expected = "true\n";
        assertPrints(expected, "(test)", code, input);
    }

    @Test
    public void testGarbageCollect() {
        assertPrints("", "ObjectSpace.garbage_collect");
        assertPrints("", "ObjectSpace.start");
    }
}
