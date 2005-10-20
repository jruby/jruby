/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 David Corbin <dcorbin@users.sourceforge.net>
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

import java.util.ArrayList;
import java.util.Iterator;

import org.jruby.MetaClass;
import org.jruby.IRuby;
import org.jruby.RubyArray;
import org.jruby.RubyBignum;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyFloat;
import org.jruby.RubyHash;
import org.jruby.RubyKernel;
import org.jruby.RubyModule;
import org.jruby.RubyProc;
import org.jruby.RubyRange;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.ast.AliasNode;
import org.jruby.ast.AndNode;
import org.jruby.ast.ArgsCatNode;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.BackRefNode;
import org.jruby.ast.BeginNode;
import org.jruby.ast.BignumNode;
import org.jruby.ast.BlockArgNode;
import org.jruby.ast.BlockNode;
import org.jruby.ast.BlockPassNode;
import org.jruby.ast.BreakNode;
import org.jruby.ast.CallNode;
import org.jruby.ast.CaseNode;
import org.jruby.ast.ClassNode;
import org.jruby.ast.ClassVarAsgnNode;
import org.jruby.ast.ClassVarDeclNode;
import org.jruby.ast.ClassVarNode;
import org.jruby.ast.Colon2Node;
import org.jruby.ast.Colon3Node;
import org.jruby.ast.ConstDeclNode;
import org.jruby.ast.ConstNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.DRegexpNode;
import org.jruby.ast.DStrNode;
import org.jruby.ast.DSymbolNode;
import org.jruby.ast.DVarNode;
import org.jruby.ast.DXStrNode;
import org.jruby.ast.DefinedNode;
import org.jruby.ast.DefnNode;
import org.jruby.ast.DefsNode;
import org.jruby.ast.DotNode;
import org.jruby.ast.EnsureNode;
import org.jruby.ast.EvStrNode;
import org.jruby.ast.FCallNode;
import org.jruby.ast.FalseNode;
import org.jruby.ast.FixnumNode;
import org.jruby.ast.FlipNode;
import org.jruby.ast.FloatNode;
import org.jruby.ast.ForNode;
import org.jruby.ast.GlobalAsgnNode;
import org.jruby.ast.GlobalVarNode;
import org.jruby.ast.HashNode;
import org.jruby.ast.IfNode;
import org.jruby.ast.InstAsgnNode;
import org.jruby.ast.InstVarNode;
import org.jruby.ast.IterNode;
import org.jruby.ast.ListNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.LocalVarNode;
import org.jruby.ast.Match2Node;
import org.jruby.ast.Match3Node;
import org.jruby.ast.MatchNode;
import org.jruby.ast.ModuleNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.NewlineNode;
import org.jruby.ast.NextNode;
import org.jruby.ast.NilNode;
import org.jruby.ast.Node;
import org.jruby.ast.NotNode;
import org.jruby.ast.NthRefNode;
import org.jruby.ast.OpAsgnAndNode;
import org.jruby.ast.OpAsgnNode;
import org.jruby.ast.OpAsgnOrNode;
import org.jruby.ast.OpElementAsgnNode;
import org.jruby.ast.OptNNode;
import org.jruby.ast.OrNode;
import org.jruby.ast.PostExeNode;
import org.jruby.ast.RedoNode;
import org.jruby.ast.RegexpNode;
import org.jruby.ast.RescueBodyNode;
import org.jruby.ast.RescueNode;
import org.jruby.ast.RetryNode;
import org.jruby.ast.ReturnNode;
import org.jruby.ast.SClassNode;
import org.jruby.ast.SValueNode;
import org.jruby.ast.ScopeNode;
import org.jruby.ast.SelfNode;
import org.jruby.ast.SplatNode;
import org.jruby.ast.StrNode;
import org.jruby.ast.SuperNode;
import org.jruby.ast.SymbolNode;
import org.jruby.ast.ToAryNode;
import org.jruby.ast.TrueNode;
import org.jruby.ast.UndefNode;
import org.jruby.ast.UntilNode;
import org.jruby.ast.VAliasNode;
import org.jruby.ast.VCallNode;
import org.jruby.ast.WhenNode;
import org.jruby.ast.WhileNode;
import org.jruby.ast.XStrNode;
import org.jruby.ast.YieldNode;
import org.jruby.ast.ZArrayNode;
import org.jruby.ast.ZSuperNode;
import org.jruby.ast.types.INameNode;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.exceptions.BreakJump;
import org.jruby.exceptions.NextJump;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.RedoJump;
import org.jruby.exceptions.RetryJump;
import org.jruby.exceptions.ReturnJump;
import org.jruby.internal.runtime.methods.DefaultMethod;
import org.jruby.internal.runtime.methods.EvaluateCallable;
import org.jruby.internal.runtime.methods.WrapperCallable;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ICallable;
import org.jruby.runtime.Iter;
import org.jruby.runtime.Scope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

// TODO this visitor often leads to very deep stacks.  If it happens to be a
// real problem, the trampoline method of tail call elimination could be used.
/**
 *
 */
public final class EvaluateVisitor implements NodeVisitor {
    private IRuby runtime;
    private ThreadContext threadContext;

    private IRubyObject self;
    private IRubyObject result;

    public EvaluateVisitor(IRuby runtime, IRubyObject self) {
        this.runtime = runtime;
        this.threadContext = runtime.getCurrentContext();
        this.self = self;
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

    private void callTraceFunction(String event, IRubyObject zelf) {
        String name = threadContext.getCurrentFrame().getLastFunc();
        RubyModule type = threadContext.getCurrentFrame().getLastClass();
        runtime.callTraceFunction(event, threadContext.getPosition(), zelf, name, type);
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
        		throw runtime.newSystemStackError("stack level too deep");
        	}
        }
        return result;
    }

    /**
     * @see NodeVisitor#visitAliasNode(AliasNode)
     */
    public void visitAliasNode(AliasNode iVisited) {
        if (threadContext.getRubyClass() == null) {
            throw runtime.newTypeError("no class to make alias");
        }

        threadContext.getRubyClass().defineAlias(iVisited.getNewName(), iVisited.getOldName());
        threadContext.getRubyClass().callMethod("method_added", runtime.newSymbol(iVisited.getNewName()));
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
        assert false;
    }

    /**
     * @see NodeVisitor#visitArgsCatNode(ArgsCatNode)
     */
    public void visitArgsCatNode(ArgsCatNode iVisited) {
        IRubyObject args = eval(iVisited.getFirstNode());
        IRubyObject secondArgs = splatValue(eval(iVisited.getSecondNode()));
        RubyArray list = args instanceof RubyArray ? (RubyArray) args :
            runtime.newArray(args);
        
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
        
        result = runtime.newArray(list);
    }

    /**
     * @see NodeVisitor#visitBackRefNode(BackRefNode)
     */
    public void visitBackRefNode(BackRefNode iVisited) {
        IRubyObject backref = threadContext.getBackref();
        switch (iVisited.getType()) {
	        case '~' :
	            result = backref;
	            break;
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
        assert false;
    }

    /**
     * @see NodeVisitor#visitBlockNode(BlockNode)
     */
    public void visitBlockNode(BlockNode iVisited) {
        for (Iterator iter = iVisited.iterator(); iter.hasNext(); ) {
            eval((Node) iter.next());
        }
    }

    /**
     * @see NodeVisitor#visitBlockPassNode(BlockPassNode)
     */
    public void visitBlockPassNode(BlockPassNode iVisited) {
        IRubyObject proc = eval(iVisited.getBodyNode());

        if (proc.isNil()) {
            threadContext.getIterStack().push(Iter.ITER_NOT);
            try {
                eval(iVisited.getIterNode());
                return;
            } finally {
                threadContext.getIterStack().pop();
            }
        }
        
        // If not already a proc then we should try and make it one.
        if (!(proc instanceof RubyProc)) {
        	proc = proc.convertToType("Proc", "to_proc", false);
        	
        	if (!(proc instanceof RubyProc)) {
                throw runtime.newTypeError("wrong argument type " + proc.getMetaClass().getName() + " (expected Proc)");
        	}
        }

        // TODO: Add safety check for taintedness
        
        Block block = (Block) threadContext.getBlockStack().peek();
        if (block != null) {
            IRubyObject blockObject = block.getBlockObject();
            // The current block is already associated with the proc.  No need to create new
            // block for it.  Just eval!
            if (blockObject != null && blockObject == proc) {
        	    try {
            	    threadContext.getIterStack().push(Iter.ITER_PRE);
            	    eval(iVisited.getIterNode());
            	    return;
        	    } finally {
                    threadContext.getIterStack().pop();
        	    }
            }
        }

        threadContext.getBlockStack().push(((RubyProc) proc).getBlock());
        threadContext.getIterStack().push(Iter.ITER_PRE);
        
        if (threadContext.getCurrentFrame().getIter() == Iter.ITER_NOT) {
            threadContext.getCurrentFrame().setIter(Iter.ITER_PRE);
        }

        try {
            eval(iVisited.getIterNode());
        } finally {
            threadContext.getIterStack().pop();
            threadContext.getBlockStack().pop();
        }
    }

    /**
     * @see NodeVisitor#visitBreakNode(BreakNode)
     */
    public void visitBreakNode(BreakNode iVisited) {
        if (iVisited.getValueNode() != null) {
            throw new BreakJump(eval(iVisited.getValueNode()));
        }
        throw new BreakJump(runtime.getNil());
    }

    /**
     * @see NodeVisitor#visitConstDeclNode(ConstDeclNode)
     */
    public void visitConstDeclNode(ConstDeclNode iVisited) {
        if (threadContext.getRubyClass() == null) {
            throw runtime.newTypeError("no class/module to define constant");
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
            throw runtime.newTypeError("no class/module to define class variable");
        }
        eval(iVisited.getValueNode());
        threadContext.getRubyClass().setClassVar(iVisited.getName(), result);
    }

    /**
     * @see NodeVisitor#visitClassVarNode(ClassVarNode)
     */
    public void visitClassVarNode(ClassVarNode iVisited) {
    	RubyModule rubyClass = threadContext.getRubyClass();
    	
        if (rubyClass == null) {
            result = self.getMetaClass().getClassVar(iVisited.getName());
        } else if (! rubyClass.isSingleton()) {
        	result = rubyClass.getClassVar(iVisited.getName());
        } else {
            RubyModule module = (RubyModule) rubyClass.getInstanceVariable("__attached__");
            	
            if (module != null) {
                result = module.getClassVar(iVisited.getName());
            }
        }
    }

    /**
     * @see NodeVisitor#visitCallNode(CallNode)
     */
    public void visitCallNode(CallNode iVisited) {
        Block tmpBlock = threadContext.beginCallArgs();
        IRubyObject receiver = null;
        IRubyObject[] args;
        try {
            receiver = eval(iVisited.getReceiverNode());
            args = setupArgs(runtime, threadContext, iVisited.getArgsNode());
        } finally {
        	threadContext.endCallArgs(tmpBlock);
        }
        assert receiver.getMetaClass() != null : receiver.getClass().getName();
        
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
        
        Node node = iVisited.getFirstWhenNode();
        while (node != null) {
            if (!(node instanceof WhenNode)) {
                eval(node);
                break;
            }

            WhenNode whenNode = (WhenNode) node;

            if (whenNode.getExpressionNodes() instanceof ArrayNode) {
		        for (Iterator iter = ((ArrayNode) whenNode.getExpressionNodes()).iterator(); iter.hasNext(); ) {
		            Node tag = (Node) iter.next();

                    threadContext.setPosition(tag.getPosition());
                    if (isTrace()) {
                        callTraceFunction("line", self);
                    }

                    // Ruby grammar has nested whens in a case body because of
                    // productions case_body and when_args.
            	    if (tag instanceof WhenNode) {
            		    RubyArray expressions = (RubyArray) eval(((WhenNode) tag).getExpressionNodes());
                    
                        for (int j = 0; j < expressions.getLength(); j++) {
                    	    IRubyObject condition = expressions.entry(j);
                    	
                            if ((expression != null && 
                        	    condition.callMethod("===", expression).isTrue()) || 
							    (expression == null && condition.isTrue())) {
                                 eval(((WhenNode) node).getBodyNode());
                                 return;
                            }
                        }
                        continue;
            	    }

                    eval(tag);
                    
                    if ((expression != null && result.callMethod("===", expression).isTrue()) ||
                        (expression == null && result.isTrue())) {
                        eval(whenNode.getBodyNode());
                        return;
                    }
                }
	        } else {
                eval(whenNode.getExpressionNodes());

                if ((expression != null && result.callMethod("===", expression).isTrue())
                    || (expression == null && result.isTrue())) {
                    eval(((WhenNode) node).getBodyNode());
                    return;
                }
            }
            
            node = whenNode.getNextCase();
        }
    }

    /**
     * @see NodeVisitor#visitClassNode(ClassNode)
     */
    public void visitClassNode(ClassNode iVisited) {
        RubyClass superClass = getSuperClassFromNode(iVisited.getSuperNode());
        Node classNameNode = iVisited.getCPath();
        String name = ((INameNode) classNameNode).getName();
        RubyModule enclosingClass = getEnclosingModule(classNameNode);
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
        RubyClass superClazz;
        try {
            superClazz = (RubyClass) eval(superNode);
        } catch (Exception e) {
            if (superNode instanceof INameNode) {
                String name = ((INameNode) superNode).getName();
                throw runtime.newTypeError(
                                    "undefined superclass '" + name + "'");
            }
			throw runtime.newTypeError("superclass undefined");
        }
        if (superClazz instanceof MetaClass) {
            throw runtime.newTypeError("can't make subclass of virtual class");
        }
        return superClazz;
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
            result = runtime.getObject().getConstant(iVisited.getName());
        } else {
            eval(iVisited.getLeftNode());
            if (result instanceof RubyModule) {
                result = ((RubyModule) result).getConstantAtOrConstantMissing(iVisited.getName());
            } else {
                result = result.callMethod(iVisited.getName());
            }
        }
    }

    /**
     * @see NodeVisitor#visitColon3Node(Colon3Node)
     */
    public void visitColon3Node(Colon3Node iVisited) {
        result = runtime.getObject().getConstant(iVisited.getName());
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

        result = runtime.newString(sb.toString());
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

        result = runtime.newSymbol(sb.toString());
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

        result = self.callMethod("`", runtime.newString(sb.toString()));
    }

    /**
     * @see NodeVisitor#visitDefinedNode(DefinedNode)
     */
    public void visitDefinedNode(DefinedNode iVisited) {
        String def = new DefinedVisitor(runtime, self).getDefinition(iVisited.getExpressionNode());
        if (def != null) {
            result = runtime.newString(def);
        }
    }

    /**
     * @see NodeVisitor#visitDefnNode(DefnNode)
     */
    public void visitDefnNode(DefnNode iVisited) {
        RubyModule containingClass = threadContext.getRubyClass();
        if (containingClass == null) {
            throw runtime.newTypeError("No class to add method.");
        }

        String name = iVisited.getName();
        if (containingClass == runtime.getObject() && name.equals("initialize")) {
            runtime.getWarnings().warn("redefining Object#initialize may cause infinite loop");
        }

        Visibility visibility = threadContext.getCurrentVisibility();
        if (name.equals("initialize") || visibility.isModuleFunction()) {
            visibility = Visibility.PRIVATE;
        } else if (visibility.isPublic() && containingClass == runtime.getObject()) {
            visibility = iVisited.getVisibility();
        }

        DefaultMethod newMethod = new DefaultMethod(containingClass, iVisited.getBodyNode(),
                                                    (ArgsNode) iVisited.getArgsNode(),
                                                    visibility,
													threadContext.getRubyClass());
        
        iVisited.getBodyNode().accept(new CreateJumpTargetVisitor(newMethod));
        
        containingClass.addMethod(name, newMethod);

        if (threadContext.getCurrentVisibility().isModuleFunction()) {
            containingClass.getSingletonClass().addMethod(name, new WrapperCallable(containingClass.getSingletonClass(), newMethod, Visibility.PUBLIC));
            containingClass.callMethod("singleton_method_added", runtime.newSymbol(name));
        }

		// 'class << self' and 'class << obj' uses defn as opposed to defs
        if (containingClass.isSingleton()) {
			((MetaClass)containingClass).getAttachedObject().callMethod("singleton_method_added", runtime.newSymbol(iVisited.getName()));
        } else {
        	containingClass.callMethod("method_added", runtime.newSymbol(name));
        }
    }


    /**
     * @see NodeVisitor#visitDefsNode(DefsNode)
     */
    public void visitDefsNode(DefsNode iVisited) {
        IRubyObject receiver = eval(iVisited.getReceiverNode());

        if (runtime.getSafeLevel() >= 4 && !receiver.isTaint()) {
            throw runtime.newSecurityError("Insecure; can't define singleton method.");
        }
        if (receiver.isFrozen()) {
            throw runtime.newFrozenError("object");
        }
        if (! receiver.singletonMethodsAllowed()) {
            throw runtime.newTypeError(
                                "can't define singleton method \"" +
                                iVisited.getName() +
                                "\" for " +
                                receiver.getType());
        }

        RubyClass rubyClass = receiver.getSingletonClass();

        if (runtime.getSafeLevel() >= 4) {
            ICallable method = (ICallable) rubyClass.getMethods().get(iVisited.getName());
            if (method != null) {
                throw runtime.newSecurityError("Redefining method prohibited.");
            }
        }

        DefaultMethod newMethod = new DefaultMethod(rubyClass, iVisited.getBodyNode(),
                                                    (ArgsNode) iVisited.getArgsNode(),
                                                    Visibility.PUBLIC,
													threadContext.getRubyClass());

        iVisited.getBodyNode().accept(new CreateJumpTargetVisitor(newMethod));

        rubyClass.addMethod(iVisited.getName(), newMethod);
        receiver.callMethod("singleton_method_added", runtime.newSymbol(iVisited.getName()));

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
        Block tmpBlock = threadContext.beginCallArgs();
        IRubyObject[] args;
        try {
            args = setupArgs(runtime, threadContext, iVisited.getArgsNode());
        } finally {
        	threadContext.endCallArgs(tmpBlock);
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
            if (! runtime.getCurrentScope().getValue(iVisited.getCount()).isTrue()) {
                //Benoit: I don't understand why the result is inversed
                result = eval(iVisited.getBeginNode()).isTrue() ? runtime.getFalse() : runtime.getTrue();
                runtime.getCurrentScope().setValue(iVisited.getCount(), result);
            } else {
                if (eval(iVisited.getEndNode()).isTrue()) {
                    runtime.getCurrentScope().setValue(iVisited.getCount(), runtime.getFalse());
                }
                result = runtime.getTrue();
            }
        } else {
            if (! runtime.getCurrentScope().getValue(iVisited.getCount()).isTrue()) {
                if (eval(iVisited.getBeginNode()).isTrue()) {
                    //Benoit: I don't understand why the result is inversed
                    runtime.getCurrentScope().setValue(iVisited.getCount(), eval(iVisited.getEndNode()).isTrue() ? runtime.getFalse() : runtime.getTrue());
                    result = runtime.getTrue();
                } else {
                    result = runtime.getFalse();
                }
            } else {
                if (eval(iVisited.getEndNode()).isTrue()) {
                    runtime.getCurrentScope().setValue(iVisited.getCount(), runtime.getFalse());
                }
                result = runtime.getTrue();
            }
        }
    }

    /**
     * @see NodeVisitor#visitForNode(ForNode)
     */
    public void visitForNode(ForNode iVisited) {
    	threadContext.getBlockStack().push(Block.createBlock(iVisited.getVarNode(), new EvaluateCallable(iVisited.getBodyNode(), iVisited.getVarNode()), self));
        threadContext.getIterStack().push(Iter.ITER_PRE);

        try {
            while (true) {
                try {
                    ISourcePosition position = threadContext.getPosition();
                    Block tmpBlock = threadContext.beginCallArgs();

                    IRubyObject recv = null;
                    try {
                        recv = eval(iVisited.getIterNode());
                    } finally {
                        threadContext.setPosition(position);
                        threadContext.endCallArgs(tmpBlock);
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
    	IRubyObject variable = self.getInstanceVariable(iVisited.getName());
    	
        result = variable == null ? runtime.getNil() : variable;
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
    	threadContext.getBlockStack().push(Block.createBlock(iVisited.getVarNode(), 
    	    new EvaluateCallable(iVisited.getBodyNode(), iVisited.getVarNode()), self));
        try {
            while (true) {
                try {
                    threadContext.getIterStack().push(Iter.ITER_PRE);
                    result = eval(iVisited.getIterNode());
                    return;
                } catch (RetryJump rExcptn) {
                } finally {
                    threadContext.getIterStack().pop();
                }
            }
        } catch (BreakJump bExcptn) {
            IRubyObject breakValue = bExcptn.getBreakValue();

            result = breakValue == null ? runtime.getNil() : breakValue;
        } finally {
            threadContext.getBlockStack().pop();
        }
    }

    /**
     * @see NodeVisitor#visitLocalAsgnNode(LocalAsgnNode)
     */
    public void visitLocalAsgnNode(LocalAsgnNode iVisited) {
        eval(iVisited.getValueNode());
        runtime.getCurrentScope().setValue(iVisited.getCount(), result);
    }

    /**
     * @see NodeVisitor#visitLocalVarNode(LocalVarNode)
     */
    public void visitLocalVarNode(LocalVarNode iVisited) {
        result = runtime.getCurrentScope().getValue(iVisited.getCount());
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
        Node classNameNode = iVisited.getCPath();
        String name = ((INameNode) classNameNode).getName();
        RubyModule enclosingModule = getEnclosingModule(classNameNode);

        if (enclosingModule == null) {
            throw runtime.newTypeError("no outer class/module");
        }

        RubyModule module;
        if (enclosingModule == runtime.getObject()) {
            module = runtime.getOrCreateModule(name);
        } else {
            module = enclosingModule.defineModuleUnder(name);
        }
        evalClassDefinitionBody(iVisited.getBodyNode(), module);
    }

    /**
     * @see NodeVisitor#visitNewlineNode(NewlineNode)
     */
    public void visitNewlineNode(NewlineNode iVisited) {
        threadContext.setPosition(iVisited.getPosition());
        
        if (isTrace()) {
           callTraceFunction("line", self);
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

        IRubyObject[] args = setupArgs(runtime, threadContext, iVisited.getArgsNode());

        IRubyObject firstValue = receiver.callMethod("[]", args);

        if (iVisited.getOperatorName().equals("||")) {
            if (firstValue.isTrue()) {
                result = firstValue;
                return;
            }
			firstValue = eval(iVisited.getValueNode());
        } else if (iVisited.getOperatorName().equals("&&")) {
            if (!firstValue.isTrue()) {
                result = firstValue;
                return;
            }
			firstValue = eval(iVisited.getValueNode());
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
            }
			value = eval(iVisited.getValueNode());
        } else if (iVisited.getOperatorName().equals("&&")) {
            if (!value.isTrue()) {
                result = value;
                return;
            }
			value = eval(iVisited.getValueNode());
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
                // TODO: Rubicon TestKernel dies without this line.  A cursory glance implies we
                // falsely set $! to nil and this sets it back to something valid.  This should 
                // get fixed at the same time we address bug #1296484.
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
        throw new ReturnJump(eval(iVisited.getValueNode()), iVisited.getTarget());
    }

    /**
     * @see NodeVisitor#visitSClassNode(SClassNode)
     */
    public void visitSClassNode(SClassNode iVisited) {
        IRubyObject receiver = eval(iVisited.getReceiverNode());

        RubyClass singletonClass;

        if (receiver.isNil()) {
            singletonClass = runtime.getClass("NilClass");
        } else if (receiver == runtime.getTrue()) {
            singletonClass = runtime.getClass("True");
        } else if (receiver == runtime.getFalse()) {
            singletonClass = runtime.getClass("False");
        } else {
            if (runtime.getSafeLevel() >= 4 && !receiver.isTaint()) {
                throw runtime.newSecurityError("Insecure: can't extend object.");
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
        threadContext.getScopeStack().push(new Scope(runtime, iVisited.getLocalNames()));
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
        result = runtime.newString(iVisited.getValue());
    }

    /**
     * @see NodeVisitor#visitSValueNode(SValueNode)
     */
    public void visitSValueNode(SValueNode iVisited) {
        result = aValueSplat(eval(iVisited.getValue()));
    }

    /**
     * @see NodeVisitor#visitSuperNode(SuperNode)
     */
    public void visitSuperNode(SuperNode iVisited) {
        if (threadContext.getCurrentFrame().getLastClass() == null) {
            throw runtime.newNameError("Superclass method '" + threadContext.getCurrentFrame().getLastFunc() + "' disabled.");
        }

        Block tmpBlock = threadContext.beginCallArgs();

        IRubyObject[] args = null;
        try {
            args = setupArgs(runtime, threadContext, iVisited.getArgsNode());
        } finally {
        	threadContext.endCallArgs(tmpBlock);
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
            throw runtime.newTypeError("No class to undef method '" + iVisited.getName() + "'.");
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
        assert false;
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
        result = self.callMethod("`", runtime.newString(iVisited.getValue()));
    }

    /**
     * @see NodeVisitor#visitYieldNode(YieldNode)
     */
    public void visitYieldNode(YieldNode iVisited) {
        eval(iVisited.getArgsNode());

	// Special Hack...We cannot tell between no args and a nil one.
	// Change it back to null for now until a better solution is 
	// found
	// TODO: Find better way of differing...
	if (iVisited.getArgsNode() == null) {
	    result = null;
	}
        
        result = threadContext.yield(result, null, null, false, iVisited.getCheckState());
    }

    /**
     * @see NodeVisitor#visitZArrayNode(ZArrayNode)
     */
    public void visitZArrayNode(ZArrayNode iVisited) {
        result = runtime.newArray();
    }

    /**
     * @see NodeVisitor#visitZSuperNode(ZSuperNode)
     */
    public void visitZSuperNode(ZSuperNode iVisited) {
        result = threadContext.callSuper();
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
        result = runtime.newFixnum(iVisited.getValue());
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
        result = RubyRegexp.newRegexp(runtime.newString(iVisited.getValue()), iVisited.getOptions(), null);
    }

    /**
     * @see NodeVisitor#visitSymbolNode(SymbolNode)
     */
    public void visitSymbolNode(SymbolNode iVisited) {
        result = runtime.newSymbol(iVisited.getName());
    }

    /** Evaluates the body in a class or module definition statement.
     *
     */
    private void evalClassDefinitionBody(ScopeNode iVisited, RubyModule type) {
		RubyModule oldParent = threadContext.setRubyClass(type); 
        threadContext.getFrameStack().pushCopy();
        threadContext.getScopeStack().push(new Scope(runtime, iVisited.getLocalNames()));
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
            threadContext.setRubyClass(oldParent);
            threadContext.getFrameStack().pop();

            if (isTrace()) {
                callTraceFunction("end", null);
            }
        }
    }

    private boolean isRescueHandled(RubyException currentException, ListNode exceptionNodes) {
        if (exceptionNodes == null) {
            return currentException.isKindOf(runtime.getClass("StandardError"));
        }

        Block tmpBlock = threadContext.beginCallArgs();

        IRubyObject[] args = null;
        try {
            args = setupArgs(runtime, threadContext, exceptionNodes);
        } finally {
        	threadContext.endCallArgs(tmpBlock);
        }

        for (int i = 0; i < args.length; i++) {
            if (! args[i].isKindOf(runtime.getClass("Module"))) {
                throw runtime.newTypeError("class or module required for rescue clause");
            }
            if (args[i].callMethod("===", currentException).isTrue())
                return true;
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
        
        return runtime.newArray(value);
    }
    
    private IRubyObject splatValue(IRubyObject value) {
        if (value.isNil()) {
            return runtime.newArray(value);
        }
        
        return arrayValue(value);
    }

    private IRubyObject aValueSplat(IRubyObject value) {
        if (!(value instanceof RubyArray) ||
            ((RubyArray) value).length().getLongValue() == 0) {
            return runtime.getNil();
        }
        
        RubyArray array = (RubyArray) value;
        
        return array.getLength() == 1 ? array.first(IRubyObject.NULL_ARRAY) : array;
    }

    /* HACK: .... */
    private RubyArray arrayValue(IRubyObject value) {
        IRubyObject newValue = value.convertToType("Array", "to_ary", false);

        if (newValue.isNil()) {
            // XXXEnebo: We should call to_a except if it is kernel def....
            // but we will forego for now.
            newValue = runtime.newArray(value);
        }
        
        return (RubyArray) newValue;
    }
    
    private IRubyObject[] setupArgs(IRuby runtime, ThreadContext context, Node node) {
        if (node == null) {
            return IRubyObject.NULL_ARRAY;
        }

        if (node instanceof ArrayNode) {
        	ISourcePosition position = context.getPosition();
            ArrayList list = new ArrayList(((ArrayNode) node).size());
            
            for (Iterator iter=((ArrayNode)node).iterator(); iter.hasNext();){
                final Node next = (Node) iter.next();
                if (next instanceof SplatNode) {
                    list.addAll(((RubyArray) eval(next)).getList());
                } else {
                    list.add(eval(next));
                }
            }

            context.setPosition(position);

            return (IRubyObject[]) list.toArray(new IRubyObject[list.size()]);
        }

        return ArgsUtil.arrayify(eval(node));
    }

    private RubyModule getEnclosingModule(Node node) {
        RubyModule enclosingModule = null;
        
        if (node instanceof Colon2Node) {
        	eval(((Colon2Node) node).getLeftNode());
        	
        	if (result != null && !result.isNil()) {
        		enclosingModule = (RubyModule) result;
        	}
        } 
        
        if (enclosingModule == null) {
        	enclosingModule = threadContext.getRubyClass();
        }

        return enclosingModule;
    }
}
