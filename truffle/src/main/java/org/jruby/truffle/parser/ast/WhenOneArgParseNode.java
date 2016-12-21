/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.truffle.parser.ast;

import org.jruby.truffle.language.RubySourceSection;

/**
 *
 * @author enebo
 */
public class WhenOneArgParseNode extends WhenParseNode {
    public WhenOneArgParseNode(RubySourceSection position, ParseNode expressionNode, ParseNode bodyNode, ParseNode nextCase) {
        super(position, expressionNode, bodyNode, nextCase);
    }
}
