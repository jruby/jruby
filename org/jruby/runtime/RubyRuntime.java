/*
 * RubyRuntime.java - No description
 * Created on 09. November 2001, 15:47
 *
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package org.jruby.runtime;

import org.ablaf.ast.INode;
import org.ablaf.common.ISourcePosition;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.Asserts;
import org.jruby.exceptions.NameError;
import org.jruby.exceptions.IOError;
import org.jruby.Ruby;
import org.jruby.RubyProc;
import org.jruby.RubyString;
import org.jruby.RubyModule;
import org.jruby.RubyFixnum;
import org.jruby.RubySymbol;
import org.jruby.RubyException;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyIO;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.InputStream;

/**
 * @version $Revision$
 * @author  jpetersen
 */
public class RubyRuntime {
    private static final int TRACE_HEAD = 8;
    private static final int TRACE_TAIL = 5;
    private static final int TRACE_MAX = TRACE_HEAD + TRACE_TAIL + 5;

    private Ruby runtime;

    private RubyProc traceFunction;
    private boolean tracing = false;

    public RubyRuntime(Ruby ruby) {
        this.runtime = ruby;
    }

    /** Call the current method in the superclass of the current object
     *
     */
    public IRubyObject callSuper(IRubyObject[] args) {
        if (runtime.getCurrentFrame().getLastClass() == null) {
            throw new NameError(
                runtime,
                "superclass method '" + runtime.getCurrentFrame().getLastFunc() + "' must be enabled by enableSuper().");
        }

        runtime.getIterStack().push(runtime.getCurrentIter().isNot() ? Iter.ITER_NOT : Iter.ITER_PRE);

        try {
            return runtime.getCurrentFrame().getLastClass().getSuperClass().call(
                runtime.getCurrentFrame().getSelf(),
                runtime.getCurrentFrame().getLastFunc(),
                args,
                CallType.SUPER);
        } finally {
            runtime.getIterStack().pop();
        }
    }

    /** This method compiles and interprets a Ruby script.
     *
     *  It can be used if you want to use JRuby as a Macro language.
     *
     */
    public void loadScript(RubyString scriptName, RubyString source, boolean wrap) {
        IRubyObject self = runtime.getTopSelf();
        Namespace savedNamespace = runtime.getNamespace();

        runtime.pushDynamicVars();

        RubyModule wrapper = runtime.getWrapper();
        runtime.setNamespace(runtime.getTopNamespace());

        if (!wrap) {
            runtime.secure(4); /* should alter global state */
            runtime.pushClass(runtime.getClasses().getObjectClass());
            runtime.setWrapper(null);
        } else {
            /* load in anonymous module as toplevel */
            runtime.setWrapper(RubyModule.newModule(runtime));
            runtime.pushClass(runtime.getWrapper());
            self = runtime.getTopSelf().rbClone();
            self.extendObject(runtime.getRubyClass());
            runtime.setNamespace(new Namespace(runtime.getWrapper(), runtime.getNamespace()));
        }

        String last_func = runtime.getCurrentFrame().getLastFunc();

        runtime.getFrameStack().push();
        runtime.getCurrentFrame().setLastFunc(null);
        runtime.getCurrentFrame().setLastClass(null);
        runtime.getCurrentFrame().setSelf(self);
        runtime.getCurrentFrame().setNamespace(new Namespace(runtime.getRubyClass(), null));
        runtime.getScope().push();

        /* default visibility is private at loading toplevel */
        runtime.setCurrentVisibility(Visibility.PRIVATE);

        try {
            INode node = runtime.parse(source.toString(), scriptName.getValue());
            self.eval(node);

        } finally {
            runtime.getCurrentFrame().setLastFunc(last_func);
            runtime.setNamespace(savedNamespace);
            runtime.getScope().pop();
            runtime.getFrameStack().pop();
            runtime.popClass();
            runtime.popDynamicVars();
            runtime.setWrapper(wrapper);
        }
    }

    /** This method loads, compiles and interprets a Ruby file.
     *  It is used by Kernel#require.
     *
     *  (matz Ruby: rb_load)
     */
    public void loadFile(File iFile, boolean wrap) {
        Asserts.assertNotNull(iFile, "No such file to load");

        try {
            StringBuffer source = new StringBuffer((int) iFile.length());
            BufferedReader br = new BufferedReader(new FileReader(iFile));
            String line;
            while ((line = br.readLine()) != null) {
                source.append(line).append('\n');
            }
            br.close();

            loadScript(new RubyString(runtime, iFile.getPath()), new RubyString(runtime, source.toString()), wrap);

        } catch (IOException ioExcptn) {
            throw IOError.fromException(runtime, ioExcptn);
        }
    }

    public void loadFile(RubyString fname, boolean wrap) {
        loadFile(new File(fname.getValue()), wrap);
    }

    /** Call the traceFunction
     *
     * MRI: eval.c - call_trace_func
     *
     */
    public synchronized void callTraceFunction(
        String event,
        ISourcePosition position,
        IRubyObject self,
        String name,
        IRubyObject type) {
        if (!tracing && traceFunction != null) {
            tracing = true;

            ISourcePosition savePosition = runtime.getPosition();
            String file = position.getFile();

            if (file == null) {
                file = "(ruby)";
            }
            if (type == null)
                type = runtime.getFalse();

            runtime.getFrameStack().push();
            // set current *frame = last *frame
            runtime.getCurrentFrame().setIter(Iter.ITER_NOT);

            try {
                traceFunction
                    .call(new IRubyObject[] {
                        RubyString.newString(runtime, event),
                        RubyString.newString(runtime, file),
                        RubyFixnum.newFixnum(runtime, position.getLine()),
                        name != null ? RubySymbol.newSymbol(runtime, name) : runtime.getNil(),
                        self != null ? self: runtime.getNil(), // rb_f_binding
                        type });
            } finally {
                runtime.getFrameStack().pop();
                runtime.setPosition(savePosition);
                tracing = false;
            }
        }
    }

    /** Prints an error with backtrace to the error stream.
     *
     * MRI: eval.c - error_print()
     *
     */
    public void printError(RubyException excp) {
        if (excp == null || excp.isNil()) {
            return;
        }

        RubyArray backtrace = (RubyArray) excp.callMethod("backtrace");

        if (backtrace.isNil()) {
            if (runtime.getSourceFile() != null) {
                getErrorStream().print(runtime.getSourceFile() + ':' + runtime.getSourceLine());
            } else {
                getErrorStream().print(runtime.getSourceLine());
            }
        } else if (backtrace.getLength() == 0) {
            printErrorPos();
        } else {
            IRubyObject mesg = backtrace.entry(0);

            if (mesg.isNil()) {
                printErrorPos();
            } else {
                getErrorStream().print(mesg);
            }
        }

        RubyClass type = excp.getInternalClass();
        String info = excp.toString();

        if (type == runtime.getExceptions().getRuntimeError() && (info == null || info.length() == 0)) {
            getErrorStream().print(": unhandled exception\n");
        } else {
            String path = type.getClassPath().toString();

            if (info.length() == 0) {
                getErrorStream().print(": " + path + '\n');
            } else {
                if (path.startsWith("#")) {
                    path = null;
                }

                String tail = null;
                if (info.indexOf("\n") != -1) {
                    tail = info.substring(info.indexOf("\n") + 1);
                    info = info.substring(0, info.indexOf("\n"));
                }

                getErrorStream().print(": " + info);

                if (path != null) {
                    getErrorStream().print(" (" + path + ")\n");
                }

                if (tail != null) {
                    getErrorStream().print(tail + '\n');
                }
            }
        }

        if (!backtrace.isNil()) {
            IRubyObject[] elements = backtrace.toJavaArray();

            for (int i = 0; i < elements.length; i++) {
                if (elements[i] instanceof RubyString) {
                    getErrorStream().print("\tfrom " + elements[i] + '\n');
                }

                if (i == TRACE_HEAD && elements.length > TRACE_MAX) {
                    getErrorStream().print("\t ... " + (elements.length - TRACE_HEAD - TRACE_TAIL) + "levels...\n");
                    i = elements.length - TRACE_TAIL;
                }
            }
        }
    }

    private void printErrorPos() {
        if (runtime.getSourceFile() != null) {
            if (runtime.getCurrentFrame().getLastFunc() != null) {
                getErrorStream().print(runtime.getSourceFile() + ':' + runtime.getSourceLine());
                getErrorStream().print(":in '" + runtime.getCurrentFrame().getLastFunc() + '\'');
            } else if (runtime.getSourceLine() != 0) {
                getErrorStream().print(runtime.getSourceFile() + ':' + runtime.getSourceLine());
            } else {
                getErrorStream().print(runtime.getSourceFile());
            }
        }
    }

    /**
     * Gets the errorStream
     * @return Returns a PrintStream
     */
    public PrintStream getErrorStream() {
        return new PrintStream(((RubyIO) runtime.getGlobalVar("$stderr")).getOutStream());
    }

    /**
     * Sets the errorStream
     * @param errorStream The errorStream to set
     */
    public void setErrorStream(PrintStream errorStream) {
        runtime.setGlobalVar("$stderr", RubyIO.stderr(runtime, runtime.getClasses().getIoClass(), errorStream));
    }

    /**
     * Gets the inputStream
     * @return Returns a InputStream
     */
    public InputStream getInputStream() {
        return ((RubyIO) runtime.getGlobalVar("$stdin")).getInStream();
    }

    /**
     * Sets the inputStream
     * @param inputStream The inputStream to set
     */
    public void setInputStream(InputStream inputStream) {
        runtime.setGlobalVar("$stdin", RubyIO.stdin(runtime, runtime.getClasses().getIoClass(), inputStream));
    }

    /**
     * Gets the outputStream
     * @return Returns a PrintStream
     */
    public PrintStream getOutputStream() {
        return new PrintStream(((RubyIO) runtime.getGlobalVar("$stdout")).getOutStream());
    }

    /**
     * Sets the outputStream
     * @param outStream The outputStream to set
     */
    public void setOutputStream(PrintStream outStream) {
        IRubyObject stdout = RubyIO.stdout(runtime, runtime.getClasses().getIoClass(), outStream);
        if (runtime.getGlobalVar("$stdout") == runtime.getGlobalVar("$>")) {
            runtime.setGlobalVar("$>", stdout);
        }
        runtime.setGlobalVar("$stdout", stdout);
    }

    /**
     * Gets the traceFunction.
     * @return Returns a RubyProc
     */
    public RubyProc getTraceFunction() {
        return traceFunction;
    }

    /**
     * Sets the traceFunction.
     * @param traceFunction The traceFunction to set
     */
    public void setTraceFunction(RubyProc traceFunction) {
        this.traceFunction = traceFunction;
    }
}