/*
 * EvaluateVisitor.java - description
 * Created on 19.02.2002, 18:14:29
 *
 * Copyright (C) 2001, 2002 Jan Arne Petersen
 * Copyright (C) 2004 Thomas E Enebo, Charles O Nutter
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Thomas E Enebo <enebo@acm.org>
 * Charles O Nutter <headius@headius.com>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package org.jruby.evaluator;

import org.jruby.Builtins;
import org.jruby.RubyKernel;
import org.jruby.MetaClass;
import org.jruby.Method;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBignum;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyProc;
import org.jruby.RubyRange;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.ast.*;
import org.jruby.ast.types.INameNode;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.exceptions.ArgumentError;
import org.jruby.exceptions.BreakJump;
import org.jruby.exceptions.FrozenError;
import org.jruby.exceptions.NameError;
import org.jruby.exceptions.NextJump;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.RedoJump;
import org.jruby.exceptions.RetryJump;
import org.jruby.exceptions.ReturnJump;
import org.jruby.exceptions.SecurityError;
import org.jruby.exceptions.SystemStackError;
import org.jruby.exceptions.TypeError;
import org.jruby.internal.runtime.methods.DefaultMethod;
import org.jruby.internal.runtime.methods.EvaluateMethod;
import org.jruby.internal.runtime.methods.WrapperCallable;
import org.jruby.lexer.yacc.SourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ICallable;
import org.jruby.runtime.Iter;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.Asserts;

import java.util.ArrayList;
import java.util.Iterator;

// TODO this visitor often leads to very deep stacks.  If it happens to be a
// real problem, the trampoline method of tail call elimination could be used.
/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public final class EvaluateVisitor implements NodeVisitor {
    private Ruby runtime;
    private ThreadContext threadContext;
    private Builtins builtins;

    private IRubyObject self;
    private IRubyObject result;

    public EvaluateVisitor(Ruby runtime, IRubyObject self) {
        this.runtime = runtime;
        this.threadContext = runtime.getCurrentContext();
        this.self = self;

        builtins = new Builtins(runtime);
    }

    public static EvaluateVisitor createVisitor(IRubyObject self) {
        return new EvaluateVisitor(self.getRuntime(), self);
    }

    /**
     * Helper method.
     *
     * test if a trace function is avaiable.
     *
     */
    private boolean isTrace() {
        return runtime.getTraceFunction() != null;
    }

    private void callTraceFunction(String event, IRubyObject self) {
        String name = threadContext.getCurrentFrame().getLastFunc();
        RubyModule type = threadContext.getCurrentFrame().getLastClass();
        runtime.callTraceFunction(event, threadContext.getPosition(), self, name, type);
    }

    public IRubyObject eval(Node node) {
        // FIXME: Poll from somewhere else in the code?
        threadContext.pollThreadEvents();

        result = runtime.getNil();
        if (node != null) {
        	try {
        		node.accept(this);
        	} catch (StackOverflowError soe) {
        		// TODO: perhaps a better place to catch this
        		throw new SystemStackError(runtime, "stack level too deep");
        	}
        }
        return result;
    }

    /**
     * @see NodeVisitor#visitAliasNode(AliasNode)
     */
    public void visitAliasNode(AliasNode iVisited) {
        if (threadContext.getRubyClass() == null) {
            throw new TypeError(runtime, "no class to make alias");
        }

        threadContext.getRubyClass().aliasMethod(iVisited.getNewName(), iVisited.getOldName());
        threadContext.getRubyClass().callMethod("method_added", builtins.toSymbol(iVisited.getNewName()));
    }

    /**
     * @see NodeVisitor#visitAndNode(AndNode)
     */
    public void visitAndNode(AndNode iVisited) {
        if (eval(iVisited.getFirstNode()).isTrue()) {
            eval(iVisited.getSecondNode());
        }
    }

    /**
     * @see NodeVisitor#visitArgsNode(ArgsNode)
     */
    public void visitArgsNode(ArgsNode iVisited) {
        Asserts.notReached();
    }

    /**
     * @see NodeVisitor#visitArgsCatNode(ArgsCatNode)
     */
    public void visitArgsCatNode(ArgsCatNode iVisited) {
        IRubyObject args = eval(iVisited.getFirstNode());
        IRubyObject secondArgs = splatValue(eval(iVisited.getSecondNode()));
        RubyArray list = args instanceof RubyArray ? (RubyArray) args :
            RubyArray.newArray(runtime, args);
        
        result = list.concat(secondArgs); 
    }

    /**
     * @see NodeVisitor#visitArrayNode(ArrayNode)
     */
    public void visitArrayNode(ArrayNode iVisited) {
        ArrayList list = new ArrayList(iVisited.size());

        for (Iterator iterator = iVisited.iterator(); iterator.hasNext();) {
            list.add(eval((Node) iterator.next()));
        }
        
        result = RubyArray.newArray(runtime, list);
    }

    /**
     * @see NodeVisitor#visitAttrSetNode(AttrSetNode)
     */
    public void visitAttrSetNode(AttrSetNode iVisited) {
        if (runtime.getCurrentFrame().getArgs().length != 1) {
            throw new ArgumentError(runtime, "wrong # of arguments(" + threadContext.getCurrentFrame().getArgs().length + "for 1)");
        }

        result = self.setInstanceVariable(iVisited.getAttributeName(), threadContext.getCurrentFrame().getArgs()[0]);
    }

    /**
     * @see NodeVisitor#visitBackRefNode(BackRefNode)
     */
    public void visitBackRefNode(BackRefNode iVisited) {
        IRubyObject backref = threadContext.getBackref();
        switch (iVisited.getType()) {
            case '&' :
                result = RubyRegexp.last_match(backref);
                break;
            case '`' :
                result = RubyRegexp.match_pre(backref);
                break;
            case '\'' :
                result = RubyRegexp.match_post(backref);
                break;
            case '+' :
                result = RubyRegexp.match_last(backref);
                break;
        }
    }

    /**
     * @see NodeVisitor#visitBeginNode(BeginNode)
     */
    public void visitBeginNode(BeginNode iVisited) {
        eval(iVisited.getBodyNode());
    }

    /**
     * @see NodeVisitor#visitBlockArgNode(BlockArgNode)
     */
    public void visitBlockArgNode(BlockArgNode iVisited) {
        Asserts.notReached();
    }

    /**
     * @see NodeVisitor#visitBlockNode(BlockNode)
     */
    public void visitBlockNode(BlockNode iVisited) {
        Iterator iter = iVisited.iterator();
        while (iter.hasNext()) {
            eval((Node) iter.next());
        }
    }

    /**
     * @see NodeVisitor#visitBlockPassNode(BlockPassNode)
     */
    public void visitBlockPassNode(BlockPassNode iVisited) {
        IRubyObject block = eval(iVisited.getBodyNode());

        if (block.isNil()) {
            threadContext.getIterStack().push(Iter.ITER_NOT);
            try {
                eval(iVisited.getIterNode());
                return;
            } finally {
                threadContext.getIterStack().pop();
            }
        } else if (block instanceof Method) {
            block = ((Method)block).to_proc();
        } else if (!(block instanceof RubyProc)) {
            throw new TypeError(runtime, "wrong argument type " + block.getMetaClass().getName() + " (expected Proc)");
        }

        Block oldBlock = threadContext.getBlockStack().getCurrent();
        threadContext.getBlockStack().push(((RubyProc) block).getBlock());

        threadContext.getIterStack().push(Iter.ITER_PRE);
        threadContext.getCurrentFrame().setIter(Iter.ITER_PRE);

        try {
            eval(iVisited.getIterNode());
        } finally {
            threadContext.getIterStack().pop();
            threadContext.getBlockStack().setCurrent(oldBlock);
        }
    }

    /**
     * @see NodeVisitor#visitBreakNode(BreakNode)
     */
    public void visitBreakNode(BreakNode iVisited) {
        if (iVisited.getValueNode() != null) {
            throw new BreakJump(eval(iVisited.getValueNode()));
        }
        throw new BreakJump();
    }

    /**
     * @see NodeVisitor#visitConstDeclNode(ConstDeclNode)
     */
    public void visitConstDeclNode(ConstDeclNode iVisited) {
        if (threadContext.getRubyClass() == null) {
            throw new TypeError(runtime, "no class/module to define constant");
        }
        
        threadContext.getRubyClass().setConstant(iVisited.getName(), 
        		eval(iVisited.getValueNode()));
    }

    /**
     * @see NodeVisitor#visitClassVarAsgnNode(ClassVarAsgnNode)
     */
    public void visitClassVarAsgnNode(ClassVarAsgnNode iVisited) {
        eval(iVisited.getValueNode());
        threadContext.getRubyClass().setClassVar(iVisited.getName(), result);
    }

    /**
     * @see NodeVisitor#visitClassVarDeclNode(ClassVarDeclNode)
     */
    public void visitClassVarDeclNode(ClassVarDeclNode iVisited) {
        if (threadContext.getRubyClass() == null) {
            throw new TypeError(runtime, "no class/module to define class variable");
        }
        eval(iVisited.getValueNode());
        threadContext.getRubyClass().setClassVar(iVisited.getName(), result);
    }

    /**
     * @see NodeVisitor#visitClassVarNode(ClassVarNode)
     */
    public void visitClassVarNode(ClassVarNode iVisited) {
        if (threadContext.getRubyClass() == null) {
            result = self.getMetaClass().getClassVar(iVisited.getName());
        } else if (! threadContext.getRubyClass().isSingleton()) {
                result = threadContext.getRubyClass().getClassVar(iVisited.getName());
            } else {
                result = ((RubyModule) threadContext.getRubyClass().getInstanceVariable("__attached__")).getClassVar(iVisited.getName());
            }
    }

    /**
     * @see NodeVisitor#visitCallNode(CallNode)
     */
    public void visitCallNode(CallNode iVisited) {
        Block tmpBlock = ArgsUtil.beginCallArgs(threadContext);
        IRubyObject receiver = null;
        IRubyObject[] args = null;
        try {
            receiver = eval(iVisited.getReceiverNode());
            args = ArgsUtil.setupArgs(runtime, threadContext, this, iVisited.getArgsNode());
        } finally {
            ArgsUtil.endCallArgs(threadContext, tmpBlock);
        }
        Asserts.notNull(receiver.getMetaClass(), receiver.getClass().getName());
        
        result = receiver.getMetaClass().call(receiver, iVisited.getName(), args, CallType.NORMAL);
    }

    /**
     * @see NodeVisitor#visitCaseNode(CaseNode)
     */
    public void visitCaseNode(CaseNode iVisited) {
        IRubyObject expression = null;
        if (iVisited.getCaseNode() != null) {
            expression = eval(iVisited.getCaseNode());
        }
        
        Node aNode = (WhenNode) iVisited.getFirstWhenNode();
        
        while (aNode != null) {
            if (aNode instanceof WhenNode == false) {
                eval(aNode);
                break;
            } 

            WhenNode whenNode = (WhenNode) aNode;
            threadContext.setPosition(whenNode.getPosition());
            if (isTrace()) {
                callTraceFunction("line", self);
            }
            RubyArray expressions = (RubyArray) eval(whenNode.getExpressionNodes());
            for (int i = 0; i < expressions.getLength(); i++) {
                if ((expression != null && expressions.entry(i).callMethod("===", expression).isTrue())
                || (expression == null && expressions.entry(i).isTrue())) {
                    eval(whenNode.getBodyNode());
                    return;
                }
            }
            
            aNode = whenNode.getNextCase();
        }
    }

    /**
     * @see NodeVisitor#visitClassNode(ClassNode)
     */
    public void visitClassNode(ClassNode iVisited) {
        RubyClass superClass = getSuperClassFromNode(iVisited.getSuperNode());
        String name = iVisited.getClassName();
        RubyModule enclosingClass = threadContext.getRubyClass();
        RubyClass rubyClass = enclosingClass.defineOrGetClassUnder(name, superClass);

        if (threadContext.getWrapper() != null) {
            rubyClass.extendObject(threadContext.getWrapper());
            rubyClass.includeModule(threadContext.getWrapper());
        }
        evalClassDefinitionBody(iVisited.getBodyNode(), rubyClass);
    }

    private RubyClass getSuperClassFromNode(Node superNode) {
        if (superNode == null) {
            return null;
        }
        RubyClass result;
        try {
            result = (RubyClass) eval(superNode);
        } catch (Exception e) {
            if (superNode instanceof INameNode) {
                String name = ((INameNode) superNode).getName();
                throw new TypeError(runtime,
                                    "undefined superclass '" + name + "'");
            } else {
                throw new TypeError(runtime,
                                    "superclass undefined");
            }
        }
        if (result instanceof MetaClass) {
            throw new TypeError(runtime, "can't make subclass of virtual class");
        }
        return result;
    }

    /**
     * @see NodeVisitor#visitColon2Node(Colon2Node)
     */
    public void visitColon2Node(Colon2Node iVisited) {
        Node node = iVisited.getLeftNode();

        // TODO: Made this more colon3 friendly because of cpath production
        // rule in grammar (it is convenient to think of them as the same thing
        // at a grammar level even though evaluation is).
        if (node == null) {
            result = runtime.getClasses().getObjectClass().getConstant(iVisited.getName());
        } else {
            eval(iVisited.getLeftNode());
            if (result instanceof RubyModule) {
                result = ((RubyModule) result).getConstant(iVisited.getName());
            } else {
                result = result.callMethod(iVisited.getName());
            }
        }
    }

    /**
     * @see NodeVisitor#visitColon3Node(Colon3Node)
     */
    public void visitColon3Node(Colon3Node iVisited) {
        result = runtime.getClasses().getObjectClass().getConstant(iVisited.getName());
    }

    /**
     * @see NodeVisitor#visitConstNode(ConstNode)
     */
    public void visitConstNode(ConstNode iVisited) {
        result = threadContext.getRubyClass().getConstant(iVisited.getName());
    }

    /**
     * @see NodeVisitor#visitDAsgnNode(DAsgnNode)
     */
    public void visitDAsgnNode(DAsgnNode iVisited) {
        eval(iVisited.getValueNode());
        threadContext.getCurrentDynamicVars().set(iVisited.getName(), result);
    }

    /**
     * @see NodeVisitor#visitDRegxNode(DRegexpNode)
     */
    public void visitDRegxNode(DRegexpNode iVisited) {
        StringBuffer sb = new StringBuffer();

        Iterator iterator = iVisited.iterator();
        while (iterator.hasNext()) {
            Node node = (Node) iterator.next();
            sb.append(eval(node));
        }

        result = RubyRegexp.newRegexp(runtime, sb.toString(), iVisited.getOptions(), null);
    }

    /**
     * @see NodeVisitor#visitDStrNode(DStrNode)
     */
    public final void visitDStrNode(final DStrNode iVisited) {
        final StringBuffer sb = new StringBuffer();

        final Iterator iterator = iVisited.iterator();
        while (iterator.hasNext()) {
            Node node = (Node) iterator.next();
            sb.append(eval(node));
        }

        result = builtins.toString(sb.toString());
    }
    
    /**
     * @see NodeVisitor#visitSymbolNode(SymbolNode)
     */
    public void visitDSymbolNode(DSymbolNode iVisited) {
        StringBuffer sb = new StringBuffer();

        for (Iterator iterator = iVisited.getNode().iterator(); 
        	iterator.hasNext();) {
            Node node = (Node) iterator.next();
            sb.append(eval(node));
        }

        result = builtins.toSymbol(sb.toString());
    }


    /**
     * @see NodeVisitor#visitDVarNode(DVarNode)
     */
    public void visitDVarNode(DVarNode iVisited) {
        result = threadContext.getDynamicValue(iVisited.getName());
    }

    /**
     * @see NodeVisitor#visitDXStrNode(DXStrNode)
     */
    public void visitDXStrNode(DXStrNode iVisited) {
        StringBuffer sb = new StringBuffer();

        Iterator iterator = iVisited.iterator();
        while (iterator.hasNext()) {
            Node node = (Node) iterator.next();
            sb.append(eval(node));
        }

        result = self.callMethod("`", builtins.toString(sb.toString()));
    }

    /**
     * @see NodeVisitor#visitDefinedNode(DefinedNode)
     */
    public void visitDefinedNode(DefinedNode iVisited) {
        String def = new DefinedVisitor(runtime, self).getDefinition(iVisited.getExpressionNode());
        if (def != null) {
            result = builtins.toString(def);
        }
    }

    /**
     * @see NodeVisitor#visitDefnNode(DefnNode)
     */
    public void visitDefnNode(DefnNode iVisited) {
        RubyModule rubyClass = threadContext.getRubyClass();
        if (rubyClass == null) {
            throw new TypeError(runtime, "No class to add method.");
        }

        String name = iVisited.getName();
        if (rubyClass == runtime.getClasses().getObjectClass() && name.equals("initialize")) {
            runtime.getErrorHandler().warn("redefining Object#initialize may cause infinite loop");
        }

        Visibility visibility = threadContext.getCurrentVisibility();
        if (name.equals("initialize") || visibility.isModuleFunction()) {
            visibility = Visibility.PRIVATE;
        } else if (visibility.isPublic() && rubyClass == runtime.getClasses().getObjectClass()) {
            visibility = iVisited.getVisibility();
        }

        DefaultMethod newMethod = new DefaultMethod(iVisited.getBodyNode(),
                                                    (ArgsNode) iVisited.getArgsNode(),
                                                    visibility,
                                                    runtime.getRubyClass());

        rubyClass.addMethod(name, newMethod);

        if (threadContext.getCurrentVisibility().isModuleFunction()) {
            rubyClass.getSingletonClass().addMethod(name, new WrapperCallable(newMethod, Visibility.PUBLIC));
            rubyClass.callMethod("singleton_method_added", builtins.toSymbol(name));
        }

        rubyClass.callMethod("method_added", builtins.toSymbol(name));
    }

    /**
     * @see NodeVisitor#visitDefsNode(DefsNode)
     */
    public void visitDefsNode(DefsNode iVisited) {
        IRubyObject receiver = eval(iVisited.getReceiverNode());

        if (runtime.getSafeLevel() >= 4 && !receiver.isTaint()) {
            throw new SecurityError(runtime, "Insecure; can't define singleton method.");
        }
        if (receiver.isFrozen()) {
            throw new FrozenError(runtime, "object");
        }
        if (! receiver.singletonMethodsAllowed()) {
            throw new TypeError(runtime,
                                "can't define singleton method \"" +
                                iVisited.getName() +
                                "\" for " +
                                receiver.getType());
        }

        RubyClass rubyClass = receiver.getSingletonClass();

        if (runtime.getSafeLevel() >= 4) {
            ICallable method = (ICallable) rubyClass.getMethods().get(iVisited.getName());
            if (method != null) {
                throw new SecurityError(runtime, "Redefining method prohibited.");
            }
        }

        DefaultMethod newMethod = new DefaultMethod(iVisited.getBodyNode(),
                                                    (ArgsNode) iVisited.getArgsNode(),
                                                    Visibility.PUBLIC,
                                                    runtime.getRubyClass());
        rubyClass.addMethod(iVisited.getName(), newMethod);
        receiver.callMethod("singleton_method_added", builtins.toSymbol(iVisited.getName()));

        result = runtime.getNil();
    }

    /**
     * @see NodeVisitor#visitDotNode(DotNode)
     */
    public void visitDotNode(DotNode iVisited) {
        result = RubyRange.newRange(runtime, eval(iVisited.getBeginNode()), eval(iVisited.getEndNode()), iVisited.isExclusive());
    }

    /**
     * @see NodeVisitor#visitEnsureNode(EnsureNode)
     */
    public void visitEnsureNode(EnsureNode iVisited) {
        try {
            result = eval(iVisited.getBodyNode());
        } finally {
            if (iVisited.getEnsureNode() != null) {
                IRubyObject oldResult = result;
                eval(iVisited.getEnsureNode());
                result = oldResult;
            }
        }
    }

    /**
     * @see NodeVisitor#visitEvStrNode(EvStrNode)
     */
    public final void visitEvStrNode(final EvStrNode iVisited) {
        eval(iVisited.getBody());
    }

    /**
     * @see NodeVisitor#visitFCallNode(FCallNode)
     */
    public void visitFCallNode(FCallNode iVisited) {
        Block tmpBlock = ArgsUtil.beginCallArgs(threadContext);
        IRubyObject[] args = null;
        try {
            args = ArgsUtil.setupArgs(runtime, threadContext, this, iVisited.getArgsNode());
        } finally {
            ArgsUtil.endCallArgs(threadContext, tmpBlock);
        }

        result = self.getMetaClass().call(self, iVisited.getName(), args, CallType.FUNCTIONAL);
    }

    /**
     * @see NodeVisitor#visitFalseNode(FalseNode)
     */
    public void visitFalseNode(FalseNode iVisited) {
        result = runtime.getFalse();
    }

    /**
     * @see NodeVisitor#visitFlipNode(FlipNode)
     */
    public void visitFlipNode(FlipNode iVisited) {
        if (iVisited.isExclusive()) {
            if (! threadContext.getScopeStack().getValue(iVisited.getCount()).isTrue()) {
                //Benoit: I don't understand why the result is inversed
                result = eval(iVisited.getBeginNode()).isTrue() ? runtime.getFalse() : runtime.getTrue();
                threadContext.getScopeStack().setValue(iVisited.getCount(), result);
            } else {
                if (eval(iVisited.getEndNode()).isTrue()) {
                    threadContext.getScopeStack().setValue(iVisited.getCount(), runtime.getFalse());
                }
                result = runtime.getTrue();
            }
        } else {
            if (! threadContext.getScopeStack().getValue(iVisited.getCount()).isTrue()) {
                if (eval(iVisited.getBeginNode()).isTrue()) {
                    //Benoit: I don't understand why the result is inversed
                    threadContext.getScopeStack().setValue(iVisited.getCount(), eval(iVisited.getEndNode()).isTrue() ? runtime.getFalse() : runtime.getTrue());
                    result = runtime.getTrue();
                } else {
                    result = runtime.getFalse();
                }
            } else {
                if (eval(iVisited.getEndNode()).isTrue()) {
                    runtime.getScope().setValue(iVisited.getCount(), runtime.getFalse());
                }
                result = runtime.getTrue();
            }
        }
    }

    /**
     * @see NodeVisitor#visitForNode(ForNode)
     */
    public void visitForNode(ForNode iVisited) {
        threadContext.getBlockStack().push(iVisited.getVarNode(), new EvaluateMethod(iVisited.getBodyNode()), self);
        threadContext.getIterStack().push(Iter.ITER_PRE);

        try {
            while (true) {
                try {
                    SourcePosition position = threadContext.getPosition();
                    Block tmpBlock = ArgsUtil.beginCallArgs(threadContext);

                    IRubyObject recv = null;
                    try {
                        recv = eval(iVisited.getIterNode());
                    } finally {
                        threadContext.setPosition(position);
                        ArgsUtil.endCallArgs(threadContext, tmpBlock);
                    }
                    result = recv.getMetaClass().call(recv, "each", IRubyObject.NULL_ARRAY, CallType.NORMAL);
                    return;
                } catch (RetryJump retry) {
                }
            }
        } catch (BreakJump bExcptn) {
            IRubyObject breakValue = bExcptn.getBreakValue();
            
            result = breakValue == null ? runtime.getNil() : breakValue;
        } finally {
            threadContext.getIterStack().pop();
            threadContext.getBlockStack().pop();
        }
    }

    /**
     * @see NodeVisitor#visitGlobalAsgnNode(GlobalAsgnNode)
     */
    public void visitGlobalAsgnNode(GlobalAsgnNode iVisited) {
        eval(iVisited.getValueNode());
        runtime.getGlobalVariables().set(iVisited.getName(), result);
    }

    /**
     * @see NodeVisitor#visitGlobalVarNode(GlobalVarNode)
     */
    public void visitGlobalVarNode(GlobalVarNode iVisited) {
        result = runtime.getGlobalVariables().get(iVisited.getName());
    }

    /**
     * @see NodeVisitor#visitHashNode(HashNode)
     */
    public void visitHashNode(HashNode iVisited) {
        RubyHash hash = RubyHash.newHash(runtime);

        if (iVisited.getListNode() != null) {
            Iterator iterator = iVisited.getListNode().iterator();
            while (iterator.hasNext()) {
                IRubyObject key = eval((Node) iterator.next());
                if (iterator.hasNext()) {
                    hash.aset(key, eval((Node) iterator.next()));
                } else {
                    // XXX
                    throw new RuntimeException("[BUG] odd number list for Hash");
                    // XXX
                }
            }
        }
        result = hash;
    }

    /**
     * @see NodeVisitor#visitInstAsgnNode(InstAsgnNode)
     */
    public void visitInstAsgnNode(InstAsgnNode iVisited) {
        eval(iVisited.getValueNode());
        self.setInstanceVariable(iVisited.getName(), result);
    }

    /**
     * @see NodeVisitor#visitInstVarNode(InstVarNode)
     */
    public void visitInstVarNode(InstVarNode iVisited) {
        result = self.getInstanceVariable(iVisited.getName());
    }

    /**
     * @see NodeVisitor#visitIfNode(IfNode)
     */
    public void visitIfNode(IfNode iVisited) {
        if (eval(iVisited.getCondition()).isTrue()) {
            eval(iVisited.getThenBody());
        } else {
            eval(iVisited.getElseBody());
        }
    }

    /**
     * @see NodeVisitor#visitIterNode(IterNode)
     */
    public void visitIterNode(IterNode iVisited) {
        threadContext.getBlockStack().push(iVisited.getVarNode(), new EvaluateMethod(iVisited.getBodyNode()), self);
        threadContext.getIterStack().push(Iter.ITER_PRE);
        try {
            while (true) {
                try {
                    result = eval(iVisited.getIterNode());
                    return;
                } catch (RetryJump rExcptn) {
                }
            }
        } catch (BreakJump bExcptn) {
            IRubyObject breakValue = bExcptn.getBreakValue();

            result = breakValue == null ? runtime.getNil() : breakValue;
        } finally {
            threadContext.getIterStack().pop();
            threadContext.getBlockStack().pop();
        }
    }

    /**
     * @see NodeVisitor#visitLocalAsgnNode(LocalAsgnNode)
     */
    public void visitLocalAsgnNode(LocalAsgnNode iVisited) {
        eval(iVisited.getValueNode());
        threadContext.getScopeStack().setValue(iVisited.getCount(), result);
    }

    /**
     * @see NodeVisitor#visitLocalVarNode(LocalVarNode)
     */
    public void visitLocalVarNode(LocalVarNode iVisited) {
        result = threadContext.getScopeStack().getValue(iVisited.getCount());
    }

    /**
     * @see NodeVisitor#visitMultipleAsgnNode(MultipleAsgnNode)
     */
    public void visitMultipleAsgnNode(MultipleAsgnNode iVisited) {
        result = new AssignmentVisitor(runtime, self).assign(iVisited, eval(iVisited.getValueNode()), false);
    }

    /**
     * @see NodeVisitor#visitMatch2Node(Match2Node)
     */
    public void visitMatch2Node(Match2Node iVisited) {
        result = ((RubyRegexp) eval(iVisited.getReceiverNode())).match(eval(iVisited.getValueNode()));
    }

    /**
     * @see NodeVisitor#visitMatch3Node(Match3Node)
     */
    public void visitMatch3Node(Match3Node iVisited) {
        IRubyObject receiver = eval(iVisited.getReceiverNode());
        IRubyObject value = eval(iVisited.getValueNode());
        if (value instanceof RubyString) {
            result = ((RubyRegexp) receiver).match(value);
        } else {
            result = value.callMethod("=~", receiver);
        }
    }

    /**
     * @see NodeVisitor#visitMatchNode(MatchNode)
     */
    public void visitMatchNode(MatchNode iVisited) {
        result = ((RubyRegexp) eval(iVisited.getRegexpNode())).match2();
    }

    /**
     * @see NodeVisitor#visitModuleNode(ModuleNode)
     */
    public void visitModuleNode(ModuleNode iVisited) {
        RubyModule enclosingModule = threadContext.getRubyClass();
        if (enclosingModule == null) {
            throw new TypeError(runtime, "no outer class/module");
        }

        RubyModule module;
        if (enclosingModule == runtime.getClasses().getObjectClass()) {
            module = runtime.getOrCreateModule(iVisited.getName());
        } else {
            module = enclosingModule.defineModuleUnder(iVisited.getName());
        }
        evalClassDefinitionBody(iVisited.getBodyNode(), module);
    }

    /**
     * @see NodeVisitor#visitNewlineNode(NewlineNode)
     */
    public void visitNewlineNode(NewlineNode iVisited) {
        final SourcePosition position = iVisited.getPosition();
        if (position != null) {
            threadContext.setPosition(position);
            if (isTrace()) {
                callTraceFunction("line", self);
            }
        }
        eval(iVisited.getNextNode());
    }

    /**
     * @see NodeVisitor#visitNextNode(NextNode)
     */
    public void visitNextNode(NextNode iVisited) {
        if (iVisited.getValueNode() != null) {
            throw new NextJump(eval(iVisited.getValueNode()));
        }
        throw new NextJump();
    }

    /**
     * @see NodeVisitor#visitNilNode(NilNode)
     */
    public void visitNilNode(NilNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitNotNode(NotNode)
     */
    public void visitNotNode(NotNode iVisited) {
        result = eval(iVisited.getConditionNode()).isTrue() ? runtime.getFalse() : runtime.getTrue();
    }

    /**
     * @see NodeVisitor#visitNthRefNode(NthRefNode)
     */
    public void visitNthRefNode(NthRefNode iVisited) {
        result = RubyRegexp.nth_match(iVisited.getMatchNumber(), threadContext.getBackref());
    }

    /**
     * @see NodeVisitor#visitOpElementAsgnNode(OpElementAsgnNode)
     */
    public void visitOpElementAsgnNode(OpElementAsgnNode iVisited) {
        IRubyObject receiver = eval(iVisited.getReceiverNode());

        IRubyObject[] args = ArgsUtil.setupArgs(runtime, threadContext, this, iVisited.getArgsNode());

        IRubyObject firstValue = receiver.callMethod("[]", args);

        if (iVisited.getOperatorName().equals("||")) {
            if (firstValue.isTrue()) {
                result = firstValue;
                return;
            } else {
                firstValue = eval(iVisited.getValueNode());
            }
        } else if (iVisited.getOperatorName().equals("&&")) {
            if (!firstValue.isTrue()) {
                result = firstValue;
                return;
            } else {
                firstValue = eval(iVisited.getValueNode());
            }
        } else {
            firstValue = firstValue.callMethod(iVisited.getOperatorName(), eval(iVisited.getValueNode()));
        }

        IRubyObject[] expandedArgs = new IRubyObject[args.length + 1];
        System.arraycopy(args, 0, expandedArgs, 0, args.length);
        expandedArgs[expandedArgs.length - 1] = firstValue;
        result = receiver.callMethod("[]=", expandedArgs);
    }

    /**
     * @see NodeVisitor#visitOpAsgnNode(OpAsgnNode)
     */
    public void visitOpAsgnNode(OpAsgnNode iVisited) {
        IRubyObject receiver = eval(iVisited.getReceiverNode());
        IRubyObject value = receiver.callMethod(iVisited.getVariableName());

        if (iVisited.getOperatorName().equals("||")) {
            if (value.isTrue()) {
                result = value;
                return;
            } else {
                value = eval(iVisited.getValueNode());
            }
        } else if (iVisited.getOperatorName().equals("&&")) {
            if (!value.isTrue()) {
                result = value;
                return;
            } else {
                value = eval(iVisited.getValueNode());
            }
        } else {
            value = value.callMethod(iVisited.getOperatorName(), eval(iVisited.getValueNode()));
        }

        receiver.callMethod(iVisited.getVariableName() + "=", value);

        result = value;
    }

    /**
     * @see NodeVisitor#visitOpAsgnAndNode(OpAsgnAndNode)
     */
    public void visitOpAsgnAndNode(OpAsgnAndNode iVisited) {
        if (eval(iVisited.getFirstNode()).isTrue()) {
            eval(iVisited.getSecondNode());
        }
    }

    /**
     * @see NodeVisitor#visitOpAsgnOrNode(OpAsgnOrNode)
     */
    public void visitOpAsgnOrNode(OpAsgnOrNode iVisited) {
        if (!eval(iVisited.getFirstNode()).isTrue()) {
            eval(iVisited.getSecondNode());
        }
    }

    /**
     * @see NodeVisitor#visitOptNNode(OptNNode)
     */
    public void visitOptNNode(OptNNode iVisited) {
        while (RubyKernel.gets(runtime.getTopSelf(), IRubyObject.NULL_ARRAY).isTrue()) {
            while (true) { // Used for the 'redo' command
                try {
                    eval(iVisited.getBodyNode());
                    break;
                } catch (RedoJump rJump) {
                    // When a 'redo' is reached eval body of loop again.
                } catch (NextJump nJump) {
                    // When a 'next' is reached ceck condition of loop again.
                    break;
                } catch (BreakJump bJump) {
                    // When a 'break' is reached leave loop.
                    return;
                }
            }
        }
    }

    /**
     * @see NodeVisitor#visitOrNode(OrNode)
     */
    public void visitOrNode(OrNode iVisited) {
        if (!eval(iVisited.getFirstNode()).isTrue()) {
            eval(iVisited.getSecondNode());
        }
    }

    /**
     * @see NodeVisitor#visitPostExeNode(PostExeNode)
     */
    public void visitPostExeNode(PostExeNode iVisited) {
    }

    /**
     * @see NodeVisitor#visitRedoNode(RedoNode)
     */
    public void visitRedoNode(RedoNode iVisited) {
        throw new RedoJump();
    }

    /**
     * @see NodeVisitor#visitRescueBodyNode(RescueBodyNode)
     */
    public void visitRescueBodyNode(RescueBodyNode iVisited) {
        eval(iVisited.getBodyNode());
    }

    /**
     * @see NodeVisitor#visitRescueNode(RescueNode)
     */
    public void visitRescueNode(RescueNode iVisited) {
        RescuedBlock : while (true) {
            try {
                // Execute rescue block
                eval(iVisited.getBodyNode());

                // If no exception is thrown execute else block
                if (iVisited.getElseNode() != null) {
                    eval(iVisited.getElseNode());
                }

                return;
            } catch (RaiseException raiseJump) {
                runtime.getGlobalVariables().set("$!", raiseJump.getException());

                RescueBodyNode rescueNode = iVisited.getRescueNode();

                while (rescueNode != null) {
                    Node  exceptionNodes = rescueNode.getExceptionNodes();
                    ListNode exceptionNodesList;
                    
                    if (exceptionNodes instanceof SplatNode) {                    
                        exceptionNodesList = (ListNode) eval(exceptionNodes);
                    } else {
                        exceptionNodesList = (ListNode) exceptionNodes;
                    }
                    
                    if (isRescueHandled(raiseJump.getException(), exceptionNodesList)) {
                        try {
                            eval(rescueNode);
                            return;
                        } catch (RetryJump retryJump) {
                            runtime.getGlobalVariables().set("$!", runtime.getNil());
                            continue RescuedBlock;
                        }
                    }
                    
                    rescueNode = rescueNode.getOptRescueNode();
                }

                throw raiseJump;
            } finally {
                runtime.getGlobalVariables().set("$!", runtime.getNil());
            }
        }
    }

    /**
     * @see NodeVisitor#visitRetryNode(RetryNode)
     */
    public void visitRetryNode(RetryNode iVisited) {
        throw new RetryJump();
    }

    /**
     * @see NodeVisitor#visitReturnNode(ReturnNode)
     */
    public void visitReturnNode(ReturnNode iVisited) {
        throw new ReturnJump(eval(iVisited.getValueNode()));
    }

    /**
     * @see NodeVisitor#visitSClassNode(SClassNode)
     */
    public void visitSClassNode(SClassNode iVisited) {
        IRubyObject receiver = eval(iVisited.getReceiverNode());

        RubyClass singletonClass = null;

        if (receiver.isNil()) {
            singletonClass = runtime.getClasses().getNilClass();
        } else if (receiver == runtime.getTrue()) {
            singletonClass = runtime.getClasses().getTrueClass();
        } else if (receiver == runtime.getFalse()) {
            singletonClass = runtime.getClasses().getFalseClass();
        } else {
            if (runtime.getSafeLevel() >= 4 && !receiver.isTaint()) {
                throw new SecurityError(runtime, "Insecure: can't extend object.");
            }

            if (receiver.getMetaClass() instanceof MetaClass) {
                RubyModule.clearMethodCache(runtime);
            }

            singletonClass = receiver.getSingletonClass();
        }

        if (threadContext.getWrapper() != null) {
            singletonClass.extendObject(threadContext.getWrapper());
            singletonClass.includeModule(threadContext.getWrapper());
        }

        evalClassDefinitionBody(iVisited.getBodyNode(), singletonClass);
    }

    /**
     * @see NodeVisitor#visitScopeNode(ScopeNode)
     */
    public void visitScopeNode(ScopeNode iVisited) {
        threadContext.getFrameStack().pushCopy();
        threadContext.getScopeStack().push(iVisited.getLocalNames());
        try {
            eval(iVisited.getBodyNode());
        } finally {
            threadContext.getScopeStack().pop();
            threadContext.getFrameStack().pop();
        }
    }

    /**
     * @see NodeVisitor#visitSelfNode(SelfNode)
     */
    public void visitSelfNode(SelfNode iVisited) {
        result = self;
    }

    public void visitSplatNode(SplatNode iVisited) {
        result = splatValue(eval(iVisited.getValue()));
    }
    
    /**
     * @see NodeVisitor#visitStrNode(StrNode)
     */
    public void visitStrNode(StrNode iVisited) {
        result = builtins.toString(iVisited.getValue());
    }

    /**
     * @see NodeVisitor#visitSValueNode(StrNode)
     */
    public void visitSValueNode(SValueNode iVisited) {
        result = aValueSplat(eval(iVisited.getValue()));
    }

    /**
     * @see NodeVisitor#visitSuperNode(SuperNode)
     */
    public void visitSuperNode(SuperNode iVisited) {
        if (threadContext.getCurrentFrame().getLastClass() == null) {
            throw new NameError(runtime, "Superclass method '" + threadContext.getCurrentFrame().getLastFunc() + "' disabled.");
        }

        Block tmpBlock = ArgsUtil.beginCallArgs(threadContext);

        IRubyObject[] args = null;
        try {
            args = ArgsUtil.setupArgs(runtime, threadContext, this, iVisited.getArgsNode());
        } finally {
            ArgsUtil.endCallArgs(threadContext, tmpBlock);
        }
        result = threadContext.callSuper(args);
    }

    /**
     * @see NodeVisitor#visitToAryNode(ToAryNode)
     */
    public void visitToAryNode(ToAryNode iVisited) {
        result = aryToAry(eval(iVisited.getValue()));
    }

    /**
     * @see NodeVisitor#visitTrueNode(TrueNode)
     */
    public void visitTrueNode(TrueNode iVisited) {
        result = runtime.getTrue();
    }

    /**
     * @see NodeVisitor#visitUndefNode(UndefNode)
     */
    public void visitUndefNode(UndefNode iVisited) {
        if (threadContext.getRubyClass() == null) {
            throw new TypeError(runtime, "No class to undef method '" + iVisited.getName() + "'.");
        }
        threadContext.getRubyClass().undef(iVisited.getName());
    }

    /**
     * @see NodeVisitor#visitUntilNode(UntilNode)
     */
    public void visitUntilNode(UntilNode iVisited) {
        while (!eval(iVisited.getConditionNode()).isTrue()) {
            while (true) { // Used for the 'redo' command
                try {
                    eval(iVisited.getBodyNode());
                    break;
                } catch (RedoJump rJump) {
                    // When a 'redo' is reached eval body of loop again.
                    continue;
                } catch (NextJump nJump) {
                    // When a 'next' is reached ceck condition of loop again.
                    break;
                } catch (BreakJump bJump) {
                    // When a 'break' is reached leave loop.
                    return;
                }
            }
        }
    }

    /**
     * @see NodeVisitor#visitVAliasNode(VAliasNode)
     */
    public void visitVAliasNode(VAliasNode iVisited) {
        runtime.getGlobalVariables().alias(iVisited.getNewName(), iVisited.getOldName());
    }

    /**
     * @see NodeVisitor#visitVCallNode(VCallNode)
     */
    public void visitVCallNode(VCallNode iVisited) {
        result = self.getMetaClass().call(self, iVisited.getMethodName(), IRubyObject.NULL_ARRAY, CallType.VARIABLE);
    }

    /**
     * @see NodeVisitor#visitWhenNode(WhenNode)
     */
    public void visitWhenNode(WhenNode iVisited) {
        Asserts.notReached();
    }

    /**
     * @see NodeVisitor#visitWhileNode(WhileNode)
     */
    public void visitWhileNode(WhileNode iVisited) {
        // while do...Initial condition not met do not enter block
        if (iVisited.evaluateAtStart() && 
            eval(iVisited.getConditionNode()).isTrue() == false) {
            return;
        }
        
        do {
            while (true) { // Used for the 'redo' command
                try {
                    eval(iVisited.getBodyNode());
                    break;
                } catch (RedoJump rJump) {
                    // When a 'redo' is reached eval body of loop again.
                } catch (NextJump nJump) {
                    // When a 'next' is reached ceck condition of loop again.
                    break;
                } catch (BreakJump bJump) {
                    // When a 'break' is reached leave loop.
                    return;
                }
            }
        } while (eval(iVisited.getConditionNode()).isTrue());
    }

    /**
     * @see NodeVisitor#visitXStrNode(XStrNode)
     */
    public void visitXStrNode(XStrNode iVisited) {
        result = self.callMethod("`", builtins.toString(iVisited.getValue()));
    }

    /**
     * @see NodeVisitor#visitYieldNode(YieldNode)
     */
    public void visitYieldNode(YieldNode iVisited) {
        eval(iVisited.getArgsNode());
        
        result = threadContext.yield(result, null, null, false, iVisited.getCheckState());
    }

    /**
     * @see NodeVisitor#visitZArrayNode(ZArrayNode)
     */
    public void visitZArrayNode(ZArrayNode iVisited) {
        result = builtins.newArray();
    }

    /**
     * @see NodeVisitor#visitZSuperNode(ZSuperNode)
     */
    public void visitZSuperNode(ZSuperNode iVisited) {
        if (threadContext.getCurrentFrame().getLastClass() == null) {
            throw new NameError(runtime, "superclass method '" + runtime.getCurrentFrame().getLastFunc() + "' disabled");
        }
        IRubyObject[] args = threadContext.getCurrentFrame().getArgs();
        result = threadContext.callSuper(args);
    }

    /**
     * @see NodeVisitor#visitBignumNode(BignumNode)
     */
    public void visitBignumNode(BignumNode iVisited) {
        result = RubyBignum.newBignum(runtime, iVisited.getValue());
    }

    /**
     * @see NodeVisitor#visitFixnumNode(FixnumNode)
     */
    public void visitFixnumNode(FixnumNode iVisited) {
        result = RubyFixnum.newFixnum(runtime, iVisited.getValue());
    }

    /**
     * @see NodeVisitor#visitFloatNode(FloatNode)
     */
    public void visitFloatNode(FloatNode iVisited) {
        result = RubyFloat.newFloat(runtime, iVisited.getValue());
    }

    /**
     * @see NodeVisitor#visitRegexpNode(RegexpNode)
     */
    public void visitRegexpNode(RegexpNode iVisited) {
        result = RubyRegexp.newRegexp(builtins.toString(iVisited.getValue()), iVisited.getOptions(), null);
    }

    /**
     * @see NodeVisitor#visitSymbolNode(SymbolNode)
     */
    public void visitSymbolNode(SymbolNode iVisited) {
        result = builtins.toSymbol(iVisited.getName());
    }

    /** Evaluates the body in a class or module definition statement.
     *
     */
    private void evalClassDefinitionBody(ScopeNode iVisited, RubyModule type) {
        threadContext.getFrameStack().pushCopy();
        threadContext.pushClass(type);
        threadContext.getScopeStack().push(iVisited.getLocalNames());
        threadContext.pushDynamicVars();

        IRubyObject oldSelf = self;

        try {
            if (isTrace()) {
                callTraceFunction("class", type);
            }

            self = type;
            eval(iVisited.getBodyNode());
        } finally {
            self = oldSelf;

            threadContext.popDynamicVars();
            threadContext.getScopeStack().pop();
            threadContext.popClass();
            threadContext.getFrameStack().pop();

            if (isTrace()) {
                callTraceFunction("end", null);
            }
        }
    }

    private boolean isRescueHandled(RubyException currentException, ListNode exceptionNodes) {
        if (exceptionNodes == null) {
            return currentException.isKindOf(runtime.getExceptions().getStandardError());
        }

        Block tmpBlock = ArgsUtil.beginCallArgs(threadContext);

        IRubyObject[] args = null;
        try {
            args = ArgsUtil.setupArgs(runtime, threadContext, this, exceptionNodes);
        } finally {
            ArgsUtil.endCallArgs(threadContext, tmpBlock);
        }

        for (int i = 0; i < args.length; i++) {
            if (! args[i].isKindOf(runtime.getClasses().getModuleClass())) {
                throw new TypeError(runtime, "class or module required for rescue clause");
            }
            if (currentException.isKindOf((RubyModule) args[i])) {
                return true;
            }
        }
        return false;
    }
    
    private IRubyObject aryToAry(IRubyObject value) {
        if (value instanceof RubyArray) {
            return value;
        }
        
        if (value.respondsTo("to_ary")) {
            return value.convertToType("Array", "to_ary", false);
        }
        
        return RubyArray.newArray(runtime, value);
    }
    
    private IRubyObject splatValue(IRubyObject value) {
        if (value.isNil()) {
            return RubyArray.newArray(runtime, value);
        }
        
        return arrayValue(value);
    }

    private IRubyObject aValueSplat(IRubyObject value) {
        if (value instanceof RubyArray == false || 
            ((RubyArray)value).length().getLongValue() == 0) {
            return runtime.getNil();
        }
        
        RubyArray array = (RubyArray) value;
        
        return array.getLength() == 1 ? array.first(null) : array;
    }

    /* HACK: .... */
    private RubyArray arrayValue(IRubyObject value) {
        IRubyObject newValue = value.convertToType("Array", "to_ary", false);

        if (newValue.isNil()) {
            // XXXEnebo: We should call to_a except if it is kernel def....
            // but we will forego for now.
            newValue = RubyArray.newArray(runtime, value);
        }
        
        return (RubyArray) newValue;
    }
}
