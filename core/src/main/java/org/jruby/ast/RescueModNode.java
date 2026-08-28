package org.jruby.ast;

/**
 * f rescue nil
 */
public class RescueModNode extends RescueNode {
    public RescueModNode(int line, Node bodyNode, RescueBodyNode rescueNode) {
        super(line, bodyNode, rescueNode, null /* else */);
    }
}
