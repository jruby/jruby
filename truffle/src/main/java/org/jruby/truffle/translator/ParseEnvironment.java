/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.translator;

import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.ReturnID;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.translator.TranslatorEnvironment.BreakID;

/**
 * Translator environment, unique per parse/translation.
 */
public class ParseEnvironment {

    public ParseEnvironment(RubyContext context) {
    }

    public ReturnID allocateReturnID() {
        return new ReturnID();
    }

    public BreakID allocateBreakID() {
        return new BreakID();
    }

}
