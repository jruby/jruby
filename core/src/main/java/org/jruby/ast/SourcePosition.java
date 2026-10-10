package org.jruby.ast;

import org.jruby.parser.ProductionState;

/**
 * A place in the source: a zero-based line and a byte column on it. Nodes refer to one for each place Coverage
 * reports besides their own span, and only when the parser records positions (see
 * RubyParserBase#recordsPositions), so that an AST parsed without Coverage does not carry them.
 */
public record SourcePosition(int line, int column) {
    /**
     * @param packed a position as the parser packs it (see ProductionState), negative for none
     * @return the position, or null for none
     */
    public static SourcePosition of(long packed) {
        return packed < 0 ? null : new SourcePosition(ProductionState.line(packed), ProductionState.column(packed));
    }
}
