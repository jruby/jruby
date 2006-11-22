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
import org.jruby.RubyClass;
import org.jruby.RubyNumeric;
import org.jruby.runtime.Arity;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.collections.SinglyLinkedList;

public class NumericMetaClass extends ObjectMetaClass {
	public NumericMetaClass(IRuby runtime) {
        super("Numeric", RubyNumeric.class, runtime.getObject());
    }
	    
	public NumericMetaClass(String name, RubyClass superClass, SinglyLinkedList parentCRef) {
        super(name, RubyNumeric.class, superClass, parentCRef);
    }

    public NumericMetaClass(String name, Class clazz, RubyClass superClass) {
    	super(name, clazz, superClass);
    }

    public NumericMetaClass(String name, Class clazz, RubyClass superClass, SinglyLinkedList parentCRef) {
    	super(name, clazz, superClass, parentCRef);
    }
    
    protected class NumericMeta extends Meta {
	    protected void initializeClass() {
	        includeModule(getRuntime().getModule("Comparable"));
	
	        defineMethod("+@", Arity.noArguments(), "op_uplus");
	        defineMethod("-@", Arity.noArguments(), "op_uminus");
	        defineMethod("<=>", Arity.singleArgument(), "cmp");
	        defineMethod("==", Arity.singleArgument(), "equal");
	        defineMethod("equal?", Arity.singleArgument(), "veryEqual");
	        defineMethod("===", Arity.singleArgument(), "equal");
	        defineMethod("abs", Arity.noArguments());
	        defineMethod("ceil", Arity.noArguments());
	        defineMethod("coerce", Arity.singleArgument());
	        defineMethod("clone", Arity.noArguments(), "rbClone");
	        defineMethod("divmod", Arity.singleArgument(), "divmod");
	        defineMethod("eql?", Arity.singleArgument(), "eql");
	        defineMethod("floor", Arity.noArguments());
	        defineMethod("integer?", Arity.noArguments(), "int_p");
	        defineMethod("modulo", Arity.singleArgument());
	        defineMethod("nonzero?", Arity.noArguments(), "nonzero_p");
	        defineMethod("remainder", Arity.singleArgument());
	        defineMethod("round", Arity.noArguments());
	        defineMethod("truncate", Arity.noArguments());
	        defineMethod("to_int", Arity.noArguments());
	        defineMethod("zero?", Arity.noArguments(), "zero_p");
            
            // Add relational operators that are faster than comparable's implementations
            defineMethod(">=", Arity.singleArgument(), "op_ge");
            defineMethod(">", Arity.singleArgument(), "op_gt");
            defineMethod("<=", Arity.singleArgument(), "op_le");
            defineMethod("<", Arity.singleArgument(), "op_lt");
	        
	        defineSingletonMethod("new", Arity.optional(), "newInstance"); 
	    }
    };
    
    protected Meta getMeta() {
    	return new NumericMeta();
    }
		
    public RubyClass newSubClass(String name, SinglyLinkedList parentCRef) {
        return new NumericMetaClass(name, this, parentCRef);
    }

	protected IRubyObject allocateObject() {
		RubyNumeric instance = getRuntime().newNumeric();

		instance.setMetaClass(this);
		
        return instance;
    }
}
