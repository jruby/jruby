package org.jruby.ast;

/**
 * What a node keeps of where it is in the source (see Node#setAutoSourceSpan), apart from the node so that an AST
 * parsed without Coverage carries one null reference per node rather than these fields, whatever its class.
 *
 * @param startLine zero-based, like endLine; only recorded when it differs from the node's line (e.g. fixpos
 *                  moves a modifier's line to its condition), else -1
 * @param startColumn a byte column, like endColumn; -1 when unknown
 * @param endLine the line of the position just past the node's last character, -1 when unknown
 * @param endColumn the column of that position
 * @param locked set explicitly: productions passing the node along no longer widen the span
 * @param parens the parentheses written around the node, if any
 * @param detail what else the node's class records of its source (see Node#setSourceDetail)
 */
record NodeSpan(int startLine, int startColumn, int endLine, int endColumn, boolean locked, SourceSpan parens,
                Record detail) {
    static final NodeSpan NONE = new NodeSpan(-1, -1, -1, -1, false, null, null);

    NodeSpan withSpan(int startLine, int startColumn, int endLine, int endColumn, boolean locked) {
        return new NodeSpan(startLine, startColumn, endLine, endColumn, locked, parens, detail);
    }

    NodeSpan withParens(SourceSpan parens) {
        return new NodeSpan(startLine, startColumn, endLine, endColumn, locked, parens, detail);
    }

    NodeSpan withDetail(Record detail) {
        return new NodeSpan(startLine, startColumn, endLine, endColumn, locked, parens, detail);
    }
}
