/*
 * RubyClass.java - No description
 * Created on 04. Juli 2001, 22:53
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
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

    public RubyClass(Ruby ruby, RubyModule rubyClass) {
        this(ruby, rubyClass, null);
    }
    
    public RubyClass(Ruby ruby, RubyModule rubyClass, RubyModule superClass) {
        super(ruby, rubyClass, superClass);
    }

    public boolean isModule() {
        return false;
    }
    
    public boolean isClass() {
        return !isIncluded();
    }
    
    // Methods of the Class class (rb_class_*):
    
    /** rb_class_s_new
     *
     */
    public static RubyClass m_newClass(Ruby ruby, RubyClass superClass) {
        return new RubyClass(ruby, ruby.getClassClass(), superClass);
    }
    
    /** rb_class_new_instance
     *
     */
    public RubyObject m_new(RubyObject[] args) {
        if (isSingleton()) {
            throw new RubyTypeException("can't create instance of virtual class");
        }
        
        RubyObject obj = new RubyObject(getRuby(), this);
        
        // PUSH_ITER(rb_block_given_p()?ITER_PRE:ITER_NOT);
        obj.funcall(getRuby().intern("initialize"), args);
        // POP_ITER();
        
        return obj;
    }
}