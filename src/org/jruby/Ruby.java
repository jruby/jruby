/*
 * Ruby.java - No description
 * Created on 04. Juli 2001, 22:53
 *
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Stefan Matthias Aust <sma@3plus4.de>
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
package org.jruby;

import java.io.Reader;
import java.io.PrintStream;
import java.io.InputStream;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.HashMap;

import org.ablaf.ast.INode;
import org.ablaf.common.ISourcePosition;
import org.ablaf.internal.lexer.DefaultLexerPosition;
import org.jruby.common.IRubyErrorHandler;
import org.jruby.common.RubyErrorHandler;
import org.jruby.exceptions.BreakJump;
import org.jruby.exceptions.RetryJump;
import org.jruby.exceptions.ReturnJump;
import org.jruby.exceptions.SecurityError;
import org.jruby.exceptions.IOError;
import org.jruby.internal.runtime.builtin.ObjectFactory;
import org.jruby.internal.runtime.methods.IterateMethod;
import org.jruby.internal.runtime.methods.RubyMethodCache;
import org.jruby.javasupport.JavaSupport;
import org.jruby.javasupport.JavaUtil;
import org.jruby.parser.Parser;
import org.jruby.runtime.AliasGlobalVariable;
import org.jruby.runtime.BlockStack;
import org.jruby.runtime.Callback;
import org.jruby.runtime.Frame;
import org.jruby.runtime.FrameStack;
import org.jruby.runtime.GlobalVariable;
import org.jruby.runtime.Iter;
import org.jruby.runtime.LastCallStatus;
import org.jruby.runtime.Namespace;
import org.jruby.runtime.ObjectSpace;
import org.jruby.runtime.ReadonlyGlobalVariable;
import org.jruby.runtime.RubyExceptions;
import org.jruby.runtime.Scope;
import org.jruby.runtime.ScopeStack;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.DynamicVariableSet;
import org.jruby.runtime.builtin.IObjectFactory;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.ILoadService;
import org.jruby.runtime.load.LoadServiceFactory;
import org.jruby.runtime.regexp.IRegexpAdapter;
import org.jruby.runtime.variables.IVariablesService;
import org.jruby.runtime.variables.VariablesServiceFactory;
import org.jruby.util.Asserts;
import org.jruby.util.RubyStack;
import org.jruby.util.collections.IStack;

/**
 * The jruby runtime.
 *
 * @author  jpetersen
 * @version $Revision$
 * @since   0.1
 */
public final class Ruby {
    private ThreadContext mainContext = new ThreadContext(Ruby.this);
    private ThreadContextLocal threadContext = new ThreadContextLocal(mainContext);

    private static class ThreadContextLocal extends ThreadLocal {
        private ThreadContext mainContext;

        public ThreadContextLocal(ThreadContext mainContext) {
            this.mainContext = mainContext;
        }

        /**
         * @see java.lang.ThreadLocal#initialValue()
         */
        protected Object initialValue() {
            return this.mainContext;
        }

        public void dereferenceMainContext() {
            this.mainContext = null;
            set(null);
        }
    }

    public void dispose() {
        threadContext.dereferenceMainContext();
    }

    private RubyMethodCache methodCache;

    public int stackTraces = 0;

    private Map globalMap;

    public ObjectSpace objectSpace = new ObjectSpace();

    public final RubyFixnum[] fixnumCache = new RubyFixnum[256];
    public final RubySymbol.SymbolTable symbolTable = new RubySymbol.SymbolTable();

    public long randomSeed = 0;
    public Random random = new Random();

    private RubyProc traceFunction;
    private boolean isWithinTrace = false;

    /** safe-level:
    		0 - strings from streams/environment/ARGV are tainted (default)
    		1 - no dangerous operation by tainted value
    		2 - process/file operations prohibited
    		3 - all genetated objects are tainted
    		4 - no global (non-tainted) variable modification/no direct output
    */
    private int safeLevel = 0;

    private IObjectFactory factory = new ObjectFactory(this);

    // Default objects
    private IRubyObject nilObject;
    private RubyBoolean trueObject;
    private RubyBoolean falseObject;

    // Default classes
    private RubyClasses classes;
    private RubyExceptions exceptions;

    private IRubyObject topSelf;

    private Scope topScope = null;

    private Frame topFrame;

    private Namespace namespace;
    private Namespace topNamespace;

    private boolean isVerbose = false;

    private RubyModule wrapper;

    private RubyStack classStack = new RubyStack();

    // Java support
    private JavaSupport javaSupport;

    // pluggable Regexp engine
    private Class regexpAdapterClass;

    private Parser parser = new Parser(this);

    private LastCallStatus lastCallStatus = new LastCallStatus(this);

    private ILoadService loadService = LoadServiceFactory.createLoadService(this);
    private IVariablesService variablesService = VariablesServiceFactory.createVariablesService(this);
    private IRubyErrorHandler errorHandler = new RubyErrorHandler(this);

    /**
     * Create and initialize a new jruby Runtime.
     */
    private Ruby(Class regexpAdapterClass) {
        this.regexpAdapterClass = regexpAdapterClass;

        globalMap = new HashMap();

        nilObject = RubyObject.nilObject(this);
        trueObject = new RubyBoolean(this, true);
        falseObject = new RubyBoolean(this, false);

        javaSupport = new JavaSupport(this);

        methodCache = new RubyMethodCache();
    }

    /**
     * Returns a default instance of the JRuby runtime.
     *
     * @param regexpEngineName The regexp engine you want to use.
     * @return the JRuby runtime
     */
    public static Ruby getDefaultInstance(String regexpEngineName) {
        Class regexpAdapterClass = IRegexpAdapter.getAdapter(regexpEngineName);
        Ruby ruby = new Ruby(regexpAdapterClass);
        ruby.init();
        return ruby;
    }

    public static Ruby getDefaultInstance() {
        return getDefaultInstance((String) null);
    }

    /**
     * @deprecated use getDefaultInstance(String) or getDefaultInstance() instead.
     */
    public static Ruby getDefaultInstance(Class regexpAdapterClass) {
        Ruby ruby = new Ruby(regexpAdapterClass);
        ruby.init();
        return ruby;
    }

    /**
     * Evaluates a script and returns an instance of class returnClass.
     *
     * @param script The script to evaluate
     * @param returnClass The class which should be returned
     * @return the result Object
     */
    public Object evalScript(String script, Class returnClass) {
        IRubyObject result = evalScript(script);
        return JavaUtil.convertRubyToJava(this, result, returnClass);
    }

    /**
     * Evaluates a script and returns a RubyObject.
     */
    public IRubyObject evalScript(String script) {
        return eval(parse(script, "<script>"));
    }

    public IRubyObject eval(INode node) {
        return getCurrentContext().eval(node);
    }

    public Class getRegexpAdapterClass() {
        return regexpAdapterClass;
    }

    public RubyClasses getClasses() {
        return classes;
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

    /** Returns a class or module from the instance pool.
     *
     * @param name The name of the class or module.
     * @return The class or module.
     */
    public RubyModule getRubyModule(String name) {
        return classes.getClass(name);
    }

    /** Returns a class from the instance pool.
     *
     * @param name The name of the class.
     * @return The class.
     */
    public RubyClass getClass(String name) {
        return (RubyClass) classes.getClass(name);
    }

    /** Define a new class with name 'name' and super class 'superClass'.
     *
     * MRI: rb_define_class / rb_define_class_id
     *
     */
    public RubyClass defineClass(String name, RubyClass superClass) {
        if (superClass == null) {
            superClass = getClasses().getObjectClass();
        }

        RubyClass newClass = RubyClass.newClass(this, superClass, name);

        newClass.makeMetaClass(superClass.getInternalClass());

        newClass.inheritedBy(superClass);

        classes.putClass(name, newClass);

        return newClass;
    }

    public RubyClass defineClass(String name, String superName) {
        RubyClass superClass = getClass(superName);
        Asserts.assertTrue(superClass != null, "can't find superclass '" + superName + "'");
        return defineClass(name, superClass);
    }

    /** rb_define_module / rb_define_module_id
     *
     */
    public RubyModule defineModule(String name) {
        RubyModule newModule = RubyModule.newModule(this, name);
        getClasses().putClass(name, newModule);
        return newModule;
    }

    /** Getter for property securityLevel.
     * @return Value of property securityLevel.
     */
    public int getSafeLevel() {
        return this.safeLevel;
    }

    /** Setter for property securityLevel.
     * @param safeLevel New value of property securityLevel.
     */
    public void setSafeLevel(int safeLevel) {
        this.safeLevel = safeLevel;
    }

    public void secure(int level) {
        if (level <= safeLevel) {
            throw new SecurityError(this, "Insecure operation '" + getCurrentFrame().getLastFunc() + "' at level " + safeLevel);
        }
    }

    /** rb_define_global_const
     *
     */
    public void defineGlobalConstant(String name, IRubyObject value) {
        getClasses().getObjectClass().defineConstant(name, value);
    }

    public IRubyObject getTopConstant(String name) {
        IRubyObject constant = getClasses().getClass(name);

        if (constant == null) {
            constant = getLoadService().autoload(name);
        }

        return constant;
    }

    public boolean isClassDefined(String name) {
        return classes.getClass(name) != null;
    }

    public Iterator globalVariableNames() {
        return globalMap.keySet().iterator();
    }

    public boolean isGlobalVarDefined(String name) {
        return globalMap.containsKey(name);
    }

    public void undefineGlobalVar(String name) {
        globalMap.remove(name);
    }

    public IRubyObject setGlobalVar(String name, IRubyObject value) {
        GlobalVariable global = (GlobalVariable) globalMap.get(name);
        if (global == null) {
            globalMap.put(name, new GlobalVariable(this, name, value));
            return value;
        }
        global.set(value);
        return value;
    }

    public IRubyObject getGlobalVar(String name) {
        GlobalVariable global = (GlobalVariable) globalMap.get(name);
        if (global == null) {
            globalMap.put(name, new GlobalVariable(this, name, getNil()));
            return getNil();
        }
        return global.get();
    }

    public void aliasGlobalVar(String oldName, String newName) {
        if (getSafeLevel() >= 4) {
            throw new SecurityError(this, "Insecure: can't alias global variable");
        }

        if (! globalMap.containsKey(oldName)) {
            globalMap.put(oldName, new GlobalVariable(this, oldName, getNil()));
        }
        GlobalVariable oldEntry = (GlobalVariable) globalMap.get(oldName);
        globalMap.put(newName, new AliasGlobalVariable(this, newName, oldEntry));
    }

    public IRubyObject yield(IRubyObject value) {
        return yield(value, null, null, false);
    }

    public IRubyObject yield(IRubyObject value, IRubyObject self, RubyModule klass, boolean checkArguments) {
        return getCurrentContext().yield(value, self, klass, checkArguments);
    }

    public Scope currentScope() {
        return getScope().current();
    }

    /** Getter for property rubyTopSelf.
     * @return Value of property rubyTopSelf.
     */
    public IRubyObject getTopSelf() {
        return topSelf;
    }

    /** rb_iterate
     *
     */
    public IRubyObject iterate(Callback iterateMethod, IRubyObject data1, Callback blockMethod, IRubyObject data2) {
        getIterStack().push(Iter.ITER_PRE);
        getBlockStack().push(null, new IterateMethod(blockMethod, data2), getTopSelf());

        try {
            while (true) {
                try {
                    return iterateMethod.execute(data1, null);
                } catch (BreakJump bExcptn) {
                    return getNil();
                } catch (ReturnJump rExcptn) {
                    return rExcptn.getReturnValue();
                } catch (RetryJump rExcptn) {
                }
            }
        } finally {
            getIterStack().pop();
            getBlockStack().pop();
        }
    }

    /** ruby_init
     *
     */
    private void init() {
        getIterStack().push(Iter.ITER_NOT);
        getFrameStack().push();
        topFrame = getCurrentFrame();

        getScope().push();
        topScope = currentScope();

        setCurrentVisibility(Visibility.PRIVATE);

        classes = new RubyClasses(this);
        classes.initCoreClasses();

        RubyGlobal.createGlobals(this);

        exceptions = new RubyExceptions(this);
        exceptions.initDefaultExceptionClasses();

        topSelf = TopSelfFactory.createTopSelf(this);

        classStack.push(getClasses().getObjectClass());
        getCurrentFrame().setSelf(topSelf);
        topNamespace = new Namespace(getClasses().getObjectClass());
        namespace = topNamespace;
        getCurrentFrame().setNamespace(namespace);

        classes.initBuiltinClasses();

        getScope().pop();
        getScope().push(topScope);
    }

    /** Getter for property rubyScope.
     * @return Value of property rubyScope.
     */
    public ScopeStack getScope() {
        return getCurrentContext().getScopeStack();
    }

    /** Getter for property methodCache.
     * @return Value of property methodCache.
     */
    public RubyMethodCache getMethodCache() {
        return methodCache;
    }

    /** Getter for property sourceFile.
     * @return Value of property sourceFile.
     */
    public String getSourceFile() {
        return getPosition().getFile();
    }

    /** Getter for property sourceLine.
     * @return Value of property sourceLine.
     */
    public int getSourceLine() {
        return getPosition().getLine();
    }

    /** Getter for property isVerbose.
     * @return Value of property isVerbose.
     */
    public boolean isVerbose() {
        return isVerbose;
    }

    public boolean isBlockGiven() {
        return getCurrentFrame().isBlockGiven();
    }

    public boolean isFBlockGiven() {
        Frame previous = getFrameStack().getPrevious();
        if (previous == null) {
            return false;
        }
        return previous.isBlockGiven();
    }

    public void pushClass(RubyModule newClass) {
        classStack.push(newClass);
    }

    public void popClass() {
        classStack.pop();
    }

    /** Setter for property isVerbose.
     * @param verbose New value of property isVerbose.
     */
    public void setVerbose(boolean verbose) {
        this.isVerbose = verbose;
        errorHandler.setVerbose(verbose);
    }

    /** Getter for property dynamicVars.
     * @return Value of property dynamicVars.
     */
    public DynamicVariableSet getDynamicVars() {
        return getCurrentContext().getCurrentDynamicVars();
    }

    public IRubyObject getDynamicValue(String name) {
        IRubyObject result = getDynamicVars().get(name);
        if (result == null) {
            return getNil();
        }
        return result;
    }

    /** Getter for property rubyClass.
     * @return Value of property rubyClass.
     */
    public RubyModule getRubyClass() {
        RubyModule rubyClass = (RubyModule) classStack.peek();
        if (rubyClass.isIncluded()) {
            return ((RubyIncludedClass) rubyClass).getDelegate();
        }
        return rubyClass;
    }

    public FrameStack getFrameStack() {
        return getCurrentContext().getFrameStack();
    }

    public Frame getCurrentFrame() {
        return getCurrentContext().getCurrentFrame();
    }

    /** Getter for property topFrame.
     * @return Value of property topFrame.
     */
    public Frame getTopFrame() {
        return topFrame;
    }

    /** Setter for property topFrame.
     * @param topFrame New value of property topFrame.
     */
    public void setTopFrame(Frame topFrame) {
        this.topFrame = topFrame;
    }

    public Namespace getNamespace() {
        return namespace;
    }

    public void setNamespace(Namespace newNamespace) {
        namespace = newNamespace;
    }

    public Namespace getTopNamespace() {
        return topNamespace;
    }

    public JavaSupport getJavaSupport() {
        return javaSupport;
    }

    public IStack getIterStack() {
        return getCurrentContext().getIterStack();
    }

    public Iter getCurrentIter() {
        return getCurrentContext().getCurrentIter();
    }

    public BlockStack getBlockStack() {
        return getCurrentContext().getBlockStack();
    }

    /** Getter for property cBase.
     * @return Value of property cBase.
     */
    public RubyModule getCBase() {
        return getCurrentFrame().getNamespace().getNamespaceModule();
    }

    public Visibility getCurrentVisibility() {
        return currentScope().getVisibility();
    }

    public void setCurrentVisibility(Visibility visibility) {
        currentScope().setVisibility(visibility);
    }

    /** Getter for property wrapper.
     * @return Value of property wrapper.
     */
    public RubyModule getWrapper() {
        return wrapper;
    }

    /** Setter for property wrapper.
     * @param wrapper New value of property wrapper.
     */
    public void setWrapper(RubyModule wrapper) {
        this.wrapper = wrapper;
    }

    /**
     * Gets the exceptions
     * @return Returns a RubyExceptions
     */
    public RubyExceptions getExceptions() {
        return exceptions;
    }

    /** Defines a global variable
     */
    public void defineVariable(GlobalVariable variable) {
        globalMap.put(variable.name(), variable);
    }

    /** defines a readonly global variable
     *
     */
    public void defineReadonlyVariable(String name, IRubyObject value) {
        globalMap.put(name, new ReadonlyGlobalVariable(this, name, value));
    }

    public INode parse(Reader content, String file) {
        return parser.parse(file, content);
    }

    public INode parse(String content, String file) {
        return parser.parse(file, content);
    }

    public IRubyObject getLastline() {
        if (getScope().hasLocalValues()) {
            return getScope().getValue(0);
        }
        return RubyString.nilString(this);
    }

    public void setLastline(IRubyObject value) {
        if (! getScope().hasLocalValues()) {
            getScope().setLocalNames(new ArrayList(Arrays.asList(new String[] { "_", "~" })));
        }
        getScope().setValue(0, value);
    }

    public IRubyObject getBackref() {
        if (getScope().hasLocalValues()) {
            return getScope().getValue(1);
        }
        return getNil();
    }

    public void setBackref(IRubyObject match) {
        if (! getScope().hasLocalValues()) {
            getScope().setLocalNames(new ArrayList(Arrays.asList(new String[] { "_", "~" })));
        }
        getScope().setValue(1, match);
    }

    public Parser getParser() {
        return parser;
    }

    public ThreadContext getCurrentContext() {
        return (ThreadContext)threadContext.get();
    }

    public ThreadContext getMainContext() {
        return mainContext;
    }

    public void registerNewContext(ThreadClass thread) {
        threadContext.set(new ThreadContext(this));
        getCurrentContext().setCurrentThread(thread);
    }

    public ISourcePosition getPosition() {
        return getCurrentContext().getPosition();
    }

    public void setPosition(String file, int line) {
        setPosition(new DefaultLexerPosition(file, line, 0));
    }

    public void setPosition(ISourcePosition position) {
        getCurrentContext().setPosition(position);
    }

    public void pushDynamicVars() {
        getCurrentContext().pushDynamicVars();
    }

    public void popDynamicVars() {
        getCurrentContext().popDynamicVars();
    }

    public void setDynamicVariable(String name, IRubyObject value) {
        getDynamicVars().set(name, value);
    }

    public List getDynamicNames() {
        return getDynamicVars().names();
    }
    /**
     * Returns the factory.
     * @return IObjectFactory
     */
    public IObjectFactory getFactory() {
        return factory;
    }

    /**
     * Returns the lastCallStatus.
     * @return LastCallStatus
     */
    public LastCallStatus getLastCallStatus() {
        return lastCallStatus;
    }

    /**
     * Returns the loadService.
     * @return ILoadService
     */
    public ILoadService getLoadService() {
        return loadService;
    }

    /**
     * Returns the errorHandler.
     * @return IRubyErrorHandler
     */
    public IRubyErrorHandler getErrorHandler() {
        return errorHandler;
    }

    public IRubyObject callSuper(IRubyObject[] args) {
        return getCurrentContext().callSuper(args);
    }

    public PrintStream getErrorStream() {
        return new PrintStream(((RubyIO) getGlobalVar("$stderr")).getOutStream());
    }

    public InputStream getInputStream() {
        return ((RubyIO) getGlobalVar("$stdin")).getInStream();
    }

    public PrintStream getOutputStream() {
        return new PrintStream(((RubyIO) getGlobalVar("$stdout")).getOutStream());
    }

    private static final int TRACE_HEAD = 8;
    private static final int TRACE_TAIL = 5;
    private static final int TRACE_MAX = TRACE_HEAD + TRACE_TAIL + 5;
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
            if (getSourceFile() != null) {
                getErrorStream().print(getSourceFile() + ':' + getSourceLine());
            } else {
                getErrorStream().print(getSourceLine());
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

        if (type == getExceptions().getRuntimeError() && (info == null || info.length() == 0)) {
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
        if (getSourceFile() != null) {
            if (getCurrentFrame().getLastFunc() != null) {
                getErrorStream().print(getSourceFile() + ':' + getSourceLine());
                getErrorStream().print(":in '" + getCurrentFrame().getLastFunc() + '\'');
            } else if (getSourceLine() != 0) {
                getErrorStream().print(getSourceFile() + ':' + getSourceLine());
            } else {
                getErrorStream().print(getSourceFile());
            }
        }
    }

    /** This method compiles and interprets a Ruby script.
     *
     *  It can be used if you want to use JRuby as a Macro language.
     *
     */
    public void loadScript(RubyString scriptName, RubyString source, boolean wrap) {
        loadScript(scriptName.getValue(), new StringReader(source.getValue()), wrap);
    }

    public void loadScript(String scriptName, Reader source, boolean wrap) {
        IRubyObject self = getTopSelf();
        Namespace savedNamespace = getNamespace();

        pushDynamicVars();

        RubyModule wrapper = getWrapper();
        setNamespace(getTopNamespace());

        if (!wrap) {
            secure(4); /* should alter global state */
            pushClass(getClasses().getObjectClass());
            setWrapper(null);
        } else {
            /* load in anonymous module as toplevel */
            setWrapper(RubyModule.newModule(this));
            pushClass(getWrapper());
            self = getTopSelf().rbClone();
            self.extendObject(getRubyClass());
            setNamespace(new Namespace(getWrapper(), getNamespace()));
        }

        String last_func = getCurrentFrame().getLastFunc();

        getFrameStack().push();
        getCurrentFrame().setLastFunc(null);
        getCurrentFrame().setLastClass(null);
        getCurrentFrame().setSelf(self);
        getCurrentFrame().setNamespace(new Namespace(getRubyClass(), null));
        getScope().push();

        /* default visibility is private at loading toplevel */
        setCurrentVisibility(Visibility.PRIVATE);

        try {
            INode node = parse(source, scriptName);
            self.eval(node);

        } finally {
            getCurrentFrame().setLastFunc(last_func);
            setNamespace(savedNamespace);
            getScope().pop();
            getFrameStack().pop();
            popClass();
            popDynamicVars();
            setWrapper(wrapper);
        }
    }
    
    public void loadNode(String scriptName, INode node, boolean wrap) {
        IRubyObject self = getTopSelf();
        Namespace savedNamespace = getNamespace();

        pushDynamicVars();

        RubyModule wrapper = getWrapper();
        setNamespace(getTopNamespace());

        if (!wrap) {
            secure(4); /* should alter global state */
            pushClass(getClasses().getObjectClass());
            setWrapper(null);
        } else {
            /* load in anonymous module as toplevel */
            setWrapper(RubyModule.newModule(this));
            pushClass(getWrapper());
            self = getTopSelf().rbClone();
            self.extendObject(getRubyClass());
            setNamespace(new Namespace(getWrapper(), getNamespace()));
        }

        String last_func = getCurrentFrame().getLastFunc();

        getFrameStack().push();
        getCurrentFrame().setLastFunc(null);
        getCurrentFrame().setLastClass(null);
        getCurrentFrame().setSelf(self);
        getCurrentFrame().setNamespace(new Namespace(getRubyClass(), null));
        getScope().push();

        /* default visibility is private at loading toplevel */
        setCurrentVisibility(Visibility.PRIVATE);

        try {
            self.eval(node);

        } finally {
            getCurrentFrame().setLastFunc(last_func);
            setNamespace(savedNamespace);
            getScope().pop();
            getFrameStack().pop();
            popClass();
            popDynamicVars();
            setWrapper(wrapper);
        }
    }


    /** Loads, compiles and interprets a Ruby file.
     *  Used by Kernel#require.
     *
     *  @mri rb_load
     */
    public void loadFile(File file, boolean wrap) {
        Asserts.assertNotNull(file, "No such file to load");
        try {
            BufferedReader source = new BufferedReader(new FileReader(file));
            loadScript(file.getPath(), source, wrap);
            source.close();
        } catch (IOException ioExcptn) {
            throw IOError.fromException(this, ioExcptn);
        }
    }

    /** Call the trace function
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
        if (!isWithinTrace && traceFunction != null) {
            isWithinTrace = true;

            ISourcePosition savePosition = getPosition();
            String file = position.getFile();

            if (file == null) {
                file = "(ruby)";
            }
            if (type == null)
                type = getFalse();

            getFrameStack().push();
            getCurrentFrame().setIter(Iter.ITER_NOT);

            try {
                traceFunction
                    .call(new IRubyObject[] {
                        RubyString.newString(this, event),
                        RubyString.newString(this, file),
                        RubyFixnum.newFixnum(this, position.getLine()),
                        name != null ? RubySymbol.newSymbol(this, name) : getNil(),
                        self != null ? self: getNil(),
                        type });
            } finally {
                getFrameStack().pop();
                setPosition(savePosition);
                isWithinTrace = false;
            }
        }
    }

    public RubyProc getTraceFunction() {
        return traceFunction;
    }

    public void setTraceFunction(RubyProc traceFunction) {
        this.traceFunction = traceFunction;
    }
}