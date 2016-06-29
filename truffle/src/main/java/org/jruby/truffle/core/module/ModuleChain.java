/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.module;

import com.oracle.truffle.api.object.DynamicObject;

/**
 * Either an IncludedModule, a RubyClass or a RubyModule.
 * Private interface, do not use outside RubyModule.
 */
public interface ModuleChain {

    ModuleChain getParentModule();

    DynamicObject getActualModule();

    void insertAfter(DynamicObject module);

}
