/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
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

import org.jruby.truffle.RubyLanguage;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import static org.junit.Assert.*;

public class RubyTckTest extends TruffleTCK {

    private static Source getSource(String path) {
        InputStream stream = ClassLoader.getSystemResourceAsStream(path);
        Reader reader = new InputStreamReader(stream);
        try {
            return Source.newBuilder(reader).name(new File(path).getName()).mimeType(RubyLanguage.MIME_TYPE).build();
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    private static PolyglotEngine engine;

    @Test
    public void checkVM() {
        PolyglotEngine engine = PolyglotEngine.newBuilder().build();
        assertNotNull(engine.getLanguages().get(mimeType()));
    }

    @Override
    protected synchronized PolyglotEngine prepareVM() throws Exception {
        if (engine == null) {
            engine = PolyglotEngine.newBuilder().build();
            engine.eval(getSource("src/test/ruby/tck.rb"));
        }

        return engine;
    }

    @Override
    protected PolyglotEngine prepareVM(PolyglotEngine.Builder preparedBuilder) throws Exception {
        final PolyglotEngine engine = preparedBuilder.build();
        engine.eval(getSource("src/test/ruby/tck.rb"));
        return engine;
    }

    @Override
    protected String mimeType() {
        return RubyLanguage.MIME_TYPE;
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
    protected String complexAddWithMethod() {
        return "complex_add_with_method";
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
    protected String valuesObject() {
        return "values_object";
    }

    @Override
    protected String addToArray() {
        return "add_array";
    }

    @Override
    protected String countUpWhile() {
        return "count_up_while";
    }

    @Override
    protected String objectWithElement() {
        return "object_with_element";
    }

    @Override
    protected String objectWithValueProperty() {
        return "object_with_value_property";
    }

    @Override
    protected String functionAddNumbers() {
        return "function_add_numbers";
    }

    @Override
    protected String objectWithValueAndAddProperty() {
        return "object_with_value_and_add_property";
    }

    @Override
    protected String callFunction() {
        return "call_function";
    }

    @Override
    protected String callMethod() {
        return "call_method";
    }

    @Override
    protected String readValueFromForeign() {
        return "read_value_from_foreign";
    }

    @Override
    protected String readElementFromForeign() {
        return "read_element_from_foreign";
    }

    @Override
    protected String writeValueToForeign() {
        return "write_value_to_foreign";
    }

    @Override
    protected String writeElementToForeign() {
        return "write_element_to_foreign";
    }

    @Override
    protected String getSizeOfForeign() {
        return "get_size_of_foreign";
    }

    @Override
    protected String hasSizeOfForeign() {
        return "has_size_of_foreign";
    }

    @Override
    protected String isNullForeign() {
        return "is_null_foreign";
    }

    @Override
    protected String isExecutableOfForeign() {
        return "is_executable_of_foreign";
    }

    @Override
    public void readWriteCharValue() throws Exception {
        // Skipped for now
    }

    @Override
    public void testCoExistanceOfMultipleLanguageInstances() throws Exception {
        /*
         * Not running this test as it clears the engine, but we're caching that globally to avoid creating tens of
         * engines concurrently.
         */
    }

}
