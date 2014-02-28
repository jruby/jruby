/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.objectstorage;

import org.jruby.truffle.runtime.objectstorage.ObjectStorage;

/**
 * A hook that allows us to perform custom behaviour when respecializing a object field node because whatever
 * assumptions we had made were not correct.
 */
public interface RespecializeHook {

    void hookRead(ObjectStorage object, String name);

    void hookWrite(ObjectStorage object, String name, Object value);

}
