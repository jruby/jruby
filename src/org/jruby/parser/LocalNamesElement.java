/*
 * LocalVariableNames.java - description
 * Created on 25.02.2002, 21:34:15
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
public class LocalNamesElement implements StackElement {
    private LocalNamesElement next;

    private List localNames;
    private int blockLevel;

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
        this.next = (LocalNamesElement) newNext;
    }

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
        if (localNames == null) {
            return false;
        } else {
            Iterator iter = localNames.iterator();
            while (iter.hasNext()) {
                if (name.equals(iter.next())) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Returns the index of the local variable 'name' in the table
     * of registered variable names.
     * 
     * If name is not registered yet, register the variable name.
     * 
     * If name == null returns the count of registered variable names.
     * MRI: cf local_cnt
     * @todo: this method is often used for its side effect (for registering a name)
	 * 		  it should either be renamed or an other method without a return and with
	 * 		  a better name should be used for this. the getLocalIndex name makes one
	 * 		  believe the main purpose of this method is getting something while the
	 * 		  result is often ignored.
     *@param name The name of the local variable
     *@return The index in the table of registered variable names.
     */
    public int getLocalIndex(String name) {
        if (name == null) {
            return localNames != null ? localNames.size() : 0;
        } else if (localNames == null) {
            return registerLocal(name);
        }

        for (int i = 0; i < localNames.size(); i++) {
            if (name.equals(localNames.get(i))) {
                return i;
            }
        }

        return registerLocal(name);
    }

    /**
     * Register the local variable name 'name' in the table 
     * of registered variable names. 
     * Returns the index of the added local variable name in the table.
     * 
     * MRI: cf local_append
     *@param name The name of the local variable.
     *@return The index of the local variable name in the table.
     */
    public int registerLocal(String name) {
        if (localNames == null) {
            localNames = new ArrayList();
            localNames.add("_");
            localNames.add("~");

            if (name.equals("_")) {
                return 0;
            } else if (name.equals("~")) {
                return 1;
            }
        }

        localNames.add(name);

        return localNames.size() - 1;
    }
    
    /**
     * Gets the blockLevel.
     * @return Returns a int
     */
    public int getBlockLevel() {
        return blockLevel;
    }

    /**
     * Sets the blockLevel.
     * @param blockLevel The blockLevel to set
     */
    public void setBlockLevel(int blockLevel) {
        this.blockLevel = blockLevel;
    }

    /**
     * Gets the localNames.
     * @return Returns a List
     */
    public List getLocalNames() {
        return localNames != null ? localNames : new ArrayList(0);
    }

    /**
     * Sets the localNames.
     * @param localNames The localNames to set
     */
    public void setLocalNames(List localNames) {
        this.localNames = localNames;
    }
}
