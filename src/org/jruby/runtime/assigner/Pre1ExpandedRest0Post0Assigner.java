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
import org.jruby.RubyArray;
import org.jruby.ast.Node;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A single fixed parameter which can handled receiving an expandable argument (RubyArray).
 */
public class Pre1ExpandedRest0Post0Assigner extends Assigner {
    private final Node parameter1;
    
    public Pre1ExpandedRest0Post0Assigner(Node parameter1) {
        this.parameter1 = parameter1;
    }

    @Override
    public void assign(Ruby runtime, ThreadContext context, IRubyObject self, Block block) {
        parameter1.assign(runtime, context, self, runtime.getNil(), block, false);
    }

    @Override
    public void assign(Ruby runtime, ThreadContext context, IRubyObject self, IRubyObject value1,
            Block block) {
        parameter1.assign(runtime, context, self, value1, block, false);
    }

    @Override
    public void assign(Ruby runtime, ThreadContext context, IRubyObject self, IRubyObject value1,
            IRubyObject value2, Block block) {
        parameter1.assign(runtime, context, self, runtime.newArray(value1, value2), block, false);
    }


    @Override
    public void assign(Ruby runtime, ThreadContext context, IRubyObject self, IRubyObject value1,
            IRubyObject value2, IRubyObject value3, Block block) {
        parameter1.assign(runtime, context, self, runtime.newArray(value1, value2, value3), block, false);
    }

    @Override
    public void assign(Ruby runtime, ThreadContext context, IRubyObject self, IRubyObject values[],
            Block block) {
        int length = values == null ? 0 : values.length;

        switch (length) {
            case 0:
                assign(runtime, context, self, block);
                break;
            case 1:
                assign(runtime, context, self, values[0], block);
                break;
            default:
                assign(runtime, context, self, runtime.newArray(values), block);
        }
    }

    @Override
    public void assignArray(Ruby runtime, ThreadContext context, IRubyObject self, IRubyObject value,
            Block block) {        
        assign(runtime, context, self, value, block);
    }

    @Override
    public IRubyObject convertToArray(Ruby runtime, IRubyObject value) {
        return value;
    }

    @Override
    public IRubyObject convertIfAlreadyArray(Ruby runtime, IRubyObject value) {
        int length = ArgsUtil.arrayLength(value);
        switch (length) {
        case 0:
            value = runtime.getNil();
            break;
        case 1:
            value = ((RubyArray)value).eltInternal(0);
            break;
        default:
            runtime.getWarnings().warn(ID.MULTIPLE_VALUES_FOR_BLOCK, "multiple values for a block parameter (" + length + " for 1)");
        }

        return value;
    }
}
