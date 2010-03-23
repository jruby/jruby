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
    private String genericString = "";

    public ReferenceTypeNode(String name) {
        super(name);
    }

    public void setGenericString(String genericString) {
        this.genericString = genericString;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof ReferenceTypeNode)) return false;

        return genericString.equals(((ReferenceTypeNode) other).genericString) &&
                super.equals(other);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + (this.genericString != null ? this.genericString.hashCode() : 0);
        return hash;
    }

    @Override
    public String getFullyTypedName() {
        return getName() + genericString;
    }

    public void setGenericsTyping(String genericString) {
        this.genericString = genericString;
    }
}
