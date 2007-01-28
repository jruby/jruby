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
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
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
package org.jruby;

import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/** Implementation of the Integer class.
 *
 * @author  jpetersen
 */
public abstract class RubyInteger extends RubyNumeric { 

    public static RubyClass createIntegerClass(IRuby runtime) {
        RubyClass integer = runtime.defineClass("Integer", runtime.getClass("Numeric"),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyInteger.class);
        integer.getSingletonClass().undefineMethod("allocate");
        integer.getSingletonClass().undefineMethod("new");

        integer.defineFastMethod("integer?", callbackFactory.getFastMethod("int_p"));
        integer.defineMethod("upto", callbackFactory.getMethod("upto", IRubyObject.class));
        integer.defineMethod("downto", callbackFactory.getMethod("downto", IRubyObject.class));
        integer.defineMethod("times", callbackFactory.getMethod("times"));

        integer.includeModule(runtime.getModule("Precision"));

        integer.defineFastMethod("succ", callbackFactory.getFastMethod("succ"));
        integer.defineFastMethod("next", callbackFactory.getFastMethod("succ"));
        integer.defineFastMethod("chr", callbackFactory.getFastMethod("chr"));
        integer.defineFastMethod("to_i", callbackFactory.getFastMethod("to_i"));
        integer.defineFastMethod("to_int", callbackFactory.getFastMethod("to_i"));
        integer.defineFastMethod("floor", callbackFactory.getFastMethod("to_i"));
        integer.defineFastMethod("ceil", callbackFactory.getFastMethod("to_i"));
        integer.defineFastMethod("round", callbackFactory.getFastMethod("to_i"));
        integer.defineFastMethod("truncate", callbackFactory.getFastMethod("to_i"));

        integer.getMetaClass().defineFastMethod("induced_from", callbackFactory.getFastSingletonMethod("induced_from",
                IRubyObject.class));
        return integer;
    }

    public RubyInteger(IRuby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass);
    }
    
    public RubyInteger convertToInteger() {
    	return this;
    }

    // conversion
    protected RubyFloat toFloat() {
        return RubyFloat.newFloat(getRuntime(), getDoubleValue());
    }

    /*  ================
     *  Instance Methods
     *  ================ 
     */

    /** int_int_p
     * 
     */
    public IRubyObject int_p() {
        return getRuntime().getTrue();
    }

    /** int_upto
     * 
     */
    public IRubyObject upto(IRubyObject to, Block block) {
        ThreadContext context = getRuntime().getCurrentContext();

        if (this instanceof RubyFixnum && to instanceof RubyFixnum) {

            RubyFixnum toFixnum = (RubyFixnum) to;
            long toValue = toFixnum.getLongValue();
            for (long i = getLongValue(); i <= toValue; i++) {
                context.yield(RubyFixnum.newFixnum(getRuntime(), i), block);
            }
        } else {
            RubyNumeric i = this;

            while (true) {
                if (i.callMethod(context, ">", to).isTrue()) {
                    break;
                }
                context.yield(i, block);
                i = (RubyNumeric) i.callMethod(context, "+", RubyFixnum.one(getRuntime()));
            }
        }
        return this;
    }

    /** int_downto
     * 
     */
    // TODO: Make callCoerced work in block context...then fix downto, step, and upto.
    public IRubyObject downto(IRubyObject to, Block block) {
        ThreadContext context = getRuntime().getCurrentContext();

        if (this instanceof RubyFixnum && to instanceof RubyFixnum) {
            RubyFixnum toFixnum = (RubyFixnum) to;
            long toValue = toFixnum.getLongValue();
            for (long i = getLongValue(); i >= toValue; i--) {
                context.yield(RubyFixnum.newFixnum(getRuntime(), i), block);
            }
        } else {
            RubyNumeric i = this;

            while (true) {
                if (i.callMethod(context, "<", to).isTrue()) {
                    break;
                }
                context.yield(i, block);
                i = (RubyNumeric) i.callMethod(context, "-", RubyFixnum.one(getRuntime()));
            }
        }
        return this;
    }

    public IRubyObject times(Block block) {
        ThreadContext context = getRuntime().getCurrentContext();

        if (this instanceof RubyFixnum) {

            long value = getLongValue();
            for (long i = 0; i < value; i++) {
                context.yield(RubyFixnum.newFixnum(getRuntime(), i), block);
            }
        } else {
            RubyNumeric i = RubyFixnum.zero(getRuntime());
            while (true) {
                if (!i.callMethod(context, "<", this).isTrue()) {
                    break;
                }
                context.yield(i, block);
                i = (RubyNumeric) i.callMethod(context, "+", RubyFixnum.one(getRuntime()));
            }
        }

        return this;
    }

    /** int_succ
     * 
     */
    public IRubyObject succ() {
        if (this instanceof RubyFixnum) {
            return RubyFixnum.newFixnum(getRuntime(), getLongValue() + 1L);
        } else {
            return callMethod(getRuntime().getCurrentContext(), "+", RubyFixnum.one(getRuntime()));
        }
    }

    /** int_chr
     * 
     */
    public RubyString chr() {
        if (getLongValue() < 0 || getLongValue() > 0xff) {
            throw getRuntime().newRangeError(this.toString() + " out of char range");
        }
        return getRuntime().newString(new String(new char[] { (char) getLongValue() }));
    }

    /** int_to_i
     * 
     */
    public RubyInteger to_i() {
        return this;
    }

    /*  ================
     *  Singleton Methods
     *  ================ 
     */

    /** rb_int_induced_from
     * 
     */
    public static IRubyObject induced_from(IRubyObject recv, IRubyObject other) {
        if (other instanceof RubyFixnum || other instanceof RubyBignum) {
            return other;
        } else if (other instanceof RubyFloat) {
            return other.callMethod(recv.getRuntime().getCurrentContext(), "to_i");
        } else {
            throw recv.getRuntime().newTypeError(
                    "failed to convert " + other.getMetaClass().getName() + " into Integer");
    }
}
}
