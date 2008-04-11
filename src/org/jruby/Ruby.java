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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jruby.anno.TypePopulator;
import org.jruby.ast.Node;
import org.jruby.ast.executable.RubiniusRunner;
import org.jruby.ast.executable.Script;
import org.jruby.ast.executable.YARVCompiledRunner;
import org.jruby.common.RubyWarnings;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.compiler.ASTCompiler;
import org.jruby.compiler.ASTInspector;
import org.jruby.compiler.NotCompilableException;
import org.jruby.compiler.impl.StandardASMCompiler;
import org.jruby.compiler.yarv.StandardYARVCompiler;
import org.jruby.evaluator.ASTInterpreter;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.ext.JRubyPOSIXHandler;
import org.jruby.ext.LateLoadingLibrary;
import org.jruby.ext.posix.POSIX;
import org.jruby.ext.posix.POSIXFactory;
import org.jruby.internal.runtime.GlobalVariables;
import org.jruby.internal.runtime.ThreadService;
import org.jruby.internal.runtime.ValueAccessor;
import org.jruby.javasupport.JavaSupport;
import org.jruby.parser.Parser;
import org.jruby.parser.ParserConfiguration;
import org.jruby.runtime.Binding;
import org.jruby.runtime.Block;
import org.jruby.runtime.CacheMap;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.EventHook;
import org.jruby.runtime.Frame;
import org.jruby.runtime.GlobalVariable;
import org.jruby.runtime.IAccessor;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ObjectSpace;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;
import org.jruby.runtime.load.LoadService;
import org.jruby.util.BuiltinScript;
import org.jruby.util.ByteList;
import org.jruby.util.IOInputStream;
import org.jruby.util.IOOutputStream;
import org.jruby.util.JRubyClassLoader;
import org.jruby.util.JavaNameMangler;
import org.jruby.util.KCode;
import org.jruby.util.SafePropertyAccessor;
import org.jruby.util.collections.WeakHashSet;
import org.jruby.util.io.ChannelDescriptor;

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
        if (RUNTIME_THREADLOCAL) {
            setCurrentInstance(ruby);
        }
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
        if(config.isSamplingEnabled()) {
            org.jruby.util.SimpleSampler.registerThreadContext(threadService.getCurrentContext());
        }

        this.in                 = config.getInput();
        this.out                = config.getOutput();
        this.err                = config.getError();
        this.objectSpaceEnabled = config.isObjectSpaceEnabled();
        this.profile            = config.getProfile();
        this.currentDirectory   = config.getCurrentDirectory();
        this.kcode              = config.getKCode();
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
        Node node = parseEval(script, "<script>", context.getCurrentScope(), 0);
        
        try {
            return ASTInterpreter.eval(this, context, node, context.getFrameSelf(), Block.NULL_BLOCK);
        } catch (JumpException.ReturnJump rj) {
            throw newLocalJumpError("return", (IRubyObject)rj.getValue(), "unexpected return");
        } catch (JumpException.BreakJump bj) {
            throw newLocalJumpError("break", (IRubyObject)bj.getValue(), "unexpected break");
        } catch (JumpException.RedoJump rj) {
            throw newLocalJumpError("redo", (IRubyObject)rj.getValue(), "unexpected redo");
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
        byte[] bytes;
        
        try {
            bytes = script.getBytes(KCode.NONE.getKCode());
        } catch (UnsupportedEncodingException e) {
            bytes = script.getBytes();
        }

        Node node = parseInline(new ByteArrayInputStream(bytes), filename, null);
        Frame frame = getCurrentContext().getCurrentFrame(); 
        
        frame.setFile(node.getPosition().getFile());
        frame.setLine(node.getPosition().getStartLine());
        return runNormally(node, false);
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

        
        if(config.isYARVEnabled()) {
            new YARVCompiledRunner(this, inputStream, filename).run();
        } else if(config.isRubiniusEnabled()) {
            new RubiniusRunner(this, inputStream, filename).run();
        } else {
            Node scriptNode = parseFromMain(inputStream, filename);
            Frame frame = getCurrentContext().getCurrentFrame(); 
            
            frame.setFile(scriptNode.getPosition().getFile());
            frame.setLine(scriptNode.getPosition().getStartLine());

            if (config.isAssumePrinting() || config.isAssumeLoop()) {
                runWithGetsLoop(scriptNode, config.isAssumePrinting(), config.isProcessLineEnds(),
                        config.isSplit(), config.isYARVCompileEnabled());
            } else {
                runNormally(scriptNode, config.isYARVCompileEnabled());
            }
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
            return parseFile(inputStream, filename, getCurrentContext().getCurrentScope());
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
     * @param yarvCompile Whether to compile the target script to YARV (Ruby 1.9)
     * bytecode before executing.
     * @return The result of executing the specified script
     */
    public IRubyObject runWithGetsLoop(Node scriptNode, boolean printing, boolean processLineEnds, boolean split, boolean yarvCompile) {
        ThreadContext context = getCurrentContext();
        
        Script script = null;
        YARVCompiledRunner runner = null;
        boolean compile = getInstanceConfig().getCompileMode().shouldPrecompileCLI();
        if (compile || !yarvCompile) {
            script = tryCompile(scriptNode);
            if (compile && script == null) {
                // terminate; tryCompile will have printed out an error and we're done
                return getNil();
            }
        } else if (yarvCompile) {
            runner = tryCompileYarv(scriptNode);
        }
        
        if (processLineEnds) {
            getGlobalVariables().set("$\\", getGlobalVariables().get("$/"));
        }
        
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
                        runScript(script);
                    } else if (runner != null) {
                        runYarv(runner);
                    } else {
                        runInterpreter(scriptNode);
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
        
        return getNil();
    }
    
    /**
     * Run the specified script without any of the loop-processing wrapper
     * code.
     * 
     * @param scriptNode The root node of the script to be executed
     * @param yarvCompile Whether to compile the script to YARV (Ruby 1.9)
     * bytecode before execution
     * @return The result of executing the script
     */
    public IRubyObject runNormally(Node scriptNode, boolean yarvCompile) {
        Script script = null;
        YARVCompiledRunner runner = null;
        boolean compile = getInstanceConfig().getCompileMode().shouldPrecompileCLI();
        boolean forceCompile = getInstanceConfig().getCompileMode().shouldPrecompileAll();
        if (compile) {
            script = tryCompile(scriptNode);
            if (forceCompile && script == null) {
                System.err.println("Error, could not compile; pass -J-Djruby.jit.logging.verbose=true for more details");
                return getNil();
            }
        } else if (yarvCompile) {
            runner = tryCompileYarv(scriptNode);
        }
        
        if (script != null) {
            return runScript(script);
        } else if (runner != null) {
            return runYarv(runner);
        } else {
            return runInterpreter(scriptNode);
        }
    }
    
    private Script tryCompile(Node node) {
        return tryCompile(node, new JRubyClassLoader(getJRubyClassLoader()));
    }
    
    private Script tryCompile(Node node, JRubyClassLoader classLoader) {
        Script script = null;
        try {
            String filename = node.getPosition().getFile();
            String classname = JavaNameMangler.mangledFilenameForStartupClasspath(filename);

            ASTInspector inspector = new ASTInspector();
            inspector.inspect(node);

            StandardASMCompiler asmCompiler = new StandardASMCompiler(classname, filename);
            ASTCompiler compiler = new ASTCompiler();
            compiler.compileRoot(node, asmCompiler, inspector);
            script = (Script)asmCompiler.loadClass(classLoader).newInstance();

            if (config.isJitLogging()) {
                System.err.println("compiled: " + node.getPosition().getFile());
            }
        } catch (NotCompilableException nce) {
            if (config.isJitLoggingVerbose()) {
                System.err.println("Error -- Not compileable: " + nce.getMessage());
                nce.printStackTrace();
            }
        } catch (ClassNotFoundException e) {
            if (config.isJitLoggingVerbose()) {
                System.err.println("Error -- Not compileable: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (InstantiationException e) {
            if (config.isJitLoggingVerbose()) {
                System.err.println("Error -- Not compileable: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (IllegalAccessException e) {
            if (config.isJitLoggingVerbose()) {
                System.err.println("Error -- Not compileable: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Throwable t) {
            if (config.isJitLoggingVerbose()) {
                System.err.println("could not compile: " + node.getPosition().getFile() + " because of: \"" + t.getMessage() + "\"");
                t.printStackTrace();
            }
        }
        
        return script;
    }
    
    private YARVCompiledRunner tryCompileYarv(Node node) {
        try {
            StandardYARVCompiler compiler = new StandardYARVCompiler(this);
            ASTCompiler.getYARVCompiler().compile(node, compiler);
            org.jruby.lexer.yacc.ISourcePosition p = node.getPosition();
            if(p == null && node instanceof org.jruby.ast.RootNode) {
                p = ((org.jruby.ast.RootNode)node).getBodyNode().getPosition();
            }
            return new YARVCompiledRunner(this,compiler.getInstructionSequence("<main>",p.getFile(),"toplevel"));
        } catch (NotCompilableException nce) {
            System.err.println("Error -- Not compileable: " + nce.getMessage());
            return null;
        } catch (JumpException.ReturnJump rj) {
            return null;
        }
    }
    
    private IRubyObject runScript(Script script) {
        ThreadContext context = getCurrentContext();
        
        try {
            return script.load(context, context.getFrameSelf(), IRubyObject.NULL_ARRAY, Block.NULL_BLOCK);
        } catch (JumpException.ReturnJump rj) {
            return (IRubyObject) rj.getValue();
        }
    }
    
    private IRubyObject runYarv(YARVCompiledRunner runner) {
        try {
            return runner.run();
        } catch (JumpException.ReturnJump rj) {
            return (IRubyObject) rj.getValue();
        }
    }
    
    private IRubyObject runInterpreter(Node scriptNode) {
        ThreadContext context = getCurrentContext();
        
        try {
            return ASTInterpreter.eval(this, context, scriptNode, getTopSelf(), Block.NULL_BLOCK);
        } catch (JumpException.ReturnJump rj) {
            return (IRubyObject) rj.getValue();
        }
    }

    /**
     * @deprecated use #newInstance()
     */
    public static Ruby getDefaultInstance() {
        return newInstance();
    }
    
    public static Ruby getCurrentInstance() {
        return currentRuntime.get();
    }
    
    public static void setCurrentInstance(Ruby runtime) {
        currentRuntime.set(runtime);
    }
    
    public int allocSymbolId() {
        return symbolLastId.incrementAndGet();
    }
    public int allocModuleId() {
        return moduleLastId.incrementAndGet();
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

    /**
     * Retrieve the module with the given name from the Object namespace. The
     * module name must be an interned string, but this method will be faster
     * than the non-interned version.
     * 
     * @param internedName The name of the module; <em>must</em> be an interned String
     * @return The module or null if not found
     */
    public RubyModule fastGetModule(String internedName) {
        return (RubyModule) objectClass.fastGetConstantAt(internedName);
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
    public RubyClass fastGetClass(String internedName) {
        return objectClass.fastGetClass(internedName);
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
            warnings.warn(ID.NO_SUPER_CLASS, "no super class for `" + className + "', Object assumed", className);
            
            superClass = objectClass;
        }

        return RubyClass.newClass(this, superClass, name, allocator, parent, !parentIsObject);
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
     * @see org.jruby.Ruby#setSaveLevel
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
     * The safe level is set using $SAFE in Ruby code. It is not particularly
     * well supported in JRuby.
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

    // FIXME moved this hear to get what's obviously a utility method out of IRubyObject.
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
     * A ThreadFactory for when we're using pooled threads; we want to create
     * the threads with daemon = true so they don't keep us from shutting down.
     */
    public static class DaemonThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            
            return thread;
        }
    }

    /** 
     * This method is called immediately after constructing the Ruby instance.
     * The main thread is prepared for execution, all core classes and libraries
     * are initialized, and any libraries required on the command line are
     * loaded.
     */
    private void init() {
        // Get the main threadcontext (gets constructed for us)
        ThreadContext tc = getCurrentContext();

        safeLevel = config.getSafeLevel();
        
        // Construct key services
        loadService = config.createLoadService(this);
        posix = POSIXFactory.getPOSIX(new JRubyPOSIXHandler(this), RubyInstanceConfig.nativeEnabled);
        javaSupport = new JavaSupport(this);
        
        if (config.POOLING_ENABLED) {
            Executors.newCachedThreadPool();
            executor = new ThreadPoolExecutor(
                    RubyInstanceConfig.POOL_MIN,
                    RubyInstanceConfig.POOL_MAX,
                    RubyInstanceConfig.POOL_TTL,
                    TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(),
                    new DaemonThreadFactory());
        }
        
        // initialize the root of the class hierarchy completely
        initRoot();

        // Construct the top-level execution frame and scope for the main thread
        tc.prepareTopLevel(objectClass, topSelf);

        // Initialize all the core classes
        bootstrap();
        
        // Create global constants and variables
        RubyGlobal.createGlobals(this);

        // Prepare LoadService and load path
        getLoadService().init(config.loadPaths());

        // initialize builtin libraries
        initBuiltins();
        
        // Require in all libraries specified on command line
        for (String scriptName : config.requiredLibraries()) {
            RubyKernel.require(getTopSelf(), newString(scriptName), Block.NULL_BLOCK);
        }
    }

    private void bootstrap() {
        undef = new RubyUndef();
        
        initCore();
        initExceptions();
    }

    private void initRoot() {
        // Bootstrap the top of the hierarchy
        objectClass = RubyClass.createBootstrapClass(this, "Object", null, RubyObject.OBJECT_ALLOCATOR);
        moduleClass = RubyClass.createBootstrapClass(this, "Module", objectClass, RubyModule.MODULE_ALLOCATOR);
        classClass = RubyClass.createBootstrapClass(this, "Class", moduleClass, RubyClass.CLASS_ALLOCATOR);

        objectClass.setMetaClass(classClass);
        moduleClass.setMetaClass(classClass);
        classClass.setMetaClass(classClass);

        RubyClass metaClass;
        metaClass = objectClass.makeMetaClass(classClass);
        metaClass = moduleClass.makeMetaClass(metaClass);
        metaClass = classClass.makeMetaClass(metaClass);

        RubyObject.createObjectClass(this, objectClass);
        RubyModule.createModuleClass(this, moduleClass);
        RubyClass.createClassClass(this, classClass);
        
        // set constants now that they're initialized
        objectClass.setConstant("Object", objectClass);
        objectClass.setConstant("Class", classClass);
        objectClass.setConstant("Module", moduleClass);

        // Initialize Kernel and include into Object
        RubyKernel.createKernelModule(this);
        objectClass.includeModule(kernelModule);

        // Object is ready, create top self
        topSelf = TopSelfFactory.createTopSelf(this);
    }

    private void initCore() {
        // Pre-create all the core classes potentially referenced during startup
        RubyNil.createNilClass(this);
        RubyBoolean.createFalseClass(this);
        RubyBoolean.createTrueClass(this);

        nilObject = new RubyNil(this);
        falseObject = new RubyBoolean(this, false);
        trueObject = new RubyBoolean(this, true);

        RubyComparable.createComparable(this);
        RubyEnumerable.createEnumerableModule(this);
        RubyString.createStringClass(this);
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
        if (profile.allowModule("Precision")) {
            RubyPrecision.createPrecisionModule(this);
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
        if (profile.allowClass("Data")) {
            defineClass("Data", objectClass, objectClass.getAllocator());
        }
        if (!isSecurityRestricted()) {
            // Signal uses sun.misc.* classes, this is not allowed
            // in the security-sensitive environments
            if (profile.allowModule("Signal")) {
                RubySignal.createSignal(this);
            }
        }
        if (profile.allowClass("Continuation")) {
            RubyContinuation.createContinuation(this);
        }
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
        }
        if (profile.allowClass("NoMethodError")) {
            RubyNoMethodError.createNoMethodErrorClass(this, nameError);
        }
        if (profile.allowClass("SystemExit")) {
            RubySystemExit.createSystemExitClass(this, exceptionClass);
        }
        if (profile.allowClass("LocalJumpError")) {
            RubyLocalJumpError.createLocalJumpErrorClass(this, standardError);
        }
        if (profile.allowClass("NativeException")) {
            NativeException.createClass(this, runtimeError);
        }
        if (profile.allowClass("SystemCallError")) {
            systemCallError = RubySystemCallError.createSystemCallErrorClass(this, standardError);
        }
        
        defineClassIfAllowed("Fatal", exceptionClass);
        defineClassIfAllowed("Interrupt", signalException);
        defineClassIfAllowed("TypeError", standardError);
        defineClassIfAllowed("ArgumentError", standardError);
        defineClassIfAllowed("IndexError", standardError);
        defineClassIfAllowed("SyntaxError", scriptError);
        defineClassIfAllowed("LoadError", scriptError);
        defineClassIfAllowed("NotImplementedError", scriptError);
        defineClassIfAllowed("SecurityError", standardError);
        defineClassIfAllowed("NoMemoryError", exceptionClass);
        defineClassIfAllowed("RegexpError", standardError);
        defineClassIfAllowed("EOFError", ioError);
        defineClassIfAllowed("ThreadError", standardError);
        defineClassIfAllowed("SystemStackError", standardError);
        defineClassIfAllowed("ZeroDivisionError", standardError);
        defineClassIfAllowed("FloatDomainError", rangeError);
        
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

            Field[] fields = IErrno.class.getFields();

            for (int i = 0; i < fields.length; i++) {
                try {
                    createSysErr(fields[i].getInt(IErrno.class), fields[i].getName());
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Someone defined a non-public constant in IErrno.java", e);
                }
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
            RubyClass errno = errnoModule.defineClassUnder(name, systemCallError, systemCallError.getAllocator());
            errnos.put(i, errno);
            errno.defineConstant("Errno", newFixnum(i));
        }
    }

    private void initBuiltins() {
        addLazyBuiltin("java.rb", "java", "org.jruby.javasupport.Java");
        addLazyBuiltin("jruby.rb", "jruby", "org.jruby.libraries.JRubyLibrary");
        addLazyBuiltin("jruby/ext.rb", "jruby/ext", "org.jruby.RubyJRuby$ExtLibrary");
        addLazyBuiltin("iconv.rb", "iconv", "org.jruby.libraries.IConvLibrary");
        addLazyBuiltin("nkf.rb", "nkf", "org.jruby.libraries.NKFLibrary");
        addLazyBuiltin("stringio.rb", "stringio", "org.jruby.libraries.StringIOLibrary");
        addLazyBuiltin("strscan.rb", "strscan", "org.jruby.libraries.StringScannerLibrary");
        addLazyBuiltin("zlib.rb", "zlib", "org.jruby.libraries.ZlibLibrary");
        addLazyBuiltin("yaml_internal.rb", "yaml_internal", "org.jruby.libraries.YamlLibrary");
        addLazyBuiltin("enumerator.rb", "enumerator", "org.jruby.libraries.EnumeratorLibrary");
        addLazyBuiltin("generator_internal.rb", "generator_internal", "org.jruby.ext.Generator$Service");
        addLazyBuiltin("readline.rb", "readline", "org.jruby.ext.Readline$Service");
        addLazyBuiltin("thread.so", "thread", "org.jruby.libraries.ThreadLibrary");
        addLazyBuiltin("digest.so", "digest", "org.jruby.libraries.DigestLibrary");
        addLazyBuiltin("digest.rb", "digest", "org.jruby.libraries.DigestLibrary");
        addLazyBuiltin("digest/md5.rb", "digest/md5", "org.jruby.libraries.DigestLibrary$MD5");
        addLazyBuiltin("digest/rmd160.rb", "digest/rmd160", "org.jruby.libraries.DigestLibrary$RMD160");
        addLazyBuiltin("digest/sha1.rb", "digest/sha1", "org.jruby.libraries.DigestLibrary$SHA1");
        addLazyBuiltin("digest/sha2.rb", "digest/sha2", "org.jruby.libraries.DigestLibrary$SHA2");
        addLazyBuiltin("bigdecimal.rb", "bigdecimal", "org.jruby.libraries.BigDecimalLibrary");
        addLazyBuiltin("io/wait.so", "io/wait", "org.jruby.libraries.IOWaitLibrary");
        addLazyBuiltin("etc.so", "etc", "org.jruby.libraries.EtcLibrary");
        addLazyBuiltin("weakref.rb", "weakref", "org.jruby.ext.WeakRef$WeakRefLibrary");
        addLazyBuiltin("socket.rb", "socket", "org.jruby.ext.socket.RubySocket$Service");
        addLazyBuiltin("rbconfig.rb", "rbconfig", "org.jruby.libraries.RbConfigLibrary");
        addLazyBuiltin("net/protocol.rb", "net/protocol", "org.jruby.libraries.NetProtocolBufferedIOLibrary");
        
        if (config.getCompatVersion() == CompatVersion.RUBY1_9) {
            addLazyBuiltin("fiber.so", "fiber", "org.jruby.libraries.FiberLibrary");
        }
        
        addBuiltinIfAllowed("openssl.so", new Library() {
            public void load(Ruby runtime, boolean wrap) throws IOException {
                runtime.getLoadService().require("jruby/openssl/stub");
            }
        });
        
        String[] builtins = {"fcntl", "yaml", "yaml/syck", "jsignal" };
        for (String library : builtins) {
            addBuiltinIfAllowed(library + ".rb", new BuiltinScript(library));
        }
        
        RubyKernel.autoload(topSelf, newSymbol("Java"), newString("java"));
    }

    private void addLazyBuiltin(String name, String shortName, String className) {
        addBuiltinIfAllowed(name, new LateLoadingLibrary(shortName, className, getJRubyClassLoader()));
    }

    private void addBuiltinIfAllowed(String name, Library lib) {
        if(profile.allowBuiltin(name)) {
            loadService.addBuiltinLibrary(name,lib);
        }
    }

    Object getRespondToMethod() {
        return respondToMethod;
    }

    void setRespondToMethod(Object rtm) {
        this.respondToMethod = rtm;
    }

    public Object getObjectToYamlMethod() {
        return objectToYamlMethod;
    }

    void setObjectToYamlMethod(Object otym) {
        this.objectToYamlMethod = otym;
    }

    /**
     * Retrieve mappings of cached methods to where they have been cached.  When a cached
     * method needs to be invalidated this map can be used to remove all places it has been
     * cached.
     *
     * @return the mappings of where cached methods have been stored
     */
    public CacheMap getCacheMap() {
        return cacheMap;
    }

    /** Getter for property rubyTopSelf.
     * @return Value of property rubyTopSelf.
     */
    public IRubyObject getTopSelf() {
        return topSelf;
    }

    public void setCurrentDirectory(String dir) {
        currentDirectory = dir;
    }

    public String getCurrentDirectory() {
        return currentDirectory;
    }
    
    public IRubyObject getUndef() {
        return undef;
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

    public RubyModule getEnumerable() {
        return enumerableModule;
    }
    void setEnumerable(RubyModule enumerableModule) {
        this.enumerableModule = enumerableModule;
    }      

    public RubyClass getString() {
        return stringClass;
    }    
    void setString(RubyClass stringClass) {
        this.stringClass = stringClass;
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

    public IRubyObject getTmsStruct() {
        return tmsStruct;
    }
    void setTmsStruct(RubyClass tmsStruct) {
        this.tmsStruct = tmsStruct;
    }
    
    public IRubyObject getPasswdStruct() {
        return passwdStruct;
    }
    void setPasswdStruct(RubyClass passwdStruct) {
        this.passwdStruct = passwdStruct;
    }

    public IRubyObject getGroupStruct() {
        return groupStruct;
    }
    void setGroupStruct(RubyClass groupStruct) {
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

    public RubyClass getException() {
        return exceptionClass;
    }
    void setException(RubyClass exceptionClass) {
        this.exceptionClass = exceptionClass;
    }
    
    public RubyClass getStandardError() {
        return standardError;
    }

    /** Getter for property isVerbose.
     * @return Value of property isVerbose.
     */
    public IRubyObject getVerbose() {
        return verbose;
    }

    /** Setter for property isVerbose.
     * @param verbose New value of property isVerbose.
     */
    public void setVerbose(IRubyObject verbose) {
        this.verbose = verbose;
    }

    /** Getter for property isDebug.
     * @return Value of property isDebug.
     */
    public IRubyObject getDebug() {
        return debug;
    }

    /** Setter for property isDebug.
     * @param debug New value of property isDebug.
     */
    public void setDebug(IRubyObject debug) {
        this.debug = debug;
    }

    public JavaSupport getJavaSupport() {
        return javaSupport;
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
    
    public Node parseFile(InputStream in, String file, DynamicScope scope) {
        return parser.parse(file, in, scope, new ParserConfiguration(0, false, false, true));
    }

    public Node parseInline(InputStream in, String file, DynamicScope scope) {
        return parser.parse(file, in, scope, new ParserConfiguration(0, false, true));
    }

    public Node parseEval(String content, String file, DynamicScope scope, int lineNumber) {
        byte[] bytes;
        
        try {
            bytes = content.getBytes(KCode.NONE.getKCode());
        } catch (UnsupportedEncodingException e) {
            bytes = content.getBytes();
        }
        
        return parser.parse(file, new ByteArrayInputStream(bytes), scope, 
                new ParserConfiguration(lineNumber, false));
    }

    public Node parse(String content, String file, DynamicScope scope, int lineNumber, 
            boolean extraPositionInformation) {
        byte[] bytes;
        
        try {
            bytes = content.getBytes(KCode.NONE.getKCode());
        } catch (UnsupportedEncodingException e) {
            bytes = content.getBytes();
        }

        return parser.parse(file, new ByteArrayInputStream(bytes), scope, 
                new ParserConfiguration(lineNumber, extraPositionInformation, false));
    }
    
    public Node parseEval(ByteList content, String file, DynamicScope scope, int lineNumber) {
        return parser.parse(file, content, scope, new ParserConfiguration(lineNumber, false));
    }

    public Node parse(ByteList content, String file, DynamicScope scope, int lineNumber, 
            boolean extraPositionInformation) {
        return parser.parse(file, content, scope, 
                new ParserConfiguration(lineNumber, extraPositionInformation, false));
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

        ThreadContext context = getCurrentContext();
        IRubyObject backtrace = excp.callMethod(context, "backtrace");

        PrintStream errorStream = getErrorStream();
        if (backtrace.isNil() || !(backtrace instanceof RubyArray)) {
            if (context.getFile() != null) {
                errorStream.print(context.getFile() + ":" + context.getLine());
            } else {
                errorStream.print(context.getLine());
            }
        } else if (((RubyArray) backtrace).getLength() == 0) {
            printErrorPos(context, errorStream);
        } else {
            IRubyObject mesg = ((RubyArray) backtrace).first(IRubyObject.NULL_ARRAY);

            if (mesg.isNil()) {
                printErrorPos(context, errorStream);
            } else {
                errorStream.print(mesg);
            }
        }

        RubyClass type = excp.getMetaClass();
        String info = excp.toString();

        if (type == fastGetClass("RuntimeError") && (info == null || info.length() == 0)) {
            errorStream.print(": unhandled exception\n");
        } else {
            String path = type.getName();

            if (info.length() == 0) {
                errorStream.print(": " + path + '\n');
            } else {
                if (path.startsWith("#")) {
                    path = null;
                }

                String tail = null;
                if (info.indexOf("\n") != -1) {
                    tail = info.substring(info.indexOf("\n") + 1);
                    info = info.substring(0, info.indexOf("\n"));
                }

                errorStream.print(": " + info);

                if (path != null) {
                    errorStream.print(" (" + path + ")\n");
                }

                if (tail != null) {
                    errorStream.print(tail + '\n');
                }
            }
        }

        excp.printBacktrace(errorStream);
    }

    private void printErrorPos(ThreadContext context, PrintStream errorStream) {
        if (context.getFile() != null) {
            if (context.getFrameName() != null) {
                errorStream.print(context.getFile() + ":" + context.getLine());
                errorStream.print(":in '" + context.getFrameName() + '\'');
            } else if (context.getLine() != 0) {
                errorStream.print(context.getFile() + ":" + context.getLine());
            } else {
                errorStream.print(context.getFile());
            }
        }
    }
    
    public void loadFile(String scriptName, InputStream in, boolean wrap) {
        if (!Ruby.isSecurityRestricted()) {
            File f = new File(scriptName);
            if(f.exists() && !f.isAbsolute() && !scriptName.startsWith("./")) {
                scriptName = "./" + scriptName;
            }
        }

        IRubyObject self = null;
        if (wrap) {
            self = TopSelfFactory.createTopSelf(this);
        } else {
            self = getTopSelf();
        }
        ThreadContext context = getCurrentContext();

        try {
            secure(4); /* should alter global state */

            context.preNodeEval(objectClass, self, scriptName);

            Node node = parseFile(in, scriptName, null);
            ASTInterpreter.eval(this, context, node, self, Block.NULL_BLOCK);
        } catch (JumpException.ReturnJump rj) {
            return;
        } finally {
            context.postNodeEval();
        }
    }
    
    public void compileAndLoadFile(String filename, InputStream in, boolean wrap) {
        IRubyObject self = null;
        if (wrap) {
            self = TopSelfFactory.createTopSelf(this);
        } else {
            self = getTopSelf();
        }
        ThreadContext context = getCurrentContext();

        try {
            secure(4); /* should alter global state */

            context.preNodeEval(objectClass, self, filename);
            
            Node scriptNode = parseFile(in, filename, null);
            
            Script script = tryCompile(scriptNode, new JRubyClassLoader(jrubyClassLoader));
            if (script == null) {
                System.err.println("Error, could not compile; pass -J-Djruby.jit.logging.verbose=true for more details");
            }

            runScript(script);
        } catch (JumpException.ReturnJump rj) {
            return;
        } finally {
            context.postNodeEval();
        }
    }

    public void loadScript(Script script) {
        IRubyObject self = getTopSelf();
        ThreadContext context = getCurrentContext();

        try {
            secure(4); /* should alter global state */

            context.preNodeEval(objectClass, self);
            
            script.load(context, self, IRubyObject.NULL_ARRAY, Block.NULL_BLOCK);
        } catch (JumpException.ReturnJump rj) {
            return;
        } finally {
            context.postNodeEval();
        }
    }

    public class CallTraceFuncHook implements EventHook {
        private RubyProc traceFunc;
        
        public void setTraceFunc(RubyProc traceFunc) {
            this.traceFunc = traceFunc;
        }
        
        public void event(ThreadContext context, int event, String file, int line, String name, IRubyObject type) {
            if (!context.isWithinTrace()) {
                if (file == null) file = "(ruby)";
                if (type == null) type = getFalse();
                
                RubyBinding binding = RubyBinding.newBinding(Ruby.this);

                context.preTrace();
                try {
                    traceFunc.call(context, new IRubyObject[] {
                        newString(EVENT_NAMES[event]), // event name
                        newString(file), // filename
                        newFixnum(line + 1), // line numbers should be 1-based
                        name != null ? newSymbol(name) : getNil(),
                        binding,
                        type
                    });
                } finally {
                    context.postTrace();
                }
            }
        }

        public boolean isInterestedInEvent(int event) {
            return true;
        }
    };
    
    private final CallTraceFuncHook callTraceFuncHook = new CallTraceFuncHook();
    
    public void addEventHook(EventHook hook) {
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
    
    public void callEventHooks(ThreadContext context, int event, String file, int line, String name, IRubyObject type) {
        for (EventHook eventHook : eventHooks) {
            if (eventHook.isInterestedInEvent(event)) {
                eventHook.event(context, event, file, line, name, type);
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
        while (!atExitBlocks.empty()) {
            RubyProc proc = atExitBlocks.pop();

            proc.call(proc.getRuntime().getCurrentContext(), IRubyObject.NULL_ARRAY);
        }
        if (finalizers != null) {
            synchronized (finalizers) {
                for (Iterator<Finalizable> finalIter = new ArrayList<Finalizable>(finalizers.keySet()).iterator(); finalIter.hasNext();) {
                    finalIter.next().finalize();
                    finalIter.remove();
                }
            }
        }

        synchronized (internalFinalizersMutex) {
            if (internalFinalizers != null) {
                for (Iterator<Finalizable> finalIter = new ArrayList<Finalizable>(
                        internalFinalizers.keySet()).iterator(); finalIter.hasNext();) {
                    finalIter.next().finalize();
                    finalIter.remove();
                }
            } 
        }
        getThreadService().disposeCurrentThread();
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

    public RubyArray newArray(IRubyObject[] objects) {
        return RubyArray.newArray(this, objects);
    }
    
    public RubyArray newArrayNoCopy(IRubyObject[] objects) {
        return RubyArray.newArrayNoCopy(this, objects);
    }
    
    public RubyArray newArrayNoCopyLight(IRubyObject[] objects) {
        return RubyArray.newArrayNoCopyLight(this, objects);
    }
    
    public RubyArray newArray(List<IRubyObject> list) {
        return RubyArray.newArray(this, list);
    }

    public RubyArray newArray(int size) {
        return RubyArray.newArray(this, size);
    }

    public RubyBoolean newBoolean(boolean value) {
        return RubyBoolean.newBoolean(this, value);
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

    public RubyFloat newFloat(double value) {
        return RubyFloat.newFloat(this, value);
    }

    public RubyNumeric newNumeric() {
        return RubyNumeric.newNumeric(this);
    }

    public RubyProc newProc(Block.Type type, Block block) {
        if (type != Block.Type.LAMBDA && block.getProcObject() != null) return block.getProcObject();

        RubyProc proc =  RubyProc.newProc(this, type);

        proc.callInit(IRubyObject.NULL_ARRAY, block);

        return proc;
    }

    public RubyBinding newBinding() {
        return RubyBinding.newBinding(this);
    }

    public RubyBinding newBinding(Binding binding) {
        return RubyBinding.newBinding(this, binding);
    }

    public RubyString newString() {
        return RubyString.newString(this, "");
    }

    public RubyString newString(String string) {
        return RubyString.newString(this, string);
    }
    
    public RubyString newString(ByteList byteList) {
        return RubyString.newString(this, byteList);
    }
    
    public RubyString newStringShared(ByteList byteList) {
        return RubyString.newStringShared(this, byteList);
    }    

    public RubySymbol newSymbol(String name) {
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
        assert internedName == internedName.intern() : internedName + " is not interned";

        return symbolTable.fastGetSymbol(internedName);
    }

    public RubyTime newTime(long milliseconds) {
        return RubyTime.newTime(this, milliseconds);
    }

    public RaiseException newRuntimeError(String message) {
        return newRaiseException(fastGetClass("RuntimeError"), message);
    }    
    
    public RaiseException newArgumentError(String message) {
        return newRaiseException(fastGetClass("ArgumentError"), message);
    }

    public RaiseException newArgumentError(int got, int expected) {
        return newRaiseException(fastGetClass("ArgumentError"), "wrong # of arguments(" + got + " for " + expected + ")");
    }

    public RaiseException newErrnoEBADFError() {
        return newRaiseException(fastGetModule("Errno").fastGetClass("EBADF"), "Bad file descriptor");
    }

    public RaiseException newErrnoENOPROTOOPTError() {
        return newRaiseException(fastGetModule("Errno").fastGetClass("ENOPROTOOPT"), "Protocol not available");
    }

    public RaiseException newErrnoEPIPEError() {
        return newRaiseException(fastGetModule("Errno").fastGetClass("EPIPE"), "Broken pipe");
    }

    public RaiseException newErrnoECONNREFUSEDError() {
        return newRaiseException(fastGetModule("Errno").fastGetClass("ECONNREFUSED"), "Connection refused");
    }

    public RaiseException newErrnoEADDRINUSEError() {
        return newRaiseException(fastGetModule("Errno").fastGetClass("EADDRINUSE"), "Address in use");
    }

    public RaiseException newErrnoEINVALError() {
        return newRaiseException(fastGetModule("Errno").fastGetClass("EINVAL"), "Invalid file");
    }

    public RaiseException newErrnoENOENTError() {
        return newRaiseException(fastGetModule("Errno").fastGetClass("ENOENT"), "File not found");
    }

    public RaiseException newErrnoEACCESError(String message) {
        return newRaiseException(
                fastGetModule("Errno").fastGetClass("EACCES"), message);
    }

    public RaiseException newErrnoEAGAINError(String message) {
        return newRaiseException(
                fastGetModule("Errno").fastGetClass("EAGAIN"), message);
    }

    public RaiseException newErrnoEISDirError() {
        return newRaiseException(fastGetModule("Errno").fastGetClass("EISDIR"), "Is a directory");
    }

    public RaiseException newErrnoESPIPEError() {
        return newRaiseException(fastGetModule("Errno").fastGetClass("ESPIPE"), "Illegal seek");
    }

    public RaiseException newErrnoEBADFError(String message) {
        return newRaiseException(fastGetModule("Errno").fastGetClass("EBADF"), message);
    }

    public RaiseException newErrnoEINVALError(String message) {
        return newRaiseException(fastGetModule("Errno").fastGetClass("EINVAL"), message);
    }

    public RaiseException newErrnoENOTDIRError(String message) {
        return newRaiseException(fastGetModule("Errno").fastGetClass("ENOTDIR"), message);
    }

    public RaiseException newErrnoENOENTError(String message) {
        return newRaiseException(fastGetModule("Errno").fastGetClass("ENOENT"), message);
    }

    public RaiseException newErrnoESPIPEError(String message) {
        return newRaiseException(fastGetModule("Errno").fastGetClass("ESPIPE"), message);
    }

    public RaiseException newErrnoEEXISTError(String message) {
        return newRaiseException(fastGetModule("Errno").fastGetClass("EEXIST"), message);
    }
    
    public RaiseException newErrnoEDOMError(String message) {
        return newRaiseException(fastGetModule("Errno").fastGetClass("EDOM"), "Domain error - " + message);
    }   
    
    public RaiseException newErrnoECHILDError() {
        return newRaiseException(fastGetModule("Errno").fastGetClass("ECHILD"), "No child processes");
    }    

    public RaiseException newIndexError(String message) {
        return newRaiseException(fastGetClass("IndexError"), message);
    }

    public RaiseException newSecurityError(String message) {
        return newRaiseException(fastGetClass("SecurityError"), message);
    }

    public RaiseException newSystemCallError(String message) {
        return newRaiseException(fastGetClass("SystemCallError"), message);
    }

    public RaiseException newTypeError(String message) {
        return newRaiseException(fastGetClass("TypeError"), message);
    }

    public RaiseException newThreadError(String message) {
        return newRaiseException(fastGetClass("ThreadError"), message);
    }

    public RaiseException newSyntaxError(String message) {
        return newRaiseException(fastGetClass("SyntaxError"), message);
    }

    public RaiseException newRegexpError(String message) {
        return newRaiseException(fastGetClass("RegexpError"), message);
    }

    public RaiseException newRangeError(String message) {
        return newRaiseException(fastGetClass("RangeError"), message);
    }

    public RaiseException newNotImplementedError(String message) {
        return newRaiseException(fastGetClass("NotImplementedError"), message);
    }
    
    public RaiseException newInvalidEncoding(String message) {
        return newRaiseException(fastGetClass("Iconv").fastGetClass("InvalidEncoding"), message);
    }

    public RaiseException newNoMethodError(String message, String name, IRubyObject args) {
        return new RaiseException(new RubyNoMethodError(this, this.fastGetClass("NoMethodError"), message, name, args), true);
    }

    public RaiseException newNameError(String message, String name) {
        return newNameError(message, name, null);
    }

    public RaiseException newNameError(String message, String name, Throwable origException) {
        return newNameError(message, name, origException, true);
    }

    public RaiseException newNameError(String message, String name, Throwable origException, boolean printWhenVerbose) {
        if (printWhenVerbose && origException != null && this.getVerbose().isTrue()) {
            origException.printStackTrace(getErrorStream());
        }
        return new RaiseException(new RubyNameError(
                this, this.fastGetClass("NameError"), message, name), true);
    }

    public RaiseException newLocalJumpError(String reason, IRubyObject exitValue, String message) {
        return new RaiseException(new RubyLocalJumpError(this, fastGetClass("LocalJumpError"), message, reason, exitValue), true);
    }

    public RaiseException newRedoLocalJumpError() {
        return new RaiseException(new RubyLocalJumpError(this, fastGetClass("LocalJumpError"), "unexpected redo", "redo", getNil()), true);
    }

    public RaiseException newLoadError(String message) {
        return newRaiseException(fastGetClass("LoadError"), message);
    }

    public RaiseException newFrozenError(String objectType) {
        // TODO: Should frozen error have its own distinct class?  If not should more share?
        return newRaiseException(fastGetClass("TypeError"), "can't modify frozen " + objectType);
    }

    public RaiseException newSystemStackError(String message) {
        return newRaiseException(fastGetClass("SystemStackError"), message);
    }

    public RaiseException newSystemExit(int status) {
        return new RaiseException(RubySystemExit.newInstance(this, status));
    }

    public RaiseException newIOError(String message) {
        return newRaiseException(fastGetClass("IOError"), message);
    }

    public RaiseException newStandardError(String message) {
        return newRaiseException(fastGetClass("StandardError"), message);
    }

    public RaiseException newIOErrorFromException(IOException ioe) {
        return newRaiseException(fastGetClass("IOError"), ioe.getMessage());
    }

    public RaiseException newTypeError(IRubyObject receivedObject, RubyClass expectedType) {
        return newRaiseException(fastGetClass("TypeError"), "wrong argument type " +
                receivedObject.getMetaClass().getRealClass() + " (expected " + expectedType + ")");
    }

    public RaiseException newEOFError() {
        return newRaiseException(fastGetClass("EOFError"), "End of file reached");
    }

    public RaiseException newEOFError(String message) {
        return newRaiseException(fastGetClass("EOFError"), message);
    }

    public RaiseException newZeroDivisionError() {
        return newRaiseException(fastGetClass("ZeroDivisionError"), "divided by 0");
    }

    public RaiseException newFloatDomainError(String message){
        return newRaiseException(fastGetClass("FloatDomainError"), message);
    }

    /**
     * @param exceptionClass
     * @param message
     * @return
     */
    private RaiseException newRaiseException(RubyClass exceptionClass, String message) {
        RaiseException re = new RaiseException(this, exceptionClass, message, true);
        return re;
    }


    public RubySymbol.SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public void setStackTraces(int stackTraces) {
        this.stackTraces = stackTraces;
    }

    public int getStackTraces() {
        return stackTraces;
    }

    public void setRandomSeed(long randomSeed) {
        this.randomSeed = randomSeed;
    }

    public long getRandomSeed() {
        return randomSeed;
    }

    public Random getRandom() {
        return random;
    }

    public ObjectSpace getObjectSpace() {
        return objectSpace;
    }

    public Map<Integer, WeakReference<ChannelDescriptor>> getDescriptors() {
        return descriptors;
    }

    public long incrementRandomSeedSequence() {
        return randomSeedSequence++;
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

    public boolean isObjectSpaceEnabled() {
        return objectSpaceEnabled;
    }

    // The method is intentionally not public, since it typically should
    // not be used outside of the core.
    /* package-private */ void setObjectSpaceEnabled(boolean objectSpaceEnabled) {
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

    private CacheMap cacheMap = new CacheMap();
    private ThreadService threadService;
    private Hashtable<Object, Object> runtimeInformation;
    
    private POSIX posix;

    private int stackTraces = 0;

    private ObjectSpace objectSpace = new ObjectSpace();

    private final RubySymbol.SymbolTable symbolTable = new RubySymbol.SymbolTable(this);
    private Map<Integer, WeakReference<ChannelDescriptor>> descriptors = new ConcurrentHashMap<Integer, WeakReference<ChannelDescriptor>>();
    private long randomSeed = 0;
    private long randomSeedSequence = 0;
    private Random random = new Random();

    private List<EventHook> eventHooks = new Vector<EventHook>();
    private boolean hasEventHooks;  
    private boolean globalAbortOnExceptionEnabled = false;
    private boolean doNotReverseLookupEnabled = false;
    private volatile boolean objectSpaceEnabled;
    
    private final Set<Script> jittedMethods = Collections.synchronizedSet(new WeakHashSet<Script>());
    
    private static ThreadLocal<Ruby> currentRuntime = new ThreadLocal<Ruby>();
    public static final boolean RUNTIME_THREADLOCAL
            = SafePropertyAccessor.getBoolean("jruby.runtime.threadlocal");
    
    private long globalState = 1;
    
    private int safeLevel = -1;

    // Default objects
    private IRubyObject undef;
    private IRubyObject topSelf;
    private RubyNil nilObject;
    private RubyBoolean trueObject;
    private RubyBoolean falseObject;
    public final RubyFixnum[] fixnumCache = new RubyFixnum[256];

    private IRubyObject verbose;
    private IRubyObject debug;

    /**
     * All the core classes we keep hard references to. These are here largely
     * so that if someone redefines String or Array we won't start blowing up
     * creating strings and arrays internally. They also provide much faster
     * access than going through normal hash lookup on the Object class.
     */
    private RubyClass
            objectClass, moduleClass, classClass, nilClass, trueClass,
            falseClass, numericClass, floatClass, integerClass, fixnumClass,
            arrayClass, hashClass, rangeClass, stringClass, symbolClass,
            procClass, bindingClass, methodClass, unboundMethodClass,
            matchDataClass, regexpClass, timeClass, bignumClass, dirClass,
            fileClass, fileStatClass, ioClass, threadClass, threadGroupClass,
            continuationClass, structClass, tmsStruct, passwdStruct,
        groupStruct, 
            procStatusClass, exceptionClass, runtimeError, ioError,
            scriptError, nameError, signalException, standardError,
            systemCallError, rangeError;
    
    /**
     * All the core modules we keep direct references to, for quick access and
     * to ensure they remain available.
     */
    private RubyModule
            kernelModule, comparableModule, enumerableModule, mathModule,
            marshalModule, etcModule, fileTestModule, gcModule,
            objectSpaceModule, processModule, procUIDModule, procGIDModule,
            procSysModule, precisionModule, errnoModule;
    
    // record separator var, to speed up io ops that use it
    private GlobalVariable recordSeparatorVar;

    // former java.lang.System concepts now internalized for MVM
    private String currentDirectory;

    private long startTime = System.currentTimeMillis();

    private RubyInstanceConfig config;

    private InputStream in;
    private PrintStream out;
    private PrintStream err;

    // Java support
    private JavaSupport javaSupport;
    private JRubyClassLoader jrubyClassLoader;

    // Note: this field and the following static initializer
    // must be located be in this order!
    private volatile static boolean securityRestricted = false;
    static {
        if (SafePropertyAccessor.isSecurityProtected("jruby.reflection")) {
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

    private Parser parser = new Parser(this);

    private LoadService loadService;
    private GlobalVariables globalVariables = new GlobalVariables(this);
    private RubyWarnings warnings = new RubyWarnings(this);

    // Contains a list of all blocks (as Procs) that should be called when
    // the runtime environment exits.
    private Stack<RubyProc> atExitBlocks = new Stack<RubyProc>();

    private Profile profile;

    private KCode kcode = KCode.NONE;

    // Atomic integers for symbol and method IDs
    private AtomicInteger symbolLastId = new AtomicInteger(128);
    private AtomicInteger moduleLastId = new AtomicInteger(0);

    private Object respondToMethod;
    private Object objectToYamlMethod;

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
}
