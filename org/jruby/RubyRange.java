/*
 * RubyRange.java - No description
 * Created on 26. Juli 2001, 00:01
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This file is part of JRuby
 * 
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */
package org.jruby;

import org.jruby.exceptions.*;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author jpetersen
 * @version $Revision$
 */
public class RubyRange extends RubyObject {

    private IRubyObject begin;
    private IRubyObject end;
    private boolean isExclusive;

    public RubyRange(Ruby ruby) {
        super(ruby, ruby.getRubyClass("Range"));
    }

    public void init(IRubyObject begin, IRubyObject end, RubyBoolean isExclusive) {
        if (!(begin instanceof RubyFixnum && end instanceof RubyFixnum)) {
            try {
                begin.callMethod("<=>", end);
            } catch (RaiseException rExcptn) {
                throw new ArgumentError(getRuntime(), "bad value for range");
            }
        }

        this.begin = begin;
        this.end = end;
        this.isExclusive = isExclusive.isTrue();
    }

    public static RubyClass createRangeClass(Ruby ruby) {
        RubyClass rangeClass = ruby.defineClass("Range", ruby.getClasses().getObjectClass());

        rangeClass.includeModule(ruby.getClasses().getEnumerableModule());

        rangeClass.defineMethod("==", CallbackFactory.getMethod(RubyRange.class, "equal", IRubyObject.class));
        rangeClass.defineMethod("===", CallbackFactory.getMethod(RubyRange.class, "op_eqq", IRubyObject.class));
        rangeClass.defineMethod("first", CallbackFactory.getMethod(RubyRange.class, "first"));
        rangeClass.defineMethod("begin", CallbackFactory.getMethod(RubyRange.class, "first"));
        rangeClass.defineMethod("last", CallbackFactory.getMethod(RubyRange.class, "last"));
        rangeClass.defineMethod("end", CallbackFactory.getMethod(RubyRange.class, "last"));

        rangeClass.defineMethod("to_s", CallbackFactory.getMethod(RubyRange.class, "inspect"));
        rangeClass.defineMethod("inspect", CallbackFactory.getMethod(RubyRange.class, "inspect"));
        rangeClass.defineMethod("exclude_end?", CallbackFactory.getMethod(RubyRange.class, "exclude_end_p"));
        rangeClass.defineMethod("length", CallbackFactory.getMethod(RubyRange.class, "length"));
        rangeClass.defineMethod("size", CallbackFactory.getMethod(RubyRange.class, "length"));
        rangeClass.defineMethod("each", CallbackFactory.getMethod(RubyRange.class, "each"));
        rangeClass.defineMethod("initialize", CallbackFactory.getOptMethod(RubyRange.class, "initialize"));

        return rangeClass;
    }

    /**
     * Converts this Range to a pair of integers representing a start position 
     * and length.  If either of the range's endpoints is negative, it is added to 
     * the <code>limit</code> parameter in an attempt to arrive at a position 
     * <i>p</i> such that <i>0&nbsp;&lt;=&nbsp;p&nbsp;&lt;=&nbsp;limit</i>. If 
     * <code>truncate</code> is true, the result will be adjusted, if possible, so 
     * that <i>begin&nbsp;+&nbsp;length&nbsp;&lt;=&nbsp;limit</i>.  If <code>strict</code> 
     * is true, an exception will be raised if the range can't be converted as 
     * described above; otherwise it just returns <b>null</b>. 
     * 
     * @param limit    the size of the object (e.g., a String or Array) that 
     *                 this range is being evaluated against.
     * @param truncate if true, result must fit within the range <i>(0..limit)</i>.
     * @param strict   if true, raises an exception if the range can't be converted.
     * @return         a two-element array representing a start value and a length, 
     *                 or <b>null</b> if the conversion failed.
     */
    public long[] getBeginLength(long limit, boolean truncate, boolean isStrict) {
        long beginLong = RubyNumeric.num2long(begin);
        long endLong = RubyNumeric.num2long(end);

        if (! isExclusive) {
            endLong++;
        }

        if (beginLong < 0) {
            beginLong += limit;
            if (beginLong < 0) {
                if (isStrict) {
                    throw new RangeError(runtime, inspect().toString() + " out of range.");
                }
                return null;
            }
        }

        if (truncate && beginLong > limit) {
            if (isStrict) {
                throw new RangeError(runtime, inspect().toString() + " out of range.");
            }
            return null;
        }

        if (truncate && endLong > limit) {
            endLong = limit;
        }

		if (endLong < 0  || (!isExclusive && endLong == 0)) {
			endLong += limit;
			if (endLong < 0) {
				if (isStrict) {
					throw new RangeError(runtime, inspect().toString() + " out of range.");
				}
				return null;
			}
		}

        if (beginLong > endLong) {
            if (isStrict) {
                throw new RangeError(runtime, inspect().toString() + " out of range.");
            }
			return null;
        }

        return new long[] { beginLong, endLong - beginLong };
    }

    // public Range methods

    public static RubyRange newRange(Ruby ruby, IRubyObject begin, IRubyObject end, boolean isExclusive) {
        RubyRange range = new RubyRange(ruby);
        range.init(begin, end, isExclusive ? ruby.getTrue() : ruby.getFalse());
        return range;
    }

    public IRubyObject initialize(IRubyObject[] args) {
        if (args.length == 3) {
            init(args[0], args[1], (RubyBoolean) args[2]);
        } else if (args.length == 2) {
            init(args[0], args[1], getRuntime().getFalse());
        } else {
            throw new ArgumentError(getRuntime(), "Wrong arguments. (anObject, anObject, aBoolean = false) excepted");
        }
        return getRuntime().getNil();
    }

    public IRubyObject first() {
        return begin;
    }

    public IRubyObject last() {
        return end;
    }

    public RubyString inspect() {
        RubyString begStr = (RubyString) begin.callMethod("to_s");
        RubyString endStr = (RubyString) end.callMethod("to_s");

        begStr.cat(isExclusive ? "..." : "..");
        begStr.concat(endStr);
        return begStr;
    }

    public RubyBoolean exclude_end_p() {
        return RubyBoolean.newBoolean(getRuntime(), isExclusive);
    }

    public RubyFixnum length() {
        long size = 0;

        if (begin.callMethod(">", end).isTrue()) {
            return RubyFixnum.newFixnum(getRuntime(), 0);
        }

        if (begin instanceof RubyFixnum && end instanceof RubyFixnum) {
            size = ((RubyNumeric) end).getLongValue() - ((RubyNumeric) begin).getLongValue();
            if (!isExclusive) {
                size++;
            }
        }
        return RubyFixnum.newFixnum(getRuntime(), size);
    }

    public RubyBoolean equal(IRubyObject obj) {
        if (!(obj instanceof RubyRange)) {
            return getRuntime().getFalse();
        }
        RubyRange otherRange = (RubyRange) obj;
        boolean result =
            begin.equals(otherRange.begin) &&
            end.equals(otherRange.end) &&
            isExclusive == otherRange.isExclusive;
        return RubyBoolean.newBoolean(getRuntime(), result);
    }

    public RubyBoolean op_eqq(IRubyObject obj) {
        if ((begin instanceof RubyFixnum) && (obj instanceof RubyFixnum) && (end instanceof RubyFixnum)) {
            long b = RubyNumeric.fix2long(begin);
            long o = RubyNumeric.fix2long(obj);

            if (b <= o) {
                long e =  RubyNumeric.fix2long(end);
                if (isExclusive) {
                    if (o < e) {
                        return getRuntime().getTrue();
                    }
                } else {
                    if (o <= e) {
                        return getRuntime().getTrue();
                    }
                }
            }
            return getRuntime().getFalse();
        } else if (begin.callMethod("<=", obj).isTrue()) {
            if (isExclusive) {
                if (end.callMethod(">", obj).isTrue()) {
                    return getRuntime().getTrue();
                }
            } else {
                if (end.callMethod(">=", obj).isTrue()) {
                    return getRuntime().getTrue();
                }
            }
        }
        return getRuntime().getFalse();
    }

    public IRubyObject each() {
        if (begin instanceof RubyFixnum && end instanceof RubyFixnum) {
            long endLong = ((RubyNumeric) end).getLongValue();
            long i = ((RubyNumeric) begin).getLongValue();

            if (!isExclusive) {
                endLong += 1;
            }

            for (; i < endLong; i++) {
                getRuntime().yield(RubyFixnum.newFixnum(getRuntime(), i));
            }
        } else if (begin instanceof RubyString) {
            ((RubyString) begin).upto(end, isExclusive);
        } else if (begin.isKindOf(getRuntime().getClasses().getNumericClass())) {
            if (!isExclusive) {
                end = end.callMethod("+", RubyFixnum.one(getRuntime()));
            }
            while (begin.callMethod("<", end).isTrue()) {
                getRuntime().yield(begin);
                begin = begin.callMethod("+", RubyFixnum.one(getRuntime()));
            }
        } else {
            IRubyObject v = begin;

            if (isExclusive) {
                while (v.callMethod("<", end).isTrue()) {
                    if (v.equals(end)) {
                        break;
                    }
                    getRuntime().yield(v);
                    v = v.callMethod("succ");
                }
            } else {
                while (v.callMethod("<=", end).isTrue()) {
                    getRuntime().yield(v);
                    if (v.equals(end)) {
                        break;
                    }
                    v = v.callMethod("succ");
                }
            }
        }

        return this;
    }
}
