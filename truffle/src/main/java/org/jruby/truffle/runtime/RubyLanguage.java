/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.source.Source;

import java.io.IOException;

public class RubyLanguage extends TruffleLanguage {

    private final RubyContext context;

    public RubyLanguage(Env env, RubyContext context) {
        super(env);
        this.context = context;
    }

    @Override
    protected Object eval(Source source) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Object findExportedSymbol(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Object getLanguageGlobal() {
        return context.getCoreLibrary().getObjectClass();
    }

    @Override
    protected boolean isObjectOfLanguage(Object o) {
        throw new UnsupportedOperationException();
    }

}
