/*
 * ScopeNode.java - No description
 * Created on 14.01.2002, 20:48:10
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina, Chad Fowler
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Chad Fowler <chadfowler@yahoo.com>
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
package org.jruby.nodes;

import java.util.*;

import org.jruby.*;
import org.jruby.exceptions.*;
import org.jruby.nodes.types.*;
import org.jruby.runtime.*;
import org.jruby.util.*;
import org.jruby.util.collections.*;

/**
 * Scope in the parse tree.
 * indicates where in the parse tree a new scope should be started when evaling.
 * Unlike many node this is not created directly as part of the parce process,
 * rather it is created as a side effect of other things like creating a ClassNode 
 * or SClassNode.  It can also be created by evaling a DefnNode or DefsNode as 
 * part of the call to copyNodeScope.
 * the meaning of the Node fields for BlockNodes is:
 * <ul>
 * <li>
 * u1 ==&gt;  table (ExtendedList) NOTE: name of local variables (I think)
 * </li>
 * <li>
 * u2 ==&gt; refValue (CRefNode)	NOTE: const reference stack
 * </li>
 * <li>
 * u3 ==&gt;  nextNode (Node) NOTE: this is the content of the scope
 * </li>
 * </ul>

 * @author  jpetersen
 * @version $Revision$
 */
public class ScopeNode extends Node implements CallableNode {
	// private RubyPointer vars = null;
	// private RubyPointer localVarsList = null;

    public ScopeNode(List table, CRefNode refValue, Node nextNode) {
		super(Constants.NODE_SCOPE, table, refValue, nextNode);
	}

	/**
	 * eval the scopenode.
	 * pushes a new fraome on the tmpFrame stack and a new scope on the scope stack,
	 * fill in the new scope with this scope's local variables. execute teh nextNode (content of the scope)
	 **/

	public RubyObject eval(Ruby ruby, RubyObject self) {
		CRefNode savedCRef = null;

		ruby.getRubyFrame().tmpPush();
		ruby.getScope().push();
		CRefNode lRefValue = getRefValue();
		if (lRefValue != null) {
			savedCRef = ruby.getCRef();
			ruby.setCRef(lRefValue);
			ruby.getRubyFrame().setCbase(lRefValue);
		}

		if (getTable() != null) {
            ruby.getScope().setLocalValues(new ArrayList(Collections.nCopies(getTable().size(), ruby.getNil())));
			ruby.getScope().setLocalNames(getTable());
        } /* else
			{
			ruby.getRubyScope().setLocalValues(null);
			ruby.getRubyScope().setLocalNames(null);
			}*/ // unneeded, it is done in RubyScope.push() Benoit

		RubyObject result = getNextNode().eval(ruby, self);

		ruby.getScope().pop();
		ruby.getRubyFrame().tmpPop();

		if (savedCRef != null) {
			ruby.setCRef(savedCRef);
		}

		return result;
	}

	public RubyObject setupModule(Ruby ruby, RubyModule module) {
		// Node node = n;

		String file = ruby.getSourceFile();
		int line = ruby.getSourceLine();

		// TMP_PROTECT;

		ruby.getRubyFrame().tmpPush();
		ruby.pushClass();
		ruby.setRubyClass(module);
		ruby.setCBase(module); //CHAD
		ruby.getScope().push();
		RubyVarmap.push(ruby);

		if (getTable() != null) {
            ruby.getScope().setLocalValues(new ArrayList(Collections.nCopies(getTable().size(), ruby.getNil())));
			ruby.getScope().setLocalNames(getTable());
		} else {
			ruby.getScope().setLocalValues(null);
			ruby.getScope().setLocalNames(null);
		}

		// +++
		// if (ruby.getCRef() != null) {
		ruby.getCRef().push(module);
		// } else {
		//    ruby.setCRef(new CRefNode(module, null));
		// }
		// ---

		ruby.getRubyFrame().setCbase(ruby.getCRef());
		// PUSH_TAG(PROT_NONE);

		RubyObject result = null;

		// if (( state = EXEC_TAG()) == 0 ) {
		// if (trace_func) {
		//     call_trace_func("class", file, line, ruby_class,
		//                     ruby_frame->last_func, ruby_frame->last_class );
		// }
		result = getNextNode() != null ? getNextNode().eval(ruby, ruby.getRubyClass()) : ruby.getNil();
		// }

		// POP_TAG();
		ruby.getCRef().pop();
		RubyVarmap.pop(ruby);
		ruby.getScope().pop();
		ruby.popClass();
		ruby.getRubyFrame().tmpPop();
		//        if (trace_func){
		//            call_trace_func("end", file, line, 0, ruby_frame->last_func, ruby_frame->last_class );
		//        }
		// if (state != 0){
		//     JUMP_TAG(state);
		// }

		return result;
	}

	public RubyObject call(Ruby ruby, RubyObject recv, String id, RubyPointer args, boolean noSuper) {
		if (args == null) {
			args = new RubyPointer();
		}
		CRefNode savedCref = null; // +++ = null;

        List valueList = null;

		ruby.getScope().push();
		CRefNode lRefValue = getRefValue();
		if (lRefValue != null) {
			savedCref = ruby.getCRef(); // s.a.
			ruby.setCRef(lRefValue);
			ruby.getRubyFrame().setCbase(lRefValue);
		}

		if (getTable() != null) {
            valueList = new ArrayList(Collections.nCopies(getTable().size(), ruby.getNil()));

			ruby.getScope().setLocalValues(valueList);
			ruby.getScope().setLocalNames(getTable());
		} else {
			valueList = ruby.getScope().getLocalValues();

			ruby.getScope().setLocalValues(null);
			ruby.getScope().setLocalNames(null);
		}

		Node callBody = getNextNode();
		Node callNode = null;
		if (callBody.getType() == Constants.NODE_ARGS) {
			callNode = callBody;
			callBody = null;
		} else if (callBody.getType() == Constants.NODE_BLOCK) {
			callNode = callBody.getHeadNode();
			callBody = callBody.getNextNode();
		}

		RubyVarmap.push(ruby);
		// PUSH_TAG(PROT_FUNC);

		RubyObject result = ruby.getNil();

		try {
			if (callNode != null) {
				//if (call_node.getType() != Constants.NODE_ARGS) {
				// rb_bug("no argument-node");
				//}

				int i = callNode.getCount();
				if (i > (args != null ? args.size() : 0)) {
					int size = 0;
					if (args != null)
						size = args.size();
					throw new RubyArgumentException(ruby, getFile() + ":" + getLine() +"wrong # of arguments(" + size + " for " + i + ")");
				}
				if (callNode.getRest() == -1) {
					int opt = i;
					Node optNode = callNode.getOptNode();

					while (optNode != null) {
						opt++;
						optNode = optNode.getNextNode();
					}
					if (opt < (args != null ? args.size() : 0)) {
						throw new RubyArgumentException(ruby, "wrong # of arguments(" + args.size() + " for " + opt + ")");
					}

                    // +++
                    ruby.getRubyFrame().setArgs(valueList != null ? new DelegateList(valueList, 2, valueList.size()) : null);
                    // ---
				}

				if (valueList != null) {
					if (i > 0) {
						for (int j = 0; j < i; j++) {
							valueList.set(j + 2, args.get(j));
						}
					}

					args.inc(i);

					if (callNode.getOptNode() != null) {
						Node optNode = callNode.getOptNode();

						while (optNode != null && args.size() != 0) {
							((AssignableNode) optNode.getHeadNode()).assign(ruby, recv, args.getRuby(0), true);
							args.inc(1);
							optNode = optNode.getNextNode();
						}
						recv.eval(optNode);
					}
					if (callNode.getRest() >= 0) {
						RubyArray array = null;
						if (args.size() > 0) {
							array = RubyArray.newArray(ruby, args);
						} else {
							array = RubyArray.newArray(ruby, 0);
						}
						valueList.set(callNode.getRest(), array);
					}
				}
			}

			result = recv.eval(callBody);
		} catch (ReturnException rExcptn) {
			// +++ jpetersen
			result = ((RubyArray) rExcptn.getReturnValue()).pop();
			// ---
		}

		RubyVarmap.pop(ruby);

		ruby.getScope().pop();

		if (savedCref != null) {
			ruby.setCRef(savedCref);
		}

		return result;
	}
	/**
	 * Accept for the visitor pattern.
	 * @param iVisitor the visitor
	 **/
	public void accept(NodeVisitor iVisitor) {
		iVisitor.visitScopeNode(this);
	}
}
