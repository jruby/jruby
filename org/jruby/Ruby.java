/*
 * Ruby.java - No description
 * Created on 04. Juli 2001, 22:53
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
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

import java.lang.reflect.*;
import java.util.*;
import java.io.*;

import org.jruby.core.*;
import org.jruby.interpreter.*;
import org.jruby.interpreter.nodes.*;
import org.jruby.original.*;
import org.jruby.parser.*;
import org.jruby.util.*;

/**
 * The jruby runtime.
 *
 * @author  jpetersen
 */
public final class Ruby {
    public static final int FIXNUM_CACHE_MAX = 0xff;
    public static final boolean AUTOMATIC_BIGNUM_CAST = true;
    
    public RubyFixnum[] fixnumCache = new RubyFixnum[FIXNUM_CACHE_MAX + 1];
    
    private HashMap methodCache = new HashMap();
    
    /** rb_global_tbl
     *
     */
    private RubyMap globalMap;
    
    private int securityLevel = 0;
    
    private RubyInterpreter rubyInterpreter = null;

    // Default objects
    private RubyNil nilObject;
    private RubyBoolean trueObject;
    private RubyBoolean falseObject;
    
    // Default classes
    private RubyClasses classes;
    
    //
    private ParserHelper parserHelper = null;
    private RubyParser rubyParser = null;
    
    private RubyObject rubyTopSelf;
    
    // Eval
    
    private RubyScope rubyScope = new RubyScope();
    private RubyScope topScope = null;
    private RubyVarmap dynamicVars = null;
    private RubyModule rubyClass = null;
    
    private RubyFrame rubyFrame;
    private RubyFrame topFrame;
    
    private NODE rubyCRef;
    private NODE topCRef;
    
    private String sourceFile;
    private int sourceLine;
    
    private int inEval;
    
    private boolean verbose;
    
    // ID
    private Map symbolTable = new HashMap(200);
    private Map symbolReverseTable = new HashMap(200);
    private RubyOperatorEntry[] operatorTable = null;
    private int lastId = Token.LAST_TOKEN;
    
    // init
    private boolean initialized = false;
    
    // Java support
    private ClassLoader javaClassLoader = ClassLoader.getSystemClassLoader();
 
 	// plugable Regexp engine	
	private Class regexpAdapterClass = GNURegexpAdapter.class;
    /** Create and initialize a new jruby Runtime.
     */    
    public Ruby() {
        RubyOperatorEntry.initOperatorTable(this);
        
        globalMap = new RubyHashMap();
        
        nilObject = new RubyNil(this);
        trueObject = new RubyBoolean(this, true);
        falseObject = new RubyBoolean(this, false);
    }
   
	public Class getRegexpAdapterClass()
	{
		return regexpAdapterClass;
	}

	public void setRegexpAdapterClass(Class iRegexpAdapterClass)
	{
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
    public RubyNil getNil() {
        return nilObject;
    }
    
    /** Returns a class from the instance pool.
     * @param name The name of the class.
     * @return The class.
     */    
    public RubyModule getRubyClass(String name) {
        return (RubyModule)classes.getClassMap().get(intern(name));
    }
    
    /** rb_define_class
     *
     */
    public RubyClass defineClass(String name, RubyClass superClass) {
        RubyClass newClass = defineClassId(intern(name), superClass);
        
        classes.getClassMap().put(intern(name), newClass);
        
        return newClass;
    }
    
    /** rb_define_class_id
     *
     */
    public RubyClass defineClassId(RubyId id, RubyClass superClass) {
        if (superClass == null) {
            superClass = getClasses().getObjectClass();
        }
        
        RubyClass newClass = RubyClass.m_newClass(this, superClass);
        newClass.setName(id);
        
        newClass.setRubyClass(((RubyClass)superClass.getRubyClass()).newSingletonClass());
        ((RubyClass)newClass.getRubyClass()).attachSingletonClass(newClass);
        
        superClass.funcall(intern("inherited"), newClass);
        
        return newClass;
    }
    
    /** rb_define_module
     *
     */
    public RubyModule defineModule(String name) {
        RubyModule newModule = defineModuleId(intern(name));
        
        getClasses().getClassMap().put(intern(name), newModule);
        
        return newModule;
    }
    
    /** rb_define_module_id
     *
     */
    public RubyModule defineModuleId(RubyId id) {
        RubyModule newModule = RubyModule.m_newModule(this);
        newModule.setName(id);
        
        return newModule;
    }
    
    /** rb_define_global_function
     *
     */
    public void defineGlobalFunction(String name, RubyCallbackMethod method) {
        getClasses().getKernelModule().defineModuleFunction(name, method);
    }
    
    /** Getter for property securityLevel.
     * @return Value of property securityLevel.
     */
    public int getSecurityLevel() {
        return this.securityLevel;
    }
    
    /** Setter for property securityLevel.
     * @param securityLevel New value of property securityLevel.
     */
    public void setSecurityLevel(int securityLevel) {
        this.securityLevel = securityLevel;
    }
    public void secure(int security) {
    }
    
    /*public void defineGlobalConst(String name, RubyObject obj) {
    }*/
    
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
    public RubyObject getTopConstant(RubyId id) {
        if (getClasses().getClassMap().get(id) != null) {
            return (RubyObject)getClasses().getClassMap().get(id);
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
    public boolean isAutoloadDefined(RubyId id) {
        return false;
    }
    
    public RubyId intern(String name) {
        return RubyId.intern(this, name);
    }
    
    public boolean isClassDefined(RubyId id) {
        return false;
    }
    
    public RubyId toId(RubyObject name) {
        return null;
    }
    
    // Compatibility
    
    public RubyId createId(int value) {
        return RubyId.newId(this, value);
    }
    
    public RubyInterpreter getInterpreter() {
        if (rubyInterpreter == null) {
            rubyInterpreter = new RubyInterpreter(this);
        }
        return rubyInterpreter;
    }
    
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
        return RubyGlobalEntry.getGlobalEntry(intern(name)).set(value);
    }
    
    /** rb_gv_get
     *
     */
    public RubyObject getGlobalVar(String name) {
        return RubyGlobalEntry.getGlobalEntry(intern(name)).get();
    }
    
    public RubyObject yield(RubyObject value) {
        return getInterpreter().yield0(value, null, null, false);
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
    public RubyObject iterate(RubyCallbackMethod iterateMethod, RubyObject data1, RubyCallbackMethod blockMethod, RubyObject data2) {
        NODE node = NODE.newIFunc(blockMethod, data2);
        
        // VALUE self = ruby_top_self;
        RubyObject result = null;
        
        getInterpreter().getRubyIter().push(Iter.ITER_PRE);
        getInterpreter().getRubyBlock().push(null, node, getRubyTopSelf());
        
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
        getInterpreter().getRubyIter().pop();
        getInterpreter().getRubyBlock().pop();
        
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
        
        getInterpreter().setRubyIter(new Iter()); // ruby_iter = &iter;
        rubyFrame = topFrame = new RubyFrame(this);
        
        // rb_origenviron = environ;
        
        // Init_stack(0);
        // Init_heap();
        rubyScope.push(); // PUSH_SCOPE();
        rubyScope.setLocalVars(null);
        rubyScope.setLocalTbl(null);
        topScope = rubyScope;
    
        getInterpreter().setActMethodScope(Scope.SCOPE_PRIVATE);

        try {
            callInits();
            
            rubyClass = getClasses().getObjectClass();
            rubyFrame.setSelf(rubyTopSelf);
            topCRef = new NODE(NODE.NODE_CREF, getClasses().getObjectClass(), null, null);
            rubyCRef = topCRef;
            rubyFrame.setCbase(rubyCRef);
            // defineGlobalConstant("TOPLEVEL_BINDING", rb_f_binding(ruby_top_self));
            // ruby_prog_init();
        } catch (Exception excptn) {
            // error_print();
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
    public HashMap getMethodCache() {
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
    
    /** Setter for property verbose.
     * @param verbose New value of property verbose.
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    
    /** Getter for property dynamicVars.
     * @return Value of property dynamicVars.
     */
    public org.jruby.interpreter.RubyVarmap getDynamicVars() {
        return dynamicVars;
    }
    
    /** Setter for property dynamicVars.
     * @param dynamicVars New value of property dynamicVars.
     */
    public void setDynamicVars(org.jruby.interpreter.RubyVarmap dynamicVars) {
        this.dynamicVars = dynamicVars;
    }
    
    /** Getter for property rubyClass.
     * @return Value of property rubyClass.
     */
    public org.jruby.RubyModule getRubyClass() {
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
    public org.jruby.interpreter.RubyFrame getRubyFrame() {
        return rubyFrame;
    }
    
    /** Setter for property rubyFrame.
     * @param rubyFrame New value of property rubyFrame.
     */
    public void setRubyFrame(org.jruby.interpreter.RubyFrame rubyFrame) {
        this.rubyFrame = rubyFrame;
    }
    
    /** Getter for property topFrame.
     * @return Value of property topFrame.
     */
    public org.jruby.interpreter.RubyFrame getTopFrame() {
        return topFrame;
    }
    
    /** Setter for property topFrame.
     * @param topFrame New value of property topFrame.
     */
    public void setTopFrame(org.jruby.interpreter.RubyFrame topFrame) {
        this.topFrame = topFrame;
    }
    
    /** Getter for property rubyCRef.
     * @return Value of property rubyCRef.
     */
    public org.jruby.original.NODE getRubyCRef() {
        return rubyCRef;
    }
    
    /** Setter for property rubyCRef.
     * @param rubyCRef New value of property rubyCRef.
     */
    public void setRubyCRef(org.jruby.original.NODE rubyCRef) {
        this.rubyCRef = rubyCRef;
    }
    
    /** Getter for property topCRef.
     * @return Value of property topCRef.
     */
    public org.jruby.original.NODE getTopCRef() {
        return topCRef;
    }
    
    /** Setter for property topCRef.
     * @param topCRef New value of property topCRef.
     */
    public void setTopCRef(org.jruby.original.NODE topCRef) {
        this.topCRef = topCRef;
    }
    
    /** Gets the value of the last_line variable, $_
     * @return Contents of $_, or Nil if $_ hasn't been set in this scope.
     */
    public RubyObject getLastLine() {
        return getRubyScope().getLocalVars(0);
    }
    
    /** Gets the value of the last-match variable, $~
     * @return Contents of $~, or Nil if $~ hasn't been set in this scope.
     */
    public RubyObject getBackRef() {
        return getRubyScope().getLocalVars(1);
    }
    
    /** Sets the value of the last-match variable, $~.
     * @param match  The new value of $~
     */
    public void setBackRef(RubyObject match) {
        getRubyScope().setLocalVars(1, match);
    }
    
    /** Getter for property symbolTable.
     * @return Value of property symbolTable.
     */
    public Map getSymbolTable() {
        return symbolTable;
    }
    
    /** Setter for property symbolTable.
     * @param symbolTable New value of property symbolTable.
     */
    public void setSymbolTable(Map symbolTable) {
        this.symbolTable = symbolTable;
    }
    
    /** Getter for property symbolReverseTable.
     * @return Value of property symbolReverseTable.
     */
    public Map getSymbolReverseTable() {
        return symbolReverseTable;
    }
    
    /** Setter for property symbolReverseTable.
     * @param symbolReverseTable New value of property symbolReverseTable.
     */
    public void setSymbolReverseTable(Map symbolReverseTable) {
        this.symbolReverseTable = symbolReverseTable;
    }
    
    /** Getter for property operatorTable.
     * @return Value of property operatorTable.
     */
    public RubyOperatorEntry[] getOperatorTable() {
        return operatorTable;
    }
    
    /** Setter for property operatorTable.
     * @param operatorTable New value of property operatorTable.
     */
    public void setOperatorTable(RubyOperatorEntry[] operatorTable) {
        this.operatorTable = operatorTable;
    }
    
    /** Getter for property lastId.
     * @return Value of property lastId.
     */
    public int getLastId() {
        return lastId;
    }
    
    /** Setter for property lastId.
     * @param lastId New value of property lastId.
     */
    public void setLastId(int lastId) {
        this.lastId = lastId;
    }
    
    /** Getter for property javaClassLoader.
     * @return Value of property javaClassLoader.
     */
    public ClassLoader getJavaClassLoader() {
        return javaClassLoader;
    }
    
    /** Setter for property javaClassLoader.
     * @param javaClassLoader New value of property javaClassLoader.
     */
    public void setJavaClassLoader(ClassLoader javaClassLoader) {
        this.javaClassLoader = javaClassLoader;
    }
}
