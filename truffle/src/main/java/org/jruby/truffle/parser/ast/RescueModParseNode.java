package org.jruby.truffle.parser.ast;

import org.jruby.truffle.language.RubySourceSection;

/**
 * f rescue nil
 */
public class RescueModParseNode extends RescueParseNode {
    public RescueModParseNode(RubySourceSection position, ParseNode bodyNode, RescueBodyParseNode rescueNode) {
        super(position, bodyNode, rescueNode, null /* else */);
    }
}
