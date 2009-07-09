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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2003-2009 Thomas E Enebo <enebo@acm.org>
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
package org.jruby.evaluator;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.ast.ListNode;
import org.jruby.ast.MultipleAsgn19Node;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.Node;
import org.jruby.ast.NodeType;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class AssignmentVisitor {
    @Deprecated
    public static IRubyObject assign(Ruby runtime, ThreadContext context, IRubyObject self, Node node, IRubyObject value, Block block, boolean checkArity) {
        return node.assign(runtime, context, self, value, block, checkArity);
    }

    public static IRubyObject multiAssign(Ruby runtime, ThreadContext context, IRubyObject self, MultipleAsgn19Node node, RubyArray value, boolean checkArity) {
        // Assign the values.
        int valueLen = value.getLength();
        int varLen = node.getPre() == null ? 0 : node.getPre().size();

        int j = 0;
        for (; j < valueLen && j < varLen; j++) {
            node.getPre().get(j).assign(runtime, context, self, value.eltInternal(j), Block.NULL_BLOCK, checkArity);
        }

        if (checkArity && j < varLen) {
            throw runtime.newArgumentError("Wrong # of arguments (" + valueLen + " for " + varLen + ")");
        }

        Node restArgument = node.getRest();
        if (restArgument != null) {
            if (varLen < valueLen) {
                restArgument.assign(runtime, context, self, value.subseqLight(varLen, valueLen), Block.NULL_BLOCK, checkArity);
            } else {
                restArgument.assign(runtime, context, self, RubyArray.newArrayLight(runtime, 0), Block.NULL_BLOCK, checkArity);
            }
        } else if (checkArity && valueLen < varLen) {
            throw runtime.newArgumentError("Wrong # of arguments (" + valueLen + " for " + varLen + ")");
        }

        while (j < varLen) {
            node.getPre().get(j++).assign(runtime, context, self, runtime.getNil(), Block.NULL_BLOCK, checkArity);
        }

        return value;
    }

    public static IRubyObject multiAssign(Ruby runtime, ThreadContext context, IRubyObject self, MultipleAsgnNode node, RubyArray value, boolean checkArity) {
        // Assign the values.
        int valueLen = value.getLength();
        int varLen = node.getHeadNode() == null ? 0 : node.getHeadNode().size();
        
        int j = 0;
        for (; j < valueLen && j < varLen; j++) {
            node.getHeadNode().get(j).assign(runtime, context, self, value.eltInternal(j), Block.NULL_BLOCK, checkArity);
        }

        if (checkArity && j < varLen) {
            throw runtime.newArgumentError("Wrong # of arguments (" + valueLen + " for " + varLen + ")");
        }

        Node argsNode = node.getArgsNode();
        if (argsNode != null) {
            if (argsNode.getNodeType() == NodeType.STARNODE) {
                // no check for '*'
            } else if (varLen < valueLen) {
                argsNode.assign(runtime, context, self, value.subseqLight(varLen, valueLen), Block.NULL_BLOCK, checkArity);
            } else {
                argsNode.assign(runtime, context, self, RubyArray.newArrayLight(runtime, 0), Block.NULL_BLOCK, checkArity);
            }
        } else if (checkArity && valueLen < varLen) {
            throw runtime.newArgumentError("Wrong # of arguments (" + valueLen + " for " + varLen + ")");
        }

        while (j < varLen) {
            node.getHeadNode().get(j++).assign(runtime, context, self, runtime.getNil(), Block.NULL_BLOCK, checkArity);
        }
        
        return value;
    }

    public static IRubyObject multiAssign(Ruby runtime, ThreadContext context, IRubyObject self, MultipleAsgn19Node node, RubyArray value) {
        // Assign the values.
        int valueLen = value.getLength();
        int postCount = node.getPostCount();
        int preCount = node.getPreCount();
        ListNode pre = node.getPre();
        ListNode post = node.getPost();

        int j = 0;
        for (; j < valueLen && j < preCount; j++) {
            pre.get(j).assign(runtime, context, self, value.eltInternal(j), Block.NULL_BLOCK, false);
        }

        Node rest = node.getRest();
        if (rest != null) {
            if (rest.getNodeType() == NodeType.STARNODE) {
                // no check for '*'
            } else if (preCount + postCount < valueLen) {
                rest.assign(runtime, context, self, value.subseqLight(preCount, valueLen - preCount - postCount), Block.NULL_BLOCK, false);
            } else {
                rest.assign(runtime, context, self, RubyArray.newArrayLight(runtime, 0), Block.NULL_BLOCK, false);
            }

            // FIXME: This is wrong
            int postIndexBase = valueLen - postCount;
            for (int i = 0; i < valueLen && i < postCount; i++) {
                post.get(i).assign(runtime, context, self, value.eltInternal(i + postIndexBase), Block.NULL_BLOCK, false);
            }
        }

        while (j < preCount) {
            pre.get(j++).assign(runtime, context, self, runtime.getNil(), Block.NULL_BLOCK, false);
        }

        return value;
    }
}
