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
 * Copyright (C) 2018 The JRuby Team
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
package org.jruby.ext.date;

import org.joda.time.DateTime;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.anno.JRubyClass;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * JRuby's <code>DateTime</code> implementation - 'native' parts.
 * In MRI, since 2.x, all of date.rb has been moved to native (C) code.
 *
 * @see RubyDate
 *
 * @author kares
 */
@JRubyClass(name = "DateTime")
public class RubyDateTime extends RubyDate {

    static RubyClass createDateTimeClass(Ruby runtime, RubyClass Date) {
        RubyClass DateTime = runtime.defineClass("DateTime", Date, ALLOCATOR);
        DateTime.setReifiedClass(RubyDateTime.class);
        DateTime.defineAnnotatedMethods(RubyDateTime.class);
        return DateTime;
    }

    private static final ObjectAllocator ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyDateTime(runtime, klass, defaultDateTime);
        }
    };

    private static RubyClass getDateTime(final Ruby runtime) {
        return (RubyClass) runtime.getObject().getConstantAt("DateTime");
    }

    public RubyDateTime(Ruby runtime, RubyClass klass, DateTime dt) {
        super(runtime, klass, dt);
    }

    public RubyDateTime(Ruby runtime, DateTime dt) {
        super(runtime, getDateTime(runtime), dt);
    }

}
