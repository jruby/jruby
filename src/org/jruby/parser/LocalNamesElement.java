/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author  jpetersen
 */
public class LocalNamesElement {
    private static final String[] EMPTY_NAMES = new String[0];
    private List localNames;
    private int blockLevel;

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
		return localNames != null && localNames.contains(name);
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
        int index = localNames.indexOf(name);
        if (index != -1) {
            return index;
        }
        return registerLocal(name);
    }

    public void ensureLocalRegistered(String name) {
        if (! isLocalRegistered(name)) {
            registerLocal(name);
        }
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

            if (name != null) {
                if (name.equals("_")) {
                    return 0;
                } else if (name.equals("~")) {
                    return 1;
                }
            }
        }
        localNames.add(name);
        return localNames.size() - 1;
    }

    /**
     * Gets the localNames.
     * @return Returns a List
     */
    public List getNames() {
        return localNames != null ? localNames : Collections.EMPTY_LIST;
    }
    
    public String[] getNamesArray() {
        if (localNames == null) {
            return EMPTY_NAMES;
        }
        
        String[] names = new String[localNames.size()];
        localNames.toArray(names);
        
        return names;
    }

    /**
     * Sets the localNames.
     * @param localNames The localNames to set
     */
    public void setNames(List localNames) {
        this.localNames = localNames;
    }

    public boolean isInBlock() {
        return blockLevel > 0;
    }

    public void changeBlockLevel(int change) {
        blockLevel = blockLevel + change;
    }
}
