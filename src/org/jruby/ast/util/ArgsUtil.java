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

import java.util.ArrayList;
import java.util.Iterator;

import org.ablaf.ast.INode;
import org.ablaf.common.ISourcePosition;
import org.jruby.*;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.ExpandArrayNode;
import org.jruby.evaluator.EvaluateVisitor;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public final class ArgsUtil {
    public static Block beginCallArgs(Ruby runtime) {
        Block currentBlock = runtime.getBlockStack().getCurrent();

        if (runtime.getCurrentIter().isPre()) {
            runtime.getBlockStack().pop();
        }
        runtime.getIterStack().push(Iter.ITER_NOT);
        return currentBlock;
    }

    public static void endCallArgs(Ruby runtime, Block currentBlock) {
        runtime.getBlockStack().setCurrent(currentBlock);
        runtime.getIterStack().pop();
    }

    public static IRubyObject[] setupArgs(Ruby runtime, EvaluateVisitor visitor, INode node) {
        if (node == null) {
            return IRubyObject.NULL_ARRAY;
        }
        final ISourcePosition position = runtime.getPosition();

        if (node instanceof ArrayNode) {
            final int size = ((ArrayNode) node).size();
            final ArrayList list = new ArrayList(size);
            final Iterator iterator = ((ArrayNode) node).iterator();
            for (int i = 0; i < size; i++) {
                final INode next = (INode) iterator.next();
                if (next instanceof ExpandArrayNode) {
                    list.addAll(((RubyArray) visitor.eval(next)).getList());
                } else {
                    list.add(visitor.eval(next));
                }
            }

            runtime.setPosition(position);

            return (IRubyObject[]) list.toArray(new IRubyObject[list.size()]);
        }

        IRubyObject args = visitor.eval(node);

        runtime.setPosition(position);

        if (args instanceof RubyArray) {
            return ((RubyArray) args).toJavaArray();
        } else {
            return new IRubyObject[] { args };
        }
    }
}
