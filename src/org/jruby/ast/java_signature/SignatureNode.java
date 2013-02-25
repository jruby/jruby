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

import java.util.List;

public class SignatureNode {
    protected List<Object> modifiers;
    protected String name;
    protected List<ParameterNode> parameterList;
    protected String extraTypeInfo;
    protected List<TypeNode> throwTypes;

    public SignatureNode(String name, List<ParameterNode> parameterList) {
        this.name = name;
        this.parameterList = parameterList;
    }

    public String getName() {
        return name;
    }

    public List<ParameterNode> getParameters() {
        return parameterList;
    }

    public void setModifiers(List<Object> modifiers) {
        this.modifiers = modifiers;
    }

    public void setExtraTypeInfo(String extraTypeInfo) {
        this.extraTypeInfo = extraTypeInfo;
    }

    public List<Object> getModifiers() {
        return modifiers;
    }

    public void setThrows(List<TypeNode> throwTypes) {
        this.throwTypes = throwTypes;
    }

    public List<TypeNode> getThrows() {
        return throwTypes;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        for (Object modifier: modifiers) {
            builder.append(modifier).append(' ');
        }

        if (extraTypeInfo != null) {
            builder.append(extraTypeInfo).append(' ');
        }

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
