/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.test.language;

import org.junit.*;

import org.jruby.truffle.test.*;

/**
 * Test variables with special semantics, such as the 'global variables', {@code $_}, {@code $~}
 * etc.
 */
public class SpecialVariableTests extends RubyTests {

    @Test
    public void testGetsResult() {
        assertPrintsWithInput("test\n", "gets; puts $_", "test\n", new String[]{});
    }

}
