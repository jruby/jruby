/*
 * RubyClass.java - No description
 * Created on 04. Juli 2001, 22:53
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
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

import org.jruby.exceptions.TypeError;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ICallable;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * @author  jpetersen
 */
public class RubyClass extends RubyModule {

    /**
     * @mri rb_boot_class
     */
    protected RubyClass(RubyClass superClass) {
        super(superClass.getRuntime(), superClass.getRuntime().getClasses().getClassClass(), superClass, null, null);

        infectBy(superClass);
    }

    protected RubyClass(Ruby ruby, RubyClass superClass) {
        super(ruby, null, superClass, null, null);
    }

    protected RubyClass(Ruby ruby, RubyClass rubyClass, RubyClass superClass) {
        super(ruby, rubyClass, superClass, null, null);
    }
    
    protected RubyClass(Ruby ruby, RubyClass rubyClass, RubyClass superClass, RubyModule parentClass, String name) {
        super(ruby, rubyClass, superClass, parentClass, name);
    }

    public boolean isModule() {
        return false;
    }

    public boolean isClass() {
        return true;
    }

    public static void createClassClass(RubyClass classClass) {
        CallbackFactory callbackFactory = classClass.callbackFactory();
        classClass.defineSingletonMethod("new", callbackFactory.getOptSingletonMethod(RubyClass.class, "newClass"));
        classClass.defineMethod("new", callbackFactory.getOptMethod(RubyClass.class, "newInstance"));
        classClass.defineMethod("superclass", callbackFactory.getMethod(RubyClass.class, "superclass"));
        classClass.defineSingletonMethod("inherited", callbackFactory.getNilMethod(1));
        classClass.undefineMethod("module_function");
    }

    /** Invokes if  a class is inherited from an other  class.
     * 
     * MRI: rb_class_inherited
     * 
     * @since Ruby 1.6.7
     * 
     */
    public void inheritedBy(RubyClass superType) {
        if (superType == null) {
            superType = runtime.getClasses().getObjectClass();
        }
        superType.callMethod("inherited", this);
    }

    /** rb_singleton_class_clone
     *
     */
    public RubyClass getSingletonClassClone() {
        if (!isSingleton()) {
            return this;
        }

        MetaClass clone = new MetaClass(getRuntime(), getMetaClass(), getSuperClass());
        clone.setupClone(this);
        clone.setInstanceVariables(new HashMap(getInstanceVariables()));

        Iterator iter = getMethods().entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();

            ICallable value = ((ICallable) entry.getValue()).dup();

            clone.getMethods().put(entry.getKey(), value);
        }

        //clone.setMethods();

        // st_foreach(RCLASS(klass)->m_tbl, clone_method, clone->m_tbl);

        return clone;
    }

    public boolean isSingleton() {
        return false;
    }

    public RubyClass getMetaClass() {
        RubyClass type = super.getMetaClass();

        return type != null ? type : getRuntime().getClasses().getClassClass();
    }

    public RubyClass getRealClass() {
        return this;
    }

    public void attachToObject(IRubyObject object) {
        // Don't do anything, because a class cannot attached to an object.
    }

    /** 
     *
     */
    public MetaClass newSingletonClass() {
        MetaClass newClass = new MetaClass(getRuntime(), this);
        newClass.infectBy(this);
        return newClass;
    }

    public static RubyClass newClass(Ruby ruby, RubyClass superClass, RubyModule parentClass, String name) {
        return new RubyClass(ruby, ruby.getClasses().getClassClass(), superClass, parentClass, name);
    }

    /** Create a new subclass of this class.
     * 
     * @mri rb_class_new
     */
    protected RubyClass subclass() {
        if (this == runtime.getClasses().getClassClass()) {
            throw new TypeError(runtime, "can't make subclass of Class");
        }
        return new RubyClass(this);
    }

    /** rb_class_new_instance
     *
     */
    public IRubyObject newInstance(IRubyObject[] args) {
        if (isSingleton()) {
            throw new TypeError(getRuntime(), "can't create instance of virtual class");
        }
        IRubyObject obj = new RubyObject(getRuntime(), this);
        obj.callInit(args);
        return obj;
    }

    /** rb_class_s_new
     *
     */
    public static RubyClass newClass(IRubyObject recv, IRubyObject[] args) {
        final Ruby runtime = recv.getRuntime();

        RubyClass superClass = runtime.getClasses().getObjectClass();
        if (args.length > 0) {
            if (args[0] instanceof RubyClass) {
                superClass = (RubyClass) args[0];
            } else {
                throw new TypeError(
                    runtime,
                    "wrong argument type " + superClass.getType().getName() + " (expected Class)");
            }
        }

        RubyClass newClass = superClass.subclass();

        newClass.makeMetaClass(superClass.getMetaClass());

        // call "initialize" method
        newClass.callInit(args);

        // call "inherited" method of the superclass
        newClass.inheritedBy(superClass);

        return newClass;
    }

    /** Return the real super class of this class.
     * 
     * rb_class_superclass
     *
     */
    public IRubyObject superclass() {
        RubyClass superClass = getSuperClass();
        while (superClass != null && superClass.isIncluded()) {
            superClass = superClass.getSuperClass();
        }

        return superClass != null ? superClass : getRuntime().getNil();
    }

    /** rb_class_s_inherited
     *
     */
    public static IRubyObject inherited(RubyClass recv) {
        throw new TypeError(recv.getRuntime(), "can't make subclass of Class");
    }

    public void marshalTo(MarshalStream output) throws java.io.IOException {
        output.write('c');
        output.dumpString(getName());
    }

    public static RubyModule unmarshalFrom(UnmarshalStream output) throws java.io.IOException {
        return (RubyClass) RubyModule.unmarshalFrom(output);
    }

    /**
     * Creates a new object of this class by calling the 'allocate' method.
     * This class must be the type of the new object.
     * 
     * @return the new allocated object.
     */
    public IRubyObject allocateObject() {
        IRubyObject newObject = callMethod("allocate");
        if (newObject.getType() != getRealClass()) {
            throw new TypeError(runtime, "wrong instance allocation");
        }
        return newObject;
    }
}
