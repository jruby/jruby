/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 ***** END LICENSE BLOCK *****/

package org.jruby.internal.runtime.methods;

/**
 * Wraps a method bound under a different name via {@code define_method(name, Method)}
 * so {@code Method#original_name} reports the source name. Per-binding wrapper, so two
 * {@code define_method} calls over the same underlying method each record their own
 * source name independently.
 */
public class RenamedDynamicMethod extends DelegatingDynamicMethod {
    private final String sourceName;

    public RenamedDynamicMethod(DynamicMethod delegate, String sourceName) {
        super(delegate);
        this.sourceName = sourceName;
    }

    @Override
    public String getOldName() {
        return sourceName;
    }

    @Override
    public DynamicMethod dup() {
        return new RenamedDynamicMethod(delegate.dup(), sourceName);
    }
}
