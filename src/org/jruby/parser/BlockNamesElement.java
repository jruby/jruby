/*
 * BlockNamesElement.java - description
 * Created on 26.02.2002, 00:52:39
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
package org.jruby.parser;

import java.util.*;

import org.jruby.util.collections.*;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class BlockNamesElement implements StackElement {
    private BlockNamesElement next;

    private List blockNames;

    /**
     * @see StackElement#getNext()
     */
    public StackElement getNext() {
        return next;
    }

    /**
     * @see StackElement#setNext(StackElement)
     */
    public void setNext(StackElement newNext) {
        next = (BlockNamesElement)newNext;
    }
    
    public boolean isDefined(String name) {
        return (blockNames != null ? blockNames.contains(name) : false) || (next != null && next.isDefined(name));
    }

    public void add(String name) {
        if (blockNames == null) {
            blockNames = new ArrayList();
        }
        blockNames.add(name);
    }

    public List getNames() {
        return blockNames != null ? Collections.unmodifiableList(blockNames) : Collections.EMPTY_LIST;
    }

    public void setNames(List names) {
        blockNames = names;
    }
}