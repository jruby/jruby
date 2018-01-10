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

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;

public abstract class MethodDescriptor<T> {
    public final boolean isStatic;
    public final boolean hasContext;
    public final boolean hasBlock;
    public final boolean hasVarArgs;
    public final int actualRequired;
    public final int arity;
    public final int required;
    public final int optional;
    public final boolean rest;
    public final JRubyMethod anno;
    public final int modifiers;
    public final String declaringClassName;
    public final String declaringClassPath;
    public final String name;
    public final String rubyName;

    protected abstract <A extends Annotation> A getAnnotation(T methodObject, Class<A> annotationType);
    protected abstract int getModifiers(T methodObject);
    protected abstract String getDeclaringClassName(T methodObject);
    protected abstract String getSimpleName(T methodObject);
    protected abstract boolean hasContext(T methodObject);
    protected abstract boolean hasBlock(T methodObject);
    protected abstract int parameterCount(T methodObject);
    protected abstract String parameterAsString(T methodObject, int index);

//    protected abstract <A extends Annotation> A getAnnotation(Class<A> annotationType);
//    protected abstract int getModifiers();
//    protected abstract String getDeclaringClassName();
//    protected abstract String getSimpleName();
//    protected abstract boolean hasContext();
//    protected abstract boolean hasBlock();
//    protected abstract int parameterCount();
//    protected abstract String parameterAsString(int index);

    public MethodDescriptor(T methodObject) {
        anno = getAnnotation(methodObject, JRubyMethod.class);
        modifiers = getModifiers(methodObject);
        declaringClassName = getDeclaringClassName(methodObject);
        declaringClassPath = declaringClassName.replace('.', '/');
        name = getSimpleName(methodObject);
        final String[] names = anno.name();
        rubyName = (names != null && names.length > 0) ? names[0] : name;
        isStatic = Modifier.isStatic(modifiers);
        hasContext = hasContext(methodObject);
        hasBlock = hasBlock(methodObject);

        final int parameterCount = parameterCount(methodObject);
        if (hasContext) {
            if (isStatic && (parameterCount < 2 || !parameterAsString(methodObject, 1).equals("org.jruby.runtime.builtin.IRubyObject"))) {
                throw new RuntimeException("static method without self argument: " + methodObject);
            }

            if (hasBlock) {
                // args should be before block
                hasVarArgs = parameterAsString(methodObject, parameterCount - 2).equals("org.jruby.runtime.builtin.IRubyObject[]");
            } else {
                // args should be at end
                hasVarArgs = parameterAsString(methodObject, parameterCount - 1).equals("org.jruby.runtime.builtin.IRubyObject[]");
            }
        } else {
            if (isStatic && (parameterCount < 1 || !parameterAsString(methodObject, 0).equals("org.jruby.runtime.builtin.IRubyObject"))) {
                throw new RuntimeException("static method without self argument: " + methodObject);
            }

            if (hasBlock) {
                hasVarArgs = parameterCount > 1 && parameterAsString(methodObject, parameterCount - 2).equals("org.jruby.runtime.builtin.IRubyObject[]");
            } else {
                hasVarArgs = parameterCount > 0 && parameterAsString(methodObject, parameterCount - 1).equals("org.jruby.runtime.builtin.IRubyObject[]");
            }
        }

        optional = anno.optional();
        rest = anno.rest();
        required = anno.required();

        if (optional == 0 && !rest) {
            int args = parameterCount;
            if (args == 0) {
                actualRequired = 0;
            } else {
                if (isStatic) args--;
                if (hasContext) args--;
                if (hasBlock) args--;

                // TODO: confirm expected args are IRubyObject (or similar)
                actualRequired = args;
            }
        } else {
            // optional args, so we have IRubyObject[]
            // TODO: confirm
            int args = parameterCount;
            if (args == 0) {
                actualRequired = 0;
            } else {
                if (isStatic) args--;
                if (hasContext) args--;
                if (hasBlock) args--;

                // minus one more for IRubyObject[]
                args--;

                // TODO: confirm expected args are IRubyObject (or similar)
                actualRequired = args;
            }

            if (actualRequired != 0) {
                throw new RuntimeException("Combining specific args with IRubyObject[] is not yet supported");
            }
        }

        int arityRequired = Math.max(required, actualRequired);
        arity = (optional > 0 || rest) ? -(arityRequired + 1) : arityRequired;
    }
}
