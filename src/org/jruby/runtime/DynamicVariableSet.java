/*
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with JRuby; if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307 USA
 */
package org.jruby.runtime;

import org.jruby.runtime.builtin.IRubyObject;

import java.util.ArrayList;
import java.util.List;

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
        } else {
            return values[index];
        }
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
