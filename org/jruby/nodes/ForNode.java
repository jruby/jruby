/*
 * ForNode.java - No description
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
import org.jruby.exceptions.*;
import org.jruby.nodes.util.*;
import org.jruby.runtime.*;

/**
 *
 * @author  jpetersen
 * @version
 */
public class ForNode extends Node {
    public ForNode(Node varNode, Node bodyNode, Node iterNode) {
        super(Constants.NODE_FOR, varNode, bodyNode, iterNode);
    }
    
    public RubyObject eval(Ruby ruby, RubyObject self) {
        RubyObject result;
        
        ruby.getBlock().push(getVarNode(), getBodyNode(), self);
        ruby.getIter().push(RubyIter.ITER_PRE);
        while (true) {
            try {
                String file = ruby.getSourceFile();
                int line = ruby.getSourceLine();
                
                ruby.getBlock().flags &= ~RubyBlock.BLOCK_D_SCOPE;
                
                RubyBlock tmpBlock = ArgsUtil.beginCallArgs(ruby);
                RubyObject recv = getIterNode().eval(ruby, self);
                ArgsUtil.endCallArgs(ruby, tmpBlock);
                
                ruby.setSourceFile(file);
                ruby.setSourceLine(line);
                result = recv.getRubyClass().call(recv, "each", null, 0);
                break;
            } catch (RetryException rExcptn) {
            } catch (ReturnException rExcptn) {
                result = rExcptn.getReturnValue();
                break;
            } catch (BreakException bExcptn) {
                result = ruby.getNil();
                break;
            }
        }
        ruby.getIter().pop();
        ruby.getBlock().pop();
        return result;
    }
}