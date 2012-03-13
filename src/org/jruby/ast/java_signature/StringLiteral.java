/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ast.java_signature;

/**
 *
 * @author enebo
 */
public class StringLiteral implements Literal {
    private String string;
    
    public StringLiteral(String string) {
        this.string = string;
    }
    
    @Override
    public String toString() {
        return "\"" + string + "\"";
    }
}
