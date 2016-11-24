/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle;

import org.jruby.JRubyTruffleInterface;
import org.jruby.RubyInstanceConfig;

import java.io.InputStream;

public class JRubyTruffleImpl implements JRubyTruffleInterface {

    private final RubyEngine engine;

    // Created by reflection from Ruby#loadTruffle

    public JRubyTruffleImpl(RubyInstanceConfig instanceConfig) {
        engine = new RubyEngine(instanceConfig);
    }

    @Override
    public int execute(String path) {
        return engine.execute(path);
    }

    @Override
    public int doCheckSyntax(InputStream in, String filename) {
        return engine.doCheckSyntax(in, filename);
    }

    @Override
    public void dispose() {
        engine.dispose();
    }
    
}
