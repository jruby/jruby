/*
 * RubyClass.java - No description
 * Created on 04. Juli 2001, 22:53
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <japetersen@web.de>
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

import org.jruby.exceptions.*;
import org.jruby.util.*;

/**
 *
 * @author  jpetersen
 */
public class RubyClass extends RubyModule {
    public RubyClass(Ruby ruby) {
        this(ruby, null);
    }

    public RubyClass(Ruby ruby, RubyModule superClass) {
        super(ruby, null, superClass);
    }

    public boolean isModule() {
        return false;
    }
    
    public boolean isClass() {
        return !isIncluded();
    }
    
    public RubyModule getRubyClass() {
        if (super.getRubyClass() == null) {
            return getRuby().getClasses().getClassClass();
        } else {
            return super.getRubyClass();
        }
    }
    
    // Methods of the Class class (rb_class_*):
    
    /** rb_class_s_new
     *
     */
    public static RubyClass m_newClass(Ruby ruby, RubyClass superClass) {
        return new RubyClass(ruby, superClass);
    }
    
    /** rb_class_new_instance
     *
     */
    public RubyObject m_new(RubyObject[] args) {
        if (isSingleton()) {
            throw new RubyTypeException(getRuby(), "can't create instance of virtual class");
        }
        
        RubyObject obj = new RubyObject(getRuby(), this);
        
        obj.callInit( args);
        
        return obj;
    }
    
    /** rb_class_s_new
     *
     */
    public static RubyModule m_new(Ruby ruby, RubyObject recv, RubyObject[] args) {
        RubyClass superClass = ruby.getClasses().getObjectClass();

        if (args.length >= 1) {
            superClass = (RubyClass)args[0];
        }

        if (superClass.isSingleton()) {
            throw new RubyTypeException(ruby, "can't make subclass of virtual class");
        }

        RubyClass newClass = m_newClass(ruby, superClass);
        
        // make metaclass
        newClass.setRubyClass(superClass.getRubyClass().newSingletonClass());
        newClass.getRubyClass().attachSingletonClass(newClass);
        
        // call "initialize" method
        newClass.callInit(args);
        
        // call "inherited" method of the superclass
        superClass.funcall(ruby.intern("inherited"), newClass);

        return newClass;
    }
    
    /** rb_class_superclass
     *
     */
    public RubyObject m_superclass() {
        RubyModule superClass = getSuperClass();
        
        while (superClass != null && superClass.isIncluded()) {
            superClass = superClass.getSuperClass();
        }
        
        if (superClass == null) {
            return getRuby().getNil();
        }
        
        return superClass;
    }
    
    /** rb_class_s_inherited
     *
     */
    public static RubyObject m_inherited(Ruby ruby, RubyClass subClass) {
        throw new RubyTypeException(ruby, "can't make subclass of Class");
    }
}