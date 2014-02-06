/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.test;

import java.io.*;
import java.util.*;

import org.junit.*;
import junit.framework.TestCase;

import com.oracle.truffle.api.*;
import org.jruby.truffle.nodes.core.*;
import org.jruby.truffle.translator.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;

import org.jruby.Ruby;

/**
 * Base class for Ruby tests.
 */
public class RubyTests extends TestCase {

    @BeforeClass
    public static void applyDefaultLocale() {
        // Avoid printing comparison issues
        Locale.setDefault(Locale.ENGLISH);
    }

    /**
     * Executes some Ruby code and asserts that it prints an expected string. Remember to include
     * the newline characters.
     */
    public static void assertPrints(String expectedOutput, String code) {
        assertPrintsWithInput(expectedOutput, code, "", new String[]{});
    }

    /**
     * Executes some Ruby code and asserts that it prints an expected string. Remember to include
     * the newline characters.
     */
    public static void assertPrints(String expectedOutput, String code, String[] args) {
        assertPrintsWithInput(expectedOutput, code, "", args);
    }

    /**
     * Executes some Ruby code and asserts that it prints an expected string. Remember to include
     * the newline characters. Allows input for {@code Kernel#gets} to be passed in.
     */
    public static void assertPrintsWithInput(String expectedOutput, String code, String input, String[] args) {
        assertPrints(expectedOutput, "(test)", code, input, args);
    }

    /**
     * Executes some Ruby code in a file and asserts that it prints an expected string. Remember to
     * include the newline characters.
     */
    public static void assertFilePrints(String expectedOutput, String fileName, String[] args) {
        assertPrints(expectedOutput, fileName, null, "", args);
    }

    /**
     * Executes some Ruby code and asserts that it prints an expected string. Remember to include
     * the newline characters. Also takes a string to simulate input.
     */
    public static void assertPrints(String expectedOutput, String fileName, String code, String input, String... args) {
        final ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        final PrintStream printStream = new PrintStream(byteArray);

        final Ruby ruby = Ruby.newInstance();

        ruby.getInstanceConfig().setOutput(printStream);
        ruby.getInstanceConfig().setInput(new ByteArrayInputStream(input.getBytes()));

        final RubyContext context = new RubyContext(ruby, new TranslatorDriver(ruby));

        CoreMethodNodeManager.addMethods(context.getCoreLibrary().getObjectClass());
        context.getCoreLibrary().initializeAfterMethodsAdded();

        for (String arg : args) {
            context.getCoreLibrary().getArgv().push(new RubyString(context.getCoreLibrary().getStringClass(), arg));
        }

        final Source source = context.getSourceManager().getFakeFile(fileName, code);

        context.execute(context, source, TranslatorDriver.ParserContext.TOP_LEVEL, context.getCoreLibrary().getMainObject(), null);
        context.shutdown();

        assertEquals(expectedOutput, byteArray.toString().replaceAll("\r\n", "\n"));
    }
}
