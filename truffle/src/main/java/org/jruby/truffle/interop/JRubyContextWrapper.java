/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.interop;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import org.jruby.Ruby;

public class JRubyContextWrapper implements TruffleObject {

    private final Ruby ruby;

    public JRubyContextWrapper(Ruby ruby) {
        this.ruby = ruby;
    }

    public Ruby getRuby() {
        return ruby;
    }

    @Override
    public ForeignAccess getForeignAccess() {
        throw new UnsupportedOperationException();
    }

}
