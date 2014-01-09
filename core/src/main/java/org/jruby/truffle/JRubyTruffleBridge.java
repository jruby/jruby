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
import org.jruby.ast.Node;
import org.jruby.ast.ArgsNode;
import org.jruby.javasupport.JavaUtil;
import org.jruby.truffle.nodes.core.CoreMethodNodeManager;
import org.jruby.truffle.nodes.methods.MethodDefinitionNode;
import org.jruby.truffle.parser.JRubyParser;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.*;
import org.jruby.truffle.runtime.core.array.RubyArray;

import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;

import java.io.Reader;

public class JRubyTruffleBridge {

    private final Ruby runtime;
    private final RubyContext truffleContext;

    public JRubyTruffleBridge(Ruby runtime) {
        assert runtime != null;

        this.runtime = runtime;

        // Set up a context

        truffleContext = new RubyContext(runtime, new JRubyParser(runtime));
    }

    public void init() {
        // Bring in core method nodes

        CoreMethodNodeManager.addMethods(truffleContext.getCoreLibrary().getObjectClass());

        // Give the core library manager a chance to tweak some of those methods

        truffleContext.getCoreLibrary().initializeAfterMethodsAdded();

        // Set program arguments

        for (IRubyObject arg : ((org.jruby.RubyArray) runtime.getObject().getConstant("ARGV")).toJavaArray()) {
            assert arg != null;

            truffleContext.getCoreLibrary().getArgv().push(truffleContext.makeString(arg.toString()));
        }

        // Set the load path

        final RubyArray loadPath = (RubyArray) truffleContext.getCoreLibrary().getGlobalVariablesObject().getInstanceVariable("$:");

        for (IRubyObject path : ((org.jruby.RubyArray) runtime.getLoadService().getLoadPath()).toJavaArray()) {
            loadPath.push(truffleContext.makeString(path.toString()));
        }
    }

    public TruffleMethod truffelize(ArgsNode argsNode, Node bodyNode) {
        final MethodDefinitionNode methodDefinitionNode = truffleContext.getParser().parse(truffleContext, argsNode, bodyNode);
        return new TruffleMethod(methodDefinitionNode.getCallTarget());
    }

    public Object execute(RubyParser.ParserContext parserContext, Object self, MaterializedFrame parentFrame, org.jruby.ast.RootNode rootNode) {
        try {
            final RubyParserResult parseResult = truffleContext.getParser().parse(truffleContext, getTruffleSource(rootNode), parserContext, parentFrame, rootNode);
            final CallTarget callTarget = Truffle.getRuntime().createCallTarget(parseResult.getRootNode(), parseResult.getFrameDescriptor());

            final RubyArguments arguments = new RubyArguments(parentFrame, self, null);
            return callTarget.call(null, arguments);
        } catch (RaiseException e) {
            throw e;
        } catch (ThrowException e) {
            throw new RaiseException(truffleContext.getCoreLibrary().nameErrorUncaughtThrow(e.getTag()));
        } catch (BreakShellException | QuitException e) {
            throw e;
        } catch (Throwable e) {
            e.printStackTrace();
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
            return runtime.getNil();
        } else {
            return JavaUtil.convertJavaToUsableRubyObject(runtime, object);
        }
    }

    public Object toTruffle(IRubyObject object) {
        if (object == runtime.getTopSelf()) {
            return truffleContext.getCoreLibrary().getMainObject();
        }

        if (object instanceof org.jruby.RubyFixnum) {
            final long value = ((org.jruby.RubyFixnum) object).getLongValue();

            if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
                throw new UnsupportedOperationException();
            }

            return (int) value;
        }

        throw new UnsupportedOperationException(object.getClass().toString());
    }

    public void shutdown() {
        truffleContext.shutdown();
    }

}
