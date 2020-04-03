/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ast.java_signature;

/**
 *
 * @author enebo
 */
public class BoolLiteral implements Literal {
    private boolean value;
    
    public BoolLiteral(boolean val) {
    	value = val;
    }
    
    @Override
    public Object getLiteral(){
    	return value;
    }

    /**
     * Accept for the visitor pattern.
     * @param visitor the visitor
     **/
    @Override
    public <T> T accept(AnnotationVisitor<T> visitor) {
    	return visitor.literal(this);
    }
    
    @Override
    public String toString() {
        return Boolean.toString(value);
    }
}
