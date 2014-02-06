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
import com.oracle.truffle.api.SourceSection;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.MaterializedFrame;
import org.jruby.RubyBoolean;
import org.jruby.RubyNil;
import org.jruby.ast.Node;
import org.jruby.ast.ArgsNode;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.truffle.nodes.core.CoreMethodNodeManager;
import org.jruby.truffle.nodes.methods.MethodDefinitionNode;
import org.jruby.truffle.translator.TranslatorDriver;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.*;
import org.jruby.truffle.runtime.core.array.RubyArray;

import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;

import java.io.InputStream;
import java.io.Reader;

public class JRubyTruffleBridge {

    private final Ruby runtime;
    private final RubyContext truffleContext;

    public JRubyTruffleBridge(Ruby runtime) {
        assert runtime != null;

        this.runtime = runtime;

        // Set up a context

        truffleContext = new RubyContext(runtime, new TranslatorDriver(runtime));
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

    public TruffleMethod truffelize(DynamicMethod originalMethod, ArgsNode argsNode, Node bodyNode) {
        final MethodDefinitionNode methodDefinitionNode = truffleContext.getTranslator().parse(truffleContext, argsNode, bodyNode);
        return new TruffleMethod(originalMethod, methodDefinitionNode.getCallTarget());
    }

    public Object execute(TranslatorDriver.ParserContext parserContext, Object self, MaterializedFrame parentFrame, org.jruby.ast.RootNode rootNode) {
        try {
            final RubyParserResult parseResult = truffleContext.getTranslator().parse(truffleContext, DUMMY_SOURCE, parserContext, parentFrame, rootNode);
            final CallTarget callTarget = Truffle.getRuntime().createCallTarget(parseResult.getRootNode());

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

    public IRubyObject toJRuby(Object object) {
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
            throw new UnsupportedOperationException("can't convert " + object.getClass() + " to JRuby");
        }
    }

    public Object toTruffle(IRubyObject object) {
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
        } else if (object instanceof org.jruby.RubyFixnum) {
            final long value = ((org.jruby.RubyFixnum) object).getLongValue();

            if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
                throw new UnsupportedOperationException();
            }

            return (int) value;
        } else if (object instanceof org.jruby.RubyFloat) {
            return ((org.jruby.RubyFloat) object).getDoubleValue();
        } else {
            throw object.getRuntime().newRuntimeError("cannot pass " + object.inspect() + " to Truffle");
        }
    }

    public void shutdown() {
        truffleContext.shutdown();
    }

    public final static Source DUMMY_SOURCE = new Source() {
        @Override
        public String getName() {
            return "(name)";
        }

        @Override
        public String getPath() {
            return "(reader)";
        }

        @Override
        public Reader getReader() {
            throw new UnsupportedOperationException();
        }

        @Override
        public InputStream getInputStream() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getCode() {
            return "(code)";
        }

        @Override
        public String getCode(int i) {
            return "(code)";
        }

        @Override
        public int getLineCount() {
            return 0;
        }

        @Override
        public int getLineNumber(int i) {
            return 0;
        }

        @Override
        public int getLineStartOffset(int i) {
            return 0;
        }

        @Override
        public int getLineLength(int i) {
            return 0;
        }
    };

    public static final SourceSection DUMMY_SOURCE_SECTION = new SourceSection() {
        @Override
        public Source getSource() {
            return DUMMY_SOURCE;
        }

        @Override
        public int getStartLine() {
            return 0;
        }

        @Override
        public int getStartColumn() {
            return 0;
        }

        @Override
        public int getCharIndex() {
            return 0;
        }

        @Override
        public int getCharLength() {
            return 0;
        }

        @Override
        public int getCharEndIndex() {
            return 0;
        }

        @Override
        public String getIdentifier() {
            return "(unknown)";
        }

        @Override
        public String getCode() {
            return "(code)";
        }
    };

}
