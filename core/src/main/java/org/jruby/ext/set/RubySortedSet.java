/*
 **** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Native implementation of Ruby's SortedSet (set.rb replacement).
 *
 * @author kares
 */
@org.jruby.anno.JRubyClass(name="SortedSet", parent = "Set")
public class RubySortedSet extends RubySet {

    static RubyClass createSortedSetClass(final Ruby runtime) {
        RubyClass SortedSet = runtime.defineClass("SortedSet", runtime.getClass("Set"), ALLOCATOR);

        SortedSet.setReifiedClass(RubySortedSet.class);

        SortedSet.defineAnnotatedMethods(RubySortedSet.class);

        return SortedSet;
    }

    private static final ObjectAllocator ALLOCATOR = new ObjectAllocator() {
        public RubySortedSet allocate(Ruby runtime, RubyClass klass) {
            return new RubySortedSet(runtime, klass);
        }
    };

    public RubySortedSet(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }

    @Override
    protected void addObject(final Ruby runtime, final IRubyObject obj) {
        if ( ! obj.respondsTo("<=>") ) { // TODO site-cache
            throw runtime.newArgumentError("value must respond to <=>");
        }
        super.addObject(runtime, obj);
    }

}
