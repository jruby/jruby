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
	        defineFastMethod("===", Arity.singleArgument(), "op_eqq");
	        defineFastMethod("<=>", Arity.singleArgument(), "op_cmp");
	        defineFastMethod("<", Arity.singleArgument(), "op_lt");
	        defineFastMethod("<=", Arity.singleArgument(), "op_le");
	        defineFastMethod(">", Arity.singleArgument(), "op_gt");
	        defineFastMethod(">=", Arity.singleArgument(), "op_ge");
	        defineFastMethod("ancestors", Arity.noArguments());
	        defineFastMethod("class_variables", Arity.noArguments());
	        defineFastMethod("clone", Arity.noArguments(), "rbClone");
	        defineFastMethod("const_defined?", Arity.singleArgument(), "const_defined");
	        defineFastMethod("const_get", Arity.singleArgument(), "const_get");
	        defineMethod("const_missing", Arity.singleArgument());
	        defineFastMethod("const_set", Arity.twoArguments());
	        defineFastMethod("constants", Arity.noArguments());
	        defineFastMethod("dup", Arity.noArguments());
            defineMethod("extended", Arity.singleArgument());
	        defineFastMethod("included", Arity.singleArgument());
	        defineFastMethod("included_modules", Arity.noArguments());
	        defineMethod("initialize", Arity.optional());
	        defineFastMethod("instance_method", Arity.singleArgument());
	        defineFastMethod("instance_methods", Arity.optional());
	        defineFastMethod("method_defined?", Arity.singleArgument(), "method_defined");
	        defineMethod("module_eval", Arity.optional());
	        defineFastMethod("name", Arity.noArguments());
	        defineFastMethod("private_class_method", Arity.optional());
	        defineFastMethod("private_instance_methods", Arity.optional());
	        defineFastMethod("protected_instance_methods", Arity.optional());
	        defineFastMethod("public_class_method", Arity.optional());
	        defineFastMethod("public_instance_methods", Arity.optional());
	        defineFastMethod("to_s",  Arity.noArguments());
	
	        defineAlias("class_eval", "module_eval");
	
	        defineFastPrivateMethod("alias_method", Arity.twoArguments());
	        defineFastPrivateMethod("append_features", Arity.singleArgument());
	        defineFastPrivateMethod("attr", Arity.optional());
	        defineFastPrivateMethod("attr_reader", Arity.optional());
	        defineFastPrivateMethod("attr_writer", Arity.optional());
	        defineFastPrivateMethod("attr_accessor", Arity.optional());
	        definePrivateMethod("define_method", Arity.optional());
	        defineFastPrivateMethod("extend_object", Arity.singleArgument());
	        defineFastPrivateMethod("include", Arity.optional());
	        definePrivateMethod("method_added", Arity.singleArgument());
	        defineFastPrivateMethod("module_function", Arity.optional());
	        definePrivateMethod("public", Arity.optional(), "rbPublic");
	        definePrivateMethod("protected", Arity.optional(), "rbProtected");
	        definePrivateMethod("private", Arity.optional(), "rbPrivate");
	        defineFastPrivateMethod("remove_class_variable", Arity.singleArgument());
	        defineFastPrivateMethod("remove_const", Arity.singleArgument());
	        defineFastPrivateMethod("remove_method", Arity.optional());
	        defineFastPrivateMethod("undef_method", Arity.singleArgument());
	
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
