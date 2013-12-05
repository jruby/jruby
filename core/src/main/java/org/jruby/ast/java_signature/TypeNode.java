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
 * Base class for all typed nodes
 */
public class TypeNode implements AnnotationExpression {
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
