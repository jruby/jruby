package org.jruby.util.cli;

import org.jruby.util.SafePropertyAccessor;

/**
 * A Boolean-based Option.
 */
public class BooleanOption extends Option<Boolean> {
    public BooleanOption(Category category, String name, Boolean defval, String description) {
        super(category, name, Boolean.class, new Boolean[] {true, false}, defval, description);
    }

    public Boolean load() {
        return SafePropertyAccessor.getBoolean("jruby." + name, defval);
    }
}