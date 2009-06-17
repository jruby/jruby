package org.jruby.compiler.ir;

public class IR_Script extends IR_BaseContext
{
        // SSS FIXME: Should this be a string literal or a string?
        // Need to check with headius
    StringLiteral _fileName;

    public StringLiteral getFileName() { return _fileName; }
}
