package org.jruby.ast.java_signature;

/**
 * Base class for all typed nodes
 */
public class TypeNode {
    protected String name;

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

    /**
     * Get the boxed or wrapper class name of the type.  Note: this
     * will only return something different for primitive types.
     */
    public String getWrapperName() {
        return name;
    }

    /**
     * Get the name of the class with all of its potential generic glory.
     */
    public String getFullyTypedName() {
        return name;
    }

    public boolean isTyped() {
        return false;
    }

    public boolean isArray() {
        return false;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TypeNode)) return false;


        return (name == null && ((TypeNode) other).name == null) ||
                name.equals(((TypeNode) other).name);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return getFullyTypedName();
    }
}
