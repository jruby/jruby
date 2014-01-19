/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime;

import java.io.*;
import java.math.*;
import java.util.concurrent.atomic.*;

import org.jruby.Ruby;
import jnr.posix.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import org.jruby.truffle.runtime.control.*;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.debug.*;
import org.jruby.truffle.runtime.methods.*;
import org.jruby.truffle.runtime.objects.*;
import org.jruby.truffle.runtime.subsystems.*;
import org.jruby.util.cli.Options;

/**
 * The global state of a running Ruby system.
 */
public class RubyContext implements ExecutionContext {

    private final Ruby runtime;
    private final RubyParser parser;
    private final CoreLibrary coreLibrary;
    private final FeatureManager featureManager;
    private final ObjectSpaceManager objectSpaceManager;
    private final TraceManager traceManager;
    private final ThreadManager threadManager;
    private final FiberManager fiberManager;
    private final AtExitManager atExitManager;
    private final RubyDebugManager debugManager;
    private final SourceManager sourceManager;

    private AtomicLong nextObjectID = new AtomicLong(0);

    public RubyContext(Ruby runtime, RubyParser parser) {
        assert runtime != null;

        this.runtime = runtime;
        this.parser = parser;

        objectSpaceManager = new ObjectSpaceManager(this);
        traceManager = new TraceManager(this);

        // See note in CoreLibrary#initialize to see why we need to break this into two statements
        coreLibrary = new CoreLibrary(this);
        coreLibrary.initialize();

        featureManager = new FeatureManager(this);
        atExitManager = new AtExitManager();
        sourceManager = new SourceManager();

        debugManager = Options.TRUFFLE_DEBUG_NODES.load() ? new RubyDebugManager(this) : null;

        // Must initialize threads before fibers

        threadManager = new ThreadManager(this);
        fiberManager = new FiberManager(this);
    }

    public String getLanguageShortName() {
        return "ruby";
    }

    public RubyDebugManager getDebugManager() {
        return debugManager;
    }

    @Override
    public ASTPrinter getASTPrinter() {
        throw new UnsupportedOperationException();
    }

    public void load(Source source) {
        execute(this, source, RubyParser.ParserContext.TOP_LEVEL, coreLibrary.getMainObject(), null);
    }

    public void loadFile(String fileName) {
        final Source source = sourceManager.get(fileName);
        final String code = source.getCode();
        if (code == null) {
            throw new RuntimeException("Can't read file " + fileName);
        }
        execute(this, source, RubyParser.ParserContext.TOP_LEVEL, coreLibrary.getMainObject(), null);
    }

    /**
     * Receives runtime notification that execution has halted.
     */
    public void haltedAt(Node node, MaterializedFrame frame) {
        runShell(node, frame);
    }

    public Object eval(String code) {
        final Source source = sourceManager.get("(eval)", code);
        return execute(this, source, RubyParser.ParserContext.TOP_LEVEL, coreLibrary.getMainObject(), null);
    }

    public Object eval(String code, RubyModule module) {
        final Source source = sourceManager.get("(eval)", code);
        return execute(this, source, RubyParser.ParserContext.MODULE, module, null);
    }

    public Object eval(String code, RubyBinding binding) {
        final Source source = sourceManager.get("(eval)", code);
        return execute(this, source, RubyParser.ParserContext.TOP_LEVEL, binding.getSelf(), binding.getFrame());
    }

    public void runShell(Node node, MaterializedFrame frame) {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        MaterializedFrame existingLocals = frame;

        String prompt = "Ruby> ";
        if (node != null) {
            final SourceSection src = node.getSourceSection();
            if (src != null) {
                prompt = (src.getSource().getName() + ":" + src.getStartLine() + "> ");
            }
        }

        while (true) {
            try {
                System.out.print(prompt);
                final String line = reader.readLine();

                final ShellResult result = evalShell(line, existingLocals);

                System.out.println("=> " + result.getResult());



                existingLocals = result.getFrame();
            } catch (BreakShellException e) {
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public ShellResult evalShell(String code, MaterializedFrame existingLocals) {
        final Source source = sourceManager.get("(shell)", code);
        return (ShellResult) execute(this, source, RubyParser.ParserContext.SHELL, coreLibrary.getMainObject(), existingLocals);
    }

    public Object execute(RubyContext context, Source source, RubyParser.ParserContext parserContext, Object self, MaterializedFrame parentFrame) {
        try {
            final RubyParserResult parseResult = parser.parse(context, source, parserContext, parentFrame);
            final RubyArguments arguments = new RubyArguments(parentFrame, self, null);
            final CallTarget callTarget = Truffle.getRuntime().createCallTarget(parseResult.getRootNode(), parseResult.getFrameDescriptor());

            return callTarget.call(null, arguments);
        } catch (RaiseException e) {
            throw e;
        } catch (ThrowException e) {
            throw new RaiseException(context.getCoreLibrary().argumentErrorUncaughtThrow(e.getTag()));
        } catch (BreakShellException | QuitException e) {
            throw e;
        } catch (Throwable e) {
            throw new RaiseException(ExceptionTranslator.translateException(this, e));
        }
    }

    public long getNextObjectID() {
        // TODO(CS): We can theoretically run out of long values

        final long id = nextObjectID.getAndIncrement();

        if (id < 0) {
            nextObjectID.set(Long.MIN_VALUE);
            throw new RuntimeException("Object IDs exhausted");
        }

        return id;
    }

    public void shutdown() {
        atExitManager.run();

        threadManager.leaveGlobalLock();

        objectSpaceManager.shutdown();

        if (fiberManager != null) {
            fiberManager.shutdown();
        }
    }

    public RubyString makeString(String string) {
        return new RubyString(coreLibrary.getStringClass(), string);
    }

    public RubyString makeString(char string) {
        return makeString(Character.toString(string));
    }

    public Ruby getRuntime() {
        return runtime;
    }

    public CoreLibrary getCoreLibrary() {
        return coreLibrary;
    }

    public FeatureManager getFeatureManager() {
        return featureManager;
    }

    public ObjectSpaceManager getObjectSpaceManager() {
        return objectSpaceManager;
    }

    public TraceManager getTraceManager() {
        return traceManager;
    }

    public FiberManager getFiberManager() {
        return fiberManager;
    }

    public ThreadManager getThreadManager() {
        return threadManager;
    }

    public RubyParser getParser() {
        return parser;
    }

    /**
     * Utility method to check if an object should be visible in a Ruby program. Used in assertions
     * at method boundaries to check that only values we want to be visible to the programmer become
     * so.
     */
    public static boolean shouldObjectBeVisible(Object object) {
        // TODO(cs): RubyMethod should never be visible

        return object instanceof UndefinedPlaceholder || //
                        object instanceof Boolean || //
                        object instanceof Integer || //
                        object instanceof BigInteger || //
                        object instanceof Double || //
                        object instanceof RubyBasicObject || //
                        object instanceof RubyMethod || //
                        object instanceof NilPlaceholder || //
                        object instanceof RubyMethod;
    }

    public static boolean shouldObjectsBeVisible(Object... objects) {
        for (Object object : objects) {
            if (!shouldObjectBeVisible(object)) {
                return false;
            }
        }

        return true;
    }

    public AtExitManager getAtExitManager() {
        return atExitManager;
    }

    public SourceManager getSourceManager() {
        return sourceManager;
    }

}
