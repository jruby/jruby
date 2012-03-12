/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ast.java_signature;

/**
 *
 * @author enebo
 */
public class StringAnnotationExpression implements AnnotationExpression {
    private String value;
    
    public StringAnnotationExpression(String value) {
        this.value = value;
    }
    
    @Override
    public String toString() {
        return "\"" + value + "\"";
    }
    
}
