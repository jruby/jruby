/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ast.java_signature;

import java.util.List;

public class ArrayAnnotationExpression implements AnnotationExpression {
    private final List<AnnotationExpression> expressions;
    
    public ArrayAnnotationExpression(List<AnnotationExpression> expressions) {
        this.expressions = expressions;
    }

    public List<AnnotationExpression> getExpressions() {
		return expressions;
	}

    /**
     * Accept for the visitor pattern.
     * @param visitor the visitor
     **/
    @Override
    public <T> T accept(AnnotationVisitor<T> visitor) {
    	return visitor.annotation_array(this);
    }
    
    @Override
    public String toString() {
        int length = expressions.size();
        StringBuilder buf = new StringBuilder("{");

        if (length == 0) return buf.append("}").toString();
        
        for (int i = 0; i < length - 1; i++) {
            buf.append(expressions.get(i)).append(", ");
        }
        buf.append(expressions.get(length - 1)).append("}");
        
        return buf.toString();
    }
}
