/*
 * LocalNamesStack.java - description
 * Created on 26.02.2002, 00:46:46
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
public class LocalNamesStack extends AbstractStack {

    /**
     * Returns true if there was already an assignment to a local
     * variable named name, false otherwise.
     *
     * MRI: cf local_id
     * @param name The name of the local variable.
     * @return true if there was already an assignment to a local
     * variable named id.
     */
    public boolean isLocalRegistered(String name) {
        return ((LocalNamesElement)getTop()).isLocalRegistered(name);
    }

    /**
     * Returns the index of the local variable 'name' in the table
     * of registered variable names.
     *
     * If name is not registered yet, register the variable name.
     *
     * If name == null returns the count of registered variable names.
     *
     * MRI: cf local_cnt
     *@param name The name of the local variable
     *@return The index in the table of registered variable names.
     */
    public int getLocalIndex(String name) {
        return ((LocalNamesElement)getTop()).getLocalIndex(name);
    }

    public int registerLocal(String name) {
        return ((LocalNamesElement)getTop()).registerLocal(name);
    }

    public List getNames() {
        return ((LocalNamesElement)getTop()).getLocalNames();
    }

    public void setNames(List names) {
        ((LocalNamesElement)getTop()).setLocalNames(names);
    }

    public boolean isInBlock() {
        return ((LocalNamesElement)getTop()).isInBlock();
    }

    public void changeBlockLevel(int change) {
        ((LocalNamesElement)getTop()).changeBlockLevel(change);
    }

    public void push() {
        push(new LocalNamesElement());
    }
}