/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle;

import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.PolyglotEngine;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.language.RubyRootNode;
import org.jruby.truffle.options.OptionsCatalog;
import org.jruby.truffle.parser.ParserContext;
import org.jruby.truffle.parser.TranslatorDriver;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;

public abstract class RubyTest {

    protected <T extends Node> void testWithNode(String text, Class<T> nodeClass, Consumer<T> test) {
        testWithAST(text, (root) -> {
            final List<T> instances = NodeUtil.findAllNodeInstances(root, nodeClass);
            assertEquals(1, instances.size());
            final T node = instances.get(0);
            test.accept(node);
        });
    }

    protected void testWithAST(String text, Consumer<RubyRootNode> test) {
        final Source source = Source.newBuilder(text).name("test.rb").mimeType(RubyLanguage.MIME_TYPE).build();

        testInEngine((context) -> {
            final TranslatorDriver translator = new TranslatorDriver(context);
            final RubyRootNode rootNode = translator.parse(context, source, UTF8Encoding.INSTANCE, ParserContext.TOP_LEVEL, null, null, null, true, null);
            rootNode.adoptChildren();
            test.accept(rootNode);
        });
    }

    protected void testInEngine(Consumer<RubyContext> test) {
        final PolyglotEngine engine = PolyglotEngine.newBuilder()
                .config(RubyLanguage.MIME_TYPE, OptionsCatalog.EXCEPTIONS_TRANSLATE_ASSERT.getName(), false)
                .config(RubyLanguage.MIME_TYPE, OptionsCatalog.BASICOPS_INLINE.getName(), false)
                .globalSymbol("action", JavaInterop.asTruffleFunction(Runnable.class, () ->
                    test.accept(RubyContext.getInstance())
                )).build();

        engine.eval(Source.newBuilder("Truffle::Interop.import('action').call").name("test.rb").mimeType(RubyLanguage.MIME_TYPE).build());
    }

}
