/*
 * JavaObject.java - No description
 * Created on 21. September 2001, 14:43
 *
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Stefan Matthias Aust <sma@3plus4.de>
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

package org.jruby.javasupport;

import org.jruby.runtime.IndexCallable;
import org.jruby.runtime.IndexedCallback;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.RubyObject;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyString;
import org.jruby.RubyBoolean;
import org.jruby.exceptions.TypeError;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class JavaObject extends RubyObject implements IndexCallable {
    private final Object value;

    protected JavaObject(Ruby ruby, RubyClass rubyClass, Object value) {
        super(ruby, rubyClass);
        this.value = value;
    }

    protected JavaObject(Ruby ruby, Object value) {
        this(ruby, ruby.getClasses().getJavaObjectClass(), value);
    }

    public static synchronized JavaObject wrap(Ruby runtime, Object value) {
        JavaObject wrapper = runtime.getJavaSupport().getJavaObjectFromCache(value);
        if (wrapper == null) {
            if (value != null && value.getClass().isArray()) {
                wrapper = new JavaArray(runtime, value);
            } else {
                wrapper = new JavaObject(runtime, value);
            }
            runtime.getJavaSupport().putJavaObjectIntoCache(wrapper);
        }
        return wrapper;
    }

    public Class getJavaClass() {
        return value != null ? value.getClass() : Object.class;
    }

    /** Getter for property value.
     * @return Value of property value.
     */
    public Object getValue() {
        return value;
    }

    private static final int TO_S = 1;
    private static final int EQUAL = 2;
    private static final int HASH = 3;
    private static final int JAVA_TYPE = 4;
    private static final int JAVA_CLASS = 5;
    private static final int LENGTH = 6;
    private static final int AREF = 7;
    private static final int ASET = 8;

    public static RubyClass createJavaObjectClass(Ruby ruby) {
        RubyClass javaObjectClass = ruby.defineClass("JavaObject", ruby.getClasses().getObjectClass());

        javaObjectClass.defineMethod("to_s", IndexedCallback.create(TO_S, 0));
        javaObjectClass.defineMethod("eql?", IndexedCallback.create(EQUAL, 1));
        javaObjectClass.defineMethod("==", IndexedCallback.create(EQUAL, 1));
		javaObjectClass.defineMethod("hash", IndexedCallback.create(HASH, 0));
        javaObjectClass.defineMethod("java_type", IndexedCallback.create(JAVA_TYPE, 0));
        javaObjectClass.defineMethod("java_class", IndexedCallback.create(JAVA_CLASS, 0));
        javaObjectClass.defineMethod("length", IndexedCallback.create(LENGTH, 0));
        javaObjectClass.defineMethod("[]", IndexedCallback.create(AREF, 1));
        javaObjectClass.defineMethod("[]=", IndexedCallback.create(ASET, 2));

        javaObjectClass.getMetaClass().undefMethod("new");

        return javaObjectClass;
    }

    public RubyFixnum hash() {
        return RubyFixnum.newFixnum(runtime, value.hashCode());
    }

    public RubyString to_s() {
        return RubyString.newString(getRuntime(), getValue() != null ? getValue().toString() : "null");
    }

    public RubyBoolean equal(IRubyObject other) {
        if (other instanceof JavaObject) {
            return (getValue() != null && getValue().equals(((JavaObject) other).getValue()))
                ? getRuntime().getTrue()
                : getRuntime().getFalse();
        }
        return getRuntime().getFalse();
    }

    public RubyString java_type() {
        return RubyString.newString(getRuntime(), getJavaClass().getName());
    }

    public IRubyObject java_class() {
        return new JavaClass(getRuntime(), getJavaClass());
    }

    public RubyFixnum length() {
        throw new TypeError(getRuntime(), "not a java array");
    }

    public IRubyObject aref(IRubyObject index) {
        throw new TypeError(getRuntime(), "not a java array");
    }

    public IRubyObject aset(IRubyObject index, IRubyObject value) {
        throw new TypeError(getRuntime(), "not a java array");
    }

    public IRubyObject callIndexed(int index, IRubyObject[] args) {
        switch (index) {
            case TO_S :
                return to_s();
            case EQUAL :
                return equal(args[0]);
            case HASH :
                return hash();
            case JAVA_TYPE :
                return java_type();
            case JAVA_CLASS :
                return java_class();
            case LENGTH :
                return length();
            case AREF :
                return aref(args[0]);
            case ASET :
                return aset(args[0], args[1]);
            default :
                return super.callIndexed(index, args);
        }
    }
}
