/*
 * StringEvaluate.java - No description
 * Created on 04. November 2001, 13:26
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

package org.jruby.nodes.util;

import org.jruby.*;
import org.jruby.nodes.*;
import org.jruby.nodes.types.*;

/**
 *
 * @author  jpetersen
 */
public class StringEvaluate {
    public static RubyObject eval(Ruby ruby, RubyObject self, Node node) {
        Node list = node.getNextNode();
        
        RubyString str = (RubyString)node.getLiteral();
        RubyString str2 = (RubyString)node.getNextNode().getHeadNode().getLiteral();
        
        while (list != null) {
            if (list.getHeadNode() != null) {
                Node head = list.getHeadNode();
                if (head instanceof StringExpandableNode) {
                    str2 = ((StringExpandableNode)head).expandString(ruby, self, list);
                } else {
                    str2 = (RubyString)head.eval(ruby, self).convertType(RubyString.class, "String", "to_s");
                }
                
                str.m_concat(str2);
                // str.infectObject(str2);
            }
            list = list.getNextNode();
        }
        
        return ((StringEvaluableNode)node).evalString(ruby, self, str);
    }
}