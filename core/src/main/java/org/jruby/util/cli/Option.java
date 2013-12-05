/*
 **** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Represents a single option, with a category, name, value type,
 * options, default value, and description.
 *
 * This type should be subclassed for specific types of values.
 *
 * @param <T> the type of value associated with the option
 */
public abstract class Option<T> {
    /**
     * Create a new option with the given values.
     * 
     * @param <C> an enumeration type
     * @param prefix the prefix used for loading this option from properties
     * @param name the rest of the property name
     * @param type the value type of the option
     * @param category the category to which this option belongs
     * @param options a list of supported for the option, or null if the set is
     *                not applicable
     * @param defval the default value for the option
     * @param description a description for the option
     */
    public Option(String prefix, String name, Class<T> type, Enum category, T[] options, T defval, String description) {
        this.category = category;
        this.prefix = prefix;
        this.name = name;
        this.longName = prefix + "." + name;
        this.displayName = name;
        this.type = type;
        this.options = options == null ? new String[]{type.getSimpleName()} : options;
        this.defval = defval;
        this.description = description;
        this.specified = false;
    }
    
    /**
     * Create a new option with the given values.
     * 
     * @param <C> an enumeration type
     * @param longName the property name
     * @param type the value type of the option
     * @param category the category to which this option belongs
     * @param options a list of supported for the option, or null if the set is
     *                not applicable
     * @param defval the default value for the option
     * @param description a description for the option
     */
    public Option(String longName, Class<T> type, Enum category, T[] options, T defval, String description) {
        this.category = category;
        this.prefix = null;
        this.name = null;
        this.longName = longName;
        this.displayName = longName;
        this.type = type;
        this.options = options == null ? new String[]{type.getSimpleName()} : options;
        this.defval = defval;
        this.description = description;
        this.specified = false;
    }
    
    public static Option<String> string(String prefix, String name, Enum category, String description) {
        return new StringOption(prefix, name, category, null, null, description);
    }
    
    public static Option<String> string(String longName, Enum category, String description) {
        return new StringOption(longName, category, null, null, description);
    }
    
    public static Option<String> string(String prefix, String name, Enum category, String defval, String description) {
        return new StringOption(prefix, name, category, null, defval, description);
    }
    
    public static Option<String> string(String longName, Enum category, String defval, String description) {
        return new StringOption(longName, category, null, defval, description);
    }
    
    public static Option<String> string(String prefix, String name, Enum category, String[] options, String description) {
        return new StringOption(prefix, name, category, options, null, description);
    }
    
    public static Option<String> string(String longName, Enum category, String[] options, String description) {
        return new StringOption(longName, category, options, null, description);
    }
    
    public static Option<String> string(String prefix, String name, Enum category, String[] options, String defval, String description) {
        return new StringOption(prefix, name, category, options, defval, description);
    }
    
    public static Option<String> string(String longName, Enum category, String[] options, String defval, String description) {
        return new StringOption(longName, category, options, defval, description);
    }
    
    public static Option<Boolean> bool(String prefix, String name, Enum category, String description) {
        return new BooleanOption(prefix, name, category, null, description);
    }
    
    public static Option<Boolean> bool(String longName, Enum category, String description) {
        return new BooleanOption(longName, category, null, description);
    }
    
    public static Option<Boolean> bool(String prefix, String name, Enum category, Boolean defval, String description) {
        return new BooleanOption(prefix, name, category, defval, description);
    }
    
    public static Option<Boolean> bool(String longName, Enum category, Boolean defval, String description) {
        return new BooleanOption(longName, category, defval, description);
    }
    
    public static Option<Integer> integer(String prefix, String name, Enum category, String description) {
        return new IntegerOption(prefix, name, category, null, description);
    }
    
    public static Option<Integer> integer(String longName, Enum category, String description) {
        return new IntegerOption(longName, category, null, description);
    }
    
    public static Option<Integer> integer(String prefix, String name, Enum category, Integer defval, String description) {
        return new IntegerOption(prefix, name, category, defval, description);
    }
    
    public static Option<Integer> integer(String longName, Enum category, Integer defval, String description) {
        return new IntegerOption(longName, category, defval, description);
    }
    
    public static Option<Integer> integer(String prefix, String name, Enum category, Integer[] options, Integer defval, String description) {
        return new IntegerOption(prefix, name, category, options, defval, description);
    }
    
    public static Option<Integer> integer(String longName, Enum category, Integer[] options, Integer defval, String description) {
        return new IntegerOption(longName, category, options, defval, description);
    }
    
    public static <T extends Enum<T>> Option<T> enumeration(String prefix, String name, Enum category, Class<T> enumClass, String description) {
        return new EnumerationOption(prefix, name, category, enumClass, null, description);
    }
    
    public static <T extends Enum<T>> Option<T> enumeration(String longName, Enum category, Class<T> enumClass, String description) {
        return new EnumerationOption(longName, category, enumClass, null, description);
    }
    
    public static <T extends Enum<T>> Option<T> enumeration(String prefix, String name, Enum category, Class<T> enumClass, T defval, String description) {
        return new EnumerationOption(prefix, name, category, enumClass, defval, description);
    }
    
    public static <T extends Enum<T>> Option<T> enumeration(String longName, Enum category, Class<T> enumClass, T defval, String description) {
        return new EnumerationOption(longName, category, enumClass, defval, description);
    }
    
    public static String formatValues(Collection<Option> options) {
        StringBuilder sb = new StringBuilder();
        List<Option> sorted = new ArrayList<Option>(options);
        Collections.sort(sorted, OptionComparator);
        
        Enum category = null;
        for (Option option : sorted) {
            if (category != option.category) {
                category = option.category;
                sb.append('\n').append(category).append('\n');
            }
            sb
                    .append(option.displayName)
                    .append('=')
                    .append(encodeWhitespace(option.load()))
                    .append('\n');
        }
        return sb.toString();
    }

    public static String formatOptions(Collection<Option> options) {
        StringBuilder sb = new StringBuilder();
        List<Option> sorted = new ArrayList<Option>(options);
        Collections.sort(sorted, OptionComparator);
        
        Enum category = null;
        for (Option option : sorted) {
            if (category != option.category) {
                category = option.category;
                sb.append("\n################################################################################");
                sb.append("\n# ").append(category);
                sb.append("\n################################################################################\n\n");
            }
            sb.append("# ").append(option.description).append('\n');
            
            sb.append("# Options: ").append(Arrays.toString(option.options)).append(", Default: ").append(encodeWhitespace(option.defval)).append(".\n");
            
            sb.append("\n#");
            sb.append(option.displayName).append('=').append(outputForValue(option.load()));
            
            sb.append("\n\n");
        }

        return sb.toString();
    }
    
    private static Comparator<Option> OptionComparator = new Comparator<Option>() {
        public int compare(Option o1, Option o2) {
            int catComp = o1.category.compareTo(o2.category);
            if (catComp != 0) return catComp;
            return o1.displayName.compareTo(o2.displayName);
        }
    };
    
    private static String encodeWhitespace(Object obj) {
        if (obj == null) return "null";
        
        String str = obj.toString();
        StringBuilder sb = new StringBuilder(str.length() * 2);
        
        boolean hasWhitespace = false;
        
        for (char c : str.toCharArray()) {
            if (Character.isWhitespace(c)) {
                hasWhitespace = true;
                switch (c) {
                    case '\t':
                        sb.append("\\t");
                        break;
                    case '\n':
                        sb.append("\\n");
                        break;
                    default:
                        sb.append(c);
                        break;
                }
            } else {
                sb.append(c);
            }
        }
        
        if (hasWhitespace) {
            return "\"" + sb.toString() + "\"";
        } else {
            return str;
        }
    }
    
    private static String outputForValue(Object obj) {
        if (obj == null) {
            return "";
        } else {
            return encodeWhitespace(obj);
        }
    }

    @Override
    public String toString() {
        return longName;
    }

    public String loadProperty() {
        String value = null;
        try {
            value = System.getProperty(longName);
        } catch (SecurityException se) {
        }
        
        if (value != null) specified = true;

        return value;
    }

    public boolean isSpecified() {
        return specified;
    }

    public T load() {
        if (this.value != null) return value;
        
        value = reload();
        
        return value;
    }
    
    public abstract T reload();

    public final Enum category;
    public final String prefix;
    public final String name;
    public final String longName;
    public final String displayName;
    public final Class<T> type;
    public final Object[] options;
    public final T defval;
    public final String description;
    public T value;
    private boolean specified;
}
