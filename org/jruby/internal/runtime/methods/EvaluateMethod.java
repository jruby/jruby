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

import org.ablaf.ast.*;
import org.jruby.*;
import org.jruby.ast.visitor.*;
import org.jruby.evaluator.*;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class EvaluateMethod extends AbstractMethod {
    private INode node;
    
    public EvaluateMethod(INode node) {
        this.node = node;
    }

    /**
     * @see IMethod#execute(Ruby, RubyObject, String, RubyObject[], boolean)
     */
    public RubyObject execute(Ruby ruby, RubyObject receiver, String name, RubyObject[] args, boolean noSuper) {
        return EvaluateVisitor.createVisitor(receiver).eval(node);
    }

    /**
     * Gets the node.
     * @return Returns a INode
     */
    public INode getNode() {
        return node;
    }

    /**
     * Sets the node.
     * @param node The node to set
     */
    public void setNode(INode node) {
        this.node = node;
    }
}
