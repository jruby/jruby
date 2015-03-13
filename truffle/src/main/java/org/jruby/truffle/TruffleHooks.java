/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle;

import org.jruby.TruffleHooksStub;
import org.jruby.truffle.runtime.RubyContext;

public interface TruffleHooks extends TruffleHooksStub {

    void afterInit(RubyContext context);

}
