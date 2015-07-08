/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.tck;

import com.oracle.truffle.api.vm.TruffleVM;
import com.oracle.truffle.tck.TruffleTCK;

import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

public class RubyTckTest extends TruffleTCK {

    @Test
    public void checkVM() {
        TruffleVM vm = TruffleVM.newVM().build();
        assertNotNull(vm.getLanguages().get("application/x-ruby"));
    }

    @Override
    protected TruffleVM prepareVM() throws Exception {
        TruffleVM vm = TruffleVM.newVM().build();
        vm.eval("application/x-ruby",
            "def sum(a, b)\n"
          + " a + b\n"
          + "end\n"
          + "def fourty_two\n"
          + " 42\n"
          + "end\n"
          + "def ret_nil\n"
          + " nil\n"
          + "end\n"
          + "def apply_numbers(f)\n"
          + " f.call(18, 32) + 10\n"
          + "end\n"
          + "Truffle::Interop.export(\"sum_ints\", method(:sum))\n"
          + "Truffle::Interop.export(\"fourty_two\", method(:fourty_two))\n"
          + "Truffle::Interop.export(\"ret_nil\", method(:ret_nil))\n"
          + "Truffle::Interop.export(\"apply_numbers\", method(:apply_numbers))\n"
        );
        return vm;
    }

    @Override
    protected String plusInt() {
        return "sum_ints";
    }

    @Override
    protected String applyNumbers() {
        return "apply_numbers";
    }

    @Override
    protected String fourtyTwo() {
        return "fourty_two";
    }

    @Override
    protected String mimeType() {
        return "application/x-ruby";
    }

    @Override
    protected String returnsNull() {
        return "ret_nil";
    }

    @Override
    protected String invalidCode() {
        return "def something\n  ret urn 4.2\ne n d";
    }

    @Ignore
    @Test
    public void testMaxOrMinValue() throws Exception {
        super.testMaxOrMinValue();
    }

    @Ignore
    @Test
    public void testMaxOrMinValue2() throws Exception {
        super.testMaxOrMinValue2();
    }
    
}
