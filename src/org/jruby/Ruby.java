/*
 **** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001 Chad Fowler <chadfowler@chadfowler.com>
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004-2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
 * Copyright (C) 2006 Michael Studman <codehaus@michaelstudman.com>
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
 * Copyright (C) 2007 Nick Sieger <nicksieger@gmail.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import jnr.constants.Constant;
import jnr.constants.ConstantSet;
import jnr.constants.platform.Errno;
import jnr.posix.POSIX;
import jnr.posix.POSIXFactory;
import org.jcodings.Encoding;
import org.joda.time.DateTimeZone;
import org.jruby.RubyInstanceConfig.CompileMode;
import org.jruby.ast.Node;
import org.jruby.ast.RootNode;
import org.jruby.ast.executable.AbstractScript;
import org.jruby.ast.executable.RuntimeCache;
import org.jruby.ast.executable.Script;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.common.RubyWarnings;
import org.jruby.compiler.ASTCompiler;
import org.jruby.compiler.ASTInspector;
import org.jruby.compiler.JITCompiler;
import org.jruby.compiler.impl.StandardASMCompiler;
import org.jruby.evaluator.ASTInterpreter;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.MainExitException;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.Unrescuable;
import org.jruby.ext.JRubyPOSIXHandler;
import org.jruby.ext.LateLoadingLibrary;
import org.jruby.ext.coverage.CoverageData;
import org.jruby.ext.ffi.FFI;
import org.jruby.ext.jruby.JRubyConfigLibrary;
import org.jruby.internal.runtime.GlobalVariables;
import org.jruby.internal.runtime.ThreadService;
import org.jruby.internal.runtime.ValueAccessor;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.ir.IRBuilder;
import org.jruby.ir.IRManager;
import org.jruby.ir.IRScope;
import org.jruby.ir.interpreter.Interpreter;
import org.jruby.ir.targets.JVMVisitor;
import org.jruby.javasupport.JavaSupport;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.management.BeanManager;
import org.jruby.management.BeanManagerFactory;
import org.jruby.management.ClassCache;
import org.jruby.management.Config;
import org.jruby.management.ParserStats;
import org.jruby.parser.IRStaticScopeFactory;
import org.jruby.parser.Parser;
import org.jruby.parser.ParserConfiguration;
import org.jruby.parser.StaticScope;
import org.jruby.parser.StaticScopeFactory;
import org.jruby.platform.Platform;
import org.jruby.runtime.Binding;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.EventHook;
import org.jruby.runtime.GlobalVariable;
import org.jruby.runtime.IAccessor;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ObjectSpace;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.encoding.EncodingService;
import org.jruby.runtime.load.BasicLibraryService;
import org.jruby.runtime.load.CompiledScriptLoader;
import org.jruby.runtime.load.Library;
import org.jruby.runtime.load.LoadService;
import org.jruby.runtime.opto.Invalidator;
import org.jruby.runtime.opto.OptoFactory;
import org.jruby.runtime.profile.IProfileData;
import org.jruby.runtime.scope.ManyVarsDynamicScope;
import org.jruby.threading.DaemonThreadFactory;
import org.jruby.util.ByteList;
import org.jruby.util.IOInputStream;
import org.jruby.util.IOOutputStream;
import org.jruby.util.JRubyClassLoader;
import org.jruby.util.JavaNameMangler;
import org.jruby.util.KCode;
import org.jruby.util.SafePropertyAccessor;
import org.jruby.util.collections.WeakHashSet;
import org.jruby.util.func.Function1;
import org.jruby.util.io.ChannelDescriptor;
import org.jruby.util.io.SelectorPool;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import org.jruby.util.unsafe.UnsafeFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.net.BindException;
import java.nio.channels.ClosedChannelException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * The Ruby object represents the top-level of a JRuby "instance" in a given VM.
 * JRuby supports spawning multiple instances in the same JVM. Generally, objects
 * created under these instances are tied to a given runtime, for such details
 * as identity and type, because multiple Ruby instances means there are
 * multiple instances of each class. This means that in multi-runtime mode
 * (or really, multi-VM mode, where each JRuby instance is a ruby "VM"), objects
 * generally can't be transported across runtimes without marshaling.
 * 
 * This class roots everything that makes the JRuby runtime function, and
 * provides a number of utility methods for constructing global types and
 * accessing global runtime structures.
 */
public final class Ruby {
    
    /**
     * The logger used to log relevant bits.
     */
    private static final Logger LOG = LoggerFactory.getLogger("Ruby");

    /**
     * Create and initialize a new JRuby runtime. The properties of the
     * specified RubyInstanceConfig will be used to determine various JRuby
     * runtime characteristics.
     * 
     * @param config The configuration to use for the new instance
     * @see org.jruby.RubyInstanceConfig
     */
    private Ruby(RubyInstanceConfig config) {
        this.config             = config;
        this.is1_9              = config.getCompatVersion().is1_9();
        this.is2_0              = config.getCompatVersion().is2_0();
        this.doNotReverseLookupEnabled = is1_9;
        this.threadService      = new ThreadService(this);
        if(config.isSamplingEnabled()) {
            org.jruby.util.SimpleSampler.registerThreadContext(threadService.getCurrentContext());
        }
        
        if (config.getCompileMode() == CompileMode.OFFIR ||
                config.getCompileMode() == CompileMode.FORCEIR) {
            this.staticScopeFactory = new IRStaticScopeFactory(this);
        } else {
            this.staticScopeFactory = new StaticScopeFactory(this);
        }

        this.in                 = config.getInput();
        this.out                = config.getOutput();
        this.err                = config.getError();
        this.objectSpaceEnabled = config.isObjectSpaceEnabled();
        this.profile            = config.getProfile();
        this.currentDirectory   = config.getCurrentDirectory();
        this.kcode              = config.getKCode();
        this.beanManager        = BeanManagerFactory.create(this, config.isManagementEnabled());
        this.jitCompiler        = new JITCompiler(this);
        this.parserStats        = new ParserStats(this);
        
        Random myRandom;
        try {
            myRandom = new SecureRandom();
        } catch (Throwable t) {
            LOG.debug("unable to instantiate SecureRandom, falling back on Random", t);
            myRandom = new Random();
        }
        this.random = myRandom;
        this.hashSeed = this.random.nextInt();
        
        this.beanManager.register(new Config(this));
        this.beanManager.register(parserStats);
        this.beanManager.register(new ClassCache(this));
        this.beanManager.register(new org.jruby.management.Runtime(this));

        this.runtimeCache = new RuntimeCache();
        runtimeCache.initMethodCache(ClassIndex.MAX_CLASSES * MethodIndex.MAX_METHODS);
        
        constantInvalidator = OptoFactory.newConstantInvalidator();
    }
    
    /**
     * Returns a new instance of the JRuby runtime configured with defaults.
     *
     * @return the JRuby runtime
     * @see org.jruby.RubyInstanceConfig
     */
    public static Ruby newInstance() {
        return newInstance(new RubyInstanceConfig());
    }

    /**
     * Returns a new instance of the JRuby runtime configured as specified.
     *
     * @param config The instance configuration
     * @return The JRuby runtime
     * @see org.jruby.RubyInstanceConfig
     */
    public static Ruby newInstance(RubyInstanceConfig config) {
        Ruby ruby = new Ruby(config);
        ruby.init();
        setGlobalRuntimeFirstTimeOnly(ruby);
        return ruby;
    }

    /**
     * Returns a new instance of the JRuby runtime configured with the given
     * input, output and error streams and otherwise default configuration
     * (except where specified system properties alter defaults).
     *
     * @param in the custom input stream
     * @param out the custom output stream
     * @param err the custom error stream
     * @return the JRuby runtime
     * @see org.jruby.RubyInstanceConfig
     */
    public static Ruby newInstance(InputStream in, PrintStream out, PrintStream err) {
        RubyInstanceConfig config = new RubyInstanceConfig();
        config.setInput(in);
        config.setOutput(out);
        config.setError(err);
        return newInstance(config);
    }

    /**
     * Tests whether globalRuntime has been instantiated or not.
     *
     * This method is used by singleton model of org.jruby.embed.ScriptingContainer
     * to decide what RubyInstanceConfig should be used. When a global runtime is
     * not there, RubyInstanceConfig of AbstractContextProvider will be used to enact
     * configurations set by a user. When a global runtime is already instantiated,
     * RubyInstanceConfig of the global runtime should be used in ScriptingContaiener.
     *
     * @return true if a global runtime is instantiated, false for other.
     * 
     */
    public static boolean isGlobalRuntimeReady() {
        return globalRuntime != null;
    }

    /**
     * Set the global runtime to the given runtime only if it has no been set.
     * 
     * @param runtime the runtime to use for global runtime
     */
    private static synchronized void setGlobalRuntimeFirstTimeOnly(Ruby runtime) {
        if (globalRuntime == null) {
            globalRuntime = runtime;
        }
    }

    /**
     * Get the global runtime.
     * 
     * @return the global runtime
     */
    public static synchronized Ruby getGlobalRuntime() {
        if (globalRuntime == null) {
            newInstance();
        }
        return globalRuntime;
    }
    
    /**
     * Convenience method for java integrators who may need to switch the notion 
     * of "global" runtime. Use <tt>JRuby.runtime.use_as_global_runtime</tt>
     * from Ruby code to activate the current runtime as the global one.
     */
    public void useAsGlobalRuntime() {
        synchronized(Ruby.class) {
            globalRuntime = null;
            setGlobalRuntimeFirstTimeOnly(this);
        }
    }

    /**
     * Get the thread-local runtime for the current thread, or null if unset.
     * 
     * @return the thread-local runtime, or null if unset
     */
    public static Ruby getThreadLocalRuntime() {
        return threadLocalRuntime.get();
    }
    
    /**
     * Set the thread-local runtime to the given runtime.
     *
     * Note that static threadlocals like this one can leak resources across
     * (for example) application redeploys. If you use this, it is your
     * responsibility to clean it up appropriately.
     * 
     * @param ruby the new runtime for thread-local
     */
    public static void setThreadLocalRuntime(Ruby ruby) {
        threadLocalRuntime.set(ruby);
    }
    
    /**
     * Evaluates a script under the current scope (perhaps the top-level
     * scope) and returns the result (generally the last value calculated).
     * This version goes straight into the interpreter, bypassing compilation
     * and runtime preparation typical to normal script runs.
     * 
     * @param script The scriptlet to run
     * @returns The result of the eval
     */
    public IRubyObject evalScriptlet(String script) {
        ThreadContext context = getCurrentContext();
        DynamicScope currentScope = context.getCurrentScope();
        ManyVarsDynamicScope newScope = new ManyVarsDynamicScope(getStaticScopeFactory().newEvalScope(currentScope.getStaticScope()), currentScope);

        return evalScriptlet(script, newScope);
    }

    /**
     * Evaluates a script under the current scope (perhaps the top-level
     * scope) and returns the result (generally the last value calculated).
     * This version goes straight into the interpreter, bypassing compilation
     * and runtime preparation typical to normal script runs.
     *
     * This version accepts a scope to use, so you can eval many times against
     * the same scope.
     *
     * @param script The scriptlet to run
     * @param scope The scope to execute against (ManyVarsDynamicScope is
     * recommended, so it can grow as needed)
     * @returns The result of the eval
     */
    public IRubyObject evalScriptlet(String script, DynamicScope scope) {
        ThreadContext context = getCurrentContext();
        Node node = parseEval(script, "<script>", scope, 0);

        try {
            context.preEvalScriptlet(scope);
            return ASTInterpreter.INTERPRET_ROOT(this, context, node, context.getFrameSelf(), Block.NULL_BLOCK);
        } catch (JumpException.ReturnJump rj) {
            throw newLocalJumpError(RubyLocalJumpError.Reason.RETURN, (IRubyObject)rj.getValue(), "unexpected return");
        } catch (JumpException.BreakJump bj) {
            throw newLocalJumpError(RubyLocalJumpError.Reason.BREAK, (IRubyObject)bj.getValue(), "unexpected break");
        } catch (JumpException.RedoJump rj) {
            throw newLocalJumpError(RubyLocalJumpError.Reason.REDO, (IRubyObject)rj.getValue(), "unexpected redo");
        } finally {
            context.postEvalScriptlet();
        }
    }
    
    /**
     * Parse and execute the specified script 
     * This differs from the other methods in that it accepts a string-based script and
     * parses and runs it as though it were loaded at a command-line. This is the preferred
     * way to start up a new script when calling directly into the Ruby object (which is
     * generally *dis*couraged.
     * 
     * @param script The contents of the script to run as a normal, root script
     * @return The last value of the script
     */
    public IRubyObject executeScript(String script, String filename) {
        byte[] bytes = script.getBytes();

        Node node = parseInline(new ByteArrayInputStream(bytes), filename, null);
        ThreadContext context = getCurrentContext();
        
        String oldFile = context.getFile();
        int oldLine = context.getLine();
        try {
            context.setFileAndLine(node.getPosition());
            return runInterpreter(node);
        } finally {
            context.setFileAndLine(oldFile, oldLine);
        }
    }
    
    /**
     * Run the script contained in the specified input stream, using the
     * specified filename as the name of the script being executed. The stream
     * will be read fully before being parsed and executed. The given filename
     * will be used for the ruby $PROGRAM_NAME and $0 global variables in this
     * runtime.
     * 
     * This method is intended to be called once per runtime, generally from
     * Main or from main-like top-level entry points.
     * 
     * As part of executing the script loaded from the input stream, various
     * RubyInstanceConfig properties will be used to determine whether to
     * compile the script before execution or run with various wrappers (for
     * looping, printing, and so on, see jruby -help).
     * 
     * @param inputStream The InputStream from which to read the script contents
     * @param filename The filename to use when parsing, and for $PROGRAM_NAME
     * and $0 ruby global variables.
     */
    public void runFromMain(InputStream inputStream, String filename) {
        IAccessor d = new ValueAccessor(newString(filename));
        getGlobalVariables().define("$PROGRAM_NAME", d);
        getGlobalVariables().define("$0", d);

        for (Iterator i = config.getOptionGlobals().entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            Object value = entry.getValue();
            IRubyObject varvalue;
            if (value != null) {
                varvalue = newString(value.toString());
            } else {
                varvalue = getTrue();
            }
            getGlobalVariables().set("$" + entry.getKey().toString(), varvalue);
        }

        if (filename.endsWith(".class")) {
            // we are presumably running a precompiled class; load directly
            Script script = CompiledScriptLoader.loadScriptFromFile(this, inputStream, filename);
            if (script == null) {
                throw new MainExitException(1, "error: .class file specified is not a compiled JRuby script");
            }
            script.setFilename(filename);
            runScript(script);
            return;
        }
        
        Node scriptNode = parseFromMain(inputStream, filename);

        // done with the stream, shut it down
        try {inputStream.close();} catch (IOException ioe) {}

        ThreadContext context = getCurrentContext();

        String oldFile = context.getFile();
        int oldLine = context.getLine();
        try {
            context.setFileAndLine(scriptNode.getPosition());

            if (config.isAssumePrinting() || config.isAssumeLoop()) {
                runWithGetsLoop(scriptNode, config.isAssumePrinting(), config.isProcessLineEnds(),
                        config.isSplit());
            } else {
                runNormally(scriptNode);
            }
        } finally {
            context.setFileAndLine(oldFile, oldLine);
        }
    }

    /**
     * Parse the script contained in the given input stream, using the given
     * filename as the name of the script, and return the root Node. This
     * is used to verify that the script syntax is valid, for jruby -c. The
     * current scope (generally the top-level scope) is used as the parent
     * scope for parsing.
     * 
     * @param inputStream The input stream from which to read the script
     * @param filename The filename to use for parsing
     * @returns The root node of the parsed script
     */
    public Node parseFromMain(InputStream inputStream, String filename) {
        if (config.isInlineScript()) {
            return parseInline(inputStream, filename, getCurrentContext().getCurrentScope());
        } else {
            return parseFileFromMain(inputStream, filename, getCurrentContext().getCurrentScope());
        }
    }

    /**
     * Run the given script with a "while gets; end" loop wrapped around it.
     * This is primarily used for the -n command-line flag, to allow writing
     * a short script that processes input lines using the specified code.
     *
     * @param scriptNode The root node of the script to execute
     * @param printing Whether $_ should be printed after each loop (as in the
     * -p command-line flag)
     * @param processLineEnds Whether line endings should be processed by
     * setting $\ to $/ and <code>chop!</code>ing every line read
     * @param split Whether to split each line read using <code>String#split</code>
     * bytecode before executing.
     * @return The result of executing the specified script
     */
    @Deprecated
    public IRubyObject runWithGetsLoop(Node scriptNode, boolean printing, boolean processLineEnds, boolean split, boolean unused) {
        return runWithGetsLoop(scriptNode, printing, processLineEnds, split);
    }
    
    /**
     * Run the given script with a "while gets; end" loop wrapped around it.
     * This is primarily used for the -n command-line flag, to allow writing
     * a short script that processes input lines using the specified code.
     * 
     * @param scriptNode The root node of the script to execute
     * @param printing Whether $_ should be printed after each loop (as in the
     * -p command-line flag)
     * @param processLineEnds Whether line endings should be processed by
     * setting $\ to $/ and <code>chop!</code>ing every line read
     * @param split Whether to split each line read using <code>String#split</code>
     * bytecode before executing.
     * @return The result of executing the specified script
     */
    public IRubyObject runWithGetsLoop(Node scriptNode, boolean printing, boolean processLineEnds, boolean split) {
        ThreadContext context = getCurrentContext();
        
        Script script = null;
        boolean compile = getInstanceConfig().getCompileMode().shouldPrecompileCLI();
        if (compile) {
            script = tryCompile(scriptNode);
            if (compile && script == null) {
                // terminate; tryCompile will have printed out an error and we're done
                return getNil();
            }
        }
        
        if (processLineEnds) {
            getGlobalVariables().set("$\\", getGlobalVariables().get("$/"));
        }

        // we do preand post load outside the "body" versions to pre-prepare
        // and pre-push the dynamic scope we need for lastline
        RuntimeHelpers.preLoad(context, ((RootNode)scriptNode).getStaticScope().getVariables());

        try {
            while (RubyKernel.gets(context, getTopSelf(), IRubyObject.NULL_ARRAY).isTrue()) {
                loop: while (true) { // Used for the 'redo' command
                    try {
                        if (processLineEnds) {
                            getGlobalVariables().get("$_").callMethod(context, "chop!");
                        }

                        if (split) {
                            getGlobalVariables().set("$F", getGlobalVariables().get("$_").callMethod(context, "split"));
                        }

                        if (script != null) {
                            runScriptBody(script);
                        } else {
                            runInterpreterBody(scriptNode);
                        }

                        if (printing) RubyKernel.print(context, getKernel(), new IRubyObject[] {getGlobalVariables().get("$_")});
                        break loop;
                    } catch (JumpException.RedoJump rj) {
                        // do nothing, this iteration restarts
                    } catch (JumpException.NextJump nj) {
                        // recheck condition
                        break loop;
                    } catch (JumpException.BreakJump bj) {
                        // end loop
                        return (IRubyObject) bj.getValue();
                    }
                }
            }
        } finally {
            RuntimeHelpers.postLoad(context);
        }
        
        return getNil();
    }

    /**
     * Run the specified script without any of the loop-processing wrapper
     * code.
     *
     * @param scriptNode The root node of the script to be executed
     * bytecode before execution
     * @return The result of executing the script
     */
    @Deprecated
    public IRubyObject runNormally(Node scriptNode, boolean unused) {
        return runNormally(scriptNode);
    }
    
    /**
     * Run the specified script without any of the loop-processing wrapper
     * code.
     * 
     * @param scriptNode The root node of the script to be executed
     * bytecode before execution
     * @return The result of executing the script
     */
    public IRubyObject runNormally(Node scriptNode) {
        Script script = null;
        boolean compile = getInstanceConfig().getCompileMode().shouldPrecompileCLI();
        if (compile || config.isShowBytecode()) {
            script = tryCompile(scriptNode, null, new JRubyClassLoader(getJRubyClassLoader()), config.isShowBytecode());
        }

        if (script != null) {
            if (config.isShowBytecode()) {
                return getNil();
            }

            return runScript(script);
        } else {
            failForcedCompile(scriptNode);
            
            return runInterpreter(scriptNode);
        }
    }

    /**
     * Try to compile the code associated with the given Node, returning an
     * instance of the successfully-compiled Script or null if the script could
     * not be compiled.
     *
     * @param node The node to attempt to compiled
     * @return an instance of the successfully-compiled Script, or null.
     */
    public Script tryCompile(Node node) {
        return tryCompile(node, null, new JRubyClassLoader(getJRubyClassLoader()), false);
    }

    /**
     * Try to compile the code associated with the given Node, returning an
     * instance of the successfully-compiled Script or null if the script could
     * not be compiled. This version accepts an ASTInspector instance assumed to
     * have appropriate flags set for compile optimizations, such as to turn
     * on heap-based local variables to share an existing scope.
     *
     * @param node The node to attempt to compiled
     * @param inspector The ASTInspector to use for making optimization decisions
     * @return an instance of the successfully-compiled Script, or null.
     */
    public Script tryCompile(Node node, ASTInspector inspector) {
        return tryCompile(node, null, new JRubyClassLoader(getJRubyClassLoader()), inspector, false);
    }

    private void failForcedCompile(Node scriptNode) throws RaiseException {
        if (config.getCompileMode().shouldPrecompileAll()) {
            throw newRuntimeError("could not compile and compile mode is 'force': " + scriptNode.getPosition().getFile());
        }
    }

    private void handeCompileError(Node node, Throwable t) {
        if (config.isJitLoggingVerbose() || config.isDebug()) {
            LOG.debug("warning: could not compile: {}; full trace follows", node.getPosition().getFile());
            LOG.debug(t.getMessage(), t);
        }
    }

    private Script tryCompile(Node node, String cachedClassName, JRubyClassLoader classLoader, boolean dump) {
        if (config.getCompileMode() == CompileMode.FORCEIR) {
            final IRScope scope = IRBuilder.createIRBuilder(getIRManager(), is1_9()).buildRoot((RootNode) node);
            final Class compiled = JVMVisitor.compile(this, scope, classLoader);
            final StaticScope staticScope = scope.getStaticScope();
            staticScope.setModule(getTopSelf().getMetaClass());
            return new AbstractScript() {
                public IRubyObject __file__(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
                    try {
                        return (IRubyObject)compiled.getMethod("__script__0", ThreadContext.class, StaticScope.class, IRubyObject.class, Block.class).invoke(null, getCurrentContext(), scope.getStaticScope(), getTopSelf(), block);
                    } catch (InvocationTargetException ite) {
                        if (ite.getCause() instanceof JumpException) {
                            throw (JumpException)ite.getCause();
                        } else {
                            throw new RuntimeException(ite);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                public IRubyObject load(ThreadContext context, IRubyObject self, boolean wrap) {
                    try {
                        RuntimeHelpers.preLoadCommon(context, staticScope, false);
                        return __file__(context, self, IRubyObject.NULL_ARRAY, Block.NULL_BLOCK);
                    } finally {
                        RuntimeHelpers.postLoad(context);
                    }
                }
            };
        }
        ASTInspector inspector = new ASTInspector();
        inspector.inspect(node);

        return tryCompile(node, cachedClassName, classLoader, inspector, dump);
    }

    private Script tryCompile(Node node, String cachedClassName, JRubyClassLoader classLoader, ASTInspector inspector, boolean dump) {
        Script script = null;
        try {
            String filename = node.getPosition().getFile();
            String classname = JavaNameMangler.mangledFilenameForStartupClasspath(filename);

            StandardASMCompiler asmCompiler = null;
            if (RubyInstanceConfig.JIT_CODE_CACHE != null && cachedClassName != null) {
                asmCompiler = new StandardASMCompiler(cachedClassName.replace('.', '/'), filename);
            } else {
                asmCompiler = new StandardASMCompiler(classname, filename);
            }
            ASTCompiler compiler = config.newCompiler();
            if (dump) {
                compiler.compileRoot(node, asmCompiler, inspector, false, false);
                asmCompiler.dumpClass(System.out);
            } else {
                compiler.compileRoot(node, asmCompiler, inspector, true, false);
            }

            if (RubyInstanceConfig.JIT_CODE_CACHE != null && cachedClassName != null) {
                // save script off to disk
                String pathName = cachedClassName.replace('.', '/');
                JITCompiler.saveToCodeCache(this, asmCompiler.getClassByteArray(), "ruby/jit", new File(RubyInstanceConfig.JIT_CODE_CACHE, pathName + ".class"));
            }
            script = (Script)asmCompiler.loadClass(classLoader).newInstance();

            if (config.isJitLogging()) {
                LOG.info("compiled: " + node.getPosition().getFile());
            }
        } catch (Throwable t) {
            handeCompileError(node, t);
        }
        
        return script;
    }
    
    public IRubyObject runScript(Script script) {
        return runScript(script, false);
    }
    
    public IRubyObject runScript(Script script, boolean wrap) {
        ThreadContext context = getCurrentContext();
        
        try {
            return script.load(context, getTopSelf(), wrap);
        } catch (JumpException.ReturnJump rj) {
            return (IRubyObject) rj.getValue();
        }
    }

    /**
     * This is used for the "gets" loop, and we bypass 'load' to use an
     * already-prepared, already-pushed scope for the script body.
     */
    public IRubyObject runScriptBody(Script script) {
        ThreadContext context = getCurrentContext();

        try {
            return script.__file__(context, getTopSelf(), Block.NULL_BLOCK);
        } catch (JumpException.ReturnJump rj) {
            return (IRubyObject) rj.getValue();
        }
    }

    public IRubyObject runInterpreter(ThreadContext context, Node rootNode, IRubyObject self) {
        assert rootNode != null : "scriptNode is not null";

        try {
            if (getInstanceConfig().getCompileMode() == CompileMode.OFFIR) {
                return Interpreter.interpret(this, rootNode, self);
            } else {
                return ASTInterpreter.INTERPRET_ROOT(this, context, rootNode, getTopSelf(), Block.NULL_BLOCK);
            }
        } catch (JumpException.ReturnJump rj) {
            return (IRubyObject) rj.getValue();
        }
    }
    
    public IRubyObject runInterpreter(Node scriptNode) {
        return runInterpreter(getCurrentContext(), scriptNode, getTopSelf());
    }

    /**
     * This is used for the "gets" loop, and we bypass 'load' to use an
     * already-prepared, already-pushed scope for the script body.
     */
    public IRubyObject runInterpreterBody(Node scriptNode) {
        assert scriptNode != null : "scriptNode is not null";
        assert scriptNode instanceof RootNode : "scriptNode is not a RootNode";

        return runInterpreter(((RootNode) scriptNode).getBodyNode());
    }

    public Parser getParser() {
        return parser;
    }
    
    public BeanManager getBeanManager() {
        return beanManager;
    }
    
    public JITCompiler getJITCompiler() {
        return jitCompiler;
    }

    /**
     * @deprecated use #newInstance()
     */
    public static Ruby getDefaultInstance() {
        return newInstance();
    }
    
    @Deprecated
    public static Ruby getCurrentInstance() {
        return null;
    }
    
    @Deprecated
    public static void setCurrentInstance(Ruby runtime) {
    }
    
    public int allocSymbolId() {
        return symbolLastId.incrementAndGet();
    }
    public int allocModuleId() {
        return moduleLastId.incrementAndGet();
    }
    public void addModule(RubyModule module) {
        synchronized (allModules) {
            allModules.add(module);
        }
    }
    public void eachModule(Function1<Object, IRubyObject> func) {
        synchronized (allModules) {
            for (RubyModule module : allModules) {
                func.apply(module);
            }
        }
    }

    /**
     * Retrieve the module with the given name from the Object namespace.
     * 
     * @param name The name of the module
     * @return The module or null if not found
     */
    public RubyModule getModule(String name) {
        return (RubyModule) objectClass.getConstantAt(name);
    }

    @Deprecated
    public RubyModule fastGetModule(String internedName) {
        return getModule(internedName);
    }

    /** 
     * Retrieve the class with the given name from the Object namespace.
     *
     * @param name The name of the class
     * @return The class
     */
    public RubyClass getClass(String name) {
        return objectClass.getClass(name);
    }

    /**
     * Retrieve the class with the given name from the Object namespace. The
     * module name must be an interned string, but this method will be faster
     * than the non-interned version.
     * 
     * @param internedName the name of the class; <em>must</em> be an interned String!
     * @return
     */
    @Deprecated
    public RubyClass fastGetClass(String internedName) {
        return getClass(internedName);
    }

    /** 
     * Define a new class under the Object namespace. Roughly equivalent to
     * rb_define_class in MRI.
     *
     * @param name The name for the new class
     * @param superClass The super class for the new class
     * @param allocator An ObjectAllocator instance that can construct
     * instances of the new class.
     * @return The new class
     */
    public RubyClass defineClass(String name, RubyClass superClass, ObjectAllocator allocator) {
        return defineClassUnder(name, superClass, allocator, objectClass);
    }

    /** 
     * A variation of defineClass that allows passing in an array of subplementary
     * call sites for improving dynamic invocation performance.
     *
     * @param name The name for the new class
     * @param superClass The super class for the new class
     * @param allocator An ObjectAllocator instance that can construct
     * instances of the new class.
     * @return The new class
     */
    public RubyClass defineClass(String name, RubyClass superClass, ObjectAllocator allocator, CallSite[] callSites) {
        return defineClassUnder(name, superClass, allocator, objectClass, callSites);
    }

    /**
     * Define a new class with the given name under the given module or class
     * namespace. Roughly equivalent to rb_define_class_under in MRI.
     * 
     * If the name specified is already bound, its value will be returned if:
     * * It is a class
     * * No new superclass is being defined
     *
     * @param name The name for the new class
     * @param superClass The super class for the new class
     * @param allocator An ObjectAllocator instance that can construct
     * instances of the new class.
     * @param parent The namespace under which to define the new class
     * @return The new class
     */
    public RubyClass defineClassUnder(String name, RubyClass superClass, ObjectAllocator allocator, RubyModule parent) {
        return defineClassUnder(name, superClass, allocator, parent, null);
    }

    /**
     * A variation of defineClassUnder that allows passing in an array of
     * supplementary call sites to improve dynamic invocation.
     *
     * @param name The name for the new class
     * @param superClass The super class for the new class
     * @param allocator An ObjectAllocator instance that can construct
     * instances of the new class.
     * @param parent The namespace under which to define the new class
     * @param callSites The array of call sites to add
     * @return The new class
     */
    public RubyClass defineClassUnder(String name, RubyClass superClass, ObjectAllocator allocator, RubyModule parent, CallSite[] callSites) {
        IRubyObject classObj = parent.getConstantAt(name);

        if (classObj != null) {
            if (!(classObj instanceof RubyClass)) throw newTypeError(name + " is not a class");
            RubyClass klazz = (RubyClass)classObj;
            if (klazz.getSuperClass().getRealClass() != superClass) {
                throw newNameError(name + " is already defined", name);
            }
            // If we define a class in Ruby, but later want to allow it to be defined in Java,
            // the allocator needs to be updated
            if (klazz.getAllocator() != allocator) {
                klazz.setAllocator(allocator);
            }
            return klazz;
        }
        
        boolean parentIsObject = parent == objectClass;

        if (superClass == null) {
            String className = parentIsObject ? name : parent.getName() + "::" + name;  
            warnings.warn(ID.NO_SUPER_CLASS, "no super class for `" + className + "', Object assumed");
            
            superClass = objectClass;
        }

        return RubyClass.newClass(this, superClass, name, allocator, parent, !parentIsObject, callSites);
    }

    /** 
     * Define a new module under the Object namespace. Roughly equivalent to
     * rb_define_module in MRI.
     * 
     * @param name The name of the new module
     * @returns The new module
     */
    public RubyModule defineModule(String name) {
        return defineModuleUnder(name, objectClass);
    }

    /**
     * Define a new module with the given name under the given module or
     * class namespace. Roughly equivalent to rb_define_module_under in MRI.
     * 
     * @param name The name of the new module
     * @param parent The class or module namespace under which to define the
     * module
     * @returns The new module
     */
    public RubyModule defineModuleUnder(String name, RubyModule parent) {
        IRubyObject moduleObj = parent.getConstantAt(name);
        
        boolean parentIsObject = parent == objectClass;

        if (moduleObj != null ) {
            if (moduleObj.isModule()) return (RubyModule)moduleObj;
            
            if (parentIsObject) {
                throw newTypeError(moduleObj.getMetaClass().getName() + " is not a module");
            } else {
                throw newTypeError(parent.getName() + "::" + moduleObj.getMetaClass().getName() + " is not a module");
            }
        }

        return RubyModule.newModule(this, name, parent, !parentIsObject);
    }

    /**
     * From Object, retrieve the named module. If it doesn't exist a
     * new module is created.
     * 
     * @param name The name of the module
     * @returns The existing or new module
     */
    public RubyModule getOrCreateModule(String name) {
        IRubyObject module = objectClass.getConstantAt(name);
        if (module == null) {
            module = defineModule(name);
        } else if (getSafeLevel() >= 4) {
            throw newSecurityError("Extending module prohibited.");
        } else if (!module.isModule()) {
            throw newTypeError(name + " is not a Module");
        }

        return (RubyModule) module;
    }


    /** 
     * Retrieve the current safe level.
     * 
     * @see org.jruby.Ruby#setSafeLevel
     */
    public int getSafeLevel() {
        return this.safeLevel;
    }


    /** 
     * Set the current safe level:
     * 
     * 0 - strings from streams/environment/ARGV are tainted (default)
     * 1 - no dangerous operation by tainted value
     * 2 - process/file operations prohibited
     * 3 - all generated objects are tainted
     * 4 - no global (non-tainted) variable modification/no direct output
     * 
     * The safe level is set using $SAFE in Ruby code. It is not supported
     * in JRuby.
    */
    public void setSafeLevel(int safeLevel) {
        this.safeLevel = safeLevel;
    }

    public KCode getKCode() {
        return kcode;
    }

    public void setKCode(KCode kcode) {
        this.kcode = kcode;
    }

    public void secure(int level) {
        if (level <= safeLevel) {
            throw newSecurityError("Insecure operation '" + getCurrentContext().getFrameName() + "' at level " + safeLevel);
        }
    }

    // FIXME moved this here to get what's obviously a utility method out of IRubyObject.
    // perhaps security methods should find their own centralized home at some point.
    public void checkSafeString(IRubyObject object) {
        if (getSafeLevel() > 0 && object.isTaint()) {
            ThreadContext tc = getCurrentContext();
            if (tc.getFrameName() != null) {
                throw newSecurityError("Insecure operation - " + tc.getFrameName());
            }
            throw newSecurityError("Insecure operation: -r");
        }
        secure(4);
        if (!(object instanceof RubyString)) {
            throw newTypeError(
                "wrong argument type " + object.getMetaClass().getName() + " (expected String)");
        }
    }

    /** rb_define_global_const
     *
     */
    public void defineGlobalConstant(String name, IRubyObject value) {
        objectClass.defineConstant(name, value);
    }

    public boolean isClassDefined(String name) {
        return getModule(name) != null;
    }

    /** 
     * This method is called immediately after constructing the Ruby instance.
     * The main thread is prepared for execution, all core classes and libraries
     * are initialized, and any libraries required on the command line are
     * loaded.
     */
    private void init() {
        safeLevel = config.getSafeLevel();
        
        // Construct key services
        loadService = config.createLoadService(this);
        posix = POSIXFactory.getPOSIX(new JRubyPOSIXHandler(this), config.isNativeEnabled());
        javaSupport = new JavaSupport(this);

        // HACK HACK HACK (jansi (part of jline) + classloader + windows == stacktrace barf)
        if (Platform.IS_WINDOWS) System.setProperty("jline.terminal", "none");

        executor = new ThreadPoolExecutor(
                RubyInstanceConfig.POOL_MIN,
                RubyInstanceConfig.POOL_MAX,
                RubyInstanceConfig.POOL_TTL,
                TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(),
                new DaemonThreadFactory("JRubyWorker"));
        
        // initialize the root of the class hierarchy completely
        initRoot();

        // Set up the main thread in thread service
        threadService.initMainThread();

        // Get the main threadcontext (gets constructed for us)
        ThreadContext tc = getCurrentContext();

        // Construct the top-level execution frame and scope for the main thread
        tc.prepareTopLevel(objectClass, topSelf);

        // Initialize all the core classes
        bootstrap();
        
        irManager = new IRManager();
        
        // Initialize the "dummy" class used as a marker
        dummyClass = new RubyClass(this, classClass);
        dummyClass.freeze(tc);
        
        // Create global constants and variables
        RubyGlobal.createGlobals(tc, this);

        // Prepare LoadService and load path
        getLoadService().init(config.getLoadPaths());
        
        booting = false;

        // initialize builtin libraries
        initBuiltins();
        
        // init Ruby-based kernel
        initRubyKernel();
        
        if(config.isProfiling()) {
            getLoadService().require("jruby/profiler/shutdown_hook");
        }

        if (config.getLoadGemfile()) {
            loadService.loadFromClassLoader(getClassLoader(), "jruby/bundler/startup.rb", false);
        }

        // Require in all libraries specified on command line
        for (String scriptName : config.getRequiredLibraries()) {
            if (is1_9) {
                topSelf.callMethod(getCurrentContext(), "require", RubyString.newString(this, scriptName));
            } else {
                loadService.require(scriptName);
            }
        }
    }

    private void bootstrap() {
        initCore();
        initExceptions();
    }

    private void initRoot() {
        boolean oneNine = is1_9();
        // Bootstrap the top of the hierarchy
        if (oneNine) {
            basicObjectClass = RubyClass.createBootstrapClass(this, "BasicObject", null, RubyBasicObject.BASICOBJECT_ALLOCATOR);
            objectClass = RubyClass.createBootstrapClass(this, "Object", basicObjectClass, RubyObject.OBJECT_ALLOCATOR);
        } else {
            objectClass = RubyClass.createBootstrapClass(this, "Object", null, RubyObject.OBJECT_ALLOCATOR);
        }
        moduleClass = RubyClass.createBootstrapClass(this, "Module", objectClass, RubyModule.MODULE_ALLOCATOR);
        classClass = RubyClass.createBootstrapClass(this, "Class", moduleClass, RubyClass.CLASS_ALLOCATOR);

        if (oneNine) basicObjectClass.setMetaClass(classClass);
        objectClass.setMetaClass(classClass);
        moduleClass.setMetaClass(classClass);
        classClass.setMetaClass(classClass);

        RubyClass metaClass;
        if (oneNine) metaClass = basicObjectClass.makeMetaClass(classClass);
        metaClass = objectClass.makeMetaClass(classClass);
        metaClass = moduleClass.makeMetaClass(metaClass);
        metaClass = classClass.makeMetaClass(metaClass);

        if (oneNine) RubyBasicObject.createBasicObjectClass(this, basicObjectClass);
        RubyObject.createObjectClass(this, objectClass);
        RubyModule.createModuleClass(this, moduleClass);
        RubyClass.createClassClass(this, classClass);
        
        // set constants now that they're initialized
        if (oneNine) objectClass.setConstant("BasicObject", basicObjectClass);
        objectClass.setConstant("Object", objectClass);
        objectClass.setConstant("Class", classClass);
        objectClass.setConstant("Module", moduleClass);

        // Initialize Kernel and include into Object
        RubyKernel.createKernelModule(this);
        objectClass.includeModule(kernelModule);

        // Object is ready, create top self
        topSelf = TopSelfFactory.createTopSelf(this);
        
        // Pre-create all the core classes potentially referenced during startup
        RubyNil.createNilClass(this);
        RubyBoolean.createFalseClass(this);
        RubyBoolean.createTrueClass(this);

        nilObject = new RubyNil(this);
        for (int i=0; i<NIL_PREFILLED_ARRAY_SIZE; i++) nilPrefilledArray[i] = nilObject;
        singleNilArray = new IRubyObject[] {nilObject};

        falseObject = new RubyBoolean(this, false);
        trueObject = new RubyBoolean(this, true);
    }

    private void initCore() {
        if (profile.allowClass("Data")) {
            defineClass("Data", objectClass, ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        }

        RubyComparable.createComparable(this);
        RubyEnumerable.createEnumerableModule(this);
        RubyString.createStringClass(this);

        encodingService = new EncodingService(this);

        RubySymbol.createSymbolClass(this);
        
        recursiveKey = newSymbol("__recursive_key__");

        if (profile.allowClass("ThreadGroup")) {
            RubyThreadGroup.createThreadGroupClass(this);
        }
        if (profile.allowClass("Thread")) {
            RubyThread.createThreadClass(this);
        }
        if (profile.allowClass("Exception")) {
            RubyException.createExceptionClass(this);
        }

        if (!is1_9()) {
            if (profile.allowModule("Precision")) {
                RubyPrecision.createPrecisionModule(this);
            }
        }

        if (profile.allowClass("Numeric")) {
            RubyNumeric.createNumericClass(this);
        }
        if (profile.allowClass("Integer")) {
            RubyInteger.createIntegerClass(this);
        }
        if (profile.allowClass("Fixnum")) {
            RubyFixnum.createFixnumClass(this);
        }

        if (is1_9()) {
            if (profile.allowClass("Complex")) {
                RubyComplex.createComplexClass(this);
            }
            if (profile.allowClass("Rational")) {
                RubyRational.createRationalClass(this);
            }
        }

        if (profile.allowClass("Hash")) {
            RubyHash.createHashClass(this);
        }
        if (profile.allowClass("Array")) {
            RubyArray.createArrayClass(this);
        }
        if (profile.allowClass("Float")) {
            RubyFloat.createFloatClass(this);
        }
        if (profile.allowClass("Bignum")) {
            RubyBignum.createBignumClass(this);
            // RubyRandom depends on Bignum existence.
            if (is1_9()) {
                RubyRandom.createRandomClass(this);
            } else {
                setDefaultRand(new RubyRandom.RandomType(this));
            }
        }
        ioClass = RubyIO.createIOClass(this);

        if (profile.allowClass("Struct")) {
            RubyStruct.createStructClass(this);
        }
        if (profile.allowClass("Tms")) {
            tmsStruct = RubyStruct.newInstance(structClass, new IRubyObject[]{newString("Tms"), newSymbol("utime"), newSymbol("stime"), newSymbol("cutime"), newSymbol("cstime")}, Block.NULL_BLOCK);
        }

        if (profile.allowClass("Binding")) {
            RubyBinding.createBindingClass(this);
        }
        // Math depends on all numeric types
        if (profile.allowModule("Math")) {
            RubyMath.createMathModule(this);
        }
        if (profile.allowClass("Regexp")) {
            RubyRegexp.createRegexpClass(this);
        }
        if (profile.allowClass("Range")) {
            RubyRange.createRangeClass(this);
        }
        if (profile.allowModule("ObjectSpace")) {
            RubyObjectSpace.createObjectSpaceModule(this);
        }
        if (profile.allowModule("GC")) {
            RubyGC.createGCModule(this);
        }
        if (profile.allowClass("Proc")) {
            RubyProc.createProcClass(this);
        }
        if (profile.allowClass("Method")) {
            RubyMethod.createMethodClass(this);
        }
        if (profile.allowClass("MatchData")) {
            RubyMatchData.createMatchDataClass(this);
        }
        if (profile.allowModule("Marshal")) {
            RubyMarshal.createMarshalModule(this);
        }
        if (profile.allowClass("Dir")) {
            RubyDir.createDirClass(this);
        }
        if (profile.allowModule("FileTest")) {
            RubyFileTest.createFileTestModule(this);
        }
        // depends on IO, FileTest
        if (profile.allowClass("File")) {
            RubyFile.createFileClass(this);
        }
        if (profile.allowClass("File::Stat")) {
            RubyFileStat.createFileStatClass(this);
        }
        if (profile.allowModule("Process")) {
            RubyProcess.createProcessModule(this);
        }
        if (profile.allowClass("Time")) {
            RubyTime.createTimeClass(this);
        }
        if (profile.allowClass("UnboundMethod")) {
            RubyUnboundMethod.defineUnboundMethodClass(this);
        }
        if (profile.allowModule("Signal")) {
            RubySignal.createSignal(this);
        }
        if (profile.allowClass("Continuation")) {
            RubyContinuation.createContinuation(this);
        }
        
        if (profile.allowClass("Enumerator")) {
            RubyEnumerator.defineEnumerator(this);
        }
        
        if (is1_9()) {
            if (RubyInstanceConfig.COROUTINE_FIBERS) {
                LoadService.reflectedLoad(this, "fiber", "org.jruby.ext.fiber.CoroutineFiberLibrary", getClassLoader(), false);
            } else {
                LoadService.reflectedLoad(this, "fiber", "org.jruby.ext.fiber.ThreadFiberLibrary", getClassLoader(), false);
            }
        } else {
            if (RubyInstanceConfig.COROUTINE_FIBERS) {
                addLazyBuiltin("jruby/fiber.jar", "jruby/fiber", "org.jruby.ext.fiber.CoroutineFiberLibrary");
            } else {
                addLazyBuiltin("jruby/fiber.jar", "jruby/fiber", "org.jruby.ext.fiber.ThreadFiberLibrary");
            }
        }
        
        // Load the JRuby::Config module for accessing configuration settings from Ruby
        new JRubyConfigLibrary().load(this, false);
    }

    public static final int NIL_PREFILLED_ARRAY_SIZE = RubyArray.ARRAY_DEFAULT_SIZE * 8;
    private final IRubyObject nilPrefilledArray[] = new IRubyObject[NIL_PREFILLED_ARRAY_SIZE];
    public IRubyObject[] getNilPrefilledArray() {
        return nilPrefilledArray;
    }

    private void initExceptions() {
        standardError = defineClassIfAllowed("StandardError", exceptionClass);
        runtimeError = defineClassIfAllowed("RuntimeError", standardError);
        ioError = defineClassIfAllowed("IOError", standardError);
        scriptError = defineClassIfAllowed("ScriptError", exceptionClass);
        rangeError = defineClassIfAllowed("RangeError", standardError);
        signalException = defineClassIfAllowed("SignalException", exceptionClass);
        
        if (profile.allowClass("NameError")) {
            nameError = RubyNameError.createNameErrorClass(this, standardError);
            nameErrorMessage = RubyNameError.createNameErrorMessageClass(this, nameError);            
        }
        if (profile.allowClass("NoMethodError")) {
            noMethodError = RubyNoMethodError.createNoMethodErrorClass(this, nameError);
        }
        if (profile.allowClass("SystemExit")) {
            systemExit = RubySystemExit.createSystemExitClass(this, exceptionClass);
        }
        if (profile.allowClass("LocalJumpError")) {
            localJumpError = RubyLocalJumpError.createLocalJumpErrorClass(this, standardError);
        }
        if (profile.allowClass("NativeException")) {
            nativeException = NativeException.createClass(this, runtimeError);
        }
        if (profile.allowClass("SystemCallError")) {
            systemCallError = RubySystemCallError.createSystemCallErrorClass(this, standardError);
        }

        fatal = defineClassIfAllowed("Fatal", exceptionClass);
        interrupt = defineClassIfAllowed("Interrupt", signalException);
        typeError = defineClassIfAllowed("TypeError", standardError);
        argumentError = defineClassIfAllowed("ArgumentError", standardError);
        indexError = defineClassIfAllowed("IndexError", standardError);
        stopIteration = defineClassIfAllowed("StopIteration", indexError);
        syntaxError = defineClassIfAllowed("SyntaxError", scriptError);
        loadError = defineClassIfAllowed("LoadError", scriptError);
        notImplementedError = defineClassIfAllowed("NotImplementedError", scriptError);
        securityError = defineClassIfAllowed("SecurityError", standardError);
        noMemoryError = defineClassIfAllowed("NoMemoryError", exceptionClass);
        regexpError = defineClassIfAllowed("RegexpError", standardError);
        eofError = defineClassIfAllowed("EOFError", ioError);
        threadError = defineClassIfAllowed("ThreadError", standardError);
        concurrencyError = defineClassIfAllowed("ConcurrencyError", threadError);
        systemStackError = defineClassIfAllowed("SystemStackError", is1_9 ? exceptionClass : standardError);
        zeroDivisionError = defineClassIfAllowed("ZeroDivisionError", standardError);
        floatDomainError  = defineClassIfAllowed("FloatDomainError", rangeError);

        if (is1_9()) {
            if (profile.allowClass("EncodingError")) {
                encodingError = defineClass("EncodingError", standardError, standardError.getAllocator());
                encodingCompatibilityError = defineClassUnder("CompatibilityError", encodingError, encodingError.getAllocator(), encodingClass);
                invalidByteSequenceError = defineClassUnder("InvalidByteSequenceError", encodingError, encodingError.getAllocator(), encodingClass);
                undefinedConversionError = defineClassUnder("UndefinedConversionError", encodingError, encodingError.getAllocator(), encodingClass);
                converterNotFoundError = defineClassUnder("ConverterNotFoundError", encodingError, encodingError.getAllocator(), encodingClass);
                fiberError = defineClass("FiberError", standardError, standardError.getAllocator());
            }
            concurrencyError = defineClassIfAllowed("ConcurrencyError", threadError);
            keyError = defineClassIfAllowed("KeyError", indexError);

            mathDomainError = defineClassUnder("DomainError", argumentError, argumentError.getAllocator(), mathModule);
            inRecursiveListOperation.set(false);
        }

        initErrno();
    }
    
    private RubyClass defineClassIfAllowed(String name, RubyClass superClass) {
	// TODO: should probably apply the null object pattern for a
	// non-allowed class, rather than null
        if (superClass != null && profile.allowClass(name)) {
            return defineClass(name, superClass, superClass.getAllocator());
        }
        return null;
    }

    private Map<Integer, RubyClass> errnos = new HashMap<Integer, RubyClass>();

    public RubyClass getErrno(int n) {
        return errnos.get(n);
    }

    /**
     * Create module Errno's Variables.  We have this method since Errno does not have it's
     * own java class.
     */
    private void initErrno() {
        if (profile.allowModule("Errno")) {
            errnoModule = defineModule("Errno");
            try {
                // define EAGAIN now, so that future EWOULDBLOCK will alias to it
                // see MRI's error.c and its explicit ordering of Errno definitions.
                createSysErr(Errno.EAGAIN.intValue(), Errno.EAGAIN.name());
                
                for (Errno e : Errno.values()) {
                    Constant c = (Constant) e;
                    if (Character.isUpperCase(c.name().charAt(0))) {
                        createSysErr(c.intValue(), c.name());
                    }
                }
            } catch (Exception e) {
                // dump the trace and continue
                // this is currently only here for Android, which seems to have
                // bugs in its enumeration logic
                // http://code.google.com/p/android/issues/detail?id=2812
                LOG.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Creates a system error.
     * @param i the error code (will probably use a java exception instead)
     * @param name of the error to define.
     **/
    private void createSysErr(int i, String name) {
        if(profile.allowClass(name)) {
            if (errnos.get(i) == null) {
                RubyClass errno = getErrno().defineClassUnder(name, systemCallError, systemCallError.getAllocator());
                errnos.put(i, errno);
                errno.defineConstant("Errno", newFixnum(i));
            } else {
                // already defined a class for this errno, reuse it (JRUBY-4747)
                getErrno().setConstant(name, errnos.get(i));
            }
        }
    }

    private void initBuiltins() {
        addLazyBuiltin("java.rb", "java", "org.jruby.javasupport.Java");
        addLazyBuiltin("jruby.rb", "jruby", "org.jruby.ext.jruby.JRubyLibrary");
        addLazyBuiltin("jruby/util.rb", "jruby/util", "org.jruby.ext.jruby.JRubyUtilLibrary");
        addLazyBuiltin("jruby/type.rb", "jruby/type", "org.jruby.ext.jruby.JRubyTypeLibrary");
        addLazyBuiltin("iconv.jar", "iconv", "org.jruby.ext.iconv.IConvLibrary");
        addLazyBuiltin("nkf.jar", "nkf", "org.jruby.ext.nkf.NKFLibrary");
        addLazyBuiltin("stringio.jar", "stringio", "org.jruby.ext.stringio.StringIOLibrary");
        addLazyBuiltin("strscan.jar", "strscan", "org.jruby.ext.strscan.StringScannerLibrary");
        addLazyBuiltin("zlib.jar", "zlib", "org.jruby.ext.zlib.ZlibLibrary");
        addLazyBuiltin("enumerator.jar", "enumerator", "org.jruby.ext.enumerator.EnumeratorLibrary");
        addLazyBuiltin("readline.jar", "readline", "org.jruby.ext.ReadlineService");
        addLazyBuiltin("thread.jar", "thread", "org.jruby.ext.thread.ThreadLibrary");
        addLazyBuiltin("thread.rb", "thread", "org.jruby.ext.thread.ThreadLibrary");
        addLazyBuiltin("digest.jar", "digest.so", "org.jruby.ext.digest.DigestLibrary");
        addLazyBuiltin("digest/md5.jar", "digest/md5", "org.jruby.ext.digest.MD5");
        addLazyBuiltin("digest/rmd160.jar", "digest/rmd160", "org.jruby.ext.digest.RMD160");
        addLazyBuiltin("digest/sha1.jar", "digest/sha1", "org.jruby.ext.digest.SHA1");
        addLazyBuiltin("digest/sha2.jar", "digest/sha2", "org.jruby.ext.digest.SHA2");
        addLazyBuiltin("bigdecimal.jar", "bigdecimal", "org.jruby.ext.bigdecimal.BigDecimalLibrary");
        addLazyBuiltin("io/wait.jar", "io/wait", "org.jruby.ext.io.wait.IOWaitLibrary");
        addLazyBuiltin("etc.jar", "etc", "org.jruby.ext.etc.EtcLibrary");
        addLazyBuiltin("weakref.rb", "weakref", "org.jruby.ext.weakref.WeakRefLibrary");
        addLazyBuiltin("delegate_internal.jar", "delegate_internal", "org.jruby.ext.delegate.DelegateLibrary");
        addLazyBuiltin("timeout.rb", "timeout", "org.jruby.ext.timeout.Timeout");
        addLazyBuiltin("socket.jar", "socket", "org.jruby.ext.socket.SocketLibrary");
        addLazyBuiltin("rbconfig.rb", "rbconfig", "org.jruby.ext.rbconfig.RbConfigLibrary");
        addLazyBuiltin("jruby/serialization.rb", "serialization", "org.jruby.ext.jruby.JRubySerializationLibrary");
        addLazyBuiltin("ffi-internal.jar", "ffi-internal", "org.jruby.ext.ffi.FFIService");
        addLazyBuiltin("tempfile.rb", "tempfile", "org.jruby.ext.tempfile.TempfileLibrary");
        addLazyBuiltin("fcntl.rb", "fcntl", "org.jruby.ext.fcntl.FcntlLibrary");
        addLazyBuiltin("rubinius.jar", "rubinius", "org.jruby.ext.rubinius.RubiniusLibrary");
        addLazyBuiltin("yecht.jar", "yecht", "YechtService");
        addLazyBuiltin("jopenssl.jar", "jopenssl", "org.jruby.ext.openssl.OSSLLibrary");

        if (is1_9()) {
            addLazyBuiltin("mathn/complex.jar", "mathn/complex", "org.jruby.ext.mathn.Complex");
            addLazyBuiltin("mathn/rational.jar", "mathn/rational", "org.jruby.ext.mathn.Rational");
            addLazyBuiltin("fiber.rb", "fiber", "org.jruby.ext.fiber.FiberExtLibrary");
            addLazyBuiltin("psych.jar", "psych", "org.jruby.ext.psych.PsychLibrary");
            addLazyBuiltin("coverage.jar", "coverage", "org.jruby.ext.coverage.CoverageLibrary");

            // TODO: implement something for these?
            Library dummy = new Library() {
                public void load(Ruby runtime, boolean wrap) throws IOException {
                    // dummy library that does nothing right now
                }
            };
            addBuiltinIfAllowed("continuation.rb", dummy);
            addBuiltinIfAllowed("io/nonblock.rb", dummy);
        } else {
            addLazyBuiltin("jruby/fiber_ext.rb", "jruby/fiber_ext", "org.jruby.ext.fiber.FiberExtLibrary");
        }

        if(RubyInstanceConfig.NATIVE_NET_PROTOCOL) {
            addLazyBuiltin("net/protocol.rb", "net/protocol", "org.jruby.ext.net.protocol.NetProtocolBufferedIOLibrary");
        }

        addBuiltinIfAllowed("win32ole.jar", new Library() {
            public void load(Ruby runtime, boolean wrap) throws IOException {
                runtime.getLoadService().require("jruby/win32ole/stub");
            }
        });
        
        RubyKernel.autoload(topSelf, newSymbol("Java"), newString("java"));
    }
    
    private void initRubyKernel() {
        // load Ruby parts of core
        loadService.loadFromClassLoader(getClassLoader(), "jruby/kernel.rb", false);
        
        switch (config.getCompatVersion()) {
            case RUBY1_8:
                loadService.loadFromClassLoader(getClassLoader(), "jruby/kernel18.rb", false);
                break;
            case RUBY1_9:
                loadService.loadFromClassLoader(getClassLoader(), "jruby/kernel19.rb", false);
                break;
            case RUBY2_0:
                loadService.loadFromClassLoader(getClassLoader(), "jruby/kernel20.rb", false);
                break;
        }
    }

    private void addLazyBuiltin(String name, String shortName, String className) {
        addBuiltinIfAllowed(name, new LateLoadingLibrary(shortName, className, getClassLoader()));
    }

    private void addBuiltinIfAllowed(String name, Library lib) {
        if(profile.allowBuiltin(name)) {
            loadService.addBuiltinLibrary(name,lib);
        }
    }

    public Object getRespondToMethod() {
        return respondToMethod;
    }

    public void setRespondToMethod(Object rtm) {
        this.respondToMethod = rtm;
    }
    
    public IRManager getIRManager() {
        return irManager;
    }

    /** Getter for property rubyTopSelf.
     * @return Value of property rubyTopSelf.
     */
    public IRubyObject getTopSelf() {
        return topSelf;
    }

    public IRubyObject getRootFiber() {
        return rootFiber;
    }

    public void setRootFiber(IRubyObject fiber) {
        rootFiber = fiber;
    }

    public void setCurrentDirectory(String dir) {
        currentDirectory = dir;
    }

    public String getCurrentDirectory() {
        return currentDirectory;
    }

    public void setCurrentLine(int line) {
        currentLine = line;
    }

    public int getCurrentLine() {
        return currentLine;
    }

    public void setArgsFile(IRubyObject argsFile) {
        this.argsFile = argsFile;
    }

    public IRubyObject getArgsFile() {
        return argsFile;
    }
    
    public RubyModule getEtc() {
        return etcModule;
    }
    
    public void setEtc(RubyModule etcModule) {
        this.etcModule = etcModule;
    }

    public RubyClass getObject() {
        return objectClass;
    }

    public RubyClass getBasicObject() {
        return basicObjectClass;
    }

    public RubyClass getModule() {
        return moduleClass;
    }

    public RubyClass getClassClass() {
        return classClass;
    }
    
    public RubyModule getKernel() {
        return kernelModule;
    }
    void setKernel(RubyModule kernelModule) {
        this.kernelModule = kernelModule;
    }

    public DynamicMethod getPrivateMethodMissing() {
        return privateMethodMissing;
    }
    public void setPrivateMethodMissing(DynamicMethod method) {
        privateMethodMissing = method;
    }
    public DynamicMethod getProtectedMethodMissing() {
        return protectedMethodMissing;
    }
    public void setProtectedMethodMissing(DynamicMethod method) {
        protectedMethodMissing = method;
    }
    public DynamicMethod getVariableMethodMissing() {
        return variableMethodMissing;
    }
    public void setVariableMethodMissing(DynamicMethod method) {
        variableMethodMissing = method;
    }
    public DynamicMethod getSuperMethodMissing() {
        return superMethodMissing;
    }
    public void setSuperMethodMissing(DynamicMethod method) {
        superMethodMissing = method;
    }
    public DynamicMethod getNormalMethodMissing() {
        return normalMethodMissing;
    }
    public void setNormalMethodMissing(DynamicMethod method) {
        normalMethodMissing = method;
    }
    public DynamicMethod getDefaultMethodMissing() {
        return defaultMethodMissing;
    }
    public void setDefaultMethodMissing(DynamicMethod method) {
        defaultMethodMissing = method;
    }
    
    public RubyClass getDummy() {
        return dummyClass;
    }

    public RubyModule getComparable() {
        return comparableModule;
    }
    void setComparable(RubyModule comparableModule) {
        this.comparableModule = comparableModule;
    }    

    public RubyClass getNumeric() {
        return numericClass;
    }
    void setNumeric(RubyClass numericClass) {
        this.numericClass = numericClass;
    }    

    public RubyClass getFloat() {
        return floatClass;
    }
    void setFloat(RubyClass floatClass) {
        this.floatClass = floatClass;
    }
    
    public RubyClass getInteger() {
        return integerClass;
    }
    void setInteger(RubyClass integerClass) {
        this.integerClass = integerClass;
    }    
    
    public RubyClass getFixnum() {
        return fixnumClass;
    }
    void setFixnum(RubyClass fixnumClass) {
        this.fixnumClass = fixnumClass;
    }

    public RubyClass getComplex() {
        return complexClass;
    }
    void setComplex(RubyClass complexClass) {
        this.complexClass = complexClass;
    }

    public RubyClass getRational() {
        return rationalClass;
    }
    void setRational(RubyClass rationalClass) {
        this.rationalClass = rationalClass;
    }

    public RubyModule getEnumerable() {
        return enumerableModule;
    }
    void setEnumerable(RubyModule enumerableModule) {
        this.enumerableModule = enumerableModule;
    }

    public RubyClass getEnumerator() {
        return enumeratorClass;
    }
    void setEnumerator(RubyClass enumeratorClass) {
        this.enumeratorClass = enumeratorClass;
    }

    public RubyClass getYielder() {
        return yielderClass;
    }
    void setYielder(RubyClass yielderClass) {
        this.yielderClass = yielderClass;
    }

    public RubyClass getString() {
        return stringClass;
    }
    void setString(RubyClass stringClass) {
        this.stringClass = stringClass;
    }

    public RubyClass getEncoding() {
        return encodingClass;
    }
    void setEncoding(RubyClass encodingClass) {
        this.encodingClass = encodingClass;
    }

    public RubyClass getConverter() {
        return converterClass;
    }
    void setConverter(RubyClass converterClass) {
        this.converterClass = converterClass;
    }

    public RubyClass getSymbol() {
        return symbolClass;
    }
    void setSymbol(RubyClass symbolClass) {
        this.symbolClass = symbolClass;
    }

    public RubyClass getArray() {
        return arrayClass;
    }    
    void setArray(RubyClass arrayClass) {
        this.arrayClass = arrayClass;
    }

    public RubyClass getHash() {
        return hashClass;
    }
    void setHash(RubyClass hashClass) {
        this.hashClass = hashClass;
    }

    public RubyClass getRange() {
        return rangeClass;
    }
    void setRange(RubyClass rangeClass) {
        this.rangeClass = rangeClass;
    }

    /** Returns the "true" instance from the instance pool.
     * @return The "true" instance.
     */
    public RubyBoolean getTrue() {
        return trueObject;
    }

    /** Returns the "false" instance from the instance pool.
     * @return The "false" instance.
     */
    public RubyBoolean getFalse() {
        return falseObject;
    }

    /** Returns the "nil" singleton instance.
     * @return "nil"
     */
    public IRubyObject getNil() {
        return nilObject;
    }

    public IRubyObject[] getSingleNilArray() {
        return singleNilArray;
    }

    public RubyClass getNilClass() {
        return nilClass;
    }
    void setNilClass(RubyClass nilClass) {
        this.nilClass = nilClass;
    }

    public RubyClass getTrueClass() {
        return trueClass;
    }
    void setTrueClass(RubyClass trueClass) {
        this.trueClass = trueClass;
    }

    public RubyClass getFalseClass() {
        return falseClass;
    }
    void setFalseClass(RubyClass falseClass) {
        this.falseClass = falseClass;
    }

    public RubyClass getProc() {
        return procClass;
    }
    void setProc(RubyClass procClass) {
        this.procClass = procClass;
    }

    public RubyClass getBinding() {
        return bindingClass;
    }
    void setBinding(RubyClass bindingClass) {
        this.bindingClass = bindingClass;
    }

    public RubyClass getMethod() {
        return methodClass;
    }
    void setMethod(RubyClass methodClass) {
        this.methodClass = methodClass;
    }    

    public RubyClass getUnboundMethod() {
        return unboundMethodClass;
    }
    void setUnboundMethod(RubyClass unboundMethodClass) {
        this.unboundMethodClass = unboundMethodClass;
    }    

    public RubyClass getMatchData() {
        return matchDataClass;
    }
    void setMatchData(RubyClass matchDataClass) {
        this.matchDataClass = matchDataClass;
    }    

    public RubyClass getRegexp() {
        return regexpClass;
    }
    void setRegexp(RubyClass regexpClass) {
        this.regexpClass = regexpClass;
    }    

    public RubyClass getTime() {
        return timeClass;
    }
    void setTime(RubyClass timeClass) {
        this.timeClass = timeClass;
    }    

    public RubyModule getMath() {
        return mathModule;
    }
    void setMath(RubyModule mathModule) {
        this.mathModule = mathModule;
    }    

    public RubyModule getMarshal() {
        return marshalModule;
    }
    void setMarshal(RubyModule marshalModule) {
        this.marshalModule = marshalModule;
    }    

    public RubyClass getBignum() {
        return bignumClass;
    }
    void setBignum(RubyClass bignumClass) {
        this.bignumClass = bignumClass;
    }    

    public RubyClass getDir() {
        return dirClass;
    }
    void setDir(RubyClass dirClass) {
        this.dirClass = dirClass;
    }    

    public RubyClass getFile() {
        return fileClass;
    }
    void setFile(RubyClass fileClass) {
        this.fileClass = fileClass;
    }    

    public RubyClass getFileStat() {
        return fileStatClass;
    }
    void setFileStat(RubyClass fileStatClass) {
        this.fileStatClass = fileStatClass;
    }    

    public RubyModule getFileTest() {
        return fileTestModule;
    }
    void setFileTest(RubyModule fileTestModule) {
        this.fileTestModule = fileTestModule;
    }
    
    public RubyClass getIO() {
        return ioClass;
    }
    void setIO(RubyClass ioClass) {
        this.ioClass = ioClass;
    }    

    public RubyClass getThread() {
        return threadClass;
    }
    void setThread(RubyClass threadClass) {
        this.threadClass = threadClass;
    }    

    public RubyClass getThreadGroup() {
        return threadGroupClass;
    }
    void setThreadGroup(RubyClass threadGroupClass) {
        this.threadGroupClass = threadGroupClass;
    }
    
    public RubyThreadGroup getDefaultThreadGroup() {
        return defaultThreadGroup;
    }
    void setDefaultThreadGroup(RubyThreadGroup defaultThreadGroup) {
        this.defaultThreadGroup = defaultThreadGroup;
    }

    public RubyClass getContinuation() {
        return continuationClass;
    }
    void setContinuation(RubyClass continuationClass) {
        this.continuationClass = continuationClass;
    }    

    public RubyClass getStructClass() {
        return structClass;
    }
    void setStructClass(RubyClass structClass) {
        this.structClass = structClass;
    }
    
    public RubyClass getRandomClass() {
        return randomClass;
    }
    void setRandomClass(RubyClass randomClass) {
        this.randomClass = randomClass;
    }

    public IRubyObject getTmsStruct() {
        return tmsStruct;
    }
    void setTmsStruct(RubyClass tmsStruct) {
        this.tmsStruct = tmsStruct;
    }
    
    public IRubyObject getPasswdStruct() {
        return passwdStruct;
    }
    public void setPasswdStruct(RubyClass passwdStruct) {
        this.passwdStruct = passwdStruct;
    }

    public IRubyObject getGroupStruct() {
        return groupStruct;
    }
    public void setGroupStruct(RubyClass groupStruct) {
        this.groupStruct = groupStruct;
    }

    public RubyModule getGC() {
        return gcModule;
    }
    void setGC(RubyModule gcModule) {
        this.gcModule = gcModule;
    }    

    public RubyModule getObjectSpaceModule() {
        return objectSpaceModule;
    }
    void setObjectSpaceModule(RubyModule objectSpaceModule) {
        this.objectSpaceModule = objectSpaceModule;
    }    

    public RubyModule getProcess() {
        return processModule;
    }
    void setProcess(RubyModule processModule) {
        this.processModule = processModule;
    }    

    public RubyClass getProcStatus() {
        return procStatusClass; 
    }
    void setProcStatus(RubyClass procStatusClass) {
        this.procStatusClass = procStatusClass;
    }
    
    public RubyModule getProcUID() {
        return procUIDModule;
    }
    void setProcUID(RubyModule procUIDModule) {
        this.procUIDModule = procUIDModule;
    }
    
    public RubyModule getProcGID() {
        return procGIDModule;
    }
    void setProcGID(RubyModule procGIDModule) {
        this.procGIDModule = procGIDModule;
    }
    
    public RubyModule getProcSysModule() {
        return procSysModule;
    }
    void setProcSys(RubyModule procSysModule) {
        this.procSysModule = procSysModule;
    }

    public RubyModule getPrecision() {
        return precisionModule;
    }
    void setPrecision(RubyModule precisionModule) {
        this.precisionModule = precisionModule;
    }
    
    public RubyHash getENV() {
        return envObject;
    }
    
    public void setENV(RubyHash env) {
        envObject = env;
    }

    public RubyModule getErrno() {
        return errnoModule;
    }

    public RubyClass getException() {
        return exceptionClass;
    }
    void setException(RubyClass exceptionClass) {
        this.exceptionClass = exceptionClass;
    }

    public RubyClass getNameError() {
        return nameError;
    }

    public RubyClass getNameErrorMessage() {
        return nameErrorMessage;
    }

    public RubyClass getNoMethodError() {
        return noMethodError;
    }

    public RubyClass getSignalException() {
        return signalException;
    }

    public RubyClass getRangeError() {
        return rangeError;
    }

    public RubyClass getSystemExit() {
        return systemExit;
    }

    public RubyClass getLocalJumpError() {
        return localJumpError;
    }

    public RubyClass getNativeException() {
        return nativeException;
    }

    public RubyClass getSystemCallError() {
        return systemCallError;
    }

    public RubyClass getKeyError() {
        return keyError;
    }

    public RubyClass getFatal() {
        return fatal;
    }
    
    public RubyClass getInterrupt() {
        return interrupt;
    }
    
    public RubyClass getTypeError() {
        return typeError;
    }

    public RubyClass getArgumentError() {
        return argumentError;
    }

    public RubyClass getIndexError() {
        return indexError;
    }

    public RubyClass getStopIteration() {
        return stopIteration;
    }

    public RubyClass getSyntaxError() {
        return syntaxError;
    }

    public RubyClass getStandardError() {
        return standardError;
    }
    
    public RubyClass getRuntimeError() {
        return runtimeError;
    }
    
    public RubyClass getIOError() {
        return ioError;
    }

    public RubyClass getLoadError() {
        return loadError;
    }

    public RubyClass getNotImplementedError() {
        return notImplementedError;
    }

    public RubyClass getSecurityError() {
        return securityError;
    }

    public RubyClass getNoMemoryError() {
        return noMemoryError;
    }

    public RubyClass getRegexpError() {
        return regexpError;
    }

    public RubyClass getEOFError() {
        return eofError;
    }

    public RubyClass getThreadError() {
        return threadError;
    }

    public RubyClass getConcurrencyError() {
        return concurrencyError;
    }

    public RubyClass getSystemStackError() {
        return systemStackError;
    }

    public RubyClass getZeroDivisionError() {
        return zeroDivisionError;
    }

    public RubyClass getFloatDomainError() {
        return floatDomainError;
    }

    public RubyClass getMathDomainError() {
        return mathDomainError;
    }

    public RubyClass getEncodingError() {
        return encodingError;
    }

    public RubyClass getEncodingCompatibilityError() {
        return encodingCompatibilityError;
    }

    public RubyClass getConverterNotFoundError() {
        return converterNotFoundError;
    }

    public RubyClass getFiberError() {
        return fiberError;
    }

    public RubyClass getUndefinedConversionError() {
        return undefinedConversionError;
    }

    public RubyClass getInvalidByteSequenceError() {
        return invalidByteSequenceError;
    }

    private RubyRandom.RandomType defaultRand;
    public RubyRandom.RandomType getDefaultRand() {
        return defaultRand;
    }
    
    public void setDefaultRand(RubyRandom.RandomType defaultRand) {
        this.defaultRand = defaultRand;
    }

    private RubyHash charsetMap;
    public RubyHash getCharsetMap() {
        if (charsetMap == null) charsetMap = new RubyHash(this);
        return charsetMap;
    }

    /** Getter for property isVerbose.
     * @return Value of property isVerbose.
     */
    public IRubyObject getVerbose() {
        return verboseValue;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean warningsEnabled() {
        return warningsEnabled;
    }

    /** Setter for property isVerbose.
     * @param verbose New value of property isVerbose.
     */
    public void setVerbose(IRubyObject verbose) {
        this.verbose = verbose.isTrue();
        this.verboseValue = verbose;
        warningsEnabled = !verbose.isNil();
    }

    /** Getter for property isDebug.
     * @return Value of property isDebug.
     */
    public IRubyObject getDebug() {
        return debug ? trueObject : falseObject;
    }

    public boolean isDebug() {
        return debug;
    }

    /** Setter for property isDebug.
     * @param debug New value of property isDebug.
     */
    public void setDebug(IRubyObject debug) {
        this.debug = debug.isTrue();
    }

    public JavaSupport getJavaSupport() {
        return javaSupport;
    }

    public static ClassLoader getClassLoader() {
        // we try to get the classloader that loaded JRuby, falling back on System
        ClassLoader loader = Ruby.class.getClassLoader();
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
        }
        
        return loader;
    }

    public synchronized JRubyClassLoader getJRubyClassLoader() {
        // FIXME: Get rid of laziness and handle restricted access elsewhere
        if (!Ruby.isSecurityRestricted() && jrubyClassLoader == null) {
            jrubyClassLoader = new JRubyClassLoader(config.getLoader());
        }
        
        return jrubyClassLoader;
    }

    /** Defines a global variable
     */
    public void defineVariable(final GlobalVariable variable) {
        globalVariables.define(variable.name(), new IAccessor() {
            public IRubyObject getValue() {
                return variable.get();
            }

            public IRubyObject setValue(IRubyObject newValue) {
                return variable.set(newValue);
            }
        });
    }

    /** defines a readonly global variable
     *
     */
    public void defineReadonlyVariable(String name, IRubyObject value) {
        globalVariables.defineReadonly(name, new ValueAccessor(value));
    }

    public Node parseFile(InputStream in, String file, DynamicScope scope, int lineNumber) {
        if (parserStats != null) parserStats.addLoadParse();
        return parser.parse(file, in, scope, new ParserConfiguration(this,
                lineNumber, false, false, true, config));
    }

    public Node parseFileFromMain(InputStream in, String file, DynamicScope scope) {
        if (parserStats != null) parserStats.addLoadParse();
        return parser.parse(file, in, scope, new ParserConfiguration(this,
                0, false, false, true, true, config));
    }
    
    public Node parseFile(InputStream in, String file, DynamicScope scope) {
        return parseFile(in, file, scope, 0);
    }

    public Node parseInline(InputStream in, String file, DynamicScope scope) {
        if (parserStats != null) parserStats.addEvalParse();
        ParserConfiguration parserConfig =
                new ParserConfiguration(this, 0, false, true, false, config);
        if (is1_9) parserConfig.setDefaultEncoding(getEncodingService().getLocaleEncoding());
        return parser.parse(file, in, scope, parserConfig);
    }

    public Node parseEval(String content, String file, DynamicScope scope, int lineNumber) {
        if (parserStats != null) parserStats.addEvalParse();
        return parser.parse(file, content.getBytes(), scope, new ParserConfiguration(this,
                lineNumber, false, false, false, false, config));
    }

    @Deprecated
    public Node parse(String content, String file, DynamicScope scope, int lineNumber, 
            boolean extraPositionInformation) {
        return parser.parse(file, content.getBytes(), scope, new ParserConfiguration(this,
                lineNumber, extraPositionInformation, false, true, config));
    }
    
    public Node parseEval(ByteList content, String file, DynamicScope scope, int lineNumber) {
        if (parserStats != null) parserStats.addEvalParse();
        return parser.parse(file, content, scope, new ParserConfiguration(this,
                lineNumber, false, false, false, config));
    }

    public Node parse(ByteList content, String file, DynamicScope scope, int lineNumber, 
            boolean extraPositionInformation) {
        if (parserStats != null) parserStats.addJRubyModuleParse();
        return parser.parse(file, content, scope, new ParserConfiguration(this,
                lineNumber, extraPositionInformation, false, true, config));
    }


    public ThreadService getThreadService() {
        return threadService;
    }

    public ThreadContext getCurrentContext() {
        return threadService.getCurrentContext();
    }

    /**
     * Returns the loadService.
     * @return ILoadService
     */
    public LoadService getLoadService() {
        return loadService;
    }

    public Encoding getDefaultInternalEncoding() {
        return defaultInternalEncoding;
    }

    public void setDefaultInternalEncoding(Encoding defaultInternalEncoding) {
        this.defaultInternalEncoding = defaultInternalEncoding;
    }

    public Encoding getDefaultExternalEncoding() {
        return defaultExternalEncoding;
    }

    public void setDefaultExternalEncoding(Encoding defaultExternalEncoding) {
        this.defaultExternalEncoding = defaultExternalEncoding;
    }

    public EncodingService getEncodingService() {
        return encodingService;
    }

    public RubyWarnings getWarnings() {
        return warnings;
    }

    public PrintStream getErrorStream() {
        // FIXME: We can't guarantee this will always be a RubyIO...so the old code here is not safe
        /*java.io.OutputStream os = ((RubyIO) getGlobalVariables().get("$stderr")).getOutStream();
        if(null != os) {
            return new PrintStream(os);
        } else {
            return new PrintStream(new org.jruby.util.SwallowingOutputStream());
        }*/
        return new PrintStream(new IOOutputStream(getGlobalVariables().get("$stderr")));
    }

    public InputStream getInputStream() {
        return new IOInputStream(getGlobalVariables().get("$stdin"));
    }

    public PrintStream getOutputStream() {
        return new PrintStream(new IOOutputStream(getGlobalVariables().get("$stdout")));
    }

    public RubyModule getClassFromPath(String path) {
        RubyModule c = getObject();
        if (path.length() == 0 || path.charAt(0) == '#') {
            throw newTypeError("can't retrieve anonymous class " + path);
        }
        int pbeg = 0, p = 0;
        for(int l=path.length(); p<l; ) {
            while(p<l && path.charAt(p) != ':') {
                p++;
            }
            String str = path.substring(pbeg, p);

            if(p<l && path.charAt(p) == ':') {
                if(p+1 < l && path.charAt(p+1) != ':') {
                    throw newTypeError("undefined class/module " + path.substring(pbeg,p));
                }
                p += 2;
                pbeg = p;
            }

            IRubyObject cc = c.getConstant(str);
            if(!(cc instanceof RubyModule)) {
                throw newTypeError("" + path + " does not refer to class/module");
            }
            c = (RubyModule)cc;
        }
        return c;
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

        PrintStream errorStream = getErrorStream();
        errorStream.print(config.getTraceType().printBacktrace(excp, errorStream == System.err && getPosix().isatty(FileDescriptor.err)));
    }
    
    public void loadFile(String scriptName, InputStream in, boolean wrap) {
        IRubyObject self = wrap ? TopSelfFactory.createTopSelf(this) : getTopSelf();
        ThreadContext context = getCurrentContext();
        String file = context.getFile();
        
        try {
            secure(4); /* should alter global state */

            ThreadContext.pushBacktrace(context, "(root)", file, 0);
            context.preNodeEval(objectClass, self, scriptName);

            Node node = parseFile(in, scriptName, null);
            if (wrap) {
                // toss an anonymous module into the search path
                ((RootNode)node).getStaticScope().setModule(RubyModule.newModule(this));
            }
            runInterpreter(context, node, self);
        } catch (JumpException.ReturnJump rj) {
            return;
        } finally {
            context.postNodeEval();
            ThreadContext.popBacktrace(context);
        }
    }
    
    public void compileAndLoadFile(String filename, InputStream in, boolean wrap) {
        ThreadContext context = getCurrentContext();
        String file = context.getFile();
        InputStream readStream = in;
        
        try {
            secure(4); /* should alter global state */

            Script script = null;
            String className = null;

            try {
                // read full contents of file, hash it, and try to load that class first
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int num;
                while ((num = in.read(buffer)) > -1) {
                    baos.write(buffer, 0, num);
                }
                buffer = baos.toByteArray();
                String hash = JITCompiler.getHashForBytes(buffer);
                className = JITCompiler.RUBY_JIT_PREFIX + ".FILE_" + hash;

                // FIXME: duplicated from ClassCache
                Class contents;
                try {
                    contents = jrubyClassLoader.loadClass(className);
                    if (RubyInstanceConfig.JIT_LOADING_DEBUG) {
                        LOG.info("found jitted code for " + filename + " at class: " + className);
                    }
                    script = (Script)contents.newInstance();
                    readStream = new ByteArrayInputStream(buffer);
                } catch (ClassNotFoundException cnfe) {
                    if (RubyInstanceConfig.JIT_LOADING_DEBUG) {
                        LOG.info("no jitted code in classloader for file " + filename + " at class: " + className);
                    }
                } catch (InstantiationException ie) {
                    if (RubyInstanceConfig.JIT_LOADING_DEBUG) {
                        LOG.info("jitted code could not be instantiated for file " + filename + " at class: " + className);
                    }
                } catch (IllegalAccessException iae) {
                    if (RubyInstanceConfig.JIT_LOADING_DEBUG) {
                        LOG.info("jitted code could not be instantiated for file " + filename + " at class: " + className);
                    }
                }
            } catch (IOException ioe) {
                // TODO: log something?
            }

            // script was not found in cache above, so proceed to compile
            Node scriptNode = parseFile(readStream, filename, null);
            if (script == null) {
                script = tryCompile(scriptNode, className, new JRubyClassLoader(jrubyClassLoader), false);
            }

            if (script == null) {
                failForcedCompile(scriptNode);

                runInterpreter(scriptNode);
            } else {
                runScript(script, wrap);
            }
        } catch (JumpException.ReturnJump rj) {
            return;
        }
    }

    public void loadScript(Script script) {
        loadScript(script, false);
    }

    public void loadScript(Script script, boolean wrap) {
        IRubyObject self = getTopSelf();
        ThreadContext context = getCurrentContext();

        try {
            secure(4); /* should alter global state */
            
            script.load(context, self, wrap);
        } catch (JumpException.ReturnJump rj) {
            return;
        }
    }

    /**
     * Load the given BasicLibraryService instance, wrapping it in Ruby framing
     * to ensure it is isolated from any parent scope.
     * 
     * @param extName The name of the extension, to go on the frame wrapping it
     * @param extension The extension object to load
     * @param wrap Whether to use a new "self" for toplevel
     */
    public void loadExtension(String extName, BasicLibraryService extension, boolean wrap) {
        IRubyObject self = wrap ? TopSelfFactory.createTopSelf(this) : getTopSelf();
        ThreadContext context = getCurrentContext();

        try {
            secure(4); /* should alter global state */

            context.preExtensionLoad(self);

            extension.basicLoad(this);
        } catch (IOException ioe) {
            throw newIOErrorFromException(ioe);
        } catch (JumpException.ReturnJump rj) {
            return;
        } finally {
            context.postNodeEval();
        }
    }

    public void addBoundMethod(String className, String methodName, String rubyName) {
        Map<String, String> javaToRuby = boundMethods.get(className);
        if (javaToRuby == null) {
            javaToRuby = new HashMap<String, String>();
            boundMethods.put(className, javaToRuby);
        }
        javaToRuby.put(methodName, rubyName);
    }

    public Map<String, Map<String, String>> getBoundMethods() {
        return boundMethods;
    }

    public class CallTraceFuncHook extends EventHook {
        private RubyProc traceFunc;
        
        public void setTraceFunc(RubyProc traceFunc) {
            this.traceFunc = traceFunc;
        }
        
        public void eventHandler(ThreadContext context, String eventName, String file, int line, String name, IRubyObject type) {
            if (!context.isWithinTrace()) {
                if (file == null) file = "(ruby)";
                if (type == null) type = getFalse();
                
                RubyBinding binding = RubyBinding.newBinding(Ruby.this, context.currentBinding());

                context.preTrace();
                try {
                    traceFunc.call(context, new IRubyObject[] {
                        newString(eventName), // event name
                        newString(file), // filename
                        newFixnum(line), // line numbers should be 1-based
                        name != null ? newSymbol(name) : getNil(),
                        binding,
                        type
                    });
                } finally {
                    context.postTrace();
                }
            }
        }

        public boolean isInterestedInEvent(RubyEvent event) {
            return true;
        }
    };
    
    private final CallTraceFuncHook callTraceFuncHook = new CallTraceFuncHook();
    
    public void addEventHook(EventHook hook) {
        if (!RubyInstanceConfig.FULL_TRACE_ENABLED) {
            // without full tracing, many events will not fire
            getWarnings().warn("tracing (e.g. set_trace_func) will not capture all events without --debug flag");
        }
        eventHooks.add(hook);
        hasEventHooks = true;
    }
    
    public void removeEventHook(EventHook hook) {
        eventHooks.remove(hook);
        hasEventHooks = !eventHooks.isEmpty();
    }

    public void setTraceFunction(RubyProc traceFunction) {
        removeEventHook(callTraceFuncHook);
        
        if (traceFunction == null) {
            return;
        }
        
        callTraceFuncHook.setTraceFunc(traceFunction);
        addEventHook(callTraceFuncHook);
    }
    
    public void callEventHooks(ThreadContext context, RubyEvent event, String file, int line, String name, IRubyObject type) {
        if (context.isEventHooksEnabled()) {
            for (EventHook eventHook : eventHooks) {
                if (eventHook.isInterestedInEvent(event)) {
                    eventHook.event(context, event, file, line, name, type);
                }
            }
        }
    }
    
    public boolean hasEventHooks() {
        return hasEventHooks;
    }
    
    public GlobalVariables getGlobalVariables() {
        return globalVariables;
    }

    // For JSR 223 support: see http://scripting.java.net/
    public void setGlobalVariables(GlobalVariables globalVariables) {
        this.globalVariables = globalVariables;
    }

    public CallbackFactory callbackFactory(Class<?> type) {
        return CallbackFactory.createFactory(this, type);
    }

    /**
     * Push block onto exit stack.  When runtime environment exits
     * these blocks will be evaluated.
     *
     * @return the element that was pushed onto stack
     */
    public IRubyObject pushExitBlock(RubyProc proc) {
        atExitBlocks.push(proc);
        return proc;
    }

    // use this for JRuby-internal finalizers
    public void addInternalFinalizer(Finalizable finalizer) {
        synchronized (internalFinalizersMutex) {
            if (internalFinalizers == null) {
                internalFinalizers = new WeakHashMap<Finalizable, Object>();
            }
            internalFinalizers.put(finalizer, null);
        }
    }

    // this method is for finalizers registered via ObjectSpace
    public void addFinalizer(Finalizable finalizer) {
        synchronized (finalizersMutex) {
            if (finalizers == null) {
                finalizers = new WeakHashMap<Finalizable, Object>();
            }
            finalizers.put(finalizer, null);
        }
    }
    
    public void removeInternalFinalizer(Finalizable finalizer) {
        synchronized (internalFinalizersMutex) {
            if (internalFinalizers != null) {
                internalFinalizers.remove(finalizer);
            }
        }
    }

    public void removeFinalizer(Finalizable finalizer) {
        synchronized (finalizersMutex) {
            if (finalizers != null) {
                finalizers.remove(finalizer);
            }
        }
    }

    /**
     * Make sure Kernel#at_exit procs get invoked on runtime shutdown.
     * This method needs to be explicitly called to work properly.
     * I thought about using finalize(), but that did not work and I
     * am not sure the runtime will be at a state to run procs by the
     * time Ruby is going away.  This method can contain any other
     * things that need to be cleaned up at shutdown.
     */
    public void tearDown() {
        tearDown(true);
    }

    // tearDown(boolean) has been added for embedding API. When an error
    // occurs in Ruby code, JRuby does system exit abruptly, no chance to
    // catch exception. This makes debugging really hard. This is why
    // tearDown(boolean) exists.
    public void tearDown(boolean systemExit) {
        int status = 0;

        // clear out threadlocals so they don't leak
        recursive = new ThreadLocal<Map<String, RubyHash>>();

        while (!atExitBlocks.empty()) {
            RubyProc proc = atExitBlocks.pop();
            try {
                proc.call(getCurrentContext(), IRubyObject.NULL_ARRAY);
            } catch (RaiseException rj) {
                RubyException raisedException = rj.getException();
                if (!getSystemExit().isInstance(raisedException)) {
                    status = 1;
                    printError(raisedException);
                } else {
                    IRubyObject statusObj = raisedException.callMethod(
                            getCurrentContext(), "status");
                    if (statusObj != null && !statusObj.isNil()) {
                        status = RubyNumeric.fix2int(statusObj);
                    }
                }
            }
        }

        if (finalizers != null) {
            synchronized (finalizersMutex) {
                for (Iterator<Finalizable> finalIter = new ArrayList<Finalizable>(finalizers.keySet()).iterator(); finalIter.hasNext();) {
                    Finalizable f = finalIter.next();
                    if (f != null) {
                        try {
                            f.finalize();
                        } catch (Throwable t) {
                            // ignore
                        }
                    }
                    finalIter.remove();
                }
            }
        }

        synchronized (internalFinalizersMutex) {
            if (internalFinalizers != null) {
                for (Iterator<Finalizable> finalIter = new ArrayList<Finalizable>(
                        internalFinalizers.keySet()).iterator(); finalIter.hasNext();) {
                    Finalizable f = finalIter.next();
                    if (f != null) {
                        try {
                            f.finalize();
                        } catch (Throwable t) {
                            // ignore
                        }
                    }
                    finalIter.remove();
                }
            }
        }

        getThreadService().disposeCurrentThread();

        getBeanManager().unregisterCompiler();
        getBeanManager().unregisterConfig();
        getBeanManager().unregisterParserStats();
        getBeanManager().unregisterClassCache();
        getBeanManager().unregisterMethodCache();
        getBeanManager().unregisterRuntime();

        getSelectorPool().cleanup();

        getJITCompiler().tearDown();

        if (getJRubyClassLoader() != null) {
            getJRubyClassLoader().tearDown(isDebug());
        }

        if (config.isProfilingEntireRun()) {
            // not using logging because it's formatted
            System.err.println("\nmain thread profile results:");
            IProfileData profileData = (IProfileData) threadService.getMainThread().getContext().getProfileData();
            config.makeDefaultProfilePrinter(profileData).printProfile(System.err);
        }

        if (systemExit && status != 0) {
            throw newSystemExit(status);
        }
    }

    // new factory methods ------------------------------------------------------------------------

    public RubyArray newEmptyArray() {
        return RubyArray.newEmptyArray(this);
    }

    public RubyArray newArray() {
        return RubyArray.newArray(this);
    }

    public RubyArray newArrayLight() {
        return RubyArray.newArrayLight(this);
    }

    public RubyArray newArray(IRubyObject object) {
        return RubyArray.newArray(this, object);
    }

    public RubyArray newArray(IRubyObject car, IRubyObject cdr) {
        return RubyArray.newArray(this, car, cdr);
    }

    public RubyArray newArray(IRubyObject... objects) {
        return RubyArray.newArray(this, objects);
    }
    
    public RubyArray newArrayNoCopy(IRubyObject... objects) {
        return RubyArray.newArrayNoCopy(this, objects);
    }
    
    public RubyArray newArrayNoCopyLight(IRubyObject... objects) {
        return RubyArray.newArrayNoCopyLight(this, objects);
    }
    
    public RubyArray newArray(List<IRubyObject> list) {
        return RubyArray.newArray(this, list);
    }

    public RubyArray newArray(int size) {
        return RubyArray.newArray(this, size);
    }

    public RubyBoolean newBoolean(boolean value) {
        return value ? trueObject : falseObject;
    }

    public RubyFileStat newFileStat(String filename, boolean lstat) {
        return RubyFileStat.newFileStat(this, filename, lstat);
    }
    
    public RubyFileStat newFileStat(FileDescriptor descriptor) {
        return RubyFileStat.newFileStat(this, descriptor);
    }

    public RubyFixnum newFixnum(long value) {
        return RubyFixnum.newFixnum(this, value);
    }

    public RubyFixnum newFixnum(int value) {
        return RubyFixnum.newFixnum(this, value);
    }

    public RubyFixnum newFixnum(Constant value) {
        return RubyFixnum.newFixnum(this, value.intValue());
    }

    public RubyFloat newFloat(double value) {
        return RubyFloat.newFloat(this, value);
    }

    public RubyNumeric newNumeric() {
        return RubyNumeric.newNumeric(this);
    }

    public RubyRational newRational(long num, long den) {
        return RubyRational.newRationalRaw(this, newFixnum(num), newFixnum(den));
    }

    public RubyRational newRationalReduced(long num, long den) {
        return (RubyRational)RubyRational.newRationalConvert(getCurrentContext(), newFixnum(num), newFixnum(den));
    }

    public RubyProc newProc(Block.Type type, Block block) {
        if (type != Block.Type.LAMBDA && block.getProcObject() != null) return block.getProcObject();

        RubyProc proc =  RubyProc.newProc(this, block, type);

        return proc;
    }

    public RubyProc newBlockPassProc(Block.Type type, Block block) {
        if (type != Block.Type.LAMBDA && block.getProcObject() != null) return block.getProcObject();

        RubyProc proc =  RubyProc.newProc(this, block, type);

        return proc;
    }

    public RubyBinding newBinding() {
        return RubyBinding.newBinding(this, getCurrentContext().currentBinding());
    }

    public RubyBinding newBinding(Binding binding) {
        return RubyBinding.newBinding(this, binding);
    }

    public RubyString newString() {
        return RubyString.newString(this, new ByteList());
    }

    public RubyString newString(String string) {
        return RubyString.newString(this, string);
    }
    
    public RubyString newString(ByteList byteList) {
        return RubyString.newString(this, byteList);
    }

    @Deprecated
    public RubyString newStringShared(ByteList byteList) {
        return RubyString.newStringShared(this, byteList);
    }    

    public RubySymbol newSymbol(String name) {
        return symbolTable.getSymbol(name);
    }

    public RubySymbol newSymbol(ByteList name) {
        return symbolTable.getSymbol(name);
    }

    /**
     * Faster than {@link #newSymbol(String)} if you already have an interned
     * name String. Don't intern your string just to call this version - the
     * overhead of interning will more than wipe out any benefit from the faster
     * lookup.
     *   
     * @param internedName the symbol name, <em>must</em> be interned! if in
     *                     doubt, call {@link #newSymbol(String)} instead.
     * @return the symbol for name
     */
    public RubySymbol fastNewSymbol(String internedName) {
        //        assert internedName == internedName.intern() : internedName + " is not interned";

        return symbolTable.fastGetSymbol(internedName);
    }

    public RubyTime newTime(long milliseconds) {
        return RubyTime.newTime(this, milliseconds);
    }

    public RaiseException newRuntimeError(String message) {
        return newRaiseException(getRuntimeError(), message);
    }    
    
    public RaiseException newArgumentError(String message) {
        return newRaiseException(getArgumentError(), message);
    }

    public RaiseException newArgumentError(int got, int expected) {
        return newRaiseException(getArgumentError(), "wrong number of arguments (" + got + " for " + expected + ")");
    }

    public RaiseException newErrnoEBADFError() {
        return newRaiseException(getErrno().getClass("EBADF"), "Bad file descriptor");
    }

    public RaiseException newErrnoEISCONNError() {
        return newRaiseException(getErrno().getClass("EISCONN"), "Socket is already connected");
    }

    public RaiseException newErrnoEINPROGRESSError() {
        return newRaiseException(getErrno().getClass("EINPROGRESS"), "Operation now in progress");
    }

    public RaiseException newErrnoEINPROGRESSWritableError() {
        return newLightweightErrnoException(getModule("JRuby").getClass("EINPROGRESSWritable"), "");
    }

    public RaiseException newErrnoENOPROTOOPTError() {
        return newRaiseException(getErrno().getClass("ENOPROTOOPT"), "Protocol not available");
    }

    public RaiseException newErrnoEPIPEError() {
        return newRaiseException(getErrno().getClass("EPIPE"), "Broken pipe");
    }

    public RaiseException newErrnoECONNABORTEDError() {
        return newRaiseException(getErrno().getClass("ECONNABORTED"),
                "An established connection was aborted by the software in your host machine");
    }

    public RaiseException newErrnoECONNREFUSEDError() {
        return newRaiseException(getErrno().getClass("ECONNREFUSED"), "Connection refused");
    }

    public RaiseException newErrnoECONNRESETError() {
        return newRaiseException(getErrno().getClass("ECONNRESET"), "Connection reset by peer");
    }

    public RaiseException newErrnoEADDRINUSEError() {
        return newRaiseException(getErrno().getClass("EADDRINUSE"), "Address in use");
    }

    public RaiseException newErrnoEADDRINUSEError(String message) {
        return newRaiseException(getErrno().getClass("EADDRINUSE"), message);
    }

    public RaiseException newErrnoEHOSTUNREACHError(String message) {
        return newRaiseException(getErrno().getClass("EHOSTUNREACH"), message);
    }

    public RaiseException newErrnoEINVALError() {
        return newRaiseException(getErrno().getClass("EINVAL"), "Invalid file");
    }

    public RaiseException newErrnoENOENTError() {
        return newRaiseException(getErrno().getClass("ENOENT"), "File not found");
    }

    public RaiseException newErrnoEACCESError(String message) {
        return newRaiseException(getErrno().getClass("EACCES"), message);
    }

    public RaiseException newErrnoEAGAINError(String message) {
        return newLightweightErrnoException(getErrno().getClass("EAGAIN"), message);
    }

    public RaiseException newErrnoEAGAINReadableError(String message) {
        return newLightweightErrnoException(getModule("JRuby").getClass("EAGAINReadable"), message);
    }

    public RaiseException newErrnoEAGAINWritableError(String message) {
        return newLightweightErrnoException(getModule("JRuby").getClass("EAGAINWritable"), message);
    }

    public RaiseException newErrnoEISDirError(String message) {
        return newRaiseException(getErrno().getClass("EISDIR"), message);
    }

    public RaiseException newErrnoEPERMError(String name) {
        return newRaiseException(getErrno().getClass("EPERM"), "Operation not permitted - " + name);
    }

    public RaiseException newErrnoEISDirError() {
        return newErrnoEISDirError("Is a directory");
    }

    public RaiseException newErrnoESPIPEError() {
        return newRaiseException(getErrno().getClass("ESPIPE"), "Illegal seek");
    }

    public RaiseException newErrnoEBADFError(String message) {
        return newRaiseException(getErrno().getClass("EBADF"), message);
    }

    public RaiseException newErrnoEINPROGRESSError(String message) {
        return newRaiseException(getErrno().getClass("EINPROGRESS"), message);
    }

    public RaiseException newErrnoEINPROGRESSWritableError(String message) {
        return newLightweightErrnoException(getModule("JRuby").getClass("EINPROGRESSWritable"), message);
    }

    public RaiseException newErrnoEISCONNError(String message) {
        return newRaiseException(getErrno().getClass("EISCONN"), message);
    }

    public RaiseException newErrnoEINVALError(String message) {
        return newRaiseException(getErrno().getClass("EINVAL"), message);
    }

    public RaiseException newErrnoENOTDIRError(String message) {
        return newRaiseException(getErrno().getClass("ENOTDIR"), message);
    }

    public RaiseException newErrnoENOTEMPTYError(String message) {
        return newRaiseException(getErrno().getClass("ENOTEMPTY"), message);
    }

    public RaiseException newErrnoENOTSOCKError(String message) {
        return newRaiseException(getErrno().getClass("ENOTSOCK"), message);
    }

    public RaiseException newErrnoENOTCONNError(String message) {
        return newRaiseException(getErrno().getClass("ENOTCONN"), message);
    }

    public RaiseException newErrnoENOTCONNError() {
        return newRaiseException(getErrno().getClass("ENOTCONN"), "Socket is not connected");
    }

    public RaiseException newErrnoENOENTError(String message) {
        return newRaiseException(getErrno().getClass("ENOENT"), message);
    }

    public RaiseException newErrnoESPIPEError(String message) {
        return newRaiseException(getErrno().getClass("ESPIPE"), message);
    }

    public RaiseException newErrnoEEXISTError(String message) {
        return newRaiseException(getErrno().getClass("EEXIST"), message);
    }
    
    public RaiseException newErrnoEDOMError(String message) {
        return newRaiseException(getErrno().getClass("EDOM"), "Domain error - " + message);
    }   
    
    public RaiseException newErrnoECHILDError() {
        return newRaiseException(getErrno().getClass("ECHILD"), "No child processes");
    }    

    public RaiseException newErrnoEADDRNOTAVAILError(String message) {
        return newRaiseException(getErrno().getClass("EADDRNOTAVAIL"), message);
    }

    public RaiseException newErrnoESRCHError() {
        return newRaiseException(getErrno().getClass("ESRCH"), null);
    }

    public RaiseException newErrnoEWOULDBLOCKError() {
        return newRaiseException(getErrno().getClass("EWOULDBLOCK"), null);
    }

    public RaiseException newErrnoEDESTADDRREQError(String func) {
        return newRaiseException(getErrno().getClass("EDESTADDRREQ"), func);
    }

    public RaiseException newIndexError(String message) {
        return newRaiseException(getIndexError(), message);
    }

    public RaiseException newSecurityError(String message) {
        return newRaiseException(getSecurityError(), message);
    }

    public RaiseException newSystemCallError(String message) {
        return newRaiseException(getSystemCallError(), message);
    }

    public RaiseException newKeyError(String message) {
        return newRaiseException(getKeyError(), message);
    }

    public RaiseException newErrnoFromLastPOSIXErrno() {
        return newRaiseException(getErrno(getPosix().errno()), null);
    }

    public RaiseException newErrnoFromInt(int errno, String message) {
        RubyClass errnoClass = getErrno(errno);
        if (errnoClass != null) {
            return newRaiseException(errnoClass, message);
        } else {
            return newSystemCallError("Unknown Error (" + errno + ") - " + message);
        }
    }

    public RaiseException newErrnoFromInt(int errno) {
        Errno errnoObj = Errno.valueOf(errno);
        if (errnoObj == null) {
            return newSystemCallError("Unknown Error (" + errno + ")");
        }
        String message = errnoObj.description();
        return newErrnoFromInt(errno, message);
    }

    private final static Pattern ADDR_NOT_AVAIL_PATTERN = Pattern.compile("assign.*address");

    public RaiseException newErrnoEADDRFromBindException(BindException be) {
        String msg = be.getMessage();
        if (msg == null) {
            msg = "bind";
        } else {
            msg = "bind - " + msg;
        }
        // This is ugly, but what can we do, Java provides the same BindingException
        // for both EADDRNOTAVAIL and EADDRINUSE, so we differentiate the errors
        // based on BindException's message.
        if(ADDR_NOT_AVAIL_PATTERN.matcher(msg).find()) {
            return newErrnoEADDRNOTAVAILError(msg);
        } else {
            return newErrnoEADDRINUSEError(msg);
        }
    }

    public RaiseException newTypeError(String message) {
        return newRaiseException(getTypeError(), message);
    }

    public RaiseException newThreadError(String message) {
        return newRaiseException(getThreadError(), message);
    }

    public RaiseException newConcurrencyError(String message) {
        return newRaiseException(getConcurrencyError(), message);
    }

    public RaiseException newSyntaxError(String message) {
        return newRaiseException(getSyntaxError(), message);
    }

    public RaiseException newRegexpError(String message) {
        return newRaiseException(getRegexpError(), message);
    }

    public RaiseException newRangeError(String message) {
        return newRaiseException(getRangeError(), message);
    }

    public RaiseException newNotImplementedError(String message) {
        return newRaiseException(getNotImplementedError(), message);
    }
    
    public RaiseException newInvalidEncoding(String message) {
        return newRaiseException(fastGetClass("Iconv").getClass("InvalidEncoding"), message);
    }
    
    public RaiseException newIllegalSequence(String message) {
        return newRaiseException(fastGetClass("Iconv").getClass("IllegalSequence"), message);
    }

    public RaiseException newNoMethodError(String message, String name, IRubyObject args) {
        return new RaiseException(new RubyNoMethodError(this, getNoMethodError(), message, name, args), true);
    }

    public RaiseException newNameError(String message, String name) {
        return newNameError(message, name, null);
    }

    public RaiseException newNameError(String message, String name, Throwable origException) {
        return newNameError(message, name, origException, true);
    }

    public RaiseException newNameError(String message, String name, Throwable origException, boolean printWhenVerbose) {
        if (origException != null) {
            if (printWhenVerbose && isVerbose()) {
                LOG.error(origException.getMessage(), origException);
            } else if (isDebug()) {
                LOG.debug(origException.getMessage(), origException);
            }
        }
        
        return new RaiseException(new RubyNameError(
                this, getNameError(), message, name), false);
    }

    public RaiseException newLocalJumpError(RubyLocalJumpError.Reason reason, IRubyObject exitValue, String message) {
        return new RaiseException(new RubyLocalJumpError(this, getLocalJumpError(), message, reason, exitValue), true);
    }

    public RaiseException newLocalJumpErrorNoBlock() {
        return newLocalJumpError(RubyLocalJumpError.Reason.NOREASON, getNil(), "no block given");
    }

    public RaiseException newRedoLocalJumpError() {
        return newLocalJumpError(RubyLocalJumpError.Reason.REDO, getNil(), "unexpected redo");
    }

    public RaiseException newLoadError(String message) {
        return newRaiseException(getLoadError(), message);
    }

    public RaiseException newFrozenError(String objectType) {
        return newFrozenError(objectType, false);
    }

    public RaiseException newFrozenError(String objectType, boolean runtimeError) {
        // TODO: Should frozen error have its own distinct class?  If not should more share?
        return newRaiseException(is1_9() || runtimeError ? getRuntimeError() : getTypeError(), "can't modify frozen " + objectType);
    }

    public RaiseException newSystemStackError(String message) {
        return newRaiseException(getSystemStackError(), message);
    }

    public RaiseException newSystemStackError(String message, StackOverflowError soe) {
        if (getDebug().isTrue()) {
            LOG.debug(soe.getMessage(), soe);
        }
        return newRaiseException(getSystemStackError(), message);
    }

    public RaiseException newSystemExit(int status) {
        return new RaiseException(RubySystemExit.newInstance(this, status));
    }

    public RaiseException newIOError(String message) {
        return newRaiseException(getIOError(), message);
    }

    public RaiseException newStandardError(String message) {
        return newRaiseException(getStandardError(), message);
    }

    public RaiseException newIOErrorFromException(IOException ioe) {
        if (ioe instanceof ClosedChannelException) {
            throw newIOError("closed stream");
        }

        // TODO: this is kinda gross
        if(ioe.getMessage() != null) {
            if (ioe.getMessage().equals("Broken pipe")) {
                throw newErrnoEPIPEError();
            } else if (ioe.getMessage().equals("Connection reset by peer") ||
                    (Platform.IS_WINDOWS && ioe.getMessage().contains("connection was aborted"))) {
                throw newErrnoECONNRESETError();
            }
            return newRaiseException(getIOError(), ioe.getMessage());
        } else {
            return newRaiseException(getIOError(), "IO Error");
        }
    }

    public RaiseException newTypeError(IRubyObject receivedObject, RubyClass expectedType) {
        return newTypeError(receivedObject, expectedType.getName());
    }

    public RaiseException newTypeError(IRubyObject receivedObject, RubyModule expectedType) {
        return newTypeError(receivedObject, expectedType.getName());
    }

    public RaiseException newTypeError(IRubyObject receivedObject, String expectedType) {
        return newRaiseException(getTypeError(), "wrong argument type " +
                receivedObject.getMetaClass().getRealClass() + " (expected " + expectedType + ")");
    }

    public RaiseException newEOFError() {
        return newRaiseException(getEOFError(), "End of file reached");
    }

    public RaiseException newEOFError(String message) {
        return newRaiseException(getEOFError(), message);
    }

    public RaiseException newZeroDivisionError() {
        return newRaiseException(getZeroDivisionError(), "divided by 0");
    }

    public RaiseException newFloatDomainError(String message){
        return newRaiseException(getFloatDomainError(), message);
    }

    public RaiseException newMathDomainError(String message) {
        return newRaiseException(getMathDomainError(), "Numerical argument is out of domain - \"" + message + "\"");
    }

    public RaiseException newEncodingError(String message){
        return newRaiseException(getEncodingError(), message);
    }

    public RaiseException newEncodingCompatibilityError(String message){
        return newRaiseException(getEncodingCompatibilityError(), message);
    }

    public RaiseException newConverterNotFoundError(String message) {
        return newRaiseException(getConverterNotFoundError(), message);
    }

    public RaiseException newFiberError(String message) {
        return newRaiseException(getFiberError(), message);
    }

    public RaiseException newUndefinedConversionError(String message) {
        return newRaiseException(getUndefinedConversionError(), message);
    }

    public RaiseException newInvalidByteSequenceError(String message) {
        return newRaiseException(getInvalidByteSequenceError(), message);
    }

    /**
     * @param exceptionClass
     * @param message
     * @return
     */
    public RaiseException newRaiseException(RubyClass exceptionClass, String message) {
        return new RaiseException(this, exceptionClass, message, true);
    }

    /**
     * Generate one of the ERRNO exceptions. This differs from the normal logic
     * by avoiding the generation of a backtrace. Many ERRNO values are expected,
     * such as EAGAIN, and JRuby pays a very high cost to generate backtraces that
     * are never used. The flags -Xerrno.backtrace=true or the property
     * jruby.errno.backtrace=true forces all errno exceptions to generate a backtrace.
     * 
     * @param exceptionClass
     * @param message
     * @return
     */
    private RaiseException newLightweightErrnoException(RubyClass exceptionClass, String message) {
        if (RubyInstanceConfig.ERRNO_BACKTRACE) {
            return new RaiseException(this, exceptionClass, message, true);
        } else {
            return new RaiseException(this, exceptionClass, ERRNO_BACKTRACE_MESSAGE, RubyArray.newEmptyArray(this), true);
        }
    }

    // Equivalent of Data_Wrap_Struct
    public RubyObject.Data newData(RubyClass objectClass, Object sval) {
        return new RubyObject.Data(this, objectClass, sval);
    }

    public RubySymbol.SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public ObjectSpace getObjectSpace() {
        return objectSpace;
    }

    private final Map<Integer, Integer> filenoExtIntMap = new HashMap<Integer, Integer>();
    private final Map<Integer, Integer> filenoIntExtMap = new HashMap<Integer, Integer>();

    public void putFilenoMap(int external, int internal) {
        filenoExtIntMap.put(external, internal);
        filenoIntExtMap.put(internal, external);
    }

    public int getFilenoExtMap(int external) {
        Integer internal = filenoExtIntMap.get(external);
        if (internal != null) return internal;
        return external;
    }

    public int getFilenoIntMap(int internal) {
        Integer external = filenoIntExtMap.get(internal);
        if (external != null) return external;
        return internal;
    }

    public int getFilenoIntMapSize() {
        return filenoIntExtMap.size();
    }

    public void removeFilenoIntMap(int internal) {
        filenoIntExtMap.remove(internal);
    }

    /**
     * Get the "external" fileno for a given ChannelDescriptor. Primarily for
     * the shared 0, 1, and 2 filenos, which we can't actually share across
     * JRuby runtimes.
     *
     * @param descriptor The descriptor for which to get the fileno
     * @return The external fileno for the descriptor
     */
    public int getFileno(ChannelDescriptor descriptor) {
        return getFilenoIntMap(descriptor.getFileno());
    }

    @Deprecated
    public void registerDescriptor(ChannelDescriptor descriptor, boolean isRetained) {
    }

    @Deprecated
    public void registerDescriptor(ChannelDescriptor descriptor) {
    }

    @Deprecated
    public void unregisterDescriptor(int aFileno) {
    }

    @Deprecated
    public ChannelDescriptor getDescriptorByFileno(int aFileno) {
        return ChannelDescriptor.getDescriptorByFileno(aFileno);
    }

    public InputStream getIn() {
        return in;
    }

    public PrintStream getOut() {
        return out;
    }

    public PrintStream getErr() {
        return err;
    }

    public boolean isGlobalAbortOnExceptionEnabled() {
        return globalAbortOnExceptionEnabled;
    }

    public void setGlobalAbortOnExceptionEnabled(boolean enable) {
        globalAbortOnExceptionEnabled = enable;
    }

    public boolean isDoNotReverseLookupEnabled() {
        return doNotReverseLookupEnabled;
    }

    public void setDoNotReverseLookupEnabled(boolean b) {
        doNotReverseLookupEnabled = b;
    }

    private ThreadLocal<Map<Object, Object>> inspect = new ThreadLocal<Map<Object, Object>>();
    public void registerInspecting(Object obj) {
        Map<Object, Object> val = inspect.get();
        if (val == null) inspect.set(val = new IdentityHashMap<Object, Object>());
        val.put(obj, null);
    }

    public boolean isInspecting(Object obj) {
        Map<Object, Object> val = inspect.get();
        return val == null ? false : val.containsKey(obj);
    }

    public void unregisterInspecting(Object obj) {
        Map<Object, Object> val = inspect.get();
        if (val != null ) val.remove(obj);
    }

    public static interface RecursiveFunction {
        IRubyObject call(IRubyObject obj, boolean recur);
    }

    private static class RecursiveError extends Error implements Unrescuable {
        public RecursiveError(Object tag) {
            this.tag = tag;
        }
        public final Object tag;
        
        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }

    private IRubyObject recursiveListAccess() {
        Map<String, RubyHash> hash = recursive.get();
        String sym = getCurrentContext().getFrameName();
        IRubyObject list = getNil();
        if(hash == null) {
            hash = new HashMap<String, RubyHash>();
            recursive.set(hash);
        } else {
            list = hash.get(sym);
        }
        if(list == null || list.isNil()) {
            list = RubyHash.newHash(this);
            list.setUntrusted(true);
            hash.put(sym, (RubyHash)list);
        }
        return list;
    }

    private void recursiveListClear() {
        Map<String, RubyHash> hash = recursive.get();
        if(hash != null) {
            hash.clear();
        }
    }

    private static class ExecRecursiveParams {
        public ExecRecursiveParams() {}
        public RecursiveFunction func;
        public IRubyObject list;
        public IRubyObject obj;
        public IRubyObject objid;
        public IRubyObject pairid;
    }

    private void recursivePush(IRubyObject list, IRubyObject obj, IRubyObject paired_obj) {
        IRubyObject pair_list;
        if(paired_obj == null) {
            ((RubyHash)list).op_aset(getCurrentContext(), obj, getTrue());
        } else if((pair_list = ((RubyHash)list).fastARef(obj)) == null) {
            ((RubyHash)list).op_aset(getCurrentContext(), obj, paired_obj);
        } else {
            if(!(pair_list instanceof RubyHash)) {
                IRubyObject other_paired_obj = pair_list;
                pair_list = RubyHash.newHash(this);
                pair_list.setUntrusted(true);
                ((RubyHash)pair_list).op_aset(getCurrentContext(), other_paired_obj, getTrue());
                ((RubyHash)list).op_aset(getCurrentContext(), obj, pair_list);
            }
            ((RubyHash)pair_list).op_aset(getCurrentContext(), paired_obj, getTrue());
        }
    }

    private void recursivePop(IRubyObject list, IRubyObject obj, IRubyObject paired_obj) {
        if(paired_obj != null) {
            IRubyObject pair_list = ((RubyHash)list).fastARef(obj);
            if(pair_list == null) {
                throw newTypeError("invalid inspect_tbl pair_list for " + getCurrentContext().getFrameName());
            }
            if(pair_list instanceof RubyHash) {
                ((RubyHash)pair_list).delete(getCurrentContext(), paired_obj, Block.NULL_BLOCK);
                if(!((RubyHash)pair_list).isEmpty()) {
                    return;
                }
            }
        }
        ((RubyHash)list).delete(getCurrentContext(), obj, Block.NULL_BLOCK);
    }

    private boolean recursiveCheck(IRubyObject list, IRubyObject obj_id, IRubyObject paired_obj_id) {
        IRubyObject pair_list = ((RubyHash)list).fastARef(obj_id);
        if(pair_list == null) {
            return false;
        }
        if(paired_obj_id != null) {
            if(!(pair_list instanceof RubyHash)) {
                if(pair_list != paired_obj_id) {
                    return false;
                }
            } else {
                IRubyObject paired_result = ((RubyHash)pair_list).fastARef(paired_obj_id);
                if(paired_result == null || paired_result.isNil()) {
                    return false;
                }
            }
        }
        return true;
    }

    // exec_recursive_i
    private IRubyObject execRecursiveI(ExecRecursiveParams p) {
        IRubyObject result = null;
        recursivePush(p.list, p.objid, p.pairid);
        try {
            result = p.func.call(p.obj, false);
        } finally {
            recursivePop(p.list, p.objid, p.pairid);
        }
        return result;
    }

    // exec_recursive
    private IRubyObject execRecursiveInternal(RecursiveFunction func, IRubyObject obj, IRubyObject pairid, boolean outer) {
        ExecRecursiveParams p = new ExecRecursiveParams();
        p.list = recursiveListAccess();
        p.objid = obj.id();
        boolean outermost = outer && !recursiveCheck(p.list, recursiveKey, null);
        if(recursiveCheck(p.list, p.objid, pairid)) {
            if(outer && !outermost) {
                throw new RecursiveError(p.list);
            }
            return func.call(obj, true); 
        } else {
            IRubyObject result = null;
            p.func = func;
            p.obj = obj;
            p.pairid = pairid;

            if(outermost) {
                recursivePush(p.list, recursiveKey, null);
                try {
                    result = execRecursiveI(p);
                } catch(RecursiveError e) {
                    if(e.tag != p.list) {
                        throw e;
                    } else {
                        result = p.list;
                    }
                }
                recursivePop(p.list, recursiveKey, null);
                if(result == p.list) {
                    result = func.call(obj, true);
                }
            } else {
                result = execRecursiveI(p);
            }

            return result;
        }
    }

    /**
     * Perform a recursive walk on the given object using the given function.
     *
     * Do not call this method directly unless you know you're within a call
     * to {@link Ruby#recursiveListOperation(java.util.concurrent.Callable) recursiveListOperation},
     * which will ensure the thread-local recursion tracking data structs are
     * cleared.
     *
     * MRI: rb_exec_recursive
     *
     * Calls func(obj, arg, recursive), where recursive is non-zero if the
     * current method is called recursively on obj
     *
     * @param func
     * @param obj
     * @return
     */
    public IRubyObject execRecursive(RecursiveFunction func, IRubyObject obj) {
        if (!inRecursiveListOperation.get()) {
            throw newThreadError("BUG: execRecursive called outside recursiveListOperation");
        }
        return execRecursiveInternal(func, obj, null, false);
    }

    /**
     * Perform a recursive walk on the given object using the given function.
     * Treat this as the outermost call, cleaning up recursive structures.
     *
     * MRI: rb_exec_recursive_outer
     *
     * If recursion is detected on the current method and obj, the outermost
     * func will be called with (obj, arg, Qtrue). All inner func will be
     * short-circuited using throw.
     *
     * @param func
     * @param obj
     * @return
     */
    public IRubyObject execRecursiveOuter(RecursiveFunction func, IRubyObject obj) {
        try {
            return execRecursiveInternal(func, obj, null, true);
        } finally {
            recursiveListClear();
        }
    }

    /**
     * Begin a recursive walk that may make one or more calls to
     * {@link Ruby#execRecursive(org.jruby.Ruby.RecursiveFunction, org.jruby.runtime.builtin.IRubyObject) execRecursive}.
     * Clean up recursive structures once complete.
     *
     * @param body
     * @param <T>
     * @return
     */
    public <T extends IRubyObject> T recursiveListOperation(Callable<T> body) {
        try {
            inRecursiveListOperation.set(true);
            return body.call();
        } catch (Exception e) {
            UnsafeFactory.getUnsafe().throwException(e);
            return null; // not reached
        } finally {
            recursiveListClear();
            inRecursiveListOperation.set(false);
        }
    }

    public boolean isObjectSpaceEnabled() {
        return objectSpaceEnabled;
    }

    public void setObjectSpaceEnabled(boolean objectSpaceEnabled) {
        this.objectSpaceEnabled = objectSpaceEnabled;
    }

    public long getStartTime() {
        return startTime;
    }

    public Profile getProfile() {
        return profile;
    }

    public String getJRubyHome() {
        return config.getJRubyHome();
    }

    public void setJRubyHome(String home) {
        config.setJRubyHome(home);
    }

    public RubyInstanceConfig getInstanceConfig() {
        return config;
    }

    public boolean is1_9() {
        return is1_9;
    }

    public boolean is2_0() {
        return is2_0;
    }

    /** GET_VM_STATE_VERSION */
    public long getGlobalState() {
        synchronized(this) {
            return globalState;
        }
    }

    /** INC_VM_STATE_VERSION */
    public void incGlobalState() {
        synchronized(this) {
            globalState = (globalState+1) & 0x8fffffff;
        }
    }

    public static boolean isSecurityRestricted() {
        return securityRestricted;
    }
    
    public static void setSecurityRestricted(boolean restricted) {
        securityRestricted = restricted;
    }
    
    public POSIX getPosix() {
        return posix;
    }
    
    public void setRecordSeparatorVar(GlobalVariable recordSeparatorVar) {
        this.recordSeparatorVar = recordSeparatorVar;
    }
    
    public GlobalVariable getRecordSeparatorVar() {
        return recordSeparatorVar;
    }
    
    public Set<Script> getJittedMethods() {
        return jittedMethods;
    }
    
    public ExecutorService getExecutor() {
        return executor;
    }

    public Map<String, DateTimeZone> getTimezoneCache() {
        return timeZoneCache;
    }

    @Deprecated
    public int getConstantGeneration() {
        return -1;
    }

    @Deprecated
    public synchronized void incrementConstantGeneration() {
        constantInvalidator.invalidate();
    }
    
    public Invalidator getConstantInvalidator() {
        return constantInvalidator;
    }
    
    public void invalidateConstants() {
        
    }

    public <E extends Enum<E>> void loadConstantSet(RubyModule module, Class<E> enumClass) {
        for (E e : EnumSet.allOf(enumClass)) {
            Constant c = (Constant) e;
            if (Character.isUpperCase(c.name().charAt(0))) {
                module.setConstant(c.name(), newFixnum(c.intValue()));
            }
        }
    }
    public void loadConstantSet(RubyModule module, String constantSetName) {
        for (Constant c : ConstantSet.getConstantSet(constantSetName)) {
            if (Character.isUpperCase(c.name().charAt(0))) {
                module.setConstant(c.name(), newFixnum(c.intValue()));
            }
        }
    }

    /**
     * Get a new serial number for a new DynamicMethod instance
     * @return a new serial number
     */
    public long getNextDynamicMethodSerial() {
        return dynamicMethodSerial.getAndIncrement();
    }

    /**
     * Get a new generation number for a module or class.
     *
     * @return a new generation number
     */
    public int getNextModuleGeneration() {
        return moduleGeneration.incrementAndGet();
    }

    /**
     * Get the global object used to synchronize class-hierarchy modifications like
     * cache invalidation, subclass sets, and included hierarchy sets.
     *
     * @return The object to use for locking when modifying the hierarchy
     */
    public Object getHierarchyLock() {
        return hierarchyLock;
    }

    /**
     * Get the runtime-global selector pool
     *
     * @return a SelectorPool from which to get Selector instances
     */
    public SelectorPool getSelectorPool() {
        return selectorPool;
    }

    /**
     * Get the core class RuntimeCache instance, for doing dynamic calls from
     * core class methods.
     */
    public RuntimeCache getRuntimeCache() {
        return runtimeCache;
    }

    /**
     * Get the list of method names being profiled
     */
    public String[] getProfiledNames() {
        return profiledNames;
    }

    /**
     * Get the list of method objects for methods being profiled
     */
    public DynamicMethod[] getProfiledMethods() {
        return profiledMethods;
    }

    /**
     * Add a method and its name to the profiling arrays, so it can be printed out
     * later.
     *
     * @param name the name of the method
     * @param method
     */
    public synchronized void addProfiledMethod(String name, DynamicMethod method) {
        if (!config.isProfiling()) return;
        if (method.isUndefined()) return;
        if (method.getSerialNumber() > MAX_PROFILE_METHODS) return;

        int index = (int)method.getSerialNumber();
        if (profiledMethods.length <= index) {
            int newSize = Math.min((int)index * 2 + 1, MAX_PROFILE_METHODS);
            String[] newProfiledNames = new String[newSize];
            System.arraycopy(profiledNames, 0, newProfiledNames, 0, profiledNames.length);
            profiledNames = newProfiledNames;
            DynamicMethod[] newProfiledMethods = new DynamicMethod[newSize];
            System.arraycopy(profiledMethods, 0, newProfiledMethods, 0, profiledMethods.length);
            profiledMethods = newProfiledMethods;
        }

        // only add the first one we encounter, since others will probably share the original
        if (profiledNames[index] == null) {
            profiledNames[index] = name;
            profiledMethods[index] = method;
        }
    }
    
    /**
     * Increment the count of exceptions generated by code in this runtime.
     */
    public void incrementExceptionCount() {
        exceptionCount.incrementAndGet();
    }
    
    /**
     * Get the current exception count.
     * 
     * @return he current exception count
     */
    public int getExceptionCount() {
        return exceptionCount.get();
    }
    
    /**
     * Increment the count of backtraces generated by code in this runtime.
     */
    public void incrementBacktraceCount() {
        backtraceCount.incrementAndGet();
    }
    
    /**
     * Get the current backtrace count.
     * 
     * @return the current backtrace count
     */
    public int getBacktraceCount() {
        return backtraceCount.get();
    }
    
    /**
     * Increment the count of backtraces generated by code in this runtime.
     */
    public void incrementCallerCount() {
        callerCount.incrementAndGet();
    }
    
    /**
     * Get the current backtrace count.
     * 
     * @return the current backtrace count
     */
    public int getCallerCount() {
        return callerCount.get();
    }
    
    /**
     * Whether the Fixnum class has been reopened and modified
     */
    public boolean isFixnumReopened() {
        return fixnumReopened;
    }
    
    /**
     * Set whether the Fixnum class has been reopened and modified
     */
    public void setFixnumReopened(boolean fixnumReopened) {
        this.fixnumReopened = fixnumReopened;
    }
    
    /**
     * Whether the Float class has been reopened and modified
     */
    public boolean isFloatReopened() {
        return floatReopened;
    }
    
    /**
     * Set whether the Float class has been reopened and modified
     */
    public void setFloatReopened(boolean floatReopened) {
        this.floatReopened = floatReopened;
    }
    
    public boolean isBooting() {
        return booting;
    }
    
    public CoverageData getCoverageData() {
        return coverageData;
    }
    
    public Random getRandom() {
        return random;
    }
    
    public int getHashSeed() {
        return hashSeed;
    }
    
    public StaticScopeFactory getStaticScopeFactory() {
        return staticScopeFactory;
    }

    public FFI getFFI() {
        return ffi;
    }

    public void setFFI(FFI ffi) {
        this.ffi = ffi;
    }

    private final Invalidator constantInvalidator;
    private final ThreadService threadService;
    
    private POSIX posix;

    private final ObjectSpace objectSpace = new ObjectSpace();

    private final RubySymbol.SymbolTable symbolTable = new RubySymbol.SymbolTable(this);

    private final List<EventHook> eventHooks = new Vector<EventHook>();
    private boolean hasEventHooks;  
    private boolean globalAbortOnExceptionEnabled = false;
    private boolean doNotReverseLookupEnabled = false;
    private volatile boolean objectSpaceEnabled;
    
    private final Set<Script> jittedMethods = Collections.synchronizedSet(new WeakHashSet<Script>());
    
    private long globalState = 1;
    
    private int safeLevel = -1;

    // Default objects
    private IRubyObject topSelf;
    private IRubyObject rootFiber;
    private RubyNil nilObject;
    private IRubyObject[] singleNilArray;
    private RubyBoolean trueObject;
    private RubyBoolean falseObject;
    public final RubyFixnum[] fixnumCache = new RubyFixnum[2 * RubyFixnum.CACHE_OFFSET];

    private boolean verbose, warningsEnabled, debug;
    private IRubyObject verboseValue;
    
    private RubyThreadGroup defaultThreadGroup;

    /**
     * All the core classes we keep hard references to. These are here largely
     * so that if someone redefines String or Array we won't start blowing up
     * creating strings and arrays internally. They also provide much faster
     * access than going through normal hash lookup on the Object class.
     */
    private RubyClass
           basicObjectClass, objectClass, moduleClass, classClass, nilClass, trueClass,
            falseClass, numericClass, floatClass, integerClass, fixnumClass,
            complexClass, rationalClass, enumeratorClass, yielderClass,
            arrayClass, hashClass, rangeClass, stringClass, encodingClass, converterClass, symbolClass,
            procClass, bindingClass, methodClass, unboundMethodClass,
            matchDataClass, regexpClass, timeClass, bignumClass, dirClass,
            fileClass, fileStatClass, ioClass, threadClass, threadGroupClass,
            continuationClass, structClass, tmsStruct, passwdStruct,
            groupStruct, procStatusClass, exceptionClass, runtimeError, ioError,
            scriptError, nameError, nameErrorMessage, noMethodError, signalException,
            rangeError, dummyClass, systemExit, localJumpError, nativeException,
            systemCallError, fatal, interrupt, typeError, argumentError, indexError, stopIteration,
            syntaxError, standardError, loadError, notImplementedError, securityError, noMemoryError,
            regexpError, eofError, threadError, concurrencyError, systemStackError, zeroDivisionError, floatDomainError, mathDomainError,
            encodingError, encodingCompatibilityError, converterNotFoundError, undefinedConversionError,
            invalidByteSequenceError, fiberError, randomClass, keyError;

    /**
     * All the core modules we keep direct references to, for quick access and
     * to ensure they remain available.
     */
    private RubyModule
            kernelModule, comparableModule, enumerableModule, mathModule,
            marshalModule, etcModule, fileTestModule, gcModule,
            objectSpaceModule, processModule, procUIDModule, procGIDModule,
            procSysModule, precisionModule, errnoModule;

    private DynamicMethod privateMethodMissing, protectedMethodMissing, variableMethodMissing,
            superMethodMissing, normalMethodMissing, defaultMethodMissing;
    
    // record separator var, to speed up io ops that use it
    private GlobalVariable recordSeparatorVar;

    // former java.lang.System concepts now internalized for MVM
    private volatile String currentDirectory;

    // The "current line" global variable
    private volatile int currentLine = 0;

    private volatile IRubyObject argsFile;

    private final long startTime = System.currentTimeMillis();

    private final RubyInstanceConfig config;
    private final boolean is1_9;
    private final boolean is2_0;

    private final InputStream in;
    private final PrintStream out;
    private final PrintStream err;

    // Java support
    private JavaSupport javaSupport;
    private JRubyClassLoader jrubyClassLoader;
    
    // Management/monitoring
    private BeanManager beanManager;

    // Parser stats
    private ParserStats parserStats;
    
    // Compilation
    private final JITCompiler jitCompiler;

    // Note: this field and the following static initializer
    // must be located be in this order!
    private volatile static boolean securityRestricted = false;
    static {
        if (SafePropertyAccessor.isSecurityProtected("jruby.reflected.handles")) {
            // can't read non-standard properties
            securityRestricted = true;
        } else {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                try {
                    sm.checkCreateClassLoader();
                } catch (SecurityException se) {
                    // can't create custom classloaders
                    securityRestricted = true;
                }
            }
        }
    }

    private final Parser parser = new Parser(this);

    private LoadService loadService;

    private Encoding defaultInternalEncoding, defaultExternalEncoding;
    private EncodingService encodingService;

    private GlobalVariables globalVariables = new GlobalVariables(this);
    private final RubyWarnings warnings = new RubyWarnings(this);

    // Contains a list of all blocks (as Procs) that should be called when
    // the runtime environment exits.
    private final Stack<RubyProc> atExitBlocks = new Stack<RubyProc>();

    private Profile profile;

    private KCode kcode = KCode.NONE;

    // Atomic integers for symbol and method IDs
    private final AtomicInteger symbolLastId = new AtomicInteger(128);
    private final AtomicInteger moduleLastId = new AtomicInteger(0);

    // Weak map of all Modules in the system (and by extension, all Classes
    private final Set<RubyModule> allModules = new WeakHashSet<RubyModule>();

    private Object respondToMethod;

    private final Map<String, DateTimeZone> timeZoneCache = new HashMap<String,DateTimeZone>();
    /**
     * A list of "external" finalizers (the ones, registered via ObjectSpace),
     * weakly referenced, to be executed on tearDown.
     */
    private Map<Finalizable, Object> finalizers;
    
    /**
     * A list of JRuby-internal finalizers,  weakly referenced,
     * to be executed on tearDown.
     */
    private Map<Finalizable, Object> internalFinalizers;

    // mutex that controls modifications of user-defined finalizers
    private final Object finalizersMutex = new Object();

    // mutex that controls modifications of internal finalizers
    private final Object internalFinalizersMutex = new Object();
    
    // A thread pool to use for executing this runtime's Ruby threads
    private ExecutorService executor;

    // A global object lock for class hierarchy mutations
    private final Object hierarchyLock = new Object();

    // An atomic long for generating DynamicMethod serial numbers
    private final AtomicLong dynamicMethodSerial = new AtomicLong(1);

    // An atomic int for generating class generation numbers
    private final AtomicInteger moduleGeneration = new AtomicInteger(1);

    // A list of Java class+method names to include in backtraces
    private final Map<String, Map<String, String>> boundMethods = new HashMap();

    // A soft pool of selectors for blocking IO operations
    private final SelectorPool selectorPool = new SelectorPool();

    // A global cache for Java-to-Ruby calls
    private final RuntimeCache runtimeCache;

    // The maximum number of methods we will track for profiling purposes
    private static final int MAX_PROFILE_METHODS = 100000;

    // The list of method names associated with method serial numbers
    public String[] profiledNames = new String[0];

    // The method objects for serial numbers
    public DynamicMethod[] profiledMethods = new DynamicMethod[0];
    
    // Message for Errno exceptions that will not generate a backtrace
    public static final String ERRNO_BACKTRACE_MESSAGE = "errno backtraces disabled; run with -Xerrno.backtrace=true to enable";
    
    // Count of RaiseExceptions generated by code running in this runtime
    private final AtomicInteger exceptionCount = new AtomicInteger();
    
    // Count of exception backtraces generated by code running in this runtime
    private final AtomicInteger backtraceCount = new AtomicInteger();
    
    // Count of Kernel#caller backtraces generated by code running in this runtime
    private final AtomicInteger callerCount = new AtomicInteger();
    
    private boolean fixnumReopened, floatReopened;
    
    private volatile boolean booting = true;
    
    private RubyHash envObject;
    
    private final CoverageData coverageData = new CoverageData();

    /** The "global" runtime. Set to the first runtime created, normally. */
    private static Ruby globalRuntime;
    
    /** The "thread local" runtime. Set to the global runtime if unset. */
    private static ThreadLocal<Ruby> threadLocalRuntime = new ThreadLocal<Ruby>();
    
    /** The runtime-local random number generator. Uses SecureRandom if permissions allow. */
    private final Random random;

    /** The runtime-local seed for hash randomization */
    private int hashSeed;
    
    private final StaticScopeFactory staticScopeFactory;
    
    private IRManager irManager;

    // structures and such for recursive operations
    private ThreadLocal<Map<String, RubyHash>> recursive = new ThreadLocal<Map<String, RubyHash>>();
    private RubySymbol recursiveKey;
    private ThreadLocal<Boolean> inRecursiveListOperation = new ThreadLocal<Boolean>();

    private FFI ffi;
}
