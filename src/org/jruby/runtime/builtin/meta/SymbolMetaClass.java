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
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2006 Derek Berner <derek.berner@state.nm.us>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
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
import org.jruby.RubySymbol;
import org.jruby.RubySymbol.SymbolMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.collections.SinglyLinkedList;

public class SymbolMetaClass extends ObjectMetaClass {
    public SymbolMetaClass(IRuby runtime) {
        super("Symbol", RubySymbol.class, runtime.getObject());
    }
    
	public SymbolMetaClass(String name, RubyClass superClass, SinglyLinkedList parentCRef) {
		super(name, RubySymbol.class, superClass, parentCRef);
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
            defineMethod("to_sym", Arity.noArguments());
            defineSingletonMethod("all_symbols", Arity.noArguments());
            defineAlias("dup", "clone");
	        defineAlias("id2name", "to_s");
	        defineAlias("to_int", "to_i");
				
	        getMetaClass().undefineMethod("new");
		}
	};
	
	protected Meta getMeta() {
		return new SymbolMeta();
	}
	
	public RubyClass newSubClass(String name, SinglyLinkedList parentCRef) {
		return new SymbolMetaClass(name, this, parentCRef);
	}
    
    public IRubyObject all_symbols() {
        return getRuntime().newArray(getRuntime().getSymbolTable().all_symbols());
    }

}
