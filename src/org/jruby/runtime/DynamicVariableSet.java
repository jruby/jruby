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
package org.jruby.runtime;

import java.util.ArrayList;
import java.util.List;

import org.jruby.runtime.builtin.IRubyObject;

/**
 * Names and values of dynamic variables. Used instead of a HashMap to avoid the memory
 * and performance overhead.
 *
 * @author Anders
 */

public final class DynamicVariableSet {
    /*
     * Since the usual # of variables is low, we'll do fine with a linear
     * search of the names.
     */

    // Largest size observed in simple example code was 6, so we use
    // the next power of 2.
    private static final int INITIAL_SIZE = 8;

    private String[] names;
    private IRubyObject[] values;
    private int size = 0;

    public DynamicVariableSet() {
        names = new String[INITIAL_SIZE];
        values = new IRubyObject[INITIAL_SIZE];
    }

    public DynamicVariableSet(DynamicVariableSet original) {
        names = new String[original.names.length];
        System.arraycopy(original.names, 0, names, 0, original.names.length);
        values = new IRubyObject[original.values.length];
        System.arraycopy(original.values, 0, values, 0, original.values.length);
        size = original.size;
    }

    public void set(String name, IRubyObject value) {
        int index = indexOf(name);
        if (index == -1) {
            expandSize();
            names[size - 1] = name;
            values[size - 1] = value;
        } else {
            values[index] = value;
        }
    }

    public IRubyObject get(String name) {
        int index = indexOf(name);
        if (index == -1) {
            return null;
        }
		return values[index];
    }

    public List names() {
        List result = new ArrayList(size);
        for (int i = 0; i < size; i++) {
            result.add(names[i]);
        }
        return result;
    }

    private int indexOf(String name) {
        for (int i = 0; i < size; i++) {
            if (name.equals(names[i])) {
                return i;
            }
        }
        return -1;
    }

    private void expandSize() {
        size++;
        if (size == names.length) {
            String[] oldNames = names;
            names = new String[oldNames.length * 2];
            System.arraycopy(oldNames, 0, names, 0, oldNames.length);
            IRubyObject[] oldValues = values;
            values = new IRubyObject[oldValues.length * 2];
            System.arraycopy(oldValues, 0, values, 0, oldValues.length);
        }
    }
}
