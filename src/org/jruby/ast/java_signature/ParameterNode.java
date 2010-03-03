package org.jruby.ast.java_signature;

// Fixme: varargs and variableNames with [] on them should ammend type on construction to save
// consumer the effort.
public class ParameterNode {
    private final TypeNode type;
    private final String variableName;
    private boolean isFinal = false;
    private boolean isVarArgs = false;
    
    public ParameterNode(TypeNode type, String variableName) {
        this.type = type;
        this.variableName = variableName;
    }

    public ParameterNode(TypeNode type, String variableName, boolean isFinal) {
        this(type, variableName);

        this.isFinal = isFinal;
    }

    public ParameterNode(TypeNode type, String variableName, boolean isFinal, boolean isVarArgs) {
        this(type, variableName, isFinal);

        this.isVarArgs = isVarArgs;
    }

    public TypeNode getType() {
        return type;
    }

    public String getVariableName() {
        return variableName;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public boolean isVarArgs() {
        return isVarArgs;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        if (isFinal) builder.append("final ");
        builder.append(type);
        if (isVarArgs()) builder.append("...");
        if (variableName != null) builder.append(" ").append(variableName);

        return builder.toString();
    }
}
