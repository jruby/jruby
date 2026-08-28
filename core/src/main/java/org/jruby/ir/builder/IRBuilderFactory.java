package org.jruby.ir.builder;

import org.jcodings.Encoding;
import org.jruby.ParseResult;
import org.jruby.ir.IRManager;
import org.jruby.ir.IRScope;

public class IRBuilderFactory {
    public IRBuilder newIRBuilder(IRManager manager, IRScope newScope, IRBuilder parent, Encoding encoding) {
        return new IRBuilderAST(manager, newScope, parent, null, encoding);
    }

    // For BEGIN processing
    public IRBuilder newIRBuilder(IRManager manager, IRScope newScope, IRBuilder parent, IRBuilder variableBuilder, Encoding encoding) {
        return new IRBuilderAST(manager, newScope, parent, variableBuilder, encoding);
    }

    public IRBuilder topIRBuilder(IRManager manager, IRScope newScope, ParseResult rootNode) {
        return new IRBuilderAST(manager, newScope, null, null, null);
    }
}
