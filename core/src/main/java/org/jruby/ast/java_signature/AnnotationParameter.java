/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ast.java_signature;

// @Foo(@bar)         DEFAULT for single value anno
// @Foo(value=@bar)   ANNO
// @Foo(value={@bar}) LIST
// @Foo(value="heh")  EXPR
public class AnnotationParameter {
    private final String name;
    private final AnnotationExpression expression;
    
    public AnnotationParameter(String name, AnnotationExpression value) {
        this.name = name;
        this.expression = value;
    }
    
    public AnnotationExpression getExpression() {
        return expression;
    }
    
    public String getName() {
		return name;
	}
    
    @Override
    public String toString() {
        return name + '=' + expression;
    }
}
