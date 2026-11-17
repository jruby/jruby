/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast;

public class WhenOneArgNode extends WhenNode {
    public WhenOneArgNode(int line, Node expressionNode, Node bodyNode, Node nextCase) {
        super(line, expressionNode, bodyNode, nextCase);
    }
}
