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

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.tck.TruffleTCK;

import org.jruby.truffle.runtime.RubyLanguage;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class RubyTckTest extends TruffleTCK {

    @Test
    public void checkVM() {
        PolyglotEngine engine = PolyglotEngine.buildNew().build();
        assertNotNull(engine.getLanguages().get(mimeType()));
    }

    @Override
    protected PolyglotEngine prepareVM() throws Exception {
        final Source source = Source.fromText(
                "def sum(a, b)\n"
                        + "  a + b\n"
                        + "end\n"
                        + "def fourty_two\n"
                        + "  42\n"
                        + "end\n"
                        + "def ret_nil\n"
                        + "  nil\n"
                        + "end\n"
                        + "$invocations = 0\n"
                        + "def count_invocations\n"
                        + "  $invocations += 1\n"
                        + "end\n"
                        + "def apply_numbers(f)\n"
                        + "  Truffle::Interop.execute(f, 18, 32) + 10\n"
                        + "end\n"
                        + "def compound_object\n"
                        + "  obj = Object.new\n"
                        + "  def obj.fourtyTwo; 42; end\n"
                        + "  def obj.plus(a, b); a + b; end\n"
                        + "  def obj.returnsNull; nil; end\n"
                        + "  def obj.returnsThis; self; end\n"
                        + "  obj\n"
                        + "end\n"
                        + "def identity(value)\n"
                        + "  value\n"
                        + "end\n"
                        + "Truffle::Interop.export(\"sum_ints\", method(:sum))\n"
                        + "Truffle::Interop.export(\"fourty_two\", method(:fourty_two))\n"
                        + "Truffle::Interop.export(\"ret_nil\", method(:ret_nil))\n"
                        + "Truffle::Interop.export(\"count_invocations\", method(:count_invocations))\n"
                        + "Truffle::Interop.export(\"apply_numbers\", method(:apply_numbers))\n"
                        + "Truffle::Interop.export(\"compound_object\", method(:compound_object))\n"
                        + "Truffle::Interop.export(\"identity\", method(:identity))\n",
                "test").withMimeType(mimeType());
        PolyglotEngine engine = PolyglotEngine.buildNew().build();
        engine.eval(source);
        return engine;
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
        return RubyLanguage.MIME_TYPE;
    }

    @Override
    protected String returnsNull() {
        return "ret_nil";
    }

    @Override
    protected String countInvocations() {
        return "count_invocations";
    }

    @Override
    protected String invalidCode() {
        return "def something\n  ret urn 4.2\ne n d";
    }

    @Override
    protected String compoundObject() {
        return "compound_object";
    }

    @Override
    protected String identity() {
        return "identity";
    }

    @Override
    protected String globalObject() {
        return null;
    }

}
