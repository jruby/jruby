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
 * Copyright (C) 2004-2005 Charles O Nutter <headius@headius.com>
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
import java.util.List;
import java.util.ListIterator;

import org.jruby.IRuby;
import org.jruby.MetaClass;
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
import org.jruby.ast.BinaryOperatorNode;
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
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.DefaultMethod;
import org.jruby.internal.runtime.methods.EvaluateCallable;
import org.jruby.internal.runtime.methods.WrapperCallable;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.Frame;
import org.jruby.runtime.ICallable;
import org.jruby.runtime.Iter;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

// TODO this visitor often leads to very deep stacks.  If it happens to be a
// real problem, the trampoline method of tail call elimination could be used.
/**
 *
 */
public final class EvaluateVisitor implements NodeVisitor {
	private static final EvaluateVisitor evaluator = new EvaluateVisitor();
	
    public static EvaluateVisitor createVisitor() {
        return evaluator;
    }

    /**
     * Helper method.
     *
     * test if a trace function is avaiable.
     *
     */
    private static boolean isTrace(EvaluationState state) {
        return state.runtime.getTraceFunction() != null;
    }

    private static void callTraceFunction(EvaluationState state, String event, IRubyObject zelf) {
        String name = state.threadContext.getCurrentFrame().getLastFunc();
        RubyModule type = state.threadContext.getCurrentFrame().getLastClass();
        state.runtime.callTraceFunction(event, state.threadContext.getPosition(), zelf, name, type);
    }

    public IRubyObject eval(IRuby runtime, IRubyObject self, Node node) {
    	// new evaluation cycle gets new state; eval() is only called externally
    	EvaluationState state = new EvaluationState(runtime, this);
    	
    	state.setSelf(self);
    	state.setResult(runtime.getNil());
    	
    	return state.begin(node);
    }
    
    // Collapsing further needs method calls
    private static class AliasNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		AliasNode iVisited = (AliasNode)ctx;
            if (state.threadContext.getRubyClass() == null) {
            	throw state.runtime.newTypeError("no class to make alias");
            }

            state.threadContext.getRubyClass().defineAlias(iVisited.getNewName(), iVisited.getOldName());
            state.threadContext.getRubyClass().callMethod("method_added", state.runtime.newSymbol(iVisited.getNewName()));
    	}
    }
    private static final AliasNodeVisitor aliasNodeVisitor = new AliasNodeVisitor();
    
    // And nodes are collapsed completely
    private static class AndNodeImplVisitor implements Instruction {
		public void execute(EvaluationState state, InstructionContext ctx) {
			if (state.getResult().isTrue()) {
				state.addNodeInstruction(((BinaryOperatorNode)ctx).getSecondNode());
			}
		}
    }
    private static final AndNodeImplVisitor andNodeImplVisitor = new AndNodeImplVisitor();
    private static class AndNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		BinaryOperatorNode iVisited = (BinaryOperatorNode)ctx;
    		
    		// add in reverse order
    		state.addNodeInstruction((Node)iVisited, andNodeImplVisitor);
    		state.addNodeInstruction((Node)iVisited.getFirstNode());
    	}
    }
    // used also for OpAsgnAndNode
    private static final AndNodeVisitor andNodeVisitor = new AndNodeVisitor();
    
    // Collapsing will require multiple results to be collected between visitors
    private static class ArgsCatNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		ArgsCatNode iVisited = (ArgsCatNode)ctx;
            IRubyObject args = state.begin(iVisited.getFirstNode());
            IRubyObject secondArgs = splatValue(state, state.begin(iVisited.getSecondNode()));
            RubyArray list = args instanceof RubyArray ? (RubyArray) args :
                state.runtime.newArray(args);
            
            state.setResult(list.concat(secondArgs)); 
    	}
    }
    private static final ArgsCatNodeVisitor argsCatNodeVisitor = new ArgsCatNodeVisitor();
    
    // Collapsing requires carrying a list through all visits (or adding array code to eval())
    private static class ArrayNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		ArrayNode iVisited = (ArrayNode)ctx;
            ArrayList list = new ArrayList(iVisited.size());

            for (Iterator iterator = iVisited.iterator(); iterator.hasNext();) {
                list.add(state.begin((Node) iterator.next()));
            }
            
            state.setResult(state.runtime.newArray(list));
    	}
    }
    private static final ArrayNodeVisitor arrayNodeVisitor = new ArrayNodeVisitor();
    
    // Collapsing requires a way to break out method calls
    private static class BackRefNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		BackRefNode iVisited = (BackRefNode)ctx;
            IRubyObject backref = state.threadContext.getBackref();
            switch (iVisited.getType()) {
    	        case '~' :
    	            state.setResult(backref);
    	            break;
            	case '&' :
                    state.setResult(RubyRegexp.last_match(backref));
                    break;
                case '`' :
                    state.setResult(RubyRegexp.match_pre(backref));
                    break;
                case '\'' :
                    state.setResult(RubyRegexp.match_post(backref));
                    break;
                case '+' :
                    state.setResult(RubyRegexp.match_last(backref));
                    break;
            }
    	}
    }
    private static final BackRefNodeVisitor backRefNodeVisitor = new BackRefNodeVisitor();
    
    // Collapsed
    private static class BeginNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		BeginNode iVisited = (BeginNode)ctx;
    		state.addNodeInstruction(iVisited.getBodyNode());
    	}
    }
    private static final BeginNodeVisitor beginNodeVisitor = new BeginNodeVisitor();

    // Collapsed
    private static class BlockNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		BlockNode iVisited = (BlockNode)ctx;
            for (ListIterator iter = iVisited.reverseIterator(); iter.hasPrevious(); ) {
                state.addNodeInstruction((Node)iter.previous());
            }
    	}
    }
    private static final BlockNodeVisitor blockNodeVisitor = new BlockNodeVisitor();
    
    // Big; collapsing will require multiple visitors
    private static class BlockPassNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		BlockPassNode iVisited = (BlockPassNode)ctx;
            IRubyObject proc = state.begin(iVisited.getBodyNode());

            if (proc.isNil()) {
                state.threadContext.pushIter(Iter.ITER_NOT);
                try {
                    state.begin(iVisited.getIterNode());
                    return;
                } finally {
                    state.threadContext.popIter();
                }
            }
            
            // If not already a proc then we should try and make it one.
            if (!(proc instanceof RubyProc)) {
            	proc = proc.convertToType("Proc", "to_proc", false);
            	
            	if (!(proc instanceof RubyProc)) {
                    throw state.runtime.newTypeError("wrong argument type " + proc.getMetaClass().getName() + " (expected Proc)");
            	}
            }

            // TODO: Add safety check for taintedness
            
            Block block = (Block) state.threadContext.getBlockStack().peek();
            if (block != null) {
                IRubyObject blockObject = block.getBlockObject();
                // The current block is already associated with the proc.  No need to create new
                // block for it.  Just eval!
                if (blockObject != null && blockObject == proc) {
            	    try {
                	    state.threadContext.pushIter(Iter.ITER_PRE);
                	    state.begin(iVisited.getIterNode());
                	    return;
            	    } finally {
                        state.threadContext.popIter();
            	    }
                }
            }

            state.threadContext.getBlockStack().push(((RubyProc) proc).getBlock());
            state.threadContext.pushIter(Iter.ITER_PRE);
            
            if (state.threadContext.getCurrentFrame().getIter() == Iter.ITER_NOT) {
                state.threadContext.getCurrentFrame().setIter(Iter.ITER_PRE);
            }

            try {
                state.begin(iVisited.getIterNode());
            } finally {
                state.threadContext.popIter();
                state.threadContext.getBlockStack().pop();
            }
    	}
    }
    private static final BlockPassNodeVisitor blockPassNodeVisitor = new BlockPassNodeVisitor();
    
    // Not collapsed, will require exception handling
    private static class BreakNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		BreakNode iVisited = (BreakNode)ctx;
    		
            JumpException je = new JumpException(JumpException.JumpType.BreakJump);
            if (iVisited.getValueNode() != null) {
                je.setPrimaryData(state.begin(iVisited.getValueNode()));
            } else {
            	je.setPrimaryData(state.runtime.getNil());
            }
            throw je;
    	}
    }
    private static final BreakNodeVisitor breakNodeVisitor = new BreakNodeVisitor();
    
    // Collapsed, other than exception handling
    private static class ConstDeclNodeVisitor1 implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		ConstDeclNode iVisited = (ConstDeclNode)ctx;
            if (state.threadContext.getRubyClass() == null) {
            	// TODO: wire into new exception handling mechanism
                throw state.runtime.newTypeError("no class/module to define constant");
            }
            state.clearResult();
    		state.addNodeInstruction(ctx, constDeclNodeVisitor2);
            state.addNodeInstruction(iVisited.getValueNode());
    	}
    }
    private static final ConstDeclNodeVisitor1 constDeclNodeVisitor1 = new ConstDeclNodeVisitor1();
    private static class ConstDeclNodeVisitor2 implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		ConstDeclNode iVisited = (ConstDeclNode)ctx;
    		state.threadContext.getRubyClass().setConstant(iVisited.getName(), state.getResult());
    	}
    }
    private static final ConstDeclNodeVisitor2 constDeclNodeVisitor2 = new ConstDeclNodeVisitor2();
    private static class ConstDeclNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
            state.addNodeInstruction(ctx, constDeclNodeVisitor1);
    	}
    }
    private static final ConstDeclNodeVisitor constDeclNodeVisitor = new ConstDeclNodeVisitor();
    
    // Collapsed
    private static class ClassVarAsgnNodeVisitor1 implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		ClassVarAsgnNode iVisited = (ClassVarAsgnNode)ctx;
            state.threadContext.getRubyClass().setClassVar(iVisited.getName(), state.getResult());
    	}
    }
    private static final ClassVarAsgnNodeVisitor1 classVarAsgnNodeVisitor1 = new ClassVarAsgnNodeVisitor1();
    private static class ClassVarAsgnNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		ClassVarAsgnNode iVisited = (ClassVarAsgnNode)ctx;
    		state.addNodeInstruction(ctx, classVarAsgnNodeVisitor1);
            state.addNodeInstruction(iVisited.getValueNode());
    	}
    }
    private static final ClassVarAsgnNodeVisitor classVarAsgnNodeVisitor = new ClassVarAsgnNodeVisitor();
    
    // Collapsed
    private static class ClassVarDeclNodeVisitor1 implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		ClassVarDeclNode iVisited = (ClassVarDeclNode)ctx;
            if (state.threadContext.getRubyClass() == null) {
                throw state.runtime.newTypeError("no class/module to define class variable");
            }
            state.addNodeInstruction(ctx, classVarDeclNodeVisitor2);
            state.addNodeInstruction(iVisited.getValueNode());
    	}
    }
    private static final ClassVarDeclNodeVisitor1 classVarDeclNodeVisitor1 = new ClassVarDeclNodeVisitor1();
    private static class ClassVarDeclNodeVisitor2 implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		ClassVarDeclNode iVisited = (ClassVarDeclNode)ctx;
            state.threadContext.getRubyClass().setClassVar(iVisited.getName(), state.getResult());
    	}
    }
    private static final ClassVarDeclNodeVisitor2 classVarDeclNodeVisitor2 = new ClassVarDeclNodeVisitor2();
    private static class ClassVarDeclNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		state.clearResult();
    		state.addNodeInstruction(ctx, classVarDeclNodeVisitor1);
    	}
    }
    private static final ClassVarDeclNodeVisitor classVarDeclNodeVisitor = new ClassVarDeclNodeVisitor();
    
    // Not collapsed, but maybe nothing to be done?
    private static class ClassVarNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		ClassVarNode iVisited = (ClassVarNode)ctx;
        	RubyModule rubyClass = state.threadContext.getRubyClass();
        	
            if (rubyClass == null) {
                state.setResult(state.getSelf().getMetaClass().getClassVar(iVisited.getName()));
            } else if (! rubyClass.isSingleton()) {
            	state.setResult(rubyClass.getClassVar(iVisited.getName()));
            } else {
                RubyModule module = (RubyModule) rubyClass.getInstanceVariable("__attached__");
                	
                if (module != null) {
                    state.setResult(module.getClassVar(iVisited.getName()));
                }
            }
    	}
    }
    private static final ClassVarNodeVisitor classVarNodeVisitor = new ClassVarNodeVisitor();
    
    // Not collapsed; probably will depend on exception handling
    private static class CallNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		CallNode iVisited = (CallNode)ctx;
            Block tmpBlock = state.threadContext.beginCallArgs();
            IRubyObject receiver = null;
            IRubyObject[] args;
            try {
                receiver = state.begin(iVisited.getReceiverNode());
                args = setupArgs(state, state.runtime, state.threadContext, iVisited.getArgsNode());
            } finally {
            	state.threadContext.endCallArgs(tmpBlock);
            }
            assert receiver.getMetaClass() != null : receiver.getClass().getName();
            
            state.setResult(receiver.callMethod(iVisited.getName(), args, CallType.NORMAL));
    	}
    }
    private static final CallNodeVisitor callNodeVisitor = new CallNodeVisitor();
    
    // Not collapsed; it's a big'un, will take some work
    private static class CaseNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		CaseNode iVisited = (CaseNode)ctx;
            IRubyObject expression = null;
            if (iVisited.getCaseNode() != null) {
                expression = state.begin(iVisited.getCaseNode());
            }
            
            Node firstWhenNode = iVisited.getFirstWhenNode();
            while (firstWhenNode != null) {
                if (!(firstWhenNode instanceof WhenNode)) {
                    state.begin(firstWhenNode);
                    break;
                }

                WhenNode whenNode = (WhenNode) firstWhenNode;

                if (whenNode.getExpressionNodes() instanceof ArrayNode) {
    		        for (Iterator iter = ((ArrayNode) whenNode.getExpressionNodes()).iterator(); iter.hasNext(); ) {
    		            Node tag = (Node) iter.next();

                        state.threadContext.setPosition(tag.getPosition());
                        if (isTrace(state)) {
                            callTraceFunction(state, "line", state.getSelf());
                        }

                        // Ruby grammar has nested whens in a case body because of
                        // productions case_body and when_args.
                	    if (tag instanceof WhenNode) {
                		    RubyArray expressions = (RubyArray) state.begin(((WhenNode) tag).getExpressionNodes());
                        
                            for (int j = 0; j < expressions.getLength(); j++) {
                        	    IRubyObject condition = expressions.entry(j);
                        	
                                if ((expression != null && 
                            	    condition.callMethod("===", expression).isTrue()) || 
    							    (expression == null && condition.isTrue())) {
                                     state.begin(((WhenNode) firstWhenNode).getBodyNode());
                                     return;
                                }
                            }
                            continue;
                	    }

                        state.begin(tag);
                        
                        if ((expression != null && state.getResult().callMethod("===", expression).isTrue()) ||
                            (expression == null && state.getResult().isTrue())) {
                            state.begin(whenNode.getBodyNode());
                            return;
                        }
                    }
    	        } else {
                    state.begin(whenNode.getExpressionNodes());

                    if ((expression != null && state.getResult().callMethod("===", expression).isTrue())
                        || (expression == null && state.getResult().isTrue())) {
                        state.begin(((WhenNode) firstWhenNode).getBodyNode());
                        return;
                    }
                }
                
                firstWhenNode = whenNode.getNextCase();
            }
    	}
    }
    private static final CaseNodeVisitor caseNodeVisitor = new CaseNodeVisitor();
    
    // Not collapsed; another big one
    private static class ClassNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		ClassNode iVisited = (ClassNode)ctx;
            RubyClass superClass = getSuperClassFromNode(state, iVisited.getSuperNode());
            Node classNameNode = iVisited.getCPath();
            String name = ((INameNode) classNameNode).getName();
            RubyModule enclosingClass = getEnclosingModule(state, classNameNode);
            RubyClass rubyClass = enclosingClass.defineOrGetClassUnder(name, superClass);

            if (state.threadContext.getWrapper() != null) {
                rubyClass.extendObject(state.threadContext.getWrapper());
                rubyClass.includeModule(state.threadContext.getWrapper());
            }
            evalClassDefinitionBody(state, iVisited.getBodyNode(), rubyClass);
    	}
    	
    	private RubyClass getSuperClassFromNode(EvaluationState state, Node superNode) {
            if (superNode == null) {
                return null;
            }
            RubyClass superClazz;
            try {
                superClazz = (RubyClass) state.begin(superNode);
            } catch (Exception e) {
                if (superNode instanceof INameNode) {
                    String name = ((INameNode) superNode).getName();
                    throw state.runtime.newTypeError("undefined superclass '" + name + "'");
                }
    			throw state.runtime.newTypeError("superclass undefined");
            }
            if (superClazz instanceof MetaClass) {
                throw state.runtime.newTypeError("can't make subclass of virtual class");
            }
            return superClazz;
        }
    }
    private static final ClassNodeVisitor classNodeVisitor = new ClassNodeVisitor();
    
    // Collapsed, other than a method call
    private static class Colon2NodeVisitor1 implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		Colon2Node iVisited = (Colon2Node)ctx;
            if (state.getResult() instanceof RubyModule) {
                state.setResult(((RubyModule) state.getResult()).getConstantAtOrConstantMissing(iVisited.getName()));
            } else {
                state.setResult(state.getResult().callMethod(iVisited.getName()));
            }
    	}
    }
    private static final Colon2NodeVisitor1 colon2NodeVisitor1 = new Colon2NodeVisitor1();
    private static class Colon2NodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		Colon2Node iVisited = (Colon2Node)ctx;
            Node leftNode = iVisited.getLeftNode();

            // TODO: Made this more colon3 friendly because of cpath production
            // rule in grammar (it is convenient to think of them as the same thing
            // at a grammar level even though evaluation is).
            if (leftNode == null) {
                state.setResult(state.runtime.getObject().getConstant(iVisited.getName()));
            } else {
            	state.clearResult();
            	state.addNodeInstruction(ctx, colon2NodeVisitor1);
                state.addNodeInstruction(iVisited.getLeftNode());
            }
    	}
    }
    private static final Colon2NodeVisitor colon2NodeVisitor = new Colon2NodeVisitor();

    // No collapsing to do (depending on getConstant())
    private static class Colon3NodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		Colon3Node iVisited = (Colon3Node)ctx;
            state.setResult(state.runtime.getObject().getConstant(iVisited.getName()));
    	}
    }
    private static final Colon3NodeVisitor colon3NodeVisitor = new Colon3NodeVisitor();
    
    // No collapsing to do (depending on getConstant())
    private static class ConstNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		ConstNode iVisited = (ConstNode)ctx;
            state.setResult(state.threadContext.getRubyClass().getConstant(iVisited.getName()));
    	}
    }
    private static final ConstNodeVisitor constNodeVisitor = new ConstNodeVisitor();
    
    // Collapsed
    private static class DAsgnNodeVisitor1 implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		DAsgnNode iVisited = (DAsgnNode)ctx;
            state.threadContext.getCurrentDynamicVars().set(iVisited.getName(), state.getResult());
    	}
    }
    private static final DAsgnNodeVisitor1 dAsgnNodeVisitor1 = new DAsgnNodeVisitor1();
    private static class DAsgnNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		DAsgnNode iVisited = (DAsgnNode)ctx;
    		state.clearResult();
    		state.addNodeInstruction(ctx, dAsgnNodeVisitor1);
            state.addNodeInstruction(iVisited.getValueNode());
    	}
    }
    private static final DAsgnNodeVisitor dAsgnNodeVisitor = new DAsgnNodeVisitor();
    
    // Not collapsed; requires carrying a StringBuffer across visitors
    private static class DRegexpNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		DRegexpNode iVisited = (DRegexpNode)ctx;
            StringBuffer sb = new StringBuffer();

            Iterator iterator = iVisited.iterator();
            while (iterator.hasNext()) {
                Node iterNode = (Node) iterator.next();
                sb.append(state.begin(iterNode));
            }

            state.setResult(RubyRegexp.newRegexp(state.runtime, sb.toString(), iVisited.getOptions(), null));
    	}
    }
    private static final DRegexpNodeVisitor dRegexpNodeVisitor = new DRegexpNodeVisitor();
    
    // Not collapsed; requires carrying a StringBuffer across visitors
    private static class DStrNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		DStrNode iVisited = (DStrNode)ctx;
            StringBuffer sb = new StringBuffer();

            Iterator iterator = iVisited.iterator();
            while (iterator.hasNext()) {
                Node iterNode = (Node) iterator.next();
                sb.append(state.begin(iterNode));
            }

            state.setResult(state.runtime.newString(sb.toString()));
    	}
    }
    private static final DStrNodeVisitor dStrNodeVisitor = new DStrNodeVisitor();
    
    // Not collapsed; requires carrying a StringBuffer across visitors
    private static class DSymbolNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		DSymbolNode iVisited = (DSymbolNode)ctx;
            StringBuffer sb = new StringBuffer();

            for (Iterator iterator = iVisited.getNode().iterator(); 
            	iterator.hasNext();) {
                Node iterNode = (Node) iterator.next();
                sb.append(state.begin(iterNode));
            }

            state.setResult(state.runtime.newSymbol(sb.toString()));
    	}
    }
    private static final DSymbolNodeVisitor dSymbolNodeVisitor = new DSymbolNodeVisitor();

    // Maybe nothing to do
    private static class DVarNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		DVarNode iVisited = (DVarNode)ctx;
            state.setResult(state.threadContext.getDynamicValue(iVisited.getName()));
    	}
    }
    private static final DVarNodeVisitor dVarNodeVisitor = new DVarNodeVisitor();
    
    // Not collapsed; requires carrying a StringBuffer across visitors
    private static class DXStrNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		DXStrNode iVisited = (DXStrNode)ctx;
            StringBuffer sb = new StringBuffer();

            Iterator iterator = iVisited.iterator();
            while (iterator.hasNext()) {
                Node iterNode = (Node) iterator.next();
                sb.append(state.begin(iterNode));
            }

            state.setResult(state.getSelf().callMethod("`", state.runtime.newString(sb.toString())));
    	}
    }
    private static final DXStrNodeVisitor dXStrNodeVisitor = new DXStrNodeVisitor();
    
    // Not collapsed; calls out to DefinedVisitor
    private static class DefinedNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		DefinedNode iVisited = (DefinedNode)ctx;
            String def = new DefinedVisitor(state.runtime, state.getSelf()).getDefinition(iVisited.getExpressionNode());
            if (def != null) {
                state.setResult(state.runtime.newString(def));
            }
    	}
    }
    private static final DefinedNodeVisitor definedNodeVisitor = new DefinedNodeVisitor();
    
    // Not collapsed; big
    private static class DefnNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		DefnNode iVisited = (DefnNode)ctx;
            RubyModule containingClass = state.threadContext.getRubyClass();
            if (containingClass == null) {
                throw state.runtime.newTypeError("No class to add method.");
            }

            String name = iVisited.getName();
            if (containingClass == state.runtime.getObject() && name.equals("initialize")) {
                state.runtime.getWarnings().warn("redefining Object#initialize may cause infinite loop");
            }

            Visibility visibility = state.threadContext.getCurrentVisibility();
            if (name.equals("initialize") || visibility.isModuleFunction()) {
                visibility = Visibility.PRIVATE;
            } else if (visibility.isPublic() && containingClass == state.runtime.getObject()) {
                visibility = iVisited.getVisibility();
            }

            DefaultMethod newMethod = new DefaultMethod(containingClass, iVisited.getBodyNode(),
                                                        (ArgsNode) iVisited.getArgsNode(),
                                                        visibility,
    													state.threadContext.getRubyClass());
            
            iVisited.getBodyNode().accept(new CreateJumpTargetVisitor(newMethod));
            
            containingClass.addMethod(name, newMethod);

            if (state.threadContext.getCurrentVisibility().isModuleFunction()) {
                containingClass.getSingletonClass().addMethod(name, new WrapperCallable(containingClass.getSingletonClass(), newMethod, Visibility.PUBLIC));
                containingClass.callMethod("singleton_method_added", state.runtime.newSymbol(name));
            }

    		// 'class << state.self' and 'class << obj' uses defn as opposed to defs
            if (containingClass.isSingleton()) {
    			((MetaClass)containingClass).getAttachedObject().callMethod("singleton_method_added", state.runtime.newSymbol(iVisited.getName()));
            } else {
            	containingClass.callMethod("method_added", state.runtime.newSymbol(name));
            }
    	}
    }
    private static final DefnNodeVisitor defnNodeVisitor = new DefnNodeVisitor();

    // Not collapsed; big
    private static class DefsNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		DefsNode iVisited = (DefsNode)ctx;
            IRubyObject receiver = state.begin(iVisited.getReceiverNode());

            if (state.runtime.getSafeLevel() >= 4 && !receiver.isTaint()) {
                throw state.runtime.newSecurityError("Insecure; can't define singleton method.");
            }
            if (receiver.isFrozen()) {
                throw state.runtime.newFrozenError("object");
            }
            if (! receiver.singletonMethodsAllowed()) {
                throw state.runtime.newTypeError("can't define singleton method \"" +
                                    iVisited.getName() +
                                    "\" for " +
                                    receiver.getType());
            }

            RubyClass rubyClass = receiver.getSingletonClass();

            if (state.runtime.getSafeLevel() >= 4) {
                ICallable method = (ICallable) rubyClass.getMethods().get(iVisited.getName());
                if (method != null) {
                    throw state.runtime.newSecurityError("Redefining method prohibited.");
                }
            }

            DefaultMethod newMethod = new DefaultMethod(rubyClass, iVisited.getBodyNode(),
                                                        (ArgsNode) iVisited.getArgsNode(),
                                                        Visibility.PUBLIC,
    													state.threadContext.getRubyClass());

            iVisited.getBodyNode().accept(new CreateJumpTargetVisitor(newMethod));

            rubyClass.addMethod(iVisited.getName(), newMethod);
            receiver.callMethod("singleton_method_added", state.runtime.newSymbol(iVisited.getName()));

            state.clearResult();
    	}
    }
    private static final DefsNodeVisitor defsNodeVisitor = new DefsNodeVisitor();
    
    // Not collapsed; requires carrying two results
    private static class DotNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		DotNode iVisited = (DotNode)ctx;
            state.setResult(RubyRange.newRange(state.runtime, state.begin(iVisited.getBeginNode()), state.begin(iVisited.getEndNode()), iVisited.isExclusive()));
    	}
    }
    private static final DotNodeVisitor dotNodeVisitor = new DotNodeVisitor();
    
    // Not collapsed; needs exception handling
    private static class EnsureNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		EnsureNode iVisited = (EnsureNode)ctx;
            try {
                state.setResult(state.begin(iVisited.getBodyNode()));
            } finally {
                if (iVisited.getEnsureNode() != null) {
                    IRubyObject oldresult = state.getResult();
                    state.begin(iVisited.getEnsureNode());
                    state.setResult(oldresult);
                }
            }
    	}
    }
    private static final EnsureNodeVisitor ensureNodeVisitor = new EnsureNodeVisitor();
    
    // Collapsed
    private static class EvStrNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		EvStrNode iVisited = (EvStrNode)ctx;
    		state.clearResult();
    		state.addNodeInstruction(iVisited.getBody());
    	}
    }
    private static final EvStrNodeVisitor evStrNodeVisitor = new EvStrNodeVisitor();
    
    // Not collapsed; function call and needs exception handling
    private static class FCallNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		FCallNode iVisited = (FCallNode)ctx;
            Block tmpBlock = state.threadContext.beginCallArgs();
            IRubyObject[] args;
            try {
                args = setupArgs(state, state.runtime, state.threadContext, iVisited.getArgsNode());
            } finally {
            	state.threadContext.endCallArgs(tmpBlock);
            }

            state.setResult(state.getSelf().callMethod(iVisited.getName(), args, CallType.FUNCTIONAL));
    	}
    }
    private static final FCallNodeVisitor fCallNodeVisitor = new FCallNodeVisitor();
    
    // Nothing to do
    private static class FalseNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
            state.setResult(state.runtime.getFalse());
    	}
    }
    private static final FalseNodeVisitor falseNodeVisitor = new FalseNodeVisitor();
    
    // Not collapsed; I do not understand this.
    private static class FlipNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		FlipNode iVisited = (FlipNode)ctx;
            if (iVisited.isExclusive()) {
                if (! state.runtime.getCurrentScope().getValue(iVisited.getCount()).isTrue()) {
                    //Benoit: I don't understand why the state.result is inversed
                    state.setResult(state.begin(iVisited.getBeginNode()).isTrue() ? state.runtime.getFalse() : state.runtime.getTrue());
                    state.runtime.getCurrentScope().setValue(iVisited.getCount(), state.getResult());
                } else {
                    if (state.begin(iVisited.getEndNode()).isTrue()) {
                        state.runtime.getCurrentScope().setValue(iVisited.getCount(), state.runtime.getFalse());
                    }
                    state.setResult(state.runtime.getTrue());
                }
            } else {
                if (! state.runtime.getCurrentScope().getValue(iVisited.getCount()).isTrue()) {
                    if (state.begin(iVisited.getBeginNode()).isTrue()) {
                        //Benoit: I don't understand why the state.result is inversed
                        state.runtime.getCurrentScope().setValue(iVisited.getCount(), state.begin(iVisited.getEndNode()).isTrue() ? state.runtime.getFalse() : state.runtime.getTrue());
                        state.setResult(state.runtime.getTrue());
                    } else {
                        state.setResult(state.runtime.getFalse());
                    }
                } else {
                    if (state.begin(iVisited.getEndNode()).isTrue()) {
                        state.runtime.getCurrentScope().setValue(iVisited.getCount(), state.runtime.getFalse());
                    }
                    state.setResult(state.runtime.getTrue());
                }
            }
    	}
    }
    private static final FlipNodeVisitor flipNodeVisitor = new FlipNodeVisitor();
    
    // Not collapsed; big and copious use of flow-control exceptions
    private static class ForNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		ForNode iVisited = (ForNode)ctx;
        	state.threadContext.getBlockStack().push(Block.createBlock(iVisited.getVarNode(), new EvaluateCallable(iVisited.getBodyNode(), iVisited.getVarNode()), state.getSelf()));
            state.threadContext.pushIter(Iter.ITER_PRE);

            try {
                while (true) {
                    try {
                        ISourcePosition position = state.threadContext.getPosition();
                        Block tmpBlock = state.threadContext.beginCallArgs();

                        IRubyObject recv = null;
                        try {
                            recv = state.begin(iVisited.getIterNode());
                        } finally {
                            state.threadContext.setPosition(position);
                            state.threadContext.endCallArgs(tmpBlock);
                        }
                        state.setResult(recv.callMethod("each", IRubyObject.NULL_ARRAY, CallType.NORMAL));
                        return;
                    } catch (JumpException je) {
                    	if (je.getJumpType() == JumpException.JumpType.RetryJump) {
                    		// do nothing, allow loop to retry
                    	} else {
                    		throw je;
                    	}
                    }
                }
            } catch (JumpException je) {
            	if (je.getJumpType() == JumpException.JumpType.BreakJump) {
	                IRubyObject breakValue = (IRubyObject)je.getPrimaryData();
	                
	                state.setResult(breakValue == null ? state.runtime.getNil() : breakValue);
            	} else {
            		throw je;
            	}
            } finally {
                state.threadContext.popIter();
                state.threadContext.getBlockStack().pop();
            }
    	}
    }
    private static final ForNodeVisitor forNodeVisitor = new ForNodeVisitor();
    
    // Collapsed
    private static class GlobalAsgnNodeVisitor1 implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		GlobalAsgnNode iVisited = (GlobalAsgnNode)ctx;
            state.runtime.getGlobalVariables().set(iVisited.getName(), state.getResult());
    	}
    }
    private static final GlobalAsgnNodeVisitor1 globalAsgnNodeVisitor1 = new GlobalAsgnNodeVisitor1();
    private static class GlobalAsgnNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		GlobalAsgnNode iVisited = (GlobalAsgnNode)ctx;
    		state.clearResult();
    		state.addNodeInstruction(ctx, globalAsgnNodeVisitor1);
            state.addNodeInstruction(iVisited.getValueNode());
    	}
    }
    private static final GlobalAsgnNodeVisitor globalAsgnNodeVisitor = new GlobalAsgnNodeVisitor();
    
    // Nothing to do
    private static class GlobalVarNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		GlobalVarNode iVisited = (GlobalVarNode)ctx;
            state.setResult(state.runtime.getGlobalVariables().get(iVisited.getName()));
    	}
    }
    private static final GlobalVarNodeVisitor globalVarNodeVisitor = new GlobalVarNodeVisitor();
    
    // Not collapsed; requires carrying a hash across visits and has method calls
    private static class HashNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		HashNode iVisited = (HashNode)ctx;
            RubyHash hash = RubyHash.newHash(state.runtime);

            if (iVisited.getListNode() != null) {
                Iterator iterator = iVisited.getListNode().iterator();
                while (iterator.hasNext()) {
                    IRubyObject key = state.begin((Node) iterator.next());
                    if (iterator.hasNext()) {
                        hash.aset(key, state.begin((Node) iterator.next()));
                    } else {
                        // XXX
                        throw new RuntimeException("[BUG] odd number list for Hash");
                        // XXX
                    }
                }
            }
            state.setResult(hash);
    	}
    }
    private static final HashNodeVisitor hashNodeVisitor = new HashNodeVisitor();
    
    // Collapsed
    private static class InstAsgnNodeVisitor1 implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		InstAsgnNode iVisited = (InstAsgnNode)ctx;
            state.getSelf().setInstanceVariable(iVisited.getName(), state.getResult());
    	}
    }
    private static final InstAsgnNodeVisitor1 instAsgnNodeVisitor1 = new InstAsgnNodeVisitor1();
    private static class InstAsgnNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		InstAsgnNode iVisited = (InstAsgnNode)ctx;
    		state.clearResult();
    		state.addNodeInstruction(ctx, instAsgnNodeVisitor1);
            state.addNodeInstruction(iVisited.getValueNode());
    	}
    }
    private static final InstAsgnNodeVisitor instAsgnNodeVisitor = new InstAsgnNodeVisitor();
    
    // Nothing to do
    private static class InstVarNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		InstVarNode iVisited = (InstVarNode)ctx;
        	IRubyObject variable = state.getSelf().getInstanceVariable(iVisited.getName());
        	
            state.setResult(variable == null ? state.runtime.getNil() : variable);
    	}
    }
    private static final InstVarNodeVisitor instVarNodeVisitor = new InstVarNodeVisitor();
    
    // Collapsed
    private static class IfNodeImplVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		IfNode iVisited = (IfNode)ctx;
    		IRubyObject result = state.getResult();
    		
    		// Must set to nil; ifs or logical statements without then/else return nil
    		state.clearResult();
    		
    		if (result.isTrue()) {
    			if (iVisited.getThenBody() != null) {
    				state.addNodeInstruction(iVisited.getThenBody());
    			}
            } else {
            	if (iVisited.getElseBody() != null) {
            		state.addNodeInstruction(iVisited.getElseBody());
            	}
            }
    	}
    }
    private static final IfNodeImplVisitor ifNodeImplVisitor = new IfNodeImplVisitor();
    private static class IfNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		IfNode iVisited = (IfNode)ctx;
    		// add in reverse order
    		state.addNodeInstruction(iVisited, ifNodeImplVisitor);
    		state.addNodeInstruction(iVisited.getCondition());
       	}
    }
    private static final IfNodeVisitor ifNodeVisitor = new IfNodeVisitor();
    
    // Not collapsed, depends on exception handling and function calls
    private static class IterNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		IterNode iVisited = (IterNode)ctx;
        	state.threadContext.getBlockStack().push(Block.createBlock(iVisited.getVarNode(), 
            	    new EvaluateCallable(iVisited.getBodyNode(), iVisited.getVarNode()), state.getSelf()));
                try {
                    while (true) {
                        try {
                            state.threadContext.pushIter(Iter.ITER_PRE);
                            state.setResult(state.begin(iVisited.getIterNode()));
                            return;
                        } catch (JumpException je) {
                        	if (je.getJumpType() == JumpException.JumpType.RetryJump) {
                        		// allow loop to retry
                        	} else {
                        		throw je;
                        	}
                        } finally {
                            state.threadContext.popIter();
                        }
                    }
                } catch (JumpException je) {
                	if (je.getJumpType() == JumpException.JumpType.BreakJump) {
	                    IRubyObject breakValue = (IRubyObject)je.getPrimaryData();
	
	                    state.setResult(breakValue == null ? state.runtime.getNil() : breakValue);
                	} else {
                		throw je;
                	}
                } finally {
                    state.threadContext.getBlockStack().pop();
                }
    	}
    }
    private static final IterNodeVisitor iterNodeVisitor = new IterNodeVisitor();
    
    // Collapsed
    private static class LocalAsgnNodeVisitor1 implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		LocalAsgnNode iVisited = (LocalAsgnNode)ctx;
            state.runtime.getCurrentScope().setValue(iVisited.getCount(), state.getResult());
    	}
    }
    private static final LocalAsgnNodeVisitor1 localAsgnNodeVisitor1 = new LocalAsgnNodeVisitor1();
    private static class LocalAsgnNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		LocalAsgnNode iVisited = (LocalAsgnNode)ctx;
    		state.clearResult();
    		state.addNodeInstruction(ctx, localAsgnNodeVisitor1);
            state.addNodeInstruction(iVisited.getValueNode());
    	}
    }
    private static final LocalAsgnNodeVisitor localAsgnNodeVisitor = new LocalAsgnNodeVisitor();
    
    // Nothing to do assuming getValue() isn't recursing
    private static class LocalVarNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		LocalVarNode iVisited = (LocalVarNode)ctx;
            state.setResult(state.runtime.getCurrentScope().getValue(iVisited.getCount()));
    	}
    }
    private static final LocalVarNodeVisitor localVarNodeVisitor = new LocalVarNodeVisitor();
    
    // Not collapsed, calls out to AssignmentVisitor
    private static class MultipleAsgnNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		MultipleAsgnNode iVisited = (MultipleAsgnNode)ctx;
            state.setResult(new AssignmentVisitor(state.runtime, state.getSelf()).assign(iVisited, state.begin(iVisited.getValueNode()), false));
    	}
    }
    private static final MultipleAsgnNodeVisitor multipleAsgnNodeVisitor = new MultipleAsgnNodeVisitor(); 
    
    // Not collapsed; requires carrying multiple results
    private static class Match2NodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		Match2Node iVisited = (Match2Node)ctx;
            state.setResult(((RubyRegexp) state.begin(iVisited.getReceiverNode())).match(state.begin(iVisited.getValueNode())));
    	}
    }
    private static final Match2NodeVisitor match2NodeVisitor = new Match2NodeVisitor();
    
    // Not collapsed; requires carrying multiple results
    private static class Match3NodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		Match3Node iVisited = (Match3Node)ctx;
            IRubyObject receiver = state.begin(iVisited.getReceiverNode());
            IRubyObject value = state.begin(iVisited.getValueNode());
            if (value instanceof RubyString) {
                state.setResult(((RubyRegexp) receiver).match(value));
            } else {
                state.setResult(value.callMethod("=~", receiver));
            }
    	}
    }
    private static final Match3NodeVisitor match3NodeVisitor = new Match3NodeVisitor();
    
    // Collapsed
    private static class MatchNodeVisitor1 implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
            state.setResult(((RubyRegexp) state.getResult()).match2());
    	}
    }
    private static final MatchNodeVisitor1 matchNodeVisitor1 = new MatchNodeVisitor1();
    private static class MatchNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		MatchNode iVisited = (MatchNode)ctx;
    		state.clearResult();
    		state.addNodeInstruction(ctx, matchNodeVisitor1);
            state.addNodeInstruction(iVisited.getRegexpNode());
    	}
    }
    private static final MatchNodeVisitor matchNodeVisitor = new MatchNodeVisitor();
    
    // Not collapsed; exceptions
    private static class ModuleNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		ModuleNode iVisited = (ModuleNode)ctx;
            Node classNameNode = iVisited.getCPath();
            String name = ((INameNode) classNameNode).getName();
            RubyModule enclosingModule = getEnclosingModule(state, classNameNode);

            if (enclosingModule == null) {
                throw state.runtime.newTypeError("no outer class/module");
            }

            RubyModule module;
            if (enclosingModule == state.runtime.getObject()) {
                module = state.runtime.getOrCreateModule(name);
            } else {
                module = enclosingModule.defineModuleUnder(name);
            }
            evalClassDefinitionBody(state, iVisited.getBodyNode(), module);
    	}
    }
    private static final ModuleNodeVisitor moduleNodeVisitor = new ModuleNodeVisitor();
    
    // Collapsed
    private static class NewlineNodeTraceVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		NewlineNode iVisited = (NewlineNode)ctx;
    		
    		// something in here is used to build up ruby stack trace...
            state.threadContext.setPosition(iVisited.getPosition());
            
            if (isTrace(state)) {
               callTraceFunction(state, "line", state.getSelf());
            }
    	}
    }
    private static final NewlineNodeTraceVisitor newlineNodeTraceVisitor = new NewlineNodeTraceVisitor();
    private static class NewlineNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		List l = new ArrayList();
    		while (ctx instanceof NewlineNode) {
    			l.add(0, ctx);
    			ctx = ((NewlineNode)ctx).getNextNode();
    		}
    		state.addNodeInstruction(ctx);
    		
    		for (Iterator i = l.iterator(); i.hasNext();) {
    			state.addNodeInstruction((Node)i.next(), newlineNodeTraceVisitor);
    		}

	        // FIXME: Poll from somewhere else in the code?
	        state.threadContext.pollThreadEvents();
	        
    		// Newlines flush result (result from previous line is not available in next line...perhaps makes sense)
            state.clearResult();
    	}
    }
    private static final NewlineNodeVisitor newlineNodeVisitor = new NewlineNodeVisitor();
    
    // Not collapsed, exceptions
    private static class NextNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		NextNode iVisited = (NextNode)ctx;
    		JumpException je = new JumpException(JumpException.JumpType.NextJump);
            if (iVisited.getValueNode() != null) {
                je.setPrimaryData(state.begin(iVisited.getValueNode()));
            }
            throw je;
    	}
    }
    private static final NextNodeVisitor nextNodeVisitor = new NextNodeVisitor();
    
    // Nothing to do
    private static class NoopVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    	}
    }
    private static final NoopVisitor noopVisitor = new NoopVisitor();
    
    // Collapsed
    private static class NotNodeVisitor1 implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
            state.setResult(state.getResult().isTrue() ? state.runtime.getFalse() : state.runtime.getTrue());
    	}
    }
    private static final NotNodeVisitor1 notNodeVisitor1 = new NotNodeVisitor1();    
    private static class NotNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		NotNode iVisited = (NotNode)ctx;
    		state.clearResult();
    		state.addNodeInstruction(ctx, notNodeVisitor1);
            state.addNodeInstruction(iVisited.getConditionNode());
    	}
    }
    private static final NotNodeVisitor notNodeVisitor = new NotNodeVisitor();
    
    // Not collapsed, method call
    private static class NthRefNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		NthRefNode iVisited = (NthRefNode)ctx;
            state.setResult(RubyRegexp.nth_match(iVisited.getMatchNumber(), state.threadContext.getBackref()));
    	}
    }
    private static final NthRefNodeVisitor nthRefNodeVisitor = new NthRefNodeVisitor();
    
    // Not collapsed, multiple evals to resolve
    private static class OpElementAsgnNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		OpElementAsgnNode iVisited = (OpElementAsgnNode)ctx;
            IRubyObject receiver = state.begin(iVisited.getReceiverNode());

            IRubyObject[] args = setupArgs(state, state.runtime, state.threadContext, iVisited.getArgsNode());

            IRubyObject firstValue = receiver.callMethod("[]", args);

            if (iVisited.getOperatorName().equals("||")) {
                if (firstValue.isTrue()) {
                    state.setResult(firstValue);
                    return;
                }
    			firstValue = state.begin(iVisited.getValueNode());
            } else if (iVisited.getOperatorName().equals("&&")) {
                if (!firstValue.isTrue()) {
                    state.setResult(firstValue);
                    return;
                }
    			firstValue = state.begin(iVisited.getValueNode());
            } else {
                firstValue = firstValue.callMethod(iVisited.getOperatorName(), state.begin(iVisited.getValueNode()));
            }

            IRubyObject[] expandedArgs = new IRubyObject[args.length + 1];
            System.arraycopy(args, 0, expandedArgs, 0, args.length);
            expandedArgs[expandedArgs.length - 1] = firstValue;
            state.setResult(receiver.callMethod("[]=", expandedArgs));
    	}
    }
    private static final OpElementAsgnNodeVisitor opElementAsgnNodeVisitor = new OpElementAsgnNodeVisitor();
    
    // Not collapsed, multiple evals to resolve
    private static class OpAsgnNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		OpAsgnNode iVisited = (OpAsgnNode)ctx;
            IRubyObject receiver = state.begin(iVisited.getReceiverNode());
            IRubyObject value = receiver.callMethod(iVisited.getVariableName());

            if (iVisited.getOperatorName().equals("||")) {
                if (value.isTrue()) {
                    state.setResult(value);
                    return;
                }
    			value = state.begin(iVisited.getValueNode());
            } else if (iVisited.getOperatorName().equals("&&")) {
                if (!value.isTrue()) {
                    state.setResult(value);
                    return;
                }
    			value = state.begin(iVisited.getValueNode());
            } else {
                value = value.callMethod(iVisited.getOperatorName(), state.begin(iVisited.getValueNode()));
            }

            receiver.callMethod(iVisited.getVariableName() + "=", value);

            state.setResult(value);
    	}
    }
    private static final OpAsgnNodeVisitor opAsgnNodeVisitor = new OpAsgnNodeVisitor();
    
    // Not collapsed, exceptions
    private static class OptNNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		OptNNode iVisited = (OptNNode)ctx;
            while (RubyKernel.gets(state.runtime.getTopSelf(), IRubyObject.NULL_ARRAY).isTrue()) {
                while (true) { // Used for the 'redo' command
                    try {
                        state.begin(iVisited.getBodyNode());
                        break;
                    } catch (JumpException je) {
                    	if (je.getJumpType() == JumpException.JumpType.RedoJump) {
                    		// When a 'redo' is reached eval body of loop again.
                    		continue;
                    	} else if (je.getJumpType() == JumpException.JumpType.NextJump) {
                    		// When a 'next' is reached ceck condition of loop again.
                    		break;
                    	} else if (je.getJumpType() == JumpException.JumpType.BreakJump) {
	                        // When a 'break' is reached leave loop.
	                        return;
                    	} else {
                    		throw je;
                    	}
                    }
                }
            }
    	}
    }
    private static final OptNNodeVisitor optNNodeVisitor = new OptNNodeVisitor();
    
    // Collapsed
    private static class OrNodeImplVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		BinaryOperatorNode iVisited = (BinaryOperatorNode)ctx;
            if (!state.getResult().isTrue()) {
                state.addNodeInstruction(iVisited.getSecondNode());
            }
    	}
    }
    private static final OrNodeImplVisitor orNodeImplVisitor = new OrNodeImplVisitor();
    private static class OrNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		BinaryOperatorNode iVisited = (BinaryOperatorNode)ctx;
    		state.clearResult();
    		state.addNodeInstruction(ctx, orNodeImplVisitor);
    		state.addNodeInstruction(iVisited.getFirstNode());	
    	}
    }
    private static final OrNodeVisitor orNodeVisitor = new OrNodeVisitor();
    
    // Not collapsed, pure exception stuff
    private static class RedoNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
            throw new JumpException(JumpException.JumpType.RedoJump);
    	}
    }
    private static final RedoNodeVisitor redoNodeVisitor = new RedoNodeVisitor();
    
    // Not collapsed; had some trouble with it
    private static class RescueBodyNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		RescueBodyNode iVisited = (RescueBodyNode)ctx;
            state.begin(iVisited.getBodyNode());
    	}
    }
    private static final RescueBodyNodeVisitor rescueBodyNodeVisitor = new RescueBodyNodeVisitor();
    
    // Not collapsed, obviously heavy ties to exception handling
    private static class RescueNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		RescueNode iVisited = (RescueNode)ctx;
            RescuedBlock : while (true) {
                try {
                    // Execute rescue block
                    state.begin(iVisited.getBodyNode());

                    // If no exception is thrown execute else block
                    if (iVisited.getElseNode() != null) {
                        state.begin(iVisited.getElseNode());
                    }

                    return;
                } catch (RaiseException raiseJump) {
                	RubyException raisedException = raiseJump.getException();
                    // TODO: Rubicon TestKernel dies without this line.  A cursory glance implies we
                    // falsely set $! to nil and this sets it back to something valid.  This should 
                    // get fixed at the same time we address bug #1296484.
                    state.runtime.getGlobalVariables().set("$!", raisedException);

                    RescueBodyNode rescueNode = iVisited.getRescueNode();

                    while (rescueNode != null) {
                        Node  exceptionNodes = rescueNode.getExceptionNodes();
                        ListNode exceptionNodesList;
                        
                        if (exceptionNodes instanceof SplatNode) {                    
                            exceptionNodesList = (ListNode) state.begin(exceptionNodes);
                        } else {
                            exceptionNodesList = (ListNode) exceptionNodes;
                        }
                        
                        if (isRescueHandled(state, raisedException, exceptionNodesList)) {
                            try {
                                state.begin(rescueNode);
                                return;
                            } catch (JumpException je) {
                            	if (je.getJumpType() == JumpException.JumpType.RetryJump) {
                            		// should be handled in the finally block below
	                                //state.runtime.getGlobalVariables().set("$!", state.runtime.getNil());
	                                //state.threadContext.setRaisedException(null);
	                                continue RescuedBlock;
                            	} else {
                            		throw je;
                            	}
                            }
                        }
                        
                        rescueNode = rescueNode.getOptRescueNode();
                    }

                    // no takers; bubble up
                    throw raiseJump;
                } finally {
                	// clear exception when handled or retried
                    state.runtime.getGlobalVariables().set("$!", state.runtime.getNil());
                }
            }
    	}
    	
        private boolean isRescueHandled(EvaluationState state, RubyException currentException, ListNode exceptionNodes) {
            if (exceptionNodes == null) {
                return currentException.isKindOf(state.runtime.getClass("StandardError"));
            }

            Block tmpBlock = state.threadContext.beginCallArgs();

            IRubyObject[] args = null;
            try {
                args = setupArgs(state, state.runtime, state.threadContext, exceptionNodes);
            } finally {
            	state.threadContext.endCallArgs(tmpBlock);
            }

            for (int i = 0; i < args.length; i++) {
                if (! args[i].isKindOf(state.runtime.getClass("Module"))) {
                    throw state.runtime.newTypeError("class or module required for rescue clause");
                }
                if (args[i].callMethod("===", currentException).isTrue())
                    return true;
            }
            return false;
        }
    }
    private static final RescueNodeVisitor rescueNodeVisitor = new RescueNodeVisitor();
    
    // Not collapsed, pure exception stuff
    private static class RetryNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		
    		// XXX: this should all change or go away with the new mechanism of recording "points" in the stack to return to
    		while (!state.getCurrentNodeStack().isEmpty()) {
    			// perhaps replace by having visitors register/listen for events
        		if (state.getCurrentNodeVisitorStack().peek() instanceof WantsRetry) {
        			state.popCurrentNodeAndVisitor();
        			return; // Allow eval to continue at node following "WantsReturn"
        		}
        		state.popCurrentNodeAndVisitor();
    		}
    		
    		// TODO remove once eval is 100% iterative
    		// nobody wants return and queue is empty...rethrow
    		JumpException je = new JumpException(JumpException.JumpType.RetryJump);
    		
    		throw je;
    	}
    }
    private static final RetryNodeVisitor retryNodeVisitor = new RetryNodeVisitor();
    
    // Not collapsed, flow-control exceptions
    private static class ReturnNodeVisitor1 implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		ReturnNode iVisited = (ReturnNode)ctx;
    		
    		// reduce node stack to nearest return point
    		// TODO: if multiple return points in a row, tail-call optimize?
    		state.returnToNearestReturnPoint();
    		
    		if (state.getCurrentNodeStack().isEmpty()) {
        		// TODO remove once eval is 100% iterative
        		// nobody wants return and queue is empty...rethrow
        		JumpException je = new JumpException(JumpException.JumpType.ReturnJump);
        		
        		je.setPrimaryData(iVisited.getTarget());
        		je.setSecondaryData(state.getResult());
        		
        		throw je;
    		}
    	}
    }
    private static final ReturnNodeVisitor1 returnNodeVisitor1 = new ReturnNodeVisitor1();
    // Not collapsed, flow-control exceptions
    private static class ReturnNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		ReturnNode iVisited = (ReturnNode)ctx;
    		
    		state.addNodeInstruction(ctx, returnNodeVisitor1);
    		if (iVisited.getValueNode() != null) {
    			state.addNodeInstruction(iVisited.getValueNode());
    		}
    		
    		state.clearResult();
    	}
    }
    private static final ReturnNodeVisitor returnNodeVisitor = new ReturnNodeVisitor();
    
    // Not collapsed, evalClassBody will take some work
    private static class SClassNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		SClassNode iVisited = (SClassNode)ctx;
            IRubyObject receiver = state.begin(iVisited.getReceiverNode());

            RubyClass singletonClass;

            if (receiver.isNil()) {
                singletonClass = state.runtime.getClass("NilClass");
            } else if (receiver == state.runtime.getTrue()) {
                singletonClass = state.runtime.getClass("True");
            } else if (receiver == state.runtime.getFalse()) {
                singletonClass = state.runtime.getClass("False");
            } else {
                if (state.runtime.getSafeLevel() >= 4 && !receiver.isTaint()) {
                    throw state.runtime.newSecurityError("Insecure: can't extend object.");
                }

                singletonClass = receiver.getSingletonClass();
            }

            if (state.threadContext.getWrapper() != null) {
                singletonClass.extendObject(state.threadContext.getWrapper());
                singletonClass.includeModule(state.threadContext.getWrapper());
            }

            evalClassDefinitionBody(state, iVisited.getBodyNode(), singletonClass);
    	}
    }
    private static final SClassNodeVisitor sClassNodeVisitor = new SClassNodeVisitor();
    
    // Not collapsed, exception handling needed
    private static class ScopeNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		ScopeNode iVisited = (ScopeNode)ctx;
            state.threadContext.pushFrameCopy();
            state.threadContext.pushScope(iVisited.getLocalNames());
            try {
                state.begin(iVisited.getBodyNode());
            } finally {
                state.threadContext.popScope();
                state.threadContext.popFrame();
            }
    	}
    }
    private static final ScopeNodeVisitor scopeNodeVisitor = new ScopeNodeVisitor();
    
    // Nothing to do
    private static class SelfNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
            state.setResult(state.getSelf());
    	}
    }
    private static final SelfNodeVisitor selfNodeVisitor = new SelfNodeVisitor();
    
    // Collapsed
    private static class SplatNodeVisitor1 implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
            state.setResult(splatValue(state, state.getResult()));
    	}
    }
    private static final SplatNodeVisitor1 splatNodeVisitor1 = new SplatNodeVisitor1();
    private static class SplatNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		SplatNode iVisited = (SplatNode)ctx;
    		state.clearResult();
    		state.addNodeInstruction(ctx, splatNodeVisitor1);
            state.addNodeInstruction(iVisited.getValue());
    	}
    }
    private static final SplatNodeVisitor splatNodeVisitor = new SplatNodeVisitor();
    
    // Nothing to do, other than concerns about newString recursing
    private static class StrNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		StrNode iVisited = (StrNode)ctx;
            state.setResult(state.runtime.newString(iVisited.getValue()));
    	}
    }
    private static final StrNodeVisitor strNodeVisitor = new StrNodeVisitor();
    
    // Collapsed
    private static class SValueNodeVisitor1 implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
            state.setResult(aValueSplat(state, state.getResult()));
    	}
    }
    private static final SValueNodeVisitor1 sValueNodeVisitor1 = new SValueNodeVisitor1();
    private static class SValueNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		SValueNode iVisited = (SValueNode)ctx;
    		state.clearResult();
    		state.addNodeInstruction(ctx, sValueNodeVisitor1);
            state.addNodeInstruction(iVisited.getValue());
    	}
    }
    private static final SValueNodeVisitor sValueNodeVisitor = new SValueNodeVisitor();
    
    // Not collapsed, exceptions
    private static class SuperNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		SuperNode iVisited = (SuperNode)ctx;
            if (state.threadContext.getCurrentFrame().getLastClass() == null) {
                throw state.runtime.newNameError("Superclass method '" + state.threadContext.getCurrentFrame().getLastFunc() + "' disabled.");
            }

            Block tmpBlock = state.threadContext.beginCallArgs();

            IRubyObject[] args = null;
            try {
                args = setupArgs(state, state.runtime, state.threadContext, iVisited.getArgsNode());
            } finally {
            	state.threadContext.endCallArgs(tmpBlock);
            }
            state.setResult(state.threadContext.callSuper(args));
    	}
    }
    private static final SuperNodeVisitor superNodeVisitor = new SuperNodeVisitor();
    
    // Collapsed
    private static class ToAryNodeVisitor1 implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
            state.setResult(aryToAry(state, state.getResult()));
    	}
    }
    private static final ToAryNodeVisitor1 toAryNodeVisitor1 = new ToAryNodeVisitor1();
    private static class ToAryNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		ToAryNode iVisited = (ToAryNode)ctx;
    		state.clearResult();
    		state.addNodeInstruction(ctx, toAryNodeVisitor1);
            state.addNodeInstruction(iVisited.getValue());
    	}
    }
    private static final ToAryNodeVisitor toAryNodeVisitor = new ToAryNodeVisitor();

    // Nothing to do
    private static class TrueNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
            state.setResult(state.runtime.getTrue());
    	}
    }
    private static final TrueNodeVisitor trueNodeVisitor = new TrueNodeVisitor();
    
    // Probably nothing to do
    private static class UndefNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		UndefNode iVisited = (UndefNode)ctx;
            if (state.threadContext.getRubyClass() == null) {
                throw state.runtime.newTypeError("No class to undef method '" + iVisited.getName() + "'.");
            }
            state.threadContext.getRubyClass().undef(iVisited.getName());
    	}
    }
    private static final UndefNodeVisitor undefNodeVisitor = new UndefNodeVisitor();
    
    // Not collapsed, exception handling like crazy
    private static class UntilNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		UntilNode iVisited = (UntilNode)ctx;
            while (!state.begin(iVisited.getConditionNode()).isTrue()) {
                while (true) { // Used for the 'redo' command
                    try {
                        state.begin(iVisited.getBodyNode());
                        break;
                    } catch (JumpException je) {
                    	if (je.getJumpType() == JumpException.JumpType.RedoJump) {
                    		// When a 'redo' is reached eval body of loop again.
                    		continue;
                    	} else if (je.getJumpType() == JumpException.JumpType.NextJump) {
                    		// When a 'next' is reached ceck condition of loop again.
                    		break;
                    	} else if (je.getJumpType() == JumpException.JumpType.BreakJump) {
	                        // When a 'break' is reached leave loop.
	                        return;
                    	} else {
                    		throw je;
                    	}
                    }
                }
            }
    	}
    }
    private static final UntilNodeVisitor untilNodeVisitor = new UntilNodeVisitor();
    
    // Nothing to do, but examine aliasing side effects
    private static class VAliasNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		VAliasNode iVisited = (VAliasNode)ctx;
            state.runtime.getGlobalVariables().alias(iVisited.getNewName(), iVisited.getOldName());
    	}
    }
    private static final VAliasNodeVisitor vAliasNodeVisitor = new VAliasNodeVisitor();
    
    // Not collapsed, method call
    private static class VCallNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		VCallNode iVisited = (VCallNode)ctx;
            state.setResult(state.getSelf().callMethod(iVisited.getMethodName(), IRubyObject.NULL_ARRAY, CallType.VARIABLE));
    	}
    }
    private static final VCallNodeVisitor vCallNodeVisitor = new VCallNodeVisitor();
    
    // Not collapsed, complicated with exceptions
    private static class WhileNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		WhileNode iVisited = (WhileNode)ctx;
            // while do...Initial condition not met do not enter block
            if (iVisited.evaluateAtStart() && 
                state.begin(iVisited.getConditionNode()).isTrue() == false) {
                return;
            }
            
            do {
                while (true) { // Used for the 'redo' command
                    try {
                        state.begin(iVisited.getBodyNode());
                        break;
                    } catch (JumpException je) {
                    	if (je.getJumpType() == JumpException.JumpType.RedoJump) {
                    		// When a 'redo' is reached eval body of loop again.
                    		continue;
                    	} else if (je.getJumpType() == JumpException.JumpType.NextJump) {
                    		// When a 'next' is reached ceck condition of loop again.
                    		break;
                    	} else if (je.getJumpType() == JumpException.JumpType.BreakJump) {
	                        // When a 'break' is reached leave loop.
	                        return;
                    	} else {
                    		throw je;
                    	}
                    }
                }
            } while (state.begin(iVisited.getConditionNode()).isTrue());
    	}
    }
    private static final WhileNodeVisitor whileNodeVisitor = new WhileNodeVisitor();
    
    // Not collapsed, method call
    private static class XStrNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		XStrNode iVisited = (XStrNode)ctx;
            state.setResult(state.getSelf().callMethod("`", state.runtime.newString(iVisited.getValue())));
    	}
    }
    private static final XStrNodeVisitor xStrNodeVisitor = new XStrNodeVisitor();
    
    // Not collapsed, yield is like a method call, needs research
    private static class YieldNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		YieldNode iVisited = (YieldNode)ctx;
            state.begin(iVisited.getArgsNode());

        	// Special Hack...We cannot tell between no args and a nil one.
        	// Change it back to null for now until a better solution is 
        	// found
        	// TODO: Find better way of differing...
        	if (iVisited.getArgsNode() == null) {
        	    state.setResult(null);
        	}
                
            state.setResult(state.threadContext.yield(state.getResult(), null, null, false, iVisited.getCheckState()));
    	}
    }
    private static final YieldNodeVisitor yieldNodeVisitor = new YieldNodeVisitor();
    
    // Nothing to do, other than array creation side effects?
    private static class ZArrayNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
            state.setResult(state.runtime.newArray());
    	}
    }
    private static final ZArrayNodeVisitor zArrayNodeVisitor = new ZArrayNodeVisitor();
    
    // Not collapsed, is this a call?
    private static class ZSuperNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		Frame frame = state.threadContext.getCurrentFrame();
    		
            if (frame.getLastClass() == null) {
                throw state.runtime.newNameError("superclass method '" + frame.getLastFunc() + "' disabled");
            }

            state.setResult(state.threadContext.callSuper(frame.getArgs()));
    	}
    }
    private static final ZSuperNodeVisitor zSuperNodeVisitor = new ZSuperNodeVisitor();
    
    // Nothing to do, other than side effects
    private static class BignumNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		BignumNode iVisited = (BignumNode)ctx;
            state.setResult(RubyBignum.newBignum(state.runtime, iVisited.getValue()));
    	}
    }
    private static final BignumNodeVisitor bignumNodeVisitor = new BignumNodeVisitor();
    
//  Nothing to do, other than side effects
    private static class FixnumNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		FixnumNode iVisited = (FixnumNode)ctx;
            state.setResult(state.runtime.newFixnum(iVisited.getValue()));
    	}
    }
    private static final FixnumNodeVisitor fixnumNodeVisitor = new FixnumNodeVisitor();
    
//  Nothing to do, other than side effects
    private static class FloatNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		FloatNode iVisited = (FloatNode)ctx;
            state.setResult(RubyFloat.newFloat(state.runtime, iVisited.getValue()));
    	}
    }
    private static final FloatNodeVisitor floatNodeVisitor = new FloatNodeVisitor();
    
//  Nothing to do, other than side effects
    private static class RegexpNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		RegexpNode iVisited = (RegexpNode)ctx;
            state.setResult(RubyRegexp.newRegexp(state.runtime.newString(iVisited.getValue()), iVisited.getOptions(), null));
    	}
    }
    private static final RegexpNodeVisitor regexpNodeVisitor = new RegexpNodeVisitor();
    
//  Nothing to do, other than side effects
    private static class SymbolNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		SymbolNode iVisited = (SymbolNode)ctx;
            state.setResult(state.runtime.newSymbol(iVisited.getName()));
    	}
    }
    private static final SymbolNodeVisitor symbolNodeVisitor = new SymbolNodeVisitor();

    /**
     * @see NodeVisitor#visitAliasNode(AliasNode)
     */
    public Instruction visitAliasNode(AliasNode iVisited) {
    	return aliasNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitAndNode(AndNode)
     */
    public Instruction visitAndNode(AndNode iVisited) {
    	return andNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitArgsNode(ArgsNode)
     */
    public Instruction visitArgsNode(ArgsNode iVisited) {
        assert false;
        return null;
    }

    /**
     * @see NodeVisitor#visitArgsCatNode(ArgsCatNode)
     */
    public Instruction visitArgsCatNode(ArgsCatNode iVisited) {
    	return argsCatNodeVisitor;
    }
    
    /**
     * @see NodeVisitor#visitArrayNode(ArrayNode)
     */
    public Instruction visitArrayNode(ArrayNode iVisited) {
    	return arrayNodeVisitor;
    }
    
    /**
     * @see NodeVisitor#visitBackRefNode(BackRefNode)
     */
    public Instruction visitBackRefNode(BackRefNode iVisited) {
    	return backRefNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitBeginNode(BeginNode)
     */
    public Instruction visitBeginNode(BeginNode iVisited) {
    	return beginNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitBlockArgNode(BlockArgNode)
     */
    public Instruction visitBlockArgNode(BlockArgNode iVisited) {
        assert false;
        return null;
    }
    
    /**
     * @see NodeVisitor#visitBlockNode(BlockNode)
     */
    public Instruction visitBlockNode(BlockNode iVisited) {
    	return blockNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitBlockPassNode(BlockPassNode)
     */
    public Instruction visitBlockPassNode(BlockPassNode iVisited) {
    	return blockPassNodeVisitor;
    }
    
    /**
     * @see NodeVisitor#visitBreakNode(BreakNode)
     */
    public Instruction visitBreakNode(BreakNode iVisited) {
    	return breakNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitConstDeclNode(ConstDeclNode)
     */
    public Instruction visitConstDeclNode(ConstDeclNode iVisited) {
    	return constDeclNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitClassVarAsgnNode(ClassVarAsgnNode)
     */
    public Instruction visitClassVarAsgnNode(ClassVarAsgnNode iVisited) {
    	return classVarAsgnNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitClassVarDeclNode(ClassVarDeclNode)
     */
    public Instruction visitClassVarDeclNode(ClassVarDeclNode iVisited) {
    	return classVarDeclNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitClassVarNode(ClassVarNode)
     */
    public Instruction visitClassVarNode(ClassVarNode iVisited) {
    	return classVarNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitCallNode(CallNode)
     */
    public Instruction visitCallNode(CallNode iVisited) {
    	return callNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitCaseNode(CaseNode)
     */
    public Instruction visitCaseNode(CaseNode iVisited) {
    	return caseNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitClassNode(ClassNode)
     */
    public Instruction visitClassNode(ClassNode iVisited) {
    	return classNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitColon2Node(Colon2Node)
     */
    public Instruction visitColon2Node(Colon2Node iVisited) {
    	return colon2NodeVisitor;
    }
    
    /**
     * @see NodeVisitor#visitColon3Node(Colon3Node)
     */
    public Instruction visitColon3Node(Colon3Node iVisited) {
    	return colon3NodeVisitor;
    }

    /**
     * @see NodeVisitor#visitConstNode(ConstNode)
     */
    public Instruction visitConstNode(ConstNode iVisited) {
    	return constNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitDAsgnNode(DAsgnNode)
     */
    public Instruction visitDAsgnNode(DAsgnNode iVisited) {
    	return dAsgnNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitDRegxNode(DRegexpNode)
     */
    public Instruction visitDRegxNode(DRegexpNode iVisited) {
    	return dRegexpNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitDStrNode(DStrNode)
     */
    public Instruction visitDStrNode(DStrNode iVisited) {
    	return dStrNodeVisitor;
    }
    
    /**
     * @see NodeVisitor#visitSymbolNode(SymbolNode)
     */
    public Instruction visitDSymbolNode(DSymbolNode iVisited) {
    	return dSymbolNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitDVarNode(DVarNode)
     */
    public Instruction visitDVarNode(DVarNode iVisited) {
    	return dVarNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitDXStrNode(DXStrNode)
     */
    public Instruction visitDXStrNode(DXStrNode iVisited) {
    	return dXStrNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitDefinedNode(DefinedNode)
     */
    public Instruction visitDefinedNode(DefinedNode iVisited) {
    	return definedNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitDefnNode(DefnNode)
     */
    public Instruction visitDefnNode(DefnNode iVisited) {
    	return defnNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitDefsNode(DefsNode)
     */
    public Instruction visitDefsNode(DefsNode iVisited) {
    	return defsNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitDotNode(DotNode)
     */
    public Instruction visitDotNode(DotNode iVisited) {
    	return dotNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitEnsureNode(EnsureNode)
     */
    public Instruction visitEnsureNode(EnsureNode iVisited) {
    	return ensureNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitEvStrNode(EvStrNode)
     */
    public final Instruction visitEvStrNode(final EvStrNode iVisited) {
    	return evStrNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitFCallNode(FCallNode)
     */
    public Instruction visitFCallNode(FCallNode iVisited) {
    	return fCallNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitFalseNode(FalseNode)
     */
    public Instruction visitFalseNode(FalseNode iVisited) {
    	return falseNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitFlipNode(FlipNode)
     */
    public Instruction visitFlipNode(FlipNode iVisited) {
    	return flipNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitForNode(ForNode)
     */
    public Instruction visitForNode(ForNode iVisited) {
    	return forNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitGlobalAsgnNode(GlobalAsgnNode)
     */
    public Instruction visitGlobalAsgnNode(GlobalAsgnNode iVisited) {
    	return globalAsgnNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitGlobalVarNode(GlobalVarNode)
     */
    public Instruction visitGlobalVarNode(GlobalVarNode iVisited) {
    	return globalVarNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitHashNode(HashNode)
     */
    public Instruction visitHashNode(HashNode iVisited) {
    	return hashNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitInstAsgnNode(InstAsgnNode)
     */
    public Instruction visitInstAsgnNode(InstAsgnNode iVisited) {
    	return instAsgnNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitInstVarNode(InstVarNode)
     */
    public Instruction visitInstVarNode(InstVarNode iVisited) {
    	return instVarNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitIfNode(IfNode)
     */
    public Instruction visitIfNode(IfNode iVisited) {
    	return ifNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitIterNode(IterNode)
     */
    public Instruction visitIterNode(IterNode iVisited) {
    	return iterNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitLocalAsgnNode(LocalAsgnNode)
     */
    public Instruction visitLocalAsgnNode(LocalAsgnNode iVisited) {
    	return localAsgnNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitLocalVarNode(LocalVarNode)
     */
    public Instruction visitLocalVarNode(LocalVarNode iVisited) {
    	return localVarNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitMultipleAsgnNode(MultipleAsgnNode)
     */
    public Instruction visitMultipleAsgnNode(MultipleAsgnNode iVisited) {
    	return multipleAsgnNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitMatch2Node(Match2Node)
     */
    public Instruction visitMatch2Node(Match2Node iVisited) {
    	return match2NodeVisitor;
    }

    /**
     * @see NodeVisitor#visitMatch3Node(Match3Node)
     */
    public Instruction visitMatch3Node(Match3Node iVisited) {
    	return match3NodeVisitor;
    }

    /**
     * @see NodeVisitor#visitMatchNode(MatchNode)
     */
    public Instruction visitMatchNode(MatchNode iVisited) {
    	return matchNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitModuleNode(ModuleNode)
     */
    public Instruction visitModuleNode(ModuleNode iVisited) {
    	return moduleNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitNewlineNode(NewlineNode)
     */
    public Instruction visitNewlineNode(NewlineNode iVisited) {
    	return newlineNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitNextNode(NextNode)
     */
    public Instruction visitNextNode(NextNode iVisited) {
    	return nextNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitNilNode(NilNode)
     */
    public Instruction visitNilNode(NilNode iVisited) {
    	return noopVisitor;
    }

    /**
     * @see NodeVisitor#visitNotNode(NotNode)
     */
    public Instruction visitNotNode(NotNode iVisited) {
    	return notNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitNthRefNode(NthRefNode)
     */
    public Instruction visitNthRefNode(NthRefNode iVisited) {
    	return nthRefNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitOpElementAsgnNode(OpElementAsgnNode)
     */
    public Instruction visitOpElementAsgnNode(OpElementAsgnNode iVisited) {
    	return opElementAsgnNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitOpAsgnNode(OpAsgnNode)
     */
    public Instruction visitOpAsgnNode(OpAsgnNode iVisited) {
    	return opAsgnNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitOpAsgnAndNode(OpAsgnAndNode)
     */
    public Instruction visitOpAsgnAndNode(OpAsgnAndNode iVisited) {
    	return andNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitOpAsgnOrNode(OpAsgnOrNode)
     */
    public Instruction visitOpAsgnOrNode(OpAsgnOrNode iVisited) {
    	return orNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitOptNNode(OptNNode)
     */
    public Instruction visitOptNNode(OptNNode iVisited) {
    	return optNNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitOrNode(OrNode)
     */
    public Instruction visitOrNode(OrNode iVisited) {
    	return orNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitPostExeNode(PostExeNode)
     */
    public Instruction visitPostExeNode(PostExeNode iVisited) {
    	return noopVisitor;
    }

    /**
     * @see NodeVisitor#visitRedoNode(RedoNode)
     */
    public Instruction visitRedoNode(RedoNode iVisited) {
    	return redoNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitRescueBodyNode(RescueBodyNode)
     */
    public Instruction visitRescueBodyNode(RescueBodyNode iVisited) {
    	return rescueBodyNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitRescueNode(RescueNode)
     */
    public Instruction visitRescueNode(RescueNode iVisited) {
    	return rescueNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitRetryNode(RetryNode)
     */
    public Instruction visitRetryNode(RetryNode iVisited) {
    	return retryNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitReturnNode(ReturnNode)
     */
    public Instruction visitReturnNode(ReturnNode iVisited) {
    	return returnNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitSClassNode(SClassNode)
     */
    public Instruction visitSClassNode(SClassNode iVisited) {
    	return sClassNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitScopeNode(ScopeNode)
     */
    public Instruction visitScopeNode(ScopeNode iVisited) {
    	return scopeNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitSelfNode(SelfNode)
     */
    public Instruction visitSelfNode(SelfNode iVisited) {
    	return selfNodeVisitor;
    }

    public Instruction visitSplatNode(SplatNode iVisited) {
    	return splatNodeVisitor;
    }
    
    /**
     * @see NodeVisitor#visitStrNode(StrNode)
     */
    public Instruction visitStrNode(StrNode iVisited) {
    	return strNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitSValueNode(SValueNode)
     */
    public Instruction visitSValueNode(SValueNode iVisited) {
    	return sValueNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitSuperNode(SuperNode)
     */
    public Instruction visitSuperNode(SuperNode iVisited) {
    	return superNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitToAryNode(ToAryNode)
     */
    public Instruction visitToAryNode(ToAryNode iVisited) {
    	return toAryNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitTrueNode(TrueNode)
     */
    public Instruction visitTrueNode(TrueNode iVisited) {
    	return trueNodeVisitor;
    }
    
    /**
     * @see NodeVisitor#visitUndefNode(UndefNode)
     */
    public Instruction visitUndefNode(UndefNode iVisited) {
    	return undefNodeVisitor;    	
    }

    /**
     * @see NodeVisitor#visitUntilNode(UntilNode)
     */
    public Instruction visitUntilNode(UntilNode iVisited) {
    	return untilNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitVAliasNode(VAliasNode)
     */
    public Instruction visitVAliasNode(VAliasNode iVisited) {
    	return vAliasNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitVCallNode(VCallNode)
     */
    public Instruction visitVCallNode(VCallNode iVisited) {
    	return vCallNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitWhenNode(WhenNode)
     */
    public Instruction visitWhenNode(WhenNode iVisited) {
        assert false;
        return null;
    }

    /**
     * @see NodeVisitor#visitWhileNode(WhileNode)
     */
    public Instruction visitWhileNode(WhileNode iVisited) {
    	return whileNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitXStrNode(XStrNode)
     */
    public Instruction visitXStrNode(XStrNode iVisited) {
    	return xStrNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitYieldNode(YieldNode)
     */
    public Instruction visitYieldNode(YieldNode iVisited) {
    	return yieldNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitZArrayNode(ZArrayNode)
     */
    public Instruction visitZArrayNode(ZArrayNode iVisited) {
    	return zArrayNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitZSuperNode(ZSuperNode)
     */
    public Instruction visitZSuperNode(ZSuperNode iVisited) {
    	return zSuperNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitBignumNode(BignumNode)
     */
    public Instruction visitBignumNode(BignumNode iVisited) {
    	return bignumNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitFixnumNode(FixnumNode)
     */
    public Instruction visitFixnumNode(FixnumNode iVisited) {
    	return fixnumNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitFloatNode(FloatNode)
     */
    public Instruction visitFloatNode(FloatNode iVisited) {
    	return floatNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitRegexpNode(RegexpNode)
     */
    public Instruction visitRegexpNode(RegexpNode iVisited) {
    	return regexpNodeVisitor;
    }

    /**
     * @see NodeVisitor#visitSymbolNode(SymbolNode)
     */
    public Instruction visitSymbolNode(SymbolNode iVisited) {
    	return symbolNodeVisitor;
    }

    /** Evaluates the body in a class or module definition statement.
     *
     */
    private static void evalClassDefinitionBody(EvaluationState state, ScopeNode iVisited, RubyModule type) {
		state.threadContext.pushRubyClass(type); 
        state.threadContext.pushFrameCopy();
        state.threadContext.pushScope(iVisited.getLocalNames());
        state.threadContext.pushDynamicVars();

        IRubyObject oldSelf = state.getSelf();

        try {
            if (isTrace(state)) {
                callTraceFunction(state, "class", type);
            }

            state.setSelf(type);
            state.begin(iVisited.getBodyNode());
        } finally {
            state.setSelf(oldSelf);

            state.threadContext.popDynamicVars();
            state.threadContext.popScope();
            state.threadContext.popRubyClass();
            state.threadContext.popFrame();

            if (isTrace(state)) {
                callTraceFunction(state, "end", null);
            }
        }
    }

    private static IRubyObject aryToAry(EvaluationState state, IRubyObject value) {
        if (value instanceof RubyArray) {
            return value;
        }
        
        if (value.respondsTo("to_ary")) {
            return value.convertToType("Array", "to_ary", false);
        }
        
        return state.runtime.newArray(value);
    }
    
    private static IRubyObject splatValue(EvaluationState state, IRubyObject value) {
        if (value.isNil()) {
            return state.runtime.newArray(value);
        }
        
        return arrayValue(state, value);
    }

    private static IRubyObject aValueSplat(EvaluationState state, IRubyObject value) {
        if (!(value instanceof RubyArray) ||
            ((RubyArray) value).length().getLongValue() == 0) {
            return state.runtime.getNil();
        }
        
        RubyArray array = (RubyArray) value;
        
        return array.getLength() == 1 ? array.first(IRubyObject.NULL_ARRAY) : array;
    }

    /* HACK: .... */
    private static RubyArray arrayValue(EvaluationState state, IRubyObject value) {
        IRubyObject newValue = value.convertToType("Array", "to_ary", false);

        if (newValue.isNil()) {
            // XXXEnebo: We should call to_a except if it is kernel def....
            // but we will forego for now.
            newValue = state.runtime.newArray(value);
        }
        
        return (RubyArray) newValue;
    }
    
    private static IRubyObject[] setupArgs(EvaluationState state, IRuby runtime, ThreadContext context, Node node) {
        if (node == null) {
            return IRubyObject.NULL_ARRAY;
        }

        if (node instanceof ArrayNode) {
        	ISourcePosition position = context.getPosition();
            ArrayList list = new ArrayList(((ArrayNode) node).size());
            
            for (Iterator iter=((ArrayNode)node).iterator(); iter.hasNext();){
                final Node next = (Node) iter.next();
                if (next instanceof SplatNode) {
                    list.addAll(((RubyArray) state.begin(next)).getList());
                } else {
                    list.add(state.begin(next));
                }
            }

            context.setPosition(position);

            return (IRubyObject[]) list.toArray(new IRubyObject[list.size()]);
        }

        return ArgsUtil.arrayify(state.begin(node));
    }

    private static RubyModule getEnclosingModule(EvaluationState state, Node node) {
        RubyModule enclosingModule = null;
        
        if (node instanceof Colon2Node) {
        	state.begin(((Colon2Node) node).getLeftNode());
        	
        	if (state.getResult() != null && !state.getResult().isNil()) {
        		enclosingModule = (RubyModule) state.getResult();
        	}
        } 
        
        if (enclosingModule == null) {
        	enclosingModule = state.threadContext.getRubyClass();
        }

        return enclosingModule;
    }
}
