/*
 * EvaluateMethod.java - description
 * Created on 03.03.2002, 00:21:11
 *
 * Copyright (C) 2001, 2002 Jan Arne Petersen
 * Copyright (C) 2004 Thomas E Enebo
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Thomas E Enebo <enebo@acm.org>
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
package org.jruby.internal.runtime.methods;

import org.jruby.Ruby;
import org.jruby.ast.AttrSetNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.GlobalAsgnNode;
import org.jruby.ast.InstVarNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.Node;
import org.jruby.ast.ZeroArgNode;
import org.jruby.evaluator.EvaluateVisitor;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ICallable;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class EvaluateMethod extends AbstractMethod {
    private final Node node;
    private final Arity arity;

    public EvaluateMethod(Node node, Visibility visibility) {
    	this(node, visibility, arityOf(node)); // for compatibility
    }
    
    public EvaluateMethod(Node node, Visibility visibility, Arity arity) {
        super(visibility);
        this.node = node;
        this.arity = arity;
    }

    public EvaluateMethod(Node node, Node vars) {
    	this(node, null, procArityOf(vars));
    }

    public IRubyObject call(Ruby runtime, IRubyObject receiver, String name, IRubyObject[] args, boolean noSuper) {
        return EvaluateVisitor.createVisitor(receiver).eval(node);
    }

    public Node getNode() {
        return node;
    }

    public Arity getArity() {
    	return arity;
    }
    
    private static Arity arityOf(Node node) {
        if (node instanceof AttrSetNode) {
            return Arity.singleArgument();
        } else if (node instanceof InstVarNode) {
            return Arity.noArguments();
        } else {
            return Arity.optional();
        }
    }
    
    private static Arity procArityOf(Node node) { //FIXME make all args a commmon type, then refactor 
    	if (node == null) {
    		return Arity.optional();
    	} else if (node instanceof ZeroArgNode) {
    		return Arity.noArguments();
    	} else if (node instanceof DAsgnNode || node instanceof LocalAsgnNode || node instanceof GlobalAsgnNode) {
    		return Arity.singleArgument();
    	} else if (node instanceof MultipleAsgnNode) {
    		MultipleAsgnNode n = (MultipleAsgnNode) node;
    		if (n.getArgsNode() != null) {
    			return Arity.required(n.getHeadNode() == null ? 0 : n.getHeadNode().size());
    		}
    		return Arity.fixed(n.getHeadNode().size());
    	} else {
    		throw new Error("unexpected type " + node.getClass());
    	}
    }
    
    public ICallable dup() {
        return new EvaluateMethod(node, getVisibility());
    }
}
