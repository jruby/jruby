package org.jruby.ast.java_signature;

import java.util.List;

/**
 * Java Method signature declaration
 */
public class MethodSignatureNode {
    protected List<Modifier> modifiers;
    protected String name;
    protected List<ParameterNode> parameterList;
    protected TypeNode returnType;
    protected List<TypeNode> throwTypes;

    public MethodSignatureNode(String name, List<ParameterNode> parameterList) {
        this.name = name;
        this.parameterList = parameterList;
    }

    public String getName() {
        return name;
    }

    public List<ParameterNode> getParameters() {
        return parameterList;
    }

    public TypeNode getReturnType() {
        return returnType;
    }

    public void setReturnType(TypeNode returnType) {
        this.returnType = returnType;
    }

    public void setModifiers(List<Modifier> modifiers) {
        this.modifiers = modifiers;
    }

    public List<Modifier> getModifiers() {
        return modifiers;
    }

    public void setThrows(List<TypeNode> throwTypes) {
        this.throwTypes = throwTypes;
    }

    public List<TypeNode> getThrows() {
        return throwTypes;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        for (Modifier modifier: modifiers) {
            builder.append(modifier).append(' ');
        }
        
        builder.append(returnType).append(' ');
        
        builder.append(name).append('(');

        int length = parameterList.size();
        for (int i = 0; i < length - 1; i++) {
            builder.append(parameterList.get(i)).append(", ");
        }

        if (length > 0) builder.append(parameterList.get(length - 1));

        builder.append(')');

        length = throwTypes.size();
        if (length > 0) {
            builder.append(" throws ");
            for (int i = 0; i < length - 1; i++) {
                builder.append(throwTypes.get(i)).append(", ");
            }
            builder.append(throwTypes.get(length - 1));
        }

        return builder.toString();
    }
}