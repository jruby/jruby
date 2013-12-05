package org.jruby.parser;

import org.jruby.ast.BlockArgNode;
import org.jruby.ast.KeywordRestArgNode;
import org.jruby.ast.ListNode;
import org.jruby.lexer.yacc.ISourcePosition;

/**
 * Simple struct to hold values until they can be inserted into the AST.
 */
public class ArgsTailHolder {
    private ISourcePosition position;
    private BlockArgNode blockArg;
    private ListNode keywordArgs;
    private KeywordRestArgNode keywordRestArg;
    
    public ArgsTailHolder(ISourcePosition position, ListNode keywordArgs,
            KeywordRestArgNode keywordRestArg, BlockArgNode blockArg) {
        this.position = position;
        this.blockArg = blockArg;
        this.keywordArgs = keywordArgs;
        this.keywordRestArg = keywordRestArg;
    }
    
    public ISourcePosition getPosition() {
        return position;
    }
    
    public BlockArgNode getBlockArg() {
        return blockArg;
    }
    
    public ListNode getKeywordArgs() {
        return keywordArgs;
    }
    
    public KeywordRestArgNode getKeywordRestArgNode() {
        return keywordRestArg;
    }
    
    /**
     * Does this holder support either keyword argument types
     */
    public boolean hasKeywordArgs() {
        return keywordArgs != null || keywordRestArg != null;
    }
}
