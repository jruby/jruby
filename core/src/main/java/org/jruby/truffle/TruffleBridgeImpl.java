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
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.MaterializedFrame;
import org.jruby.*;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.Node;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.TruffleBridge;
import org.jruby.truffle.nodes.core.CoreMethodNodeManager;
import org.jruby.truffle.nodes.methods.MethodDefinitionNode;
import org.jruby.truffle.runtime.NilPlaceholder;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.RubyParserResult;
import org.jruby.truffle.runtime.control.*;
import org.jruby.truffle.runtime.core.array.RubyArray;
import org.jruby.truffle.translator.TranslatorDriver;
import org.jruby.util.cli.Options;

public class TruffleBridgeImpl implements TruffleBridge {

    private final Ruby runtime;
    private final RubyContext truffleContext;

    public TruffleBridgeImpl(Ruby runtime) {
        assert runtime != null;

        this.runtime = runtime;

        // Set up a context

        truffleContext = new RubyContext(runtime, new TranslatorDriver(runtime));
    }

    @Override public void init() {
        if (Options.TRUFFLE_PRINT_RUNTIME.load()) {
            runtime.getInstanceConfig().getError().println("jruby: using " + Truffle.getRuntime().getName());
        }

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

    @Override public TruffleMethod truffelize(DynamicMethod originalMethod, ArgsNode argsNode, Node bodyNode) {
        final MethodDefinitionNode methodDefinitionNode = truffleContext.getTranslator().parse(truffleContext, argsNode, bodyNode);
        return new TruffleMethod(originalMethod, methodDefinitionNode.getCallTarget());
    }

    @Override public Object execute(TranslatorDriver.ParserContext parserContext, Object self, MaterializedFrame parentFrame, org.jruby.ast.RootNode rootNode) {
        try {
            final RubyParserResult parseResult = truffleContext.getTranslator().parse(truffleContext, truffleContext.getSourceManager().get(rootNode.getPosition().getFile()), parserContext, parentFrame, rootNode);
            final CallTarget callTarget = Truffle.getRuntime().createCallTarget(parseResult.getRootNode());

            final RubyArguments arguments = new RubyArguments(parentFrame, self, null);
            return callTarget.call(null, arguments);
        } catch (ThrowException e) {
            throw new RaiseException(truffleContext.getCoreLibrary().nameErrorUncaughtThrow(e.getTag()));
        } catch (RaiseException | BreakShellException | QuitException e) {
            throw e;
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RaiseException(ExceptionTranslator.translateException(truffleContext, e));
        }
    }

    @Override public IRubyObject toJRuby(Object object) {
        if (object instanceof NilPlaceholder) {
            return runtime.getNil();
        } else if (object == truffleContext.getCoreLibrary().getKernelModule()) {
            return runtime.getKernel();
        } else if (object == truffleContext.getCoreLibrary().getMainObject()) {
            return runtime.getTopSelf();
        } else if (object instanceof Boolean) {
            return runtime.newBoolean((boolean) object);
        } else if (object instanceof Integer) {
            return runtime.newFixnum((int) object);
        } else if (object instanceof Double) {
            return runtime.newFloat((double) object);
        } else {
            return runtime.getTopSelf();
        }
    }

    @Override public Object toTruffle(IRubyObject object) {
        if (object == runtime.getTopSelf()) {
            return truffleContext.getCoreLibrary().getMainObject();
        } else if (object == runtime.getKernel()) {
            return truffleContext.getCoreLibrary().getKernelModule();
        } else if (object instanceof RubyNil) {
            return NilPlaceholder.INSTANCE;
        } else if (object instanceof RubyBoolean.True) {
            return true;
        } else if (object instanceof RubyBoolean.False) {
            return false;
        } else if (object instanceof RubyFixnum) {
            final long value = ((RubyFixnum) object).getLongValue();

            if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
                throw new UnsupportedOperationException();
            }

            return (int) value;
        } else if (object instanceof RubyFloat) {
            return ((RubyFloat) object).getDoubleValue();
        } else {
            throw object.getRuntime().newRuntimeError("cannot pass " + object.inspect() + " to Truffle");
        }
    }

    @Override public void shutdown() {
        truffleContext.shutdown();
    }

}
