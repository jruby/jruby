/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Source;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.source.SourceManager;
import org.jruby.truffle.nodes.core.CoreMethodNodeManager;
import org.jruby.truffle.parser.JRubyParser;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.configuration.Configuration;
import org.jruby.truffle.runtime.configuration.ConfigurationBuilder;
import org.jruby.truffle.runtime.control.*;
import org.jruby.truffle.runtime.core.array.RubyArray;

import org.jruby.Ruby;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.io.Reader;

public class JRubyTruffleBridge {

    private final ThreadContext jrubyContext;
    private final RubyContext truffleContext;

    public JRubyTruffleBridge(ThreadContext jrubyContext) {
        assert jrubyContext != null;

        this.jrubyContext = jrubyContext;

        // Override the home directory if RUBYHOME is set

        final ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();

        if (System.getenv("RUBYHOME") != null) {
            configurationBuilder.setStandardLibrary(System.getenv("RUBYHOME") + "/" + ConfigurationBuilder.JRUBY_STDLIB_JAR);
        }

        // Set up a context

        truffleContext = new RubyContext(new Configuration(configurationBuilder), new JRubyParser(jrubyContext.getRuntime()));

        // Bring in core method nodes

        CoreMethodNodeManager.addMethods(truffleContext.getCoreLibrary().getObjectClass());

        // Give the core library manager a chance to tweak some of those methods

        truffleContext.getCoreLibrary().initializeAfterMethodsAdded();

        // Set program arguments

        for (IRubyObject arg : ((org.jruby.RubyArray) jrubyContext.getRuntime().getObject().getConstant("ARGV")).toJavaArray()) {
            truffleContext.getCoreLibrary().getArgv().push(truffleContext.makeString(arg.toString()));
        }

        // Set the load path

        final RubyArray loadPath = (RubyArray) truffleContext.getCoreLibrary().getGlobalVariablesObject().getInstanceVariable("$:");

        for (IRubyObject path : ((org.jruby.RubyArray) jrubyContext.getRuntime().getLoadService().getLoadPath()).toJavaArray()) {
            loadPath.push(truffleContext.makeString(path.toString()));
        }
    }

    public Object execute(RubyParser.ParserContext parserContext, Object self, MaterializedFrame parentFrame, org.jruby.ast.RootNode rootNode) {
        try {
            final RubyParserResult parseResult = truffleContext.getParser().parse(truffleContext, getTruffleSource(rootNode), parserContext, parentFrame, rootNode);
            final RubyArguments arguments = new RubyArguments(parentFrame, self, null);
            final CallTarget callTarget = Truffle.getRuntime().createCallTarget(parseResult.getRootNode(), parseResult.getFrameDescriptor());

            return callTarget.call(null, arguments);
        } catch (RaiseException e) {
            throw e;
        } catch (ThrowException e) {
            if (truffleContext.getConfiguration().getRubyVersion().is18OrEarlier()) {
                throw new RaiseException(truffleContext.getCoreLibrary().nameErrorUncaughtThrow(e.getTag()));
            } else {
                throw new RaiseException(truffleContext.getCoreLibrary().argumentErrorUncaughtThrow(e.getTag()));
            }
        } catch (BreakShellException | QuitException e) {
            throw e;
        } catch (Throwable e) {
            throw new RaiseException(ExceptionTranslator.translateException(truffleContext, e));
        }
    }

    public Source getTruffleSource(org.jruby.ast.Node node) {
        return new SourceManager.SourceImpl() {
            @Override
            protected void reset() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getName() {
                return "(name)";
            }

            @Override
            public String getPath() {
                return "(path)";
            }

            @Override
            public Reader getReader() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getCode() {
                return "(code)";
            }
        };
    }

    public IRubyObject toJRuby(Object object) {
        if (object instanceof NilPlaceholder) {
            return jrubyContext.getRuntime().getNil();
        }

        throw new UnsupportedOperationException(object.getClass().toString());
    }

    public Object toTruffle(IRubyObject object) {
        if (object == jrubyContext.getRuntime().getTopSelf()) {
            return truffleContext.getCoreLibrary().getMainObject();
        }

        throw new UnsupportedOperationException(object.getClass().toString());
    }

    public void shutdown() {
        truffleContext.shutdown();
    }

}
