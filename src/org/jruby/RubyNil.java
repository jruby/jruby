/*
 * RubyNil.java - No description
 * Created on 09. Juli 2001, 21:38
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

import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author  jpetersen
 */
public class RubyNil {
    public static RubyClass createNilClass(Ruby ruby) {
        RubyClass nilClass = ruby.defineClass("NilClass", ruby.getClasses().getObjectClass());
        
        nilClass.defineMethod("type", CallbackFactory.getSingletonMethod(RubyNil.class, "type"));
        nilClass.defineMethod("to_i", CallbackFactory.getSingletonMethod(RubyNil.class, "to_i"));
        nilClass.defineMethod("to_s", CallbackFactory.getSingletonMethod(RubyNil.class, "to_s"));
        nilClass.defineMethod("to_a", CallbackFactory.getSingletonMethod(RubyNil.class, "to_a"));
        nilClass.defineMethod("inspect", CallbackFactory.getSingletonMethod(RubyNil.class, "inspect"));
        
        nilClass.defineMethod("&", CallbackFactory.getSingletonMethod(RubyNil.class, "op_and", IRubyObject.class));
        nilClass.defineMethod("|", CallbackFactory.getSingletonMethod(RubyNil.class, "op_or", IRubyObject.class));
        nilClass.defineMethod("^", CallbackFactory.getSingletonMethod(RubyNil.class, "op_xor", IRubyObject.class));
        nilClass.defineMethod("nil?", CallbackFactory.getTrueMethod(0));
        nilClass.defineMethod("id", CallbackFactory.getSingletonMethod(RubyNil.class, "id"));
        nilClass.defineMethod("taint", CallbackFactory.getSelfMethod(0));
        nilClass.defineMethod("freeze", CallbackFactory.getSelfMethod(0));

        nilClass.getMetaClass().undefineMethod("new");
        
        ruby.defineGlobalConstant("NIL", ruby.getNil());
        
        return nilClass;
    }

    // Methods of the Nil Class (nil_*):
        
    /** nil_to_i
     *
     */
    public static RubyFixnum to_i(IRubyObject recv) {
        return RubyFixnum.zero(recv.getRuntime());
    }

    /** nil_to_s
     *
     */
    public static RubyString to_s(IRubyObject recv) {
        return RubyString.newString(recv.getRuntime(), "");
    }
    
    /** nil_to_a
     *
     */
    public static RubyArray to_a(IRubyObject recv) {
        return RubyArray.newArray(recv.getRuntime(), 0);
    }
    
    /** nil_inspect
     *
     */
    public static RubyString inspect(IRubyObject recv) {
        return RubyString.newString(recv.getRuntime(), "nil");
    }
    
    /** nil_type
     *
     */
    public static RubyClass type(IRubyObject recv) {
        return recv.getRuntime().getClass("NilClass");
    }
    
    /** nil_and
     *
     */
    public static RubyBoolean op_and(IRubyObject recv, IRubyObject obj) {
        return recv.getRuntime().getFalse();
    }
    
    /** nil_or
     *
     */
    public static RubyBoolean op_or(IRubyObject recv, IRubyObject obj) {
        return RubyBoolean.newBoolean(recv.getRuntime(), obj.isTrue());
    }

    /** nil_xor
     *
     */
    public static RubyBoolean op_xor(IRubyObject recv, IRubyObject obj) {
        return RubyBoolean.newBoolean(recv.getRuntime(), obj.isTrue());
    }

    public static RubyFixnum id(IRubyObject recv) {
        return RubyFixnum.newFixnum(recv.getRuntime(), 4);
    }
}
