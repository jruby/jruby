/*
 * Created on Jun 22, 2005
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.jruby.runtime.builtin.meta;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubySymbol;
import org.jruby.runtime.Arity;

public class SymbolMetaClass extends ObjectMetaClass {
    public SymbolMetaClass(Ruby runtime) {
        super("Symbol", RubySymbol.class, runtime.getObject());
    }
    
	public SymbolMetaClass(String name, RubyClass superClass, RubyModule parentModule) {
		super(name, RubySymbol.class, superClass, parentModule);
	}

	protected class SymbolMeta extends Meta {
		public void initializeClass() {
	        defineMethod("==", Arity.singleArgument(), "equal");
	        defineMethod("clone", Arity.noArguments(), "rbClone");
	        defineMethod("freeze", Arity.noArguments()); 
	        defineMethod("hash", Arity.noArguments()); 
	        defineMethod("inspect", Arity.noArguments());
	        defineMethod("taint", Arity.noArguments());
	        defineMethod("to_i", Arity.noArguments());
	        defineMethod("to_s", Arity.noArguments());
	        defineAlias("dup", "clone");
	        defineAlias("id2name", "to_s");
	        defineAlias("to_int", "to_i");
				
	        getMetaClass().undefineMethod("new");
		}
	};
	
	protected Meta getMeta() {
		return new SymbolMeta();
	}
	
	public RubyClass newSubClass(String name, RubyModule parentModule) {
		return new SymbolMetaClass(name, this, parentModule);
	}
}
