/*
 * NodeUtil.java - description
 * Created on 27.02.2002, 13:11:03
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
package org.jruby.ast.util;

import org.ablaf.ast.*;
import org.jruby.ast.visitor.*;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public final class NodeUtil {
    // Visitors
    private static ExpressionVisitor expressionVisitor = new ExpressionVisitor();
    private static BreakStatementVisitor breakStatementVisitor = new BreakStatementVisitor();

    public static final boolean isExpression(INode node) {
        return expressionVisitor.isExpression(node);
    }

    public static final boolean isBreakStatement(INode node) {
        return breakStatementVisitor.isBreakStatement(node);
    }
}