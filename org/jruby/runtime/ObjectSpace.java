/*
 * ObjectSpace.java
 * Created on 27 May 2002
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore,
 * Benoit Cerrina, Chad Fowler, Anders Bengtsson
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Chad Fowler <chadfowler@chadfowler.com>
 * Anders Bengtsson <ndrsbngtssn@yahoo.se>
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

package org.jruby.runtime;

import java.util.*;
import java.lang.ref.SoftReference;
import org.jruby.RubyObject;
import org.jruby.RubyModule;

/**
 *
 * @author Anders
 */

public class ObjectSpace {

    private List objects = new LinkedList();

    public void add(RubyObject object) {
        objects.add(new SoftReference(object));
    }

    public Iterator iterator(RubyModule rubyClass) {
        return new ObjectSpaceIterator(rubyClass);
    }

    private class ObjectSpaceIterator implements Iterator {
        private final RubyModule rubyClass;
        private final Iterator iterator;

        private RubyObject next;

        public ObjectSpaceIterator(RubyModule rubyClass) {
            this.rubyClass = rubyClass;
            this.iterator = new LinkedList(objects).iterator();
            prefetch();
        }

        public Object next() {
            RubyObject result = next;
            prefetch();
            return result;
        }

        public boolean hasNext() {
            return next != null;
        }

        public void remove() {
            // FIXME: throw exception
        }

        private void prefetch() {
            while (true) {
                if (! iterator.hasNext()) {
                    next = null;
                    return;
                }
                SoftReference ref = (SoftReference) iterator.next();
                next = (RubyObject) ref.get();
                if (next == null) {
                    objects.remove(ref);
                } else if (next != null && next.isKindOf(rubyClass)) {
                    return;
                }
            }
        }
    }
}
