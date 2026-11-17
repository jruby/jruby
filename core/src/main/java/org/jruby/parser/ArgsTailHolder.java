package org.jruby.parser;

import org.jruby.ast.BlockArgNode;
import org.jruby.ast.KeywordRestArgNode;
import org.jruby.ast.ListNode;

/**
 * Simple struct to hold values until they can be inserted into the AST.
 */
public class ArgsTailHolder {
    private final int line;
    private final BlockArgNode blockArg;
    private final ListNode keywordArgs;
    private final KeywordRestArgNode keywordRestArg;
    
    public ArgsTailHolder(int line, ListNode keywordArgs,
            KeywordRestArgNode keywordRestArg, BlockArgNode blockArg) {
        this.line = line;
        this.blockArg = blockArg;
        this.keywordArgs = keywordArgs;
        this.keywordRestArg = keywordRestArg;
    }
    
    public int getLine() {
        return line;
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
