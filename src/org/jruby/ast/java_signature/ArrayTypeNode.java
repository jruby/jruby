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
    private final TypeNode typeForArray;

    public ArrayTypeNode(TypeNode typeForArray) {
        super(null);

        this.typeForArray = typeForArray;
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
