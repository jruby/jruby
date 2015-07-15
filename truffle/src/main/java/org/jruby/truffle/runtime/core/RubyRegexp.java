/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import org.joni.Regex;
import org.jruby.util.ByteList;
import org.jruby.util.RegexpOptions;

/**
 * Represents the Ruby {@code Regexp} class.
 */
@Deprecated
public class RubyRegexp extends RubyBasicObject {

    // TODO(CS): not sure these compilation finals are correct - are they needed anyway?
    @CompilationFinal public Regex regex;
    @CompilationFinal public ByteList source;
    @CompilationFinal public RegexpOptions options = RegexpOptions.NULL_OPTIONS;
    public Object cachedNames;

    public RubyRegexp(RubyClass regexpClass) {
        super(regexpClass);
    }

}
