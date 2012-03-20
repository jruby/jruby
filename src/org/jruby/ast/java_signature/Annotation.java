package org.jruby.ast.java_signature;

import java.util.List;

/**
 *
 */
public class Annotation implements AnnotationExpression {
    private String name;
    private List<AnnotationParameter> parameters;
    
    public Annotation(String name, List<AnnotationParameter> parameters) {
        this.name = name;
        this.parameters = parameters;
    }
    
    /**
     * modifiers and annotations can be mixed together in java signatures.
     */
    public boolean isAnnotation() {
        return true;
    }
    
    public String getName() {
        return name;
    }
    
    public List<AnnotationParameter> getParameters() {
        return parameters;
    }
    
    @Override
    public String toString() {
        return name + toStringParameters();
    }
    
    public String toStringParameters() {
        int length = parameters.size();
        if (length == 0) return "";
        
        StringBuilder buf = new StringBuilder("(");
        for (int i = 0; i < length - 1; i++) {
            buf.append(parameters.get(i)).append(", ");
        }
        buf.append(parameters.get(length - 1)).append(')');
        
        return buf.toString();
    }
}
