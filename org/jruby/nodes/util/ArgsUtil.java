/*
 * ArgsUtil.java - No description
 * Created on 04. November 2001, 18:13
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
import org.jruby.runtime.*;

/**
 *
 * @author  jpetersen
 */
public class ArgsUtil {
    public static RubyBlock beginCallArgs(Ruby ruby) {
        RubyBlock tmpBlock = ruby.getBlock().getTmp();
        if (ruby.getIter().getIter() == RubyIter.ITER_PRE) {
            ruby.getBlock().pop();
        }
        ruby.getIter().push(RubyIter.ITER_NOT);
        return tmpBlock;
    }
    
    public static void endCallArgs(Ruby ruby, RubyBlock tmpBlock) {
        ruby.getBlock().setTmp(tmpBlock);
        ruby.getIter().pop();
    }
    
    public static RubyObject[] setupArgs(Ruby ruby, RubyObject self, ArrayNode node) {
        String file = ruby.getSourceFile();
        int line = ruby.getSourceLine();
        
        RubyObject[] args = node.getArray(ruby, self);
        
        ruby.setSourceFile(file);
        ruby.setSourceLine(line);
        
        return args;
    }
    
    public static RubyObject[] setupArgs(Ruby ruby, RubyObject self, Node node) {
        RubyObject[] result = new RubyObject[0];
        
        if (node != null) {
            RubyObject args = node.eval(ruby, self);
            
            String file = ruby.getSourceFile();
            int line = ruby.getSourceLine();
            
            if (!(args instanceof RubyArray)) {
                args = RubyArray.m_newArray(ruby, args);
            }
            result = ((RubyArray)args).toJavaArray();
            
            ruby.setSourceFile(file);
            ruby.setSourceLine(line);
            
        }
        
        return result;
    }
}
