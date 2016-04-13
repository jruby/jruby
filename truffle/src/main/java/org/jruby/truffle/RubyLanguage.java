/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.instrument.Visualizer;
import com.oracle.truffle.api.instrument.WrapperNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.runtime.Constants;
import org.jruby.truffle.instrument.RubyWrapperNode;
import org.jruby.truffle.interop.JRubyContextWrapper;
import org.jruby.truffle.language.LazyRubyRootNode;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;

import java.io.IOException;

@TruffleLanguage.Registration(
        name = "Ruby",
        version = Constants.RUBY_VERSION,
        mimeType = RubyLanguage.MIME_TYPE)
public class RubyLanguage extends TruffleLanguage<RubyContext> {

    public static final String MIME_TYPE = "application/x-ruby";
    public static final String EXTENSION = ".rb";

    private RubyLanguage() {
    }

    public static final RubyLanguage INSTANCE = new RubyLanguage();

    @Override
    public RubyContext createContext(Env env) {
        final JRubyContextWrapper runtimeWrapper = (JRubyContextWrapper) env.importSymbol(JRubyTruffleImpl.RUNTIME_SYMBOL);

        final Ruby runtime;

        if (runtimeWrapper == null) {
            RubyInstanceConfig config = new RubyInstanceConfig();
            config.setCompileMode(RubyInstanceConfig.CompileMode.TRUFFLE);
            runtime = Ruby.newInstance(config);
        } else {
            runtime = runtimeWrapper.getRuby();
        }

        return new RubyContext(runtime, env);
    }

    @Override
    protected void disposeContext(RubyContext context) {
        context.shutdown();
    }

    @Override
    protected CallTarget parse(Source source, Node node, String... argumentNames) throws IOException {
        return Truffle.getRuntime().createCallTarget(new LazyRubyRootNode(null, null, source, argumentNames));
    }

    @Override
    protected Object findExportedSymbol(RubyContext context, String s, boolean b) {
        return context.getInteropManager().findExportedObject(s);
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
    protected Visualizer getVisualizer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isInstrumentable(Node node) {
        return !(node instanceof RubyWrapperNode);
    }

    @Override
    protected WrapperNode createWrapperNode(Node node) {
        return new RubyWrapperNode((RubyNode) node);
    }

    @Override
    protected Object evalInContext(Source source, Node node, MaterializedFrame mFrame) throws IOException {
        return null;
    }

    @Override
    protected String toString(RubyContext context, Object value) {
        if (value == null) {
            return "<null>";
        } else if (RubyGuards.isBoxedPrimitive(value) ||  RubyGuards.isRubyBasicObject(value)) {
            return context.send(value, "inspect", null).toString();
        } else if (value instanceof String) {
            return (String) value;
        } else {
            return "<foreign>";
        }
    }

    public Node unprotectedCreateFindContextNode() {
        return super.createFindContextNode();
    }

    public RubyContext unprotectedFindContext(Node node) {
        return super.findContext(node);
    }

}
