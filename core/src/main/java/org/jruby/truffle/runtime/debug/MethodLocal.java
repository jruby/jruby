/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.debug;

import org.jruby.truffle.runtime.methods.*;

/**
 * A method and local variable identifier tuple.
 */
public class MethodLocal {

    private final UniqueMethodIdentifier method;
    private final String local;

    public MethodLocal(UniqueMethodIdentifier method, String local) {
        super();
        this.method = method;
        this.local = local;
    }

    @Override
    public String toString() {
        return "MethodLocal [method=" + method + ", local=" + local + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((local == null) ? 0 : local.hashCode());
        result = prime * result + ((method == null) ? 0 : method.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof MethodLocal)) {
            return false;
        }
        MethodLocal other = (MethodLocal) obj;
        if (local == null) {
            if (other.local != null) {
                return false;
            }
        } else if (!local.equals(other.local)) {
            return false;
        }
        if (method == null) {
            if (other.method != null) {
                return false;
            }
        } else if (!method.equals(other.method)) {
            return false;
        }
        return true;
    }

}
