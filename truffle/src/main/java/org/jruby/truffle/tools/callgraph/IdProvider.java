/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.tools.callgraph;

import java.util.IdentityHashMap;
import java.util.Map;

public class IdProvider {

    private long nextId = 0;
    private final Map<Object, Long> ids = new IdentityHashMap<Object, Long>();

    public synchronized long getId(Object object) {
        Long id = ids.get(object);

        if (id == null) {
            id = nextId++;
            ids.put(object, id);
        }

        return id;
    }


}
