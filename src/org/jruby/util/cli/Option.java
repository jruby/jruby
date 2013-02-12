/*
 **** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
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
 * Copyright (C) 2001-2011 The JRuby Community (and contribs)
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.util.cli;

import org.jruby.util.SafePropertyAccessor;

/**
 * Represents a single option to JRuby, with a category, name, value type,
 * options, default value, and description.
 *
 * This type should be subclassed for specific types of values.
 *
 * @param <T> the type of value associated with the option
 */
public abstract class Option<T> {
    public Option(Category category, String name, Class<T> type, T[] options, T defval, String description) {
        this.category = category;
        this.name = name;
        this.longName = "jruby." + name;
        this.type = type;
        this.options = options == null ? new String[]{type.getSimpleName()} : options;
        this.defval = defval;
        this.description = description;
        this.specified = false;
    }

    @Override
    public String toString() {
        return longName;
    }

    public String loadProperty() {
        String value = SafePropertyAccessor.getProperty(longName);

        if (value != null) specified = true;

        return value;
    }

    public boolean isSpecified() {
        return specified;
    }

    public abstract T load();

    public final Category category;
    public final String name;
    public final String longName;
    public final Class type;
    public final Object[] options;
    public final T defval;
    public final String description;
    private boolean specified;
}
