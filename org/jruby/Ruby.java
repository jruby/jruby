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

import java.util.*;
import java.io.File;

import org.jruby.exceptions.*;
import org.jruby.javasupport.*;
import org.jruby.nodes.*;
import org.jruby.nodes.types.*;
import org.jruby.parser.*;
import org.jruby.runtime.*;
import org.jruby.util.*;

/**
 * The jruby runtime.
 *
 * @author  jpetersen
 * @version $Revision$
 * @since   0.1
 */
public final class Ruby {
    public static final String RUBY_VERSION = "1.6";

    public static final int FIXNUM_CACHE_MAX = 0xff;
    public RubyFixnum[] fixnumCache = new RubyFixnum[FIXNUM_CACHE_MAX + 1];

    private RubyMethodCache methodCache;

    /** rb_global_tbl
     *
     */
    private RubyMap globalMap;

    public LinkedList objectSpace = new LinkedList();

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
    private ParserHelper parserHelper = null;
    private RubyParser rubyParser = null;
    private RubyRuntime runtime = new RubyRuntime(this);

    private RubyObject rubyTopSelf;

    // Eval
    private RubyScope rubyScope = new RubyScope(this);
    private RubyScope topScope = null;
    private RubyVarmap dynamicVars = null;
    private RubyModule rubyClass = null;

    private RubyFrame rubyFrame;
    private RubyFrame topFrame;

    private CRefNode cRef = new CRefNode(null, null);
    private CRefNode topCRef;

    private String sourceFile;
    private int sourceLine;

    private int inEval;

    private boolean verbose;

    // 
    private RubyIter iter;
    private RubyBlock block = new RubyBlock(this);

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
        Ruby ruby = new Ruby();
        ruby.setRegexpAdapterClass(regexpAdapterClass);
        ruby.init();
        return ruby;
    }

    /**
     * Evaluates a Java script. And return an object of class returnClass.
     * 
     * @param script The script to evaluate
     * @param returnClass The class which should be returned
     * @return the result Object
     */
    public Object evalScript(String script, Class returnClass) {
        RubyObject result = getRubyTopSelf().eval(getRubyParser().compileJavaString("<script>", script, script.length(), 1));
        return JavaUtil.convertRubyToJava(this, result, returnClass);
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
        return (RubyModule) classes.getClassMap().get(name);
    }

    /** Returns a class from the instance pool.
     * 
     * @param name The name of the class.
     * @return The class.
     */
    public RubyClass getRubyClass(String name) {
        return (RubyClass) classes.getClassMap().get(name);
    }

    /** rb_define_class / rb_define_class_id
     *
     */
    public RubyClass defineClass(String name, RubyClass superClass) {
        if (superClass == null) {
            superClass = getClasses().getObjectClass();
        }

        RubyClass newClass = RubyClass.newClass(this, superClass);
        newClass.setName(name);

        newClass.setRubyClass(superClass.getRubyClass().newSingletonClass());
        newClass.getRubyClass().attachSingletonClass(newClass);

        superClass.funcall("inherited", newClass);

        classes.getClassMap().put(name, newClass);

        return newClass;
    }

    /** rb_define_module / rb_define_module_id
     *
     */
    public RubyModule defineModule(String name) {
        RubyModule newModule = RubyModule.newModule(this);
        newModule.setName(name);

        getClasses().getClassMap().put(name, newModule);

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
            throw new RubySecurityException(
                this,
                "Insecure operation '" + getRubyFrame().getLastFunc() + "' at level " + safeLevel);
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
        if (getClasses().getClassMap().get(id) != null) {
            return (RubyObject) getClasses().getClassMap().get(id);
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
    public boolean isAutoloadDefined(String id) {
        return false;
    }

    public boolean isClassDefined(String id) {
        return false;
    }

    /* public RubyInterpreter getInterpreter() {
        if (rubyInterpreter == null) {
            rubyInterpreter = new RubyInterpreter(this);
        }
        return rubyInterpreter;
    }*/

    /** Getter for property globalMap.
     * @return Value of property globalMap.
     */
    public RubyMap getGlobalMap() {
        return globalMap;
    }

    /** Setter for property globalMap.
     * @param globalMap New value of property globalMap.
     */
    public void setGlobalMap(RubyMap globalMap) {
        this.globalMap = globalMap;
    }

    /** rb_gv_set
     *
     */
    public RubyObject setGlobalVar(String name, RubyObject value) {
        return getGlobalEntry(name).set(value);
    }

    /** rb_gv_get
     *
     */
    public RubyObject getGlobalVar(String name) {
        return getGlobalEntry(name).get();
    }

    public RubyObject yield(RubyObject value) {
        return yield0(value, null, null, false);
    }

    public RubyObject yield0(RubyObject value, RubyObject self, RubyModule klass, boolean acheck) {
        RubyObject result = getNil();

        if (!(isBlockGiven() || isFBlockGiven()) || (block == null)) {
            throw new RuntimeException("yield called out of block");
        }

        RubyVarmap.push(this);
        pushClass();
        RubyBlock tmpBlock = block.getTmp();

        RubyFrame frame = new RubyFrame(tmpBlock.frame); // block.frame;
        frame.setPrev(getRubyFrame());
        setRubyFrame(frame);

        CRefNode oldCRef = getCRef();
        setCRef(getRubyFrame().getCbase());

        RubyScope oldScope = getRubyScope();
        setRubyScope(tmpBlock.scope);
        block.pop();

        if ((block.flags & RubyBlock.BLOCK_D_SCOPE) != 0) {
            setDynamicVars(new RubyVarmap(null, null, tmpBlock.dynamicVars));
        } else {
            setDynamicVars(block.dynamicVars);
        }

        setRubyClass((klass != null) ? klass : tmpBlock.klass);
        if (klass == null) {
            self = tmpBlock.self;
        }

        Node node = tmpBlock.body;

        if (tmpBlock.var != null) {
            // try {
            if (tmpBlock.var == Node.ONE) {
                if (acheck && value != null && value instanceof RubyArray && ((RubyArray) value).getLength() != 0) {

                    throw new RubyArgumentException(this, "wrong # of arguments (" + ((RubyArray) value).getLength() + " for 0)");
                }
            } else {
                if (!(tmpBlock.var instanceof MAsgnNode)) {
                    if (acheck && value != null && value instanceof RubyArray && ((RubyArray) value).getLength() == 1) {

                        value = ((RubyArray) value).entry(0);
                    }
                }
                ((AssignableNode) tmpBlock.var).assign(this, self, value, acheck);
            }
            // } catch () {
            //    goto pop_state;
            // }

        } else {
            if (acheck && value != null && value instanceof RubyArray && ((RubyArray) value).getLength() == 1) {

                value = ((RubyArray) value).entry(0);
            }
        }

        iter.push(tmpBlock.iter);
        while (true) {
            try {
                if (node == null) {
                    result = getNil();
                } else if (node instanceof ExecutableNode) {
                    if (value == null) {
                        value = RubyArray.newArray(this, 0);
                    }
                    result = ((ExecutableNode) node).execute(value, new RubyObject[] { node.getTValue(), self }, this);
                } else {
                    result = node.eval(this, self);
                }
                break;
            } catch (RedoException rExcptn) {
            } catch (NextException nExcptn) {
                result = getNil();
                break;
            } catch (BreakException bExcptn) {
                break;
            } catch (ReturnException rExcptn) {
                break;
            }
        }

        // pop_state:

        iter.pop();
        popClass();
        RubyVarmap.pop(this);

        block.setTmp(tmpBlock);
        setRubyFrame(getRubyFrame().getPrev());

        setCRef(oldCRef);

        // if (ruby_scope->flag & SCOPE_DONT_RECYCLE)
        //    scope_dup(old_scope);
        setRubyScope(oldScope);

        /*
         * if (state) {
         *    if (!block->tag) {
         *       switch (state & TAG_MASK) {
         *          case TAG_BREAK:
         *          case TAG_RETURN:
         *             jump_tag_but_local_jump(state & TAG_MASK);
         *             break;
         *       }
         *    }
         *    JUMP_TAG(state);
         * }
         */

        return result;
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
        Node node = new NodeFactory(this).newIFunc(blockMethod, data2);

        // VALUE self = ruby_top_self;
        RubyObject result = null;

        getIter().push(RubyIter.ITER_PRE);
        getBlock().push(null, node, getRubyTopSelf());

        while (true) {
            try {
                result = iterateMethod.execute(data1, null, this);

                break;
            } catch (RetryException rExcptn) {
            } catch (BreakException bExcptn) {
                result = getNil();
                break;
            } catch (ReturnException rExcptn) {
                result = rExcptn.getReturnValue();
                break;
            }
        }
        getIter().pop();
        getBlock().pop();

        return result;
    }

    private void createFixnumCache() {
        for (int i = 0; i <= FIXNUM_CACHE_MAX; i++) {
            fixnumCache[i] = new RubyFixnum(this, i);
        }
    }

    private void callInits() {
        classes = new RubyClasses(this);
        classes.initCoreClasses();

        exceptions = new RubyExceptions(this);
        exceptions.initDefaultExceptionClasses();

        rubyTopSelf = new RubyObject(this, classes.getObjectClass());
        /*rubyTopSelf.defineSingletonMethod("to_s", new RubyCallbackMethod() {
            public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                return RubyString.m_newString(ruby, "main");
            }
        });*/

        createFixnumCache();
    }

    /** ruby_init
     *
     */
    public void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        setIter(new RubyIter()); // ruby_iter = &iter;
        rubyFrame = topFrame = new RubyFrame(this);

        // rb_origenviron = environ;

        // Init_stack(0);
        // Init_heap();
        rubyScope.push(); // PUSH_SCOPE();
        // rubyScope.setLocalVars(null);
        topScope = rubyScope;

        setActMethodScope(Constants.SCOPE_PRIVATE);

        try {
            callInits();

            rubyClass = getClasses().getObjectClass();
            rubyFrame.setSelf(rubyTopSelf);
            topCRef = new CRefNode(getClasses().getObjectClass(), null);
            cRef = topCRef;
            rubyFrame.setCbase(cRef);
            // defineGlobalConstant("TOPLEVEL_BINDING", rb_f_binding(ruby_top_self));
            // ruby_prog_init();
        } catch (Exception excptn) {
            excptn.printStackTrace(getRuntime().getErrorStream());
        }

        rubyScope.pop();
        rubyScope = topScope;
    }

    /** Getter for property rubyScope.
     * @return Value of property rubyScope.
     */
    public RubyScope getRubyScope() {
        return rubyScope;
    }

    /** Setter for property rubyScope.
     * @param rubyScope New value of property rubyScope.
     */
    public void setRubyScope(RubyScope rubyScope) {
        this.rubyScope = rubyScope;
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
        return getRubyFrame().getIter() != RubyIter.ITER_NOT;
    }

    public boolean isFBlockGiven() {
        return (getRubyFrame().getPrev() != null) && (getRubyFrame().getPrev().getIter() != RubyIter.ITER_NOT);
    }

    public void pushClass() {
        classStack.push(getRubyClass());
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

    /** Getter for property parserHelper.
     * @return Value of property parserHelper.
     */
    public ParserHelper getParserHelper() {
        if (parserHelper == null) {
            parserHelper = new ParserHelper(this);
            parserHelper.init();
        }
        return parserHelper;
    }

    public RubyParser getRubyParser() {
        if (rubyParser == null) {
            rubyParser = new DefaultRubyParser(this);
        }
        return rubyParser;
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

    /** Getter for property rubyFrame.
     * @return Value of property rubyFrame.
     */
    public RubyFrame getRubyFrame() {
        return rubyFrame;
    }

    /** Setter for property rubyFrame.
     * @param rubyFrame New value of property rubyFrame.
     */
    public void setRubyFrame(RubyFrame rubyFrame) {
        this.rubyFrame = rubyFrame;
    }

    /** Getter for property topFrame.
     * @return Value of property topFrame.
     */
    public RubyFrame getTopFrame() {
        return topFrame;
    }

    /** Setter for property topFrame.
     * @param topFrame New value of property topFrame.
     */
    public void setTopFrame(RubyFrame topFrame) {
        this.topFrame = topFrame;
    }

    /** Getter for property rubyCRef.
     * @return Value of property rubyCRef.
     */
    public CRefNode getCRef() {
        return cRef;
    }

    /** Setter for property rubyCRef.
     * @param rubyCRef New value of property rubyCRef.
     */
    public void setCRef(CRefNode newCRef) {
        cRef = newCRef;
    }

    /** Getter for property topCRef.
     * @return Value of property topCRef.
     */
    public CRefNode getTopCRef() {
        return topCRef;
    }

    /** Setter for property topCRef.
     * @param topCRef New value of property topCRef.
     */
    public void setTopCRef(CRefNode topCRef) {
        this.topCRef = topCRef;
    }

    public JavaSupport getJavaSupport() {
        return javaSupport;
    }

    /** Getter for property iter.
     * @return Value of property iter.
     */
    public RubyIter getIter() {
        return iter;
    }

    /** Setter for property iter.
     * @param iter New value of property iter.
     */
    public void setIter(RubyIter iter) {
        this.iter = iter;
    }

    /** Getter for property block.
     * @return Value of property block.
     */
    public RubyBlock getBlock() {
        return block;
    }

    /** Setter for property block.
     * @param block New value of property block.
     */
    public void setBlock(RubyBlock block) {
        this.block = block;
    }

    /** Getter for property cBase.
     * @return Value of property cBase.
     */
    public RubyModule getCBase() {
        return cBase;
    }

    /** Setter for property cBase.
     * @param cBase New value of property cBase.
     */
    public void setCBase(RubyModule cBase) {
        this.cBase = cBase;
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
     */
    public void defineHookedVariable(
        String name,
        RubyObject value,
        RubyGlobalEntry.GetterMethod getter,
        RubyGlobalEntry.SetterMethod setter) {

        RubyGlobalEntry globalEntry = getGlobalEntry(name);

        globalEntry.setData(value);
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

    public void defineVirtualVariable(
        String name,
        RubyGlobalEntry.GetterMethod getter,
        RubyGlobalEntry.SetterMethod setter) {
        getter = getter != null ? getter : RubyGlobalEntry.valueMethods;
        setter = setter != null ? setter : RubyGlobalEntry.readonlySetter;

        defineHookedVariable(name, null, getter, setter);
    }

    /** rb_global_entry
     *
     */
    public RubyGlobalEntry getGlobalEntry(String name) {
        name = name.charAt(0) == '$' ? name : "$" + name;

        //Ruby ruby = id.getRuby();

        RubyGlobalEntry entry = (RubyGlobalEntry) getGlobalMap().get(name);

        if (entry == null) {
            entry = new RubyGlobalEntry(this, name);
            getGlobalMap().put(name, entry);
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
     **/
    public void initLoad(ArrayList ioAdditionalDirectory) {
        //	don't know what this is used for in MRI, it holds the handle of all loaded libs
        //		ruby_dln_librefs = rb_ary_new();

        //in MRI the ruby installation path is determined from the place where the ruby lib is found
        //of course we can't do that, let's just use the org.jruby.HOME property
        String lRubyHome = System.getProperty("jruby.home");
        String lRubyLib = System.getProperty("jruby.lib");

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
            String lSiteRubyVersion = lSiteRuby + File.separatorChar + RUBY_VERSION;
            String lArch = File.separatorChar + "JAVA";
            String lRubyVersion = lRuby + RUBY_VERSION;

            ioAdditionalDirectory.add(new RubyString(this, lSiteRubyVersion));
            ioAdditionalDirectory.add(new RubyString(this, lSiteRubyVersion + lArch));
            ioAdditionalDirectory.add(new RubyString(this, lSiteRuby));
            ioAdditionalDirectory.add(new RubyString(this, lRubyVersion));
            ioAdditionalDirectory.add(new RubyString(this, lRubyVersion + lArch));
        }

        //FIXME: safe level pb here
        ioAdditionalDirectory.add(new RubyString(this, "."));
        RubyArray rb_load_path = new RubyArray(this, ioAdditionalDirectory);
        defineReadonlyVariable("$:", rb_load_path);
        defineReadonlyVariable("$-I", rb_load_path);
        defineReadonlyVariable("$LOAD_PATH", rb_load_path);
        RubyArray rb_features = new RubyArray(this);
        defineReadonlyVariable("$\"", rb_features);
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
    public File findFile(File i2find) {
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
            throw new RuntimeException("file " + i2find.getPath() + " can't be found!");
        }
    }
}