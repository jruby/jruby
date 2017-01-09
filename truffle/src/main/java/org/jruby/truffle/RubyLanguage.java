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
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.core.kernel.TraceManager;
import org.jruby.truffle.language.LazyRubyRootNode;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.stdlib.CoverageManager;

import java.io.IOException;

@TruffleLanguage.Registration(
        name = "Ruby",
        version = RubyLanguage.RUBY_VERSION,
        mimeType = RubyLanguage.MIME_TYPE)
@ProvidedTags({
        CoverageManager.LineTag.class,
        TraceManager.CallTag.class,
        TraceManager.ClassTag.class,
        TraceManager.LineTag.class,
        StandardTags.RootTag.class,
        StandardTags.StatementTag.class,
        StandardTags.CallTag.class
})
public class RubyLanguage extends TruffleLanguage<RubyContext> {

    public static final String PLATFORM = "java";
    public static final String RUBY_VERSION = "2.3.1";
    public static final int    RUBY_REVISION = 54768;
    public static final String COMPILE_DATE = "2016-12-03";
    public static final String VERSION = "9.1.7.0-SNAPSHOT";
    public static final String ENGINE = "jruby+truffle";

    public static final String MIME_TYPE = "application/x-ruby";
    public static final String EXTENSION = ".rb";

    public static final String CEXT_MIME_TYPE = "application/x-sulong-library";
    public static final String CEXT_EXTENSION = ".su";

    private RubyLanguage() {
    }

    public static final RubyLanguage INSTANCE = new RubyLanguage();

    @CompilerDirectives.TruffleBoundary
    public static String fileLine(SourceSection section) {
        if (section == null) {
            return "unknown";
        } else {
            final Source source = section.getSource();

            if (section.isAvailable()) {
                return String.format("%s:%d", source.getName(), section.getStartLine());
            } else {
                return source.getName();
            }
        }
    }

    @Override
    public RubyContext createContext(Env env) {
        // TODO CS 3-Dec-16 need to parse RUBY_OPT here if it hasn't been already?
        return new RubyContext(env);
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

    // @Override in Truffle 0.22
    protected Object findMetaObject(RubyContext context, Object value) {
        return context.getCoreLibrary().getMetaClass(value);
    }

    // @Override in Truffle 0.22
    protected SourceSection findSourceLocation(RubyContext context, Object value) {
        if (RubyGuards.isRubyModule(value)) {
            return Layouts.CLASS.getFields((DynamicObject) value).getSourceSection();
        } else if (RubyGuards.isRubyMethod(value)) {
            return Layouts.METHOD.getMethod((DynamicObject) value).getSharedMethodInfo().getSourceSection();
        } else if (RubyGuards.isRubyUnboundMethod(value)) {
            return Layouts.UNBOUND_METHOD.getMethod((DynamicObject) value).getSharedMethodInfo().getSourceSection();
        } else if (RubyGuards.isRubyProc(value)) {
            return Layouts.PROC.getMethod((DynamicObject) value).getSharedMethodInfo().getSourceSection();
        } else {
            return null;
        }
    }

    public Node unprotectedCreateFindContextNode() {
        return super.createFindContextNode();
    }

    public RubyContext unprotectedFindContext(Node node) {
        return super.findContext(node);
    }

}
