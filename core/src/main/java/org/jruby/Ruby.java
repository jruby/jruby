/*
 **** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby;

import org.jcodings.specific.UTF8Encoding;
import org.jruby.anno.TypePopulator;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.BlockNode;
import org.jruby.ast.CallNode;
import org.jruby.ast.FCallNode;
import org.jruby.ast.GlobalAsgnNode;
import org.jruby.ast.GlobalVarNode;
import org.jruby.ast.StrNode;
import org.jruby.ast.VCallNode;
import org.jruby.ast.WhileNode;
import org.jruby.compiler.Constantizable;
import org.jruby.compiler.NotCompilableException;
import org.jruby.ext.thread.ConditionVariable;
import org.jruby.ext.thread.Mutex;
import org.jruby.ext.thread.SizedQueue;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScriptBody;
import org.jruby.ir.instructions.Instr;
import org.jruby.javasupport.JavaSupport;
import org.jruby.javasupport.JavaSupportImpl;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.management.Caches;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.invokedynamic.InvokeDynamicSupport;
import org.jruby.util.CommonByteLists;
import org.jruby.util.MRIRecursionGuard;
import org.jruby.util.StringSupport;
import org.jruby.util.StrptimeParser;
import org.jruby.util.StrptimeToken;
import org.jruby.util.io.EncodingUtils;
import org.objectweb.asm.util.TraceClassVisitor;

import jnr.constants.Constant;
import jnr.constants.ConstantSet;
import jnr.constants.platform.Errno;
import jnr.posix.POSIX;
import jnr.posix.POSIXFactory;

import org.jcodings.Encoding;
import org.joda.time.DateTimeZone;
import org.joni.WarnCallback;
import org.jruby.ast.Node;
import org.jruby.ast.RootNode;
import org.jruby.ast.executable.RuntimeCache;
import org.jruby.ast.executable.Script;
import org.jruby.ast.executable.ScriptAndCode;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.common.RubyWarnings;
import org.jruby.compiler.JITCompiler;
import org.jruby.embed.Extension;
import org.jruby.exceptions.MainExitException;
import org.jruby.exceptions.RaiseException;
import org.jruby.ext.JRubyPOSIXHandler;
import org.jruby.ext.LateLoadingLibrary;
import org.jruby.ext.coverage.CoverageData;
import org.jruby.ext.ffi.FFI;
import org.jruby.ext.fiber.ThreadFiber;
import org.jruby.ext.fiber.ThreadFiberLibrary;
import org.jruby.ext.tracepoint.TracePoint;
import org.jruby.internal.runtime.GlobalVariables;
import org.jruby.internal.runtime.ThreadService;
import org.jruby.internal.runtime.ValueAccessor;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.ir.Compiler;
import org.jruby.ir.IRManager;
import org.jruby.ir.interpreter.Interpreter;
import org.jruby.ir.persistence.IRReader;
import org.jruby.ir.persistence.IRReaderStream;
import org.jruby.ir.persistence.util.IRFileExpert;
import org.jruby.javasupport.proxy.JavaProxyClassFactory;
import org.jruby.management.BeanManager;
import org.jruby.management.BeanManagerFactory;
import org.jruby.management.Config;
import org.jruby.management.ParserStats;
import org.jruby.parser.Parser;
import org.jruby.parser.ParserConfiguration;
import org.jruby.parser.StaticScopeFactory;
import org.jruby.platform.Platform;
import org.jruby.runtime.Binding;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.EventHook;
import org.jruby.runtime.GlobalVariable;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.IAccessor;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ObjectSpace;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.encoding.EncodingService;
import org.jruby.runtime.invokedynamic.MethodNames;
import org.jruby.runtime.load.BasicLibraryService;
import org.jruby.runtime.load.CompiledScriptLoader;
import org.jruby.runtime.load.Library;
import org.jruby.runtime.load.LoadService;
import org.jruby.runtime.opto.Invalidator;
import org.jruby.runtime.opto.OptoFactory;
import org.jruby.runtime.profile.ProfileCollection;
import org.jruby.runtime.profile.ProfilingService;
import org.jruby.runtime.profile.ProfilingServiceLookup;
import org.jruby.runtime.profile.builtin.ProfiledMethods;
import org.jruby.runtime.scope.ManyVarsDynamicScope;
import org.jruby.threading.DaemonThreadFactory;
import org.jruby.util.ByteList;
import org.jruby.util.ClassDefiningClassLoader;
import org.jruby.util.DefinedMessage;
import org.jruby.util.JRubyClassLoader;
import org.jruby.util.SecurityHelper;
import org.jruby.util.SelfFirstJRubyClassLoader;
import org.jruby.util.IOInputStream;
import org.jruby.util.IOOutputStream;
import org.jruby.util.ClassDefiningJRubyClassLoader;
import org.jruby.util.KCode;
import org.jruby.util.SafePropertyAccessor;
import org.jruby.util.cli.Options;
import org.jruby.util.collections.WeakHashSet;
import org.jruby.util.func.Function1;
import org.jruby.util.io.FilenoUtil;
import org.jruby.util.io.SelectorPool;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import org.objectweb.asm.ClassReader;

import java.io.ByteArrayInputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandle;
import java.lang.ref.WeakReference;
import java.net.BindException;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static java.lang.invoke.MethodHandles.explicitCastArguments;
import static java.lang.invoke.MethodHandles.insertArguments;
import static java.lang.invoke.MethodType.methodType;
import static org.jruby.internal.runtime.GlobalVariable.Scope.GLOBAL;
import static org.jruby.util.RubyStringBuilder.str;
import static org.jruby.util.RubyStringBuilder.ids;
import static org.jruby.util.RubyStringBuilder.types;

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
public final class Ruby implements Constantizable {

    /**
     * The logger used to log relevant bits.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Ruby.class);
    static { // enable DEBUG output
        if (RubyInstanceConfig.JIT_LOADING_DEBUG) LOG.setDebugEnable(true);
    }

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
        this.threadService      = new ThreadService(this);

        if( config.isProfiling() ) {
            this.profilingServiceLookup = new ProfilingServiceLookup(this);
        } else {
            this.profilingServiceLookup = null;
        }

        constant = OptoFactory.newConstantWrapper(Ruby.class, this);

        // force JRubyClassLoader to init if possible
        if (!Ruby.isSecurityRestricted()) {
            if (config.isClassloaderDelegate()){
                jrubyClassLoader = new JRubyClassLoader(config.getLoader());
            }
            else {
                jrubyClassLoader = new SelfFirstJRubyClassLoader(config.getLoader());
            }
        }
        else {
            jrubyClassLoader = null; // a NullClassLoader object would be better ...
        }

        this.staticScopeFactory = new StaticScopeFactory(this);
        this.beanManager        = BeanManagerFactory.create(this, config.isManagementEnabled());
        this.jitCompiler        = new JITCompiler(this);
        this.parserStats        = new ParserStats(this);
        this.caches             = new Caches();

        Random myRandom;
        try {
            myRandom = new SecureRandom();
        } catch (Throwable t) {
            LOG.debug("unable to instantiate SecureRandom, falling back on Random", t);
            myRandom = new Random();
        }
        this.random = myRandom;

        if (RubyInstanceConfig.CONSISTENT_HASHING_ENABLED) {
            this.hashSeedK0 = -561135208506705104l;
            this.hashSeedK1 = 7114160726623585955l;
        } else {
            this.hashSeedK0 = this.random.nextLong();
            this.hashSeedK1 = this.random.nextLong();
        }

        this.configBean = new Config(this);
        this.runtimeBean = new org.jruby.management.Runtime(this);

        registerMBeans();

        this.runtimeCache = new RuntimeCache();
        runtimeCache.initMethodCache(ClassIndex.MAX_CLASSES.ordinal() * MethodNames.values().length - 1);

        checkpointInvalidator = OptoFactory.newConstantInvalidator(this);

        if (config.isObjectSpaceEnabled()) {
            objectSpacer = ENABLED_OBJECTSPACE;
        } else {
            objectSpacer = DISABLED_OBJECTSPACE;
        }

        posix = POSIXFactory.getPOSIX(new JRubyPOSIXHandler(this), config.isNativeEnabled());
        filenoUtil = new FilenoUtil(posix);

        reinitialize(false);
    }

    public void registerMBeans() {
        this.beanManager.register(jitCompiler);
        this.beanManager.register(configBean);
        this.beanManager.register(parserStats);
        this.beanManager.register(runtimeBean);
        this.beanManager.register(caches);
    }

    void reinitialize(boolean reinitCore) {
        this.doNotReverseLookupEnabled = true;
        this.staticScopeFactory = new StaticScopeFactory(this);
        this.in                 = config.getInput();
        this.out                = config.getOutput();
        this.err                = config.getError();
        this.objectSpaceEnabled = config.isObjectSpaceEnabled();
        this.siphashEnabled     = config.isSiphashEnabled();
        this.profile            = config.getProfile();
        this.currentDirectory   = config.getCurrentDirectory();
        this.kcode              = config.getKCode();

        if (reinitCore) {
            RubyGlobal.initARGV(this);
            RubyGlobal.initSTDIO(this, globalVariables);
        }
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
            globalRuntime = this;
        }
    }

    /**
     * Clear the global runtime.
     */
    public static void clearGlobalRuntime() {
        globalRuntime = null;
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
        Node rootNode = parseEval(script, "<script>", scope, 0);

        context.preEvalScriptlet(scope);

        try {
            return interpreter.execute(this, rootNode, context.getFrameSelf());
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
        byte[] bytes = encodeToBytes(script);

        RootNode root = (RootNode) parseInline(new ByteArrayInputStream(bytes), filename, null);
        ThreadContext context = getCurrentContext();

        String oldFile = context.getFile();
        int oldLine = context.getLine();
        try {
            context.setFileAndLine(root.getFile(), root.getLine());
            return runInterpreter(root);
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
        getGlobalVariables().define("$PROGRAM_NAME", d, GLOBAL);
        getGlobalVariables().define("$0", d, GLOBAL);

        for (Map.Entry<String, String> entry : config.getOptionGlobals().entrySet()) {
            final IRubyObject varvalue;
            if (entry.getValue() != null) {
                varvalue = newString(entry.getValue());
            } else {
                varvalue = getTrue();
            }
            getGlobalVariables().set('$' + entry.getKey(), varvalue);
        }

        if (filename.endsWith(".class")) {
            // we are presumably running a precompiled class; load directly
            IRScope script = CompiledScriptLoader.loadScriptFromFile(this, inputStream, null, filename, false);
            if (script == null) {
                throw new MainExitException(1, "error: .class file specified is not a compiled JRuby script");
            }
            script.setFileName(filename);
            runInterpreter(script);
            return;
        }

        ParseResult parseResult = parseFromMain(filename, inputStream);

        // if no DATA, we're done with the stream, shut it down
        if (fetchGlobalConstant("DATA") == null) {
            try {inputStream.close();} catch (IOException ioe) {}
        }

        if (parseResult instanceof RootNode) {
            RootNode scriptNode = (RootNode) parseResult;

            ThreadContext context = getCurrentContext();

            String oldFile = context.getFile();
            int oldLine = context.getLine();
            try {
                context.setFileAndLine(scriptNode.getFile(), scriptNode.getLine());

                if (config.isAssumePrinting() || config.isAssumeLoop()) {
                    runWithGetsLoop(scriptNode, config.isAssumePrinting(), config.isProcessLineEnds(),
                            config.isSplit());
                } else {
                    runNormally(scriptNode);
                }
            } finally {
                context.setFileAndLine(oldFile, oldLine);
            }
        } else {
            // TODO: Only interpreter supported so far
            runInterpreter(parseResult);
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

    public ParseResult parseFromMain(String fileName, InputStream in) {
        if (config.isInlineScript()) return parseInline(in, fileName, getCurrentContext().getCurrentScope());

        return parseFileFromMain(fileName, in, getCurrentContext().getCurrentScope());
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
        return runWithGetsLoop((RootNode) scriptNode, printing, processLineEnds, split);
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
    public IRubyObject runWithGetsLoop(RootNode scriptNode, boolean printing, boolean processLineEnds, boolean split) {
        ThreadContext context = getCurrentContext();

        // We do not want special scope types in IR so we amend the AST tree to contain the elements representing
        // a while gets; ...your code...; end
        scriptNode = addGetsLoop(scriptNode, printing, processLineEnds, split);

        Script script = null;
        boolean compile = getInstanceConfig().getCompileMode().shouldPrecompileCLI();
        if (compile) {
            try {
                script = tryCompile(scriptNode);
                if (Options.JIT_LOGGING.load()) {
                    LOG.info("Successfully compiled: {}", scriptNode.getFile());
                }
            } catch (Throwable e) {
                if (Options.JIT_LOGGING.load()) {
                    if (Options.JIT_LOGGING_VERBOSE.load()) {
                        LOG.error("Failed to compile: " + scriptNode.getFile(), e);
                    }
                    else {
                        LOG.error("Failed to compile: " + scriptNode.getFile());
                    }
                }
            }
            if (compile && script == null) {
                // IR JIT does not handle all scripts yet, so let those that fail run in interpreter instead
                // FIXME: restore error once JIT should handle everything
            }
        }

        // we do pre and post load outside the "body" versions to pre-prepare
        // and pre-push the dynamic scope we need for lastline
        Helpers.preLoad(context, ((RootNode) scriptNode).getStaticScope().getVariables());

        try {
            if (script != null) {
                runScriptBody(script);
            } else {
                runInterpreterBody(scriptNode);
            }

        } finally {
            Helpers.postLoad(context);
        }

        return getNil();
    }

    // Modifies incoming source for -n, -p, and -F
    private RootNode addGetsLoop(RootNode oldRoot, boolean printing, boolean processLineEndings, boolean split) {
        ISourcePosition pos = oldRoot.getPosition();
        BlockNode newBody = new BlockNode(pos);
        RubySymbol dollarSlash = newSymbol(CommonByteLists.DOLLAR_SLASH);
        newBody.add(new GlobalAsgnNode(pos, dollarSlash, new StrNode(pos, ((RubyString) globalVariables.get("$/")).getByteList())));

        if (processLineEndings) newBody.add(new GlobalAsgnNode(pos, newSymbol(CommonByteLists.DOLLAR_BACKSLASH), new GlobalVarNode(pos, dollarSlash)));

        GlobalVarNode dollarUnderscore = new GlobalVarNode(pos, newSymbol("$_"));

        BlockNode whileBody = new BlockNode(pos);
        newBody.add(new WhileNode(pos, new VCallNode(pos, newSymbol("gets")), whileBody));

        if (processLineEndings) whileBody.add(new CallNode(pos, dollarUnderscore, newSymbol("chomp!"), null, null, false));
        if (split) whileBody.add(new GlobalAsgnNode(pos, newSymbol("$F"), new CallNode(pos, dollarUnderscore, newSymbol("split"), null, null, false)));

        if (oldRoot.getBodyNode() instanceof BlockNode) {   // common case n stmts
            whileBody.addAll(((BlockNode) oldRoot.getBodyNode()));
        } else {                                            // single expr script
            whileBody.add(oldRoot.getBodyNode());
        }

        if (printing) whileBody.add(new FCallNode(pos, newSymbol("puts"), new ArrayNode(pos, dollarUnderscore), null));

        return new RootNode(pos, oldRoot.getScope(), newBody, oldRoot.getFile());
    }

    /**
     * Run the specified script without any of the loop-processing wrapper
     * code.
     *
     * @param scriptNode The root node of the script to be executed
     * bytecode before execution
 *     @param wrap whether to wrap the execution in an anonymous module
     * @return The result of executing the script
     */
    public IRubyObject runNormally(Node scriptNode, boolean wrap) {
        ScriptAndCode scriptAndCode = null;
        boolean compile = getInstanceConfig().getCompileMode().shouldPrecompileCLI();
        if (compile || config.isShowBytecode()) {
            scriptAndCode = precompileCLI((RootNode) scriptNode);
        }

        if (scriptAndCode != null) {
            if (config.isShowBytecode()) {
                TraceClassVisitor tracer = new TraceClassVisitor(new PrintWriter(System.err));
                ClassReader reader = new ClassReader(scriptAndCode.bytecode());
                reader.accept(tracer, 0);
                return getNil();
            }

            return runScript(scriptAndCode.script(), wrap);
        } else {
            // FIXME: temporarily allowing JIT to fail for $0 and fall back on interpreter
//            failForcedCompile(scriptNode);

            return runInterpreter(scriptNode);
        }
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
        return runNormally(scriptNode, false);
    }

    private ScriptAndCode precompileCLI(RootNode scriptNode) {
        ScriptAndCode scriptAndCode = null;

        // IR JIT does not handle all scripts yet, so let those that fail run in interpreter instead
        // FIXME: restore error once JIT should handle everything
        try {
            scriptAndCode = tryCompile(scriptNode, new ClassDefiningJRubyClassLoader(getJRubyClassLoader()));
            if (scriptAndCode != null && Options.JIT_LOGGING.load()) {
                LOG.info("done compiling target script: " + scriptNode.getFile());
            }
        } catch (Exception e) {
            if (Options.JIT_LOGGING.load()) {
                LOG.error("failed to compile target script '" + scriptNode.getFile() + "'");
                if (Options.JIT_LOGGING_VERBOSE.load()) {
                    e.printStackTrace();
                }
            }
        }
        return scriptAndCode;
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
        return tryCompile((RootNode) node, new ClassDefiningJRubyClassLoader(getJRubyClassLoader())).script();
    }

    private void failForcedCompile(RootNode scriptNode) throws RaiseException {
        if (config.getCompileMode().shouldPrecompileAll()) {
            throw newRuntimeError("could not compile and compile mode is 'force': " + scriptNode.getFile());
        }
    }

    private ScriptAndCode tryCompile(RootNode root, ClassDefiningClassLoader classLoader) {
        try {
            return Compiler.getInstance().execute(this, root, classLoader);
        } catch (NotCompilableException e) {
            if (Options.JIT_LOGGING.load()) {
                if (Options.JIT_LOGGING_VERBOSE.load()) {
                    LOG.error("failed to compile target script " + root.getFile() + ": ", e);
                }
                else {
                    LOG.error("failed to compile target script " + root.getFile() + ": " + e.getLocalizedMessage());
                }
            }
            return null;
        }
    }

    public IRubyObject runScript(Script script) {
        return runScript(script, false);
    }

    public IRubyObject runScript(Script script, boolean wrap) {
        return script.load(getCurrentContext(), getTopSelf(), wrap);
    }

    /**
     * This is used for the "gets" loop, and we bypass 'load' to use an
     * already-prepared, already-pushed scope for the script body.
     */
    public IRubyObject runScriptBody(Script script) {
        return script.__file__(getCurrentContext(), getTopSelf(), Block.NULL_BLOCK);
    }

    public IRubyObject runInterpreter(ThreadContext context, ParseResult parseResult, IRubyObject self) {
        return interpreter.execute(this, parseResult, self);
   }

    public IRubyObject runInterpreter(ThreadContext context,  Node rootNode, IRubyObject self) {
        assert rootNode != null : "scriptNode is not null";
        return interpreter.execute(this, rootNode, self);
    }

    public IRubyObject runInterpreter(Node scriptNode) {
        return runInterpreter(getCurrentContext(), scriptNode, getTopSelf());
    }

    public IRubyObject runInterpreter(ParseResult parseResult) {
        return runInterpreter(getCurrentContext(), parseResult, getTopSelf());
    }

    /**
     * This is used for the "gets" loop, and we bypass 'load' to use an
     * already-prepared, already-pushed scope for the script body.
     */
    public IRubyObject runInterpreterBody(Node scriptNode) {
        assert scriptNode != null : "scriptNode is not null";
        assert scriptNode instanceof RootNode : "scriptNode is not a RootNode";

        return runInterpreter(scriptNode);
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
     * Get the Caches management object.
     *
     * @return the current runtime's Caches management object
     */
    public Caches getCaches() {
        return caches;
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
    @Extension
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
    @Extension
    public RubyClass defineClassUnder(String name, RubyClass superClass, ObjectAllocator allocator, RubyModule parent) {
        return defineClassUnder(name, superClass, allocator, parent, null);
    }

    /**
     * A variation of defineClassUnder that allows passing in an array of
     * supplementary call sites to improve dynamic invocation.
     *
     * @param id The name for the new class as an ISO-8859_1 String (id-value)
     * @param superClass The super class for the new class
     * @param allocator An ObjectAllocator instance that can construct
     * instances of the new class.
     * @param parent The namespace under which to define the new class
     * @param callSites The array of call sites to add
     * @return The new class
     */
    public RubyClass defineClassUnder(String id, RubyClass superClass, ObjectAllocator allocator, RubyModule parent, CallSite[] callSites) {
        IRubyObject classObj = parent.getConstantAt(id);

        if (classObj != null) {
            if (!(classObj instanceof RubyClass)) throw newTypeError(str(this, ids(this, id), " is not a class"));
            RubyClass klazz = (RubyClass)classObj;
            if (klazz.getSuperClass().getRealClass() != superClass) {
                throw newNameError(str(this, ids(this, id), " is already defined"), id);
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
            IRubyObject className = parentIsObject ? ids(this, id) :
                    parent.toRubyString(getCurrentContext()).append(newString("::")).append(ids(this, id));
            warnings.warn(ID.NO_SUPER_CLASS, str(this, "no super class for `", className, "', Object assumed"));

            superClass = objectClass;
        }

        return RubyClass.newClass(this, superClass, id, allocator, parent, !parentIsObject, callSites);
    }

    /**
     * Define a new module under the Object namespace. Roughly equivalent to
     * rb_define_module in MRI.
     *
     * @param name The name of the new module
     * @returns The new module
     */
    @Extension
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
    @Extension
    public RubyModule defineModuleUnder(String name, RubyModule parent) {
        IRubyObject moduleObj = parent.getConstantAt(name);

        boolean parentIsObject = parent == objectClass;

        if (moduleObj != null ) {
            if (moduleObj.isModule()) return (RubyModule)moduleObj;

            RubyString typeName = parentIsObject ?
                    types(this, moduleObj.getMetaClass()) : types(this, parent, moduleObj.getMetaClass());

            throw newTypeError(str(this, typeName, " is not a module"));
        }

        return RubyModule.newModule(this, name, parent, !parentIsObject);
    }

    /**
     * From Object, retrieve the named module. If it doesn't exist a
     * new module is created.
     *
     * @param id The name of the module
     * @returns The existing or new module
     */
    public RubyModule getOrCreateModule(String id) {
        IRubyObject module = objectClass.getConstantAt(id);
        if (module == null) {
            module = defineModule(id);
        } else if (!module.isModule()) {
            throw newTypeError(str(this, ids(this, id), " is not a Module"));
        }

        return (RubyModule) module;
    }

    public KCode getKCode() {
        return kcode;
    }

    public void setKCode(KCode kcode) {
        this.kcode = kcode;
    }

    /** rb_define_global_const
     * Define a constant on the global namespace (i.e. Object) with the given
     * name and value.
     *
     * @param name the name
     * @param value the value
     */
    public void defineGlobalConstant(String name, IRubyObject value) {
        objectClass.defineConstant(name, value);
    }

    /**
     * Fetch a constant from the global namespace (i.e. Object) with the given
     * name.
     *
     * @param name the name
     * @return the value
     */
    public IRubyObject fetchGlobalConstant(String name) {
        return objectClass.fetchConstant(name, false);
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
        // Construct key services
        loadService = config.createLoadService(this);
        javaSupport = loadJavaSupport();

        executor = new ThreadPoolExecutor(
                RubyInstanceConfig.POOL_MIN,
                RubyInstanceConfig.POOL_MAX,
                RubyInstanceConfig.POOL_TTL,
                TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(),
                new DaemonThreadFactory("Ruby-" + getRuntimeNumber() + "-Worker"));

        fiberExecutor = new ThreadPoolExecutor(
                0,
                Integer.MAX_VALUE,
                RubyInstanceConfig.FIBER_POOL_TTL,
                TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(),
                new DaemonThreadFactory("Ruby-" + getRuntimeNumber() + "-Fiber"));

        // initialize the root of the class hierarchy completely
        initRoot();

        // Set up the main thread in thread service
        threadService.initMainThread();

        // Get the main threadcontext (gets constructed for us)
        final ThreadContext context = getCurrentContext();

        // Construct the top-level execution frame and scope for the main thread
        context.prepareTopLevel(objectClass, topSelf);

        // Initialize all the core classes
        bootstrap();

        // set up defined messages
        initDefinedMessages();

        // set up thread statuses
        initThreadStatuses();

        // Create an IR manager and a top-level IR scope and bind it to the top-level static-scope object
        irManager = new IRManager(this, getInstanceConfig());
        // FIXME: This registers itself into static scope as a side-effect.  Let's make this
        // relationship handled either more directly or through a descriptice method
        // FIXME: We need a failing test case for this since removing it did not regress tests
        IRScope top = new IRScriptBody(irManager, newSymbol(""), context.getCurrentScope().getStaticScope());
        top.allocateInterpreterContext(new ArrayList<Instr>());

        // Initialize the "dummy" class used as a marker
        dummyClass = new RubyClass(this, classClass);
        dummyClass.freeze(context);

        // Create global constants and variables
        RubyGlobal.createGlobals(context, this);

        // Prepare LoadService and load path
        getLoadService().init(config.getLoadPaths());

        // initialize builtin libraries
        initBuiltins();

        // load JRuby internals, which loads Java support
        // if we can't use reflection, 'jruby' and 'java' won't work; no load.
        boolean reflectionWorks = doesReflectionWork();

        if (!RubyInstanceConfig.DEBUG_PARSER && reflectionWorks) {
            loadService.require("jruby");
        }

        SecurityHelper.checkCryptoRestrictions(this);

        // out of base boot mode
        bootingCore = false;

        // init Ruby-based kernel
        initRubyKernel();

        // Define blank modules for feature detection in preludes
        if (!config.isDisableGems()) {
            defineModule("Gem");
        }
        if (!config.isDisableDidYouMean()) {
            defineModule("DidYouMean");
        }

        initRubyPreludes();

        // everything booted, so SizedQueue should be available; set up root fiber
        ThreadFiber.initRootFiber(context);

        if(config.isProfiling()) {
            // additional twiddling for profiled mode
            getLoadService().require("jruby/profiler/shutdown_hook");

            // recache core methods, since they'll have profiling wrappers now
            kernelModule.invalidateCacheDescendants(); // to avoid already-cached methods
            RubyKernel.recacheBuiltinMethods(this);
            RubyBasicObject.recacheBuiltinMethods(this);
        }

        if (config.getLoadGemfile()) {
            loadBundler();
        }

        deprecatedNetworkStackProperty();

        // Done booting JRuby runtime
        bootingRuntime = false;

        // Require in all libraries specified on command line
        for (String scriptName : config.getRequiredLibraries()) {
            topSelf.callMethod(context, "require", RubyString.newString(this, scriptName));
        }
    }

    public JavaSupport loadJavaSupport() {
        return new JavaSupportImpl(this);
    }

    private void loadBundler() {
        loadService.loadFromClassLoader(getClassLoader(), "jruby/bundler/startup.rb", false);
    }

    private boolean doesReflectionWork() {
        try {
            ClassLoader.class.getDeclaredMethod("getResourceAsStream", String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void bootstrap() {
        initCore();
        initExceptions();
        initLibraries();
    }

    private void initDefinedMessages() {
        for (DefinedMessage definedMessage : DefinedMessage.values()) {
            RubyString str = freezeAndDedupString(
                RubyString.newString(this, ByteList.create(definedMessage.getText())));
            definedMessages.put(definedMessage, str);
        }
    }

    private void initThreadStatuses() {
        for (RubyThread.Status status : RubyThread.Status.values()) {
            RubyString str = RubyString.newString(this, status.bytes);
            str.setFrozen(true);
            threadStatuses.put(status, str);
        }
    }

    private void initRoot() {
        // Bootstrap the top of the hierarchy
        basicObjectClass = RubyClass.createBootstrapClass(this, "BasicObject", null, RubyBasicObject.BASICOBJECT_ALLOCATOR);
        objectClass = RubyClass.createBootstrapClass(this, "Object", basicObjectClass, RubyObject.OBJECT_ALLOCATOR);
        moduleClass = RubyClass.createBootstrapClass(this, "Module", objectClass, RubyModule.MODULE_ALLOCATOR);
        classClass = RubyClass.createBootstrapClass(this, "Class", moduleClass, RubyClass.CLASS_ALLOCATOR);

        basicObjectClass.setMetaClass(classClass);
        objectClass.setMetaClass(basicObjectClass);
        moduleClass.setMetaClass(classClass);
        classClass.setMetaClass(classClass);

        RubyClass metaClass;
        metaClass = basicObjectClass.makeMetaClass(classClass);
        metaClass = objectClass.makeMetaClass(metaClass);
        metaClass = moduleClass.makeMetaClass(metaClass);
        metaClass = classClass.makeMetaClass(metaClass);

        RubyBasicObject.createBasicObjectClass(this, basicObjectClass);
        RubyObject.createObjectClass(this, objectClass);
        RubyModule.createModuleClass(this, moduleClass);
        RubyClass.createClassClass(this, classClass);

        // set constants now that they're initialized
        basicObjectClass.setConstant("BasicObject", basicObjectClass);
        objectClass.setConstant("Object", objectClass);
        objectClass.setConstant("Class", classClass);
        objectClass.setConstant("Module", moduleClass);

        // Initialize Kernel and include into Object
        RubyModule kernel = RubyKernel.createKernelModule(this);
        objectClass.includeModule(kernelModule);

        // In 1.9 and later, Kernel.gsub is defined only when '-p' or '-n' is given on the command line
        if (config.getKernelGsubDefined()) {
            kernel.addMethod("gsub", new JavaMethod(kernel, Visibility.PRIVATE, "gsub") {

                @Override
                public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
                    switch (args.length) {
                        case 1:
                            return RubyKernel.gsub(context, self, args[0], block);
                        case 2:
                            return RubyKernel.gsub(context, self, args[0], args[1], block);
                        default:
                            throw newArgumentError(String.format("wrong number of arguments %d for 1..2", args.length));
                    }
                }
            });
        }

        // Object is ready, create top self
        topSelf = TopSelfFactory.createTopSelf(this, false);

        // Pre-create all the core classes potentially referenced during startup
        RubyNil.createNilClass(this);
        RubyBoolean.createFalseClass(this);
        RubyBoolean.createTrueClass(this);

        nilObject = new RubyNil(this);
        for (int i=0; i<NIL_PREFILLED_ARRAY_SIZE; i++) nilPrefilledArray[i] = nilObject;
        singleNilArray = new IRubyObject[] {nilObject};

        falseObject = new RubyBoolean.False(this);
        falseObject.setFrozen(true);
        trueObject = new RubyBoolean.True(this);
        trueObject.setFrozen(true);

        reportOnException = trueObject;
    }

    private void initCore() {
        if (profile.allowClass("Data")) {
            dataClass = defineClass("Data", objectClass, ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
            getObject().deprecateConstant(this, "Data");
        }

        RubyComparable.createComparable(this);
        RubyEnumerable.createEnumerableModule(this);
        RubyString.createStringClass(this);

        encodingService = new EncodingService(this);

        RubySymbol.createSymbolClass(this);

        if (profile.allowClass("ThreadGroup")) {
            RubyThreadGroup.createThreadGroupClass(this);
        }
        if (profile.allowClass("Thread")) {
            RubyThread.createThreadClass(this);
        }
        if (profile.allowClass("Exception")) {
            RubyException.createExceptionClass(this);
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

        RubyEncoding.createEncodingClass(this);
        RubyConverter.createConverterClass(this);

        encodingService.defineEncodings();
        encodingService.defineAliases();

        // External should always have a value, but Encoding.external_encoding{,=} will lazily setup
        String encoding = config.getExternalEncoding();
        if (encoding != null && !encoding.equals("")) {
            Encoding loadedEncoding = encodingService.loadEncoding(ByteList.create(encoding));
            if (loadedEncoding == null) throw new MainExitException(1, "unknown encoding name - " + encoding);
            setDefaultExternalEncoding(loadedEncoding);
        } else {
            Encoding consoleEncoding = encodingService.getConsoleEncoding();
            Encoding availableEncoding = consoleEncoding == null ? encodingService.getLocaleEncoding() : consoleEncoding;
            setDefaultExternalEncoding(availableEncoding);
        }

        // Filesystem should always have a value
        if (Platform.IS_WINDOWS) {
            encoding = SafePropertyAccessor.getProperty("file.encoding", "UTF-8");
            Encoding filesystemEncoding = encodingService.loadEncoding(ByteList.create(encoding));
            if (filesystemEncoding == null) throw new MainExitException(1, "unknown encoding name - " + encoding);
            setDefaultFilesystemEncoding(filesystemEncoding);
        } else {
            setDefaultFilesystemEncoding(getDefaultExternalEncoding());
        }

        encoding = config.getInternalEncoding();
        if (encoding != null && !encoding.equals("")) {
            Encoding loadedEncoding = encodingService.loadEncoding(ByteList.create(encoding));
            if (loadedEncoding == null) throw new MainExitException(1, "unknown encoding name - " + encoding);
            setDefaultInternalEncoding(loadedEncoding);
        }

        if (profile.allowClass("Complex")) {
            RubyComplex.createComplexClass(this);
        }
        if (profile.allowClass("Rational")) {
            RubyRational.createRationalClass(this);
        }

        if (profile.allowClass("Hash")) {
            RubyHash.createHashClass(this);
        }
        if (profile.allowClass("Array")) {
            RubyArray.createArrayClass(this);
            emptyFrozenArray = newEmptyArray();
            emptyFrozenArray.setFrozen(true);
        }
        if (profile.allowClass("Float")) {
            RubyFloat.createFloatClass(this);
        }
        if (profile.allowClass("Bignum")) {
            RubyBignum.createBignumClass(this);
            // RubyRandom depends on Bignum existence.
            RubyRandom.createRandomClass(this);
        }
        ioClass = RubyIO.createIOClass(this);

        if (profile.allowClass("Struct")) {
            RubyStruct.createStructClass(this);
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

        TracePoint.createTracePointClass(this);

        RubyWarnings.createWarningModule(this);
    }

    public static final int NIL_PREFILLED_ARRAY_SIZE = RubyArray.ARRAY_DEFAULT_SIZE * 8;
    private final IRubyObject nilPrefilledArray[] = new IRubyObject[NIL_PREFILLED_ARRAY_SIZE];
    public IRubyObject[] getNilPrefilledArray() {
        return nilPrefilledArray;
    }

    private void initExceptions() {
        ifAllowed("StandardError",          (ruby) -> standardError = RubyStandardError.define(ruby, exceptionClass));
        ifAllowed("RubyError",              (ruby) -> runtimeError = RubyRuntimeError.define(ruby, standardError));
        ifAllowed("FrozenError",            (ruby) -> frozenError = RubyFrozenError.define(ruby, runtimeError));
        ifAllowed("IOError",                (ruby) -> ioError = RubyIOError.define(ruby, standardError));
        ifAllowed("ScriptError",            (ruby) -> scriptError = RubyScriptError.define(ruby, exceptionClass));
        ifAllowed("RangeError",             (ruby) -> rangeError = RubyRangeError.define(ruby, standardError));
        ifAllowed("SignalException",        (ruby) -> signalException = RubySignalException.define(ruby, exceptionClass));
        ifAllowed("NameError",              (ruby) -> {
            nameError = RubyNameError.define(ruby, standardError);
            nameErrorMessage = RubyNameError.RubyNameErrorMessage.define(ruby, nameError);
        });
        ifAllowed("NoMethodError",          (ruby) -> noMethodError = RubyNoMethodError.define(ruby, nameError));
        ifAllowed("SystemExit",             (ruby) -> systemExit = RubySystemExit.define(ruby, exceptionClass));
        ifAllowed("LocalJumpError",         (ruby) -> localJumpError = RubyLocalJumpError.define(ruby, standardError));
        ifAllowed("SystemCallError",        (ruby) -> systemCallError = RubySystemCallError.define(ruby, standardError));
        ifAllowed("Fatal",                  (ruby) -> fatal = RubyFatal.define(ruby, exceptionClass));
        ifAllowed("Interrupt",              (ruby) -> interrupt = RubyInterrupt.define(ruby, signalException));
        ifAllowed("TypeError",              (ruby) -> typeError = RubyTypeError.define(ruby, standardError));
        ifAllowed("ArgumentError",          (ruby) -> argumentError = RubyArgumentError.define(ruby, standardError));
        ifAllowed("UncaughtThrowError",     (ruby) -> uncaughtThrowError = RubyUncaughtThrowError.define(ruby, argumentError));
        ifAllowed("IndexError",             (ruby) -> indexError = RubyIndexError.define(ruby, standardError));
        ifAllowed("StopIteration",          (ruby) -> stopIteration = RubyStopIteration.define(ruby, indexError));
        ifAllowed("SyntaxError",            (ruby) -> syntaxError = RubySyntaxError.define(ruby, scriptError));
        ifAllowed("LoadError",              (ruby) -> loadError = RubyLoadError.define(ruby, scriptError));
        ifAllowed("NotImplementedError",    (ruby) -> notImplementedError = RubyNotImplementedError.define(ruby, scriptError));
        ifAllowed("SecurityError",          (ruby) -> securityError = RubySecurityError.define(ruby, exceptionClass));
        ifAllowed("NoMemoryError",          (ruby) -> noMemoryError = RubyNoMemoryError.define(ruby, exceptionClass));
        ifAllowed("RegexpError",            (ruby) -> regexpError = RubyRegexpError.define(ruby, standardError));
        // Proposal to RubyCommons for interrupting Regexps
        ifAllowed("InterruptedRegexpError", (ruby) -> interruptedRegexpError = RubyInterruptedRegexpError.define(ruby, regexpError));
        ifAllowed("EOFError",               (ruby) -> eofError = RubyEOFError.define(ruby, ioError));
        ifAllowed("ThreadError",            (ruby) -> threadError = RubyThreadError.define(ruby, standardError));
        ifAllowed("ConcurrencyError",       (ruby) -> concurrencyError = RubyConcurrencyError.define(ruby, threadError));
        ifAllowed("SystemStackError",       (ruby) -> systemStackError = RubySystemStackError.define(ruby, exceptionClass));
        ifAllowed("ZeroDivisionError",      (ruby) -> zeroDivisionError = RubyZeroDivisionError.define(ruby, standardError));
        ifAllowed("FloatDomainError",       (ruby) -> floatDomainError = RubyFloatDomainError.define(ruby, rangeError));
        ifAllowed("EncodingError",          (ruby) -> {
            encodingError = RubyEncodingError.define(ruby, standardError);
            encodingCompatibilityError = RubyEncodingError.RubyCompatibilityError.define(ruby, encodingError, encodingClass);
            invalidByteSequenceError = RubyEncodingError.RubyInvalidByteSequenceError.define(ruby, encodingError, encodingClass);
            undefinedConversionError = RubyEncodingError.RubyUndefinedConversionError.define(ruby, encodingError, encodingClass);
            converterNotFoundError = RubyEncodingError.RubyConverterNotFoundError.define(ruby, encodingError, encodingClass);
        });
        ifAllowed("Fiber",                  (ruby) -> fiberError = RubyFiberError.define(ruby, standardError));
        ifAllowed("ConcurrencyError",       (ruby) -> concurrencyError = RubyConcurrencyError.define(ruby, threadError));
        ifAllowed("KeyError",               (ruby) -> keyError = RubyKeyError.define(ruby, indexError));
        ifAllowed("DomainError",            (ruby) -> mathDomainError = RubyDomainError.define(ruby, argumentError, mathModule));

        initErrno();

        initNativeException();
    }

    private void ifAllowed(String name, Consumer<Ruby> callback) {
        if (profile.allowClass(name)) {
            callback.accept(this);
        }
    }

    @SuppressWarnings("deprecation")
    private void initNativeException() {
        if (profile.allowClass("NativeException")) {
            nativeException = NativeException.createClass(this, runtimeError);
        }
    }

    private void initLibraries() {
        Mutex.setup(this);
        ConditionVariable.setup(this);
        org.jruby.ext.thread.Queue.setup(this);
        SizedQueue.setup(this);
        new ThreadFiberLibrary().load(this, false);
    }

    private Map<Integer, RubyClass> errnos = new HashMap<Integer, RubyClass>();

    public RubyClass getErrno(int n) {
        return errnos.get(n);
    }

    /**
     * Create module Errno's Variables.  We have this method since Errno does not have its
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

                // map ENOSYS to NotImplementedError
                errnos.put(Errno.ENOSYS.intValue(), notImplementedError);
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
        // We cannot load any .rb and debug new parser features
        if (RubyInstanceConfig.DEBUG_PARSER) return;

        addLazyBuiltin("java.rb", "java", "org.jruby.javasupport.Java");
        addLazyBuiltin("jruby.rb", "jruby", "org.jruby.ext.jruby.JRubyLibrary");
        addLazyBuiltin("jruby/util.rb", "jruby/util", "org.jruby.ext.jruby.JRubyUtilLibrary");
        addLazyBuiltin("jruby/type.rb", "jruby/type", "org.jruby.ext.jruby.JRubyTypeLibrary");
        addLazyBuiltin("nkf.jar", "nkf", "org.jruby.ext.nkf.NKFLibrary");
        addLazyBuiltin("stringio.jar", "stringio", "org.jruby.ext.stringio.StringIOLibrary");
        addLazyBuiltin("strscan.jar", "strscan", "org.jruby.ext.strscan.StringScannerLibrary");
        addLazyBuiltin("zlib.jar", "zlib", "org.jruby.ext.zlib.ZlibLibrary");
        addLazyBuiltin("digest.jar", "digest.so", "org.jruby.ext.digest.DigestLibrary");
        addLazyBuiltin("digest/md5.jar", "digest/md5", "org.jruby.ext.digest.MD5");
        addLazyBuiltin("digest/rmd160.jar", "digest/rmd160", "org.jruby.ext.digest.RMD160");
        addLazyBuiltin("digest/sha1.jar", "digest/sha1", "org.jruby.ext.digest.SHA1");
        addLazyBuiltin("digest/sha2.jar", "digest/sha2", "org.jruby.ext.digest.SHA2");
        addLazyBuiltin("digest/bubblebabble.jar", "digest/bubblebabble", "org.jruby.ext.digest.BubbleBabble");
        addLazyBuiltin("bigdecimal.jar", "bigdecimal", "org.jruby.ext.bigdecimal.BigDecimalLibrary");
        addLazyBuiltin("io/wait.jar", "io/wait", "org.jruby.ext.io.wait.IOWaitLibrary");
        addLazyBuiltin("etc.jar", "etc", "org.jruby.ext.etc.EtcLibrary");
        addLazyBuiltin("timeout.rb", "timeout", "org.jruby.ext.timeout.Timeout");
        addLazyBuiltin("socket.jar", "socket", "org.jruby.ext.socket.SocketLibrary");
        addLazyBuiltin("rbconfig.rb", "rbconfig", "org.jruby.ext.rbconfig.RbConfigLibrary");
        addLazyBuiltin("jruby/serialization.rb", "serialization", "org.jruby.ext.jruby.JRubySerializationLibrary");
        addLazyBuiltin("ffi-internal.jar", "ffi-internal", "org.jruby.ext.ffi.FFIService");
        addLazyBuiltin("tempfile.jar", "tempfile", "org.jruby.ext.tempfile.TempfileLibrary");
        addLazyBuiltin("fcntl.rb", "fcntl", "org.jruby.ext.fcntl.FcntlLibrary");
        addLazyBuiltin("pathname.jar", "pathname", "org.jruby.ext.pathname.PathnameLibrary");
        addLazyBuiltin("set.rb", "set", "org.jruby.ext.set.SetLibrary");
        addLazyBuiltin("date.jar", "date", "org.jruby.ext.date.DateLibrary");

        addLazyBuiltin("mathn/complex.jar", "mathn/complex", "org.jruby.ext.mathn.Complex");
        addLazyBuiltin("mathn/rational.jar", "mathn/rational", "org.jruby.ext.mathn.Rational");
        addLazyBuiltin("ripper.jar", "ripper", "org.jruby.ext.ripper.RipperLibrary");
        addLazyBuiltin("coverage.jar", "coverage", "org.jruby.ext.coverage.CoverageLibrary");

        // TODO: implement something for these?
        addBuiltinIfAllowed("continuation.rb", Library.DUMMY);

        // for backward compatibility
        loadService.provide("enumerator.jar"); // can't be in RubyEnumerator because LoadService isn't ready then
        loadService.provide("rational.jar");
        loadService.provide("complex.jar");

        // we define the classes at boot because we need them
        addBuiltinIfAllowed("thread.rb", Library.DUMMY);

        if (RubyInstanceConfig.NATIVE_NET_PROTOCOL) {
            addLazyBuiltin("net/protocol.rb", "net/protocol", "org.jruby.ext.net.protocol.NetProtocolBufferedIOLibrary");
        }

        addBuiltinIfAllowed("win32ole.jar", new Library() {
            public void load(Ruby runtime, boolean wrap) throws IOException {
                runtime.getLoadService().require("jruby/win32ole/stub");
            }
        });

        addLazyBuiltin("cgi/escape.jar", "cgi/escape", "org.jruby.ext.cgi.escape.CGIEscape");

        if(Platform.IS_WINDOWS) {
          addLazyBuiltin("win32/resolv.jar", "win32/resolv", "org.jruby.ext.win32.resolv.Win32Resolv");
        }
    }

    private void initRubyKernel() {
        // We cannot load any .rb and debug new parser features
        if (RubyInstanceConfig.DEBUG_PARSER) return;

        // load Ruby parts of core
        loadService.loadFromClassLoader(getClassLoader(), "jruby/kernel.rb", false);
    }

    private void initRubyPreludes() {
        // We cannot load any .rb and debug new parser features
        if (RubyInstanceConfig.DEBUG_PARSER) return;

        // load Ruby parts of core
        loadService.loadFromClassLoader(getClassLoader(), "jruby/preludes.rb", false);
    }

    private void addLazyBuiltin(String name, String shortName, String className) {
        addBuiltinIfAllowed(name, new LateLoadingLibrary(shortName, className, getClassLoader()));
    }

    private void addBuiltinIfAllowed(String name, Library lib) {
        if (profile.allowBuiltin(name)) {
            loadService.addBuiltinLibrary(name, lib);
        }
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

    ///////////////////////////////////////////////////////////////////////////
    // Cached DynamicMethod objects, used for direct dispatch or for short
    // circuiting dynamic invocation logic.
    ///////////////////////////////////////////////////////////////////////////

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

    public boolean isDefaultMethodMissing(DynamicMethod method) {
        return defaultMethodMissing == method || defaultModuleMethodMissing == method;

    }

    public void setDefaultMethodMissing(DynamicMethod method, DynamicMethod moduleMethod) {
        defaultMethodMissing = method;
        defaultModuleMethodMissing = moduleMethod;
    }

    public DynamicMethod getRespondToMethod() {
        return respondTo;
    }

    public void setRespondToMethod(DynamicMethod rtm) {
        this.respondTo = rtm;
    }

    public DynamicMethod getRespondToMissingMethod() {
        return respondToMissing;
    }

    public void setRespondToMissingMethod(DynamicMethod rtmm) {
        this.respondToMissing = rtmm;
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

    public RubyClass getGenerator() {
        return generatorClass;
    }
    public void setGenerator(RubyClass generatorClass) {
        this.generatorClass = generatorClass;
    }

    public RubyClass getFiber() {
        return fiberClass;
    }
    public void setFiber(RubyClass fiberClass) {
        this.fiberClass = fiberClass;
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

    public RubyClass getLocation() {
        return locationClass;
    }

    public void setLocation(RubyClass location) {
        this.locationClass = location;
    }

    public RubyModule getWarning() {
        return warningModule;
    }

    public void setWarning(RubyModule warningModule) {
        this.warningModule = warningModule;
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

    public RubyClass getUncaughtThrowError() {
        return uncaughtThrowError;
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

    public RubyClass getFrozenError() {
        return frozenError;
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

    public RubyClass getInterruptedRegexpError() {
        return interruptedRegexpError;
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

    RubyRandom.RandomType defaultRand;

    /**
     * @deprecated internal API, to be hidden
     */
    public RubyRandom.RandomType getDefaultRand() {
        return defaultRand;
    }

    /**
     * @deprecated internal API, to be hidden
     */
    public void setDefaultRand(RubyRandom.RandomType defaultRand) {
        this.defaultRand = defaultRand;
    }

    private RubyHash charsetMap;
    @Deprecated // no longer used (internal API)
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
        // we try to getService the classloader that loaded JRuby, falling back on System
        ClassLoader loader = Ruby.class.getClassLoader();
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
        }

        return loader;
    }

    public JRubyClassLoader getJRubyClassLoader() {
        return jrubyClassLoader;
    }

    /** Defines a global variable
     */
    public void defineVariable(final GlobalVariable variable, org.jruby.internal.runtime.GlobalVariable.Scope scope) {
        globalVariables.define(variable.name(), new IAccessor() {
            @Override
            public IRubyObject getValue() {
                return variable.get();
            }

            @Override
            public IRubyObject setValue(IRubyObject newValue) {
                return variable.set(newValue);
            }
        }, scope);
    }

    /** defines a readonly global variable
     *
     */
    public void defineReadonlyVariable(String name, IRubyObject value, org.jruby.internal.runtime.GlobalVariable.Scope scope) {
        globalVariables.defineReadonly(name, new ValueAccessor(value), scope);
    }

    // Obsolete parseFile function
    public Node parseFile(InputStream in, String file, DynamicScope scope) {
        return parseFile(in, file, scope, 0);
    }

    // Modern variant of parsFile function above
    public ParseResult parseFile(String file, InputStream in, DynamicScope scope) {
       return parseFile(file, in, scope, 0);
    }

    // Obsolete parseFile function
    public Node parseFile(InputStream in, String file, DynamicScope scope, int lineNumber) {
        addLoadParseToStats();
        return parseFileAndGetAST(in, file, scope, lineNumber, false);
    }

    // Modern variant of parseFile function above
    public ParseResult parseFile(String file, InputStream in, DynamicScope scope, int lineNumber) {
        addLoadParseToStats();

        if (!RubyInstanceConfig.IR_READING) return parseFileAndGetAST(in, file, scope, lineNumber, false);

        try {
            // Get IR from .ir file
            return IRReader.load(getIRManager(), new IRReaderStream(getIRManager(), IRFileExpert.getIRPersistedFile(file), new ByteList(file.getBytes())));
        } catch (IOException e) {
            // FIXME: What is something actually throws IOException
            return parseFileAndGetAST(in, file, scope, lineNumber, false);
        }
    }

    // Obsolete parseFileFromMain function
    public Node parseFileFromMain(InputStream in, String file, DynamicScope scope) {
        addLoadParseToStats();

        return parseFileFromMainAndGetAST(in, file, scope);
    }

    // Modern variant of parseFileFromMain function above
    public ParseResult parseFileFromMain(String file, InputStream in, DynamicScope scope) {
        addLoadParseToStats();

        if (!RubyInstanceConfig.IR_READING) return parseFileFromMainAndGetAST(in, file, scope);

        try {
            return IRReader.load(getIRManager(), new IRReaderStream(getIRManager(), IRFileExpert.getIRPersistedFile(file), new ByteList(file.getBytes())));
        } catch (IOException e) {
            System.out.println(e);
            e.printStackTrace();
            return parseFileFromMainAndGetAST(in, file, scope);
        }
    }

     private Node parseFileFromMainAndGetAST(InputStream in, String file, DynamicScope scope) {
         return parseFileAndGetAST(in, file, scope, 0, true);
     }

     private Node parseFileAndGetAST(InputStream in, String file, DynamicScope scope, int lineNumber, boolean isFromMain) {
         ParserConfiguration parserConfig =
                 new ParserConfiguration(this, lineNumber, false, true, isFromMain, config);
         setupSourceEncoding(parserConfig, UTF8Encoding.INSTANCE);
         return parser.parse(file, in, scope, parserConfig);
     }

    public Node parseInline(InputStream in, String file, DynamicScope scope) {
        addEvalParseToStats();
        ParserConfiguration parserConfig =
                new ParserConfiguration(this, 0, false, true, false, config);
        setupSourceEncoding(parserConfig, getEncodingService().getLocaleEncoding());
        return parser.parse(file, in, scope, parserConfig);
    }

    private void setupSourceEncoding(ParserConfiguration parserConfig, Encoding defaultEncoding) {
        if (config.getSourceEncoding() != null) {
            if (config.isVerbose()) {
                config.getError().println("-K is specified; it is for 1.8 compatibility and may cause odd behavior");
            }
            parserConfig.setDefaultEncoding(getEncodingService().getEncodingFromString(config.getSourceEncoding()));
        } else {
            parserConfig.setDefaultEncoding(defaultEncoding);
        }
    }

    public Node parseEval(String content, String file, DynamicScope scope, int lineNumber) {
        addEvalParseToStats();

        return parser.parse(file, encodeToBytes(content), scope, new ParserConfiguration(this, lineNumber, false, false, config));
    }

    private byte[] encodeToBytes(String string) {
        Charset charset = getDefaultCharset();

        byte[] bytes = charset == null ? string.getBytes() : string.getBytes(charset);

        return bytes;
    }

    @Deprecated
    public Node parse(String content, String file, DynamicScope scope, int lineNumber,
            boolean extraPositionInformation) {
        return parser.parse(file, content.getBytes(), scope, new ParserConfiguration(this,
                lineNumber, extraPositionInformation, false, true, config));
    }

    public Node parseEval(ByteList content, String file, DynamicScope scope, int lineNumber) {
        addEvalParseToStats();
        return parser.parse(file, content, scope, new ParserConfiguration(this,
                lineNumber, false, false, false, config));
    }

    public Node parse(ByteList content, String file, DynamicScope scope, int lineNumber,
            boolean extraPositionInformation) {
        addEvalParseToStats();
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

    /**
     * This is an internal encoding if actually specified via default_internal=
     * or passed in via -E.
     *
     * @return null or encoding
     */
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

    public Encoding getDefaultFilesystemEncoding() {
        return defaultFilesystemEncoding;
    }

    public void setDefaultFilesystemEncoding(Encoding defaultFilesystemEncoding) {
        this.defaultFilesystemEncoding = defaultFilesystemEncoding;
    }

    /**
     * Get the default java.nio.charset.Charset for the current default internal encoding.
     */
    public Charset getDefaultCharset() {
        Encoding enc = getDefaultEncoding();

        Charset charset = EncodingUtils.charsetForEncoding(enc);

        return charset;
    }

    /**
     * Return the default internal encoding, if set, or UTF-8 by default.
     *
     * @return the default encoding used for new Ruby strings
     */
    public Encoding getDefaultEncoding() {
        Encoding enc = getDefaultInternalEncoding();
        if (enc == null) {
            enc = UTF8Encoding.INSTANCE;
        }
        return enc;
    }

    public EncodingService getEncodingService() {
        return encodingService;
    }

    public RubyWarnings getWarnings() {
        return warnings;
    }

    WarnCallback getRegexpWarnings() {
        return regexpWarnings;
    }

    public PrintStream getErrorStream() {
        // FIXME: We can't guarantee this will always be a RubyIO...so the old code here is not safe
        /*java.io.OutputStream os = ((RubyIO) getGlobalVariables().getService("$stderr")).getOutStream();
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

    public RubyModule getClassFromPath(final String path) {
        if (path.length() == 0 || path.charAt(0) == '#') {
            throw newTypeError(str(this, "can't retrieve anonymous class ", ids(this, path)));
        }

        RubyModule c = getObject();
        int pbeg = 0, p = 0;
        for ( final int l = path.length(); p < l; ) {
            while ( p < l && path.charAt(p) != ':' ) p++;

            final String str = path.substring(pbeg, p);

            if ( p < l && path.charAt(p) == ':' ) {
                if ( ++p < l && path.charAt(p) != ':' ) {
                    throw newTypeError(str(this, "undefined class/module ", ids(this, str)));
                }
                pbeg = ++p;
            }

            IRubyObject cc = c.getConstant(str);
            if ( ! ( cc instanceof RubyModule ) ) {
                throw newTypeError(str(this, ids(this, path), " does not refer to class/module"));
            }
            c = (RubyModule) cc;
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
        String backtrace = config.getTraceType().printBacktrace(excp, errorStream == System.err && getPosix().isatty(FileDescriptor.err));
        try {
            errorStream.print(backtrace);
        } catch (Exception e) {
            System.err.print(backtrace);
        }
    }

    public void printError(Throwable t) {
        if (t instanceof RaiseException) {
            printError(((RaiseException) t).getException());
        }
        PrintStream errorStream = getErrorStream();
        try {
            t.printStackTrace(errorStream);
        } catch (Exception e) {
            t.printStackTrace(System.err);
        }
    }

    static final String ROOT_FRAME_NAME = "(root)";

    public void loadFile(String scriptName, InputStream in, boolean wrap) {
        IRubyObject self = wrap ? getTopSelf().rbClone() : getTopSelf();
        ThreadContext context = getCurrentContext();
        String file = context.getFile();

        try {
            ThreadContext.pushBacktrace(context, ROOT_FRAME_NAME, file, 0);
            context.preNodeEval(self);
            ParseResult parseResult = parseFile(scriptName, in, null);
            RootNode root = (RootNode) parseResult;

            if (wrap) {
                // toss an anonymous module into the search path
                wrapRootForLoad((RubyBasicObject) self, root);
            }

            runInterpreter(context, parseResult, self);
        } finally {
            context.postNodeEval();
            ThreadContext.popBacktrace(context);
        }
    }

    public void loadScope(IRScope scope, boolean wrap) {
        IRubyObject self = wrap ? TopSelfFactory.createTopSelf(this, true) : getTopSelf();
        ThreadContext context = getCurrentContext();
        String file = context.getFile();

        try {
            ThreadContext.pushBacktrace(context, ROOT_FRAME_NAME, file, 0);
            context.preNodeEval(self);

            if (wrap) {
                // toss an anonymous module into the search path
                scope.getStaticScope().setModule(RubyModule.newModule(this));
            }

            runInterpreter(context, scope, self);
        } finally {
            context.postNodeEval();
            ThreadContext.popBacktrace(context);
        }
    }

    public void compileAndLoadFile(String filename, InputStream in, boolean wrap) {
        IRubyObject self = wrap ? getTopSelf().rbClone() : getTopSelf();
        ThreadContext context = getCurrentContext();
        InputStream readStream = in;

        String oldFile = context.getFile();
        int oldLine = context.getLine();
        try {
            context.preNodeEval(self);
            ParseResult parseResult = parseFile(filename, in, null);
            RootNode root = (RootNode) parseResult;

            if (wrap) {
                wrapRootForLoad((RubyBasicObject) self, root);
            } else {
                root.getStaticScope().setModule(getObject());
            }

            runNormally(root, wrap);
        } finally {
            context.postNodeEval();
        }
    }

    private void wrapRootForLoad(RubyBasicObject self, RootNode root) {
        // toss an anonymous module into the search path
        RubyModule wrapper = RubyModule.newModule(this);
        self.extend(new IRubyObject[] {wrapper});
        StaticScope top = root.getStaticScope();
        StaticScope newTop = staticScopeFactory.newLocalScope(null);
        top.setPreviousCRefScope(newTop);
        top.setModule(wrapper);
    }

    public void loadScript(Script script) {
        loadScript(script, false);
    }

    public void loadScript(Script script, boolean wrap) {
        script.load(getCurrentContext(), getTopSelf(), wrap);
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
        IRubyObject self = wrap ? TopSelfFactory.createTopSelf(this, true) : getTopSelf();
        ThreadContext context = getCurrentContext();

        try {
            context.preExtensionLoad(self);

            extension.basicLoad(this);
        } catch (IOException ioe) {
            throw newIOErrorFromException(ioe);
        } finally {
            context.postNodeEval();
        }
    }

    public void addBoundMethod(String className, String methodName, String rubyName) {
        Map<String, String> javaToRuby = boundMethods.get(className);
        if (javaToRuby == null) boundMethods.put(className, javaToRuby = new HashMap<>());
        javaToRuby.put(methodName, rubyName);
    }

    public void addBoundMethods(String className, String... tuples) {
        Map<String, String> javaToRuby = boundMethods.get(className);
        if (javaToRuby == null) boundMethods.put(className, javaToRuby = new HashMap<>(tuples.length / 2 + 1, 1));
        for (int i = 0; i < tuples.length; i += 2) {
            javaToRuby.put(tuples[i], tuples[i+1]);
        }
    }

    @Deprecated // no longer used -> except for IndyBinder
    public void addBoundMethodsPacked(String className, String packedTuples) {
        List<String> names = StringSupport.split(packedTuples, ';');
        for (int i = 0; i < names.size(); i += 2) {
            addBoundMethod(className, names.get(i), names.get(i+1));
        }
    }

    @Deprecated // no longer used -> except for IndyBinder
    public void addSimpleBoundMethodsPacked(String className, String packedNames) {
        List<String> names = StringSupport.split(packedNames, ';');
        for (String name : names) {
            addBoundMethod(className, name, name);
        }
    }

    public Map<String, Map<String, String>> getBoundMethods() {
        return boundMethods;
    }

    public void setJavaProxyClassFactory(JavaProxyClassFactory factory) {
        this.javaProxyClassFactory = factory;
    }

    public JavaProxyClassFactory getJavaProxyClassFactory() {
        return javaProxyClassFactory;
    }

    public class CallTraceFuncHook extends EventHook {
        private RubyProc traceFunc;
        private EnumSet<RubyEvent> interest =
                EnumSet.of(
                        RubyEvent.C_CALL,
                        RubyEvent.C_RETURN,
                        RubyEvent.CALL,
                        RubyEvent.CLASS,
                        RubyEvent.END,
                        RubyEvent.LINE,
                        RubyEvent.RAISE,
                        RubyEvent.RETURN
                );

        public void setTraceFunc(RubyProc traceFunc) {
            this.traceFunc = traceFunc;
        }

        public void eventHandler(ThreadContext context, String eventName, String file, int line, String name, IRubyObject type) {
            if (!context.isWithinTrace()) {
                if (file == null) file = "(ruby)";
                if (type == null) type = getNil();

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

        @Override
        public boolean isInterestedInEvent(RubyEvent event) {
            return interest.contains(event);
        }
    };

    private final CallTraceFuncHook callTraceFuncHook = new CallTraceFuncHook();

    public synchronized void addEventHook(EventHook hook) {
        if (!RubyInstanceConfig.FULL_TRACE_ENABLED) {
            // without full tracing, many events will not fire
            getWarnings().warn("tracing (e.g. set_trace_func) will not capture all events without --debug flag");
        }

        EventHook[] hooks = eventHooks;
        EventHook[] newHooks = Arrays.copyOf(hooks, hooks.length + 1);
        newHooks[hooks.length] = hook;
        eventHooks = newHooks;
        hasEventHooks = true;
    }

    public synchronized void removeEventHook(EventHook hook) {
        EventHook[] hooks = eventHooks;

        if (hooks.length == 0) return;

        int pivot = -1;
        for (int i = 0; i < hooks.length; i++) {
            if (hooks[i] == hook) {
                pivot = i;
                break;
            }
        }

        if (pivot == -1) return; // No such hook found.

        EventHook[] newHooks = new EventHook[hooks.length - 1];
        // copy before and after pivot into the new array but don't bother
        // to arraycopy if pivot is first/last element of the old list.
        if (pivot != 0) System.arraycopy(hooks, 0, newHooks, 0, pivot);
        if (pivot != hooks.length-1) System.arraycopy(hooks, pivot + 1, newHooks, pivot, hooks.length - (pivot + 1));

        eventHooks = newHooks;
        hasEventHooks = newHooks.length > 0;
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
            for (int i = 0; i < eventHooks.length; i++) {
                EventHook eventHook = eventHooks[i];

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

    /**
     * It is possible for looping or repeated execution to encounter the same END
     * block multiple times.  Rather than store extra runtime state we will just
     * make sure it is not already registered.  at_exit by contrast can push the
     * same block many times (and should use pushExistBlock).
     */
    public void pushEndBlock(RubyProc proc) {
        if (alreadyRegisteredEndBlock(proc) != null) return;
        pushExitBlock(proc);
    }

    private RubyProc alreadyRegisteredEndBlock(RubyProc newProc) {
        Block block = newProc.getBlock();

        for (RubyProc proc: atExitBlocks) {
            if (block.equals(proc.getBlock())) return proc;
        }
        return null;
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
     * Make sure Kernel#at_exit procs getService invoked on runtime shutdown.
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

        // clear out old style recursion guards so they don't leak
        mriRecursionGuard = null;

        final ThreadContext context = getCurrentContext();

        // FIXME: 73df3d230b9d92c7237d581c6366df1b92ad9b2b exposed no toplevel scope existing anymore (I think the
        // bogus scope I removed was playing surrogate toplevel scope and wallpapering this bug).  For now, add a
        // bogus scope back for at_exit block run.  This is buggy if at_exit is capturing vars.
        if (!context.hasAnyScopes()) {
            StaticScope topStaticScope = getStaticScopeFactory().newLocalScope(null);
            context.pushScope(new ManyVarsDynamicScope(topStaticScope, null));
        }

        while (!atExitBlocks.empty()) {
            RubyProc proc = atExitBlocks.pop();
            // IRubyObject oldExc = context.runtime.getGlobalVariables().get("$!"); // Save $!
            try {
                proc.call(context, IRubyObject.NULL_ARRAY);
            } catch (RaiseException rj) {
                RubyException raisedException = rj.getException();
                if (!getSystemExit().isInstance(raisedException)) {
                    status = 1;
                    printError(raisedException);
                } else {
                    IRubyObject statusObj = raisedException.callMethod(context, "status");
                    if (statusObj != null && !statusObj.isNil()) {
                        status = RubyNumeric.fix2int(statusObj);
                    }
                }
                // Reset $! now that rj has been handled
                // context.runtime.getGlobalVariables().set("$!", oldExc);
            }
        }

        // Fetches (and unsets) the SIGEXIT handler, if one exists.
        IRubyObject trapResult = RubySignal.__jtrap_osdefault_kernel(this.getNil(), this.newString("EXIT"));
        if (trapResult instanceof RubyArray) {
            IRubyObject[] trapResultEntries = ((RubyArray) trapResult).toJavaArray();
            IRubyObject exitHandlerProc = trapResultEntries[0];
            if (exitHandlerProc instanceof RubyProc) {
                ((RubyProc) exitHandlerProc).call(context, getSingleNilArray());
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
        getBeanManager().unregisterMethodCache();
        getBeanManager().unregisterRuntime();

        getSelectorPool().cleanup();

        // NOTE: its intentional that we're not doing releaseClassLoader();

        if (config.isProfilingEntireRun()) {
            // not using logging because it's formatted
            ProfileCollection profileCollection = threadService.getMainThread().getContext().getProfileCollection();
            printProfileData(profileCollection);
        }

        if (systemExit && status != 0) {
            throw newSystemExit(status);
        }

        // This is a rather gross way to ensure nobody else performs the same clearing of globalRuntime followed by
        // initializing a new runtime, which would cause our clear below to clear the wrong runtime. Synchronizing
        // against the class is a problem, but the overhead of teardown and creating new containers should outstrip
        // a global synchronize around a few field accesses. -CON
        if (this == globalRuntime) {
            synchronized (Ruby.class) {
                if (this == globalRuntime) {
                    globalRuntime = null;
                }
            }
        }
    }

    /**
     * By default {@link #tearDown(boolean)} does not release the class-loader's
     * resources as threads might be still running accessing the classes/packages
     * even after the runtime has been torn down.
     *
     * This method exists to handle such cases, e.g. with embedded uses we always
     * release the runtime loader but not otherwise - you should do that manually.
     */
    public void releaseClassLoader() {
        if (jrubyClassLoader != null) {
            jrubyClassLoader.close();
            //jrubyClassLoader = null;
        }
    }

    /**
     * TDOD remove the synchronized. Synchronization should be a implementation detail of the ProfilingService.
     * @param profileData
     */
    public synchronized void printProfileData( ProfileCollection profileData ) {
        getProfilingService().newProfileReporter(getCurrentContext()).report(profileData);
    }

    /**
     * Simple getter for #profilingServiceLookup to avoid direct property access
     * @return #profilingServiceLookup
     */
    private ProfilingServiceLookup getProfilingServiceLookup() {
        return profilingServiceLookup;
    }

    /**
     *
     * @return the, for this ruby instance, configured implementation of ProfilingService, or null
     */
    public ProfilingService getProfilingService() {
        ProfilingServiceLookup lockup = getProfilingServiceLookup();
        return lockup == null ? null : lockup.getService();
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

    public RubyArray getEmptyFrozenArray() {
        return emptyFrozenArray;
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

        return RubyProc.newProc(this, block, type);
    }

    public RubyProc newBlockPassProc(Block.Type type, Block block) {
        if (type != Block.Type.LAMBDA && block.getProcObject() != null) return block.getProcObject();

        return RubyProc.newProc(this, block, type);
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

    public RubySymbol newSymbol(String name, Encoding encoding) {
        ByteList byteList = RubyString.encodeBytelist(name, encoding);
        return symbolTable.getSymbol(byteList);
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

    public RaiseException newArgumentError(String name, int got, int expected) {
        return newRaiseException(getArgumentError(), str(this, "wrong number of arguments calling `", ids(this, name),  ("` (" + got + " for " + expected + ")")));
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
        return newLightweightErrnoException(getIO().getClass("EINPROGRESSWaitWritable"), "");
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

    public RaiseException newErrnoECONNREFUSEDError(String message) {
        return newRaiseException(getErrno().getClass("ECONNREFUSED"), message);
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

    public RaiseException newErrnoELOOPError() {
        return newRaiseException(getErrno().getClass("ELOOP"), "Too many levels of symbolic links");
    }

    public RaiseException newErrnoEMFILEError() {
        return newRaiseException(getErrno().getClass("EMFILE"), "Too many open files");
    }

    public RaiseException newErrnoENFILEError() {
        return newRaiseException(getErrno().getClass("ENFILE"), "Too many open files in system");
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
        return newLightweightErrnoException(getModule("IO").getClass("EAGAINWaitReadable"), message);
    }

    public RaiseException newErrnoEAGAINWritableError(String message) {
        return newLightweightErrnoException(getModule("IO").getClass("EAGAINWaitWritable"), message);
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
        return newLightweightErrnoException(getIO().getClass("EINPROGRESSWaitWritable"), message);
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

    public RaiseException newErrnoEOPNOTSUPPError(String message) {
        return newRaiseException(getErrno().getClass("EOPNOTSUPP"), message);
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

    public RaiseException newErrnoENETUNREACHError() {
        return newRaiseException(getErrno().getClass("ENETUNREACH"), null);
    }

    public RaiseException newErrnoEMSGSIZEError() {
        return newRaiseException(getErrno().getClass("EMSGSIZE"), null);
    }

    public RaiseException newErrnoEXDEVError(String message) {
        return newRaiseException(getErrno().getClass("EXDEV"), message);
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

    public RaiseException newKeyError(String message, IRubyObject recv, IRubyObject key) {
        return new RubyKeyError(this, getKeyError(), message, recv, key).toThrowable();
    }

    public RaiseException newErrnoEINTRError() {
        return newRaiseException(getErrno().getClass("EINTR"), "Interrupted");
    }

    public RaiseException newErrnoEAFNOSUPPORTError(String message) {
        return newRaiseException(getErrno().getClass("EAFNOSUPPORT"), message);
    }

    public RaiseException newErrnoFromLastPOSIXErrno() {
        RubyClass errnoClass = getErrno(getPosix().errno());
        if (errnoClass == null) errnoClass = systemCallError;

        return newRaiseException(errnoClass, null);
    }

    public RaiseException newErrnoFromInt(int errno, String methodName, String message) {
        if (Platform.IS_WINDOWS && ("stat".equals(methodName) || "lstat".equals(methodName))) {
            if (errno == 20047) return newErrnoENOENTError(message); // boo:bar UNC stat failure
            if (errno == Errno.ESRCH.intValue()) return newErrnoENOENTError(message); // ESRCH on stating ""
        }

        return newErrnoFromInt(errno, message);
    }

    public RaiseException newErrnoFromInt(int errno, String message) {
        RubyClass errnoClass = getErrno(errno);
        if (errnoClass != null) {
            return newRaiseException(errnoClass, message);
        } else {
            return newSystemCallError("Unknown Error (" + errno + ") - " + message);
        }
    }

    public RaiseException newErrnoFromErrno(Errno errno, String message) {
        if (errno == null || errno == Errno.__UNKNOWN_CONSTANT__) {
            return newSystemCallError(message);
        }
        return newErrnoFromInt(errno.intValue(), message);
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
		return newErrnoEADDRFromBindException(be, null);
	}

    public RaiseException newErrnoEADDRFromBindException(BindException be, String contextMessage) {
        String msg = be.getMessage();
        if (msg == null) {
            msg = "bind";
        } else {
            msg = "bind - " + msg;
        }
        if (contextMessage != null) {
            msg = msg + contextMessage;
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

    public RaiseException newInterruptedRegexpError(String message) {
        return newRaiseException(getInterruptedRegexpError(), message);
    }

    public RaiseException newRangeError(String message) {
        return newRaiseException(getRangeError(), message);
    }

    public RaiseException newNotImplementedError(String message) {
        return newRaiseException(getNotImplementedError(), message);
    }

    @Deprecated
    public RaiseException newInvalidEncoding(String message) {
        return newRaiseException(getClass("Iconv").getClass("InvalidEncoding"), message);
    }

    @Deprecated
    public RaiseException newIllegalSequence(String message) {
        return newRaiseException(getClass("Iconv").getClass("IllegalSequence"), message);
    }

    /**
     * @see Ruby#newNameError(String, IRubyObject, IRubyObject, boolean)
     */
    public RaiseException newNameError(String message, IRubyObject recv, IRubyObject name) {
        return newNameError(message, recv, name, false);
    }

    /**
     * Construct a NameError that formats its message with an sprintf format string.
     *
     * The arguments given to sprintf are as follows:
     *
     * 0: the name that failed
     * 1: the receiver object that failed
     * 2: a ":" character for non-singleton recv, blank otherwise
     * 3: the name of the a non-singleton recv's class, blank if recv is a singleton
     *
     * Passing a string with no format characters will warn in verbose mode and error in debug mode.
     *
     * See jruby/jruby#3934.
     *
     * @param message an sprintf format string for the message
     * @param recv the receiver object
     * @param name the name that failed
     * @param privateCall whether the failure was due to method visibility
     * @return a new NameError
     */
    public RaiseException newNameError(String message, IRubyObject recv, IRubyObject name, boolean privateCall) {
        IRubyObject msg = new RubyNameError.RubyNameErrorMessage(this, message, recv, name);
        RubyException err = RubyNameError.newNameError(getNameError(), msg, name, privateCall);

        return err.toThrowable();
    }

    /**
     * Construct a NameError that formats its message with an sprintf format string.
     *
     * This version just accepts a java.lang.String for the name.
     *
     * @see Ruby#newNameError(String, IRubyObject, IRubyObject)
     */
    public RaiseException newNameError(String message, IRubyObject recv, String name) {
        return newNameError(message, recv, name, false);
    }

    /**
     * Construct a NameError that formats its message with an sprintf format string and has private_call? set to given.
     *
     * This version just accepts a java.lang.String for the name.
     *
     * @see Ruby#newNameError(String, IRubyObject, IRubyObject)
     */
    public RaiseException newNameError(String message, IRubyObject recv, String name, boolean privateCall) {
        RubySymbol nameSym = newSymbol(name);
        return newNameError(message, recv, nameSym, privateCall);
    }

    /**
     * Construct a NameError with the given pre-formatted message, name, and optional original exception.
     *
     * If the original exception is given, and either we are in verbose mode with printWhenVerbose set to true
     * or we are in debug mode.
     *
     * @param message the pre-formatted message for the NameError
     * @param name the name that failed
     * @param origException the original exception, or null
     * @param printWhenVerbose whether to log this exception when verbose mode is enabled
     * @return a new NameError
     */
    public RaiseException newNameError(String message, String name, Throwable origException, boolean printWhenVerbose) {
        if (origException != null) {
            if (printWhenVerbose && isVerbose()) {
                LOG.error(origException.getMessage(), origException);
            } else if (isDebug()) {
                LOG.debug(origException.getMessage(), origException);
            }
        }

        return new RubyNameError(this, getNameError(), message, name).toThrowable();
    }

    /**
     * Construct a NameError with a pre-formatted message and name.
     *
     * This is the same as calling {@link #newNameError(String, String, Throwable)} with a null
     * originating exception.
     *
     * @param message the pre-formatted message for the error
     * @param name the name that failed
     * @return a new NameError
     */
    public RaiseException newNameError(String message, String name) {
        return newNameError(message, name, null);
    }

    /**
     * Construct a NameError with an optional originating exception and a pre-formatted message.
     *
     * This is the same as calling {@link #newNameError(String, String, Throwable, boolean)} with a null
     * originating exception and false for verbose-mode logging.
     *
     * @param message a formatted string message for the error
     * @param name the name that failed
     * @param origException the original exception, or null if none
     * @return a new NameError
     */
    public RaiseException newNameError(String message, String name, Throwable origException) {
        return newNameError(message, name, origException, false);
    }

    /**
     * @see Ruby#newNoMethodError(String, IRubyObject, String, RubyArray, boolean)
     */
    public RaiseException newNoMethodError(String message, IRubyObject recv, String name, RubyArray args) {
        return newNoMethodError(message, recv, name, args, false);
    }

    /**
     * Construct a NoMethodError that formats its message with an sprintf format string.
     *
     * This works like {@link #newNameError(String, IRubyObject, IRubyObject)} but accepts
     * a java.lang.String for name and a RubyArray of the original call arguments.
     *
     * @see Ruby#newNameError(String, IRubyObject, IRubyObject)
     *
     * @return a new NoMethodError
     */
    public RaiseException newNoMethodError(String message, IRubyObject recv, String name, RubyArray args, boolean privateCall) {
        RubySymbol nameStr = newSymbol(name);
        IRubyObject msg = new RubyNameError.RubyNameErrorMessage(this, message, recv, nameStr);
        RubyException err = RubyNoMethodError.newNoMethodError(getNoMethodError(), msg, nameStr, args, privateCall);

        return err.toThrowable();
    }

    /**
     * Construct a NoMethodError with a pre-formatted message.
     *
     * @param message the pre-formatted message
     * @param name the name that failed
     * @param args the original arguments to the call that failed
     * @return a new NoMethodError
     */
    public RaiseException newNoMethodError(String message, String name, IRubyObject args) {
        return new RubyNoMethodError(this, getNoMethodError(), message, name, args).toThrowable();
    }

    public RaiseException newLocalJumpError(RubyLocalJumpError.Reason reason, IRubyObject exitValue, String message) {
        return new RubyLocalJumpError(this, getLocalJumpError(), message, reason, exitValue).toThrowable();
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

    public RaiseException newLoadError(String message, String path) {
        RaiseException loadError = newRaiseException(getLoadError(), message);
        loadError.getException().setInstanceVariable("@path", newString(path));
        return loadError;
    }

    public RaiseException newFrozenError(String objectType) {
        return newFrozenError(objectType, false);
    }

    public RaiseException newFrozenError(RubyModule type) {
        return newRaiseException(getFrozenError(), str(this, "can't modify frozen ", types(this, type)));
    }

    public RaiseException newFrozenError(String objectType, boolean runtimeError) {
        return newRaiseException(getFrozenError(), str(this, "can't modify frozen ", ids(this, objectType)));
    }

    public RaiseException newSystemStackError(String message) {
        return newRaiseException(getSystemStackError(), message);
    }

    public RaiseException newSystemStackError(String message, StackOverflowError soe) {
        if ( isDebug() ) LOG.debug(soe);
        return newRaiseException(getSystemStackError(), message);
    }

    public RaiseException newSystemExit(int status) {
        return RubySystemExit.newInstance(this, status, "exit").toThrowable();
    }

    public RaiseException newSystemExit(int status, String message) {
        return RubySystemExit.newInstance(this, status, message).toThrowable();
    }

    public RaiseException newIOError(String message) {
        return newRaiseException(getIOError(), message);
    }

    public RaiseException newStandardError(String message) {
        return newRaiseException(getStandardError(), message);
    }

    /**
     * Java does not give us enough information for specific error conditions
     * so we are reduced to divining them through string matches...
     *
     * TODO: Should ECONNABORTED get thrown earlier in the descriptor itself or is it ok to handle this late?
     * TODO: Should we include this into Errno code somewhere do we can use this from other places as well?
     */
    public RaiseException newIOErrorFromException(final IOException ex) {
        return Helpers.newIOErrorFromException(this, ex);
    }

    public RaiseException newTypeError(IRubyObject receivedObject, RubyClass expectedType) {
        return newTypeError(receivedObject, expectedType.getName());
    }

    public RaiseException newTypeError(IRubyObject receivedObject, RubyModule expectedType) {
        return newTypeError(receivedObject, expectedType.getName());
    }

    public RaiseException newTypeError(IRubyObject receivedObject, String expectedType) {
        return newRaiseException(getTypeError(),
                str(this, "wrong argument type ",
                        receivedObject.getMetaClass().getRealClass().toRubyString(getCurrentContext()),
                        " (expected ", ids(this, expectedType), ")"));
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
        return RaiseException.from(this, exceptionClass, message);
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
            return RaiseException.from(this, exceptionClass, message);
        } else {
            return RaiseException.from(this, exceptionClass, ERRNO_BACKTRACE_MESSAGE, disabledBacktrace());
        }
    }

    /**
     * Generate a StopIteration exception. This differs from the normal logic
     * by avoiding the generation of a backtrace. StopIteration is used by
     * Enumerator to end an external iteration, and so generating a full
     * backtrace is usually unreasonable overhead. The flag
     * -Xstop_iteration.backtrace=true or the property
     * jruby.stop_iteration.backtrace=true forces all StopIteration exceptions
     * to generate a backtrace.
     *
     * @param message the message for the exception
     */
    public RaiseException newStopIteration(IRubyObject result, String message) {
        final ThreadContext context = getCurrentContext();

        if (message == null) message = STOPIERATION_BACKTRACE_MESSAGE;

        RubyException ex = RubyStopIteration.newInstance(context, result, message);

        if (!RubyInstanceConfig.STOPITERATION_BACKTRACE) {
            ex.forceBacktrace(disabledBacktrace());
        }

        return ex.toThrowable();
    }

    @Deprecated
    public RaiseException newLightweightStopIterationError(String message) {
        return newStopIteration(null, message);
    }

    private IRubyObject disabledBacktrace() {
        return RubyArray.newEmptyArray(this);
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

    public IRubyObject getReportOnException() {
        return reportOnException;
    }

    public void setReportOnException(IRubyObject enable) {
        reportOnException = enable;
    }

    public boolean isDoNotReverseLookupEnabled() {
        return doNotReverseLookupEnabled;
    }

    public void setDoNotReverseLookupEnabled(boolean b) {
        doNotReverseLookupEnabled = b;
    }

    private final ThreadLocal<Map<Object, Object>> inspect = new ThreadLocal<>();
    public void registerInspecting(Object obj) {
        Map<Object, Object> val = inspect.get();
        if (val == null) inspect.set(val = new IdentityHashMap<>(8));
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

    public boolean isObjectSpaceEnabled() {
        return objectSpaceEnabled;
    }

    public void setObjectSpaceEnabled(boolean objectSpaceEnabled) {
        this.objectSpaceEnabled = objectSpaceEnabled;
    }

    // You cannot set siphashEnabled property except via RubyInstanceConfig to avoid mixing hash functions.
    public boolean isSiphashEnabled() {
        return siphashEnabled;
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

    @Deprecated
    public boolean is2_0() {
        return true;
    }

    /** GET_VM_STATE_VERSION */
    @Deprecated // not used
    public long getGlobalState() {
        synchronized(this) {
            return globalState;
        }
    }

    /** INC_VM_STATE_VERSION */
    @Deprecated // not used
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

    public ExecutorService getExecutor() {
        return executor;
    }

    public ExecutorService getFiberExecutor() {
        return fiberExecutor;
    }

    public Map<String, DateTimeZone> getTimezoneCache() {
        return timeZoneCache;
    }

    @Deprecated
    public int getConstantGeneration() {
        return -1;
    }

    public Invalidator getConstantInvalidator(String constantName) {
        Invalidator invalidator = constantNameInvalidators.get(constantName);
        if (invalidator != null) {
            return invalidator;
        } else {
            return addConstantInvalidator(constantName);
        }
    }

    private Invalidator addConstantInvalidator(String constantName) {
        final Invalidator invalidator = OptoFactory.newConstantInvalidator(this);
        constantNameInvalidators.putIfAbsent(constantName, invalidator);

        // fetch the invalidator back from the ConcurrentHashMap to ensure that
        // only one invalidator for a given constant name is ever used:
        return constantNameInvalidators.get(constantName);
    }

    public Invalidator getCheckpointInvalidator() {
        return checkpointInvalidator;
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
     * @return a SelectorPool from which to getService Selector instances
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

    public List<StrptimeToken> getCachedStrptimePattern(String pattern) {
        List<StrptimeToken> tokens = strptimeFormatCache.get(pattern);

        if (tokens == null) {
            tokens = new StrptimeParser().compilePattern(pattern);
            strptimeFormatCache.put(pattern, tokens);
        }

        return tokens;
    }

    /**
     * Add a method, so it can be printed out later.
     *
     * @param id raw name String of the method to be profiled
     * @param method
     */
    void addProfiledMethod(final String id, final DynamicMethod method) {
        if (!config.isProfiling() || method.isUndefined()) return;

        getProfilingService().addProfiledMethod(id, method);
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
     * Increment the count of backtraces generated for warnings in this runtime.
     */
    public void incrementWarningCount() {
        warningCount.incrementAndGet();
    }

    /**
     * Get the current backtrace count.
     *
     * @return the current backtrace count
     */
    public int getWarningCount() {
        return warningCount.get();
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
     * Mark Fixnum as reopened
     */
    public void reopenFixnum() {
        fixnumInvalidator.invalidate();
        fixnumReopened = true;
    }

    /**
     * Retrieve the invalidator for Fixnum reopening
     */
    public Invalidator getFixnumInvalidator() {
        return fixnumInvalidator;
    }

    /**
     * Whether the Float class has been reopened and modified
     */
    public boolean isFixnumReopened() {
        return fixnumReopened;
    }

    /**
     * Mark Float as reopened
     */
    public void reopenFloat() {
        floatInvalidator.invalidate();
        floatReopened = true;
    }

    /**
     * Retrieve the invalidator for Float reopening
     */
    public Invalidator getFloatInvalidator() {
        return floatInvalidator;
    }

    /**
     * Whether the Float class has been reopened and modified
     */
    public boolean isFloatReopened() {
        return floatReopened;
    }

    public boolean isBootingCore() {
        return bootingCore;
    }

    public boolean isBooting() {
        return bootingRuntime;
    }

    public CoverageData getCoverageData() {
        return coverageData;
    }

    /**
     * @deprecated internal API, to be removed
     */
    public Random getRandom() {
        return random;
    }

    public long getHashSeedK0() {
        return hashSeedK0;
    }

    public long getHashSeedK1() {
        return hashSeedK1;
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

    public RubyString getDefinedMessage(DefinedMessage definedMessage) {
        return definedMessages.get(definedMessage);
    }

    public RubyString getThreadStatus(RubyThread.Status status) {
        return threadStatuses.get(status);
    }

    /**
     * Given a Ruby string, cache a frozen, duplicated copy of it, or find an
     * existing copy already prepared. This is used to reduce in-memory
     * duplication of pre-frozen or known-frozen strings.
     *
     * Note that this cache does some sync against the Ruby instance. This
     * could cause contention under heavy concurrent load, so a reexamination
     * of this design might be warranted.
     *
     * @param string the string to freeze-dup if an equivalent does not already exist
     * @return the freeze-duped version of the string
     */
    public RubyString freezeAndDedupString(RubyString string) {
        if (string.getMetaClass() != stringClass) {
            // never cache a non-natural String
            RubyString duped = string.strDup(this);
            duped.setFrozen(true);
            return duped;
        }

        WeakReference<RubyString> dedupedRef = dedupMap.get(string);
        RubyString deduped;

        if (dedupedRef == null || (deduped = dedupedRef.get()) == null) {
            // Never use incoming value as key
            deduped = string.strDup(this);
            deduped.setFrozen(true);

            final WeakReference<RubyString> weakref = new WeakReference<>(deduped);

            // try to insert new
            dedupedRef = dedupMap.computeIfAbsent(deduped, key -> weakref);
            if (dedupedRef == null) return deduped;

            // entry exists, return result if not vacated
            RubyString unduped = dedupedRef.get();
            if (unduped != null) return unduped;

            // ref is there but vacated, try to replace it until we have a result
            while (true) {
                dedupedRef = dedupMap.computeIfPresent(string, (key, old) -> old.get() == null ? weakref : old);

                // return result if not vacated
                unduped = dedupedRef.get();
                if (unduped != null) return unduped;
            }
        } else if (deduped.getEncoding() != string.getEncoding()) {
            // if encodings don't match, new string loses; can't dedup
            // FIXME: This may never happen, if we are properly considering encoding in RubyString.hashCode
            deduped = string.strDup(this);
            deduped.setFrozen(true);
        }

        return deduped;
    }

    public int getRuntimeNumber() {
        return runtimeNumber;
    }

    /**
     * @see org.jruby.compiler.Constantizable
     */
    @Override
    public Object constant() {
        return constant;
    }

    /**
     * Set the base Class#new method.
     *
     * @param baseNewMethod
     */
    public void setBaseNewMethod(DynamicMethod baseNewMethod) {
        this.baseNewMethod = baseNewMethod;
    }

    /**
     * Get the base Class#new method.
     *
     * @return the base Class#new method
     */
    public DynamicMethod getBaseNewMethod() {
        return baseNewMethod;
    }

    /**
     * Get the "nullToNil" method handle filter for this runtime.
     *
     * @return a method handle suitable for filtering a single IRubyObject value from null to nil
     */
    public MethodHandle getNullToNilHandle() {
        MethodHandle nullToNil = this.nullToNil;

        if (nullToNil != null) return nullToNil;

        nullToNil = InvokeDynamicSupport.findStatic(Helpers.class, "nullToNil", methodType(IRubyObject.class, IRubyObject.class, IRubyObject.class));
        nullToNil = insertArguments(nullToNil, 1, nilObject);
        nullToNil = explicitCastArguments(nullToNil, methodType(IRubyObject.class, Object.class));

        return this.nullToNil = nullToNil;
    }

    // Parser stats methods
    private void addLoadParseToStats() {
        if (parserStats != null) parserStats.addLoadParse();
    }

    private void addEvalParseToStats() {
        if (parserStats != null) parserStats.addEvalParse();
    }

    public FilenoUtil getFilenoUtil() {
        return filenoUtil;
    }

    public RubyClass getData() {
        return dataClass;
    }

    /**
     * @return Class -> extension initializer map
     * @note Internal API, subject to change!
     */
    public Map<Class, Consumer<RubyModule>> getJavaExtensionDefinitions() { return javaExtensionDefinitions; }

    @Deprecated
    private static final RecursiveFunctionEx<RecursiveFunction> LEGACY_RECURSE = new RecursiveFunctionEx<RecursiveFunction>() {
        @Override
        public IRubyObject call(ThreadContext context, RecursiveFunction func, IRubyObject obj, boolean recur) {
            return func.call(obj, recur);
        }
    };

    @Deprecated
    public int getSafeLevel() {
        return 0;
    }

    @Deprecated
    public void setSafeLevel(int safeLevel) {
    }

    @Deprecated
    public void checkSafeString(IRubyObject object) {
    }

    @Deprecated
    public void secure(int level) {
    }

    @Deprecated
    public RaiseException newNameErrorObject(String message, IRubyObject name) {
        RubyException error = new RubyNameError(this, getNameError(), message, name);

        return error.toThrowable();
    }

    @Deprecated
    public boolean is1_8() {
        return false;
    }

    @Deprecated
    public boolean is1_9() {
        return true;
    }

    @Deprecated
    public IRubyObject safeRecurse(RecursiveFunction func, IRubyObject obj, String name, boolean outer) {
        return safeRecurse(LEGACY_RECURSE, getCurrentContext(), func, obj, name, outer);
    }

    @Deprecated
    public ProfiledMethods getProfiledMethods() {
        return new ProfiledMethods(this);
    }

    public interface RecursiveFunctionEx<T> extends ThreadContext.RecursiveFunctionEx<T> {
        IRubyObject call(ThreadContext context, T state, IRubyObject obj, boolean recur);
    }

    @Deprecated
    public interface RecursiveFunction extends MRIRecursionGuard.RecursiveFunction {}

    /**
     * @deprecated Use ThreadContext.safeRecurse
     */
    @Deprecated
    public <T> IRubyObject safeRecurse(RecursiveFunctionEx<T> func, ThreadContext context, T state, IRubyObject obj, String name, boolean outer) {
        return context.safeRecurse(func, state, obj, name, outer);
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
    @Deprecated
    public IRubyObject execRecursive(RecursiveFunction func, IRubyObject obj) {
        return oldRecursionGuard().execRecursive(func, obj);
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
     * @deprecated Use ThreadContext.safeRecurse.
     *
     * @param func
     * @param obj
     * @return
     */
    @Deprecated
    public IRubyObject execRecursiveOuter(RecursiveFunction func, IRubyObject obj) {
        return oldRecursionGuard().execRecursiveOuter(func, obj);
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
    @Deprecated
    public <T extends IRubyObject> T recursiveListOperation(Callable<T> body) {
        return oldRecursionGuard().recursiveListOperation(body);
    }

    @Deprecated
    private MRIRecursionGuard oldRecursionGuard() {
        MRIRecursionGuard mriRecursionGuard = this.mriRecursionGuard;
        if (mriRecursionGuard != null) return mriRecursionGuard;

        synchronized (this) {
            mriRecursionGuard = this.mriRecursionGuard;
            if (mriRecursionGuard != null) return mriRecursionGuard;
            return mriRecursionGuard = new MRIRecursionGuard(this);
        }
    }

    private final ConcurrentHashMap<String, Invalidator> constantNameInvalidators =
        new ConcurrentHashMap<String, Invalidator>(
            16    /* default initial capacity */,
            0.75f /* default load factory */,
            1     /* concurrency level - mostly reads here so this can be 1 */);

    private final Invalidator checkpointInvalidator;
    private final ThreadService threadService;

    private final POSIX posix;

    private final ObjectSpace objectSpace = new ObjectSpace();

    private final RubySymbol.SymbolTable symbolTable = new RubySymbol.SymbolTable(this);

    private static final EventHook[] EMPTY_HOOKS = new EventHook[0];
    private volatile EventHook[] eventHooks = EMPTY_HOOKS;
    private boolean hasEventHooks;
    private boolean globalAbortOnExceptionEnabled = false;
    private IRubyObject reportOnException;
    private boolean doNotReverseLookupEnabled = false;
    private volatile boolean objectSpaceEnabled;
    private boolean siphashEnabled;

    private long globalState = 1;

    // Default objects
    private IRubyObject topSelf;
    private IRubyObject rootFiber;
    private RubyNil nilObject;
    private IRubyObject[] singleNilArray;
    private RubyBoolean trueObject;
    private RubyBoolean falseObject;
    final RubyFixnum[] fixnumCache = new RubyFixnum[2 * RubyFixnum.CACHE_OFFSET];
    final Object[] fixnumConstants = new Object[fixnumCache.length];

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
            complexClass, rationalClass, enumeratorClass, yielderClass, fiberClass, generatorClass,
            arrayClass, hashClass, rangeClass, stringClass, encodingClass, converterClass, symbolClass,
            procClass, bindingClass, methodClass, unboundMethodClass,
            matchDataClass, regexpClass, timeClass, bignumClass, dirClass,
            fileClass, fileStatClass, ioClass, threadClass, threadGroupClass,
            continuationClass, structClass, tmsStruct, passwdStruct,
            groupStruct, procStatusClass, exceptionClass, runtimeError, frozenError, ioError,
            scriptError, nameError, nameErrorMessage, noMethodError, signalException,
            rangeError, dummyClass, systemExit, localJumpError, nativeException,
            systemCallError, fatal, interrupt, typeError, argumentError, uncaughtThrowError, indexError, stopIteration,
            syntaxError, standardError, loadError, notImplementedError, securityError, noMemoryError,
            regexpError, eofError, threadError, concurrencyError, systemStackError, zeroDivisionError, floatDomainError, mathDomainError,
            encodingError, encodingCompatibilityError, converterNotFoundError, undefinedConversionError,
            invalidByteSequenceError, fiberError, randomClass, keyError, locationClass, interruptedRegexpError, dataClass;

    /**
     * All the core modules we keep direct references to, for quick access and
     * to ensure they remain available.
     */
    private RubyModule
            kernelModule, comparableModule, enumerableModule, mathModule,
            marshalModule, etcModule, fileTestModule, gcModule,
            objectSpaceModule, processModule, procUIDModule, procGIDModule,
            procSysModule, precisionModule, errnoModule, warningModule;

    private DynamicMethod privateMethodMissing, protectedMethodMissing, variableMethodMissing,
            superMethodMissing, normalMethodMissing, defaultMethodMissing, defaultModuleMethodMissing,
            respondTo, respondToMissing;

    // record separator var, to speed up io ops that use it
    private GlobalVariable recordSeparatorVar;

    // former java.lang.System concepts now internalized for MVM
    private volatile String currentDirectory;

    // The "current line" global variable
    private volatile int currentLine = 0;

    private volatile IRubyObject argsFile;

    private final long startTime = System.currentTimeMillis();

    final RubyInstanceConfig config;

    private InputStream in;
    private PrintStream out;
    private PrintStream err;

    // Java support
    private JavaSupport javaSupport;
    private final JRubyClassLoader jrubyClassLoader;

    // Management/monitoring
    private BeanManager beanManager;

    // Parser stats
    private ParserStats parserStats;

    // Compilation
    private final JITCompiler jitCompiler;

    // Cache invalidation
    private final Caches caches;

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

    private Encoding defaultInternalEncoding, defaultExternalEncoding, defaultFilesystemEncoding;
    private EncodingService encodingService;

    private GlobalVariables globalVariables = new GlobalVariables(this);
    private final RubyWarnings warnings = new RubyWarnings(this);
    private final WarnCallback regexpWarnings = new WarnCallback() {
        @Override
        public void warn(String message) {
            getWarnings().warning(message);
        }
    };

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

    // A thread pool to use for running fibers
    private ExecutorService fiberExecutor;

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

    // Message for Errno exceptions that will not generate a backtrace
    public static final String ERRNO_BACKTRACE_MESSAGE = "errno backtraces disabled; run with -Xerrno.backtrace=true to enable";

    // Message for Errno exceptions that will not generate a backtrace
    public static final String STOPIERATION_BACKTRACE_MESSAGE = "StopIteration backtraces disabled; run with -Xstop_iteration.backtrace=true to enable";

    // Count of RaiseExceptions generated by code running in this runtime
    private final AtomicInteger exceptionCount = new AtomicInteger();

    // Count of exception backtraces generated by code running in this runtime
    private final AtomicInteger backtraceCount = new AtomicInteger();

    // Count of Kernel#caller backtraces generated by code running in this runtime
    private final AtomicInteger callerCount = new AtomicInteger();

    // Count of built-in warning backtraces generated by code running in this runtime
    private final AtomicInteger warningCount = new AtomicInteger();

    private Invalidator
            fixnumInvalidator = OptoFactory.newGlobalInvalidator(0),
            floatInvalidator = OptoFactory.newGlobalInvalidator(0);
    private boolean fixnumReopened, floatReopened;

    private volatile boolean bootingCore = true;
    private volatile boolean bootingRuntime = true;

    private RubyHash envObject;

    private final CoverageData coverageData = new CoverageData();

    /** The "global" runtime. Set to the first runtime created, normally. */
    private static volatile Ruby globalRuntime;

    /** The "thread local" runtime. Set to the global runtime if unset. */
    private static ThreadLocal<Ruby> threadLocalRuntime = new ThreadLocal<Ruby>();

    /** The runtime-local random number generator. Uses SecureRandom if permissions allow. */
    final Random random;

    /** The runtime-local seed for hash randomization */
    private final long hashSeedK0;
    private final long hashSeedK1;

    private StaticScopeFactory staticScopeFactory;

    private IRManager irManager;

    private FFI ffi;

    private JavaProxyClassFactory javaProxyClassFactory;

    /** Used to find the ProfilingService implementation to use. If profiling is disabled it's null */
    private final ProfilingServiceLookup profilingServiceLookup;

    private EnumMap<DefinedMessage, RubyString> definedMessages = new EnumMap<DefinedMessage, RubyString>(DefinedMessage.class);
    private EnumMap<RubyThread.Status, RubyString> threadStatuses = new EnumMap<RubyThread.Status, RubyString>(RubyThread.Status.class);

    public interface ObjectSpacer {
        void addToObjectSpace(Ruby runtime, boolean useObjectSpace, IRubyObject object);
    }

    private static final ObjectSpacer DISABLED_OBJECTSPACE = new ObjectSpacer() {
        @Override
        public void addToObjectSpace(Ruby runtime, boolean useObjectSpace, IRubyObject object) {
        }
    };

    private static final ObjectSpacer ENABLED_OBJECTSPACE = new ObjectSpacer() {
        @Override
        public void addToObjectSpace(Ruby runtime, boolean useObjectSpace, IRubyObject object) {
            if (useObjectSpace) runtime.objectSpace.add(object);
        }
    };

    private final ObjectSpacer objectSpacer;

    public void addToObjectSpace(boolean useObjectSpace, IRubyObject object) {
        objectSpacer.addToObjectSpace(this, useObjectSpace, object);
    }

    private RubyArray emptyFrozenArray;

    /**
     * A map from Ruby string data to a pre-frozen global version of that string.
     *
     * Access must be synchronized.
     */
    private ConcurrentHashMap<RubyString, WeakReference<RubyString>> dedupMap = new ConcurrentHashMap<>();

    private static final AtomicInteger RUNTIME_NUMBER = new AtomicInteger(0);
    private final int runtimeNumber = RUNTIME_NUMBER.getAndIncrement();

    private final Config configBean;
    private final org.jruby.management.Runtime runtimeBean;

    private final FilenoUtil filenoUtil;

    private final Interpreter interpreter = new Interpreter();

    /**
     * A representation of this runtime as a JIT-optimizable constant. Used for e.g. invokedynamic binding of runtime
     * accesses.
     */
    private final Object constant;

    /**
     * The built-in Class#new method, so we can bind more directly to allocate and initialize.
     */
    private DynamicMethod baseNewMethod;

    /**
     * The nullToNil filter for this runtime.
     */
    private MethodHandle nullToNil;

    public final ClassValue<TypePopulator> POPULATORS = new ClassValue<TypePopulator>() {
        @Override
        protected TypePopulator computeValue(Class<?> type) {
            return RubyModule.loadPopulatorFor(type);
        }
    };

    public final JavaSites sites = new JavaSites();

    private volatile MRIRecursionGuard mriRecursionGuard;

    private final Map<Class, Consumer<RubyModule>> javaExtensionDefinitions = new WeakHashMap<>(); // caller-syncs

    // For strptime processing we cache the parsed format strings since most applications
    // reuse the same formats over and over.  This is also unbounded (for now) since all applications
    // I know of use very few of them.  Even if there are many the size of these lists are modest.
    private final Map<String, List<StrptimeToken>> strptimeFormatCache = new ConcurrentHashMap<>();

    transient RubyString tzVar;

    @Deprecated
    private void setNetworkStack() {
        deprecatedNetworkStackProperty();
    }

    @SuppressWarnings("deprecation")
    private void deprecatedNetworkStackProperty() {
        if (Options.PREFER_IPV4.load()) {
            LOG.warn("Warning: not setting network stack system property because socket subsystem may already be booted."
                    + "If you need this option please set it manually as a JVM property.\n"
                    + "Use JAVA_OPTS=-Djava.net.preferIPv4Stack=true OR prepend -J as a JRuby option.");
        }
    }

}
