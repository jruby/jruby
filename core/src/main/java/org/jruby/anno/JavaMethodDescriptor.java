/*
 ***** BEGIN LICENSE BLOCK *****
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

import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.util.CodegenUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class JavaMethodDescriptor extends MethodDescriptor<Method> {
    public final Class[] parameters;
    public final Class returnClass;
    public final Class declaringClass;
    public final String signature;
    public final Class[] argumentTypes;

    public JavaMethodDescriptor(Method method) {
        super(method);

        declaringClass = method.getDeclaringClass();
        parameters = method.getParameterTypes();
        returnClass = method.getReturnType();

        int start = (hasContext ? 1 : 0) + (isStatic ? 1 : 0);
        int end = parameters.length - (hasBlock ? 1 : 0);
        argumentTypes = new Class[end - start];
        System.arraycopy(parameters, start, argumentTypes, 0, end - start);

        signature = CodegenUtils.sig(method.getReturnType(), method.getParameterTypes());
    }

    public Class getDeclaringClass() {
        return declaringClass;
    }

    public Class[] getParameterClasses() {
        return parameters;
    }

    public Class getReturnClass() {
        return returnClass;
    }

    protected <A extends Annotation> A getAnnotation(Method methodObject, Class<A> annotationType) {
        return methodObject.getAnnotation(annotationType);
    }

    protected int getModifiers(Method methodObject) {
        return methodObject.getModifiers();
    }

    protected String getDeclaringClassName(Method methodObject) {
        return methodObject.getDeclaringClass().getName();
    }

    protected String getSimpleName(Method methodObject) {
        return methodObject.getName();
    }

    protected boolean hasContext(Method methodObject) {
        Class[] parameters = methodObject.getParameterTypes();
        if (parameters.length > 0) {
            return parameters[0] == ThreadContext.class;
        }

        return false;
    }

    protected boolean hasBlock(Method methodObject) {
        Class[] parameters = methodObject.getParameterTypes();
        if (parameters.length > 0) {
            return parameters[parameters.length - 1] == Block.class;
        }

        return false;
    }

    protected int parameterCount(Method methodObject) {
        return methodObject.getParameterTypes().length;
    }

    protected String parameterAsString(Method methodObject, int index) {
        return methodObject.getParameterTypes()[index].getName();
    }
}
