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
 * Copyright (C) 2016 Karol Bucek
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

import org.jruby.*;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.*;

import static org.jruby.RubyArray.DefaultComparator;

/**
 * Native implementation of Ruby's SortedSet (set.rb replacement).
 *
 * @author kares
 */
@org.jruby.anno.JRubyClass(name="SortedSet", parent = "Set")
public class RubySortedSet extends RubySet implements SortedSet {

    static RubyClass createSortedSetClass(final Ruby runtime) {
        RubyClass SortedSet = runtime.defineClass("SortedSet", runtime.getClass("Set"), RubySortedSet::new);

        SortedSet.setReifiedClass(RubySortedSet.class);
        SortedSet.defineAnnotatedMethods(RubySortedSet.class);

        return SortedSet;
    }

    private static class OrderComparator extends DefaultComparator {

        private final Ruby runtime;

        OrderComparator(final Ruby runtime) {
            super(null); this.runtime = runtime;
        }

        @Override
        protected ThreadContext context() {
            return runtime.getCurrentContext();
        }

        // NOTE: need a custom impl so that we use special care when compare returns 0
        // this is required since/if we're using a TreeSet which assumes 0 -> eql == true

        public int compare(IRubyObject obj1, IRubyObject obj2) {
            final int cmp = super.compare(obj1, obj2);
            if (cmp == 0) {
                return equalInternal(context(), obj1, obj2) ? 0 : 1;
            }
            return cmp;
        }

    }

    private final TreeSet<IRubyObject> order;

    protected RubySortedSet(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
        order = new TreeSet<>(new OrderComparator(runtime));
    }

    @Override
    void unmarshal() {
        super.unmarshal();
        IRubyObject[] elems = hash.keys().toJavaArrayMaybeUnsafe();
        for ( int i=0; i<elems.length; i++ ) order.add( elems[i] );
    }

    @JRubyMethod(name = "[]", rest = true, meta = true) // re-def Set[] for SortedSet
    public static RubySortedSet create(final ThreadContext context, IRubyObject self, IRubyObject... ary) {
        final Ruby runtime = context.runtime;

        RubySortedSet set = new RubySortedSet(runtime, (RubyClass) self);
        return (RubySortedSet) set.initSet(context, ary, 0, ary.length);
    }

    @Override
    protected void addImpl(final Ruby runtime, final IRubyObject obj) {
        // NOTE: we're able to function without the check - comparator will raise ArgumentError
        //if ( ! obj.respondsTo("<=>") ) {
        //    throw runtime.newArgumentError("value must respond to <=>");
        //}
        super.addImpl(runtime, obj); // @hash[obj] = true
        order.add(obj);
    }

    @Override
    protected void addImplSet(final ThreadContext context, final RubySet set) {
        super.addImplSet(context, set);
        order.addAll(set.elements());
    }

    @Override
    protected boolean deleteImpl(final IRubyObject obj) {
        if ( super.deleteImpl(obj) ) {
            order.remove(obj); return true;
        }
        return false;
    }

    @Override
    protected void deleteImplIterator(final IRubyObject obj, final Iterator it) {
        super.deleteImpl(obj);
        // iterator over elementsOrdered()
        it.remove(); // order.remove(obj)
    }

    @Override
    protected void clearImpl() {
        hash.rb_clear(getRuntime().getCurrentContext());
        order.clear();
    }

    @JRubyMethod(name = "sort") // re-def Enumerable#sort
    public RubyArray sort(final ThreadContext context) {
        return RubyArray.newArray(context.runtime, order); // instead of this.hash.keys();
    }

    @Override
    public RubyArray to_a(final ThreadContext context) {
        return sort(context); // instead of this.hash.keys();
    }

    @Override
    public IRubyObject initialize_dup(ThreadContext context, IRubyObject orig) {
        super.initialize_dup(context, orig);
        if (this != orig) order.addAll(((RubySortedSet) orig).order);
        return this;
    }

    @Override
    public IRubyObject initialize_clone(ThreadContext context, IRubyObject orig) {
        super.initialize_clone(context, orig);
        if (this != orig) order.addAll(((RubySortedSet) orig).order);
        return this;
    }

    // NOTE: weirdly Set/SortedSet in Ruby do not have sort!

    @Override
    protected Set<IRubyObject> elementsOrdered() { return order; }

    @Override
    public Iterator<Object> iterator() {
        return new JavaIterator();
    }

    // java.util.SortedSet :

    public Comparator<? super IRubyObject> comparator() {
        return order.comparator();
    }

    public Object first() {
        return firstValue().toJava(Object.class);
    }

    public IRubyObject firstValue() {
        return order.first();
    }

    public Object last() {
        return lastValue().toJava(Object.class);
    }

    public IRubyObject lastValue() {
        return order.last();
    }

    public SortedSet headSet(Object toElement) {
        throw new UnsupportedOperationException("NOT IMPLEMENTED");
    }

    public SortedSet subSet(Object fromElement, Object toElement) {
        throw new UnsupportedOperationException("NOT IMPLEMENTED");
    }

    public SortedSet tailSet(Object fromElement) {
        throw new UnsupportedOperationException("NOT IMPLEMENTED");
    }

    public SortedSet<IRubyObject> rawHeadSet(IRubyObject toElement) {
        return order.headSet(toElement);
    }

    public SortedSet<IRubyObject> rawSubSet(IRubyObject fromElement, IRubyObject toElement) {
        return order.subSet(fromElement, toElement);
    }

    public SortedSet<IRubyObject> rawTailSet(IRubyObject fromElement) {
        return order.tailSet(fromElement);
    }

    private class JavaIterator implements Iterator<Object> {

        private final Iterator<IRubyObject> rawIterator;

        JavaIterator() {
            rawIterator = RubySortedSet.this.order.iterator();
        }

        @Override
        public boolean hasNext() {
            return rawIterator.hasNext();
        }

        @Override
        public Object next() {
            return rawIterator.next().toJava(Object.class);
        }

        @Override
        public void remove() {
            rawIterator.remove();
        }

    }

}
