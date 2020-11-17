package org.jruby.java.proxies;

import org.jruby.*;
import org.jruby.ast.*;
import org.jruby.ast.visitor.AbstractNodeVisitor;
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

		node.childNodes().forEach(this::defaultVisit); 
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
		def.getArgsNode().accept(new ScopeKeeper(scope));

		// TODO: capture args?
		ret.add(new LambdaNode(line, an, blk, scope, line)
		/*
		 * new FCallNode(line, RubySymbol.newSymbol(runtime, "lambda"), null, new
		 * IterNode(line, an, blk, scope, 1))
		 */);
		found = true;
		return new ReturnNode(line, ret);
	}

	public static class ScopeKeeper extends AbstractNodeVisitor<StaticScope>
	{
		private StaticScope scope;

		ScopeKeeper(StaticScope ss)
		{
			this.scope = ss;
		}

		@Override
		protected StaticScope defaultVisit(Node node)
		{
			if (node == null)
				return null;

			node.childNodes().forEach(n -> n.accept(this));
			return scope;
		}

		@Override
		public StaticScope visitArgumentNode(ArgumentNode node)
		{
			scope.addVariableThisScope(node.getName().toString());
			return scope;
		}
		
		@Override
		public StaticScope visitRestArgNode(RestArgNode node)
		{
			scope.addVariableThisScope(node.getName().toString());
			return scope;
		}

		@Override
		public StaticScope visitKeywordRestArgNode(KeywordRestArgNode node)
		{
			scope.addVariableThisScope(node.getName().toString());
			return scope;
		}
	}
}