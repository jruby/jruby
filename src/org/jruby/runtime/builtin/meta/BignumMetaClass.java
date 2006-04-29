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
import org.jruby.runtime.Arity;
import org.jruby.util.collections.SinglyLinkedList;

public class BignumMetaClass extends ObjectMetaClass {
    public BignumMetaClass(IRuby runtime) {
        super("Bignum", RubyBignum.class, runtime.getClass("Integer"));
    }
    
	public BignumMetaClass(String name, RubyClass superClass, SinglyLinkedList parentCRef) {
		super(name, RubyBignum.class, superClass, parentCRef);
	}

	protected class BignumMeta extends Meta {
		protected void initializeClass() {
	        defineMethod("~", Arity.noArguments(), "op_invert");
	        defineMethod("&", Arity.singleArgument(), "op_and");
	        defineMethod("<<", Arity.singleArgument(), "op_lshift");
	        defineMethod("%", Arity.singleArgument(), "op_mod");
	        defineMethod("+", Arity.singleArgument(), "op_plus");
	        defineMethod("*", Arity.singleArgument(), "op_mul");
	        defineMethod("**", Arity.singleArgument(), "op_pow");
	        defineMethod("-", Arity.singleArgument(), "op_minus");
	        defineMethod("modulo", Arity.singleArgument(), "op_mod");
	        defineMethod("/", Arity.singleArgument(), "op_div");
	        defineMethod(">>", Arity.singleArgument(), "op_rshift");
	        defineMethod("|", Arity.singleArgument(), "op_or");
	        defineMethod("^", Arity.singleArgument(), "op_xor");
	        defineMethod("-@", Arity.noArguments(), "op_uminus");
	        defineMethod("[]", Arity.singleArgument(), "aref");
	        defineMethod("coerce", Arity.singleArgument(), "coerce");
	        defineMethod("remainder", Arity.singleArgument(), "remainder");
	        defineMethod("hash", Arity.noArguments(), "hash");
	        defineMethod("size", Arity.noArguments(), "size");
	        defineMethod("quo", Arity.singleArgument(), "quo");
	        defineMethod("to_f", Arity.noArguments(), "to_f");
	        defineMethod("to_i", Arity.noArguments(), "to_i");
	        defineMethod("to_s", Arity.optional(), "to_s");
		}
	};
	
	protected Meta getMeta() {
		return new BignumMeta();
	}
	
	public RubyClass newSubClass(String name, SinglyLinkedList parentCRef) {
		return new BignumMetaClass(name, this, parentCRef);
	}
}
