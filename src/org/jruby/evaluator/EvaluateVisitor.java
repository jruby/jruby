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
 * Copyright (C) 2004-2006 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004-2006 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 David Corbin <dcorbin@users.sourceforge.net>
 * Copyright (C) 2006 Ola Bini <ola.bini@ki.se>
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
import org.jruby.internal.runtime.methods.DefaultMethod;
import org.jruby.internal.runtime.methods.WrapperCallable;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ICallable;
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
	
    public static EvaluateVisitor getInstance() {
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
        ThreadContext tc = state.getThreadContext();
        String name = tc.getFrameLastFunc();
        RubyModule type = tc.getFrameLastClass();
        state.runtime.callTraceFunction(event, tc.getPosition(), zelf, name, type);
    }
    
    // Collapsing further needs method calls
    private static class AliasNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		AliasNode iVisited = (AliasNode)ctx;
            ThreadContext tc = state.getThreadContext();
            
            if (tc.getRubyClass() == null) {
            	throw state.runtime.newTypeError("no class to make alias");
            }

            tc.getRubyClass().defineAlias(iVisited.getNewName(), iVisited.getOldName());
            tc.getRubyClass().callMethod("method_added", state.runtime.newSymbol(iVisited.getNewName()));
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
    		state.addInstruction((Node)iVisited, andNodeImplVisitor);
    		state.addNodeInstruction((Node)iVisited.getFirstNode());
    	}
    }
    // used also for OpAsgnAndNode
    private static final AndNodeVisitor andNodeVisitor = new AndNodeVisitor();
    
    // Used for pushing down the result stack
    private static class AggregateResultInstruction implements Instruction {
        public void execute(EvaluationState state, InstructionContext ctx) {
            state.aggregateResult();
        }
    }
    private static final AggregateResultInstruction aggregateResult = new AggregateResultInstruction();
    
    // Used for popping the result stack
    private static class DeaggregateResultInstruction implements Instruction {
        public void execute(EvaluationState state, InstructionContext ctx) {
            state.deaggregateResult();
        }
    }
    private static final DeaggregateResultInstruction deaggregateResult = new DeaggregateResultInstruction();

    // Collapsed; use of stack to aggregate args could be improved (see FIXME in EvaluationState#aggregateResult) but it's not bad
    private static class ArgsCatNodeVisitor2 implements Instruction {
        public void execute(EvaluationState state, InstructionContext ctx) {
            IRubyObject secondArgs = splatValue(state, state.deaggregateResult());
            IRubyObject args = state.getResult();
            RubyArray list = args instanceof RubyArray ? (RubyArray) args :
                state.runtime.newArray(args);
            
            state.setResult(list.concat(secondArgs)); 
        }
    }
    private static final ArgsCatNodeVisitor2 argsCatNodeVisitor2 = new ArgsCatNodeVisitor2();
    private static class ArgsCatNodeVisitor implements Instruction {
        public void execute(EvaluationState state, InstructionContext ctx) {
            ArgsCatNode iVisited = (ArgsCatNode)ctx;
            
            state.addInstruction(iVisited, argsCatNodeVisitor2);
            state.addNodeInstruction(iVisited.getSecondNode());
            state.addInstruction(iVisited, aggregateResult);
            state.addNodeInstruction(iVisited.getFirstNode());
        }
    }
    private static final ArgsCatNodeVisitor argsCatNodeVisitor = new ArgsCatNodeVisitor();
    
    // Collapsed; use of stack to aggregate Array elements could be optimized a bit
    private static class ArrayBuilder implements Instruction {
        public void execute(EvaluationState state, InstructionContext ctx) {
            ListNode iVisited = (ListNode)ctx;
            int size = iVisited.size();
            ArrayList list = new ArrayList(size);
            
            // backwards on purpose
            for (int i = 0; i < size - 1; i++) {
                list.add(0, state.deaggregateResult());
            }
            
            if (size > 0) {
                // normal add for the last one
                list.add(0, state.getResult());
            }
            
            state.setResult(state.runtime.newArray(list));
        }
    }
    private static final ArrayBuilder arrayBuilder = new ArrayBuilder();
    private static class ArrayNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		ArrayNode iVisited = (ArrayNode)ctx;
            
            state.addInstruction(iVisited, arrayBuilder);
            
            // backwards so nodes are eval'ed forwards
            for (ListIterator iterator = iVisited.reverseIterator(); iterator.hasPrevious();) {
                Node node = (Node)iterator.previous();
                
                state.addNodeInstruction(node);
                
                if (iterator.hasPrevious()) {
                    // more nodes coming; aggregate. Last node is not aggregated
                    state.addInstruction(iVisited, aggregateResult);
                }
            }
    	}
    }
    private static final ArrayNodeVisitor arrayNodeVisitor = new ArrayNodeVisitor();
    
    // Collapsing requires a way to break out method calls
    // CON20060104: This actually may not be a big deal, since the RubyRegexp functions are lightweight and run inline
    private static class BackRefNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		BackRefNode iVisited = (BackRefNode)ctx;
            IRubyObject backref = state.getThreadContext().getBackref();
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
            ThreadContext tc = state.getThreadContext();

            if (proc.isNil()) {
                tc.setNoBlock();
                try {
                    state.begin(iVisited.getIterNode());
                    return;
                } finally {
                    tc.clearNoBlock();
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
            
            Block block = (Block) tc.getCurrentBlock();
            if (block != null) {
                IRubyObject blockObject = block.getBlockObject();
                // The current block is already associated with the proc.  No need to create new
                // block for it.  Just eval!
                if (blockObject != null && blockObject == proc) {
            	    try {
                        tc.setBlockAvailable();
                	    state.begin(iVisited.getIterNode());
                	    return;
            	    } finally {
                        tc.clearBlockAvailable();
            	    }
                }
            }

            tc.preBlockPassEval(((RubyProc)proc).getBlock());
            
            try {
                state.begin(iVisited.getIterNode());
            } finally {
                tc.postBlockPassEval();
            }
    	}
    }
    private static final BlockPassNodeVisitor blockPassNodeVisitor = new BlockPassNodeVisitor();
    
    // Collapsed using new interpreter events
    private static class BreakThrower implements Instruction {
        public void execute(EvaluationState state, InstructionContext ctx) {
            // throw exception for code that continues to need it
            JumpException je = new JumpException(JumpException.JumpType.BreakJump);
            
            je.setPrimaryData(state.getResult());
            je.setSecondaryData(ctx);
            
            state.setCurrentException(je);
            
            throw je;
        }
    }
    private static final BreakThrower breakThrower = new BreakThrower();
    private static class BreakNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		BreakNode iVisited = (BreakNode)ctx;

            state.setResult(state.runtime.getNil());
            state.addInstruction(iVisited, breakThrower);
            if (iVisited.getValueNode() != null) {
                state.addNodeInstruction(iVisited.getValueNode());
            }
    	}
    }
    private static final BreakNodeVisitor breakNodeVisitor = new BreakNodeVisitor();
    
    // Collapsed, other than exception handling
    private static class ConstDeclNodeVisitor2 implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		ConstDeclNode iVisited = (ConstDeclNode)ctx;
            IRubyObject module;
            IRubyObject value;

    		if (iVisited.getPathNode() != null) {
   			 	module = state.deaggregateResult();
   			 	value = state.getResult();
    		} else { 
                ThreadContext tc = state.getThreadContext();
                
                // FIXME: why do we check RubyClass and then use CRef?
                if (tc.getRubyClass() == null) {
                    // TODO: wire into new exception handling mechanism
                    throw state.runtime.newTypeError("no class/module to define constant");
                }
                module = (RubyModule) tc.peekCRef().getValue();
                value = state.getResult();
            } 

            // FIXME: shouldn't we use the result of this set in setResult?
    		((RubyModule) module).setConstant(iVisited.getName(), value);
    		state.setResult(value);
    	}
    }
    private static final ConstDeclNodeVisitor2 constDeclNodeVisitor2 = new ConstDeclNodeVisitor2();
    private static class ConstDeclNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
            ConstDeclNode iVisited = (ConstDeclNode)ctx;
            
            state.clearResult();
            state.addInstruction(ctx, constDeclNodeVisitor2);
            if (iVisited.getPathNode() != null) {
                state.addNodeInstruction(iVisited.getPathNode());
                state.addInstruction(iVisited, aggregateResult);
            } 
            state.addNodeInstruction(iVisited.getValueNode());
    	}
    }
    private static final ConstDeclNodeVisitor constDeclNodeVisitor = new ConstDeclNodeVisitor();
    
    // Collapsed
    private static class ClassVarAsgnNodeVisitor1 implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		ClassVarAsgnNode iVisited = (ClassVarAsgnNode)ctx;
    		RubyModule rubyClass = (RubyModule) state.getThreadContext().peekCRef().getValue();
    		
            if (rubyClass == null) {
            	rubyClass = state.getSelf().getMetaClass();
            } else if (rubyClass.isSingleton()) {
                rubyClass = (RubyModule) rubyClass.getInstanceVariable("__attached__");
            }
            
            // FIXME shouldn't we use the return value for setResult?
        	rubyClass.setClassVar(iVisited.getName(), state.getResult());
    	}
    }
    private static final ClassVarAsgnNodeVisitor1 classVarAsgnNodeVisitor1 = new ClassVarAsgnNodeVisitor1();
    private static class ClassVarAsgnNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		ClassVarAsgnNode iVisited = (ClassVarAsgnNode)ctx;
    		state.addInstruction(ctx, classVarAsgnNodeVisitor1);
            state.addNodeInstruction(iVisited.getValueNode());
    	}
    }
    private static final ClassVarAsgnNodeVisitor classVarAsgnNodeVisitor = new ClassVarAsgnNodeVisitor();
    
    // Collapsed
    private static class ClassVarDeclNodeVisitor2 implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		ClassVarDeclNode iVisited = (ClassVarDeclNode)ctx;
            ((RubyModule)state.getThreadContext().peekCRef().getValue()).setClassVar(iVisited.getName(), state.getResult());
    	}
    }
    private static final ClassVarDeclNodeVisitor2 classVarDeclNodeVisitor2 = new ClassVarDeclNodeVisitor2();
    private static class ClassVarDeclNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		state.clearResult();
            ClassVarDeclNode iVisited = (ClassVarDeclNode)ctx;
            
            // FIXME: shouldn't we use cref here?
            if (state.getThreadContext().getRubyClass() == null) {
                throw state.runtime.newTypeError("no class/module to define class variable");
            }
            state.addInstruction(ctx, classVarDeclNodeVisitor2);
            state.addNodeInstruction(iVisited.getValueNode());
    	}
    }
    private static final ClassVarDeclNodeVisitor classVarDeclNodeVisitor = new ClassVarDeclNodeVisitor();
    
    // Not collapsed, but maybe nothing to be done?
    private static class ClassVarNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		ClassVarNode iVisited = (ClassVarNode)ctx;
        	RubyModule rubyClass = state.getThreadContext().getRubyClass();
        	
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
            ThreadContext tc = state.getThreadContext();
            
            tc.beginCallArgs();
            IRubyObject receiver = null;
            IRubyObject[] args = null;
            try {
                receiver = state.begin(iVisited.getReceiverNode());
                args = setupArgs(state, state.runtime, tc, iVisited.getArgsNode());
            } finally {
            	tc.endCallArgs();
            }
            assert receiver.getMetaClass() != null : receiver.getClass().getName();
            // If reciever is self then we do the call the same way as vcall
            CallType callType = (receiver == state.getSelf() ? CallType.VARIABLE : CallType.NORMAL);
            
            state.setResult(receiver.callMethod(iVisited.getName(), args, callType));
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

            state.getThreadContext().pollThreadEvents();
            
            Node firstWhenNode = iVisited.getFirstWhenNode();
            while (firstWhenNode != null) {
                if (!(firstWhenNode instanceof WhenNode)) {
                    state.begin(firstWhenNode);
                    return;
                }

                WhenNode whenNode = (WhenNode) firstWhenNode;

                if (whenNode.getExpressionNodes() instanceof ArrayNode) {
    		        for (Iterator iter = ((ArrayNode) whenNode.getExpressionNodes()).iterator(); iter.hasNext(); ) {
    		            Node tag = (Node) iter.next();

                        state.getThreadContext().setPosition(tag.getPosition());
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

                state.getThreadContext().pollThreadEvents();
                
                firstWhenNode = whenNode.getNextCase();
            }
            
            state.setResult(state.runtime.getNil());
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
            ThreadContext tc = state.getThreadContext();

            if (tc.getWrapper() != null) {
                rubyClass.extendObject(tc.getWrapper());
                rubyClass.includeModule(tc.getWrapper());
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
                state.setResult(((RubyModule) state.getResult()).getConstantFrom(iVisited.getName()));
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
                state.setResult(state.runtime.getObject().getConstantFrom(iVisited.getName()));
            } else {
            	state.clearResult();
            	state.addInstruction(ctx, colon2NodeVisitor1);
                state.addNodeInstruction(iVisited.getLeftNode());
            }
    	}
    }
    private static final Colon2NodeVisitor colon2NodeVisitor = new Colon2NodeVisitor();

    // No collapsing to do (depending on getConstant())
    private static class Colon3NodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		Colon3Node iVisited = (Colon3Node)ctx;
            state.setResult(state.runtime.getObject().getConstantFrom(iVisited.getName()));
    	}
    }
    private static final Colon3NodeVisitor colon3NodeVisitor = new Colon3NodeVisitor();
    
    // No collapsing to do (depending on getConstant())
    private static class ConstNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		ConstNode iVisited = (ConstNode)ctx;
            state.setResult(state.getThreadContext().getConstant(iVisited.getName()));
    	}
    }
    private static final ConstNodeVisitor constNodeVisitor = new ConstNodeVisitor();
    
    // Collapsed
    private static class DAsgnNodeVisitor1 implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		DAsgnNode iVisited = (DAsgnNode)ctx;
            state.getThreadContext().getCurrentDynamicVars().set(iVisited.getName(), state.getResult());
    	}
    }
    private static final DAsgnNodeVisitor1 dAsgnNodeVisitor1 = new DAsgnNodeVisitor1();
    private static class DAsgnNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		DAsgnNode iVisited = (DAsgnNode)ctx;
    		state.clearResult();
    		state.addInstruction(ctx, dAsgnNodeVisitor1);
            state.addNodeInstruction(iVisited.getValueNode());
    	}
    }
    private static final DAsgnNodeVisitor dAsgnNodeVisitor = new DAsgnNodeVisitor();
    
    // Collapsed; use of stack to aggregate strings could be optimized
    private static class DRegexpNodeVisitor2 implements Instruction {
        public void execute(EvaluationState state, InstructionContext ctx) {
            DRegexpNode iVisited = (DRegexpNode)ctx;
            
            // FIXME: oughta just stay as RubyString, rather than toString
            state.setResult(RubyRegexp.newRegexp(state.runtime, state.getResult().toString(), iVisited.getOptions(), null));
        }
    }
    private static final DRegexpNodeVisitor2 dRegexpNodeVisitor2 = new DRegexpNodeVisitor2();
    private static class DRegexpNodeVisitor implements Instruction {
        public void execute(EvaluationState state, InstructionContext ctx) {
            DRegexpNode iVisited = (DRegexpNode)ctx;

            state.addInstruction(iVisited, dRegexpNodeVisitor2);
            state.addInstruction(iVisited, dStrStringBuilder);
            
            for (ListIterator iterator = iVisited.reverseIterator(); iterator.hasPrevious();) {
                Node iterNode = (Node) iterator.previous();
                state.addNodeInstruction(iterNode);
                
                // aggregate if there's more nodes coming
                if (iterator.hasPrevious()) {
                    state.addInstruction(iVisited, aggregateResult);
                }
            }
        }
    }
    private static final DRegexpNodeVisitor dRegexpNodeVisitor = new DRegexpNodeVisitor();
    
    // Collapsed using result stack
    private static class DStrStringBuilder implements Instruction {
        public void execute(EvaluationState state, InstructionContext ctx) {
            ListNode iVisited = (ListNode)ctx;
            int size = iVisited.size();
            String[] strArray = new String[size];
            StringBuffer sb = new StringBuffer();
            
            // has to be done backwards, which kinda sucks
            for (int i = 0; i < size - 1; i++) {
                strArray[i] = state.deaggregateResult().toString();
            }
            
            if (size > 0) {
                // normal add for the last one
                strArray[size - 1] = state.getResult().toString();
            }
            
            for (int i = size - 1; i >= 0; i--) {
                sb.append(strArray[i]);
            }
            
            state.setResult(state.runtime.newString(sb.toString()));
        }
    }
    private static final DStrStringBuilder dStrStringBuilder = new DStrStringBuilder();
    private static class DStrNodeVisitor implements Instruction {
        public void execute(EvaluationState state, InstructionContext ctx) {
            DStrNode iVisited = (DStrNode)ctx;
            
            state.addInstruction(iVisited, dStrStringBuilder);

            for (ListIterator iterator = iVisited.reverseIterator(); iterator.hasPrevious();) {
                Node iterNode = (Node) iterator.previous();
                
                // FIXME: skipping null node...but why would we find null nodes in here?!
                if (iterNode == null) continue;
                
                state.addNodeInstruction(iterNode);

                // aggregate if there's more nodes coming
                if (iterator.hasPrevious()) {
                    state.addInstruction(iVisited, aggregateResult);
                }
            }
        }
    }
    private static final DStrNodeVisitor dStrNodeVisitor = new DStrNodeVisitor();
    
    // Collapsed using result stack
    private static class DSymbolNodeVisitor2 implements Instruction {
        public void execute(EvaluationState state, InstructionContext ctx) {
            // FIXME: oughta just stay RubyString rather than toString()
            state.setResult(state.runtime.newSymbol(state.getResult().toString()));
        }
    }
    private static final DSymbolNodeVisitor2 dSymbolNodeVisitor2 = new DSymbolNodeVisitor2();
    private static class DSymbolNodeVisitor implements Instruction {
        public void execute(EvaluationState state, InstructionContext ctx) {
            DSymbolNode iVisited = (DSymbolNode)ctx;
            
            state.addInstruction(iVisited, dSymbolNodeVisitor2);
            state.addInstruction(iVisited.getNode(), dStrStringBuilder);

            for (ListIterator iterator = iVisited.getNode().reverseIterator(); 
                iterator.hasPrevious();) {
                Node iterNode = (Node) iterator.previous();
                
                state.addNodeInstruction(iterNode);

                // aggregate if there's more nodes coming
                if (iterator.hasPrevious()) {
                    state.addInstruction(iVisited, aggregateResult);
                }
            }
        }
    }
    private static final DSymbolNodeVisitor dSymbolNodeVisitor = new DSymbolNodeVisitor();

    // Done; nothing to do
    private static class DVarNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		DVarNode iVisited = (DVarNode)ctx;
            state.setResult(state.getThreadContext().getDynamicValue(iVisited.getName()));
    	}
    }
    private static final DVarNodeVisitor dVarNodeVisitor = new DVarNodeVisitor();
    
    // Collapsed using result stack, other than a method call
    private static class DXStrNodeVisitor2 implements Instruction {
        public void execute(EvaluationState state, InstructionContext ctx) {
            state.setResult(state.getSelf().callMethod("`", state.getResult()));
        }
    }
    private static final DXStrNodeVisitor2 dXStrNodeVisitor2 = new DXStrNodeVisitor2();
    private static class DXStrNodeVisitor implements Instruction {
        public void execute(EvaluationState state, InstructionContext ctx) {
            DXStrNode iVisited = (DXStrNode)ctx;
            
            // Reuse string builder instruction for DStrNode
            state.addInstruction(iVisited, dXStrNodeVisitor2);
            state.addInstruction(iVisited, dStrStringBuilder);

            for (ListIterator iterator = iVisited.reverseIterator(); iterator.hasPrevious();) {
                Node iterNode = (Node) iterator.previous();
                
                state.addNodeInstruction(iterNode);
                
                if (iterator.hasPrevious()) {
                    state.addInstruction(iVisited, aggregateResult);
                }
            }
        }
    }
    private static final DXStrNodeVisitor dXStrNodeVisitor = new DXStrNodeVisitor();
    
    // Not collapsed; calls out to DefinedVisitor
    private static class DefinedNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		DefinedNode iVisited = (DefinedNode)ctx;
            String def = new DefinedVisitor(state).getDefinition(iVisited.getExpressionNode());
            if (def != null) {
                state.setResult(state.runtime.newString(def));
            } else {
                state.setResult(state.runtime.getNil());
            }
    	}
    }
    private static final DefinedNodeVisitor definedNodeVisitor = new DefinedNodeVisitor();
    
    // Not collapsed; big
    private static class DefnNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		DefnNode iVisited = (DefnNode)ctx;
            ThreadContext tc = state.getThreadContext();
            RubyModule containingClass = tc.getRubyClass();
            
            if (containingClass == null) {
                throw state.runtime.newTypeError("No class to add method.");
            }

            String name = iVisited.getName();
            if (containingClass == state.runtime.getObject() && name.equals("initialize")) {
                state.runtime.getWarnings().warn("redefining Object#initialize may cause infinite loop");
            }

            Visibility visibility = tc.getCurrentVisibility();
            if (name.equals("initialize") || visibility.isModuleFunction()) {
                visibility = Visibility.PRIVATE;
            }

            DefaultMethod newMethod = new DefaultMethod(containingClass, iVisited.getBodyNode(),
                                                        (ArgsNode) iVisited.getArgsNode(),
                                                        visibility,
                                                        tc.peekCRef());
            
            iVisited.getBodyNode().accept(new CreateJumpTargetVisitor(newMethod));
            
            containingClass.addMethod(name, newMethod);

            if (tc.getCurrentVisibility().isModuleFunction()) {
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
    													state.getThreadContext().peekCRef());

            iVisited.getBodyNode().accept(new CreateJumpTargetVisitor(newMethod));

            rubyClass.addMethod(iVisited.getName(), newMethod);
            receiver.callMethod("singleton_method_added", state.runtime.newSymbol(iVisited.getName()));

            state.clearResult();
    	}
    }
    private static final DefsNodeVisitor defsNodeVisitor = new DefsNodeVisitor();
    
    // Collapsed using result stack
    private static class DotNodeVisitor2 implements Instruction {
        public void execute(EvaluationState state, InstructionContext ctx) {
            DotNode iVisited = (DotNode)ctx;
            IRubyObject end = state.deaggregateResult();
            state.setResult(RubyRange.newRange(state.runtime, state.getResult(), end, iVisited.isExclusive()));
        }
    }
    private static final DotNodeVisitor2 dotNodeVisitor2 = new DotNodeVisitor2();
    private static class DotNodeVisitor implements Instruction {
        public void execute(EvaluationState state, InstructionContext ctx) {
            DotNode iVisited = (DotNode)ctx;
            
            state.addInstruction(iVisited, dotNodeVisitor2);

            state.addNodeInstruction(iVisited.getEndNode());
            state.addInstruction(iVisited, aggregateResult);
            state.addNodeInstruction(iVisited.getBeginNode());
        }
    }
    private static final DotNodeVisitor dotNodeVisitor = new DotNodeVisitor();
    
    // Collapsed
    private static class Ensurer implements Instruction {
        public void execute(EvaluationState state, InstructionContext ctx) {
            state.addInstruction(ctx, deaggregateResult);
            state.addNodeInstruction(ctx);
            state.addInstruction(ctx, aggregateResult);
        }
    }
    private static final Ensurer ensurer = new Ensurer();
    private static class EnsureNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		EnsureNode iVisited = (EnsureNode)ctx;

            if (iVisited.getEnsureNode() != null) {
                state.addEnsuredInstruction(iVisited.getEnsureNode(), ensurer);
            }
            
            state.addNodeInstruction(iVisited.getBodyNode());
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
            ThreadContext tc = state.getThreadContext();
            
            tc.beginCallArgs();
            IRubyObject[] args = null;
            try {
                args = setupArgs(state, state.runtime, tc, iVisited.getArgsNode());
            } finally {
            	tc.endCallArgs();
            }

            state.setResult(state.getSelf().callMethod(iVisited.getName(), args, CallType.FUNCTIONAL));
    	}
    }
    private static final FCallNodeVisitor fCallNodeVisitor = new FCallNodeVisitor();
    
    // Nothing to do
    private static class FalseNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
            state.setResult(state.runtime.getFalse());

            state.getThreadContext().pollThreadEvents();
    	}
    }
    private static final FalseNodeVisitor falseNodeVisitor = new FalseNodeVisitor();
    
    // Not collapsed; I do not understand this. It's pretty heinous.
    private static class FlipNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		FlipNode iVisited = (FlipNode)ctx;
            ThreadContext tc = state.runtime.getCurrentContext();
            if (iVisited.isExclusive()) {
                if (! tc.getFrameScope().getValue(iVisited.getCount()).isTrue()) {
                    //Benoit: I don't understand why the state.result is inversed
                    state.setResult(state.begin(iVisited.getBeginNode()).isTrue() ? state.runtime.getFalse() : state.runtime.getTrue());
                    tc.getFrameScope().setValue(iVisited.getCount(), state.getResult());
                } else {
                    if (state.begin(iVisited.getEndNode()).isTrue()) {
                        tc.getFrameScope().setValue(iVisited.getCount(), state.runtime.getFalse());
                    }
                    state.setResult(state.runtime.getTrue());
                }
            } else {
                if (! tc.getFrameScope().getValue(iVisited.getCount()).isTrue()) {
                    if (state.begin(iVisited.getBeginNode()).isTrue()) {
                        //Benoit: I don't understand why the state.result is inversed
                        tc.getFrameScope().setValue(iVisited.getCount(), state.begin(iVisited.getEndNode()).isTrue() ? state.runtime.getFalse() : state.runtime.getTrue());
                        state.setResult(state.runtime.getTrue());
                    } else {
                        state.setResult(state.runtime.getFalse());
                    }
                } else {
                    if (state.begin(iVisited.getEndNode()).isTrue()) {
                        tc.getFrameScope().setValue(iVisited.getCount(), state.runtime.getFalse());
                    }
                    state.setResult(state.runtime.getTrue());
                }
            }
    	}
    }
    private static final FlipNodeVisitor flipNodeVisitor = new FlipNodeVisitor();
    
    // Not collapsed; big and use of flow-control exceptions
    private static class ForNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		ForNode iVisited = (ForNode)ctx;
            ThreadContext tc = state.getThreadContext();
            
            tc.preForLoopEval(Block.createBlock(iVisited.getVarNode(), iVisited.getCallable(), state.getSelf()));
        	
            try {
                while (true) {
                    try {
                        ISourcePosition position = tc.getPosition();
                        tc.beginCallArgs();

                        IRubyObject recv = null;
                        try {
                            recv = state.begin(iVisited.getIterNode());
                        } finally {
                            tc.setPosition(position);
                            tc.endCallArgs();
                        }
                        
                        state.setResult(recv.callMethod("each", IRubyObject.NULL_ARRAY, CallType.NORMAL));
                        return;
                    } catch (JumpException je) {
                    	if (je.getJumpType() == JumpException.JumpType.RetryJump) {
                    		// do nothing, allow loop to retry
                    	} else {
                            state.setCurrentException(je);
                    		throw je;
                    	}
                    }
                }
            } catch (JumpException je) {
            	if (je.getJumpType() == JumpException.JumpType.BreakJump) {
	                IRubyObject breakValue = (IRubyObject)je.getPrimaryData();
	                
	                state.setResult(breakValue == null ? state.runtime.getNil() : breakValue);
            	} else {
                    state.setCurrentException(je);
            		throw je;
            	}
            } finally {
                tc.postForLoopEval();
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
    		state.addInstruction(ctx, globalAsgnNodeVisitor1);
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
    
    // Collapsed using result stack
    private static class HashNodeVisitor2 implements Instruction {
        public void execute(EvaluationState state, InstructionContext ctx) {
            HashNode iVisited = (HashNode)ctx;
            RubyHash hash = RubyHash.newHash(state.runtime);

            if (iVisited.getListNode() != null) {
                int size = iVisited.getListNode().size();
                
                for (int i = 0; i < (size / 2) - 1; i++) {
                    IRubyObject value = state.deaggregateResult();
                    IRubyObject key = state.deaggregateResult();
                    hash.aset(key, value);
                }
                
                if (size > 0) {
                    IRubyObject value = state.deaggregateResult();
                    IRubyObject key = state.getResult();
                    hash.aset(key, value);
                }
            }
            state.setResult(hash);
        }
    }
    private static final HashNodeVisitor2 hashNodeVisitor2 = new HashNodeVisitor2();
    private static class HashNodeVisitor implements Instruction {
        public void execute(EvaluationState state, InstructionContext ctx) {
            HashNode iVisited = (HashNode)ctx;
            
            state.addInstruction(iVisited, hashNodeVisitor2);

            if (iVisited.getListNode() != null) {
                for (ListIterator iterator = iVisited.getListNode().reverseIterator(); iterator.hasPrevious();) {
                    // insert all nodes in sequence, hash them in the final instruction
                    // KEY
                    state.addNodeInstruction((Node) iterator.previous());
                    
                    state.addInstruction(iVisited, aggregateResult);
                    
                    if (iterator.hasPrevious()) {
                        // VALUE
                        state.addNodeInstruction((Node) iterator.previous());
                        
                        if (iterator.hasPrevious()) {
                            state.addInstruction(iVisited, aggregateResult);
                        }
                    } else {
                        // XXX
                        throw new RuntimeException("[BUG] odd number list for Hash");
                        // XXX
                    }
                }
            }
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
    		state.addInstruction(ctx, instAsgnNodeVisitor1);
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
    		state.addInstruction(iVisited, ifNodeImplVisitor);
    		state.addNodeInstruction(iVisited.getCondition());
       	}
    }
    private static final IfNodeVisitor ifNodeVisitor = new IfNodeVisitor();
    
    // Not collapsed, depends on exception handling and function calls
    private static class IterNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		IterNode iVisited = (IterNode)ctx;
            ThreadContext tc = state.getThreadContext();
            
            tc.preIterEval(Block.createBlock(iVisited.getVarNode(), iVisited.getCallable(), state.getSelf()));
                try {
                    while (true) {
                        try {
                            tc.setBlockAvailable();
                            state.setResult(state.begin(iVisited.getIterNode()));
                            return;
                        } catch (JumpException je) {
                        	if (je.getJumpType() == JumpException.JumpType.RetryJump) {
                        		// allow loop to retry
                        	} else {
                                state.setCurrentException(je);
                        		throw je;
                        	}
                        } finally {
                            tc.clearBlockAvailable();
                        }
                    }
                } catch (JumpException je) {
                	if (je.getJumpType() == JumpException.JumpType.BreakJump) {
	                    IRubyObject breakValue = (IRubyObject)je.getPrimaryData();
	
	                    state.setResult(breakValue == null ? state.runtime.getNil() : breakValue);
                	} else {
                        state.setCurrentException(je);
                		throw je;
                	}
                } finally {
                    tc.postIterEval();
                }
    	}
    }
    private static final IterNodeVisitor iterNodeVisitor = new IterNodeVisitor();
    
    // Collapsed
    private static class LocalAsgnNodeVisitor1 implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		LocalAsgnNode iVisited = (LocalAsgnNode)ctx;
            state.runtime.getCurrentContext().getFrameScope().setValue(iVisited.getCount(), state.getResult());
        }
	}
    private static final LocalAsgnNodeVisitor1 localAsgnNodeVisitor1 = new LocalAsgnNodeVisitor1();
    private static class LocalAsgnNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		LocalAsgnNode iVisited = (LocalAsgnNode)ctx;
    		state.clearResult();
    		state.addInstruction(ctx, localAsgnNodeVisitor1);
            state.addNodeInstruction(iVisited.getValueNode());
    	}
    }
    private static final LocalAsgnNodeVisitor localAsgnNodeVisitor = new LocalAsgnNodeVisitor();
    
    // Nothing to do assuming getValue() isn't recursing
    private static class LocalVarNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		LocalVarNode iVisited = (LocalVarNode)ctx;
            state.setResult(state.runtime.getCurrentContext().getFrameScope().getValue(iVisited.getCount()));
    	}
    }
    private static final LocalVarNodeVisitor localVarNodeVisitor = new LocalVarNodeVisitor();
    
    // Not collapsed, calls out to AssignmentVisitor
    private static class MultipleAsgnNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		MultipleAsgnNode iVisited = (MultipleAsgnNode)ctx;
            state.setResult(new AssignmentVisitor(state).assign(iVisited, state.begin(iVisited.getValueNode()), false));
    	}
    }
    private static final MultipleAsgnNodeVisitor multipleAsgnNodeVisitor = new MultipleAsgnNodeVisitor(); 
    
    // Collapsed using result stack
    private static class Match2NodeVisitor2 implements Instruction {
        public void execute(EvaluationState state, InstructionContext ctx) {
            IRubyObject value = state.deaggregateResult();
            IRubyObject recv = state.getResult();
            
            state.setResult(((RubyRegexp) recv).match(value));
        }
    }
    private static final Match2NodeVisitor2 match2NodeVisitor2 = new Match2NodeVisitor2();
    private static class Match2NodeVisitor implements Instruction {
        public void execute(EvaluationState state, InstructionContext ctx) {
            Match2Node iVisited = (Match2Node)ctx;
            
            state.addInstruction(iVisited, match2NodeVisitor2);
            
            state.addNodeInstruction(iVisited.getValueNode());
            state.addInstruction(iVisited, aggregateResult);
            state.addNodeInstruction(iVisited.getReceiverNode());
        }
    }
    private static final Match2NodeVisitor match2NodeVisitor = new Match2NodeVisitor();
    
    // Collapsed using result stack, other than a method call
    private static class Match3NodeVisitor2 implements Instruction {
        public void execute(EvaluationState state, InstructionContext ctx) {
            IRubyObject value = state.deaggregateResult();
            IRubyObject receiver = state.getResult();
            if (value instanceof RubyString) {
                state.setResult(((RubyRegexp) receiver).match(value));
            } else {
                state.setResult(value.callMethod("=~", receiver));
            }
        }
    }
    private static final Match3NodeVisitor2 match3NodeVisitor2 = new Match3NodeVisitor2();
    private static class Match3NodeVisitor implements Instruction {
        public void execute(EvaluationState state, InstructionContext ctx) {
            Match3Node iVisited = (Match3Node)ctx;
            
            state.addInstruction(iVisited, match3NodeVisitor2);
            
            state.addNodeInstruction(iVisited.getValueNode());
            state.addInstruction(iVisited, aggregateResult);
            state.addNodeInstruction(iVisited.getReceiverNode());
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
    		state.addInstruction(ctx, matchNodeVisitor1);
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
            state.getThreadContext().setPosition(iVisited.getPosition());
            
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
    			state.addInstruction((Node)i.next(), newlineNodeTraceVisitor);
    		}
            
    		// Newlines flush result (result from previous line is not available in next line...perhaps makes sense)
            state.clearResult();
    	}
    }
    private static final NewlineNodeVisitor newlineNodeVisitor = new NewlineNodeVisitor();
    
    // Collapsed
    private static class NextThrower implements Instruction {
        public void execute(EvaluationState state, InstructionContext ctx) {
            // now used as an interpreter event
            JumpException je = new JumpException(JumpException.JumpType.NextJump);
            
            je.setPrimaryData(state.getResult());
            je.setSecondaryData(ctx);

            //state.setCurrentException(je);
            throw je;
        }
    }
    private static final NextThrower nextThrower = new NextThrower();
    private static class NextNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		NextNode iVisited = (NextNode)ctx;

            state.getThreadContext().pollThreadEvents();
            
            state.setResult(state.runtime.getNil());
            state.addInstruction(iVisited, nextThrower);
            if (iVisited.getValueNode() != null) {
                state.addNodeInstruction(iVisited.getValueNode());
            }
    	}
    }
    private static final NextNodeVisitor nextNodeVisitor = new NextNodeVisitor();
    
    // Nothing to do
    // FIXME: This is called for "NilNode" visited...shouldn't NilNode visits setResult(nil)?
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
    		state.addInstruction(ctx, notNodeVisitor1);
            state.addNodeInstruction(iVisited.getConditionNode());
    	}
    }
    private static final NotNodeVisitor notNodeVisitor = new NotNodeVisitor();
    
    // Not collapsed, method call - maybe ok since no dispatching
    private static class NthRefNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		NthRefNode iVisited = (NthRefNode)ctx;
            state.setResult(RubyRegexp.nth_match(iVisited.getMatchNumber(), state.getThreadContext().getBackref()));
    	}
    }
    private static final NthRefNodeVisitor nthRefNodeVisitor = new NthRefNodeVisitor();
    
    // Not collapsed, multiple evals to resolve
    private static class OpElementAsgnNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		OpElementAsgnNode iVisited = (OpElementAsgnNode)ctx;
            IRubyObject receiver = state.begin(iVisited.getReceiverNode());

            IRubyObject[] args = setupArgs(state, state.runtime, state.getThreadContext(), iVisited.getArgsNode());

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
            state.getThreadContext().pollThreadEvents();
    	}
    }
    private static final OpAsgnNodeVisitor opAsgnNodeVisitor = new OpAsgnNodeVisitor();
    
    private static class OpAsgnOrNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		OpAsgnOrNode iVisited = (OpAsgnOrNode) ctx;
    		String def = new DefinedVisitor(state).getDefinition(iVisited.getFirstNode());
    		
    		state.clearResult();
    		state.addInstruction(ctx, orNodeImplVisitor);
    		if (def != null) {
    			state.addNodeInstruction(iVisited.getFirstNode());
    		}
    	}
    }
    private static final OpAsgnOrNodeVisitor opAsgnOrNodeVisitor = new OpAsgnOrNodeVisitor();
    
    // Collapsed
    private static class OptNNodeGets implements Instruction {
        public void execute(EvaluationState state, InstructionContext ctx) {
            OptNNode iVisited = (OptNNode)ctx;
            
            // FIXME: exceptions could bubble out of this, but no exception handling available in system yet..
            if (RubyKernel.gets(state.runtime.getTopSelf(), IRubyObject.NULL_ARRAY).isTrue()) {
                // re-push body and self
                state.addBreakableInstruction(ctx, this);
                state.addNodeInstruction(iVisited.getBodyNode());
            } // else continue on out
        }
    }
    private static final OptNNodeGets optNNodeGets = new OptNNodeGets();
    private static class OptNNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		OptNNode iVisited = (OptNNode)ctx;
            
            state.addBreakableInstruction(iVisited, optNNodeGets);
            state.addRedoMarker(iVisited.getBodyNode());
            state.addNodeInstruction(iVisited.getBodyNode());

            state.getThreadContext().pollThreadEvents();
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
    		state.addInstruction(ctx, orNodeImplVisitor);
    		state.addNodeInstruction(iVisited.getFirstNode());	
    	}
    }
    private static final OrNodeVisitor orNodeVisitor = new OrNodeVisitor();
    
    // Collapsed; exception is now an interpreter event trigger
    private static class RedoNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
            state.getThreadContext().pollThreadEvents();
            
            // now used as an interpreter event
            JumpException je = new JumpException(JumpException.JumpType.RedoJump);
            
            je.setSecondaryData(ctx);
            
            throw je;
            //state.setCurrentException(je);
    	}
    }
    private static final RedoNodeVisitor redoNodeVisitor = new RedoNodeVisitor();
    
    // Collapsed, but FIXME I don't like the null check
    private static class RescueBodyNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		RescueBodyNode iVisited = (RescueBodyNode)ctx;
            if (iVisited.getBodyNode() != null) {
                state.addNodeInstruction(iVisited.getBodyNode());
            }
    	}
    }
    private static final RescueBodyNodeVisitor rescueBodyNodeVisitor = new RescueBodyNodeVisitor();
    
    // Collapsed
    private static class Rescuer implements Instruction {
        public void execute(EvaluationState state, InstructionContext ctx) {
            // dummy, just to mark enclosing rescuers
        }
    }
    private static final Rescuer rescuer = new Rescuer();
    private static class RescueNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		RescueNode iVisited = (RescueNode)ctx;
            
            state.addRescuableInstruction(iVisited, rescuer);
            if (iVisited.getElseNode() != null) {
                state.addNodeInstruction(iVisited.getElseNode());
            }
            state.addNodeInstruction(iVisited.getBodyNode());
    	}
    }
    private static final RescueNodeVisitor rescueNodeVisitor = new RescueNodeVisitor();
    
    // Collapsed; exception is now an interpreter event trigger
    private static class RetryNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
            state.getThreadContext().pollThreadEvents();
            
    		JumpException je = new JumpException(JumpException.JumpType.RetryJump);

            state.setCurrentException(je);
    		throw je;
    	}
    }
    private static final RetryNodeVisitor retryNodeVisitor = new RetryNodeVisitor();
    
    // Collapsed; exception is now an interpreter event trigger
    private static class ReturnNodeVisitor1 implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		ReturnNode iVisited = (ReturnNode)ctx;
    		
    		JumpException je = new JumpException(JumpException.JumpType.ReturnJump);
        		
    		je.setPrimaryData(iVisited.getTarget());
    		je.setSecondaryData(state.getResult());
            je.setTertiaryData(iVisited);

            state.setCurrentException(je);
    		throw je;
    	}
    }
    private static final ReturnNodeVisitor1 returnNodeVisitor1 = new ReturnNodeVisitor1();
    private static class ReturnNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		ReturnNode iVisited = (ReturnNode)ctx;
    		
    		state.addInstruction(ctx, returnNodeVisitor1);
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
            
            ThreadContext tc = state.getThreadContext();
            
            if (tc.getWrapper() != null) {
                singletonClass.extendObject(tc.getWrapper());
                singletonClass.includeModule(tc.getWrapper());
            }

            evalClassDefinitionBody(state, iVisited.getBodyNode(), singletonClass);
    	}
    }
    private static final SClassNodeVisitor sClassNodeVisitor = new SClassNodeVisitor();
    
    // Not collapsed, exception handling needed
    private static class ScopeNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		ScopeNode iVisited = (ScopeNode)ctx;
            ThreadContext tc = state.getThreadContext();
            
            tc.preScopedBody(iVisited.getLocalNames());
            try {
                state.begin(iVisited.getBodyNode());
            } finally {
                tc.postScopedBody();
            }
    	}
    }
    private static final ScopeNodeVisitor scopeNodeVisitor = new ScopeNodeVisitor();
    
    // Nothing to do
    private static class SelfNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
            state.setResult(state.getSelf());

            state.getThreadContext().pollThreadEvents();
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
    		state.addInstruction(ctx, splatNodeVisitor1);
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
    		state.addInstruction(ctx, sValueNodeVisitor1);
            state.addNodeInstruction(iVisited.getValue());
    	}
    }
    private static final SValueNodeVisitor sValueNodeVisitor = new SValueNodeVisitor();
    
    // Not collapsed, exceptions
    private static class SuperNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		SuperNode iVisited = (SuperNode)ctx;
            ThreadContext tc = state.getThreadContext();
            
            if (tc.getFrameLastClass() == null) {
                throw state.runtime.newNameError("Superclass method '" + tc.getFrameLastFunc() + "' disabled.");
            }

            tc.beginCallArgs();

            IRubyObject[] args = null;
            try {
                args = setupArgs(state, state.runtime, tc, iVisited.getArgsNode());
            } finally {
            	tc.endCallArgs();
            }
            state.setResult(tc.callSuper(args));
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
    		state.addInstruction(ctx, toAryNodeVisitor1);
            state.addNodeInstruction(iVisited.getValue());
    	}
    }
    private static final ToAryNodeVisitor toAryNodeVisitor = new ToAryNodeVisitor();

    // Nothing to do
    private static class TrueNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
            state.setResult(state.runtime.getTrue());

            state.getThreadContext().pollThreadEvents();
    	}
    }
    private static final TrueNodeVisitor trueNodeVisitor = new TrueNodeVisitor();
    
    // Not collapsed, exceptions
    private static class UndefNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		UndefNode iVisited = (UndefNode)ctx;
            ThreadContext tc = state.getThreadContext();
            
            if (tc.getRubyClass() == null) {
                throw state.runtime.newTypeError("No class to undef method '" + iVisited.getName() + "'.");
            }
            tc.getRubyClass().undef(iVisited.getName());
    	}
    }
    private static final UndefNodeVisitor undefNodeVisitor = new UndefNodeVisitor();
    
    // Collapsed
    private static class UntilConditionCheck implements Instruction {
        public void execute(EvaluationState state, InstructionContext ctx) {
            UntilNode iVisited = (UntilNode)ctx;
            
            // result contains condition check
            IRubyObject condition = state.getResult();
            
            if (!condition.isTrue()) {
                // re-push body, condition, and check
                state.addBreakableInstruction(iVisited, this);
                state.addNodeInstruction(iVisited.getConditionNode());
                if (iVisited.getBodyNode() != null) {
                    // FIXME: Hack?...bodynode came up as null for lex method in irb's ruby-lex.rb
                    state.addRedoMarker(iVisited.getBodyNode());
                    state.addNodeInstruction(iVisited.getBodyNode());
                }
            }
            // else loop terminates
        }
    }
    private static final UntilConditionCheck untilConditionCheck = new UntilConditionCheck();
    private static class UntilNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		UntilNode iVisited = (UntilNode)ctx;
                    
            state.addBreakableInstruction(iVisited, untilConditionCheck);
            state.addNodeInstruction(iVisited.getConditionNode());

            state.getThreadContext().pollThreadEvents();
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
    
    // Collapsed
    private static class WhileConditionCheck implements Instruction {
        public void execute(EvaluationState state, InstructionContext ctx) {
            WhileNode iVisited = (WhileNode)ctx;
            
            // result contains condition check
            IRubyObject condition = state.getResult();
            
            if (condition.isTrue()) {
                // re-push body, condition, and check
                state.addBreakableInstruction(iVisited, whileConditionCheck);
                state.addNodeInstruction(iVisited.getConditionNode());
                // FIXME: Hack? See UntilConditionCheck for explanation of why this may not be kosher
                if (iVisited.getBodyNode() != null) {
                    state.addRedoMarker(iVisited.getBodyNode());
                    state.addNodeInstruction(iVisited.getBodyNode());
                }
            }
            // else loop terminates
        }
    }
    private static final WhileConditionCheck whileConditionCheck = new WhileConditionCheck();
    private static class WhileNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		WhileNode iVisited = (WhileNode)ctx;
            
            state.addBreakableInstruction(iVisited, whileConditionCheck);
            state.addNodeInstruction(iVisited.getConditionNode());

            if (!iVisited.evaluateAtStart() && iVisited.getBodyNode() != null) {
                state.addRedoMarker(iVisited.getBodyNode());
                state.addNodeInstruction(iVisited.getBodyNode());
            }

            state.getThreadContext().pollThreadEvents();
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
    private static class Yield2 implements Instruction {
        public void execute(EvaluationState state, InstructionContext ctx) {
            YieldNode iVisited = (YieldNode)ctx;    
            // Special Hack...We cannot tell between no args and a nil one.
            // Change it back to null for now until a better solution is 
            // found
            // TODO: Find better way of differing...
            if (iVisited.getArgsNode() == null) {
                state.setResult(null);
            }
                
            state.setResult(state.getThreadContext().yieldCurrentBlock(state.getResult(), null, null, iVisited.getCheckState()));
        }
    }
    private static final Yield2 yield2 = new Yield2();
    private static class YieldNodeVisitor implements Instruction {
    	public void execute(EvaluationState state, InstructionContext ctx) {
    		YieldNode iVisited = (YieldNode)ctx;
            
            state.addInstruction(iVisited, yield2);
            if (iVisited.getArgsNode() != null) {
                state.addNodeInstruction(iVisited.getArgsNode());
            }
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
            ThreadContext tc = state.getThreadContext();
    		
            if (tc.getFrameLastClass() == null) {
                throw state.runtime.newNameError("superclass method '" + tc.getFrameLastFunc() + "' disabled");
            }

            state.setResult(tc.callSuper(tc.getFrameArgs()));
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
            state.setResult(RubyRegexp.newRegexp(state.runtime, iVisited.getPattern(), null));
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
    	return opAsgnOrNodeVisitor;
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
        ThreadContext tc = state.getThreadContext();
        
		tc.preClassEval(iVisited.getLocalNames(), type);

        IRubyObject oldSelf = state.getSelf();

        try {
            if (isTrace(state)) {
                callTraceFunction(state, "class", type);
            }

            state.setSelf(type);
            state.begin(iVisited.getBodyNode());
        } finally {
            state.setSelf(oldSelf);

            tc.postClassEval();

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

    private static RubyArray arrayValue(EvaluationState state, IRubyObject value) {
        IRubyObject newValue = value.convertToType("Array", "to_ary", false);

        if (newValue.isNil()) {
            // Object#to_a is obsolete.  We match Ruby's hack until to_a goes away.  Then we can 
            // remove this hack too.
            if (value.getType().searchMethod("to_a").getImplementationClass() != state.runtime.getKernel()) {
                newValue = value.convertToType("Array", "to_a", false);
                if(newValue.getType() != state.runtime.getClass("Array")) {
                    throw state.runtime.newTypeError("`to_a' did not return Array");
                }
            } else {
                newValue = state.runtime.newArray(value);
            }
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
        } else if (node instanceof Colon3Node) {
            enclosingModule = state.runtime.getObject(); 
        }
        
        if (enclosingModule == null) {
        	enclosingModule = (RubyModule)state.getThreadContext().peekCRef().getValue();
        }

        return enclosingModule;
    }
}
