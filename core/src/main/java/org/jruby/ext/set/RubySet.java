/*
 **** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2016-2017 Karol Bucek
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

package org.jruby.ext.set;

import org.jcodings.specific.USASCIIEncoding;
import org.jruby.*;
import org.jruby.RubyEnumerator.SizeFn;
import org.jruby.anno.JRubyMethod;
import org.jruby.api.Access;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.NewMarshal;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.ArraySupport;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Set;

import static org.jruby.RubyEnumerator.enumeratorizeWithSize;
import static org.jruby.api.Access.enumerableModule;
import static org.jruby.api.Access.getModule;
import static org.jruby.api.Access.hashClass;
import static org.jruby.api.Access.loadService;
import static org.jruby.api.Access.objectClass;
import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Define.defineClass;
import static org.jruby.api.Create.newArray;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Warn.warning;

/**
 * Native implementation of Ruby's Set (set.rb replacement).
 *
 * @author kares
 */
@org.jruby.anno.JRubyClass(name="Set", include = { "Enumerable" })
public class RubySet extends RubyObject implements Set {

    static RubyClass createSetClass(ThreadContext context, RubyClass Object, RubyModule Enumerable) {
        RubyClass Set = defineClass(context, "Set", Object, RubySet::new).
                reifiedClass(RubySet.class).
                include(context, Enumerable).
                defineMethods(context, RubySet.class).
                tap(c -> c.marshalWith(new SetMarshal(c.getMarshal())));

        loadService(context).require("jruby/set.rb");

        return Set;
    }

    // custom Set marshaling without _marshal_dump and _marshal_load for maximum compatibility
    private static final class SetMarshal implements ObjectMarshal {

        protected final ObjectMarshal defaultMarshal;

        SetMarshal(ObjectMarshal defaultMarshal) {
            this.defaultMarshal = defaultMarshal;
        }

        public void marshalTo(Ruby runtime, Object obj, RubyClass type, MarshalStream marshalStream) throws IOException {
            defaultMarshal.marshalTo(runtime, obj, type, marshalStream);
        }

        public void marshalTo(Object obj, RubyClass type, NewMarshal marshalStream, ThreadContext context, NewMarshal.RubyOutputStream out) {
            defaultMarshal.marshalTo(obj, type, marshalStream, context, out);
        }

        public Object unmarshalFrom(Ruby runtime, RubyClass type, UnmarshalStream unmarshalStream) throws IOException {
            Object result = defaultMarshal.unmarshalFrom(runtime, type, unmarshalStream);
            ((RubySet) result).unmarshal();
            return result;
        }

    }

    void unmarshal() {
        this.hash = (RubyHash) getInstanceVariable("@hash");
    }

    RubyHash hash; // @hash

    protected RubySet(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }

    /*
    private RubySet(Ruby runtime, RubyHash hash) {
        super(runtime, runtime.getClass("Set"));
        allocHash(hash);
    } */

    // since MRI uses Hash.new(false) we'll (initially) strive for maximum compatibility
    // ... this is important with Rails using Sprockets at its marshalling Set instances

    final void allocHash(final ThreadContext context) {
        setHash(new RubyHash(context.runtime, context.fals));
    }

    final void allocHash(final Ruby runtime) {
        setHash(new RubyHash(runtime, runtime.getFalse()));
    }

    final void allocHash(final ThreadContext context, final int size) {
        setHash(new RubyHash(context.runtime, context.fals, size));
    }

    final void setHash(final RubyHash hash) {
        this.hash = hash;
        setInstanceVariable("@hash", hash); // MRI compat with set.rb
    }

    /**
     * Construct a new Set with the same class as this one.
     *
     * @param runtime the current runtime
     * @return a new Set
     */
    RubySet newSetFast(final Ruby runtime) {
        return newSet(runtime, getMetaClass());
    }

    /**
     * Construct a new Set. The Set class will be retrieved from the global namespace.
     *
     * @param runtime the current runtime
     * @return a new Set
     */
    public static RubySet newSet(final Ruby runtime) {
        return newSet(runtime, (RubyClass) runtime.getClassFromPath("Set"));
    }

    /**
     * Construct a new Set.
     *
     * @param runtime the current runtime
     * @param metaclass the class to assign to the new set
     * @return a new Set
     */
    public static RubySet newSet(final Ruby runtime, final RubyClass metaclass) {
        RubySet set = new RubySet(runtime, metaclass);
        set.allocHash(runtime);
        return set;
    }

    private static RubySet newSet(final ThreadContext context, final RubyClass metaClass, final RubyArray elements) {
        final RubySet set = new RubySet(context.runtime, metaClass);
        return set.initSet(context, elements.toJavaArrayMaybeUnsafe(), 0, elements.size());
    }

    final RubySet initSet(final ThreadContext context, final IRubyObject[] elements, final int off, final int len) {
        allocHash(context, Math.max(4, len));
        for ( int i = off; i < len; i++ ) {
            invokeAdd(context, elements[i]);
        }
        return this;
    }

    /**
     * Creates a new set containing the given objects.
     */
    @JRubyMethod(name = "[]", rest = true, meta = true) // def self.[](*ary)
    public static RubySet create(final ThreadContext context, IRubyObject self, IRubyObject... ary) {
        final Ruby runtime = context.runtime;

        RubySet set = new RubySet(runtime, (RubyClass) self);
        return set.initSet(context, ary, 0, ary.length);
    }

    /**
     * initialize(enum = nil, &amp;block)
     */
    @JRubyMethod(visibility = Visibility.PRIVATE) // def initialize(enum = nil, &block)
    public IRubyObject initialize(ThreadContext context, Block block) {
        if (block.isGiven() && context.runtime.isVerbose()) warning(context, "given block not used");

        allocHash(context);
        return this;
    }

    /**
     * initialize(enum = nil, &amp;block)
     */
    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject enume, Block block) {
        if ( enume.isNil() ) return initialize(context, block);

        if ( block.isGiven() ) {
            return initWithEnum(context, enume, block);
        }

        allocHash(context);
        return sites(context).merge.call(context, this, this, enume); // TODO site-cache
    }

    protected IRubyObject initialize(ThreadContext context, IRubyObject[] args, Block block) {
        return switch (args.length) {
            case 0 -> initialize(context, block);
            case 1 -> initialize(context, args[0], block);
            default -> throw argumentError(context, args.length, 1);
        };
    }

    private IRubyObject initWithEnum(final ThreadContext context, final IRubyObject enume, final Block block) {
        if (enume instanceof RubyArray ary) {
            allocHash(context, ary.size());
            for ( int i = 0; i < ary.size(); i++ ) {
                invokeAdd(context, block.yield(context, ary.eltInternal(i)));
            }
            return ary; // done
        } else if (enume instanceof RubySet set) {
            allocHash(context, set.size());
            for ( IRubyObject elem : set.elementsOrdered() ) {
                invokeAdd(context, block.yield(context, elem));
            }
            return set; // done
        } else {
            allocHash(context);

            // set.rb do_with_enum :
            return doWithEnum(context, enume, new EachBody(context) {
                IRubyObject yieldImpl(ThreadContext context, IRubyObject val) {
                    return invokeAdd(context, block.yield(context, val));
                }
            });
        }
    }

    // set.rb do_with_enum (block is required)
    private static IRubyObject doWithEnum(final ThreadContext context, final IRubyObject enume, final EachBody blockImpl) {
        JavaSites.SetSites sites = sites(context);
        if ( sites.respond_to_each_entry.respondsTo(context, enume, enume) ) {
            return sites.each_entry.call(context, enume, enume, new Block(blockImpl));
        }
        if ( sites.respond_to_each.respondsTo(context, enume, enume) ) {
            return sites.each.call(context, enume, enume, new Block(blockImpl));
        }

        throw argumentError(context, "value must be enumerable");
    }

    // YAML doesn't have proper treatment for Set serialization, it dumps it just like
    // any Ruby object, meaning on YAML.load will allocate an "initialize" all i-vars!
    @Override
    public IRubyObject instance_variable_set(IRubyObject name, IRubyObject value) {
        if (getRuntime().newSymbol("@hash").equals(name)) {
            if (value instanceof RubyHash) {
                setHash((RubyHash) value); return value;
            }
        }
        return super.instance_variable_set(name, value);
    }

    IRubyObject invokeAdd(final ThreadContext context, final IRubyObject val) {
        return sites(context).add.call(context, this, this, val);
    }

    private static abstract class EachBody extends JavaInternalBlockBody {

        EachBody(final ThreadContext context) {
            super(context.runtime, Signature.ONE_ARGUMENT);
        }

        @Override
        public IRubyObject yield(ThreadContext context, IRubyObject[] args) {
            return yieldImpl(context, args[0]);
        }

        abstract IRubyObject yieldImpl(ThreadContext context, IRubyObject val) ;

        @Override
        protected final IRubyObject doYield(ThreadContext context, Block block, IRubyObject[] args, IRubyObject self) {
            return yieldImpl(context, args[0]);
        }

        @Override
        protected final IRubyObject doYield(ThreadContext context, Block block, IRubyObject value) {
            return yieldImpl(context, value); // avoid new IRubyObject[] { value }
        }

    }

    @JRubyMethod(frame = true)
    public IRubyObject initialize_dup(ThreadContext context, IRubyObject orig) {
        sites(context).initialize_dup_super.call(context, this, this, orig);
        setHash((RubyHash) (((RubySet) orig).hash).dup(context));
        return this;
    }

    @JRubyMethod(frame = true, keywords = true, required = 1, optional = 1, checkArity = false)
    public IRubyObject initialize_clone(ThreadContext context, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 1, 2);

        sites(context).initialize_clone_super.call(context, this, this, args);
        IRubyObject orig = args[0];
        setHash((RubyHash) (((RubySet) orig).hash).rbClone(context));
        return this;
    }

    @Override
    @JRubyMethod
    public IRubyObject freeze(ThreadContext context) {
        final RubyHash hash = this.hash;
        if ( hash != null ) hash.freeze(context);
        return super.freeze(context);
    }

    @JRubyMethod(name = "size", alias = "length")
    public IRubyObject length(ThreadContext context) {
        return asFixnum(context, size());
    }

    @JRubyMethod(name = "empty?")
    public IRubyObject empty_p(ThreadContext context) {
        return asBoolean(context, isEmpty());
    }

    @JRubyMethod(name = "clear")
    public IRubyObject rb_clear(ThreadContext context) {
        modifyCheck(context);

        clearImpl(context);
        return this;
    }

    @Deprecated(since = "10.0")
    protected void clearImpl() {
        clearImpl(getCurrentContext());
    }

    protected void clearImpl(ThreadContext context) {
        hash.rb_clear(context);
    }

    /**
     * Replaces the contents of the set with the contents of the given enumerable object and returns self.
     */
    @JRubyMethod
    public RubySet replace(final ThreadContext context, IRubyObject enume) {
        if (enume instanceof RubySet enu) {
            modifyCheck(context);
            clearImpl(context);
            addImplSet(context, enu);
        } else {
            // do_with_enum(enum)  # make sure enum is enumerable before calling clear :
            if (!enume.getMetaClass().hasModuleInHierarchy(enumerableModule(context))) {
                // NOTE: likely no need to do this but due MRI compat (do_with_enum) :
                if (!enume.respondsTo("each_entry")) throw argumentError(context, "value must be enumerable");
            }
            clearImpl(context);
            rb_merge(context, enume);
        }

        return this;
    }

    /**
     * Converts the set to an array.  The order of elements is uncertain.
     */
    @JRubyMethod
    public RubyArray to_a(final ThreadContext context) {
        // except MRI relies on Hash order so we do as well
        return this.hash.keys(context);
    }

    // Returns self if no arguments are given.
    @JRubyMethod
    public RubySet to_set(final ThreadContext context, final Block block) {
        if ( block.isGiven() ) {
            RubySet set = new RubySet(context.runtime, getMetaClass());
            set.initialize(context, this, block);
            return set;
        }
        return this;
    }

    // Otherwise, converts the set to another with klass.new(self, *args, &block).
    @JRubyMethod(rest = true)
    public RubySet to_set(final ThreadContext context, final IRubyObject[] args, final Block block) {
        if ( args.length == 0 ) return to_set(context, block);

        IRubyObject klass = args[0];
        final RubyClass Set = Access.getClass(context, "Set");

        if (klass == Set && args.length == 1 && !block.isGiven()) return this;

        final IRubyObject[] rest;
        if (klass instanceof RubyClass) {
            rest = ArraySupport.newCopy(args, 1, args.length - 1);
        } else {
            klass = Set;
            rest = args;
        }

        RubySet set = new RubySet(context.runtime, (RubyClass) klass);
        set.initialize(context, rest, block);
        return set;
    }

    @JRubyMethod
    public IRubyObject compare_by_identity(ThreadContext context) {
        this.hash.compare_by_identity(context);
        return this;
    }

    @JRubyMethod(name = "compare_by_identity?")
    public IRubyObject compare_by_identity_p(ThreadContext context) {
        return this.hash.compare_by_identity_p(context);
    }

    @JRubyMethod(visibility = Visibility.PROTECTED)
    public RubySet flatten_merge(final ThreadContext context, IRubyObject set) {
        flattenMerge(context, set, new IdentityHashMap());
        return this;
    }

    private void flattenMerge(final ThreadContext context, final IRubyObject setArg, final IdentityHashMap seen) {
        if (setArg instanceof RubySet set) {
            for (IRubyObject e: set.elementsOrdered()) {
                addFlattened(context, seen, e);
            }
        } else {
            sites(context).each.call(context, setArg, setArg, new Block(
                new EachBody(context) {
                    IRubyObject yieldImpl(ThreadContext context, IRubyObject e) {
                        addFlattened(context, seen, e); return context.nil;
                    }
                })
            );
        }
    }

    private void addFlattened(final ThreadContext context, final IdentityHashMap seen, IRubyObject e) {
        if (e instanceof RubySet) {
            if (seen.containsKey(e)) throw argumentError(context, "tried to flatten recursive Set");

            seen.put(e, null);
            flattenMerge(context, e, seen);
            seen.remove(e);
        } else {
            add(context, e); // self.add(e)
        }
    }

    // Returns a new set that is a copy of the set, flattening each containing set recursively.
    @JRubyMethod
    public RubySet flatten(final ThreadContext context) {
        return newSetFast(context.runtime).flatten_merge(context, this);
    }

    @JRubyMethod(name = "flatten!")
    public IRubyObject flatten_bang(final ThreadContext context) {
        for ( IRubyObject e : elementsOrdered() ) {
            if ( e instanceof RubySet ) { // needs flatten
                return replace(context, flatten(context));
            }
        }
        return context.nil;
    }

    /**
     * Returns true if the set contains the given object.
     */
    @JRubyMethod(name = "include?", alias = { "member?", "===" })
    public RubyBoolean include_p(final ThreadContext context, IRubyObject obj) {
        return asBoolean(context,  containsImpl(obj) );
    }

    final boolean containsImpl(IRubyObject obj) {
        return hash.fastARef(obj) != null;
    }

    private boolean allElementsIncluded(final RubySet set) {
        for (IRubyObject o : set.elements()) { // set.all? { |o| include?(o) }
            if (!containsImpl(o)) return false;
        }
        return true;
    }

    // Returns true if the set is a superset of the given set.
    @JRubyMethod(name = "superset?", alias = { ">=" })
    public IRubyObject superset_p(final ThreadContext context, IRubyObject setArg) {
        if (!(setArg instanceof RubySet set)) throw argumentError(context, "value must be a set");
        if (getMetaClass().isInstance(set)) return hash.op_ge(context, set.hash);

        // size >= set.size && set.all? { |o| include?(o) }
        return asBoolean(context, size() >= set.size() && allElementsIncluded(set));
    }

    // Returns true if the set is a proper superset of the given set.
    @JRubyMethod(name = "proper_superset?", alias = { ">" })
    public IRubyObject proper_superset_p(final ThreadContext context, IRubyObject setArg) {
        if (!(setArg instanceof RubySet set)) throw argumentError(context, "value must be a set");
        if (getMetaClass().isInstance(set)) return hash.op_gt(context, set.hash);

        // size >= set.size && set.all? { |o| include?(o) }
        return asBoolean(context, size() > set.size() && allElementsIncluded(set));
    }

    @JRubyMethod(name = "subset?", alias = { "<=" })
    public IRubyObject subset_p(final ThreadContext context, IRubyObject setArg) {
        if (!(setArg instanceof RubySet set)) throw argumentError(context, "value must be a set");
        if (getMetaClass().isInstance(set)) return this.hash.op_le(context, set.hash);

        // size >= set.size && set.all? { |o| include?(o) }
        return asBoolean(context, size() <= set.size() && allElementsIncluded(set));
    }

    @JRubyMethod(name = "proper_subset?", alias = { "<" })
    public IRubyObject proper_subset_p(final ThreadContext context, IRubyObject setArg) {
        if (!(setArg instanceof RubySet set)) throw argumentError(context, "value must be a set");
        if (getMetaClass().isInstance(set)) return this.hash.op_lt(context, set.hash);

        // size >= set.size && set.all? { |o| include?(o) }
        return asBoolean(context, size() < set.size() && allElementsIncluded(set));
    }

    /**
     * Returns true if the set and the given set have at least one element in common.
     */
    @JRubyMethod(name = "intersect?")
    public IRubyObject intersect_p(final ThreadContext context, IRubyObject setArg) {
        if (!(setArg instanceof RubySet set)) throw argumentError(context, "value must be a set");

        return asBoolean(context, intersect(set));
    }

    public boolean intersect(final RubySet set) {
        if ( size() < set.size() ) {
            // any? { |o| set.include?(o) }
            for ( IRubyObject o : elementsOrdered() ) {
                if ( set.containsImpl(o) ) return true;
            }
        } else {
            // set.any? { |o| include?(o) }
            for ( IRubyObject o : set.elementsOrdered() ) {
                if ( containsImpl(o) ) return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the set and the given set have no element in common.
     * This method is the opposite of +intersect?+.
     */
    @JRubyMethod(name = "disjoint?")
    public IRubyObject disjoint_p(final ThreadContext context, IRubyObject setArg) {
        if (!(setArg instanceof RubySet set)) throw argumentError(context, "value must be a set");

        return asBoolean(context,  !intersect(set));
    }

    @JRubyMethod
    public IRubyObject each(final ThreadContext context, Block block) {
        if (!block.isGiven()) return enumeratorizeWithSize(context, this, "each", RubySet::size);

        for (IRubyObject elem : elementsOrdered()) block.yield(context, elem);
        return this;
    }

    /**
     * A size method suitable for lambda method reference implementation of {@link SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])}
     *
     * @see SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])
     */
    private static IRubyObject size(ThreadContext context, RubySet recv, IRubyObject[] args) {
        return asFixnum(context, recv.size());
    }

    /**
     * Adds the given object to the set and returns self.
     */
    @JRubyMethod(name = "add", alias = "<<")
    public RubySet add(final ThreadContext context, IRubyObject obj) {
        modifyCheck(context);
        addImpl(context, obj);
        return this;
    }

    @Deprecated(since = "10.0")
    protected void addImpl(final Ruby runtime, final IRubyObject obj) {
        addImpl(runtime.getCurrentContext(), obj);
    }

    protected void addImpl(final ThreadContext context, final IRubyObject obj) {
        hash.fastASetCheckString(context.runtime, obj, context.tru); // @hash[obj] = true
    }

    protected void addImplSet(final ThreadContext context, final RubySet set) {
        // NOTE: MRI cheats - does not call Set#add thus we do not care ...
        hash.merge_bang(context, new IRubyObject[]{set.hash}, Block.NULL_BLOCK);
    }

    /**
     * Adds the given object to the set and returns self.  If the object is already in the set, returns nil.
     */
    @JRubyMethod(name = "add?")
    public IRubyObject add_p(final ThreadContext context, IRubyObject obj) {
        // add(o) unless include?(o)
        if ( containsImpl(obj) ) return context.nil;
        return add(context, obj);
    }

    @JRubyMethod
    public IRubyObject delete(final ThreadContext context, IRubyObject obj) {
        modifyCheck(context);
        deleteImpl(obj);
        return this;
    }

    protected boolean deleteImpl(final IRubyObject obj) {
        hash.modify();
        return hash.fastDelete(obj);
    }

    protected void deleteImplIterator(final IRubyObject obj, final Iterator it) {
        it.remove();
    }

    /**
     * Deletes the given object from the set and returns self.  If the object is not in the set, returns nil.
     */
    @JRubyMethod(name = "delete?")
    public IRubyObject delete_p(final ThreadContext context, IRubyObject obj) {
        // delete(o) if include?(o)
        if ( ! containsImpl(obj) ) return context.nil;
        return delete(context, obj);
    }

    @JRubyMethod
    public IRubyObject delete_if(final ThreadContext context, Block block) {
        if ( ! block.isGiven() ) {
            return enumeratorizeWithSize(context, this, "delete_if", RubySet::size);
        }

        Iterator<IRubyObject> it = elementsOrdered().iterator();
        while ( it.hasNext() ) {
            IRubyObject elem = it.next();
            if ( block.yield(context, elem).isTrue() ) deleteImplIterator(elem, it); // it.remove
        }
        return this;
    }

    @JRubyMethod
    public IRubyObject keep_if(final ThreadContext context, Block block) {
        if ( ! block.isGiven() ) {
            return enumeratorizeWithSize(context, this, "keep_if", RubySet::size);
        }

        Iterator<IRubyObject> it = elementsOrdered().iterator();
        while ( it.hasNext() ) {
            IRubyObject elem = it.next();
            if ( ! block.yield(context, elem).isTrue() ) deleteImplIterator(elem, it); // it.remove
        }
        return this;
    }

    @JRubyMethod(name = "collect!", alias = "map!")
    public IRubyObject collect_bang(final ThreadContext context, Block block) {
        if (!block.isGiven()) return enumeratorizeWithSize(context, this, "collect!", RubySet::size);

        final RubyArray elems = to_a(context); clearImpl(context);
        for ( int i=0; i<elems.size(); i++ ) {
            addImpl(context, block.yield(context, elems.eltInternal(i)));
        }
        return this;
    }

    // Equivalent to Set#delete_if, but returns nil if no changes were made.
    @JRubyMethod(name = "reject!")
    public IRubyObject reject_bang(final ThreadContext context, Block block) {
        if (!block.isGiven()) return enumeratorizeWithSize(context, this, "reject!", RubySet::size);

        final int size = size();
        for(Iterator<IRubyObject> it = elementsOrdered().iterator(); it.hasNext(); ) {
            IRubyObject elem = it.next();
            if (block.yield(context, elem).isTrue()) deleteImplIterator(elem, it); // it.remove
        }
        return size == size() ? context.nil : this;
    }

    // Equivalent to Set#keep_if, but returns nil if no changes were made.
    @JRubyMethod(name = "select!", alias = "filter!")
    public IRubyObject select_bang(final ThreadContext context, Block block) {
        if ( ! block.isGiven() ) {
            return enumeratorizeWithSize(context, this, "select!", RubySet::size);
        }

        final int size = size();
        Iterator<IRubyObject> it = elementsOrdered().iterator();
        while ( it.hasNext() ) {
            IRubyObject elem = it.next();
            if ( ! block.yield(context, elem).isTrue() ) deleteImplIterator(elem, it); // it.remove
        }
        return size == size() ? context.nil : this;
    }

    public RubySet rb_merge(final ThreadContext context, IRubyObject enume) {
        return rb_merge(context, new IRubyObject[] { enume});
    }

    /**
     * Merges the elements of the given enumerable object to the set and returns self.
     */
    @JRubyMethod(name = "merge", required=1, rest=true)
    public RubySet rb_merge(final ThreadContext context, IRubyObject... args) {
        var length = args.length;
        for (int i = 0; i < length; i++) {
            var arg = args[i];
            if (arg instanceof RubySet set) {
                modifyCheck(context);
                addImplSet(context, set);
            } else if (arg instanceof RubyArray ary) {
                modifyCheck(context);
                for ( int j = 0; j < ary.size(); j++ ) {
                    addImpl(context, ary.eltInternal(j));
                }
            } else { // do_with_enum(enum) { |o| add(o) }
                doWithEnum(context, arg, new EachBody(context) {
                    IRubyObject yieldImpl(ThreadContext context, IRubyObject val) {
                        addImpl(context, val); return context.nil;
                    }
                });
            }
        }

        return this;
    }

    /**
     * Deletes every element that appears in the given enumerable object and returns self.
     */
    @JRubyMethod(name = "subtract")
    public IRubyObject subtract(final ThreadContext context, IRubyObject enume) {
        if (enume instanceof RubySet set) {
            modifyCheck(context);
            for (IRubyObject elem : set.elementsOrdered()) {
                deleteImpl(elem);
            }
        } else if (enume instanceof RubyArray ary) {
            modifyCheck(context);
            for ( int i = 0; i < ary.size(); i++ ) {
                deleteImpl(ary.eltInternal(i));
            }
        } else { // do_with_enum(enum) { |o| delete(o) }
            doWithEnum(context, enume, new EachBody(context) {
                IRubyObject yieldImpl(ThreadContext context, IRubyObject val) {
                    deleteImpl(val); return context.nil;
                }
            });
        }

        return this;
    }

    /**
     * Returns a new set built by merging the set and the elements of the given enumerable object.
     */
    @JRubyMethod(name = "|", alias = { "+", "union" })
    public IRubyObject op_or(final ThreadContext context, IRubyObject enume) {
        return ((RubySet) dup()).rb_merge(context, enume); // dup.merge(enum)
    }

    /**
     * Returns a new set built by duplicating the set, removing every element that appears in the given enumerable object.
     */
    @JRubyMethod(name = "-", alias = { "difference" })
    public IRubyObject op_diff(final ThreadContext context, IRubyObject enume) {
        return ((RubySet) dup()).subtract(context, enume);
    }

    /**
     * Returns a new set built by merging the set and the elements of the given enumerable object.
     */
    @JRubyMethod(name = "&", alias = { "intersection" })
    public IRubyObject op_and(final ThreadContext context, IRubyObject enume) {
        final RubySet newSet = new RubySet(context.runtime, getMetaClass());
        if (enume instanceof RubySet set) {
            newSet.allocHash(context, set.size());
            for ( IRubyObject obj : set.elementsOrdered() ) {
                if (containsImpl(obj)) newSet.addImpl(context, obj);
            }
        } else if (enume instanceof RubyArray ary) {
            newSet.allocHash(context, ary.size());
            for ( int i = 0; i < ary.size(); i++ ) {
                final IRubyObject obj = ary.eltInternal(i);
                if (containsImpl(obj)) newSet.addImpl(context, obj);
            }
        } else {
            newSet.allocHash(context);
            // do_with_enum(enum) { |o| newSet.add(o) if include?(o) }
            doWithEnum(context, enume, new EachBody(context) {
                IRubyObject yieldImpl(ThreadContext context, IRubyObject obj) {
                    if (containsImpl(obj)) newSet.addImpl(context, obj);
                    return context.nil;
                }
            });
        }

        return newSet;
    }

    /**
     * Returns a new set containing elements exclusive between the set and the given enumerable object.
     * `(set ^ enum)` is equivalent to `((set | enum) - (set &amp; enum))`.
     */
    @JRubyMethod(name = "^")
    public IRubyObject op_xor(final ThreadContext context, IRubyObject enume) {
        RubySet newSet = new RubySet(context.runtime, Access.getClass(context, "Set"));
        newSet.initialize(context, enume, Block.NULL_BLOCK); // Set.new(enum)
        for (IRubyObject o : elementsOrdered()) {
            if (newSet.containsImpl(o)) {
                newSet.deleteImpl(o); // exclusive or
            } else {
                newSet.addImpl(context, o);
            }
        }

        return newSet;
    }

    @Override
    @JRubyMethod(name = "==")
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
        if (this == other) return context.tru;
        if (getMetaClass().isInstance(other)) {
            return hash.op_equal(context, ((RubySet) other).hash); // @hash == ...
        }
        if (other instanceof RubySet that) {
            if (size() == that.size()) { // && includes all of our elements :
                for (IRubyObject obj: elementsOrdered()) {
                    if (!that.containsImpl(obj)) return context.fals;
                }
                return context.tru;
            }
        }
        return context.fals;
    }

    @JRubyMethod(name = "reset")
    public IRubyObject reset(ThreadContext context) {
        this.hash.rehash(context);
        return this;
    }

    @JRubyMethod(name = "eql?")
    public IRubyObject op_eql(ThreadContext context, IRubyObject other) {
        if ( other instanceof RubySet ) {
            return this.hash.op_eql(context, ((RubySet) other).hash);
        }
        return context.fals;
    }

    @Override
    public boolean eql(IRubyObject otherArg) {
        if ( otherArg instanceof RubySet set) {
            final ThreadContext context = getRuntime().getCurrentContext();
            return hash.op_eql(context, set.hash) == context.tru;
        }
        return false;
    }

    @JRubyMethod
    public RubyFixnum hash(ThreadContext context) { // @hash.hash
        RubyHash hash = this.hash;

        return hash == null ?
                ((RubyBasicObject) context.nil).hash(context) :  // Emulate set.rb for jruby/jruby#8393
                hash.hash(context);
    }

    @JRubyMethod(name = "classify")
    public IRubyObject classify(ThreadContext context, final Block block) {
        if (!block.isGiven()) return enumeratorizeWithSize(context, this, "classify", RubySet::size);

        final RubyHash h = new RubyHash(context.runtime, size());

        for ( IRubyObject i : elementsOrdered() ) {
            final IRubyObject key = block.yield(context, i);
            RubySet set = (RubySet) h.fastARef(key);
            if (set == null) {
                set = newSetFast(context.runtime);
                h.fastASet(key, set);
            }
            set.invokeAdd(context, i);
        }

        return h;
    }

    /**
      * Divides the set into a set of subsets according to the commonality
      * defined by the given block.
      *
      * If the arity of the block is 2, elements o1 and o2 are in common
      * if block.call(o1, o2) is true.  Otherwise, elements o1 and o2 are
      * in common if block.call(o1) == block.call(o2).
      *
      * e.g.:
      *
      *   require 'set'
      *   numbers = Set[1, 3, 4, 6, 9, 10, 11]
      *   set = numbers.divide { |i,j| (i - j).abs == 1 }
      *   p set     # =&gt; #&lt;Set: {#&lt;Set: {1}&gt;,
      *             #            #&lt;Set: {11, 9, 10}&gt;,
      *             #            #&lt;Set: {3, 4}&gt;,
      *             #            #&lt;Set: {6}&gt;}&gt;
      */
    @JRubyMethod(name = "divide")
    public IRubyObject divide(ThreadContext context, final Block block) {
        if (!block.isGiven()) return enumeratorizeWithSize(context, this, "divide", RubySet::size);

        if (block.getSignature().arityValue() == 2) return divideTSort(context, block);

        RubyHash vals = (RubyHash) classify(context, block);
        final RubySet set = new RubySet(context.runtime, Access.getClass(context, "Set"));
        set.allocHash(context, vals.size());
        for ( IRubyObject val : (Collection<IRubyObject>) vals.directValues() ) {
            set.invokeAdd(context, val);
        }
        return set;
    }

    private IRubyObject divideTSort(ThreadContext context, final Block block) {
        final RubyHash dig = DivideTSortHash.newInstance(context);

        /*
          each { |u|
            dig[u] = a = []
            each{ |v| func.call(u, v) and a << v }
          }
         */
        for ( IRubyObject u : elementsOrdered() ) {
            var a = newArray(context);
            dig.fastASet(u, a);
            for ( IRubyObject v : elementsOrdered() ) {
                IRubyObject ret = block.call(context, u, v);
                if ( ret.isTrue() ) a.append(context, v);
            }
        }

        /*
          set = Set.new()
          dig.each_strongly_connected_component { |css|
            set.add(self.class.new(css))
          }
          set
         */
        final RubyClass Set = Access.getClass(context, "Set");
        final RubySet set = new RubySet(context.runtime, Set);
        set.allocHash(context, dig.size());
        sites(context).each_strongly_connected_component.call(context, this, dig, new Block(
            new JavaInternalBlockBody(context.runtime, Signature.ONE_REQUIRED) {
                @Override
                public IRubyObject yield(ThreadContext context, IRubyObject[] args) {
                    return doYield(context, null, args[0]);
                }

                @Override
                protected IRubyObject doYield(ThreadContext context, Block block, IRubyObject css) {
                    // set.add( self.class.new(css) ) :
                    set.addImpl(context, newSet(context, Set, (RubyArray) css));
                    return context.nil;
                }
            })
        );

        return set;
    }

    // NOTE: a replacement for set.rb's eval in Set#divide : `class << dig = {} ...`
    public static final class DivideTSortHash extends RubyHash {

        private static final String NAME = "DivideTSortHash"; // private constant under Set::

        static DivideTSortHash newInstance(final ThreadContext context) {
            RubyClass Set = Access.getClass(context, "Set");
            RubyClass klass = (RubyClass) Set.getConstantAt(context, NAME, true);
            if (klass == null) { // initialize on-demand when Set#divide is first called
                synchronized (DivideTSortHash.class) {
                    klass = (RubyClass) Set.getConstantAt(context, NAME, true);
                    if (klass == null) {
                        var Hash = hashClass(context);
                        klass = Set.defineClassUnder(context, NAME, Hash, Hash.getAllocator()).
                                include(context, getTSort(context)).
                                defineMethods(context, DivideTSortHash.class);
                        Set.setConstantVisibility(context, NAME, true); // private
                    }
                }
            }
            return new DivideTSortHash(context.runtime, klass);
        }

        DivideTSortHash(final Ruby runtime, final RubyClass metaClass) {
            super(runtime, metaClass);
        }

        /*
         class << dig = {}         # :nodoc:
           include TSort

           alias tsort_each_node each_key
           def tsort_each_child(node, &block)
             fetch(node).each(&block)
           end
         end
         */

        @JRubyMethod
        public IRubyObject tsort_each_node(ThreadContext context, Block block) {
            return each_key(context, block);
        }

        @JRubyMethod
        public IRubyObject tsort_each_child(ThreadContext context, IRubyObject node, Block block) {
            IRubyObject set = fetch(context, node, Block.NULL_BLOCK);
            if ( set instanceof RubySet ) {
                return ((RubySet) set).each(context, block);
            }
            // some Enumerable (we do not expect this to happen)
            return sites(context).each.call(context, this, set, block);
        }

    }

    @JRubyMethod(name = "<=>")
    public IRubyObject op_cmp(ThreadContext context, IRubyObject other) {
        /*
          def <=>(set)
            return unless set.is_a?(Set)

            case size <=> set.size
            when -1 then -1 if proper_subset?(set)
            when +1 then +1 if proper_superset?(set)
            else 0 if self.==(set)
            end
          end
         */
        if (!(other instanceof RubySet)) return context.nil;

        RubySet otherSet = (RubySet) other;

        int size = size();
        int otherSize = otherSet.size();

        if (size < otherSize) {
            if (sites(context).proper_subset.call(context, this, this, other).isTrue()) {
                return RubyFixnum.minus_one(context.runtime);
            }
        } else if (size > otherSize){
            if (sites(context).proper_superset.call(context, this, this, other).isTrue()) {
                return RubyFixnum.one(context.runtime);
            }
        } else {
            if (sites(context).op_equal.call(context, this, this, other).isTrue()) {
                return RubyFixnum.zero(context.runtime);
            }
        }

        return context.nil;
    }

    @JRubyMethod(name = "join")
    public IRubyObject join(ThreadContext context, IRubyObject sep) {
        return sites(context).ary_join.call(
                context,
                this,
                sites(context).to_a.call(context, this, this),
                sep);
    }

    @JRubyMethod(name = "join")
    public IRubyObject join(ThreadContext context) {
        return join(context, context.nil);
    }

    static RubyModule getTSort(ThreadContext context) {
        if (!objectClass(context).hasConstant("TSort")) loadService(context).require("tsort");

        return getModule(context, "TSort");
    }

    private static final byte[] RECURSIVE_BYTES = new byte[] { '.','.','.' };

    // Returns a string containing a human-readable representation of the set.
    // e.g. "#<Set: {element1, element2, ...}>"
    @JRubyMethod(name = "inspect", alias = "to_s")
    public RubyString inspect(ThreadContext context) {
        if (size() == 0) return inspectEmpty(context);
        if (context.runtime.isInspecting(this)) return inspectRecurse(context);

        RubyString str = RubyString.newStringLight(context.runtime, 32, USASCIIEncoding.INSTANCE);
        inspectPrefix(context, str, getMetaClass());

        try {
            context.runtime.registerInspecting(this);
            inspectSet(context, str);
            return str.cat('>');
        } finally {
            context.runtime.unregisterInspecting(this);
        }
    }

    private RubyString inspectEmpty(ThreadContext context) {
        RubyString str = RubyString.newStringLight(context.runtime, 16, USASCIIEncoding.INSTANCE);
        inspectPrefix(context, str, getMetaClass());
        str.cat('{').cat('}').cat('>'); // "#<Set: {}>"
        return str;
    }

    private RubyString inspectRecurse(ThreadContext context) {
        RubyString str = RubyString.newStringLight(context.runtime, 20, USASCIIEncoding.INSTANCE);
        inspectPrefix(context, str, getMetaClass());
        str.cat('{').cat(RECURSIVE_BYTES).cat('}').cat('>'); // "#<Set: {...}>"
        return str;
    }

    private static RubyString inspectPrefix(ThreadContext context, final RubyString str, final RubyClass metaClass) {
        str.cat('#').cat('<').cat(metaClass.getRealClass().getName(context).getBytes(RubyEncoding.UTF8));
        str.cat(':').cat(' ');
        return str;
    }

    private void inspectSet(final ThreadContext context, final RubyString str) {

        str.cat((byte) '{');

        boolean notFirst = false;

        for ( IRubyObject elem : elementsOrdered() ) {
            final RubyString s = inspect(context, elem);
            if ( notFirst ) str.cat((byte) ',').cat((byte) ' ');
            else str.setEncoding( s.getEncoding() ); notFirst = true;
            str.catWithCodeRange(s);
        }

        str.cat((byte) '}');
    }

    // pp (in __jruby/set.rb__)

    //@JRubyMethod
    //public IRubyObject pretty_print_cycle(ThreadContext context, final IRubyObject pp) {
    //    RubyString str = isEmpty() ? inspectEmpty(context.runtime) : inspectRecurse(context.runtime);
    //    return pp.callMethod(context, "text", str); // pp.text ...
    //}

    protected final Set<IRubyObject> elements() {
        return hash.directKeySet(); // Hash view -> no copying
    }

    // NOTE: implementation does not expect to be used for altering contents using iterator
    protected Set<IRubyObject> elementsOrdered() {
        return elements(); // potentially -> to be re-defined by SortedSet
    }

    @Deprecated
    protected final void modifyCheck(final Ruby runtime) {
        modifyCheck(runtime.getCurrentContext());
    }

    protected final void modifyCheck(final ThreadContext context) {
        if ((flags & FROZEN_F) != 0) throw context.runtime.newFrozenError("Set", this);
    }

    // java.util.Set

    public int size() { return hash.size(); }

    public boolean isEmpty() { return hash.isEmpty(); }

    // FIXME: How do we obey Set#clear() but not access runtime?  Probably make special path into hash which does unsafe clear
    public void clear() {
        clear(getRuntime().getCurrentContext());
    }

    public void clear(ThreadContext context) {
        clearImpl(context);
    }

    public boolean contains(Object o) {
        return containsImpl(toRuby(o));
    }

    public Iterator<IRubyObject> rawIterator() {
        return elementsOrdered().iterator();
    }

    public Iterator<Object> iterator() {
        return hash.keySet().iterator();
    }

    public Object[] toArray() {
        Object[] array = new Object[size()]; int i = 0;
        for ( IRubyObject elem : elementsOrdered() ) {
            array[i++] = elem.toJava(Object.class);
        }
        return array;
    }

    public Object[] toArray(final Object[] ary) {
        final Class type = ary.getClass().getComponentType();
        Object[] array = ary;
        if (array.length < size()) {
            array = (Object[]) Array.newInstance(type, size());
        }

        int i = 0;
        for ( IRubyObject elem : elementsOrdered() ) {
            array[i++] = elem.toJava(type);
        }
        return array;
    }

    public boolean add(Object element) {
        final Ruby runtime = getRuntime();
        final int size = size();
        addImpl(runtime.getCurrentContext(), toRuby(runtime, element));
        return size() > size; // if added
    }

    public boolean remove(Object element) {
        return deleteImpl(toRuby(element));
    }

    public boolean containsAll(Collection coll) {
        for ( Object elem : coll ) {
            if ( ! contains(elem) ) return false;
        }
        return true;
    }

    public boolean addAll(Collection coll) {
        final Ruby runtime = getRuntime();
        ThreadContext context = runtime.getCurrentContext();
        final int size = size();
        for (Object elem: coll) {
            addImpl(context, toRuby(runtime, elem));
        }
        return size() > size; // if added
    }

    public boolean retainAll(Collection coll) {
        final int size = size();
        for (Iterator<IRubyObject> iter = rawIterator(); iter.hasNext();) {
            IRubyObject elem = iter.next();
            if ( ! coll.contains(elem.toJava(Object.class)) ) {
                deleteImplIterator(elem, iter);
            }
        }
        return size() < size;
    }

    public boolean removeAll(Collection coll) {
        boolean removed = false;
        for ( Object elem : coll ) {
            removed = remove(elem) | removed;
        }
        return removed;
    }

    static IRubyObject toRuby(Ruby runtime, Object obj) {
        return JavaUtil.convertJavaToUsableRubyObject(runtime, obj);
    }

    final IRubyObject toRuby(Object obj) {
        return JavaUtil.convertJavaToUsableRubyObject(getRuntime(), obj);
    }

    @Deprecated
    @Override
    @JRubyMethod
    public IRubyObject taint(ThreadContext context) {
        return this;
    }

    @Deprecated
    @Override
    @JRubyMethod
    public IRubyObject untaint(ThreadContext context) {
        return this;
    }

    private static JavaSites.SetSites sites(ThreadContext context) {
        return context.sites.Set;
    }

}
