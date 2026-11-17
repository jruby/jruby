package org.jruby.ast.java_signature;

public class DefaultAnnotationParameter extends AnnotationParameter {
    public DefaultAnnotationParameter(AnnotationExpression expr) {
        super("value", expr);
    }

    @Override
    public String toString() {
        return getExpression().toString();
    }
}
