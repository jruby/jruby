/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.control;

import com.oracle.truffle.api.nodes.ControlFlowException;

/**
 * Controls breaking out of all executions and ending Ruby execution.
 */
public final class QuitException extends ControlFlowException {

    private static final long serialVersionUID = -3568511099628564190L;

}
