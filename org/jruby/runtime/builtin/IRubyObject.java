/*
 * IObject.java - description
 * Created on 12.03.2002, 01:26:26
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
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
package org.jruby.runtime.builtin;

import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyString;

/** Represents an object in Ruby. All the methods defined by this interface
 * are avaiable for all Ruby objects.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public interface IRubyObject {
    /** SHOULD REMOVED */
    RubyObject toRubyObject();
    /**
     * Method setInstanceVar.
     * @param string
     * @param rubyObject
     * @return RubyObject
     */
    RubyObject setInstanceVariable(String string, RubyObject rubyObject);

    /**
     * Method funcall.
     * @param string
     * @return RubyObject
     */
    RubyObject callMethod(String string);

    /**
     * Method isNil.
     * @return boolean
     */
    boolean isNil();
    
    boolean isTrue();

    /**
     * Method funcall.
     * @param string
     * @param arg
     * @return RubyObject
     */
    RubyObject callMethod(String string, RubyObject arg);

    /**
     * Method getRubyClass.
     */
    RubyClass getInternalClass();

    /**
     * Method getInstanceVar.
     * @param string
     * @return RubyObject
     */
    RubyObject getInstanceVariable(String string);

    /**
     * Method isTaint.
     * @return boolean
     */
    boolean isTaint();

    /**
     * Method isFrozen.
     * @return boolean
     */
    boolean isFrozen();

    /**
     * Method getSingletonClass.
     * @return RubyClass
     */
    RubyClass getSingletonClass();

    /**
     * Method getType.
     * @return RubyClass
     */
    RubyClass getType();

}