/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.truffle.parser.ast;

import org.jruby.truffle.language.SourceIndexLength;

/**
 *
 * @author enebo
 */
public class Colon2ConstParseNode extends Colon2ParseNode {
    public Colon2ConstParseNode(SourceIndexLength position, ParseNode leftNode, String name) {
        super(position, leftNode, name);

        assert leftNode != null: "Colon2ConstParseNode cannot have null leftNode";
    }
}
