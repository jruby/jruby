/*
 * WhenNode.java - No description
 * Created on 25. Oktober 2001, 22:02
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
public class WhenNode extends Node {
    
    public WhenNode(Node headNode, Node bodyNode, Node nextNode) {
        super(Constants.NODE_WHEN, headNode, bodyNode, nextNode);
    }
    
    public RubyObject eval(Ruby ruby, RubyObject self) {
        WhenNode node = this;
        
        while (node != null) {
            Node tag = node.getHeadNode();
            while (tag != null) {
/*             if (trace_func) {
                   call_trace_func("line", tag->nd_file, nd_line(tag), self,
                                    ruby_frame->last_func,
                                    ruby_frame->last_class);
                        }*/
                //
                
                ruby.setSourceFile(tag.getFile());
                ruby.setSourceLine(tag.getLine());
                
                if (tag.getHeadNode() instanceof WhenNode) {
                    RubyObject obj = tag.getHeadNode().getHeadNode().eval(ruby, self);
                    
                    if (!(obj instanceof RubyArray)) {
                        obj = RubyArray.m_newArray(ruby, obj);
                    }
                    
                    for (int i = 0; i < ((RubyArray)obj).length(); i++) {
                        if (((RubyArray)obj).entry(i).isTrue()) {
                            return node.getBodyNode().eval(ruby, self);
                        }
                    }
                } else if (tag.getHeadNode().eval(ruby, self).isTrue()) {
                    return node.getBodyNode().eval(ruby, self);
                }
                tag = tag.getNextNode();
            }
            if (node.getNextNode() instanceof WhenNode) {
                node = (WhenNode)node.getNextNode();
            } else {
                return node.getNextNode().eval(ruby, self);
            }
        }
        return ruby.getNil();
    }
}