/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2017 JRuby Community
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
package org.jruby.ext.securerandom;

import org.jruby.RubyBignum;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyInteger;
import org.jruby.RubyRange;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ConvertBytes;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * securerandom.rb native parts.
 */
@JRubyModule(name = "SecureRandom")
public class RubySecureRandom {

    @JRubyMethod(meta = true, name = "random_bytes")
    public static IRubyObject random_bytes(ThreadContext context, IRubyObject self) {
        return RubyString.newStringNoCopy(context.runtime, nextBytes(context, 16));
    }

    @JRubyMethod(meta = true, name = "random_bytes", alias = "gen_random") // gen_random for 'better' compat (not-used)
    public static IRubyObject random_bytes(ThreadContext context, IRubyObject self, IRubyObject n) {
        return RubyString.newStringNoCopy(context.runtime, nextBytes(context, n));
    }

    @JRubyMethod(meta = true)
    public static IRubyObject hex(ThreadContext context, IRubyObject self) {
        return RubyString.newStringNoCopy(context.runtime, ConvertBytes.twosComplementToHexBytes(nextBytes(context, 16), false));
    }

    @JRubyMethod(meta = true)
    public static IRubyObject hex(ThreadContext context, IRubyObject self, IRubyObject n) {
        return RubyString.newStringNoCopy(context.runtime, ConvertBytes.twosComplementToHexBytes(nextBytes(context, n), false));
    }

    @JRubyMethod(meta = true)
    public static IRubyObject uuid(ThreadContext context, IRubyObject self) {
        return RubyString.newStringNoCopy(context.runtime, ConvertBytes.bytesToUUIDBytes(nextBytes(context, 16), false));
    }

    private static byte[] nextBytes(ThreadContext context, IRubyObject n) {
        return nextBytes(context, n.isNil() ? 16 : n.convertToInteger().getIntValue());
    }

    private static byte[] nextBytes(ThreadContext context, int size) {
        if (size < 0) throw context.runtime.newArgumentError("negative argument: " + size);

        byte[] bytes = new byte[size];
        getSecureRandom(context).nextBytes(bytes);

        return bytes;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject random_number(ThreadContext context, IRubyObject self) {
        return randomDouble(context);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject random_number(ThreadContext context, IRubyObject self, IRubyObject n) {
        if (n instanceof RubyFixnum) {
            final long bound = ((RubyFixnum) n).getLongValue();
            return ( bound < 0 ) ? randomDouble(context) : randomFixnum(context, 0, bound - 1);
        }
        if (n instanceof RubyFloat) {
            final double bound = ((RubyFloat) n).getDoubleValue();
            return ( bound < 0 ) ? randomDouble(context) : randomDouble(context, 0, bound - Double.MIN_VALUE);
        }
        if (n instanceof RubyBignum) {
            final BigInteger bound = ((RubyBignum) n).getBigIntegerValue();
            return ( bound.signum() < 0 ) ? randomDouble(context) : randomBignum(context, 0, bound);
        }

        if (n instanceof RubyRange) {
            final IRubyObject beg = ((RubyRange) n).begin(context);
            final IRubyObject end = ((RubyRange) n).end(context);
            final boolean exclude = ((RubyRange) n).isExcludeEnd();

            if (beg instanceof RubyFixnum && end instanceof RubyFixnum) {
                long lower = ((RubyFixnum) beg).getLongValue();
                long upper = ((RubyFixnum) end).getLongValue();
                if ( lower > upper ) return randomDouble(context);
                if ( exclude ) upper--; // rand(2) never returns 2 but rand(0..2) does
                return randomFixnum(context, lower, upper);
            }
            if (beg instanceof RubyInteger && end instanceof RubyInteger) {
                BigInteger lower = ((RubyInteger) beg).getBigIntegerValue();
                BigInteger upper = ((RubyInteger) end).getBigIntegerValue();
                if ( lower.compareTo(upper) > 0 ) return randomDouble(context);
                if ( ! exclude ) upper = upper.add(BigInteger.ONE);
                return randomBignum(context, lower, upper);
            }
            if (beg instanceof RubyFloat && end instanceof RubyFloat) {
                double lower = ((RubyFloat) beg).getDoubleValue();
                double upper = ((RubyFloat) end).getDoubleValue();
                if ( lower > upper ) return randomDouble(context);
                if ( exclude ) upper = upper - Double.MIN_VALUE;
                return randomDouble(context, lower, upper);
            }
        }

        throw context.runtime.newArgumentError("invalid argument - " + n.anyToString());
    }

    private static RubyFixnum randomFixnum(final ThreadContext context, final long lower, final long upper) {
        double rnd = getSecureRandom(context).nextDouble();
        rnd = rnd * upper + (1.0 - rnd) * lower + rnd;
        return context.runtime.newFixnum((long) Math.floor(rnd));
    }

    // NOTE: upper is exclusive here compared to others
    private static RubyBignum randomBignum(final ThreadContext context, final Number lower, final BigInteger upperExc) {
        BigInteger lowerBig = lower instanceof BigInteger ? (BigInteger) lower : BigInteger.valueOf(lower.longValue());
        BigInteger bound = upperExc.subtract(lowerBig);
        BigInteger rnd = nextBigInteger(getSecureRandom(context), bound, bound.bitLength());
        return RubyBignum.newBignum(context.runtime, rnd.add(lowerBig));
    }

    private static final int BI_ADD_BITS = 96; // 8+4 random bytes 'wasted'

    // <0, bound)
    private static BigInteger nextBigInteger(final SecureRandom random, final BigInteger bound, final int bits) {
        BigInteger val = new BigInteger(bits + BI_ADD_BITS, random);
        BigInteger rnd = val.mod(bound);

        if (val.add(bound).subtract(rnd).subtract(BigInteger.ONE).bitLength() >= bits + BI_ADD_BITS) {
            return nextBigInteger(random, bound, bits); // highly unlikely to recurse at all
        }
        return rnd;
    }

    private static RubyFloat randomDouble(final ThreadContext context, final double lower, final double upper) {
        double rnd = getSecureRandom(context).nextDouble();
        return context.runtime.newFloat( rnd * upper + (1.0 - rnd) * lower );
    }

    private static RubyFloat randomDouble(final ThreadContext context) {
        return context.runtime.newFloat( getSecureRandom(context).nextDouble() );
    }

    private static SecureRandom getSecureRandom(ThreadContext context) {
        return context.getSecureRandom();
    }

}
