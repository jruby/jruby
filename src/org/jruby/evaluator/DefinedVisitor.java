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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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

import java.util.Iterator;

import org.jruby.IRuby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.BackRefNode;
import org.jruby.ast.CallNode;
import org.jruby.ast.ClassVarAsgnNode;
import org.jruby.ast.ClassVarDeclNode;
import org.jruby.ast.ClassVarNode;
import org.jruby.ast.Colon2Node;
import org.jruby.ast.ConstDeclNode;
import org.jruby.ast.ConstNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.DVarNode;
import org.jruby.ast.FCallNode;
import org.jruby.ast.FalseNode;
import org.jruby.ast.GlobalAsgnNode;
import org.jruby.ast.GlobalVarNode;
import org.jruby.ast.InstVarNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.LocalVarNode;
import org.jruby.ast.Match2Node;
import org.jruby.ast.Match3Node;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.NilNode;
import org.jruby.ast.Node;
import org.jruby.ast.NthRefNode;
import org.jruby.ast.OpAsgnNode;
import org.jruby.ast.OpElementAsgnNode;
import org.jruby.ast.SelfNode;
import org.jruby.ast.SuperNode;
import org.jruby.ast.TrueNode;
import org.jruby.ast.VCallNode;
import org.jruby.ast.YieldNode;
import org.jruby.ast.ZSuperNode;
import org.jruby.ast.visitor.AbstractVisitor;
import org.jruby.exceptions.JumpException;
import org.jruby.runtime.ICallable;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * This visitor is used to evaluate a defined? statement.
 * 
 * @author jpetersen
 */
public class DefinedVisitor extends AbstractVisitor {
    private IRuby runtime;

    private String definition;

    public DefinedVisitor(IRuby runtime) {
        this.runtime = runtime;
    }

    public String getDefinition(Node expression) {
        definition = null;

        acceptNode(expression);

        return definition;
    }

    public String getArgumentDefinition(Node node, String type) {
        if (node == null) {
            return type;
        } else if (node instanceof ArrayNode) {
            Iterator iter = ((ArrayNode)node).iterator();
            while (iter.hasNext()) {
                if (getDefinition((Node)iter.next()) == null) {
                    return null;
                }
            }
        } else if (getDefinition(node) == null) {
            return null;
        }
        return type;
    }

    /**
     * @see AbstractVisitor#visitNode(Node)
     */
    protected Instruction visitNode(Node iVisited) {
        try {
            EvaluationState.eval(runtime.getCurrentContext(), iVisited, runtime.getCurrentContext().getFrameSelf());
            definition = "expression";
        } catch (JumpException jumpExcptn) {
        }
		return null;
    }

	/**
	 * @see AbstractVisitor#visitSuperNode(SuperNode)
	 */
	public Instruction visitSuperNode(SuperNode iVisited) {
        ThreadContext tc = runtime.getCurrentContext();
		String lastMethod = tc.getFrameLastFunc();
		RubyModule lastClass = tc.getFrameLastClass();
		if (lastMethod != null && lastClass != null
				&& lastClass.getSuperClass().isMethodBound(lastMethod, false)) {
			definition = getArgumentDefinition(iVisited.getArgsNode(), "super");
		}
		return null;
	}

	/**
	 * @see AbstractVisitor#visitZSuperNode(ZSuperNode)
	 */
	public Instruction visitZSuperNode(ZSuperNode iVisited) {
        ThreadContext tc = runtime.getCurrentContext();
		String lastMethod = tc.getFrameLastFunc();
		RubyModule lastClass = tc.getFrameLastClass();
		if (lastMethod != null && lastClass != null
				&& lastClass.getSuperClass().isMethodBound(lastMethod, false)) {
			definition = "super";
		}
		return null;
	}

	/**
	 * @see AbstractVisitor#visitCallNode(CallNode)
	 */
	public Instruction visitCallNode(CallNode iVisited) {
		if (getDefinition(iVisited.getReceiverNode()) != null) {
			try {
                IRubyObject receiver = EvaluationState.eval(runtime.getCurrentContext(), iVisited.getReceiverNode(), runtime.getCurrentContext().getFrameSelf());
				RubyClass metaClass = receiver.getMetaClass();
				ICallable method = metaClass.searchMethod(iVisited.getName());
				Visibility visibility = method.getVisibility();

				if (!visibility.isPrivate()
						&& (!visibility.isProtected() || runtime.getCurrentContext().getFrameSelf()
								.isKindOf(metaClass.getRealClass()))) {
					if (metaClass.isMethodBound(iVisited.getName(), false)) {
						definition = getArgumentDefinition(iVisited
								.getArgsNode(), "method");
						return null;
					}
				}
			} catch (JumpException excptn) {
			}
		}
		definition = null;
		return null;
	}

	/**
	 * @see AbstractVisitor#visitFCallNode(FCallNode)
	 */
	public Instruction visitFCallNode(FCallNode iVisited) {
		if (runtime.getCurrentContext().getFrameSelf().getMetaClass().isMethodBound(iVisited.getName(), false)) {
			definition = getArgumentDefinition(iVisited.getArgsNode(), "method");
		}
		return null;
	}

	/**
	 * @see AbstractVisitor#visitVCallNode(VCallNode)
	 */
	public Instruction visitVCallNode(VCallNode iVisited) {
		if (runtime.getCurrentContext().getFrameSelf().getMetaClass().isMethodBound(iVisited.getMethodName(), false)) {
			definition = "method";
		}
		return null;
	}

	/**
	 * @see AbstractVisitor#visitMatch2Node(Match2Node)
	 */
	public Instruction visitMatch2Node(Match2Node iVisited) {
		definition = "method";
		return null;
	}

	/**
	 * @see AbstractVisitor#visitMatch3Node(Match3Node)
	 */
	public Instruction visitMatch3Node(Match3Node iVisited) {
		definition = "method";
		return null;
	}

	/**
	 * @see AbstractVisitor#visitFalseNode(FalseNode)
	 */
	public Instruction visitFalseNode(FalseNode iVisited) {
		definition = "false";
		return null;
	}

	/**
	 * @see AbstractVisitor#visitNilNode(NilNode)
	 */
	public Instruction visitNilNode(NilNode iVisited) {
		definition = "nil";
		return null;
	}

	/**
	 * @see AbstractVisitor#visitNullNode()
	 */
	public Instruction visitNullNode() {
		definition = "expression";
		return null;
	}

	/**
	 * @see AbstractVisitor#visitNode(state.getSelf()Node)
	 */
	public Instruction visitSelfNode(SelfNode iVisited) {
		definition = "state.getSelf()";
		return null;
	}

	/**
	 * @see AbstractVisitor#visitTrueNode(TrueNode)
	 */
	public Instruction visitTrueNode(TrueNode iVisited) {
		definition = "true";
		return null;
	}

	/**
	 * @see AbstractVisitor#visitYieldNode(YieldNode)
	 */
	public Instruction visitYieldNode(YieldNode iVisited) {
		if (runtime.getCurrentContext().isBlockGiven()) {
			definition = "yield";
		}
		return null;
	}

	/**
	 * @see AbstractVisitor#visitClassVarAsgnNode(ClassVarAsgnNode)
	 */
	public Instruction visitClassVarAsgnNode(ClassVarAsgnNode iVisited) {
		definition = "assignment";
		return null;
	}

	/**
	 * @see AbstractVisitor#visitClassVarDeclNode(ClassVarDeclNode)
	 */
	public Instruction visitClassVarDeclNode(ClassVarDeclNode iVisited) {
		definition = "assignment";
		return null;
	}

	/**
	 * @see AbstractVisitor#visitConstDeclNode(ConstDeclNode)
	 */
	public Instruction visitConstDeclNode(ConstDeclNode iVisited) {
		definition = "assignment";
		return null;
	}

	/**
	 * @see AbstractVisitor#visitDAsgnNode(DAsgnNode)
	 */
	public Instruction visitDAsgnNode(DAsgnNode iVisited) {
		definition = "assignment";
		return null;
	}

	/**
	 * @see AbstractVisitor#visitGlobalAsgnNode(GlobalAsgnNode)
	 */
	public Instruction visitGlobalAsgnNode(GlobalAsgnNode iVisited) {
		definition = "assignment";
		return null;
	}

	/**
	 * @see AbstractVisitor#visitLocalAsgnNode(LocalAsgnNode)
	 */
	public Instruction visitLocalAsgnNode(LocalAsgnNode iVisited) {
		definition = "assignment";
		return null;
	}

	/**
	 * @see AbstractVisitor#visitMultipleAsgnNode(MultipleAsgnNode)
	 */
	public Instruction visitMultipleAsgnNode(MultipleAsgnNode iVisited) {
		definition = "assignment";
		return null;
	}

	/**
	 * @see AbstractVisitor#visitOpAsgnNode(OpAsgnNode)
	 */
	public Instruction visitOpAsgnNode(OpAsgnNode iVisited) {
		definition = "assignment";
		return null;
	}

	/**
	 * @see AbstractVisitor#visitOpElementAsgnNode(OpElementAsgnNode)
	 */
	public Instruction visitOpElementAsgnNode(OpElementAsgnNode iVisited) {
		definition = "assignment";
		return null;
	}

	/**
	 * @see AbstractVisitor#visitDVarNode(DVarNode)
	 */
	public Instruction visitDVarNode(DVarNode iVisited) {
		definition = "local-variable(in-block)";
		return null;
	}

	/**
	 * @see AbstractVisitor#visitLocalVarNode(LocalVarNode)
	 */
	public Instruction visitLocalVarNode(LocalVarNode iVisited) {
		definition = "local-variable";
		return null;
	}

	/**
	 * @see AbstractVisitor#visitClassVarNode(ClassVarNode)
	 */
	public Instruction visitClassVarNode(ClassVarNode iVisited) {
        ThreadContext tc = runtime.getCurrentContext();
        
		if (tc.getRubyClass() == null
				&& runtime.getCurrentContext().getFrameSelf().getMetaClass().isClassVarDefined(iVisited.getName())) {
			definition = "class_variable";
		} else if (!tc.getRubyClass().isSingleton()
				&& tc.getRubyClass().isClassVarDefined(
						iVisited.getName())) {
			definition = "class_variable";
		} else {
			RubyModule module = (RubyModule) tc.getRubyClass()
					.getInstanceVariable("__attached__");

			if (module != null && module.isClassVarDefined(iVisited.getName())) {
				definition = "class_variable";
			}
		}
		return null;
	}

	/**
	 * @see AbstractVisitor#visitConstNode(ConstNode)
	 */
	public Instruction visitConstNode(ConstNode iVisited) {
	    if (runtime.getCurrentContext().getConstantDefined(iVisited.getName())) {
	        definition = "constant";
	    }
		return null;
	}

	/**
	 * @see AbstractVisitor#visitGlobalVarNode(GlobalVarNode)
	 */
	public Instruction visitGlobalVarNode(GlobalVarNode iVisited) {
		if (runtime.getGlobalVariables().isDefined(iVisited.getName())) {
			definition = "global-variable";
		}
		return null;
	}

	/**
	 * @see AbstractVisitor#visitInstVarNode(InstVarNode)
	 */
	public Instruction visitInstVarNode(InstVarNode iVisited) {
		if (runtime.getCurrentContext().getFrameSelf().getInstanceVariable(iVisited.getName()) != null) {
			definition = "instance-variable";
		}
		return null;
	}

	/**
	 * @see AbstractVisitor#visitColon2Node(Colon2Node)
	 */
	public Instruction visitColon2Node(Colon2Node iVisited) {
		try {
            IRubyObject left = EvaluationState.eval(runtime.getCurrentContext(), iVisited.getLeftNode(), runtime.getCurrentContext().getFrameSelf());
			if (left instanceof RubyModule) {
				if (((RubyModule) left).getConstantAt(iVisited.getName()) != null) {
					definition = "constant";
				}
			} else if (left.getMetaClass().isMethodBound(iVisited.getName(),
					true)) {
				definition = "method";
			}
		} catch (JumpException excptn) {
		}
		return null;
	}

	/**
	 * @see AbstractVisitor#visitBackRefNode(BackRefNode)
	 * 
	 * @fixme Add test if back ref exists.
	 */
	public Instruction visitBackRefNode(BackRefNode iVisited) {
		// if () {
		definition = "$" + iVisited.getType();
		// }
		return null;
	}

	/**
	 * @see AbstractVisitor#visitNthRefNode(NthRefNode)
	 * 
	 * @fixme Add test if nth ref exists.
	 */
	public Instruction visitNthRefNode(NthRefNode iVisited) {
		// if () {
		definition = "$" + iVisited.getMatchNumber();
		// }
		return null;
	}
}