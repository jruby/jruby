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

import org.jruby.exceptions.BreakJump;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.internal.runtime.methods.AbstractMethod;

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
            CallbackFactory.getSingletonMethod(RubyEnumerable.class, "grep", IRubyObject.class));
        enumerableModule.defineMethod("include?",
            CallbackFactory.getSingletonMethod(RubyEnumerable.class, "member", IRubyObject.class));
        enumerableModule.defineMethod("map", CallbackFactory.getSingletonMethod(RubyEnumerable.class, "collect"));
        enumerableModule.defineMethod("max", CallbackFactory.getSingletonMethod(RubyEnumerable.class, "max"));
        enumerableModule.defineMethod("member?",
            CallbackFactory.getSingletonMethod(RubyEnumerable.class, "member", IRubyObject.class));
        enumerableModule.defineMethod("min", CallbackFactory.getSingletonMethod(RubyEnumerable.class, "min"));
        enumerableModule.defineMethod("reject", CallbackFactory.getSingletonMethod(RubyEnumerable.class, "reject"));
        enumerableModule.defineMethod("select", CallbackFactory.getSingletonMethod(RubyEnumerable.class, "find_all"));
        enumerableModule.defineMethod("sort", CallbackFactory.getSingletonMethod(RubyEnumerable.class, "sort"));
        enumerableModule.defineMethod("to_a", CallbackFactory.getSingletonMethod(RubyEnumerable.class, "to_a"));

        return enumerableModule;
    }

    public static IRubyObject each(IRubyObject recv) {
        return recv.callMethod("each");
    }

    private static void iterateEach(IRubyObject recv, String iter_method, IRubyObject arg1) {
        recv.getRuntime().iterate(CallbackFactory.getSingletonMethod(RubyEnumerable.class, "each"), recv,
            CallbackFactory.getBlockMethod(RubyEnumerable.class, iter_method), arg1);
    }

    // Block methods

    public static IRubyObject collect_i(IRubyObject blockArg, IRubyObject arg1, IRubyObject self) {
        ((RubyArray) arg1).append(self.getRuntime().yield(blockArg));

        return self.getRuntime().getNil();
    }

    public static IRubyObject each_with_index_i(IRubyObject blockArg, IRubyObject arg1, IRubyObject self) {
        IRubyObject index = ((RubyArray) arg1).pop();

        self.getRuntime().yield(RubyArray.newArray(self.getRuntime(), blockArg, index));

        ((RubyArray) arg1).append(((RubyFixnum) index).op_plus(RubyFixnum.one(self.getRuntime())));

        return self.getRuntime().getNil();
    }

    public static IRubyObject enum_all_i(IRubyObject blockArg, IRubyObject arg1, IRubyObject self) {
        ((RubyArray) arg1).append(blockArg);

        return self.getRuntime().getNil();
    }

    public static IRubyObject find_i(IRubyObject blockArg, IRubyObject arg1, IRubyObject self) {
        if (self.getRuntime().yield(blockArg).isTrue()) {
            ((RubyArray) arg1).append(blockArg);
            throw new BreakJump();
        }

        return self.getRuntime().getNil();
    }

    public static IRubyObject find_all_i(IRubyObject blockArg, IRubyObject arg1, IRubyObject self) {
        if (self.getRuntime().yield(blockArg).isTrue()) {
            ((RubyArray) arg1).append(blockArg);
        }

        return self.getRuntime().getNil();
    }

    public static IRubyObject grep_i(IRubyObject blockArg, IRubyObject arg1, IRubyObject self) {
        IRubyObject matcher = ((RubyArray) arg1).entry(0);
        RubyArray resultArray = (RubyArray) ((RubyArray) arg1).entry(1);

        if (matcher.callMethod("===", blockArg).isTrue()) {
            resultArray.append(blockArg);
        }

        return self.getRuntime().getNil();
    }

    public static IRubyObject grep_iter_i(IRubyObject blockArg, IRubyObject arg1, IRubyObject self) {
        IRubyObject matcher = ((RubyArray) arg1).entry(0);
        RubyArray resultArray = (RubyArray) ((RubyArray) arg1).entry(1);

        if (matcher.callMethod("===", blockArg).isTrue()) {
            resultArray.append(self.getRuntime().yield(blockArg));
        }

        return self.getRuntime().getNil();
    }

    public static IRubyObject max_i(IRubyObject blockArg, IRubyObject arg1, IRubyObject self) {
        IRubyObject maxItem = ((RubyArray) arg1).pop();

        if (maxItem.isNil()) {
            ((RubyArray) arg1).append(blockArg);
        } else if (RubyFixnum.fix2int(blockArg.callMethod("<=>", maxItem)) > 0) {
            ((RubyArray) arg1).append(blockArg);
        } else {
            ((RubyArray) arg1).append(maxItem);
        }

        return self.getRuntime().getNil();
    }

    public static IRubyObject max_iter_i(IRubyObject blockArg, IRubyObject arg1, IRubyObject self) {
        IRubyObject maxItem = ((RubyArray) arg1).pop();

        if (maxItem.isNil()) {
            ((RubyArray) arg1).append(blockArg);
        } else if (RubyFixnum.fix2int(self.getRuntime().yield(RubyArray.newArray(self.getRuntime(), blockArg, maxItem))) > 0) {
            ((RubyArray) arg1).append(blockArg);
        } else {
            ((RubyArray) arg1).append(maxItem);
        }

        return self.getRuntime().getNil();
    }

    public static IRubyObject member_i(IRubyObject blockArg, IRubyObject arg1, IRubyObject self) {
        if (blockArg.callMethod("==", ((RubyArray) arg1).entry(0)).isTrue()) {
            ((RubyArray) arg1).append(self.getRuntime().getTrue());
            throw new BreakJump();
        }

        return self.getRuntime().getNil();
    }

    public static IRubyObject min_i(IRubyObject blockArg, IRubyObject arg1, IRubyObject self) {
        IRubyObject maxItem = ((RubyArray) arg1).pop();
        if (maxItem.isNil()) {
            ((RubyArray) arg1).append(blockArg);
        } else if (RubyFixnum.fix2int(blockArg.callMethod("<=>", maxItem)) < 0) {
            ((RubyArray) arg1).append(blockArg);
        } else {
            ((RubyArray) arg1).append(maxItem);
        }

        return self.getRuntime().getNil();
    }

    public static IRubyObject min_iter_i(IRubyObject blockArg, IRubyObject arg1, IRubyObject self) {
        IRubyObject maxItem = ((RubyArray) arg1).pop();
        if (maxItem.isNil()) {
            ((RubyArray) arg1).append(blockArg);
        } else if (RubyFixnum.fix2int(self.getRuntime().yield(RubyArray.newArray(self.getRuntime(), blockArg, maxItem))) < 0) {
            ((RubyArray) arg1).append(blockArg);
        } else {
            ((RubyArray) arg1).append(maxItem);
        }

        return self.getRuntime().getNil();
    }

    public static IRubyObject reject_i(IRubyObject blockArg, IRubyObject arg1, IRubyObject self) {
        if (!self.getRuntime().yield(blockArg).isTrue()) {
            ((RubyArray) arg1).append(blockArg);
        }

        return self.getRuntime().getNil();
    }

    /* methods of the Enumerable module. */

    public static IRubyObject collect(IRubyObject recv) {
        RubyArray result = RubyArray.newArray(recv.getRuntime());
        iterateEach(recv, recv.getRuntime().isBlockGiven() ? "collect_i" : "enum_all_i", result);
        return result;
    }

    public static IRubyObject each_with_index(IRubyObject recv) {
        iterateEach(recv, "each_with_index_i", RubyArray.newArray(recv.getRuntime(), RubyFixnum.zero(recv.getRuntime())));
        return recv.getRuntime().getNil();
    }

    public static IRubyObject find(IRubyObject recv) {
        RubyArray ary = RubyArray.newArray(recv.getRuntime());
        iterateEach(recv, "find_i", ary);
        if (ary.getLength() > 0) {
            return ary.shift();
        }

        return recv.getRuntime().getNil();
    }

    public static IRubyObject find_all(IRubyObject recv) {
        RubyArray result = RubyArray.newArray(recv.getRuntime());
        iterateEach(recv, "find_all_i", result);
        return result;
    }

    public static IRubyObject grep(IRubyObject recv, IRubyObject matcher) {
        RubyArray result = RubyArray.newArray(recv.getRuntime());
        result.append(matcher);
        result.append(RubyArray.newArray(recv.getRuntime()));
        if (recv.getRuntime().isBlockGiven()) {
            iterateEach(recv, "grep_iter_i", result);
        } else {
            iterateEach(recv, "grep_i", result);
        }

        return result.pop();
    }

    public static IRubyObject max(IRubyObject recv) {
        RubyArray ary = RubyArray.newArray(recv.getRuntime());
        ary.append(recv.getRuntime().getNil());
        if (recv.getRuntime().isBlockGiven()) {
            iterateEach(recv, "max_iter_i", ary);
        } else {
            iterateEach(recv, "max_i", ary);
        }

        return ary.pop();
    }

    public static RubyBoolean member(IRubyObject recv, IRubyObject item) {
        RubyArray ary = RubyArray.newArray(recv.getRuntime());
        ary.append(item);
        iterateEach(recv, "member_i", ary);
        return ary.getLength() > 1 ? recv.getRuntime().getTrue() : recv.getRuntime().getFalse();
    }

    public static IRubyObject min(IRubyObject recv) {
        RubyArray ary = RubyArray.newArray(recv.getRuntime());
        ary.append(recv.getRuntime().getNil());
        if (recv.getRuntime().isBlockGiven()) {
            iterateEach(recv, "min_iter_i", ary);
        } else {
            iterateEach(recv, "min_i", ary);
        }

        return ary.pop();
    }

    public static IRubyObject reject(IRubyObject recv) {
        RubyArray result = RubyArray.newArray(recv.getRuntime());
        iterateEach(recv, "reject_i", result);
        return result;
    }

    public static IRubyObject sort(IRubyObject recv) {
        RubyArray result = (RubyArray) to_a(recv);
        result.sort_bang();
        return result;
    }

    public static IRubyObject to_a(IRubyObject recv) {
        RubyArray ary = RubyArray.newArray(recv.getRuntime());
        iterateEach(recv, "enum_all_i", ary);
        return ary;
    }
}
