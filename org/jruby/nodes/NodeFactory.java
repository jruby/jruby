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
	private Node setLine(Node iNode)
	{
		iNode.setLine(ph.getLine());
		iNode.setFile(ruby.getSourceFile());
		return iNode;
	}
	public Node newMethod(Node body, int noex) {
		return setLine(new MethodNode(body, noex));
	}

	public Node newFBody(Node bodyNode, String id, RubyModule origin) {
		return setLine(new FBodyNode(bodyNode, id, origin));
	}

	public Node newDefn(String mid, Node a, Node d, int p) {
		return setLine(new DefnNode(p, mid, newRFunc(a, d)));
	}

	public Node newDefs(Node recvNode, String mid, Node a, Node d) {
		return setLine(new DefsNode(recvNode, mid, newRFunc(a, d)));
	}

	public Node newCFunc(Callback cfunc) {
		return setLine(new CFuncNode(cfunc));
	}

	public Node newIFunc(Callback cfunc, RubyObject tval) {
		return setLine(new IFuncNode(cfunc, tval));
	}

	public ScopeNode newRFunc(Node b1, Node b2) {
		return newScope(ph.block_append(b1, b2));
	}

	public ScopeNode newScope(Node body) {
		return (ScopeNode)setLine(new ScopeNode(ph.local_tbl(), null, body));
	}

	public BlockNode newBlock(Node head) {
		return (BlockNode)setLine(new BlockNode(head));
	}

	public Node newIf(Node condition, Node body, Node elseBody) {
		return setLine(new IfNode(condition, body, elseBody));
	}

	public Node newUnless(Node condition, Node body, Node elseBody) {
		return setLine(newIf(condition, body, elseBody));
	}

	public Node newCase(Node head, Node body) {
		return setLine(new CaseNode(head, body));
	}

	public Node newWhen(Node head, Node body, Node next) {
		return setLine(new WhenNode(head, body, next));
	}

	public Node newOptN(Node body) {
		return setLine(new OptNNode(body));
	}

	public Node newWhile(Node condition, Node body) {
		return setLine(new WhileNode(condition, body));
	}

	public Node newUntil(Node condition, Node body) {
		return setLine(new UntilNode(condition, body));
	}

	public Node newFor(Node var, Node iter, Node body) {
		return setLine(new ForNode(var, body, iter));
	}

	public Node newIter(Node var, Node iter, Node body) {
		return setLine(new IterNode(var, body, iter));
	}

	public Node newBreak() {
		return setLine(new BreakNode());
	}

	public Node newNext() {
		return setLine(new NextNode());
	}

	public Node newRedo() {
		return setLine(new RedoNode());
	}

	public Node newRetry() {
		return setLine(new RetryNode());
	}

	public Node newBegin(Node body) {
		return setLine(new BeginNode(body));
	}

	public Node newRescue(Node headNode /*b*/, Node rescueNode, Node elseNode) {
		return setLine(new RescueNode(headNode, rescueNode, elseNode));
	}

	public RescueBodyNode newResBody(Node args, Node body, Node head /*n*/) {
		return (RescueBodyNode)setLine(new RescueBodyNode(head, body, args));
	}

	public Node newEnsure(Node head /*b*/, Node ensure) {
		return setLine(new EnsureNode(head, ensure));
	}

	public Node newReturn(Node stts) {
		return setLine(new ReturnNode(stts));
	}

	public Node newYield(Node stts) {
		return setLine(new YieldNode(stts));
	}

	public ArrayNode newList(Node head) {
		return newArray(head);
	}

	public ArrayNode newArray(Node head) {
		return (ArrayNode)setLine(new ArrayNode(head, 1, null));
	}

	public ZArrayNode newZArray() {
		return (ZArrayNode)setLine(new ZArrayNode());
	}

	public Node newHash(Node head)  {
		return (HashNode)setLine(new HashNode(head));
	}

	public Node newNot(Node body)   {
		return setLine(new NotNode(body));
	}

	public Node newMAsgn(Node valueNode, Node argsNode)   {
		return setLine(new MAsgnNode(valueNode, argsNode));
	}

	public Node newGAsgn(String vid, Node valueNode) {
		return setLine(new GAsgnNode(/*vid, */ valueNode, RubyGlobalEntry.getGlobalEntry(ruby, vid)));
	}

	public Node newLAsgn(String vid, Node valueNode) {
		return setLine(new LAsgnNode(valueNode, ph.local_cnt(vid)));
	}

	public Node newDAsgn(String vid, Node valueNode) {
		return setLine(new DAsgnNode(vid, valueNode));
	}

	public Node newDAsgnCurr(String vid, Node valueNode) {
		return setLine(new DAsgnCurrNode(vid, valueNode));
	}

	public Node newIAsgn(String vid, Node valueNode) {
		return setLine(new IAsgnNode(vid, valueNode));
	}

	public Node newCDecl(String vid, Node valueNode) {
		return setLine(new CDeclNode(vid, valueNode));
	}

	public Node newCVAsgn(String vid, Node valueNode) {
		return setLine(new CVAsgnNode(vid, valueNode));
	}

	public Node newCVDecl(String vid, Node valueNode) {
		return setLine(new CVDeclNode(vid, valueNode));
	}

	public Node newOpAsgn1(Node recvNode, String mid, Node argsNode) {
		return setLine(new OpAsgn1Node(recvNode, mid, argsNode));
	}

	public Node newOpAsgn2(Node recvNode, String vid, String mid, Node valueNode) {
		return setLine(new OpAsgn2Node(recvNode, valueNode, vid, mid));
	}

	/*public NODE newOpAsgn22(RubyId id, RubyId o) {
	  return setLine(new OpAsgn22Node(id, o, id.toAttrSetId()));
	  }*/

	public Node newOpAsgnOr(Node head, Node valueNode) {
		return setLine(new OpAsgnOrNode(head, valueNode, null));
	}

	public Node newOpAsgnAnd(Node head, Node valueNode) {
		return setLine(new OpAsgnAndNode(head, valueNode));
	}

	public Node newGVar(String id) {
		return setLine(new GVarNode(RubyGlobalEntry.getGlobalEntry(ruby, id)));
	}

	public Node newLVar(String vid) {
		return setLine(new LVarNode(ph.local_cnt(vid)));
	}

	public Node newDVar(String vid) {
		return setLine(new DVarNode(vid));
	}

	public Node newIVar(String vid) {
		return setLine(new IVarNode(vid));
	}

	public Node newConst(String vid) {
		return setLine(new ConstNode(vid));
	}

	public Node newCVar(String vid) {
		return setLine(new CVarNode(vid));
	}

	public Node newCVar2(String vid) {
		return setLine(new CVar2Node(vid));
	}

	public Node newNthRef(int nth)  {
		return setLine(new NthRefNode(nth));
	}

	public Node newBackRef(int nth) {
		return setLine(new BackRefNode(nth));
	}

	public Node newMatch(Node head) {
		return setLine(new MatchNode(head));
	}

	public Node newMatch2(Node recv, Node value) {
		return setLine(new Match2Node(recv, value));
	}

	public Node newMatch3(Node recv, Node value) {
		return setLine(new Match3Node(recv, value));
	}

	public Node newLit(RubyObject lit) {
		return setLine(new LitNode(lit));
	}

	public Node newStr(RubyObject str) {
		return setLine(new StrNode(str));
	}

	public Node newDStr(RubyObject str) {
		return setLine(new DStrNode(str));
	}

	public Node newXStr(RubyObject str) {
		return setLine(new XStrNode(str));
	}

	public Node newDXStr(RubyObject str) {
		return setLine(new DXStrNode(str));
	}

	public Node newEVStr(String s, int len) {
		return setLine(new EvStrNode(RubyString.newString(ruby, s, len)));
	}

	public Node newCall(Node recv, String mid, Node args) {
		return setLine(new CallNode(recv, mid, args));
	}

	public Node newFCall(String mid, Node args) {
		return setLine(new FCallNode(mid, args));
	}

	public Node newVCall(String mid) {
		return setLine(new VCallNode(mid));
	}

	public Node newSuper(Node args) {
		return setLine(new SuperNode(args));
	}

	public Node newZSuper() {
		return setLine(new ZSuperNode());
	}

	public Node newArgs(Integer count, Node optNode, int rest) {
		return setLine(new ArgsNode(optNode, rest, count != null ? count.intValue() : 0));
	}

	public Node newArgsCat(Node head /*a*/, Node body) {
		return setLine(new ArgsCatNode(head, body));
	}

	public Node newArgsPush(Node head /*a*/, Node body) {
		return setLine(new ArgsPushNode(head, body));
	}

	public Node newRestArgs(Node head) {
		return setLine(new RestArgsNode(head));
	}

	public Node newBlockArg(String vid) {
		return setLine(new BlockArgNode(ph.local_cnt(vid)));
	}

	public Node newBlockPass(Node bodyNode) {
		return setLine(new BlockPassNode(bodyNode));
	}

	public Node newAlias(String newId, String oldId) {
		return setLine(new AliasNode(oldId, newId));
	}

	public Node newVAlias(String newId, String oldId) {
		return setLine(new VAliasNode(oldId, newId));
	}

	public Node newUndef(String mid) {
		return setLine(new UndefNode(mid));
	}

	public Node newClass(String cnameId, Node body, Node superNode) {
		return setLine(new ClassNode(cnameId, newScope(body), superNode));
	}

	public Node newSClass(Node receiverNode, Node body) {
		return setLine(new SClassNode(receiverNode, newScope(body)));
	}

	public Node newModule(String moduleName, Node body) {
		return setLine(new ModuleNode(moduleName, newScope(body)));
	}

	public Node newColon2(Node head, String mid) {
		return setLine(new Colon2Node(head, mid));
	}

	public Node newColon3(String mid) {
		return setLine(new Colon3Node(mid));
	}

	/*public Node newCRef(Object c) {
	  return setLine(new CRefNode(c));
	  }*/

	public Node newDot2(Node begin, Node end) {
		return setLine(new DotNode(begin, end, false));
	}

	public Node newDot3(Node begin, Node end) {
		return setLine(new DotNode(begin, end, true));
	}

	public Node newAttrSet(String vid /*a*/) {
		return setLine(new AttrSetNode(vid));
	}

	public Node newSelf() {
		return setLine(new SelfNode());
	}

	public Node newNil() {
		return setLine(new NilNode());
	}

	public Node newTrue() {
		return setLine(new TrueNode());
	}

	public Node newFalse() {
		return setLine(new FalseNode());
	}

	public Node newDefined(Node head /* e */) {
		return setLine(new DefinedNode(head));
	}

	public NewlineNode newNewline(Node nextNode) {
		return (NewlineNode)setLine(new NewlineNode(nextNode));
	}

	public Node newPreExe(Node body) {
		return newScope(body);
	}

	public Node newPostExe() {
		return setLine(new PostExeNode());
	}

	/*public Node newDMethod(Object b) {
	  return setLine(new DMethodNode(b));
	  }

	  public Node newBMethod(Object b) {
	  return setLine(new BMethodNode(b));
	  }*/
}
