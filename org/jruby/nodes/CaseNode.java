/*
 * CaseNode.java - No description
 * Created on 05. November 2001, 21:45
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

package org.jruby.nodes;

import org.jruby.*;
import org.jruby.runtime.*;

/**
 *
 * @author  jpetersen
 * @version
 */
public class CaseNode extends Node {
    public CaseNode(Node headNode, Node bodyNode) {
        super(Constants.NODE_CASE, headNode, bodyNode, null);
    }
    
    public RubyObject eval(Ruby ruby, RubyObject self) {
        RubyObject obj = getHeadNode().eval(ruby, self);
        
        Node node = getBodyNode();
        while (node != null) {
            if (!(node instanceof WhenNode)) {
                return node.eval(ruby, self);
            }
            
            Node tag = node.getHeadNode();
            while (tag != null) {
                /* if (trace_func) {
                    call_trace_func("line", tag->nd_file, nd_line(tag), self,
                    ruby_frame->last_func,
                    ruby_frame->last_class);
                }*/
                
                ruby.setSourceFile(tag.getFile());
                ruby.setSourceLine(tag.getLine());
                
                if (tag.getHeadNode() instanceof WhenNode) {
                    RubyObject obj2 = tag.getHeadNode().getHeadNode().eval(ruby, self);
                    
                    if (!(obj2 instanceof RubyArray)) {
                        obj2 = RubyArray.newArray(ruby, obj2);
                    }

                    for (int i = 0; i < ((RubyArray)obj).getLength(); i++) {
                        RubyObject eqq = ((RubyArray)obj2).entry(i).funcall("===", obj);
                        if (eqq.isTrue()) {
                            return getBodyNode().eval(ruby, self);
                        }
                    }
                } else if (tag.getHeadNode().eval(ruby, self).funcall("===", obj).isTrue()) {
                    return getBodyNode().eval(ruby, self);
                }
                tag = tag.getNextNode();
            }
            node = node.getNextNode();
        }
        return ruby.getNil();
    }
}