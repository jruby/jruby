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

import java.util.List;

import org.jruby.util.collections.AbstractStack;

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

    public void ensureLocalRegistered(String name) {
        ((LocalNamesElement)getTop()).ensureLocalRegistered(name);
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
