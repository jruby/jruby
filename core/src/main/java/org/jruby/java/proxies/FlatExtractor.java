package org.jruby.java.proxies;

import org.jruby.*;
import org.jruby.ast.*;
import org.jruby.ast.visitor.AbstractNodeVisitor;
import org.jruby.parser.*;
import org.jruby.runtime.Signature;

class FlatExtractor extends AbstractNodeVisitor<Node>
		{
			private final DefNode def;
			boolean found = false;
			BlockNode bn = null;
			int level = 0;
			boolean error = false;
			boolean foundsuper = false;
			private Ruby runtime;

			FlatExtractor(Ruby runtime, DefNode def)
			{
				this.runtime = runtime;
				this.def = def;
			}

			@Override
			protected Node defaultVisit(Node node)
			{
			    if (node == null) return null;

			    if (error)
			    	return null;
			    
			    level++;

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
				Node sarg = node.getArgsNode();
				if (sarg == null)
					sarg = new ArrayNode(node.getLine());
				ArrayNode ret = new ArrayNode(node.getLine(), sarg);// TODO: block args!
				ArgsNode an = new ArgsNode(node.getLine(), null, null, null, null,null);
				StaticScope scope = StaticScopeFactory.newIRBlockScope(def.getScope());
				scope.setSignature(Signature.from(an));
				//TODO: capture args?
				ret.add(
					new FCallNode(node.getLine(), RubySymbol.newSymbol(runtime, "lambda"), null,
						new IterNode(node.getLine(), 
							an, 
							bn = new BlockNode(node.getLine()), 
							scope, 1)));
				found = true;
				return  new ReturnNode(node.getLine(), ret);
				// TODO Auto-generated method stub
				//return super.visitSuperNode(node);
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
				Node sarg = new NilNode(node.getLine());
				ArrayNode ret = new ArrayNode(node.getLine(), sarg);// TODO: block args!
				ArgsNode an = new ArgsNode(node.getLine(), null, null, null, null,null);
				StaticScope scope = StaticScopeFactory.newIRBlockScope(def.getScope());
				scope.setSignature(Signature.from(an));
				ret.add(
					new FCallNode(node.getLine(), RubySymbol.newSymbol(runtime, "lambda"), null,
						new IterNode(node.getLine(), 
							an, 
							bn = new BlockNode(node.getLine()), 
							scope, 1)));
				found = true;
				return  new ReturnNode(node.getLine(), ret);
			}
		}