package org.jruby.ast;

import org.jruby.parser.StaticScope;

/**
 * Methods and blocks both implement these.
 */
public interface DefNode {
    /**
     * Gets the argsNode.
     * @return Returns a Node
     */
    ArgsNode getArgsNode();

    /**
     * Get the static scoping information.
     *
     * @return the scoping info
     */
    StaticScope getScope();

    /**
     * Gets the body of this class.
     *
     * @return the contents
     */
    Node getBodyNode();


    /**
     * Which line if the end keyword located
     * @return the line (zero-offset)
     */
    int getEndLine();

    /**
     * Zero-based byte column where the source of this definition starts: the def keyword, or the opening brace
     * of a block. -1 if unknown.
     * @return the column
     */
    int getStartColumn();

    /**
     * Zero-based byte column just after the source of this definition ends: after the end keyword, or after
     * the closing brace of a block. -1 if unknown.
     * @return the column
     */
    int getEndColumn();
}
