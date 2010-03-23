/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast.java_signature;

/**
 *
 * @author enebo
 */
public class ArrayTypeNode extends ReferenceTypeNode {
    protected TypeNode typeForArray;

    public ArrayTypeNode() {
        super(null);
    }

    public ArrayTypeNode(TypeNode typeForArray) {
        this();

        this.typeForArray = typeForArray;
    }

    public void setTypeForArray(TypeNode referenceType) {
        // This may be a chain of [][][] arrays.  We want to set in last in chain.
        if (typeForArray != null && typeForArray instanceof ArrayTypeNode) {
            ((ArrayTypeNode) typeForArray).setTypeForArray(referenceType);
        } else {
            this.typeForArray = referenceType;
        }
    }

    @Override
    public String getName() {
        return typeForArray.getName() + "[]";
    }

    @Override
    public String getFullyTypedName() {
        return typeForArray.getFullyTypedName() + "[]";
    }

    @Override
    public boolean isArray() {
        return true;
    }
}
