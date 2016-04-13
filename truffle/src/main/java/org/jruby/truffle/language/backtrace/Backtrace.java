/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.backtrace;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Backtrace {

    private final Activation[] activations;
    private final Throwable javaThrowable;

    public Backtrace(Activation[] activations, Throwable javaThrowable) {
        this.activations = activations;
        this.javaThrowable = javaThrowable;
    }

    public List<Activation> getActivations() {
        return Collections.unmodifiableList(Arrays.asList(activations));
    }

    public Throwable getJavaThrowable() {
        return javaThrowable;
    }

}
