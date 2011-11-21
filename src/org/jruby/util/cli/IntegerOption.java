/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.util.cli;

import org.jruby.util.SafePropertyAccessor;

/**
 * An Integer-based Option.
 */
public class IntegerOption extends Option<Integer> {
    public IntegerOption(Category category, String name, Integer defval, String description) {
        super(category, name, Integer.class, null, defval, description);
    }

    public Integer load() {
        return SafePropertyAccessor.getInt("jruby." + name, defval);
    }
}
