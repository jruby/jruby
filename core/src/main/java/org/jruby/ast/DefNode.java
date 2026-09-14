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
     * Column (zero-offset, in bytes) where the source of this definition starts (e.g. at the 'def' keyword or
     * the opening brace of a block).  -1 if unknown.
     * @return the column
     */
    int getStartColumn();

    /**
     * Column (zero-offset, in bytes) just past the end of the source of this definition (e.g. after the 'end'
     * keyword or the closing brace of a block).  -1 if unknown.
     * @return the column
     */
    int getEndColumn();
}
