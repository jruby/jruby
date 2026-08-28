/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ast.java_signature;

public class StringLiteral implements Literal {
    private final String string;
    
    public StringLiteral(String string) {
        this.string = string;
    }
    
    @Override
    public Object getLiteral(){
    	return string;
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
        return "\"" + string + "\"";
    }
}
