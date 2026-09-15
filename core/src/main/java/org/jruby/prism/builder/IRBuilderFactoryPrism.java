package org.jruby.prism.builder;

import org.jcodings.Encoding;
import org.jruby.ParseResult;
import org.jruby.ir.IRManager;
import org.jruby.ir.IRScope;
import org.jruby.ir.builder.IRBuilder;
import org.jruby.ir.builder.IRBuilderFactory;

public class IRBuilderFactoryPrism extends IRBuilderFactory {
    public IRBuilder newIRBuilder(IRManager manager, IRScope newScope, IRBuilder parent, Encoding encoding) {
        return new IRBuilderPrism(manager, newScope, parent, null, encoding);
    }

    // For BEGIN processing
    public IRBuilder newIRBuilder(IRManager manager, IRScope newScope, IRBuilder parent, IRBuilder variableBuilder, Encoding encoding) {
        return new IRBuilderPrism(manager, newScope, parent, variableBuilder, encoding);
    }

    public IRBuilder topIRBuilder(IRManager manager, IRScope newScope, ParseResult rootNode) {
        return new IRBuilderPrism(manager, newScope, null, null, rootNode.getEncoding());
    }
}
