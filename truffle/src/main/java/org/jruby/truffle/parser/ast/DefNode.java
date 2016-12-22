package org.jruby.truffle.parser.ast;

import org.jruby.truffle.parser.scope.StaticScope;

/**
 * Methods and blocks both implement these.
 */
public interface DefNode {
    /**
     * Gets the argsNode.
     * @return Returns a ParseNode
     */
    ArgsParseNode getArgsNode();

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
    ParseNode getBodyNode();

}
