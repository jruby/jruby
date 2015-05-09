/*
 **** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import org.jcodings.Encoding;
import org.jcodings.exception.EncodingException;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.Numeric;
import org.jruby.util.StringSupport;

import static org.jruby.RubyEnumerator.enumeratorizeWithSize;
import static org.jruby.util.Numeric.checkInteger;
import static org.jruby.util.Numeric.f_gcd;
import static org.jruby.util.Numeric.f_lcm;
import static org.jruby.RubyEnumerator.SizeFn;

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

        integer.setClassIndex(ClassIndex.INTEGER);
        integer.setReifiedClass(RubyInteger.class);
        
        integer.kindOf = new RubyModule.JavaClassKindOf(RubyInteger.class);

        integer.getSingletonClass().undefineMethod("new");

        integer.defineAnnotatedMethods(RubyInteger.class);
        
        return integer;
    }

    public RubyInteger(Ruby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass);
    }

    public RubyInteger(RubyClass rubyClass) {
        super(rubyClass);
    }
    
    public RubyInteger(Ruby runtime, RubyClass rubyClass, boolean useObjectSpace) {
        super(runtime, rubyClass, useObjectSpace);
    }     

    @Deprecated
    public RubyInteger(Ruby runtime, RubyClass rubyClass, boolean useObjectSpace, boolean canBeTainted) {
        super(runtime, rubyClass, useObjectSpace, canBeTainted);
    }     

    @Override
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
    @Override
    @JRubyMethod(name = "integer?")
    public IRubyObject integer_p() {
        return getRuntime().getTrue();
    }

    /** int_upto
     * 
     */
    @JRubyMethod
    public IRubyObject upto(ThreadContext context, IRubyObject to, Block block) {
        if (block.isGiven()) {
            if (this instanceof RubyFixnum && to instanceof RubyFixnum) {
                fixnumUpto(context, ((RubyFixnum)this).getLongValue(), ((RubyFixnum)to).getLongValue(), block);
            } else {
                duckUpto(context, this, to, block);
            }
            return this;
        } else {
            return enumeratorizeWithSize(context, this, "upto", new IRubyObject[] { to }, uptoSize(context, this, to));
        }
    }

    private static void fixnumUpto(ThreadContext context, long from, long to, Block block) {
        // We must avoid "i++" integer overflow when (to == Long.MAX_VALUE).
        Ruby runtime = context.runtime;
        if (block.getSignature() == Signature.NO_ARGUMENTS) {
            IRubyObject nil = runtime.getNil();
            long i;
            for (i = from; i < to; i++) {
                block.yield(context, nil);
            }
            if (i <= to) {
                block.yield(context, nil);
            }
        } else {
            long i;
            for (i = from; i < to; i++) {
                block.yield(context, RubyFixnum.newFixnum(runtime, i));
            }
            if (i <= to) {
                block.yield(context, RubyFixnum.newFixnum(runtime, i));
            }
        }
    }

    private static void duckUpto(ThreadContext context, IRubyObject from, IRubyObject to, Block block) {
        Ruby runtime = context.runtime;
        IRubyObject i = from;
        RubyFixnum one = RubyFixnum.one(runtime);
        while (true) {
            if (i.callMethod(context, ">", to).isTrue()) {
                break;
            }
            block.yield(context, i);
            i = i.callMethod(context, "+", one);
        }
    }

    private static SizeFn uptoSize(final ThreadContext context, final IRubyObject from, final IRubyObject to) {
        return new SizeFn() {
            @Override
            public IRubyObject size(IRubyObject[] args) {
                return intervalStepSize(context, from, to, RubyFixnum.one(context.runtime), false);
            }
        };
    }

    /** int_downto
     * 
     */
    // TODO: Make callCoerced work in block context...then fix downto, step, and upto.
    @JRubyMethod
    public IRubyObject downto(ThreadContext context, IRubyObject to, Block block) {
        if (block.isGiven()) {
            if (this instanceof RubyFixnum && to instanceof RubyFixnum) {
                fixnumDownto(context, ((RubyFixnum)this).getLongValue(), ((RubyFixnum)to).getLongValue(), block);
            } else {
                duckDownto(context, this, to, block);
            }
            return this;
        } else {
            return enumeratorizeWithSize(context, this, "downto", new IRubyObject[] { to }, downToSize(context, this, to));
        }
    }

    private static void fixnumDownto(ThreadContext context, long from, long to, Block block) {
        // We must avoid "i--" integer overflow when (to == Long.MIN_VALUE).
        Ruby runtime = context.runtime;
        if (block.getSignature() == Signature.NO_ARGUMENTS) {
            IRubyObject nil = runtime.getNil();
            long i;
            for (i = from; i > to; i--) {
                block.yield(context, nil);
            }
            if (i >= to) {
                block.yield(context, nil);
            }
        } else {
            long i;
            for (i = from; i > to; i--) {
                block.yield(context, RubyFixnum.newFixnum(runtime, i));
            }
            if (i >= to) {
                block.yield(context, RubyFixnum.newFixnum(runtime, i));
            }
        }
    }

    private static void duckDownto(ThreadContext context, IRubyObject from, IRubyObject to, Block block) {
        Ruby runtime = context.runtime;
        IRubyObject i = from;
        RubyFixnum one = RubyFixnum.one(runtime);
        while (true) {
            if (i.callMethod(context, "<", to).isTrue()) {
                break;
            }
            block.yield(context, i);
            i = i.callMethod(context, "-", one);
        }
    }

    private static SizeFn downToSize(final ThreadContext context, final IRubyObject from, final IRubyObject to) {
        return new SizeFn() {
            @Override
            public IRubyObject size(IRubyObject[] args) {
                return intervalStepSize(context, from, to, RubyFixnum.newFixnum(context.runtime, -1), false);
            }
        };
    }

    @JRubyMethod
    public IRubyObject times(ThreadContext context, Block block) {
        if (block.isGiven()) {
            Ruby runtime = context.runtime;
            IRubyObject i = RubyFixnum.zero(runtime);
            RubyFixnum one = RubyFixnum.one(runtime);
            while (true) {
                if (!i.callMethod(context, "<", this).isTrue()) {
                    break;
                }
                block.yield(context, i);
                i = i.callMethod(context, "+", one);
            }
            return this;
        } else {
            return enumeratorizeWithSize(context, this, "times", timesSizeFn(context.runtime));
        }
    }

    protected SizeFn timesSizeFn(final Ruby runtime) {
        final RubyInteger self = this;
        return new SizeFn() {
            @Override
            public IRubyObject size(IRubyObject[] args) {
                RubyFixnum zero = RubyFixnum.zero(runtime);
                if ((self instanceof RubyFixnum && getLongValue() < 0)
                        || self.callMethod("<", zero).isTrue()) {
                    return zero;
                }

                return self;
            }
        };
    }

    /** int_succ
     * 
     */
    @JRubyMethod(name = {"succ", "next"})
    public IRubyObject succ(ThreadContext context) {
        if (this instanceof RubyFixnum) {
            return ((RubyFixnum) this).op_plus_one(context);
        } else {
            return callMethod(context, "+", RubyFixnum.one(context.runtime));
        }
    }

    static final ByteList[] SINGLE_CHAR_BYTELISTS;
    public static final ByteList[] SINGLE_CHAR_BYTELISTS19;
    static {
        SINGLE_CHAR_BYTELISTS = new ByteList[256];
        SINGLE_CHAR_BYTELISTS19 = new ByteList[256];
        for (int i = 0; i < 256; i++) {
            ByteList usascii = new ByteList(new byte[]{(byte)i}, false);
            SINGLE_CHAR_BYTELISTS[i] = usascii;
            SINGLE_CHAR_BYTELISTS19[i] = i < 0x80 ?
                new ByteList(new byte[]{(byte)i}, USASCIIEncoding.INSTANCE)
                :
                new ByteList(
                    new byte[]{(byte)i},
                    ASCIIEncoding.INSTANCE);
        }
    }

    /** int_chr
     * 
     */
    public RubyString chr(ThreadContext context) {
        return chr19(context);
    }

    @JRubyMethod(name = "chr")
    public RubyString chr19(ThreadContext context) {
        Ruby runtime = context.runtime;
        int value = (int)getLongValue();
        if (value >= 0 && value <= 0xFF) {
            ByteList bytes = SINGLE_CHAR_BYTELISTS19[value];
            return RubyString.newStringShared(runtime, bytes, bytes.getEncoding());
        } else {
            Encoding enc = runtime.getDefaultInternalEncoding();
            if (value > 0xFF && (enc == null || enc == ASCIIEncoding.INSTANCE)) {
                throw runtime.newRangeError(this.toString() + " out of char range");
            } else {
                if (enc == null) enc = USASCIIEncoding.INSTANCE;
                return RubyString.newStringNoCopy(runtime, fromEncodedBytes(runtime, enc, (int)value), enc, 0);
            }
        }
    }

    @JRubyMethod(name = "chr")
    public RubyString chr19(ThreadContext context, IRubyObject arg) {
        Ruby runtime = context.runtime;
        long value = getLongValue();
        Encoding enc;
        if (arg instanceof RubyEncoding) {
            enc = ((RubyEncoding)arg).getEncoding();
        } else {
            enc =  arg.convertToString().toEncoding(runtime);
        }
        if (enc == ASCIIEncoding.INSTANCE && value >= 0x80) {
            return chr19(context);
        }
        return RubyString.newStringNoCopy(runtime, fromEncodedBytes(runtime, enc, value), enc, 0);
    }

    private ByteList fromEncodedBytes(Ruby runtime, Encoding enc, long value) {
        int n;
        try {
            n = value < 0 ? 0 : enc.codeToMbcLength((int)value);
        } catch (EncodingException ee) {
            n = 0;
        }

        if (n <= 0) throw runtime.newRangeError(this.toString() + " out of char range");
        
        ByteList bytes = new ByteList(n);

        boolean ok = false;
        try {
            enc.codeToMbc((int)value, bytes.getUnsafeBytes(), 0);
            ok = StringSupport.preciseLength(enc, bytes.unsafeBytes(), 0, n) == n;
        } catch (EncodingException e) {
            // ok = false, fall through
        }

        if (!ok) {
            throw runtime.newRangeError("invalid codepoint " + String.format("0x%x in ", value) + enc.getCharsetName());
        }

        bytes.setRealSize(n);
        return bytes;
    }

    /** int_ord
     * 
     */
    @JRubyMethod(name = "ord")
    public IRubyObject ord(ThreadContext context) {
        return this;
    }

    /** int_to_i
     * 
     */
    @JRubyMethod(name = {"to_i", "to_int", "floor", "ceil", "truncate"})
    public IRubyObject to_i() {
        return this;
    }

    @Override
    public IRubyObject round() {
        return this;
    }

    @JRubyMethod(name = "round")
    public IRubyObject round19() {
        return this;
    }

    @JRubyMethod(name = "round")
    public IRubyObject round19(ThreadContext context, IRubyObject arg) {
        int ndigits = RubyNumeric.num2int(arg);
        if (ndigits > 0) return RubyKernel.new_float(this, this);
        if (ndigits == 0) return this;
        Ruby runtime = context.runtime;
        
        long bytes = (this instanceof RubyFixnum) ? 8 : RubyFixnum.fix2long(callMethod("size"));
        /* If 10**N/2 > this, return 0 */
        /* We have log_256(10) > 0.415241 and log_256(1/2)=-0.125 */
        if (-0.415241 * ndigits - 0.125 > bytes) {
            return RubyFixnum.zero(runtime);
        }
        
        IRubyObject f = Numeric.int_pow(context, 10, -ndigits);

        if (this instanceof RubyFixnum && f instanceof RubyFixnum) {
            long x = ((RubyFixnum)this).getLongValue();
            long y = ((RubyFixnum)f).getLongValue();
            boolean neg = x < 0;
            if (neg) x = -x;
            x = (x + y / 2) / y * y;
            if (neg) x = -x;
            return RubyFixnum.newFixnum(runtime, x);
        } else if (f instanceof RubyFloat) {
            return RubyFixnum.zero(runtime);
        } else {
            IRubyObject h = f.callMethod(context, "/", RubyFixnum.two(runtime));
            IRubyObject r = callMethod(context, "%", f);
            IRubyObject n = callMethod(context, "-", r);
            String op = callMethod(context, "<", RubyFixnum.zero(runtime)).isTrue() ? "<=" : "<";
            if (!r.callMethod(context, op, h).isTrue()) n = n.callMethod(context, "+", f);
            return n;
        }
    }

    /** integer_to_r
     * 
     */
    @JRubyMethod(name = "to_r")
    public IRubyObject to_r(ThreadContext context) {
        return RubyRational.newRationalCanonicalize(context, this);
    }

    /** integer_rationalize
     *
     */
    @JRubyMethod(name = "rationalize", optional = 1)
    public IRubyObject rationalize(ThreadContext context, IRubyObject[] args) {
        return to_r(context);
    }
    

    @JRubyMethod(name = "odd?")
    public RubyBoolean odd_p(ThreadContext context) {
        Ruby runtime = context.runtime;
        if (callMethod(context, "%", RubyFixnum.two(runtime)) != RubyFixnum.zero(runtime)) {
            return runtime.getTrue();
        }
        return runtime.getFalse();
    }

    @JRubyMethod(name = "even?")
    public RubyBoolean even_p(ThreadContext context) {
        Ruby runtime = context.runtime;
        if (callMethod(context, "%", RubyFixnum.two(runtime)) == RubyFixnum.zero(runtime)) {
            return runtime.getTrue();
        }
        return runtime.getFalse();
    }

    @JRubyMethod(name = "pred")
    public IRubyObject pred(ThreadContext context) {
        return callMethod(context, "-", RubyFixnum.one(context.runtime));
    }

    /** rb_gcd
     * 
     */
    @JRubyMethod(name = "gcd")
    public IRubyObject gcd(ThreadContext context, IRubyObject other) {
        checkInteger(context, other);
        return f_gcd(context, this, RubyRational.intValue(context, other));
    }    

    /** rb_lcm
     * 
     */
    @JRubyMethod(name = "lcm")
    public IRubyObject lcm(ThreadContext context, IRubyObject other) {
        checkInteger(context, other);
        return f_lcm(context, this, RubyRational.intValue(context, other));
    }    

    /** rb_gcdlcm
     * 
     */
    @JRubyMethod(name = "gcdlcm")
    public IRubyObject gcdlcm(ThreadContext context, IRubyObject other) {
        checkInteger(context, other);
        other = RubyRational.intValue(context, other);
        return context.runtime.newArray(f_gcd(context, this, other), f_lcm(context, this, other));
    }

    @Override
    @JRubyMethod(name = "numerator")
    public IRubyObject numerator(ThreadContext context) {
        return this;
    }

    @Override
    @JRubyMethod(name = "denominator")
    public IRubyObject denominator(ThreadContext context) {
        return RubyFixnum.one(context.runtime);
    }

    /*  ================
     *  Singleton Methods
     *  ================ 
     */

    /** rb_int_induced_from
     * 
     */
    @Deprecated
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
