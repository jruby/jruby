/*
 * RubyStruct.java - No description
 * Created on 18.01.2002, 01:25:39
 *
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina, Chad Fowler
 * Copyright (C) 2004 Thomas E Enebo
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Chad Fowler <chadfowler@yahoo.com>
 * Thomas E Enebo <enebo@acm.org>
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

import java.util.List;

import org.jruby.exceptions.ArgumentError;
import org.jruby.exceptions.NameError;
import org.jruby.exceptions.FrozenError;
import org.jruby.exceptions.IndexError;
import org.jruby.exceptions.SecurityError;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.Asserts;
import org.jruby.util.IdUtil;

/**
 * @version $Revision$
 * @author  jpetersen
 */
public class RubyStruct extends RubyObject {
    private IRubyObject[] values;

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
        CallbackFactory callbackFactory = ruby.callbackFactory();
        structClass.includeModule(ruby.getModule("Enumerable"));

        structClass.defineSingletonMethod("new", callbackFactory.getOptSingletonMethod(RubyStruct.class, "newInstance"));

        structClass.defineMethod("initialize", callbackFactory.getOptMethod(RubyStruct.class, "initialize"));
        structClass.defineMethod("clone", callbackFactory.getMethod(RubyStruct.class, "rbClone"));

        structClass.defineMethod("==", callbackFactory.getMethod(RubyStruct.class, "equal", IRubyObject.class));

        structClass.defineMethod("to_s", callbackFactory.getMethod(RubyStruct.class, "to_s"));
        structClass.defineMethod("inspect", callbackFactory.getMethod(RubyStruct.class, "inspect"));
        structClass.defineMethod("to_a", callbackFactory.getMethod(RubyStruct.class, "to_a"));
        structClass.defineMethod("values", callbackFactory.getMethod(RubyStruct.class, "to_a"));
        structClass.defineMethod("size", callbackFactory.getMethod(RubyStruct.class, "size"));
        structClass.defineMethod("length", callbackFactory.getMethod(RubyStruct.class, "size"));

        structClass.defineMethod("each", callbackFactory.getMethod(RubyStruct.class, "each"));
        structClass.defineMethod("[]", callbackFactory.getMethod(RubyStruct.class, "aref", IRubyObject.class));
        structClass.defineMethod("[]=", callbackFactory.getMethod(RubyStruct.class, "aset", IRubyObject.class, IRubyObject.class));

        structClass.defineMethod("members", callbackFactory.getMethod(RubyStruct.class, "members"));

        return structClass;
    }

    private static IRubyObject getInstanceVariable(RubyClass type, String name) {
        RubyClass structClass = type.getRuntime().getClasses().getStructClass();

        while (type != null && type != structClass) {
            if (type.hasInstanceVariable(name)) {
                return type.getInstanceVariable(name);
            }

            type = type.getSuperClass();
        }

        return type.getRuntime().getNil();
    }

    private RubyClass classOf() {
        return getMetaClass() instanceof MetaClass ? getMetaClass().getSuperClass() : getMetaClass();
    }

    private void modify() {
        if (isFrozen()) {
            throw new FrozenError(runtime, "Struct is frozen.");
        }

        if (!isTaint() && runtime.getSafeLevel() >= 4) {
            throw new SecurityError(runtime, "Insecure: can't modify struct");
        }
    }

    private IRubyObject setByName(String name, IRubyObject value) {
        RubyArray member = (RubyArray) getInstanceVariable(classOf(), "__member__");

        Asserts.isTrue(!member.isNil(), "uninitialized struct");

        modify();

        for (int i = 0; i < member.getLength(); i++) {
            if (member.entry(i).asSymbol().equals(name)) {
                return values[i] = value;
            }
        }

        throw new NameError(runtime, name + " is not struct member");
    }

    private IRubyObject getByName(String name) {
        RubyArray member = (RubyArray) getInstanceVariable(classOf(), "__member__");

        Asserts.isTrue(!member.isNil(), "uninitialized struct");

        for (int i = 0; i < member.getLength(); i++) {
            if (member.entry(i).asSymbol().equals(name)) {
                return values[i];
            }
        }

        throw new NameError(runtime, name + " is not struct member");
    }

    // Struct methods

    /** Create new Struct class.
     *
     * MRI: rb_struct_s_def / make_struct
     *
     */
    public static RubyClass newInstance(IRubyObject recv, IRubyObject[] args) {
        String name = null;

        if (args.length > 0 && args[0] instanceof RubyString) {
            name = args[0].toString();
        }

        RubyArray member = RubyArray.newArray(recv.getRuntime());

        for (int i = name == null ? 0 : 1; i < args.length; i++) {
            member.append(RubySymbol.newSymbol(recv.getRuntime(), args[i].asSymbol()));
        }

        RubyClass newStruct;

        if (name == null) {
            newStruct = new RubyClass((RubyClass) recv);
        } else {
            if (!IdUtil.isConstant(name)) {
                throw new NameError(recv.getRuntime(), "identifier " + name + " needs to be constant");
            }
            newStruct = ((RubyClass) recv).defineClassUnder(name, ((RubyClass) recv));
        }

        newStruct.setInstanceVariable("__size__", member.length());
        newStruct.setInstanceVariable("__member__", member);

        newStruct.defineSingletonMethod("new", recv.getRuntime().callbackFactory().getOptSingletonMethod(RubyStruct.class, "newStruct"));
        newStruct.defineSingletonMethod("[]", recv.getRuntime().callbackFactory().getOptSingletonMethod(RubyStruct.class, "newStruct"));
        newStruct.defineSingletonMethod("members", recv.getRuntime().callbackFactory().getSingletonMethod(RubyStruct.class, "members"));

        // define access methods.
        for (int i = name == null ? 0 : 1; i < args.length; i++) {
            String memberName = args[i].asSymbol();
            newStruct.defineMethod(memberName, recv.getRuntime().callbackFactory().getMethod(RubyStruct.class, "get"));
            newStruct.defineMethod(memberName + "=", recv.getRuntime().callbackFactory().getMethod(RubyStruct.class, "set", IRubyObject.class));
        }

        return newStruct;
    }

    /** Create new Structure.
     *
     * MRI: struct_alloc
     *
     */
    public static RubyStruct newStruct(IRubyObject recv, IRubyObject[] args) {
        RubyStruct struct = new RubyStruct(recv.getRuntime(), (RubyClass) recv);

        int size = RubyFixnum.fix2int(getInstanceVariable((RubyClass) recv, "__size__"));

        struct.values = new IRubyObject[size];

        struct.callInit(args);

        return struct;
    }

    public IRubyObject initialize(IRubyObject[] args) {
        modify();

        int size = RubyFixnum.fix2int(getInstanceVariable(getMetaClass(), "__size__"));

        if (args.length > size) {
            throw new ArgumentError(runtime, "struct size differs (" + args.length +" for " + size + ")");
        }

        for (int i = 0; i < args.length; i++) {
            values[i] = args[i];
        }

        for (int i = args.length; i < size; i++) {
            values[i] = runtime.getNil();
        }

        return runtime.getNil();
    }

    public static RubyArray members(IRubyObject recv) {
        RubyArray member = (RubyArray) getInstanceVariable((RubyClass) recv, "__member__");

        Asserts.isTrue(!member.isNil(), "uninitialized struct");

        RubyArray result = RubyArray.newArray(recv.getRuntime(), member.getLength());
        for (int i = 0; i < member.getLength(); i++) {
            result.append(RubyString.newString(recv.getRuntime(), member.entry(i).asSymbol()));
        }

        return result;
    }

    public RubyArray members() {
        return members(classOf());
    }

    public IRubyObject set(IRubyObject value) {
        String name = runtime.getCurrentFrame().getLastFunc();
        if (name.endsWith("=")) {
            name = name.substring(0, name.length() - 1);
        }

        RubyArray member = (RubyArray) getInstanceVariable(classOf(), "__member__");

        Asserts.isTrue(!member.isNil(), "uninitialized struct");

        modify();

        for (int i = 0; i < member.getLength(); i++) {
            if (member.entry(i).asSymbol().equals(name)) {
                return values[i] = value;
            }
        }

        throw new NameError(runtime, name + " is not struct member");
    }

    public IRubyObject get() {
        String name = runtime.getCurrentFrame().getLastFunc();

        RubyArray member = (RubyArray) getInstanceVariable(classOf(), "__member__");

        Asserts.isTrue(!member.isNil(), "uninitialized struct");

        for (int i = 0; i < member.getLength(); i++) {
            if (member.entry(i).asSymbol().equals(name)) {
                return values[i];
            }
        }

        throw new NameError(runtime, name + " is not struct member");
    }

    public IRubyObject rbClone() {
        RubyStruct clone = new RubyStruct(runtime, getMetaClass());

        clone.values = new IRubyObject[values.length];
        System.arraycopy(values, 0, clone.values, 0, values.length);
        
        clone.setFrozen(this.isFrozen());
        clone.setTaint(this.isTaint());
        
        return clone;
    }

    public RubyBoolean equal(IRubyObject other) {
        if (this == other) {
            return runtime.getTrue();
        } else if (!(other instanceof RubyStruct)) {
            return runtime.getFalse();
        } else if (getMetaClass() != other.getMetaClass()) {
            return runtime.getFalse();
        } else {
            for (int i = 0; i < values.length; i++) {
                if (!values[i].equals(((RubyStruct) other).values[i])) {
                    return runtime.getFalse();
                }
            }
            return runtime.getTrue();
        }
    }

    public RubyString to_s() {
        return RubyString.newString(runtime, "#<" + getMetaClass().toName() + ">");
    }

    public RubyString inspect() {
        RubyArray member = (RubyArray) getInstanceVariable(classOf(), "__member__");

        Asserts.isTrue(!member.isNil(), "uninitialized struct");

        StringBuffer sb = new StringBuffer(100);

        sb.append("#<").append(getMetaClass().toName()).append(' ');

        for (int i = 0; i < member.getLength(); i++) {
            if (i > 0) {
                sb.append(", ");
            }

            sb.append(member.entry(i).asSymbol()).append("=");
            sb.append(values[i].callMethod("inspect"));
        }

        sb.append('>');

        return RubyString.newString(runtime, sb.toString()); // OBJ_INFECT
    }

    public RubyArray to_a() {
        return RubyArray.newArray(runtime, values);
    }

    public RubyFixnum size() {
        return RubyFixnum.newFixnum(runtime, values.length);
    }

    public IRubyObject each() {
        for (int i = 0; i < values.length; i++) {
            runtime.yield(values[i]);
        }

        return this;
    }

    public IRubyObject aref(IRubyObject key) {
        if (key instanceof RubyString || key instanceof RubySymbol) {
            return getByName(key.asSymbol());
        }

        int idx = RubyFixnum.fix2int(key);

        idx = idx < 0 ? values.length + idx : idx;

        if (idx < 0) {
            throw new IndexError(runtime, "offset " + idx + " too large for struct (size:" + values.length + ")");
        } else if (idx >= values.length) {
            throw new IndexError(runtime, "offset " + idx + " too large for struct (size:" + values.length + ")");
        }

        return values[idx];
    }

    public IRubyObject aset(IRubyObject key, IRubyObject value) {
        if (key instanceof RubyString || key instanceof RubySymbol) {
            return setByName(key.asSymbol(), value);
        }

        int idx = RubyFixnum.fix2int(key);

        idx = idx < 0 ? values.length + idx : idx;

        if (idx < 0) {
            throw new IndexError(runtime, "offset " + idx + " too large for struct (size:" + values.length + ")");
        } else if (idx >= values.length) {
            throw new IndexError(runtime, "offset " + idx + " too large for struct (size:" + values.length + ")");
        }

        modify();
        return values[idx] = value;
    }

    public void marshalTo(MarshalStream output) throws java.io.IOException {
        output.write('S');

        String className = getMetaClass().getClassname();
        if (className == null) {
            throw new ArgumentError(runtime, "can't dump anonymous class");
        }
        output.dumpObject(RubySymbol.newSymbol(runtime, className));

        List members = ((RubyArray) getInstanceVariable(classOf(), "__member__")).getList();
        output.dumpInt(members.size());

        for (int i = 0; i < members.size(); i++) {
            RubySymbol name = (RubySymbol) members.get(i);
            output.dumpObject(name);
            output.dumpObject(values[i]);
        }
    }

    public static RubyStruct unmarshalFrom(UnmarshalStream input) throws java.io.IOException {
        Ruby ruby = input.getRuntime();

        RubySymbol className = (RubySymbol) input.unmarshalObject();
        RubyClass rbClass = pathToClass(ruby, className.asSymbol());
        if (rbClass == null) {
            throw new NameError(ruby, "uninitialized constant " + className);
        }

        int size = input.unmarshalInt();

        IRubyObject[] values = new IRubyObject[size];
        for (int i = 0; i < size; i++) {
            input.unmarshalObject(); // Read and discard a Symbol, which is the name
            values[i] = input.unmarshalObject();
        }

        RubyStruct result = newStruct(rbClass, values);
        input.registerLinkTarget(result);
        return result;
    }

    private static RubyClass pathToClass(Ruby ruby, String path) {
        // FIXME: Throw the right ArgumentError's if the class is missing
        // or if it's a module.
        return (RubyClass) ruby.getClasses().getClassFromPath(path);
    }
}
