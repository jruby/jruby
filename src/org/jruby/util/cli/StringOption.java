package org.jruby.util.cli;

import org.jruby.util.SafePropertyAccessor;

/**
 * A String-based Option.
 */
public class StringOption extends Option<String> {
    public StringOption(Category category, String name, String[] options, String defval, String description) {
        super(category, name, String.class, options, defval, description);
    }

    public String load() {
        return SafePropertyAccessor.getProperty("jruby." + name, defval);
    }
}