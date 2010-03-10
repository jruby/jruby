/**
 * **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2009 Yoko Harada <yokolet@gmail.com>
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
 * **** END LICENSE BLOCK *****
 */
package org.jruby.embed.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PropertyResourceBundle;

/**
 * Reader of a property file that describes a container/engine info.
 * 
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class PropertyReader {
    private final Map<String, String[]> properties;
    
    public PropertyReader(String propertyname) {
        try {
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            PropertyResourceBundle resource =
                    new PropertyResourceBundle(classloader.getResourceAsStream(propertyname));
            Enumeration<String> keys = resource.getKeys();
            Map map = new HashMap<String, String[]>();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                String[] values = resource.getString(key).split(",\\s*");
                for (int i = 0; i < values.length; i++) {
                    values[i] = values[i].trim();
                }
                map.put(key, values);
            }
            properties = Collections.unmodifiableMap(map);
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Returns an array of values associated to a key.
     *
     * @param key is a key in a property file
     * @return values associated to the key
     */
    public String[] getProperty(String key) {
        return properties.get(key);
    }

    /**
     * Returns a String when a given key is supposed to be associated to only one
     * value.
     *
     * @param key String name of a key.
     * @return the value associated to the given key.
     */
    public String getSingleValue(String key) {
        String[] array = getProperty(key);
        if (array == null) {
            throw new NullPointerException(key + "is not defined");
        }
        return array[0];
    }

    /**
     * Returns an array of String when a given key is supposed to be associated to
     * multiple values.
     *
     * @param key String name of a key.
     * @return the value associated to the given key.
     */
    public List getMultipleValue(String key) {
        String[] array = getProperty(key);
        if (array == null) {
            throw new NullPointerException(key + "is not defined");
        }
        List list = new ArrayList();
        for (String s : array) {
            list.add(s);
        }
        return list;
    }
}
