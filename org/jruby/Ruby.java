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

import java.util.*;

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
    /** rb_class_tbl
     *
     */
    private RubyMap classMap;
    
    /** rb_global_tbl
     *
     */
    private RubyMap globalMap;
    
    private int securityLevel = 0;
    
    private RubyInterpreter rubyInterpreter = null;

    // Default objects and classes.
    
    private RubyNil nilObject;
    private RubyBoolean trueObject;
    private RubyBoolean falseObject;
    
    private RubyClass classClass;
    private RubyClass moduleClass;
    private RubyClass objectClass;
    
    private RubyClass numericClass;
    private RubyClass integerClass;
    private RubyClass fixnumClass;
    private RubyClass bignumClass;
    private RubyClass floatClass;
    
    private RubyClass nilClass;
    private RubyClass trueClass;
    private RubyClass falseClass;
    private RubyClass symbolClass;
    
    private RubyClass stringClass;
    private RubyModule kernelModule;
    
    private RubyObject rubyTopSelf;
    
    private RubyOriginalMethods originalMethods;
    
    /**
     *
     */
    // public SCOPE ruby_scope = new SCOPE();
    public RubyScope rubyScope = new RubyScope();
    
    public op_tbl[] op_tbl;
    
    /** Create and initialize a new jruby Runtime.
     */    
    public Ruby() {
        initOperatorTable();
        
        originalMethods = new RubyOriginalMethods(this);
        
        globalMap = new RubyHashMap();
        classMap = new RubyHashMap();

        nilObject = new RubyNil(this);
        trueObject = new RubyBoolean(this, true);
        falseObject = new RubyBoolean(this, false);
        
        initializeCoreClasses();
    }
    
    /** Returns an in
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
    
    public RubyNil getNil() {
        return nilObject;
    }
    
    public RubyClass getTrueClass() {
        return trueClass;
    }
    
    public RubyClass getFalseClass() {
        return falseClass;
    }
    
    public RubyClass getNilClass() {
        return nilClass;
    }
    
    public RubyClass getSymbolClass() {
        return symbolClass;
    }
    
    /** Returns a class from the instance pool.
     * @param name The name of the class.
     * @return The class.
     */    
    public RubyModule getRubyClass(String name) {
        return (RubyModule)classMap.get(intern(name));
    }
    
    /** rb_define_boot?
     *
     */    
    private RubyClass defineBootClass(String name, RubyClass superClass) {
        RubyClass bootClass = RubyClass.m_newClass(this, superClass);
        bootClass.setName(intern(name));
        classMap.put(intern(name), bootClass);
        
        return bootClass;
    }
    
    private void initializeCoreClasses() {
        RubyClass metaClass;
        
        objectClass = defineBootClass("Object", null);
        moduleClass = defineBootClass("Module", objectClass);
        classClass = defineBootClass("Class", moduleClass);
        
        metaClass = classClass.newSingletonClass();
        objectClass.setRubyClass(metaClass);
        metaClass.attachSingletonClass(objectClass);
        
        metaClass = metaClass.newSingletonClass();
        moduleClass.setRubyClass(metaClass);
        metaClass.attachSingletonClass(moduleClass);
        
        metaClass = metaClass.newSingletonClass();
        classClass.setRubyClass(metaClass);
        metaClass.attachSingletonClass(classClass);
        
        RubyModule kernelModule = RBKernel.createKernelModule(this);
        objectClass.includeModule(kernelModule);
        
        objectClass.definePrivateMethod("initialize", DefaultCallbackMethods.getMethodNil());
        classClass.definePrivateMethod("inherited", DefaultCallbackMethods.getMethodNil());

        /*
         *
         * Ruby's Class Hierarchy Chart
         *
         *                           +------------------+
         *                           |                  |
         *             Object---->(Object)              |
         *              ^  ^        ^  ^                |
         *              |  |        |  |                |
         *              |  |  +-----+  +---------+      |
         *              |  |  |                  |      |
         *              |  +-----------+         |      |
         *              |     |        |         |      |
         *       +------+     |     Module--->(Module)  |
         *       |            |        ^         ^      |
         *  OtherClass-->(OtherClass)  |         |      |
         *                             |         |      |
         *                           Class---->(Class)  |
         *                             ^                |
         *                             |                |
         *                             +----------------+
         *
         *   + All metaclasses are instances of the class `Class'.
         */
        
        RbObject.initObjectClass(objectClass);
        RbClass.initClassClass(classClass);
        RbModule.initModuleClass(moduleClass);
        
        rubyTopSelf = objectClass.m_new((RubyObject[])null);
        rubyTopSelf.defineSingletonMethod("to_s", new RubyCallbackMethod() {
            public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                return RubyString.m_newString(ruby, "main");
            }
        });
        
        symbolClass = RbSymbol.createSymbolClass(this);
        
        nilClass = RbNilClass.createNilClass(this);
        
        falseClass = RbFalseClass.createFalseClass(this);
        trueClass = RbTrueClass.createTrueClass(this);
        
        RbComparable.createComparable(this);
        RbEnumerable.createEnumerableModule(this);
        
        numericClass = RbNumeric.createNumericClass(this);
        integerClass = RbInteger.createIntegerClass(this);
        fixnumClass = RbFixnum.createFixnum(this);
        floatClass = RbFloat.createFloat(this);
        
        stringClass = RbString.createStringClass(this);
        
        RbArray.createArrayClass(this);
        RbRange.createRangeClass(this);
        
        RbJavaObject.defineJavaObjectClass(this);
    }
    
    /** rb_define_class
     *
     */
    public RubyClass defineClass(String name, RubyClass superClass) {
        RubyClass newClass = defineClassId(intern(name), superClass);
        
        classMap.put(intern(name), newClass);
        
        return newClass;
    }
    
    /** rb_define_class_id
     *
     */
    public RubyClass defineClassId(RubyId id, RubyClass superClass) {
        if (superClass == null) {
            superClass = getObjectClass();
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
        
        classMap.put(intern(name), newModule);
        
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
        getKernelModule().defineModuleFunction(name, method);
    }
    
    public RubyModule getKernelModule() {
        return kernelModule;
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
    
    public RubyClass getClassClass() {
        return classClass;
    }
    
    public RubyClass getObjectClass() {
        return objectClass;
    }
    
    public RubyClass getModuleClass() {
        return moduleClass;
    }
    
    public RubyClass getNumericClass() {
        return numericClass;
    }
    
    public RubyClass getIntegerClass() {
        return integerClass;
    }
    
    public RubyClass getFixnumClass() {
        return fixnumClass;
    }
    
    public RubyClass getBignumClass() {
        return bignumClass;
    }
    
    public RubyClass getFloatClass() {
        return floatClass;
    }
    
    public RubyClass getStringClass() {
        return stringClass;
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
        getObjectClass().defineConstant(name, value);
    }
    
    /** top_const_get
     *
     */
    public RubyObject getTopConstant(RubyId id) {
        if (classMap.get(id) != null) {
            return (RubyObject)classMap.get(id);
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
    
    /** Getter for property classMap.
     * @return Value of property classMap.
     */
    public org.jruby.util.RubyMap getClassMap() {
        return classMap;
    }
    
    /** Setter for property classMap.
     * @param classMap New value of property classMap.
     */
    public void setClassMap(org.jruby.util.RubyMap classMap) {
        this.classMap = classMap;
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
}