package org.jruby.ast;

import org.jruby.parser.ProductionState;

/**
 * A stretch of source that is not a node of its own, such as the parentheses around an expression or the
 * statement of a modifier loop: the zero-based line and byte column of its first character and the line and
 * column just past its last. Like a SourcePosition, a node only refers to one when the parser records positions.
 */
public record SourceSpan(int startLine, int startColumn, int endLine, int endColumn) {
    /**
     * @param start where the span starts, as the parser packs positions (see ProductionState)
     * @param end where it ends
     */
    public static SourceSpan of(long start, long end) {
        return new SourceSpan(ProductionState.line(start), ProductionState.column(start),
                ProductionState.line(end), ProductionState.column(end));
    }
}
