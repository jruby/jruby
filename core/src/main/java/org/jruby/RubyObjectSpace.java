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
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
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

package org.jruby;

import static org.jruby.RubyEnumerator.enumeratorize;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.stream.Stream;

import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.exceptions.StopIteration;
import org.jruby.javasupport.JavaPackage;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;

import static org.jruby.api.Access.basicObjectClass;
import static org.jruby.api.Access.classClass;
import static org.jruby.api.Access.moduleClass;
import static org.jruby.api.Access.objectClass;
import static org.jruby.api.Convert.*;
import static org.jruby.api.Create.newArray;
import static org.jruby.api.Create.newArrayNoCopy;
import static org.jruby.api.Define.defineModule;
import static org.jruby.api.Error.*;
import static org.jruby.api.Warn.warn;
import static org.jruby.runtime.Visibility.*;
import static org.jruby.util.Inspector.inspectPrefix;

import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ConvertBytes;
import org.jruby.util.Inspector;
import org.jruby.util.Numeric;
import org.jruby.util.collections.WeakValuedIdentityMap;
import org.jruby.util.collections.WeakValuedMap;

@JRubyModule(name="ObjectSpace")
public class RubyObjectSpace {

    /** Create the ObjectSpace module and add it to the Ruby runtime.
     *
     */
    public static RubyModule createObjectSpaceModule(ThreadContext context, RubyClass Object) {
        RubyModule ObjectSpace = defineModule(context, "ObjectSpace").defineMethods(context, RubyObjectSpace.class);

        WeakMap.createWeakMap(context, Object, ObjectSpace);
        WeakKeyMap.createWeakMap(context, Object, ObjectSpace);

        return ObjectSpace;
    }

    @Deprecated(since = "10.0.0.0")
    public static IRubyObject define_finalizer(IRubyObject recv, IRubyObject[] args, Block block) {
        return define_finalizer(recv.getRuntime().getCurrentContext(), recv, args, block);
    }

    @JRubyMethod(required = 1, optional = 1, checkArity = false, module = true, visibility = PRIVATE)
    public static IRubyObject define_finalizer(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        int argc = Arity.checkArgumentCount(context, args, 1, 2);

        IRubyObject finalizer;
        IRubyObject obj = args[0];
        if (argc == 2) {
            finalizer = args[1];
            if (!finalizer.respondsTo("call")) {
                throw argumentError(context, "wrong type argument " + finalizer.getType() + " (should be callable)");
            }
            if (finalizer instanceof RubyMethod) {
                if (((RubyMethod) finalizer).getReceiver() == obj) referenceWarning(context);
            }
            if (finalizer instanceof RubyProc) {
                if (((RubyProc) finalizer).getBlock().getBinding().getSelf() == obj) referenceWarning(context);
            }
        } else {
            if (blockReferencesObject(obj, block)) referenceWarning(context);
            finalizer = context.runtime.newProc(Block.Type.PROC, block);
        }
        finalizer = context.runtime.getObjectSpace().addFinalizer(context, obj, finalizer);
        return newArray(context, asFixnum(context, 0), finalizer);
    }

    private static void referenceWarning(ThreadContext context) {
        warn(context, "finalizer references object to be finalized");
    }

    private static boolean blockReferencesObject(IRubyObject object, Block block) {
        return block.getBinding().getSelf() == object;
    }

    @Deprecated(since = "10.0.0.0")
    public static IRubyObject undefine_finalizer(IRubyObject recv, IRubyObject obj, Block block) {
        return undefine_finalizer(((RubyBasicObject) recv).getCurrentContext(), recv, obj, block);
    }

    @JRubyMethod(module = true, visibility = PRIVATE)
    public static IRubyObject undefine_finalizer(ThreadContext context, IRubyObject recv, IRubyObject obj, Block block) {
        context.runtime.getObjectSpace().removeFinalizers(toLong(context, obj.id()));
        return recv;
    }

    @Deprecated(since = "10.0.0.0")
    public static IRubyObject id2ref(IRubyObject recv, IRubyObject id) {
        return id2ref(((RubyBasicObject) recv).getCurrentContext(), recv, id);
    }

    @JRubyMethod(name = "_id2ref", module = true, visibility = PRIVATE)
    public static IRubyObject id2ref(ThreadContext context, IRubyObject recv, IRubyObject id) {
        long longId = castAsFixnum(context, id).getValue();
        if (longId == 0) return context.fals;
        if (longId == 20) return context.tru;
        if (longId == 8) return context.nil;
        if ((longId & 0b01) == 0b01) return asFixnum(context, (longId - 1) / 2);  // fixnum
        if ((longId & 0b11) == 0b10) {
            // flonum
            double d = 0.0;
            if (longId != 0x8000000000000002L) {
                long b63 = (longId >>> 63);
                /* e: xx1... -> 011... */
                /*    xx0... -> 100... */
                /*      ^b63           */
                long longBits = Numeric.rotr((2 - b63) | (longId & ~0x03), 3);
                d = Double.longBitsToDouble(longBits);
            }
            return asFloat(context, d);
        } else {
            if (context.runtime.isObjectSpaceEnabled()) {
                IRubyObject object = context.runtime.getObjectSpace().id2ref(longId);
                if (object == null) {
                    return context.nil;
                }
                return object;
            } else {
                warn(context, "ObjectSpace is disabled; _id2ref only supports immediates, pass -X+O to enable");
                throw rangeError(context, String.format("0x%016x is not id value", longId));
            }
        }
    }

    public static IRubyObject each_objectInternal(final ThreadContext context, IRubyObject recv, IRubyObject[] args, final Block block) {
        final Ruby runtime = context.runtime;
        final RubyModule rubyClass;
        if (args.length == 0) {
            rubyClass = objectClass(context);
        } else {
            if (!(args[0] instanceof RubyModule)) throw argumentError(context, "class or module required");
            rubyClass = (RubyModule) args[0];
        }
        if (rubyClass == classClass(context) || rubyClass == moduleClass(context)) {

            // Use set in case there's any overlaps between eachModule and BasicObject descendants (should not be).
            final HashSet<IRubyObject> modules = new HashSet<>(96);

            // if Module is requested, iterate over true modules as well as all classes
            if (rubyClass == moduleClass(context)) {
                runtime.eachModule((module) -> {
                    modules.add(module); // store the module to avoid concurrent modification exceptions
                });
            }

            // Iterate over all classes
            basicObjectClass(context).subclasses(true).forEach((module) -> {
                if (rubyClass.isInstance(module)) {
                    if (!(module instanceof IncludedModule || module instanceof PrependedModule
                            || module == runtime.getJavaSupport().getJavaPackageClass()
                            || (module instanceof MetaClass && (((MetaClass) module).getAttached() instanceof JavaPackage)))) {
                        // do nothing for included wrappers or singleton classes
                        modules.add(module); // store the module to avoid concurrent modification exceptions
                    }
                }
            });

            modules.forEach((obj) -> block.yield(context, obj));
            return asFixnum(context, modules.size());
        }
        if (rubyClass.getClass() == MetaClass.class) {
            // each_object(Cls.singleton_class) is basically a walk of Cls and all descendants of Cls.
            // In other words, this is walking all instances of Cls's singleton class and its subclasses.
            IRubyObject attached = ((MetaClass) args[0]).getAttached();
            block.yield(context, attached); int count = 1;
            if (attached instanceof RubyClass) {
                for (RubyClass child : ((RubyClass) attached).subclasses(true)) {
                    if (!(child instanceof IncludedModule)) {
                        // do nothing for included wrappers or singleton classes
                        count++; block.yield(context, child);
                    }
                }
            }
            return asFixnum(context, count);
        }
        if (!runtime.isObjectSpaceEnabled()) {
            throw runtimeError(context, "ObjectSpace is disabled; each_object will only work with Class, pass -X+O to enable");
        }
        final Iterator iter = runtime.getObjectSpace().iterator(rubyClass);
        IRubyObject obj; int count = 0;
        while ((obj = (IRubyObject) iter.next()) != null) {
            count++; block.yield(context, obj);
        }
        return asFixnum(context, count);
    }

    @JRubyMethod(name = "each_object", optional = 1, checkArity = false, module = true, visibility = PRIVATE)
    public static IRubyObject each_object(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Arity.checkArgumentCount(context, args, 0, 1);

        return block.isGiven() ? each_objectInternal(context, recv, args, block) : enumeratorize(context.runtime, recv, "each_object", args);
    }

    @JRubyMethod(name = "garbage_collect", module = true, visibility = PRIVATE, optional = 1, checkArity = false)
    public static IRubyObject garbage_collect(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return RubyGC.start(context, recv, args);
    }

    public static abstract class AbstractWeakMap extends RubyObject {
        public AbstractWeakMap(Ruby runtime, RubyClass cls) {
            super(runtime, cls);
        }

        protected abstract Map<IRubyObject, IRubyObject> getWeakMapFor(IRubyObject key);

        protected abstract Stream<Map.Entry<IRubyObject, IRubyObject>> getEntryStream();

        @JRubyMethod(name = {"length", "size"})
        public abstract IRubyObject size(ThreadContext context);

        @JRubyMethod(name = "inspect")
        public abstract IRubyObject inspect(ThreadContext context);

        @JRubyMethod(name = "[]")
        public IRubyObject op_aref(ThreadContext context, IRubyObject key) {
            Map<IRubyObject, IRubyObject> weakMap = getWeakMapFor(key);
            IRubyObject value = weakMap.get(key);
            if (value != null) return value;
            return context.nil;
        }

        @JRubyMethod(name = "[]=")
        public IRubyObject op_aref(ThreadContext context, IRubyObject key, IRubyObject value) {
            Map<IRubyObject, IRubyObject> weakMap = getWeakMapFor(key);
            weakMap.put(key, value);

            return asFixnum(context, System.identityHashCode(value));
        }

        @JRubyMethod(name = "key?")
        public IRubyObject key_p(ThreadContext context, IRubyObject key) {
            Map<IRubyObject, IRubyObject> weakMap = getWeakMapFor(key);
            return asBoolean(context, weakMap.get(key) != null);
        }

        @JRubyMethod(name = "keys")
        public IRubyObject keys(ThreadContext context) {
            return newArrayNoCopy(context, getEntryStream().map(Map.Entry::getKey).toArray(IRubyObject[]::new));
        }

        @JRubyMethod(name = "values")
        public IRubyObject values(ThreadContext context) {
            return newArrayNoCopy(context, getEntryStream().map(Map.Entry::getValue).toArray(IRubyObject[]::new));
        }

        @JRubyMethod(name = {"each", "each_pair"})
        public IRubyObject each(ThreadContext context, Block block) {
            getEntryStream().forEach((entry) -> {
                block.yieldSpecific(context, entry.getKey(), entry.getValue());
            });

            return this;
        }

        @JRubyMethod(name = "each_key")
        public IRubyObject each_key(ThreadContext context, Block block) {
            getEntryStream().forEach((entry) -> {
                block.yieldSpecific(context, entry.getKey());
            });

            return this;
        }

        @JRubyMethod(name = "each_value")
        public IRubyObject each_value(ThreadContext context, Block block) {
            getEntryStream().forEach((entry) -> {
                block.yieldSpecific(context, entry.getValue());
            });

            return this;
        }

        @JRubyMethod(name = {"include?", "member?"})
        public IRubyObject member_p(ThreadContext context, IRubyObject key) {
            return asBoolean(context, getWeakMapFor(key).containsKey(key));
        }

        @JRubyMethod(name = "delete")
        public IRubyObject delete(ThreadContext context, IRubyObject key, Block block) {
            IRubyObject value = getWeakMapFor(key).remove(key);

            if (value != null) {
                return value;
            }

            if (block.isGiven()) {
                return block.yieldSpecific(context, key);
            }

            return context.nil;
        }

        @JRubyMethod(name = "clear")
        public abstract IRubyObject clear(ThreadContext context);
    }

    public static class WeakMap extends AbstractWeakMap {
        static void createWeakMap(ThreadContext context, RubyClass Object, RubyModule ObjectSpace) {
            ObjectSpace.defineClassUnder(context, "WeakMap", Object, WeakMap::new).
                    defineMethods(context, AbstractWeakMap.class);
        }

        public WeakMap(Ruby runtime, RubyClass cls) {
            super(runtime, cls);
        }

        protected Map<IRubyObject, IRubyObject> getWeakMapFor(IRubyObject key) {
            if (key instanceof RubyFixnum || key instanceof RubyFloat) {
                return valueMap;
            }

            return identityMap;
        }

        protected Stream<Map.Entry<IRubyObject, IRubyObject>> getEntryStream() {
            return Stream.concat(identityMap.entrySet().stream(), valueMap.entrySet().stream()).filter((entry) -> entry.getValue() != null);
        }

        public IRubyObject size(ThreadContext context) {
            return asFixnum(context, identityMap.size() + valueMap.size());
        }

        public IRubyObject inspect(ThreadContext context) {
            RubyString part = inspectPrefix(context, metaClass.getRealClass(), inspectHashCode());
            int base = part.length();

            getEntryStream().forEach(entry -> {
                part.cat(part.length() == base ? Inspector.COLON_SPACE : Inspector.COMMA_SPACE);
                part.cat(entry.getKey().inspect(context).convertToString());
                part.cat(Inspector.SPACE_HASHROCKET_SPACE);
                part.cat(entry.getValue().inspect(context).convertToString());
            });

            part.cat(Inspector.GT);

            return part;
        }

        @Override
        public IRubyObject clear(ThreadContext context) {
            identityMap.clear();
            valueMap.clear();

            return this;
        }

        private final WeakValuedIdentityMap<IRubyObject, IRubyObject> identityMap = new WeakValuedIdentityMap<>();
        private final WeakValuedMap<IRubyObject, IRubyObject> valueMap = new WeakValuedMap<>();
    }

    public static class WeakKeyMap extends AbstractWeakMap {
        static void createWeakMap(ThreadContext context, RubyClass Object, RubyModule ObjectSpace) {
            ObjectSpace.defineClassUnder(context, "WeakKeyMap", Object, WeakKeyMap::new).
                    defineMethods(context, AbstractWeakMap.class, WeakKeyMap.class);
        }

        public WeakKeyMap(Ruby runtime, RubyClass cls) {
            super(runtime, cls);
        }

        protected Map<IRubyObject, IRubyObject> getWeakMapFor(IRubyObject key) {
            // TODO: we don't have a supertype for these?
            if (key instanceof RubyInteger || key instanceof RubyFloat || key instanceof RubySymbol || key instanceof RubyNil || key instanceof RubyBoolean) {
                throw argumentError(getRuntime().getCurrentContext(), "WeakKeyMap must be garbage collectable");
            }

            return weakMap;
        }

        protected Stream<Map.Entry<IRubyObject, IRubyObject>> getEntryStream() {
            return weakMap.entrySet().stream();
        }

        public IRubyObject op_aref(ThreadContext context, IRubyObject key, IRubyObject value) {
            // defensively call #hash since #hashCode will fall back on super in RubyBasicObject if #hash is undefined.
            key.callMethod(context, "hash");

            super.op_aref(context, key, value);

            return value;
        }

        public IRubyObject size(ThreadContext context) {
            return asFixnum(context, weakMap.size());
        }

        public IRubyObject inspect(ThreadContext context) {
            RubyString part = inspectPrefix(context, metaClass.getRealClass(), inspectHashCode());

            part.cat(Inspector.SPACE);
            part.cat(Inspector.SIZE_EQUALS);
            part.cat(ConvertBytes.longToCharBytes(weakMap.size()));

            part.cat(Inspector.GT);

            return part;
        }

        @JRubyMethod(name = "getkey")
        public IRubyObject getkey(ThreadContext context, IRubyObject key) {
            // FIXME: inefficient, but JDK WeakHashMap provides no other way to access the actual keys
            IRubyObject result = null;
            try {
                getWeakMapFor(key).keySet().forEach((k) -> {if (key.equals(k)) throw context.runtime.newStopIteration(k, "");});
            } catch (StopIteration si) {
                result = ((RubyStopIteration) si.getException()).result();
            }

            if (result == null) {
                return context.nil;
            }

            return result;
        }

        @Override
        public IRubyObject clear(ThreadContext context) {
            weakMap.clear();

            return this;
        }

        private final WeakHashMap<IRubyObject, IRubyObject> weakMap = new WeakHashMap();
    }
}
