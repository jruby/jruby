/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 *
 * Some of the code in this class is modified from org.jruby.util.StringSupport,
 * licensed under the same EPL1.0/GPL 2.0/LGPL 2.1 used throughout.
 */
package org.jruby.truffle.aot;

import org.jcodings.exception.InternalException;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

// Checkstyle: stop

final class Target_org_jcodings_Encoding {
    static org.jcodings.Encoding load(String name) {
        JRubySupport.EncodingInstance instance = JRubySupport.allEncodings.get(name);
        if (instance == null) {
            throw new InternalException(org.jcodings.exception.ErrorMessages.ERR_ENCODING_CLASS_DEF_NOT_FOUND, "org.jcodings.specific." + name + "Encoding");
        }
        return instance.get(false);
    }
}

final class Target_org_jcodings_util_ArrayReader {
    static DataInputStream openStream(String name) {
        byte[] table = JRubySupport.allJCodingsTables.get(name);
        if (table == null) {
            throw new InternalException(("entry: /tables/" + name + ".bin not found"));
        }
        return new DataInputStream(new ByteArrayInputStream(table));
    }
}

/** Dummy class to have a class with the file's name. */
public final class JRubySubstitutions {
}
