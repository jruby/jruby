/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2009 Thomas E Enebo <enebo@acm.org>
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

package org.jruby.runtime.assigner;

import org.jruby.Ruby;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Base class for block assignment logic.  This class and all subclasses tries to minimize the
 * amount of conditional logic for doing simple assignment to block parameters during block
 * invocation.
 * 
 * The naming conventions for subclasses is the same logic used by the interpreter to represent
 * all parameter types with their arity:
 * pre - Require parameters before the rest argument (if there is one)
 * opt - optional argument (name = value) [1.9]
 * rest - A rest argument is present
 * post - required arguments at the end of the list [1.9]
 *
 * There is also some logic about expanded/non-expanded arguments.  This refers to
 * ParserSupport.new_yield and YieldNode expanded attribute.
 */
public abstract class Assigner {

    public abstract void assign(Ruby runtime, ThreadContext context, IRubyObject self, Block block);
    public abstract void assign(Ruby runtime, ThreadContext context, IRubyObject self, IRubyObject value1,
            Block block);
    public abstract void assign(Ruby runtime, ThreadContext context, IRubyObject self, IRubyObject value1,
            IRubyObject value2, Block block);
    public abstract void assign(Ruby runtime, ThreadContext context, IRubyObject self, IRubyObject value1,
            IRubyObject value2, IRubyObject value3, Block block);
    public abstract void assign(Ruby runtime, ThreadContext context, IRubyObject self, IRubyObject[] values,
            Block block);
    public abstract void assignArray(Ruby runtime, ThreadContext context, IRubyObject self, IRubyObject values,
            Block block);

    /*
     * This is the proper behavior for all non-expanded assigners which have a pre > 0.  The rest
     * override this.
     */
    public IRubyObject convertToArray(Ruby runtime, IRubyObject value) {
        return ArgsUtil.convertToRubyArray(runtime, value, true);
    }

    /*
     * This is the proper behavior for all non-expanded assigners.
     */
    public IRubyObject convertIfAlreadyArray(Ruby runtime, IRubyObject value) {
        return value;
    }

    protected IRubyObject[] shiftedArray(IRubyObject[] originalValues, int numberOfElementsToShift) {
        int newLength = originalValues.length - numberOfElementsToShift;

        IRubyObject[] newValues = new IRubyObject[newLength];

        System.arraycopy(originalValues, numberOfElementsToShift, newValues, 0, newLength);

        return newValues;
    }
}
