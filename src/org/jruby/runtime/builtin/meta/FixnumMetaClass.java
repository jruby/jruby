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

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyInteger;
import org.jruby.RubyModule;
import org.jruby.RubySymbol;
import org.jruby.runtime.Arity;
import org.jruby.runtime.builtin.IRubyObject;

public class FixnumMetaClass extends IntegerMetaClass {
	public FixnumMetaClass(Ruby runtime) {
	       super("Fixnum", RubyFixnum.class, runtime.getClasses().getIntegerClass());
	}
	
	public FixnumMetaClass(String name, RubyClass superClass, RubyModule parentModule) {
     super(name, RubyFixnum.class, superClass, parentModule);
 }
	protected void initializeClass() {
	        defineMethod("quo", Arity.singleArgument());
	        defineMethod("to_f", Arity.noArguments());
	        defineMethod("to_i", Arity.noArguments());
	        defineMethod("to_s", Arity.noArguments());
	        defineMethod("taint", Arity.noArguments());
	        defineMethod("freeze", Arity.noArguments());
	        defineMethod("<<", Arity.singleArgument(), "op_lshift");
	        defineMethod(">>", Arity.singleArgument(), "op_rshift");
	        defineMethod("+", Arity.singleArgument(), "op_plus");
	        defineMethod("-", Arity.singleArgument(), "op_minus");
	        defineMethod("*", Arity.singleArgument(), "op_mul");
	        defineMethod("/", Arity.singleArgument(), "op_div");
	        defineAlias("div", "/");
	        defineMethod("%", Arity.singleArgument(), "op_mod");
	        defineMethod("**", Arity.singleArgument(), "op_pow");
	        defineMethod("&", Arity.singleArgument(), "op_and");
	        defineMethod("|", Arity.singleArgument(), "op_or");
	        defineMethod("^", Arity.singleArgument(), "op_xor");
	        defineMethod("size", Arity.noArguments());
	        defineMethod("[]", Arity.singleArgument(), "aref");
	        defineMethod("hash", Arity.noArguments());
	        defineMethod("id2name", Arity.noArguments());
	        defineMethod("~", Arity.noArguments(), "invert");
	        defineMethod("id", Arity.noArguments());

	        defineSingletonMethod("induced_from", Arity.singleArgument(), "induced_from");
	}

	public RubyClass newSubClass(String name, RubyModule parentModule) {
        return new FixnumMetaClass(name, this, parentModule);
	}

    public RubyInteger induced_from(IRubyObject number) {
    	// TODO: Remove once asNumeric in RubyObject tries to convert
        if (number instanceof RubySymbol) {
            return (RubyInteger) number.callMethod("to_i");
        } 

        return ((IntegerMetaClass) getRuntime().getClasses().getIntegerClass()).induced_from(number);
    }


}
