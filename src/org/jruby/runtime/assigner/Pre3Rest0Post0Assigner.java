/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.runtime.assigner;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.ast.Node;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Assigner for three fixed arguments (pre).
 */
public class Pre3Rest0Post0Assigner extends Assigner {
    private Node parameter1;
    private Node parameter2;
    private Node parameter3;

    public Pre3Rest0Post0Assigner(Node element1, Node element2, Node element3) {
        this.parameter1 = element1;
        this.parameter2 = element2;
        this.parameter3 = element3;
    }

    @Override
    public void assign(Ruby runtime, ThreadContext context, IRubyObject self, Block block) {
        IRubyObject nil = runtime.getNil();

        parameter1.assign(runtime, context, self, nil, block, false);
        parameter2.assign(runtime, context, self, nil, block, false);
        parameter3.assign(runtime, context, self, nil, block, false);
    }

    @Override
    public void assign(Ruby runtime, ThreadContext context, IRubyObject self, IRubyObject value1,
            Block block) {
        parameter1.assign(runtime, context, self, value1, block, false);
        parameter2.assign(runtime, context, self, runtime.getNil(), block, false);
        parameter3.assign(runtime, context, self, runtime.getNil(), block, false);
    }

    @Override
    public void assign(Ruby runtime, ThreadContext context, IRubyObject self, IRubyObject value1,
            IRubyObject value2, Block block) {
        parameter1.assign(runtime, context, self, value1, block, false);
        parameter2.assign(runtime, context, self, value2, block, false);
        parameter3.assign(runtime, context, self, runtime.getNil(), block, false);
    }

    @Override
    public void assign(Ruby runtime, ThreadContext context, IRubyObject self, IRubyObject value1,
            IRubyObject value2, IRubyObject value3, Block block) {
        parameter1.assign(runtime, context, self, value1, block, false);
        parameter2.assign(runtime, context, self, value2, block, false);
        parameter3.assign(runtime, context, self, value3, block, false);
    }

    @Override
    public void assign(Ruby runtime, ThreadContext context, IRubyObject self, IRubyObject values[],
            Block block) {
        int length = values == null ? 0 : values.length;

        switch (length) {
            case 0:
                assign(runtime, context, self, runtime.getNil(), runtime.getNil(), runtime.getNil(), block);
                break;
            case 1:
                assign(runtime, context, self, values[0], runtime.getNil(), runtime.getNil(), block);
                break;
            case 2:
                assign(runtime, context, self, values[0], values[1], runtime.getNil(), block);
                break;
            default:
                assign(runtime, context, self, values[0], values[1], values[2], block);
                break;
        }
    }

    @Override
    public void assignArray(Ruby runtime, ThreadContext context, IRubyObject self, IRubyObject arg,
            Block block) {
        RubyArray values = (RubyArray) arg;
        int length = values.getLength();

        switch (length) {
            case 0:
                assign(runtime, context, self, block);
                break;
            case 1:
                assign(runtime, context, self, values.eltInternal(0), block);
                break;
            case 2:
                assign(runtime, context, self, values.eltInternal(0), values.eltInternal(1), block);
                break;
            default:
                assign(runtime, context, self, values.eltInternal(0), values.eltInternal(1),
                        values.eltInternal(2), block);
                break;
        }
    }
}
