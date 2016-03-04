/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.language.parser.jruby;

import com.oracle.truffle.api.source.SourceSection;

/**
 * Some 3rd party code make assumptions about the JRuby runtime based upon the values in some constants.  Where the
 * JRuby+Truffle runtime diverges from the behavior of JRuby, we may need to replace the value of these constants
 * in order to lead the code down a different branch.
 */
public class ConstantReplacer {

    public static String replacementName(SourceSection sourceSection, String name) {
        // The thread_safe gem checks if JRUBY_VERSION is defined and then uses RUBY_VERSION as
        // a fallback.  We rename the constant being looked for to one that doesn't exist so the defined?
        // lookup fails.
        if (sourceSection.getSource().getName().endsWith("thread_safe.rb")) {
            if (name.equals("JRUBY_VERSION") || name.equals("RUBY_VERSION")) {
                return name + "_NONEXISTENT";
            }
        }

        return name;
    }

}
