/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
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
        // @formatter:off
        TruffleVM jsVM = TruffleVM.newVM().build();
        jsVM.eval("application/x-ruby",
            "def sum a, b\n"
          + " return a + b\n"
          + "end\n"
          + "def fourtyTwo\n"
          + " return 42\n"
          + "end\n"
          + "def retNil\n"
          + " return nil\n"
          + "end\n"
          + "def applyNumbers(f)\n"
          + " return f.call(18, 32) + 10\n"
          + "end\n"
          + "Truffle::Interop.export(\"sumInts\", method(:sum))\n"
          + "Truffle::Interop.export(\"fourtyTwo\", method(:fourtyTwo))\n"
          + "Truffle::Interop.export(\"retNil\", method(:retNil))\n"
          + "Truffle::Interop.export(\"applyNumbers\", method(:applyNumbers))\n"
        );
        // @formatter:on
        return jsVM;
    }

    @Override
    protected String plusInt() {
        return "sumInts";
    }

    @Override
    protected String applyNumbers() {
        return "applyNumbers";
    }

    @Override
    protected String fourtyTwo() {
        return "fourtyTwo";
    }

    @Override
    protected String mimeType() {
        return "application/x-ruby";
    }

    @Override
    protected String returnsNull() {
        return "retNil";
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
