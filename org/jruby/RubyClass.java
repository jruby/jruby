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

import java.util.*;

import org.jruby.core.DefaultCallbackMethods;
import org.jruby.core.ReflectionCallbackMethod;
import org.jruby.core.RubyCallbackMethod;
import org.jruby.exceptions.*;
import org.jruby.util.*;

/**
 *
 * @author  jpetersen
 */
public class RubyClass extends RubyModule {
    // Flags
    private boolean singleton = false;

    public RubyClass(Ruby ruby) {
        this(ruby, null, null);
    }

    public RubyClass(Ruby ruby, RubyClass superClass) {
        this(ruby, null, superClass);
    }

    public RubyClass(Ruby ruby, RubyClass rubyClass, RubyClass superClass) {
        super(ruby, rubyClass, superClass);
    }

    public static RubyClass nilClass(Ruby ruby) {
        return new RubyClass(ruby) {
            public boolean isNil() {
                return true;
            }
        };
    }

    protected void testFrozen() {
        if (isFrozen()) {
            if (isSingleton()) {
                throw new RubyFrozenException(getRuby(), "object");
            } else {
                throw new RubyFrozenException(getRuby(), "class");
            }
        }
    }

    public boolean isModule() {
        return false;
    }

    public boolean isClass() {
        return true;
    }

    public void setSingleton(boolean singleton) {
        this.singleton = singleton;
    }

    public static void createClassClass(RubyClass classClass) {
        RubyCallbackMethod s_newInstance = new ReflectionCallbackMethod(RubyClass.class, "newInstance", true, true);
        RubyCallbackMethod newInstance = new ReflectionCallbackMethod(RubyClass.class, "newInstance", true);
        RubyCallbackMethod superclass = new ReflectionCallbackMethod(RubyClass.class, "superclass");
        // RubyCallbackMethod inherited = new ReflectionCallbackMethod(RubyClass.class, "inherited", RubyClass.class, false, true);

        classClass.defineSingletonMethod("new", s_newInstance);

        classClass.defineMethod("new", newInstance);
        classClass.defineMethod("superclass", superclass);

        classClass.defineSingletonMethod("inherited", DefaultCallbackMethods.getMethodNil());

        classClass.undefMethod("module_function");
    }

    /** rb_singleton_class_clone
     *
     */
    public RubyClass getSingletonClassClone() {
        if (!isSingleton()) {
            return (RubyClass) this;
        }

        RubyClass clone = newClass(getRuby(), getRubyClass(), getSuperClass());
        clone.setupClone(this);
        clone.setInstanceVariables(getInstanceVariables().cloneRubyMap());

        //clone.setMethods();

        // st_foreach(RCLASS(klass)->m_tbl, clone_method, clone->m_tbl);

        clone.setSingleton(true);

        return clone;
    }

    public boolean isSingleton() {
        return this.singleton;
    }

    public RubyClass getRubyClass() {
        RubyClass type = super.getRubyClass();

        return type != null ? type : getRuby().getClasses().getClassClass();
    }

    /** rb_singleton_class_attached
     *
     */
    public void attachSingletonClass(RubyObject object) {
        if (isSingleton()) {
            getInstanceVariables().put(getRuby().intern("__atached__"), object);
        } else {
            getRuby().getRuntime().printBug("attachSingletonClass called on a non singleton class.");
        }
    }

    /** rb_singleton_class_new
     *
     */
    public RubyClass newSingletonClass() {
        RubyClass newClass = RubyClass.newClass(getRuby(), this);
        newClass.setSingleton(true);

        return newClass;
    }

    // Methods of the Class class (rb_class_*):

    /** rb_class_s_new
     *
     */
    public static RubyClass newClass(Ruby ruby, RubyClass superClass) {
        return new RubyClass(ruby, superClass);
    }

    public static RubyClass newClass(Ruby ruby, RubyClass rubyClass, RubyClass superClass) {
        return new RubyClass(ruby, rubyClass, superClass);
    }

    /** rb_class_new_instance
     *
     */
    public RubyObject newInstance(RubyObject[] args) {
        if (isSingleton()) {
            throw new RubyTypeException(getRuby(), "can't create instance of virtual class");
        }

        RubyObject obj = new RubyObject(getRuby(), this);

        obj.callInit(args);

        return obj;
    }

    /** rb_class_s_new
     *
     */
    public static RubyModule newInstance(Ruby ruby, RubyObject recv, RubyObject[] args) {
        RubyClass superClass = ruby.getClasses().getObjectClass();

        if (args.length >= 1) {
            superClass = (RubyClass) args[0];
        }

        if (superClass.isSingleton()) {
            throw new RubyTypeException(ruby, "can't make subclass of virtual class");
        }

        RubyClass newClass = newClass(ruby, superClass);

        // make metaclass
        newClass.setRubyClass(superClass.getRubyClass().newSingletonClass());
        newClass.getRubyClass().attachSingletonClass(newClass);

        // call "initialize" method
        newClass.callInit(args);

        // call "inherited" method of the superclass
        superClass.funcall(ruby.intern("inherited"), newClass);

        return newClass;
    }

    /** Return the real super class of this class.
     * 
     * rb_class_superclass
     *
     */
    public RubyClass superclass() {
        RubyClass superClass = getSuperClass();
        while (superClass.isIncluded()) {
            superClass = superClass.getSuperClass();
        }

        return getSuperClass() != null ? superClass : nilClass(getRuby());
    }

    /** rb_class_s_inherited
     *
     */
    public static RubyObject inherited(Ruby ruby, RubyClass subClass) {
        throw new RubyTypeException(ruby, "can't make subclass of Class");
    }
}