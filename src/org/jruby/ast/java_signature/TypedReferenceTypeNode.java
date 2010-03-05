/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast.java_signature;

/**
 *
 * @author enebo
 */
public class TypedReferenceTypeNode extends ReferenceTypeNode {
    protected Object typeParameter;

    public TypedReferenceTypeNode(String typeName, Object typeParameter) {
        super(typeName);

        this.typeParameter = typeParameter;
    }

    @Override
    public boolean isTyped() {
        return true;
    }
}
