/*
 * Created on Jun 21, 2005
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.jruby.runtime.builtin.meta;

import org.jruby.IRuby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.runtime.Arity;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.collections.SinglyLinkedList;

public class ModuleMetaClass extends ObjectMetaClass {
	public ModuleMetaClass(IRuby runtime, RubyClass superClass) {
		super(runtime, null, superClass, runtime.getObject().getCRef(), "Module", RubyModule.class);
	}

    public ModuleMetaClass(IRuby runtime) {
        super("Module", RubyModule.class, runtime.getObject());
    }
    
	public ModuleMetaClass(String name, RubyClass superClass, SinglyLinkedList parentCRef) {
		super(name, RubyModule.class, superClass, parentCRef);
	}
	
	protected ModuleMetaClass(String name, Class builtinClass, RubyClass superClass) {
		super(name, builtinClass, superClass);
	}
	
	protected ModuleMetaClass(String name, Class builtinClass, RubyClass superClass, SinglyLinkedList parentCRef) {
		super(name, builtinClass, superClass, parentCRef);
	}

	protected class ModuleMeta extends Meta {
		public void initializeClass() {
	        defineMethod("===", Arity.singleArgument(), "op_eqq");
	        defineMethod("<=>", Arity.singleArgument(), "op_cmp");
	        defineMethod("<", Arity.singleArgument(), "op_lt");
	        defineMethod("<=", Arity.singleArgument(), "op_le");
	        defineMethod(">", Arity.singleArgument(), "op_gt");
	        defineMethod(">=", Arity.singleArgument(), "op_ge");
	        defineMethod("ancestors", Arity.noArguments());
	        defineMethod("class_variables", Arity.noArguments());
	        defineMethod("clone", Arity.noArguments(), "rbClone");
	        defineMethod("const_defined?", Arity.singleArgument(), "const_defined");
	        defineMethod("const_get", Arity.singleArgument(), "const_get");
	        defineMethod("const_missing", Arity.singleArgument());
	        defineMethod("const_set", Arity.twoArguments());
	        defineMethod("constants", Arity.noArguments());
	        defineMethod("dup", Arity.noArguments());
            defineMethod("extended", Arity.singleArgument());
	        defineMethod("included", Arity.singleArgument());
	        defineMethod("included_modules", Arity.noArguments());
	        defineMethod("initialize", Arity.optional());
	        defineMethod("instance_method", Arity.singleArgument());
	        defineMethod("instance_methods", Arity.optional());
	        defineMethod("method_defined?", Arity.singleArgument(), "method_defined");
	        defineMethod("module_eval", Arity.optional());
	        defineMethod("name", Arity.noArguments());
	        defineMethod("private_class_method", Arity.optional());
	        defineMethod("private_instance_methods", Arity.optional());
	        defineMethod("protected_instance_methods", Arity.optional());
	        defineMethod("public_class_method", Arity.optional());
	        defineMethod("public_instance_methods", Arity.optional());
	        defineMethod("to_s",  Arity.noArguments());
	
	        defineAlias("class_eval", "module_eval");
	
	        definePrivateMethod("alias_method", Arity.twoArguments());
	        definePrivateMethod("append_features", Arity.singleArgument());
	        definePrivateMethod("attr", Arity.optional());
	        definePrivateMethod("attr_reader", Arity.optional());
	        definePrivateMethod("attr_writer", Arity.optional());
	        definePrivateMethod("attr_accessor", Arity.optional());
	        definePrivateMethod("define_method", Arity.optional());
	        definePrivateMethod("extend_object", Arity.singleArgument());
	        definePrivateMethod("include", Arity.optional());
	        definePrivateMethod("method_added", Arity.singleArgument());
	        definePrivateMethod("module_function", Arity.optional());
	        definePrivateMethod("public", Arity.optional(), "rbPublic");
	        definePrivateMethod("protected", Arity.optional(), "rbProtected");
	        definePrivateMethod("private", Arity.optional(), "rbPrivate");
	        definePrivateMethod("remove_class_variable", Arity.singleArgument());
	        definePrivateMethod("remove_const", Arity.singleArgument());
	        definePrivateMethod("remove_method", Arity.optional());
	        definePrivateMethod("undef_method", Arity.singleArgument());
	
	        defineSingletonMethod("nesting", Arity.noArguments());
		}
	};
	
	protected Meta getMeta() {
		return new ModuleMeta();
	}
	
	public RubyClass newSubClass(String name, SinglyLinkedList parentCRef) {
		return new ModuleMetaClass(name, this, parentCRef);
	}

	protected IRubyObject allocateObject() {
        RubyModule instance = RubyModule.newModule(getRuntime(), null);
        
		instance.setMetaClass(this);
		
		return instance;
	}
    
    protected void defineModuleFunction(String name, Arity arity) {
        definePrivateMethod(name, arity);
        defineSingletonMethod(name, arity);
    }

    protected void defineModuleFunction(String name, Arity arity, String javaName) {
        definePrivateMethod(name, arity, javaName);
        defineSingletonMethod(name, arity, javaName);
    }
    
   /** 
    * Return an array of nested modules or classes.
    */
   public RubyArray nesting() {
	   IRuby runtime = getRuntime();
       RubyModule object = runtime.getObject();
       SinglyLinkedList base = runtime.getCurrentContext().peekCRef();
       RubyArray result = runtime.newArray();
       
       for (SinglyLinkedList current = base; current.getValue() != object; current = current.getNext()) {
           result.append((RubyModule)current.getValue());
       }
       
       return result;
   }
}
