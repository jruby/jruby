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
