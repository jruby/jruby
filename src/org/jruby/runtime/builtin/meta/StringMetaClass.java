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
 * Copyright (C) 2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2005 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
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

import java.util.Locale;

import org.jruby.IRuby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyInteger;
import org.jruby.RubyString;
import org.jruby.RubyString.StringMethod;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.PrintfFormat;
import org.jruby.util.collections.SinglyLinkedList;

public class StringMetaClass extends ObjectMetaClass {
    public StringMetaClass(IRuby runtime) {
        super("String", RubyString.class, runtime.getObject());
    }

    private StringMetaClass(String name, RubyClass superClass, SinglyLinkedList parentCRef) {
        super(name, RubyString.class, superClass, parentCRef);
    }

    public StringMethod hash = new StringMethod(this, Arity.noArguments(), Visibility.PUBLIC) {
        public IRubyObject invoke(RubyString self, IRubyObject[] args) {
            return self.getRuntime().newFixnum(self.toString().hashCode());
        }
    };
    
    public StringMethod to_s = new StringMethod(this, Arity.noArguments(), Visibility.PUBLIC) {
        public IRubyObject invoke(RubyString self, IRubyObject[] args) {
            return self;
        }
    };
    
    /* rb_str_cmp_m */
    public StringMethod op_cmp = new StringMethod(this, Arity.singleArgument(), Visibility.PUBLIC) {
        public IRubyObject invoke(RubyString self, IRubyObject[] args) {
            if (args[0] instanceof RubyString) {
                return getRuntime().newFixnum(self.cmp((RubyString) args[0]));
            }
                
            if (args[0].respondsTo("to_str") && args[0].respondsTo("<=>")) {
                IRubyObject tmp = args[0].callMethod("<=>", self);

                if (!tmp.isNil()) {
                    return tmp instanceof RubyFixnum ? tmp.callMethod("-") :
                        getRuntime().newFixnum(0).callMethod("-", tmp);
                }
            }
            
            return getRuntime().getNil();
        }
    };

    public StringMethod equal = new StringMethod(this, Arity.singleArgument(), Visibility.PUBLIC) {
        public IRubyObject invoke(RubyString self, IRubyObject[] args) {
            IRubyObject other = args[0];
            
            if (other == self) {
                return self.getRuntime().getTrue();
            } else if (!(other instanceof RubyString)) {
                return self.getRuntime().getFalse();
            }
            /* use Java implementation if both different String instances */
            return self.getRuntime().newBoolean(
                    self.toString().equals(((RubyString) other).toString()));
            
        }
    };
    
    public StringMethod veryEqual = new StringMethod(this, Arity.singleArgument(), Visibility.PUBLIC) {
        public IRubyObject invoke(RubyString self, IRubyObject[] args) {
            IRubyObject other = args[0];
            IRubyObject truth = self.callMethod("==", other);
            
            return truth == self.getRuntime().getNil() ? self.getRuntime().getFalse() : truth;
        }
    };

    public StringMethod op_plus = new StringMethod(this, Arity.singleArgument(), Visibility.PUBLIC) {
        public IRubyObject invoke(RubyString self, IRubyObject[] args) {
            IRubyObject other = args[0];
            RubyString str = RubyString.stringValue(other);
            
            return (RubyString) self.newString(self.toString() + str.toString()).infectBy(str);
        }
    };

    public StringMethod op_mul = new StringMethod(this, Arity.singleArgument(), Visibility.PUBLIC) {
        public IRubyObject invoke(RubyString self, IRubyObject[] args) {
            IRubyObject other = args[0];
            
            RubyInteger otherInteger =
                    (RubyInteger) other.convertType(RubyInteger.class, "Integer", "to_i");
            long len = otherInteger.getLongValue();
    
            if (len < 0) {
                throw self.getRuntime().newArgumentError("negative argument");
            }
    
            if (len > 0 && Long.MAX_VALUE / len < self.getValue().length()) {
                throw self.getRuntime().newArgumentError("argument too big");
            }
            StringBuffer sb = new StringBuffer((int) (self.getValue().length() * len));
    
            for (int i = 0; i < len; i++) {
                sb.append(self.getValue());
            }
    
            RubyString newString = self.newString(sb.toString());
            newString.setTaint(self.isTaint());
            return newString;
        }
    };

    public StringMethod format = new StringMethod(this, Arity.singleArgument(), Visibility.PUBLIC) {
        public IRubyObject invoke(RubyString self, IRubyObject[] args) {
            IRubyObject arg = args[0];
            
            if (arg instanceof RubyArray) {
                Object[] args2 = new Object[((RubyArray) arg).getLength()];
                for (int i = 0; i < args2.length; i++) {
                    args2[i] = JavaUtil.convertRubyToJava(((RubyArray) arg).entry(i));
                }
                return self.getRuntime().newString(new PrintfFormat(Locale.US, self.toString()).sprintf(args2));
            }
            return self.getRuntime().newString(new PrintfFormat(Locale.US, self.toString()).sprintf(JavaUtil.convertRubyToJava(arg)));
        }
    };

    protected class StringMeta extends Meta {
	    protected void initializeClass() {
	        includeModule(getRuntime().getModule("Comparable"));
	        includeModule(getRuntime().getModule("Enumerable"));
	
            addMethod("<=>", op_cmp);
            addMethod("==", equal);
            addMethod("===", veryEqual);
            addMethod("+", op_plus);
            addMethod("*", op_mul);
            addMethod("%", format);
            addMethod("hash", hash);
            addMethod("to_s", to_s);
            
	        defineMethod("[]", Arity.optional(), "aref");
	        defineMethod("[]=", Arity.optional(), "aset");
	        defineMethod("=~", Arity.singleArgument(), "match");
	        defineMethod("~", Arity.noArguments(), "match2");
	        defineMethod("capitalize", Arity.noArguments());
	        defineMethod("capitalize!", Arity.noArguments(), "capitalize_bang");
	        defineMethod("casecmp", Arity.singleArgument());
	        defineMethod("center", Arity.optional());
	        defineMethod("chop", Arity.noArguments());
	        defineMethod("chop!", Arity.noArguments(), "chop_bang");
	        defineMethod("chomp", Arity.optional());
	        defineMethod("chomp!", Arity.optional(), "chomp_bang");
	        defineMethod("clone", Arity.noArguments(), "rbClone");
	        defineMethod("concat", Arity.singleArgument());
	        defineMethod("count", Arity.optional());
	        defineMethod("crypt", Arity.singleArgument());
	        defineMethod("delete", Arity.optional());
	        defineMethod("delete!", Arity.optional(), "delete_bang");
	        defineMethod("downcase", Arity.noArguments());
	        defineMethod("downcase!", Arity.noArguments(), "downcase_bang");
	        defineMethod("dump", Arity.noArguments());
	        defineMethod("dup", Arity.noArguments());
	        defineMethod("each_line", Arity.optional());
	        defineMethod("each_byte", Arity.noArguments());
	        defineMethod("empty?", Arity.noArguments(), "empty");
	        defineMethod("gsub", Arity.optional());
	        defineMethod("gsub!", Arity.optional(), "gsub_bang");
	        defineMethod("hex", Arity.noArguments());
	        defineMethod("include?", Arity.singleArgument(), "include");
	        defineMethod("index", Arity.optional());
	        defineMethod("initialize", Arity.optional(), "initialize");
	        defineMethod("initialize_copy", Arity.singleArgument(), "replace");
	        defineMethod("insert", Arity.twoArguments());
	        defineMethod("inspect", Arity.noArguments());
	        defineMethod("length", Arity.noArguments());
	        defineMethod("ljust", Arity.optional());
	        defineMethod("lstrip", Arity.noArguments());
	        defineMethod("lstrip!", Arity.noArguments(), "lstrip_bang");
	        defineMethod("match", Arity.singleArgument(), "match3");
	        defineMethod("oct", Arity.noArguments());
	        defineMethod("replace", Arity.singleArgument());
	        defineMethod("reverse", Arity.noArguments());
	        defineMethod("reverse!", Arity.noArguments(), "reverse_bang");
	        defineMethod("rindex", Arity.optional());
	        defineMethod("rjust", Arity.optional());
	        defineMethod("rstrip", Arity.noArguments());
	        defineMethod("rstrip!", Arity.noArguments(), "rstrip_bang");
	        defineMethod("scan", Arity.singleArgument());
	        defineMethod("slice!", Arity.optional(), "slice_bang");
	        defineMethod("split", Arity.optional());
	        defineMethod("strip", Arity.noArguments());
	        defineMethod("strip!", Arity.noArguments(), "strip_bang");
	        defineMethod("succ", Arity.noArguments());
	        defineMethod("succ!", Arity.noArguments(), "succ_bang");
	        defineMethod("squeeze", Arity.optional());
	        defineMethod("squeeze!", Arity.optional(), "squeeze_bang");
	        defineMethod("sub", Arity.optional());
	        defineMethod("sub!", Arity.optional(), "sub_bang");
	        defineMethod("sum", Arity.optional());
	        defineMethod("swapcase", Arity.noArguments());
	        defineMethod("swapcase!", Arity.noArguments(), "swapcase_bang");
	        defineMethod("to_f", Arity.noArguments());
	        defineMethod("to_i", Arity.optional());
	        defineMethod("to_str", Arity.noArguments());
	        defineMethod("to_sym", Arity.noArguments());
	        defineMethod("tr", Arity.twoArguments());
	        defineMethod("tr!", Arity.twoArguments(), "tr_bang");
	        defineMethod("tr_s", Arity.twoArguments());
	        defineMethod("tr_s!", Arity.twoArguments(), "tr_s_bang");
	        defineMethod("unpack", Arity.singleArgument());
	        defineMethod("upcase", Arity.noArguments());
	        defineMethod("upcase!", Arity.noArguments(), "upcase_bang");
	        defineMethod("upto", Arity.singleArgument());
	
	        defineAlias("<<", "concat");
	        defineAlias("each", "each_line");
	        defineAlias("eql?", "==");
	        defineAlias("intern", "to_sym");
	        defineAlias("next", "succ");
	        defineAlias("next!", "succ!");
	        defineAlias("size", "length");
	        defineAlias("slice", "[]");
	    }
    };
    
    protected Meta getMeta() {
    	return new StringMeta();
    }

    public RubyClass newSubClass(String name, SinglyLinkedList parentCRef) {
        return new StringMetaClass(name, this, parentCRef);
    }

    protected IRubyObject allocateObject() {
        RubyString newString = getRuntime().newString("");
		newString.setMetaClass(this);
		return newString;
    }
}
