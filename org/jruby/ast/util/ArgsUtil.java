/*
 * ArgsUtil.java - description
 * Created on 01.03.2002, 13:54:45
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

import org.ablaf.ast.INode;
import org.jruby.*;
import org.jruby.evaluator.EvaluateVisitor;
import org.jruby.runtime.*;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class ArgsUtil {
    public static Block beginCallArgs(Ruby ruby) {
        Block actBlock = ruby.getBlock().getAct();

        if (ruby.getActIter().isPre()) {
            ruby.getBlock().pop();
        }
        ruby.getIterStack().push(Iter.ITER_NOT);
        return actBlock;
    }

    public static void endCallArgs(Ruby ruby, Block actBlock) {
        if (actBlock != null) {
            // XXX
            ruby.getBlock().push(actBlock); // Refresh the next attribute.
        }
        ruby.getIterStack().pop();
    }

    public static RubyObject[] setupArgs(Ruby ruby, EvaluateVisitor visitor, INode node) {
        if (node == null) {
            return new RubyObject[0];
        }

        String file = ruby.getSourceFile();
        int line = ruby.getSourceLine();

        RubyObject args = visitor.eval(node);

        ruby.setSourceFile(file);
        ruby.setSourceLine(line);

        if (args instanceof RubyArray) {
            return ((RubyArray) args).toJavaArray();
        } else {
            return new RubyObject[] { args };
        }
    }
}
