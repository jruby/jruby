/*
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 *
 * JRuby - http://jruby.sourceforge.net
 * 
 * This file is part of JRuby
 * 
 * JRuby is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with JRuby; if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307 USA
 */
package org.jruby.runtime;

import org.jruby.RubyModule;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 *
 * @author Anders
 */
public class ObjectSpace {
    private Set references = new HashSet();
    private ReferenceQueue deadReferences = new ReferenceQueue();

    public void add(IRubyObject object) {
        cleanup();
        references.add(new WeakReference(object, deadReferences));
    }

    public Iterator iterator(RubyModule rubyClass) {
        return new ObjectSpaceIterator(rubyClass);
    }

    private void cleanup() {
        Reference reference;
        while ((reference = deadReferences.poll()) != null) {
            references.remove(reference);
        }
    }

    private class ObjectSpaceIterator implements Iterator {
        private final RubyModule rubyClass;
        private final Iterator iterator;

        private IRubyObject next;

        public ObjectSpaceIterator(RubyModule rubyClass) {
            this.rubyClass = rubyClass;
            this.iterator = new ArrayList(references).iterator();
            prefetch();
        }

        public Object next() {
            if (! hasNext()) {
                throw new NoSuchElementException();
            }
            IRubyObject result = next;
            prefetch();
            return result;
        }

        public boolean hasNext() {
            return next != null;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        private void prefetch() {
            while (true) {
                if (! iterator.hasNext()) {
                    next = null;
                    return;
                }
                WeakReference ref = (WeakReference) iterator.next();
                next = (IRubyObject) ref.get();
                if (next != null && next.isKindOf(rubyClass)) {
                    return;
                }
            }
        }
    }
}
