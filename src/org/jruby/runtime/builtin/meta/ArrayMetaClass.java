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
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.runtime.Arity;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.collections.SinglyLinkedList;

public class ArrayMetaClass extends ObjectMetaClass {
    public ArrayMetaClass(IRuby runtime) {
        super("Array", RubyArray.class, runtime.getObject());
    }
    
	public ArrayMetaClass(String name, RubyClass superClass, SinglyLinkedList parentCRef) {
		super(name, RubyArray.class, superClass, parentCRef);
	}

	protected class ArrayMeta extends Meta {
		protected void initializeClass() {
	        includeModule(getRuntime().getModule("Enumerable"));
	
	        defineMethod("+", Arity.singleArgument(), "op_plus");
	        defineMethod("*", Arity.singleArgument(), "op_times");
	        defineMethod("-", Arity.singleArgument(), "op_diff");
	        defineMethod("&", Arity.singleArgument(), "op_and");
	        defineMethod("|", Arity.singleArgument(), "op_or");
	        defineMethod("[]", Arity.optional(), "aref");
	        defineMethod("[]=", Arity.optional(), "aset");
	        defineMethod("<=>", Arity.singleArgument(), "op_cmp");
	        defineMethod("<<", Arity.singleArgument(), "append");
	        defineMethod("==", Arity.singleArgument(), "array_op_equal");
	        defineMethod("assoc", Arity.singleArgument());
	        defineMethod("at", Arity.singleArgument(), "at");
	        defineMethod("clear", Arity.noArguments(), "rb_clear");
	        defineMethod("clone", Arity.noArguments(), "rbClone");
	        defineMethod("collect", Arity.noArguments());
	        defineMethod("collect!", Arity.noArguments(), "collect_bang");
	        defineMethod("compact", Arity.noArguments());
	        defineMethod("compact!", Arity.noArguments(), "compact_bang");
	        defineMethod("concat", Arity.singleArgument());
	        defineMethod("delete", Arity.singleArgument());
	        defineMethod("delete_at", Arity.singleArgument());
	        defineMethod("delete_if", Arity.noArguments());
	        defineMethod("each", Arity.noArguments());
	        defineMethod("each_index", Arity.noArguments());
	        defineMethod("eql?", Arity.singleArgument(), "eql");
	        defineMethod("empty?", Arity.noArguments(), "empty_p");
	        defineMethod("fetch", Arity.optional());
	        defineMethod("fill", Arity.optional());
	        defineMethod("first", Arity.optional());
	        defineMethod("flatten", Arity.noArguments());
	        defineMethod("flatten!", Arity.noArguments(), "flatten_bang");
	        defineMethod("frozen?", Arity.noArguments(), "frozen");
	        defineMethod("hash", Arity.noArguments());
	        defineMethod("include?", Arity.singleArgument(), "include_p");
	        defineMethod("index", Arity.singleArgument());
	        defineMethod("indices", Arity.optional());
	        defineMethod("initialize", Arity.optional());
	        defineMethod("insert", Arity.optional());
	        defineMethod("inspect", Arity.noArguments());
	        defineMethod("join", Arity.optional());
	        defineMethod("last", Arity.optional());
	        defineMethod("length", Arity.noArguments());
	        defineMethod("nitems", Arity.noArguments());
	        defineMethod("pack", Arity.singleArgument());
	        defineMethod("pop", Arity.noArguments());
	        defineMethod("push", Arity.optional());
	        defineMethod("rassoc", Arity.singleArgument());
	        defineMethod("reject!", Arity.noArguments(), "reject_bang");
	        defineMethod("replace", Arity.singleArgument(), "replace");
	        defineMethod("reverse", Arity.noArguments());
	        defineMethod("reverse!", Arity.noArguments(), "reverse_bang");
	        defineMethod("reverse_each", Arity.noArguments());
	        defineMethod("rindex", Arity.singleArgument());
	        defineMethod("shift", Arity.noArguments());
	        defineMethod("sort", Arity.noArguments());
	        defineMethod("sort!", Arity.noArguments(), "sort_bang");
	        defineMethod("slice", Arity.optional(), "aref");
	        defineMethod("slice!", Arity.optional(), "slice_bang");
	        defineMethod("to_a", Arity.noArguments());
	        defineMethod("to_ary", Arity.noArguments());
	        defineMethod("to_s", Arity.noArguments());
	        defineMethod("transpose", Arity.noArguments());
	        defineMethod("uniq", Arity.noArguments());
	        defineMethod("uniq!", Arity.noArguments(), "uniq_bang");
	        defineMethod("unshift", Arity.optional());
	        defineMethod("values_at", Arity.optional());
	        defineAlias("===", "==");
	        defineAlias("size", "length");
	        defineAlias("indexes", "indices");
	        defineAlias("filter", "collect!");
	        defineAlias("map!", "collect!");
	
	        defineSingletonMethod("new", Arity.optional(), "newInstance");
	        defineSingletonMethod("[]", Arity.optional(), "create");
		}
	};

	protected Meta getMeta() {
		return new ArrayMeta();
	}
	
	public RubyClass newSubClass(String name, SinglyLinkedList parentCRef) {
		return new ArrayMetaClass(name, this, parentCRef);
	}

	protected IRubyObject allocateObject() {
        RubyArray instance = getRuntime().newArray();
        
		instance.setMetaClass(this);
		
		return instance;
	}

    public IRubyObject newInstance(IRubyObject[] args) {
        RubyArray instance = (RubyArray)allocateObject();
        
        instance.setMetaClass(this);
        instance.callInit(args);
       
        return instance;
    }
    
    public IRubyObject create(IRubyObject[] args) {
        RubyArray array = (RubyArray)allocateObject();
        array.setMetaClass(this);
        
        if (args.length >= 1) {
            for (int i = 0; i < args.length; i++) {
                array.add(args[i]);
            }
        }
        
        return array;
    }
}
