/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby;

import java.io.InputStream;

public interface TruffleRubyEngineInterface {

    int execute(String path);

    void dispose();

    int doCheckSyntax(InputStream in, String filename);

}
