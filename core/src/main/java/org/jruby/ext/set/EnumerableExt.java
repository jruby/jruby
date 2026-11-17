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

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.api.Access;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Enumerable#to_set (from require 'set')
 *
 * @author kares
 */
public abstract class EnumerableExt {

    //@JRubyMethod
    public static IRubyObject to_set(final ThreadContext context, final IRubyObject self, final Block block) {
        RubySet set = new RubySet(context.runtime, Access.getClass(context, "Set"), false);
        set.initialize(context, self, block);
        return set; // return runtime.getClass("Set").newInstance(context, self, block);
    }

    @JRubyMethod(rest = true) // to_set(klass = Set, *args, &block)
    public static IRubyObject to_set(final ThreadContext context, final IRubyObject self,
        final IRubyObject[] args, final Block block) {

        if ( args.length == 0 ) return to_set(context, self, block);

        final IRubyObject klass = args[0]; args[0] = self;
        return ((RubyClass) klass).newInstance(context, args, block);
    }

}
