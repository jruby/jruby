package org.jruby.ast.java_signature;

import java.util.List;

/**
 * Java Method signature declaration
 */
public class MethodSignatureNode extends SignatureNode {
    protected TypeNode returnType;

    public MethodSignatureNode(String name, List<ParameterNode> parameterList) {
        super(name, parameterList);
    }

    public TypeNode getReturnType() {
        return returnType;
    }

    public void setReturnType(TypeNode returnType) {
        this.returnType = returnType;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        for (Modifier modifier: modifiers) {
            builder.append(modifier).append(' ');
        }

        if (extraTypeInfo != null) {
            builder.append(extraTypeInfo).append(' ');
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