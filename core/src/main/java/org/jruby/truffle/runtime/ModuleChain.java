/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyModule;
import org.jruby.truffle.runtime.methods.RubyMethod;

import java.util.Map;

public interface ModuleChain {

    ModuleChain getParentModule();

    RubyModule getLexicalParentModule();

    RubyModule getActualModule();

    Map<String, RubyConstant> getConstants();

    Map<String, RubyMethod> getMethods();

    Map<String, Object> getClassVariables();

    RubyContext getContext();

    RubyClass getSingletonClass(Node currentNode);

    void newVersion();

    void addDependent(ModuleChain dependent);

    Assumption getUnmodifiedAssumption();

}
