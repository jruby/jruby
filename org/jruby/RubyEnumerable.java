/*
 * RubyEnumerable.java - No description
 * Created on 15.01.2002, 18:00:54
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina, Chad Fowler
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Chad Fowler <chadfowler@yahoo.com>
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

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class RubyEnumerable {

    public static RubyModule createEnumerableModule(Ruby ruby) {
        RubyModule enumerableModule = ruby.defineModule("Enumerable");

        enumerableModule.defineMethod("collect", CallbackFactory.getSingletonMethod(RubyEnumerable.class, "collect"));
        enumerableModule.defineMethod("detect", CallbackFactory.getSingletonMethod(RubyEnumerable.class, "find"));
        enumerableModule.defineMethod("each_with_index",
            CallbackFactory.getSingletonMethod(RubyEnumerable.class, "each_with_index"));
        enumerableModule.defineMethod("entries", CallbackFactory.getSingletonMethod(RubyEnumerable.class, "to_a"));
        enumerableModule.defineMethod("find", CallbackFactory.getSingletonMethod(RubyEnumerable.class, "find"));
        enumerableModule.defineMethod("find_all", CallbackFactory.getSingletonMethod(RubyEnumerable.class, "find_all"));
        enumerableModule.defineMethod("grep",
            CallbackFactory.getSingletonMethod(RubyEnumerable.class, "grep", RubyObject.class));
        enumerableModule.defineMethod("include?",
            CallbackFactory.getSingletonMethod(RubyEnumerable.class, "member", RubyObject.class));
        enumerableModule.defineMethod("map", CallbackFactory.getSingletonMethod(RubyEnumerable.class, "collect"));
        enumerableModule.defineMethod("max", CallbackFactory.getSingletonMethod(RubyEnumerable.class, "max"));
        enumerableModule.defineMethod("member?",
            CallbackFactory.getSingletonMethod(RubyEnumerable.class, "member", RubyObject.class));
        enumerableModule.defineMethod("min", CallbackFactory.getSingletonMethod(RubyEnumerable.class, "min"));
        enumerableModule.defineMethod("reject", CallbackFactory.getSingletonMethod(RubyEnumerable.class, "reject"));
        enumerableModule.defineMethod("select", CallbackFactory.getSingletonMethod(RubyEnumerable.class, "find_all"));
        enumerableModule.defineMethod("sort", CallbackFactory.getSingletonMethod(RubyEnumerable.class, "sort"));
        enumerableModule.defineMethod("to_a", CallbackFactory.getSingletonMethod(RubyEnumerable.class, "to_a"));

        return enumerableModule;
    }

    public static RubyObject each(Ruby ruby, RubyObject recv) {
        return recv.callMethod("each");
    }

    private static void iterateEach(Ruby ruby, RubyObject recv, String iter_method, RubyObject arg1) {
        ruby.iterate(CallbackFactory.getSingletonMethod(RubyEnumerable.class, "each"), recv,
            CallbackFactory.getBlockMethod(RubyEnumerable.class, iter_method), arg1);
    }

    // Block methods

    public static RubyObject collect_i(Ruby ruby, RubyObject blockArg, RubyObject arg1, RubyObject self) {
        ((RubyArray) arg1).append(ruby.yield(blockArg).toRubyObject());

        return ruby.getNil();
    }

    public static RubyObject each_with_index_i(Ruby ruby, RubyObject blockArg, RubyObject arg1, RubyObject self) {
        RubyObject index = ((RubyArray) arg1).pop();
        
        ruby.yield(RubyArray.newArray(ruby, blockArg, index));

        ((RubyArray) arg1).append(((RubyFixnum) index).op_plus(RubyFixnum.one(ruby)));

        return ruby.getNil();
    }

    public static RubyObject enum_all_i(Ruby ruby, RubyObject blockArg, RubyObject arg1, RubyObject self) {
        ((RubyArray) arg1).append(blockArg);

        return ruby.getNil();
    }

    public static RubyObject find_i(Ruby ruby, RubyObject blockArg, RubyObject arg1, RubyObject self) {
        if (ruby.yield(blockArg).isTrue()) {
            ((RubyArray) arg1).append(blockArg);
            throw new BreakJump();
        }

        return ruby.getNil();
    }

    public static RubyObject find_all_i(Ruby ruby, RubyObject blockArg, RubyObject arg1, RubyObject self) {
        if (ruby.yield(blockArg).isTrue()) {
            ((RubyArray) arg1).append(blockArg);
        }

        return ruby.getNil();
    }

    public static RubyObject grep_i(Ruby ruby, RubyObject blockArg, RubyObject arg1, RubyObject self) {
        RubyObject matcher = ((RubyArray) arg1).entry(0);
        RubyArray resultArray = (RubyArray) ((RubyArray) arg1).entry(1);

        if (matcher.callMethod("===", blockArg).isTrue()) {
            resultArray.append(blockArg);
        }

        return ruby.getNil();
    }

    public static RubyObject grep_iter_i(Ruby ruby, RubyObject blockArg, RubyObject arg1, RubyObject self) {
        RubyObject matcher = ((RubyArray) arg1).entry(0);
        RubyArray resultArray = (RubyArray) ((RubyArray) arg1).entry(1);

        if (matcher.callMethod("===", blockArg).isTrue()) {
            resultArray.append(ruby.yield(blockArg).toRubyObject());
        }

        return ruby.getNil();
    }

    public static RubyObject max_i(Ruby ruby, RubyObject blockArg, RubyObject arg1, RubyObject self) {
        RubyObject maxItem = ((RubyArray) arg1).pop();

        if (maxItem.isNil()) {
            ((RubyArray) arg1).append(blockArg);
        } else if (RubyFixnum.fix2int(blockArg.callMethod("<=>", maxItem)) > 0) {
            ((RubyArray) arg1).append(blockArg);
        } else {
            ((RubyArray) arg1).append(maxItem);
        }

        return ruby.getNil();
    }

    public static RubyObject max_iter_i(Ruby ruby, RubyObject blockArg, RubyObject arg1, RubyObject self) {
        RubyObject maxItem = ((RubyArray) arg1).pop();

        if (maxItem.isNil()) {
            ((RubyArray) arg1).append(blockArg);
        } else if (RubyFixnum.fix2int(ruby.yield(RubyArray.newArray(ruby, blockArg, maxItem)).toRubyObject()) > 0) {
            ((RubyArray) arg1).append(blockArg);
        } else {
            ((RubyArray) arg1).append(maxItem);
        }

        return ruby.getNil();
    }

    public static RubyObject member_i(Ruby ruby, RubyObject blockArg, RubyObject arg1, RubyObject self) {
        if (blockArg.callMethod("==", ((RubyArray) arg1).entry(0)).isTrue()) {
            ((RubyArray) arg1).append(ruby.getTrue());
            throw new BreakJump();
        }

        return ruby.getNil();
    }

    public static RubyObject min_i(Ruby ruby, RubyObject blockArg, RubyObject arg1, RubyObject self) {
        RubyObject maxItem = ((RubyArray) arg1).pop();
        if (maxItem.isNil()) {
            ((RubyArray) arg1).append(blockArg);
        } else if (RubyFixnum.fix2int(blockArg.callMethod("<=>", maxItem)) < 0) {
            ((RubyArray) arg1).append(blockArg);
        } else {
            ((RubyArray) arg1).append(maxItem);
        }

        return ruby.getNil();
    }

    public static RubyObject min_iter_i(Ruby ruby, RubyObject blockArg, RubyObject arg1, RubyObject self) {
        RubyObject maxItem = ((RubyArray) arg1).pop();
        if (maxItem.isNil()) {
            ((RubyArray) arg1).append(blockArg);
        } else if (RubyFixnum.fix2int(ruby.yield(RubyArray.newArray(ruby, blockArg, maxItem)).toRubyObject()) < 0) {
            ((RubyArray) arg1).append(blockArg);
        } else {
            ((RubyArray) arg1).append(maxItem);
        }

        return ruby.getNil();
    }

    public static RubyObject reject_i(Ruby ruby, RubyObject blockArg, RubyObject arg1, RubyObject self) {
        if (!ruby.yield(blockArg).isTrue()) {
            ((RubyArray) arg1).append(blockArg);
        }

        return ruby.getNil();
    }
    
    /* methods of the Enumerable module. */

    public static RubyObject collect(Ruby ruby, RubyObject recv) {
        RubyArray ary = RubyArray.newArray(ruby);
        iterateEach(ruby, recv, ruby.isBlockGiven() ? "collect_i" : "enum_all_i", ary);
        return ary;
    }

    public static RubyObject each_with_index(Ruby ruby, RubyObject recv) {
        iterateEach(ruby, recv, "each_with_index_i", RubyArray.newArray(ruby, RubyFixnum.zero(ruby)));
        return ruby.getNil();
    }

    public static RubyObject find(Ruby ruby, RubyObject recv) {
        RubyArray ary = RubyArray.newArray(ruby);
        iterateEach(ruby, recv, "find_i", ary);
        if (ary.getLength() > 0) {
            return ary.shift();
        }

        return ruby.getNil();
    }

    public static RubyObject find_all(Ruby ruby, RubyObject recv) {
        RubyArray ary = RubyArray.newArray(ruby);
        iterateEach(ruby, recv, "find_all_i", ary);
        return ary;
    }

    public static RubyObject grep(Ruby ruby, RubyObject recv, RubyObject matcher) {
        RubyArray ary = RubyArray.newArray(ruby);
        ary.append(matcher);
        ary.append(RubyArray.newArray(ruby));
        if (ruby.isBlockGiven()) {
            iterateEach(ruby, recv, "grep_iter_i", ary);
        } else {
            iterateEach(ruby, recv, "grep_i", ary);
        }

        return ary.pop();
    }

    public static RubyObject max(Ruby ruby, RubyObject recv) {
        RubyArray ary = RubyArray.newArray(ruby);
        ary.append(ruby.getNil());
        if (ruby.isBlockGiven()) {
            iterateEach(ruby, recv, "max_iter_i", ary);
        } else {
            iterateEach(ruby, recv, "max_i", ary);
        }

        return ary.pop();
    }

    public static RubyBoolean member(Ruby ruby, RubyObject recv, RubyObject item) {
        RubyArray ary = RubyArray.newArray(ruby);
        ary.append(item);
        iterateEach(ruby, recv, "member_i", ary);
        return ary.getLength() > 1 ? ruby.getTrue() : ruby.getFalse();
    }

    public static RubyObject min(Ruby ruby, RubyObject recv) {
        RubyArray ary = RubyArray.newArray(ruby);
        ary.append(ruby.getNil());
        if (ruby.isBlockGiven()) {
            iterateEach(ruby, recv, "min_iter_i", ary);
        } else {
            iterateEach(ruby, recv, "min_i", ary);
        }

        return ary.pop();
    }

    public static RubyObject reject(Ruby ruby, RubyObject recv) {
        RubyArray ary = RubyArray.newArray(ruby);
        iterateEach(ruby, recv, "reject_i", ary);
        return ary;
    }

    public static RubyObject sort(Ruby ruby, RubyObject recv) {
        RubyArray ary = (RubyArray) to_a(ruby, recv);
        ary.sort_bang();
        return ary;
    }

    public static RubyObject to_a(Ruby ruby, RubyObject recv) {
        RubyArray ary = RubyArray.newArray(ruby);
        iterateEach(ruby, recv, "enum_all_i", ary);
        return ary;
    }
}
