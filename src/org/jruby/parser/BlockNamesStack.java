/*
 * BlockNamesStack.java - description
 * Created on 26.02.2002, 00:58:03
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

import java.util.List;

import org.jruby.util.collections.AbstractStack;
import org.jruby.util.collections.StackElement;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class BlockNamesStack extends AbstractStack {
    private LocalNamesStack localNames;

    public BlockNamesStack(LocalNamesStack localNames) {
        this.localNames = localNames;
    }

    public boolean isDefined(String name) {
        return getTop() != null ? ((BlockNamesElement)getTop()).isDefined(name) : false;
    }

    public void add(String name) {
        ((BlockNamesElement)getTop()).add(name);
    }

    public boolean isInBlock() {
        return localNames.isInBlock();
    }

    /**
     * @see AbstractStack#pop()
     */
    public StackElement pop() {
        localNames.changeBlockLevel(-1);
        return super.pop();
    }

    /**
     * @see AbstractStack#push(StackElement)
     */
    public void push(StackElement newElement) {
        localNames.changeBlockLevel(1);
        super.push(newElement);
    }

    public void push() {
        push(new BlockNamesElement());
    }

    public void push(List blockNames) {
        push(new BlockNamesElement());
        setNames(blockNames);
    }

    public List getNames() {
        return ((BlockNamesElement)getTop()).getNames();
    }

    public void setNames(List names) {
        ((BlockNamesElement)getTop()).setNames(names);
    }
}