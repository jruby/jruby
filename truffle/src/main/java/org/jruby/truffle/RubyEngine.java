/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.PolyglotEngine;
import org.jruby.truffle.options.OptionsCatalog;
import org.jruby.truffle.platform.graal.Graal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class RubyEngine {

    private final PolyglotEngine engine;
    private final RubyContext context;

    public RubyEngine(
            String home,
            String[] loadPaths,
            String[] requiredLibraries,
            byte[] inlineScript,
            String[] arguments,
            String displayedFileName,
            boolean debug,
            int verbosity,
            boolean frozenStringLiterals,
            boolean disabledGems,
            String internalEncoding,
            String externalEncoding) {
        engine = PolyglotEngine.newBuilder()
                .config(RubyLanguage.MIME_TYPE, OptionsCatalog.HOME.getName(), home)
                .config(RubyLanguage.MIME_TYPE, OptionsCatalog.LOAD_PATHS.getName(), loadPaths)
                .config(RubyLanguage.MIME_TYPE, OptionsCatalog.REQUIRED_LIBRARIES.getName(), requiredLibraries)
                .config(RubyLanguage.MIME_TYPE, OptionsCatalog.INLINE_SCRIPT.getName(), inlineScript)
                .config(RubyLanguage.MIME_TYPE, OptionsCatalog.ARGUMENTS.getName(), arguments)
                .config(RubyLanguage.MIME_TYPE, OptionsCatalog.DISPLAYED_FILE_NAME.getName(), displayedFileName)
                .config(RubyLanguage.MIME_TYPE, OptionsCatalog.DEBUG.getName(), debug)
                .config(RubyLanguage.MIME_TYPE, OptionsCatalog.VERBOSITY.getName(), verbosity)
                .config(RubyLanguage.MIME_TYPE, OptionsCatalog.FROZEN_STRING_LITERALS.getName(), frozenStringLiterals)
                .config(RubyLanguage.MIME_TYPE, OptionsCatalog.DISABLE_GEMS.getName(), disabledGems)
                .config(RubyLanguage.MIME_TYPE, OptionsCatalog.INTERNAL_ENCODING.getName(), internalEncoding)
                .config(RubyLanguage.MIME_TYPE, OptionsCatalog.EXTERNAL_ENCODING.getName(), externalEncoding)
                .build();

        Main.printTruffleTimeMetric("before-load-context");
        context = engine.eval(loadSource("Truffle::Boot.context", "context")).as(RubyContext.class);
        Main.printTruffleTimeMetric("after-load-context");
    }

    public int execute(String path) {
        if (!Graal.isGraal() && context.getOptions().GRAAL_WARNING_UNLESS) {
            Log.warning("This JVM does not have the Graal compiler - performance will be limited - " +
                    "see https://github.com/jruby/jruby/wiki/Truffle-FAQ#how-do-i-get-jrubytruffle");
        }

        context.setOriginalInputFile(path);

        return engine.eval(loadSource("Truffle::Boot.main", "main")).as(Integer.class);
    }

    public RubyContext getContext() {
        return context;
    }

    public void dispose() {
        engine.dispose();
    }

    @TruffleBoundary
    private Source loadSource(String source, String name) {
        return Source.newBuilder(source).name(name).mimeType(RubyLanguage.MIME_TYPE).build();
    }

    public int checkSyntax(InputStream in, String filename) {
        // check primary script
        boolean status = runCheckSyntax(in, filename);

        // check other scripts specified on argv
        for (String arg : context.getOptions().ARGUMENTS) {
            status = status && checkFileSyntax(arg);
        }

        return status ? 0 : -1;
    }

    private boolean checkFileSyntax(String filename) {
        File file = new File(filename);
        if (file.exists()) {
            try {
                return runCheckSyntax(new FileInputStream(file), filename);
            } catch (FileNotFoundException fnfe) {
                System.err.println("File not found: " + filename);
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean runCheckSyntax(InputStream in, String filename) {
        context.setSyntaxCheckInputStream(in);
        context.setOriginalInputFile(filename);

        return engine.eval(loadSource("Truffle::Boot.check_syntax", "check_syntax")).as(Boolean.class);
    }

}
