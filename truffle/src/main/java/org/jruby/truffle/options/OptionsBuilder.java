/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.options;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class OptionsBuilder {

    private final String LEGACY_PREFIX = "jruby.truffle.";

    private final Map<OptionDescription, Object> options = new HashMap<>();

    public void set(Properties properties) {
        for (Map.Entry<Object, Object> property : properties.entrySet()) {
            final String name = (String) property.getKey();

            if (name.startsWith(LEGACY_PREFIX)) {
                set(name.substring(LEGACY_PREFIX.length()), property.getValue());
            }
        }
    }

    public void set(Map<String, Object> properties) {
        for (Map.Entry<String, Object> property : properties.entrySet()) {
            set(property.getKey(), property.getValue());
        }
    }

    private void set(String name, Object value) {
        final OptionDescription description = OptionsCatalogue.fromName(name);

        if (description == null) {
            //throw new UnsupportedOperationException(name);

            // Don't throw for now - not all the options are transalted across
            return;
        }

        options.put(description, description.checkValue(value));
    }

    public NewOptions build() {
        return new NewOptions(this);
    }

    <T> T getOrDefault(OptionDescription description) {
        Object value = options.get(description);

        if (value == null) {
            value = description.getDefaultValue();
        }

        return (T) value;
    }

}
