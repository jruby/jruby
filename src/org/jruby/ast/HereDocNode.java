/*
 * HereDocNode.java - description
 *
 * Copyright (C) 2004 Thomas E Enebo
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
package org.jruby.ast;

import org.ablaf.ast.visitor.INodeVisitor;
import org.ablaf.common.ISourcePosition;

public class HereDocNode extends AbstractNode {
    String value;
    int func;
    int lastLineLength;
    
    public HereDocNode(ISourcePosition position, String value, int func, 
            int lastLineLength) {
        this.value = value;
        this.func = func;
        this.lastLineLength = lastLineLength;
    }
    
    public String getValue() {
        return value;
    }
    
    public int getFunc() {
        return func;
    }
    
    public int getLastLineLength() {
        return lastLineLength;
    }

    public void accept(INodeVisitor visitor) {
    }
}
