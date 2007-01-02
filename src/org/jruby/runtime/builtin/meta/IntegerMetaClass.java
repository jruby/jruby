/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2005 Thomas E Enebo <enebo@acm.org>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.runtime.builtin.meta;

import org.jruby.IRuby;
import org.jruby.RubyBignum;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyInteger;
import org.jruby.runtime.Arity;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.collections.SinglyLinkedList;

public class IntegerMetaClass extends NumericMetaClass {
	public IntegerMetaClass(IRuby runtime) {
        super("Integer", RubyInteger.class, runtime.getClass("Numeric"));
	}
	
	public IntegerMetaClass(String name, RubyClass superClass, SinglyLinkedList parentCRef) {
        super(name, RubyInteger.class, superClass, parentCRef);
    }
	
    public IntegerMetaClass(String name, Class clazz, RubyClass superClass) {
    	super(name, clazz, superClass);
    }

    public IntegerMetaClass(String name, Class clazz, RubyClass superClass, SinglyLinkedList parentCRef) {
    	super(name, clazz, superClass, parentCRef);
    }
    
    protected class IntegerMeta extends Meta {
		protected void initializeClass() {
	        includeModule(getRuntime().getModule("Precision"));
	        
	        defineFastMethod("chr", Arity.noArguments());
	        defineMethod("downto",  Arity.singleArgument());
	        defineFastMethod("integer?", Arity.noArguments(), "int_p");
	        defineFastMethod("next",  Arity.noArguments());
	        defineAlias("succ", "next");
	        defineMethod("times", Arity.noArguments());
	        defineMethod("upto", Arity.singleArgument());
	        
	        getSingletonClass().undefineMethod("new");
	        defineFastSingletonMethod("induced_from",  Arity.singleArgument());
		}
    };
    
    protected Meta getMeta() {
    	return new IntegerMeta();
    }

	public RubyClass newSubClass(String name, SinglyLinkedList parentCRef) {
        return new IntegerMetaClass(name, this, parentCRef);
	}

	// This cannot be allocated directly
	protected IRubyObject allocateObject() {
		return null;
	}
	
    public RubyInteger induced_from(IRubyObject number) {
        if (number instanceof RubyFixnum) {
            return (RubyFixnum) number;
        } else if (number instanceof RubyFloat) {
            return ((RubyFloat) number).to_i();
        } else if (number instanceof RubyBignum) {
            return getRuntime().newFixnum(((RubyBignum) number).getLongValue());
        } else {
            throw getRuntime().newTypeError("failed to convert " + number.getMetaClass() + 
                " into Integer");
        }
    }
}
