/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.debug.DebugSupportProvider;
import com.oracle.truffle.api.instrument.ToolSupportProvider;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import org.jruby.Ruby;
import org.jruby.runtime.Constants;
import org.jruby.truffle.nodes.LazyRubyRootNode;

import java.io.IOException;

@TruffleLanguage.Registration(name = "Ruby", version = Constants.RUBY_VERSION, mimeType = "application/x-ruby")
public class RubyLanguage extends TruffleLanguage<RubyContext> {

    private RubyLanguage() {
    }

    public static final RubyLanguage INSTANCE = new RubyLanguage();

    @Override
    public RubyContext createContext(Env env) {
        Ruby r = Ruby.newInstance();
        return new RubyContext(r, env);
    }

    @Override
    protected CallTarget parse(Source source, Node node, String... strings) throws IOException {
        return Truffle.getRuntime().createCallTarget(new LazyRubyRootNode(null, null, source));
    }

    @Override
    protected Object findExportedSymbol(RubyContext context, String s, boolean b) {
        return context.findExportedObject(s);
    }

    @Override
    protected Object getLanguageGlobal(RubyContext context) {
        return context.getCoreLibrary().getObjectClass();
    }

    @Override
    protected boolean isObjectOfLanguage(Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected ToolSupportProvider getToolSupport() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected DebugSupportProvider getDebugSupport() {
        return null;
    }

    public Node unprotectedCreateFindContextNode() {
        return super.createFindContextNode();
    }

    public RubyContext unprotectedFindContext(Node node) {
        return super.findContext(node);
    }

}
