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

import org.jruby.Ruby;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Arity;

/**
 * Represents the argument declarations of a method.  The fields:
 * foo(p1, ..., pn, o1 = v1, ..., on = v2, *r, q1, ..., qn)
 *
 * p1...pn = pre arguments
 * o1...on = optional arguments
 * r       = rest argument
 * q1...qn = post arguments (only in 1.9)
 */
public class ArgsNode extends Node {
    private final ListNode pre;
    private final int preCount;
    private final ListNode optArgs;
    protected final ArgumentNode restArgNode;
    protected final int restArg;
    private final BlockArgNode blockArgNode;
    protected Arity arity;
    private final int requiredArgsCount;
    protected final boolean hasOptArgs;
    protected final boolean hasMasgnArgs;
    protected final boolean hasKwargs;
    protected int maxArgsCount;
    protected final boolean isSimple;

    // Only in ruby 1.9 methods
    private final ListNode post;
    private final int postCount;
    private final int postIndex;

    // Only in ruby 2.0 methods
    private final ListNode keywords;
    private final KeywordRestArgNode keyRest;

    /**
     * Construct a new ArgsNode with no keyword arguments.
     *
     * @param position
     * @param pre
     * @param optionalArguments
     * @param rest
     * @param post
     * @param blockArgNode
     */
    public ArgsNode(ISourcePosition position, ListNode pre, ListNode optionalArguments,
                    RestArgNode rest, ListNode post, BlockArgNode blockArgNode) {
        this(position, pre, optionalArguments, rest, post, null, null, blockArgNode);
    }

    /**
     * Construct a new ArgsNode with keyword arguments.
     *
     * @param position
     * @param pre
     * @param optionalArguments
     * @param rest
     * @param post
     * @param keywords
     * @param keyRest
     * @param blockArgNode
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
        this.preCount = pre == null ? 0 : pre.size();
        this.post = post;
        this.postCount = post == null ? 0 : post.size();
        int optArgCount = optionalArguments == null ? 0 : optionalArguments.size();
        this.postIndex = getPostCount(preCount, optArgCount, rest);
        this.optArgs = optionalArguments;
        this.restArg = rest == null ? -1 : rest.getIndex();
        this.restArgNode = rest;
        this.blockArgNode = blockArgNode;
        this.keywords = keywords;
        this.keyRest = keyRest;
        this.requiredArgsCount = preCount + postCount;
        this.hasOptArgs = getOptArgs() != null;
        this.hasMasgnArgs = hasMasgnArgs();
        this.hasKwargs = keywords != null || keyRest != null;
        this.maxArgsCount = getRestArg() >= 0 ? -1 : getRequiredArgsCount() + getOptionalArgsCount();
        this.arity = calculateArity();

        this.isSimple = !(hasMasgnArgs || hasOptArgs || restArg >= 0 || postCount > 0 || hasKwargs);
    }
    
    private int getPostCount(int preCount, int optArgCount, RestArgNode rest) {
        // Simple-case: If we have a rest we know where it is
        if (rest != null) return rest.getIndex() + 1;

        return preCount + optArgCount;
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.ARGSNODE;
    }

    protected Arity calculateArity() {
        if (getOptArgs() != null || getRestArg() >= 0) return Arity.required(getRequiredArgsCount());

        return Arity.createArity(getRequiredArgsCount());
    }

    public boolean hasKwargs() {
        return hasKwargs;
    }
    
    public int countKeywords() {
        if (hasKwargs) {
            if (keywords == null) {
                // Rest keyword argument
                return 0;
            }
            return keywords.size();
        } else {
            return 0;
        }
    }

    protected boolean hasMasgnArgs() {
        if (preCount > 0) for (Node node : pre.childNodes()) {
            if (node instanceof AssignableNode) return true;
        }
        if (postCount > 0) for (Node node : post.childNodes()) {
            if (node instanceof AssignableNode) return true;
        }
        return false;
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

    public Arity getArity() {
        return arity;
    }

    public int getRequiredArgsCount() {
        return requiredArgsCount;
    }

    public int getOptionalArgsCount() {
        return optArgs == null ? 0 : optArgs.size();
    }

    public ListNode getPost() {
        return post;
    }

    public int getMaxArgumentsCount() {
        return maxArgsCount;
    }

    /**
     * Gets the optArgs.
     * @return Returns a ListNode
     */
    public ListNode getOptArgs() {
        return optArgs;
    }

    /**
     * Gets the restArg.
     * @return Returns a int
     */
    public int getRestArg() {
        return restArg;
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
        return postCount;
    }

    public int getPostIndex() {
        return postIndex;
    }

    public int getPreCount() {
        return preCount;
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

    public void checkArgCount(Ruby runtime, int argsLength) {
        Arity.checkArgumentCount(runtime, argsLength, requiredArgsCount, maxArgsCount, hasKwargs);
    }

    public void checkArgCount(Ruby runtime, String name, int argsLength) {
        Arity.checkArgumentCount(runtime, name, argsLength, requiredArgsCount, maxArgsCount, hasKwargs);
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

    public int getRequiredKeywordCount() {
        if (hasRequiredKeywordArg()) return 1;
        return 0;
    }

    private boolean hasRequiredKeywordArg() {
        if (getKeywords() == null) return false;

        for (Node keyWordNode :getKeywords().childNodes()) {
            for (Node asgnNode : keyWordNode.childNodes()) {
                if (isRequiredKeywordArgumentValueNode(asgnNode)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isRequiredKeywordArgumentValueNode(Node asgnNode) {
        return asgnNode.childNodes().get(0) instanceof RequiredKeywordArgumentValueNode;
    }

}
