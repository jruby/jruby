package org.jruby.parser;

import org.jruby.ast.BlockArgNode;
import org.jruby.lexer.yacc.ISourcePosition;

/**
 * Simple struct to hold values until they can be inserted into the AST.
 */
public class ArgsTailHolder {
    private ISourcePosition position;
    private BlockArgNode blockArg;
    
    public ArgsTailHolder(ISourcePosition position, BlockArgNode blockArg) {
        this.position = position;
        this.blockArg = blockArg;
    }
    
    public ISourcePosition getPosition() {
        return position;
    }
    
    public BlockArgNode getBlockArg() {
        return blockArg;
    }
}
