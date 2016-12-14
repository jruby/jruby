/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.interop;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.SnippetNode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ForeignCodeNode extends RubyNode {

    private final DynamicObject mimeType;
    private final DynamicObject code;
    private final DynamicObject name;

    @Child private SnippetNode snippetNode = new SnippetNode();

    private static final Pattern NAME_PATTERN = Pattern.compile(".*function\\s+(\\w+)\\s*\\(.*", Pattern.DOTALL);

    public ForeignCodeNode(RubyContext context, String mimeType, String code) {
        final Matcher matcher = NAME_PATTERN.matcher(code);
        matcher.find();
        final String functionName = matcher.group(1);
        this.mimeType = StringOperations.createString(context, StringOperations.encodeRope(mimeType, UTF8Encoding.INSTANCE));
        this.code = StringOperations.createString(context, StringOperations.encodeRope(code + "\nInterop.export('" + functionName + "', " + functionName +  ".bind(this));", UTF8Encoding.INSTANCE));
        this.name = context.getSymbolTable().getSymbol(functionName);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        snippetNode.execute(frame, "Truffle::Interop.eval mimeType, code; Truffle::Interop.import_method name",
                "mimeType", mimeType,
                "code", code,
                "name", name);
        return nil();
    }

}
