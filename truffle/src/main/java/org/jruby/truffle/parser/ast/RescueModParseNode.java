package org.jruby.truffle.parser.ast;

import org.jruby.truffle.parser.TempSourceSection;

/**
 * f rescue nil
 */
public class RescueModParseNode extends RescueParseNode {
    public RescueModParseNode(TempSourceSection position, ParseNode bodyNode, RescueBodyParseNode rescueNode) {
        super(position, bodyNode, rescueNode, null /* else */);
    }
}
