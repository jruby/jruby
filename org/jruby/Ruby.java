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

import java.io.*;
import java.util.*;

import org.ablaf.ast.*;
import org.ablaf.lexer.*;
import org.ablaf.common.*;
import org.ablaf.internal.lexer.DefaultLexerPosition;
import org.ablaf.parser.*;

import org.jruby.ast.*;
import org.jruby.common.*;
import org.jruby.evaluator.*;
import org.jruby.exceptions.*;
import org.jruby.internal.runtime.methods.*;
import org.jruby.javasupport.*;
import org.jruby.parser.*;
import org.jruby.runtime.*;
import org.jruby.runtime.methods.*;
import org.jruby.util.*;
import org.jruby.util.collections.*;

/**
 * The jruby runtime.
 *
 * @author  jpetersen
 * @version $Revision$
 * @since   0.1
 * @fixme  a mechanism should be there to avoid creating several instances of the same
 * 		   value objects with the same value (this would apply to Symbol and number specifically)
 */
public final class Ruby {

    public static final String RUBY_MAJOR_VERSION = "1.6";
    public static final String RUBY_VERSION = "1.6.7";

    private static final String[] REGEXP_ADAPTER =
        { "org.jruby.regexp.JDKRegexpAdapter", "org.jruby.regexp.GNURegexpAdapter", "org.jruby.regexp.ORORegexpAdapter" };

    private RubyMethodCache methodCache;

    public int stackTraces = 0;
    /** rb_global_tbl
     *
     */
    private RubyMap globalMap;

    public LinkedList objectSpace = new LinkedList();

    public final RubyFixnum[] fixnumCache = new RubyFixnum[256];

    public long randomSeed = 0;
    public Random random = new Random();


    /** safe-level:
    		0 - strings from streams/environment/ARGV are tainted (default)
    		1 - no dangerous operation by tainted value
    		2 - process/file operations prohibited
    		3 - all genetated objects are tainted
    		4 - no global (non-tainted) variable modification/no direct output
    */
    private int safeLevel = 0;

    // private RubyInterpreter rubyInterpreter = null;

    // Default objects
    private RubyObject nilObject;
    private RubyBoolean trueObject;
    private RubyBoolean falseObject;

    // Default classes
    private RubyClasses classes;
    private RubyExceptions exceptions;

    //
    private RubyRuntime runtime = new RubyRuntime(this);

    private RubyObject rubyTopSelf;

    // Eval
    private ScopeStack scope = new ScopeStack(this);
    private Scope topScope = null;
    private RubyVarmap dynamicVars = null;
    private RubyModule rubyClass = null;

    private FrameStack frameStack = new FrameStack(this);
    private Frame topFrame;

    private Namespace namespace;
    private Namespace topNamespace;

	public ISourcePosition getPosition()
	{
		return new DefaultLexerPosition(getSourceFile(), getSourceLine(), 0);
	}
    private String sourceFile;
    private int sourceLine;

    private int inEval;

    private boolean verbose;

    // 
    private IStack iterStack = CollectionFactory.getInstance().newStack();
    private BlockStack block = new BlockStack(this);

    private RubyModule cBase;

    private int actMethodScope;

    private RubyModule wrapper;

    private RubyStack classStack = new RubyStack(new LinkedList());
    public RubyStack varMapStack = new RubyStack(new LinkedList());

    // init
    private boolean initialized = false;

    // Java support
    private JavaSupport javaSupport;

    // pluggable Regexp engine
    private Class regexpAdapterClass;

    private IParser parser;

    /**
     * Create and initialize a new jruby Runtime.
     */
    public Ruby() {
        globalMap = new RubyHashMap();

        nilObject = RubyObject.nilObject(this);
        trueObject = new RubyBoolean(this, true);
        falseObject = new RubyBoolean(this, false);

        javaSupport = new JavaSupport(this);

        methodCache = new RubyMethodCache(this);
    }

    /**
     * Returns a default instance of the JRuby runtime.
     * 
     * @param regexpAdapterClass The RegexpAdapter class you want to use.
     * @return the JRuby runtime
     */
    public static Ruby getDefaultInstance(Class regexpAdapterClass) {
        for (int i = 0; regexpAdapterClass == null && i < REGEXP_ADAPTER.length; i++) {
            try {
                regexpAdapterClass = Class.forName(REGEXP_ADAPTER[i]);
            } catch (ClassNotFoundException cnfExcptn) {
            } catch (NoClassDefFoundError ncdfError) {
            }
        }

        Ruby ruby = new Ruby();
        ruby.setRegexpAdapterClass(regexpAdapterClass);
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
        RubyObject result = evalScript(script);
        return JavaUtil.convertRubyToJava(this, result, returnClass);
    }

    /**
     * Evaluates a script and returns a RubyObject.
     */
    public RubyObject evalScript(String script) {
        return getRubyTopSelf().eval(compile(script, "<script>", 1));
    }

    /**
     * 
     * Prints out an error message.
     * 
     * @param exception An Exception thrown by JRuby
     */
    public void printException(Exception exception) {
        if (exception instanceof RaiseException) {
            getRuntime().printError(((RaiseException) exception).getActException());
        } else if (exception instanceof ThrowJump) {
            getRuntime().printError(((ThrowJump) exception).getNameError());
        } else if (exception instanceof BreakJump) {
            getRuntime().getErrorStream().println("break without block.");
        } else if (exception instanceof ReturnException) {
            getRuntime().getErrorStream().println("return without block.");
        }
    }

    public Class getRegexpAdapterClass() {
        return regexpAdapterClass;
    }

    public void setRegexpAdapterClass(Class iRegexpAdapterClass) {
        regexpAdapterClass = iRegexpAdapterClass;
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
    public RubyObject getNil() {
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
    public RubyClass getRubyClass(String name) {
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

        RubyClass newClass = RubyClass.newClass(this, superClass);
        newClass.setName(name);

        newClass.makeMetaClass(superClass.getRubyClass());

        newClass.inheritedBy(superClass);

        classes.putClass(name, newClass);

        return newClass;
    }

    /** rb_define_module / rb_define_module_id
     *
     */
    public RubyModule defineModule(String name) {
        RubyModule newModule = RubyModule.newModule(this);
        newModule.setName(name);

        getClasses().putClass(name, newModule);

        return newModule;
    }

    /** rb_define_global_function
     *
     */
    public void defineGlobalFunction(String name, Callback method) {
        getClasses().getKernelModule().defineModuleFunction(name, method);
    }

    /** Getter for property securityLevel.
     * @return Value of property securityLevel.
     */
    public int getSafeLevel() {
        return this.safeLevel;
    }

    /** Setter for property securityLevel.
     * @param securityLevel New value of property securityLevel.
     */
    public void setSafeLevel(int safeLevel) {
        this.safeLevel = safeLevel;
    }

    public void secure(int level) {
        if (level <= safeLevel) {
            throw new RubySecurityException(this, "Insecure operation '" + getActFrame().getLastFunc() + "' at level " + safeLevel);
        }
    }

    public RubyFixnum getFixnumInstance(long value) {
        return new RubyFixnum(this, value);
    }

    /** rb_define_global_const
     *
     */
    public void defineGlobalConstant(String name, RubyObject value) {
        getClasses().getObjectClass().defineConstant(name, value);
    }

    /** top_const_get
     *
     */
    public RubyObject getTopConstant(String id) {
        if (getClasses().getClass(id) != null) {
            return (RubyObject) getClasses().getClass(id);
        }

        /* autoload */
        // if (autoload_tbl && st_lookup(autoload_tbl, id, 0)) {
        //    rb_autoload_load(id);
        //    *klassp = rb_const_get(rb_cObject, id);
        //    return Qtrue;
        //}

        return null;
    }

    /**
     *
     */
    public boolean isAutoloadDefined(String name) {
        return false;
    }

    public boolean isClassDefined(String name) {
        return classes.getClass(name) != null;
    }

    /* public RubyInterpreter getInterpreter() {
        if (rubyInterpreter == null) {
            rubyInterpreter = new RubyInterpreter(this);
        }
        return rubyInterpreter;
    }*/

    public Iterator globalVariableNames() {
        return globalMap.keySet().iterator();
    }

    public boolean isGlobalVarDefined(String name) {
        return getGlobalEntry(name).isDefined();
    }

    public void undefineGlobalVar(String name) {
        getGlobalEntry(name).undefine();
    }

    public RubyObject setGlobalVar(String name, RubyObject value) {
        return getGlobalEntry(name).set(value);
    }

    public RubyObject getGlobalVar(String name) {
        return getGlobalEntry(name).get();
    }

    public void aliasGlobalVar(String oldName, String newName) {
        getGlobalEntry(oldName).alias(newName);
    }

    public RubyObject yield(RubyObject value) {
        return yield0(value, null, null, false);
    }

    public RubyObject yield0(RubyObject value, RubyObject self, RubyModule klass, boolean acheck) {
        if (!isBlockGiven()) {
            throw new RaiseException(this, getExceptions().getLocalJumpError(), "yield called out of block");
        }

        RubyVarmap.push(this);
        Block actBlock = block.getAct();

        getFrameStack().push(actBlock.getFrame());

        Namespace oldNamespace = getNamespace();
        setNamespace(getActFrame().getNamespace());

        Scope oldScope = (Scope) getScope().getTop();
        getScope().setTop(actBlock.getScope());
        // getScope().push(tmpBlock.scope);

        // block.pop();
        // XXX
        block.pop(); // setAct((Block)actBlock.getNext());

        setDynamicVars(actBlock.getDynamicVars());

        pushClass((klass != null) ? klass : actBlock.getKlass());

        if (klass == null) {
            self = actBlock.getSelf();
        }

        if (value == null) {
            value = RubyArray.newArray(this, 0);
        }

        IMethod method = actBlock.getMethod();

        if (method == null) {
            return getNil();
        }

        INode blockVar = actBlock.getVar(); // ... What exactly is a "var"? A variable?

        if (blockVar != null) {
            // try {
            if (blockVar instanceof ZeroArgNode) {
                if (acheck && value instanceof RubyArray && ((RubyArray) value).getLength() != 0) {
                    throw new ArgumentError(this, "wrong # of arguments (" + ((RubyArray) value).getLength() + " for 0)");
                }
            } else {
                if (!(blockVar instanceof MultipleAsgnNode)) {
                    if (acheck && value instanceof RubyArray && ((RubyArray) value).getLength() == 1) {
                        value = ((RubyArray) value).entry(0);
                    }
                }
                new AssignmentVisitor(this, self).assign(blockVar, value, acheck);
            }
        } else {
            if (acheck && value instanceof RubyArray && ((RubyArray) value).getLength() == 1) {
                value = ((RubyArray) value).entry(0);
            }
        }

        getIterStack().push(actBlock.getIter());

        RubyObject[] args;
        if (value instanceof RubyArray) {
            args = ((RubyArray) value).toJavaArray();
        } else {
            args = new RubyObject[] { value };
        }

        try {
            while (true) {
                try {
                    return method.execute(this, self, null, args, false);
                } catch (RedoJump rExcptn) {
                }
            }
        } catch (NextJump nExcptn) {
            return getNil();
        } catch (ReturnException rExcptn) {
            return rExcptn.getReturnValue();
        } finally {
            getIterStack().pop();
            popClass();
            RubyVarmap.pop(this);

            block.setAct(actBlock);
            getFrameStack().pop();

            setNamespace(oldNamespace);

            // if (ruby_scope->flag & SCOPE_DONT_RECYCLE)
            //    scope_dup(old_scope);
            getScope().setTop(oldScope);
        }
    }

    /** Getter for property rubyTopSelf.
     * @return Value of property rubyTopSelf.
     */
    public RubyObject getRubyTopSelf() {
        return rubyTopSelf;
    }

    /** rb_iterate
     *
     */
    public RubyObject iterate(Callback iterateMethod, RubyObject data1, Callback blockMethod, RubyObject data2) {
        // VALUE self = ruby_top_self;
        getIterStack().push(Iter.ITER_PRE);
        getBlock().push(null, new IterateMethod(blockMethod, data2), getRubyTopSelf());

        try {
            while (true) {
                try {
                    return iterateMethod.execute(data1, null, this);
                } catch (BreakJump bExcptn) {
                    return getNil();
                } catch (ReturnException rExcptn) {
                    return rExcptn.getReturnValue();
                } catch (RetryException rExcptn) {
                }
            }
        } finally {
            getIterStack().pop();
            getBlock().pop();
        }
    }

    /** ruby_init
     *
     */
    public void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        getIterStack().push(Iter.ITER_NOT);
        getFrameStack().push();
        topFrame = getActFrame();

        // rb_origenviron = environ;

        // Init_stack(0);
        // Init_heap();
        getScope().push(); // PUSH_SCOPE();
        // rubyScope.setLocalVars(null);
        topScope = (Scope) getScope().getTop();

        setActMethodScope(Constants.SCOPE_PRIVATE);

        try {
            classes = new RubyClasses(this);
            classes.initCoreClasses();

            RubyGlobal.createGlobals(this);

            exceptions = new RubyExceptions(this);
            exceptions.initDefaultExceptionClasses();

            rubyTopSelf = new RubyObject(this, classes.getObjectClass());

            rubyClass = getClasses().getObjectClass();
            getActFrame().setSelf(rubyTopSelf);
            topNamespace = new Namespace(getClasses().getObjectClass());
            namespace = topNamespace;
            getActFrame().setNamespace(namespace);
            // defineGlobalConstant("TOPLEVEL_BINDING", rb_f_binding(ruby_top_self));
            // ruby_prog_init();
        } catch (Exception excptn) {
            excptn.printStackTrace();
        }

        getScope().pop();
        getScope().push(topScope);
    }

    /** Getter for property rubyScope.
     * @return Value of property rubyScope.
     */
    public ScopeStack getScope() {
        return scope;
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
        return sourceFile;
    }

    /** Setter for property sourceFile.
     * @param sourceFile New value of property sourceFile.
     */
    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    /** Getter for property sourceLine.
     * @return Value of property sourceLine.
     */
    public int getSourceLine() {
        return sourceLine;
    }

    /** Setter for property sourceLine.
     * @param sourceLine New value of property sourceLine.
     */
    public void setSourceLine(int sourceLine) {
        this.sourceLine = sourceLine;
    }

    /** Getter for property verbose.
     * @return Value of property verbose.
     */
    public boolean isVerbose() {
        return verbose;
    }

    public boolean isBlockGiven() {
        return !getActFrame().getIter().isNot();
    }

    public boolean isFBlockGiven() {
        return (getFrameStack().getPrevious() != null) && (!getFrameStack().getPrevious().getIter().isNot());
    }

    public void pushClass(RubyModule newClass) {
        classStack.push(getRubyClass());
        setRubyClass(newClass);
    }

    public void popClass() {
        setRubyClass((RubyModule) classStack.pop());
    }

    /** Setter for property verbose.
     * @param verbose New value of property verbose.
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /** Getter for property dynamicVars.
     * @return Value of property dynamicVars.
     */
    public RubyVarmap getDynamicVars() {
        return dynamicVars;
    }

    /** Setter for property dynamicVars.
     * @param dynamicVars New value of property dynamicVars.
     */
    public void setDynamicVars(RubyVarmap dynamicVars) {
        this.dynamicVars = dynamicVars;
    }

    /** Getter for property rubyClass.
     * @return Value of property rubyClass.
     */
    public RubyModule getRubyClass() {
        return rubyClass;
    }

    /** Setter for property rubyClass.
     * @param rubyClass New value of property rubyClass.
     */
    public void setRubyClass(org.jruby.RubyModule rubyClass) {
        this.rubyClass = rubyClass;
    }

    /** Getter for property inEval.
     * @return Value of property inEval.
     */
    public int getInEval() {
        return inEval;
    }

    /** Setter for property inEval.
     * @param inEval New value of property inEval.
     */
    public void setInEval(int inEval) {
        this.inEval = inEval;
    }

    public FrameStack getFrameStack() {
        return frameStack;
    }

    public Frame getActFrame() {
        return (Frame) getFrameStack().peek();
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

    /** Getter for property iter.
     * @return Value of property iter.
     */
    public IStack getIterStack() {
        return iterStack;
    }

    /** Setter for property iter.
     * @param iter New value of property iter.
     */
    public Iter getActIter() {
        return (Iter) getIterStack().peek();
    }

    /** Getter for property block.
     * @return Value of property block.
     */
    public BlockStack getBlock() {
        return block;
    }

    /** Getter for property cBase.
     * @return Value of property cBase.
     */
    public RubyModule getCBase() {
        return getActFrame().getNamespace().getNamespaceModule();
    }

    /** Setter for property cBase.
     * @param cBase New value of property cBase.
     */
    public void setCBase(RubyModule cBase) {
        getActFrame().getNamespace().setNamespaceModule(cBase);
    }

    public boolean isScope(int scope) {
        return (getActMethodScope() & scope) != 0;
    }

    /** Getter for property actMethodScope.
     * @return Value of property actMethodScope.
     */
    public int getActMethodScope() {
        return actMethodScope;
    }

    /** Setter for property actMethodScope.
     * @param actMethodScope New value of property actMethodScope.
     */
    public void setActMethodScope(int actMethodScope) {
        this.actMethodScope = actMethodScope;
    }

    /** Getter for property wrapper.
     * @return Value of property wrapper.
     */
    public org.jruby.RubyModule getWrapper() {
        return wrapper;
    }

    /** Setter for property wrapper.
     * @param wrapper New value of property wrapper.
     */
    public void setWrapper(org.jruby.RubyModule wrapper) {
        this.wrapper = wrapper;
    }

    /** Getter for property runtime.
     * @return Value of property runtime.
     */
    public org.jruby.runtime.RubyRuntime getRuntime() {
        return this.runtime;
    }

    /** Setter for property runtime.
     * @param runtime New value of property runtime.
     */
    public void setRuntime(org.jruby.runtime.RubyRuntime runtime) {
        this.runtime = runtime;
    }

    /**
     * Gets the exceptions
     * @return Returns a RubyExceptions
     */
    public RubyExceptions getExceptions() {
        return exceptions;
    }

    /** defines a global variable with getter and setter methods
     * 
     * @param name name of the new variable, since it is a global it
     *  should normally start with a $
     * @param value starting value for this variable, this value is used
     *  by the default getter and setter implementation.
     * @param getter the getter method for this variable, if null a default
     *  method which reads the value is used
     * @param setter the setter method for this variable, if null a default
     *  method which writes the value is used
     */
    public void defineHookedVariable(String name, RubyObject value, RubyGlobalEntry.GetterMethod getter, RubyGlobalEntry.SetterMethod setter) {

        RubyGlobalEntry globalEntry = getGlobalEntry(name);

        globalEntry.setInternalData(value);
        globalEntry.setGetter(getter != null ? getter : RubyGlobalEntry.valueMethods);
        globalEntry.setSetter(setter != null ? setter : RubyGlobalEntry.valueMethods);
    }

    /** defines a global variable
     * 
     */
    public void defineVariable(String name, RubyObject value) {
        defineHookedVariable(name, value, null, null);
    }

    /** defines a readonly global variable
     * 
     */
    public void defineReadonlyVariable(String name, RubyObject value) {
        defineHookedVariable(name, value, null, RubyGlobalEntry.readonlySetter);
    }

    /** rb_global_entry
     *
     */
    public RubyGlobalEntry getGlobalEntry(String name) {
        name = name.charAt(0) == '$' ? name : "$" + name;

        //Ruby ruby = id.getRuby();

        RubyGlobalEntry entry = (RubyGlobalEntry) globalMap.get(name);

        if (entry == null) {
            entry = new RubyGlobalEntry(this, name);
            globalMap.put(name, entry);
        }

        return entry;
    }

    /**
     * Init the LOAD_PATH variable.
     * MRI: eval.c:void Init_load()
     * 		from programming ruby
     *			
     *   An array of strings, where each string specifies a directory to be searched
     *   for Ruby scripts and binary extensions used by the load and require 
     *   The initial value is the value of the arguments passed via the -I command-line
     *	 option, followed by an installation-defined standard library location, followed
     *   by the current directory (``.''). This variable may be set from within a program to alter
     *   the default search path; typically, programs use $: &lt;&lt; dir to append dir to the path.
     *   Warning: the ioAdditionalDirectory list will be modified by this process!
     *   @param ioAdditionalDirectory the directory specified on the command line
     *   @fixme: use the version number in some other way than hardcoded here
     *   @fixme: safe level pb here
     **/
    public void initLoad(ArrayList ioAdditionalDirectory) {
        //	don't know what this is used for in MRI, it holds the handle of all loaded libs
        //		ruby_dln_librefs = rb_ary_new();

        // in MRI the ruby installation path is determined from the place where the ruby lib is found
        // of course we can't do that, let's just use the jruby.home property
        String lRubyHome = System.getProperty("jruby.home");
        String lRubyLib = System.getProperty("jruby.lib");
        /*if (lRubyLib == null && lRubyHome != null && lRubyHome.length() != 0) {
            lRubyLib = lRubyHome + File.separatorChar + "lib";
        }*/

        for (int i = ioAdditionalDirectory.size() - 1; i >= 0; i--) {
            ioAdditionalDirectory.set(i, new RubyString(this, (String) ioAdditionalDirectory.get(i)));
        }

        if (lRubyLib != null && lRubyLib.length() != 0) {
            ioAdditionalDirectory.add(new RubyString(this, lRubyLib));
        }

        if (lRubyHome != null && lRubyHome.length() != 0) {
            //FIXME: use the version number in some other way than hardcoded here
            String lRuby = lRubyHome + File.separatorChar + "lib" + File.separatorChar + "ruby" + File.separatorChar;
            String lSiteRuby = lRuby + "site_ruby";
            String lSiteRubyVersion = lSiteRuby + File.separatorChar + RUBY_MAJOR_VERSION;
            String lArch = File.separatorChar + "java";
            String lRubyVersion = lRuby + RUBY_MAJOR_VERSION;

            ioAdditionalDirectory.add(new RubyString(this, lSiteRubyVersion));
            ioAdditionalDirectory.add(new RubyString(this, lSiteRubyVersion + lArch));
            ioAdditionalDirectory.add(new RubyString(this, lSiteRuby));
            ioAdditionalDirectory.add(new RubyString(this, lRubyVersion));
            ioAdditionalDirectory.add(new RubyString(this, lRubyVersion + lArch));
        }

        //FIXME: safe level pb here
        ioAdditionalDirectory.add(new RubyString(this, "."));

        RubyArray loadPath = (RubyArray) getGlobalVar("$:");
        loadPath.getList().addAll(ioAdditionalDirectory);
    }

    /**
     * this method uses the appropriate lookup strategy to find a file.
     * It is used by Kernel#require.
     * NOTE: this is only public for unit testing reasons.
     * 		 it should have package (default) protection
     *  (matz Ruby: rb_find_file)
     *  @param ruby the ruby interpreter
     *  @param i2find the file to find, this is a path name
     *  @return the correct file
     */
    public File findFile(Ruby ruby, File i2find) {
        RubyArray lLoadPath = (RubyArray) getGlobalVar("$:");
        int lPathNb = lLoadPath.getLength();
        String l2Find = i2find.getPath();
        for (int i = 0; i < lPathNb; i++) {
            String lCurPath = ((RubyString) lLoadPath.entry(i)).getValue();
            File lCurFile = new File(lCurPath + File.separatorChar + l2Find);
            if (lCurFile.exists()) {
                i2find = lCurFile;
                break;
            }
        }
        if (i2find.exists()) {
            return i2find;
        } else {
            //try to load the file from a resource
            throw new LoadError(ruby, "No such file to load -- " + i2find.getPath());
            // throw new RuntimeException("file " + i2find.getPath() + " can't be found!");
        }
    }

    /**
     * @fixme
     **/
    public INode compile(String content, String file, int line) {
        // FIXME
        RubyParserConfiguration config = new RubyParserConfiguration();

        config.setLocalVariables(getScope().getLocalNames());

        getParser().init(config);

        IRubyParserResult result = (IRubyParserResult) getParser().parse(LexerFactory.getInstance().getSource(file, content));

        if (result.getLocalVariables() != null) {
            getScope().setLocalNames(new ArrayList(result.getLocalVariables()));
            if (getScope().getLocalNames() != null && getScope().getLocalNames().size() > 0) {
                if (getScope().getLocalValues() == null) {
                    getScope().setLocalValues(new ArrayList(Collections.nCopies(getScope().getLocalNames().size(), getNil())));
                } else {
                    getScope().getLocalValues().addAll(Collections.nCopies(getScope().getLocalNames().size() - getScope().getLocalValues().size(), getNil()));
                }
            }
        }

        return result.getAST();
    }

    public RubyObject getLastline() {
        if (getScope().getLocalValues() != null) {
            return getScope().getValue(0);
        }
        return RubyString.nilString(this);
    }

    public void setLastline(RubyObject value) {
        if (getScope().getLocalValues() == null) {
            getScope().setLocalValues(new ArrayList(Collections.nCopies(2, getNil())));
            getScope().setLocalNames(new ArrayList(Arrays.asList(new String[] { "_", "~" })));
        }
        getScope().setValue(0, value);
    }

    public RubyObject getBackref() {
        if (getScope().getLocalValues() != null) {
            return getScope().getValue(1);
        }
        return getNil();
    }

    public void setBackref(RubyObject match) {
        if (getScope().getLocalValues() == null) {
            getScope().setLocalValues(new ArrayList(Collections.nCopies(2, getNil())));
            getScope().setLocalNames(new ArrayList(Arrays.asList(new String[] { "_", "~" })));
        }
        getScope().setValue(1, match);
    }

    /**
     * Gets the parser.
     * @return Returns a IParser
     */
    public IParser getParser() {
        if (parser == null) {
            parser = new DefaultRubyParser();
            parser.setErrorHandler(new RubyErrorHandler(this, verbose));
        }
        return parser;
    }
}
