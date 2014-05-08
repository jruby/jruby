/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.constants;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import org.jruby.truffle.runtime.core.RubyBasicObject;

public abstract class ReadConstantChainNode extends Node {

    abstract public Object execute(RubyBasicObject receiver);

    abstract public boolean executeBoolean(RubyBasicObject receiver) throws UnexpectedResultException;

    abstract public int executeIntegerFixnum(RubyBasicObject receiver) throws UnexpectedResultException;

    abstract public long executeLongFixnum(RubyBasicObject receiver) throws UnexpectedResultException;

    abstract public double executeFloat(RubyBasicObject receiver) throws UnexpectedResultException;

}
