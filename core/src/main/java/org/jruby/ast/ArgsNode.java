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
 * Copyright (C) 2001 Chad Fowler <chadfowler@chadfowler.com>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2007 Mirko Stocker <me@misto.ch>
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

package org.jruby.ast;

import java.util.List;

import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.lexer.yacc.ISourcePosition;

/**
 * Represents the argument declarations of a method.  The fields:
 * foo(p1, ..., pn, o1 = v1, ..., on = v2, *r, q1, ..., qn, k1:, ..., kn:, **K, &b)
 *
 * p1...pn = pre arguments
 * o1...on = optional arguments
 * r       = rest argument
 * q1...qn = post arguments (only in 1.9)
 * k1...kn = keyword arguments
 * K       = keyword rest argument
 * b       = block arg
 */
public class ArgsNode extends Node {
    private final ListNode pre;
    private final ListNode optArgs;
    protected final ArgumentNode restArgNode;
    private final ListNode post;
    private final ListNode keywords;
    private final KeywordRestArgNode keyRest;
    private final BlockArgNode blockArgNode;

    /**
     * Construct a new ArgsNode with no keyword arguments.
     */
    public ArgsNode(ISourcePosition position, ListNode pre, ListNode optionalArguments,
                    RestArgNode rest, ListNode post, BlockArgNode blockArgNode) {
        this(position, pre, optionalArguments, rest, post, null, null, blockArgNode);
    }

    /**
     * Construct a new ArgsNode with keyword arguments.
     */
    public ArgsNode(ISourcePosition position, ListNode pre, ListNode optionalArguments,
            RestArgNode rest, ListNode post, ListNode keywords, KeywordRestArgNode keyRest, BlockArgNode blockArgNode) {
        super(position, pre != null && pre.containsVariableAssignment() ||
                        optionalArguments != null && optionalArguments.containsVariableAssignment() ||
                        rest != null && rest.containsVariableAssignment() ||
                        post != null && post.containsVariableAssignment() ||
                        keywords != null && keywords.containsVariableAssignment() ||
                        keyRest != null && keyRest.containsVariableAssignment() ||
                        blockArgNode != null && blockArgNode.containsVariableAssignment());

        this.pre = pre;
        this.post = post;
        this.optArgs = optionalArguments;
        this.restArgNode = rest;
        this.blockArgNode = blockArgNode;
        this.keywords = keywords;
        this.keyRest = keyRest;
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.ARGSNODE;
    }

    public boolean hasKwargs() {
        return keywords != null || keyRest != null;
    }
    
    public int countKeywords() {
        if (hasKwargs()) {
            if (keywords == null) {
                // Rest keyword argument
                return 0;
            }
            return keywords.size();
        } else {
            return 0;
        }
    }

    public boolean hasRestArg() {
        return restArgNode != null;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    @Override
    public <T> T accept(NodeVisitor<T> iVisitor) {
        return iVisitor.visitArgsNode(this);
    }

    /**
     * Gets the required arguments at the beginning of the argument definition
     */
    public ListNode getPre() {
        return pre;
    }

    public int getRequiredArgsCount() {
        return getPreCount() + getPostCount();
    }

    public int getOptionalArgsCount() {
        return optArgs == null ? 0 : optArgs.size();
    }

    public ListNode getPost() {
        return post;
    }

    public int getMaxArgumentsCount() {
        return hasRestArg() ? -1 : getRequiredArgsCount() + getOptionalArgsCount();
    }

    /**
     * Gets the optArgs.
     * @return Returns a ListNode
     */
    public ListNode getOptArgs() {
        return optArgs;
    }

    /**
     * Gets the restArgNode.
     * @return Returns an ArgumentNode
     */
    public ArgumentNode getRestArgNode() {
        return restArgNode;
    }

    /**
     * Gets the explicit block argument of the parameter list (&block).
     *
     * @return Returns a BlockArgNode
     */
    public BlockArgNode getBlock() {
        return blockArgNode;
    }

    public int getPostCount() {
        return post == null ? 0 : post.size();
    }

    public int getPreCount() {
        return pre == null ? 0 : pre.size();
    }

    public ListNode getKeywords() {
        return keywords;
    }

    public KeywordRestArgNode getKeyRest() {
        return keyRest;
    }

    public boolean hasKeyRest() {
        return keyRest != null;
    }

    // FIXME: This is a hot mess and I think we will still have some extra nulls inserted
    @Override
    public List<Node> childNodes() {
        if (post != null) {
            if (keywords != null) {
                if (keyRest != null) return Node.createList(pre, optArgs, restArgNode, post, keywords, keyRest, blockArgNode);
                    
                return Node.createList(pre, optArgs, restArgNode, post, keywords, blockArgNode);
            }

            return Node.createList(pre, optArgs, restArgNode, post, blockArgNode);
        }

        if (keywords != null) {
            if (keyRest != null) return Node.createList(pre, optArgs, restArgNode, keywords, keyRest, blockArgNode);
            
            return Node.createList(pre, optArgs, restArgNode, keywords, blockArgNode);
        }

        return Node.createList(pre, optArgs, restArgNode, blockArgNode);
    }

    public int getKeywordCount() {
        return keywords == null ? 0 : keywords.size();
    }

    /**
     * How many of the keywords listed happen to be required keyword args.  Note: total kwargs - req kwarg = opt kwargs.
     */
    public int getRequiredKeywordCount() {
        if (keywords == null) return 0;

        int count = 0;
        for (Node keyWordNode :getKeywords().childNodes()) {
            for (Node asgnNode : keyWordNode.childNodes()) {
                if (isRequiredKeywordArgumentValueNode(asgnNode)) count++;
            }
        }
        return count;
    }

    private boolean isRequiredKeywordArgumentValueNode(Node asgnNode) {
        return asgnNode.childNodes().get(0) instanceof RequiredKeywordArgumentValueNode;
    }

}
