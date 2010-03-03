/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast.java_signature;

/**
 * For Java primitive types: byte, short, int, long, char, float, double, boolean, void
 */
public class PrimitiveTypeNode extends TypeNode {
    public static PrimitiveTypeNode BYTE = new PrimitiveTypeNode("byte");
    public static PrimitiveTypeNode SHORT = new PrimitiveTypeNode("short");
    public static PrimitiveTypeNode INT = new PrimitiveTypeNode("int");
    public static PrimitiveTypeNode LONG = new PrimitiveTypeNode("long");
    public static PrimitiveTypeNode CHAR = new PrimitiveTypeNode("char");
    public static PrimitiveTypeNode FLOAT = new PrimitiveTypeNode("float");
    public static PrimitiveTypeNode DOUBLE = new PrimitiveTypeNode("double");
    public static PrimitiveTypeNode BOOLEAN = new PrimitiveTypeNode("boolean");
    public static PrimitiveTypeNode VOID = new PrimitiveTypeNode("void");

    // This should only be used by constants above, but I left it a little open if you want to
    // add your own new primitives!
    protected PrimitiveTypeNode(String name) {
        super(name);
    }

    @Override
    public boolean isPrimitive() {
        return true;
    }

    @Override
    public String toString() {
        return name;
    }
}
