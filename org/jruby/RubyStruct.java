/*
 * RubyStruct.java - No description
 * Created on 18.01.2002, 01:25:39
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

import java.util.*;

import org.jruby.exceptions.*;
import org.jruby.runtime.*;
import org.jruby.util.*;

/**
 * @version $Revision$
 * @author  jpetersen
 */
public class RubyStruct extends RubyObject {
    private RubyObject[] values;

    /**
     * Constructor for RubyStruct.
     * @param ruby
     * @param rubyClass
     */
    public RubyStruct(Ruby ruby, RubyClass rubyClass) {
        super(ruby, rubyClass);
    }

    public static RubyClass createStructClass(Ruby ruby) {
        RubyClass structClass = ruby.defineClass("Struct", ruby.getClasses().getObjectClass());
        structClass.includeModule(ruby.getClasses().getEnumerableModule());

        structClass.defineSingletonMethod("new", CallbackFactory.getOptSingletonMethod(RubyStruct.class, "newInstance"));

        structClass.defineMethod("initialize", CallbackFactory.getOptMethod(RubyStruct.class, "initialize"));
        structClass.defineMethod("clone", CallbackFactory.getMethod(RubyStruct.class, "rbClone"));

        structClass.defineMethod("==", CallbackFactory.getMethod(RubyStruct.class, "equal", RubyObject.class));

        structClass.defineMethod("to_s", CallbackFactory.getMethod(RubyStruct.class, "to_s"));
        structClass.defineMethod("inspect", CallbackFactory.getMethod(RubyStruct.class, "inspect"));
        structClass.defineMethod("to_a", CallbackFactory.getMethod(RubyStruct.class, "to_a"));
        structClass.defineMethod("values", CallbackFactory.getMethod(RubyStruct.class, "to_a"));
        structClass.defineMethod("size", CallbackFactory.getMethod(RubyStruct.class, "size"));
        structClass.defineMethod("length", CallbackFactory.getMethod(RubyStruct.class, "size"));

        structClass.defineMethod("each", CallbackFactory.getMethod(RubyStruct.class, "each"));
        structClass.defineMethod("[]", CallbackFactory.getMethod(RubyStruct.class, "aref", RubyObject.class));
        structClass.defineMethod("[]=", CallbackFactory.getMethod(RubyStruct.class, "aset", RubyObject.class, RubyObject.class));

        structClass.defineMethod("members", CallbackFactory.getMethod(RubyStruct.class, "members"));

        return structClass;
    }

    private static RubyObject getInstanceVariable(RubyClass type, String name) {
        RubyClass structClass = type.getRuby().getClasses().getStructClass();

        while (type != null && type != structClass) {
            if (type.isInstanceVarDefined(name)) {
                return type.getInstanceVar(name);
            }

            type = type.getSuperClass();
        }

        return type.getRuby().getNil();
    }

    private RubyClass classOf() {
        return getRubyClass().isSingleton() ? getRubyClass().getSuperClass() : getRubyClass();
    }

    private void modify() {
        if (isFrozen()) {
            throw new RubyFrozenException(ruby, "Struct is frozen.");
        }

        if (!isTaint() && ruby.getSafeLevel() >= 4) {
            throw new RubySecurityException(ruby, "Insecure: can't modify struct");
        }
    }

    private RubyObject setByName(String name, RubyObject value) {
        RubyArray member = (RubyArray) getInstanceVariable(classOf(), "__member__");

        if (member.isNil()) {
            throw new RubyBugException("uninitialized struct");
        }

        modify();

        for (int i = 0; i < member.getLength(); i++) {
            if (member.entry(i).toId().equals(name)) {
                return values[i] = value;
            }
        }

        throw new NameError(ruby, name + " is not struct member");
    }

    private RubyObject getByName(String name) {
        RubyArray member = (RubyArray) getInstanceVariable(classOf(), "__member__");

        if (member.isNil()) {
            throw new RubyBugException("uninitialized struct");
        }

        for (int i = 0; i < member.getLength(); i++) {
            if (member.entry(i).toId().equals(name)) {
                return values[i];
            }
        }

        throw new NameError(ruby, name + " is not struct member");
    }

    // Struct methods

    /** Create new Struct class.
     * 
     * MRI: rb_struct_s_def / make_struct
     * 
     */
    public static RubyClass newInstance(Ruby ruby, RubyObject recv, RubyObject[] args) {
        String name = null;

        if (args.length > 0 && args[0] instanceof RubyString) {
            name = args[0].toString();
        }

        RubyArray member = RubyArray.newArray(ruby);

        for (int i = name == null ? 0 : 1; i < args.length; i++) {
            member.push(RubySymbol.newSymbol(ruby, args[i].toId()));
        }

        RubyClass newStruct;

        if (name == null) {
            newStruct = RubyClass.newClass(ruby, (RubyClass) recv);
        } else {
            if (!IdUtil.isConstant(name)) {
                throw new NameError(ruby, "identifier " + name + " needs to be constant");
            }
            newStruct = ((RubyClass) recv).defineClassUnder(name, ((RubyClass) recv));
        }

        newStruct.setInstanceVar("__size__", member.length());
        newStruct.setInstanceVar("__member__", member);

        newStruct.defineSingletonMethod("new", CallbackFactory.getOptSingletonMethod(RubyStruct.class, "newStruct"));
        newStruct.defineSingletonMethod("[]", CallbackFactory.getOptSingletonMethod(RubyStruct.class, "newStruct"));
        newStruct.defineSingletonMethod("members", CallbackFactory.getSingletonMethod(RubyStruct.class, "members"));

        // define access methods.
        for (int i = name == null ? 0 : 1; i < args.length; i++) {
            newStruct.defineMethod(args[i].toId(), CallbackFactory.getMethod(RubyStruct.class, "get"));
            newStruct.defineMethod(args[i].toId() + "=", CallbackFactory.getMethod(RubyStruct.class, "set", RubyObject.class));
        }

        return newStruct;
    }

    /** Create new Structure.
     * 
     * MRI: struct_alloc
     * 
     */
    public static RubyStruct newStruct(Ruby ruby, RubyObject recv, RubyObject[] args) {
        RubyStruct struct = new RubyStruct(ruby, (RubyClass) recv);

        int size = RubyFixnum.fix2int(getInstanceVariable((RubyClass) recv, "__size__"));

        struct.values = new RubyObject[size];

        struct.callInit(args);

        return struct;
    }

    public RubyObject initialize(RubyObject[] args) {
        modify();

        int size = RubyFixnum.fix2int(getInstanceVariable(getRubyClass(), "__size__"));

        if (args.length > size) {
            throw new ArgumentError(ruby, "struct size differs");
        }

        for (int i = 0; i < args.length; i++) {
            values[i] = args[i];
        }

        for (int i = args.length; i < size; i++) {
            values[i] = ruby.getNil();
        }

        return ruby.getNil();
    }

    public static RubyArray members(Ruby ruby, RubyObject recv) {
        RubyArray member = (RubyArray) getInstanceVariable((RubyClass) recv, "__member__");

        if (member.isNil()) {
            throw new RubyBugException("uninitialized struct");
        }

        RubyArray result = RubyArray.newArray(ruby, member.getLength());
        for (int i = 0; i < member.getLength(); i++) {
            result.push(RubyString.newString(ruby, member.entry(i).toId()));
        }

        return result;
    }

    public RubyArray members() {
        return members(ruby, classOf());
    }

    public RubyObject set(RubyObject value) {
        String name = ruby.getActFrame().getLastFunc();
        if (name.endsWith("=")) {
            name = name.substring(0, name.length() - 1);
        }

        RubyArray member = (RubyArray) getInstanceVariable(classOf(), "__member__");

        if (member.isNil()) {
            throw new RubyBugException("uninitialized struct");
        }

        modify();

        for (int i = 0; i < member.getLength(); i++) {
            if (member.entry(i).toId().equals(name)) {
                return values[i] = value;
            }
        }

        throw new NameError(ruby, name + " is not struct member");
    }

    public RubyObject get() {
        String name = ruby.getActFrame().getLastFunc();

        RubyArray member = (RubyArray) getInstanceVariable(classOf(), "__member__");

        if (member.isNil()) {
            throw new RubyBugException("uninitialized struct");
        }

        for (int i = 0; i < member.getLength(); i++) {
            if (member.entry(i).toId().equals(name)) {
                return values[i];
            }
        }

        throw new NameError(ruby, name + " is not struct member");
    }

    public RubyObject rbClone() {
        RubyStruct clone = new RubyStruct(ruby, getRubyClass());

        clone.values = new RubyObject[values.length];
        System.arraycopy(values, 0, clone.values, 0, values.length);

        return clone;
    }

    public RubyBoolean equal(RubyObject other) {
        if (this == other) {
            return ruby.getTrue();
        } else if (!(other instanceof RubyStruct)) {
            return ruby.getFalse();
        } else if (getRubyClass() != other.getRubyClass()) {
            return ruby.getFalse();
        } else {
            for (int i = 0; i < values.length; i++) {
                if (!values[i].equals(((RubyStruct) other).values[i])) {
                    return ruby.getFalse();
                }
            }
            return ruby.getTrue();
        }
    }

    public RubyString to_s() {
        return RubyString.newString(ruby, "#<" + getRubyClass().toName() + ">");
    }

    public RubyString inspect() {
        RubyArray member = (RubyArray) getInstanceVariable(classOf(), "__member__");
        if (member.isNil()) {
            throw new RubyBugException("uninitialized struct");
        }

        StringBuffer sb = new StringBuffer(100);

        sb.append("#<").append(getRubyClass().toName()).append(' ');

        for (int i = 0; i < member.getLength(); i++) {
            if (i > 0) {
                sb.append(", ");
            }

            sb.append(member.entry(i).toId()).append("=");
            sb.append(values[i].funcall("inspect"));
        }

        sb.append('>');

        return RubyString.newString(ruby, sb.toString()); // OBJ_INFECT
    }

    public RubyArray to_a() {
        return RubyArray.newArray(ruby, Arrays.asList(values));
    }

    public RubyFixnum size() {
        return RubyFixnum.newFixnum(ruby, values.length);
    }

    public RubyObject each() {
        for (int i = 0; i < values.length; i++) {
            ruby.yield(values[i]);
        }

        return this;
    }

    public RubyObject aref(RubyObject key) {
        if (key instanceof RubyString || key instanceof RubySymbol) {
            return getByName(key.toId());
        }

        int idx = RubyFixnum.fix2int(key);

        idx = idx < 0 ? values.length + idx : idx;

        if (idx < 0) {
            throw new RubyIndexException(ruby, "offset " + idx + " too large for struct (size:" + values.length + ")");
        } else if (idx >= values.length) {
            throw new RubyIndexException(ruby, "offset " + idx + " too large for struct (size:" + values.length + ")");
        }

        return values[idx];
    }

    public RubyObject aset(RubyObject key, RubyObject value) {
        if (key instanceof RubyString || key instanceof RubySymbol) {
            return setByName(key.toId(), value);
        }

        int idx = RubyFixnum.fix2int(key);

        idx = idx < 0 ? values.length + idx : idx;

        if (idx < 0) {
            throw new RubyIndexException(ruby, "offset " + idx + " too large for struct (size:" + values.length + ")");
        } else if (idx >= values.length) {
            throw new RubyIndexException(ruby, "offset " + idx + " too large for struct (size:" + values.length + ")");
        }

        modify();
        return values[idx] = value;
    }
}
