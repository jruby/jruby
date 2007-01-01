/*
 * Created on Jun 21, 2005
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.jruby.runtime.builtin.meta;

import java.util.HashMap;

import org.jruby.IRuby;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.runtime.Arity;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.collections.SinglyLinkedList;

public class HashMetaClass extends ObjectMetaClass {
    public HashMetaClass(IRuby runtime) {
        super("Hash", RubyHash.class, runtime.getObject());
    }
    
	public HashMetaClass(String name, RubyClass superClass, SinglyLinkedList parentCRef) {
		super(name, RubyHash.class, superClass, parentCRef);
	}

	protected class HashMeta extends Meta {
		protected void initializeClass() {
	        includeModule(getRuntime().getModule("Enumerable"));
	
	        defineFastMethod("==", Arity.singleArgument(), "equal");
	        defineFastMethod("[]", Arity.singleArgument(), "aref");
	        defineFastMethod("[]=", Arity.twoArguments(), "aset");
			defineFastMethod("clear", Arity.noArguments(), "rb_clear");
			defineFastMethod("clone", Arity.noArguments(), "rbClone");
			defineMethod("default", Arity.optional(), "getDefaultValue");
            defineMethod("default_proc", Arity.noArguments()); 
			defineMethod("default=", Arity.singleArgument(), "setDefaultValue");
			defineMethod("delete", Arity.singleArgument());
			defineMethod("delete_if", Arity.noArguments());
			defineMethod("each", Arity.noArguments());
			defineMethod("each_pair", Arity.noArguments());
			defineMethod("each_value", Arity.noArguments());
			defineMethod("each_key", Arity.noArguments());
	        defineFastMethod("empty?", Arity.noArguments(), "empty_p");
	        defineMethod("fetch", Arity.optional());
	        defineFastMethod("has_value?", Arity.singleArgument(), "has_value");
	        defineFastMethod("index", Arity.singleArgument());
	        defineFastMethod("indices", Arity.optional());
	        defineMethod("initialize", Arity.optional());
	        defineFastMethod("inspect", Arity.noArguments());
			defineFastMethod("invert", Arity.noArguments());
	        defineFastMethod("include?", Arity.singleArgument(), "has_key");
			defineFastMethod("keys", Arity.noArguments());
	        defineMethod("merge", Arity.singleArgument());
	        defineFastMethod("rehash", Arity.noArguments());
			defineMethod("reject", Arity.noArguments());
			defineMethod("reject!", Arity.noArguments(), "reject_bang");
	        defineFastMethod("replace", Arity.singleArgument());
			defineFastMethod("shift", Arity.noArguments());
	        defineFastMethod("size", Arity.noArguments(), "rb_size");
			defineMethod("sort", Arity.noArguments());
	        defineFastMethod("to_a", Arity.noArguments());
	        defineFastMethod("to_hash", Arity.noArguments());
	        defineFastMethod("to_s", Arity.noArguments());
	        defineMethod("update", Arity.singleArgument());
			defineFastMethod("values", Arity.noArguments(), "rb_values");
	        defineFastMethod("values_at", Arity.optional());

	        defineAlias("has_key?", "include?");
			defineAlias("indexes", "indices");
	        defineAlias("key?", "include?");
			defineAlias("length", "size");
	        defineAlias("member?", "include?");
	        defineAlias("merge!", "update");
	        defineAlias("store", "[]=");
	        defineAlias("value?", "has_value?");
	        
	        defineSingletonMethod("new", Arity.optional(), "newInstance");
	        defineFastSingletonMethod("[]", Arity.optional(), "create");
		}
	};
	
	protected Meta getMeta() {
		return new HashMeta();
	}
	
	public RubyClass newSubClass(String name, SinglyLinkedList parentCRef) {
		return new HashMetaClass(name, this, parentCRef);
	}

	protected IRubyObject allocateObject() {
        RubyHash instance = new RubyHash(getRuntime());
        
		instance.setMetaClass(this);
		
		return instance;
	}

    public IRubyObject newInstance(IRubyObject[] args) {
    	IRuby runtime = getRuntime();
        RubyHash hash = (RubyHash)allocateObject();

        // A block to represent 'default' value for unknown values
        if (runtime.getCurrentContext().isBlockGiven()) {
        	hash.setDefaultProc(runtime.newProc());
        }
        
        hash.setMetaClass(this);
        hash.callInit(args);
        
        return hash;
    }
    
    public IRubyObject create(IRubyObject[] args) {
        RubyHash hash = (RubyHash)allocateObject();

        if (args.length == 1) {
            hash.setValueMap(new HashMap(((RubyHash) args[0]).getValueMap()));
        } else if (args.length % 2 != 0) {
            throw getRuntime().newArgumentError("odd number of args for Hash");
        } else {
            for (int i = 0; i < args.length; i += 2) {
                hash.aset(args[i], args[i + 1]);
            }
        }
        return hash;
    }
}
