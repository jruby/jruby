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
import org.jruby.ast.ListNode;
import org.jruby.ast.Node;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * This will only be true for blocks which have a pre > 3 in length with a rest argument.
 */
public class PreManyRest1Post0Assigner extends Assigner {
    private int preLength;
    private ListNode pre;
    private Node rest;

    public PreManyRest1Post0Assigner(ListNode pre, int preLength, Node rest) {
        this.pre = pre;
        this.preLength = preLength;
        this.rest = rest;
    }

    @Override
    public void assign(Ruby runtime, ThreadContext context, IRubyObject self, Block block) {
        assignNilTo(runtime, context, self, block, 0);

        rest.assign(runtime, context, self, runtime.newArrayNoCopyLight(IRubyObject.NULL_ARRAY), block, true);
    }

    @Override
    public void assign(Ruby runtime, ThreadContext context, IRubyObject self, IRubyObject value1,
            Block block) {
        pre.get(0).assign(runtime, context, self, value1, block, false);

        assignNilTo(runtime, context, self, block, 1);

        rest.assign(runtime, context, self, runtime.newArrayNoCopyLight(IRubyObject.NULL_ARRAY), block, true);
    }

    @Override
    public void assign(Ruby runtime, ThreadContext context, IRubyObject self, IRubyObject value1,
            IRubyObject value2, Block block) {
        pre.get(0).assign(runtime, context, self, value1, block, false);
        pre.get(1).assign(runtime, context, self, value1, block, false);

        assignNilTo(runtime, context, self, block, 2);

        rest.assign(runtime, context, self, runtime.newArrayNoCopyLight(IRubyObject.NULL_ARRAY), block, true);
    }

    @Override
    public void assign(Ruby runtime, ThreadContext context, IRubyObject self, IRubyObject value1,
            IRubyObject value2, IRubyObject value3, Block block) {
        pre.get(0).assign(runtime, context, self, value1, block, false);
        pre.get(1).assign(runtime, context, self, value1, block, false);
        pre.get(2).assign(runtime, context, self, value1, block, false);

        assignNilTo(runtime, context, self, block, 3);

        rest.assign(runtime, context, self, runtime.newArrayNoCopyLight(IRubyObject.NULL_ARRAY), block, true);
    }

    @Override
    public void assign(Ruby runtime, ThreadContext context, IRubyObject self, IRubyObject values[],
            Block block) {
        int valueLength = values == null ? 0 : values.length;

        switch (valueLength) {
            case 0:
                assign(runtime, context, self, block);
                return;
            case 1:
                assign(runtime, context, self, values[0], block);
                return;
            case 2:
                assign(runtime, context, self, values[0], values[1], block);
                return;
            case 3:
                assign(runtime, context, self, values[0], values[1], values[2], block);
                return;
        }

        // Populate up to shorter of calling arguments or local parameters in the block
        for (int i = 0; i < preLength && i < valueLength; i++) {
            pre.get(i).assign(runtime, context, self, values[i], block, false);
        }

        // nil pad since we provided less values than block parms
        if (valueLength < preLength) {
            assignNilTo(runtime, context, self, block, valueLength);
        } else if (valueLength == preLength) { // no extra args for rest
            rest.assign(runtime, context, self, runtime.newArrayNoCopyLight(IRubyObject.NULL_ARRAY), block, true);
        } else { // extra args for rest
            rest.assign(runtime, context, self,
                    runtime.newArrayNoCopyLight(shiftedArray(values, valueLength - preLength)), block, true);
        }
    }

    @Override
    public void assignArray(Ruby runtime, ThreadContext context, IRubyObject self, IRubyObject arg,
            Block block) {
        RubyArray values = (RubyArray) arg;
        int valueLength = values.getLength();

        switch (valueLength) {
            case 0:
                assign(runtime, context, self, block);
                break;
            case 1:
                assign(runtime, context, self, values.eltInternal(0), block);
                break;
            case 2:
                assign(runtime, context, self, values.eltInternal(0), values.eltInternal(1), block);
                break;
            case 3:
                assign(runtime, context, self, values.eltInternal(0), values.eltInternal(1),
                        values.eltInternal(2), block);
                break;
        }

        // Populate up to shorter of calling arguments or local parameters in the block
        for (int i = 0; i < preLength && i < valueLength; i++) {
            pre.get(i).assign(runtime, context, self, values.eltInternal(i), block, false);
        }

        // nil pad since we provided less values than block parms
        if (valueLength < preLength) {
            assignNilTo(runtime, context, self, block, valueLength);
        } else if (valueLength == preLength) { // no extra args for rest
            rest.assign(runtime, context, self, runtime.newArrayNoCopyLight(IRubyObject.NULL_ARRAY),
                    block, true);
        } else { // extra args for rest
            rest.assign(runtime, context, self,
                    values.subseqLight(preLength, valueLength - preLength), block, true);
        }
    }

    private void assignNilTo(Ruby runtime, ThreadContext context, IRubyObject self, Block block,
            int start) {
        IRubyObject nil = runtime.getNil();

        for (int i = start; i < preLength; i++) {
           pre.get(i).assign(runtime, context, self, nil, block, false);
        }
    }
}
