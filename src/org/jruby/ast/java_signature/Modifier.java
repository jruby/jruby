package org.jruby.ast.java_signature;

/**
 * valid method declaration modifiers
 */
public enum Modifier {
    PUBLIC("public"), PROTECTED("protected"), PRIVATE("private"), STATIC("static"),
    ABSTRACT("abstract"), FINAL("final"), NATIVE("native"), SYNCHRONIZED("synchronized"),
    TRANSIENT("transient"), VOLATILE("volatile"), STRICTFP("strictfp");

    private String name;

    Modifier(String name) {
        this.name = name;
    }
    
    /**
     * Annotations and modifiers can be mixed together in a signature.
     */
    public boolean isAnnotation() {
        return false;
    }

    @Override
    public String toString() {
        return name;
    }
}
