/*
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
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

import java.util.*;
import java.lang.ref.*;
import org.jruby.RubyObject;
import org.jruby.RubyModule;

/**
 *
 * @author Anders
 */
public final class ObjectSpace {
    private HashMap references = new HashMap();
    private ReferenceQueue deadReferences = new ReferenceQueue();
    
    public ObjectSpace() {
        /* Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                cleanup();
            }
        }, 8000, 2000);
        */
    }

    public void add(RubyObject object) {
		//cleanup();
        references.put(new WeakReference(object, deadReferences), null);
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

        private RubyObject next;

        public ObjectSpaceIterator(RubyModule rubyClass) {
            this.rubyClass = rubyClass;
            this.iterator = new ArrayList(references.keySet()).iterator();
            prefetch();
        }

        public Object next() {
            if (! hasNext()) {
                throw new NoSuchElementException();
            }
            RubyObject result = next;
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
                next = (RubyObject) ref.get();
                if (next == null) {
                    references.remove(ref);
                } else if (next != null && next.isKindOf(rubyClass)) {
                    return;
                }
            }
        }
    }
}