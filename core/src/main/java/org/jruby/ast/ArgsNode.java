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
import org.jruby.RubyArray;
import org.jruby.RubyHash;
import org.jruby.RubySymbol;
import org.jruby.ast.types.INameNode;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.runtime.Helpers;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

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
        super(position);

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
    public Object accept(NodeVisitor iVisitor) {
        return iVisitor.visitArgsNode(this);
    }

    /**
     * Gets the required arguments at the beginning of the argument definition
     */
    public ListNode getPre() {
        return pre;
    }

    @Deprecated
    public ListNode getArgs() {
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

    @Deprecated
    public BlockArgNode getBlockArgNode() {
        return blockArgNode;
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

    public void prepare(ThreadContext context, Ruby runtime, IRubyObject self, IRubyObject[] args, Block block) {
        DynamicScope scope = context.getCurrentScope();
        RubyHash kwvals = getKeywordValues(args);

        // Bind 'normal' parameter values to the local scope for this method.
        if (!hasMasgnArgs) {
            // no arg grouping, just use bulk assignment methods
            if (preCount > 0) scope.setArgValues(args, Math.min(args.length, preCount));
            if (postCount > 0 && args.length > preCount) scope.setEndArgValues(args, postIndex, Math.min(args.length - preCount, postCount));
        } else {
            masgnAwareArgAssign(context, runtime, self, args, block, scope);
        }

        // optArgs and restArgs require more work, so isolate them and ArrayList creation here
        if (hasOptArgs || restArg != -1) prepareOptOrRestArgs(context, runtime, scope, self, args, kwvals);
        if (hasKwargs) {
            // We couldn't check this in Arity, because we didn't know at that point whether the call
            // site actually provided us with keyword values.
            if (kwvals == null && maxArgsCount > -1 && args.length == maxArgsCount + 1) {
              throw runtime.newArgumentError(args.length, requiredArgsCount);
            }
            assignKwargs(kwvals, runtime, context, scope, self);
        }
        if (getBlock() != null) processBlockArg(scope, runtime, block);
    }

    private RubyHash getKeywordValues(IRubyObject[] args) {
        if (hasKwargs
            && (args.length > requiredArgsCount)
            && (args[args.length - 1] instanceof RubyHash)) {
            return (RubyHash) args[args.length - 1];
        }
        return null;
    }

    private void masgnAwareArgAssign(ThreadContext context, Ruby runtime, IRubyObject self, IRubyObject[] args, Block block, DynamicScope scope) {
        // arg grouping, use slower arg walking logic
        if (preCount > 0) {
            int size = pre.size();
            for (int i = 0; i < size && i < args.length; i++) {
                Node next = pre.get(i);
                if (next instanceof AssignableNode) {
                    ((AssignableNode)next).assign(runtime, context, self, args[i], block, false);
                } else if (next instanceof ArgumentNode) {
                    ArgumentNode argNode = (ArgumentNode) next;
                    scope.setValue(argNode.getIndex(), args[i], argNode.getDepth());
                } else {
                    // TODO: Replace with assert later
                    throw new RuntimeException("Whoa..not assignable and not an argument...what is it: " + next);
                }
            }
        }
        if (postCount > 0) {
            int size = post.size();
            int argsLength = args.length;
            for (int i = 0; i < size; i++) {
                Node next = post.get(i);
                if (next instanceof AssignableNode) {
                    ((AssignableNode)next).assign(runtime, context, self, args[argsLength - postCount + i], block, false);
                } else if (next instanceof ArgumentNode) {
                    ArgumentNode argNode = (ArgumentNode) next;
                    scope.setValue(argNode.getIndex(), args[argsLength - postCount + i], argNode.getDepth());
                } else {
                    // TODO: Replace with assert later
                    throw new RuntimeException("Whoa..not assignable and not an argument...what is it: " + next);
                }
            }
        }
    }

    public void prepare(ThreadContext context, Ruby runtime, IRubyObject self, Block block) {
        DynamicScope scope = context.getCurrentScope();

        if (isSimple) {
            scope.setArgValues();
        } else {
            prepare(context, runtime, self, IRubyObject.NULL_ARRAY, block);
        }
        if (getBlock() != null) processBlockArg(scope, runtime, block);
    }
    public void prepare(ThreadContext context, Ruby runtime, IRubyObject self, IRubyObject arg0, Block block) {
        DynamicScope scope = context.getCurrentScope();

        if (isSimple) {
            scope.setArgValues(arg0);
        } else {
            prepare(context, runtime, self, new IRubyObject[] {arg0}, block);
        }
        if (getBlock() != null) processBlockArg(scope, runtime, block);
    }
    public void prepare(ThreadContext context, Ruby runtime, IRubyObject self, IRubyObject arg0, IRubyObject arg1, Block block) {
        DynamicScope scope = context.getCurrentScope();

        if (isSimple) {
            scope.setArgValues(arg0, arg1);
        } else {
            prepare(context, runtime, self, new IRubyObject[] {arg0, arg1}, block);
        }
        if (getBlock() != null) processBlockArg(scope, runtime, block);
    }
    public void prepare(ThreadContext context, Ruby runtime, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        DynamicScope scope = context.getCurrentScope();

        if (isSimple) {
            scope.setArgValues(arg0, arg1, arg2);
        } else {
            prepare(context, runtime, self, new IRubyObject[] {arg0, arg1, arg2}, block);
        }
        if (getBlock() != null) processBlockArg(scope, runtime, block);
    }
    public void prepare(ThreadContext context, Ruby runtime, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        DynamicScope scope = context.getCurrentScope();

        if (isSimple) {
            scope.setArgValues(arg0, arg1, arg2, arg3);
        } else {
            prepare(context, runtime, self, new IRubyObject[] {arg0, arg1, arg2, arg3}, block);
        }
        if (getBlock() != null) processBlockArg(scope, runtime, block);
    }
    public void prepare(ThreadContext context, Ruby runtime, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, IRubyObject arg4, Block block) {
        DynamicScope scope = context.getCurrentScope();

        if (isSimple) {
            scope.setArgValues(arg0, arg1, arg2, arg3, arg4);
        } else {
            prepare(context, runtime, self, new IRubyObject[] {arg0, arg1, arg2, arg3, arg4}, block);
        }
        if (getBlock() != null) processBlockArg(scope, runtime, block);
    }
    public void prepare(ThreadContext context, Ruby runtime, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, IRubyObject arg4, IRubyObject arg5, Block block) {
        DynamicScope scope = context.getCurrentScope();

        if (isSimple) {
            scope.setArgValues(arg0, arg1, arg2, arg3, arg4, arg5);
        } else {
            prepare(context, runtime, self, new IRubyObject[] {arg0, arg1, arg2, arg3, arg4, arg5}, block);
        }
        if (getBlock() != null) processBlockArg(scope, runtime, block);
    }
    public void prepare(ThreadContext context, Ruby runtime, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, IRubyObject arg4, IRubyObject arg5, IRubyObject arg6, Block block) {
        DynamicScope scope = context.getCurrentScope();

        if (isSimple) {
            scope.setArgValues(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
        } else {
            prepare(context, runtime, self, new IRubyObject[] {arg0, arg1, arg2, arg3, arg4, arg5, arg6}, block);
        }
        if (getBlock() != null) processBlockArg(scope, runtime, block);
    }
    public void prepare(ThreadContext context, Ruby runtime, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, IRubyObject arg4, IRubyObject arg5, IRubyObject arg6, IRubyObject arg7, Block block) {
        DynamicScope scope = context.getCurrentScope();

        if (isSimple) {
            scope.setArgValues(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
        } else {
            prepare(context, runtime, self, new IRubyObject[] {arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7}, block);
        }
        if (getBlock() != null) processBlockArg(scope, runtime, block);
    }
    public void prepare(ThreadContext context, Ruby runtime, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, IRubyObject arg4, IRubyObject arg5, IRubyObject arg6, IRubyObject arg7, IRubyObject arg8, Block block) {
        DynamicScope scope = context.getCurrentScope();

        if (isSimple) {
            scope.setArgValues(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
        } else {
            prepare(context, runtime, self, new IRubyObject[] {arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8}, block);
        }
        if (getBlock() != null) processBlockArg(scope, runtime, block);
    }
    public void prepare(ThreadContext context, Ruby runtime, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, IRubyObject arg4, IRubyObject arg5, IRubyObject arg6, IRubyObject arg7, IRubyObject arg8, IRubyObject arg9, Block block) {
        DynamicScope scope = context.getCurrentScope();

        if (isSimple) {
            scope.setArgValues(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9);
        } else {
            prepare(context, runtime, self, new IRubyObject[] {arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9}, block);
        }
        if (getBlock() != null) processBlockArg(scope, runtime, block);
    }

    public void checkArgCount(Ruby runtime, int argsLength) {
        Arity.checkArgumentCount(runtime, argsLength, requiredArgsCount, maxArgsCount, hasKwargs);
    }

    public void checkArgCount(Ruby runtime, String name, int argsLength) {
        Arity.checkArgumentCount(runtime, name, argsLength, requiredArgsCount, maxArgsCount, hasKwargs);
    }

    protected void prepareOptOrRestArgs(ThreadContext context, Ruby runtime, DynamicScope scope,
            IRubyObject self, IRubyObject[] args, RubyHash kwvals) {
        int arglen = (kwvals == null) ? args.length : args.length - 1;
        int givenArgsCount = prepareOptionalArguments(context, runtime, self, args, arglen);
        prepareRestArg(context, runtime, scope, args, arglen, givenArgsCount);
    }

    protected int prepareOptionalArguments(ThreadContext context, Ruby runtime, IRubyObject self, 
            IRubyObject[] args, int arglen) {
        return hasOptArgs ? assignOptArgs(args, runtime, context, self, preCount, arglen) : preCount;
    }

    protected void prepareRestArg(ThreadContext context, Ruby runtime, DynamicScope scope,
            IRubyObject[] args, int arglen, int givenArgsCount) {
        if (restArg >= 0) {
            int sizeOfRestArg = arglen - postCount - givenArgsCount - (postCount == 0 && keywords == null && keyRest != null ? 1 : 0);
            if (sizeOfRestArg <= 0) { // no more values to stick in rest arg
                scope.setValue(restArg, RubyArray.newArray(runtime), 0);
            } else {
                scope.setValue(restArg, RubyArray.newArrayNoCopy(runtime, args, givenArgsCount, sizeOfRestArg), 0);
            }
        }
    }

    protected int assignOptArgs(IRubyObject[] args, Ruby runtime, ThreadContext context, IRubyObject self, int givenArgsCount, int arglen) {
        // assign given optional arguments to their variables
        int j = 0;
        for (int i = preCount; i < arglen - postCount && j < optArgs.size(); i++, j++) {
            // in-frame EvalState should already have receiver set as self, continue to use it
            optArgs.get(j).assign(runtime, context, self, args[i], Block.NULL_BLOCK, true);
            givenArgsCount++;
        }

        // assign the default values, adding to the end of allArgs
        for (int i = 0; j < optArgs.size(); i++, j++) {
            optArgs.get(j).interpret(runtime, context, self, Block.NULL_BLOCK);
        }

        return givenArgsCount;
    }

    protected void assignKwargs(RubyHash keyValues, Ruby runtime, ThreadContext context, DynamicScope scope, IRubyObject self) {
        if (keyValues != null) {
            if (keywords != null) {
                for (Node knode : keywords.childNodes()) {
                    KeywordArgNode kwarg = (KeywordArgNode) knode;
                    AssignableNode kasgn = (AssignableNode) kwarg.getAssignable();
                    String name = ((INameNode) kasgn).getName();
                    RubySymbol sym = runtime.newSymbol(name);

                    if (keyValues.has_key_p(sym).isFalse()) {
                        kasgn.interpret(runtime, context, self, Block.NULL_BLOCK);
                    } else {
                        IRubyObject value = keyValues.delete(context, sym, Block.NULL_BLOCK);
                        // Enebo: This casting is gruesome but IR solution will replace it in 9k.
                        if (kasgn instanceof LocalAsgnNode) {
                            scope.setValue(((LocalAsgnNode) kasgn).getIndex(), value, ((LocalAsgnNode) kasgn).getDepth());    
                        } else if (kasgn instanceof DAsgnNode) {
                            scope.setValue(((DAsgnNode) kasgn).getIndex(), value, ((DAsgnNode) kasgn).getDepth());
                        } else {
                            assert false: "Should never happen";
                        }
                    }
                }

                if (keyRest != null) {
                    scope.setValue(keyRest.getIndex(), keyValues, keyRest.getDepth());
                } else if (!keyValues.isEmpty()) { // No kwrest and no explicit keyword args
                    throw runtime.newArgumentError("unknown keyword: " + keyValues.directKeySet().iterator().next());
                }
                return;
            }
        }

        if (keywords != null) {
            for (Node knode : keywords.childNodes()) {
                KeywordArgNode kwarg = (KeywordArgNode)knode;
                if (kwarg.getAssignable() instanceof LocalAsgnNode) {
                    ((LocalAsgnNode)kwarg.getAssignable()).interpret(runtime, context, self, Block.NULL_BLOCK);
                } else if (kwarg.getAssignable() instanceof DAsgnNode) {
                    ((DAsgnNode)kwarg.getAssignable()).interpret(runtime, context, self, Block.NULL_BLOCK);                    
                }
            }
        }
        
        if (keyRest != null) {
            scope.setValue(keyRest.getIndex(), RubyHash.newSmallHash(runtime), keyRest.getDepth());
        }
    }

    protected void processBlockArg(DynamicScope scope, Ruby runtime, Block block) {
        scope.setValue(getBlock().getCount(), Helpers.processBlockArgument(runtime, block), 0);
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
}
