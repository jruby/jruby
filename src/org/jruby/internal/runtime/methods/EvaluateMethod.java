/*
 * EvaluateMethod.java - description
 * Created on 03.03.2002, 00:21:11
 *
 * Copyright (C) 2001, 2002 Jan Arne Petersen
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
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

import org.ablaf.ast.INode;
import org.jruby.evaluator.EvaluateVisitor;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.Arity;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.Ruby;
import org.jruby.ast.AttrSetNode;
import org.jruby.ast.InstVarNode;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class EvaluateMethod extends AbstractMethod {
    private INode node;

    public EvaluateMethod(INode node, Visibility visibility) {
        super(visibility);
        this.node = node;
    }

    public EvaluateMethod(INode node) {
        this(node, null);
    }

    public IRubyObject call(Ruby ruby, IRubyObject receiver, String name, IRubyObject[] args, boolean noSuper) {
        return EvaluateVisitor.createVisitor(receiver).eval(node);
    }

    public INode getNode() {
        return node;
    }

    public void setNode(INode node) {
        this.node = node;
    }

    public Arity getArity() {
        if (getNode() instanceof AttrSetNode) {
            return Arity.singleArgument();
        } else if (getNode() instanceof InstVarNode) {
            return Arity.noArguments();
        } else {
            return Arity.optional();
        }
    }
}
