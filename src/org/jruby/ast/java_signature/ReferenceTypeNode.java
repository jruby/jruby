/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast.java_signature;

/**
 *
 * @author enebo
 */
public class ReferenceTypeNode extends TypeNode {
    public ReferenceTypeNode(String name) {
        super(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
