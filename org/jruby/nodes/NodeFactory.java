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
import org.jruby.core.*;
import org.jruby.runtime.*;
import org.jruby.runtime.RubyGlobalEntry; // TMP
import org.jruby.parser.*;
import org.jruby.util.*;

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

    public Node newMethod(Node body, int noex) {
        return new MethodNode(body, noex);
    }
    
    public Node newFBody(Node bodyNode, RubyId id, RubyModule origin) {
        return new FBodyNode(bodyNode, id, origin);
    }
        
    public Node newDefn(RubyId mid, Node a, Node d, int p) {
        return new DefnNode(p, mid, newRFunc(a, d));
    }
    
    public Node newDefs(Node recvNode, RubyId mid, Node a, Node d) {
        return new DefsNode(recvNode, mid, newRFunc(a, d));
    }
    
    public Node newCFunc(RubyCallbackMethod cfunc) {
        return new CFuncNode(cfunc);
    }
    
    public Node newIFunc(RubyCallbackMethod cfunc, RubyObject tval) {
        return new IFuncNode(cfunc, tval);
    }
       
    public ScopeNode newRFunc(Node b1, Node b2) {
        return newScope(ph.block_append(b1, b2));
    }
       
    public ScopeNode newScope(Node body) {
        return new ScopeNode(ph.local_tbl(), null, body);
    }
       
    public BlockNode newBlock(Node head) {
        return new BlockNode(head);
    }
       
    public Node newIf(Node condition, Node body, Node elseBody) {
        return new IfNode(condition, body, elseBody);
    }
       
    public Node newUnless(Node condition, Node body, Node elseBody) {
        return newIf(condition, body, elseBody);
    }
    
    public Node newCase(Node head, Node body) {
        return new CaseNode(head, body);
    }
       
    public Node newWhen(Node head, Node body, Node next) {
        return new WhenNode(head, body, next);
    }
       
    public Node newOptN(Node body) {
        return new OptNNode(body);
    }
       
    public Node newWhile(Node condition, Node body) {
        return new WhileNode(condition, body);
    }
       
    public Node newUntil(Node condition, Node body) {
        return new UntilNode(condition, body);
    }
       
    public Node newFor(Node var, Node iter, Node body) {
        return new ForNode(var, body, iter);
    }
       
    public Node newIter(Node var, Node iter, Node body) {
        return new IterNode(var, body, iter);
    }
       
    public Node newBreak() {
        return new BreakNode();
    }
       
    public Node newNext() {
        return new NextNode();
    }
       
    public Node newRedo() {
        return new RedoNode();
    }
       
    public Node newRetry() {
        return new RetryNode();
    }
       
    public Node newBegin(Node body) {
        return new BeginNode(body);
    }
       
    public Node newRescue(Node headNode /*b*/, Node rescueNode, Node elseNode) {
        return new RescueNode(headNode, rescueNode, elseNode);
    }
       
    public RescueBodyNode newResBody(Node args, Node body, Node head /*n*/) {
        return new RescueBodyNode(head, body, args);
    }
       
    public Node newEnsure(Node head /*b*/, Node ensure) {
        return new EnsureNode(head, ensure);
    }
       
    public Node newReturn(Node stts) {
        return new ReturnNode(stts);
    }
       
    public Node newYield(Node stts) {
        return new YieldNode(stts);
    }
       
    public ArrayNode newList(Node head) {
        return newArray(head);
    }
       
    public ArrayNode newArray(Node head) {
        return new ArrayNode(head, 1, null);
    }
       
    public ZArrayNode newZArray() {
        return new ZArrayNode();
    }
       
    public Node newHash(Node head)  {
        return new HashNode(head);
    }
       
    public Node newNot(Node body)   {
        return new NotNode(body);
    }
       
    public Node newMAsgn(Node valueNode, Node argsNode)   {
        return new MAsgnNode(valueNode, argsNode);
    }
       
    public Node newGAsgn(RubyId vid, Node valueNode) {
        return new GAsgnNode(/*vid, */ valueNode, RubyGlobalEntry.getGlobalEntry(vid));
    }
       
    public Node newLAsgn(RubyId vid, Node valueNode) {
        return new LAsgnNode(/*vid, */ valueNode, ph.local_cnt(vid));
    }

    public Node newDAsgn(RubyId vid, Node valueNode) {
        return new DAsgnNode(vid, valueNode);
    }

    public Node newDAsgnCurr(RubyId vid, Node valueNode) {
        return new DAsgnCurrNode(vid, valueNode);
    }

    public Node newIAsgn(RubyId vid, Node valueNode) {
        return new IAsgnNode(vid, valueNode);
    }

    public Node newCDecl(RubyId vid, Node valueNode) {
        return new CDeclNode(vid, valueNode);
    }

    public Node newCVAsgn(RubyId vid, Node valueNode) {
        return new CVAsgnNode(vid, valueNode);
    }
       
    public Node newCVDecl(RubyId vid, Node valueNode) {
        return new CVDeclNode(vid, valueNode);
    }
       
    public Node newOpAsgn1(Node recvNode, RubyId mid, Node argsNode) {
        return new OpAsgn1Node(recvNode, mid, argsNode);
    }
       
    public Node newOpAsgn2(Node recvNode, RubyId vid, RubyId mid, Node valueNode) {
        return new OpAsgn2Node(recvNode, valueNode, vid, mid);
    }
       
    /*public NODE newOpAsgn22(RubyId id, RubyId o) {
        return new OpAsgn22Node(id, o, id.toAttrSetId());
    }*/
       
    public Node newOpAsgnOr(Node head, Node valueNode) {
        return new OpAsgnOrNode(head, valueNode, null);
    }
       
    public Node newOpAsgnAnd(Node head, Node valueNode) {
        return new OpAsgnAndNode(head, valueNode);
    }
       
    public Node newGVar(RubyId id) {
        return new GVarNode(RubyGlobalEntry.getGlobalEntry(id));
    }
       
    public Node newLVar(RubyId vid) {
        return new LVarNode(/*v,*/ ph.local_cnt(vid));
    }
       
    public Node newDVar(RubyId vid) {
        return new DVarNode(vid);
    }
       
    public Node newIVar(RubyId vid) {
        return new IVarNode(vid);
    }
       
    public Node newConst(RubyId vid) {
        return new ConstNode(vid);
    }
       
    public Node newCVar(RubyId vid) {
        return new CVarNode(vid);
    }
       
    public Node newCVar2(RubyId vid) {
        return new CVar2Node(vid);
    }
       
    public Node newNthRef(int nth)  {
        return new NthRefNode(nth, ph.local_cnt('~'));
    }
       
    public Node newBackRef(int nth) {
        return new BackRefNode(nth, ph.local_cnt('~'));
    }
       
    public Node newMatch(Node head) {
        return new MatchNode(head);
    }
       
    public Node newMatch2(Node recv, Node value) {
        return new Match2Node(recv, value);
    }
       
    public Node newMatch3(Node recv, Node value) {
        return new Match3Node(recv, value);
    }
       
    public Node newLit(RubyObject lit) {
        return new LitNode(lit);
    }
       
    public Node newStr(RubyObject str) {
        return new StrNode(str);
    }
       
    public Node newDStr(RubyObject str) {
        return new DStrNode(str);
    }
       
    public Node newXStr(RubyObject str) {
        return new XStrNode(str);
    }
       
    public Node newDXStr(RubyObject str) {
        return new DXStrNode(str);
    }
       
    public Node newEVStr(String s, int len) {
        return new EvStrNode(RubyString.m_newString(ruby, s, len));
    }
       
    public Node newCall(Node recv, RubyId mid, Node args) {
        return new CallNode(recv, mid, args);
    }
    
    public Node newFCall(RubyId mid, Node args) {
        return new FCallNode(mid, args);
    }
    
    public Node newVCall(RubyId mid) {
        return new VCallNode(mid);
    }
    
    public Node newSuper(Node args) {
        return new SuperNode(args);
    }
    
    public Node newZSuper() {
        return new ZSuperNode();
    }
    
    public Node newArgs(Integer count, Node optNode, int rest) {
        return new ArgsNode(optNode, rest, count != null ? count.intValue() : 0);
    }
    
    public Node newArgsCat(Node head /*a*/, Node body) {
        return new ArgsCatNode(head, body);
    }
    
    public Node newArgsPush(Node head /*a*/, Node body) {
        return new ArgsPushNode(head, body);
    }
    
    public Node newRestArgs(Node head) {
        return new RestArgsNode(head);
    }
    
    public Node newBlockArg(RubyId vid) {
        return new BlockArgNode(ph.local_cnt(vid));
    }
    
    public Node newBlockPass(Node bodyNode) {
        return new BlockPassNode(bodyNode);
    }
    
    public Node newAlias(RubyId newId, RubyId oldId) {
        return new AliasNode(oldId, newId);
    }
    
    public Node newVAlias(RubyId newId, RubyId oldId) {
        return new VAliasNode(oldId, newId);
    }
    
    public Node newUndef(RubyId mid) {
        return new UndefNode(mid);
    }
    
    public Node newClass(RubyId cnameId, Node body, Node superNode) {
        return new ClassNode(cnameId, newScope(body), superNode);
    }
    
    public Node newSClass(Node receiverNode, Node body) {
        return new SClassNode(receiverNode, newScope(body));
    }
    
    public Node newModule(RubyId moduleName, Node body) {
        return new ModuleNode(moduleName, newScope(body));
    }

    public Node newColon2(Node head, RubyId mid) {
        return new Colon2Node(head, mid);
    }

    public Node newColon3(RubyId mid) {
        return new Colon3Node(mid);
    }

    /*public Node newCRef(Object c) {
        return new CRefNode(c);
    }*/

    public Node newDot2(Node begin, Node end) {
        return new DotNode(begin, end, false);
    }

    public Node newDot3(Node begin, Node end) {
        return new DotNode(begin, end, true);
    }

    public Node newAttrSet(RubyId vid /*a*/) {
        return new AttrSetNode(vid);
    }

    public Node newSelf() {
        return new SelfNode();
    }

    public Node newNil() {
        return new NilNode();
    }
    
    public Node newTrue() {
        return new TrueNode();
    }
    
    public Node newFalse() {
        return new FalseNode();
    }
    
    public Node newDefined(Node head /* e */) {
        return new DefinedNode(head);
    }
    
    public NewlineNode newNewline(Node nextNode) {
        return new NewlineNode(nextNode);
    }
    
    public Node newPreExe(Node body) {
        return newScope(body);
    }
    
    public Node newPostExe() {
        return new PostExeNode();
    }
    
    /*public Node newDMethod(Object b) {
        return new DMethodNode(b);
    }
    
    public Node newBMethod(Object b) {
        return new BMethodNode(b);
    }*/
}
