/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Represents the Ruby {@code File} class.
 */
public class RubyFile extends RubyBasicObject {

    private final Reader reader;
    private final Writer writer;

    public RubyFile(RubyClass rubyClass, Reader reader, Writer writer) {
        super(rubyClass);
        this.reader = reader;
        this.writer = writer;
    }

    public void close() {
        RubyNode.notDesignedForCompilation("d4a1f84d684d44b288ff8d3dd7204460");

        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static String expandPath(RubyContext context, String fileName) {
        RubyNode.notDesignedForCompilation("4c6bd8a5f04d459eb2a1a2861a32559e");

        // TODO (nirvdrum 11-Feb-15) This needs to work on Windows without calling into non-Truffle JRuby.
        if (context.isRunningOnWindows()) {
            final org.jruby.RubyString path = context.toJRuby(context.makeString(fileName));
            final org.jruby.RubyString expanded = (org.jruby.RubyString) org.jruby.RubyFile.expand_path19(
                    context.getRuntime().getCurrentContext(),
                    null,
                    new org.jruby.runtime.builtin.IRubyObject[] { path });

            return expanded.asJavaString();
        } else {
            return expandPath(fileName, null);
        }
    }

    public static String expandPath(String fileName, String dir) {
        RubyNode.notDesignedForCompilation("eef172efc44a407eb63ecf24e3d18296");

        /*
         * TODO(cs): this isn't quite correct - I think we want to collapse .., but we don't want to
         * resolve symlinks etc. This might be where we want to start borrowing JRuby's
         * implementation, but it looks quite tied to their data structures.
         */

        return org.jruby.RubyFile.canonicalize(new File(dir, fileName).getAbsolutePath());
    }

    public static RubyFile open(RubyContext context, String fileName, String mode) {
        RubyNode.notDesignedForCompilation("56b832a968c241dbb9119014fde9e9f3");

        Reader reader;
        Writer writer;

        if (mode.equals("r") || mode.equals("rb")) {
            try {
                reader = new InputStreamReader(new FileInputStream(fileName), StandardCharsets.UTF_8);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }

            writer = null;
        } else if (mode.equals("w") || mode.equals("wb")) {
            reader = null;

            try {
                writer = new OutputStreamWriter(new FileOutputStream(fileName), StandardCharsets.UTF_8);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else if (mode.equals("a") || mode.equals("ab")) {
            reader = null;

            try {
                writer = new OutputStreamWriter(new FileOutputStream(fileName, true), StandardCharsets.UTF_8);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new UnsupportedOperationException();
        }

        final RubyFile file = new RubyFile(context.getCoreLibrary().getFileClass(), reader, writer);

        return file;
    }

    public Reader getReader() {
        return reader;
    }

    public Writer getWriter() {
        return writer;
    }

}
