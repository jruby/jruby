package org.jruby.java.proxies;

import org.jruby.*;
import org.jruby.ast.*;
import org.jruby.ast.types.INameNode;
import org.jruby.ast.visitor.*;
import org.jruby.ir.IRScopeType;
import org.jruby.parser.*;
import org.jruby.runtime.Signature;

class FlatExtractor extends AbstractNodeVisitor<Node>
{
	private final DefNode def;
	BlockNode bn = null;
	int level = 0;
	boolean error = false;
	private boolean found = false;
	boolean foundsuper = false;
	int superline = -1;
	private Ruby runtime;

	FlatExtractor(Ruby runtime, DefNode def)
	{
		this.runtime = runtime;
		this.def = def;
	}

	@Override
	protected Node defaultVisit(Node node)
	{
		if (node == null)
			return null;

		if (error)
			return null;

		level++;
		System.out.println("Node " + node.getLine() + "/" + node.getClass().getSimpleName());

		node.childNodes().forEach(x -> x.accept(this)); 
		level--;

		return node;
	}

	@Override
	public Node visitBlockNode(BlockNode node)
	{
		if (error)
			return null;
		if (found)
			return node;

		BlockNode replacement = new BlockNode(node.getLine());

		level++;

		boolean seenReturn = false;

		for (Node child : node.children())
		{
			Node newc = child.accept(this);
			if (found)
			{
				if (seenReturn)
				{
					bn.add(newc);
					continue;
				}
				else
				{
					seenReturn = true;
				}
			}
			replacement.add(newc);
		}
		level--;

		return replacement;
	}

	@Override
	public Node visitSuperNode(SuperNode node)
	{
		foundsuper = true;
		if (level != 1)
		{
			error = true;
			return null;
		}
		superline = node.getLine();
		Node sarg = node.getArgsNode();
		if (sarg == null)
			sarg = new ArrayNode(node.getLine());
		return buildRewrite(node.getLine(), sarg, bn = new BlockNode(node.getLine()));
	}

	@Override
	public Node visitZSuperNode(ZSuperNode node)
	{
		foundsuper = true;
		if (level != 1)
		{
			error = true;
			return null;
		}
		superline = node.getLine();
		Node sarg = new NilNode(node.getLine());
		return buildRewrite(node.getLine(), sarg, bn = new BlockNode(node.getLine()));
	}

	public ReturnNode buildRewrite(int line, Node sarg, Node blk)
	{
		ArrayNode ret = new ArrayNode(line, sarg);// TODO: block args!
		ArgsNode an = new ArgsNode(line, null, null, null, null, null);
		StaticScope scope = StaticScopeFactory.newIRBlockScope(def.getScope());// .duplicate()
		scope.setScopeType(IRScopeType.CLOSURE);
		scope.setSignature(Signature.from(an));
		// def.getArgsNode().

		// TODO: capture args?
		ret.add(//new LambdaNode(line, an, blk, scope, line)
		new FCallNode(line, RubySymbol.newSymbol(runtime, "lambda"), null, new
		 IterNode(line, an, blk, scope, 1))
		 );
		found = true;
		return new ReturnNode(line, ret);
	}
	
	@Override
	public Node visitLocalVarNode(LocalVarNode node)
	{
		if (!found)
			return node;
		
		return new LocalVarNode(node.getLine(), deepen(node), node.getName());
		
	}

	private int deepen(IScopedNode node)
	{
		return ((node.getDepth()+1) << 16) | node.getIndex();
	}
	

	@Override
	public Node visitLocalAsgnNode(LocalAsgnNode node) {
		//TODO: is this true for all situations?
		if (!found)
			return node;
		
		return new LocalAsgnNode(node.getLine(), node.getName(), deepen(node), v(node.getValueNode()));
	}
	
	private Node v(Node node)
	{
		if (node == null)
			return null;
		return node.accept(this);
	}
	
	@Override
	public Node visitFlipNode(FlipNode node) {
		throw new IllegalStateException("No flip flops in the constructor!");
	}
	
	/// boring
	
	
	
	
	@Override
	public Node visitArgumentNode(ArgumentNode node) {
		return new ArgumentNode(node.getLine(), node.getName(), ((node.getDepth()+0) << 16) | node.getIndex());// TODO: is this 0?
	}
	
	@Override
	public Node visitVCallNode(VCallNode node) {
		return new VCallNode(node.getLine(), node.getName());
	}
	
	@Override
	public Node visitGlobalAsgnNode(GlobalAsgnNode node) {
		return new GlobalAsgnNode(node.getLine(), node.getName(), v(node.getValueNode()));
	}
	
	@Override
	public Node visitGlobalVarNode(GlobalVarNode node) {
		return new GlobalVarNode(node.getLine(), node.getName());
	}
	
	
	@Override
	public Node visitCallNode(CallNode node) {
		return new CallNode(node.getLine(), v(node.getReceiverNode()), node.getName(), v(node.getArgsNode()), v(node.getIterNode()), node.isLazy());
	}
	
	@Override
	public Node visitDVarNode(DVarNode node) {
		return new DVarNode(node.getLine(), deepen(node), node.getName());
	}
	
	@Override
	public Node visitDAsgnNode(DAsgnNode node) {
		return new DAsgnNode(node.getLine(), node.getName(), deepen(node), v(node.getValueNode()));
	}
	
	@Override
	public Node visitBeginNode(BeginNode node) {
		return new BeginNode(node.getLine(), v(node.getBodyNode()));
	}
	
	@Override
	public Node visitBignumNode(BignumNode node) {
		return new BignumNode(node.getLine(), node.getValue());
	}
	
	@Override
	public Node visitBlockPassNode(BlockPassNode node) {
		return new BlockPassNode(node.getLine(), v(node.getBodyNode()));
	}
	
	@Override
	public Node visitBreakNode(BreakNode node) {
		return new BreakNode(node.getLine(), v(node.getValueNode()));
	}
	
	@Override
	public Node visitConstDeclNode(ConstDeclNode node) {
		return new ConstDeclNode(node.getLine(), node.getName(), (INameNode)v(node.getConstNode()), v(node.getValueNode()));
	}
	
	@Override
	public Node visitClassVarAsgnNode(ClassVarAsgnNode node) {
		return new ClassVarAsgnNode(node.getLine(), node.getName(), v(node.getValueNode()));
	}
	
	@Override
	public Node visitClassVarNode(ClassVarNode node) {
		return new ClassVarNode(node.getLine(), node.getName());
	}
	
	@Override
	public Node visitCaseNode(CaseNode node) {
		return new CaseNode(node.getLine(), v(node.getCaseNode()), node.getCases());
	}
	
	@Override
	public Node visitClassNode(ClassNode node) {
		return new ClassNode(node.getLine(), node.getCPath(), node.getScope(), v(node.getBodyNode()), v(node.getSuperNode()), node.getEndLine());
	}
	
	@Override
	public Node visitColon2Node(Colon2Node node) {
		if (node instanceof Colon2ConstNode)
			return new Colon2ConstNode(node.getLine(), v(node.getLeftNode()), node.getName());
		else if (node instanceof Colon2ImplicitNode)
			return new Colon2ImplicitNode(node.getLine(), node.getName());
		throw new IllegalStateException("Ack");// TODO:???
	}
	
	@Override
	public Node visitColon3Node(Colon3Node node) {
		return new Colon3Node(node.getLine(), node.getName());
	}
	
	@Override
	public Node visitComplexNode(ComplexNode node) {
		return new ComplexNode(node.getLine(), node.getNumber());
	}
	
	@Override
	public Node visitConstNode(ConstNode node) {
		return new ConstNode(node.getLine(), node.getName());
	}
	
	@Override
	public Node visitDRegxNode(DRegexpNode node) {
		ListNode an = new DRegexpNode(node.getLine(), node.getOptions(), node.getEncoding());
		for (Node child : node.children())
			an.add(v(child));
		return an;
	}
	
	@Override
	public Node visitDStrNode(DStrNode node) {
		ListNode an = new DStrNode(node.getLine(), node.getEncoding());
		for (Node child : node.children())
			an.add(v(child));
		return an;
	}
	
	@Override
	public Node visitDefinedNode(DefinedNode node) {
		return new DefinedNode(node.getLine(), v(node.getExpressionNode()));
	}
	
	@Override
	public Node visitDefnNode(DefnNode node) {
		return new DefnNode(node.getLine(), node.getName(), node.getArgsNode(), node.getScope(), v(node.getBodyNode()), node.getEndLine());
	}
	
	@Override
	public Node visitDefsNode(DefsNode node) {
		return new DefsNode(node.getLine(), v(node.getReceiverNode()), node.getName(), node.getArgsNode(), node.getScope(), v(node.getBodyNode()), node.getEndLine());
	}
	
	@Override
	public Node visitDotNode(DotNode node) {
		return new DotNode(node.getLine(), v(node.getBeginNode()), v(node.getEndNode()), node.isExclusive(), node.isLiteral());
	}
	
	@Override
	public Node visitEncodingNode(EncodingNode node) {
		return new EncodingNode(node.getLine(), node.getEncoding());
	}
	
	@Override
	public Node visitEnsureNode(EnsureNode node) {
		return new EnsureNode(node.getLine(), v(node.getBodyNode()), v(node.getEnsureNode()));
	}
	
	@Override
	public Node visitEvStrNode(EvStrNode node) {
		return new EvStrNode(node.getLine(), v(node.getBody()));
	}
	
	@Override
	public Node visitFalseNode(FalseNode node) {
		return new FalseNode(node.getLine());
	}
	
	@Override
	public Node visitFixnumNode(FixnumNode node) {
		return new FixnumNode(node.getLine(), node.getValue());
	}
	
	@Override
	public Node visitFloatNode(FloatNode node) {
		return new FloatNode(node.getLine(), node.getValue());
	}
	
	@Override
	public Node visitForNode(ForNode node) {
		return new ForNode(node.getLine(), v(node.getVarNode()), v(node.getBodyNode()), v(node.getIterNode()), node.getScope(), node.getEndLine());
	}
	
	@Override
	public Node visitInstAsgnNode(InstAsgnNode node) {
		return new InstAsgnNode(node.getLine(), node.getName(), v(node.getValueNode()));
	}
	
	@Override
	public Node visitInstVarNode(InstVarNode node) {
		return new InstVarNode(node.getLine(), node.getName());
	}
	
	@Override
	public Node visitIfNode(IfNode node) {
		return new IfNode(node.getLine(), v(node.getCondition()), v(node.getThenBody()), v(node.getElseBody()));
	}
	
	@Override
	public Node visitKeywordArgNode(KeywordArgNode node) {
		return new KeywordArgNode(node.getLine(), node.getAssignable());
	}
	
	@Override
	public Node visitKeywordRestArgNode(KeywordRestArgNode node) {
		return new KeywordRestArgNode(node.getLine(), node.getName(), node.getIndex());
	}
	
	@Override
	public Node visitLambdaNode(LambdaNode node) {
		return new LambdaNode(node.getLine(), node.getArgs(), v(node.getBody()), node.getScope(), node.getEndLine());
	}
	
	@Override
	public Node visitLiteralNode(LiteralNode node) {
		return new LiteralNode(node.getLine(), node.getSymbolName());
	}
	
	@Override
	public Node visitMultipleAsgnNode(MultipleAsgnNode node) {
		return new MultipleAsgnNode(node.getLine(), node.getPre(), v(node.getRest()), node.getPost());
	}
	
	@Override
	public Node visitMatch2Node(Match2Node node) {
		return new Match2Node(node.getLine(), v(node.getReceiverNode()), v(node.getValueNode()));
	}
	
	@Override
	public Node visitMatch3Node(Match3Node node) {
		return new Match3Node(node.getLine(), v(node.getReceiverNode()), v(node.getValueNode()));
	}
	
	@Override
	public Node visitMatchNode(MatchNode node) {
		return new MatchNode(node.getLine(), v(node.getRegexpNode()));
	}
	
	@Override
	public Node visitModuleNode(ModuleNode node) {
		return new ModuleNode(node.getLine(), node.getCPath(), node.getScope(), v(node.getBodyNode()), node.getEndLine());
	}
	
	@Override
	public Node visitNewlineNode(NewlineNode node) {
		return new NewlineNode(node.getLine(), v(node.getNextNode()));
	}
	
	@Override
	public Node visitNextNode(NextNode node) {
		return new NextNode(node.getLine(), v(node.getValueNode()));
	}
	
	@Override
	public Node visitNilNode(NilNode node) {
		return new NilNode(node.getLine());
	}
	
	@Override
	public Node visitNthRefNode(NthRefNode node) {
		return new NthRefNode(node.getLine(), node.getMatchNumber());
	}
	
	@Override
	public Node visitOpElementAsgnNode(OpElementAsgnNode node) {
		return new OpElementAsgnNode(node.getLine(), v(node.getReceiverNode()), node.getOperatorSymbolName(), v(node.getArgsNode()), v(node.getValueNode()), v(node.getBlockNode()));
	}
	
	@Override
	public Node visitOpAsgnNode(OpAsgnNode node) {
		return new OpAsgnNode(node.getLine(), v(node.getReceiverNode()), v(node.getValueNode()), node.getVariableSymbolName(), node.getOperatorSymbolName(), node.isLazy());
	}
	
	@Override
	public Node visitOptArgNode(OptArgNode node) {
		return new OptArgNode(node.getLine(), v(node.getValue()));
	}
	
	@Override
	public Node visitOrNode(OrNode node) {
		return new OrNode(node.getLine(), v(node.getFirstNode()), v(node.getSecondNode()));
	}
	
	@Override
	public Node visitPreExeNode(PreExeNode node) {
		return new PreExeNode(node.getLine(), node.getScope(), v(node.getBodyNode()), node.getEndLine());
	}
	
	@Override
	public Node visitPostExeNode(PostExeNode node) {
		return new PostExeNode(node.getLine(), v(node.getBodyNode()), node.getEndLine());
	}
	
	@Override
	public Node visitRationalNode(RationalNode node) {
		return new RationalNode(node.getLine(), node.getNumerator(), node.getDenominator());
	}
	
	@Override
	public Node visitRedoNode(RedoNode node) {
		return new RedoNode(node.getLine());
	}
	
	@Override
	public Node visitRegexpNode(RegexpNode node) {
		return new RegexpNode(node.getLine(), node.getValue(), node.getOptions());
	}
	
	@Override
	public Node visitRequiredKeywordArgumentValueNode(RequiredKeywordArgumentValueNode node) {
		return new RequiredKeywordArgumentValueNode();
	}
	
	@Override
	public Node visitRescueBodyNode(RescueBodyNode node) {
		return new RescueBodyNode(node.getLine(), v(node.getExceptionNodes()), v(node.getBodyNode()), node.getOptRescueNode());
	}
	
	@Override
	public Node visitRescueNode(RescueNode node) {
		return new RescueNode(node.getLine(), v(node.getBodyNode()), node.getRescueNode(), v(node.getElseNode()));
	}
	
	@Override
	public Node visitRetryNode(RetryNode node) {
		return new RetryNode(node.getLine());
	}
	
	@Override
	public Node visitReturnNode(ReturnNode node) {
		return new ReturnNode(node.getLine(), v(node.getValueNode()));
	}
	
	@Override
	public Node visitSClassNode(SClassNode node) {
		return new SClassNode(node.getLine(), v(node.getReceiverNode()), node.getScope(), v(node.getBodyNode()), node.getEndLine());
	}
	
	@Override
	public Node visitSelfNode(SelfNode node) {
		return new SelfNode(node.getLine());
	}
	
	@Override
	public Node visitSplatNode(SplatNode node) {
		return new SplatNode(node.getLine(), v(node.getValue()));
	}
	
	@Override
	public Node visitStarNode(StarNode node) {
		return new StarNode(node.getLine());
	}
	
	@Override
	public Node visitSValueNode(SValueNode node) {
		return new SValueNode(node.getLine(), v(node.getValue()));
	}
	
	@Override
	public Node visitSymbolNode(SymbolNode node) {
		return new SymbolNode(node.getLine(), node.getName());
	}
	
	@Override
	public Node visitAliasNode(AliasNode node) {
		return new AliasNode(node.getLine(), v(node.getNewName()), v(node.getOldName()));
	}
	
	@Override
	public Node visitAndNode(AndNode node) {
		return new AndNode(node.getLine(), v(node.getFirstNode()), v(node.getSecondNode()));
	}
	
	@Override
	public Node visitArgsCatNode(ArgsCatNode node) {
		return new ArgsCatNode(node.getLine(), v(node.getFirstNode()), v(node.getSecondNode()));
	}
	
	@Override
	public Node visitArgsPushNode(ArgsPushNode node) {
		return new ArgsPushNode(node.getLine(), v(node.getFirstNode()), v(node.getSecondNode()));
	}
	
	@Override
	public Node visitAttrAssignNode(AttrAssignNode node) {
		return new AttrAssignNode(node.getLine(), v(node.getReceiverNode()), node.getName(), v(node.getArgsNode()), v(node.getBlockNode()), node.isLazy());
	}
	
	@Override
	public Node visitBackRefNode(BackRefNode node) {
		return new BackRefNode(node.getLine(), node.getType());
	}
	
	@Override
	public Node visitTrueNode(TrueNode node) {
		return new TrueNode(node.getLine());
	}
	
	@Override
	public Node visitUndefNode(UndefNode node) {
		return new UndefNode(node.getLine(), v(node.getName()));
	}
	
	@Override
	public Node visitVAliasNode(VAliasNode node) {
		return new VAliasNode(node.getLine(), node.getNewName(), node.getOldName());
	}
	
	@Override
	public Node visitWhenNode(WhenNode node) {
		return new WhenNode(node.getLine(), v(node.getExpressionNodes()), v(node.getBodyNode()), v(node.getNextCase()));
	}
	
	@Override
	public Node visitYieldNode(YieldNode node) {
		return new YieldNode(node.getLine(), v(node.getArgsNode()));
	}
	
	@Override
	public Node visitZArrayNode(ZArrayNode node) {
		ListNode an = new ZArrayNode(node.getLine());
		for (Node child : node.children())
			an.add(v(child));
		return an;
	}
	
	
	@Override
	public Node visitClassVarDeclNode(ClassVarDeclNode node) {
		return new ClassVarDeclNode(node.getLine(), node.getName(), v(node.getValueNode()));
	}
	

}