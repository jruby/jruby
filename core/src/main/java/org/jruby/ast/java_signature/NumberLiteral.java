/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ast.java_signature;

/**
 *
 * @author enebo
 */
public class NumberLiteral implements Literal {
    private String valueRaw;
    
    public NumberLiteral(String value) {
        this.valueRaw = value.replaceAll("_", "");// remove all separators, regardless
    }
    
    public boolean isFloat() {
    	return valueRaw.contains(".");
    }
    
    @Override
    public Object getLiteral() {
    	return valueRaw;
    }

    /**
     * Accept for the visitor pattern.
     * @param visitor the visitor
     **/
    @Override
    public <T> T accept(AnnotationVisitor<T> visitor) {
    	return visitor.number_literal(this);
    }
    
    @Override
    public String toString() {
        return valueRaw;
    }
}
