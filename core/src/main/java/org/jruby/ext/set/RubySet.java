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
import org.jruby.common.IRubyWarnings;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.ArraySupport;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Set;

import static org.jruby.RubyEnumerator.enumeratorizeWithSize;

/**
 * Native implementation of Ruby's Set (set.rb replacement).
 *
 * @author kares
 */
@org.jruby.anno.JRubyClass(name="Set", include = { "Enumerable" })
public class RubySet extends RubyObject implements Set {

    static RubyClass createSetClass(final Ruby runtime) {
        RubyClass Set = runtime.defineClass("Set", runtime.getObject(), RubySet::new);

        Set.setReifiedClass(RubySet.class);

        Set.includeModule(runtime.getEnumerable());
        Set.defineAnnotatedMethods(RubySet.class);

        Set.setMarshal(new SetMarshal(Set.getMarshal()));

        runtime.getLoadService().require("jruby/set.rb");

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

    final void allocHash(final Ruby runtime) {
        setHash(new RubyHash(runtime, runtime.getFalse()));
    }

    final void allocHash(final Ruby runtime, final int size) {
        setHash(new RubyHash(runtime, runtime.getFalse(), size));
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
        allocHash(context.runtime, Math.max(4, len));
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
     * initialize(enum = nil, &block)
     */
    @JRubyMethod(visibility = Visibility.PRIVATE) // def initialize(enum = nil, &block)
    public IRubyObject initialize(ThreadContext context, Block block) {
        if ( block.isGiven() && context.runtime.isVerbose() ) {
            context.runtime.getWarnings().warning(IRubyWarnings.ID.BLOCK_UNUSED, "given block not used");
        }
        allocHash(context.runtime);
        return this;
    }

    /**
     * initialize(enum = nil, &block)
     */
    @JRubyMethod(required = 1, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject enume, Block block) {
        if ( enume.isNil() ) return initialize(context, block);

        if ( block.isGiven() ) {
            return initWithEnum(context, enume, block);
        }

        allocHash(context.runtime);
        return callMethod(context, "merge", enume); // TODO site-cache
    }

    protected IRubyObject initialize(ThreadContext context, IRubyObject[] args, Block block) {
        switch (args.length) {
            case 0: return initialize(context, block);
            case 1: return initialize(context, args[0], block);
        }
        throw context.runtime.newArgumentError(args.length, 1);
    }

    private IRubyObject initWithEnum(final ThreadContext context, final IRubyObject enume, final Block block) {
        if ( enume instanceof RubyArray ) {
            RubyArray ary = (RubyArray) enume;
            allocHash(context.runtime, ary.size());
            for ( int i = 0; i < ary.size(); i++ ) {
                invokeAdd(context, block.yield(context, ary.eltInternal(i)));
            }
            return ary; // done
        }

        if ( enume instanceof RubySet ) {
            RubySet set = (RubySet) enume;
            allocHash(context.runtime, set.size());
            for ( IRubyObject elem : set.elementsOrdered() ) {
                invokeAdd(context, block.yield(context, elem));
            }
            return set; // done
        }

        final Ruby runtime = context.runtime;

        allocHash(runtime);

        // set.rb do_with_enum :
        return doWithEnum(context, enume, new EachBody(runtime) {
            IRubyObject yieldImpl(ThreadContext context, IRubyObject val) {
                return invokeAdd(context, block.yield(context, val));
            }
        });
    }

    // set.rb do_with_enum (block is required)
    private static IRubyObject doWithEnum(final ThreadContext context, final IRubyObject enume, final EachBody blockImpl) {
        if ( enume.respondsTo("each_entry") ) {
            return enume.callMethod(context, "each_entry", IRubyObject.NULL_ARRAY, new Block(blockImpl));
        }
        if ( enume.respondsTo("each") ) {
            return enume.callMethod(context, "each", IRubyObject.NULL_ARRAY, new Block(blockImpl));
        }

        throw context.runtime.newArgumentError("value must be enumerable");
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
        return this.callMethod(context,"add", val); // TODO site-cache
    }

    private static abstract class EachBody extends JavaInternalBlockBody {

        EachBody(final Ruby runtime) {
            super(runtime, Signature.ONE_ARGUMENT);
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

    @JRubyMethod
    public IRubyObject initialize_dup(ThreadContext context, IRubyObject orig) {
        super.initialize_copy(orig);
        setHash((RubyHash) (((RubySet) orig).hash).dup(context));
        return this;
    }

    @JRubyMethod
    public IRubyObject initialize_clone(ThreadContext context, IRubyObject orig) {
        super.initialize_copy(orig);
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

    @Override
    @JRubyMethod
    public IRubyObject taint(ThreadContext context) {
        final RubyHash hash = this.hash;
        if ( hash != null ) hash.taint(context);
        return super.taint(context);
    }

    @Override
    @JRubyMethod
    public IRubyObject untaint(ThreadContext context) {
        final RubyHash hash = this.hash;
        if ( hash != null ) hash.untaint(context);
        return super.untaint(context);
    }

    @JRubyMethod(name = "size", alias = "length")
    public IRubyObject length(ThreadContext context) {
        return context.runtime.newFixnum( size() );
    }

    @JRubyMethod(name = "empty?")
    public IRubyObject empty_p(ThreadContext context) {
        return RubyBoolean.newBoolean(context,  isEmpty() );
    }

    @JRubyMethod(name = "clear")
    public IRubyObject rb_clear(ThreadContext context) {
        modifyCheck(context.runtime);

        clearImpl();
        return this;
    }

    protected void clearImpl() {
        hash.rb_clear(getRuntime().getCurrentContext());
    }

    /**
     * Replaces the contents of the set with the contents of the given enumerable object and returns self.
     */
    @JRubyMethod
    public RubySet replace(final ThreadContext context, IRubyObject enume) {
        if ( enume instanceof RubySet ) {
            modifyCheck(context.runtime);
            clearImpl();
            addImplSet(context, (RubySet) enume);
        }
        else {
            final Ruby runtime = context.runtime;
            // do_with_enum(enum)  # make sure enum is enumerable before calling clear :
            if ( ! enume.getMetaClass().hasModuleInHierarchy(runtime.getEnumerable()) ) {
                // NOTE: likely no need to do this but due MRI compat (do_with_enum) :
                if ( ! enume.respondsTo("each_entry") ) {
                    throw runtime.newArgumentError("value must be enumerable");
                }
            }
            clearImpl();
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

        final Ruby runtime = context.runtime;

        IRubyObject klass = args[0]; final RubyClass Set = runtime.getClass("Set");

        if ( klass == Set && args.length == 1 & ! block.isGiven() ) {
            return this;
        }

        final IRubyObject[] rest;
        if ( klass instanceof RubyClass ) {
            rest = ArraySupport.newCopy(args, 1, args.length - 1);
        }
        else {
            klass = Set; rest = args;
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

    private void flattenMerge(final ThreadContext context, final IRubyObject set, final IdentityHashMap seen) {
        if ( set instanceof RubySet ) {
            for ( IRubyObject e : ((RubySet) set).elementsOrdered() ) {
                addFlattened(context, seen, e);
            }
        }
        else {
            set.callMethod(context, "each", IRubyObject.NULL_ARRAY, new Block(
                new EachBody(context.runtime) {
                    IRubyObject yieldImpl(ThreadContext context, IRubyObject e) {
                        addFlattened(context, seen, e); return context.nil;
                    }
                })
            );
        }
    }

    private void addFlattened(final ThreadContext context, final IdentityHashMap seen, IRubyObject e) {
        if ( e instanceof RubySet ) {
            if ( seen.containsKey(e) ) {
                throw context.runtime.newArgumentError("tried to flatten recursive Set");
            }
            seen.put(e, null);
            flattenMerge(context, e, seen);
            seen.remove(e);
        }
        else {
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
        return RubyBoolean.newBoolean(context,  containsImpl(obj) );
    }

    final boolean containsImpl(IRubyObject obj) {
        return hash.fastARef(obj) != null;
    }

    private boolean allElementsIncluded(final RubySet set) {
        for ( IRubyObject o : set.elements() ) { // set.all? { |o| include?(o) }
            if ( ! containsImpl(o) ) return false;
        }
        return true;
    }

    // Returns true if the set is a superset of the given set.
    @JRubyMethod(name = "superset?", alias = { ">=" })
    public IRubyObject superset_p(final ThreadContext context, IRubyObject set) {
        if ( set instanceof RubySet ) {
            if ( getMetaClass().isInstance(set) ) {
                return this.hash.op_ge(context, ((RubySet) set).hash);
            }
            // size >= set.size && set.all? { |o| include?(o) }
            return RubyBoolean.newBoolean(context,
                    size() >= ((RubySet) set).size() && allElementsIncluded((RubySet) set)
            );
        }
        throw context.runtime.newArgumentError("value must be a set");
    }

    // Returns true if the set is a proper superset of the given set.
    @JRubyMethod(name = "proper_superset?", alias = { ">" })
    public IRubyObject proper_superset_p(final ThreadContext context, IRubyObject set) {
        if ( set instanceof RubySet ) {
            if ( getMetaClass().isInstance(set) ) {
                return this.hash.op_gt(context, ((RubySet) set).hash);
            }
            // size >= set.size && set.all? { |o| include?(o) }
            return RubyBoolean.newBoolean(context,
                    size() > ((RubySet) set).size() && allElementsIncluded((RubySet) set)
            );
        }
        throw context.runtime.newArgumentError("value must be a set");
    }

    @JRubyMethod(name = "subset?", alias = { "<=" })
    public IRubyObject subset_p(final ThreadContext context, IRubyObject set) {
        if ( set instanceof RubySet ) {
            if ( getMetaClass().isInstance(set) ) {
                return this.hash.op_le(context, ((RubySet) set).hash);
            }
            // size >= set.size && set.all? { |o| include?(o) }
            return RubyBoolean.newBoolean(context,
                    size() <= ((RubySet) set).size() && allElementsIncluded((RubySet) set)
            );
        }
        throw context.runtime.newArgumentError("value must be a set");
    }

    @JRubyMethod(name = "proper_subset?", alias = { "<" })
    public IRubyObject proper_subset_p(final ThreadContext context, IRubyObject set) {
        if ( set instanceof RubySet ) {
            if ( getMetaClass().isInstance(set) ) {
                return this.hash.op_lt(context, ((RubySet) set).hash);
            }
            // size >= set.size && set.all? { |o| include?(o) }
            return RubyBoolean.newBoolean(context,
                    size() < ((RubySet) set).size() && allElementsIncluded((RubySet) set)
            );
        }
        throw context.runtime.newArgumentError("value must be a set");
    }

    /**
     * Returns true if the set and the given set have at least one element in common.
     */
    @JRubyMethod(name = "intersect?")
    public IRubyObject intersect_p(final ThreadContext context, IRubyObject set) {
        if ( set instanceof RubySet ) {
            return RubyBoolean.newBoolean(context,  intersect((RubySet) set) );
        }
        throw context.runtime.newArgumentError("value must be a set");
    }

    public boolean intersect(final RubySet set) {
        if ( size() < set.size() ) {
            // any? { |o| set.include?(o) }
            for ( IRubyObject o : elementsOrdered() ) {
                if ( set.containsImpl(o) ) return true;
            }
        }
        else {
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
    public IRubyObject disjoint_p(final ThreadContext context, IRubyObject set) {
        if ( set instanceof RubySet ) {
            return RubyBoolean.newBoolean(context,  ! intersect((RubySet) set) );
        }
        throw context.runtime.newArgumentError("value must be a set");
    }

    @JRubyMethod
    public IRubyObject each(final ThreadContext context, Block block) {
        if ( ! block.isGiven() ) {
            return enumeratorizeWithSize(context, this, "each", RubySet::size);
        }

        for (IRubyObject elem : elementsOrdered()) block.yield(context, elem);
        return this;
    }

    /**
     * A size method suitable for lambda method reference implementation of {@link SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])}
     *
     * @see SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])
     */
    private static IRubyObject size(ThreadContext context, RubySet recv, IRubyObject[] args) {
        return context.runtime.newFixnum(recv.size());
    }

    /**
     * Adds the given object to the set and returns self.
     */
    @JRubyMethod(name = "add", alias = "<<")
    public RubySet add(final ThreadContext context, IRubyObject obj) {
        modifyCheck(context.runtime);
        addImpl(context.runtime, obj);
        return this;
    }

    protected void addImpl(final Ruby runtime, final IRubyObject obj) {
        hash.fastASetCheckString(runtime, obj, runtime.getTrue()); // @hash[obj] = true
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
        modifyCheck(context.runtime);
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
        if ( ! block.isGiven() ) {
            return enumeratorizeWithSize(context, this, "collect!", RubySet::size);
        }

        final RubyArray elems = to_a(context); clearImpl();
        for ( int i=0; i<elems.size(); i++ ) {
            addImpl(context.runtime, block.yield(context, elems.eltInternal(i)));
        }
        return this;
    }

    // Equivalent to Set#delete_if, but returns nil if no changes were made.
    @JRubyMethod(name = "reject!")
    public IRubyObject reject_bang(final ThreadContext context, Block block) {
        if ( ! block.isGiven() ) {
            return enumeratorizeWithSize(context, this, "reject!", RubySet::size);
        }

        final int size = size();
        Iterator<IRubyObject> it = elementsOrdered().iterator();
        while ( it.hasNext() ) {
            IRubyObject elem = it.next();
            if ( block.yield(context, elem).isTrue() ) deleteImplIterator(elem, it); // it.remove
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

    /**
     * Merges the elements of the given enumerable object to the set and returns self.
     */
    @JRubyMethod(name = "merge")
    public RubySet rb_merge(final ThreadContext context, IRubyObject enume) {
        final Ruby runtime = context.runtime;

        if ( enume instanceof RubySet ) {
            modifyCheck(runtime);
            addImplSet(context, (RubySet) enume);
        }
        else if ( enume instanceof RubyArray ) {
            modifyCheck(runtime);
            RubyArray ary = (RubyArray) enume;
            for ( int i = 0; i < ary.size(); i++ ) {
                addImpl(runtime, ary.eltInternal(i));
            }
        }
        else { // do_with_enum(enum) { |o| add(o) }
            doWithEnum(context, enume, new EachBody(runtime) {
                IRubyObject yieldImpl(ThreadContext context, IRubyObject val) {
                    addImpl(context.runtime, val); return context.nil;
                }
            });
        }

        return this;
    }

    /**
     * Deletes every element that appears in the given enumerable object and returns self.
     */
    @JRubyMethod(name = "subtract")
    public IRubyObject subtract(final ThreadContext context, IRubyObject enume) {
        final Ruby runtime = context.runtime;

        if ( enume instanceof RubySet ) {
            modifyCheck(runtime);
            for ( IRubyObject elem : ((RubySet) enume).elementsOrdered() ) {
                deleteImpl(elem);
            }
        }
        else if ( enume instanceof RubyArray ) {
            modifyCheck(runtime);
            RubyArray ary = (RubyArray) enume;
            for ( int i = 0; i < ary.size(); i++ ) {
                deleteImpl(ary.eltInternal(i));
            }
        }
        else { // do_with_enum(enum) { |o| delete(o) }
            doWithEnum(context, enume, new EachBody(runtime) {
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
        final Ruby runtime = context.runtime;

        final RubySet newSet = new RubySet(runtime, getMetaClass());
        if ( enume instanceof RubySet ) {
            newSet.allocHash(runtime, ((RubySet) enume).size());
            for ( IRubyObject obj : ((RubySet) enume).elementsOrdered() ) {
                if ( containsImpl(obj) ) newSet.addImpl(runtime, obj);
            }
        }
        else if ( enume instanceof RubyArray ) {
            RubyArray ary = (RubyArray) enume;
            newSet.allocHash(runtime, ary.size());
            for ( int i = 0; i < ary.size(); i++ ) {
                final IRubyObject obj = ary.eltInternal(i);
                if ( containsImpl(obj) ) newSet.addImpl(runtime, obj);
            }
        }
        else {
            newSet.allocHash(runtime);
            // do_with_enum(enum) { |o| newSet.add(o) if include?(o) }
            doWithEnum(context, enume, new EachBody(runtime) {
                IRubyObject yieldImpl(ThreadContext context, IRubyObject obj) {
                    if ( containsImpl(obj) ) newSet.addImpl(runtime, obj);
                    return context.nil;
                }
            });
        }

        return newSet;
    }

    /**
     * Returns a new set containing elements exclusive between the set and the given enumerable object.
     * `(set ^ enum)` is equivalent to `((set | enum) - (set & enum))`.
     */
    @JRubyMethod(name = "^")
    public IRubyObject op_xor(final ThreadContext context, IRubyObject enume) {
        final Ruby runtime = context.runtime;

        RubySet newSet = new RubySet(runtime, runtime.getClass("Set"));
        newSet.initialize(context, enume, Block.NULL_BLOCK); // Set.new(enum)
        for ( IRubyObject o : elementsOrdered() ) {
            if ( newSet.containsImpl(o) ) newSet.deleteImpl(o); // exclusive or
            else newSet.addImpl(runtime, o);
        }

        return newSet;
    }

    @Override
    @JRubyMethod(name = "==")
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
        if ( this == other ) return context.tru;
        if ( getMetaClass().isInstance(other) ) {
            return this.hash.op_equal(context, ((RubySet) other).hash); // @hash == ...
        }
        if ( other instanceof RubySet ) {
            RubySet that = (RubySet) other;
            if ( this.size() == that.size() ) { // && includes all of our elements :
                for ( IRubyObject obj : elementsOrdered() ) {
                    if ( ! that.containsImpl(obj) ) return context.fals;
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
    public boolean eql(IRubyObject other) {
        if ( other instanceof RubySet ) {
            final Ruby runtime = getRuntime();
            return this.hash.op_eql(runtime.getCurrentContext(), ((RubySet) other).hash) == runtime.getTrue();
        }
        return false;
    }

    @Override
    @JRubyMethod
    public RubyFixnum hash() { // @hash.hash
        return hash.hash();
    }

    @JRubyMethod(name = "classify")
    public IRubyObject classify(ThreadContext context, final Block block) {
        if ( ! block.isGiven() ) {
            return enumeratorizeWithSize(context, this, "classify", RubySet::size);
        }

        final Ruby runtime = context.runtime;

        final RubyHash h = new RubyHash(runtime, size());

        for ( IRubyObject i : elementsOrdered() ) {
            final IRubyObject key = block.yield(context, i);
            IRubyObject set;
            if ( ( set = h.fastARef(key) ) == null ) {
                h.fastASet(key, set = newSetFast(runtime));
            }
            ((RubySet) set).invokeAdd(context, i);
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
      *   p set     # => #<Set: {#<Set: {1}>,
      *             #            #<Set: {11, 9, 10}>,
      *             #            #<Set: {3, 4}>,
      *             #            #<Set: {6}>}>
      */
    @JRubyMethod(name = "divide")
    public IRubyObject divide(ThreadContext context, final Block block) {
        if ( ! block.isGiven() ) {
            return enumeratorizeWithSize(context, this, "divide", RubySet::size);
        }

        if ( block.getSignature().arityValue() == 2 ) {
            return divideTSort(context, block);
        }

        final Ruby runtime = context.runtime; // Set.new(classify(&func).values) :

        RubyHash vals = (RubyHash) classify(context, block);
        final RubySet set = new RubySet(runtime, runtime.getClass("Set"));
        set.allocHash(runtime, vals.size());
        for ( IRubyObject val : (Collection<IRubyObject>) vals.directValues() ) {
            set.invokeAdd(context, val);
        }
        return set;
    }

    private IRubyObject divideTSort(ThreadContext context, final Block block) {
        final Ruby runtime = context.runtime;

        final RubyHash dig = DivideTSortHash.newInstance(context);

        /*
          each { |u|
            dig[u] = a = []
            each{ |v| func.call(u, v) and a << v }
          }
         */
        for ( IRubyObject u : elementsOrdered() ) {
            RubyArray a;
            dig.fastASet(u, a = runtime.newArray());
            for ( IRubyObject v : elementsOrdered() ) {
                IRubyObject ret = block.call(context, u, v);
                if ( ret.isTrue() ) a.append(v);
            }
        }

        /*
          set = Set.new()
          dig.each_strongly_connected_component { |css|
            set.add(self.class.new(css))
          }
          set
         */
        final RubyClass Set = runtime.getClass("Set");
        final RubySet set = new RubySet(runtime, Set);
        set.allocHash(runtime, dig.size());
        dig.callMethod(context, "each_strongly_connected_component", IRubyObject.NULL_ARRAY, new Block(
            new JavaInternalBlockBody(runtime, Signature.ONE_REQUIRED) {
                @Override
                public IRubyObject yield(ThreadContext context, IRubyObject[] args) {
                    return doYield(context, null, args[0]);
                }

                @Override
                protected IRubyObject doYield(ThreadContext context, Block block, IRubyObject css) {
                    // set.add( self.class.new(css) ) :
                    set.addImpl(runtime, newSet(context, Set, (RubyArray) css));
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
            final Ruby runtime = context.runtime;

            RubyClass Set = runtime.getClass("Set");
            RubyClass klass = (RubyClass) Set.getConstantAt(NAME, true);
            if (klass == null) { // initialize on-demand when Set#divide is first called
                synchronized (DivideTSortHash.class) {
                    klass = (RubyClass) Set.getConstantAt(NAME, true);
                    if (klass == null) {
                        klass = Set.defineClassUnder(NAME, runtime.getHash(), runtime.getHash().getAllocator());
                        Set.setConstantVisibility(runtime, NAME, true); // private
                        klass.includeModule(getTSort(runtime));
                        klass.defineAnnotatedMethods(DivideTSortHash.class);
                    }
                }
            }
            return new DivideTSortHash(runtime, klass);
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
            return set.callMethod(context, "each", IRubyObject.NULL_ARRAY, block);
        }

    }

    static RubyModule getTSort(final Ruby runtime) {
        if ( ! runtime.getObject().hasConstant("TSort") ) {
            runtime.getLoadService().require("tsort");
        }
        return runtime.getModule("TSort");
    }

    @Override
    public final IRubyObject inspect() {
        return inspect(getRuntime().getCurrentContext());
    }

    private static final byte[] RECURSIVE_BYTES = new byte[] { '.','.','.' };

    // Returns a string containing a human-readable representation of the set.
    // e.g. "#<Set: {element1, element2, ...}>"
    @JRubyMethod(name = "inspect", alias = "to_s")
    public RubyString inspect(ThreadContext context) {
        final Ruby runtime = context.runtime;

        final RubyString str;

        if (size() == 0) {
            return inspectEmpty(runtime);
        }

        if (runtime.isInspecting(this)) {
            return inspectRecurse(runtime);
        }

        str = RubyString.newStringLight(runtime, 32, USASCIIEncoding.INSTANCE);
        inspectPrefix(str, getMetaClass());

        try {
            runtime.registerInspecting(this);
            inspectSet(context, str);
            return str.cat('>');
        }
        finally {
            runtime.unregisterInspecting(this);
        }
    }

    private RubyString inspectEmpty(final Ruby runtime) {
        RubyString str = RubyString.newStringLight(runtime, 16, USASCIIEncoding.INSTANCE);
        inspectPrefix(str, getMetaClass()); str.cat('{').cat('}').cat('>'); // "#<Set: {}>"
        return str;
    }

    private RubyString inspectRecurse(final Ruby runtime) {
        RubyString str = RubyString.newStringLight(runtime, 20, USASCIIEncoding.INSTANCE);
        inspectPrefix(str, getMetaClass());
        str.cat('{').cat(RECURSIVE_BYTES).cat('}').cat('>'); // "#<Set: {...}>"
        return str;
    }

    private static RubyString inspectPrefix(final RubyString str, final RubyClass metaClass) {
        str.cat('#').cat('<').cat(metaClass.getRealClass().getName().getBytes(RubyEncoding.UTF8));
        str.cat(':').cat(' '); return str;
    }

    private void inspectSet(final ThreadContext context, final RubyString str) {

        str.cat((byte) '{');

        boolean tainted = isTaint(); boolean notFirst = false;

        for ( IRubyObject elem : elementsOrdered() ) {
            final RubyString s = inspect(context, elem);
            if ( s.isTaint() ) tainted = true;
            if ( notFirst ) str.cat((byte) ',').cat((byte) ' ');
            else str.setEncoding( s.getEncoding() ); notFirst = true;
            str.cat19( s );
        }

        str.cat((byte) '}');

        if ( tainted ) str.setTaint(true);
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

    protected final void modifyCheck(final Ruby runtime) {
        if ((flags & FROZEN_F) != 0) throw runtime.newFrozenError("Set");
    }

    // java.util.Set

    public int size() { return hash.size(); }

    public boolean isEmpty() { return hash.isEmpty(); }

    public void clear() { clearImpl(); }

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
        addImpl(runtime, toRuby(runtime, element));
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
        final int size = size();
        for ( Object elem : coll ) {
            addImpl(runtime, toRuby(runtime, elem));
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

}
