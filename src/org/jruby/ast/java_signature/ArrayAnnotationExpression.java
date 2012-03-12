/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ast.java_signature;

import java.util.List;

/**
 *
 * @author enebo
 */
public class ArrayAnnotationExpression implements AnnotationExpression {
    private List<AnnotationExpression> expressions;
    
    public ArrayAnnotationExpression(List<AnnotationExpression> expressions) {
        this.expressions = expressions;
    }
    
}
