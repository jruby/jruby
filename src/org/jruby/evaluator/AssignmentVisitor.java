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
 * Copyright (C) 2003-2004 Thomas E Enebo <enebo@acm.org>
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

import org.jruby.RubyArray;
import org.jruby.RubyModule;
import org.jruby.ast.CallNode;
import org.jruby.ast.ClassVarAsgnNode;
import org.jruby.ast.ClassVarDeclNode;
import org.jruby.ast.ConstDeclNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.GlobalAsgnNode;
import org.jruby.ast.InstAsgnNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.Node;
import org.jruby.ast.visitor.AbstractVisitor;
import org.jruby.runtime.CallType;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * 
 * @author jpetersen
 */
public class AssignmentVisitor extends AbstractVisitor {
    private EvaluationState state;
    private IRubyObject value;
    private boolean check;

    public AssignmentVisitor(EvaluationState state) {
        this.state = state;
    }

    public IRubyObject assign(Node node, IRubyObject aValue, boolean aCheck) {
        this.value = aValue;
        this.check = aCheck;

        acceptNode(node);

        return state.getResult();
    }

	/**
	 * @see AbstractVisitor#visitNode(Node)
	 */
	protected Instruction visitNode(Node iVisited) {
		assert false;
		return null;
	}

	/**
	 * @see AbstractVisitor#visitCallNode(CallNode)
	 */
	public Instruction visitCallNode(CallNode iVisited) {
        IRubyObject receiver = state.begin(iVisited.getReceiverNode());

        if (iVisited.getArgsNode() == null) { // attribute set.
            receiver.callMethod(iVisited.getName(), new IRubyObject[] {value}, CallType.NORMAL);
        } else { // element set
            RubyArray args = (RubyArray)state.begin(iVisited.getArgsNode());
            args.append(value);
            receiver.callMethod(iVisited.getName(), args.toJavaArray(), CallType.NORMAL);
        }
		return null;
	}

	/**
	 * @see AbstractVisitor#visitClassVarAsgnNode(ClassVarAsgnNode)
	 */
	public Instruction visitClassVarAsgnNode(ClassVarAsgnNode iVisited) {
		state.getThreadContext().getRubyClass().setClassVar(iVisited.getName(), value);
		return null;
	}

	/**
	 * @see AbstractVisitor#visitClassVarDeclNode(ClassVarDeclNode)
	 */
	public Instruction visitClassVarDeclNode(ClassVarDeclNode iVisited) {
		if (state.runtime.getVerbose().isTrue()
				&& state.getThreadContext().getRubyClass().isSingleton()) {
            state.runtime.getWarnings().warn(iVisited.getPosition(),
					"Declaring singleton class variable.");
		}
        state.getThreadContext().getRubyClass().setClassVar(iVisited.getName(), value);
		return null;
	}

	/**
	 * @see AbstractVisitor#visitConstDeclNode(ConstDeclNode)
	 */
	public Instruction visitConstDeclNode(ConstDeclNode iVisited) {
		if (iVisited.getPathNode() == null) {
			state.getThreadContext().getRubyClass().defineConstant(iVisited.getName(), value);
		} else {
			((RubyModule) state.begin(iVisited.getPathNode())).defineConstant(iVisited.getName(), value);
		}
		return null;
	}

	/**
	 * @see AbstractVisitor#visitDAsgnNode(DAsgnNode)
	 */
	public Instruction visitDAsgnNode(DAsgnNode iVisited) {
        state.getThreadContext().getCurrentDynamicVars().set(iVisited.getName(), value);
		return null;
	}

	/**
	 * @see AbstractVisitor#visitGlobalAsgnNode(GlobalAsgnNode)
	 */
	public Instruction visitGlobalAsgnNode(GlobalAsgnNode iVisited) {
        state.runtime.getGlobalVariables().set(iVisited.getName(), value);
		return null;
	}

	/**
	 * @see AbstractVisitor#visitInstAsgnNode(InstAsgnNode)
	 */
	public Instruction visitInstAsgnNode(InstAsgnNode iVisited) {
		state.getSelf().setInstanceVariable(iVisited.getName(), value);
		return null;
	}

	/**
	 * @see AbstractVisitor#visitLocalAsgnNode(LocalAsgnNode)
	 */
	public Instruction visitLocalAsgnNode(LocalAsgnNode iVisited) {
        state.runtime.getCurrentContext().getCurrentScope().setValue(iVisited.getCount(), value);
		return null;
	}

	/**
	 * @see AbstractVisitor#visitMultipleAsgnNode(MultipleAsgnNode)
	 */
	public Instruction visitMultipleAsgnNode(MultipleAsgnNode iVisited) {
		if (!(value instanceof RubyArray)) {
			value = RubyArray.newArray(state.runtime, value);
		}
        state.setResult(state.getThreadContext()
				.mAssign(state.getSelf(), iVisited, (RubyArray) value, check));
		return null;
	}
}