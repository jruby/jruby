/*
 * NodeFactory.java - No description
 * Created on 05. November 2001, 21:46
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
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

import org.jruby.*;
import org.jruby.parser.*;
import org.jruby.runtime.*;

/**
 *
 * @author  jpetersen
 * @version 
 */
public class NodeFactory {
	private Ruby ruby;
	private ParserHelper ph;

	public NodeFactory(Ruby ruby) {
		this.ruby = ruby;

		ph = ruby.getParserHelper();
	}
	private Node setFileNLine(Node iNode)
	{
		iNode.setLine(ph.getLine());
		iNode.setFile(ruby.getSourceFile());
		return iNode;
	}
	public Node newMethod(Node body, int noex) {
		return setFileNLine(new MethodNode(body, noex));
	}

	public Node newFBody(Node bodyNode, String id, RubyModule origin) {
		return setFileNLine(new FBodyNode(bodyNode, id, origin));
	}

	/**
	 * Builds a method definition node
	 * @param mid method identifier
	 * @param args method arguments node
	 * @param body method body node
	 * @param noex
	 * @return newly built node 
	 **/
	public Node newDefn(String mid, Node args, Node body, int noex) {
		return setFileNLine(new DefnNode(p, mid, newRFunc(a, d)));
	}

	public Node newDefs(Node recvNode, String mid, Node a, Node d) {
		return setFileNLine(new DefsNode(recvNode, mid, newRFunc(a, d)));
	}

	public Node newCFunc(Callback cfunc) {
		return setFileNLine(new CFuncNode(cfunc));
	}

	public Node newIFunc(Callback cfunc, RubyObject tval) {
		return setFileNLine(new IFuncNode(cfunc, tval));
	}

	public ScopeNode newRFunc(Node b1, Node b2) {
		return newScope(ph.block_append(b1, b2));
	}

	public ScopeNode newScope(Node body) {
		return (ScopeNode)setFileNLine(new ScopeNode(ph.local_tbl(), null, body));
	}

	public BlockNode newBlock(Node head) {
		return (BlockNode)setFileNLine(new BlockNode(head));
	}

	public Node newIf(Node condition, Node body, Node elseBody) {
		return setFileNLine(new IfNode(condition, body, elseBody));
	}

	public Node newUnless(Node condition, Node body, Node elseBody) {
		return setFileNLine(newIf(condition, body, elseBody));
	}

	public Node newCase(Node head, Node body) {
		return setFileNLine(new CaseNode(head, body));
	}

	public Node newWhen(Node head, Node body, Node next) {
		return setFileNLine(new WhenNode(head, body, next));
	}

	public Node newOptN(Node body) {
		return setFileNLine(new OptNNode(body));
	}

	public Node newWhile(Node condition, Node body) {
		return setFileNLine(new WhileNode(condition, body));
	}

	public Node newUntil(Node condition, Node body) {
		return setFileNLine(new UntilNode(condition, body));
	}

	public Node newFor(Node var, Node iter, Node body) {
		return setFileNLine(new ForNode(var, body, iter));
	}

	public Node newIter(Node var, Node iter, Node body) {
		return setFileNLine(new IterNode(var, body, iter));
	}

	public Node newBreak() {
		return setFileNLine(new BreakNode());
	}

	public Node newNext() {
		return setFileNLine(new NextNode());
	}

	public Node newRedo() {
		return setFileNLine(new RedoNode());
	}

	public Node newRetry() {
		return setFileNLine(new RetryNode());
	}

	public Node newBegin(Node body) {
		return setFileNLine(new BeginNode(body));
	}

	public Node newRescue(Node headNode /*b*/, Node rescueNode, Node elseNode) {
		return setFileNLine(new RescueNode(headNode, rescueNode, elseNode));
	}

	public RescueBodyNode newResBody(Node args, Node body, Node head /*n*/) {
		return (RescueBodyNode)setFileNLine(new RescueBodyNode(head, body, args));
	}

	public Node newEnsure(Node head /*b*/, Node ensure) {
		return setFileNLine(new EnsureNode(head, ensure));
	}

	public Node newReturn(Node stts) {
		return setFileNLine(new ReturnNode(stts));
	}

	public Node newYield(Node stts) {
		return setFileNLine(new YieldNode(stts));
	}

	public ArrayNode newList(Node head) {
		return newArray(head);
	}

	public ArrayNode newArray(Node head) {
		return (ArrayNode)setFileNLine(new ArrayNode(head, 1, null));
	}

	public ZArrayNode newZArray() {
		return (ZArrayNode)setFileNLine(new ZArrayNode());
	}

	public Node newHash(Node head)  {
		return (HashNode)setFileNLine(new HashNode(head));
	}

	public Node newNot(Node body)   {
		return setFileNLine(new NotNode(body));
	}

	public Node newMAsgn(Node valueNode, Node argsNode)   {
		return setFileNLine(new MAsgnNode(valueNode, argsNode));
	}

	public Node newGAsgn(String vid, Node valueNode) {
		return setFileNLine(new GAsgnNode(/*vid, */ valueNode, RubyGlobalEntry.getGlobalEntry(ruby, vid)));
	}

	public Node newLAsgn(String vid, Node valueNode) {
		return setFileNLine(new LAsgnNode(valueNode, ph.local_cnt(vid)));
	}

	public Node newDAsgn(String vid, Node valueNode) {
		return setFileNLine(new DAsgnNode(vid, valueNode));
	}

	public Node newDAsgnCurr(String vid, Node valueNode) {
		return setFileNLine(new DAsgnCurrNode(vid, valueNode));
	}

	public Node newIAsgn(String vid, Node valueNode) {
		return setFileNLine(new IAsgnNode(vid, valueNode));
	}

	public Node newCDecl(String vid, Node valueNode) {
		return setFileNLine(new CDeclNode(vid, valueNode));
	}

	public Node newCVAsgn(String vid, Node valueNode) {
		return setFileNLine(new CVAsgnNode(vid, valueNode));
	}

	public Node newCVDecl(String vid, Node valueNode) {
		return setFileNLine(new CVDeclNode(vid, valueNode));
	}

	public Node newOpAsgn1(Node recvNode, String mid, Node argsNode) {
		return setFileNLine(new OpAsgn1Node(recvNode, mid, argsNode));
	}

	public Node newOpAsgn2(Node recvNode, String vid, String mid, Node valueNode) {
		return setFileNLine(new OpAsgn2Node(recvNode, valueNode, vid, mid));
	}

	/*public NODE newOpAsgn22(RubyId id, RubyId o) {
	  return setFileNLine(new OpAsgn22Node(id, o, id.toAttrSetId()));
	  }*/

	public Node newOpAsgnOr(Node head, Node valueNode) {
		return setFileNLine(new OpAsgnOrNode(head, valueNode, null));
	}

	public Node newOpAsgnAnd(Node head, Node valueNode) {
		return setFileNLine(new OpAsgnAndNode(head, valueNode));
	}

	public Node newGVar(String id) {
		return setFileNLine(new GVarNode(RubyGlobalEntry.getGlobalEntry(ruby, id)));
	}

	public Node newLVar(String vid) {
		return setFileNLine(new LVarNode(ph.local_cnt(vid)));
	}

	public Node newDVar(String vid) {
		return setFileNLine(new DVarNode(vid));
	}

	public Node newIVar(String vid) {
		return setFileNLine(new IVarNode(vid));
	}

	public Node newConst(String vid) {
		return setFileNLine(new ConstNode(vid));
	}

	public Node newCVar(String vid) {
		return setFileNLine(new CVarNode(vid));
	}

	public Node newCVar2(String vid) {
		return setFileNLine(new CVar2Node(vid));
	}

	public Node newNthRef(int nth)  {
		return setFileNLine(new NthRefNode(nth));
	}

	public Node newBackRef(int nth) {
		return setFileNLine(new BackRefNode(nth));
	}

	public Node newMatch(Node head) {
		return setFileNLine(new MatchNode(head));
	}

	public Node newMatch2(Node recv, Node value) {
		return setFileNLine(new Match2Node(recv, value));
	}

	public Node newMatch3(Node recv, Node value) {
		return setFileNLine(new Match3Node(recv, value));
	}

	public Node newLit(RubyObject lit) {
		return setFileNLine(new LitNode(lit));
	}

	public Node newStr(RubyObject str) {
		return setFileNLine(new StrNode(str));
	}

	public Node newDStr(RubyObject str) {
		return setFileNLine(new DStrNode(str));
	}

	public Node newXStr(RubyObject str) {
		return setFileNLine(new XStrNode(str));
	}

	public Node newDXStr(RubyObject str) {
		return setFileNLine(new DXStrNode(str));
	}

	public Node newEVStr(String s, int len) {
		return setFileNLine(new EvStrNode(RubyString.newString(ruby, s, len)));
	}

	public Node newCall(Node recv, String mid, Node args) {
		return setFileNLine(new CallNode(recv, mid, args));
	}

	public Node newFCall(String mid, Node args) {
		return setFileNLine(new FCallNode(mid, args));
	}

	public Node newVCall(String mid) {
		return setFileNLine(new VCallNode(mid));
	}

	public Node newSuper(Node args) {
		return setFileNLine(new SuperNode(args));
	}

	public Node newZSuper() {
		return setFileNLine(new ZSuperNode());
	}

	public Node newArgs(Integer count, Node optNode, int rest) {
		return setFileNLine(new ArgsNode(optNode, rest, count != null ? count.intValue() : 0));
	}

	public Node newArgsCat(Node head /*a*/, Node body) {
		return setFileNLine(new ArgsCatNode(head, body));
	}

	public Node newArgsPush(Node head /*a*/, Node body) {
		return setFileNLine(new ArgsPushNode(head, body));
	}

	public Node newRestArgs(Node head) {
		return setFileNLine(new RestArgsNode(head));
	}

	public Node newBlockArg(String vid) {
		return setFileNLine(new BlockArgNode(ph.local_cnt(vid)));
	}

	public Node newBlockPass(Node bodyNode) {
		return setFileNLine(new BlockPassNode(bodyNode));
	}

	public Node newAlias(String newId, String oldId) {
		return setFileNLine(new AliasNode(oldId, newId));
	}

	public Node newVAlias(String newId, String oldId) {
		return setFileNLine(new VAliasNode(oldId, newId));
	}

	public Node newUndef(String mid) {
		return setFileNLine(new UndefNode(mid));
	}

	public Node newClass(String cnameId, Node body, Node superNode) {
		return setFileNLine(new ClassNode(cnameId, newScope(body), superNode));
	}

	public Node newSClass(Node receiverNode, Node body) {
		return setFileNLine(new SClassNode(receiverNode, newScope(body)));
	}

	public Node newModule(String moduleName, Node body) {
		return setFileNLine(new ModuleNode(moduleName, newScope(body)));
	}

	public Node newColon2(Node head, String mid) {
		return setFileNLine(new Colon2Node(head, mid));
	}

	public Node newColon3(String mid) {
		return setFileNLine(new Colon3Node(mid));
	}

	/*public Node newCRef(Object c) {
	  return setFileNLine(new CRefNode(c));
	  }*/

	public Node newDot2(Node begin, Node end) {
		return setFileNLine(new DotNode(begin, end, false));
	}

	public Node newDot3(Node begin, Node end) {
		return setFileNLine(new DotNode(begin, end, true));
	}

	public Node newAttrSet(String vid /*a*/) {
		return setFileNLine(new AttrSetNode(vid));
	}

	public Node newSelf() {
		return setFileNLine(new SelfNode());
	}

	public Node newNil() {
		return setFileNLine(new NilNode());
	}

	public Node newTrue() {
		return setFileNLine(new TrueNode());
	}

	public Node newFalse() {
		return setFileNLine(new FalseNode());
	}

	public Node newDefined(Node head /* e */) {
		return setFileNLine(new DefinedNode(head));
	}

	public NewlineNode newNewline(Node nextNode) {
		return (NewlineNode)setFileNLine(new NewlineNode(nextNode));
	}

	public Node newPreExe(Node body) {
		return newScope(body);
	}

	public Node newPostExe() {
		return setFileNLine(new PostExeNode());
	}

	/*public Node newDMethod(Object b) {
	  return setFileNLine(new DMethodNode(b));
	  }

	  public Node newBMethod(Object b) {
	  return setFileNLine(new BMethodNode(b));
	  }*/
}
