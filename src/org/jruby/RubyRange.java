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
 * Copyright (C) 2001 Chad Fowler <chadfowler@chadfowler.com>
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001 Ed Sinjiashvili <slorcim@users.sourceforge.net>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2006 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
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

import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author jpetersen
 */
public class RubyRange extends RubyObject {

    private IRubyObject begin;
    private IRubyObject end;
    private boolean isExclusive;

    public RubyRange(IRuby runtime) {
        super(runtime, runtime.getClass("Range"));
    }

    public void init(IRubyObject aBegin, IRubyObject aEnd, RubyBoolean aIsExclusive) {
        if (!(aBegin instanceof RubyFixnum && aEnd instanceof RubyFixnum)) {
            try {
                aBegin.callMethod(getRuntime().getCurrentContext(), "<=>", aEnd);
            } catch (RaiseException rExcptn) {
                throw getRuntime().newArgumentError("bad value for range");
            }
        }

        this.begin = aBegin;
        this.end = aEnd;
        this.isExclusive = aIsExclusive.isTrue();
    }

    public static RubyClass createRangeClass(IRuby runtime) {
        RubyClass result = runtime.defineClass("Range", runtime.getObject());
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyRange.class);
        
        result.includeModule(runtime.getModule("Enumerable"));

        result.defineMethod("==", callbackFactory.getMethod("equal", IRubyObject.class));
        result.defineMethod("begin", callbackFactory.getMethod("first"));
        result.defineMethod("each", callbackFactory.getMethod("each"));
        result.defineMethod("end", callbackFactory.getMethod("last"));
        result.defineMethod("exclude_end?", callbackFactory.getMethod("exclude_end_p"));
        result.defineMethod("first", callbackFactory.getMethod("first"));
        result.defineMethod("hash", callbackFactory.getMethod("hash"));
        result.defineMethod("initialize", callbackFactory.getOptMethod("initialize"));
        result.defineMethod("inspect", callbackFactory.getMethod("inspect"));
        result.defineMethod("last", callbackFactory.getMethod("last"));
        result.defineMethod("length", callbackFactory.getMethod("length"));
        result.defineMethod("size", callbackFactory.getMethod("length"));
        result.defineMethod("step", callbackFactory.getOptMethod("step"));
        result.defineMethod("to_s", callbackFactory.getMethod("to_s"));

        result.defineMethod("to_a", callbackFactory.getMethod("to_a"));
        result.defineMethod("include?", callbackFactory.getMethod("include_p", IRubyObject.class));
		// We override Enumerable#member? since ranges in 1.8.1 are continuous.
		result.defineAlias("member?", "include?");
        result.defineAlias("===", "include?");
		
		result.defineSingletonMethod("new", callbackFactory.getOptSingletonMethod("newInstance"));
        
        return result;
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
     * @param isStrict   if true, raises an exception if the range can't be converted.
     * @return         a two-element array representing a start value and a length, 
     *                 or <b>null</b> if the conversion failed.
     */
    public long[] getBeginLength(long limit, boolean truncate, boolean isStrict) {
        long beginLong = RubyNumeric.num2long(begin);
        long endLong = RubyNumeric.num2long(end);
        
        // Apparent legend for MRI 'err' param to JRuby 'truncate' and 'isStrict':
        // 0 =>  truncate && !strict
        // 1 => !truncate &&  strict
        // 2 =>  truncate &&  strict

        if (! isExclusive) {
            endLong++;
        }

        if (beginLong < 0) {
            beginLong += limit;
            if (beginLong < 0) {
                if (isStrict) {
                    throw getRuntime().newRangeError(inspect().toString() + " out of range.");
                }
                return null;
            }
        }

        if (truncate && beginLong > limit) {
            if (isStrict) {
                throw getRuntime().newRangeError(inspect().toString() + " out of range.");
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
					throw getRuntime().newRangeError(inspect().toString() + " out of range.");
				}
				return null;
			}
		}

        return new long[] { beginLong, Math.max(endLong - beginLong, 0L) };
    }

    // public Range methods
    public static RubyRange newInstance(IRubyObject recv, IRubyObject[] args) {
    	RubyRange range = new RubyRange(recv.getRuntime());
    	
    	range.initialize(args);
    	
    	return range;
    }

    public static RubyRange newRange(IRuby runtime, IRubyObject begin, IRubyObject end, boolean isExclusive) {
        RubyRange range = new RubyRange(runtime);
        range.init(begin, end, isExclusive ? runtime.getTrue() : runtime.getFalse());
        return range;
    }

    public IRubyObject initialize(IRubyObject[] args) {
        if (args.length == 3) {
            init(args[0], args[1], (RubyBoolean) args[2]);
        } else if (args.length == 2) {
            init(args[0], args[1], getRuntime().getFalse());
        } else {
            throw getRuntime().newArgumentError("Wrong arguments. (anObject, anObject, aBoolean = false) expected");
        }
        return getRuntime().getNil();
    }

    public IRubyObject first() {
        return begin;
    }

    public IRubyObject last() {
        return end;
    }
    
    public RubyFixnum hash() {
        ThreadContext context = getRuntime().getCurrentContext();
        long baseHash = (isExclusive ? 1 : 0);
        long beginHash = ((RubyFixnum) begin.callMethod(context, "hash")).getLongValue();
        long endHash = ((RubyFixnum) end.callMethod(context, "hash")).getLongValue();
        
        long hash = baseHash;
        hash = hash ^ (beginHash << 1);
        hash = hash ^ (endHash << 9);
        hash = hash ^ (baseHash << 24);
        
        return getRuntime().newFixnum(hash);
    }

    private IRubyObject asString(String stringMethod) {
        ThreadContext context = getRuntime().getCurrentContext();
        RubyString begStr = (RubyString) begin.callMethod(context, stringMethod);
        RubyString endStr = (RubyString) end.callMethod(context, stringMethod);

        return begStr.cat(isExclusive ? "..." : "..").concat(endStr);
    }
    
    public IRubyObject inspect() {
        return asString("inspect");
    }
    
    public IRubyObject to_s() {
        return asString("to_s");
    }

    public RubyBoolean exclude_end_p() {
        return getRuntime().newBoolean(isExclusive);
    }

    public RubyFixnum length() {
        long size = 0;
        ThreadContext context = getRuntime().getCurrentContext();

        if (begin.callMethod(context, ">", end).isTrue()) {
            return getRuntime().newFixnum(0);
        }

        if (begin instanceof RubyFixnum && end instanceof RubyFixnum) {
            size = ((RubyNumeric) end).getLongValue() - ((RubyNumeric) begin).getLongValue();
            if (!isExclusive) {
                size++;
            }
        } else { // Support length for arbitrary classes
            IRubyObject currentObject = begin;
	    String compareMethod = isExclusive ? "<" : "<=";

	    while (currentObject.callMethod(context, compareMethod, end).isTrue()) {
		size++;
		if (currentObject.equals(end)) {
		    break;
		}
		currentObject = currentObject.callMethod(context, "succ");
	    }
	}
        return getRuntime().newFixnum(size);
    }

    public IRubyObject equal(IRubyObject obj) {
        if (!(obj instanceof RubyRange)) {
            return getRuntime().getFalse();
        }
        RubyRange otherRange = (RubyRange) obj;
        boolean result =
            begin.equals(otherRange.begin) &&
            end.equals(otherRange.end) &&
            isExclusive == otherRange.isExclusive;
        return getRuntime().newBoolean(result);
    }

    public IRubyObject each() {
        ThreadContext context = getRuntime().getCurrentContext();
        
        if (begin instanceof RubyFixnum && end instanceof RubyFixnum) {
            long endLong = ((RubyNumeric) end).getLongValue();
            long i = ((RubyNumeric) begin).getLongValue();

            if (!isExclusive) {
                endLong += 1;
            }

            for (; i < endLong; i++) {
                context.yield(getRuntime().newFixnum(i));
            }
        } else if (begin instanceof RubyString) {
            ((RubyString) begin).upto(end, isExclusive);
        } else if (begin.isKindOf(getRuntime().getClass("Numeric"))) {
            if (!isExclusive) {
                end = end.callMethod(context, "+", RubyFixnum.one(getRuntime()));
            }
            while (begin.callMethod(context, "<", end).isTrue()) {
                context.yield(begin);
                begin = begin.callMethod(context, "+", RubyFixnum.one(getRuntime()));
            }
        } else {
            IRubyObject v = begin;

            if (isExclusive) {
                while (v.callMethod(context, "<", end).isTrue()) {
                    if (v.equals(end)) {
                        break;
                    }
                    context.yield(v);
                    v = v.callMethod(context, "succ");
                }
            } else {
                while (v.callMethod(context, "<=", end).isTrue()) {
                    context.yield(v);
                    if (v.equals(end)) {
                        break;
                    }
                    v = v.callMethod(context, "succ");
                }
            }
        }

        return this;
    }
    
    public IRubyObject step(IRubyObject[] args) {
        checkArgumentCount(args, 0, 1);
        
        IRubyObject currentObject = begin;
        String compareMethod = isExclusive ? "<" : "<=";
        int stepSize = (int) (args.length == 0 ? 1 : args[0].convertToInteger().getLongValue());
        
        if (stepSize <= 0) {
            throw getRuntime().newArgumentError("step can't be negative");
        }

        ThreadContext context = getRuntime().getCurrentContext();
        if (begin instanceof RubyNumeric && end instanceof RubyNumeric) {
            RubyFixnum stepNum = getRuntime().newFixnum(stepSize);
            while (currentObject.callMethod(context, compareMethod, end).isTrue()) {
                context.yield(currentObject);
                currentObject = currentObject.callMethod(context, "+", stepNum);
            }
        } else {
            while (currentObject.callMethod(context, compareMethod, end).isTrue()) {
                context.yield(currentObject);
                
                for (int i = 0; i < stepSize; i++) {
                    currentObject = currentObject.callMethod(context, "succ");
                }
            }
        }
        
        return this;
    }
    
    public RubyArray to_a() {
        IRubyObject currentObject = begin;
	    String compareMethod = isExclusive ? "<" : "<=";
	    RubyArray array = getRuntime().newArray();
        ThreadContext context = getRuntime().getCurrentContext();
        
	    while (currentObject.callMethod(context, compareMethod, end).isTrue()) {
	        array.append(currentObject);
	        
			if (currentObject.equals(end)) {
			    break;
			}
			
			currentObject = currentObject.callMethod(context, "succ");
	    }
	    
	    return array;
    }
    
    // this could have been easily written in Ruby --sma
    public RubyBoolean include_p(IRubyObject obj) {
    	String compareMethod = isExclusive ? ">" : ">=";
    	IRubyObject[] arg = new IRubyObject[]{ obj };
    	IRubyObject f = obj.getRuntime().getFalse();
        ThreadContext context = getRuntime().getCurrentContext();
        
    	return f.getRuntime().newBoolean(
    			f != first().callMethod(context, "<=", arg) && f != last().callMethod(context, compareMethod, arg));
    }
}
