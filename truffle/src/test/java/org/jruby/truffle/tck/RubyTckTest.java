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
        PolyglotEngine engine = PolyglotEngine.newBuilder().build();
        assertNotNull(engine.getLanguages().get(mimeType()));
    }

    @Override
    protected PolyglotEngine prepareVM() throws Exception {
        PolyglotEngine engine = PolyglotEngine.newBuilder().build();
        engine.eval(Source.fromFileName("src/test/ruby/tck.rb"));
        return engine;
    }

    @Override
    protected String plusInt() {
        return "plus_int";
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

    @Override
    protected String multiplyCode(String firstName, String secondName) {
        return firstName + " * " + secondName;
    }

    @Override
    protected String evaluateSource() {
        return "evaluate_source";
    }

    @Override
    protected String complexAdd() {
        return "complex_add";
    }

    @Override
    protected String complexSumReal() {
        return "complex_sum_real";
    }

    @Override
    protected String complexCopy() {
        return "complex_copy";
    }

    @Override
    public void testSumRealOfComplexNumbersAsStructuredDataRowBased() throws Exception {
        // Ignored temporarily
    }

    @Override
    public void testSumRealOfComplexNumbersA() throws Exception {
        // Ignored temporarily
    }

    @Override
    public void testSumRealOfComplexNumbersB() throws Exception {
        // Ignored temporarily
    }

    @Override
    public void testSumRealOfComplexNumbersAsStructuredDataColumnBased() throws Exception {
        // Ignored temporarily
    }

    @Override
    public void testCopyComplexNumbersA() throws Exception {
        // Ignored temporarily
    }

    @Override
    public void testCopyComplexNumbersB() throws Exception {
        // Ignored temporarily
    }

    @Override
    public void testCopyStructuredComplexToComplexNumbersA() throws Exception {
        // Ignored temporarily
    }

    }
