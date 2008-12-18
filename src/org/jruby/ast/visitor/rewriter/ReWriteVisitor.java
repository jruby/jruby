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
 * Copyright (C) 2006-2007 Mirko Stocker <me@misto.ch>
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

package org.jruby.ast.visitor.rewriter;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jruby.ast.*;
import org.jruby.ast.types.INameNode;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.ast.visitor.rewriter.utils.Operators;
import org.jruby.ast.visitor.rewriter.utils.ReWriterContext;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.parser.StaticScope;

/**
 * Visits each node and outputs the corresponding Ruby sourcecode for the nodes. 
 * 
 * @author Mirko Stocker
 * 
 */
public class ReWriteVisitor implements NodeVisitor {
	
	protected final ReWriterContext config;
	protected final ReWriterFactory factory;
	
	public ReWriteVisitor(Writer out, String source) {
		this(new ReWriterContext(new PrintWriter(out), source, new DefaultFormatHelper()));
	}

	public ReWriteVisitor(OutputStream out, String source) {
		this(new ReWriterContext(new PrintWriter(out, true), source, new DefaultFormatHelper()));
	}

	public ReWriteVisitor(ReWriterContext config) {
		this.config = config;
		factory = new ReWriterFactory(config);
	}

	public void flushStream() {
		config.getOutput().flush();
	}
	
	protected void print(String s) {
		config.getOutput().print(s);
	}
	
	protected void print(char c) {
		config.getOutput().print(c);
	}
	
	protected void print(BigInteger i) {
		config.getOutput().print(i);
	}
	
	protected void print(int i) {
		config.getOutput().print(i);
	}
	
	protected void print(long l) {
		config.getOutput().print(l);
	}
	
	protected void print(double d) {
		config.getOutput().print(d);
	}
	
	private void enterCall() {
		config.getCallDepth().enterCall();
	}

	private void leaveCall() {
		config.getCallDepth().leaveCall();
	}

	private boolean inCall() {
		return config.getCallDepth().inCall();
	}

	protected void printNewlineAndIndentation() {
		if (config.hasHereDocument()) config.fetchHereDocument().print();

		print(config.getFormatHelper().getLineDelimiter());
		config.getIndentor().printIndentation(config.getOutput());
	}

	private static boolean isReceiverACallNode(CallNode n) {
		return (n.getReceiverNode() instanceof CallNode || n.getReceiverNode() instanceof FCallNode);
	}

    private void printCommentsBefore(Node iVisited) {

        for (CommentNode n: iVisited.getComments()) {
			if (getStartLine(n) < getStartLine(iVisited)) {
				visitNode(n);
				print(n.getContent());
                printNewlineAndIndentation();
			}
		}
	}

	protected boolean printCommentsAfter(Node iVisited) {
		boolean hasComment = false;
        
        for (CommentNode n: iVisited.getComments()) {
			if(getStartLine(n) >= getEndLine(iVisited)) {
				print(' ');
				visitNode(n);
                print(n.getContent());
				hasComment = true;
			}
		}
        
		return hasComment;
	}
	
	public void visitNode(Node iVisited) {
            if (iVisited == null || iVisited.isInvisible()) return;
        
        printCommentsBefore(iVisited);

        if (iVisited instanceof ArgumentNode) {
            print(((ArgumentNode) iVisited).getName());
        } else {
            iVisited.accept(this);
        }

        printCommentsAfter(iVisited);
        config.setLastPosition(iVisited.getPosition());
	}

	public void visitIter(Iterator iterator) {
		while (iterator.hasNext()) {
			visitNode((Node) iterator.next());
		}
	}

	private void visitIterAndSkipFirst(Iterator iterator) {
		iterator.next();
		visitIter(iterator);
	}

	private static boolean isStartOnNewLine(Node first, Node second) {
		if (first == null || second == null) return false;
        
		return (getStartLine(first) < getStartLine(second));
	}

	private boolean needsParentheses(Node n) {
        return (n != null && (n.childNodes().size() > 1 || inCall() || firstChild(n) instanceof HashNode)
				|| firstChild(n) instanceof NewlineNode || firstChild(n) instanceof IfNode);
	}

	private void printCallArguments(Node argsNode, Node iterNode) {
        if (argsNode != null && argsNode.childNodes().size() < 1 && iterNode == null) return;
                    
        if (argsNode != null && argsNode.childNodes().size() == 1 && 
                firstChild(argsNode) instanceof HashNode && iterNode == null) {
			HashNode hashNode = (HashNode) firstChild(argsNode);
			if(hashNode.getListNode().childNodes().size() < 1) {
				print("({})");
			} else {
				print(' ');
				printHashNodeContent(hashNode);
			}
			return;
		}

		boolean paranthesesPrinted = needsParentheses(argsNode)
            || (argsNode == null && iterNode != null && iterNode instanceof BlockPassNode)
            || (argsNode != null && argsNode.childNodes().size() > 0 && iterNode != null);
        
		if (paranthesesPrinted) {
			print('(');
        } else if (argsNode != null) { 
			print(config.getFormatHelper().beforeCallArguments());
        }

		if (firstChild(argsNode) instanceof NewlineNode) {
			config.setSkipNextNewline(true);
        }

		enterCall();

		if (argsNode instanceof SplatNode) {
			visitNode(argsNode);
        } else if (argsNode != null) {
			visitAndPrintWithSeparator(argsNode.childNodes().iterator());
        }

        if (iterNode instanceof BlockPassNode) {
            if (argsNode != null) print(config.getFormatHelper().getListSeparator());
            
            print('&');
            visitNode(((BlockPassNode) iterNode).getBodyNode());
        }

		if (paranthesesPrinted) {
			print(')');
        } else {
			print(config.getFormatHelper().afterCallArguments());
        }

		leaveCall();
	}

	public void visitAndPrintWithSeparator(Iterator<Node> it) {
		while (it.hasNext()) {
			Node n = it.next();
			factory.createIgnoreCommentsReWriteVisitor().visitNode(n);
			if (it.hasNext())
				print(config.getFormatHelper().getListSeparator());
			if(n.hasComments()) {
				factory.createReWriteVisitor().visitIter(n.getComments().iterator());
				printNewlineAndIndentation();
			}
		}
	}

	public Object visitAliasNode(AliasNode iVisited) {
		print("alias ");
		print(iVisited.getNewName());
		print(' ');
		print(iVisited.getOldName());
		printCommentsAtEnd(iVisited);
		return null;
	}

	private boolean sourceRangeEquals(int start, int stop, String compare) {
		return (stop <= config.getSource().length() && sourceSubStringEquals(start, stop - start, compare));
	}
	
	private boolean sourceRangeContains(ISourcePosition pos, String searched) {
		return pos.getStartOffset() < config.getSource().length() 
			&& pos.getEndOffset() < config.getSource().length() + 1
			&& config.getSource().substring(pos.getStartOffset(), pos.getEndOffset()).indexOf(searched) > -1;
	}
	public Object visitAndNode(AndNode iVisited) {
		enterCall();
		visitNode(iVisited.getFirstNode());
        
		if (sourceRangeContains(iVisited.getPosition(), "&&")) {
			print(" && ");
        } else {
			print(" and ");
        }
		visitNode(iVisited.getSecondNode());
		leaveCall();
		return null;
	}


	private ArrayList<Node> collectAllArguments(ArgsNode iVisited) {
		ArrayList<Node> arguments = new ArrayList<Node>();
        
		if (iVisited.getPre() != null) arguments.addAll(iVisited.getPre().childNodes());
        if (iVisited.getOptArgs() != null) arguments.addAll(iVisited.getOptArgs().childNodes());
        if (iVisited.getRestArgNode() != null) {
            arguments.add(new ConstNode(iVisited.getRestArgNode().getPosition(), '*' + iVisited.getRestArgNode().getName()));
        }
        if (iVisited.getPost() != null) arguments.addAll(iVisited.getPost().childNodes());
		if (iVisited.getBlock() != null) arguments.add(iVisited.getBlock());
        
		return arguments;
	}
	
	private boolean hasNodeCommentsAtEnd(Node n) {
	    for (Node comment: n.getComments()) {
			if (getStartLine(comment) == getStartLine(n)) return true;
		}
        
		return false;
	}
	
	private void printCommentsInArgs(Node n, boolean hasNext) {
		if (hasNodeCommentsAtEnd(n) && hasNext) print(",");
        
		if (printCommentsAfter(n) && hasNext) {
			printNewlineAndIndentation();
		} else if (hasNext) {
			print(config.getFormatHelper().getListSeparator());
        }
	}
	
	public Object visitArgsNode(ArgsNode iVisited) {

		for (Iterator<Node> it = collectAllArguments(iVisited).iterator(); it.hasNext(); ) {
			Node n = it.next();
            
			if (n instanceof ArgumentNode) {
				print(((ArgumentNode) n).getName());
				printCommentsInArgs(n, it.hasNext());
			} else {
				visitNode(n);
				if (it.hasNext()) print(config.getFormatHelper().getListSeparator());
			}
            
			if (!it.hasNext()) print(config.getFormatHelper().afterMethodArguments());
		}
			
		return null;
	}

	public Object visitArgsCatNode(ArgsCatNode iVisited) {
		print("[");
		visitAndPrintWithSeparator(iVisited.getFirstNode().childNodes().iterator());
		print(config.getFormatHelper().getListSeparator());
		print("*");
		visitNode(iVisited.getSecondNode());
		print("]");
		return null;
	}

	public Object visitArrayNode(ArrayNode iVisited) {
		print('[');
		enterCall();
		visitAndPrintWithSeparator(iVisited.childNodes().iterator());
		leaveCall();
		print(']');
		return null;
	}

	public Object visitBackRefNode(BackRefNode iVisited) {
		print('$');
		print(iVisited.getType());
		return null;
	}

	public Object visitBeginNode(BeginNode iVisited) {
		print("begin");
		visitNodeInIndentation(iVisited.getBodyNode());
		printNewlineAndIndentation();
		print("end");
		return null;
	}

	public Object visitBignumNode(BignumNode iVisited) {
		print(iVisited.getValue());
		return null;
	}

	public Object visitBlockArgNode(BlockArgNode iVisited) {
		print('&');
		print(iVisited.getName());
		return null;
	}

	public Object visitBlockNode(BlockNode iVisited) {
		visitIter(iVisited.childNodes().iterator());
		return null;
	}

	public static int getLocalVarIndex(Node n) {
		return n instanceof LocalVarNode ? ((LocalVarNode) n).getIndex() : -1; 
	}

	public Object visitBlockPassNode(BlockPassNode iVisited) {
	    visitNode(iVisited.getBodyNode());
		return null;
	}

	public Object visitBreakNode(BreakNode iVisited) {
		print("break");
		return null;
	}

	public Object visitConstDeclNode(ConstDeclNode iVisited) {
		printAsgnNode(iVisited);
		return null;
	}

	public Object visitClassVarAsgnNode(ClassVarAsgnNode iVisited) {
		printAsgnNode(iVisited);
		return null;
	}

	public Object visitClassVarDeclNode(ClassVarDeclNode iVisited) {
		printAsgnNode(iVisited);
		return null;
	}

	public Object visitClassVarNode(ClassVarNode iVisited) {
		print(iVisited.getName());
		return null;
	}

	private boolean isNumericNode(Node n) {
		return (n != null && (n instanceof FixnumNode || n instanceof BignumNode));
	}

	private boolean isNameAnOperator(String name) {
		return Operators.contain(name);
	}

	private boolean printSpaceInsteadOfDot(CallNode n) {
		return (isNameAnOperator(n.getName()) && !(n.getArgsNode().childNodes().size() > 1));
	}
	
	protected void printAssignmentOperator(){
		print(config.getFormatHelper().beforeAssignment());
		print("=");
		print(config.getFormatHelper().afterAssignment());
	}

	private Object printIndexAssignment(AttrAssignNode iVisited) {
		enterCall();
		visitNode(iVisited.getReceiverNode());
		leaveCall();
		print('[');
		visitNode(firstChild(iVisited.getArgsNode()));
		print("]");
		printAssignmentOperator();
		if (iVisited.getArgsNode().childNodes().size() > 1)
			visitNode((Node) iVisited.getArgsNode().childNodes().get(1));
		return null;
	}

	private Object printIndexAccess(CallNode visited) {
		enterCall();
		visitNode(visited.getReceiverNode());
		leaveCall();
		print('[');
		if (visited.getArgsNode() != null) {
			visitAndPrintWithSeparator(visited.getArgsNode().childNodes().iterator());
        }
		print("]");
		return null;
	}
	
	private Object printNegativNumericNode(CallNode visited) {
		print('-');
		visitNode(visited.getReceiverNode());
		return null;
	}
	
	private boolean isNegativeNumericNode(CallNode visited) {
		return isNumericNode(visited.getReceiverNode()) && visited.getName().equals("-@");
	}
	
	private void printCallReceiverNode(CallNode iVisited) {
		if (iVisited.getReceiverNode() instanceof HashNode) print('(');

		if (isReceiverACallNode(iVisited) && !printSpaceInsteadOfDot(iVisited)) {
			enterCall();
			visitNewlineInParentheses(iVisited.getReceiverNode());
			leaveCall();
		} else {
			visitNewlineInParentheses(iVisited.getReceiverNode());
        }

		if (iVisited.getReceiverNode() instanceof HashNode) print(')');
	}
	
	protected boolean inMultipleAssignment() {
		return false;
	}

	public Object visitCallNode(CallNode iVisited) {
		if (isNegativeNumericNode(iVisited)) return printNegativNumericNode(iVisited);
        
		if (iVisited.getName().equals("[]")) return printIndexAccess(iVisited);
		 
		printCallReceiverNode(iVisited);

		print(printSpaceInsteadOfDot(iVisited) ? ' ' : '.');

		if (inMultipleAssignment() && iVisited.getName().endsWith("=")) {
			print(iVisited.getName().substring(0, iVisited.getName().length() - 1));
        } else {
			print(iVisited.getName());
        }

		if (isNameAnOperator(iVisited.getName())) {
			if (firstChild(iVisited.getArgsNode()) instanceof NewlineNode) print(' ');
            
			config.getCallDepth().disableCallDepth();
		}
		printCallArguments(iVisited.getArgsNode(), iVisited.getIterNode());

		if (isNameAnOperator(iVisited.getName())) config.getCallDepth().enableCallDepth();
        if (!(iVisited.getIterNode() instanceof BlockPassNode)) visitNode(iVisited.getIterNode());

		return null;
	}

	public Object visitCaseNode(CaseNode iVisited) {
		print("case ");
		visitNode(iVisited.getCaseNode());
		visitNode(iVisited.getFirstWhenNode());
		printNewlineAndIndentation();
		print("end");
		return null;
	}
        
	private boolean printCommentsIn(Node iVisited) {
		boolean hadComment = false;
		for (CommentNode n: iVisited.getComments()) {
			if(getStartLine(n) > getStartLine(iVisited) && getEndLine(n) < getEndLine(iVisited)) {
				hadComment = true;
				visitNode(n);
                print(n.getContent());
				printNewlineAndIndentation();
			}
		}
		
		return hadComment;
	}

	public Object visitClassNode(ClassNode iVisited) {

		print("class ");
		visitNode(iVisited.getCPath());
		if (iVisited.getSuperNode() != null) {
			print(" < ");
			visitNode(iVisited.getSuperNode());
		}

		new ClassBodyWriter(this, iVisited.getBodyNode()).write();
		
		printNewlineAndIndentation();		
		printCommentsIn(iVisited);
		
		print("end");
		return null;
	}

	public Object visitColon2Node(Colon2Node iVisited) {
		if (iVisited.getLeftNode() != null) { 
			visitNode(iVisited.getLeftNode());
			print("::");
		}
		print(iVisited.getName());
		return null;
	}

	public Object visitColon3Node(Colon3Node iVisited) {
		print("::");
		print(iVisited.getName());
		return null;
	}

	public Object visitConstNode(ConstNode iVisited) {
		print(iVisited.getName());
		return null;
	}

	public Object visitDAsgnNode(DAsgnNode iVisited) {
		printAsgnNode(iVisited);
		return null;
	}

	public Object visitDRegxNode(DRegexpNode iVisited) {
		config.getPrintQuotesInString().set(false);
		print(getFirstRegexpEnclosure(iVisited));
		factory.createDRegxReWriteVisitor().visitIter(iVisited.childNodes().iterator());
		print(getSecondRegexpEnclosure(iVisited));
		printRegexpOptions(iVisited.getOptions());
		config.getPrintQuotesInString().revert();
		return null;
	}
	
	private Object createHereDocument(DStrNode iVisited) {
		config.getPrintQuotesInString().set(false);
		print("<<-EOF");
		StringWriter writer = new StringWriter();
		PrintWriter oldOut = config.getOutput();
		config.setOutput(new PrintWriter(writer));

		for (Iterator it = iVisited.childNodes().iterator(); it.hasNext(); ) {
			factory.createHereDocReWriteVisitor().visitNode((Node) it.next());
            
			if (it.hasNext()) config.setSkipNextNewline(true);
		}
        
		config.setOutput(oldOut);
		config.depositHereDocument(writer.getBuffer().toString());
		config.getPrintQuotesInString().revert();

		return null;
	}

	public Object visitDStrNode(DStrNode iVisited) {

		if (firstChild(iVisited) instanceof StrNode && stringIsHereDocument((StrNode) firstChild(iVisited))) {
			return createHereDocument(iVisited);
		}

		if (config.getPrintQuotesInString().isTrue()) print(getSeparatorForStr(iVisited));
        
		config.getPrintQuotesInString().set(false);
		leaveCall();
		for (Node child: iVisited.childNodes()) {
            visitNode(child);
		}
		enterCall();
		config.getPrintQuotesInString().revert();
        
		if (config.getPrintQuotesInString().isTrue()) print(getSeparatorForStr(iVisited));
        
		return null;
	}

	public Object visitDSymbolNode(DSymbolNode iVisited) {
		print(':');
        if (config.getPrintQuotesInString().isTrue()) print(getSeparatorForSym(iVisited));
        
        config.getPrintQuotesInString().set(false);
        leaveCall();
        for (Node child: iVisited.childNodes()) {
            visitNode(child);
        }        
        enterCall();
        config.getPrintQuotesInString().revert();
        
        if (config.getPrintQuotesInString().isTrue()) print(getSeparatorForSym(iVisited));
		return null;
	}

	public Object visitDVarNode(DVarNode iVisited) {
		print(iVisited.getName());
		return null;
	}

	public Object visitDXStrNode(DXStrNode iVisited) {
		config.getPrintQuotesInString().set(false);
		print("%x{");
		visitIter(iVisited.childNodes().iterator());
		print('}');
		config.getPrintQuotesInString().revert();
		return null;
	}

	public Object visitDefinedNode(DefinedNode iVisited) {
		print("defined? ");
		enterCall();
		visitNode(iVisited.getExpressionNode());
		leaveCall();
		return null;
	}

	private boolean hasArguments(Node n) {
		if (n instanceof ArgsNode) {
			ArgsNode args = (ArgsNode) n;
			return (args.getPre() != null || args.getOptArgs() != null
					|| args.getBlock() != null || args.getRestArgNode() != null);
		} else if (n instanceof ArrayNode && n.childNodes().isEmpty()) {
			return false;
		}
		return true;
	}
        
	protected void printCommentsAtEnd(Node n) {
		for (CommentNode comment: n.getComments()) {
			if (getStartLine(n) == getStartLine(comment)) {
				print(' ');
				visitNode(comment);
                print(comment.getContent());
			}
		}
	}
	
	private void printDefNode(Node parent, String name, Node args, StaticScope scope, Node bodyNode) {
		print(name);
		config.getLocalVariables().addLocalVariable(scope);

		if (hasArguments(args)) {
		    print(config.getFormatHelper().beforeMethodArguments());
            visitNode(args);
		}
        printCommentsAtEnd(parent);
        
		visitNode(bodyNode);
		config.getIndentor().outdent();
		printNewlineAndIndentation();
		printCommentsIn(parent);
		print("end");
	}

	public Object visitDefnNode(DefnNode iVisited) {
		config.getIndentor().indent();
		print("def ");
		printDefNode(iVisited, iVisited.getName(), iVisited.getArgsNode(), iVisited.getScope(), iVisited.getBodyNode());
		return null;
	}

	public Object visitDefsNode(DefsNode iVisited) {
		config.getIndentor().indent();
		print("def ");
		visitNode(iVisited.getReceiverNode());
		print('.');
		printDefNode(iVisited, iVisited.getName(), iVisited.getArgsNode(), iVisited.getScope(), iVisited.getBodyNode());
		return null;
	}

	public Object visitDotNode(DotNode iVisited) {
		enterCall();
		visitNode(iVisited.getBeginNode());
		print("..");
		if (iVisited.isExclusive()) print('.');
		visitNode(iVisited.getEndNode());
		leaveCall();
		return null;
	}

	public Object visitEnsureNode(EnsureNode iVisited) {
		visitNode(iVisited.getBodyNode());
		config.getIndentor().outdent();
		printNewlineAndIndentation();
		print("ensure");
		visitNodeInIndentation(iVisited.getEnsureNode());
		config.getIndentor().indent();
		return null;
	}

	public Object visitEvStrNode(EvStrNode iVisited) {
		print('#');
		if (!(iVisited.getBody() instanceof NthRefNode)) print('{');
		config.getPrintQuotesInString().set(true);
		visitNode(unwrapNewlineNode(iVisited.getBody()));
		config.getPrintQuotesInString().revert();
		if (!(iVisited.getBody() instanceof NthRefNode)) print('}');
		return null;
	}
	
	private Node unwrapNewlineNode(Node node) {
		return node instanceof NewlineNode ? ((NewlineNode) node).getNextNode() : node; 
	}

	public Object visitFCallNode(FCallNode iVisited) {
		print(iVisited.getName());
        
        if (iVisited.getIterNode() != null) config.getCallDepth().enterCall();
        
		printCallArguments(iVisited.getArgsNode(), iVisited.getIterNode());
        
        if (iVisited.getIterNode() != null) config.getCallDepth().leaveCall();

        if (!(iVisited.getIterNode() instanceof BlockPassNode)) visitNode(iVisited.getIterNode());

		return null;
	}

	public Object visitFalseNode(FalseNode iVisited) {
		print("false");
		return null;
	}

	public Object visitFixnumNode(FixnumNode iVisited) {
		print(iVisited.getValue());
		return null;
	}

	public Object visitFlipNode(FlipNode iVisited) {
		enterCall();
		visitNode(iVisited.getBeginNode());
		print(" ..");
		if (iVisited.isExclusive())	print('.');
		print(' ');
		visitNode(iVisited.getEndNode());
		leaveCall();
		return null;
	}

	public Object visitFloatNode(FloatNode iVisited) {
		print(iVisited.getValue());
		return null;
	}

	public Object visitForNode(ForNode iVisited) {
		print("for ");
		visitNode(iVisited.getVarNode());
		print(" in ");
		visitNode(iVisited.getIterNode());
		visitNodeInIndentation(iVisited.getBodyNode());
		printNewlineAndIndentation();
		print("end");
		return null;
	}

	public Object visitGlobalAsgnNode(GlobalAsgnNode iVisited) {
		printAsgnNode(iVisited);
		return null;
	}

	public Object visitGlobalVarNode(GlobalVarNode iVisited) {
		print(iVisited.getName());
		return null;
	}

	private void printHashNodeContent(HashNode iVisited) {
		print(config.getFormatHelper().beforeHashContent());
		if (iVisited.getListNode() != null) {
			for (Iterator it = iVisited.getListNode().childNodes().iterator(); it.hasNext(); ) {
				visitNode((Node) it.next());
				print(config.getFormatHelper().hashAssignment());
				visitNode((Node) it.next());
                
				if (it.hasNext()) print(config.getFormatHelper().getListSeparator());
			}
		}
		print(config.getFormatHelper().afterHashContent());
	}

	public Object visitHashNode(HashNode iVisited) {
		print('{');
		printHashNodeContent(iVisited);
		print('}');
		return null;
	}

	private void printAsgnNode(AssignableNode n) {
		print(((INameNode) n).getName());
		if (n.getValueNode() == null || n.getValueNode().isInvisible()) return;
		printAssignmentOperator();
		visitNewlineInParentheses(n.getValueNode());
	}

	public Object visitInstAsgnNode(InstAsgnNode iVisited) {
		printAsgnNode(iVisited);
		return null;
	}

	public Object visitInstVarNode(InstVarNode iVisited) {
		print(iVisited.getName());
		return null;
	}

	/**
	 * Elsif-conditions in the AST are represented by multiple nested if / else
	 * combinations. This method takes a node and checks if the node is an
	 * elsif-statement or a normal else node.
	 * 
	 * @param iVisited
	 * @return Returns the last ElseNode or null.
	 */
	private Node printElsIfNodes(Node iVisited) {
		if (iVisited != null && iVisited instanceof IfNode) {
			IfNode n = (IfNode) iVisited;
			printNewlineAndIndentation();
			print("elsif ");
			visitNode(n.getCondition());
			visitNodeInIndentation(n.getThenBody());
			return printElsIfNodes(n.getElseBody());
		} 
			
        return iVisited != null ? iVisited : null;
	}

	private Object printShortIfStatement(IfNode n) {
		if (n.getThenBody() == null) {
			visitNode(n.getElseBody());
			print(" unless ");
			visitNode(n.getCondition());
		} else {
			enterCall();
			factory.createShortIfNodeReWriteVisitor().visitNode(n.getCondition());
			print(" ? ");
			factory.createShortIfNodeReWriteVisitor().visitNode(n.getThenBody());
			print(" : ");
			factory.createShortIfNodeReWriteVisitor().visitNewlineInParentheses(n.getElseBody());
			leaveCall();
		}
		return null;
	}

	private boolean isAssignment(Node n) {
		return (n instanceof DAsgnNode || n instanceof GlobalAsgnNode
				|| n instanceof InstAsgnNode || n instanceof LocalAsgnNode || n instanceof ClassVarAsgnNode);
	}

	private boolean sourceSubStringEquals(int offset, int length, String str) {
		return config.getSource().length() >= offset + length
			&& config.getSource().substring(offset, offset + length).equals(str);
	}
	
	private boolean isShortIfStatement(IfNode iVisited) {
		return (isOnSingleLine(iVisited.getCondition(), iVisited.getElseBody())
				&& !(iVisited.getElseBody() instanceof IfNode)
				&& !sourceSubStringEquals(getStartOffset(iVisited), 2, "if"));
	}

	public Object visitIfNode(IfNode iVisited) {

		if (isShortIfStatement(iVisited)) return printShortIfStatement(iVisited);

		print("if ");

		if (isAssignment(iVisited.getCondition())) enterCall();

		// We have to skip a possible Newline here:
		visitNewlineInParentheses(iVisited.getCondition());
        
		if (isAssignment(iVisited.getCondition())) leaveCall();

		config.getIndentor().indent();
		// we have to check this to generate valid code for this style: "return
		// if true", because there is no newline
		if (!isStartOnNewLine(iVisited.getCondition(), iVisited.getThenBody()) && iVisited.getThenBody() != null) {
			printNewlineAndIndentation();
        }

		visitNode(iVisited.getThenBody());
		config.getIndentor().outdent();
		Node elseNode = printElsIfNodes(iVisited.getElseBody());

		if (elseNode != null) {
			printNewlineAndIndentation();
			print("else");
			config.getIndentor().indent();
			visitNode(elseNode);
			config.getIndentor().outdent();
		}
		printNewlineAndIndentation();
		print("end");
		return null;
	}

	private boolean isOnSingleLine(Node n) {
		return isOnSingleLine(n, n);
	}

	private boolean isOnSingleLine(Node n1, Node n2) {
		if (n1 == null || n2 == null) return false;
        
		return (getStartLine(n1) == getEndLine(n2));
	}

	private boolean printIterVarNode(IterNode n) {
		if (n.getVarNode() == null) return false;
        
        print('|');
        visitNode(n.getVarNode());
        print('|');
        
        return true;
	}

	public Object visitIterNode(IterNode iVisited) {
		if (isOnSingleLine(iVisited)) {
			print(config.getFormatHelper().beforeIterBrackets());
			print("{");
			print(config.getFormatHelper().beforeIterVars());
			if(printIterVarNode(iVisited)) print(config.getFormatHelper().afterIterVars());
			config.setSkipNextNewline(true);
			visitNode(iVisited.getBodyNode());
			print(config.getFormatHelper().beforeClosingIterBrackets());
			print('}');
		} else {
			print(" do ");
			printIterVarNode(iVisited);
			visitNodeInIndentation(iVisited.getBodyNode());
			printNewlineAndIndentation();
			print("end");
		}
		return null;
	}

	public Object visitLocalAsgnNode(LocalAsgnNode iVisited) {
		config.getLocalVariables().addLocalVariable(iVisited.getIndex(), iVisited.getName());
		printAsgnNode(iVisited);
		return null;
	}

	public Object visitLocalVarNode(LocalVarNode iVisited) {
		print(iVisited.getName());
		return null;
	}

	public Object visitMultipleAsgnNode(MultipleAsgnNode iVisited) {
		if (iVisited.getHeadNode() != null) {
			factory.createMultipleAssignmentReWriteVisitor().visitAndPrintWithSeparator(iVisited.getHeadNode().childNodes().iterator());
        }
		if (iVisited.getValueNode() == null || iVisited.getValueNode().isInvisible()) {
			visitNode(iVisited.getArgsNode());
			return null;
		}
		print(config.getFormatHelper().beforeAssignment());
		print("=");
		print(config.getFormatHelper().afterAssignment());
		enterCall();
		if (iVisited.getValueNode() instanceof ArrayNode) {
			visitAndPrintWithSeparator(iVisited.getValueNode().childNodes().iterator());
        } else {
			visitNode(iVisited.getValueNode());
        }
		leaveCall();
		return null;
	}

	public Object visitMultipleAsgnNode(MultipleAsgn19Node iVisited) {
		if (iVisited.getPre() != null) {
			factory.createMultipleAssignmentReWriteVisitor().visitAndPrintWithSeparator(iVisited.getPre().childNodes().iterator());
        }
		if (iVisited.getValueNode() == null || iVisited.getValueNode().isInvisible()) {
			visitNode(iVisited.getRest());
			return null;
		}
		print(config.getFormatHelper().beforeAssignment());
		print("=");
		print(config.getFormatHelper().afterAssignment());
		enterCall();
		if (iVisited.getValueNode() instanceof ArrayNode) {
			visitAndPrintWithSeparator(iVisited.getValueNode().childNodes().iterator());
        } else {
			visitNode(iVisited.getValueNode());
        }
		leaveCall();
		return null;
	}

	public Object visitMatch2Node(Match2Node iVisited) {
		visitNode(iVisited.getReceiverNode());
		print(config.getFormatHelper().matchOperator());
		enterCall();
		visitNode(iVisited.getValueNode());
		leaveCall();
		return null;
	}

	public Object visitMatch3Node(Match3Node iVisited) {
		visitNode(iVisited.getValueNode());
		print(config.getFormatHelper().matchOperator());
		visitNode(iVisited.getReceiverNode());
		return null;
	}

	public Object visitMatchNode(MatchNode iVisited) {
		visitNode(iVisited.getRegexpNode());
		return null;
	}

	public Object visitModuleNode(ModuleNode iVisited) {
		print("module ");
		config.getIndentor().indent();
		visitNode(iVisited.getCPath());
		visitNode(iVisited.getBodyNode());
		config.getIndentor().outdent();
		printNewlineAndIndentation();
		print("end");
		return null;
	}
	
	public Object visitNewlineNode(NewlineNode iVisited) {
		if (config.isSkipNextNewline()) {
			config.setSkipNextNewline(false);
		} else {
			printNewlineAndIndentation();
		}
		visitNode(iVisited.getNextNode());
		return null;
	}

	public Object visitNextNode(NextNode iVisited) {
		print("next");
		return null;
	}

	public Object visitNilNode(NilNode iVisited) {
		print("nil");
		return null;
	}

	public Object visitNotNode(NotNode iVisited) {
		if (iVisited.getConditionNode() instanceof CallNode) enterCall();
        
		print(sourceRangeContains(iVisited.getPosition(), "not") ? "not " : "!"); 
		visitNewlineInParentheses(iVisited.getConditionNode());

		if (iVisited.getConditionNode() instanceof CallNode) leaveCall();
        
		return null;
	}

	public Object visitNthRefNode(NthRefNode iVisited) {
		print('$');
		print(iVisited.getMatchNumber());
		return null;
	}

	private boolean isSimpleNode(Node n) {
		return (n instanceof LocalVarNode || n instanceof AssignableNode
				|| n instanceof InstVarNode || n instanceof ClassVarNode
				|| n instanceof GlobalVarNode || n instanceof ConstDeclNode
				|| n instanceof VCallNode || isNumericNode(n));
	}

	public Object visitOpElementAsgnNode(OpElementAsgnNode iVisited) {

		if (!isSimpleNode(iVisited.getReceiverNode())) {
			visitNewlineInParentheses(iVisited.getReceiverNode());
        } else {
			visitNode(iVisited.getReceiverNode());
        }

		visitNode(iVisited.getArgsNode());
		print(' ');
		print(iVisited.getOperatorName());
		print("=");
		print(config.getFormatHelper().afterAssignment());
		visitNode(iVisited.getValueNode());
		return null;
	}

	public Object visitOpAsgnNode(OpAsgnNode iVisited) {
		visitNode(iVisited.getReceiverNode());
		print('.');
		print(iVisited.getVariableName());
		print(' ');
		print(iVisited.getOperatorName());
		print("=");
		print(config.getFormatHelper().afterAssignment());
		visitNode(iVisited.getValueNode());
		return null;
	}

	private void printOpAsgnNode(Node n, String operator) {
		enterCall();
		
		print(((INameNode) n).getName());
		print(config.getFormatHelper().beforeAssignment());
		print(operator);
		print(config.getFormatHelper().afterAssignment());
		visitNode(((AssignableNode) n).getValueNode());
			
		leaveCall();
	}

	public Object visitOpAsgnAndNode(OpAsgnAndNode iVisited) {
		printOpAsgnNode(iVisited.getSecondNode(), "&&=");
		return null;
	}

	public Object visitOpAsgnOrNode(OpAsgnOrNode iVisited) {
		printOpAsgnNode(iVisited.getSecondNode(), "||=");
		return null;
	}

	public Object visitOrNode(OrNode iVisited) {
		enterCall();
		visitNode(iVisited.getFirstNode());
		leaveCall();
        
		print(sourceRangeContains(iVisited.getPosition(), "||") ? " || " : " or ");
        
		enterCall();
		visitNewlineInParentheses(iVisited.getSecondNode());
		leaveCall();
        
		return null;
	}

	public Object visitPostExeNode(PostExeNode iVisited) {
		// this node contains nothing but an empty list, so we don't have to
		// process anything
		return null;
	}

    public Object visitPreExeNode(PreExeNode iVisited) {
        // this node contains nothing but an empty list, so we don't have to
        // process anything
        return null;
    }

    public Object visitRedoNode(RedoNode iVisited) {
		print("redo");
		return null;
	}

	private String getFirstRegexpEnclosure(Node n) {
		return isSpecialRegexNotation(n) ? "%r(" : "/";
	}

	private String getSecondRegexpEnclosure(Node n) {
		return isSpecialRegexNotation(n) ? ")" : "/";
	}

	private boolean isSpecialRegexNotation(Node n) {
		return getStartOffset(n) >= 2
			&& !(config.getSource().length() < getStartOffset(n))
			&& config.getSource().charAt(getStartOffset(n) - 3) == '%';
	}

	private void printRegexpOptions(int option) {
		if ((option & 1) == 1) print('i');
		if ((option & 2) == 2) print('x');
		if ((option & 4) == 4) print('m');
	}

	public Object visitRegexpNode(RegexpNode iVisited) {
		print(getFirstRegexpEnclosure(iVisited));
		print(iVisited.getValue().toString());
		print(getSecondRegexpEnclosure(iVisited));
		printRegexpOptions(iVisited.getOptions());
		return null;
	}

	public static Node firstChild(Node n) {
		if (n == null || n.childNodes().size() <= 0) return null;
        
		return (Node) n.childNodes().get(0);
	}

	public Object visitRescueBodyNode(RescueBodyNode iVisited) {
		if (!iVisited.getBodyNode().isInvisible() && config.getLastPosition().getStartLine() == getEndLine(iVisited.getBodyNode())) {
			print(" rescue ");
		} else {
			print("rescue");
		}

		if (iVisited.getExceptionNodes() != null) {
			printExceptionNode(iVisited);
		} else {
			visitNodeInIndentation(iVisited.getBodyNode());
		}
        
		if (iVisited.getOptRescueNode() != null) printNewlineAndIndentation();
        
		visitNode(iVisited.getOptRescueNode());
		return null;
	}

	private void printExceptionNode(RescueBodyNode n) {
		if (n.getExceptionNodes() == null) return;

		print(' ');
		visitNode(firstChild(n.getExceptionNodes()));

		Node firstBodyNode = n.getBodyNode();
		if (n.getBodyNode() instanceof BlockNode) firstBodyNode = firstChild(n.getBodyNode());

		// if the exception is assigned to a variable, we have to skip the first
		// node in the body
		if (firstBodyNode instanceof AssignableNode) {
			print(config.getFormatHelper().beforeAssignment());
			print("=>");
			print(config.getFormatHelper().afterAssignment());
			print(((INameNode) firstBodyNode).getName());
			if (firstBodyNode instanceof LocalAsgnNode)
				config.getLocalVariables().addLocalVariable(((LocalAsgnNode) firstBodyNode).getIndex(),
						((LocalAsgnNode) firstBodyNode).getName());

			config.getIndentor().indent();
			visitIterAndSkipFirst(n.getBodyNode().childNodes().iterator());
			config.getIndentor().outdent();
		} else {
			visitNodeInIndentation(n.getBodyNode());
        }
	}

	public Object visitRescueNode(RescueNode iVisited) {
		visitNode(iVisited.getBodyNode());
		config.getIndentor().outdent();

		if (!iVisited.getRescueNode().getBodyNode().isInvisible()
				&& getStartLine(iVisited) != getEndLine(iVisited.getRescueNode().getBodyNode())) {
			printNewlineAndIndentation();
        }

		if (iVisited.getRescueNode().getBodyNode().isInvisible()) {
			printNewlineAndIndentation();
			print("rescue");
			printExceptionNode(iVisited.getRescueNode());
		} else {
			visitNode(iVisited.getRescueNode());
		}
        
		if (iVisited.getElseNode() != null) {
			printNewlineAndIndentation();
			print("else");
			visitNodeInIndentation(iVisited.getElseNode());
		}
        
		config.getIndentor().indent();
		return null;
	}

	public Object visitRetryNode(RetryNode iVisited) {
		print("retry");
		return null;
	}

	public static Node unwrapSingleArrayNode(Node n) {
		if (!(n instanceof ArrayNode)) return n;
		if (((ArrayNode) n).childNodes().size() > 1) return n;
        
		return firstChild((ArrayNode) n);
	}

	public Object visitReturnNode(ReturnNode iVisited) {
		print("return");
		enterCall();
		if (!iVisited.getValueNode().isInvisible()) {
			print(' ');
			visitNode(unwrapSingleArrayNode(iVisited.getValueNode()));
		}
		leaveCall();
		return null;
	}

	public Object visitSClassNode(SClassNode iVisited) {
		print("class << ");
		config.getIndentor().indent();
		visitNode(iVisited.getReceiverNode());
		visitNode(iVisited.getBodyNode());
		config.getIndentor().outdent();
		printNewlineAndIndentation();
		print("end");
		return null;
	}
	
	public Object visitSelfNode(SelfNode iVisited) {
		print("self");
		return null;
	}

	public Object visitSplatNode(SplatNode iVisited) {
		print("*");
		visitNode(iVisited.getValue());
		return null;
	}

	private boolean stringIsHereDocument(StrNode n) {
		return sourceRangeEquals(getStartOffset(n) + 1, getStartOffset(n) + 3, "<<") || 
            sourceRangeEquals(getStartOffset(n), getStartOffset(n) + 3, "<<-");
	}

    protected char getSeparatorForSym(Node n) {
        // ENEBO: I added one since a sym will start with ':'...This seems like an incomplete assumption
        if (config.getSource().length() >= (getStartOffset(n)+1) && 
                config.getSource().charAt(getStartOffset(n)+1) == '\'') {
            return '\'';
        }
        return '"';
    }

	protected char getSeparatorForStr(Node n) {
		if (config.getSource().length() >= getStartOffset(n) && 
                config.getSource().charAt(getStartOffset(n)) == '\'') {
			return '\'';
		}
		return '"';
	}
	
	protected boolean inDRegxNode() {
		return false;
	}

	public Object visitStrNode(StrNode iVisited) {
		// look for a here-document:
		if (stringIsHereDocument(iVisited)) {
			print("<<-EOF");
			config.depositHereDocument(iVisited.getValue().toString());
			return null;
		}
		
		if(iVisited.getValue().equals("")) {
			if(config.getPrintQuotesInString().isTrue()) print("\"\"");
			
			return null;
		}

		// don't print quotes if we are a subpart of an other here-document
		if (config.getPrintQuotesInString().isTrue()) print(getSeparatorForStr(iVisited));

		if (inDRegxNode()) {
			print(iVisited.getValue().toString());
        } else {
			Matcher matcher = Pattern.compile("([\\\\\\n\\f\\r\\t\\\"\\\'])").matcher(iVisited.getValue().toString());

			if (matcher.find()) {
				String unescChar = unescapeChar(matcher.group(1).charAt(0));
				print(matcher.replaceAll("\\\\" + unescChar));
			} else {
				print(iVisited.getValue().toString());
			}
		}
		if (config.getPrintQuotesInString().isTrue()) print(getSeparatorForStr(iVisited));

		return null;
	}

	public static String unescapeChar(char escapedChar) {
		switch (escapedChar) {
		case '\n':
			return "n";
		case '\f':
			return "f";
		case '\r':
			return "r";
		case '\t':
			return "t";
		case '\"':
			return "\"";
		case '\'':
			return "'";
		case '\\':
			return "\\\\";
		default:
			return null;
		}
	}

	private boolean needsSuperNodeParentheses(SuperNode n) {
		return n.getArgsNode().childNodes().isEmpty() && 
            config.getSource().charAt(getEndOffset(n)) == '(';
	}

	public Object visitSuperNode(SuperNode iVisited) {
		print("super");
		if (needsSuperNodeParentheses(iVisited)) print('(');
        
		printCallArguments(iVisited.getArgsNode(), iVisited.getIterNode());
        
		if (needsSuperNodeParentheses(iVisited)) print(')');
        
		return null;
	}

	public Object visitSValueNode(SValueNode iVisited) {
		visitNode(iVisited.getValue());
		return null;
	}

	public Object visitSymbolNode(SymbolNode iVisited) {
		print(':');
		print(iVisited.getName());
		return null;
	}

	public Object visitToAryNode(ToAryNode iVisited) {
		visitNode(iVisited.getValue());
		return null;
	}

	public Object visitTrueNode(TrueNode iVisited) {
		print("true");
		return null;
	}

	public Object visitUndefNode(UndefNode iVisited) {
		print("undef ");
		print(iVisited.getName());
		return null;
	}

	public Object visitUntilNode(UntilNode iVisited) {
		print("until ");
		visitNode(iVisited.getConditionNode());
		visitNodeInIndentation(iVisited.getBodyNode());
		printNewlineAndIndentation();
		print("end");
		return null;
	}

	public Object visitVAliasNode(VAliasNode iVisited) {
		print("alias ");
		print(iVisited.getNewName());
		print(' ');
		print(iVisited.getOldName());
		return null;
	}

	public Object visitVCallNode(VCallNode iVisited) {
		print(iVisited.getName());
		return null;
	}

	public void visitNodeInIndentation(Node n) {
		config.getIndentor().indent();
		visitNode(n);
		config.getIndentor().outdent();
	}

	public Object visitWhenNode(WhenNode iVisited) {
		printNewlineAndIndentation();
		print("when ");
		enterCall();
		visitAndPrintWithSeparator(iVisited.getExpressionNodes().childNodes().iterator());
		leaveCall();
		visitNodeInIndentation(iVisited.getBodyNode());
		if ((iVisited.getNextCase() instanceof WhenNode || iVisited.getNextCase() == null)) {
			visitNode(iVisited.getNextCase());
        } else {
			printNewlineAndIndentation();
			print("else");
			visitNodeInIndentation(iVisited.getNextCase());
		}
		return null;
	}

	protected void visitNewlineInParentheses(Node n) {
		if (n instanceof NewlineNode) {
			if (((NewlineNode) n).getNextNode() instanceof SplatNode) {
				print('[');
				visitNode(((NewlineNode) n).getNextNode());
				print(']');
			} else {
				print('(');
				visitNode(((NewlineNode) n).getNextNode());
				print(')');
			}
		} else {
			visitNode(n);
		}
	}
	
	private void printWhileStatement(WhileNode iVisited) {
		print("while ");
		
		if (isAssignment(iVisited.getConditionNode())) enterCall();
		
		visitNewlineInParentheses(iVisited.getConditionNode());
		
		if (isAssignment(iVisited.getConditionNode())) leaveCall();
		
		visitNodeInIndentation(iVisited.getBodyNode());
		
		printNewlineAndIndentation();
		print("end");
	}
	
	private void printDoWhileStatement(WhileNode iVisited) {
		print("begin");
		visitNodeInIndentation(iVisited.getBodyNode());
		printNewlineAndIndentation();
		print("end while ");
		visitNode(iVisited.getConditionNode());
	}

	public Object visitWhileNode(WhileNode iVisited) {
		if (iVisited.evaluateAtStart()) {
			printWhileStatement(iVisited);
		} else {
			printDoWhileStatement(iVisited);
		}
		return null;
	}

	public Object visitXStrNode(XStrNode iVisited) {
		print('`');
		print(iVisited.getValue().toString());
		print('`');
		return null;
	}

	public Object visitYieldNode(YieldNode iVisited) {
		print("yield");
		
		if (iVisited.getArgsNode() != null) {
			print(needsParentheses(iVisited.getArgsNode()) ? '(' : ' ');

			enterCall();

			if (iVisited.getArgsNode() instanceof ArrayNode) {
				visitAndPrintWithSeparator(iVisited.getArgsNode().childNodes().iterator());
            } else {
				visitNode(iVisited.getArgsNode());
            }

			leaveCall();

			if (needsParentheses(iVisited.getArgsNode())) print(')');
		}
		return null;
	}

	public Object visitZArrayNode(ZArrayNode iVisited) {
		print("[]");
		return null;
	}

	public Object visitZSuperNode(ZSuperNode iVisited) {
		print("super");
		return null;
	}

	private static int getStartLine(Node n) {
		return n.getPosition().getStartLine();
	}

	private static int getStartOffset(Node n) {
		return n.getPosition().getStartOffset();
	}

	private static int getEndLine(Node n) {
		return n.getPosition().getEndLine();
	}

	protected static int getEndOffset(Node n) {
		return n.getPosition().getEndOffset();
	}

	public ReWriterContext getConfig() {
		return config;
	}
	
	public static String createCodeFromNode(Node node, String document){
		return createCodeFromNode(node, document, new DefaultFormatHelper());
	}
	
	public static String createCodeFromNode(Node node, String document, FormatHelper helper){
		StringWriter writer = new StringWriter();
		ReWriterContext ctx = new ReWriterContext(writer, document, helper);
		ReWriteVisitor rewriter = new ReWriteVisitor(ctx);
		rewriter.visitNode(node);
		return writer.toString();
	}

	public Object visitArgsPushNode(ArgsPushNode node) {
		assert false : "Unhandled node";
		return null;
	}

	public Object visitAttrAssignNode(AttrAssignNode iVisited) {
		if (iVisited.getName().equals("[]=")) return printIndexAssignment(iVisited);
        
		if (iVisited.getName().endsWith("=")) {
			visitNode(iVisited.getReceiverNode());
			print('.');
			
			printNameWithoutEqualSign(iVisited);
			printAssignmentOperator();
			if (iVisited.getArgsNode() != null) {
				visitAndPrintWithSeparator(iVisited.getArgsNode().childNodes().iterator());
			}
		} else {
			assert false : "Unhandled AttrAssignNode";
		}
		
		return null;
	}

	private void printNameWithoutEqualSign(INameNode iVisited) {
		print(iVisited.getName().substring(0, iVisited.getName().length() - 1));
	}

	public Object visitRootNode(RootNode iVisited) {
		config.getLocalVariables().addLocalVariable(iVisited.getStaticScope());
		visitNode(iVisited.getBodyNode());
		if (config.hasHereDocument()) config.fetchHereDocument().print();

		return null;
	}

    public Object visitRestArgNode(RestArgNode iVisited) {
        print("*" + iVisited.getName());
        return null;
    }
}
