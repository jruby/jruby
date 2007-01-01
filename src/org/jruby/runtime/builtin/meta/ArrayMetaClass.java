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
	
	        defineFastMethod("+", Arity.singleArgument(), "op_plus");
	        defineFastMethod("*", Arity.singleArgument(), "op_times");
	        defineFastMethod("-", Arity.singleArgument(), "op_diff");
	        defineFastMethod("&", Arity.singleArgument(), "op_and");
	        defineFastMethod("|", Arity.singleArgument(), "op_or");
	        defineFastMethod("[]", Arity.optional(), "aref");
	        defineFastMethod("[]=", Arity.optional(), "aset");
	        defineFastMethod("<=>", Arity.singleArgument(), "op_cmp");
	        defineFastMethod("<<", Arity.singleArgument(), "append");
	        defineFastMethod("==", Arity.singleArgument(), "array_op_equal");
	        defineFastMethod("assoc", Arity.singleArgument());
	        defineFastMethod("at", Arity.singleArgument(), "at");
	        defineFastMethod("clear", Arity.noArguments(), "rb_clear");
	        defineFastMethod("clone", Arity.noArguments(), "rbClone");
	        defineMethod("collect", Arity.noArguments());
	        defineMethod("collect!", Arity.noArguments(), "collect_bang");
	        defineFastMethod("compact", Arity.noArguments());
	        defineFastMethod("compact!", Arity.noArguments(), "compact_bang");
	        defineFastMethod("concat", Arity.singleArgument());
	        defineMethod("delete", Arity.singleArgument());
	        defineFastMethod("delete_at", Arity.singleArgument());
	        defineMethod("delete_if", Arity.noArguments());
	        defineMethod("each", Arity.noArguments());
	        defineMethod("each_index", Arity.noArguments());
	        defineFastMethod("eql?", Arity.singleArgument(), "eql");
	        defineFastMethod("empty?", Arity.noArguments(), "empty_p");
	        defineMethod("fetch", Arity.optional());
	        defineMethod("fill", Arity.optional());
	        defineFastMethod("first", Arity.optional());
	        defineFastMethod("flatten", Arity.noArguments());
	        defineFastMethod("flatten!", Arity.noArguments(), "flatten_bang");
	        defineFastMethod("frozen?", Arity.noArguments(), "frozen");
	        defineFastMethod("hash", Arity.noArguments());
	        defineFastMethod("include?", Arity.singleArgument(), "include_p");
	        defineFastMethod("index", Arity.singleArgument());
	        defineFastMethod("indices", Arity.optional());
            // FIXME: shouldn't this be private?
	        defineMethod("initialize", Arity.optional());
	        defineFastMethod("insert", Arity.optional());
	        defineFastMethod("inspect", Arity.noArguments());
	        defineFastMethod("join", Arity.optional());
	        defineFastMethod("last", Arity.optional());
	        defineFastMethod("length", Arity.noArguments());
	        defineFastMethod("nitems", Arity.noArguments());
	        defineFastMethod("pack", Arity.singleArgument());
	        defineFastMethod("pop", Arity.noArguments());
	        defineFastMethod("push", Arity.optional());
	        defineFastMethod("rassoc", Arity.singleArgument());
	        defineMethod("reject!", Arity.noArguments(), "reject_bang");
	        defineFastMethod("replace", Arity.singleArgument(), "replace");
	        defineFastMethod("reverse", Arity.noArguments());
	        defineFastMethod("reverse!", Arity.noArguments(), "reverse_bang");
	        defineMethod("reverse_each", Arity.noArguments());
	        defineFastMethod("rindex", Arity.singleArgument());
	        defineFastMethod("shift", Arity.noArguments());
	        defineMethod("sort", Arity.noArguments());
	        defineMethod("sort!", Arity.noArguments(), "sort_bang");
	        defineFastMethod("slice", Arity.optional(), "aref");
	        defineFastMethod("slice!", Arity.optional(), "slice_bang");
	        defineFastMethod("to_a", Arity.noArguments());
	        defineFastMethod("to_ary", Arity.noArguments());
	        defineFastMethod("to_s", Arity.noArguments());
	        defineFastMethod("transpose", Arity.noArguments());
	        defineFastMethod("uniq", Arity.noArguments());
	        defineFastMethod("uniq!", Arity.noArguments(), "uniq_bang");
	        defineFastMethod("unshift", Arity.optional());
	        defineFastMethod("values_at", Arity.optional());
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
