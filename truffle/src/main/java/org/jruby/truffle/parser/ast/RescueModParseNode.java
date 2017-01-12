package org.jruby.truffle.parser.ast;

import org.jruby.truffle.language.SourceIndexLength;

/**
 * f rescue nil
 */
public class RescueModParseNode extends RescueParseNode {
    public RescueModParseNode(SourceIndexLength position, ParseNode bodyNode, RescueBodyParseNode rescueNode) {
        super(position, bodyNode, rescueNode, null /* else */);
    }
}
