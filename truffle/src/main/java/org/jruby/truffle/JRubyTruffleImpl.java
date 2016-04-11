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

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.PolyglotEngine;
import org.jruby.JRubyTruffleInterface;
import org.jruby.Ruby;
import org.jruby.truffle.interop.JRubyContextWrapper;
import org.jruby.truffle.language.control.ExitException;
import org.jruby.truffle.platform.Graal;
import org.jruby.util.cli.Options;

import java.io.IOException;

public class JRubyTruffleImpl implements JRubyTruffleInterface {

    private final PolyglotEngine engine;
    private final RubyContext context;

    // Created by reflection from Ruby#loadTruffle

    public JRubyTruffleImpl(Ruby runtime) {
        try {
            Class.forName("com.oracle.truffle.llvm.LLVM", true, ClassLoader.getSystemClassLoader());
        } catch (ClassNotFoundException e) {
            // Sulong may not be on the classpath, or if it genuinely failed we'll be happy with the exception later on
        }

        engine = PolyglotEngine.newBuilder()
                .globalSymbol(JRubyTruffleInterface.RUNTIME_SYMBOL, new JRubyContextWrapper(runtime))
                .build();

        try {
            context = (RubyContext) engine.eval(Source.fromText("Truffle::Primitive.context", "context")
                    .withMimeType(RubyLanguage.MIME_TYPE)).get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object execute(org.jruby.ast.RootNode rootNode) {
        if (!Graal.isGraal() && Options.TRUFFLE_GRAAL_WARNING_UNLESS.load()) {
            System.err.println("WARNING: This JVM does not have the Graal compiler. " +
                    "JRuby+Truffle's performance without it will be limited. " +
                    "See https://github.com/jruby/jruby/wiki/Truffle-FAQ#how-do-i-get-jrubytruffle");
        }

        context.setInitialJRubyRootNode(rootNode);

        try {
            return engine.eval(Source.fromText("Truffle::Primitive.run_jruby_root", "run_jruby_root")
                    .withMimeType(RubyLanguage.MIME_TYPE)).get();
        } catch (IOException e) {
            if (e.getCause() instanceof ExitException) {
                final ExitException exit = (ExitException) e.getCause();
                throw new org.jruby.exceptions.MainExitException(exit.getCode());
            }

            throw new RuntimeException(e);
        }
    }

    @Override
    public void dispose() {
        engine.dispose();
    }
    
}
