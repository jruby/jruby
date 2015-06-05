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
import org.jruby.Ruby;
import org.jruby.truffle.runtime.core.RubyBasicObject;

@TruffleLanguage.Registration(name = "Ruby", mimeType = "application/x-ruby")
public class RubyLanguage extends TruffleLanguage {

    private final RubyContext context;

    public RubyLanguage(Env env) {
        super(env);
        Ruby r = Ruby.newInstance();
        this.context = new RubyContext(r, env);
    }

    @Override
    protected Object eval(Source source) throws IOException {
        try {
            return context.eval(source);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    protected Object findExportedSymbol(String s, boolean b) {
        return context.findExportedObject(s);
    }

    @Override
    protected Object getLanguageGlobal() {
        return context.getCoreLibrary().getObjectClass();
    }

    @Override
    protected boolean isObjectOfLanguage(Object o) {
        return o instanceof RubyBasicObject;
    }

}
