/*
 * ListNodeUtil.java - description
 * Created on 25.02.2002, 12:56:30
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
import org.jruby.ast.types.IListNode;

import java.util.Iterator;

/** Some IListNode utils.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public final class ListNodeUtil {
    public static final IListNode addAll(IListNode self, IListNode other) {
        if (other != null) {
            Iterator iter = other.iterator();
            int size = other.size();
            for (int i = 0; i < size; i++) {
                self.add((INode) iter.next());
            }
        }

        return self;
    }

    public static final INode getLast(IListNode self) {
        INode result = null;

        Iterator iterator = self.iterator();
        while (iterator.hasNext()) {
            result = (INode) iterator.next();
        }

        return result;
    }

    public static final int getLength(IListNode self) {
        if (self == null) {
            return 0;
        } else {
            return self.size();
        }
    }
}