/*
 * RubyNil.java - No description
 * Created on 09. Juli 2001, 21:38
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Copyright (C) 2004 Charles O Nutter
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Charles O Nutter <headius@headius.com>
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
public class RubyNil extends RubyObject {
	private final Ruby runtime;
	
	public RubyNil(Ruby runtime) {
		super(runtime, null);
		this.runtime = runtime;
	}
	
	public Ruby getRuntime() {
		return runtime;
	}
	
    public static RubyClass createNilClass(Ruby runtime) {
        RubyClass nilClass = runtime.defineClass("NilClass", runtime.getClasses().getObjectClass());
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyNil.class);
        nilClass.defineMethod("type", callbackFactory.getSingletonMethod("type"));
        nilClass.defineMethod("to_i", callbackFactory.getSingletonMethod("to_i"));
        nilClass.defineMethod("to_s", callbackFactory.getSingletonMethod("to_s"));
        nilClass.defineMethod("to_a", callbackFactory.getSingletonMethod("to_a"));
        nilClass.defineMethod("to_f", callbackFactory.getSingletonMethod("to_f"));
        nilClass.defineMethod("inspect", callbackFactory.getSingletonMethod("inspect"));
        
        nilClass.defineMethod("&", callbackFactory.getSingletonMethod("op_and", IRubyObject.class));
        nilClass.defineMethod("|", callbackFactory.getSingletonMethod("op_or", IRubyObject.class));
        nilClass.defineMethod("^", callbackFactory.getSingletonMethod("op_xor", IRubyObject.class));
        nilClass.defineMethod("nil?", callbackFactory.getTrueMethod(0));
        nilClass.defineMethod("id", callbackFactory.getSingletonMethod("id"));
        nilClass.defineMethod("taint", callbackFactory.getSelfMethod(0));
        nilClass.defineMethod("freeze", callbackFactory.getSelfMethod(0));

        nilClass.getMetaClass().undefineMethod("new");
        
        runtime.defineGlobalConstant("NIL", runtime.getNil());
        
        return nilClass;
    }

    public boolean isImmediate() {
    	return true;
    }
    
    // Methods of the Nil Class (nil_*):
        
    /** nil_to_i
    *
    */
   public static RubyFixnum to_i(IRubyObject recv) {
       return RubyFixnum.zero(recv.getRuntime());
   }

   /**
    * nil_to_f
    *  
    */
	public static RubyFloat to_f(IRubyObject recv) {
		return RubyFloat.newFloat(recv.getRuntime(), 0.0D);
	}

    /** nil_to_s
     *
     */
    public static RubyString to_s(IRubyObject recv) {
        return recv.getRuntime().newString("");
    }
    
    /** nil_to_a
     *
     */
    public static RubyArray to_a(IRubyObject recv) {
        return recv.getRuntime().newArray(0);
    }
    
    /** nil_inspect
     *
     */
    public static RubyString inspect(IRubyObject recv) {
        return recv.getRuntime().newString("nil");
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
        return recv.getRuntime().newBoolean(obj.isTrue());
    }

    /** nil_xor
     *
     */
    public static RubyBoolean op_xor(IRubyObject recv, IRubyObject obj) {
        return recv.getRuntime().newBoolean(obj.isTrue());
    }

    public static RubyFixnum id(IRubyObject recv) {
        return recv.getRuntime().newFixnum(4);
    }

    public boolean isNil() {
        return true;
    }
    
    public boolean isFalse() {
    	return true;
	}

	public boolean isTrue() {
		return false;
	}
}
