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
import org.jruby.runtime.Helpers;

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
    private Node[] args;
    private short optIndex;
    private short postIndex;
    private short keywordsIndex;

    protected final ArgumentNode restArgNode;
    private final KeywordRestArgNode keyRest;
    private final BlockArgNode blockArgNode;
    private ListNode blockLocalVariables = null;

    private static final Node[] NO_ARGS = new Node[] {};
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

        int preSize = pre != null ? pre.size() : 0;
        int optSize = optionalArguments != null ? optionalArguments.size() : 0;
        int postSize = post != null ? post.size() : 0;
        int keywordsSize = keywords != null ? keywords.size() : 0;
        int size = preSize + optSize + postSize + keywordsSize;

        args = size > 0 ? new Node[size] : NO_ARGS;
        optIndex = (short) (preSize != 0 ? preSize : 0);
        postIndex = (short) (optSize != 0 ? optIndex + optSize : optIndex);
        keywordsIndex = (short) (postSize != 0 ? postIndex + postSize : postIndex);

        if (preSize > 0) System.arraycopy(pre.children(), 0,  args, 0, preSize);
        if (optSize > 0) System.arraycopy(optionalArguments.children(), 0, args, optIndex, optSize);
        if (postSize > 0) System.arraycopy(post.children(), 0, args, postIndex, postSize);
        if (keywordsSize > 0) System.arraycopy(keywords.children(), 0, args, keywordsIndex, keywordsSize);

        this.restArgNode = rest;
        this.blockArgNode = blockArgNode;
        this.keyRest = keyRest;
    }

    public Node[] getArgs() {
        return args;
    }

    public int getOptArgIndex() {
        return optIndex;
    }

    public int getPostIndex() {
        return postIndex;
    }

    public int getKeywordsIndex() {
        return keywordsIndex;
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.ARGSNODE;
    }

    public boolean hasKwargs() {
        boolean keywords = getKeywordCount() > 0;
        return keywords || keyRest != null;
    }
    
    public int countKeywords() {
        if (hasKwargs()) {
            boolean keywords = args.length - keywordsIndex > 0;
            if (keywords) {
                // Rest keyword argument
                return 0;
            }
            return args.length - keywordsIndex;
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
        return new ListNode(getPosition()).addAll(args, 0, getPreCount());
    }

    public int getRequiredArgsCount() {
        return getPreCount() + getPostCount();
    }

    public int getOptionalArgsCount() {
        return postIndex - optIndex;
    }

    public ListNode getPost() {
        return new ListNode(getPosition()).addAll(args, postIndex, getPostCount());
    }

    public int getMaxArgumentsCount() {
        return hasRestArg() ? -1 : getRequiredArgsCount() + getOptionalArgsCount();
    }

    /**
     * Gets the optArgs.
     * @return Returns a ListNode
     */
    public ListNode getOptArgs() {
        return new ListNode(getPosition()).addAll(args, optIndex, getOptionalArgsCount());
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
        return keywordsIndex - postIndex;
    }

    public int getPreCount() {
        return optIndex;
    }

    public ListNode getKeywords() {
        return new ListNode(getPosition()).addAll(args, keywordsIndex, getKeywordCount());
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
        ListNode post = getPost();
        ListNode keywords = getKeywords();
        ListNode pre = getPre();
        ListNode optArgs = getOptArgs();


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
        return args.length - keywordsIndex;
    }

    /**
     * How many of the keywords listed happen to be required keyword args.  Note: total kwargs - req kwarg = opt kwargs.
     */
    public int getRequiredKeywordCount() {
        if (getKeywordCount() < 1) return 0;

        int count = 0;
        for (int i = 0; i < getKeywordCount(); i++) {
            for (Node asgnNode : args[keywordsIndex + i].childNodes()) {
                if (Helpers.isRequiredKeywordArgumentValueNode(asgnNode)) count++;
            }
        }
        return count;
    }

    public ListNode getBlockLocalVariables() {
        return blockLocalVariables;
    }

    public void setBlockLocalVariables(ListNode blockLocalVariables) {
        this.blockLocalVariables = blockLocalVariables;
    }
}
