package org.jruby.ast.java_signature;

/**
 * Base class for all typed nodes
 */
public class TypeNode {
    protected String name;
    protected boolean isArray = false;

    public TypeNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isPrimitive() {
        return false;
    }

    public boolean isVoid() {
        return false;
    }

    public String getWrapperName() {
        return name;
    }

    public boolean isTyped() {
        return false;
    }

    public boolean isArray() {
        return isArray;
    }

    public void setIsArray(boolean isArray) {
        this.isArray = isArray;
    }
}
