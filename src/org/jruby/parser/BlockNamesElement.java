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
 * Copyright (C) 2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
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

import org.jruby.util.collections.StackElement;

/**
 *
 * @author  jpetersen
 */
public class BlockNamesElement implements StackElement {
    private BlockNamesElement next;

    private List blockNames;

	public BlockNamesElement() {}
	
    public BlockNamesElement(List names) {
		setNames(names);
	}

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
