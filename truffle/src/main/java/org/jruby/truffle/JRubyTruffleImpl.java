/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
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
import org.jruby.Ruby;
import org.jruby.JRubyTruffleInterface;
import org.jruby.ast.RootNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.RubyLanguage;

import java.io.IOException;

public class JRubyTruffleImpl implements JRubyTruffleInterface {

    private final PolyglotEngine engine;
    private final RubyContext context;

    // Run by reflection from Ruby#loadTruffle
    public JRubyTruffleImpl(Ruby runtime) {
        engine = PolyglotEngine.buildNew().globalSymbol(JRubyTruffleInterface.RUNTIME_SYMBOL, new RubyLanguage.JRubyContextWrapper(runtime)).build();

        try {
            context = (RubyContext) engine.eval(Source.fromText("Truffle::Primitive.context", "context").withMimeType(RubyLanguage.MIME_TYPE)).get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object execute(RootNode rootNode) {
        return context.execute(rootNode);
    }

    @Override
    public void dispose() {
        engine.dispose();
    }
}
