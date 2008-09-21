/*
 **** BEGIN LICENSE BLOCK *****
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

import static org.jruby.RubyEnumerator.enumeratorize;
import static org.jruby.util.Numeric.f_gcd;
import static org.jruby.util.Numeric.f_lcm;

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/** Implementation of the Integer class.
 *
 * @author  jpetersen
 */
@JRubyClass(name="Integer", parent="Numeric", include="Precision")
public abstract class RubyInteger extends RubyNumeric { 

    public static RubyClass createIntegerClass(Ruby runtime) {
        RubyClass integer = runtime.defineClass("Integer", runtime.getNumeric(),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        runtime.setInteger(integer);
        integer.kindOf = new RubyModule.KindOf() {
            public boolean isKindOf(IRubyObject obj, RubyModule type) {
                return obj instanceof RubyInteger;
            }

        };

        integer.getSingletonClass().undefineMethod("new");

        if (runtime.getInstanceConfig().getCompatVersion() == CompatVersion.RUBY1_8) {
            integer.includeModule(runtime.getPrecision());
        }

        integer.defineAnnotatedMethods(RubyInteger.class);
        
        return integer;
    }

    public RubyInteger(Ruby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass);
    }
    
    public RubyInteger(Ruby runtime, RubyClass rubyClass, boolean useObjectSpace) {
        super(runtime, rubyClass, useObjectSpace);
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
    @JRubyMethod(name = "integer?")
    public IRubyObject integer_p() {
        return getRuntime().getTrue();
    }

    /** int_upto
     * 
     */
    @JRubyMethod(name = "upto", frame = true)
    public IRubyObject upto(ThreadContext context, IRubyObject to, Block block) {
        final Ruby runtime = getRuntime();

        if (this instanceof RubyFixnum && to instanceof RubyFixnum) {
            RubyFixnum toFixnum = (RubyFixnum) to;
            final long toValue = toFixnum.getLongValue();
            final long fromValue = getLongValue();

            if (block.getBody().getArgumentType() == BlockBody.ZERO_ARGS) {
                final IRubyObject nil = runtime.getNil();
                for (long i = fromValue; i <= toValue; i++) {
                    block.yield(context, nil);
                }
            } else {
                for (long i = fromValue; i <= toValue; i++) {
                    block.yield(context, RubyFixnum.newFixnum(runtime, i));
                }
            }
        } else {
            RubyNumeric i = this;

            while (true) {
                if (i.callMethod(context, MethodIndex.OP_GT, ">", to).isTrue()) {
                    break;
                }
                block.yield(context, i);
                i = (RubyNumeric) i.callMethod(context, MethodIndex.OP_PLUS, "+", RubyFixnum.one(runtime));
            }
        }
        return this;
    }

    @JRubyMethod(name = "upto", frame = true, compat = CompatVersion.RUBY1_9)
    public IRubyObject upto19(final ThreadContext context, IRubyObject to, final Block block) {
        return block.isGiven() ? upto(context, to, block) : enumeratorize(context.getRuntime(), this, "upto", to);
    }

    /** int_downto
     * 
     */
    // TODO: Make callCoerced work in block context...then fix downto, step, and upto.
    @JRubyMethod(name = "downto", frame = true)
    public IRubyObject downto(ThreadContext context, IRubyObject to, Block block) {
        final Ruby runtime = getRuntime();

        if (this instanceof RubyFixnum && to instanceof RubyFixnum) {
            RubyFixnum toFixnum = (RubyFixnum) to;
            final long toValue = toFixnum.getLongValue();
            if (block.getBody().getArgumentType() == BlockBody.ZERO_ARGS) {
                final IRubyObject nil = runtime.getNil();
                for (long i = getLongValue(); i >= toValue; i--) {
                    block.yield(context, nil);
                }
            } else {
                for (long i = getLongValue(); i >= toValue; i--) {
                    block.yield(context, RubyFixnum.newFixnum(getRuntime(), i));
                }
            }
        } else {
            RubyNumeric i = this;

            while (true) {
                if (i.callMethod(context, MethodIndex.OP_LT, "<", to).isTrue()) {
                    break;
                }
                block.yield(context, i);
                i = (RubyNumeric) i.callMethod(context, MethodIndex.OP_MINUS, "-", RubyFixnum.one(getRuntime()));
            }
        }
        return this;
    }

    @JRubyMethod(name = "downto", frame = true, compat = CompatVersion.RUBY1_9)
    public IRubyObject downto19(final ThreadContext context, IRubyObject to, final Block block) {
        return block.isGiven() ? downto(context, to, block) : enumeratorize(context.getRuntime(), this, "downto", to);
    }

    @JRubyMethod(name = "times", frame = true)
    public IRubyObject times(ThreadContext context, Block block) {
        final Ruby runtime = context.getRuntime();

        if (this instanceof RubyFixnum) {
            final long value = getLongValue();
            if (block.getBody().getArgumentType() == BlockBody.ZERO_ARGS) {
                final IRubyObject nil = runtime.getNil();
                for (long i = 0; i < value; i++) {
                    block.yield(context, nil);
                }
            } else {
                for (long i = 0; i < value; i++) {
                    block.yield(context, RubyFixnum.newFixnum(runtime, i));
                }
            }
        } else {
            RubyNumeric i = RubyFixnum.zero(runtime);
            while (true) {
                if (!i.callMethod(context, MethodIndex.OP_LT, "<", this).isTrue()) {
                    break;
                }
                block.yield(context, i);
                i = (RubyNumeric) i.callMethod(context, MethodIndex.OP_PLUS, "+", RubyFixnum.one(runtime));
            }
        }

        return this;
    }

    @JRubyMethod(name = "times", frame = true, compat = CompatVersion.RUBY1_9)
    public IRubyObject times19(final ThreadContext context, final Block block) {
        return block.isGiven() ? times(context, block) : enumeratorize(context.getRuntime(), this, "times");
    }

    /** int_succ
     * 
     */
    @JRubyMethod(name = {"succ", "next"})
    public IRubyObject succ(ThreadContext context) {
        if (this instanceof RubyFixnum) {
            return RubyFixnum.newFixnum(context.getRuntime(), getLongValue() + 1L);
        } else {
            return callMethod(context, MethodIndex.OP_PLUS, "+", RubyFixnum.one(context.getRuntime()));
        }
    }

    /** int_chr
     * 
     */
    @JRubyMethod(name = "chr")
    public RubyString chr() {
        if (getLongValue() < 0 || getLongValue() > 0xff) {
            throw getRuntime().newRangeError(this.toString() + " out of char range");
        }
        return RubyString.newString(getRuntime(), new ByteList(new byte[]{(byte)getLongValue()}, false)); 
    }

    /** int_to_i
     * 
     */
    @JRubyMethod(name = {"to_i", "to_int", "floor", "ceil", "round", "truncate"})
    public RubyInteger to_i() {
        return this;
    }
    
    /** integer_to_r
     * 
     */
    @JRubyMethod(name = "to_r", compat = CompatVersion.RUBY1_9)
    public IRubyObject to_r(ThreadContext context) {
        return RubyRational.newRationalCanonicalize(context, this);
    }
    

    @JRubyMethod(name = "odd?")
    public static RubyBoolean odd_p(ThreadContext context, IRubyObject recv) {
        Ruby runtime = context.getRuntime();
        if (recv.callMethod(context, "%", RubyFixnum.two(runtime)) != RubyFixnum.zero(runtime)) {
            return runtime.getTrue();
        }
        return runtime.getFalse();
    }

    @JRubyMethod(name = "even?")
    public static RubyBoolean even_p(ThreadContext context, IRubyObject recv) {
        Ruby runtime = context.getRuntime();
        if (recv.callMethod(context, "%", RubyFixnum.two(runtime)) == RubyFixnum.zero(runtime)) {
            return runtime.getTrue();
        }
        return runtime.getFalse();
    }

    @JRubyMethod(name = "pred")
    public static IRubyObject pred(ThreadContext context, IRubyObject recv) {
        return recv.callMethod(context, "-", RubyFixnum.one(context.getRuntime()));
    }

    /** rb_gcd
     * 
     */
    @JRubyMethod(name = "gcd", compat = CompatVersion.RUBY1_9)
    public static IRubyObject gcd(ThreadContext context, IRubyObject recv, IRubyObject other) {
        return f_gcd(context, recv, RubyRational.intValue(context, other));
    }    

    /** rb_lcm
     * 
     */
    @JRubyMethod(name = "lcm", compat = CompatVersion.RUBY1_9)
    public static IRubyObject lcm(ThreadContext context, IRubyObject recv, IRubyObject other) {
        return f_lcm(context, recv, RubyRational.intValue(context, other));
    }    

    /** rb_gcdlcm
     * 
     */
    @JRubyMethod(name = "gcdlcm", compat = CompatVersion.RUBY1_9)
    public static IRubyObject gcdlcm(ThreadContext context, IRubyObject recv, IRubyObject other) {
        other = RubyRational.intValue(context, other);
        return context.getRuntime().newArray(f_gcd(context, recv, other), f_lcm(context, recv, other));
    }

    /*  ================
     *  Singleton Methods
     *  ================ 
     */

    /** rb_int_induced_from
     * 
     */
    @JRubyMethod(name = "induced_from", meta = true, compat = CompatVersion.RUBY1_8)
    public static IRubyObject induced_from(ThreadContext context, IRubyObject recv, IRubyObject other) {
        if (other instanceof RubyFixnum || other instanceof RubyBignum) {
            return other;
        } else if (other instanceof RubyFloat || other instanceof RubyRational) {
            return other.callMethod(context, "to_i");
        } else {
            throw recv.getRuntime().newTypeError(
                    "failed to convert " + other.getMetaClass().getName() + " into Integer");
        }
    }
}
