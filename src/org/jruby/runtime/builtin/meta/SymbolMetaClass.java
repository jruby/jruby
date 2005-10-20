/*
 * Created on Jun 22, 2005
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.jruby.runtime.builtin.meta;

import org.jruby.IRuby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubySymbol;
import org.jruby.RubySymbol.SymbolMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public class SymbolMetaClass extends ObjectMetaClass {
    public SymbolMetaClass(IRuby runtime) {
        super("Symbol", RubySymbol.class, runtime.getObject());
    }
    
	public SymbolMetaClass(String name, RubyClass superClass, RubyModule parentModule) {
		super(name, RubySymbol.class, superClass, parentModule);
	}

    public SymbolMethod equal = new SymbolMethod(this, Arity.singleArgument(), Visibility.PUBLIC) {
        public IRubyObject invoke(RubySymbol self, IRubyObject[] args) {
            IRubyObject other = args[0];
            
            // Symbol table ensures only one instance for every name,
            // so object identity is enough to compare symbols.
            return self.getRuntime().newBoolean(self == other);
        }
    };

	protected class SymbolMeta extends Meta {
		public void initializeClass() {
            addMethod("==", equal);
            
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
