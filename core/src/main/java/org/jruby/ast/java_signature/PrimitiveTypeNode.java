/*
 **** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2010 Thomas E Enebo <tom.enebo@gmail.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ast.java_signature;

/**
 * For Java primitive types: byte, short, int, long, char, float, double, boolean, void
 */
public class PrimitiveTypeNode extends TypeNode {
    public static final PrimitiveTypeNode BYTE = new PrimitiveTypeNode("byte", "Byte");
    public static final PrimitiveTypeNode SHORT = new PrimitiveTypeNode("short", "Short");
    public static final PrimitiveTypeNode INT = new PrimitiveTypeNode("int", "Integer");
    public static final PrimitiveTypeNode LONG = new PrimitiveTypeNode("long", "Long");
    public static final PrimitiveTypeNode CHAR = new PrimitiveTypeNode("char", "Character");
    public static final PrimitiveTypeNode FLOAT = new PrimitiveTypeNode("float", "Float");
    public static final PrimitiveTypeNode DOUBLE = new PrimitiveTypeNode("double", "Double");
    public static final PrimitiveTypeNode BOOLEAN = new PrimitiveTypeNode("boolean", "Boolean");
    public static final PrimitiveTypeNode VOID = new PrimitiveTypeNode("void", "void");

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
