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
import org.jruby.original.*;
import org.jruby.interpreter.*;
import org.jruby.util.*;

/**
 * The jruby runtime.
 *
 * @author  jpetersen
 */
public final class Ruby implements token {
    public static final int FIXNUM_CACHE_SIZE = 0xff;
    public static final boolean AUTOMATIC_BIGNUM_CAST = true;
    
    public RubyFixnum[] fixnumCache = new RubyFixnum[FIXNUM_CACHE_SIZE];
    
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
    
    private RubyObject rubyTopSelf;
    
    private RubyOriginalMethods originalMethods;
    
    private RubyScope rubyScope = new RubyScope();
    private RubyVarmap dynamicVars = null;
    
    private String sourceFile;
    private int sourceLine;
    
    private boolean verbose;
    
    public op_tbl[] op_tbl;
    
    /** Create and initialize a new jruby Runtime.
     */    
    public Ruby() {
        initOperatorTable();
        
        originalMethods = new RubyOriginalMethods(this);
        
        globalMap = new RubyHashMap();
        
        nilObject = new RubyNil(this);
        trueObject = new RubyBoolean(this, true);
        falseObject = new RubyBoolean(this, false);
        
        classes = new RubyClasses(this);
        classes.initCoreClasses();
        
        createFixnumCache();
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
    public boolean isAutoloadDefined(ID id) {
        return false;
    }
    
    public RubyOriginalMethods getOriginalMethods() {
        return this.originalMethods;
    }
    
    public RubyId intern(String name) {
        return (RubyId)ID.rb_intern(name, this);
    }
    
    public boolean isClassDefined(RubyId id) {
        return false;
    }
    
    public RubyId toId(RubyObject name) {
        return null;
    }
    
    // Compatibility
    
    public ID createId(int value) {
        return new RubyId(this, value);
    }
    
    public RubyInterpreter getInterpreter() {
        if (rubyInterpreter == null) {
            rubyInterpreter = new RubyInterpreter(this);
        }
        return rubyInterpreter;
    }
    
    // --
    
    public static class op_tbl {
        public RubyId token;
        public String name;

        private op_tbl(Ruby ruby, int token, String name) {
            this.token = new RubyId(ruby, token);
            this.name = name;
        }
    }

    private void initOperatorTable() {
        op_tbl = new op_tbl[] {
            new op_tbl(this, tDOT2,	".."),
            new op_tbl(this, tDOT3,	"..."),
            new op_tbl(this, '+',	"+"),
            new op_tbl(this, '-',	"-"),
            new op_tbl(this, '+',	"+(binary)"),
            new op_tbl(this, '-',	"-(binary)"),
            new op_tbl(this, '*',	"*"),
            new op_tbl(this, '/',	"/"),
            new op_tbl(this, '%',	"%"),
            new op_tbl(this, tPOW,	"**"),
            new op_tbl(this, tUPLUS,	"+@"),
            new op_tbl(this, tUMINUS,	"-@"),
            new op_tbl(this, tUPLUS,	"+(unary)"),
            new op_tbl(this, tUMINUS,	"-(unary)"),
            new op_tbl(this, '|',	"|"),
            new op_tbl(this, '^',	"^"),
            new op_tbl(this, '&',	"&"),
            new op_tbl(this, tCMP,	"<=>"),
            new op_tbl(this, '>',	">"),
            new op_tbl(this, tGEQ,	">="),
            new op_tbl(this, '<',	"<"),
            new op_tbl(this, tLEQ,	"<="),
            new op_tbl(this, tEQ,	"=="),
            new op_tbl(this, tEQQ,	"==="),
            new op_tbl(this, tNEQ,      "!="),
            new op_tbl(this, tMATCH,    "=~"),
            new op_tbl(this, tNMATCH,   "!~"),
            new op_tbl(this, '!',       "!"),
            new op_tbl(this, '~',       "~"),
            new op_tbl(this, '!',       "!(unary)"),
            new op_tbl(this, '~',       "~(unary)"),
            new op_tbl(this, '!',       "!@"),
            new op_tbl(this, '~',       "~@"),
            new op_tbl(this, tAREF,     "[]"),
            new op_tbl(this, tASET,     "[]="),
            new op_tbl(this, tLSHFT,    "<<"),
            new op_tbl(this, tRSHFT,    ">>"),
            new op_tbl(this, tCOLON2,   "::"),
            new op_tbl(this, '`',       "`"),
            new op_tbl(this, 0,         null),
        };
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
        for (int i = 0; i < FIXNUM_CACHE_SIZE; i++) {
            fixnumCache[i] = new RubyFixnum(this, i);
        }
    }
    
    /** Getter for property rubyScope.
     * @return Value of property rubyScope.
     */
    public org.jruby.interpreter.RubyScope getRubyScope() {
        return rubyScope;
    }
    
    /** Setter for property rubyScope.
     * @param rubyScope New value of property rubyScope.
     */
    public void setRubyScope(org.jruby.interpreter.RubyScope rubyScope) {
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
    
}