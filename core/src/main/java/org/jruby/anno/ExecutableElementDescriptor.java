/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2008-2013 Charles Oliver Nutter <headius@headius.com>
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
package org.jruby.anno;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;

/**
 * A version of MethodDescriptor that works against ExecutableElement during compile time annotation processing.
 */
public class ExecutableElementDescriptor extends MethodDescriptor<ExecutableElement> {
    public final ExecutableElement method;

    public ExecutableElementDescriptor(ExecutableElement method) {
        super(method);
        this.method = method;
    }

    protected <A extends Annotation> A getAnnotation(ExecutableElement methodObject, Class<A> annotationType) {
        return methodObject.getAnnotation(annotationType);
    }

    protected int getModifiers(ExecutableElement methodObject) {
        Set<javax.lang.model.element.Modifier> mods = methodObject.getModifiers();
        int modifierTmp = 0;
        try {
            // TODO: gross
            for (javax.lang.model.element.Modifier mod : mods) {
                modifierTmp |= (Integer) Modifier.class.getField(mod.name()).get(null);
            }
        }
        catch (NoSuchFieldException|IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return modifierTmp;
    }

    protected String getDeclaringClassName(ExecutableElement methodObject) {
        return getActualQualifiedName((TypeElement) methodObject.getEnclosingElement()).toString();
    }

    protected String getSimpleName(ExecutableElement methodObject) {
        return methodObject.getSimpleName().toString();
    }

    protected boolean hasContext(ExecutableElement methodObject) {
        List<? extends VariableElement> symbolicParameters = methodObject.getParameters();
        if (symbolicParameters.size() > 0) {
            return symbolicParameters.get(0).asType().toString().equals("org.jruby.runtime.ThreadContext");
        }

        return false;
    }

    protected boolean hasBlock(ExecutableElement methodObject) {
        List<? extends VariableElement> symbolicParameters = methodObject.getParameters();

        if (symbolicParameters.size() > 0) {
            return symbolicParameters.get(symbolicParameters.size() - 1).asType().toString().equals("org.jruby.runtime.Block");
        }

        return false;
    }

    protected int parameterCount(ExecutableElement methodObject) {
        return methodObject.getParameters().size();
    }

    protected String parameterAsString(ExecutableElement methodObject, int index) {
        return methodObject.getParameters().get(index).asType().toString();
    }

    public static CharSequence getActualQualifiedName(TypeElement td) {
        if (td.getNestingKind() == NestingKind.MEMBER) {
            return getActualQualifiedName((TypeElement)td.getEnclosingElement()) + "$" + td.getSimpleName();
        }
        return td.getQualifiedName().toString();
    }
}
