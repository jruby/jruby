/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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

package org.jruby.runtime;

import com.headius.invokebinder.Binder;
import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MutableCallSite;
import java.util.function.Function;
import java.util.function.Supplier;

public class GlobalSite extends MutableCallSite {
    public static final Function IDENTITY = Function.identity();
    public static final Runnable NOP = () -> {};

    public static class Copy extends GlobalSite {
        private GlobalSite other;

        public Copy(Ruby runtime, String name, GlobalSite other) {
            super(runtime, name, other.dynamicInvoker());
            this.other = other;
        }

        @Override
        public IRubyObject get() {
            return other.get();
        }

        @Override
        public IRubyObject set(IRubyObject value) {
            return other.set(value);
        }
    }

    protected final Ruby runtime;

    protected final String name;

    public static String variableName(String name) {
        return "$" + name;
    }

    public GlobalSite(Ruby runtime, String name, IRubyObject value) {
        this(runtime, name, MethodHandles.constant(IRubyObject.class, value));
    }

    public GlobalSite(Ruby runtime, String name, Supplier<IRubyObject> supplier) {
        this(runtime, name, getter(supplier));
    }

    public GlobalSite(Ruby runtime, String name, MethodHandle target) {
        super(target.type());

        assert name != null;
        assert name.startsWith("$");

        this.runtime = runtime;
        this.name = name;

        setTargetDirect(target);
    }

    public String name() {
        return name;
    }

    public IRubyObject get() {
        try {
            return (IRubyObject) super.getTarget().invokeExact();
        } catch (Throwable t) {
            Helpers.throwException(t);
            return null; // not reached
        }
    }

    public static MethodHandle getter(Supplier getter) {
        return Binder.from(IRubyObject.class)
                .cast(Object.class)
                .insert(0, Supplier.class, getter)
                .invokeVirtualQuiet(LOOKUP, "get");
    }

    public IRubyObject set(IRubyObject value) {
        value = setFilter(value);
        setTarget(MethodHandles.constant(IRubyObject.class, value));
        return value;
    }

    public static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private static final MethodHandle GET_FOLD = Binder.from(void.class, GlobalSite.class).invokeVirtualQuiet(LOOKUP, "getFold");
    private static final MethodHandle GET_FILTER = Binder.from(IRubyObject.class, GlobalSite.class, IRubyObject.class).invokeVirtualQuiet(LOOKUP, "getFilter");

    protected void getFold() {
    }

    protected IRubyObject getFilter(IRubyObject value) {
        return value;
    }

    protected void setTargetDirect(MethodHandle target) {
        super.setTarget(
                Binder.from(target.type())
                        .foldVoid(GET_FOLD.bindTo(this))
                        .filterReturn(GET_FILTER.bindTo(this)).invoke(target));
    }

    protected IRubyObject setFilter(IRubyObject value) {
        assert value != null;
        return value;
    }

    public boolean isDefined() {
        return getTarget() != null;
    }
}
