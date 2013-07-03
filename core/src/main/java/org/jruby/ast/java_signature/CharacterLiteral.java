/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ast.java_signature;

/**
 *
 * @author enebo
 */
public class CharacterLiteral implements Literal {
    private String string;
    
    public CharacterLiteral(String string) {
        this.string = string;
    }
    
    @Override
    public String toString() {
        return "'" + string + "'";
    }
}