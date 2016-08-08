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
}
