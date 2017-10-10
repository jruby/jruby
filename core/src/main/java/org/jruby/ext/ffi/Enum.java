/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2010, 2011 Wayne Meissner
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

package org.jruby.ext.ffi;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyInteger;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubySymbol;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;

/**
 * Represents a C enum
 */
@JRubyClass(name="FFI::Enum", parent="Object")
public final class Enum extends RubyObject {
    private IRubyObject nativeType;
    private final RubyHash kv_map;
    private volatile IRubyObject tag;

    private volatile Map<RubySymbol, RubyInteger> symbolToValue = new IdentityHashMap<RubySymbol, RubyInteger>();
    private volatile ConcurrentHashMap<Long, RubySymbol> valueToSymbol = new ConcurrentHashMap<Long, RubySymbol>();

    public static RubyClass createEnumClass(Ruby runtime, RubyModule ffiModule) {
        RubyClass enumClass = ffiModule.defineClassUnder("Enum", runtime.getObject(),
                Allocator.INSTANCE);
        enumClass.defineAnnotatedMethods(Enum.class);
        enumClass.defineAnnotatedConstants(Enum.class);
        enumClass.includeModule(ffiModule.getConstant("DataConverter"));
        
        return enumClass;
    }

    private static final class Allocator implements ObjectAllocator {
        private static final ObjectAllocator INSTANCE = new Allocator();

        public final IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new Enum(runtime, klass);
        }
    }

    private Enum(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
        kv_map = RubyHash.newHash(runtime);
        tag = runtime.getNil();
    }

    @JRubyMethod(name = "initialize", visibility = Visibility.PRIVATE)
    public final IRubyObject initialize(ThreadContext context, IRubyObject arg) {
        return initialize(context, null, null, arg);
    }

    @JRubyMethod(name = "initialize", visibility = Visibility.PRIVATE)
    public final IRubyObject initialize(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        if (arg0 instanceof org.jruby.ext.ffi.Type)
            return initialize(context, arg0, arg1, null);

        if (arg1.isNil())
            return initialize(context, null, arg0, null);

        // Handles bad args and tag, values case.
        return initialize(context, null, arg0, arg1);
    }

    @JRubyMethod(name = "initialize", visibility = Visibility.PRIVATE)
    public final IRubyObject initialize(ThreadContext context, IRubyObject type, IRubyObject values, IRubyObject tag) {
        int offset = 0;
        if (type instanceof org.jruby.ext.ffi.Type) {
            nativeType = type;
        } else {
            if (!(type == null || type.isNil()))
                throw context.runtime.newTypeError(type, context.runtime.getModule("FFI").getClass("Type"));

            nativeType = context.runtime.getModule("FFI").getClass("Type").getConstant("INT");
        }

        if (!(tag == null || tag.isNil() || tag instanceof RubySymbol))
            throw context.runtime.newTypeError(tag, context.runtime.getSymbol());

        this.tag = tag;

        if (!(values instanceof RubyArray)) {
            throw context.runtime.newTypeError(values, context.runtime.getArray());
        }

        RubyArray ary = (RubyArray) values;

        Map<RubySymbol, RubyInteger> s2v = new IdentityHashMap<RubySymbol, RubyInteger>();
        IRubyObject prevConstant = null;
        long nextValue = 0;

        for (int i = 0; i < ary.size(); i++) {
            IRubyObject v = ary.entry(i);

            if (v instanceof RubySymbol) {
                s2v.put((RubySymbol) v, RubyFixnum.newFixnum(context.runtime, nextValue));
                prevConstant = v;
                nextValue++;

            } else if (v instanceof RubyInteger) {
                if (prevConstant == null) {
                    throw context.runtime.newArgumentError("invalid enum sequence - no symbol for value "
                            + v);
                }
                s2v.put((RubySymbol) prevConstant, (RubyFixnum) v);
                nextValue = ((RubyInteger) v).getLongValue() + 1;

            } else {
                throw context.runtime.newTypeError(v, context.runtime.getSymbol());
            }
        }

        symbolToValue = new IdentityHashMap<RubySymbol, RubyInteger>(s2v);
        valueToSymbol = new ConcurrentHashMap<Long, RubySymbol>(symbolToValue.size());
        for (Map.Entry<RubySymbol, RubyInteger> e : symbolToValue.entrySet()) {
            kv_map.fastASet(e.getKey(), e.getValue());
            valueToSymbol.put(e.getValue().getLongValue(), e.getKey());
        }

        return this;
    }

    @JRubyMethod(name = { "[]", "find" })
    public final IRubyObject find(ThreadContext context, IRubyObject query) {
        if (query instanceof RubySymbol) {
            IRubyObject value = kv_map.fastARef(query);
            return value != null ? value : context.runtime.getNil();

        } else if (query instanceof RubyInteger) {
            RubySymbol symbol = valueToSymbol.get((Long)((RubyInteger) query).getLongValue());
            return symbol != null ? symbol : context.runtime.getNil();

        } else {
            return context.runtime.getNil();
        }
    }

    @JRubyMethod(name = { "symbol_map", "to_h", "to_hash" })
    public final IRubyObject symbol_map(ThreadContext context) {
        return kv_map.dup(context);
    }

    @JRubyMethod(name = { "symbols" })
    public final IRubyObject symbols(ThreadContext context) {
        return kv_map.keys();
    }

    @JRubyMethod(name = { "tag" })
    public final IRubyObject tag(ThreadContext context) {
        return tag;
    }

    @JRubyMethod(name = "native_type")
    public final IRubyObject native_type(ThreadContext context) {
        return nativeType;
    }

    @JRubyMethod(name = "to_native")
    public final IRubyObject to_native(ThreadContext context, IRubyObject name, IRubyObject ctx) {
        RubyInteger value;

        if (name instanceof RubyFixnum) {
            return name;

        } else if (name instanceof RubySymbol && (value = symbolToValue.get(name)) != null) {
            return value;

        } else if (name instanceof RubyInteger) {
            return name;

        } else if (name.respondsTo("to_int")) {

            return name.convertToInteger();

        } else {
            throw name.getRuntime().newArgumentError("invalid enum value, " + name.inspect());
        }
    }

    @JRubyMethod(name = "from_native")
    public final IRubyObject from_native(ThreadContext context, IRubyObject value, IRubyObject ctx) {

        RubySymbol sym;

        if (value instanceof RubyInteger && (sym = valueToSymbol.get((Long)((RubyInteger) value).getLongValue())) != null) {
            return sym;
        }

        return value;
    }

    @JRubyMethod(name = "reference_required?")
    public IRubyObject reference_required_p(ThreadContext context) {
        return context.runtime.getFalse();
    }
}
