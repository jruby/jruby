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

import static org.jruby.api.Convert.*;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.rangeError;
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

        random = new RubyRandom.RandomType(context, (argc == 0) ? RubyRandom.randomSeed(context.runtime) : args[0]);

        return this;
    }

    @JRubyMethod
    public IRubyObject seed(ThreadContext context) {
        return random.getSeed();
    }

    @JRubyMethod(name = "rand", meta = true)
    public static IRubyObject randDefault(ThreadContext context, IRubyObject recv) {
        return RubyRandom.randFloat(context, RubyRandom.getDefaultRand(context));
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
        var v = randRandom(context, this, random, arg);
        RubyRandom.checkRandomNumber(context, v, arg);
        return v;
    }

    // c: rand_int
    static IRubyObject randInt(ThreadContext context, IRubyObject self, RubyRandom.RandomType random, RubyInteger vmax,
                               boolean restrictive) {
        if (vmax instanceof RubyFixnum fixnum) {
            long max = fixnum.getValue();
            if (max == 0) return context.nil;
            if (max < 0) {
                if (restrictive) return context.nil;
                max = -max;
            }
            return randomUlongLimited(context, self, random, max - 1);
        } else {
            BigInteger big = vmax.asBigInteger(context);
            if (big.equals(BigInteger.ZERO)) return context.nil;

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
        return asFloat(context, random.genrandReal());
    }

    public static RubyInteger randLimited(ThreadContext context, long limit) {
        return randLimitedFixnum(context, getDefaultRand(context), limit);
    }

    // c: limited_rand
    // limited_rand gets/returns ulong but we do this in signed long only.
    private static RubyInteger randLimitedFixnum(ThreadContext context, RubyRandom.RandomType random, long limit) {
        if (limit == 0) return asFixnum(context, 0);
        Random impl = random == null ? new Random(context.runtime.getDefaultRandom().random.genrandInt32()) : random.impl;

        return asFixnum(context, randLimitedFixnumInner(impl, limit));
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
                    if (limit < val) continue retry;
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
        if (limit == 0) return asFixnum(context, 0);
        if (random == null) {
            int octets = limit <= Integer.MAX_VALUE ? 4 : 8;

            byte[] octetBytes = objRandomBytes(context, self, octets);
            long randLong = 0L;
            for (byte b : octetBytes) {
                randLong = (randLong << 8) + Byte.toUnsignedLong(b);
            }

            if (randLong < 0) randLong = -randLong;
            if (randLong > limit) randLong = randLong % limit;

            return asFixnum(context, randLong);
        }
        return randLimitedFixnum(context, random, limit);
    }

    // c: random_ulong_limited_big
    private static RubyInteger randomUlongLimitedBig(ThreadContext context, IRubyObject self, RubyRandom.RandomType random, BigInteger limit) {
        if (random == null) {
            int octets = (int) ((long) limit.bitLength() + 7) / 8;
            byte[] octetBytes = objRandomBytes(context, self, octets);
            BigInteger randBig = new BigInteger(octetBytes);

            if (randBig.compareTo(BigInteger.ZERO) < 0) randBig = randBig.abs();
            if (randBig.compareTo(limit) > 0) randBig = randBig.mod(limit);

            return RubyBignum.bignorm(context.runtime, randBig);
        }
        return limitedBigRand(context, random, limit);
    }

    // obj_random_bytes
    private static byte[] objRandomBytes(ThreadContext context, IRubyObject obj, long n) {
        IRubyObject len = asFixnum(context, n);
        IRubyObject v = obj.callMethod(context, "bytes", len);

        TypeConverter.checkStringType(context.runtime, v);
        long l = ((RubyString) v).length();
        if (l < n) throw rangeError(context, "random data too short " + l);
        if (l > n) throw rangeError(context, "random data too long " + l);
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
        throw argumentError(context, "invalid argument - " + arg0);
    }

    // c: rand_random
    static IRubyObject randRandom(ThreadContext context, IRubyObject obj, RubyRandom.RandomType rnd) {
        return asFloat(context, randomReal(context, obj, rnd, true));
    }

    // c: rand_random
    static IRubyObject randRandom(ThreadContext context, IRubyObject obj, RubyRandom.RandomType rnd, IRubyObject vmax) {
        IRubyObject v;

        if (vmax.isNil()) return vmax;
        if (!(vmax instanceof RubyFloat)) {
            v = checkToInteger(context, vmax);
            if (!v.isNil()) return randInt(context, obj, rnd, (RubyInteger) v, true);
        }
        v = TypeConverter.checkFloatType(context.runtime, vmax);
        if (!v.isNil()) {
            double max = floatValue(context, v);
            if (max < 0.0) return context.nil;

            double r = randomReal(context, obj, rnd, true);
            if (max > 0.0) r *= max;
            return asFloat(context, r);
        }

        return randRange(context, obj, vmax, rnd);
    }

    private static double floatValue(ThreadContext context, IRubyObject v) {
        double value = v.convertToFloat().asDouble(context);
        if (!Double.isFinite(value)) domainError(context);
        return value;
    }

    private static void domainError(ThreadContext context) {
        throw context.runtime.newErrnoEDOMError();
    }

    private static IRubyObject randRange(ThreadContext context, IRubyObject obj, IRubyObject vmax, RubyRandom.RandomType random) {
        RubyRange.RangeLike rangeLike = rangeValues(context, vmax);
        if (rangeLike == null) return context.fals;

        IRubyObject nil = context.nil;
        IRubyObject v = vmax = rangeLike.getRange(context);

        if (v.isNil()) domainError(context);

        IRubyObject beg = rangeLike.begin;
        IRubyObject end = rangeLike.end;
        boolean excl = rangeLike.excl;

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
        } else if ((v = TypeConverter.checkFloatType(context.runtime, vmax)) != nil) {
            int scale = 1;
            double max = ((RubyFloat) v).value;
            double mid = 0.5;
            double r;
            if (Double.isInfinite(max)) {
                double min = floatValue(context, toFloat(context.runtime, beg)) / 2.0;
                max = floatValue(context, toFloat(context.runtime, end)) / 2.0;
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
                if (scale > 1) return asFloat(context, +(+(+(r - 0.5) * max) * scale) + mid);

                v = asFloat(context, r * max);
            } else if (max == 0.0 && !excl) {
                v = asFloat(context, 0.0);
            }
        }

        if (beg instanceof RubyFixnum begf && v instanceof RubyFixnum vv) {
            return asFixnum(context, begf.getValue() + vv.getValue());
        }
        if (v == nil) return v;
        if (v instanceof RubyBignum big) return big.op_plus(context, beg);
        if (v instanceof RubyFloat flote) {
            IRubyObject f = TypeConverter.checkFloatType(context.runtime, beg);
            if (!f.isNil()) return asFloat(context,flote.getValue() + ((RubyFloat) f).getValue());
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
        if (Double.isInfinite(x) || Double.isNaN(x)) domainError(context);
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
        int n = toInt(context, arg);
        return context.runtime.newString(new ByteList(getBytes(random, n)));
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
            RubyInteger v = Helpers.invokePublic(context, obj, "rand", asFixnum(context, limit + 1)).convertToInteger();
            long r = v.asLong(context);
            if (r < 0) throw rangeError(context, "random number too small " + r);
            if (r > limit) throw rangeError(context, "random number too big " + r);

            return r;
        }

        return randLimitedFixnumInner(rnd.impl, limit);
    }

    // c: rb_random_real
    public static double randomReal(ThreadContext context, IRubyObject obj, boolean excl) {
        return randomReal(context, obj, tryGetRandomType(context, obj), excl);
    }

    private static double randomReal(ThreadContext context, IRubyObject obj, RubyRandom.RandomType random, boolean excl) {
        if (random == null) random = getDefaultRand(context);

        return random.genrandReal(excl);
    }
}
