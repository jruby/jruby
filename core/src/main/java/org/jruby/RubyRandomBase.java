package org.jruby;

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.Random;
import org.jruby.util.TypeConverter;

import java.math.BigInteger;
import java.nio.ByteBuffer;

import static org.jruby.api.Convert.checkToInteger;
import static org.jruby.api.Convert.numericToLong;
import static org.jruby.runtime.Visibility.PRIVATE;
import static org.jruby.util.TypeConverter.toFloat;

@JRubyClass(name = "Random::Base", parent = "Object")
public class RubyRandomBase extends RubyObject {
    public RubyRandomBase(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    static int getIntBigIntegerBuffer(byte[] src, int loc) {
        int v = 0;
        int idx = src.length - loc * 4 - 1;
        if (idx >= 0) {
            v |= (src[idx--] & 0xff);
            if (idx >= 0) {
                v |= (src[idx--] & 0xff) << 8;
                if (idx >= 0) {
                    v |= (src[idx--] & 0xff) << 16;
                    if (idx >= 0) {
                        v |= (src[idx--] & 0xff) << 24;
                    }
                }
            }
        }
        return v;
    }

    static void setIntBigIntegerBuffer(byte[] dest, int loc, int value) {
        int idx = dest.length - loc * 4 - 1;
        if (idx >= 0) {
            dest[idx--] = (byte) (value & 0xff);
            if (idx >= 0) {
                dest[idx--] = (byte) ((value >> 8) & 0xff);
                if (idx >= 0) {
                    dest[idx--] = (byte) ((value >> 16) & 0xff);
                    if (idx >= 0) {
                        dest[idx--] = (byte) ((value >> 24) & 0xff);
                    }
                }
            }
        }
    }

    protected RubyRandom.RandomType random = null;

    @JRubyMethod(visibility = PRIVATE, optional = 1, checkArity = false)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 0, 1);

        checkFrozen();

        random = new RubyRandom.RandomType((argc == 0) ? RubyRandom.randomSeed(context.runtime) : args[0]);

        return this;
    }

    @JRubyMethod
    public IRubyObject seed(ThreadContext context) {
        return random.getSeed();
    }

    @JRubyMethod(name = "rand", meta = true)
    public static IRubyObject randDefault(ThreadContext context, IRubyObject recv) {
        RubyRandom.RandomType random = RubyRandom.getDefaultRand(context);
        return RubyRandom.randFloat(context, random);
    }

    @JRubyMethod(name = "rand", meta = true)
    public static IRubyObject randDefault(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        IRubyObject v = RubyRandom.randRandom(context, recv, RubyRandom.getDefaultRand(context), arg);
        RubyRandom.checkRandomNumber(context, v, arg);
        return v;
    }

    @JRubyMethod(name = "rand")
    public IRubyObject rand(ThreadContext context) {
        return randFloat(context, random);
    }

    @JRubyMethod(name = "rand")
    public IRubyObject rand(ThreadContext context, IRubyObject arg) {
        IRubyObject v1 = randRandom(context, this, random, arg);
        IRubyObject v = v1;
        RubyRandom.checkRandomNumber(context, v, arg);
        return v;
    }

    // c: rand_int
    static IRubyObject randInt(ThreadContext context, IRubyObject self, RubyRandom.RandomType random, RubyInteger vmax,
                               boolean restrictive) {
        if (vmax instanceof RubyFixnum) {
            long max = RubyNumeric.fix2long(vmax);
            if (max == 0) return context.nil;
            if (max < 0) {
                if (restrictive) return context.nil;
                max = -max;
            }
            return randomUlongLimited(context, self, random, max - 1);
        } else {
            BigInteger big = vmax.getBigIntegerValue();
            if (big.equals(BigInteger.ZERO)) {
                return context.nil;
            }
            if (big.signum() < 0) {
                if (restrictive) return context.nil;
                big = big.abs();
            }
            big = big.subtract(BigInteger.ONE);
            return randomUlongLimitedBig(context, self, random, big);
        }
    }

    public static RubyFloat randFloat(ThreadContext context) {
        return randFloat(context, getDefaultRand(context));
    }

    public static RubyFloat randFloat(ThreadContext context, RubyRandom.RandomType random) {
        return context.runtime.newFloat(random.genrandReal());
    }

    public static RubyInteger randLimited(ThreadContext context, long limit) {
        return randLimitedFixnum(context, getDefaultRand(context), limit);
    }

    // c: limited_rand
    // limited_rand gets/returns ulong but we do this in signed long only.
    private static RubyInteger randLimitedFixnum(ThreadContext context, RubyRandom.RandomType random, long limit) {
        Ruby runtime = context.runtime;

        if (limit == 0) return RubyFixnum.zero(runtime);
        Random impl;
        if (random == null) {
            impl = new Random(runtime.getDefaultRandom().random.genrandInt32());
        } else {
            impl = random.impl;
        }
        return RubyFixnum.newFixnum(runtime, randLimitedFixnumInner(impl, limit));
    }

    public static long randLimitedFixnumInner(Random random, long limit) {
        long val;
        if (limit == 0) {
            val = 0;
        } else {
            long mask = makeMask(limit);
            // take care before code cleanup; it might break random sequence compatibility
            retry: while (true) {
                val = 0;
                for (int i = 1; 0 <= i; --i) {
                    if (((mask >>> (i * 32)) & 0xffffffffL) != 0) {
                        val |= (random.genrandInt32() & 0xffffffffL) << (i * 32);
                        val &= mask;
                    }
                    if (limit < val) {
                        continue retry;
                    }
                }
                break;
            }
        }
        return val;
    }

    public static RubyInteger randLimited(ThreadContext context, BigInteger limit) {
        return limitedBigRand(context, getDefaultRand(context), limit);
    }

    // c: random_ulong_limited
    // Note this is a modified version of randomUlongLimitedBig due to lack of ulong in Java
    private static IRubyObject randomUlongLimited(ThreadContext context, IRubyObject self, RubyRandom.RandomType random, long limit) {
        if (limit == 0) {
            return RubyFixnum.zero(context.runtime);
        }
        if (random == null) {
            int octets = limit <= Integer.MAX_VALUE ? 4 : 8;

            byte[] octetBytes = objRandomBytes(context, self, octets);
            long randLong = 0L;
            for (byte b : octetBytes) {
                randLong = (randLong << 8) + Byte.toUnsignedLong(b);
            }

            if (randLong < 0) {
                randLong = -randLong;
            }

            if (randLong > limit) {
                randLong = randLong % limit;
            }

            return RubyFixnum.newFixnum(context.runtime, randLong);
        }
        return randLimitedFixnum(context, random, limit);
    }

    // c: random_ulong_limited_big
    private static RubyInteger randomUlongLimitedBig(ThreadContext context, IRubyObject self, RubyRandom.RandomType random, BigInteger limit) {
        if (random == null) {
            int octets = (int) ((long) limit.bitLength() + 7) / 8;

            byte[] octetBytes = objRandomBytes(context, self, octets);
            BigInteger randBig = new BigInteger(octetBytes);

            if (randBig.compareTo(BigInteger.ZERO) < 0) {
                randBig = randBig.abs();
            }

            if (randBig.compareTo(limit) > 0) {
                randBig = randBig.mod(limit);
            }

            return RubyBignum.bignorm(context.runtime, randBig);
        }
        return limitedBigRand(context, random, limit);
    }

    // obj_random_bytes
    private static byte[] objRandomBytes(ThreadContext context, IRubyObject obj, long n) {
        IRubyObject len = RubyFixnum.newFixnum(context.runtime, n);
        IRubyObject v = obj.callMethod(context, "bytes", len);
        long l;
        TypeConverter.checkStringType(context.runtime, v);
        l = ((RubyString) v).length();
        if (l < n)
            throw context.runtime.newRangeError("random data too short " + l);
        else if (l > n)
            throw context.runtime.newRangeError("random data too long " + l);
        return ((RubyString) v).getBytes();
    }

    // c: limited_big_rand
    private static RubyInteger limitedBigRand(ThreadContext context, RubyRandom.RandomType random, BigInteger limit) {
        byte[] buf = limit.toByteArray();
        byte[] bytes = new byte[buf.length];
        int len = (buf.length + 3) / 4;
        // take care before code cleanup; it might break random sequence compatibility
        retry: while (true) {
            long mask = 0;
            boolean boundary = true;
            for (int idx = len - 1; 0 <= idx; --idx) {
                long lim = getIntBigIntegerBuffer(buf, idx) & 0xffffffffL;
                mask = (mask != 0) ? 0xffffffffL : makeMask(lim);
                long rnd;
                if (mask != 0) {
                    rnd = (random.genrandInt32() & 0xffffffffL) & mask;
                    if (boundary) {
                        if (lim < rnd) {
                            continue retry;
                        }
                        if (rnd < lim) {
                            boundary = false;
                        }
                    }
                } else {
                    rnd = 0;
                }
                setIntBigIntegerBuffer(bytes, idx, (int) rnd);
            }
            break;
        }
        return RubyBignum.newBignum(context.runtime, new BigInteger(bytes));
    }

    private static long makeMask(long x) {
        x = x | x >>> 1;
        x = x | x >>> 2;
        x = x | x >>> 4;
        x = x | x >>> 8;
        x = x | x >>> 16;
        x = x | x >>> 32;
        return x;
    }

    static RubyRandom.RandomType getDefaultRand(ThreadContext context) {
        return context.runtime.getDefaultRandom().random;
    }

    static RubyRandom getDefaultRandom(Ruby runtime) {
        return runtime.getDefaultRandom();
    }

    static void checkRandomNumber(ThreadContext context, IRubyObject v, IRubyObject arg0) {
        if (v == context.fals) {
            arg0.convertToInteger();
        } else if (v == context.nil) {
            invalidArgument(context, arg0);
        }
    }

    static void invalidArgument(ThreadContext context, IRubyObject arg0) {
        throw context.runtime.newArgumentError("invalid argument - " + arg0);
    }

    // c: rand_random
    static IRubyObject randRandom(ThreadContext context, IRubyObject obj, RubyRandom.RandomType rnd) {
        return RubyFloat.newFloat(context.runtime, randomReal(context, obj, rnd, true));
    }

    // c: rand_random
    static IRubyObject randRandom(ThreadContext context, IRubyObject obj, RubyRandom.RandomType rnd, IRubyObject vmax) {
        IRubyObject v;

        if (vmax.isNil()) return vmax;
        if (!(vmax instanceof RubyFloat)) {
            v = checkToInteger(context, vmax);
            if (!v.isNil()) return randInt(context, obj, rnd, (RubyInteger) v, true);
        }
        Ruby runtime = context.runtime;
        v = TypeConverter.checkFloatType(runtime, vmax);
        if (!v.isNil()) {
            double max = floatValue(context, v);
            if (max < 0.0) {
                return context.nil;
            } else {
                double r = randomReal(context, obj, rnd, true);
                if (max > 0.0) r *= max;
                return RubyFloat.newFloat(runtime, r);
            }
        }

        return randRange(context, obj, vmax, rnd);
    }

    private static double floatValue(ThreadContext context, IRubyObject v) {
        double value = v.convertToFloat().getDoubleValue();
        if (!Double.isFinite(value)) {
            domainError(context);
        }
        return value;
    }

    private static void domainError(ThreadContext context) {
        throw context.runtime.newErrnoEDOMError();
    }

    private static IRubyObject randRange(ThreadContext context, IRubyObject obj, IRubyObject vmax, RubyRandom.RandomType random) {
        IRubyObject v;
        IRubyObject nil = context.nil;
        IRubyObject beg;
        IRubyObject end;
        boolean excl;

        RubyBoolean fals = context.fals;
        RubyRange.RangeLike rangeLike = rangeValues(context, vmax);
        if (rangeLike == null) {
            return fals;
        }

        v = vmax = rangeLike.getRange(context);

        if (v.isNil()) domainError(context);

        Ruby runtime = context.runtime;

        beg = rangeLike.begin;
        end = rangeLike.end;
        excl = rangeLike.excl;

        if ((v = checkMaxInt(context, vmax)) != null) {
            do {
                if (v instanceof RubyFixnum) {
                    long max = ((RubyFixnum) v).value;
                    if (excl) {
                        max -= 1;
                    }
                    if (max >= 0) {
                        v = randLimitedFixnum(context, random, max);
                    } else {
                        v = nil;
                    }
                } else if (v instanceof RubyBignum) {
                    BigInteger big = ((RubyBignum) v).value;
                    if (big.signum() > 0) {
                        if (excl) {
                            big = big.subtract(BigInteger.ONE);
                        }
                        v = limitedBigRand(context, random, big);
                    } else {
                        v = nil;
                    }
                } else {
                    v = nil;
                }
            } while (false);
        } else if ((v = TypeConverter.checkFloatType(runtime, vmax)) != nil) {
            int scale = 1;
            double max = ((RubyFloat) v).value;
            double mid = 0.5;
            double r;
            if (Double.isInfinite(max)) {
                double min = floatValue(context, toFloat(runtime, beg)) / 2.0;
                max = floatValue(context, toFloat(runtime, end)) / 2.0;
                scale = 2;
                mid = max + min;
                max -= min;
            } else {
                checkFloatValue(context, max); // v
            }
            v = nil;
            if (max > 0.0) {
                if (random == null) {
                    byte[] bytes = objRandomBytes(context, obj, 8);
                    ByteBuffer buffer = ByteBuffer.wrap(bytes);
                    int a = buffer.getInt();
                    int b = buffer.getInt();
                    r = excl ? Random.intPairToRealExclusive(a, b) : Random.intPairToRealInclusive(a, b);
                } else {
                    r = random.genrandReal(excl);
                }
                if (scale > 1) {
                    return runtime.newFloat(+(+(+(r - 0.5) * max) * scale) + mid);
                }
                v = runtime.newFloat(r * max);
            } else if (max == 0.0 && !excl) {
                v = runtime.newFloat(0.0);
            }
        }

        if (beg instanceof RubyFixnum && v instanceof RubyFixnum) {
            long x = RubyNumeric.fix2long(beg) + RubyNumeric.fix2long(v);
            return RubyFixnum.newFixnum(runtime, x);
        }
        if (v == nil) {
            return v;
        } else if (v instanceof RubyBignum) {
            return ((RubyBignum) v).op_plus(context, beg);
        } else if (v instanceof RubyFloat) {
            IRubyObject f = TypeConverter.checkFloatType(runtime, beg);
            if (!f.isNil()) {
                return RubyFloat.newFloat(runtime, ((RubyFloat) v).getValue() + ((RubyFloat) f).getValue());
            }
        }
        return Helpers.invoke(context, beg, "+", v);
    }

    // c: float_value
    private static double floatValue(ThreadContext context, RubyFloat v) {
        final double x = v.value;
        checkFloatValue(context, x);
        return x;
    }

    private static void checkFloatValue(ThreadContext context, double x) {
        if (Double.isInfinite(x) || Double.isNaN(x)) {
            domainError(context);
        }
    }

    private static IRubyObject checkMaxInt(ThreadContext context, IRubyObject vmax) {
        if (!(vmax instanceof RubyFloat)) {
            IRubyObject v = checkToInteger(context, vmax);
            if (v != context.nil) return v;
        }
        return null;
    }

    static RubyRange.RangeLike rangeValues(ThreadContext context, IRubyObject range) {
        RubyRange.RangeLike like = RubyRange.rangeValues(context, range);
        if (like == null) return null;
        if (like.begin.isNil() || like.end.isNil()) domainError(context);
        return like;
    }

    // c: rb_random_bytes
    @JRubyMethod(name = "bytes")
    public IRubyObject bytes(ThreadContext context, IRubyObject arg) {
        return bytesCommon(context, random, arg);
    }

    protected static IRubyObject bytesCommon(ThreadContext context, RubyRandom.RandomType random, IRubyObject arg) {
        int n = RubyNumeric.num2int(arg);
        byte[] bytes = getBytes(random, n);
        return context.runtime.newString(new ByteList(bytes));
    }

    // MRI: rb_rand_bytes_int32, rand_mt_get_bytes
    static byte[] getBytes(RubyRandom.RandomType random, int n) {
        byte[] bytes = new byte[n];
        int idx = 0;
        for (; n >= 4; n -= 4) {
            int r = random.genrandInt32();
            for (int i = 0; i < 4; ++i) {
                bytes[idx++] = (byte) (r & 0xff);
                r >>>= 8;
            }
        }
        if (n > 0) {
            int r = random.genrandInt32();
            for (int i = 0; i < n; ++i) {
                bytes[idx++] = (byte) (r & 0xff);
                r >>>= 8;
            }
        }
        return bytes;
    }

    // try_get_rnd
    static RubyRandom.RandomType tryGetRandomType(ThreadContext context, IRubyObject obj) {
        if (obj.equals(context.runtime.getRandomClass())) return getDefaultRand(context);
        if (obj instanceof RubyRandom) return ((RubyRandom) obj).random;
        return null;
    }

    // rb_random_ulong_limited
    public static long randomLongLimited(ThreadContext context, IRubyObject obj, long limit) {
        RubyRandom.RandomType rnd = tryGetRandomType(context, obj);

        if (rnd == null) {
            RubyInteger v = Helpers.invokePublic(context, obj, "rand", context.runtime.newFixnum(limit + 1)).convertToInteger();
            long r = numericToLong(context, v);
            if (r < 0) throw context.runtime.newRangeError("random number too small " + r);
            if (r > limit) throw context.runtime.newRangeError("random number too big " + r);

            return r;
        }

        return randLimitedFixnumInner(rnd.impl, limit);
    }

    // c: rb_random_real
    public static double randomReal(ThreadContext context, IRubyObject obj, boolean excl) {
        RubyRandom.RandomType random = tryGetRandomType(context, obj);

        return randomReal(context, obj, random, excl);
    }

    private static double randomReal(ThreadContext context, IRubyObject obj, RubyRandom.RandomType random, boolean excl) {
        if (random == null) random = getDefaultRand(context);

        return random.genrandReal(excl);
    }
}
