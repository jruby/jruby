/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast.java_signature;

/**
 * For Java primitive types: byte, short, int, long, char, float, double, boolean, void
 */
public class PrimitiveTypeNode extends TypeNode {
    public static PrimitiveTypeNode BYTE = new PrimitiveTypeNode("byte", "Byte");
    public static PrimitiveTypeNode SHORT = new PrimitiveTypeNode("short", "Short");
    public static PrimitiveTypeNode INT = new PrimitiveTypeNode("int", "Integer");
    public static PrimitiveTypeNode LONG = new PrimitiveTypeNode("long", "Long");
    public static PrimitiveTypeNode CHAR = new PrimitiveTypeNode("char", "Character");
    public static PrimitiveTypeNode FLOAT = new PrimitiveTypeNode("float", "Float");
    public static PrimitiveTypeNode DOUBLE = new PrimitiveTypeNode("double", "Double");
    public static PrimitiveTypeNode BOOLEAN = new PrimitiveTypeNode("boolean", "Boolean");
    public static PrimitiveTypeNode VOID = new PrimitiveTypeNode("void", "void");

    private final String wrapperName;

    // This should only be used by constants above, but I left it a little open if you want to
    // add your own new primitives!
    protected PrimitiveTypeNode(String name, String wrapperName) {
        super(name);
        this.wrapperName = wrapperName;
    }

    @Override
    public boolean isPrimitive() {
        return true;
    }

    @Override
    public boolean isVoid() {
        return name.equals("void");
    }

    @Override
    public String getWrapperName() {
        return wrapperName;
    }
}
