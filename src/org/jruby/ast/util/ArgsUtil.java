/*
 * ArgsUtil.java - description
 * Created on 01.03.2002, 13:54:45
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen
 * Copyright (C) 2004 Thomas E Enebo
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
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
package org.jruby.ast.util;

import org.ablaf.ast.INode;
import org.ablaf.common.ISourcePosition;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.SplatNode;
import org.jruby.evaluator.EvaluateVisitor;
import org.jruby.runtime.Block;
import org.jruby.runtime.Iter;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public final class ArgsUtil {

    public static Block beginCallArgs(ThreadContext context) {
        Block currentBlock = context.getBlockStack().getCurrent();

        if (context.getCurrentIter().isPre()) {
            context.getBlockStack().pop();
        }
        context.getIterStack().push(Iter.ITER_NOT);
        return currentBlock;
    }

    public static void endCallArgs(ThreadContext context, Block currentBlock) {
        context.getBlockStack().setCurrent(currentBlock);
        context.getIterStack().pop();
    }

    public static IRubyObject[] setupArgs(Ruby runtime, ThreadContext context, EvaluateVisitor visitor, INode node) {
        if (node == null) {
            return IRubyObject.NULL_ARRAY;
        }
        final ISourcePosition position = context.getPosition();

        if (node instanceof ArrayNode) {
            final int size = ((ArrayNode) node).size();
            final ArrayList list = new ArrayList(size);
            final Iterator iterator = ((ArrayNode) node).iterator();
            for (int i = 0; i < size; i++) {
                final INode next = (INode) iterator.next();
                if (next instanceof SplatNode) {
                    list.addAll(((RubyArray) visitor.eval(next)).getList());
                } else {
                    list.add(visitor.eval(next));
                }
            }

            context.setPosition(position);

            return (IRubyObject[]) list.toArray(new IRubyObject[list.size()]);
        }

        context.setPosition(position);

        return arrayify(runtime, visitor.eval(node));
    }
    
    public static IRubyObject[] arrayify(Ruby runtime, IRubyObject value) {
        if (value == null) {
            value = RubyArray.newArray(runtime, 0);
        }
        
        if (value instanceof RubyArray) {
            return ((RubyArray) value).toJavaArray();
        }
        
        return new IRubyObject[] { value };
    }
}
